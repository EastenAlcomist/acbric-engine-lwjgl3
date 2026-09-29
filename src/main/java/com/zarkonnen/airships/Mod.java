package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.StrategicLobbyScreen.GAME_DATA_CHUNK_MAX_SIZE;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import com.zarkonnen.catengine.util.Utils;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.newdawn.slick.Image;

public class Mod {
	public static final ArrayList<Mod> mods = new ArrayList<Mod>();
	private static ArrayList<String> overriddenEnabledModIds;
	public static HashMap<String, Long> modChecksumVersions;
	public static HashSet<String> cachedModChecksumChecked = new HashSet<String>();
	
	public final boolean isLocal;
	public final File dir;
	public final HashMap<Locale, String> name = new HashMap<Locale, String>();
	public final HashMap<Locale, String> description = new HashMap<Locale, String>();
	public final ArrayList<String> tags = new ArrayList<String>();
	public String id;
	public Image logo;
	public boolean loadInfoFailed = false;
	public boolean loadFailed = false;
	public String infoLog;
	public String loadLog;
	public String buildLog;
	public boolean buildFailed = false;
	public Mod preemptedBy = null; // True if another mod with the same ID already exists.
	
	public boolean isPermanentlyEnabled() {
		return AirshipGame.PREFS.getBoolean("mod_enabled_" + id.substring(0, StrictMath.min(id.length(), 200)), false);
	}
	
	public void setPermanentlyEnabled(boolean enabled) {
		AirshipGame.PREFS.putBoolean("mod_enabled_" + id.substring(0, StrictMath.min(id.length(), 200)), enabled);
	}
	
	public boolean isNew() {
		return !AirshipGame.PREFS.hasKey("mod_known_" + id.substring(0, StrictMath.min(id.length(), 200)));
	}
	
	public void setNotNew() {
		AirshipGame.PREFS.putBoolean("mod_known_" + id.substring(0, StrictMath.min(id.length(), 200)), true);
	}
	
	public boolean isCurrentlyEnabled() {
		if (loadInfoFailed || loadFailed || preemptedBy != null) { return false; }
		if (overriddenEnabledModIds != null) {
			return overriddenEnabledModIds.contains(id);
		}
		return isPermanentlyEnabled();
	}
	
	public boolean isAvailable() {
		return !loadInfoFailed && !loadFailed && preemptedBy == null;
	}
	
	public String getWarnings() {
		if (!isAvailable()) { return ""; }
		StringBuilder sb = new StringBuilder();
		for (TimeOfDay tod : Loadable.all(TimeOfDay.class)) {
			if (tod.sourceMod != this) { continue; }
			sb.append("Warning: TimeOfDay ").append(tod.name).append(" should specify an appearancePostfix for simple graphics mode. One of ");
			for (String s : TimeOfDay.ORIGINAL_APPEARANCE_POSTFIXES) {
				if (!s.equals(TimeOfDay.ORIGINAL_APPEARANCE_POSTFIXES.get(0))) {
					sb.append(", ");
				}
				sb.append(s);
			}
			sb.append("\n");
		}
		HashSet<SpritesheetBundle> ssbsToExamine = new HashSet<SpritesheetBundle>();
		for (ModuleType m : Loadable.all(ModuleType.class)) {
			if (m.sourceMod != this) { continue; }
			ssbsToExamine.add(m.getApp(BonusSet.empty(), 1).spritesheetBundle);
			for (BonusSet b : m.getAppBonuses()) {
				ssbsToExamine.add(m.getApp(b, 1).spritesheetBundle);
			}
			for (BonusSet b : m.getDepletedResourceAppBonuses()) {
				for (Appearance da : m.getDepletedResourceApps(b)) {
					ssbsToExamine.add(da.spritesheetBundle);
				}
			}
			if (m.getExternalApps(BonusSet.empty(), false, false) != null) {
				for (ExternalApp ea : m.getExternalApps(BonusSet.empty(), false, false)) {
					ssbsToExamine.add(ea.app.spritesheetBundle);
				}
			}
			for (BonusSet b : m.getExternalAppBonuses(false, false)) {
				if (m.getExternalApps(b, false, false) != null) {
					for (ExternalApp ea : m.getExternalApps(b, false, false)) {
						ssbsToExamine.add(ea.app.spritesheetBundle);
					}
				}
			}
			if (m.getExternalApps(BonusSet.empty(), true, false) != null) {
				for (ExternalApp ea : m.getExternalApps(BonusSet.empty(), true, false)) {
					ssbsToExamine.add(ea.app.spritesheetBundle);
				}
			}
			for (BonusSet b : m.getExternalAppBonuses(true, false)) {
				if (m.getExternalApps(b, true, false) != null) {
					for (ExternalApp ea : m.getExternalApps(b, true, false)) {
						ssbsToExamine.add(ea.app.spritesheetBundle);
					}
				}
			}
			if (m.getExternalApps(BonusSet.empty(), true, true) != null) {
				for (ExternalApp ea : m.getExternalApps(BonusSet.empty(), true, true)) {
					ssbsToExamine.add(ea.app.spritesheetBundle);
				}
			}
			for (BonusSet b : m.getExternalAppBonuses(true, true)) {
				if (m.getExternalApps(b, true, true) != null) {
					for (ExternalApp ea : m.getExternalApps(b, true, true)) {
						ssbsToExamine.add(ea.app.spritesheetBundle);
					}
				}
			}
		}
		for (ArmourType at : Loadable.all(ArmourType.class)) {
			if (at.sourceMod != this) { continue; }
			if (at.damagedApps.get(BonusSet.empty()) != null && !at.damagedApps.get(BonusSet.empty()).isEmpty()) {
				ssbsToExamine.add(at.damagedApps.get(BonusSet.empty()).get(0).spritesheetBundle);
			}
			for (BonusSet b : at.getAppBonuses()) {
				if (at.damagedApps.get(b) != null && !at.damagedApps.get(b).isEmpty()) {
					ssbsToExamine.add(at.damagedApps.get(b).get(0).spritesheetBundle);
				}
			}
		}
		for (SpritesheetBundle ssb : ssbsToExamine) {
			if (ssb.getDamagedVersion() == null) {
				sb.append("Warning: SpriteSheetBundle ").append(ssb.name).append(" has no damaged sheet.\n");
			}
			if (ssb.getFragmentsSheet()== null) {
				sb.append("Warning: SpriteSheetBundle ").append(ssb.name).append(" has no fragments sheet.\n");
			}
		}
		return sb.toString();
	}

