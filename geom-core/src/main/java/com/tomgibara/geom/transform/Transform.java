package com.tomgibara.geom.transform;

import java.util.ArrayList;
import java.util.List;

import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Norm;
import com.tomgibara.geom.core.Offset;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;

//TODO add reflections, rotations around point
public final class Transform implements Transformable {

	private static final int ORIGIN_PRESERVING = 1; //  m02 & m12 == 0
	private static final int SKEW_PRESERVING = 2;   //  m10 & m01 == 0
	private static final int SCALE_PRESERVING = 4;  //  m00 * m11 - m10 * m01 == 1

	private static final int APPLY_MASK          = ORIGIN_PRESERVING | SKEW_PRESERVING | SCALE_PRESERVING;

	private static final int CHIRAL_PRESERVING = 8;  //  m00 * m11 - m10 * m01 > 0
	private static final int CIRCLE_PRESERVING = 16;  //  m10 == -m01 and m00 == m11
	private static final int RECTILNEAR_PRESERVING = 32; // perpendicular only rotation

	private static final int RIGHT_MASK =    ORIGIN_PRESERVING | /*maybe skew p*/  SCALE_PRESERVING | CHIRAL_PRESERVING | CIRCLE_PRESERVING | RECTILNEAR_PRESERVING;
	private static final int IDENTITY_MASK = RIGHT_MASK | SKEW_PRESERVING;

	private static final Transform IDENTITY = new Transform( 1f,  0f,  0f,  1f,  0f,  0f, IDENTITY_MASK );
	private static final Transform ROT_90 =   new Transform( 0f, -1f,  1f,  0f,  0f,  0f, RIGHT_MASK    );
	private static final Transform ROT_180 =  new Transform(-1f,  0f,  0f, -1f,  0f,  0f, IDENTITY_MASK );
	private static final Transform ROT_270 =  new Transform( 0f,  1f,  0f, -1f,  0f,  0f, RIGHT_MASK    );

	public static Transform identity() {
		return IDENTITY;
	}

	public static Transform rotateRightAngles(int quarterTurns) {
		switch (quarterTurns & 3) {
		case  0 : return IDENTITY;
		case  1 : return ROT_90;
		case  2 : return ROT_180;
		case  3 : return ROT_270;
		default : throw new IllegalStateException();
		}
	}

	public static Transform translation(float x, float y) {
		if (x == 0f && y == 0f) return IDENTITY;
		return new Transform(1f, 0f, 0f, 1f, x, y, SKEW_PRESERVING | SCALE_PRESERVING | CHIRAL_PRESERVING | CIRCLE_PRESERVING | RECTILNEAR_PRESERVING);
	}

	public static Transform rotation(float angle) {
		if (angle == 0f) return IDENTITY;

		int flags = ORIGIN_PRESERVING | CHIRAL_PRESERVING | CIRCLE_PRESERVING | SCALE_PRESERVING;
		// TODO trap right angles and set flags
		float s = (float) Math.sin(angle);
		float c = (float) Math.cos(angle);
		if (s == 0f) {
			switch (Math.round(c)) {
			case -1 : return IDENTITY;
			case  1 : return ROT_180;
			/* shouldn't be possible - but safe to fall through */
			}
		} else if (c == 0f) {
			switch (Math.round(s)) {
			case -1 : return ROT_270;
			case  1 : return ROT_90;
			/* shouldn't be possible - but safe to fall through */
			}
		}
		return new Transform(c, s, -s, c, 0f, 0f, flags);
	}

	public static Transform rotationAbout(Point pt, float angle) {
		// trivial cases
		if (pt.isOrigin()) return rotation(angle);
		if (angle == 0f) return IDENTITY;
		//TODO make an optimize version of this
		Transform a = Transform.translation(-pt.x, -pt.y);
		Transform b = Transform.rotation(angle);
		Transform c = Transform.translation(pt.x, pt.y);
		return a.apply(b).apply(c);
	}


	public static Transform rotationAndScale(Vector v) {
		if (v == null) throw new IllegalArgumentException("null v");
		if (v.isZero()) throw new IllegalArgumentException("zero v");
		int flags = ORIGIN_PRESERVING | CIRCLE_PRESERVING | CHIRAL_PRESERVING;
		if (v.x == 0f) {
			flags |= RECTILNEAR_PRESERVING;
		} else if (v.y == 0f) {
			 flags |= RECTILNEAR_PRESERVING | SKEW_PRESERVING;
		}
		return new Transform(v.x, v.y, -v.y, v.x, 0f, 0f, flags);
	}

