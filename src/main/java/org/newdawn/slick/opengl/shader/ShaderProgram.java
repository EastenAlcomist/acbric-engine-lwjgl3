package org.newdawn.slick.opengl.shader;

import com.zarkonnen.catengine.lwjgl3.GlProgram;
import org.newdawn.slick.SlickException;

/** Slick2D ShaderProgram 兼容层，包装后端 GlProgram。 */
public class ShaderProgram {
	private final GlProgram prog;

	private ShaderProgram(GlProgram prog) { this.prog = prog; }

	public static ShaderProgram loadProgram(String vert, String frag) throws SlickException {
		try {
			return new ShaderProgram(GlProgram.loadProgram(vert, frag));
		} catch (RuntimeException e) {
			throw new SlickException("Unable to load shader program", e);
		}
	}

	public void bind() { prog.bind(); }
	public void unbind() { prog.unbind(); }
	public int getID() { return prog.getID(); }
	public boolean valid() { return true; }
	public int getUniformID(String name) { return prog.getUniformID(name); }
	public int getAttributeID(String name) { return prog.getAttributeID(name); }
	public boolean enableVertexAttribute(String name) { return prog.enableVertexAttribute(name); }
	public boolean disableVertexAttribute(String name) { return prog.disableVertexAttribute(name); }
	public void setUniform1f(String n, float v) { prog.setUniform1f(n, v); }
	public void setUniform1i(String n, int v) { prog.setUniform1i(n, v); }
	public void setUniform2f(String n, float a, float b) { prog.setUniform2f(n, a, b); }
	public void setUniform3f(String n, float a, float b, float c) { prog.setUniform3f(n, a, b, c); }
	public void setUniform4f(String n, float a, float b, float c, float d) { prog.setUniform4f(n, a, b, c, d); }
	public void setUniform4f(String n, org.newdawn.slick.Color c) { prog.setUniform4f(n, c.r, c.g, c.b, c.a); }
	public void setUniform2i(String n, int a, int b) { prog.setUniform2i(n, a, b); }
	public void setUniform3i(String n, int a, int b, int c) { prog.setUniform3i(n, a, b, c); }
	public void setUniform4i(String n, int a, int b, int c, int d) { prog.setUniform4i(n, a, b, c, d); }
	public void release() { prog.release(); }
}
