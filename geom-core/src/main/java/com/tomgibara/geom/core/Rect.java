package com.tomgibara.geom.core;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.helper.Bounder;
import com.tomgibara.geom.transform.Transform;

//TODO use from not at in cons?
public final class Rect implements Geometric {

	public static final Rect UNIT_SQUARE = new Rect(0f, 0f, 1f, 1f);
	public static final Rect BASIS_SQUARE = new Rect(-1f, -1f, 1f, 1f);

	public static Rect atOrigin(float x, float y) {
		return new Rect(
			Math.min(x, 0),
			Math.min(y, 0),
			Math.max(x, 0),
			Math.max(y, 0));
	}

	public static Rect centerAtOrigin(float width, float height) {
		float hw = width * 0.5f;
		float hh = height * 0.5f;
		return new Rect(-hw, -hh, hw, hh);
	}

	public static Rect atOrigin(Point pt) {
		if (pt == null) throw new IllegalArgumentException("null pt");
		return atOrigin(pt.x, pt.y);
	}

	public static Rect atCenter(Point pt, float width, float height) {
		if (pt == null) throw new IllegalArgumentException("null pt");
		if (width < 0f) throw new IllegalArgumentException("negative width");
		if (height < 0f) throw new IllegalArgumentException("negative height");
		float hw = width * 0.5f;
		float hh = height * 0.5f;
		return new Rect(pt.x - hw, pt.y - hh, pt.x + hw, pt.y + hh);
	}

	public static Rect atPoints(float x1, float y1, float x2, float y2) {
		return new Rect(
			Math.min(x1, x2),
			Math.min(y1, y2),
			Math.max(x1, x2),
			Math.max(y1, y2));
	}

	public static Rect atPoints(Point pt1, Point pt2) {
		if (pt1 == null) throw new IllegalArgumentException("null pt1");
		if (pt2 == null) throw new IllegalArgumentException("null pt2");
		return atPoints(pt1.x, pt1.y, pt2.x, pt2.y);
	}

	public static Rect atPoint(Point pt) {
		if (pt == null) throw new IllegalArgumentException("null pt");
		return new Rect(pt.x, pt.y, pt.x, pt.y);
	}

	public static Rect unionRect(Rect r1, Rect r2) {
		if (r1 == null) throw new IllegalArgumentException("null r1");
		if (r2 == null) throw new IllegalArgumentException("null r2");

		if (r1.containsRect(r2)) return r1;
		if (r2.containsRect(r1)) return r2;

		return new Rect(
			Math.min(r1.minX, r2.minX),
			Math.min(r1.minY, r2.minY),
			Math.max(r1.maxX, r2.maxX),
			Math.max(r1.maxY, r2.maxY)
		);
	}

	public static boolean rectsIntersect(Rect r1, Rect r2) {
		if (r1 == null) throw new IllegalArgumentException("null r1");
		if (r2 == null) throw new IllegalArgumentException("null r2");
		return r1.maxX > r2.minX && r1.minX < r2.maxX && r1.maxY > r2.minY && r1.minY < r2.maxY;
	}

	public static Rect intersectionRect(Rect r1, Rect r2) {
		if (r1 == null) throw new IllegalArgumentException("null r1");
		if (r2 == null) throw new IllegalArgumentException("null r2");
		float minX = Math.max(r1.minX, r2.minX);
		float minY = Math.max(r1.minY, r2.minY);
		float maxX = Math.min(r1.maxX, r2.maxX);
		float maxY = Math.min(r1.maxY, r2.maxY);
		return minX < maxX && minY < maxY ? new Rect(minX, minY, maxX, maxY) : null;
	}

	public final float minX;
	public final float minY;
	public final float maxX;
	public final float maxY;

