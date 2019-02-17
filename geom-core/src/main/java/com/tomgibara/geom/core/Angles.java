package com.tomgibara.geom.core;

public class Angles {

	public static final float TWO_PI = (float) (2.0 * Math.PI);
	public static final float PI = (float) Math.PI;
	public static final float PI_BY_TWO = (float) (Math.PI * 0.5);
	public static final float PI_BY_THREE = (float) (Math.PI / 3);
	public static final float PI_BY_FOUR = (float) (Math.PI * 0.25);
	public static final float PI_BY_SIX = (float) (Math.PI / 6);
	public static final float ONE_EIGHTY_BY_PIE = (float) (180.0 / Math.PI);
	public static final float PIE_BY_ONE_EIGHTY = (float) (Math.PI / 180.0);
	public static final float COS_PI_BY_THREE = (float) 0.86602540378;
	public static final float SIN_PI_BY_THREE = 0.5f;
	public static final float COS_PI_BY_FOUR = (float) 0.70710678118;
	public static final float SIN_PI_BY_FOUR = (float) 0.70710678118;

	public static final float toDegrees(float radians) {
		return ONE_EIGHTY_BY_PIE * radians;
	}

	public static final float toRadians(float degrees) {
		return PIE_BY_ONE_EIGHTY * degrees;
	}

}
