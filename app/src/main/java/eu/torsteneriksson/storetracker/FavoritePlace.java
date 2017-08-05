package eu.torsteneriksson.storetracker;

/**
 * Created by torsten on 2016-05-07.
 */
public class FavoritePlace {
    public
    void setName(String name) {mName = name;}
    void setAddress(String address) {mAddress = address;}
    void setLat(double lat) {mLat=lat;}
    void setLon(double lon) {mLon = lon;}
    void setIcon(String icon) {
        mIcon = icon;
    }
    String getName() {return mName;}
    String getAddress() {return mAddress;}
    double getLat() {return mLat;}
    double getLng() {return mLon;}
    String getIcon() {
        return mIcon;
    }

    private
    String mName;
    String mAddress;
    double mLat;
    double mLon;
    String mIcon;
};
