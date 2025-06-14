package com.tomgibara.geom.core;

import com.tomgibara.geom.transform.Transform;

public final class LineSegment implements Traceable, Linear {

	private static int relPos(double f, double min, double max) {
		if (f < min) return -1;
		if (f > max) return 1;
		return 0;
	}

	public static LineSegment fromPoints(Point p1, Point p2) {
		if (p1 == null) throw new IllegalArgumentException("null p1");
		if (p2 == null) throw new IllegalArgumentException("null p2");
		if (p1.equals(p2)) throw new IllegalArgumentException("coincident points");
		return new LineSegment(p1, p2, null, null);
	}

	public static LineSegment fromPoint(Point startAndFinish, Vector tangent) {
		if (startAndFinish == null) throw new IllegalArgumentException("null startAndFinish");
		if (tangent == null) throw new IllegalArgumentException("null tangent");
		if (tangent.isZero()) throw new IllegalArgumentException("zero tangent");
		return new LineSegment(startAndFinish, tangent);
	}

	public static LineSegment fromVector(Point start, Vector vector) {
		if (start == null) throw new IllegalArgumentException("null start");
		if (vector == null) throw new IllegalArgumentException("null vector");
		if (vector.isZero()) throw new IllegalArgumentException("zero vector");
		return new LineSegment(start, vector.translate(start), vector.isUnit() ? vector : null, null);
	}

	public static LineSegment fromCoords(double x1, double y1, double x2, double y2) {
		if (x1 == x2 && y1 == y2) throw new IllegalArgumentException("coincident coords");
		return new LineSegment(new Point(x1, y1), new Point(x2, y2), null, null);
	}

	//TODO find a better name
	public static LineSegment fromCoords(double x, double y, Vector tangent) {
		Point startAndFinish = new Point(x, y);
		if (tangent == null) throw new IllegalArgumentException("null tangent");
		if (tangent.isZero()) throw new IllegalArgumentException("zero tangent");
		return new LineSegment(startAndFinish, tangent);
	}


	private final Point start;
	private final Point finish;
	private Vector tangent;
	private Line line = null;
	private Rect bounds = null;
	private LinearPath path = null;

	private LineSegment(Point p1, Point p2, Vector tangent, Line line) {
		assert (!p1.equals(p2) || tangent != null);
		this.start = p1;
		this.finish = p2;
		this.tangent = tangent;
		this.line = line;
	}

	LineSegment(Point point, Vector tangent) {
		start = point;
		finish = point;
		this.tangent = tangent;
	}

	public LinearPath getPath() {
		return path == null ? path = new LinearPath(this) : path;
	}

	public Point getStart() {
		return start;
	}

	public Point getFinish() {
		return finish;
	}

	public Point getMidpoint() {
		return Point.Util.midpoint(start, finish);
	}

	public boolean isZeroLength() {
		return start == finish;
	}

	// ranges from 0 to 1, clamped to that range
	public Point getInterpolation(double t) {
		if (t <= 0.0) return start;
		if (t >= 1.0) return finish;
		return Point.Util.interpolate(start, finish, t);
	}

	public Line getLine() {
		if (line == null) {
			line = isZeroLength() ?
					Line.fromTangentAtPoint(tangent, start) :
					Line.fromPoints(start, finish);
		}
		return line;
	}

	public boolean isRectilinear() {
		return line == null ? start.x == finish.x || start.y == finish.y : line.getTangent().isRectilinear();
	}

	public Vector getVector() {
		return finish.vectorFrom(start);
	}

	//TODO name is slighly misleading, since true for total inclusion too
	public boolean intersectsRect(Rect rect) {
		if (rect.containsPoint(start)) return true;
		if (rect.containsPoint(finish)) return true;
		if (!Rect.rectsIntersect(rect, getBounds())) return false;
		//TODO could use orientation of line to reduce checks
		int sides =
			sideOf(rect.minX, rect.minY) +
			sideOf(rect.maxX, rect.minY) +
			sideOf(rect.minX, rect.maxY) +
			sideOf(rect.maxX, rect.maxY);
		if (sides == -4 || sides == 4) {
			return false;
		}
		int sx = relPos(start.x, rect.minX, rect.maxX);
		int sy = relPos(start.y, rect.minY, rect.maxY);
		int fx = relPos(finish.x, rect.minX, rect.maxX);
		int fy = relPos(finish.y, rect.minY, rect.maxY);
		if (sy == 0 && sx != 0 && fx == sx) return false;
		if (sx == 0 && sy != 0 && fy == sy) return false;
		if (Math.abs(sx - fx) + Math.abs(sy - fy) < 2) return false;
		return true;
	}