	// TODO should allow zero scales?
	public static Transform scale(float s) {
		if (s == 0f) throw new IllegalArgumentException("non-invertible");
		if (s == 1f) return IDENTITY;
		int flags = ORIGIN_PRESERVING | SKEW_PRESERVING | CHIRAL_PRESERVING | CIRCLE_PRESERVING | RECTILNEAR_PRESERVING;
		if (s == -1f) flags |= SCALE_PRESERVING;
		return new Transform(s, 0, 0, s, 0, 0, flags);
	}

	public static Transform scale(float sx, float sy) {
		if (sx == sy) return scale(sx);
		return scaleAbout(Point.ORIGIN, sx, sy);
	}

	public static Transform scaleAbout(Point pt, float sx, float sy) {
		if (sx == 0f || sy == 0f) throw new IllegalArgumentException("non-invertible");
		if (sx == 1f && sy == 1f) return IDENTITY;
		int flags = SKEW_PRESERVING | RECTILNEAR_PRESERVING;
		if (pt.isOrigin()) flags |= ORIGIN_PRESERVING;
		if (sx > 0 == sy > 0) flags |= CHIRAL_PRESERVING;
		return new Transform(sx, 0, 0, sy, (1f - sx) * pt.x, (1f - sy) * pt.y, flags);
	}

	public static Transform translateAndScale(Rect from, Rect to) {
		if (from.isDegenerate()) throw new IllegalArgumentException("from degenerate");
		if (to.isDegenerate()) throw new IllegalArgumentException("to degenerate");

		float wf = from.getWidth();
		float hf = from.getHeight();
		float wt = to.getWidth();
		float ht = to.getHeight();
		Point cf = from.getCenter();
		Point ct = to.getCenter();

		boolean op = cf.equals(ct);

		// no scaling case
		if (wf == wt && hf == ht) {
			return op ? IDENTITY :
				new Transform(1f, 0f, 0f, 1f, ct.x - cf.x, ct.y - cf.y,
						SKEW_PRESERVING | RECTILNEAR_PRESERVING | CHIRAL_PRESERVING | SCALE_PRESERVING | CIRCLE_PRESERVING
						);
		} else {
			float sx = wt/wf;
			float sy = ht/hf;
			//TODO could add circle preserving if sx == sy?
			return op ?
				new Transform(sx, 0f, 0f, sy, 0f, 0f,
						SKEW_PRESERVING | RECTILNEAR_PRESERVING | CHIRAL_PRESERVING | ORIGIN_PRESERVING
						) :
				new Transform(sx, 0f, 0f, sy, ct.x - cf.x * sx, ct.y - cf.y * sy,
						SKEW_PRESERVING | RECTILNEAR_PRESERVING | CHIRAL_PRESERVING
						);
		}
	}

	public static Transform skew(float sx, float sy) {
		float p = sx * sy;
		if (p == 1f) throw new IllegalArgumentException("non-invertible");
		if (sx == 0f && sy == 0f) return IDENTITY;
		int flags = ORIGIN_PRESERVING;
		if (p < 1) flags |= CHIRAL_PRESERVING;
		if (sx == -sy) flags |= CIRCLE_PRESERVING;
		if (sx == 0 || sy == 0) flags |= SCALE_PRESERVING;
		return new Transform(1, sy, sx, 1, 0, 0, flags);
	}

	public static Transform components(float m00, float m10, float m01, float m11, float m02, float m12) {
		return new Transform(m00, m10, m01, m11, m02, m12);
	}

	private final int flags;

	public final float m00; // scale x
	public final float m10; // shear y
	public final float m01; // shear x
	public final float m11; // scale y
	public final float m02; // translate x
	public final float m12; // translate y

