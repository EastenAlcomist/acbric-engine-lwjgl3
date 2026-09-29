package com.zarkonnen.airships;

import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;

public strictfp class MonsterSetting extends Loadable {
	public final boolean useDifficultyLevelSetting;
	public final BonusableValue<Double> monsterNestDensity;
	public final BonusableValue<Double> monsterNestIncomeMult;
	
	public MonsterSetting(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		useDifficultyLevelSetting = o.getBoolean("useDifficultyLevelSetting");
		monsterNestDensity = BonusableValue.doubleFromJSON(o, "monsterNestDensity", 1);
		monsterNestIncomeMult = BonusableValue.doubleFromJSON(o, "monsterNestIncomeMult", 1);
	}
	
	public static MonsterSetting ofName(String name) {
		return ofName(MonsterSetting.class, name);
	}
	
	public String getName() {
		return _t("monstersetting_" + name);
	}
}
