package org.lwjgl.opengl;

/** LWJGL2 DisplayMode 兼容层（仅窗口尺寸信息）。 */
public class DisplayMode {
	private final int width;
	private final int height;
	private final int bitsPerPixel;

	public DisplayMode(int width, int height) { this(width, height, 32); }
	public DisplayMode(int width, int height, int bitsPerPixel) {
		this.width = width; this.height = height; this.bitsPerPixel = bitsPerPixel;
	}

	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getBitsPerPixel() { return bitsPerPixel; }
	@Override public String toString() { return width + " x " + height; }
}
