package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.Relationship.Level;
import static com.zarkonnen.airships.Relationship.Level.*;
import com.zarkonnen.airships.Relationship.Offer;
import java.util.ArrayList;
import static com.zarkonnen.airships.Relationship.Challenge.Type.DELEGATION;
import static com.zarkonnen.airships.Relationship.Challenge.Type.INSULT;
import static com.zarkonnen.airships.Relationship.Direction.A_TO_B;
import static com.zarkonnen.airships.Relationship.Direction.B_TO_A;
import static com.zarkonnen.airships.Relationship.Direction.NEITHER;
import com.zarkonnen.airships.Relationship.Ultimatum;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.json.JSONObject;

public class DiplomacyAI {
	public static final int RECENT_AGREED_EXPIRATION = 60 * WorldMap.DAYS_PER_WEEK * WorldMap.MS_PER_DAY;
	public static final int RECENT_OFFER_EXPIRATION = 180 * WorldMap.DAYS_PER_WEEK * WorldMap.MS_PER_DAY;
	public static final int RECENT_ACCEPTED_ULT_EXPIRATION = 120 * WorldMap.DAYS_PER_WEEK * WorldMap.MS_PER_DAY;
	public static final int EXPIRATION_TOLERANCE_PER_QUALITY = WorldMap.MS_PER_DAY;
	
	public final DiplomacyPersonality personality;
	private final DiplomacyPersonality base = new DiplomacyPersonality();
	public int diplomacyCooldown = 0;
	public int accedeToUltimatumCooldown = 0;
	
	public HashMap<Empire, EmpireRecord> records = new HashMap<Empire, EmpireRecord>();
	public Empire activeEmpire;
	
	public EmpireRecord rec(JSONObject o, WorldMap m) {
		return new EmpireRecord(o, m);
	}
	
	static EnumMap<Relationship.Level, Integer> levelCounts = new EnumMap<Relationship.Level, Integer>(Relationship.Level.class);
	static int tradeCount, researchCount, tributeCount, paymentCount, submissionCount, cityTransferCount;
	private void countAIAgreement(Offer offer) {
		/*if (offer.newLevel != null) {
			if (!levelCounts.containsKey(offer.newLevel)) {
				levelCounts.put(offer.newLevel, 1);
			} else {
				levelCounts.put(offer.newLevel, levelCounts.get(offer.newLevel) + 1);
			}
		}
		if (offer.newTradeTreaty != null && offer.newTradeTreaty) {
			tradeCount++;
		}
		if (offer.newResearchTreaty != null && offer.newResearchTreaty) {
			researchCount++;
		}
		if (offer.newTribute != null && offer.newTribute != Relationship.Direction.NEITHER) {
			tributeCount++;
		}
		if (offer.moneyTransferAToB != 0) {
			paymentCount++;
		}
		if (offer.submissionAToB != null) {
			submissionCount++;
		}
		cityTransferCount += offer.cityTransfers.size();
		System.out.println("\nAI Agreements:");
		for (Relationship.Level l : Relationship.Level.values()) {
			if (levelCounts.containsKey(l)) {
				System.out.println(l.name() + ": " + levelCounts.get(l));
			}
		}
		if (tradeCount != 0) { System.out.println("Trade: " + tradeCount); }
		if (researchCount != 0) { System.out.println("Research: " + researchCount); }
		if (tributeCount != 0) { System.out.println("Tribute: " + tributeCount); }
		if (paymentCount != 0) { System.out.println("Payment: " + paymentCount); }
		if (submissionCount != 0) { System.out.println("Submission: " + submissionCount); }
		if (cityTransferCount != 0) { System.out.println("Transfers: " + cityTransferCount); }*/
	}

	public class EmpireRecord {
		public Empire e;
		public Relationship.Level recentLevel;
		
		private EnumMap<Level, Integer> levelOfferCooldowns = new EnumMap<Level, Integer>(Level.class);
		private int tradeTreatyOfferCooldown = 0;
		private int researchTreatyOfferCooldown = 0;
		private EnumMap<Relationship.Direction, Integer> tributeOfferCooldowns = new EnumMap<Relationship.Direction, Integer>(Relationship.Direction.class);
		private int moneyTransferOfferCooldown = 0;
		private int submissionOfferCooldown = 0;
		private int cityTransferOfferCooldown = 0;
		public boolean annexOffered = false;
		
		private EnumMap<Level, Integer> levelAgreeCooldowns = new EnumMap<Level, Integer>(Level.class);
		private int tradeTreatyAgreeCooldown = 0;
		private int researchTreatyAgreeCooldown = 0;
		public EnumMap<Relationship.Direction, Integer> tributeAgreeCooldowns = new EnumMap<Relationship.Direction, Integer>(Relationship.Direction.class);
		
		public int warAggression;
		public int numCitiesForWarAggression;
		
		public JSONObject toJSON() {
			JSONObject o = new JSONObject();
			o.put("empire", e.id);
			if (recentLevel != null) {
				o.put("recentLevel", recentLevel.name());
			}
			
			for (Level l : Level.values()) {
				o.put("levelOfferAge_" + l.name(), levelOfferCooldowns.get(l));
			}
			o.put("tradeTreatyOfferAge", tradeTreatyOfferCooldown);
			o.put("researchTreatyOfferAge", researchTreatyOfferCooldown);
			for (Relationship.Direction d : Relationship.Direction.values()) {
				o.put("tributeOfferAge_" + d.name(), tributeOfferCooldowns.get(d));
			}
			o.put("moneyTransferOfferCooldown", moneyTransferOfferCooldown);
			o.put("submissionOfferCooldown", submissionOfferCooldown);
			o.put("cityTransferOfferCooldown", cityTransferOfferCooldown);
			o.put("annexOffered", annexOffered);
			
			for (Level l : Level.values()) {
				o.put("levelAgreeAge_" + l.name(), levelAgreeCooldowns.get(l));
			}
			o.put("tradeTreatyAgreeAge", tradeTreatyAgreeCooldown);
			o.put("researchTreatyAgreeAge", researchTreatyAgreeCooldown);
			for (Relationship.Direction d : Relationship.Direction.values()) {
				o.put("tributeAgreeAge_" + d.name(), tributeAgreeCooldowns.get(d));
			}
			
			o.put("warAggression", warAggression);
			o.put("numCitiesForWarAggression", numCitiesForWarAggression);
			
			return o;
		}
		
		public EmpireRecord(JSONObject o, WorldMap m) {
			e = m.getEmpire(o.getInt("empire"));
			recentLevel = o.has("recentLevel") ? valueOf(o.getString("recentLevel")) : valueOf("PEACE");
			
			for (Level l : Level.values()) {
				levelOfferCooldowns.put(l, o.getInt("levelOfferAge_" + l.name()));
			}
			tradeTreatyOfferCooldown = o.getInt("tradeTreatyOfferAge");
			researchTreatyOfferCooldown = o.getInt("researchTreatyOfferAge");
			for (Relationship.Direction d : Relationship.Direction.values()) {
				tributeOfferCooldowns.put(d, o.getInt("tributeOfferAge_" + d.name()));
			}
			moneyTransferOfferCooldown = o.optInt("moneyTransferOfferCooldown", 0);
			submissionOfferCooldown = o.optInt("submissionOfferCooldown", 0);
			cityTransferOfferCooldown = o.optInt("cityTransferOfferCooldown", 0);
			annexOffered = o.optBoolean("annexOffered", false);
			
			for (Level l : Level.values()) {
				levelAgreeCooldowns.put(l, o.getInt("levelAgreeAge_" + l.name()));
			}
			tradeTreatyAgreeCooldown = o.getInt("tradeTreatyAgreeAge");
			researchTreatyAgreeCooldown = o.getInt("researchTreatyAgreeAge");
			for (Relationship.Direction d : Relationship.Direction.values()) {
				tributeAgreeCooldowns.put(d, o.getInt("tributeAgreeAge_" + d.name()));
			}
			
			warAggression = o.optInt("warAggression", 0);
			numCitiesForWarAggression = o.optInt("numCitiesForWarAggression", 0);
		}
		
		public EmpireRecord(Empire e) {
			this.e = e;
			for (Level l : Level.values()) {
				levelOfferCooldowns.put(l, 0);
			}
			for (Relationship.Direction d : Relationship.Direction.values()) {
				tributeOfferCooldowns.put(d, 0);
			}
			for (Level l : Level.values()) {
				levelAgreeCooldowns.put(l, 0);
			}
			for (Relationship.Direction d : Relationship.Direction.values()) {
				tributeAgreeCooldowns.put(d, 0);
			}
		}
		
		public void addAgreed(Offer o) {
			addAgreed(o, RECENT_AGREED_EXPIRATION);
		}
		
		public void addAccepted(Ultimatum ult) {
			addAgreed(ult.getDemand(), RECENT_AGREED_EXPIRATION);
		}
		
		public void addOffered(Offer o) {
			addOffered(o, RECENT_OFFER_EXPIRATION);
		}
		
		public void addOffered(Ultimatum ult) {
			addOffered(ult.getDemand(), RECENT_OFFER_EXPIRATION);
		}
		
		private void addAgreed(Offer o, int expiration) {
			//System.out.println("addAgreed " + describeOffer(o.rel.other(e), o));
			if (o.newLevel != null) {
				levelAgreeCooldowns.put(o.newLevel, StrictMath.max(levelAgreeCooldowns.get(o.newLevel), expiration));
			}
			if (o.newTradeTreaty != null) {
				tradeTreatyAgreeCooldown = StrictMath.max(tradeTreatyAgreeCooldown, expiration);
			}
			if (o.newResearchTreaty != null) {
				researchTreatyAgreeCooldown = StrictMath.max(researchTreatyAgreeCooldown, expiration);
			}
			if (o.newTribute != null) {
				tributeAgreeCooldowns.put(o.newTribute, StrictMath.max(tributeAgreeCooldowns.get(o.newTribute), expiration));
			}
		}
		
		private void addOffered(Offer o, int expiration) {
			//System.out.println("addOffered " + describeOffer(o.rel.other(e), o));
			if (o.newLevel != null) {
				levelOfferCooldowns.put(o.newLevel, StrictMath.max(levelOfferCooldowns.get(o.newLevel), expiration));
			}
			if (o.newTradeTreaty != null) {
				tradeTreatyOfferCooldown = StrictMath.max(tradeTreatyOfferCooldown, expiration);
			}
			if (o.newResearchTreaty != null) {
				researchTreatyOfferCooldown = StrictMath.max(researchTreatyOfferCooldown, expiration);
			}
			if (o.newTribute != null) {
				tributeOfferCooldowns.put(o.newTribute, StrictMath.max(tributeOfferCooldowns.get(o.newTribute), expiration));
			}
			if (o.moneyTransferAToB != 0) {
				moneyTransferOfferCooldown = expiration;
			}
			if (o.submissionAToB != null) {
				submissionOfferCooldown = expiration;
			}
			if (!o.cityTransfers.isEmpty()) {
				cityTransferOfferCooldown = expiration;
			}
			if (o.annexationAToB != null) {
				annexOffered = true;
			}
		}
		
		/* Whether an offer's term were agreed to and offered long enough ago that it's OK to ask again. */
		public boolean canAsk(Offer o, HasRelationships m) {
			if (!canForce(o, m)) { return false; }
			Empire me = o.rel.other(e);
			//System.out.println("canAsk " + describeOffer(me, o));
			if (o.newLevel != null) {
				int cooldown = StrictMath.max(levelOfferCooldowns.get(o.rel.level), levelOfferCooldowns.get(o.newLevel));
				int eval = newLevelQuality(me, e, o.newLevel, o.rel, m, null);
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of " + o.newLevel.name()+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of " + o.newLevel.name()+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.newTradeTreaty != null) {
				int cooldown = tradeTreatyOfferCooldown;
				int eval = tradeTreatyQuality(me, e, m, null);
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of trade " + o.newTradeTreaty+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of trade " + o.newTradeTreaty+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.newResearchTreaty != null) {
				int cooldown = researchTreatyOfferCooldown;
				int eval = researchTreatyQuality(me, e, m, null);
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of research " + o.newResearchTreaty+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of research " + o.newResearchTreaty+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.newTribute != null) {
				int cooldown = tributeOfferCooldowns.get(o.newTribute);
				if (o.rel.tribute != null) {
					cooldown = StrictMath.max(cooldown, tributeOfferCooldowns.get(o.rel.tribute));
				}
				int oldQuality = 0;
				if (o.rel.getSendingTribute(me)) {
					oldQuality = giveTributeQuality(me, e, m, null);
				}
				if (o.rel.getReceivingTribute(me)) {
					oldQuality = receiveTributeQuality(me, e, m, null);
				}
				int newQuality = 0;
				if (o.getSendingTribute(me)) {
					newQuality = giveTributeQuality(me, e, m, null);
				}
				if (o.getReceivingTribute(me)) {
					newQuality = receiveTributeQuality(me, e, m, null);
				}
				int eval = newQuality - oldQuality;
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of tribute age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of tribute age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.moneyTransferAToB != 0 && moneyTransferOfferCooldown - 3 * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
				return false;
			}
			if (o.submissionAToB != null && submissionOfferCooldown - 5 * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
				return false;
			}
			if (!o.cityTransfers.isEmpty() && cityTransferOfferCooldown > 0) {
				return false;
			}
			if (o.annexationAToB != null && annexOffered) {
				return false;
			}
			return true;
		}
		
