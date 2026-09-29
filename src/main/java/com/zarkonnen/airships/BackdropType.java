package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;

public strictfp class BackdropType extends Loadable {
	public HashMap<String, ArrayList<Backdrop>> backdropsByVariant = new HashMap<String, ArrayList<Backdrop>>();
	
	public BackdropType(JSONObject o) {
		super(o.getString("name"));
		for (Backdrop bd : all(Backdrop.class)) {
			if (bd.typeName.equals(name)) {
				if (!backdropsByVariant.containsKey(bd.variant)) {
					backdropsByVariant.put(bd.variant, new ArrayList<Backdrop>());
				}
				backdropsByVariant.get(bd.variant).add(bd);
			}
		}
	}
	
	public static BackdropType ofName(String name) {
		return ofName(BackdropType.class, name);
	}
}
