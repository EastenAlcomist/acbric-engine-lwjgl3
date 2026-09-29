package com.zarkonnen.airships;

import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.ArrayList;

public class CrewExperienceLevel extends Loadable {
	public final double minExperience;
	public final Img icon;
	
	public final int medalLevel;
	public final int numMedals;
	public final Img medalPlaceholder;
	public final String medalSound;
	
	public CrewExperienceLevel(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		minExperience = o.getDouble("minExperience");
		icon = new Img(o.getJSONObject("icon").getString("src"), o.getJSONObject("icon").getInt("x"), o.getJSONObject("icon").getInt("y"), 16, 16, false);
		if (o.has("medalLevel")) {
			medalLevel = o.getInt("medalLevel");
			numMedals = o.getInt("numMedals");
			JSONObject po = o.getJSONObject("medalPlaceholder");
			medalPlaceholder = new Img(po.getString("src"), po.getInt("x"), po.getInt("y"), po.getInt("w"), po.getInt("h"), po.optBoolean("flipped", false));
			medalSound = o.optString("medalSound", null);
		} else {
			medalLevel = 0;
			numMedals = 0;
			medalPlaceholder = null;
			medalSound= null;
		}
	}
	
	public static int getMaxAvailableMedalLevel(double xp) {
		int lvl = 0;
		for (CrewExperienceLevel cel : all(CrewExperienceLevel.class)) {
			if (xp >= cel.minExperience) {
				lvl = Math.max(lvl, cel.medalLevel);
			}
		}
		return lvl;
	}
	
	public static String getMedalSound(int level) {
		for (CrewExperienceLevel cel : all(CrewExperienceLevel.class)) {
			if (cel.medalLevel == level) {
				return cel.medalSound;
			}
		}
		return null;
	}
	
	public static Img getMedalPlaceholder(int level) {
		for (CrewExperienceLevel cel : all(CrewExperienceLevel.class)) {
			if (cel.medalLevel == level) {
				return cel.medalPlaceholder;
			}
		}
		return null;
	}
	
	public static int getNumMedals(int level) {
		for (CrewExperienceLevel cel : all(CrewExperienceLevel.class)) {
			if (cel.medalLevel == level) {
				return cel.numMedals;
			}
		}
		return 0;
	}
	
	public static int getMaxMedalLevel(BonusSet bs) {
		int maxLevel = 0;
		double maxXP = EmpireStat.CREW_MAX_XP.get(bs);
		for (CrewExperienceLevel cel : all(CrewExperienceLevel.class)) {
			if (cel.minExperience <= maxXP) {
				maxLevel = StrictMath.max(maxLevel, cel.medalLevel);
			}
		}
		return maxLevel;
	}
	
	public String getName() {
		return _t("crew_XP_" + name);
	}
	
	public static CrewExperienceLevel getLevel(double xp) {
		ArrayList<CrewExperienceLevel> l = all(CrewExperienceLevel.class);
		CrewExperienceLevel lvl = l.get(0);
		for (CrewExperienceLevel cel : l) {
			if (xp >= cel.minExperience) {
				lvl = cel;
			}
		}
		return lvl;
	}
}
