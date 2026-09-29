package com.zarkonnen.airships;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class CloudType extends Loadable {
	public ArrayList<Appearance> clouds = new ArrayList<Appearance>();
	
	public CloudType(JSONObject o) {
		super(o.getString("name"));
		JSONArray a = o.getJSONArray("clouds");
		for (int i = 0; i < a.length(); i++) {
			clouds.add(new Appearance(a.getJSONObject(i)));
		}
	}
	
	public static CloudType ofName(String name) {
		return ofName(CloudType.class, name);
	}
}
