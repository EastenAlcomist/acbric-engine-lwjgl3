package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Utils.Pair;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Clr;
import java.util.Random;

public class LandscapeType extends Loadable {
	public final ArrayList<CombatBackgroundFlavor> backgroundFlavors = new ArrayList<CombatBackgroundFlavor>();
	public final double hills;
	public final boolean hasTrees;
	public final int minTreeClusterDistance;
	public final int maxTreeClusterDistance;
	public final int minTreeDistance;
	public final int maxTreeDistance;
	public final int minTreeClusterSize;
	public final int maxTreeClusterSize;
	public final int minSmallTreeHeight;
	public final int maxSmallTreeHeight;
	public final int minLargeTreeHeight;
	public final int maxLargeTreeHeight;
	public final double largeTreeP;
	public final double bushDensity;
	public final double specialGrassDensity;
	public final ArrayList<LandBlockType> specialGrasses = new ArrayList<LandBlockType>();
	public final double specialFloaterTopDensity;
	public final ArrayList<LandBlockType> specialFloaterTops = new ArrayList<LandBlockType>();
	public final ArrayList<Pair<Integer, TimeOfDay>> weightedTimesOfDay = new ArrayList<Pair<Integer, TimeOfDay>>();
	public final ArrayList<TimeOfDay> timesOfDay = new ArrayList<TimeOfDay>();
	public final LandBlockType soil;
	public final LandBlockType rock;
	public final LandBlockType suspendiumOre;
	public final LandBlockType grass;
	public final LandBlockType bush;
	public final LandBlockType smallTreeRoot;
	public final LandBlockType smallTreeTrunk;
	public final LandBlockType smallTreeCrown;
	public final LandBlockType largeTreeRoot;
	public final LandBlockType largeTreeTrunk;
	public final LandBlockType largeTreeCrown;
	public final LandBlockType floaterTop;
	public final LandBlockType floaterUp;
	public final LandBlockType floaterMiddle;
	public final LandBlockType floaterDown;
	public final LandBlockType floaterDowner;
	public final LandBlockType floaterDownerRock;
	public final LandBlockType floaterDownerSuspendium;
	public final LandBlockType floaterBottom;
	public final boolean hasFloaters;
	public final int spawnWeightPercent;
	public final boolean hasWater, deepWater, coast;
	public final boolean canChoose;
	public final int lakeMinDepth, lakeMaxDepth, lakeMinWidth, lakeMaxWidth, lakeMaxDistFromCenter;
	
