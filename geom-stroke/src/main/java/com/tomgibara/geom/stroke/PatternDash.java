package com.tomgibara.geom.stroke;

import java.util.Iterator;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.path.Path;

public class PatternDash implements Dash {

	public static PatternDash single(float dashLength, float gapLength) {
		return new PatternDash(dashLength + gapLength, new FloatRange(0f, dashLength));
	}

	//TODO could allow empty ranges?
	public static PatternDash pattern(float length, FloatRange... ranges) {
		if (ranges == null) throw new IllegalArgumentException("null ranges");
		if (ranges.length == 0) throw new IllegalArgumentException("no ranges");
		float min = 0f;
		for (int i = 0; i < ranges.length; i++) {
			FloatRange range = ranges[i];
			if (!range.isClosed() || range.isEmpty() || range.min < min) throw new IllegalArgumentException("invalid range");
			min = range.max;
		}
		if (min > length) throw new IllegalArgumentException("ranges exceed length");
		return new PatternDash(length, ranges.clone());
	}

	private final float length;
	private final FloatRange[] ranges;

	private PatternDash(float length, FloatRange... ranges) {
		this.length = length;
		this.ranges = ranges;
	}

	@Override
	public Iterator<FloatRange> getPattern(Path path) {
		return new Iterator<FloatRange>() {

			float offset = 0f;
			int index = 0;

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public FloatRange next() {
				try {
					return ranges[index++].translate(offset);
				} finally {
					if (index == ranges.length) {
						index = 0;
						offset += length;
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

}
