package com.tomgibara.geom.core;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.transform.Transform;

public final class Line implements Geometric, Linear {

	public static final Line X_AXIS = new Line(Vector.UNIT_X, 0f);
	public static final Line Y_AXIS = new Line(Vector.UNIT_Y, 0f);

	private static float dot(Vector v, Point p) {
		// equivalent to v.dot(p.vectorFromOrigin());
		return v.x * p.x + v.y * p.y;
	}

	public static Line fromAngle(float angle, float distFromOrigin) {
		if (distFromOrigin < 0f) throw new IllegalArgumentException("negative distFromOrigin");
		return new Line(new Vector(angle), distFromOrigin);
	}

	public static Line fromAngle(float angle) {
		return new Line(new Vector(angle), 0f);
	}

	public static Line fromTangent(Vector tangent, float distFromOrigin) {
		if (tangent == null) throw new IllegalArgumentException("null tangent");
		if (tangent.isZero()) throw new IllegalArgumentException("zero tangent");
		if (distFromOrigin < 0f) throw new IllegalArgumentException("negative distFromOrigin");
		return new Line(tangent.normalized(), distFromOrigin);
	}

	public static Line fromTangent(Vector tangent) {
		return fromTangent(tangent, 0f);
	}

	public static Line fromTangentAtPoint(Vector tangent, Point point) {
		if (tangent == null) throw new IllegalArgumentException("null tangent");
		if (tangent.isZero()) throw new IllegalArgumentException("zero tangent");
		if (point == null) throw new IllegalArgumentException("null point");
		tangent = tangent.normalized();
		Vector normal = tangent.rotateThroughRightAngles(1);
		float distFromOrigin = dot(normal, point);
		return distFromOrigin < 0 ? new Line(tangent.negated(), -distFromOrigin) : new Line(tangent, normal, distFromOrigin);
	}

	public static Line fromPoints(Point p1, Point p2) {
		if (p2 == null) throw new IllegalArgumentException("null p2");
		return fromTangentAtPoint(p2.vectorFrom(p1), p1);
	}

	private final Vector tangent;
	private final Vector normal; // points away from origin
	private final float distFromOrigin;

	private Line(Vector tangent, float distFromOrigin) {
		this(tangent, tangent.rotateThroughRightAngles(1), distFromOrigin);
	}

	private Line(Vector tangent, Vector normal, float distFromOrigin) {
		this.tangent = tangent;
		this.normal = normal;
		this.distFromOrigin = distFromOrigin;
	}

	public float getDistFromOrigin() {
		return distFromOrigin;
	}

	public boolean isThroughOrigin() {
		return distFromOrigin == 0f;
	}

	public Line parallelLineThrough(Point pt) {
		if (pt == null) throw new IllegalArgumentException("null pt");
		float d = dot(normal, pt);
		if (d < 0f) return new Line(tangent.negated(), -d);
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
		float d = dot(normal, pt); // distance to pt on parallel line
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
			float y = distFromOrigin * tangent.x;
			if (!yRange.containsValue(y)) return null;
			float s;
			float f;
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
			float x = distFromOrigin * -tangent.y;
			if (!xRange.containsValue(x)) return null;
			float s;
			float f;
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
			float minY = yAt(rect.minX);
			if (yRange.containsValue(minY)) {
				p1 = new Point(rect.minX, minY);
			}
		}
		{
			float maxY = yAt(rect.maxX);
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
			float minX = xAt(rect.minY);
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
			float maxX = xAt(rect.maxY);
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
		float dist = normal.dot(normal);
		float d = dist - distFromOrigin;
		if (d == 0f) return 0;
		return d < 0f ? -1 : 1;
	}

	@Override
	public int sideOf(float x, float y) {
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
		return normal.hashCode() ^ Float.floatToIntBits(distFromOrigin);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Line)) return false;
		Line that = (Line) obj;
		if (this.distFromOrigin != that.distFromOrigin) return false;
		if (this.normal.equals(that.normal)) return false;
		return true;
	}

	@Override
	public String toString() {
		return distFromOrigin + " from origin at " + tangent.getAngle();
	}

	private float yAt(float x) {
		return (distFromOrigin - normal.x * x) / normal.y;
	}

	private float xAt(float y) {
		return (distFromOrigin - normal.y * y) / normal.x;
	}

}
