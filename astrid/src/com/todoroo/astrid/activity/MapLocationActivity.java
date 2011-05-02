package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.aroundroidgroup.astrid.googleAccounts.AroundRoidAppConstants;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest.FriendProps;
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
import com.todoroo.astrid.data.Task;

public class MapLocationActivity extends MapActivity implements OnZoomListener  {

    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task mCurrentTask = null;
    //private final Location deviceLocation = null;
    private MapController mapController;
    private MapView mapView;
    private MapItemizedOverlay itemizedoverlay;
    private MapItemizedOverlay specificOverlay;
    private MapItemizedOverlay peopleOverlay;
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

    public void onZoom(boolean zoomIn) {
        /* just for checking */
        mapController.setZoom(1);
        Toast.makeText(this, "did zoom", Toast.LENGTH_LONG).show();
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
    setContentView(R.layout.map_main);
    boolean specificTitleToPresent = false;
    boolean kindTitleToPresent = false;
    //deviceLocation = myService.getLastUserLocation();
    mapView = (MapView) findViewById(R.id.mapview);

    mapController = mapView.getController();
    /* receiving task from the previous activity and extracting the tags from it */
    Bundle b = getIntent().getExtras();
    mCurrentTask = (Task) b.getParcelable(MAP_EXTRA_TASK);
    TextView title = (TextView)findViewById(R.id.takeTitle);
    title.setText(mCurrentTask.getValue(Task.TITLE));

    /* setting up the overlay system which will allow us to add drawable object that will mark */
    /* LocationsByType and/or SpecificLocation and/or People */
    mapOverlays = mapView.getOverlays();
    Drawable drawable = this.getResources().getDrawable(R.drawable.icon_32);
    itemizedoverlay = new MapItemizedOverlay(drawable);

    Drawable drawable2 = this.getResources().getDrawable(R.drawable.icon_pp);
    specificOverlay = new MapItemizedOverlay(drawable2);

    Drawable drawable3 = this.getResources().getDrawable(R.drawable.icon_producteev);
    peopleOverlay = new MapItemizedOverlay(drawable3);


    /* adding people that are related to the task */
    people = locationService.getLocationsByPeopleAsArray(mCurrentTask.getId());
    if (people.length > 0) {
        try {
            String cat = AroundRoidAppConstants.join(people, "::");
            List<FriendProps> fp = PeopleRequest.requestPeople(new Location(new String()), cat);
            for (FriendProps f : fp) {
                peopleOverlay.addOverlay(new OverlayItem(Misc.degToGeo(new DPoint(Double.parseDouble(f.getLat()), Double.parseDouble(f.getLon()))), f.getMail(), "people!"));
                mapOverlays.add(peopleOverlay);
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* determine the central point in the map to be current location of the device */
//    if (myService.getLastUserLocation() != null){

        /* Centralizing the map to the last */
        mapView.getController().setCenter(Misc.degToGeo(new DPoint(40.725405, -73.998756)));

        /* enable zoom option */
        mapView.setBuiltInZoomControls(true);



        String[] specifics = locationService.getLocationsBySpecificAsArray(mCurrentTask.getId());
        if (specifics != null) {
            for (int i = 0 ; i < specifics.length ; i++) {
                DPoint d = new DPoint(Double.parseDouble(specifics[i].substring(0, specifics[i].indexOf(','))), Double.parseDouble(specifics[i].substring(specifics[i].indexOf(',') + 1)));
                specificOverlay.addOverlay(new OverlayItem(Misc.degToGeo(d), "specific location", "specific location"));
                mapOverlays.add(specificOverlay);
                specificTitleToPresent = true;
            }
        }


        /* if the task is location-based, the following code will add the locations to the map */
        locationTags = locationService.getLocationsByTypeAsArray(mCurrentTask.getId());
        if (locationTags.length > 0) {
            kindTitleToPresent = true;
            try {
                PlacesLocations places;
                /* running on all the tags (bank, post-office, ATM, etc... */
                for (int i = 0 ; i < locationTags.length ; i++) {
                    /* initializing the PlacesLocations object with the relevant tag and current location */
                    places = new PlacesLocations(locationTags[i], new DPoint(40.725405, -73.998756));
                    /* calling the function, which is responsible adding location to the map, with the */
                    /* all the places obtained from Google Local Search */
                    addToMap(places.getPlaces());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            /* zooming-in for a better view on the results */
            mapController.setZoom(12);

            /* allowing the user to act with the map, including tapping on the locations that were found in */
            /* order to view their names and etc... */
            mapView.setClickable(true);
        }
//    }
    /* showing to the user how many location were found */
    TextView tv = (TextView)findViewById(R.id.searchResults);
    if (kindTitleToPresent)
        tv.setText(itemizedoverlay.size() + " results found !");
    if (specificTitleToPresent)
        tv.setText(tv.getText() + " " + specificOverlay.size() + " specifics found !");
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
