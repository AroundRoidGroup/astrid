package com.aroundroidgroup.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Geocoding {
    public static DPoint geocoding(String address) throws IOException, JSONException {
        URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?" + //$NON-NLS-1$
                "address=" + webString(address) + "&sensor=true");  //$NON-NLS-1$//$NON-NLS-2$
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000000);
        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while((line = reader.readLine()) != null)
            builder.append(line);

        reader.close();
        JSONObject json = new JSONObject(builder.toString());
        if (json.getString("status").equalsIgnoreCase("ok") == true) { //$NON-NLS-1$ //$NON-NLS-2$
            JSONArray results = json.getJSONArray("results"); //$NON-NLS-1$
            if (results != null) {
                JSONObject firstResult = results.getJSONObject(0);
                JSONObject geometry = firstResult.getJSONObject("geometry"); //$NON-NLS-1$
                if (geometry != null) {
                    JSONObject location = geometry.getJSONObject("location"); //$NON-NLS-1$
                    if (location != null)
                        return new DPoint(location.getDouble("lat"), location.getDouble("lng")); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        return null;
    }

    public static String reverseGeocoding(DPoint coordinate) throws IOException, JSONException {
        URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?" + //$NON-NLS-1$
                "latlng=" + coordinate.getX() + "," + coordinate.getY() +  //$NON-NLS-1$//$NON-NLS-2$
        "&sensor=true"); //$NON-NLS-1$
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000000);

        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while((line = reader.readLine()) != null)
            builder.append(line);

        reader.close();
        JSONObject json = new JSONObject(builder.toString());
            /* checking for successful query */
            if (json.getString("status").equalsIgnoreCase("ok") == true) { //$NON-NLS-1$ //$NON-NLS-2$
                JSONArray results = json.getJSONArray("results"); //$NON-NLS-1$
                if (results != null) {
                    JSONObject firstResult = results.getJSONObject(0);
                    return firstResult.getString("formatted_address"); //$NON-NLS-1$
                }
            }

        return null;
    }

    private static String webString(String str) {
        return new String(str.replace(' ', '+'));
    }
}
