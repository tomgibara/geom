package com.tomgibara.geom.floats;

//TODO construct via static methods?
public final class FloatRange {

	public static final FloatRange UNIT_CLOSED = new FloatRange(0, false, 1, false);
	public static final FloatRange UNIT_OPEN = new FloatRange(0, true, 1, true);

	public final double min;
	public final double max;
	public final boolean minOpen;
	public final boolean maxOpen;

	public FloatRange(double min, boolean minOpen, double max, boolean maxOpen) {
		if (min > max) throw new IllegalArgumentException();
		this.min = min;
		this.max = max;
		this.minOpen = minOpen;
		this.maxOpen = maxOpen;
	}

	public FloatRange(double a, double b) {
		this(Math.min(a, b), false, Math.max(a, b), false);
	}

	private FloatRange(FloatRange that, double value) {
		if (value < that.min) {
			min = value;
			minOpen = false;
			max = that.max;
			maxOpen = that.maxOpen;
		} else {
			max = value;
			maxOpen = false;
			min = that.min;
			minOpen = that.minOpen;
		}
	}

	public boolean isEmpty() {
		return min == max && minOpen && maxOpen;
	}

	public boolean isOpen() {
		return minOpen && maxOpen;
	}

	public boolean isClosed() {
		return !minOpen && !maxOpen;
	}

	public boolean isZeroSize() {
		return min == max;
	}

	public boolean containsValue(double value) {
		if (value <= min) return !minOpen && value == min;
		if (value >= max) return !maxOpen && value == max;
		return true;
	}

	public boolean containsRange(FloatRange range) {
		if (range.min < this.min || range.min == this.min && this.minOpen && !range.minOpen) return false;
		if (range.max > this.max || range.max == this.max && this.maxOpen && !range.maxOpen) return false;
		return true;
	}

	public boolean isStrictlyLessThan(FloatRange range) {
		return this.max < range.min || this.max == range.min && (this.maxOpen || range.minOpen);
	}

	public boolean isStrictlyGreaterThan(FloatRange range) {
		return this.min > range.max || this.min == range.max && (this.minOpen || range.maxOpen);
	}

	public boolean intersects(FloatRange range) {
		return !isStrictlyLessThan(range) && !isStrictlyGreaterThan(range);
	}

	public double getSize() {
		return max - min;
	}

	public FloatRange intersectionAsRange(FloatRange range) {
		if (this.containsRange(range)) return range;
		if (range.containsRange(this)) return this;
		if (!this.intersects(range)) return null;

		double min;
		boolean minOpen;
		if (this.min < range.min) {
			min = range.min;
			minOpen = range.minOpen;
		} else if (this.min > range.min) {
			min = this.min;
			minOpen = this.minOpen;
		} else {
			min = this.min;
			minOpen = this.minOpen || range.minOpen;
		}

		double max;
		boolean maxOpen;
		if (this.max > range.max) {
			max = range.max;
			maxOpen = range.maxOpen;
		} else if (this.max < range.max) {
			max = this.max;
			maxOpen = this.maxOpen;
		} else {
			max = this.max;
			maxOpen = this.maxOpen || range.maxOpen;
		}

		return new FloatRange(min, minOpen, max, maxOpen);
	}

	//TODO add union

	public FloatRange unionRange(double value) {
		return containsValue(value) ? this : new FloatRange(this, value);
	}

	public double clampValue(double value) {
		if (value < min) {
			if (minOpen) throw new IllegalArgumentException("min open");
			return min;
		}
		if (value > max) {
			if (maxOpen) throw new IllegalArgumentException("max open");
			return max;
		}
		return value;
	}

	public FloatRange translate(double shift) {
		//TODO should use private cons
		return shift == 0.0 ? this : new FloatRange(min + shift, minOpen, max + shift, maxOpen);
	}

	@Override
	public int hashCode() {
		int h = Double.hashCode(min) + 31 * Double.hashCode(max);
		if (minOpen) h += 0x1b0a6998;
		if (maxOpen) h += 0x09562e01;
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof FloatRange that)) return false;
        if (this.minOpen != that.minOpen) return false;
		if (this.maxOpen != that.maxOpen) return false;
		if (this.min != that.min) return false;
		if (this.max != that.max) return false;
		return true;
	}

	@Override
	public String toString() {
		return (minOpen ? "(" : "[") + min + "," + max + (maxOpen ? ")" : "]");
	}

}
