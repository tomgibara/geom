package com.tomgibara.geom.path;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.GeomUtil;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.curve.BezierCurve;
import com.tomgibara.geom.transform.Transform;

public class SequencePath implements CompositePath {

	public static class Builder implements Point.Consumer<Builder> {

		//TODO replace IGNORE with a builder constructor call which nulls policy
		public enum Policy {
			FAIL,
			JOIN,
			TRANSLATE,
			IGNORE
		}

		private Policy policy = Policy.FAIL;
		private final List<Path> paths = new ArrayList<>();
		private final Deque<Point> points = new ArrayDeque<>();
		private Collection<Point> remainingPoints = null;
		private Point endpoint = null;

		private Builder() { }

		public Builder withPolicy(Policy policy) {
			if (policy == null) throw new IllegalArgumentException("null policy");
			this.policy = policy;
			return this;
		}

		public boolean isEmpty() {
			return paths.isEmpty();
		}

		public Collection<Point> getRemainingPoints() {
			return remainingPoints == null ? remainingPoints = Collections.unmodifiableCollection(points) : remainingPoints;
		}

		public Point getEndpoint() {
			return endpoint;
		}

		public Builder addPath(Path path) {
			if (path == null) throw new IllegalArgumentException("null path");
			checkNoPoints();
			return safeAddPath(path);
		}

		Builder safeAddPath(Path path) {
			if (endpoint != null) {
				Point start = path.getStart();
				if (policy != Policy.IGNORE && !endpoint.equals(start)) {
					switch (policy) {
					case FAIL : throw new IllegalArgumentException("non-contiguous: " + endpoint + " != " + start);
					case JOIN : paths.add(LineSegment.fromPoints(endpoint, start).getPath()); break;
					case TRANSLATE : path = path.apply(endpoint.vectorFrom(start).asTranslation()); break;
					}
				}
			}
			paths.add(path);
			endpoint = path.getFinish();
			return this;
		}

		public Builder addPaths(Path... paths) {
			if (paths == null) throw new IllegalArgumentException("null paths");
			if (paths.length == 0) return this;
			for (int i = 0; i < paths.length; i++) {
				if (paths[i] == null) throw new IllegalArgumentException("null path");
			}
			checkNoPoints();
			return safeAddPaths(paths);
		}

		Builder safeAddPaths(Path... paths) {
			if (endpoint != null) {
				Point start = paths[0].getStart();
				if (policy != Policy.IGNORE && !endpoint.equals(start)) {
					switch (policy) {
					case FAIL : throw new IllegalArgumentException("non-contiguous: " + endpoint + " != " + start);
					case JOIN : this.paths.add(LineSegment.fromPoints(endpoint, start).getPath()); break;
					case TRANSLATE : endpoint.vectorFrom(start).asTranslation().transform(paths); break;
					}
				}
			}
			this.paths.addAll(Arrays.asList(paths));
			endpoint = paths[paths.length -1].getFinish();
			return this;
		}

		public Builder addPaths(List<Path> paths) {
			if (paths == null) throw new IllegalArgumentException("null paths");
			if (paths.isEmpty()) return this;
			if (paths.contains(null)) throw new IllegalArgumentException("null path");
			checkNoPoints();
			return safeAddPaths(paths);
		}

		Builder safeAddPaths(List<Path> paths) {
			if (endpoint != null) {
				Point start = paths.get(0).getStart();
				if (policy != Policy.IGNORE && !endpoint.equals(start)) {
					switch (policy) {
					case FAIL : throw new IllegalArgumentException("non-contiguous: " + endpoint + " != " + start);
					case JOIN : this.paths.add(LineSegment.fromPoints(endpoint, start).getPath()); break;
					//note: this is safe because Path overrides transform to guarantee that a path is returned
					case TRANSLATE : paths = (List<Path>) (List) endpoint.vectorFrom(start).asTranslation().transform(paths); break;
					}
				}
			}
			this.paths.addAll(paths);
			endpoint = paths.get(paths.size() - 1).getFinish();
			return this;
		}

		@Override
		public Builder addPoint(Point pt) {
			if (pt == null) throw new IllegalArgumentException("null pt");
			return safeAddPoint(pt);
		}

		@Override
		public Builder addPoint(float x, float y) {
			return safeAddPoint(new Point(x, y));
		}

		Builder safeAddPoint(Point pt) {
			points.add(pt);
			return this;
		}

