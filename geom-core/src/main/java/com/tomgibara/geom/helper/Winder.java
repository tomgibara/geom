package com.tomgibara.geom.helper;

import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;

public class Winder {

	private final Intersector intersector;
	private final Consumer consumer = new Consumer();

	public Winder() {
		intersector = new Intersector(consumer, true, true);
	}

	public int countWindings(Contour contour, Point pt) {
		if (contour == null) throw new IllegalArgumentException("null contour");
		if (pt == null) throw new IllegalArgumentException("null pt");
		Rect bounds = contour.getBounds();
		if (!bounds.containsPoint(pt)) return 0;
		Point nearest = bounds.nearestPointTo(pt, true);
		//TODO should this be zero or one, what convention?
		if (nearest.equals(pt)) return 1;
		//TODO this is hacky - theoretically, line could clip back into the contour
		LineSegment line = LineSegment.fromPoints(pt, nearest).scaleLength(1.001f);
		consumer.reset(line);
		intersector.intersect(contour.getPath(), line.getPath());
		return consumer.getWinding();
	}

	// TODO remove
	public Point.List test(Contour contour, Point pt) {
		if (contour == null) throw new IllegalArgumentException("null contour");
		if (pt == null) throw new IllegalArgumentException("null pt");
		Point.List list = new Point.List();
		Rect bounds = contour.getBounds();
		if (bounds.containsPoint(pt)) {
			LineSegment line = LineSegment.fromPoints(pt, bounds.nearestPointTo(pt, true)).scaleLength(1.1f);
			new Intersector(list).intersect(contour.getPath(), line.getPath());
		}
		return list;
	}

	private static class Consumer implements Point.Consumer<Void> {

		private static final int INC_X = 0;
		private static final int DEC_X = 1;
		private static final int INC_Y = 2;
		private static final int DEC_Y = 3;

		private int direction;
		private float value;

		private int count;
		private int winding;

		private float startX;
		private float startY;

		void reset(LineSegment line) {
			Point start = line.getStart();
			Point finish = line.getFinish();
			if (start.y == finish.y) {
				direction = start.x < finish.x ? INC_X : DEC_X;
			} else {
				direction = start.y < finish.y ? INC_Y : DEC_Y;
			}

			count = 0;
			winding = 0;
		}

		int getWinding() {
			return winding;
		}

		@Override
		public Void addPoint(float x, float y) {
			switch (count) {
			case 0 :
				startX = x;
				startY = y;
				count = 1;
				break;
			case 1 :
				count = 2;
				break;
				default:
					// TODO how to handle equality situations
					final int delta;
					switch (direction) {
					case INC_X :
						delta = y > startY ? 1 : -1;
						break;
					case DEC_X :
						delta = y < startY ? 1 : -1;
						break;
					case INC_Y :
						delta = x < startX ? 1 : -1;
						break;
					case DEC_Y :
						delta = x > startX ? 1 : -1;
						break;
						default:
							throw new UnsupportedOperationException();
					}
					winding += delta;
					count = 0;
			}
			return null;
		}

		@Override
		public Void addPoint(Point pt) {
			return addPoint(pt.x, pt.y);
		}

	}

}
