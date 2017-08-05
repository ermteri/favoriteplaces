package eu.torsteneriksson.storetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Created by torsten on 1/18/2017.
 */

public class MarkerClusterRenderer extends DefaultClusterRenderer<FavoriteMarker> {
    Context mContext;

    public MarkerClusterRenderer(Context context, GoogleMap map, ClusterManager<FavoriteMarker> clusterManager) {
        super(context, map, clusterManager);
        mContext = context;
        //constructor
    }

    @Override
    protected void onBeforeClusterItemRendered(final FavoriteMarker marker, MarkerOptions markerOptions) {
        // Change icon based on category
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitMap(FavoriteCategory.getCategoryImage(marker.getCategory()))));
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        /* TODO
        if(cluster.getSize() > 1 && mCurrentZoom < mMaxZoom) {
            return true;
        } else {
            return false;
        } */
        return cluster.getSize() > 10; // if markers <= 10 then not clustering
    }

    private Bitmap getBitMap(int resource) {
        //Log.d(TAG,"getBitMap");

        int px = mContext.getResources().getDimensionPixelSize(R.dimen.map_dot_marker_size);
        Bitmap bitMap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitMap);
        Drawable shape = mContext.getResources().getDrawable(resource);
        shape.setBounds(0, 0, bitMap.getWidth(), bitMap.getHeight());
        shape.draw(canvas);
        return bitMap;
    }
}

