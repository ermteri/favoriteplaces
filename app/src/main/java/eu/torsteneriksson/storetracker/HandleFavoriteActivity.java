package eu.torsteneriksson.storetracker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

//import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class HandleFavoriteActivity extends Activity implements OnMapReadyCallback {
    int mKeyId;
    GoogleMap mCurrentMap;
    private static final String TAG = "HandleFavoriteActivity";
    ListView mListPlaces;
    FavoritePlace mFavoritePlace;

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        mCurrentMap = map;
        mCurrentMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        updateEditFavoriteView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_favorite);
        mListPlaces = (ListView) findViewById(R.id.list_places);
        // Get the selected favorite
        mKeyId = getIntent().getIntExtra("EXTRA_KEYID",0);
        Log.d(TAG,"EXTRA_KEYID:" + mKeyId);
        ((MapFragment) getFragmentManager().findFragmentById(R.id.handle_favorite_map))
                .getMapAsync(this);
        ImageButton category = (ImageButton) findViewById(R.id.favorite_category);
        category.requestFocus();
        setListViewListener();
        // updateEditFavoriteView();
        // Also start a listener to the description field in case the user change it.
        EditText description = (EditText) findViewById(R.id.favorite_description);
        //description.setHorizontallyScrolling(true);
        description.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Update the database now.
                    Log.d(TAG, "Description updated" + v.getText().toString());
                    SQLiteDatabase db = getDb();
                    TrackerDatabaseHelper.setFavoriteDescription(db, (int) mKeyId, v.getText().toString());
                    db.close();
                    //handled = true;
                }
                updateEditFavoriteView();
                return handled;
            }
        });

