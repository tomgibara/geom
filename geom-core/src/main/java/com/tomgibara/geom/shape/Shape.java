package com.tomgibara.geom.shape;

import java.util.List;

import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.core.GeomUtil;
import com.tomgibara.geom.core.Geometric;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.helper.Winder;
import com.tomgibara.geom.transform.Transform;

public final class Shape implements Geometric {

    private final Contour[] contours;
    private final WindingRule windingRule;
    private List<Contour> publicContours = null;
    private Rect bounds = null;

    public Shape(WindingRule windingRule, Contour contour) {
        if (windingRule == null) throw new IllegalArgumentException("null winding Rule");
        if (contour == null) throw new IllegalArgumentException("null contour");

        this.windingRule = windingRule;
        this.contours = new Contour[] { contour };
    }

    public Shape(WindingRule windingRule, Contour... contours) {
        if (windingRule == null) throw new IllegalArgumentException("null winding Rule");
        if (contours == null) throw new IllegalArgumentException("null contours");

        this.windingRule = windingRule;
        for (Contour contour : contours) {
            if (contour == null) throw new IllegalArgumentException("null contour");
        }
        this.contours = contours.clone();
    }

    public Shape(WindingRule windingRule, List<Contour> contours) {
        if (windingRule == null) throw new IllegalArgumentException("null winding Rule");
        if (contours == null) throw new IllegalArgumentException("null contours");

        this.windingRule = windingRule;
        if (contours.contains(null)) throw new IllegalArgumentException("null contour");
        this.contours = (Contour[]) contours.toArray(new Contour[contours.size()]);
    }

    public List<Contour> getContours() {
        return publicContours == null ? publicContours = GeomUtil.asList(contours) : publicContours;
    }

    public WindingRule getWindingRule() {
        return windingRule;
    }

    public boolean containsPoint(Point pt) {
        if (pt == null) throw new IllegalArgumentException("null pt");
        //TODO cache this object?
        Winder winder = new Winder();
        int windingNumber = 0;
        for (int i = 0; i < contours.length; i++) {
            windingNumber += winder.countWindings(contours[i], pt);
        }
        return windingRule.isInterior(windingNumber);
    }

    @Override
    public Rect getBounds() {
        return bounds == null ? bounds = computeBounds() : bounds;
    }

    @Override
    public Shape apply(Transform t) {
        if (t.isIdentity()) return this;
        Contour[] contours = this.contours.clone();
        for (int i = 0; i < contours.length; i++) {
            Contour contour = contours[i];
            contours[i] = contour.apply(t);
        }
        //TODO ideally need an additional constructor which doesn't recheck array
        return new Shape(windingRule, contours);
    }

    private Rect computeBounds() {
        Rect bounds = contours[0].getBounds();
        for (int i = 1; i < contours.length; i++) {
            bounds = Rect.unionRect(bounds, contours[i].getBounds());
        }
        return bounds;
    }
}
