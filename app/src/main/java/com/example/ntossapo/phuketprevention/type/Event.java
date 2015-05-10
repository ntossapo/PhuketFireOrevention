package com.example.ntossapo.phuketprevention.type;

import android.graphics.Bitmap;
import android.util.Base64;

import com.example.ntossapo.phuketprevention.AppStatus;
import com.example.ntossapo.phuketprevention.Command;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Patchara on 30/12/2557.
 */
public class Event implements Serializable {
    private int evId;
    private double latitude;
    private double longitude;
    private String id;
    private String informerId;
    private String type;
    private String name;
    private String tel;
    private String descript;
    private String status;
    private String option;
    private Date dateTime;          //format dd/MM/yy HH:mm:ss
    private String place;
    private String address;
    private Bitmap image;

    public Bitmap getImage() {
        return image;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPlace() {
        return place;
    }

    public String getAddress() {
        return address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInformerId() {
        return informerId;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public void setInformerId(String informerId) {
        this.informerId = informerId;
    }

    public int getEvId() {
        return evId;
    }

    public Event(int evId, String id, double lat, double lng, String type, String name, String tel,
                 String desc, Date dateTime, String informerId, String status, String option, String place,
                 String address, Bitmap image){
        this.evId = evId;
        this.id = id;
        this.informerId = informerId;
        this.latitude = lat;
        this.longitude = lng;
        this.type = type;
        this.name = name;
        this.descript = desc;
        this.tel = tel;
        this.dateTime = dateTime;
        this.status = status;
        this.option = option;
        this.place = place;
        this.address = address;
        this.image = image;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public MarkerOptions getMarkerOption(){
        return new MarkerOptions().title(type).snippet(descript).position(new LatLng(latitude, longitude));
    }

    public LatLng getLatLng(){
        return new LatLng(latitude, longitude);
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescript(String descript) {
        this.descript = descript;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescript() {
        return descript;
    }

    public Date getDateTime() {
        return dateTime;
    }
    public JSONObject toJSONObject(){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        this.image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        JSONObject pos = new JSONObject();
        JSONObject result = new JSONObject();

        try {
            pos.put("lat", this.latitude);
            pos.put("long", this.longitude);

            result.put("pos", pos)
                    .put("id", this.id)
                    .put("type", this.type)
                    .put("name", this.name)
                    .put("informerid", AppStatus.USER_ID)
                    .put("telnum", AppStatus.USER_TEL)
                    .put("desc", this.descript)
                    .put("image", Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT))
                    .put("dateTime", Command.formatter.format(this.dateTime))
                    .put("status", this.status)
                    .put("place", this.place)
                    .put("address", this.place)
                    .put("option", this.option);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
