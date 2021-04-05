package com.tomgibara.geom.floats;

public interface FloatMapping {

	class Util {

		private static void checkRange(FloatRange range) {
			if (range == null) throw new IllegalArgumentException("null domain");
			if (!range.isClosed()) throw new IllegalArgumentException("unclosed range");
		}

		public static boolean isIdentity(FloatMapping mapping) {
			return mapping instanceof Identity;
		}

		public static FloatMapping identity(FloatRange domain) {
			checkRange(domain);
			return new Identity(domain);
		}

		public static FloatMapping constant(FloatRange domain, float c) {
			checkRange(domain);
			if (domain.isZeroSize() && domain.containsValue(c)) return new Identity(domain);
			return new Constant(domain, c);
		}

		public static FloatMapping linear(FloatRange domain, float y1, float y2) {
			checkRange(domain);
			if (y1 == domain.min && y2 == domain.max) return new Identity(domain);
			if (y1 == y2) return new Constant(domain, y1);
			return new Linear(domain, y1, y2);
		}

		public static FloatMapping linear(FloatRange domain, FloatRange range) {
			checkRange(domain);
			if (range == null) throw new IllegalArgumentException("null range");
			if (range.equals(domain)) return new Identity(domain);
			if (range.isZeroSize()) return new Constant(domain, range.min);
			return new Linear(domain, range);
		}

		public static FloatMapping compose(FloatMapping map1, FloatMapping map2) {
			if (map1 == null) throw new IllegalArgumentException("null map1");
			if (map2 == null) throw new IllegalArgumentException("null map2");

			FloatRange range = map1.getRange();
			FloatRange domain = map2.getDomain();
			if (domain.equals(range)) {
				if (map1 instanceof Identity) return map2;
				if (map2 instanceof Identity) return map1;
				if (map1 instanceof Constant) return new Constant(map1.getDomain(), map2.map(range.min));
				if (map2 instanceof Constant) return new Constant(map1.getDomain(), map2.getRange());
				if (map1 instanceof Linear && map2 instanceof Linear) {
					Linear linear = (Linear) map2;
					// not necessarily linear - could result in identity
					return linear(map1.getDomain(), linear.y1, linear.y2);
				}
			}

			if (!domain.containsRange(range)) throw new IllegalArgumentException("map1 range " + range + " exceeds map2 domain + " + domain);
			//TODO more cases to consider?
			if (map1 instanceof Constant) {
				return new Constant(map1.getDomain(), map2.map(map1.getRange().max));
			}
			if (map2 instanceof Constant) {
				return new Constant(map1.getDomain(), map2.getRange());
			}
			if (map1 instanceof Linear && map2 instanceof Linear) {
				FloatRange d = map1.getDomain();
				return linear(d, map2.map(map1.map(d.min)), map2.map(map1.map(d.max)));
			}
			return new Compose(map1, map2);
		}

		private static class Identity implements FloatMapping {

			private final FloatRange domain;

			Identity(FloatRange domain) {
				this.domain = domain;
			}

			@Override
			public FloatRange getDomain() {
				return domain;
			}

			@Override
			public FloatRange getRange() {
				return domain;
			}

			@Override
			public float map(float x) {
				return x;
			}

			@Override
			public FloatMapping inverse() {
				return this;
			}

			@Override
			public boolean isIdentity() {
				return true;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

			@Override
			public boolean isLinear() {
				return true;
			}
		}

		private static class Constant implements FloatMapping {

			private final FloatRange domain;
			private final FloatRange range;

			Constant(FloatRange domain, float c) {
				this(domain, new FloatRange(c,c));
			}

			Constant(FloatRange domain, FloatRange range) {
				this.domain = domain;
				this.range = range;
			}

			@Override
			public FloatRange getDomain() {
				return domain;
			}

			@Override
			public FloatRange getRange() {
				return range;
			}

			@Override
			public float map(float x) {
				return range.min;
			}

			@Override
			public FloatMapping inverse() {
				throw new NonInvertibleMappingException();
			}

			@Override
			public String toString() {
				return "constant " + range.min + " on " + domain;
			}

			@Override
			public boolean isIdentity() {
				return false;
			}

			@Override
			public boolean isConstant() {
				return true;
			}

			@Override
			public boolean isLinear() {
				return true;
			}
		}

		private static class Linear implements FloatMapping {

			private final FloatRange domain;
			private final float y1;
			private final float y2;
			private FloatRange range = null;

			Linear(FloatRange domain, float y1, float y2) {
				this.domain = domain;
				this.y1 = y1;
				this.y2 = y2;
			}

			Linear(FloatRange domain, FloatRange range) {
				this.domain = domain;
				this.range = range;
				this.y1 = range.min;
				this.y2 = range.max;
			}

			@Override
			public FloatRange getDomain() {
				return domain;
			}

			@Override
			public FloatRange getRange() {
				return range == null ? range = new FloatRange(y1, y2) : range;
			}

			@Override
			public float map(float x) {
				x = domain.clampValue(x);
				return (y1 * (domain.max - x) + y2 * (x - domain.min)) / domain.getSize();
			}

			@Override
			public FloatMapping inverse() {
				float x1;
				float x2;
				if (y1 < y2) {
					x1 = domain.min;
					x2 = domain.max;
				} else {
					x1 = domain.max;
					x2 = domain.min;
				}
				return new Linear(getRange(), x1, x2);
			}

			@Override
			public boolean isIdentity() {
				return false;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

			@Override
			public boolean isLinear() {
				return true;
			}

			@Override
			public String toString() {
				return "linear: " + domain.min + "," + y1 + " to " + domain.max + "," + y2;
			}

		}

		private static class Compose implements FloatMapping {

			private final FloatMapping map1;
			private final FloatMapping map2;

			Compose(FloatMapping map1, FloatMapping map2) {
				this.map1 = map1;
				this.map2 = map2;
			}

			@Override
			public FloatRange getDomain() {
				return map1.getDomain();
			}

			@Override
			public FloatRange getRange() {
				return map2.getRange();
			}

			@Override
			public float map(float x) {
				return map2.map(map1.map(x));
			}

			@Override
			public FloatMapping inverse() {
				if (!map1.getRange().containsRange(map2.getDomain())) throw new NonInvertibleMappingException();
				FloatMapping inv1 = map1.inverse();
				FloatMapping inv2 = map2.inverse();
				return new Compose(inv2, inv1);
			}

			@Override
			public boolean isIdentity() {
				return false;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

			@Override
			public boolean isLinear() {
				return false;
			}
		}

	}

	FloatRange getDomain();

	FloatRange getRange();

	float map(float x);

	FloatMapping inverse();

	boolean isIdentity();

	boolean isConstant();

	boolean isLinear();

}
