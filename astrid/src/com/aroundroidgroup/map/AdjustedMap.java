package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.locationTags.LocationService;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;

public class AdjustedMap extends MapView {

    Context context = null;
    private DPoint lastPointedLocation = null;
    private long currentTastID = -1;
    private String address = null;
    private Map<String, MapItemizedOverlay> overlays;
    private List<Overlay> mapOverlays;

    /* setting this boolean to true, will enable the feature that add location by */
    /* tapping on the map */
    private boolean addByTap = false;
    private static final String UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER = "KAPZHrRxCrtfTINb4zRjSWCXYuFBBc34P1hF6jgwuV059jLr"; //$NON-NLS-1$

    /* setting this boolean to true, will enable the feature that shows the device location on the map */
    private boolean showDeviceLocation = false;

    public static final String SPECIFIC_OVERLAY_UNIQUE_NAME = "specific"; //$NON-NLS-1$
    public static final String KIND_OVERLAY_UNIQUE_NAME = "kind"; //$NON-NLS-1$
    public static final String PEOPLE_OVERLAY_UNIQUE_NAME = "people"; //$NON-NLS-1$
    private static final String DEVICE_LOCATION_OVERLAY_UNIQUE_NAME = "deviceLocation"; //$NON-NLS-1$

    private AroundroidDbAdapter db;

    public AdjustedMap(Context context, String apiKey) {
        super(context, apiKey);
        this.context = context;
        init();
    }

    public AdjustedMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public AdjustedMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    public void showDeviceLocation() {
        if (showDeviceLocation == false) {
            showDeviceLocation = true;
            createOverlay(DEVICE_LOCATION_OVERLAY_UNIQUE_NAME, getResources().getDrawable(R.drawable.device_location));
            DPoint deviceLocation = db.specialUserToDPoint();
            if (deviceLocation != null) {
                GeoPoint lastDeviceLocation = Misc.degToGeo(deviceLocation);
                addItemToOverlay(lastDeviceLocation, "Your Location", deviceLocation.toString(), null, DEVICE_LOCATION_OVERLAY_UNIQUE_NAME); //$NON-NLS-1$
            }
        }
    }

    public DPoint getDeviceLocation() {
        return db.specialUserToDPoint();
    }

    public void removeDeviceLocation() {
        if (showDeviceLocation == true) {
            showDeviceLocation = false;
            mapOverlays.remove(overlays.get(DEVICE_LOCATION_OVERLAY_UNIQUE_NAME));
            overlays.remove(DEVICE_LOCATION_OVERLAY_UNIQUE_NAME);
        }
    }

    public boolean createOverlay(String uniqueName, Drawable d) {
        if (uniqueName == null || d == null)
            return false;
        if (overlays.get(uniqueName) == null) {
            overlays.put(uniqueName, new MapItemizedOverlay(d));
            return true;
        }
        return false;
    }

    public MapItemizedOverlay getOverlay(String uniqueName) {
        return overlays.get(uniqueName);
    }

    public void addItemToOverlay(GeoPoint g, String title, String snippet, String address, String identifier) {
        if (identifier == null)
            return;
        MapItemizedOverlay overlay = overlays.get(identifier);
        if (overlay != null) {
            if (g != null && title != null && snippet != null) {
                overlay.addOverlay(new AdjustedOverlayItem(g, title, snippet, address));
                mapOverlays.add(overlay);
            }
        }
        invalidate();
    }

    public int getAllPointsCount() {
        int count = 0;
        for (Map.Entry<String, MapItemizedOverlay> pair : overlays.entrySet())
            count += pair.getValue().size();
        return count;
    }

    public int getTappedPointsCount() {
        if (addByTap)
            return overlays.get(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER).size();
        return 0;
    }

    public int getOverlaySize(String identifier) {
        if (identifier == null)
            return -1;
        MapItemizedOverlay iOver = overlays.get(identifier);
        if (iOver == null)
            return 0;
        return iOver.size();
    }

    public void associateMapWithTask(long taskID) {
        currentTastID = taskID;
    }

