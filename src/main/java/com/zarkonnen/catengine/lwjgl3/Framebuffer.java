package com.zarkonnen.catengine.lwjgl3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 离屏帧缓冲（FBO），替代 Slick2D 的 FBOGraphics 渲染到纹理。
 * 绑定后，后续绘制写入内部纹理，而非默认帧缓冲。
 */
public final class Framebuffer {
	private final int fboID;
	private final Tex texture;
	private boolean destroyed;

	public Framebuffer(int width, int height) {
		texture = Tex.create(width, height, GL_RGBA, GL_RGBA, null, GL_NEAREST);

		fboID = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fboID);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.getTextureID(), 0);

		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			glDeleteFramebuffers(fboID);
			throw new RuntimeException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
		}
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	/** 绑定此 FBO 为渲染目标，并设置视口为其尺寸。 */
	public void bind() {
		glBindFramebuffer(GL_FRAMEBUFFER, fboID);
		glViewport(0, 0, texture.getImageWidth(), texture.getImageHeight());
	}

	/** 解绑，恢复默认帧缓冲。 */
	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public Tex texture() { return texture; }
	public int getWidth() { return texture.getImageWidth(); }
	public int getHeight() { return texture.getImageHeight(); }

	public void destroy() {
		if (!destroyed) {
			glDeleteFramebuffers(fboID);
			texture.release();
			destroyed = true;
		}
	}
}
