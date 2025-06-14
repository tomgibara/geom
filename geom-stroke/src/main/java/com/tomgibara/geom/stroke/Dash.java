package com.tomgibara.geom.stroke;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.path.Path;

public interface Dash {

    public static final Iterator<FloatRange> EVERYWHERE = new Universal();
    public static final Iterator<FloatRange> NOWHERE = new Universal();

    public static final Dash EVERYWHERE_DASH = new EverywhereDash();
    public static final Dash NOWHERE_DASH = new NowhereDash();

    Iterator<FloatRange> getPattern(Path path);

    public static final class Universal implements Iterator<FloatRange> {

        public boolean hasNext() {
            return false;
        }

        @Override
        public FloatRange next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    };

    public static class EverywhereDash implements Dash {

        @Override
        public Iterator<FloatRange> getPattern(Path path) {
            return EVERYWHERE;
        }

    }

    public static class NowhereDash implements Dash {

        @Override
        public Iterator<FloatRange> getPattern(Path path) {
            return NOWHERE;
        }

    }

}
