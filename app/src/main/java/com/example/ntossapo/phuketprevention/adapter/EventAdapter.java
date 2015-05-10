package com.example.ntossapo.phuketprevention.adapter;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ntossapo.phuketprevention.AppMain;
import com.example.ntossapo.phuketprevention.AppStatus;
import com.example.ntossapo.phuketprevention.Command;
import com.example.ntossapo.phuketprevention.R;
import com.example.ntossapo.phuketprevention.holderadapter.HolderListEventAdapter;
import com.example.ntossapo.phuketprevention.type.Event;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Tossapon on 6/5/2558.
 */
public class EventAdapter extends BaseAdapter {
    private ArrayList<Event> arrayList;
    private Context context;
    private LayoutInflater inflater;

    public EventAdapter(ArrayList<Event> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return arrayList.get(position).getEvId();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final HolderListEventAdapter holderListEventAdapter;

        if(convertView == null){
            convertView = inflater.inflate(R.layout.listview_event_item, null);
            holderListEventAdapter = new HolderListEventAdapter();
            holderListEventAdapter.evImage = (ImageView) convertView.findViewById(R.id.lv_ev_it_image);
            holderListEventAdapter.evTextType = (TextView) convertView.findViewById(R.id.lv_ev_it_texttype);
            holderListEventAdapter.evTextAddr = (TextView) convertView.findViewById(R.id.lv_ev_it_textaddr);
            holderListEventAdapter.evTextStatus = (TextView) convertView.findViewById(R.id.lv_ev_it_textstatus);

            convertView.setTag(holderListEventAdapter);
        }else{
            holderListEventAdapter = (HolderListEventAdapter) convertView.getTag();
        }

        holderListEventAdapter.evImage.setImageBitmap(arrayList.get(position).getImage());
        holderListEventAdapter.evTextStatus.setText("สถานการณ์ : " + arrayList.get(position).getStatus());

        if(!arrayList.get(position).getAddress().equals(""))
            if(arrayList.get(position).getAddress().length() > 50)
                holderListEventAdapter.evTextAddr.setText(arrayList.get(position).getAddress().substring(0, 50)+"...");
            else
                holderListEventAdapter.evTextAddr.setText(arrayList.get(position).getAddress());
        else
            holderListEventAdapter.evTextAddr.setText(arrayList.get(position).getAddress());

        if(!arrayList.get(position).getPlace().equals(""))
            if(arrayList.get(position).getPlace().length() > 50)
                holderListEventAdapter.evTextType.setText(arrayList.get(position).getPlace().substring(0, 50)+"...");
            else
                holderListEventAdapter.evTextType.setText(arrayList.get(position).getPlace());
        else
            holderListEventAdapter.evTextType.setText(arrayList.get(position).getAddress());



        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton("รายละเอียด", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        LayoutInflater inflater = ((AppMain)context).getLayoutInflater();
                        View v = inflater.inflate(R.layout.dialog_accidentdetail, null);
                        ImageView iv = (ImageView) v.findViewById(R.id.dialog_ed_image);
                        EditText ep = (EditText) v.findViewById(R.id.dialog_ed_place);
                        EditText ed = (EditText) v.findViewById(R.id.dialog_ed_desc);
                        EditText en = (EditText) v.findViewById(R.id.dialog_ed_name);
                        EditText et = (EditText) v.findViewById(R.id.dialog_ed_tel);
                        EditText es = (EditText) v.findViewById(R.id.dialog_ed_status);
                        iv.setImageBitmap(arrayList.get(position).getImage());
                        ep.setHint(arrayList.get(position).getPlace());
                        ed.setHint(arrayList.get(position).getDescript());
                        en.setHint(arrayList.get(position).getName());
                        et.setHint(arrayList.get(position).getTel());
                        es.setHint(arrayList.get(position).getStatus());
                        builder.setView(v);
                        builder.setPositiveButton("แก้ไข", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        builder.setNegativeButton("ลบ", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    String stringResult = (new AsyncTask<JSONObject, Void, String>() {
                                        @Override
                                        protected String doInBackground(JSONObject... params) {
                                            String result = null;
                                            HttpPost httpPost = new HttpPost("http://" + AppStatus.IP_ADDRESS + ":" + AppStatus.PORT + "/eventremove");

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
                                    }).execute(arrayList.get(position).toJSONObject()).get(4000, TimeUnit.MILLISECONDS);

                                    if(stringResult != null) {
                                        JSONObject json = new JSONObject(stringResult);
                                        Log.d("AppStatus", "Delete event resp : " + json.toString());
                                        if(json.getString("Status").equals("success : remove success")){
                                            arrayList.remove(position);
                                            Command.SyncEvent(context);
                                            Toast.makeText(context, "ลบสำเร็จ", Toast.LENGTH_LONG).show();
                                            ((AppMain) context).ShowEventList();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (TimeoutException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.show();
                    }
                });

                builder.setNegativeButton("นำทาง", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(AppStatus.MODE.equals("offline")){
                            Toast.makeText(context, "คุณอยู่ในระบบออฟไลน์ไม่สามารถนำทางได้", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if(AppStatus.USER_LOCATION_LAT == 0.0 && AppStatus.USER_LOCATION_LONG == 0.0) {
                            Toast.makeText(context, "ระบบจีพีเอสยังไม่ทำงาน", Toast.LENGTH_LONG).show();
                            return;
                        }
                        ((AppMain) context).ShowRouting(arrayList.get(position));
                    }
                });

                /*builder.setNeutralButton("ลบ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });*/
                builder.show();
            }
        });

        return convertView;
    }
}
