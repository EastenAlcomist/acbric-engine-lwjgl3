package com.zarkonnen.airships;

import static com.zarkonnen.airships.FormatUtils.twoDigitsAccuracy;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils;
import static com.zarkonnen.catengine.util.Utils.*;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public strictfp class  ModuleType extends Loadable implements HasName {
	public static final int COST_PER_TILE = 5;
	private static final HashMap<String, BufferedImage> MASKCACHE = new HashMap<String, BufferedImage>();

	public void draw(Draw d, double x, double y, int ms, boolean flipped, int variant, BonusSet bonuses, double visibleResourceLevel) {
		Appearance a = getApp(bonuses, visibleResourceLevel);
		if (framesAreVariants) { ms = variant * a.getInterval(); }
		a.draw(d, x, y, ms, flipped);
	}
	
	public void drawAsBlueprint(Draw d, double x, double y, int ms, boolean flipped, int variant, BonusSet bonuses, float intensity) {
		if (framesAreVariants) { ms = variant * app.get(bonuses).getInterval(); }
		getApp(bonuses, 1).drawAsBlueprint(d, x, y, ms, flipped, intensity);
	}
	
	public void drawAsRedOutline(Draw d, double x, double y, int ms, boolean flipped, int variant, BonusSet bonuses) {
		if (framesAreVariants) { ms = variant * app.get(bonuses).getInterval(); }
		getApp(bonuses, 1).drawAsRedSolid(d, x, y, ms, flipped);
	}
	
	public void draw(Draw d, double x, double y, int ms, boolean flipped, int variant, BonusSet bonuses, double visibleResourceLevel, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		Appearance a = getApp(bonuses, visibleResourceLevel);
		if (framesAreVariants) { ms = variant * a.getInterval(); }
		a.draw(d, x, y, ms, flipped, light, lightStrength, ambient, ambientSaturation);
	}

	public void draw(Draw d, double x, double y, int ms, Clr clr, boolean flipped, int variant, BonusSet bonuses, double visibleResourceLevel, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		Appearance a = getApp(bonuses, visibleResourceLevel);
		if (framesAreVariants) { ms = variant * a.getInterval(); }
		a.draw(d, x, y, ms, clr, flipped, light, lightStrength, ambient, ambientSaturation);
	}
	
	public void drawBack(Draw d, double x, double y, int ms, Clr clr, boolean flipped, int variant, BonusSet bonuses, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		if (framesAreVariants) { ms = variant * app.get(bonuses).getInterval(); }
		Appearance back = getBack(bonuses);
		if (this.flipped ^ flipped) {
			x += (w - back.width()) * AGame.SGS;
		}
		back.draw(d, x, y, ms, clr, flipped, light, lightStrength, ambient, ambientSaturation);
	}
	
	public void drawExternal(Draw d, double x, double y, int ms, boolean flipped, int variant, BonusSet bonuses, Image[] light, float lightStrength, Color ambient, float ambientSaturation, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, boolean damaged, boolean destroyed, PaintType subPaintType) {
		if (framesAreVariants) { ms = variant * app.get(bonuses).getInterval(); }
		ArrayList<ExternalApp> eas = getExternalApps(bonuses, damaged, destroyed);
		int easz = eas.size();
		for (int eai = 0; eai < easz; eai++) {
			ExternalApp ea = eas.get(eai);
			if (ea.app.spritesheetBundle != ssb) {
				if (additionalSSBs != null) { additionalSSBs.add(ea.app.spritesheetBundle); }
				continue;
			}
			Clr subTrg = null;
			if (externalSubColor != null) {
				subTrg = subPaintType == null ? externalSubColor : externalSubColorByPaintIndex[subPaintType.ordinal()];
			}
			float shiny = subPaintType == null ? 0.36f : subPaintType.shinyOrPassthrough();
			if (flipped) {
				int newDx = w - ea.dx - ea.app.width();
				ea.app.drawSub(d, x + newDx * AGame.SGS, y + ea.dy * AGame.SGS, 0, 0, ms, null, flipped, light, lightStrength, ambient, ambientSaturation, externalSubColor, subTrg, shiny);
			} else {
				ea.app.drawSub(d, x + ea.dx * AGame.SGS, y + ea.dy * AGame.SGS, 0, 0, ms, null, flipped, light, lightStrength, ambient, ambientSaturation, externalSubColor, subTrg, shiny);
			}
		}
	}
	
	public boolean hasColoration() {
		return externalSubColor != null;
	}
	
	public Clr colorationDefault() {
		return externalSubColor;
	}

	public boolean canOccupy(int x, int y) {
		for (Pair<Integer, Integer> p : canOccupy) {
			if (p.a == x && p.b == y) { return true; }
		}
		return false;
	}
	
	public int getResourceCapacity(Resource r, BonusSet bonuses) {
		switch (r) {
			case AMMO:
				return ammo.get(bonuses);
			case COAL:
				return coal.get(bonuses);
			case REPAIR:
				return repair.get(bonuses);
			case WATER:
				return water.get(bonuses);
			default: return 0;
		}
	}

	public final boolean isOccupable() {
		return !canOccupy.isEmpty();
	}

	public boolean isWeapon() {
		return isWeapon;
	}
	
	public boolean isRam() {
		return isRam;
	}
	
	public double approxDPS(BonusSet bonuses) {
		int blast = blastDmg.get(bonuses);
		int pen = penDmg.get(bonuses);
		if (blast > 4) {
			blast = blast - 4;
		} else if (blast > 0) {
			blast = 1;
		}
		if (pen > 4) {
			pen = pen - 4;
		} else if (pen > 0) {
			pen = 1;
		}
		return (blast * (blastSplashRadius.get(bonuses) > 0 ? 3 : 1) + pen * (penSplashRadius.get(bonuses) > 0 ? 3 : 1) + directDmg.get(bonuses)) * 1000.0 / reload.get(bonuses);
	}

	public int getOccupableTileCount() {
		return canOccupy.size();
	}

	public boolean in(ModuleCategory mc) {
		return categories.contains(mc);
	}
	
	public boolean hidden() {
		return categories.isEmpty();
	}

	boolean drawAppearanceInside() {
		return drawAppearanceInside;
	}

	boolean producesHorizontalDrag() {
		return producesHorizontalDrag;
	}
	
	public boolean playDestructionSoundAtStartOfDestruction() {
		return playDestructionSoundAtStartOfDestruction;
	}
	
	public boolean runsWhenDestroyed() {
		return runsWhenDestroyed;
	}
		
	public static strictfp class FragmentImg {
		int dx, dy;
		Img img;
		Img flipped;
		SpritesheetBundle ssb;

		public FragmentImg(int dx, int dy, Img img) {
			this.dx = dx;
			this.dy = dy;
			this.img = img;
			this.flipped = img.flip();
			if (img.src.endsWith("FRAGMENTS")) {
				ssb = SpritesheetBundle.ofName(img.src.replace("FRAGMENTS", "")).getFragmentsSheet();
			} else {
				ssb = SpritesheetBundle.ofName(img.src);
			}
		}
	}
	
	private String flippedFrom;
	private boolean flipped;
	private ModuleType flippedVersion;
	private ModuleType verticallyFlippedVersion; private String verticallyFlippedVersionName;
	private ArrayList<ModuleType> variants = new ArrayList<ModuleType>();
	private ArrayList<String> variantNames = new ArrayList<String>();
	private ModuleType variantGroupHead;
	private VariantType variantType;
	private final ArrayList<ModuleCategory> categories = new ArrayList<ModuleCategory>();
	private EnumSet<ShipType> availableFor = EnumSet.allOf(ShipType.class);
	private int w;
	private int h;
	private BonusableValue<Integer> hp;
	private BonusableValue<Integer> destroyedHP;
	private BonusableValue<Integer> destructionLength;
	private boolean instantlyDestroyed;
	private boolean destroyEntireShipOnDestruction;
	private boolean hasGenericDestructionFragments;
	private double fragmentsSpeedMult;
	private double fragmentsFireMult;
	private double fragmentsDensity;
	private BonusableValue<Double> destructionParticleDensity;
	private BonusableValue<ParticleType> destructionParticle = BonusableValue.of(null);
	private BonusableValue<SoundEffect> destructionSound = BonusableValue.of(null);
	private BonusableValue<ParticleType> hitParticle;
	private BonusableValue<SoundEffect> runningLoop;
	private BonusableValue<Integer> msUntilPlayLoop;
	private BonusableValue<Integer> fireHP = BonusableValue.of(0);
	private BonusableValue<Integer> explodeHP = BonusableValue.of(0);
	private BonusableValue<Integer> explodeDmg;
	private BonusableValue<Integer> explodeRadius;
	private BonusableValue<Integer> explodeFuzeLength;
	private BonusableValue<Integer> moveDelay;
	private BonusableValue<Integer> weight;
	private BonusableValue<Integer> firedWeightDecrease;
	private BonusableValue<Integer> coal = BonusableValue.of(0);
	private BonusableValue<Integer> ammo = BonusableValue.of(0);
	private BonusableValue<Integer> ammoRegenerationEvery = BonusableValue.of(0);
	private BonusableValue<Integer> coalRegenerationEvery = BonusableValue.of(0);
	private BonusableValue<Integer> repairRegenerationEvery = BonusableValue.of(0);
	private BonusableValue<Integer> waterRegenerationEvery = BonusableValue.of(0);
	private boolean hasResources;
	private BonusableValue<Integer> sickbay = BonusableValue.of(0);
	private BonusableValue<Boolean> necromancy = BonusableValue.of(Boolean.FALSE);
	private BonusableValue<Integer> repair = BonusableValue.of(0);
	private BonusableValue<Integer> water = BonusableValue.of(0);
	private BonusableValue<Integer> quarters = BonusableValue.of(0);
	private BonusableValue<Integer> supplyRequired = BonusableValue.of(-1);  // -1 means don't override
	private BonusableValue<Integer> extraSupplyCapacity = BonusableValue.of(0);
	private BonusableValue<CrewType> quartersType = BonusableValue.of(null);
	private BonusableValue<Integer> command = BonusableValue.of(0);
	private BonusableValue<Integer> extraCommandPointsRequired = BonusableValue.of(0);
	private BonusableValue<Integer> lift = BonusableValue.of(0);
	private BonusableValue<Double> propulsion = BonusableValue.of(0d);
	private BonusableValue<Integer> coalReload; // in rounds
	private boolean reloadCoalFromSelf;
	private BonusableValue<Integer> reload = BonusableValue.of(0);
	private boolean obeysFireMode = true;
	private boolean inaccuracyFromWeather = true;
	private BonusableValue<Integer> ammoPerClip = BonusableValue.of(0);
	private BonusableValue<Integer> clip = BonusableValue.of(0);
	private BonusableValue<Integer> clipReloadTime = BonusableValue.of(0); // The time taken to reload the clip from an ammo resource.
	private BonusableValue<Boolean> canResupplyInCombat = BonusableValue.of(true);
	private BonusableValue<Double> inaccuracy = BonusableValue.of(0d); // distance of 1 standard deviation from target tile
	private BonusableValue<Double> fixedInaccuracyVsTroops;
	private BonusableValue<Integer> blastDmg;
	private BonusableValue<Integer> blastSplashRadius;
	private BonusableValue<Boolean> splashFriendlyFire = BonusableValue.of(true);
	private BonusableValue<Integer> penDmg;
	private BonusableValue<Integer> penSplashRadius;
	private BonusableValue<Integer> directDmg;
	private BonusableValue<Integer> directSplashRadius;
	private BonusableValue<Integer> numShots;
	private BonusableValue<Integer> shootTroopsRange;
	private BonusableValue<Double> multiShotJitter;
	private BonusableValue<Double> shotSpeedVariation;
	private BonusableValue<Double> recoilForce;
	private BonusableValue<Double> impactForce;
	private BonusableValue<Double> targetAttractivenessMult;
	private BonusableValue<CrewType> spawnCrewOnImpact;
	private BonusableValue<Integer> spawnNumCrewOnImpact;
	private BonusableValue<Boolean> spawnCrewInsideIfArmourPierced;
	private BonusableValue<Boolean> alwaysSpawnCrewInside;
	private BonusableValue<Boolean> spawnCrewOnMiss;
	private boolean[] frontOnly;
	private boolean[] backOnly;
	private boolean[] bottomOnly;
	private boolean[] topOnly;
	private boolean countsAsActiveCrew;
	private boolean preventsBoarding;
	private boolean preventsSurrender;
	private BonusableValue<Integer> crew;
	private BonusableValue<Integer> optionalCrew;
	private BonusableValue<Integer> recommendedCrew;
	private BonusableValue<Integer> recommendedGuards;
	private BonusableValue<Integer> fixedGuards;
	private BonusableValue<Integer> cost = BonusableValue.of(0);
	private BonusableValue<Double> maintenanceCost = BonusableValue.of(Double.NaN);
	private BonusableValue<Integer> shipHPBonus = BonusableValue.of(0);
	private BonusableValue<Boolean> canGivePlaneCommands = BonusableValue.of(false);
	private BonusableValue<Appearance> app;
	private boolean isRam;
	private boolean isSail;
	private BonusableValue<Arc> fireArc = BonusableValue.of(null), innerTwoThirdsFireArc = BonusableValue.of(null);
	private BonusableValue<Double> muzzleCenterX;
	private BonusableValue<Double> muzzleCenterY;
	private BonusableValue<Double> muzzleLength;
	private BonusableValue<Boolean> muzzleFlash;
	private BonusableValue<WeaponAppearance> weaponAppearance = BonusableValue.of(null);
	private BonusableValue<GuidanceSystemInfo> guidanceSystem = BonusableValue.of(null);
	private BonusableValue<BeamSpec> beamSpec = BonusableValue.of(null);
	private boolean isWeapon;
	private BonusableValue<Tether.Spec> tetherSpec = BonusableValue.of(null);
	private BonusableValue<Integer> maxXRange = BonusableValue.of(0);
	private BonusableValue<Integer> minXRange = BonusableValue.of(0);
	private BonusableValue<Integer> maxUpRange = BonusableValue.of(0);
	private BonusableValue<Integer> maxRange = BonusableValue.of(0);
	private BonusableValue<SoundEffect> fireSound = BonusableValue.of(null);
	private BonusableValue<SoundEffect> hitSound = BonusableValue.of(null);
	private BonusableValue<Integer> optimumRange = BonusableValue.of(500);
	private BonusableValue<Integer> soundEvery = BonusableValue.of(1);
	private final ArrayList<Utils.Pair<Integer, Integer>> windows = new ArrayList<Utils.Pair<Integer, Integer>>();
	private final ArrayList<Utils.Pair<Integer, Integer>> canOccupy = new ArrayList<Utils.Pair<Integer, Integer>>();
	private final ArrayList<Utils.Pair<Integer, Integer>> hangarPositions = new ArrayList<Pair<Integer, Integer>>();
	private BonusableValue<ArrayList<ModuleParticleEmitter>> emitters;// = new ArrayList<ModuleParticleEmitter>();
	private BonusableValue<ArrayList<ModuleParticleEmitter>> damagedEmitters;
	private BonusableValue<ArrayList<ModuleParticleEmitter>> destroyedEmitters;
	private BonusableValue<ArrayList<Appearance>> depletedResourceApps;
	private BonusableValue <ArrayList<ExternalApp>> externalApps;// = new ArrayList<ExternalApp>();
	private BonusableValue<ArrayList<ExternalApp>> damagedExternalApps;// = null;
	private BonusableValue<ArrayList<ExternalApp>> destroyedExternalApps;// = null;
	private boolean gunPortsCreateDrag;
	private double extraVerticalAirFriction = 0;
	private boolean[] leftDoors;
	private boolean[] rightDoors;
	private boolean[] upDoors;
	private Bonus required;
	private BonusableValue<Double> jitterMerge;
	private BonusableValue<Double> shotSpeed;// = 1.9;
	private boolean external = false;
	private ArmourType armourType = null;
	private boolean drawDoors = true;
	private boolean framesAreVariants = false;
	private int externalDrawPriority = 1;
	private boolean drawExternalsWhenInside = false;
	private BonusableValue<Integer> supplyProvided;
	private boolean doesCreak = true, preventsCreak = false;
	private BonusableValue<Double> adjacencyBonusStrength;
	private boolean ignoreHPAdjacency;
	private BonusableValue<Integer> structuralStressAmount;
	private BonusableValue<Double> hardness;
	private BonusableValue<Double> collisionDamageReceivedMult;
	private boolean canParticlesStick;
	private ArrayList<Spring> springs = new ArrayList<Spring>();
	private ArrayList<Wheel.Spec> wheelSpecs = new ArrayList<Wheel.Spec>();
	private ArrayList<Leg.Spec> legSpecs = new ArrayList<Leg.Spec>();
	private BonusableValue<ArrayList<ModuleLightSource>> lights;
	private ArrayList<TentacleSpec> tentacleSpecs = new ArrayList<TentacleSpec>();
	private boolean tentacleDeathSpasms = false;
	private int createsExceptionalCombatEventAfterMs = 0;
	private boolean nonCombat = false;
	private int aiMaxY = 10000;
	private BonusableValue<TileMask[][]> tileMasks;
	private BonusableValue<Img> armourMask;
	public Clr externalSubColor;
	public Clr externalSubBaseColor;
	public Clr[] externalSubColorByPaintIndex;
	private boolean drawAppearanceInside;
	private boolean producesHorizontalDrag;
	private boolean playDestructionSoundAtStartOfDestruction;
	private boolean runsWhenDestroyed;
	private Bonus providesBonus;
	private boolean providesBonusWhenDestroyed;
	private boolean usesSuspendium;
	private int destroySuspendiumInRadius;
	public boolean hasSolidAppearance;
	private boolean hasArcingShot;
	private double arcGravity;
	private BonusableValue<Double> accuracyVsAirships = BonusableValue.of(1.0);
	private BonusableValue<Double> accuracyFromAirships = BonusableValue.of(1.0);
	private BonusableValue<Integer> shotDelay = BonusableValue.of(0);
	private BonusableValue<Double> maxXSpeed = BonusableValue.of(10000.0);
	private boolean isSpider;
	private BonusableValue<Integer> sniperChancePercent = BonusableValue.of(0);
	private BonusableValue<Double> setFireMultiplier = BonusableValue.of(0.0);
	
	public HashMap<String, ShipwideModifier> shipwideModifiers = new HashMap<String, ShipwideModifier>();
	
	private BonusableValue<ArrayList<ArrayList<FragmentImg>>> appFragments;// = new ArrayList<ArrayList<FragmentImg>>();
	private BonusableValue<ArrayList<ArrayList<FragmentImg>>> appWreckage;// = new ArrayList<ArrayList<FragmentImg>>();
	private BonusableValue<ArrayList<ArrayList<ArrayList<FragmentImg>>>> externalFragments;// = new ArrayList<ArrayList<ArrayList<FragmentImg>>>();
	
	public ModuleType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
						
		if (o.has("flippedFrom")) {
			flippedFrom = o.getString("flippedFrom");
			return; // Everything else is handled in post-load.
		}
		
		verticallyFlippedVersionName = o.optString("verticallyFlippedVersion", null);
		
		if (o.has("variants")) {
			JSONArray a = o.getJSONArray("variants");
			variantNames.add(name);
			for (int i = 0; i < a.length(); i++) {
				variantNames.add(a.getString(i));
			}
		}
		
		if (o.has("variantType")) {
			variantType = VariantType.ofName(o.getString("variantType"));
		} else {
			variantType = VariantType.getDefault();
		}
		
		w = o.getInt("w");
		h = o.getInt("h");
		app = BonusableValue.objectFromJSONRequired(o, "appearance", new Appearance.FromJSON());
		hasSolidAppearance = o.optBoolean("hasSolidAppearance", false);
		hasArcingShot = o.optBoolean("hasArcingShot", false);
		arcGravity = o.optDouble("arcGravity", AGame.G);
		accuracyVsAirships = BonusableValue.doubleFromJSON(o, "accuracyVsAirships", 1.0);
		accuracyFromAirships = BonusableValue.doubleFromJSON(o, "accuracyFromAirships", 1.0);
		shotDelay = BonusableValue.intFromJSON(o, "shotDelay", 0);
		maxXSpeed = BonusableValue.doubleFromJSON(o, "maxXSpeed", 10000);
		isSpider = o.optBoolean("isSpider", false);
		sniperChancePercent = BonusableValue.intFromJSON(o, "sniperChancePercent", 0);
		setFireMultiplier = BonusableValue.doubleFromJSON(o, "setFireMultiplier", 1);
		
		appFragments = BonusableValue.derive(app, new BonusableValue.Derive<Appearance, ArrayList<ArrayList<FragmentImg>>>() {
			@Override
			public ArrayList<ArrayList<FragmentImg>> derive(Appearance from) {
				return new ArrayList<ArrayList<FragmentImg>>();
			}
		});
		
		appWreckage = BonusableValue.derive(app, new BonusableValue.Derive<Appearance, ArrayList<ArrayList<FragmentImg>>>() {
			@Override
			public ArrayList<ArrayList<FragmentImg>> derive(Appearance from) {
				return new ArrayList<ArrayList<FragmentImg>>();
			}
		});
		
		TileMask[][] defaultTMs = new TileMask[h][w];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				defaultTMs[y][x] = TileMask.FULL;
			}
		}
		tileMasks = BonusableValue.objectFromJSON(o, "mask", defaultTMs, new BonusableValue.FromJSON<TileMask[][]>() {
			@Override
			public TileMask[][] construct(JSONObject o, BonusSet b) {
				TileMask[][] tms = new TileMask[h][w];
				String src = app.get(b).spritesheetBundle.name;
				int mx = o.getInt("x");
				int my = o.getInt("y");
				if (!MASKCACHE.containsKey(src)) {
					MASKCACHE.put(src, SpriteUtils.loadBufferedImage(src));
				}
				BufferedImage img = MASKCACHE.get(src);
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						tms[y][x] = TileMask.fromImage(img, (mx + x) * AGame.SGS, (my + y) * AGame.SGS);
					}
				}
				return tms;
			}
		});
		if (!o.optBoolean("external", false)) {
			armourMask = BonusableValue.objectFromJSON(o, "mask", null, new BonusableValue.FromJSON<Img>() {
				@Override
				public Img construct(JSONObject o, BonusSet b) {
					String src = app.get(b).spritesheetBundle.name;
					int mx = o.getInt("x");
					int my = o.getInt("y");
					return new Img(src, mx * AGame.SGS, my * AGame.SGS, w * AGame.SGS, h * AGame.SGS, false);
				}
				
			});
		} else {
			armourMask = BonusableValue.of(null);
		}
		
		drawAppearanceInside = o.optBoolean("drawAppearanceInside", true);
		for (int x = 0; x < w; x++) {
			canOccupy.add(p(x, h - 1));
		}
		if (o.has("canOccupy")) {
			canOccupy.clear();
			if (o.optBoolean("canOccupy")) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						canOccupy.add(p(x, y));
					}
				}
			} else {
				JSONArray ws = o.getJSONArray("canOccupy");
				for (int i = 0; i < ws.length(); i++) {
					canOccupy.add(new Pair<Integer, Integer>(
							ws.getJSONObject(i).getInt("x"), ws.getJSONObject(i).getInt("y")));
				}
			}
		}
		leftDoors = new boolean[h]; leftDoors[h - 1] = true;
		rightDoors = new boolean[h]; rightDoors[h - 1] = true;
		upDoors = new boolean[w];
		frontOnly = new boolean[h];
		backOnly = new boolean[h];
		topOnly = new boolean[w];
		bottomOnly = new boolean[w];
		
		JSONArray cats = o.getJSONArray("categories");
		for (int i = 0; i < cats.length(); i++) {
			categories.add(ModuleCategory.ofName(cats.getString(i)));
		}
		hp = BonusableValue.intFromJSON(o, "hp", 100);
		destroyedHP = BonusableValue.intFromJSON(o, "destroyedHP", -hp.get(BonusSet.empty()) / 2);
		destructionLength = BonusableValue.intFromJSON(o, "destructionLength", 0);
		instantlyDestroyed = o.optBoolean("instantlyDestroyed", false);
		destroyEntireShipOnDestruction = o.optBoolean("destroyEntireShipOnDestruction", false);
		fireHP = BonusableValue.intFromJSON(o, "fireHP", 0);
		explodeHP = BonusableValue.intFromJSON(o, "explodeHP", -1);
		explodeDmg = BonusableValue.intFromJSON(o, "explodeDmg", 0);
		explodeRadius = BonusableValue.derive(explodeDmg, new BonusableValue.Derive<Integer, Integer>() {
			@Override
			public Integer derive(Integer from) {
				return from == 0 ? 0 : (int) StrictMath.floor(30 + StrictMath.sqrt(from * 2.5));
			}
		});
		explodeFuzeLength = BonusableValue.intFromJSON(o, "explodeFuzeLength", 1500);
		hasGenericDestructionFragments = o.optBoolean("hasGenericDestructionFragments", true);
		destructionParticle = BonusableValue.loadableFromJSON(o, "destructionParticle", ParticleType.ofName("big_smoke"), ParticleType.class);
		destructionParticleDensity = BonusableValue.doubleFromJSON(o, "destructionParticleDensity", 0.3);
		aiMaxY = o.optInt("aiMaxY", 10000);
		canGivePlaneCommands = BonusableValue.booleanFromJSON(o, "canGivePlaneCommands", false);
		if (o.has("destructionSound")) {
			try {
				destructionSound = BonusableValue.of(new SoundEffect(o.getString("destructionSound"), o.optInt("numDestructionSounds", 1), o.optDouble("destructionSoundVolume", 1.0)));
			} catch (Exception e) {
				destructionSound = BonusableValue.objectFromJSON(o, "destructionSound", null, new SoundEffect.FromJSON(false));
			}
		}
		hitParticle = BonusableValue.loadableFromJSON(o, "hitParticle", null, ParticleType.class);
		fragmentsSpeedMult = o.optDouble("fragmentsSpeedMult", 1.0);
		fragmentsFireMult = o.optDouble("fragmentsFireMult", 1.0);
		fragmentsDensity = o.optDouble("fragmentsDensity", 0.5);
		
		runningLoop = BonusableValue.objectFromJSON(o, "runningLoop", null, new SoundEffect.FromJSON(true));

		msUntilPlayLoop = BonusableValue.intFromJSON(o, "msUntilPlayLoop", 80);
		
		moveDelay = BonusableValue.intFromJSONWithDivAndMinAndMax(o, "moveDelay", 800, 1, 10, 10000);
		weight = BonusableValue.intFromJSON(o, "weight", 100);
		firedWeightDecrease = BonusableValue.intFromJSON(o, "firedWeightDecrease", 0);
		coal = BonusableValue.intFromJSON(o, "coal", 0);
		ammo = BonusableValue.intFromJSON(o, "ammo", 0);
		ammoRegenerationEvery = BonusableValue.intFromJSON(o, "ammoRegenerationEvery", 0);
		coalRegenerationEvery = BonusableValue.intFromJSON(o, "coalRegenerationEvery", 0);
		repairRegenerationEvery = BonusableValue.intFromJSON(o, "repairRegenerationEvery", 0);
		waterRegenerationEvery = BonusableValue.intFromJSON(o, "waterRegenerationEvery", 0);
		sickbay = BonusableValue.intFromJSON(o, "sickbay", 0);
		necromancy = BonusableValue.booleanFromJSON(o, "necromancy", false);
		repair = BonusableValue.intFromJSON(o, "repair", 0);
		water = BonusableValue.intFromJSON(o, "water", 0);
		quarters = isOccupable() ? BonusableValue.intFromJSON(o, "quarters", 0) : BonusableValue.of(0);
		supplyRequired = BonusableValue.intFromJSON(o, "supplyRequired", -1); // -1 means don't override
		extraSupplyCapacity = BonusableValue.intFromJSON(o, "extraSupplyCapacity", 0);
		nonCombat = o.optBoolean("nonCombat", false);
		countsAsActiveCrew = o.optBoolean("countsAsActiveCrew", false);
		preventsBoarding = o.optBoolean("preventsBoarding", false);
		preventsSurrender = o.optBoolean("preventsSurrender", false);
		if (isOccupable()) {
			quartersType = BonusableValue.loadableFromJSON(o, "quartersType", null, CrewType.class);
		} else {
			quartersType = BonusableValue.of(null);
			quarters = BonusableValue.of(0);
		}
		command = BonusableValue.intFromJSON(o, "command", 0);
		extraCommandPointsRequired = BonusableValue.intFromJSON(o, "extraCommandPointsRequired", 0);
		shipwideModifiers.put("fleetCommandBonus", new ShipwideModifier("fleetCommandBonus", o, "fleetCommandBonus", 0));
		lift = BonusableValue.intFromJSON(o, "lift", 0);
		propulsion = BonusableValue.doubleFromJSON(o, "propulsion", 0);
		coalReload = BonusableValue.intFromJSON(o, "coalReload", 0);
		reloadCoalFromSelf = o.optBoolean("reloadCoalFromSelf", false);
		crew = isOccupable() ? BonusableValue.intFromJSON(o, "crew", 0) : BonusableValue.of(0);
		optionalCrew = isOccupable() ? BonusableValue.intFromJSON(o, "optionalCrew", 0) : BonusableValue.of(0);
		recommendedCrew = isOccupable() ? BonusableValue.intFromJSON(o, "recommendedCrew", 0) : BonusableValue.of(0);
		recommendedGuards = isOccupable() ? BonusableValue.intFromJSON(o, "recommendedGuards", 0) : BonusableValue.of(0);
		fixedGuards = isOccupable() ? BonusableValue.intFromJSON(o, "fixedGuards", 0) : BonusableValue.of(0);
		cost = BonusableValue.intFromJSON(o, "cost", 0);
		maintenanceCost = BonusableValue.doubleFromJSON(o, "maintenanceCost", Double.NaN);
		shipHPBonus = BonusableValue.intFromJSON(o, "shipHPBonus", 0);
		shipwideModifiers.put("accuracyBonus", new ShipwideModifier("accuracyBonus", o, "accuracyBonus", 0));
		shipwideModifiers.put("planeRepairBonus", new ShipwideModifier("planeRepairBonus", o, "planeRepairBonus", 0));
		shipwideModifiers.put("planeRearmBonus", new ShipwideModifier("planeRearmBonus", o, "planeRearmBonus", 0));
		shipwideModifiers.put("planeLaunchSpeedBonus", new ShipwideModifier("planeLaunchSpeedBonus", o, "planeLaunchSpeedBonus", 0));
		isWeapon = o.optBoolean("isWeapon", false);
		isRam = o.optBoolean("isRam", false);
		isSail = o.optBoolean("isSail", false);
		framesAreVariants = o.optBoolean("framesAreVariants", false);
		doesCreak = o.optBoolean("doesCreak", true);
		preventsCreak = o.optBoolean("preventsCreak", false);
		adjacencyBonusStrength = BonusableValue.doubleFromJSON(o, "adjacencyBonusStrength", 1.0);
		ignoreHPAdjacency = o.optBoolean("ignoreHPAdjacency", false);
		structuralStressAmount = o.has("structuralStressAmount") ? BonusableValue.intFromJSON(o, "structuralStressAmount", 0) : weight;
		hardness = BonusableValue.doubleFromJSON(o, "hardness", 1.0);
		collisionDamageReceivedMult = BonusableValue.doubleFromJSON(o, "collisionDamageReceivedMult", 1.0);
		canParticlesStick = o.optBoolean("canParticlesStick", true);
		createsExceptionalCombatEventAfterMs = o.optInt("createsExceptionalCombatEventAfterMs", 0);
		producesHorizontalDrag = o.optBoolean("producesHorizontalDrag", true);
		playDestructionSoundAtStartOfDestruction = o.optBoolean("playDestructionSoundAtStartOfDestruction", false);
		runsWhenDestroyed = o.optBoolean("runsWhenDestroyed", false);
		extraVerticalAirFriction = o.optDouble("extraVerticalAirFriction", 0);
		if (o.has("providesBonus")) {
			providesBonus = Bonus.ofName(o.getString("providesBonus"));
		}
		providesBonusWhenDestroyed = o.optBoolean("providesBonusWhenDestroyed", false);
		usesSuspendium = o.optBoolean("usesSuspendium", lift.get(BonusSet.empty()) > 0);
		destroySuspendiumInRadius = o.optInt("destroySuspendiumInRadius", 0);
		
		if (o.has("availableFor")) {
			availableFor.clear();
			JSONArray af = o.getJSONArray("availableFor");
			for (int i = 0; i < af.length(); i++) {
				availableFor.add(ShipType.valueOf(af.getString(i)));
			}
		}
		if (o.has("windows")) {
			JSONArray ws = o.getJSONArray("windows");
			for (int i = 0; i < ws.length(); i++) {
				windows.add(new Pair<Integer, Integer>(
						ws.getJSONObject(i).getInt("x"), ws.getJSONObject(i).getInt("y")));
			}
		}
		if (o.has("hangarPositions")) {
			JSONArray ws = o.getJSONArray("hangarPositions");
			for (int i = 0; i < ws.length(); i++) {
				hangarPositions.add(new Pair<Integer, Integer>(
						ws.getJSONObject(i).getInt("x"), ws.getJSONObject(i).getInt("y")));
			}
		}
		emitters = BonusableValue.listFromJSON(o, "emitters", new ArrayList<ModuleParticleEmitter>(), new ModuleParticleEmitter.FromJSON());
		damagedEmitters = BonusableValue.listFromJSON(o, "damagedEmitters", new ArrayList<ModuleParticleEmitter>(), new ModuleParticleEmitter.FromJSON());
		destroyedEmitters = BonusableValue.listFromJSON(o, "destroyedEmitters", new ArrayList<ModuleParticleEmitter>(), new ModuleParticleEmitter.FromJSON());
		externalApps = BonusableValue.listFromJSON(o, "externalAppearances", new ArrayList<ExternalApp>(), new ExternalApp.FromJSON());
		damagedExternalApps = o.has("damagedExternalAppearances") ? BonusableValue.listFromJSON(o, "damagedExternalAppearances", new ArrayList<ExternalApp>(), new ExternalApp.FromJSON()) : null;
		destroyedExternalApps = o.has("destroyedExternalAppearances") ? BonusableValue.listFromJSON(o, "destroyedExternalAppearances", new ArrayList<ExternalApp>(), new ExternalApp.FromJSON()) : null;
		
		depletedResourceApps = BonusableValue.listFromJSON(o, "depletedResourceAppearances", new ArrayList<Appearance>(), new Appearance.FromJSON());
		
		externalFragments = BonusableValue.derive(externalApps, new BonusableValue.Derive<ArrayList<ExternalApp>, ArrayList<ArrayList<ArrayList<FragmentImg>>>>() {
			@Override
			public ArrayList<ArrayList<ArrayList<FragmentImg>>> derive(ArrayList<ExternalApp> from) {
				return new ArrayList<ArrayList<ArrayList<FragmentImg>>>();
			}
		});
		
		if (o.has("externalSubColor")) {
			JSONObject co = o.getJSONObject("externalSubColor");
			this.externalSubColor = new Clr(co.getInt("r"), co.getInt("g"), co.getInt("b"));
			if (o.has("externalSubBaseColor")) {
				JSONObject bco = o.getJSONObject("externalSubBaseColor");
				this.externalSubBaseColor = new Clr(bco.getInt("r"), bco.getInt("g"), bco.getInt("b"));
			} else {
				this.externalSubBaseColor = externalSubColor;
			}
			ArrayList<PaintType> ps = PaintType.values();
			externalSubColorByPaintIndex = new Clr[ps.size()];
			for (int i = 0; i < ps.size(); i++) {
				Clr tc = ps.get(i).getBaseTint();
				externalSubColorByPaintIndex[i] = new Clr(
						externalSubBaseColor.r * (255 - tc.a) / 255 + tc.r * tc.a / 255,
						externalSubBaseColor.g * (255 - tc.a) / 255 + tc.g * tc.a / 255,
						externalSubBaseColor.b * (255 - tc.a) / 255 + tc.b * tc.a / 255
				);
			}
		}
		
		if (o.has("leftDoors")) {
			if (o.optBoolean("leftDoors")) {
				for (int y = 0; y < h; y++) {
					leftDoors[y] = canOccupy(0, y);
				}
			} else {
				JSONArray ds = o.getJSONArray("leftDoors");
				leftDoors[h - 1] = false;
				for (int i = 0; i < ds.length(); i++) {
					leftDoors[ds.getInt(i)] = true;
				}
			}
		}
		if (o.has("rightDoors")) {
			if (o.optBoolean("rightDoors")) {
				for (int y = 0; y < h; y++) {
					rightDoors[y] = canOccupy(w - 1, y);
				}
			} else {
				JSONArray ds = o.getJSONArray("rightDoors");
				rightDoors[h - 1] = false;
				for (int i = 0; i < ds.length(); i++) {
					rightDoors[ds.getInt(i)] = true;
				}
			}
		}
		if (o.has("upDoors")) {
			if (o.optBoolean("upDoors")) {
				for (int x = 0; x < w; x++) {
					upDoors[x] = canOccupy(x, 0);
				}
			} else {
				JSONArray ds = o.getJSONArray("upDoors");
				for (int i = 0; i < ds.length(); i++) {
					upDoors[ds.getInt(i)] = true;
				}
			}
		}
		if (o.optBoolean("frontOnly", false)) {
			for (int i = 0; i < rightDoors.length; i++) {
				rightDoors[i] = false;
				frontOnly[i] = true;
			}
		}
		if (o.optBoolean("backOnly", false)) {
			for (int i = 0; i < leftDoors.length; i++) {
				leftDoors[i] = false;
				backOnly[i] = true;
			}
		}
		if (o.optBoolean("bottomOnly", false)) {
			for (int i = 0; i < bottomOnly.length; i++) {
				bottomOnly[i] = true;
			}
		}
		if (o.optBoolean("topOnly", false)) {
			for (int i = 0; i < upDoors.length; i++) {
				upDoors[i] = false;
				topOnly[i] = true;
			}
		}
		if (o.has("frontOnlyList")) {
			JSONArray l = o.getJSONArray("frontOnlyList");
			for (int i = 0; i < l.length(); i++) {
				frontOnly[l.getInt(i)] = true;
			}
		}
		if (o.has("backOnlyList")) {
			JSONArray l = o.getJSONArray("backOnlyList");
			for (int i = 0; i < l.length(); i++) {
				backOnly[l.getInt(i)] = true;
			}
		}
		if (o.has("bottomOnlyList")) {
			JSONArray l = o.getJSONArray("bottomOnlyList");
			for (int i = 0; i < l.length(); i++) {
				bottomOnly[l.getInt(i)] = true;
			}
		}
		if (o.has("topOnlyList")) {
			JSONArray l = o.getJSONArray("topOnlyList");
			for (int i = 0; i < l.length(); i++) {
				topOnly[l.getInt(i)] = true;
			}
		}
		if (o.has("required")) {
			required = Bonus.ofNameOrNull(o.getString("required"));
		}
		external = o.optBoolean("external", false);
		if (o.has("armourType")) {
			armourType = ArmourType.ofName(o.getString("armourType"));
		}
		drawDoors = o.optBoolean("drawDoors", true);
		if (o.optBoolean("externalDrawPriority", false)) {
			externalDrawPriority = 2;
		}
		if (o.optBoolean("drawExternalsBelowDecals", false)) {
			externalDrawPriority = 0;
		}
		if (o.optBoolean("drawExternalsBelowEverything", false)) {
			externalDrawPriority = -1;
		}
		drawExternalsWhenInside = o.optBoolean("drawExternalsWhenInside", false);
		supplyProvided = BonusableValue.intFromJSON(o, "supplyProvided", 0);
		
		if (o.has("springs")) {
			JSONArray ss = o.getJSONArray("springs");
			for (int i = 0; i < ss.length(); i++) {
				JSONObject s = ss.getJSONObject(i);
				springs.add(new Spring(
						s.getDouble("xOffset"),
						s.getInt("length"),
						s.getInt("minCompressedLength"),
						s.getDouble("k"),
						s.optDouble("xFriction", 0.002),
						s.optDouble("yFriction", 0.003)
				));
			}
		}
		if (o.has("wheels")) {
			JSONArray ws = o.getJSONArray("wheels");
			for (int i = 0; i < ws.length(); i++) {
				JSONObject wh = ws.getJSONObject(i);
				if (wh.has("appearance")) {
					wheelSpecs.add(new Wheel.Spec(
							wh.getDouble("xOffset"),
							wh.getDouble("maxYOffset"),
							wh.getDouble("radius"),
							wh.optBoolean("randomPhase", false),
							new Appearance(wh.getJSONObject("appearance")),
							wh.optInt("appX", 0), wh.optInt("appY", 0),
							wh.has("leadStartX") ? new Pt(wh.getDouble("leadStartX"), wh.getDouble("leadStartY")) : null,
							wh.has("leadStartX") ? new Pt(wh.getDouble("leadEndOffsetX"), wh.getDouble("leadEndOffsetY")) : null
					));
				} else {
					wheelSpecs.add(new Wheel.Spec(
							wh.getDouble("xOffset"),
							wh.getDouble("maxYOffset"),
							wh.getDouble("radius"),
							wh.optBoolean("randomPhase", false),
							img(wh.getJSONObject("wheel")),
							wh.has("lowerLink") ? img(wh.getJSONObject("lowerLink")) : null,
							wh.has("lowerLink") ? img(wh.getJSONObject("upperLink")) : null,
							wh.optDouble("segmentStride", 3)
					));
				}
			}
		}
		if (o.has("legs")) {
			JSONArray ls = o.getJSONArray("legs");
			for (int i = 0; i < ls.length(); i++) {
				JSONObject l = ls.getJSONObject(i);
				JSONObject s = l.getJSONObject("spring");
				Spring spr = new Spring(
						s.getDouble("xOffset"),
						s.getInt("length"),
						s.getInt("minCompressedLength"),
						s.getDouble("k"),
						s.optDouble("xFriction", 0.004),
						s.optDouble("yFriction", 0.005)
				);
				SoundEffect beginStepSoundEffect = null;
				if (l.has("beginStepSound")) {
					try {
						beginStepSoundEffect = new SoundEffect(l.getString("beginStepSound"), l.optInt("numBeginStepSounds", 1), l.optDouble("beginStepSoundVolume", 1.0));
					} catch (Exception e) {
						beginStepSoundEffect = new SoundEffect(l.getJSONObject("beginStepSound"));
					}
				}
				SoundEffect footDownSoundEffect = null;
				if (l.has("footDownSound")) {
					footDownSoundEffect = new SoundEffect(l.getJSONObject("footDownSound"));
				}
				legSpecs.add(new Leg.Spec(
						l.getBoolean("back"),
						l.getDouble("xOffset"),
						l.getDouble("yOffset"),
						l.optDouble("upperLimbLength", l.optDouble("limbLength", 0)),
						l.optDouble("middleLimbLength", 0),
						l.optDouble("lowerLimbLength", l.optDouble("limbLength", 0)),
						l.getDouble("footWidth"),
						l.getDouble("footHeight"),
						l.getInt("stepLength"),
						l.getInt("maxStepTime"),
						l.getBoolean("bendForwards"),
						spr,
						img(l.getJSONObject("upperLeg")),
						l.has("middleLeg") ? img(l.getJSONObject("middleLeg")) : null,
						img(l.getJSONObject("lowerLeg")),
						img(l.getJSONObject("foot")),
						beginStepSoundEffect,
						footDownSoundEffect,
						l.optDouble("minFootY", -10000),
						l.optDouble("footOffset", 0)
				));
			}
		}
		if (o.has("tentacles")) {
			JSONArray ts = o.getJSONArray("tentacles");
			for (int i = 0; i < ts.length(); i++) {
				tentacleSpecs.add(new TentacleSpec(ts.getJSONObject(i), this));
			}
			tentacleDeathSpasms = o.optBoolean("tentacleDeathSpasms", false);
		}
		if (o.has(/*only*/"tentacleFans")) {
			JSONArray fs = o.getJSONArray("tentacleFans");
			for (int i = 0; i < fs.length(); i++) {
				JSONObject fan = fs.getJSONObject(i);
				int number = fan.getInt("number");
				GuardedRandom r = new GuardedRandom(fan.getInt("seed"));
				double startRadians = fan.getInt("startDegrees") * Math.PI / 180;
				double endRadians = fan.getInt("endDegrees") * Math.PI / 180;
				int centerX = fan.getInt("centerX");
				int centerY = fan.getInt("centerY");
				int jitterX = fan.getInt("jitterX");
				int jitterY = fan.getInt("jitterY");
				double tentacleAngleJitterRadians = fan.getInt("tentacleAngleJitterDegrees") * Math.PI / 180;
				double sizeMultMin = fan.getDouble("sizeMultMin");
				double sizeMultMax = fan.getDouble("sizeMultMax");
				int distMin = fan.getInt("distMin");
				int distMax = fan.getInt("distMax");
				ArrayList<Pair<Integer, Double>> ordering = new ArrayList<Pair<Integer, Double>>();
				for (int j = 0; j < number; j++) {
					ordering.add(new Pair(j, r.nextDouble()));
				}
				Collections.sort(ordering, new Comparator<Pair<Integer, Double>>() {
					@Override
					public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
						return Double.compare(o2.b, o1.b);
					}
				});
				for (int j = 0; j < number; j++) {
					double angle = startRadians + (endRadians - startRadians) / (number - 1) * ordering.get(j).a;
					double dist = distMin + ordering.get(j).b * (distMax - distMin);
					double x = (centerX + Math.cos(angle) * dist + jitterX * (r.nextDouble() - 0.5)) / AGame.SGS;
					double y = (centerY + Math.sin(angle) * dist + jitterY * (r.nextDouble() - 0.5)) / AGame.SGS;
					double tAngle = angle + tentacleAngleJitterRadians * (r.nextDouble() - 0.5);
					double sizeMult = sizeMultMin + r.nextDouble() * (sizeMultMax - sizeMultMin);
					TentacleSpec ts = new TentacleSpec(fan.getJSONObject("tentacle"), this);
					ts.baseAngle = tAngle;
					ts.baseXOffset = x;
					ts.baseYOffset = y;
					ts.baseLength *= sizeMult;
					ts.tipLength *= sizeMult;
					ts.baseWidth *= sizeMult;
					ts.tipWidth *= sizeMult;
					tentacleSpecs.add(ts);
				}
			}
		}
		lights = BonusableValue.listFromJSON(o, "lights", new ArrayList<ModuleLightSource>(), new BonusableValue.FromJSON<ModuleLightSource>() {
			@Override
			public ModuleLightSource construct(JSONObject l, BonusSet bs) {
				return new ModuleLightSource(
						new Clr(l.getInt("r"), l.getInt("g"), l.getInt("b")),
						l.getInt("radius"),
						l.getDouble("x"),
						l.getDouble("y"),
						l.optBoolean("outside")
				);
			}
		});
		soundEvery = BonusableValue.intFromJSON(o, "soundEvery", 1);
		targetAttractivenessMult = BonusableValue.doubleFromJSON(o, "targetAttractivenessMult", 1);
		hasResources = !BonusableValue.isAlways(coal, 0) || !BonusableValue.isAlways(ammo, 0) || !BonusableValue.isAlways(water, 0) || !BonusableValue.isAlways(repair, 0);
		canResupplyInCombat = BonusableValue.booleanFromJSON(o, "canResupplyInCombat", true);
		gunPortsCreateDrag = o.optBoolean("gunPortsCreateDrag", isWeapon);
		if (isWeapon) {
			reload = BonusableValue.intFromJSONWithDivAndMinAndMax(o, "reload", 1000, 1, 1, 100000);
			obeysFireMode = o.optBoolean("obeysFireMode", true);
			inaccuracyFromWeather = o.optBoolean("inaccuracyFromWeather", true);
			clip = BonusableValue.intFromJSON(o, "clip", 1);
			ammoPerClip = BonusableValue.intFromJSON(o, "ammoPerClip", 1);
			clipReloadTime = BonusableValue.intFromJSON(o, "clipReloadTime", 0);
			inaccuracy = BonusableValue.doubleFromJSON(o, "inaccuracy", 0);
			fixedInaccuracyVsTroops = BonusableValue.doubleFromJSON(o, "fixedInaccuracyVsTroops", 5);
			if (o.has("blastSplashRadius")) {
				blastDmg = BonusableValue.intFromJSON(o, "blastDmg", 0);
				blastSplashRadius = BonusableValue.intFromJSON(o, "blastSplashRadius", 0);
				splashFriendlyFire = BonusableValue.booleanFromJSON(o, "splashFriendlyFire", true);
			} else {
				blastDmg = BonusableValue.intFromJSON(o, "blastDmg", 0);
				blastSplashRadius = BonusableValue.of(blastDmg.get(BonusSet.empty()) > 9 ? (int) StrictMath.floor(13 + StrictMath.sqrt(blastDmg.get(BonusSet.empty()) * 5)) : 0);
				if (blastDmg.get(BonusSet.empty()) > 9) {
					blastDmg = BonusableValue.intFromJSONWithDivAndMinAndMax(o, "blastDmg", 0, 4, 0, 100000);
				}
			}
			penDmg = BonusableValue.intFromJSON(o, "penDmg", 0);
			penSplashRadius = BonusableValue.intFromJSON(o, "penSplashRadius", 0);
			directDmg = BonusableValue.intFromJSON(o, "directDmg", 0);
			directSplashRadius = BonusableValue.intFromJSON(o, "directSplashRadius", 0);
			numShots = BonusableValue.intFromJSON(o, "numShots", 1);
			multiShotJitter = BonusableValue.doubleFromJSON(o, "multiShotJitter", 0);
			shotSpeedVariation = BonusableValue.doubleFromJSON(o, "shotSpeedVariation", 0.25);
			recoilForce = BonusableValue.doubleFromJSON(o, "recoilForce", 0);
			if (o.has("impactForce")) {
				impactForce = BonusableValue.doubleFromJSON(o, "impactForce", 0);
			} else {
				impactForce = BonusableValue.derive(recoilForce, new BonusableValue.Derive<Double, Double>() {
					@Override
					public Double derive(Double from) {
						return from / 2;
					}
				});
			}
			fireArc = BonusableValue.objectFromJSONRequired(o, "fireArc", new Arc.FromJSON());
			innerTwoThirdsFireArc = BonusableValue.derive(fireArc, new Arc.InnerTwoThirds());
			muzzleCenterX = BonusableValue.doubleFromJSON(o, "muzzleCenterX", w * 0.5);
			muzzleCenterY = BonusableValue.doubleFromJSON(o, "muzzleCenterY", h * 0.5);
			muzzleLength = BonusableValue.doubleFromJSON(o, "muzzleLength", 1);
			muzzleFlash = BonusableValue.booleanFromJSON(o, "muzzleFlash", true);
			weaponAppearance = BonusableValue.objectFromJSONRequired(o, "weaponAppearance", new WeaponAppearance.FromJSON(w));
			guidanceSystem = BonusableValue.objectFromJSON(o, "guidanceSystem", null, new GuidanceSystemInfo.FromJSON());
			beamSpec = BonusableValue.objectFromJSON(o, "beamSpec", null, new BeamSpec.FromJSON());
			maxRange = BonusableValue.intFromJSON(o, "maxRange", 0);
			maxXRange = BonusableValue.intFromJSON(o, "maxXRange", 0);
			minXRange = BonusableValue.intFromJSON(o, "minXRange", 0);
			maxUpRange = BonusableValue.intFromJSON(o, "maxUpRange", 0);
			if (o.has("fireSound")) {
				try {
					fireSound = BonusableValue.of(new SoundEffect(
							o.getString("fireSound"),
							o.optInt("fireSoundCount", 1)));
				} catch (Exception e) {
					fireSound = BonusableValue.objectFromJSON(o, "fireSound", null, new SoundEffect.FromJSON(false));
				}
			}
			if (o.has("hitSound")) {
				try {
					hitSound = BonusableValue.of(new SoundEffect(o.getString("hitSound")));
				} catch (Exception e) {
					hitSound = BonusableValue.objectFromJSON(o, "hitSound", null, new SoundEffect.FromJSON(false));
				}
			}
			optimumRange = BonusableValue.intFromJSON(o, "optimumRange", 500);
			jitterMerge = BonusableValue.doubleFromJSON(o, "jitterMerge", 0);
			shotSpeed = BonusableValue.doubleFromJSON(o, "shotSpeed", 1.6);
			tetherSpec = BonusableValue.objectFromJSON(o, "tetherSpec", null, new Tether.Spec.FromJSON());
			int defaultSTR = 0;
			if (penDmg.get(BonusSet.empty()) + blastDmg.get(BonusSet.empty()) + directDmg.get(BonusSet.empty()) <= 20 && fireArc.get(BonusSet.empty()).sizeRadians >= StrictMath.PI / 2 && reload.get(BonusSet.empty()) <= 2000 && minXRange.get(BonusSet.empty()) == 0) {
				defaultSTR = 400;
				if (maxRange.get(BonusSet.empty()) > 0) {
					defaultSTR = StrictMath.min(defaultSTR, maxRange.get(BonusSet.empty()) / 2);
				}
				if (maxXRange.get(BonusSet.empty()) > 0) {
					defaultSTR = StrictMath.min(defaultSTR, maxXRange.get(BonusSet.empty()) / 2);
				}
			}
			shootTroopsRange = BonusableValue.intFromJSON(o, "shootTroopsRange", defaultSTR);
			spawnCrewOnImpact = BonusableValue.loadableFromJSON(o, "spawnCrewOnImpact", null, CrewType.class);
			spawnNumCrewOnImpact = BonusableValue.intFromJSON(o, "spawnNumCrewOnImpact", 1);
			spawnCrewInsideIfArmourPierced = BonusableValue.booleanFromJSON(o, "spawnCrewInsideIfArmourPierced", true);
			alwaysSpawnCrewInside = BonusableValue.booleanFromJSON(o, "alwaysSpawnCrewInside", false);
			spawnCrewOnMiss = BonusableValue.booleanFromJSON(o, "spawnCrewOnMiss", false);
		}
	}
	
	public static ArrayList<ModuleType> tracks = new ArrayList<ModuleType>();
	public static ArrayList<ModuleType> interesting = new ArrayList<ModuleType>();
	public static ArrayList<ModuleType> keelsAndRams = new ArrayList<ModuleType>();
	
	public static ArrayList<String> checkForRefProblems() {
		ArrayList<String> errs = new ArrayList<String>();
		for (ModuleType mt : all(ModuleType.class)) {
			if (mt.flippedFrom != null) {
				try {
					ofName(mt.flippedFrom);
				} catch (Exception e) {
					errs.add("Cannot create " + mt.name + " because " + mt.flippedFrom + " is missing.");
				}
			}
		}
		return errs;
	}
	
	public static void postLoad() {
		MASKCACHE.clear();
		for (ModuleType mt : all(ModuleType.class)) {
			if (mt.verticallyFlippedVersionName != null && hasOfName(ModuleType.class, mt.verticallyFlippedVersionName)) {
				ModuleType mt2 = ofName(ModuleType.class, mt.verticallyFlippedVersionName);
				mt.verticallyFlippedVersion = mt2;
				mt2.verticallyFlippedVersion = mt;
			}
		}
		for (ModuleType mt : all(ModuleType.class)) {
			if (mt.flippedFrom != null) {
				mt.deriveFlipped(ofName(mt.flippedFrom));
			}
		}
		for (ModuleType mt : all(ModuleType.class)) {
			if (mt.flippedVersion != null && mt.flippedVersion.verticallyFlippedVersion != null && mt.flippedVersion.verticallyFlippedVersion.flippedVersion != null) {
				mt.verticallyFlippedVersion = mt.flippedVersion.verticallyFlippedVersion.flippedVersion;
			}
		}
		for (ModuleType mt : all(ModuleType.class)) {
			if (mt.variantNames.isEmpty()) { continue; }
			for (String name : mt.variantNames) {
				ModuleType mt2 = ModuleType.ofName(name);
				mt.variants.add(mt2);
				mt2.variantGroupHead = mt;
			}
		}
		for (ModuleType mt : all(ModuleType.class)) {
			if (mt.flippedFrom != null && !mt.flippedVersion.variants.isEmpty() && !mt.flippedVersion.variants.contains(mt)) {
				for (ModuleType mt2 : mt.flippedVersion.variants) {
					ModuleType fmt2 = mt2.flippedVersion;
					if (fmt2 != null) {
						mt.variants.add(fmt2);
						fmt2.variantGroupHead = mt;
					}
				}
			}
		}
		ShipwideModifier.finish();
		/*for (ModuleType mt : Loadable.all(ModuleType.class)) {
			if (mt.name.startsWith("PIPE_T_")) {
				System.out.println(mt.name);
				if (mt.variantGroupHead != null) {
					System.out.println("  vgh " + mt.variantGroupHead.name);
				}
				if (mt.getSymmetryGroupHead() != mt) {
					System.out.println("  sgh " + mt.getSymmetryGroupHead().name);
				}
				for (ModuleType v : mt.variants) {
					System.out.println("  v " + v.name);
				}
			}
		}*/
		tracks.clear();
		interesting.clear();
		keelsAndRams.clear();
		for (ModuleType mt : all(ModuleType.class)) {
			if (!mt.wheelSpecs.isEmpty()) {
				tracks.add(mt);
			}
			if (mt.getCommand(BonusSet.empty()) > 0 ||
				mt.isWeapon() ||
				(mt.getLift(BonusSet.empty()) > 0 && !mt.getExternalApps(BonusSet.empty(), false, false).isEmpty()) ||
				mt.getPropulsion(BonusSet.empty()) > 0 ||
				mt.shipwideModifiers.get("accuracyBonus").value.get(BonusSet.empty()) > 0)
			{
				interesting.add(mt);
			}
			if (mt.getShipHPBonus(BonusSet.empty()) > 0 || mt.isRam()) {
				keelsAndRams.add(mt);
			}
		}
		
		try {
			loadBasicFragments();
		} catch (Exception e) {
			throw new RuntimeException("Unable to load base game fragments.", e);
		}
		
		for (Mod m : Mod.getEnabledMods()) {
			File fragmentsF = new File(new File(m.dir, "generated"), "fragments.txt");
			if (fragmentsF.exists()) {
				try {
					loadFragments(new BufferedReader(new InputStreamReader(new FileInputStream(fragmentsF), "UTF-8")));
				} catch (Exception e) {
					e.printStackTrace(); // qqDPS
				}
			}
		}
	}

	public void deriveFlipped(ModuleType b) {
		flippedVersion = b;
		b.flippedVersion = this;
		flipped = !b.flipped;
		if (b.sourceMod != null) {
			sourceMod = b.sourceMod;
		}
		availableFor = b.availableFor;
		for (ModuleCategory mc : b.categories) {
			if (mc != ModuleCategory.ofName("BASIC")) { categories.add(mc); }
		}
		w = b.w; h = b.h;
		for (ShipwideModifier swm : b.shipwideModifiers.values()) {
			shipwideModifiers.put(swm.name, new ShipwideModifier(swm));
		}
		hasSolidAppearance = b.hasSolidAppearance;
		hasArcingShot = b.hasArcingShot;
		accuracyFromAirships = b.accuracyFromAirships;
		accuracyVsAirships = b.accuracyVsAirships;
		shotDelay = b.shotDelay;
		maxXSpeed = b.maxXSpeed;
		arcGravity = b.arcGravity;
		variantType = b.variantType;
		drawAppearanceInside = b.drawAppearanceInside;
		hp = b.hp; fireHP = b.fireHP; destroyedHP = b.destroyedHP;
		instantlyDestroyed = b.instantlyDestroyed;
		destroyEntireShipOnDestruction = b.destroyEntireShipOnDestruction;
		explodeHP = b.explodeHP; explodeDmg = b.explodeDmg; explodeFuzeLength = b.explodeFuzeLength; explodeRadius = b.explodeRadius;
		hasGenericDestructionFragments = b.hasGenericDestructionFragments;
		fragmentsSpeedMult = b.fragmentsSpeedMult;
		fragmentsFireMult = b.fragmentsFireMult;
		fragmentsDensity = b.fragmentsDensity;
		destructionParticle = b.destructionParticle;
		destructionLength = b.destructionLength;
		destructionParticleDensity = b.destructionParticleDensity;
		aiMaxY = b.aiMaxY;
		destructionSound = b.destructionSound;
		hitParticle = b.hitParticle;
		runningLoop = b.runningLoop;
		msUntilPlayLoop = b.msUntilPlayLoop;
		moveDelay = b.moveDelay;
		weight = b.weight;
		firedWeightDecrease = b.firedWeightDecrease;
		canGivePlaneCommands = b.canGivePlaneCommands;
		coal = b.coal; ammo = b.ammo; sickbay = b.sickbay; necromancy = b.necromancy; repair = b.repair; water = b.water;
		quarters = b.quarters; quartersType = b.quartersType;
		countsAsActiveCrew = b.countsAsActiveCrew;
		preventsBoarding = b.preventsBoarding; preventsSurrender = b.preventsSurrender;
		command = b.command;
		extraCommandPointsRequired = b.extraCommandPointsRequired;
		gunPortsCreateDrag = b.gunPortsCreateDrag;
		lift = b.lift; propulsion = b.propulsion; coalReload = b.coalReload; reloadCoalFromSelf = b.reloadCoalFromSelf;
		reload = b.reload; ammoPerClip = b.ammoPerClip; clip = b.clip; clipReloadTime = b.clipReloadTime; canResupplyInCombat = b.canResupplyInCombat;
		inaccuracy = b.inaccuracy; fixedInaccuracyVsTroops = b.fixedInaccuracyVsTroops;
		isWeapon = b.isWeapon;
		blastDmg = b.blastDmg; penDmg = b.penDmg; directDmg = b.directDmg;
		blastSplashRadius = b.blastSplashRadius;
		penSplashRadius = b.penSplashRadius;
		directSplashRadius = b.directSplashRadius;
		splashFriendlyFire = b.splashFriendlyFire;
		numShots = b.numShots; multiShotJitter = b.multiShotJitter; shootTroopsRange = b.shootTroopsRange;
		spawnCrewOnImpact = b.spawnCrewOnImpact; spawnNumCrewOnImpact = b.spawnNumCrewOnImpact; spawnCrewInsideIfArmourPierced = b.spawnCrewInsideIfArmourPierced; alwaysSpawnCrewInside = b.alwaysSpawnCrewInside; spawnCrewOnMiss = b.spawnCrewOnMiss;
		shotSpeedVariation = b.shotSpeedVariation;
		recoilForce = b.recoilForce; impactForce = b.impactForce;
		targetAttractivenessMult = b.targetAttractivenessMult;
		frontOnly = b.backOnly; backOnly = b.frontOnly;
		bottomOnly = flipped(b.bottomOnly);
		topOnly = flipped(b.topOnly);
		crew = b.crew; optionalCrew = b.optionalCrew; recommendedCrew = b.recommendedCrew;
		recommendedGuards = b.recommendedGuards; fixedGuards = b.fixedGuards;
		cost = b.cost;
		maintenanceCost = b.maintenanceCost;
		extraVerticalAirFriction = b.extraVerticalAirFriction;
		providesBonus = b.providesBonus;
		providesBonusWhenDestroyed = b.providesBonusWhenDestroyed;
		usesSuspendium = b.usesSuspendium;
		destroySuspendiumInRadius = b.destroySuspendiumInRadius;
		isSpider = b.isSpider;
		sniperChancePercent = b.sniperChancePercent;
		setFireMultiplier = b.setFireMultiplier;
		ammoRegenerationEvery = b.ammoRegenerationEvery;
		coalRegenerationEvery = b.coalRegenerationEvery;
		waterRegenerationEvery = b.waterRegenerationEvery;
		repairRegenerationEvery = b.repairRegenerationEvery;
		doesCreak = b.doesCreak;
		preventsCreak = b.preventsCreak;
		app = BonusableValue.derive(b.app, new BonusableValue.Derive<Appearance, Appearance>() {
			@Override
			public Appearance derive(Appearance from) {
				return from.flip();
			}
		});
		weaponAppearance = b.weaponAppearance == null ? null : BonusableValue.derive(b.weaponAppearance, new BonusableValue.Derive<WeaponAppearance, WeaponAppearance>() {
			@Override
			public WeaponAppearance derive(WeaponAppearance from) {
				return from == null ? null : from.flipped(w);
			}
		});
		guidanceSystem = b.guidanceSystem == null ? null : BonusableValue.derive(b.guidanceSystem, new BonusableValue.Derive<GuidanceSystemInfo, GuidanceSystemInfo>() {
			@Override
			public GuidanceSystemInfo derive(GuidanceSystemInfo from) {
				return from == null ? null : from.flipped(w);
			}
		});
		beamSpec = b.beamSpec;
		tetherSpec = b.tetherSpec == null ? null : BonusableValue.derive(b.tetherSpec, new BonusableValue.Derive<Tether.Spec, Tether.Spec>() {
			@Override
			public Tether.Spec derive(Tether.Spec from) {
				return from == null ? null : from.flipped(w);
			}
		});
		fireArc = b.fireArc == null ? null : BonusableValue.derive(b.fireArc, new BonusableValue.Derive<Arc, Arc>() {
			@Override
			public Arc derive(Arc from) {
				return from == null ? null : from.flipHorizontal();
			}
		});
		innerTwoThirdsFireArc = b.innerTwoThirdsFireArc == null ? null : BonusableValue.derive(b.innerTwoThirdsFireArc, new BonusableValue.Derive<Arc, Arc>() {
			@Override
			public Arc derive(Arc from) {
				return from == null ? null : from.flipHorizontal();
			}
		});
		muzzleCenterX = b.muzzleCenterX == null ? null : BonusableValue.derive(b.muzzleCenterX, new BonusableValue.Derive<Double, Double>() {
			@Override
			public Double derive(Double from) {
				return w - from;
			}
		});
		muzzleCenterY = b.muzzleCenterY;
		muzzleLength = b.muzzleLength;
		muzzleFlash = b.muzzleFlash;
		maxRange = b.maxRange;
		minXRange = b.minXRange;
		maxXRange = b.maxXRange;
		maxUpRange = b.maxUpRange;
		isRam = b.isRam;
		isSail = b.isSail;
		fireSound = b.fireSound;
		hitSound = b.hitSound;
		optimumRange = b.optimumRange;
		soundEvery = b.soundEvery;
		shipHPBonus = b.shipHPBonus;
		jitterMerge = b.jitterMerge;
		shotSpeed = b.shotSpeed;
		drawDoors = b.drawDoors;
		framesAreVariants = b.framesAreVariants;
		externalDrawPriority = b.externalDrawPriority;
		drawExternalsWhenInside = b.drawExternalsWhenInside;
		supplyProvided = b.supplyProvided;
		supplyRequired = b.supplyRequired;
		extraSupplyCapacity = b.extraSupplyCapacity;
		nonCombat = b.nonCombat;
		adjacencyBonusStrength = b.adjacencyBonusStrength;
		ignoreHPAdjacency = b.ignoreHPAdjacency;
		structuralStressAmount = b.structuralStressAmount;
		hardness = b.hardness;
		collisionDamageReceivedMult = b.collisionDamageReceivedMult;
		canParticlesStick = b.canParticlesStick;
		createsExceptionalCombatEventAfterMs = b.createsExceptionalCombatEventAfterMs;
		producesHorizontalDrag = b.producesHorizontalDrag;
		playDestructionSoundAtStartOfDestruction = b.playDestructionSoundAtStartOfDestruction;
		runsWhenDestroyed = b.runsWhenDestroyed;
		obeysFireMode = b.obeysFireMode;
		inaccuracyFromWeather = b.inaccuracyFromWeather;
		springs = b.springs; // qqDPS no flipping
		wheelSpecs = b.wheelSpecs; // qqDPS no flipping
		legSpecs = b.legSpecs; // qqDPS no flipping
		lights = BonusableValue.derive(b.lights, BonusableValue.list(new BonusableValue.Derive<ModuleLightSource, ModuleLightSource>() {
			@Override
			public ModuleLightSource derive(ModuleLightSource from) {
				return from.flipped(w);
			}
		}));
		for (Pair<Integer, Integer> win : b.windows) {
			windows.add(new Pair<Integer, Integer>(w - 1 - win.a, win.b));
		}
		for (Pair<Integer, Integer> hp : b.hangarPositions) {
			hangarPositions.add(new Pair<Integer, Integer>(w - 1 - hp.a, hp.b));
		}
		for (Pair<Integer, Integer> co : b.canOccupy) {
			canOccupy.add(new Pair<Integer, Integer>(w - 1 - co.a, co.b));
		}
		emitters = BonusableValue.derive(b.emitters, BonusableValue.list(new ModuleParticleEmitter.Flip(w)));
		if (b.damagedEmitters != null) {
			damagedEmitters = BonusableValue.derive(b.damagedEmitters, BonusableValue.list(new ModuleParticleEmitter.Flip(w)));
		}
		if (b.destroyedEmitters != null) {
			destroyedEmitters = BonusableValue.derive(b.destroyedEmitters, BonusableValue.list(new ModuleParticleEmitter.Flip(w)));
		}
		
		externalApps = BonusableValue.derive(b.externalApps, BonusableValue.list(new ExternalApp.Flip(w)));
		if (b.damagedExternalApps != null) {
			damagedExternalApps = BonusableValue.derive(b.damagedExternalApps, BonusableValue.list(new ExternalApp.Flip(w)));
		}
		if (b.destroyedExternalApps != null) {
			destroyedExternalApps = BonusableValue.derive(b.destroyedExternalApps, BonusableValue.list(new ExternalApp.Flip(w)));
		}
		depletedResourceApps = BonusableValue.derive(b.depletedResourceApps, BonusableValue.list(new BonusableValue.Derive<Appearance, Appearance>() {
			@Override
			public Appearance derive(Appearance from) {
				return from.flip();
			}
		}));
		appFragments = BonusableValue.derive(b.appFragments, new BonusableValue.Derive<ArrayList<ArrayList<FragmentImg>>, ArrayList<ArrayList<FragmentImg>>>() {
			@Override
			public ArrayList<ArrayList<FragmentImg>> derive(ArrayList<ArrayList<FragmentImg>> from) {
				return new ArrayList<ArrayList<FragmentImg>>();
			}
		});
		appWreckage = BonusableValue.derive(b.appWreckage, new BonusableValue.Derive<ArrayList<ArrayList<FragmentImg>>, ArrayList<ArrayList<FragmentImg>>>() {
			@Override
			public ArrayList<ArrayList<FragmentImg>> derive(ArrayList<ArrayList<FragmentImg>> from) {
				return new ArrayList<ArrayList<FragmentImg>>();
			}
		});
		externalFragments = BonusableValue.derive(b.externalFragments, new BonusableValue.Derive<ArrayList<ArrayList<ArrayList<FragmentImg>>>, ArrayList<ArrayList<ArrayList<FragmentImg>>>>() {
			@Override
			public ArrayList<ArrayList<ArrayList<FragmentImg>>> derive(ArrayList<ArrayList<ArrayList<FragmentImg>>> from) {
				return new ArrayList<ArrayList<ArrayList<FragmentImg>>>();
			}
		});
		externalSubColor = b.externalSubColor;
		externalSubBaseColor = b.externalSubBaseColor;
		externalSubColorByPaintIndex = b.externalSubColorByPaintIndex;
		leftDoors = b.rightDoors;
		rightDoors = b.leftDoors;
		upDoors = new boolean[b.upDoors.length];
		for (int i = 0; i < b.upDoors.length; i++) {
			upDoors[upDoors.length - 1 - i] = b.upDoors[i];
		}
		required = b.required;
		external = b.external;
		armourType = b.armourType;
		for (TentacleSpec ts : b.tentacleSpecs) {
			tentacleSpecs.add(new TentacleSpec(ts, w, this));
		}
		tentacleDeathSpasms = b.tentacleDeathSpasms;
		tileMasks = BonusableValue.derive(b.tileMasks, new BonusableValue.Derive<TileMask[][], TileMask[][]>() {
			@Override
			public TileMask[][] derive(TileMask[][] from) {
				TileMask[][] to = new TileMask[h][w];
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						if (from[y][w - x - 1] != null) {
							to[y][x] = from[y][w - x - 1].flipped;
						}
					}
				}
				return to;
			}
		});
		
		if (b.armourMask != null) {
			armourMask = BonusableValue.derive(b.armourMask, new BonusableValue.Derive<Img, Img>() {
				@Override
				public Img derive(Img from) {
					return from == null ? null : from.flip();
				}
			});
		}
	}
	
	private static boolean[] flipped(boolean[] a) {
		boolean[] b = new boolean[a.length];
		for (int i = 0; i < a.length; i++) {
			b[a.length - i - 1] = a[i];
		}
		return b;
	}
	
	public VariantType variantType() {
		return variantType;
	}
	
	public boolean isVariantGroupMember() {
		return variantGroupHead != null;
	}
	
	public boolean isVariantGroupHead() {
		return variantGroupHead == this;
	}
	
	public ModuleType getVariantGroupHeadOrThis() {
		return variantGroupHead == null ? this : variantGroupHead;
	}
	
	public ArrayList<ModuleType> getVariants() {
		return variants;
	}
	
	public double getShotSpeed(BonusSet bonuses) {
		return shotSpeed.get(bonuses);
	}

	@Override
	public String getName() {
		return _t("mod_" + name);
	}

	public EnumSet<ShipType> availableFor() {
		return availableFor;
	}

	public boolean availableFor(ShipType t) {
		return availableFor.contains(t);
	}

	public int getW() {
		return w;
	}

	public int getH() {
		return h;
	}

	public int getHp(BonusSet bonuses) {
		return hp.get(bonuses);
	}
	
	public int getDestroyedHP(BonusSet bonuses) {
		return destroyedHP.get(bonuses);
	}
	
	public int getDestructionLength(BonusSet bonuses) {
		return destructionLength.get(bonuses);
	}
	
	public boolean getInstantlyDestroyed() {
		return instantlyDestroyed;
	}
	
	public boolean getDestroyEntireShipOnDestruction() {
		return destroyEntireShipOnDestruction;
	}
	
	public boolean hasGenericDestructionFragments() {
		return hasGenericDestructionFragments;
	}
	
	public double getFragmentsSpeedMult() {
		return fragmentsSpeedMult;
	}
	
	public double getFragmentsFireMult() {
		return fragmentsFireMult;
	}
	
	public double getFragmentsDensity() {
		return fragmentsDensity;
	}
	
	public ParticleType destructionParticle(BonusSet bonuses) {
		return destructionParticle.get(bonuses);
	}
	
	public double destructionParticleDensity(BonusSet bonuses) {
		return destructionParticleDensity.get(bonuses);
	}
	
	public SoundEffect destructionSound(BonusSet bonuses) {
		return destructionSound.get(bonuses);
	}
	
	public ParticleType hitParticle(BonusSet bonuses) {
		return hitParticle.get(bonuses);
	}
	
	public SoundEffect runningLoop(BonusSet bonuses) {
		return runningLoop.get(bonuses);
	}
	
	public int msUntilPlayLoop(BonusSet bonuses) {
		return msUntilPlayLoop.get(bonuses);
	}

	public Arc getFireArc(BonusSet bonuses) {
		return fireArc.get(bonuses);
	}
	
	public Arc getInnerTwoThirdsFireArc(BonusSet bonuses) {
		return innerTwoThirdsFireArc.get(bonuses);
	}

	public double muzzleCenterX(BonusSet bonuses) {
		return muzzleCenterX.get(bonuses);
	}

	public double muzzleCenterY(BonusSet bonuses) {
		return muzzleCenterY.get(bonuses);
	}
	
	public double muzzleLength(BonusSet bonuses) {
		return muzzleLength.get(bonuses);
	}
	
	public WeaponAppearance weaponAppearance(BonusSet bonuses) {
		return weaponAppearance.get(bonuses);
	}
	
	public GuidanceSystemInfo guidanceSystem(BonusSet bonuses) {
		return guidanceSystem.get(bonuses);
	}
	
	public BeamSpec beamSpec(BonusSet bonuses) {
		return beamSpec.get(bonuses);
	}
	
	public Appearance getBack(BonusSet bonuses) {
		return weaponAppearance.get(bonuses) != null ? weaponAppearance.get(bonuses).back : null;
	}

	public int getFireHP(BonusSet bonuses) {
		return fireHP.get(bonuses);
	}

	public int getExplodeHP(BonusSet bonuses) {
		return explodeHP.get(bonuses);
	}

	public int getExplodeDmg(BonusSet bonuses) {
		return explodeDmg.get(bonuses);
	}
	
	public int getExplodeRadius(BonusSet bonuses) {
		return explodeRadius.get(bonuses);
	}
	
	public int explodeFuzeLength(BonusSet bonuses) {
		return explodeFuzeLength.get(bonuses);
	}

	public int getMoveDelay(BonusSet bonuses) {
		return moveDelay.get(bonuses);
	}

	public int getWeight(BonusSet bonuses) {
		return weight.get(bonuses);
	}
	
	public int getFiredWeightDecrease(BonusSet bonuses) {
		return firedWeightDecrease.get(bonuses);
	}

	public int getCoal(BonusSet bonuses) {
		return coal.get(bonuses);
	}
	
	public int getAmmoRegenerationEvery(BonusSet bonuses) {
		return ammoRegenerationEvery.get(bonuses);
	}
	
	public int getCoalRegenerationEvery(BonusSet bonuses) {
		return coalRegenerationEvery.get(bonuses);
	}
	
	public int getRepairRegenerationEvery(BonusSet bonuses) {
		return repairRegenerationEvery.get(bonuses);
	}
	
	public int getWaterRegenerationEvery(BonusSet bonuses) {
		return waterRegenerationEvery.get(bonuses);
	}
	
	public int getSniperChancePercent(BonusSet bonuses) {
		return sniperChancePercent.get(bonuses);
	}
	
	public double getSetFireMultiplier(BonusSet bonuses) {
		return setFireMultiplier.get(bonuses);
	}
	
	public double maintenanceCost(BonusSet bonuses, boolean supplyCostsMaintenance, double maintenanceCostPerSupply) {
		double mc = maintenanceCost.get(bonuses);
		if (Double.isNaN(mc)) {
			if (supplyCostsMaintenance) {
				return getSupplyRequired(bonuses) * maintenanceCostPerSupply;
			} else {
				return 0;
			}
		}
		return mc;
	}

	public int getAmmo(BonusSet bonuses) {
		return ammo.get(bonuses);
	}

	public int getSickbay(BonusSet bonuses) {
		return sickbay.get(bonuses);
	}
	
	public boolean necromancy(BonusSet bonuses) {
		return necromancy.get(bonuses);
	}

	public int getRepair(BonusSet bonuses) {
		return repair.get(bonuses);
	}

	public int getWater(BonusSet bonuses) {
		return water.get(bonuses);
	}

	public int getQuarters(BonusSet bonuses) {
		return quartersType.get(bonuses) == null ? 0 : quarters.get(bonuses);
	}
	
	public CrewType getQuartersType(BonusSet bonuses) {
		return quartersType.get(bonuses);
	}
	
	public boolean getCountsAsActiveCrew() {
		return countsAsActiveCrew;
	}
	
	public boolean getPreventsBoarding() {
		return preventsBoarding;
	}
	
	public boolean getPreventsSurrender() {
		return preventsSurrender;
	}

	public int getCommand(BonusSet bonuses) {
		return command.get(bonuses);
	}
	
	public int getExtraCommandPointsRequired(BonusSet bonuses) {
		return extraCommandPointsRequired.get(bonuses);
	}

	public int getLift(BonusSet bonuses) {
		return lift.get(bonuses);
	}
	
	public boolean hasLift() { return lift.get(BonusSet.empty()) > 0; }

	public double getPropulsion(BonusSet bonuses) {
		return propulsion.get(bonuses);
	}

	public int getCoalReload(BonusSet bonuses) {
		return coalReload.get(bonuses);
	}
	
	public boolean reloadCoalFromSelf() { return reloadCoalFromSelf; }

	public int getReload(BonusSet bonuses) {
		return reload.get(bonuses);
	}
	
	public boolean getObeysFireMode() {
		return obeysFireMode;
	}
	
	public boolean getInaccuracyFromWeather() {
		return inaccuracyFromWeather;
	}

	public int getAmmoPerClip(BonusSet bonuses) {
		return ammoPerClip.get(bonuses);
	}

	public boolean canResupplyInCombat(BonusSet bonuses) {
		return canResupplyInCombat.get(bonuses);
	}
	
	public int getClip(BonusSet bonuses) {
		return clip.get(bonuses);
	}
	
	public int getClipReloadTime(BonusSet bonuses) {
		return clipReloadTime.get(bonuses);
	}

	public double getInaccuracy(BonusSet bonuses) {
		return inaccuracy.get(bonuses);
	}
	
	public double getFixedInaccuracyVsTroops(BonusSet bonuses) {
		return fixedInaccuracyVsTroops.get(bonuses);
	}

	public int getBlastDmg(BonusSet bonuses) {
		return blastDmg.get(bonuses);
	}
	
	public int getBlastSplashRadius(BonusSet bonuses) {
		return blastSplashRadius.get(bonuses);
	}
	
	public int getPenSplashRadius(BonusSet bonuses) {
		return penSplashRadius.get(bonuses);
	}
	
	public int getDirectSplashRadius(BonusSet bonuses) {
		return directSplashRadius.get(bonuses);
	}
	
	public boolean splashFriendlyFire(BonusSet bonuses) {
		return splashFriendlyFire.get(bonuses);
	}
	
	public int getNumShots(BonusSet bonuses) {
		return numShots.get(bonuses);
	}
	
	public int getShootTroopsRange(BonusSet bonuses) {
		return shootTroopsRange.get(bonuses);
	}
	
	public CrewType spawnCrewOnImpact(BonusSet bonuses) {
		return spawnCrewOnImpact.get(bonuses);
	}
	
	public int spawnNumCrewOnImpact(BonusSet bonuses) {
		return spawnNumCrewOnImpact.get(bonuses);
	}
	
	public boolean spawnCrewInsideIfArmourPierced(BonusSet bonuses) {
		return spawnCrewInsideIfArmourPierced.get(bonuses);
	}
	
	public boolean alwaysSpawnCrewInside(BonusSet bonuses) {
		return alwaysSpawnCrewInside.get(bonuses);
	}
	
	public boolean spawnCrewOnMiss(BonusSet bonuses) {
		return spawnCrewOnMiss.get(bonuses);
	}
	
	public double getMultiShotJitter(BonusSet bonuses) {
		return multiShotJitter.get(bonuses);
	}
	
	public double getShotSpeedVariation(BonusSet bonuses) {
		return shotSpeedVariation.get(bonuses);
	}

	public int getPenDmg(BonusSet bonuses) {
		return penDmg.get(bonuses);
	}
	
	public int getDirectDmg(BonusSet bonuses) {
		return directDmg.get(bonuses);
	}
	
	public double getRecoilForce(BonusSet bonuses) {
		return recoilForce.get(bonuses);
	}
	
	public double getImpactForce(BonusSet bonuses) {
		return impactForce.get(bonuses);
	}
	
	public double getTargetAttractivenessMult(BonusSet bonuses) {
		return targetAttractivenessMult.get(bonuses);
	}

	public boolean[] isFrontOnly() {
		return frontOnly;
	}

	public boolean[] isBackOnly() {
		return backOnly;
	}
	
	public boolean[] isBottomOnly() {
		return bottomOnly;
	}
	
	public boolean[] isTopOnly() {
		return topOnly;
	}
	
	public int getCrew(BonusSet bonuses) {
		return crew.get(bonuses);
	}
	
	public int getOptionalCrew(BonusSet bonuses) {
		return optionalCrew.get(bonuses);
	}

	public int getRecommendedCrew(BonusSet bonuses) {
		return Math.max(recommendedCrew.get(bonuses), crew.get(bonuses));
	}
	
	public int getRecommendedGuards(BonusSet bonuses) {
		return recommendedGuards.get(bonuses);
	}
	
	public int getFixedGuards(BonusSet bonuses) {
		return fixedGuards.get(bonuses);
	}

	public int getSoundEvery(BonusSet bonuses) {
		return soundEvery.get(bonuses);
	}

	public int getCost(BonusSet bonuses) {
		return cost.get(bonuses);
	}

	public int getMinXRange(BonusSet bonuses) {
		return minXRange.get(bonuses);
	}
	
	public int getMaxXRange(BonusSet bonuses) {
		return maxXRange.get(bonuses);
	}
	
	public int getMaxRange(BonusSet bonuses) {
		return maxRange.get(bonuses);
	}
	
	public int getMaxAccurateRange(BonusSet bonuses) {
		return AGame.SGS + (int) (AGame.SGS / 1.7 / (inaccuracy.get(bonuses) + multiShotJitter.get(bonuses) * 2 + 0.000001));
	}

	public int getMaxUpRange(BonusSet bonuses) {
		return maxUpRange.get(bonuses);
	}
	
	public boolean doesCreak() {
		return doesCreak;
	}
	
	public boolean preventsCreak() {
		return preventsCreak;
	}
	
	public double getAdjacencyBonusStrength(BonusSet bonuses) {
		return adjacencyBonusStrength.get(bonuses);
	}
	
	public boolean ignoreHPAdjacency() { return ignoreHPAdjacency; }
	
	public int getStructuralStressAmount(BonusSet bonuses) {
		return structuralStressAmount.get(bonuses);
	}
	
	public double getHardness(BonusSet bonuses) {
		return hardness.get(bonuses);
	}
	
	public double getCollisionDamageReceivedMult(BonusSet bonuses) {
		return collisionDamageReceivedMult.get(bonuses);
	}
	
	public boolean canParticlesStick() {
		return canParticlesStick;
	}
	
	public int createsExceptionalCombatEventAfterMs() {
		return createsExceptionalCombatEventAfterMs;
	}

	public boolean[] getLeftDoors() {
		return leftDoors;
	}

	public boolean[] getRightDoors() {
		return rightDoors;
	}

	public boolean[] getUpDoors() {
		return upDoors;
	}
	
	public boolean hasResources() {
		return hasResources;
	}
	
	public Bonus providesBonus() {
		return providesBonus;
	}
	
	public boolean providesBonusWhenDestroyed() {
		return providesBonusWhenDestroyed;
	}
	
	public boolean usesSuspendium() {
		return usesSuspendium;
	}
	
	public int destroySuspendiumInRadius() {
		return destroySuspendiumInRadius;
	}

	public Appearance getApp(BonusSet bonuses, double resourceLevel) {
		if (hasResources) {
			ArrayList<Appearance> dep = getDepletedResourceApps(bonuses);
			if (dep.isEmpty()) {
				return app.get(bonuses);
			}
			int depIndex = resourceLevel == 0 ? dep.size() - 1 : ((int) StrictMath.round((1 - resourceLevel) * (dep.size() - 1)) - 1);
			return depIndex <= -1 ? app.get(bonuses) : dep.get(depIndex);
		}
		return app.get(bonuses);
	}
	
	public List<BonusSet> getAppBonuses() {
		return app.getBonusesIfAvailable();
	}
	
	public List<BonusSet> getExternalAppBonuses(boolean damaged, boolean destroyed) {
		if (destroyed) {
			return destroyedExternalApps != null ? destroyedExternalApps.getBonusesIfAvailable() : damagedExternalApps != null ? damagedExternalApps.getBonusesIfAvailable() : externalApps.getBonusesIfAvailable();
		}
		if (damaged) {
			return damagedExternalApps != null ? damagedExternalApps.getBonusesIfAvailable() : externalApps.getBonusesIfAvailable();
		}
		return externalApps.getBonusesIfAvailable();
	}
	
	public ArrayList<ExternalApp> getExternalApps(BonusSet bonuses, boolean damaged, boolean destroyed) {
		if (destroyed) {
			return destroyedExternalApps != null ? destroyedExternalApps.get(bonuses) : damagedExternalApps != null ? damagedExternalApps.get(bonuses) : externalApps.get(bonuses);
		}
		if (damaged) {
			return damagedExternalApps != null ? damagedExternalApps.get(bonuses) : externalApps.get(bonuses);
		}
		return externalApps.get(bonuses);
	}
	
	public ArrayList<Appearance> getDepletedResourceApps(BonusSet bonuses) {
		return depletedResourceApps.get(bonuses);
	}
	
	public List<BonusSet> getDepletedResourceAppBonuses() {
		return depletedResourceApps.getBonusesIfAvailable();
	}

	public ArrayList<Utils.Pair<Integer, Integer>> getWindows() {
		return windows;
	}
	
	public ArrayList<Utils.Pair<Integer, Integer>> getHangarPositions() {
		return hangarPositions;
	}
	
	public ArrayList<ModuleParticleEmitter> getEmitters(BonusSet bonuses) {
		return emitters.get(bonuses);
	}
	
	public ArrayList<ModuleParticleEmitter> getDamagedOrDestroyedEmitters(BonusSet bonuses, boolean damaged, boolean destroyed) {
		if (destroyed) {
			return destroyedEmitters != null ? destroyedEmitters.get(bonuses) : damagedEmitters != null ? damagedEmitters.get(bonuses) : null;
		}
		if (damaged) {
			return damagedEmitters != null ? damagedEmitters.get(bonuses) : null;
		}
		return null;
	}
	
	public ArrayList<Utils.Pair<Integer, Integer>> getCanOccupy() {
		return canOccupy;
	}

	public Bonus getRequired() {
		return required;
	}
	
	public boolean isSail() {
		return isSail;
	}

	public SoundEffect getFireSound(BonusSet bonuses) {
		return fireSound.get(bonuses);
	}

	public SoundEffect getHitSound(BonusSet bonuses) {
		return hitSound.get(bonuses);
	}

	public int getShipHPBonus(BonusSet bonuses) {
		return shipHPBonus.get(bonuses);
	}
	
	public double getJitterMerge(BonusSet bonuses) {
		return jitterMerge.get(bonuses);
	}
	
	public boolean isExternal() {
		return external;
	}
	
	public ArmourType getArmourType() {
		return armourType;
	}
	
	public boolean drawDoors() {
		return drawDoors;
	}
	
	public boolean areFramesVariants() {
		return framesAreVariants;
	}
	
	public int getExternalDrawPriority() {
		return externalDrawPriority;
	}
	
	public boolean drawExternalsWhenInside() {
		return drawExternalsWhenInside;
	}
	
	public boolean hasSpecificDestroyedExternalAppearances() {
		return destroyedExternalApps != null;
	}
	
	public int getSupplyProvided(BonusSet bonuses) {
		return supplyProvided.get(bonuses);
	}
	
	public int getOptimumRange(BonusSet bonuses) {
		return optimumRange.get(bonuses);
	}
	
	public Tether.Spec getTetherSpec(BonusSet bonuses) {
		return tetherSpec.get(bonuses);
	}
	
	public boolean gunPortsCreateDrag() {
		return gunPortsCreateDrag;
	}
	
	public ArrayList<ModuleLightSource> getLights(BonusSet bonuses) { return lights.get(bonuses); }
	
	public ArrayList<Spring> getSprings() { return springs; }
	public ArrayList<Wheel.Spec> getWheelSpecs() { return wheelSpecs; }
	public ArrayList<Leg.Spec> getLegSpecs() { return legSpecs; }
	public ArrayList<TentacleSpec> getTentacleSpecs() { return tentacleSpecs; }
	public boolean tentacleDeathSpasms() { return tentacleDeathSpasms; }
	
	public int getExtraSupplyCapacity(BonusSet bonuses) {
		return extraSupplyCapacity.get(bonuses);
	}
	
	public int getSupplyRequired(BonusSet bonuses) {
		int sr = supplyRequired.get(bonuses);
		if (sr >= 0) {
			return sr;
		}
		return (int) StrictMath.ceil(
				quarters.get(bonuses) * (quartersType.get(bonuses) != null ? quartersType.get(bonuses).supplyCost : 0) +
				coal.get(bonuses) * 0.01 +
				water.get(bonuses) * 0.01 +
				ammo.get(bonuses) * 0.0075 +
				sickbay.get(bonuses) * 0.2
		);
	}
	
	public boolean nonCombat() { return nonCombat; }
	
	public boolean isHatch(BonusSet bonuses) {
		return supplyProvided.get(bonuses) > 0;
	}
	
	public ModuleType getFlippedIfAvailable() {
		return flippedVersion;
	}
	
	public ModuleType getVerticalFlippedIfAvailable() {
		return verticallyFlippedVersion;
	}
	
	public boolean isFlipped() {
		return flipped;
	}
	
	public boolean isSymmetryGroupHead() {
		return isSymmetryGroupMember() && !flipped && (verticallyFlippedVersion == null || verticallyFlippedVersionName != null);
	}
	
	public boolean isSymmetryGroupMember() {
		return flippedVersion != null || verticallyFlippedVersion != null;
	}
	
	public ModuleType getSymmetryGroupHead() {
		if (isSymmetryGroupHead()) { return this; }
		if (flippedVersion != null) {
			if (flippedVersion.isSymmetryGroupHead()) {
				return flippedVersion;
			}
			if (flippedVersion.verticallyFlippedVersion != null && flippedVersion.verticallyFlippedVersion.isSymmetryGroupHead()) {
				return flippedVersion.verticallyFlippedVersion;
			}
		}
		if (verticallyFlippedVersion != null && verticallyFlippedVersion.isSymmetryGroupHead()) {
			return verticallyFlippedVersion;
		}
		return this;
	}
	
	public boolean muzzleFlash(BonusSet bonuses) {
		return muzzleFlash.get(bonuses);
	}
	
	public int aiMaxY() {
		return aiMaxY;
	}
	
	public boolean hasArcingShot() {
		return hasArcingShot;
	}
	
	public double arcGravity() {
		return arcGravity;
	}
	
	public double accuracyVsAirships(BonusSet bonuses) {
		return accuracyVsAirships.get(bonuses);
	}
	
	public double accuracyFromAirships(BonusSet bonuses) {
		return accuracyFromAirships.get(bonuses);
	}
	
	public int shotDelay(BonusSet bonuses) {
		return shotDelay.get(bonuses);
	}
	
	public double maxXSpeed(BonusSet bonuses) {
		return maxXSpeed.get(bonuses);
	}
	
	public boolean canGivePlaneCommands(BonusSet bonuses) {
		return canGivePlaneCommands.get(bonuses);
	}
	
	public TileMask[][] getTileMasks(BonusSet bonuses) {
		return tileMasks.get(bonuses);
	}
	
	public Img getArmourMask(BonusSet bonuses) {
		return armourMask.get(bonuses);
	}
	
	public double getExtraVerticalAirFriction() { return extraVerticalAirFriction; }
	
	public static strictfp class ModuleLightSource {
		public final Clr clr;
		public final int radius;
		public final double xOffset, yOffset;
		public final boolean outside;

		public ModuleLightSource(Clr lightClr, int lightRadius, double lightX, double lightY, boolean outside) {
			this.clr = lightClr;
			this.radius = lightRadius;
			this.xOffset = lightX;
			this.yOffset = lightY;
			this.outside = outside;
		}
		
		public ModuleLightSource flipped(int w) {
			return new ModuleLightSource(clr, radius, w - xOffset, yOffset, outside);
		}
	}
	
	public static strictfp class ModuleParticleEmitter extends Particle.Emitter {
		public double x, y;
		public boolean inside;

		public ModuleParticleEmitter(double x, double y, boolean inside, ParticleType t, double emitProbability, int numParticles, SoundEffect soundEffect) {
			super(t, emitProbability, numParticles, soundEffect);
			this.x = x;
			this.y = y;
			this.inside = inside;
		}
		
		public static strictfp class Flip implements BonusableValue.Derive<ModuleParticleEmitter, ModuleParticleEmitter> {
			private final int w;

			public Flip(int w) {
				this.w = w;
			}

			@Override
			public ModuleParticleEmitter derive(ModuleParticleEmitter mpe) {
				return new ModuleParticleEmitter(w - mpe.x, mpe.y, mpe.inside, mpe.t, mpe.emitProbability, mpe.numParticles, mpe.soundEffect);
			}			
		}
		
		public static strictfp class FromJSON implements BonusableValue.FromJSON<ModuleParticleEmitter> {
			@Override
			public ModuleParticleEmitter construct(JSONObject o, BonusSet b) {
				SoundEffect ef = null;
				if (o.has("sound")) {
					try {
						String sound = o.getString("sound");
						ef = new SoundEffect(sound, o.optDouble("volume"));
					} catch (Exception e) {
						ef = new SoundEffect(o.getJSONObject("sound"));
					}
				}
				return new ModuleParticleEmitter(
						o.getDouble("x"),
						o.getDouble("y"),
						o.optBoolean("inside", false),
						ParticleType.ofName(o.getString("type")),
						o.getDouble("emitProbability"),
						o.optInt("numParticles", 1),
						ef
				);
			}
		}
	}
	
	public ArrayList<ArrayList<FragmentImg>> getAppFragments(BonusSet bonuses) {
		return appFragments.get(bonuses);
	}
	
	public ArrayList<ArrayList<FragmentImg>> getAppWreckage(BonusSet bonuses) {
		return appWreckage.get(bonuses);
	}
	
	public ArrayList<ArrayList<ArrayList<FragmentImg>>> getExternalFragments(BonusSet bonuses) {
		return externalFragments.get(bonuses);
	}
	
	public boolean isSpider() { return isSpider; }
	
	public boolean canReplace(ModuleType src) {
		if (src == null) { return false; }
		if (w != src.w || h != src.h) { return false; }
                for (int y = 0; y < h; y++) {
                        if (frontOnly[y] && !src.frontOnly[y]) { return false; }
                        if (backOnly[y] && !src.backOnly[y]) { return false; }
                }
                for (int x = 0; x < w; x++) {
                        if (topOnly[x] && !src.topOnly[x]) { return false; }
                        if (bottomOnly[x] && !src.bottomOnly[x]) { return false; }
                }
		return true;
	}
	
	private static int twoDigitsAccuracy(int x) {
		int multAgain = 0;
		while (x >= 100) {
			x /= 10;
			multAgain++;
		}
		while (multAgain > 0) {
			x *= 10;
			multAgain--;
		}
		return x;
	}
	
	private void shipWideModifierPercentDesc(StringBuilder sb, String name, String tKey, BonusSet bonuses) {
		ShipwideModifier swm = shipwideModifiers.get(name);
		if (swm.value.get(bonuses) > 0) {
			sb.append(_t(tKey, (int) (100 * swm.value.get(bonuses)))).append("\n");
			if (!swm.stacksWith.isEmpty()) {
				sb.append(_t("Stacks_with_"));
				boolean first = true;
				for (ModuleType mt : swm.stacksWith) {
					if (!first) {
						sb.append(", ");
					}
					sb.append(mt.getName());
					first = false;
				}
				sb.append("\n");
			}
			if (!swm.doesNotStackWith.isEmpty()) {
				sb.append(_t("Doesnt_stack_with_"));
				boolean first = true;
				for (ModuleType mt : swm.doesNotStackWith) {
					if (!first) {
						sb.append(", ");
					}
					sb.append(mt.getName());
					first = false;
				}
				sb.append("\n");
			}
		}
	}
	
	public String getDescription(BonusSet bonuses, ShipType st, boolean explain) {
		StringBuilder sb = new StringBuilder();
		sb.append(getName().toUpperCase());
		if (getRequired() != null && getRequired() != Bonus.ofName("NO_BONUS")) {
			Tech.Choice providingTech = Tech.findProvider(getRequired());
			if (providingTech != null) {
				sb.append("\n").append(_t("Requires_tech", _t("tech_" + providingTech.name), providingTech.tech.tier + 1));
			} else {
				sb.append("\n").append(_t("Requires_bonus_x", getRequired().getName()));
			}
		}
		sb.append("\n\n");
		sb.append(_t("mod_desc_" + name));
		if (explain) {
			ArrayList<String> notices = new ArrayList<String>();
			notices.addAll(maintenanceCost.descriptions(bonuses));
			if (isWeapon) {
				notices.addAll(blastDmg.descriptions(bonuses));
				notices.addAll(blastSplashRadius.descriptions(bonuses));
				notices.addAll(penDmg.descriptions(bonuses));
				notices.addAll(penSplashRadius.descriptions(bonuses));
				notices.addAll(directDmg.descriptions(bonuses));
				notices.addAll(reload.descriptions(bonuses));
				notices.addAll(clip.descriptions(bonuses));
				notices.addAll(ammoPerClip.descriptions(bonuses));
				notices.addAll(minXRange.descriptions(bonuses));
				notices.addAll(maxRange.descriptions(bonuses));
				notices.addAll(maxXRange.descriptions(bonuses));
				notices.addAll(inaccuracy.descriptions(bonuses));
			}
			notices.addAll(coal.descriptions(bonuses));
			notices.addAll(water.descriptions(bonuses));
			notices.addAll(repair.descriptions(bonuses));
			notices.addAll(ammo.descriptions(bonuses));
			notices.addAll(command.descriptions(bonuses));
			notices.addAll(sickbay.descriptions(bonuses));
			notices.addAll(lift.descriptions(bonuses));
			notices.addAll(propulsion.descriptions(bonuses));
			notices.addAll(shipHPBonus.descriptions(bonuses));
			// notices.addAll(accuracyBonus.descriptions(bonuses)); MERGEME
			FormatUtils.stringList(sb, notices, "\n\n", "\n\n", "");
		}
		String nameAndDesc = sb.toString();
		sb = new StringBuilder();
		
		int mc = (int) Math.ceil(maintenanceCost(bonuses, st.supplyCostsMaintenance, EmpireStat.MAINTENANCE_COST_PER_SUPPLY.get(bonuses)) * EmpireStat.SHIP_TYPE_MAINTENANCE_MULTIPLIER.get(st).get(bonuses));
		if (maintenanceCost(bonuses, st.supplyCostsMaintenance, EmpireStat.MAINTENANCE_COST_PER_SUPPLY.get(bonuses)) > 0) {
			sb.append(_t("Maintenance_cost_x", mc)).append("\n");
			// MERGEME sb.append(_t("Maintenance_cost_x", maintenanceCost.bexplain(explain, bonuses, FormatUtils.MONEY))).append("\n");
		} else if (maintenanceCost(bonuses, st.supplyCostsMaintenance, EmpireStat.MAINTENANCE_COST_PER_SUPPLY.get(bonuses)) < 0) {
			sb.append(_t("Income_x", -mc)).append("\n");
		}
		if (!canResupplyInCombat(bonuses)) {
			sb.append(_t("cannot_resupply_module")).append("\n");
		}
		for (TentacleSpec ts : tentacleSpecs) {
			sb.append(_t("Tentacle")).append("\n");
			if (ts.attacksHull && ts.attackBlastDmg != 0) {
				sb.append("  ").append(_t("Blast_damage_x", ts.attackBlastDmg)).append("\n");
			}
			if (ts.attacksHull && ts.attackPenDmg != 0) {
				sb.append("  ").append(_t("Penetration_damage_x", ts.attackPenDmg)).append("\n");
			}
			if (ts.attacksHull && ts.attackDirectDmg != 0) {
				sb.append("  ").append(_t("Direct_damage_x", ts.attackDirectDmg)).append("\n");
			}
			if (ts.snatchesCrew) {
				sb.append("  ").append(_t("Grabs_enemy_crew")).append("\n");
			}
		}
		if (isWeapon()) {
			if (getBlastDmg(bonuses) > 0) {
				String dmg = blastDmg.bexplain(explain, bonuses);
				if (getNumShots(bonuses) > 1) {
					dmg = numShots.bexplain(explain, bonuses) + "x" + dmg;
				}
				sb.append(_t("Blast_damage_x", dmg)).append("\n");
			}
			if (getBlastSplashRadius(bonuses) > 0) {
				sb.append(_t("Splash_distance_x", blastSplashRadius.bexplain(explain, bonuses, FormatUtils.PX_TO_M))).append("\n");
			}
			if (getPenDmg(bonuses) > 0) {
				String dmg = penDmg.bexplain(explain, bonuses);
				if (getNumShots(bonuses) > 1) {
					dmg = numShots.bexplain(explain, bonuses) + "x" + dmg;
				}
				sb.append(_t("Penetration_damage_x", dmg)).append("\n");
			}
			if (getPenSplashRadius(bonuses) > 0) {
				sb.append(_t("Splash_distance_x", penSplashRadius.bexplain(explain, bonuses, FormatUtils.PX_TO_M))).append("\n");
			}
			if (getDirectDmg(bonuses) > 0) {
				String dmg = directDmg.bexplain(explain, bonuses);
				if (getNumShots(bonuses) > 1) {
					dmg = numShots.bexplain(explain, bonuses) + "x" + dmg;
				}
				sb.append(_t("Direct_damage_x", dmg)).append("\n");
			}
			if (getDirectSplashRadius(bonuses) > 0) {
				sb.append(_t("Splash_distance_x", directSplashRadius.bexplain(explain, bonuses, FormatUtils.PX_TO_M))).append("\n");
			}
			if (getSniperChancePercent(bonuses) > 0) {
				sb.append(_t("x_sniper_chance", sniperChancePercent.bexplain(explain, bonuses))).append("\n");
			}
			if (getSetFireMultiplier(bonuses) != 1) {
				sb.append(_t("x_set_fire_multiplier", setFireMultiplier.bexplain(explain, bonuses))).append("\n");
			}
			if (getClip(bonuses) > 1 || canResupplyInCombat(bonuses)) {
				if (getReload(bonuses) < 1000) {
					sb.append(_t("Rate_of_fire_x_per_second", reload.bexplain(explain, bonuses, FormatUtils.ROUNDS_PER_SECOND, FormatUtils.T_PER_SECOND))).append("\n");
				} else {
					sb.append(_t("Reload_time_x_seconds", reload.bexplain(explain, bonuses, FormatUtils.SECONDS_PRECISE))).append("\n");
				}
			}
			if (getClip(bonuses) > 1) {
				sb.append(_t("Clip_size_x_rounds", clip.bexplain(explain, bonuses, FormatUtils.ROUNDS))).append("\n");
			}
			if (canResupplyInCombat(bonuses) && getAmmoPerClip(bonuses) > 1) {
				if (getClip(bonuses) == 1) {
					sb.append(ammoPerClip.bexplain(explain, bonuses, FormatUtils.AMMO_PER_SHOT)).append("\n");
				} else {
					sb.append(ammoPerClip.bexplain(explain, bonuses, FormatUtils.AMMO_PER_CLIP)).append("\n");
				}
			}
			sb.append(_t("Fire_arc_x_degrees", (int) StrictMath.ceil(getFireArc(bonuses).sizeRadians * 180 / StrictMath.PI))).append("\n");
			if (getMinXRange(bonuses) != 0) {
				sb.append(_t("Minimum_range_x_metres", minXRange.bexplain(explain, bonuses, FormatUtils.PX_TO_M))).append("\n");
			}
			if (getMaxRange(bonuses) != 0) {
				sb.append(_t("Maximum_range_x_metres", maxRange.bexplain(explain, bonuses, FormatUtils.PX_TO_M))).append("\n");
			} else if (getMaxXRange(bonuses) != 0) {
				sb.append(_t("Maximum_range_x_metres", maxXRange.bexplain(explain, bonuses, FormatUtils.PX_TO_M))).append("\n");
			} else {
				sb.append(_t("Maximum_accurate_range_x_metres", inaccuracy.bexplain(explain, bonuses, FormatUtils.PX_TO_M_D, FormatUtils.T_INACCURACY_TO_RANGE))).append("\n");
			}
			if (getShootTroopsRange(bonuses) > 0) {
				sb.append(_t("Shoots_troops_within_x_metres", twoDigitsAccuracy(getShootTroopsRange(bonuses) / AGame.PX_TO_M))).append("\n");
			}
			if (accuracyVsAirships(bonuses) != 1) {
				sb.append(_t("x_accuracy_vs_airships", accuracyVsAirships(bonuses))).append("\n");
			}
			if (accuracyFromAirships(bonuses) != 1) {
				sb.append(_t("x_accuracy_from_airships", accuracyFromAirships(bonuses))).append("\n");
			}
			if (hasArcingShot()) {
				sb.append(_t("arcing_shot")).append("\n");
			}
			if (spawnCrewOnImpact(bonuses) != null) {
				if (spawnNumCrewOnImpact(bonuses) == 1) {
					sb.append(_t("weapon_spawn_crew_single", spawnCrewOnImpact(bonuses).getName())).append("\n");
				} else {
					sb.append(_t("weapon_spawn_crew", spawnNumCrewOnImpact(bonuses), spawnCrewOnImpact(bonuses).getPlural())).append("\n");
				}
			}
		}
		sb.append(_t("Weight_x", getWeight(bonuses))).append("\n");
		if (!canResupplyInCombat(bonuses) && getFiredWeightDecrease(bonuses) != 0) {
			sb.append(_t("Weight_after_firing_x", getWeight(bonuses) - getFiredWeightDecrease(bonuses))).append("\n");
		}
		sb.append(_t("HP_x", getHp(bonuses))).append("\n");
		if (getCoal(bonuses) > 0) {
			sb.append(_t("Coal_capacity_x", coal.bexplain(explain, bonuses))).append("\n");
		}
		if (getWater(bonuses) > 0) {
			sb.append(_t("Water_capacity_x", water.bexplain(explain, bonuses))).append("\n");
		}
		if (getRepair(bonuses) > 0) {
			sb.append(_t("Repair_supplies_x", repair.bexplain(explain, bonuses))).append("\n");
		}
		if (getAmmo(bonuses) > 0) {
			sb.append(_t("Ammo_storage_x", ammo.bexplain(explain, bonuses))).append("\n");
		}
		if (getCommand(bonuses) > 0) {
			sb.append(_t("Provides_x_command", command.bexplain(explain, bonuses, FormatUtils.COMMAND))).append("\n");
		}
		if (canGivePlaneCommands(bonuses)) {
			sb.append(_t("canGivePlaneCommands")).append("\n");
		}
		shipWideModifierPercentDesc(sb, "fleetCommandBonus", "Increases_command_of_all_ships_by_x_percent", bonuses);
		shipWideModifierPercentDesc(sb, "planeLaunchSpeedBonus", "planeLaunchSpeedBonus_x_percent", bonuses);
		if (getSupplyProvided(bonuses) > 0) {
			sb.append(_t("Provides_x_supply", getSupplyProvided(bonuses))).append("\n");
		}
		if (getSupplyRequired(bonuses) > 0) {
			sb.append(_t("Requires_x_supply", getSupplyRequired(bonuses))).append("\n");
		}
		if (getExtraSupplyCapacity(bonuses) > 0) {
			sb.append(_t("x_extra_supply_capacity", getExtraSupplyCapacity(bonuses) * EmpireStat.SHIP_SUPPLY_MULT.get(bonuses))).append("\n");
		}
		if (getQuarters(bonuses) > 0) {
			if (getQuartersType(bonuses).isMachine) {
				sb.append(_t("Contains_and_supports_x_y", getQuarters(bonuses), getQuarters(bonuses) == 1 ? getQuartersType(bonuses).getName(): getQuartersType(bonuses).getPlural())).append("\n");
			} else {
				sb.append(_t("Provides_quarters_for_x_y", getQuarters(bonuses), getQuarters(bonuses) == 1 ? getQuartersType(bonuses).getName() : getQuartersType(bonuses).getPlural())).append("\n");
			}
		}
		shipWideModifierPercentDesc(sb, "planeRepairBonus", "planeRepairBonus_x_percent", bonuses);
		shipWideModifierPercentDesc(sb, "planeRearmBonus", "planeRearmBonus_x_percent", bonuses);
		if (getSickbay(bonuses) > 0) {
			sb.append(_t("Provides_healing_for_up_to_x_crew", sickbay.bexplain(explain, bonuses, FormatUtils.CREW))).append("\n");
			if (necromancy(bonuses)) {
				sb.append(_t("Can_resurrect_dead_crew")).append("\n");
			}
		}
		if (getLift(bonuses) > 0) {
			sb.append(_t("Generates_x_lift", lift.bexplain(explain, bonuses, FormatUtils.LIFT))).append("\n");
		}
		if (getPropulsion(bonuses) > 0) {
			sb.append(_t("Generates_x_propulsion", propulsion.bexplain(explain, bonuses, FormatUtils.PROPULSION, FormatUtils.T_X_1000_TO_INT))).append("\n");
		}
		if (canResupplyInCombat(bonuses) && getCoalReload(bonuses) > 0) {
			sb.append(_t("Requires_a_unit_of_coal_every_x_seconds", getCoalReload(bonuses) / 1000)).append("\n");
		}
		if (getShipHPBonus(bonuses) > 0) {
			sb.append(_t("Increases_ship_hit_points_by_x", shipHPBonus.bexplain(explain, bonuses))).append("\n");
		}
		shipWideModifierPercentDesc(sb, "accuracyBonus", "Increases_weapon_accuracy_by_x_percent", bonuses);
		// MERGEME
		// sb.append(_t("Increases_weapon_accuracy_by_x_percent", accuracyBonus.bexplain(explain, bonuses, FormatUtils.PERCENTAGE))).append("\n");
		if (getCrew(bonuses) == 1) {
			sb.append(_t("Operators_one_crew_member")).append("\n");
		} else if (getCrew(bonuses) > 1) {
			sb.append(_t("Operators_x_crew_members", getCrew(bonuses))).append("\n");
		}
		if (getRecommendedCrew(bonuses) > 0) {
			sb.append(_t("Recommended_crew_x", getRecommendedCrew(bonuses))).append("\n");
		}
		if (getFixedGuards(bonuses) > 0) {
			sb.append(_t("Guards_stationed_x", getFixedGuards(bonuses))).append("\n");
		}
		int fireHP = getFireHP(bonuses);
		int hp = getHp(bonuses);
		if (fireHP > 0) {
			if (fireHP >= hp) {
				sb.append(_t("very_flammable")).append("\n");
			} else if (fireHP >= hp / 2) {
				sb.append(_t("flammable")).append("\n");
			} else {
				sb.append(_t("slightly_flammable")).append("\n");
			}
		}
		int explodeHP = getExplodeHP(bonuses);
		if (explodeHP > 0) {
			if (explodeHP >= hp / 3) {
				sb.append(_t("explodes_easily")).append("\n");
			} else {
				sb.append(_t("may_explode")).append("\n");
			}
		}
		
		if (getQuarters(bonuses) > 0) {
			sb.append("\n").append(getQuartersType(bonuses).getDescription()).append("\n");
		}
		
		if (sourceMod != null) {
			sb.append("\n").append(sourceMod.getName()).append("\n");
		}
		if (sb.length() == 0) {
			return nameAndDesc;
		} else {
			String info = sb.toString();
			info = info.substring(0, info.length() - 1);
			return nameAndDesc + "\n\n" + info;
		}
	}
	
	public static ModuleType ofName(String name) {
		return ofName(ModuleType.class, name);
	}
	
	private static final ArrayList<String> WHEEL_AND_LEG_PREFIXES = new ArrayList<String>(Arrays.asList(new String[] {
		"wheel", "lowerLink", "upperLink", "foot", "lowerLeg", "upperLeg"
	}));
	
	public static void loadBasicFragments() throws Exception {
		File fragsF = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "images"), "fragments.txt");
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fragsF), "UTF-8"));
		loadFragments(r);
	}
		
	public static void loadFragments(BufferedReader br) throws Exception {
		String l;
		while ((l = br.readLine()) != null) {
			String[] idBits = l.split(" ");
			String imgSrc = br.readLine();
			String[] imgBits = br.readLine().split(" ");
			Img img = new Img(imgSrc,
					Integer.parseInt(imgBits[0]), Integer.parseInt(imgBits[1]),
					Integer.parseInt(imgBits[2]), Integer.parseInt(imgBits[3]),
					false);
			int xOffset = Integer.parseInt(imgBits[4]);
			int yOffset = Integer.parseInt(imgBits[5]);
			
			if (idBits[0].equals("arm")) {
				ArmourType at = null;
				try {
					at = ArmourType.ofName(idBits[1]);
				} catch (Exception e) {
					continue;
				}
				int prevIndex = Integer.parseInt(idBits[2]);
				int newIndex = Integer.parseInt(idBits[3]);
				BonusSet bs = idBits.length > 4 ? fromToken(idBits[4]) :  BonusSet.empty();
				if (prevIndex < at.fragments.get(bs).length && newIndex < at.fragments.get(bs)[prevIndex].length) {
					if (at.fragments.get(bs)[prevIndex][newIndex] == null) {
						at.fragments.get(bs)[prevIndex][newIndex] = new ArrayList<FragmentImg>();
					}
					at.fragments.get(bs)[prevIndex][newIndex].add(new FragmentImg(xOffset, yOffset, img));
				}/* else {
					AirshipGame.instance.reportError("Bad armour line: " + l, null, null, false, true);
				}*/
			} else if (WHEEL_AND_LEG_PREFIXES.contains(idBits[0])) {
				ModuleType mt = null;
				try {
					mt = ModuleType.ofName(idBits[1]);
				} catch (Exception e) {
					continue;
				}
				if (idBits[0].equals("wheel")) {
					mt.getWheelSpecs().get(0).wheelFrag.add(new FragmentImg(xOffset, yOffset, img));
				} else if (idBits[0].equals("lowerLink")) {
					mt.getWheelSpecs().get(0).lowerLinkFrag.add(new FragmentImg(xOffset, yOffset, img));
				} else if (idBits[0].equals("upperLink")) {
					mt.getWheelSpecs().get(0).upperLinkFrag.add(new FragmentImg(xOffset, yOffset, img));
				} else if (idBits[0].equals("foot")) {
					mt.getLegSpecs().get(0).footFrag.add(new FragmentImg(xOffset, yOffset, img));
				} else if (idBits[0].equals("upperLeg")) {
					mt.getLegSpecs().get(0).upperLegFrag.add(new FragmentImg(xOffset, yOffset, img));
				} else if (idBits[0].equals("lowerLeg")) {
					mt.getLegSpecs().get(0).lowerLegFrag.add(new FragmentImg(xOffset, yOffset, img));
				}
			} else {
				ModuleType mt = null;
				try {
					mt = ModuleType.ofName(idBits[0]);
				} catch (Exception e) {
					continue;
				}
				if (idBits[1].equals("ex")) {
					int exIndex = Integer.parseInt(idBits[2]);
					int frameIndex = Integer.parseInt(idBits[3]);
					BonusSet bs = idBits.length > 4 ? fromToken(idBits[4]) :  BonusSet.empty();
					while (mt.externalFragments.get(bs).size() <= exIndex) {
						mt.externalFragments.get(bs).add(new ArrayList<ArrayList<FragmentImg>>());
					}
					while (mt.externalFragments.get(bs).get(exIndex).size() <= frameIndex) {
						mt.externalFragments.get(bs).get(exIndex).add(new ArrayList<FragmentImg>());
					}
					mt.externalFragments.get(bs).get(exIndex).get(frameIndex).add(new FragmentImg(xOffset, yOffset, img));
				} else {
					boolean wreckageFragment = idBits.length > 2 && idBits[2].equals("wreckage");
					int frameIndex = Integer.parseInt(idBits[1]);
					BonusSet bs = idBits.length > 3 ? fromToken(idBits[3]) :  BonusSet.empty();
					if (wreckageFragment) {
						while (mt.appWreckage.get(bs).size() <= frameIndex) {
							mt.appWreckage.get(bs).add(new ArrayList<FragmentImg>());
						}
						mt.appWreckage.get(bs).get(frameIndex).add(new FragmentImg(xOffset, yOffset, img));
					} else {
						while (mt.appFragments.get(bs).size() <= frameIndex) {
							mt.appFragments.get(bs).add(new ArrayList<FragmentImg>());
						}
						mt.appFragments.get(bs).get(frameIndex).add(new FragmentImg(xOffset, yOffset, img));
					}
				}
			}
		}
		br.close();
	}
	
	private static BonusSet fromToken(String tok) {
		BonusSet bs = new BonusSet();
		for (String name : tok.split(",")) {
			if (hasOfName(Bonus.class, name)) {
				bs.add(Bonus.ofName(name));
			}
		}
		return bs;
	}
}
