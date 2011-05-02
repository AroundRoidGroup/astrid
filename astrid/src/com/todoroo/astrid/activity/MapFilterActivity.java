package com.todoroo.astrid.activity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Misc;
import com.aroundroidgroup.map.PlacesLocations;
import com.aroundroidgroup.map.placeInfo;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;
@SuppressWarnings("unused")

public class MapFilterActivity extends MapActivity {
    public static final String MAP_EXTRA_TASK = "of"; //$NON-NLS-1$
    private final String TAG = "mapFilterActivity"; //$NON-NLS-1$
    private MapView map;
    private MapController mapController;
    private MapItemizedOverlay kindOverlay;
    private MapItemizedOverlay specificOverlay;
    private MapItemizedOverlay peopleOverlay;
    private List<Overlay> mapOverlays;
    private String[] tags;
    private final LocationService locationService = new LocationService();

    private static final int KIND = 1;
    private static final int SPECIFIC = 2;
    private static final int PEOPLE = 3;

    private void addToMap(List<placeInfo> locations) {
        GeoPoint geoP;
        for (placeInfo loc : locations) {
            geoP = Misc.degToGeo(new DPoint(loc.getLat(), loc.getLng()));
            kindOverlay.addOverlay(new OverlayItem(geoP, loc.getStreetAddress(), loc.getTitle()));
            mapOverlays.add(kindOverlay);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_filter_activity);

        map = (MapView) findViewById(R.id.mapview);

        mapController = map.getController();

        //if (myService.getLastUserLocation() != null){

            // TODO change back to GPS location

            map.getController().setCenter(Misc.degToGeo(new DPoint(40.725405, -73.998756)));
            //mapView.getController().setCenter(Misc.locToGeo(myService.getLastUserLocation()));
            /* enable zoom option */
            map.setBuiltInZoomControls(true);

            mapOverlays = map.getOverlays();

            Drawable drawable = this.getResources().getDrawable(R.drawable.icon_32);
            kindOverlay = new MapItemizedOverlay(drawable);

            Drawable drawable2 = this.getResources().getDrawable(R.drawable.icon_pp);
            specificOverlay = new MapItemizedOverlay(drawable2);

            Drawable drawable3 = this.getResources().getDrawable(R.drawable.notif_pink_alarm);
            peopleOverlay = new MapItemizedOverlay(drawable3);

            mapController.setZoom(13);

            /* adding the locations of the kind-location */

            tags = locationService.getAllLocationsByType();
            PlacesLocations pl = null;
            for (String tag : tags) {
                try {
                    // TODO change back to GPS coordinates
                    pl = new PlacesLocations(tag, new DPoint(40.725405, -73.998756));
                    addToMap(pl.getPlaces());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            /* adding the locations of the specific-locations */

            String[] str = locationService.getAllLocationsBySpecific();
            if (str != null) {
                for (String s : str) {
                    specificOverlay.addOverlay(new OverlayItem(Misc.degToGeo(new DPoint(Double.parseDouble(s.substring(0, s.indexOf(','))),Double.parseDouble(s.substring(s.indexOf(',') + 1)) )), "bla bla", "lalalal"));
                    mapOverlays.add(specificOverlay);
                }
            }
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
        protected boolean onTap(int index) {
            OverlayItem item = mOverlays.get(index);
            Toast.makeText(MapFilterActivity.this, item.getSnippet(), Toast.LENGTH_LONG).show();
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
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }

}


