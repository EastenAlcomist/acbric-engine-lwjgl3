package org.newdawn.slick.opengl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

/** Slick2D PNGImageData 兼容层（基于 ImageIO 解码）。 */
public class PNGImageData implements LoadableImageData {
	private ByteBuffer data;
	private int width;
	private int height;
	private int texWidth;
	private int texHeight;

	public PNGImageData() {}

	public Format getFormat() { return Format.RGBA; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getTexWidth() { return texWidth; }
	public int getTexHeight() { return texHeight; }
	public ByteBuffer getImageBufferData() { return data; }

	public ByteBuffer loadImage(InputStream fis) throws IOException {
		return loadImage(fis, false, null);
	}
	public ByteBuffer loadImage(InputStream fis, boolean flipped, int[] transparent) throws IOException {
		return loadImage(fis, flipped, true, transparent);
	}
	public ByteBuffer loadImage(InputStream fis, boolean flipped, boolean forceAlpha, int[] transparent) throws IOException {
		BufferedImage img = ImageIO.read(fis);
		if (img == null) throw new IOException("Unable to decode PNG image");
		width = img.getWidth(); height = img.getHeight();
		texWidth = width; texHeight = height;
		data = ImageDataUtil.toRGBA(img, flipped);
		return data;
	}

	public void configureEdging(boolean edging) {}
}
