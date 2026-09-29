package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.Lang._tWithFallback;
import com.zarkonnen.catengine.Img;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class Bonus extends Loadable implements HasName {
	private int ordinal = -1;
	private final ArrayList<Airship> enabledSpecialConstructions = new ArrayList<Airship>();
	public final boolean standard;
	public static BonusSet standardSet = new BonusSet();
	
	public Bonus(JSONObject o) {
		super(o.getString("name"));
		standard = o.optBoolean("standard", false);
		BonusSet.resetCount();
	}
	
	public static void postLoad2() {
		standardSet = new BonusSet();
		for (Bonus b : all(Bonus.class)) {
			if (b.standard) {
				standardSet.add(b);
			}
			b.enabledSpecialConstructions.clear();
			HashMap<String, Airship> byFileName = new HashMap<String, Airship>();
			File dir = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "bonusConstructions"), b.name());
			File[] fs = dir.listFiles();
			if (fs != null) {
				Arrays.sort(fs, new FileNameComparator());
				for (File f : fs) {
					if (!f.getName().startsWith(".") && f.getName().endsWith(".json")) {
						try {
							Airship s = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
							s.isBonusConstruction = true;
							byFileName.put(f.getName(), s);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			for (Expansion m : Expansion.enableds()) {
				dir = new File(new File(m.getDataDir(), "bonusConstructions"), b.name());
				fs = dir.listFiles();
				if (fs != null) {
					Arrays.sort(fs, new FileNameComparator());
					for (File f : fs) {
						if (!f.getName().startsWith(".") && f.getName().endsWith(".json")) {
							try {
								Airship s = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
								s.isBonusConstruction = true;
								byFileName.put(f.getName(), s);
							} catch (IOException e) {
								e.printStackTrace();
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			for (Mod m : Mod.getEnabledMods()) {
				dir = new File(new File(m.dir, "bonusConstructions"), b.name());
				fs = dir.listFiles();
				if (fs != null) {
					Arrays.sort(fs, new FileNameComparator());
					for (File f : fs) {
						if (!f.getName().startsWith(".") && f.getName().endsWith(".json")) {
							try {
								Airship s = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
								s.isBonusConstruction = true;
								byFileName.put(f.getName(), s);
							} catch (IOException e) {
								e.printStackTrace();
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			ArrayList<String> keys = new ArrayList<String>(byFileName.keySet());
			Collections.sort(keys);
			for (String k : keys) {
				b.enabledSpecialConstructions.add(byFileName.get(k));
			}
		}
	}
		
	public ArrayList<Airship> getEnabledSpecialConstructions() {
		return enabledSpecialConstructions;
	}
	
	public String name() { return name; }
	public int ordinal() {
		if (ordinal == -1) {
			ordinal = all(Bonus.class).indexOf(this);
		}
		return ordinal;
	}
	
	public static ArrayList<Bonus> values() {
		return all(Bonus.class);
	}
	
	public static Bonus ofName(String name) {
		return ofName(Bonus.class, name);
	}
	
	public static Bonus ofNameOrNone(String name) {
		if (hasOfName(Bonus.class, name)) {
			return ofName(name);
		} else {
			return ofName("NO_BONUS");
		}
	}
	
	public static Bonus ofNameOrNull(String name) {
		if (hasOfName(Bonus.class, name)) {
			return ofName(name);
		} else {
			return null;
		}
	}

	public String getName() {
		return _t("bonus_" + name);
	}

	public String getDesc() {
		return _tWithFallback("bonus_" + name + "_desc", "bonus_" + name);
	}
	
	public Tech.Choice getTech() {
		for (Tech t : all(Tech.class)) {
			for (Tech.Choice c : t.choices) {
				if (c.bonuses.contains[this.ordinal()]) {
					return c;
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}