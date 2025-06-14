package com.tomgibara.geom.path;

import java.util.List;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;

public interface Parameterization {

    Path getPath();

    //TODO rename, newLocation maybe?
    Path.Location location();

    // duplicated for efficiency (to avoid creating intermediate path locations)

    Point pointAt(double p);

    Vector tangentAt(double p);

    PointPath pointTangentAt(double p);

    SplitPath splitAt(double p);

    Path segment(double minP, double maxP);

    double parameterNearest(Point p);

    List<Path.Corner> getCorners();

    public interface ByIntrinsic extends Parameterization {

        double lengthAt(double p);

    }

    public interface ByLength extends Parameterization {

        double intrinsicAt(double p);

    }

}
