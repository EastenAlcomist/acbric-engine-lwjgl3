package com.zarkonnen.airships;

import com.zarkonnen.airships.HeroType.CombatAbility;
import com.zarkonnen.airships.HeroType.Stat;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Utils.Pair;
import static com.zarkonnen.catengine.util.Utils.p;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;

public class Hero implements HasName {
	public static final int RECRUIT_COOLDOWN = 6 * 4 * 7 * 400;
	public static final int RANDOMLY = 6 * 4 * 7 * 400;
	public static final int RARELY = 19 * 4 * 7 * 400;
	public static final int EVERY_MONTH_COOLDOWN = 4 * 7 * 400;
	
	public int id;
	public final HeroType type;
	public String instanceName;
	public HashMap<String, Integer> stats = new HashMap<String, Integer>();
	public City homeCity;
	public Empire nemesisEmpire;
	public int recruitCooldown;
	public int everyMonthCooldown;
	public Empire inEmpire;
	private Airship inShip;
	private City inCity;
	public boolean hired;
	public HashMap<Empire, EmpireRecord> empireRecords = new HashMap<Empire, EmpireRecord>();
	public int prevPowerSabotages = 0;
	public int injuredTime = 0;
	public transient int unassignedTime;
	
	public static class EmpireRecord {
		public final int id;
		public boolean bribed;
		public boolean wasHired;
		public int lastHireRequest;
		public int numHireRequests;
		
		public EmpireRecord(int id) {
			this.id = id;
		}
		
		public EmpireRecord(int id, JSONArray bribedBy, JSONArray visitedEmpires) {
			this.id = id;
			for (int i = 0; i < bribedBy.length(); i++) {
				if (bribedBy.getInt(i) == id) { bribed = true; break; }
			}
			for (int i = 0; i < visitedEmpires.length(); i++) {
				if (visitedEmpires.getInt(i) == id) { wasHired = true; break; }
			}
		}
		
		public EmpireRecord(JSONObject o) {
			id = o.getInt("id");
			bribed = o.optBoolean("bribed", false);
			wasHired = o.optBoolean("wasHired", false);
			lastHireRequest = o.optInt("lastHireRequest", 0);
			numHireRequests = o.optInt("numHireRequests", 0);
		}
		
		public JSONObject toJSON() {
			JSONObject o = new JSONObject().put("id", id);
			if (bribed) { o.put("bribed", true); }
			if (wasHired) { o.put("wasHired", true); }
			if (lastHireRequest != 0) { o.put("lastHireRequest", lastHireRequest); }
			if (numHireRequests != 0) { o.put("numHireRequests", numHireRequests); }
			return o;
		}
	}
	
	public boolean isBribed(Empire e) {
		return empireRecords.containsKey(e) && empireRecords.get(e).bribed;
	}
	
	public void setBribed(Empire e, boolean b) {
		if (!empireRecords.containsKey(e)) {
			empireRecords.put(e, new EmpireRecord(e.id));
		}
		empireRecords.get(e).bribed = b;
	}
	
	public boolean wasHiredBy(Empire e) {
		return empireRecords.containsKey(e) && empireRecords.get(e).wasHired;
	}
	
	public void setWasHiredBy(Empire e, boolean b) {
		if (!empireRecords.containsKey(e)) {
			empireRecords.put(e, new EmpireRecord(e.id));
		}
		empireRecords.get(e).wasHired = b;
	}
	
	public int getLastHireRequest(Empire e) {
		return empireRecords.containsKey(e) ? empireRecords.get(e).lastHireRequest : 0;
	}
	
	public void setLastHireRequest(Empire e, int lhr) {
		if (!empireRecords.containsKey(e)) {
			empireRecords.put(e, new EmpireRecord(e.id));
		}
		empireRecords.get(e).lastHireRequest = lhr;
	}
	
	public int getNumHireRequests(Empire e) {
		return empireRecords.containsKey(e) ? empireRecords.get(e).numHireRequests : 0;
	}
	
	public void incrementNumHireRequests(Empire e) {
		if (!empireRecords.containsKey(e)) {
			empireRecords.put(e, new EmpireRecord(e.id));
		}
		empireRecords.get(e).numHireRequests++;
	}

	@Override
	public String getName() {
		return type.isTemplate ? instanceName : _t(type.name);
	}
	
	public String getDesc() {
		return _t(type.name + "_desc");
	}
	
