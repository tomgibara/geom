package com.tomgibara.geom.curve;

import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Tolerances;
import com.tomgibara.geom.helper.Bounder;
import com.tomgibara.geom.helper.Locator;
import com.tomgibara.geom.path.AbstractPath;
import com.tomgibara.geom.path.Parameterization;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.Reparameterization;
import com.tomgibara.geom.path.SimplifiedPath;
import com.tomgibara.geom.path.SingletonPath;
import com.tomgibara.geom.path.SplitPath;
import com.tomgibara.geom.transform.Transform;

public class CurvePath extends AbstractPath {

	// fields

	private final Curve z;
	private Parameterization.ByLength byLength = null;
	private float length = -1;
	private Rect bounds = null;
	private Boolean closed = null;

	// constructors

	public CurvePath(Curve z) {
		if (z == null) throw new IllegalArgumentException("null z");
		this.z = z;
		if (z instanceof Parameterization.ByLength) {
			byLength = (Parameterization.ByLength) z;
		}
	}

	// accessors

	public Curve getCurve() {
		return z;
	}

	// path methods

	@Override
	public Point getStart() {
		return z.pointAt(0f);
	}

	@Override
	public Point getFinish() {
		return z.pointAt(1f);
	}

	@Override
	public <K> K linearize(Point.Consumer<K> consumer) {
		if (consumer == null) throw new IllegalArgumentException("null consumer");
		return linearizeImpl(consumer, 0, null);
	}

	private <K> K linearizeImpl(Point.Consumer<K> consumer, int depth, K last) {
		//TODO remove this hacky recursion limit if possible - it doesn't work in all cases and is ugly
		if (depth > Tolerances.current().getSplitRecursionLimit()) {
			return last;
		}
		if (z.isLinear()) {
			consumer.addPoint(getStart());
			return consumer.addPoint(getFinish());
		}
		float h = z.getDefaultSplitParam();
		SplitCurvePath curves = z.splitAt(h);
		//TODO hacky
		Path s1 = curves.getFirstPath().simplifyCurve();
		if (s1 instanceof CurvePath) {
			last = (K) ((CurvePath) s1).linearizeImpl(consumer, depth + 1, last);
		} else {
			last = s1.linearize(consumer);
		}
		Path s2 = curves.getLastPath().simplifyCurve();
		if (s2 instanceof CurvePath) {
			last = (K) ((CurvePath) s2).linearizeImpl(consumer, depth + 1, last);
		} else {
			last = s2.linearize(consumer);
		}
		return last;
	}

	@Override
	public SimplifiedPath simplify() {
		if (z.isLinear()) {
			Point start = getStart();
			Point finish = getFinish();
			LineSegment segment = start.equals(finish) ?
				LineSegment.fromPoint(start, z.tangentAt(0.5f)) :
				LineSegment.fromPoints(start, finish);
			return new SimplifiedPath(segment.getPath());
		}
		SplitCurvePath curves = z.splitAt(z.getDefaultSplitParam());
		Path p1 = curves.getFirstPath().simplifyCurve();
		Path p2 = curves.getLastPath().simplifyCurve();
		return new SimplifiedPath(new SplitPath(p1, p2, isClosed()));
	}

	@Override
	public float getLength() {
		return length < 0 ? length = z.intrinsicToLength(1f) : length;
	}

	@Override
	public boolean isClosed() {
		if (closed == null) closed = z.isClosed();
		return closed;
	}

	// all curves should be smooth
	@Override
	public boolean isSmooth() {
		return true;
	}

	@Override
	public Parameterization.ByIntrinsic byIntrinsic() {
		return z;
	}

	@Override
	public Parameterization.ByLength byLength() {
		return byLength == null ? byLength = new ByLength(z) : byLength;
	}

	@Override
	public CurvePath getReverse() {
		//TODO cache, or cache on curve?
		return z.computeReverse().getPath();
	}

	@Override
	public SingletonPath splitAtCorners() {
		return new SingletonPath(this);
	}

	@Override
	public Rect getBounds() {
		return bounds == null ? bounds = computeBounds() : bounds;
	}

	@Override
	public CurvePath apply(Transform t) {
		return z.apply(t).getPath();
	}

	@Override
	public Curve getGeometry() {
		return z;
	}

	// object methods

	@Override
	public int hashCode() {
		return z.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof CurvePath)) return false;
		CurvePath that = (CurvePath) obj;
		return this.z.equals(that.z);
	}

	@Override
	public String toString() {
		return z.toString();
	}

	// public abstract methods

	public Path simplifyCurve() {
		return Context.currentContext().simplify(this);
	}

	// protected accessors

	protected Rect computeBounds() {
		//TODO need an algorithm that can compute bounds from derivative?
		return linearize( new Bounder() ).getBounds();
	}

	// inner classes

	private static class ByLength extends Reparameterization.ByLength {

		private final Curve z;

		ByLength(Curve z) {
			super(z);
			this.z = z;
		}

		@Override
		public Path getPath() {
			return z.getPath();
		}

		@Override
		public float intrinsicAt(float s) {
			return z.lengthToIntrinsic(s);
		}

		@Override
		public float parameterNearest(Point pt) {
			return new Locator(z.getPath()).getNearestLengthAlongPath(pt);
		}

		@Override
		protected float map(float s) {
			return z.lengthToIntrinsic(s);
		}

		@Override
		protected float unmap(float t) {
			if (t <= 0f) return 0f;
			if (t >= 1f) return z.getPath().getLength();
			return z.intrinsicToLength(t);
		}

	}

}
