package org.newdawn.slick.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Slick2D LoadableImageData 接口兼容层。 */
public interface LoadableImageData extends ImageData {
	ByteBuffer loadImage(InputStream fis) throws IOException;
	ByteBuffer loadImage(InputStream fis, boolean flipped, int[] transparent) throws IOException;
	ByteBuffer loadImage(InputStream fis, boolean flipped, boolean forceAlpha, int[] transparent) throws IOException;
	void configureEdging(boolean edging);
}
