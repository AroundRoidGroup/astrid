package com.aroundroidgroup.map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;
import com.todoroo.astrid.service.TaskService;

public class AdjustedMap extends MapView {

    public static int equatorLen = 40075016;

    Context mContext = null;
    private DPoint lastPointedLocation = null;
    private GeoPoint lastCenter = null;
    private long currentTaskID = -1;
    private Map<Integer, MapItemizedOverlay> overlays;
    private Map<Integer, Boolean> mDoubleItem;
    private Map<MapItemizedOverlay, String[]> mConfigurations;
    private Map<MapItemizedOverlay, String> mNames;
    private List<Overlay> mapOverlays;
    private MapItemizedOverlay mTappedOverlay;
    private MapItemizedOverlay mDeviceOverlay;

    private MapItemizedOverlay lastPressedOverlay = null;
    private int lastPressedIndex = -1;
    private GeoPoint lastDeviceLocation;


    public static final String TASK_NAME = "Task_Name_For_POPUP_HEADER"; //$NON-NLS-1$

    public static final String AM_TAPPED_LOCATION = "tap"; //$NON-NLS-1$
    public static final String AM_KIND_LOCATION = "kind"; //$NON-NLS-1$
    public static final String AM_PEOPLE_LOCATION = "people"; //$NON-NLS-1$
    public static final String AM_DEVICE_LOCATION = "device"; //$NON-NLS-1$

    public static final int AM_REQUEST_CODE = 1000;

    /* identifiers for content to be shown when touching the overlay */
    public static final String OVERLAY_DEVICE = "oDevice"; //$NON-NLS-1$
    public static final String OVERLAY_TAP = "oTap"; //$NON-NLS-1$

    private AroundroidDbAdapter db;
    private LocationsDbAdapter locDB;

    private boolean editable = true;

    public AdjustedMap(Context context, String apiKey) {
        super(context, apiKey);
        mContext = context;
        init();
    }

    public AdjustedMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public AdjustedMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    /**
     *
     * @return true if the map object contains any kind of overlay items except device location if enabled by showDeviceLocation().
     * otherwise, returns false.
     */
    public boolean hasPlaces() {
        if (getTappedPointsCount() > 0)
            return true;
        for (Map.Entry<Integer, MapItemizedOverlay> p : overlays.entrySet())
            if (p.getValue().size() > 0)
                return true;
        return false;
    }

    /**
     * associating an ID of a task to the map, so all the items that will be added to the map and does not specify
     * an ID of a task, will associate with this ID. if map is already associated with a task, the new task's ID
     * will be the current associated task.
     *
     * @param taskID the ID of the task to associate with.
     * @return true if map yet no associated with task ID. returns false if the map is already associated.
     */
    public boolean associateMapWithTask(long taskID) {
        if (currentTaskID == -1) {
            currentTaskID = taskID;
            return true;
        }
        currentTaskID = taskID;
        return false;
    }

    /**
     * Enables the option to edit (add or remove) locations from the map.
     * To cancel this option, a call to makeUneditable() has to be made.
     */
    public void makeEditable() {
        editable = true;
    }

    /**
     * Disables the option to edit (add or remove) locations from the map.
     * To allow this option, a call to makeEditable() has to be made.
     */
    public void makeUneditable() {
        editable = false;
    }

    /**
     * tells if an overlay exists
     *
     * @param id the id of the overlay in which it has been registered with createOverlay()
     * @return true if an overlay with this id is exists. otherwise, false is returned.
     */
    public boolean hasOverlayWithID(int id) {
        return overlays.get(id) != null;
    }

