package com.tomgibara.geom.stroke;

import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.curve.CurvePath;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;

public interface Join {

    public static final BevelJoin BEVEL_JOIN = new BevelJoin();
    public static final RoundJoin ROUND_JOIN = new RoundJoin();

    Path join(PointPath startPtAndTan, PointPath finishPtAndTan, Point center);

    public static final class BevelJoin implements Join {

        BevelJoin() { }

        @Override
        public Path join(PointPath startPtAndTan, PointPath finishPtAndTan, Point center) {
            return LineSegment.fromPoints(startPtAndTan.getStart(), finishPtAndTan.getStart()).getPath();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public String toString() {
            return "BevelJoin";
        }

    }

    public final static class RoundJoin implements Join {

        RoundJoin() { }

        @Override
        public Path join(PointPath startPtAndTan, PointPath finishPtAndTan, Point center) {
            Vector v1 = startPtAndTan.getStart().vectorFrom(center);
            Vector v2 = finishPtAndTan.getStart().vectorFrom(center);

            int side1 = startPtAndTan.sideOf(center);
            int side2 = finishPtAndTan.sideOf(center);
            if (side1 == 0 || side2 == 0) return BEVEL_JOIN.join(startPtAndTan, finishPtAndTan, center);
            //TG I don't think this should happen
            if (side1 != side2) throw new IllegalStateException("mismatched tangents");

            //TODO shouldn't assume v1 & v2 are the same length
            // ideally want to use an ellipse
            double radius = v1.getMagnitude();
            double a1 = v1.getAngle();

            //double a2 = a1 + v1.angleTo(v2);
            double a2 = v2.getAngle();
            if (side1 > 0) {
                if (a2 < a1) a2 += Angles.TWO_PI;
            } else {
                if (a2 > a1) a2 -= Angles.TWO_PI;
            }
            CurvePath path = Ellipse.fromRadius(center, radius).arc(a1, a2).getPath();
            return path;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public String toString() {
            return "RoundJoin";
        }

    }

}
