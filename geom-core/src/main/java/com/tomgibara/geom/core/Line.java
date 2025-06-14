package com.tomgibara.geom.core;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.transform.Transform;

public final class Line implements Geometric, Linear {

    public static final Line X_AXIS = new Line(Vector.UNIT_X, 0.0);
    public static final Line Y_AXIS = new Line(Vector.UNIT_Y, 0.0);

    private static double dot(Vector v, Point p) {
        // equivalent to v.dot(p.vectorFromOrigin());
        return v.x * p.x + v.y * p.y;
    }

    public static Line fromAngle(double angle, double distFromOrigin) {
        if (distFromOrigin < 0.0) throw new IllegalArgumentException("negative distFromOrigin");
        return new Line(new Vector(angle), distFromOrigin);
    }

    public static Line fromAngle(double angle) {
        return new Line(new Vector(angle), 0.0);
    }

    public static Line fromTangent(Vector tangent, double distFromOrigin) {
        if (tangent == null) throw new IllegalArgumentException("null tangent");
        if (tangent.isZero()) throw new IllegalArgumentException("zero tangent");
        if (distFromOrigin < 0.0) throw new IllegalArgumentException("negative distFromOrigin");
        return new Line(tangent.normalized(), distFromOrigin);
    }

    public static Line fromTangent(Vector tangent) {
        return fromTangent(tangent, 0.0);
    }

    public static Line fromTangentAtPoint(Vector tangent, Point point) {
        if (tangent == null) throw new IllegalArgumentException("null tangent");
        if (tangent.isZero()) throw new IllegalArgumentException("zero tangent");
        if (point == null) throw new IllegalArgumentException("null point");
        tangent = tangent.normalized();
        Vector normal = tangent.rotateThroughRightAngles(1);
        double distFromOrigin = dot(normal, point);
        return distFromOrigin < 0 ? new Line(tangent.negated(), -distFromOrigin) : new Line(tangent, normal, distFromOrigin);
    }

    public static Line fromPoints(Point p1, Point p2) {
        if (p2 == null) throw new IllegalArgumentException("null p2");
        return fromTangentAtPoint(p2.vectorFrom(p1), p1);
    }

    private final Vector tangent;
    private final Vector normal; // points away from origin
    private final double distFromOrigin;

    private Line(Vector tangent, double distFromOrigin) {
        this(tangent, tangent.rotateThroughRightAngles(1), distFromOrigin);
    }

    private Line(Vector tangent, Vector normal, double distFromOrigin) {
        this.tangent = tangent;
        this.normal = normal;
        this.distFromOrigin = distFromOrigin;
    }

    public double getDistFromOrigin() {
        return distFromOrigin;
    }

    public boolean isThroughOrigin() {
        return distFromOrigin == 0.0;
    }

    public Line parallelLineThrough(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        double d = dot(normal, pt);
        if (d < 0.0) return new Line(tangent.negated(), -d);
        return d == distFromOrigin ? this : new Line(tangent, normal, d);
    }

    public Point getNearestPointToOrigin() {
        return isThroughOrigin() ? Point.ORIGIN : normal.scaled(distFromOrigin).translateOrigin();
    }

    @Override
    public Line getLine() {
        return this;
    }

    @Override
    public Vector getTangent() {
        return tangent;
    }

    @Override
    public Vector getNormal() {
        return normal;
    }

    @Override
    public Point nearestPointTo(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        if (pt.isOrigin()) return getNearestPointToOrigin();
        double d = dot(normal, pt); // distance to pt on parallel line
        if (d == distFromOrigin) return pt; // quick check to see if pt already on line
        // vector to travel back to line from pt
        return normal.scaled(distFromOrigin - d).translate(pt);
    }

    @Override
    public LineSegment bounded(Rect rect) {
        if (rect == null) throw new IllegalArgumentException("null rect");

        FloatRange xRange = rect.getXRange();
        FloatRange yRange = rect.getYRange();

        // horizontal case
        if (tangent.isParallelToXAxis()) {
            double y = distFromOrigin * tangent.x;
            if (!yRange.containsValue(y)) return null;
            double s;
            double f;
            if (tangent.x < 0) {
                s = rect.minX;
                f = rect.maxX;
            } else {
                s = rect.maxX;
                f = rect.minX;
            }
            return LineSegment.fromCoords(s, y, f, y);
        }

        // vertical case
        if (tangent.isParallelToYAxis()) {
            double x = distFromOrigin * -tangent.y;
            if (!xRange.containsValue(x)) return null;
            double s;
            double f;
            if (tangent.y < 0) {
                s = rect.maxY;
                f = rect.minY;
            } else {
                s = rect.minY;
                f = rect.maxY;
            }
            return LineSegment.fromCoords(s, x, f, x);
        }


        Point p1 = null;
        Point p2 = null;
        {
            double minY = yAt(rect.minX);
            if (yRange.containsValue(minY)) {
                p1 = new Point(rect.minX, minY);
            }
        }
        {
            double maxY = yAt(rect.maxX);
            if (yRange.containsValue(maxY)) {
                Point p = new Point(rect.maxX, maxY);
                if (p1 == null) {
                    p1 = p;
                } else {
                    p2 = p;
                }
            }
        }
        if (p2 == null) {
            double minX = xAt(rect.minY);
            if (xRange.containsValue(minX)) {
                Point p = new Point(minX, rect.minY);
                if (p1 == null) {
                    p1 = p;
                } else {
                    p2 = p;
                }
            }
        }
        if (p1 == null) return null;
        {
            double maxX = xAt(rect.maxY);
            if (xRange.containsValue(maxX)) {
                p2 = new Point(maxX, rect.maxY);
            }
        }
        if (p2 == null) return null;
        //TODO ideally want to order them?
        return LineSegment.fromPoints(p1, p2);
    }

    @Override
    public int sideOf(Point point) {
        double dist = normal.dot(normal);
        double d = dist - distFromOrigin;
        if (d == 0.0) return 0;
        return d < 0.0 ? -1 : 1;
    }

    @Override
    public int sideOf(double x, double y) {
        return sideOf(new Point(x, y));
    }

    @Override
    public Line apply(Transform t) {
        if (t == null) throw new IllegalArgumentException("null t");
        //TODO is there a more direct way?
        return fromTangentAtPoint(t.transform(tangent), t.transform(getNearestPointToOrigin()));
    }

    @Override
    public Rect getBounds() {
        //TODO need to support infinite bounds
        // or not implement geometric :-(
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return normal.hashCode() + Double.hashCode(distFromOrigin);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Line that)) return false;
        if (this.distFromOrigin != that.distFromOrigin) return false;
        if (this.normal.equals(that.normal)) return false;
        return true;
    }

    @Override
    public String toString() {
        return distFromOrigin + " from origin at " + tangent.getAngle();
    }

    private double yAt(double x) {
        return (distFromOrigin - normal.x * x) / normal.y;
    }

    private double xAt(double y) {
        return (distFromOrigin - normal.y * y) / normal.x;
    }

}