	private Mod(File dir, boolean isLocal) {
		this.dir = dir;
		this.isLocal = isLocal;
		id = "_" + dir.getName() + "_" + AGame.ANIM_R.nextInt();
		SpriteUtils.ensureTexFilesInGeneratedDirectory(new File(dir, "images"));
	}
	
	public String getName() {
		return
				name.containsKey(Lang.currentLocale)
				? name.get(Lang.currentLocale)
				: name.containsKey(Locale.ENGLISH)
				? name.get(Locale.ENGLISH)
				: dir.getName();
	}
	
	public static boolean isLocalPresentWithName(String name) {
		for (Mod m : mods) {
			if (m.isLocal && m.getIdeallyEnglishName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public String getIdeallyEnglishDescription() {
		if (description.containsKey(Locale.ENGLISH)) {
			return description.get(Locale.ENGLISH);
		}
		if (!description.keySet().isEmpty()) {
			return description.get(description.keySet().iterator().next());
		}
		return "(No description provided.)";
	}
	
	public String getIdeallyEnglishName() {
		if (name.containsKey(Locale.ENGLISH)) {
			return name.get(Locale.ENGLISH);
		}
		return id;
	}
	
	private void loadInfo() {
		StringBuilder log = new StringBuilder();
		log.append("Loading mod from ").append(dir.getAbsolutePath()).append("\n");
		try {
			JSONObject info = new JSONObject(FileUtils.readFileToString(new File(dir, "info.json"), "UTF-8"));
			id = info.getString("id");
			if (logo == null) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(new File(dir, "logo.png"));
					logo = new Image(fis, id + "-logo", false);
				} catch (Exception e) {
					log.append("Unable to load logo: ").append(e.getMessage()).append("\n");
					logo = null;
				} finally {
					try { fis.close(); } catch (Exception e) {}
				}
			}
			JSONObject nameO = info.getJSONObject("name");
			for (String k : (Set<String>) nameO.keySet()) {
				try {
					Locale l = Locale.forLanguageTag(k);
					name.put(l, nameO.getString(k));
				} catch (Exception e) {
					log.append("Unknown locale: ").append(k).append("\n");
				}
			}
			JSONObject descO = info.getJSONObject("description");
			for (String k : (Set<String>) descO.keySet()) {
				try {
					Locale l = Locale.forLanguageTag(k);
					description.put(l, descO.getString(k));
				} catch (Exception e) {
					log.append("Unknown locale: ").append(k).append("\n");
				}
			}
			if (info.has("tags")) {
				JSONArray ta = info.getJSONArray("tags");
				for (int i = 0; i < ta.length(); i++) {
					tags.add(ta.getString(i));
				}
			}
		} catch (Exception e) {
			log.append("Unable to read info.json: ").append(e.getMessage()).append("\n");
			infoLog = log.toString();
			loadInfoFailed = true;
			return;
		}
		infoLog = log.toString();
		loadInfoFailed = false;
	}
	
	private void addLoadBases(Lwjgl3Engine.MyInput in) {
		in.addLoadBase(new File(dir, "images"));
		in.addLoadBase(new File(dir, "generated"));
		in.addSoundLoadBase(new File(dir, "sounds"));
	}
	
	public static void refreshMods() {
		ArrayList<Mod> local = new ArrayList<Mod>();
		new File(AGame.getGameDirectory(), "mods").mkdirs();
		File[] fss = new File(AGame.getGameDirectory(), "mods").listFiles();
		if (fss != null) {
			List<File> fs = Arrays.asList(fss);
			Collections.sort(fs);
			lp: for (File f : fs) {
				if (f.isDirectory() && new File(f, "info.json").exists()) {
					for (Mod m : mods) {
						if (m.dir.equals(f)) {
							local.add(m);
							m.preemptedBy = null;
							m.loadInfo();
							continue lp;
						}
					}
					Mod m = new Mod(f, true);
					local.add(m);
					m.loadInfo();
				}
			}
		}
		
		ArrayList<Mod> steam = new ArrayList<Mod>();
		new File(new File(AGame.getGameDirectory(), "steam"), "mods").mkdirs();
		fss = new File(new File(AGame.getGameDirectory(), "steam"), "mods").listFiles();
		if (fss != null) {
			List<File> fs = Arrays.asList(fss);
			Collections.sort(fs);
			lp: for (File outerF : fs) {
				if (outerF.isDirectory()) {
					File[] innerFs = outerF.listFiles();
					if (innerFs != null) {
						List<File> innerFsA = Arrays.asList(innerFs);
						Collections.sort(innerFsA);
						for (File f : innerFsA) {
							if (f.isDirectory() && new File(f, "info.json").exists()) {
								for (Mod m : mods) {
									if (m.dir.equals(f)) {
										steam.add(m);
										m.preemptedBy = null;
										m.loadInfo();
										continue lp;
									}
								}
								Mod m = new Mod(f, false);
								steam.add(m);
								m.loadInfo();
							}
						}
					}
				}
			}
		}
		
		ArrayList<Mod> overrides = new ArrayList<Mod>();
		if (modChecksumVersions != null) {
			ArrayList<String> idsToOverride = new ArrayList<String>(modChecksumVersions.keySet());
			Collections.sort(idsToOverride);
			for (String idToOverride : idsToOverride) {
				long checksumToUse = modChecksumVersions.get(idToOverride);
				Mod m = new Mod(getCachedModF(idToOverride, checksumToUse), true);
				m.loadInfo();
				overrides.add(m);
			}
		}
		
		mods.clear();
		mods.addAll(overrides);
		mods.addAll(local);
		mods.addAll(steam);
		Collections.sort(mods, modSorter);
		
		for (int i = 0; i < mods.size(); i++) {
			Mod m = mods.get(i);
			for (int j = 0; j < i; j++) {
				// If there is an earlier mod m2 (so an override or a local) we set this mod to be preempted by it.
				Mod m2 = mods.get(j);
				if (m.id.equals(m2.id)) {
					m.preemptedBy = m2;
					break;
				}
			}
		}
	}
	
	public static File getCachedModF(String id, long checksum) {
		File f = getCachedModF2(id, checksum);
		if (f.exists() && !cachedModChecksumChecked.contains(id + "//" + checksum)) {
			boolean checksumCorrect = false;
			File parentDir = new File(new File(AGame.getGameDirectory(), "cached_mods"), AGame.makeFileSafe(id));
			try {
				checksumCorrect = getChecksum(f) == checksum; 
			} catch (IOException e) {}
			if (checksumCorrect) {
				cachedModChecksumChecked.add(id + "//" + checksum);
			} else {
				System.err.println("Checksum of " + f.getAbsolutePath() + " does not match, deleting.");
				try {
					FileUtils.deleteDirectory(parentDir);
				} catch (IOException e) {
					throw new RuntimeException("Unable to delete damaged cache files at " + parentDir.getAbsolutePath() + ".", e);
				}
			}
		}
		return f;
	}
	
	public static File getCachedModF2(String id, long checksum) {
		File f = new File(new File(new File(AGame.getGameDirectory(), "cached_mods"), AGame.makeFileSafe(id)), "" + checksum);
		File[] fs = f.listFiles();
		if (fs != null) {
			for (int i = 0; i < fs.length; i++) {
				if (fs[i].isDirectory()) { return fs[i]; }
			}
		}
		return f;
	}
	
	public static void cacheModF(String id, long checksum, ArrayList<String> chunks) throws IOException {
		File f = new File(new File(new File(AGame.getGameDirectory(), "cached_mods"), AGame.makeFileSafe(id)), "" + checksum);
		if (f.exists()) { return; }
		StringBuilder sb = new StringBuilder();
		for (String c : chunks) { sb.append(c); }
		ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decodeBase64(sb.toString()));
		sb = null;
		ZipUtils.unzip(bis, f);
		cachedModChecksumChecked.remove(id + "//" + checksum);
		if (!getCachedModF(id, checksum).exists()) {
			throw new RuntimeException("Crossloaded mod " + id + " failed to verify.");
		} else {
			System.out.println("xload " + id + " verification successful");
		}
	}
	
