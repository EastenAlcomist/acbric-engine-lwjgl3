package com.zarkonnen.catengine.lwjgl3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import java.nio.FloatBuffer;
import java.util.HashMap;
import org.lwjgl.BufferUtils;

/**
 * OpenGL 1.x 立即模式 + 固定管线矩阵的 core-profile 仿真层（阶段 5）。
 *
 * 用法：游戏/兼容层代码把 glBegin/glEnd/glVertexX/glTexCoordX/glColorX/glVertexAttribX、
 * glMatrixMode/glLoadIdentity/glOrtho/glTranslateX/glRotateX/glScaleX/glPushMatrix/glPopMatrix、
 * glGetFloatv(GL_*_MATRIX)、glEnable/glDisable(GL_TEXTURE_2D)、glBindTexture、glUseProgram
 * 的调用指向本类；其余 GL 调用（混合、裁剪、纹理上传、FBO 等）继续用真实 GL。
 *
 * 顶点格式（56 float = 224 字节/顶点）：
 *   pos(2) color(4) tex(2) gen[12](4f)
 * 属性位置：0=aPos 1=aColor 2=aTex 3..14=aGen0..aGen11。
 * 旧式自定义属性（flipped/tint/srcA/...）经 GlProgram 映射到 gen 槽位，
 * 游戏侧传入的“假位置” = 1000 + slot*4 + 组件偏移，glVertexAttribX 据此写入对应分量。
 */
public final class GLCompat {
	private GLCompat() {}

	// ---- 常量（与原 GL11 取值一致，供游戏代码引用） ----
	public static final int GL_POINTS = 0x0000;
	public static final int GL_LINES = 0x0001;
	public static final int GL_LINE_LOOP = 0x0002;
	public static final int GL_TRIANGLES = 0x0004;
	public static final int GL_TRIANGLE_FAN = 0x0006;
	public static final int GL_QUADS = 0x0007;
	public static final int GL_TEXTURE_2D = 0x0DE1;
	public static final int GL_MODELVIEW = 0x1700;
	public static final int GL_PROJECTION = 0x1701;
	public static final int GL_MODELVIEW_MATRIX = 0x0BA6;
	public static final int GL_PROJECTION_MATRIX = 0x0BA7;

	// ---- 矩阵栈（CPU） ----
	private static final float[] IDENTITY = {
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 1, 0,
		0, 0, 0, 1
	};
	private static float[] modelview = IDENTITY.clone();
	private static float[] projection = IDENTITY.clone();
	private static float[][] mvStack = new float[32][];
	private static float[][] projStack = new float[32][];
	private static int mvDepth = 0;
	private static int projDepth = 0;
	private static int matrixMode = 0x1700; // GL_MODELVIEW

	// ---- 立即模式仿真状态 ----
	private static final int GEN_SLOTS = 12;
	private static float[] verts = new float[64 * 56];
	private static int vertCount = 0;
	private static int primitive = 0;          // 0 = 不在 begin/end 内
	private static final float[] curColor = { 1, 1, 1, 1 };
	private static final float[] curTex = { 0, 0 };
	private static final float[] gen = new float[GEN_SLOTS * 4];
	private static float[] modelviewAtBegin = IDENTITY.clone();
	private static boolean tex2dEnabled = false;
	private static int boundTexture = 0;
	private static int currentProgram = 0;

	// ---- 批渲染资源 ----
	private static int vao = 0;
	private static int vbo = 0;
	private static int batchProg = 0;
	private static FloatBuffer vboBuf;
	private static float[] triScratch;
	private static final HashMap<Integer, Integer> uProjCache = new HashMap<>();
	private static final HashMap<Integer, Integer> uModelCache = new HashMap<>();
	private static int uBatchHasTex = -2;
	private static int uBatchTex = -2;

	// ---- 矩阵函数 ----
	public static void glMatrixMode(int mode) { matrixMode = mode; }

	public static void glLoadIdentity() {
		if (matrixMode == 0x1701) { projection = IDENTITY.clone(); }
		else { modelview = IDENTITY.clone(); }
		if (primitive != 0) { flush(); }
	}

	public static void glOrtho(double l, double r, double b, double t, double n, double f) {
		float[] m = {
			(float) (2 / (r - l)), 0, 0, 0,
			0, (float) (2 / (t - b)), 0, 0,
			0, 0, (float) (-2 / (f - n)), 0,
			(float) (-(r + l) / (r - l)), (float) (-(t + b) / (t - b)), (float) (-(f + n) / (f - n)), 1
		};
		mult(m);
	}

	public static void glTranslatef(float x, float y, float z) {
		mult(new float[] {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 1, 0,
			x, y, z, 1
		});
	}

