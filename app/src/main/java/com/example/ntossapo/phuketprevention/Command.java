package com.example.ntossapo.phuketprevention;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Tossapon on 5/5/2558.
 */
public class Command {
    public static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    public static boolean NeedUpdate(Context context){
        try {
            String jsonResult = (new AsyncTask<Void, Void, String>(){
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
            }.execute()).get(2000, TimeUnit.MILLISECONDS);

            if(jsonResult == null){
                AppStatus.MODE = "offline";
                Toast.makeText(context, "Application running offline mode", Toast.LENGTH_LONG).show();
            }else{
                JSONObject jsonObject = new JSONObject(jsonResult);
                if(AppStatus.EVENT_VERSION != ((JSONObject) jsonObject.get("DataVersion")).getInt("EventVersion"))
                    return true;
                if(AppStatus.BLOCK_VERSION != ((JSONObject) jsonObject.get("DataVersion")).getInt("BlockVersion"))
                    return true;
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public static void SyncEvent(Context context) {
        String jsonString = null;
        try {
            jsonString = (new AsyncTask<Void, Void, String>(){
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
            }).execute().get(2000, TimeUnit.MILLISECONDS);
            if(jsonString != null) {
                AppStatus.DATA.getAccidentData().clear();
                AppStatus.DATA.setAccidentData(new JSONArray(jsonString));
            }else{
                Toast.makeText(context, "Internet Connection Problem", Toast.LENGTH_LONG).show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }
}
