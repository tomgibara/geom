package com.tomgibara.geom.core;

public abstract class Norm {

    public static final Norm L1 = new Norm() {

        @Override
        public double magnitude(double x, double y) {
            return Math.abs(x) + Math.abs(y);
        }

        @Override
        public double powMagnitude(double x, double y) {
            return Math.abs(x) + Math.abs(y);
        }

        @Override
        public double magnitude(int x, int y) {
            return Math.abs(x) + Math.abs(y);
        }

        @Override
        public double powMagnitude(int x, int y) {
            return Math.abs(x) + Math.abs(y);
        }

    };

    public static final Norm L2 = new Norm() {

        @Override
        public double magnitude(int x, int y) {
            if (x == 0) return Math.abs(y);
            if (y == 0) return Math.abs(x);
            return Math.sqrt(x * x + y * y);
        }

        @Override
        public double powMagnitude(int x, int y) {
            return x * x + y * y;
        }

        @Override
        public double magnitude(double x, double y) {
            if (x == 0.0) return Math.abs(y);
            if (y == 0.0) return Math.abs(x);
            return Math.sqrt(x * x + y * y);
        }

        @Override
        public double powMagnitude(double x, double y) {
            return x * x + y * y;
        }

        @Override
        public Vector normalize(Vector vector) {
            return vector.normalized();
        }

        @Override
        public double magnitude(Vector vector) {
            return vector.getMagnitude();
        }

    };

    public static final Norm Linf = new Norm() {

        @Override
        public double magnitude(double x, double y) {
            return Math.max(x, y);
        }

        @Override
        public double powMagnitude(int x, int y) {
            return x == 0 && y == 0 ? 0 : Double.POSITIVE_INFINITY;
        }

        @Override
        public double magnitude(int x, int y) {
            return Math.max(x, y);
        }

        @Override
        public double powMagnitude(double x, double y) {
            return x == 0.0 && y == 0.0 ? 0 : Double.POSITIVE_INFINITY;
        }

    };

    public static Norm L(final double p) {
        if (p < 1) throw new IllegalArgumentException("p less than 1");
        if (p == 1.0) return L1;
        if (p == 2.0) return L2;
        if (p == Double.POSITIVE_INFINITY) return Linf;
        return new Norm() {
            @Override
            public double magnitude(double x, double y) {
                if (x == 0.0) return Math.abs(y);
                if (y == 0.0) return Math.abs(x);
                return Math.pow( Math.pow(Math.abs(x), p) + Math.pow(Math.abs(y), p), 1/p );
            }

            @Override
            public double powMagnitude(int x, int y) {
                return Math.pow(Math.abs(x), p) + Math.pow(Math.abs(y), p);
            }

            @Override
            public double magnitude(int x, int y) {
                return magnitude((double) x, (double) y);
            }

            @Override
            public double powMagnitude(double x, double y) {
                return Math.pow(Math.abs(x), p) + Math.pow(Math.abs(y), p);
            }
        };
    }

    public double distanceFromOrigin(Point point) {
        return magnitude(point.x, point.y);
    }

    public double powDistanceFromOrigin(Point point) {
        return powMagnitude(point.x, point.y);
    }

    public double lengthOf(LinearPath path) {
        return distanceBetween(path.getStart(), path.getFinish());
    }

    public double powLengthOf(LinearPath path) {
        return powDistanceBetween(path.getStart(), path.getFinish());
    }

    public double distanceBetween(Point pt1, Point pt2) {
        return magnitude(pt1.x - pt2.x, pt1.y - pt2.y);
    }

    public double powDistanceBetween(Point pt1, Point pt2) {
        return powMagnitude(pt1.x - pt2.x, pt1.y - pt2.y);
    }

    public double magnitude(Vector vector) {
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

    public abstract double magnitude(int x, int y);

    public abstract double powMagnitude(int x, int y);

    public abstract double magnitude(double x, double y);

    public abstract double powMagnitude(double x, double y);

}
