package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class HeroType extends Loadable implements HasName {
	public final Role role;
	public final int singleCombatCost;
	public boolean spawnAtStart;
	public final boolean interesting;
	public final boolean isTemplate;
	public final boolean isStarter;
	public final double templateSpawnPerEmpire;
	public final String templateFirstNameKey, templateLastNameKey;
	public final int templateFirstNameNum, templateLastNameNum;
	public final int minReputation, maxReputation;
	public final ArrayList<Edict> edicts = new ArrayList<Edict>();
	public final ArrayList<CityUpgradeType> instabuild = new ArrayList<CityUpgradeType>();
	public final ArrayList<MonsterNestType> clearableNests = new ArrayList<MonsterNestType>();
	public final int nestClearRep, nestClearMoney, nestClearResearch, nestClearStatChange;
	public final String nestClearStat, nestClearComment;
	public final ArrayList<MonsterNestType> moveableNests = new ArrayList<MonsterNestType>();
	public final int nestMoveRep, nestMoveMoney, nestMoveResearch, nestMoveStatChange;
	public final String nestMoveStat, nestMoveComment;
	public final ArrayList<CombatAbility> combatAbilities = new ArrayList<CombatAbility>();
	public final CrewType aerialAceCrewType;
	public final CrewType airSupportCrewType;
	public final int airSupportNumCrew;
	public final CrewType guardCrewType;
	public final int numGuards;
	public final ArrayList<Tech.Choice> techs = new ArrayList<Tech.Choice>();
	public final Bonus bonus;
	public final ArrayList<Stat> stats = new ArrayList<Stat>();
	public final ArrayList<HeroEvent.Hook> recruitHooks = new ArrayList<HeroEvent.Hook>();
	public final Bonus required;
	public final BonusSet blockers = new BonusSet();
	public final int hireCost;
	public final int maintenance;	
	public final boolean hasHomeCity;
	public final boolean hasNemesisEmpire;
	public final boolean canDismiss;
	public final String turnIntoIfNemesisIsGone;
	public Bonus departureBonus;
	public final Img img;
	private JSONObject origO;
	public TakeoverMethod aiTakeoverMethod;
	
	// Passive governor abilities
	public final int unrest;
	public final int spyDefence;
	public final int productionPercent;
	public final int defenceBudget;
	public final int incomePercent;
	public final int research;
	public final int researchPercent;
	public final int upgradeCostPercent;
	public final int upgradeSpeedPercent;
	public final int airshipSpeedPercent;
	public final int landshipSpeedPercent;
	public final int buildingSpeedPercent;
	
	// Passive captain abilities
	public final int expeditionStrengthPercent;
	public final int fleetSpeedPercent;
	public final Bonus shipBonus;
	public final int fireRatePercent;
	public final int accuracyPercent;
	public final int crewSpeedPercent;
	public final int flammabilityPercent;
	public final int explosionRiskPercent;
	public final int commandCooldownPercent;
	public final int repairAmountPercent;
	public final int firefightAmountPercent;
	public final int propulsionPercent;
	public final int liftPercent;
	public final int armourRepairPercent;
	public final int lootMoneyPercentage;
	public final double scavengeMoneyToSupply;
	public final int experiencePercent;
	public final boolean surpriseAttack;
	
	public final int fleetFireRatePercent;
	public final int fleetAccuracyPercent;
	public final int fleetCrewSpeedPercent;
	public final int fleetFlammabilityPercent;
	public final int fleetExplosionRiskPercent;
	public final int fleetCommandCooldownPercent;
	public final int fleetRepairAmountPercent;
	public final int fleetFirefightAmountPercent;
	
	public String getNestClearDesc(MonsterNestType nt, Hero h, Empire e, CampaignWorld w) {
		StringBuilder sb = new StringBuilder(_t("nest_clear_explanation", nt.getName()));
		if (e != null && e.getMoney() + nestClearMoney < 0) {
			sb.append("\n").append(_t("edict_insufficient_money"));
		}
		sb.append("\n");
		if (nestClearMoney > 0) {
			sb.append("\n").append(_t("edict_money", nestClearMoney));
		}
		if (nestClearMoney < 0) {
			sb.append("\n").append(_t("edict_cost", -nestClearMoney));
		}
		if (nestClearRep != 0) {
			sb.append("\n").append(_t("edict_rep", nestClearRep));
		}
		if (nestClearResearch != 0) {
			sb.append("\n").append(_t("edict_research", nestClearResearch));
		}
		if (h != null) {
			Hero.StatChange sc = getNestClearStatChange(h);
			if (sc != null) {
				sb.append("\n").append(sc.getText(false, w.map));
			}
			sb.append(e.getRepChangeHeroAppendix(nestClearRep, w.map, false));
		} else if (nestClearStat != null) {
			if (nestClearStatChange > 0) {
				sb.append("\n+ ").append(nestClearStatChange).append(" ").append(_t("stat_" + nestClearStat));
			}
			if (nestClearStatChange < 0) {
				sb.append("\n- ").append(-nestClearStatChange).append(" ").append(_t("stat_" + nestClearStat));
			}
		}
		return sb.toString();
	}
	
	private Hero.StatChange getNestClearStatChange(Hero h) {
		if (nestClearStat == null) { return null; }
		HeroType.Stat s = h.type.getStat(nestClearStat);
		if (s == null) { return null; }
		return new Hero.StatChange(h, new HeroType.Stat.Changer(nestClearStatChange, -100, 100, 0, null, s));
	}
	
	public String getNestMoveDesc(MonsterNestType nt, Hero h, Empire e, CampaignWorld w, City c) {
		StringBuilder sb = new StringBuilder(_t("nest_move_explanation", nt.getName()));
		if (e != null && e.getMoney() + nestMoveMoney < 0) {
			sb.append("\n").append(_t("edict_insufficient_money"));
		}
		if (c != null && c.nestMovePair(nt, w.map) == null) {
			sb.append("\n").append(_t("move_no_target"));
		}
		sb.append("\n");
		if (nestMoveMoney > 0) {
			sb.append("\n").append(_t("edict_money", nestMoveMoney));
		}
		if (nestMoveMoney < 0) {
			sb.append("\n").append(_t("edict_cost", -nestMoveMoney));
		}
		if (nestMoveRep != 0) {
			sb.append("\n").append(_t("edict_rep", nestMoveRep));
		}
		if (nestMoveResearch != 0) {
			sb.append("\n").append(_t("edict_research", nestMoveResearch));
		}
		if (h != null) {
			Hero.StatChange sc = getNestMoveStatChange(h);
			if (sc != null) {
				sb.append("\n").append(sc.getText(false, w.map));
			}
			sb.append(e.getRepChangeHeroAppendix(nestMoveRep, w.map, false));
		} else if (nestMoveStat != null) {
			if (nestMoveStatChange > 0) {
				sb.append("\n+ ").append(nestMoveStatChange).append(" ").append(_t("stat_" + nestMoveStat));
			}
			if (nestMoveStatChange < 0) {
				sb.append("\n- ").append(-nestMoveStatChange).append(" ").append(_t("stat_" + nestMoveStat));
			}
		}
		return sb.toString();
	}
	
	private Hero.StatChange getNestMoveStatChange(Hero h) {
		if (nestMoveStat == null) { return null; }
		HeroType.Stat s = h.type.getStat(nestMoveStat);
		if (s == null) { return null; }
		return new Hero.StatChange(h, new HeroType.Stat.Changer(nestMoveStatChange, -100, 100, 0, null, s));
	}
	
	public int getSpeedPercent(ShipType t) {
		switch (t) {
			case AIRSHIP: return airshipSpeedPercent;
			case LANDSHIP: return landshipSpeedPercent;
			case BUILDING: return buildingSpeedPercent;
			default: return 0;
		}
	}
	
	public boolean canWinOrCoronate() {
		for (Stat s : stats) {
			if (s.winOn100 || s.coronationOn100) {
				return true;
			}
		}
		return false;
	}
	
	public HeroType(JSONObject o) {
		super(o.getString("name"));
		role = Role.valueOf(o.getString("role"));
		isTemplate = o.optBoolean("isTemplate", false);
		singleCombatCost = o.optInt("singleCombatCost", 0);
		interesting = o.optBoolean("interesting", false);
		canDismiss = o.optBoolean("canDismiss", true);
		if (isTemplate) {
			isStarter = o.optBoolean("isStarter", false);
			templateFirstNameKey = o.getString("templateFirstNameKey");
			templateLastNameKey = o.getString("templateLastNameKey");
			templateFirstNameNum = o.getInt("templateFirstNameNum");
			templateLastNameNum = o.getInt("templateLastNameNum");
			templateSpawnPerEmpire = o.optDouble("templateSpawnPerEmpire", 0);
		} else {
			isStarter = false;
			templateFirstNameKey = null;
			templateLastNameKey = null;
			templateFirstNameNum = 0;
			templateLastNameNum = 0;
			templateSpawnPerEmpire = 0;
		}
		minReputation = o.optInt("minReputation", 0);
		maxReputation = o.optInt("maxReputation", 100);
		if (o.has("required")) {
			required = Bonus.ofName(o.getString("required"));
		} else {
			required = null;
		}
		if (o.has("blockers")) {
			JSONArray a = o.getJSONArray("blockers");
			for (int i = 0; i < a.length(); i++) {
				blockers.add(Bonus.ofNameOrNone(a.getString(i)));
			}
		}
		img = new Img(o.optString("img", name));
		JSONArray a;
		if (o.has("edicts")) {
			a = o.getJSONArray("edicts");
			for (int i = 0; i < a.length(); i++) {
				edicts.add(Edict.ofName(a.getString(i)));
			}
		}
		if (o.has("instabuild")) {
			a = o.getJSONArray("instabuild");
			for (int i = 0; i < a.length(); i++) {
				instabuild.add(CityUpgradeType.ofName(a.getString(i)));
			}
		}
		if (o.has("combatAbilities")) {
			a = o.getJSONArray("combatAbilities");
			for (int i = 0; i < a.length(); i++) {
				combatAbilities.add(CombatAbility.valueOf(a.getString(i)));
			}
		}
		if (o.has("techs")) {
			a = o.getJSONArray("techs");
			for (int i = 0; i < a.length(); i++) {
				techs.add(Tech.choiceOfName(a.getString(i)));
			}
		}
		if (o.has("bonus")) {
			bonus = Bonus.ofName(o.getString("bonus"));
		} else {
			bonus = null;
		}
		if (o.has("departureBonus")) {
			departureBonus = Bonus.ofName(o.getString("departureBonus"));
		} else {
			departureBonus = null;
		}
		hireCost = o.optInt("hireCost", 0);
		maintenance = o.optInt("maintenance", 0);
		hasHomeCity = o.optBoolean("hasHomeCity", false);
		hasNemesisEmpire = o.optBoolean("hasNemesisEmpire", false);
		turnIntoIfNemesisIsGone = o.optString("turnIntoIfNemesisIsGone", null);
		origO = new JSONObject(o.toString());
		
		unrest = o.optInt("unrest", 0);
		spyDefence = o.optInt("spyDefence", 0);
		productionPercent = o.optInt("productionPercent", 0);
		defenceBudget = o.optInt("defenceBudget", 0);
		incomePercent = o.optInt("incomePercent", 0);
		research = o.optInt("research", 0);
		researchPercent = o.optInt("researchPercent", 0);
		upgradeCostPercent = o.optInt("upgradeCostPercent", 0);
		upgradeSpeedPercent = o.optInt("upgradeSpeedPercent", 0);
		airshipSpeedPercent = o.optInt("airshipSpeedPercent", 0);
		landshipSpeedPercent = o.optInt("landshipSpeedPercent", 0);
		buildingSpeedPercent = o.optInt("buildingSpeedPercent", 0);
		nestClearRep = o.optInt("nestClearRep", 0);
		nestClearMoney = o.optInt("nestClearMoney", 0);
		nestClearResearch = o.optInt("nestClearResearch", 0);
		nestClearStatChange = o.optInt("nestClearStatChange", 0);
		nestClearStat = o.optString("nestClearStat", null);
		nestMoveRep = o.optInt("nestMoveRep", 0);
		nestMoveMoney = o.optInt("nestMoveMoney", 0);
		nestMoveResearch = o.optInt("nestMoveResearch", 0);
		nestMoveStatChange = o.optInt("nestMoveStatChange", 0);
		nestMoveStat = o.optString("nestMoveStat", null);
		nestClearComment = o.optString("nestClearComment", null);
		nestMoveComment = o.optString("nestMoveComment", null);
		if (o.has("clearableNests")) {
			a = o.getJSONArray("clearableNests");
			for (int i = 0; i < a.length(); i++) {
				clearableNests.add(MonsterNestType.ofName(a.getString(i)));
			}
		}
		if (o.has("moveableNests")) {
			a = o.getJSONArray("moveableNests");
			for (int i = 0; i < a.length(); i++) {
				moveableNests.add(MonsterNestType.ofName(a.getString(i)));
			}
		}
		expeditionStrengthPercent = o.optInt("expeditionStrengthPercent", 0);
		fleetSpeedPercent = o.optInt("fleetSpeedPercent", 0);
		if (o.has("shipBonus")) {
			shipBonus = Bonus.ofName(o.getString("shipBonus"));
		} else {
			shipBonus = null;
		}
		fireRatePercent = o.optInt("fireRatePercent", 0);
		accuracyPercent = o.optInt("accuracyPercent", 0);
		crewSpeedPercent = o.optInt("crewSpeedPercent", 0);
		flammabilityPercent = o.optInt("flammabilityPercent", 0);
		explosionRiskPercent = o.optInt("explosionRiskPercent", 0);
		commandCooldownPercent = o.optInt("commandCooldownPercent", 0);
		repairAmountPercent = o.optInt("repairAmountPercent", 0);
		firefightAmountPercent = o.optInt("firefightAmountPercent", 0);
		propulsionPercent = o.optInt("propulsionPercent", 0);
		liftPercent = o.optInt("liftPercent", 0);
		armourRepairPercent = o.optInt("armourRepairPercent", 0);
		experiencePercent = o.optInt("experiencePercent", 0);
		surpriseAttack = o.optBoolean("surpriseAttack", false);
		
		fleetFireRatePercent = o.optInt("fleetFireRatePercent", 0);
		fleetAccuracyPercent = o.optInt("fleetAccuracyPercent", 0);
		fleetCrewSpeedPercent = o.optInt("fleetCrewSpeedPercent", 0);
		fleetFlammabilityPercent = o.optInt("fleetFlammabilityPercent", 0);
		fleetExplosionRiskPercent = o.optInt("fleetExplosionRiskPercent", 0);
		fleetCommandCooldownPercent = o.optInt("fleetCommandCooldownPercent", 0);
		fleetRepairAmountPercent = o.optInt("fleetRepairAmountPercent", 0);
		fleetFirefightAmountPercent = o.optInt("fleetFirefightAmountPercent", 0);
		
		if (o.has("guardCrewType")) {
			guardCrewType = CrewType.ofName(o.getString("guardCrewType"));
			numGuards = o.getInt("numGuards");
		} else {
			guardCrewType = null;
			numGuards = 0;
		}
		if (combatAbilities.contains(CombatAbility.AERIAL_ACE)) {
			aerialAceCrewType = CrewType.ofName(o.getString("aerialAceCrewType"));
		} else {
			aerialAceCrewType = null;
		}
		if (combatAbilities.contains(CombatAbility.AIR_SUPPORT)) {
			airSupportCrewType = CrewType.ofName(o.getString("airSupportCrewType"));
			airSupportNumCrew = o.getInt("airSupportNumCrew");
		} else {
			airSupportCrewType = null;
			airSupportNumCrew = 0;
		}
		lootMoneyPercentage = o.optInt("lootMoneyPercentage", 0);
		scavengeMoneyToSupply = o.optDouble("scavengeMoneyToSupply", 0);
		
		aiTakeoverMethod = o.has("aiTakeoverMethod") ? TakeoverMethod.ofName(o.getString("aiTakeoverMethod")) : null;
	}
	
	private void finish() {
		try {
			if (origO.has("stats")) {
				JSONArray a = origO.getJSONArray("stats");
				for (int i = 0; i < a.length(); i++) {
					stats.add(new Stat(a.getJSONObject(i)));
				}
			}
			if (origO.has("recruitHooks")) {
				JSONArray  a = origO.getJSONArray("recruitHooks");
				for (int i = 0; i < a.length(); i++) {
					recruitHooks.add(HeroEvent.hookFromJSON(a.getJSONObject(i)));
				}
			}
			spawnAtStart = origO.optBoolean("spawnAtStart", !recruitHooks.isEmpty());
			origO = null;
		} catch (Exception e) {
			String msg = "Unable to finish loading HeroType " + name + ": " + e.getMessage();
			if (sourceMod != null) {
				sourceMod.buildFailed = true;
				sourceMod.buildLog = sourceMod.buildLog == null ? msg : sourceMod.buildLog + "\n[bb421d]" + msg;
			}
			throw new RuntimeException(msg, e);
		}
	}
	
	public static void postLoad() {
		for (HeroType t : all(HeroType.class)) { t.finish(); }
	}

	public Stat getStat(String name) {
		for (Stat s : stats) {
			if (s.name.equals(name)) { return s; }
		}
		return null;
	}
	
	public static enum Role { CAPTAIN, GOVERNOR }
	public static enum CombatAbility {
		SMOKESCREEN("S", new Img("heroes", 0, 32, 16, 16, false)),
		FLANK("F", new Img("heroes", 0, 64, 16, 16, false)),
		IMPROVISE_MUNITIONS("M", new Img("ui", 176, 416, 16, 16, false)),
		SCAVENGE_MATERIALS("R", new Img("ui", 48, 416, 16, 16, false)),
		SCAVENGE_FUEL("F", new Img("ui", 192, 416, 16, 16, false)),
		ENGINEERING_MIRACLE("E", new Img("ui", 368, 384, 16, 16, false)),
		NECROMANTIC_INCANTATION("N", new Img("heroes", 16, 80, 16, 16, false)),
		EXTINGUISH("X", new Img("heroes", 80, 80, 16, 16, false)),
		BURST_OF_SPEED("B", new Img("ui", 0, 416, 16, 16, false)),
		SUPERCHARGE_SUSPENDIUM("U", new Img("ui", 496, 384, 16, 16, false)),
		FEAR("5", new Img("ui", 224, 368, 16, 16, false)),
		DOUBLE_TIME("D", new Img("ui", 128, 512, 16, 16, false)),
		TAUNT("T", new Img("ui", 256, 368, 16, 16, false)),
		BLINDING_GLIMMER("G", new Img("heroes", 16, 64, 16, 16, false)),
		CROSSWINDS("C", new Img("heroes", 64, 64, 16, 16, false)),
		CRIPPLING_SHOT("I", new Img("heroes", 48, 64, 16, 16, false)),
		DISARMING_SHOT("J", new Img("heroes", 112, 64, 16, 16, false)),
		PARALYSIS("P", new Img("heroes", 112, 80, 16, 16, false)),
		TURNABOUT("V", new Img("ui", 304, 512, 16, 16, false)),
		GUST_OF_WIND("W", new Img("ui", 272, 416, 16, 16, false)),		
		AERIAL_ACE("L", new Img("ui", 400, 384, 16, 16, false)),
		AIR_SUPPORT("A", new Img("ui", 464, 400, 16, 16, false)),
		PERSONAL_GUARD("3", new Img("ui", 528, 384, 16, 16, false)),
		LAST_STAND("4", new Img("ui", 304, 384, 16, 16, false)),
		SINKHOLE("K", new Img("heroes", 0, 80, 16, 16, false)),
		SUDDEN_STORM("O", new Img("heroes", 32, 112, 16, 16, false)),
		MOMENT_OF_DOUBT("2", new Img("heroes", 48, 112, 16, 16, false)),
		HYSTERICAL_BLINDNESS("Y", new Img("heroes", 64, 112, 16, 16, false)),
		EARTHQUAKE("Q", new Img("heroes", 80, 112, 16, 16, false)),
		CRASH_ZONE("Z", new Img("heroes", 96, 112, 16, 16, false)),
		EMERGENCY_ORDERS("1", new Img("heroes", 112, 112, 16, 16, false)),
		HIGH_STORM("H", new Img("heroes", 112, 96, 16, 16, false)),
		;

		private CombatAbility(String shortcut, Img icon) {
			this.icon = icon;
			this.shortcut = shortcut;
		}
		
		public final String shortcut;
		public final Img icon;
		public String getName() { return _t("ability_" + name()); }
		public String getDesc(HeroType ht) {
			return _t("ability_desc_" + name(),
					ht.aerialAceCrewType == null ? "" : ht.aerialAceCrewType.getName(),
					ht.airSupportNumCrew,
					ht.airSupportCrewType == null ? "" : ht.airSupportCrewType.getPlural(),
					ht.numGuards,
					ht.guardCrewType == null ? "" : ht.guardCrewType.getPlural());
		}
	}
	
	public static class Stat {
		public final String name;
		public final int startingValue;
		public final ArrayList<Changer> changers = new ArrayList<Changer>();
		public final boolean leaveOn0;
		public final boolean leaveOn100;
		public final boolean dieOn0;
		public final boolean dieOn100;
		public final String evolveOn0;
		public final String evolveOn100;
		public final boolean winOn100;
		public final boolean coronationOn100;
		public final boolean loseOn0;
		public final boolean loseOn100;
		public final String victoryMessage, defeatMessage, victoryImage, defeatImage;
		
		public String getName() {
			return _t("stat_" + name);
		}
		
		public static class Changer {
			private final int amount;
			private final int minimum;
			private final int maximum;
			private final int divByNumCities;
			public final HeroEvent.Hook hook;
			public final Stat stat;
			public Changer(int amt, int minimum, int maximum, int divByNumCities, HeroEvent.Hook hook, Stat stat) {
				this.amount = amt;
				this.minimum = minimum;
				this.maximum = maximum;
				this.divByNumCities = divByNumCities;
				this.hook = hook;
				this.stat = stat;
			}

			public int getAmount(WorldMap wm) {
				if (wm == null) { return StrictMath.max(minimum, StrictMath.min(maximum, amount + divByNumCities / 12)); }
				return StrictMath.max(minimum, StrictMath.min(maximum, amount + divByNumCities / wm.size.empires));
			}
		}
		
		public Stat(JSONObject o) {
			name = o.getString("name");
			startingValue = o.getInt("startingValue");
			leaveOn0 = o.optBoolean("leaveOn0", false);
			leaveOn100 = o.optBoolean("leaveOn100", false);
			dieOn0 = o.optBoolean("dieOn0", false);
			dieOn100 = o.optBoolean("dieOn100", false);
			evolveOn0 = o.optString("evolveOn0", null);
			evolveOn100 = o.optString("evolveOn100", null);
			winOn100 = o.optBoolean("winOn100", false);
			coronationOn100 = o.optBoolean("coronationOn100", false);
			loseOn0 = o.optBoolean("loseOn0", false);
			loseOn100 = o.optBoolean("loseOn100", false);
			victoryMessage = o.optString("victoryMessage", null);
			defeatMessage = o.optString("defeatMessage", null);
			victoryImage = o.optString("victoryImage", null);
			defeatImage = o.optString("defeatImage", null);
			JSONArray a = o.getJSONArray("changers");
			for (int i = 0; i < a.length(); i++) {
				JSONObject co = a.getJSONObject(i);
				changers.add(new Changer(co.getInt("change"), co.optInt("changeMin", -100), co.optInt("changeMax", 100), co.optInt("changeDivByCities", 0), HeroEvent.hookFromJSON(co), this));
			}
		}
	}
	
	public static HeroType ofName(String name) {
		return ofName(HeroType.class, name);
	}
	
	@Override
	public String getName() {
		return _t(name);
	}
}
