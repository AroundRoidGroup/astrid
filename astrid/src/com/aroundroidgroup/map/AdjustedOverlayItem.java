package com.aroundroidgroup.map;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class AdjustedOverlayItem extends OverlayItem {

    private String locationAddress = null;
    private String extras = null;
    private long taskID;

    public AdjustedOverlayItem(GeoPoint point, String title, String snippet) {
        super(point, title, snippet);
        // TODO Auto-generated constructor stub
    }

    public AdjustedOverlayItem(GeoPoint point, String title, String snippet, String address, long taskID, String extras) {
        super(point, title, snippet);
        locationAddress = address;
        this.taskID = taskID;
        this.extras = extras;
    }

    public void setLocationAddress(String address) {
        if (address != null)
            locationAddress = new String(address);
    }

    public String getAddress() {
        return locationAddress;
    }

    public boolean hasAddress() {
        return locationAddress != null;
    }

    public long getTaskID() {
        return taskID;
    }

    public void setTaskID(long taskID) {
        this.taskID = taskID;
    }

    public String getExtras() {
        return extras;
    }

    public void setExtras(String extars) {
        this.extras = extars;
    }

}
