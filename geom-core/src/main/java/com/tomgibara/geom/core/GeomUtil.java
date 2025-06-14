package com.tomgibara.geom.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.contour.Contour;
import com.tomgibara.geom.path.Path;

public class GeomUtil {

    private static final ThreadLocal<double[]> workingFloats = new ThreadLocal<>();

    public static List<Point> asList(Point... points) {
        return Collections.unmodifiableList( Arrays.asList(points) );
    }

    public static List<Contour> asList(Contour... contours) {
        return Collections.unmodifiableList( Arrays.asList(contours) );
    }

    public static Path[] asArray(List<? extends Path> paths) {
        if (paths == null) throw new IllegalArgumentException("null paths");
        return paths.toArray(new Path[paths.size()]);
    }

    public static double[] workingFloats(int length) {
        if (length < 0) throw new IllegalArgumentException("negative length");
        double[] fs = workingFloats.get();
        if (fs == null || fs.length < length) {
            fs = new double[length];
            workingFloats.set(fs);
        }
        return fs;
    }

    public static void reversePointArray(double[] fs) {
        int i = 0;
        int j = fs.length - 1;
        while (j > i) {
            double t = fs[i];
            fs[i++] = fs[j];
            fs[j--] = t;
        }
    }

    public static void reverseArray(Object[] objs) {
        int i = 0;
        int j = objs.length - 1;
        while (j > i) {
            Object t = objs[i];
            objs[i++] = objs[j];
            objs[j--] = t;
        }
    }

    public static Point[] asPoints(double... fs) {
        if (fs == null) throw new IllegalArgumentException("null fs");
        if ((fs.length & 1) != 0) throw new IllegalArgumentException("uneven length");
        Point[] points = new Point[fs.length / 2];
        for (int i = 0, j = 0; i < points.length; i++, j++) {
            points[i] = new Point(fs[j], fs[++j]);
        }
        return points;
    }

}
