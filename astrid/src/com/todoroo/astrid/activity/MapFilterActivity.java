package com.todoroo.astrid.activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.MapActivity;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;
import com.todoroo.astrid.service.TaskService;

public class MapFilterActivity extends MapActivity {

    private double mRadius;
    private AdjustedMap mMapView;
    private AroundroidDbAdapter mPeopleDB;
    private final LocationService mLocationService = new LocationService();

    /* identifiers for the overlays in the mapView */
    private static final int SPECIFIC_OVERLAY = 1;
    private static final int TYPE_OVERLAY = 2;
    private static final int PEOPLE_OVERLAY = 3;

    /* overlays' names */
    private static final String OVERLAY_TYPE_NAME = "Type Location"; //$NON-NLS-1$
    private static final String OVERLAY_SPECIFIC_NAME = "Specific Location"; //$NON-NLS-1$
    private static final String OVERLAY_PEOPLE_NAME = "People Location"; //$NON-NLS-1$

    private int mTaskNumber;
    private int mLocationNumber;

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPeopleDB.close();
    }

    private class DeviceLocation extends AbstractAction {

        public DeviceLocation() {
            super(R.drawable.ic_menu_mylocation);
        }

        @Override
        public void performAction(View view) {
            DPoint deviceLocation = mMapView.getDeviceLocation();
            if (deviceLocation != null)
                mMapView.getController().setCenter(Misc.degToGeo(deviceLocation));
            return;
        }

    }
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

    private class InformationOnLocations extends AbstractAction {

        public InformationOnLocations() {
            super(R.drawable.ic_menu_info);
        }

        @Override
        public void performAction(View view) {
            Resources r = getResources();
            String locationsCount = ""; //$NON-NLS-1$
            if (mMapView.getOverlaySize(SPECIFIC_OVERLAY) > 0)
                locationsCount += "Specifics: " + mMapView.getOverlaySize(SPECIFIC_OVERLAY) + " "; //$NON-NLS-1$ //$NON-NLS-2$
            if (mMapView.getOverlaySize(TYPE_OVERLAY) > 0)
                locationsCount += "Types: " + mMapView.getOverlaySize(TYPE_OVERLAY) + " "; //$NON-NLS-1$ //$NON-NLS-2$
            if (mMapView.getOverlaySize(PEOPLE_OVERLAY) > 0)
                locationsCount += "People: " + mMapView.getOverlaySize(PEOPLE_OVERLAY); //$NON-NLS-1$

            AlertDialog dialog = new AlertDialog.Builder(MapFilterActivity.this).create();
            dialog.setIcon(android.R.drawable.ic_dialog_alert);
            dialog.setTitle(r.getString(R.string.map_alert_dialog_title));
            dialog.setMessage(r.getString(R.string.alert_dialog_tasks) + mTaskNumber + r.getString(R.string.alert_dialog_locations) + mLocationNumber);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.DLG_ok),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dg, int which) {
                    return;
                }
            });
            dialog.show();
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Resources r = getResources();
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
            Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, OVERLAY_SPECIFIC_NAME);
        mMapView.createOverlay(TYPE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_type), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_AMOUNT_BY_EXTRAS, Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, OVERLAY_TYPE_NAME);
        mMapView.createOverlay(PEOPLE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_people), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS
        }, OVERLAY_PEOPLE_NAME);

        TaskService taskService = new TaskService();
        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID).where(Criterion.and(TaskCriteria.isActive(),
                TaskCriteria.isVisible())).
                orderBy(SortHelper.defaultTaskOrder()).limit(100));
        mTaskNumber = cursor.getCount();
        try {

            Task task = new Task();
            for (int k = 0; k < cursor.getCount(); k++) {
                cursor.moveToNext();
                task.readFromCursor(cursor);
                Long taskID = task.getId();

                /* adding the locations by SPECIFIC */
                String[] specificLocations = mLocationService.getLocationsBySpecificAsArray(taskID);

                if (specificLocations != null) {
                    mLocationNumber += specificLocations.length;
                    /* converting from location written as string to DPoint */
                    DPoint[] points = new DPoint[specificLocations.length];
                    for (int i = 0 ; i < specificLocations.length ; i++)
                        points[i] = new DPoint(specificLocations[i]);
                    mapFunctions.addLocationSetToMap(mMapView, SPECIFIC_OVERLAY, points, "Specific Location", taskID); //$NON-NLS-1$
                }
                /* adding the locations by KIND */
                String[] tags = mLocationService.getLocationsByTypeAsArray(taskID);
                if (tags != null) {
                    mLocationNumber += tags.length;
                    mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, tags, mRadius, taskID);
                }

                /* Adding people that are related to the task */
                String[] people = mLocationService.getLocationsByPeopleAsArray(taskID);
                if (people != null) {
                    mLocationNumber += people.length;
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

            }
        } finally {
            cursor.close();
        }

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle(r.getString(R.string.location_filter_title));

        actionBar.addAction(new InformationOnLocations());
        actionBar.addAction(new DeviceLocation());
    }
}


