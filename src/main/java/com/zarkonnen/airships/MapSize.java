package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import org.json.JSONObject;

public strictfp class MapSize extends Loadable {
	public final int gridSize;
	public final int empires;
	public final int townsPerEmpire;
	public final int nests;
	
	public MapSize(JSONObject o) {
		super(o.getString("name"), o.getInt("gridSize"));
		gridSize = 256 * o.getInt("gridSize") / WorldMap.SCALE_FACTOR;
		empires = o.getInt("empires");
		townsPerEmpire = o.optInt("townsPerEmpire", 2);
		nests = o.getInt("nests");
	}
	
	public static MapSize ofName(String name) {
		return ofName(MapSize.class, name);
	}
	
	public String getName() {
		return _t("mapsize_" + name);
	}
}