	public String getDetails(boolean forCampaign) {
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		sb.append("\n").append(getDesc());
		sb.append("\n").append(_t("role_" + type.role.name()));
		if (injuredTime > 0) {
			sb.append("\n").append(MyDraw.ERROR_C).append(_t("hero_injury_info", WorldMap.describeTime(injuredTime, BonusSet.empty()))).append("[]");
		}
		sb.append("\n");
		if (forCampaign) {
			sb.append(_t("Maintenance_cost_x", "$" + type.maintenance));
			if (!type.canDismiss) {
				sb.append("\n").append(_t("cannotBeDismissed"));
			}
			if (type.departureBonus != null) {
				sb.append("\n").append(_t("on_departure_bonus", type.departureBonus.getName()));
			}
			if (type.bonus != null) {
				sb.append("\n").append(type.bonus.getName());
			}
			if (type.research != 0) {
				sb.append("\n").append(_t("hero_research", (type.research > 0 ? "+" : "") + type.research));
			}
			if (type.unrest != 0) {
				sb.append("\n").append(_t("hero_unrest", (type.unrest > 0 ? "+" : "") + type.unrest));
			}
			if (type.spyDefence != 0) {
				sb.append("\n").append(_t("hero_spyDefence", (type.spyDefence > 0 ? "+" : "") + type.spyDefence));
			}
			if (type.productionPercent != 0) {
				sb.append("\n").append(_t("hero_production", (type.productionPercent > 0 ? "+" : "") + type.productionPercent));
			}
			if (type.defenceBudget != 0) {
				sb.append("\n").append(_t("hero_defenceBudget", (type.defenceBudget > 0 ? "+" : "") + type.defenceBudget));
			}
			if (type.incomePercent != 0) {
				sb.append("\n").append(_t("hero_incomePercent", (type.incomePercent > 0 ? "+" : "") + type.incomePercent));
			}
			if (type.researchPercent != 0) {
				sb.append("\n").append(_t("hero_researchPercent", (type.researchPercent > 0 ? "+" : "") + type.researchPercent));
			}
			if (type.upgradeCostPercent != 0) {
				sb.append("\n").append(_t("hero_upgradeCostPercent", (type.upgradeCostPercent > 0 ? "+" : "") + type.upgradeCostPercent));
			}
			if (type.upgradeSpeedPercent != 0) {
				sb.append("\n").append(_t("hero_upgradeSpeedPercent", (type.upgradeSpeedPercent > 0 ? "+" : "") + type.upgradeSpeedPercent));
			}
			if (type.airshipSpeedPercent != 0) {
				sb.append("\n").append(_t("hero_airshipSpeedPercent", (type.airshipSpeedPercent > 0 ? "+" : "") + type.airshipSpeedPercent));
			}
			if (type.landshipSpeedPercent != 0) {
				sb.append("\n").append(_t("hero_landshipSpeedPercent", (type.landshipSpeedPercent > 0 ? "+" : "") + type.landshipSpeedPercent));
			}
			if (type.buildingSpeedPercent != 0) {
				sb.append("\n").append(_t("hero_buildingSpeedPercent", (type.buildingSpeedPercent > 0 ? "+" : "") + type.buildingSpeedPercent));
			}
			if (type.expeditionStrengthPercent != 0) {
				sb.append("\n").append(_t("hero_expeditionStrengthPercent", (type.expeditionStrengthPercent > 0 ? "+" : "") + type.expeditionStrengthPercent));
			}
		}
		if (type.fleetSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_fleetSpeedPercent", (type.fleetSpeedPercent > 0 ? "+" : "") + type.fleetSpeedPercent));
		}
		if (type.shipBonus != null) {
			sb.append("\n").append(_t("hero_shipBonus_x", type.shipBonus.getName()));
		}
		if (type.fireRatePercent != 0) {
			sb.append("\n").append(_t("hero_fireRatePercent", (type.fireRatePercent > 0 ? "+" : "") + type.fireRatePercent));
		}
		if (type.fleetFireRatePercent != 0) {
			sb.append("\n").append(_t("hero_fleetFireRatePercent", (type.fleetFireRatePercent > 0 ? "+" : "") + type.fleetFireRatePercent));
		}
		if (type.accuracyPercent != 0) {
			sb.append("\n").append(_t("hero_accuracyPercent", (type.accuracyPercent > 0 ? "+" : "") + type.accuracyPercent));
		}
		if (type.fleetAccuracyPercent != 0) {
			sb.append("\n").append(_t("hero_fleetAccuracyPercent", (type.fleetAccuracyPercent > 0 ? "+" : "") + type.fleetAccuracyPercent));
		}
		if (type.crewSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_crewSpeedPercent", (type.crewSpeedPercent > 0 ? "+" : "") + type.crewSpeedPercent));
		}
		if (type.fleetCrewSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_fleetCrewSpeedPercent", (type.fleetCrewSpeedPercent > 0 ? "+" : "") + type.fleetCrewSpeedPercent));
		}
		if (type.flammabilityPercent != 0) {
			sb.append("\n").append(_t("hero_flammabilityPercent", (type.flammabilityPercent > 0 ? "+" : "") + type.flammabilityPercent));
		}
		if (type.fleetFlammabilityPercent != 0) {
			sb.append("\n").append(_t("hero_fleetFlammabilityPercent", (type.fleetFlammabilityPercent > 0 ? "+" : "") + type.fleetFlammabilityPercent));
		}
		if (type.explosionRiskPercent != 0) {
			sb.append("\n").append(_t("hero_explosionRiskPercent", (type.explosionRiskPercent > 0 ? "+" : "") + type.explosionRiskPercent));
		}
		if (type.fleetExplosionRiskPercent != 0) {
			sb.append("\n").append(_t("hero_fleetExplosionRiskPercent", (type.fleetExplosionRiskPercent > 0 ? "+" : "") + type.fleetExplosionRiskPercent));
		}
		if (type.commandCooldownPercent != 0) {
			sb.append("\n").append(_t("hero_commandCooldownPercent", (type.commandCooldownPercent > 0 ? "+" : "") + type.commandCooldownPercent));
		}
		if (type.fleetCommandCooldownPercent != 0) {
			sb.append("\n").append(_t("hero_fleetCommandCooldownPercent", (type.fleetCommandCooldownPercent > 0 ? "+" : "") + type.fleetCommandCooldownPercent));
		}
		if (type.repairAmountPercent != 0) {
			sb.append("\n").append(_t("hero_repairAmountPercent", (type.repairAmountPercent > 0 ? "+" : "") + type.repairAmountPercent));
		}
		if (type.fleetRepairAmountPercent != 0) {
			sb.append("\n").append(_t("hero_fleetRepairAmountPercent", (type.fleetRepairAmountPercent > 0 ? "+" : "") + type.fleetRepairAmountPercent));
		}
		if (type.firefightAmountPercent != 0) {
			sb.append("\n").append(_t("hero_firefightAmountPercent", (type.firefightAmountPercent > 0 ? "+" : "") + type.firefightAmountPercent));
		}
		if (type.fleetFirefightAmountPercent != 0) {
			sb.append("\n").append(_t("hero_firefightAmountPercent", (type.fleetFirefightAmountPercent > 0 ? "+" : "") + type.fleetFirefightAmountPercent));
		}
		if (type.propulsionPercent != 0) {
			sb.append("\n").append(_t("hero_propulsionPercent", (type.propulsionPercent > 0 ? "+" : "") + type.propulsionPercent));
		}
		if (type.liftPercent != 0) {
			sb.append("\n").append(_t("hero_liftPercent", (type.liftPercent > 0 ? "+" : "") + type.liftPercent));
		}
		if (type.armourRepairPercent != 0) {
			sb.append("\n").append(_t("hero_armourRepairPercent", type.armourRepairPercent));
		}
		if (type.surpriseAttack) {
			sb.append("\n").append(_t("hero_surprise_attack"));
		}
		if (forCampaign) {
			if (type.experiencePercent != 0) {
				sb.append("\n").append(_t("hero_experiencePercent", (type.experiencePercent > 0 ? "+" : "") + type.experiencePercent));
			}
			if (type.lootMoneyPercentage != 0) {
				sb.append("\n").append(_t("hero_looter"));
			}
			if (type.scavengeMoneyToSupply != 0) {
				sb.append("\n").append(_t("hero_scavenger"));
			}
			for (Edict e : type.edicts) {
				sb.append("\n").append(e.getName());
			}
			for (CityUpgradeType cut : type.instabuild) {
				sb.append("\n").append(_t("can_instabuild_info", cut.getName(), cut.getInstabuildCost()));
			}
		}
		for (CombatAbility ab : type.combatAbilities) {
			sb.append("\n").append(ab.getName());
		}
		if (forCampaign) {
			for (MonsterNestType nt : type.clearableNests) {
				sb.append("\n").append(_t("can_clear_nest_x", nt.getName()));
			}
			for (MonsterNestType nt : type.moveableNests) {
				sb.append("\n").append(_t("can_move_nest_x", nt.getName()));
			}
			for (Stat stat : type.stats) {
				sb.append("\n").append(stats.get(stat.name)).append(" ").append(stat.getName());
				if (stat.winOn100) {
					sb.append("\n").append(_t("win_on_x_stat", 100, stat.getName()));
				}
				if (stat.loseOn0) {
					sb.append("\n").append(_t("lose_on_x_stat", 0, stat.getName()));
				}
				if (stat.loseOn100) {
					sb.append("\n").append(_t("lose_on_x_stat", 100, stat.getName()));
				}
				if (stat.coronationOn100) {
					sb.append("\n").append(_t("coronation_on_x_stat", 100, stat.getName()));
				}
			}
		}
		return sb.toString();
	}
	
