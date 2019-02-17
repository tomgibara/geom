package com.tomgibara.geom.curve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.GeomUtil;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.helper.Bounder;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;
import com.tomgibara.geom.path.SimplifiedPath;
import com.tomgibara.geom.path.SplitPath;
import com.tomgibara.geom.transform.Transform;

public final class BezierCurve extends Curve {

	// static helper methods

	private static List<Point> safePoints(Point... points) {
		if (points == null) throw new IllegalArgumentException("null points");
		if (points.length == 0) throw new IllegalArgumentException("empty points");
		List<Point> list = GeomUtil.asList(points.clone());
		if (list.contains(null)) throw new IllegalArgumentException("null point");
		return list;
	}

	private static List<Point> safePoints(List<Point> points) {
		if (points == null) throw new IllegalArgumentException("null points");
		if (points.isEmpty()) throw new IllegalArgumentException("empty points");
		return Collections.unmodifiableList(new ArrayList<>(points));
	}

	public static BezierCurve fromPoints(Point... points) {
		return new BezierCurve(safePoints(points), null);
	}

	public static BezierCurve fromPoints(List<Point> points) {
		return new BezierCurve(safePoints(points), null);
	}

	// fields

	private final int order;
	private final float[] coords;
	private final List<Point> points;

	BezierCurve(List<Point> points, float[] coords) {
		int size = points.size();
		if (size == 0) throw new IllegalStateException();
		int order = size - 1;
		if (coords == null) {
			coords = new float[size * 2];
			for (int i = 0, j = 0; i < size; i++) {
				Point point = points.get(i);
				coords[j++] = point.x;
				coords[j++] = point.y;
			}
		}
		this.points = points;
		this.coords = coords;
		this.order = order;
	}

	private BezierCurve(Point from, Point to) {
		points = GeomUtil.asList(from, to);
		coords = new float[] { from.x, from.y, to.x, to.y };
		order = 1;
	}

	private BezierCurve(Point only) {
		points = Collections.singletonList(only);
		coords = new float[] { only.x, only.y };
		order = 0;
	}

	public int getOrder() {
		return order;
	}

	public List<Point> getPoints() {
		return points;
	}

	@Override
	public Point pointAt(float t) {
		if (t <= 0) return points.get(0);
		if (t >= 1) return points.get(order);
		switch (order) {
		case 0 : return points.get(0);
		case 1 : return Point.Util.interpolate(points.get(0), points.get(1), t);
		default:
			float[] fs = computeCasteljau(t, coords);
			return new Point(fs[0], fs[1]);
		}
	}

	@Override
	public Vector tangentAt(float t) {
		switch (order) {
		case 0 : return Vector.ZERO;
		case 1 : return points.get(0).vectorTo(points.get(1)).normalized();
		default:
			t = clamp(t);
			if (t == 0f) return points.get(1).vectorFrom(points.get(0)).normalized();
			if (t == 1f) return points.get(order).vectorFrom(points.get(order - 1)).normalized();
			float[] fs = computeCasteljau(clamp(t), coords);
			return new Vector(fs[2] - fs[0], fs[3] - fs[1]).normalized();
		}
	}

	@Override
	public PointPath pointTangentAt(float t) {
		if (order == 0) {
			//TODO how to handle this properly
			return PointPath.from(points.get(0), Vector.UNIT_X);
		}
		if (order < 3) return PointPath.from(pointAt(t), tangentAt(t));
		t = clamp(t);
		Point start;
		Vector tangent;
		if (t == 0f) {
			start = points.get(0);
			tangent = points.get(1).vectorFrom(start).normalized();
		} else if (t == 1f) {
			start = points.get(order);
			tangent = points.get(order - 1).vectorTo(start).normalized();
		} else {
			float[] fs = computeCasteljau(t, coords);
			start = new Point(fs[0], fs[1]);
			tangent = new Vector(fs[2] - fs[0], fs[3] - fs[1]).normalized();
		}
		return PointPath.from(start, tangent);
	}

