package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.json.JSONArray;

public class EraModifier extends Loadable {
	public final Bonus bonus;
	
	public Img eraIcon;
	public Img eraImg;
	
	public int minStartTime;
	
	public CityUpgradeType eraStartSpawnUpgrade;
	public int eraStartSpawnNumUpgrades;
	public Bonus doNotSpawnUpgradeInEmpiresWithBonus;
	public MonsterNestType eraStartSpawnNest;
	public ArrayList<MonsterNestType> eraStartSpawnAdditionalNests;
	public double eraStartSpawnAdditionalNestsProportion;
	public PlagueLevel eraStartInfectCity;
	
	public boolean eraEndsWhenControllingUpgrades;
	public boolean eraEndsWhenClearingNest;
	public CityUpgradeType eraEndsWhenUpgradesBuilt;
	public double eraEndsWhenProportionHasUpgrade;
	public Tech.Choice eraEndsWhenResearched;
	public BonusableValue<Reward> eraEnderReward;
	public BonusableValue<BonusableValue<Reward>> eraNonEnderReward;
	public BonusableValue<String> desc;
	public BonusableValue<String> effectsDesc;
	public String endEraMeter;
	public Img endEraMeterIcon;
	public int pattern;
	
	public BonusableValue<ArrayList<Reward>> expeditions;
	public BonusableValue<Integer> expeditionLowRewardsBoundary;
	public BonusableValue<Integer> expeditionMediumRewardsBoundary;
	public BonusableValue<Integer> expeditionHighRewardsBoundary;
	public BonusableValue<Integer> expeditionVeryHighRewardsBoundary;
	
	public boolean requireResearch;
	public boolean requireMonsters;
	public ConquestToggle requireToggle;
			
	public Empire hasBeenEnded(WorldMap m) {
		if (endEraMeter != null && m.endEraMeter >= 100 && m.endEraMeterIncreaseEmpire != null) {
			m.endEraMeter = 0;
			return m.endEraMeterIncreaseEmpire;
		}
		if (eraEndsWhenControllingUpgrades) {
			for (Empire e : m.empires) {
				int numUpgradesControlled = 0;
				for (City c : e.cities) {
					if (c.upgrades.contains(eraStartSpawnUpgrade)) {
						numUpgradesControlled++;
						if (numUpgradesControlled >= eraStartSpawnNumUpgrades) {
							return e;
						}
					}
				}
			}
		}
		if (eraEndsWhenUpgradesBuilt != null) {
			int totalNumUpgradesBuilt = 0;
			int totalCities = 0;
			for (Empire e : m.empires) {
				for (City c : e.cities) {
					if (c.upgrades.contains(eraEndsWhenUpgradesBuilt)) {
						totalNumUpgradesBuilt++;
					}
				}
				totalCities += e.cities.size();
			}
			if (totalNumUpgradesBuilt >= StrictMath.ceil(eraEndsWhenProportionHasUpgrade * totalCities)) {
				Empire best = null;
				int bestNumUpgradesBuilt = 0;
				for (Empire e : m.empires) {
					int numUpgradesBuilt = 0;
					for (City c : e.cities) {
						if (c.upgrades.contains(eraEndsWhenUpgradesBuilt)) {
							numUpgradesBuilt++;
						}
					}
					if (numUpgradesBuilt > bestNumUpgradesBuilt) {
						best = e;
						bestNumUpgradesBuilt = numUpgradesBuilt;
					}
				}
				return best;
			}
		}
		if (eraEndsWhenResearched != null) {
			for (Empire e : m.empires) {
				if (e.techs.contains(eraEndsWhenResearched)) {
					return e;
				}
			}
		}
		return null;
	}
	