	public void changeStat(String name, int change) {
		if (stats.containsKey(name)) {
			stats.put(name, Math.max(0, Math.min(100, stats.get(name) + change)));
		}
	}
	
	public Airship getInShip() {
		return inShip;
	}
	
	public void setInShipOnly(Airship inShip) {
		this.inShip = inShip;
	}

	public final void setInShip(Airship inShip) {
		if (this.inShip != null) { this.inShip.setCaptain(null); }
		this.inShip = inShip;
		if (inShip != null) {
			inShip.setCaptain(this);
		}
	}
	
	public boolean onExpedition() {
		return inEmpire != null && hired && inEmpire.expedition != null && inEmpire.expedition.contains(getInShip());
	}
	
	public boolean isBusyInCity() {
		if (inEmpire == null || !hired || getInCity() == null) { return false; }
		if (type.edicts.contains(getInCity().edict)) { return true; }
		if (type.upgradeCostPercent != 0 || type.upgradeSpeedPercent != 0) {
			for (int i = 0; i < getInCity().constructing.size(); i++) {
				if (getInCity().constructing.get(i).upgrade != null) { return true; }
			}
		}
		if (type.airshipSpeedPercent != 0) {
			for (int i = 0; i < getInCity().constructing.size(); i++) {
				if (getInCity().constructing.get(i).ship != null && getInCity().constructing.get(i).ship.type == ShipType.AIRSHIP) { return true; }
			}
		}
		if (type.landshipSpeedPercent != 0) {
			for (int i = 0; i < getInCity().constructing.size(); i++) {
				if (getInCity().constructing.get(i).ship != null && getInCity().constructing.get(i).ship.type == ShipType.LANDSHIP) { return true; }
			}
		}
		if (type.buildingSpeedPercent != 0) {
			for (int i = 0; i < getInCity().constructing.size(); i++) {
				if (getInCity().constructing.get(i).ship != null && getInCity().constructing.get(i).ship.type == ShipType.BUILDING) { return true; }
			}
		}
		return false;
	}
	
	public int getLoyalty() {
		if (stats.containsKey("loyalty")) {
			return stats.get("loyalty");
		}
		return 100;
	}
	
	public Hero(HeroType type, WorldMap wm) {
		this.type = type;
		if (wm == null) {
			instanceName = type.name;
			return;
		}
		if (type.isTemplate) {
			instanceName = Lang.localeT(wm.lang, "hero_name_pattern",
					Lang.localeT(wm.lang, "HERO_" + type.templateFirstNameKey + "_" + wm.r.nextInt(type.templateFirstNameNum)),
					Lang.localeT(wm.lang, "HERO_" + type.templateLastNameKey + "_" + wm.r.nextInt(type.templateLastNameNum)));
		} else {
			instanceName = type.name;
		}
		id = wm.heroIDCounter++;
		for (HeroType.Stat stat : type.stats) {
			stats.put(stat.name, stat.startingValue);
		}
		if (type.hasHomeCity) {
			homeCity = wm.cities().get(wm.r.nextInt(wm.cities().size()));
		}
		if (type.hasNemesisEmpire) {
			nemesisEmpire = wm.empires.get(wm.r.nextInt(wm.empires.size()));
		}
	}
	
	private Hero(HeroType type, Locale lang, GuardedRandom r, int id) {
		this.type = type;
		if (type.isTemplate) {
			instanceName = Lang.localeT(lang, "hero_name_pattern",
					Lang.localeT(lang, "HERO_" + type.templateFirstNameKey + "_" + r.nextInt(type.templateFirstNameNum)),
					Lang.localeT(lang, "HERO_" + type.templateLastNameKey + "_" + r.nextInt(type.templateLastNameNum)));
		} else {
			instanceName = type.name;
		}
		this.id = id;
		for (HeroType.Stat stat : type.stats) {
			stats.put(stat.name, stat.startingValue);
		}
	}
	