	public static void glTranslated(double x, double y, double z) { glTranslatef((float) x, (float) y, (float) z); }

	public static void glScalef(float x, float y, float z) {
		mult(new float[] {
			x, 0, 0, 0,
			0, y, 0, 0,
			0, 0, z, 0,
			0, 0, 0, 1
		});
	}

	public static void glScaled(double x, double y, double z) { glScalef((float) x, (float) y, (float) z); }

	public static void glRotatef(float degrees, float x, float y, float z) {
		double a = Math.toRadians(degrees);
		double len = Math.sqrt(x * x + y * y + z * z);
		if (len < 1e-9) { return; }
		x /= (float) len; y /= (float) len; z /= (float) len;
		double c = Math.cos(a), s = Math.sin(a), t = 1 - c;
		mult(new float[] {
			(float) (t * x * x + c), (float) (t * x * y + s * z), (float) (t * x * z - s * y), 0,
			(float) (t * x * y - s * z), (float) (t * y * y + c), (float) (t * y * z + s * x), 0,
			(float) (t * x * z + s * y), (float) (t * y * z - s * x), (float) (t * z * z + c), 0,
			0, 0, 0, 1
		});
	}

	public static void glRotated(double degrees, double x, double y, double z) { glRotatef((float) degrees, (float) x, (float) y, (float) z); }

	public static void glPushMatrix() {
		if (matrixMode == 0x1701) {
			if (projDepth < projStack.length) { projStack[projDepth++] = projection.clone(); }
		} else {
			if (mvDepth < mvStack.length) { mvStack[mvDepth++] = modelview.clone(); }
		}
		if (primitive != 0) { flush(); }
	}

	public static void glPopMatrix() {
		if (matrixMode == 0x1701) {
			if (projDepth > 0) { projection = projStack[--projDepth]; }
		} else {
			if (mvDepth > 0) { modelview = mvStack[--mvDepth]; }
		}
		if (primitive != 0) { flush(); }
	}