	public String scoreDetailString(WorldMap map, Empire viewer) {
		if (eraEndsWhenControllingUpgrades && eraStartSpawnNumUpgrades > 1) {
			int viewerNumUpgrades = 0;
			StringBuilder lbsb = new StringBuilder();
			for (Empire e : map.empires) {
				int empireNumUpgrades = 0;
				for (City c : e.cities) {
					if (c.upgrades.contains(eraStartSpawnUpgrade)) {
						empireNumUpgrades++;
						if (e == viewer) { viewerNumUpgrades++; }
					}
				}
				if (empireNumUpgrades > 0) {
					lbsb.append("\n");
					lbsb.append(e.getName()).append(": ").append(empireNumUpgrades);
				}
			}

			return "\n\n" + _t("x_y_z_upgrades_to_end_the_age", viewerNumUpgrades, eraStartSpawnNumUpgrades, _t("cityUpgrade_plural_" + eraStartSpawnUpgrade.name), _t("bonus_" + name)) + lbsb.toString();
		}
		
		/*if (endEraMeter != null) {
			return "\n\n" + _t("x_y_z_upgrades_to_end_the_age", map.endEraMeter, 100, _t(endEraMeter), _t("bonus_" + name));
		}*/
		
		if (eraEndsWhenUpgradesBuilt != null) {
			//int totalNumUpgrades = 0;
			Empire leadingEmpire = null;
			int leadingEmpireNumUpgrades = 0;
			for (Empire e : map.empires) {
				int empireNumUpgrades = 0;
				for (City c : e.cities) {
					if (c.upgrades.contains(eraEndsWhenUpgradesBuilt)) {
						//totalNumUpgrades++;
						empireNumUpgrades++;
					}
				}
				if (empireNumUpgrades > leadingEmpireNumUpgrades) {
					leadingEmpire = e;
					leadingEmpireNumUpgrades = empireNumUpgrades;
				}
			}
			StringBuilder lbsb = new StringBuilder();
			for (Empire e : map.empires) {
				int empireNumUpgrades = 0;
				for (City c : e.cities) {
					if (c.upgrades.contains(eraEndsWhenUpgradesBuilt)) {
						empireNumUpgrades++;
					}
				}
				if (empireNumUpgrades > 0) {
					lbsb.append("\n");
					lbsb.append(e == leadingEmpire ? MyDraw.SELECTED_C : "[]");
					lbsb.append(e.getName()).append(": ").append(empireNumUpgrades);
				}
			}
			if (lbsb.length() == 0) {
				lbsb.append("\n").append(_t("Nothing_Built"));
			}
			//int requiredUpgrades = (int) StrictMath.ceil(map.cities().size() * eraEndsWhenProportionHasUpgrade);
			return "\n\n" + _t("cityUpgrade_plural_" + eraEndsWhenUpgradesBuilt.name) + ":" + lbsb.toString();
		}
		
		return "";
	}
	
	public void endPrematurely(WorldMap m, ArrayList<Empire> enders) {
		HashMap<Empire, String> scoreDetails = new HashMap<Empire, String>();
		for (Empire e : m.empires) {
			scoreDetails.put(e, scoreDetailString(m, e));
		}
		m.eraModifier = EraModifier.ofName("NO_BONUS");
		m.eraModifier.start(m, null, null, null);
		m.skipNextEraModifierPick = false;
		ArrayList<Reward> rews = new ArrayList<Reward>();
		for (Empire e : m.empires) {
			rews.clear();
			if (enders.contains(e)) {
				if (eraEnderReward.get(e.bonuses) == null) { continue; }
				rews.add(eraEnderReward.get(e.bonuses));
			} else {
				if (eraNonEnderReward.get(e.bonuses) == null || eraNonEnderReward.get(e.bonuses).get(enders.get(0).bonuses) == null) {
					continue;
				}
				rews.add(eraNonEnderReward.get(e.bonuses).get(enders.get(0).bonuses));
			}
			Reward.giveRewardTo(rews, e, m, null, 0, scoreDetails.get(e));
			e.rewardWinnerEmpires = enders;
		}
	}
	