	public Hero(JSONObject o, WorldMap m) {
		id = o.getInt("id");
		type = HeroType.ofName(o.getString("type"));
		instanceName = o.optString("instanceName", null);
		recruitCooldown = o.optInt("recruitCooldown", 0);
		everyMonthCooldown = o.optInt("everyMonthCooldown", 0);
		hired = o.optBoolean("hired", false);
		prevPowerSabotages = o.optInt("prevPowerSabotages", 0);
		injuredTime = o.optInt("injuredTime", 0);
		if (m != null) {
			if (o.has("homeCity")) {
				homeCity = m.getCity(o.getInt("homeCity"));
			}
			if (o.has("nemesisEmpire")) {
				nemesisEmpire = m.getEmpire(o.getInt("nemesisEmpire"));
			}
			if (o.has("inEmpire")) {
				inEmpire = m.getEmpire(o.getInt("inEmpire"));
			}
			if (o.has("inShip")) {
				inShip = m.getShip(o.getString("inShip"));
				if (inShip != null) {
					inShip.setCaptainOnly(this);
				}
			}
			if (o.has("inCity")) {
				setInCity(m.getCity(o.getInt("inCity")));
			}
			if (o.has("visitedEmpires") && o.has("bribedBy")) {
				for (Empire e : m.empires) {
					empireRecords.put(e, new EmpireRecord(e.id, o.getJSONArray("bribedBy"), o.getJSONArray("visitedEmpires")));
				}
			} else if (o.has("empireRecords")) {
				JSONObject recs = o.getJSONObject("empireRecords");
				for (Empire e : m.empires) {
					if (recs.has("" + e.id)) {
						empireRecords.put(e, new EmpireRecord(recs.getJSONObject("" + e.id)));
					}
				}
			}
		}
		for (HeroType.Stat stat : type.stats) {
			stats.put(stat.name, o.optInt(stat.name, stat.startingValue));
		}
	}
	
	public JSONObject toJSON() {
		JSONObject o = new JSONObject()
				.put("id", id)
				.put("type", type.name)
				.put("recruitCooldown", recruitCooldown)
				.put("everyMonthCooldown", everyMonthCooldown)
				.put("hired", hired)
				.put("prevPowerSabotages", prevPowerSabotages)
				.put("injuredTime", injuredTime);
		for (HeroType.Stat stat : type.stats) {
			o.put(stat.name, stats.get(stat.name));
		}
		if (instanceName != null) {
			o.put("instanceName", instanceName);
		}
		if (homeCity != null) {
			o.put("homeCity", homeCity.id);
		}
		if (nemesisEmpire != null) {
			o.put("nemesisEmpire", nemesisEmpire.id);
		}
		if (inEmpire != null) {
			o.put("inEmpire", inEmpire.id);
		}
		if (getInShip() != null) {
			o.put("inShip", getInShip().networkID);
		}
		if (getInCity() != null) {
			o.put("inCity", getInCity().id);
		}
		JSONObject recs = new JSONObject();
		ArrayList<Empire> emps = new ArrayList<Empire>(empireRecords.keySet());
		Collections.sort(emps, new Comparator<Empire>() {
			@Override
			public int compare(Empire o1, Empire o2) {
				return o1.id - o2.id;
			}
		});
		for (Empire e : emps) {
			recs.put("" + e.id, empireRecords.get(e).toJSON());
		}
		o.put("empireRecords", recs);
		return o;
	}
	
	public static void printList(WorldMap m) {
		for (Hero h : m.heroes) {
			//System.out.println(h.getName());
			if (h.inEmpire != null/* && h.type.role == HeroType.Role.CAPTAIN*/ && !h.type.isTemplate) {
				System.out.print(h.getName());
				System.out.print(" -- " + h.inEmpire.name);
				if (h.inShip != null) {
					System.out.print(" -- " + h.inShip.getName());
				}
				if (h.getInCity() != null) {
					System.out.print(" -- " + h.getInCity().name);
				}
				System.out.println();
			}
			
		}
	}
	
	public static Hero get(int id, WorldMap m) {
		for (int i = 0; i < m.heroes.size(); i++) {
			Hero h = m.heroes.get(i);
			if (h.id == id) { return h; }
		}
		return null;
	}
	
	public static Hero get(Airship ship, WorldMap m) {
		for (int i = 0; i < m.heroes.size(); i++) {
			if (m.heroes.get(i).getInShip() == ship) { return m.heroes.get(i); }
		}
		return null;
	}
	
	public static Hero get(City city, WorldMap m) {
		/*for (int i = 0; i < m.heroes.size(); i++) {
			if (m.heroes.get(i).getInCity() == city) { return m.heroes.get(i); }
		}
		return null;*/
		return city.governor;
	}
	
	public static ArrayList<Hero> getHeroes(Empire e, WorldMap m) {
		ArrayList<Hero> heroes = new ArrayList<Hero>();
		for (int i = 0; i < m.heroes.size(); i++) {
			if (m.heroes.get(i).inEmpire == e) {
				heroes.add(m.heroes.get(i));
			}
		}
		return heroes;
	}
	
	public static ArrayList<Hero> getHeroes(HeroType.Role r, boolean hiredOnly, Empire e, WorldMap m) {
		ArrayList<Hero> heroes = new ArrayList<Hero>();
		for (int i = 0; i < m.heroes.size(); i++) {
			if (m.heroes.get(i).inEmpire == e && m.heroes.get(i).type.role == r && (!hiredOnly || m.heroes.get(i).hired)) {
				heroes.add(m.heroes.get(i));
			}
		}
		return heroes;
	}
	
