package com.todoroo.astrid.activity;
import android.os.Bundle;

import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.MapActivity;
import com.timsu.astrid.R;
@SuppressWarnings("unused")

public class MapFilterActivity extends MapActivity {
    public static final String MAP_EXTRA_TASK = "of"; //$NON-NLS-1$
    private final String TAG = "mapFilterActivity"; //$NON-NLS-1$

    private AdjustedMap mapView;

    private final LocationService locationService = new LocationService();

    private static final int KIND = 1;
    private static final int SPECIFIC = 2;
    private static final int PEOPLE = 3;

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_filter_activity);

        mapView = (AdjustedMap) findViewById(R.id.mapview);

        DPoint deviceLocation = mapView.getDeviceLocation();
        if (deviceLocation != null)
            mapView.getController().setCenter(Misc.degToGeo(deviceLocation));

        /* enable zoom option */
        mapView.setBuiltInZoomControls(true);

        mapView.getController().setZoom(13);

        /* adding the locations by SPECIFIC */
        String[] specificLocations = locationService.getAllLocationsBySpecific();
        if (specificLocations.length > 0) {
            mapView.createOverlay(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_pp));

            /* converting from location written as string to DPoint*/
            DPoint[] points = new DPoint[specificLocations.length];
            for (int i = 0 ; i < specificLocations.length ; i++)
                points[i] = new DPoint(specificLocations[i]);

            mapFunctions.addLocationSetToMap(mapView, AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME, points, "Specific Location"); //$NON-NLS-1$
        }

        /* adding the locations by KIND */
        String[] tags = locationService.getAllLocationsByType();
        if (tags.length > 0) {
            mapView.createOverlay(AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_32));
            mapFunctions.addTagsToMap(mapView, AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, tags, 500.0);
        }

        /* adding the people locations */
        String[] people = locationService.getAllLocationsByPeople();
        if (people.length > 0) {
            mapView.createOverlay(AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_people));
            mapFunctions.addPeopleToMap(mapView, AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, people);
        }
    }
}


