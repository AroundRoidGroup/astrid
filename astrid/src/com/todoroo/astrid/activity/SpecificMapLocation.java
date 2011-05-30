package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.ConnectedContactsActivity;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.AdjustedOverlayItem;
import com.aroundroidgroup.map.AsyncAutoComplete;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.Geocoding;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.MapActivity;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;

public class SpecificMapLocation extends MapActivity {


    public static final String SPECIFIC_POINTS = "SpecificMapLocation"; //$NON-NLS-1$
    public static final String SPECIFIC_POINTS_SECOND = "SpecificMapLocation2"; //$NON-NLS-1$
    public static final int FOCACCIA_RESULT_CODE = 1;
    public static final int FOCACCIA_RESULT_CODE_BACK_PRESSED = 2;
    public static final int FOCACCIA_RESULT_CODE_FOR_KIND = 3;
    public static final int CONTACTS_REQUEST_CODE = 0;


    private static final int MENU_SPECIFIC_GROUP = 1;
    private static final int MENU_KIND_GROUP = 65536;
    private static final int MENU_PEOPLE_GROUP = 1048576;

    private long taskID;
    private AdjustedMap mapView;

    private LocationService locationService;
    private List<String> types = null;
    private Map<String, DPoint> people = null;
    private Thread previousThread = null;
    private static ArrayAdapter<String> adapter;

    private final AroundroidDbAdapter mdba = new AroundroidDbAdapter(this);

