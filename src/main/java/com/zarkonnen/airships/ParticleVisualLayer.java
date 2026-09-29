package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import java.util.HashSet;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;

public strictfp class ParticleVisualLayer implements UniScreen.VisualLayer {
	private final HashSet<SpritesheetBundle> additionalSSBs = new HashSet<SpritesheetBundle>();
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {}

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (us.combat == null) { return; }
		TimeOfDay tod = us.getTimeOfDay();
						
		TextureImpl.bindNone();
		glColor3f(1.0f, 1.0f, 1.0f);
		glBegin(GL_QUADS);
		Color ambient = us.ambient(tod);
		float ambientR = ambient.r, ambientG = ambient.g, ambientB = ambient.b;
		if (us.combat.physics != null) {
			for (Body b : us.combat.physics.bodies) {
				if (b instanceof Airship && !((Airship) b).showingOutside) { continue; }
				for (Particle p : b.stuckParticles) {
					double px = (int) b.getX() + p.x;
					double py = (int) b.getY() + p.y;
					if (p.type.pt == null && Rect2D.intersects(cropX, cropY, cropW, cropH, px, py, p.type.maxSize, p.type.maxSize)) {
						if (p.life > p.lifespan) { p.life = p.lifespan; }
						double sz = (p.startSize + 1.0 * (p.endSize - p.startSize) * (p.lifespan - p.life) / p.lifespan) / 2;
						int index = Math.min((p.type.gradient.length - 1) * (p.lifespan - p.life) / p.lifespan, p.type.gradient.length);
						float[] c = p.type.gradient[index];
						if (p.type.lightClr == null) {
							glColor4f(c[0] * ambientR, c[1] * ambientG, c[2] * ambientB, c[3]);
						} else {
							glColor4f(c[0], c[1], c[2], c[3]);
						}
						glVertex2d(px + sz, py - sz);
						glVertex2d(px + sz, py + sz);
						glVertex2d(px - sz, py + sz);
						glVertex2d(px - sz, py - sz);
					}
				}
			}
		}
		
		for (Particle p : us.combat.particles) {
			if (p.type.pt == null && Rect2D.intersects(cropX, cropY, cropW, cropH, p.x, p.y, p.type.maxSize, p.type.maxSize)) {
				if (p.life > p.lifespan) { p.life = p.lifespan; }
				double sz = (p.startSize + 1.0 * (p.endSize - p.startSize) * (p.lifespan - p.life) / p.lifespan) / 2;
				int index = Math.min((p.type.gradient.length - 1) * (p.lifespan - p.life) / p.lifespan, p.type.gradient.length - 1);
				float[] c = p.type.gradient[index];
				if (p.type.lightClr == null) {
					glColor4f(c[0] * ambientR, c[1] * ambientG, c[2] * ambientB, c[3]);
				} else {
					glColor4f(c[0], c[1], c[2], c[3]);
				}
				glVertex2d(p.x + sz, p.y - sz);
				glVertex2d(p.x + sz, p.y + sz);
				glVertex2d(p.x - sz, p.y + sz);
				glVertex2d(p.x - sz, p.y - sz);
			}
		}
		glEnd();
		
		SpritesheetBundle base = SpritesheetBundle.ofName("particles");
		additionalSSBs.clear();
		drawAsPicture(d, us, cropX, cropY, cropW, cropH, base, additionalSSBs);
		for (SpritesheetBundle ssb : additionalSSBs) {
			drawAsPicture(d, us, cropX, cropY, cropW, cropH, ssb, null);
		}

		if (!Appearance.useLighting || Appearance.useSimpleGraphics || Appearance.shaderLoadFailed) { return; }
				
		Image[] light = us.lightingMap;
		
		if (Appearance.lsp == null) {
			try {
				Appearance.lsp = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/litfrag.vert"),
						AGame.getStaticGameDirectoryPath("data/litfrag.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				Appearance.shaderLoadFailed = true;
				return;
			}
		}
		
		additionalSSBs.clear();
		drawAsShadedPicture(d, us, cropX, cropY, cropW, cropH, base, additionalSSBs, light, tod);
		for (SpritesheetBundle ssb : additionalSSBs) {
			drawAsShadedPicture(d, us, cropX, cropY, cropW, cropH, ssb, null, light, tod);
		}
	}
	
	private void drawAsPicture(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs) {
		GLCompat.glEnable(GLCompat.GL_TEXTURE_2D);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex("").getTextureID());
		glBegin(GL_QUADS);
		for (Particle p : us.combat.particles) {
			if (p.type.pt != null && (!p.type.lit || !Appearance.useLighting || Appearance.useSimpleGraphics || Appearance.shaderLoadFailed) && Rect2D.intersects(cropX, cropY, cropW, cropH, p.x, p.y, p.type.maxSize, p.type.maxSize)) {
				if (p.pic != null && p.pic.ssb != ssb) {
					if (additionalSSBs != null) {
						additionalSSBs.add(p.pic.ssb);
					}
					continue;
				}
				p.drawAsPicture(d, 0, 0);
			}
		}
		glEnd();
		TextureImpl.bindNone();
	}
	
	private void drawAsShadedPicture(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, Image[] light, TimeOfDay tod) {
		Appearance.lsp.bind();
				
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.bumpTex.getTextureID());
		Appearance.lsp.setUniform1i("bump", 1);

		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
		Appearance.lsp.setUniform1i("lightFromLeft", 2);

		glActiveTexture(GL_TEXTURE3);
		glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
		Appearance.lsp.setUniform1i("lightFromTop", 3);

		glActiveTexture(GL_TEXTURE4);
		glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
		Appearance.lsp.setUniform1i("lightFromRight", 4);

		glActiveTexture(GL_TEXTURE5);
		glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
		Appearance.lsp.setUniform1i("lightFromBottom", 5);

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(Appearance.currentPostfix).getTextureID());
		Appearance.lsp.setUniform1i("tex", 0);

		//Appearance.lsp.setUniform1f("strength", lightStrength);
		Appearance.lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
		Appearance.lsp.setUniform1f("screenHeight", d.frame().mode().height);
		Appearance.lsp.setUniform4f("ambient", tod.ambient);
		Appearance.lsp.setUniform1f("ambientSaturation", tod.ambientSaturation);
		Appearance.lsp.setUniform1f("texSize", ssb.bumpTex.getTextureWidth());

		Appearance.lsp.enableVertexAttribute("flipped");
		Appearance.lsp.enableVertexAttribute("strength");
		Appearance.lsp.enableVertexAttribute("tint");
		
		glBegin(GL_QUADS);
		GLCompat.glVertexAttrib1f(Appearance.lsp.getAttributeID("flipped"), 0f);
		GLCompat.glVertexAttrib1f(Appearance.lsp.getAttributeID("strength"), us.lightStrength(tod));
		for (Particle p : us.combat.particles) {
			if (p.type.pt != null && p.type.lit && Rect2D.intersects(cropX, cropY, cropW, cropH, p.x, p.y, p.type.maxSize, p.type.maxSize)) {
				if (p.pic != null && p.pic.ssb != ssb) {
					if (additionalSSBs != null) {
						additionalSSBs.add(p.pic.ssb);
					}
					continue;
				}
				p.drawAsShadedPicture(d);
			}
		}
		glEnd();
		glColor3f(1.0f, 1.0f, 1.0f);
		Appearance.lsp.unbind();
		TextureImpl.bindNone();
	}
}
