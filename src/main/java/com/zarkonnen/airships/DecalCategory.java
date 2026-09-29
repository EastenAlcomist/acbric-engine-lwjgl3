package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import org.json.JSONObject;

public strictfp class DecalCategory extends Loadable implements Comparator<DecalType> {
	private Locale sortLocale;
	public Mod fromMod;
	
	public DecalCategory(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
	}
	
	public DecalCategory(Mod m) {
		super(m.id, 999999);
		fromMod = m;
	}
	
	public String getName() {
		return fromMod == null ? _t("decalcategory_" + name) : fromMod.getName();
	}
	
	public static DecalCategory ofName(String name) {
		return ofName(DecalCategory.class, name);
	}
	
	private ArrayList<DecalType> contents;
	
	public ArrayList<DecalType> getContents() {
		if (contents == null) {
			contents = new ArrayList<DecalType>();
			for (DecalType dt : all(DecalType.class)) {
				if (dt.in(this)) {
					contents.add(dt);
				}
			}
		}
		if (Lang.currentLocale != sortLocale) {
			Collections.sort(contents, this);
			sortLocale = Lang.currentLocale;
		}
		return contents;
	}

	@Override
	public int compare(DecalType t, DecalType t1) {
		if (t.sort == t1.sort) {
			return t.getName().compareToIgnoreCase(t1.getName());
		}
		return t.sort - t1.sort;
	}
	
	public static void postLoad() {
		for (DecalType dt : all(DecalType.class)) {
			if (dt.categories.isEmpty() && dt.sourceMod != null) {
				DecalCategory cat = null;
				for (DecalCategory dc : all(DecalCategory.class)) {
					if (dc.fromMod == dt.sourceMod) {
						cat = dc;
					}
				}
				if (cat == null) {
					cat = new DecalCategory(dt.sourceMod);
					cat.contents = new ArrayList<DecalType>();
					map.get(DecalCategory.class).put(cat.name, cat);
				}
				dt.categories.add(cat);
				cat.contents.add(dt);
			}
		}
	}
}