	private Transform(float m00, float m10, float m01, float m11, float m02, float m12) {
		this.m00 = m00;
		this.m10 = m10;
		this.m01 = m01;
		this.m11 = m11;
		this.m02 = m02;
		this.m12 = m12;

		float det = m00 * m11 - m10 * m01;
		if (det == 0f) throw new IllegalArgumentException("non-invertible transform");
		if (Float.isInfinite(det) || Float.isInfinite(m02) || Float.isInfinite(m12)) throw new IllegalArgumentException("overflowing transform");
		if (Float.isNaN(det) || Float.isNaN(m02) || Float.isNaN(m12)) throw new IllegalArgumentException("invalid transform");

		int flags = 0;
		if (m02 == 0f && m12 == 0f) flags |= ORIGIN_PRESERVING;
		if (m10 == 0f && m01 == 0f) flags |= SKEW_PRESERVING;
		if (Math.abs(det) == 1) flags |= SCALE_PRESERVING;
		if (m10 == -m01 && m00 == m11) flags |= CIRCLE_PRESERVING;
		if (det < 0f) flags |= CHIRAL_PRESERVING;
		if (m10 == 0f && m01 == 0f) flags |= RECTILNEAR_PRESERVING;
		this.flags = flags;
	}

	private Transform(float m00, float m10, float m01, float m11, float m02, float m12, int flags) {
		this.m00 = m00;
		this.m10 = m10;
		this.m01 = m01;
		this.m11 = m11;
		this.m02 = m02;
		this.m12 = m12;
		this.flags = flags;
	}

	public float[] getComponents() {
		return new float[] { m00, m10, m01, m11, m02, m12 };
	}

	public float getTrace() {
		return m00 + m11;
	}

	public float getDeterminant() {
		return m00 * m11 - m10 * m01;
	}

	public Vector getColumn(int index) {
		switch (index) {
		case 0 : return new Vector(m00, m10);
		case 1 : return new Vector(m01, m11);
		case 2 : return new Vector(m02, m12);
		default: throw new IllegalArgumentException("invalid index");
		}
	}

	//TODO optimize cases
	public Transform getInverse() {
		if (isIdentity()) return this;
		float x00;
		float x01;
		float x10;
		float x11;
		float x02 = 0f;
		float x12 = 0f;
		if (isSkewPreserving()) {
			x00 = 1f / m00;
			x01 = 0f;
			x10 = 0f;
			x11 = 1f / m11;
			if (!isOriginPreserving()) {
				x02 = -x00 * m02;
				x12 = -x11 * m12;
			}
		} else {
			float d =  getDeterminant();
			x00 =  m11 / d;
			x10 = -m10 / d;
			x01 = -m01 / d;
			x11 =  m00 / d;
			if (!isOriginPreserving()) {
				x02 = -x01 * m12 - x00 * m02;
				x12 = -x10 * m02 - x11 * m12;
			}
		}
		return new Transform(x00, x10, x01, x11, x02, x12, flags);
	}

	//TODO can we avoid recomputing these for basis?
	public Vector getEigenValues() {
		if (isSkewPreserving()) {
			// already diagonalized
			return new Vector(m00, m11);
		}

		// identify eigenvalues
		float t = getTrace() * 0.5f;
		float d = getDeterminant();
		float s = (float) Math.sqrt( Math.abs(t * t - d) );
		//TODO reverse if sign is neg?
		return new Vector(t + s, t - s);
	}

	public Transform getEigenBasis() {
		if (isSkewPreserving()) {
			// already diagonalized
			return this;
		}

		// identify eigenvalues
		float t = getTrace() * 0.5f;
		float d = getDeterminant();
		//TODO check abs here
		float s = (float) Math.sqrt( Math.abs(t * t - d) );
		float e1 = t + s;
		float e2 = t - s;

		Vector v1;
		Vector v2;
		if (Math.abs(m01) > Math.abs(m10)) {
			v1 = new Vector(m01, e1 - m00);
			v2 = new Vector(m01, e2 - m00);
		} else {
			v1 = new Vector(e1 - m11, m10);
			v2 = new Vector(e2 - m11, m10);
		}
		Norm norm = Norm.L2;
		v1 = norm.normalize(v1);
		v2 = norm.normalize(v2);

		float x00 = v1.x;
		float x10 = v1.y;
		float x01 = v2.x;
		float x11 = v2.y;
		//TODO what should these be?
//		float x02 = -x01 * m12 - x00 * m02;
//		float x12 = -x10 * m02 - x11 * m12;
		float x02 = 0f;
		float x12 = 0f;
		return new Transform(x00, x10, x01, x11, x02, x12);
	}

	public boolean isFirstColumnMajor() {
		float m0 = Norm.L2.powMagnitude(m00, m10);
		float m1 = Norm.L2.powMagnitude(m01, m11);
		return m0 >= m1;
	}

