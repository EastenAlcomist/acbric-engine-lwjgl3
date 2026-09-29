package org.newdawn.slick.geom;

/** Slick2D Rectangle 兼容层。 */
public class Rectangle extends Shape {
	public Rectangle(float x, float y, float width, float height) {
		this.x = x; this.y = y;
		rebuild(width, height);
	}
	public Rectangle() {
		this(0, 0, 0, 0);
	}

	private void rebuild(float width, float height) {
		points = new float[] { x, y, x + width, y, x + width, y + height, x, y + height };
	}

	public boolean contains(float x, float y) {
		return x >= this.x && x <= this.x + getWidth() && y >= this.y && y <= this.y + getHeight();
	}
	public void setBounds(Rectangle r) { setBounds(r.x, r.y, r.getWidth(), r.getHeight()); }
	public void setBounds(float x, float y, float width, float height) {
		this.x = x; this.y = y;
		rebuild(width, height);
	}
	public void setSize(float width, float height) { rebuild(width, height); }
	public float getWidth() { return points[2] - points[0]; }
	public float getHeight() { return points[5] - points[1]; }
	public void grow(float h, float v) { setBounds(x - h, y - v, getWidth() + h * 2, getHeight() + v * 2); }
	public void scaleGrow(float h, float v) { grow(getWidth() * (h - 1), getHeight() * (v - 1)); }
	public void setWidth(float width) { rebuild(width, getHeight()); }
	public void setHeight(float height) { rebuild(getWidth(), height); }
	public boolean intersects(Shape s) {
		return x < s.getMaxX() && x + getWidth() > s.getMinX() && y < s.getMaxY() && y + getHeight() > s.getMinY();
	}
	public float getCenterX() { return x + getWidth() / 2; }
	public float getCenterY() { return y + getHeight() / 2; }

	@Override public String toString() {
		return "[" + x + "," + y + " " + getWidth() + "x" + getHeight() + "]";
	}
}
