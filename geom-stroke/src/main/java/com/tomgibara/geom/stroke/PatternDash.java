package com.tomgibara.geom.stroke;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.path.Path;

public class PatternDash implements Dash {

    public static PatternDash single(double dashLength, double gapLength) {
        return new PatternDash(dashLength + gapLength, new FloatRange(0.0, dashLength));
    }

    //TODO could allow empty ranges?
    public static PatternDash pattern(double length, FloatRange... ranges) {
        if (ranges == null) throw new IllegalArgumentException("null ranges");
        if (ranges.length == 0) throw new IllegalArgumentException("no ranges");
        double min = 0.0;
        for (int i = 0; i < ranges.length; i++) {
            FloatRange range = ranges[i];
            if (!range.isClosed() || range.isEmpty() || range.min < min) throw new IllegalArgumentException("invalid range");
            min = range.max;
        }
        if (min > length) throw new IllegalArgumentException("ranges exceed length");
        return new PatternDash(length, ranges.clone());
    }

    private final double length;
    private final FloatRange[] ranges;

    private PatternDash(double length, FloatRange... ranges) {
        this.length = length;
        this.ranges = ranges;
    }

    public double getLength() {
        return length;
    }

    public List<FloatRange> getRanges() {
        return Collections.unmodifiableList( Arrays.asList(ranges) );
    }

    @Override
    public Iterator<FloatRange> getPattern(Path path) {
        return new Iterator<FloatRange>() {

            double offset = 0.0;
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
