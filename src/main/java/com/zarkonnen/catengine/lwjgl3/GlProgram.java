package com.zarkonnen.catengine.lwjgl3;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;
import java.util.HashMap;
import java.util.Map;

/**
 * GLSL 着色器程序封装（LWJGL3 后端，阶段 5 core profile）。
 *
 * 顶点属性布局由 GLCompat 统一（0=aPos 1=aColor 2=aTex 3..14=aGen0..aGen11），
 * 旧式 GLSL 1.20 自定义属性名（flipped/tint/srcA/...）按 LEGACY_ATTRIBS 表绑定到
 * gen 槽位；getAttributeID 返回"假位置"（1000 + slot*4 + 分量偏移），
 * 游戏代码将其传给 GLCompat.glVertexAttrib*，由仿真层写入对应槽位分量。
 */
public final class GlProgram {
	/** 旧式属性名 → {gen 槽位, 槽内分量偏移}。 */
	public static final Map<String, int[]> LEGACY_ATTRIBS = new HashMap<>();
	static {
		// gen0：flipped/angle/strength/flipped_concave/globalTexCoord/flagSize/texOffset/awind
		put("flipped", 0, 0);
		put("angle", 0, 1);
		put("strength", 0, 2);
		put("flipped_concave", 0, 0);
		put("globalTexCoord", 0, 2);
		put("flagSize", 0, 0);
		put("texOffset", 0, 1);
		put("awind", 0, 3);
		// gen1：tint
		put("tint", 1, 0);
		// gen2：at/ayShift/acoord/srcA
		put("at", 2, 0);
		put("ayShift", 2, 1);
		put("acoord", 2, 2);
		put("srcA", 2, 0);
		// gen3：trgA/bevel
		put("trgA", 3, 0);
		put("bevel", 3, 0);
		// gen4：srcB/t
		put("srcB", 4, 0);
		put("t", 4, 0);
		// gen5：trgB/m
		put("trgB", 5, 0);
		put("m", 5, 0);
		// gen6：b
		put("b", 6, 0);
		// gen7：paint
		put("paint", 7, 0);
		// gen8：maskOffsetAndEnabled
		put("maskOffsetAndEnabled", 8, 0);
	}

	private static void put(String name, int slot, int off) {
		LEGACY_ATTRIBS.put(name, new int[] { slot, off });
	}

	/** 假位置（GLCompat.glVertexAttrib* 接收） = 1000 + slot*4 + off。 */
	public static int fakeLoc(String name) {
		int[] s = LEGACY_ATTRIBS.get(name);
		return s == null ? -1 : 1000 + s[0] * 4 + s[1];
	}

	private final int programID;
	private boolean released;
	private final HashMap<String, Integer> uniformCache = new HashMap<>();
	private final HashMap<String, Integer> attribCache = new HashMap<>();

	private GlProgram(int programID) {
		this.programID = programID;
	}

	/** 从文件路径加载并编译顶点/片元着色器。失败抛出 RuntimeException。 */
	public static GlProgram loadProgram(String vertPath, String fragPath) {
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, Utils.readFile(vertPath));
		glCompileShader(vs);
		checkCompile(vs, vertPath);

		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, Utils.readFile(fragPath));
		glCompileShader(fs);
		checkCompile(fs, fragPath);

		int prog = glCreateProgram();
		glAttachShader(prog, vs);
		glAttachShader(prog, fs);
		// 标准顶点布局属性名（330 着色器也用 layout(location=N) 声明，重复绑定无害）
		glBindAttribLocation(prog, 0, "aPos");
		glBindAttribLocation(prog, 1, "aColor");
		glBindAttribLocation(prog, 2, "aTex");
		// 旧式自定义属性名 → gen 槽位（真实 GL 位置 = 3 + slot）
		for (Map.Entry<String, int[]> e : LEGACY_ATTRIBS.entrySet()) {
			glBindAttribLocation(prog, 3 + e.getValue()[0], e.getKey());
		}
		glBindFragDataLocation(prog, 0, "fragColor");
		glLinkProgram(prog);
		if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
			String log = glGetProgramInfoLog(prog, 4096);
			throw new RuntimeException("Shader link failed (" + vertPath + "/" + fragPath + "): " + log);
		}
		glDeleteShader(vs);
		glDeleteShader(fs);
		return new GlProgram(prog);
	}

	private static void checkCompile(int shader, String path) {
		if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
			String log = glGetShaderInfoLog(shader, 4096);
			throw new RuntimeException("Shader compile failed (" + path + "): " + log);
		}
	}

	public void bind() { GLCompat.glUseProgram(programID); }
	public void unbind() { GLCompat.glUseProgram(0); }

	public int getID() { return programID; }

	public int getUniformID(String name) {
		Integer cached = uniformCache.get(name);
		if (cached != null) return cached;
		int loc = glGetUniformLocation(programID, name);
		uniformCache.put(name, loc);
		return loc;
	}

	/** 返回旧式属性的假位置（传给 GLCompat.glVertexAttrib*）。 */
	public int getAttributeID(String name) {
		Integer cached = attribCache.get(name);
		if (cached != null) return cached;
		int loc = fakeLoc(name);
		attribCache.put(name, loc);
		return loc;
	}

	public boolean enableVertexAttribute(String name) { return getAttributeID(name) >= 0; }
	public boolean disableVertexAttribute(String name) { return getAttributeID(name) >= 0; }

	public void setUniform1f(String name, float v) { glUniform1f(getUniformID(name), v); }
	public void setUniform1i(String name, int v) { glUniform1i(getUniformID(name), v); }
	public void setUniform2f(String name, float a, float b) { glUniform2f(getUniformID(name), a, b); }
	public void setUniform3f(String name, float a, float b, float c) { glUniform3f(getUniformID(name), a, b, c); }
	public void setUniform4f(String name, float a, float b, float c, float d) { glUniform4f(getUniformID(name), a, b, c, d); }
	public void setUniform2i(String name, int a, int b) { glUniform2i(getUniformID(name), a, b); }
	public void setUniform3i(String name, int a, int b, int c) { glUniform3i(getUniformID(name), a, b, c); }
	public void setUniform4i(String name, int a, int b, int c, int d) { glUniform4i(getUniformID(name), a, b, c, d); }

	public void release() {
		if (!released) {
			glDeleteProgram(programID);
			released = true;
		}
	}
}
