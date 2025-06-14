package com.tomgibara.geom.helper;

import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;

public class Winder {

    private final Intersector intersector;
    private final Consumer consumer = new Consumer();

    public Winder() {
        intersector = new Intersector(consumer, true, true);
    }

    public int countWindings(Contour contour, Point pt) {
        if (contour == null) throw new IllegalArgumentException("null contour");
        if (pt == null) throw new IllegalArgumentException("null pt");
        Rect bounds = contour.getBounds();
        if (!bounds.containsPoint(pt)) return 0;
        Point nearest = bounds.nearestPointTo(pt, true);
        //TODO should this be zero or one, what convention?
        if (nearest.equals(pt)) return 1;
        //TODO this is hacky - theoretically, line could clip back into the contour
        LineSegment line = LineSegment.fromPoints(pt, nearest).scaleLength(1.001);
        consumer.reset(line);
        intersector.intersect(contour.getPath(), line.getPath());
        return consumer.getWinding();
    }

    private static class Consumer implements Point.Consumer<Void> {

        private static final int INC_X = 0;
        private static final int DEC_X = 1;
        private static final int INC_Y = 2;
        private static final int DEC_Y = 3;

        private int direction;

        private int count;
        private int winding;

        private double startX;
        private double startY;

        void reset(LineSegment line) {
            Point start = line.getStart();
            Point finish = line.getFinish();
            if (start.y == finish.y) {
                direction = start.x < finish.x ? INC_X : DEC_X;
            } else {
                direction = start.y < finish.y ? INC_Y : DEC_Y;
            }

            count = 0;
            winding = 0;
        }

        int getWinding() {
            return winding;
        }

        @Override
        public Void addPoint(double x, double y) {
            switch (count) {
            case 0 :
                startX = x;
                startY = y;
                count = 1;
                break;
            case 1 :
                count = 2;
                break;
                default:
                    // TODO how to handle equality situations
                    final int delta = switch (direction) {
                        case INC_X -> y > startY ? 1 : -1;
                        case DEC_X -> y < startY ? 1 : -1;
                        case INC_Y -> x < startX ? 1 : -1;
                        case DEC_Y -> x > startX ? 1 : -1;
                        default -> throw new UnsupportedOperationException();
                    };
                    winding += delta;
                    count = 0;
            }
            return null;
        }

        @Override
        public Void addPoint(Point pt) {
            return addPoint(pt.x, pt.y);
        }

    }

}