	public static ArrayList<Hero> spawnInitial(WorldMap m) {
		ArrayList<Hero> heroes = new ArrayList<Hero>();
		for (HeroType t : Loadable.all(HeroType.class)) {
			if (t.spawnAtStart) {
				if (t.isTemplate) {
					int n = (int) StrictMath.ceil(m.size.empires * t.templateSpawnPerEmpire);
					for (int i = 0; i < n; i++) {
						heroes.add(new Hero(t, m));
					}
				} else {
					heroes.add(new Hero(t, m));
				}
			}
		}
		return heroes;
	}
	
	public static ArrayList<Hero> getStarters(Locale lang, GuardedRandom r) {
		ArrayList<Hero> heroes = new ArrayList<Hero>();
		for (HeroType t : Loadable.all(HeroType.class)) {
			if (t.isStarter) {
				heroes.add(new Hero(t, lang, r, 0));
			}
		}
		return heroes;
	}
	
	public static void processHeroEvent(HeroEvent evt, WorldMap m) {
		if (!EHeroes.it.enabled) { return; }
		Pair<Hero, HeroEvent.Hook> recruit = getRecruitFromEvent(evt.e, evt, m);
		if (recruit != null) {
			recruit.a.setLastHireRequest(evt.e, m.age);
			recruit.a.incrementNumHireRequests(evt.e);
			evt.e.timeSinceHeroAppeared = 0;
			//System.out.println("RECRUIT " + recruit.a.getName() + " in " + evt.e.getName() + " because " + recruit.b.getDesc(recruit.a));
			recruit.a.inEmpire = evt.e;
			recruit.a.hired = false;
			recruit.a.recruitCooldown = RECRUIT_COOLDOWN;
			evt.e.newRecruits.add(recruit);
		}
		//System.out.println("evt " + evt);
		changeStatsFromEvent(evt.e, evt, m);
	}
	
	private static Hero replaceHero(Hero h, String newHeroName, WorldMap m) {
		Hero newHero = new Hero(HeroType.ofName(newHeroName), m);
		if (h.type.isTemplate) {
			newHero.instanceName = h.instanceName;
		}
		newHero.inEmpire = h.inEmpire;
		newHero.hired = true;
		newHero.setInShip(h.getInShip());
		newHero.setInCity(h.getInCity());
		if (newHero.type.hasNemesisEmpire) {
			newHero.nemesisEmpire = h.nemesisEmpire;
		}
		if (newHero.type.hasHomeCity) {
			newHero.homeCity = h.homeCity;
		}
		m.heroes.set(m.heroes.indexOf(h), newHero);
		return newHero;
	}
	
	public static boolean hire(Hero h, WorldMap m) {
		if (h == null || h.inEmpire == null || h.inEmpire.getMoney() < h.type.hireCost || h.hired) { return false; }
		h.hired = true;
		h.inEmpire.setMoney(h.inEmpire.getMoney() - h.type.hireCost);
		for (Tech.Choice research : h.type.techs) {
			for (Tech.Choice c : research.tech.choices) {
				h.inEmpire.techs.remove(c);
				h.inEmpire.bonuses.removeAll(c.bonuses);
			}
			h.inEmpire.bonuses.addAll(research.bonuses);
			h.inEmpire.techs.add(research);
		}
		if (h.type.bonus != null) {
			h.inEmpire.bonuses.set(h.type.bonus.ordinal(), true);
		}
		h.setWasHiredBy(h.inEmpire, true);
		Hero.processHeroEvent(HeroEvent.heroHired(h.inEmpire, h.type), m);
		return true;
	}
	
	public void clearStats() {
		for (EmpireRecord er : empireRecords.values()) {
			er.bribed = false;
		}
		stats.clear();
		for (HeroType.Stat stat : type.stats) {
			stats.put(stat.name, stat.startingValue);
		}
	}
	
	public static boolean fire(Hero h, WorldMap m) {
		if (h == null || h.inEmpire == null) { return false; }
		if (!h.type.canDismiss) { return false; }
		if (h.hired && h.type.departureBonus != null) {
			h.inEmpire.bonuses.set(h.type.departureBonus.ordinal(), true);
		}
		Hero.processHeroEvent(HeroEvent.heroLeft(h.inEmpire, h.type), m);
		h.inEmpire = null;
		h.setInCity(null);
		h.setInShip(null);
		h.clearStats();
		return true;
	}
	
	public static boolean assign(Hero hero, Empire e, City c, WorldMap m) {
		if (hero == null || hero.inEmpire != e || !e.cities.contains(c)) { return false; }
		for (Hero h : m.heroes) {
			if (h.getInCity() == c) {
				h.setInCity(null);
			}
		}
		hero.setInShip(null);
		hero.setInCity(c);
		return true;
	}

	public static boolean assign(Hero hero, Empire e, Airship ship, WorldMap m) {
		if (hero == null || hero.inEmpire != e || m.owner(ship) != e) { return false; }
		for (Hero h : m.heroes) {
			if (h.getInShip() == ship) {
				h.setInShip(null);
			}
		}
		for (Fleet f : e.getFleets()) {
			for (Airship s : f.actives) {
				if (s.getCaptain() == hero) {
					s.setCaptain(null);
				}
			}
			for (Airship s : f.reserve) {
				if (s.getCaptain() == hero) {
					s.setCaptain(null);
				}
			}
		}
		hero.setInShip(ship);
		hero.setInCity(null);
		return true;
	}

	public static boolean unassign(Hero hero, Empire e) {
		if (hero == null || hero.inEmpire != e) { return false; }
		hero.setInShip(null);
		hero.setInCity(null);
		return true;
	}
	
