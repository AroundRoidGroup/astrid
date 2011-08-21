package com.todoroo.astrid.activity;
import android.database.Cursor;
import android.os.Bundle;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.MapActivity;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;
import com.todoroo.astrid.service.TaskService;
@SuppressWarnings("unused")

public class MapFilterActivity extends MapActivity {
    public static final String MAP_EXTRA_TASK = "of"; //$NON-NLS-1$
    private final String TAG = "mapFilterActivity"; //$NON-NLS-1$

    private AdjustedMap mMapView;

    private final LocationService locationService = new LocationService();

    /* identifiers for the overlays in the mapView */
    private static final int SPECIFIC_OVERLAY = 1;
    private static final int TYPE_OVERLAY = 2;
    private static final int PEOPLE_OVERLAY = 3;

    /* overlays' names */
    private static final String OVERLAY_TYPE_NAME = "Type Location";
    private static final String OVERLAY_SPECIFIC_NAME = "Specific Location";
    private static final String OVERLAY_PEOPLE_NAME = "People Location";

    private double mRadius;

    private AroundroidDbAdapter mPeopleDB;

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPeopleDB.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_filter_activity);

        mMapView = (AdjustedMap) findViewById(R.id.mapview);
        mMapView.makeUneditable();

        mPeopleDB = new AroundroidDbAdapter(this);
        mPeopleDB.open();

        mRadius = 100;

        DPoint deviceLocation = mMapView.getDeviceLocation();
        if (deviceLocation != null)
            mMapView.getController().setCenter(Misc.degToGeo(deviceLocation));

        /* enable zoom option */
        mMapView.setBuiltInZoomControls(true);

        mMapView.getController().setZoom(13);

        mMapView.createOverlay(SPECIFIC_OVERLAY, this.getResources().getDrawable(R.drawable.icon_specific), new String[] {
            AdjustedMap.SHOW_TITLE, AdjustedMap.SHOW_ADDRESS
        }, OVERLAY_SPECIFIC_NAME);
        mMapView.createOverlay(TYPE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_type), new String[] {
            AdjustedMap.SHOW_NAME, AdjustedMap.SHOW_AMOUNT_BY_EXTRAS, AdjustedMap.SHOW_TITLE, AdjustedMap.SHOW_ADDRESS
        }, OVERLAY_TYPE_NAME);
        mMapView.createOverlay(PEOPLE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_people), new String[] {
            AdjustedMap.SHOW_NAME, AdjustedMap.SHOW_ADDRESS
        }, OVERLAY_PEOPLE_NAME);

        TaskService taskService = new TaskService();
        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID).where(Criterion.and(TaskCriteria.isActive(),
                TaskCriteria.isVisible())).
                orderBy(SortHelper.defaultTaskOrder()).limit(100));
        try {

            Task task = new Task();
            for (int k = 0; k < cursor.getCount(); k++) {
                cursor.moveToNext();
                task.readFromCursor(cursor);
                Long taskID = task.getId();

        /* adding the locations by SPECIFIC */
        String[] specificLocations = locationService.getLocationsBySpecificAsArray(taskID);

        /* converting from location written as string to DPoint */
        DPoint[] points = new DPoint[specificLocations.length];
        for (int i = 0 ; i < specificLocations.length ; i++)
            points[i] = new DPoint(specificLocations[i]);
        mapFunctions.addLocationSetToMap(mMapView, SPECIFIC_OVERLAY, points, "Specific Location", taskID); //$NON-NLS-1$

        /* adding the locations by KIND */
        String[] tags = locationService.getLocationsByTypeAsArray(taskID);
        mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, tags, Misc.geoToDeg(mMapView.getMapCenter()), mRadius, taskID);

        /* Adding people that are related to the task */
        String[] people = locationService.getLocationsByPeopleAsArray(taskID);
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
        mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY, people, coords, taskID);

            }
        } finally {
            cursor.close();
        }
    }
}


