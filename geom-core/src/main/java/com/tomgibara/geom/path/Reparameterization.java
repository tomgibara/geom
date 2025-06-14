package com.tomgibara.geom.path;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;

public abstract class Reparameterization implements Parameterization {

    private final Parameterization z;

    private Reparameterization(Parameterization z) {
        this.z = z;
    }

    @Override
    public Path getPath() {
        return z.getPath();
    }

    @Override
    public Point pointAt(double p) {
        return z.pointAt(map(p));
    }

    @Override
    public Vector tangentAt(double p) {
        return z.tangentAt(map(p));
    }

    @Override
    public PointPath pointTangentAt(double p) {
        return z.pointTangentAt(map(p));
    }

    @Override
    public SplitPath splitAt(double p) {
        return z.splitAt(map(p));
    }

    @Override
    public Path segment(double minP, double maxP) {
        return z.segment(map(minP), map(maxP));
    }

    @Override
    public double parameterNearest(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        return unmap(z.parameterNearest(pt));
    }

    @Override
    public Path.Location location() {
        return new Path.Location(this, 0.0);
    }

    @Override
    public List<Path.Corner> getCorners() {
        List<Path.Corner> corners = z.getCorners();
        switch (corners.size()) {
        case 0 : return Path.Corner.NO_CORNERS;
        case 1 : return Collections.singletonList(unmap(corners.get(0)));
        default:
            Path.Corner[] array = new Path.Corner[corners.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = unmap(corners.get(i));
            }
            return Collections.unmodifiableList(Arrays.asList(array));
        }
    }

    //TODO could this default to length mapping?
    protected abstract double map(double p);

    protected abstract double unmap(double q);

    private Path.Corner unmap(Path.Corner corner) {
        return new Path.Corner(this, unmap(corner.getParameter()), corner.getPoint(), corner.getStartTangent(), corner.getFinishTangent());
    }

    public static abstract class ByIntrinsic extends Reparameterization implements Parameterization.ByIntrinsic {

        public ByIntrinsic(Parameterization z) {
            super(z);
        }

    }

    public static abstract class ByLength extends Reparameterization implements Parameterization.ByLength {

        public ByLength(Parameterization z) {
            super(z);
        }

    }

}
