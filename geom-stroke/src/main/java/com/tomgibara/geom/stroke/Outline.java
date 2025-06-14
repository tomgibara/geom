package com.tomgibara.geom.stroke;

import java.util.List;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.curve.OffsetCurve;
import com.tomgibara.geom.floats.FloatMapping;
import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;
import com.tomgibara.geom.path.SequencePath;
import com.tomgibara.geom.path.SequencePath.Builder;
import com.tomgibara.geom.path.SequencePath.Builder.Policy;

public class Outline {

    private static FloatMapping submapping(double from, double to, FloatMapping mapping) {
        FloatMapping remapping = FloatMapping.Util.linear(new FloatRange(0, to - from), new FloatRange(from, to));
        return FloatMapping.Util.compose(remapping, mapping);
    }

    private final Join join;
    private final FloatMapping width;

    public Outline(Join join, FloatMapping width) {
        if (join == null) throw new IllegalArgumentException("null join");
        if (width == null) throw new IllegalArgumentException("null width");

        this.join = join;
        this.width = width;
    }

    public Outline(Join join, double width) {
        if (join == null) throw new IllegalArgumentException("null join");
        if (width <= 0.0) throw new IllegalArgumentException("non-positive width");

        this.join = join;
        this.width = FloatMapping.Util.constant(FloatRange.UNIT_CLOSED, width);
    }

    public double fixedWidth() {
        return width.isConstant() ? width.getRange().min : -1.0;
    }

    public Outline getReverse() {
        FloatRange domain = width.getDomain();
        FloatMapping remapping = FloatMapping.Util.linear(domain, domain.max, domain.min);
        FloatMapping reversed = FloatMapping.Util.compose(remapping, width);
        return new Outline(join, reversed);
    }

    public Path outline(Path path, FloatRange range) {
        if (path == null) throw new IllegalArgumentException("null path");
        if (range == null) throw new IllegalArgumentException("null range");
        FloatRange whole = new FloatRange(0.0, path.getLength());
        range = whole.intersectionAsRange(range);
        if (range == null) throw new IllegalStateException("range does not intersect path extent");

        //Path segment = path.byLength().splitAt(range.min).getLastPath().byLength().splitAt(range.getSize()).getFirstPath();
        Path segment = path.byLength().segment(range.min, range.max);
        // first map width function to length of path
        FloatMapping remapping1 = FloatMapping.Util.linear(whole, width.getDomain());
        // then pick out a segment of it
        FloatMapping remapping2 = FloatMapping.Util.linear(new FloatRange(0.0, range.getSize()), range);
        FloatMapping mapping;
        //TODO how do we handle small errors here?
        mapping = FloatMapping.Util.compose(remapping1, width);
        mapping = FloatMapping.Util.compose(remapping2, mapping);

        return outlineImpl(segment, mapping);

    }

    public Path outline(Point point) {
        if (point == null) throw new IllegalArgumentException("null point");
        if (width.getRange().isZeroSize()) { // simple case, constant makes a point round a circle
            return Ellipse.fromRadius(point, width.getRange().min).completeArc().getPath();
        } else {
            //TODO want a PolarCurve implementation
            throw new UnsupportedOperationException("PolarCurve required");
        }
    }

    public Path outline(Path path) {
        if (path == null) throw new IllegalArgumentException("null path");
        if (path.getLength() == 0.0) {
            PointPath ptPth;
            if (path instanceof PointPath) {
                ptPth = (PointPath) path;
            } else {
                ptPth = path.byIntrinsic().pointTangentAt(0.0);
            }
            return outlineImpl(ptPth);
        }

        // remap the width to the path length
        double length = path.getLength();
        FloatMapping remapping = FloatMapping.Util.linear(new FloatRange(0.0, length), width.getDomain());
        FloatMapping mapping = FloatMapping.Util.compose(remapping, width);

        return outlineImpl(path, mapping);
    }

    private PointPath outlineImpl(PointPath path) {
        //TODO should compute adjusted tangent based on slope at start of width mapping
        return PointPath.from(
                path.getNormal().scaled(width.map(width.getDomain().min)).translate(path.getStart()),
                path.getTangent());
    }

    private Path outlineImpl(Path path, FloatMapping mapping) {
        if (path.isSmooth()) return OffsetCurve.from(path, mapping).getPath();
        FloatRange domain = mapping.getDomain();

        // split up the path at the corners
        Builder builder = SequencePath.builder().withPolicy(Policy.JOIN);
        //TODO seems untidy - getting paths and corners separately
        List<? extends Path> subpaths = path.splitAtCorners().getSubpaths();
        List<Path.Corner> corners = path.byLength().getCorners();
        int cornerCount = corners.size();

        // stitch together offset paths
        boolean closed = path.isClosed();
        if (closed) {
            //TODO
            //TODO check if end width matches start width
            throw new UnsupportedOperationException("TODO: closed path " + path.getClass());
        } else {
            double startP = domain.min;
            int subpathCount = subpaths.size();
            Path previous = null;
            Point previousCenter = null;
            if (subpathCount != cornerCount + 1) {
                throw new IllegalStateException("subpaths: " + subpathCount + "  corners: " + cornerCount);
            }
            for (int i = 0; i < cornerCount; i++) {
                Path.Corner corner = corners.get(i);
                double finishP = corner.getParameter();
                Path subpath = subpaths.get(i);
                Path next = OffsetCurve.from(subpath, submapping(startP, finishP, mapping)).getPath();
                if (previous != null) builder.addPath(joint(previous, next, previousCenter));
                builder.addPath(next);
                previous = next;
                previousCenter = corner.getPoint();
                startP = finishP;
            }
            Path next = OffsetCurve.from(subpaths.get(cornerCount), submapping(startP, domain.max, mapping)).getPath();
            if (previous != null) builder.addPath(joint(previous, next, previousCenter));
            builder.addPath(next);
        }
        return builder.build();
    }

    private Path joint(Path previous, Path next, Point center) {
        //TODO add method to get terminal tangent & points to path?
        PointPath a = previous.byIntrinsic().pointTangentAt(1.0);
        PointPath b = next.byIntrinsic().pointTangentAt(0.0);
        return join.join(a, b, center);
    }

}
