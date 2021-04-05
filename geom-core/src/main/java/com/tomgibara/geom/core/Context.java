package com.tomgibara.geom.core;

import java.text.MessageFormat;

import com.tomgibara.geom.path.Parameterization.ByIntrinsic;
import com.tomgibara.geom.path.Path;
import com.tomgibara.geom.path.PointPath;

//TODO add sync around entering exiting contexts?
public class Context {

	public enum Policy {
		IGNORE,
		LOG_MESSAGE,
		LOG_EXCEPTION,
		THROW_EXCEPTION;
	}

	private static final Context DEFAULT_CONTEXT = new Context();

	//Note: one of root or current is always null
	// current null indicates thread local contexts are used
	// new thread local contexts default to root
	private static Context root = null;
	private static Context current = DEFAULT_CONTEXT;

	private static ThreadLocal<Context> local = new ThreadLocal<>();

	public static void setThreadLocalContextRequired(boolean required) {
		if (required) {
			if (current != null) {
				root = current;
				current = null;
			}
		} else {
			if (current == null) {
				current = root;
				root = null;
			}
		}
	}

	// enter/exit not instance methods because there's not much to gain
	// API must still check if context being used is current context
	public static Context enter(Tolerances tolerances, Policy policy) {
		if (current == null) {
			Context parent = local.get();
			if (parent == null) parent = root;
			Context context = new Context(parent, tolerances, policy);
			local.set(context);
			return context;
		}
		return current = new Context(current, tolerances, policy);
	}

	public static Context exit() {
		final Context parent;
		if (current == null) {
			Context context = local.get();
			parent = context == null ? null : context.parent;
			if (parent != null) local.set(parent);
		} else {
			parent = current.parent;
			if (parent != null) current = parent;
		}
		if (parent == null) throw new IllegalStateException("No context to exit to");
		return parent;
	}

	public static Context currentContext() {
		if (current != null) return current;
		Context context = local.get();
		if (context == null) {
			context = root;
			local.set(context);
		}
		return context;
	}

	private static void log(String message) {
		System.err.println(message);
	}

	private static void log(Throwable t) {
		t.printStackTrace();
	}

	private static boolean isContinuous(float tolerance, Point pt1, Point pt2) {
		return tolerance == 0f ? pt1.equals(pt2) : Norm.L2.powDistanceBetween(pt1, pt2) <= tolerance;
	}

	private final Context parent;
	public final Tolerances tolerances;
	public final Policy policy;

	private Context() {
		parent = null;
		tolerances = Tolerances.defaults();
		policy = Policy.THROW_EXCEPTION;
	}

	private Context(Context parent, Tolerances tolerances, Policy policy) {
		this.parent = parent;
		this.tolerances = tolerances == null ? parent.tolerances : tolerances;
		this.policy = policy == null ? parent.policy : policy;
	}

	public boolean isContinuous(Point pt1, Point pt2) {
		return isContinuous(tolerances.powContinuityTolerance, pt1, pt2);
	}

	public void log(boolean isArgument, String message, Object... params) {
		if (policy == Policy.IGNORE) return;
		String msg = MessageFormat.format(message, params);
		if (policy == Policy.LOG_MESSAGE) {
			log(msg);
			return;
		}
		RuntimeException t = isArgument ? new IllegalArgumentException(msg) : new IllegalStateException(msg);
		if (policy == Policy.LOG_EXCEPTION) {
			log(t);
			return;
		}
		throw t;
	}

	public void checkContinuity(String message, boolean isArgument, Point pt1, Point pt2) {
		if (policy == Policy.IGNORE) return;
		if (!isContinuous(tolerances.powContinuityTolerance, pt1, pt2)) {
			String msg = MessageFormat.format(message, pt1, pt2);
			if (policy == Policy.LOG_MESSAGE) {
				log(msg);
			} else {
				RuntimeException t = isArgument ? new IllegalArgumentException(msg) : new IllegalStateException(msg);
				if (policy == Policy.THROW_EXCEPTION) throw t;
				log(t);
			}
		}
	}

	//TODO limit to curve paths?
	//TODO really need something better
	public Path simplify(Path path) {
		if (path instanceof Linear) return path;
		Point start = path.getStart();
		Point finish = path.getFinish();
		ByIntrinsic intrinsic = path.byIntrinsic();
		if (start.equals(finish)) return intrinsic.pointTangentAt(0f);
		Point halfway = intrinsic.pointAt(0.5f);
		if (isApproxLinear(start, finish, halfway)) return LineSegment.fromPoints(start, finish).getPath();
		return path;
	}

	public boolean isApproxLinear(Path path) {
		return (path instanceof Linear) || testApproxLinear(path);
	}

	public boolean isApproxLinear(Point start, Point finish, Point halfway) {
		float length = Norm.L2.powDistanceBetween(start, finish);
		if (length < tolerances.powShortestNonLinearCurve) return true;
		Point midpoint = Point.Util.midpoint(start, finish);
		float dist = Norm.L2.powDistanceBetween(midpoint, halfway);
		return dist / length < tolerances.powLeastNonLinearDeviation;
	}

	//TODO should change to isSmooth?
	public boolean isCorner(Vector v1, Vector v2) {
		float tolerance = tolerances.getCornerTolerance();
		if (tolerance == 0f) return !v1.equals(v2);
		float dot = v1.dot(v2);
		return Math.abs(1f - dot) > tolerance;
	}

	private boolean testApproxLinear(Path path) {
		Point start = path.getStart();
		Point finish = path.getFinish();
		Point halfway = path.byIntrinsic().pointAt(0.5f);
		return isApproxLinear(start, finish, halfway);
	}

}
