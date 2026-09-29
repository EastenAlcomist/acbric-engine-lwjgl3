package org.newdawn.slick.opengl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

/** Slick2D ImageIOImageData 兼容层（基于 ImageIO 解码）。 */
public class ImageIOImageData implements LoadableImageData {
	private ByteBuffer data;
	private int width;
	private int height;

	public ImageIOImageData() {}

	public Format getFormat() { return Format.RGBA; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getTexWidth() { return width; }
	public int getTexHeight() { return height; }
	public ByteBuffer getImageBufferData() { return data; }

	public ByteBuffer loadImage(InputStream fis) throws IOException {
		return loadImage(fis, false, null);
	}
	public ByteBuffer loadImage(InputStream fis, boolean flipped, int[] transparent) throws IOException {
		return loadImage(fis, flipped, true, transparent);
	}
	public ByteBuffer loadImage(InputStream fis, boolean flipped, boolean forceAlpha, int[] transparent) throws IOException {
		BufferedImage img = ImageIO.read(fis);
		if (img == null) throw new IOException("Unable to decode image");
		width = img.getWidth(); height = img.getHeight();
		data = ImageDataUtil.toRGBA(img, flipped);
		return data;
	}

	/** 直接由 BufferedImage 转 ByteBuffer（用于窗口图标等）。 */
	public ByteBuffer imageToByteBuffer(BufferedImage image, boolean flipped, boolean forceAlpha, int[] transparent) {
		width = image.getWidth(); height = image.getHeight();
		data = ImageDataUtil.toRGBA(image, flipped);
		return data;
	}

	public void configureEdging(boolean edging) {}
}
