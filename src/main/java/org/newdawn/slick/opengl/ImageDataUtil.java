package org.newdawn.slick.opengl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/** BufferedImage → RGBA ByteBuffer 转换（包内共享）。 */
final class ImageDataUtil {
	private ImageDataUtil() {}

	static ByteBuffer toRGBA(BufferedImage img, boolean flip) {
		int w = img.getWidth(), h = img.getHeight();
		ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
		int[] row = new int[w];
		for (int y = 0; y < h; y++) {
			int sy = flip ? (h - 1 - y) : y;
			img.getRGB(0, sy, w, 1, row, 0, w);
			for (int x = 0; x < w; x++) {
				int argb = row[x];
				buf.put((byte) ((argb >> 16) & 0xFF));
				buf.put((byte) ((argb >> 8) & 0xFF));
				buf.put((byte) (argb & 0xFF));
				buf.put((byte) ((argb >> 24) & 0xFF));
			}
		}
		buf.flip();
		return buf;
	}
}
