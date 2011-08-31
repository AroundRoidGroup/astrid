package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.AdjustedOverlayItem;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.LocationsDbAdapter;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.MyEventClassListener;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
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

public class MapLocationActivity extends MapActivity implements OnZoomListener  {

    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task mCurrentTask = null;
    private long mTaskID;
    private AdjustedMap mMapView;
    private final LocationService locationService = new LocationService();
    private double mRadius;
    private Button mViewAll;
    private List<String> mTypes;
    private List<String> mNullPeople;
    private Map<String, DPoint> mPeople;

    /* identifiers for the overlays in the mapView */
    private static final int SPECIFIC_OVERLAY = 1;
    private static final int TYPE_OVERLAY = 2;
    private static final int PEOPLE_OVERLAY = 3;

    private static final int MENU_SPECIFIC_GROUP = 1;
    private static final int MENU_KIND_GROUP = 65536;
    private static final int MENU_PEOPLE_GROUP = 1048576;
    private static final int MENU_TAPPED_GROUP = 16777216;


    private AroundroidDbAdapter mPeopleDB;
    private LocationsDbAdapter mLocationDB;

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

    //    private class ViewAll extends AbstractAction {
    //
    //        public ViewAll() {
    //            super(R.drawable.ic_menu_list);
    //        }
    //
    //        @Override
    //        public void performAction(View view) {
    //            if (!mMapView.hasPlaces()) {
    //                AlertDialog dialog = new AlertDialog.Builder(MapLocationActivity.this).create();
    //                dialog.setIcon(android.R.drawable.ic_dialog_alert);
    //                dialog.setTitle("Information");
    //                dialog.setMessage("No locations for this task.");
    //                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
    //                        new DialogInterface.OnClickListener() {
    //                    public void onClick(DialogInterface dg, int which) {
    //                        return;
    //                    }
    //                });
    //                dialog.show();
    //            }
    //            else mViewAll.showContextMenu();
    //            return;
    //        }
    //
    //    }
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@ Adding the button that centralizing the map to the last known location of the device  @@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

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


    private class ViewAll extends AbstractAction {

        public ViewAll() {
            super(R.drawable.ic_menu_list);
        }

