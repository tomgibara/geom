package com.tomgibara.geom.curve;

import static com.tomgibara.geom.floats.FloatMapping.Util.compose;
import static com.tomgibara.geom.floats.FloatMapping.Util.linear;

import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.floats.FloatMapping;
import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.path.Parameterization;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;
import com.tomgibara.geom.path.SplitPath;

//TODO move to curve?
public final class OffsetCurve extends Curve {

	public static OffsetCurve from(Path path, FloatMapping mapping) {
		if (path == null) throw new IllegalArgumentException("null path");
		if (!path.isSmooth()) throw new IllegalArgumentException("path has corners");
		if (mapping == null) throw new IllegalArgumentException("null mapping");

		FloatRange domain = mapping.getDomain();
		if (!domain.equals(FloatRange.UNIT_CLOSED)) {
			// composition would factor is away anyway, but more efficient to check first
			mapping = compose(linear(FloatRange.UNIT_CLOSED, domain), mapping);
		}
		//TODO is there an optimization we can make when offset is constant zero?
		return new OffsetCurve(path, mapping, path.getLength());
	}

	private final Parameterization.ByLength param;
	private final FloatMapping mapping;
	private final float length;
	private final boolean constant;

	private OffsetCurve(Path path, FloatMapping mapping, float length) {
		this.mapping = mapping;
		this.length = path.getLength();
		param = path.byLength();
		constant = mapping.getRange().isZeroSize();
	}

	@Override
	public Point pointAt(float t) {
		t = clamp(t);
		// convert to length
		float p = t * length;
		// obtain point and tangent
		PointPath pp = param.pointTangentAt(p);
		// compute the displacement
		float d = mapping.map(t);
		// derive the point
		return pp.getTangent().rotateThroughRightAngles(1).scaled(d).translate(pp.getStart());
	}

	@Override
	public SplitCurvePath splitAt(float t) {
		t = clamp(t);
		FloatMapping m1 = compose(linear(FloatRange.UNIT_CLOSED, 0f, t), mapping);
		FloatMapping m2 = compose(linear(FloatRange.UNIT_CLOSED, t, 1f), mapping);
		float p = t * length;
		SplitPath split = param.splitAt(t * length);
		OffsetCurve c1 = new OffsetCurve(split.getFirstPath(), m1, p);
		OffsetCurve c2 = new OffsetCurve(split.getLastPath(), m2, length - p);
		return new SplitCurvePath(c1.getPath(), c2.getPath(), isClosed());
	}

	@Override
	public Vector tangentAt(float t) {
		return constant ? param.tangentAt(t * length) : super.tangentAt(t);
	}

	@Override
	protected CurvePath createPath() {
		return new OffsetPath(this);
	}

	@Override
	protected boolean isClosed() {
		return param.getPath().isClosed() && isMappingPeriodic();
	}

	@Override
	protected boolean isLinear() {
		Point p0 = pointAt(0f);
		Point p1 = pointAt(1f);
		// hack test
		Vector v1 = p1.vectorFrom(p0);
		if (v1.isZero()) return true;
		PointPath pp = pointTangentAt(0.5f);
		Point pm = Point.Util.midpoint(p0, p1);
		//TODO use configurable threshold
		float pd = Norm.L2.powDistanceBetween(pm, pp.getStart());
		if (pd > 0.00001f) return false;
		float d = pp.getTangent().dot(v1.normalized());
		if (Math.abs(1f - d) > 0.001) return false;
		return true;
	}

	private boolean isMappingPeriodic() {
		if (constant) return true;
		FloatRange domain = mapping.getDomain();
		return mapping.map(domain.min) == mapping.map(domain.max);
	}

	@Override
	public String toString() {
		return mapping + " offset from " + param.getPath();
	}

	private static class OffsetPath extends CurvePath {

		OffsetPath(OffsetCurve z) {
			super(z);
		}

		@Override
		protected Rect computeBounds() {
			// note: guaranteed to be an offset curve because that is what we pass in to constructor
			OffsetCurve z = (OffsetCurve) getCurve();
			return z.param.getPath().getBounds().offset(z.mapping.getRange().max);
		}

	}

}
