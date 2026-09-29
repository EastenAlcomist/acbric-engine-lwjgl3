package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import java.util.EnumSet;
import org.json.JSONObject;

public strictfp class TakeoverMethod extends Loadable implements HasName {
	public final BonusableValue<Integer> timeTaken;
	public final BonusableValue<Double> incomeMultiplier;
	public final BonusableValue<Double> supplyMultiplier;
	public final BonusableValue<Integer> economicDamage;
	public final BonusableValue<Integer> reputationChange;
	public final BonusableValue<Integer> unrest;
	public final BonusableValue<Integer> unrestPeriod;
	public final Bonus required;

	public final Img img;
	
	public TakeoverMethod(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 0));
		timeTaken = BonusableValue.intFromJSON(o, "timeTaken", 60000);
		incomeMultiplier = BonusableValue.doubleFromJSON(o, "incomeMultiplier", 1);
		supplyMultiplier = BonusableValue.doubleFromJSON(o, "supplyMultiplier", 1);
		economicDamage = BonusableValue.intFromJSON(o, "economicDamage", 0);
		reputationChange = BonusableValue.intFromJSON(o, "reputationChange", 0);
		unrest = BonusableValue.intFromJSON(o, "unrest", 0);
		unrestPeriod = BonusableValue.intFromJSON(o, "unrestPeriod", 0);
		img = new Img("takeover_" + o.getString("name"));
		required = o.has("required") ? Bonus.ofName(o.getString("required")) : null;
	}
	
	public static ArrayList<TakeoverMethod> getAvailable(BonusSet bs) {
		ArrayList<TakeoverMethod> l = new ArrayList<TakeoverMethod>();
		for (TakeoverMethod m : all(TakeoverMethod.class)) {
			if (m.required == null || bs.contains[m.required.ordinal()]) {
				l.add(m);
			}
		}
		return l;
	}
	
	public String getName() {
		return _t("takeover_" + name);
	}
	
	public int repChange(Empire conqueror, Empire owner) {
		if (owner == null) { return 0; }
		int repDelta = reputationChange.get(conqueror.bonuses);
		if (owner.getReputationLevel() == Empire.ReputationLevel.HATED &&
			conqueror.getReputationLevel() != Empire.ReputationLevel.HATED &&
			EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(owner.bonuses) != 0)
		{
			repDelta += EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(owner.bonuses);
		}
		if (owner.getReputationLevel() == Empire.ReputationLevel.LOVED &&
			conqueror.getReputationLevel() != Empire.ReputationLevel.LOVED &&
			EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(owner.bonuses) != 0)
		{
			repDelta -= EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(owner.bonuses);
		}
		return repDelta;
	}
	
	public String getDescription(EnumSet<ConquestToggle> toggles, City city, Empire conqueror, Empire prevOwner, WorldMap m) {
		BonusSet bonuses = conqueror.bonuses;
		StringBuilder sb = new StringBuilder();
		sb.append(_t("takeover_tooltip_" + name));
		ArrayList<String> notices = new ArrayList<String>();
		int repDelta = 0;
		notices.addAll(timeTaken.descriptions(bonuses));
		notices.addAll(incomeMultiplier.descriptions(bonuses));
		notices.addAll(supplyMultiplier.descriptions(bonuses));
		notices.addAll(economicDamage.descriptions(bonuses));
		if (toggles.contains(ConquestToggle.REPUTATION) && prevOwner != null) {
			notices.addAll(reputationChange.descriptions(bonuses));
			if (prevOwner.getReputationLevel() == Empire.ReputationLevel.HATED &&
				conqueror.getReputationLevel() != Empire.ReputationLevel.HATED &&
				EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(prevOwner.bonuses) != 0)
			{
				notices.add(_t("conquer_hated_city_rep_gain_notice", city.name, prevOwner.getName(), EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(prevOwner.bonuses)));
				repDelta = EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(prevOwner.bonuses);
			}
			if (prevOwner.getReputationLevel() == Empire.ReputationLevel.LOVED &&
				conqueror.getReputationLevel() != Empire.ReputationLevel.LOVED &&
				EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(prevOwner.bonuses) != 0)
			{
				notices.add(_t("conquer_loved_city_rep_loss_notice", city.name, prevOwner.getName(), EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(prevOwner.bonuses)));
				repDelta = -EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(prevOwner.bonuses);
			}
		}
		sb.append(FormatUtils.stringList(notices, "\n\n", "\n\n", ""));
		sb.append("\n");
		sb.append("\n").append(_t("takeover_timeTaken", timeTaken.explain(bonuses, new FormatUtils.TimeFormatter(bonuses))));
		if (incomeMultiplier.get(bonuses) != 1) {
			sb.append("\n").append(_t("takeover_incomeMultiplier", incomeMultiplier.explain(bonuses, FormatUtils.MULTIPLIER)));
		}
		if (supplyMultiplier.get(bonuses) != 1) {
			sb.append("\n").append(_t("takeover_supplyMultiplier", supplyMultiplier.explain(bonuses, FormatUtils.MULTIPLIER)));
		}
		if (economicDamage.get(bonuses) != 0) {
			sb.append("\n").append(_t("takeover_economicDamage", economicDamage.explain(bonuses)));
		}
		if (unrest.get(bonuses) != 0) {
			sb.append("\n").append(_t("takeover_postUnrest", unrest.explain(bonuses), unrestPeriod.explain(bonuses, new FormatUtils.TimeFormatter(bonuses))));
		}
		if (toggles.contains(ConquestToggle.REPUTATION) && reputationChange.get(bonuses) != 0) {
			sb.append("\n").append(_t("takeover_reputationChange", reputationChange.explain(bonuses)));
		}
		if (toggles.contains(ConquestToggle.REPUTATION) && repDelta > 0) {
			sb.append("\n").append(_t("takeover_reputationChange_extra", "+" + repDelta));
		}
		if (toggles.contains(ConquestToggle.REPUTATION) && repDelta < 0) {
			sb.append("\n").append(_t("takeover_reputationChange_extra", repDelta));
		}
		sb.append(Hero.getStatChangeAppendix(conqueror, HeroEvent.takeover(conqueror, city, this), m, false));
		sb.append(conqueror.getRepChangeHeroAppendix(reputationChange.get(bonuses) + repDelta, m, false));
		return sb.toString();
	}
	
	public String getTooltip(BonusSet bonuses) {
		return _t("takeover_tooltip_" + name) + "\n" + timeTaken.explain(bonuses, new FormatUtils.TimeFormatter(bonuses));
	}

	public static TakeoverMethod ofName(String name) {
		return ofName(TakeoverMethod.class, name);
	}
}
