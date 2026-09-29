package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import org.json.JSONObject;

public class MedalMetal extends Loadable {
	public final Clr solid, outline, charge;
	public final int minLevel;
	public final int maxRandomLevel;
	
	public MedalMetal(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		solid = new Clr(o.getJSONObject("solid").getInt("r"), o.getJSONObject("solid").getInt("g"), o.getJSONObject("solid").getInt("b"));
		outline = new Clr(o.getJSONObject("outline").getInt("r"), o.getJSONObject("outline").getInt("g"), o.getJSONObject("outline").getInt("b"));
		charge = new Clr(o.getJSONObject("charge").getInt("r"), o.getJSONObject("charge").getInt("g"), o.getJSONObject("charge").getInt("b"));
		minLevel = o.optInt("minLevel", 0);
		maxRandomLevel = o.optInt("maxRandomLevel", 10000);
	}
	
	public static ArrayList<MedalMetal> forLevel(int level) {
		ArrayList<MedalMetal> l = new ArrayList<MedalMetal>();
		for (MedalMetal e : all(MedalMetal.class)) {
			if (level >= e.minLevel) {
				l.add(e);
			}
		}
		return l;
	}
	
	public static ArrayList<MedalMetal> forLevelRandom(int level) {
		ArrayList<MedalMetal> l = new ArrayList<MedalMetal>();
		for (MedalMetal e : all(MedalMetal.class)) {
			if (level >= e.minLevel && level <= e.maxRandomLevel) {
				l.add(e);
			}
		}
		return l;
	}
	
	public static MedalMetal ofName(String name) {
		return ofName(MedalMetal.class, name);
	}
}
