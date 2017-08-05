package eu.torsteneriksson.storetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by torsten on 2016-05-14.
 */
public class OptionsMenuHandler {
    private static final String TAG = "OptionsMenyHandler";

    public boolean handleOptionsSelected(Activity activity, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.action_share:
                return shareFavorites(activity);
            case R.id.action_help:
                intent = new Intent(activity, HelpActivity.class);
                activity.startActivity(intent);
                return true;
            /*
            case R.id.action_import_export:
                backupDb();
                Intent intent = new Intent(this, ExportImportActivity.class);
                startActivity(intent);
                return true;
                */
        }
        return activity.onOptionsItemSelected(item);
    }

    private boolean shareFavorites(Activity activity) {
        //http://maps.google.com/maps?q=24.197611,120.780512
        Log.d(TAG,"shareFavorites");
        Cursor cursor;
        TrackerDatabaseHelper tdh = new TrackerDatabaseHelper(activity);
        SQLiteDatabase db = tdh.getReadableDatabase();
        cursor = tdh.getFilteredRecords(db,MainActivity.getFilter());
        String message = "";
        cursor.moveToFirst();
        if(cursor.getCount() > 0) {
            int count = 0;
            while (!cursor.isAfterLast()) {
                message += String.valueOf(cursor.getString(7)) + "\n" +
                        String.valueOf(cursor.getString(5)) + "\n" +
                        String.valueOf(TrackerUtilities.getDateTime(cursor.getLong(3))) + ", " +
                        String.valueOf(TrackerUtilities.formatSpentTime(cursor.getInt(4) / 1000)) + "\n" +
                        "http://maps.google.com/maps?q=" +
                        String.valueOf(cursor.getDouble(1)) + "," +
                        String.valueOf(cursor.getDouble(2)) + "\n\n";
                cursor.moveToNext();
                if(++count > 1000) {
                    TrackerUtilities.displayToast(true,activity,activity.getString(R.string.not_all_shared));
                    break;
                }
            }
        } else {
            message = activity.getString(R.string.noFavoritesFound);
        }
        cursor.close();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_subject));

        activity.startActivity(intent);
        return true;
    }
}

