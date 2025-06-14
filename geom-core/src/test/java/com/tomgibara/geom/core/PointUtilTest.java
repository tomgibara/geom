package com.tomgibara.geom.core;

import junit.framework.TestCase;

public class PointUtilTest extends TestCase {

    public void testMidpoint() {
        Point a = new Point(1.0, 2.0);
        Point b = new Point(3.0, 4.0);
        Point mid = Point.Util.midpoint(a, b);
        assertEquals(new Point(2.0, 3.0), mid);
    }
}
