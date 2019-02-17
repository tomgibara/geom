package com.tomgibara.geom.path;

import java.util.List;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;

public interface Parameterization {

	Path getPath();

	//TODO rename, newLocation maybe?
	Path.Location location();

	// duplicated for efficiency (to avoid creating intermediate path locations)

	Point pointAt(float p);

	Vector tangentAt(float p);

	PointPath pointTangentAt(float p);

	SplitPath splitAt(float p);

	Path segment(float minP, float maxP);

	float parameterNearest(Point p);

	List<Path.Corner> getCorners();

	public interface ByIntrinsic extends Parameterization {

		float lengthAt(float p);

	}

	public interface ByLength extends Parameterization {

		float intrinsicAt(float p);

	}

}
