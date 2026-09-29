package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;
import org.json.JSONObject;

public strictfp class ParticleType extends Loadable {
	public final boolean directional;
	public final ParticlePictureType pt;
	public final boolean lit;
	public final int maxSize;
	public final boolean dissolveWhenStuck;
	public final double stickSpeed;
	public final double grav;
	public final double windMult;
	public final double offset;
	public final double minDx;
	public final double maxDx;
	public final double minDy;
	public final double maxDy;
	public final int minLifespan;
	public final int maxLifespan;
	public final int startSize;
	public final int endSize;
	public final Clr startClr;
	public final Clr earlyClr;
	public final Clr endClr;
	public final Clr tintStartClr;
	public final Clr tintEndClr;
	public final float[][] gradient = new float[30][4];
	public final Clr[] tintGradient = new Clr[30];
	public final Clr lightClr;
	public final int lightRadius;
	public final double haloStrength;
	public final boolean strongLight;
	public final boolean accumulates;
	
	public ParticleType(JSONObject o) {
		super(o.getString("name"));
		this.pt = o.has("pictureType") ? ParticlePictureType.ofName(o.getString("pictureType")) : null;
		this.directional = o.optBoolean("directional", false);
		this.lit = o.optBoolean("lit", false);
		this.stickSpeed = o.optDouble("stickSpeed", 0);
		this.dissolveWhenStuck = o.optBoolean("dissolveWhenStuck", false);
		this.grav = o.getDouble("grav");
		this.windMult = o.optDouble("windMult", 1);
		this.offset = o.getDouble("offset");
		this.minDx = o.getDouble("minDx");
		this.maxDx = o.getDouble("maxDx");
		this.minDy = o.getDouble("minDy");
		this.maxDy = o.getDouble("maxDy");
		this.minLifespan = StrictMath.max(10, o.getInt("minLifespan"));
		this.maxLifespan = StrictMath.max(10, o.getInt("maxLifespan"));
		this.startSize = o.getInt("startSize");
		this.endSize = o.getInt("endSize");
		this.startClr = o.has("startClr") ? new Clr(o.getJSONObject("startClr").getInt("r"), o.getJSONObject("startClr").getInt("g"), o.getJSONObject("startClr").getInt("b"), o.getJSONObject("startClr").getInt("a")) : null;
		this.earlyClr = o.has("earlyClr") ? new Clr(o.getJSONObject("earlyClr").getInt("r"), o.getJSONObject("earlyClr").getInt("g"), o.getJSONObject("earlyClr").getInt("b"), o.getJSONObject("earlyClr").getInt("a")) : null;
		this.endClr = o.has("endClr") ? new Clr(o.getJSONObject("endClr").getInt("r"), o.getJSONObject("endClr").getInt("g"), o.getJSONObject("endClr").getInt("b"), o.getJSONObject("endClr").getInt("a")) : null;
		this.tintStartClr = o.has("tintStartClr") ? new Clr(o.getJSONObject("tintStartClr").getInt("r"), o.getJSONObject("tintStartClr").getInt("g"), o.getJSONObject("tintStartClr").getInt("b"), o.getJSONObject("tintStartClr").getInt("a")) : null;
		this.tintEndClr = o.has("tintEndClr") ? new Clr(o.getJSONObject("tintEndClr").getInt("r"), o.getJSONObject("tintEndClr").getInt("g"), o.getJSONObject("tintEndClr").getInt("b"), o.getJSONObject("tintEndClr").getInt("a")) : null;
		this.haloStrength = o.optDouble("haloStrength", 1.0);
		if (earlyClr != null) {
			for (int i = 0; i < 3; i++) {
				Clr c = startClr.mix(1.0 * i / 5, earlyClr);
				gradient[i] = new float[] {c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f};
			}
			for (int i = 3; i < gradient.length; i++) {
				Clr c = earlyClr.mix(1.0 * (i - 3) / (gradient.length - 3), endClr);
				gradient[i] = new float[] {c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f};
			}
		} else {
			for (int i = 0; i < gradient.length; i++) {
				Clr c = startClr.mix(1.0 * i / gradient.length, endClr);
				gradient[i] = new float[] {c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f};
			}
		}
		if (tintStartClr != null) {
			for (int i = 0; i < tintGradient.length; i++) {
				tintGradient[i] = tintStartClr.mix(1.0 * i / tintGradient.length, tintEndClr);
			}
		}
		maxSize = StrictMath.max(startSize, endSize);
		this.lightClr = o.has("lightClr") ? new Clr(o.getJSONObject("lightClr").getInt("r"), o.getJSONObject("lightClr").getInt("g"), o.getJSONObject("lightClr").getInt("b"), o.getJSONObject("lightClr").optInt("a", 255)) : null;
		this.lightRadius = o.optInt("lightRadius", 0);
		this.strongLight = o.optBoolean("strongLight", false);
		this.accumulates = o.optBoolean("accumulates", false);
	}
	
	public static ParticleType ofName(String name) {
		return ofName(ParticleType.class, name);
	}
}
