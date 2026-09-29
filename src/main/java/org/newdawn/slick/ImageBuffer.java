package org.newdawn.slick;

import java.nio.ByteBuffer;
import org.newdawn.slick.opengl.ImageData;

/** Slick2D ImageBuffer 兼容层（游戏仅 import 未实际使用，最小实现）。 */
public class ImageBuffer implements ImageData {
	private final int width;
	private final int height;
	private final byte[] data;

	public ImageBuffer(int width, int height) {
		this.width = width; this.height = height;
		this.data = new byte[width * height * 4];
	}

	public byte[] getRGBA() { return data; }
	public Format getFormat() { return Format.RGBA; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getTexWidth() { return width; }
	public int getTexHeight() { return height; }
	public ByteBuffer getImageBufferData() { return ByteBuffer.wrap(data); }

	public void setRGBA(int x, int y, int r, int g, int b, int a) {
		if (x < 0 || x >= width || y < 0 || y >= height) return;
		int i = (y * width + x) * 4;
		data[i] = (byte) r; data[i + 1] = (byte) g; data[i + 2] = (byte) b; data[i + 3] = (byte) a;
	}

	public Image getImage() { return getImage(Image.FILTER_NEAREST); }
	public Image getImage(int filter) {
		Image img = new Image(this);
		img.setFilter(filter);
		return img;
	}
}
