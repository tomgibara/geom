package com.tomgibara.geom.core;

import com.tomgibara.geom.transform.Transform;
import com.tomgibara.geom.transform.Transformable;

public final class Vector implements Transformable {

    public static Vector ZERO = new Vector(0.0, 0.0, 0.0);
    public static Vector UNIT_X = new Vector(1.0, 0.0, 1.0);
    public static Vector UNIT_Y = new Vector(0.0, 1.0, 1.0);
    public static Vector UNIT_NEG_X = new Vector(-1.0, 0.0, 1.0);
    public static Vector UNIT_NEG_Y = new Vector(0.0, -1.0, 1.0);

    public final double x;
    public final double y;
    private double magnitude;

    //TODO make constructors statics?

    public Vector(Point from, Point to) {
        this.x = to.x - from.x;
        this.y = to.y - from.y;
        magnitude = -1.0;
    }

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
        magnitude = -1.0;
    }

    public Vector(double angle) {
        x = Math.cos(angle);
        y = Math.sin(angle);
        magnitude = 1.0;
    }

    private Vector(double x, double y, double magnitude) {
        this.x = x;
        this.y = y;
        this.magnitude = magnitude;
    }

    public boolean isZero() {
        return x == 0.0 && y == 0.0;
    }

    public boolean isUnit() {
        return getMagnitudeSqr() == 1.0;
    }

    public boolean isRectilinear() {
        return x == 0.0 || y == 0.0;
    }

    public boolean isParallelToXAxis() {
        return y == 0.0;
    }

    public boolean isParallelToYAxis() {
        return x == 0.0;
    }

    // from x axis
    public double getAngle() {
        return Math.atan2(y, x);
    }

    public double angleFrom(Vector v) {
        return v.angleTo(this);
    }

    public double angleTo(Vector v) {
        double det = x * v.y - y * v.x;
        double d = dot(v);
        if (det == 0.0) return d > 0.0 ? 0 : Angles.PI;
        double c = d / (getMagnitude() * v.getMagnitude());
        if (c > 1.0 || c < -1.0) return 0.0; // guard against rounding errors pushing value over 1.0
        return Math.signum(det) * Math.acos(c);
    }

    public Vector scaled(double s) {
        if (s == 1.0 || isZero()) return this;
        if (s == 0.0) return ZERO;
        return magnitude != -1.0 ? new Vector(x * s, y * s, magnitude * s) : new Vector(x * s, y * s);
    }

    public Vector negated() {
        return isZero() ? this : new Vector(-x, -y);
    }

    public double getMagnitudeSqr() {
        return x * x + y * y;
    }

    public double getMagnitude() {
        if (magnitude == -1.0) {
            if (x == 0.0) {
                if (y == 0.0) {
                    magnitude = 0.0;
                } else {
                    magnitude = Math.abs(y);
                }
            } else if (y == 0) {
                magnitude = Math.abs(x);
            } else {
                magnitude = Math.sqrt(x * x + y * y);
            }
        }
        return magnitude;
    }

    public Vector normalized() {
        if (isZero() || isUnit()) return this;
        double s = 1.0 / getMagnitude();
        // magnitude may underflow or s may overflow
        if (Double.isInfinite(s)) return ZERO;
        return new Vector(x * s, y * s, 1.0);
    }

    public Vector strictlyNormalized() {
        if (isUnit()) return this;
        if (isZero()) {
            throw new IllegalStateException("cannot normalize zero vector");
        }
        double s = 1.0 / getMagnitude();
        if (Double.isInfinite(s)) throw new IllegalStateException("normalization failed on underflow");
        return new Vector(x * s, y * s, 1.0);
    }

    public Vector add(Vector v) {
        if (this.isZero()) return v;
        if (v.isZero()) return this;
        return new Vector(x + v.x, y + v.y);
    }

    public Vector subtract(Vector v) {
        if (v.isZero()) return this;
        return new Vector(x - v.x, y - v.y);
    }

    public double dot(Vector vector) {
        if (vector == null) throw new IllegalArgumentException("null vector");
        return x * vector.x + y * vector.y;
    }

    public Offset asOffset() {
        return Offset.translation(x, y);
    }

    public Transform asTranslation() {
        return Transform.translation(x, y);
    }

    public Transform asRotation() {
        return Transform.rotationAndScale(this);
    }

    public Point translate(Point p) {
        return isZero() ? p : new Point(p.x + x, p.y + y);
    }

    public Point translateOrigin() {
        return isZero() ? Point.ORIGIN : new Point(x, y);
    }

    // needed in addition to transform optimization to preserve magnitude
    public Vector rotateThroughRightAngles(int rightAngles) {
        int a = rightAngles & 0x3;
        return switch (a) {
            case 0 -> this;
            case 1 -> new Vector(-y,  x, magnitude);
            case 2 -> new Vector(-x, -y, magnitude);
            case 3 -> new Vector( y, -x, magnitude);
            default -> throw new IllegalStateException();
        };
    }

    public Vector apply(Transform t) {
        if (t == null) throw new IllegalArgumentException("null t");
        return t.transform(this);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(x) * 31 + Double.hashCode(y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Vector that)) return false;
        return (this.x == that.x) && (this.y == that.y);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

}
