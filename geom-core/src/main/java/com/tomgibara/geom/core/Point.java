package com.tomgibara.geom.core;

import java.util.ArrayList;

import com.tomgibara.geom.transform.Transform;

public final class Point implements Geometric {

	public static Point ORIGIN = new Point(0.0, 0.0);

	public static class Util {

		public static Point midpoint(Point pt1, Point pt2) {
			return midpoint(pt1.x, pt2.y, pt2.x, pt2.y);
		}

		public static Point midpoint(double x1, double y1, double x2, double y2) {
			return new Point((x1 + x2) * 0.5, (y1 + y2) * 0.5);
		}

		public static Point interpolate(Point pt1, Point pt2, double t) {
			return interpolate(pt1.x, pt1.y, pt2.x, pt2.y, t);
		}

		public static Point interpolate(double x1, double y1, double x2, double y2, double t) {
			double s = 1 - t;
			return new Point(x1 * s + x2 * t, y1 * s + y2 * t);
		}

	}

	public interface Consumer<C> {

		C addPoint(Point pt);

		C addPoint(double x, double y);

	}

	public static class List extends ArrayList<Point> implements Consumer<List> {

		private static final long serialVersionUID = -3708280469741672132L;

		@Override
		public List addPoint(double x, double y) {
			add(new Point(x, y));
			return this;
		}

		@Override
		public List addPoint(Point pt) {
			add(pt);
			return this;
		}

	}

	public final double x;
	public final double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public boolean isOrigin() {
		return x == 0.0 && y == 0.0;
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
		return Double.hashCode(x) + 31 * Double.hashCode(y);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Point that)) return false;
        return (this.x == that.x) && (this.y == that.y);
	}

	@Override
	public String toString() {
		return x + "," + y;
	}

}
