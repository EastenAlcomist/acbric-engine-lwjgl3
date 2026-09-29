package org.newdawn.slick.geom;

/** Slick2D Shape 基类（最小实现）。 */
public abstract class Shape {
	protected float x;
	protected float y;
	protected float[] points;

	public Shape() {}

	public float getX() { return x; }
	public float getY() { return y; }
	public void setX(float x) { this.x = x; }
	public void setY(float y) { this.y = y; }
	public abstract float getWidth();
	public abstract float getHeight();
	public abstract float getCenterX();
	public abstract float getCenterY();
	public float[] getPoints() { return points; }
	public int getPointCount() { return points.length / 2; }
	public float[] getPoint(int i) { return new float[] { points[i * 2], points[i * 2 + 1] }; }
	public float getMinX() {
		float min = Float.MAX_VALUE;
		for (int i = 0; i < points.length; i += 2) min = Math.min(min, points[i]);
		return min;
	}
	public float getMaxX() {
		float max = Float.MIN_VALUE;
		for (int i = 0; i < points.length; i += 2) max = Math.max(max, points[i]);
		return max;
	}
	public float getMinY() {
		float min = Float.MAX_VALUE;
		for (int i = 1; i < points.length; i += 2) min = Math.min(min, points[i]);
		return min;
	}
	public float getMaxY() {
		float max = Float.MIN_VALUE;
		for (int i = 1; i < points.length; i += 2) max = Math.max(max, points[i]);
		return max;
	}
	public boolean contains(float x, float y) {
		// 射线法
		boolean inside = false;
		int n = getPointCount();
		for (int i = 0, j = n - 1; i < n; j = i++) {
			float xi = points[i * 2], yi = points[i * 2 + 1];
			float xj = points[j * 2], yj = points[j * 2 + 1];
			if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
				inside = !inside;
			}
		}
		return inside;
	}
}
