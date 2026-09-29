package com.zarkonnen.airships;

import java.util.List;
import java.util.EnumSet;
import static com.zarkonnen.airships.Lang._t;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class  ArmourType extends Loadable implements HasName {
	public final BonusableValue<Appearance> windowApp;
	public final BonusableValue<ArrayList<Appearance>> damagedApps;
	public final Bonus required;
	public final String placeSound;
	public final double placePitch;
	public final BonusableValue<Integer> cost;
	public final BonusableValue<Integer> hp;
	public final BonusableValue<Integer> weight;
	public final BonusableValue<Integer> lift;
	public final BonusableValue<Integer> blastDmgAbsorb;
	public final BonusableValue<Integer> penDmgAbsorb;
	public final boolean hidden;
	public final BonusableValue<SoundEffect> brokenSound, largeHitSound, smallHitSound;
	public final ArrayList<String> variantNames = new ArrayList<>();
	public final ArrayList<ArmourType> variants = new ArrayList<>();
	public ArmourType variantGroupHead;
	public final VariantType variantType;
	public final boolean enterable;
	public final EnumSet<ShipType> availableFor = EnumSet.allOf(ShipType.class);
	
	public final BonusableValue<ArrayList<ModuleType.FragmentImg>[][]> fragments;

	public int getCost(BonusSet bonuses) { return cost.get(bonuses); }
	public int getHP(BonusSet bonuses) { return hp.get(bonuses); }
	public int getWeight(BonusSet bonuses) { return weight.get(bonuses); }
	public int getLift(BonusSet bonuses) { return lift.get(bonuses); }
	public int getBlastDmgAbsorb(BonusSet bonuses) { return blastDmgAbsorb.get(bonuses); }
	public int getPenDmgAbsorb(BonusSet bonuses) { return penDmgAbsorb.get(bonuses); }
	
	public static ArmourType ofName(String name) {
		return ofName(ArmourType.class, name);
	}
	
	public ArmourType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		windowApp = BonusableValue.objectFromJSON(o, "windowApp", null, new Appearance.FromJSON());
		damagedApps = BonusableValue.listFromJSONRequired(o, "damagedApps", new Appearance.FromJSON());
		required = o.has("required") ? Bonus.ofNameOrNull(o.getString("required")) : null;
		placeSound = o.optString("placeSound", null);
		placePitch = o.optDouble("placePitch", 1.0);
		cost = BonusableValue.intFromJSON(o, "cost", 0);
		hp = BonusableValue.intFromJSON(o, "hp", 1);
		weight = BonusableValue.intFromJSON(o, "weight", 0);
		blastDmgAbsorb = BonusableValue.intFromJSON(o, "blastDmgAbsorb", 0);
		penDmgAbsorb = BonusableValue.intFromJSON(o, "penDmgAbsorb", 0);
		hidden = o.optBoolean("hidden", false);
		brokenSound = BonusableValue.objectFromJSON(o, "brokenSound", null, new SoundEffect.FromJSON(false));
		largeHitSound = BonusableValue.objectFromJSON(o, "largeHitSound", null, new SoundEffect.FromJSON(false));
		smallHitSound = BonusableValue.objectFromJSON(o, "smallHitSound", null, new SoundEffect.FromJSON(false));
		lift = BonusableValue.intFromJSON(o, "lift", 0);
		enterable = o.optBoolean("enterable", false);
		fragments = BonusableValue.derive(damagedApps, new BonusableValue.Derive<ArrayList<Appearance>, ArrayList<ModuleType.FragmentImg>[][]>() {
			@Override
			public ArrayList<ModuleType.FragmentImg>[][] derive(ArrayList<Appearance> from) {
				return new ArrayList[5][5];
			}
		});
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
		if (o.has("availableFor")) {
			availableFor.clear();
			JSONArray af = o.getJSONArray("availableFor");
			for (int i = 0; i < af.length(); i++) {
				availableFor.add(ShipType.valueOf(af.getString(i)));
			}
		}
	}
	
	public static void postLoad() {
		for (ArmourType dt : all(ArmourType.class)) {
			if (!dt.variantNames.isEmpty()) {
				dt.variants.add(dt);
				dt.variantGroupHead = dt;
				for (String n : dt.variantNames) {
					ArmourType dt2 = ArmourType.ofName(n);
					dt.variants.add(dt2);
					dt2.variantGroupHead = dt;
				}
			}
		}
	}
	
	public boolean isVariantGroupMember() {
		return variantGroupHead != null;
	}
	
	public boolean isVariantGroupHead() {
		return variantGroupHead == this;
	}
	
	public ArmourType getVariantGroupHeadOrThis() {
		return variantGroupHead == null ? this : variantGroupHead;
	}
	
	public ArrayList<ArmourType> getVariants() {
		return variants;
	}
	
	public boolean availableFor(ShipType t) {
		return availableFor.contains(t);
	}
	
	public String getDesc() {
		StringBuilder sb = new StringBuilder();
		sb.append(_t("armour_" + name).toUpperCase(Locale.ENGLISH));
		if (required != null && required != Bonus.ofName("NO_BONUS")) {
			Tech.Choice providingTech = Tech.findProvider(required);
			if (providingTech != null) {
				sb.append("\n").append(_t("Requires_tech", _t("tech_" + providingTech.name), providingTech.tech.tier + 1));
			} else {
				sb.append("\n").append(_t("Requires_bonus_x", required.getName()));
			}
		}
		sb.append("\n\n").append(_t("armour_" + name + "_desc"));
		if (sourceMod != null) {
			sb.append("\n\n").append(sourceMod.getName());
		}
		return sb.toString();
	}
	
	public List<BonusSet> getAppBonuses() {
		return damagedApps.getBonusesIfAvailable();
	}
	
	@Override
	public String getName() {
		return _t("armour_" + name);
	}

	public String getDescription(BonusSet bonuses, boolean explain) {
		StringBuilder sb = new StringBuilder(getDesc());
		if (explain) {
			ArrayList<String> notices = new ArrayList<String>();
			notices.addAll(hp.descriptions(bonuses));
			notices.addAll(weight.descriptions(bonuses));
			notices.addAll(lift.descriptions(bonuses));
			notices.addAll(blastDmgAbsorb.descriptions(bonuses));
			notices.addAll(penDmgAbsorb.descriptions(bonuses));
			FormatUtils.stringList(sb, notices, "\n\n", "\n\n", "");
		}
		sb.append("\n");
		sb.append("\n").append(_t("HP_x", hp.bexplain(explain, bonuses)));
		sb.append("\n").append(_t("Weight_x", weight.bexplain(explain, bonuses)));
		if (getLift(bonuses) > 0) {
			sb.append("\n").append(_t("Lift_x", lift.bexplain(explain, bonuses)));
		}
		if (getBlastDmgAbsorb(bonuses) > 0) {
			sb.append("\n").append(_t("Absorbs_x_blast_damage", blastDmgAbsorb.bexplain(explain, bonuses)));
		}
		if (getPenDmgAbsorb(bonuses) > 0) {
			sb.append("\n").append(_t("Absorbs_x_penetration_damage", penDmgAbsorb.bexplain(explain, bonuses)));
		}
		return sb.toString();
	}
}
