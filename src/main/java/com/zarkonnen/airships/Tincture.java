package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;

public class Tincture extends Loadable {
	public final Clr tint;
	public final Color mapColor;
	public final Clr paintTint;
	public final Clr nicePaintTint;
	public final Color tintColor;
	public final boolean metal;
	public final float[] replacementValue;
	public PaintType paintType;
	public final ArrayList<String> addToHeraldicStylesAsLayoutTincture = new ArrayList<String>();
	public final ArrayList<String> addToHeraldicStylesAsChargeTincture = new ArrayList<String>();
	
	public Tincture(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		tint = new Clr(o.getJSONObject("pureColour").getInt("r"), o.getJSONObject("pureColour").getInt("g"), o.getJSONObject("pureColour").getInt("b"));
		metal = o.optBoolean("metal", false);
		paintTint = new Clr(tint.r, tint.g, tint.b, 100);
		nicePaintTint = new Clr(tint.r, tint.g, tint.b, 200).mix(0.2, Clr.DARK_GREY);
		replacementValue = new float[] {
			o.getJSONObject("moduleColour").getInt("r") / 255f,
			o.getJSONObject("moduleColour").getInt("g") / 255f,
			o.getJSONObject("moduleColour").getInt("b") / 255f
		};
		this.mapColor = new Color(tint.r, tint.g, tint.b, 64);
		this.tintColor = new Color(tint.r, tint.g, tint.b);
		if (o.has("addToHeraldicStylesAsLayoutTincture")) {
			JSONArray a = o.getJSONArray("addToHeraldicStylesAsLayoutTincture");
			for (int i = 0; i < a.length(); i++) {
				addToHeraldicStylesAsLayoutTincture.add(a.getString(i));
			}
		}
		if (o.has("addToHeraldicStylesAsChargeTincture")) {
			JSONArray a = o.getJSONArray("addToHeraldicStylesAsChargeTincture");
			for (int i = 0; i < a.length(); i++) {
				addToHeraldicStylesAsChargeTincture.add(a.getString(i));
			}
		}
	}

	public String getName() {
		return Lang._t("tincture_" + name);
	}
	
	public static Tincture valueOf(String name) {
		return ofName(Tincture.class, name);
	}
	
	public static ArrayList<Tincture> values() {
		return all(Tincture.class);
	}

	public int ordinal() {
		return values().indexOf(this);
	}
	
	public String name() {
		return name;
	}
}
