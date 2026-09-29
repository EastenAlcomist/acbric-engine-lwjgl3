package org.newdawn.slick;

import static org.lwjgl.opengl.GL11.*;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.catengine.lwjgl3.Tex;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.newdawn.slick.opengl.ImageData;
import org.newdawn.slick.opengl.PNGImageData;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;

/**
 * Slick2D Image 兼容层（LWJGL3 后端，立即模式渲染）。
 * 约定：纹理不翻转上传，v=0 对应图像顶部。
 */
public class Image {
	public static final int FILTER_LINEAR = GL_LINEAR;
	public static final int FILTER_NEAREST = GL_NEAREST;

	protected TextureImpl texture;
	protected int width;
	protected int height;
	protected float imageX = 0;   // 子图在大纹理中的像素偏移
	protected float imageY = 0;
	protected float angle = 0;
	protected float alpha = 1.0f;
	protected float centerX = -1;
	protected float centerY = -1;
	protected boolean destroyed = false;
	protected String ref;

	public Image(Texture texture) {
		this.texture = (TextureImpl) texture;
		this.width = texture.getImageWidth();
		this.height = texture.getImageHeight();
	}

	public Image(String ref) throws SlickException { this(ref, false); }
	public Image(String ref, boolean flipped) throws SlickException { this(ref, flipped, FILTER_LINEAR); }
	public Image(String ref, boolean flipped, int filter) throws SlickException {
		loadFromResource(ref, flipped, filter);
	}

	public Image(InputStream in, String ref, boolean flipped) throws SlickException {
		this(in, ref, flipped, FILTER_LINEAR);
	}
	public Image(InputStream in, String ref, boolean flipped, int filter) throws SlickException {
		try {
			PNGImageData pid = new PNGImageData();
			pid.loadImage(in, flipped, true, null);
			init(pid, filter);
			this.ref = ref;
		} catch (IOException e) {
			throw new SlickException("Failed to load image " + ref, e);
		}
	}

	public Image(ImageData data) { init(data, FILTER_LINEAR); }
	public Image(ImageData data, int filter) { init(data, filter); }
	public Image(ImageBuffer buffer) { init(buffer, FILTER_LINEAR); }
	public Image(ImageBuffer buffer, int filter) { init(buffer, filter); }

	public Image(int width, int height) throws SlickException { this(width, height, FILTER_LINEAR); }
	public Image(int width, int height, int filter) throws SlickException {
		Tex tex = Tex.create(width, height, GL_RGBA, GL_RGBA, null, filter);
		this.texture = new TextureImpl(tex);
		this.texture.setTextureData(new byte[width * height * 4]);
		this.width = width;
		this.height = height;
	}

	protected Image() {}

	private void loadFromResource(String ref, boolean flipped, int filter) throws SlickException {
		InputStream in = Image.class.getResourceAsStream(ref);
		if (in == null) {
			in = Image.class.getClassLoader().getResourceAsStream(ref.startsWith("/") ? ref.substring(1) : ref);
		}
		if (in == null) {
			throw new SlickException("Resource not found: " + ref);
		}
		try {
			PNGImageData pid = new PNGImageData();
			pid.loadImage(in, flipped, true, null);
			init(pid, filter);
			this.ref = ref;
		} catch (IOException e) {
			throw new SlickException("Failed to load image " + ref, e);
		} finally {
			try { in.close(); } catch (IOException e) {}
		}
	}

	private void init(ImageData data, int filter) {
		ByteBuffer buf = data.getImageBufferData();
		width = data.getWidth();
		height = data.getHeight();
		int texW = data.getTexWidth();
		int texH = data.getTexHeight();
		// 复制原始 RGBA 字节，供 getTextureData() 缓存与 getColor()
		byte[] raw = new byte[buf.remaining()];
		buf.duplicate().get(raw);
		Tex tex = Tex.create(texW, texH, GL_RGBA, GL_RGBA, buf, filter);
		this.texture = new TextureImpl(tex);
		this.texture.setTextureData(raw);
	}

