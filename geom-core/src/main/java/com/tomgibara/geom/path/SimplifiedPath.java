package com.tomgibara.geom.path;

import com.tomgibara.geom.core.LinearPath;

//TODO investigate alternatives to this class - possibly get it to implement compound path?
//TODO perhaps allow a third option, a point path
public class SimplifiedPath {

	private final LinearPath linear;
	private final SplitPath split;

	public SimplifiedPath(LinearPath linear) {
		if (linear == null) throw new IllegalArgumentException("null linear");
		this.linear = linear;
		this.split = null;
	}

	public SimplifiedPath(SplitPath split) {
		if (split == null) throw new IllegalArgumentException("null split");
		this.split = split;
		this.linear = null;
	}

	public boolean isLinear() {
		return linear != null;
	}

	public LinearPath getLinear() {
		return linear;
	}

	public SplitPath getSplit() {
		return split;
	}

}
