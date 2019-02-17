package com.tomgibara.geom.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tomgibara.geom.core.Context;
import com.tomgibara.geom.core.LineSegment;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.helper.Locator;
import com.tomgibara.geom.path.SequencePath.Builder;
import com.tomgibara.geom.path.SequencePath.Builder.Policy;

public class Parameterizations {

	final Path path;
	final int treeLength;
	final int leafOffset; // every tree node at an index greater than or equal to this, is a leaf
	final int stragglers; // number leaf nodes not at the full height of the tree
	final ByIntrinsic byIntrinsic;
	final ByLength byLength;

	public Parameterizations(Path path, List<? extends Path> paths) {
		if (path == null) throw new IllegalArgumentException("null path");
		if (paths == null) throw new IllegalArgumentException("null paths");
		this.path = path;

		int size = paths.size();
		treeLength = 2 * size - 1;
		leafOffset = treeLength - size;
		int pwrOf2 = Integer.highestOneBit(size);
		stragglers = size == pwrOf2 ? 0 : 2 * pwrOf2 - size;

		byIntrinsic = new ByIntrinsic(paths);
		byLength = new ByLength(paths);
	}

	public Parameterization.ByIntrinsic getByIntrinsic() {
		return byIntrinsic;
	}

	public Parameterization.ByLength getByLength() {
		return byLength;
	}

	public Path.Location locateAtLength(float p) {
		return byLength.locationAt(p);
	}

	private abstract class SeqParam implements Parameterization {

		private final boolean byLength;
		private final Parameterization[] zs;
		private final float[] tree;

		public SeqParam(List<? extends Path> paths, boolean byLength) {
			Parameterization[] zs = new Parameterization[paths.size()];
			for (int i = 0; i < zs.length; i++) {
				Path p = paths.get(i);
				zs[i] = byLength ? p.byLength() : p.byIntrinsic();
			}
			this.byLength = byLength;
			int treeLength = Parameterizations.this.treeLength;
			float[] tree = new float[treeLength];
			int pwrOf2 = Integer.highestOneBit(treeLength);
			int j = 0;
			//TODO could reorganize algorithm for intrinsic case to avoid summing values
			for (int s = pwrOf2 - 1, limit = treeLength; s >= 0 ; limit = s, s = (s+1)/2 - 1) {
				for (int i = s; i < limit; i++) {
					int offset = limit + 2 * (i - s);
					float value;
					if (offset >= treeLength) {
						Parameterization z = zs[j++];
						value = byLength ? z.getPath().getLength() : 1f / zs.length;
					} else {
						value = tree[offset] + tree[offset + 1];
					}
					tree[i] = value;
				}
			}
			this.zs = zs;
			this.tree = tree;
		}

		@Override
		public Path getPath() {
			return path;
		}

		@Override
		public Point pointAt(float p) {
			return zAt(p).getPoint();
		}

		@Override
		public Vector tangentAt(float p) {
			return zAt(p).getTangent();
		}

		@Override
		public PointPath pointTangentAt(float p) {
			return zAt(p).getPointTangent();
		}

		@Override
		public SplitPath splitAt(float p) {
			Z zee = zAt(p);
			Parameterization z = zee.z;
			SplitPath split = z.splitAt(zee.p);
			int index = zee.index;

			Path first = split.getFirstPath();
			if (index > 0) {
				Builder builder = SequencePath.builder().withPolicy(Policy.JOIN);
				for (int i = 0; i < index; i++) {
					builder.addPath(zs[i].getPath());
				}
				first = builder.addPath(first).build();
			}

			Path second = split.getLastPath();
			if (index < zs.length - 1) {
				Builder builder = SequencePath.builder().withPolicy(Policy.JOIN).addPath(second);
				for (int i = index + 1; i < zs.length; i++) {
					builder.addPath(zs[i].getPath());
				}
				second = builder.build();
			}

			return new SplitPath(first, second, path.isClosed());
		}

		// only called on intrinsic instance
		float lengthAt(float p) {
			Z z = zAt(p);
			return other().preSum(z.index) + z.getLength();
		}

		// only called on length instance
		//TODO this can be simplified now
		float intrinsicAt(float p) {
			Z z = zAt(p);
			return other().preSum(z.index) + z.getIntrinsic();
		}

		Path.Location locationAt(float p) {
			return zAt(p).getLocation();
		}

		@Override
		public float parameterNearest(Point pt) {
			float length = new Locator(path).getNearestLengthAlongPath(pt);
			return byLength ? length : other().intrinsicAt(length);
		}

		@Override
		public Path.Location location() {
			return new Path.Location(this, 0f);
		}

