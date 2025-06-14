package com.tomgibara.geom.core;

public class Angles {

	public static final double TWO_PI = 2.0 * Math.PI;
	public static final double PI = Math.PI;
	public static final double PI_BY_TWO = Math.PI * 0.5;
	public static final double PI_BY_THREE = Math.PI / 3;
	public static final double PI_BY_FOUR = Math.PI * 0.25;
	public static final double PI_BY_SIX = Math.PI / 6;
	public static final double ONE_EIGHTY_BY_PIE = 180.0 / Math.PI;
	public static final double PIE_BY_ONE_EIGHTY = Math.PI / 180.0;
	public static final double COS_PI_BY_THREE = 0.86602540378;
	public static final double SIN_PI_BY_THREE = 0.5;
	public static final double COS_PI_BY_FOUR = 0.70710678118;
	public static final double SIN_PI_BY_FOUR = 0.70710678118;

	public static double toDegrees(double radians) {
		return ONE_EIGHTY_BY_PIE * radians;
	}

	public static double toRadians(double degrees) {
		return PIE_BY_ONE_EIGHTY * degrees;
	}

}
