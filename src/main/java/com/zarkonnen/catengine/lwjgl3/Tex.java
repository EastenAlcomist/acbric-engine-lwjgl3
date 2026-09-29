package com.zarkonnen.catengine.lwjgl3;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import static org.lwjgl.opengl.GL11.*;
import java.nio.ByteBuffer;

/**
 * GPU 纹理封装（LWJGL3 后端），替代 Slick2D 的 org.newdawn.slick.opengl.TextureImpl。
 * 语义对齐 Slick2D：imageWidth/imageHeight 为原始图像尺寸，textureWidth/textureHeight 为
 * 实际纹理尺寸（本后端不做 2 的幂填充，二者相等），getWidth/getHeight 返回归一化 0..1。
 */
public final class Tex {
	private final int id;
	private final int imageWidth;
	private final int imageHeight;
	private final int textureWidth;
	private final int textureHeight;
	private boolean released;

	public Tex(int id, int width, int height) {
		this.id = id;
		this.imageWidth = width;
		this.imageHeight = height;
		this.textureWidth = width;
		this.textureHeight = height;
	}

	public static Tex create(int width, int height, int internalFormat, int format, ByteBuffer data, int filter) {
		int id = glGenTextures();
		GLCompat.glBindTexture(GL_TEXTURE_2D, id);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, data);
		return new Tex(id, width, height);
	}

	public void bind() {
		GLCompat.glBindTexture(GL_TEXTURE_2D, id);
	}

	public static void bindNone() {
		GLCompat.glBindTexture(GL_TEXTURE_2D, 0);
	}

	public int getTextureID() { return id; }
	public int getImageWidth() { return imageWidth; }
	public int getImageHeight() { return imageHeight; }
	public int getTextureWidth() { return textureWidth; }
	public int getTextureHeight() { return textureHeight; }
	public float getWidth() { return textureWidth == 0 ? 0 : (float) imageWidth / textureWidth; }
	public float getHeight() { return textureHeight == 0 ? 0 : (float) imageHeight / textureHeight; }

	public void release() {
		if (!released) {
			glDeleteTextures(id);
			released = true;
		}
	}
}
