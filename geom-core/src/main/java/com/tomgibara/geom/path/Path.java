package com.tomgibara.geom.path;

import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.Geometric;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Traceable;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public interface Path extends Geometric {

	// more specific return type for geometric defined method

	@Override
	Path apply(Transform t);

	// core properties which are available without recourse to parameterization

	Point getStart();

	Point getFinish();

	float getLength();

	boolean isRectilinear();

	boolean isSmooth();

	boolean isClosed();

	// methods relating to parameterization

	Parameterization.ByIntrinsic byIntrinsic();

	Parameterization.ByLength byLength();

	// methods for decomposing a path

	<K> K linearize(Point.Consumer<K> consumer);

	// used for intersection testing
	SimplifiedPath simplify();

	CompositePath splitAtCorners();

	// additional methods

	Path getReverse();

	Traceable getGeometry();

	// inner classes

	public final class Corner {

		public static final List<Corner> NO_CORNERS = Collections.emptyList();

		private final Parameterization parameterization;
		private final float parameter;
		private final Point point;
		private final Vector startTangent;
		private final Vector finishTangent;

		public Corner(Parameterization parameterization, float parameter, Point point, Vector startTangent, Vector finishTangent) {
			if (parameterization == null) throw new IllegalArgumentException("null parameterization");
			if (point == null) throw new IllegalArgumentException("null point");
			if (startTangent == null) throw new IllegalArgumentException("null startTangent");
			if (finishTangent == null) throw new IllegalArgumentException("null finishTangent");
			this.parameterization = parameterization;
			//TODO find way to clamp parameter? worthwhile?
			this.parameter = parameter;
			this.point = point;
			this.startTangent = startTangent;
			this.finishTangent = finishTangent;
		}

		public Parameterization getParameterization() {
			return parameterization;
		}

		public float getParameter() {
			return parameter;
		}

		public Point getPoint() {
			return point;
		}

		public Vector getStartTangent() {
			return startTangent;
		}

		public Vector getFinishTangent() {
			return finishTangent;
		}

		public Corner reparameterize(Parameterization parameterization, float parameter) {
			return new Corner(parameterization, parameter, point, startTangent, finishTangent);
		}

		@Override
		public int hashCode() {
			return parameterization.hashCode() ^ Float.floatToIntBits(parameter) ^ point.hashCode() ^ startTangent.hashCode() ^ finishTangent.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Corner)) return false;
			Corner that = (Corner) obj;
			if (this.parameter != that.parameter) return false;
			if (!this.parameterization.equals(that.parameterization)) return false;
			if (!this.point.equals(that.point)) return false;
			if (!this.startTangent.equals(that.startTangent)) return false;
			if (!this.finishTangent.equals(that.finishTangent)) return false;
			return true;
		}

		@Override
		public String toString() {
			return "corner at " + parameter;
		}

	}

	public final class Location {

		private final Parameterization parameterization;
		private final boolean byLength;
		private final boolean byIntrinsic;

		private float parameter;
		private List<Corner> corners = null;

		private Point point = null;
		private Vector tangent = null;
		private PointPath pointTangent = null;
		private float otherParameter = Float.NaN;

		//TODO would like a safe version of this so that clamp isn't needed (since it could be costly)
		public Location(Parameterization parameterization, float parameter) {
			if (parameterization == null) throw new IllegalArgumentException("null parameterization");
			byLength = parameterization instanceof Parameterization.ByLength;
			byIntrinsic = parameterization instanceof  Parameterization.ByIntrinsic;
			if (!byLength && !byIntrinsic) throw new IllegalArgumentException("invalid parameterization");
			this.parameterization = parameterization;
			this.parameter = clamp(parameter);
		}

		private Location(Location that) {
			this.parameterization = that.parameterization;
			this.parameter = that.parameter;
			this.byLength = that.byLength;
			this.byIntrinsic = that.byIntrinsic;
			this.corners = that.corners;
			this.point = that.point;
			this.tangent = that.tangent;
			this.pointTangent = that.pointTangent;
			this.otherParameter = that.otherParameter;
		}

		public Parameterization getParameterization() {
			return parameterization;
		}

		public float getParameter() {
			return parameter;
		}

		public boolean isByLength() {
			return byLength;
		}

		public boolean isByIntrinsic() {
			return byIntrinsic;
		}

		public boolean isAtFinish() {
			return parameter == getMaximum();
		}

		public boolean isAtCorner() {
			List<Corner> corners = getCorners();
			int count = corners.size();
			if (count == 0) return false;
			for (int i = 0; i < count; i++) {
				if (corners.get(i).getParameter() == parameter) return true;
			}
			return false;
		}

		public Point getPoint() {
			return point == null ? point = parameterization.pointAt(parameter) : point;
		}

		public Vector getTangent() {
			return tangent == null ? tangent = parameterization.tangentAt(parameter) : tangent;
		}

		public PointPath getPointTangent() {
			return pointTangent == null ? pointTangent = parameterization.pointTangentAt(parameter) : pointTangent;
		}

		public Corner getCorner() {
			List<Corner> corners = getCorners();
			int count = corners.size();
			for (int i = 0; i < count; i++) {
				Corner corner = corners.get(i);
				if (corner.getParameter() == parameter) return corner;
			}
			return null;
		}

		public SplitPath split() {
			return parameterization.splitAt(parameter);
		}

		public float getLength() {
			if (byLength) return parameter;
			if (Float.isNaN(otherParameter)) {
				otherParameter = ((Parameterization.ByIntrinsic) parameterization).lengthAt(parameter);
			}
			return otherParameter;
		}

		public float getIntrinsic() {
			if (byIntrinsic) return parameter;
			if (Float.isNaN(otherParameter)) {
				otherParameter = ((Parameterization.ByLength) parameterization).intrinsicAt(parameter);
			}
			return otherParameter;
		}

		public Location moveToStart() {
			if (parameter != 0f) {
				parameter = 0f;
				clear();
			}
			return this;
		}

		public Location moveToFinish() {
			float max = getMaximum();
			if (parameter != max) {
				parameter = max;
				clear();
			}
			return this;
		}

		public Location moveTo(float parameter) {
			parameter = clamp(parameter);
			if (parameter != this.parameter) {
				this.parameter = parameter;
				clear();
			}
			return this;
		}

		public Location moveBy(float delta) {
			float parameter = clamp(this.parameter + delta);
			if (parameter != this.parameter) {
				this.parameter = parameter;
				clear();
			}
			return this;
		}

		public Location moveClosestTo(Point pt) {
			if (pt == null) throw new IllegalArgumentException("null pt");
			return moveTo(parameterization.parameterNearest(pt));
		}

		public Location moveTo(Location location) {
			if (location == null) throw new IllegalArgumentException("null location");
			Parameterization z = location.getParameterization();
			if (z.getPath() != parameterization.getPath()) throw new IllegalArgumentException("mismatched path");
			float p = location.getParameter();
			float q;
			if (byLength && location.byLength || byIntrinsic && location.byIntrinsic) {
				q = p;
			} else if (location.byIntrinsic) {
				q = ((Parameterization.ByIntrinsic) z).lengthAt(p);
			} else {
				q = ((Parameterization.ByLength) z).intrinsicAt(p);
			}
			return moveTo(q);
		}

		public Location moveToNextCorner() {
			List<Corner> corners = getCorners();
			int count = corners.size();
			for (int i = 0; i < count; i++) {
				float p = corners.get(i).getParameter();
				if (p > parameter) return moveTo(p);
			}
			return moveToFinish();
		}

		public Location moveToPreviousCorner() {
			List<Corner> corners = getCorners();
			for (int i = corners.size() - 1; i >= 0; i++) {
				float p = corners.get(i).getParameter();
				if (p < parameter) return moveTo(p);
			}
			return moveToStart();
		}

		public Location copy() {
			return new Location(this);
		}

		@Override
		public int hashCode() {
			return parameterization.hashCode() ^ Float.floatToIntBits(parameter);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Location)) return false;
			Location that = (Location) obj;
			if (this.parameter != that.parameter) return false;
			if (!this.parameterization.equals(that.parameterization)) return false;
			return true;
		}

		@Override
		public String toString() {
			return parameter + " @ " + parameterization;
		}

		private void clear() {
			point = null;
			tangent = null;
			pointTangent = null;
			otherParameter = Float.NaN;
		}

		private float getMaximum() {
			return byIntrinsic ? 1f : parameterization.getPath().getLength();
		}

		private float clamp(float p) {
			if (p <= 0f) return 0f; // avoid getting maximum length until necessary
			float max = getMaximum();
			return p >= max ? max : p;
		}

		private List<Corner> getCorners() {
			return corners == null ? parameterization.getCorners() : corners;
		}

	}

}
