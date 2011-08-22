package com.todoroo.astrid.activity;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.ManageContactsActivity;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.AdjustedOverlayItem;
import com.aroundroidgroup.map.AutoComplete;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.Geocoding;
import com.aroundroidgroup.map.LocationControlSet;
import com.aroundroidgroup.map.LocationsDbAdapter;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.MyEventClassListener;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;
import com.todoroo.astrid.service.TaskService;

public class SpecificMapLocation extends MapActivity{


    public static final String SPECIFIC_POINTS = "SpecificMapLocation"; //$NON-NLS-1$
    public static final int FOCACCIA_RESULT_CODE = 1;
    public static final int FOCACCIA_RESULT_CODE_BACK_PRESSED = 2;
    public static final int FOCACCIA_RESULT_CODE_FOR_KIND = 3;
    public static final int CONTACTS_REQUEST_CODE = 0;

    public static final String CMENU_TAP = "ContextMenu_Tap_Selection";
    public static final String CMENU_KIND = "ContextMenu_Kind_Selection";
    public static final String CMENU_SPECIFIC = "ContextMenu_Specific_Selection";
    public static final String CMENU_PEOPLE = "ContextMenu_People_Selection";
    public static final String TASK_NAME = "Task_Name_For_POPUP_HEADER";

    private static final int MENU_SPECIFIC_GROUP = 1;
    private static final int MENU_KIND_GROUP = 65536;
    private static final int MENU_PEOPLE_GROUP = 1048576;
    private static final int MENU_TAPPED_GROUP = 16777216;

    /* identifiers for the overlays in the mapView */
    private static final int SPECIFIC_OVERLAY = 1;
    private static final int TYPE_OVERLAY = 2;
    private static final int PEOPLE_OVERLAY = 3;

    public static final String READ_ONLY = "0";
    public static final String DELETE = "1";
    public static final String DELETE_ALL = "2";

    /* overlays' names */
    private static final String OVERLAY_TYPE_NAME = "Type Location";
    private static final String OVERLAY_SPECIFIC_NAME = "Specific Location";
    private static final String OVERLAY_PEOPLE_NAME = "People Location";

    /* identifiers for the intent that is sent back to TaskEditActivity from SaveAndQuit function */
    public static final String SPECIFIC_TO_SEND = "specific"; //$NON-NLS-1$
    public static final String TYPE_TO_SEND = "kind"; //$NON-NLS-1$
    public static final String PEOPLE_TO_SEND = "people"; //$NON-NLS-1$

    /* identifiers for the intent and foccacia */
    public static final String SHOW_NAME = "name";
    public static final String SHOW_ADDRESS = "address";
    public static final String SHOW_TITLE = "title";
    public static final String SHOW_SNIPPET = "snippet";
    public static final String SHOW_AMOUNT_BY_EXTRAS = "amount";
    public static final String CMENU_EXTRAS = "contextMenuExtras";

    private static final String SPECIFIC_TYPE_FIELD_TEXT = "Specific Location"; //$NON-NLS-1$
    private long taskID;
    private AdjustedMap mMapView;
    private double radius;

    private LocationService locationService;
    private List<String> mTypes = null;
    private Map<String, DPoint> mPeople = null;
    private final Thread previousThread = null;
    private static ArrayAdapter<String> adapter;
    private DPoint deviceLocation;

    private final AroundroidDbAdapter db = new AroundroidDbAdapter(this);
    private LocationsDbAdapter locDB;

    private int pressedItemIndex;
    private String pressedItemExtras;

    private static EditText mAddress;