    private final OnClickListener nothingToShowClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Toast.makeText(SpecificMapLocation.this, "No locations for this task", Toast.LENGTH_LONG).show();
        }
    };

    private final OnLongClickListener nothingToShowLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(SpecificMapLocation.this, "No locations for this task", Toast.LENGTH_LONG).show();
            return false;
        }
    };

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    public static void updateSuggestions(List<String> lst) {
        adapter.clear();
        for (String s : lst)
            adapter.add(s);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("All Locations"); //$NON-NLS-1$
        String[] specificAsAddress = mapView.getTappedAddress();
        DPoint[] specificAsCoords = mapView.getTappedCoords();
        for (int i = 0 ; i < specificAsAddress.length ; i++) {
            String address = null;
            if (mapView.getTappedItem(i).getAddress() == null) {
                try {
                    address = Geocoding.reverseGeocoding(specificAsCoords[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (address != null)
                    mapView.getOverlay(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME).getItem(i).setLocationAddress(address);
                else address = specificAsCoords[i].toString();
            }
            else address = mapView.getTappedItem(i).getAddress();
            menu.add(MENU_SPECIFIC_GROUP, MENU_SPECIFIC_GROUP + i, Menu.NONE, address);
        }
        for (int i = 0 ; i < types.size() ; i++)
            menu.add(MENU_KIND_GROUP, MENU_KIND_GROUP + i, Menu.NONE, types.get(i));
        int i = 0;
        for (Map.Entry<String, DPoint> p : people.entrySet()) {
            menu.add(MENU_PEOPLE_GROUP, MENU_PEOPLE_GROUP + i++, Menu.NONE, p.getKey());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getGroupId()) {
        case MENU_SPECIFIC_GROUP:
            mapView.getController().setCenter(Misc.degToGeo(new DPoint(item.getTitle().toString())));
            Intent intent = new Intent(ContextManager.getContext(), Focaccia.class);
            AdjustedOverlayItem mItem = mapView.getTappedItem(item.getItemId() - MENU_SPECIFIC_GROUP);
            String[] sentData = new String[6];
            sentData[0] = item.getItemId() - MENU_SPECIFIC_GROUP + ""; //$NON-NLS-1$
            sentData[1] = item.getTitle().toString();
            sentData[2] = mItem.getTitle();
            sentData[3] = mItem.getSnippet();
            sentData[4] = mItem.getAddress();
            sentData[5] = "1"; // can be removed //$NON-NLS-1$
            intent.putExtra(Focaccia.SOURCE_SPECIFICMAP, sentData);
            this.startActivityForResult(intent, 1);
            return true;
        case MENU_KIND_GROUP:
            //TODO find the closest location of this type and center the map around it
            Intent intentKind = new Intent(ContextManager.getContext(), Focaccia.class);
            String[] sentDataKind = new String[2];
            sentDataKind[0] = mapView.getOverlaySize(AdjustedMap.KIND_OVERLAY_UNIQUE_NAME) + ""; //$NON-NLS-1$
            sentDataKind[1] = item.getTitle().toString();
            intentKind.putExtra(Focaccia.SOURCE_SPECIFICMAP_KIND, sentDataKind);
            this.startActivityForResult(intentKind, 1);
            return true;
        case MENU_PEOPLE_GROUP:
            mapView.getController().setCenter(Misc.degToGeo(people.get(item.getTitle())));
            Intent intentPeople = new Intent(ContextManager.getContext(), Focaccia.class);
            AdjustedOverlayItem peopleItem = mapView.getOverlay(AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME).getItem(item.getItemId() - MENU_PEOPLE_GROUP);
            String[] sentDataPeople = new String[6];
            sentDataPeople[0] = item.getItemId() - MENU_PEOPLE_GROUP + ""; //$NON-NLS-1$
            sentDataPeople[1] = item.getTitle().toString();
            sentDataPeople[2] = peopleItem.getTitle();
            sentDataPeople[3] = peopleItem.getSnippet();
            sentDataPeople[4] = peopleItem.getAddress();
            sentDataPeople[5] = "1"; // can be removed //$NON-NLS-1$
            intentPeople.putExtra(Focaccia.SOURCE_SPECIFICMAP, sentDataPeople);
            this.startActivityForResult(intentPeople, 1);
            return true;
        default: return super.onContextItemSelected(item);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.specific_map);

        mapView = (AdjustedMap) findViewById(R.id.mapview);

        mdba.open();



        DPoint deviceLocation = mapView.getDeviceLocation();

        /* allowing adding of location by tapping on the map */
        mapView.enableAddByTap();

        //        /* disabling the feature that shows the device location on the map */
        //        mapView.removeDeviceLocation();

        mapView.createOverlay(AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_32));
        mapView.createOverlay(AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, this.getResources().getDrawable(R.drawable.icon_people));



        /* adding the auto-complete mechanism */
        final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.specificAddress);
        adapter = new ArrayAdapter<String>(SpecificMapLocation.this, R.layout.search_result_list, new String[0]);
        textView.setAdapter(adapter);

        textView.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (previousThread != null) {
                    if (previousThread.isAlive())
                        previousThread.destroy();
                    previousThread = null;
                }
                previousThread = new Thread(new AsyncAutoComplete(textView.getText().toString()));
                previousThread.run();
                return false;
            }
        });





        /* getting the data from the calling activity. */
        /* this data contains the taskID as first object in the array and the points */
        /* that already added as specific locations as the other array elements. */
        Bundle bundle = getIntent().getExtras();
        String[] existedSpecificLocations = bundle.getStringArray(SPECIFIC_POINTS);
        taskID = Long.parseLong(existedSpecificLocations[0]);

        mapView.associateMapWithTask(taskID);
        for (int i = 1 ; i < existedSpecificLocations.length ; i++) {
            DPoint d = new DPoint(existedSpecificLocations[i]);
            if (d.isNaN())
                continue;
            String address = null;
            try {
                address = Geocoding.reverseGeocoding(d);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (address == null)
                address = new String("Specific Location"); //$NON-NLS-1$
            mapView.addTappedLocation(Misc.degToGeo(d), "Specific Location", address); //$NON-NLS-1$
        }

        locationService = new LocationService();
        types = new ArrayList<String>();
        /* adding the existed business types */
        String[] existedTypes = locationService.getLocationsByTypeAsArray(taskID);
        for (String s : existedTypes)
            types.add(s);
        int[] returnValues = mapFunctions.addTagsToMap(mapView, AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, existedTypes, 500.0);
        int sum = 0;
        for (int i : returnValues)
            sum += i;
        if (sum == returnValues.length)
            Toast.makeText(this, "All types have been added successfully!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
        else if (sum == 0)
            Toast.makeText(this, "Failed to add types!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
        else Toast.makeText(this, "Only some types have been added!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$

        locationService = new LocationService();
        people = new HashMap<String, DPoint>();
        /* adding the existed business types */
        String[] existedPeople = locationService.getLocationsByPeopleAsArray(taskID);
        for (String s : existedPeople)
            people.put(s, null);

        String[] tomer = new String[1];
        tomer[0] = "tomer.keshet@gmail.com";
        DPoint[] coordTomer = new DPoint[1];
        coordTomer[0] = new DPoint(40.710215,-74.009013);
        mapFunctions.addPeopleToMap(mapView, AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, tomer, coordTomer);
        people.put(tomer[0], coordTomer[0]);

        final Button viewAll = (Button)findViewById(R.id.viewAll);
        registerForContextMenu(viewAll);
        if (mapView.getTappedPointsCount() == 0 && types.size() == 0 && people.size() == 0) {
            viewAll.setOnClickListener(nothingToShowClickListener);
            viewAll.setOnLongClickListener(nothingToShowLongClickListener);
        }

        ((Button)findViewById(R.id.addPeople)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContextManager.getContext(),ConnectedContactsActivity.class);
                startActivityForResult(intent,CONTACTS_REQUEST_CODE);
            }
        });


        /* getting the task by the taskID that has been extracted from the Intent */
        //        TaskService taskService = new TaskService();
        //        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.TITLE).
        //                where(MetadataCriteria.byTask(taskID)).
        //                orderBy(SortHelper.defaultTaskOrder()).limit(100));
        //        try {
        //            Toast.makeText(this, cursor.getCount() + " results", Toast.LENGTH_LONG).show();
        //            Task task = new Task();
        //            cursor.moveToNext();
        //            task.readFromCursor(cursor);
        //        } finally {
        //            cursor.close();
        //        }

        /* setting a headline with the task's title */
        //        TextView title = (TextView)findViewById(R.id.takeTitle);

        /* enable zoom option */
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(13);

        //TODO USERLOCATION



        //        if (false){

        /* Centralizing the map to the last known location of the device */
        mapView.getController().setCenter(Misc.degToGeo(deviceLocation));

        //        }
        //        else {

        final EditText address = (EditText)findViewById(R.id.specificAddress);

        Button addressButton = (Button)findViewById(R.id.specificAddAddressButton);
        addressButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DPoint d = null;
                String text = address.getText().toString();

                if (Misc.isType(text)) {
                    String[] type = new String[1];
                    type[0] = text.replace(' ', '_');
                    mapFunctions.addTagsToMap(mapView, AdjustedMap.KIND_OVERLAY_UNIQUE_NAME, type, 500.0);
                    mapView.invalidate();
                    types.add(text);
                    viewAll.setOnClickListener(null);
                    viewAll.setOnLongClickListener(null);
                }
                else {
                    try {
                        d = Geocoding.geocoding(text);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (d != null) {
                        String address = null;
                        try {
                            address = Geocoding.reverseGeocoding(d);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (address == null)
                            address = new String("Specific Location"); //$NON-NLS-1$
                        mapView.addTappedLocation(Misc.degToGeo(d), "Specific Location", address); //$NON-NLS-1$
                        mapView.invalidate();
                    }
                    else Toast.makeText(SpecificMapLocation.this, "Address not found!", Toast.LENGTH_LONG).show(); //$NON-NLS-1$

                }
            }
        });

        address.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                address.setText(""); //$NON-NLS-1$
                address.setOnClickListener(null);
            }
        });
        //        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CONTACTS_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                //TODO a contact was picked! add it to control set
                Bundle bundle = data.getExtras();
                String contact = bundle.getCharSequence(ConnectedContactsActivity.FRIEND_MAIL).toString();
                Cursor x = mdba.fetchByMail(contact);
                //TODO tomer change this to a better implementation
                if (x!=null && x.moveToFirst()){
                    //LAT AND THEN LON
                    //DPoint dp = new DPoint(x.getDouble(x.getColumnIndex(AroundroidDbAdapter.KEY_LAT)),x.getDouble(x.getColumnIndex(AroundroidDbAdapter.KEY_LON)));
                    DPoint dp = new DPoint(40.716558,-74.00013);
                    people.put(contact, dp);
                    String[] person = new String[1];
                    person[0] = contact;
                    DPoint[] nekuda = new DPoint[1];
                    nekuda[0] = dp;
                    mapFunctions.addPeopleToMap(mapView, AdjustedMap.PEOPLE_OVERLAY_UNIQUE_NAME, person, nekuda);
                }
                if (x!=null){
                    x.close();
                }

            }
        }

        if (resultCode == Focaccia.FOCACCIA_RESULT_CODE_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode == Focaccia.FOCACCIA_RESULT_CODE_REMOVE_TAP) {
            Bundle bundle = data.getExtras();
            //            Toast.makeText(this, "before there were " + mapView.getAllPointsCount() + " points", Toast.LENGTH_LONG).show(); //$NON-NLS-1$ //$NON-NLS-2$
            mapView.removeTappedLocation(Integer.parseInt(bundle.getString(Focaccia.SOURCE_ADJUSTEDMAP)));
            //            Toast.makeText(this, "after there are " + mapView.getAllPointsCount() + " points", Toast.LENGTH_LONG).show(); //$NON-NLS-1$ //$NON-NLS-2$
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode == Focaccia.FOCACCIA_RESULT_CODE_REMOVE_TYPE) {
            Bundle bundle = data.getExtras();
            String type = bundle.getString(Focaccia.SOURCE_SPECIFICMAP_KIND);
            if (types.contains(type)) {
                //                Toast.makeText(this, "going to remove the type: " + type, Toast.LENGTH_LONG).show();
                mapView.removeTypeLocation(type);
                //                Toast.makeText(this, "type: " + type + " has been removed", Toast.LENGTH_LONG).show();
                types.remove(type);
                if (mapView.getTappedPointsCount() == 0 && types.size() == 0 && people.size() == 0) {
                    Button viewAll = (Button)findViewById(R.id.viewAll);
                    viewAll.setOnClickListener(nothingToShowClickListener);
                    viewAll.setOnLongClickListener(nothingToShowLongClickListener);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode == Focaccia.FOCACCIA_RESULT_CODE_BACK_PRESSED) {
            Toast.makeText(this, "no deletion", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            String[] receivedData = data.getExtras().getStringArray(Focaccia.SOURCE_ADJUSTEDMAP);
            if (receivedData[1] != null)
                mapView.getOverlay(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME).getItem(Integer.parseInt(receivedData[0])).setLocationAddress(receivedData[1]);
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void saveAndQuit() {
        /* WHAT IS SENT BACK: array which the first part of it contains the location and second part
         *                    of it contains the addresses (null if certain location has never been
         *                    reversed geocoded. location at position k in the array has its address
         *                    at position 2k in the array. */
        Intent intent = new Intent();

        List<String> dataToSendBack = new ArrayList<String>();

        /* adding the points coordinates */
        DPoint[] points = mapView.getTappedCoords();
        for (DPoint p : points)
            dataToSendBack.add(p.toString());
        points = mapView.getAllByIDAsCoords(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME);
        for (DPoint p : points)
            dataToSendBack.add(p.toString());

        /* adding the points addresses */
        String[] addresses = mapView.getTappedAddress();
        for (String s : addresses)
            dataToSendBack.add(s);
        addresses = mapView.getAllByIDAsAddress(AdjustedMap.SPECIFIC_OVERLAY_UNIQUE_NAME);
        for (String s : addresses)
            dataToSendBack.add(s);

        /* adding delimiter separating specificPoints and types */
        dataToSendBack.add(Misc.locationsDelimiter);

        /* adding the types */
        for (String t : types)
            dataToSendBack.add(t);

        /* adding delimiter separating types and people */
        dataToSendBack.add(Misc.locationsDelimiter);

        /* adding the people */
        for (Map.Entry<String, DPoint> pair : people.entrySet())
            dataToSendBack.add(pair.getKey());

        String[] dataToSendBackAsArray = new String[dataToSendBack.size()];
        for (int i = 0 ; i < dataToSendBackAsArray.length ; i++)
            dataToSendBackAsArray[i] = dataToSendBack.get(i);

        intent.putExtra(SPECIFIC_POINTS_SECOND, dataToSendBackAsArray);
        setResult(TaskEditActivity.SPECIFIC_LOCATION_MAP_RESULT_CODE, intent);
        SpecificMapLocation.this.finish();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mdba.close();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Toast.makeText(this, "press back", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            saveAndQuit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
