package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.AdjustedMap;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Geocoding;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.placeInfo;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;

public class SpecificMapLocation extends MapActivity implements OnZoomListener  {

    public static final String SPECIFIC_POINTS = "SpecificMapLocation"; //$NON-NLS-1$
    public static final String SPECIFIC_POINTS_SECOND = "SpecificMapLocation2"; //$NON-NLS-1$

    private final Task mCurrentTask = null;
    //private final Location deviceLocation = null;
    private MapController mapController;
    private AdjustedMap mapView;
    private MapItemizedOverlay itemizedoverlay;
    private List<Overlay> mapOverlays;
    private String[] locationTags;
    private String[] places;
    private String[] people;
    private final LocationService locationService = new LocationService();
    private final int locationCount = 0;
    /** Called when the activity is first created. */
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void removeFromMap() {
        mapOverlays.clear();
    }

    private void addToMap(List<placeInfo> locations) {
        GeoPoint geoP;
        for (placeInfo loc : locations) {
            geoP = Misc.degToGeo(new DPoint(loc.getLat(), loc.getLng()));
            itemizedoverlay.addOverlay(new OverlayItem(geoP, loc.getStreetAddress(), loc.getTitle()));
            mapOverlays.add(itemizedoverlay);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.specific_map);

        mapView = (AdjustedMap) findViewById(R.id.mapview);

        mapController = mapView.getController();

        Drawable drawable = this.getResources().getDrawable(R.drawable.icon_pp);
        itemizedoverlay = new MapItemizedOverlay(drawable);

        mapOverlays = mapView.getOverlays();

        /* receiving task from the previous activity and extracting the tags from it */

        Bundle bb = getIntent().getExtras();
        String[] existedSpecificLocations = bb.getStringArray(SPECIFIC_POINTS);
        for (int i = 0 ; i < existedSpecificLocations.length ; i++) {
            DPoint d = new DPoint(Double.parseDouble(existedSpecificLocations[i].substring(0, existedSpecificLocations[i].indexOf(','))), Double.parseDouble(existedSpecificLocations[i].substring(existedSpecificLocations[i].indexOf(',') + 1)) );
            itemizedoverlay.addOverlay(new OverlayItem(Misc.degToGeo(d), "bla", "ofa"));
            mapOverlays.add(itemizedoverlay);
        }


        TextView title = (TextView)findViewById(R.id.takeTitle);
        title.setText("Specific Location Activity"); //$NON-NLS-1$

        /* enable zoom option */
        mapView.setBuiltInZoomControls(true);


        /* determine the central point in the map to be current location of the device */
        //if (myService.getLastUserLocation() != null){

            /* Centralizing the map to the last */
            // TODO change back to GPS location

            mapView.getController().setCenter(Misc.degToGeo(new DPoint(40.725405, -73.998756)));
            //mapView.getController().setCenter(Misc.locToGeo(myService.getLastUserLocation()));


        //}

        final EditText address = (EditText)findViewById(R.id.specificAddress);

        mapView.getController().setZoom(13);
        Button b = (Button)findViewById(R.id.specificButton);

        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                String[] classOvelrays = new String[itemizedoverlay.size()];
                for (int i = 0 ; i < itemizedoverlay.size() ; i++) {
                    DPoint dd = Misc.geoToDeg(itemizedoverlay.getItem(i).getPoint());
                    classOvelrays[i] = new String(dd.getX() + "," + dd.getY());
                }
                String[] allPointsFromMap = mapView.getAllPoints();
                String[] alllll = new String[classOvelrays.length + allPointsFromMap.length];
                for (int i = 0 ; i < classOvelrays.length ; i++)
                    alllll[i] = classOvelrays[i];
                for (int i = classOvelrays.length ; i < classOvelrays.length + allPointsFromMap.length ; i++)
                    alllll[i] = allPointsFromMap[i - classOvelrays.length];
                intent.putExtra(SPECIFIC_POINTS_SECOND, alllll);
//                for (String d : mapView.getAllPoints())
//                    Toast.makeText(SpecificMapLocation.this, d.getX() + " " + d.getY(), Toast.LENGTH_LONG).show(); //$NON-NLS-1$
                setResult(TaskEditActivity.SPECIFIC_LOCATION_MAP_RESULT_CODE, intent);
                SpecificMapLocation.this.finish();
            }
        });

        Button addressButton = (Button)findViewById(R.id.specificAddAddressButton);
        addressButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DPoint d = null;
                try {
                    d = Geocoding.geocoding(address.getText().toString());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (d != null) {
                    itemizedoverlay.addOverlay(new OverlayItem(Misc.degToGeo(d), "bla", "ofa"));
                    mapOverlays.add(itemizedoverlay);
                    mapView.invalidate();
                }
                else Toast.makeText(SpecificMapLocation.this, "Address not found!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public class MapItemizedOverlay extends ItemizedOverlay<OverlayItem> {
        private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
        Context mContext;

        public MapItemizedOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
            // TODO Auto-generated constructor stub
        }

        public MapItemizedOverlay(Drawable defaultMarker, Context context) {
            super(defaultMarker);
            mContext = context;
        }

        @Override
        protected OverlayItem createItem(int i) {
            return mOverlays.get(i);
        }

        @Override
        public int size() {
            return mOverlays.size();
        }

        public void addOverlay(OverlayItem overlay) {
            mOverlays.add(overlay);
            populate();
        }
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onZoom(boolean arg0) {
        // TODO Auto-generated method stub

    }
}
