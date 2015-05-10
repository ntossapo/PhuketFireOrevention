package com.example.ntossapo.phuketprevention;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ntossapo.phuketprevention.fragment.EventListFragment;
import com.example.ntossapo.phuketprevention.fragment.MapRouteFragment;
import com.example.ntossapo.phuketprevention.fragment.NavigationDrawerFragment;
import com.example.ntossapo.phuketprevention.type.Event;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class AppMain extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static LocationManager locationManager;
    private FragmentManager fragmentManager;

    private Spinner accidentypeSpinner;
    private EditText editTextName;
    private EditText editTextDescript;

    private CheckBox checkBox_nr;
    private CheckBox checkBox_em;
    private CheckBox checkBox_hb;
    private AlertDialog.Builder builder;
    private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    private static Fragment currentFragment = null;
    private static Place place;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        SettingLocalManager();
        syncTheUnSync();
        if(AppStatus.MODE.equals("offline"))
            showDataIfHave();
    }

    private void checkOnline(){
        try {
            //SYNCHRONISE TO SERVER
            String json = (new AsyncTask<Void, Void, String>(){
                @Override
                protected String doInBackground(Void... params) {
                    String result = null;
                    HttpGet httpGet = new HttpGet("http://" + AppStatus.IP_ADDRESS + ":" + AppStatus.PORT + "/server");
                    HttpParams httpParams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                    HttpConnectionParams.setSoTimeout(httpParams, 2000);

                    HttpClient httpClient = new DefaultHttpClient(httpParams);
                    try {
                        HttpResponse response = httpClient.execute(httpGet);
                        switch (response.getStatusLine().getStatusCode()){
                            case 200:
                                HttpEntity entity = response.getEntity();
                                if(entity != null){
                                    result = EntityUtils.toString(entity, "UTF-8");
                                }
                        }
                    }catch (Exception e){
                        Log.d("e", e.getMessage());
                    }
                    return result;
                }
            }.execute()).get();

            //WORK OFFLINE MODE
            if(json == null) {
                AppStatus.MODE = "offline";
                Toast.makeText(getApplicationContext(), "Application using offline mode", Toast.LENGTH_LONG).show();
                //WORK ONLINE MODE
            }else {
                JSONObject j = new JSONObject(json);
                if (j.getString("ServerStatus").equals("online")) {
                    AppStatus.EVENT_VERSION = ((JSONObject) j.get("DataVersion")).getInt("EventVersion");
                    AppStatus.BLOCK_VERSION = ((JSONObject) j.get("DataVersion")).getInt("BlockVersion");
                    Log.d("blockver", AppStatus.BLOCK_VERSION + "");
                    Log.d("eventver", AppStatus.EVENT_VERSION + "");
                    AppStatus.MODE = "online";
                }
            }
        } catch (InterruptedException e) {
            Log.d("e", e.getMessage());
        } catch (ExecutionException e) {
            Log.d("e", e.getMessage());
        } catch (JSONException e) {
            Log.d("e", e.getMessage());
        }
    }

    private void showDataIfHave() {
        Log.d("AppStatus", "Check to load data form db to show in list");
        SQLiteDatabase db = openOrCreateDatabase("patong", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS " +
                "EVENTUNSYNC(TYPE TEXT, NAME TEXT, DESC TEXT, STATUS TEXT," +
                " OPTION TEXT, DATETIME TEXT, PLACE TEXT, ADDRESS TEXT, LAT REAL, LONG REAL);");
        Cursor cursor = db.rawQuery("SELECT TYPE, NAME, DESC, STATUS, OPTION, " +
                "DATETIME, PLACE, ADDRESS, LAT, LONG FROM EVENTUNSYNC", null);
        Log.d("AppStatus", "data in db : " + cursor.getCount());

        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            JSONArray array = new JSONArray();
            for (int i = 0; (i < cursor.getCount()); i++) {
                try {
                    JSONObject pos = new JSONObject();
                    pos.put("Lat", cursor.getDouble(8));
                    pos.put("Long", cursor.getDouble(9));
                    JSONObject jsonInput = new JSONObject();
                    jsonInput.put("Pos", pos)
                            .put("Type", cursor.getString(0))
                            .put("Name", cursor.getString(1))
                            .put("InformerId", AppStatus.USER_ID)
                            .put("Telnum", AppStatus.USER_TEL)
                            .put("Desc", cursor.getString(2))
                            .put("DateTime", cursor.getString(5))
                            .put("Status", cursor.getString(3))
                            .put("Place", cursor.getString(6))
                            .put("Address", cursor.getString(7))
                            .put("Option", cursor.getString(4))
                            .put("id", "unregister");
                    array.put(jsonInput);
                } catch (JSONException e) {
                    Log.d("AppStatus", e.getMessage());
                }
            }
            AppStatus.DATA.setAccidentData(array);
            Log.d("AppStatus", "set data in offline mode");
        }
    }

    private void syncTheUnSync(){
        boolean syncSuccess = true;
        if(AppStatus.MODE.equals("online")) {
            Log.d("AppStatus", "Check to Sync");
            SQLiteDatabase db = openOrCreateDatabase("patong", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS " +
                    "EVENTUNSYNC(TYPE TEXT, NAME TEXT, DESC TEXT, STATUS TEXT," +
                    " OPTION TEXT, DATETIME TEXT, PLACE TEXT, ADDRESS TEXT, LAT REAL, LONG REAL);");
            Cursor cursor = db.rawQuery("SELECT TYPE, NAME, DESC, STATUS, OPTION, " +
                    "DATETIME, PLACE, ADDRESS, LAT, LONG FROM EVENTUNSYNC", null);

            if (cursor.getCount() != 0) {
                Log.d("AppStatus", "Need Sync :" + cursor.getCount());
                cursor.moveToFirst();
                for (int i = 0; (i < cursor.getCount()) && syncSuccess; i++) {
                    try {
                        JSONObject pos = new JSONObject();
                        pos.put("lat", cursor.getDouble(8));
                        pos.put("long", cursor.getDouble(9));
                        JSONObject jsonInput = new JSONObject();
                        String[] s = cursor.getString(3).split("/");
                        jsonInput.put("pos", pos)
                                .put("type", cursor.getString(0))
                                .put("name", cursor.getString(1))
                                .put("informerid", AppStatus.USER_ID)
                                .put("telnum", AppStatus.USER_TEL)
                                .put("desc", cursor.getString(2))
                                .put("dateTime", cursor.getString(5))
                                .put("status", s[0])
                                .put("place", cursor.getString(6))
                                .put("address", cursor.getString(7))
                                .put("option", cursor.getString(4));

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
                                Log.d("AppData", "syncTheUnSync data " + params[0].toString());

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
                                    Log.d("AppStatus", e.getMessage());
                                }

                                return result;
                            }

                        }).execute(jsonInput).get(4000, TimeUnit.MILLISECONDS);
                        JSONObject out = new JSONObject(resultJson);
                        if(!out.getString("Status").equals("success : insert success")) {
                            syncSuccess = false;
                        }
                    } catch (InterruptedException e) {
                        Log.d("AppStatus", e.getMessage());
                    } catch (ExecutionException e) {
                        Log.d("AppStatus", e.getMessage());
                    } catch (JSONException e) {
                        Log.d("AppStatus", e.getMessage());
                    } catch (TimeoutException e) {
                        Log.d("AppStatus", e.getMessage());
                    }
                }
                if(syncSuccess) {
                    db.execSQL("DELETE FROM EVENTUNSYNC WHERE 1");
                    Log.d("AppStatus", "Sync the unSync Success");
                    AppStatus.DATA.getAccidentData().clear();
                    Command.SyncEvent(AppMain.this);
                }
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        int count = 0;
        syncTheUnSync();
        if(AppStatus.MODE.equals("offline")) {
            SQLiteDatabase db = openOrCreateDatabase("patong", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS " +
                    "EVENTUNSYNC(TYPE TEXT, NAME TEXT, DESC TEXT, STATUS TEXT," +
                    " OPTION TEXT, DATETIME TEXT, PLACE TEXT, ADDRESS TEXT, LAT REAL, LONG REAL);");

            for (int i = 0; i < AppStatus.DATA.getAccidentData().size(); i++) {
                if (AppStatus.DATA.getAccidentData().get(i).getId().equals("unregister")
                        && !AppStatus.DATA.getAccidentData().get(i).getStatus().contains("unregister")) {
                    Event e = AppStatus.DATA.getAccidentData().get(i);
                    db.execSQL("INSERT INTO EVENTUNSYNC(TYPE, NAME, DESC, STATUS, OPTION," +
                            " DATETIME, PLACE, ADDRESS, LAT, LONG) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{
                            e.getType(),
                            e.getName(),
                            e.getDescript(),
                            e.getStatus()+"/unregister",
                            e.getOption(),
                            formatter.format(e.getDateTime()),
                            e.getPlace(),
                            e.getAddress(),
                            e.getLatitude(),
                            e.getLongitude()});
                    count++;
                }
            }
            Cursor c = db.rawQuery("select * from EVENTUNSYNC", null);
            Log.d("AppDebug", c.getCount()+"");
            Log.d("AppStatus", "Add event to database : " + count);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOnline();
        syncTheUnSync();
        if(currentFragment != null)
            fragmentManager.beginTransaction()
                    .replace(R.id.container, currentFragment)
                    .commit();
    }

    public void ShowRouting(Event ev){
        currentFragment = MapRouteFragment.newInstance(ev);
        fragmentManager.beginTransaction()
                .replace(R.id.container, currentFragment)
                .commit();
    }

    public void ShowEventList(){
        currentFragment = EventListFragment.newInstance();
        fragmentManager.beginTransaction()
                .replace(R.id.container, currentFragment)
                .commit();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                currentFragment = EventListFragment.newInstance();
                break;
            case 1:
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                Intent intent = null;
                try {
                    intent = intentBuilder.build(this);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                // Start the intent by requesting a result,
                // identified by a request code.
                startActivityForResult(intent, 1);
                break;
        }
        if(currentFragment != null)
            fragmentManager.beginTransaction()
                    .replace(R.id.container, currentFragment)
                    .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }



    private void showInformDialog(final Place p, final byte[] image){
        builder = new AlertDialog.Builder(AppMain.this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_accident_addnew, null);
        accidentypeSpinner = (Spinner) v.findViewById(R.id.spinnerAccidentType);
        editTextName = (EditText) v.findViewById(R.id.txt_name);
        editTextDescript = (EditText) v.findViewById(R.id.txt_descript);
        checkBox_nr = (CheckBox) v.findViewById(R.id.checkbox_nr);
        checkBox_em = (CheckBox) v.findViewById(R.id.checkbox_em);
        checkBox_hb = (CheckBox) v.findViewById(R.id.checkbox_hb);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(AppMain.this, R.array.accident_type, R.layout.layout_textviewforspinner);
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
                    Date d = new Date();
                    JSONObject pos = new JSONObject();
                    pos.put("lat", p.getLatLng().latitude);
                    pos.put("long", p.getLatLng().longitude);
                    JSONObject jsonInput = new JSONObject();
                    jsonInput.put("pos", pos)
                            .put("type", type)
                            .put("name", name)
                            .put("informerid", AppStatus.USER_ID)
                            .put("telnum", AppStatus.USER_TEL)
                            .put("desc", desc)
                            .put("image", Base64.encodeToString(image, Base64.DEFAULT))
                            .put("dateTime", formatter.format(d))
                            .put("status", "แจ้งเหตุ")
                            .put("place", p.getName())
                            .put("address", p.getAddress())
                            .put("option", option);
                    Log.d("AppStatus", "new Event : " + jsonInput.toString());
                    JSONArray j = null;
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
                                        Log.d("AppStatus", e.getMessage());
                                    }

                                    return result;
                                }
                            }).execute(jsonInput).get(4000, TimeUnit.MILLISECONDS);
                            JSONObject jsonObject = new JSONObject(resultJson);
                            if(jsonObject.getString("Status").equals("success : insert success")) {
                                Toast.makeText(getApplicationContext(), "แจ้งเหตุเสร็จสมบูรณ์", Toast.LENGTH_LONG).show();
                            }
                        } catch (InterruptedException e) {
                            Log.d("AppStatus", "InterruptedException :" + e.getMessage());
                        } catch (ExecutionException e) {
                            Log.d("AppStatus", "ExecutionException :" + e.getMessage());
                        } catch (TimeoutException e) {
                            Log.d("AppStatus", "TimeoutException :" + e.getMessage());
                        }
                    }else{
                        JSONObject pos2 = new JSONObject();
                        pos2.put("Lat", p.getLatLng().latitude);
                        pos2.put("Long", p.getLatLng().longitude);
                        JSONObject jsonObject2 = new JSONObject();
                        jsonObject2.put("Pos", pos2)
                                .put("id", "unregister")
                                .put("Type", type)
                                .put("Name", name)
                                .put("InformerId", AppStatus.USER_ID)
                                .put("Telnum", AppStatus.USER_TEL)
                                .put("Desc", desc)
                                .put("DateTime", formatter.format(d))
                                .put("Status", "แจ้งเหตุ")
                                .put("Place", p.getName())
                                .put("Address", p.getAddress())
                                .put("Image", Base64.encodeToString(image, Base64.DEFAULT))
                                .put("Option", option);
                        j = new JSONArray();
                        j.put(jsonObject2);
                        AppStatus.DATA.setAccidentData(j);
                        Log.d("AppData", "offline mode add data " + jsonObject2.toString());
                    }
                    Log.d("AppData", "Data in AppStatus.DATA " + AppStatus.DATA.getAccidentData().size());
                    Fragment f = null;
                    f = EventListFragment.newInstance();
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, f)
                            .commit();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1
                && resultCode == Activity.RESULT_OK) {
            // The user has selected a place. Extract the name and address.
            place = PlacePicker.getPlace(data, this);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,2);
        }
        if(requestCode == 2 && resultCode == Activity.RESULT_OK){
            Bitmap bmp = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            showInformDialog(place, stream.toByteArray());
        }
        if(requestCode == 2 && resultCode == Activity.RESULT_CANCELED){

        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            final EditText t = new EditText(AppMain.this);
            t.setHint("ป้อน IP Address ของเซิร์ฟเวอร์");
            AlertDialog.Builder builder = new AlertDialog.Builder(AppMain.this);
            builder.setTitle("ตั้งค่า หมายเลข IP Address ของเซิร์ฟเวอร์");
            builder.setView(t);
            builder.setPositiveButton("เปลี่ยนแปลง", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Validator.isIPAddress(t.getText().toString())){
                        AppStatus.IP_ADDRESS = t.getText().toString();
                        Toast.makeText(getApplicationContext(), "เปลี่ยนแปลงไอพีแอดเดรสแล้ว", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getApplicationContext(), "รูปแบบไอพีแอดเดรสไม่ถูกต้อง", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            });
            builder.setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void SettingLocalManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, AppStatus.INTERVAL_MINTIME_UPDATE, AppStatus.MINIMUM_DISTANCE_UPDATE, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                AppStatus.USER_LOCATION_LAT = location.getLatitude();
                AppStatus.USER_LOCATION_LONG = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

}