package eu.torsteneriksson.storetracker;

import com.android.volley.toolbox.StringRequest;

/**
 * Created by torsten on 2015-11-22.
 */
public class FavoriteFilter {
    private long mMillis;
    private int mFavoriteCategory;
    private boolean mTodayOnly;
    private String mSearchText;
    FavoriteFilter(long millis, boolean todayOnly, int category, String searchText) {
        mMillis = millis;
        mFavoriteCategory = category;
        mTodayOnly = todayOnly;
        mSearchText = searchText;
    }
    public long getMillis() {
        return mMillis;
    }

    public boolean getTodayOnly() {
        return mTodayOnly;
    }

    public int getCategory() {
        return mFavoriteCategory;
    }

    public String getSearchText() {
        return mSearchText;
    }

    public void setMillis(long millis) {
      mMillis = millis;
    }
    public void setFavoriteCategory(int category) {
        mFavoriteCategory = category;
    }
    public void setTodayOnly(boolean todayOnly) {
        mTodayOnly = todayOnly;
    }
    public void setSearchText(String searchText) {
        mSearchText = searchText;
    }

}
