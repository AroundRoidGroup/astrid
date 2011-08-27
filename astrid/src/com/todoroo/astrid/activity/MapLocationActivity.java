package com.todoroo.astrid.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Focaccia;
import com.aroundroidgroup.map.LocationsDbAdapter;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.mapFunctions;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
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

              AlertDialog dialog = new AlertDialog.Builder(MapLocationActivity.this).create();
              dialog.setIcon(android.R.drawable.ic_dialog_alert);
              dialog.setTitle("Information");
              dialog.setMessage(locationsCount);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (people != null) {
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
        }

        /* If the task is location-based, the following code will add the locations to the map */
        String[] locationTags = locationService.getLocationsByTypeAsArray(mTaskID);
        mapFunctions.addTagsToMap(mMapView, TYPE_OVERLAY, locationTags, mRadius, mTaskID);

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle(mCurrentTask.getValue(Task.TITLE));

        actionBar.addAction(new InformationOnLocations());
        actionBar.addAction(new DeviceLocation());
    }
}