/*        LinearLayout categoryButton = (LinearLayout) findViewById(R.id.favorite_summary);
        categoryButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO Auto-generated method stub
                clearFavorite();
                return true;}
        });*/
    }

    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        //updateEditFavoriteView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d(TAG, "onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        OptionsMenuHandler omh = new OptionsMenuHandler();
        if (item.getItemId() == R.id.action_share) {
            shareFavorite();
            return (true);
        } else {
            return (omh.handleOptionsSelected(this, item));
        }
    }

    private void setListViewListener() {
        mListPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
                mFavoritePlace = (FavoritePlace) listView.getItemAtPosition(position);
                askReplacePosition();
            }
        });
    }

    private void navigate(String mode) {
        SQLiteDatabase db = getDb();
        Cursor cursor = getCursor(db);
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + cursor.getDouble(1) + "," + cursor.getDouble(2) + "&mode=" + mode);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
        cursor.close();
        db.close();
    }

    public void onClickNavigateByWalk(View view) {
        navigate("w");
    }

    public void onClickNavigateByBike(View view) {
        navigate("b");
    }

    public void onClickNavigateByCar(View view) {
        navigate("c");
    }

    public void onClickNavigateByBus(View view) {
        navigate("r");
    }

    public void onClickShowMap(View view) {
        Intent showOnMapIntent = new Intent(this,MapsActivity.class);
        showOnMapIntent.putExtra(TrackerDatabaseHelper.KEY_ID,mKeyId);
        startActivity(showOnMapIntent);
    }

    public void onClickSearchFavorites(View view) {

        SQLiteDatabase db = getDb();
        Cursor cursor = getCursor(db);
        Places p = new Places(this);
        EditText radiusField = (EditText) findViewById(R.id.radius);
        String radius = radiusField.getText().toString();
        p.searchPlaces(this, mListPlaces, cursor.getDouble(1), cursor.getDouble(2), radius);
        cursor.close();
        db.close();
    }

    public void onClickChangeCategory(View view) {
        SQLiteDatabase db = getDb();
        Cursor cursor = getCursor(db);
        TrackerUtilities.setCategoryDialog(this, (int) cursor.getLong(0));
        cursor.close();
        db.close();
    }

    void shareFavorite() {
        Cursor cursor;
        TrackerDatabaseHelper trackerDatabaseHelper = new TrackerDatabaseHelper(this);
        SQLiteDatabase db = trackerDatabaseHelper.getReadableDatabase();
        cursor=TrackerDatabaseHelper.getOneRecord(db,(int)mKeyId);

        String message = "";
        cursor.moveToFirst();
        message+=String.valueOf(cursor.getString(7))+"\n"+
                String.valueOf(cursor.getString(5))+"\n"+
                String.valueOf(TrackerUtilities.getDateTime(cursor.getLong(3)))+", "+
                String.valueOf(TrackerUtilities.formatSpentTime(cursor.getInt(4)/1000))+"\n"+
                "http://maps.google.com/maps?q="+
                String.valueOf(cursor.getDouble(1))+","+
                String.valueOf(cursor.getDouble(2))+"\n\n";
        cursor.close();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,message);
        intent.putExtra(Intent.EXTRA_SUBJECT,

                getString(R.string.share_subject)

        );

        startActivity(intent);

    }
    public void onClickDeleteFavorite(View view) {
        clearFavorite();
    }

    public void closeActivity() {
        this.finish();
    }

    private Cursor getCursor(SQLiteDatabase db) {
        Cursor cursor = TrackerDatabaseHelper.getOneRecord(db, (int) mKeyId);
        if(cursor.getCount() == 0) {
            Log.d(TAG,"getCount==0");
            this.closeActivity();
            return null;
        }
        cursor.moveToFirst();
        return cursor;
    }

    private SQLiteDatabase getDb() {
        SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(this);
        return trackerDatabaseHelper.getReadableDatabase();
    }

    public void updateEditFavoriteView() {
        SQLiteDatabase db = getDb();
        Cursor cursor = getCursor(db);
        if(cursor==null)
            return;
        ImageButton categoryIcon = (ImageButton) findViewById(R.id.favorite_category);
        categoryIcon.setImageResource(FavoriteCategory.getCategoryImage(cursor.getInt(6)));

        EditText description = (EditText) findViewById(R.id.favorite_description);
        description.setText(cursor.getString(7));

        TextView address = (TextView) findViewById(R.id.favorite_address);
        address.setText(" " + cursor.getString(5));

        TextView latitude = (TextView) findViewById(R.id.favorite_latitude);
        latitude.setText(String.format( " Lat: %.4f, ", cursor.getDouble(1)));

        TextView longitude = (TextView) findViewById(R.id.favorite_longitude);
        longitude.setText(String.format( " Lng: %.4f", cursor.getDouble(2)));

        TextView dateTime = (TextView) findViewById(R.id.favorite_datetime);
        dateTime.setText(String.valueOf(" " + TrackerUtilities.getDateTime(cursor.getLong(3)) + " "));

        TextView timeSpent = (TextView) findViewById(R.id.favorite_timespent);
        timeSpent.setText(String.valueOf(" " + TrackerUtilities.formatSpentTime(cursor.getInt(4) / 1000)));
        displayFavoriteOnMap(cursor);
        cursor.close();
        db.close();

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

    private void displayFavoriteOnMap(Cursor cursor) {
        Log.d(TAG, "displayFavoriteOnMap");

        Log.d(TAG,"category:"+cursor.getInt(6));
        Log.d(TAG,"mCurrentMap:"+ mCurrentMap);
        mCurrentMap.clear();
        LatLng position = new LatLng(
                cursor.getDouble(1),
                cursor.getDouble(2));
        Marker m =
                mCurrentMap.addMarker(new MarkerOptions()
                        .position(position)
                        //.icon(BitmapDescriptorFactory
                        //        .fromResource(FavoriteCategory.getCategoryImage(cursor.getInt(6))))
                        .icon(BitmapDescriptorFactory.fromBitmap(getBitMap(FavoriteCategory.getCategoryImage(cursor.getInt(6)))))
                        .title(cursor.getString(7))
                        .snippet(TrackerUtilities.getDateTime(cursor.getLong(3)) + ", " +
                                TrackerUtilities.formatSpentTime(cursor.getInt(4) / 1000)));
        //mCurrentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position,14));
        mCurrentMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position,15));
    }


    private void clearFavorite() {
        Log.d(TAG, "clearFavorite");
        DialogHandler appDialog = new DialogHandler();
        appDialog.Confirm(this, getString(R.string.confirm_delete_one), getString(R.string.confirm_delete_one_message),
                getString(R.string.no), getString(R.string.yes), deleteFavorite(), noDeleteFavorites());

    }

    private Runnable deleteFavorite(){
        Log.d(TAG, "deleteFavorite");
        Runnable rb = new Runnable() {
            public void run() {
                Log.d(TAG, "Delete favorite");
                SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(HandleFavoriteActivity.this);
                SQLiteDatabase db = trackerDatabaseHelper.getReadableDatabase();
                db.execSQL(String.format("DELETE FROM %s WHERE %s = %d",
                        TrackerDatabaseHelper.TRACKER, TrackerDatabaseHelper.KEY_ID, mKeyId));
                db.close();
                closeActivity();
            }
        };
        return rb;
    }

    private Runnable noDeleteFavorites(){
        return new Runnable() {
            public void run() {
                Log.d(TAG, "No delete of favorites");
                TrackerUtilities.displayToast(true, HandleFavoriteActivity.this, getString(R.string.no_favorites_deleted));
            }
        };
    }

    private void askReplacePosition() {
        Log.d(TAG, "askReplacePosition");
        DialogHandler appDialog = new DialogHandler();
        appDialog.Confirm(this, getString(R.string.confirm_replace_position), getString(R.string.confirm_replace_position_message),
                getString(R.string.no), getString(R.string.yes),replacePosition(), noReplacePosition());

    }

    private Runnable replacePosition(){
        Log.d(TAG, "deleteFavorite");
        Runnable rb = new Runnable() {
            public void run() {
                Log.d(TAG, "Update favorite");
                EditText description = (EditText) findViewById(R.id.favorite_description);
                description.setText(mFavoritePlace.getName().toString());
                TextView address = (TextView) findViewById(R.id.favorite_address);
                address.setText(mFavoritePlace.getAddress());
                TextView latitude = (TextView) findViewById(R.id.favorite_latitude);
                latitude.setText(String.format( " Lat: %.4f, ", mFavoritePlace.getLat()));
                TextView longitude = (TextView) findViewById(R.id.favorite_longitude);
                longitude.setText(String.format( " Lat: %.4f, ", mFavoritePlace.getLng()));
                SQLiteDatabase db = getDb();

                TrackerDatabaseHelper.setFavoriteDescription(db, (int) mKeyId, mFavoritePlace.getName().toString());
                TrackerDatabaseHelper.setFavoriteAddress(db, (int) mKeyId, mFavoritePlace.getAddress().toString());
                TrackerDatabaseHelper.setFavoriteLatitude(db, (int) mKeyId, mFavoritePlace.getLat());
                TrackerDatabaseHelper.setFavoriteLongitude(db, (int) mKeyId, mFavoritePlace.getLng());
                Cursor cursor = TrackerDatabaseHelper.getOneRecord(db,(int) mKeyId);
                cursor.moveToFirst();
                displayFavoriteOnMap(cursor);
                cursor.close();
                db.close();
                TrackerUtilities.displayToast(true, HandleFavoriteActivity.this, getString(R.string.position_replaced));            }
        };
        return rb;
    }

    private Runnable noReplacePosition(){
        return new Runnable() {
            public void run() {
                Log.d(TAG, "No update of favorites");
                TrackerUtilities.displayToast(true, HandleFavoriteActivity.this, getString(R.string.position_not_replaced));
            }
        };
    }


}