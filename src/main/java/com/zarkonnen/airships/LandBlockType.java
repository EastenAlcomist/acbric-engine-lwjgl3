package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class LandBlockType extends Loadable {	
	public final boolean landFormationRemoveStopper;
	public final boolean canPlace;
	public final boolean canPlaceInMissionEditor;
	public final boolean canOverhang;
	public final int addCost;
	public final int removeCost;
	public final boolean solid;
	public final boolean opaque;
	public final double damageMultiplier;
	public final int hp;
	public final int armour;
	public final int weight;
	public final int liftGenerated;
	public final ParticleType destroyparticle;
	public final SoundEffect destroySound;
	public final HashMap<String, Appearance> appByVariant = new HashMap<String, Appearance>();
	public final HashMap<String, Appearance> topAppByVariant = new HashMap<String, Appearance>();
	public final HashMap<String, ExternalApp> externalAppByVariant = new HashMap<String, ExternalApp>();
	public final HashMap<String, ExternalApp> externalTopAppByVariant = new HashMap<String, ExternalApp>();
	public final HashMap<String, ExternalApp> externalOverhangAppByVariant = new HashMap<String, ExternalApp>();
	public final boolean externalDrawPriority;
	public final HashMap<String, Particle.Emitter> particleEmitterByVariant = new HashMap<String, Particle.Emitter>();
	public int currentMappingIndex = -1;
	public final Clr lightClr;
	public final int lightRadius;
	public final double lightX, lightY;
	
	public LandBlockType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		if (o.has("lightClr")) {
			JSONObject l = o.getJSONObject("lightClr");
			lightClr = new Clr(l.getInt("r"), l.getInt("g"), l.getInt("b"));
			lightRadius = o.getInt("lightRadius");
			lightX = o.optDouble("lightX", AGame.SGS / 2);
			lightY = o.optDouble("lightY", AGame.SGS / 2);
		} else {
			lightClr = null;
			lightRadius = 0;
			lightX = 0;
			lightY = 0;
		}
		landFormationRemoveStopper = o.optBoolean("landFormationRemoveStopper");
		canPlace = o.optBoolean("canPlace", false);
		canPlaceInMissionEditor = o.optBoolean("canPlaceInMissionEditor", false);
		canOverhang = o.optBoolean("canOverhang", false);
		removeCost = o.getInt("removeCost");
		addCost = o.getInt("addCost");
		solid = o.optBoolean("solid", true);
		opaque = o.optBoolean("opaque", true);
		damageMultiplier = o.getDouble("damageMultiplier");
		hp = StrictMath.max(0, StrictMath.min(35, o.getInt("hp")));
		armour = o.getInt("armour");
		weight = o.getInt("weight");
		liftGenerated = o.optInt("liftGenerated", 0);
		if (o.has("destroyParticle")) {
			destroyparticle = ParticleType.ofName(o.getString("destroyParticle"));
		} else {
			destroyparticle = null;
		}
		SoundEffect ds = null;
		if (o.has("destroySound")) {
			try {
				ds = new SoundEffect(o.getString("destroySound"), o.optInt("numDestroySounds", 1));
			} catch (Exception e) {
				ds = new SoundEffect(o.getJSONObject("destroySound"));
			}
		}
		destroySound = ds;
		if (o.has("appearance")) {
			appByVariant.put("normal", new Appearance(o.getJSONObject("appearance")));
		}
		if (o.has("topSnowAppearance")) {
			topAppByVariant.put("snow", new Appearance(o.getJSONObject("topSnowAppearance")));
		}
		if (o.has("snowAppearance")) {
			appByVariant.put("snow", new Appearance(o.getJSONObject("snowAppearance")));
		}
		if (o.has("appearances")) {
			JSONObject apps = o.getJSONObject("appearances");
			for (Object k : apps.keySet()) {
				String kk = (String) k;
				appByVariant.put(kk, new Appearance(apps.getJSONObject(kk)));
			}
		}
		if (o.has("particleEmitter")) {
			JSONObject pe = o.getJSONObject("particleEmitter");
			SoundEffect ef = null;
			if (pe.has("sound")) {
				try {
					String sound = pe.getString("sound");
					ef = new SoundEffect(sound, pe.optDouble("volume"));
				} catch (Exception e) {
					ef = new SoundEffect(pe.getJSONObject("sound"));
				}
			}
			particleEmitterByVariant.put("normal", new Particle.Emitter(
					ParticleType.ofName(pe.getString("type")),
					pe.getDouble("emitProbability"),
					pe.optInt("numParticles", 1),
					ef));
		}
		
		if (o.has("particleEmitters")) {
			JSONObject ems = o.getJSONObject("particleEmitters");
			for (Object k : ems.keySet()) {
				String kk = (String) k;
				JSONObject pe = ems.getJSONObject(kk);
				SoundEffect ef = null;
				if (pe.has("sound")) {
					try {
						String sound = pe.getString("sound");
						ef = new SoundEffect(sound, pe.optDouble("volume"));
					} catch (Exception e) {
						ef = new SoundEffect(pe.getJSONObject("sound"));
					}
				}
				particleEmitterByVariant.put(kk, new Particle.Emitter(
						ParticleType.ofName(pe.getString("type")),
						pe.getDouble("emitProbability"),
						pe.optInt("numParticles", 1),
						ef));
			}
		}
		if (o.has("snowParticleEmitter")) {
			JSONObject pe = o.getJSONObject("snowParticleEmitter");
			SoundEffect ef = null;
			if (pe.has("sound")) {
				try {
					String sound = pe.getString("sound");
					ef = new SoundEffect(sound, pe.optDouble("volume"));
				} catch (Exception e) {
					ef = new SoundEffect(pe.getJSONObject("sound"));
				}
			}
			particleEmitterByVariant.put("snow", new Particle.Emitter(
					ParticleType.ofName(pe.getString("type")),
					pe.getDouble("emitProbability"),
					pe.optInt("numParticles", 1),
					ef));
		}
		if (o.has("externalApp")) {
			JSONObject ea = o.getJSONObject("externalApp");
			externalAppByVariant.put("normal", new ExternalApp(
						new Appearance(ea.getJSONObject("appearance")),
						ea.optInt("x"),
						ea.optInt("y")));
		}
		if (o.has("externalSnowApp")) {
			JSONObject ea = o.getJSONObject("externalSnowApp");
			externalAppByVariant.put("snow", new ExternalApp(
						new Appearance(ea.getJSONObject("appearance")),
						ea.optInt("x"),
						ea.optInt("y")));
		}
		if (o.has("externalApps")) {
			JSONObject eas = o.getJSONObject("externalApps");
			for (Object k : eas.keySet()) {
				String kk = (String) k;
				JSONObject ea = eas.getJSONObject(kk);
				externalAppByVariant.put(kk, new ExternalApp(
						new Appearance(ea.getJSONObject("appearance")),
						ea.optInt("x"),
						ea.optInt("y")));
			}
		}
		if (o.has("externalTopSnowApp")) {
			JSONObject ea = o.getJSONObject("externalTopSnowApp");
			externalTopAppByVariant.put("snow", new ExternalApp(
						new Appearance(ea.getJSONObject("appearance")),
						ea.optInt("x"),
						ea.optInt("y")));
		}
		if (o.has("externalTopApps")) {
			JSONObject eas = o.getJSONObject("externalTopApps");
			for (Object k : eas.keySet()) {
				String kk = (String) k;
				JSONObject ea = eas.getJSONObject(kk);
				externalTopAppByVariant.put(kk, new ExternalApp(
						new Appearance(ea.getJSONObject("appearance")),
						ea.optInt("x"),
						ea.optInt("y")));
			}
		}
		if (o.has("externalOverhangApps")) {
			JSONObject eas = o.getJSONObject("externalOverhangApps");
			for (Object k : eas.keySet()) {
				String kk = (String) k;
				JSONObject ea = eas.getJSONObject(kk);
				externalOverhangAppByVariant.put(kk, new ExternalApp(
						new Appearance(ea.getJSONObject("appearance")),
						ea.optInt("x"),
						ea.optInt("y")));
			}
		}
		externalDrawPriority = o.optBoolean("externalDrawPriority", false);
	}

	public String getName() {
		return _t(name);
	}
	
	public static LandBlockType ofName(String name) {
		return ofName(LandBlockType.class, name);
	}
	
	public static HashMap<Integer, LandBlockType> getMapping(JSONObject o) {
		if (o.has("landBlockMapping")) {
			HashMap<Integer, LandBlockType> mapping = new HashMap<Integer, LandBlockType>();
			JSONArray ma = o.getJSONArray("landBlockMapping");
			for (int i = 0; i < ma.length(); i += 2) {
				mapping.put(ma.getInt(i), LandBlockType.ofName(ma.getString(i + 1)));
			}
			return mapping;
		}
		return null;
	}
	
	public static void writeMapping(JSONObject o) {
		JSONArray ma = new JSONArray();
		o.put("landBlockMapping", ma);
		ArrayList<LandBlockType> lbts = all(LandBlockType.class);
		for (int i = 0; i < lbts.size(); i++) {
			ma.put(i);
			ma.put(lbts.get(i).name);
		}
	}
	
	public static final HashMap<Integer, LandBlockType> OLD_MAPPING = new HashMap<Integer, LandBlockType>();
	public static final HashMap<Integer, LandBlockType> V_1_0_10_MAPPING = new HashMap<Integer, LandBlockType>();
	public static final HashMap<Integer, LandBlockType> CURRENT_MAPPING = new HashMap<Integer, LandBlockType>();
	
	public static void postLoad() {
		OLD_MAPPING.clear();
		OLD_MAPPING.put(0, LandBlockType.ofName("AIR"));
		OLD_MAPPING.put(1, LandBlockType.ofName("BEDROCK"));
		OLD_MAPPING.put(2, LandBlockType.ofName("BRANCH"));
		OLD_MAPPING.put(3, LandBlockType.ofName("BUSH"));
		OLD_MAPPING.put(4, LandBlockType.ofName("GRASS"));
		OLD_MAPPING.put(5, LandBlockType.ofName("LEAF"));
		OLD_MAPPING.put(6, LandBlockType.ofName("ROCK"));
		OLD_MAPPING.put(7, LandBlockType.ofName("SOIL"));
		OLD_MAPPING.put(8, LandBlockType.ofName("SUSPENDIUM_ORE"));
		OLD_MAPPING.put(9, LandBlockType.ofName("TRUNK"));
		OLD_MAPPING.put(10, LandBlockType.ofName("TRUNKBRANCH"));
		OLD_MAPPING.put(11, LandBlockType.ofName("CROWN"));
		OLD_MAPPING.put(12, LandBlockType.ofName("FLOATER_BOTTOM"));
		OLD_MAPPING.put(13, LandBlockType.ofName("FLOATER_DOWN"));
		OLD_MAPPING.put(14, LandBlockType.ofName("FLOATER_DOWNER"));
		OLD_MAPPING.put(15, LandBlockType.ofName("FLOATER_DOWNER_ROCK"));
		OLD_MAPPING.put(16, LandBlockType.ofName("FLOATER_DOWNER_SUSPENDIUM"));
		OLD_MAPPING.put(17, LandBlockType.ofName("FLOATER_MIDDLE"));
		OLD_MAPPING.put(18, LandBlockType.ofName("FLOATER_TOP"));
		OLD_MAPPING.put(19, LandBlockType.ofName("FLOATER_TOP_POPPIES"));
		OLD_MAPPING.put(20, LandBlockType.ofName("FLOATER_TOP_SUSPENDIUM"));
		OLD_MAPPING.put(21, LandBlockType.ofName("FLOATER_TOP_VINE"));
		OLD_MAPPING.put(22, LandBlockType.ofName("FLOATER_UP"));
		OLD_MAPPING.put(23, LandBlockType.ofName("GRASS_FENCES"));
		OLD_MAPPING.put(24, LandBlockType.ofName("GRASS_FLOATPLANT"));
		OLD_MAPPING.put(25, LandBlockType.ofName("GRASS_GRAVESTONE"));
		OLD_MAPPING.put(26, LandBlockType.ofName("GRASS_POPPIES"));
		OLD_MAPPING.put(27, LandBlockType.ofName("GRASS_ROCK"));
		OLD_MAPPING.put(28, LandBlockType.ofName("GRASS_RUIN"));
		OLD_MAPPING.put(29, LandBlockType.ofName("GRASS_SIGNPOST"));
		OLD_MAPPING.put(30, LandBlockType.ofName("GRASS_SUSPENDIUM"));
		OLD_MAPPING.put(31, LandBlockType.ofName("GRASS_TROUGH"));
		OLD_MAPPING.put(32, LandBlockType.ofName("GRASS_WOOD_PILE"));
		OLD_MAPPING.put(33, LandBlockType.ofName("ROOTS"));
		OLD_MAPPING.put(34, LandBlockType.ofName("SMALL_CROWN"));
		OLD_MAPPING.put(35, LandBlockType.ofName("THIN_TRUNK"));
		
		V_1_0_10_MAPPING.clear();
		V_1_0_10_MAPPING.put(0, LandBlockType.ofName("AIR"));
		V_1_0_10_MAPPING.put(1, LandBlockType.ofName("GRASS"));
		V_1_0_10_MAPPING.put(2, LandBlockType.ofName("STONE_TOP"));
		V_1_0_10_MAPPING.put(3, LandBlockType.ofName("STONE_MIDDLE"));
		V_1_0_10_MAPPING.put(4, LandBlockType.ofName("SOIL"));
		V_1_0_10_MAPPING.put(5, LandBlockType.ofName("ROCK"));
		V_1_0_10_MAPPING.put(6, LandBlockType.ofName("SUSPENDIUM_ORE"));
		V_1_0_10_MAPPING.put(7, LandBlockType.ofName("BEDROCK"));
		V_1_0_10_MAPPING.put(8, LandBlockType.ofName("BUSH"));
		V_1_0_10_MAPPING.put(9, LandBlockType.ofName("ROOTS"));
		V_1_0_10_MAPPING.put(10, LandBlockType.ofName("TRUNK"));
		V_1_0_10_MAPPING.put(11, LandBlockType.ofName("CROWN"));
		V_1_0_10_MAPPING.put(12, LandBlockType.ofName("THIN_TRUNK"));
		V_1_0_10_MAPPING.put(13, LandBlockType.ofName("SMALL_CROWN"));
		V_1_0_10_MAPPING.put(14, LandBlockType.ofName("TRUNK_BLOCK"));
		V_1_0_10_MAPPING.put(15, LandBlockType.ofName("TRUNKBRANCH"));
		V_1_0_10_MAPPING.put(16, LandBlockType.ofName("BRANCH"));
		V_1_0_10_MAPPING.put(17, LandBlockType.ofName("LEAF"));
		V_1_0_10_MAPPING.put(18, LandBlockType.ofName("FLOATER_TOP"));
		V_1_0_10_MAPPING.put(19, LandBlockType.ofName("FLOATER_TOP_VINE"));
		V_1_0_10_MAPPING.put(20, LandBlockType.ofName("FLOATER_TOP_POPPIES"));
		V_1_0_10_MAPPING.put(21, LandBlockType.ofName("FLOATER_TOP_SUSPENDIUM"));
		V_1_0_10_MAPPING.put(22, LandBlockType.ofName("FLOATER_UP"));
		V_1_0_10_MAPPING.put(23, LandBlockType.ofName("FLOATER_MIDDLE"));
		V_1_0_10_MAPPING.put(24, LandBlockType.ofName("FLOATER_DOWN"));
		V_1_0_10_MAPPING.put(25, LandBlockType.ofName("FLOATER_DOWNER"));
		V_1_0_10_MAPPING.put(26, LandBlockType.ofName("FLOATER_DOWNER_SUSPENDIUM"));
		V_1_0_10_MAPPING.put(27, LandBlockType.ofName("FLOATER_DOWNER_ROCK"));
		V_1_0_10_MAPPING.put(28, LandBlockType.ofName("FLOATER_BOTTOM"));
		V_1_0_10_MAPPING.put(29, LandBlockType.ofName("GRASS_FENCES"));
		V_1_0_10_MAPPING.put(30, LandBlockType.ofName("GRASS_FLOATPLANT"));
		V_1_0_10_MAPPING.put(31, LandBlockType.ofName("GRASS_GRAVESTONE"));
		V_1_0_10_MAPPING.put(32, LandBlockType.ofName("GRASS_POPPIES"));
		V_1_0_10_MAPPING.put(33, LandBlockType.ofName("GRASS_ROCK"));
		V_1_0_10_MAPPING.put(34, LandBlockType.ofName("GRASS_RUIN"));
		V_1_0_10_MAPPING.put(35, LandBlockType.ofName("GRASS_SIGNPOST"));
		V_1_0_10_MAPPING.put(36, LandBlockType.ofName("GRASS_SUSPENDIUM"));
		V_1_0_10_MAPPING.put(37, LandBlockType.ofName("GRASS_TROUGH"));
		V_1_0_10_MAPPING.put(38, LandBlockType.ofName("GRASS_WOOD_PILE"));
		V_1_0_10_MAPPING.put(39, LandBlockType.ofName("STELE_TOP"));
		V_1_0_10_MAPPING.put(40, LandBlockType.ofName("STELE_BODY"));
		
		ArrayList<LandBlockType> lbts = all(LandBlockType.class);
		CURRENT_MAPPING.clear();
		for (int i = 0; i < lbts.size(); i++) {
			CURRENT_MAPPING.put(i, lbts.get(i));
			lbts.get(i).currentMappingIndex = i;
		}
	}
}
