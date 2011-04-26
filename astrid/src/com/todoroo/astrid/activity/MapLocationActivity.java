package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.map.DPoint;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;


public class MapLocationActivity extends MapActivity implements OnZoomListener  {

    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task mCurrentTask = null;
    //private final Location deviceLocation = null;
    private MapController mapController;
    private MapView mapView;
    private HelloItemizedOverlay itemizedoverlay;
    private List<Overlay> mapOverlays;
    private String[] locationTags;
    /** Called when the activity is first created. */
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private double getParameterizedRadius() {
        /* consider using speed */
        double radius = 1000;
        int zoomLevel = mapView.getZoomLevel();
        return radius;
    }

    public void onZoom(boolean zoomIn) {
        /* just for checking */
        mapController.setZoom(1);
        removeFromMap();
        addToMap();
    }

    private void removeFromMap() {
        mapOverlays.clear();
    }

    private void addToMap() {
        DPoint placeCoord = null;
        Map<String, String> places;
        GeoPoint geoP;
        String s = "";
        /**for (String kind : locationTags) {
            s += kind + ": ";
            places = Misc.getPlaces(kind, getParameterizedRadius(), myService.getLastUserLocation(), 5);
            s += places.size() + " result found.\n";
            if (places != null) {
                for (Map.Entry<String, String> p : places.entrySet()) {
                    try {
                        placeCoord = Misc.getCoords(p.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (placeCoord != null) {
                        geoP = degToGeo(placeCoord);
                        itemizedoverlay.addOverlay(new OverlayItem(geoP, kind,  p.getKey()));
                        mapOverlays.add(itemizedoverlay);
                    }
                }
            }
            TextView results = (TextView)findViewById(R.id.searchResults);
            results.setText(s.substring(0, s.length() - 1));
        }*/
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_main);

        //deviceLocation = myService.getLastUserLocation();
        mapView = (MapView) findViewById(R.id.mapview);
        mapController = mapView.getController();
        /* receiving task from the previous activity and extracting the tags from it */
        Bundle b = getIntent().getExtras();
        mCurrentTask = (Task) b.getParcelable(MAP_EXTRA_TASK);
        TextView title = (TextView)findViewById(R.id.takeTitle);
        title.setText(mCurrentTask.getValue(Task.TITLE));
        /* determine the central point in the map to be current location of the device */
        if (myService.getLastUserLocation() != null){
            mapView.getController().setCenter(locToGeo(myService.getLastUserLocation()));
            /* enable zoom option */
            mapView.setBuiltInZoomControls(true);

            mapOverlays = mapView.getOverlays();
            Drawable drawable = this.getResources().getDrawable(R.drawable.icon_32);
            itemizedoverlay = new HelloItemizedOverlay(drawable);
            //mapOverlays.add(itemizedoverlay);
            addToMap();
            mapController.setZoom(15);
            mapView.setClickable(true);
        }
        else{
            Toast.makeText(this, "null location", Toast.LENGTH_LONG);
        }
    }

    private GeoPoint degToGeo(DPoint dp) {
        return new GeoPoint((int)(dp.getX() * 1000000), (int)(dp.getY() * 1000000));
    }

    private GeoPoint locToGeo(Location l) {
        return new GeoPoint((int)(l.getLatitude() * 1000000), (int)(l.getLongitude() * 1000000));
    }

    /*
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            makeUseOfNewLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {    }

        public void onProviderDisabled(String provider) {}
    };
    */

    /*
    private void gpsSetup() {
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        makeUseOfNewLocation(location);
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
    }
    */

    /*
    private void makeUseOfNewLocation(Location location) {
        deviceLocation = location;
    }
    */

    public class HelloItemizedOverlay extends ItemizedOverlay<OverlayItem> {
        private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
        Context mContext;

        public HelloItemizedOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
            // TODO Auto-generated constructor stub
        }

        public HelloItemizedOverlay(Drawable defaultMarker, Context context) {
            super(defaultMarker);
            mContext = context;
        }

        @Override
        protected boolean onTap(int index) {
            OverlayItem item = mOverlays.get(index);
            Toast.makeText(MapLocationActivity.this, item.getSnippet(), Toast.LENGTH_LONG).show();
            return true;
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
    public void onVisibilityChanged(boolean arg0) {
        // TODO Auto-generated method stub

    }
}
