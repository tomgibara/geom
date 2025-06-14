package com.tomgibara.geom.helper;

import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Point;

public class Measurer implements Point.Consumer<Measurer> {

    private final Norm norm;
    private double length = 0.0;
    private Point last = null;

    public Measurer(Norm norm) {
        if (norm == null) throw new IllegalArgumentException("null norm");
        this.norm = norm;
    }

    public Measurer() {
        this.norm = Norm.L2;
    }

    public double getLength() {
        return length;
    }

    @Override
    public Measurer addPoint(double x, double y) {
        return addPoint(new Point(x, y));
    }

    @Override
    public Measurer addPoint(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        if (last != null) length += norm.distanceBetween(last, pt);
        last = pt;
        return this;
    }

}
