package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONObject;

public class Tech extends Loadable {
	public static final int ALL_TIERS = 100000;
	
	public final int tier;
	public final double tierCost;
	public final ArrayList<String> dependencyNames = new ArrayList<String>();
	public final ArrayList<Tech> dependencies = new ArrayList<Tech>();
	public final ArrayList<Choice> choices = new ArrayList<Choice>();
	public final Bonus requiresBonus;
	public int row;
	public ArrayList<String> aliases = new ArrayList<String>();
	
	public static Tech[][] layout;
	private static boolean[][] depMatrix;
	private int depMatrixID;
	private static int maxTier;
	private static int minTier;
	
	public transient int x, y, w, h, newDependencyDistance, oldDependencyDistance;
	
	public static class Choice implements Comparable<Choice>, HasName {
		private final Img fallbackImg = new Img("tech", 128, 0, 64, 64, false);
		
		public final String name;
		public final BonusSet bonuses;
		public final ArrayList<Bonus> bonusList;
		private final Img img;
		public final Tech tech;
		public final boolean isSpider;
		
		public Choice(JSONObject o, Tech tech) {
			this.tech = tech;
			name = o.getString("name");
			bonuses = new BonusSet();
			JSONArray a = o.getJSONArray("bonuses");
			for (int i = 0; i < a.length(); i++) {
				bonuses.add(Bonus.ofNameOrNone(a.getString(i)));
			}
			bonusList = bonuses.list();
			isSpider = o.optBoolean("isSpider", false);
			JSONObject imgO = o.getJSONObject("img");
			img = new Img(imgO.getString("src"), imgO.getInt("x"), imgO.getInt("y"), imgO.getInt("w"), imgO.getInt("h"), imgO.optBoolean("flipped", false));
		}
		
		public ArrayList<Tech.Choice> getAllDependenciesIncludingThis() {
			ArrayList<Tech> ts = all(Tech.class);
			ArrayList<Tech.Choice> deps = new ArrayList<Tech.Choice>();
			deps.add(this);
			lp: while (true) {
				candidates: for (Tech candidate : ts) {
					for (Choice c : candidate.choices) {
						if (deps.contains(c)) {
							continue candidates;
						}
					}
					for (Tech.Choice dep : deps) {
						if (candidate.dependsOn(dep.tech)) {
							deps.add(candidate.choices.get(0));
							continue lp;
						}
					}
				}
				break;
			}
			Collections.sort(deps);
			return deps;
		}
		
		public int cost(Empire researcher, WorldMap wm) {
			int base = tech.baseCost(researcher.bonuses);
			if (hasCoResearcher(researcher, wm)) {
				return base * (100 - EmpireStat.RESEARCH_COST_DECREASE_PERCENT_FOR_TREATY.get(researcher.bonuses)) / 100;
			}
			return base;
		}
		
		public boolean hasCoResearcher(Empire researcher, WorldMap wm) {
			for (Relationship rel : wm.getRelationships(researcher)) {
				if (rel.researchTreaty && rel.other(researcher).techs.contains(this)) {
					return true;
				}
			}
			return false;
		}

		public ArrayList<Empire> coResearchers(Empire researcher, WorldMap wm) {
			ArrayList<Empire> es = new ArrayList<Empire>();
			for (Relationship rel : wm.getRelationships(researcher)) {
				if (rel.researchTreaty && rel.other(researcher).techs.contains(this)) {
					es.add(rel.other(researcher));
				}
			}
			return es;
		}
		
		public ArrayList<Tech.Choice> getAllPrerequisitesIncludingThis() {
			ArrayList<Tech> ts = all(Tech.class);
			ArrayList<Tech.Choice> prereqs = new ArrayList<Tech.Choice>();
			prereqs.add(this);
			lp: while (true) {
				candidates: for (Tech candidate : ts) {
					for (Choice c : candidate.choices) {
						if (prereqs.contains(c)) {
							continue candidates;
						}
					}
					for (Tech.Choice prereq : prereqs) {
						if (prereq.tech.dependsOn(candidate)) {
							prereqs.add(candidate.choices.get(0));
							continue lp;
						}
					}
				}
				break;
			}
			Collections.sort(prereqs);
			return prereqs;
		}