	public Point intersectionWith(LineSegment that) {
		if (that == null) throw new IllegalArgumentException("null that");
		double dax = this.finish.x - this.start.x;
		double day = this.finish.y - this.start.y;
		double dbx = that.finish.x - that.start.x;
		double dby = that.finish.y - that.start.y;
		double d = day * dbx - dax * dby;
		if (d == 0.0) return null;
		double ex = that.start.x - this.start.x;
		double ey = that.start.y - this.start.y;
		double s = ey * dbx - ex * dby;
		if (d > 0 && (s < 0 || s > d)) return null;
		if (d < 0 && (s > 0 || s < d)) return null;
		double t = ey * dax - ex * day;
		if (d > 0 && (t < 0 || t > d)) return null;
		if (d < 0 && (t > 0 || t < d)) return null;
		s /= d;
		return new Point(this.start.x + s * dax, this.start.y + s * day);
	}

	public LineSegment getReverse() {
		return new LineSegment(finish, start, tangent == null ? null : tangent.negated(), line);
	}

	public LineSegment scaleLength(double scale) {
		if (isZeroLength()) return this;
		if (scale == 1.0) return this;
		if (scale == 0.0) return new LineSegment(start, start, tangent, line);
		if (scale == -1.0) return new LineSegment(start, start.vectorFrom(finish).translate(start), tangent == null ? null : tangent.negated(), line);
		return new LineSegment(start, new Point(start.x + (finish.x - start.x) * scale, start.y + (finish.y - start.y) * scale), tangent == null ? null : (scale < 0 ? tangent.negated() : tangent), line);
	}

	@Override
	public LineSegment apply(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		if (t.isIdentity()) return this;
		Point point = t.transform(start);
		return isZeroLength() ?
				new LineSegment(point, point, t.transform(tangent), null) :
				new LineSegment(point, finish.apply(t), tangent == null ? null : t.transform(tangent), null);
	}

	@Override
	public Vector getTangent() {
		if (tangent == null) {
			tangent = finish.vectorFrom(start).normalized();
			assert(!tangent.isZero());
		};
		return tangent;
	}

	@Override
	public Vector getNormal() {
		//TODO cache?
		return getTangent().rotateThroughRightAngles(1);
	}

	@Override
	public Rect getBounds() {
		return bounds == null ? bounds = Rect.atPoints(start, finish) : bounds;
	}

	@Override
	public LineSegment bounded(Rect rect) {
		// simple cases
		Rect bounds = getBounds();
		if (rect.containsRect(bounds)) return this; // fully contained
		if (!Rect.rectsIntersect(rect, bounds)) return null; // no intersection

		// rely on line bounding
		LineSegment extended = getLine().bounded(rect);
		if (extended == null) return null;
		if (extended.getVector().dot(getTangent()) < 0.0) extended = extended.getReverse();
		if (rect.containsPoint(start)) return new LineSegment(start, extended.finish, tangent, line);
		if (rect.containsPoint(finish)) return new LineSegment(extended.start, finish, tangent, line);
		return extended;
	}

	@Override
	public Point nearestPointTo(Point pt) {
		double t = LinearPath.nearestParamIntrinsic(start, finish, pt);
		return getInterpolation(t);
	}

	@Override
	public int sideOf(Point pt) {
		return sideOf(pt.x, pt.y);
	}

	@Override
	public int sideOf(double x, double y) {
		Point finish = isZeroLength() ? tangent.translate(start) : this.finish;
		double det = (finish.x - start.x) * (y - start.y) - (finish.y - start.y) * (x - start.x);
		if (det == 0) return 0;
		return det < 0 ? -1 : 1;
	}

	// object methods

	@Override
	public int hashCode() {
		return start.hashCode() * 31 + finish.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof LineSegment that)) return false;
        if (!this.start.equals(that.start)) return false;
		if (!this.finish.equals(that.finish)) return false;
		return true;
	}

	@Override
	public String toString() {
		return start + " to " + finish;
	}

	LineSegment zeroSubsegment(Point point) {
		return isZeroLength() ? this : new LineSegment(point, point, getTangent(), line);
	}

	// assumes start not equal to finish
	LineSegment nonZeroSubsegment(Point start, Point finish) {
		assert !start.equals(finish);
		return new LineSegment(start, finish, tangent, line);
	}

	LineSegment subsegment(Point start, Point finish) {
		return start.equals(finish) ? zeroSubsegment(start) : nonZeroSubsegment(start, finish);
	}

}
