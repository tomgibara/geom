package com.tomgibara.geom.helper;

import com.tomgibara.geom.core.LinearPath;
import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.SimplifiedPath;
import com.tomgibara.geom.path.SplitPath;

public class Locator {

	private final Path path;
	private final Norm norm = Norm.L2;

	public Locator(Path path) {
		if (path == null) throw new IllegalArgumentException("null path");
		this.path = path;
	}

	public float getNearestLengthAlongPath(Point pt) {
		if (pt == null) throw new IllegalArgumentException("null pt");
		Stack stack = evaluate(pt, new Stack(path));
		if (stack == null) throw new IllegalStateException();
		float d = stack.parameter;
		stack = stack.stack;
		while (stack != null) {
			float length = stack.path.getLength();
			d += length;
			stack = stack.stack;
		}
		return d;
	}
	// stack needs to represent all ancestors, in order
	// return the best thing you can find or null if you can't beat the supplied stack
	private Stack evaluate(Point pt, Stack stack) {
		SimplifiedPath simplified = stack.path.simplify();
		if (simplified.isLinear()) {
			LinearPath linear = simplified.getLinear();
			Point nearest = linear.nearestPointTo(pt);
			float dist = norm.distanceBetween(nearest, pt);
			if (dist <= stack.distance) return stack.replaceMatch(linear, dist, norm.distanceBetween(nearest, linear.getStart()));
			return null;
		} else {
			SplitPath split = simplified.getSplit();
			Path first = split.getFirstPath();
			Path second = split.getLastPath();
			float firstDistLB = norm.distanceBetween(pt, first.getBounds().nearestPointTo(pt, false));
			float secondDistLB = norm.distanceBetween(pt, second.getBounds().nearestPointTo(pt, false));
			boolean firstPossible = firstDistLB <= stack.distance;
			boolean secondPossible = secondDistLB <= stack.distance;
			if (!firstPossible && !secondPossible) return null;
			float firstDistUB = norm.distanceBetween(pt, first.getBounds().furthestPointTo(pt));
			if (firstPossible && !secondPossible) {
				return evaluate(pt, stack.replacePossible(first, firstDistUB));
			}
			float secondDistUB = norm.distanceBetween(pt, second.getBounds().furthestPointTo(pt));
			if (!firstPossible) {
				stack = stack.replacePossible(first, firstDistUB);
				return evaluate(pt, stack.addPossible(second, secondDistUB));
			}
			stack = stack.replacePossible(first, firstDistUB);
			Stack s1 = evaluate(pt, stack);
			if (s1 == null) {
				return evaluate(pt, stack.addPossible(second, secondDistUB));
			}
			if (secondDistLB > s1.distance) return s1;
			Stack s2 = evaluate(pt, stack.addPossible(second, Math.min(s1.distance, secondDistUB)));
			return s2 == null ? s1 : s2;
		}
	}

	private static final class Stack {

		final Stack stack;
		final Path path;
		final float distance;
		final float parameter; // -1 if distance is just an upper bound

		Stack(Path path) {
			this.path = path;
			stack = null;
			distance = Float.POSITIVE_INFINITY;
			parameter = -1f;
		}

		private Stack(Stack stack, Path path, float distance, float parameter) {
			this.stack = stack;
			this.path = path;
			this.distance = distance;
			this.parameter = parameter;
		}

		Stack addPossible(Path path, float distance) {
			return new Stack(this, path, Math.min(distance, this.distance), -1f);
		}

		Stack replacePossible(Path path, float distance) {
			return new Stack(stack, path,  Math.min(distance, this.distance), -1f);
		}

		Stack replaceMatch(Path path, float distance, float parameter) {
			return new Stack(stack, path,  distance, parameter);
		}

	}

}
