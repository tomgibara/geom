package com.tomgibara.geom.path;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public class RectPath implements Path {

	public static RectPath fromRect(Rect rect, boolean positiveRotation) {
		if (rect == null) throw new IllegalArgumentException("null rect");
		return new RectPath(rect, positiveRotation);
	}

	private final boolean positiveRotation;
	private final Rect rect;
	private final Point start;
	private PathTraces geometry = null;

	private RectPath(Rect rect, boolean positiveRotation) {
		this.rect = rect;
		this.positiveRotation = positiveRotation;
		start = new Point(rect.minX, rect.minY);
	}

	public boolean isPositiveRotation() {
		return positiveRotation;
	}

	@Override
	public Rect getBounds() {
		return rect;
	}

	@Override
	public Path apply(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		if (t.isRectilinearPreserving()) {
			return new RectPath(t.transform(rect), !t.isChiralPreserving() ^ positiveRotation);
		}
		PolygonalPath.Builder builder = PolygonalPath.builder()
				.addPoint(t.transform(rect.minX, rect.minY));
		if (positiveRotation) {
			builder
				.addPoint(t.transform(rect.minX, rect.maxY))
				.addPoint(t.transform(rect.maxX, rect.maxY))
				.addPoint(t.transform(rect.maxX, rect.minY));
		} else {
			builder
				.addPoint(t.transform(rect.maxX, rect.minY))
				.addPoint(t.transform(rect.maxX, rect.maxY))
				.addPoint(t.transform(rect.minX, rect.maxY));
		}
		return builder.closeAndBuild();
	}

	@Override
	public PathTraces getGeometry() {
		if (geometry == null) {
			LineSegment[] segments = new LineSegment[4];
			boolean nonZeroWidth = rect.getWidth() != 0.0;
			boolean nonZeroHeight = rect.getHeight() != 0.0;
			if (positiveRotation) {
				segments[0] = nonZeroWidth  ? LineSegment.fromCoords(rect.minX, rect.minY, rect.maxX, rect.maxY) : LineSegment.fromCoords(rect.minX, rect.minY, Vector.UNIT_X);
				segments[1] = nonZeroHeight ? LineSegment.fromCoords(rect.maxX, rect.minY, rect.maxX, rect.maxY) : LineSegment.fromCoords(rect.maxX, rect.minY, Vector.UNIT_Y);
				segments[2] = nonZeroWidth  ? LineSegment.fromCoords(rect.maxX, rect.maxY, rect.minX, rect.maxY) : LineSegment.fromCoords(rect.maxX, rect.maxY, Vector.UNIT_NEG_X);
				segments[3] = nonZeroHeight ? LineSegment.fromCoords(rect.minX, rect.maxY, rect.minX, rect.minY) : LineSegment.fromCoords(rect.minX, rect.maxY, Vector.UNIT_NEG_Y);
			} else {
				segments[0] = nonZeroHeight ? LineSegment.fromCoords(rect.minX, rect.minY, rect.minX, rect.maxY) : LineSegment.fromCoords(rect.minX, rect.minY, Vector.UNIT_Y);
				segments[1] = nonZeroWidth  ? LineSegment.fromCoords(rect.minX, rect.maxY, rect.maxX, rect.maxY) : LineSegment.fromCoords(rect.minX, rect.maxY, Vector.UNIT_X);
				segments[2] = nonZeroHeight ? LineSegment.fromCoords(rect.maxX, rect.maxY, rect.maxX, rect.minY) : LineSegment.fromCoords(rect.maxX, rect.maxY, Vector.UNIT_NEG_Y);
				segments[3] = nonZeroWidth  ? LineSegment.fromCoords(rect.maxX, rect.minY, rect.minX, rect.minY) : LineSegment.fromCoords(rect.maxX, rect.minY, Vector.UNIT_NEG_X);
			}
			geometry = new PathTraces(this, segments);
		}
		return geometry;
	}

	@Override
	public Point getStart() {
		return start;
	}

	@Override
	public Point getFinish() {
		return start;
	}

	@Override
	public double getLength() {
		return rect.getPerimeterLength();
	}

	@Override
	public boolean isRectilinear() {
		return true;
	}

	@Override
	public boolean isSmooth() {
		return false;
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	//TODO cache?
	public Parameterization.ByIntrinsic byIntrinsic() {
		return new ByIntrinsic();
	}

	@Override
	//TODO cache?
	public Parameterization.ByLength byLength() {
		return new ByLength();
	}

	@Override
	public <C> C linearize(Point.Consumer<C> consumer) {
		if (consumer == null) throw new IllegalArgumentException("null consumer");
		C c = consumer.addPoint(start);
		boolean nonZeroWidth = rect.getWidth() != 0.0;
		boolean nonZeroHeight = rect.getHeight() != 0.0;
		if (!nonZeroWidth & !nonZeroHeight) return c;
		if (positiveRotation) {
			if (nonZeroWidth)  c = consumer.addPoint(rect.maxX, rect.minY);
			if (nonZeroHeight) c = consumer.addPoint(rect.maxX, rect.maxY);
			if (nonZeroWidth)  c = consumer.addPoint(rect.minX, rect.maxY);
			if (nonZeroHeight) c = consumer.addPoint(start);
		} else {
			if (nonZeroHeight) c = consumer.addPoint(rect.minX, rect.maxY);
			if (nonZeroWidth)  c = consumer.addPoint(rect.maxX, rect.maxY);
			if (nonZeroHeight) c = consumer.addPoint(rect.maxX, rect.minY);
			if (nonZeroWidth)  c = consumer.addPoint(start);
		}
		return c;
	}

	@Override
	public SimplifiedPath simplify() {
		return splitAtCorners().simplify();
	}

	@Override
	public SequencePath splitAtCorners() {
		SequencePath.Builder builder = SequencePath.builder();
		linearize(builder);
		return builder.asLinearPaths().closeAndBuild();
	}

	@Override
	public RectPath getReverse() {
		return new RectPath(rect, !positiveRotation);
	}

	private double lengthAtImpl(double p) {
		if (p <= 0.0) return 0.0;
		if (p >= 1.0) return rect.getPerimeterLength();
		p *= 4;
		int s = (int) p;
		p -= s;
		if (positiveRotation) {
            return switch (s) {
                case 0 -> rect.getWidth() * p;
                case 1 -> rect.getWidth() + rect.getHeight() * p;
                case 2 -> rect.getWidth() * (p + 1) + rect.getHeight();
                default -> rect.getWidth() * 2 + rect.getHeight() * (p + 1);
            };
		} else {
            return switch (s) {
                case 0 -> rect.getHeight() * p;
                case 1 -> rect.getHeight() + rect.getWidth() * p;
                case 2 -> rect.getHeight() * (p + 1) + rect.getWidth();
                default -> rect.getHeight() * 2 + rect.getWidth() * (p + 1);
            };
		}
	}

	private double intrinsicAtImpl(double p) {
		if (p <= 0.0) return 0.0;
		double prev = 0.0;
		double next = 0.0;
		double w = rect.getWidth();
		double h = rect.getHeight();
		double a, b;
		if (positiveRotation) {
			a = w; b = h;
		} else {
			a = h; b = w;
		}
		{
			next += a;
			if (p < next) return         p         * 0.25 / a;
			prev = next;
			next += b;
			if (p < next) return 0.25 + (p - prev) * 0.25 / b;
			prev = next;
			next += a;
			if (p < next) return 0.50 + (p - prev) * 0.25 / a;
			prev = next;
			next += b;
			if (p < next) return 0.75 + (p - prev) * 0.25 / b;
		}
		return 1.0;
	}

	private class ByIntrinsic implements Parameterization.ByIntrinsic {

		private List<Corner> corners = null;
		private PolygonalPath polygonal = null;

		@Override
		public Path getPath() {
			return RectPath.this;
		}

		@Override
		public Location location() {
			return new Location(this, 0.0);
		}

		@Override
		public Point pointAt(double p) {
			if (p <= 0) return start;
			if (p >= 1) return start;
			p *= 4;
			int s = (int) p;
			p -= s;
			return pointAtImpl(s, p);
		}

		@Override
		public Vector tangentAt(double p) {
			if (p <= 0.0) return Vector.UNIT_X;
			if (p >= 1.0) return Vector.UNIT_NEG_Y;
			return tangentAtImpl((int) (p * 4));
		}

		@Override
		public PointPath pointTangentAt(double p) {
			if (p <= 0.0) p = 0.0;
			else if (p >= 1.0) p = 1.0;
			p *= 4;
			int s = (int) p;
			p -= s;
			return PointPath.from(pointAtImpl(s, p), tangentAtImpl(s));
		}

		@Override
		public SplitPath splitAt(double p) {
			//TODO can we assume this p value is mapped equivalently?
			return polygonal().byIntrinsic().splitAt(p);
		}

		@Override
		public Path segment(double minP, double maxP) {
			//TODO can we assume this p value is mapped equivalently?
			return polygonal().byIntrinsic().segment(minP, maxP);
		}

		@Override
		public double parameterNearest(Point p) {
			Point pt = rect.nearestPointTo(p, true);
			double w = rect.getWidth();
			double h = rect.getHeight();
			if (h == 0.0 && w == 0.0)        return 0.0;
			if (positiveRotation) {
				if (pt.y == rect.minY)     return        (pt.x - rect.minX) / w * 0.25;
				if (pt.x == rect.maxX)     return 0.25 + (pt.y - rect.minY) / h * 0.25;
				if (pt.y == rect.maxY)     return 0.75 - (pt.x - rect.minX) / w * 0.25;
				/*if (pt.x == rect.minX)*/ return 1.00 - (pt.y - rect.minY) / h * 0.25;
			} else {
				if (pt.x == rect.minX)     return        (pt.y - rect.minY) / h * 0.25;
				if (pt.y == rect.maxY)     return 0.25 + (pt.x - rect.minX) / w * 0.25;
				if (pt.x == rect.maxX)     return 0.75 - (pt.y - rect.minY) / h * 0.25;
				/*if (pt.y == rect.minY)*/ return 1.00 - (pt.x - rect.minX) / w * 0.25;
			}
		}

		@Override
		public List<Corner> getCorners() {
			if (corners == null) {
				Corner[] array;
				if (positiveRotation) {
					array = new Corner[] {
							new Corner(this, 0.00, new Point(rect.minX, rect.minY), Vector.UNIT_NEG_Y, Vector.UNIT_X    ),
							new Corner(this, 0.25, new Point(rect.maxX, rect.minY), Vector.UNIT_X,     Vector.UNIT_Y    ),
							new Corner(this, 0.50, new Point(rect.maxX, rect.maxY), Vector.UNIT_Y,     Vector.UNIT_NEG_X),
							new Corner(this, 0.75, new Point(rect.minX, rect.maxY), Vector.UNIT_NEG_X, Vector.UNIT_NEG_Y),
					};
				} else {
					array = new Corner[] {
							new Corner(this, 0.00, new Point(rect.minX, rect.minY), Vector.UNIT_NEG_X, Vector.UNIT_Y    ),
							new Corner(this, 0.25, new Point(rect.minX, rect.maxY), Vector.UNIT_Y,     Vector.UNIT_X    ),
							new Corner(this, 0.50, new Point(rect.maxX, rect.maxY), Vector.UNIT_X,     Vector.UNIT_NEG_Y),
							new Corner(this, 0.75, new Point(rect.maxX, rect.minY), Vector.UNIT_NEG_Y, Vector.UNIT_NEG_X),
					};
				}
				this.corners = Collections.unmodifiableList(Arrays.asList(array));
			}
			return corners;
		}

		@Override
		public double lengthAt(double p) {
			return lengthAtImpl(p);
		}

		private Vector tangentAtImpl(int s) {
			if (positiveRotation) {
                return switch (s) {
                    case 0 -> Vector.UNIT_X;
                    case 1 -> Vector.UNIT_Y;
                    case 2 -> Vector.UNIT_NEG_X;
                    default -> Vector.UNIT_NEG_Y;
                };
			} else {
                return switch (s) {
                    case 0 -> Vector.UNIT_Y;
                    case 1 -> Vector.UNIT_X;
                    case 2 -> Vector.UNIT_NEG_Y;
                    default -> Vector.UNIT_NEG_X;
                };
			}
		}

		private Point pointAtImpl(int s, double p) {
			double q;
			if (positiveRotation) {
				q = 1 - p;
			} else {
				q = p;
				p = 1 - q;
				s = 3 - s;
			}
			switch (s) {
			case 0 : return new Point(rect.maxX * p + rect.minX * q, rect.minY);
			case 1 : return new Point(rect.maxX, p * rect.maxY + q * rect.minY);
			case 2 : return new Point(rect.minX * p + rect.maxX * q, rect.maxY);
			default: return new Point(rect.minX, p * rect.minY + q * rect.maxY);
			}
		}

		private PolygonalPath polygonal() {
			if (polygonal == null) {
				PolygonalPath.Builder builder = PolygonalPath.builder();
				linearize(builder);
				polygonal = builder.closeAndBuild();
			}
			return polygonal;
		}
	}

	private class ByLength extends Reparameterization.ByLength {

		private ByLength() {
			super(byIntrinsic());
		}

		@Override
		public Path getPath() {
			return RectPath.this;
		}

		@Override
		public double parameterNearest(Point p) {
			Point pt = rect.nearestPointTo(p, true);
			double w = rect.getWidth();
			double h = rect.getHeight();
			if (h == 0.0 && w == 0.0)        return 0.0;
			if (positiveRotation) {
				if (pt.y == rect.minY)     return                 (pt.x - rect.minX);
				if (pt.x == rect.maxX)     return w             + (pt.y - rect.minY);
				if (pt.y == rect.maxY)     return w + h + w     - (pt.x - rect.minX);
				/*if (pt.x == rect.minX)*/ return w + h + w + h - (pt.y - rect.minY);
			} else {
				if (pt.x == rect.minX)     return                 (pt.y - rect.minY);
				if (pt.y == rect.maxY)     return h             + (pt.x - rect.minX);
				if (pt.x == rect.maxX)     return h + w + h     - (pt.y - rect.minY);
				/*if (pt.y == rect.minY)*/ return h + w + h + w - (pt.x - rect.minX);
			}
		}

		@Override
		public double intrinsicAt(double p) {
			return intrinsicAtImpl(p);
		}

		@Override
		protected double map(double p) {
			return intrinsicAtImpl(p);
		}

		@Override
		protected double unmap(double q) {
			return lengthAtImpl(q);
		}

	}

}
