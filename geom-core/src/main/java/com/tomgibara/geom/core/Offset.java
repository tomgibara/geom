package com.tomgibara.geom.core;

import com.tomgibara.geom.transform.Transform;

public final class Offset {

	public static final Offset IDENTITY = new Offset();

	public static Offset offset(float toMinX, float toMaxX, float toMinY, float toMaxY) {
		return toMinX == 0f && toMaxX == 0f && toMinY == 0f && toMaxY == 0f ?
				IDENTITY : new Offset(toMinX, toMaxX, toMinY, toMaxY);
	}

	public static Offset translation(float x, float y) {
		return x == 0f && y == 0f ?
				IDENTITY : new Offset(x, y);
	}

	public static Offset uniform(float offset) {
		return offset == 0f ?
				IDENTITY: new Offset(offset);
	}

	public static Offset symmetric(float offsetX, float offsetY) {
		return offsetX == 0f && offsetY == 0f ?
				IDENTITY : new Offset(offsetX, -offsetX, offsetY, -offsetY);
	}

	public final float toMinX;
	public final float toMaxX;
	public final float toMinY;
	public final float toMaxY;

	private Offset() {
		toMinX = 0f;
		toMaxX = 0f;
		toMinY = 0f;
		toMaxY = 0f;
	}

	private Offset(float offset) {
		this.toMinX =  offset;
		this.toMaxX = -offset;
		this.toMinY =  offset;
		this.toMaxY = -offset;
	}

	// used by vector
	private Offset(float x, float y) {
		toMinX = x;
		toMaxX = x;
		toMinY = y;
		toMaxY = y;
	}

	private Offset(float toMinX, float toMaxX, float toMinY, float toMaxY) {
		this.toMinX = toMinX;
		this.toMaxX = toMaxX;
		this.toMinY = toMinY;
		this.toMaxY = toMaxY;
	}

	public boolean isIdentity() {
		return this == IDENTITY;
	}

	public boolean isConvex() {
		return this == IDENTITY || toMinX <= 0 && toMaxX >= 0 && toMinY <= 0 && toMaxY >= 0;
	}

	public boolean isConcave() {
		return this == IDENTITY || toMinX >= 0 && toMaxX <= 0 && toMinY >= 0 && toMaxY <= 0;
	}

	public Offset reversed() {
		return this == IDENTITY ? IDENTITY : new Offset(-toMinX, -toMaxX, -toMinY, -toMaxY);
	}

	public Rect offset(Rect rect) {
		return rect.offset(this);
	}

	//TODO just have a transform that takes two rectangles? have both?
	public Transform toTransform(Rect rect) {
		return Transform.translateAndScale(rect, rect.offset(this));
	}

	@Override
	public int hashCode() {
		return
				      Float.floatToIntBits(toMinX) +
				(31 * Float.floatToIntBits(toMaxX) +
				(31 * Float.floatToIntBits(toMinY) +
				(31 * Float.floatToIntBits(toMaxY))));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Offset)) return false;
		Offset that = (Offset) obj;
		if (this.toMinX != that.toMinX) return false;
		if (this.toMaxX != that.toMaxX) return false;
		if (this.toMinY != that.toMinY) return false;
		if (this.toMaxY != that.toMaxY) return false;
		return true;
	}

	@Override
	public String toString() {
		return toMinX + "<-x->" + toMaxX + ", " + toMinY + "<-y->" + toMaxY;
	}
}
