package com.tomgibara.geom.curve;

import static com.tomgibara.geom.core.Angles.TWO_PI;

import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Geometric;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public final class Ellipse implements Geometric {

	private static final Rect BASIC_BOUND = Rect.atPoints(-1f, -1f, 1f, 1f);

	public static Ellipse fromTransform(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		return new Ellipse(t);
	}

	public static Ellipse fromRadius(float radius) {
		if (radius == 0f) throw new IllegalArgumentException("zero radius");
		return new Ellipse( Transform.scale(radius) );
	}

	public static Ellipse fromRadius(Point center, float radius) {
		if (center == null) throw new IllegalArgumentException("null center");
		if (radius == 0f) throw new IllegalArgumentException("zero radius");
		Transform t = Transform.scale(radius);
		if (!center.isOrigin()) t = t.apply(center.vectorFromOrigin().asTranslation());
		return new Ellipse(t);
	}

	public static Ellipse fromRadii(float xRadius, float yRadius) {
		if (xRadius == 0f) throw new IllegalArgumentException("zero xRadius");
		if (yRadius == 0f) throw new IllegalArgumentException("zero yRadius");
		return new Ellipse( Transform.scale(xRadius, yRadius) );
	}

	public static Ellipse fromRadii(Point center, float xRadius, float yRadius) {
		if (center == null) throw new IllegalArgumentException("null center");
		if (xRadius == 0f) throw new IllegalArgumentException("zero xRadius");
		if (yRadius == 0f) throw new IllegalArgumentException("zero yRadius");
		Transform t = Transform.scale(xRadius, yRadius);
		if (!center.isOrigin()) t = t.apply(center.vectorFromOrigin().asTranslation());
		return new Ellipse(t);
	}

	public static Ellipse fromRect(Rect rect) {
		if (rect == null) throw new IllegalArgumentException("null rect");
		return fromRadii(rect.getCenter(), rect.getWidth() * 0.5f, rect.getHeight() * 0.5f);
	}

	private final Transform transform;
	// derived from transform
	private Point center;
	private Transform eigenBasis = null;
	private Vector semiMinorAxis = null;
	private Vector semiMajorAxis = null;
	//private boolean firstIsMajor;
	private float orientation;
	private float maxR = Float.NaN;
	private float minR = Float.NaN;
	private Rect bounds = null;

	private Ellipse(Transform transform) {
		this.transform = transform;
	}

	// accessors

	public Transform getTransform() {
		return transform;
	}

	public boolean isCircular() {
		return transform.isCirclePreserving();
	}

	public Point getCenter() {
		return center == null ? center = transform.transformOrigin() : center;
	}

	public float getMajorRadius() {
		if (Float.isNaN(maxR)) computeRadii();
		return maxR;
	}

	public float getMinorRadius() {
		if (Float.isNaN(minR)) computeRadii();
		return minR;
	}

	public Vector getSemiMajorAxis() {
		if (semiMajorAxis == null) computeSemiAxes();
		return semiMajorAxis;
	}

	public Vector getSemiMinorAxis() {
		if (semiMinorAxis == null) computeSemiAxes();
		return semiMinorAxis;
	}

	public EllipticalArc arc(float startAngle, float finishAngle) {
		return new EllipticalArc(this, startAngle / Angles.TWO_PI, finishAngle / Angles.TWO_PI);
	}

	public EllipticalArc completeArc() {
		return new EllipticalArc(this, 0f, 1f);
	}

	@Override
	public Ellipse apply(Transform t) {
		return new Ellipse(transform.apply(t));
	}

	public Point getPoint(float scaledAngle) {
		float angle = scaledAngle * TWO_PI;
		float c = (float) Math.cos(angle);
		float s = (float) Math.sin(angle);
		return transform.transform(c, s);
	}

	@Override
	public Rect getBounds() {
		return bounds == null ? bounds = computeBounds() : bounds;
	}

	public Rect getArcBounds(float startAngleScaled, float finishAngleScaled) {
		Rect b1 = getBounds();
		//TODO
//		if (Math.abs(finishAngleScaled - startAngleScaled) >= 0.5f) return b1;
//		// this is fairly efficient for small arcs
//		Point pt1 = getPoint(startAngleScaled);
//		Point pt2 = getPoint(finishAngleScaled);
//		Point c = Point.Util.midpoint(pt1, pt2);
//		float r = Norm.L2.distanceBetween(c, pt1);
//		Rect b2 = Rect.atPoints(c.x - r, c.y - r, c.x + r, c.y + r);
//		return Rect.intersectionRect(b1, b2);
		return b1;
	}

	@Override
	public int hashCode() {
		return transform.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Ellipse)) return false;
		Ellipse that = (Ellipse) obj;
		return this.transform.equals(that.transform);
	}

	@Override
	public String toString() {
		return transform.toString();
	}

	private Rect computeBounds() {
		return BASIC_BOUND.apply(transform);
	}

	private Transform getEigenBasis() {
		return eigenBasis == null ? eigenBasis = transform.getEigenBasis() : eigenBasis;
	}

	private void computeRadii() {
//		Transform basis = getEigenBasis();
//		//TODO is there a way to avoid these sqrts?
//		float r1 = basis.getColumn(0).getMagnitude();
//		float r2 = basis.getColumn(1).getMagnitude();
//		if (basis.isFirstColumnMajor()) {
//			maxR = r1;
//			minR = r2;
//		} else {
//			maxR = r2;
//			minR = r1;
//		}
		Vector es = transform.getEigenValues();
		float e1 = Math.abs(es.x);
		float e2 = Math.abs(es.y);
		if (e2 > e1) {
			maxR = e2;
			minR = e1;
		} else {
			maxR = e1;
			minR = e2;
		}
	}

	private void computeSemiAxes() {
		float m00 = transform.m00;
		float m01 = transform.m01;
		float m10 = transform.m10;
		float m11 = transform.m11;

		float a = -(m00 * m01 + m10 * m11);
		float b = (m11 + m00) * (m11 - m00);
		float c = (m00 * m10 + m01 * m11);

		float d = (float) Math.sqrt(b*b - 4 * a * c);
//		float t1 = (-b + d) / (a * 2f);
//		float t2 = (-b - d) / (a * 2f);

		//float angle = (float) Math.atan2(-b + d, a * 2f);
		semiMajorAxis = new Vector(a * 2f, -b + d).normalized().scaled(50f);
		semiMinorAxis = new Vector(a * 2f, -b - d).normalized().scaled(50f);

		Vector vx = transform.transform(Vector.UNIT_X);
		Vector vz = transform.transform(Vector.ZERO);
		semiMajorAxis = vx.subtract(vz).normalized().scaled(50f);
		semiMinorAxis = Vector.ZERO;

	}
}
