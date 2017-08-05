package eu.torsteneriksson.storetracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by torsten on 2015-09-02.
 */
public class TrackerUtilities {
    private static final String TAG = "TrackerUtilities";
    private static Context mContext;
    private static int mKeyId;
    // Display a Toast message
    static public void displayToast(boolean show, Context context, String message) {
        if(show) {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    // Create a notification in the notification bar.
    static public void notify(Context context, String title, String text, long key_id) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.favorite_icon)
                        .setContentTitle(title)
                        .setContentText(String.valueOf(key_id) + ":" + text);
        // Sets an ID for the notification
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Add the activity that should start
        Intent intent = new Intent(context, HandleFavoriteActivity.class);
        intent.putExtra("EXTRA_KEYID", (int) key_id);
        PendingIntent contentIntent = PendingIntent.getActivity(context, (int)key_id,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setAutoCancel(true);
        mBuilder.setContentIntent(contentIntent);
        // Builds the notification and issues it.
        mNotifyMgr.notify((int) key_id, mBuilder.build());
        Log.d(TAG,"EXTRA_KEYID" + intent.getIntExtra("EXTRA_KEYID",0));
    }

    //
    public static String getAddress(Location location,Context context) {
        Log.d(TAG,"getAddress");
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        String address = "<"+ context.getString(R.string.notavailable) + ">" +
                String.format("\nLat: %.4f, Long:%.4f",latitude,longitude);
        if(isNetworkAvailable(context)) {
            Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());
            try {
                List<Address> listAddresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (null != listAddresses && listAddresses.size() > 0) {
                    address = listAddresses.get(0).getAddressLine(0) ;
                }
            } catch (IOException e) {
                e.printStackTrace();
                address = "<"+ context.getString(R.string.notavailable) + ">" +
                        String.format("\nLat: %.4f, Long:%.4f",latitude,longitude);
            }
        }
        Log.d(TAG,"address:" + address);
        return address;
    }

    public static boolean isSameDay(long time1,long time2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(new Date(time1));
        cal2.setTime(new Date(time2));
        boolean result = cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        Log.d(TAG,"getYearMonth returned:"+result);
        return result;
    }

    public static String getYearMonthString(long time) {
        Date date = new Date(time);
        String result = DateFormat.getDateInstance(DateFormat.YEAR_FIELD).format(date);
        Log.d(TAG,"getYearMonthString returned:"+ result);
        return result;
    }

    public static String getDateTime(long time) {
        Date date = new Date(time);
        return DateFormat.getDateTimeInstance().format(date);
    }

    public static String getDate(long time) {
        Date date = new Date(time);
        return DateFormat.getDateInstance().format(date);
    }

    public static int getFullDay(long time) {
        Date d  = new Date(time);
        SimpleDateFormat format =
                new SimpleDateFormat("yyyyMMdd");
        String result = format.format(d);
        Log.d(TAG,"getFullDay returned:" + result);
        return Integer.parseInt(result);
    }

    public static long getStartOfAnyDay(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));
        // Change the date so it will be the first second of that day.
        cal.set(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                0, 0, 0);
        Log.d(TAG,"getStartOfAnyDay returned:" + String.valueOf(cal.getTimeInMillis()));
        return cal.getTimeInMillis();
    }

    public static long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        // Change the date so it will be the first second of that day.
        cal.set(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                0, 0, 0);
        return cal.getTimeInMillis();
    }
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public static String formatSpentTime(int seconds) {
        String result;
        int hours = seconds /3600;
        seconds = seconds % 3600;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if(hours > 0) {
            result = String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if(minutes > 0) {
            result = String.format("%dm %ds", minutes, seconds);
        } else
            result = String.format("%ds", seconds);
        return result;
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void setCategoryDialog(final Activity context, int keyId) {
        mContext = context;
        mKeyId = keyId;
        final Item[] items = {
                new Item(mContext.getString(FavoriteCategory.getCategoryText(0)),
                        FavoriteCategory.getCategoryImage(0)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(1)),
                        FavoriteCategory.getCategoryImage(1)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(2)),
                        FavoriteCategory.getCategoryImage(2)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(3)),
                        FavoriteCategory.getCategoryImage(3)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(4)),
                        FavoriteCategory.getCategoryImage(4)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(5)),
                        FavoriteCategory.getCategoryImage(5)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(6)),
                        FavoriteCategory.getCategoryImage(6)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(7)),
                        FavoriteCategory.getCategoryImage(7)),
                new Item(mContext.getString(FavoriteCategory.getCategoryText(8)),
                        FavoriteCategory.getCategoryImage(8))

        };
        SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(mContext);
        SQLiteDatabase db = trackerDatabaseHelper.getReadableDatabase();
        Cursor cursor = TrackerDatabaseHelper.getOneRecord(db, (int) keyId);
        cursor.moveToFirst();
        String title = mContext.getString(R.string.select_category);
        cursor.close();
        db.close();

        ListAdapter adapter = new ArrayAdapter<Item>(
                mContext,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                items){
            public View getView(int position, View convertView, ViewGroup parent) {
                //Use super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

                //Add margin between image and text (support various screen densities)
                int dp5 = (int) (5 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                return v;
            }
        };

        new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int category) {
                        Log.d(TAG, "mKeyId:" + mKeyId);
                        SQLiteOpenHelper trackerDatabaseHelper = new TrackerDatabaseHelper(mContext);
                        SQLiteDatabase db = trackerDatabaseHelper.getWritableDatabase();
                        TrackerDatabaseHelper.setFavoriteCategory(db, (int) mKeyId, category);
                        db.close();
                        if (context instanceof MainActivity) {
                            MainActivity ma = (MainActivity) context;
                            ma.updateMainLayout();
                        } else if (context instanceof ListFavoritesActivity) {
                            ListFavoritesActivity la = (ListFavoritesActivity) context;
                            la.updateListView();
                        } else if (context instanceof HandleFavoriteActivity) {
                            HandleFavoriteActivity ea = (HandleFavoriteActivity) context;
                            ea.updateEditFavoriteView();
                        }
                    }
                }).show();
    }
    private static class Item{
        public final String text;
        public final int icon;
        public Item(String text, Integer icon) {
            this.text = text;
            this.icon = icon;
        }
        @Override
        public String toString() {
            return text;
        }
    }
}
