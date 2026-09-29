package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import com.zarkonnen.catengine.util.Pt;
import java.io.InputStream;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;

public class LightHaloLayer implements UniScreen.VisualLayer {
	@Override
	public void tick(Input in, int ms, UniScreen us) {}
	public static Texture light;

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (!Appearance.useLighting || Appearance.useSimpleGraphics || Appearance.shaderLoadFailed) { return; }
		try {
			if (light == null) {
				light = SpriteUtils.loadTexture("light_hardb");
			}
			
			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
			GLCompat.glEnable(GLCompat.GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light.getTextureID());
			glBegin(GLCompat.GL_QUADS);
			
			TimeOfDay tod = us.getTimeOfDay();
			double haloStrength;
			if (us.prevTOD != null && us.prevTOD != tod) {
				haloStrength = tod.haloStrength * us.todMix + us.prevTOD.haloStrength * (1 - us.todMix);
			} else {
				haloStrength = tod.haloStrength;
			}
			draw(d, us, haloStrength, cropX, cropY, cropW, cropH);
			
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			TextureImpl.bindNone();
			GLCompat.glDisable(GLCompat.GL_TEXTURE_2D);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void draw(MyDraw __d, UniScreen us, double strength, double cropX, double cropY, double cropW, double cropH) throws SlickException {
		if (us.combat != null) {
			for (Side s : us.combat.sides) {
				for (Airship as : s.ships) {
					draw(as, strength);
				}
			}
			for (LandFormation lf : us.combat.landFormations) {
				draw(lf, strength);
			}
			for (Particle p : us.combat.particles) {
				if (p.type.lightClr != null) {
					double r = p.type.lightRadius * p.type.haloStrength * (p.life * 1.0 / p.lifespan) * strength;
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, p.x - r, p.y - r, r * 2, r * 2)) {
						continue;
					}
					glColor3f(p.type.lightClr.r / 255f, p.type.lightClr.g / 255f, p.type.lightClr.b / 255f);
					glTexCoord2d(0, 0);
					glVertex2d((float) (p.x - r), (float) (p.y - r));
					glTexCoord2d(0, 1f);
					glVertex2d((float) (p.x - r), (float) (p.y + r));
					glTexCoord2d(1f, 1f);
					glVertex2d((float) (p.x + r), (float) (p.y + r));
					glTexCoord2d(1f, 0f);
					glVertex2d((float) (p.x + r), (float) (p.y - r));
				}
			}
		}
		if (us.getSetupFleet() != null) {
			for (Airship as : us.getSetupFleet()) {
				draw(as, strength);
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
				double r = dls.radius * strength / 7;
				if (bf.type.underwater) {
					r = dls.radius;
				}

				glColor3f(dls.clr.r / 255f / 2, dls.clr.g / 255f / 2, dls.clr.b / 255f / 2);
				glTexCoord2d(0, 0);
				glVertex2d((float) (mx - r), (float) (my - r));
				glTexCoord2d(0, 1);
				glVertex2d((float) (mx - r), (float) (my + r));
				glTexCoord2d(1, 1);
				glVertex2d((float) (mx + r), (float) (my + r));
				glTexCoord2d(1, 0);
				glVertex2d((float) (mx + r), (float) (my - r));
			}
		}
	}
	
	public void draw(LandFormation lf, double strength) {
		for (int gy = 0; gy < lf.grid.length; gy++) {
			for (int gx = 0; gx < lf.grid[0].length; gx++) {
				LandBlockType lbt = lf.grid[gy][gx];
				if (lbt.lightClr == null) { continue; }
				double mx = lf.getX() + gx * AGame.SGS + lbt.lightX;
				double my = lf.getY() + gy * AGame.SGS + lbt.lightY;
				double r = lbt.lightRadius * strength / 7;
				glColor3f(lbt.lightClr.r / 255f / 2, lbt.lightClr.g / 255f / 2, lbt.lightClr.b / 255f / 2);
				glTexCoord2d(0, 0);
				glVertex2d((float) (mx - r), (float) (my - r));
				glTexCoord2d(0, 1);
				glVertex2d((float) (mx - r), (float) (my + r));
				glTexCoord2d(1, 1);
				glVertex2d((float) (mx + r), (float) (my + r));
				glTexCoord2d(1, 0);
				glVertex2d((float) (mx + r), (float) (my - r));
			}
		}
	}
	
	public void draw(Airship sh, double strength) {
		if (sh.tauntTime > 0) {
			glColor3f(0.3f, 0, 0);
			glTexCoord2d(0, 0);
			glVertex2d((float) (sh.getX() - sh.getBBWidth()), (float) (sh.getY() - sh.getBBHeight()));
			glTexCoord2d(0, 1);
			glVertex2d((float) (sh.getX() - sh.getBBWidth()), (float) (sh.getY() + sh.getBBHeight() * 2));
			glTexCoord2d(1, 1);
			glVertex2d((float) (sh.getX() + sh.getBBWidth() * 2), (float) (sh.getY() + sh.getBBHeight() * 2));
			glTexCoord2d(1, 0);
			glVertex2d((float) (sh.getX() + sh.getBBWidth() * 2), (float) (sh.getY() - sh.getBBHeight()));
		}
		int msz = sh.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = sh.modules.get(mi);
			if (m.running()) {
				ArrayList<ModuleType.ModuleLightSource> lights = m.type.getLights(sh.currentBonuses);
				int lsz = lights.size();
				for (int li = 0; li < lsz; li++) {
					ModuleType.ModuleLightSource mls = lights.get(li);
					if (!mls.outside) { continue; }
					double mx = sh.getX() + sh.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
					if (sh.flipped) {
						mx += (m.type.getW() - mls.xOffset) * AGame.SGS;
					} else {
						mx += mls.xOffset * AGame.SGS;
					}
					double my = sh.getY() + m.y * AGame.SGS + mls.yOffset * AGame.SGS;
					double r = mls.radius * strength / 7;

					glColor3f(mls.clr.r / 255f / 2, mls.clr.g / 255f / 2, mls.clr.b / 255f / 2);
					glTexCoord2d(0, 0);
					glVertex2d((float) (mx - r), (float) (my - r));
					glTexCoord2d(0, 1);
					glVertex2d((float) (mx - r), (float) (my + r));
					glTexCoord2d(1, 1);
					glVertex2d((float) (mx + r), (float) (my + r));
					glTexCoord2d(1, 0);
					glVertex2d((float) (mx + r), (float) (my - r));
				}
				
				if (m.firingBeam) {
					BeamSpec bs = m.type.beamSpec(sh.currentBonuses);
					
					Pt mz = m.currentMuzzle();
					double mx = mz.x;
					double my = mz.y;

					double intensity = 1;
					if (m.beamAge < bs.fadeInTime) {
						intensity = m.beamAge * 1.0 / bs.fadeInTime;
					} else if (m.beamAge > bs.fadeInTime + bs.steadyTime && bs.fadeOutTime > 0) {
						intensity = 1 - ((m.beamAge - bs.fadeInTime - bs.steadyTime) * 1.0 / bs.fadeOutTime);
					}
					double r = bs.emitBloomRadius * intensity;
					
					if (bs.emitBloomRadius > 0) {
						glColor4f(bs.emitBloomClr.r / 255f, bs.emitBloomClr.g / 255f, bs.emitBloomClr.b / 255f, bs.emitBloomClr.a / 255f);
						glTexCoord2d(0, 0);
						glVertex2d((float) (mx - r), (float) (my - r));
						glTexCoord2d(0, 1);
						glVertex2d((float) (mx - r), (float) (my + r));
						glTexCoord2d(1, 1);
						glVertex2d((float) (mx + r), (float) (my + r));
						glTexCoord2d(1, 0);
						glVertex2d((float) (mx + r), (float) (my - r));
					}
					
					r *= strength;
					
					if (bs.lightClr != null) {
						double beamTimeProportion = m.beamAge * 1.0 / (bs.fadeInTime + bs.steadyTime + bs.fadeOutTime);
						double endX = m.beamStartPositionX * (1 - beamTimeProportion) + m.beamEndPositionX * beamTimeProportion;
						double endY = m.beamStartPositionY * (1 - beamTimeProportion) + m.beamEndPositionY * beamTimeProportion;

						double a = Math.atan2(endY - my, endX - mx);

						glColor3f(bs.lightClr.r / 255f, bs.lightClr.g / 255f, bs.lightClr.b / 255f);

						glTexCoord2d(0.01f, 0.5f);
						glVertex2d((float) mx + r * Math.cos(a + Math.PI / 2), (float) my + r * Math.sin(a + Math.PI / 2));
						glTexCoord2d(0.01f, 0.5f);
						glVertex2d((float) endX + r * Math.cos(a + Math.PI / 2), (float) endY + r * Math.sin(a + Math.PI / 2));
						glTexCoord2d(0.99f, 0.5f);
						glVertex2d((float) endX + r * Math.cos(a - Math.PI / 2), (float) endY + r * Math.sin(a - Math.PI / 2));
						glTexCoord2d(0.99f, 0.5f);
						glVertex2d((float) mx + r * Math.cos(a - Math.PI / 2), (float) my + r * Math.sin(a - Math.PI / 2));
						
						glTexCoord2d(0.01f, 0.5f);
						glVertex2d((float) endX + r * Math.cos(a + Math.PI / 2), (float) endY + r * Math.sin(a + Math.PI / 2));
						glTexCoord2d(0.01f, 0.99f);
						glVertex2d((float) endX + r * Math.cos(a + Math.PI / 2) + r * Math.cos(a), (float) endY + r * Math.sin(a + Math.PI / 2) + r * Math.sin(a));
						glTexCoord2d(0.99f, 0.99f);
						glVertex2d((float) endX + r * Math.cos(a - Math.PI / 2) + r * Math.cos(a), (float) endY + r * Math.sin(a - Math.PI / 2) + r * Math.sin(a));
						glTexCoord2d(0.99f, 0.5f);
						glVertex2d((float) endX + r * Math.cos(a - Math.PI / 2), (float) endY + r * Math.sin(a - Math.PI / 2));
						
						glTexCoord2d(0.01f, 0.5f);
						glVertex2d((float) mx + r * Math.cos(a + Math.PI / 2), (float) my + r * Math.sin(a + Math.PI / 2));
						glTexCoord2d(0.01f, 0.99f);
						glVertex2d((float) mx + r * Math.cos(a + Math.PI / 2) - r * Math.cos(a), (float) my + r * Math.sin(a + Math.PI / 2) - r * Math.sin(a));
						glTexCoord2d(0.99f, 0.99f);
						glVertex2d((float) mx + r * Math.cos(a - Math.PI / 2) - r * Math.cos(a), (float) my + r * Math.sin(a - Math.PI / 2) - r * Math.sin(a));
						glTexCoord2d(0.99f, 0.5f);
						glVertex2d((float) mx + r * Math.cos(a - Math.PI / 2), (float) my + r * Math.sin(a - Math.PI / 2));
					}
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
				DecalType.DecalLightSource mls = lights.get(li);
				double mx = sh.getX() + sh.gridXToWorldX(d.x, d.type.imgW) * AGame.SGS;
				if (sh.flipped) {
					mx += (d.type.imgW - mls.xOffset) * AGame.SGS;
				} else {
					mx += mls.xOffset * AGame.SGS;
				}
				double my = sh.getY() + d.y * AGame.SGS + mls.yOffset * AGame.SGS;
				double r = mls.radius * strength / 7;

				glColor3f(mls.clr.r / 255f / 2, mls.clr.g / 255f / 2, mls.clr.b / 255f / 2);
				glTexCoord2d(0, 0);
				glVertex2d((float) (mx - r), (float) (my - r));
				glTexCoord2d(0, 1);
				glVertex2d((float) (mx - r), (float) (my + r));
				glTexCoord2d(1, 1);
				glVertex2d((float) (mx + r), (float) (my + r));
				glTexCoord2d(1, 0);
				glVertex2d((float) (mx + r), (float) (my - r));
			}
		}
	}
}
