package com.todoroo.astrid.activity;
import java.util.EventObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.MyEventClassListener;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.GeoPoint;
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
            String locationsCount = ""; //$NON-NLS-1$
            if (mMapView.getOverlaySize(SPECIFIC_OVERLAY) > 0)
                locationsCount += "Specifics: " + mMapView.getOverlaySize(SPECIFIC_OVERLAY) + " "; //$NON-NLS-1$ //$NON-NLS-2$
            if (mMapView.getOverlaySize(TYPE_OVERLAY) > 0)
                locationsCount += "Types: " + mMapView.getOverlaySize(TYPE_OVERLAY) + " "; //$NON-NLS-1$ //$NON-NLS-2$
            if (mMapView.getOverlaySize(PEOPLE_OVERLAY) > 0)
                locationsCount += "People: " + mMapView.getOverlaySize(PEOPLE_OVERLAY); //$NON-NLS-1$

            AlertDialog dialog = new AlertDialog.Builder(MapFilterActivity.this).create();
            dialog.setIcon(android.R.drawable.ic_dialog_alert);
            dialog.setTitle("Information");
            dialog.setMessage("Tasks: " + mTaskNumber + "\nLocations: " + mLocationNumber);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
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
            Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS, Focaccia.SHOW_SNIPPET
        }, OVERLAY_PEOPLE_NAME);

        TodorooCursor<Task> cursor = (new TaskService()).query(Query.select(Task.ID).where(Criterion.and(TaskCriteria.isActive(),
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
                    DPoint[] coords = new DPoint[people.length];
                    for (int i = 0 ; i < people.length ; i++) {
                        Cursor c = mPeopleDB.fetchByMail(people[i]);
                        if (c == null || !c.moveToFirst()) {
                            coords[i] = null;
                            if (c != null)
                                c.close();
                            continue;
                        }
                        FriendProps fp = AroundroidDbAdapter.userToFP(c);
                        if (fp != null) {
                            Double lat = c.getDouble(c.getColumnIndex(AroundroidDbAdapter.KEY_LAT));
                            Double lon = c.getDouble(c.getColumnIndex(AroundroidDbAdapter.KEY_LON));
                            coords[i] = new DPoint(lat, lon);
                        }
                        else coords[i] = null;
                        c.close();

                    }
                    mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY, people, coords, taskID);
                }

            }
        } finally {
            cursor.close();
        }

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle("Places Nearby");

        actionBar.addAction(new InformationOnLocations());
        actionBar.addAction(new DeviceLocation());

        mMapView.addEventListener(new MyEventClassListener() {

            @Override
            public void handleMyEventClassEvent(EventObject e) {
                TodorooCursor<Task> c = (new TaskService()).query(Query.select(Task.ID).where(Criterion.and(TaskCriteria.isActive(),
                        TaskCriteria.isVisible())).
                        orderBy(SortHelper.defaultTaskOrder()).limit(100));
                mTaskNumber = c.getCount();
                try {

                    Task task = new Task();
                    for (int k = 0; k < c.getCount(); k++) {
                        c.moveToNext();
                        mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, mLocationService.getLocationsByTypeAsArray(task.getId()), mRadius, task.getId());
                    }

                } finally {
                    c.close();
                }

            }

        });

        mMapView.setZoomByAllLocations();
    }

    private final Handler mHan = new Handler();
    final int mDelayMillis = 10 * 1000;
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            Toast.makeText(MapFilterActivity.this, "now", Toast.LENGTH_LONG).show();
            /* my code */
            mMapView.clearOverlay(PEOPLE_OVERLAY);
            mMapView.invalidate();
            TodorooCursor<Task> c = (new TaskService()).query(Query.select(Task.ID).where(Criterion.and(TaskCriteria.isActive(),
                    TaskCriteria.isVisible())).
                    orderBy(SortHelper.defaultTaskOrder()).limit(100));
            mTaskNumber = c.getCount();
            try {

                Task task = new Task();
                for (int k = 0; k < c.getCount(); k++) {
                    c.moveToNext();
                    String[] existedPeople = mLocationService.getLocationsByPeopleAsArray(task.getId());
                    if (existedPeople != null) {
                        for (String person : existedPeople) {
                            Cursor cur = mPeopleDB.fetchByMail(person);
                            if (cur == null) {
                                continue;
                            }
                            if (!cur.moveToFirst()) {
                                cur.close();
                                continue;
                            }
                            FriendProps fp = AroundroidDbAdapter.userToFP(cur);
                            if (fp != null) {
                                if (fp.isValid()) {
                                    GeoPoint gp = Misc.degToGeo(new DPoint(fp.getDlat(), fp.getDlon()));
                                    String savedAddr = mapFunctions.getSavedAddressAndUpdate(gp.getLatitudeE6(), gp.getLongitudeE6());
                                    mMapView.addItemToOverlay(gp, OVERLAY_PEOPLE_NAME, person, savedAddr, PEOPLE_OVERLAY, task.getId(), person);
                                }
                            }
                            cur.close();
                        }
                    }
                }
            } finally {
                c.close();
            }

                mMapView.updateDeviceLocation();

                mHan.postDelayed(this, mDelayMillis);
            }
        };

        private void setUITimer(){
            mHan.removeCallbacks(mUpdateTimeTask);
            mHan.postDelayed(mUpdateTimeTask, mDelayMillis);

        }

        @Override
        protected void onPause() {
            mPeopleDB.close();
            mHan.removeCallbacks(mUpdateTimeTask);
            super.onPause();
        }

        @Override
        protected void onResume() {
            mPeopleDB.open();
            setUITimer();
            super.onResume();
        }

    }


