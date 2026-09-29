package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Utils;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MonsterNestType extends Loadable implements HasName {
	private final Img fallbackMapImage = new Img("ui", 384, 368, 16, 16, false);
	private final Img fallbackMapBackground = new Img("ui", 400, 368, 16, 16, false);
	private final Img largeFallbackMapImage = new Img("ui", 512, 224, 32, 32, false);
	private final Img largeFallbackMapBackground = new Img("ui", 544, 224, 32, 32, false);
	
	public final boolean easyStartingNest;
	public final boolean needsRoad;
	private final Img mapImage;
	private final Img mapFleetImage;
	private final Img mapBackground;
	private final Img mapFleetBackground;
	public final BonusableValue<Integer> income;
	private final Img largeMapImage;
	private final Img largeMapFleetImage;
	private final Img largeMapBackground;
	private final Img largeMapFleetBackground;
	public final ArrayList<Airship> baseConstructions;
	public final ArrayList<String> baseConstructionNames;
	public final ArrayList<Airship> additionalConstructions;
	public final ArrayList<String> additionalConstructionNames;
	public final BonusableValue<Integer> spawnWeight;
	public final BonusableValue<Integer> incomeModifierPercentage;
	public final BonusableValue<Integer> incomeModifier;
	public final BonusableValue<Integer> unrest;

	public final ArrayList<Reward> rewards;
	private final Img img;
	public final AIQuality aiQuality;
	public final ArrayList<String> homeFleet;
	public final int minAttackFleetStrength;
	public final int attackRadius;
	public final HeraldicStyle heraldicStyle;
	public final boolean isBiological;
	public final boolean shipsCanJoin;
	public final double repToAttackWeight;
	
	public final int msUntilUpgrade;
	public final int upgradeMsPerRaidSuccess;
	public final int msForUpgrade;
	public final boolean upgradeNearingCompletionMessage;
	public final String upgradeToName;
	public final boolean oneOnly;
	public final boolean respawns;
	
	public final BonusableValue<Integer> minTimeBetweenRaids;
	public final BonusableValue<Integer> minTimeBetweenCityRaids;
	
	public final ArrayList<Utils.Pair<Integer, LandscapeType>> landscapeTypes;
	public ArrayList<Bonus> specialTextWithBonuses = new ArrayList<Bonus>();
	
	public final DifficultyLevel minDifficultyLevel;
	public final DifficultyLevel maxDifficultyLevel;
	
	public final boolean isSpider;
	
	public String bonusSuffix(Empire viewer) {
		if (viewer == null) { return ""; }
		for (int i = 0; i < specialTextWithBonuses.size(); i++) {
			Bonus b = specialTextWithBonuses.get(i);
			if (viewer.bonuses.contains[b.ordinal()]) {
				return "_" + b.name;
			}
		}
		return "";
	}
	
	public MonsterNestType(JSONObject o) {
		super(o.getString("name"));
		easyStartingNest = o.optBoolean("easyStartingNest", false);
		img = new Img(o.optString("img", o.getString("name")));
		needsRoad = o.getBoolean("needsRoad");
		mapImage = o.has("mapImage") ? img(o.getJSONObject("mapImage")) : null;
		mapFleetImage = o.has("mapFleetImage") ? img(o.getJSONObject("mapFleetImage")) : null;
		mapBackground = o.has("mapImage") ? img(o.getJSONObject("mapBackground")) : null;
		mapFleetBackground = o.has("mapFleetImage") ? img(o.getJSONObject("mapFleetBackground")) : null;
		income = BonusableValue.intFromJSON(o, "income", 0);
		spawnWeight = BonusableValue.intFromJSON(o, "spawnWeight", 1);
		largeMapImage = o.has("largeMapImage") ? img(o.getJSONObject("largeMapImage")) : null;
		largeMapFleetImage = o.has("largeMapFleetImage") ? img(o.getJSONObject("largeMapFleetImage")) : null;
		largeMapBackground = o.has("largeMapImage") ? img(o.getJSONObject("largeMapBackground")) : null;
		largeMapFleetBackground = o.has("largeMapFleetImage") ? img(o.getJSONObject("largeMapFleetBackground")) : null;
		incomeModifierPercentage = BonusableValue.intFromJSON(o, "incomeModifierPercentage", 0);
		incomeModifier = BonusableValue.intFromJSON(o, "incomeModifier", 0);
		aiQuality = AIQuality.valueOf(o.optString("aiQuality", AIQuality.NORMAL.name()));
		minAttackFleetStrength = o.optInt("minAttackFleetStrength", 0);
		attackRadius = o.optInt("attackRadius", 0);
		repToAttackWeight = o.optDouble("repToAttackWeight", 0);
		oneOnly = o.optBoolean("oneOnly", false);
		respawns = o.optBoolean("respawns", true);
		isBiological = o.optBoolean("isBiological", false);
		shipsCanJoin = o.optBoolean("shipsCanJoin", false);
		unrest = BonusableValue.intFromJSON(o, "unrest", 10);
		if (o.has("heraldicStyle")) {
			heraldicStyle = HeraldicStyle.ofName(o.getString("heraldicStyle"));
		} else {
			heraldicStyle = null;
		}
		baseConstructions = new ArrayList<Airship>();
		baseConstructionNames = new ArrayList<String>();
		JSONArray a = o.getJSONArray("baseConstructions");
		for (int i = 0; i < a.length(); i++) {
			if (a.getString(i).length() > 0) { // Entirely to fix Cataclystic Expansion.
				baseConstructionNames.add(a.getString(i));
			}
		}
		additionalConstructions = new ArrayList<Airship>();
		additionalConstructionNames = new ArrayList<String>();
		a = o.getJSONArray("additionalConstructions");
		for (int i = 0; i < a.length(); i++) {
			additionalConstructionNames.add(a.getString(i));
		}
		a = o.getJSONArray("rewards");
		rewards = new ArrayList<Reward>();
		for (int i = 0; i < a.length(); i++) {
			rewards.add(new Reward(a.getJSONObject(i), getImg(), this));
		}
		homeFleet = new ArrayList<String>();
		if (o.has("homeFleet")) {
			a = o.getJSONArray("homeFleet");
			for (int i = 0; i < a.length(); i++) {
				homeFleet.add(a.getString(i));
			}
		}
		if (o.has("specialTextWithBonuses")) {
			a = o.getJSONArray("specialTextWithBonuses");
			for (int i = 0; i < a.length(); i++) {
				specialTextWithBonuses.add(Bonus.ofName(a.getString(i)));
			}
		}
		
		msUntilUpgrade = o.optInt("msUntilUpgrade", 240000);
		upgradeMsPerRaidSuccess = o.optInt("upgradeMsPerRaidSuccess", 30000);
		msForUpgrade = o.optInt("msForUpgrade", 60000);
		upgradeToName = o.optString("upgradeTo", null);
		upgradeNearingCompletionMessage = o.optBoolean("upgradeNearingCompletionMessage", false);
		
		minTimeBetweenRaids = BonusableValue.intFromJSON(o, "minTimeBetweenRaids", 120000);
		minTimeBetweenCityRaids = BonusableValue.intFromJSON(o, "minTimeBetweenCityRaids", 240000);
		
		if (o.has("landscapeTypes")) {
			landscapeTypes = new ArrayList<Utils.Pair<Integer, LandscapeType>>();
			a = o.getJSONArray("landscapeTypes");
			for (int i = 0; i < a.length(); i++) {
				landscapeTypes.add(new Pair<Integer, LandscapeType>(a.getJSONObject(i).getInt("spawnWeight"), LandscapeType.ofName(a.getJSONObject(i).getString("name"))));
			}
		} else {
			landscapeTypes = null;
		}
		
		if (o.has("minDifficultyLevel")) {
			minDifficultyLevel = DifficultyLevel.ofName(o.getString("minDifficultyLevel"));
		} else {
			minDifficultyLevel = null;
		}
		if (o.has("maxDifficultyLevel")) {
			maxDifficultyLevel = DifficultyLevel.ofName(o.getString("maxDifficultyLevel"));
		} else {
			maxDifficultyLevel = null;
		}
		
		isSpider = o.optBoolean("isSpider", false);
	}
	
	public LandscapeType getLandscapeType(GuardedRandom r) {
		int total = 0;
		for (Pair<Integer, LandscapeType> p : landscapeTypes) {
			total += p.a;
		}
		int roll = r.nextInt(total);
		for (Pair<Integer, LandscapeType> p : landscapeTypes) {
			roll -= p.a;
			if (roll <= 0) {
				return p.b;
			}
		}
		return landscapeTypes.get(0).b;
	}
	
	public static void postLoad() {
		lp: for (MonsterNestType mnt : all(MonsterNestType.class)) {
			for (String n : mnt.baseConstructionNames) {
				try {
					Airship s = mnt.getShip(n);
					s.repair(/* resetXP */ true);
					mnt.baseConstructions.add(s);
				} catch (Exception e) {
					e.printStackTrace();
					remove(mnt);
					continue lp;
				}
			}
			for (String n : mnt.additionalConstructionNames) {
				try {
					Airship s = mnt.getShip(n);
					s.repair(/* resetXP */ true);
					mnt.additionalConstructions.add(s);
				} catch (Exception e) {
					e.printStackTrace();
					remove(mnt);
					continue lp;
				}
			}
		}
	}
	
	public MonsterNestType upgradeTo() {
		return hasOfName(MonsterNestType.class, upgradeToName) ? ofName(MonsterNestType.class, upgradeToName) : null;
	}
	
	public final Airship getShip(String name) {
		ArrayList<Mod> ms = Mod.getEnabledMods();
		Collections.reverse(ms);
		for (Mod m : ms) {
			File shipF = new File(new File(m.dir, "monsters"), name + ".json");
			if (shipF.exists()) {
				try {
					return new Airship(new JSONObject(FileUtils.readFileToString(shipF, "UTF-8")));
				} catch (Exception e) {
					throw new RuntimeException("Cannot load monster " + name + " from mod " + m.getName() + ".", e);
				}
			}
		}
		File shipF = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "monsters"), name + ".json");
		try {
			return new Airship(new JSONObject(FileUtils.readFileToString(shipF, "UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot load monster " + name + ".", e);
		}
	}
	
	public static MonsterNestType ofName(String name) {
		return ofName(MonsterNestType.class, name);
	}

	@Override
	public String getName() {
		return _t(name + "_displayName");
	}

	public Img getMapImage() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? fallbackMapImage : mapImage;
	}

	public Img getMapFleetImage() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? fallbackMapImage : mapFleetImage;
	}

	public Img getMapBackground() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? fallbackMapBackground : mapBackground;
	}

	public Img getMapFleetBackground() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? fallbackMapBackground : mapFleetBackground;
	}

	public Img getLargeMapImage() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? largeFallbackMapImage : largeMapImage;
	}

	public Img getLargeMapFleetImage() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? largeFallbackMapImage : largeMapFleetImage;
	}

	public Img getLargeMapBackground() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? largeFallbackMapBackground : largeMapBackground;
	}

	public Img getLargeMapFleetBackground() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? largeFallbackMapBackground : largeMapFleetBackground;
	}

	public Img getImg() {
		return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? null : img;
	}
}