        @Override
        public void performAction(View view) {
            Resources r = getResources();
            if (!hasPlaces()) {
                AlertDialog dialog = new AlertDialog.Builder(MapLocationActivity.this).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.setTitle(r.getString(R.string.map_alert_dialog_title));
                dialog.setMessage(r.getString(R.string.no_location_for_task));
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.DLG_ok),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        return;
                    }
                });
                dialog.show();
            }
            else mViewAll.showContextMenu();
            return;
        }

    }

    public boolean hasPlaces() {
        return mMapView.hasPlaces() || mTypes.size() > 0 ;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        Resources r = getResources();
        menu.setHeaderTitle(r.getString(R.string.all_locations));
        int len = mMapView.getOverlaySize(SPECIFIC_OVERLAY);
        DPoint[] specCoords = mMapView.getAllByIDAsCoords(SPECIFIC_OVERLAY);
        String[] specAddrs = mMapView.getAllByIDAsAddress(SPECIFIC_OVERLAY);
        for (int i = 0 ; i < len ; i++) {
            if (specAddrs[i] != null)
                menu.add(MENU_SPECIFIC_GROUP, MENU_SPECIFIC_GROUP + i, Menu.NONE, specAddrs[i]);
            else menu.add(MENU_SPECIFIC_GROUP, MENU_SPECIFIC_GROUP + i, Menu.NONE, specCoords[i].toString());
        }
        for (int i = 0 ; i < mTypes.size() ; i++)
            menu.add(MENU_KIND_GROUP, MENU_KIND_GROUP + i, Menu.NONE, mTypes.get(i));
        for (Entry<String, DPoint> element : mPeople.entrySet()) {
            List<AdjustedOverlayItem> contactMail = mMapView.selectItemFromOverlayByExtrasAsAjustedItem(PEOPLE_OVERLAY, element.getKey());
            if (!contactMail.isEmpty()) {
                menu.add(MENU_PEOPLE_GROUP, contactMail.get(0).getUniqueID(), Menu.NONE, contactMail.get(0).getSnippet());
            }
        }
        int i = 0;
        for (i = 0 ; i < mNullPeople.size() ; i++)
            menu.add(MENU_PEOPLE_GROUP, -1, Menu.NONE, mNullPeople.get(i));
        for (i = 0 ; i < mMapView.getTappedPointsCount() ; i++) {
            String addr = mMapView.getTappedItem(i).getAddress();
            if (addr == null)
                addr = Misc.geoToDeg(mMapView.getTappedItem(i).getPoint()).toString();
            menu.add(MENU_TAPPED_GROUP, MENU_TAPPED_GROUP + i, Menu.NONE, addr);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent = new Intent(this, Focaccia.class);
        Resources r = getResources();
        /* getting task's title by its ID */
        TaskService taskService = new TaskService();
        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE).where(Criterion.and(TaskCriteria.isActive(),Criterion.and(TaskCriteria.byId(mTaskID),
                TaskCriteria.isVisible()))).
                orderBy(SortHelper.defaultTaskOrder()).limit(100));
        try {

            Task task = new Task();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                task.readFromCursor(cursor);
                intent.putExtra(Focaccia.TASK_NAME, cursor.getString(cursor.getColumnIndex(Task.TITLE.toString())));
                break;
            }
        } finally {
            cursor.close();
        }
        intent.putExtra(Focaccia.READ_ONLY, Focaccia.READ_ONLY);
        switch (item.getGroupId()) {
        case MENU_SPECIFIC_GROUP:
            AdjustedOverlayItem specItem = mMapView.getOverlay(SPECIFIC_OVERLAY).getItem(item.getItemId() - MENU_SPECIFIC_GROUP);
            mMapView.getController().setCenter(specItem.getPoint());

            intent.putExtra(Focaccia.CMENU_EXTRAS, item.getItemId() - MENU_SPECIFIC_GROUP + ""); //$NON-NLS-1$
            if (mMapView.hasConfig(SPECIFIC_OVERLAY, Focaccia.SHOW_TITLE))
                intent.putExtra(Focaccia.SHOW_TITLE, specItem.getTitle().toString());
            if (mMapView.hasConfig(SPECIFIC_OVERLAY, Focaccia.SHOW_ADDRESS))
                intent.putExtra(Focaccia.SHOW_ADDRESS, (specItem.getAddress() == null) ? Misc.geoToDeg(specItem.getPoint()).toString() : specItem.getAddress());

            startActivity(intent);
            return true;
        case MENU_KIND_GROUP:
            GeoPoint closestType = mMapView.getPointWithMinimalDistanceFromDeviceLocation(TYPE_OVERLAY, item.getTitle().toString());
            if (closestType != null)
                mMapView.getController().setCenter(closestType);

            if (mMapView.hasConfig(TYPE_OVERLAY, Focaccia.SHOW_AMOUNT_BY_EXTRAS)) {
                int bla = mMapView.getItemsByExtrasCount(TYPE_OVERLAY, item.getTitle().toString());
                intent.putExtra(Focaccia.SHOW_AMOUNT_BY_EXTRAS, bla + ""); //$NON-NLS-1$
            }
            if (mMapView.hasConfig(TYPE_OVERLAY, Focaccia.SHOW_NAME))
                intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.kind_type));
            if (mMapView.hasConfig(TYPE_OVERLAY, Focaccia.SHOW_TITLE))
                intent.putExtra(Focaccia.SHOW_TITLE, item.getTitle().toString());

            startActivity(intent);
            return true;
        case MENU_PEOPLE_GROUP:
            if (item.getItemId() == -1) {
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_NAME))
                    intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.kind_people));
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_TITLE))
                    intent.putExtra(Focaccia.SHOW_TITLE, item.getTitle());
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_ADDRESS))
                    intent.putExtra(Focaccia.SHOW_ADDRESS, Focaccia.NO_ADDRESS_WARNING);
                startActivity(intent);
                return true;
            }
            AdjustedOverlayItem peopleItem = mMapView.getOverlay(PEOPLE_OVERLAY).getItem(item.getItemId());

            DPoint da = mPeople.get(item.getTitle());
            if (da != null && !da.isNaN()) {
                mMapView.getController().setCenter(Misc.degToGeo(da));
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_NAME))
                    intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.kind_people));
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_TITLE))
                    intent.putExtra(Focaccia.SHOW_TITLE, peopleItem.getTitle());
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_SNIPPET))
                    intent.putExtra(Focaccia.SHOW_SNIPPET, peopleItem.getSnippet());
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_ADDRESS))
                    intent.putExtra(Focaccia.SHOW_ADDRESS, peopleItem.getAddress());

                startActivity(intent);
                return true;
            }
        default: return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources r = getResources();
        setContentView(R.layout.map_of_task);

        mMapView = (AdjustedMap) findViewById(R.id.mapview);
        mMapView.makeUneditable();

        MapController mapController = mMapView.getController();
        DPoint deviceLocation = mMapView.getDeviceLocation();

        mPeopleDB = new AroundroidDbAdapter(this);
        mPeopleDB.open();
        mLocationDB = new LocationsDbAdapter(this);
        mLocationDB.open();
        mRadius = 100;

        mViewAll = (Button)findViewById(R.id.forcontextmenuonlybutton);
        mViewAll.setVisibility(View.GONE);
        registerForContextMenu(mViewAll);

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

        mMapView.createOverlay(true, SPECIFIC_OVERLAY, this.getResources().getDrawable(R.drawable.icon_specific), new String[] {
            Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, r.getString(R.string.kind_specific));
        mMapView.createOverlay(false, TYPE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_type), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_AMOUNT_BY_EXTRAS, Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, r.getString(R.string.kind_type));
        mMapView.createOverlay(true, PEOPLE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_people), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS, Focaccia.SHOW_SNIPPET
        }, r.getString(R.string.kind_people));


        mPeople = new HashMap<String, DPoint>();
        mNullPeople = new ArrayList<String>();


        /* Adding people that are related to the task */
        String[] people = locationService.getLocationsByPeopleAsArray(mTaskID);
        if (people != null) {
            DPoint[] coords = new DPoint[people.length];
            for (int i = 0 ; i < people.length ; i++) {
                Cursor c = mPeopleDB.fetchByMail(people[i]);
                if (c == null || !c.moveToFirst()) {
                    coords[i] = null;
                    mNullPeople.add(people[i]);
                    if (c != null)
                        c.close();
                    continue;
                }
                FriendProps fp = AroundroidDbAdapter.userToFP(c);
                if (fp != null) {
                    Double lat = c.getDouble(c.getColumnIndex(AroundroidDbAdapter.KEY_LAT));
                    Double lon = c.getDouble(c.getColumnIndex(AroundroidDbAdapter.KEY_LON));
                    coords[i] = new DPoint(lat, lon);
                    mPeople.put(people[i], coords[i]);
                }
                else {
                    coords[i] = null;
                    mNullPeople.add(people[i]);
                }
                c.close();

            }
            mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY, people, coords, mTaskID);
        }
        String[] specificLocations = locationService.getLocationsBySpecificAsArray(mTaskID);
        /* Converting from location written as string to DPoint */
        DPoint[] coords = new DPoint[specificLocations.length];
        for (int i = 0 ; i < specificLocations.length ; i++)
            coords[i] = new DPoint(specificLocations[i]);
        mapFunctions.addLocationSetToMap(mMapView, SPECIFIC_OVERLAY, coords, "Specific Location", mTaskID); //$NON-NLS-1$


        /* If the task is location-based, the following code will add the locations to the map */
        String[] locationTags = locationService.getLocationsByTypeAsArray(mTaskID);
        int[] feedback = mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, locationTags, mRadius, mTaskID);
        mTypes = new ArrayList<String>();
        for (int i = 0 ; i < feedback.length ; i++)
            if (feedback[i] == mapFunctions.SUCCESS)
                mTypes.add(locationTags[i]);

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle(mCurrentTask.getValue(Task.TITLE));

        actionBar.addAction(new ViewAll());
        actionBar.addAction(new DeviceLocation());

        mMapView.addEventListener(new MyEventClassListener() {

            @Override
            public void handleMyEventClassEvent(EventObject e) {
                mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, Misc.ListToArray(mTypes), mRadius, mTaskID);

            }

        });

        mMapView.setZoomByAllLocations();
    }

    private final Handler mHan = new Handler();
    final int mDelayMillis = 10 * 1000;
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
           /* my code */
            Resources r = getResources();
            mMapView.clearOverlay(PEOPLE_OVERLAY);
            mMapView.invalidate();
            String[] existedPeople = locationService.getLocationsByPeopleAsArray(mTaskID);
            if (existedPeople != null) {
                for (String person : existedPeople) {
                    Cursor c = mPeopleDB.fetchByMail(person);
                    if (c == null) {
                        continue;
                    }
                    if (!c.moveToFirst()) {
                        c.close();
                        continue;
                    }
                    FriendProps fp = AroundroidDbAdapter.userToFP(c);
                    if (fp != null) {
                        if (fp.isValid()) {
                            GeoPoint gp = Misc.degToGeo(new DPoint(fp.getDlat(), fp.getDlon()));
                            String savedAddr = mapFunctions.getSavedAddressAndUpdate(gp.getLatitudeE6(), gp.getLongitudeE6());
                            mMapView.addItemToOverlay(gp, r.getString(R.string.kind_people), person, savedAddr, PEOPLE_OVERLAY, mTaskID, person);
                        }
                    }
                    c.close();
                }
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
        mHan.removeCallbacks(mUpdateTimeTask);
        super.onPause();
    }

    @Override
    protected void onResume() {
        setUITimer();
        super.onResume();
    }
}
