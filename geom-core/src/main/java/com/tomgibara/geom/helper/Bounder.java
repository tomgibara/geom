package com.tomgibara.geom.helper;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;

public final class Bounder implements Point.Consumer<Bounder> {

	private float minX = Float.POSITIVE_INFINITY;
	private float minY = Float.POSITIVE_INFINITY;
	private float maxX = Float.NEGATIVE_INFINITY;
	private float maxY = Float.NEGATIVE_INFINITY;

	@Override
	public Bounder addPoint(float x, float y) {
		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		maxX = Math.max(maxX, x);
		maxY = Math.max(maxY, y);
		return this;
	}

	@Override
	public Bounder addPoint(Point pt) {
		addPoint(pt.x, pt.y);
		return this;
	}

	public Rect getBounds() {
		if (maxX < minX) throw new IllegalStateException("no points added");
		//TODO shame we can't use direct constructor
		return Rect.atPoints(minX, minY, maxX, maxY);
	}
}