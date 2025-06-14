package com.tomgibara.geom.path;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.transform.Transform;

//TODO should extend SequencePath?
public class SplitPath implements CompositePath {

    private final Path p1;
    private final Path p2;
    private final boolean closed;
    private Parameterizations params = null;

    public SplitPath(Path p1, Path p2, boolean closed) {
        if (p1 == null) throw new IllegalArgumentException("null p1");
        if (p2 == null) throw new IllegalArgumentException("null p2");
        Context.currentContext().checkContinuity("p2 start {1} is not p1 finish {0}", true, p1.getFinish(), p2.getStart());
        this.p1 = p1;
        this.p2 = p2;
        this.closed = closed;
    }

    @Override
    public Point getStart() {
        return p1.getStart();
    }

    public Point getFinish() {
        return p2.getFinish();
    }

    @Override
    public double getLength() {
        return p1.getLength() + p2.getLength();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isRectilinear() {
        return p1.isRectilinear() && p2.isRectilinear();
    }

    @Override
    public boolean isSmooth() {
        return p1.isSmooth() && p2.isSmooth() && isSmoothAtSplit();
    }

    @Override
    public <C> C linearize(Point.Consumer<C> consumer) {
        p1.linearize(consumer);
        return p2.linearize(consumer);
    }

    @Override
    public SimplifiedPath simplify() {
        //note this is safe because SplitPath only has getters
        return new SimplifiedPath((SplitPath) this);
    }

    @Override
    public SplitPath getReverse() {
        return new SplitPath(p2.getReverse(), p1.getReverse(), closed);
    }

    //TODO could produce a more specialized Parameterization
    @Override
    public Parameterization.ByIntrinsic byIntrinsic() {
        return getParams().getByIntrinsic();
    }

    @Override
    public Parameterization.ByLength byLength() {
        return getParams().getByLength();
    }

    //TODO doesn't deal with closed paths
    @Override
    public CompositePath splitAtCorners() {
        if (p1.isSmooth()) {
            if (p2.isSmooth()) {
                if (isSmoothAtSplit()) { // entirely smooth
                    return new SingletonPath(this);
                } else { // smooth except at split
                    return new SplitPath(p1, p2, closed);
                }
            } else {
                List<? extends Path> p2paths = p2.splitAtCorners().getSubpaths();
                if (isSmoothAtSplit()) { // smooth except p2
                    Path first = new SplitPath(p1, p2paths.get(0), false);
                    Path second = new SequencePath(false, p2paths.subList(1, p2paths.size()));
                    return new SplitPath(first, second, closed);
                } else { // only p1 smooth
                    List<Path> paths = new ArrayList<>(p2paths.size() + 1);
                    paths.add(p1);
                    paths.addAll(p2paths);
                    return new SequencePath(closed, paths);
                }
            }
        } else {
            List<? extends Path> p1Paths = p1.splitAtCorners().getSubpaths();
            if (p2.isSmooth()) {
                if (isSmoothAtSplit()) { // smooth except p1
                    int last = p1Paths.size() - 1;
                    Path first = new SequencePath(false, p1Paths.subList(0, last));
                    Path second = new SplitPath(p1Paths.get(last), p2, false);
                    return new SplitPath(first, second, closed);
                } else { // only p2 smooth
                    List<Path> paths = new ArrayList<>(p1Paths.size() + 1);
                    paths.addAll(p1Paths);
                    paths.add(p2);
                    return new SequencePath(closed, paths);
                }
            } else {
                List<? extends Path> p2Paths = p2.splitAtCorners().getSubpaths();
                if (isSmoothAtSplit()) { // smooth only at split
                    int p1Index = p1Paths.size() - 1;
                    List<Path> paths = new ArrayList<>(p1Index + p2Paths.size());
                    paths.addAll(p1Paths.subList(0, p1Index));
                    paths.add(new SplitPath(p1Paths.get(p1Index), p2Paths.get(0), false));
                    paths.addAll(p2Paths.subList(1, p2Paths.size()));
                    return new SequencePath(closed, paths);
                } else { // smooth nowhere
                    List<Path> paths = new ArrayList<>(p1Paths.size() + p2Paths.size());
                    paths.addAll(p1Paths);
                    paths.addAll(p2Paths);
                    return new SequencePath(closed, paths);
                }
            }
        }
    }

    @Override
    public Rect getBounds() {
        return Rect.unionRect(p1.getBounds(), p2.getBounds());
    }

    @Override
    public SplitPath apply(Transform t) {
        if (t == null) throw new IllegalArgumentException("null t");
        if (t.isIdentity()) return this;
        return new SplitPath(p1.apply(t), p2.apply(t), closed);
    }

    @Override
    public PathTraces getGeometry() {
        return new PathTraces(this, p1, p2);
    }

    @Override
    public Path getFirstPath() {
        return p1;
    }

    @Override
    public Path getLastPath() {
        return p2;
    }

    @Override
    public int getSubpathCount() {
        return 2;
    }

    @Override
    public List<? extends Path> getSubpaths() {
        return new PathPair();
    }

    @Override
    public Path.Location locateAtLength(double p) {
        return getParams().locateAtLength(p);
    }

    @Override
    public String toString() {
        return p1 + " + " + p2;
    }

    private class PathPair extends AbstractList<Path> {

        @Override
        public Path get(int index) {
            if (index < 0 || index >= 2) throw new NoSuchElementException();
            return index == 0 ? p1 : p2;
        }

        @Override
        public int size() {
            return 2;
        }

    }

    private Parameterizations getParams() {
        return params == null ? params = new Parameterizations(this, new PathPair()) : params;
    }

    private boolean isSmoothAtSplit() {
        Vector t1 = p1.byIntrinsic().tangentAt(1.0);
        Vector t2 = p2.byIntrinsic().tangentAt(0.0);
        //TODO should have a tolerance here
        return t1.equals(t2);
    }

}
