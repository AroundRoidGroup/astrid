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
    public static String reverseGeocoding(DPoint coordinate) throws IOException, JSONException {
        URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?" +
        		"latlng=" + coordinate.getX() + "," + coordinate.getY() +
        				"&sensor=true");
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
        if (json != null) {
            /* checking for successful query */
            if (json.getString("status").equalsIgnoreCase("ok") == true) {
                JSONArray results = json.getJSONArray("results");
                if (results != null) {
                    JSONObject firstResult = results.getJSONObject(0);
                    return firstResult.getString("formatted_address");
                }
            }
        }
        return null;
    }
}