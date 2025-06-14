package com.tomgibara.geom.core;

import java.util.List;

import com.tomgibara.geom.path.Parameterization;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;
import com.tomgibara.geom.path.Reparameterization;
import com.tomgibara.geom.path.SimplifiedPath;
import com.tomgibara.geom.path.SingletonPath;
import com.tomgibara.geom.path.SplitPath;
import com.tomgibara.geom.transform.Transform;

// directed line segment

public final class LinearPath implements Path, Linear {

    static double nearestParamIntrinsic(Point start, Point finish, Point pt) {
        double dx = finish.x - start.x;
        double dy = finish.y - start.y;
        double magSqr = dx * dx + dy * dy;
        if (magSqr == 0) return 0.0;
        double x = pt.x - start.x;
        double y = pt.y - start.y;
        double p = x * dx + y * dy;
        if (p <= 0) return 0.0;
        if (p >= magSqr) return 1.0;
        return p / magSqr;
    }

    static double nearestParamLength(Point start, Point finish, Point pt, double length) {
        if (length == 0.0) return 0.0;
        double dx = finish.x - start.x;
        double dy = finish.y - start.y;
        double x = pt.x - start.x;
        double y = pt.y - start.y;
        double p = x * dx + y * dy;
        if (p <= 0) return 0.0;
        if (p >= length*length) return 1.0;
        return p / length;
    }

    private final LineSegment segment;
    private double length = -1;
    private ByIntrinsic byIntrinsic;
    private ByLength byLength;

    LinearPath(LineSegment segment) {
        this.segment = segment;
        if (segment.isZeroLength()) length = 0.0;
    }

    public LineSegment getSegment() {
        return segment;
    }

    @Override
    public Point nearestPointTo(Point pt) {
        return segment.nearestPointTo(pt);
    }

    @Override
    public Vector getTangent() {
        return segment.getTangent();
    }

    @Override
    public Vector getNormal() {
        return segment.getNormal();
    }

    @Override
    public Line getLine() {
        return segment.getLine();
    }

    @Override
    public LineSegment bounded(Rect rect) {
        return segment.bounded(rect);
    }

    @Override
    public int sideOf(double x, double y) {
        return segment.sideOf(x, y);
    }

    @Override
    public int sideOf(Point point) {
        return segment.sideOf(point);
    }

    @Override
    public Point getStart() {
        return segment.getStart();
    }

    @Override
    public Point getFinish() {
        return segment.getFinish();
    }

    @Override
    public <C> C linearize(Point.Consumer<C> consumer) {
        consumer.addPoint(segment.getStart());
        return consumer.addPoint(segment.getFinish());
    }

    @Override
    public SimplifiedPath simplify() {
        return new SimplifiedPath(this);
    }

    @Override
    public Parameterization.ByIntrinsic byIntrinsic() {
        return byIntrinsic == null ? byIntrinsic = new ByIntrinsic() : byIntrinsic;
    }

    @Override
    public Parameterization.ByLength byLength() {
        return byLength == null ? byLength = new ByLength() : byLength;
    }

    @Override
    public double getLength() {
        return length < 0 ? length = Norm.L2.lengthOf(this) : length;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isRectilinear() {
        return segment.isRectilinear();
    }

    @Override
    public boolean isSmooth() {
        return true;
    }

    @Override
    public LinearPath getReverse() {
        LineSegment reverse = segment.getReverse();
        return reverse == segment ? this : reverse.getPath();
    }

    @Override
    public SingletonPath splitAtCorners() {
        return new SingletonPath(this);
    }

    @Override
    public Rect getBounds() {
        return segment.getBounds();
    }

    @Override
    public LinearPath apply(Transform t) {
        LineSegment transformed = segment.apply(t);
        return transformed == segment ? this : transformed.getPath();
    }

    @Override
    public LineSegment getGeometry() {
        return segment;
    }

    @Override
    public int hashCode() {
        return segment.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof LinearPath that)) return false;
        return this.segment.equals(that.segment);
    }

    @Override
    public String toString() {
        return "linear path from " + segment.getStart() + " to " + segment.getFinish();
    }

    private class ByIntrinsic implements Parameterization.ByIntrinsic {

        private final Point start = getStart();
        private final Point finish = getFinish();

        @Override
        public Path getPath() {
            return LinearPath.this;
        }

        @Override
        public Point pointAt(double p) {
            if (p <= 0) return start;
            if (p >= 1) return finish;
            return Point.Util.interpolate(start, finish, p);
        }

        @Override
        public Vector tangentAt(double p) {
            return getTangent();
        }

        @Override
        public PointPath pointTangentAt(double p) {
            return PointPath.from(pointAt(p), segment.getTangent());
        }

        @Override
        public SplitPath splitAt(double p) {
            Point point = pointAt(p);
            LinearPath first;
            LinearPath second;
            if (start == finish) {
                first = LinearPath.this;
                second = LinearPath.this;
            } else if (point.equals(start)) {
                //TODO eliminate this case
                first = segment.zeroSubsegment(start).getPath();
                second = LinearPath.this;
            } else if (point.equals(finish)) {
                first = LinearPath.this;
                //TODO eliminate this case
                second = segment.zeroSubsegment(finish).getPath();
            } else {
                first = segment.nonZeroSubsegment(start, point).getPath();
                second = segment.nonZeroSubsegment(point, finish).getPath();
            }
            return new SplitPath(first, second, false);
        }

        @Override
        public LinearPath segment(double minP, double maxP) {
            Point minPt = Point.Util.interpolate(start, finish, minP);
            Point maxPt = Point.Util.interpolate(start, finish, maxP);
            return segment.subsegment(minPt, maxPt).getPath();
        }

        @Override
        public double lengthAt(double p) {
            if (p <= 0.0) return 0.0;
            if (p >= 1.0) return getLength();
            return getLength() * p;
        }

        @Override
        public double parameterNearest(Point pt) {
            return nearestParamIntrinsic(start, finish, pt);
        }

        @Override
        public Path.Location location() {
            return new Path.Location(this, 0);
        }

        @Override
        public List<Path.Corner> getCorners() {
            return Path.Corner.NO_CORNERS;
        }

    }

    private class ByLength extends Reparameterization.ByLength {

        private final double length = getLength();

        public ByLength() {
            super(byIntrinsic());
        }

        @Override
        public Path getPath() {
            return LinearPath.this;
        }

        @Override
        public double parameterNearest(Point pt) {
            return nearestParamLength(getStart(), getFinish(), pt, length);
        }

        @Override
        public double intrinsicAt(double p) {
            return p / length;
        }

        @Override
        protected double map(double p) {
            return p / length;
        }

        @Override
        protected double unmap(double q) {
            return q * length;
        }

    }

}
