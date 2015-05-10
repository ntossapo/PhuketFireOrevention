package com.example.ntossapo.phuketprevention.Functional;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Patchara on 6/1/2558.
 */
public class GoogleMapDirectionsFunctional {
    public static String makeUrl(LatLng origin, LatLng destination){
        StringBuilder url = new StringBuilder();
        url.append("http://maps.googleapis.com/maps/api/directions/json");

        url.append("?origin=");
        url.append(Double.toString(origin.latitude));
        url.append(",");
        url.append(Double.toString(origin.longitude));

        url.append("&destination=");
        url.append(Double.toString(destination.latitude));
        url.append(",");
        url.append(Double.toString(destination.longitude));

        url.append("&sensor=false&mode=driving&alternatives=true");

        return url.toString();
    }

    public static String getJsonDirection(String url){
       /* try {
            return (new HttpAsyncTaskGetDirection()).execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }*/
        return null;
    }

    public static List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }
}
