package com.zarkonnen.airships;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.PropertyResourceBundle;

/** i18n system */
public class Lang {
	public static Locale currentLocale = getBestLocalLocale();
	public static MultiResourceBundle bundle = null;
	public static MultiResourceBundle enBundle = null;
	
	public static Locale currentAltLocale = null;
	public static MultiResourceBundle currentAltBundle = null;
	
	public static void setCurrentLocale(Locale l) {
		if (availableLocales().contains(l)) {
			currentLocale = l;
			bundle = null;
			currentAltLocale = null;
			currentAltBundle = null;
			Airship.nt = "[bb421d]" + _t("No_target_available");
		}
	}
	
	public static boolean hasEnKey(String key) {
		if (enBundle == null) {
			enBundle = loadBundle("en");
		}
		return enBundle.containsKey(key);
	}
	
	public static boolean hasString(String key) {
		return getString(key) != null;
	}
	
	public static boolean hasLocalString(String key) {
		if (bundle == null) {
			bundle = loadBundle(currentLocale.toLanguageTag());
		}
		return bundle != null && bundle.containsKey(key);
	}
	
	private static String getString(String key) {
		return getString(key, true);
	}
	
	private static String getString(String key, boolean canRedirect) {
		if (bundle == null) {
			bundle = loadBundle(currentLocale.toLanguageTag());
		}
		if (enBundle == null) {
			enBundle = loadBundle("en");
		}
		
		String v =
				bundle.containsKey(key)
				? bundle.getString(key)
				: enBundle.containsKey(key)
				? enBundle.getString(key)
				: null;
		if (canRedirect && v != null && v.startsWith(">>")) {
			return getString(v.substring(2), false);
		} else {
			return v;
		}
	}
	
	private static String getLocaleString(Locale l, String key) {
		if (currentAltLocale == null || !currentAltLocale.equals(l)) {
			currentAltLocale = l;
			currentAltBundle = loadBundle(l.toLanguageTag());
		}
		
		return
				currentAltBundle.containsKey(key)
				? currentAltBundle.getString(key)
				: enBundle.containsKey(key)
				? enBundle.getString(key)
				: null;
	}
	
	public static void reloadBundle() {
		bundle = loadBundle(currentLocale.toLanguageTag());
		enBundle = loadBundle("en");
		currentAltLocale = null;
		currentAltBundle = null;
	}
	
