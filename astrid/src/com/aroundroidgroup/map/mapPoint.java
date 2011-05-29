package com.aroundroidgroup.map;

public class mapPoint {
    private final DPoint coordinates;
    private String address;

    public mapPoint(DPoint d) {
        coordinates = new DPoint(d);
        address = null;
    }

    public boolean hasAddress() {
        return address != null;
    }

    public void setAddress(String address) {
        this.address = new String(address);
    }

    public DPoint getCoordinates() {
        return coordinates;
    }

    public String getString() {
        return address;
    }
}
