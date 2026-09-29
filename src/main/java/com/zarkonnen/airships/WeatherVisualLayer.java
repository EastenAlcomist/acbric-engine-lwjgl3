package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import java.util.ArrayList;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2f;
import org.newdawn.slick.opengl.TextureImpl;

public class WeatherVisualLayer implements UniScreen.VisualLayer {
	ArrayList<Zone> zones = new ArrayList<Zone>();
	ArrayList<Zone> oldZones = new ArrayList<Zone>();
	TimeOfDay currentTOD;
	
	public static class Drop {
		public double x;
		public double y;
		public double dx;
		public double dy;
		public double normalDx;
		public double normalDy;

		public Drop(double x, double y, double dx, double dy) {
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			double l = Math.sqrt(dx * dx + dy * dy);
			normalDx = dx / l;
			normalDy = dy / l;
		}
	}
	
	class Zone {
		WeatherEffect.PrecipitationSpawnZone spec;
		ArrayList<Drop> drops = new ArrayList<Drop>();
		Zone(WeatherEffect.PrecipitationSpawnZone spec) {
			this.spec = spec;
		}
	}
	
	public static final int N_SNOW = 1000;
	public static final int N_RAIN = 120;
	public static final int N_FEW_STARS = 100;
	public static final int N_MANY_STARS = 1000;
	
	public static final Clr RAIN_C = new Clr(191, 191, 191, 128);
	public static final Clr DUST_C = new Clr(51, 26, 8, 128);
	public static final Clr STAR_C = new Clr(255, 255, 255, 128);
	public static final Clr STAR_C_BRIGHT = new Clr(255, 255, 255, 192);
	
	private int ticksDone = 0;
			
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (ticksDone++ < 3) { return; }
		if (!us.isTimeMoving()) { ms = 0; }
		TimeOfDay tod = us.getTimeOfDay();
		if (currentTOD != tod) {
			currentTOD = tod;
			oldZones.clear();
			oldZones.addAll(zones);
			zones.clear();
		}
		if (us.prevTOD == null || us.prevTOD == tod) {
			oldZones.clear();
		}
		if (!tod.effect.precipitationSpawnZones.isEmpty() && zones.isEmpty()) {
			for (WeatherEffect.PrecipitationSpawnZone spec : tod.effect.precipitationSpawnZones) {
				Zone z = new Zone(spec);
				zones.add(z);
				z.drops.ensureCapacity(spec.drops);
				for (int i = 0; i < spec.drops; i++) {
					double dx = spec.minXSpeed + AGame.ANIM_R.nextDouble() * (spec.maxXSpeed - spec.minXSpeed);
					double dy = spec.minYSpeed + AGame.ANIM_R.nextDouble() * (spec.maxYSpeed - spec.minYSpeed);
					double startX = spec.x + AGame.ANIM_R.nextDouble() * spec.w;
					double startY = spec.y + AGame.ANIM_R.nextDouble() * spec.h;
					double distRange = 0;
					if (dx != 0) {
						distRange = 6400 / Math.abs(dx);
					}
					if (dy != 0) {
						if (distRange == 0) {
							distRange = 4000 / Math.abs(dy);
						} else {
							distRange = Math.min(distRange, 4000 / Math.abs(dy));
						}
					}
					double dist = AGame.ANIM_R.nextDouble() * distRange;
					z.drops.add(new Drop(startX + dist * dx, startY + dist * dy, dx, dy));
				}
			}
		}
		for (int zsi = 0; zsi < 2; zsi++) {
			ArrayList<Zone> zs = zsi == 0 ? zones : oldZones;
			for (int zi = 0; zi < zs.size(); zi++) {
				Zone zone = zs.get(zi);
				for (int i = 0; i < zone.drops.size(); i++) {
					Drop d = zone.drops.get(i);
					d.x += d.dx * ms;
					d.y += d.dy * ms;
					boolean pleaseRespawn = false;
					if (zone.spec.sticksAs != null && us.combat != null && us.combat.physics != null) {
						int bsz = us.combat.physics.bodies.size();
						for (int bi = 0; bi < bsz; bi++) {
							Body b = us.combat.physics.bodies.get(bi);
							if (b == us.combat.landFormations.get(0)) { continue; }
							if (b.canParticleStick(d.x, d.y)) {
								Particle snowP = new Particle(zone.spec.sticksAs, d.x - b.getX(), d.y - b.getY(), 0, 0);
								b.stuckParticles.add(snowP);
								pleaseRespawn = true;
							}
						}
					}
					pleaseRespawn = pleaseRespawn ||
							(d.dy > 0 && d.y > AGame.GROUND_LEVEL) ||
							(d.dy < 0 && d.y < AGame.GROUND_LEVEL - 2500) ||
							(d.dx > 0 && d.x > us.combatAreaW() / 2) ||
							(d.dx < 0 && d.x < -us.combatAreaW() / 2);

					if (pleaseRespawn) {
						d.x = zone.spec.x + AGame.ANIM_R.nextDouble() * zone.spec.w;
						d.y = zone.spec.y + AGame.ANIM_R.nextDouble() * zone.spec.h;
					}
				}
			}
		}
		
		if (us.combat != null && tod.effect.occasionalSound != null && AGame.ANIM_R.nextDouble() < tod.effect.occasionalSoundChance * ms) {
			us.combat.play(tod.effect.occasionalSound, AGame.ANIM_R.nextInt(us.combatAreaW()) - us.combatAreaW() / 2, AGame.GROUND_LEVEL - AGame.ANIM_R.nextInt(2000), 0, 0, false);
		}
		if (us.combat != null && tod.effect.backgroundLoop != null) {
			us.combat.loop("weatherBackground-" + tod.effect.name, tod.effect.backgroundLoop, 0, AGame.GROUND_LEVEL - 500, 0, 0, 1, false);
		}
	}

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (SimplePref.REDUCED_VISUAL_NOISE.get()) { return; }
		TimeOfDay tod = us.getTimeOfDay();
		WeatherEffect ef = tod.effect;
		TextureImpl.bindNone();
		glBegin(GL_QUADS);
		for (int zsi = 0; zsi < 2; zsi++) {
			ArrayList<Zone> zs = zsi == 0 ? zones : oldZones;
			float aMult = 1;
			if (us.prevTOD != tod) {
				aMult = (float) (zsi == 0 ? us.todMix : (1 - us.todMix));
			}
			for (int zi = 0; zi < zs.size(); zi++) {
				Zone z = zs.get(zi);
				glColor4f(z.spec.clr.r / 255f, z.spec.clr.g / 255f, z.spec.clr.b / 255f, z.spec.clr.a / 255f * aMult);
				double w = Math.max(1.1 / us.zoom, z.spec.dropW);
				double h = Math.max(1.1 / us.zoom, z.spec.dropL);
				for (int i = 0; i < z.drops.size(); i++) {
					Drop drop = z.drops.get(i);
					if (Rect2D.contains(cropX, cropY, cropW, cropH, drop.x, drop.y)) {
						glVertex2d(drop.x, drop.y);
						glVertex2d(drop.x + drop.normalDy * w, drop.y - drop.normalDx * w);
						glVertex2d(drop.x + drop.normalDy * w + drop.normalDx * h, drop.y - drop.normalDx * w + drop.normalDy * h);
						glVertex2d(drop.x + drop.normalDx * h, drop.y + drop.normalDy * h);
					}
				}
			}
		}
		glEnd();
	}
}
