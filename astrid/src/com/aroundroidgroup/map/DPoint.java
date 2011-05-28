package com.aroundroidgroup.map;


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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isNaN() {
        return Double.isNaN(x);
    }

    @Override
    public String toString() {
        return x + "," + y; //$NON-NLS-1$
    }
}
