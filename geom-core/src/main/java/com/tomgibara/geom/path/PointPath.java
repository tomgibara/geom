package com.tomgibara.geom.path;

import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.Line;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Linear;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Point.Consumer;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public class PointPath implements Path, Linear {

	public static PointPath from(Point point, Vector tangent) {
		if (point == null) throw new IllegalArgumentException("null point");
		if (tangent == null) throw new IllegalArgumentException("null tangent");
		return new PointPath(point, tangent.normalized());
	}

	public static PointPath from(LineSegment segment) {
		if (segment == null) throw new IllegalArgumentException("null segment");
		return new PointPath(segment.getStart(), segment.getTangent());
	}

	private final Point point;
	private final Vector tangent;

	private Rect bounds = null;
	private ByParam byParam = null;
	private LineSegment segment = null;

	private PointPath(Point point, Vector tangent) {
		this.point = point;
		this.tangent = tangent;
	}

	public LineSegment getTangentAsSegment() {
		return LineSegment.fromVector(point, tangent);
	}

	@Override
	public LineSegment bounded(Rect bounds) {
		return bounds.containsPoint(point) ? getSegment() : null;
	}

	@Override
	public Line getLine() {
		return getSegment().getLine();
	}

	@Override
	public Vector getNormal() {
		return tangent.rotateThroughRightAngles(1);
	}

	@Override
	public Vector getTangent() {
		return tangent;
	}

	@Override
	public Point nearestPointTo(Point pt) {
		return point;
	}

	@Override
	public int sideOf(float x, float y) {
		Point finish = tangent.translate(point);
		float det = (finish.x - point.x) * (y - point.y) - (finish.y - point.y) * (x - point.x);
		if (det == 0) return 0;
		return det < 0 ? -1 : 1;
	}

	@Override
	public int sideOf(Point point) {
		return sideOf(point.x, point.y);
	}

	@Override
	public Rect getBounds() {
		return bounds == null ? bounds = Rect.atPoint(point) : bounds;
	}

	@Override
	public PointPath apply(Transform t) {
		return t.isIdentity() ? this : new PointPath(t.transform(point), t.transform(tangent));
	}

	@Override
	public LineSegment getGeometry() {
		return LineSegment.fromPoint(point, tangent);
	}

	@Override
	public Point getStart() {
		return point;
	}

	@Override
	public Point getFinish() {
		return point;
	}

	@Override
	public float getLength() {
		return 0;
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public boolean isRectilinear() {
		return true;
	}

	@Override
	public boolean isSmooth() {
		return true;
	}

	@Override
	public ByParam byIntrinsic() {
		return byParam == null ? new ByParam() : byParam;
	}

	@Override
	public ByParam byLength() {
		return byParam == null ? new ByParam() : byParam;
	}

	@Override
	public <K> K linearize(Consumer<K> consumer) {
		return consumer.addPoint(point);
	}

	@Override
	public SimplifiedPath simplify() {
		return new SimplifiedPath(LineSegment.fromPoint(point, tangent).getPath());
	}

	@Override
	public PointPath getReverse() {
		return new PointPath(point, tangent.negated());
	}

	@Override
	public SingletonPath splitAtCorners() {
		return new SingletonPath(this);
	}

	private LineSegment getSegment() {
		return segment == null ? segment = LineSegment.fromPoint(point, tangent) : segment;
	}

	private class ByParam implements Parameterization.ByIntrinsic, Parameterization.ByLength {

		@Override
		public Path getPath() {
			return PointPath.this;
		}

		//TODO consider abstract base class
		@Override
		public Path.Location location() {
			return new Path.Location(this, 0f);
		}

		@Override
		public Point pointAt(float p) {
			return point;
		}

		@Override
		public Vector tangentAt(float p) {
			return tangent;
		}

		@Override
		public PointPath pointTangentAt(float p) {
			return PointPath.this;
		}

		@Override
		// returns point paths - this path twice
		public SplitPath splitAt(float p) {
			return new SplitPath(PointPath.this, PointPath.this, false);
		}

		@Override
		public Path segment(float minP, float maxP) {
			return PointPath.this;
		}

		@Override
		public float parameterNearest(Point p) {
			return 0f;
		}

		@Override
		public float lengthAt(float p) {
			return 0f;
		}

		@Override
		public List<Path.Corner> getCorners() {
			return Path.Corner.NO_CORNERS;
		}

		@Override
		public float intrinsicAt(float p) {
			return 0f;
		}

	}

}