	public static Image createOffscreenImage(int width, int height) throws SlickException {
		return createOffscreenImage(width, height, FILTER_LINEAR);
	}
	public static Image createOffscreenImage(int width, int height, int filter) throws SlickException {
		return new Image(width, height, filter);
	}

	public Texture getTexture() { return texture; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public float getTextureWidth() { return texture.getTextureWidth(); }
	public float getTextureHeight() { return texture.getTextureHeight(); }
	public float getTextureOffsetX() { return imageX; }
	public float getTextureOffsetY() { return imageY; }
	public int getFilter() { return 0; }

	public void setFilter(int filter) { texture.setTextureFilter(filter); }

	public void bind() { texture.bind(); }
	public void startUse() {}
	public void endUse() {}

	public void setImageColor(float r, float g, float b, float a) { GLCompat.glColor4f(r, g, b, a); }
	public void setImageColor(float r, float g, float b) { GLCompat.glColor4f(r, g, b, 1); }
	public void setColor(int corner, float r, float g, float b, float a) {}
	public void setColor(int corner, float r, float g, float b) {}

	public void setAlpha(float alpha) { this.alpha = alpha; }
	public float getAlpha() { return alpha; }
	public void setRotation(float angle) { this.angle = angle; }
	public float getRotation() { return angle; }
	public void rotate(float angle) { this.angle += angle; }
	public void setCenterOfRotation(float x, float y) { this.centerX = x; this.centerY = y; }
	public float getCenterOfRotationX() { return centerX < 0 ? width / 2.0f : centerX; }
	public float getCenterOfRotationY() { return centerY < 0 ? height / 2.0f : centerY; }

	public void draw() { draw(0, 0); }
	public void draw(float x, float y) { draw(x, y, width, height); }
	public void draw(float x, float y, Color filter) { draw(x, y, width, height, filter); }
	public void draw(float x, float y, float scale) { draw(x, y, width * scale, height * scale); }
	public void draw(float x, float y, float scale, Color filter) { draw(x, y, width * scale, height * scale, filter); }
	public void drawCentered(float x, float y) { draw(x - width / 2.0f, y - height / 2.0f); }

	public void draw(float x, float y, float w, float h) { draw(x, y, w, h, null); }
	public void draw(float x, float y, float w, float h, Color filter) {
		float a = alpha;
		float r = 1, g = 1, b = 1;
		if (filter != null) { r = filter.r; g = filter.g; b = filter.b; a *= filter.a; }
		GLCompat.glColor4f(r, g, b, a);
		render(x, y, w, h);
	}
	// Slick2D 语义：draw(x, y, x2, y2, ...) 第 3/4 参为右下角坐标（不是宽高），sx/sy 为源像素坐标
	public void draw(float x, float y, float x2, float y2, float sx1, float sy1, float sx2, float sy2) {
		draw(x, y, x2, y2, sx1, sy1, sx2, sy2, null);
	}
	public void draw(float x, float y, float x2, float y2, float sx1, float sy1, float sx2, float sy2, Color filter) {
		float a = alpha;
		float r = 1, g = 1, b = 1;
		if (filter != null) { r = filter.r; g = filter.g; b = filter.b; a *= filter.a; }
		GLCompat.glColor4f(r, g, b, a);
		float texW = texture.getTextureWidth();
		float texH = texture.getTextureHeight();
		// Slick2D 原版语义：源坐标 sx/sy 相对于子图左上角，需加上子图在纹理中的偏移
		render(x, y, x2 - x, y2 - y, (imageX + sx1) / texW, (imageY + sy1) / texH, (imageX + sx2) / texW, (imageY + sy2) / texH);
	}

	public void drawEmbedded(float x, float y, float w, float h) { draw(x, y, w, h); }
	public void drawEmbedded(float x, float y) { draw(x, y); }
	public void drawEmbedded(float x, float y, float w, float h, float sx1, float sy1, float sx2, float sy2) {
		draw(x, y, w, h, sx1, sy1, sx2, sy2);
	}

	/** 绘制完整纹理（当前 sub-rect）到 (x,y,w,h)。 */
	private void render(float x, float y, float w, float h) {
		float texW = texture.getTextureWidth();
		float texH = texture.getTextureHeight();
		render(x, y, w, h, imageX / texW, imageY / texH, (imageX + width) / texW, (imageY + height) / texH);
	}

	private void render(float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
		GLCompat.glEnable(GL_TEXTURE_2D);   // 确保纹理启用（防止其他绘制 glDisable 后泄漏导致白屏）
		texture.bind();
		boolean rot = angle != 0;
		if (rot) {
			GLCompat.glPushMatrix();
			float cx = x + w * (getCenterOfRotationX() / width);
			float cy = y + h * (getCenterOfRotationY() / height);
			GLCompat.glTranslatef(cx, cy, 0);
			GLCompat.glRotatef(angle, 0, 0, 1);
			GLCompat.glTranslatef(-cx, -cy, 0);
		}
		GLCompat.glBegin(GL_QUADS);
		GLCompat.glTexCoord2f(u0, v0); GLCompat.glVertex2f(x, y);
		GLCompat.glTexCoord2f(u0, v1); GLCompat.glVertex2f(x, y + h);
		GLCompat.glTexCoord2f(u1, v1); GLCompat.glVertex2f(x + w, y + h);
		GLCompat.glTexCoord2f(u1, v0); GLCompat.glVertex2f(x + w, y);
		GLCompat.glEnd();
		if (rot) GLCompat.glPopMatrix();
	}

	public Image copy() {
		Image img = new Image(texture);
		img.width = width; img.height = height;
		img.imageX = imageX; img.imageY = imageY;
		img.angle = angle; img.alpha = alpha;
		return img;
	}

	public Image getSubImage(int x, int y, int w, int h) {
		Image img = new Image(texture);
		img.width = w; img.height = h;
		img.imageX = imageX + x; img.imageY = imageY + y;
		return img;
	}

	public Image getScaledCopy(float scale) { return getScaledCopy((int) (width * scale), (int) (height * scale)); }
	public Image getScaledCopy(int w, int h) {
		Image img = copy();
		img.width = w; img.height = h;
		return img;
	}

	public Image getFlippedCopy(boolean flipX, boolean flipY) {
		Image img = copy();
		// 翻转通过交换纹理坐标实现，需覆盖 render；简化：暂不支持，返回拷贝
		return img;
	}

	public Color getColor(int x, int y) {
		byte[] raw = texture.getTextureData();
		if (raw == null) return Color.black;
		int px = (int) imageX + x;
		int py = (int) imageY + y;
		int texW = texture.getTextureWidth();
		int i = (py * texW + px) * 4;
		if (i < 0 || i + 3 >= raw.length) return Color.black;
		return new Color(raw[i] & 0xFF, raw[i + 1] & 0xFF, raw[i + 2] & 0xFF, raw[i + 3] & 0xFF);
	}

	/** 用 RGBA 像素更新整个纹理（供 Graphics.copyArea 读回帧缓冲使用）。 */
	public void setPixels(byte[] rgba) {
		texture.bind();
		// glTexSubImage2D 需要直接缓冲区（LWJGL3 对堆缓冲区会传无效指针导致驱动崩溃）
		ByteBuffer buf = ByteBuffer.allocateDirect(rgba.length);
		buf.put(rgba).flip();
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buf);
	}

	public void destroy() throws SlickException {
		if (!destroyed) {
			texture.release();
			destroyed = true;
		}
	}
	public boolean isDestroyed() { return destroyed; }
	public void flushPixelData() {}

	public String getResourceReference() { return ref; }
	public void setName(String name) { this.ref = name; }
	public String getName() { return ref; }
}
