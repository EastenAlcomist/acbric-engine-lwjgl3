package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

import static com.zarkonnen.airships.Lang._t;
import java.util.HashMap;
import org.json.JSONArray;

public strictfp class Charge extends Loadable {
	public final Appearance app;
	public final Appearance leftApp, rightApp, topApp, bottomApp, topLeftApp, topRightApp, bottomLeftApp, bottomRightApp;
	public final Appearance unlitApp;
	public final Appearance unlitLeftApp, unlitRightApp, unlitTopApp, unlitBottomApp, unlitTopLeftApp, unlitTopRightApp, unlitBottomLeftApp, unlitBottomRightApp;
	public final Bonus bonus;
	public final Tech.Choice tech;
	public final ArrayList<String> addToHeraldicStyles = new ArrayList<String>();
	public HashMap<ArmsLayout, HashMap<Integer, ArmsLayout.TincturedApp>> overrides = new HashMap<ArmsLayout, HashMap<Integer, ArmsLayout.TincturedApp>>();
	public HashMap<ArmsLayout, HashMap<Integer, ArmsLayout.TincturedApp>> unlitOverrides = new HashMap<ArmsLayout, HashMap<Integer, ArmsLayout.TincturedApp>>();
	public HashMap<ArmsLayout, HashMap<Integer, ArmsLayout.TincturedImg>> shieldOverrides = new HashMap<ArmsLayout, HashMap<Integer, ArmsLayout.TincturedImg>>();
	
	public ArmsLayout.TincturedImg getShieldOverride(ArmsLayout layout, int chargeImgIndex) {
		return shieldOverrides.containsKey(layout) && shieldOverrides.get(layout).containsKey(chargeImgIndex) ? shieldOverrides.get(layout).get(chargeImgIndex) : null;
	}
	
	public ArmsLayout.TincturedApp getOverride(ArmsLayout layout, int chargeImgIndex) {
		return overrides.containsKey(layout) && overrides.get(layout).containsKey(chargeImgIndex) ? overrides.get(layout).get(chargeImgIndex) : null;
	}
	
	public ArmsLayout.TincturedApp getUnlitOverride(ArmsLayout layout, int chargeImgIndex) {
		return unlitOverrides.containsKey(layout) && unlitOverrides.get(layout).containsKey(chargeImgIndex) ? unlitOverrides.get(layout).get(chargeImgIndex) : null;
	}
	
	public Charge(JSONObject o) {
		super(o.getString("name"));
		app = new Appearance(o.getJSONObject("appearance"));
		unlitApp = new Appearance(o.getJSONObject("unlitAppearance"));
		bonus = Bonus.ofName(o.getString("bonus"));
		leftApp = app.leftSide();
		rightApp = app.rightSide();
		topApp = app.topSide();
		bottomApp = app.bottomSide();
		topLeftApp = app.topLeftSide();
		topRightApp = app.topRightSide();
		bottomLeftApp = app.bottomLeftSide();
		bottomRightApp = app.bottomRightSide();
		unlitLeftApp = unlitApp.leftSide();
		unlitRightApp = unlitApp.rightSide();
		unlitTopApp = unlitApp.topSide();
		unlitBottomApp = unlitApp.bottomSide();
		unlitTopLeftApp = unlitApp.topLeftSide();
		unlitTopRightApp = unlitApp.topRightSide();
		unlitBottomLeftApp = unlitApp.bottomLeftSide();
		unlitBottomRightApp = unlitApp.bottomRightSide();
		if (o.has("tech")) {
			Tech t = Tech.ofName(o.getString("tech"));
			if (t.choices.size() > 1) {
				tech = t.getChoice(o.getString("techChoice"));
			} else {
				tech = t.choices.get(0);
			}
		} else {
			tech = null;
		}
		if (o.has("overrides")) {
			JSONObject o2 = o.getJSONObject("overrides");
			for (Object o2k : o2.keySet()) {
				String k2 = (String) o2k;
				ArmsLayout al = ArmsLayout.valueOf(k2);
				HashMap<Integer, ArmsLayout.TincturedApp> ovs = new HashMap<Integer, ArmsLayout.TincturedApp>();
				overrides.put(al, ovs);
				JSONObject o3 = o2.getJSONObject(k2);
				for (Object o3k : o3.keySet()) {
					ovs.put(Integer.parseInt((String) o3k), new ArmsLayout.TincturedApp(o3.getJSONObject((String) o3k)));
				}
			}
		}
		if (o.has("unlitOverrides")) {
			JSONObject o2 = o.getJSONObject("unlitOverrides");
			for (Object o2k : o2.keySet()) {
				String k2 = (String) o2k;
				ArmsLayout al = ArmsLayout.valueOf(k2);
				HashMap<Integer, ArmsLayout.TincturedApp> ovs = new HashMap<Integer, ArmsLayout.TincturedApp>();
				unlitOverrides.put(al, ovs);
				JSONObject o3 = o2.getJSONObject(k2);
				for (Object o3k : o3.keySet()) {
					ovs.put(Integer.parseInt((String) o3k), new ArmsLayout.TincturedApp(o3.getJSONObject((String) o3k)));
				}
			}
		}
		if (o.has("shieldOverrides")) {
			JSONObject o2 = o.getJSONObject("shieldOverrides");
			for (Object o2k : o2.keySet()) {
				String k2 = (String) o2k;
				ArmsLayout al = ArmsLayout.valueOf(k2);
				HashMap<Integer, ArmsLayout.TincturedImg> ovs = new HashMap<Integer, ArmsLayout.TincturedImg>();
				shieldOverrides.put(al, ovs);
				JSONObject o3 = o2.getJSONObject(k2);
				for (Object o3k : o3.keySet()) {
					ovs.put(Integer.parseInt((String) o3k), new ArmsLayout.TincturedImg(o3.getJSONObject((String) o3k)));
				}
			}
		}
		if (o.has("addToHeraldicStyles")) {
			JSONArray a = o.getJSONArray("addToHeraldicStyles");
			for (int i = 0; i < a.length(); i++) {
				addToHeraldicStyles.add(a.getString(i));
			}
		}
	}

	public String getName() {
		return Lang._t(name + "_singular");
	}

	public String getPlural() {
		return Lang._t(name + "_plural");
	}
	
	public String getBonusOrTechDesc() {
		StringBuilder sb = new StringBuilder();
		if (tech == null) {
			return bonus.getDesc();
		}
		if (bonus != Bonus.ofName("NO_BONUS")) {
			sb.append(bonus.getDesc()).append(", ");
		}
		ArrayList<Tech.Choice> prereqs = tech.getAllPrerequisitesIncludingThis();
		if (!prereqs.isEmpty()) {
			sb.append(_t("Technology_one_of_"));
		}
		for (Tech.Choice c : prereqs) {
			sb.append(_t("tech_" + c.name));
			if (c != prereqs.get(prereqs.size() - 1)) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	
	public static Charge ofName(String name) {
		return ofName(Charge.class, name);
	}
		
	public static List<Charge> withBonus = new ArrayList<Charge>();
	
	public static void postLoad() {
		withBonus.clear();
		for (Charge c : all(Charge.class)) {
			if (c.bonus != Bonus.ofName("NO_BONUS")) {
				withBonus.add(c);
			}
		}
	}
	
	public static Charge bonusGiver(Bonus b) {
		for (Charge c : all(Charge.class)) {
			if (c.bonus == b) { return c; }
		}
		return null;
	}
}
