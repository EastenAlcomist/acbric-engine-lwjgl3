package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class Shouts extends Loadable {
	public final HashMap<String, ArrayList<String>> shouts = new HashMap<String, ArrayList<String>>();
	
	public Shouts(JSONObject o) {
		super(o.getString("name"));
		for (Object key : o.keySet()) {
			if (key instanceof String) {
				String k = (String) key;
				if (k.equals("name")) { continue; }
				JSONArray a = o.getJSONArray(k);
				ArrayList<String> l = new ArrayList<String>();
				for (int i = 0; i < a.length(); i++) {
					l.add(a.getString(i));
				}
				shouts.put(k, l);
			}
		}
	}
	
	public static Shouts ofName(String name) {
		return ofName(Shouts.class, name);
	}
}
