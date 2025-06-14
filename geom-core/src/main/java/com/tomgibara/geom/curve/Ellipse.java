package com.tomgibara.geom.curve;

import static com.tomgibara.geom.core.Angles.TWO_PI;

import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Geometric;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

public final class Ellipse implements Geometric {

	private static final Rect BASIC_BOUND = Rect.atPoints(-1.0, -1.0, 1.0, 1.0);

	public static Ellipse fromTransform(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		return new Ellipse(t);
	}

	public static Ellipse fromRadius(double radius) {
		if (radius == 0.0) throw new IllegalArgumentException("zero radius");
		return new Ellipse( Transform.scale(radius) );
	}

	public static Ellipse fromRadius(Point center, double radius) {
		if (center == null) throw new IllegalArgumentException("null center");
		if (radius == 0.0) throw new IllegalArgumentException("zero radius");
		Transform t = Transform.scale(radius);
		if (!center.isOrigin()) t = t.apply(center.vectorFromOrigin().asTranslation());
		return new Ellipse(t);
	}

	public static Ellipse fromRadii(double xRadius, double yRadius) {
		if (xRadius == 0.0) throw new IllegalArgumentException("zero xRadius");
		if (yRadius == 0.0) throw new IllegalArgumentException("zero yRadius");
		return new Ellipse( Transform.scale(xRadius, yRadius) );
	}

	public static Ellipse fromRadii(Point center, double xRadius, double yRadius) {
		if (center == null) throw new IllegalArgumentException("null center");
		if (xRadius == 0.0) throw new IllegalArgumentException("zero xRadius");
		if (yRadius == 0.0) throw new IllegalArgumentException("zero yRadius");
		Transform t = Transform.scale(xRadius, yRadius);
		if (!center.isOrigin()) t = t.apply(center.vectorFromOrigin().asTranslation());
		return new Ellipse(t);
	}

	public static Ellipse fromRect(Rect rect) {
		if (rect == null) throw new IllegalArgumentException("null rect");
		return fromRadii(rect.getCenter(), rect.getWidth() * 0.5, rect.getHeight() * 0.5);
	}

	private final Transform transform;
	// derived from transform
	private Point center;
	private Transform eigenBasis = null;
	private Vector semiMinorAxis = null;
	private Vector semiMajorAxis = null;
	//private boolean firstIsMajor;
	private double orientation;
	private double maxR = Double.NaN;
	private double minR = Double.NaN;
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

	public double getMajorRadius() {
		if (Double.isNaN(maxR)) computeRadii();
		return maxR;
	}

	public double getMinorRadius() {
		if (Double.isNaN(minR)) computeRadii();
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

	public EllipticalArc arc(double startAngle, double finishAngle) {
		return new EllipticalArc(this, startAngle / Angles.TWO_PI, finishAngle / Angles.TWO_PI);
	}

	public EllipticalArc completeArc() {
		return new EllipticalArc(this, 0.0, 1.0);
	}

	@Override
	public Ellipse apply(Transform t) {
		return new Ellipse(transform.apply(t));
	}

	public Point getPoint(double scaledAngle) {
		double angle = scaledAngle * TWO_PI;
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		return transform.transform(c, s);
	}

	@Override
	public Rect getBounds() {
		return bounds == null ? bounds = computeBounds() : bounds;
	}

	public Rect getArcBounds(double startAngleScaled, double finishAngleScaled) {
		Rect b1 = getBounds();
		//TODO
//		if (Math.abs(finishAngleScaled - startAngleScaled) >= 0.5) return b1;
//		// this is fairly efficient for small arcs
//		Point pt1 = getPoint(startAngleScaled);
//		Point pt2 = getPoint(finishAngleScaled);
//		Point c = Point.Util.midpoint(pt1, pt2);
//		double r = Norm.L2.distanceBetween(c, pt1);
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
//		double r1 = basis.getColumn(0).getMagnitude();
//		double r2 = basis.getColumn(1).getMagnitude();
//		if (basis.isFirstColumnMajor()) {
//			maxR = r1;
//			minR = r2;
//		} else {
//			maxR = r2;
//			minR = r1;
//		}
		Vector es = transform.getEigenValues();
		double e1 = Math.abs(es.x);
		double e2 = Math.abs(es.y);
		if (e2 > e1) {
			maxR = e2;
			minR = e1;
		} else {
			maxR = e1;
			minR = e2;
		}
	}

	private void computeSemiAxes() {
		double m00 = transform.m00;
		double m01 = transform.m01;
		double m10 = transform.m10;
		double m11 = transform.m11;

		double a = -(m00 * m01 + m10 * m11);
		double b = (m11 + m00) * (m11 - m00);
		double c = (m00 * m10 + m01 * m11);

		double d = Math.sqrt(b*b - 4 * a * c);
//		double t1 = (-b + d) / (a * 2.0);
//		double t2 = (-b - d) / (a * 2.0);

		//double angle = Math.atan2(-b + d, a * 2.0);
		semiMajorAxis = new Vector(a * 2.0, -b + d).normalized().scaled(50.0);
		semiMinorAxis = new Vector(a * 2.0, -b - d).normalized().scaled(50.0);

		Vector vx = transform.transform(Vector.UNIT_X);
		Vector vz = transform.transform(Vector.ZERO);
		semiMajorAxis = vx.subtract(vz).normalized().scaled(50.0);
		semiMinorAxis = Vector.ZERO;

	}
}