	public LandscapeType(JSONObject o) {
		super(o.getString("name"));
		soil = LandBlockType.ofName(o.optString("soil", "SOIL"));
		rock = LandBlockType.ofName(o.optString("rock", "ROCK"));
		suspendiumOre = LandBlockType.ofName(o.optString("suspendiumOre", "SUSPENDIUM_ORE"));
		grass = LandBlockType.ofName(o.optString("grass", "GRASS"));
		bush = LandBlockType.ofName(o.optString("bush", "BUSH"));
		smallTreeRoot = LandBlockType.ofName(o.optString("smallTreeRoot", "THIN_TRUNK"));
		smallTreeTrunk = LandBlockType.ofName(o.optString("smallTreeTrunk", "THIN_TRUNK"));
		smallTreeCrown = LandBlockType.ofName(o.optString("smallTreeCrown", "SMALL_CROWN"));
		largeTreeRoot = LandBlockType.ofName(o.optString("largeTreeRoot", "ROOTS"));
		largeTreeTrunk = LandBlockType.ofName(o.optString("largeTreeTrunk", "TRUNK"));
		largeTreeCrown = LandBlockType.ofName(o.optString("largeTreeCrown", "CROWN"));
		floaterTop = LandBlockType.ofName(o.optString("floaterTop", "FLOATER_TOP"));
		floaterUp = LandBlockType.ofName(o.optString("floaterUp", "FLOATER_UP"));
		floaterMiddle = LandBlockType.ofName(o.optString("floaterMiddle", "FLOATER_MIDDLE"));
		floaterDown = LandBlockType.ofName(o.optString("floaterDown", "FLOATER_DOWN"));
		floaterDowner = LandBlockType.ofName(o.optString("floaterDowner", "FLOATER_DOWNER"));
		floaterDownerRock = LandBlockType.ofName(o.optString("floaterDownerRock", "FLOATER_DOWNER_ROCK"));
		floaterDownerSuspendium = LandBlockType.ofName(o.optString("floaterDownerSuspendium", "FLOATER_DOWNER_SUSPENDIUM"));
		floaterBottom = LandBlockType.ofName(o.optString("floaterBottom", "FLOATER_BOTTOM"));
		hasFloaters = o.optBoolean("hasFloaters", true);
		hasWater = o.optBoolean("hasWater", false);
		deepWater = o.optBoolean("deepWater", false);
		coast = o.optBoolean("coast", false);
		canChoose = o.optBoolean("canChoose", true);
		lakeMinDepth = o.optInt("lakeMinDepth", 0);
		lakeMaxDepth = o.optInt("lakeMaxDepth", 0);
		lakeMinWidth = o.optInt("lakeMinWidth", 0);
		lakeMaxWidth = o.optInt("lakeMaxWidth", 0);
		lakeMaxDistFromCenter = o.optInt("lakeMaxDistFromCenter", 0);
		JSONArray a = o.getJSONArray("backgroundFlavors");
		for (int i = 0; i < a.length(); i++) {
			backgroundFlavors.add(CombatBackgroundFlavor.ofName(a.getString(i)));
		}
		hills = o.optDouble("hills", 0);
		if (o.has("treeDensity")) {
			if (o.getDouble("treeDensity") > 0) {
				hasTrees = true;
				minTreeClusterDistance = 1;
				maxTreeClusterDistance = 1;
				minTreeDistance = 4;
				maxTreeDistance = 30;
				minTreeClusterSize = 100;
				maxTreeClusterSize = 100;
			} else {
				hasTrees = false;
				minTreeClusterDistance = 0;
				maxTreeClusterDistance = 0;
				minTreeDistance = 0;
				maxTreeDistance = 0;
				minTreeClusterSize = 0;
				maxTreeClusterSize = 0;
			}
		} else if (o.optBoolean("hasTrees", false)) {
			hasTrees = true;
			minTreeClusterDistance = o.getInt("minTreeClusterDistance");
			maxTreeClusterDistance = o.getInt("maxTreeClusterDistance");
			minTreeDistance = o.getInt("minTreeDistance");
			maxTreeDistance = o.getInt("maxTreeDistance");
			minTreeClusterSize = o.getInt("minTreeClusterSize");
			maxTreeClusterSize = o.getInt("maxTreeClusterSize");
		} else {
			hasTrees = false;
			minTreeClusterDistance = 0;
			maxTreeClusterDistance = 0;
			minTreeDistance = 0;
			maxTreeDistance = 0;
			minTreeClusterSize = 0;
			maxTreeClusterSize = 0;
		}
		minSmallTreeHeight = o.optInt("minSmallTreeHeight", 1);
		maxSmallTreeHeight = o.optInt("maxSmallTreeHeight", 2);
		minLargeTreeHeight = o.optInt("minLargeTreeHeight", 2);
		maxLargeTreeHeight = o.optInt("maxLargeTreeHeight", 3);
		largeTreeP = o.optDouble("largeTreeP", 1);
		bushDensity = o.optDouble("bushDensity");
		if (o.has("specialGrasses")) {
			a = o.getJSONArray("specialGrasses");
			for (int i = 0; i < a.length(); i++) {
				specialGrasses.add(LandBlockType.ofName(a.getString(i)));
			}
		}
		if (specialGrasses.isEmpty()) {
			specialGrassDensity = 0;
		} else {
			specialGrassDensity = o.getDouble("specialGrassDensity");
		}
		
		if (o.has("specialFloaterTops")) {
			a = o.getJSONArray("specialFloaterTops");
			for (int i = 0; i < a.length(); i++) {
				specialFloaterTops.add(LandBlockType.ofName(a.getString(i)));
			}
		}
		if (specialFloaterTops.isEmpty()) {
			specialFloaterTopDensity = 0;
		} else {
			specialFloaterTopDensity = o.getDouble("specialFloaterTopDensity");
		}
		if (o.has("timesOfDay")) {
			a = o.getJSONArray("timesOfDay");
			for (int i = 0; i < a.length(); i++) {
				weightedTimesOfDay.add(new Pair<Integer, TimeOfDay>(a.getJSONObject(i).getInt("spawnWeight"), TimeOfDay.ofName(a.getJSONObject(i).getString("name"))));
				timesOfDay.add(TimeOfDay.ofName(a.getJSONObject(i).getString("name")));
			}
		} else {
			for (TimeOfDay tod : all(TimeOfDay.class)) {
				if (!tod.standard) { continue; }
				weightedTimesOfDay.add(new Pair<Integer, TimeOfDay>(1, tod));
				timesOfDay.add(tod);
			}
		}
		spawnWeightPercent = o.optInt("spawnWeightPercent", 100);
	}
	
	public static ArrayList<LandscapeType> getChooseables() {
		ArrayList<LandscapeType> l = all(LandscapeType.class);
		for (int i = 0; i < l.size(); i++) {
			if (!l.get(i).canChoose) {
				l.remove(i);
				i--;
			}
		}
		return l;
	}
	
	public static LandscapeType ofName(String name) {
		return ofName(LandscapeType.class, name);
	}
	
	public String getName() {
		return _t("landscape_" + name);
	}
	
	public static LandscapeType getRandom(ArrayList<LandscapeType> types, GuardedRandom r) {
		int total = 0;
		for (int i = 0; i < types.size(); i++) {
			total += types.get(i).spawnWeightPercent;
		}
		if (total <= 0) {
			return LandscapeType.ofName("GRASSLAND");
		}
		int roll = r.nextInt(total);
		for (int i = 0; i < types.size(); i++) {
			roll -= types.get(i).spawnWeightPercent;
			if (roll <= 0) {
				return types.get(i);
			}
		}
		return types.get(types.size() - 1);
	}
}