		/* Whether an offer's terms were agreed to long enough ago that they can be forced to be different. */
		public boolean canForce(Offer o, HasRelationships m) {
			if (o.newLevel == WAR || o.newLevel == TRUCE) { return true; } // If we're declaring war anyway, we don't care, and if we want to make a truce, we don't care.
			
			Empire me = o.rel.other(e);
			//System.out.println("canForce " + describeOffer(me, o));
			if (o.newLevel != null) {
				int cooldown = StrictMath.max(levelAgreeCooldowns.get(o.rel.level), levelAgreeCooldowns.get(o.newLevel));
				int eval = newLevelQuality(me, e, o.newLevel, o.rel, m, null);
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of " + o.newLevel.name()+ " cooldown " + cooldown + " vs " + StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of " + o.newLevel.name()+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.newTradeTreaty != null) {
				int cooldown = tradeTreatyAgreeCooldown;
				int eval = tradeTreatyQuality(me, e, m, null);
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of trade " + o.newTradeTreaty+ " cooldown " + cooldown + " vs " + StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of trade " + o.newTradeTreaty+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.newResearchTreaty != null) {
				int cooldown = researchTreatyAgreeCooldown;
				int eval = researchTreatyQuality(me, e, m, null);
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println("rejected " + describeOffer(me, o) + " because of research " + o.newResearchTreaty+ " cooldown " + cooldown + " vs " + StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println("passed " + describeOffer(me, o) + " because of research " + o.newResearchTreaty+ " age " + age + " vs " + StrictMath.abs(eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			if (o.newTribute != null) {
				int cooldown = tributeAgreeCooldowns.get(o.newTribute);
				if (o.rel.tribute != null) {
					cooldown = StrictMath.max(cooldown, tributeAgreeCooldowns.get(o.rel.tribute));
				}
				int oldQuality = 0;
				if (o.rel.getSendingTribute(me)) {
					oldQuality = giveTributeQuality(me, e, m, null);
				}
				if (o.rel.getReceivingTribute(me)) {
					oldQuality = receiveTributeQuality(me, e, m, null);
				}
				int newQuality = 0;
				if (o.getSendingTribute(me)) {
					newQuality = giveTributeQuality(me, e, m, null);
				}
				if (o.getReceivingTribute(me)) {
					newQuality = receiveTributeQuality(me, e, m, null);
				}
				int eval = newQuality - oldQuality;
				if (cooldown - StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY > 0) {
					//System.out.println(me.name + " rejected " + describeOffer(me, o) + " because of tribute cooldown " + cooldown + " vs " + StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
					return false;
				}
				//System.out.println(me.name + " passed " + describeOffer(me, o) + " because of tribute age " + cooldown + " vs " + StrictMath.max(0, eval) * EXPIRATION_TOLERANCE_PER_QUALITY);
			}
			return true;
		}
		
		public void tick(int ms, Relationship rel) {
			if (rel.level != recentLevel) {
				recentLevel = rel.level;
				for (Map.Entry<Level, Integer> e : levelOfferCooldowns.entrySet()) {
					e.setValue(0);
				}
				tradeTreatyOfferCooldown = 0;
				researchTreatyOfferCooldown = 0;
				for (Map.Entry<Relationship.Direction, Integer> e : tributeOfferCooldowns.entrySet()) {
					e.setValue(0);
				}
				moneyTransferOfferCooldown = 0;
				submissionOfferCooldown = 0;
				cityTransferOfferCooldown = 0;
			} else {
				for (Map.Entry<Level, Integer> e : levelOfferCooldowns.entrySet()) {
					e.setValue(StrictMath.max(0, e.getValue() - ms));
				}
				tradeTreatyOfferCooldown = StrictMath.max(0, tradeTreatyOfferCooldown - ms);
				researchTreatyOfferCooldown = StrictMath.max(0, researchTreatyOfferCooldown - ms);
				for (Map.Entry<Relationship.Direction, Integer> e : tributeOfferCooldowns.entrySet()) {
					e.setValue(StrictMath.max(0, e.getValue() - ms));
				}
				moneyTransferOfferCooldown = StrictMath.max(0, moneyTransferOfferCooldown - ms);
				submissionOfferCooldown = StrictMath.max(0, submissionOfferCooldown - ms);
				cityTransferOfferCooldown = StrictMath.max(0, cityTransferOfferCooldown - ms);
			}
			
			for (Map.Entry<Level, Integer> e : levelAgreeCooldowns.entrySet()) {
				e.setValue(StrictMath.max(0, e.getValue() - ms));
			}
			tradeTreatyAgreeCooldown = StrictMath.max(0, tradeTreatyAgreeCooldown - ms);
			researchTreatyAgreeCooldown = StrictMath.max(0, researchTreatyAgreeCooldown - ms);
			for (Map.Entry<Relationship.Direction, Integer> e : tributeAgreeCooldowns.entrySet()) {
				e.setValue(StrictMath.max(0, e.getValue() - ms));
			}
			
			Empire me = rel.other(e);
			int numCities = me.cities.size();
			if (rel.level == WAR) {
				if (numCitiesForWarAggression != numCities) {
					warAggression = StrictMath.min(personality.maxRisingAggressionBonus, warAggression / 2);
					numCitiesForWarAggression = numCities;
				}
			} else {
				numCitiesForWarAggression = numCities;
				if (me.timeAtPeace > personality.minWeeksAtPeaceForRisingAggression * WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK) {
					warAggression = StrictMath.min(personality.maxRisingAggressionBonus, (int) (((me.timeAtPeace / WorldMap.MS_PER_DAY / WorldMap.DAYS_PER_WEEK) - personality.minWeeksAtPeaceForRisingAggression) * personality.risingAggressionPerWeekAtPeace));
				} else {
					warAggression = 0;
				}
			}
		}
	}

	public DiplomacyAI(DiplomacyPersonality personality) {
		this.personality = personality;
	}
	
	public EmpireRecord getRecord(Empire them) {
		if (!records.containsKey(them)) {
			records.put(them, new EmpireRecord(them));
		}
		return records.get(them);
	}
	
	static EnumMap<Relationship.Level, Integer> agreementsMade = new EnumMap<Relationship.Level, Integer>(Relationship.Level.class);
	static {
		for (Relationship.Level l : values()) {
			agreementsMade.put(l, 0);
		}
	}
	
	public void warDeclared(Empire me, Empire other) {
		getRecord(other).warAggression += personality.newWarExtraAggression;
	}
	
	public void quickProcessOffer(Empire me, Offer o, CampaignWorld w) {
		Relationship rel = o.rel;
		Empire them = rel.other(me);
		if (!records.containsKey(them)) {
			records.put(them, new EmpireRecord(them));
		}
		int eval = evaluateOffer(o, me, them, w.map, false, null);
		if (eval > 0 || w.compliantAICheat) {
			rel.agree(o, w.map);
			records.get(them).addAgreed(o);
			if (them.diplomacyAI != null) {
				if (!them.diplomacyAI.records.containsKey(me)) {
					them.diplomacyAI.records.put(me, new EmpireRecord(me));
				}
				them.diplomacyAI.records.get(me).addAgreed(o);
			}
		} else {
			rel.rejectOffer(me);
		}
	}
	
	private String getDebugDetails(Empire me, Empire them, Offer o, WorldMap m) {
		StringBuilder sb2 = new StringBuilder();
		sb2.append("\nWar:");
		warQuality(me, them, m, sb2, false, true);
		sb2.append("\nPeace:");
		peaceQuality(me, them, m, sb2);
		sb2.append("\nNon-Aggression Pact:");
		nonAggressionPactQuality(me, them, m, sb2);
		sb2.append("\nTrade:");
		tradeTreatyQuality(me, them, m, sb2);
		sb2.append("\nResearch:");
		researchTreatyQuality(me, them, m, sb2);
		sb2.append("\nRelative at war strength: ").append(relativeAtWarStrength(me, them, m));
		for (Empire e : m.empires) {
			sb2.append("\n").append(e.name).append(": ").append(singleEmpireStrength(e, m));
			if (m.isAdjacent(me, e)) {
				sb2.append(" adj");
			}
		}
		/*sb2.append("\nMy strength: ").append(singleEmpireStrength(me, m));
		sb2.append("\nTheir strength: ").append(singleEmpireStrength(them, m));*/
		sb2.append("\nRESET");
		clearAllCaches(m);
		m.fleetStrengths.clear();
		m.defensesStrengths.clear();
		m.isAdjacents.clear();
		m.averageUnrests.clear();
		sb2.append("\nWar:");
		warQuality(me, them, m, sb2, false, true);
		sb2.append("\nPeace:");
		peaceQuality(me, them, m, sb2);
		sb2.append("\nNon-Aggression Pact:");
		nonAggressionPactQuality(me, them, m, sb2);
		sb2.append("\nTrade:");
		tradeTreatyQuality(me, them, m, sb2);
		sb2.append("\nResearch:");
		researchTreatyQuality(me, them, m, sb2);
		sb2.append("\nRelative at war strength: ").append(relativeAtWarStrength(me, them, m));
		for (Empire e : m.empires) {
			sb2.append("\n").append(e.name).append(": ").append(singleEmpireStrength(e, m));
			if (m.isAdjacent(me, e)) {
				sb2.append(" adj");
			}
		}
		/*sb2.append("\nMy strength: ").append(singleEmpireStrength(me, m));
		sb2.append("\nTheir strength: ").append(singleEmpireStrength(them, m));*/
		for (Relationship.CityTransfer ct : o.cityTransfers) {
			sb2.append("\n");
			if (ct.aToB == (me == o.rel.a)) {
				int q = giveCityQuality(ct.city, me, them, m, null);
				ex(q, "Give_city_x", sb2, ct.city.name);
			} else {
				int q = receiveCityQuality(ct.city, me, them, m, null);
				ex(q, "Receive_city_x", sb2, ct.city.name);
			}
		}
		return sb2.toString();
	}

	public void tick(Empire me, CampaignWorld w, WorldMap m, int ms, boolean smart) {
		//System.out.println(me.getName() + " Diplo:" + smart);
		if (ms == 0) { return; }
		if (!w.has(ConquestToggle.DIPLOMACY)) { return; }
		boolean waitingForAnswers = false;
		/*for (Relationship rel : m.getRelationships(me)) {
			Empire other = rel.other(me);
			if (!other.isPlayerControlled() && rel.level == DEFENSIVE_PACT) {
				/*Offer o = new Offer(rel);
				o.newLevel = NON_AGGRESSION_PACT;
				StringBuilder sb1 = new StringBuilder();
				int myEval = evaluateOffer(o, me, other, m, false, sb1);
				StringBuilder sb2 = new StringBuilder();
				int theirEval = other.diplomacyAI.evaluateOffer(o, other, me, m, false, sb2);
				System.out.println("Defensive pact cancel for " + me.name + " & " + other.name + ": " + myEval + " & " + theirEval);
				System.out.println(sb1);
				System.out.println(sb2);
				System.out.println();*/
				/*StringBuilder sb = new StringBuilder();
				System.out.println(me.name + " & " + other.name + " " + defensivePactQuality(me, other, m, sb));
				System.out.println(sb);
				System.out.println("");
			}
		}*/
		// Reset cached strengths and adjacencies to make sure values are correct.
		m.fleetStrengths.clear();
		m.defensesStrengths.clear();
		m.isAdjacents.clear();
		for (Relationship rel : m.getRelationships(me)) {
			Empire them = rel.other(me);
			if (!records.containsKey(them)) {
				records.put(them, new EmpireRecord(them));
			}
			records.get(them).tick(ms, rel);
			Offer o = rel.getOffer(m);
			if (o != null) {
				clearAllCaches(m);
				if (rel.isOfferingNegotiation(them)) {
					StringBuilder sb = new StringBuilder();
					Locale loc = Lang.currentLocale;
					//Lang.currentLocale = Locale.ENGLISH;
					Lang.setCurrentLocale(Locale.ENGLISH);
					int eval = evaluateOffer(o, me, them, m, false, sb);
					if (eval > 0) {
						//System.out.println("Accepting offer");
						//System.out.println(sb);
						rel.agree(o, m);
						records.get(them).addAgreed(o);
						m.addAIDiplomacyDecisionRecord("agree", eval, o.toJSON(), sb.toString() + getDebugDetails(me, them, o, m), me, them, m, this);
						if (them.diplomacyAI != null) {
							if (!them.diplomacyAI.records.containsKey(me)) {
								them.diplomacyAI.records.put(me, new EmpireRecord(me));
							}
							them.diplomacyAI.records.get(me).addAgreed(o);
						}
					} else {
						//System.out.println("Rejecting offer");
						//System.out.println(sb);
						m.addAIDiplomacyDecisionRecord("reject", eval, o.toJSON(), sb.toString(), me, them, m, this);
						rel.rejectOffer(me);
					}
					Lang.setCurrentLocale(loc);
				} else {
					StringBuilder sb = null;//new StringBuilder();
					int eval = evaluateOffer(o, me, them, m, false, sb);
					if (eval < -1) {
						/*System.out.println("Rescinding offer");
						System.out.println(sb);*/
						m.addAIDiplomacyDecisionRecord("rescind", eval, o.toJSON(), null, me, them, m, this);
						rel.rejectOffer(me);
					} else {
						waitingForAnswers = true; // qqDPS Right now this means the AI is blocked from doing diplo if they're waiting for anyone.
					}
				}
			}
			if (rel.challenge != null && rel.challenge.target(rel) == me) {
				switch (rel.challenge.type) {
					case DELEGATION:
						if (m.r.nextInt(100) < acceptDelegationProbability(me, them, m)) {
							rel.challengeResponse(m, EmpireStat.DELEGATION_ACCEPTED_PREFIX.get(me.bonuses) + m.r.nextInt(EmpireStat.DELEGATION_ACCEPTED_NUM.get(me.bonuses)), null);
						} else {
							rel.noChallengeResponse(m, EmpireStat.DELEGATION_REJECTED_PREFIX.get(me.bonuses) + m.r.nextInt(EmpireStat.DELEGATION_REJECTED_NUM.get(me.bonuses)), null);
						}
						break;
					case INSULT:
						if (m.r.nextInt(100) < insultBackProbability(me, them, m)) {
							rel.challengeResponse(m,
									EmpireStat.INSULT_1_PREFIX.get(me.bonuses) + m.r.nextInt(EmpireStat.INSULT_1_NUM.get(me.bonuses)),
									EmpireStat.INSULT_2_PREFIX.get(me.bonuses) + m.r.nextInt(EmpireStat.INSULT_2_NUM.get(me.bonuses)));
											
						} else {
							rel.noChallengeResponse(m, null, null);
						}
						break;
				}
			}
		}
		diplomacyCooldown -= ms;
		accedeToUltimatumCooldown -= ms;
		if (diplomacyCooldown > 0) { return; }
		for (Relationship rel : m.getRelationships(me)) {
			Empire them = rel.other(me);
			Ultimatum ult = rel.getUltimatum(m);
			if (ult != null && ult.forcer == them && !ult.defied) {
				clearAllCaches(m);
				StringBuilder sb = null;//new StringBuilder();
				//sb.append("Demand from ").append(them.getName());
				int demandQuality = evaluateOffer(ult.getDemand(), me, them, m, false, sb, /* considerWarQuality */ true, /* ultimatumDemand */ true);
				//sb.append("\nThreat");
				int threatQuality = evaluateOffer(ult.getOrElse(), me, them, m, false, sb);
				int relativeQuality = demandQuality - threatQuality;
				if (ult.isDeFactoWarDeclaration()) {
					// No SB!
					relativeQuality -= 999;
				}
				int acceptLikelihoodPercent = (int) (relativeQuality * personality.ultimatumQualityDifferenceToLikelihoodPercent) + personality.agreeToUltimatumBaseline;
				if (accedeToUltimatumCooldown > 0) {
					acceptLikelihoodPercent -= personality.recentAccededToUltimatumPenalty;
				}
				//System.out.println(sb);
				//System.out.println(demandQuality + " vs " + threatQuality + " -> " + acceptLikelihoodPercent + "%");
				if ((w.compliantAICheat && them.playerControlled) || m.r.nextInt(100) < acceptLikelihoodPercent) {
					//System.out.println("Ult accepted");
					rel.accedeToUltimatum(m);
					records.get(them).addAccepted(ult);
					accedeToUltimatumCooldown = personality.accedeToUltimatumCooldown;
					m.addAIDiplomacyDecisionRecord("accedeToUltimatum", relativeQuality, ult.toJSON(), null, me, them, m, this);
				} else {
					//System.out.println("Ult rejected");
					rel.defyUltimatum(m);
					m.addAIDiplomacyDecisionRecord("defyUltimatum", relativeQuality, ult.toJSON(), null, me, them, m, this);
				}
			}
			if (ult != null && ult.forcer == me) {
				if (ult.defied) {
					//System.out.println(them.getName() + " defied my ultimatum");
					StringBuilder sb = null;//new StringBuilder();
					int enforceThreatProbability = enforceThreatProbability(ult, me, them, m, sb);
					//System.out.println(sb);
					if (m.r.nextInt(100) < enforceThreatProbability) {
						//System.out.println("Enforcing");
						rel.enforceUltimatum(m);
						m.addAIDiplomacyDecisionRecord("enforceUltimatum", enforceThreatProbability, ult.toJSON(), null, me, them, m, this);
					} else {
						//System.out.println("Backing down");
						rel.dontEnforceUltimatum(m);
						m.addAIDiplomacyDecisionRecord("dontEnforceUltimatum", enforceThreatProbability, ult.toJSON(), null, me, them, m, this);
					}
				} else {
					waitingForAnswers = true;
				}
			}
		}
		if (w.deactivateAICheat) { return; }
		if (smart && !waitingForAnswers) {
			diplomacyCooldown = 10000;
			// Should we try to be annexed?
			if (me.cities.size() == 1 && me.cities.get(0).isTown) {
				clearAllCaches(m);
				int bestAnnexedQuality = 0;
				Empire bestAnnexer = null;
				Offer bestAnnex = null;
				for (Relationship rel : m.getRelationships(me)) {
					Empire them = rel.other(me);
					Offer annex = new Offer(rel);
					annex.setBeingAnnexed(me);
					if (rel.isValid(annex, null, m, false) && !getRecord(them).annexOffered) {
						int q = becomeAnnexedQuality(me, them, m, null);
						if (q > bestAnnexedQuality) {
							DiplomacyAI theirAI = them.diplomacyAI == null ? new DiplomacyAI(personality.assumeOtherPersonality(them)) : them.diplomacyAI;
							int theirQ = theirAI.doAnnexQuality(them, me, m, null);
							if (theirQ > 0) {
								bestAnnexedQuality= q;
								bestAnnexer = them;
								bestAnnex = annex;
							}
						}
					}
				}
				if (bestAnnexer != null) {
					Relationship rel = m.getRelationship(me, bestAnnexer);
					Offer annex = bestAnnex;
					if (bestAnnexer.playerControlled) {
						rel.makeOffer(annex, me, m);
						m.addAIDiplomacyDecisionRecord("makeOffer", bestAnnexedQuality, annex.toJSON(), null, me, rel.other(me), m, this);
						if (w.player == bestAnnexer) {
							w.playerOffers.add(annex);
						}
						records.get(bestAnnexer).addOffered(annex);
					} else {
						m.addAIDiplomacyDecisionRecord("mutuallyAgree", bestAnnexedQuality, annex.toJSON(), null, me, rel.other(me), m, this);
						rel.agree(annex, m);
						countAIAgreement(annex);
					}
					return;
				}
			}
			if (activeEmpire == null) {
				activeEmpire = m.empires.get(m.r.nextInt(m.empires.size()));
			}
			activeEmpire = m.empires.get((m.empires.indexOf(activeEmpire) + 1) % m.empires.size());
			StringBuilder sb = null;//new StringBuilder();
			if (activeEmpire != null && activeEmpire != me) {
				clearAllCaches(m);
				Decision dec = getDecision(me, m, w.map.r, activeEmpire, sb);
				if (dec.offer != null || dec.ultimatum != null || dec.challenge != null) {
					//System.out.println(me.getName() + ": decision " + dec);
					//System.out.println(sb);
					if (dec.offer != null) {
						Empire other = dec.offer.rel.other(me);
						/*sb = new StringBuilder();
						sb.append("My evaluation:\n");
						int val = evaluateOffer(dec.offer, me, other, m, dec.offerIsForced, sb);
						sb.append("\nTotal ").append(val);
						if (!dec.offerIsForced) {
							sb.append("\n\nOther evaluation:");
							DiplomacyAI theirAI = other.diplomacyAI == null ? new DiplomacyAI(personality.assumeOtherPersonality(other)) : other.diplomacyAI;
							val = theirAI.evaluateOffer(dec.offer, other, me, m, false, sb);
							sb.append("\nTotal ").append(val);
						}*/
						//System.out.println(sb);
						//System.out.println();
						if (dec.offerIsForced) {
							//System.out.println("force");
							dec.offer.rel.force(dec.offer, me, m, true);
							m.addAIDiplomacyDecisionRecord("force", dec.quality, dec.offer.toJSON(), null, me, other, m, this);
						} else {
							if (other.playerControlled) {
								//System.out.println("mkoffer");
								dec.offer.rel.makeOffer(dec.offer, me, m);
								m.addAIDiplomacyDecisionRecord("makeOffer", dec.quality, dec.offer.toJSON(), null, me, other, m, this);
								if (w.player == other) {
									w.playerOffers.add(dec.offer);
								}
								records.get(other).addOffered(dec.offer);
							} else {
								/*if (dec.offer.newLevel != null) {
									agreementsMade.put(dec.offer.newLevel, agreementsMade.get(dec.offer.newLevel) + 1);
									System.out.println(dec.offer.newLevel.name() + ": " + agreementsMade.get(dec.offer.newLevel));
								}*/
								//System.out.println("agree");
								StringBuilder sb2 = new StringBuilder();
								Locale loc = Lang.currentLocale;
								Lang.setCurrentLocale(Locale.ENGLISH);
								evaluateOffer(dec.offer, me, other, m, false, sb2);
								m.addAIDiplomacyDecisionRecord("mutuallyAgree", dec.quality, dec.offer.toJSON(), sb2.toString() + getDebugDetails(me, other, dec.offer, m), me, other, m, this);
								Lang.setCurrentLocale(loc);
								dec.offer.rel.agree(dec.offer, m);
								countAIAgreement(dec.offer);		
							}
						}
						//System.out.println("---");
					}
					if (dec.ultimatum != null) {
						dec.ultimatum.getDemand().rel.makeUltimatum(dec.ultimatum, m);
						Empire other = dec.ultimatum.getDemand().rel.other(me);
						m.addAIDiplomacyDecisionRecord("makeUltimatum", dec.quality, dec.ultimatum.toJSON(), null, me, other, m, this);
						if (other.playerControlled) {
							records.get(other).addOffered(dec.ultimatum);
						}
					}
					if (dec.challenge != null) {
						Relationship rel = m.getRelationship(me, dec.challengeTarget);
						rel.makeChallenge(dec.challenge, m);
					}
				}
			}
		}
	}
	
	public static class Decision {
		public final Empire me;
		public final int quality;
		public final Offer offer;
		public final boolean offerIsForced;
		public final Ultimatum ultimatum;
		public final Relationship.Challenge challenge;
		public final Empire challengeTarget;

		public Decision(Empire me, int quality, Offer offer, boolean offerIsForced) {
			this.me = me;
			this.quality = quality;
			this.offer = offer;
			this.offerIsForced = offerIsForced;
			ultimatum = null;
			challenge = null;
			challengeTarget = null;
		}

		public Decision(Empire me, int quality, Ultimatum ultimatum) {
			this.me = me;
			this.quality = quality;
			this.ultimatum = ultimatum;
			offerIsForced = false;
			offer = null;
			challenge = null;
			challengeTarget = null;
		}
		
		public Decision(Empire me, int quality, Relationship.Challenge challenge, Empire challengeTarget) {
			this.me = me;
			this.quality = quality;
			this.challenge = challenge;
			this.challengeTarget = challengeTarget;
			offer = null;
			ultimatum = null;
			offerIsForced = false;
		}
		
		public Decision() {
			me = null;
			quality = 0;
			ultimatum = null;
			offerIsForced = false;
			offer = null;
			challenge = null;
			challengeTarget = null;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (offer != null) {
				if (offerIsForced) {
					sb.append("force ");
				} else if (offer.rel.other(me).playerControlled) {
					sb.append("offer ");
				} else {
					sb.append("agree ");
				}
				describeOffer(me, offer, sb);
			} else if (ultimatum != null) {
				sb.append("demand ");
				describeOffer(me, ultimatum.getDemand(), sb);
				sb.append(", or else ");
				describeOffer(me, ultimatum.getOrElse(), sb);
			} else if (challenge != null) {
				sb.append(challenge.type.name().toLowerCase()).append(" ").append(challengeTarget.getName());
			} else {
				sb.append("nothing");
			}
			
			return sb.toString();
		}
	}
	
	public static String getDecisionsOverview(ArrayList<Empire> empires, HasRelationships m, GuardedRandom r) {
		StringBuilder sb = new StringBuilder();
		for (Empire e : empires) {
			if (sb.length() > 0) { sb.append("\n"); }
			DiplomacyAI ai = e.diplomacyAI == null ? new DiplomacyAI(new DiplomacyPersonality()) : e.diplomacyAI;
			Decision dec = ai.getDecision(e, m, r, null, null);
			sb.append(e.getName()).append(": ").append(dec);
			sb.append("\n  fleet: ").append(m.getFleetStrength(e)).append(" defenses: ").append(m.getDefensesStrength(e)).append(" adjacentTo:");
			for (Empire e2 : empires) {
				if (e2 == e) { continue; }
				if (m.isAdjacent(e, e2)) {
					sb.append(" ").append(e2.getName());
				}
			}
			/*if (dec.offer != null) {
				ai.evaluateOffer(dec.offer, e, dec.offer.rel.other(e), m, dec.offerIsForced, sb);
				sb.append("\n\n");
			}*/

		}
		return sb.toString();
	}
	
	public static String describeOffer(Empire me, Offer o) {
		StringBuilder sb = new StringBuilder();
		describeOffer(me, o, sb);
		return sb.toString();
	}
	
	public static void describeOffer(Empire me, Offer o, StringBuilder sb) {
		sb.append(o.rel.other(me).getName());
		if (o.newLevel != null) {
			sb.append(" ").append(o.newLevel.name());
		}
		if (o.newLevel != WAR) {
			if (o.newTradeTreaty != null && o.newTradeTreaty) {
				sb.append(" trade");
			}
			if (o.newTradeTreaty != null && !o.newTradeTreaty) {
				sb.append(" break-trade");
			}
			if (o.newResearchTreaty != null && o.newResearchTreaty) {
				sb.append(" research");
			}
			if (o.newResearchTreaty != null && !o.newResearchTreaty) {
				sb.append(" break-research");
			}
			if (o.newTribute != null) {
				if (o.newTribute == NEITHER) {
					sb.append(" break-tribute");
				} else if (o.newTribute == A_TO_B) {
					sb.append(me == o.rel.a ? " give-tribute" : " receive-tribute");
				} else if (o.newTribute == B_TO_A) {
					sb.append(me == o.rel.b ? " give-tribute" : " receive-tribute");
				}
			}
			if (o.moneyTransferAToB > 0) {
				sb.append(" give $").append(o.moneyTransferAToB);
			}
			if (o.moneyTransferAToB < 0) {
				sb.append(" receive $").append(-o.moneyTransferAToB);
			}
			if (o.submissionAToB != null) {
				if (o.submissionAToB) {
					sb.append(" give-submission");
				} else {
					sb.append(" receive-submission");
				}
			}
			for (Relationship.CityTransfer ct : o.cityTransfers) {
				if (ct.aToB) {
					sb.append(" give-city ").append(ct.city.name);
				} else {
					sb.append(" receive-city ").append(ct.city.name);
				}
			}
		}
	}
	
	public void clearAllCaches(WorldMap wm) {
		for (Empire e : wm.empires) {
			e.diplomacyAI.clearCaches();
		}
	}
	
	public void clearCaches() {
		warQualities.clear();
		warQualitiesForced.clear();
		peaceQualities.clear();
		nonAggressionPactQualities.clear();
		defensivePactQualities.clear();
		allianceQualities.clear();
		giveTributeQualities.clear();
		receiveTributeQualities.clear();
		tradeTreatyQualities.clear();
		researchTreatyQualities.clear();
		relativeAtWarStrengths.clear();
		giveCityQualities.clear();
		receiveCityQualities.clear();
		isDoingBadly.clear();
	}
	
	public Decision getDecision(final Empire me, final HasRelationships m, GuardedRandom r, Empire onlyThem, StringBuilder sb) {
		Offer plan = null;
		int planQuality = 0;
		boolean planIsForced = false;
		int betweenAIsPlanQuality = 0;
		
		Offer ultimatumDemand = null;
		Offer ultimatumThreat = null;
		int bestDemandUpside = 0;
		
		for (Relationship rel : m.getRelationships(me)) {
			final Empire them = rel.other(me);
			if (onlyThem != null && them != onlyThem) { continue; }
			DiplomacyAI theirAssumedAI = new DiplomacyAI(personality.assumeOtherPersonality(them));
			EmpireRecord rec = them.playerControlled ? records.get(them) : null;
			if (sb != null) {
				sb.append(them.getName());
				sb.append("\n= War =");
				warQuality(me, them, m, sb, true, true);
				sb.append("\n= Peace =");
				peaceQuality(me, them, m, sb);
				sb.append("\n= Non-Aggression Pact =");
				nonAggressionPactQuality(me, them, m, sb);
				sb.append("\n= Defensive Pact =");
				defensivePactQuality(me, them, m, sb);
				sb.append("\n= Alliance =");
				allianceQuality(me, them, m, sb);
				sb.append("\n= Trade Treaty =");
				tradeTreatyQuality(me, them, m, sb);
				sb.append("\n= Research Treaty =");
				researchTreatyQuality(me, them, m, sb);
				sb.append("\n= Give Tribute =");
				giveTributeQuality(me, them, m, sb);
				sb.append("\n= Receive Tribute =");
				receiveTributeQuality(me, them, m, sb);
				sb.append("\n= Give $ ").append(Relationship.giveMoneyAmount(me, them, m)).append(" =");
				giveMoneyQuality(Relationship.giveMoneyAmount(me, them, m), me, them, m, sb);
				sb.append("\n= Receive $ ").append(Relationship.giveMoneyAmount(them, me, m)).append(" =");
				receiveMoneyQuality(Relationship.giveMoneyAmount(them, me, m), me, them, m, sb);
				sb.append("\n= Give Submission =");
				giveSubmissionQuality(me, them, m, sb);
				sb.append("\n= Receive Submission =");
				receiveSubmissionQuality(me, them, m, sb);
				sb.append("\n\n");
			}
			
			if (rel.getOffer(m) != null || rel.getUltimatum(m) != null || rel.challenge != null) { continue; }
			
			ArrayList<Offer> os = generateValidOffers(me, them, m, false);
			if (sb != null) {
				Collections.sort(os, new Comparator<Offer>() {
					@Override
					public int compare(Offer o1, Offer o2) {
						int meVal1 = evaluateOffer(o1, me, them, m, false, null);
						int meVal2 = evaluateOffer(o2, me, them, m, false, null);
						return meVal2 - meVal1;
					}				
				});
			}
			
			for (Offer o : os) {
				if (rec != null && !rec.canAsk(o, m)) { continue; }
				int meBaseVal = evaluateOffer(o, me, them, m, false, null);
				int themVal = theirAssumedAI.evaluateOffer(o, them, me, m, false, null);
				int meVal = meBaseVal + compromiseQuality(meBaseVal, themVal, singleEmpireStrength(me, m), singleEmpireStrength(them, m), null);
				// Don't print things that won't work, for now.
				//if (meVal <= 0 || themVal <= -personality.unfairOfferForHumansBonus) { continue; }
				if (sb != null) {
					String icon;
					if (meVal >= personality.minimalOfferUpside) {
						if (themVal > 0) {
							icon = "& ";
						} else {
							icon = "< ";
						}
					} else {
						if (themVal > 0) {
							icon = "> ";
						} else {
							icon = "X ";
						}
					}
					sb.append(icon);
					describeOffer(me, o, sb);
					sb.append(" me: ").append(meVal).append(" them: ").append(themVal);
					if (!them.playerControlled) {
						sb.append(" actual: ").append(them.diplomacyAI.evaluateOffer(o, them, me, m, false, null));
					}
					/*sb.append("\nMe:");
					myAI.evaluateOffer(o, me, them, m, false, sb);
					sb.append("\nThem:");
					theirAI.evaluateOffer(o, them, me, m, false, sb);*/
					sb.append("\nCompromise Quality:");
					compromiseQuality(meBaseVal, themVal, singleEmpireStrength(me, m), singleEmpireStrength(them, m), sb);
					sb.append("\n\n");
				}
				
				// Check quality
				if (!them.playerControlled) {
					int theirActualVal = them.diplomacyAI.evaluateOffer(o, them, me, m, false, null);
					if (meVal >= personality.minimalOfferUpside && theirActualVal > 0 && meVal > planQuality) {
						int aiVsAIQuality = betweenAIsCompromiseQuality(meVal, theirActualVal, singleEmpireStrength(me, m), singleEmpireStrength(them, m));
						if (aiVsAIQuality > betweenAIsPlanQuality) {
							plan = o;
							planQuality = meVal;
							betweenAIsPlanQuality = aiVsAIQuality;
							planIsForced = false;
						}
					}
				} else {
					themVal += personality.unfairOfferForHumansBonus;
					if (meVal >= personality.minimalOfferUpside && meVal > planQuality && themVal > 0) {
						planQuality = meVal;
						plan = o;
						planIsForced = false;
						if (o.moneyTransferAToB == 0) {
							int amt = Relationship.giveMoneyAmount(me, them, m);
							int amtQuality = giveMoneyQuality(amt, me, them, m, null);
							if (amt <= me.getMoney() && meVal + amtQuality > 0) {
								o.moneyTransferAToB = rel.a == me ? amt : -amt;
								planQuality += amtQuality;
								//System.out.println(me.name + " adding giving $" + amt + " to player with quality " + amtQuality + " and resulting quality of " + planQuality);
							}
						}
					}
				}
			}
			
			ArrayList<Offer> fs = generateValidOffers(me, them, m, true);
			if (sb != null) {
				Collections.sort(fs, new Comparator<Offer>() {
					@Override
					public int compare(Offer o1, Offer o2) {
						int meVal1 = evaluateOffer(o1, me, them, m, false, null);
						int meVal2 = evaluateOffer(o2, me, them, m, false, null);
						return meVal2 - meVal1;
					}
				});
			}
			
			for (Offer o : fs) {
				if (rec != null && !rec.canForce(o, m)) { continue; }
				int meVal = evaluateOffer(o, me, them, m, true, null);
				if (sb != null) {
					String icon = meVal > 0 ? "& " : "X ";
					sb.append(icon);
					describeOffer(me, o, sb);
					evaluateOffer(o, me, them, m, true, sb);
					sb.append("\nme: ").append(meVal).append("\n");
				}
				
				// Check AI quality
				if (meVal >= personality.minimalForceUpside && meVal > planQuality) {
					plan = o;
					planQuality = meVal;
					betweenAIsPlanQuality = 0;
					planIsForced = true;
				}
			}
			
			if (sb != null) { sb.append("\n\n"); }
			
			// Try to construct a nier ultimatum
			if (them.isPlayerControlled() || them.diplomacyAI == null || them.diplomacyAI.accedeToUltimatumCooldown <= 0) {
				for (Offer demand : os) {
					if (rec != null && !rec.canAsk(demand, m)) { continue; }
					if (!rel.isValid(demand, null, m, /* demand */ true)) { continue; }
					int meDemandVal = evaluateOffer(demand, me, them, m, false, null);
					if (!m.isAdjacent(me, them) && m.hasAdjacentNonAlliedEmpires(me)) {
						meDemandVal -= 30;
					}
					int themDemandVal = theirAssumedAI.evaluateOffer(demand, them, me, m, false, null);
					if (themDemandVal <= 0 && meDemandVal > bestDemandUpside) {
						for (Offer threat : fs) {
							int meThreatVal = evaluateOffer(threat, me, them, m, true, null);
							int themThreatVal = theirAssumedAI.evaluateOffer(threat, them, me, m, false, null);
							if (meThreatVal > 0 && meThreatVal <= meDemandVal - personality.minUltimatumVersusThreatMyQualityDifference && themThreatVal <= themDemandVal - personality.minUltimatumVersusThreatTheirQualityDifference) {
								ultimatumDemand = demand;
								ultimatumThreat = threat;
								bestDemandUpside = meDemandVal;
							}
						}
					}
				}
			}
			
			if (sb != null) {
				sb.append("\n\n\n");
			}
		}
		
		Relationship.Challenge.Type challengeType = null;
		Empire challengeTarget = null;
		int challengeQuality = -1;
		if (plan == null && ultimatumDemand == null && me.sendChallengeCooldown == 0) {
			for (Relationship rel : m.getRelationships(me)) {
				if (rel.level == WAR) { continue; }
				if (rel.getOffer(m) != null || rel.getUltimatum(m) != null || rel.challenge != null) { continue; }
				Empire them = rel.other(me);
				if (them.playerControlled && them.timeSinceLastChallenge < 400 * 28 * 8) { continue; }
				if (onlyThem != null && them != onlyThem) { continue; }
				int q = insultQuality(me, them, rel, m, null);
				if (q > challengeQuality) {
					challengeType = INSULT;
					challengeTarget = them;
					challengeQuality = q;
				}
				q = delegationQuality(me, them, rel, m, null);
				if (q > challengeQuality) {
					challengeType = DELEGATION;
					challengeTarget = them;
					challengeQuality = q;
				}
			}
		}
		
		if (sb != null) {
			sb.append("\n\n= Decision =\n");
			if (plan != null) {
				sb.append("Upside: ").append(planQuality).append("\n");
				if (planIsForced) { sb.append("force "); } else { sb.append(plan.rel.other(me).playerControlled ? "offer " : "agree "); }
				sb.append(me.getName()).append(" ");
				describeOffer(me, plan, sb);
				if (!planIsForced) {
					sb.append("\nThem: ");
					evaluateOffer(plan, plan.rel.other(me), me, m, false, sb);
				}
			} else {
				sb.append("nothing");
			}

			sb.append("\n\n= Ultimatum =\n");
			if (ultimatumDemand != null) {
				sb.append("Upside: ").append(bestDemandUpside).append("\n");
				sb.append(ultimatumDemand.rel.other(me).getName()).append("\nDemand:");
				describeOffer(me, ultimatumDemand, sb);
				sb.append("\nThreat:");
				describeOffer(me, ultimatumThreat, sb);
			} else {
				sb.append("nothing");
			}
			
			sb.append("\n\n= Challenge =\n");
			if (challengeType == null) {
				sb.append("nothing");
			} else {
				sb.append(challengeType.name().toLowerCase()).append(" ").append(challengeTarget.getName());
			}
		}
		
		if (plan != null) {
			if (ultimatumDemand != null) {
				return planQuality > bestDemandUpside + personality.makeUltimatumBaseline ? new Decision(me, planQuality, plan, planIsForced) : new Decision(me, bestDemandUpside, new Ultimatum(m, ultimatumDemand.rel, me, ultimatumDemand, ultimatumThreat, Relationship.ULTIMATUM_TIME));
			} else {
				return new Decision(me, planQuality, plan, planIsForced);
			}
		} else if (ultimatumDemand != null) {
			return new Decision(me, bestDemandUpside, new Ultimatum(m, ultimatumDemand.rel, me, ultimatumDemand, ultimatumThreat, Relationship.ULTIMATUM_TIME));
		} else if (challengeType != null) {
			Relationship.Challenge ch = new Relationship.Challenge(me, m.getRelationship(me, challengeTarget), challengeType, Relationship.CHALLENGE_TIME, r);
			/*StringBuilder csb = new StringBuilder();
			switch (ch.type) {
				case DELEGATION:
					delegationQuality(me, challengeTarget, m.getRelationship(me, challengeTarget), m, csb);
					break;
				case INSULT:
					insultQuality(me, challengeTarget, m.getRelationship(me, challengeTarget), m, csb);
					break;
			}
			System.out.println(me.getName() + " " + ch.type + " " + challengeTarget.getName());
			System.out.println(csb);
			System.out.println("---");*/
			return new Decision(me, challengeQuality, ch, challengeTarget);
		} else {
			return new Decision();
		}
	}
	
	public static String getSummary(Empire me, HasRelationships m, GuardedRandom r) {
		StringBuilder sb = new StringBuilder();
		DiplomacyAI myAI;
		if (me.playerControlled) {
			sb.append("Assuming Default AI\n");
			myAI = new DiplomacyAI(new DiplomacyPersonality());
		} else {
			myAI = me.diplomacyAI;
		}
		myAI.getDecision(me, m, r, null, sb);
		return sb.toString();
	}
	
	private static final Boolean[] NONE_YES_NO = {null, Boolean.TRUE, Boolean.FALSE };
	private static final Relationship.Direction[] NONE_AND_DIRS = {null, NEITHER, A_TO_B, B_TO_A};
	
	private ArrayList<Offer> generateValidOffers(Empire me, Empire them, HasRelationships m, boolean forced) {
		ArrayList<Offer> offers = new ArrayList<Offer>();
		Relationship rel = m.getRelationship(me, them);
		for (int i = 0; i < Level.values().length + 1; i++) {
			Level l = i == 0 ? null : Level.values()[i - 1];
			if (l == rel.level) { continue; }
			if (l == WAR) {
				Offer o = new Offer(rel);
				o.setToWar();
				if (rel.isValid(o, forced ? me : null, m, false)) {
					offers.add(o);
				}
				continue;
			}
			if (l == TRUCE && (Relationship.inPlayerAlliance(me, m) || Relationship.inPlayerAlliance(them, m))) {
				// Don't do truces on behalf of human players.
				continue;
			}
			for (int tradeTreaty = 0; tradeTreaty < 3; tradeTreaty++) {
				if (tradeTreaty > 0 && NONE_YES_NO[tradeTreaty] == rel.tradeTreaty) { continue; }
				for (int researchTreaty = 0; researchTreaty < 3; researchTreaty++) {
					if (researchTreaty > 0 && NONE_YES_NO[researchTreaty] == rel.researchTreaty) { continue; }
					for (int tribute = 0; tribute < 4; tribute++) {
						if (tribute > 0 && NONE_AND_DIRS[tribute] == rel.tribute) { continue; }
						for (int money = 0; money < 3; money++) {
							for (int submission = 0; submission < 3; submission++) {
								for (int giveCityIndex = -1; giveCityIndex < me.cities.size(); giveCityIndex++) {
									for (int receiveCityIndex = -1; receiveCityIndex < them.cities.size(); receiveCityIndex++) {
										if (submission != 0 && !m.toggles().contains(ConquestToggle.REPUTATION)) {
											continue;
										}
										if (forced && (money != 0 || submission != 0 || giveCityIndex != 0 || receiveCityIndex != 0)) {
											// If we're in forced mode, skip stuff that cannot be forced.
											continue;
										}
										Offer o = new Offer(rel);
										o.newLevel = l;
										o.newTradeTreaty = NONE_YES_NO[tradeTreaty];
										o.newResearchTreaty = NONE_YES_NO[researchTreaty];
										o.newTribute = NONE_AND_DIRS[tribute];
										if (money == 1) {
											if (rel.a.playerControlled) { continue; }
											o.moneyTransferAToB = Relationship.giveMoneyAmount(rel.a, rel.b, m);
											if (o.moneyTransferAToB > rel.a.getMoney()) {
												continue;
											}
										}
										if (money == 2) {
											if (rel.b.playerControlled) { continue; }
											o.moneyTransferAToB = -Relationship.giveMoneyAmount(rel.b, rel.a, m);
											if (-o.moneyTransferAToB > rel.b.getMoney()) {
												continue;
											}
										}
										if (submission == 1) {
											o.submissionAToB = true;
										}
										if (submission == 2) {
											o.submissionAToB = false;
										}
										if (giveCityIndex >= 0) {
											City c = me.cities.get(giveCityIndex);
											if (!m.isConnectedToCapital(them,  c) && c.originalEmpire != them) {
												continue;
											}
											o.cityTransfers.add(new Relationship.CityTransfer(c, /*aToB*/ me == rel.a));
										}
										if (receiveCityIndex >= 0) {
											City c = them.cities.get(receiveCityIndex);
											if (!m.isConnectedToCapital(me,  c) && c.originalEmpire != me) {
												continue;
											}
											o.cityTransfers.add(new Relationship.CityTransfer(c, /*aToB*/ them == rel.a));
										}
										if (!o.isEmpty() && rel.isValid(o, forced ? me : null, m, false) && (forced || !them.playerControlled || !isSimpleCancellation(o))) {
											offers.add(o);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return offers;
	}
	
	// Is this just cancelling/downgrading a relationship?
	private static boolean isSimpleCancellation(Offer o) {
		return
				o.cityTransfers.isEmpty() &&
				o.submissionAToB == null &&
				o.annexationAToB == null &&
				(o.newResearchTreaty == null || !o.newResearchTreaty) &&
				(o.newTradeTreaty == null || !o.newTradeTreaty) &&
				o.moneyTransferAToB == 0 &&
				o.newTribute == null &&
				o.newLevel != WAR &&
				(o.newLevel == null || o.newLevel.ordinal() <= o.rel.level.ordinal());
	}
	
	public int newLevelQuality(Empire me, Empire them, Level level, Relationship rel, HasRelationships m, StringBuilder e) {
		if (e != null) { e.append("\n").append(_t("current_relationship_")).append(_t("relationship_" + rel.level)); }
		int oldLevel = 0;
		switch (rel.level) {
			case WAR:
				oldLevel += warQuality(me, them, m, e, false, true);
				break;
			case TRUCE:
			case PEACE:
				oldLevel += peaceQuality(me, them, m, e);
				break;
			case NON_AGGRESSION_PACT:
				oldLevel += nonAggressionPactQuality(me, them, m, e);
				break;
			case DEFENSIVE_PACT:
				oldLevel += defensivePactQuality(me, them, m, e);
				break;
			case ALLIANCE:
				oldLevel += allianceQuality(me, them, m, e);
				break;
		}
		if (e != null) { e.append("\n\n").append(_t("proposed_relationship_")).append(_t("relationship_" + level)); }
		int newLevel = 0;
		switch (level) {
			case WAR:
				newLevel += warQuality(me, them, m, e, false, true);
				break;
			case TRUCE:
			case PEACE:
				newLevel += peaceQuality(me, them, m, e);
				break;
			case NON_AGGRESSION_PACT:
				newLevel += nonAggressionPactQuality(me, them, m, e);
				break;
			case DEFENSIVE_PACT:
				newLevel += defensivePactQuality(me, them, m, e);
				break;
			case ALLIANCE:
				newLevel += allianceQuality(me, them, m, e);
				break;
		}
		if (e != null) { e.append("\n\n").append(_t("quality_difference_")).append(newLevel - oldLevel); }
		return newLevel - oldLevel;
	}
	
	public int evaluateOffer(Offer o, Empire me, Empire them, HasRelationships m, boolean forced, StringBuilder e) {
		return evaluateOffer(o, me, them, m, forced, e, true, false);
	}
	
	public int evaluateOffer(Offer o, Empire me, Empire them, HasRelationships m, boolean forced, StringBuilder e, boolean considerWarQuality, boolean ultimatumDemand) {
		int total = 0;
		if (o.newLevel != null) {
			int newLevel = 0;
			switch (o.newLevel) {
				case WAR:
					if (considerWarQuality) {
						newLevel += warQuality(me, them, m, null, forced, true);
					}
					break;
				case TRUCE:
				case PEACE:
					newLevel += peaceQuality(me, them, m, null);
					break;
				case NON_AGGRESSION_PACT:
					newLevel += nonAggressionPactQuality(me, them, m, null);
					break;
				case DEFENSIVE_PACT:
					newLevel += defensivePactQuality(me, them, m, null);
					break;
				case ALLIANCE:
					newLevel += allianceQuality(me, them, m, null);
					break;
			}
			int oldLevel = 0;
			switch (o.rel.level) {
				case WAR:
					if (considerWarQuality) {
						oldLevel += warQuality(me, them, m, null, forced, true);
					}
					break;
				case TRUCE:
				case PEACE:
					oldLevel += peaceQuality(me, them, m, null);
					break;
				case NON_AGGRESSION_PACT:
					oldLevel += nonAggressionPactQuality(me, them, m, null);
					break;
				case DEFENSIVE_PACT:
					oldLevel += defensivePactQuality(me, them, m, null);
					break;
				case ALLIANCE:
					oldLevel += allianceQuality(me, them, m, null);
					break;
			}
			if (o.newLevel.ordinal() >= PEACE.ordinal() && o.newLevel.ordinal() < o.rel.level.ordinal()) {
				ex(newLevel - oldLevel, "downgrade_" + o.rel.level + "_to_" + o.newLevel, e);
			} else {
				ex(newLevel - oldLevel, "relationship_" + o.newLevel, e);
			}
			total += newLevel - oldLevel;
		}
		if (o.newTradeTreaty != null) {
			int newTrade = o.newTradeTreaty ? tradeTreatyQuality(me, them, m, null) : 0;
			int oldTrade = o.rel.tradeTreaty ? tradeTreatyQuality(me, them, m, null) : 0;
			ex(newTrade - oldTrade, o.newTradeTreaty ? "Trade_Treaty" : "Break_Trade_Treaty", e);
			total += newTrade - oldTrade;
		}
		if (o.newResearchTreaty != null) {
			int newResearch = o.newResearchTreaty ? researchTreatyQuality(me, them, m, null) : 0;
			int oldResearch = o.rel.researchTreaty ? researchTreatyQuality(me, them, m, null) : 0;
			ex(newResearch - oldResearch, o.newResearchTreaty ? "Research_Treaty" : "Break_Research_Treaty", e);
			total += newResearch - oldResearch;
		}
		if (o.newTribute != null) {
			int newTribute = 0;
			int oldTribute = 0;
			if ((o.newTribute == A_TO_B && me == o.rel.a) || (o.newTribute == B_TO_A && me == o.rel.b)) {
				// giving tribute
				newTribute = giveTributeQuality(me, them, m, null);
			} else if ((o.newTribute == A_TO_B && me == o.rel.b) || (o.newTribute == B_TO_A && me == o.rel.a)) {
				// receiving tribute
				newTribute = receiveTributeQuality(me, them, m, null);
			}
			if ((o.rel.tribute == A_TO_B && me == o.rel.a) || (o.rel.tribute == B_TO_A && me == o.rel.b)) {
				// giving tribute
				oldTribute = giveTributeQuality(me, them, m, null);
			} else if ((o.rel.tribute == A_TO_B && me == o.rel.b) || (o.rel.tribute == B_TO_A && me == o.rel.a)) {
				// receiving tribute
				oldTribute = receiveTributeQuality(me, them, m, null);
			}
			String desc = "Tribute";
			if (o.newTribute != null) {
				switch (o.newTribute) {
					case A_TO_B:
						desc = me == o.rel.a ? "Paying_Tribute" : "Receiving_Tribute";
						break;
					case B_TO_A:
						desc = me == o.rel.b ? "Paying_Tribute" : "Receiving_Tribute";
						break;
					case NEITHER:
						switch (o.rel.tribute) {
							case A_TO_B:
							desc = me == o.rel.a ? "Stop_Paying_Tribute" : "Stop_Receiving_Tribute";
							break;
						case B_TO_A:
							desc = me == o.rel.b ? "Stop_Paying_Tribute" : "Stop_Receiving_Tribute";
							break;
						}
						break;
				}
			}
			ex(newTribute - oldTribute, desc, e);
			total += newTribute - oldTribute;
		}
		int moneyTransfer = o.moneyTransferAToB * (me == o.rel.a ? 1 : -1);
		if (moneyTransfer > 0) {
			int q = giveMoneyQuality(moneyTransfer, me, them, m, null);
			ex(q, "Send_x_Money", e, moneyTransfer);
			total += q;
		}
		if (moneyTransfer < 0) {
			int q = receiveMoneyQuality(-moneyTransfer, me, them, m, null);
			ex(q, "Receive_x_Money", e, moneyTransfer);
			total += q;
		}
		if (o.submissionAToB != null) {
			if (o.submissionAToB == (me == o.rel.a)) {
				int q = giveSubmissionQuality(me, them, m, null);
				ex(q, "Display_of_Submission", e);
				total += q;
			} else {
				int q = receiveSubmissionQuality(me, them, m, null);
				ex(q, "Receive_Submission", e);
				total += q;
			}
		}
		if (o.annexationAToB != null) {
			if (o.annexationAToB == (me == o.rel.a)) {
				int q = becomeAnnexedQuality(me, them, m, null);
				ex(q, "Become_Annexed", e);
				total += q;
			} else {
				int q = doAnnexQuality(me, them, m, null);
				ex(q, "Do_Annex", e);
				total += q;
			}
		}
		int myTerritoryLosses = 0;
		int theirTerritoryLosses = 0;
		for (Relationship.CityTransfer ct : o.cityTransfers) {
			if (ct.aToB == (me == o.rel.a)) {
				myTerritoryLosses++;
				int q = giveCityQuality(ct.city, me, them, m, null);
				ex(q, "Give_city_x", e, ct.city.name);
				total += q;
			} else {
				theirTerritoryLosses++;
				int q = receiveCityQuality(ct.city, me, them, m, null);
				ex(q, "Receive_city_x", e, ct.city.name);
				total += q;
			}
		}
		if (myTerritoryLosses > theirTerritoryLosses) {
			int q = ultimatumDemand ? personality.territoryUltimatumMalus : personality.territoryLossMalus;
			ex(q, "demanding_territory", e);
			total += q;
		}
		if (forced && m.toggles().contains(ConquestToggle.REPUTATION)) {
			int repLoss = o.totalCascadedForceRepCostsAfterGrievances(m, me);
			total += repLossQuality(repLoss, me, them, m, e);
			int desireToHurt = (int) (m.getRelationship(me, them).getGrievances(me) * personality.grievancesToForcedActionMultiplier);
			ex(desireToHurt, "Diplomatic_Grievances", e);
			total += desireToHurt;
		}
		if (forced) {
			if (o.newLevel != WAR) {
				int relStrength = relativeIndividualStrength(me, them, m);
				if (relStrength < 0) {
					int theirDownside = new DiplomacyAI(personality.assumeOtherPersonality(them)).evaluateOffer(o, them, me, m, false, null);
					if (theirDownside < 0) {
						int avoidAngeringStrongerEmpire = (int) (-relStrength * theirDownside * personality.avoidAngeringStrongerEmpireFactor);
						ex(avoidAngeringStrongerEmpire, "Fear", e);
						total += avoidAngeringStrongerEmpire;
					}
				}
			}
			ArrayList<Offer> cascades = Relationship.cascadeOfferEffects(m, o, me);
			for (Offer c : cascades) {
				if (c.rel.contains(me)) {
					if (e != null) { e.append("\n").append(c.rel.other(me).getName()).append(":"); }
					total += evaluateOffer(c, me, c.rel.other(me), m, false, e, /* considerWarQuality */ false, /* ultimatumDemand */ false);
				}
			}
		}
		
		//total(total, e);
		return total;
	}
	
	public int repLossQuality(int repLoss, Empire me, Empire them, HasRelationships m, StringBuilder e) {
		repLoss = StrictMath.min(repLoss, me.getReputation());
		if (m.isCoronating(me) && me.getReputation() - repLoss < EmpireStat.CORONATION_REPUTATION.get(me.bonuses)) {
			exCmp(-personality.repLossWouldCancelCoronationPenalty, "Reputation_Loss_Would_Cancel_Coronation", e, -base.repLossWouldCancelCoronationPenalty, personality);
			return -personality.repLossWouldCancelCoronationPenalty;
		} else {
			double baseSensitivity = base.reputationLossSensitivity;
			double sensitivity = personality.reputationLossSensitivity;
			if (isDoingBadly(me, m)) {
				baseSensitivity = base.doingBadlyReputationLossSensitivity;
				sensitivity = personality.doingBadlyReputationLossSensitivity;
			}
			if (m.isAboutToConquerAll(me)) {
				baseSensitivity = base.aboutToConquerReputationLossSensitivity;
				sensitivity = personality.aboutToConquerReputationLossSensitivity;
			}
			if (m.isCoronating(me)) {
				baseSensitivity = base.aboutToCoronateReputationLossSensitivity;
				sensitivity = personality.aboutToCoronateReputationLossSensitivity;
			}
			int repCost = (int) (-repLoss * sensitivity);
			exCmp(repCost, "Reputation_Loss", e, (int) (-repLoss * baseSensitivity), personality);
			return repCost;
		}
	}
	
	public int repGainQuality(int repGain, Empire me, Empire them, HasRelationships m, StringBuilder e) {
		repGain = StrictMath.min(repGain, 100 - me.getReputation());
		if (m.hasEnoughCitiesForCoronation(me)) {
			int repValue = (int) (repGain * personality.enoughCitiesForCoronationReputationGainSensitivity);
			exCmp(repValue, "Reputation_Gain_For_Coronation", e, (int) (repGain * base.enoughCitiesForCoronationReputationGainSensitivity), personality);
			return repValue;
		} else {
			int repValue = (int) (repGain * personality.reputationGainSensitivity);
			ex(repValue, "Reputation_Gain", e, (int) (repGain * base.reputationGainSensitivity), personality);
			return repValue;
		}
	}
	
	static void ex(int amt, String text, StringBuilder explain, Object... textParams) {
		if (explain != null && amt != 0) {
			if (explain.length() > 0) { explain.append("\n"); }
			if (amt > 0) {
				explain.append("+ ").append(amt).append(" ").append(_t(text, (Object[]) textParams));
			} else {
				explain.append("- ").append(-amt).append(" ").append(_t(text, (Object[]) textParams));
			}
		}
	}
	
	static void exCmp(int amt, String text, StringBuilder explain, int baseAmt, DiplomacyPersonality personality, Object... textParams) {
		if (explain != null && (amt != 0 || amt != baseAmt)) {
			if (explain.length() > 0) { explain.append("\n"); }
			if (amt != baseAmt) {
				if (amt > 0) {
					explain.append("+ ").append(amt).append(" ").append(_t(text, (Object[]) textParams))
							.append("\n  (").append(baseAmt).append(amt > baseAmt ? " + " : " - ").append(Math.abs(amt - baseAmt)).append(" ").append(_t("from_personality_x", _t("personality_" + personality.name))).append(")");
				} else {
					explain.append("- ").append(-amt).append(" ").append(_t(text, (Object[]) textParams))
							.append("\n  (").append(baseAmt).append(amt > baseAmt ? " + " : " - ").append(Math.abs(amt - baseAmt)).append(" ").append(_t("from_personality_x", _t("personality_" + personality.name))).append(")");
				}
			} else {
				if (amt > 0) {
					explain.append("+ ").append(amt).append(" ").append(_t(text, (Object[]) textParams));
				} else {
					explain.append("- ").append(-amt).append(" ").append(_t(text, (Object[]) textParams));
				}
			}
		}
	}
	
	static void exCmpAlways(int amt, String text, StringBuilder explain, int baseAmt, DiplomacyPersonality personality, Object... textParams) {
		if (explain != null) {
			if (explain.length() > 0) { explain.append("\n"); }
			if (amt != baseAmt) {
				if (amt > 0) {
					explain.append("+ ").append(amt).append(" ").append(_t(text, (Object[]) textParams))
							.append("\n  (").append(baseAmt).append(amt > baseAmt ? " + " : " - ").append(Math.abs(amt - baseAmt)).append(" ").append(_t("from_personality_x", _t("personality_" + personality.name))).append(")");
				} else {
					explain.append("- ").append(-amt).append(" ").append(_t(text, (Object[]) textParams))
							.append("\n  (").append(baseAmt).append(amt > baseAmt ? " + " : " - ").append(Math.abs(amt - baseAmt)).append(" ").append(_t("from_personality_x", _t("personality_" + personality.name))).append(")");
				}
			} else {
				if (amt > 0) {
					explain.append("+ ").append(amt).append(" ").append(_t(text, (Object[]) textParams));
				} else {
					explain.append("- ").append(-amt).append(" ").append(_t(text, (Object[]) textParams));
				}
			}
		}
	}
	
	static void exMult(double mult, String text, StringBuilder explain, Object... textParams) {
		if (explain != null && mult != 1) {
			if (explain.length() > 0) { explain.append("\n"); }
			explain.append("x ").append(mult).append(" ").append(_t(text, (Object[]) textParams));
		}
	}
	
	static void rawEx(int amt, String text, StringBuilder explain) {
		if (explain != null && amt != 0) {
			if (explain.length() > 0) { explain.append("\n"); }
			if (amt > 0) {
				explain.append("+ ").append(amt).append(" ").append(text);
			} else {
				explain.append("- ").append(-amt).append(" ").append(text);
			}
		}
	}
	
	static void rawExCmp(int amt, String text, StringBuilder explain, int baseAmt, DiplomacyPersonality personality) {
		if (explain != null && (amt != 0 || amt != baseAmt)) {
			if (explain.length() > 0) { explain.append("\n"); }
			if (amt != baseAmt) {
				if (amt > 0) {
					explain.append("+ ").append(amt).append(" ").append(text)
							.append(" (").append(baseAmt).append(amt > baseAmt ? " + " : " - ").append(Math.abs(amt - baseAmt)).append(" ").append(_t("from_personality_x", _t("personality_" + personality.name))).append(")");
				} else {
					explain.append("- ").append(-amt).append(" ").append(text)
							.append(" (").append(baseAmt).append(amt > baseAmt ? " + " : " - ").append(Math.abs(amt - baseAmt)).append(" ").append(_t("from_personality_x", _t("personality_" + personality.name))).append(")");
				}
			} else {
				if (amt > 0) {
					explain.append("+ ").append(amt).append(" ").append(text);
				} else {
					explain.append("- ").append(-amt).append(" ").append(text);
				}
			}
		}
	}
	
	static void maximum(int amt, StringBuilder explain) {
		if (explain != null) {
			if (explain.length() > 0) {
				explain.append("\n");
			}
			explain.append(_t("diplo_quality_maximum_")).append(amt);
		}
	}
	
	static void minimum(int amt, StringBuilder explain) {
		if (explain != null) {
			if (explain.length() > 0) {
				explain.append("\n");
			}
			explain.append(_t("diplo_quality_minimum_")).append(amt);
		}
	}
	
	static void total(int amt, StringBuilder explain) {
		if (explain != null) {
			if (explain.length() > 0) {
				explain.append("\n\n");
			}
			explain.append(_t("diplo_quality_total_")).append(amt);
		}
	}
	
	public int offensiveStrengthIncludingAllies(Empire me, Empire victim, HasRelationships m) {
		int str = m.getFleetStrength(me);
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.level == ALLIANCE && m.getRelationship(rel.other(me), victim).level != ALLIANCE) {
				str += (int) (m.getFleetStrength(rel.other(me)) * personality.allyStrengthEstimationFactor);
			}
		}
		return str;
	}
	
	public int defensiveStrengthIncludingPacts(Empire me, Empire exceptFor, HasRelationships m) {
		int str = m.getFleetStrength(me) + m.getDefensesStrength(me);
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == exceptFor) { continue; }
			if (rel.level == ALLIANCE || rel.level == DEFENSIVE_PACT) {
				str += (int) (m.getFleetStrength(rel.other(me)) * personality.allyStrengthEstimationFactor);
			}
		}
		return str;
	}
	
	public int relativeDeclareWarStrength(Empire me, Empire them, HasRelationships m) {
		return relStrength(offensiveStrengthIncludingAllies(me, them, m), defensiveStrengthIncludingPacts(them, me, m));
	}
		
	public int singleEmpireStrength(Empire me, HasRelationships m) {
		int s = m.getFleetStrength(me) + m.getDefensesStrength(me);
		return s;
	}
	
	public static int relStrength(int me, int them) {
		return (me - them) * 100 / (me + them + 4000);
	}
	
	public int relativeIndividualStrength(Empire me, Empire them, HasRelationships m) {
		return relStrength((int) (singleEmpireStrength(me, m) * personality.ownStrengthEstimationMultiplier), singleEmpireStrength(them, m));
	}
	
	HashMap<Empire, Integer> relativeAtWarStrengths = new HashMap<Empire, Integer>();
	
	public int relativeAtWarStrength(Empire me, Empire them, HasRelationships m) {
		if (relativeAtWarStrengths.containsKey(them)) { return relativeAtWarStrengths.get(them); }
		
		int myStrength = (int) (m.getFleetStrength(me) * personality.ownStrengthEstimationMultiplier) + m.getDefensesStrength(me);
		for (Relationship rel : m.getRelationships(me)) {
			Empire other = rel.other(me);
			if ((rel.level == DEFENSIVE_PACT || rel.level == ALLIANCE) && m.getRelationship(other, them).level == WAR) {
				myStrength += (int) (m.getFleetStrength(other) * personality.allyStrengthEstimationFactor);
			}
		}
		int theirStrength = m.getFleetStrength(them) + m.getDefensesStrength(them);
		for (Relationship rel : m.getRelationships(them)) {
			Empire other = rel.other(them);
			if ((rel.level == DEFENSIVE_PACT || rel.level == ALLIANCE) && m.getRelationship(other, me).level == WAR) {
				theirStrength += (int) (m.getFleetStrength(other) * personality.allyStrengthEstimationFactor);
			}
		}
		int rs = relStrength(myStrength, theirStrength);
		relativeAtWarStrengths.put(them, rs);
		return rs;
	}
			
	public double similarStrengthMult(Empire me, Empire them, HasRelationships m) {
		int myStrength = (int) (m.getFleetStrength(me) * personality.ownStrengthEstimationMultiplier);
		int theirStrength = m.getFleetStrength(them);
		// output 1 if they are the same, 0 if they are infinitely different
		return 1 - StrictMath.abs(myStrength - theirStrength) * 1.0 / StrictMath.max(myStrength, theirStrength);
	}
	
	// Outputs positive values for the strength of other wars we are involved in.
	public int otherWars(Empire me, Empire them, HasRelationships m) {
		int otherWars = 0;
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == them) { continue; }
			Relationship otherRelWithThem = m.getRelationship(them, rel.other(me));
			if (rel.level == WAR && otherRelWithThem.level != WAR && !(otherRelWithThem.level == DEFENSIVE_PACT && otherRelWithThem.defensivePactWarMainDefender == them) && otherRelWithThem.level != ALLIANCE) {
				otherWars += 5; // Baseline
				Empire other = rel.other(me);
				int otherWarStrength = relativeAtWarStrength(me, other, m);
				if (otherWarStrength < 0) {
					otherWars -= otherWarStrength;
				}
			}
		}
		return StrictMath.min(50, otherWars);
	}
	
	public double sharedWars(Empire me, Empire them, HasRelationships m) {
		int enemyStrength = 0;
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == them) { continue; }
			if (rel.level == WAR && m.getRelationship(them, rel.other(me)).level == WAR) {
				enemyStrength += m.getFleetStrength(rel.other(me));
			}
		}
		return enemyStrength * 1.0 / (m.getFleetStrength(me) + m.getFleetStrength(them) + 1000);
	}
	
	public static int distrustScale(int value, Empire e, HasRelationships m) {
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			return value * (100 - e.getReputation()) / 100;
		} else {
			return value / 2;
		}
	}
		
	public int neighbourThreat(Empire me, Empire them, HasRelationships m) {
		int worstNeighbour = 0;
		int totalThreat = 0;
		int largestNeighbour = 0;
		for (Relationship rel : m.getRelationships(me)) {
			Empire other = rel.other(me);
			if (other == them) { continue; }
			largestNeighbour = StrictMath.max(largestNeighbour, other.cities.size());
			if (rel.level != WAR) {
				int relativeStrength = relativeIndividualStrength(me, other, m);
				if (rel.level.ordinal() >= NON_AGGRESSION_PACT.ordinal()) {
					// You have a treaty, but do you trust them?
					relativeStrength = distrustScale(relativeStrength, them, m);
				}
				if (!m.isAdjacent(me, other)) {
					relativeStrength /= 4;
				}
				//System.out.println(other.getName() + " " + relativeStrength);
				// Note that relativeStrength values for empires we are scared of are negative.
				worstNeighbour = StrictMath.min(worstNeighbour, relativeStrength);
				totalThreat += StrictMath.min(0, relativeStrength);
			}
		}
		int fromNeighbourSize = StrictMath.max(0, largestNeighbour * 400 / m.numCities() - 100);
		return fromNeighbourSize + StrictMath.max(0, -(worstNeighbour * 4 / 5 + totalThreat / 5)); // 80-20 mix of the worst enemy and everyone
	}
	
	HashMap<Empire, Integer> warQualities = new HashMap<Empire, Integer>();
	HashMap<Empire, Integer> warQualitiesForced = new HashMap<Empire, Integer>();
	HashMap<Empire, Boolean> isDoingBadly = new HashMap<Empire, Boolean>();
	
	public boolean isDoingBadly(Empire me, HasRelationships m) {
		if (!isDoingBadly.containsKey(me)) {
			isDoingBadly.put(me, m.isDoingBadly(me));
		}
		return isDoingBadly.get(me);
	}
	
	public int warQuality(Empire me, Empire them, HasRelationships m, StringBuilder e, boolean forced, boolean canConsultAllies) {
		if (canConsultAllies && e == null && (forced ? warQualitiesForced : warQualities).containsKey(them)) {
			return (forced ? warQualitiesForced : warQualities).get(them);
		}
		
		int q = personality.warBaseline;
		exCmp(personality.warBaseline, "Baseline", e, base.warBaseline, personality);
		
		// has ritual sites that I want
		if (personality.ritualSitesWarBonus != 0 && m.hasRitualSite(them)) {
			ex(personality.ritualSitesWarBonus, "Desires_Ritual_Site", e);
			q += personality.ritualSitesWarBonus;
		}
		
		// relative strength
		int relativeStrength = (int) (relativeDeclareWarStrength(me, them, m) * personality.warRelativeStrengthFactor);
		ex(relativeStrength, "Relative_Strength", e);
		q += relativeStrength;
		
		// desires
		for (CityUpgradeType cut : me.desiredUpgrades(m)) {
			if (them.has(cut)) {
				rawEx(personality.desireSpecialWarBonus, _t("desires_x", cut.getName()), e);
				q += personality.desireSpecialWarBonus;
				break;
			}
		}
		
		// adjacency
		if (forced && !m.isAdjacent(me, them) && m.hasAdjacentNonAlliedEmpires(me)) {
			int notAdjacent = -80;
			ex(notAdjacent, "Not_Adjacent", e);
			q += notAdjacent;
		}
		
		// other wars I'm involved in
		int otherWars = -otherWars(me, them, m);
		ex(otherWars, "Other_Wars", e);
		q += otherWars;
		
		// unrest
		int avgUnrest = m.getAverageUnrest(me);
		int fromUnrest = (int) - ((avgUnrest - personality.warUnrestTolerance) * (m.unrestPerCity(me.bonuses) + m.unrestPerTown(me.bonuses)) * personality.extraUnrestAvoidWarFactor);
		int fromUnrestBase = (int) - ((avgUnrest - base.warUnrestTolerance) * (m.unrestPerCity(me.bonuses) +m.unrestPerTown(me.bonuses)) * base.extraUnrestAvoidWarFactor);
		if (fromUnrest < 0) {
			exCmp(fromUnrest, "Unrest", e, fromUnrestBase, personality);
			q += fromUnrest;
		}
		
		if (m.isAboutToWin(me) && !m.isCoronating(me) && !m.isDoingFinalRitual(me) && m.getRelationship(me, them).level != ALLIANCE) {
			exCmp(personality.inTheWayOfVictoryWarBonus, "In_The_Way_Of_Victory", e, base.inTheWayOfVictoryWarBonus, personality);
			q += personality.inTheWayOfVictoryWarBonus;
		}
		
		if (m.isCoronating(them)) {
			exCmp(personality.aboutToCoronateWarBonus, "Prevent_Coronation", e, base.aboutToCoronateWarBonus, personality);
			q += personality.aboutToCoronateWarBonus;
		}
		
		if (m.isDoingFinalRitual(them)) {
			exCmp(personality.aboutToDoFinalRitualWarBonus, "Prevent_Final_Ritual", e, base.aboutToDoFinalRitualWarBonus, personality);
			q += personality.aboutToDoFinalRitualWarBonus;
		}
		
		if (records.containsKey(them)) {
			int aggression = records.get(them).warAggression;
			ex(aggression, "Hunger_For_War", e);
			q += aggression;
		}
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			int rep = StrictMath.max(0, (int) ((50 - them.getReputation()) * 2 * personality.declareWarOnLowRepMultiplier));
			int repBase = StrictMath.max(0, (int) ((50 - them.getReputation()) * 2 * base.declareWarOnLowRepMultiplier));
			exCmp(rep, "Enemy_Reputation", e, repBase, personality);
			q += rep;
		}
		
		// opinion
		int op = -personality.bonusOpinion.get(them.bonuses);
		ex(op, "Opinion", e);
		q += op;
		
		// one ninja at a time
		if (them.playerControlled) {
			boolean theyAreAtWar = false;
			for (Relationship rel : m.getRelationships(them)) {
				// They are at war or there's a war ultimatum going.
				if (rel.other(them) != me && (rel.level == WAR || (rel.getUltimatum(m) != null && rel.getUltimatum(m).getOrElse().newLevel == WAR))) {
					theyAreAtWar = true;
					break;
				}
			}
			if (theyAreAtWar) {
				ex(-m.aiAvoidWarWithHumanPlayersInOtherWars(), "Difficulty_Level", e);
				q -= m.aiAvoidWarWithHumanPlayersInOtherWars();
			}
		}
		
		if (q > personality.maxWarQuality) {
			maximum(personality.maxWarQuality, e);
			q = personality.maxWarQuality;
		} else if (q < personality.minWarQuality) {
			minimum(personality.minWarQuality, e);
			q = personality.minWarQuality;
		}
		
		int highestAllyQ = 0;
		String ally = null;
		if (canConsultAllies) {
			for (Relationship rel : m.getRelationships(me)) {
				if (rel.level == ALLIANCE && rel.other(me) != them && !rel.other(me).playerControlled) {
					int allyQ = rel.other(me).diplomacyAI.warQuality(rel.other(me), them, m, null, forced, false);
					if (allyQ > q && (ally == null || allyQ > highestAllyQ)) {
						highestAllyQ = allyQ;
						ally = rel.other(me).getName();
					}
				}
			}
			if (ally != null) {
				int amt = (highestAllyQ - q) * 3 / 4; 
				rawEx(amt, ally, e);
				q += amt;
			}
		}
		
		total(q, e);
		
		if (canConsultAllies) { (forced ? warQualitiesForced : warQualities).put(them, q); }
		
		return q;
	}
	
	HashMap<Empire, Integer> peaceQualities = new HashMap<Empire, Integer>();
	
	public int peaceQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && peaceQualities.containsKey(them)) { return peaceQualities.get(them); }
		
		int q = personality.peaceBaseline;
		exCmp(personality.peaceBaseline, "Baseline", e, base.peaceBaseline, personality);
		
		// relative strength and other wars -- makes no sense, is already accounted for
		/*
		int relativeStrength = (int) (StrictMath.max(0, -relativeAtWarStrength(me, them, m)) * personality.peaceRelativeStrengthFactor);
		int relativeStrengthBase = (int) (StrictMath.max(0, -relativeAtWarStrength(me, them, m)) * base.peaceRelativeStrengthFactor);
		exCmp(relativeStrength, "Relative_Strength", e, relativeStrengthBase, personality);
		q += relativeStrength;
		
		// other wars I'm involved in
		int otherWars = otherWars(me, them, m);
		ex(otherWars, "Other_Wars", e);
		q += otherWars;*/
		
		// long war
		Relationship rel = m.getRelationship(me, them);
		if (rel.level == WAR && rel.levelAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50) {
			int longWar = (rel.levelAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4);
			ex(longWar, "Long_War", e);
			q += longWar;
		}
		
		total(q, e);
		
		peaceQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> nonAggressionPactQualities = new HashMap<Empire, Integer>();
	
	public int nonAggressionPactQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && nonAggressionPactQualities.containsKey(them)) { return nonAggressionPactQualities.get(them); }
		
