package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class HeraldicStyle extends Loadable {
	public final ArrayList<ArmsLayout> layouts = new ArrayList<ArmsLayout>();
	public final ArrayList<Charge> charges = new ArrayList<Charge>();
	public final ArrayList<Charge> weightedCharges = new ArrayList<Charge>();
	public final ArrayList<Charge> weightedChargesWithBonusOrTech = new ArrayList<Charge>();
	public final ArrayList<Tincture> layoutTinctures = new ArrayList<Tincture>();
	public final ArrayList<Tincture> chargeTinctures = new ArrayList<Tincture>();
	public final boolean ruleOfTincture, firstChargeShouldHaveBonus;
	
	public HeraldicStyle(JSONObject o) {
		super(o.getString("name"));
		ruleOfTincture = o.getBoolean("ruleOfTincture");
		firstChargeShouldHaveBonus = o.optBoolean("firstChargeShouldHaveBonus", false);
		JSONArray a;

		if (o.has("layouts")) {
			a = o.getJSONArray("layouts");
			for (int i = 0; i < a.length(); i++) {
				layouts.add(ArmsLayout.valueOf(a.getString(i)));
			}
			for (ArmsLayout l : all(ArmsLayout.class)) {
				if (l.addToHeraldicStyles.contains(name)) {
					layouts.add(l);
				}
			}
		} else {
			layouts.addAll(all(ArmsLayout.class));
		}
		if (o.has("charges")) {
			a = o.getJSONArray("charges");
			for (int i = 0; i < a.length(); i++) {
				charges.add(Charge.ofName(a.getString(i)));
				weightedCharges.add(Charge.ofName(a.getString(i)));
			}
			for (Charge c : all(Charge.class)) {
				if (c.addToHeraldicStyles.contains(name)) {
					charges.add(c);
					weightedCharges.add(c);
				}
			}
		} else {
			charges.addAll(all(Charge.class));
		}
		for (Charge c : charges) {
			if (c.bonus != Bonus.ofName("NO_BONUS") || c.tech != null) {
				weightedChargesWithBonusOrTech.add(c);
			}
		}
		if (o.has("layoutTinctures")) {
			a = o.getJSONArray("layoutTinctures");
			for (int i = 0; i < a.length(); i++) {
				layoutTinctures.add(Tincture.valueOf(a.getString(i)));
			}
			for (Tincture t : Tincture.values()) {
				if (t.addToHeraldicStylesAsLayoutTincture.contains(name)) {
					layoutTinctures.add(t);
				}
			}
		} else {
			layoutTinctures.addAll(Tincture.values());
		}
		if (o.has("chargeTinctures")) {
			a = o.getJSONArray("chargeTinctures");
			for (int i = 0; i < a.length(); i++) {
				chargeTinctures.add(Tincture.valueOf(a.getString(i)));
			}
			for (Tincture t : Tincture.values()) {
				if (t.addToHeraldicStylesAsChargeTincture.contains(name)) {
					chargeTinctures.add(t);
				}
			}
		} else {
			chargeTinctures.addAll(Tincture.values());
		}
		if (o.has("chargeSpawnWeights")) {
			JSONObject csw = o.getJSONObject("chargeSpawnWeights");
			for (Object k : csw.keySet()) {
				Charge c = Charge.ofName((String) k);
				int weight = csw.getInt((String) k);
				for (int i = 1; i < weight; i++) {
					weightedCharges.add(c);
					if (c.bonus != Bonus.ofName("NO_BONUS") || c.tech != null) {
						weightedChargesWithBonusOrTech.add(c);
					}
				}
			}
		}
	}
	
	public static HeraldicStyle ofName(String name) {
		return ofName(HeraldicStyle.class, name);
	}
}