	public float magnitudeAt(float t) {
		Norm norm = Norm.L2;
		if (t <= 0) return norm.distanceFromOrigin(points.get(0));
		if (t >= 1) return norm.distanceFromOrigin(points.get(order));
		switch (order) {
		case 0 : return norm.distanceFromOrigin(points.get(0));
		case 1 : return norm.distanceFromOrigin(Point.Util.interpolate(points.get(0), points.get(1), t));
		default:
			float[] fs = computeCasteljau(t, coords);
			return norm.magnitude(fs[0], fs[1]);
		}
	}

	@Override
	// note: splits into smaller beziers
	public SplitCurvePath splitAt(float t) {
		// TODO consider a pointpath?
		boolean closed = getPath().isClosed();
		if (t <= 0) return new SplitCurvePath(new BezierCurve(GeomUtil.asList(points.get(0)), null).getPath(), getPath(), closed);
		if (t >= 1) return new SplitCurvePath(getPath(), new BezierCurve(GeomUtil.asList(points.get(order)), null).getPath(), closed);
		float[] fs = computeCasteljau(t, coords);
		return new SplitCurvePath(curveTo(t, fs).getPath(), curveFrom(t, fs).getPath(), closed);
	}

	@Override
	protected BezierCurve computeSegment(float minP, float maxP) {
		BezierCurve tail = fromPoints(computeCasteljau(minP, coords).clone(), true);
		BezierCurve head = fromPoints(computeCasteljau((maxP - minP)/(1f - minP), tail.coords).clone(), false);
		return head;
	}

	@Override
	protected Curve computeDerivative() {
		List<Point> pts = new ArrayList<>(order);
		float[] cs = new float[order * 2];
		float[] coords = this.coords;
		for (int i = 0; i < cs.length;) {
			float ax = coords[i    ];
			float ay = coords[i + 1];
			float bx = coords[i + 2];
			float by = coords[i + 3];
			float x = (bx - ax) * order;
			float y = (by - ay) * order;
			cs[i++] = x;
			cs[i++] = y;
			pts.add(new Point(x,y));
		}
		return new BezierCurve(pts, cs);
	}

	@Override
	protected Curve computeReverse() {
		return getPath().getReverse().getCurve();
	}

	@Override
	protected boolean isLinear() {
		return order <= 1;
	}

	@Override
	protected CurvePath createPath() {
		return new BezierPath(this);
	}

	// returns working floats
	private static float[] computeCasteljau(float t, float[] coords) {
		final float s = 1 - t;
		int length = coords.length;
		float[] fs = GeomUtil.workingFloats(length * 2);
		System.arraycopy(coords, 0, fs, 0, length);
		int j = length;
		for (length -= 2; length > 0; length -=2) {
			fs[j++] = fs[0];
			fs[j++] = fs[1];
			for (int i = 0; i < length; i += 2) {
				fs[i    ] = fs[i    ] * s + fs[i + 2] * t;
				fs[i + 1] = fs[i + 1] * s + fs[i + 3] * t;
			}
		}
		fs[j++] = fs[0];
		fs[j++] = fs[1];
		return fs;
	}

	private BezierCurve curveFrom(float t, float[] fs) {
		if (t <= 0) return this;
		if (t >= 1) return new BezierCurve(points.get(order));
		switch (order) {
		case 0 : return this;
		case 1 :
			Point pt0 = points.get(0);
			Point pt1 = points.get(1);
			Point ptT = Point.Util.interpolate(pt0, pt1, t);
			return new BezierCurve(ptT, pt1);
		default:
			return fromPoints(fs == null ? computeCasteljau(t, coords) : fs, true);
		}
	}