		public Builder addPoints(Point... pts) {
			if (pts == null) throw new IllegalArgumentException("null pts");
			for (Point pt : pts) {
				if (pt == null) throw new IllegalArgumentException("null pt");
				safeAddPoints(pts);
			}
			return this;
		}

		Builder safeAddPoints(Point... pts) {
			points.addAll(Arrays.asList(pts));
			return this;
		}

		public Builder addPoints(List<Point> pts) {
			if (pts == null) throw new IllegalArgumentException("null pts");
			for (Point pt : pts) {
				if (pt == null) throw new IllegalArgumentException("null pt");
				safeAddPoints(pts);
			}
			return this;
		}

		Builder safeAddPoints(List<Point> pts) {
			points.addAll(pts);
			return this;
		}

		public Builder addPoints(float... coords) {
			return addPoints(coords, 0, coords.length);
		}

		public Builder addPoints(float[] coords, int offset, int length) {
			if (coords == null) throw new IllegalArgumentException("null coords");
			if (offset < 0) throw new IllegalArgumentException("negative offset");
			if (length < 0) throw new IllegalArgumentException("negative length");
			if (offset + length > coords.length) throw new IllegalArgumentException("offset + length exceeds number of coords");
			if ((length & 1) != 0) throw new IllegalArgumentException("odd length");

			for (int i = offset; i < offset + length; i+= 2) {
				safeAddPoint(new Point(coords[i], coords[i + 1]));
			}
			return this;
		}

		public Builder asLinearPath() {
			int count = points.size();
			if (endpoint != null) count++;
			if (count < 2) throw new IllegalStateException("insufficient points");
			Point start = endpoint == null ? points.remove() : endpoint;
			Point finish = points.remove();
			if (start.equals(finish)) {
				endpoint = start;
				return this;
			}
			return safeAddPath( LineSegment.fromPoints(start, finish).getPath() );
		}

		public Builder asLinearPaths() {
			int count = points.size();
			int limit = endpoint == null ? 2 : 1;
			if (count < limit) throw new IllegalStateException("insufficient points");
			Point prev = endpoint == null ? points.remove() : endpoint;
			while (!points.isEmpty()) {
				Point next = points.remove();
				if (!next.equals(prev)) safeAddPath( LineSegment.fromPoints(prev, next).getPath() );
				prev = next;
			}
			endpoint = prev;
			return this;
		}

		public Builder asPolygonalPath() {
			PolygonalPath.Builder builder = PolygonalPath.builder();
			if (endpoint != null) builder.addPoint(endpoint);
			while (!points.isEmpty()) {
				builder.addPoint(points.removeFirst());
			}
			return safeAddPath(builder.build());
		}

		public Builder asBezierCurve(int order) {
			if (order < 1) throw new IllegalArgumentException("non-positive order");
			int count = points.size();
			if (endpoint != null) count ++;
			if (count <= order) throw new IllegalStateException("insufficient points");
			return safeAsBezierCurve(order);
		}

		public Builder asBezierCurve() {
			int order = points.size();
			if (endpoint == null) order --;
			if (order == 0) throw new IllegalStateException("no points");
			return safeAsBezierCurve(order);

		}

		public SequencePath closeAndBuild() {
			return build(true);
		}

		public SequencePath build() {
			return build(false);
		}

		private void checkNoPoints() {
			if (!points.isEmpty()) throw new IllegalStateException("unconverted points");
		}

		private Builder safeAsBezierCurve(int order) {
			if (order == 1) {
				// we're not going to add a zero length segment
				// so just remove the point if necessary and return
				if (endpoint == null) {
					endpoint = points.removeFirst();
				}
				return this;
			}
			ArrayList<Point> pts = new ArrayList<>(order + 1);
			Point first;
			if (endpoint == null) {
				first = points.removeFirst();
			} else {
				first = endpoint;
			}
			pts.add(first);
			boolean allSame = true;
			while (pts.size() <= order) {
				Point next = points.removeFirst();
				pts.add(next);
				allSame = allSame && first.equals(next);
			}
			if (allSame) {
				if (endpoint == null) {
					endpoint = first;
				}
				return this;
			}
			return safeAddPath(BezierCurve.fromPoints(pts).getPath());
		}