		int q = personality.nonAggressionPactBaseline;
		exCmp(personality.nonAggressionPactBaseline, "Baseline", e, base.nonAggressionPactBaseline, personality);
		
		// loyalty
		if (m.getRelationship(me, them).level == NON_AGGRESSION_PACT) {
			exCmp(personality.nonAggressionPactLoyalty, "Treaty_Commitment", e, base.nonAggressionPactLoyalty, personality);
			q += personality.nonAggressionPactLoyalty;
		}
		
		// relative strength
		int relativeStrength = (int) -relativeIndividualStrength(me, them, m);
		int relativeStrengthBase = relativeStrength;
		if (relativeStrength < 0) {
			relativeStrength *= personality.nonAggressionPactOtherIsWeakerFactor;
			relativeStrengthBase *= base.nonAggressionPactOtherIsWeakerFactor;
		} else {
			relativeStrength *= personality.nonAggressionPactOtherIsStrongerFactor;
			relativeStrengthBase *= base.nonAggressionPactOtherIsStrongerFactor;
		}
		exCmp(relativeStrength, "Relative_Strength", e, relativeStrengthBase, personality);
		q += relativeStrength;
		
		// adjacency
		if (!m.isAdjacent(me, them)) {
			int notAdjacent = -25;
			ex(notAdjacent, "Not_Adjacent", e);
			q += notAdjacent;
		}
		
