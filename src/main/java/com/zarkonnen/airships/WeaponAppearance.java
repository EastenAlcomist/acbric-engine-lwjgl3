package com.zarkonnen.airships;

import static com.zarkonnen.airships.ModuleType.img;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Pt;

import java.util.ArrayList;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class WeaponAppearance {
	public Appearance back;
	public boolean onlyShowBarrelIfLoaded;
	public boolean ignoreBarrelRotation;
	public Img[] shot;
	public int shotAnimationInterval = 150;
	public Img[] barrelLoadStages;
	public Img[] flippedBarrelLoadStages;
	public Img barrel, externalBarrel;
	public Img flippedbarrel, externalFlippedbarrel;
	public BarrelAnimation barrelAnimation, externalBarrelAnimation;
	public BarrelAnimation flippedBarrelAnimation, externalFlippedBarrelAnimation;
	public Pt barrelOffset;
	public Pt flippedBarrelOffset;
	public double recoil;
	public double hitExplosionSize;
	public double missExplosionSize;
	public Particle.Emitter shotEmitter;
	public ShotExhaustEmitter exhaust;
	public ParticleType impactParticle;
	public int numImpactParticles;
	public ArrayList<Shell> shells = new ArrayList<Shell>();
	public double fireVibrate;
	public double fireFlash;
	public double fireVibrateDecay;
	public boolean useSubColorForBarrel = true;
	
	public static strictfp class ShellEmitter {
		public ParticleType type;
		public ArrayList<Pt> emitPoints = new ArrayList<Pt>();
		public ShellEmitter flipped(int moduleW) {
			ShellEmitter e2 = new ShellEmitter();
			e2.type = type;
			for (int i = 0; i < emitPoints.size(); i++) {
				e2.emitPoints.add(new Pt(moduleW * AGame.SGS - emitPoints.get(i).x, emitPoints.get(i).y));
			}
			return e2;
		}
		private ShellEmitter() {}
		public ShellEmitter(JSONObject o) {
			type = ParticleType.ofName(o.getString("type"));
			JSONArray a = o.getJSONArray("emitPoints");
			for (int i = 0; i < a.length(); i += 2) {
				emitPoints.add(new Pt(a.getDouble(i), a.getDouble(i + 1)));
			}
		}

		public void emit(Combat c, Module m) {
			double mx = m.ship.getX() + m.ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
			double my = m.ship.getY() + m.y * AGame.SGS;
			for (int i = 0; i < emitPoints.size(); i++) {
				Pt p = emitPoints.get(i);
				double px = m.ship.flipped ? (mx + m.type.getW() * AGame.SGS - p.x) : (mx + p.x);
				double py = my + p.y;
				c.particles.add(new Particle(type, px, py));
			}
		}
	}
	
	public static strictfp class Shell {
		public Img img;
		public Img internalImg;
		public int x, y, pivotX, pivotY;
		public double openAngle;
		public double openShiftX, openShiftY;
		public int startOpenTimeBeforeFiring;
		public int endOpenTimeBeforeFiring;
		public int startCloseTimeAfterFiring;
		public int endCloseTimeAfterFiring;
		public int shellClosedPenAbsorb;
		public int shellClosedBlastAbsorb;
		public ArrayList<ShellEmitter> openEmitters = new ArrayList<ShellEmitter>();
		public ArrayList<ShellEmitter> closeEmitters = new ArrayList<ShellEmitter>();
		public SoundEffect openSound;
		public SoundEffect closeSound;

		private Shell(Img img, Img internalImg, int x, int y, int pivotX, int pivotY, double openAngle, double openShiftX, double openShiftY, int startOpenTimeBeforeFiring, int endOpenTimeBeforeFiring, int startCloseTimeAfterFiring, int endCloseTimeAfterFiring, int shellClosedPenAbsorb, int shellClosedBlastAbsorb) {
			this.img = img;
			this.internalImg = internalImg;
			this.x = x;
			this.y = y;
			this.pivotX = pivotX;
			this.pivotY = pivotY;
			this.openAngle = openAngle;
			this.openShiftX = openShiftX;
			this.openShiftY = openShiftY;
			this.startOpenTimeBeforeFiring = startOpenTimeBeforeFiring;
			this.endOpenTimeBeforeFiring = endOpenTimeBeforeFiring;
			this.startCloseTimeAfterFiring = startCloseTimeAfterFiring;
			this.endCloseTimeAfterFiring = endCloseTimeAfterFiring;
			this.shellClosedPenAbsorb = shellClosedPenAbsorb;
			this.shellClosedBlastAbsorb = shellClosedBlastAbsorb;
		}
		
		public Shell(JSONObject o) {
			img = img(o.getJSONObject("img"));
			internalImg = o.has("internalImg") ? img(o.getJSONObject("internalImg")) : null;
			x = o.getInt("x");
			y = o.getInt("y");
			pivotX = o.optInt("pivotX", 0);
			pivotY = o.optInt("pivotY", 0);
			openShiftX = o.optDouble("openShiftX", 0);
			openShiftY = o.optDouble("openShiftY", 0);
			openAngle = o.optDouble("openAngle", 0);
			startOpenTimeBeforeFiring = o.getInt("startOpenTimeBeforeFiring");
			endOpenTimeBeforeFiring = o.getInt("endOpenTimeBeforeFiring");
			startCloseTimeAfterFiring = o.getInt("startCloseTimeAfterFiring");
			endCloseTimeAfterFiring = o.getInt("endCloseTimeAfterFiring");
			shellClosedPenAbsorb = o.optInt("shellClosedPenAbsorb", 0);
			shellClosedBlastAbsorb = o.optInt("shellClosedBlastAbsorb", 0);
			if (o.has("openEmitters")) {
				JSONArray a = o.getJSONArray("openEmitters");
				for (int i = 0; i < a.length(); i++) {
					openEmitters.add(new ShellEmitter(a.getJSONObject(i)));
				}
			}
			if (o.has("closeEmitters")) {
				JSONArray a = o.getJSONArray("closeEmitters");
				for (int i = 0; i < a.length(); i++) {
					closeEmitters.add(new ShellEmitter(a.getJSONObject(i)));
				}
			}
			if (o.has("openSound")) {
				openSound = new SoundEffect(o.getJSONObject("openSound"));
			}
			if (o.has("closeSound")) {
				closeSound = new SoundEffect(o.getJSONObject("closeSound"));
			}
		}
		
		public Shell flipped(int moduleW) {
			Shell s2 = new Shell(img.flip(), internalImg == null ? null : internalImg.flip(),
					moduleW * AGame.SGS - x - img.srcWidth,
					y,
					img.srcWidth - pivotX,
					pivotY,
					-openAngle,
					-openShiftX, openShiftY,
					startOpenTimeBeforeFiring, endOpenTimeBeforeFiring, startCloseTimeAfterFiring, endCloseTimeAfterFiring,
					shellClosedPenAbsorb, shellClosedBlastAbsorb);
			for (int i = 0; i < openEmitters.size(); i++) {
				s2.openEmitters.add(openEmitters.get(i).flipped(moduleW));
			}
			for (int i = 0; i < closeEmitters.size(); i++) {
				s2.closeEmitters.add(closeEmitters.get(i).flipped(moduleW));
			}
			s2.openSound = openSound;
			s2.closeSound = closeSound;
			return s2;
		}
		
		public double getOpenness(Module m) {
			double o = 0;
			if (m.msSinceFired < startCloseTimeAfterFiring) {
				o = 1;
			} else if (m.msSinceFired < endCloseTimeAfterFiring) {
				o = (1 - (m.msSinceFired - startCloseTimeAfterFiring) * 1.0 / (endCloseTimeAfterFiring - startCloseTimeAfterFiring));
			}
			// qqDPS Fire mode, etc.
			int shootAccum = m.shootAccumulator;
			int reload = m.type.getReload(m.ship.currentBonuses);
			if (shootAccum > reload - endOpenTimeBeforeFiring) {
				o = 1;
			} else if (shootAccum > reload - startOpenTimeBeforeFiring) {
				o = (shootAccum - (reload - startOpenTimeBeforeFiring)) * 1.0 / (startOpenTimeBeforeFiring - endOpenTimeBeforeFiring);
			}
			return o;
		}
	}
	
	public static strictfp class BarrelAnimation {
		public final int interval;
		public final int msPerShot;
		public final int totalLength;
		public final boolean loop;
		public final boolean loopConstantly;
		public final boolean finishLoopCycle;
		public final ArrayList<Img> frames;

		public BarrelAnimation(int interval, int msPerShot, boolean loop, boolean loopConstantly, boolean finishLoopCycle, ArrayList<Img> frames) {
			this.interval = interval;
			this.loop = loop;
			this.loopConstantly = loopConstantly;
			this.finishLoopCycle = finishLoopCycle;
			this.frames = frames;
			this.totalLength = interval * frames.size();
			this.msPerShot = msPerShot == -1 ? totalLength : msPerShot;
		}

		public BarrelAnimation flip() {
			ArrayList<Img> flippedFrames = new ArrayList<Img>();
			for (Img f : frames) {
				flippedFrames.add(f.flip());
			}
			return new BarrelAnimation(interval, msPerShot, loop, loopConstantly, finishLoopCycle, flippedFrames);
		}
	}
	
	public static strictfp class FromJSON implements BonusableValue.FromJSON<WeaponAppearance> {
		private final int w;

		public FromJSON(int w) {
			this.w = w;
		}

		@Override
		public WeaponAppearance construct(JSONObject o, BonusSet b) {
			return new WeaponAppearance(o, w);
		}
	}
	
	public WeaponAppearance(JSONObject o, int moduleW) {
		if (o.has("back")) {
			back = new Appearance(o.getJSONObject("back"));
		}
		if (o.has("shot")) {
			shot = new Img[] { img(o.getJSONObject("shot")) };
		}
		if (o.has("shotFrames")) {
			JSONArray a = o.getJSONArray("shotFrames");
			shot = new Img[a.length()];
			for (int i = 0; i < a.length(); i++) {
				shot[i] = img(a.getJSONObject(i));
			}
		}
		onlyShowBarrelIfLoaded = o.optBoolean("onlyShowBarrelIfLoaded", false);
		ignoreBarrelRotation = o.optBoolean("ignoreBarrelRotation", false);
		shotAnimationInterval = o.optInt("shotAnimationInterval", 150);
		hitExplosionSize = o.optDouble("hitExplosionSize", 0);
		missExplosionSize = o.optDouble("missExplosionSize", 0);
		fireVibrate = o.optDouble("fireVibrate", 0);
		fireFlash = o.optDouble("fireFlash", 0);
		fireVibrateDecay = o.optDouble("fireVibrateDecay", 0.8);
		if (o.has("barrelLoadStages")) {
			JSONArray a = o.getJSONArray("barrelLoadStages");
			barrelLoadStages = new Img[a.length()];
			flippedBarrelLoadStages = new Img[a.length()];
			for (int i = 0; i < a.length(); i++) {
				barrelLoadStages[i] = img(a.getJSONObject(i));
				flippedBarrelLoadStages[i] = barrelLoadStages[i].flip();
			}
		}
		if (o.has("barrel")) {
			barrel = img(o.getJSONObject("barrel"));
			flippedbarrel = barrel.flip();
		} else if (o.has("barrelAnimation")) {
			JSONObject bao = o.getJSONObject("barrelAnimation");
			JSONArray framesA = bao.getJSONArray("frames");
			ArrayList<Img> frames = new ArrayList<Img>();
			for (int i = 0; i < framesA.length(); i++) {
				frames.add(img(framesA.getJSONObject(i)));
			}
			barrelAnimation = new BarrelAnimation(
					bao.getInt("interval"),
					bao.optInt("msPerShot", -1),
					bao.optBoolean("loop", false),
					bao.optBoolean("loopConstantly", false),
					bao.optBoolean("finishLoopCycle", false),
					frames);
			flippedBarrelAnimation = barrelAnimation.flip();
		}
		
		if (o.has("externalBarrel")) {
			externalBarrel = img(o.getJSONObject("externalBarrel"));
			externalFlippedbarrel = externalBarrel.flip();
		} else if (o.has("externalBarrelAnimation")) {
			JSONObject bao = o.getJSONObject("externalBarrelAnimation");
			JSONArray framesA = bao.getJSONArray("frames");
			ArrayList<Img> frames = new ArrayList<Img>();
			for (int i = 0; i < framesA.length(); i++) {
				frames.add(img(framesA.getJSONObject(i)));
			}
			externalBarrelAnimation = new BarrelAnimation(
					bao.getInt("interval"),
					bao.optInt("msPerShot", -1),
					bao.optBoolean("loop", false),
					bao.optBoolean("loopConstantly", false),
					bao.optBoolean("finishLoopCycle", false),
					frames);
			externalFlippedBarrelAnimation = externalBarrelAnimation.flip();
		}
		
		if (o.has("barrelX")) {
			barrelOffset = new Pt(o.getDouble("barrelX"), o.getDouble("barrelY"));
			Img b = barrel != null ? barrel : barrelAnimation.frames.get(0);
			flippedBarrelOffset = new Pt(moduleW * AGame.SGS - barrelOffset.x - b.srcWidth, barrelOffset.y);
		}
		recoil = o.optDouble("recoil", 0);
		if (o.has("shotEmitter")) {
			JSONObject em = o.getJSONObject("shotEmitter");
			SoundEffect ef = null;
			if (em.has("sound")) {
				try {
					String sound = em.getString("sound");
					ef = new SoundEffect(sound, em.optDouble("volume"));
				} catch (Exception e) {
					ef = new SoundEffect(em.getJSONObject("sound"));
				}
			}
			shotEmitter = new Particle.Emitter(
					ParticleType.ofName(em.getString("type")),
					em.getDouble("emitProbability"),
					em.optInt("numParticles", 1),
					ef);
		}
		if (o.has("exhaust")) {
			JSONObject e = o.getJSONObject("exhaust");
			exhaust = new ShotExhaustEmitter(
					ParticleType.ofName(e.getString("type")),
					e.getDouble("p"),
					e.getDouble("backOffset"),
					e.getDouble("angleRange"),
					e.getDouble("randomOffset"),
					e.getDouble("speedMin"),
					e.getDouble("speedMax")
			);
		}
		impactParticle = ParticleType.ofName(o.optString("impactParticle", "impact"));
		numImpactParticles = o.optInt("numImpactParticles", 0);
		if (o.has("shells")) {
			JSONArray a = o.getJSONArray("shells");
			for (int i = 0; i < a.length(); i++) {
				shells.add(new Shell(a.getJSONObject(i)));
			}
		}
		useSubColorForBarrel = o.optBoolean("useSubColorForBarrel", true);
	}
	
	public WeaponAppearance exhaust(ShotExhaustEmitter exhaust) {
		this.exhaust = exhaust;
		return this;
	}
	
	public WeaponAppearance back(Appearance back) {
		this.back = back;
		return this;
	}
	
	public WeaponAppearance barrel(Img barrel, Pt barrelOffset, int w) {
		this.barrel = barrel;
		this.barrelOffset = barrelOffset;
		this.flippedbarrel = barrel.flip();
		this.flippedBarrelOffset = new Pt(w * AGame.SGS - barrelOffset.x - barrel.srcWidth, barrelOffset.y);
		return this;
	}
	
	public WeaponAppearance recoil(double recoil) {
		this.recoil = recoil;
		return this;
	}
	
	public WeaponAppearance shotEmitter(Particle.Emitter shotEmitter) {
		this.shotEmitter = shotEmitter;
		return this;
	}
	
	private WeaponAppearance() {}

	public WeaponAppearance flipped(int w) {
		WeaponAppearance wa2 = new WeaponAppearance();
		wa2.shotAnimationInterval = shotAnimationInterval;
		wa2.hitExplosionSize = hitExplosionSize;
		wa2.missExplosionSize = missExplosionSize;
		wa2.back = back == null ? null : back.flip();
		wa2.onlyShowBarrelIfLoaded = onlyShowBarrelIfLoaded;
		wa2.fireVibrate = fireVibrate;
		wa2.fireFlash = fireFlash;
		wa2.fireVibrateDecay = fireVibrateDecay;
		wa2.barrelLoadStages = flippedBarrelLoadStages;
		wa2.flippedBarrelLoadStages = barrelLoadStages;
		if (barrel != null) {
			wa2.barrel = flippedbarrel;
			wa2.flippedbarrel = barrel;
			wa2.barrelOffset = flippedBarrelOffset;
			wa2.flippedBarrelOffset = barrelOffset;
		}
		if (barrelAnimation != null) {
			wa2.barrelAnimation = flippedBarrelAnimation;
			wa2.flippedBarrelAnimation = barrelAnimation;
			wa2.barrelOffset = flippedBarrelOffset;
			wa2.flippedBarrelOffset = barrelOffset;
		}
		
		if (externalBarrel != null) {
			wa2.externalBarrel = externalFlippedbarrel;
			wa2.externalFlippedbarrel = externalBarrel;
			wa2.barrelOffset = flippedBarrelOffset;
			wa2.flippedBarrelOffset = barrelOffset;
		}
		if (externalBarrelAnimation != null) {
			wa2.externalBarrelAnimation = externalFlippedBarrelAnimation;
			wa2.externalFlippedBarrelAnimation = externalBarrelAnimation;
			wa2.barrelOffset = flippedBarrelOffset;
			wa2.flippedBarrelOffset = barrelOffset;
		}
		
		wa2.recoil = recoil;
		wa2.shot = shot;
		wa2.shotEmitter = shotEmitter;
		wa2.exhaust = exhaust;
		wa2.impactParticle = impactParticle;
		wa2.numImpactParticles = numImpactParticles;
		for (Shell s : shells) {
			wa2.shells.add(s.flipped(w));
		}
		wa2.useSubColorForBarrel = useSubColorForBarrel;
		return wa2;
	}
	
	public /* nostrictfp */ static strictfp class ShotExhaustEmitter {
		public final ParticleType type;
		public final double p;
		public final double backOffset;
		public final double angleRange;
		public final double randomOffset;
		public final double speedMin, speedMax;

		public ShotExhaustEmitter(ParticleType type, double p, double backOffset, double angleRange, double randomOffset, double speedMin, double speedMax) {
			this.type = type;
			this.p = p;
			this.backOffset = backOffset;
			this.angleRange = angleRange;
			this.randomOffset = randomOffset;
			this.speedMin = speedMin;
			this.speedMax = speedMax;
		}
		
		public void emit(Shot shot, int ms, LinkedList<Particle> particles) {
			if (p < 1 && AGame.ANIM_R.nextDouble() > ms * p) { return; }
			int nParticles = (int) StrictMath.ceil(ms * p);
			boolean isGuided = shot.isGuided();

			double shotAngle = isGuided ? shot.a : StrictMath.atan2((shot.tY - shot.sY), (shot.tX - shot.sX));
			double baseXOffset = StrictMath.cos(shotAngle) * -backOffset;
			double baseYOffset = StrictMath.sin(shotAngle) * -backOffset;
			
			for (int i = 0; i < nParticles; i++) {
				int shotTime = shot.time - i * ms / nParticles;
				double shotX = isGuided ? shot.sX : (shot.sX + ((shot.tX - shot.sX) * shotTime / shot.travelTime));
				double shotY = isGuided ? shot.sY : (shot.sY + ((shot.tY - shot.sY) * shotTime / shot.travelTime));
				if (shot.isArcing) {
					shotX = shot.getX() - i * ms * shot.arcingDx / nParticles;
					shotY = shot.getY() - i * ms * shot.arcingDy / nParticles;
				}
				double xOffset = baseXOffset + (AGame.ANIM_R.nextDouble() - 0.5) * randomOffset;
				double yOffset = baseYOffset + (AGame.ANIM_R.nextDouble() - 0.5) * randomOffset;
				double angle = shotAngle + (AGame.ANIM_R.nextDouble() - 0.5) * angleRange;
				double speed = speedMin + AGame.ANIM_R.nextDouble() * (speedMax - speedMin);
				double dx = StrictMath.cos(angle) * -speed;
				double dy = StrictMath.sin(angle) * -speed;
				if (isGuided) {
					dx += shot.dX;
					dy += shot.dY;
				}
				Particle particle = new Particle(type, shotX + xOffset, shotY + yOffset, dx, dy);
				particle.life -= i * ms / nParticles;
				particles.add(particle);
			}
		}
	}
}
