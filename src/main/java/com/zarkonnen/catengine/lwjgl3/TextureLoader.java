package com.zarkonnen.catengine.lwjgl3;

import static org.lwjgl.opengl.GL11.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

/**
 * 图像加载（STB），替代 Slick2D 的 PNGImageData/ImageIOImageData。
 * 从文件或内存加载 RGBA 数据并上传为 GL 纹理。
 */
public final class TextureLoader {
	private TextureLoader() {}

	/** 从文件路径加载纹理。filter 取 GL_NEAREST / GL_LINEAR。 */
	public static Tex load(String path, int filter) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);
			// 不翻转：glTexImage2D 首元素=左下角，数据首行=图像顶行 → v=0 对应图像顶部
			ByteBuffer data = STBImage.stbi_load(path, w, h, comp, 4);
			if (data == null) {
				throw new RuntimeException("Unable to load image: " + path + " (" + STBImage.stbi_failure_reason() + ")");
			}
			try {
				return Tex.create(w.get(0), h.get(0), GL_RGBA, GL_RGBA, data, filter);
			} finally {
				STBImage.stbi_image_free(data);
			}
		}
	}

	/** 从内存字节加载纹理。 */
	public static Tex load(byte[] bytes, int filter) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer buf = stack.malloc(bytes.length).put(bytes).flip();
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);
			ByteBuffer data = STBImage.stbi_load_from_memory(buf, w, h, comp, 4);
			if (data == null) {
				throw new RuntimeException("Unable to decode image: " + STBImage.stbi_failure_reason());
			}
			try {
				return Tex.create(w.get(0), h.get(0), GL_RGBA, GL_RGBA, data, filter);
			} finally {
				STBImage.stbi_image_free(data);
			}
		}
	}
}
