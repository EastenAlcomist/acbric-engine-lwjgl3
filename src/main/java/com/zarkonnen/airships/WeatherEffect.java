package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class WeatherEffect extends Loadable {
	public final double shootJitterMult;
	public final double shootToRightJitterMult;
	public final double shootToLeftJitterMult;
	public final double fireExtinguishChance;
	public final double wind;
	public final double lightningChance;
	public final int maxLightningY;
	public final int weakFogLevel;
	public final int fogLevel;
	public final Clr weakFogClr;
	public final Clr fogClr;
	public final double fogJitterMult;
	public final ParticleType groundImpactParticle;
	public final int numGroundImpactParticles;
	public final Appearance icon;
	public final Appearance icon2;
	public final int numStars;
	public final SoundEffect backgroundLoop;
	public final ArrayList<PrecipitationSpawnZone> precipitationSpawnZones = new ArrayList<PrecipitationSpawnZone>();
	public final String landscapeVisualVariant;
	public final double particleDripSpeed;
	public final SoundEffect occasionalSound;
	public final double occasionalSoundChance;
	
	public static class PrecipitationSpawnZone {
		public final int x, y, w, h;
		public final double minXSpeed;
		public final double maxXSpeed;
		public final double minYSpeed;
		public final double maxYSpeed;
		public final int drops;
		public final Clr clr;
		public final double dropW;
		public final double dropL;
		public final ParticleType sticksAs;
		
		public PrecipitationSpawnZone(JSONObject o) {
			x = o.getInt("x");
			y = o.getInt("y");
			w = o.getInt("w");
			h = o.getInt("h");
			drops = o.getInt("drops");
			minXSpeed = o.getDouble("minXSpeed");
			maxXSpeed = o.getDouble("maxXSpeed");
			minYSpeed = o.getDouble("minYSpeed");
			maxYSpeed = o.getDouble("maxYSpeed");
			JSONObject c = o.getJSONObject("clr");
			clr = new Clr(c.getInt("r"), c.getInt("g"), c.getInt("b"), c.optInt("a", 255));
			dropW = o.getInt("dropW");
			dropL = o.getInt("dropL");
			if (o.has("sticksAs")) {
				sticksAs = ParticleType.ofName(o.getString("sticksAs"));
			} else {
				sticksAs = null;
			}
		}

		public PrecipitationSpawnZone(int x, int y, int w, int h, double minXSpeed, double maxXSpeed, double minYSpeed, double maxYSpeed, int drops, Clr clr, double dropW, double dropL, ParticleType sticksAs) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.minXSpeed = minXSpeed;
			this.maxXSpeed = maxXSpeed;
			this.minYSpeed = minYSpeed;
			this.maxYSpeed = maxYSpeed;
			this.drops = drops;
			this.clr = clr;
			this.dropW = dropW;
			this.dropL = dropL;
			this.sticksAs = sticksAs;
		}
	}
	
	public WeatherEffect(JSONObject o) {
		super(o.getString("name"));
		shootJitterMult = o.optDouble("shootJitterMult", 1.0);
		shootToRightJitterMult = o.optDouble("shootToRightJitterMult", 1.0);
		shootToLeftJitterMult = o.optDouble("shootToLeftJitterMult", 1.0);
		fireExtinguishChance = o.optDouble("fireExtinguishChance", 0);
		wind = o.optDouble("wind", 0);
		if (o.has("lightning")) {
			lightningChance = 1.0 / 10000;
		} else {
			lightningChance = o.optDouble("lightningChance", 0);
		}
		maxLightningY = o.optInt("maxLightningY", 1000);
		if (o.optBoolean("fog")) {
			weakFogLevel = 412;
			fogLevel = 400;
			weakFogClr = new Clr(200, 200, 200, 40);
			fogClr = new Clr(200, 200, 200, 40);
			fogJitterMult = 3;
		} else if (o.has("fogClr")) {
			JSONObject c = o.getJSONObject("fogClr");
			fogClr = new Clr(c.getInt("r"), c.getInt("g"), c.getInt("b"), c.optInt("a", 40));
			fogLevel = o.getInt("fogLevel");
			weakFogLevel = o.optInt("weakFogLevel", fogLevel);
			fogJitterMult = o.optDouble("fogJitterMult", 3);
			if (o.has("weakFogClr")) {
				c = o.getJSONObject("weakFogClr");
				weakFogClr = new Clr(c.getInt("r"), c.getInt("g"), c.getInt("b"), c.optInt("a", 40));
			} else {
				weakFogClr = null;
			}
		} else {
			weakFogLevel = 0;
			fogLevel = 0;
			fogClr = null;
			weakFogClr = null;
			fogJitterMult = 1;
		}
		if (o.optBoolean("manyStars", false)) {
			numStars = 1000;
		} else if (o.optBoolean("fewStars", false)) {
			numStars = 100;
		} else {
			numStars = o.optInt("numStars", 0);
		}
		if (o.optBoolean("snow", false)) {
			landscapeVisualVariant = "snow";
		} else {
			landscapeVisualVariant = o.optString("landscapeVisualVariant", "normal");
		}
		if (o.optBoolean("rain", false)) {
			precipitationSpawnZones.add(new PrecipitationSpawnZone(-3200, -2500, 6400, 0, 0, 0, 0.7, 0.9, 120, new Clr(192, 192, 192, 128), 1, 25, null));
		} else if (o.optBoolean("snow", false) && !o.has("snowing")) {
			precipitationSpawnZones.add(new PrecipitationSpawnZone(-3200, -2500, 6400, 0, 0, 0, 0.1, 0.3, 1000, Clr.WHITE, 1.5, 1.5, ParticleType.ofName("ground_snow")));
		} else if (o.has("precipitationSpawnZones")) {
			JSONArray a = o.getJSONArray("precipitationSpawnZones");
			for (int i = 0; i < a.length(); i++) {
				precipitationSpawnZones.add(new PrecipitationSpawnZone(a.getJSONObject(i)));
			}
		}
		if (o.has("backgroundLoop")) {
			backgroundLoop = new SoundEffect(o.getJSONObject("backgroundLoop"), true);
		} else {
			backgroundLoop = null;
		}
		groundImpactParticle = ParticleType.ofName(o.getString("groundImpactParticle"));
		numGroundImpactParticles = o.getInt("numGroundImpactParticles");
		if (o.has("icon")) {
			icon = new Appearance(o.getJSONObject("icon"));
		} else {
			icon = null;
		}
		if (o.has("icon2")) {
			icon2 = new Appearance(o.getJSONObject("icon2"));
		} else {
			icon2 = null;
		}
		if (o.optBoolean("rain", false)) {
			particleDripSpeed = 0.0005;
			occasionalSound = new SoundEffect("thunder", 7, 2.5);
			occasionalSoundChance = 1.0 / 1000 / 16;
		} else {
			particleDripSpeed = o.optDouble("particleDripSpeed", 0.0002);
			if (o.has("occasionalSound")) {
				occasionalSound = new SoundEffect(o.getJSONObject("occasionalSound"));
				occasionalSoundChance = o.optDouble("occasionalSoundChance", 1.0 / 1000 / 16);
			} else {
				occasionalSound = null;
				occasionalSoundChance = 0;
			}
		}
	}
	
	public String getEffectTooltip() {
		return _t("weather_" + name + "_tooltip");
	}
	
	public static WeatherEffect ofName(String name) {
		return ofName(WeatherEffect.class, name);
	}
}
