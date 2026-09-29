package org.newdawn.slick.opengl.pbuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * Slick2D FBOGraphics 兼容层：将绘制渲染到目标 Image 的纹理（FBO）。
 * bind() 保存当前投影/模型视图/视口/帧缓冲，绑定 FBO 并设置 FBO 尺寸的投影；
 * unbind()/flush() 完整恢复原状态，防止 FBO 状态泄漏到主渲染。
 * 阶段 5：矩阵存取经 GLCompat（CPU 矩阵栈），FBO 用真实 GL30。
 */
public class FBOGraphics extends Graphics {
	private final int fboID;
	private final int width;
	private final int height;
	// 直接缓冲区（GLCompat.glGetFloatv 填充；堆缓冲区会让写入无效地址 → 崩溃）
	private final FloatBuffer savedProj = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer savedModel = BufferUtils.createFloatBuffer(16);
	private final IntBuffer savedViewport = BufferUtils.createIntBuffer(4);
	private int savedFBO;

	public FBOGraphics(Image image) throws SlickException {
		this.width = image.getWidth();
		this.height = image.getHeight();
		int texID = image.getTexture().getTextureID();

		fboID = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fboID);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texID, 0);
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			glDeleteFramebuffers(fboID);
			throw new SlickException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
		}
	}

	/** 绑定 FBO 为渲染目标；保存原窗口状态（投影/模型视图/视口/帧缓冲），unbind 恢复。 */
	public void bind() {
		savedProj.rewind();
		GLCompat.glGetFloatv(GL_PROJECTION_MATRIX, savedProj);
		savedModel.rewind();
		GLCompat.glGetFloatv(GL_MODELVIEW_MATRIX, savedModel);
		savedViewport.rewind();
		glGetIntegerv(GL_VIEWPORT, savedViewport);
		savedFBO = glGetInteger(GL_FRAMEBUFFER_BINDING);

		glBindFramebuffer(GL_FRAMEBUFFER, fboID);
		glViewport(0, 0, width, height);
		GLCompat.glMatrixMode(GL_PROJECTION);
		GLCompat.glLoadIdentity();
		GLCompat.glOrtho(0, width, height, 0, -1, 1);
		GLCompat.glMatrixMode(GL_MODELVIEW);
		GLCompat.glLoadIdentity();
	}

	/** 解绑并完整恢复原窗口状态。 */
	public void unbind() {
		glFlush();
		glBindFramebuffer(GL_FRAMEBUFFER, savedFBO);
		glViewport(savedViewport.get(0), savedViewport.get(1), savedViewport.get(2), savedViewport.get(3));
		GLCompat.glMatrixMode(GL_PROJECTION);
		savedProj.rewind();
		GLCompat.glLoadMatrixf(savedProj);
		GLCompat.glMatrixMode(GL_MODELVIEW);
		savedModel.rewind();
		GLCompat.glLoadMatrixf(savedModel);
	}

	@Override
	public void flush() {
		unbind();
	}

	@Override
	public void destroy() {
		unbind();
		glDeleteFramebuffers(fboID);
	}
}
