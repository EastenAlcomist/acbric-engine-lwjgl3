package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import org.json.JSONObject;

public strictfp class VariantType extends Loadable {
	public final Img icon;
	public final boolean isDefault;
	public final boolean reverseIterationOrderIfFlipped;
	private static VariantType def;
	
	public VariantType(JSONObject o) {
		super(o.getString("name"));
		icon = new Img(o.getJSONObject("icon").getString("src"), o.getJSONObject("icon").getInt("x"), o.getJSONObject("icon").getInt("y"), o.getJSONObject("icon").optInt("w", 16), o.getJSONObject("icon").optInt("h", 16), o.getJSONObject("icon").optBoolean("flipped", false));
		isDefault = o.optBoolean("isDefault", false);
		reverseIterationOrderIfFlipped = o.optBoolean("reverseIterationOrderIfFlipped", false);
		if (isDefault) {
			def = this;
		}
	}
	
	public static VariantType ofName(String name) {
		return ofName(VariantType.class, name);
	}
	
	public static VariantType getDefault() {
		return def;
	}
}
