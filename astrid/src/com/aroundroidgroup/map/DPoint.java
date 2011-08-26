package com.aroundroidgroup.map;

import java.text.DecimalFormat;


public class DPoint{

    private final double x;
    private final double y;

    public DPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public DPoint(DPoint d) {
        this.x = d.x;
        this.y = d.y;
    }

    public DPoint(String str) {
        int delimiterIndex = str.indexOf(',');
        if (delimiterIndex == -1) {
            this.x = Double.NaN;
            this.y = Double.NaN;
            return;
        }
        double tmpX = Double.parseDouble(str.substring(0, delimiterIndex));
        double tmpY = Double.parseDouble(str.substring(delimiterIndex + 1));
        if (Double.isNaN(tmpX) || Double.isNaN(tmpY)) {
            this.x = Double.NaN;
            this.y = Double.NaN;
            return;
        }
        this.x = tmpX;
        this.y = tmpY;
    }

    //X IS LAT
    public double getX() {
        return x;
    }

    //Y IS LON
    public double getY() {
        return y;
    }

    public boolean isNaN() {
        return Double.isNaN(x);
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("##.######"); //$NON-NLS-1$
        return df.format(x) + "," + df.format(y); //$NON-NLS-1$
    }

    @Override
    public boolean equals(Object obj) {
        DPoint d = null;
        try {
            d = (DPoint)obj;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (d == null)
            return false;
        return (this.getX() == d.getX() && this.getY() == d.getY());
    }
}
