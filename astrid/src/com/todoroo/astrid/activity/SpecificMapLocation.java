package com.todoroo.astrid.activity;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.ManageContactsActivity;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.AdjustedOverlayItem;
import com.aroundroidgroup.map.AutoCompleteSuggestions;
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
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
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

    private static final int CONTACTS_REQUEST_CODE = 0;

    public static final String CMENU_TAP = "ContextMenu_Tap_Selection"; //$NON-NLS-1$
    public static final String CMENU_KIND = "ContextMenu_Kind_Selection"; //$NON-NLS-1$
    public static final String CMENU_SPECIFIC = "ContextMenu_Specific_Selection"; //$NON-NLS-1$
    public static final String CMENU_PEOPLE = "ContextMenu_People_Selection"; //$NON-NLS-1$

    private static final int MENU_SPECIFIC_GROUP = 1;
    private static final int MENU_KIND_GROUP = 65536;
    private static final int MENU_PEOPLE_GROUP = 1048576;
    private static final int MENU_TAPPED_GROUP = 16777216;

    /* identifiers for the overlays in the mapView */
    private static final int SPECIFIC_OVERLAY = 1;
    private static final int TYPE_OVERLAY = 2;
    private static final int PEOPLE_OVERLAY = 3;

    /* identifiers for the intent that is sent back to TaskEditActivity from SaveAndQuit function */
    public static final String SPECIFIC_TO_SEND = "specific"; //$NON-NLS-1$
    public static final String TYPE_TO_SEND = "kind"; //$NON-NLS-1$
    public static final String PEOPLE_TO_SEND = "people"; //$NON-NLS-1$

    private long mTaskID;
    private double mRadius;
    private Button mViewAll;
    private List<String> mTypes;
    private AdjustedMap mMapView;
    private int mPressedItemIndex;
    private String mLastNullPeople;
    private DPoint mDeviceLocation;
    private static EditText mAddress;
    private List<String> mNullPeople;
    private String mPressedItemExtras;
    private Map<String, DPoint> mPeople;
    private LocationsDbAdapter mLocationDB;
    private AutoCompleteTextView mAutoTextView;
    private static ArrayAdapter<String> mAdapter;
    private final AroundroidDbAdapter mPeopleDB = new AroundroidDbAdapter(this);
    private final LocationService mLocationService = new LocationService();

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    public boolean hasPlaces() {
        return mMapView.hasPlaces() || mTypes.size() > 0 || mNullPeople.size() > 0;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.AD_all_locations);
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

        switch (item.getGroupId()) {
        case MENU_TAPPED_GROUP:
            mPressedItemExtras = null;
            mPressedItemIndex = item.getItemId() - MENU_TAPPED_GROUP;
            AdjustedOverlayItem tapItem = mMapView.getTappedItem(mPressedItemIndex);
            mMapView.getController().setCenter(tapItem.getPoint());

            intent.putExtra(Focaccia.DELETE, Focaccia.DELETE);
            intent.putExtra(Focaccia.CMENU_EXTRAS, item.getItemId() - MENU_TAPPED_GROUP + ""); //$NON-NLS-1$
            intent.putExtra(Focaccia.SHOW_TITLE, tapItem.getTitle().toString());
            intent.putExtra(Focaccia.SHOW_ADDRESS, (tapItem.getAddress() == null) ? Misc.geoToDeg(tapItem.getPoint()).toString() : tapItem.getAddress());

            startActivityForResult(intent, MENU_TAPPED_GROUP);
            return true;
        case MENU_SPECIFIC_GROUP:
            mPressedItemExtras = null;
            mPressedItemIndex = item.getItemId() - MENU_SPECIFIC_GROUP;
            AdjustedOverlayItem specItem = mMapView.getOverlay(SPECIFIC_OVERLAY).getItem(mPressedItemIndex);
            mMapView.getController().setCenter(specItem.getPoint());

            intent.putExtra(Focaccia.DELETE, Focaccia.DELETE);
            intent.putExtra(Focaccia.CMENU_EXTRAS, item.getItemId() - MENU_SPECIFIC_GROUP + ""); //$NON-NLS-1$
            if (mMapView.hasConfig(SPECIFIC_OVERLAY, Focaccia.SHOW_TITLE))
                intent.putExtra(Focaccia.SHOW_TITLE, specItem.getTitle().toString());
            if (mMapView.hasConfig(SPECIFIC_OVERLAY, Focaccia.SHOW_ADDRESS))
                intent.putExtra(Focaccia.SHOW_ADDRESS, (specItem.getAddress() == null) ? Misc.geoToDeg(specItem.getPoint()).toString() : specItem.getAddress());

            startActivityForResult(intent, MENU_SPECIFIC_GROUP);
            return true;
        case MENU_KIND_GROUP:
            mPressedItemIndex = -1;
            mPressedItemExtras = item.getTitle().toString();
            GeoPoint closestType = mMapView.getPointWithMinimalDistanceFromDeviceLocation(TYPE_OVERLAY, item.getTitle().toString());
            if (closestType != null)
                mMapView.getController().setCenter(closestType);

            intent.putExtra(Focaccia.DELETE_ALL, Focaccia.DELETE_ALL);
            if (mMapView.hasConfig(TYPE_OVERLAY, Focaccia.SHOW_AMOUNT_BY_EXTRAS)) {
                int bla = mMapView.getItemsByExtrasCount(TYPE_OVERLAY, item.getTitle().toString());
                intent.putExtra(Focaccia.SHOW_AMOUNT_BY_EXTRAS, bla + ""); //$NON-NLS-1$
            }
            if (mMapView.hasConfig(TYPE_OVERLAY, Focaccia.SHOW_NAME))
                intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.AD_kind_type));
            if (mMapView.hasConfig(TYPE_OVERLAY, Focaccia.SHOW_TITLE))
                intent.putExtra(Focaccia.SHOW_TITLE, item.getTitle().toString());

            startActivityForResult(intent, MENU_KIND_GROUP);
            return true;
        case MENU_PEOPLE_GROUP:
            mPressedItemExtras = null;
            if (item.getItemId() == -1) {
                mLastNullPeople = item.getTitle().toString();
                intent.putExtra(Focaccia.DELETE, Focaccia.DELETE);
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_NAME))
                    intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.AD_kind_people));
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_TITLE))
                    intent.putExtra(Focaccia.SHOW_TITLE, mLastNullPeople);
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_ADDRESS))
                    intent.putExtra(Focaccia.SHOW_ADDRESS, Focaccia.NO_ADDRESS_WARNING);
                startActivityForResult(intent, MENU_PEOPLE_GROUP);
                return true;
            }
            mLastNullPeople = null;
            mPressedItemIndex = item.getItemId();
            AdjustedOverlayItem peopleItem = mMapView.getOverlay(PEOPLE_OVERLAY).getItem(mPressedItemIndex);

            DPoint da = mPeople.get(item.getTitle());
            if (da != null && !da.isNaN()) {
                mMapView.getController().setCenter(Misc.degToGeo(da));
                intent.putExtra(Focaccia.DELETE, Focaccia.DELETE);
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_NAME))
                    intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.AD_kind_people));
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_TITLE))
                    intent.putExtra(Focaccia.SHOW_TITLE, peopleItem.getTitle());
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_SNIPPET))
                    intent.putExtra(Focaccia.SHOW_SNIPPET, peopleItem.getSnippet());
                if (mMapView.hasConfig(PEOPLE_OVERLAY, Focaccia.SHOW_ADDRESS))
                    intent.putExtra(Focaccia.SHOW_ADDRESS, peopleItem.getAddress());

                startActivityForResult(intent, MENU_PEOPLE_GROUP);
                return true;
            }
        default: return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        mLocationDB.open();
        setUITimer();
        super.onResume();
    }

    private final Handler mHan = new Handler();
    final int mDelayMillis = 10 * 1000;
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            /* my code */
            Resources r = getResources();
            mPeople.clear();
            mNullPeople.clear();
            mMapView.clearOverlay(PEOPLE_OVERLAY);
            mMapView.invalidate();
            String[] existedPeople = mLocationService.getLocationsByPeopleAsArray(mTaskID);
            if (existedPeople != null) {
                for (String person : existedPeople) {
                    Cursor c = mPeopleDB.fetchByMail(person);
                    if (c == null) {
                        mNullPeople.add(person);
                        continue;
                    }
                    if (!c.moveToFirst()) {
                        mNullPeople.add(person);
                        c.close();
                        continue;
                    }
                    FriendProps fp = AroundroidDbAdapter.userToFP(c);
                    if (fp != null) {
                        if (fp.isValid())
                            mPeople.put(person, new DPoint(fp.getDlat(), fp.getDlon()));
                        else mNullPeople.add(person);
                    }
                    c.close();
                }
            }
            for (Entry<String, DPoint> entry : mPeople.entrySet()) {
                GeoPoint gp = Misc.degToGeo(entry.getValue());
                String savedAddr = mLocationDB.fetchByCoordinateAsString(gp.getLatitudeE6(), gp.getLongitudeE6());
                if (savedAddr == null) {
                    try {
                        savedAddr = Geocoding.reverseGeocoding(entry.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (savedAddr == null)
                        savedAddr = LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE;
                    mLocationDB.createTranslate(gp.getLatitudeE6(), gp.getLongitudeE6(), savedAddr);
                    if (savedAddr == LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE)
                        savedAddr = entry.getValue().toString();
                }
                mMapView.addItemToOverlay(Misc.degToGeo(entry.getValue()), r.getString(R.string.AD_kind_people), entry.getKey(), savedAddr, PEOPLE_OVERLAY, mTaskID, entry.getKey());
            }

            mMapView.updateDeviceLocation();

            mHan.postDelayed(this, mDelayMillis);
        }
    };

    private final LocationService locationService = new LocationService();
    private void setUITimer(){
        mHan.removeCallbacks(mUpdateTimeTask);
        mHan.postDelayed(mUpdateTimeTask, mDelayMillis);

    }


    private class ViewAll extends AbstractAction {

        public ViewAll() {
            super(R.drawable.ic_menu_list);
        }

        @Override
        public void performAction(View view) {
            Resources r = getResources();
            if (!hasPlaces()) {
                AlertDialog dialog = new AlertDialog.Builder(SpecificMapLocation.this).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.setTitle(R.string.AD_map_alert_dialog_title);
                dialog.setMessage(r.getString(R.string.AD_no_location_for_task));
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.AD_DLG_ok),
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
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@ Adding the button that centralizing the map to the last known location of the device  @@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

    private class DeviceLocation extends AbstractAction {

        public DeviceLocation() {
            super(R.drawable.ic_menu_mylocation);
        }

        @Override
        public void performAction(View view) {
            mDeviceLocation = mMapView.getDeviceLocation();
            if (mDeviceLocation != null)
                mMapView.getController().setCenter(Misc.degToGeo(mDeviceLocation));
            return;
        }

    }
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@ Adding the button that allowing to add people to the task                             @@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

    private class InvitePeople extends AbstractAction {

        public InvitePeople() {
            super(R.drawable.ic_menu_invite);
        }

        @Override
        public void performAction(View view) {
            Intent intent = new Intent(ContextManager.getContext(),ManageContactsActivity.class);
            intent.putExtra(ManageContactsActivity.taskIDSTR, mTaskID);
            intent.putExtra(ManageContactsActivity.peopleArraySTR, mLocationService.getLocationsByPeopleAsArray(mTaskID));
            startActivityForResult(intent, CONTACTS_REQUEST_CODE);
            return;
        }
    }
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources r = getResources();
        setContentView(R.layout.specific_map);

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle(R.string.AD_action_bar_title);

        actionBar.addAction(new ViewAll());
        actionBar.addAction(new DeviceLocation());
        actionBar.addAction(new InvitePeople());

        ImageButton b = new ImageButton(this);
        b.setImageResource(R.drawable.search_button_style);
        actionBar.addView(b);

        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent suggIntent = new Intent(SpecificMapLocation.this, AutoCompleteSuggestions.class);
                suggIntent.putExtra(AutoCompleteSuggestions.AUTOCOMPLETE_CENTER, Misc.geoToDeg(mMapView.getMapCenter()).toString());
                suggIntent.putExtra(AutoCompleteSuggestions.AUTOCOMPLETE_RADIUS, mRadius);
                startActivityForResult(suggIntent, 12);

            }
        });

        mMapView = (AdjustedMap) findViewById(R.id.mapview);

        mRadius = 100;
        mPeopleDB.open();
        mMapView.setSatellite(false);
        mLocationDB = new LocationsDbAdapter(this);
        mLocationDB.open();

        mMapView.addEventListener(new MyEventClassListener() {

            @Override
            public void handleMyEventClassEvent(EventObject e) {
                mLocationDB.close();
                mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, Misc.ListToArray(mTypes), mRadius, mTaskID);
                mLocationDB.open();

            }

        });


        /* enables adding locations by tapping on the map */
        mMapView.enableAddByTap();

        mMapView.createOverlay(false, SPECIFIC_OVERLAY, this.getResources().getDrawable(R.drawable.icon_specific), new String[] {
            Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, r.getString(R.string.AD_kind_specific));
        mMapView.createOverlay(false, TYPE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_type), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_AMOUNT_BY_EXTRAS, Focaccia.SHOW_TITLE, Focaccia.SHOW_ADDRESS
        }, r.getString(R.string.AD_kind_type));
        mMapView.createOverlay(true, PEOPLE_OVERLAY, this.getResources().getDrawable(R.drawable.icon_people), new String[] {
            Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS, Focaccia.SHOW_SNIPPET
        }, r.getString(R.string.AD_kind_people));



        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Adding the auto-complete mechanism                                                    @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        mAutoTextView = (AutoCompleteTextView) findViewById(R.id.specificAddress);
        mAdapter = new ArrayAdapter<String>(SpecificMapLocation.this, R.layout.search_result_list, Misc.types);

        mAutoTextView.setAdapter(mAdapter);
        mAutoTextView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> c = null;
                try {
                    String searchText = mAutoTextView.getText().toString();
                    DPoint center = Misc.geoToDeg(mMapView.getMapCenter());
                    c = Misc.googleAutoCompleteQuery(searchText, center, mRadius);
                    for (String type : Misc.types)
                        c.add(type);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (c != null) {
                    mAdapter = new ArrayAdapter<String>(SpecificMapLocation.this, R.layout.search_result_list, c);
                    mAdapter.sort(new Comparator<String>() {

                        @Override
                        public int compare(String object1, String object2) {
                            boolean firstObj = false;
                            boolean secondObj = false;
                            for (String type : Misc.types) {
                                if (type.equals(object1))
                                    firstObj = true;
                                if (type.equals(object2))
                                    secondObj = true;
                            }
                            if (firstObj && !secondObj)
                                return -1;
                            if (!firstObj && secondObj)
                                return 1;
                            return 0;
                        }
                    });
                    mAutoTextView.setAdapter(mAdapter);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                return;
            }

            @Override
            public void afterTextChanged(Editable s) {
                return;
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
        mTaskID = bundle.getLong(LocationControlSet.TASK_ID);
        String[] existedPeople = locationService.getLocationsByPeopleAsArray(mTaskID);
        mMapView.associateMapWithTask(mTaskID);

        for (int i = 0 ; i < existedSpecific.length ; i+=2) {
            DPoint d = new DPoint(existedSpecific[i]);
            if (d.isNaN())
                continue;
            String address = null;
            if (existedSpecific[i + 1] != null)
                address = existedSpecific[i + 1];
            else {
                GeoPoint gp = Misc.degToGeo(d);
                address = mapFunctions.getSavedAddressAndUpdate(gp.getLatitudeE6(), gp.getLongitudeE6());
            }
            mMapView.addItemToOverlay(Misc.degToGeo(d), "Specific Location", address, address, SPECIFIC_OVERLAY, mTaskID, null); //$NON-NLS-1$
        }

        mTypes = new ArrayList<String>();
        for (String type : existedTypes)
            mTypes.add(type);
        mLocationDB.close();
        mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, existedTypes, mRadius, mTaskID);
        mLocationDB.open();

        mPeople = new HashMap<String, DPoint>();
        mNullPeople = new ArrayList<String>();
        for (String s : existedPeople) {
            Cursor c = mPeopleDB.fetchByMail(s);
            if (c == null)
                continue;
            if (!c.moveToFirst()) {
                c.close();
                continue;
            }
            FriendProps fp = AroundroidDbAdapter.userToFP(c);
            if (fp.isValid())
                mPeople.put(s, new DPoint(fp.getDlat(), fp.getDlat()));
            else mNullPeople.add(s);
            c.close();
        }
        /* adding the people that has location */
        for (Entry<String, DPoint> element : mPeople.entrySet()) {
            GeoPoint coordsGP = Misc.degToGeo(element.getValue());
            String addr = mapFunctions.getSavedAddressAndUpdate(coordsGP.getLatitudeE6(), coordsGP.getLongitudeE6());
            mMapView.addItemToOverlay(coordsGP, element.getKey(), element.getKey(), addr, PEOPLE_OVERLAY, mTaskID, element.getKey());
        }
        LinkedHashSet<String> locations = new LinkedHashSet<String>();
        locations.addAll(mPeople.keySet());
        locations.addAll(mNullPeople);
        mLocationService.syncLocationsByPeople(mTaskID, locations);

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Adding the button that displays all the locations that have been added to the task    @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        mViewAll = (Button)findViewById(R.id.viewAll);
        mViewAll.setVisibility(View.GONE);
        registerForContextMenu(mViewAll);
        //        mViewAll.setOnClickListener(mViewAllListener);
        //        mViewAll.setOnLongClickListener(mViewAllLongListener);

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */


        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Enabling zooming and setting the initial zoom level so all the locations will be      @@@@@    */
        /* @@@@@ visible to the user.                                                                  @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        mDeviceLocation = mMapView.getDeviceLocation();
        if (mDeviceLocation != null) {
            /* Centralizing the map to the last known location of the device */
            mMapView.getController().setCenter(Misc.degToGeo(mDeviceLocation));
        }
        else {
            /* in case device location cannot be obtained, center the map on google headquarters */
            mMapView.getController().setCenter(Misc.degToGeo(new DPoint(37.422032, -122.084059)));
        }
        mMapView.setBuiltInZoomControls(true);
        mMapView.setZoomByAllLocations();
        //        mRadius = AdjustedMap.equatorLen / Math.pow(2, mMapView.getZoomLevel() - 1);

        int lon = mMapView.getLongitudeSpan();
        int lat = mMapView.getLatitudeSpan();
        float[] hight = new float[1], width = new float[1];
        if (lat==0 || lon==0){
            hight[0]=286; // initialized map hight. need to be changed if initial zoom level changes
            width[0]=241; //initialized map width. need to be changed if initial zoom level changes
        }else{
            Location.distanceBetween(0, 0, 0, ((double)lon)/1000000, hight);
            Location.distanceBetween(0, 0, ((double)lat)/1000000, 0, width);
        }

        mRadius = Math.min(hight[0], width[0]) / 2;

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */

        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        /* @@@@@ Setting the mechanism of reading user input and interpreting it whether it's specific @@@@@    */
        /* @@@@@ location or Type location                                                             @@@@@    */
        /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    */
        mAddress = (EditText)findViewById(R.id.specificAddress);
        final Button addressButton = (Button)findViewById(R.id.specificAddAddressButton);
        mMapView.setFocusableInTouchMode(true);
        addressButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = mAddress.getText().toString();
                mMapView.requestFocus();
                /* 2 following rows are for hiding the keyboard */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mAddress.getWindowToken(), 0);


                if (Misc.isType(text)) {
                    if (!mTypes.contains(text)) {
                        String a = Misc.geoToDeg(mMapView.getMapCenter()).toString();
                        new AsyncGooglePlacesQuery().execute(text, a, (new Double(mRadius).toString()));
                    }
                    else {
                        AlertDialog dialog = new AlertDialog.Builder(SpecificMapLocation.this).create();
                        dialog.setIcon(android.R.drawable.ic_dialog_alert);
                        dialog.setTitle(R.string.AD_map_alert_dialog_title);
                        dialog.setMessage(r.getString(R.string.AD_location_already_attached));
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.AD_DLG_ok),
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dg, int which) {
                                return;
                            }
                        });
                        dialog.show();

                    }
                }
                else new AsyncLocationResolver().execute(text);
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

        mAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mAddress.setText(""); //$NON-NLS-1$
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                // only will trigger it if no physical keyboard is open
                mgr.showSoftInput(mAddress, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        mLocationDB.close();
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
                    mNullPeople.clear();
                    mMapView.clearOverlay(PEOPLE_OVERLAY);
                    for (String contact : ger) {
                        Cursor c = mPeopleDB.fetchByMail(contact);
                        DPoint dp = null;
                        if (c != null) {
                            if (c.moveToFirst()) {
                                FriendProps fp = AroundroidDbAdapter.userToFP(c);
                                if (fp.isValid()) {
                                    dp = new DPoint(fp.getDlat(), fp.getDlon());
                                    mPeople.put(contact, dp);
                                    mapFunctions.addPeopleToMap(mMapView, PEOPLE_OVERLAY,
                                            new String[] { contact }, new DPoint[] { dp }, mTaskID);
                                    mMapView.getController().setCenter(Misc.degToGeo(dp));
                                }
                                else mNullPeople.add(contact);
                            }
                            c.close();
                        }
                        else mNullPeople.add(contact);
                    }
                }
                LinkedHashSet<String> locations = new LinkedHashSet<String>();
                locations.addAll(mPeople.keySet());
                locations.addAll(mNullPeople);
                mLocationService.syncLocationsByPeople(mTaskID, locations);
            }
        }

        if (requestCode == MENU_TAPPED_GROUP) {
            if (resultCode == RESULT_FIRST_USER) {
                mMapView.removeTapItem(mPressedItemIndex);
            }
        }

        if (requestCode == MENU_SPECIFIC_GROUP) {
            if (resultCode == RESULT_FIRST_USER) {
                mMapView.removeItemFromOverlay(SPECIFIC_OVERLAY, mPressedItemIndex);
            }
        }
        if (requestCode == MENU_KIND_GROUP) {
            if (resultCode == Focaccia.RESULT_CODE_DELETE_ALL) {
                if (mPressedItemExtras != null) {
                    mMapView.removeItemFromOverlayByExtras(TYPE_OVERLAY, mPressedItemExtras);
                    mTypes.remove(mPressedItemExtras);
                }
            }
        }
        if (requestCode == MENU_PEOPLE_GROUP) {
            if (resultCode == RESULT_FIRST_USER) { /* DELETE was made */
                if (mLastNullPeople != null) {
                    mNullPeople.remove(mLastNullPeople);
                    mLastNullPeople = null;
                }
                else mMapView.removeItemFromOverlay(PEOPLE_OVERLAY, mPressedItemIndex);
            }
            LinkedHashSet<String> locations = new LinkedHashSet<String>();
            locations.addAll(mPeople.keySet());
            locations.addAll(mNullPeople);
            mLocationService.syncLocationsByPeople(mTaskID, locations);
        }

        if (requestCode == AdjustedMap.AM_REQUEST_CODE) {
            if (resultCode == Focaccia.RESULT_CODE_DELETE) {
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
        for (String t : mTypes)
            types[i++] = t;

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
        mLocationDB.close();
        saveAndQuit();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPeopleDB.close();
        mLocationDB.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHan.removeCallbacks(mUpdateTimeTask);
        mLocationDB.close();
        saveAndQuit();
    }

    private class AsyncGooglePlacesQuery extends AsyncTask<String, Void, Map<String, DPoint>> {
        private ProgressDialog pDialog = null;
        private String searchType;
        private double radius;
        private DPoint center;
        private GeoPoint centerGP;
        @Override
        protected void onPreExecute() {
            Resources r = getResources();
            pDialog = ProgressDialog.show(SpecificMapLocation.this, null, r.getString(R.string.AD_map_searching), true);
            super.onPreExecute();
        }

        @Override
        protected Map<String, DPoint> doInBackground(String... params) {
            searchType = params[0];
            radius = new Double(params[2]);
            center = new DPoint(params[1]);
            centerGP = Misc.degToGeo(center);
            Map<String, DPoint> data = null;
            Cursor cursor = mLocationDB.fetchByTypeComplex(searchType, centerGP.getLatitudeE6(), centerGP.getLongitudeE6(), radius);
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null)
                    cursor.close();
                try {
                    data = Misc.googlePlacesQuery(searchType, center, radius);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                data = new HashMap<String, DPoint>();
                while (!cursor.isAfterLast()) {
                    data.put(cursor.getString(cursor.getColumnIndex(LocationsDbAdapter.KEY_BUSINESS_NAME)),
                            Misc.geoToDeg(new GeoPoint(cursor.getInt(cursor.getColumnIndex(LocationsDbAdapter.KEY_LATITUDE)),
                                    cursor.getInt(cursor.getColumnIndex(LocationsDbAdapter.KEY_LONGITUDE)))));
                    cursor.moveToNext();
                }
                cursor.close();
            }
            return data;
        }

        @Override
        protected void onPostExecute(Map<String, DPoint> result) {
            pDialog.cancel();
            if (result == null) {
                mTypes.add(searchType);
                return;
            }
            mLocationDB.createType(searchType, centerGP.getLatitudeE6(), centerGP.getLongitudeE6(), radius, result);
            for (Entry<String, DPoint> element : result.entrySet()) {
                GeoPoint gp = Misc.degToGeo(element.getValue());
                String savedAddr = mapFunctions.getSavedAddressAndUpdate(gp.getLatitudeE6(), gp.getLongitudeE6());
                mMapView.addItemToOverlay(gp, element.getKey(), searchType, savedAddr, TYPE_OVERLAY, mTaskID, searchType);
            }
            mMapView.invalidate();
            mTypes.add(searchType);
            GeoPoint closetPoint = mMapView.getPointWithMinimalDistanceFromGivenPoint(TYPE_OVERLAY, searchType, center);
            if (closetPoint != null)
                mMapView.getController().setCenter(closetPoint);
            super.onPostExecute(result);
        }

    }

    private class AsyncLocationResolver extends AsyncTask<String, Void, GeoPoint> {
        private ProgressDialog pDialog = null;
        private String resolvedAddr = null;

        @Override
        protected void onPreExecute() {
            Resources r = getResources();
            pDialog = ProgressDialog.show(SpecificMapLocation.this, null, r.getString(R.string.AD_map_searching), true);
            super.onPreExecute();
        }

        @Override
        protected GeoPoint doInBackground(String... params) {
            DPoint savedCoords = mapFunctions.getSavedCoordinateAndUpdate(params[0]);
            GeoPoint gp = Misc.degToGeo(savedCoords);
            if (savedCoords != null) {
                String savedAddr = mapFunctions.getSavedAddressAndUpdate(gp.getLatitudeE6(), gp.getLongitudeE6());
                if (savedAddr != null)
                    resolvedAddr = savedAddr;
                return gp;
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoPoint result) {
            Resources r = getResources();
            if (result == null) {
                pDialog.cancel();
                AlertDialog dialog = new AlertDialog.Builder(SpecificMapLocation.this).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.setTitle(R.string.AD_map_alert_dialog_title);
                dialog.setMessage(r.getString(R.string.AD_unresolvable_location));
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.AD_DLG_ok),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        return;
                    }
                });
                dialog.show();
                super.onPostExecute(result);
                return;
            }
            if (false == mMapView.addItemToOverlay(result, r.getString(R.string.AD_kind_specific), resolvedAddr, resolvedAddr, SPECIFIC_OVERLAY, mTaskID, null)) {
                pDialog.cancel();
                AlertDialog dialog = new AlertDialog.Builder(SpecificMapLocation.this).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.setTitle(R.string.AD_map_alert_dialog_title);
                dialog.setMessage(r.getString(R.string.AD_location_already_attached));
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.AD_DLG_ok),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        return;
                    }
                });
                dialog.show();
                super.onPostExecute(result);
                return;
            }
            else mMapView.getController().setCenter(result);
            mMapView.invalidate();
            pDialog.cancel();
            super.onPostExecute(result);
        }

    }

}
