package org.newdawn.slick;

import com.zarkonnen.catengine.lwjgl3.GLCompat;

/**
 * Slick2D Color 兼容层（LWJGL3 后端）。
 * 语义与 Slick2D 一致：r/g/b/a 为 0..1 浮点，bind() 设置 GLCompat 当前颜色。
 */
public class Color implements java.io.Serializable {
	public static final Color transparent = new Color(0.0f, 0.0f, 0.0f, 0.0f);
	public static final Color white = new Color(1.0f, 1.0f, 1.0f, 1.0f);
	public static final Color yellow = new Color(1.0f, 1.0f, 0.0f, 1.0f);
	public static final Color red = new Color(1.0f, 0.0f, 0.0f, 1.0f);
	public static final Color blue = new Color(0.0f, 0.0f, 1.0f, 1.0f);
	public static final Color green = new Color(0.0f, 1.0f, 0.0f, 1.0f);
	public static final Color black = new Color(0.0f, 0.0f, 0.0f, 1.0f);
	public static final Color gray = new Color(0.5f, 0.5f, 0.5f, 1.0f);
	public static final Color cyan = new Color(0.0f, 1.0f, 1.0f, 1.0f);
	public static final Color darkGray = new Color(0.3f, 0.3f, 0.3f, 1.0f);
	public static final Color lightGray = new Color(0.7f, 0.7f, 0.7f, 1.0f);
	public static final Color pink = new Color(255, 175, 175, 255);
	public static final Color orange = new Color(255, 200, 0, 255);
	public static final Color magenta = new Color(255, 0, 255, 255);

	public float r;
	public float g;
	public float b;
	public float a = 1.0f;

	public Color(Color copy) { this(copy.r, copy.g, copy.b, copy.a); }
	public Color(float r, float g, float b) { this(r, g, b, 1.0f); }
	public Color(float r, float g, float b, float a) {
		this.r = r; this.g = g; this.b = b; this.a = a;
	}
	public Color(int r, int g, int b) { this(r, g, b, 255); }
	public Color(int r, int g, int b, int a) {
		this.r = r / 255.0f; this.g = g / 255.0f; this.b = b / 255.0f; this.a = a / 255.0f;
	}

	public void bind() { GLCompat.glColor4f(r, g, b, a); }

	public Color darker() { return darker(0.5f); }
	public Color darker(float scale) {
		float s = 1.0f - scale;
		return new Color(r * s, g * s, b * s, a);
	}
	public Color brighter() { return brighter(0.5f); }
	public Color brighter(float scale) {
		scale += 1;
		return new Color(r * scale, g * scale, b * scale, a);
	}

	public Color multiply(Color c) { return new Color(r * c.r, g * c.g, b * c.b, a * c.a); }
	public void add(Color c) { r += c.r; g += c.g; b += c.b; a += c.a; }
	public void scale(float value) { r *= value; g *= value; b *= value; a *= value; }
	public Color addToCopy(Color c) { Color copy = new Color(this); copy.add(c); return copy; }
	public Color scaleCopy(float value) { Color copy = new Color(this); copy.scale(value); return copy; }

	public int getRed() { return (int) (r * 255); }
	public int getGreen() { return (int) (g * 255); }
	public int getBlue() { return (int) (b * 255); }
	public int getAlpha() { return (int) (a * 255); }
	public int getRedByte() { return (int) (r * 255); }
	public int getGreenByte() { return (int) (g * 255); }
	public int getBlueByte() { return (int) (b * 255); }
	public int getAlphaByte() { return (int) (a * 255); }

	public static Color decode(String nm) {
		String s = nm.startsWith("#") ? nm.substring(1) : nm;
		int i = (int) Long.parseLong(s, 16);
		return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
	}

	@Override public int hashCode() {
		return ((int) (r + g + b + a) * 255);
	}
	@Override public boolean equals(Object other) {
		if (other instanceof Color) {
			Color o = (Color) other;
			return o.r == r && o.g == g && o.b == b && o.a == a;
		}
		return false;
	}
	@Override public String toString() {
		return "Color (" + r + "," + g + "," + b + "," + a + ")";
	}
}