	Rect(float minX, float minY, float maxX, float maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public float getWidth() {
		return maxX - minX;
	}

	public float getHeight() {
		return maxY - minY;
	}

	public Point getCenter() {
		return new Point((minX + maxX) * 0.5f, (minY + maxY) * 0.5f);
	}

	// if the width or height are zero
	public boolean isDegenerate() {
		return minX == maxX || minY == maxY;
	}

	public Rect translate(float x, float y) {
		return x == 0 && y == 0 ? this : new Rect(minX + x, minY + y, maxX + x, maxY + y);
	}

	public Rect translateToOrigin() {
		return minX == 0f && minY == 0f ? this : new Rect(0f, 0f, getWidth(), getHeight());
	}

	//TODO rename to outset and supply an inset too?
	public Rect offset(float offset) {
		if (offset == 0f) return this;
		if (offset > 0f) return new Rect(minX - offset, minY - offset, maxX + offset, maxY + offset);
		return Rect.atPoints(minX - offset, minY - offset, maxX + offset, maxY + offset);
	}

	//TODO put this on offset instead?
	public Rect offset(Offset offset) {
		if (offset == Offset.IDENTITY) return this;
		float x1 = minX + offset.toMinX;
		float y1 = minY + offset.toMinY;
		float x2 = maxX + offset.toMaxX;
		float y2 = maxX + offset.toMaxY;
		//TODO do this more efficiently: if it flips hz or vt then slower path
		// ie if hz. growth + width < 0 flip x1 x2
		// ditto vt. growth
		return offset.isConvex() ?
				new Rect(x1, y1, x2, y2) :
				Rect.atPoints(x1, y1, x2, y2);
	}

	public Offset offsetFrom(Rect rect) {
		return Offset.offset(
				this.minX - rect.minX,
				this.maxX - rect.maxX,
				this.minY - rect.minY,
				this.maxY - rect.maxY
				);
	}

	public Offset offsetTo(Rect rect) {
		return rect.offsetFrom(this);
	}

	public boolean containsPoint(Point pt) {
		return (pt.x >= minX && pt.x <= maxX && pt.y >= minY && pt.y <= maxY);
	}

	public boolean containsRect(Rect rect) {
		return minX <= rect.minX && maxX >= rect.maxX && minY <= rect.minY && maxY >= rect.maxY;
	}

	public Point nearestPointTo(Point pt, boolean edgeOnly) {
		if (pt == null) throw new IllegalArgumentException("null pt");

		float x = pt.x;
		float y = pt.y;
		//  0 | 1 | 2
		// ---+---+---
		//  3 | 4 | 5
		// ---+---+---
		//  6 | 7 | 8
		int c = 0;
		if (x >= minX) c ++;
		if (x > maxX) c ++;
		if (y >= minY) c += 3;
		if (y > maxY) c += 3;

		switch (c) {
		case 0 : return new Point(minX, minY);
		case 1 : return new Point(x, minY);
		case 2 : return new Point(maxX, minY);
		case 3 : return new Point(minX, y);
		case 5 : return new Point(maxX, y);
		case 6 : return new Point(minX, maxY);
		case 7 : return new Point(x, maxY);
		case 8 : return new Point(maxX, maxY);
		default:
			if (edgeOnly) {
				float dxMin = x - minX;
				float dxMax = maxX - x;
				float dyMin = y - minY;
				float dyMax = maxY - y;
				if (dxMin == 0 || dxMax == 0 || dyMin == 0 || dyMax==0) return pt;
				if (dxMin <= dxMax) {
					if (dyMin <= dyMax) {
						return dxMin <= dyMin ? new Point(minX, y) : new Point(x, minY);
					} else {
						return dxMin <= dyMax ? new Point(minX, y) : new Point(x, maxY);
					}
				} else {
					if (dyMin <= dyMax) {
						return dxMax <= dyMin ? new Point(maxX, y) : new Point(x, minY);
					} else {
						return dxMax <= dyMax ? new Point(maxX, y) : new Point(x, maxY);
					}
				}
			}
			return pt;
		}
	}

	public Point furthestPointTo(Point pt) {
		Point c = getCenter();
		return new Point(
				pt.x <= c.x ? maxX : minX,
				pt.y <= c.y ? maxY : minY
		);
	}

	public FloatRange getXRange() {
		return new FloatRange(minX, maxX);
	}

	public FloatRange getYRange() {
		return new FloatRange(minY, maxY);
	}

	public float getDiagonalLength() {
		return Norm.L2.magnitude(maxX - minX, maxY - minY);
	}

	public float getPerimeterLength() {
		return 2 * (maxX - minX + maxY - minY);
	}
	// geometric methods

	@Override
	public Rect apply(Transform t) {
		if (t == null) throw new IllegalArgumentException("null t");
		//TODO what's the best optimization when t is rectilinear preserving?
		//TODO move to method on Transform?
		return new Bounder()
			.addPoint(t.transform(minX, minY))
			.addPoint(t.transform(maxX, minY))
			.addPoint(t.transform(minX, maxY))
			.addPoint(t.transform(maxX, maxY))
			.getBounds();
	}

	@Override
	public Rect getBounds() {
		return this;
	}

	@Override
	public int hashCode() {
		int h = Float.floatToIntBits(minX);
		h = h * 31 + Float.floatToIntBits(maxX);
		h = h * 31 + Float.floatToIntBits(minY);
		h = h * 31 + Float.floatToIntBits(maxY);
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Rect)) return false;
		Rect that = (Rect) obj;
		if (this.minX != that.minX) return false;
		if (this.maxX != that.maxX) return false;
		if (this.minY != that.minY) return false;
		if (this.maxY != that.maxY) return false;
		return true;
	}

	@Override
	public String toString() {
		return "x: " + getXRange() + ", y: " + getYRange();
	}

}
