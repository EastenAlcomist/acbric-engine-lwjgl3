package com.zarkonnen.catengine.lwjgl3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** 后端内部工具方法。 */
final class Utils {
	private Utils() {}

	/** 读取文本文件（UTF-8），用于加载 GLSL 着色器源码。 */
	static String readFile(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read file: " + path, e);
		}
	}
}
