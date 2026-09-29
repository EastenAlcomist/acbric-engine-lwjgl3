package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class CombatBackgroundFlavor extends Loadable {
	public final BirdType birds;
	public final BirdType fish;
	public final ArrayList<BackdropType> backdrops = new ArrayList<BackdropType>();

	public CombatBackgroundFlavor(JSONObject o) {
		super(o.getString("name"));
		if (o.has("birds")) {
			birds = BirdType.ofName(o.getString("birds"));
		} else {
			birds = null;
		}
		if (o.has("fish")) {
			fish = BirdType.ofName(o.getString("fish"));
		} else if (hasOfName(BirdType.class, "fish")) {
			fish = BirdType.ofName("fish");
		} else {
			fish = null;
		}
		JSONArray bs = o.getJSONArray("backdrops");
		for (int i = 0; i < bs.length(); i++) {
			backdrops.add(BackdropType.ofName(bs.getString(i)));
		}
	}
	
	public static CombatBackgroundFlavor ofName(String name) {
		return ofName(CombatBackgroundFlavor.class, name);
	}
	
	public LandscapeType getLandscapeType(GuardedRandom r) {
		ArrayList<LandscapeType> types = new ArrayList<LandscapeType>();
		for (LandscapeType t : all(LandscapeType.class)) {
			if (t.backgroundFlavors.contains(this)) {
				types.add(t);
			}
		}
		return LandscapeType.getRandom(types, r);
	}
}
