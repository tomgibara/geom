package com.tomgibara.geom.path;

import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Point.Consumer;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Traceable;
import com.tomgibara.geom.path.Parameterization.ByIntrinsic;
import com.tomgibara.geom.path.Parameterization.ByLength;
import com.tomgibara.geom.transform.Transform;

public final class SingletonPath implements CompositePath {

	private final Path path;

	public SingletonPath(Path path) {
		if (path == null) throw new IllegalArgumentException("null path");
		this.path = path;
	}

	@Override
	public Point getStart() {
		return path.getStart();
	}

	@Override
	public Point getFinish() {
		return path.getFinish();
	}

	@Override
	public double getLength() {
		return path.getLength();
	}

	@Override
	public boolean isRectilinear() {
		return path.isRectilinear();
	}

	@Override
	public boolean isSmooth() {
		return path.isSmooth();
	}

	@Override
	public boolean isClosed() {
		return path.isClosed();
	}

	@Override
	public ByIntrinsic byIntrinsic() {
		//TODO should wrap
		return path.byIntrinsic();
	}

	@Override
	public ByLength byLength() {
		//TODO should wrap
		return path.byLength();
	}

	@Override
	public <K> K linearize(Consumer<K> consumer) {
		return path.linearize(consumer);
	}

	@Override
	public SimplifiedPath simplify() {
		return path.simplify();
	}

	@Override
	public CompositePath splitAtCorners() {
		return path.splitAtCorners();
	}

	@Override
	public Path getReverse() {
		return path.getReverse();
	}

	@Override
	public Rect getBounds() {
		return path.getBounds();
	}

	@Override
	public Path apply(Transform t) {
		return path.apply(t);
	}

	@Override
	public Traceable getGeometry() {
		return path.getGeometry();
	}

	@Override
	public int getSubpathCount() {
		return 1;
	}

	@Override
	public Path getFirstPath() {
		return path;
	}

	@Override
	public Path getLastPath() {
		return path;
	}

	@Override
	public List<? extends Path> getSubpaths() {
		return Collections.singletonList(path);
	}

	@Override
	public Path.Location locateAtLength(double p) {
		return new Path.Location(path.byLength(), p);
	}

}
