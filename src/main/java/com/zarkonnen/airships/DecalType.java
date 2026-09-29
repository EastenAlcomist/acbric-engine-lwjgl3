package com.zarkonnen.airships;

import com.zarkonnen.airships.Tincture;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public strictfp class DecalType extends Loadable implements HasName {
	public final int w;
	public final int h;
	public final int imgW;
	public final boolean flipWithShip;
	public final ArrayList<TintedApp> apps = new ArrayList<TintedApp>();
	private final NameDetails nameDetails;
	public final ArmsDetails armsDetails;
	public final ChargeDetails chargeDetails;
	public final FlagSpec flag;
	public final int availableTextWidth;
	public Clr subColor;
	public Clr subBaseColor;
	public Clr[] subColorByPaintTypeIndex;
	public DecalType flipped, verticalFlipped;
	public String flippedName, verticalFlippedName;
	public boolean isHead;
	public ArrayList<DecalLightSource> lights = new ArrayList<DecalLightSource>();
	public final ArrayList<DecalCategory> categories = new ArrayList<DecalCategory>();
	public ArrayList<String> variantNames = new ArrayList<>();
	public ArrayList<DecalType> variants = new ArrayList<>();
	public DecalType variantGroupHead;
	public VariantType variantType;
	public ArrayList<ModuleType.ModuleParticleEmitter> emitters = new ArrayList<ModuleType.ModuleParticleEmitter>();
	
	public boolean canReplace(DecalType src) {
		return w <= src.w && h <= src.h && (canPlaceBelow() || !src.canPlaceBelow());
	}
	
	public static strictfp class DecalLightSource {
		public final Clr clr;
		public final int radius;
		public final double xOffset, yOffset;

		public DecalLightSource(Clr lightClr, int lightRadius, double lightX, double lightY) {
			this.clr = lightClr;
			this.radius = lightRadius;
			this.xOffset = lightX;
			this.yOffset = lightY;
		}
	}
	
	public DecalType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		w = o.getInt("w");
		h = o.getInt("h");
		availableTextWidth = o.optInt("availableTextWidth", w * AGame.SGS - 8);
		flippedName = o.optString("flippedVersion", null);
		verticalFlippedName = o.optString("verticallyFlippedVersion", null);
		if (o.has("variants")) {
			JSONArray a = o.getJSONArray("variants");
			for (int i = 0; i < a.length(); i++) {
				variantNames.add(a.getString(i));
			}
		}
		if (o.has("variantType")) {
			variantType = VariantType.ofName(o.getString("variantType"));
		} else {
			variantType = VariantType.getDefault();
		}
		if (o.has("categories")) {
			JSONArray cats = o.getJSONArray("categories");
			for (int i = 0; i < cats.length(); i++) {
				categories.add(DecalCategory.ofName(cats.getString(i)));
			}
		}
		if (o.has("appearances")) {
			JSONArray a = o.getJSONArray("appearances");
			for (int i = 0; i < a.length(); i++) {
				apps.add(new TintedApp(a.getJSONObject(i)));
			}
		}
		flipWithShip = o.optBoolean("flipWithShip", false);
		nameDetails = o.has("shipName") ? new NameDetails(o.getJSONObject("shipName")) : null;
		armsDetails = o.has("arms") ? new ArmsDetails(o.getJSONObject("arms")) : null;
		chargeDetails = o.has("charge") ? new ChargeDetails(o.getJSONObject("charge")) : null;
		if (o.has("flag")) {
			flag = new FlagSpec(o.getJSONObject("flag"));
		} else {
			flag = null;
		}
		
		if (o.has("subColor")) {
			JSONObject co = o.getJSONObject("subColor");
			this.subColor = new Clr(co.getInt("r"), co.getInt("g"), co.getInt("b"));
			if (o.has("subBaseColor")) {
				JSONObject bco = o.getJSONObject("subBaseColor");
				this.subBaseColor = new Clr(bco.getInt("r"), bco.getInt("g"), bco.getInt("b"));
			} else {
				this.subBaseColor = subColor;
			}
			ArrayList<PaintType> ps = PaintType.values();
			subColorByPaintTypeIndex = new Clr[ps.size()];
			for (int i = 0; i < ps.size(); i++) {
				Clr tc = ps.get(i).getBaseTint();
				subColorByPaintTypeIndex[i] = new Clr(
						subBaseColor.r * (255 - tc.a) / 255 + tc.r * tc.a / 255,
						subBaseColor.g * (255 - tc.a) / 255 + tc.g * tc.a / 255,
						subBaseColor.b * (255 - tc.a) / 255 + tc.b * tc.a / 255
				);
			}
		}
		if (!apps.isEmpty() && !apps.get(0).app.frames.isEmpty()) {
			imgW = apps.get(0).app.frames.get(0).srcWidth / AGame.SGS;
		} else {
			imgW = w;
		}
		
		if (o.has("lights")) {
			JSONArray a = o.getJSONArray("lights");
			for (int i = 0; i < a.length(); i++) {
				JSONObject l = a.getJSONObject(i);
				lights.add(new DecalLightSource(
						new Clr(l.getInt("r"), l.getInt("g"), l.getInt("b")),
						l.getInt("radius"),
						l.getDouble("x"),
						l.getDouble("y")));
			}
		}
		if (o.has("emitters")) {
			JSONArray a = o.getJSONArray("emitters");
			for (int i = 0; i < a.length(); i++) {
				JSONObject em = a.getJSONObject(i);
				SoundEffect ef = null;
				if (em.has("sound")) {
					try {
						String sound = em.getString("sound");
						ef = new SoundEffect(sound, em.optDouble("volume"));
					} catch (Exception e) {
						ef = new SoundEffect(em.getJSONObject("sound"));
					}
				}
				emitters.add(new ModuleType.ModuleParticleEmitter(
						em.getDouble("x"),
						em.getDouble("y"),
						em.optBoolean("inside", false),
						ParticleType.ofName(em.getString("type")),
						em.getDouble("emitProbability"),
						em.optInt("numParticles", 1),
						ef
				));
			}
		}
	}
	
	public static void postLoad() {
		for (DecalType dt : all(DecalType.class)) {
			if (dt.flippedName != null && hasOfName(DecalType.class, dt.flippedName)) {
				DecalType dt2 = DecalType.ofName(dt.flippedName);
				dt.flipped = dt2;
				dt2.flipped = dt;
			}
			if (dt.verticalFlippedName != null && hasOfName(DecalType.class, dt.verticalFlippedName)) {
				DecalType dt2 = DecalType.ofName(dt.verticalFlippedName);
				dt.verticalFlipped = dt2;
				dt2.verticalFlipped = dt;
			}
		}
		for (DecalType dt : all(DecalType.class)) {
			if (dt.flipped != null && dt.flipped.verticalFlipped != null && dt.flipped.verticalFlipped.flipped != null) {
				dt.verticalFlipped = dt.flipped.verticalFlipped.flipped;
			}
		}
		for (DecalType dt : all(DecalType.class)) {
			if (dt.flippedName != null) {
				dt.isHead = dt.verticalFlippedName != null || dt.verticalFlipped == null;
			} else {
				dt.isHead = dt.flipped == null && dt.verticalFlippedName != null;
			}
		}
		for (DecalType dt : all(DecalType.class)) {
			if (!dt.variantNames.isEmpty()) {
				dt.variants.add(dt);
				dt.variantGroupHead = dt;
				for (String n : dt.variantNames) {
					DecalType dt2 = DecalType.ofName(n);
					dt.variants.add(dt2);
					dt2.variantGroupHead = dt;
				}
			}
		}
	}
	
	public boolean isSymmetryGroupHead() {
		return isHead;
	}
	
	public boolean isSymmetryGroupMember() {
		return flipped != null || verticalFlipped != null;
	}
	
	public DecalType getSymmetryGroupHead() {
		if (isHead) { return this; }
		if (flipped != null) {
			if (flipped.isHead) {
				return flipped;
			}
			if (flipped.verticalFlipped != null && flipped.verticalFlipped.isHead) {
				return flipped.verticalFlipped;
			}
		}
		if (verticalFlipped != null && verticalFlipped.isHead) {
			return verticalFlipped;
		}
		return this;
	}
	
	public boolean isVariantGroupMember() {
		return variantGroupHead != null;
	}
	
	public boolean isVariantGroupHead() {
		return variantGroupHead == this;
	}
	
	public DecalType getVariantGroupHeadOrThis() {
		return variantGroupHead == null ? this : variantGroupHead;
	}
	
	public ArrayList<DecalType> getVariants() {
		return variants;
	}
	
	public boolean hasColoration() {
		return subColor != null;
	}
	
	public Clr colorationDefault() {
		return subColor;
	}
	
	public boolean canPlaceBelow() {
		return true;
	}
	
	public boolean in(DecalCategory mc) {
		return categories.contains(mc);
	}
	
	public static DecalType ofName(String name) {
		return ofName(DecalType.class, name);
	}
	
	static void pixelateStencil() {
		for (Img img : AGame.STENCIL.imgs) {
			if (img != null && img.machineImgCache != null) {
				((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
		}
		for (Img img : AGame.STENCIL.extended.values()) {
			if (img != null && img.machineImgCache != null) {
				((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
		}
	}

	public static final Clr STENCIL = Clr.fromHex("000000cc");
	private static enum ArmsTint {
		ARMS0, ARMS1, ARMS2
	}
	
	public static strictfp class TintedApp {
		public final ArmsTint tint;
		public final int x, y;
		public final Appearance app;
		private TintedApp(JSONObject o) {
			tint = o.has("tint") ? ArmsTint.valueOf(o.getString("tint")) : null;
			x = o.optInt("x", 0);
			y = o.optInt("y", 0);
			app = new Appearance(o.getJSONObject("appearance"));
		}
	}
	
	private static enum NameAlign {
		LEFT, CENTER;
	}
	
	private static strictfp class NameDetails {
		public final int x, y;
		public final NameAlign align;
		private NameDetails(JSONObject o) {
			x = o.optInt("x", 0);
			y = o.optInt("y", 0);
			align = o.has("align") ? NameAlign.valueOf(o.getString("align")) : NameAlign.LEFT;
		}
	}
	
	public static strictfp class ArmsDetails {
		public final int x, y, size;
		public ArmsDetails(JSONObject o) {
			x = o.optInt("x", 0);
			y = o.optInt("y", 0);
			size = o.getInt("size");
		}
	}
	
	public static strictfp class ChargeDetails {
		public final int x, y, w, h;
		public final ArmsTint tint;
		public final Tincture tincture;
		public ChargeDetails(JSONObject o) {
			x = o.optInt("x", 0);
			y = o.optInt("y", 0);
			w = o.getInt("w");
			h = o.getInt("h");
			tint = o.has("tint") ? ArmsTint.valueOf(o.getString("tint")) : null;
			tincture = o.has("tincture") ? Tincture.valueOf(o.getString("tincture")) : null;
		}
	}
	
	public void drawAsBlueprint(Draw d, double x, double y, int ms, boolean flipped, float intensity) {
		int appSz = apps.size();
		for (int appI = 0; appI < appSz; appI++) {
			TintedApp ta = apps.get(appI);
			ta.app.drawAsBlueprint(d, x + ta.x * (flipped ? -1 : 1), y + ta.y, ms, flipped, intensity);
		}
		if (armsDetails != null) {
			d.rect(Clr.WHITE, x + armsDetails.x, y + armsDetails.y, armsDetails.size, armsDetails.size);
		}
		if (chargeDetails != null) {
			d.rect(Clr.WHITE, x + chargeDetails.x, y + chargeDetails.y, chargeDetails.w, chargeDetails.h);
		}
	}
	
	public boolean drawAsOutline(Draw d, double x, double y, int ms, boolean flipped, Clr c) {
		boolean appExceedsSize = false;
		int appSz = apps.size();
		for (int appI = 0; appI < appSz; appI++) {
			TintedApp ta = apps.get(appI);
			ta.app.drawAsOutline(d, x + ta.x * (flipped ? -1 : 1), y + ta.y, 0, 0, ms, flipped, c);
			appExceedsSize = appExceedsSize || ta.x < 0 || ta.y < 0 || ta.x + ta.app.width() > w || ta.y + ta.app.height() > h;
		}
		if (armsDetails != null) {
			d.rect(c, x + armsDetails.x, y + armsDetails.y, armsDetails.size, 1);
			d.rect(c, x + armsDetails.x, y + armsDetails.y + armsDetails.size - 1, armsDetails.size, 1);
			d.rect(c, x + armsDetails.x, y + armsDetails.y, 1, armsDetails.size);
			d.rect(c, x + armsDetails.x + armsDetails.size - 1, y + armsDetails.y, 1, armsDetails.size);
		}
		if (chargeDetails != null) {
			d.rect(c, x + chargeDetails.x, y + chargeDetails.y, chargeDetails.w, 1);
			d.rect(c, x + chargeDetails.x, y + chargeDetails.y + chargeDetails.h - 1, chargeDetails.w, 1);
			d.rect(c, x + chargeDetails.x, y + chargeDetails.y, 1, chargeDetails.h);
			d.rect(c, x + chargeDetails.x + chargeDetails.w - 1, y + chargeDetails.y, 1, chargeDetails.h);
		}
		return appExceedsSize;
	}
	
	public void drawBase(MyDraw d, double x, double y, int ms, boolean flipped, CoatOfArms coa, String name, Image[] light, float lightStrength, Color ambient, float ambientSaturation,
			SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, PaintType subPaint)
	{
		int appSz = apps.size();
		for (int appI = 0; appI < appSz; appI++) {
			TintedApp ta = apps.get(appI);
			Clr tint = null;
			float ls = lightStrength;
			if (ta.tint != null) {
				Tincture tinc = coa.getRoundelTinctures()[ta.tint.ordinal()];
				tint = tinc.tint;
				if (tinc.metal) {
					ls *= 5;
				}
			}
			if (ta.app.spritesheetBundle == ssb || ssb == null) {
				Clr subTrg = null;
				if (subColor != null) {
					subTrg = subPaint == null ? subColor : subColorByPaintTypeIndex[subPaint.ordinal()];
				}
				float shiny = subPaint == null ? 0.36f : subPaint.shinyOrPassthrough();
				ta.app.drawSub(d,
						x + ta.x * (flipped ? -1 : 1), y + ta.y,
						0, 0,
						ms,
						tint,
						flipWithShip ? flipped : false,
						light,
						ls,
						ambient,
						ambientSaturation,
						subColor, subTrg, shiny);
			} else if (additionalSSBs != null) {
				additionalSSBs.add(ta.app.spritesheetBundle);
			}
		}
	}
	
	public void drawArmsBase(MyDraw d, double x, double y, int ms, boolean flipped, CoatOfArms coa, String name, Image[] light, float lightStrength, Color ambient, float ambientSaturation,
			SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs)
	{
		if (armsDetails != null && (ssb == null || ssb.name.equals("heraldry"))) {
			coa.drawBase(d, x + armsDetails.x, y + armsDetails.y, armsDetails.size, light, lightStrength, ambient, ambientSaturation);
		}
	}
	
	public void drawCharges(MyDraw d, double x, double y, int ms, boolean flipped, CoatOfArms coa, String name, Image[] light, float lightStrength, Color ambient, float ambientSaturation,
			SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs)
	{
		if (armsDetails != null) {
			coa.drawCharges(d, x + armsDetails.x, y + armsDetails.y, armsDetails.size, light, lightStrength, ambient, ambientSaturation, ssb, additionalSSBs);
		}
		if (chargeDetails != null && coa.charge[0] != null) {
			Tincture tinc = chargeDetails.tint == null ? chargeDetails.tincture : coa.getRoundelTinctures()[chargeDetails.tint.ordinal()];
			float ls = lightStrength;
			if (tinc.metal) {
				ls *= 5;
			}
			if (coa.charge[0].app.spritesheetBundle == ssb || ssb == null) {
				coa.charge[0].app.draw(d, x + chargeDetails.x, y + chargeDetails.y, chargeDetails.w, chargeDetails.h, 0, tinc.tint, false, light, ls, ambient, ambientSaturation);
			} else if (additionalSSBs != null) {
				additionalSSBs.add(coa.charge[0].app.spritesheetBundle);
			}
		}
	}
	
	public void drawUnlitCharges(MyDraw d, double x, double y, int ms, boolean flipped, CoatOfArms coa)
	{
		if (armsDetails != null) {
			coa.drawUnlitCharges(d, x + armsDetails.x, y + armsDetails.y, armsDetails.size);
		}
	}
	
	public void drawNonShader(MyDraw d, double x, double y, int ms, boolean flipped, CoatOfArms coa, String name, Clr tint) {
		if (chargeDetails != null && coa.specialCharge[0] != null) {
			Tincture tinc = chargeDetails.tint == null ? chargeDetails.tincture : coa.getRoundelTinctures()[chargeDetails.tint.ordinal()];
			d.drawSpecialCharge(coa.specialCharge[0], tinc.tint.mult(tint), x + chargeDetails.x, y + chargeDetails.y, chargeDetails.w, chargeDetails.h);
		}
		if (nameDetails != null) {
			name = name.toUpperCase().replaceAll("\\[|\\]|\\{|\\}", "");
			pixelateStencil();
			double textX = x + nameDetails.x;
			double textY = y + nameDetails.y;
			name = name.toUpperCase().trim();
			int textW = (int) d.textSize(name, AGame.STENCIL).x;
			if (textW > availableTextWidth) {
				while (name.contains(" ") && textW > availableTextWidth) {
					name = name.substring(0, name.lastIndexOf(" "));
					textW = (int) d.textSize(name, AGame.STENCIL).x;
				}
				while (!name.isEmpty()  && textW > availableTextWidth) {
					name = name.substring(0, name.length() - 1);
					textW = (int) d.textSize(name, AGame.STENCIL).x;
				}
			}
			if (nameDetails.align == NameAlign.CENTER) {
				textX -= textW / 2;
			}
			d.text("[000000f7]" + name.toUpperCase(), AGame.STENCIL, textX, textY);
		}
	}
	
	@Override
	public String getName() {
		return _t("decal_" + name);
	}
}
