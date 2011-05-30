package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;

public class mapFunctions {

    public static final int SNIPPET_ADDRESS = 0;
    public static final int SNIPPET_EMAIL = 1;
    public static final int TITLE_BUSINESS_NAME = 2;
    public static final int TITLE_SPECIFIC = 3;
    public static final int TITLE_TASK_NAME = 4;

    public static int[] addTagsToMap(AdjustedMap map, String overlayUniqueName, String[] locationTypes, double radius) {
        DPoint deviceLocation = map.getDeviceLocation();
        if (deviceLocation == null)
            return new int[locationTypes.length];

        int i = 0;
        int[] feedback = new int[locationTypes.length];
        for (String type : locationTypes) {
            try {
                Map<String, DPoint> kindLocations = Misc.googlePlacesQuery(type, deviceLocation, radius);
                if (!kindLocations.isEmpty())
                    feedback[i] = 1;
                /* running on all the tags (bank, post-office, ATM, etc...) */
                for (Map.Entry<String, DPoint> p : kindLocations.entrySet())
                    map.addItemToOverlay(Misc.degToGeo(p.getValue()), p.getKey(),
                            type, Geocoding.reverseGeocoding(p.getValue()),
                            overlayUniqueName);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
        return feedback;
    }

    public static void addLocationSetToMap(AdjustedMap map, String overlayUniqueName, DPoint[] locations, String title) {
        for (DPoint d : locations)
            map.addItemToOverlay(Misc.degToGeo(d), title, d.toString(), null, overlayUniqueName);
    }

    public static void addPeopleToMap(AdjustedMap map, String overlayUniqueName, String[] people, DPoint[] locations) {
        for (int i = 0 ; i < people.length ; i++)
            map.addItemToOverlay(Misc.degToGeo(locations[i]), people[i], people[i], people[i], overlayUniqueName);
    }
}
