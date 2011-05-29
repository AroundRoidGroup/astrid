package com.aroundroidgroup.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class PlacesLocations {

	JSONObject json;
	URL url;

	String query;
	double lat;
	double lng;

	public PlacesLocations(String query, Location location) throws IOException, JSONException {
		this.query = query;
		this.lat = location.getLatitude();
		this.lng = location.getLongitude();
		url = urlBuilder(query, lat, lng, 0);
		json = urlToJSONObject(url);
	}

	public List<placeInfo> getPlaces() throws JSONException, MalformedURLException, IOException {
		List<placeInfo> list = new ArrayList<placeInfo>();
		if (json != null) {
			JSONObject all = json.getJSONObject("responseData"); //$NON-NLS-1$
			if (all != null) {
				JSONObject extraInfo = all.getJSONObject("cursor"); //$NON-NLS-1$
				if (extraInfo != null) {
					JSONArray pages = extraInfo.getJSONArray("pages"); //$NON-NLS-1$
					if (pages != null)
						for (int k = 0 ; k < pages.length() ; k++)
							list.addAll(jsonToList(urlToJSONObject(urlBuilder(query, lat, lng, k))));
				}
			}
		}
		return list;
	}

    private URL urlBuilder(String q, double latitude, double longtitude, int start) throws MalformedURLException {
        return new URL(
                "http://ajax.googleapis.com/ajax/services/search/local?" + /* basic url */ //$NON-NLS-1$
                "v=1.0&" + /* protocol version number (always 1.0) */ //$NON-NLS-1$
                "q=" + q + "&" + /* query (in this case we want all results */ //$NON-NLS-1$ //$NON-NLS-2$
                "sll=" + latitude + "," + longtitude + "&" + /* search center point (we'll use current device location */ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "rsz=8&" + /* number of results to return per page */ //$NON-NLS-1$
                "start=" + start + //$NON-NLS-1$
                /* "sspn=&" +  a box that the local search should be relative to. */
                //              "key=ABQIAAAAObA0GRIqXQNR793dPFwR8BTZfQpz0IQ9_INffx12OPoBPuPM7hSLpvO3lnM8iCuwWf3V8zc49jSZEQ&" + /* */
                //              "userip=192.168.0.1" + /* IP address of the end-user on whose behalf the request is being made */
        ""); //$NON-NLS-1$
    }

    private JSONObject urlToJSONObject(URL URL) throws IOException, JSONException {
        URLConnection connection = URL.openConnection();
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000000);

        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while((line = reader.readLine()) != null)
            builder.append(line);

        reader.close();
        return new JSONObject(builder.toString());
    }

    private List<placeInfo> jsonToList(JSONObject jObj) throws JSONException {
        List<placeInfo> list = new ArrayList<placeInfo>();
        JSONObject all = jObj.getJSONObject("responseData"); //$NON-NLS-1$
        if (all != null) {
            JSONArray q = all.getJSONArray("results"); //$NON-NLS-1$
            if (q != null) {
                for (int i = 0 ; i < q.length() ; i++) {
                    JSONObject info = q.getJSONObject(i);
                    if (info != null)
                        list.add(new placeInfo(info.getString("title"), //$NON-NLS-1$
                                info.getString("streetAddress"), //$NON-NLS-1$
                                info.getString("region"), //$NON-NLS-1$
                                info.getString("city"), //$NON-NLS-1$
                                info.getString("country"), //$NON-NLS-1$
                                Double.parseDouble(info.getString("lng")), //$NON-NLS-1$
                                Double.parseDouble(info.getString("lat")), //$NON-NLS-1$
                                info.getString("staticMapUrl"), //$NON-NLS-1$
                                info.getString("url"))); //$NON-NLS-1$
                }
            }
        }
        return list;
    }

}
