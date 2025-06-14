package com.tomgibara.geom.stroke;

import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.curve.CurvePath;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;
import com.tomgibara.geom.path.PolygonalPath;

public interface Cap {

	ButtCap BUTT_CAP = new ButtCap();
	SquareCap SQUARE_CAP = new SquareCap();
	RoundCap ROUND_CAP = new RoundCap();

	Path cap(PointPath startPtAndTan, PointPath finishPtAndTan, Point center);

	class ButtCap implements Cap {

		ButtCap() { }

		@Override
		public Path cap(PointPath startPtAndTan, PointPath finishPtAndTan, Point center) {
			Point start = startPtAndTan.getStart();
			Point finish = finishPtAndTan.getStart();
			return start.equals(finish) ? null : LineSegment.fromPoints(start, finish).getPath();
		}

	}

	public static final class SquareCap implements Cap {

		SquareCap() { }

		@Override
		public Path cap(PointPath startPtAndTan, PointPath finishPtAndTan, Point center) {
			Point start = startPtAndTan.getStart();
			Point finish = finishPtAndTan.getStart();
			double width = Norm.L2.distanceBetween(start, finish);
			double length = width * 0.5;
			return PolygonalPath.builder()
				.addPoint(start)
				.addPoint(startPtAndTan.getTangentAsSegment().scaleLength(length).getFinish())
				.addPoint(finishPtAndTan.getTangentAsSegment().scaleLength(-length).getFinish())
				.addPoint(finish)
				.build();
		}

	}

	public static final class RoundCap implements Cap {

		RoundCap() { }

		@Override
		public Path cap(PointPath startPtAndTan, PointPath finishPtAndTan, Point center) {
			Vector v1 = startPtAndTan.getStart().vectorFrom(center);
			Vector v2 = finishPtAndTan.getStart().vectorFrom(center);

			int side1 = startPtAndTan.sideOf(center);
			int side2 = finishPtAndTan.sideOf(center);
			//TODO?
			if (side1 == 0 || side2 == 0) return BUTT_CAP.cap(startPtAndTan, finishPtAndTan, center);
			//TG I don't think this should happen
			if (side1 != side2) Context.currentContext().log(false, "mismatched tangents: {0} {1}", side1, side2);

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


	}

}