		private SequencePath build(boolean closed) {
			checkNoPoints();
			if (endpoint == null) throw new IllegalStateException("no paths");
			// note: endpoint may not be null, but there could still be no paths because everything was zero length
			if (paths.isEmpty()) {
				//TODO can we do anything better here?
				paths.add(PointPath.from(endpoint, Vector.UNIT_X));
			} else if (closed) {
				Point start = paths.get(0).getStart();
				if (!start.equals(endpoint)) {
					safeAddPath(LineSegment.fromPoints(endpoint, start).getPath());
				}
			}
			Path[] array = (Path[]) paths.toArray(new Path[paths.size()]);
			SequencePath path = new SequencePath(closed, -1f, array);
			return path;
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	private final Path[] paths;
	private final boolean closed;
	private List<Path> publicPaths = null;
	private Parameterizations params = null;
	private float length;
	private Boolean rectilinear;

	public SequencePath(boolean closed, List<? extends Path> paths) {
		this(closed, GeomUtil.asArray(paths));
	}

	private SequencePath(boolean closed, Path... array) {
		if (array.length == 0) throw new IllegalArgumentException("empty path");
		Point finish = null;
		for (Path path : array) {
			if (path == null) {
				throw new IllegalArgumentException("null path");
			}
			if (finish != null && !path.getStart().equals(finish)) {
				throw new IllegalArgumentException("disjoint path " + path.getStart() + " " + finish);
			}
			finish = path.getFinish();
		}
		this.paths = array;
		this.closed = closed;
		this.length = -1f;
	}

	private SequencePath(boolean closed, float length, Path... paths) {
		this.paths = paths;
		this.closed = closed;
		this.length = length;
	}

	@Override
	public Point getStart() {
		return paths[0].getStart();
	}

	@Override
	public Point getFinish() {
		return paths[paths.length - 1].getFinish();
	}

	@Override
	public float getLength() {
		return length == -1 ? length = computeLength() : length;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean isRectilinear() {
		if (rectilinear == null) rectilinear = computeRectilinear();
		return rectilinear;
	}

	@Override
	public boolean isSmooth() {
		for (Path path : paths) {
			if (!path.isSmooth()) return false;
		}
		return byIntrinsic().getCorners().isEmpty();
	}

	@Override
	public Parameterization.ByIntrinsic byIntrinsic() {
		return getParams().byIntrinsic;
	}

	@Override
	public Parameterization.ByLength byLength() {
		return getParams().byLength;
	}

	@Override
	public <C> C linearize(Point.Consumer<C> consumer) {
		SkipOneConsumer<C> soc = null;
		C c = null;
		for (Path path : paths) {
			if (soc == null) {
				c = path.linearize(consumer);
				soc = new SkipOneConsumer<>(consumer);
			} else {
				soc.skipNext();
				c = path.linearize(consumer);
			}
		}
		return c;
	}

	@Override
	public SimplifiedPath simplify() {
		int length = paths.length;
		if (length == 1) return paths[0].simplify();
		int i = length / 2;
		SequencePath p1 = new SequencePath(false, -1f, Arrays.copyOfRange(paths, 0, i));
		SequencePath p2 = new SequencePath(false, -1f, Arrays.copyOfRange(paths, i, paths.length));
		return new SimplifiedPath(new SplitPath(p1, p2, closed));
	}

	@Override
	public SequencePath getReverse() {
		Path[] paths = this.paths.clone();
		int i = 0;
		int j = paths.length - 1;
		while (j > i) {
			Path t = paths[i];
			paths[i++] = paths[j].getReverse();
			paths[j--] = t.getReverse();
		}
		return new SequencePath(closed, length, paths);
	}

	@Override
	public SequencePath splitAtCorners() {
		List<Path> list = new ArrayList<>();
		Context context = Context.currentContext();
		// possible corner at start of closed path
		boolean joinFirstAndLast = false;
		if (closed) {
			Vector v1 = paths[paths.length - 1].byIntrinsic().tangentAt(1f);
			Vector v2 = paths[0].byIntrinsic().tangentAt(0f);
			joinFirstAndLast = context.isCorner(v1, v2);
		}
		List<Path> acc = new ArrayList<>();
		Path p1 = paths[0];
		for (int i = 0; i < paths.length; i++) {
			// p1 may contain corners
			List<? extends Path> smoothPaths = p1.splitAtCorners().getSubpaths();
			int smoothPathCount = smoothPaths.size();
			if (smoothPathCount == 1) { // p1 is smooth
				acc.add(p1);
			} else { // p1 is not smooth but does not need combining with anything before it
				// flush the accumulated paths
				if (acc.isEmpty()) {
					list.add(smoothPaths.get(0)); // we know this is followed by corner
				} else { // p1 is not smooth, attach first to previous and continue
					acc.add(smoothPaths.get(0));
					list.add(new SequencePath(false, acc));
					acc.clear();
				}
				// accumulate remaining subpaths
				for (int j = 1; j < smoothPathCount; j++) {
					acc.add(smoothPaths.get(j));
				}
			}
			// note: acc can never be empty at this point
			// p1 may be followed by corner
			if (i < paths.length - 1) {
				Path p2 = paths[i + 1];
				Vector v1 = p1.byIntrinsic().tangentAt(1f);
				Vector v2 = p2.byIntrinsic().tangentAt(0f);
				boolean smooth = !context.isCorner(v1, v2);
				if (!smooth) { // flush accumulated paths we know the end in a corner
					list.add(new SequencePath(false, acc));
					acc.clear();
				}
				p1 = p2;
			}
		}
		// acc will contain last sequence of paths
		if (joinFirstAndLast) {
			// special case, entirely smooth
			if (list.isEmpty()) {
				list.add(this);
			} else {
				Path first = list.get(0);
				// try to avoid nesting sequence paths unecessarily
				if (first instanceof SequencePath) {
					Path[] smoothPaths = ((SequencePath) first).paths;
					for (int i = 0; i < smoothPaths.length; i++) {
						acc.add(smoothPaths[i]);
					}
				} else {
					acc.add(first);
				}
				list.add(new SequencePath(false, acc));
			}
		} else {
			list.add(new SequencePath(false, acc));
		}
		return new SequencePath(closed, list);
	}

	public List<Path> getSubpaths() {
		return publicPaths == null ? publicPaths = Collections.unmodifiableList(Arrays.asList(paths)) : publicPaths;
	}

	@Override
	public int getSubpathCount() {
		return paths.length;
	}

	@Override
	public Path getFirstPath() {
		return paths[0];
	}

	@Override
	public Path getLastPath() {
		return paths[paths.length - 1];
	}

	@Override
	public Path.Location locateAtLength(float p) {
		return getParams().locateAtLength(p);
	}

	@Override
	public Rect getBounds() {
		Rect bounds = paths[0].getBounds();
		for (int i = 1; i < paths.length; i++) {
			bounds = Rect.unionRect(bounds, paths[i].getBounds());
		}
		return bounds;
	}

	@Override
	public Path apply(Transform t) {
		List<Path> list = new ArrayList<>();
		Path tPrev = null;
		for (Path path : paths) {
			Path tPath = path.apply(t);
			if (tPrev != null) {
				Point start = tPath.getStart();
				Point finish = tPrev.getFinish();
				if (!start.equals(finish)) {
					list.add(LineSegment.fromPoints(start, finish).getPath());
				}
			}
			list.add(tPath);
			tPrev = tPath;
		}
		Path[] array = (Path[]) list.toArray(new Path[list.size()]);
		return new SequencePath(closed, t.isScalePreserving() ? length : -1f, array);
	}

	@Override
	public PathTraces getGeometry() {
		return new PathTraces(this, paths);
	}

	private Parameterizations getParams() {
		return params == null ? params = new Parameterizations(this, getSubpaths()) : params;
	}

	private float computeLength() {
		float sum = 0f;
		for (Path path : paths) {
			sum += path.getLength();
		}
		return sum;
	}

	private boolean computeRectilinear() {
		for (Path path : paths) {
			if (!path.isRectilinear()) return false;
		}
		return true;
	}

	private static class SkipOneConsumer<C> implements Point.Consumer<C> {

		private final Point.Consumer<C> consumer;
		private boolean skipNext = false;

		SkipOneConsumer(Point.Consumer<C> consumer) {
			this.consumer = consumer;
		}

		void skipNext() {
			this.skipNext = true;
		}

		@Override
		public C addPoint(float x, float y) {
			if (skipNext) {
				skipNext = false;
				return null;
			} else {
				return consumer.addPoint(x, y);
			}
		}

		@Override
		public C addPoint(Point pt) {
			if (skipNext) {
				skipNext = false;
				return null;
			} else {
				return consumer.addPoint(pt);
			}
		}

	}

}
