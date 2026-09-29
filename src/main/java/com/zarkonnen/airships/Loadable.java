package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/** NB also requires a constructor taking JSONObject. */
public abstract class Loadable implements Comparable<Loadable> {
	public final String name;
	public final int sort;
	public Expansion sourceExpansion;
	public Mod sourceMod;
	
	public static final Class[] LOADABLES = {
		LoadingQuote.class,
		FrequencySetting.class,
		CrewExperienceLevel.class,
		Shouts.class,
		VariantType.class,
		GenericFragment.class,
		CityName.class,
		Tincture.class,
		PaintType.class,
		GameSetting.class,
		MapSize.class,
		EmergentMapFeature.class,
		Bonus.class,
		DifficultyLevel.class,
		EmpireStat.class,
		StrategicEra.class,
		TakeoverMethod.class,
		CityUpgradeType.class,
		DiplomacyPersonality.class,
		SpritesheetBundle.class,
		ParticlePictureType.class,
		ArmsLayout.class,
		Cursor.class,
		CloudType.class,
		Backdrop.class,
		BackdropType.class,
		BackgroundFloatieType.class,
		BirdType.class,
		CombatBackgroundFlavor.class,
		TerrainFeatureType.class,
		ParticleType.class,
		WeatherEffect.class,
		TimeOfDay.class,
		Season.class,
		Moon.class,
		LandBlockType.class,
		LandscapeType.class,
		BodyPlan.class,
		AnimationBundle.class,
		AnimationAppearance.class,
		CrewType.class,
		DecalCategory.class,
		DecalType.class,
		ArmourType.class,
		ModuleCategory.class,
		ModuleType.class,
		Substitution.class,
		Tech.class,
		Charge.class,
		HeraldicStyle.class,
		ConstructionName.class,
		ConstructionStrategy.class,
		PortraitMessageType.class,
		MonsterNestType.class,
		MusicAffinity.class,
		PlagueLevel.class,
		EraModifier.class,
		MiscCombatSound.class,
		GUISetting.class,
		Splinter.class,
		MonsterSetting.class,
		SeaLevelSetting.class,
		TechSpeedSetting.class,
		Edict.class,
		HeroType.class,
		IncidentType.class,
		MedalPart.class,
		MedalMetal.class,
		MedalEffect.class,
		MedalRibbonLayout.class,
		ExpansionMusic.class
	};

	public Loadable(String name) {
		this.name = name;
		sort = 0;
	}
	
	public Loadable(String name, int sort) {
		this.name = name;
		this.sort = sort;
	}
	
	/** Override this to allow the loadable to release any resources it may be holding. */
	public void close() {}
	
	@Override
	public int compareTo(Loadable l2) {
		if (sort == l2.sort) {
			return name.compareTo(l2.name);
		} else {
			return sort - l2.sort;
		}
	}
	
	public static HashMap<Class, HashMap<String, Object>> map = new HashMap<Class, HashMap<String, Object>>();
	public static HashMap<Class, ArrayList> alls;
	public static HashMap<Class, ArrayList<String>> errorLogs = new HashMap<Class, ArrayList<String>>();
	private static HashMap<Class, HashMap<String, JSONObject>> baseEntries = new HashMap<Class, HashMap<String, JSONObject>>();
	
	public static class NotFoundException extends RuntimeException {
		public final Class clazz;
		public final String name;

		public NotFoundException(Class clazz, String name, String message) {
			super(message);
			this.clazz = clazz;
			this.name = name;
		}
	}
	
	public static <T extends Loadable> T ofName(Class<T> clazz, String name) {
		if (!map.containsKey(clazz)) {
			throw new NotFoundException(clazz, name, _t("no_loadables", clazz.getSimpleName()));
		}
		if (!map.get(clazz).containsKey(name)) {
			throw new NotFoundException(clazz, name, _t("no_loadable_of_name", clazz.getSimpleName(), name));
		}
		return (T) map.get(clazz).get(name);
	}
	
	public static <T extends Loadable> boolean hasOfName(Class<T> clazz, String name) {
		return map.containsKey(clazz) && map.get(clazz).containsKey(name);
	}
	
	public static <T extends Loadable> ArrayList<T> all(Class<T> clazz) {
		if (alls == null) {
			if (!map.containsKey(clazz)) {
				return new ArrayList();
			} else {
				ArrayList l = new ArrayList(map.get(clazz).values());
				Collections.sort(l);
				return l;
			}
		}
		if (!alls.containsKey(clazz)) {
			if (!map.containsKey(clazz)) {
				alls.put(clazz, new ArrayList());
			} else {
				ArrayList l = new ArrayList(map.get(clazz).values());
				Collections.sort(l);
				alls.put(clazz, l);
			}
		}
		return new ArrayList<T>((ArrayList<T>) alls.get(clazz));
	}
	