	public boolean isIdentity() {
		// note: extra check needed because identity mask admits possibility that transform is 180deg rotation
		return (flags & IDENTITY_MASK) == IDENTITY_MASK && m00 == 1f;
	}

	public boolean isIdentityOrTranslation() {
		return (flags & (SKEW_PRESERVING | SCALE_PRESERVING)) == (SKEW_PRESERVING | SCALE_PRESERVING) && m00 == 1f;
	}

	public boolean isOriginPreserving() {
		return (flags & ORIGIN_PRESERVING) == ORIGIN_PRESERVING;
	}

	// TODO find better name
	public boolean isSkewPreserving() {
		return (flags & SKEW_PRESERVING) == SKEW_PRESERVING;
	}

	public boolean isScalePreserving() {
		return (flags & SCALE_PRESERVING) == SCALE_PRESERVING;
	}

	public boolean isRectilinearPreserving() {
		return (flags & RECTILNEAR_PRESERVING) == RECTILNEAR_PRESERVING;
	}

	public boolean isChiralPreserving() {
		return (flags & CHIRAL_PRESERVING) == CHIRAL_PRESERVING;
	}

	public boolean isCirclePreserving() {
		return (flags & CIRCLE_PRESERVING) == CIRCLE_PRESERVING;
	}

	// t times this
	@Override
	public Transform apply(Transform t) {
		if (isIdentity()) return t;
		return t.preApply(this);
	}

	// this times t
	public Transform preApply(Transform t) {
		if (isIdentity()) return t;

		// TODO could apply many optimizing cases
		return new Transform(
				m00 * t.m00 + m01 * t.m10,
				m10 * t.m00 + m11 * t.m10,
				m00 * t.m01 + m01 * t.m11,
				m10 * t.m01 + m11 * t.m11,
				m00 * t.m02 + m01 * t.m12 + m02,
				m10 * t.m02 + m11 * t.m12 + m12,
				flags & t.flags
				);
	}

	public Transformable transform(Transformable t) {
		if (t == null) throw new IllegalArgumentException("null t");
		return t.apply(this);
	}

	public Point transformOrigin() {
		return isOriginPreserving() ? Point.ORIGIN : new Point(m02, m12);
	}

	//TODO would vector be a better return type?
	public Point transform(float x, float y) {
		return transformImpl(new Pair(x, y)).asPoint();
	}

	public void transform(Transformable... ts) {
		if (ts == null) throw new IllegalArgumentException("null ts");
		for (int i = 0; i < ts.length; i++) {
			ts[i] = ts[i].apply(this);
		}
	}

	public List<Transformable> transform(List<? extends Transformable> ts) {
		if (ts == null) throw new IllegalArgumentException("null ts");
		List<Transformable> list = new ArrayList<>(ts.size());
		for (Transformable t : ts) {
			list.add(t.apply(this));
		}
		return list;
	}

	public Point transform(Point point) {
		if (isIdentity()) return point;
		return transformImpl(new Pair(point)).asPoint();
	}

	public Vector transform(Vector vector) {
		int rightTurns = getRightTurns();
		return rightTurns >= 0 ? vector.rotateThroughRightAngles(rightTurns) : transformImpl(new Pair(vector)).asVector();
	}

	public float transform(float angle) {
		if (isCirclePreserving()) {
			if (isRectilinearPreserving()) {
				if (isSkewPreserving()) {
					return m00 > 0 ? angle : angle + Angles.PI;
				} else {
					return m10 > 0 ? angle + Angles.PI_BY_TWO : angle - Angles.PI_BY_TWO;
				}
			} else {
				float theta = (float) Math.atan2(m10, m00);
				return angle + theta;
			}
		} else {
			Transform t = Transform.rotation(angle).apply(this);
			return (float) Math.atan2(t.m10, t.m00);
		}
	}

	//TODO change to use optional?
	// rename to transformRect
	public Rect transform(Rect rect) {
		if (!isRectilinearPreserving()) throw new IllegalStateException("not rectilinear preserving");
		if (isIdentity()) return rect;
		Pair pair = new Pair(rect.minX, rect.minY);
		transformImpl(pair);
		float x = pair.x;
		float y = pair.y;
		pair.setXY(rect.maxX, rect.maxY);
		transformImpl(pair);
		return Rect.atPoints(x, y, pair.x, pair.y);
	}

