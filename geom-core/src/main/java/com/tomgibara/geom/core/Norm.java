package com.tomgibara.geom.core;

public abstract class Norm {

	public static final Norm L1 = new Norm() {

		@Override
		public float magnitude(float x, float y) {
			return Math.abs(x) + Math.abs(y);
		}

		@Override
		public float powMagnitude(float x, float y) {
			return Math.abs(x) + Math.abs(y);
		}

		@Override
		public float magnitude(int x, int y) {
			return Math.abs(x) + Math.abs(y);
		}

		@Override
		public float powMagnitude(int x, int y) {
			return Math.abs(x) + Math.abs(y);
		}

	};

	public static final Norm L2 = new Norm() {

		@Override
		public float magnitude(int x, int y) {
			if (x == 0) return Math.abs(y);
			if (y == 0) return Math.abs(x);
			return (float) Math.sqrt(x * x + y * y);
		}

		@Override
		public float powMagnitude(int x, int y) {
			return x * x + y * y;
		}

		@Override
		public float magnitude(float x, float y) {
			if (x == 0f) return Math.abs(y);
			if (y == 0f) return Math.abs(x);
			return (float) Math.sqrt(x * x + y * y);
		}

		@Override
		public float powMagnitude(float x, float y) {
			return x * x + y * y;
		}

		@Override
		public Vector normalize(Vector vector) {
			return vector.normalized();
		}

		@Override
		public float magnitude(Vector vector) {
			return vector.getMagnitude();
		}

	};

	public static final Norm Linf = new Norm() {

		@Override
		public float magnitude(float x, float y) {
			return Math.max(x, y);
		}

		@Override
		public float powMagnitude(int x, int y) {
			return x == 0 && y == 0 ? 0 : Float.POSITIVE_INFINITY;
		}

		@Override
		public float magnitude(int x, int y) {
			return Math.max(x, y);
		}

		@Override
		public float powMagnitude(float x, float y) {
			return x == 0f && y == 0f ? 0 : Float.POSITIVE_INFINITY;
		}

	};

	public static Norm L(final double p) {
		if (p < 1) throw new IllegalArgumentException("p less than 1");
		if (p == 1.0) return L1;
		if (p == 2.0) return L2;
		if (p == Double.POSITIVE_INFINITY) return Linf;
		return new Norm() {
			@Override
			public float magnitude(float x, float y) {
				if (x == 0f) return Math.abs(y);
				if (y == 0f) return Math.abs(x);
				return (float) Math.pow( Math.pow(Math.abs(x), p) + Math.pow(Math.abs(y), p), 1/p );
			}

			@Override
			public float powMagnitude(int x, int y) {
				return (float) Math.pow(Math.abs(x), p) + (float) Math.pow(Math.abs(y), p);
			}

			@Override
			public float magnitude(int x, int y) {
				return magnitude(x, y);
			}

			@Override
			public float powMagnitude(float x, float y) {
				return (float) Math.pow(Math.abs(x), p) + (float) Math.pow(Math.abs(y), p);
			}
		};
	}

	public float distanceFromOrigin(Point point) {
		return magnitude(point.x, point.y);
	}

	public float powDistanceFromOrigin(Point point) {
		return powMagnitude(point.x, point.y);
	}

	public float lengthOf(LinearPath path) {
		return distanceBetween(path.getStart(), path.getFinish());
	}

	public float powLengthOf(LinearPath path) {
		return powDistanceBetween(path.getStart(), path.getFinish());
	}

	public float distanceBetween(Point pt1, Point pt2) {
		return magnitude(pt1.x - pt2.x, pt1.y - pt2.y);
	}

	public float powDistanceBetween(Point pt1, Point pt2) {
		return powMagnitude(pt1.x - pt2.x, pt1.y - pt2.y);
	}

	public float magnitude(Vector vector) {
		return magnitude(vector.x, vector.y);
	}

	public Vector normalize(Vector vector) {
		return vector.isZero() ? vector : vector.scaled(1 / magnitude(vector));
	}

	public LineSegment normalize(LineSegment segment) {
		if (segment.isZeroLength()) return segment;
		Point start = segment.getStart();
		Vector v = normalize(segment.getVector());
		return LineSegment.fromVector(start, v);
	}

	public abstract float magnitude(int x, int y);

	public abstract float powMagnitude(int x, int y);

	public abstract float magnitude(float x, float y);

	public abstract float powMagnitude(float x, float y);

}
