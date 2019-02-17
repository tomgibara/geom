package com.tomgibara.geom.curve;

import com.tomgibara.geom.path.SplitPath;

public class SplitCurvePath extends SplitPath {

	public SplitCurvePath(CurvePath p1, CurvePath p2, boolean closed) {
		super(p1, p2, closed);
	}

	@Override
	public CurvePath getFirstPath() {
		return (CurvePath) super.getFirstPath();
	}

	@Override
	public CurvePath getLastPath() {
		return (CurvePath) super.getLastPath();
	}

}
