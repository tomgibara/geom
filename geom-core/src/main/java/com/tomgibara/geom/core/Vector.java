package com.tomgibara.geom.core;

import com.tomgibara.geom.transform.Transform;
import com.tomgibara.geom.transform.Transformable;

public final class Vector implements Transformable {

	public static Vector ZERO = new Vector(0f, 0f, 0f);
	public static Vector UNIT_X = new Vector(1f, 0f, 1f);
	public static Vector UNIT_Y = new Vector(0f, 1f, 1f);
	public static Vector UNIT_NEG_X = new Vector(-1f, 0f, 1f);
	public static Vector UNIT_NEG_Y = new Vector(0f, -1f, 1f);

	public final float x;
	public final float y;
	private float magnitude;

	//TODO make constructors statics?

	public Vector(Point from, Point to) {
		this.x = to.x - from.x;
		this.y = to.y - from.y;
		magnitude = -1f;
	}

	public Vector(float x, float y) {
		this.x = x;
		this.y = y;
		magnitude = -1f;
	}

	public Vector(float angle) {
		x = (float) Math.cos(angle);
		y = (float) Math.sin(angle);
		magnitude = 1f;
	}

	private Vector(float x, float y, float magnitude) {
		this.x = x;
		this.y = y;
		this.magnitude = magnitude;
	}

	public boolean isZero() {
		return x == 0f && y == 0f;
	}

	public boolean isUnit() {
		return getMagnitudeSqr() == 1f;
	}

	public boolean isRectilinear() {
		return x == 0f || y == 0f;
	}

	public boolean isParallelToXAxis() {
		return y == 0f;
	}

	public boolean isParallelToYAxis() {
		return x == 0f;
	}

	// from x axis
	public float getAngle() {
		return (float) Math.atan2(y, x);
	}

	public float angleFrom(Vector v) {
		return v.angleTo(this);
	}

	public float angleTo(Vector v) {
		float det = x * v.y - y * v.x;
		float d = dot(v);
		if (det == 0f) return d > 0f ? 0 : Angles.PI;
		float c = d / (getMagnitude() * v.getMagnitude());
		if (c > 1.0 || c < -1.0) return 0f; // guard against rounding errors pushing value over 1.0
		return Math.signum(det) * (float) Math.acos(c);
	}

	public Vector scaled(float s) {
		if (s == 1f || isZero()) return this;
		if (s == 0f) return ZERO;
		return magnitude != -1f ? new Vector(x * s, y * s, magnitude * s) : new Vector(x * s, y * s);
	}

	public Vector negated() {
		return isZero() ? this : new Vector(-x, -y);
	}

	public float getMagnitudeSqr() {
		return x * x + y * y;
	}

	public float getMagnitude() {
		if (magnitude == -1f) {
			if (x == 0f) {
				if (y == 0f) {
					magnitude = 0f;
				} else {
					magnitude = Math.abs(y);
				}
			} else if (y == 0) {
				magnitude = Math.abs(x);
			} else {
				magnitude = (float) Math.sqrt(x * x + y * y);
			}
		}
		return magnitude;
	}

	public Vector normalized() {
		if (isZero() || isUnit()) return this;
		float s = 1f / getMagnitude();
		// magnitude may underflow or s may overflow
		if (Float.isInfinite(s)) return ZERO;
		return new Vector(x * s, y * s, 1f);
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

	public float dot(Vector vector) {
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
		switch (a) {
		case 0 : return this;
		case 1 : return new Vector(-y,  x, magnitude);
		case 2 : return new Vector(-x, -y, magnitude);
		case 3 : return new Vector( y, -x, magnitude);
		default: throw new IllegalStateException();
		}
	}

	public Vector apply(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		return t.transform(this);
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(x) ^ 31 * Float.floatToIntBits(y);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Vector)) return false;
		Vector that = (Vector) obj;
		return (this.x == that.x) && (this.y == that.y);
	}

	@Override
	public String toString() {
		return x + "," + y;
	}

}
