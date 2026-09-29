package com.zarkonnen.airships;

import static com.zarkonnen.airships.Combat.COMBAT_AREA_W_NEW;
import static com.zarkonnen.airships.Combat.COMBAT_AREA_W_OLD;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public class City extends MapLocation {
	public static final int ECON_DMG_MALUS = 20;
	public static final int MAX_ECON_DAMAGES = 5;
	public static final int LOOT_PER_ECON_DAMAGE = 650;
	public static final int STRIKE_LENGTH = 400 * 28 * 3;
	
	public String name;
	public int income;
	public PlagueLevel plagueLevel;
	public int plagueLevelAge;
	private int economicDamage;
	public int econDmgRecovery;
	public int appearance;
	public int takeoverAmount;
	public boolean isTown;
	public TakeoverMethod takeoverMethod;
	public Empire previousOwner;
	public boolean takeoverNeeded = false;
	public boolean constructionTargetForcedToBeShip;
	public Empire originalEmpire;
	public CoatOfArms originalArms;
	public int postRaidCooldown, postRaidCooldownInitial, fomentUnrestCooldown, fomentUnrestCooldownInitial, postTakeoverCooldown;
	public TakeoverMethod postTakeoverMethod;
	public int strike;
	public ArrayList<CityUpgradeType> upgrades = new ArrayList<CityUpgradeType>();
	public ArrayList<CityUpgradeType> upgradesEverBuilt = new ArrayList<CityUpgradeType>();
	public int specialX = -1, specialY = -1;
	
	public boolean hadScandal;
	
	public transient boolean probablyCoastal;
	
	public boolean coronation;
	public int prevCoronationSabotages;
	public int coronationProgress;
	public boolean coronationStartedSent;
	public boolean coronationWarningSent;
	public transient boolean sendCoronationCancellationMessage;
	public transient Empire coronationFailedDueToCityLostEmpire;
	public transient boolean coronationCancelledIntentionally;

	public boolean isRitualSite;
	public boolean finalRitual;
	public int prevFinalRitualSabotages;
	public int finalRitualProgress;
	public boolean finalRitualStartedSent;
	public boolean finalRitualWarningSent;
	
	public transient ArrayList<MonsterNest> nestsInTerritory = null;
	public transient ShapeUtils.TrianglesArea territoryPolygon;
	private transient HashMap<City, Boolean> adj = new HashMap<City, Boolean>();
	public transient Empire ownerCache;
	
	public Edict edict;
	public int edictTimeLeft;
	
	public ArrayList<UnrestSource> unrests = new ArrayList<UnrestSource>();
	
	public transient Hero governor; // Managed by Hero
	
	public static class UnrestSource {
		public int unrest;
		public int timeout;
		public String reason;

		public UnrestSource(int unrest, int timeout, String reason) {
			this.unrest = unrest;
			this.timeout = timeout;
			this.reason = reason;
		}
		
		public boolean tick(int ms) {
			timeout -= ms;
			return timeout <= 0;
		}
		
		public JSONObject toJSON() {
			return new JSONObject().put("unrest", unrest).put("timeout", timeout).put("reason", reason);
		}
		
		public UnrestSource(JSONObject o) {
			unrest = o.getInt("unrest");
			timeout = o.getInt("timeout");
			reason = o.getString("reason");
		}
	}
	
	public City(int id, int x, int y, String name, boolean isTown, int income, CoatOfArms originalArms, int appearance) {
		super(id, x, y);
		this.name = name;
		this.isTown = isTown;
		this.income = income;
		this.originalArms = originalArms;
		this.appearance = appearance;
	}
	
	public void startCoronation() {
		if (coronation) { return; }
		coronation = true;
		coronationProgress = 0;
		coronationWarningSent = false;
		sendCoronationCancellationMessage = false;
		coronationStartedSent = false;
	}
	
	public void cancelCoronation(Empire coronationFailedDueToCityLostEmpire, boolean coronationCancelledIntentionally) {
		if (!coronation) { return; }
		coronation = false;
		coronationProgress = 0;
		coronationWarningSent = false;
		sendCoronationCancellationMessage = true;
		coronationStartedSent = false;
		prevCoronationSabotages = 0;
		this.coronationFailedDueToCityLostEmpire = coronationFailedDueToCityLostEmpire;
		this.coronationCancelledIntentionally = coronationCancelledIntentionally;
	}
	
	public void startFinalRitual() {
		if (finalRitual) { return; }
		finalRitual = true;
		finalRitualProgress = 0;
		finalRitualWarningSent = false;
		finalRitualStartedSent = false;
	}
	
	@Override
	public String getDisplayName() {
		return name;
	}

	public City(JSONObject o, HashMap<Integer, LandBlockType>[] mappingRef, InPipe ip, BonusSet ownerBonuses) throws IOException {
		super(o, mappingRef, ip, ownerBonuses);
		name = o.getString("name");
		income = o.getInt("income");
		takeoverAmount = o.getInt("takeoverAmount");
		takeoverNeeded = o.getBoolean("takeoverNeeded");
		economicDamage = o.optInt("economicDamage", 0);
		econDmgRecovery = o.optInt("econDmgRecovery", 0);
		appearance = o.optInt("appearance", AGame.ANIM_R.nextInt(1000));
		coronation = o.optBoolean("coronation", false);
		coronationProgress = o.optInt("coronationProgress", 0);
		coronationWarningSent = o.optBoolean("coronationWarningSent", false);
		coronationStartedSent = o.optBoolean("coronationStartedSent", false);
		prevCoronationSabotages = o.optInt("prevCoronationSabotages", 0);
		isTown = o.optBoolean("isTown", false);
		postRaidCooldown = o.optInt("postAttackCooldown", 0);
		postRaidCooldownInitial = o.optInt("postAttackCooldownInitial", 0);
		fomentUnrestCooldown = o.optInt("fomentUnrestCooldown", 0);
		fomentUnrestCooldownInitial = o.optInt("fomentUnrestCooldownInitial", 0);
		postTakeoverCooldown = o.optInt("postTakeoverCooldown", 0);
		plagueLevelAge = o.optInt("plagueLevelAge", 0);
		plagueLevel = o.has("plagueLevel") ? PlagueLevel.ofName(o.getString("plagueLevel")) : null;
		strike = o.optInt("strike", 0);
		isRitualSite = o.optBoolean("isRitualSite", false);
		finalRitual = o.optBoolean("finalRitual", false);
		finalRitualProgress = o.optInt("finalRitualProgress", 0);
		finalRitualWarningSent = o.optBoolean("finalRitualWarningSent", false);
		finalRitualStartedSent = o.optBoolean("finalRitualStartedSent", false);
		prevFinalRitualSabotages = o.optInt("prevFinalRitualSabotages", 0);
		specialX = o.optInt("specialX", -1);
		specialY = o.optInt("specialY", -1);
		hadScandal = o.optBoolean("hadScandal", false);
		if (o.has("postTakeoverMethod")) {
			postTakeoverMethod = TakeoverMethod.ofName(o.getString("postTakeoverMethod"));
		}
		if (o.has("takeoverMethod")) {
			takeoverMethod = TakeoverMethod.ofName(o.getString("takeoverMethod"));
		}
		
		constructionTargetForcedToBeShip = o.optBoolean("constructionTargetForcedToBeShip", false);
		
		if (o.has("originalArms")) {
			try {
				originalArms = new CoatOfArms(o.getJSONObject("originalArms"));
			} catch (Exception e) {
				originalArms = CoatOfArms.getRandom(new GuardedRandom(name.hashCode()), HeraldicStyle.ofName("city"));
			}
		} else {
			originalArms = CoatOfArms.getRandom(new GuardedRandom(name.hashCode()), HeraldicStyle.ofName("city"));
		}
		
		if (o.has("upgrades")) {
			JSONArray a = o.getJSONArray("upgrades");
			for (int i = 0; i < a.length(); i++) {
				upgrades.add(CityUpgradeType.ofName(a.getString(i)));
			}
		}
		if (o.has("upgradesEverBuilt")) {
			JSONArray a = o.getJSONArray("upgradesEverBuilt");
			for (int i = 0; i < a.length(); i++) {
				upgradesEverBuilt.add(CityUpgradeType.ofName(a.getString(i)));
			}
		}
		if (o.has("edict")) {
			edict = Edict.ofName(o.getString("edict"));
			edictTimeLeft = o.getInt("edictTimeLeft");
		}
		if (o.has("unrests")) {
			JSONArray a = o.getJSONArray("unrests");
			for (int i = 0; i < a.length(); i++) {
				unrests.add(new UnrestSource(a.getJSONObject(i)));
			}
		}
	}
	
	public void finish(JSONObject o, WorldMap wm) {
		if (o.has("originalEmpire") && o.getInt("originalEmpire") != -1) {
			originalEmpire = wm.empires.get(o.getInt("originalEmpire"));
		}
		if (o.has("previousOwner") && o.getInt("previousOwner") != -1) {
			previousOwner = wm.empires.get(o.getInt("previousOwner"));
		}
	}

	public JSONObject toJSON(WorldMap wm, OutPipe op) {
		JSONObject o = super.toJSON(op)
				.put("name", name)
				.put("income", income)
				.put("isTown", isTown)
				.put("takeoverAmount", takeoverAmount)
				.put("constructionTargetForcedToBeShip", constructionTargetForcedToBeShip)
				.put("takeoverNeeded", takeoverNeeded)
				.put("economicDamage", getEconomicDamage())
				.put("econDmgRecovery", econDmgRecovery)
				.put("appearance", appearance)
				.put("postAttackCooldown", postRaidCooldown)
				.put("postAttackCooldownInitial", postRaidCooldownInitial)
				.put("fomentUnrestCooldown", fomentUnrestCooldown)
				.put("fomentUnrestCooldownInitial", fomentUnrestCooldownInitial)
				.put("postTakeoverCooldown", postTakeoverCooldown)
				.put("strike", strike)
				.put("coronation", coronation)
				.put("coronationProgress", coronationProgress)
				.put("coronationWarningSent", coronationWarningSent)
				.put("coronationStartedSent", coronationStartedSent)
				.put("prevCoronationSabotages", prevCoronationSabotages)
				.put("isRitualSite", isRitualSite)
				.put("finalRitual", finalRitual)
				.put("finalRitualProgress", finalRitualProgress)
				.put("finalRitualWarningSent", finalRitualWarningSent)
				.put("finalRitualStartedSent", finalRitualStartedSent)
				.put("prevFinalRitualSabotages", prevFinalRitualSabotages)
				.put("specialX", specialX).put("specialY", specialY)
				.put("hadScandal", hadScandal);
		if (plagueLevel != null) {
			o.put("plagueLevel", plagueLevel.name);
			o.put("plagueLevelAge", plagueLevelAge);
		}
		if (takeoverMethod != null) {
			o.put("takeoverMethod", takeoverMethod.name);
		}
		if (postTakeoverMethod != null) {
			o.put("postTakeoverMethod", postTakeoverMethod.name);
		}
		if (originalEmpire != null && wm.empires.contains(originalEmpire)) {
			o.put("originalEmpire", wm.empires.indexOf(originalEmpire));
		}
		if (previousOwner != null && wm.empires.contains(previousOwner)) {
			o.put("previousOwner", wm.empires.indexOf(previousOwner));
		}
		o.put("originalArms", originalArms.toJSON());
		JSONArray a = new JSONArray();
		for (CityUpgradeType cut : upgrades) {
			a.put(cut.name);
		}
		o.put("upgrades", a);
		a = new JSONArray();
		for (CityUpgradeType cut : upgradesEverBuilt) {
			a.put(cut.name);
		}
		o.put("upgradesEverBuilt", a);
		if (edict != null) {
			o.put("edict", edict.name);
			o.put("edictTimeLeft", edictTimeLeft);
		}
		if (!unrests.isEmpty()) {
			a = new JSONArray();
			for (UnrestSource u : unrests) {
				a.put(u.toJSON());
			}
			o.put("unrests", a);
		}
		return o;
	}
	
	public boolean canBuildShips() {
		if (!isTown) { return true; }
		for (int i = 0; i < upgrades.size(); i++) {
			if (upgrades.get(i).enableShipBuilding) { return true; }
		}
		return false;
	}
	
	public boolean isAdjacentTo(City c2, WorldMap wm) {
		return wm.connectedDirectlyByLandOrSea(this, c2) || isPhysicallyAdjacentTo(c2, wm);
	}
	
	public boolean isPhysicallyAdjacentTo(City c2, WorldMap wm) {
		if (!adj.containsKey(c2)) {
			boolean isAdj = false;
			lp: for (int y = 0; y < wm.cityOwnership.length; y++) {
				for (int x = 0; x < wm.cityOwnership[y].length; x++) {
					if (wm.cityOwnership[y][x] == id) {
						for (int yy = Math.max(0, y - 1); yy < Math.min(wm.cityOwnership.length, y + 2); yy++) {
							for (int xx = Math.max(0, x - 1); xx < Math.min(wm.cityOwnership[y].length, x + 2); xx++) {
								if (wm.cityOwnership[yy][xx] == c2.id) {
									isAdj = true;
									break lp;
								}
							}
						}
					}
				}
			}
			adj.put(c2, isAdj);
		}
		return adj.get(c2);
	}
	
	public boolean isConnectedToCapital(Empire player, WorldMap wm) {
		return wm.isConnectedToCapital(player, this);
	}

	public int adjustedIncome(WorldMap wm, StringBuilder explain) {
		Empire owner = wm.owner(this);
		int in = income; // Base
		if (explain != null) { explain.append(_t("Base_Income_")).append(income); }

		if (!takeoverNeeded && takeoverMethod == null) {
			for (int i = 0; i < upgrades.size(); i++) {
				CityUpgradeType u = upgrades.get(i);
				int ui = u.income.get(owner.bonuses);
				if (ui != 0) {
					if (explain != null) { explain.append("\n").append(u.getName()).append(ui > 0 ? ": +" : ": ").append(ui); }
					in += ui;
				}
			}
			for (int i = 0; i < defences.size(); i++) {
				int maintenance = defences.get(i).maintenanceCost();
				if (maintenance < 0) {
					in -= maintenance;
					if (explain != null) {
						explain.append("\n").append(defences.get(i).getName()).append(": +").append(-maintenance);
					}
				}
			}
		}
		
		ArrayList<MonsterNest> nests = nestsInTerritory(wm);
		for (int i = 0; i < nests.size(); i++) {
			MonsterNest n = nests.get(i);
			if (n.type != null) {
				in += n.type.incomeModifier.get(owner.bonuses);
				if (n.type.incomeModifier.get(owner.bonuses) != 0 && explain != null) {
					explain.append("\n").append(n.getDisplayName()).append(n.type.incomeModifier.get(owner.bonuses) > 0 ? ": +" : ": ").append(n.type.incomeModifier.get(owner.bonuses));
				}
			}
		}
		int mult = 100;
		
		if (plagueLevel != null && plagueLevel.incomeReduction != 0) {
			if (explain != null) { explain.append("\n").append(_t("Plague_Income_")).append("-").append(plagueLevel.incomeReduction).append("%"); }
			mult -= plagueLevel.incomeReduction;
		}
		
		int fromUnrest = unrest(wm, null);
		if (fromUnrest > 0) {
			if (explain != null) { explain.append("\n").append(_t("Unrest_Income_")).append("-").append(fromUnrest).append("%"); }
			mult -= fromUnrest;
		}
		
		if (economicDamage > 0) {
			if (explain != null) { explain.append("\n").append(_t("Econ_Damage_Income_")).append("-").append(economicDamage * ECON_DMG_MALUS).append("%"); }
			mult -= economicDamage * ECON_DMG_MALUS;
		}
		
		for (int i = 0; i < nests.size(); i++) {
			MonsterNest n = nests.get(i);
			if (n.type != null && n.type.incomeModifierPercentage.get(owner.bonuses) != 0) {
				mult += n.type.incomeModifierPercentage.get(owner.bonuses);
				if (explain != null) { explain.append("\n").append(n.getDisplayName()).append(n.type.incomeModifier.get(owner.bonuses) > 0 ? ": +" : ": ").append(n.type.incomeModifierPercentage.get(owner.bonuses)).append("%"); }
			}
		}
		
		if (takeoverMethod != null && takeoverMethod.incomeMultiplier.get(owner.bonuses) != 1) {
			mult += (int) ((takeoverMethod.incomeMultiplier.get(owner.bonuses) * 100));
			if (explain != null) {
				explain.append("\n").append(takeoverMethod.getName()).append(" x").append(takeoverMethod.incomeMultiplier.explain(owner.bonuses));
			}
		}
		
		int fromBonuses = EmpireStat.CITY_INCOME_PERCENTAGE_BONUS.get(owner.bonuses);
		if (fromBonuses != 0) {
			mult += fromBonuses;
			if (explain != null) { explain.append("\n").append(_t("Bonus_Income_")).append(fromBonuses > 0 ? "+" : "").append(fromBonuses).append("%"); }
		}
		
		Hero h = Hero.get(this, wm);
		if (h != null && h.type.incomePercent != 0) {
			mult += h.type.incomePercent;
			if (explain != null) { explain.append("\n").append(h.getName()).append(": ").append(h.type.incomePercent > 0 ? "+" : "").append(h.type.incomePercent).append("%"); }
		}
		
		if (edict != null && edict.incomePercent != 0) {
			mult += edict.incomePercent;
			if (explain != null) { explain.append("\n").append(edict.getName()).append(": ").append(edict.incomePercent > 0 ? "+" : "").append(edict.incomePercent).append("%"); }
		}
		
		int total = StrictMath.max(0, in * mult / 100);
		
		if (explain != null) { explain.append("\n").append(_t("budget_total")).append(": ").append(total); }
		
		return total;
	}
	
	public ArrayList<MonsterNest> nestsInTerritory(WorldMap wm) {
		if (nestsInTerritory == null) {
			nestsInTerritory = new ArrayList<MonsterNest>();
			for (MonsterNest n : wm.nests) {
				if (wm.cityOwnership[n.y][n.x] == id) {
					nestsInTerritory.add(n);
				}
			}
		}
		return nestsInTerritory;
	}
	
	public int unrest(WorldMap wm, StringBuilder explain) {
		return unrest(wm, explain, true);
	}
	
	public int unrest(WorldMap wm, StringBuilder explain, boolean max0) {
		Empire owner = wm.owner(this);
		BonusSet bs = owner.bonuses;
		int u = isTown ? EmpireStat.TOWN_BASE_UNREST.get(bs) : EmpireStat.CITY_BASE_UNREST.get(bs);
		if (owner.playerControlled) {
			u += wm.difficulty.playerUnrestModifier;
		}
		if (explain != null) { explain.append(_t("unrest_explanation")).append("\n\n").append(_t("Base_Unrest_")).append(u); }
		Hero h = Hero.get(this, wm);
		if (h != null && h.type.unrest != 0) {
			u += h.type.unrest;
			if (explain != null) { explain.append("\n").append(h.getName()).append(h.type.unrest > 0 ? ": +" : ": ").append(h.type.unrest); }
		}
		if (edict != null && edict.unrest != 0) {
			u += edict.unrest;
			if (explain != null) { explain.append("\n").append(edict.getName()).append(edict.unrest > 0 ? ": +" : ": ").append(edict.unrest); }
		}
		if (plagueLevel != null && plagueLevel.unrest != 0) {
			int v = plagueLevel.unrest;
			u += v;
			if (explain != null) { explain.append("\n").append(_t("Plague_Unrest_")).append("+").append(v); }
		}
		if (takeoverNeeded) {
			int v = EmpireStat.NO_TAKEOVER_SELECTED_UNREST.get(bs);
			u += v;
			if (explain != null) { explain.append("\n").append(_t("No_Takeover_Selected_Unrest_")).append("+").append(v); }
		}
		if (postTakeoverCooldown > 0) {
			int v = postTakeoverMethod.unrest.get(bs);
			u += v;
			if (explain != null && v != 0) {
				explain.append("\n").append(_t(postTakeoverMethod.name + "_Takeover_Unrest_")).append("+").append(postTakeoverMethod.unrest.explain(bs)).append(" ").append(_t("x_time_left", WorldMap.describeTime(postTakeoverCooldown, bs)));
			}
		}
		if (takeoverMethod != null) {
			int v = takeoverMethod.unrest.get(bs);
			u += v;
			if (explain != null && v != 0) {
				explain.append("\n").append(_t(takeoverMethod.name + "_Takeover_Unrest_")).append("+").append(takeoverMethod.unrest.explain(bs));
			}
		}
		if (strike > 0) {
			int v = EmpireStat.STRIKE_UNREST.get(bs);
			u += v;
			if (explain != null) { explain.append("\n").append(_t("strike")).append(": +").append(v); }
		}
		if (wm.toggles.contains(ConquestToggle.REPUTATION)) {
			int v = EmpireStat.CITY_REP_UNREST.get(bs);
			u += v;
			if (explain != null && v != 0) { explain.append("\n").append(_t("Reputation")).append(v > 0 ? ": +" : ": ").append(v); }
		}

		for (MonsterNest n : nestsInTerritory(wm)) {
			if (n.type != null) {
				int v = n.type.unrest.get(owner.bonuses);
				u += v;
				if (explain != null) { explain.append("\n").append(n.getDisplayName()).append(": +").append(v); }
			}
		}
		if (postRaidCooldown > 0) {
			int v = EmpireStat.POST_RAID_UNREST.get(bs);
			u += v;
			if (explain != null) { explain.append("\n").append(_t("Post_Raid_Unrest_")).append("+").append(v).append(" ").append(_t("x_time_left", WorldMap.describeTime(postRaidCooldown, bs))); }
		}
		if (fomentUnrestCooldown > 0) {
			int v = EmpireStat.FOMENTING_UNREST_UNREST.get(bs);
			u += v;
			if (explain != null) { explain.append("\n").append(_t("Fomented_Unrest_")).append("+").append(v).append(" ").append(_t("x_time_left", WorldMap.describeTime(fomentUnrestCooldown, bs))); }
		}
		if (!isConnectedToCapital(owner, wm)) {
			int v = EmpireStat.DISCONNECTED_FROM_CAPITAL_UNREST.get(bs);
			u += v;
			if (explain != null) { explain.append("\n").append(_t("Disconnected_Unrest_")).append("+").append(v); }
		}
		if (!takeoverNeeded && takeoverMethod == null) {
			for (int i = 0; i < upgrades.size(); i++) {
				CityUpgradeType cut = upgrades.get(i);
				if (cut.unrest.get(bs) != 0) {
					u += cut.unrest.get(bs);
					if (explain != null) { explain.append("\n").append(cut.getName()).append(cut.unrest.get(bs) > 0 ? ": +" : ": ").append(cut.unrest.get(bs)); }
				}
			}
		}
		for (UnrestSource us : unrests) {
			u += us.unrest;
			if (explain != null) {
				explain.append("\n").append(_t(us.reason)).append(us.unrest > 0 ? ": +" : ": ").append(us.unrest);
			}
		}
		int numCities = owner.numFullCities();
		int numTowns = owner.cities.size() - numCities;
		int fromCities = numCities * wm.unrestPerCity(bs);
		int fromTowns = numTowns * wm.unrestPerTown(bs);
		int empireSize = StrictMath.max(0, fromCities + fromTowns);
		u += empireSize;
		if (explain != null) { explain.append("\n").append(_t("Empire_Size_Unrest_")).append("+").append(empireSize).append(" ").append(_t("x_from_cities_y_from_towns", fromCities, fromTowns)); }
		// Reduction
		int defencesMaintenance = 0;
		for (int i = 0; i < defences.size(); i++) {
			Airship s = defences.get(i);
			if (s.nonCombat()) { continue; }
			defencesMaintenance += StrictMath.max(0, s.maintenanceCost());
		}
		int garrisonMaintenance = 0;
		Fleet g = wm.getGarrison(this);
		if (g != null) {
			for (int i = 0; i < g.actives.size(); i++) {
				Airship s = g.actives.get(i);
				garrisonMaintenance += StrictMath.max(0, s.maintenanceCost());
			}
			for (int i = 0; i < g.reserve.size(); i++) {
				Airship s = g.reserve.get(i);
				if (s.nonCombat()) { continue; }
				garrisonMaintenance += StrictMath.max(0, s.maintenanceCost());
			}
		}
		int defencesReduction = StrictMath.min(EmpireStat.MAX_UNREST_REDUCTION_FROM_DEFENCES.get(bs), (int) (defencesMaintenance * EmpireStat.UNREST_REDUCTION_PER_DEFENCES_MAINTENANCE.get(bs)));
		int garrisonReduction = StrictMath.min(EmpireStat.MAX_UNREST_REDUCTION_FROM_GARRISON.get(bs), (int) (garrisonMaintenance * EmpireStat.UNREST_REDUCTION_PER_GARRISON_MAINTENANCE.get(bs)));
		if (defencesReduction > 0) {
			u -= defencesReduction;
			if (explain != null) { explain.append("\n").append(_t("Defences_Unrest_Reduction_")).append(-defencesReduction); }
		}
		if (garrisonReduction > 0) {
			u -= garrisonReduction;
			if (explain != null) { explain.append("\n").append(_t("Garrison_Unrest_Reduction_")).append(-garrisonReduction); }
		}
		if (max0) {
			u = StrictMath.max(0, StrictMath.min(100, u));
		}
		if (explain != null) { explain.append("\n").append(_t("Unrest_Total_")).append(u).append("%"); }
		return u;
	}
	
	public int defencesMaintenanceCost(Empire owner, WorldMap m, StringBuilder sb) {
		int c = 0;
		for (Airship b : defences) {
			int cost = b.maintenanceCost();
			if (cost > 0) {
				c += cost;
				if (sb != null) {
					if (sb.length() > 0) {
						sb.append("\n");
					}
					sb.append(b.getName()).append(": ").append(cost);
				}
			}
		}
		int budget = (isTown ? EmpireStat.TOWN_DEFENCE_BUDGET : EmpireStat.CITY_DEFENCE_BUDGET).get(owner.bonuses);
		if (this == owner.getCapital()) {
			budget += EmpireStat.CAPITAL_EXTRA_DEFENCE_BUDGET.get(owner.bonuses);
		}
		Hero h = Hero.get(this, m);
		int heroBudget = h == null ? 0 : h.type.defenceBudget;
		int edictBudget = edict == null ? 0 : edict.defenceBudget;
		int total = StrictMath.max(0, c - budget - heroBudget - edictBudget);
		
		if (sb != null) {
			if (sb.length() > 0) {
				sb.append("\n").append(_t("local_defence_budget")).append(": -").append(budget);
				if (h != null && heroBudget != 0) {
					sb.append("\n").append(h.getName()).append(heroBudget > 0 ? ": -" : ": +").append(Math.abs(heroBudget));
				}
				if (edict != null && edictBudget != 0) {
					sb.append("\n").append(edict.getName()).append(edictBudget > 0 ? ": -" : ": +").append(Math.abs(edictBudget));
				}
				sb.append("\n");
			}
			
			sb.append(_t("budget_total")).append(": ").append(total);
		}
		
		return total;
	}

	/*public int adjustedConstructionTimeCost(ConstructionEntry ce, BonusSet bonuses, WorldMap wm) {
		Hero h = Hero.get(this, wm);
		if (ce.type == ConstructionEntry.Type.CITY_UPGRADE) {
			int fromHero = h == null ? 0 : h.type.upgradeSpeedPercent;
			int fromEdict = edict == null ? 0 : edict.upgradeSpeedPercent;
			return ce.upgrade.constructionTime.get(bonuses) * 100 / (100 + EmpireStat.UPGRADE_SPEED_PERCENTAGE_BONUS.get(bonuses) + fromHero + fromEdict);
		} else {
			int fromHero = h == null ? 0 : h.type.getSpeedPercent(ce.ship.type);
			int fromEdict = edict == null ? 0 : edict.getSpeedPercent(ce.ship.type);
			return (StrictMath.max(0, ce.cost) * 70 + 3000) * 100 / (100 + EmpireStat.SHIP_TYPE_CONSTRUCTION_SPEED_PERCENTAGE_BONUS.get(ce.ship.type).get(bonuses) + fromHero + fromEdict);
		}
	}*/
	
	public int baseConstructionTimeCost(ConstructionEntry ce, BonusSet bonuses, WorldMap wm) {
		Hero h = Hero.get(this, wm);
		if (ce.type == ConstructionEntry.Type.CITY_UPGRADE) {
			return ce.upgrade.constructionTime.get(bonuses);
		} else {
			return (StrictMath.max(0, ce.cost) * 70 + 3000);
		}
	}
	
	public int adjustedConstructionAmount(int amt, ConstructionEntry ce, BonusSet bonuses, WorldMap wm) {
		Hero h = Hero.get(this, wm);
		if (ce.type == ConstructionEntry.Type.CITY_UPGRADE) {
			int fromHero = h == null ? 0 : h.type.upgradeSpeedPercent;
			int fromEdict = edict == null ? 0 : edict.upgradeSpeedPercent;
			return amt * (100 + EmpireStat.UPGRADE_SPEED_PERCENTAGE_BONUS.get(bonuses) + fromHero + fromEdict) / 100;
		} else {
			int fromHero = h == null ? 0 : h.type.getSpeedPercent(ce.ship.type);
			int fromEdict = edict == null ? 0 : edict.getSpeedPercent(ce.ship.type);
			return amt * (100 + EmpireStat.SHIP_TYPE_CONSTRUCTION_SPEED_PERCENTAGE_BONUS.get(ce.ship.type).get(bonuses) + fromHero + fromEdict) / 100;
		}
	}
	
	public static int constructionUnitCost(int cost) {
		return cost * 70 + 3000;
	}
	
	public boolean canBuild(ShipType st) {
		return !takeoverNeeded && takeoverMethod == null && (canBuildShips() || !st.mobile);
	}
	
	public int production(Empire owner, WorldMap map) {
		if (strike > 0) { return 1; }
		int p = isTown ? EmpireStat.TOWN_PRODUCTION.get(owner.bonuses) : EmpireStat.CITY_PRODUCTION.get(owner.bonuses);
		for (int i = 0; i < upgrades.size(); i++) {
			p += upgrades.get(i).production.get(owner.bonuses);
		}
		Hero h = Hero.get(this, map);
		int fromHero = h == null ? 0 : h.type.productionPercent;
		int fromEdict = edict == null ? 0 : edict.production;
		p = (int) Math.ceil(p * (owner.playerControlled ? 1 : map.difficulty.aiProductionMultiplier(map.age)) * (100 + EmpireStat.PRODUCTION_PERCENTAGE_BONUS.get(owner.bonuses) + fromHero + fromEdict) / 100);
		if (plagueLevel != null) {
			p -= plagueLevel.productionReduction;
		}
		return StrictMath.max(1, p);
	}

	public void tick(int ms, Empire owner, WorldMap map) {
		if (edict != null) {
			edictTimeLeft -= ms;
			if (edictTimeLeft <= 0) {
				edict = null;
			}
		}
		
		for (int i = 0; i < unrests.size(); i++) {
			if (unrests.get(i).tick(ms)) {
				unrests.remove(i);
				i--;
			}
		}
		
		Hero h = Hero.get(this, map);
		
		if (plagueLevel != null) {
			plagueLevel.tick(this, ms, map, owner);
		}

		if (sendCoronationCancellationMessage) {
			sendCoronationCancellationMessage = false;
			if (coronationCancelledIntentionally) {
				for (Empire e : map.empires) {
					if (e == owner) { continue; }
					e.coronationIntentionallyCancelledCity = this;
				}
			} else if (coronationFailedDueToCityLostEmpire != null) {
				for (Empire e : map.empires) {
					e.coronationFailedDueToLostCityCity = this;
					e.coronationFailedDueToLostCityEmpire = coronationFailedDueToCityLostEmpire;
				}
			} else {
				for (Empire e : map.empires) {
					e.coronationFailedDueToNoLongerReadyCity = this;
				}
			}
		} else if (coronation && !coronationWarningSent && coronationProgress >= EmpireStat.CORONATION_TIME.get(owner.bonuses()) * 3 / 4) {
			coronationWarningSent = true;
			for (Empire e : map.empires) {
				if (e == owner) { continue; }
				e.coronationWarningCity = this;
			}
		} else if (coronation && !coronationStartedSent) {
			for (Empire e : map.empires) {
				if (e == owner) { continue; }
				e.coronationStartedCity = this;
			}
			coronationStartedSent = true;
		}
		
		if (finalRitual && !finalRitualWarningSent && finalRitualProgress >= EmpireStat.FINAL_RITUAL_TIME.get(owner.bonuses()) * 3 / 4) {
			finalRitualWarningSent = true;
			for (Empire e : map.empires) {
				if (e == owner) { continue; }
				e.finalRitualWarningCity = this;
			}
		} else if (finalRitual && !finalRitualStartedSent) {
			for (Empire e : map.empires) {
				if (e == owner) { continue; }
				e.finalRitualStartedCity = this;
			}
			finalRitualStartedSent = true;
		}
		
		tickMessages();
		strike -= ms;
		if (strike <= 0) { strike = 0; }
		postRaidCooldown -= ms;
		if (postRaidCooldown <= 0) { postRaidCooldown = 0; postRaidCooldownInitial = 0; }
		fomentUnrestCooldown -= ms;
		if (fomentUnrestCooldown <= 0) { fomentUnrestCooldown = 0; fomentUnrestCooldownInitial = 0; }
		postTakeoverCooldown -= ms;
		if (postTakeoverCooldown <= 0) { postTakeoverCooldown = 0; postTakeoverMethod = null; }
		if (getEconomicDamage() > 0) {
			econDmgRecovery += ms;
			if (econDmgRecovery > EmpireStat.ECON_DAMAGE_RECOVERY_TIME.get(owner.bonuses)) {
				setEconomicDamage(getEconomicDamage() - 1);
				econDmgRecovery -= EmpireStat.ECON_DAMAGE_RECOVERY_TIME.get(owner.bonuses);
				addMessage(owner, MessageType.ECON_RECOVERY, _t(getEconomicDamage() == 0 ? "x_has_fully_recovered_from_the_war" : "x_has_partly_recovered_from_the_war", name));
			}
		}
		if (takeoverMethod != null) {
			takeoverAmount += ms;
			if (takeoverAmount >= takeoverMethod.timeTaken.get(owner.bonuses)) {
				postTakeoverMethod = takeoverMethod;
				postTakeoverCooldown = takeoverMethod.unrestPeriod.get(owner.bonuses);
				setEconomicDamage(StrictMath.min(MAX_ECON_DAMAGES, getEconomicDamage() + takeoverMethod.economicDamage.get(owner.bonuses)));
				takeoverMethod = null;
				Fleet g = map.getGarrison(this);
				if (g != null) {
					g.repair(owner);
				}
				for (Airship s : defences) {
					s.repair(/* resetXP */ false, /* forceResetCrew */ false);
				}
			}
		}
		if (!takeoverNeeded && takeoverMethod == null && !constructing.isEmpty()) {
			if (constructing.get(0).type == ConstructionEntry.Type.CITY_UPGRADE) {
				CityUpgradeType cut = constructing.get(0).upgrade;
				constructing.get(0).progress += adjustedConstructionAmount(ms, constructing.get(0), owner.bonuses, map);
				if (constructing.get(0).progress >= baseConstructionTimeCost(constructing.get(0), owner.bonuses, map)) {
					constructing.remove(0);
					upgrades.add(cut);
					if (!upgradesEverBuilt.contains(cut)) {
						Hero.processHeroEvent(HeroEvent.upgradeBuilt(owner, cut), map);
						upgradesEverBuilt.add(cut);
					}
				}
			} else {
				ConstructionEntry ce = constructing.get(0);
				Airship c = ce.ship;
				ce.progress += adjustedConstructionAmount((ms * production(owner, map)), ce, owner.bonuses, map);
				if (ce.progress >= baseConstructionTimeCost(ce, owner.bonuses, map)) {
					constructing.remove(0);
					int oldCost = 0;
					try {
						oldCost = c.getCost();
						c.setBaseBonuses(owner.bonuses);
						c.repair(/* resetXP */ true);
					} catch (Exception e) {
						e.printStackTrace();
						c = null;
						owner.setMoney(owner.getMoney() + oldCost);
					}
					if (c != null) {
						if (ce.original != null) {
							c.transferExperienceAndMedals(ce.original);
						}
						if (c.type.mobile) {
							if (c.type == ShipType.AIRSHIP) {
								owner.hasBuiltAirship = true;
							}
							if (c.isBonusConstruction) {
								if (c.getName().equals("Fleshcracker")) {
									owner.hasBuiltFleshcracker = true;
								}
								if (c.getName().equals("MechSquid")) {
									owner.hasBuiltMechSquid = true;
								}
							}
							owner.addShipAt(c, this, map);
							if (ce.heroIDToAttach != -1) {
								Hero hero = Hero.get(ce.heroIDToAttach, map);
								if (hero != null && hero.getInShip() == null && hero.hired && hero.inEmpire == owner) {
									hero.setInShip(c);
								}
							}
						} else {
							defences.add(c);
						}
					}
				}
			}
		}
		if (coronation) {
			coronationProgress = Math.min(EmpireStat.CORONATION_TIME.get(owner.bonuses()), coronationProgress + ms);
		}
		if (finalRitual) {
			finalRitualProgress = Math.min(EmpireStat.FINAL_RITUAL_TIME.get(owner.bonuses()), finalRitualProgress + ms);
		}
	}
	
	@Override
	public ShipList shipList(WorldMap wm) {
		ShipArrayList l = new ShipArrayList(defences);
		Fleet fl = wm.getGarrison(this);
		if (fl != null) {
			l.ships.addAll(fl.actives);
		}
		return l;
	}
	
	public int refund(ConstructionEntry ce, BonusSet bonuses, WorldMap wm) {
		if (constructing.isEmpty()) { return 0; }
		double constructionProportion = ce.progress * 1.0 / baseConstructionTimeCost(ce, bonuses, wm);
		return (int) StrictMath.floor(ce.cost * (constructionProportion * 0.25 + (1 - constructionProportion)));
	}

	@Override
	public int getResupplySpeed(FleetOwner e, WorldMap m, StringBuilder explain) {
		boolean multiLine = false;
		int sup = isTown ? EmpireStat.RESUPPLY_TOWN.get(e.bonuses()) : EmpireStat.RESUPPLY_CITY.get(e.bonuses());
		if (explain != null) {
			if (isTown) {
				explain.append(_t("Town_resupply")).append(EmpireStat.RESUPPLY_TOWN.explain(e.bonuses()));
			} else {
				explain.append(_t("City_resupply")).append(EmpireStat.RESUPPLY_CITY.explain(e.bonuses()));
			}
		}
		for (int i = 0; i < upgrades.size(); i++) {
			sup += upgrades.get(i).localSupply.get(e.bonuses());
			if (explain != null && upgrades.get(i).localSupply.get(e.bonuses()) != 0) {
				multiLine = true;
				explain.append("\n").append(upgrades.get(i).getName()).append(": ").append(upgrades.get(i).localSupply.explain(e.bonuses()));
			}
		}
		int global = e.globalSupply();
		sup += global;
		if (explain != null) {
			if (global != 0) {
				multiLine = true;
				explain.append("\n").append(_t("Global_resupply_")).append(global);
			}
			if (multiLine) {
				explain.append("\n").append(_t("Total_resupply_")).append(sup);
			}
		}
		if (e instanceof Empire && !((Empire) e).playerControlled) {
			sup = (int) (sup * m.difficulty.aiResupplyMultiplier);
		}
		return sup;
	}

	public int getEconomicDamage() {
		return economicDamage;
	}

	public void setEconomicDamage(int economicDamage) {
		this.economicDamage = Math.max(0, Math.min(MAX_ECON_DAMAGES, economicDamage));
	}
	
	public boolean hasNestOfType(MonsterNestType nt, WorldMap wm) {
		for (MonsterNest mn : nestsInTerritory(wm)) {
			if (mn.type == nt) { return true; }
		}
		return false;
	}
	
	public Pair<MonsterNest, MonsterNest> nestMovePair(MonsterNestType nt, WorldMap wm) {
		for (MonsterNest mn : nestsInTerritory(wm)) {
			if (mn.type == nt) {
				Empire owner = wm.owner(this);
				for (MonsterNest newLocation : wm.nests) {
					if (newLocation.type == null && !owner.cities.contains(newLocation.city(wm))) {
						return new Pair<MonsterNest, MonsterNest>(mn, newLocation);
					}
				}
			}
		}
		return null;
	}
	
	public boolean clearNest(Hero h, Empire owner, MonsterNestType nt, WorldMap wm) {
		for (MonsterNest mn : nestsInTerritory(wm)) {
			if (mn.type == nt) {
				mn.clear(wm);
				owner.setMoney(owner.getMoney() + h.type.nestClearMoney);
				if (owner.research != null) {
					owner.researchPoints += h.type.nestClearResearch * 3000;
					owner.checkResearchComplete(wm);
				} else {
					owner.unassignedResearchPoints += h.type.nestClearResearch * 3000;
				}
				owner.changeReputation(h.type.nestClearRep, wm);
				if (h.type.nestClearStat != null) { h.changeStat(h.type.nestClearStat, h.type.nestClearStatChange); }
				owner.makeComment(h, _t(h.type.nestClearComment, nt.getName()), null);
				return true;
			}
		}
		return false;
	}
	
	public void moveNest(Hero h, Empire owner, MonsterNestType nt, WorldMap wm) {
		Pair<MonsterNest, MonsterNest> nestMovePair = nestMovePair(nt, wm);
		if (nestMovePair == null) { return; }
		nestMovePair.b.occupy(nt, wm);
		nestMovePair.a.clear(wm);
		owner.setMoney(owner.getMoney() + h.type.nestMoveMoney);
		if (owner.research != null) {
			owner.researchPoints += h.type.nestMoveResearch * 3000;
			owner.checkResearchComplete(wm);
		} else {
			owner.unassignedResearchPoints += h.type.nestMoveResearch * 3000;
		}
		owner.changeReputation(h.type.nestMoveRep, wm);
		if (h.type.nestMoveStat != null) { h.changeStat(h.type.nestMoveStat, h.type.nestMoveStatChange); }
		owner.makeComment(h, _t(h.type.nestMoveComment, nt.getName()), null);
	}
	
	public void makeEdict(Hero h, Empire owner, Edict edict, WorldMap wm) {
		if (this.edict != null) { return; }
		this.edict = edict;
		edictTimeLeft = edict.duration;
		economicDamage = StrictMath.max(0, StrictMath.min(MAX_ECON_DAMAGES, economicDamage += edict.pillaging));
		owner.setMoney(owner.getMoney() + edict.money);
		if (owner.research != null) {
			owner.researchPoints += edict.instantResearch * 3000;
			owner.checkResearchComplete(wm);
		} else {
			owner.unassignedResearchPoints += edict.instantResearch * 3000;
		}
		owner.changeReputation(edict.rep, wm);
		if (edict.stat != null) { h.changeStat(edict.stat, edict.statChange); }
		if (edict.stat2 != null) { h.changeStat(edict.stat2, edict.statChange2); }
		owner.makeComment(h, _t(edict.comment, getDisplayName()), null);
	}
		
	@Override
	public boolean canSeeFleetsHere(WorldMap m, Empire viewer) {
		return (viewer.getSpyFor(this) != null && viewer.getSpyFor(this).infiltrationTimeout <= 0) || super.canSeeFleetsHere(m, viewer);
	}
	
	@Override
	public String toString() { return name; }
}
