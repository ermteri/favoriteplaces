package eu.torsteneriksson.storetracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

public class TrackerService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static Location mCurrentLocation = null;
    private static final String TAG = "TrackerService";
    private FavoriteHandler mFavoriteHandler = null;

    // A request to connect to Location Services
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationListener mLocationListener;

    // Settings
    private int cTrackingInterval;
    private boolean cShowLocationData;
    private int cMaxFavorites;
    private String mPlace = "";
    private boolean mUseFavorite;
    private String mFavoriteCandidateAddress = "";

    // Messenger stuff
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;

    static final int MSG_SVC_STATUS = 4;

    static final int MSG_SAVE_CURRENT = 5;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,msg.toString());
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    updateActivity(false);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_VALUE:
                    // Handle the information got from the activity!
                    break;
                case MSG_SVC_STATUS:
                    //updateActivity(false);
                    break;
                case MSG_SAVE_CURRENT:
                    mUseFavorite = false;
                    new StoreToDatabase().execute();
                    mFavoriteHandler.reset();
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    void getPlaces() {
        Log.d(TAG,"getPlaces");
        PendingResult<PlaceLikelihoodBuffer> result = null;
        try {
            result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
        } catch(SecurityException e) {
            Log.d(TAG,e.toString());
        }
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                Log.d(TAG,"onResult");
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    TrackerUtilities.displayToast(false,TrackerService.this,placeLikelihood.getPlace().getName().toString());
                    Log.i(TAG, String.format("FavoritePlace '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                    mPlace = mPlace + placeLikelihood.getPlace().getName().toString();
                }
                likelyPlaces.release();
            }
        });
    }

    private class LocationListener implements
            com.google.android.gms.location.LocationListener {

        public LocationListener() {
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: " + location.getProvider());
            mCurrentLocation = location;
            //getPlaces();
            Log.d(TAG,"onLocationChanged::currentLocation" + TrackerUtilities.getAddress(mCurrentLocation,TrackerService.this));
            boolean store = mFavoriteHandler.isFavorite(location);
            if(store) {
                Log.d(TAG,"FavoriteHandler found favorite");
                if(mFavoriteHandler.getLocation() != null) {
                    mUseFavorite = true;
                    new StoreToDatabase().execute();
                } else {
                    Log.d(TAG,"Inconsistency in FavoriteHandler");
                }
            }
            updateActivity(store);
        }
    }

    void updateActivity(boolean stored) {
        Log.d(TAG,"updateActivity");
        final Location location = mFavoriteHandler.getmFavoriteCandidate();
        if(location == null)
            return;
        Bundle b = new Bundle();
        b.putString("str1", "ab" );
        b.putInt("STATE",mFavoriteHandler.getState());
        b.putBoolean("STORED",stored);
        b.putDouble("LAT",location.getLatitude());
        b.putDouble("LNG",location.getLongitude());
        b.putDouble("ACCURACY",location.getAccuracy());
        b.putInt("TIME_SPENT",(int)mFavoriteHandler.getTimeSpent()/1000);
        if(mFavoriteHandler.getState() != FavoriteHandler.FAVORITE_FOUND)
            mFavoriteCandidateAddress = "";
        if(mFavoriteCandidateAddress.isEmpty() && mFavoriteHandler.getState() == FavoriteHandler.FAVORITE_FOUND) {
            Runnable getAddress = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Retrieve address for favoritecandidate");
                    mFavoriteCandidateAddress = TrackerUtilities.getAddress(location, TrackerService.this);
                    }
            };
            // do it
            getAddress.run();
        }
        Log.d(TAG,"mFavoriteCandidateAddress" + mFavoriteCandidateAddress);
        b.putString("FAVORITE_CANDIDATE_ADDRESS",mFavoriteCandidateAddress);

        Message msg = Message.obtain(null, MSG_SVC_STATUS);
        msg.setData(b);
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        mCurrentLocation = null;
        if(mFavoriteHandler == null) {
            mFavoriteHandler = new FavoriteHandler(this);
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API).addConnectionCallbacks(this)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this).build();
        // TrackerUtilities.notify(this, "onCreate", "newFavoriteHandler: " + String.valueOf(newFavoriteHandler));
        Toast.makeText(this, "Service started!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        //TrackerUtilities.notify(this, "onDestroy", "");
        Toast.makeText(this, "Service stopped!", Toast.LENGTH_LONG).show();
        mCurrentLocation = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.e(TAG, "onStartCommand");
        // Get settings.
        updateSettings();

        if(intent!= null && intent.getBooleanExtra("START",true)) {
            mLocationListener = new LocationListener();
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
            startInForeground();
        } else {
            stopLocationUpdates();
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
            else
                startLocationUpdates();
        }
        return START_STICKY;
    }

    private void updateSettings() {
        Log.d(TAG, "updateSettings");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        cTrackingInterval = Integer.parseInt(sharedPref.getString(SettingsActivity.TRACKING_INTERVAL, "30")) * 1000;
        cMaxFavorites = Integer.parseInt(sharedPref.getString(SettingsActivity.MAX_FAVORITES,"10"));
        cShowLocationData = sharedPref.getBoolean(SettingsActivity.SHOW_LOCATION_DATA, false);
        mFavoriteHandler.updateSettings();
        Log.d(TAG, "cTrackingInterval " + String.valueOf(cTrackingInterval));
    }

    private void startInForeground() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.description))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.favorite_icon)
                .build();
        startForeground(1298, notification);
    }

    private void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(cTrackingInterval);
        mLocationRequest.setFastestInterval(cTrackingInterval);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, mLocationListener);
        } catch (SecurityException e) {
            Log.d(TAG,e.toString());
        }
    }
    private void stopLocationUpdates() {
        Log.d(TAG,"stopLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, mLocationListener);
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Log.d(TAG, "onConnectionFailed:" + arg0.toString());
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.d(TAG, "onConnected");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Log.d(TAG,"onConnectionSuspended:" + String.valueOf(arg0));
    }

    // Run storeToDatabase on its own thread.
    private class StoreToDatabase extends AsyncTask<Void, Void, Boolean> {
        protected void onPreExecute() {
        }

        protected Boolean doInBackground(Void... params ) {
            Log.d(TAG,"doInBackgound");
            storeToDatabase();
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if(!result) {
                Log.d(TAG,"onPostExecute returned false");
            }
        }
    }

    // Store the found location to the database
    private void storeToDatabase() {
        Log.d(TAG,"storeToDatabase");
        SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(TrackerService.this);
        SQLiteDatabase db = trackerDatabaseHelper.getWritableDatabase();

        Location location;
        long time;
        if(mUseFavorite) {
            location = mFavoriteHandler.getLocation();
            time = mFavoriteHandler.getTime();
        } else {
            location = mFavoriteHandler.getmFavoriteCandidate();
            if(location != null) {
                time = location.getTime();
            } else {
                //TrackerUtilities.displayToast(true,TrackerService.this,getString(R.string.no_location_received_yet));
                return;
            }
        }
        long timeSpent = mFavoriteHandler.getTimeSpent();
        String address = TrackerUtilities.getAddress(location, TrackerService.this);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        Log.d(TAG,"adress to store:" + address);
        // Store
        ContentValues locations = new ContentValues();
        locations.put(TrackerDatabaseHelper.LATITUDE, latitude);
        locations.put(TrackerDatabaseHelper.LONGITUDE, longitude);
        locations.put(TrackerDatabaseHelper.TIME, time);
        locations.put(TrackerDatabaseHelper.TIME_SPENT, timeSpent);
        locations.put(TrackerDatabaseHelper.ADDRESS, address);
        locations.put(TrackerDatabaseHelper.CATEGORY,0);
        locations.put(TrackerDatabaseHelper.DESCRIPTION, mPlace);
        long key_id = db.insert(TrackerDatabaseHelper.TRACKER, null, locations);
        long numRows = DatabaseUtils.queryNumEntries(db, TrackerDatabaseHelper.TRACKER);
        Log.d(TAG,"Len:"+String.valueOf(numRows));
        while(DatabaseUtils.queryNumEntries(db, TrackerDatabaseHelper.TRACKER)> cMaxFavorites) {
            deleteFirstRow(db);
        }
        db.close();
        TrackerUtilities.notify(TrackerService.this, getString(R.string.favorite_found), address, key_id);
        Log.d(TAG,"key_id:" + key_id);
        updateActivity(true);
    }

    private void deleteFirstRow(SQLiteDatabase db) {
        Cursor cursor = db.query(TrackerDatabaseHelper.TRACKER, null, null, null, null, null, null);
        if(cursor.moveToFirst()) {
            String rowId = cursor.getString(cursor.getColumnIndex("_id"));
            db.delete(TrackerDatabaseHelper.TRACKER,
                    TrackerDatabaseHelper.KEY_ID + "=?", new String[]{rowId});
            Log.d(TAG, "One row deleted");
        }
        cursor.close();
    }
}