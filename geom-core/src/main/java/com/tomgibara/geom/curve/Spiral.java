package com.tomgibara.geom.curve;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public class Spiral extends Curve {

    public static Spiral from(Point center, double startAngle, double finishAngle, double a, double b) {
        return new Spiral(center, startAngle, finishAngle, a, b);
    }

    private final Point center;
    private final double startAngle;
    private final double finishAngle;
    private final double startRadius;
    private final double finishRadius;

    Spiral(Point center, double startAngle, double finishAngle, double startRadius, double finishRadius) {
        this.center = center;
        this.startAngle = startAngle;
        this.finishAngle = finishAngle;
        this.startRadius = startRadius;
        this.finishRadius = finishRadius;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getFinishAngle() {
        return finishAngle;
    }

    public double getStartRadius() {
        return startRadius;
    }

    public double getFinishRadius() {
        return finishRadius;
    }

    @Override
    protected CurvePath createPath() {
        return new CurvePath(this);
    }

    @Override
    public Point pointAt(double p) {
        return Transform.rotation(angle(p)).transform(new Vector(radius(p), 0.0)).translate(center);
    }

    @Override
    public SplitCurvePath splitAt(double p) {
        p = clamp(p);
        double angle = angle(p);
        CurvePath s1 = new Spiral(center, startAngle, angle,       radius(0.0), radius(p)  ).getPath();
        CurvePath s2 = new Spiral(center, angle,      finishAngle, radius(p),  radius(1.0) ).getPath();
        return new SplitCurvePath(s1, s2, false);
    }

    @Override
    protected boolean isClosed() {
        return false;
    }

    private double angle(double p) {
        return startAngle + (finishAngle - startAngle) * p;
    }

    private double radius(double p) {
        if (startAngle == finishAngle) return startRadius;
        return startRadius * (1 - p) + finishRadius * p;
    }

    @Override
    //TODO consider a similar computeSplit?
    protected Spiral computeSegment(double minP, double maxP) {
        return new Spiral(center, angle(minP), angle(maxP), radius(minP), radius(maxP));
    }

    @Override
    protected Curve computeReverse() {
        return new Spiral(center, finishAngle, startAngle, finishRadius, startRadius);
    }

    @Override
    public String toString() {
        return "Spiral center: "+ center + ", angles: [" + startAngle + "," + finishAngle + "], radii: [" + startRadius + "," + finishRadius + "]";
    }

}
