package org.newdawn.slick.opengl;

import static org.lwjgl.opengl.GL11.*;
import com.zarkonnen.catengine.lwjgl3.Tex;

/** Slick2D TextureImpl 兼容层，包装后端 Tex。 */
public class TextureImpl implements Texture {
	private final Tex tex;
	private byte[] textureData;

	public TextureImpl(Tex tex) { this.tex = tex; }

	public void setTextureData(byte[] data) { this.textureData = data; }

	public boolean hasAlpha() { return true; }
	public String getTextureRef() { return null; }
	public void bind() { tex.bind(); }
	public static void bindNone() { Tex.bindNone(); }
	public static void unbind() { Tex.bindNone(); }
	public int getImageHeight() { return tex.getImageHeight(); }
	public int getImageWidth() { return tex.getImageWidth(); }
	public float getHeight() { return tex.getHeight(); }
	public float getWidth() { return tex.getWidth(); }
	public int getTextureHeight() { return tex.getTextureHeight(); }
	public int getTextureWidth() { return tex.getTextureWidth(); }
	public void release() { tex.release(); }
	public int getTextureID() { return tex.getTextureID(); }
	public byte[] getTextureData() { return textureData; }

	public void setTextureFilter(int filter) {
		tex.bind();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
	}
}