	public static void tick(int ms, WorldMap m) {
		if (!EHeroes.it.enabled) { return; }
		heroes: for (Hero h : new ArrayList<Hero>(m.heroes)) {
			if (!m.empires.contains(h.inEmpire)) {
				h.inEmpire = null;
				h.setInCity(null);
				h.inShip = null;
			}
			if (h.injuredTime > 0) {
				h.injuredTime = StrictMath.max(0, h.injuredTime - ms);
				if (h.injuredTime == 0 && h.inEmpire != null && h.inEmpire.playerControlled) {
					h.inEmpire.heroRecoveryNotices.add(h);
				}
			}
			if (!m.empires.contains(h.nemesisEmpire) && h.type.turnIntoIfNemesisIsGone != null) {
				replaceHero(h, h.type.turnIntoIfNemesisIsGone, m);
				continue;
			} else if (!m.empires.contains(h.nemesisEmpire)) {
				h.nemesisEmpire = null;
			}
			if (h.inEmpire == null) {
				h.unassignedTime = 0;
				continue;
			}
			if (h.hired) {
				if (h.getInCity() == null && h.inShip == null && h.injuredTime == 0) {
					h.unassignedTime += ms;
				} else {
					h.unassignedTime = 0;
				}
				h.everyMonthCooldown -= ms;
				if (h.everyMonthCooldown <= 0) {
					h.everyMonthCooldown += EVERY_MONTH_COOLDOWN;
					changeStatsFromEvent(h, HeroEvent.everyMonth(h.inEmpire), m);
				}
				if (m.r.nextInt(RANDOMLY) < ms) {
					changeStatsFromEvent(h, HeroEvent.randomly(h.inEmpire), m);
				}
				if (m.r.nextInt(RARELY) < ms) {
					changeStatsFromEvent(h, HeroEvent.rarely(h.inEmpire), m);
				}
				for (int si = 0; si < h.type.stats.size(); si++) {
					HeroType.Stat stat = h.type.stats.get(si);
					int value = h.stats.get(stat.name);
					if (value == 0) {
						if (stat.leaveOn0) {
							h.inEmpire.heroStatEvents.add(new HeroStatNotice(h, null, stat, false, true, false));
							if (h.hired && h.type.departureBonus != null) {
								h.inEmpire.bonuses.set(h.type.departureBonus.ordinal(), true);
							}
							h.inEmpire = null;
							h.setInCity(null);
							h.setInShip(null);
							h.clearStats();
							continue heroes;
						} else if (stat.dieOn0) {
							h.inEmpire.heroStatEvents.add(new HeroStatNotice(h, null, stat, false, false, true));
							if (h.getInShip() != null) {
								h.getInShip().setCaptain(null);
							}
							h.setInCity(null);
							m.heroes.remove(h);
							continue heroes;
						} else if (stat.evolveOn0 != null) {
							Hero newHero = replaceHero(h, stat.evolveOn0, m);
							h.inEmpire.heroStatEvents.add(new HeroStatNotice(h, newHero, stat, false, false, false));
							continue heroes;
						}
					} else if (value == 100) {
						if (stat.leaveOn100) {
							h.inEmpire.heroStatEvents.add(new HeroStatNotice(h, null, stat, true, true, false));
							if (h.hired && h.type.departureBonus != null) {
								h.inEmpire.bonuses.set(h.type.departureBonus.ordinal(), true);
							}
							h.inEmpire = null;
							h.setInCity(null);
							h.setInShip(null);
							h.clearStats();
							continue heroes;
						} else if (stat.dieOn100) {
							h.inEmpire.heroStatEvents.add(new HeroStatNotice(h, null, stat, true, false, true));
							if (h.getInShip() != null) {
								h.getInShip().setCaptain(null);
							}
							h.setInCity(null);
							m.heroes.remove(h);
							continue heroes;
						} else if (stat.evolveOn100 != null) {
							Hero newHero = replaceHero(h, stat.evolveOn100, m);
							h.inEmpire.heroStatEvents.add(new HeroStatNotice(h, newHero, stat, true, false, false));
							continue heroes;
						}
					}
				}
			} else {
				h.recruitCooldown -= ms;
				h.unassignedTime = 0;
				if (h.recruitCooldown <= 0) {
					h.inEmpire = null;
				}
			}
		}
		boolean isRandomly = m.r.nextInt(RANDOMLY) < ms;
		boolean isRarely = m.r.nextInt(RARELY) < ms;
		if (isRandomly || isRarely) {
			for (Empire e : m.empires) {
				Pair<Hero, HeroEvent.Hook> recruit = getRecruitFromEvent(e, isRarely ? HeroEvent.rarely(e) : HeroEvent.randomly(e), m);
				if (recruit != null) {
					recruit.a.setLastHireRequest(e, m.age);
					recruit.a.incrementNumHireRequests(e);
					recruit.a.inEmpire = e;
					recruit.a.hired = false;
					recruit.a.recruitCooldown = RECRUIT_COOLDOWN;
					e.timeSinceHeroAppeared = 0;
					e.newRecruits.add(recruit);
				}
			}
		}
	}
	
