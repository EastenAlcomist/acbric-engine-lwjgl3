package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import org.json.JSONObject;

public class MedalPart extends Loadable {
	public final Img solid, outline;
	public final int layer;
	
	public MedalPart(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		solid = new Img(o.getJSONObject("solid").getString("src"), o.getJSONObject("solid").getInt("x"), o.getJSONObject("solid").getInt("y"), o.getJSONObject("solid").getInt("w"), o.getJSONObject("solid").getInt("h"), o.getJSONObject("solid").optBoolean("flipped", false));
		outline = new Img(o.getJSONObject("outline").getString("src"), o.getJSONObject("outline").getInt("x"), o.getJSONObject("outline").getInt("y"), o.getJSONObject("outline").getInt("w"), o.getJSONObject("outline").getInt("h"), o.getJSONObject("outline").optBoolean("flipped", false));
		layer = o.getInt("layer");
	}
	
	public static ArrayList<MedalPart> forLayer(int layer) {
		ArrayList<MedalPart> l = new ArrayList<MedalPart>();
		for (MedalPart e : all(MedalPart.class)) {
			if (layer == e.layer) {
				l.add(e);
			}
		}
		return l;
	}
	
	public static MedalPart ofName(String name) {
		return ofName(MedalPart.class, name);
	}
}
