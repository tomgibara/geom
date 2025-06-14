package com.tomgibara.geom.helper;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;

public final class Bounder implements Point.Consumer<Bounder> {

    private double minX = Double.POSITIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    @Override
    public Bounder addPoint(double x, double y) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        return this;
    }

    @Override
    public Bounder addPoint(Point pt) {
        addPoint(pt.x, pt.y);
        return this;
    }

    public Rect getBounds() {
        if (maxX < minX) throw new IllegalStateException("no points added");
        //TODO shame we can't use direct constructor
        return Rect.atPoints(minX, minY, maxX, maxY);
    }
}