package eu.torsteneriksson.storetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.view.MenuItem;

public class SettingsActivity extends Activity {
    public final static String MAX_FAVORITES = "pref_key_max_favorites";
    public final static String MIN_TIME_IN_AREA = "pref_key_min_time_in_area";
    public final static String TRACKING_DISTANCE = "pref_key_tracking_distance";
    public final static String TRACKING_INTERVAL = "pref_key_tracking_interval";
    public final static String FAVORITE_UPDATE = "pref_key_favorite_update";
    public final static String MAX_HITS_OUTSIDE_AREA = "pref_key_max_hits_outside_area";
    public final static String MIN_HITS_INSIDE_AREA = "pref_key_min_hits_inside_area";
    public final static String SHOW_LOCATION_DATA = "pref_key_show_location_data";
    public final static String LOCATION_ACCURACY = "pref_key_location_accuracy";
    public final static String APP_VERSION = "pref_key_app_version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onDestroy() {
        notifySettingsUsers(this);
        super.onDestroy();
    }

    public static class SettingsFragment extends PreferenceFragment  {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    private static void notifySettingsUsers(Context context) {
        if (TrackerUtilities.isMyServiceRunning(TrackerService.class, context)) {
            Intent serviceIntent = new Intent(context, TrackerService.class);
            serviceIntent.putExtra("START", false);
            context.startService(serviceIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
}
