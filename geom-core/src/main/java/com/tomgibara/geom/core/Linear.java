package com.tomgibara.geom.core;

public interface Linear {

	Vector getTangent();

	Vector getNormal();

	Point nearestPointTo(Point pt);

	Line getLine();

	LineSegment bounded(Rect bounds);

	int sideOf(Point point);

	int sideOf(double x, double y);
}
