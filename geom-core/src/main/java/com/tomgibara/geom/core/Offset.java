package com.tomgibara.geom.core;

import com.tomgibara.geom.transform.Transform;

public final class Offset {

    public static final Offset IDENTITY = new Offset();

    public static Offset offset(double toMinX, double toMaxX, double toMinY, double toMaxY) {
        return toMinX == 0.0 && toMaxX == 0.0 && toMinY == 0.0 && toMaxY == 0.0 ?
                IDENTITY : new Offset(toMinX, toMaxX, toMinY, toMaxY);
    }

    public static Offset translation(double x, double y) {
        return x == 0.0 && y == 0.0 ?
                IDENTITY : new Offset(x, y);
    }

    public static Offset uniform(double offset) {
        return offset == 0.0 ?
                IDENTITY: new Offset(offset);
    }

    public static Offset symmetric(double offsetX, double offsetY) {
        return offsetX == 0.0 && offsetY == 0.0 ?
                IDENTITY : new Offset(offsetX, -offsetX, offsetY, -offsetY);
    }

    public final double toMinX;
    public final double toMaxX;
    public final double toMinY;
    public final double toMaxY;

    private Offset() {
        toMinX = 0.0;
        toMaxX = 0.0;
        toMinY = 0.0;
        toMaxY = 0.0;
    }

    private Offset(double offset) {
        this.toMinX =  offset;
        this.toMaxX = -offset;
        this.toMinY =  offset;
        this.toMaxY = -offset;
    }

    // used by vector
    private Offset(double x, double y) {
        toMinX = x;
        toMaxX = x;
        toMinY = y;
        toMaxY = y;
    }

    private Offset(double toMinX, double toMaxX, double toMinY, double toMaxY) {
        this.toMinX = toMinX;
        this.toMaxX = toMaxX;
        this.toMinY = toMinY;
        this.toMaxY = toMaxY;
    }

    public boolean isIdentity() {
        return this == IDENTITY;
    }

    public boolean isConvex() {
        return this == IDENTITY || toMinX <= 0 && toMaxX >= 0 && toMinY <= 0 && toMaxY >= 0;
    }

    public boolean isConcave() {
        return this == IDENTITY || toMinX >= 0 && toMaxX <= 0 && toMinY >= 0 && toMaxY <= 0;
    }

    public Offset reversed() {
        return this == IDENTITY ? IDENTITY : new Offset(-toMinX, -toMaxX, -toMinY, -toMaxY);
    }

    public Rect offset(Rect rect) {
        return rect.offset(this);
    }

    //TODO just have a transform that takes two rectangles? have both?
    public Transform toTransform(Rect rect) {
        return Transform.translateAndScale(rect, rect.offset(this));
    }

    @Override
    public int hashCode() {
        return
                      Double.hashCode(toMinX) +
                (31 * Double.hashCode((toMaxX)) +
                (31 * Double.hashCode((toMinY)) +
                (31 * Double.hashCode((toMaxY)))));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Offset that)) return false;
        if (this.toMinX != that.toMinX) return false;
        if (this.toMaxX != that.toMaxX) return false;
        if (this.toMinY != that.toMinY) return false;
        if (this.toMaxY != that.toMaxY) return false;
        return true;
    }

    @Override
    public String toString() {
        return toMinX + "<-x->" + toMaxX + ", " + toMinY + "<-y->" + toMaxY;
    }
}
