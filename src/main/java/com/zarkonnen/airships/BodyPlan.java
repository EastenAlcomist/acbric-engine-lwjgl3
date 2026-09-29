package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class BodyPlan extends Loadable {
	public final EnumMap<Side, List<String>> partNames = new EnumMap<Side, List<String>>(Side.class);
	public final boolean isSpider;
	
	public BodyPlan(JSONObject o) {
		super(o.getString("name"));
		isSpider = o.optBoolean("isSpider", false);
		for (Side side : Side.values()) {
			JSONArray pn = o.getJSONArray(side.name());
			ArrayList<String> pns = new ArrayList<String>();
			for (int i = 0; i < pn.length(); i++) {
				pns.add(pn.getString(i));
			}
			partNames.put(side, Collections.unmodifiableList(pns));
		}
	}
	
	public int getIndex(Side side, String partName) {
		return partNames.get(side).indexOf(partName);
	}
		
	public static BodyPlan ofName(String name) {
		return ofName(BodyPlan.class, name);
	}
}
