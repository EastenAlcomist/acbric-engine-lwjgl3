package com.zarkonnen.airships;

import com.zarkonnen.airships.CoatOfArms.TinctureSlot;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public strictfp class ArmsLayout extends Loadable {
	public final int charges;
	public final int tinctures;
	public final String detailPrefix;
	public final EnumMap<CoatOfArms.TinctureSlot, EnumSet<CoatOfArms.TinctureSlot>> adjacency = new EnumMap<CoatOfArms.TinctureSlot, EnumSet<CoatOfArms.TinctureSlot>>(CoatOfArms.TinctureSlot.class);
	public final EnumMap<CoatOfArms.TinctureSlot, EnumSet<CoatOfArms.TinctureSlot>> onTop = new EnumMap<CoatOfArms.TinctureSlot, EnumSet<CoatOfArms.TinctureSlot>>(CoatOfArms.TinctureSlot.class);
	public final ArrayList<TincturedApp> apps = new ArrayList<TincturedApp>();
	public final ArrayList<TincturedApp> unlitApps = new ArrayList<TincturedApp>();
	public final ArrayList<TincturedImg> shieldImages = new ArrayList<TincturedImg>();
	public final Img shieldOutline;
	public final ArrayList<ChargeLocation> chargeLocations = new ArrayList<ChargeLocation>();
	public final ArrayList<ChargeLocation> shieldChargeLocations = new ArrayList<ChargeLocation>();
	public final ArrayList<String> addToHeraldicStyles = new ArrayList<String>();
	public static class ChargeLocation {
		final int x, y, w, h;
		public final CoatOfArms.TinctureSlot tincture;
		public final boolean leftOnly, rightOnly, topOnly, bottomOnly;
		public ChargeLocation(JSONObject o, TinctureSlot defaultTS) {
			x = o.getInt("x");
			y = o.getInt("y");
			w = o.getInt("w");
			h = o.getInt("h");
			leftOnly = o.optBoolean("leftOnly", false);
			rightOnly = o.optBoolean("rightOnly", false);
			topOnly = o.optBoolean("topOnly", false);
			bottomOnly = o.optBoolean("bottomOnly", false);
			tincture = o.has("tincture") ? TinctureSlot.valueOf(o.getString("tincture")) : defaultTS;
		}
	}
	public static class TincturedApp {
		public final Appearance app;
		public final int x, y, w, h;
		public final CoatOfArms.TinctureSlot tincture;
		public TincturedApp(JSONObject o) {
			app = new Appearance(o.getJSONObject("app"));
			x = o.optInt("x", 0);
			y = o.optInt("y", 0);
			w = o.optInt("w", 32);
			h = o.optInt("h", 32);
			tincture = TinctureSlot.valueOf(o.getString("tincture"));
		}
	}
	public static class TincturedImg {
		public final Img img;
		public final int dx, dy;
		public final CoatOfArms.TinctureSlot tincture;
		public TincturedImg(JSONObject o) {
			img = new Img(o.getString("src"), o.getInt("x"), o.getInt("y"), o.getInt("w"), o.getInt("h"), o.optBoolean("flipped", false));
			dx = o.optInt("dx", 0);
			dy = o.optInt("dy", 0);
			tincture = TinctureSlot.valueOf(o.getString("tincture"));
		}
	}
	
	public ArmsLayout(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		tinctures = o.getInt("tinctures");
		detailPrefix = o.optString("detailPrefix", null);
		JSONArray a;
		if (o.has("charges")) {
			a = o.getJSONArray("charges");
			charges = o.optInt("numCharges", a.length());
			JSONArray a2 = o.getJSONArray("shieldCharges");
			for (int i = 0; i < a.length(); i++) {
				chargeLocations.add(new ChargeLocation(a.getJSONObject(i), TinctureSlot.chargeTOf(i)));
				shieldChargeLocations.add(new ChargeLocation(a2.getJSONObject(i), TinctureSlot.chargeTOf(i)));
			}
		} else {
			charges = 0;
		}
		if (o.has("adjacent")) {
			a = o.getJSONArray("adjacent");
			for (int i = 0; i < a.length(); i += 2) {
				adjacent(CoatOfArms.TinctureSlot.valueOf(a.getString(i)), CoatOfArms.TinctureSlot.valueOf(a.getString(i + 1)));
			}
		}
		if (o.has("onTop")) {
			a = o.getJSONArray("onTop");
			for (int i = 0; i < a.length(); i += 2) {
				onTop(CoatOfArms.TinctureSlot.valueOf(a.getString(i)), CoatOfArms.TinctureSlot.valueOf(a.getString(i + 1)));
			}
		}
		if (o.has("apps")) {
			a = o.getJSONArray("apps");
			for (int i = 0; i < a.length(); i++) {
				apps.add(new TincturedApp(a.getJSONObject(i)));
			}
		}
		if (o.has("unlitApps")) {
			a = o.getJSONArray("unlitApps");
			for (int i = 0; i < a.length(); i++) {
				unlitApps.add(new TincturedApp(a.getJSONObject(i)));
			}
		}
		if (o.has("shieldImages")) {
			a = o.getJSONArray("shieldImages");
			for (int i = 0; i < a.length(); i++) {
				shieldImages.add(new TincturedImg(a.getJSONObject(i)));
			}
		}
		if (o.has("shieldOutline")) {
			JSONObject io = o.getJSONObject("shieldOutline");
			shieldOutline = new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.getInt("w"), io.getInt("h"), io.optBoolean("flipped", false));
		} else {
			shieldOutline = null;
		}
		if (o.has("addToHeraldicStyles")) {
			a = o.getJSONArray("addToHeraldicStyles");
			for (int i = 0; i < a.length(); i++) {
				addToHeraldicStyles.add(a.getString(i));
			}
		}
	}
	
	private void adjacent(CoatOfArms.TinctureSlot a, CoatOfArms.TinctureSlot b) {
		if (!adjacency.containsKey(a)) {
			adjacency.put(a, EnumSet.of(b));
		} else {
			adjacency.get(a).add(b);
		}
		if (!adjacency.containsKey(b)) {
			adjacency.put(b, EnumSet.of(a));
		} else {
			adjacency.get(b).add(a);
		}
	}

	private void onTop(CoatOfArms.TinctureSlot a, CoatOfArms.TinctureSlot b) {
		if (!onTop.containsKey(a)) {
			onTop.put(a, EnumSet.of(b));
		} else {
			onTop.get(a).add(b);
		}
		if (!onTop.containsKey(b)) {
			onTop.put(b, EnumSet.of(a));
		} else {
			onTop.get(b).add(a);
		}
	}
	
	
	public void draw(CoatOfArms coa, MyDraw d, double x, double y, double size, CoatOfArms.TinctureSlot highlight) {
		drawUnlit(coa, d, x, y, size, highlight);
	}

	public void drawBase(CoatOfArms coa, MyDraw d, double x, double y, double size, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		for (int i = 0; i < apps.size(); i++) {
			TincturedApp ta = apps.get(i);
			ta.app.draw(d, x + ta.x * size / 32, y + ta.y * size / 32, ta.w * size / 32, ta.h * size / 32, 0, CoatOfArms.tinc(d, coa, ta.tincture, null), false, light, CoatOfArms.li(coa, ta.tincture, lightStrength), ambient, ambientSaturation);
		}
	}

	public void drawCharges(CoatOfArms coa, MyDraw d, double x, double y, double size, Image[] light, float lightStrength, Color ambient, float ambientSaturation, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs) {
		for (int i = 0; i < chargeLocations.size(); i++) {
			int chargeI = Math.min(charges - 1, i);
			if (coa.charge[chargeI] != null) {
				TincturedApp override = coa.charge[chargeI].getOverride(this, i);
				if (override != null) {
					if (override.app.spritesheetBundle == ssb || ssb == null) {
						override.app.draw(d, x + override.x * size / 32, y + override.y * size / 32, override.w * size / 32, override.h * size / 32, 0, CoatOfArms.tinc(d, coa, override.tincture, null), false, light, CoatOfArms.li(coa, override.tincture, lightStrength), ambient, ambientSaturation);
					} else if (additionalSSBs != null) {
						additionalSSBs.add(override.app.spritesheetBundle);
					}
				} else if (coa.charge[chargeI].app.spritesheetBundle == ssb || ssb == null) {
					ChargeLocation loc = chargeLocations.get(i);
					if (loc.leftOnly) {
						if (loc.topOnly) {
							coa.charge[chargeI].topLeftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
						} else if (loc.bottomOnly) {
							coa.charge[chargeI].bottomLeftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
						} else {
							coa.charge[chargeI].leftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
						}
					} else if (loc.rightOnly) {
						if (loc.topOnly) {
							coa.charge[chargeI].topRightApp.draw(d, x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
						} else if (loc.bottomOnly) {
							coa.charge[chargeI].bottomRightApp.draw(d, x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
						} else {
							coa.charge[chargeI].rightApp.draw(d, x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
						}
					} else if (loc.topOnly) {
						coa.charge[chargeI].leftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
					} else if (loc.bottomOnly) {
						coa.charge[chargeI].bottomApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
					} else {
						coa.charge[chargeI].app.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32, 0, CoatOfArms.tinc(d, coa, loc.tincture, null), false, light, CoatOfArms.li(coa, loc.tincture, lightStrength), ambient, ambientSaturation);
					}
				} else if (additionalSSBs != null) {
					additionalSSBs.add(coa.charge[chargeI].app.spritesheetBundle);
				}
			}
		}
	}

	public void drawUnlitCharges(CoatOfArms coa, MyDraw d, double x, double y, double size) {
		for (int i = 0; i < chargeLocations.size(); i++) {
			int chargeI = Math.min(charges - 1, i);
			if (coa.charge[chargeI] == null) {
				ChargeLocation loc = chargeLocations.get(i);
				if (loc.leftOnly) {
					if (loc.topOnly) {
						d.drawSpecialChargeTopLeft(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else if (loc.bottomOnly) {
						d.drawSpecialChargeBottomLeft(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else {
						d.drawSpecialChargeLeft(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32);
					}
				} else if (loc.rightOnly) {
					if (loc.topOnly) {
						d.drawSpecialChargeTopRight(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else if (loc.bottomOnly) {
						d.drawSpecialChargeBottomRight(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else {
						d.drawSpecialChargeRight(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32);
					}
				} else if (loc.topOnly) {
					d.drawSpecialChargeTop(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32 / 2);
				} else if (loc.bottomOnly) {
					d.drawSpecialChargeBottom(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32, loc.h * size / 32 / 2);
				} else {
					d.drawSpecialCharge(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32);
				}
			}
		}
	}
	
	public void drawUnlit(CoatOfArms coa, MyDraw d, double x, double y, double size, CoatOfArms.TinctureSlot highlight) {
		for (int i = 0; i < unlitApps.size(); i++) {
			TincturedApp ta = unlitApps.get(i);
			ta.app.draw(d, x + ta.x * size / 32, y + ta.y * size / 32, ta.w * size / 32, ta.h * size / 32, 0, CoatOfArms.tinc(d, coa, ta.tincture, highlight), false);
		}
		for (int i = 0; i < chargeLocations.size(); i++) {
			int chargeI = Math.min(charges - 1, i);
			ChargeLocation loc = chargeLocations.get(i);
			if (coa.charge[chargeI] == null) {
				if (loc.leftOnly) {
					if (loc.topOnly) {
						d.drawSpecialChargeTopLeft(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else if (loc.bottomOnly) {
						d.drawSpecialChargeBottomLeft(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else {
						d.drawSpecialChargeLeft(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32);
					}
				} else if (loc.rightOnly) {
					if (loc.topOnly) {
						d.drawSpecialChargeTopRight(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else if (loc.bottomOnly) {
						d.drawSpecialChargeBottomRight(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2);
					} else {
						d.drawSpecialChargeRight(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32);
					}
				} else if (loc.topOnly) {
					d.drawSpecialChargeTop(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32 / 2);
				} else if (loc.bottomOnly) {
					d.drawSpecialChargeBottom(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32, loc.h * size / 32 / 2);
				} else {
					d.drawSpecialCharge(coa.specialCharge[chargeI], CoatOfArms.tinc(d, coa, loc.tincture, null), x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32);
				}
			} else {
				TincturedApp override = coa.charge[chargeI].getUnlitOverride(this, i);
				if (override != null) {
					override.app.draw(d, x + override.x * size / 32, y + override.y * size / 32, override.w * size / 32, override.h * size / 32, 0, CoatOfArms.tinc(d, coa, override.tincture, highlight), false);
				} else if (loc.leftOnly) {
					if (loc.topOnly) {
						coa.charge[chargeI].unlitTopLeftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
					} else if (loc.bottomOnly) {
						coa.charge[chargeI].unlitBottomLeftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
					} else {
						coa.charge[chargeI].unlitLeftApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
					}
				} else if (loc.rightOnly) {
					if (loc.topOnly) {
						coa.charge[chargeI].unlitTopRightApp.draw(d, x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
					} else if (loc.bottomOnly) {
						coa.charge[chargeI].unlitBottomRightApp.draw(d, x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32 / 2, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
					} else {
						coa.charge[chargeI].unlitRightApp.draw(d, x + loc.x * size / 32 + loc.w * size / 32 / 2, y + loc.y * size / 32, loc.w * size / 32 / 2, loc.h * size / 32, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
					}
				} else if (loc.topOnly) {
					coa.charge[chargeI].unlitTopApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
				} else if (loc.bottomOnly) {
					coa.charge[chargeI].unlitBottomApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32 + loc.h * size / 32 / 2, loc.w * size / 32, loc.h * size / 32 / 2, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
				} else {
					coa.charge[chargeI].unlitApp.draw(d, x + loc.x * size / 32, y + loc.y * size / 32, loc.w * size / 32, loc.h * size / 32, 0, CoatOfArms.tinc(d, coa, loc.tincture, highlight), false);
				}
			}
		}
	}

	public void drawLayout(MyDraw d, double x, double y) {
		CoatOfArms coa = new CoatOfArms();
		coa.setLayout(this);
		/*for (int i = 0; i < 4; i++) {
			if (!coa.tincture[i].name.equals("ARGENT") && !coa.tincture[i].name.equals("SABLE")) {
				coa.tincture[i] = Tincture.valueOf("CENDREE");
			}
			if (!coa.chargeT[i].name.equals("ARGENT") && !coa.chargeT[i].name.equals("SABLE")) {
				coa.chargeT[i] = Tincture.valueOf("CENDREE");
			}
		}*/ // Do we need this? It somehow contaminates everything else??
		drawUnlit(coa, d, x, y, 16, null);
	}

	public void drawShield(CoatOfArms coa, MyDraw d, double x, double y, double scale, Clr outlineTint) {
		for (int i = 0; i < shieldImages.size(); i++) {
			TincturedImg ta = shieldImages.get(i);
			if (ta.img.machineImgCache != null) {
				((Image) ta.img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			d.blit(ta.img, ta.tincture.get(coa).tint, x + ta.dx * scale, y + ta.dy * scale, ta.img.srcWidth * scale, ta.img.srcHeight * scale);
		}
		for (int i = 0; i < chargeLocations.size(); i++) {
			int chargeI = Math.min(charges - 1, i);
			ChargeLocation loc = shieldChargeLocations.get(i);
			//System.out.println(loc.x + " / " + loc.y + " " + loc.tincture.name() + " chargeI " + chargeI);
			if (coa.charge[chargeI] == null) {
				if (loc.leftOnly) {
					if (loc.topOnly) {
						d.drawSpecialChargeTopLeft(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale, y + loc.y * scale, loc.w * scale / 2, loc.h * scale / 2);
					} else if (loc.bottomOnly) {
						d.drawSpecialChargeBottomLeft(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale, y + loc.y * scale + loc.h * scale / 2, loc.w * scale / 2, loc.h * scale / 2);
					} else {
						d.drawSpecialChargeLeft(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale, y + loc.y * scale, loc.w * scale / 2, loc.h * scale);
					}
				} else if (loc.rightOnly) {
					if (loc.topOnly) {
						d.drawSpecialChargeTopRight(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale + loc.w * scale / 2, y + loc.y * scale, loc.w * scale / 2, loc.h * scale / 2);
					} else if (loc.bottomOnly) {
						d.drawSpecialChargeBottomRight(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale + loc.w * scale / 2, y + loc.y * scale + loc.h * scale / 2, loc.w * scale / 2, loc.h * scale / 2);
					} else {
						d.drawSpecialChargeRight(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale + loc.w * scale / 2, y + loc.y * scale, loc.w * scale / 2, loc.h * scale);
					}
				} else if (loc.topOnly) {
					d.drawSpecialChargeTop(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale, y + loc.y * scale, loc.w * scale, loc.h * scale / 2);
				} else if (loc.bottomOnly) {
					d.drawSpecialChargeBottom(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale, y + loc.y * scale + loc.h * scale / 2, loc.w * scale, loc.h * scale / 2);
				} else {
					d.drawSpecialCharge(coa.specialCharge[chargeI], loc.tincture.get(coa).tint, x + loc.x * scale, y + loc.y * scale, loc.w * scale, loc.h * scale);
				}
			} else {
				TincturedImg override = coa.charge[chargeI].getShieldOverride(this, i);
				if (override != null) {
					if (override.img.machineImgCache != null) {
						((Image) override.img.machineImgCache).setFilter(Image.FILTER_NEAREST);
					}
					d.blit(override.img, override.tincture.get(coa).tint, x + override.dx * scale, y + override.dy * scale, override.img.srcWidth * scale, override.img.srcHeight * scale);
				} else if (loc.leftOnly) {
					if (loc.topOnly) {
						coa.charge[chargeI].unlitTopLeftApp.draw(d, x + loc.x * scale, y + loc.y * scale, loc.w * scale / 2, loc.h * scale / 2, 0, loc.tincture.get(coa).tint, false);
					} else if (loc.bottomOnly) {
						coa.charge[chargeI].unlitBottomLeftApp.draw(d, x + loc.x * scale, y + loc.y * scale + loc.h * scale / 2, loc.w * scale / 2, loc.h * scale / 2, 0, loc.tincture.get(coa).tint, false);
					} else {
						coa.charge[chargeI].unlitLeftApp.draw(d, x + loc.x * scale, y + loc.y * scale, loc.w * scale / 2, loc.h * scale, 0, loc.tincture.get(coa).tint, false);
					}
				} else if (loc.rightOnly) {
					if (loc.topOnly) {
						coa.charge[chargeI].unlitTopRightApp.draw(d, x + loc.x * scale + loc.w * scale / 2, y + loc.y * scale, loc.w * scale / 2, loc.h * scale / 2, 0, loc.tincture.get(coa).tint, false);
					} else if (loc.bottomOnly) {
						coa.charge[chargeI].unlitBottomRightApp.draw(d, x + loc.x * scale + loc.w * scale / 2, y + loc.y * scale + loc.h * scale / 2, loc.w * scale / 2, loc.h * scale / 2, 0, loc.tincture.get(coa).tint, false);
					} else {
						coa.charge[chargeI].unlitRightApp.draw(d, x + loc.x * scale + loc.w * scale / 2, y + loc.y * scale, loc.w * scale / 2, loc.h * scale, 0, loc.tincture.get(coa).tint, false);
					}
				} else if (loc.topOnly) {
					coa.charge[chargeI].unlitTopApp.draw(d, x + loc.x * scale, y + loc.y * scale, loc.w * scale, loc.h * scale / 2, 0, loc.tincture.get(coa).tint, false);
				} else if (loc.bottomOnly) {
					coa.charge[chargeI].unlitBottomApp.draw(d, x + loc.x * scale, y + loc.y * scale + loc.h * scale / 2, loc.w * scale, loc.h * scale / 2, 0, loc.tincture.get(coa).tint, false);
				} else {
					coa.charge[chargeI].unlitApp.draw(d, x + loc.x * scale, y + loc.y * scale, loc.w * scale, loc.h * scale, 0, loc.tincture.get(coa).tint, false);
				}
			}
		}
		if (shieldOutline != null) {
			if (shieldOutline.machineImgCache != null) {
				((Image) shieldOutline.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			d.blit(shieldOutline, outlineTint, x, y, shieldOutline.srcWidth * scale, shieldOutline.srcHeight * scale);
		}
	}

	public String blazon(CoatOfArms coa) {
		// First the detail prefix, then all the tincture names, then all the charges, then all the plural charges.
		/*
		0 detailPrefix
		1 tinc
		2 tinc
		3 tinc
		4 tinc
		5 charge
		6 charge
		7 charge
		8 charge
		9 charges
		10 charges
		11 charges
		12 charges
		13 first charge tincture
		14 first charge name
		15 first charge plural name
		 */
		return Lang._t("layout_" + name() + "_blazon", detailPrefix != null ? Lang._t(detailPrefix + coa.tincture[1].name()) : "", coa.tincture[0].getName(), coa.tincture[1].getName(), coa.tincture[2].getName(), coa.tincture[3].getName(), coa.charge[0] == null ? coa.chargeName(0) + " " + coa.chargeT[0].getName() : Lang._t(coa.charge[0].name + "_" + coa.chargeT[0].name()), coa.charge[1] == null ? coa.chargeName(1) + " " + coa.chargeT[1].getName() : Lang._t(coa.charge[1].name + "_" + coa.chargeT[1].name()), coa.charge[2] == null ? coa.chargeName(2) + " " + coa.chargeT[2].getName() : Lang._t(coa.charge[2].name + "_" + coa.chargeT[2].name()), coa.charge[3] == null ? coa.chargeName(3) + " " + coa.chargeT[3].getName() : Lang._t(coa.charge[3].name + "_" + coa.chargeT[3].name()), coa.charge[0] == null ? coa.chargesName(0) + " " + coa.chargeT[0].getName() : Lang._t(coa.charge[0].name + "_" + coa.chargeT[0].name() + "_plural"), coa.charge[1] == null ? coa.chargesName(1) + " " + coa.chargeT[1].getName() : Lang._t(coa.charge[1].name + "_" + coa.chargeT[1].name() + "_plural"), coa.charge[2] == null ? coa.chargesName(2) + " " + coa.chargeT[2].getName() : Lang._t(coa.charge[2].name + "_" + coa.chargeT[2].name() + "_plural"), coa.charge[3] == null ? coa.chargesName(3) + " " + coa.chargeT[3].getName() : Lang._t(coa.charge[3].name + "_" + coa.chargeT[3].name() + "_plural"), coa.chargeT[0].getName(), coa.chargeName(0), coa.chargesName(0));
	}

	public String getName() {
		return Lang._t("layout_" + name());
	}
	
	public String name() { return name; }
	
	public static ArmsLayout valueOf(String name) {
		return ofName(ArmsLayout.class, name);
	}
}