    /**
     * shows the location of the device on the map in a special overlay
     */
    public void showDeviceLocation() {
        Resources r = getResources();
        if (mDeviceOverlay == null) {
            DPoint deviceLocation = getDeviceLocation();
            if (deviceLocation != null) {
                mDeviceOverlay = new MapItemizedOverlay(getResources().getDrawable(R.drawable.device_location));
                mConfigurations.put(mDeviceOverlay, new String[] { Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS });
                mNames.put(mDeviceOverlay, r.getString(R.string.AD_your_location_title));
                DPoint lastDeviceLocationAsDPoint = Misc.geoToDeg(lastDeviceLocation);
                String savedAddr = locDB.fetchByCoordinateAsString(lastDeviceLocation.getLatitudeE6(), lastDeviceLocation.getLongitudeE6());
                if (savedAddr == null) {
                    try {
                        savedAddr = Geocoding.reverseGeocoding(lastDeviceLocationAsDPoint);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (savedAddr == null)
                        savedAddr = LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE;
                    locDB.createTranslate(lastDeviceLocation.getLatitudeE6(), lastDeviceLocation.getLongitudeE6(), savedAddr);
                    if (savedAddr.equals(LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE))
                        savedAddr = lastDeviceLocationAsDPoint.toString();
                }
                mDeviceOverlay.addOverlay(new AdjustedOverlayItem(lastDeviceLocation, r.getString(R.string.AD_your_location_title), null, savedAddr, -1, null, -1));
                mapOverlays.add(mDeviceOverlay);
            }
        }
    }

    /**
     * gives the location of the device
     * @return location of the device if exists, otherwise returns null.
     */
    public DPoint getDeviceLocation() {
        FriendProps fp = db.specialUserToFP();
        if (fp != null && fp.isValid()) {
            DPoint d = new DPoint(fp.getDlat(), fp.getDlon());
            lastDeviceLocation = Misc.degToGeo(d);
            return d;
        }
        else
            return null;
    }

    /**
     * updates the location of the device if it has a new location from the last call of this function.
     */
    public void updateDeviceLocation() {
        Resources r = getResources();
        if (mDeviceOverlay == null)
            return;
        DPoint potentialNewLocation = getDeviceLocation();
        if (potentialNewLocation != null && !mDeviceOverlay.getItem(0).getPoint().equals(lastDeviceLocation)) {
            mDeviceOverlay.clear();
            String savedAddr = locDB.fetchByCoordinateAsString(lastDeviceLocation.getLatitudeE6(), lastDeviceLocation.getLongitudeE6());
            if (savedAddr == null) {
                try {
                    savedAddr = Geocoding.reverseGeocoding(Misc.geoToDeg(lastDeviceLocation));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (savedAddr == null)
                    savedAddr = LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE;
                locDB.createTranslate(lastDeviceLocation.getLatitudeE6(), lastDeviceLocation.getLongitudeE6(), savedAddr);
                if (savedAddr == LocationsDbAdapter.DATABASE_COORDINATE_GEOCODE_FAILURE)
                    savedAddr = Misc.geoToDeg(lastDeviceLocation).toString();
            }
            mDeviceOverlay.addOverlay(new AdjustedOverlayItem(Misc.degToGeo(potentialNewLocation), r.getString(R.string.AD_your_location_title), null, savedAddr, currentTaskID, null, -1));
            invalidate();
        }

    }

    /**
     * disable the view of the device overlay.
     */
    public void removeDeviceLocation() {
        if (mDeviceOverlay != null) {
            mapOverlays.remove(mDeviceOverlay);
            overlays.remove(mDeviceOverlay);
            mDeviceOverlay = null;
        }
    }

    /**
     * tells if an overlay has a configuration.
     * @param uniqueName the ID of the overlay.
     * @param configuaration the configuration to check for existence.
     * @return true if the overlay contains this configuration. otherwise returns false.
     */
    public boolean hasConfig(int uniqueName, String configuaration) {
        MapItemizedOverlay overlay = overlays.get(uniqueName);
        if (overlay == null || configuaration == null)
            return false;
        String[] configs = mConfigurations.get(overlay);
        for (String s : configs)
            if (s.equals(configuaration))
                return true;
        return false;
    }

    /**
     * creates an overlay
     * @param doubles true if multiple items with the same coordinates are allowed to be added.
     * @param uniqueName a unique number that will identify the overlay.
     * @param d a drawable that will be painted in the coordinates of the overlay's items
     * @param config configurations which tell what to show in the Focaccia activity
     * @param name name of the overlay
     * @return true if the overlay was created successfully, otherwise false.
     */
    public boolean createOverlay(boolean doubles, int uniqueName, Drawable d, String[] config, String name) {
        if (d == null)
            return false;
        if (overlays.get(uniqueName) == null) {
            MapItemizedOverlay overlay = new MapItemizedOverlay(d);
            mDoubleItem.put(uniqueName, doubles);
            overlays.put(uniqueName, overlay);
            mConfigurations.put(overlay, config);
            mNames.put(overlay, name);
            return true;
        }
        return false;
    }
    /**
     * tells if an overlay contains item with the given snippet property
     * @param id the ID of the overlay
     * @param snippet the snippet to be matched.
     * @return true if such an item exists, otherwise false.
     */
    public boolean isContains(int id, String snippet) {
        MapItemizedOverlay typeOverlay = overlays.get(id);
        for (int i = typeOverlay.size() - 1 ; i >= 0 ; i--)
            if (typeOverlay.getItem(i).getSnippet().equals(snippet))
                return true;
        return false;
    }

    /**
     * tells if an overlay contains item with the given coordinates
     * @param id the ID of the overlay
     * @param coord the coordinates to be matched
     * @return true if such an item exists, otherwise false.
     */
    public boolean isContainsByCoords(int id, GeoPoint coord) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay != null && coord != null) {
            for (AdjustedOverlayItem item : overlay)
                if (item.getPoint().equals(coord))
                    return true;
            return false;
        }
        return false;
    }

