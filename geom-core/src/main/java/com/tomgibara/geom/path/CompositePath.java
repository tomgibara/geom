package com.tomgibara.geom.path;

import java.util.List;

public interface CompositePath extends Path {

    int getSubpathCount();

    Path getFirstPath();

    Path getLastPath();

    List<? extends Path> getSubpaths();

    //TODO necessary?
    Path.Location locateAtLength(double p);

}