	public void start(final WorldMap m, Fleet startingFleet, Empire startingEmpire, City startingCity) {
		ArrayList<City> targetCities = m.cities();
		Collections.shuffle(targetCities, m.r.getRandom());
		Collections.sort(targetCities, new Comparator<City>() {
			@Override
			public int compare(City o1, City o2) {
				return m.owner(o1).cities.size() - m.owner(o2).cities.size();
			}
		});
		if (eraStartSpawnUpgrade != null) {
			int spawned = 0;
			if (startingCity != null || (startingFleet != null && startingFleet.location instanceof City)) {
				City c = startingCity != null ? startingCity : (City) startingFleet.location;
				if (!c.upgrades.contains(eraStartSpawnUpgrade)) {
					c.upgrades.add(eraStartSpawnUpgrade);
					c.addMessage(null, MapLocation.MessageType.NEW_UPGRADE, _t("eraStartSpawnUpgrade_" + name));
					m.newEraDetail[0] = c.getDisplayName();
					spawned++;
				}
			}
			for (int ignoreBonusRestriction = 0; ignoreBonusRestriction < 2; ignoreBonusRestriction++) {
				for (int i = 0; i < targetCities.size() && spawned < eraStartSpawnNumUpgrades; i++) {
					City c = targetCities.get(i);
					if (ignoreBonusRestriction == 0 && doNotSpawnUpgradeInEmpiresWithBonus != null && m.owner(c).bonuses.contains[doNotSpawnUpgradeInEmpiresWithBonus.ordinal()]) {
						continue;
					}
					if (!c.upgrades.contains(eraStartSpawnUpgrade) && c.isTown == eraStartSpawnUpgrade.forTown) {
						c.upgrades.add(eraStartSpawnUpgrade);
						c.addMessage(null, MapLocation.MessageType.NEW_UPGRADE, _t("eraStartSpawnUpgrade_" + name));
						if (m.newEraDetail.length > spawned) {
							m.newEraDetail[spawned] = c.getDisplayName();
						}
						spawned++;
					}
				}
			}
		}
		if (eraStartSpawnNest != null) {
			MonsterNest target = null;
			for (MonsterNest n : m.nests) {
				if (n.type == eraStartSpawnNest) {
					target = n;
					break;
				}
			}
			if (target == null) {
				for (MonsterNest n : m.nests) {
					if (n.type != null && n.type.upgradeTo() == eraStartSpawnNest) {
						target = n;
						break;
					}
				}
			}
			if (target == null) {
				for (MonsterNest n : m.nests) {
					if (n.type == null) {
						target = n;
						break;
					}
				}
			}
			if (target == null) {
				target = m.nests.get(0);
			}
			
			target.occupy(eraStartSpawnNest, m);
			City c = m.getCity(m.cityOwnership[target.y][target.x]);
			if (c == null) {
				ArrayList<City> cities = m.cities();
				for (int i = 0; i < cities.size(); i++) {
					if (c == null || cities.get(i).dist(target) < c.dist(target)) {
						c = cities.get(i);
					}
				}
			}
			if (c != null) {
				m.newEraDetail[0] = c.getDisplayName();
			}
		}
		if (eraStartSpawnAdditionalNests != null) {
			for (MonsterNest mn : m.nests) {
				if (mn.type == null && m.r.nextDouble() < eraStartSpawnAdditionalNestsProportion) {
					ArrayList<MonsterNestType> possibleNests = new ArrayList<MonsterNestType>();
					for (MonsterNestType mnt : eraStartSpawnAdditionalNests) {
						if (mnt.isSpider && m.arachnophobia) {
							continue;
						}
						if (!mnt.needsRoad || m.connectedToAnywhere(mn)) {
							possibleNests.add(mnt);
						}
					}
					if (!possibleNests.isEmpty()) {
						MonsterNestType t = possibleNests.size() == 1 ? possibleNests.get(0) : possibleNests.get(m.r.nextInt(possibleNests.size()));
						mn.occupy(t, m);
					}
				}
			}
		}
		if (eraStartInfectCity != null) {
			City c;
			if (startingFleet != null && startingFleet.location instanceof City) {
				c = (City) startingFleet.location;
			} else if (startingEmpire != null) {
				c = startingEmpire.getCapital();
			} else {
				c = targetCities.get(targetCities.size() - 1); // Somewhere in the biggest empire.
			}
			c.plagueLevel = eraStartInfectCity;
			c.plagueLevelAge = c.plagueLevel.timeUntilNextLevel / 3;
			c.addMessage(null, MapLocation.MessageType.PLAGUE, _t("cityInfected", c.getDisplayName()));
			Fleet g = m.getGarrison(c);
			if (g != null && c.plagueLevel.spreadsAs != null) {
				g.spreadingPlague = PlagueLevel.ofName(c.plagueLevel.spreadsAs);
			}
			m.newEraDetail[0] = c.getDisplayName();
		} else {
			for (City c : m.cities()) {
				if (c.plagueLevel != null) {
					c.plagueLevel = c.plagueLevel.abatesTo == null ? null : PlagueLevel.ofName(c.plagueLevel.abatesTo);
					c.plagueLevelAge = m.r.nextInt(30000);
				}
			}
			for (Empire e : m.empires) {
				for (Fleet f : e.getFleets()) {
					f.spreadingPlague = null;
				}
			}
			for (MonsterNest n : m.nests) {
				for (Fleet f : n.getFleets()) {
					f.spreadingPlague = null;
				}
			}
		}
	}
	
	public EraModifier(JSONObject o) {
		super(o.getString("name"));
		minStartTime = o.optInt("minStartTime", 0);
		requireResearch = o.optBoolean("requireResearch", false);
		requireMonsters = o.optBoolean("requireMonsters", false);
		if (o.has("requireToggle")) {
			requireToggle = ConquestToggle.valueOf(o.getString("requireToggle"));
		}
		
		bonus = Bonus.ofName(name);
		if (o.has("eraIcon")) {
			JSONObject io = o.getJSONObject("eraIcon");
			eraIcon = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.optInt("w", 16), io.optInt("h", 16), io.optBoolean("flipped", false));
		}
		if (o.has("eraImg")) {
			JSONObject io = o.getJSONObject("eraImg");
			eraImg = new Img(io.getString("src"), io.optInt("x", 0), io.optInt("y", 0), io.optInt("w", 400), io.optInt("h", 300), io.optBoolean("flipped", false));
		}
		