	public static ArrayList<String> getErrors(Class clazz) {
		return errorLogs.containsKey(clazz) ? new ArrayList<String>() : errorLogs.get(clazz);
	}
	
	public static void remove(MonsterNestType mnt) {
		map.get(mnt.getClass()).remove(mnt.name);
	}
	
	public static boolean load() {
		try {
			//System.out.println("loadable.load");
			for (HashMap<String, Object> os : map.values()) {
				for (Object o : os.values()) {
					((Loadable) o).close();
				}
			}
			map.clear();
			alls = null;
			errorLogs.clear();
			baseEntries.clear();
			ArrayList<Mod> mods = Mod.getEnabledMods();
			/*System.out.println("Mods:");
			for (Mod m : mods) {
				System.out.println(m.id);
			}*/
			for (Class clazz : LOADABLES) {
				//System.err.println("Loading " + clazz.getSimpleName());
				if ((clazz == HeroType.class) && !EHeroes.it.enabled) {
					continue;
				}
				if (AirshipGame.instance != null) {
					AirshipGame.instance.tickClients();
				}
				HashMap<String, LoadFailure> baseFailures = loadBase(clazz);
				if (!errorLogs.get(clazz).isEmpty()) {
					for (Mod mod : mods) {
						mod.loadFailed = true;
					}
					return false;
				}
				for (Expansion ex : Expansion.enableds()) {
					//System.err.println(ex.name);
					LoadResult p = loadDir(clazz, new File(ex.getDataDir(), clazz.getSimpleName()), baseEntries.get(clazz));
					if (!p.log.isEmpty()) {
						System.err.println("Loading expansion " + ex.name + " failed!");
						for (String l : p.log) {
							System.err.println(l);
						}
						return false;
					} else {
						for (Object o : p.loaded.values()) {
							((Loadable) o).sourceExpansion = ex;
						}
						for (String k : p.removed) {
							if (map.get(clazz).containsKey(k)) {
								((Loadable) map.get(clazz).get(k)).close();
								map.get(clazz).remove(k);
							}
						}
						for (String k : p.loaded.keySet()) {
							if (map.get(clazz).containsKey(k)) {
								((Loadable) map.get(clazz).get(k)).close();
								map.get(clazz).remove(k);
							}
						}
						map.get(clazz).putAll(p.loaded);
						baseFailures.putAll(p.failures);
					}
				}
				for (Mod mod : mods) {
					//System.err.println("Loading " + clazz.getSimpleName() + " " + mod.id);
					// No longer duplicating this hashmap means we can stack patches on top of one another.
					LoadResult p = loadDir(clazz, new File(mod.dir, clazz.getSimpleName()), /*new HashMap<String, JSONObject>(*/baseEntries.get(clazz)/*)*/);
					if (!p.log.isEmpty()) {
						mod.loadLog = join(p.log);
						mod.loadFailed = true;
						System.err.println("mod load failed: " + mod.loadLog);
						return false;
					} else {
						for (Object o : p.loaded.values()) {
							((Loadable) o).sourceMod = mod;
						}
						for (String k : p.removed) {
							baseFailures.remove(k);
							if (map.get(clazz).containsKey(k)) {
								((Loadable) map.get(clazz).get(k)).close();
								map.get(clazz).remove(k);
							}
						}
						for (String k : p.loaded.keySet()) {
							baseFailures.remove(k);
							if (map.get(clazz).containsKey(k)) {
								((Loadable) map.get(clazz).get(k)).close();
								map.get(clazz).remove(k);
							}
						}
						if (!p.failures.isEmpty()) {
							mod.loadFailed = true;
							mod.setPermanentlyEnabled(false);
							System.err.println(clazz.getSimpleName() + " mod " + mod.id + " load failed");
							for (LoadFailure lf : p.failures.values()) {
								System.err.println(lf);
								lf.e.printStackTrace();
								mod.loadLog += "\n" + lf.e.getMessage();
							}
							return false;
						}
						map.get(clazz).putAll(p.loaded);
						if (clazz == ModuleType.class) {
							ArrayList<String> refProblemLog = ModuleType.checkForRefProblems();
							if (!refProblemLog.isEmpty()) {
								mod.loadLog = join(refProblemLog);
								mod.loadFailed = true;
								System.err.println("mod load failed: " + mod.loadLog);
								return false;
							}
						}
					}
				}
				
				if (!baseFailures.isEmpty()) {
					System.err.println(clazz.getSimpleName() + " load failed due to unresolved errors when loading base data");
					for (LoadFailure lf : baseFailures.values()) {
						System.err.println(lf);
						lf.e.printStackTrace();
					}
					for (Mod mod : mods) {
						mod.loadFailed = true;
						mod.setPermanentlyEnabled(false);
						mod.loadLog = clazz.getSimpleName() + " load failed due to unresolved errors when loading base data.\nCheck log.txt for details.";
						for (LoadFailure lf : baseFailures.values()) {
							mod.loadLog += "\n" + lf.e.getMessage();
						}
					}
					return false;
				}
			}
			alls = new HashMap<Class, ArrayList>();
			//System.out.println("post");
			CrewType.postLoad();
			ArmourPlate.updateAppearances();
			CoatOfArms.updateAppearances();
			Charge.postLoad();
			TimeOfDay.postLoad();
			Resource.updateAppearances();
			ModuleType.postLoad();
			DecalType.postLoad();
			DecalCategory.postLoad();
			ArmourType.postLoad();
			Challenge.updateTypes();
			SettingsScreen.postLoad();
			Bonus.postLoad2();
			LandBlockType.postLoad();
			HeroType.postLoad();
			CityUpgradeType.postLoad();
			if (!Tech.postLoad()) {
				System.err.println("tech postload failed");
				return false;
			}
			MonsterNestType.postLoad();
			for (Expansion ex : Expansion.enableds()) {
				ex.afterLoad();
			}
			if (AirshipGame.instance != null) {
				AirshipGame.instance.currentGUIScale.activate();
			}
			//System.out.println("loaded");
			/*for (Class clazz : LOADABLES) {
				boolean all = true;
				boolean any = false;
				for (Object o : all(clazz)) {
					Loadable l = (Loadable) o;
					if (l.sourceMod == null) {
						all = false;
					} else {
						any = true;
					}
				}
				if (all) {
					System.out.println(clazz.getSimpleName() + " all overridden");
				} else if (any) {
					System.out.println(clazz.getSimpleName() + " some overridden");
					for (Object o : all(clazz)) {
						Loadable l = (Loadable) o;
						System.out.println(l.name + ": " + (l.sourceMod != null));
					}
				}
			}*/
			return true;
		} catch (Exception e) {
			System.err.println("fatal mod load crash");
			e.printStackTrace();
			AirshipGame.instance.reportError("Mod load crash", e, null, false, true);
			for (Mod m : Mod.mods) {
				m.setPermanentlyEnabled(false);
			}
			AirshipGame.instance.showError(_t("fatal_mod_load_error"));
			return false;
		}
	}
	