    private void init() {
        db = new AroundroidDbAdapter(context);
        //TODO close db
        db.open();
        overlays = new HashMap<String, MapItemizedOverlay>();
        mapOverlays = getOverlays();
        showDeviceLocation();
        getController().setZoom(18);
        //TODO USERLOCATION
        //        if (true)
        //            return;
        DPoint d = new DPoint(40.714867,-74.006009);
        getController().setCenter(Misc.degToGeo(d));
    }

    /* calling this function will automatically add an overlay for specific locations */
    public void enableAddByTap() {
        addByTap = true;
        createOverlay(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER,
                this.getResources().getDrawable(R.drawable.icon_pp));
    }

    public void addTappedLocation(GeoPoint g, String title, String snippet) {
        if (addByTap)
            addItemToOverlay(g, title, snippet, snippet, UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER);
    }

    public void removeTappedLocation(int index) {
        removePoint(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER, index);
    }

    public void removeTypeLocation(String type) {
        MapItemizedOverlay typeOverlay = overlays.get(KIND_OVERLAY_UNIQUE_NAME);
        for (int i = typeOverlay.size() - 1 ; i >= 0 ; i--)
            if (typeOverlay.getItem(i).getSnippet().equals(type))
                typeOverlay.removeOverlay(i);
        mapOverlays.add(typeOverlay);
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (addByTap) {
            int actionType = event.getAction();
            switch (actionType) {
            case MotionEvent.ACTION_UP:
                if (event.getEventTime() - event.getDownTime() > 1500) {
                    GeoPoint p = this.getProjection().fromPixels((int)event.getX(), (int)event.getY());
                    lastPointedLocation = Misc.geoToDeg(p);

                    /* popping up a dialog so the user could confirm his location choice */
                    AlertDialog dialog = new AlertDialog.Builder(context).create();
                    dialog.setIcon(android.R.drawable.ic_dialog_alert);

                    /* setting the dialog title */
                    dialog.setTitle("Confirm Specific Location"); //$NON-NLS-1$

                    /* setting the dialog message. in this case, asking the user if he's sure he */
                    /* wants to add the tapped location to the task, so the task will be specific- */
                    /* location based */


                    try {
                        address = Geocoding.reverseGeocoding(lastPointedLocation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (address != null) {
                        dialog.setMessage("Would you like to add the following location:\n" + address); //$NON-NLS-1$

                        /* setting the confirm button text and action to be executed if it has been chosen */
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", //$NON-NLS-1$
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dg, int which) {
                                /* adding the location to the task */
                                LocationService x = new LocationService();
                                String[] allSpecific = x.getLocationsBySpecificAsArray(currentTastID);
                                LinkedHashSet<String> la = new LinkedHashSet<String>();
                                for (int i = 0 ; i < allSpecific.length ; i++)
                                    la.add(allSpecific[i]);
                                la.add(lastPointedLocation.getX() + "," + lastPointedLocation.getY()); //$NON-NLS-1$

                                //TODO consider remove this line because when this close, all points are saved
                                x.syncLocationsBySpecific(currentTastID, la);
                                addItemToOverlay(Misc.degToGeo(lastPointedLocation), "Specific Location", address, address, UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER); //$NON-NLS-1$
                                refresh();
                            }
                        });
                        /* setting the refuse button text and action to be executed if it has been chosen */
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", //$NON-NLS-1$
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dg, int which) {
                                return;
                            }
                        });
                        dialog.show();
                    }
                    else Toast.makeText(context, "geocoding failed!", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
                }
            }
        }
        boolean b = super.dispatchTouchEvent(event);
        return b;
    }

    public void refresh() {
        this.invalidate();
    }

    public DPoint getPointedCoordinates() {
        return lastPointedLocation;
    }

    public void removePoint(String identifier, int index) {
        if (identifier == null)
            return;
        overlays.get(identifier).removeOverlay(index);
        invalidate();
    }

    public String[] getAllByIDAsAddress(String identifier) {
        MapItemizedOverlay overlay = overlays.get(identifier);
        if (overlay == null)
            return new String[0];
        String[] points = new String[overlay.size()];
        for (int i = 0 ; i < points.length ; i++)
            points[i] = overlay.getItem(i).getAddress();
        return points;
    }

    public DPoint[] getAllByIDAsCoords(String identifier) {
        MapItemizedOverlay overlay = overlays.get(identifier);
        if (overlay == null)
            return new DPoint[0];
        DPoint[] points = new DPoint[overlay.size()];
        for (int i = 0 ; i < points.length ; i++)
            points[i] = Misc.geoToDeg(overlay.getItem(i).getPoint());
        return points;
    }

    public String[] getTappedAddress() {
        return getAllByIDAsAddress(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER);
    }

    public DPoint[] getTappedCoords() {
        return getAllByIDAsCoords(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER);
    }

    public DPoint[] getAllPoints() {
        int count = 0;
        DPoint[] allPoints = new DPoint[getAllPointsCount()];
        for (Map.Entry<String, MapItemizedOverlay> pair : overlays.entrySet()) {
            MapItemizedOverlay overlay = pair.getValue();
            for (int i = 0 ; i < overlay.size() ; i++)
                allPoints[count + i] = Misc.geoToDeg(overlay.getItem(i).getPoint());
            count += overlay.size();
        }
        return allPoints;
    }

    public String[] getAllAddresses() {
        int count = 0;
        String[] AllAddresses = new String[getAllPointsCount()];
        for (Map.Entry<String, MapItemizedOverlay> pair : overlays.entrySet()) {
            MapItemizedOverlay overlay = pair.getValue();
            for (int i = 0 ; i < overlay.size() ; i++)
                AllAddresses[count + i] = overlay.getItem(i).getAddress();
            count += overlay.size();
        }
        return AllAddresses;
    }

    public AdjustedOverlayItem getTappedItem(int index) {
        return overlays.get(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER).createItem(index);
    }

    public class MapItemizedOverlay extends ItemizedOverlay<AdjustedOverlayItem> {
        private final ArrayList<AdjustedOverlayItem> mOverlays = new ArrayList<AdjustedOverlayItem>();

        public MapItemizedOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
            populate();
        }

        @Override
        protected AdjustedOverlayItem createItem(int i) {
            return mOverlays.get(i);
        }

        @Override
        public int size() {
            return mOverlays.size();
        }

        @Override
        protected boolean onTap(int index) {
            AdjustedOverlayItem item = mOverlays.get(index);
            Intent intent = new Intent(ContextManager.getContext(), Focaccia.class);
            String[] sentData = new String[6];
            sentData[0] = index + ""; //$NON-NLS-1$
            sentData[1] = Misc.geoToDeg(item.getPoint()).toString();
            sentData[2] = item.getTitle();
            sentData[3] = item.getSnippet();
            sentData[4] = item.getAddress();
            if (overlays.get(SPECIFIC_OVERLAY_UNIQUE_NAME) == this) {
                sentData[5] = "1"; // can be removed //$NON-NLS-1$
            }
            else if (overlays.get(KIND_OVERLAY_UNIQUE_NAME) == this) {
                sentData[5] = "0"; // can't be removed //$NON-NLS-1$
            }
            else if (overlays.get(PEOPLE_OVERLAY_UNIQUE_NAME) == this) {
                sentData[5] = "1"; // can't be removed //$NON-NLS-1$
            }
            else if (overlays.get(DEVICE_LOCATION_OVERLAY_UNIQUE_NAME) == this) {
                sentData[5] = "0"; // can't be removed //$NON-NLS-1$
            }
            else if (overlays.get(UNIQUE_SPECIFIC_OVERLAY_IDENTIFIER) == this) {
                sentData[5] = "1"; // can be removed //$NON-NLS-1$
            }
            intent.putExtra(Focaccia.SOURCE_ADJUSTEDMAP, sentData);
            ((Activity) AdjustedMap.this.context).startActivityForResult(intent, 1);
            return true;
        }

        public void addOverlay(AdjustedOverlayItem overlay) {
            mOverlays.add(overlay);
            setLastFocusedIndex(-1);
            populate();
        }

        public void removeOverlay(int index) {
            //TODO check if index is not out of borders
            mOverlays.remove(createItem(index));
            //            mOverlays.clear();
            setLastFocusedIndex(-1);
            populate();
        }

    }

}