	private static final Comparator<Mod> modSorter = new Comparator<Mod>() {
		@Override
		public int compare(Mod t, Mod t1) {
			String id = t.id == null ? t.dir.getName() : t.id;
			String id1 = t1.id == null ? t1.dir.getName() : t1.id;
			return id.toLowerCase(Locale.ENGLISH).compareTo(id1.toLowerCase(Locale.ENGLISH));
		}
	};
	
	public static boolean isCached(String id, long checksum) {
		return getCachedModF(id, checksum).exists();
	}
	
	public static boolean isAvailable(String id, long checksum) {
		for (Mod m : mods) {
			if (m.id.equals(id)) {
				return m.isAvailable() && m.getCachedChecksum() == checksum;
			}
		}
		return false;
	}
	
	public static Mod getById(String id) {
		for (Mod m : mods) {
			if (m.id.equals(id)) { return m; }
		}
		return null;
	}
	
	public static ArrayList<Mod> getNewMods() {
		ArrayList<Mod> l = new ArrayList<Mod>();
		for (Mod m : mods) {
			if (m.isNew())
			{
				l.add(m);
			}
		}
		return l;
	}
	
	public static ArrayList<Mod> getEnabledMods() {
		ArrayList<Mod> l = new ArrayList<Mod>();
		for (Mod m : mods) {
			if (m.isCurrentlyEnabled())
			{
				l.add(m);
			}
		}
		return l;
	}
	
	public static ArrayList<String> getEnabledModIDs() {
		ArrayList<String> l = new ArrayList<String>();
		for (Mod m : mods) {
			if (m.isCurrentlyEnabled())
			{
				l.add(m.id);
			}
		}
		return l;
	}
	
	public static ArrayList<Mod> getAvailableMods() {
		ArrayList<Mod> l = new ArrayList<Mod>();
		for (Mod m : mods) {
			if (m.isAvailable()) {
				l.add(m);
			}
		}
		return l;
	}
	
	public static void resetLoadBases(Lwjgl3Engine.MyInput in) {
		in.clearLoadBases();
		in.clearSoundLoadBases();
		in.addLoadBase(new File(new File(AGame.getStaticGameDirectory(), "data"), "images"));
		in.addSoundLoadBase(new File(new File(AGame.getStaticGameDirectory(), "data"), "sounds"));
		for (Expansion ex : Expansion.enableds()) {
			ex.addLoadBases(in);
		}
	}
	
	private static boolean doLoadMods(Input in) {
		//System.out.println("doLoadMods");
		Lwjgl3Engine.MyInput myIn = AirshipGame.getMyInput(in);
		resetLoadBases(myIn);
		for (Mod m : getEnabledMods()) {
			m.addLoadBases(myIn);
		}

		boolean success = false;
		while (!getEnabledMods().isEmpty() && !(success = Loadable.load())) {
			System.out.println(".");
			resetLoadBases(myIn);
			for (Mod m : getEnabledMods()) {
				m.addLoadBases(myIn);
			}
		}
		if (!success) {
			resetLoadBases(myIn);
			for (Mod m : getEnabledMods()) {
				m.addLoadBases(myIn);
			}
			success = Loadable.load();
		}
		//System.out.println("success: " + success);
		return success;
	}
	
	public static class LoadProgress {
		public final boolean complete;
		public final boolean failed;
		public final int progress;
		public final int totalSteps;
		public final String desc;

		public LoadProgress(boolean complete, boolean failed, int progress, int totalSteps, String desc) {
			this.complete = complete;
			this.failed = failed;
			this.progress = progress;
			this.totalSteps = totalSteps;
			this.desc = desc;
		}
		
