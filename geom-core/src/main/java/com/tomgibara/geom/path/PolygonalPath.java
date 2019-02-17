package com.tomgibara.geom.path;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.GeomUtil;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.helper.Bounder;
import com.tomgibara.geom.transform.Transform;

public class PolygonalPath implements Path {

	public static class Builder implements Point.Consumer<Builder> {

		private List<Point> points = new ArrayList<>();
		private Point last = null;
		private boolean closed = false;
		private boolean rectilinear = true;

		private Builder() { }

		@Override
		public Builder addPoint(float x, float y) {
			return addPoint(new Point(x, y));
		}

		@Override
		public Builder addPoint(Point pt) {
			if (pt == null) throw new IllegalArgumentException("null pt");
			addPointImpl(pt);
			return this;
		}

		public Builder addPath(Path path) {
			if (path == null) throw new IllegalArgumentException("null path");
			path.linearize(this);
			return this;
		}

		public Builder addPoints(List<Point> points) {
			for (Point point : points) {
				addPoint(point);
			}
			return this;
		}

		public Builder addPoints(Point... points) {
			for (Point point : points) {
				addPoint(point);
			}
			return this;
		}

		public int pointCount() {
			return points.size();
		}

		public PolygonalPath closeAndBuild() {
			checkNotTrivial();
			Point first = points.get(0);
			if (!first.equals(last)) {
				addPointImpl(first);
			}
			closed = true;
			return build();
		}

		public PolygonalPath build() {
			checkNotTrivial();
			return new PolygonalPath( (Point[]) points.toArray(new Point[points.size()]), closed, rectilinear );
		}

//		public Path buildPossiblePointPath() {
//			switch (points.size()) {
//			case 0 : throw new IllegalStateException("no points");
//			case 1 : return PointPath.from(points.get(0), null);
//			default : return new PolygonalPath( (Point[]) points.toArray(new Point[points.size()]), closed, rectilinear );
//			}
//		}

		private void addPointImpl(Point pt) {
			if (last != null && last.equals(pt)) {
				return;
			}

			points.add(pt);
			if (rectilinear && last != null && last.x != pt.x && last.y != pt.y) {
					rectilinear = false;
			}
			last = pt;
		}

