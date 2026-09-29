package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.util.ArrayList;

public strictfp class TimeOfDay extends Loadable {
	public final Color[] light;
	public final float lightStrength;
	public final double haloStrength;
	public final Clr[] skyColors;
	public final int skyColorBandHeight;
	public final WeatherEffect effect;
	public final Color ambient;
	public final float ambientAmt;
	public final float ambientSaturation;
	public final Color backdropAmbient;
	public final float backdropAmbientSaturation;
	public final Clr ambientTint;
	public final Clr deepSoilTint;
	public final int minCloudSpawnHeight, maxCloudSpawnHeight;
	public final int minNumClouds;
	public final int maxNumClouds;
	public final CloudType clouds;
	public final boolean cloudBottomsAligned;
	public final String appearancePostfix;
	public final boolean appearancePostfixIsBad;
	public final boolean isFairForTournament;
	public final boolean standard;
	public final boolean allowCloudOverlap;
	public final ArrayList<String> canTurnInto = new ArrayList<String>();
	public final Clr waterColor, deepWaterColor, waterOverlay, waterOverlayOpaque;
	public final Img waveImg, waveOverlayImg;
	public final double waveSpeed;
	
	public static final ArrayList<String> ORIGINAL_APPEARANCE_POSTFIXES = new ArrayList<String>();
	static {
		ORIGINAL_APPEARANCE_POSTFIXES.add("DAY");
		ORIGINAL_APPEARANCE_POSTFIXES.add("NIGHT");
		ORIGINAL_APPEARANCE_POSTFIXES.add("DAWN");
		ORIGINAL_APPEARANCE_POSTFIXES.add("DUSK");
		ORIGINAL_APPEARANCE_POSTFIXES.add("SNOW");
		ORIGINAL_APPEARANCE_POSTFIXES.add("RAIN");
		ORIGINAL_APPEARANCE_POSTFIXES.add("FOG");
		ORIGINAL_APPEARANCE_POSTFIXES.add("STORM");
	}
	
	public String getName() {
		return _t("timeOfDay_" + name);
	}
	
	private static Color getColor(JSONObject o) {
		return new Color(o.getInt("r"), o.getInt("g"), o.getInt("b"));
	}
	
	private static Clr getClr(JSONObject o) {
		return new Clr(o.getInt("r"), o.getInt("g"), o.getInt("b"));
	}
	
	private static Clr optClrWithAlpha(JSONObject o, String key, Clr def) {
		if (o.has(key)) {
			o = o.getJSONObject(key);
			return new Clr(o.getInt("r"), o.getInt("g"), o.getInt("b"), o.optInt("a", 255));
		}
		return def;
	}
	
	public TimeOfDay(JSONObject o) {
		super(o.getString("name"));
		if (o.has("topLight")) {
			light = new Color[] {
				getColor(o.getJSONObject("topLight")),
				getColor(o.getJSONObject("leftLight")),
				getColor(o.getJSONObject("rightLight")),
				getColor(o.getJSONObject("bottomLight"))
			};
		} else {
			light = new Color[] {
				getColor(o.getJSONObject("fromLeftLight")),
				getColor(o.getJSONObject("fromTopLight")),
				getColor(o.getJSONObject("fromRightLight")),
				getColor(o.getJSONObject("fromBottomLight"))
			};
		}
		standard = o.optBoolean("standard", true);
		ambientSaturation = (float) o.optDouble("ambientSaturation", 1.0);
		if (o.optJSONObject("ambient") != null) {
			ambient = getColor(o.getJSONObject("ambient"));
			ambientTint = getClr(o.getJSONObject("ambient"));
			ambientAmt = (float) ((ambient.r + ambient.g + ambient.b) * 1.0 / (255 * 3));
			deepSoilTint = new Clr((int) (97 * 0.9 * ambientTint.r / 255), (int) (63 * 0.9 * ambientTint.g / 255), (int) (45 * 0.9 * ambientTint.b / 255));
		} else {
			ambientAmt = (float) o.getDouble("ambient");
			ambient = new Color(ambientAmt, ambientAmt, ambientAmt);
			ambientTint = new Clr((int) (255 * ambientAmt), (int) (255 * ambientAmt), (int) (255 * ambientAmt));
			float a2 = 0.1f + ambientAmt;
			deepSoilTint = new Clr((int) (97 * 0.9 * a2), (int) (63 * 0.9 * a2), (int) (45 * 0.9 * a2));
		}
		if (o.optJSONObject("backdropAmbient") != null) {
			backdropAmbient = getColor(o.getJSONObject("backdropAmbient"));
		} else {
			backdropAmbient = ambient;
		}
		backdropAmbientSaturation = (float) o.optDouble("backdropAmbientSaturation", ambientSaturation * 0.5);
		lightStrength = (float) o.getDouble("lightStrength");
		haloStrength = o.getDouble("haloSize");
		JSONArray sc = o.getJSONArray("skyColors");
		skyColors = new Clr[sc.length()];
		for (int i = 0; i < skyColors.length; i++) {
			skyColors[i] = getClr(sc.getJSONObject(i));
		}
		skyColorBandHeight = o.getInt("skyColorBandHeight");
		effect = WeatherEffect.ofName(o.getString("weatherEffect"));
		if (o.has("clouds")) {
			clouds = CloudType.ofName(o.getString("clouds"));
			minNumClouds = o.optInt("minNumClouds", 5);
			maxNumClouds = o.optInt("maxNumClouds", 7);
		} else {
			clouds = CloudType.ofName("small");
			minNumClouds = 0;
			maxNumClouds = 0;
		}
		cloudBottomsAligned = o.optBoolean("cloudBottomsAligned", false);
		String apf = o.optString("appearancePostfix", name);
		if (!ORIGINAL_APPEARANCE_POSTFIXES.contains(apf)) {
			appearancePostfixIsBad = true;
			appearancePostfix = "DAY";
		} else {
			appearancePostfixIsBad = false;
			appearancePostfix = apf;
		}
		if (o.has("isFairForTournament")) {
			isFairForTournament = o.getBoolean("isFairForTournament");
		} else {
			isFairForTournament = name.equals("DAY") || name.equals("NIGHT") || name.equals("RAIN") || name.equals("FOG") || name.equals("STORM") || name.equals("SNOW");
		}
		minCloudSpawnHeight = o.optInt("minCloudSpawnHeight", 200);
		maxCloudSpawnHeight = o.optInt("maxCloudSpawnHeight", 1600);
		allowCloudOverlap = o.optBoolean("allowCloudOverlap", false);
		if (o.has("canTurnInto")) {
			JSONArray a = o.getJSONArray("canTurnInto");
			for (int i = 0; i < a.length(); i++) {
				canTurnInto.add(a.getString(i));
			}
		}
		waterColor = optClrWithAlpha(o, "waterColor", new Clr(64, 124, 184));
		deepWaterColor = optClrWithAlpha(o, "deepWaterColor", new Clr(44, 86, 128));
		waterOverlay = optClrWithAlpha(o, "waterOverlay", new Clr(44, 86, 128, 128));
		waterOverlayOpaque = new Clr(waterOverlay.r, waterOverlay.g, waterOverlay.b);
		waveSpeed = o.optDouble("waveSpeed", 0);
		if (waveSpeed != 0) {
			JSONObject io = o.getJSONObject("waveImg");
			waveImg = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.getInt("w"), io.getInt("h"), io.optBoolean("flipped", false));
			io = o.getJSONObject("waveOverlayImg");
			waveOverlayImg = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.getInt("w"), io.getInt("h"), io.optBoolean("flipped", false));
		} else {
			waveImg = null;
			waveOverlayImg = null;
		}
	}
	
	public static TimeOfDay ofName(String name) {
		return ofName(TimeOfDay.class, name);
	}
	
	public static void postLoad() {
		TOURNAMENT = new ArrayList<TimeOfDay>();
		for (TimeOfDay tod : all(TimeOfDay.class)) {
			if (tod.isFairForTournament) {
				TOURNAMENT.add(tod);
			}
		}
	}
	
	public static ArrayList<TimeOfDay> TOURNAMENT = null;
	
	public static ArrayList<TimeOfDay> getOriginals() {
		ArrayList<TimeOfDay> l = new ArrayList<TimeOfDay>();
		for (String s : ORIGINAL_APPEARANCE_POSTFIXES) {
			l.add(TimeOfDay.ofName(s));
		}
		return l;
	}
	
	public static TimeOfDay getRandom(GuardedRandom r, LandscapeType lst, Season season, BonusSet worldBonuses) {
		if (season != null) {
			int total = 0;
			for (Pair<Integer, TimeOfDay> p : lst.weightedTimesOfDay) {
				if (season.timesOfDay.get(worldBonuses).isEmpty() || season.timesOfDay.get(worldBonuses).contains(p.b)) {
					total += p.a;
				}
			}
			if (total > 0) {
				int roll = r.nextInt(total);
				for (Pair<Integer, TimeOfDay> p : lst.weightedTimesOfDay) {
					if (season.timesOfDay.get(worldBonuses).isEmpty() || season.timesOfDay.get(worldBonuses).contains(p.b)) {
						roll -= p.a;
						if (roll <= 0) {
							return p.b;
						}
					}
				}
				return lst.weightedTimesOfDay.get(0).b;
			}
		}
		int total = 0;
		for (Pair<Integer, TimeOfDay> p : lst.weightedTimesOfDay) {
			total += p.a;
		}
		int roll = r.nextInt(total);
		for (Pair<Integer, TimeOfDay> p : lst.weightedTimesOfDay) {
			roll -= p.a;
			if (roll <= 0) {
				return p.b;
			}
		}
		return lst.weightedTimesOfDay.get(0).b;
	}
	
	public static TimeOfDay getRandomForTournament(GuardedRandom r) {
		return TOURNAMENT.get(r.nextInt(TOURNAMENT.size()));
	}
}