	public static String join(ArrayList<String> l) {
		StringBuilder sb = new StringBuilder();
		for (String s : l) {
			sb.append(s).append("\n");
		}
		return sb.toString();
	}
	
	public static <T extends Loadable> HashMap<String, LoadFailure> loadBase(Class<T> clazz) {
		File dir = new File(new File(AGame.getStaticGameDirectory(), "data"), clazz.getSimpleName());
		HashMap<String, JSONObject> prevEntries = new HashMap<String, JSONObject>();
		baseEntries.put(clazz, prevEntries);
		LoadResult lr = loadDir(clazz, dir, prevEntries);
		errorLogs.put(clazz, lr.log);
		map.put(clazz, lr.loaded);
		for (String k : lr.removed) {
			lr.loaded.remove(k);
		}
		return lr.failures;
	}
	
	public static <T extends Loadable> LoadResult loadDir(Class<T> clazz, File dir, HashMap<String, JSONObject> prevEntries) {
		HashMap<String, Object> m = new HashMap<String, Object>();
		ArrayList<String> l = new ArrayList<String>();
		HashSet<String> rm = new HashSet<String>();
		HashMap<String, LoadFailure> failures = new HashMap<String, LoadFailure>();
		File[] fs = dir.listFiles();
		if (fs != null) {
			List<File> fsL = Arrays.asList(fs);
			Collections.sort(fsL);
			for (File f : fsL) {
				if (!f.getName().startsWith(".") && f.getName().endsWith(".json")) {
					LoadResult p = loadFile(clazz, f, prevEntries);
					l.addAll(p.log);
					for (String k : p.loaded.keySet()) {
						if (m.containsKey(k)) {
							System.out.println("Duplicate item " + k + " in " + f.getAbsolutePath());
						}
					}
					m.putAll(p.loaded);
					rm.addAll(p.removed);
					failures.putAll(p.failures);
				}
			}
		}
		return new LoadResult(l, m, rm, failures);
	}
	
