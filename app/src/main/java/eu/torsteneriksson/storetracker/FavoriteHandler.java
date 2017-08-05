package eu.torsteneriksson.storetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import java.security.PublicKey;

/**
 * Created by torsten on 2015-09-06.
 */
public class FavoriteHandler {

    private static final String TAG = "FavoriteHandler";


    /*public enum States  {
        INIT,
        INSIDE_AREA,
        OUTSIDE_AREA,
        FAVORITE_FOUND
    }*/
    public final static int INIT = 0;
    public final static int INSIDE_AREA = 1;
    public final static int OUTSIDE_AREA = 2;
    public final static int FAVORITE_FOUND =3;

    private int mState;
    private Location mFavoriteCandidate;
    private Location mFavorite;

    private int mOutsideAreaHitCounter;
    private int mInsideAreaHitCounter = 0;
    private final Context mContext;
    private long mTimeSpentInArea;
    private long mCountLocationUpdates;

    // Configuration
    private float cTrackingDistance; // [m]
    private int cMaxHitsOutSideArea;  // n:o
    private int cMinHitsInSideArea; // n:o
    private int cMinTimeInArea;       // [ms]
    private float cAccuracy;          //[m]
    private boolean cShowLocationData;


    // Constructor
    public FavoriteHandler(Context context) {
        mState = INIT;
        mCountLocationUpdates = 0;
        mOutsideAreaHitCounter = 0;
        mTimeSpentInArea = 0;
        mContext = context;
        updateSettings();
    }

    public void reset() {
        mOutsideAreaHitCounter = 0;
        mState = INIT;
    }

    // getters
    public long getTime() {
        return mFavorite.getTime() ;
    }

    public long getTimeSpent() {
        return mTimeSpentInArea;
    }

    public Location getLocation() {
        return mFavorite;
    }

    public Location getmFavoriteCandidate() {
        return mFavoriteCandidate;
    }

    public int getState() {
        return mState;
    }

    // Read settings
    public void updateSettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        cMinTimeInArea = Integer.parseInt(sharedPref.getString(SettingsActivity.MIN_TIME_IN_AREA,"120")) * 1000;
        cTrackingDistance = Integer.parseInt(sharedPref.getString(SettingsActivity.TRACKING_DISTANCE, "50"));
        cMaxHitsOutSideArea = Integer.parseInt(sharedPref.getString(SettingsActivity.MAX_HITS_OUTSIDE_AREA, "3"));
        cMinHitsInSideArea = Integer.parseInt(sharedPref.getString(SettingsActivity.MIN_HITS_INSIDE_AREA, "2"));
        cAccuracy = Float.parseFloat(sharedPref.getString(SettingsActivity.LOCATION_ACCURACY, "10.0"));
        cShowLocationData = sharedPref.getBoolean(SettingsActivity.SHOW_LOCATION_DATA, false);
        Log.d(TAG,"cMaxHitsOutSideArea:" + String.valueOf(cMaxHitsOutSideArea));
    }
    //
    public boolean isFavorite(Location location) {
        Log.d(TAG, "isFavorite:" + String.valueOf(mState));
        mCountLocationUpdates++;
        boolean result = false;
        boolean isAccuracyOk = (location.getAccuracy() < cAccuracy);
        // Don't use locations that are too bad.
        if(!isAccuracyOk && (cAccuracy > 0.0)) {
            TrackerUtilities.displayToast(cShowLocationData,mContext,
                    String.format("Bad accuracy(%.2f)",location.getAccuracy()));
            return false;
        }
        switch (mState) {
            case INIT: {
                mFavoriteCandidate = location;
                mState = INSIDE_AREA;
            }
            break;
            case INSIDE_AREA: {
                mFavorite = null;
                mTimeSpentInArea = location.getTime() - mFavoriteCandidate.getTime();
                if(location.distanceTo(mFavoriteCandidate) > cTrackingDistance) {
                    mOutsideAreaHitCounter++;
                    mState = OUTSIDE_AREA;
                } else {
                    // We are still inside the area
                    mInsideAreaHitCounter++;
                    if ((mTimeSpentInArea > cMinTimeInArea) &&
                            (mInsideAreaHitCounter >= cMinHitsInSideArea)) {
                        mInsideAreaHitCounter = 0;
                        mState = FAVORITE_FOUND;
                    }
                }
            }
            break;
            case OUTSIDE_AREA: {
                // User is outside area but time spent is too little
                // If more than cMaxHitsOutSideArea mFavoriteCandidate was not a Favorite
                if (location.distanceTo(mFavoriteCandidate) <= cTrackingDistance) {
                    mOutsideAreaHitCounter = 0;
                    mState = INSIDE_AREA;
                } else {
                    mOutsideAreaHitCounter++;
                    if (mOutsideAreaHitCounter >= cMaxHitsOutSideArea) {
                        // We left the area and spent and a new favorite candidate is set.
                        mFavoriteCandidate = location;
                        mOutsideAreaHitCounter = 0;
                        mInsideAreaHitCounter = 0;
                        mState = INSIDE_AREA;
                    }
                }
            }
            break;
            case FAVORITE_FOUND: {
                // Just wait for the user to leave the area.
                mTimeSpentInArea = location.getTime() - mFavoriteCandidate.getTime();
                if (location.distanceTo(mFavoriteCandidate) > cTrackingDistance) {
                    mOutsideAreaHitCounter++;
                } else {
                    mOutsideAreaHitCounter = 0;
                }

                if (mOutsideAreaHitCounter >= cMaxHitsOutSideArea) {
                    // Area is left and a new favorite candidate is set.
                    result = true;
                    mOutsideAreaHitCounter = 0;
                    mFavorite = mFavoriteCandidate;
                    mFavoriteCandidate = location;
                    mState = INSIDE_AREA;
                }
            }
            break;
            default:{

            }
        }
        String stateResult = "Favorite: " + String.valueOf(result) +
                "\nDistance: " + String.format("%.2f m", location.distanceTo(mFavoriteCandidate)) +
                "\ntimeSpent: " + String.valueOf(mTimeSpentInArea / 1000) + " s" +
                "\nhitsInside: " + String.valueOf(mInsideAreaHitCounter) +
                "\nhitsOutside: " + String.valueOf(mOutsideAreaHitCounter) +
                "\nisAccuracyOk: "+ String.valueOf(isAccuracyOk)+
                "\nState: " + String.valueOf(mState) +
                "\nmCountLocationUpdates: " +  String.valueOf(mCountLocationUpdates);
        Log.d(TAG, "StateResult\n" + stateResult);
        TrackerUtilities.displayToast(cShowLocationData, mContext, stateResult);
        return result;
    }
}
