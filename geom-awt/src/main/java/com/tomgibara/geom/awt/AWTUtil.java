package com.tomgibara.geom.awt;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.contour.EllipseContour;
import com.tomgibara.geom.contour.RectContour;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Traceable;
import com.tomgibara.geom.curve.BezierCurve;
import com.tomgibara.geom.curve.CurvePath;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.curve.EllipticalArc;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.SequencePath;
import com.tomgibara.geom.path.SequencePath.Builder;
import com.tomgibara.geom.shape.Shape;
import com.tomgibara.geom.shape.WindingRule;
import com.tomgibara.geom.transform.Transform;

public class AWTUtil {

	private static final Rectangle2D.Float basisSquare = new Rectangle2D.Float(-1f, -1f, 2f, 2f);
	private static final Ellipse2D.Float basisCircle = new Ellipse2D.Float(-1f, -1f, 2f, 2f);

	public static AffineTransform toAffineTransform(Transform transform) {
		if (transform == null) throw new IllegalArgumentException("null transform");
		return new AffineTransform(transform.getComponents());
	}

	public static Transform fromAffineTransform(AffineTransform transform) {
		if (transform == null) throw new IllegalArgumentException("null transform");
		return Transform.components(
				(float) transform.getScaleX(),
				(float) transform.getShearY(),
				(float) transform.getShearX(),
				(float) transform.getScaleY(),
				(float) transform.getTranslateX(),
				(float) transform.getTranslateY()
				);
	}

	public static Point2D toPoint(Point point) {
		if (point == null) throw new IllegalArgumentException("null point");
		return new Point2D.Float(point.x, point.y);
	}

	public static Point fromPoint(Point2D point) {
		if (point instanceof Point2D.Float) {
			Point2D.Float f = (Point2D.Float) point;
			return new Point(f.x, f.y);
		}
		return new Point((float) point.getX(), (float) point.getY());
	}

	public static Rectangle2D toRectangle(Rect rect) {
		if (rect == null) throw new IllegalArgumentException("null rect");
		return new Rectangle2D.Float(rect.minX, rect.minY, rect.maxX - rect.minX, rect.maxY - rect.minY);
	}

	public static Rect fromRectangle(Rectangle r) {
		if (r == null) throw new IllegalArgumentException("null r");
		return Rect.atPoints(r.x, r.y, r.x + r.width, r.x + r.height);
	}

	public static Rect fromRectangle(Rectangle2D.Float r) {
		if (r == null) throw new IllegalArgumentException("null r");
		return Rect.atPoints(r.x, r.y, r.x + r.width, r.y + r.height);
	}

	public static Rect fromRectangle(Rectangle2D r) {
		if (r == null) throw new IllegalArgumentException("null r");
		if (r instanceof Rectangle2D.Float) {
			Rectangle2D.Float f = (Rectangle2D.Float) r;
			return Rect.atPoints(f.x, f.y, f.x + f.width, f.y + f.height);
		} else {
			return Rect.atPoints((float) r.getMinY(), (float) r.getMinY(), (float) r.getMaxX(), (float) r.getMaxY());
		}
	}

	public static java.awt.Shape toEllipse(Ellipse ellipse) {
		Transform t = ellipse.getTransform();
		if (t.isRectilinearPreserving()) {
			// simple case - a direct match to awt ellipse
			Rect r = t.transform(Rect.BASIS_SQUARE);
			return new Ellipse2D.Float(r.minX, r.minY, r.getWidth(), r.getHeight());
		}
		return toAffineTransform(t).createTransformedShape(basisCircle);
	}

	public static Ellipse fromEllipse(Ellipse2D ellipse) {
		return Ellipse.fromRect(fromRectangle(ellipse.getBounds2D()));
	}

	public static Path2D.Float toPath2D(Path path) {
		Path2D.Float p = new Path2D.Float();
		toPath2D(p, path);
		return p;
	}

	public static Path2D.Float toPath2D(Path... paths) {
		Path2D.Float p = new Path2D.Float();
		for (Path path : paths) {
			toPath2D(p, path);
		}
		return p;
	}

	public static Path2D.Float toPath2D(List<Path> paths) {
		Path2D.Float p = new Path2D.Float();
		for (Path path : paths) {
			toPath2D(p, path);
		}
		return p;
	}

