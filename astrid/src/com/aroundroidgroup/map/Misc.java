package com.aroundroidgroup.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

import com.google.android.maps.GeoPoint;

public class Misc {

    public static final String[] types = {"accounting", "airport", "amusement park", "aquarium", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "art gallery", "atm", "bakery", "bank", "bar", "beauty salon", "bicycle store", "book store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
        "bowling alley", "bus station", "cafe", "campground", "car dealer", "car rental", "car repair", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "car wash", "casino", "cemetery", "church", "city hall", "clothing store", "convenience store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "courthouse", "dentist", "department store", "doctor", "electrician", "electronics store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "embassy", "establishment", "finance", "fire station", "florist", "food", "funeral home", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "furniture store", "gas station", "general contractor", "geocode", "grocery or supermarket", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "gym", "hair care", "hardware store", "health", "hindu temple", "home goods store", "hospital", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "insurance agency", "jewelry store", "laundry", "lawyer", "library", "liquor store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "local government office", "locksmith", "lodging", "meal delivery", "meal takeaway", "mosque", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "movie rental", "movie theater", "moving company", "museum", "night club", "painter", "park", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "parking", "pet store", "pharmacy", "physiotherapist", "place of worship", "plumber", "police", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "post office", "real estate agency", "restaurant", "roofing contractor", "rv park", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "shoe store", "shopping mall", "spa", "stadium", "storage", "store", "subway station", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "synagogue", "taxi stand", "train station", "travel agency", "university", "veterinary care", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "zoo"};     //$NON-NLS-1$

    public static final String locationsDelimiter = "@@@"; //$NON-NLS-1$

    public static int startTypeIndex(String [] data) {
        for (int i = 0 ; i < data.length ; i++)
            if (data[i].equals(locationsDelimiter))
                return i;
        return -1;
    }

    public static int startPeopleIndex(String [] data) {
        boolean flag = false;
        for (int i = 0 ; i < data.length ; i++) {
            if (flag == true && data[i].equals(locationsDelimiter))
                return i;
            if (flag == false && data[i].equals(locationsDelimiter))
                flag = true;
        }
        return -1;
    }

    public static boolean isType(String type) {
        for (String s : types)
            if (s.equalsIgnoreCase(type))
                return true;
        return false;
    }

    /* the service returns up to 20 results. */
    public static Map<String, DPoint> googlePlacesQuery(String type, DPoint location, double radius) throws IOException, JSONException {
        URL googlePlacesURL = new URL("https://maps.googleapis.com/maps/api/place/search/json?" + //$NON-NLS-1$
                "location=" + location.getX() + "," + location.getY() + //$NON-NLS-1$ //$NON-NLS-2$
                "&radius=" + radius + //$NON-NLS-1$
                "&types=" + type + //$NON-NLS-1$
                "&sensor=false" + //$NON-NLS-1$
                "&key=AIzaSyAqaQJGYnY4lOXZN-nqIS0EEkmlPBIGZFs"); //$NON-NLS-1$
        /* connecting to the URL */
        URLConnection googlePlacesCon = googlePlacesURL.openConnection();
        googlePlacesCon.setConnectTimeout(10000000); /* 10 seconds */
        googlePlacesCon.setDoOutput(true);

        /* reading the response of the URL */
        BufferedReader in = new BufferedReader(new InputStreamReader(googlePlacesCon.getInputStream()));
        String decodedString;
        StringBuilder builder = new StringBuilder();
        while ((decodedString = in.readLine()) != null)
            builder.append(decodedString);
        in.close();
        JSONObject queryResponse = new JSONObject(builder.toString());

        /* parsing the JSON object to extract places information */
        Map<String, DPoint> results = new HashMap<String, DPoint>();
        String queryStatus = queryResponse.getString("status"); //$NON-NLS-1$
        if (queryStatus != null) {
            if (queryStatus.equals("OK") == true) { //$NON-NLS-1$
                JSONArray queryResults = queryResponse.getJSONArray("results"); //$NON-NLS-1$
                if (queryResults != null) {
                    for (int i = 0 ; i < queryResults.length() ; i++) {
                        JSONObject place = queryResults.getJSONObject(i);
                        if (place != null) {
                            String placeName = place.getString("name"); //$NON-NLS-1$
                            JSONObject subObject = place.getJSONObject("geometry"); //$NON-NLS-1$
                            if (subObject != null) {
                                JSONObject subSubObject = subObject.getJSONObject("location"); //$NON-NLS-1$
                                if (subSubObject != null) {
                                    DPoint placeLocation = new DPoint(subSubObject.getString("lat") + "," + //$NON-NLS-1$ //$NON-NLS-2$
                                            subSubObject.getString("lng")); //$NON-NLS-1$
                                    if (!placeLocation.isNaN())
                                        results.put(placeName, placeLocation);
                                }
                                else {
                                    /* bad JSON object structure */
                                }
                            }
                            else {
                                /* bad JSON object structure */
                            }
                        }
                        else {
                            /* bad JSON object structure */
                        }
                    }
                }
                else {
                    /* bad JSON object structure */
                }
            }
            else {
                /* query response's status isn't OK */
            }
        }
        return results;
    }

    public static List<String> googleAutoCompleteQuery(String text, DPoint location, double radius) throws IOException, JSONException {
        URL googleAutoCompleteURL = new URL("https://maps.googleapis.com/maps/api/place/autocomplete/json?" + //$NON-NLS-1$
                "input=" + text + //$NON-NLS-1$
                "&types=geocode" + //$NON-NLS-1$
                "&location=" + location.getX() + "," + location.getY() + //$NON-NLS-1$ //$NON-NLS-2$
                "&radius=" + radius + //$NON-NLS-1$
                "&sensor=false" + //$NON-NLS-1$
                "&key=AIzaSyAqaQJGYnY4lOXZN-nqIS0EEkmlPBIGZFs"); //$NON-NLS-1$

        /* connecting to the URL */
        URLConnection googleAutoCompleteCon = googleAutoCompleteURL.openConnection();
        googleAutoCompleteCon.setConnectTimeout(10000000); /* 10 seconds */
        googleAutoCompleteCon.setDoOutput(true);

        /* reading the response of the URL */
        BufferedReader in = new BufferedReader(new InputStreamReader(googleAutoCompleteCon.getInputStream()));
        String decodedString;
        StringBuilder builder = new StringBuilder();
        while ((decodedString = in.readLine()) != null)
            builder.append(decodedString);
        in.close();
        JSONObject queryResponse = new JSONObject(builder.toString());

        /* parsing the JSON object to extract places information */
        List<String> results = new ArrayList<String>();
        String queryStatus = queryResponse.getString("status"); //$NON-NLS-1$
        if (queryStatus != null) {
            if (queryStatus.equals("OK") == true) { //$NON-NLS-1$
                JSONArray queryResults = queryResponse.getJSONArray("predictions"); //$NON-NLS-1$
                if (queryResults != null) {
                    for (int i = 0 ; i < queryResults.length() ; i++) {
                        JSONObject predictions = queryResults.getJSONObject(i);
                        if (predictions != null) {
                            String description = predictions.getString("description"); //$NON-NLS-1$
                            if (description != null)
                                results.add(description);
                            else {
                                /* bad JSON object structure */
                            }
                        }
                        else {
                            /* bad JSON object structure */
                        }
                    }
                }
                else {
                    /* bad JSON object structure */
                }
            }
            else {
                // response's status isn't OK
            }
        }
        return results;
    }

    public static GeoPoint degToGeo(DPoint dp) {
        if (dp == null)
            return null;
        return new GeoPoint((int)(dp.getX() * 1000000), (int)(dp.getY() * 1000000));
    }

    public static GeoPoint locToGeo(Location l) {
        return new GeoPoint((int)(l.getLatitude() * 1000000), (int)(l.getLongitude() * 1000000));
    }

    public static DPoint geoToDeg(GeoPoint gp) {
        return new DPoint((double)gp.getLatitudeE6() / 1000000, (double)gp.getLongitudeE6() / 1000000);
    }

    public static boolean similarDegs(DPoint d1, DPoint d2) {
        double deltaX = Math.abs(d1.getX() - d2.getX());
        double deltaY = Math.abs(d1.getY() - d2.getY());
        return (deltaX <= 0.000001 || deltaY <= 0.000001);
    }

    public static double distance(DPoint p1, DPoint p2) {
        if (p1 == null || p2 == null)
            return -1;
        double latDelta = Math.toRadians(p2.getX() - p1.getX());
        double lngDelta = Math.toRadians(p2.getY() - p1.getY());
        double a = Math.pow(Math.sin(latDelta / 2), 2) + Math.cos(Math.toRadians(p1.getY())) * Math.cos(Math.toRadians(p2.getY())) * Math.pow(Math.sin(lngDelta / 2), 2);
        double b = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return b * 6371;
    }

    public static String[] extractType(String text) {
        for (int i = 0 ; i < text.length() ; i++)
            if (Character.isDigit(text.charAt(i)))
                return new String[] { text.substring(0, i), text.substring(i) };
        return new String[] { text, text.substring(text.length()) };
    }

    public static String[] ListToArray(List<String> lst) {
        String[] arr = new String[lst.size()];
        int i = 0;
        for (String s : lst)
            arr[i++] = s;
        return arr;
    }

    public static DPoint[] ListToArrayDPoint(List<DPoint> lst) {
        DPoint[] arr = new DPoint[lst.size()];
        int i = 0;
        for (DPoint s : lst)
            arr[i++] = s;
        return arr;
    }
}
