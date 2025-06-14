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
        private double differential;
        private double continuityTolerance;
        private int splitRecursionLimit;
        // difference in cosine
        private double cornerTolerance;
        private double shortestNonLinearCurve;
        // as a proportion of distance
        private double leastNonLinearDeviation;

        private Builder() {
            iterationSteps = MAX_ITERATION_STEPS;
            quadratureSteps = MAX_QUADRATURE_STEPS;
            //0.0001 caused problems with underflow when offsetting simple curves like a cubic bezier
            differential = 0.001f;
            // small values fail on trig based offset curves
            continuityTolerance = 1.0;
            splitRecursionLimit = 20;
            cornerTolerance = 0.005;
            shortestNonLinearCurve = 1.0;
            leastNonLinearDeviation = 0.05;
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

        public Builder setDifferential(double differential) {
            if (differential <= 0.0) throw new IllegalArgumentException("invalid differential");
            this.differential = differential;
            return this;
        }

        public Builder setContinuityTolerance(double continuityTolerance) {
            if (continuityTolerance < 0.0) throw new IllegalArgumentException("invalid continuityTolerance");
            this.continuityTolerance = continuityTolerance;
            return this;
        }

        public Builder setSplitRecursionLimit(int splitRecursionLimit) {
            if (splitRecursionLimit < 1) throw new IllegalArgumentException("invalid splitRecursionLimit");
            this.splitRecursionLimit = splitRecursionLimit;
            return this;
        }

        public Builder setShortestNonLinearCurve(double shortestNonLinearCurve) {
            if (shortestNonLinearCurve <= 0.0) throw new IllegalArgumentException("invalid shortestNonLinearCurve");
            this.shortestNonLinearCurve = shortestNonLinearCurve;
            return this;
        }

        public Builder setLeastNonLinearDeviation(double leastNonLinearDeviation) {
            if (leastNonLinearDeviation <= 0.0) throw new IllegalArgumentException("invalid leastNonLinearDeviation");
            this.leastNonLinearDeviation = leastNonLinearDeviation;
            return this;
        }

        public Tolerances build() {
            return new Tolerances(this);
        }

    }

    private final int iterationSteps;
    private final int quadratureSteps;
    private final double differential;
    private final double continuityTolerance;
    final double powContinuityTolerance;
    private final int splitRecursionLimit;
    private final double cornerTolerance;
    private final double shortestNonLinearCurve;
    final double powShortestNonLinearCurve;
    private final double leastNonLinearDeviation;
    final double powLeastNonLinearDeviation;

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

    public double getDifferential() {
        return differential;
    }

    public int getSplitRecursionLimit() {
        return splitRecursionLimit;
    }

    public double getContinuityTolerance() {
        return continuityTolerance;
    }

    public double getCornerTolerance() {
        return cornerTolerance;
    }

    public double getShortestNonLinearCurve() {
        return shortestNonLinearCurve;
    }

    public double getLeastNonLinearDeviation() {
        return leastNonLinearDeviation;
    }

}
