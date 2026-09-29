package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class PlagueLevel extends Loadable {
	public final BonusableValue<Boolean> visible;
	public final int unrest;
	public final int devastation;
	public final int incomeReduction;
	public final int productionReduction;
	public final int timeUntilNextLevel;
	public final String nextLevelGood;
	public final String nextLevelBad;
	public final double nextLevelGoodChance;
	public final BonusableValue<Double> nextLevelGoodChanceFromHospital;
	public final BonusableValue<Double> nextLevelBadChanceToAdjacent;
	public final String spreadsAs;
	public final BonusableValue<Double> spreadProbabilityPerWeek;
	public final String abatesTo;
	public final Clr color;

	public PlagueLevel(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		visible = BonusableValue.booleanFromJSON(o, "visible", true);
		unrest = o.optInt("unrest", 0);
		devastation = o.optInt("devastation", 0);
		incomeReduction = o.optInt("incomeReduction", 0);
		productionReduction = o.optInt("productionReduction", 0);
		timeUntilNextLevel = o.optInt("timeUntilNextLevel", 400 * 5 * 7);
		nextLevelGood = o.optString("nextLevelGood", null);
		nextLevelBad = o.optString("nextLevelBad", null);
		nextLevelGoodChance = o.getDouble("nextLevelGoodChance");
		nextLevelGoodChanceFromHospital = BonusableValue.doubleFromJSON(o, "nextLevelGoodChanceFromHospital", 0.0);
		nextLevelBadChanceToAdjacent = BonusableValue.doubleFromJSON(o, "nextLevelBadChanceToAdjacent", 0.0);
		spreadsAs = o.optString("spreadsAs", null);
		spreadProbabilityPerWeek = BonusableValue.doubleFromJSON(o, "spreadProbabilityPerWeek", 0.0);
		abatesTo = o.optString("abatesTo", null);
		JSONObject c = o.getJSONObject("color");
		color = new Clr(c.getInt("r"), c.getInt("g"), c.getInt("b"));
	}
	
	public static PlagueLevel ofName(String name) {
		return ofName(PlagueLevel.class, name);
	}
	
	public String tooltip() {
		StringBuilder sb = new StringBuilder(_t("plagueLevel_" + name));
		boolean hasLines = false;
		if (incomeReduction != 0) {
			if (!hasLines) { sb.append("\n"); hasLines = true; }
			sb.append("\n").append(_t("plague_income_loss_percent", incomeReduction));
		}
		if (productionReduction != 0) {
			if (!hasLines) { sb.append("\n"); hasLines = true; }
			sb.append("\n").append(_t("plague_production_loss", productionReduction));
		}
		if (unrest != 0) {
			if (!hasLines) { sb.append("\n"); hasLines = true; }
			sb.append("\n").append(_t("plague_unrest", unrest));
		}
		return sb.toString();
	}

	public void tick(City c, int ms, WorldMap m, Empire owner) {
		ArrayList<City> cities = m.cities();
		
		// Damage
		c.setEconomicDamage(Math.max(c.getEconomicDamage(), devastation));
		
		// Spread horribly
		if (spreadsAs != null) {
			PlagueLevel sa = ofName(spreadsAs);
			double spreadProbability = spreadProbabilityPerWeek.get(owner.bonuses) / WorldMap.DAYS_PER_WEEK / WorldMap.MS_PER_DAY * ms;
			for (int i = 0; i < cities.size(); i++) {
				City c2 = cities.get(i);
				if (c == c2) { continue; }
				if ((c2.plagueLevel == null || c2.plagueLevel.sort < sa.sort)) {
					if (m.r.nextDouble() < spreadProbability * (m.connected(c, c2) ? 1 : 1.0 / (c.dist(c2) + 1))) {
						if (sa.visible.get(owner.bonuses) && (c2.plagueLevel == null || !c2.plagueLevel.visible.get(owner.bonuses))) {
							c2.addMessage(null, MapLocation.MessageType.PLAGUE, _t("cityInfected", c2.getDisplayName()));
						}
						c2.plagueLevel = sa;
						//System.out.println(c.getDisplayName() + " spreads to " + c2.getDisplayName());
						Fleet f = m.getGarrison(c2);
						if (f != null && (f.spreadingPlague == null || sa.sort > f.spreadingPlague.sort)) {
							f.spreadingPlague = sa;
							//System.out.println("Fleet infected at " + c2.getDisplayName());
						}
					}
				}
			}
		}
		
		// If it's the end of the time, figure out if good or bad outcome
		c.plagueLevelAge += ms;
		if (c.plagueLevelAge >= timeUntilNextLevel) {
			c.plagueLevelAge -= timeUntilNextLevel;
			double goodChance = nextLevelGoodChance;
			PlagueLevel goodOutcome = nextLevelGood == null ? null : PlagueLevel.ofName(nextLevelGood);
			PlagueLevel badOutcome = nextLevelBad == null ? null : PlagueLevel.ofName(nextLevelBad);
			boolean hospital = false;
			for (int i = 0; i < c.upgrades.size(); i++) {
				if (c.upgrades.get(i).preventsPlague) {
					goodChance += nextLevelGoodChanceFromHospital.get(owner.bonuses);
					hospital = true;
					break;
				}
			}
			for (int i = 0; i < cities.size(); i++) {
				City c2 = cities.get(i);
				if (c != c2 && c2.plagueLevel != null && m.connected(c, c2)) {
					goodChance -= c2.plagueLevel.nextLevelBadChanceToAdjacent.get(m.owner(c2).bonuses);
				}
			}
			//System.out.println(c.getDisplayName() + " GoodChance " + goodChance + " hospital? " + hospital);

			PlagueLevel newLevel = m.r.nextDouble() < goodChance ? goodOutcome : badOutcome;
			//System.out.println(c.getDisplayName() + " " + c.plagueLevel.name + " -> " + (newLevel == null ? null : newLevel.name));
			if (newLevel != null && newLevel.visible.get(owner.bonuses) && !c.plagueLevel.visible.get(owner.bonuses)) {
				c.addMessage(null, MapLocation.MessageType.PLAGUE, _t("cityInfected", c.getDisplayName()));
			}
			if ((newLevel == null || !newLevel.visible.get(owner.bonuses)) && c.plagueLevel.visible.get(owner.bonuses)) {
				c.addMessage(null, MapLocation.MessageType.PLAGUE, _t("cityDeplagued", c.getDisplayName()));
			}
			c.plagueLevel = newLevel;
			if (newLevel != null) {
				c.plagueLevelAge += newLevel.timeUntilNextLevel * (m.r.nextInt(100) - 50) / 200;
			}
		}
	}
}
