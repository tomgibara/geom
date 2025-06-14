package com.tomgibara.geom.path;

import java.util.List;

import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Traceable;
import com.tomgibara.geom.path.SequencePath.Builder;
import com.tomgibara.geom.transform.Transform;

//TODO flatten contained sequences
public final class PathTraces implements Traceable {

    private final Traceable[] traces;

    private Path path;
    private Rect bounds = null;

    PathTraces(PathTraces that, Transform t) {
        traces = new Traceable[that.traces.length];
        for (int i = 0; i < traces.length; i++) {
            traces[i] = that.traces[i].apply(t);
        }
        path = null;
    }

    PathTraces(SequencePath path, Path[] paths) {
        this.path = path;
        this.traces = new Traceable[paths.length];
        for (int i = 0; i < traces.length; i++) {
            this.traces[i] = paths[i].getGeometry();
        }
    }

    PathTraces(SplitPath path, Path path1, Path path2) {
        this.path = path;
        traces = new Traceable[2];
        traces[0] = path1.getGeometry();
        traces[1] = path2.getGeometry();
    }

    PathTraces(PolygonalPath path, List<LineSegment> segments, Rect bounds) {
        this.path = path;
        this.traces = (Traceable[]) segments.toArray(new Traceable[segments.size()]);
    }

    PathTraces(RectPath path, LineSegment[] segments) {
        this.path = path;
        this.traces = segments;
        this.bounds = path.getBounds();
    }

    @Override
    public Rect getBounds() {
        if (bounds == null) {
            bounds = traces[0].getBounds();
            for (int i = 1; i < traces.length; i++) {
                bounds = Rect.unionRect(bounds, traces[i].getBounds());
            }
        }
        return bounds;
    }

    @Override
    public Traceable apply(Transform transform) {
        if (transform == null) throw new IllegalArgumentException("null transform");
        return transform.isIdentity() ? this : new PathTraces(this, transform);
    }

    @Override
    public Path getPath() {
        if (path == null) {
            Builder builder = SequencePath.builder();
            for (Traceable trace : traces) {
                builder.addPath(trace.getPath());
            }
            path = builder.build();
        }
        return path;
    }

}