		@Override
		//TODO should combine corners at same parameter
		public List<Path.Corner> getCorners() {
			List<Path.Corner> list = new ArrayList<>();
			Context context = Context.currentContext();
			// possible corner at start of closed path
			if (path.isClosed()) {
				Vector v1 = zs[zs.length - 1].tangentAt(Float.MAX_VALUE);
				Vector v2 = zs[0].tangentAt(0f);
				if (context.isCorner(v1, v2)) list.add(new Path.Corner(this, 0f, path.getStart(), v1, v2));
			}
			float offset = 0f;
			Parameterization z1 = zs[0];
			for (int i = 0; i < zs.length; i++) {
				// z may contain corners
				List<Path.Corner> corners = z1.getCorners();
				for (Path.Corner corner : corners) {
					list.add(corner.reparameterize(this, (offset + corner.getParameter())));
				}
				if (i < zs.length - 1) {
					Parameterization z2 = zs[i + 1];
					float dist = byLength ? z1.getPath().getLength() : 1f;
					offset += dist;
					Vector v1 = z1.tangentAt(dist);
					Vector v2 = z2.tangentAt(0f);
					Point pt = z2.getPath().getStart();
					if (context.isCorner(v1, v2)) list.add(new Path.Corner(this, offset, pt, v1, v2));
					z1 = z2;
				}
			}
			return Collections.unmodifiableList(list);
		}

		@Override
		public Path segment(float minP, float maxP) {
			if (minP <= 0f) minP = 0f;
			if (maxP >= 1f) maxP = 1f;
			if (minP > maxP) throw new IllegalArgumentException("minP exceeds maxP");
			if (minP == 0f && maxP == 1f) return path;
			Z minZ = zAt(minP);
			Z maxZ = zAt(maxP);
			if (minZ.z == maxZ.z) return minZ.z.segment(minZ.p, maxZ.p);
			//TODO ideally want to optimize cases where z.p's are exactly 0 or 1
			Builder builder = SequencePath.builder().withPolicy(Policy.IGNORE);
			for (int i = minZ.index; i <= maxZ.index; i++) {
				if (i == minZ.index) {
					if (minZ.p == 0f) {
						builder.addPath(minZ.z.getPath());
					} else if (minZ.p == 1f) {
						/* don't add zero length path */
					} else {
						builder.addPath(minZ.z.segment(minZ.p, 1f));
					}
				} else if (i == maxZ.index) {
					if (maxZ.p == 1f) {
						builder.addPath(maxZ.z.getPath());
					} else if (maxZ.p == 0f) {
						/* don't add zero length path */
					} else {
						builder.addPath(maxZ.z.segment(0f, maxZ.p));
					}
				} else {
					builder.addPath(zs[i].getPath());
				}
			}
			if (builder.isEmpty()) {
				//TODO return point path based on pt at value?
				throw new UnsupportedOperationException();
			}
			return builder.build();
		}

		private SeqParam other() {
			return byLength ? Parameterizations.this.byIntrinsic : Parameterizations.this.byLength;
		}

		private Z zAt(float p) {
			if (p <= 0 || treeLength == 1) return new Z(zs, 0, p);
			int length = zs.length;
			if (p >= tree[0]) return new Z(zs, length - 1, p);
			int offset = leafOffset;
			int i = 0;
			while (true) {
				i = 2 * i + 1;
				if (p > tree[i]) {
					p -= tree[i];
					i++;
				}
				if (i >= offset) {
					i -= offset;
					int index = i < stragglers ? length - stragglers + i : i - stragglers;
					return new Z(zs, index, byLength ? p : p * zs.length);
				}
			}
		}

		private float preSum(int zi) {
			if (zi == 0) return 0f;
			int length = zs.length;
			int i = zi >= length - stragglers ? zi - length + stragglers : zi + stragglers;
			i += leafOffset;
			float s = 0;
			while (i > 0) {
				if ((i & 1) == 1) {
					s -= tree[i    ];
					s -= tree[i + 1];
					i -= 1;
				} else {
					s -= tree[i    ];
					i -= 2;
				}
				i >>= 1;
				s+=tree[i];
			}
			return s;
		}

	}

	private class ByIntrinsic extends SeqParam implements Parameterization.ByIntrinsic {

		public ByIntrinsic(List<? extends Path> paths) {
			super(paths, false);
		}

		@Override
		public float lengthAt(float p) {
			return super.lengthAt(p);
		}

	}

	private class ByLength extends SeqParam implements Parameterization.ByLength {

		public ByLength(List<? extends Path> paths) {
			super(paths, true);
		}

		@Override
		public float intrinsicAt(float p) {
			return super.intrinsicAt(p);
		}

	}

	private static final class Z {

		final int index;
		final Parameterization z;
		final float p;

		Z(Parameterization[] zs, int index, float p) {
			this.index = index;
			this.z = zs[index];
			this.p = p;
		}

		Point getPoint() {
			return z.pointAt(p);
		}

		Vector getTangent() {
			return z.tangentAt(p);
		}

		PointPath getPointTangent() {
			return z.pointTangentAt(p);
		}

		Path.Location getLocation() {
			return new Path.Location(z, p);
		}

		float getLength() {
			return ((Parameterization.ByIntrinsic) z).lengthAt(p);
		}

		float getIntrinsic() {
			return ((Parameterization.ByLength) z).intrinsicAt(p);
		}

	}

}