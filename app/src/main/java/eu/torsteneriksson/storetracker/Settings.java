package eu.torsteneriksson.storetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by torsten on 2015-09-05.
 */
public class Settings {
    private SharedPreferences mSharedPref = null;
    Settings(Context context){
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getFavoriteUpdate() {
        return Integer.parseInt(mSharedPref.getString(SettingsActivity.FAVORITE_UPDATE,"30")) * 1000;
    }

    public int getFavoriteArea() {
        return Integer.parseInt(mSharedPref.getString(SettingsActivity.TRACKING_DISTANCE, "30"));
    }

    public boolean getShowLocationData() {
        return mSharedPref.getBoolean(SettingsActivity.SHOW_LOCATION_DATA,false);
    }

}
