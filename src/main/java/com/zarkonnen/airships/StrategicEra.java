package com.zarkonnen.airships;

import org.json.JSONObject;

public class StrategicEra extends Loadable {
	public final int startTime;
	public final int startTimePerEmpire;
	public final Bonus bonus;
	public final double modifierChance;
	
	public StrategicEra(JSONObject o) {
		super(o.getString("name"), o.getInt("startTime"));
		startTime = o.getInt("startTime");
		startTimePerEmpire = o.optInt("startTimePerEmpire", 0);
		bonus = Bonus.ofName(o.getString("bonus"));
		modifierChance = o.getDouble("modifierChance");
	}
	
	public boolean isFinalEra() {
		for (StrategicEra se : all(StrategicEra.class)) {
			if (se != this && se.startTime > startTime) {
				return false;
			}
		}
		return true;
	}
		
	private int startTime(WorldMap wm) {
		return startTime + wm.size.empires * startTimePerEmpire;
	}
		
	public static StrategicEra get(WorldMap wm) {
		StrategicEra era = null;
		for (StrategicEra se : all(StrategicEra.class)) {
			if (wm.era != null && se.startTime < wm.era.startTime) { continue; } // No backsliding to earlier eras.
			if (se.startTime(wm) <= wm.age && (era == null || se.startTime(wm) > era.startTime(wm))) {
				era = se;
			}
		}
		return era == null ? wm.era : era;
	}
	
	public static int msUntilNextEra(WorldMap wm) {
		if (wm.era == null) { return 0; }
		StrategicEra nextEra = null;
		for (StrategicEra se : all(StrategicEra.class)) {
			if (se == wm.era) { continue; }
			if (se.startTime > wm.era.startTime && (nextEra == null || se.startTime < nextEra.startTime)) {
				nextEra = se;
			}
		}
		return nextEra == null ? 0 : nextEra.startTime(wm) - wm.age;
	}
	
	public int timeLength(WorldMap wm) {
		StrategicEra nextEra = null;
		for (StrategicEra se : all(StrategicEra.class)) {
			if (se == wm.era) { continue; }
			if (se.startTime > wm.era.startTime && (nextEra == null || se.startTime < nextEra.startTime)) {
				nextEra = se;
			}
		}
		return nextEra == null ? 0 : nextEra.startTime(wm) - startTime(wm);
	}
	
	public static StrategicEra ofName(String name) {
		return ofName(StrategicEra.class, name);
	}
}