		private void checkNotTrivial() {
			if (points.size() < 2) throw new IllegalStateException("fewer than two points");
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	private final Point[] points; // at least length 2
	private final boolean closed;
	private final boolean rectilinear;
	private final List<Point> publicPoints;
	private Rect bounds = null;
	private List<LineSegment> segments = null;
	private float length = -1;
	private Parameterizations params = null;
	private CompositePath cornerSplit = null;

	private PolygonalPath(Point[] points, boolean closed, boolean rectilinear) {
		this.points = points;
		this.closed = closed;
		this.rectilinear = rectilinear;
		publicPoints = GeomUtil.asList(points);
	}

	public List<Point> getPoints() {
		return publicPoints;
	}

	public List<LineSegment> getSegments() {
		return segments == null ? segments = new SegmentList() : segments;
	}

	@Override
	public Point getStart() {
		return points[0];
	}

	@Override
	public Point getFinish() {
		return points[points.length - 1];
	}

	@Override
	public float getLength() {
		return length < 0 ? length = computeLength() : length;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean isRectilinear() {
		return rectilinear;
	}

	@Override
	public boolean isSmooth() {
		// easy case, straight line
		if (points.length == 2) return true;
		// cannot be closed without a corner
		if (isClosed()) return false;
		Vector t = null;
		Context context = Context.currentContext();
		for (LineSegment segment : getSegments()) {
			if (t == null) {
				t = segment.getTangent();
			} else {
				Vector s = segment.getTangent();
				if (context.isCorner(s, t)) return false;
				t = s;
			}
		}
		return true;
	}

	@Override
	public <C> C linearize(Point.Consumer<C> consumer) {
		C c = null;
		for (Point point : points) {
			c = consumer.addPoint(point);
		}
		return c;
	}

	@Override
	public SimplifiedPath simplify() {
	//case 2 : return new SimplifiedPath( getSegments().get(0) );
		int length = points.length;
		if (length == 2) return new SimplifiedPath( getSegments().get(0).getPath() );

		int i = length / 2;
		//TODO can we supply precomputed segments?
		Path p1 = i == 1 ? getSegments().get(0).getPath() : new PolygonalPath(Arrays.copyOfRange(points, 0, i + 1), false, rectilinear);
		Path p2 = length - i == 2 ? getSegments().get(i).getPath() : new PolygonalPath(Arrays.copyOfRange(points, i, length), false, rectilinear);
		return new SimplifiedPath(new SplitPath(p1, p2, closed));
	}

	@Override
	public Parameterization.ByIntrinsic byIntrinsic() {
		return getParams().getByIntrinsic();
	}

	@Override
	public Parameterization.ByLength byLength() {
		return getParams().getByLength();
	}

	@Override
	public PolygonalPath getReverse() {
		Point[] pts = points.clone();
		GeomUtil.reverseArray(pts);
		return new PolygonalPath(pts, closed, rectilinear);
	}

	@Override
	public CompositePath splitAtCorners() {
		if (cornerSplit == null) {
			// easy case, straight line
			if (points.length == 2) {
				cornerSplit = new SingletonPath(this);
			} else {
				Context context = Context.currentContext();
				List<Path> list = new ArrayList<>();
				//TODO should deal with closed paths
				List<LineSegment> segments = getSegments();
				int count = segments.size();
				LineSegment acc = segments.get(0);
				Vector t = acc.getTangent();
				for (int i = 1; i < count; i++) {
					LineSegment linear = segments.get(i);
					Vector s = linear.getTangent();
					if (context.isCorner(s, t)) { // smooth, accumulate
						acc = LineSegment.fromPoints(acc.getStart(), linear.getFinish());
					} else { // corner, split
						list.add(acc.getPath());
						acc = linear;
					}
					t = s;
				}
				list.add(acc.getPath());
				cornerSplit = new SequencePath(closed, list);
			}
		}
		return cornerSplit;
	}

	@Override
	public Rect getBounds() {
		return bounds == null ? bounds = computeBounds() : bounds;
	}

	@Override
	public PolygonalPath apply(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		if (t.isIdentity()) return this;
		Point[] points = this.points.clone();
		t.transform(points);
		return new PolygonalPath(points, closed, rectilinear && t.isRectilinearPreserving());
	}

	@Override
	public PathTraces getGeometry() {
		return new PathTraces(this, getSegments(), bounds);
	}

	@Override
	public String toString() {
		return publicPoints.toString();
	}

	private Rect computeBounds() {
		Bounder consumer = new Bounder();
		linearize(consumer);
		return consumer.getBounds();
	}

	private float computeLength() {
		float length = 0f;
		List<LineSegment> segments = getSegments();
		int size = segments.size();
		for (int i = 0; i < size; i++) {
			length += segments.get(i).getPath().getLength();
		}
		return length;
	}

	private class SegmentList extends AbstractList<LineSegment> {

		private LineSegment[] segments = new LineSegment[points.length - 1];

		@Override
		public LineSegment get(int index) {
			if (index < 0 || index >= segments.length) throw new NoSuchElementException("no linear path at index " + index);
			LineSegment segment = segments[index];
			if (segment == null) {
				segment = LineSegment.fromPoints(points[index], points[index + 1]);
				segments[index] = segment;
			}
			return segment;
		}

		@Override
		public int size() {
			return segments.length;
		}

	}

	private Parameterizations getParams() {
		if (params == null) {
			List<Path> adapted = new AbstractList<Path>() {
				List<LineSegment> segments = getSegments();
				@Override public Path get(int index) { return segments.get(index).getPath(); }
				@Override public int size() { return segments.size(); }
			};
			params = new Parameterizations(this, adapted);
		}
		return params;
	}

}
