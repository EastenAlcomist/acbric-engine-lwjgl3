package com.zarkonnen.airships;

import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class MusicAffinity extends Loadable {
	public final ModuleType module;
	public final String music;
	public final ArrayList<String> musicL = new ArrayList<String>();
	
	public MusicAffinity(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		module = ModuleType.ofName(o.getString("module"));
		music = o.getString("music");
		musicL.add(music);
	}
	
	public static ArrayList<String> pickMusic(Combat c) {
		if (c.musicChoice != null) { return c.musicChoice; }
		ArrayList<MusicAffinity> as = all(MusicAffinity.class);
		for (int i = 0; i < as.size(); i++) {
			MusicAffinity a = as.get(i);
			for (int j = 0; j < c.sides.size(); j++) {
				Combat.Side s = c.sides.get(j);
				for (int k = 0; k < s.ships.size(); k++) {
					Airship ship = s.ships.get(k);
					for (int l = 0; l < ship.modules.size(); l++) {
						Module m = ship.modules.get(l);
						if (m.type == a.module) {
							c.musicChoice = a.musicL;
							return a.musicL;
						}
					}
				}
			}
		}
		c.musicChoice = AGame.COMBAT_MUSIC;
		return AGame.COMBAT_MUSIC;
	}
}
