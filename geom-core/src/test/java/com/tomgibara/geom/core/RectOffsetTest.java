package com.tomgibara.geom.core;

import junit.framework.TestCase;

public class RectOffsetTest extends TestCase {

    public void testOffset() {
        Rect r = Rect.atPoints(0, 0, 2, 2);
        Offset o = Offset.offset(-1, 3, -2, 4);
        Rect expected = Rect.atPoints(r.minX + o.toMinX, r.minY + o.toMinY,
                r.maxX + o.toMaxX, r.maxY + o.toMaxY);
        assertEquals(expected, r.offset(o));
    }
}