	private static Pair<Hero, HeroEvent.Hook> getRecruitFromEvent(Empire e, HeroEvent evt, WorldMap m) {
		if (m.heroFrequency.frequencyMultiplier == 0) { return null; }
		if (e.timeSinceHeroAppeared < 28 * 400) { return null; }
		int numCaptains = 0;
		int numGovernors = 0;
		boolean hasInteresting = false;
		for (int i = 0; i < m.heroes.size(); i++) {
			if (m.heroes.get(i).inEmpire == e && m.heroes.get(i).hired) {
				hasInteresting = hasInteresting || m.heroes.get(i).type.interesting;
				if (m.heroes.get(i).type.role == HeroType.Role.CAPTAIN) {
					numCaptains++;
				} else {
					numGovernors++;
				}
			}
		}
		
		int numHeroesBasedOnSize = 0;
		if (e.playerControlled) {
			numHeroesBasedOnSize = 4;
		} else {
			int numPlayers = 0;
			for (int i = 0; i < m.empires.size(); i++) {
				if (m.empires.get(i).playerControlled) {
					numPlayers++;
				}
			}
			int aiAvailableHeroes = m.heroes.size() / 2 - numPlayers * 3;
			numHeroesBasedOnSize = StrictMath.max(1, StrictMath.min(4, aiAvailableHeroes / StrictMath.max(1, m.empires.size() - numPlayers)));
		}
		int timeDiv = 400 * 28 * 13 * 3 + 400 * 28 * 9 * m.empires.size(); // Three years plus 9 months per *surviving* empire.
		int numHeroesBasedOnTime = StrictMath.max(1, StrictMath.min(numHeroesBasedOnSize, (int) (1.5 + (numHeroesBasedOnSize - 1.5) * m.age / timeDiv)));
		
		int noTriggerCaptains = (int) StrictMath.max(2, (StrictMath.pow(5, numCaptains - numHeroesBasedOnTime) * 200 / evt.strength() / m.heroFrequency.frequencyMultiplier));
		int noTriggerGovernors = (int) StrictMath.max(2, (StrictMath.pow(5, numGovernors - numHeroesBasedOnTime) * 200 / evt.strength() / m.heroFrequency.frequencyMultiplier));
		
		boolean needsInteresting = !hasInteresting && (e.playerControlled || m.empires.size() < 6) && m.age > 400 * 28 * 20; // One and a half years
		
		/*System.out.println(e.name + " " + evt + ":");
		//if (e.playerControlled) {
			System.out.println("numHeroesBasedOnSize " + numHeroesBasedOnSize + " numHeroesBasedOnTime " + numHeroesBasedOnTime + " needsInteresting " + needsInteresting);
			System.out.println(evt + " captains 1/" + noTriggerCaptains + " numCaptains " + numCaptains + " excessCaptains " + (numCaptains - numHeroesBasedOnTime) + " pow " + StrictMath.pow(5, numCaptains - numHeroesBasedOnTime) + " * 50 / evtStrength " + evt.strength());
			System.out.println(evt + " governors 1/" + noTriggerGovernors + " numGovernors " + numGovernors + " excessGovernors " + (numGovernors - numHeroesBasedOnTime) + " pow " + StrictMath.pow(5, numCaptains - numHeroesBasedOnTime) + " * 50 / evtStrength " + evt.strength());
		//}*/
		
		int leastNumHireRequests = 10000;
		ArrayList<Pair<Hero, HeroEvent.Hook>> candidates = new ArrayList<Pair<Hero, HeroEvent.Hook>>();
		lp: for (int i = 0; i < m.heroes.size(); i++) {
			Hero h = m.heroes.get(i);
			if (h.inEmpire != null) { continue; }
			if (h.nemesisEmpire == e) { continue; }
			if (h.type.required != null && !e.bonuses.contains[h.type.required.ordinal()]) { continue; }
			if (h.type.blockers.intersects(e.bonuses)) { continue; }
			if (h.wasHiredBy(e)) { /*System.out.println("Skip " + h.getName() + ": already previously hired");*/continue; }
			if (h.getLastHireRequest(e) != 0 && h.getLastHireRequest(e) > m.age - 13 * 28 * 400) { /*System.out.println("Skip " + h.getName() + ": last hire request " + (m.age - h.getLastHireRequest(e)) / 400 + " days ago");*/continue; }
			if (m.toggles.contains(ConquestToggle.REPUTATION) && (e.getReputation() < h.type.minReputation || e.getReputation() > h.type.maxReputation)) {
				continue;
			}
			for (int i2 = 0; i2 < m.heroes.size(); i2++) {
				if (i == i2) { continue; }
				Hero h2 = m.heroes.get(i2);
				if (h2.inEmpire == e && h.type == h2.type) {
					continue lp;
				}
			}
			for (int j = 0; j < h.type.recruitHooks.size(); j++) {
				HeroEvent.Hook hk = h.type.recruitHooks.get(j);
				if (hk.check(evt, h)) {
					boolean checkCaptains = noTriggerCaptains / hk.strength < 2 || m.r.nextInt(noTriggerCaptains / hk.strength) == 1;
					boolean checkGovernors = noTriggerGovernors / hk.strength < 2 || m.r.nextInt(noTriggerGovernors / hk.strength) == 1;
					if (!checkCaptains && h.type.role == HeroType.Role.CAPTAIN && !(h.type.interesting && needsInteresting)) { continue; }
					if (!checkGovernors && h.type.role == HeroType.Role.GOVERNOR && !(h.type.interesting && needsInteresting)) { continue; }
					candidates.add(p(h, h.type.recruitHooks.get(j)));
					leastNumHireRequests = StrictMath.min(leastNumHireRequests, h.getNumHireRequests(e));
					break;
				}
			}
		}
		
		//if (e.playerControlled) {
			//System.out.println("candidates " + candidates.size());
		//}
		if (candidates.isEmpty()) { return null; }
		for (int i = 0; i < candidates.size(); i++) {
			if (candidates.get(i).a.getNumHireRequests(e) > leastNumHireRequests) {
				//System.out.println("Skip " + candidates.get(i).a.getName() + ": " + candidates.get(i).a.getNumHireRequests(e) + " prev hire requests vs " + leastNumHireRequests);
				candidates.remove(i);
				i--;
			}
		}
		return candidates.get(m.r.nextInt(candidates.size()));
	}
	
	private static void changeStatsFromEvent(Empire e, HeroEvent evt, WorldMap m) {
		for (int hi = 0; hi < m.heroes.size(); hi++) {
			Hero h = m.heroes.get(hi);
			if (h.inEmpire != e || !h.hired) { continue; }
			changeStatsFromEvent(h, evt, m);
		}
	}
	
