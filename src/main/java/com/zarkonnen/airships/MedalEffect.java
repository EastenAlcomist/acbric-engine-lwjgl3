package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class MedalEffect extends Loadable {
	public final int level;
	public final int fireRatePercent;
	public final int accuracyPercent;
	public final int crewSpeedPercent;
	public final int flammabilityPercent;
	public final int explosionRiskPercent;
	public final int commandCooldownPercent;
	public final int repairAmountPercent;
	public final int firefightAmountPercent;
	public final int propulsionPercent;
	public final int liftPercent;
	public final int fleetSpeedPercent;
	public final int armourRepairPercent;
	public final boolean surpriseAttack;
	
	public MedalEffect(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		level = o.getInt("level");
		fireRatePercent = o.optInt("fireRatePercent", 0);
		accuracyPercent = o.optInt("accuracyPercent", 0);
		crewSpeedPercent = o.optInt("crewSpeedPercent", 0);
		flammabilityPercent = o.optInt("flammabilityPercent", 0);
		explosionRiskPercent = o.optInt("explosionRiskPercent", 0);
		commandCooldownPercent = o.optInt("commandCooldownPercent", 0);
		repairAmountPercent = o.optInt("repairAmountPercent", 0);
		firefightAmountPercent = o.optInt("firefightAmountPercent", 0);
		propulsionPercent = o.optInt("propulsionPercent", 0);
		liftPercent = o.optInt("liftPercent", 0);
		fleetSpeedPercent = o.optInt("fleetSpeedPercent", 0);
		armourRepairPercent = o.optInt("armourRepairPercent", 0);
		surpriseAttack = o.optBoolean("surpriseAttack", false);
	}
	
	public static ArrayList<MedalEffect> forLevel(int level) {
		ArrayList<MedalEffect> l = new ArrayList<MedalEffect>();
		for (MedalEffect e : all(MedalEffect.class)) {
			if (e.level == level) {
				l.add(e);
			}
		}
		return l;
	}
	
	public static MedalEffect ofName(String name) {
		return ofName(MedalEffect.class, name);
	}
	
	public String getDesc() {
		StringBuilder sb = new StringBuilder();
		if (fireRatePercent != 0) {
			sb.append("\n").append(_t("hero_fireRatePercent", (fireRatePercent > 0 ? "+" : "") + fireRatePercent));
		}
		if (accuracyPercent != 0) {
			sb.append("\n").append(_t("hero_accuracyPercent", (accuracyPercent > 0 ? "+" : "") + accuracyPercent));
		}
		if (crewSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_crewSpeedPercent", (crewSpeedPercent > 0 ? "+" : "") + crewSpeedPercent));
		}
		if (flammabilityPercent != 0) {
			sb.append("\n").append(_t("hero_flammabilityPercent", (flammabilityPercent > 0 ? "+" : "") + flammabilityPercent));
		}
		if (explosionRiskPercent != 0) {
			sb.append("\n").append(_t("hero_explosionRiskPercent", (explosionRiskPercent > 0 ? "+" : "") + explosionRiskPercent));
		}
		if (commandCooldownPercent != 0) {
			sb.append("\n").append(_t("hero_commandCooldownPercent", (commandCooldownPercent > 0 ? "+" : "") + commandCooldownPercent));
		}
		if (repairAmountPercent != 0) {
			sb.append("\n").append(_t("hero_repairAmountPercent", (repairAmountPercent > 0 ? "+" : "") + repairAmountPercent));
		}
		if (firefightAmountPercent != 0) {
			sb.append("\n").append(_t("hero_firefightAmountPercent", (firefightAmountPercent > 0 ? "+" : "") + firefightAmountPercent));
		}
		if (propulsionPercent != 0) {
			sb.append("\n").append(_t("hero_propulsionPercent", (propulsionPercent > 0 ? "+" : "") + propulsionPercent));
		}
		if (liftPercent != 0) {
			sb.append("\n").append(_t("hero_liftPercent", (liftPercent > 0 ? "+" : "") + liftPercent));
		}
		if (surpriseAttack) {
			sb.append("\n").append(_t("hero_surprise_attack"));
		}
		if (fleetSpeedPercent != 0) {
			sb.append("\n").append(_t("hero_fleetSpeedPercent", (fleetSpeedPercent > 0 ? "+" : "") + fleetSpeedPercent));
		}
		if (armourRepairPercent != 0) {
			sb.append("\n").append(_t("hero_armourRepairPercent", armourRepairPercent));
		}
		return sb.toString().substring(1);
	}
}
