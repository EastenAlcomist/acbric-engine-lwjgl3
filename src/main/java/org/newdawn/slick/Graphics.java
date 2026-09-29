package org.newdawn.slick;

import static org.lwjgl.opengl.GL11.*;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import java.nio.ByteBuffer;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.Triangulator;

/**
 * Slick2D Graphics 兼容层（LWJGL3 后端，阶段 5 core profile）。
 * 图元/变换走 GLCompat 仿真层（CPU 矩阵栈 + VBO 批渲染），裁剪/混合仍用真实 GL。
 * 屏幕坐标约定：原点左上，Y 向下（由引擎的正交投影保证）。
 */
public class Graphics {
	public static int MODE_NORMAL = 0;
	public static int MODE_ALPHA_MAP = 1;
	public static int MODE_ALPHA_BLEND = 2;
	public static int MODE_COLOR_MULTIPLY = 3;
	public static int MODE_ADD = 4;
	public static int MODE_SCREEN = 5;
	public static int MODE_ADD_ALPHA = 6;
	public static int MODE_COLOR_MULTIPLY_ALPHA = 7;

	private Color color = Color.white;
	private Color background = Color.black;
	private float lineWidth = 1.0f;
	private int screenWidth;
	private int screenHeight;

	/** 供引擎在窗口尺寸变化时同步视口尺寸。 */
	public void setScreenSize(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
	}
	private Rectangle clip;
	private Rectangle worldClip;
	private boolean antialias = false;
	private int drawMode = MODE_NORMAL;

	public Graphics() {}
	public Graphics(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
	}

	public void setDrawMode(int mode) {
		this.drawMode = mode;
		switch (mode) {
			case 0: // MODE_NORMAL
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				break;
			case 4: // MODE_ADD
				glBlendFunc(GL_SRC_ALPHA, GL_ONE);
				break;
			default:
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		}
	}
	public void clearAlphaMap() { glColorMask(true, true, true, false); glClear(GL_COLOR_BUFFER_BIT); glColorMask(true, true, true, true); }

