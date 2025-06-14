package com.tomgibara.geom.contour;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.curve.CurvePath;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.transform.Transform;

public class EllipseContour implements Contour {

    private final Ellipse geom;
    private final boolean positiveRotation;

    public EllipseContour(Ellipse geom, boolean positiveRotation) {
        if (geom == null) throw new IllegalArgumentException("null geom");
        this.geom = geom;
        this.positiveRotation = positiveRotation;
    }

    @Override
    public Rect getBounds() {
        return geom.getBounds();
    }

    @Override
    public EllipseContour apply(Transform t) {
        return t.isIdentity() ? this : new EllipseContour(geom.apply(t), positiveRotation);
    }

    @Override
    public CurvePath getPath() {
        //TODO should be cached?
        return geom.completeArc().getPath();
    }

    @Override
    public Path getPathStartingAt(Point pt) {
        // TODO best implemented when ellipse can compute angles
        throw new UnsupportedOperationException();
    }

    @Override
    public EllipseContour getReverse() {
        return new EllipseContour(geom, !positiveRotation);
    }

    @Override
    public Ellipse getGeometry() {
        return geom;
    }

}
