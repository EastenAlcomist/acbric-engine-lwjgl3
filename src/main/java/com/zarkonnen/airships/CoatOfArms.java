package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public class  CoatOfArms implements JSONAble {
	public static final int MAX_PARTS = 4;
	
	public ArmsLayout layout = ArmsLayout.valueOf("CHARGE");
	public final Charge[] charge = { Charge.ofName("ROUNDEL"), Charge.ofName("ROUNDEL"), Charge.ofName("ROUNDEL"), Charge.ofName("ROUNDEL") };
	public final String[] specialCharge = { null, null, null, null };
	public final Tincture[] tincture = { Tincture.valueOf("ARGENT"), Tincture.valueOf("ARGENT"), Tincture.valueOf("ARGENT"), Tincture.valueOf("ARGENT") };
	public final Tincture[] chargeT = { Tincture.valueOf("SABLE"), Tincture.valueOf("SABLE"), Tincture.valueOf("SABLE"), Tincture.valueOf("SABLE") };
	public boolean ruleOfTincture = true;
	private transient Color mixedColor;
	public Bonus customBonus;
	
	public static ArrayList<Bonus> chargeBonuses() {
		ArrayList<Bonus> bs = new ArrayList<Bonus>();
		for (Charge c : HeraldicStyle.ofName("player").charges) {
			if (c.bonus != null && !c.bonus.name.equals("NO_BONUS")) {
				bs.add(c.bonus);
			}
		}
		Collections.sort(bs);
		return bs;
	}
	
	public boolean hasSpecialCharge() {
		for (int i = 0; i < layout.charges; i++) {
			if (specialCharge[i] != null) { return true; }
		}
		return false;
	}
	
	private void cleanupCharges() {
		for (int i = 0; i < charge.length; i++) {
			if (i < layout.charges) {
				if (charge[i] == null) {
					if (specialCharge[i] == null) {
						charge[i] = Charge.ofName("RAT");
					}
				} else {
					if (specialCharge[i] != null) {
						specialCharge[i] = null;
					}
				}
			} else {
				charge[i] = null;
				specialCharge[i] = null;
			}
		}
		if (!hasSpecialCharge()) { customBonus = null; }
	}
	
	public Bonus getBonus() {
		cleanupCharges();
		if (customBonus != null) { return customBonus; }
		for (int i = 0; i < layout.charges; i++) {
			if (charge[i] != null && (charge[i].bonus != Bonus.ofName("NO_BONUS") || charge[i].tech != null)) {
				return charge[i].bonus;
			}
		}
		return Bonus.ofName("NO_BONUS");
	}
	
	public Tech.Choice getTech() {
		if (customBonus != null && !customBonus.name.equals("NO_BONUS")) { return null; }
		cleanupCharges();
		for (int i = 0; i < layout.charges; i++) {
			if (charge[i] != null && (charge[i].bonus != Bonus.ofName("NO_BONUS") || charge[i].tech != null)) {
				return charge[i].tech;
			}
		}
		return null;
	}
	
	public Charge getActiveCharge() {
		cleanupCharges();
		for (int i = 0; i < layout.charges; i++) {
			if (charge[i] != null && (charge[i].bonus != Bonus.ofName("NO_BONUS") || charge[i].tech != null)) {
				return charge[i];
			}
		}
		return null;
	}
	
	public Tincture[] getRoundelTinctures() {
		return new Tincture[] { getTincture(0), getTincture(1), getTincture(2) };
	}
	
	public Tincture getMapTincture() {
		Tincture[] ts = getRoundelTinctures();
		int ti = 0;
		while (ti < ts.length - 1 && !ts[ti].metal) {
			ti++;
		}
		return ts[ti];
	}
	
	public ArrayList<Clr> getMapColors() {
		ArrayList<Clr> cs = new ArrayList<Clr>();
		// If we have multiple base tinctures, mix them.
		if (layout.tinctures > 1) {
			int r = 0, g = 0, b = 0;
			for (int i = 0; i < layout.tinctures; i++) {
				r += tincture[i].tint.r;
				g += tincture[i].tint.g;
				b += tincture[i].tint.b;
			}
			r /= layout.tinctures;
			g /= layout.tinctures;
			b /= layout.tinctures;
			cs.add(new Clr(r, g, b));
		}
		// If we have base and charge tinctures, combine the first ones.
		if (layout.tinctures >= 1 && layout.charges >= 1) {
			cs.add(new Clr(tincture[0].tint.r / 2 + chargeT[0].tint.r / 2, tincture[0].tint.g / 2 + chargeT[0].tint.g / 2, tincture[0].tint.b / 2 + chargeT[0].tint.b / 2));
		}
		// Then try each base tincture in turn.
		for (int i = 0; i < layout.tinctures; i++) {
			cs.add(tincture[i].tint);
		}
		// Then try each charge tincture in turn.
		for (int i = 0; i < layout.charges; i++) {
			cs.add(chargeT[i].tint);
		}
		return cs;
	}
	
	public Color getMixedColor() {
		if (mixedColor == null) {
			Tincture t1 = tincture[0];
			Tincture t2 = layout.charges > 0 ? chargeT[0] : layout.tinctures > 1 ? tincture[1] : tincture[0];
			Tincture t3 = layout.charges > 0 && layout.tinctures > 1 ? tincture[1] : tincture[0];
			mixedColor = new Color(
					t1.tint.r / 2 + t2.tint.r / 4 + t3.tint.r / 4,
					t1.tint.g / 2 + t2.tint.g / 4 + t3.tint.g / 4,
					t1.tint.b / 2 + t2.tint.b / 4 + t3.tint.b / 4
			);
		}
		return mixedColor;
	}
	
	public Tincture getFirstColour() {
		for (int i = 0; i < 4; i++) {
			if (i < layout.tinctures && !tincture[i].metal) {
				return tincture[i];
			}
			if (i < layout.charges && !chargeT[i].metal) {
				return chargeT[i];
			}
		}
		return Tincture.valueOf("GULES");
	}
	
	public Tincture getFirstMetal() {
		for (int i = 0; i < 4; i++) {
			if (i < layout.tinctures && tincture[i].metal) {
				return tincture[i];
			}
			if (i < layout.charges && chargeT[i].metal) {
				return chargeT[i];
			}
		}
		return Tincture.valueOf("OR");
	}
	
	public Tincture getTincture(int index) {
		if (layout.charges == 0) {
			return tincture[index % layout.tinctures];
		}
	
		switch (index) {
			case 0:
				return chargeT[0];
			case 1:
				for (int i = 0; i < layout.tinctures; i++) {
					if (tincture[i].metal != chargeT[0].metal) {
						return tincture[i];
					}
				}
				for (int i = 0; i < layout.tinctures; i++) {
					if (tincture[i] != chargeT[0]) {
						return tincture[i];
					}
				}
				return tincture[0];
			case 2:
				for (int i = 0; i < layout.tinctures; i++) {
					if (tincture[i].metal != getTincture(1).metal && tincture[i] != chargeT[0]) {
						return tincture[i];
					}
				}
				for (int i = 0; i < layout.tinctures; i++) {
					if (tincture[i] != getTincture(1) && tincture[i] != chargeT[0]) {
						return tincture[i];
					}
				}
				for (int i = 1; i < layout.charges; i++) {
					if (chargeT[i] != getTincture(1) && chargeT[i] != chargeT[0]) {
						return chargeT[i];
					}
				}
				return chargeT[0];
		}
		return null;
	}

	@Override
	public JSONObject toJSON() {
		cleanupCharges();
		JSONObject o = new JSONObject().
				put("layout", layout.name()).
				put("fieldT", tincture[0].name()).
				put("chargeT", chargeT[0].name()).
				put("ruleOfTincture", ruleOfTincture);
		if (charge[0] == null) {
			o.put("specialCharge", specialCharge[0]);
		} else {
			o.put("charge", charge[0].name);
		}
		if (customBonus != null) {
			o.put("customBonus", customBonus.name);
		}
		if (layout.tinctures > 1) {
			o.put("secondT", tincture[1].name());
		}
		for (int i = 1; i < layout.charges; i++) {
			if (charge[i] == null) {
				o.put("specialCharge" + i, specialCharge[i]);
			} else {
				o.put("charge" + i, charge[i].name);
			}
			o.put("chargeT" + i, chargeT[i].name());
		}
		for (int i = 2; i < layout.tinctures; i++) {
			o.put("tincture" + i, tincture[i].name());
		}
		return o;
	}
	
	@Override
	public int hashCode() {
		cleanupCharges();
		int code = layout.name.hashCode();
		for (int i = 0; i < layout.charges; i++) {
			code *= 37;
			code += charge[i] == null ? specialCharge[i].hashCode() : charge[i].name.hashCode();
		}
		for (int i = 0; i < layout.tinctures; i++) {
			code *= 37;
			code += tincture[i].ordinal();
		}
		if (customBonus != null) {
			code *= 17;
			code += customBonus.name.hashCode();
		}
		return code;
	}
	
	@Override
	public boolean equals(Object o) {
		cleanupCharges();
		if (!(o instanceof CoatOfArms)) { return false; }
		CoatOfArms coa2 = (CoatOfArms) o;
		coa2.cleanupCharges();
		if (layout != coa2.layout) { return false; }
		for (int i = 0; i < layout.charges; i++) {
			if (charge[i] != coa2.charge[i]) {
				return false;
			}
			if (chargeT[i] != coa2.chargeT[i]) {
				return false;
			}
			if (specialCharge[i] != null && !specialCharge[i].equals(coa2.specialCharge[i])) {
				return false;
			}
		}
		for (int i = 0; i < layout.tinctures; i++) {
			if (tincture[i] != coa2.tincture[i]) {
				return false;
			}
		}
		return customBonus == coa2.customBonus;
	}
	
	public CoatOfArms copy() {
		cleanupCharges();
		CoatOfArms coa = new CoatOfArms();
		coa.layout = layout;
		coa.ruleOfTincture = ruleOfTincture;
		for (int i = 0; i < MAX_PARTS; i++) {
			coa.charge[i] = charge[i];
			coa.specialCharge[i] = specialCharge[i];
			coa.chargeT[i] = chargeT[i];
			coa.tincture[i] = tincture[i];
		}
		coa.customBonus = customBonus;
		return coa;
	}

	public CoatOfArms() {}
	
	@Override
	public String toString() {
		return toJSON().toString(4);
	}

	public CoatOfArms(JSONObject o) {
		try {
			layout = ArmsLayout.valueOf(o.getString("layout"));
			ruleOfTincture = o.optBoolean("ruleOfTincture", true);
			if (layout.charges > 0) {
				if (o.has("charge")) {
					charge[0] = Charge.ofName(o.getString("charge"));
				} else {
					charge[0] = null;
					specialCharge[0] = o.getString("specialCharge");
				}
			} else {
				charge[0] = null;
				specialCharge[0] = null;
			}
			tincture[0] = Tincture.valueOf(o.getString("fieldT"));
			chargeT[0] = Tincture.valueOf(o.getString("chargeT"));
			if (layout.tinctures > 1) {
				tincture[1] = Tincture.valueOf(o.getString("secondT"));
			}
			for (int i = 1; i < layout.charges; i++) {
				if (o.has("charge" + i)) {
					charge[i] = Charge.ofName(o.getString("charge" + i));
					chargeT[i] = Tincture.valueOf(o.getString("chargeT" + i));
				} else if (o.has("specialCharge" + i)) {
					charge[i] = null;
					specialCharge[i] = o.getString("specialCharge" + i);
					chargeT[i] = Tincture.valueOf(o.getString("chargeT" + i));
				} else {
					charge[i] = charge[0];
					specialCharge[i] = specialCharge[0];
					chargeT[i] = chargeT[0];
				}
			}
			if (layout.tinctures > 2) {
				if (o.has("tincture2")) {
					tincture[2] = Tincture.valueOf(o.getString("tincture2"));
				} else {
					tincture[2] = tincture[0];
				}
			}
			if (layout.tinctures > 3) {
				if (o.has("tincture3")) {
					tincture[3] = Tincture.valueOf(o.getString("tincture3"));
				} else {
					tincture[3] = tincture[1];
				}
			}
			if (o.has("customBonus")) {
				customBonus = Bonus.ofNameOrNull(o.getString("customBonus"));
			}
		} catch (Exception e) {
			layout = ArmsLayout.valueOf("SALTIRE");
			tincture[0] = Tincture.valueOf("SABLE");
			tincture[1] = Tincture.valueOf("OR");
		}
	}

	public void setCharge(Charge c, int index) {
		charge[index] = c;
		specialCharge[index] = null;
		cleanupCharges();
	}
	
	public void setSpecialCharge(String specialCharge, int index) {
		this.specialCharge[index] = specialCharge;
		charge[index] = null;
		cleanupCharges();
	}
	
	public static enum TinctureSlot {
		TINCTURE0(0, -1),
		TINCTURE1(1, -1),
		TINCTURE2(2, -1),
		TINCTURE3(3, -1),
		CHARGE0(-1, 0),
		CHARGE1(-1, 1),
		CHARGE2(-1, 2),
		CHARGE3(-1, 3),
		;
		public final int tinctureIndex;
		public final int chargeIndex;
		
		public static TinctureSlot tinctureOf(int index) {
			for (TinctureSlot ts : values()) {
				if (ts.tinctureIndex == index) { return ts; }
			}
			return null;
		}

		public static TinctureSlot chargeTOf(int index) {
			for (TinctureSlot ts : values()) {
				if (ts.chargeIndex == index) { return ts; }
			}
			return null;
		}
		
		private TinctureSlot(int tinctureIndex, int chargeIndex) {
			this.tinctureIndex = tinctureIndex;
			this.chargeIndex = chargeIndex;
		}

		public Tincture get(CoatOfArms coa) {
			return tinctureIndex == -1 ? coa.chargeT[chargeIndex] : coa.tincture[tinctureIndex];
		}
	}

	public void draw(MyDraw d, double x, double y) {
		layout.draw(this, d, x, y, 16, null);
	}

	public void draw(MyDraw d, double x, double y, int size) {
		layout.draw(this, d, x, y, size, null);
	}
	
	public void draw(MyDraw d, double x, double y, int size, TinctureSlot highlight) {
		layout.draw(this, d, x, y, size, highlight);
	}
	
	public void drawBase(MyDraw d, double x, double y, int size, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		layout.drawBase(this, d, x, y, size, light, lightStrength, ambient, ambientSaturation);
	}
	
	public void drawCharges(MyDraw d, double x, double y, int size, Image[] light, float lightStrength, Color ambient, float ambientSaturation, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs) {
		layout.drawCharges(this, d, x, y, size, light, lightStrength, ambient, ambientSaturation, ssb, additionalSSBs);
	}
	
	public void drawUnlitCharges(MyDraw d, double x, double y, int size) {
		layout.drawUnlitCharges(this, d, x, y, size);
	}

	public String blazon() {
		return layout.blazon(this);
	}
	
	public String getBonusOrTechDesc() {
		if (getBonus() != null && !getBonus().name.equals("NO_BONUS")) { return getBonus().getDesc(); }
		Charge c = getActiveCharge();
		if (c == null) {
			return Bonus.ofName("NO_BONUS").getDesc();
		}
		return c.getBonusOrTechDesc();
	}
	
	public void setLayout(ArmsLayout newLayout) {
		// Gather elements
		ArrayList<Tincture> tinctures = new ArrayList<Tincture>();
		ArrayList<Charge> charges = new ArrayList<Charge>();
		String theSpecialCharge = null;
		for (int i = 0; i < layout.tinctures; i++) {
			Tincture t = tincture[i];
			tinctures.add(t);
		}
		for (int i = 0; i < layout.charges; i++) {
			Tincture t = chargeT[i];
			tinctures.add(t);
			if (charge[i] != null) {
				charges.add(charge[i]);
			} else {
				theSpecialCharge = specialCharge[i];
			}
		}
		// Fill up
		for (int i = layout.tinctures; i < newLayout.tinctures; i++) {
			tincture[i] = tinctures.get(i % tinctures.size());
		}
		for (int i = layout.charges; i < newLayout.charges; i++) {
			chargeT[i] = tinctures.get((i + layout.tinctures) % tinctures.size());
			if (charges.isEmpty()) {
				charge[i] = null;
				specialCharge[i] = theSpecialCharge;
			} else {
				charge[i] = charges.get(i % charges.size());
				specialCharge[i] = null;
			}
		}
		layout = newLayout;
		cleanupTinctures(null);
		cleanupCharges();
	}
	
	private void cleanupTinctures(TinctureSlot keepFixed) {
		// Gather elements
		ArrayList<Tincture> metals = new ArrayList<Tincture>();
		ArrayList<Tincture> nonMetals = new ArrayList<Tincture>();
		for (int i = 0; i < layout.tinctures; i++) {
			Tincture t = tincture[i];
			(t.metal ? metals : nonMetals).add(t);
		}
		for (int i = 0; i < layout.charges; i++) {
			Tincture t = chargeT[i];
			(t.metal ? metals : nonMetals).add(t);
		}
		if (metals.isEmpty()) {
			metals.add(Tincture.valueOf("OR"));
		}
		if (nonMetals.isEmpty()) {
			nonMetals.add(Tincture.valueOf("SABLE"));
		}
		int n1 = 0, n2 = 0, n3 = 0;
		while (!legal()) {
			for (TinctureSlot tsA : TinctureSlot.values()) {
				if (layout.adjacency.containsKey(tsA)) {
					for (TinctureSlot tsB : layout.adjacency.get(tsA)) {
						if (tsB == keepFixed) { continue; }
						if (getTincture(tsA) == getTincture(tsB)) {
							ArrayList<Tincture> ts = Tincture.values();
							setTinctureOnly(ts.get((n1++) % ts.size()), tsB);
						}
					}
				}
			}
			for (TinctureSlot tsA : TinctureSlot.values()) {
				if (!layout.onTop.containsKey(tsA)) { continue; }
				for (TinctureSlot tsB : layout.onTop.get(tsA)) {
					if (tsB == keepFixed) { continue; }
					if (ruleOfTincture) {
						if (getTincture(tsA).metal == getTincture(tsB).metal) {
							ArrayList<Tincture> ts = (getTincture(tsB).metal ? nonMetals : metals);
							setTinctureOnly(ts.get((n2++) % ts.size()), tsB);
						}
					} else {
						if (getTincture(tsA) == getTincture(tsB)) {
							ArrayList<Tincture> ts = Tincture.values();
							setTinctureOnly(ts.get((n3++) % ts.size()), tsB);
						}
					}
				}
			}
		}
	}
		
	public void setTincture(Tincture t, TinctureSlot slot) {
		EnumSet<TinctureSlot> visited = EnumSet.of(slot);
		setTincture(t, slot, visited);
	}
	
	public Tincture getTincture(TinctureSlot slot) {
		if (slot.tinctureIndex > -1) {
			return tincture[slot.tinctureIndex];
		} else {
			return chargeT[slot.chargeIndex];
		}
	}
	
	private Tincture setTinctureOnly(Tincture t, TinctureSlot slot) {
		Tincture old;
		if (slot.tinctureIndex > -1) {
			old = tincture[slot.tinctureIndex];
			tincture[slot.tinctureIndex] = t;
		} else {
			old = chargeT[slot.chargeIndex];
			chargeT[slot.chargeIndex] = t;
		}
		return old;
	}
	
	public void setTincture(Tincture t, TinctureSlot slot, EnumSet<TinctureSlot> visited) {
		Tincture old = setTinctureOnly(t, slot);
		if (layout.adjacency.containsKey(slot)) {
			for (TinctureSlot ts : layout.adjacency.get(slot)) {
				if (!visited.contains(ts) && getTincture(ts) == t) {
					visited.add(ts);
					setTincture(old, ts, visited);
				}
			}
		}
		if (layout.onTop.containsKey(slot)) {
			for (TinctureSlot ts : layout.onTop.get(slot)) {
				if (!visited.contains(ts) && getTincture(ts).metal == t.metal) {
					visited.add(ts);
					setTincture(old, ts, visited);
				}
			}
		}
		cleanupTinctures(slot);
	}

	public boolean legal() {
		for (TinctureSlot tsA : layout.adjacency.keySet()) {
			for (TinctureSlot tsB : layout.adjacency.get(tsA)) {
				if (getTincture(tsA) == getTincture(tsB)) {
					return false;
				}
			}
		}
		
		if (ruleOfTincture) {
			for (TinctureSlot tsA : layout.onTop.keySet()) {
				for (TinctureSlot tsB : layout.onTop.get(tsA)) {
					if (getTincture(tsA).metal == getTincture(tsB).metal) {
						return false;
					}
				}
			}
		} else {
			for (TinctureSlot tsA : layout.onTop.keySet()) {
				for (TinctureSlot tsB : layout.onTop.get(tsA)) {
					if (getTincture(tsA) == getTincture(tsB)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public static CoatOfArms getConsistentAndSafe() {
		CoatOfArms coa = new CoatOfArms();
		coa.ruleOfTincture = false;
		HeraldicStyle style = HeraldicStyle.ofName("player");
		coa.layout = style.layouts.get(0);
		for (int i = 0; i < coa.layout.charges; i++) {
			coa.charge[i] = style.charges.get(0);
			coa.chargeT[i] = style.chargeTinctures.get(0);
		}
		for (int i = 0; i < coa.layout.tinctures; i++) {
			coa.tincture[i] = style.layoutTinctures.get(0);
		}
		return coa;
	}
	
	public static CoatOfArms spectatorArms() {
		try {
			CoatOfArms coa = new CoatOfArms();
			coa.layout = ArmsLayout.valueOf("CHARGE");
			coa.tincture[0] = Tincture.valueOf("CENDREE");
			coa.charge[0] = Charge.ofName("EYE");
			coa.chargeT[0] = Tincture.valueOf("SABLE");
			return coa;
		} catch (Exception e) {
			return getConsistentAndSafe();
		}
	}
	
	public static CoatOfArms aiArms() {
		try {
			CoatOfArms coa = new CoatOfArms();
			coa.layout = ArmsLayout.valueOf("CHARGE");
			coa.tincture[0] = Tincture.valueOf("OR");
			coa.charge[0] = Charge.ofName("GEAR");
			coa.chargeT[0] = Tincture.valueOf("SABLE");
			return coa;
		} catch (Exception e) {
			return getConsistentAndSafe();
		}
	}

	public static CoatOfArms getRandom(GuardedRandom r, HeraldicStyle style) {
		CoatOfArms coa = new CoatOfArms();
		coa.ruleOfTincture = style.ruleOfTincture;
		coa.layout = style.layouts.get(r.nextInt(style.layouts.size()));
		do {
			if (r.nextInt(10) == 1) {
				coa.layout = style.layouts.get(r.nextInt(style.layouts.size()));
			}
			for (int i = 0; i < coa.layout.charges; i++) {
				if (style.firstChargeShouldHaveBonus && i == 0) {
					coa.charge[i] = style.weightedChargesWithBonusOrTech.get(r.nextInt(style.weightedChargesWithBonusOrTech.size()));
				} else {
					coa.charge[i] = style.weightedCharges.get(r.nextInt(style.weightedCharges.size()));
				}
				coa.chargeT[i] = style.chargeTinctures.get(r.nextInt(style.chargeTinctures.size()));
			}
			for (int i = 0; i < coa.layout.tinctures; i++) {
				coa.tincture[i] = style.layoutTinctures.get(r.nextInt(style.layoutTinctures.size()));
			}
		} while (!coa.legal() || (coa.getBonus() == null && coa.getTech() == null));
		coa.cleanupCharges();
		return coa;
	}



	private static Appearance FLAG = null;
	private static Appearance UL_FLAG = null;

	private static Appearance PARTY_PER_PALE_DETAIL = null;
	private static Appearance UL_PARTY_PER_PALE_DETAIL = null;

	private static Appearance TIERCED_PALE_DETAIL = null;
	private static Appearance UL_TIERCED_PALE_DETAIL = null;

	private static Appearance PARTY_PER_FESS_DETAIL = null;
	private static Appearance UL_PARTY_PER_FESS_DETAIL = null;

	private static Appearance TIERCED_FESS_DETAIL = null;
	private static Appearance UL_TIERCED_FESS_DETAIL = null;

	private static Appearance Q_BL_DETAIL = null;
	private static Appearance UL_Q_BL_DETAIL = null;

	private static Appearance Q_BR_DETAIL = null;
	private static Appearance UL_Q_BR_DETAIL = null;

	private static Appearance PARTY_PER_BEND_DETAIL = null;
	private static Appearance UL_PARTY_PER_BEND_DETAIL = null;

	private static Appearance PARTY_PER_BEND_SINISTER_DETAIL = null;
	private static Appearance UL_PARTY_PER_BEND_SINISTER_DETAIL = null;

	private static Appearance TIERCED_BEND_DETAIL = null;
	private static Appearance UL_TIERCED_BEND_DETAIL = null;

	private static Appearance TIERCED_BEND_SINISTER_DETAIL = null;
	private static Appearance UL_TIERCED_BEND_SINISTER_DETAIL = null;

	private static Appearance PARTY_PER_SALTIRE_T_DETAIL = null;
	private static Appearance UL_PARTY_PER_SALTIRE_T_DETAIL = null;

	private static Appearance PARTY_PER_SALTIRE_B_DETAIL = null;
	private static Appearance UL_PARTY_PER_SALTIRE_B_DETAIL = null;

	private static Appearance PARTY_PER_CHEVRON_DETAIL = null;
	private static Appearance UL_PARTY_PER_CHEVRON_DETAIL = null;

	private static Appearance PARTY_PER_CHEVRON_INV_DETAIL = null;
	private static Appearance UL_PARTY_PER_CHEVRON_INV_DETAIL = null;

	private static Appearance TIERCED_CHEVRON_DETAIL = null;
	private static Appearance UL_TIERCED_CHEVRON_DETAIL = null;

	private static Appearance TIERCED_CHEVRON_INV_DETAIL = null;
	private static Appearance UL_TIERCED_CHEVRON_INV_DETAIL = null;

	private static Appearance PARTY_PER_PILE_DETAIL = null;
	private static Appearance UL_PARTY_PER_PILE_DETAIL = null;

	private static Appearance PARTY_PER_PILE_INV_DETAIL = null;
	private static Appearance UL_PARTY_PER_PILE_INV_DETAIL = null;

	private static Appearance BORDURE_DETAIL = null;
	private static Appearance UL_BORDURE_DETAIL = null;

	private static Appearance ORLE_DETAIL = null;
	private static Appearance UL_ORLE_DETAIL = null;

	private static Appearance TRESSURE_DETAIL = null;
	private static Appearance UL_TRESSURE_DETAIL = null;

	private static Appearance DOUBLE_TRESSURE_DETAIL = null;
	private static Appearance UL_DOUBLE_TRESSURE_DETAIL = null;

	private static Appearance PALE_DETAIL = null;
	private static Appearance UL_PALE_DETAIL = null;

	private static Appearance FESS_DETAIL = null;
	private static Appearance UL_FESS_DETAIL = null;

	private static Appearance ON_BEND_DETAIL = null;
	private static Appearance UL_ON_BEND_DETAIL = null;

	private static Appearance ON_BEND_SINISTER_DETAIL = null;
	private static Appearance UL_ON_BEND_SINISTER_DETAIL = null;

	private static Appearance BEND_DETAIL = null;
	private static Appearance UL_BEND_DETAIL = null;

	private static Appearance BEND_SINISTER_DETAIL = null;
	private static Appearance UL_BEND_SINISTER_DETAIL = null;

	private static Appearance CROSS_DETAIL = null;
	private static Appearance UL_CROSS_DETAIL = null;

	private static Appearance ON_CROSS_DETAIL = null;
	private static Appearance UL_ON_CROSS_DETAIL = null;

	private static Appearance SALTIRE_DETAIL = null;
	private static Appearance UL_SALTIRE_DETAIL = null;

	private static Appearance PALL_DETAIL = null;
	private static Appearance UL_PALL_DETAIL = null;

	private static Appearance PALL_INV_DETAIL = null;
	private static Appearance UL_PALL_INV_DETAIL = null;

	private static Appearance PALY_DETAIL = null;
	private static Appearance UL_PALY_DETAIL = null;

	private static Appearance BARRY_DETAIL = null;
	private static Appearance UL_BARRY_DETAIL = null;

	private static Appearance BENDY_DETAIL = null;
	private static Appearance UL_BENDY_DETAIL = null;

	private static Appearance BENDY_SINISTER_DETAIL = null;
	private static Appearance UL_BENDY_SINISTER_DETAIL = null;

	private static Appearance CHEVRONNY_DETAIL = null;
	private static Appearance UL_CHEVRONNY_DETAIL = null;

	private static Appearance CHEVRONNY_INV_DETAIL = null;
	private static Appearance UL_CHEVRONNY_INV_DETAIL = null;

	public static final int SHIELD_W = 36;

	public static final int SHIELD_H = 41;

	public static void updateAppearances() {
		FLAG = new Appearance(SpritesheetBundle.ofName("heraldry"), 2, 8);
		UL_FLAG = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 2, 8);
		PARTY_PER_PALE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 3, 8);
		UL_PARTY_PER_PALE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 3, 8);
		TIERCED_PALE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 4, 8);
		UL_TIERCED_PALE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 4, 8);
		PARTY_PER_FESS_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 5, 8);
		UL_PARTY_PER_FESS_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 5, 8);
		TIERCED_FESS_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 6, 8);
		UL_TIERCED_FESS_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 6, 8);
		Q_BL_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 7, 8);
		UL_Q_BL_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 7, 8);
		Q_BR_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 8, 8);
		UL_Q_BR_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 8, 8);
		PARTY_PER_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 9, 8);
		UL_PARTY_PER_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 9, 8);
		PARTY_PER_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 10, 8);
		UL_PARTY_PER_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 10, 8);
		TIERCED_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 11, 8);
		UL_TIERCED_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 11, 8);
		TIERCED_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 12, 8);
		UL_TIERCED_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 12, 8);
		PARTY_PER_SALTIRE_T_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 13, 8);
		UL_PARTY_PER_SALTIRE_T_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 13, 8);
		PARTY_PER_SALTIRE_B_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 14, 8);
		UL_PARTY_PER_SALTIRE_B_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 14, 8);
		PARTY_PER_CHEVRON_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 15, 8);
		UL_PARTY_PER_CHEVRON_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 15, 8);
		PARTY_PER_CHEVRON_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 16, 8);
		UL_PARTY_PER_CHEVRON_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 16, 8);
		TIERCED_CHEVRON_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 17, 8);
		UL_TIERCED_CHEVRON_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 17, 8);
		TIERCED_CHEVRON_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 18, 8);
		UL_TIERCED_CHEVRON_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 18, 8);
		PARTY_PER_PILE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 19, 8);
		UL_PARTY_PER_PILE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 19, 8);
		PARTY_PER_PILE_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 20, 8);
		UL_PARTY_PER_PILE_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 20, 8);
		BORDURE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 23, 8);
		UL_BORDURE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 23, 8);
		ORLE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 24, 8);
		UL_ORLE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 24, 8);
		TRESSURE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 25, 8);
		UL_TRESSURE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 25, 8);
		DOUBLE_TRESSURE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 26, 8);
		UL_DOUBLE_TRESSURE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 26, 8);
		PALE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 27, 8);
		UL_PALE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 27, 8);
		FESS_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 28, 8);
		UL_FESS_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 28, 8);
		ON_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 29, 8);
		UL_ON_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 29, 8);
		ON_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 30, 8);
		UL_ON_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 30, 8);
		BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 31, 8);
		UL_BEND_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 31, 8);
		BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 32, 8);
		UL_BEND_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 32, 8);
		CROSS_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 33, 8);
		UL_CROSS_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 33, 8);
		ON_CROSS_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 34, 8);
		UL_ON_CROSS_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 34, 8);
		SALTIRE_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 35, 8);
		UL_SALTIRE_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 35, 8);
		PALL_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 36, 8);
		UL_PALL_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 36, 8);
		PALL_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 37, 8);
		UL_PALL_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 37, 8);
		PALY_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 2, 16);
		UL_PALY_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 2, 16);
		BARRY_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 23, 16);
		UL_BARRY_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 23, 16);
		BENDY_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 2, 23);
		UL_BENDY_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 2, 23);
		BENDY_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 23, 23);
		UL_BENDY_SINISTER_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 23, 23);
		CHEVRONNY_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 2, 30);
		UL_CHEVRONNY_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 2, 30);
		CHEVRONNY_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("heraldry"), 23, 30);
		UL_CHEVRONNY_INV_DETAIL = new Appearance(SpritesheetBundle.ofName("unlit_heraldry"), 23, 30);
	}

	public static final Img SHIELD_OUTLINE = new Img("heraldry", 0, 87, 36, 41, false);

	private static final Img SHIELD_CONTENT = new Img("heraldry", 0, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_PALE = new Img("heraldry", 37, 151, 36, 41, false);

	private static final Img SHIELD_TIERCED_PALE = new Img("heraldry", 74, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_FESS = new Img("heraldry", 111, 151, 36, 41, false);

	private static final Img SHIELD_TIERCED_FESS = new Img("heraldry", 148, 151, 36, 41, false);

	private static final Img SHIELD_Q_BL = new Img("heraldry", 185, 151, 36, 41, false);

	private static final Img SHIELD_Q_BR = new Img("heraldry", 222, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_BEND = new Img("heraldry", 259, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_BEND_SINISTER = new Img("heraldry", 296, 151, 36, 41, false);

	private static final Img SHIELD_TIERCED_BEND = new Img("heraldry", 333, 151, 36, 41, false);

	private static final Img SHIELD_TIERCED_BEND_SINISTER = new Img("heraldry", 370, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_SALTIRE_T = new Img("heraldry", 407, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_SALTIRE_B = new Img("heraldry", 444, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_CHEVRON = new Img("heraldry", 481, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_CHEVRON_INV = new Img("heraldry", 518, 151, 36, 41, false);

	private static final Img SHIELD_TIERCED_CHEVRON = new Img("heraldry", 555, 151, 36, 41, false);

	private static final Img SHIELD_TIERCED_CHEVRON_INV = new Img("heraldry", 592, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_PILE = new Img("heraldry", 629, 151, 36, 41, false);

	private static final Img SHIELD_PARTY_PER_PILE_INV = new Img("heraldry", 666, 151, 36, 41, false);

	private static final Img SHIELD_BORDURE = new Img("heraldry", 0, 215, 36, 41, false);

	private static final Img SHIELD_ORLE = new Img("heraldry", 37, 215, 36, 41, false);

	private static final Img SHIELD_TRESSURE = new Img("heraldry", 74, 215, 36, 41, false);

	private static final Img SHIELD_DOUBLE_TRESSURE = new Img("heraldry", 111, 215, 36, 41, false);

	private static final Img SHIELD_PALE = new Img("heraldry", 148, 215, 36, 41, false);

	private static final Img SHIELD_FESS = new Img("heraldry", 185, 215, 36, 41, false);

	private static final Img SHIELD_ON_BEND = new Img("heraldry", 222, 215, 36, 41, false);

	private static final Img SHIELD_ON_BEND_SINISTER = new Img("heraldry", 259, 215, 36, 41, false);

	private static final Img SHIELD_BEND = new Img("heraldry", 296, 215, 36, 41, false);

	private static final Img SHIELD_BEND_SINISTER = new Img("heraldry", 333, 215, 36, 41, false);

	private static final Img SHIELD_CROSS = new Img("heraldry", 370, 215, 36, 41, false);

	private static final Img SHIELD_ON_CROSS = new Img("heraldry", 407, 215, 36, 41, false);

	private static final Img SHIELD_SALTIRE = new Img("heraldry", 444, 215, 36, 41, false);

	private static final Img SHIELD_PALL = new Img("heraldry", 481, 215, 36, 41, false);

	private static final Img SHIELD_PALL_INV = new Img("heraldry", 518, 215, 36, 41, false);

	private static final Img SHIELD_PALY = new Img("heraldry", 0, 279, 36, 41, false);

	private static final Img SHIELD_BARRY = new Img("heraldry", 0, 327, 36, 41, false);

	private static final Img SHIELD_BENDY = new Img("heraldry", 0, 391, 36, 41, false);

	private static final Img SHIELD_BENDY_SINISTER = new Img("heraldry", 0, 439, 36, 41, false);

	private static final Img SHIELD_CHEVRONNY = new Img("heraldry", 0, 503, 36, 41, false);

	private static final Img SHIELD_CHEVRONNY_INV = new Img("heraldry", 0, 551, 36, 41, false);

	private static final Img SHIELD_CHEVRON = new Img("ui", 37, 544, 36, 41, false);

	private static final Img SHIELD_Q_TL = new Img("ui", 333, 544, 36, 41, false);

	private static final Img SHIELD_Q_TR = new Img("ui", 444, 544, 36, 41, false);
	
	public String chargeName(int i) {
		if (charge[i] == null) {
			if (specialCharge[i] == null) {
				return "?";
			} else {
				return specialCharge[i].split("_")[0];
			}
		} else {
			return charge[i].getName();
		}
	}
	
	public String chargesName(int i) {
		if (charge[i] == null) {
			if (specialCharge[i] == null) {
				return "?";
			} else {
				return specialCharge[i].split("_")[1];
			}
		} else {
			return charge[i].getPlural();
		}
	}
	
	private static void d(MyDraw d, Img img, Clr tint, double x, double y, double w, double h) {
		if (img.machineImgCache != null) {
			((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
		}
		d.blit(img, tint, x, y, w, h);
	}
	
	static float li(CoatOfArms coa, TinctureSlot slot, float lightIntensity) {
		return slot.get(coa).metal ? lightIntensity * 6 : lightIntensity;
	}
	
	static Clr tinc(MyDraw d, CoatOfArms coa, TinctureSlot slot, TinctureSlot highlight) {
		Tincture t = slot.get(coa);
		if (slot == highlight) {
			if (t.metal) {
				return d.darkPulse(t.tint);
			} else {
				return d.pulse(t.tint);
			}
		} else {
			return t.tint;
		}
	}
}
