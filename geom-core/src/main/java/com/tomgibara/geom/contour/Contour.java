package com.tomgibara.geom.contour;

import com.tomgibara.geom.core.Geometric;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.transform.Transform;

public interface Contour extends Geometric {

    @Override
    Contour apply(Transform t);

    Path getPath();

    Path getPathStartingAt(Point pt);

    Contour getReverse();

    Geometric getGeometry();
}