    /**
     * get an overlay.
     * @param id the ID of the overlay.
     * @return the overlay object with the given id, otherwise null.
     */
    public MapItemizedOverlay getOverlay(int id) {
        return overlays.get(id);
    }

    /**
     * adds an item to an overlay.
     * @param g the coordinates of the item.
     * @param title the title of the item.
     * @param snippet the snippet of the item.
     * @param addr the address of the item.
     * @param identifier the ID of the overlay that the item will be added to.
     * @param taskID the ID of the task to associate this item to.
     * @param extras some extra data to add to the item.
     * @return true if the item has been added successfully, otherwise false.
     */
    public boolean addItemToOverlay(GeoPoint g, String title, String snippet, String addr, int identifier, long taskID, String extras) {
        currentTaskID = taskID;
        MapItemizedOverlay overlay = overlays.get(identifier);
        if (overlay != null && g != null && title != null && snippet != null) {
            if (!(overlay.getIndexOf(g) != -1 && !mDoubleItem.get(identifier))) {
                overlay.addOverlay(new AdjustedOverlayItem(g, title, snippet, addr, taskID, extras, -1));
                mapOverlays.add(overlay);
                invalidate();
                return true;
            }
        }
        return false;
    }

    /**
     * updates an item with new details.
     * @param oldG the old coordinates of the item.
     * @param newG the new coordinates of the item.
     * @param newAddr the new address for the item.
     * @param identifier the ID of the overlay that the item is belong to.
     * @return true if the update succeeded, otherwise returns false.
     */
    public boolean updateItemInOverlay(GeoPoint oldG, GeoPoint newG, String newAddr, int identifier) {
        MapItemizedOverlay overlay = overlays.get(identifier);
        if (overlay != null && oldG != null && newG != null && newAddr != null) {
            if (isContainsByCoords(identifier, oldG)) {
                AdjustedOverlayItem item = overlay.getItem(overlay.getIndexOf(oldG));
                overlay.addOverlay(new AdjustedOverlayItem(newG, item.getTitle(), item.getSnippet(),
                        newAddr, item.getTaskID(), item.getExtras(), -1));
                return removeItemFromOverlayByCoords(identifier, oldG);
            }
        }
        return false;
    }

