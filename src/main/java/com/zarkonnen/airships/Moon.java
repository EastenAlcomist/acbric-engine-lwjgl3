package com.zarkonnen.airships;

import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class Moon extends Loadable {
	public final int offsetInDays;
	public final int periodInDays;
	public BonusableValue<Boolean> show;

	public Moon(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		offsetInDays = o.getInt("offsetInDays");
		periodInDays = o.getInt("periodInDays");
		show = BonusableValue.booleanFromJSON(o, "show", true);
	}
	
	public static ArrayList<Moon> visibleMoons(BonusSet bonuses) {
		ArrayList<Moon> ms = new ArrayList<Moon>();
		for (Moon m : all(Moon.class)) {
			if (m.show.get(bonuses)) {
				ms.add(m);
			}
		}
		return ms;
	}
}
