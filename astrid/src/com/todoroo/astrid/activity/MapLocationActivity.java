package com.todoroo.astrid.activity;

import java.net.URL;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;

public class MapLocationActivity extends MapActivity implements OnZoomListener  {

    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task mCurrentTask = null;
    private MapController mapController;
    private AdjustedMap mapView;
    private String[] locationTags;
    private final LocationService locationService = new LocationService();
    static URL u;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_of_task);
        boolean specificTitleToPresent = false;
        boolean kindTitleToPresent = false;

        mapView = (AdjustedMap) findViewById(R.id.mapview);
        mapView.removeDeviceLocation();

        mapController = mapView.getController();
        mapController.setZoom(18);

        /* receiving task from the previous activity and extracting the tags from it */
        Bundle b = getIntent().getExtras();
        mCurrentTask = (Task) b.getParcelable(MAP_EXTRA_TASK);

        /* setting the text-view to hold the task title */
        TextView title = (TextView)findViewById(R.id.takeTitle);
        title.setText(mCurrentTask.getValue(Task.TITLE));

        /* setting up the overlay system which will allow us to add drawable object that will mark */
        /* LocationsByType and/or SpecificLocation and/or People */

        mapView.createOverlay(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_pp));
        mapView.createOverlay(AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_32));
        mapView.createOverlay(AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_people));

        /* adding people that are related to the task */
        String[] people = locationService.getLocationsByPeopleAsArray(mCurrentTask.getId());
        mapFunctions.addPeopleToMap(mapView, AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, people);

      //TODO USERLOCATION
        if (true)
            return;
        DPoint d = new DPoint(1.0,1.0);

            /* Centralizing the map to the current (to be more accurate, the last) location of the device */
            mapController.setCenter(Misc.degToGeo(d));

            /* enable zoom option */
            mapView.setBuiltInZoomControls(true);

            String[] specificLocations = locationService.getLocationsBySpecificAsArray(mCurrentTask.getId());

            /* converting from location written as string to DPoint*/
            DPoint[] points = new DPoint[specificLocations.length];
            for (int i = 0 ; i < specificLocations.length ; i++)
                points[i] = new DPoint(specificLocations[i]);

            mapFunctions.addLocationSetToMap(mapView, AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME, points, "Specific Location"); //$NON-NLS-1$

            /* if the task is location-based, the following code will add the locations to the map */
            locationTags = locationService.getLocationsByTypeAsArray(mCurrentTask.getId());
            int[] returnValues = mapFunctions.addTagsToMap(mapView, AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, locationTags, 500.0);
            int sum = 0;
            for (int i : returnValues)
                sum += i;
            if (sum == returnValues.length)
                Toast.makeText(this, "All types have been added successfully!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            else if (sum == 0)
                Toast.makeText(this, "Failed to add types!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            else Toast.makeText(this, "Only some types have been added!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$



        /* showing to the user how many location were found */
        TextView tv = (TextView)findViewById(R.id.searchResults);
        if (kindTitleToPresent)
            tv.setText(mapView.getOverlaySize(AdjustedMap.KIND_OVERLAY_UNIQUE_NAME) + " results found !"); //$NON-NLS-1$
        if (specificTitleToPresent)
            tv.setText(tv.getText() + " " + mapView.getOverlaySize(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME) + " specifics found !"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
