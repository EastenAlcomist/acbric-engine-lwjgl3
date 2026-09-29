package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Reward {
	public final String name;
	public final String title;
	public final int spawnWeight;
	private final int money;
	private final int research;
	public final Bonus bonus;
	public final Tech.Choice tech;
	public final int rep;
	private final Img img;
	public final MonsterNestType nestType;
	public final int supplies;
	public final String specialConstructionName;
	public final int numSpecialConstructions;
	public Airship specialConstruction;
	private final String startEraModifierName;
	public final int minWinningFleetCost;
	public final int maxWinningFleetCost;
	public final String[] detailPrefixes = { "?", "?", "?" };
	public final int[] numDetails = { 1, 1, 1 };
	public final boolean onceOnly;
	public final Bonus requires;
	public final boolean requireResearch;
	public final boolean requireMonsters;
	public final ConquestToggle requireToggle;
	private final String endsEraModifierName;
	public CityUpgradeType destroyAllUpgrades;
	
	public Reward(JSONObject o) {
		this(o, null, null);
	}
	
	public Reward(JSONObject o, Img img, MonsterNestType nestType) {
		title = o.optString("title", "VICTORY");
		name = o.getString("name");
		spawnWeight = o.optInt("spawnWeight", 1);
		money = o.optInt("money", 0);
		bonus = o.has("bonus") ? Bonus.ofNameOrNull(o.getString("bonus")) : null;
		tech = o.has("tech") ? Tech.choiceOfName(o.getString("tech")) : null;
		rep = o.optInt("rep", 0);
		research = o.optInt("research", 0);
		supplies = o.optInt("supplies", 0);
		specialConstructionName = o.optString("specialConstruction", null);
		numSpecialConstructions = o.optInt("numSpecialConstructions", 1);
		startEraModifierName = o.optString("startEraModifier", null);
		minWinningFleetCost = o.optInt("minWinningFleetCost", 0);
		maxWinningFleetCost = o.optInt("maxWinningFleetCost", 0);
		onceOnly = o.optBoolean("onceOnly", false);
		requires = o.has("requires") ? Bonus.ofNameOrNull(o.getString("requires")) : null;
		requireResearch = o.optBoolean("requireResearch", false);
		requireMonsters = o.optBoolean("requireMonsters", false);
		if (o.has("requireToggle")) {
			requireToggle = ConquestToggle.valueOf(o.getString("requireToggle"));
		} else {
			requireToggle = null;
		}
		endsEraModifierName = o.optString("endsEraModifier", null);
		destroyAllUpgrades = o.has("destroyAllUpgrades") ? CityUpgradeType.ofName(o.getString("destroyAllUpgrades")) : null;
		this.nestType = nestType;
		if (o.has("img")) {
			JSONObject io = o.getJSONObject("img");
			this.img = new Img(io.getString("src"), io.optInt("x", 0), io.optInt("y", 0), io.optInt("w", 0), io.optInt("h", 0), io.optBoolean("flipped", false));
		} else {
			this.img = img;
		}
		if (o.has("details")) {
			JSONArray a = o.getJSONArray("details");
			for (int i = 0; i < a.length() && i < 3; i++) {
				detailPrefixes[i] = a.getJSONObject(i).getString("prefix");
				numDetails[i] = a.getJSONObject(i).getInt("number");
			}
		}
	}

	public int getMoney(Empire e) {
		int m = money;
		if (nestType != null && nestType.isBiological) {
			m += EmpireStat.BIO_MONSTER_EXTRA_MONEY.get(e.bonuses);
			return (int) (m * EmpireStat.BIO_MONSTER_MONEY_MULT.get(e.bonuses));
		}
		return m;
	}

	public int getResearch(Empire e) {
		if (nestType != null && nestType.isBiological) {
			return (int) (research * EmpireStat.BIO_MONSTER_RESEARCH_MULT.get(e.bonuses()));
		}
		return research;
	}
	
	public static void giveRewardTo(ArrayList<Reward> rewards, Empire e, WorldMap wm, Fleet winningFleet, int winningFleetCost, String extraInfo) {
		int totalWeight = 0;
		ArrayList<Reward> available = new ArrayList<Reward>();
		if (rewards.size() == 1) {
			available = rewards;
			totalWeight = rewards.get(0).spawnWeight;
		} else {
			for (Reward r : rewards) {
				if (r.requireResearch && wm.techSpeed.speedMultiplier == 0) {
					continue;
				}
				if (r.requireMonsters && wm.monsterity.monsterNestDensity.get(BonusSet.empty()) == 0) {
					continue;
				}
				if (r.requireToggle != null && !wm.toggles.contains(r.requireToggle)) {
					continue;
				}
				if (r.requires != null && !e.hasBonus(r.requires)) {
					continue;
				}
				if (r.getEndsEraModifier() != null && wm.eraModifier != r.getEndsEraModifier()) {
					continue;
				}
				if (r.bonus != null && e.hasBonus(r.bonus)) {
					continue;
				}
				if (r.tech != null && e.techs.contains(r.tech)) {
					continue;
				}
				if (r.minWinningFleetCost > 0 && (winningFleet == null || winningFleetCost < r.minWinningFleetCost)) {
					continue;
				}
				if (r.maxWinningFleetCost > 0 && (winningFleet == null || winningFleetCost > r.maxWinningFleetCost)) {
					continue;
				}
				if (r.getStartEraModifier() != null && (wm.skipNextEraModifierPick || wm.eraModifier == r.getStartEraModifier() || wm.prevEraModifiers.contains(r.getStartEraModifier()))) {
					continue;
				}
				if ((r.tech != null || r.research > 0) && wm.techSpeed.speedMultiplier == 0) {
					continue;
				}
				if (r.research > 0 && e.isAllResearchDone(wm)) {
					continue;
				}
				if (r.onceOnly && wm.onceOnlyRewardNames.contains(r.name)) {
					continue;
				}
				available.add(r);
				totalWeight += r.spawnWeight;
			}
			if (totalWeight == 0) {
				return;
			}
		}
		int roll = wm.r.nextInt(totalWeight);
		int typeIndex = 0;
		while (roll >= available.get(typeIndex).spawnWeight) {
			roll -= available.get(typeIndex++).spawnWeight;
		}
		Reward reward = available.get(typeIndex);
		e.setMoney(e.getMoney() + reward.getMoney(e));
		if (winningFleet != null) {
			winningFleet.changeSupply(reward.supplies);
			if (reward.specialConstructionName != null) {
				if (reward.specialConstruction == null) {
					ArrayList<Mod> mods = Mod.getEnabledMods();
					for (int i = mods.size() - 1; i >= 0; i--) {
						File f = new File(new File(mods.get(i).dir, "bonusConstructions"), reward.specialConstructionName + ".json");
						if (f.exists()) {
							try {
								reward.specialConstruction = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
								reward.specialConstruction.isBonusConstruction = true;
								break;
							} catch (IOException e2) {
								e2.printStackTrace();
							} catch (JSONException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
				if (reward.specialConstruction == null) {
					ArrayList<Expansion> exp = Expansion.enableds();
					for (int i = exp.size() - 1; i >= 0; i--) {
						File f = new File(new File(exp.get(i).getDataDir(), "bonusConstructions"), reward.specialConstructionName + ".json");
						if (f.exists()) {
							try {
								reward.specialConstruction = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
								reward.specialConstruction.isBonusConstruction = true;
								break;
							} catch (IOException e2) {
								e2.printStackTrace();
							} catch (JSONException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
				if (reward.specialConstruction == null) {
					File f = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "bonusConstructions"), reward.specialConstructionName + ".json");
					if (f.exists()) {
						try {
							reward.specialConstruction = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
							reward.specialConstruction.isBonusConstruction = true;
						} catch (IOException e2) {
							e2.printStackTrace();
						} catch (JSONException e2) {
							e2.printStackTrace();
						}
					}
				}
				if (reward.specialConstruction != null) {
					for (int i = 0; i < reward.numSpecialConstructions; i++) {
						winningFleet.reserve.add(reward.specialConstruction.clone());
					}
					if (winningFleet.location != null && e.cities.contains(winningFleet.location)) {
						winningFleet.location.layoutGarrison(winningFleet);
					}
				}
			}
		}
		if (reward.getStartEraModifier() != null && wm.eraModifier != reward.getStartEraModifier()) {
			wm.startNewEraModifier(reward.getStartEraModifier(), winningFleet, e, null);
			wm.skipNextEraModifierPick = true; // So it lasts a while.
		}
		if (wm.toggles.contains(ConquestToggle.REPUTATION)) {
			e.changeReputation(reward.rep, wm);
		}
		if (reward.bonus != null) {
			e.bonuses.add(reward.bonus);
			e.rewardedBonuses.add(reward.bonus);
		}
		if (reward.tech != null) {
			if (e.research == reward.tech) {
				e.researchPoints += e.research.cost(e, wm);
				e.checkResearchComplete(wm);
				if (e.researchedTech == reward.tech) {
					e.researchedTech = null;
				}
			} else {
				e.techs.add(reward.tech);
				e.bonuses.addAll(reward.tech.bonuses);
			}
		}
		if (e.research != null) {
			e.researchPoints += reward.getResearch(e) * 3000;
			e.checkResearchComplete(wm);
		} else {
			e.unassignedResearchPoints += reward.getResearch(e) * 3000;
		}
		e.rewardGiven = reward;
		e.rewardExtraInfo = extraInfo;
		for (int i = 0; i < 3; i++) {
			e.rewardDetails[i] = reward.detailPrefixes[i] + AGame.ANIM_R.nextInt(reward.numDetails[i]);
		}
		if (reward.destroyAllUpgrades != null) {
			for (City c : wm.cities()) {
				c.upgrades.remove(reward.destroyAllUpgrades);
				for (City.ConstructionEntry ce : c.constructing) {
					if (ce.upgrade == reward.destroyAllUpgrades) {
						c.constructing.remove(ce);
					}
				}
			}
		}
		if (reward.onceOnly) {
			wm.onceOnlyRewardNames.add(reward.name);
		}
		if (wm.eraModifier  == reward.getEndsEraModifier()) {
			ArrayList<Empire> enders = new ArrayList<Empire>();
			enders.add(e);
			wm.eraModifier.endPrematurely(wm, enders);
		}
		//System.out.println("Giving reward " + reward.name + " to " + e.name);
	}

	public EraModifier getStartEraModifier() {
		return startEraModifierName == null ? null : EraModifier.ofName(startEraModifierName);
	}

	public EraModifier getEndsEraModifier() {
		return endsEraModifierName == null ? null : EraModifier.ofName(endsEraModifierName);
	}

	public Img getImg() {
		return nestType != null && nestType.isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? null : img;
	}
}
