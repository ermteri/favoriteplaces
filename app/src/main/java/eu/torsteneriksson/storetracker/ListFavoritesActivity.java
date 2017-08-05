package eu.torsteneriksson.storetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class ListFavoritesActivity extends Activity
{
    private long mSelectedDateInMillis;
    private static final String TAG = "ListFavoritesActivity";
    private int mIdToDelete = -1;
    private ListView mListfavorites;
    private int mCurrentListPosition = 0;
    private Cursor mFavoritesCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_favorites);
        mSelectedDateInMillis = MainActivity.getFilter().getMillis();

        mListfavorites = (ListView)findViewById(R.id.list_favorites);
        updateListView();

        //Navigate to MapsActivity if a drink is clicked
        final Context context = this;
        mListfavorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View v, int position, long id) {

                //TrackerUtilities.setCategoryDialog(ListFavoritesActivity.this, (int) id);
                Intent editFavoritesIntent = new Intent(context,HandleFavoriteActivity.class);
                editFavoritesIntent.putExtra("EXTRA_KEYID", (int) id);
                startActivity(editFavoritesIntent);
                mCurrentListPosition = listView.getFirstVisiblePosition();
                updateListView();
            }
        });

        mListfavorites.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long id) {
                mIdToDelete =  (int) id;
                clearFavorite();
                mCurrentListPosition = position;
                return true;
            }
        });
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
        return (omh.handleOptionsSelected(this, item));
    }

    @Override
    protected void onResume(){
        Log.d(TAG, "onResume");
        super.onResume();
        updateListView();
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy");
        mFavoritesCursor.close();
        super.onDestroy();
    }

    public void updateListView(){
        Log.d(TAG, "updateListView");
        TrackerDatabaseHelper trackerDatabaseHelper = new TrackerDatabaseHelper(ListFavoritesActivity.this);
        SQLiteDatabase db = trackerDatabaseHelper.getReadableDatabase();
        try {

            mFavoritesCursor = TrackerDatabaseHelper.getFilteredRecords(db,MainActivity.getFilter());

            int cursorSize = mFavoritesCursor.getCount();
            TextView title = (TextView) findViewById(R.id.list_favorites_title);
            if(mFavoritesCursor.getCount() == 0) {
                title.setText(getString(R.string.noFavoritesFound));
            } else {
                title.setText(getString(R.string.position_label)+ " " +String.valueOf(cursorSize));
            }

            BaseAdapter favoriteAdapter =
                    new ImageCursorAdapter(ListFavoritesActivity.this,
                            android.R.layout.two_line_list_item,
                            mFavoritesCursor,
                            new String[]{"DESCRIPTION","ADDRESS","TIME"},
                            new int[]{android.R.id.text1,android.R.id.text2});
            mListfavorites.setAdapter(favoriteAdapter);
            Log.d(TAG,"scroll to position:" + mCurrentListPosition);
            mListfavorites.setSelection(mCurrentListPosition);
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }
        db.close();

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
                SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(ListFavoritesActivity.this);
                SQLiteDatabase db = trackerDatabaseHelper.getReadableDatabase();
                db.execSQL(String.format("DELETE FROM %s WHERE %s = %d",
                        TrackerDatabaseHelper.TRACKER, TrackerDatabaseHelper.KEY_ID, mIdToDelete));
                db.close();
                updateListView();
            }
        };
        return rb;
    }

    private Runnable noDeleteFavorites(){
        return new Runnable() {
            public void run() {
                Log.d(TAG, "No delete of favoritesT6his from B proc");
                TrackerUtilities.displayToast(true, ListFavoritesActivity.this, getString(R.string.no_favorites_deleted));
            }
        };
    }
}