	private static MultiResourceBundle loadBundle(String lang) {
		MultiResourceBundle mrb = loadBaseBundle(lang);
		if (mrb == null) { return null; }
		for (Expansion ex : Expansion.enableds()) {
			File bundleF = new File(new File(ex.getDataDir(), "strings"), lang + ".properties");
			if (bundleF.exists()) {
				try {
					BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(bundleF), "UTF-8"));
					mrb.add(new PropertyResourceBundle(r));
					r.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		for (Mod m : Mod.getEnabledMods()) {
			if (!m.loadFailed) {
				File bundleF = new File(new File(m.dir, "strings"), lang + ".properties");
				if (bundleF.exists()) {
					try {
						BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(bundleF), "UTF-8"));
						mrb.add(new PropertyResourceBundle(r));
						r.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return mrb;
	}
	
	private static MultiResourceBundle loadBaseBundle(String lang) {
		MultiResourceBundle mrb = null;
		File bundleF = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "lang"), lang + ".properties");
		if (!bundleF.exists()) {
			for (Mod m : Mod.getEnabledMods()) {
				if (!m.loadFailed) {
					bundleF = new File(new File(m.dir, "translations"), currentLocale.toLanguageTag() + ".properties");
					if (bundleF.exists()) { break; }
				}
			}
		}
		if (!bundleF.exists()) { return null; }
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(bundleF), "UTF-8"));
			mrb = new MultiResourceBundle(new PropertyResourceBundle(r));
			r.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mrb;
	}
	
	public static boolean flavour() {
		return "true".equals(getString("flavour"));
	}
	
	//static HashSet<String> bads = new HashSet<String>();
	
	public static String _t(String key, Object... arguments) {
		if (key == null) { return null; }
		for (int i = 0; i < arguments.length; i++) {
			if (arguments[i] instanceof Integer) {
				arguments[i] = ((Integer) arguments[i]).toString();
			}
			if (arguments[i] instanceof Double) {
				arguments[i] = ((Double) arguments[i]).toString();
			}
		}
		String value = getString(key);
		if (value == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[RED]? ");
			sb.append(key);
			sb.append(": ");
			for (Object o : arguments) {
				sb.append(o.toString().replaceAll("\\[|\\]|\\{\\}", ""));
				sb.append(", ");
			}
			sb.append("[]");
			return sb.toString();
		} else {
			try {
				return new MessageFormat(getString(key), currentLocale).format(arguments).replace("QUOTE", "'");
			} catch (Exception e) {
				return key + " (ERROR: BAD FORMAT)";
			}
		}
	}
	
	public static String _tWithFallback(String key, String altKey, Object... arguments) {
		for (int i = 0; i < arguments.length; i++) {
			if (arguments[i] instanceof Integer) {
				arguments[i] = ((Integer) arguments[i]).toString();
			}
		}
		String value = getString(key);
		if (value == null) {
			return _t(altKey, arguments);
		} else {
			try {
				return new MessageFormat(getString(key), currentLocale).format(arguments).replace("QUOTE", "'");
			} catch (Exception e) {
				return key + " (ERROR: BAD FORMAT)";
			}
		}
	}
	
	public static String _tWithUntranslatedFallback(String key, String fallback, Object... arguments) {
		for (int i = 0; i < arguments.length; i++) {
			if (arguments[i] instanceof Integer) {
				arguments[i] = ((Integer) arguments[i]).toString();
			}
		}
		String value = getString(key);
		if (value == null) {
			return fallback;
		} else {
			try {
				return new MessageFormat(getString(key), currentLocale).format(arguments).replace("QUOTE", "'");
			} catch (Exception e) {
				return key + " (ERROR: BAD FORMAT)";
			}
		}
	}
	
	public static String localeT(Locale l, String key, Object... arguments) {
		for (int i = 0; i < arguments.length; i++) {
			if (arguments[i] instanceof Integer) {
				arguments[i] = ((Integer) arguments[i]).toString();
			}
		}
		String value = getLocaleString(l, key);
		if (value == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[RED]? ");
			sb.append(key);
			sb.append(": ");
			for (Object o : arguments) {
				sb.append(o.toString().replaceAll("\\[|\\]|\\{\\}", ""));
				sb.append(", ");
			}
			sb.append("[]");
			return sb.toString();
		} else {
			try {
				String s = getLocaleString(l, key);
				return new MessageFormat(s, currentAltLocale).format(arguments).replace("QUOTE", "'");
			} catch (Exception e) {
				AirshipGame.instance.reportError("Alt locale translation fail", e, key, false, true);
				return key;
			}
		}
	}
	
	public static ArrayList<Locale> availableLocales() {
		ArrayList<Locale> ls = new ArrayList<Locale>();
		File langDir = new File(new File(AGame.getStaticGameDirectory(), "data"), "lang");
		File[] fs = langDir.listFiles();
		if (fs == null) {
			throw new RuntimeException("Cannot load languages from " + langDir.getAbsolutePath());
		}
		for (File f : fs) {
			if (f.getName().endsWith(".properties")) {
				Locale l = Locale.forLanguageTag(f.getName().substring(0, f.getName().length() - ".properties".length()));
				if (l != null) {
					ls.add(l);
				}
			}
		}
		for (Mod m : Mod.getEnabledMods()) {
			if (!m.loadFailed && new File(m.dir, "translations").exists()) {
				for (File f : new File(m.dir, "translations").listFiles()) {
					if (f.getName().endsWith(".properties")) {
						Locale l = Locale.forLanguageTag(f.getName().substring(0, f.getName().length() - ".properties".length()));
						if (l != null) {
							ls.add(l);
						}
					}
				}
			}
		}
		return ls;
	}
	
	public static Locale getBestLocalLocale() {
		Locale loc = Locale.getDefault();
		ArrayList<Locale> ls = availableLocales();
		if (ls.contains(loc)) {
			return loc;
		}
		loc = new Locale(loc.getLanguage());
		if (ls.contains(loc)) {
			return loc;
		}
		return Locale.ENGLISH;
	}
}
