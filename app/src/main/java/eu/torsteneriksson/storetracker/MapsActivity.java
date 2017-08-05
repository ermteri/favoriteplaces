package eu.torsteneriksson.storetracker;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final static String TAG = "MapsActivity";
    private SQLiteDatabase mDb;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private long mSelectedDateInMillis = 0;
    private int mSelectedFavorite = 0;
    @Override
    public void onMapReady(GoogleMap map) {

        mMap = map;
        setUpMap();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        Intent intent = getIntent();
        mSelectedFavorite = intent.getIntExtra(TrackerDatabaseHelper.KEY_ID,0);
        mSelectedDateInMillis = intent.getLongExtra("SELECTED_DATE", 0);
        if(mSelectedDateInMillis == 0) {
            setTitle(R.string.title_activity_maps);
        } else {
            setTitle(getString(R.string.title_activity_maps) + ", " +
                    TrackerUtilities.getDate(mSelectedDateInMillis));
        }
        super.onCreate(savedInstanceState);
        SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(this);
        mDb = trackerDatabaseHelper.getReadableDatabase();
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG,"onDestroy");
        mDb.close();
        super.onDestroy();
    }
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        Log.d(TAG, "setUpMapIfNeeded");
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment)
                    getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

            // Check if we were successful in obtaining the map.
        }
    }

    Bitmap getBitMap(int resource) {
        int px = getResources().getDimensionPixelSize(R.dimen.map_dot_marker_size);
        Bitmap bitMap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitMap);
        Drawable shape = getResources().getDrawable(resource);
        shape.setBounds(0, 0, bitMap.getWidth(), bitMap.getHeight());
        shape.draw(canvas);
        return bitMap;
    }
    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Log.d(TAG, "setUpMap");
        Cursor cursor;

        if(mSelectedFavorite == 0) {
            if (mSelectedDateInMillis == 0)
                cursor = TrackerDatabaseHelper.getAllRecords(mDb);
            else
                cursor = TrackerDatabaseHelper.getSelectedDateRecords(mDb, mSelectedDateInMillis);
        } else {
            cursor = TrackerDatabaseHelper.getOneRecord(mDb,mSelectedFavorite);
        }
        if (cursor.getCount() == 0) {
            TrackerUtilities.displayToast(true,this, getString(R.string.noFavoritesFound));
        } else {
            cursor.moveToLast();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(cursor.getDouble(1),
                    cursor.getDouble(2)), 14.0f));
            while (!cursor.isBeforeFirst()) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(cursor.getDouble(1),
                                cursor.getDouble(2)))
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(getBitMap(FavoriteCategory.getCategoryImage(cursor.getInt(6)))))
                        .title(cursor.getString(7))
                        .snippet(TrackerUtilities.getDateTime(cursor.getLong(3)) + ", " +
                                TrackerUtilities.formatSpentTime(cursor.getInt(4) / 1000)));
                cursor.moveToPrevious();
                //Log.d(TAG, pos);
            }
        }
        cursor.close();
    }
}
