package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;

public strictfp class LightMapLayer implements UniScreen.VisualLayer {
	@Override
	public void tick(Input in, int ms, UniScreen us) {}
	
	public static Texture lightCones;
	public static Texture lightConesStrong;
	public static Texture ltrGradient;
	public static Texture bottomLeftRadialGradient;
	public static ShaderProgram shader;
	
	@Override
	public void draw(MyDraw __d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (!Appearance.useLighting || Appearance.useSimpleGraphics || Appearance.shaderLoadFailed) { return; }
		TimeOfDay tod = us.getTimeOfDay();
		try {
			if (lightCones == null) {
				lightCones = SpriteUtils.loadTexture("lightcones");
				lightConesStrong = SpriteUtils.loadTexture("lightconesStrong");
				ltrGradient = SpriteUtils.loadTexture("ltrGradient");
				bottomLeftRadialGradient = SpriteUtils.loadTexture("bottomLeftRadialGradient");
			}
			if (lightCones == null) {
				AirshipGame.instance.reportError("Unable to load lightCones.\n" + SpriteUtils.loadImageReport("lightcones"), null, null, false, true);
				Appearance.shaderLoadFailed = true;
			}
			if (lightConesStrong == null) {
				AirshipGame.instance.reportError("Unable to load lightConesStrong.\n" + SpriteUtils.loadImageReport("lightConesStrong"), null, null, false, true);
				Appearance.shaderLoadFailed = true;
			}
			if (ltrGradient == null) {
				AirshipGame.instance.reportError("Unable to load ltrGradient.\n" + SpriteUtils.loadImageReport("ltrGradient"), null, null, false, true);
				Appearance.shaderLoadFailed = true;
			}
			if (bottomLeftRadialGradient == null) {
				AirshipGame.instance.reportError("Unable to load bottomLeftRadialGradient.\n" + SpriteUtils.loadImageReport("bottomLeftRadialGradient"), null, null, false, true);
				Appearance.shaderLoadFailed = true;
			}
			if (Appearance.shaderLoadFailed) { return; }
			
			/*if (shader == null) {
				try {
					shader = ShaderProgram.loadProgram(
							AGame.getGameDirectoryPath("data/smoothfrag.vert"),
							AGame.getGameDirectoryPath("data/smoothfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					Appearance.shaderLoadFailed = true;
					return;
				}
			}*/
			
			Color light0 = tod.light[0];
			Color light1 = tod.light[1];
			Color light2 = tod.light[2];
			Color light3 = tod.light[3];
			
			if (us.prevTOD != null && us.prevTOD != tod) {
				light0 = UniScreen.mix(us.prevTOD.light[0], light0, (float) us.todMix);
				light1 = UniScreen.mix(us.prevTOD.light[1], light1, (float) us.todMix);
				light2 = UniScreen.mix(us.prevTOD.light[2], light2, (float) us.todMix);
				light3 = UniScreen.mix(us.prevTOD.light[3], light3, (float) us.todMix);
			}
			
			draw(us.lightingMap[0],    0,    0, __d, us, light0, 0, cropX, cropY, cropW, cropH, 0);
			draw(us.lightingMap[1], 0.5f,    0, __d, us, light1, 1, cropX, cropY, cropW, cropH, 1);
			draw(us.lightingMap[2],    0, 0.5f, __d, us, light2, 2, cropX, cropY, cropW, cropH, 2);
			draw(us.lightingMap[3], 0.5f, 0.5f, __d, us, light3, 3, cropX, cropY, cropW, cropH, 3);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void draw(Image img, float srcX, float srcY, MyDraw __d, UniScreen us, Color bg, int side, double cropX, double cropY, double cropW, double cropH, int dir) throws SlickException {
		Graphics g = FBOGraphicsFactory.getGraphicsForImage(img);
		g.setColor(bg);
		g.fillRect(0, 0, __d.frame().mode().width, __d.frame().mode().height);
		g.setColor(Color.white);
		g.scale(1.0f / AGame.LIGHTMAP_DOWNSCALE, 1.0f / AGame.LIGHTMAP_DOWNSCALE);
		g.scale((float) us.zoom, (float) us.zoom);
		g.translate((float) us.adjScrollX, (float) us.adjScrollY);
		//shader.bind();
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		GLCompat.glEnable(GLCompat.GL_TEXTURE_2D);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, lightCones.getTextureID());
		//shader.setUniform1i("tex", 0);
		glBegin(GLCompat.GL_QUADS);
		if (us.combat != null) {
			for (Side s : us.combat.sides) {
				for (Airship as : s.ships) {
					draw(g, as, srcX, srcY, as.showingOutside);
				}
			}
			for (LandFormation lf : us.combat.landFormations) {
				draw(g, lf, srcX, srcY);
			}
			for (Particle p : us.combat.particles) {
				if (p.type.lightClr != null && !p.type.strongLight) {
					double r = p.type.lightRadius * (p.life * 1.0 / p.lifespan) * p.scale;
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, p.x - r, p.y - r, r * 2, r * 2)) {
						continue;
					}
					glColor3f(p.type.lightClr.r / 255f, p.type.lightClr.g / 255f, p.type.lightClr.b / 255f);
					glTexCoord2d(srcX, srcY);
					glVertex2d((float) (p.x - r), (float) (p.y - r));
					glTexCoord2d(srcX, srcY + 0.5f);
					glVertex2d((float) (p.x - r), (float) (p.y + r));
					glTexCoord2d(srcX + 0.5f, srcY + 0.5f);
					glVertex2d((float) (p.x + r), (float) (p.y + r));
					glTexCoord2d(srcX + 0.5f, srcY);
					glVertex2d((float) (p.x + r), (float) (p.y - r));
				}
			}
		}
		if (us.getSetupFleet() != null) {
			for (Airship as : us.getSetupFleet()) {
				draw(g, as, srcX, srcY, false);
			}
		}
		FloatieVisualLayer fvl = us.visualLayer(FloatieVisualLayer.class);
		boolean deepWater = us.ground() != null && us.ground().landscapeType.deepWater;
		for (BackgroundFloatie bf : fvl.floaties) {
			if (bf.type.underwater && !deepWater) { continue; }
			for (int i = 0; i < bf.type.lights.size(); i++) {
				DecalType.DecalLightSource dls = bf.type.lights.get(i);
				double mx = bf.x;
				if (bf.flipped) {
					mx += (bf.type.app.width() - dls.xOffset) * AGame.SGS;
				} else {
					mx += dls.xOffset * AGame.SGS;
				}
				double my = bf.y + dls.yOffset * AGame.SGS;
				double r = dls.radius;
				glColor3f(dls.clr.r / 255f, dls.clr.g / 255f, dls.clr.b / 255f);
				glTexCoord2d(srcX, srcY);
				glVertex2d((float) (mx - r), (float) (my - r));
				glTexCoord2d(srcX, srcY + 0.5f);
				glVertex2d((float) (mx - r), (float) (my + r));
				glTexCoord2d(srcX + 0.5f, srcY + 0.5f);
				glVertex2d((float) (mx + r), (float) (my + r));
				glTexCoord2d(srcX + 0.5f, srcY);
				glVertex2d((float) (mx + r), (float) (my - r));
			}
		}
		glEnd();
		if (us.combat != null) {
			glBindTexture(GLCompat.GL_TEXTURE_2D, lightConesStrong.getTextureID());
			glBegin(GLCompat.GL_QUADS);
			for (Particle p : us.combat.particles) {
				if (p.type.lightClr != null && p.type.strongLight) {
					double r = p.type.lightRadius * (p.life * 1.0 / p.lifespan) * p.scale;
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, p.x - r, p.y - r, r * 2, r * 2)) {
						continue;
					}
					glColor3f(p.type.lightClr.r / 255f, p.type.lightClr.g / 255f, p.type.lightClr.b / 255f);
					glTexCoord2d(srcX, srcY);
					glVertex2d((float) (p.x - r), (float) (p.y - r));
					glTexCoord2d(srcX, srcY + 0.5f);
					glVertex2d((float) (p.x - r), (float) (p.y + r));
					glTexCoord2d(srcX + 0.5f, srcY + 0.5f);
					glVertex2d((float) (p.x + r), (float) (p.y + r));
					glTexCoord2d(srcX + 0.5f, srcY);
					glVertex2d((float) (p.x + r), (float) (p.y - r));
				}
			}
			glEnd();
		}
		glBindTexture(GLCompat.GL_TEXTURE_2D, ltrGradient.getTextureID());
		glBegin(GLCompat.GL_QUADS);
		if (us.combat != null) {
			for (Side s : us.combat.sides) {
				for (Airship as : s.ships) {
					drawBeams(g, as, dir);
				}
			}
		}
		glEnd();
		glBindTexture(GLCompat.GL_TEXTURE_2D, bottomLeftRadialGradient.getTextureID());
		glBegin(GLCompat.GL_QUADS);
		if (us.combat != null) {
			for (Side s : us.combat.sides) {
				for (Airship as : s.ships) {
					drawBeamEndcaps(g, as, dir);
				}
			}
		}
		glEnd();
			
		glColor3f(1.0f, 1.0f, 1.0f);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		g.resetTransform();
		//shader.unbind();
		TextureImpl.bindNone();
		GLCompat.glDisable(GLCompat.GL_TEXTURE_2D);
		// 恢复默认帧缓冲/投影/模型视图/视口（防止 FBO 状态泄漏导致主渲染出现透视/深度错误）
		g.flush();
	}
	
	public void drawBeams(Graphics g, Airship sh, int dir) {
		int msz = sh.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = sh.modules.get(mi);
			if (m.firingBeam) {
				BeamSpec bs = m.type.beamSpec(sh.currentBonuses);
				if (bs.lightClr != null) {
					// So each direction is up/down/left/right. And we want to make the gradient go from the beam in that direction.
					// First, we want the beam locations.
					Pt mz = m.currentMuzzle();
					double startX = mz.x;
					double startY = mz.y;
					double beamTimeProportion = m.beamAge * 1.0 / (bs.fadeInTime + bs.steadyTime + bs.fadeOutTime);
					double endX = m.beamStartPositionX * (1 - beamTimeProportion) + m.beamEndPositionX * beamTimeProportion;
					double endY = m.beamStartPositionY * (1 - beamTimeProportion) + m.beamEndPositionY * beamTimeProportion;
					double intensity = 1;
					if (m.beamAge < bs.fadeInTime) {
						intensity = m.beamAge * 1.0 / bs.fadeInTime;
					} else if (m.beamAge > bs.fadeInTime + bs.steadyTime && bs.fadeOutTime > 0) {
						intensity = 1 - ((m.beamAge - bs.fadeInTime - bs.steadyTime) * 1.0 / bs.fadeOutTime);
					}
					float r = (float) (bs.lightRadius * intensity);
					
					double beamAngle = Math.atan2(endY - startY, endX - startX);
					double horizontal = Math.sin(beamAngle);
					double vertical = Math.sin(beamAngle + Math.PI / 2);
					
					// So let's just figure it out for the gradient going right.
					// Really basic version: we just add in extra x. That kinda works actually.
					glColor3f(bs.lightClr.r / 255f, bs.lightClr.g / 255f, bs.lightClr.b / 255f);
					
					// I arrived at this logic empirically. I am so sorry.
					int side = dir > 0 && dir < 3 ? -1 : 1;
					if (dir % 2 == 0 && (endY > startY) != (endX > startX)) {
						side *= -1;
					}
					if (endX < startX) {
						side *= -1;
					}
					glTexCoord2d(0.01f, 0.01f);
					glVertex2d((float) startX, (float) startY);
					glTexCoord2d(0.01f, 0.99f);
					glVertex2d((float) endX, (float) endY);
					glTexCoord2d(0.99f, 0.99f);
					glVertex2d((float) endX + r * horizontal * side, (float) endY - r * vertical * side);
					glTexCoord2d(0.99f, 0.01f);
					glVertex2d((float) startX + r * horizontal * side, (float) startY - r * vertical * side);
				}
			}
		}
	}
	
	public void drawBeamEndcaps(Graphics g, Airship sh, int dir) {
		int msz = sh.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = sh.modules.get(mi);
			if (m.firingBeam) {
				BeamSpec bs = m.type.beamSpec(sh.currentBonuses);
				if (bs.lightClr != null) {
					// So each direction is up/down/left/right. And we want to make the gradient go from the beam in that direction.
					// First, we want the beam locations.
					Pt mz = m.currentMuzzle();
					double startX = mz.x;
					double startY = mz.y;
					double beamTimeProportion = m.beamAge * 1.0 / (bs.fadeInTime + bs.steadyTime + bs.fadeOutTime);
					double endX = m.beamStartPositionX * (1 - beamTimeProportion) + m.beamEndPositionX * beamTimeProportion;
					double endY = m.beamStartPositionY * (1 - beamTimeProportion) + m.beamEndPositionY * beamTimeProportion;
					double intensity = 1;
					if (m.beamAge < bs.fadeInTime) {
						intensity = m.beamAge * 1.0 / bs.fadeInTime;
					} else if (m.beamAge > bs.fadeInTime + bs.steadyTime && bs.fadeOutTime > 0) {
						intensity = 1 - ((m.beamAge - bs.fadeInTime - bs.steadyTime) * 1.0 / bs.fadeOutTime);
					}
					float r = (float) (bs.lightRadius * intensity);
					
					double beamAngle = Math.atan2(endY - startY, endX - startX);
					double horizontal = Math.sin(beamAngle);
					double horizontalAlong = Math.cos(beamAngle);
					double vertical = Math.sin(beamAngle + Math.PI / 2);
					double verticalAlong = Math.cos(beamAngle + Math.PI / 2);
					
					// So let's just figure it out for the gradient going right.
					// Really basic version: we just add in extra x. That kinda works actually.
					glColor3f(bs.lightClr.r / 255f, bs.lightClr.g / 255f, bs.lightClr.b / 255f);
					
					// I arrived at this logic empirically. I am so sorry.
					int side = dir > 0 && dir < 3 ? -1 : 1;
					if (dir % 2 == 0 && (endY > startY) != (endX > startX)) {
						side *= -1;
					}
					if (endX < startX) {
						side *= -1;
					}
					glTexCoord2d(0.01f, 0.99f);
					glVertex2d((float) endX, (float) endY);
					glTexCoord2d(0.01f, 0.01f);
					glVertex2d((float) endX + r * horizontalAlong, (float) endY - r * verticalAlong);
					glTexCoord2d(0.99f, 0.01f);
					glVertex2d((float) endX + r * horizontalAlong + r * horizontal * side, (float) endY - r * verticalAlong - r * vertical * side);
					glTexCoord2d(0.99f, 0.99f);
					glVertex2d((float) endX + r * horizontal * side, (float) endY - r * vertical * side);
				}
			}
		}
	}
	
	public void draw(Graphics g, LandFormation lf, float srcX, float srcY) {
		for (int gy = 0; gy < lf.grid.length; gy++) {
			for (int gx = 0; gx < lf.grid[0].length; gx++) {
				LandBlockType lbt = lf.grid[gy][gx];
				if (lbt.lightClr == null) { continue; }
				double mx = lf.getX() + gx * AGame.SGS + lbt.lightX;
				double my = lf.getY() + gy * AGame.SGS + lbt.lightY;
				double r = lbt.lightRadius;
				glColor3f(lbt.lightClr.r / 255f, lbt.lightClr.g / 255f, lbt.lightClr.b / 255f);
				glTexCoord2d(srcX, srcY);
				glVertex2d((float) (mx - r), (float) (my - r));
				glTexCoord2d(srcX, srcY + 0.5f);
				glVertex2d((float) (mx - r), (float) (my + r));
				glTexCoord2d(srcX + 0.5f, srcY + 0.5f);
				glVertex2d((float) (mx + r), (float) (my + r));
				glTexCoord2d(srcX + 0.5f, srcY);
				glVertex2d((float) (mx + r), (float) (my - r));
			}
		}
	}
	
	public void draw(Graphics g, Airship sh, float srcX, float srcY, boolean outsideOnly) {
		int msz = sh.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = sh.modules.get(mi);
			if (m.glowAmt > 0) {
				ArrayList<ModuleType.ModuleLightSource> lights = m.type.getLights(sh.currentBonuses);
				int lsz = lights.size();
				for (int li = 0; li < lsz; li++) {
					ModuleType.ModuleLightSource mls = lights.get(li);
					if (outsideOnly && !mls.outside) { continue; }
					double mx = sh.getX() + sh.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
					if (sh.flipped) {
						mx += (m.type.getW() - mls.xOffset) * AGame.SGS;
					} else {
						mx += mls.xOffset * AGame.SGS;
					}
					double my = sh.getY() + m.y * AGame.SGS + mls.yOffset * AGame.SGS;
					double r = mls.radius * m.glowAmt;
					glColor3f(mls.clr.r / 255f, mls.clr.g / 255f, mls.clr.b / 255f);
					glTexCoord2d(srcX, srcY);
					glVertex2d((float) (mx - r), (float) (my - r));
					glTexCoord2d(srcX, srcY + 0.5f);
					glVertex2d((float) (mx - r), (float) (my + r));
					glTexCoord2d(srcX + 0.5f, srcY + 0.5f);
					glVertex2d((float) (mx + r), (float) (my + r));
					glTexCoord2d(srcX + 0.5f, srcY);
					glVertex2d((float) (mx + r), (float) (my - r));
				}
			}
		}
		
		int dsz = sh.decals.size();
		for (int di = 0; di < dsz; di++) {
			Decal d = sh.decals.get(di);
			if (!d.enabled) { continue; }
			ArrayList<DecalType.DecalLightSource> lights = d.type.lights;
			int lsz = lights.size();
			for (int li = 0; li < lsz; li++) {
				DecalType.DecalLightSource dls = lights.get(li);
				double mx = sh.getX() + sh.gridXToWorldX(d.x, d.type.imgW) * AGame.SGS;
				if (sh.flipped) {
					mx += (d.type.imgW - dls.xOffset) * AGame.SGS;
				} else {
					mx += dls.xOffset * AGame.SGS;
				}
				double my = sh.getY() + d.y * AGame.SGS + dls.yOffset * AGame.SGS;
				double r = dls.radius;
				glColor3f(dls.clr.r / 255f, dls.clr.g / 255f, dls.clr.b / 255f);
				glTexCoord2d(srcX, srcY);
				glVertex2d((float) (mx - r), (float) (my - r));
				glTexCoord2d(srcX, srcY + 0.5f);
				glVertex2d((float) (mx - r), (float) (my + r));
				glTexCoord2d(srcX + 0.5f, srcY + 0.5f);
				glVertex2d((float) (mx + r), (float) (my + r));
				glTexCoord2d(srcX + 0.5f, srcY);
				glVertex2d((float) (mx + r), (float) (my - r));
			}
		}
	}
}
