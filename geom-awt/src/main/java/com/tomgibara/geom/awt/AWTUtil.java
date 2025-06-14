package com.tomgibara.geom.awt;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
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

    private static final Rectangle2D.Double basisSquare = new Rectangle2D.Double(-1.0, -1.0, 2.0, 2.0);
    private static final Ellipse2D.Double basisCircle = new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0);

    public static AffineTransform toAffineTransform(Transform transform) {
        if (transform == null) throw new IllegalArgumentException("null transform");
        return new AffineTransform(transform.getComponents());
    }

    public static Transform fromAffineTransform(AffineTransform transform) {
        if (transform == null) throw new IllegalArgumentException("null transform");
        return Transform.components(
                transform.getScaleX(),
                transform.getShearY(),
                transform.getShearX(),
                transform.getScaleY(),
                transform.getTranslateX(),
                transform.getTranslateY()
                );
    }

    public static Point2D toPoint(Point point) {
        if (point == null) throw new IllegalArgumentException("null point");
        return new Point2D.Double(point.x, point.y);
    }

    public static Point fromPoint(Point2D point) {
        return new Point(point.getX(), point.getY());
    }

    public static Rectangle2D toRectangle(Rect rect) {
        if (rect == null) throw new IllegalArgumentException("null rect");
        return new Rectangle2D.Double(rect.minX, rect.minY, rect.maxX - rect.minX, rect.maxY - rect.minY);
    }

    public static Rect fromRectangle(Rectangle r) {
        if (r == null) throw new IllegalArgumentException("null r");
        return Rect.atPoints(r.x, r.y, r.x + r.width, r.y + r.height);
    }

    public static Rect fromRectangle(Rectangle2D.Float r) {
        if (r == null) throw new IllegalArgumentException("null r");
        return Rect.atPoints(r.x, r.y, r.x + r.width, r.y + r.height);
    }

    public static Rect fromRectangle(Rectangle2D r) {
        if (r == null) throw new IllegalArgumentException("null r");
        return Rect.atPoints(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
    }

    public static Line2D.Double toLine(LineSegment lineSegment) {
        if (lineSegment == null) throw new IllegalArgumentException("null lineSegment");
        var start = lineSegment.getStart();
        var finish = lineSegment.getFinish();
        return new Line2D.Double(start.x, start.y, finish.x, finish.y);

    }

    public static LineSegment fromLine(Line2D line) {
        if (line == null) throw new IllegalArgumentException("null line");
        var start = fromPoint(line.getP1());
        var finish = fromPoint(line.getP2());
        return LineSegment.fromPoints(start, finish);
    }

    public static java.awt.Shape toEllipse(Ellipse ellipse) {
        Transform t = ellipse.getTransform();
        if (t.isRectilinearPreserving()) {
            // simple case - a direct match to awt ellipse
            Rect r = t.transform(Rect.BASIS_SQUARE);
            return new Ellipse2D.Double(r.minX, r.minY, r.getWidth(), r.getHeight());
        }
        return toAffineTransform(t).createTransformedShape(basisCircle);
    }

    public static Ellipse fromEllipse(Ellipse2D ellipse) {
        return Ellipse.fromRect(fromRectangle(ellipse.getBounds2D()));
    }

    public static Path2D.Double toPath2D(Path path) {
        Path2D.Double p = new Path2D.Double();
        toPath2D(p, path);
        return p;
    }

    public static Path2D.Double toPath2D(Path... paths) {
        Path2D.Double p = new Path2D.Double();
        for (Path path : paths) {
            toPath2D(p, path);
        }
        return p;
    }

    public static Path2D.Double toPath2D(List<? extends Path> paths) {
        Path2D.Double p = new Path2D.Double();
        for (Path path : paths) {
            toPath2D(p, path);
        }
        return p;
    }

    public static List<Path> fromPath2D(Path2D p) {
        List<Path> paths = new ArrayList<>();
        PathIterator pi = p.getPathIterator(null);
        Builder builder = SequencePath.builder();
        double[] coords = new double[6];
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
        return switch (rule) {
            case EVEN_ODD -> Path2D.WIND_EVEN_ODD;
            case NON_ZERO -> Path2D.WIND_NON_ZERO;
        };
    }

    public static WindingRule fromWindingRule(int rule) {
        return switch (rule) {
            case Path2D.WIND_EVEN_ODD -> WindingRule.EVEN_ODD;
            case Path2D.WIND_NON_ZERO -> WindingRule.NON_ZERO;
            default -> throw new IllegalArgumentException("unsupported winding rule: " + rule);
        };
    }

    public static java.awt.Shape toShape2D(Shape shape) {
        List<Contour> contours = shape.getContours();
        if (contours.size() == 1) { // we may be able to handle this as a special case
            Contour contour = contours.get(0);
            if (contour instanceof RectContour rc) {
                return toRectangle(rc.getBounds());
            }
            if (contour instanceof EllipseContour ec) {
                return toEllipse(ec.getGeometry());
            }
        }
        Path2D.Double p = new Path2D.Double();
        for (Contour contour : contours) {
            toPath2D(p, contour.getPath());
        }
        p.setWindingRule(toWindingRule(shape.getWindingRule()));
        return p;
    }

    private static void toPath2D(final Path2D.Double p, Path path) {
        Point start = path.getStart();
        p.moveTo(start.x, start.y);
        Traceable g = path.getGeometry();
        boolean processed = false;
        if (g instanceof LineSegment segment) {
            Point finish = segment.getFinish();
            p.lineTo(finish.x, finish.y);
            processed = true;
        } else if (g instanceof BezierCurve b) {
            switch (b.getOrder()) {
                case 0: { // point - ignored
                    processed = true;
                    break;
                }
                case 1: { // linear
                    Point finish = b.getPath().getFinish();
                    p.lineTo(finish.x, finish.y);
                    processed = true;
                    break;
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
                    break;
                }
            }
        } else if (g instanceof EllipticalArc ea) {
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
                public Void addPoint(double x, double y) {
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
