package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import org.json.JSONObject;

public strictfp class ModuleCategory extends Loadable implements Comparator<ModuleType> {
	private Locale sortLocale;
	
	public ModuleCategory(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
	}
	
	public String getName() {
		return _t("modulecategory_" + name);
	}
	
	public static ModuleCategory ofName(String name) {
		return ofName(ModuleCategory.class, name);
	}
	
	private ArrayList<ModuleType> contents;
	
	public ArrayList<ModuleType> getContents() {
		if (contents == null) {
			contents = new ArrayList<ModuleType>();
			for (ModuleType mt : all(ModuleType.class)) {
				if (mt.in(this)) {
					contents.add(mt);
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
	public int compare(ModuleType t, ModuleType t1) {
		if (t.sort == t1.sort) {
			return t.getName().compareToIgnoreCase(t1.getName());
		}
		return t.sort - t1.sort;
	}
}
