package com.example.ntossapo.phuketprevention;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;

import com.example.ntossapo.phuketprevention.type.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by Patchara on 30/12/2557.
 */
public class AppData
{
    private ArrayList<Event> AccidentData = new ArrayList<Event>();

    public ArrayList<Event> getAccidentData() {
        return AccidentData;
    }


    public void setAccidentData(JSONArray json) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Log.d("AppData", "" + json.length());
        for (int i = 0; i < json.length(); i++) {
            try {
                byte[] byteImage = Base64.decode(json.getJSONObject(i).getString("Image").getBytes(), Base64.DEFAULT);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap bmp = BitmapFactory.decodeByteArray(byteImage, 0,  byteImage.length);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bmp , 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                AccidentData.add(
                        new Event(
                                i,
                                json.getJSONObject(i).getString("id"),
                                ((JSONObject)json.getJSONObject(i).get("Pos")).getDouble("Lat"),
                                ((JSONObject)json.getJSONObject(i).get("Pos")).getDouble("Long"),
                                json.getJSONObject(i).getString("Type"),
                                json.getJSONObject(i).getString("Name"),
                                json.getJSONObject(i).getString("Telnum"),
                                json.getJSONObject(i).getString("Desc"),
                                formatter.parse((String) json.getJSONObject(i).get("DateTime")),
                                json.getJSONObject(i).getString("InformerId"),
                                json.getJSONObject(i).getString("Status"),
                                json.getJSONObject(i).getString("Option"),
                                json.getJSONObject(i).getString("Place"),
                                json.getJSONObject(i).getString("Address"),
                                rotatedBitmap
                        ));
            } catch (JSONException e) {
                Log.d("AppData", e.getMessage());
            } catch (ParseException e) {
                Log.d("AppData", e.getMessage());
            }
        }
    }
}