	public static List<Path> fromPath2D(Path2D p) {
		List<Path> paths = new ArrayList<>();
		PathIterator pi = p.getPathIterator(null);
		Builder builder = SequencePath.builder();
		float[] coords = new float[6];
		while(!pi.isDone()) {
			int type = pi.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_MOVETO: {
				if (!builder.isEmpty()) {
					//TODO want a proper simplify method
					paths.add( builder.build() );
					builder = SequencePath.builder();
				}
				builder.addPoint(coords[0], coords[1]);
				break;
			}
			case PathIterator.SEG_LINETO: {
				builder.addPoints(coords, 0, 2).asLinearPath();
				break;
			}
			case PathIterator.SEG_QUADTO: {
				builder.addPoints(coords, 0, 4).asBezierCurve();
				break;
			}
			case PathIterator.SEG_CUBICTO: {
				builder.addPoints(coords, 0, 6).asBezierCurve();
				break;
			}
			case PathIterator.SEG_CLOSE: {
				paths.add( builder.closeAndBuild() );
				builder = SequencePath.builder();
				break;
			}
			}
			pi.next();
		}
		if (!builder.isEmpty()) {
			paths.add( builder.build() );
		}
		return paths;
	}

	public static int toWindingRule(WindingRule rule) {
		switch (rule) {
		case EVEN_ODD : return Path2D.WIND_EVEN_ODD;
		case NON_ZERO : return Path2D.WIND_EVEN_ODD;
		default:
			throw new IllegalArgumentException("unsupported winding rule: " + rule);
		}
	}

	public static WindingRule fromWindingRule(int rule) {
		switch (rule) {
		case Path2D.WIND_EVEN_ODD : return WindingRule.EVEN_ODD;
		case Path2D.WIND_NON_ZERO : return WindingRule.NON_ZERO;
		default:
			throw new IllegalArgumentException("unsupported winding rule: " + rule);
		}
	}

	public static java.awt.Shape toShape2D(Shape shape) {
		List<Contour> contours = shape.getContours();
		if (contours.size() == 1) { // we may be able to handle this as a special case
			Contour contour = contours.get(0);
			if (contour instanceof RectContour) {
				RectContour rc = (RectContour) contour;
				return toRectangle(rc.getBounds());
			}
			if (contour instanceof EllipseContour) {
				EllipseContour ec = (EllipseContour) contour;
				return toEllipse(ec.getGeometry());
			}
		}
		Path2D.Float p = new Path2D.Float();
		for (Contour contour : contours) {
			toPath2D(p, contour.getPath());
		}
		p.setWindingRule(toWindingRule(shape.getWindingRule()));
		return p;
	}

	private static void toPath2D(final Path2D.Float p, Path path) {
		Point start = path.getStart();
		p.moveTo(start.x, start.y);
		Traceable g = path.getGeometry();
		boolean processed = false;
		if (g instanceof LineSegment) {
			LineSegment segment = (LineSegment) g;
			Point finish = segment.getFinish();
			p.lineTo(finish.x, finish.y);
			processed = true;
		} else if (g instanceof BezierCurve) {
			BezierCurve b = (BezierCurve) g;
			switch (b.getOrder()) {
			case 0: { // point - ignored
				processed = true;
			}
			case 1: { // linear
				Point finish = b.getPath().getFinish();
				p.lineTo(finish.x, finish.y);
				processed = true;
			}
			case 2: { // quadratic
				List<Point> pts = b.getPoints();
				Point pt1 = pts.get(1);
				Point pt2 = pts.get(2);
				p.quadTo(pt1.x, pt1.y, pt2.x, pt2.y);
				processed = true;
				break;
			}
			case 3: { // cubic
				List<Point> pts = b.getPoints();
				Point pt1 = pts.get(1);
				Point pt2 = pts.get(2);
				Point pt3 = pts.get(3);
				p.curveTo(pt1.x, pt1.y, pt2.x, pt2.y, pt3.x, pt3.y);
				processed = true;
			}
			}
		} else if (g instanceof EllipticalArc) {
			EllipticalArc ea = (EllipticalArc) g;
			Transform t = ea.getGeom().getTransform();
			AffineTransform xform = toAffineTransform(t);
			Arc2D.Float arc = new Arc2D.Float(basisSquare, 0, 360, Arc2D.OPEN);
			CurvePath cp = ea.getPath();
			Transform i = t.getInverse();
			arc.setAngles(toPoint(i.transform(cp.getStart())), toPoint(i.transform(cp.getFinish())));
			if (ea.getFinishAngle() > ea.getStartAngle()) arc.extent = - 360 + arc.extent;
			PathIterator pi = arc.getPathIterator(xform);
			p.append(pi, true);
			processed = true;
		}
		if (!processed) {
			g.getPath().linearize(new Point.Consumer<Void>() {
				private boolean first = true;
				@Override
				public Void addPoint(float x, float y) {
					if (first) {
						first = false;
					} else {
						p.lineTo(x, y);
					}
					return null;
				}
				@Override
				public Void addPoint(Point pt) {
					if (first) {
						first = false;
					} else {
						p.lineTo(pt.x, pt.y);
					}
					return null;
				}
			});
		}
		if (path.isClosed()) p.closePath();
	}
}
