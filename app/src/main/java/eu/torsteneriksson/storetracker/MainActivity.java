package eu.torsteneriksson.storetracker;
/**
 * A. Database
 * Tables:
 * tracker
 * _id, date-time, location, time-spent
 * configuration
 * -id, key, value
 *
 * B. Algorithm
 * 1. Store first location received to last location and time when it was received.
 * 2. New location compare to last location
 * - if distance <= configured distance
 * - do nothing
 * - if distance > configured distance
 * - if now - timestamp for last location > configured time
 * - store last location to DB with timestamp and time spent.
 * - put last location = new location.
 * 3. Goto 2.
 *
 * -----------------------------------------
 * Future:
 * 1. DONE! Filter on date.
 * 2. DONE! Possibility to delete selected favorites, from the list and from the map
 * 3. Possibility to add own comments on a favorite
 * 4. Share should include all data from DB.
 * 5. Export to kml file so result can be presented on Google Earth.*
 * 6. Add priority to a favorite
 * 7. Extend the dialog for categories to be a menu to manage each favorite.
 *
 * Improvements/Ideas:
 * 1. Better calendar. Maybe this one is ok:
 * http://www.java2s.com/Open-Source/Android_Free_Code/Development/calendar/manishsri01_CustomCalendarAndroid.htm
 *
 * 2. Find places
 * https://developers.google.com/places/android-api/start
 *

 */

/**
 * C A T E G O R I E S
 *
 * Start with
 * - Pleasure,
 * - Religion,
 * - Transport,
 * - Bank,
 * - Health,
 * - Café & Restaurants,
 * - Shopping,
 * - Officials
 * - Sport
 * - Fishing
 * - Nature scenery
 * - Other
 *Pleasure
 amusement_park
 aquarium
 art_gallery
 bowling_alley
 casino
 movie_rental
 movie_theater
 moving_company
 museum
 zoo
 night_club

 *Religion
 church
 funeral_home
 furniture_store
 hindu_temple
 synagogue

 *Transportation
 car_dealer
 car_rental
 car_repair
 car_wash
 bus_station
 gas_station
 park
 parking
 subway_station
 taxi_stand
 train_station
 travel_agency

 *Finance
 atm
 bank
 finance

 *Health
 doctor
 beauty_salon
 dentist
 gym
 hair_care
 health
 hospital
 pharmacy
 physiotherapist
 spa
 veterinary_care

 *Food
 bakery
 bar
 cafe
 food
 meal_delivery
 meal_takeaway
 grocery_or_supermarket
 restaurant

 *Shopping
 bicycle_store
 book_store
 clothing_store
 convenience_store
 department_store
 electronics_store
 florist
 hardware_store
 jewelry_store
 liquor_store
 school
 shoe_store
 shopping_mall

 *Officials
 city_hall
 courthouse
 embassy
 library
 local_government_office
 police
 post_office
 university

 *Handyman
 electrician
 fire_station
 painter
 plumber

 *Sports
 stadium
 *
 */




