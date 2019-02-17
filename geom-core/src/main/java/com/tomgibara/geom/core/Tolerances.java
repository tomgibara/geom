package com.tomgibara.geom.core;

public class Tolerances {

	private static final Tolerances DEFAULTS = new Tolerances(new Builder());

	//TODO is there really a maximum?
	public static final int MAX_ITERATION_STEPS = 20;
	public static final int MAX_QUADRATURE_STEPS = 20;

	public static Tolerances defaults() {
		return DEFAULTS;
	}

	public static Tolerances current() {
		return Context.currentContext().tolerances;
	}

	public static class Builder {

		private int iterationSteps;
		private int quadratureSteps;
		private float differential;
		private float continuityTolerance;
		private int splitRecursionLimit;
		// difference in cosine
		private float cornerTolerance;
		private float shortestNonLinearCurve;
		// as a proportion of distance
		private float leastNonLinearDeviation;

		private Builder() {
			iterationSteps = MAX_ITERATION_STEPS;
			quadratureSteps = MAX_QUADRATURE_STEPS;
			//0.0001 caused problems with underflow when offsetting simple curves like a cubic bezier
			differential = 0.001f;
			// small values fail on trig based offset curves
			continuityTolerance = 1f;
			splitRecursionLimit = 20;
			cornerTolerance = 0.005f;
			shortestNonLinearCurve = 1f;
			leastNonLinearDeviation = 0.05f;
		}

		private Builder(Tolerances tolerances) {
			iterationSteps = tolerances.iterationSteps;
			quadratureSteps = tolerances.quadratureSteps;
			differential = tolerances.differential;
			continuityTolerance = tolerances.continuityTolerance;
			splitRecursionLimit = tolerances.splitRecursionLimit;
			continuityTolerance = tolerances.cornerTolerance;
			shortestNonLinearCurve = tolerances.shortestNonLinearCurve;
			leastNonLinearDeviation = tolerances.leastNonLinearDeviation;
		}

		public Builder setIterationSteps(int iterationSteps) {
			if (iterationSteps < 1) throw new IllegalArgumentException("invalid iterationSteps");
			if (iterationSteps > MAX_ITERATION_STEPS) iterationSteps = MAX_ITERATION_STEPS;
			this.iterationSteps = iterationSteps;
			return this;
		}

		public Builder setQuadratureSteps(int quadratureSteps) {
			if (quadratureSteps < 1) throw new IllegalArgumentException("invalid quadratureSteps");
			if (quadratureSteps > MAX_QUADRATURE_STEPS) quadratureSteps = MAX_QUADRATURE_STEPS;
			this.quadratureSteps = quadratureSteps;
			return this;
		}

		public Builder setDifferential(float differential) {
			if (differential <= 0f) throw new IllegalArgumentException("invalid differential");
			this.differential = differential;
			return this;
		}

		public Builder setContinuityTolerance(float continuityTolerance) {
			if (continuityTolerance < 0f) throw new IllegalArgumentException("invalid continuityTolerance");
			this.continuityTolerance = continuityTolerance;
			return this;
		}

		public Builder setSplitRecursionLimit(int splitRecursionLimit) {
			if (splitRecursionLimit < 1) throw new IllegalArgumentException("invalid splitRecursionLimit");
			this.splitRecursionLimit = splitRecursionLimit;
			return this;
		}

		public Builder setShortestNonLinearCurve(float shortestNonLinearCurve) {
			if (shortestNonLinearCurve <= 0f) throw new IllegalArgumentException("invalid shortestNonLinearCurve");
			this.shortestNonLinearCurve = shortestNonLinearCurve;
			return this;
		}

		public Builder setLeastNonLinearDeviation(float leastNonLinearDeviation) {
			if (leastNonLinearDeviation <= 0f) throw new IllegalArgumentException("invalid leastNonLinearDeviation");
			this.leastNonLinearDeviation = leastNonLinearDeviation;
			return this;
		}

		public Tolerances build() {
			return new Tolerances(this);
		}

	}

	private final int iterationSteps;
	private final int quadratureSteps;
	private final float differential;
	private final float continuityTolerance;
	final float powContinuityTolerance;
	private final int splitRecursionLimit;
	private final float cornerTolerance;
	private final float shortestNonLinearCurve;
	final float powShortestNonLinearCurve;
	private final float leastNonLinearDeviation;
	final float powLeastNonLinearDeviation;

	private Tolerances(Builder builder) {
		this.iterationSteps = builder.iterationSteps;
		this.quadratureSteps = builder.quadratureSteps;
		this.differential = builder.differential;
		this.continuityTolerance = builder.continuityTolerance;
		powContinuityTolerance = continuityTolerance * continuityTolerance;
		this.splitRecursionLimit = builder.splitRecursionLimit;
		this.cornerTolerance = builder.cornerTolerance;
		this.shortestNonLinearCurve = builder.shortestNonLinearCurve;
		this.powShortestNonLinearCurve = builder.shortestNonLinearCurve * builder.shortestNonLinearCurve;
		this.leastNonLinearDeviation = builder.leastNonLinearDeviation;
		powLeastNonLinearDeviation = builder.leastNonLinearDeviation * builder.leastNonLinearDeviation;
	}

	public Builder builder() {
		return this == DEFAULTS ? new Builder() : new Builder(this);
	}

	public int getIterationSteps() {
		return iterationSteps;
	}

	public int getQuadratureSteps() {
		return quadratureSteps;
	}

	public float getDifferential() {
		return differential;
	}

	public int getSplitRecursionLimit() {
		return splitRecursionLimit;
	}

	public float getContinuityTolerance() {
		return continuityTolerance;
	}

	public float getCornerTolerance() {
		return cornerTolerance;
	}

	public float getShortestNonLinearCurve() {
		return shortestNonLinearCurve;
	}

	public float getLeastNonLinearDeviation() {
		return leastNonLinearDeviation;
	}

}
