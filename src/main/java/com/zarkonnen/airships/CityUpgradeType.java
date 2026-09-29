package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class CityUpgradeType extends Loadable implements HasName {
	public final boolean forTown;
	public final boolean special;
	
	public final BonusableValue<Integer> research;
	public final BonusableValue<Integer> production;
	public final BonusableValue<Integer> income;
	public final BonusableValue<Integer> unrest;
	public final BonusableValue<Integer> reputation;
	public final BonusableValue<Integer> localSupply;
	public final BonusableValue<Integer> globalSupply;
	/**
	 * 防御预算加成。
	 *
	 * <p>本字段是游戏 1.2.15.2 新增、迁移基线 1.2.14 没有的。回填它的原因是二进制的：
	 * asplit-*.zip 里的 1.2.15.2 {@code City} / {@code HeroManagementAI} 会
	 * {@code getfield CityUpgradeType.defenceBudget}，缺失即 {@code NoSuchFieldError}。
	 * 取值与默认值按 1.2.15.2 字节码还原（{@code BonusableValue.intFromJSON(o,"defenceBudget",0)}），
	 * 描述行使用同一版本里的 lang key {@code local_defence_budget}。</p>
	 */
	public final BonusableValue<Integer> defenceBudget;
	public final String requiredUpgradesName;
	public final int numRequiredUpgrades;
	public final boolean enableShipBuilding;
	public final boolean preventsPlague;
	public final boolean removable;
	public final Bonus consumesBonus;
	public final Bonus gives;
	public final String givesTechName;
	public Tech.Choice givesTech;
	public final boolean allowBuildingDespiteGivenBonus;
	public final boolean canBuild;
	public final BonusableValue<Img> icon;
	public final BonusableValue<Img> icon32;
	public final boolean hintToBuild;
	
	public final BonusableValue<Integer> extraCost;
	public final BonusableValue<Integer> constructionTime;
	public final BonusableValue<Integer> maintenance;
	
	public final boolean requiresCoast;
	public final boolean requiresNoCoast;
	public final BonusSet requires;
	public final ConquestToggle requiresToggle;
	
	public final boolean aiBuildIfPossible;
		
	public CityUpgradeType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		special = o.optBoolean("special", false);
		forTown = o.optBoolean("forTown", special);
		research = BonusableValue.intFromJSON(o, "research", 0);
		production = BonusableValue.intFromJSON(o, "production", 0);
		income = BonusableValue.intFromJSON(o, "income", 0);
		unrest = BonusableValue.intFromJSON(o, "unrest", 0);
		reputation = BonusableValue.intFromJSON(o, "reputation", 0);
		localSupply = BonusableValue.intFromJSON(o, "localSupply", 0);
		globalSupply = BonusableValue.intFromJSON(o, "globalSupply", 0);
		defenceBudget = BonusableValue.intFromJSON(o, "defenceBudget", 0);
		enableShipBuilding = o.optBoolean("enableShipBuilding", false);
		extraCost = BonusableValue.intFromJSON(o, "extraCost", 0);
		constructionTime = BonusableValue.intFromJSON(o, "constructionTime", 400 * 28 * 6); // Six months
		requiresCoast = o.optBoolean("requiresCoast", false);
		requiresNoCoast = o.optBoolean("requiresNoCoast", false);
		preventsPlague = o.optBoolean("preventsPlague", false);
		removable = o.optBoolean("removable", !special);
		hintToBuild = o.optBoolean("hintToBuild", false);
		allowBuildingDespiteGivenBonus = o.optBoolean("allowBuildingDespiteGivenBonus", false);
		if (o.has("consumesBonus")) {
			consumesBonus = Bonus.ofName(o.getString("consumesBonus"));
		} else {
			consumesBonus = null;
		}
		requiredUpgradesName = o.optString("requiredUpgrades", null);
		numRequiredUpgrades = o.optInt("numRequiredUpgrades", 0);
		canBuild = o.optBoolean("canBuild", !special);
		requires = new BonusSet();
		if (o.has("requires")) {
			JSONArray a = o.getJSONArray("requires");
			for (int i = 0; i < a.length(); i++) {
				requires.set(Bonus.ofName(a.getString(i)).ordinal(), true);
				//System.out.println(name + " req " + Bonus.ofName(a.getString(i)).ordinal() + " " + Bonus.ofName(a.getString(i)).name + " " + requires.contains[Bonus.ofName(a.getString(i)).ordinal()]);
			}
		}
		if (o.has("requiresToggle")) {
			requiresToggle = ConquestToggle.valueOf(o.getString("requiresToggle"));
		} else {
			requiresToggle = null;
		}
		if (o.has("gives")) {
			gives = Bonus.ofName(o.getString("gives"));
		} else {
			gives = null;
		}
		givesTechName = o.optString("givesTech", null);
		
		icon = BonusableValue.objectFromJSON(o, "icon", null, BonusableValue.ImgFromJSON(16, 16));
		icon32 = BonusableValue.objectFromJSON(o, "icon32", null, BonusableValue.ImgFromJSON(32, 32));
		
		if (special) {
			maintenance = BonusableValue.of(0);
		} else {
			maintenance = BonusableValue.intFromJSON(o, "maintenance", income.get(BonusSet.empty()) == 0 ? (forTown ? 3 : 10) : 0);
		}
		
		aiBuildIfPossible = o.optBoolean("aiBuildIfPossible", false);
	}
	
	private void finish() {
		try {
			if (givesTechName != null) {
				givesTech = Tech.choiceOfName(givesTechName);
			}
		} catch (Exception e) {
			String msg = "Unable to finish loading CityUpgradeType " + name + ": " + e.getMessage();
			if (sourceMod != null) {
				sourceMod.buildFailed = true;
				sourceMod.buildLog = sourceMod.buildLog == null ? msg : sourceMod.buildLog + "\n[bb421d]" + msg;
			}
			throw new RuntimeException(msg, e);
		}
	}
	
	public static void postLoad() {
		for (CityUpgradeType t : all(CityUpgradeType.class)) { t.finish(); }
	}
	
	public CityUpgradeType requiredUpgrades() {
		return requiredUpgradesName == null ? null : CityUpgradeType.ofName(requiredUpgradesName);
	}
	
	public String getName() {
		return _t("cityUpgrade_" + name);
	}
	
	public String getDesc(BonusSet bs, WorldMap wm, boolean enabled) {
		StringBuilder sb = new StringBuilder();
		if (Lang.hasLocalString("cityUpgrade_desc_" + name)) {
			sb.append(_t("cityUpgrade_desc_" + name)).append("\n");
			if (special && gives != null) {
				sb.append("\n").append(_t("bonus_" + gives.name + "_desc"));
			}
		} else if (gives != null) {
			sb.append(gives.getDesc()).append("\n");
		}
		if (!enabled) {
			sb.append("\n").append(_t("no_control_upgrade_disabled_notice")).append("\n");
		}
		if (numRequiredUpgrades > 0 && requiredUpgrades() != null) {
			sb.append("\n").append(_t("requires_x_y_upgrades_to_be_built", numRequiredUpgrades, requiredUpgrades().getName()));
		}
		if (consumesBonus != null) {
			sb.append("\n").append(_t("uses_up_bonus_x", consumesBonus.getName()));
		}
		if (givesTech != null) {
			sb.append("\n").append(_t("Technology_")).append(givesTech.getName());
		}
		if (maintenance.get(bs) != 0) {
			sb.append("\n").append(maintenance.get(bs)).append(" ").append(_t("upgrade_maintenance"));
		}
		if (research.get(bs) != 0 && bs.contains[Bonus.ofName("RESEARCH").ordinal()]) {
			sb.append("\n").append(research.get(bs) > 0 ? "+" : "").append(research.get(bs)).append(" ").append(_t("upgrade_research"));
		}
		if (production.get(bs) != 0) {
			int base = forTown ? EmpireStat.TOWN_PRODUCTION.get(BonusSet.empty()) : EmpireStat.CITY_PRODUCTION.get(BonusSet.empty());
			int percentage = production.get(bs) * 100 / base;
			sb.append("\n").append(percentage > 0 ? "+" : "").append(percentage).append("% ").append(_t("upgrade_production"));
		}
		if (income.get(bs) != 0) {
			sb.append("\n").append(income.get(bs) > 0 ? "+" : "").append(income.get(bs)).append(" ").append(_t("upgrade_income"));
		}
		if (unrest.get(bs) != 0) {
			sb.append("\n").append(unrest.get(bs) > 0 ? "+" : "").append(unrest.get(bs)).append(" ").append(_t("upgrade_unrest"));
		}
		if (defenceBudget.get(bs) != 0) {
			sb.append("\n").append(defenceBudget.get(bs) > 0 ? "+" : "").append(defenceBudget.get(bs)).append(" ").append(_t("local_defence_budget"));
		}
		if (wm.toggles.contains(ConquestToggle.REPUTATION)) {
			if (reputation.get(bs) != 0) {
				sb.append("\n").append(reputation.get(bs) > 0 ? "+" : "").append(reputation.get(bs)).append(" ").append(_t("upgrade_reputation"));
			}
		}
		if (wm.toggles.contains(ConquestToggle.SUPPLY)) {
			if (localSupply.get(bs) != 0) {
				sb.append("\n").append(localSupply.get(bs) > 0 ? "+" : "").append(localSupply.get(bs)).append(" ").append(_t("upgrade_supply"));
			}
			if (globalSupply.get(bs) != 0) {
				sb.append("\n").append(globalSupply.get(bs) > 0 ? "+" : "").append(globalSupply.get(bs)).append(" ").append(_t("upgrade_global_supply"));
			}
		}
		if (enableShipBuilding) {
			sb.append("\n").append(_t("upgrade_enables_ship_building"));
		}
		if (preventsPlague) {
			sb.append("\n").append(_t("upgrade_prevents_plague"));
		}
		if (!removable && !special) {
			sb.append("\n").append(_t("upgrade_not_removable"));
		}
		String s = sb.toString();
		if (s.startsWith("\n")) { return s.substring(1); } else { return s; }
	}
	
	public boolean isAvailable(Empire e, City c, WorldMap m) {
		if (!canBuild) { return false; }
		if (requiresToggle != null && !m.toggles.contains(requiresToggle)) { return false; }
		if (requiredUpgradesName != null) {
			CityUpgradeType req = requiredUpgrades();
			int count = 0;
			for (int i = 0; i < e.cities.size(); i++) {
				if (e.cities.get(i).upgrades.contains(req)) {
					count++;
				}
			}
			if (count < numRequiredUpgrades) { return false; }
		}
		for (int i = 0; i < c.constructing.size(); i++) {
			if (c.constructing.get(i).upgrade == this) { return false; }
		}
		if (!allowBuildingDespiteGivenBonus && gives != null) {
			if (e.bonuses.contains[gives.ordinal()]) { return false; }
			for (int ci = 0; ci < e.cities.size(); ci++) {
				City c2 = e.cities.get(ci);
				for (int ui = 0; ui < c2.constructing.size(); ui++) {
					if (c2.constructing.get(ui).upgrade != null && c2.constructing.get(ui).upgrade.gives == gives) {
						return false;
					}
				}
			}
		}
		return c.isTown == forTown && !c.upgrades.contains(this) && (!requiresCoast || c.coastal) && (!requiresNoCoast || !c.coastal) && (requires == null || e.bonuses.containsAll(requires));
	}
	
	public static ArrayList<CityUpgradeType> available(Empire e, City c, WorldMap m) {
		ArrayList<CityUpgradeType> l = new ArrayList<CityUpgradeType>();
		for (CityUpgradeType cut : all(CityUpgradeType.class)) {
			if (cut.isAvailable(e, c, m)) {
				l.add(cut);
			}
		}
		return l;
	}
	
	public static CityUpgradeType ofName(String name) {
		return ofName(CityUpgradeType.class, name);
	}

	public int getCost(Empire player, City c, WorldMap map) {
		int upgradeNumber = 1;
		for (int i = 0; i < c.upgrades.size(); i++) {
			if (c.upgrades.get(i).canBuild) {
				upgradeNumber++;
			}
		}
		for (int i = 0; i < c.constructing.size(); i++) {
			if (c.constructing.get(i).upgrade != null) { upgradeNumber++; }
		}
		int cost = (int) (StrictMath.pow(upgradeNumber, EmpireStat.UPGRADE_COST_EXPONENT.get(player.bonuses)) * (forTown ? EmpireStat.TOWN_UPGRADE_COST.get(player.bonuses) : EmpireStat.CITY_UPGRADE_COST.get(player.bonuses)) + extraCost.get(player.bonuses));
		Hero guvnor = Hero.get(c, map);
		if (guvnor != null) {
			cost = (cost * (100 + guvnor.type.upgradeCostPercent)) / 100;
		}
		return cost;
	}
	
	public int getInstabuildCost() {
		return 2 * ((forTown ? EmpireStat.TOWN_UPGRADE_COST.get(BonusSet.empty()) : EmpireStat.CITY_UPGRADE_COST.get(BonusSet.empty())) + extraCost.get(BonusSet.empty()));
	}
	
	public int getDestroyRefund() {
		return ((forTown ? EmpireStat.TOWN_UPGRADE_COST.get(BonusSet.empty()) : EmpireStat.CITY_UPGRADE_COST.get(BonusSet.empty())) + extraCost.get(BonusSet.empty())) / 2;
	}
}
