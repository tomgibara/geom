package com.tomgibara.geom.curve;

import static com.tomgibara.geom.core.Angles.TWO_PI;

import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.core.Point.Consumer;
import com.tomgibara.geom.transform.Transform;

public class EllipticalArc extends Curve {

    private static double frac(double f) {
        double i = Math.floor(f);
        return f - i;
    }

    // creates a circular arc starting at pt1, ending at pt3 and passing through pt2
    public static EllipticalArc circularArcThroughThreePoints(Point p1, Point p2, Point p3) {
        return circularArcThroughThreePoints(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
    }

    private static EllipticalArc circularArcThroughThreePoints(double x1, double y1, double x2, double y2, double x3, double y3) {
        double d = 2 * (x1 - x3) * (y3 - y2) + 2 * (x2 - x3) * (y1 - y3);
        double m1 = x1 * x1 - x3 * x3 + y1 * y1 - y3 * y3;
        double m2 = x3 * x3 - x2 * x2 + y3 * y3 - y2 * y2;
        double nx = m1 * (y3 - y2) + m2 * (y3 - y1);
        double ny = m1 * (x2 - x3) + m2 * (x1 - x3);
        double cx = nx / d;
        double cy = ny / d;
        double r = Math.hypot(x1 - cx, y1 - cy);
        double a1 = Math.atan2(y1 - cy, x1 - cx);
        double a2 = Math.atan2(y2 - cy, x2 - cx); //TODO computing this is inefficient - is there a better approach?
        double a3 = Math.atan2(y3 - cy, x3 - cx);

        double adj;
        if (a1 < a3) {
            adj = a1 <= a2 && a2 <= a3 ? 0.0 : -Angles.TWO_PI;
        } else {
            adj = a3 <= a2 && a2 <= a1 ? 0.0 : +Angles.TWO_PI;
        }

        return new EllipticalArc(Ellipse.fromRadius(new Point(cx, cy), r), a1 / Angles.TWO_PI, (a3 + adj) / Angles.TWO_PI);
    }

    private final Ellipse geom;
    private final double startAngle;
    private final double finishAngle;

    EllipticalArc(Ellipse geom, double startAngle, double finishAngle) {
        this.geom = geom;
        this.startAngle = startAngle;
        this.finishAngle = finishAngle;
    }

    public Ellipse getGeom() {
        return geom;
    }

    public double getStartAngle() {
        return Angles.TWO_PI * startAngle;
    }

    public double getFinishAngle() {
        return Angles.TWO_PI * finishAngle;
    }

    @Override
    public Point pointAt(double p) {
        return geom.getPoint(angle(clamp(p)));
    }

    @Override
    public Vector tangentAt(double p) {
        double ra = startAngle > finishAngle ? - Angles.PI_BY_TWO : + Angles.PI_BY_TWO;
        double angle = angle(clamp(p)) * TWO_PI + ra;
        Vector tangent = new Vector(angle);
        Transform transform = geom.getTransform();
        //TODO how to optimize?
        Point pt1 = pointAt(p);
        Point pt2 = tangent.translate(pt1);
        pt1 = transform.transform(pt1);
        pt2 = transform.transform(pt2);
        return pt2.vectorFrom(pt1).normalized();
    }

    @Override
    public SplitCurvePath splitAt(double p) {
        double angle = angle(clamp(p));
        CurvePath s1 = new EllipticalArc(geom, startAngle, angle).getPath();
        CurvePath s2 = new EllipticalArc(geom, angle, finishAngle).getPath();
        return new SplitCurvePath(s1, s2, isClosed());
    }

    @Override
    protected CurvePath createPath() {
        return new EllipticalPath(this);
    }

    @Override
    public EllipticalArc apply(Transform t) {
        return t.isIdentity() ? this : new EllipticalArc(geom.apply(t), startAngle, finishAngle);
    }

    @Override
    protected boolean isLinear() {
        //TODO need to use length of major axis - or better
        //TODO should be configurable tolerance
        return Math.abs(finishAngle - startAngle) * geom.getBounds().getDiagonalLength() < 0.1;
    }

    @Override
    protected Curve computeReverse() {
        return new EllipticalArc(geom, finishAngle, startAngle);
    }

    @Override
    protected boolean isClosed() {
        return frac(startAngle) == frac(finishAngle);
    }

    @Override
    public String toString() {
        return "angle [" + startAngle + "," + finishAngle + "] on ellipse " + geom;
    }

    private double angle(double p) {
        return startAngle + (finishAngle - startAngle) * p;
    }

    private <K> K linearize(Consumer<K> consumer) {
        //TODO must have a better implementation that is sensitive to proximity to major axis
        K k = null;
        //TODO properly fix
        //int steps = Math.round(geom.getBounds().getDiagonalLength() / 0.1.0);
        int steps = 20;
        double d = 1.0 / steps;
        for (int i = 0; i <= steps; i++) {
            double angle = (startAngle * (steps - i) + finishAngle * i) * d;
            k = consumer.addPoint(geom.getPoint(angle));
        }
        return k;
    }

    private static final class EllipticalPath extends CurvePath {

        private final EllipticalArc arc;
        private Rect bounds = null;

        EllipticalPath(EllipticalArc arc) {
            super(arc);
            this.arc = arc;
        }

        @Override
        public Rect getBounds() {
            return bounds == null ? bounds = arc.getGeom().getArcBounds(arc.startAngle, arc.finishAngle) : bounds;
        }

        @Override
        public CurvePath apply(Transform t) {
            return t.isIdentity() ? this : arc.apply(t).getPath();
        }

        @Override
        public <K> K linearize(Consumer<K> consumer) {
            if (consumer == null) throw new IllegalArgumentException("null consumer");
            return arc.linearize(consumer);
        }

        @Override
        public CurvePath getReverse() {
            return arc.computeReverse().getPath();
        }

    }

}
