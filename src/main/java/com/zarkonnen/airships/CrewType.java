package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class  CrewType extends Loadable {	
	public final boolean doesWork;
	public final boolean doesGuard;
	public final boolean canBoard;
	public final int commandPointsRequired;

	public final boolean hasHook;
	public final double supplyCost;
	public final double crewEffectiveness;
	public final int maxHP;
	public final int minWorkingHP;
	public final int penDmg, blastDmg, directDmg;
	public final int blastSplashRadius;
	public final int penSplashRadius;
	public final int directSplashRadius;
	public final int numShots;
	public final Img shot;
	public final Particle.Emitter shotEmitter;
	public final WeaponAppearance.ShotExhaustEmitter exhaust;
	public final BonusableValue<Integer> weaponReload;
	public final int aimTime;
	public final boolean meleeAttack;
	public final double reloadSlowdown;
	public final double insideSpeed;
	public final int pickupMs;
	public final double pickupDmgMaxMalus;
	public final double pickupFireMult;
	public final double fireSpeedMult;
	public final double dmgWorstSpeedMalus;
	public final double goingUpSpeedMult;
	public final double carrySpeedMult;
	public final int repairTime;
	public final Img simpleLook;
	public final Img simpleLookSpider;
	public final AnimationAppearance animLooks[];
	
	public final double sickbayHealPMs;
	public final double fireHarmPMs;
	public final double hookSpeed;
	public final double winchSpeed;
	public final int hookRopeLength;
	public final double jumpStrength;
	public final double outsideSpeed;
	public final int assumedJumpDist;
	public final double hookRopeWidth;
	
	public Shouts shouts;
	
	public final Img hookImg;
	
	public final SoundEffect deathSnd;
	public final SoundEffect coughSnd;
	public final SoundEffect hookLaunchSnd;
	public final SoundEffect hookHitSnd;
	public final SoundEffect attackSnd;
	
	public final ParticleType bloodParticle;
	public final ParticleType bloodParticleExternal;
	public final ParticleType attackParticle;
	public final double explosionSize;
	public final double missExplosionSize, hitExplosionSize;
	
	public final int barrelX, barrelY;
	public final double shotSpeed, inaccuracy, maxRange, minRange, baseInaccuracy;
	
	public final int popOutDelayMin;
	public final int popOutDelayRange;
	
	public final boolean isMachine;
	
	public final boolean canWalk;
	public final boolean canFly;
	public final boolean crashesOnDeath;
	public final double
			airXTopSpeed, airUpTopSpeed, airDownTopSpeed,
			airXMinSpeed,
			airXAcceleration, airUpAcceleration, airDownAcceleration,
			launchMinXSpeed, launchMaxXSpeed, launchMinYSpeed, launchMaxYSpeed,
			airOvershoot;
	public final int launchLength;
	public final SoundEffect launchSnd;
	
	public final boolean shootsShips;
	public final int shootTroopsRange;
	public final boolean interceptTroops;
	public final boolean bombs;
	public final boolean aimForCenter;
	public final int ammoCapacity;
	public final boolean dieOnEmptyAmmo;
	public final int rearmTime;
	public final int guardRange;
	public final int shipWeaponsDodge;

	public final int strafeOvershoot;
	public final int returnToRepairHP, msPerHPRepaired;
	
	public String spawnCrewOnImpact;
	public int spawnNumCrewOnImpact;
	public boolean spawnCrewInsideIfArmourPierced;
	public boolean alwaysSpawnCrewInside;
	public boolean spawnCrewOnMiss;
	public boolean spawnCrewOnKillBiologicalOnly;
	
	public final int sniperChancePercent;
	public final double setFireMultiplier;
	
	public final int underwaterGraceTime, drowningTime;
	
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append(":\n");
		boolean hasProperty = false;
		if (canFly) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_flying"));
		}
		if (canBoard) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_boarder"));
		}
		if (doesWork) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_worker"));
		}
		if (doesGuard) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_guard"));
		}
		if (hasHook) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_grappling_hook"));
		}
		if (meleeAttack) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_melee_attack"));
		}
		if (interceptTroops) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_interceptor"));
		}
		if (bombs) {
			if (hasProperty) { sb.append(", "); } hasProperty = true;
			sb.append(_t("crew_bomber"));
		}
		sb.append("\n").append(_t("HP_x", maxHP));
		if (canWalk && !canFly && insideSpeed < 1) {
			sb.append("\n").append(_t("techspeedsetting_SLOW"));
		}
		if (canWalk && !canFly && insideSpeed > 1) {
			sb.append("\n").append(_t("techspeedsetting_FAST"));
		}
		if (canFly) {
			sb.append("\n").append(_t("Speed_")).append((int) (airXTopSpeed * 3600 / AGame.PX_TO_M)).append(_t("kmperhour"));
			int accelSeconds = (int) (((airXTopSpeed - airXMinSpeed) / airXAcceleration) / 1000);
			if (accelSeconds > 0) {
				sb.append("\n").append(_t("crew_accel_time")).append(_t("x_seconds", accelSeconds));
			}
		}
		if (blastDmg != 0) {
			if (numShots > 1) {
				sb.append("\n").append(_t("Blast_damage_x", numShots + "x" + blastDmg));
			} else {
				sb.append("\n").append(_t("Blast_damage_x", blastDmg));
			}
			if (blastSplashRadius != 0) {
				sb.append("\n").append(_t("Splash_distance_x", _t("x_metres", blastSplashRadius / AGame.PX_TO_M)));
			}
		}
		if (penDmg != 0) {
			if (numShots > 1) {
				sb.append("\n").append(_t("Penetration_damage_x", numShots + "x" + penDmg));
			} else {
				sb.append("\n").append(_t("Penetration_damage_x", penDmg));
			}
			if (penSplashRadius != 0) {
				sb.append("\n").append(_t("Splash_distance_x", _t("x_metres", penSplashRadius / AGame.PX_TO_M)));
			}
		}
		if (directDmg != 0) {
			if (numShots > 1) {
				sb.append("\n").append(_t("Direct_damage_x", numShots + "x" + directDmg));
			} else {
				sb.append("\n").append(_t("Direct_damage_x", directDmg));
			}
			if (directSplashRadius != 0) {
				sb.append("\n").append(_t("Splash_distance_x", _t("x_metres", directSplashRadius / AGame.PX_TO_M)));
			}
		}
		if (sniperChancePercent > 0) {
			sb.append("\n").append(_t("x_sniper_chance", sniperChancePercent));
		}
		if (setFireMultiplier != 1) {
			sb.append("\n").append(_t("x_set_fire_multiplier", setFireMultiplier));
		}
		if (blastDmg != 0 || penDmg != 0 || directDmg != 0) {
			if (ammoCapacity > 1) {
				int reload = weaponReload.get(BonusSet.empty());
				if (reload <= 500) {
					sb.append("\n").append(_t("Rate_of_fire_x_per_second", _t("x_rounds_per_second", 1000 / reload)));
				} else {
					sb.append("\n").append(_t("Reload_time_x_seconds", FormatUtils.SECONDS_PRECISE.format(reload)));
				}
			}
			if (ammoCapacity != 0) {
				sb.append("\n").append(_t("Clip_size_x_rounds", ammoCapacity));
			}
			if (canFly && !meleeAttack && shootsShips) {
				sb.append("\n").append(_t("Maximum_range_x_metres", _t("x_metres", (int) maxRange / AGame.PX_TO_M)));
			}
		}
		if (spawnCrewOnImpact != null) {
			if (spawnNumCrewOnImpact == 1) {
				sb.append("\n").append(_t("weapon_spawn_crew_single", CrewType.ofName(spawnCrewOnImpact).getName()));
			} else {
				sb.append("\n").append(_t("weapon_spawn_crew", spawnNumCrewOnImpact, CrewType.ofName(spawnCrewOnImpact).getPlural()));
			}
		}
				
		return sb.toString();
	}
	
	public int boardingCombatStrength() {
		return 3 + (int) (1000 * (maxHP - minWorkingHP) * (penDmg + blastDmg + directDmg) * StrictMath.min(1.25, insideSpeed) / weaponReload.get(BonusSet.empty()) * (meleeAttack ? 0.5 : 1));
	}
	
	public boolean isAircraft() {
		return canFly && !canWalk && isMachine;
	}
		
	public static enum RecolorReplacement {
		COA_1(0), COA_2(1), COA_3(2), COA_COLOUR(3), COA_METAL(4);
		public final int armsColorIndex;

		private RecolorReplacement(int armsColorIndex) {
			this.armsColorIndex = armsColorIndex;
		}
	}
	
	public final float[] recolorOriginalA, recolorOriginalB;
	public final RecolorReplacement recolorReplacementA, recolorReplacementB;
	
	public int totalDamage() {
		return penDmg + blastDmg + directDmg;
	}
	
	public static final float[] NO_COLOUR = { 1, 0, 1 };

	public CrewType(JSONObject o) {		
		super(o.getString("name"));
		
		this.doesWork = o.optBoolean("doesWork", false);
		this.doesGuard = o.optBoolean("doesGuard", false);
		this.canBoard = o.optBoolean("canBoard", false);
		
		commandPointsRequired = o.optInt("commandPointsRequired", 50);
		
		this.crewEffectiveness = o.optDouble("crewEffectiveness", 1);
		this.hasHook = o.optBoolean("hasHook", false);
		this.supplyCost = o.optDouble("supplyCost", 0);
		if (o.has("animLook")) {
			this.animLooks = new AnimationAppearance[] { AnimationAppearance.ofName(o.getString("animLook")) };
		} else {
			JSONArray lx = o.getJSONArray("animLooks");
			this.animLooks = new AnimationAppearance[lx.length()];
			for (int i = 0; i < animLooks.length; i++) {
				this.animLooks[i] = AnimationAppearance.ofName(lx.getString(i));
			}
		}
		if (o.has("simpleLookImg")) {
			this.simpleLook = new Img(
					o.getJSONObject("simpleLookImg").getString("src"),
					o.getJSONObject("simpleLookImg").getInt("x"),
					o.getJSONObject("simpleLookImg").getInt("y"),
					o.getJSONObject("simpleLookImg").getInt("w"),
					o.getJSONObject("simpleLookImg").getInt("h"),
					o.getJSONObject("simpleLookImg").optBoolean("flipped", false)
			);
		} else {
			this.simpleLook = new Img(
					o.getJSONObject("simpleLook").getString("src"),
					o.getJSONObject("simpleLook").getInt("x") * AGame.SGS,
					o.getJSONObject("simpleLook").getInt("y") * AGame.SGS,
					16,
					16,
					false
			);
		}
		if (o.has("simpleLookSpider")) {
			this.simpleLookSpider = new Img(
					o.getJSONObject("simpleLookSpider").getString("src"),
					o.getJSONObject("simpleLookSpider").getInt("x") * AGame.SGS,
					o.getJSONObject("simpleLookSpider").getInt("y") * AGame.SGS,
					16,
					16,
					false
			);
		} else {
			this.simpleLookSpider = null;
		}
		this.maxHP = o.getInt("maxHP");
		this.minWorkingHP = o.getInt("minWorkingHP");
		
		isMachine = o.optBoolean("isMachine", false);
		double ceDiv = Math.max(0.25, crewEffectiveness);
		pickupMs = (int) (300 / ceDiv);
		insideSpeed = o.optDouble("insideSpeed", 1.0);
		reloadSlowdown = o.optDouble("reloadSlowdown", 0.5);
		pickupDmgMaxMalus = 3;
		pickupFireMult = 1 + 0.5 / ceDiv;
		fireSpeedMult = 1 - 0.3 / ceDiv;
		dmgWorstSpeedMalus = -0.3;
		goingUpSpeedMult = o.optDouble("goingUpSpeedMult", 1);
		carrySpeedMult = 1 - 0.2 / ceDiv;
		repairTime = (int) (4000 / ceDiv);
		if (o.has("shotDamageMin")) {
			penDmg = o.getInt("shotDamageMin") + o.optInt("shotDamageRange", 2) / 2;
			blastDmg = 0;
			directDmg = 0;
		} else {
			penDmg = o.optInt("penDmg", 0);
			blastDmg = o.optInt("blastDmg", 0);
			directDmg = o.optInt("directDmg", 0);
		}
		blastSplashRadius = o.optInt("blastSplashRadius", 0);
		penSplashRadius = o.optInt("penSplashRadius", 0);
		directSplashRadius = o.optInt("directSplashRadius", 0);
		numShots = o.optInt("numShots", 1);
		weaponReload = BonusableValue.intFromJSONWithDivAndMinAndMax(o, "weaponReload", 1000, 1, 1, 100000);
		meleeAttack = o.optBoolean("meleeAttack", false);
		
		sickbayHealPMs = o.optDouble("sickbayHealPMs", 0.00003);
		fireHarmPMs = o.optDouble("fireHarmPMs", 0.00005);
		hookSpeed = o.optDouble("hookSpeed", 0.5);
		winchSpeed = o.optDouble("winchSpeed", 0.1);
		hookRopeLength = o.optInt("hookRopeLength", 260);
		jumpStrength = o.optDouble("jumpStrength", 0.15);
		outsideSpeed = o.optDouble("outsideSpeed", 0.035);
		assumedJumpDist = o.optInt("assumedJumpDist", 50);
		
		popOutDelayMin = o.optInt("popOutDelayMin", 0);
		popOutDelayRange = o.optInt("popOutDelayRange", 0);
		
		aimTime = o.optInt("aimTime", 500);
		
		explosionSize = o.optDouble("explosionSize", 0);
		
		shouts = o.has("shouts") ? Shouts.ofName(o.getString("shouts")) : null;
		
		underwaterGraceTime = o.optInt("underwaterGraceTime", isMachine ? 0 : 4000);
		drowningTime = o.optInt("drowningTime", isMachine ? 0 : 8000);
		
		if (o.has("hookImg")) {
			JSONObject hi = o.getJSONObject("hookImg");
			hookImg = new Img(hi.getString("src"), hi.getInt("x"), hi.getInt("y"), hi.getInt("w"), hi.getInt("h"), hi.optBoolean("flipped", false));
		} else {
			hookImg = null;
		}
		
		hookRopeWidth = o.optDouble("hookRopeWidth", 1);
		
		if (o.has("deathSnd")) {
			SoundEffect se = null;
			try {
				se = new SoundEffect(o.getString("deathSnd"), o.optInt("numDeathSnds", 1));
			} catch (Exception e) {
				se = new SoundEffect(o.getJSONObject("deathSnd"));
			}
			deathSnd = se;
		} else {
			deathSnd = null;
		}
		if (o.has("coughSnd")) {
			SoundEffect se = null;
			try {
				se = new SoundEffect(o.getString("coughSnd"), o.optInt("numCoughSnds", 1));
			} catch (Exception e) {
				se = new SoundEffect(o.getJSONObject("coughSnd"));
			}
			coughSnd = se;
		} else {
			coughSnd = null;
		}
		if (o.has("hookLaunchSnd")) {
			SoundEffect se = null;
			try {
				se = new SoundEffect(o.getString("hookLaunchSnd"), o.optInt("numHookLaunchSnds", 1));
			} catch (Exception e) {
				se = new SoundEffect(o.getJSONObject("hookLaunchSnd"));
			}
			hookLaunchSnd = se;
		} else {
			hookLaunchSnd = null;
		}
		if (o.has("hookHitSnd")) {
			SoundEffect se = null;
			try {
				se = new SoundEffect(o.getString("hookHitSnd"), o.optInt("numHookHitSnds", 1));
			} catch (Exception e) {
				se = new SoundEffect(o.getJSONObject("hookHitSnd"));
			}
			hookHitSnd = se;
		} else {
			hookHitSnd = null;
		}
		if (o.has("attackSnd")) {
			SoundEffect se = null;
			try {
				se = new SoundEffect(o.getString("attackSnd"), o.optInt("numAttackSnds", 1));
			} catch (Exception e) {
				se = new SoundEffect(o.getJSONObject("attackSnd"));
			}
			attackSnd = se;
		} else {
			attackSnd = null;
		}
		
		bloodParticle = o.has("bloodParticle")
				? ParticleType.ofName(o.getString("bloodParticle"))
				: null;
		
		bloodParticleExternal = o.has("bloodParticleExternal")
				? ParticleType.ofName(o.getString("bloodParticleExternal"))
				: null;
		
		attackParticle = o.has("attackParticle")
				? ParticleType.ofName(o.getString("attackParticle"))
				: null;
		
		shootsShips = o.optBoolean("shootsShips", false);
		strafeOvershoot = o.optInt("strafeOvershoot", AGame.SGS * 8);
		
		if (o.has("recolorOriginalA")) {
			recolorOriginalA = new float[] {
				o.getJSONObject("recolorOriginalA").getInt("r") / 255f,
				o.getJSONObject("recolorOriginalA").getInt("g") / 255f,
				o.getJSONObject("recolorOriginalA").getInt("b") / 255f
			};
			recolorReplacementA = RecolorReplacement.valueOf(o.getString("recolorReplacementA"));
		} else {
			recolorOriginalA = NO_COLOUR;
			recolorReplacementA = RecolorReplacement.COA_1;
		}
		
		if (o.has("recolorOriginalB")) {
			recolorOriginalB = new float[] {
				o.getJSONObject("recolorOriginalB").getInt("r") / 255f,
				o.getJSONObject("recolorOriginalB").getInt("g") / 255f,
				o.getJSONObject("recolorOriginalB").getInt("b") / 255f
			};
			recolorReplacementB = RecolorReplacement.valueOf(o.getString("recolorReplacementB"));
		} else {
			recolorOriginalB = NO_COLOUR;
			recolorReplacementB = RecolorReplacement.COA_1;
		}
		
		if (o.has("shot")) {
			JSONObject so = o.getJSONObject("shot");
			shot = new Img(so.getString("src"), so.getInt("x"), so.getInt("y"), so.getInt("w"), so.getInt("h"), so.optBoolean("flipped"));
		} else {
			shot = null;
		}
		
		inaccuracy = o.optDouble("inaccuracy", 0.005);
		baseInaccuracy = o.optDouble("baseInaccuracy", 0);
		maxRange = o.optDouble("maxRange", 500);
		minRange = o.optDouble("minRange", 16);
		barrelX = o.optInt("barrelX", 18);
		barrelY = o.optInt("barrelY", 4);
		shotSpeed = o.optDouble("shotSpeed", 0.7);
		missExplosionSize = o.optDouble("missExplosionSize", 0);
		hitExplosionSize = o.optDouble("hitExplosionSize", 0);
		
		returnToRepairHP = o.optInt("returnToRepairHP", 0);
		msPerHPRepaired = o.optInt("msPerHPRepaired", 0);
		
		shootTroopsRange = o.optInt("shootTroopsRange", 0);
		interceptTroops = o.optBoolean("interceptTroops", false);
		
		canWalk = o.optBoolean("canWalk", true);
		canFly = o.optBoolean("canFly", false);
		
		shipWeaponsDodge = o.optInt("shipWeaponsDodge", canFly ? 10 : 0);
		
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
		} else {
			shotEmitter = null;
		}
		
		if (o.has("exhaust")) {
			JSONObject e = o.getJSONObject("exhaust");
			exhaust = new WeaponAppearance.ShotExhaustEmitter(
					ParticleType.ofName(e.getString("type")),
					e.getDouble("p"),
					e.getDouble("backOffset"),
					e.getDouble("angleRange"),
					e.getDouble("randomOffset"),
					e.getDouble("speedMin"),
					e.getDouble("speedMax")
			);
		} else {
			exhaust = null;
		}
		
		spawnCrewOnImpact = o.optString("spawnCrewOnImpact", null);
		spawnNumCrewOnImpact = o.optInt("spawnNumCrewOnImpact", 1);
		spawnCrewInsideIfArmourPierced = o.optBoolean("spawnCrewInsideIfArmourPierced", true);
		alwaysSpawnCrewInside = o.optBoolean("alwaysSpawnCrewInside", false);
		spawnCrewOnMiss = o.optBoolean("spawnCrewOnMiss", false);
		spawnCrewOnKillBiologicalOnly = o.optBoolean("spawnCrewOnKillBiologicalOnly", false);
		
		sniperChancePercent = o.optInt("sniperChancePercent", 0);
		setFireMultiplier = o.optDouble("setFireMultiplier", 1);
		
		if (canFly) {
			crashesOnDeath = o.optBoolean("crashesOnDeath", false);
			bombs = o.optBoolean("bombs", false);
			airXTopSpeed = o.getDouble("airXTopSpeed");
			airUpTopSpeed = o.getDouble("airUpTopSpeed");
			airDownTopSpeed = o.getDouble("airDownTopSpeed");
			airXAcceleration = o.getDouble("airXAcceleration");
			airUpAcceleration = o.getDouble("airUpAcceleration");
			airDownAcceleration = o.getDouble("airDownAcceleration");
			launchMinXSpeed = o.getDouble("launchMinXSpeed");
			launchMaxXSpeed = o.getDouble("launchMaxXSpeed");
			launchMinYSpeed = o.getDouble("launchMinYSpeed");
			launchMaxYSpeed = o.getDouble("launchMaxYSpeed");
			airOvershoot = o.getDouble("airOvershoot");
			launchLength = o.optInt("launchLength", 0);
			airXMinSpeed = o.optDouble("airXMinSpeed", 0);
			aimForCenter = o.optBoolean("aimForCenter");
			ammoCapacity = o.optInt("ammoCapacity", 0);
			dieOnEmptyAmmo = o.optBoolean("dieOnEmptyAmmo", false);
			rearmTime = o.optInt("rearmTime", 0);
			guardRange = o.optInt("guardRange", 40 * AGame.SGS);
			if (o.has("launchSnd")) {
				SoundEffect se = null;
				try {
					se = new SoundEffect(o.getString("launchSnd"), o.optInt("numLaunchSnds", 1));
				} catch (Exception e) {
					se = new SoundEffect(o.getJSONObject("launchSnd"));
				}
				launchSnd = se;
			} else {
				launchSnd = null;
			}
		} else {
			crashesOnDeath = false;
			airXTopSpeed = 0;
			airUpTopSpeed = 0;
			airDownTopSpeed = 0;
			airXAcceleration = 0;
			airUpAcceleration = 0;
			airDownAcceleration = 0;
			launchMinXSpeed = 0;
			launchMaxXSpeed = 0;
			launchMinYSpeed = 0;
			launchMaxYSpeed = 0;
			airOvershoot = 0;
			launchSnd = null;
			launchLength = 0;
			airXMinSpeed = 0;
			bombs = false;
			aimForCenter = false;
			ammoCapacity = 0;
			dieOnEmptyAmmo = false;
			rearmTime = 0;
			guardRange = 0;
		}
	}
	
	public String getName() {
		return _t("crew_" + name.toUpperCase(Locale.ENGLISH));
	}
	
	public String getPlural() {
		return _t("crew_" + name.toUpperCase(Locale.ENGLISH) + "_plural");
	}
	
	public static CrewType ofName(String name) {
		return ofName(CrewType.class, name);
	}
	
	public static void postLoad() {		
		onlyWorkers.clear();
		allWorkersByAbility.clear();
		allGuardsByAbility.clear();
		nonBoarderGuardsByAbility.clear();
		nonWorkerGuards.clear();
		jumpers.clear();
		grapplers.clear();
		boarders.clear();
		
		for (CrewType ct : all(CrewType.class)) {
			if (ct.doesWork && !ct.canBoard && !ct.doesGuard) {
				onlyWorkers.add(ct);
			}
			if (ct.doesWork) {
				allWorkersByAbility.add(ct);
			}
			if (ct.doesGuard) {
				allGuardsByAbility.add(ct);
			}
			if (ct.doesGuard && !ct.canBoard) {
				nonBoarderGuardsByAbility.add(ct);
			}
			if (ct.doesGuard && !ct.doesWork) {
				nonWorkerGuards.add(ct);
			}
			if (ct.canBoard && !ct.hasHook) {
				jumpers.add(ct);
			}
			if (ct.canBoard && ct.hasHook) {
				grapplers.add(ct);
			}
			if (ct.canBoard) {
				boarders.add(ct);
			}
		}
		
		Collections.sort(allWorkersByAbility, new Comparator<CrewType>() {
			@Override
			public int compare(CrewType t, CrewType t1) {
				return Double.compare(t1.crewEffectiveness, t.crewEffectiveness);
			}
		});
		
		Collections.sort(allGuardsByAbility, new Comparator<CrewType>() {
			@Override
			public int compare(CrewType t, CrewType t1) {
				if (t.doesWork && !t1.doesWork) {
					return 1;
				}
				if (!t.doesWork && t1.doesWork) {
					return -1;
				}
				return Double.compare(t1.totalDamage(), t.totalDamage());
			}
		});
		Collections.sort(nonBoarderGuardsByAbility, new Comparator<CrewType>() {
			@Override
			public int compare(CrewType t, CrewType t1) {
				if (t.doesWork && !t1.doesWork) {
					return 1;
				}
				if (!t.doesWork && t1.doesWork) {
					return -1;
				}
				return Double.compare(t1.totalDamage(), t.totalDamage());
			}
		});
	}
	
	public static ArrayList<CrewType> onlyWorkers = new ArrayList<CrewType>();
	public static ArrayList<CrewType> allWorkersByAbility = new ArrayList<CrewType>();
	public static ArrayList<CrewType> allGuardsByAbility = new ArrayList<CrewType>();
	public static ArrayList<CrewType> nonBoarderGuardsByAbility = new ArrayList<CrewType>();
	public static ArrayList<CrewType> nonWorkerGuards = new ArrayList<CrewType>();
	public static ArrayList<CrewType> jumpers = new ArrayList<CrewType>();
	public static ArrayList<CrewType> grapplers = new ArrayList<CrewType>();
	public static ArrayList<CrewType> boarders = new ArrayList<CrewType>();
}