		@Override
		public String toString() {
			return (complete ? "C" : "") + (failed ? "F": "") + " " + progress + "/" + totalSteps + " " + desc;
		}
	}
	
	public static class LoadProcess {
		private boolean inited = false;
		private boolean initialLoadComplete = false;
		private boolean toGenerateFound = false;
		private final ArrayList<Mod> toGenerate = new ArrayList<Mod>();
		private int generateIndex = 0;
		private boolean needReload = false;
		private boolean graphicsPreloadStarted = false;
		public boolean disableAllMods = false;
		public boolean preloadGraphics = true;
		
		public LoadProcess(boolean preloadGraphics) {
			this.preloadGraphics = preloadGraphics;
		}
		
		public LoadProgress doLoad(Input in) {
			PerfStats.didModLoading = true;
			if (!inited) {
				inited = true;
				return new LoadProgress(false, false, 0, 100, _t("Loading_game_data_"));
			}
			if (!initialLoadComplete) {
				initialLoadComplete = true;
				refreshMods();
				
				if (disableAllMods) {
					for (Mod mod : Mod.mods) {
						mod.setPermanentlyEnabled(false);
					}
				}

				for (Mod m : mods) {
					m.loadFailed = false;
				}

				if (doLoadMods(in)) {
					Lang.reloadBundle();
					ShipHelperWidget.clearAllCaches();
					MonsterHelperWidget.clearAllCaches();
					Appearance.reloadSpritesheets();
					return new LoadProgress(false, false, 1, 2 + mods.size(), _t("Checking_mods"));
				} else {
					AirshipGame.instance.doLowMemoryCheck();
					return new LoadProgress(true, true, 1, 1, _t("Loading_game_data_failed"));
				}
			}
			if (!toGenerateFound) {
				toGenerateFound = true;
				for (Mod m : getEnabledMods()) {
					try {
						if (m.needsGenerateDerivedData()) {
							toGenerate.add(m);
						}
					} catch (IOException e) {
						e.printStackTrace();
						m.loadFailed = true;
						needReload = true;
					}
				}
				if (!toGenerate.isEmpty()) {
					needReload = true;
					return new LoadProgress(false, false, 2, 3 + toGenerate.size(), _t("Generating_graphics_for_x", toGenerate.get(0).id));
				}
				if (needReload) {
					return new LoadProgress(false, false, 2, 3, _t("Loading_game_data_"));
				}
				/*Lang.reloadBundle();
				ShipHelperWidget.clearAllCaches();
				MonsterHelperWidget.clearAllCaches();
				Appearance.reloadSpritesheets();
				AirshipGame.instance.doLowMemoryCheck();
				return new LoadProgress(true, false, 2, 2, _t("Loading_game_data_complete"));*/
			}
			if (generateIndex < toGenerate.size()) {
				toGenerate.get(generateIndex).generateDerivedData(in);
				generateIndex++;
				if (generateIndex >= toGenerate.size()) {
					return new LoadProgress(false, false, 2 + toGenerate.size(), 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, _t("Loading_game_data_"));
				} else {
					return new LoadProgress(false, false, 2 + generateIndex, 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, _t("Generating_graphics_for_x", toGenerate.get(generateIndex).id));
				}
			}
			if (needReload) {
				needReload = false;
				if (doLoadMods(in)) {
					return new LoadProgress(false, false, 3 + toGenerate.size(), 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, _t("Loading_game_data_"));
				} else {
					AirshipGame.instance.doLowMemoryCheck();
					return new LoadProgress(true, true, 1, 1, _t("Loading_game_data_failed"));
				}
			}
			if (!graphicsPreloadStarted) {
				graphicsPreloadStarted = true;
				Lang.reloadBundle();
				ShipHelperWidget.clearAllCaches();
				MonsterHelperWidget.clearAllCaches();
				Appearance.reloadSpritesheets();
				AirshipGame.instance.doLowMemoryCheck();
				return new LoadProgress(false, false, 4 + toGenerate.size(), 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, _t("Loading_game_data_complete"));
			}
			//System.out.println("preloadGraphics " + preloadGraphics);
			if (preloadGraphics) {
				ArrayList<SpritesheetBundle> bs = Loadable.all(SpritesheetBundle.class);
				for (int day = 0; day < 2; day++) {
					String postfix = day == 0 ? "" : "DAY";
					for (int i = 0; i < bs.size(); i++) {
						//System.out.println(" preload "+ bs.get(i).name + postfix);
						if (!bs.get(i).isPostfixLoaded(postfix)) {
							bs.get(i).loadPostfix(postfix);
							int bundleProgress = day * bs.size() + i;
							//System.out.println("load");
							return new LoadProgress(false, false, 4 + toGenerate.size() + bundleProgress, 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, _t("Preloading_graphics"));
						}
					}
				}
			}
			return new LoadProgress(true, false, 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, 4 + toGenerate.size() + Loadable.all(SpritesheetBundle.class).size() * 2, _t("Loading_game_data_complete"));
		}
	}
	
	public static final int GENERATED_DATA_VERSION = 1;
	
	public boolean needsGenerateDerivedData() throws IOException {
		System.out.println(id);
		File csF = new File(new File(dir, "generated"), "checksum.txt");
		File ssbCSF = new File(new File(dir, "generated"), "ssb_checksum.txt");
		if (ssbCSF.exists()) {
			try {
				boolean wrong = Long.parseLong(FileUtils.readFileToString(ssbCSF, "UTF-8")) != getSSBChecksum2();
				System.out.println("SSB Checksum wrong: " + wrong);
				return wrong;
			} catch (Exception e) {
				System.out.println("SSB checksumming failed");
				System.err.println("SSB checksumming failed");
				e.printStackTrace();
				return true;
			}
		} else if (csF.exists()) {
			try {
				boolean wrong = Long.parseLong(FileUtils.readFileToString(csF, "UTF-8")) != getChecksum();
				System.out.println("Old Checksum wrong: " + wrong);
				return wrong;
			} catch (Exception e) {
				System.out.println("Old checksumming failed");
				System.err.println("Old checksumming failed");
				e.printStackTrace();
				return true;
			}
		} else {
			System.out.println("No checksum files exist");
			return true;
		}
	}
	
