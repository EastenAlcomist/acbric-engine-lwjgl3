package com.zarkonnen.airships;

import static com.zarkonnen.airships.Loadable.LOADABLES;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;

public abstract class Expansion {
	public final String name;
	public final String itchName;
	public final boolean installed;
	public boolean enabled;
	
	public long dataChecksum = 0;
	public long expectedDataChecksum = 0;
	
	public boolean hasCorrectChecksum() {
		return dataChecksum == expectedDataChecksum;
	}
	
	public long getDataChecksum() {
		long t = System.currentTimeMillis();
		long sum = 0;
		for (Class clazz : LOADABLES) {
			File dir = new File(getDataDir(), clazz.getSimpleName());
			if (dir.exists()) {
				try {
					sum += Mod.checksum(dir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			sum += Mod.checksum(new File(getDataDir(), "strings"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (sum == 0) {
			sum = 1;
		}
		System.out.println(name + " data checksum took " + (System.currentTimeMillis() - t) + " ms and is " + sum);
		return sum;
	}
		
	public Expansion(String name, String itchName) {
		this.name = name;
		this.itchName = itchName;
		installed = getInstallDir() != null;
		enabled = installed;
	}
	
	public final File getInstallDir() {
		File p = new File(AGame.getStaticGameDirectory(), "expansions");
		do {
			if (new File(new File(p, name), "expansion.lioq").exists()) {
				return new File(p, name);
			}
			if (new File(new File(new File(p, itchName), name), "expansion.lioq").exists()) {
				return new File(new File(p, itchName), name);
			}
		} while ((p = p.getParentFile()) != null);
		
		// Try Mac location
		String home = System.getProperty("user.home");
		if (home != null) {
			if (new File(new File(new File(new File(new File(new File(new File(new File(new File(new File(home), "Library"), "Application Support"), "Steam"), "steamapps"), "common"), "Airships Conquer the Skies"), "expansions"), name), "expansion.lioq").exists()) {
				return new File(new File(new File(new File(new File(new File(new File(new File(new File(home), "Library"), "Application Support"), "Steam"), "steamapps"), "common"), "Airships Conquer the Skies"), "expansions"), name);
			}
		}
		
		return null;
	}
	
	public File getDataDir() {
		return new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "crossplay"), name);
	}
	
	public static ArrayList<Expansion> ofNames(JSONArray names) {
		ArrayList<String> ns = new ArrayList<String>();
		for (int i = 0; i < names.length(); i++) {
			ns.add(names.getString(i));
		}
		ArrayList<Expansion> l = new ArrayList<Expansion>();
		for (Expansion ex : all()) {
			if (ns.contains(ex.name)) {
				l.add(ex);
			}
		}
		return l;
	}
	
	public static JSONArray names(ArrayList<Expansion> expansions) {
		JSONArray l = new JSONArray();
		for (Expansion ex : expansions) {
			l.put(ex.name);
		}
		return l;
	}
		
	public static ArrayList<Expansion> all() {
		ArrayList<Expansion> l = new ArrayList<Expansion>();
		l.add((Expansion) EHeroes.it);
		return l;
	}
	
	public static ArrayList<Expansion> enableds() {
		ArrayList<Expansion> l = all();
		for (Iterator<Expansion> it = l.iterator(); it.hasNext();) {
			if (!it.next().enabled) { it.remove(); }
		}
		return l;
	}
	
	public static ArrayList<Expansion> installeds() {
		ArrayList<Expansion> l = all();
		for (Iterator<Expansion> it = l.iterator(); it.hasNext();) {
			if (!it.next().installed) { it.remove(); }
		}
		return l;
	}

	public void addLoadBases(Lwjgl3Engine.MyInput in) {
		File dir = getDataDir();
		in.addLoadBase(new File(dir, "images"));
		in.addLoadBase(new File(dir, "generated"));
		in.addSoundLoadBase(new File(dir, "sounds"));
		in.addSoundLoadBase(new File(getInstallDir(), "music"));
	}
	
	public void afterLoad() {
		for (ExpansionMusic en : Loadable.all(ExpansionMusic.class)) {
			if (en.expansion.equals(name)) {
				if (en.forCombat) {
					AGame.COMBAT_MUSIC.add(en.name);
				}
				if (en.forEditor) {
					AGame.EDITOR_MUSIC.add(en.name);
				}
				if (en.forStrategic) {
					AGame.STRATEGIC_MUSIC.add(en.name);
				}
			}
		}
	}
}
