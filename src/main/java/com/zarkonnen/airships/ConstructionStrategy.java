package com.zarkonnen.airships;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConstructionStrategy extends Loadable {
	public ArrayList<ArrayList<String>> shipNames = new ArrayList<ArrayList<String>>(), landshipNames = new ArrayList<ArrayList<String>>(), buildingNames = new ArrayList<ArrayList<String>>();
	public ArrayList<Tech.Choice> techs = new ArrayList<Tech.Choice>();
	private final ArrayList<ArrayList<Airship>> ships = new ArrayList<ArrayList<Airship>>(), landships = new ArrayList<ArrayList<Airship>>(), buildings = new ArrayList<ArrayList<Airship>>();
	private final HashMap<String, Airship> name2Ship = new HashMap<String, Airship>(), name2Landship = new HashMap<String, Airship>(), name2Building = new HashMap<String, Airship>();
	public final ArrayList<ArrayList<String>> shipUpgradeSequences = new ArrayList<ArrayList<String>>(), landshipUpgradeSequences = new ArrayList<ArrayList<String>>(), buildingUpgradeSequences = new ArrayList<ArrayList<String>>();
	public HashSet<Charge> charges = new HashSet<Charge>();
	public Charge requiredCharge;
	public final DifficultyLevel minDifficultyLevel;
	public final DifficultyLevel maxDifficultyLevel;
	public ArrayList<CityUpgradeType> desiredSpecials = new ArrayList<CityUpgradeType>();
	public String displayName;
	public boolean enabled;
	private boolean notForMP;
	
	public static String getReport() {
		StringBuilder sb = new StringBuilder();
		for (ConstructionStrategy cs : all(ConstructionStrategy.class)) {
			sb.append("\n").append(cs.name);
			ArrayList<ArrayList<ArrayList<Airship>>> llll = new ArrayList<ArrayList<ArrayList<Airship>>>();
			llll.add(cs.ships);
			llll.add(cs.landships);
			llll.add(cs.buildings);
			ArrayList<String> names = new ArrayList<String>(); names.add("Ships"); names.add("Landships"); names.add("Buildings");
			for (int iiii = 0; iiii < llll.size(); iiii++) {
				ArrayList<ArrayList<Airship>> lll = llll.get(iiii);
				sb.append("\n  ").append(names.get(iiii));
				for (int iii = 0; iii < lll.size(); iii++) {
					ArrayList<Airship> ll = lll.get(iii);
					sb.append("\n    ").append(iii);
					for (int ii = 0; ii < ll.size(); ii++) {
						Airship l = ll.get(ii);
						sb.append("\n      ").append(l.getName()).append(" ").append(l.getCost());
					}
				}
			}
		}
		return sb.toString();
	}
	
	public static ArrayList<ConstructionStrategy> forCharge(Charge c, DifficultyLevel d) {
		ArrayList<ConstructionStrategy> l = new ArrayList<ConstructionStrategy>();
		for (ConstructionStrategy s : all(ConstructionStrategy.class)) {
			if (!s.enabled) { continue; }
			if (s.minDifficultyLevel != null && s.minDifficultyLevel.sort > d.sort) { continue; }
			if (s.maxDifficultyLevel != null && s.maxDifficultyLevel.sort < d.sort) { continue; }
			if (s.charges.contains(c) || s.requiredCharge == c) {
				l.add(s);
			}
		}
		if (!l.isEmpty()) { return l; }
		for (ConstructionStrategy s : all(ConstructionStrategy.class)) {
			if (!s.enabled) { continue; }
			if (s.minDifficultyLevel != null && s.minDifficultyLevel.sort > d.sort) { continue; }
			if (s.maxDifficultyLevel != null && s.maxDifficultyLevel.sort < d.sort) { continue; }
			if (s.requiredCharge == null) {
				l.add(s);
			}
		}
		if (!l.isEmpty()) { return l; }
		for (ConstructionStrategy s : all(ConstructionStrategy.class)) {
			if (!s.enabled) { continue; }
			if (s.requiredCharge == null) {
				l.add(s);
			}
		}
		return l;
	}
	
	private void loadTier(JSONObject o, String name, ArrayList<ArrayList<String>> names) {
		JSONArray a = o.getJSONArray(name);
		for (int i = 0; i < a.length(); i++) {
			ArrayList<String> tier = new ArrayList<String>();
			JSONArray a2 = a.getJSONArray(i);
			for (int j = 0; j < a2.length(); j++) {
				tier.add(a2.getString(j));
			}
			names.add(tier);
		}
	}
	
	public static void mergeSequences(ArrayList<ArrayList<String>> names) {
		ArrayList<ArrayList<String>> names2 = new ArrayList<ArrayList<String>>();
		boolean progress = true;
		while (progress) {
			progress = false;
			for (int i = 0; i < names.size(); i++) {
				ArrayList<String> postfix = names.get(i);
				boolean postfixHasCombined = false;
				for (int j = 0; j < names.size(); j++) {
					ArrayList<String> prefix = names.get(j);
					if (prefix.equals(postfix)) { continue; }
					if (prefix.get(prefix.size() - 1).equals(postfix.get(0))) {
						ArrayList<String> combined = new ArrayList<String>(prefix);
						combined.addAll(postfix.subList(1, postfix.size()));
						// Consume prefix.
						names.remove(prefix);
						names2.remove(prefix);
						j--;
						i--;
						names2.add(combined);
						postfixHasCombined = true;
						progress = true;
					}
				}
				if (!postfixHasCombined) {
					names2.add(postfix);
				}
			}
			
			names.clear();
			names.addAll(names2);
			names2.clear();
		}
	}
	
	public ConstructionStrategy(JSONObject o) {
		super(o.getString("name"));
		displayName = o.optString("displayName", o.getString("name"));
		enabled = o.optBoolean("enabled", true);
		loadTier(o, "shipTiers", shipNames);
		loadTier(o, "landshipTiers", landshipNames);
		loadTier(o, "buildingTiers", buildingNames);
		loadTier(o, "shipUpgradeSequences", shipUpgradeSequences);
		loadTier(o, "landshipUpgradeSequences", landshipUpgradeSequences);
		loadTier(o, "buildingUpgradeSequences", buildingUpgradeSequences);
		mergeSequences(shipUpgradeSequences);
		mergeSequences(landshipUpgradeSequences);
		mergeSequences(buildingUpgradeSequences);
		if (o.has("techs")) {
			JSONArray a = o.getJSONArray("techs");
			for (int i = 0; i < a.length(); i++) {
				Tech.Choice c = Tech.choiceOfName(a.getString(i));
				if (c == null) {
					System.out.println("Unknown tech choice " + a.getString(i));
				}
				if (c != null) {
					techs.add(c);
				}
			}
		}
		if (o.has("charges")) {
			JSONArray a = o.getJSONArray("charges");
			for (int i = 0; i < a.length(); i++) {
				if (hasOfName(Charge.class, a.getString(i))) {
					charges.add(Charge.ofName(a.getString(i)));
				}
			}
		}
		if (o.has("requiredCharge")) {
			requiredCharge = Charge.ofName(o.getString("requiredCharge"));
		}
		if (o.has("minDifficultyLevel")) {
			minDifficultyLevel = DifficultyLevel.ofName(o.getString("minDifficultyLevel"));
		} else {
			minDifficultyLevel = null;
		}
		if (o.has("maxDifficultyLevel")) {
			maxDifficultyLevel = DifficultyLevel.ofName(o.getString("maxDifficultyLevel"));
		} else {
			maxDifficultyLevel = null;
		}
		if (o.has("desiredSpecials")) {
			JSONArray a = o.getJSONArray("desiredSpecials");
			for (int i = 0; i < a.length(); i++) {
				desiredSpecials.add(CityUpgradeType.ofName(a.getString(i)));
			}
		}
	}
	
	public ArrayList<ArrayList<String>> getUpgradeSequences(ShipType t) {
		switch (t) {
			case AIRSHIP:
				return shipUpgradeSequences;
			case LANDSHIP:
				return landshipUpgradeSequences;
			case BUILDING:
				return buildingUpgradeSequences;
		}
		return null;
	}
	
	public Airship findDesign(ShipType t, String name) {
		switch (t) {
			case AIRSHIP:
				return name2Ship.get(name);
			case LANDSHIP:
				return name2Landship.get(name);
			case BUILDING:
				return name2Building.get(name);
		}
		return null;
	}
	
	public ArrayList<Airship> getBestAvailableDesignTier(ShipType t, BonusSet bonuses, boolean forMP) {
		ArrayList<ArrayList<Airship>> designs = getDesigns(t, forMP);
		ArrayList<ArrayList<String>> seqs = getUpgradeSequences(t);
		ArrayList<Airship> result = new ArrayList<Airship>();
		if (designs.isEmpty()) {
			return result;
		}
		ArrayList<Airship> bestTier = designs.get(designs.size() - 1);
		// Find the highest tier with a buildable ship in it.
		lp: for (int i = 0; i < designs.size(); i++) {
			ArrayList<Airship> tier = designs.get(i);
			for (int j = 0; j < tier.size(); j++) {
				if (bonuses.containsAll(tier.get(j).designRequiredBonuses)) {
					bestTier = tier;
					break lp;
				}
			}
		}
		// Go through all the ships in that tier and add them if they are buildable
		lp: for (int i = 0; i < bestTier.size(); i++) {
			Airship bestTierShip = bestTier.get(i);
			if (bonuses.containsAll(bestTierShip.designRequiredBonuses)) {
				result.add(bestTierShip);
			} else {
				// Otherwise see if there's an upgrade sequence predecessor that's buildable.
				for (int j = 0; j < seqs.size(); j++) {
					ArrayList<String> seq = seqs.get(j);
					int seqIndex = seq.indexOf(bestTierShip.designName);
					while (--seqIndex >= 0) { // Go down from the design in the sequence, or don't do anything if the index was 0 or -1.
						Airship seqShip = findDesign(t, seq.get(seqIndex));
						if (seqShip != null && bonuses.containsAll(seqShip.designRequiredBonuses)) {
							result.add(seqShip);
						}
						continue lp;
					}
				}
			}
		}
		
		return result;
	}
		
	/*public static void reload() {
		for (ConstructionStrategy cs : Loadable.all(ConstructionStrategy.class)) {
			cs.ships.clear();
			cs.landships.clear();
			cs.buildings.clear();
			cs.name2Ship.clear();
			cs.name2Landship.clear();
			cs.name2Building.clear();
		}
	}*/
	
	public ArrayList<ArrayList<Airship>> getDesigns(ShipType t, boolean forMP) {
		if (forMP && notForMP) {
			ships.clear();
			landships.clear();
			buildings.clear();
			name2Ship.clear();
			name2Landship.clear();
			name2Building.clear();
			notForMP = false;
		}
		switch (t) {
			case LANDSHIP:
				loadDesigns(t, landshipNames, landships, name2Landship, forMP);
				return landships;
			case BUILDING:
				loadDesigns(t, buildingNames, buildings, name2Building, forMP);
				return buildings;
			case AIRSHIP:
				loadDesigns(t, shipNames, ships, name2Ship, forMP);
				return ships;
		}
		return ships;
	}
	
	private void loadDesigns(ShipType st, ArrayList<ArrayList<String>> names, ArrayList<ArrayList<Airship>> designs, HashMap<String, Airship> map, boolean forMP) {
		if (designs.isEmpty()) {
			if (!forMP) {
				notForMP = true;
			}
			for (ArrayList<String> tierNames : names) {
				ArrayList<Airship> tier = new ArrayList<Airship>();
				designs.add(tier);
				for (String cname : tierNames) {
					Airship s = getShip(st, cname, forMP);
					if (s != null) {
						tier.add(s);
						map.put(cname, s);
					}
				}
			}
		}
	}
	
	public final Airship getShip(ShipType st, String name, boolean forMP) {
		ArrayList<Mod> ms = Mod.getEnabledMods();
		Collections.reverse(ms);
		for (Mod m : ms) {
			File shipF = new File(new File(m.dir, st.dirName), name + ".json");
			if (shipF.exists()) {
				for (int i = 0; i < 3; i++) {
					try {
						Airship s = new Airship(new JSONObject(FileUtils.readFileToString(shipF, "UTF-8")));
						s.designName = name;
						s.designRequiredBonuses = s.getRequiredBonuses();
						return s;
					} catch (Loadable.NotFoundException lnfe) {
						System.err.println("LoadableNotFoundException at " + shipF.getAbsolutePath() + " : " + lnfe.clazz.getSimpleName() + " " + lnfe.name);
						return null;
					} catch (Exception e) {
						if (!forMP) { return null; }
						if (i >= 2) {
							e.printStackTrace();
							throw new RuntimeException("Cannot load " + st.name().toLowerCase() + " " + name + " from " + m.getName() + ". The mod may have errors or be corrupt.", e);
						} else {
							try { Thread.sleep(10); } catch (Exception e2) {}
						}
					}
				}
			}
		}
		File shipF = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), st.dirName), name + ".json");
		for (int i = 0; i < 3; i++) {
			try {
				Airship s = new Airship(new JSONObject(FileUtils.readFileToString(shipF, "UTF-8")));
				s.designName = name;
				s.designRequiredBonuses = s.getRequiredBonuses();
				return s;
			} catch (Loadable.NotFoundException lnfe) {
				System.err.println("LoadableNotFoundException at " + shipF.getAbsolutePath() + " : " + lnfe.clazz.getSimpleName() + " " + lnfe.name);
				return null;
			} catch (Exception e) {
				if (!forMP) { return null; }
				if (i >= 2) {
					e.printStackTrace();
					throw new RuntimeException("Cannot load " + st.name().toLowerCase() + " " + name + ". Try restarting the game.", e);
				} else {
					try { Thread.sleep(10); } catch (Exception e2) {}
				}
			}
		}
		return null;
	}
	
	public static ConstructionStrategy ofName(String name) {
		return ofName(ConstructionStrategy.class, name);
	}
}