	/** current = current * m（列主序 4x4，与 GL 语义一致）。 */
	private static void mult(float[] m) {
		float[] cur = matrixMode == 0x1701 ? projection : modelview;
		float[] out = new float[16];
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				double v = 0;
				for (int k = 0; k < 4; k++) {
					v += (double) cur[k * 4 + row] * m[col * 4 + k];
				}
				out[col * 4 + row] = (float) v;
			}
		}
		if (matrixMode == 0x1701) { projection = out; } else { modelview = out; }
		if (primitive != 0) { flush(); }
	}

	/** 直接设置当前矩阵（FBOGraphics 恢复投影/模型视图用）。 */
	public static void glLoadMatrixf(FloatBuffer m) {
		float[] src = new float[16];
		m.rewind();
		m.get(src);
		if (matrixMode == 0x1701) { projection = src; } else { modelview = src; }
		if (primitive != 0) { flush(); }
	}

	public static void glLoadMatrix(float[] m) {
		if (matrixMode == 0x1701) { projection = m.clone(); } else { modelview = m.clone(); }
		if (primitive != 0) { flush(); }
	}

	public static void glGetFloatv(int pname, FloatBuffer params) {
		float[] src = pname == 0x0BA7 ? projection : modelview; // PROJECTION_MATRIX / 其它按 MODELVIEW
		for (int i = 0; i < 16; i++) {
			if (params.remaining() == 0) { break; }
			params.put(src[i]);
		}
		params.flip();
	}

	public static void glGetFloatv(int pname, float[] params) {
		float[] src = pname == 0x0BA7 ? projection : modelview;
		System.arraycopy(src, 0, params, 0, Math.min(16, params.length));
	}

	// ---- 立即模式仿真 ----
	public static void glBegin(int mode) {
		if (primitive != 0) { return; } // 嵌套 begin 忽略（防御）
		primitive = mode;
		vertCount = 0;
		modelviewAtBegin = modelview.clone();
	}

	public static void glEnd() {
		if (primitive == 0) { return; }
		flush();
		primitive = 0;
	}

	public static void glVertex2f(float x, float y) { vertex(x, y, 0); }
	public static void glVertex2d(double x, double y) { vertex((float) x, (float) y, 0); }
	public static void glVertex2i(int x, int y) { vertex(x, y, 0); }
	public static void glVertex3f(float x, float y, float z) { vertex(x, y, z); }
	public static void glVertex3d(double x, double y, double z) { vertex((float) x, (float) y, (float) z); }

	private static void vertex(float x, float y, float z) {
		if (primitive == 0) { return; }
		if (vertCount * 56 + 56 > verts.length) {
			float[] nv = new float[verts.length * 2];
			System.arraycopy(verts, 0, nv, 0, vertCount * 56);
			verts = nv;
		}
		// 布局：pos(2f) color(4f) tex(2f) gen(48f)
		int i = vertCount * 56;
		verts[i] = x;
		verts[i + 1] = y;
		verts[i + 2] = curColor[0];
		verts[i + 3] = curColor[1];
		verts[i + 4] = curColor[2];
		verts[i + 5] = curColor[3];
		verts[i + 6] = curTex[0];
		verts[i + 7] = curTex[1];
		System.arraycopy(gen, 0, verts, i + 8, GEN_SLOTS * 4);
		vertCount++;
	}

	public static void glTexCoord2f(float s, float t) { curTex[0] = s; curTex[1] = t; }
	public static void glTexCoord2d(double s, double t) { curTex[0] = (float) s; curTex[1] = (float) t; }

	public static void glColor3f(float r, float g, float b) { curColor[0] = r; curColor[1] = g; curColor[2] = b; curColor[3] = 1; }
	public static void glColor4f(float r, float g, float b, float a) { curColor[0] = r; curColor[1] = g; curColor[2] = b; curColor[3] = a; }
	public static void glColor3d(double r, double g, double b) { curColor[0] = (float) r; curColor[1] = (float) g; curColor[2] = (float) b; curColor[3] = 1; }
	public static void glColor4d(double r, double g, double b, double a) { curColor[0] = (float) r; curColor[1] = (float) g; curColor[2] = (float) b; curColor[3] = (float) a; }

	/** 旧式自定义属性写入：index 为 GlProgram 分配的假位置（1000 + slot*4 + offset）。 */
	public static void glVertexAttrib1f(int index, float x) { writeAttrib(index, x, 0, 0, 0, 1); }
	public static void glVertexAttrib2f(int index, float x, float y) { writeAttrib(index, x, y, 0, 0, 2); }
	public static void glVertexAttrib3f(int index, float x, float y, float z) { writeAttrib(index, x, y, z, 0, 3); }
	public static void glVertexAttrib4f(int index, float x, float y, float z, float w) { writeAttrib(index, x, y, z, w, 4); }

	private static void writeAttrib(int index, float x, float y, float z, float w, int size) {
		if (index < 1000) { return; }
		int slot = (index - 1000) / 4;
		int off = (index - 1000) % 4;
		if (slot < 0 || slot >= GEN_SLOTS) { return; }
		if (size >= 1) { gen[slot * 4 + off] = x; }
		if (size >= 2) { gen[slot * 4 + off + 1] = y; }
		if (size >= 3) { gen[slot * 4 + off + 2] = z; }
		if (size >= 4) { gen[slot * 4 + off + 3] = w; }
	}

	// ---- 状态转发（拦截纹理启用/绑定/程序） ----
	public static void glEnable(int cap) {
		if (cap == 0x0DE1) { tex2dEnabled = true; return; } // GL_TEXTURE_2D
		org.lwjgl.opengl.GL11.glEnable(cap);
	}
	public static void glDisable(int cap) {
		if (cap == 0x0DE1) { tex2dEnabled = false; return; }
		org.lwjgl.opengl.GL11.glDisable(cap);
	}
	public static void glBindTexture(int target, int texture) {
		boundTexture = texture;
		org.lwjgl.opengl.GL11.glBindTexture(target, texture);
	}
	public static void glUseProgram(int program) {
		currentProgram = program;
		org.lwjgl.opengl.GL20.glUseProgram(program);
	}

	// ---- 批渲染 ----
	private static void flush() {
		if (vertCount == 0 || primitive == 0) { return; }
		ensureResources();

		int mode = primitive;
		int n = vertCount;
		float[] data = verts;
		// GL_QUADS → 三角形列表（0,1,2 0,2,3 每 quad）
		if (mode == 0x0007) { // GL_QUADS
			int quads = n / 4;
			int need = quads * 6 * 56;
			if (triScratch == null || triScratch.length < need) {
				triScratch = new float[need];
			}
			for (int q = 0; q < quads; q++) {
				int[] idx = { q * 4, q * 4 + 1, q * 4 + 2, q * 4, q * 4 + 2, q * 4 + 3 };
				for (int k = 0; k < 6; k++) {
					System.arraycopy(data, idx[k] * 56, triScratch, (q * 6 + k) * 56, 56);
				}
			}
			data = triScratch;
			n = quads * 6;
			mode = 0x0004; // GL_TRIANGLES
		}

		int prog = currentProgram != 0 ? currentProgram : batchProg;
		org.lwjgl.opengl.GL20.glUseProgram(prog);
		// 上传投影/模型矩阵（每个程序缓存 uniform 位置）
		glUniformMatrix4fv(getUniform(prog, uProjCache, "uProj"), false, projection);
		glUniformMatrix4fv(getUniform(prog, uModelCache, "uModel"), false, modelviewAtBegin);
		if (prog == batchProg) {
			if (uBatchHasTex == -2) {
				uBatchHasTex = glGetUniformLocation(prog, "uHasTex");
				uBatchTex = glGetUniformLocation(prog, "uTex");
			}
			glUniform1i(uBatchHasTex, tex2dEnabled && boundTexture != 0 ? 1 : 0);
			glUniform1i(uBatchTex, 0);
			// 批着色器固定采样单元 0：把当前记录纹理重新绑到单元 0
			// （游戏可能在其它单元上绑定过纹理，此处确保单元 0 就是当前纹理）
			glActiveTexture(GL_TEXTURE0);
			org.lwjgl.opengl.GL11.glBindTexture(GL_TEXTURE_2D, boundTexture);
		}

		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		// 复用上传缓冲（避免每批次分配直接缓冲造成 GC 扰动/卡顿）
		if (vboBuf == null || vboBuf.capacity() < n * 56) {
			vboBuf = BufferUtils.createFloatBuffer(n * 56);
		}
		vboBuf.clear();
		vboBuf.put(data, 0, n * 56);
		vboBuf.flip();
		glBufferData(GL_ARRAY_BUFFER, vboBuf, GL_STREAM_DRAW);
		glDrawArrays(mode, 0, n);
		glBindVertexArray(0);
		vertCount = 0;
	}

	private static int getUniform(int prog, HashMap<Integer, Integer> cache, String name) {
		Integer u = cache.get(prog);
		if (u == null) {
			u = glGetUniformLocation(prog, name);
			cache.put(prog, u);
		}
		return u == null ? -1 : u;
	}

	private static void ensureResources() {
		if (vao != 0) { return; }
		vao = glGenVertexArrays();
		vbo = glGenBuffers();
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		int stride = 56 * 4;
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 8);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 24);
		for (int s = 0; s < GEN_SLOTS; s++) {
			int loc = 3 + s;
			glEnableVertexAttribArray(loc);
			glVertexAttribPointer(loc, 4, GL_FLOAT, false, stride, 32 + s * 16);
		}
		glBindVertexArray(0);
		batchProg = createBatchProgram();
	}

	private static int createBatchProgram() {
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs,
			"#version 330 core\n" +
			"layout(location=0) in vec2 aPos;\n" +
			"layout(location=1) in vec4 aColor;\n" +
			"layout(location=2) in vec2 aTex;\n" +
			"uniform mat4 uProj;\n" +
			"uniform mat4 uModel;\n" +
			"out vec4 vColor;\n" +
			"out vec2 vTex;\n" +
			"void main() {\n" +
			"    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);\n" +
			"    vColor = aColor;\n" +
			"    vTex = aTex;\n" +
			"}\n");
		glCompileShader(vs);
		if (glGetShaderi(vs, GL_COMPILE_STATUS) == GL_FALSE) {
			throw new RuntimeException("GLCompat batch vertex shader compile failed: " + glGetShaderInfoLog(vs));
		}
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs,
			"#version 330 core\n" +
			"uniform sampler2D uTex;\n" +
			"uniform int uHasTex;\n" +
			"in vec4 vColor;\n" +
			"in vec2 vTex;\n" +
			"layout(location=0) out vec4 fragColor;\n" +
			"void main() {\n" +
			"    fragColor = uHasTex == 1 ? texture(uTex, vTex) * vColor : vColor;\n" +
			"}\n");
		glCompileShader(fs);
		if (glGetShaderi(fs, GL_COMPILE_STATUS) == GL_FALSE) {
			throw new RuntimeException("GLCompat batch fragment shader compile failed: " + glGetShaderInfoLog(fs));
		}
		int p = glCreateProgram();
		glAttachShader(p, vs);
		glAttachShader(p, fs);
		glLinkProgram(p);
		glDeleteShader(vs);
		glDeleteShader(fs);
		if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE) {
			throw new RuntimeException("GLCompat batch program link failed: " + glGetProgramInfoLog(p));
		}
		return p;
	}

	/** 释放批渲染资源（引擎 destroy 时调用）。 */
	public static void destroy() {
		if (vao != 0) { glDeleteVertexArrays(vao); glDeleteBuffers(vbo); vao = 0; vbo = 0; }
		if (batchProg != 0) { glDeleteProgram(batchProg); batchProg = 0; }
	}
}
