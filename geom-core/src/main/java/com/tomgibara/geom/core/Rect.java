package com.tomgibara.geom.core;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.helper.Bounder;
import com.tomgibara.geom.transform.Transform;

//TODO use from not at in cons?
public final class Rect implements Geometric {

    public static final Rect UNIT_SQUARE = new Rect(0.0, 0.0, 1.0, 1.0);
    public static final Rect BASIS_SQUARE = new Rect(-1.0, -1.0, 1.0, 1.0);

    public static Rect atOrigin(double x, double y) {
        return new Rect(
            Math.min(x, 0),
            Math.min(y, 0),
            Math.max(x, 0),
            Math.max(y, 0));
    }

    public static Rect centerAtOrigin(double width, double height) {
        double hw = width * 0.5;
        double hh = height * 0.5;
        return new Rect(-hw, -hh, hw, hh);
    }

    public static Rect atOrigin(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        return atOrigin(pt.x, pt.y);
    }

    public static Rect atCenter(Point pt, double width, double height) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        if (width < 0.0) throw new IllegalArgumentException("negative width");
        if (height < 0.0) throw new IllegalArgumentException("negative height");
        double hw = width * 0.5;
        double hh = height * 0.5;
        return new Rect(pt.x - hw, pt.y - hh, pt.x + hw, pt.y + hh);
    }

    public static Rect atPoints(double x1, double y1, double x2, double y2) {
        return new Rect(
            Math.min(x1, x2),
            Math.min(y1, y2),
            Math.max(x1, x2),
            Math.max(y1, y2));
    }

    public static Rect atPoints(Point pt1, Point pt2) {
        if (pt1 == null) throw new IllegalArgumentException("null pt1");
        if (pt2 == null) throw new IllegalArgumentException("null pt2");
        return atPoints(pt1.x, pt1.y, pt2.x, pt2.y);
    }

    public static Rect atPoint(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        return new Rect(pt.x, pt.y, pt.x, pt.y);
    }

    public static Rect atPointWithDimensions(double x, double y, double dx, double dy) {
        return atPoints(x, y, x + dx, y + dy);
    }

    public static Rect atPointWithDimensions(Point pt, Vector dimensions) {
        return atPointWithDimensions(pt.x, pt.y, dimensions.x, dimensions.y);
    }

    public static Rect unionRect(Rect r1, Rect r2) {
        if (r1 == null) throw new IllegalArgumentException("null r1");
        if (r2 == null) throw new IllegalArgumentException("null r2");

        if (r1.containsRect(r2)) return r1;
        if (r2.containsRect(r1)) return r2;

        return new Rect(
            Math.min(r1.minX, r2.minX),
            Math.min(r1.minY, r2.minY),
            Math.max(r1.maxX, r2.maxX),
            Math.max(r1.maxY, r2.maxY)
        );
    }

    public static boolean rectsIntersect(Rect r1, Rect r2) {
        if (r1 == null) throw new IllegalArgumentException("null r1");
        if (r2 == null) throw new IllegalArgumentException("null r2");
        return r1.maxX > r2.minX && r1.minX < r2.maxX && r1.maxY > r2.minY && r1.minY < r2.maxY;
    }

    public static Rect intersectionRect(Rect r1, Rect r2) {
        if (r1 == null) throw new IllegalArgumentException("null r1");
        if (r2 == null) throw new IllegalArgumentException("null r2");
        double minX = Math.max(r1.minX, r2.minX);
        double minY = Math.max(r1.minY, r2.minY);
        double maxX = Math.min(r1.maxX, r2.maxX);
        double maxY = Math.min(r1.maxY, r2.maxY);
        return minX < maxX && minY < maxY ? new Rect(minX, minY, maxX, maxY) : null;
    }

    public final double minX;
    public final double minY;
    public final double maxX;
    public final double maxY;

    Rect(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double getWidth() {
        return maxX - minX;
    }

    public double getHeight() {
        return maxY - minY;
    }

    public Point getCenter() {
        return new Point((minX + maxX) * 0.5, (minY + maxY) * 0.5);
    }

    // if the width or height are zero
    public boolean isDegenerate() {
        return minX == maxX || minY == maxY;
    }

    public Rect translate(double x, double y) {
        return x == 0 && y == 0 ? this : new Rect(minX + x, minY + y, maxX + x, maxY + y);
    }

    public Rect translateToOrigin() {
        return minX == 0.0 && minY == 0.0 ? this : new Rect(0.0, 0.0, getWidth(), getHeight());
    }

    //TODO rename to outset and supply an inset too?
    public Rect offset(double offset) {
        if (offset == 0.0) return this;
        if (offset > 0.0) return new Rect(minX - offset, minY - offset, maxX + offset, maxY + offset);
        return Rect.atPoints(minX - offset, minY - offset, maxX + offset, maxY + offset);
    }

    //TODO put this on offset instead?
    public Rect offset(Offset offset) {
        if (offset == Offset.IDENTITY) return this;
        double x1 = minX + offset.toMinX;
        double y1 = minY + offset.toMinY;
        double x2 = maxX + offset.toMaxX;
        double y2 = maxX + offset.toMaxY;
        //TODO do this more efficiently: if it flips hz or vt then slower path
        // ie if hz. growth + width < 0 flip x1 x2
        // ditto vt. growth
        return offset.isConvex() ?
                new Rect(x1, y1, x2, y2) :
                Rect.atPoints(x1, y1, x2, y2);
    }

    public Offset offsetFrom(Rect rect) {
        return Offset.offset(
                this.minX - rect.minX,
                this.maxX - rect.maxX,
                this.minY - rect.minY,
                this.maxY - rect.maxY
                );
    }

    public Offset offsetTo(Rect rect) {
        return rect.offsetFrom(this);
    }

    public boolean containsPoint(Point pt) {
        return (pt.x >= minX && pt.x <= maxX && pt.y >= minY && pt.y <= maxY);
    }

    public boolean containsRect(Rect rect) {
        return minX <= rect.minX && maxX >= rect.maxX && minY <= rect.minY && maxY >= rect.maxY;
    }

    public Point nearestPointTo(Point pt, boolean edgeOnly) {
        if (pt == null) throw new IllegalArgumentException("null pt");

        double x = pt.x;
        double y = pt.y;
        //  0 | 1 | 2
        // ---+---+---
        //  3 | 4 | 5
        // ---+---+---
        //  6 | 7 | 8
        int c = 0;
        if (x >= minX) c ++;
        if (x > maxX) c ++;
        if (y >= minY) c += 3;
        if (y > maxY) c += 3;

        switch (c) {
        case 0 : return new Point(minX, minY);
        case 1 : return new Point(x, minY);
        case 2 : return new Point(maxX, minY);
        case 3 : return new Point(minX, y);
        case 5 : return new Point(maxX, y);
        case 6 : return new Point(minX, maxY);
        case 7 : return new Point(x, maxY);
        case 8 : return new Point(maxX, maxY);
        default:
            if (edgeOnly) {
                double dxMin = x - minX;
                double dxMax = maxX - x;
                double dyMin = y - minY;
                double dyMax = maxY - y;
                if (dxMin == 0 || dxMax == 0 || dyMin == 0 || dyMax==0) return pt;
                if (dxMin <= dxMax) {
                    if (dyMin <= dyMax) {
                        return dxMin <= dyMin ? new Point(minX, y) : new Point(x, minY);
                    } else {
                        return dxMin <= dyMax ? new Point(minX, y) : new Point(x, maxY);
                    }
                } else {
                    if (dyMin <= dyMax) {
                        return dxMax <= dyMin ? new Point(maxX, y) : new Point(x, minY);
                    } else {
                        return dxMax <= dyMax ? new Point(maxX, y) : new Point(x, maxY);
                    }
                }
            }
            return pt;
        }
    }

    public Point furthestPointTo(Point pt) {
        Point c = getCenter();
        return new Point(
                pt.x <= c.x ? maxX : minX,
                pt.y <= c.y ? maxY : minY
        );
    }

    public FloatRange getXRange() {
        return new FloatRange(minX, maxX);
    }

    public FloatRange getYRange() {
        return new FloatRange(minY, maxY);
    }

    public double getDiagonalLength() {
        return Norm.L2.magnitude(maxX - minX, maxY - minY);
    }

    public double getPerimeterLength() {
        return 2 * (maxX - minX + maxY - minY);
    }
    // geometric methods

    @Override
    public Rect apply(Transform t) {
        if (t == null) throw new IllegalArgumentException("null t");
        //TODO what's the best optimization when t is rectilinear preserving?
        //TODO move to method on Transform?
        return new Bounder()
            .addPoint(t.transform(minX, minY))
            .addPoint(t.transform(maxX, minY))
            .addPoint(t.transform(minX, maxY))
            .addPoint(t.transform(maxX, maxY))
            .getBounds();
    }

    @Override
    public Rect getBounds() {
        return this;
    }

    @Override
    public int hashCode() {
        int h = Double.hashCode(minX);
        h = h * 31 + Double.hashCode(maxX);
        h = h * 31 + Double.hashCode(minY);
        h = h * 31 + Double.hashCode(maxY);
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Rect that)) return false;
        if (this.minX != that.minX) return false;
        if (this.maxX != that.maxX) return false;
        if (this.minY != that.minY) return false;
        if (this.maxY != that.maxY) return false;
        return true;
    }

    @Override
    public String toString() {
        return "x: " + getXRange() + ", y: " + getYRange();
    }

}