    private final OnClickListener nothingToShowClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Toast.makeText(SpecificMapLocation.this, "No locations for this task", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
        }
    };

    private final OnLongClickListener nothingToShowLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(SpecificMapLocation.this, "No locations for this task", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            return false;
        }
    };

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    public static void updateSuggestions(List<String> lst) {
        adapter.clear();
        if (lst == null)
            return;
        for (String s : lst)
            adapter.add(s);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("All Locations"); //$NON-NLS-1$
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
        int i = 0;
        for (Map.Entry<String, DPoint> p : mPeople.entrySet()) {
            int index = mMapView.getItemID(OVERLAY_PEOPLE_NAME, p.getValue());
            if (index != -1)
                menu.add(MENU_PEOPLE_GROUP, MENU_PEOPLE_GROUP + index, Menu.NONE, p.getKey());
        }
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

        /* getting task's title by its ID */
        TaskService taskService = new TaskService();
        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE).where(Criterion.and(TaskCriteria.isActive(),Criterion.and(TaskCriteria.byId(taskID),
                TaskCriteria.isVisible()))).
                orderBy(SortHelper.defaultTaskOrder()).limit(100));
        try {

            Task task = new Task();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                task.readFromCursor(cursor);
                intent.putExtra(TASK_NAME, cursor.getString(cursor.getColumnIndex(Task.TITLE.toString())));
                break;
            }
        } finally {
            cursor.close();
        }

        switch (item.getGroupId()) {
        //TODO consider removing the arrays and add each field as a stand-alone extra
        case MENU_TAPPED_GROUP:
            pressedItemExtras = null;
            pressedItemIndex = item.getItemId() - MENU_TAPPED_GROUP;
            AdjustedOverlayItem tapItem = mMapView.getTappedItem(pressedItemIndex);
            mMapView.getController().setCenter(tapItem.getPoint());

            intent.putExtra(DELETE, DELETE);
            intent.putExtra(CMENU_EXTRAS, item.getItemId() - MENU_TAPPED_GROUP + "");
            intent.putExtra(SHOW_TITLE, tapItem.getTitle().toString());
            intent.putExtra(SHOW_ADDRESS, (tapItem.getAddress() == null) ? Misc.geoToDeg(tapItem.getPoint()).toString() : tapItem.getAddress());

            startActivityForResult(intent, MENU_TAPPED_GROUP);
            return true;
        case MENU_SPECIFIC_GROUP:
            pressedItemExtras = null;
            pressedItemIndex = item.getItemId() - MENU_SPECIFIC_GROUP;
            AdjustedOverlayItem specItem = mMapView.getOverlay(SPECIFIC_OVERLAY).getItem(pressedItemIndex);
            mMapView.getController().setCenter(specItem.getPoint());

            intent.putExtra(DELETE, DELETE);
            intent.putExtra(CMENU_EXTRAS, item.getItemId() - MENU_SPECIFIC_GROUP + "");
            if (mMapView.hasConfig(SPECIFIC_OVERLAY, SHOW_TITLE))
                intent.putExtra(SHOW_TITLE, specItem.getTitle().toString());
            if (mMapView.hasConfig(SPECIFIC_OVERLAY, SHOW_ADDRESS))
                intent.putExtra(SHOW_ADDRESS, (specItem.getAddress() == null) ? Misc.geoToDeg(specItem.getPoint()).toString() : specItem.getAddress());

            startActivityForResult(intent, MENU_SPECIFIC_GROUP);
            return true;
        case MENU_KIND_GROUP:
            pressedItemIndex = -1;
            pressedItemExtras = item.getTitle().toString();
            GeoPoint closestType = mMapView.getPointWithMinimalDistanceFromDeviceLocation(TYPE_OVERLAY, item.getTitle().toString());
            if (closestType != null)
                mMapView.getController().setCenter(closestType);

            intent.putExtra(DELETE_ALL, DELETE_ALL);
            if (mMapView.hasConfig(TYPE_OVERLAY, SHOW_AMOUNT_BY_EXTRAS))
                intent.putExtra(SHOW_AMOUNT_BY_EXTRAS, mMapView.getItemsByExtrasCount(TYPE_OVERLAY, item.getTitle().toString()));
            if (mMapView.hasConfig(TYPE_OVERLAY, SHOW_NAME))
                intent.putExtra(SHOW_NAME, OVERLAY_TYPE_NAME);
            if (mMapView.hasConfig(TYPE_OVERLAY, SHOW_TITLE))
                intent.putExtra(SHOW_TITLE, item.getTitle().toString());

            startActivityForResult(intent, MENU_KIND_GROUP);
            return true;
        case MENU_PEOPLE_GROUP:
            pressedItemExtras = null;
            pressedItemIndex = item.getItemId() - MENU_PEOPLE_GROUP;
            AdjustedOverlayItem peopleItem = mMapView.getOverlay(PEOPLE_OVERLAY).getItem(pressedItemIndex);
            if (mPeople.get(peopleItem.getSnippet()) == null) {
                Toast.makeText(this, "Cannot retrieve person's locations !", Toast.LENGTH_LONG).show();
                return true;
            }
            mMapView.getController().setCenter(Misc.degToGeo(mPeople.get(item.getTitle())));
            intent.putExtra(DELETE, DELETE);
            if (mMapView.hasConfig(PEOPLE_OVERLAY, SHOW_NAME))
                intent.putExtra(SHOW_NAME, OVERLAY_PEOPLE_NAME);
            if (mMapView.hasConfig(PEOPLE_OVERLAY, SHOW_TITLE))
                intent.putExtra(SHOW_TITLE, peopleItem.getSnippet());
            if (mMapView.hasConfig(PEOPLE_OVERLAY, SHOW_ADDRESS))
                intent.putExtra(SHOW_ADDRESS, (peopleItem.getAddress() == null) ? Misc.geoToDeg(peopleItem.getPoint()).toString() : peopleItem.getAddress());

            startActivityForResult(intent, MENU_PEOPLE_GROUP);
            return true;
        default: return super.onContextItemSelected(item);
        }
    }

    public boolean hasPlaces() {
        return !(mMapView.getTappedPointsCount() == 0 && mTypes.size() == 0 && mPeople.size() == 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.specific_map);

        mMapView = (AdjustedMap) findViewById(R.id.mapview);
        radius = 100;
        db.open();

        locDB = new LocationsDbAdapter(this);
        locDB.open();

        mMapView.addEventListener(new MyEventClassListener() {

            @Override
            public void handleMyEventClassEvent(EventObject e) {
                mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, Misc.ListToArray(mTypes), Misc.geoToDeg(mMapView.getMapCenter()), radius, taskID);

            }

        });
        deviceLocation = mMapView.getDeviceLocation();
        if (deviceLocation != null) {
            /* Centralizing the map to the last known location of the device */
            mMapView.getController().setCenter(Misc.degToGeo(deviceLocation));
        }

        /* enables adding locations by tapping on the map */
        mMapView.enableAddByTap();

        mMapView.createOverlay(SPECIFIC_OVERLAY, this.getResources().getDrawable(R.drawable.icon_specific), new String[] {
            AdjustedMap.SHOW_TITLE, AdjustedMap.SHOW_ADDRESS
        }, OVERLAY_SPECIFIC_NAME);
        mMapView.createOverlay(TYPE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_type), new String[] {
            AdjustedMap.SHOW_NAME, AdjustedMap.SHOW_AMOUNT_BY_EXTRAS, AdjustedMap.SHOW_TITLE, AdjustedMap.SHOW_ADDRESS
        }, OVERLAY_TYPE_NAME);
        mMapView.createOverlay(PEOPLE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_people), new String[] {
            AdjustedMap.SHOW_NAME, AdjustedMap.SHOW_ADDRESS
        }, OVERLAY_PEOPLE_NAME);

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Adding the button that centralizing the map to the last known location of the device  @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        Button focus = (Button)findViewById(R.id.focusDevLoc);
        focus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (deviceLocation != null)
                    mMapView.getController().setCenter(Misc.degToGeo(deviceLocation));
            }
        });
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Adding the auto-complete mechanism                                                    @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.specificAddress);
        adapter = new ArrayAdapter<String>(SpecificMapLocation.this, R.layout.search_result_list, new String[0]);
        textView.setAdapter(adapter);

        textView.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //                if (previousThread != null) {
                //                    if (previousThread.isAlive())
                //                        previousThread.destroy();
                //                    previousThread = null;
                //                }
                //previousThread = new Thread(new AsyncAutoComplete(textView.getText().toString()));
                //previousThread.run();
                AutoComplete x = new AutoComplete(SpecificMapLocation.this);
                x.execute(textView.getText().toString());
                return false;
            }
        });
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Getting the data from the calling activity (TaskEditActivity). this data contains the @@@@@    */
        /* @@@@@ taskID and the locations that already been added.                                     @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        Bundle bundle = getIntent().getExtras();
        String[] existedSpecific = bundle.getStringArray(LocationControlSet.SPECIFIC_TO_LOAD);
        String[] existedTypes = bundle.getStringArray(LocationControlSet.TYPE_TO_LOAD);
        String[] existedPeople = bundle.getStringArray(LocationControlSet.PEOPLE_TO_LOAD);
        taskID = bundle.getLong(LocationControlSet.TASK_ID);

        //        mMapView.associateMapWithTask(taskID);
        for (int i = 0 ; i < existedSpecific.length ; i+=2) {
            DPoint d = new DPoint(existedSpecific[i]);
            if (d.isNaN())
                continue;
            String address = null;
            if (existedSpecific[i + 1] != null)
                address = existedSpecific[i + 1];
            else {
                address = locDB.fetchByCoordinateAsString(d.toString());
                if (address == null) { /* such mapping does not exist, lets make one */
                    try {
                        address = Geocoding.reverseGeocoding(d);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (address == null)
                        address = LocationsDbAdapter.DATABASE_ADDRESS_GEOCODE_FAILURE;
                    locDB.createTranslate(d.toString(), address);
                }
                /* mapping exists but previous trial to geocode failed */
                if (address != null && address.equals(LocationsDbAdapter.DATABASE_ADDRESS_GEOCODE_FAILURE))
                    address = d.toString();
            }
            mMapView.addItemToOverlay(Misc.degToGeo(d), "Specific Location", address, address, SPECIFIC_OVERLAY, taskID, null); //$NON-NLS-1$
        }

        mTypes = new ArrayList<String>();
        int i = 0;
        mainLoop: while (i < existedTypes.length) {
            int len = 0;
            String[] typeAndSize = Misc.extractType(existedTypes[i++]);
            if (!mTypes.contains(typeAndSize[0])) {
                mTypes.add(typeAndSize[0]);
                len = Integer.parseInt(typeAndSize[1]);
                Map<String, DPoint> data = null;
                for (int k = 0 ; k < len ; k++) {
                    String savedBusiness = locDB.fetchByCoordinateFromType(typeAndSize[0], existedTypes[i]);
                    String savedAddr = locDB.fetchByCoordinateAsString(existedTypes[i]);
                    if (savedAddr == null) {
                        try {
                            savedAddr = Geocoding.reverseGeocoding(new DPoint(existedTypes[i]));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (savedAddr == null)
                            savedAddr = LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE;
                        locDB.createTranslate(existedTypes[i], savedAddr);
                    }
                    if (savedBusiness != null) {
                        mMapView.addItemToOverlay(Misc.degToGeo(new DPoint(existedTypes[i + k])),
                                savedBusiness,
                                typeAndSize[0],
                                (savedAddr == LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE) ? existedTypes[i + k] : savedAddr,
                                        TYPE_OVERLAY,
                                        taskID,
                                        typeAndSize[0]);
                    }
                    else {
                        if (data == null) {
                            try {
                                data = Misc.googlePlacesQuery(typeAndSize[0], deviceLocation, radius);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (data == null) { /* parse the next Type location */
                                i += len;
                                continue mainLoop;
                            }
                            for (Map.Entry<String, DPoint> element : data.entrySet()) {
                                data.put(element.getKey(), new DPoint(element.getValue().toString()));
                            }
                        }
                        DPoint p = new DPoint(existedTypes[i + k]);
                        for (Map.Entry<String, DPoint> pair : data.entrySet()) {
                            DPoint s = pair.getValue();
                            if (Misc.similarDegs(p, s))
                                if (p.toString().equals(pair.getValue().toString()))
                                    savedBusiness = pair.getKey();
                        }
                        mMapView.addItemToOverlay(Misc.degToGeo(new DPoint(existedTypes[i + k])),
                                (savedBusiness != null) ? savedBusiness : "Untitled", //$NON-NLS-1$
                                        typeAndSize[0],
                                        (savedAddr == LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE) ? existedTypes[i + k] : savedAddr,
                                                TYPE_OVERLAY,
                                                taskID,
                                                typeAndSize[0]);
                    }
                }
            }
            i += len;
        }

        mPeople = new HashMap<String, DPoint>();
        for (String s : existedPeople)
            mPeople.put(s, null);

        String[] tomer = new String[1];
        tomer[0] = "tomer.keshet@gmail.com";
        DPoint[] coordTomer = new DPoint[1];
        coordTomer[0] = new DPoint(40.710215,-74.009013);
        mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY, tomer, coordTomer, taskID);
        mPeople.put(tomer[0], coordTomer[0]);
        db.updatePeople(db.createPeople(tomer[0]), 40.710215, -74.009013, 21600L);
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Adding the button that displays all the locations that have been added to the task    @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        final Button viewAll = (Button)findViewById(R.id.viewAll);
        registerForContextMenu(viewAll);
        if (!hasPlaces()) {
            viewAll.setOnClickListener(nothingToShowClickListener);
            viewAll.setOnLongClickListener(nothingToShowLongClickListener);
        }

        viewAll.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openContextMenu(v);
            }
        });
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */


        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Enabling zooming and setting the initial zoom level so all the locations will be      @@@@@    */
        /* @@@@@ visible to the user.                                                                  @@@@@ */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        mMapView.setBuiltInZoomControls(true);
        mMapView.setZoomByAllLocations();
        radius = AdjustedMap.equatorLen / Math.pow(2, mMapView.getZoomLevel() - 1);
        Toast.makeText(this, "radius = " + radius, Toast.LENGTH_LONG).show();

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Adding the button that allowing to add people to the task                             @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        ((Button)findViewById(R.id.addPeople)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContextManager.getContext(),ManageContactsActivity.class);
                intent.putExtra(ManageContactsActivity.taskIDSTR, taskID);
                startActivityForResult(intent, CONTACTS_REQUEST_CODE);
            }
        });
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Setting the mechanism of reading user input and interpreting it whether it's specific @@@@@    */
        /* @@@@@ location or Type location                                                             @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        mAddress = (EditText)findViewById(R.id.specificAddress);
        final Button addressButton = (Button)findViewById(R.id.specificAddAddressButton);
        addressButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DPoint d = null;
                String text = mAddress.getText().toString();

                /* 2 following rows are for hiding the keyboard */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mAddress.getWindowToken(), 0);

                if (Misc.isType(text)) { /* input is one of the 'Type locations' */
                    if (!mTypes.contains(text)) {
                        locDB.close();
                        int degSuccess = mapFunctions.degreeOfSuccess(mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY,
                                new String[] { text.replace(' ', '_') }, Misc.geoToDeg(mMapView.getMapCenter()), radius, taskID));
                        if (degSuccess == mapFunctions.ALL_GOOD)
                            Toast.makeText(SpecificMapLocation.this, "Location type '" + text + "' has been added successfully !", //$NON-NLS-1$ //$NON-NLS-2$
                                    Toast.LENGTH_LONG).show();
                        else if (degSuccess == mapFunctions.ALL_BAD)
                            Toast.makeText(SpecificMapLocation.this, "Failed to add location type '" + text + "' !", Toast.LENGTH_LONG).show(); //$NON-NLS-1$ //$NON-NLS-2$
                        mMapView.invalidate();
                        mTypes.add(text);
                        locDB.open();
                    }
                }
                else { /* input is an address */
                    String savedCoordinate = locDB.fetchByAddressAsString(text);
                    if (savedCoordinate == null) {
                        try {
                            d = Geocoding.geocoding(text);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    String theCoord;
                    if (d != null)
                        theCoord = d.toString();
                    else theCoord = savedCoordinate;

                    if (d != null) {
                        String addr = null;
                        String savedAddr = locDB.fetchByCoordinateAsString(theCoord);
                        if (savedAddr == null) {
                            try {
                                addr = Geocoding.reverseGeocoding(d);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        String theAddr;
                        if (addr != null)
                            theAddr = addr;
                        else theAddr = savedAddr;

                        locDB.createTranslate(theCoord, theAddr);
                        mMapView.addItemToOverlay(Misc.degToGeo(new DPoint(theCoord)), SPECIFIC_TYPE_FIELD_TEXT, theAddr, theAddr, SPECIFIC_OVERLAY, taskID, null);
                        mMapView.invalidate();
                    }
                    else Toast.makeText(SpecificMapLocation.this, "Address not found!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$

                }
            }
        });

        /* this is for the first time the user click the address text box so the content will be deleted. */
        mAddress.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mAddress.setText(""); //$NON-NLS-1$
                mAddress.setOnClickListener(null);
            }
        });
    }
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONTACTS_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                Bundle bundle = data.getExtras();
                if (bundle == null)
                    return;
                Serializable gerbil = bundle.getSerializable(ManageContactsActivity.PEOPLE_BACK);
                LinkedHashSet<String> ger = (LinkedHashSet<String>) gerbil;
                if (ger != null) {
                    mPeople.clear();
                    mMapView.clearOverlay(PEOPLE_OVERLAY);
                    for (String contact : ger) {
                        Cursor c = db.fetchByMail(contact);
                        DPoint dp = null;
                        if (c != null) {
                            if (c.moveToFirst()) {
                                FriendProps fp = AroundroidDbAdapter.userToFP(c);
                                if (fp.isValid()) {
                                    dp = new DPoint(fp.getDlat(), fp.getDlon());
                                    mPeople.put(contact, dp);
                                }
                                else mPeople.put(contact, null);
                            }
                            c.close();
                        }
                        else mPeople.put(contact, null);
                        mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY,
                                new String[] { contact }, new DPoint[] { dp }, taskID);
                    }
                }
            }
        }

        if (requestCode == MENU_SPECIFIC_GROUP) {
            if (resultCode == RESULT_FIRST_USER) {
                mMapView.removeItemFromOverlay(SPECIFIC_OVERLAY, pressedItemIndex);
            }
        }
        if (requestCode == MENU_KIND_GROUP) {
            if (resultCode == Focaccia.DELETE_ALL) {
                mMapView.removeItemFromOverlayByExtras(TYPE_OVERLAY, pressedItemExtras);
            }
        }
        if (requestCode == MENU_PEOPLE_GROUP) {
            if (resultCode == RESULT_FIRST_USER) { /* DELETE was made */
                mMapView.removeItemFromOverlay(PEOPLE_OVERLAY, pressedItemIndex);
            }
        }

        if (requestCode == AdjustedMap.AM_REQUEST_CODE) {
            if (resultCode == Focaccia.DELETE) {
                AdjustedOverlayItem removedItem = mMapView.removeLastPressedItem();
                if (0 == mMapView.removeItemFromOverlayByExtras(TYPE_OVERLAY, removedItem.getExtras())) {
                    mTypes.remove(removedItem.getExtras());
                }
            }
        }
    }
    private void saveAndQuit() {
        Intent intent = new Intent();

        /* adding the tapped and specific coordinates and addresses */
        DPoint[] tappedPoints = mMapView.getTappedCoords();
        DPoint[] specificPoints = mMapView.getAllByIDAsCoords(SPECIFIC_OVERLAY);
        String[] tappedAddresses = mMapView.getTappedAddress();
        String[] specificAddresses = mMapView.getAllByIDAsAddress(SPECIFIC_OVERLAY);
        String[] tapAndSpec = new String[2 * tappedPoints.length + 2 * specificPoints.length];
        int i = 0;
        for (DPoint p : tappedPoints)
            tapAndSpec[i++] = p.toString();
        for (DPoint p : specificPoints)
            tapAndSpec[i++] = p.toString();
        for (String s : tappedAddresses)
            tapAndSpec[i++] = s;
        for (String s : specificAddresses)
            tapAndSpec[i++] = s;
        intent.putExtra(SPECIFIC_TO_SEND, tapAndSpec);

        /* adding the types */
        i = 0;
        String[] types = new String[mTypes.size()];
        for (String t : mTypes) {
            List<String> items = mMapView.selectItemFromOverlayByExtras(TYPE_OVERLAY, t);
            int j = 0;
            t += "$$"; //$NON-NLS-1$
            while (j < items.size())
                t += items.get(j++) + "%"; //$NON-NLS-1$
            types[i++] = t;
        }
        intent.putExtra(TYPE_TO_SEND, types);

        /* adding the people */
        i = 0;
        String[] people = new String[mPeople.size()];
        for (Map.Entry<String, DPoint> pair : mPeople.entrySet())
            people[i++] = pair.getKey();
        intent.putExtra(PEOPLE_TO_SEND, people);

        setResult(RESULT_OK, intent);
    }

    @Override
    public void onBackPressed() {
        locDB.close();
        saveAndQuit();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
        locDB.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locDB.close();
        saveAndQuit();
    }
}
