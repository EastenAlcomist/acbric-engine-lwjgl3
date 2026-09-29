package com.zarkonnen.airships;

import org.json.JSONObject;

public class BesiegeRequest {
	public final Empire from;
	public final City city;
	public transient boolean announced;

	public BesiegeRequest(Empire from, City city) {
		this.from = from;
		this.city = city;
	}
	
	public BesiegeRequest(JSONObject o, WorldMap m) {
		from = m.getEmpire(o.getInt("from"));
		city = m.getCity(o.getInt("city"));
	}
	
	public JSONObject toJSON() {
		return new JSONObject().put("from", from.id).put("city", city.id);
	}
}