	private long lastChecksumCache;
	private boolean checksumCalculated = false;
	
	public ArrayList<String> getChunks() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipUtils.enzip(dir, bos);
		String s = Base64.encodeBase64String(bos.toByteArray());
		ArrayList<String> chunks = new ArrayList<String>();
		int n = 0;
		while (n < s.length()) {
			chunks.add(s.substring(n, StrictMath.min(n + GAME_DATA_CHUNK_MAX_SIZE, s.length())));
			n += GAME_DATA_CHUNK_MAX_SIZE;
		}
		return chunks;
	}
	
	public long getCachedChecksum() {
		if (!checksumCalculated) {
			try {
				return getChecksum();
			} catch (Exception e) {
				return 0;
			}
		}
		return lastChecksumCache;
	}
	
	private long getChecksum() throws IOException {
		long cs = getChecksum(dir);
		lastChecksumCache = cs;
		checksumCalculated = true;
		return cs;
	}
	
	private static long getChecksum(File dir) throws IOException {
		long cs = GENERATED_DATA_VERSION;
		for (File f : dir.listFiles()) {
			if (f.getName().startsWith(".") || f.getName().equals("generated")) {
				continue;
			}
			cs += checksum(f);
		}
		return cs;
	}
	
	private static class D_App {
		public final String spritesheetBundle;
		public final ArrayList<Img> frames = new ArrayList<Img>();
		public final int interval;
		public int w, h;
		
		public D_App(JSONObject o) {
			spritesheetBundle = o.getString("src");
			if (o.has("x")) {
				frame(o.getInt("x"), o.getInt("y"), o.optInt("w", 1), o.optInt("h", 1), o.optBoolean("flipped", false));
			} else {
				JSONArray framesA = o.getJSONArray("frames");
				for (int i = 0; i < framesA.length(); i++) {
					JSONObject f = framesA.getJSONObject(i);
					frame(f.getInt("x"), f.getInt("y"), f.optInt("w", 1), f.optInt("h", 1), f.optBoolean("flipped", false));
				}
			}
			interval = o.optInt("interval", 300) <= 0 ? 300 : o.optInt("interval", 300);
		}
		
		private D_App flip() {
			return this;
		}
		
		private void frame(int x, int y, int w, int h, boolean flipped) {
			Img f = new Img(spritesheetBundle, x * AGame.SGS, y * AGame.SGS, w * AGame.SGS, h * AGame.SGS, flipped);
			frames.add(f);
			this.w = StrictMath.max(this.w, w);
			this.h = StrictMath.max(this.h, h);
		}
		
		private long checksum() {
			int cs = spritesheetBundle.hashCode();
			for (Img img : frames) {
				cs += img.src.hashCode() * 7 + img.srcX * 37 + img.srcY * 193 + img.srcWidth * 401 + img.srcHeight * 1299;
				cs *= 19;
			}
			cs += interval;
			return cs;
		}
	}
	
	private static class D_ExternalApp {
		public final D_App app;
		public final int dx, dy;

		public D_ExternalApp(D_App app, int dx, int dy) {
			this.app = app;
			this.dx = dx;
			this.dy = dy;
		}
	}
	
	private static class D_Apps {
		public ArrayList<ArrayList<String>> bonuses = new ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<D_App>> apps = new ArrayList<ArrayList<D_App>>();
		
		private D_Apps() {}
		
		public D_Apps(JSONObject o, String key, boolean appList) {
			if (!o.has(key)) { return; }
			if (o.get(key) instanceof JSONArray) {
				if (!appList) {
					throw new RuntimeException("Expected single app at " + o.toString() + " key " + key);
				}
				bonuses.add(new ArrayList<String>());
				apps.add(values(o, key, true));
			} else {
				JSONObject o2 = o.getJSONObject(key);
				if (o2.has("base")) {
					if (o2.has("cases")) {
						bonuses.add(new ArrayList<String>());
						apps.add(values(o2, "base", appList));
						JSONArray cases = o2.getJSONArray("cases");
						for (int ci = 0; ci < cases.length(); ci++) {
							JSONObject c = cases.getJSONObject(ci);
							JSONArray ba = c.getJSONArray("bonuses");
							ArrayList<String> bl = new ArrayList<String>();
							for (int bi = 0; bi < ba.length(); bi++) {
								bl.add(ba.getString(bi));
							}
							bonuses.add(bl);
							apps.add(values(c, "value", appList));
						}
					} else {
						for (Object ko : o2.keySet()) { // This is OK because it's a tree map.
							ArrayList<String> bl = new ArrayList<String>();
							bl.add((String) ko);
							bonuses.add(bl);
							apps.add(values(o2, (String) ko, appList));
						}
					}
				} else {
					if (appList) {
						throw new RuntimeException("Expected list of apps at " + o.toString() + " key " + key);
					}
					bonuses.add(new ArrayList<String>());
					apps.add(values(o, key, false));
				}
			}
		}
		
		private ArrayList<D_App> values(JSONObject o, String key, boolean appList) {
			ArrayList<D_App> l = new ArrayList<D_App>();
			if (appList) {
				JSONArray a = o.getJSONArray(key);
				for (int i = 0; i < a.length(); i++) {
					l.add(new D_App(a.getJSONObject(i)));
				}
			} else {
				l.add(new D_App(o.getJSONObject(key)));
			}
			return l;
		}
		
		private D_Apps flip() {
			D_Apps das2 = new D_Apps();
			das2.bonuses = bonuses;
			for (ArrayList<D_App> dal : apps) {
				ArrayList<D_App> dal2 = new ArrayList<D_App>();
				das2.apps.add(dal2);
				for (D_App da : dal) {
					dal2.add(da.flip());
				}
			}
			return das2;
		}
				
		private long checksum(long cs, String ssbName) {
			if (apps.size() == 1) {
				// Old case with no bonusable values, must be backwards-compatible
				ArrayList<D_App> l = apps.get(0);
				if (l.size() == 1) {
					// Module type app
					if (l.get(0).spritesheetBundle.equals(ssbName)) {
						cs += l.get(0).checksum();
					}
				} else {
					// ArmourType damaged apps
					for (D_App app : l) {
						if (app.spritesheetBundle.equals(ssbName)) {
							cs *= 17;
							cs += app.checksum();
						}
					}
				}
			} else {
				// New case with bonusable value, can be different
				for (ArrayList<String> bs : bonuses) {
					for (String b : bs) {
						cs += b.hashCode();
						cs *= 19;
					}
				}
				for (ArrayList<D_App> bs : apps) {
					for (D_App a : bs) {
						cs += a.checksum();
						cs *= 17;
					}
				}
				cs *= 7;
			}			
			return cs;
		}
	}
	
	private static class D_ExternalApps {
		public ArrayList<ArrayList<String>> bonuses = new ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<D_ExternalApp>> apps = new ArrayList<ArrayList<D_ExternalApp>>();
		
		private D_ExternalApps() {}
		
		public D_ExternalApps(JSONObject o, String key) {
			if (!o.has(key)) { return; }
			if (o.get(key) instanceof JSONArray) {
				bonuses.add(new ArrayList<String>());
				apps.add(values(o, key));
			} else {
				JSONObject o2 = o.getJSONObject(key);
				if (o2.has("base")) {
					if (o2.has("cases")) {
						bonuses.add(new ArrayList<String>());
						apps.add(values(o2, "base"));
						JSONArray cases = o2.getJSONArray("cases");
						for (int ci = 0; ci < cases.length(); ci++) {
							JSONObject c = cases.getJSONObject(ci);
							JSONArray ba = c.getJSONArray("bonuses");
							ArrayList<String> bl = new ArrayList<String>();
							for (int bi = 0; bi < ba.length(); bi++) {
								bl.add(ba.getString(bi));
							}
							bonuses.add(bl);
							apps.add(values(c, "value"));
						}
					} else {
						for (Object ko : o2.keySet()) { // This is OK because it's a tree map.
							ArrayList<String> bl = new ArrayList<String>();
							bl.add((String) ko);
							bonuses.add(bl);
							apps.add(values(o2, (String) ko));
						}
					}
				} else {
					throw new RuntimeException("Expected list of external apps at " + o.toString() + " key " + key);
				}
			}
		}
		
		private ArrayList<D_ExternalApp> values(JSONObject o, String key) {
			ArrayList<D_ExternalApp> l = new ArrayList<D_ExternalApp>();
			JSONArray a = o.getJSONArray(key);
			for (int i = 0; i < a.length(); i++) {
				JSONObject ea = a.getJSONObject(i);
				l.add(new D_ExternalApp(new D_App(ea.getJSONObject("appearance")),
						ea.optInt("x", 0),
						ea.optInt("y", 0)));
			}
			return l;
		}
		
		private D_ExternalApps flip(int w) {
			D_ExternalApps deas2 = new D_ExternalApps();
			deas2.bonuses = bonuses;
			for (ArrayList<D_ExternalApp> eal : apps) {
				ArrayList<D_ExternalApp> eal2 = new ArrayList<D_ExternalApp>();
				deas2.apps.add(eal2);
				for (D_ExternalApp ea : eal) {
					int fromRight = w - ea.app.w - ea.dx;
					eal2.add(new D_ExternalApp(ea.app.flip(), fromRight, ea.dy));
				}
			}
			return deas2;
		}
		
		private long checksum(long cs, String ssbName) {
			if (apps.isEmpty()) {
				return cs;
			}
			if (apps.size() == 1) {
				// Old case with no bonusable values, must be backwards-compatible
				ArrayList<D_ExternalApp> l = apps.get(0);
				for (D_ExternalApp ea : l) {
					if (ea.app.spritesheetBundle.equals(ssbName)) {
						cs *= 41;
						cs += ea.app.checksum();
					}
				}
			} else {
				// New case with bonusable value, can be different
				for (ArrayList<String> bs : bonuses) {
					for (String b : bs) {
						cs *= 19;
						cs += b.hashCode();
					}
				}
				for (ArrayList<D_ExternalApp> bs : apps) {
					for (D_ExternalApp a : bs) {
						cs *= 11;
						cs += a.app.checksum();
						cs *= 17;
						cs += a.dx;
						cs *= 7;
						cs += a.dy;
					}
				}
				cs *= 7;
			}			
			return cs;
		}
	}
	
	private static class D_WheelSpec {
		public final Img wheel, upperLink, lowerLink;
		
		public D_WheelSpec(JSONObject o) {
			wheel = Loadable.img(o.getJSONObject("wheel"));
			upperLink = Loadable.img(o.getJSONObject("upperLink"));
			lowerLink = Loadable.img(o.getJSONObject("lowerLink"));
		}
	}
	
	private static class D_LegSpec {
		public final Img upperLeg, lowerLeg, foot;
		
		public D_LegSpec(JSONObject o) {
			upperLeg = Loadable.img(o.getJSONObject("upperLeg"));
			lowerLeg = Loadable.img(o.getJSONObject("lowerLeg"));
			foot = Loadable.img(o.getJSONObject("foot"));
		}
	}
	
	private static class R_ implements Comparable<R_> {
		public final String name;
		public final int sort;

		public R_(String name, int sort) {
			this.name = name;
			this.sort = sort;
		}
		
		@Override
		public int compareTo(R_ l2) {
			if (sort == l2.sort) {
				return name.compareTo(l2.name);
			} else {
				return sort - l2.sort;
			}
		}
	}
	
	private static class R_SpritesheetBundle extends R_ {
		public final String bump;
		public final String fragments;

		public R_SpritesheetBundle(JSONObject o) {
			super(o.getString("name"), o.optInt("sort", 0));
			bump = o.optString("bump", null);
			fragments = o.optString("fragments", null);
		}
	}
	
	private static class R_ModuleType extends R_ {
		public String flippedFrom;
		public D_Apps app;
		public int w;
		public D_ExternalApps externalApps = new D_ExternalApps();
		public ArrayList<D_LegSpec> legSpecs = new ArrayList<D_LegSpec>();
		public ArrayList<D_WheelSpec> wheelSpecs = new ArrayList<D_WheelSpec>();
		
		public void deriveFlipped(R_ModuleType b) {
			app = b.app.flip();
			externalApps = b.externalApps.flip(b.w);
		}
		
		public R_ModuleType(JSONObject o) {
			super(o.getString("name"), o.optInt("sort", 0));
			
			if (o.has("flippedFrom")) {
				flippedFrom = o.getString("flippedFrom");
				return; // Everything else is handled in post-load.
			}
			
			w = o.optInt("w", 0);
			
			app = new D_Apps(o, "appearance", false);
			if (o.has("externalAppearances")) {
				externalApps = new D_ExternalApps(o, "externalAppearances");
			}
			if (o.has("wheels")) {
				JSONArray ws = o.getJSONArray("wheels");
				for (int i = 0; i < ws.length(); i++) {
					JSONObject wh = ws.getJSONObject(i);
					wheelSpecs.add(new D_WheelSpec(wh));
				}
			}
			if (o.has("legs")) {
				JSONArray ls = o.getJSONArray("legs");
				for (int i = 0; i < ls.length(); i++) {
					JSONObject l = ls.getJSONObject(i);
					legSpecs.add(new D_LegSpec(l));
				}
			}
		}
	}
	
	private static class R_ArmourType extends R_ {
		public final D_Apps damagedApps;
		
		public R_ArmourType(JSONObject o) {
			super(o.getString("name"), o.optInt("sort", 0));
			damagedApps = new D_Apps(o, "damagedApps", true);
		}
	}
	
	private long getSSBChecksum2() throws IOException {
		File[] fs = new File(dir, "SpritesheetBundle").listFiles();
		HashMap<String, R_SpritesheetBundle> ssbsH = new HashMap<String, R_SpritesheetBundle>();
		if (fs != null) {
			List<File> fsL = Arrays.asList(fs);
			Collections.sort(fsL);
			for (File f : fsL) {
				if (!f.getName().endsWith(".json")) { continue; }
				JSONArray a = new JSONArray(FileUtils.readFileToString(f, "UTF-8"));
				for (int i = 0; i < a.length(); i++) {
					if (!a.getJSONObject(i).optBoolean("remove", false)) {
						ssbsH.put(a.getJSONObject(i).getString("name"), new R_SpritesheetBundle(a.getJSONObject(i)));
					}
				}
			}
		}
		ArrayList<R_SpritesheetBundle> ssbsL = new ArrayList<R_SpritesheetBundle>();
		ssbsL.addAll(ssbsH.values());
		Collections.sort(ssbsL);
		
		HashMap<String, R_ModuleType> mtsH = new HashMap<String, R_ModuleType>();
		fs = new File(dir, "ModuleType").listFiles();
		if (fs != null) {
			List<File> fsL = Arrays.asList(fs);
			Collections.sort(fsL);
			for (File f : fsL) {
				if (!f.getName().endsWith(".json")) { continue; }
				JSONArray a = new JSONArray(FileUtils.readFileToString(f, "UTF-8"));
				for (int i = 0; i < a.length(); i++) {
					if (!a.getJSONObject(i).optBoolean("remove", false)) {
						mtsH.put(a.getJSONObject(i).getString("name"), new R_ModuleType(a.getJSONObject(i)));
					}
				}
			}
		}
		ArrayList<R_ModuleType> mtsL = new ArrayList<R_ModuleType>();
		mtsL.addAll(mtsH.values());
		for (R_ModuleType mt : mtsL) {
			if (mt.flippedFrom != null) {
				for (R_ModuleType mt2 : mtsL) {
					if (mt2.name.equals(mt.flippedFrom)) {
						mt.deriveFlipped(mt2);
					}
				}
			}
		}
		Collections.sort(mtsL);
		
		HashMap<String, R_ArmourType> atsH = new HashMap<String, R_ArmourType>();
		fs = new File(dir, "ArmourType").listFiles();
		if (fs != null) {
			for (File f : fs) {
				if (!f.getName().endsWith(".json")) { continue; }
				JSONArray a = new JSONArray(FileUtils.readFileToString(f, "UTF-8"));
				for (int i = 0; i < a.length(); i++) {
					if (!a.getJSONObject(i).optBoolean("remove", false)) {
						atsH.put(a.getJSONObject(i).getString("name"), new R_ArmourType(a.getJSONObject(i)));
					}
				}
			}
		}
		ArrayList<R_ArmourType> atsL = new ArrayList<R_ArmourType>();
		atsL.addAll(atsH.values());
		Collections.sort(atsL);
		
		long cs = 0;
		for (R_SpritesheetBundle ssb : ssbsL) {
			cs *= 677;
			cs += checksum(new File(new File(dir, "images"), ssb.name + ".png"));
			if (ssb.bump != null) {
				cs *= 73;
				cs += checksum(new File(new File(dir, "images"), ssb.bump + ".png"));
			}
			if (ssb.fragments != null) {
				cs *= 73;
				cs += checksum(new File(new File(dir, "images"), ssb.fragments + ".png"));
				for (R_ModuleType mt : mtsL) {
					cs = mt.app.checksum(cs, ssb.name);
					cs = mt.externalApps.checksum(cs, ssb.name);
					for (D_LegSpec ls : mt.legSpecs) {
						if (ls.upperLeg.src.equals(ssb.name)) {
							cs *= 31;
							cs += checksum(ls.upperLeg);
						}
						if (ls.lowerLeg.src.equals(ssb.name)) {
							cs *= 31;
							cs += checksum(ls.lowerLeg);
						}
						if (ls.foot != null && ls.foot.src.equals(ssb.name)) {
							cs *= 31;
							cs += checksum(ls.foot);
						}
					}
					for (D_WheelSpec ws : mt.wheelSpecs) {
						if (ws.wheel.src.equals(ssb.name)) {
							cs *= 19;
							cs += checksum(ws.wheel);
						}
						if (ws.upperLink.src.equals(ssb.name)) {
							cs *= 19;
							cs += checksum(ws.upperLink);
						}
						if (ws.lowerLink.src.equals(ssb.name)) {
							cs *= 19;
							cs += checksum(ws.lowerLink);
						}
					}
				}
				for (R_ArmourType at : atsL) {
					cs *= 73;
					cs = at.damagedApps.checksum(cs, ssb.name);
				}
			}
		}
		return cs;
	}
	
	public static long checksum(Img img) {
		return img.srcX + 41 * img.srcY + 199 * img.srcWidth + 701 * img.srcHeight;
	}
	
	public static long checksum(File f) throws IOException {
		if (f.isDirectory()) {
			long cs = 0;
			for (File child : f.listFiles()) {
				if (child.getName().startsWith(".")) {
					continue;
				}
				cs += checksum(child);
			}
			//System.out.println(f.getPath() + " " + cs);
			return cs;
		} else {
			return FileUtils.checksumCRC32(f);
		}
	}
	
	public void regenerateDerivedData(Input in) {
		generateDerivedData(in);
		resetLoadBases(AirshipGame.getMyInput(in));
		for (Mod m : getEnabledMods()) {
			m.addLoadBases(AirshipGame.getMyInput(in));
		}
		Loadable.load();
		Lang.reloadBundle();
		ShipHelperWidget.clearAllCaches();
		MonsterHelperWidget.clearAllCaches();
		Appearance.reloadSpritesheets();
	}

	private void generateDerivedData(Input in) {
		ArrayList<String> originalOEMIs = overriddenEnabledModIds;
		ArrayList<String> l = new ArrayList<String>();
		l.add(id);
		overrideModsToLoad(l, null, Expansion.all());
		resetLoadBases(AirshipGame.getMyInput(in));
		Loadable.load();
		addLoadBases(AirshipGame.getMyInput(in));
		ArrayList<String> log = doGenerateDerivedData();
		StringBuilder sb = new StringBuilder();
		for (String s : log) {
			sb.append(s).append("\n");
		}
		buildLog = sb.toString();
		overrideModsToLoad(originalOEMIs, null, Expansion.installeds());
	}
	
	private ArrayList<String> doGenerateDerivedData() {
		buildFailed = false;
		// qqDPS Make sure to unload all other mods before calling this.
		ArrayList<String> log = new ArrayList<String>();
		try {
			log.add("Generating derived data for mod " + id + ".");
			String mapping = "";
			File genF = new File(dir, "generated");
			genF.mkdirs();
			for (SpritesheetBundle ssb : Loadable.all(SpritesheetBundle.class)) {
				if (ssb.sourceMod != this) { continue; }
				AirshipGame.instance.tickClients();
				log.add("Spritesheet Bundle " + ssb.name + ".");
				if (ssb.bump != null) {
					log.add("Baking light maps.");
					for (TimeOfDay tod : Loadable.all(TimeOfDay.class)) {
						ImageIO.write(LightmapBakery.bake(ssb.name, ssb.bump, tod), "PNG", new File(genF, ssb.name + tod.name + ".png"));
					}
					ImageIO.write(LightmapBakery.bakeBlueprint(ssb.name), "PNG", new File(genF, ssb.name + "BLUEPRINT" + ".png"));
				}
				if (ssb.bump != null && ssb.fragments != null) {
					log.add("Generating fragments.");
					FragmentGen.FragmentInfo fi = FragmentGen.generate(ssb);
					ImageIO.write(fi.sheet, "PNG", new File(genF, ssb.name + "FRAGMENTS.png"));
					ImageIO.write(fi.bump, "PNG", new File(genF, ssb.bump + "FRAGMENTS.png"));
					mapping += fi.mapping;
					log.add("Generating damaged variant.");
					Utils.Pair<BufferedImage, BufferedImage> p = DamagedAppGen.generate(
							ssb.name, ssb.bump, ssb.fragments
					);
					ImageIO.write(p.a, "PNG", new File(genF, ssb.name + "DAMAGED.png"));
					ImageIO.write(p.b, "PNG", new File(genF, ssb.bump + "DAMAGED.png"));
					log.add("Baking damaged variants.");
					for (TimeOfDay tod : Loadable.all(TimeOfDay.class)) {
						ImageIO.write(LightmapBakery.bake(p.a, p.b, tod), "PNG", new File(genF, ssb.name + "DAMAGED" + tod.name + ".png"));
					}
				}
			}
			log.add("Writing out fragment mapping.");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(genF, "fragments.txt")), "UTF-8"));
			try {
				bw.write(mapping);
			} finally {
				bw.close();
			}
			log.add("Writing out SSB checksum.");
			FileUtils.write(new File(genF, "ssb_checksum.txt"), "" + getSSBChecksum2(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			loadFailed = true;
			buildFailed = true;
			log.add("Failure: " + e.getMessage());
		}
		return log;
	}

	public static void overrideModsToLoad(ArrayList<String> modIDs, HashMap<String, Long> useHashVersions, ArrayList<Expansion> expansions) {
		for (Expansion ex : Expansion.all()) {
			ex.enabled = expansions.contains(ex);
		}
		overriddenEnabledModIds = modIDs;
		if (modIDs == null) {
			modChecksumVersions = null;
		} else if (useHashVersions != null) {
			HashMap<String, Long> versions = new HashMap<String, Long>();
			for (Map.Entry<String, Long> e : useHashVersions.entrySet()) {
				Mod m = getById(e.getKey());
				if (m == null || m.getCachedChecksum() != e.getValue()) {
					versions.put(e.getKey(), e.getValue());
				}
			}
			if (versions.isEmpty()) {
				modChecksumVersions = null;
			} else {
				modChecksumVersions = versions;
			}
		}
	}
	
	public static boolean areModsToLoadOverridden() {
		return overriddenEnabledModIds != null || !Expansion.enableds().equals(Expansion.installeds());
	}
}
