package com.tomgibara.geom.path;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.helper.Bounder;

//TODO eliminate and return RectChecker to CurvePath
public abstract class AbstractPath implements Path {

	// static inner classes

	private final static class RectChecker implements Point.Consumer<Void> {
		private double x = Double.NaN;
		private double y = Double.NaN;
		boolean rect = true;

		@Override
		public Void addPoint(double x, double y) {
			if (rect) {
				if (x == this.x) {
					this.y = y;
				} else if (y == this.y) {
					this.x = x;
				} else if (x == Double.NaN) {
					this.x = x;
					this.y = y;
				} else {
					rect = false;
				}
			}
			return null;
		}

		@Override
		public Void addPoint(Point pt) {
			return addPoint(pt.x, pt.y);
		}
	}

	// default implementations

	@Override
	public Point getStart() {
		return byIntrinsic().pointAt(0.0);
	}

	@Override
	public Point getFinish() {
		return byIntrinsic().pointAt(1.0);
	}

	@Override
	public double getLength() {
		return byIntrinsic().lengthAt(1.0);
	}

	@Override
	public Rect getBounds() {
		return linearize( new Bounder() ).getBounds();
	}

	@Override
	//TODO is there a way we can halt linearization early?
	public boolean isRectilinear() {
		RectChecker checker = new RectChecker();
		linearize(checker);
		return checker.rect;
	}

}
