package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import org.json.JSONObject;

public class FrequencySetting extends Loadable {
	public final String label;
	public final double frequencyMultiplier;
	
	public FrequencySetting(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		label = o.getString("label");
		frequencyMultiplier = o.getDouble("frequencyMultiplier");
	}
	
	public static FrequencySetting ofName(String name) {
		return ofName(FrequencySetting.class, name);
	}
	
	public String getName() {
		return _t(label);
	}
}
