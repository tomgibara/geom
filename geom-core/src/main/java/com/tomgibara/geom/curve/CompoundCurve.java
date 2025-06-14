package com.tomgibara.geom.curve;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.SplitPath;

//TODO remove orthogonal
public final class CompoundCurve extends Curve {

    public enum Construction {
        ORTHOGONAL,
        ROTATED,
        UNROTATED;
    }

    private final Curve mainCurve;
    //TODO should treat point curve specially - allows derivative to pass through from mainCurve
    private final Path offsetPath;
    private final Construction construction;

    public CompoundCurve(Curve mainCurve, Path offsetPath, Construction construction) {
        this.mainCurve = mainCurve;
        this.offsetPath = offsetPath;
        this.construction = construction;
    }

    public Curve getMainCurve() {
        return mainCurve;
    }

    public Path getOffsetPath() {
        return offsetPath;
    }

    @Override
    public Point pointAt(double t) {
        Vector v = offsetPath.byIntrinsic().pointAt(t).vectorFromOrigin();
        switch (construction) {
        case ORTHOGONAL:
            v = normalAt(t).scaled(v.getMagnitude());
            break;
        case ROTATED:
            v = normalAt(t).asRotation().transform(v);
            break;
        case UNROTATED:
            /*noop*/
            break;
        }
        return v.translate(mainCurve.pointAt(t));
    }

    @Override
    public SplitCurvePath splitAt(double p) {
        SplitCurvePath mainSplit = mainCurve.splitAt(p);
        SplitPath offsetSplit = offsetPath.byIntrinsic().splitAt(p);
        CompoundCurve firstCurve = new CompoundCurve(mainSplit.getFirstPath().getCurve(), offsetSplit.getFirstPath(), construction);
        CompoundCurve secondCurve = new CompoundCurve(mainSplit.getLastPath().getCurve(), offsetSplit.getLastPath(), construction);
        return new SplitCurvePath(firstCurve.getPath(), secondCurve.getPath(), getPath().isClosed());
    }

    private Vector normalAt(double t) {
        return mainCurve.getDerivative().pointAt(t).vectorFromOrigin().normalized().rotateThroughRightAngles(1);
    }

}
