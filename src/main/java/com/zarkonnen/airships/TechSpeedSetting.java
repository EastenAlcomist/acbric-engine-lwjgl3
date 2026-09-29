package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import org.json.JSONObject;

public strictfp class TechSpeedSetting extends Loadable {
	public final double speedMultiplier;
	
	public TechSpeedSetting(JSONObject o) {
		super(o.getString("name"), o.optInt("sort"));
		speedMultiplier = o.getDouble("speedMultiplier");
	}
	
	public static TechSpeedSetting ofName(String name) {
		return ofName(TechSpeedSetting.class, name);
	}
	
	public String getName() {
		return _t("techspeedsetting_" + name);
	}
}
