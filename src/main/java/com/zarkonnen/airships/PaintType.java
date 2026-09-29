package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;

import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class PaintType extends Loadable implements HasName {
	public final Clr tint;
	public final boolean metal;
	public final Tincture tinc;
	public final int armsTincIndex;
	
	public PaintType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		if (o.has("tincture")) {
			tinc = Tincture.valueOf(o.getString("tincture"));
			if (tinc.paintType != null) {
				throw new RuntimeException("Tincture " + tinc.name + " already referenced by a paint type.");
			}
			tinc.paintType = this;
			if (o.has("tint")) {
				JSONObject t = o.getJSONObject("tint");
				tint = new Clr(t.getInt("r"), t.getInt("g"), t.getInt("b"), t.optInt("a", 200));
			} else {
				tint = null;
			}
			armsTincIndex = 0;
			metal = tinc.metal;
		} else if (o.has("tint")) {
			JSONObject t = o.getJSONObject("tint");
			tint = new Clr(t.getInt("r"), t.getInt("g"), t.getInt("b"), t.optInt("a", 200));
			tinc = null;
			armsTincIndex = 0;
			metal = o.optBoolean("metal", false);
		} else {
			tint = null;
			tinc = null;
			armsTincIndex = o.getInt("armsTinctureIndex");
			metal = false;
		}
	}
	
	public float shinyOrPassthrough() {
		return tinc == null ? (metal ? 0.5f : 0.36f) : -1f;
	}
	
	public float shiny() {
		return metal ? 0.5f : 0.36f;
	}
	
	public PaintType getPaintType(CoatOfArms shipArms) {
		if (tint != null || tinc != null) {
			return this;
		}
		return shipArms.getTincture(armsTincIndex).paintType;
	}
	
	public Clr getTint(CoatOfArms shipArms) {
		if (tint != null) {
			return tint;
		}
		if (tinc != null) {
			return tinc.nicePaintTint;
		}
		return shipArms.getTincture(armsTincIndex).nicePaintTint;
	}
	
	public Clr getBaseTint() {
		if (tint != null) {
			return tint;
		}
		if (tinc != null) {
			return tinc.nicePaintTint;
		}
		return Clr.RED;
	}

	@Override
	public String getName() {
		if (tinc != null) {
			return tinc.getName();
		}
		return _t(name);
	}
	
	public static PaintType valueOf(String name) {
		return ofName(PaintType.class, name);
	}
	
	public static ArrayList<PaintType> values() {
		return all(PaintType.class);
	}
	
	public int ordinal() {
		return values().indexOf(this);
	}

	public boolean isArmsBased() {
		return tinc == null && tint == null;
	}
}
