package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.Geocoding;
import com.aroundroidgroup.map.LocationsDbAdapter;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;

public class MapLocationActivity extends MapActivity implements OnZoomListener  {

    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task mCurrentTask = null;
    private long mTaskID;
    private AdjustedMap mMapView;
    private final LocationService locationService = new LocationService();
    private double mRadius;

    /* identifiers for the overlays in the mapView */
    private static final int SPECIFIC_OVERLAY = 1;
    private static final int TYPE_OVERLAY = 2;
    private static final int PEOPLE_OVERLAY = 3;

    /* overlays' names */
    private static final String OVERLAY_TYPE_NAME = "Type Location";
    private static final String OVERLAY_SPECIFIC_NAME = "Specific Location";
    private static final String OVERLAY_PEOPLE_NAME = "People Location";

    private AroundroidDbAdapter mPeopleDB;
    private LocationsDbAdapter mLocationDB;

    private TextView mTaskTitleTV;
    private TextView mTaskLocationsCountTV;

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public void onVisibilityChanged(boolean arg0) {
        return;
    }

    @Override
    public void onZoom(boolean arg0) {
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPeopleDB.close();
        mLocationDB.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_of_task);

        mTaskTitleTV = (TextView)findViewById(R.id.takeTitle);
        mTaskLocationsCountTV = (TextView)findViewById(R.id.searchResults);
        mMapView = (AdjustedMap) findViewById(R.id.mapview);
        mMapView.makeUneditable();

        MapController mapController = mMapView.getController();
        DPoint deviceLocation = mMapView.getDeviceLocation();

        mPeopleDB = new AroundroidDbAdapter(this);
        mPeopleDB.open();
        mLocationDB = new LocationsDbAdapter(this);
        mLocationDB.open();
        mRadius = 100;


        /* Centralizing the map to the current (to be more accurate, the last) location of the device */
        if (deviceLocation != null)
            mapController.setCenter(Misc.degToGeo(deviceLocation));

        /* Enable zoom option */
        mMapView.setBuiltInZoomControls(true);
        mapController = mMapView.getController();
        mapController.setZoom(18);

        /* Receiving task from the previous activity and extracting the tags from it */
        Bundle b = getIntent().getExtras();
        mCurrentTask = (Task) b.getParcelable(MAP_EXTRA_TASK);
        mTaskID = mCurrentTask.getId();

        /* Setting up the overlay system which will allow us to add drawable object that will mark */
        /* LocationsByType and/or SpecificLocation and/or People */

        mMapView.createOverlay(SPECIFIC_OVERLAY, this.getResources().getDrawable(R.drawable.icon_specific), new String[] {
            Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, OVERLAY_SPECIFIC_NAME);
        mMapView.createOverlay(TYPE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_type), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_AMOUNT_BY_EXTRAS, Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, OVERLAY_TYPE_NAME);
        mMapView.createOverlay(PEOPLE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_people), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS
        }, OVERLAY_PEOPLE_NAME);

        /* Adding people that are related to the task */
        String[] people = locationService.getLocationsByPeopleAsArray(mTaskID);
        DPoint[] coords = new DPoint[people.length];
        for (int i = 0 ; i < people.length ; i++) {
            Cursor c = mPeopleDB.fetchByMail(people[i]);
            if (c != null && c.moveToFirst()) {
                Double lat = c.getDouble(c.getColumnIndex(AroundroidDbAdapter.KEY_LAT));
                Double lon = c.getDouble(c.getColumnIndex(AroundroidDbAdapter.KEY_LON));
                coords[i] = new DPoint(lat, lon);
                c.close();
            }
        }
        mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY, people, coords, mTaskID);

        String[] specificLocations = locationService.getLocationsBySpecificAsArray(mTaskID);
        /* Converting from location written as string to DPoint */
        coords = new DPoint[specificLocations.length];
        for (int i = 0 ; i < specificLocations.length ; i++)
            coords[i] = new DPoint(specificLocations[i]);
        mapFunctions.addLocationSetToMap(mMapView, SPECIFIC_OVERLAY, coords, "Specific Location", mTaskID); //$NON-NLS-1$

        /* If the task is location-based, the following code will add the locations to the map */
        String[] locationTags = locationService.getLocationsByTypeAsArray(mTaskID);
        mainLoop: for (String type : locationTags) {
            List<String> locationByType = new ArrayList<String>();//locationService.getLocations(mTaskID, type);
            Map<String, DPoint> data = null;
            for (String location : locationByType) {
                GeoPoint geoLocation = Misc.degToGeo(new DPoint(location));
                String savedAddr = mLocationDB.fetchByCoordinateAsString(geoLocation.getLatitudeE6(), geoLocation.getLongitudeE6());
                if (savedAddr == null) {
                    try {
                        savedAddr = Geocoding.reverseGeocoding(new DPoint(location));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (savedAddr == null)
                        savedAddr = LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE;
                    mLocationDB.createTranslate(geoLocation.getLatitudeE6(), geoLocation.getLongitudeE6(), savedAddr);
                    if (savedAddr == LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE)
                        savedAddr = location;
                }
                String savedBusiness = mLocationDB.fetchByCoordinateFromType(type, geoLocation.getLatitudeE6(), geoLocation.getLongitudeE6());
                if (savedBusiness == null) {
                    if (data == null) {
                        try {
                            data = Misc.googlePlacesQuery(type, deviceLocation, mRadius);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (data == null) { /* parse the next Type location */
                            continue mainLoop;
                        }
                        for (Map.Entry<String, DPoint> element : data.entrySet()) {
                            data.put(element.getKey(), new DPoint(element.getValue().toString()));
                        }
                    }
                    DPoint p = new DPoint(location);
                    for (Map.Entry<String, DPoint> pair : data.entrySet()) {
                        DPoint s = pair.getValue();
                        if (Misc.similarDegs(p, s))
                            if (p.toString().equals(pair.getValue().toString()))
                                savedBusiness = pair.getKey();
                    }
                }
                mMapView.addItemToOverlay(Misc.degToGeo(new DPoint(location)), savedBusiness, type, savedAddr, TYPE_OVERLAY, mTaskID, type);
            }
            data = null;
        }
        mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, locationTags, Misc.geoToDeg(mMapView.getMapCenter()), mRadius, mTaskID);

        /* Setting the text-view to hold the task title */
        mTaskTitleTV.setText(mCurrentTask.getValue(Task.TITLE));

        String locationsCount = ""; //$NON-NLS-1$
        if (specificLocations.length > 0)
            locationsCount += "Specifics: " + specificLocations.length + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (locationTags.length > 0)
            locationsCount += "Types: " + locationTags.length + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (people.length > 0)
            locationsCount += "People: " + people.length; //$NON-NLS-1$

        /* Setting the text-view to hold the different locations types */
        mTaskLocationsCountTV.setText(locationsCount);
    }
}
