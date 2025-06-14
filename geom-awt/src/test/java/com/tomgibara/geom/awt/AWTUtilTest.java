package com.tomgibara.geom.awt;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import com.tomgibara.geom.core.Rect;

import junit.framework.TestCase;

public class AWTUtilTest extends TestCase {

    public void testFromRectangle() {
        Rectangle r = new Rectangle(1, 2, 3, 4);
        Rect rect = AWTUtil.fromRectangle(r);
        assertEquals(Rect.atPoints(1, 2, 4, 6), rect);
    }

    public void testFromRectangle2D() {
        Rectangle2D d = new Rectangle2D.Double(1.5, 2.5, 3.5, 4.5);
        Rect rect = AWTUtil.fromRectangle(d);
        assertEquals(Rect.atPoints(1.5, 2.5, 5.0, 7.0), rect);
    }
}
