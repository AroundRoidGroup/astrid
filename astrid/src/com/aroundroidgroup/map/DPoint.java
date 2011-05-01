package com.aroundroidgroup.map;

import android.os.Parcel;
import android.os.Parcelable;

public class DPoint implements Parcelable{

	private final double x;
	private final double y;

	public DPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        // TODO Auto-generated method stub

    }
}
