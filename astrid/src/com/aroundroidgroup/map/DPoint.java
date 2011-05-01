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
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        double[] dblArray = new double[2];
        dblArray[0] = x;
        dblArray[1] = y;
        arg0.writeDoubleArray(dblArray);
    }
    public static final Parcelable.Creator<DPoint> CREATOR
    = new Parcelable.Creator<DPoint>() {
        public DPoint createFromParcel(Parcel in) {
            return new DPoint(in);
        }

        public DPoint[] newArray(int size) {
            return new DPoint[size];
        }
    };

    private DPoint(Parcel in) {
        double[] darr = new double[2];
        in.readDoubleArray(darr);
        x = darr[0];
        y = darr[1];
    }
}
