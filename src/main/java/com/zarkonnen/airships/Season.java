package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class Season extends Loadable {
	public final BonusableValue<ArrayList<TimeOfDay>> timesOfDay;
	public final BonusableValue<ArrayList<TimeOfDay>> defaultTimeOfDay;
	public final Img icon;
	public final int lengthInDays;
	
	public Season(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		lengthInDays = o.getInt("lengthInDays");
		JSONObject icn = o.getJSONObject("icon");
		icon = new Img(icn.getString("src"), icn.getInt("x"), icn.getInt("y"), icn.optInt("w", 16), icn.optInt("h", 16), icn.optBoolean("flipped", false));
		timesOfDay = BonusableValue.loadableListFromJSON(o, "timesOfDay", new ArrayList<TimeOfDay>(), TimeOfDay.class, false);
		defaultTimeOfDay = BonusableValue.loadableListFromJSON(o, "defaultTimeOfDay", new ArrayList<TimeOfDay>(), TimeOfDay.class, false);
	}
	
	public static Season getSeason(int mapAge) {
		int totalSeasonLength = 0;
		ArrayList<Season> seasons = all(Season.class);
		for (Season s : seasons) {
			totalSeasonLength += s.lengthInDays;
		}
		int day = (mapAge / WorldMap.MS_PER_DAY) % totalSeasonLength;
		for (Season s : seasons) {
			if (day < s.lengthInDays) {
				return s;
			}
			day -= s.lengthInDays;
		}
		return seasons.get(0);
	}
}
