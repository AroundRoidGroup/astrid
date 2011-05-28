package com.aroundroidgroup.map;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class AdjustedOverlayItem extends OverlayItem {

    private String locationAddress = null;

    public AdjustedOverlayItem(GeoPoint point, String title, String snippet) {
        super(point, title, snippet);
        // TODO Auto-generated constructor stub
    }

    public AdjustedOverlayItem(GeoPoint point, String title, String snippet, String address) {
        super(point, title, snippet);
        locationAddress = address;
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

}
