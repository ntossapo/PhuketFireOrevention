package com.example.ntossapo.phuketprevention.fragment;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.ntossapo.phuketprevention.AppStatus;
import com.example.ntossapo.phuketprevention.Functional.GoogleMapDirectionsFunctional;
import com.example.ntossapo.phuketprevention.R;
import com.example.ntossapo.phuketprevention.type.Event;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Patchara on 5/1/2558.
 */
public class MapRouteFragment extends Fragment{

    private static String serialize_key_accident = "maproutefragment";
    private SupportMapFragment supportMapFragment;
    private GoogleMap googleMap;
    private Event accident;
    private boolean resume = true;
    private static LatLng oldLatLng;
    private Button btnResume;
    public static MapRouteFragment newInstance(Event acc) {
        MapRouteFragment fragment = new MapRouteFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(serialize_key_accident, acc);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        try {
            getMapFragmentAndSetting();
            accident = (Event)getArguments().getSerializable(serialize_key_accident);

            JSONObject originJson = new JSONObject();
            originJson.put("lat", AppStatus.USER_LOCATION_LAT)
                    .put("long", AppStatus.USER_LOCATION_LONG);

            JSONObject destJson = new JSONObject();
            destJson.put("lat", accident.getLatitude())
                    .put("long", accident.getLongitude());

            JSONObject input = new JSONObject();
            input.put("origin", originJson)
                    .put("destination", destJson);

            String jsonResult = (new AsyncTask<JSONObject, Void, String>() {
                @Override
                protected String doInBackground(JSONObject... params) {
                    String result = null;
                    HttpPost httpPost = new HttpPost("http://" + AppStatus.IP_ADDRESS + ":" + AppStatus.PORT + "/route");
                    HttpParams httpParams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                    HttpConnectionParams.setSoTimeout(httpParams, 2000);
                    HttpClient httpClient = new DefaultHttpClient(httpParams);

                    List<NameValuePair> nameValuePairs = new ArrayList<>();
                    nameValuePairs.add(new BasicNameValuePair("data", params[0].toString()));
                    try {
                        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
                        HttpResponse response = httpClient.execute(httpPost);
                        switch (response.getStatusLine().getStatusCode()) {
                            case 200:
                                HttpEntity entity = response.getEntity();
                                if (entity != null) {
                                    result = EntityUtils.toString(entity, "UTF-8");
                                }
                        }
                    } catch (Exception e) {
                        Log.d("e", e.getMessage());
                    }
                    return result;
                }
            }).execute(input).get(4000, TimeUnit.MILLISECONDS);
            Log.d("AppStatus", "Route result json format : " + jsonResult);
            setPolyLine(jsonResult);
            animateCameraToMarker();
            btnResume = (Button) rootView.findViewById(R.id.map_frag_btnresume);
            btnResume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resume = true;
                    CameraPosition cameraPosition = new CameraPosition(new LatLng(AppStatus.USER_LOCATION_LAT, AppStatus.USER_LOCATION_LONG), 20, 80, googleMap.getCameraPosition().bearing);
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            });
            btnResume.setVisibility(View.GONE);
            Log.d("AppStatus", "btnResume " + btnResume.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        return rootView;
    }

    private void getMapFragmentAndSetting() {
        Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.container);
        supportMapFragment = (SupportMapFragment) f.getChildFragmentManager().findFragmentById(R.id.map);
        googleMap = supportMapFragment.getMap();
        googleMap.setMyLocationEnabled(true);
        googleMap.setBuildingsEnabled(true);
    }

    private void animateCameraToMarker() {

        googleMap.addMarker(new MarkerOptions().position(accident.getLatLng()).title(accident.getType()).snippet(accident.getDescript()));
        CameraPosition cameraPosition = new CameraPosition(new LatLng(AppStatus.USER_LOCATION_LAT, AppStatus.USER_LOCATION_LONG), 20, 80, googleMap.getCameraPosition().bearing);
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.setBuildingsEnabled(true);
        googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if(!resume)
                    return;
                CameraPosition cameraPosition = new CameraPosition(new LatLng(AppStatus.USER_LOCATION_LAT, AppStatus.USER_LOCATION_LONG), 20, 80, location.getBearing());
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                oldLatLng = new LatLng(AppStatus.USER_LOCATION_LAT, AppStatus.USER_LOCATION_LONG);
                btnResume.setVisibility(View.GONE);
            }
        });
    }


    private void setPolyLine(String json) {
        try {
            JSONArray routeArray = new JSONArray(json);
            for(int i = 0; i < routeArray.length(); i++) {
                List<LatLng> list = GoogleMapDirectionsFunctional.decodePoly(((JSONObject) routeArray.get(i)).getString("Path"));
                int red = 0;
                int green = 255;
                red += (((JSONObject)routeArray.get(i)).getInt("Score") * 15);
                green -= (((JSONObject)routeArray.get(i)).getInt("Score") * 15);

                if(red > 255) red = 255;
                if(green < 0) green = 0;

                StringBuilder redSb = new StringBuilder();
                redSb.append(Integer.toHexString(red).toUpperCase());
                if(redSb.length() < 2) {
                    redSb.insert(0, '0'); // pad with leading zero if needed
                }

                StringBuilder greenSb = new StringBuilder();
                greenSb.append(Integer.toHexString(green).toUpperCase());
                if(greenSb.length() < 2) {
                    greenSb.insert(0, '0'); // pad with leading zero if needed
                }

                String colorCode = "#"+redSb.toString()+greenSb.toString()+"00";
                Log.d("AppStatus", "Color code : " + colorCode);

                PolylineOptions border = new PolylineOptions().width(50).color(Color.parseColor("#000099")).geodesic(true);
                PolylineOptions options = new PolylineOptions().width(40).color(Color.parseColor(colorCode)).geodesic(true);

                for (int z = 0; z < list.size(); z++) {
                    border.add(list.get(z));
                    options.add(list.get(z));
                }

                googleMap.addPolyline(border);
                googleMap.addPolyline(options);
            }
            Log.d("AppStatus", "route count : " + routeArray.length());
        } catch (JSONException e) {
            Log.d("AppStatus", "Json Exception : " + e.getMessage());
        }
    }
}