	public Offset transform(Offset offset) {
		if (isIdentity() || offset.isIdentity()) return offset;
		if (!isRectilinearPreserving()) throw new IllegalStateException("not rectilinear preserving");
		if (!isOriginPreserving()) throw new IllegalStateException("not origin preserving");
		if (isSkewPreserving()) {
			// simple case - must be a straightforward scale
			return Offset.offset(
					offset.toMinX * m00,
					offset.toMaxX * m00,
					offset.toMinY * m11,
					offset.toMaxY * m11
					);
		}
		//TODO need to deal with rotations and reflections
		throw new UnsupportedOperationException("TODO: support rotations and reflections");
	}

	//TODO rename
	public Offset transformOffset(Rect rect) {
		return transform(rect).offsetFrom(rect);
	}

	public void transform(Point[] points, int from, int to) {
		if (points == null) throw new IllegalArgumentException("null points");
		if (to < from) throw new IllegalArgumentException("to less than from");
		if (from < 0) throw new IllegalArgumentException("from negative");
		if (to > points.length) throw new IllegalArgumentException("to exceeds length");
		transformImpl(points, from, to);
	}

	public void transform(Point[] points) {
		if (points == null) throw new IllegalArgumentException("null points");
		transformImpl(points, 0, points.length);
	}

	public void transform(Vector[] vectors, int from, int to) {
		if (vectors == null) throw new IllegalArgumentException("null vectors");
		if (to < from) throw new IllegalArgumentException("to less than from");
		if (from < 0) throw new IllegalArgumentException("from negative");
		if (to > vectors.length) throw new IllegalArgumentException("to exceeds length");
		transformImpl(vectors, from, to);
	}

	public void transform(Vector[] vectors) {
		if (vectors == null) throw new IllegalArgumentException("null vectors");
		transformImpl(vectors, 0, vectors.length);
	}

	public void transform(float[] coords, int from, int to) {
		if (coords == null) throw new IllegalArgumentException("null coords");
		if (to < from) throw new IllegalArgumentException("to less than from");
		if (from < 0) throw new IllegalArgumentException("from negative");
		if (to > coords.length) throw new IllegalArgumentException("to exceeds length");
		if (((to-from) & 1) != 0) throw new IllegalArgumentException("uneven span");
		transformImpl(coords, from, to);
	}

	public void transform(float[] coords) {
		if (coords == null) throw new IllegalArgumentException("null coords");
		if ((coords.length & 1) != 0) throw new IllegalArgumentException("uneven coords length");
		transformImpl(coords, 0, coords.length);
	}

	public void transform(float[] src, float[] dst, int srcOffset, int dstOffset, int count) {
		if (src == null) throw new IllegalArgumentException("null src");
		if (dst == null) throw new IllegalArgumentException("null dst");
		if (count < 0) throw new IllegalArgumentException("negative count");
		if ((count & 1) != 0) throw new IllegalArgumentException("uneven count");
		if (srcOffset < 0) throw new IllegalArgumentException("negative srcOffset");
		if (dstOffset < 0) throw new IllegalArgumentException("negative dstOffset");
		if (srcOffset + count > src.length) throw new IllegalArgumentException("srcOffset plus count exceeds length");
		if (dstOffset + count > dst.length) throw new IllegalArgumentException("dstOffset plus count exceeds length");
		transformImpl(src, dst, srcOffset, dstOffset, count);
	}

	public void transform(float[] src, float[] dst) {
		if (src == null) throw new IllegalArgumentException("null src");
		if (dst == null) throw new IllegalArgumentException("null dst");
		if ((src.length & 1) != 0) throw new IllegalArgumentException("src length uneven");
		if (dst.length < src.length) throw new IllegalArgumentException("dst length too short");
		transformImpl(src, dst, 0, 0, src.length);
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(m00) +
				31 * Float.floatToIntBits(m10) +
				31 * 31 * Float.floatToIntBits(m01) +
				31 * 31 * 31 * Float.floatToIntBits(m11) +
				31 * 31 * 31 * 31 * Float.floatToIntBits(m02) +
				31 * 31 * 31 * 31 * 31 * Float.floatToIntBits(m12);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Transform)) return false;
		Transform that = (Transform) obj;
		return
				this.m00 == that.m00 &&
				this.m10 == that.m10 &&
				this.m01 == that.m01 &&
				this.m11 == that.m11 &&
				this.m02 == that.m12 &&
				this.m12 == that.m12;
	}

	@Override
	public String toString() {
		return m00 + ", " + m10 + ", " + m01 + ", " + m11 + ", " + m02 + ", " + m12;
	}

