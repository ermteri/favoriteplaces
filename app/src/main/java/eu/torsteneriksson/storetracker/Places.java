package eu.torsteneriksson.storetracker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by torsten on 2016-04-25.
 */
public class Places {
    //private static final String API_KEY = "AIzaSyAOTLxI3n7MtSxTbDPUJAjF91t50PRrDSs";
    private static final String API_KEY = "AIzaSyDaes5SLuSNR9WWx6ceiQvFdw28L0vNPGk";
    //private static final String API_KEY = "AIzaSyCRLa4LQZWNQBcjCYcIVYA45i9i8zfClqc";
    private static final String PLACES_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/search/json?";
    //private static final String PLACES_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private static final String PLACES_TEXT_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";
    private static final String TAG ="Places" ;
    //https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=500&type=restaurant&name=cruise&key=YOUR_API_KEY

    Places(Context context){
        Settings settings = new Settings(context);

    }

    public void searchPlaces_old(final Context context, final ListView placeList, double latitude, double longitude, String radius) {
        String url = PLACES_SEARCH_URL +"location="+String.valueOf(latitude)+"," +
                String.valueOf(longitude) + "&radius="+ radius + "&key=" + API_KEY;
        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // the response is already constructed as a JSONObject!
                        List<String> your_array_list = new ArrayList<String>();
                        Log.d(TAG, response.toString());
                        try {
                            JSONArray places = response.getJSONArray("results");
                            for(int i = 0;i<places.length();i++) {
                                JSONObject place = places.getJSONObject(i);
                                JSONArray types = place.getJSONArray("types");
                                if(types.length()==1) {
                                    Log.d(TAG,"Type:" + types.getString(0));
                                    if (types.getString(0).equals("street_address")) {
                                        // We skip this place, it is only an address
                                        Log.d(TAG, "Skipped " + place.getString("name") + ","+
                                                types.getString(0));
                                        continue;
                                    }
                                }
                                your_array_list.add(place.getString("name"));
                                Log.d(TAG, "Name: " + place.getString("name"));
                            }
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                    context,
                                    R.layout.listplaces,
                                    your_array_list );

                            placeList.setAdapter(arrayAdapter);
                        } catch (JSONException e) {
                            Log.d(TAG,"getArray failed"+e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(jsonRequest);
    }

    public void searchPlaces(final Context context, final ListView placeList, double latitude, double longitude, String radius) {
        String url = PLACES_SEARCH_URL +"location="+String.valueOf(latitude)+"," +
                String.valueOf(longitude) + "&radius="+ radius + "&key=" + API_KEY;

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // the response is already constructed as a JSONObject!
                        List<FavoritePlace> place_array_list = new ArrayList<FavoritePlace>();
                        Log.d(TAG, response.toString());
                        try {
                            JSONArray places = response.getJSONArray("results");
                            Log.d(TAG,"Information from Google");
                            Log.d(TAG,"Places:" + places.toString());
                            for(int i = 0;i<places.length();i++) {
                                JSONObject place = places.getJSONObject(i);
                                JSONArray types = place.getJSONArray("types");
                                if(types.length()==1) {
                                    Log.d(TAG,"Type:" + types.getString(0));
                                    if (types.getString(0).equals("street_address")) {
                                        // We skip this place, it is only an address
                                        Log.d(TAG, "Skipped " + place.getString("name") + ","+
                                                types.getString(0));
                                        continue;
                                    }
                                }
                                FavoritePlace fp = new FavoritePlace();
                                fp.setName(place.getString("name"));
                                JSONObject geometry = place.getJSONObject("geometry");
                                JSONObject location = geometry.getJSONObject("location");
                                fp.setLat(location.getDouble("lat"));
                                fp.setLon(location.getDouble("lng"));
                                fp.setAddress(place.getString("vicinity"));
                                fp.setIcon(place.getString("icon"));
                                place_array_list.add(fp);
                                Log.d(TAG, "Name: " + fp.getName());
                                Log.d(TAG,"icon:" + place.get("icon"));
                            }

                            PlacesArrayAdapter adapter = new PlacesArrayAdapter(context, place_array_list);
                            placeList.setAdapter(adapter);
                            Log.d(TAG,"Number of places found:" + String.valueOf(place_array_list.size()));
                            for(int i=0;i< place_array_list.size();i++) {
                                Log.d(TAG,"Place:"+place_array_list.get(i).getName());
                            }
                        } catch (JSONException e) {
                            Log.d(TAG,"getArray failed"+e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(jsonRequest);
    }
}
