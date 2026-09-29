package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;

/** A temporary change to city stats caused by a governor. */
public strictfp class Edict extends Loadable  {
	public final Img icon;
	public final Img iconBackground;
	public final String sound;
	public final double volume;
	public final int duration;
	public final int money;
	public final int instantResearch;
	public final int rep;
	public final int pillaging;
	public final String stat;
	public final int statChange;
	public final String stat2;
	public final int statChange2;
	public final String comment;
	public final boolean cityOnly;
	
	public final int unrest;
	public final int spyDefence;
	public final int production;
	public final int defenceBudget;
	public final int incomePercent;
	public final int research;
	public final int upgradeCostPercent;
	public final int upgradeSpeedPercent;
	public final int airshipSpeedPercent;
	public final int landshipSpeedPercent;
	public final int buildingSpeedPercent;
	
	public final boolean aiEnactIfPossible;
	
	public Edict(JSONObject o) {
		super(o.getString("name"));
		JSONObject io = o.getJSONObject("icon");
		icon = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.optInt("w", 16), io.optInt("h", 16), io.optBoolean("flipped", false));
		io = o.getJSONObject("iconBackground");
		iconBackground = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.optInt("w", 16), io.optInt("h", 16), io.optBoolean("flipped", false));	
		duration = o.getInt("duration");
		sound = o.optString("sound", null);
		volume = o.optDouble("volume", 1);
		
		money = o.optInt("money", 0);
		instantResearch = o.optInt("instantResearch", 0);
		rep = o.optInt("rep", 0);
		pillaging = o.optInt("pillaging", 0);
		cityOnly = o.optBoolean("cityOnly", false);
		if (o.has("stat")) {
			stat = o.getString("stat");
			statChange = o.getInt("statChange");
		} else {
			stat = null;
			statChange = 0;
		}
		if (o.has("stat2")) {
			stat2 = o.getString("stat2");
			statChange2 = o.getInt("statChange2");
		} else {
			stat2 = null;
			statChange2 = 0;
		}
		
		unrest = o.optInt("unrest", 0);
		spyDefence = o.optInt("spyDefence", 0);
		production = o.optInt("production", 0);
		defenceBudget = o.optInt("defenceBudget", 0);
		incomePercent = o.optInt("incomePercent", 0);
		research = o.optInt("research", 0);
		upgradeCostPercent = o.optInt("upgradeCostPercent", 0);
		upgradeSpeedPercent = o.optInt("upgradeSpeedPercent", 0);
		airshipSpeedPercent = o.optInt("airshipSpeedPercent", 0);
		landshipSpeedPercent = o.optInt("landshipSpeedPercent", 0);
		buildingSpeedPercent = o.optInt("buildingSpeedPercent", 0);
		comment = o.optString("comment", null);
		aiEnactIfPossible = o.optBoolean("aiEnactIfPossible", false);
	}
	
	public int getSpeedPercent(ShipType t) {
		switch (t) {
			case AIRSHIP: return airshipSpeedPercent;
			case LANDSHIP: return landshipSpeedPercent;
			case BUILDING: return buildingSpeedPercent;
			default: return 0;
		}
	}
	
	private Hero.StatChange getStatChange(Hero h) {
		if (stat == null) { return null; }
		HeroType.Stat s = h.type.getStat(stat);
		if (s == null) { return null; }
		return new Hero.StatChange(h, new HeroType.Stat.Changer(statChange, -100, 100, 0, null, s));
	}
	
	private Hero.StatChange getStatChange2(Hero h) {
		if (stat2 == null) { return null; }
		HeroType.Stat s = h.type.getStat(stat2);
		if (s == null) { return null; }
		return new Hero.StatChange(h, new HeroType.Stat.Changer(statChange2, -100, 100, 0, null, s));
	}
	
	public String getName() { return _t("edict_" + name); }
	
	public String getDesc(Hero h, WorldMap m) {
		return getDesc(h, null, null, null, m);
	}
	
	public String getDesc(Hero h, Empire e, City c, CampaignWorld w, WorldMap m) {
		StringBuilder sb = new StringBuilder();
		if (w != null) {
			sb.append(_t("edict_duration", w.describeTime(duration)));
		} else {
			sb.append(_t("edict_duration", WorldMap.describeTime(duration, BonusSet.empty())));
		}
		if (e != null && e.getMoney() + money < 0) {
			sb.append("\n").append(_t("edict_insufficient_money"));
		}
		if (c != null && c.getEconomicDamage() + pillaging > City.MAX_ECON_DAMAGES) {
			sb.append("\n").append(_t("edict_insufficient_pillage"));
		}
		if (cityOnly) {
			sb.append("\n").append(_t("edict_cities_only"));
		}
		
		if (money != 0 || rep != 0 || instantResearch != 0 || pillaging != 0 || (h != null && (getStatChange(h) != null || !(e != null && e.getRepChangeHeroAppendix(rep, w.map, false).isEmpty())))) {
			sb.append("\n\n").append(_t("edict_when_activated"));
		}
		if (money > 0) {
			sb.append("\n").append(_t("edict_money", money));
		}
		if (money < 0) {
			sb.append("\n").append(_t("edict_cost", -money));
		}
		if (rep != 0) {
			sb.append("\n").append(_t("edict_rep", (rep > 0 ? "+" : "") + rep));
		}
		if (pillaging != 0) {
			sb.append("\n").append(_t("edict_pillaging", (pillaging > 0 ? "+" : "") + pillaging));
		}
		if (instantResearch != 0) {
			sb.append("\n").append(_t("edict_research", (instantResearch > 0 ? "+" : "") + instantResearch));
		}
		if (h != null) {
			Hero.StatChange sc = getStatChange(h);
			if (sc != null) {
				sb.append("\n").append(sc.getText(false, m));
			}
			Hero.StatChange sc2 = getStatChange2(h);
			if (sc2 != null) {
				sb.append("\n").append(sc2.getText(false, m));
			}
			if (e != null) {
				sb.append(e.getRepChangeHeroAppendix(rep, m, false));
			}
		}
		if (unrest != 0 || spyDefence != 0 || production != 0 || defenceBudget != 0 || incomePercent != 0 || research != 0 || upgradeCostPercent != 0 || upgradeSpeedPercent != 0 || airshipSpeedPercent != 0 || landshipSpeedPercent != 0 || buildingSpeedPercent != 0) {
			sb.append("\n\n").append(_t("edict_while_active"));
		}
		if (unrest != 0) {
			sb.append("\n").append(_t("hero_unrest", (unrest > 0 ? "+" : "") + unrest));
		}
		if (spyDefence != 0) {
			sb.append("\n").append(_t("hero_spyDefence", (spyDefence > 0 ? "+" : "") + spyDefence));
		}
		if (production != 0) {
			sb.append("\n").append(_t("hero_production", (production > 0 ? "+" : "") + production));
		}
		if (defenceBudget != 0) {
			sb.append("\n").append(_t("hero_defenceBudget", (defenceBudget > 0 ? "+" : "") + defenceBudget));
		}
		if (incomePercent != 0) {
			sb.append("\n").append(_t("hero_incomePercent", (incomePercent > 0 ? "+" : "") + incomePercent));
		}
		if (research != 0) {
			sb.append("\n").append(_t("hero_researchPercent", (research > 0 ? "+" : "") + research));
		}
		if (upgradeCostPercent != 0) {
			sb.append("\n").append(_t("hero_upgradeCostPercent", (upgradeCostPercent > 0 ? "+" : "") + upgradeCostPercent));
		}
		if (upgradeSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_upgradeSpeedPercent", (upgradeSpeedPercent > 0 ? "+" : "") + upgradeSpeedPercent));
		}
		if (airshipSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_airshipSpeedPercent", (airshipSpeedPercent > 0 ? "+" : "") + airshipSpeedPercent));
		}
		if (landshipSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_landshipSpeedPercent", (landshipSpeedPercent > 0 ? "+" : "") + landshipSpeedPercent));
		}
		if (buildingSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_buildingSpeedPercent", (buildingSpeedPercent > 0 ? "+" : "") + buildingSpeedPercent));
		}
		return sb.toString();
	}
	
	public static Edict ofName(String name) {
		return ofName(Edict.class, name);
	}
}
