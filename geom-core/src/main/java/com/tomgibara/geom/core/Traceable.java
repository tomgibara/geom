package com.tomgibara.geom.core;

import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.transform.Transform;

public interface Traceable extends Geometric {

	Path getPath();

	@Override
	Traceable apply(Transform transform);
}
