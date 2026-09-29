package com.zarkonnen.airships;

public abstract class  PhysicsRect {
	private double x;
	private double y;
	public abstract double getBBWidth();
	public abstract double getBBHeight();

	public final double getX() {
		return x;
	}

	public final void setX(double x) {
		if (Double.isNaN(x)) { throw new IllegalArgumentException("x is NaN"); }
		if (Double.isInfinite(x)) { throw new IllegalArgumentException("x is infinite"); }
		this.x = x;
	}

	public final double getY() {
		return y;
	}

	public final void setY(double y) {
		if (Double.isNaN(y)) { throw new IllegalArgumentException("y is NaN"); }
		if (Double.isInfinite(y)) { throw new IllegalArgumentException("y is infinite"); }
		this.y = y;
	}
}
