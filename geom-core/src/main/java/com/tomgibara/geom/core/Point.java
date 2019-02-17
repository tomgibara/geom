package com.tomgibara.geom.core;

import java.util.ArrayList;

import com.tomgibara.geom.transform.Transform;

public final class Point implements Geometric {

	public static Point ORIGIN = new Point(0f, 0f);

	public static class Util {

		public static Point midpoint(Point pt1, Point pt2) {
			return midpoint(pt1.x, pt2.y, pt2.x, pt2.y);
		}

		public static Point midpoint(float x1, float y1, float x2, float y2) {
			return new Point((x1 + x2) * 0.5f, (y1 + y2) * 0.5f);
		}

		public static Point interpolate(Point pt1, Point pt2, float t) {
			return interpolate(pt1.x, pt1.y, pt2.x, pt2.y, t);
		}

		public static Point interpolate(float x1, float y1, float x2, float y2, float t) {
			float s = 1 - t;
			return new Point(x1 * s + x2 * t, y1 * s + y2 * t);
		}

	}

	public interface Consumer<C> {

		C addPoint(Point pt);

		C addPoint(float x, float y);

	}

	public static class List extends ArrayList<Point> implements Consumer<List> {

		private static final long serialVersionUID = -3708280469741672132L;

		@Override
		public List addPoint(float x, float y) {
			add(new Point(x, y));
			return this;
		}

		@Override
		public List addPoint(Point pt) {
			add(pt);
			return this;
		}

	}

	public final float x;
	public final float y;

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public boolean isOrigin() {
		return x == 0f && y == 0f;
	}

	public Vector vectorFrom(Point p) {
		if (p == null) throw new IllegalArgumentException("null p");
		return p.equals(this) ? Vector.ZERO : new Vector(p, this);
	}

	public Vector vectorTo(Point p) {
		if (p == null) throw new IllegalArgumentException("null p");
		return p.equals(this) ? Vector.ZERO : new Vector(this, p);
	}

	public Vector vectorFromOrigin() {
		return isOrigin() ? Vector.ZERO : new Vector(this.x, this.y);
	}

	@Override
	public Rect getBounds() {
		return Rect.atPoint(this);
	}

	@Override
	public Point apply(Transform t) {
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
		if (!(obj instanceof Point)) return false;
		Point that = (Point) obj;
		return (this.x == that.x) && (this.y == that.y);
	}

	@Override
	public String toString() {
		return x + "," + y;
	}

}
