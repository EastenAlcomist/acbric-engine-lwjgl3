package com.zarkonnen.airships;

import static com.zarkonnen.catengine.util.Utils.*;
import com.zarkonnen.catengine.util.Utils.Pair;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Spy {
	public static final int INFILTRATION_TIME = 400 * 7 * 5;
	public static final int MIN_SUCCESS_CHANCE = 25;
	
	public City location;
	public int infiltrationTimeout;
	public int actionCooldown;
	public int actionCooldownStartValue;
	public int increaseNetworkLevelAccumulator;
	public int networkLevel;
	public boolean hasPaidForNetworkUpgrade;
	
	public String getNetworkLevelName() {
		if (networkLevel == 0) { return "spy_network_none"; }
		if (networkLevel < 13) { return "spy_network_limited"; }
		if (networkLevel < 30) { return "spy_network_established"; }
		if (networkLevel < 47) { return "spy_network_large"; }
		if (networkLevel < 60) { return "spy_network_very_large"; }
		return "spy_network_all_pervasive";
	}

	public Spy(City location) {
		if (location == null) {
			throw new RuntimeException("Spy location is null!");
		}
		this.location = location;
		infiltrationTimeout = INFILTRATION_TIME;
	}
	
	public Spy(JSONObject o) {
		infiltrationTimeout = o.getInt("infiltrationTimeout");
		actionCooldown = o.optInt("actionCooldown", 0);
		actionCooldownStartValue = o.optInt("actionCooldownStartValue", 0);
		networkLevel = o.optInt("networkLevel", 0);
		increaseNetworkLevelAccumulator = o.optInt("increaseNetworkLevelAccumulator", 0);
		hasPaidForNetworkUpgrade = o.optBoolean("hasPaidForNetworkUpgrade", false);
	}
	
	public void finish(JSONObject o, WorldMap wm) {
		location = wm.empires.get(o.getInt("locationEmpire")).cities.get(o.getInt("location"));
	}
	
	public JSONObject toJSON(WorldMap wm) {
		Empire cityOwner = wm.owner(location);
		JSONObject o = new JSONObject();
		o.put("infiltrationTimeout", infiltrationTimeout);
		o.put("locationEmpire", wm.empires.indexOf(cityOwner));
		o.put("location", cityOwner.cities.indexOf(location));
		o.put("actionCooldown", actionCooldown);
		o.put("actionCooldownStartValue", actionCooldownStartValue);
		o.put("networkLevel", networkLevel);
		o.put("increaseNetworkLevelAccumulator", increaseNetworkLevelAccumulator);
		o.put("hasPaidForNetworkUpgrade", hasPaidForNetworkUpgrade);
		return o;
	}

	public void tick(int ms, Empire owner, WorldMap map) {
		infiltrationTimeout -= ms;
		if (infiltrationTimeout < 0) { infiltrationTimeout = 0; }
		actionCooldown -= ms;
		if (actionCooldown < 0) { actionCooldown = 0; }
		if (actionCooldown == 0 && infiltrationTimeout == 0 && networkLevel < EmpireStat.MAX_SPY_NETWORK_LEVEL.get(owner.bonuses)) {
			increaseNetworkLevelAccumulator += ms;
			int increaseCost = EmpireStat.SPY_NETWORK_INCREASE_LEVEL_TIME.get(owner.bonuses) + EmpireStat.SPY_NETWORK_INCREASE_LEVEL_TIME_PER_LEVEL.get(owner.bonuses) * networkLevel;
			if (increaseNetworkLevelAccumulator >= increaseCost) {
				networkLevel++;
				increaseNetworkLevelAccumulator -= increaseCost;
			}
		}
	}
	
	public static enum CitySpyAction implements HasName {
		SABOTAGE_RISE_TO_POWER {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return wm.owner(city).riseToPowerHero(wm) != null;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				Hero h =  wm.owner(city).riseToPowerHero(wm);
				return 60 + h.prevPowerSabotages * 30;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 500;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				Hero h = wm.owner(city).riseToPowerHero(wm);
				if (h != null) {
					h.stats.put("power", StrictMath.max(0, h.stats.get("power") - 50));
					h.prevPowerSabotages++;
				}
				return p(_t("spy_city_SABOTAGE_RISE_TO_POWER_success_spy", h == null ? "?" : h.getName()), _t("spy_city_SABOTAGE_RISE_TO_POWER_success_victim", h == null ? "?" : h.getName()));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				Hero h = wm.owner(city).riseToPowerHero(wm);
				return p(_t("spy_city_SABOTAGE_RISE_TO_POWER_failure_spy", h == null ? "?" : h.getName()), _t("spy_city_SABOTAGE_RISE_TO_POWER_failure_victim", h == null ? "?" : h.getName()));
			}
			@Override
			public int cooldown() { return 400 * 7 * 5; }
			@Override
			public int failureNetworkLoss() { return 12; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Hero h =  wm.owner(city).riseToPowerHero(wm);
				if (h == null) { return 0; }
				if (h.stats.get("power") < 80) {
					return 50;
				} else {
					return 1000;
				}
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				Hero h =  wm.owner(city).riseToPowerHero(wm);
				if (h == null) { return false; }
				if (h.stats.get("power") < 80) {
					return successChance(spy, e, city, wm) > 50 && failureNetworkLoss() < spy.networkLevel;
				} else {
					return successChance(spy, e, city, wm) > 15;
				}
			}
		},
		BRIBE_GOVERNOR {
			@Override
			public boolean isGovernorAction() { return true; }
			
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(city, wm);
				return h != null && h.getLoyalty() < EmpireStat.CORRUPTABLE_LOYALTY.get(wm.owner(city).bonuses) && !h.isBribed(wm.owner(spy));
			}

			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(city, wm);
				return 20 + (h == null ? 100 : h.getLoyalty() * 2);
			}

			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 400;
			}

			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(city, wm);
				if (h != null && wm.owner(spy) != null) {
					h.setBribed(wm.owner(spy), true);
				}
				return p(_t("spy_city_BRIBE_GOVERNOR_success_spy", h == null ? "?" : h.getName(), city.name), _t("spy_city_BRIBE_GOVERNOR_success_victim", h == null ? "?" : h.getName(), city.name));
			}

			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_BRIBE_GOVERNOR_failure_spy", city.name), _t("spy_city_BRIBE_GOVERNOR_failure_victim", city.name));
			}

			@Override
			public int cooldown() { return 400 * 7 * 8; }

			@Override
			public int failureNetworkLoss() {
				return 4;
			}

			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				return 60;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 60 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		CONVERT_GOVERNOR {
			@Override
			public boolean isGovernorAction() { return true; }
			
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				Empire owner = wm.owner(city);
				Hero h = Hero.get(city, wm);
				return h != null && owner != null && h.getLoyalty() < EmpireStat.CORRUPTABLE_LOYALTY.get(owner.bonuses) && /*city != owner.getCapital() &&*/ !city.takeoverNeeded && city.takeoverMethod == null;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(city, wm);
				return 50 + h.getLoyalty() * 4;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(city, wm);
				return h.type.maintenance * 100;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				Empire owner = wm.owner(spy);
				owner.spies.remove(spy);
				Empire oldEmp = wm.owner(city);
				oldEmp.cancelFinalRitual(city, /* intentionally */ false, wm);
				Fleet garrison = wm.getGarrison(city);
				Hero h = Hero.get(city, wm);
				if (h != null) {
					h.inEmpire = owner;
					h.clearStats();
				}
				oldEmp.cities.remove(city);
				oldEmp.hasLostTerritory = true;
				owner.cities.add(city);
				Hero.processHeroEvent(HeroEvent.cityGained(owner, city), wm);
				Hero.processHeroEvent(HeroEvent.cityLost(oldEmp, city), wm);
				wm.clearPathCaches();
				wm.isAdjacents.clear();
				if (garrison != null) {
					oldEmp.getFleets().remove(garrison);
					owner.getFleets().add(garrison);
					garrison.broadcastDestroyed(wm);
				}
				city.takeoverAmount = 0;
				city.takeoverMethod = null;
				city.takeoverNeeded = false;
				city.cancelCoronation(oldEmp, /* intentionally */ false);
				city.addMessage(null, City.MessageType.REVOLT, _t("The_citizens_of_x_revolt", city.name));
				return p(_t("spy_city_CONVERT_GOVERNOR_success_spy", city.name, h == null ? "?" : h.getName()), _t("spy_city_CONVERT_GOVERNOR_success_victim", city.name, h == null ? "?" : h.getName(), owner.name));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(city, wm);
				return p(_t("spy_city_CONVERT_GOVERNOR_failure_spy", city.name, h == null ? "?" : h.getName()), _t("spy_city_CONVERT_GOVERNOR_failure_victim", city.name, h == null ? "?" : h.getName()));
			}
			@Override
			public int cooldown() { return 400 * 182; } // Half a year
			@Override
			public int failureNetworkLoss() { return 12; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 20;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
						q -= 30;
						break;
					case DEFENSIVE_PACT:
						q -= 10;
					case WAR:
						q += 10;
						break;
				}
				if (spy.networkLevel > 40) {
					q += 10;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 65;
			}
		},
		INTRIGUE {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return ((wm.eraModifier != null && wm.eraModifier.name.equals("AGE_OF_INTRIGUE")) || EmpireStat.CAN_DO_INTRIGUE.get(wm.owner(spy).bonuses)) && !city.isTown;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return EmpireStat.INTRIGUE_FAILURE_RATE.get(wm.owner(spy).bonuses);//70;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				Empire actor = wm.owner(spy);
				Empire target = wm.owner(city);
				String missingShipName = "?";
				String researchName = "?";
				String key = "?";
				switch (wm.r.nextInt(45)) {
					case 0:
						key = "necklace";
						actor.setMoney(actor.getMoney() + 600);
						break;
					case 1:
						if (actor.research != null) {
							key = "poison";
							actor.researchPoints /= 2;
							researchName = _t("tech_" + actor.research.name);
							break;
						}
					case 2:
						if (actor.research != null) {
							key = "fishmonger";
							actor.researchPoints /= 2;
							researchName = _t("tech_" + actor.research.name);
							break;
						}
					case 3:
						MonsterNest n = null;
						for (MonsterNest mn : city.nestsInTerritory(wm)) {
							if (mn.type == null) {
								n = mn;
							}
						}
						if (n != null) {
							n.occupy(MonsterNestType.ofName("cultists"), wm);
							key = "cult";
							break;
						}
					case 4:
						if (!wm.prevEraModifiers.contains(EraModifier.ofName("AGE_OF_PIETY")) && wm.eraAge > 60000) {
							wm.startNewEraModifier(EraModifier.ofName("AGE_OF_PIETY"), null, null, city);
							key = "relics";
							break;
						}
					case 5:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "riot";
							break;
						}
					case 6:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "anemones";
							break;
						}
					case 7:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "anemonesEat";
							break;
						}
					case 8:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "riteOfGug";
							break;
						}
					case 9:
						Fleet f = wm.getGarrison(city);
						if (f != null && f.actives.size() > 1) {
							Airship missingShip = f.actives.get(wm.r.nextInt(f.actives.size()));
							f.actives.remove(missingShip);
							missingShipName = missingShip.getName();
							key = "skyWhales";
							break;
						}
					case 10:
						city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
						city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
						key = "newReligion";
						break;
					case 11:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "floatball";
							break;
						}
					case 12:
						key = "scam";
						actor.setMoney(actor.getMoney() + 300);
						break;
					case 13:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "cdos";
							break;
						}
					case 14:
						key = "statue";
						actor.setMoney(actor.getMoney() + 500);
						break;
					case 15:
						city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
						city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
						key = "cheese";
						break;
					case 16:
						if (wm.eraAge > 60000) {
							wm.startNewEraModifier(EraModifier.ofName("AGE_OF_REASON"), null, null, city);
							key = "archeology";
							break;
						}
					case 17:
						key = "paintings";
						actor.setMoney(actor.getMoney() + 400);
						break;
					case 18:
						if (wm.eraAge > 60000) {
							wm.startNewEraModifier(EraModifier.ofName("AGE_OF_MADNESS"), null, null, city);
							key = "madness";
							break;
						}
					case 19:
						if (city.upgrades.contains(CityUpgradeType.ofName("university"))) {
							city.upgrades.remove(CityUpgradeType.ofName("university"));
							key = "spit";
							break;
						}
					case 20:
						city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
						city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
						key = "fakeDragon";
						break;
					case 21:
						city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
						city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
						key = "fishFutures";
						break;
					case 22:
						if (wm.eraAge > 60000) {
							wm.startNewEraModifier(EraModifier.ofName("AGE_OF_DECADENCE"), null, null, city);
							key = "decadence";
							break;
						}
					case 23:
						key = "floatIsland";
						actor.setMoney(actor.getMoney() + 500);
						break;
					case 24:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "wurmRace";
							break;
						}
					case 25:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "endOfWorld";
							break;
						}
					case 26:
						key = "invisifish";
						actor.setMoney(actor.getMoney() + 50);
						break;
					case 27:
						city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
						city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
						key = "ripper";
						break;
					case 28: case 29: case 30: case 31:
						if (actor.research != null) {
							key = "research";
							actor.researchPoints /= 2;
							researchName = _t("tech_" + actor.research.name);
							break;
						}
					case 32: case 33: case 34: case 35:
						if (city.getEconomicDamage() < City.MAX_ECON_DAMAGES) {
							city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
							key = "riot";
							break;
						}
					case 36: case 37: case 38: case 39:
						city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
						city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
						key = "unrest";
						break;
					default:
						key = "fence";
						actor.setMoney(actor.getMoney() + 200);
						break;
				}
				return p(
						_t("spy_city_" + key + "_success_spy", city.name, "?", target.getName(), missingShipName, "?", "?", actor.getName(), "?", researchName, "?", "?", "?"),
						_t("spy_city_" + key + "_success_victim", city.name, "?", target.getName(), missingShipName, "?", "?", actor.getName(), "?", researchName, "?", "?", "?")
				);
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				Empire actor = wm.owner(spy);
				Empire target = wm.owner(city);
				String[] keys = {"cheese", "hats", "money", "necklace", "fire", "poison", "fishmonger", "cult", "relics", "pamphlets", "anemones", "play", "skyWhales", "theology", "floatball", "scam", "construction", "statue", "archeology", "party", "handwriting", "brag", "overthinking", "riot", "tiara", "lament", "scholar", "plot", "plot", "plot", "franz", "disinterest", "disinterest", "disinterest"};
				int roll = wm.r.nextInt(keys.length);
				switch (roll) {
					case 0:
						actor.setMoney(actor.getMoney() - 100);
						break;
					case 1:
						actor.setMoney(actor.getMoney() - 100);
						break;
					case 2:
						actor.setMoney(actor.getMoney() - 200);
						break;
				}
				String key = keys[roll];
				return p(
						_t("spy_city_" + key + "_failure_spy", city.name, "?", target.getName(), "?", "?", "?", actor.getName(), "?", "?", "?", "?", "?"),
						_t("spy_city_" + key + "_failure_victim", city.name, "?", target.getName(), "?", "?", "?", actor.getName(), "?", "?", "?", "?", "?")
				);
			}
			@Override
			public int cooldown() { return 400 * 28 * 3; }
			@Override
			public int failureNetworkLoss() { return 8; }

			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				int q = 20;
				Relationship rel = wm.getRelationship(e, wm.owner(city));
				switch (rel.level) {
					case ALLIANCE:
						q -= 10;
						break;
					case WAR:
						q += 10;
						break;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 50;
			}
		},
		UNEARTH_SCANDALS {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return wm.eraModifier != null && wm.eraModifier.name.equals("AGE_OF_DECADENCE") && wm.toggles.contains(ConquestToggle.REPUTATION);
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				if (Loadable.hasOfName(CityUpgradeType.class, "pleasurePalace") && !city.hadScandal && city.upgrades.contains(CityUpgradeType.ofName("pleasurePalace"))) {
					return 40;
				}
				return city.hadScandal ? 120 : 90;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				city.hadScandal = true;
				wm.owner(city).changeReputation(-3, wm);
				wm.endEraMeter += 10;
				wm.endEraMeterIncreaseEmpire = wm.owner(spy);
				if (Loadable.hasOfName(CityUpgradeType.class, "pleasurePalace") && city.upgrades.contains(CityUpgradeType.ofName("pleasurePalace"))) {
					return p(_t("spy_city_UNEARTH_SCANDALS_success_spy_pleasurePalace", city.name, wm.owner(city).getName()), _t("spy_city_UNEARTH_SCANDALS_success_victim_pleasurePalace", city.name, wm.owner(city).getName()));
				} else {
					return p(_t("spy_city_UNEARTH_SCANDALS_success_spy", city.name, wm.owner(city).getName()), _t("spy_city_UNEARTH_SCANDALS_success_victim", city.name, wm.owner(city).getName()));
				}
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				if (Loadable.hasOfName(CityUpgradeType.class, "pleasurePalace") && city.upgrades.contains(CityUpgradeType.ofName("pleasurePalace"))) {
					return p(_t("spy_city_UNEARTH_SCANDALS_failure_spy_pleasurePalace", city.name, wm.owner(city).getName()), _t("spy_city_UNEARTH_SCANDALS_failure_victim_pleasurePalace", city.name, wm.owner(city).getName()));
				} else {
					return p(_t("spy_city_UNEARTH_SCANDALS_failure_spy", city.name, wm.owner(city).getName()), _t("spy_city_UNEARTH_SCANDALS_failure_victim", city.name, wm.owner(city).getName()));
				}
			}
			@Override
			public int cooldown() { return 400 * 7 * 12; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 20;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
						q -= 20;
						break;
					case WAR:
						q += 10;
						break;
				}
				
				if (e.hasBonus(Bonus.ofName("REVOLUTION"))) {
					q += 10;
				}
				if (wm.toggles.contains(ConquestToggle.REPUTATION) && victim.getReputation() >= EmpireStat.CORONATION_REPUTATION.get(victim.bonuses)) {
					q += 20;
					if (victim.isCoronating()) {
						q += 10;
						if (victim.getReputation() - 5 < EmpireStat.CORONATION_REPUTATION.get(victim.bonuses)) {
							q += 2000;
						}
					}
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 50;
			}
		},
		ORGANISE_STRIKES {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return !city.isTown && wm.eraModifier != null && wm.eraModifier.name.equals("INDUSTRIAL_REVOLUTION") && city.strike <= 0;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				if (Loadable.hasOfName(CityUpgradeType.class, "factory") && city.upgrades.contains(CityUpgradeType.ofName("factory"))) {
					return 40;
				}
				if (Loadable.hasOfName(CityUpgradeType.class, "shipyard") && city.upgrades.contains(CityUpgradeType.ofName("shipyard"))) {
					return 40;
				}
				return 90;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				wm.endEraMeter += 10;
				wm.endEraMeterIncreaseEmpire = wm.owner(spy);
				city.strike = City.STRIKE_LENGTH;
				city.addMessage(null, City.MessageType.STRIKE, _t("The_workers_of_x_are_striking", city.name));
				return p(_t("spy_city_ORGANISE_STRIKES_success_spy", city.name, wm.owner(city).getName()), _t("spy_city_ORGANISE_STRIKES_success_victim", city.name, wm.owner(city).getName()));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_ORGANISE_STRIKES_failure_spy", city.name, wm.owner(city).getName()), _t("spy_city_ORGANISE_STRIKES_failure_victim", city.name, wm.owner(city).getName()));
			}
			@Override
			public int cooldown() { return City.STRIKE_LENGTH + 400 * 5 * 10; }
			@Override
			public int failureNetworkLoss() { return 12; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 20;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
						q -= 20;
						break;
					case WAR:
						q += 10;
						break;
				}
				
				if (e.hasBonus(Bonus.ofName("REVOLUTION"))) {
					q += 20;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 50 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		SABOTAGE_PRODUCTION {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return !city.constructing.isEmpty() && city.constructing.get(0).progress > 0;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 80;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				city.constructing.get(0).progress /= 2;
				return p(_t("spy_city_SABOTAGE_PRODUCTION_success_spy", city.constructing.get(0).getName()), _t("spy_city_SABOTAGE_PRODUCTION_success_victim", city.name, city.constructing.get(0).getName()));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_SABOTAGE_PRODUCTION_failure_spy"), _t("spy_city_SABOTAGE_PRODUCTION_failure_victim", city.name, city.constructing.get(0).getName()));
			}
			@Override
			public int cooldown() { return 400 * 7 * 20; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 0;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case WAR:
						q += 20;
						break;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 50 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		STEAL_RESEARCH {
			Tech.Choice target(Spy spy, City city, WorldMap wm) {
				Empire attacker = wm.owner(spy);
				Empire defender = wm.owner(city);
				lp: for (int di = 0; di < defender.techs.size(); di++) {
					Tech.Choice c = defender.techs.get(di);
					// Don't steal techs we already have, even if it was the other branch.
					for (int ai = 0; ai < attacker.techs.size(); ai++) {
						if (attacker.techs.get(ai).tech == c.tech) { continue lp; }
					}
					// Don't steal techs we haven't unlocked.
					if (c.tech.requiresBonus != null && !attacker.bonuses.contains[c.tech.requiresBonus.ordinal()]) {
						continue lp;
					}
					if (attacker.getResearchPoints(c) < c.cost(attacker, wm) / 2) {
						return c;
					}
				}
				return null;
			}
			
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return !city.isTown && target(spy, city, wm) != null;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 110;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				Tech.Choice c = target(spy, city, wm);
				Empire e = wm.owner(spy);
				if (c == e.research) {
					e.researchPoints += c.cost(e, wm) * 3 / 4;
				} else {
					e.partialResearchPoints.put(c, e.getResearchPoints(c) + c.cost(e, wm) * 3 / 4);
				}
				return p(_t("spy_city_STEAL_RESEARCH_success_spy", city.name, _t("tech_" + c.name)), _t("spy_city_STEAL_RESEARCH_success_victim", city.name, _t("tech_" + c.name)));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_STEAL_RESEARCH_failure_spy", city.name), _t("spy_city_STEAL_RESEARCH_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 20; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				return 5;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 80 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		STEAL_SUPPLIES {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				if (!wm.toggles.contains(ConquestToggle.SUPPLY)) { return false; }
				Fleet g = wm.getGarrison(city);
				return g != null && g.supply() > 0;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 80;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, final City city, WorldMap wm) {
				Fleet g = wm.getGarrison(city);
				int prevSupplies = g.supply();
				g.changeSupply((int) (-2000 * EmpireStat.SUPPLY_PER_DIST.get(wm.owner(city).bonuses)));
				int stolenSupplies = prevSupplies - g.supply();
				int stolenSuppliesLeft = stolenSupplies;
				ArrayList<Fleet> playerFleets = new ArrayList<Fleet>();
				for (Fleet f : wm.owner(spy).getFleets()) {
					if (f.location != null && !f.fleeDestinationNeeded) {
						playerFleets.add(f);
					}
				}
				// Sort by how close to the city the fleet is.
				Collections.sort(playerFleets, new Comparator<Fleet>() {
					@Override
					public int compare(Fleet a, Fleet b) {
						int aDistSq = (a.location.x - city.x) * (a.location.x - city.x) + (a.location.y - city.y) * (a.location.y - city.y);
						int bDistSq = (b.location.x - city.x) * (b.location.x - city.x) + (b.location.y - city.y) * (b.location.y - city.y);
						return aDistSq - bDistSq;
					}
				});
				for (Fleet f : playerFleets) {
					int giveSupply = StrictMath.min(stolenSuppliesLeft, f.maxSupply() - f.supply());
					f.changeSupply(giveSupply);
					stolenSuppliesLeft -= giveSupply;
				}
				return p(_t("spy_city_STEAL_SUPPLIES_success_spy", city.name, prevSupplies - g.supply()), _t("spy_city_STEAL_SUPPLIES_success_victim", city.name, stolenSupplies));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_STEAL_SUPPLIES_failure_spy", city.name), _t("spy_city_STEAL_SUPPLIES_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 24; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 0;
				Relationship rel = wm.getRelationship(e, victim);
				if (rel.level == Relationship.Level.WAR && wm.getFleetStrength(victim) > wm.getFleetStrength(e) * 2) {
					q += 20;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 70 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		STEAL_FUNDS {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return amt(city, wm) >= 200;
			}
			private int amt(City city, WorldMap wm) {
				Empire owner = wm.owner(city);
				return owner.getMoney() * (city.isTown ? 1 : 3) / (owner.cities.size() + owner.numFullCities() * 2);
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 110;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				int amt = amt(city, wm);
				wm.owner(city).setMoney(wm.owner(city).getMoney() - amt);
				wm.owner(spy).setMoney(wm.owner(spy).getMoney() + amt);
				return p(_t("spy_city_STEAL_FUNDS_success_spy", city.name, amt), _t("spy_city_STEAL_FUNDS_success_victim", city.name, amt));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_STEAL_FUNDS_failure_spy", city.name), _t("spy_city_STEAL_FUNDS_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 24; }
			@Override
			public int failureNetworkLoss() { return 4; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = amt(city, wm) / 100;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
					case DEFENSIVE_PACT:
						q -= 5;
						break;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 75 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		BUILD_NETWORK {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return !spy.hasPaidForNetworkUpgrade && spy.networkLevel < EmpireStat.MAX_SPY_NETWORK_LEVEL.get(wm.owner(spy).bonuses);
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 80;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 300;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				int prevNetwork = spy.networkLevel;
				spy.networkLevel = StrictMath.min(EmpireStat.MAX_SPY_NETWORK_LEVEL.get(wm.owner(spy).bonuses), spy.networkLevel + 15);
				spy.hasPaidForNetworkUpgrade = true;
				return p(_t("spy_city_BUILD_NETWORK_success_spy", city.name, spy.networkLevel - prevNetwork, _t(spy.getNetworkLevelName())), _t("spy_city_BUILD_NETWORK_success_victim", city.name));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_BUILD_NETWORK_failure_spy", city.name), _t("spy_city_BUILD_NETWORK_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 5; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 0;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
					case DEFENSIVE_PACT:
						q -= 10;
						break;
				}
				if (e.getMoney() > 4000) {
					q += 5;
				}
				if (city.coronation || city.finalRitual) {
					q += 60;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 85 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		FOMENT_UNREST {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return city.fomentUnrestCooldown <= 0;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 110;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				city.fomentUnrestCooldown = EmpireStat.FOMENTING_UNREST_TIME.get(wm.owner(spy).bonuses);
				city.fomentUnrestCooldownInitial = city.fomentUnrestCooldown;
				return p(_t("spy_city_FOMENT_UNREST_success_spy", city.name), _t("spy_city_FOMENT_UNREST_success_victim", city.name));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_FOMENT_UNREST_failure_spy", city.name), _t("spy_city_FOMENT_UNREST_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 16; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 5;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
					case DEFENSIVE_PACT:
						q -= 20;
						break;
					case WAR:
						q += 5;
						break;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 50 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		SABOTAGE_CORONATION {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return city.coronation;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 50 + city.prevCoronationSabotages * 20;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				city.coronationProgress -= 33600;
				city.prevCoronationSabotages++;
				wm.owner(spy).coronationsSabotaged++;
				int scenario = wm.r.nextInt(16);
				return p(_t("spy_city_SABOTAGE_CORONATION_success_spy_" + scenario, city.name, WorldMap.describeTime(33600, wm.owner(spy).bonuses)), _t("spy_city_SABOTAGE_CORONATION_success_victim_" + scenario, city.name, WorldMap.describeTime(33600, wm.owner(city).bonuses)));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_SABOTAGE_CORONATION_failure_spy", city.name), _t("spy_city_SABOTAGE_CORONATION_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 5; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				if (city.coronationProgress < EmpireStat.CORONATION_TIME.get(wm.owner(city).bonuses) / 4) {
					return 50;
				} else {
					return 1000;
				}
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				if (city.coronationProgress < EmpireStat.CORONATION_TIME.get(wm.owner(city).bonuses) / 2) {
					return successChance(spy, e, city, wm) > 50 && failureNetworkLoss() < spy.networkLevel;
				} else {
					return successChance(spy, e, city, wm) > 15;
				}
			}
		},
		SABOTAGE_FINAL_RITUAL {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				return city.finalRitual;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 25 + city.prevFinalRitualSabotages * 20;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				city.finalRitualProgress -= 33600;
				city.prevFinalRitualSabotages++;
				int scenario = wm.r.nextInt(10);
				return p(_t("spy_city_SABOTAGE_FINAL_RITUAL_success_spy_" + scenario, city.name, WorldMap.describeTime(33600, wm.owner(spy).bonuses)), _t("spy_city_SABOTAGE_FINAL_RITUAL_success_victim_" + scenario, city.name, WorldMap.describeTime(33600, wm.owner(city).bonuses)));
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_SABOTAGE_FINAL_RITUAL_failure_spy", city.name), _t("spy_city_SABOTAGE_FINAL_RITUAL_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 5; }
			@Override
			public int failureNetworkLoss() { return 8; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				if (city.finalRitualProgress < EmpireStat.FINAL_RITUAL_TIME.get(wm.owner(city).bonuses) / 4) {
					return 50;
				} else {
					return 1000;
				}
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				if (city.finalRitualProgress < EmpireStat.FINAL_RITUAL_TIME.get(wm.owner(city).bonuses) / 2) {
					return successChance(spy, e, city, wm) > 50 && failureNetworkLoss() < spy.networkLevel;
				} else {
					return successChance(spy, e, city, wm) > 15;
				}
			}
		},
		INCITE_RIOT {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				Empire owner = wm.owner(city);
				return owner != null && city != owner.getCapital() && city.unrest(wm, null) >= 30 && (city.getEconomicDamage() < City.MAX_ECON_DAMAGES || !city.upgrades.isEmpty());
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 120;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return 0;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				city.setEconomicDamage(StrictMath.min(city.getEconomicDamage() + 1, City.MAX_ECON_DAMAGES));
				ArrayList<CityUpgradeType> cuts = new ArrayList<CityUpgradeType>();
				for (CityUpgradeType cut : city.upgrades) {
					if (cut.canBuild && cut.removable && (cut.requires == null || wm.owner(city).bonuses.containsAll(cut.requires))) {
						cuts.add(cut);
					}
				}
				if (!cuts.isEmpty()) {
					CityUpgradeType cut = cuts.get(wm.r.nextInt(cuts.size()));
					city.upgrades.remove(cut);
					return p(_t("spy_city_INCITE_RIOT_success_spy_destroyed_upgrade", city.name, cut.getName()), _t("spy_city_INCITE_RIOT_success_victim_destroyed_upgrade", city.name, cut.getName()));
				} else {
					return p(_t("spy_city_INCITE_RIOT_success_spy", city.name), _t("spy_city_INCITE_RIOT_success_victim", city.name));
				}
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_INCITE_RIOT_failure_spy", city.name), _t("spy_city_INCITE_RIOT_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 20; }
			@Override
			public int failureNetworkLoss() { return 15; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = -5;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
					case DEFENSIVE_PACT:
						q -= 30;
						break;
					case WAR:
						q += 12;
						break;
				}
				for (CityUpgradeType cut : city.upgrades) {
					if (cut.canBuild && cut.removable && (cut.requires == null || wm.owner(city).bonuses.containsAll(cut.requires))) {
						q += 12;
						break;
					}
				}
				if (city.isTown) {
					q -= 5;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 75 && failureNetworkLoss() < spy.networkLevel;
			}
		},
		INCITE_REVOLT {
			@Override
			public boolean available(Spy spy, City city, WorldMap wm) {
				Empire owner = wm.owner(city);
				return owner != null && city != owner.getCapital() && city.unrest(wm, null) >= 30 && !city.takeoverNeeded && city.takeoverMethod == null;
			}
			@Override
			public int baseFailureRate(Spy spy, City city, WorldMap wm) {
				return 150;
			}
			@Override
			public int baseCost(Spy spy, City city, WorldMap wm) {
				return city.isTown ? 200 : 500;
			}
			@Override
			public Pair<String, String> success(Spy spy, City city, WorldMap wm) {
				Empire owner = wm.owner(spy);
				owner.spies.remove(spy);
				if (EmpireStat.REVOLTING_CITIES_JOIN_EMPIRE.get(owner.bonuses())) {
					Empire oldEmp = wm.owner(city);
					oldEmp.cancelFinalRitual(city, /* intentionally */ false, wm);
					Fleet garrison = wm.getGarrison(city);
					wm.clearHeroFrom(city);
					oldEmp.cities.remove(city);
					oldEmp.hasLostTerritory = true;
					owner.cities.add(city);
					Hero.processHeroEvent(HeroEvent.cityGained(owner, city), wm);
					Hero.processHeroEvent(HeroEvent.cityLost(oldEmp, city), wm);
					wm.clearPathCaches();
					if (garrison != null) {
						oldEmp.getFleets().remove(garrison);
						owner.getFleets().add(garrison);
						garrison.broadcastDestroyed(wm);
					}
					city.takeoverAmount = 0;
					city.takeoverMethod = null;
					city.takeoverNeeded = false;
					city.cancelCoronation(oldEmp, /* intentionally */ false);
					city.addMessage(null, City.MessageType.REVOLT, _t("The_citizens_of_x_revolt", city.name));
					return p(_t("spy_city_INCITE_REVOLT_success_join_spy", city.name), _t("spy_city_INCITE_REVOLT_success_join_victim", city.name, owner.name));
				} else {
					Empire newEmp = new Empire(wm.getFreeEmpireID(), city.name, CoatOfArms.getRandom(wm.r, HeraldicStyle.ofName("city")), new ArrayList<String>(), Bonus.ofName("NO_BONUS"), 100, 1.0, owner.constructionStrategy, DiplomacyPersonality.pick(null, wm.r), wm);
					if (wm.techSpeed.speedMultiplier != 0) {
						newEmp.bonuses.set(Bonus.ofName("RESEARCH").ordinal(), true);
					}
					wm.determineMapColor(newEmp);
					wm.empires.add(newEmp);
					Empire oldEmp = wm.owner(city);
					oldEmp.cancelFinalRitual(city, /* intentionally */ false, wm);
					Fleet garrison = wm.getGarrison(city);
					wm.clearHeroFrom(city);
					oldEmp.cities.remove(city);
					oldEmp.hasLostTerritory = true;
					newEmp.cities.add(city);
					Hero.processHeroEvent(HeroEvent.cityGained(newEmp, city), wm);
					Hero.processHeroEvent(HeroEvent.cityLost(oldEmp, city), wm);
					wm.clearPathCaches();
					if (garrison != null) {
						oldEmp.getFleets().remove(garrison);
						newEmp.getFleets().add(garrison);
						garrison.broadcastDestroyed(wm);
					}
					newEmp.bonuses.addAll(oldEmp.bonuses());
					newEmp.techs.addAll(oldEmp.techs);
					city.takeoverAmount = 0;
					city.takeoverMethod = null;
					city.takeoverNeeded = false;
					city.cancelCoronation(oldEmp, /* intentionally */ false);
					city.addMessage(null, City.MessageType.REVOLT, _t("The_citizens_of_x_revolt", city.name));
					return p(_t("spy_city_INCITE_REVOLT_success_spy", city.name), _t("spy_city_INCITE_REVOLT_success_victim", city.name));
				}
			}
			@Override
			public Pair<String, String> failure(Spy spy, City city, WorldMap wm) {
				return p(_t("spy_city_INCITE_REVOLT_failure_spy"), _t("spy_city_INCITE_REVOLT_failure_victim", city.name));
			}
			@Override
			public int cooldown() { return 400 * 182; } // Half a year
			@Override
			public int failureNetworkLoss() { return 30; }
			
			@Override
			public int aiPriority(Empire e, Spy spy, City city, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 0;
				Relationship rel = wm.getRelationship(e, victim);
				switch (rel.level) {
					case ALLIANCE:
						q -= 30;
						break;
					case WAR:
						q += 10;
						break;
				}
				
				if (EmpireStat.REVOLTING_CITIES_JOIN_EMPIRE.get(e.bonuses)) {
					q += 10;
					if (spy.networkLevel > 40) {
						q += 20;
					}
				}
				if (spy.networkLevel > 40) {
					q += 10;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, WorldMap wm) {
				return successChance(spy, e, city, wm) > 70;
			}
		},
		;
		public abstract boolean available(Spy spy, City city, WorldMap wm);
		public abstract int baseFailureRate(Spy spy, City city, WorldMap wm);
		public abstract int baseCost(Spy spy, City city, WorldMap wm);
		public abstract Pair<String, String> success(Spy spy, City city, WorldMap wm);
		public abstract Pair<String, String> failure(Spy spy, City city, WorldMap wm);
		public abstract int cooldown();
		public abstract int failureNetworkLoss();
		public abstract int aiPriority(Empire e, Spy spy, City city, WorldMap wm);
		public abstract boolean aiReady(Empire e, Spy spy, City city, WorldMap wm);
		
		public int successChance(Spy spy, Empire empire, City city, WorldMap wm) {
			if (empire.easySpyCheat) { return 100; }
			int failure = baseFailureRate(spy, city, wm) - city.unrest(wm, null) - spy.networkLevel;
			int heroDefence = 0;
			Hero h = Hero.get(city, wm);
			if (h != null) {
				heroDefence = h.type.spyDefence;
				if (h.isBribed(empire)) {
					failure -= EmpireStat.BRIBED_GOVERNOR_SPY_BONUS.get(empire.bonuses);
					heroDefence = 0;
				}
				if (isGovernorAction()) {
					if (h.nemesisEmpire == empire) {
						failure += 999;
					} else if (h.type.blockers.intersects(empire.bonuses)) {
						failure += 999;
					}
				}
			}
			int success = StrictMath.max(0, 100 - failure);
			int edictDefence = city.edict != null ? city.edict.spyDefence : 0;
			return StrictMath.min(100, success * (100 + EmpireStat.ESPIONAGE_SUCCESS_PERCENTAGE_BONUS.get(empire.bonuses()) - heroDefence - edictDefence - EmpireStat.ESPIONAGE_DEFENCE_PERCENTAGE_BONUS.get(wm.owner(city).bonuses())) / 100);
		}
		
		public boolean isGovernorAction() { return false; }
		
		public int cost(Spy spy, Empire empire, City city, WorldMap wm) {
			return baseCost(spy, city, wm);
		}
		
		@Override
		public String getName() {
			return _t("spy_city_" + name());
		}
		
		public String getTooltip() {
			return _t("spy_city_tooltip_" + name());
		}
	}
	
	public static enum ShipSpyAction {
		CONVERT_CAPTAIN {
			@Override
			public boolean isCaptainAction() { return true; }
			
			@Override
			public boolean available(Airship ship, Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(ship, wm);
				return h != null && h.getLoyalty() < EmpireStat.CORRUPTABLE_LOYALTY.get(wm.owner(city).bonuses) && closestCity(ship, spy, city, wm) != null;
			}
			@Override
			public int baseFailureRate(Airship ship, Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(ship, wm);
				return 40 + ship.getCost() / 1000 + h.getLoyalty() * 2;
			}
			@Override
			public int baseCost(Airship ship, Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(ship, wm);
				return h.type.maintenance * 100 + ship.getCost() / 50;
			}
			private City closestCity(Airship ship, Spy spy, City city, WorldMap wm) {
				Empire newOwner = wm.owner(spy);
				City closest = null;
				double closestDist = 0;
				for (City c : newOwner.cities) {
					if (ship.type == ShipType.LANDSHIP && !wm.hasConnection(city.x, city.y, c, newOwner, true, false, false)) {
						continue;
					}
					double d = c.dist(city);
					if (closest == null || d < closestDist) {
						closest = c;
						closestDist = d;
					}
				}
				return closest;
			}
			@Override
			public Pair<String, String> success(Airship ship, Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(ship, wm);
				Empire newOwner = wm.owner(spy);
				Empire victim = wm.owner(city);
				h.inEmpire = newOwner;
				h.clearStats();
				victim.remove(ship, wm);
				Fleet newF = new Fleet(city, wm);
				newF.actives.add(ship);
				newOwner.getFleets().add(newF);
				if (!newF.doTravelTo(closestCity(ship, spy, city, wm), wm, false)) {
					System.out.println("Converted captain cannot travel to nearest city.");
					newOwner.getFleets().remove(newF);
				}
				return p(_t("spy_ship_CONVERT_CAPTAIN_success_spy", h == null ? "?" : h.getName(), ship.getName()), _t("spy_ship_CONVERT_CAPTAIN_success_victim", h == null ? "?" : h.getName(), ship.getName(), city.name));
			}
			@Override
			public Pair<String, String> failure(Airship ship, Spy spy, City city, WorldMap wm) {
				Hero h = Hero.get(ship, wm);
				return p(_t("spy_ship_CONVERT_CAPTAIN_failure_spy", h == null ? "?" : h.getName(), ship.getName()), _t("spy_ship_CONVERT_CAPTAIN_failure_victim", h == null ? "?" : h.getName(), ship.getName(), city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 8; }
			@Override
			public int failureNetworkLoss() { return 5; }
			@Override
			public String getTooltip(Airship ship, Spy spy, City city, WorldMap wm) {
				return _t("spy_ship_tooltip_CONVERT_CAPTAIN");
			}

			@Override
			public int aiPriority(Empire e, Spy spy, City city, Airship ship, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = 15;
				Relationship rel = wm.getRelationship(e, victim);
				if (ship.getCost() > 2000) { q += 5; }
				switch (rel.level) {
					case ALLIANCE:
					case DEFENSIVE_PACT:
						q -= 30;
						break;
					case WAR:
						if (wm.getFleetStrength(victim) > wm.getFleetStrength(e) * 2) {
							q += 10;
						}
						break;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, Airship ship, WorldMap wm) {
				return successChance(ship, spy, e, city, wm) > 70;
			}
		},
		DESERTION {
			private MonsterNest nest(Airship ship, Spy spy, City city, WorldMap wm) {
				for (MonsterNest n : city.nestsInTerritory(wm)) {
					if (ship.type.mobile && n.type != null && n.type.shipsCanJoin && (!ship.type.onGround || wm.connectedByLand(city, n))) {
						return n;
					}
				}
				return null;
			}
			
			@Override
			public boolean available(Airship ship, Spy spy, City city, WorldMap wm) {
				return nest(ship, spy, city, wm) != null;
			}
			@Override
			public int baseFailureRate(Airship ship, Spy spy, City city, WorldMap wm) {
				return 90 + ship.getCost() / 200;
			}
			@Override
			public int baseCost(Airship ship, Spy spy, City city, WorldMap wm) {
				return ship.maintenanceCost() * 10;
			}
			@Override
			public Pair<String, String> success(Airship ship, Spy spy, City city, WorldMap wm) {
				Empire owner = wm.owner(city);
				owner.remove(ship, wm);
				MonsterNest nest = nest(ship, spy, city, wm);
				Fleet nestF = new Fleet(city, wm);
				nestF.actives.add(ship);
				nest.getFleets().add(nestF);
				if (!nestF.doTravelTo(nest, wm, false)) {
					System.out.println("Deserters cannot travel to deserter nest.");
					nest.getFleets().remove(nestF);
				}
				return p(_t("spy_ship_DESERTION_" + nest.type.name + "_success_spy", ship.getName()), _t("spy_ship_DESERTION_" + nest.type.name + "_success_victim", ship.getName(), city.name));
			}
			@Override
			public Pair<String, String> failure(Airship ship, Spy spy, City city, WorldMap wm) {
				return p(_t("spy_ship_DESERTION_failure_spy", ship.getName()), _t("spy_ship_DESERTION_failure_victim", ship.getName(), city.name));
			}
			@Override
			public int cooldown() { return 400 * 7 * 10; }
			@Override
			public int failureNetworkLoss() { return 8; }
			@Override
			public String getTooltip(Airship ship, Spy spy, City city, WorldMap wm) {
				MonsterNest nest = nest(ship, spy, city, wm);
				if (nest == null) { return "?"; }
				return _t("spy_ship_tooltip_DESERTION_" + nest.type.name);
			}

			@Override
			public int aiPriority(Empire e, Spy spy, City city, Airship ship, WorldMap wm) {
				Empire victim = wm.owner(city);
				int q = -10;
				Relationship rel = wm.getRelationship(e, victim);
				if (ship.getCost() > 2000) { q += 5; }
				switch (rel.level) {
					case ALLIANCE:
					case DEFENSIVE_PACT:
						q -= 30;
						break;
					case WAR:
						if (wm.getFleetStrength(victim) > wm.getFleetStrength(e) * 2) {
							q += 10;
						}
						if (successChance(ship, spy, e, city, wm) > 50) {
							q += 20;
						}
						break;
				}
				return q;
			}

			@Override
			public boolean aiReady(Empire e, Spy spy, City city, Airship ship, WorldMap wm) {
				return successChance(ship, spy, e, city, wm) > 65;
			}
		},
		;
		public abstract boolean available(Airship ship, Spy spy, City city, WorldMap wm);
		public abstract int baseFailureRate(Airship ship, Spy spy, City city, WorldMap wm);
		public abstract int baseCost(Airship ship, Spy spy, City city, WorldMap wm);
		public abstract Pair<String, String> success(Airship ship, Spy spy, City city, WorldMap wm);
		public abstract Pair<String, String> failure(Airship ship, Spy spy, City city, WorldMap wm);
		public abstract int cooldown();
		public abstract int failureNetworkLoss();
		public abstract int aiPriority(Empire e, Spy spy, City city, Airship ship, WorldMap wm);
		public abstract boolean aiReady(Empire e, Spy spy, City city, Airship ship, WorldMap wm);
		
		public boolean isCaptainAction() { return false; }
		
		public int successChance(Airship ship, Spy spy, Empire empire, City city, WorldMap wm) {
			if (empire.easySpyCheat) { return 100; }
			int failure = baseFailureRate(ship, spy, city, wm) - city.unrest(wm, null) - spy.networkLevel;
			int heroDefence = 0;
			Hero h = Hero.get(city, wm);
			if (h != null) {
				heroDefence = h.type.spyDefence;
				if (h.isBribed(empire)) {
					failure -= EmpireStat.BRIBED_GOVERNOR_SPY_BONUS.get(empire.bonuses);
					heroDefence = 0;
				}
			}
			Hero captain = Hero.get(ship, wm);
			if (captain != null && isCaptainAction()) {
				if (captain.nemesisEmpire == empire) {
					failure += 999;
				} else if (captain.type.blockers.intersects(empire.bonuses)) {
					failure += 999;
				}
			}
			int success = StrictMath.max(0, 100 - failure);
			int edictDefence = city.edict != null ? city.edict.spyDefence : 0;
			return StrictMath.min(100, success * (100 + EmpireStat.ESPIONAGE_SUCCESS_PERCENTAGE_BONUS.get(empire.bonuses()) - heroDefence - edictDefence - EmpireStat.ESPIONAGE_DEFENCE_PERCENTAGE_BONUS.get(wm.owner(city).bonuses())) / 100);
		}
		
		public int cost(Airship ship, Spy spy, Empire empire, City city, WorldMap wm) {
			return baseCost(ship, spy, city, wm);
		}
		
		public String getName(Airship ship, Spy spy, City city, WorldMap wm) {
			return _t("spy_ship_" + name());
		}
		
		public String getTooltip(Airship ship, Spy spy, City city, WorldMap wm) {
			return _t("spy_ship_tooltip_" + name());
		}
	}
}
