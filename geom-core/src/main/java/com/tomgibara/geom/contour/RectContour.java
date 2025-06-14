package com.tomgibara.geom.contour;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PolygonalPath;
import com.tomgibara.geom.path.RectPath;
import com.tomgibara.geom.transform.Transform;

//TODO would be nice to be able to optimize the nearestPoint queries on the path returned
public class RectContour implements Contour {

    private final RectPath path;

    public RectContour(RectPath path) {
        if (path == null) throw new IllegalArgumentException("null path");
        this.path = path;
    }

    //TODO consider placing on a Chiral interface?
    public boolean isPositiveRotation() {
        return path.isPositiveRotation();
    }

    @Override
    public Rect getBounds() {
        return path.getBounds();
    }

    @Override
    public Contour apply(Transform t) {
        Path p = path.apply(t);
        if (p instanceof RectPath) return new RectContour((RectPath) p);
        return new PathContour(p);
    }

    @Override
    public RectPath getPath() {
        return path;
    }

    @Override
    public PolygonalPath getPathStartingAt(Point pt) {
        pt = path.getBounds().nearestPointTo(pt, true);
        int pos = positionOf(pt);
        PolygonalPath.Builder builder = PolygonalPath.builder();
        if (pos < 0) {
            builder.addPoint(pt);
            pos = path.isPositiveRotation() ? pos + 4 : -1 - pos;
        }
        return buildPath(builder, pos).closeAndBuild();
    }

    @Override
    public Contour getReverse() {
        return new RectContour( RectPath.fromRect(path.getBounds(), !path.isPositiveRotation()) );
    }

    @Override
    public Rect getGeometry() {
        return path.getBounds();
    }

    //  0 TR, 1 BR, 2 BL, 3 TL
    // -1 T, -2 L, -3 B, -4 R
    private int positionOf(Point pt) {
        double x = pt.x;
        double y = pt.y;
        Rect rect = path.getBounds();
        if (x == rect.minX) {
            if (y == rect.minY) return 3;
            if (y == rect.maxY) return 2;
            return -2;
        }
        if (x == rect.maxX) {
            if (y == rect.minY) return 0;
            if (y == rect.maxY) return 1;
            return -4;
        }

        if (y == rect.minY) return -1;
        if (y == rect.maxY) return -3;

        throw new IllegalArgumentException("invalid pt");
    }

    private PolygonalPath.Builder buildPath(PolygonalPath.Builder builder, int pos) {
        int i = pos;
        int d = path.isPositiveRotation() ? -1 : 1;
        Rect rect = path.getBounds();
        do {
            switch (i) {
            case 0 : builder.addPoint(rect.maxX, rect.minY); break;
            case 1 : builder.addPoint(rect.minX, rect.minY); break;
            case 2 : builder.addPoint(rect.minX, rect.maxY); break;
            case 3 : builder.addPoint(rect.maxX, rect.maxY); break;
            }
            i = (i + d) & 3;
        } while (i != pos);
        return builder;
    }

}
