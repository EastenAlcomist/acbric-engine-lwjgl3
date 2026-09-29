package com.zarkonnen.airships;

import org.json.JSONObject;

public strictfp class ResearchSpeed extends Loadable {
	public final int baseCost, cityCost, baseResearch, cityResearch;
	public final boolean isDefault;
	
	public ResearchSpeed(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		baseCost = o.getInt("baseCost");
		cityCost = o.getInt("cityCost");
		baseResearch = o.getInt("baseResearch");
		cityResearch = o.getInt("cityResearch");
		isDefault = o.optBoolean("isDefault", false);
	}
	
	public static ResearchSpeed ofName(String name) {
		return ofName(ResearchSpeed.class, name);
	}
	
	public static ResearchSpeed getDefault() {
		for (ResearchSpeed rs : all(ResearchSpeed.class)) {
			if (rs.isDefault) {
				return rs;
			}
		}
		return all(ResearchSpeed.class).isEmpty() ? null : all(ResearchSpeed.class).get(0);
	}
}