//	private boolean isRight() {
//		return ;
//	}

	// -1 if not applicable
	private int getRightTurns() {
		if ((flags & RIGHT_MASK) != RIGHT_MASK) return -1;
		if (isSkewPreserving()) {
			return m00 > 0f ? 0 : 2;
		} else {
			return m10 > 0f ? 3 : 1;
		}
	}

	private void transformImpl(Point[] points, int from, int to) {
		if (isIdentity()) return;
		Pair pair = new Pair();
		for (int i = from; i < to; i++) {
			points[i] = transformImpl(pair.setPoint(points[i])).asPoint();
		}
	}

	private void transformImpl(Vector[] vectors, int from, int to) {
		if (isIdentity()) return;
		Pair pair = new Pair();
		for (int i = from; i < to; i++) {
			vectors[i] = transformImpl(pair.setVector(vectors[i])).asVector();
		}
	}

	private void transformImpl(float[] coords, int from, int to) {
		Pair pair = new Pair();
		for (int i = from; i < to;) {
			pair.x = coords[i    ];
			pair.y = coords[i + 1];
			transformImpl(pair);
			coords[i++] = pair.x;
			coords[i++] = pair.y;
		}
	}

	private void transformImpl(float[] src, float[] dst, int srcOffset, int dstOffset, int count) {
		Pair pair = new Pair();
		int limit = srcOffset + count;
		int offset0 = dstOffset - srcOffset - 2;
		int offset1 = dstOffset - srcOffset - 1;
		for (int i = srcOffset; i < limit;) {
			pair.x = src[i ++];
			pair.y = src[i ++];
			transformImpl(pair);
			dst[offset0 + i] = pair.x;
			dst[offset1 + i] = pair.y;
		}
	}

	// assumes not identity
	private Pair transformImpl(Pair pair) {
		float x = pair.x;
		float y = pair.y;

		int flags = this.flags;
		if (pair.noTrans) flags |= ORIGIN_PRESERVING;
		flags &= APPLY_MASK;
		switch (flags) {

		// non-translations

		case ORIGIN_PRESERVING | SKEW_PRESERVING | SCALE_PRESERVING :
			break;
		case ORIGIN_PRESERVING | SKEW_PRESERVING :
			pair.x = x * m00;
			pair.y = y * m11;
			break;
		case ORIGIN_PRESERVING | SCALE_PRESERVING :
		case ORIGIN_PRESERVING :
			pair.x = x * m00 + y * m01;
			pair.y = x * m10 + y * m11;
			break;

		// translations

		case SKEW_PRESERVING | SCALE_PRESERVING :
			pair.x = x + m02;
			pair.y = y + m12;
			break;
		case SKEW_PRESERVING :
			pair.x = x * m00 + m02;
			pair.y = y * m11 + m12;
			break;
		case SCALE_PRESERVING :
			default :
				pair.x = x * m00 + y * m01 + m02;
				pair.y = x * m10 + y * m11 + m12;
				break;
		}
		return pair;
	}

	private static final class Pair {
		float x;
		float y;
		boolean noTrans;

		Pair() { }

		Pair(float x, float y) {
			this.x = x;
			this.y = y;
			noTrans = false;
		}

		Pair(Point point) {
			if (point == null) throw new IllegalArgumentException("null point");
			x = point.x;
			y = point.y;
			noTrans = false;
		}

		Pair(Vector vector) {
			if (vector == null) throw new IllegalArgumentException("null vector");
			x = vector.x;
			y = vector.y;
			noTrans = true;
		}

		Pair setPoint(Point point) {
			x = point.x;
			y = point.y;
			noTrans = false;
			return this;
		}

		Pair setXY(float x, float y) {
			this.x = x;
			this.y = y;
			noTrans = false;
			return this;
		}

		Point asPoint() {
			return new Point(x, y);
		}

		Pair setVector(Vector vector) {
			x = vector.x;
			y = vector.y;
			noTrans = true;
			return this;
		}

		Vector asVector() {
			return new Vector(x, y);
		}

	}

}