	private static void changeStatsFromEvent(Hero h, HeroEvent evt, WorldMap m) {
		for (int si = 0; si < h.type.stats.size(); si++) {
			HeroType.Stat stat = h.type.stats.get(si);
			for (int ci = 0; ci < stat.changers.size(); ci++) {
				HeroType.Stat.Changer changer = stat.changers.get(ci);
				if (changer.hook.check(evt, h)) {
					int newValue = StrictMath.max(0, StrictMath.min(100, h.stats.get(stat.name) + changer.getAmount(m)));
					boolean statChanged = newValue != h.stats.get(stat.name);
					h.stats.put(stat.name, newValue);
					h.inEmpire.makeComment(h, changer.hook.getComment(h), statChanged && evt.type != HeroEvent.MONTHLY ? new StatChange(h, changer) : null);
				}
			}
		}
		HeroType hiredHeroType = HeroEvent.getHiredHeroType(evt);
		if (hiredHeroType != null && Lang.hasLocalString(h.type.name + "_hire_" + hiredHeroType.name)) {
			h.inEmpire.makeComment(h, _t(h.type.name + "_hire_" + hiredHeroType.name), null);
		}
	}
	
	@Override
	public String toString() {
		return getName();
	}
		
	public static class StatChange {
		public final Hero h;
		public final HeroType.Stat.Changer changer;
		public int age; // Used for showing notice info.

		public StatChange(Hero h, HeroType.Stat.Changer changer) {
			this.h = h;
			this.changer = changer;
		}
		
		public String getShortText(WorldMap m) {
			String amt = changer.getAmount(m) >= 0 ? "+" + changer.getAmount(m) : "" + changer.getAmount(m);
			return amt + " " + changer.stat.getName() + " > " + h.stats.get(changer.stat.name);
		}
		
		public String getChangeTextOnly(WorldMap m) {
			String amt = changer.getAmount(m) >= 0 ? "+" + changer.getAmount(m) : "" + changer.getAmount(m);
			return changer.hook == null
					? _t("hero_statChange_noHook", h.getName(), amt, changer.stat.getName())
					: _t("hero_statChange", h.getName(), amt, changer.stat.getName(), changer.hook.getDesc(h));
		}
		
		public String getText(boolean past, WorldMap m) {
			String suffix = past ? "Past" : "";
			String amt = changer.getAmount(m) >= 0 ? "+" + changer.getAmount(m) : "" + changer.getAmount(m);
			String text = changer.hook == null
					? _t("hero_statChange_noHook", h.getName(), amt, changer.stat.getName())
					: _t("hero_statChange", h.getName(), amt, changer.stat.getName(), changer.hook.getDesc(h));
			if (h.stats.get(changer.stat.name) < 100 && h.stats.get(changer.stat.name) + changer.getAmount(m) >= 100) {
				if (changer.stat.dieOn100) {
					text += " " + MyDraw.ERROR_C + _t("dieConsequence" + suffix) + "[]";
				}
				if (changer.stat.leaveOn100) {
					text += " " + MyDraw.ERROR_C + _t("leaveConsequence" + suffix) + "[]";
					if (h.type.departureBonus != null) {
						text += " (+ " + h.type.departureBonus.getName() + ")";
					}
				}
				if (changer.stat.evolveOn100 != null) {
					text += " " + MyDraw.SELECTED_C + _t("evolveConsequence" + suffix) + "[]";
				}
				if (changer.stat.winOn100 && m.toggles.contains(ConquestToggle.HERO_VICTORY)) {
					text += " " + MyDraw.SELECTED_C + _t("winConsequence" + suffix) + "[]";
				}
				if (changer.stat.loseOn100 && m.toggles.contains(ConquestToggle.HERO_VICTORY)) {
					text += " " + MyDraw.ERROR_C + _t("loseConsequence" + suffix) + "[]";
				}
				if (changer.stat.coronationOn100 && m.toggles.contains(ConquestToggle.HERO_VICTORY) && m.toggles.contains(ConquestToggle.CORONATION)) {
					text += " " + MyDraw.SELECTED_C + _t("coronationConsequence" + suffix) + "[]";
				}
			}
			if (h.stats.get(changer.stat.name) > 0 && h.stats.get(changer.stat.name) + changer.getAmount(m) <= 0) {
				if (changer.stat.dieOn0) {
					text += " " + MyDraw.ERROR_C + _t("dieConsequence" + suffix) + "[]";
				}
				if (changer.stat.leaveOn0) {
					text += " " + MyDraw.ERROR_C + _t("leaveConsequence" + suffix) + "[]";
					if (h.type.departureBonus != null) {
						text += " (+ " + h.type.departureBonus.getName() + ")";
					}
				}
				if (changer.stat.loseOn0) {
					text += " " + MyDraw.ERROR_C + _t("loseConsequence" + suffix) + "[]";
				}
				if (changer.stat.evolveOn0 != null) {
					text += " " + MyDraw.SELECTED_C + _t("evolveConsequence" + suffix) + "[]";
				}
			}
			
			return text;
		}
	}
	
	public static String getStatChangeAppendix(Empire e, HeroEvent evt, WorldMap m, boolean past) {
		ArrayList<StatChange> l = getStatChangesFromEvent(e, evt, m);
		if (l.isEmpty()) { return ""; }
		StringBuilder sb = new StringBuilder();
		for (StatChange sc : l) {
			sb.append("\n").append(sc.getText(past, m));
		}
		return sb.toString();
	}
	
	private static ArrayList<StatChange> getStatChangesFromEvent(Empire e, HeroEvent evt, WorldMap m) {
		ArrayList<StatChange> l = new ArrayList<StatChange>();
		for (int hi = 0; hi < m.heroes.size(); hi++) {
			Hero h = m.heroes.get(hi);
			if (h.inEmpire != e || !h.hired) { continue; }
			for (int si = 0; si < h.type.stats.size(); si++) {
				HeroType.Stat stat = h.type.stats.get(si);
				for (int ci = 0; ci < stat.changers.size(); ci++) {
					HeroType.Stat.Changer changer = stat.changers.get(ci);
					if (changer.hook.check(evt, h)) {
						l.add(new StatChange(h, changer));
					}
				}
			}
		}
		return l;
	}

	public final City getInCity() {
		return inCity;
	}

	public final void setInCity(City inCity) {
		if (this.inCity != null) {
			this.inCity.governor = null;
		}
		this.inCity = inCity;
		if (this.inCity != null) {
			this.inCity.governor = this;
		}
	}
}