import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class MainActivity extends Activity  implements OnMapReadyCallback {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "MyPrefsFile";
    private int cFavoriteUpdate = 30000;
    private boolean mIsActive = false;
    private Settings mSettings;
    private GoogleMap mCurrentMap;
    private ClusterManager<FavoriteMarker> mClusterManager;
    private float mDefaultZoomLevel = 14.0f;
    public static FavoriteFilter mFilter;
    final Handler mUpdateMainLayoutHandler = new Handler();
    private Cursor mLastCursorRetrieved = null;
    /*final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mUpdateMainLayoutHandler-id:" + this);
            updateMainLayout();
            mUpdateMainLayoutHandler.postDelayed(this, cFavoriteUpdate);
        }
    };*/

    // ******************* Messenger part ***************************
    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /** Some text view we are using to show state information. */
    TextView mCallbackText;

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,"IncomingHandler::handleMessage"+msg.what);
            switch (msg.what) {
                case TrackerService.MSG_SVC_STATUS:
                    updateStatus(msg);
                    updateMainLayout();
                    break;
                default:
                    super.handleMessage(msg);
            }
            removeMessages(0);
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            Log.d(TAG,"ServiceConnection::onServiceConnected:");
            mService = new Messenger(service);
            //mCallbackText.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        TrackerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"onServiceDisconnected");

            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            //mCallbackText.setText("Disconnected.");
        }
    };

    void doBindService() {
        Log.d(TAG,"doBindService");

        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(MainActivity.this,
                TrackerService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        Log.d(TAG,"doUnbindService");

        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            TrackerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            TextView currentLocation = (TextView) findViewById(R.id.current_location);
            currentLocation.setText(getString(R.string.wait_connection));
            TextView currentStatusTime = (TextView) findViewById(R.id.current_status_time);
            currentStatusTime.setText("...");
            TextView currentStatusAcc = (TextView) findViewById(R.id.current_status_accuracy);
            currentStatusAcc.setText("...");
            updateStatusField(FavoriteHandler.INIT);
        }
    }

    void updateStatus(Message msg) {
        Log.d(TAG,"updateStatus:"+msg.what);
        TextView currentLocation = (TextView) findViewById(R.id.current_location);
        TextView currentStatusTimeSpent = (TextView) findViewById(R.id.current_status_time);
        TextView currentStatusAccuracy = (TextView) findViewById(R.id.current_status_accuracy);

        Bundle b = msg.getData();
        String position = "";
        String statusTimeSpent = "";
        String statusAccuracy = "";
        if(b.getBoolean("STORED"))
            position = getString(R.string.favorite_stored);
        else {
            if (b.getString("FAVORITE_CANDIDATE_ADDRESS").isEmpty()) {
                position = String.format(
                        "Lat: %.4f Lng:%.4f",
                        b.getDouble("LAT"),
                        b.getDouble("LNG"));
            } else {
                position = b.getString("FAVORITE_CANDIDATE_ADDRESS");
            }
            statusAccuracy = getString(R.string.accuracy) + ": " + String.format("%.0f", b.getDouble("ACCURACY")) + "m ";
            statusTimeSpent = getString(R.string.timespent) + ": " + String.valueOf(
                    TrackerUtilities.formatSpentTime(b.getInt("TIME_SPENT")));
        }
        currentLocation.setText(position);
        currentStatusTimeSpent.setText(statusTimeSpent);
        currentStatusAccuracy.setText(statusAccuracy);
        updateStatusField(b.getInt("STATE"));
    }
    // ******************* Messenger part end ***************************


    @Override
    protected void onStart() {
        Log.d(TAG,"onStart");

        super.onStart();
       // updateMainLayout();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        mCurrentMap = map;

        mCurrentMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        mClusterManager = new ClusterManager<FavoriteMarker>(this, mCurrentMap);
        mCurrentMap.setOnCameraIdleListener(mClusterManager);
        mCurrentMap.setOnMarkerClickListener(mClusterManager);
        mClusterManager.setRenderer(new MarkerClusterRenderer(
                this, mCurrentMap, mClusterManager));

        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<FavoriteMarker>() {
            @Override
            public boolean onClusterClick(Cluster<FavoriteMarker> cluster) {
                Log.d(TAG,"onClusterClick");
                return false;
            }

        });

        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<FavoriteMarker>() {
            @Override
            public boolean onClusterItemClick(FavoriteMarker item) {
                Log.d(TAG,"onClusterItemClick");
                int key = item.getId();
                if(key != -1) {
                    Intent editFavoritesIntent = new Intent(MainActivity.this,HandleFavoriteActivity.class);
                    editFavoritesIntent.putExtra("EXTRA_KEYID",key);
                    startActivity(editFavoritesIntent);
                }
                return true;
            }
        });

        moveToLastFavorite(5);
        displayFavoritesOnMap();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCallbackText = (TextView) findViewById(R.id.current_location);
        mSettings = new Settings(this);

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mFilter = new FavoriteFilter(
                settings.getLong("selectedDateInMillis",0),
                settings.getBoolean("todayOnly",false),
                settings.getInt("category",-1),
                settings.getString("searchText",""));

        //Switch tracker_switch = (Switch) findViewById(R.id.switch_tracker_service);
        ToggleButton tracker_switch = (ToggleButton) findViewById(R.id.switch_tracker_service);
        tracker_switch.setChecked(TrackerUtilities.isMyServiceRunning(TrackerService.class, this));
        if(TrackerUtilities.isMyServiceRunning(TrackerService.class, this)) {
            doBindService();
        }

        cFavoriteUpdate = mSettings.getFavoriteUpdate();
        ((MapFragment) getFragmentManager().findFragmentById(R.id.current_map))
                .getMapAsync(this);

        mIsActive = true;
        new UpdateAddress().execute();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mIsActive = false;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("selectedDateInMillis", mFilter.getMillis());
        editor.putBoolean("todayOnly", mFilter.getTodayOnly());
        editor.putInt("category", mFilter.getCategory());
        editor.putString("searchText",mFilter.getSearchText());

        // Commit the edits!
        editor.commit();
       // updateMainLayout();
        //mUpdateMainLayoutHandler.removeCallbacks(mUpdateRunnable);
        if(TrackerUtilities.isMyServiceRunning(TrackerService.class, this)) {
            doUnbindService();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        Log.d(TAG, "onPause");
        mIsActive = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        mLastCursorRetrieved = null;
        if(mClusterManager!= null) {
            Log.d(TAG, "ClearItems");
            mClusterManager.clearItems();
            mCurrentMap.clear();
            displayFavoritesOnMap();
            mClusterManager.cluster();
        }
        updateMainLayout();

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG,"onCreateOptionsMenu");

        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d(TAG, "onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    public static FavoriteFilter getFilter() {
        return mFilter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Log.d(TAG,"onOptionsItemSelected");

        OptionsMenuHandler omh = new OptionsMenuHandler();
        return(omh.handleOptionsSelected(this,item));
    }

    private void backupDb() {
        Log.d(TAG,"backupDb");

        final String inFileName = this.getDatabasePath(TrackerDatabaseHelper.DB_NAME).getPath();
        File dbFile = new File(inFileName);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(dbFile);
        } catch (FileNotFoundException e) {
            TrackerUtilities.displayToast(true, this,"Inputfile not found");
        }

        new FileChooser(this)
                .setFileListener(new FileChooser.FileSelectedListener() {
                    @Override public void fileSelected(final File file) {
                        // do something with the file
                    }})
                .showDialog();

        String outFileName = Environment.getExternalStorageDirectory()+"/database_copy.db";

        // Open the empty db as the output stream
        OutputStream output = null;
        try {
            output = new FileOutputStream(outFileName);
        } catch (FileNotFoundException e) {
            TrackerUtilities.displayToast(true, this,"Outputfile not found");
        }

        // Transfer bytes from the inputfile to the outputfile7
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            // Close the streams
            output.flush();
            output.close();
            fis.close();
        } catch (IOException e) {
            TrackerUtilities.displayToast(true, this,"Copy failed");
        }
    }
    /**
     * #####################################################################################
     * ################### Database Utilities ##############################################
     * #####################################################################################
     */
    private SQLiteDatabase getDb(boolean writeable) {
        Log.d(TAG,"getDb");

        if(writeable) {
            TrackerDatabaseHelper trackerDatabaseHelper = new TrackerDatabaseHelper(this);
            return trackerDatabaseHelper.getWritableDatabase();
        } else {
            TrackerDatabaseHelper trackerDatabaseHelper = new TrackerDatabaseHelper(this);
            return trackerDatabaseHelper.getReadableDatabase();
        }
    }
    private Cursor getFavorites() {
        Log.d(TAG,"getFavorites");
        if(mLastCursorRetrieved != null) {
            Log.d(TAG,"mLasCursorRetrieved" + mLastCursorRetrieved);
            mLastCursorRetrieved.moveToFirst();
            return mLastCursorRetrieved;
        }
        Cursor cursor;
        SQLiteDatabase db = getDb(false);
        cursor = TrackerDatabaseHelper.getFilteredRecords(db, mFilter);
        cursor.moveToFirst();
        db.close();
        mLastCursorRetrieved = cursor;
        return cursor;
    }

    private Cursor getLastFavorite() {
        Log.d(TAG,"getLastFavorite");
        Cursor cursor;
        cursor = getFavorites();
        return cursor;
    }

    private Location getLastFavoriteLocation() {
        Log.d(TAG,"getLastFavoriteLocation");

        Location lastFavoriteLocation = new Location("Dummyprovider");
        Cursor cursor = getLastFavorite();
        if(cursor.getCount() > 0) {
            lastFavoriteLocation.setLatitude(cursor.getDouble(1));
            lastFavoriteLocation.setLongitude(cursor.getDouble(2));
        } else lastFavoriteLocation = null;
        return lastFavoriteLocation;
    }

    private Cursor getFavorite(int key_id) {
        Log.d(TAG,"getFavorite");

        Cursor cursor;
        SQLiteDatabase db = getDb(false);
        cursor = TrackerDatabaseHelper.getOneRecord(db, key_id);
        cursor.moveToFirst();
        db.close();
        return cursor;
    }

    // Run storeToDatabase on its own thread.
    private class UpdateAddress extends AsyncTask<Void, Void, Boolean> {
        protected void onPreExecute() {
            Log.d(TAG,"UpdateAddress::onPreExecute");

        }

        protected Boolean doInBackground(Void... params ) {
            Log.d(TAG,"UpdateAddress::doInBackgound");
            SQLiteDatabase db = getDb(true);
            Cursor cursor = TrackerDatabaseHelper.getNotAvailableAddressRecords(db);
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                Location location = new Location("dummy");
                while (!cursor.isAfterLast()) {
                    Log.d(TAG,"Update address");
                    location.setLatitude(cursor.getDouble(1));
                    location.setLongitude(cursor.getDouble(2));
                    String address = TrackerUtilities.getAddress(location, MainActivity.this);
                    TrackerDatabaseHelper.setFavoriteAddress(db,cursor.getInt(0), address);
                    cursor.moveToNext();
                }
                cursor.close();
            }
            db.close();
            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.d(TAG,"UpdateAddress::onPostExecute");

            if(!result) {
                Log.d(TAG,"onPostExecute returned false");
            }
        }
    }
    /* #################################################################################### */

    void updateAppVersion(){
        Log.d(TAG, "updateAppVersion");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        String app_ver = "";
        try {
            app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e)
        {
            Log.e(TAG, e.getMessage());
        }
        Log.d(TAG, "Appversion is:" + app_ver);

        editor.putString(SettingsActivity.APP_VERSION, app_ver);
        editor.commit();

        //EditTextPreference prefCat=(EditTextPreference) preferences.findPreference("myPreferencesTitle");
        //prefCat.setTitle("My New Title");
    }


    public void onSwitchTrackerServiceClicked(View view) {
        Log.d(TAG,"onSwitchTrackerServiceClicked");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        cFavoriteUpdate = Integer.parseInt(sharedPref.getString(SettingsActivity.FAVORITE_UPDATE,"30")) * 1000;
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            startTrackerService();
            doBindService();
        } else {
            stopTrackerService();
            doUnbindService();
        }
    }

    private void startTrackerService() {
        Log.d(TAG, "startTrackerService");
        Intent serviceIntent = new Intent(this, TrackerService.class);
        startService(serviceIntent);
    }

    private void stopTrackerService() {
        Log.d(TAG, "stopTrackerService");
        Intent serviceIntent = new Intent(this, TrackerService.class);
        stopService(serviceIntent);
    }

    public void saveCurrentPosition(View view) {
        Log.d(TAG,"saveCurrentPosition");

        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            TrackerService.MSG_SAVE_CURRENT);
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
                // Make sure cursor is refetched.
                mLastCursorRetrieved = null;
            }
        } else {
            TrackerUtilities.displayToast(true,this,getString(R.string.service_not_started));
        }
    }

    public void clearFavorites(View view) {
        Log.d(TAG, "clearFavorites");
        DialogHandler appDialog = new DialogHandler();
        appDialog.Confirm(this, getString(R.string.confirm_delete), getString(R.string.confirm_delete_message),
                getString(R.string.no), getString(R.string.yes), deleteFavorites(), noDeleteFavorites());
        updateMainLayout();
    }

    private Runnable deleteFavorites(){
        return new Runnable() {
            public void run() {
                Log.d(TAG, "Delete all favorites");
                SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(MainActivity.this);
                SQLiteDatabase db = trackerDatabaseHelper.getReadableDatabase();
                db.execSQL("delete from " + TrackerDatabaseHelper.TRACKER);
                db.close();
            }
        };
    }

    private Runnable noDeleteFavorites(){
        return new Runnable() {
            public void run() {
                Log.d(TAG, "No delete of favoritesT6his from B proc");
                TrackerUtilities.displayToast(true, MainActivity.this, getString(R.string.no_favorites_deleted));
            }
        };
    }

    /** ########################################################################################
     *  ###################### Presentation on main layout. ####################################
     *  ########################################################################################
     */
    public void updateMainLayout() {
        Log.d(TAG,"updateMainLayout");
        if(mFilter.getTodayOnly())
            mFilter.setMillis(TrackerUtilities.getStartOfToday());
        displayLastFavorite();
        //displayFavoritesOnMap();
    }

    private void displayLastFavorite() {
        Log.d(TAG, "displayLastFavorite");
        Cursor lastFavorite = getLastFavorite();
        TextView lf = (TextView) findViewById(R.id.last_favorite);
        TextView lfTime = (TextView) findViewById(R.id.last_favorite_time);
        ImageButton cl_button = (ImageButton) findViewById(R.id.button_last_favorite);

        if(lastFavorite.getCount() > 0) {
            String headLine;
            if(lastFavorite.getString(7).isEmpty()) {
                headLine = lastFavorite.getString(5);
            } else {
                headLine = lastFavorite.getString(7);
            }
            lf.setText(headLine);
            lfTime.setText(TrackerUtilities.getDateTime(lastFavorite.getLong(3)) + "; " +
                    TrackerUtilities.formatSpentTime(lastFavorite.getInt(4) / 1000));
            cl_button.setImageResource(FavoriteCategory.getCategoryImage(lastFavorite.getInt(6)));
            //lastFavorite.close();
        } else {
            lf.setText(getString(R.string.noFavoriteFound));
            lfTime.setText("");
            cl_button.setImageResource(R.drawable.ic_favorite);
        }
    }

    private void updateStatusField(int state) {
        Log.d(TAG,"updateStatusField");

        ImageButton myImageView = (ImageButton) findViewById(R.id.current_location_button);
        Animation myFadeInAnimation;
        switch (state) {
            case FavoriteHandler.INIT:
                // This will probably never happen
                myImageView.setBackgroundResource(R.drawable.ic_status_init);
                myImageView.clearAnimation();
                break;
            case FavoriteHandler.INSIDE_AREA:
                myImageView.setBackgroundResource(R.drawable.ic_status_inside_area);
                myFadeInAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.status);
                myImageView.startAnimation(myFadeInAnimation);
                break;
            case FavoriteHandler.OUTSIDE_AREA:
                myImageView.setBackgroundResource(R.drawable.ic_status_outside_area);
                myFadeInAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.status);
                myImageView.startAnimation(myFadeInAnimation);
                break;
            case FavoriteHandler.FAVORITE_FOUND:
                myImageView.setBackgroundResource(R.drawable.ic_status_favorite_found);
                myImageView.clearAnimation();
                break;
            default:
        }
    }

    Bitmap getBitMap(int resource) {
        //Log.d(TAG,"getBitMap");

        int px = getResources().getDimensionPixelSize(R.dimen.map_dot_marker_size);
        Bitmap bitMap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitMap);
        Drawable shape = getResources().getDrawable(resource);
        shape.setBounds(0, 0, bitMap.getWidth(), bitMap.getHeight());
        shape.draw(canvas);
        return bitMap;
    }


    private void addMarker(LatLng pos, int id, int category) {
        FavoriteMarker marker = new FavoriteMarker(pos.latitude, pos.longitude);
        marker.setId(id);
        marker.setCategory(category);
        mClusterManager.addItem(marker);
    }

    private void displayFavoritesOnMap() {
        Log.d(TAG, "displayFavoritesOnMap");
        //mCurrentMap.clear();
        // mapDbIdToMarker.clear();
        Cursor cursor = getFavorites();

       // LatLngBounds bounds = mCurrentMap.getProjection().getVisibleRegion().latLngBounds;

        if (cursor.getCount() > 0) {
            while (!cursor.isAfterLast()) {
                LatLng pos = new LatLng(cursor.getDouble(1),
                        cursor.getDouble(2));
                addMarker(pos,cursor.getInt(0),cursor.getInt(6));
                cursor.moveToNext();
            }
        }

        //cursor.close();
        Location loc = TrackerService.mCurrentLocation;
        if (loc != null) {
            Log.d(TAG, "displayFavoritesOnMap::currentLocation" + TrackerUtilities.getAddress(loc, this));
            Log.d(TAG,"mCurrentMap:" + mCurrentMap);
            Marker m = mCurrentMap.addMarker(new MarkerOptions()
                    .position(new LatLng(loc.getLatitude(), loc.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromBitmap(getBitMap(R.drawable.ic_current_location)))
                    .title(getString(R.string.current_position))
                    .snippet(TrackerUtilities.getAddress(loc,this)));
        }
    }

    /** ################################################################################
     *  ################### Button callbacks ###########################################
     *  ################################################################################
     */
    public void onClickFilter(View view) {
        Log.d(TAG, "onClickFilter");
        Intent filterIntent = new Intent(this, FilterActivity.class);
        startActivity(filterIntent);
    }


    public void onClicklistFavorites(View view) {
        Log.d(TAG, "onClicklistFavorites");
        Intent listFavoritesIntent = new Intent(this, ListFavoritesActivity.class);
        startActivity(listFavoritesIntent);
    }

    public void onClickLastFavorite(View view) {
        Log.d(TAG, "onClickLastFavorite");
        moveToLastFavorite(mDefaultZoomLevel);
    }

    private void moveToLastFavorite(float zoomLevel) {
        Cursor lastFavorite = getLastFavorite();
        if(lastFavorite.getCount() > 0) {
            mCurrentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastFavorite.getDouble(1),
                    lastFavorite.getDouble(2)), zoomLevel));
        }
    }
    public void onClickCurrentPosition(View view) {
        Log.d(TAG,"onClickCurrentPosition");

        moveToCurrentPosition(mDefaultZoomLevel);
    }

    private void moveToCurrentPosition(float zoomLevel) {
        Location currentLocation = TrackerService.mCurrentLocation;
        if(currentLocation != null) {
            Log.d(TAG,"onClickCurrentPosition::currentLocation" + TrackerUtilities.getAddress(currentLocation,this));
            mCurrentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()), zoomLevel));
        }
    }
}
