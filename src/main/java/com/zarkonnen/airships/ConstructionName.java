package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConstructionName extends Loadable {
	public ArrayList<Charge> charges = new ArrayList<Charge>();
	public ArrayList<DiplomacyPersonality> personalities = new ArrayList<DiplomacyPersonality>();
	public ArrayList<String> languages = new ArrayList<String>();
	public boolean building, normal;
	
	public ConstructionName(JSONObject o) {
		super(o.getString("name"));
		if (o.has("charges")) {
			JSONArray a = o.getJSONArray("charges");
			for (int i = 0; i < a.length(); i++) {
				if (hasOfName(Charge.class, a.getString(i))) {
					charges.add(Charge.ofName(a.getString(i)));
				}
			}
		}
		if (o.has("personalities")) {
			JSONArray a = o.getJSONArray("personalities");
			for (int i = 0; i < a.length(); i++) {
				if (hasOfName(Charge.class, a.getString(i))) {
					personalities.add(DiplomacyPersonality.ofName(a.getString(i)));
				}
			}
		}
		JSONArray a = o.getJSONArray("languages");
		for (int i = 0; i < a.length(); i++) {
			languages.add(a.getString(i));
		}
		normal = o.optBoolean("normal", false);
		building = o.optBoolean("building", false);
	}
	
	public static String getName(Empire e, Locale loc, ShipType t, GuardedRandom r) {
		return getName(e.arms.charge[0], e.playerControlled ? null : e.diplomacyAI.personality, loc, t == ShipType.BUILDING, r);
	}
	
	public static String getName(Charge c, DiplomacyPersonality p, Locale loc, boolean building, GuardedRandom r) {
		ArrayList<String> ns = getNames(c, p, loc.toLanguageTag(), building);
		if (ns.isEmpty()) {
			return "Airship";
		}
		return ns.get(r.nextInt(ns.size()));
	}
	
	public static ArrayList<String> getNames(Charge c, DiplomacyPersonality p, String lang, boolean building) {
		ArrayList<String> ns = new ArrayList<String>();
		ArrayList<ConstructionName> cns = all(ConstructionName.class);
		getNames(ns, cns, c, p, lang, building);
		if (!ns.isEmpty()) { return ns; }
		getNames(ns, cns, null, null, lang, building);
		if (!ns.isEmpty()) { return ns; }
		getNames(ns, cns, c, p, "en", building);
		if (!ns.isEmpty()) { return ns; }
		getNames(ns, cns, null, null, "en", building);
		if (!ns.isEmpty()) { return ns; }
		return ns;
	}
	
	private static void getNames(ArrayList<String> ns, ArrayList<ConstructionName> cns, Charge c, DiplomacyPersonality p, String lang, boolean building) {
		for (int i = 0; i < cns.size(); i++) {
			ConstructionName cn = cns.get(i);
			if (
					cn.languages.contains(lang) &&
					building == cn.building &&
					(
						cn.charges.contains(c) ||
						cn.personalities.contains(p) ||
						(c == null && p == null && cn.normal)
					)
			) {
				ns.add(cn.name);
			}
		}
		/*if (!building) {
			System.out.println((c == null ? "null" : c.name) + ", " + (p == null ? "null" : p.name) + " => " + ns.size());
		}*/
	}
}