    /**
     * the number of locations in the map.
     * @return returns the number of locations in the map.
     */
    public int getAllPointsCount() {
        int count = 0;
        for (Map.Entry<Integer, MapItemizedOverlay> pair : overlays.entrySet())
            count += pair.getValue().size();
        return count;
    }

    /**
     * the number of the locations that has been added by longPress on the map.
     * @return the number of the locations that has been added by longPress on the map.
     */
    public int getTappedPointsCount() {
        if (mTappedOverlay != null)
            return mTappedOverlay.size();
        return 0;
    }

    /**
     * the number of the items in the overlay with the given ID
     * @param identifier
     * @return
     */
    public int getOverlaySize(int identifier) {
        MapItemizedOverlay iOver = overlays.get(identifier);
        if (iOver == null)
            return -1;
        return iOver.size();
    }

    public void setZoomByAllLocations() {
        int minLat = Integer.MAX_VALUE;
        int maxLat = Integer.MIN_VALUE;
        int minLon = Integer.MAX_VALUE;
        int maxLon = Integer.MIN_VALUE;

        List<GeoPoint> allItemsLocations = getAllLocations();
        for (GeoPoint item : allItemsLocations)
        {

            int lat = item.getLatitudeE6();
            int lon = item.getLongitudeE6();

            maxLat = Math.max(lat, maxLat);
            minLat = Math.min(lat, minLat);
            maxLon = Math.max(lon, maxLon);
            minLon = Math.min(lon, minLon);
        }
        if (!allItemsLocations.isEmpty()) {
            getController().zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
            getController().animateTo(new GeoPoint( (maxLat + minLat)/2,
                    (maxLon + minLon)/2 ));
        }
    }

    private List<GeoPoint> getAllLocations() {
        List<GeoPoint> lst = new ArrayList<GeoPoint>();
        for (MapItemizedOverlay pair : overlays.values()) {
            for (AdjustedOverlayItem item : pair) {
                lst.add(item.getPoint());
            }
        }
        if (mTappedOverlay != null)
            for (AdjustedOverlayItem item : mTappedOverlay)
                lst.add(item.getPoint());
        if (mDeviceOverlay != null)
            for (AdjustedOverlayItem item : mDeviceOverlay)
                lst.add(item.getPoint());
        return lst;
    }

    public GeoPoint getPointWithMinimalDistanceFromDeviceLocation(int id, String extras) {
        if (mDeviceOverlay == null)
            return null;
        return getPointWithMinimalDistanceFromGivenPoint(id, extras, getDeviceLocation());
    }

