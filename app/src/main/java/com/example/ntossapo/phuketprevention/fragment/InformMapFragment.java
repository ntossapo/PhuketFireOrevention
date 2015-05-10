package com.example.ntossapo.phuketprevention.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ntossapo.phuketprevention.AppStatus;
import com.example.ntossapo.phuketprevention.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class InformMapFragment extends Fragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    private GoogleMap googleMap = null;
    private SupportMapFragment supportMapFragment = null;

    private Spinner accidentypeSpinner;
    private EditText editTextName;
    private EditText editTextDescript;

    private CheckBox checkBox_nr;
    private CheckBox checkBox_em;
    private CheckBox checkBox_hb;
    private AlertDialog.Builder builder;
    private LatLng position;

    private static final LatLng phuketLatLng = new LatLng(7.9741161, 98.3254469);

    public static InformMapFragment newInstance() {
        InformMapFragment fragment = new InformMapFragment();
        return fragment;
    }

    public InformMapFragment() {
    }

    @Override
    public void onDestroy() {
        saveOldCameraPosition();
        super.onDestroy();
    }

    private void saveOldCameraPosition() {
        CameraPosition mMyCam = googleMap.getCameraPosition();
        double longitude = mMyCam.target.longitude;
        double latitude = mMyCam.target.latitude;
        double zoom = mMyCam.zoom;
        double bearing = mMyCam.bearing;
        double tilt = mMyCam.tilt;
        SharedPreferences setting = this.getActivity().getSharedPreferences("MAP_RESUME", 0);
        SharedPreferences.Editor editor = setting.edit();
        editor.putFloat("longitude", (float) longitude);
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("zoom", (float) zoom);
        editor.putFloat("bearing", (float) bearing);
        editor.putFloat("tilt", (float) tilt);
        editor.commit();
    }

    @Override
    public void onResume() {

        super.onResume();
        loadOldCameraPosition();

    }

    private void loadOldCameraPosition() {
        SharedPreferences setting = this.getActivity().getSharedPreferences("MAP_RESUME", 0);
        float longitude = setting.getFloat("longitude", -1);
        if (longitude == -1) {
            CameraPosition cameraPosition = new CameraPosition(phuketLatLng, 11, 0, 0);
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            return;
        }
        float latitude = setting.getFloat("latitude", 0);
        float zoom = setting.getFloat("zoom", 0);
        float bearing = setting.getFloat("bearing", 0);
        float tilt = setting.getFloat("tilt", 0);

        LatLng resumePosition = new LatLng(latitude, longitude);

        CameraPosition cameraPosition = new CameraPosition(resumePosition, zoom, tilt, bearing);
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        getMapFragmentAndSetting();
        addMarker();
        setOnLongClickMap();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(phuketLatLng, 10));

        return rootView;
    }

    private void setOnLongClickMap() {
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onMapLongClick(LatLng latLng) {
                position = latLng;
                builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getLayoutInflater(null);
                View v = inflater.inflate(R.layout.dialog_accident_addnew, null);
                accidentypeSpinner = (Spinner) v.findViewById(R.id.spinnerAccidentType);
                editTextName = (EditText) v.findViewById(R.id.txt_name);
                editTextDescript = (EditText) v.findViewById(R.id.txt_descript);
                checkBox_nr = (CheckBox) v.findViewById(R.id.checkbox_nr);
                checkBox_em = (CheckBox) v.findViewById(R.id.checkbox_em);
                checkBox_hb = (CheckBox) v.findViewById(R.id.checkbox_hb);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.accident_type, R.layout.layout_textviewforspinner);
                adapter.setDropDownViewResource(R.layout.layout_textviewforspinner);
                accidentypeSpinner.setAdapter(adapter);



                builder.setView(v);
                builder.setPositiveButton("แจ้งเหตุ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String option = "";
                            if (checkBox_em.isChecked())
                                option = option + "em/";
                            if (checkBox_nr.isChecked())
                                option = option + "nr/";
                            if (checkBox_hb.isChecked())
                                option = option + "hb";
                            String name = editTextName.getText().toString();
                            String desc = editTextDescript.getText().toString();
                            String type = ((TextView)accidentypeSpinner.getSelectedView()).getText().toString();
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                            Date d = new Date();
                            JSONObject pos = new JSONObject();
                            pos.put("lat", position.latitude);
                            pos.put("long", position.longitude);
                            JSONObject jsonInput = new JSONObject();
                            jsonInput.put("pos", pos)
                                    .put("type", type)
                                    .put("name", name)
                                    .put("informerid", AppStatus.USER_ID)
                                    .put("telnum", AppStatus.USER_TEL)
                                    .put("desc", desc)
                                    .put("dateTime", formatter.format(d))
                                    .put("status", "แจ้งเหตุ")
                                    .put("option", option);
                            if (AppStatus.MODE.equals("online")) {
                                try {
                                    String resultJson = (new AsyncTask<JSONObject, Void, String>() {
                                        @Override
                                        protected String doInBackground(JSONObject... params) {
                                            String result = null;
                                            HttpPost httpPost = new HttpPost("http://" + AppStatus.IP_ADDRESS + ":" + AppStatus.PORT + "/eventadd");

                                            HttpParams httpParams = new BasicHttpParams();
                                            HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                                            HttpConnectionParams.setSoTimeout(httpParams, 2000);
                                            HttpClient httpClient = new DefaultHttpClient(httpParams);

                                            List<NameValuePair> nameValuePairs = new ArrayList<>();
                                            nameValuePairs.add(new BasicNameValuePair("data", params[0].toString()));
                                            Log.d("add event", params[0].toString());
                                            try {
                                                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
                                                HttpResponse response = httpClient.execute(httpPost);
                                                switch (response.getStatusLine().getStatusCode()) {
                                                    case 200:
                                                        HttpEntity entity = response.getEntity();
                                                        if (entity != null)
                                                            result = EntityUtils.toString(entity, "UTF-8");
                                                }
                                            } catch (Exception e) {
                                                Log.d("e", e.getMessage());
                                            }

                                            return result;
                                        }
                                    }).execute(jsonInput).get();

                                    if(resultJson != null)
                                        googleMap.addMarker(new MarkerOptions().title(name).snippet(desc).position(position));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                builder.setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.setTitle("แจ้งเหตุ");
                builder.show();

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1
                && resultCode == Activity.RESULT_OK) {
            //final Place place = PlacePicker.getPlace(data, this);
        }
    }

    private void addMarker() {
        for(int i =0; i < AppStatus.DATA.getAccidentData().size();i++)
            googleMap.addMarker(AppStatus.DATA.getAccidentData().get(i).getMarkerOption());
    }

    private void getMapFragmentAndSetting() {
        Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.container);
        supportMapFragment = (SupportMapFragment) f.getChildFragmentManager().findFragmentById(R.id.map);
        googleMap = supportMapFragment.getMap();
        googleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
}

