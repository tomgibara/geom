package com.tomgibara.geom.contour;

import com.tomgibara.geom.core.Geometric;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.SplitPath;
import com.tomgibara.geom.transform.Transform;

public class PathContour implements Contour {

	private final Path path;

	public PathContour(Path path) {
		if (path == null) throw new IllegalArgumentException("null path");
		if (!path.isClosed()) throw new IllegalArgumentException("path not closed");
		this.path = path;
	}

	@Override
	public Rect getBounds() {
		return path.getBounds();
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public Path getPathStartingAt(Point pt) {
		if (pt == null) throw new IllegalArgumentException("null pt");
		if (pt.equals(path.getStart())) return path;
		// TODO could optimize for most built-in path types;
		SplitPath split = path.byIntrinsic().location().moveClosestTo(pt).split();
		return new SplitPath(split.getLastPath(), split.getFirstPath(), split.isClosed());
	}

	@Override
	public Contour getReverse() {
		return new PathContour(path.getReverse());
	}

	@Override
	public PathContour apply(Transform t) {
		return t.isIdentity() ? this : new PathContour(path.apply(t));
	}

	@Override
	public Geometric getGeometry() {
		return path.getGeometry();
	}

}