		@Override
		public int compareTo(Choice o) {
			if (tech == o.tech) {
				return tech.choices.indexOf(this) - tech.choices.indexOf(o);
			}
			if (tech.tier == o.tech.tier) {
				return tech.sort - o.tech.sort;
			}
			return tech.tier - o.tech.tier;
		}

		@Override
		public String getName() {
			return _t("tech_" + name);
		}

		public Img getImg() {
			return isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() ? fallbackImg : img;
		}
	}
	
	public Tech(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		
		tier = o.getInt("tier");
		
		if (o.has("choices")) {
			JSONArray a = o.getJSONArray("choices");
			for (int i = 0; i < a.length(); i++) {
				choices.add(new Choice(a.getJSONObject(i), this));
			}
		} else {
			choices.add(new Choice(o, this));
		}
		
		if (o.has("dependencies")) {
			JSONArray a = o.getJSONArray("dependencies");
			for (int i = 0; i < a.length(); i++) {
				dependencyNames.add(a.getString(i));
			}
		}
		
		if (o.has("requiresBonus")) {
			requiresBonus = Bonus.ofName(o.getString("requiresBonus"));
		} else {
			requiresBonus = null;
		}
		
		if (o.has("aliases")) {
			JSONArray a = o.getJSONArray("aliases");
			for (int i = 0; i < a.length(); i++) {
				aliases.add(a.getString(i));
			}
		}
		tierCost = StrictMath.pow(EmpireStat.RESEARCH_COST_EXPONENT, tier);
	}
	
	public boolean visible(Empire e) {
		return !(requiresBonus != null && !e.bonuses().contains[requiresBonus.ordinal()]);
	}
	
	public boolean available(Empire e) {
		int depSz = dependencies.size();
		for (int depI = 0; depI < depSz; depI++) {
			boolean hasDep = false;
			Tech dep = dependencies.get(depI);
			int techsSz = e.techs.size();
			for (int techsI = 0; techsI < techsSz; techsI++) {
				if (dep == e.techs.get(techsI).tech) {
					hasDep = true;
				}
			}
			if (!hasDep) {
				return false;
			}
		}
		return visible(e);
	}
	
	public Tech.Choice getChoice(String name) {
		for (Tech.Choice c : choices) {
			if (c.name.equals(name)) {
				return c;
			}
		}
		return choices.get(0);
	}
	
	public static boolean hasOfName(String name) {
		if (hasOfName(Tech.class, name)) { return true; }
		for (Tech t : all(Tech.class)) {
			if (t.aliases.contains(name)) { return true; }
		}
		return false;
	}
	
	public static Tech ofName(String name) {
		if (hasOfName(Tech.class, name)) { return ofName(Tech.class, name); }
		for (Tech t : all(Tech.class)) {
			if (t.aliases.contains(name)) { return t; }
		}
		throw new NotFoundException(Tech.class, name, _t("no_loadable_of_name", Tech.class.getSimpleName(), name));
	}
	
	public static Choice choiceOfName(String name) {
		for (Tech t : all(Tech.class)) {
			for (Choice c : t.choices) {
				if (c.name.equals(name)) {
					return c;
				}
			}
		}
		for (Tech t : all(Tech.class)) {
			if (t.aliases.contains(name)) {
				return t.choices.get(0);
			}
		}
		return null;
	}
	