    public GeoPoint getPointWithMinimalDistanceFromGivenPoint(int id, String extras, DPoint point) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay == null)
            return null;
        double delta = Double.MAX_VALUE;
        GeoPoint minimalItem = null;
        for (AdjustedOverlayItem item : overlay) {
            if ((item.getExtras().equals(extras)) &&(delta > Misc.distance(getDeviceLocation(), Misc.geoToDeg(item.getPoint())))) {
                delta = Misc.distance(point, Misc.geoToDeg(item.getPoint()));
                minimalItem = item.getPoint();
            }
        }
        return minimalItem;
    }

    private void init() {
        db = new AroundroidDbAdapter(mContext);
        db.open();
        locDB = new LocationsDbAdapter(mContext);
        locDB.open();
        getOverlays().add(new MapGestureDetectorOverlay(new MapOnGestureListener()));
        overlays = new HashMap<Integer, MapItemizedOverlay>();
        mDoubleItem = new HashMap<Integer, Boolean>();
        mConfigurations = new HashMap<MapItemizedOverlay, String[]>();
        mNames = new HashMap<MapItemizedOverlay, String>();
        mapOverlays = getOverlays();
        showDeviceLocation();
        getController().setZoom(18);
        lastCenter = getMapCenter();
    }

    /* calling this function will automatically add an overlay for specific locations */
    public void enableAddByTap() {
        Resources r = getResources();
        mTappedOverlay = new MapItemizedOverlay(this.getResources().getDrawable(R.drawable.icon_tap));
        mapOverlays.add(mTappedOverlay);
        overlays.put(0, mTappedOverlay);
        mConfigurations.put(mTappedOverlay, new String[] { Focaccia.SHOW_NAME, Focaccia.SHOW_ADDRESS });
        mNames.put(mTappedOverlay, r.getString(R.string.AD_kind_specific));
    }

    public void diableAddByTap() {
        mapOverlays.remove(mTappedOverlay);
        mTappedOverlay = null;
    }

    private void addTappedLocation(GeoPoint g, String addr, long taskID) {
        Resources r = getResources();
        if (mTappedOverlay != null) {
            mTappedOverlay.addOverlay(new AdjustedOverlayItem(g, r.getString(R.string.AD_kind_specific), addr, addr, taskID, null, -1));
            mapOverlays.add(mTappedOverlay);
        }
    }

    public boolean clearOverlay(int id) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay == null)
            return false;
        overlay.clear();
        return true;
    }

    public boolean removeTapItem(int index) {
        if (mTappedOverlay == null)
            return false;
        mTappedOverlay.removeOverlayByIndex(index);
        invalidate();
        return true;
    }

    public boolean removeItemFromOverlayByCoords(int id, GeoPoint coords) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay != null && coords != null)
            for (AdjustedOverlayItem item : overlay)
                if (item.getPoint().equals(coords))
                    return removeItemFromOverlay(id, overlay.getIndexOf(coords));
        return false;
    }

    public boolean removeItemFromOverlay(int id, int index) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay != null) {
            overlay.removeOverlayByIndex(index);
            mapOverlays.add(overlay);
            invalidate();
            return true;
        }
        return false;
    }

    public int getItemsByExtrasCount(int id, String extras) {
        MapItemizedOverlay overlay = overlays.get(id);
        int counter = 0;
        if (overlay != null) {
            for (AdjustedOverlayItem item : overlay) {
                counter += (item.getExtras().equals(extras)) ? 1 : 0;
            }
        }
        return counter;
    }

    public List<AdjustedOverlayItem> selectItemFromOverlayByExtrasAsAjustedItem(int id, String extras) {
        MapItemizedOverlay overlay = overlays.get(id);
        List<AdjustedOverlayItem> xtraLst = new ArrayList<AdjustedOverlayItem>();
        if (overlay != null) {
            for (AdjustedOverlayItem item : overlay) {
                if (item.getExtras() != null && item.getExtras().equals(extras))
                    xtraLst.add(item);
            }
        }
        return xtraLst;
    }

    public List<String> selectItemFromOverlayByExtras(int id, String extras) {
        MapItemizedOverlay overlay = overlays.get(id);
        List<String> xtraLst = new ArrayList<String>();
        if (overlay != null) {
            for (AdjustedOverlayItem item : overlay) {
                if (item.getExtras() != null && item.getExtras().equals(extras))
                    xtraLst.add(Misc.geoToDeg(item.getPoint()).toString());
            }
        }
        return xtraLst;
    }

    public double getMapRadius() {
        int lon = getLongitudeSpan();
        int lat = getLatitudeSpan();
        float[] hight = new float[1], width = new float[1];
        if (lat==0 || lon==0){ // the function is called from onCreate
            setZoomByAllLocations();
        }else{
            Location.distanceBetween(0, 0, 0, ((double)lon)/1000000, hight);
            Location.distanceBetween(0, 0, ((double)lat)/1000000, 0, width);
        }
        return Math.min(hight[0], width[0])/2;
    }

    public int removeItemFromOverlayByExtras(int id, String extras) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay != null) {
            ListIterator<AdjustedOverlayItem> iterator = overlay.iterator();
            while (iterator.hasNext()) {
                AdjustedOverlayItem item = iterator.next();
                if (item.getExtras() != null && item.getExtras().equals(extras)) {
                    iterator.remove();
                }
            }
        }
        invalidate();
        return getItemsByExtrasCount(id, extras);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        int actionType = event.getAction();
        switch (actionType) {
        case MotionEvent.ACTION_UP:
            GeoPoint pdf = getMapCenter();
            if (!pdf.equals(lastCenter)) {
                /* map center changed */

                fireEvent();
                lastCenter = getMapCenter();
            }
        }

        return super.dispatchTouchEvent(event);
    }

    public void refresh() {
        this.invalidate();
    }

    public DPoint getPointedCoordinates() {
        return lastPointedLocation;
    }

    public void removePoint(int identifier, int index) {
        MapItemizedOverlay overlay = overlays.get(identifier);
        if (overlay == null)
            return;
        overlay.removeOverlayByIndex(index);
        invalidate();
    }

    private void removeTappedPoint(int index) {
        mTappedOverlay.removeOverlayByIndex(index);
        invalidate();
    }

    public AdjustedOverlayItem getItemByExtras(int id, String extras) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay != null && extras != null) {
            int index = overlay.getIndexOf(extras);
            if (index != -1)
                return overlay.getItem(index);
        }
        return null;
    }

    public AdjustedOverlayItem selectLastPressedItem() {
        if (lastPressedOverlay == mDeviceOverlay)
            return null;
        if (lastPressedOverlay == mTappedOverlay) {
            AdjustedOverlayItem removedCopy = mTappedOverlay.createItem(lastPressedIndex);
            return removedCopy;
        }
        for (Map.Entry<Integer, MapItemizedOverlay> pair : overlays.entrySet())
            if (pair.getValue() == lastPressedOverlay) {
                AdjustedOverlayItem removedCopy = pair.getValue().createItem(lastPressedIndex);
                return removedCopy;
            }
        return null;
    }

    public AdjustedOverlayItem removeLastPressedItem() {
        if (lastPressedOverlay == mDeviceOverlay)
            return null;
        if (lastPressedOverlay == mTappedOverlay) {
            AdjustedOverlayItem removedCopy = mTappedOverlay.createItem(lastPressedIndex);
            removeTappedPoint(lastPressedIndex);
            return removedCopy;
        }
        for (Map.Entry<Integer, MapItemizedOverlay> pair : overlays.entrySet())
            if (pair.getValue() == lastPressedOverlay) {
                AdjustedOverlayItem removedCopy = pair.getValue().createItem(lastPressedIndex);
                removePoint(pair.getKey(), lastPressedIndex);
                return removedCopy;
            }
        invalidate();
        return null;
    }

    public String[] getAllByIDAsAddress(int id) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay == null)
            return new String[0];
        String[] points = new String[overlay.size()];
        for (int i = 0 ; i < points.length ; i++)
            points[i] = overlay.getItem(i).getAddress();
        return points;
    }

    public DPoint[] getAllByIDAsCoords(int id) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay == null)
            return new DPoint[0];
        DPoint[] points = new DPoint[overlay.size()];
        for (int i = 0 ; i < points.length ; i++)
            points[i] = Misc.geoToDeg(overlay.getItem(i).getPoint());
        return points;
    }

    public String[] getTappedAddress() {
        if (mTappedOverlay == null)
            return new String[0];
        String[] tapAddr = new String[mTappedOverlay.size()];
        for (int i = 0 ; i < tapAddr.length ; i++)
            tapAddr[i] = mTappedOverlay.getItem(i).getAddress();
        return tapAddr;
    }

    public DPoint[] getTappedCoords() {
        if (mTappedOverlay == null)
            return new DPoint[0];
        DPoint[] tapAddr = new DPoint[mTappedOverlay.size()];
        for (int i = 0 ; i < tapAddr.length ; i++)
            tapAddr[i] = Misc.geoToDeg(mTappedOverlay.getItem(i).getPoint());
        return tapAddr;
    }

    public DPoint[] getAllPoints() {
        int count = 0;
        DPoint[] allPoints = new DPoint[getAllPointsCount()];
        for (Map.Entry<Integer, MapItemizedOverlay> pair : overlays.entrySet()) {
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
        for (Map.Entry<Integer, MapItemizedOverlay> pair : overlays.entrySet()) {
            MapItemizedOverlay overlay = pair.getValue();
            for (int i = 0 ; i < overlay.size() ; i++)
                AllAddresses[count + i] = overlay.getItem(i).getAddress();
            count += overlay.size();
        }
        return AllAddresses;
    }

    public AdjustedOverlayItem getTappedItem(int index) {
        return mTappedOverlay.createItem(index);
    }

    public int getItemID(int id, DPoint coords) {
        MapItemizedOverlay overlay = overlays.get(id);
        if (overlay == null || coords == null)
            return -1;
        return overlay.getIndexOf(Misc.degToGeo(coords));
    }

    public class MapItemizedOverlay extends ItemizedOverlay<AdjustedOverlayItem> implements java.lang.Iterable<AdjustedOverlayItem> {
        private final ArrayList<AdjustedOverlayItem> mOverlays = new ArrayList<AdjustedOverlayItem>();

        public MapItemizedOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
            populate();
        }

        @Override
        protected AdjustedOverlayItem createItem(int i) {
            if (i < 0 || i >= mOverlays.size())
                return null;
            AdjustedOverlayItem item = mOverlays.get(i);
            item.setUniqueID(i);
            return item;
        }

        @Override
        public int size() {
            return mOverlays.size();
        }

        public void clear() {
            mOverlays.clear();
        }

        public int getIndexOf(GeoPoint g) {
            for (AdjustedOverlayItem item : mOverlays)
                if (item.getPoint().equals(g))
                    return mOverlays.indexOf(item);
            return -1;
        }

        public int getIndexOf(String extras) {
            for (AdjustedOverlayItem item : mOverlays)
                if (item.getExtras().equals(extras))
                    return mOverlays.indexOf(item);
            return -1;
        }

        @Override
        protected boolean onTap(int index) {
            Resources r = getResources();
            lastPressedOverlay = this;
            lastPressedIndex = index;
            AdjustedOverlayItem item = mOverlays.get(index);
            Intent intent = new Intent(ContextManager.getContext(), Focaccia.class);

            if (item.getTaskID() != mapFunctions.MULTIPLE_TASKS_ID) {

                /* getting task's title by its ID */
                TaskService taskService = new TaskService();
                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE).where(Criterion.and(TaskCriteria.isActive(),Criterion.and(TaskCriteria.byId(item.getTaskID()),
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
            }
            else {
                intent.putExtra(Focaccia.TASK_NAME, r.getString(R.string.AD_multiple_tasks));
            }
            String addr = null;
            if (item.getAddress() == null)
                addr = Misc.geoToDeg(item.getPoint()).toString();
            else addr = item.getAddress();
            if (mTappedOverlay == this) {
                if (editable)
                    intent.putExtra(Focaccia.DELETE, Focaccia.DELETE);
                intent.putExtra(Focaccia.SHOW_ADDRESS, addr);
                intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.AD_kind_specific));
            }
            else if (mDeviceOverlay == this) {
                intent.putExtra(Focaccia.READ_ONLY, Focaccia.READ_ONLY);
                intent.putExtra(Focaccia.SHOW_ADDRESS, addr);
                intent.putExtra(Focaccia.SHOW_NAME, r.getString(R.string.AD_your_location_title));
                intent.putExtra(Focaccia.TASK_NAME, r.getString(R.string.AD_your_location_title));
            }
            else {
                if (editable)
                    intent.putExtra(Focaccia.DELETE, Focaccia.DELETE);
                MapItemizedOverlay currentOverlay = null;
                for (Map.Entry<Integer, MapItemizedOverlay> pair : overlays.entrySet()) {
                    if (pair.getValue() == this) {
                        currentOverlay = pair.getValue();
                        break;
                    }
                }
                if (currentOverlay != null) {
                    String[] configs = mConfigurations.get(currentOverlay);
                    if (configs == null)
                        return true;
                    for (String cfg : configs) {
                        if (cfg.equals(Focaccia.SHOW_ADDRESS)) {
                            intent.putExtra(Focaccia.SHOW_ADDRESS, addr);
                            continue;
                        }
                        if (cfg.equals(Focaccia.SHOW_AMOUNT_BY_EXTRAS)) {
                            int counter = 0;
                            for (int i = 0 ; i < this.size() ; i++)
                                counter += (this.getItem(i).getExtras().equals(item.getExtras())) ? 1 : 0;
                            intent.putExtra(Focaccia.SHOW_AMOUNT_BY_EXTRAS, counter);
                            continue;
                        }
                        if (cfg.equals(Focaccia.SHOW_NAME)) {
                            intent.putExtra(Focaccia.SHOW_NAME, mNames.get(this));
                            continue;
                        }
                        if (cfg.equals(Focaccia.SHOW_SNIPPET)) {
                            intent.putExtra(Focaccia.SHOW_SNIPPET, item.getSnippet());
                            continue;
                        }
                        if (cfg.equals(Focaccia.SHOW_TITLE))
                            intent.putExtra(Focaccia.SHOW_TITLE, item.getTitle());
                    }
                }
            }
            ((Activity)mContext).startActivityForResult(intent, AM_REQUEST_CODE);
            return true;
        }

        public void addOverlay(AdjustedOverlayItem overlay) {
            if (overlay.getPoint() == null)
                return;
            mOverlays.add(overlay);
            overlay.setUniqueID(mOverlays.indexOf(overlay));
            setLastFocusedIndex(-1);
            populate();
        }

        public void removeOverlayByItem(AdjustedOverlayItem item) {
            mOverlays.remove(item);
            setLastFocusedIndex(-1);
            populate();
        }

        public void removeOverlayByIndex(int index) {
            mOverlays.remove(createItem(index));
            setLastFocusedIndex(-1);
            populate();
        }

        @Override
        public ListIterator<AdjustedOverlayItem> iterator() {
            return mOverlays.listIterator();
        }

    }
    private final List<MyEventClassListener> _listeners = new ArrayList<MyEventClassListener>();
    public synchronized void addEventListener(MyEventClassListener listener)  {
        _listeners.add(listener);
    }
    public synchronized void removeEventListener(MyEventClassListener listener)   {
        _listeners.remove(listener);
    }
    // call this method whenever you want to notify
    //the event listeners of the particular event
    private synchronized void fireEvent() {
        MyEventClass event = new MyEventClass(this);
        Iterator<MyEventClassListener> i = _listeners.iterator();
        while(i.hasNext())  {
            i.next().handleMyEventClassEvent(event);
        }
    }

    public class MapOnGestureListener implements OnGestureListener {

        @Override
        public boolean onDown(MotionEvent arg0) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            Resources r = getResources();
            if (mTappedOverlay != null) {

                GeoPoint p = AdjustedMap.this.getProjection().fromPixels((int)event.getX(), (int)event.getY());
                lastPointedLocation = Misc.geoToDeg(p);

                /* popping up a dialog so the user could confirm his location choice */
                AlertDialog dialog = new AlertDialog.Builder(mContext).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);

                /* setting the dialog title */
                dialog.setTitle("Confirm Specific Location"); //$NON-NLS-1$

                /* setting the dialog message. in this case, asking the user if he's sure he */
                /* wants to add the tapped location to the task, so the task will be specific- */
                /* location based */


                final String savedAddr = mapFunctions.getSavedAddressAndUpdate(p.getLatitudeE6(), p.getLongitudeE6());

                dialog.setMessage("Would you like to add the following location:"+"\n" + savedAddr); //$NON-NLS-1$ //$NON-NLS-2$

                /* setting the confirm button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, r.getString(R.string.AD_DLG_ok),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        /* adding the location to the task */
                        addTappedLocation(Misc.degToGeo(lastPointedLocation), savedAddr, currentTaskID);
                        refresh();
                    }
                });
                /* setting the refuse button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, r.getString(R.string.DLG_no),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        return;
                    }
                });
                dialog.show();
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            //
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

    }



}

