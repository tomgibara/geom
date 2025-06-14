package com.tomgibara.geom.stroke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.contour.PathContour;
import com.tomgibara.geom.floats.FloatRange;
import com.tomgibara.geom.path.Parameterization.ByIntrinsic;
import com.tomgibara.geom.path.Parameterization.ByLength;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.SequencePath;
import com.tomgibara.geom.path.SequencePath.Builder;
import com.tomgibara.geom.path.SequencePath.Builder.Policy;
import com.tomgibara.geom.path.SplitPath;


//TODO should support two outlines
public class Stroke {

    private final Outline outline;
    private final Cap cap;
    private final Dash dash;

    public Stroke(Outline outline, Cap cap) {
        this(outline, cap, Dash.EVERYWHERE_DASH);
    }

    public Stroke(Outline outline, Cap cap, Dash dash) {
        if (outline == null) throw new IllegalArgumentException("null outline");
        if (cap == null) throw new IllegalArgumentException("null cap");
        if (dash == null) throw new IllegalArgumentException("null dash");

        this.outline = outline;
        this.cap = cap;
        this.dash = dash;
    }

    public Outline getOutline() {
        return outline;
    }

    public Cap getCap() {
        return cap;
    }

    public Dash getDash() {
        return dash;
    }

    public List<Contour> stroke(Path path) {
        Iterator<FloatRange> pattern = dash.getPattern(path);
        //TODO this is unpleasant - is there anything we can do about it?
        if (pattern == Dash.NOWHERE) return Collections.emptyList();
        double length = path.getLength();
        if (pattern == Dash.EVERYWHERE) {
            if (path.isClosed()) {
                if (length == 0.0) {
                    Path polar = outline.outline(path.getStart());
                    if (!polar.isClosed()) {
                        //TODO this makes no sense - caps will have mismatched tangents
                        ByIntrinsic z = polar.byIntrinsic();
                        Path closer = cap.cap(z.pointTangentAt(1.0), z.pointTangentAt(0.0), path.getStart());
                        polar = new SplitPath(polar, closer, true);
                    }
                    return Collections.singletonList((Contour) new PathContour(polar));
                } else {
                    Path outer = outline.outline(path);
                    Path inner = outline.outline(path.getReverse());
                    return Arrays.asList(new Contour[] { new PathContour(outer), new PathContour(inner) });
                }
            } else {
                return Collections.singletonList(strokeEverywhere(path, length, null));
            }
        } else {
            if (length == 0.0) {
                if (!pattern.hasNext()) return Collections.emptyList();
                FloatRange range = pattern.next();
                if (!range.containsValue(0.0)) return Collections.emptyList();
                return Collections.singletonList(strokeEverywhere(path, length, null));
            } else {
                List<Contour> list = new ArrayList<>();
                //ByLength byLength = path.byLength();
                while (pattern.hasNext()) {
                    FloatRange range = pattern.next();
                    //TODO how should terminal be joined up?
                    if (range.min >= length) break;
                    boolean last = range.max >= length;
                    if (last) range = range.intersectionAsRange(new FloatRange(0, length));
                    list.add(strokeEverywhere(path, length, range));
                    if (last) break;
                }
                return list;
            }
        }
    }

    private Contour strokeEverywhere(Path path, double pathLength, FloatRange segment) {
        if (pathLength < 0) pathLength = path.getLength();
        //TODO outline needs splitting in half? or reversing?
        Path outer;
        Path inner;
        if (segment == null) {
            outer = outline.outline(path);
            inner = outline.getReverse().outline(path.getReverse());
        } else {
            outer = outline.outline(path, segment);
            if (pathLength < 0) pathLength = path.getLength();
            inner = outline.getReverse().outline(path.getReverse(), new FloatRange(pathLength - segment.max, pathLength - segment.min));
        }
        ByLength z = path.byLength(); //TODO could avoid this if path was segmented before being passed for outlining
        ByIntrinsic outerZ = outer.byIntrinsic();
        ByIntrinsic innerZ = inner.byIntrinsic();
        Builder builder = SequencePath.builder().withPolicy(Policy.IGNORE);
        if (pathLength > 0.0) builder.addPath(outer);
        Path capPath1 = cap.cap(outerZ.pointTangentAt(1.0), innerZ.pointTangentAt(0.0), z.pointAt(segment == null ? pathLength : segment.max));
        if (capPath1 != null) builder.addPath(capPath1);
        if (pathLength > 0.0) builder.addPath(inner);
        Path capPath2 = cap.cap(innerZ.pointTangentAt(1.0), outerZ.pointTangentAt(0.0), z.pointAt(segment == null ? 0.0 : segment.min));
        if (capPath2 != null) builder.addPath(capPath2);
        return new PathContour(builder.closeAndBuild());
    }

}