	public static <T extends Loadable> LoadResult loadFile(Class<T> clazz, File f, HashMap<String, JSONObject> prevEntries) {
		Constructor<T> cons = null;
		try {
			cons = clazz.getDeclaredConstructor(JSONObject.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		HashMap<String, Object> map = new HashMap<String, Object>();
		ArrayList<String> loadErrors = new ArrayList<String>();
		HashSet<String> removed = new HashSet<String>();
		HashMap<String, LoadFailure> failures = new HashMap<String, LoadFailure>();
		try {
			JSONArray a = new JSONArray(FileUtils.readFileToString(f, "UTF-8"));
			for (int i = 0; i < a.length(); i++) {
				JSONObject o = a.getJSONObject(i);
				if (o.optBoolean("remove")) {
					removed.add(o.getString("name"));
				} else {
					boolean patch = o.optBoolean("patch", false);
					if (patch || o.has("deriveFrom")) {
						String deriveFromName = patch ? o.getString("name") : o.getString("deriveFrom");
						if (!prevEntries.containsKey(deriveFromName)) {
							if (patch) {
								throw new RuntimeException("Cannot patch " + clazz.getSimpleName() + " " + deriveFromName + " because it does not exist.");
							} else {
								throw new RuntimeException("Cannot derive " + clazz.getSimpleName() + " " + o.optString("name", "?") + " from " + o.getString("deriveFrom") + " because it does not exist.");
							}
						}
						JSONObject deriveFrom = prevEntries.get(deriveFromName);
						for (Object ko : deriveFrom.keySet()) {
							String k = (String) ko;
							if (!patch && (clazz == ModuleType.class || clazz == ArmourType.class || clazz == DecalType.class) && (k.equals("variants") || k.equals("verticallyFlippedVersion") || k.equals("flippedFrom") || k.equals("variantType") || k.equals("flippedVersion"))) {
								continue;
							}
							if (!o.has(k)) {
								o.put(k, deriveFrom.get(k));
							}
						}
					}
					T t = null;
					try {
						t = cons.newInstance(o);
					} catch (Exception e) {
						/*System.err.println("Failed to instantiate:");
						System.err.println(o.toString(4));
						throw e;*/
						//e.fillInStackTrace();
						// also need to store object and file
						//e.printStackTrace();
						if (o.has("name")) {
							//e.fillInStackTrace(); // Do we need this?
							//System.out.println("err " + o.getString("name"));
							failures.put(o.getString("name"), new LoadFailure(o, f, e));
						}
						continue;
					}
					prevEntries.put(t.name, o);
					if (map.containsKey(t.name)) {
						System.out.println("Duplicate entry " + t.name + " in " + f.getAbsolutePath());
					}
					map.put(t.name, t);
				}
			}
		} catch (Exception e) {
			loadErrors.add(f.getName() + ": " + e.getMessage());
			System.err.println(f.getAbsolutePath());
			e.printStackTrace();
		}
		return new LoadResult(loadErrors, map, removed, failures);
	}
	
	public static class LoadFailure {
		public JSONObject o;
		public File f;
		public Throwable e;
		
		public LoadFailure(JSONObject o, File f, Throwable e) {
			this.o = o;
			this.f = f;
			this.e = e.getCause() != null ? e.getCause() : e;
		}
		
		@Override
		public String toString() {
			return f.getAbsolutePath() + "\n" + o.toString(4) + "\n" + e.getMessage();
		}
	}
	
	public static class LoadResult {
		public ArrayList<String> log;
		public HashMap<String, Object> loaded;
		public HashSet<String> removed;
		public HashMap<String, LoadFailure> failures;

		public LoadResult(ArrayList<String> log, HashMap<String, Object> loaded, HashSet<String> removed, HashMap<String, LoadFailure> errors) {
			this.log = log;
			this.loaded = loaded;
			this.removed = removed;
			this.failures = errors;
		}
	}
	
	public static Img img(JSONObject o) {
		return new Img(o.getString("src"), o.getInt("x"), o.getInt("y"), o.getInt("w"), o.getInt("h"), o.optBoolean("flipped", false));
	}
	
	public static long getDataChecksum() {
		/*try {
			FileInputStream is = new FileInputStream(new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "LoadingQuote"), "quotes.json"));
			int in = 0;
			while ((in = is.read()) != -1) {
				System.out.println(in);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		long t = System.currentTimeMillis();
		long sum = 0;
		for (Class clazz : LOADABLES) {
			File dir = new File(new File(AGame.getStaticGameDirectory(), "data"), clazz.getSimpleName());
			if (dir.exists()) {
				try {
					sum += Mod.checksum(dir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			sum += Mod.checksum(new File(new File(AGame.getStaticGameDirectory(), "data"), "lang"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (sum == 0) {
			sum = 1;
		}
		System.out.println("data checksum took " + (System.currentTimeMillis() - t) + " ms and is " + sum);
		return sum;
	}
}
