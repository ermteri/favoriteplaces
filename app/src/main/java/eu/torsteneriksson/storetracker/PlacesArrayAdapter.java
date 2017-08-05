package eu.torsteneriksson.storetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Created by torsten on 2016-05-07.
 */
public class PlacesArrayAdapter extends ArrayAdapter {
    private final List mList;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final static String TAG = "PlacesArrayAdapter";
    private Bitmap mIconImg;
    private String mUrl;


    public PlacesArrayAdapter(Context context, List<FavoritePlace> list) {
        super(context,R.layout.placesrow,list);
        this.mContext = context;
        this.mList = list;
        this.mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder mHolder;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(convertView == null)
        {
            // Get a new instance of the row layout view
            convertView = mLayoutInflater.inflate(R.layout.placesrow, null);
            mHolder = new ViewHolder();
            convertView.setTag(mHolder);

        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }
        // Hold the view objects in an object, that way the don't need to be "re-  finded"
        mHolder.mPlaceName = (TextView) convertView.findViewById(R.id.place_name);
        mHolder.mPlaceAddress = (TextView) convertView.findViewById(R.id.place_address);
        mHolder.mIcon = (ImageView) convertView.findViewById(R.id.place_icon);

        /** Set data to your Views. */
        FavoritePlace item = (FavoritePlace) mList.get(position);
        mHolder.mPlaceName.setText(item.getName());
        mHolder.mPlaceAddress.setText(item.getAddress());
        mUrl = item.getIcon();
        URL url;
        try {
            url = new URL(mUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            mIconImg = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Log.d(TAG,"Exception:"+ e.toString());
        }

        mHolder.mIcon.setImageBitmap(mIconImg);
        Log.d(TAG,"icon URL:" + item.getIcon());
        return convertView;
    }
    private class ViewHolder {
        private TextView mPlaceName;
        private TextView mPlaceAddress;
        private ImageView mIcon;
    }
}