		pattern = o.optInt("pattern", -1);
		eraStartSpawnUpgrade = o.has("eraStartSpawnUpgrade") ? CityUpgradeType.ofName(o.getString("eraStartSpawnUpgrade")) : null;
		eraStartSpawnNumUpgrades = o.optInt("eraStartSpawnNumUpgrades", 1);
		doNotSpawnUpgradeInEmpiresWithBonus = o.has("doNotSpawnUpgradeInEmpiresWithBonus") ? Bonus.ofName(o.getString("doNotSpawnUpgradeInEmpiresWithBonus")) : null;
		eraStartSpawnNest = o.has("eraStartSpawnNest") ? MonsterNestType.ofName(o.getString("eraStartSpawnNest")) : null;
		if (o.has("eraStartSpawnAdditionalNests")) {
			JSONArray a = o.getJSONArray("eraStartSpawnAdditionalNests");
			eraStartSpawnAdditionalNests = new ArrayList<MonsterNestType>();
			for (int i = 0; i < a.length(); i++) {
				eraStartSpawnAdditionalNests.add(MonsterNestType.ofName(a.getString(i)));
			}
			eraStartSpawnAdditionalNestsProportion = o.getDouble("eraStartSpawnAdditionalNestsProportion");
		}
		eraStartInfectCity = o.has("eraStartInfectCity") ? PlagueLevel.ofName(o.getString("eraStartInfectCity")) : null;
		eraEndsWhenControllingUpgrades = o.optBoolean("eraEndsWhenControllingUpgrades", false);
		eraEndsWhenClearingNest = o.optBoolean("eraEndsWhenClearingNest", false);
		eraEndsWhenUpgradesBuilt = o.has("eraEndsWhenUpgradesBuilt") ? CityUpgradeType.ofName(o.getString("eraEndsWhenUpgradesBuilt")) : null;
		eraEndsWhenProportionHasUpgrade = o.optDouble("eraEndsWhenProportionHasUpgrade", 0.2);
		eraEndsWhenResearched = o.has("eraEndsWhenResearched") ? Tech.choiceOfName(o.getString("eraEndsWhenResearched")) : null;
		eraEnderReward = BonusableValue.objectFromJSON(o, "eraEnderReward", null, new BonusableValue.FromJSON<Reward>() {
			@Override
			public Reward construct(JSONObject o, BonusSet bs) {
				return new Reward(o);
			}
		});
		eraNonEnderReward = BonusableValue.objectFromJSON(o, "eraNonEnderReward", null, new BonusableValue.BonusableObjectFromJSON<Reward>(new BonusableValue.FromJSON<Reward>() {
			@Override
			public Reward construct(JSONObject o, BonusSet bs) {
				return new Reward(o);
			}
		}));
		expeditions = BonusableValue.listFromJSON(o, "expeditions", null, new BonusableValue.FromJSON<Reward>() {
			@Override
			public Reward construct(JSONObject o, BonusSet bs) {
				return new Reward(o);
			}
		});
		effectsDesc = BonusableValue.stringFromJSON(o, "effectsDesc", "age_fx_" + name);
		desc = BonusableValue.stringFromJSON(o, "desc", "age_desc_" + name);
		if (o.has("endEraMeter")) {
			endEraMeter = o.getString("endEraMeter");
			JSONObject io = o.getJSONObject("endEraMeterIcon");
			endEraMeterIcon = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.optInt("w", 16), io.optInt("h", 16), io.optBoolean("flipped", false));
		}
		
		expeditionLowRewardsBoundary = BonusableValue.intFromJSON(o, "expeditionLowRewardsBoundary", 500);
		expeditionMediumRewardsBoundary = BonusableValue.intFromJSON(o, "expeditionMediumRewardsBoundary", 1500);
		expeditionHighRewardsBoundary = BonusableValue.intFromJSON(o, "expeditionHighRewardsBoundary", 3000);
		expeditionVeryHighRewardsBoundary = BonusableValue.intFromJSON(o, "expeditionVeryHighRewardsBoundary", 6000);
	}
		
	public static EraModifier ofName(String name) {
		return ofName(EraModifier.class, name);
	}
}