	public void setBackground(Color color) { this.background = color; }
	public Color getBackground() { return background; }
	public void clear() {
		glClearColor(background.r, background.g, background.b, background.a);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	public void setColor(Color color) { this.color = color; }
	public Color getColor() { return color; }

	public void setLineWidth(float width) { this.lineWidth = width; }
	public float getLineWidth() { return lineWidth; }
	public void resetLineWidth() { lineWidth = 1.0f; }

	public void setAntiAlias(boolean anti) { this.antialias = anti; }
	public boolean isAntiAlias() { return antialias; }

	// ---- 变换（CPU 矩阵栈） ----
	public void translate(float x, float y) { GLCompat.glTranslatef(x, y, 0); }
	public void scale(float sx, float sy) { GLCompat.glScalef(sx, sy, 1); }
	public void rotate(float rx, float ry, float ang) {
		GLCompat.glTranslatef(rx, ry, 0);
		GLCompat.glRotatef(ang, 0, 0, 1);
		GLCompat.glTranslatef(-rx, -ry, 0);
	}
	public void resetTransform() { GLCompat.glLoadIdentity(); }
	public void resetTransforms() { GLCompat.glLoadIdentity(); }
	public void pushTransform() { GLCompat.glPushMatrix(); }
	public void popTransform() { GLCompat.glPopMatrix(); }

	// ---- 裁剪 ----
	public void setClip(int x, int y, int w, int h) { setClip(new Rectangle(x, y, w, h)); }
	public void setClip(Rectangle rect) {
		this.clip = rect;
		applyClip();
	}
	private void applyClip() {
		if (clip == null && worldClip == null) { glDisable(GL_SCISSOR_TEST); return; }
		glEnable(GL_SCISSOR_TEST);
		if (clip != null) {
			glScissor((int) clip.getX(), screenHeight - (int) (clip.getY() + clip.getHeight()),
				(int) clip.getWidth(), (int) clip.getHeight());
		} else if (worldClip != null) {
			glScissor((int) worldClip.getX(), screenHeight - (int) (worldClip.getY() + worldClip.getHeight()),
				(int) worldClip.getWidth(), (int) worldClip.getHeight());
		}
	}
	public void clearClip() { this.clip = null; applyClip(); }
	public Rectangle getClip() { return clip; }
	public void setWorldClip(float x, float y, float w, float h) { setWorldClip(new Rectangle(x, y, w, h)); }
	public void setWorldClip(Rectangle rect) { this.worldClip = rect; applyClip(); }
	public void clearWorldClip() { this.worldClip = null; applyClip(); }
	public Rectangle getWorldClip() { return worldClip; }

	// ---- 图元（GLCompat 仿真） ----
	public void fillRect(float x, float y, float w, float h) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		GLCompat.glBegin(GL_QUADS);
		GLCompat.glVertex2f(x, y);
		GLCompat.glVertex2f(x + w, y);
		GLCompat.glVertex2f(x + w, y + h);
		GLCompat.glVertex2f(x, y + h);
		GLCompat.glEnd();
		GLCompat.glEnable(GL_TEXTURE_2D);
	}
	public void drawRect(float x, float y, float w, float h) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		glLineWidth(lineWidth);
		GLCompat.glBegin(GL_LINE_LOOP);
		GLCompat.glVertex2f(x, y);
		GLCompat.glVertex2f(x + w, y);
		GLCompat.glVertex2f(x + w, y + h);
		GLCompat.glVertex2f(x, y + h);
		GLCompat.glEnd();
		GLCompat.glEnable(GL_TEXTURE_2D);
	}
	public void drawLine(float x1, float y1, float x2, float y2) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		glLineWidth(lineWidth);
		GLCompat.glBegin(GL_LINES);
		GLCompat.glVertex2f(x1, y1);
		GLCompat.glVertex2f(x2, y2);
		GLCompat.glEnd();
		GLCompat.glEnable(GL_TEXTURE_2D);
	}
	public void fillOval(float x, float y, float w, float h) { oval(x, y, w, h, GL_TRIANGLE_FAN); }
	public void drawOval(float x, float y, float w, float h) { oval(x, y, w, h, GL_LINE_LOOP); }
	private void oval(float x, float y, float w, float h, int mode) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		float cx = x + w / 2, cy = y + h / 2;
		float rx = w / 2, ry = h / 2;
		int segments = 40;
		if (mode == GL_LINE_LOOP) glLineWidth(lineWidth);
		GLCompat.glBegin(mode);
		// TRIANGLE_FAN 的扇心必须是第一个顶点，否则填充会退化成多边形（速度轮盘等椭圆填充错误）
		if (mode == GL_TRIANGLE_FAN) GLCompat.glVertex2f(cx, cy);
		for (int i = 0; i < segments; i++) {
			float a = (float) (i * 2 * Math.PI / segments);
			GLCompat.glVertex2f(cx + (float) Math.cos(a) * rx, cy + (float) Math.sin(a) * ry);
		}
		if (mode == GL_TRIANGLE_FAN) GLCompat.glVertex2f(cx + rx, cy);   // 闭合回起点
		GLCompat.glEnd();
		GLCompat.glEnable(GL_TEXTURE_2D);
	}
	public void fillRoundRect(float x, float y, float w, float h, int cornerRadius) {
		fillRoundRect(x, y, w, h, cornerRadius, cornerRadius);
	}
	public void fillRoundRect(float x, float y, float w, float h, int rx, int ry) {
		// 简化：以普通矩形近似圆角
		fillRect(x, y, w, h);
	}
	public void drawRoundRect(float x, float y, float w, float h, int cornerRadius) { drawRect(x, y, w, h); }
	public void drawRoundRect(float x, float y, float w, float h, int rx, int ry) { drawRect(x, y, w, h); }
	/**
	 * 画椭圆弧（Slick2D 原版语义）：start/end 为角度制（0°=东，逆时针为正），
	 * 50 段采样，LINE_STRIP。用于武器射界楔形显示等。
	 */
	public void drawArc(float x, float y, float w, float h, float start, float end) { arc(x, y, w, h, start, end, GL_LINE_STRIP); }
	/** 填充椭圆弧（Slick2D 原版语义）：TRIANGLE_FAN 填充楔形。 */
	public void fillArc(float x, float y, float w, float h, float start, float end) { arc(x, y, w, h, start, end, GL_TRIANGLE_FAN); }
	private void arc(float x, float y, float w, float h, float start, float end, int mode) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		if (mode == GL_LINE_STRIP) glLineWidth(lineWidth);
		float cx = x + w / 2, cy = y + h / 2;
		float step = 360f / 50f;
		GLCompat.glBegin(mode);
		// TRIANGLE_FAN 的扇心必须是第一个顶点，否则扇形（速度轮盘段、武器射界楔形）填充错误
		if (mode == GL_TRIANGLE_FAN) GLCompat.glVertex2f(cx, cy);
		for (float a = start; a < end + step; a += step) {
			float ang = a > end ? end : a;
			double rad = Math.toRadians(ang);
			GLCompat.glVertex2f(cx + (float) Math.cos(rad) * w / 2, cy + (float) Math.sin(rad) * h / 2);
		}
		GLCompat.glEnd();
		GLCompat.glEnable(GL_TEXTURE_2D);
	}

	// ---- 形状 ----
	public void fill(Shape shape) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		if (shape instanceof Polygon) {
			Triangulator t = ((Polygon) shape).getTriangles();
			GLCompat.glBegin(GL_TRIANGLES);
			for (int i = 0; i < t.getTriangleCount(); i++) {
				for (int p = 0; p < 3; p++) {
					float[] xy = t.getTrianglePoint(i, p);
					GLCompat.glVertex2f(xy[0], xy[1]);
				}
			}
			GLCompat.glEnd();
		} else {
			float[] pts = shape.getPoints();
			GLCompat.glBegin(GL_TRIANGLE_FAN);
			for (int i = 0; i < pts.length; i += 2) GLCompat.glVertex2f(pts[i], pts[i + 1]);
			GLCompat.glEnd();
		}
		GLCompat.glEnable(GL_TEXTURE_2D);
	}
	public void draw(Shape shape) {
		color.bind();
		GLCompat.glDisable(GL_TEXTURE_2D);
		glLineWidth(lineWidth);
		float[] pts = shape.getPoints();
		GLCompat.glBegin(GL_LINE_LOOP);
		for (int i = 0; i < pts.length; i += 2) GLCompat.glVertex2f(pts[i], pts[i + 1]);
		GLCompat.glEnd();
		GLCompat.glEnable(GL_TEXTURE_2D);
	}

	// ---- 图像 ----
	public void drawImage(Image image, float x, float y) { image.draw(x, y); }
	public void drawImage(Image image, float x, float y, Color filter) { image.draw(x, y, filter); }
	public void drawImage(Image image, float x, float y, float w, float h) { image.draw(x, y, w, h); }
	public void drawImage(Image image, float x, float y, float x2, float y2, float sx1, float sy1, float sx2, float sy2) {
		image.draw(x, y, x2, y2, sx1, sy1, sx2, sy2);
	}
	public void drawImage(Image image, float x, float y, float x2, float y2, float sx1, float sy1, float sx2, float sy2, Color filter) {
		image.draw(x, y, x2, y2, sx1, sy1, sx2, sy2, filter);
	}

	public void copyArea(Image target, int x, int y) {
		int w = target.getWidth();
		int h = target.getHeight();
		// glReadPixels 的 y 从视口底部起算（GL 约定），屏幕坐标 y 从顶部起算
		int glY = Math.max(0, screenHeight - (y + h));
		int readH = Math.min(h, Math.max(0, screenHeight - y));
		ByteBuffer buf = ByteBuffer.allocateDirect(w * readH * 4);
		glReadPixels(x, glY, w, readH, GL_RGBA, GL_UNSIGNED_BYTE, buf);
		byte[] raw = new byte[w * readH * 4];
		buf.get(raw);
		// glReadPixels 行序自底向上 → 翻转为自顶向下；未读到的行（区域超出视口）保持透明
		byte[] flipped = new byte[w * h * 4];
		int rowBytes = w * 4;
		for (int row = 0; row < readH; row++) {
			System.arraycopy(raw, (readH - 1 - row) * rowBytes, flipped, row * rowBytes, rowBytes);
		}
		target.setPixels(flipped);
	}
	public void copyArea(Image target, int x, int y, int sx, int sy, int sw, int sh) {
		// 简化：仅支持全尺寸读回
		copyArea(target, x, y);
	}

	public void flush() { glFlush(); }
	public void destroy() {}
}