		// other wars I'm involved in
		int otherWars = otherWars(me, them, m);
		ex(otherWars, "Other_Wars", e);
		q += otherWars;
		
		// threatening neighbours
		int neighbourThreat = neighbourThreat(me, them, m);
		int neighbourThreatBase = (int) (neighbourThreat * base.nonAggressionPactNeighbourThreatMultiplier);
		neighbourThreat = (int) (neighbourThreat * personality.nonAggressionPactNeighbourThreatMultiplier);
		exCmp(neighbourThreat, "Threatening_Neighbours", e, neighbourThreatBase, personality);
		q += neighbourThreat;
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION) && them.getReputation() < 50) {
			int rep = (int) ((them.getReputation() - 50) * personality.avoidNonAggressionPactWithLowRepMultiplier);
			int repBase = (int) ((them.getReputation() - 50) * base.avoidNonAggressionPactWithLowRepMultiplier);
			exCmp(rep, "Reputation", e, repBase, personality);
			q += rep;
		}
		
		// opinion
		int op = personality.bonusOpinion.get(them.bonuses);
		ex(op, "Opinion", e);
		q += op;
		
		// other treaties I'm involved in
		int numTreaties = 0;
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == them) { continue; }
			if (rel.level.ordinal() > PEACE.ordinal()) {
				numTreaties++;
			}
		}
		exCmp(-numTreaties * numTreaties * personality.avoidNonAggressionPactPerOtherPact, "Other_Pacts", e, -numTreaties * numTreaties * base.avoidNonAggressionPactPerOtherPact, personality);
		q += -numTreaties * numTreaties * personality.avoidNonAggressionPactPerOtherPact;
		
		// long peace / rel
		Relationship rel = m.getRelationship(me, them);
		if (rel.positiveRelationshipAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50) {
			int longPact = StrictMath.min(personality.nonAggressionPactLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			int longPactBase = StrictMath.min(base.nonAggressionPactLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			exCmp(longPact, "Long_Relationship", e, longPactBase, personality);
			q += longPact;
		}
		
		// about to win by conquest
		if (m.isAboutToConquerAll(me)) {
			ex(-personality.avoidTreatiesWhenAboutToConquerAll, "About_To_Conquer_All", e);
			q -= personality.avoidTreatiesWhenAboutToConquerAll;
		}
		
		if (!me.playerControlled && !them.playerControlled) {
			ex(m.aiTreatyBonus(), "Cooperation", e);
			q += m.aiTreatyBonus();
		}
		
		if (q > personality.maxNonAggressionPactQuality) {
			maximum(personality.maxNonAggressionPactQuality, e);
			q = personality.maxNonAggressionPactQuality;
		}
		
		total(q, e);
		
		nonAggressionPactQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> defensivePactQualities = new HashMap<Empire, Integer>();
	
	public int defensivePactQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && defensivePactQualities.containsKey(them)) { return defensivePactQualities.get(them); }
		
		int q = personality.defensivePactBaseline;
		exCmp(personality.defensivePactBaseline, "Baseline", e, base.defensivePactBaseline, personality);
		
		if (EmpireStat.DEFENSIVE_PACT_INCOME_GAINED_PERCENTAGE.get(me.bonuses) > 0) {
			double ratio = m.incomeForComparison(them) * 1.0 / StrictMath.max(40, m.incomeForComparison(me));
			int relativeIncomeGain = (int) (ratio * personality.tradeTreatyRelativeIncomeBonus);
			int relativeIncomeGainBase = (int) (ratio * personality.tradeTreatyRelativeIncomeBonus);
			exCmp(relativeIncomeGain, "Income_Gained", e, relativeIncomeGainBase, personality);
			q += relativeIncomeGain;
		}
		
		// loyalty
		if (m.getRelationship(me, them).level == NON_AGGRESSION_PACT) {
			exCmp(personality.nonAggressionPactLoyalty, "Treaty_Commitment", e, base.nonAggressionPactLoyalty, personality);
			q += personality.nonAggressionPactLoyalty;
		}
		if (m.getRelationship(me, them).level == DEFENSIVE_PACT) {
			exCmp(personality.defensivePactLoyalty, "Treaty_Commitment", e, base.defensivePactLoyalty, personality);
			q += personality.defensivePactLoyalty;
		}
		
		// relative strength
		int relativeStrength = (int) -relativeIndividualStrength(me, them, m);
		int relativeStrengthBase = relativeStrength;
		if (relativeStrength < 0) {
			relativeStrength = 0;
		} else {
			relativeStrength *= personality.defensivePactOtherIsStrongerFactor;
			relativeStrengthBase *= base.defensivePactOtherIsStrongerFactor;
		}
		double ssm = similarStrengthMult(me, them, m);
		int similarStrength = (int) (ssm * personality.defensivePactSimilarStrengthBonus);
		int similarStrengthBase = (int) (ssm * base.defensivePactSimilarStrengthBonus);
		if (relativeStrength > similarStrength) {
			exCmp(relativeStrength, "Relative_Strength", e, relativeStrengthBase, personality);
			q += relativeStrength;
		} else {
			exCmp(similarStrength, "Similar_Strength", e, similarStrengthBase, personality);
			q += similarStrength;
		}
		
		// adjacency
		if (!m.isAdjacent(me, them)) {
			int notAdjacent = -25;
			ex(notAdjacent, "Not_Adjacent", e);
			q += notAdjacent;
		}
		
		// other wars I'm involved in
		int otherWars = otherWars(me, them, m);
		ex(otherWars, "Other_Wars", e);
		q += otherWars;
		
		// threatening neighbours
		int neighbourThreat = (int) neighbourThreat(me, them, m);
		int neighbourThreatBase = (int) ((neighbourThreat * base.defensivePactNeighbourThreatMultiplier));
		neighbourThreat = (int) (neighbourThreat * personality.defensivePactNeighbourThreatMultiplier);
		exCmp(neighbourThreat, "Threatening_Neighbours", e, neighbourThreatBase, personality);
		q += neighbourThreat;
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			int rep = (int) ((them.getReputation() - 50) * personality.defensivePactPerRepMultiplier);
			int repBase = (int) ((them.getReputation() - 50) * base.defensivePactPerRepMultiplier);
			exCmp(rep, "Reputation", e, repBase, personality);
			q += rep;
		}
		
		// opinion
		int op = personality.bonusOpinion.get(them.bonuses);
		ex(op, "Opinion", e);
		q += op;
		
		// other treaties I'm involved in
		int numOtherTreaties = 0;
		int numAITreaties = 0;
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == them) { continue; }
			if (rel.level.ordinal() > NON_AGGRESSION_PACT.ordinal()) {
				numOtherTreaties++;
				if (!me.playerControlled && !rel.other(me).playerControlled) {
					numAITreaties++;
				}
			}
		}
		// other treaties they're involved in
		for (Relationship rel : m.getRelationships(them)) {
			if (rel.other(them) == me) { continue; }
			if (rel.level.ordinal() > NON_AGGRESSION_PACT.ordinal()) {
				numOtherTreaties++;
				if (!them.playerControlled && !rel.other(them).playerControlled) {
					numAITreaties++;
				}
			}
		}
		int otherTreaties = StrictMath.min(0, -numOtherTreaties * numOtherTreaties * personality.avoidDefensivePactPerOtherPact - numAITreaties * numAITreaties * m.aiAvoidPactPerOtherAIPact());
		int otherTreatiesBase = StrictMath.min(0, -numOtherTreaties * numOtherTreaties * base.avoidDefensivePactPerOtherPact - numAITreaties * numAITreaties * m.aiAvoidPactPerOtherAIPact());
		exCmp(otherTreaties, "Other_Pacts", e, otherTreatiesBase, personality);
		//System.out.println("numOtherTreaties " + numOtherTreaties + ": " + otherTreaties);
		q += otherTreaties;
		
		// long relationship
		Relationship rel = m.getRelationship(me, them);
		if (rel.positiveRelationshipAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50) {
			int longPact = StrictMath.min(personality.defensivePactLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			int longPactBase = StrictMath.min(base.defensivePactLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			exCmp(longPact, "Long_Relationship", e, longPactBase, personality);
			q += longPact;
		}
		
		// about to win by conquest
		if (m.isAboutToConquerAll(me)) {
			ex(-personality.avoidTreatiesWhenAboutToConquerAll, "About_To_Conquer_All", e);
			q -= personality.avoidTreatiesWhenAboutToConquerAll;
		}
		
		if (!me.playerControlled && !them.playerControlled) {
			ex(m.aiTreatyBonus(), "Cooperation", e);
			q += m.aiTreatyBonus();
		}
		
		if (q > personality.maxDefensivePactQuality) {
			maximum(personality.maxDefensivePactQuality, e);
			q = personality.maxDefensivePactQuality;
		}
		
		total(q, e);
		
		defensivePactQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> allianceQualities = new HashMap<Empire, Integer>();
	
	public int allianceQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && allianceQualities.containsKey(them)) { return allianceQualities.get(them); }
		
		int q = personality.allianceBaseline;
		exCmp(personality.allianceBaseline, "Baseline", e, base.allianceBaseline, personality);
		
		if (EmpireStat.DEFENSIVE_PACT_INCOME_GAINED_PERCENTAGE.get(me.bonuses) > 0) {
			double ratio = m.incomeForComparison(them) * 1.0 / StrictMath.max(40, m.incomeForComparison(me));
			int relativeIncomeGain = (int) (ratio * personality.tradeTreatyRelativeIncomeBonus);
			int relativeIncomeGainBase = (int) (ratio * personality.tradeTreatyRelativeIncomeBonus);
			exCmp(relativeIncomeGain, "Income_Gained", e, relativeIncomeGainBase, personality);
			q += relativeIncomeGain;
		}
		
		// loyalty
		if (m.getRelationship(me, them).level == NON_AGGRESSION_PACT) {
			exCmp(personality.nonAggressionPactLoyalty, "Treaty_Commitment", e, base.nonAggressionPactLoyalty, personality);
			q += personality.nonAggressionPactLoyalty;
		}
		if (m.getRelationship(me, them).level == DEFENSIVE_PACT) {
			exCmp(personality.defensivePactLoyalty, "Treaty_Commitment", e, base.defensivePactLoyalty, personality);
			q += personality.defensivePactLoyalty;
		}
		if (m.getRelationship(me, them).level == ALLIANCE) {
			exCmp(personality.allianceLoyalty, "Treaty_Commitment", e, base.allianceLoyalty, personality);
			q += personality.allianceLoyalty;
		}
		
		// relative strength
		int relativeStrength = (int) -relativeIndividualStrength(me, them, m);
		int relativeStrengthBase = relativeStrength;
		if (relativeStrength < 0) {
			relativeStrength = 0;
		} else {
			relativeStrength *= personality.allianceOtherIsStrongerFactor;
			relativeStrengthBase *= base.allianceOtherIsStrongerFactor;
		}
		double ssm = similarStrengthMult(me, them, m);
		int similarStrength = (int) (ssm * personality.allianceSimilarStrengthBonus);
		int similarStrengthBase = (int) (ssm * base.allianceSimilarStrengthBonus);
		if (relativeStrength > similarStrength) {
			exCmp(relativeStrength, "Relative_Strength", e, relativeStrengthBase, personality);
			q += relativeStrength;
		} else {
			exCmp(similarStrength, "Similar_Strength", e, similarStrengthBase, personality);
			q += similarStrength;
		}
		
		// adjacency
		if (!m.isAdjacent(me, them)) {
			int notAdjacent = -20;
			ex(notAdjacent, "Not_Adjacent", e);
			q += notAdjacent;
		}
		
		// other wars I'm involved in
		int otherWars = otherWars(me, them, m);
		ex(otherWars, "Other_Wars", e);
		q += otherWars;
		
		int theirOtherWars = -otherWars(them, me, m);
		ex(theirOtherWars, "Their_Other_Wars", e);
		q += theirOtherWars;
		
		// shared wars
		double sharedWarsMult = sharedWars(me, them, m);
		int sharedWars = (int) (sharedWarsMult * personality.allianceSharedWarsBonus);
		int sharedWarsBase = (int) (sharedWarsMult * base.allianceSharedWarsBonus);
		exCmp(sharedWars, "Shared_Wars", e, sharedWarsBase, personality);
		q += sharedWars;
		
		// threatening neighbours
		int neighbourThreat = neighbourThreat(me, them, m);
		int neighbourThreatBase = (int) (neighbourThreat * base.allianceNeighbourThreatMultiplier);
		neighbourThreat = (int) (neighbourThreat * personality.allianceNeighbourThreatMultiplier);
		exCmp(neighbourThreat, "Threatening_Neighbours", e, neighbourThreatBase, personality);
		q += neighbourThreat;
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			int rep = (int) ((them.getReputation() - 50) * personality.alliancePerRepMultiplier);
			int repBase = (int) ((them.getReputation() - 50) * base.alliancePerRepMultiplier);
			exCmp(rep, "Reputation", e, repBase, personality);
			q += rep;
		}
		
		// opinion
		int op = personality.bonusOpinion.get(them.bonuses);
		ex(op, "Opinion", e);
		q += op;
		
		int numOtherTreaties = 0;
		int numAITreaties = 0;
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == them) { continue; }
			if (rel.level == ALLIANCE) {
				numOtherTreaties++;
				if (!me.playerControlled && !rel.other(me).playerControlled) {
					numAITreaties++;
				}
			}
		}
		// other treaties they're involved in
		for (Relationship rel : m.getRelationships(them)) {
			if (rel.other(them) == me) { continue; }
			if (rel.level == ALLIANCE) {
				numOtherTreaties++;
				if (!them.playerControlled && !rel.other(them).playerControlled) {
					numAITreaties++;
				}
			}
		}
		int otherTreaties = StrictMath.min(0, -numOtherTreaties * numOtherTreaties * personality.avoidAlliancePerOtherAlliance - numAITreaties * (numAITreaties + 1) * m.aiAvoidPactPerOtherAIPact());
		int otherTreatiesBase = StrictMath.min(0, -numOtherTreaties * numOtherTreaties * base.avoidAlliancePerOtherAlliance - numAITreaties * (numAITreaties + 1) * m.aiAvoidPactPerOtherAIPact());
		exCmp(otherTreaties, "Other_Alliances", e, otherTreatiesBase, personality);
		q += otherTreaties;
		
		// long relationship
		Relationship rel = m.getRelationship(me, them);
		if (rel.positiveRelationshipAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50) {
			int longRel = StrictMath.min(personality.allianceLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			int longRelBase = StrictMath.min(base.allianceLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			exCmp(longRel, "Long_Relationship", e, longRelBase, personality);
			q += longRel;
		}
		
		// is someone about to win?
		if (m.isAboutToConquerAll(me)) {
			ex(-personality.avoidTreatiesWhenAboutToConquerAll, "About_To_Conquer_All", e);
			q -= personality.avoidTreatiesWhenAboutToConquerAll;
		} else if (m.isAboutToWin(me) && m.toggles().contains(ConquestToggle.ALLIANCE_VICTORY)) {
			ex(-personality.avoidAllianceWhenAboutToWin, "Unwilling_To_Share_Victory", e);
			q -= personality.avoidAllianceWhenAboutToWin;
		} else if (m.isAboutToWin(them) && m.toggles().contains(ConquestToggle.ALLIANCE_VICTORY)) {
			ex(personality.wantAllianceWhenOtherAboutToWin, "Wants_To_Be_In_Victorious_Alliance", e);
			q += personality.wantAllianceWhenOtherAboutToWin;
		}
		
		if (!me.playerControlled && !them.playerControlled) {
			ex(m.aiTreatyBonus(), "Cooperation", e);
			q += m.aiTreatyBonus();
		}
		
		total(q, e);
		
		if (q > personality.maxAllianceQuality) {
			maximum(personality.maxAllianceQuality, e);
			q = personality.maxAllianceQuality;
		}
		
		allianceQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> giveTributeQualities = new HashMap<Empire, Integer>();
	
	public int giveTributeQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && giveTributeQualities.containsKey(them)) { return giveTributeQualities.get(them); }
		
		int q = personality.giveTributeBaseline;
		exCmp(personality.giveTributeBaseline, "Baseline", e, base.giveTributeBaseline, personality);
		
		int relativeStrength = relativeIndividualStrength(me, them, m);
		int relativeStrengthBase = (int) (relativeStrength * base.tributeRelativeStrengthMult);
		relativeStrength = (int) (relativeStrength * personality.tributeRelativeStrengthMult);
		
		if (relativeStrength < 0 && m.getRelationship(me, them).level.ordinal() < NON_AGGRESSION_PACT.ordinal()) {
			exCmp(-relativeStrength, "Fear", e, -relativeStrengthBase, personality);
			q -= relativeStrength;
		} else if (relativeStrength > 0) {
			exCmp(-relativeStrength, "Relative_Strength", e, -relativeStrengthBase, personality);
			q -= relativeStrength;
		}
		
		int otherTributes = 0;
		int otherTributesBase = 0;
		for (Relationship rel : m.getRelationships(me)) {
			if (rel.other(me) == them) { continue; }
			if (rel.getSendingTribute(me)) {
				otherTributes -= personality.avoidGivingTributePerOtherTribute;
				otherTributesBase -= base.avoidGivingTributePerOtherTribute;
			}
		}
		exCmp(otherTributes, "Other_Tributes", e, otherTributesBase, personality);
		q += otherTributes;
		
		total(q, e);
		
		giveTributeQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> receiveTributeQualities = new HashMap<Empire, Integer>();
	
	public int receiveTributeQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && receiveTributeQualities.containsKey(them)) { return receiveTributeQualities.get(them); }
		
		int q = personality.receiveTributeBaseline;
		exCmp(personality.receiveTributeBaseline, "Baseline", e, base.receiveTributeBaseline, personality);
		
		double ratio = m.incomeForComparison(them) * 1.0 / StrictMath.max(40, m.incomeForComparison(me));
		int relativeIncomeGain = (int) (ratio * personality.receiveTributeRelativeIncomeBonus);
		int relativeIncomeGainBase = (int) (ratio * base.receiveTributeRelativeIncomeBonus);
		exCmp(relativeIncomeGain, "Income_Gained", e, relativeIncomeGainBase, personality);
		q += relativeIncomeGain;
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION) && them.getReputation() < 25) {
			int rep = (int) ((them.getReputation() - 25) * personality.avoidTributeFromVeryLowRepMultiplier);
			int repBase = (int) ((them.getReputation() - 25) * base.avoidTributeFromVeryLowRepMultiplier);
			exCmp(rep, "Reputation", e, repBase, personality);
			q += rep;
		}
		
		if (q > personality.maxReceiveTributeQuality) {
			maximum(personality.maxReceiveTributeQuality, e);
			q = personality.maxReceiveTributeQuality;
		}
		
		total(q, e);
		
		receiveTributeQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> tradeTreatyQualities = new HashMap<Empire, Integer>();
	
	public int tradeTreatyQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && tradeTreatyQualities.containsKey(them)) { return tradeTreatyQualities.get(them); }
		
		int q = personality.tradeTreatyBaseline;
		exCmp(personality.tradeTreatyBaseline, "Baseline", e, base.tradeTreatyBaseline, personality);
		
		double ratio = m.incomeForComparison(them) * 1.0 / StrictMath.max(40, m.incomeForComparison(me));
		int relativeIncomeGain = (int) (ratio * personality.tradeTreatyRelativeIncomeBonus);
		int relativeIncomeGainBase = (int) (ratio * personality.tradeTreatyRelativeIncomeBonus);
		exCmp(relativeIncomeGain, "Income_Gained", e, relativeIncomeGainBase, personality);
		q += relativeIncomeGain;
		
		// long relationship
		Relationship rel = m.getRelationship(me, them);
		if (rel.positiveRelationshipAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50) {
			int longRel = StrictMath.min(personality.tradeTreatyLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			int longRelBase = StrictMath.min(base.tradeTreatyLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			exCmp(longRel, "Long_Relationship", e, longRelBase, personality);
			q += longRel;
		}
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION) && them.getReputation() < 25) {
			int rep = (int) ((them.getReputation() - 25) * personality.avoidTradeTreatyFromVeryLowRepMultiplier);
			int repBase = (int) ((them.getReputation() - 25) * base.avoidTradeTreatyFromVeryLowRepMultiplier);
			exCmp(rep, "Reputation", e, repBase, personality);
			q += rep;
		}
		
		// opinion
		int op = personality.bonusOpinion.get(them.bonuses);
		ex(op, "Opinion", e);
		q += op;
		
		if (!me.playerControlled && !them.playerControlled) {
			ex(m.aiCooperationBonus(), "Cooperation", e);
			q += m.aiCooperationBonus();
		}
		
		if (q > personality.maxTradeTreatyQuality) {
			maximum(personality.maxTradeTreatyQuality, e);
			q = personality.maxTradeTreatyQuality;
		}
		
		total(q, e);
		
		tradeTreatyQualities.put(them, q);
		
		return q;
	}
	
	HashMap<Empire, Integer> researchTreatyQualities = new HashMap<Empire, Integer>();
	
	public int researchTreatyQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && researchTreatyQualities.containsKey(them)) { return researchTreatyQualities.get(them); }
		
		int q = personality.researchTreatyBaseline;
		exCmp(personality.researchTreatyBaseline, "Baseline", e, base.researchTreatyBaseline, personality);
		
		double ratio = m.shareableResearch(them) * 1.0 / m.shareableResearch(me);
		int relativeResearchGain = (int) (ratio * personality.researchTreatyRelativeResearchBonus);
		int relativeResearchGainBase = (int) (ratio * base.researchTreatyRelativeResearchBonus);
		exCmp(relativeResearchGain, "Research_Gained", e, relativeResearchGainBase, personality);
		q += relativeResearchGain;
		
		int techs = m.getNumberOfUnknownTechs(me, them);
		int techsBoosted = techs * personality.researchTreatyPerBoostedTechBonus;
		int techsBoostedBase = techs * base.researchTreatyPerBoostedTechBonus;
		exCmp(techsBoosted, "Technologies_Boosted", e, techsBoostedBase, personality);
		q += techsBoosted;
		
		// long relationship
		Relationship rel = m.getRelationship(me, them);
		if (rel.positiveRelationshipAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50) {
			int longRel = StrictMath.min(personality.researchTreatyLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			int longRelBase = StrictMath.min(base.researchTreatyLongRelationshipMaxBonus, (rel.positiveRelationshipAge - (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 50)) / (WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 4));
			exCmp(longRel, "Long_Relationship", e, longRelBase, personality);
			q += longRel;
		}
		
		// opinion
		int op = personality.bonusOpinion.get(them.bonuses);
		ex(op, "Opinion", e);
		q += op;
		
		// reputation
		if (m.toggles().contains(ConquestToggle.REPUTATION) && them.getReputation() < 25) {
			int rep = (int) ((them.getReputation() - 25) * personality.avoidResearchTreatyFromVeryLowRepMultiplier);
			int repBase = (int) ((them.getReputation() - 25) * base.avoidResearchTreatyFromVeryLowRepMultiplier);
			exCmp(rep, "Reputation", e, repBase, personality);
			q += rep;
		}
		
		if (!me.playerControlled && !them.playerControlled) {
			ex(m.aiCooperationBonus(), "Cooperation", e);
			q += m.aiCooperationBonus();
		}
		
		if (q > personality.maxResearchTreatyQuality) {
			maximum(personality.maxResearchTreatyQuality, e);
			q = personality.maxResearchTreatyQuality;
		}
		
		total(q, e);
		
		researchTreatyQualities.put(them, q);
		
		return q;
	}
	
	public int giveMoneyQuality(int money, Empire me, Empire them, HasRelationships m, StringBuilder e) {
		double ratio = (-money * 1.0 / StrictMath.max(40, m.incomeForComparison(me)));
		int q = StrictMath.min(-1, (int) (ratio * personality.giveMoneyFactor));
		int qBase = StrictMath.min(1, (int) (ratio * base.giveMoneyFactor));
		exCmpAlways(q, "Send_x_Money_short", e, qBase, personality, money);
		return q;
	}
	
	public int receiveMoneyQuality(int money, Empire me, Empire them, HasRelationships m, StringBuilder e) {
		double ratio = (money * 1.0 / StrictMath.max(40, m.incomeForComparison(me)));
		int q = (int) (ratio * personality.receiveMoneyFactor);
		int qBase = (int) (ratio * base.receiveMoneyFactor);
		q = StrictMath.min(q, personality.maxReceiveMoneyBonus);
		qBase = StrictMath.min(qBase, base.maxReceiveMoneyBonus);
		exCmpAlways(q, "Send_x_Money_short", e, qBase, personality, money);
		return q;
	}

	public int giveSubmissionQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		int q = personality.giveSubmissionBaseline;
		exCmp(q, "Baseline", e, base.giveSubmissionBaseline, personality);
		q += repLossQuality(EmpireStat.SUBMISSION_REP_LOSS.get(me.bonuses), me, them, m, e);
		total(q, e);
		return q;
	}

	public int receiveSubmissionQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		int q = personality.receiveSubmissionBaseline;
		exCmp(q, "Baseline", e, base.receiveSubmissionBaseline, personality);
		q += repGainQuality(EmpireStat.SUBMISSION_REP_GAIN.get(me.bonuses), me, them, m, e);
		total(q, e);
		return q;
	}
	
	public int becomeAnnexedQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		int q = personality.becomeAnnexedBase;
		exCmp(q, "Baseline", e, base.becomeAnnexedBase, personality);
		
		int amt = them.cities.size() * personality.becomeAnnexedPerSize;
		exCmp(amt, "Empire_Size", e, them.cities.size() * base.becomeAnnexedPerSize, personality);
		q += amt;
		
		Relationship rel = m.getRelationship(me, them);
		
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			int grievances = rel.getGrievances(me);
			amt = grievances * personality.becomeAnnexedPerGrievance;
			exCmp(amt, "Grievances", e, grievances * base.becomeAnnexedPerGrievance, personality);
			q += amt;
		
			int rep = them.getReputation() - 50;
			amt = (int) (rep * personality.becomeAnnexedPerRep);
			exCmp(amt, "Reputation", e, (int) (rep * base.becomeAnnexedPerRep), personality);
			q += amt;
			
			int sharedGrievances = 0;
			ArrayList<Relationship> rels = m.getRelationships(me);
			for (int i = 0; i < rels.size(); i++) {
				Relationship meAndOther = rels.get(i);
				if (meAndOther.other(me) == them) { continue; }
				Relationship themAndOther = m.getRelationship(them, meAndOther.other(me));
				sharedGrievances += StrictMath.min(meAndOther.getGrievances(me), themAndOther.getGrievances(them));
			}
			amt = sharedGrievances * personality.becomeAnnexedSharedGrievances;
			exCmp(amt, "shared_grievances", e, sharedGrievances * base.becomeAnnexedSharedGrievances, personality);
			q += amt;
		}
		
		if (rel.level == ALLIANCE) {
			exCmp(personality.becomeAnnexedAlliance, "relationship_ALLIANCE", e, base.becomeAnnexedAlliance, personality);
			q += personality.becomeAnnexedAlliance;
		} else if (rel.level == DEFENSIVE_PACT) {
			exCmp(personality.becomeAnnexedDefensivePact, "relationship_DEFENSIVE_PACT", e, base.becomeAnnexedDefensivePact, personality);
			q += personality.becomeAnnexedDefensivePact;
		}
		
		if (!m.isAdjacent(me, them)) {
			exCmp(personality.becomeAnnexedNotAdjacent, "Not_Adjacent", e, base.becomeAnnexedNotAdjacent, personality);
			q += personality.becomeAnnexedNotAdjacent;
		}
		
		total(q, e);
		return q;
	}

	public int doAnnexQuality(Empire me, Empire them, HasRelationships m, StringBuilder e) {
		int q = personality.doAnnexBase;
		exCmp(q, "Baseline", e, base.doAnnexBase, personality);
		
		Relationship rel = m.getRelationship(me, them);
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			int grievances = rel.getGrievances(me);
			int amt = grievances * personality.doAnnexPerGrievance;
			exCmp(amt, "Grievances", e, grievances * base.doAnnexPerGrievance, personality);
			q += amt;
		}
		
		total(q, e);
		return q;
	}
	
	HashMap<City, Integer> giveCityQualities = new HashMap<City, Integer>();

	public int giveCityQuality(City city, Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && giveCityQualities.containsKey(city)) { return giveCityQualities.get(city); }
		int q = city.isTown ? personality.townSendBaseline : personality.citySendBaseline;
		int baseBaseline = city.isTown ? base.townSendBaseline : base.citySendBaseline;
		exCmp(q, "Baseline", e, baseBaseline, personality);
		if (!city.isTown) {
			int numCities = 0;
			for (int i = 0; i < me.cities.size(); i++) {
				if (!me.cities.get(i).isTown) {
					numCities++;
				}
			}
			if (numCities == 1) {
				exCmp(-personality.cityTransferOnlyCityMalus, "Giving_Away_Only_City", e, -base.cityTransferOnlyCityMalus, personality);
				q -= personality.cityTransferOnlyCityMalus;
			}
		}
		int income = m.income(city);
		int incomeBase = (int) (income * base.cityTransferIncomeMultiplier);
		income = (int) (income * personality.cityTransferIncomeMultiplier);
		exCmp(-income, "Income_Lost_Transferred_City", e, -incomeBase, personality);
		q -= income;
		
		if (m.hasRitualSite(city)) {
			ex(-personality.cityTransferRitualSiteBonus, "Desires_Ritual_Site", e);
			q -= personality.cityTransferRitualSiteBonus;
		}
		
		ArrayList<CityUpgradeType> desires = me.desiredUpgrades(m);
		for (CityUpgradeType cut : city.upgrades) {
			if (desires.contains(cut)) {
				ex(-personality.cityTransferDesiredBonus, "desires_x", e, cut.getName());
				q -= personality.cityTransferDesiredBonus;
			} else if (cut.special) {
				rawEx(-personality.cityTransferSpecialBonus, cut.getName(), e);
				q -= personality.cityTransferSpecialBonus;
			}
		}
		
		if (!city.isTown && m.isCoronating(me)) {
			ex(-personality.cityTransferWhenCoronatingMalus, "Coronation", e);
			q -= personality.cityTransferWhenCoronatingMalus;
		} else if (!city.isTown && m.hasEnoughCitiesForCoronation(me)) {
			exCmp(-personality.cityTransferEnoughCitiesForCoronationMalus, "Has_Enough_Cities_For_Coronation", e, -base.cityTransferEnoughCitiesForCoronationMalus, personality);
			q -= personality.cityTransferEnoughCitiesForCoronationMalus;
		}
		
		if (city.originalEmpire == me) {
			exCmp(-personality.cityTransferBelongsToSenderMalus, "Giving_Away_Original_Territory", e, -base.cityTransferBelongsToSenderMalus, personality);
			q -= personality.cityTransferBelongsToSenderMalus;
		}
		
		if (city.originalEmpire == them) {
			exCmp(personality.cityTransferBelongsToReceiverBonus, "Restoring_Original_Territory", e, base.cityTransferBelongsToReceiverBonus, personality);
			q += personality.cityTransferBelongsToReceiverBonus;
		}
		
		if (!m.isConnectedToCapital(me, city)) {
			exCmp(personality.cityTransferDisconnectedFromCapitalBonus, "Giving_Away_Disconnected_From_Capital", e, base.cityTransferDisconnectedFromCapitalBonus, personality);
			q += personality.cityTransferDisconnectedFromCapitalBonus;
		}
				
		int empSize = 0;
		for (int i = 0; i < me.cities.size(); i++) {
			empSize += me.cities.get(i).isTown ? 1 : 3;
		}

		double mult = StrictMath.max(0.5, 1.25 - empSize * 0.05);
		if (empSize < 5) {
			mult *= 2; // Really don't give away the crown jewels.
		}
		mult = StrictMath.ceil(mult * 10) / 10.0;
		exMult(mult, "Empire_Size", e);
		q *= mult;
		
		total(q, e);
		
		giveCityQualities.put(city, q);
		
		return q;
	}
	
	HashMap<City, Integer> receiveCityQualities = new HashMap<City, Integer>();

	public int receiveCityQuality(City city, Empire me, Empire them, HasRelationships m, StringBuilder e) {
		if (e == null && receiveCityQualities.containsKey(city)) { return receiveCityQualities.get(city); }
		int q = city.isTown ? personality.townReceiveBaseline : personality.cityReceiveBaseline;
		int baseBaseline = city.isTown ? base.townReceiveBaseline : base.cityReceiveBaseline;
		exCmp(q, "Baseline", e, baseBaseline, personality);
		
		int income = m.income(city);
		int incomeBase = (int) (income * base.cityTransferIncomeMultiplier);
		income = (int) (income * personality.cityTransferIncomeMultiplier);
		exCmp(income, "Income_Gained_Transferred_City", e, incomeBase, personality);
		q += income;
		
		if (m.hasRitualSite(city)) {
			ex(personality.cityTransferRitualSiteBonus, "Desires_Ritual_Site", e);
			q += personality.cityTransferRitualSiteBonus;
		}
		
		ArrayList<CityUpgradeType> desires = me.desiredUpgrades(m);
		for (CityUpgradeType cut : city.upgrades) {
			if (desires.contains(cut)) {
				ex(personality.cityTransferDesiredBonus, "desires_x", e, cut.getName());
				q += personality.cityTransferDesiredBonus;
			} else if (cut.special) {
				rawEx(personality.cityTransferSpecialBonus, cut.getName(), e);
				q += personality.cityTransferSpecialBonus;
			}
		}
		
		if (city.originalEmpire == me) {
			exCmp(personality.cityTransferBelongsToReceiverBonus, "Restoring_Original_Territory", e, base.cityTransferBelongsToReceiverBonus, personality);
			q += personality.cityTransferBelongsToReceiverBonus;
		}
		
		if (!m.isConnectedToCapital(me, city)) {
			exCmp(-personality.cityTransferDisconnectedFromCapitalMalus, "Receiving_Disconnected_From_Capital", e, -base.cityTransferDisconnectedFromCapitalMalus, personality);
			q += -personality.cityTransferDisconnectedFromCapitalMalus;
		}
		
		int empSize = 0;
		for (int i = 0; i < me.cities.size(); i++) {
			empSize += me.cities.get(i).isTown ? 1 : 3;
		}

		double mult = StrictMath.max(0.25, 1.25 - empSize * 0.05);
		mult = StrictMath.ceil(mult * 10) / 10.0;
		exMult(mult, "Empire_Size", e);
		q *= mult;
		
		total(q, e);
		
		receiveCityQualities.put(city, q);
		
		return q;
	}
	
	public int compromiseQuality(int myQuality, int theirQuality, int myStrength, int theirStrength, StringBuilder e) {
		int q = 0;
		
		int otherSideIsHappy = (int) StrictMath.floor(theirQuality * personality.compromiseOtherSideIsHappyFactor);
		ex(otherSideIsHappy, "Both_Sides_Benefit", e);
		q += otherSideIsHappy;
		
		// use ceil to round values of less magnitude than -1, like 0.7, to 0, to avoid spamming minor modifiers
		int unfairnessToMe = (int) StrictMath.ceil(StrictMath.min(0, myQuality - theirQuality) * personality.compromiseUnfairToMeFactor);
		ex(unfairnessToMe, "Unfair_To_Me", e);
		q += unfairnessToMe;
		
		int unfairnessToThem = (int) StrictMath.ceil(StrictMath.min(0, theirQuality - myQuality) * personality.compromiseUnfairToThemFactor);
		ex(unfairnessToThem, "Unfair_To_Other_Side", e);
		q += unfairnessToThem;
		
		int expectedQualityForMeBasedOnPowerRatio = (myStrength * (myQuality + theirQuality)) / (myStrength + theirStrength + 1);
		int iAmStronger = (int) StrictMath.ceil(StrictMath.min(0, myQuality - expectedQualityForMeBasedOnPowerRatio) * personality.compromiseRelativeStrengthFactor);
		ex(iAmStronger, "Relative_Strength", e);
		q += iAmStronger;
		
		total(q, e);
		return q;
	}
	
	public static int betweenAIsCompromiseQuality(int myQuality, int theirQuality, int myStrength, int theirStrength) {
		int expectedQualityForMeBasedOnPowerRatio = (myStrength * (myQuality + theirQuality)) / (myStrength + theirStrength + 1);
		return 100 - StrictMath.abs(myQuality - expectedQualityForMeBasedOnPowerRatio);
	}
	
	public int enforceThreatProbability(Ultimatum ult, Empire me, Empire them, HasRelationships m, StringBuilder sb) {
		int threatQuality = evaluateOffer(ult.getOrElse(), me, them, m, true, sb);
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			int repLoss = EmpireStat.FAILED_FALSE_ULTIMATUM_COST.get(me.bonuses);
			if (m.isCoronating(me) && me.getReputation() - repLoss < EmpireStat.CORONATION_REPUTATION.get(me.bonuses)) {
				ex(personality.repLossWouldCancelCoronationPenalty, "Reputation_Loss_From_Backing_Down_Would_Cancel_Coronation", sb);
				threatQuality += personality.repLossWouldCancelCoronationPenalty;
			} else {
				double sensitivity = personality.reputationLossSensitivity;
				if (isDoingBadly(me, m)) {
					sensitivity = personality.doingBadlyReputationLossSensitivity;
				}
				if (m.isAboutToConquerAll(me)) {
					sensitivity = personality.aboutToConquerReputationLossSensitivity;
				}
				if (m.isCoronating(me)) {
					sensitivity = personality.aboutToCoronateReputationLossSensitivity;
				}
				int repCost = (int) (repLoss * sensitivity);
				ex(repCost, "Reputation_Loss_From_Backing_Down", sb);
				threatQuality += repCost;
				int desireToHurt = (int) (-m.getRelationship(me, them).getGrievances(me) * personality.grievancesToForcedActionMultiplier);
				threatQuality += desireToHurt;
				ex(desireToHurt, "Diplomatic_Grievances", sb);
			}
		}
		int enforceThreatProbability = (int) (threatQuality * personality.enforceThreatQualityToPercentageMultiplier);
		if (sb != null) { sb.append("\n").append(_t("enforce_threat_probability")).append(": ").append(enforceThreatProbability).append("%"); }
		ex(personality.enforceUltimatumBaseline, "Baseline", sb);
		enforceThreatProbability += personality.enforceUltimatumBaseline;
		return enforceThreatProbability;
	}
	
	public int insultQuality(Empire me, Empire them, Relationship rel, HasRelationships m, StringBuilder sb) {
		int q = personality.sendInsultBaseline;
		ex(personality.sendInsultBaseline, "Baseline", sb);
		if (rel.getGrievances(me) > 0) {
			q += 4;
			ex(4, "Our Grievances", sb);
		}
		if (!m.isAdjacent(me, them)) {
			q -= 4;
			ex(-4, "Not Adjacent", sb);
		}
		int war = StrictMath.max(0, relativeDeclareWarStrength(me, them, m) / 5);
		q += war;
		ex(war, "Our Strength", sb);
		if (them.getReputation() >= 75) {
			q += 4;
			ex(4, "Their High Rep", sb);
		}
		if (me.getReputation() < 20) {
			q += 3;
			ex(3, "Our Very Low Rep", sb);
		}
		if (hasDislikedAgreement(me, them, rel, m)) {
			q += 5;
			ex(5, "We dislike an agreement", sb);
		}
		if (hasDislikedAgreement(them, me, rel, m)) {
			q -= 4;
			ex(-4, "They dislike an agreement", sb);
		}
		for (Relationship r2 : m.getRelationships(me)) {
			if (r2.level == WAR) {
				q -= 3;
				ex(-3, "We are at war", sb);
			}
		}
		ex(q, "Total", sb);
		return q;
	}

	public int delegationQuality(Empire me, Empire them, Relationship rel, HasRelationships m, StringBuilder sb) {
		int q = personality.sendDelegationBaseline;
		ex(personality.sendDelegationBaseline, "Baseline", sb);
		if (rel.getGrievances(them) > 0) {
			q += 2;
			ex(2, "Grievances", sb);
		}
		if (!m.isAdjacent(me, them)) {
			q -= 4;
			ex(-4, "Not Adjacent", sb);
		}
		int war = StrictMath.max(0, relativeDeclareWarStrength(them, me, m) / 5);
		q += war;
		ex(war, "Their Strength", sb);
		if (rel.level.ordinal() > PEACE.ordinal() && rel.levelAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 30) {
			q += 5;
			ex(5, "Long Positive Relationship", sb);
		}
		if (me.getReputation() > them.getReputation()) {
			q += 2;
			ex(2, "My Higher Rep", sb);
		}
		if (them.getReputation() < 45) {
			if (them.getReputation() < 20) {
				q -= 3;
				ex(-3, "Their Very Low Rep", sb);
			} else {
				q += 2;
				ex(2, "Their Lowish Rep", sb);
			}
		}
		if (m.isCoronating(me)) {
			q -= 15;
			ex(-15, "Their Coronation", sb);
		}
		ex(q, "Total", sb);
		return q;
	}
	
	public int insultBackProbability(Empire me, Empire them, HasRelationships m) {
		Relationship rel = m.getRelationship(me, them);
		int p = personality.insultBackBaseline;
		if (m.isCoronating(me)) {
			p += personality.aboutToCoronateInsultBackBonus;
		}
		if (hasDislikedAgreement(me, them, rel, m) && rel.getGrievances(me) == 0) {
			p -= personality.allowInsultToGetWantedGrievancesBonus;
		}
		if (hasDislikedAgreement(them, me, rel, m) && rel.getGrievances(them) == 0) {
			p -= personality.allowInsultToPreventUnwantedGrievancesBonus;
		}
		return p;
	}
	
	public int acceptDelegationProbability(Empire me, Empire them, HasRelationships m) {
		Relationship rel = m.getRelationship(me, them);
		int p = personality.acceptDelegationBaseline;
		if (rel.getGrievances(me) > 0) {
			p = personality.acceptDelegationWithGrievancesBaseline;
			if (m.isCoronating(them)) {
				p -= personality.rejectDelegationToPreventCoronationPenalty;
			}
		} else if (rel.getGrievances(them) > 0) {
			if (m.isCoronating(them)) {
				p -= personality.rejectDelegationToPreventCoronationPenalty * 2;
			}
		}
		if (m.isCoronating(me)) {
			p += personality.acceptDelegationDuringCoronationBonus;
		}
		if (rel.level.ordinal() > PEACE.ordinal() && rel.levelAge > WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 30) {
			p += personality.acceptDelegationInPositiveRelationshipBonus;
		}
		return p;
	}
	
	private boolean hasDislikedAgreement(Empire me, Empire them, Relationship rel, HasRelationships m) {
		switch (rel.level) {
			case NON_AGGRESSION_PACT:
				if (nonAggressionPactQuality(me, them, m, null) < 0) { return true; }
				break;
			case DEFENSIVE_PACT:
				if (defensivePactQuality(me, them, m, null) < 0) { return true; }
				break;
			case ALLIANCE:
				if (allianceQuality(me, them, m, null) < 0) { return true; }
				break;
		}
		
		if (rel.getSendingTribute(me)) { return true; }
		if (rel.researchTreaty && researchTreatyQuality(me, them, m, null) < 0) { return true; }
		if (rel.tradeTreaty && tradeTreatyQuality(me, them, m, null) < 0) { return true; }
		
		return false;
	}
	
	// defensive pact
	// relative strength: similarity
	// adjacency
	// shared wars
	// other wars
	// threatening neighbours
	// rep
	// other treaties
	// long non-aggression
	
	// alliance
	// relative strength: similarity or they're stronger?
	// potential ally about to win
	// we are about to win and don't want to share
	// adjacency
	// shared wars
	// other wars I'm involved in
	// wars they're involved in
	// rep
	// other treaties
	// long defensive pact
	
	// give tribute
	// flat cost
	
	// receive tribute
	// amount gained relative to own income
	
	// trade treaty
	// amount gained relative to own income
	
	// research treaty
	// relative research output
	// number of boosted techs
}
