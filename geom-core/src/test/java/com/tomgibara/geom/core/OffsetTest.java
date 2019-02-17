package com.tomgibara.geom.core;

import com.tomgibara.geom.transform.Transform;

import junit.framework.TestCase;

public class OffsetTest extends TestCase {

	public void testBasic() {
		Rect a = Rect.atPoints( 0,  0, 2, 2);
		Rect b = Rect.atPoints(-1, -1, 5, 5);
		Offset x = Offset.offset(-1, 3, -1, 3);
		assertEquals(x, b.offsetFrom(a));
		assertEquals(x.reversed(), a.offsetFrom(b));
		Transform t = x.toTransform(a);
		assertEquals(b, t.transform(a));
	}

}
