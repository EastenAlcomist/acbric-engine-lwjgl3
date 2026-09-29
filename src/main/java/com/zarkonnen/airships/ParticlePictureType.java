package com.zarkonnen.airships;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public /* nostrictfp */ class ParticlePictureType extends Loadable {
	public ArrayList<Pic> pictures = new ArrayList<Pic>();
	
	public static class Pic {
		public final SpritesheetBundle ssb;
		public final int x, y, w, h;

		public Pic(SpritesheetBundle ssb, int x, int y, int w, int h) {
			this.ssb = ssb;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}
	
	public ParticlePictureType(JSONObject o) {
		super(o.getString("name"));
		JSONArray a = o.getJSONArray("pictures");
		for (int i = 0; i < a.length(); i++) {
			JSONObject io = a.getJSONObject(i);
			pictures.add(new Pic(SpritesheetBundle.ofName(io.getString("src")), io.getInt("x"), io.getInt("y"), io.getInt("w"), io.getInt("h")));
		}
	}
	
	public static ParticlePictureType ofName(String name) {
		return ofName(ParticlePictureType.class, name);
	}
}
