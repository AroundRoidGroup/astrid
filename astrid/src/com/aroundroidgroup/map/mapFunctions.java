package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import android.database.Cursor;

import com.aroundroidgroup.locationTags.LocationService;
import com.google.android.maps.GeoPoint;
import com.todoroo.andlib.service.ContextManager;

public class mapFunctions {

    public static final int SNIPPET_ADDRESS = 0;
    public static final int SNIPPET_EMAIL = 1;
    public static final int TITLE_BUSINESS_NAME = 2;
    public static final int TITLE_SPECIFIC = 3;
    public static final int TITLE_TASK_NAME = 4;

    public static final int FAILURE = 0;
    public static final int SUCCESS = 1;
    public static final int ALL_GOOD = 2;
    public static final int PARTIAL_GOOD = 3;
    public static final int ALL_BAD = 4;

    public static String getSavedAddressAndUpdate(int latitude, int longitude) {
        LocationsDbAdapter locDB = new LocationsDbAdapter(ContextManager.getContext());
        locDB.open();
        String savedAddr = locDB.fetchByCoordinateAsString(latitude, longitude);
        if (savedAddr == null) {
            DPoint d = Misc.geoToDeg(new GeoPoint(latitude, longitude));
            try {
                savedAddr = Geocoding.reverseGeocoding(d);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (savedAddr == null)
                savedAddr = LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE;
            locDB.createTranslate(latitude, longitude, savedAddr);
            if (savedAddr == LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE)
                savedAddr = d.toString();
        }
        locDB.close();
        return savedAddr;
    }

    public static DPoint getSavedCoordinateAndUpdate(String addr) {
        LocationsDbAdapter locDB = new LocationsDbAdapter(ContextManager.getContext());
        locDB.open();
        DPoint coords = null;
        GeoPoint savedCoordsGP = locDB.fetchByAddressAsString(addr);
        if (savedCoordsGP == null) {
            try {
                coords = Geocoding.geocoding(addr);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (coords == null) {
                locDB.createTranslate(LocationsDbAdapter.DATABASE_COORDINATE_FAILURE_VALUE,
                        LocationsDbAdapter.DATABASE_COORDINATE_FAILURE_VALUE, addr);
            }
            else {
                GeoPoint gp = Misc.degToGeo(coords);
                locDB.createTranslate(gp.getLatitudeE6(), gp.getLongitudeE6(), addr);
            }
        }
        else coords = Misc.geoToDeg(savedCoordsGP);

        locDB.close();
        return coords;
    }

    public static int[] addTagsToMap(AdjustedMap map, int id, String[] locationTypes, double radius, long taskID) {
        if (locationTypes != null && map != null && map.hasOverlayWithID(id)) {
            int i = 0;
            int[] feedback = new int[locationTypes.length];
            GeoPoint center = map.getMapCenter();
            LocationsDbAdapter locationDB = new LocationsDbAdapter(ContextManager.getContext());
            locationDB.open();
            for (String type : locationTypes) {
                List<DPoint> locationByType = (new LocationService()).getLocationsByTypeBlacklist(taskID, type);
                Cursor c = locationDB.fetchByTypeComplex(type, center.getLatitudeE6(), center.getLongitudeE6(), radius);
                if (c == null || !c.moveToFirst()) {
                    if (c != null)
                        c.close();
                    Map<String, DPoint> data = null;
                    try {
                        data = Misc.googlePlacesQuery(type, Misc.geoToDeg(center), radius);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (data == null) {
                        feedback[i++] = FAILURE;
                        continue;
                    }
                    locationDB.createType(type, center.getLatitudeE6(), center.getLongitudeE6(), radius, data);
                    for (Entry<String, DPoint> element : data.entrySet()) {
                        GeoPoint g = Misc.degToGeo(element.getValue());
                        if (!locationByType.contains(g))
                            map.addItemToOverlay(g, element.getKey(), type,
                                    mapFunctions.getSavedAddressAndUpdate(g.getLatitudeE6(), g.getLongitudeE6()), id, taskID, type);
                    }
                    feedback[i++] = SUCCESS;
                    continue;
                }
                while (!c.isAfterLast()) {
                    int lat = c.getInt(c.getColumnIndex(LocationsDbAdapter.KEY_LATITUDE));
                    int lon = c.getInt(c.getColumnIndex(LocationsDbAdapter.KEY_LONGITUDE));
                    String businessName = c.getString(c.getColumnIndex(LocationsDbAdapter.KEY_BUSINESS_NAME));
                    GeoPoint g = new GeoPoint(lat, lon);
                    DPoint d = Misc.geoToDeg(g);
                    if (locationByType.contains(d)) // this coordinate is in the black list, so it wont be added as a location
                        continue;
                    String savedAddr = mapFunctions.getSavedAddressAndUpdate(lat, lon);
                    map.addItemToOverlay(g, businessName, type, savedAddr, id, taskID, type);
                    c.moveToNext();
                }
                feedback[i++] = SUCCESS;
                c.close();
            }
            locationDB.close();
            return feedback;
        }
        return new int[] { FAILURE };
    }

    //    public static int[] addTagsToMap(AdjustedMap map, int id, String[] locationTypes, DPoint center, double radius, long taskID) {
    //        if (locationTypes.length == 0)
    //            return new int[0];
    //        LocationsDbAdapter locDB = new LocationsDbAdapter(ContextManager.getContext());
    //        locDB.open();
    //        if (center == null)
    //            return new int[locationTypes.length];
    //
    //        int i = 0;
    //        int[] feedback = new int[locationTypes.length];
    //        GeoPoint gp = Misc.degToGeo(center);
    //        for (String type : locationTypes) {
    //            Map<String, DPoint> kindLocations = null;
    //            try {
    //                Cursor c = locDB.fetchByTypeComplex(type, gp.getLatitudeE6(), gp.getLongitudeE6(), radius);
    //                //TODO although c is not empty, it enters to createType :(
    //                if (c == null || !c.moveToFirst()) {
    //                    if (c != null)
    //                        c.close();
    //                    kindLocations = Misc.googlePlacesQuery(type, center, radius);
    //                    if (!kindLocations.isEmpty()) {
    //                        feedback[i] = SUCCESS;
    //                        locDB.createType(type, gp.getLatitudeE6(), gp.getLongitudeE6(), radius, kindLocations);
    //                    }
    //                    else {
    //                        feedback[i] = FAILURE;
    //                        continue;
    //                    }
    //                    /* running on all the tags (bank, post-office, ATM, etc...) */
    //                    for (Map.Entry<String, DPoint> p : kindLocations.entrySet()) {
    //                        GeoPoint geoP = Misc.degToGeo(p.getValue());
    //                        String savedAddr = locDB.fetchByCoordinateAsString(geoP.getLatitudeE6(), geoP.getLongitudeE6());
    //                        if (savedAddr == null) {
    //                            savedAddr = Geocoding.reverseGeocoding(p.getValue());
    //                            if (savedAddr == null)
    //                                savedAddr = LocationsDbAdapter.DATABASE_ADDRESS_GEOCODE_FAILURE;
    //                            locDB.createTranslate(geoP.getLatitudeE6(), geoP.getLongitudeE6(), savedAddr);
    //                        }
    //                        map.addItemToOverlay(Misc.degToGeo(p.getValue()), p.getKey(),
    //                                type, ((savedAddr.equals(LocationsDbAdapter.DATABASE_ADDRESS_GEOCODE_FAILURE))?p.getValue().toString():savedAddr),
    //                                id, taskID, type);
    //                    }
    //                }
    //                else {
    //                    /* having a record of this type in DB */
    //                    while (!c.isAfterLast()) {
    //                        int lat = c.getInt(c.getColumnIndex(LocationsDbAdapter.KEY_LATITUDE));
    //                        int lon = c.getInt(c.getColumnIndex(LocationsDbAdapter.KEY_LONGITUDE));
    //                        String savedAddr = locDB.fetchByCoordinateAsString(lat, lon);
    //                        DPoint dp = Misc.geoToDeg(new GeoPoint(lat, lon));
    //                        if (savedAddr == null) {
    //                            savedAddr = Geocoding.reverseGeocoding(dp);
    //                            if (savedAddr == null)
    //                                savedAddr = LocationsDbAdapter.DATABASE_ADDRESS_GEOCODE_FAILURE;
    //                            locDB.createTranslate(lat, lon, savedAddr);
    //                        }
    //                        map.addItemToOverlay(Misc.degToGeo(dp),
    //                                c.getString(c.getColumnIndex(LocationsDbAdapter.KEY_BUSINESS_NAME)),
    //                                type,
    //                                savedAddr,
    //                                id,
    //                                taskID,
    //                                type);
    //                        c.moveToNext();
    //                    }
    //                    feedback[i] = 1;
    //                    c.close();
    //                }
    //            } catch (IOException e) {
    //                e.printStackTrace();
    //            } catch (JSONException e) {
    //                e.printStackTrace();
    //            }
    //            i++;
    //        }
    //        locDB.close();
    //        return feedback;
    //    }

    public static void addLocationSetToMap(AdjustedMap map, int id, DPoint[] locations, String title, long taskID) {
        LocationsDbAdapter locDB = new LocationsDbAdapter(ContextManager.getContext());
        locDB.open();
        for (DPoint d : locations) {
            GeoPoint gp = Misc.degToGeo(d);
            String savedAddr = locDB.fetchByCoordinateAsString(gp.getLatitudeE6(), gp.getLongitudeE6());
            if (savedAddr == null) {
                try {
                    savedAddr = Geocoding.reverseGeocoding(d);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (savedAddr == null)
                    locDB.createTranslate(gp.getLatitudeE6(), gp.getLongitudeE6(), LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE);
                else locDB.createTranslate(gp.getLatitudeE6(), gp.getLongitudeE6(), savedAddr);
            }
            map.addItemToOverlay(Misc.degToGeo(d), title, d.toString(), savedAddr, id, taskID, null);
        }

    }

    public static int[] addPeopleToMap(AdjustedMap map, int id, String[] people, DPoint[] locations, long taskID) {
        if (map == null || people.length != locations.length)
            return new int[0];
        int[] feedback = new int[people.length];
        for (int i = 0 ; i < people.length ; i++) {
            if (people[i] == null || locations[i] == null || locations[i].isNaN() || locations[i] == null) {
                feedback[i] = FAILURE;
                continue;
            }
            map.addItemToOverlay(Misc.degToGeo(locations[i]), people[i], people[i], people[i], id, taskID, people[i]);
            feedback[i] = SUCCESS;
        }
        return feedback;
    }

    public static int degreeOfSuccess(int[] feedback) {
        if (feedback == null)
            return FAILURE;
        boolean notAllGood = false;
        boolean notAllBad = false;
        for (int i = 0 ; i < feedback.length ; i++) {
            if (feedback[i] != SUCCESS)
                notAllGood = true;
            if (feedback[i] != FAILURE)
                notAllBad = true;
        }
        if (!notAllBad)
            return ALL_BAD;
        if (!notAllGood)
            return ALL_GOOD;
        return PARTIAL_GOOD;
    }

}
