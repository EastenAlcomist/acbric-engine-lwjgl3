package org.newdawn.slick.opengl;

import java.nio.ByteBuffer;

/** Slick2D ImageData 接口兼容层。 */
public interface ImageData {
	Format getFormat();
	int getWidth();
	int getHeight();
	int getTexWidth();
	int getTexHeight();
	ByteBuffer getImageBufferData();

	enum Format {
		RGB(false), BGRA(true), RGBA(true), ALPHA(false), GRAY(false), GRAYALPHA(true);
		private final boolean alpha;
		Format(boolean alpha) { this.alpha = alpha; }
		public boolean hasAlpha() { return alpha; }
		public int getColorComponents() { return hasAlpha() ? 4 : 3; }
		public int getBitPerPixel() { return getColorComponents() * 8; }
		public int getOGLType() { return hasAlpha() ? 0x1908 /* GL_RGBA */ : 0x1907 /* GL_RGB */; }
	}
}