	private BezierCurve curveTo(float t, float[] fs) {
		if (t <= 0) return new BezierCurve(points.get(0));
		if (t >= 1) return this;
		switch (order) {
		case 0 : return this;
		case 1 :
			Point pt0 = points.get(0);
			Point pt1 = points.get(1);
			Point ptT = Point.Util.interpolate(pt0, pt1, t);
			return new BezierCurve(pt0, ptT);
		default:
			return fromPoints(fs == null ? computeCasteljau(t, coords) : fs, false);
		}
	}

	private BezierCurve fromPoints(float[] fs, boolean from) {
		Point[] pts = new Point[order + 1];
		int offset = from ? 0 : order * 2 + 2;
		for (int i = 0; i < pts.length; i++) {
			pts[i] = new Point(fs[offset + 2 * i], fs[offset + 2 * i + 1]);
		}
		return new BezierCurve(GeomUtil.asList(pts), null);
	}

	// inner classes

	private static final class BezierPath extends CurvePath {

		// fields

		// duplicates field in base class, but commonly used, and quicker
		private final BezierCurve z;

		// constructors

		BezierPath(BezierCurve z) {
			super(z);
			this.z = z;
		}

		// optimized curve methods

		@Override
		public Point getStart() {
			return z.points.get(0);
		}

		@Override
		public Point getFinish() {
			return z.points.get(z.order);
		}

		@Override
		public <K> K linearize(Point.Consumer<K> consumer) {
			switch (z.order) {
			case 0 : return consumer.addPoint(z.points.get(0));
			case 1 :
				consumer.addPoint(z.points.get(0));
				return consumer.addPoint(z.points.get(1));
			default:
				SplitCurvePath curves = ((BezierCurve)z).splitAt(0.5f);
				curves.getFirstPath().simplifyCurve().linearize(consumer);
				return curves.getLastPath().simplifyCurve().linearize(consumer);
			}
		}

		@Override
		public SimplifiedPath simplify() {
			switch (z.order) {
			//TODO disallow zero order curve
			case 0 : return new SimplifiedPath(LineSegment.fromPoint(z.points.get(0), Vector.UNIT_X).getPath());
			case 1 : return new SimplifiedPath(LineSegment.fromPoints(z.points.get(0), z.points.get(1)).getPath());
			default:
				SplitCurvePath curves = ((BezierCurve)z).splitAt(0.5f);
				Path p1 = curves.getFirstPath().simplifyCurve();
				Path p2 = curves.getLastPath().simplifyCurve();
				return new SimplifiedPath(new SplitPath(p1, p2, isClosed()));
			}
		}

		@Override
		public boolean isRectilinear() {
			switch (z.order) {
			case 0 : return true;
			case 1 :
				Point start = z.points.get(0);
				Point finish = z.points.get(z.order);
				return start.x == finish.x || start.y == finish.y;
				default: return false;
			}
		}

		@Override
		public CurvePath getReverse() {
			Point[] pts = (Point[]) z.points.toArray(new Point[z.order + 1]);
			GeomUtil.reverseArray(pts);
			return new BezierCurve(GeomUtil.asList(pts), null).getPath();
		}

		@Override
		public CurvePath apply(Transform t) {
			if (t == null) throw new IllegalArgumentException("null t");
			if (t.isIdentity()) return this;
			float[] coords = z.coords.clone();
			t.transform(coords);
			Point[] points = new Point[z.order + 1];
			for (int i = 0, j = 0; i < points.length; i++, j++) {
				points[i] = new Point(coords[j], coords[j++]);
			}
			return new BezierCurve(GeomUtil.asList(points), coords).getPath();
		}

		// object methods

		@Override
		public int hashCode() {
			return z.points.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof BezierPath)) return false;
			BezierPath that = (BezierPath) obj;
			return this.z.points.equals(that.z.points);
		}

		@Override
		public String toString() {
			return z.points.toString();
		}

		protected Rect computeBounds() {
			Bounder consumer = new Bounder();
			for (Point point : z.points) {
				consumer.addPoint(point);
			}
			return consumer.getBounds();
		}

	}


}