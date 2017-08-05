package eu.torsteneriksson.storetracker;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by torsten on 1/16/2017.
 */

public class FavoriteMarker implements ClusterItem {
    private final LatLng mPosition;
    private int mId;

    private int mCategory;

    public FavoriteMarker(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public int getCategory() {
        return mCategory;
    }

    public void setCategory(int mCategory) {
        this.mCategory = mCategory;
    }


}
