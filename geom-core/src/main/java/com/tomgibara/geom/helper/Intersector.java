package com.tomgibara.geom.helper;

import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.LinearPath;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.SimplifiedPath;
import com.tomgibara.geom.path.SplitPath;

public class Intersector {

    private final Point.Consumer<?> consumer;
    private final boolean reportingPreviousVertex;
    private final boolean reportingNextVertex;
    //TODO should remove this? in theory two successive intersections could occur at the same point
    private Point lastPoint = null;
    private int boundsChecks = 0;
    private int rectChecks = 0;
    private int lineChecks = 0;

    public Intersector(Point.Consumer<?> consumer) {
        this(consumer, false, false);
    }

    public Intersector(Point.Consumer<?> consumer, boolean reportingNextVertex, boolean reportingPreviousVertex) {
        if (consumer == null) throw new IllegalArgumentException("null consumer");
        this.consumer = consumer;
        this.reportingNextVertex = reportingNextVertex;
        this.reportingPreviousVertex = reportingPreviousVertex;
    }

    public boolean isReportingNextVertex() {
        return reportingNextVertex;
    }

    public boolean isReportingPreviousVertex() {
        return reportingPreviousVertex;
    }

    //TODO consider returning the consumer here
    // finds intersections bwtween two paths in order of first path
    public void intersect(Path p1, Path p2) {
        if (p1 == null) throw new IllegalArgumentException("null p1");
        if (p2 == null) throw new IllegalArgumentException("null p2");
        doIntersect(p1, p2);
    }

    public int getBoundsChecks() {
        return boundsChecks;
    }

    public int getRectChecks() {
        return rectChecks;
    }

    public int getLineChecks() {
        return lineChecks;
    }

    private void doIntersect(Path p1, Path p2) {
        boundsChecks++;
        Rect b1 = p1.getBounds();
        Rect b2 = p2.getBounds();
        if (!Rect.rectsIntersect(b1, b2)) return;

        SimplifiedPath s1 = p1.simplify();
        SimplifiedPath s2 = p2.simplify();
        if (s1.isLinear() && s2.isLinear()) {
            doIntersect(s1.getLinear(), s2.getLinear());
        } else if (s1.isLinear()) {
            doIntersect(s1.getLinear(), s2.getSplit());
        } else if (s2.isLinear()) {
            doIntersect(s1.getSplit(), s2.getLinear());
        } else {
            doIntersect(s1.getSplit(), s2.getSplit());
        }
    }

    private void doIntersect(SplitPath j1, SplitPath j2) {
        doIntersect(j1.getFirstPath(), j2.getFirstPath());
        doIntersect(j1.getFirstPath(), j2.getLastPath());
        doIntersect(j1.getLastPath(), j2.getFirstPath());
        doIntersect(j1.getLastPath(), j2.getLastPath());
    }

    private void doIntersect(LinearPath d1, Path p2) {
        rectChecks++;
        if (!d1.getSegment().intersectsRect(p2.getBounds())) return;

        SimplifiedPath s2 = p2.simplify();
        if (s2.isLinear()) {
            doIntersect(d1, s2.getLinear());
        } else {
            doIntersect(d1, s2.getSplit());
        }
    }

    private void doIntersect(LinearPath d1, SplitPath j2) {
        doIntersect(d1, j2.getFirstPath());
        doIntersect(d1, j2.getLastPath());
    }

    private void doIntersect(Path p1, LinearPath d2) {
        rectChecks++;
        if (!d2.getSegment().intersectsRect(p1.getBounds())) return;

        SimplifiedPath s1 = p1.simplify();
        if (s1.isLinear()) {
            doIntersect(s1.getLinear(), d2);
        } else {
            doIntersect(s1.getSplit(), d2);
        }

    }

    private void doIntersect(SplitPath j1, LinearPath d2) {
        doIntersect(j1.getFirstPath(), d2);
        doIntersect(j1.getLastPath(), d2);
    }

    private void doIntersect(LinearPath d1, LinearPath d2) {
        Point pt = d1.getSegment().intersectionWith(d2.getSegment());
        lineChecks++;
        if (pt == null || lastPoint != null && lastPoint.equals(pt)) return;
        if (isReportingPreviousVertex()) consumer.addPoint(d1.getStart());
        consumer.addPoint(pt);
        if (isReportingNextVertex()) consumer.addPoint(d1.getFinish());
        lastPoint = pt;
    }


}
