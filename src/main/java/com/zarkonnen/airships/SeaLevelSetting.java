package com.zarkonnen.airships;

import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;

public strictfp class SeaLevelSetting extends Loadable {
	public final double boundary;
	public final double tilt;
	public final double empireLandshipFocusChance;
	
	public SeaLevelSetting(JSONObject o) {
		super(o.getString("name"), o.optInt("sort"));
		boundary = o.getDouble("boundary");
		tilt = o.getDouble("tilt");
		empireLandshipFocusChance = o.optDouble("empireLandshipFocusChance", 0.2);
	}
	
	public static SeaLevelSetting ofName(String name) {
		return ofName(SeaLevelSetting.class, name);
	}
	
	public String getName() {
		return _t("sealevelsetting_" + name);
	}
}
