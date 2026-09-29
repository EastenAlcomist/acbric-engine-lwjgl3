package com.zarkonnen.airships;

import org.json.JSONArray;
import org.json.JSONObject;

public class MedalRibbonLayout extends Loadable {
	public final int numTinctures;
	public final int[] stripes;
	
	public MedalRibbonLayout(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		JSONArray a = o.getJSONArray("stripes");
		int maxTinctureID = 0;
		stripes = new int[a.length()];
		for (int i = 0; i < stripes.length; i++) {
			stripes[i] = a.getInt(i);
			if (i % 2 == 0) {
				maxTinctureID = Math.max(stripes[i], maxTinctureID);
			}
		}
		numTinctures = maxTinctureID + 1;
	}
	
	public static MedalRibbonLayout ofName(String name) {
		return ofName(MedalRibbonLayout.class, name);
	}
}
