package com.example.ntossapo.phuketprevention;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Tossapon on 18/1/2558.
 */
public class Splash extends Activity {
    static EditText txtTelnum;
    AlertDialog.Builder builder;
    SQLiteDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        txtTelnum = new EditText(getApplicationContext());
        txtTelnum.setHint("เบอร์โทรศัพท์ / Telephone Number");
        txtTelnum.setTextColor(Color.parseColor("#000000"));
        builder = new AlertDialog.Builder(Splash.this);
        db = openOrCreateDatabase("patong", MODE_PRIVATE, null);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void run() {
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


                //ON WORK ONLINE
                if(AppStatus.MODE.equals("online")){
                    try {
                        //GET ALL EVENT
                        String jsonString = (new AsyncTask<Void, Void, String>(){
                            @Override
                            protected String doInBackground(Void... params) {
                                String result = null;
                                HttpGet httpGet = new HttpGet("http://" + AppStatus.IP_ADDRESS + ":" + AppStatus.PORT + "/eventget");
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
                        }).execute().get();
                        if(jsonString != null)
                            AppStatus.DATA.setAccidentData(new JSONArray(jsonString));
                        Log.d("event count", AppStatus.DATA.getAccidentData().size()+"");
                        Log.d("event json", jsonString);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                //CHECK REGISTER
                SQLiteDatabase db = openOrCreateDatabase("patong", MODE_PRIVATE, null);
                db.execSQL("CREATE TABLE IF NOT EXISTS User(ID TEXT, TELNUM TEXT, ISSYNC TEXT);");
                Cursor resultSet = db.rawQuery("SELECT ID, TELNUM, ISSYNC FROM User", null);
                Log.d("MODE", AppStatus.MODE);
                Log.d("RESULTSET_COUNT", resultSet.getCount()+"");
                //NOT REGISTER YET AND WORK IN ONLINE MODE
                if(resultSet.getCount() == 0 && AppStatus.MODE.equals("online")) {
                    txtTelnum.setHint("เบอร์โทรศัพท์ / Telephone Number");
                    txtTelnum.setTextColor(Color.parseColor("#000000"));
                    showOnlineDialog();
                }else if (resultSet.getCount() != 0 && AppStatus.MODE.equals("online")){
                    resultSet.moveToFirst();
                    AppStatus.USER_ID = resultSet.getString(0);
                    AppStatus.USER_TEL = resultSet.getString(1);
                    Log.d("USER_ID", AppStatus.USER_ID);
                    Log.d("USER_TEL", AppStatus.USER_TEL);
                    Intent intent = new Intent(getApplicationContext(), AppMain.class);
                    startActivity(intent);
                    finish();
                }else if(AppStatus.MODE.equals("offline") && resultSet.getCount() == 0){
                    showOfflineDialog();
                    AppStatus.USER_ID = "";
                    AppStatus.USER_TEL = txtTelnum.getText().toString();
                }else{
                    resultSet.moveToFirst();
                    AppStatus.USER_ID = resultSet.getString(0);
                    AppStatus.USER_TEL = resultSet.getString(1);
                    Log.d("USER_ID", AppStatus.USER_ID);
                    Log.d("USER_TEL", AppStatus.USER_TEL);
                    Intent intent = new Intent(getApplicationContext(), AppMain.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, 2000);
    }


    public void showOnlineDialog(){
        builder.setTitle("สมัครสมาชิก");
        builder.setView(txtTelnum);
        builder.setPositiveButton("สมัครสมาชิก", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String telNum = txtTelnum.getText().toString();
                if(!Validator.isTelephoneNumber(telNum)){
                    Toast.makeText(getApplicationContext(), "เบอร์โทรศัพทมือถือไม่ถูกต้อง / Invalid Telephone Number", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    JSONObject jsonInput = new JSONObject();
                    jsonInput.put("telnum", telNum);
                    String jsonResult = (new AsyncTask<JSONObject, Void, String>() {

                        @Override
                        protected String doInBackground(JSONObject... params) {
                            Log.d("telnum", params[0].toString());
                            String result = null;
                            HttpPost httpPost = new HttpPost("http://" + AppStatus.IP_ADDRESS + ":" + AppStatus.PORT + "/useradd");
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
                    }).execute(jsonInput).get();

                    JSONObject jsonOutput = new JSONObject(jsonResult);
                    Log.d("jsonResult", jsonResult);
                    if(jsonResult == null){
                        Toast.makeText(getApplicationContext(), "Something Error", Toast.LENGTH_LONG).show();
                        return;
                    }else if(jsonOutput.getString("ServerStatus").equals("success : register success")){
                        AppStatus.USER_TEL = ((JSONObject)jsonOutput.get("UserData")).getString("Telnum");
                        AppStatus.USER_ID = ((JSONObject)jsonOutput.get("UserData")).getString("id");
                    }else if(jsonOutput.getString("ServerStatus").equals("success : already register")){
                        AppStatus.USER_TEL = ((JSONObject)jsonOutput.get("UserData")).getString("Telnum");
                        AppStatus.USER_ID = ((JSONObject)jsonOutput.get("UserData")).getString("id");
                    }else{
                        Toast.makeText(getApplicationContext(), "Something Error", Toast.LENGTH_LONG).show();
                        return;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("USER_ID", AppStatus.USER_ID);
                Log.d("USER_TEL", AppStatus.USER_TEL);
                db.execSQL("INSERT INTO User(ID, TELNUM, ISSYNC) VALUES(?, ?, ?)", new String[]{AppStatus.USER_ID, AppStatus.USER_TEL, "1"});
                Intent intent = new Intent(getApplicationContext(), AppMain.class);
                startActivity(intent);
                finish();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(getApplicationContext(), "แอปพลิเคชั่นไม่สามารถทำงานต่อได้หากไม่สมัครสมาชิก / Application can't continue", Toast.LENGTH_LONG).show();
                finish();
            }
        });
        builder.show();
    }

    public void showOfflineDialog(){
        builder.setTitle("สมัครสมาชิก");
        builder.setView(txtTelnum);
        builder.setPositiveButton("สมัครสมาชิก", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String telNum = txtTelnum.getText().toString();
                if (!Validator.isTelephoneNumber(telNum)) {
                    Toast.makeText(getApplicationContext(), "เบอร์โทรศัพทมือถือไม่ถูกต้อง / Invalid Telephone Number", Toast.LENGTH_LONG).show();
                    return;
                }
                db.execSQL("INSERT INTO User(ID, TELNUM, ISSYNC) VALUES(?, ?, ?)", new String[]{AppStatus.USER_ID, AppStatus.USER_TEL, "0"});
                Intent intent = new Intent(getApplicationContext(), AppMain.class);
                startActivity(intent);
                finish();
            };
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(getApplicationContext(), "แอปพลิเคชั่นไม่สามารถทำงานต่อได้หากไม่สมัครสมาชิก / Application can't continue", Toast.LENGTH_LONG).show();
                finish();
            }
        });
        builder.show();
    }
}
