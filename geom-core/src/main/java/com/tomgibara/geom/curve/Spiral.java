package com.tomgibara.geom.curve;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public class Spiral extends Curve {

	public static Spiral from(Point center, float startAngle, float finishAngle, float a, float b) {
		return new Spiral(center, startAngle, finishAngle, a, b);
	}

	private final Point center;
	private final float startAngle;
	private final float finishAngle;
	private final float startRadius;
	private final float finishRadius;

	Spiral(Point center, float startAngle, float finishAngle, float startRadius, float finishRadius) {
		this.center = center;
		this.startAngle = startAngle;
		this.finishAngle = finishAngle;
		this.startRadius = startRadius;
		this.finishRadius = finishRadius;
	}

	public float getStartAngle() {
		return startAngle;
	}

	public float getFinishAngle() {
		return finishAngle;
	}

	public float getStartRadius() {
		return startRadius;
	}

	public float getFinishRadius() {
		return finishRadius;
	}

	@Override
	protected CurvePath createPath() {
		return new CurvePath(this);
	}

	@Override
	public Point pointAt(float p) {
		return Transform.rotation(angle(p)).transform(new Vector(radius(p), 0f)).translate(center);
	}

	@Override
	public SplitCurvePath splitAt(float p) {
		p = clamp(p);
		float angle = angle(p);
		CurvePath s1 = new Spiral(center, startAngle, angle,       radius(0f), radius(p)  ).getPath();
		CurvePath s2 = new Spiral(center, angle,      finishAngle, radius(p),  radius(1f) ).getPath();
		return new SplitCurvePath(s1, s2, false);
	}

	@Override
	protected boolean isClosed() {
		return false;
	}

	private float angle(float p) {
		return startAngle + (finishAngle - startAngle) * p;
	}

	private float radius(float p) {
		if (startAngle == finishAngle) return startRadius;
		return startRadius * (1 - p) + finishRadius * p;
	}

	@Override
	//TODO consider a similar computeSplit?
	protected Spiral computeSegment(float minP, float maxP) {
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
