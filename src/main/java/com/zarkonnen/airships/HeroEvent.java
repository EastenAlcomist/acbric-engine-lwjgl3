package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class HeroEvent {
	public final int type;
	public final Empire e;
	public HeroEvent(int type, Empire e) { this.type = type; this.e = e; }
	
	public static class Hook {
		public final int type;
		public final String tkey;
		public final String comment;
		public final int strength;
		public Hook(int type, int strength, String tkey, String comment) { this.type = type; this.strength = strength; this.tkey = tkey; this.comment = comment; }
		public boolean check(HeroEvent evt, Hero h) {
			return type == evt.type;
		}
		public String getDesc(Hero h) { return _t(tkey); }
		public String getComment(Hero h) { return comment == null ? null : _t(comment); }
	}
	
	private static class HEvtN extends HeroEvent {
		public final Object[] os;

		public HEvtN(int type, Empire e, Object... os) {
			super(type, e);
			this.os = os;
		}
		
		private static class Hk extends Hook {
			public final HasName[] os;

			public Hk(int type, int strength, String tkey, String comment, HasName... os) {
				super(type, strength, tkey, comment);
				this.os = os;
			}
			
			@Override
			public boolean check(HeroEvent evt, Hero h) {
				if (!super.check(evt, h)) { return false; }
				HEvtN e = (HEvtN) evt;
				for (int i = 0; i < os.length; i++) { // Note that because we're looping over os.length we can do partial matches.
					if (os[i] != e.os[i]) { return false; }
				}
				return true;
			}
			
			@Override
			public String getDesc(Hero h) {
				String[] strings = new String[os.length];
				for (int i = 0; i < os.length; i++) {
					strings[i] = os[i].getName();
				}
				return _t(tkey, (Object[]) strings);
			}
			
			@Override
			public String getComment(Hero h) {
				if (comment == null) { return null; }
				String[] strings = new String[os.length];
				for (int i = 0; i < os.length; i++) {
					strings[i] = os[i].getName();
				}
				return _t(comment, (Object[]) strings);
			}
		}
	}
	
	private static class MultiHook<T extends HasName> extends Hook {
		public final ArrayList<T> os;

		public MultiHook(int type, int strength, String tkey, String comment, ArrayList<T> os) {
			super(type, strength, tkey, comment);
			this.os = os;
		}

		@Override
		public boolean check(HeroEvent evt, Hero h) {
			if (!super.check(evt, h)) { return false; }
			HEvtN e = (HEvtN) evt;
			return os.contains(e.os[0]);
		}

		@Override
		public String getDesc(Hero h) {
			return _t(tkey, os.get(0).getName());
		}

		@Override
		public String getComment(Hero h) {
			if (comment == null) { return null; }
			return _t(comment, os.get(0).getName());
		}
	}
	
	private static class HEvtStrings extends HeroEvent {
		public final String[] os;

		public HEvtStrings(int type, Empire e, String... os) {
			super(type, e);
			this.os = os;
		}
		
		private static class Hk extends Hook {
			public final String[] os;

			public Hk(int type, int strength, String tkey, String comment, String... os) {
				super(type, strength, tkey, comment);
				this.os = os;
			}
			
			@Override
			public boolean check(HeroEvent evt, Hero h) {
				if (!super.check(evt, h)) { return false; }
				HEvtStrings e = (HEvtStrings) evt;
				for (int i = 0; i < os.length; i++) { // Note that because we're looping over os.length we can do partial matches.
					if (!os[i].equals(e.os[i])) { return false; }
				}
				return true;
			}
			
			@Override
			public String getDesc(Hero h) {
				return _t(tkey, (Object[]) os);
			}
			
			@Override
			public String getComment(Hero h) {
				if (comment == null) { return null; }
				return _t(comment, (Object[]) os);
			}
		}
	}
	
	private static class OtherEmpireEvt extends HEvtN {
		final Empire other;
		
		public OtherEmpireEvt(int type, Empire e, Empire other, Object... os) {
			super(type, e, os);
			this.other = other;
		}
	}
	
	private static class NemesisEvtHook extends HEvtN.Hk {
		public NemesisEvtHook(int type, int strength, String tkey, String comment, HasName... os) {
			super(type, strength, tkey, comment, os);
		}
		
		@Override
		public boolean check(HeroEvent evt, Hero h) {
			if (!super.check(evt, h)) { return false; }
			return ((OtherEmpireEvt) evt).other == h.nemesisEmpire;
		}
		
		@Override
		public String getDesc(Hero h) {
			if (h.nemesisEmpire == null) { return "?"; }
			String[] strings = new String[os.length + 1];
			strings[0] = h.nemesisEmpire.getName();
			for (int i = 0; i < os.length; i++) {
				strings[i + 1] = os[i].getName();
			}
			return _t(tkey, (Object[]) strings);
		}
		
		@Override
		public String getComment(Hero h) {
			if (comment == null || h.nemesisEmpire == null) { return null; }
			String[] strings = new String[os.length + 1];
			strings[0] = h.nemesisEmpire.getName();
			for (int i = 0; i < os.length; i++) {
				strings[i + 1] = os[i].getName();
			}
			return _t(comment, (Object[]) strings);
		}
	}
	
	private static class BioNestHook extends HEvtN.Hk {
		public boolean isBio;
		
		public BioNestHook(int type, int strength, boolean isBio, String tkey, String comment) {
			super(type, strength, tkey, comment);
			this.isBio = isBio;
		}
		
		@Override
		public boolean check(HeroEvent evt, Hero h) {
			if (!super.check(evt, h)) { return false; }
			return ((MonsterNestType) ((HEvtN) evt).os[0]).isBiological == isBio;
		}
	}
	
	private static class OtherEmpireWithBonusEvtHook extends HEvtN.Hk {
		final Bonus bonus;
		
		public OtherEmpireWithBonusEvtHook(int type, int strength, String tkey, String comment, Bonus bonus, HasName... os) {
			super(type, strength, tkey, comment, os);
			this.bonus = bonus;
		}
		
		@Override
		public boolean check(HeroEvent evt, Hero h) {
			if (!super.check(evt, h)) { return false; }
			return ((OtherEmpireEvt) evt).other.bonuses.contains[bonus.ordinal()];
		}
		
		@Override
		public String getDesc(Hero h) {
			String[] strings = new String[os.length + 1];
			strings[0] = bonus.getName();
			for (int i = 0; i < os.length; i++) {
				strings[i + 1] = os[i].getName();
			}
			return _t(tkey, (Object[]) strings);
		}
		
		@Override
		public String getComment(Hero h) {
			if (comment == null) { return null; }
			String[] strings = new String[os.length + 1];
			strings[0] =  bonus.getName();
			for (int i = 0; i < os.length; i++) {
				strings[i + 1] = os[i].getName();
			}
			return _t(comment, (Object[]) strings);
		}
	}
	
	private static class CityEvt extends HEvtN {
		final City c;
		
		public CityEvt(int type, Empire e, City c, Object... os) {
			super(type, e, os);
			this.c = c;
		}
	}
	
	private static class HomeCityEvtHook extends HEvtN.Hk {
		public HomeCityEvtHook(int type, int strength, String tkey, String comment, HasName... os) {
			super(type, strength, tkey, comment, os);
		}
		
		@Override
		public boolean check(HeroEvent evt, Hero h) {
			if (!super.check(evt, h)) { return false; }
			return ((CityEvt) evt).c == h.homeCity;
		}
		
		@Override
		public String getDesc(Hero h) {
			if (h.homeCity == null) { return ""; }
			String[] strings = new String[os.length + 1];
			strings[0] = h.homeCity.name;
			for (int i = 0; i < os.length; i++) {
				strings[i + 1] = os[i].getName();
			}
			return _t(tkey, (Object[]) strings);
		}
		
		@Override
		public String getComment(Hero h) {
			if (comment == null || h.homeCity == null) { return null; }
			String[] strings = new String[os.length + 1];
			strings[0] = h.homeCity.name;
			for (int i = 0; i < os.length; i++) {
				strings[i + 1] = os[i].getName();
			}
			return _t(comment, (Object[]) strings);
		}
	}
	
	public static HeroType getHiredHeroType(HeroEvent evt) {
		if (evt.type == 16) {
			return (HeroType) ((HEvtN) evt).os[0];
		}
		return null;
	}
	
	public static HeroEvent.Hook hookFromJSON(JSONObject o) {
		String type = o.getString("type");
		if (type.equals("anyNestDestroyed")) {
			return anyNestDestroyedHook(o.optString("comment", null));
		}
		if (type.equals("bioNestDestroyed")) {
			return bioNestDestroyedHook(o.optString("comment", null));
		}
		if (type.equals("nonBioNestDestroyed")) {
			return nonBioNestDestroyedHook(o.optString("comment", null));
		}
		if (type.equals("nestDestroyed")) {
			return nestDestroyedHook(MonsterNestType.ofName(o.getString("nestType")), o.optString("comment", null));
		}
		if (type.equals("multiNestDestroyed")) {
			ArrayList<MonsterNestType> ns = new ArrayList<MonsterNestType>();
			JSONArray a = o.getJSONArray("nestTypes");
			for (int i = 0; i < a.length(); i++) {
				ns.add(MonsterNestType.ofName(a.getString(i)));
			}
			return multiNestDestroyedHook(ns, o.optString("comment", null));
		}
		if (type.equals("anyNestAppeared")) {
			return anyNestAppearedHook(o.optString("comment", null));
		}
		if (type.equals("bioNestAppeared")) {
			return bioNestAppearedHook(o.optString("comment", null));
		}
		if (type.equals("nonBioNestAppeared")) {
			return nonBioNestAppearedHook(o.optString("comment", null));
		}
		if (type.equals("nestAppeared")) {
			return nestAppearedHook(MonsterNestType.ofName(o.getString("nestType")), o.optString("comment", null));
		}
		if (type.equals("multiNestAppeared")) {
			ArrayList<MonsterNestType> ns = new ArrayList<MonsterNestType>();
			JSONArray a = o.getJSONArray("nestTypes");
			for (int i = 0; i < a.length(); i++) {
				ns.add(MonsterNestType.ofName(a.getString(i)));
			}
			return multiNestAppearedHook(ns, o.optString("comment", null));
		}
		if (type.equals("upgradeBuilt")) {
			return upgradeBuiltHook(CityUpgradeType.ofName(o.getString("upgradeType")), o.optString("comment", null));
		}
		if (type.equals("anyUpgradeBuilt")) {
			return anyUpgradeBuiltHook(o.optString("comment", null));
		}
		if (type.equals("takeover")) {
			return takeoverHook(TakeoverMethod.ofName(o.getString("takeoverType")), o.optString("comment", null));
		}
		if (type.equals("spyAction")) {
			return spyActionHook(Spy.CitySpyAction.valueOf(o.getString("spyActionType")), o.optString("comment", null));
		}
		if (type.equals("anySpyAction")) {
			return anySpyActionHook(o.optString("comment", null));
		}
		if (type.equals("spyActionAgainstNemesis")) {
			return spyActionAgainstNemesisHook(Spy.CitySpyAction.valueOf(o.getString("spyActionType")), o.optString("comment", null));
		}
		if (type.equals("anySpyActionAgainstNemesis")) {
			return anySpyActionAgainstNemesisHook(o.optString("comment", null));
		}
		if (type.equals("randomly")) {
			return randomlyHook(o.optString("comment", null));
		}
		if (type.equals("rarely")) {
			return rarelyHook(o.optString("comment", null));
		}
		if (type.equals("everyMonth")) {
			return everyMonthHook(o.optString("comment", null));
		}
		if (type.equals("relationshipLevelUpgrade")) {
			return relationshipLevelUpgradeHook(Relationship.Level.valueOf(o.getString("newLevel")), o.optString("comment", null));
		}
		if (type.equals("relationshipLevelUpgradeWithBonusEmpire")) {
			return relationshipLevelUpgradeWithBonusEmpireHook(Relationship.Level.valueOf(o.getString("newLevel")), Bonus.ofName(o.getString("bonus")), o.optString("comment", null));
		}
		if (type.equals("nemesisRelationshipLevelUpgrade")) {
			return nemesisRelationshipLevelUpgradeHook(Relationship.Level.valueOf(o.getString("newLevel")), o.optString("comment", null));
		}
		if (type.equals("relationshipLevelDowngrade")) {
			return relationshipLevelDowngradeHook(Relationship.Level.valueOf(o.getString("newLevel")), o.optString("comment", null));
		}
		if (type.equals("relationshipLevelDowngradeWithBonusEmpire")) {
			return relationshipLevelDowngradeWithBonusEmpireHook(Relationship.Level.valueOf(o.getString("newLevel")), Bonus.ofName(o.getString("bonus")), o.optString("comment", null));
		}
		if (type.equals("nemesisRelationshipLevelDowngrade")) {
			return nemesisRelationshipLevelDowngradeHook(Relationship.Level.valueOf(o.getString("newLevel")), o.optString("comment", null));
		}
		if (type.equals("tradeTreaty")) {
			return tradeTreatyHook(o.optString("comment", null));
		}
		if (type.equals("tradeTreatyEnded")) {
			return tradeTreatyEndedHook(o.optString("comment", null));
		}
		if (type.equals("researchTreaty")) {
			return researchTreatyHook(o.optString("comment", null));
		}
		if (type.equals("researchTreatyEnded")) {
			return researchTreatyEndedHook(o.optString("comment", null));
		}
		if (type.equals("sendTribute")) {
			return sendTributeHook(o.optString("comment", null));
		}
		if (type.equals("sendTributeToNemesis")) {
			return sendTributeToNemesisHook(o.optString("comment", null));
		}
		if (type.equals("sendTributeEnded")) {
			return sendTributeEndedHook(o.optString("comment", null));
		}
		if (type.equals("receiveTribute")) {
			return receiveTributeHook(o.optString("comment", null));
		}
		if (type.equals("receiveTributeFromNemesis")) {
			return receiveTributeFromNemesisHook(o.optString("comment", null));
		}
		if (type.equals("receiveTributeEnded")) {
			return receiveTributeEndedHook(o.optString("comment", null));
		}
		if (type.equals("demonstrateSubmission")) {
			return demonstrateSubmissionHook(o.optString("comment", null));
		}
		if (type.equals("demonstrateSubmissionToNemesis")) {
			return demonstrateSubmissionToNemesisHook(o.optString("comment", null));
		}
		if (type.equals("receiveSubmission")) {
			return receiveSubmissionHook(o.optString("comment", null));
		}
		if (type.equals("receiveSubmissionFromNemesis")) {
			return receiveSubmissionFromNemesisHook(o.optString("comment", null));
		}
		if (type.equals("cityGained")) {
			return cityGainedHook(o.optString("comment", null));
		}
		if (type.equals("homeCityGained")) {
			return homeCityGainedHook(o.optString("comment", null));
		}
		if (type.equals("cityLost")) {
			return cityLostHook(o.optString("comment", null));
		}
		if (type.equals("homeCityLost")) {
			return homeCityLostHook(o.optString("comment", null));
		}
		if (type.equals("combatVictory")) {
			return combatVictoryHook(o.optString("comment", null));
		}
		if (type.equals("combatVictoryAgainstNemesis")) {
			return combatVictoryAgainstNemesisHook(o.optString("comment", null));
		}
		if (type.equals("combatDefeat")) {
			return combatDefeatHook(o.optString("comment", null));
		}
		if (type.equals("combatDefeatAgainstNemesis")) {
			return combatDefeatAgainstNemesisHook(o.optString("comment", null));
		}
		if (type.equals("nemesisDestroyed")) {
			return nemesisDestroyedHook(o.optString("comment", null));
		}
		if (type.equals("techResearched")) {
			if (o.has("tech")) {
				return techResearchedHook(Tech.choiceOfName(o.getString("tech")), o.optString("comment", null));
			} else {
				return anyTechResearchedHook(o.optString("comment", null));
			}
		}
		if (type.equals("heroHired")) {
			return heroHiredHook(HeroType.ofName(o.getString("hero")), o.optString("comment", null));
		}
		if (type.equals("heroLeft")) {
			return heroLeftHook(HeroType.ofName(o.getString("hero")), o.optString("comment", null));
		}
		if (type.equals("repLevelUpgrade")) {
			if (o.has("level")) {
				return repLevelUpgradeHook(Empire.ReputationLevel.valueOf(o.getString("level")), o.optString("comment", null));
			} else {
				return anyRepLevelUpgradeHook(o.optString("comment", null));
			}
		}
		if (type.equals("repLevelDowngrade")) {
			if (o.has("level")) {
				return repLevelDowngradeHook(Empire.ReputationLevel.valueOf(o.getString("level")), o.optString("comment", null));
			} else {
				return anyRepLevelDowngradeHook(o.optString("comment", null));
			}
		}
		if (type.equals("incident")) {
			return incidentHook(o.getString("tag"), o.optString("comment", null));
		}
		throw new RuntimeException("Unknown hero hook type " + type);
	}
	
	public static HeroEvent nestDestroyed(Empire e, MonsterNestType n) { return new HEvtN(1, e, n); }
	public static HeroEvent.Hook nestDestroyedHook(MonsterNestType n, String comment) { return new HEvtN.Hk(1, 4, "HnestDestroyed", comment, n); }
	public static HeroEvent.Hook anyNestDestroyedHook(String comment) { return new Hook(1, 1, "HanyNestDestroyed", comment); }
	public static HeroEvent.Hook bioNestDestroyedHook(String comment) { return new BioNestHook(1, 2, true, "HbioNestDestroyed", comment); }
	public static HeroEvent.Hook nonBioNestDestroyedHook(String comment) { return new BioNestHook(1, 2, false, "HnonBioNestDestroyed", comment); }
	public static HeroEvent.Hook multiNestDestroyedHook(ArrayList<MonsterNestType> ns, String comment) { return new MultiHook(1, 4, "HnestDestroyed", comment, ns); }
	
	public static HeroEvent nestAppeared(Empire e, MonsterNestType n) { return new HEvtN(2, e, n); }
	public static HeroEvent.Hook nestAppearedHook(MonsterNestType n, String comment) { return new HEvtN.Hk(2, 4, "HnestAppeared", comment, n); }
	public static HeroEvent.Hook anyNestAppearedHook(String comment) { return new Hook(2, 1, "HanyNestAppeared", comment); }
	public static HeroEvent.Hook bioNestAppearedHook(String comment) { return new BioNestHook(2, 2, true, "HbioNestAppeared", comment); }
	public static HeroEvent.Hook nonBioNestAppearedHook(String comment) { return new BioNestHook(2, 2, true, "HnonBioNestAppeared", comment); }
	public static HeroEvent.Hook multiNestAppearedHook(ArrayList<MonsterNestType> ns, String comment) { return new MultiHook(2, 4, "HnestAppeared", comment, ns); }
	
	public static HeroEvent upgradeBuilt(Empire e, CityUpgradeType t) { return new HEvtN(3, e, t); }
	public static HeroEvent.Hook upgradeBuiltHook(CityUpgradeType t, String comment) { return new HEvtN.Hk(3, 3, "HupgradeBuilt", comment, t); }
	public static HeroEvent.Hook anyUpgradeBuiltHook(String comment) { return new Hook(3, 1, "HanyUpgradeBuilt", comment); }
	
	public static HeroEvent takeover(Empire e, City c, TakeoverMethod m) { return new CityEvt(4, e, c, m); }
	public static HeroEvent.Hook takeoverHook(TakeoverMethod m, String comment) { return new HEvtN.Hk(4, 1, "Htakeover", comment, m); }
	public static HeroEvent.Hook homeCityTakeoverHook(TakeoverMethod m, String comment) { return new HomeCityEvtHook(4, 10, "HhomeCityTakeover", comment, m); }
	
	public static HeroEvent spyAction(Empire e, Empire victim, Spy.CitySpyAction a) { return new OtherEmpireEvt(5, e, victim, a); }
	public static HeroEvent.Hook spyActionHook(Spy.CitySpyAction a, String comment) { return new HEvtN.Hk(5, 3, "HspyAction", comment, a); }
	public static HeroEvent.Hook anySpyActionHook(String comment) { return new HEvtN.Hk(5, 1, "HanySpyAction", comment); }
	public static HeroEvent.Hook spyActionAgainstNemesisHook(Spy.CitySpyAction a, String comment) { return new NemesisEvtHook(5, 10, "HspyActionAgainstNemesis", comment, a); }
	public static HeroEvent.Hook anySpyActionAgainstNemesisHook(String comment) { return new NemesisEvtHook(5, 5, "HanySpyActionAgainstNemesis", comment); }
	
	public static HeroEvent randomly(Empire e) { return new HeroEvent(6, e); }
	public static HeroEvent.Hook randomlyHook(String comment) { return new Hook(6, 1, "HrandomChance", comment); }
	
	public static HeroEvent rarely(Empire e) { return new HeroEvent(20, e); }
	public static HeroEvent.Hook rarelyHook(String comment) { return new Hook(20, 1, "HrandomChance", comment); }
	
	public static HeroEvent everyMonth(Empire e) { return new HeroEvent(7, e); }
	public static HeroEvent.Hook everyMonthHook(String comment) { return new Hook(7, 1, "HeveryMonth", comment); }
	
	public static HeroEvent relationshipLevelUpgrade(Empire e, Empire other, Relationship.Level newLevel) {
		return new OtherEmpireEvt(8, e, other, newLevel);
	}
	public static HeroEvent.Hook relationshipLevelUpgradeHook(Relationship.Level newLevel, String comment) {
		return new HEvtN.Hk(8, 1, "HupgradeLevel", comment, newLevel);
	}
	public static HeroEvent.Hook relationshipLevelUpgradeWithBonusEmpireHook(Relationship.Level newLevel, Bonus b, String comment) {
		return new OtherEmpireWithBonusEvtHook(8, 4, "HupgradeLevelWithBonus", comment, b, newLevel);
	}
	public static HeroEvent.Hook nemesisRelationshipLevelUpgradeHook(Relationship.Level newLevel, String comment) {
		return new NemesisEvtHook(8, 5, "HnemesisUpgradeLevel", comment, newLevel);
	}
	public static HeroEvent relationshipLevelDowngrade(Empire e, Empire other, Relationship.Level newLevel) {
		return new OtherEmpireEvt(9, e, other, newLevel);
	}
	public static HeroEvent.Hook relationshipLevelDowngradeWithBonusEmpireHook(Relationship.Level newLevel, Bonus b, String comment) {
		return new OtherEmpireWithBonusEvtHook(9, 4, "HdowngradeLevelWithBonus", comment, b, newLevel);
	}
	public static HeroEvent.Hook relationshipLevelDowngradeHook(Relationship.Level newLevel, String comment) {
		return new HEvtN.Hk(9, 1, "HdowngradeLevel", comment, newLevel);
	}
	public static HeroEvent.Hook nemesisRelationshipLevelDowngradeHook(Relationship.Level newLevel, String comment) {
		return new NemesisEvtHook(9, 5, "HnemesisDowngradeLevel", comment, newLevel);
	}
	
	public static HeroEvent cityGained(Empire e, City c) { return new CityEvt(10, e, c); }
	public static HeroEvent.Hook cityGainedHook(String comment) { return new Hook(10, 1, "HcityGained", comment); }
	public static HeroEvent.Hook homeCityGainedHook(String comment) { return new HomeCityEvtHook(10, 10, "HhomeCityGained", comment); }
	public static HeroEvent cityLost(Empire e, City c) { return new CityEvt(11, e, c); }
	public static HeroEvent.Hook cityLostHook(String comment) { return new Hook(11, 1, "HcityLost", comment); }
	public static HeroEvent.Hook homeCityLostHook(String comment) { return new HomeCityEvtHook(11, 10, "HhomeCityLost", comment); }
	
	public static HeroEvent combatVictory(Empire e, Empire other) { return new OtherEmpireEvt(12, e, other); }
	public static HeroEvent.Hook combatVictoryHook(String comment) { return new Hook(12, 1, "HcombatVictory", comment); }
	public static HeroEvent.Hook combatVictoryAgainstNemesisHook(String comment) { return new NemesisEvtHook(12, 5, "HcombatVictoryAgainstNemesis", comment); }
	public static HeroEvent combatDefeat(Empire e, Empire other) { return new OtherEmpireEvt(18, e, other); }
	public static HeroEvent.Hook combatDefeatHook(String comment) { return new Hook(18, 1, "HcombatDefeat", comment); }
	public static HeroEvent.Hook combatDefeatAgainstNemesisHook(String comment) { return new NemesisEvtHook(18, 5, "HcombatDefeatAgainstNemesis", comment); }
	
	public static HeroEvent repLevelUpgrade(Empire e, Empire.ReputationLevel l) { return new HEvtN(13, e, l); }
	public static HeroEvent.Hook anyRepLevelUpgradeHook(String comment) { return new Hook(13, 1, "HrepAnyLevelUpgrade", comment); }
	public static HeroEvent.Hook repLevelUpgradeHook(Empire.ReputationLevel l, String comment) { return new HEvtN.Hk(13, 1, "HrepLevelUpgrade", comment, l); }
	
	public static HeroEvent repLevelDowngrade(Empire e, Empire.ReputationLevel l) { return new HEvtN(14, e, l); }
	public static HeroEvent.Hook anyRepLevelDowngradeHook(String comment) { return new Hook(14, 1, "HrepAnyLevelDowngrade", comment); }
	public static HeroEvent.Hook repLevelDowngradeHook(Empire.ReputationLevel l, String comment) { return new HEvtN.Hk(14, 1, "HrepLevelDowngrade", comment, l); }
	
	public static HeroEvent techResearched(Empire e, Tech.Choice t) { return new HEvtN(15, e, t); }
	public static HeroEvent.Hook anyTechResearchedHook(String comment) { return new Hook(15, 1, "HanyTechResearched", comment); }
	public static HeroEvent.Hook techResearchedHook(Tech.Choice t, String comment) { return new HEvtN.Hk(15, 4, "HtechResearched", comment, t); }
	
	public static HeroEvent heroHired(Empire e, HeroType t) { return new HEvtN(16, e, t); }
	public static HeroEvent.Hook heroHiredHook(HeroType t, String comment) { return new HEvtN.Hk(16, 1, "HheroHired", comment, t); }
	
	public static HeroEvent heroLeft(Empire e, HeroType t) { return new HEvtN(17, e, t); }
	public static HeroEvent.Hook heroLeftHook(HeroType t, String comment) { return new HEvtN.Hk(17, 1, "HheroLeft", comment, t); }	
	
	public static HeroEvent incident(Empire e, String tag) { return new HEvtStrings(19, e, tag); }
	public static HeroEvent.Hook incidentHook(String tag, String comment) { return new HEvtStrings.Hk(19, 1, "H" + tag, comment, tag); }
	
	public static HeroEvent tradeTreaty(Empire e, Empire other) {
		return new OtherEmpireEvt(21, e, other);
	}
	public static HeroEvent.Hook tradeTreatyHook(String comment) {
		return new HEvtN.Hk(21, 1, "HtradeTreaty", comment);
	}
	
	public static HeroEvent tradeTreatyEnded(Empire e, Empire other) {
		return new OtherEmpireEvt(22, e, other);
	}
	public static HeroEvent.Hook tradeTreatyEndedHook(String comment) {
		return new HEvtN.Hk(22, 1, "HtradeTreatyEnded", comment);
	}
	
	public static HeroEvent researchTreaty(Empire e, Empire other) {
		return new OtherEmpireEvt(23, e, other);
	}
	public static HeroEvent.Hook researchTreatyHook(String comment) {
		return new HEvtN.Hk(23, 1, "HresearchTreaty", comment);
	}
	
	public static HeroEvent researchTreatyEnded(Empire e, Empire other) {
		return new OtherEmpireEvt(24, e, other);
	}
	public static HeroEvent.Hook researchTreatyEndedHook(String comment) {
		return new HEvtN.Hk(24, 1, "HresearchTreatyEnded", comment);
	}
	
	public static HeroEvent sendTribute(Empire e, Empire other) {
		return new OtherEmpireEvt(25, e, other);
	}
	public static HeroEvent.Hook sendTributeHook(String comment) {
		return new HEvtN.Hk(25, 1, "HsendTribute", comment);
	}
	public static HeroEvent.Hook sendTributeToNemesisHook(String comment) {
		return new NemesisEvtHook(25, 5, "HsendTributeToNemesis", comment);
	}
	
	public static HeroEvent sendTributeEnded(Empire e, Empire other) {
		return new OtherEmpireEvt(26, e, other);
	}
	public static HeroEvent.Hook sendTributeEndedHook(String comment) {
		return new HEvtN.Hk(26, 1, "HsendTributeEnded", comment);
	}
	
	public static HeroEvent receiveTribute(Empire e, Empire other) {
		return new OtherEmpireEvt(27, e, other);
	}
	public static HeroEvent.Hook receiveTributeHook(String comment) {
		return new HEvtN.Hk(27, 1, "HreceiveTribute", comment);
	}
	public static HeroEvent.Hook receiveTributeFromNemesisHook(String comment) {
		return new NemesisEvtHook(27, 5, "HreceiveTributeFromNemesis", comment);
	}
	
	public static HeroEvent receiveTributeEnded(Empire e, Empire other) {
		return new OtherEmpireEvt(28, e, other);
	}
	public static HeroEvent.Hook receiveTributeEndedHook(String comment) {
		return new HEvtN.Hk(28, 1, "HreceiveTributeEnded", comment);
	}
	
	public static HeroEvent demonstrateSubmission(Empire e, Empire other) {
		return new OtherEmpireEvt(29, e, other);
	}
	public static HeroEvent.Hook demonstrateSubmissionHook(String comment) {
		return new HEvtN.Hk(29, 1, "HdemonstrateSubmission", comment);
	}
	public static HeroEvent.Hook demonstrateSubmissionToNemesisHook(String comment) {
		return new NemesisEvtHook(29, 1, "HdemonstrateSubmissionToNemesis", comment);
	}
	
	public static HeroEvent receiveSubmission(Empire e, Empire other) {
		return new OtherEmpireEvt(30, e, other);
	}
	public static HeroEvent.Hook receiveSubmissionHook(String comment) {
		return new HEvtN.Hk(30, 1, "HreceiveSubmission", comment);
	}
	public static HeroEvent.Hook receiveSubmissionFromNemesisHook(String comment) {
		return new NemesisEvtHook(30, 5, "HreceiveSubmissionFromNemesis", comment);
	}
	
	public static HeroEvent empireDestroyed(Empire e, Empire other) {
		return new OtherEmpireEvt(31, e, other);
	}
	public static HeroEvent.Hook nemesisDestroyedHook(String comment) {
		return new NemesisEvtHook(31, 1, "HnemesisDestroyed", comment);
	}
	
	public static final int MONTHLY = 7;
	public static final int RARELY = 20;
	
	public int strength() {
		switch (type) {
			case 1: return 3; //"nestDestroyed";
			case 2: return 8; //"nestAppeared";
			case 3: return 1; //"upgradeBuilt";
			case 4: return 1; //"takeover";
			case 5: return 1; //"spyAction";
			case 6: return 1; //"randomly";
			case 20: return 1; //"rarely";
			case 7: return 1; //"everyMonth";
			case 8: return 2; //"relationshipLevelUpgrade";
			case 9: return 2; //"relationshipLevelDowngrade";
			case 10: return 1; //"cityGained";
			case 11: return 2; //"cityLost";
			case 12: return 1; //"combatVictory";
			case 18: return 1; //"combatDefeat";
			case 13: return 10; //"repLevelUpgrade";
			case 14: return 10; //"repLevelDowngrade";
			case 15: return 3; //"techResearched";
			case 16: return 1; //"heroHired";
			case 17: return 1; //"heroLeft";
			case 19: return 10; //"incident";
			case 21: return 2; //"tradeTreaty";
			case 22: return 2; //"tradeTreatyEnded";
			case 23: return 2; //"researchTreaty";
			case 24: return 2; //"researchTreatyEnded";
			case 25: return 3; //"sendTribute";
			case 26: return 2; //"sendTributeEnded";
			case 27: return 3; //"receiveTribute";
			case 28: return 2; //"receiveTributeEnded";
			case 29: return 3; //"demonstrateSubmission";
			case 30: return 3; //"receiveSubmission";
			case 31: return 3; //"empireDestroyed";
			default: return 1; //"?";
		} 
	}
	
	@Override
	public String toString() {
		switch (type) {
			case 1: return "nestDestroyed";
			case 2: return "nestAppeared";
			case 3: return "upgradeBuilt";
			case 4: return "takeover";
			case 5: return "spyAction";
			case 6: return "randomly";
			case 20: return "rarely";
			case 7: return "everyMonth";
			case 8: return "relationshipLevelUpgrade";
			case 9: return "relationshipLevelDowngrade";
			case 10: return "cityGained";
			case 11: return "cityLost";
			case 12: return "combatVictory";
			case 18: return "combatDefeat";
			case 13: return "repLevelUpgrade";
			case 14: return "repLevelDowngrade";
			case 15: return "techResearched";
			case 16: return "heroHired";
			case 17: return "heroLeft";
			case 19: return "incident";
			case 21: return "tradeTreaty";
			case 22: return "tradeTreatyEnded";
			case 23: return "researchTreaty";
			case 24: return "researchTreatyEnded";
			case 25: return "sendTribute";
			case 26: return "sendTributeEnded";
			case 27: return "receiveTribute";
			case 28: return "receiveTributeEnded";
			case 29: return "demonstrateSubmission";
			case 30: return "receiveSubmission";
			case 31: return "empireDestroyed";
			default: return "?";
		}
	}
}
