package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class TerrainFeatureType extends Loadable {
	public boolean inWater;
	public boolean unclaimedLandOnly;
	public double density;
	public double minHeight, maxHeight;
	public int obstacleRadius;
	public double exclusionRadius;
	public double noiseXScale, noiseYScale, noiseXOffset, noiseYOffset, noiseZOffset, noiseThreshold;
	public ArrayList<Img> foregrounds = new ArrayList<Img>();
	public ArrayList<Img> backgrounds = null;
	public Clr backgroundColor = StrategicScreen.PARCHMENT;
	public CombatBackgroundFlavor combatBackgroundFlavor, combatBackgroundFlavorCoast, combatBackgroundFlavorOffCoast;
	
	public TerrainFeatureType(JSONObject o) {
		super(o.getString("name"), o.getInt("sort"));
		inWater = o.optBoolean("inWater", false);
		unclaimedLandOnly = o.optBoolean("unclaimedLandOnly", false);
		density = o.getDouble("density");
		minHeight = o.optDouble("minHeight", -1000);
		maxHeight = o.optDouble("maxHeight", 1000);
		obstacleRadius = o.optInt("obstacleRadius", 0);
		exclusionRadius = o.optInt("exclusionRadius", 0);
		noiseXScale = o.optDouble("noiseXScale", 0);
		noiseYScale = o.optDouble("noiseYScale", 0);
		noiseXOffset = o.optDouble("noiseXOffset", 0);
		noiseYOffset = o.optDouble("noiseYOffset", 0);
		noiseZOffset = o.optDouble("noiseZOffset", 0);
		noiseThreshold = o.optDouble("noiseThreshold", 0);
		if (o.has("combatBackgroundFlavor")) {
			combatBackgroundFlavor = CombatBackgroundFlavor.ofName(o.getString("combatBackgroundFlavor"));
		}
		if (o.has("combatBackgroundFlavorCoast")) {
			combatBackgroundFlavorCoast = CombatBackgroundFlavor.ofName(o.getString("combatBackgroundFlavorCoast"));
		}
		if (o.has("combatBackgroundFlavorOffCoast")) {
			combatBackgroundFlavorOffCoast = CombatBackgroundFlavor.ofName(o.getString("combatBackgroundFlavorOffCoast"));
		}
		if (o.has("backgroundColor")) {
			backgroundColor = new Clr(o.getJSONObject("backgroundColor").getInt("r"), o.getJSONObject("backgroundColor").getInt("g"), o.getJSONObject("backgroundColor").getInt("b"));
		}
		JSONArray a = o.getJSONArray("foregrounds");
		for (int i = 0; i < a.length(); i++) {
			JSONObject img = a.getJSONObject(i);
			foregrounds.add(new Img(img.getString("src"), img.getInt("x"), img.getInt("y"), img.getInt("w"), img.getInt("h"), img.optBoolean("flipped", false)));
		}
		if (o.has("backgrounds")) {
			backgrounds = new ArrayList<Img>();
			a = o.getJSONArray("backgrounds");
			for (int i = 0; i < a.length(); i++) {
				JSONObject img = a.getJSONObject(i);
				backgrounds.add(new Img(img.getString("src"), img.getInt("x"), img.getInt("y"), img.getInt("w"), img.getInt("h"), img.optBoolean("flipped", false)));
			}
		}
	}
	
	public static TerrainFeatureType ofName(String name) {
		return ofName(TerrainFeatureType.class, name);
	}
}
