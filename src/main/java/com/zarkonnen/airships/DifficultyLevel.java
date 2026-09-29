package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import org.json.JSONObject;

public class DifficultyLevel extends Loadable {
	public final AIQuality enemyAI;
	public final int playerIncome;
	public final int playerExtraMoney;
	public final int playerFinalCash;
	public final int aiFinalCash;
	public final int extraAIStartingTechs;
	public final double aiIncomeMultiplier;
	public final double aiProductionMultiplier;
	public final double aiResupplyMultiplier;
	public final double aiResearchMultiplier;
	public final int aiExtraMoney;
	public final int aiAvoidWarWithHumanPlayersInOtherWars;
	public final int aiAvoidPactPerOtherAIPact;
	public final int aiTreatyBonus;
	public final int aiCooperationBonus;
	private final BonusableValue<Double> monsterNestDensity;
	private final BonusableValue<Double> monsterNestIncomeMult;
	public final double easyStartingNestDensity;
	public final int attackInterval;
	public final int playerAttackInterval;
	public final boolean startingShip;
	public final int maxAIBonusConstructions;
	public final int minimalNestUpgradeTimeDivider;
	public final int playerUnrestModifier;
	public final int minSpyInterval;
	public final int rampUpTime;
	
	public double aiResearchMultiplier(int age) {
		if (aiResearchMultiplier <= 1) {
			return aiResearchMultiplier;
		}
		return 1 + (aiResearchMultiplier - 1) * StrictMath.min(1, 0.3 + 0.7 * age / rampUpTime);
	}
	
	public double aiResupplyMultiplier(int age) {
		if (aiResupplyMultiplier <= 1) {
			return aiResupplyMultiplier;
		}
		return 1 + (aiResupplyMultiplier - 1) * StrictMath.min(1, 0.3 + 0.7 * age / rampUpTime);
	}
	
	public double aiIncomeMultiplier(int age) {
		if (aiIncomeMultiplier <= 1) {
			return aiIncomeMultiplier;
		}
		return 1 + (aiIncomeMultiplier - 1) * StrictMath.min(1, 0.3 + 0.7 * age / rampUpTime);
	}
	
	public double aiProductionMultiplier(int age) {
		if (aiProductionMultiplier <= 1) {
			return aiProductionMultiplier;
		}
		return 1 + (aiProductionMultiplier - 1) * StrictMath.min(1, 0.3 + 0.7 * age / rampUpTime);
	}
	
	public double monsterNestDensity(MonsterSetting ms, BonusSet bs) {
		if (ms.useDifficultyLevelSetting) {
			return monsterNestDensity.get(bs);
		} else {
			return ms.monsterNestDensity.get(bs);
		}
	}
	
	public double easyStartingNestDensity(MonsterSetting ms, BonusSet bs) {
		if (ms.monsterNestDensity.get(bs) == 0) {
			return 0;
		} else {
			return easyStartingNestDensity;
		}
	}
	
	public double monsterNestIncomeMult(MonsterSetting ms, BonusSet bs) {
		if (ms.useDifficultyLevelSetting) {
			return monsterNestIncomeMult.get(bs);
		} else {
			return ms.monsterNestIncomeMult.get(bs);
		}
	}
	
	private String enemyAggressiveness() {
		if (playerAttackInterval >= 80000) {
			return _t("very_low");
		} else if (playerAttackInterval >= 40000) {
			return _t("low");
		} else if (playerAttackInterval >= 20000) {
			return _t("medium");
		} else {
			return _t("high");
		}
	}
	
	private String nestUpgradeSpeed() {
		if (minimalNestUpgradeTimeDivider >= 8) {
			return _t("very_high");
		} else if (minimalNestUpgradeTimeDivider >= 4) {
			return _t("high");
		} else if (minimalNestUpgradeTimeDivider >= 2) {
			return _t("medium");
		} else {
			return _t("low");
		}
	}
	
	public String getDesc() {
		String d = _t("aiDesc",
				enemyAI.getName(),
				playerIncome * 100 / 80,
				(int) (monsterNestDensity.get(BonusSet.empty()) * 100),
				(int) (monsterNestIncomeMult.get(BonusSet.empty()) / 0.9 * 100),
				extraAIStartingTechs,
				(int) (aiIncomeMultiplier * 100),
				(int) (aiProductionMultiplier * 100),
				enemyAggressiveness(),
				nestUpgradeSpeed(),
				(int) (aiResearchMultiplier),
				(int) (aiResupplyMultiplier));
		if (aiIncomeMultiplier > 1 || aiProductionMultiplier > 1 || aiResearchMultiplier > 1 || aiResupplyMultiplier > 1) {
			d += "\n" + _t("aiDescRampup", WorldMap.describeTime(rampUpTime, BonusSet.empty()));
		}
		return d;
	}

	public DifficultyLevel(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		enemyAI = AIQuality.valueOf(o.getString("enemyAI"));
		playerIncome = o.getInt("playerIncome");
		playerExtraMoney = o.getInt("playerExtraMoney");
		playerFinalCash = o.getInt("playerFinalCash");
		aiFinalCash = o.optInt("aiFinalCash", 0);
		monsterNestDensity = BonusableValue.doubleFromJSON(o, "monsterNestDensity", 1);
		monsterNestIncomeMult = BonusableValue.doubleFromJSON(o, "monsterNestIncomeMult", 1);
		extraAIStartingTechs = o.optInt("extraAIStartingTechs", 2);
		aiIncomeMultiplier = o.optDouble("aiIncomeMultiplier", 1);
		aiProductionMultiplier = o.optDouble("aiProductionMultiplier", 1);
		attackInterval = o.optInt("attackInterval", 40000);
		playerAttackInterval = o.optInt("playerAttackInterval", 30000);
		aiExtraMoney = o.optInt("aiExtraMoney", 0);
		startingShip = o.optBoolean("startingShip", true);
		maxAIBonusConstructions = o.optInt("maxAIBonusConstructions", 9);
		minimalNestUpgradeTimeDivider = StrictMath.max(o.optInt("minimalNestUpgradeTimeDivider", 4), 1);
		playerUnrestModifier = o.optInt("playerUnrestModifier", 0);
		easyStartingNestDensity = o.optDouble("easyStartingNestDensity", 0.3);
		aiAvoidWarWithHumanPlayersInOtherWars = o.optInt("aiAvoidWarWithHumanPlayersInOtherWars", 0);
		aiAvoidPactPerOtherAIPact = o.optInt("aiAvoidPactPerOtherAIPact", 0);
		aiTreatyBonus = o.optInt("aiTreatyBonus", 0);
		aiCooperationBonus = o.optInt("aiCooperationBonus", 0);
		minSpyInterval = o.optInt("minSpyInterval", 120000);
		rampUpTime = o.optInt("rampUpTime", 3 * 400 * 28 * 13);
		aiResupplyMultiplier = o.optDouble("aiResupplyMultiplier", 1);
		aiResearchMultiplier = o.optDouble("aiResearchMultiplier", 1);
	}
	
	public String getName() {
		return _t("difficulty_" + name);
	}
	
	public static DifficultyLevel ofName(String name) {
		return ofName(DifficultyLevel.class, name);
	}
}
