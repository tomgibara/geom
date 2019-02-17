package com.tomgibara.geom.shape;

public enum WindingRule {

	EVEN_ODD,
	NON_ZERO;

	public boolean isInterior(int windingNumber) {
		switch (this) {
		case EVEN_ODD : return (windingNumber & 1) == 1;
		case NON_ZERO : return windingNumber != 0;
		default : throw new IllegalStateException("unsupported winding rule " + this);
		}
	}

}
