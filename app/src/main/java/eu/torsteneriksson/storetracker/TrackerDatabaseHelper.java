package eu.torsteneriksson.storetracker;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by torsten on 2015-08-10.
 */
public class TrackerDatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "tracker";
    private static final int DB_VERSION = 10;
    public static final String TRACKER = "TRACKER";
    public static final String KEY_ID = "_id";
    public static final String LATITUDE= "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String TIME = "TIME";
    public static final String TIME_SPENT = "TIME_SPENT";
    public static final String ADDRESS = "ADDRESS";
    public static final String CATEGORY = "CATEGORY";
    public static final String DESCRIPTION = "DESCRIPTION";
    private static final String PREFS_NAME = "MyPrefsFile";
    private static String TAG = "TrackerDatabaseHelper";

    public TrackerDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDatabase(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDatabase(db,oldVersion, newVersion);
    }

    private static void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 1) {
            db.execSQL("CREATE TABLE "+ TRACKER + "("
                    + KEY_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + LATITUDE    + " DOUBLE, "
                    + LONGITUDE   + " DOUBLE, "
                    + TIME        + " INTEGER, "
                    + TIME_SPENT  + " INTEGER, "
                    + ADDRESS     + " TEXT, "
                    + CATEGORY    + " INTEGER DEFAULT 0, "
                    + DESCRIPTION + " STRING);");
        } else if (oldVersion < 10) {
            db.execSQL("ALTER TABLE " + TRACKER +
                    " ADD COLUMN " + CATEGORY + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TRACKER +
                    " ADD COLUMN " + DESCRIPTION + " STRING");
        }
    }

    public static Cursor getAllRecords(SQLiteDatabase db) {
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE,
                        TrackerDatabaseHelper.TIME,
                        TrackerDatabaseHelper.TIME_SPENT,
                        TrackerDatabaseHelper.ADDRESS,
                        TrackerDatabaseHelper.CATEGORY,
                        TrackerDatabaseHelper.DESCRIPTION},
                        null, null, null, null,
                        TrackerDatabaseHelper.KEY_ID + " DESC", null);
    }
    /**
     * Search the database for records which time field matches the millis argument.
     * Returns a Cursor with the selected records.
     */
    public static Cursor getSelectedDateRecords(SQLiteDatabase db, long millis) {
        String whereClause = TrackerDatabaseHelper.TIME+" > ? AND "+TrackerDatabaseHelper.TIME + " < ?";
        String[] whereArgs = new String [] {String.valueOf(millis),String.valueOf(millis + 86400000)};
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE,
                        TrackerDatabaseHelper.TIME,
                        TrackerDatabaseHelper.TIME_SPENT,
                        TrackerDatabaseHelper.ADDRESS,
                        TrackerDatabaseHelper.CATEGORY,
                        TrackerDatabaseHelper.DESCRIPTION},
                        whereClause, whereArgs, null,
                        null, TrackerDatabaseHelper.KEY_ID + " DESC", null);
    }

    /**
     * Search the database for records which time field matches the millis argument.
     * Returns a Cursor with the selected records.
     */
    public static Cursor getFilteredRecords(SQLiteDatabase db, FavoriteFilter filter) {
        String whereClause = "";
        List<String> whereArgslist = new ArrayList<String>();
        boolean clauseFound = false;

        if(filter.getMillis() > 0){
            whereClause = TrackerDatabaseHelper.TIME + " > ? AND " + TrackerDatabaseHelper.TIME + " < ?";
            whereArgslist.add(String.valueOf(filter.getMillis()));
            whereArgslist.add(String.valueOf(filter.getMillis() + 86400000));
            clauseFound = true;
        }

        if(filter.getCategory() > -1) {
            // Filter on date and category
            whereClause += clauseFound?" AND ":"";
            whereClause += TrackerDatabaseHelper.CATEGORY + " IN (?)";
            whereArgslist.add(String.valueOf(filter.getCategory()));
            clauseFound = true;
        }
        if(filter.getSearchText() != "") {
            whereClause += clauseFound?" AND ":"";
            whereClause += "(" + TrackerDatabaseHelper.DESCRIPTION + " LIKE ? OR " + TrackerDatabaseHelper.ADDRESS + " LIKE ?)";
            whereArgslist.add("%"+filter.getSearchText()+"%");
            whereArgslist.add("%"+filter.getSearchText()+"%");
        }
            // no filter was active.
        if(whereArgslist.isEmpty()) {
            return getAllRecords(db);
        }
        String[] whereArgs = whereArgslist.toArray(new String[0]);

        Log.d(TAG,"whereClause:" + whereClause);
        for(int i=0;i<whereArgs.length;i++) {
            Log.d(TAG, "wherArgs:" + whereArgs[i]);
        }
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE,
                        TrackerDatabaseHelper.TIME,
                        TrackerDatabaseHelper.TIME_SPENT,
                        TrackerDatabaseHelper.ADDRESS,
                        TrackerDatabaseHelper.CATEGORY,
                        TrackerDatabaseHelper.DESCRIPTION},
                whereClause, whereArgs, null,
                null, TrackerDatabaseHelper.KEY_ID + " DESC", null);
    }

    /**
     * Search the database for records which time field matches the millis argument.
     * Returns a Cursor with the selected records.
     */
    public static Cursor getFilteredRecordsOrig(SQLiteDatabase db, FavoriteFilter filter) {
        // cursor = database.query(contentUri, projection, "columname IN(?,?)", new String[]{"value1" , "value2"}, sortOrder);
        //cursor = database.query(contentUri, projection, "columnName IN(?)", new String[] {" 'value1' , 'value2' "}, sortOrder)
        String whereClause = "";
        String[] whereArgs = new String[0];
        if(filter.getMillis() > 0 && filter.getCategory() > -1) {
            // Filter on date and category
            whereClause = TrackerDatabaseHelper.TIME + " > ? AND " + TrackerDatabaseHelper.TIME + " < ? AND " +
                    TrackerDatabaseHelper.CATEGORY + " IN (?)";
            whereArgs = new String[]{String.valueOf(filter.getMillis()), String.valueOf(filter.getMillis() + 86400000),
                    String.valueOf(filter.getCategory())};
        } else if (filter.getCategory() > -1) {
            // Filter on category only
            whereClause = TrackerDatabaseHelper.CATEGORY + " IN (?)";
            whereArgs = new String[] { String.valueOf(filter.getCategory())};

        } else if(filter.getMillis() > 0) {
            // Filter on time only
            whereClause = TrackerDatabaseHelper.TIME + " > ? AND " + TrackerDatabaseHelper.TIME + " < ?";
            whereArgs = new String[]{String.valueOf(filter.getMillis()), String.valueOf(filter.getMillis() + 86400000)};

        } else {
            // no filter was active.
            return getAllRecords(db);
        }
        Log.d(TAG,"whereClause:" + whereClause);
        for(int i=0;i<whereArgs.length;i++) {
            Log.d(TAG, "wherArgs:" + whereArgs[i]);
        }
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE,
                        TrackerDatabaseHelper.TIME,
                        TrackerDatabaseHelper.TIME_SPENT,
                        TrackerDatabaseHelper.ADDRESS,
                        TrackerDatabaseHelper.CATEGORY,
                        TrackerDatabaseHelper.DESCRIPTION},
                whereClause, whereArgs, null,
                null, TrackerDatabaseHelper.KEY_ID + " DESC", null);
    }

    /**
     * Returns a cursor with one record that matches the key_id.
     */
    public static Cursor getOneRecord(SQLiteDatabase db, int key_id) {
        String whereClause = TrackerDatabaseHelper.KEY_ID+" = ?";
        String[] whereArgs = new String [] {String.valueOf(key_id)};
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE,
                        TrackerDatabaseHelper.TIME,
                        TrackerDatabaseHelper.TIME_SPENT,
                        TrackerDatabaseHelper.ADDRESS,
                        TrackerDatabaseHelper.CATEGORY,
                        TrackerDatabaseHelper.DESCRIPTION},
                whereClause, whereArgs, null, null, null);
    }

    /**
     * Returns the most recent favorite.
     */
    public static Cursor getLastFavorite(SQLiteDatabase db) {
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE,
                        TrackerDatabaseHelper.TIME,
                        TrackerDatabaseHelper.TIME_SPENT,
                        TrackerDatabaseHelper.ADDRESS,
                        TrackerDatabaseHelper.CATEGORY,
                        TrackerDatabaseHelper.DESCRIPTION},
                        null, null, null, null,
                        TrackerDatabaseHelper.KEY_ID + " DESC", "1");
    }

    /**
     * Retrieves all records where the address field starts with "<" indicating
     * a not "Not available address" field.
     * @param db the database to look in
     * @return a cursor with the selected records
     */
    public static Cursor getNotAvailableAddressRecords(SQLiteDatabase db) {
        String whereClause = TrackerDatabaseHelper.ADDRESS + " LIKE ?";
        String[] whereArgs = new String[] {String.valueOf("<%")};
        Log.d(TAG,"whereClause:" + whereClause);
        return db.query(TrackerDatabaseHelper.TRACKER,
                new String[] {
                        TrackerDatabaseHelper.KEY_ID,
                        TrackerDatabaseHelper.LATITUDE,
                        TrackerDatabaseHelper.LONGITUDE},
                whereClause, whereArgs, null,
                null, null, null);
    }



    /**
     * Set the category of the provided record
     */
    public static void setFavoriteCategory(SQLiteDatabase db, int key_id, int category) {
        Log.d(TAG, "setFavoriteCategory");
        Log.d(TAG, "Category:" + String.valueOf(category));
        Log.d(TAG, "key_id:" + String.valueOf(key_id));
        ContentValues cv = new ContentValues();
        cv.put(CATEGORY, String.valueOf(category));
        db.update(TRACKER, cv, "_id = " + key_id, null);
    }

    /**
     * Set the address of the provided record
     */
    public static void setFavoriteAddress(SQLiteDatabase db, int key_id, String address) {
        Log.d(TAG, "setFavoriteAddress");
        Log.d(TAG, "Address:" + address);
        Log.d(TAG, "key_id:" + String.valueOf(key_id));
        ContentValues cv = new ContentValues();
        cv.put(ADDRESS, address);
        db.update(TRACKER, cv, "_id = " + key_id, null);
    }

    /**
     * Set the description of the provided record
     */
    public static void setFavoriteDescription(SQLiteDatabase db, int key_id, String description) {
        Log.d(TAG, "setFavoriteDescription");
        Log.d(TAG, "Description:" + description);
        Log.d(TAG, "key_id:" + String.valueOf(key_id));
        ContentValues cv = new ContentValues();
        cv.put(DESCRIPTION, description);
        db.update(TRACKER, cv, "_id = " + key_id, null);
    }
    /**
     * Set the latitude of the provided record
     */
    public static void setFavoriteLatitude(SQLiteDatabase db, int key_id, double latitude) {
        Log.d(TAG, "setFavoriteLatitude");
        Log.d(TAG, "Latitude:" + latitude);
        Log.d(TAG, "key_id:" + String.valueOf(key_id));
        ContentValues cv = new ContentValues();
        cv.put(LATITUDE, latitude);
        db.update(TRACKER, cv, "_id = " + key_id, null);
    }
    /**
     * Set the longitude of the provided record
     */
    public static void setFavoriteLongitude(SQLiteDatabase db, int key_id, double longitude) {
        Log.d(TAG, "setFavoriteLongitude");
        Log.d(TAG, "Longitude:" + longitude);
        Log.d(TAG, "key_id:" + String.valueOf(key_id));
        ContentValues cv = new ContentValues();
        cv.put(LONGITUDE, longitude);
        db.update(TRACKER, cv, "_id = " + key_id, null);
    }

    private SQLiteDatabase getDb(boolean writeable, Activity activity) {
        if(writeable) {
            return this.getWritableDatabase();
        } else {
            return this.getReadableDatabase();
        }
    }
    public Cursor getFavorites(Activity activity) {
        Log.d(TAG,"getFavorites");
        Cursor cursor;
        SQLiteDatabase db = getDb(false, activity);
        SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
        FavoriteFilter filter = new FavoriteFilter(
                settings.getLong("selectedDateInMillis",0),
                settings.getBoolean("todayOnly",false),
                settings.getInt("category",-1),
                settings.getString("searchText",""));
        cursor = TrackerDatabaseHelper.getFilteredRecords(db, filter);
        cursor.moveToFirst();
        db.close();
        return cursor;
    }

}