	public static Choice findProvider(Bonus required) {
		ArrayList<Tech> ts = all(Tech.class);
		int tsz = ts.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tech t = ts.get(ti);
			int csz = t.choices.size();
			for (int ci = 0; ci < csz; ci++) {
				Choice c = t.choices.get(ci);
				if (c.bonuses.contains[required.ordinal()]) {
					return c;
				}
			}
		}
		return null;
	}
	
	public boolean dependsOn(Tech t2) {
		return t2 != null && depMatrix[depMatrixID][t2.depMatrixID];
	}
	
	public int baseCost(BonusSet bonuses) {
		return (int) (EmpireStat.BASE_RESEARCH_COST.get(bonuses) * EmpireStat.RESEARCH_COST_MULTIPLIER.get(bonuses) * Empire.MS_PER_INCOME * tierCost);
	}
	
	public static int getMinTier() {
		return minTier;
	}
	
	public static int getMaxTier() {
		return maxTier;
	}
	
	public static BonusSet getStandardNonArmsBonuses() {
		BonusSet bs = getBonusesForTier(maxTier);
		for (Charge c : all(Charge.class)) {
			bs.remove(c.bonus);
		}
		return bs;
	}
	
	public static BonusSet getStandardBonuses() {
		return getBonusesForTier(maxTier);
	}
	
	public static BonusSet getBonusesForTier(int tier) {
		BonusSet bonuses = new BonusSet();
		bonuses.addAll(Bonus.standardSet);
		for (Tech t : all(Tech.class)) {
			if (t.tier > tier) { continue; }
			if (t.choices.size() > 1) { continue; }
			for (Choice c : t.choices) {
				bonuses.addAll(c.bonuses);
			}
		}
		return bonuses;
	}
	
	private static int yPos(Tech t) {
		int yPos = 0;
		for (int row = 0; row < layout[t.tier - minTier].length; row++) {
			if (t == layout[t.tier - minTier][row]) { return yPos; }
			int rowH = 1;
			for (int col = 0; col < layout.length; col++) {
				Tech t2 = layout[col][row];
				if (t2 != null) {
					rowH = StrictMath.max(rowH, t2.choices.size());
				}
			}
			yPos += rowH;
		}
		throw new RuntimeException("Cannot find tech " + t.name + " in layout tier " + t.tier + ".");
	}
	
	public static boolean postLoad() {
		int idCounter = 0;
		ArrayList<Tech> techs = all(Tech.class);
		for (Tech t : techs) {
			t.depMatrixID = idCounter++;
			for (String dep : t.dependencyNames) {
				if (!hasOfName(dep)) {
					if (t.sourceMod != null) {
						t.sourceMod.loadLog += "\nUnknown dependency: " + dep;
						t.sourceMod.loadFailed = true;
						return false;
					} else {
						continue;
					}
				}
				t.dependencies.add(ofName(dep));
			}
		}
		depMatrix = new boolean[idCounter][idCounter];
		for (Tech a : techs) { for (Tech b : techs) {
			if (a.dependencies.contains(b)) {
				depMatrix[a.depMatrixID][b.depMatrixID] = true;
			}
		}}
		
		minTier = 0;
		maxTier = 0;
		for (Tech t : techs) {
			maxTier = StrictMath.max(maxTier, t.tier);
			minTier = StrictMath.min(minTier, t.tier);
		}

		int maxTierSize = 0;

		for (int tier = minTier; tier <= maxTier; tier++) {
			int size = 0;
			for (Tech t : all(Tech.class)) {
				if (t.tier == tier) {
					size++;
				}
			}
			maxTierSize = StrictMath.max(maxTierSize, size);
		}

		layout = new Tech[maxTier + 1 - minTier][maxTierSize + 5];

		for (int tier = minTier; tier <= maxTier; tier++) {
			int y = 0;
			for (Tech t : techs) {
				if (t.tier == tier) {
					layout[tier - minTier][y] = t;
					y++;
				}
			}
		}

		boolean progress = true;
		while (progress) {
			progress = false;
			for (int column = 0; column < layout.length; column++) {
				for (int row = 0; row < layout[column].length - 1; row++) {
					Tech t = layout[column][row];
					if (t == null) { continue; }
					if (layout[column][row + 1] != null) { continue; }
					int tDists = 0;
					int tDistsOneDown = 0;
					for (Tech a : techs) {
						for (Tech dep : techs) {
							if (depMatrix[a.depMatrixID][dep.depMatrixID] || depMatrix[dep.depMatrixID][a.depMatrixID]) {
								tDists += StrictMath.abs(yPos(a) - yPos(dep));
								if (t == a) {
									tDistsOneDown += StrictMath.abs(yPos(a) + 1 - yPos(dep));
								} else if (t == dep) {
									tDistsOneDown += StrictMath.abs(yPos(a) - yPos(dep) - 1);
								} else {
									tDistsOneDown += StrictMath.abs(yPos(a) - yPos(dep));
								}
							}
						}
					}
					if (tDistsOneDown < tDists) {
						layout[column][row + 1] = t;
						layout[column][row] = null;
						progress = true;
					}
				}
			}
		}
		
		// Shift-down
		// If we moved everything to the right and some below us down by one, would this purely reduce distances?
		calcDistances();
		new2OldDists();
		for (int column = 0; column < layout.length; column++) {
			for (int row = 0; row < layout[0].length - 1; row++) {
				if (layout[column][row] == null) { continue; }
				// How much can we shift down at once?
				blockH: for (int blockHeight = layout[0].length - row - 1; blockHeight > 0; blockHeight--) {
					blockW: for (int blockWidth = layout.length - column; blockWidth > 0; blockWidth--) {
						for (int blockCol = column; blockCol < column + blockWidth; blockCol++) {
							if (layout[blockCol][row + blockHeight] != null) { continue blockW; } // Can't move block down.
						}

						// Attempt shift-down.
						for (int blockCol = column; blockCol < column + blockWidth; blockCol++) {
							System.arraycopy(layout[blockCol], row, layout[blockCol], row + 1, blockHeight);
							layout[blockCol][row] = null;
						}

						calcDistances();
						if (distProgress()) {
							//System.out.println("shift " + column + ", " + row + " " + blockWidth + "x" + blockHeight);
							new2OldDists();
						} else {
							// Undo shift
							for (int blockCol = column; blockCol < column + blockWidth; blockCol++) {
								System.arraycopy(layout[blockCol], row + 1, layout[blockCol], row, blockHeight);
								layout[blockCol][row + blockHeight] = null;
							}
						}
					}
				}
			}
		}
				
		for (int column = 0; column < layout.length; column++) {
			for (int row = 0; row < layout[0].length; row++) {
				if (layout[column][row] != null) {
					layout[column][row].row = row;
				}
			}
		}
		
		for (Tech t : techs) {
			Collections.sort(t.dependencies, new Comparator<Tech>() {
				@Override
				public int compare(Tech o1, Tech o2) {
					return o1.row - o2.row;
				}
			});
		}
		
		return true;
	}
	
	private static void calcDistances() {
		for (int column = 1; column < layout.length; column++) {
			for (int row = 0; row < layout[0].length; row++) {
				if (layout[column][row] == null) { continue; }
				layout[column][row].newDependencyDistance = 0;
				for (int depCol = 0; depCol < column; depCol++) {
					for (int depRow = 0; depRow < layout[0].length; depRow++) {
						if (layout[column][row].dependsOn(layout[depCol][depRow])) {
							layout[column][row].newDependencyDistance += Math.abs(row - depRow);
						}
					}
				}
			}
		}
	}
	
	private static void new2OldDists() {
		for (int column = 1; column < layout.length; column++) {
			for (int row = 0; row < layout[0].length; row++) {
				if (layout[column][row] == null) { continue; }
				layout[column][row].oldDependencyDistance = layout[column][row].newDependencyDistance;
			}
		}
	}
	
	private static boolean distProgress() {
		boolean progress = false;
		for (int column = 1; column < layout.length; column++) {
			for (int row = 0; row < layout[0].length; row++) {
				if (layout[column][row] == null) { continue; }
				if (layout[column][row].oldDependencyDistance < layout[column][row].newDependencyDistance) {
					return false;
				}
				if (layout[column][row].oldDependencyDistance > layout[column][row].newDependencyDistance) {
					progress = true;
				}
			}
		}
		return progress;
	}
}
