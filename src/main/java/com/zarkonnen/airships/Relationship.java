package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import java.util.EnumSet;
import java.util.Random;
import org.json.JSONArray;

public class Relationship {
	public static final int ULTIMATUM_TIME = 60000;
	public static final int CHALLENGE_TIME = 60000;
	public static final int AGE_MAX = WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 100;
	public final Empire a, b;
	public Level level = Level.PEACE;
	public int levelAge = 0;
	public int positiveRelationshipAge = 0;
	public boolean researchTreaty;
	public boolean tradeTreaty;
	public Boolean hadSubmissionAToB;
	private Offer negotiation;
	private boolean negotiationOfferedFromAToB;
	private Ultimatum ultimatum;
	public Challenge challenge;
	public Direction tribute = Direction.NEITHER;
	private int aGrievancesTowardsB;
	private int aLikesB, bLikesA;
	public Empire defensivePactWarMainDefender;
	public ArrayList<String> previousIncidents = new ArrayList<String>();
	
	public EnumSet<Level> alreadyHadHeroEventFromUpgrade = EnumSet.noneOf(Level.class);
	public EnumSet<Level> alreadyHadHeroEventFromDowngrade = EnumSet.noneOf(Level.class);
	public boolean alreadyHadHeroEventFromTradeTreaty = false;
	public boolean alreadyHadHeroEventFromTradeTreatyEnded = false;
	public boolean alreadyHadHeroEventFromResearchTreaty = false;
	public boolean alreadyHadHeroEventFromResearchTreatyEnded = false;
	public boolean alreadyHadHeroEventFromAToBTribute = false;
	public boolean alreadyHadHeroEventFromAToBTributeEnded = false;
	public boolean alreadyHadHeroEventFromBToATribute = false;
	public boolean alreadyHadHeroEventFromBToATributeEnded = false;
	//public boolean alreadyHadHeroEventFromAToBSubmission = false; // Not needed, submission is once only anyway.
	//public boolean alreadyHadHeroEventFromBToASubmission = false;
	
	public Relationship cloneRelationshipOnly() {
		Relationship rel = new Relationship(a, b);
		rel.level = level;
		rel.levelAge = levelAge;
		rel.positiveRelationshipAge = positiveRelationshipAge;
		rel.researchTreaty = researchTreaty;
		rel.tradeTreaty = tradeTreaty;
		rel.tribute = tribute;
		rel.aGrievancesTowardsB = aGrievancesTowardsB;
		rel.aLikesB = aLikesB;
		rel.bLikesA = bLikesA;
		return rel;
	}
	
	@Override
	public String toString() {
		return "Rel " + a.getName() + " / " + b.getName();
	}
	
	public boolean contains(Empire e) {
		return e == a || e == b;
	}
	
	public void clean(HasRelationships m) {
		negotiation = getOffer(m);
		ultimatum = getUltimatum(m);
	}
	
	public void setToEternalWar() {
		level = Level.WAR;
		positiveRelationshipAge = 0;
		levelAge = AGE_MAX;
		researchTreaty = false;
		tradeTreaty = false;
		negotiation = null;
		tribute = Direction.NEITHER;
		aGrievancesTowardsB = 0;
		hadSubmissionAToB = null;
	}
	
	public void makeOffer(Offer negotiation, Empire offerer, HasRelationships m) {
		if (!isValid(negotiation, null, m, false)) { return; }
		if (getUltimatum(m) != null) { return; }
		if (a == offerer) {
			this.negotiation = negotiation.clone();
			negotiationOfferedFromAToB = true;
			return;
		}
		if (b == offerer) {
			this.negotiation = negotiation.clone();
			negotiationOfferedFromAToB = false;
			return;
		}
		throw new RuntimeException("Asking about Empire " + offerer.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public void rejectOffer(Empire rejecter) {
		if (negotiation == null) { return; }
		if (!contains(rejecter)) {
			throw new RuntimeException("Asking about Empire " + rejecter.getName() + " in relationship between " + a.getName() + " and " + b.getName());
		}
		Empire sender = negotiationOfferedFromAToB ? a : b;
		if (sender != rejecter) {
			sender.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.OFFER_REJECTED, other(sender), negotiation, null));
		}
		clearOffer();
	}
	
	public Offer getOffer(HasRelationships m) {
		if (negotiation == null) { return null; }
		Offer n2 = negotiation.clone();
		n2.clean();
		return isValid(n2, null, m, false) ? n2 : null;
	}
	
	private void clearOffer() {
		negotiation = null;
	}
	
	public boolean isOffering(Empire offerer, HasRelationships m) {
		return getOffer(m) != null && ((offerer == a && negotiationOfferedFromAToB) || (offerer == b && !negotiationOfferedFromAToB));
	}
	
	public boolean isOffered(Empire receiver, HasRelationships m) {
		return getOffer(m) != null && ((receiver == a && !negotiationOfferedFromAToB) || (receiver == b && negotiationOfferedFromAToB));
	}
	
	public Empire other(Empire one) {
		if (a == one) {
			return b;
		}
		if (b == one) {
			return a;
		}
		throw new RuntimeException("Asking about Empire " + one.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public boolean canAnnex(Empire annexer) {
		return level.ordinal() >= Level.PEACE.ordinal() && other(annexer).cities.size() == 1 && other(annexer).cities.get(0).isTown;
	}
	
	public boolean isValid(Offer offer, Empire forcer, HasRelationships m, boolean isDemand) {
		return getInvalidReason(offer, forcer, m, isDemand) == null;
	}
	
	public String getInvalidReason(Offer offer, Empire forcer, HasRelationships m, boolean isDemand) {
		// Can't unilaterally declare peace.
		// Can't bilaterally dissolve alliances with more than 2 members.
		
		// Check nulls
		// Can only get to truce from war.
		// Cannot agree to dissolve a multi-alliance.
		
		if (offer.rel != this) {
			throw new RuntimeException("Checking validity of misparented offer.");
		}
		
		if (forcer != null) {
			// Forcer needs to actually be one of the people in the relationship.
			if (forcer != a && forcer != b) {
				return "cannot_force_agreements_on_other_empires";
			}
			
			// You can always declare war.
			if (offer.newLevel == Level.WAR) {
				if (!forcer.playerControlled && inPlayerAlliance(forcer, m)) {
					return "non_human_players_cannot_declare_alliance_wars";
				}
				// But it should fully break off everything.
				return offer.newResearchTreaty != null && offer.newTradeTreaty != null && offer.newTribute != null && !offer.newResearchTreaty && !offer.newTradeTreaty && offer.newTribute == Direction.NEITHER
						? null
						: "incomplete_declaration_of_war";
			}
			
			// You can't make someone love you.
			if (offer.newLevel != null && offer.newLevel.ordinal() > level.ordinal()) {
				return "cannot_forcibly_improve_relationship";
			}
			
			if (offer.annexationAToB != null) {
				return "cannot_forcibly_improve_relationship";
			}
			
			// You can't force a truce.
			if (offer.newLevel == Level.TRUCE) {
				return "cannot_force_truce";
			}
			
			// You can't force tribute or treaties.
			if (offer.newTribute != null && offer.newTribute != tribute && offer.newTribute != Direction.NEITHER) {
				return "cannot_force_tribute";
			}
			
			if (!tradeTreaty && offer.newTradeTreaty != null && offer.newTradeTreaty) { return "cannot_force_treaty"; }
			if (!researchTreaty && offer.newResearchTreaty != null && offer.newResearchTreaty) { return "cannot_force_treaty"; }
			
			if (offer.submissionAToB != null) {
				return "cannot_force_submission";
			}
			
			if (offer.moneyTransferAToB != 0) {
				return "cannot_force_payment";
			}
			
			if (!offer.cityTransfers.isEmpty()) {
				return "cannot_force_city_transfer";
			}
			
			return null;
		}
		
		// You can't agree to a war, that would be weird.
		if (offer.newLevel == Level.WAR) { return "cannot_agree_to_a_war"; }
		
		// You have to declare a truce to get out of war, and that's all you can do.
		if (level == Level.WAR && offer.newLevel != Level.TRUCE) {
			return "currently_at_war_must_declare_truce";
		}
		// You can only do a truce from a war.
		if (level != Level.WAR && level != Level.TRUCE && offer.newLevel != null && offer.newLevel == Level.TRUCE) {
			return "not_at_war_cannot_declare_truce";
		}
		// You cannot do anything else while you have a truce.
		if (level == Level.TRUCE && offer.newLevel != null && offer.newLevel != Level.TRUCE) {
			return "must_wait_for_truce_to_mature_before_improving_relationship";
		}
		if ((level == Level.WAR || level == Level.TRUCE) && ((offer.newTradeTreaty != null && offer.newTradeTreaty) || (offer.newResearchTreaty != null && offer.newResearchTreaty))) {
			return "must_wait_for_truce_to_mature_before_improving_relationship";
		}
		
		if (level == Level.ALLIANCE && offer.newLevel != null && offer.newLevel != Level.ALLIANCE) {
			// You can't agree to dissolve an alliance with more than 2 members.
			int allianceSize = 1;
			ArrayList<Relationship> aRels = m.getRelationships(a);
			for (int i = 0; i < aRels.size(); i++) {
				Relationship aRel = aRels.get(i);
				if (aRel.level == Level.ALLIANCE) {
					allianceSize++;
				}
			}
			if (allianceSize > 2) { return "cannot_agree_to_dissolve_alliance_with_more_than_two_members"; }
		}
		
		if (offer.newLevel == Level.TRUCE && defensivePactWarMainDefender != null && (m.getRelationship(a, defensivePactWarMainDefender).level == Level.DEFENSIVE_PACT || m.getRelationship(b, defensivePactWarMainDefender).level == Level.DEFENSIVE_PACT)) {
			return "cannot_agree_to_defensive_war_truce";
		}
		
		if (offer.newTradeTreaty != null && offer.newTradeTreaty && (!m.isAdjacent(a, b) || !m.isAdjacent(b, a))) {
			return "must_be_adjacent_for_trade_treaty";
		}
		
		if (offer.newResearchTreaty != null && offer.newResearchTreaty && (!m.isAdjacent(a, b) || !m.isAdjacent(b, a))) {
			return "must_be_adjacent_for_research_treaty";
		}
		
		if (offer.newLevel == Level.ALLIANCE && ((!offer.rel.a.playerControlled && inPlayerAlliance(offer.rel.a, m)) || (!offer.rel.b.playerControlled && inPlayerAlliance(offer.rel.b, m)))) {
			return "must_negotiate_alliance_between_human_players";
		}
		
		if (offer.moneyTransferAToB > 0 && offer.rel.a.getMoney() < offer.moneyTransferAToB) {
			return "cannot_afford_payment";
		}
		
		if (offer.moneyTransferAToB < 0 && offer.rel.b.getMoney() < -offer.moneyTransferAToB) {
			return "cannot_afford_payment";
		}
		
		for (CityTransfer ct : offer.cityTransfers) {
			if ((ct.aToB ? a : b).cities.size() < 2) {
				return "cannot_give_away_last_city";
			}
			if (!(ct.aToB ? a : b).cities.contains(ct.city)) {
				return "cannot_give_away_unowned_city";
			}
		}
		
		if (offer.submissionAToB != null && hadSubmissionAToB != null && offer.submissionAToB.equals(hadSubmissionAToB)) {
			return "cannot_submit_twice";
		}
		
		if (offer.submissionAToB != null && offer.submissionAToB && offer.rel.a.getReputation() < EmpireStat.SUBMISSION_REP_LOSS.get(offer.rel.a.bonuses)) {
			return "not_enough_reputation_to_submit";
		}
		
		if (offer.submissionAToB != null && !offer.submissionAToB && offer.rel.b.getReputation() < EmpireStat.SUBMISSION_REP_LOSS.get(offer.rel.b.bonuses)) {
			return "not_enough_reputation_to_submit";
		}
		
		if (isDemand && offer.moneyTransferAToB != 0) {
			return "cannot_demand_payment";
		}
		
		if (offer.annexationAToB != null) {
			if (level.ordinal() < Level.PEACE.ordinal()) {
				return "must_be_at_peace_to_annex";
			}
			if (offer.annexationAToB && (a.cities.size() != 1 || !a.cities.get(0).isTown)) {
				return "cannot_annex_nontiny_empires";
			}
			if (!offer.annexationAToB && (b.cities.size() != 1 || !b.cities.get(0).isTown)) {
				return "cannot_annex_nontiny_empires";
			}
			Empire annexee = offer.annexationAToB ? a : b;
			Empire annexer = offer.annexationAToB ? b : a;
			ArrayList<Relationship> annexeeRels = m.getRelationships(annexee);
			for (int i = 0; i < annexeeRels.size(); i++) {
				Relationship arel = annexeeRels.get(i);
				if (arel.other(annexee) == annexer) { continue; }
				if (arel.level == Level.WAR && m.getRelationship(annexer, arel.other(annexee)).level != Level.WAR) {
					return "cannot_annex_towns_at_war";
				}
			}
			if (!(offer.newLevel == null && offer.newTribute == null && offer.newTradeTreaty == null && offer.newResearchTreaty == null && offer.moneyTransferAToB == 0 && offer.submissionAToB == null && offer.cityTransfers.isEmpty())) {
				return "cannot_have_other_terms_for_annexing";
			}
		}
		
		// That's all that really has to be the case right now.
		return null;
	}
	
	public static boolean inPlayerAlliance(Empire e, HasRelationships m) {
		for (Relationship rel : m.getRelationships(e)) {
			if (rel.level == Level.ALLIANCE && rel.other(e).playerControlled) { return true; }
		}
		return false;
	}
	
	public void makeUltimatum(Ultimatum ult, HasRelationships m) {
		if (m.toggles().contains(ConquestToggle.REPUTATION) && ult.forcer.getReputation() < EmpireStat.MIN_ULTIMATUM_REPUTATION.get(ult.forcer.bonuses)) {
			return;
		}
		if (getUltimatum(m) != null && ultimatum.forcer != ult.forcer) {
			return; // Can't reply to an ultimatum with one of your own.
		}
		ultimatum = ult;
		negotiation = null;
	}
	
	public Ultimatum getUltimatum(HasRelationships m) {
		return ultimatum != null && ultimatum.isValid(m) ? ultimatum : null;
	}
	
	public String getUltimatumInvalidReason(HasRelationships m) {
		return ultimatum == null ? "no_ultimatum_present" : ultimatum.getInvalidReason(m);
	}
	
	public void cancelUltimatum(HasRelationships m) {
		if (ultimatum == null) { return; }
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			ultimatum.forcer.changeReputation(-EmpireStat.CANCELLED_ULTIMATUM_COST.get(ultimatum.forcer.bonuses), m);
		}
		other(ultimatum.forcer).ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.CANCELLED, ultimatum, null));
		ultimatum = null;
	}
	
	public void accedeToUltimatum(HasRelationships m) {
		if (getUltimatum(m) == null) { return; }
		doAgree(ultimatum.getDemand(), m);
		ultimatum.forcer.ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.ACCEDED, ultimatum, null));
		ultimatum = null;
	}
	
	public void defyUltimatum(HasRelationships m) {
		if (getUltimatum(m) == null) { return; }
		ultimatum.defied = true;
		ultimatum.timeLeft = ULTIMATUM_TIME;
	}
	
	public void enforceUltimatum(HasRelationships m) {
		if (getUltimatum(m) == null) { return; }
		doForce(ultimatum.orElse, ultimatum.forcer, m, true);
		other(ultimatum.forcer).ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.ENFORCED, ultimatum, null));
		ultimatum = null;
	}
	
	public void dontEnforceUltimatum(HasRelationships m) {
		if (getUltimatum(m) == null) { return; }
		int repCost = EmpireStat.FAILED_FALSE_ULTIMATUM_COST.get(ultimatum.forcer.bonuses);
		if (m.toggles().contains(ConquestToggle.REPUTATION)) {
			ultimatum.forcer.changeReputation(-repCost, m);
		}
		a.ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.NOT_ENFORCED, ultimatum, null));
		b.ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.NOT_ENFORCED, ultimatum, null));
		ultimatum = null;
	}
	
	public void agree(Offer offer, HasRelationships m) {
		String invalid = getInvalidReason(offer, null, m, false);
		if (invalid != null) {
			throw new RuntimeException(invalid);
		}
		doAgree(offer, m);
		Empire sender = negotiationOfferedFromAToB ? a : b;
		sender.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.OFFER_ACCEPTED, other(sender), offer, null));
		clearOffer();
	}
	
	public void doAgree(Offer offer, HasRelationships m) {
		ArrayList<Offer> cascades = cascadeOfferEffects(m, offer, null);
		for (Offer c : cascades) {
			if (c.newLevel == Level.ALLIANCE) {
				// OK, we look at the cascades. If one of the people in it is allied to one of the people in the original offer, we send them a notice.
				if (a != c.rel.a && m.getRelationship(a, c.rel.a).level == Level.ALLIANCE) {
					c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_MADE_NEW_ALLY, a, c, m.getRelationship(a, c.rel.b)));
				}
				if (a != c.rel.b && m.getRelationship(a, c.rel.b).level == Level.ALLIANCE) {
					c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_MADE_NEW_ALLY, a, c, m.getRelationship(a, c.rel.a)));
				}
				if (b != c.rel.a && m.getRelationship(b, c.rel.a).level == Level.ALLIANCE) {
					c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_MADE_NEW_ALLY, b, c, m.getRelationship(b, c.rel.b)));
				}
				if (b != c.rel.b && m.getRelationship(b, c.rel.b).level == Level.ALLIANCE) {
					c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_MADE_NEW_ALLY, b, c, m.getRelationship(b, c.rel.a)));
				}
			} else if (c.newLevel == Level.WAR) {
				// If we've been dragged into a war due to a cascade caused by an ally, we also want to be told.
				if (a != c.rel.a && b != c.rel.a) {
					if (m.getRelationship(c.rel.a, a).level == Level.ALLIANCE) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR, c.rel.b, offer, m.getRelationship(a, c.rel.b)));
					} else if (m.getRelationship(c.rel.a, b).level == Level.ALLIANCE) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR, c.rel.b, offer, m.getRelationship(b, c.rel.b)));
					}
				}
				if (a != c.rel.b && b != c.rel.b) {
					if (m.getRelationship(c.rel.b, a).level == Level.ALLIANCE) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR, c.rel.a, offer, m.getRelationship(a, c.rel.a)));
					} else if (m.getRelationship(c.rel.b, b).level == Level.ALLIANCE) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR, c.rel.a, offer, m.getRelationship(b, c.rel.a)));
					}
				}
				// If the alliance caused the new allies to declare war, we want to know.
				if (offer.newLevel == Level.ALLIANCE) {
					if (b == c.rel.a && m.getRelationship(c.rel.b, a).level == Level.WAR) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ENEMY_ALLIANCE_CAUSED_WAR, a, offer, this));
					} else if (b == c.rel.b && m.getRelationship(c.rel.a, a).level == Level.WAR) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ENEMY_ALLIANCE_CAUSED_WAR, a, offer, this));
					} else if (a == c.rel.a && m.getRelationship(c.rel.b, b).level == Level.WAR) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ENEMY_ALLIANCE_CAUSED_WAR, b, offer, this));
					} else if (a == c.rel.b && m.getRelationship(c.rel.a, b).level == Level.WAR) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ENEMY_ALLIANCE_CAUSED_WAR, b, offer, this));
					}
				}
			} else if (c.newLevel == Level.TRUCE && offer.newLevel == Level.TRUCE) {
				if (c.rel.defensivePactWarMainDefender == a) {
					if (a != c.rel.a && m.getRelationship(a, c.rel.a).level == Level.DEFENSIVE_PACT) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.PACT_SIBLING_MADE_TRUCE, a, offer, m.getRelationship(a, c.rel.b)));
					} else if (a != c.rel.b && m.getRelationship(a, c.rel.b).level == Level.DEFENSIVE_PACT) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.PACT_SIBLING_MADE_TRUCE, a, offer, m.getRelationship(a, c.rel.a)));
					}
				} else if (c.rel.defensivePactWarMainDefender == b) {
					if (b != c.rel.a && m.getRelationship(b, c.rel.a).level == Level.DEFENSIVE_PACT) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.PACT_SIBLING_MADE_TRUCE, b, offer, m.getRelationship(b, c.rel.b)));
					} else if (b != c.rel.b && m.getRelationship(b, c.rel.b).level == Level.DEFENSIVE_PACT) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.PACT_SIBLING_MADE_TRUCE, b, offer, m.getRelationship(b, c.rel.a)));
					}
				} else {
					if (b != c.rel.a && m.getRelationship(b, c.rel.a).level == Level.ALLIANCE) {
						c.rel.a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_MADE_TRUCE, b, offer, m.getRelationship(b, c.rel.b)));
					} else if (b != c.rel.b && m.getRelationship(b, c.rel.b).level == Level.ALLIANCE) {
						c.rel.b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_MADE_TRUCE, b, offer, m.getRelationship(b, c.rel.a)));
					}
				}
			}
		}
		applyIndividualOffer(offer, m);
		for (Offer c : cascades) {
			c.rel.applyIndividualOffer(c, m);
		}
	}
	
	private void applyIndividualOffer(Offer offer, HasRelationships m) {
		if (offer.rel != this) {
			throw new RuntimeException("Attempting to apply an offer for " + offer.rel + " to " + this + ".");
		}
		if (offer.newLevel != null && level != offer.newLevel) {
			if (m instanceof WorldMap) {
				if (offer.newLevel.ordinal() > level.ordinal() && !alreadyHadHeroEventFromUpgrade.contains(offer.newLevel)) {
					alreadyHadHeroEventFromUpgrade.add(offer.newLevel);
					Hero.processHeroEvent(HeroEvent.relationshipLevelUpgrade(a, b, offer.newLevel), (WorldMap) m);
					Hero.processHeroEvent(HeroEvent.relationshipLevelUpgrade(b, a, offer.newLevel), (WorldMap) m);
				}
				if (offer.newLevel.ordinal() < level.ordinal() && !alreadyHadHeroEventFromDowngrade.contains(offer.newLevel)) {
					alreadyHadHeroEventFromDowngrade.add(offer.newLevel);
					Hero.processHeroEvent(HeroEvent.relationshipLevelDowngrade(a, b, offer.newLevel), (WorldMap) m);
					Hero.processHeroEvent(HeroEvent.relationshipLevelDowngrade(b, a, offer.newLevel), (WorldMap) m);
				}
			}
			levelAge = 0;
			level = offer.newLevel;
			if (level.ordinal() < Level.NON_AGGRESSION_PACT.ordinal()) {
				positiveRelationshipAge = 0;
			}
			defensivePactWarMainDefender = offer.defensivePactWarMainDefender;
			if (offer.newLevel == Level.WAR) {
				if (a.diplomacyAI != null) {
					a.diplomacyAI.warDeclared(a, b);
				}
				if (b.diplomacyAI != null) {
					b.diplomacyAI.warDeclared(b, a);
				}
			}
		}
		if (offer.newResearchTreaty != null) {
			researchTreaty = offer.newResearchTreaty;
			if (m instanceof WorldMap) {
				if (offer.newResearchTreaty) {
					if (!alreadyHadHeroEventFromResearchTreaty) {
						alreadyHadHeroEventFromResearchTreaty = true;
						Hero.processHeroEvent(HeroEvent.researchTreaty(a, b), (WorldMap) m);
						Hero.processHeroEvent(HeroEvent.researchTreaty(b, a), (WorldMap) m);
					}
				} else {
					if (!alreadyHadHeroEventFromResearchTreatyEnded) {
						alreadyHadHeroEventFromResearchTreatyEnded = true;
						Hero.processHeroEvent(HeroEvent.researchTreatyEnded(a, b), (WorldMap) m);
						Hero.processHeroEvent(HeroEvent.researchTreatyEnded(b, a), (WorldMap) m);
					}
				}
			}
		}
		if (offer.newTradeTreaty != null) {
			tradeTreaty = offer.newTradeTreaty;
			if (m instanceof WorldMap) {
				if (offer.newTradeTreaty) {
					if (!alreadyHadHeroEventFromTradeTreaty) {
						alreadyHadHeroEventFromTradeTreaty = true;
						Hero.processHeroEvent(HeroEvent.tradeTreaty(a, b), (WorldMap) m);
						Hero.processHeroEvent(HeroEvent.tradeTreaty(b, a), (WorldMap) m);
					}
				} else {
					if (!alreadyHadHeroEventFromTradeTreatyEnded) {
						alreadyHadHeroEventFromTradeTreatyEnded = true;
						Hero.processHeroEvent(HeroEvent.tradeTreatyEnded(a, b), (WorldMap) m);
						Hero.processHeroEvent(HeroEvent.tradeTreatyEnded(b, a), (WorldMap) m);
					}
				}
			}
		}
		if (offer.newTribute != null)  {
			if (m instanceof WorldMap) {
				switch (offer.newTribute) {
					case A_TO_B:
						if (!alreadyHadHeroEventFromAToBTribute) {
							alreadyHadHeroEventFromAToBTribute = true;
							Hero.processHeroEvent(HeroEvent.sendTribute(a, b), (WorldMap) m);
							Hero.processHeroEvent(HeroEvent.receiveTribute(b, a), (WorldMap) m);
						}
						break;
					case B_TO_A:
						if (!alreadyHadHeroEventFromBToATribute) {
							alreadyHadHeroEventFromBToATribute = true;
							Hero.processHeroEvent(HeroEvent.sendTribute(b, a), (WorldMap) m);
							Hero.processHeroEvent(HeroEvent.receiveTribute(a, b), (WorldMap) m);
						}
						break;
					case NEITHER:
						switch (tribute) {
							case A_TO_B:
								if (!alreadyHadHeroEventFromAToBTributeEnded) {
									alreadyHadHeroEventFromAToBTributeEnded = true;
									Hero.processHeroEvent(HeroEvent.sendTributeEnded(a, b), (WorldMap) m);
									Hero.processHeroEvent(HeroEvent.receiveTributeEnded(b, a), (WorldMap) m);
								}
								break;
							case B_TO_A:
								if (!alreadyHadHeroEventFromBToATributeEnded) {
									alreadyHadHeroEventFromBToATributeEnded = true;
									Hero.processHeroEvent(HeroEvent.sendTributeEnded(b, a), (WorldMap) m);
									Hero.processHeroEvent(HeroEvent.receiveTributeEnded(a, b), (WorldMap) m);
								}
								break;
						}
						break;
				}
			}
			tribute = offer.newTribute;
		}
		a.setMoney(a.getMoney() - offer.moneyTransferAToB);
		b.setMoney(b.getMoney() + offer.moneyTransferAToB);
		if (offer.submissionAToB != null) {
			if (offer.submissionAToB) {
				a.changeReputation(-EmpireStat.SUBMISSION_REP_LOSS.get(a.bonuses), m);
				b.changeReputation(EmpireStat.SUBMISSION_REP_GAIN.get(b.bonuses), m);
				if (m instanceof WorldMap) {
					//if (!alreadyHadHeroEventFromAToBSubmission) {
					//	alreadyHadHeroEventFromAToBSubmission = true;
					Hero.processHeroEvent(HeroEvent.demonstrateSubmission(a, b), (WorldMap) m);
					Hero.processHeroEvent(HeroEvent.receiveSubmission(b, a), (WorldMap) m);
					//}
				}
			} else {
				b.changeReputation(-EmpireStat.SUBMISSION_REP_LOSS.get(b.bonuses), m);
				a.changeReputation(EmpireStat.SUBMISSION_REP_GAIN.get(a.bonuses), m);
				if (m instanceof WorldMap) {
					//if (!alreadyHadHeroEventFromBToASubmission) {
					//	alreadyHadHeroEventFromBToASubmission = true;
					Hero.processHeroEvent(HeroEvent.demonstrateSubmission(b, a), (WorldMap) m);
					Hero.processHeroEvent(HeroEvent.receiveSubmission(a, b), (WorldMap) m);
					//}
				}
			}
			hadSubmissionAToB = offer.submissionAToB;
		}
		for (CityTransfer ct : offer.cityTransfers) {
			Empire from = ct.aToB ? a : b;
			Empire to = ct.aToB ? b : a;
			//System.out.println("Transferring " + ct.city.name + " from " + from.getName() + " to " + to.getName());
			if (from.cities.contains(ct.city) && from.cities.size() > 1) {
				m.clearHeroFrom(ct.city);
				from.cities.remove(ct.city);
				from.hasLostTerritory = true;
				to.cities.add(ct.city);
				if (m instanceof WorldMap) {
					((WorldMap) m).isAdjacents.clear();
					((WorldMap) m).clearPathCaches();
				}
				Spy spy = to.getSpyFor(ct.city);
				if (spy != null) {
					to.spies.remove(spy);
				}
			}/* else {
				System.out.println("transfer failed, contains: " + (from.cities.contains(ct.city) + ", numCities " + from.cities.size()));
			}*/
		}
		if (!offer.cityTransfers.isEmpty() || offer.newLevel != null) {
			m.clearPathCaches();
		}
		if (offer.annexationAToB != null) {
			Empire annexee = offer.annexationAToB ? a : b;
			Empire annexer = offer.annexationAToB ? b : a;
			annexer.cities.addAll(annexee.cities);
			annexee.cities.clear();
			annexer.setMoney(annexer.getMoney() + annexee.getMoney());
			annexer.getFleets().addAll(annexee.getFleets());
			if (m instanceof WorldMap) {
				WorldMap wm = (WorldMap) m;
				for (Hero h : Hero.getHeroes(annexee, wm)) {
					h.inEmpire = annexer;
				}
				wm.removeEmpire(annexee);
				m.clearPathCaches();
			}
		}
	}
	
	public int individualForceRepCostLevel(Offer offer, Empire forcer) {
		BonusSet bonuses = bonuses();
		if (offer.newLevel != null && offer.newLevel.ordinal() < level.ordinal()) {
			int repCost = Math.max(0, EmpireStat.LEVEL_BREAK_COST.get(level).get(forcer.bonuses) - EmpireStat.LEVEL_BREAK_COST.get(offer.newLevel).get(forcer.bonuses));
			if (levelAge < EmpireStat.RECENT_LEVEL_CHANGE_TIMEOUT.get(bonuses)) {
				repCost *= 2;
			}
			return repCost;
		}
		return 0;
	}
	
	public String individualForceRepCostLevelText(Offer offer, Empire forcer) {
		BonusSet bonuses = bonuses();
		if (offer.newLevel != null && offer.newLevel.ordinal() < level.ordinal()) {
			int repCost = Math.max(0, EmpireStat.LEVEL_BREAK_COST.get(level).get(forcer.bonuses) - EmpireStat.LEVEL_BREAK_COST.get(offer.newLevel).get(forcer.bonuses));
			if (levelAge < EmpireStat.RECENT_LEVEL_CHANGE_TIMEOUT.get(bonuses)) {
				return "2x" + repCost;
			}
			return "" + repCost;
		}
		return "0";
	}
	
	public String individualForceRepCostLevelTooltip(Offer offer, Empire forcer) {
		BonusSet bonuses = bonuses();
		if (offer.newLevel != null && offer.newLevel.ordinal() < level.ordinal()) {
			int repCost = Math.max(0, EmpireStat.LEVEL_BREAK_COST.get(level).get(forcer.bonuses) - EmpireStat.LEVEL_BREAK_COST.get(offer.newLevel).get(forcer.bonuses));
			if (repCost > 0 && levelAge < EmpireStat.RECENT_LEVEL_CHANGE_TIMEOUT.get(bonuses)) {
				return _t("recent_treaty_cost_tooltip");
			}
			return null;
		}
		return null;
	}
	
	public int individualForceRepCostResearch(Offer offer, Empire forcer) {
		if (offer.newResearchTreaty != null && researchTreaty && !offer.newResearchTreaty) {
			return EmpireStat.RESEARCH_TREATY_BREAK_COST.get(forcer.bonuses);
		}
		return 0;
	}
	
	public int individualForceRepCostTrade(Offer offer, Empire forcer) {
		if (offer.newTradeTreaty != null && tradeTreaty && !offer.newTradeTreaty) {
			return EmpireStat.TRADE_TREATY_BREAK_COST.get(forcer.bonuses);
		}
		return 0;
	}
	
	public int individualForceRepCostTribute(Offer offer, Empire forcer) {
		if (offer.newTribute == Direction.NEITHER) {
			if (getSendingTribute(forcer)) {
				return EmpireStat.TRIBUTE_BREAK_COST.get(forcer.bonuses);
			}
			if (getReceivingTribute(forcer)) {
				return EmpireStat.TRIBUTE_CANCEL_COST.get(forcer.bonuses);
			}
		}
		return 0;
	}
	
	public int cascadedForceRepCost(Offer offer, Empire forcer, HasRelationships m) {
		ArrayList<Offer> cascades = cascadeOfferEffects(m, offer, forcer);
		int repCost = individualForceRepCost(offer, forcer);
		for (int i = 0; i < cascades.size(); i++) {
			Offer c = cascades.get(i);
			if (c.rel.contains(forcer)) {
				repCost += individualForceRepCost(c, forcer);
			}
		}
		return repCost;
	}
	
	public int individualForceRepCost(Offer offer, Empire forcer) {
		return individualForceRepCostLevel(offer, forcer) + individualForceRepCostResearch(offer, forcer) + individualForceRepCostTrade(offer, forcer) + individualForceRepCostTribute(offer, forcer);
	}
	
	public boolean force(Offer offer, Empire forcer, HasRelationships m, boolean applyRepAndGrievances) {
		String invalid = getInvalidReason(offer, forcer, m, false);
		if (invalid != null) {
			return false;
			//throw new RuntimeException(invalid);
		}
		doForce(offer, forcer, m, applyRepAndGrievances);
		if (getUltimatum(m) == null) {
			ultimatum = null; // Clear no longer valid ultimatum.
		}
		return true;
	}
	
	private void doForce(Offer offer, Empire forcer, HasRelationships m, boolean applyRepAndGrievances) {
		if (offer.newLevel == Level.WAR) {
			for (Relationship rel : m.getRelationships(forcer)) {
				if (rel.level == Level.ALLIANCE) {
					rel.other(forcer).diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.ALLY_DECLARED_WAR, forcer, offer, offer.rel));
				}
			}
		}
		if (offer.rel.level == Level.ALLIANCE && offer.newLevel != null && offer.newLevel.ordinal() < Level.ALLIANCE.ordinal()) {
			forcer.allianceBreakings++;
		}
		ArrayList<Offer> cascades = cascadeOfferEffects(m, offer, forcer);
		String info = null;
		int score = 0;
		if (!forcer.playerControlled && forcer.diplomacyAI != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(_t("x_evaluation", forcer.getName()));
			if (!researchTreaty && !tradeTreaty && tribute == Direction.NEITHER && offer.newLevel == Level.WAR) {
				// Simple war declaration
				score = forcer.diplomacyAI.warQuality(forcer, other(forcer), m, sb, true, true);
			} else {
				score = forcer.diplomacyAI.evaluateOffer(offer, forcer, other(forcer), m, true, sb);
				sb.append("\n").append(_t("quality_total")).append(score);
				if (offer.newLevel == Level.WAR) {
					sb.append("\n\n").append(_t("war_details_"));
					forcer.diplomacyAI.warQuality(forcer, other(forcer), m, sb, true, true);
				}
			}
			sb.append("\n\n").append(_t("war_eval_help"));
			info = sb.toString();
		}
		forceIndividual(offer, forcer, m, info, score, null, applyRepAndGrievances);
		for (int i = 0; i < cascades.size(); i++) {
			Offer c = cascades.get(i);
			if (c.rel.a == forcer || c.rel.b == forcer) {
				c.rel.forceIndividual(c, forcer, m, info, score, other(forcer), applyRepAndGrievances);
			} else {
				c.rel.applyIndividualOffer(c, m);
			}
		}
	}
	
	private void forceIndividual(Offer offer, Empire forcer, HasRelationships m, String forcedInfo, int forcedScore, Empire originalTarget, boolean applyRepAndGrievances) {
		if (offer.newLevel != null && offer.newLevel == Level.WAR) {
			other(forcer).diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.WAR_DECLARED, forcer, offer, cloneRelationshipOnly(), forcedInfo, forcedScore, originalTarget));
		} else {
			other(forcer).diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.FORCED_MESSAGE, forcer, offer, cloneRelationshipOnly(), forcedInfo, forcedScore, null));
		}
		int repCost = applyRepAndGrievances && m.toggles().contains(ConquestToggle.REPUTATION) ? individualForceRepCost(offer, forcer) : 0;
		applyIndividualOffer(offer, m);
		int absorbedByGrievances = Math.min(repCost, getGrievances(forcer));
		if (absorbedByGrievances > 0) {
			setGrievances(forcer, getGrievances(forcer) - absorbedByGrievances);
		}
		repCost -= absorbedByGrievances;
		forcer.changeReputation(-repCost, m);
		if (repCost > 0) {
			Empire victim = other(forcer);
			setGrievances(victim, getGrievances(victim) + repCost);
		}
	}
	
	private BonusSet bonuses() {
		BonusSet bs = new BonusSet();
		bs.addAll(a.bonuses);
		bs.addAll(b.bonuses);
		return bs;
	}
	
	public void tick(int ms, HasRelationships m) {
		if (defensivePactWarMainDefender != null && !m.exists(defensivePactWarMainDefender)) {
			defensivePactWarMainDefender = null;
		}
		levelAge = Math.min(AGE_MAX, levelAge += ms);
		if (level.ordinal() >= Level.NON_AGGRESSION_PACT.ordinal()) {
			positiveRelationshipAge = Math.min(AGE_MAX, positiveRelationshipAge + ms);
		}
		if (level == Level.TRUCE && levelAge >= EmpireStat.TRUCE_TIME.get(bonuses())) {
			level = Level.PEACE;
			levelAge = 0;
			a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.CEASEFIRE_EXPIRED, b, null, null));
			b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.CEASEFIRE_EXPIRED, a, null, null));
			if (m instanceof WorldMap) {
				if (!alreadyHadHeroEventFromUpgrade.contains(Level.PEACE)) {
					alreadyHadHeroEventFromUpgrade.add(Level.PEACE);
					Hero.processHeroEvent(HeroEvent.relationshipLevelUpgrade(a, b, Level.PEACE), (WorldMap) m);
					Hero.processHeroEvent(HeroEvent.relationshipLevelUpgrade(b, a, Level.PEACE), (WorldMap) m);
				}
			}
		}
		if (tradeTreaty && (!m.isAdjacent(a, b) || !m.isAdjacent(b, a))) {
			tradeTreaty = false;
			a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.TRADE_TREATY_INVALID, b, null, null));
			b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.TRADE_TREATY_INVALID, a, null, null));
		}
		if (researchTreaty && (!m.isAdjacent(a, b) || !m.isAdjacent(b, a))) {
			researchTreaty = false;
			a.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.RESEARCH_TREATY_INVALID, b, null, null));
			b.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.RESEARCH_TREATY_INVALID, a, null, null));
		}
		if (ultimatum != null) {
			String invalidReason = ultimatum.getInvalidReason(m);
			if (invalidReason != null) {
				ultimatum.getRelationship().a.ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.INVALID, ultimatum, invalidReason));
				ultimatum.getRelationship().b.ultimatumNotices.add(new UltimatumNotice(UltimatumNotice.Type.INVALID, ultimatum, invalidReason));
				ultimatum = null;
			} else {
				ultimatum.timeLeft -= ms;
				if (ultimatum.timeLeft <= 0) {
					if (ultimatum.defied) {
						dontEnforceUltimatum(m);
					} else {
						defyUltimatum(m);
					}
				}
			}
		}
		if (challenge != null) {
			if (level == Level.WAR) {
				challenge = null;
			} else {
				challenge.timeLeft -= ms;
				if (challenge.timeLeft <= 0) {
					noChallengeResponse(m, challenge.type == Challenge.Type.DELEGATION ? "DELEGATION_IGNORED" : null, null);
				}
			}
		}
	}
	
	public Key key() {
		return new Key(a, b);
	}
	
	public boolean hasGrievances() {
		return aGrievancesTowardsB != 0;
	}
	
	public boolean hasTribute() {
		return tribute != Direction.NEITHER;
	}
	
	public boolean getSendingTribute(Empire e) {
		if (e == a) {
			return tribute == Direction.A_TO_B;
		}
		if (e == b) {
			return tribute == Direction.B_TO_A;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public boolean getReceivingTribute(Empire e) {
		if (e == a) {
			return tribute == Direction.B_TO_A;
		}
		if (e == b) {
			return tribute == Direction.A_TO_B;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public void setSendingTribute(Empire e, boolean t) {
		if (e == a) {
			tribute = t ? Direction.A_TO_B : Direction.NEITHER;
			return;
		}
		if (e == b) {
			tribute = t ? Direction.B_TO_A : Direction.NEITHER;
			return;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public int getGrievances(Empire e) {
		if (e == a) {
			return Math.max(0, aGrievancesTowardsB);
		}
		if (e == b) {
			return Math.max(0, -aGrievancesTowardsB);
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public void changeGrievances(Empire e, int g) {
		if (e == a) {
			aGrievancesTowardsB += g;
			return;
		}
		if (e == b) {
			aGrievancesTowardsB -= g;
			return;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public void setGrievances(Empire e, int g) {
		if (e == a) {
			aGrievancesTowardsB = g;
			return;
		}
		if (e == b) {
			aGrievancesTowardsB = -g;
			return;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public int getLikes(Empire e) {
		if (e == a) {
			return aLikesB;
		}
		if (e == b) {
			return bLikesA;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public void setLikes(Empire e, int l) {
		if (e == a) {
			aLikesB = l;
			return;
		}
		if (e == b) {
			bLikesA = l;
			return;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}
	
	public boolean isOfferingNegotiation(Empire e) {
		if (e == a) {
			return negotiationOfferedFromAToB;
		}
		if (e == b) {
			return !negotiationOfferedFromAToB;
		}
		throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship between " + a.getName() + " and " + b.getName());
	}

	public Relationship(Empire a, Empire b) {
		if (a.id < b.id) {
			this.a = a;
			this.b = b;
		} else {
			this.a = b;
			this.b = a;
		}
		
		// qqDPS
		//if (AGame.ANIM_R.nextBoolean()) { return; }
		/*aGrievancesTowardsB = AGame.ANIM_R.nextInt(20) - 10;
		
		level = Relationship.Level.ALLIANCE;
		tradeTreaty = true;
		researchTreaty = true;
		tribute = Direction.values()[AGame.ANIM_R.nextInt(3)];
				
		/*level = Relationship.Level.values()[AGame.ANIM_R.nextInt(Relationship.Level.values().length)];
		if (level == Level.WAR || level == Level.TRUCE || AGame.ANIM_R.nextBoolean()) { return; }
		tribute = Relationship.Direction.values()[AGame.ANIM_R.nextInt(Relationship.Direction.values().length)];
		if (AGame.ANIM_R.nextBoolean()) { return; }
		tradeTreaty = AGame.ANIM_R.nextBoolean();
		if (AGame.ANIM_R.nextBoolean()) { return; }
		researchTreaty = AGame.ANIM_R.nextBoolean();
		
		if (AGame.ANIM_R.nextBoolean()) {
			negotiation = new Offer(this);
			negotiation.newLevel = Relationship.Level.values()[AGame.ANIM_R.nextInt(Relationship.Level.values().length)];
			negotiation.newTribute = Relationship.Direction.values()[AGame.ANIM_R.nextInt(Relationship.Direction.values().length)];
			negotiation.newTradeTreaty = AGame.ANIM_R.nextBoolean();
			negotiation.newResearchTreaty = AGame.ANIM_R.nextBoolean();
			negotiationOfferedFromAToB = AGame.ANIM_R.nextBoolean();
		}*/
		/*negotiation = new Offer(this);
		negotiation.newLevel = Relationship.Level.NON_AGGRESSION_PACT;
		negotiation.newTribute = AGame.ANIM_R.nextBoolean() ? (AGame.ANIM_R.nextBoolean() ? Direction.A_TO_B : Direction.B_TO_A) : null;
		negotiation.newResearchTreaty = AGame.ANIM_R.nextBoolean();
		negotiationOfferedFromAToB = AGame.ANIM_R.nextBoolean();*/
		/*Offer demand = new Offer(this);
		demand.newResearchTreaty = true;
		Offer threat = new Offer(this);
		threat.newTradeTreaty = false;*/
		//ultimatum = new Ultimatum(AGame.ANIM_R.nextBoolean() ? a : b, demand, threat, AGame.ANIM_R.nextInt(30000));
	}
	
	public JSONObject toJSON() {
		JSONObject o = new JSONObject()
				.put("a", a.id)
				.put("b", b.id)
				.put("level", level.name())
				.put("levelAge", levelAge)
				.put("positiveRelationshipAge", positiveRelationshipAge)
				.put("researchTreaty", researchTreaty)
				.put("tradeTreaty", tradeTreaty)
				.put("tribute", tribute.name())
				.put("aGrievancesTowardsB", aGrievancesTowardsB)
				.put("aLikesB", aLikesB)
				.put("bLikesA", bLikesA);
		if (negotiation != null) {
			o.put("negotiation", negotiation.toJSON());
			o.put("negotiationOfferedFromAToB", negotiationOfferedFromAToB);
		}
		if (ultimatum != null) {
			o.put("ultimatum", ultimatum.toJSON());
		}
		if (challenge != null) {
			o.put("challenge", challenge.toJSON());
		}
		if (defensivePactWarMainDefender != null) {
			o.put("defensivePactWarMainDefender", defensivePactWarMainDefender.id);
		}
		if (hadSubmissionAToB != null) {
			o.put("hadSubmissionAToB", hadSubmissionAToB.booleanValue());
		}
		JSONArray a = new JSONArray();
		for (String pi : previousIncidents) {
			a.put(pi);
		}
		o.put("previousIncidents", a);
		a = new JSONArray();
		for (Level l : alreadyHadHeroEventFromUpgrade) {
			a.put(l.name());
		}
		o.put("alreadyHadHeroEventFromUpgrade", a);
		a = new JSONArray();
		for (Level l : alreadyHadHeroEventFromDowngrade) {
			a.put(l.name());
		}
		o.put("alreadyHadHeroEventFromDowngrade", a);
		
		o.put("alreadyHadHeroEventFromTradeTreaty", alreadyHadHeroEventFromTradeTreaty);
		o.put("alreadyHadHeroEventFromTradeTreatyEnded", alreadyHadHeroEventFromTradeTreatyEnded);
		o.put("alreadyHadHeroEventFromResearchTreaty", alreadyHadHeroEventFromResearchTreaty);
		o.put("alreadyHadHeroEventFromResearchTreatyEnded", alreadyHadHeroEventFromResearchTreatyEnded);
		o.put("alreadyHadHeroEventFromAToBTribute", alreadyHadHeroEventFromAToBTribute);
		o.put("alreadyHadHeroEventFromAToBTributeEnded", alreadyHadHeroEventFromAToBTributeEnded);
		o.put("alreadyHadHeroEventFromBToATribute", alreadyHadHeroEventFromBToATribute);
		o.put("alreadyHadHeroEventFromBToATributeEnded", alreadyHadHeroEventFromBToATributeEnded);
		//o.put("alreadyHadHeroEventFromAToBSubmission", alreadyHadHeroEventFromAToBSubmission);
		//o.put("alreadyHadHeroEventFromBToASubmission", alreadyHadHeroEventFromBToASubmission);
		
		return o;
	}
	
	public Relationship(JSONObject o, WorldMap m) {
		a = m.getEmpire(o.getInt("a"));
		b = m.getEmpire(o.getInt("b"));
		level = Level.valueOf(o.getString("level"));
		levelAge = o.optInt("levelAge", 0);
		researchTreaty = o.getBoolean("researchTreaty");
		tradeTreaty = o.getBoolean("tradeTreaty");
		tribute = Direction.valueOf(o.getString("tribute"));
		aGrievancesTowardsB = o.getInt("aGrievancesTowardsB");
		aLikesB = o.getInt("aLikesB");
		bLikesA = o.getInt("bLikesA");
		if (o.has("negotiation")) {
			negotiation = new Offer(o.getJSONObject("negotiation"), this, m);
			negotiationOfferedFromAToB = o.getBoolean("negotiationOfferedFromAToB");
		}
		if (o.has("ultimatum")) {
			ultimatum = new Ultimatum(o.getJSONObject("ultimatum"), m, this);
		}
		if (o.has("challenge")) {
			challenge = new Challenge(o.getJSONObject("challenge"));
		}
		if (o.has("defensivePactWarMainDefender")) {
			defensivePactWarMainDefender = m.getEmpire(o.getInt("defensivePactWarMainDefender"));
		}
		if (o.has("hadSubmissionAToB")) {
			hadSubmissionAToB = o.getBoolean("hadSubmissionAToB");
		}
		if (o.has("previousIncidents")) {
			JSONArray pis = o.getJSONArray("previousIncidents");
			for (int i = 0; i < pis.length(); i++) {
				previousIncidents.add(pis.getString(i));
			}
		}
		if (o.has("alreadyHadHeroEventFromUpgrade")) {
			JSONArray a = o.getJSONArray("alreadyHadHeroEventFromUpgrade");
			for (int i = 0; i < a.length(); i++) {
				alreadyHadHeroEventFromUpgrade.add(Level.valueOf(a.getString(i)));
			}
		}
		if (o.has("alreadyHadHeroEventFromDowngrade")) {
			JSONArray a = o.getJSONArray("alreadyHadHeroEventFromDowngrade");
			for (int i = 0; i < a.length(); i++) {
				alreadyHadHeroEventFromDowngrade.add(Level.valueOf(a.getString(i)));
			}
		}
		
		alreadyHadHeroEventFromTradeTreaty = o.optBoolean("alreadyHadHeroEventFromTradeTreaty", false);
		alreadyHadHeroEventFromTradeTreatyEnded = o.optBoolean("alreadyHadHeroEventFromTradeTreatyEnded", false);
		alreadyHadHeroEventFromResearchTreaty = o.optBoolean("alreadyHadHeroEventFromResearchTreaty", false);
		alreadyHadHeroEventFromResearchTreatyEnded = o.optBoolean("alreadyHadHeroEventFromResearchTreatyEnded", false);
		alreadyHadHeroEventFromAToBTribute = o.optBoolean("alreadyHadHeroEventFromAToBTribute", false);
		alreadyHadHeroEventFromAToBTributeEnded = o.optBoolean("alreadyHadHeroEventFromAToBTributeEnded", false);
		alreadyHadHeroEventFromBToATribute = o.optBoolean("alreadyHadHeroEventFromBToATribute", false);
		alreadyHadHeroEventFromBToATributeEnded = o.optBoolean("alreadyHadHeroEventFromBToATributeEnded", false);
		//alreadyHadHeroEventFromAToBSubmission = o.optBoolean("alreadyHadHeroEventFromAToBSubmission", false);
		//alreadyHadHeroEventFromBToASubmission = o.optBoolean("alreadyHadHeroEventFromBToASubmission", false);
	}
	
	public static enum Level implements HasName {
		WAR(0, 592, 139, 613),
		TRUCE(206, 592, 201, 613),
		PEACE(185, 592, 180, 613),
		NON_AGGRESSION_PACT(165, 592, 240, 612),
		DEFENSIVE_PACT(71, 592, 221, 613),
		ALLIANCE(20, 590, 159, 611);
		
		public final Img icon;
		public final Img monochromeIcon;
		
		private Level(int icx, int icy, int micx, int micy) {
			this.icon = new Img("ui", icx, icy, 18, 18, false);
			this.monochromeIcon = new Img("ui", micx, micy, 18, 18, false);
		}

		@Override
		public String getName() {
			return _t("a_relationship_" + name());
		}
	}
	
	public static enum Direction {
		NEITHER, A_TO_B, B_TO_A
	}
	
	public static final class Key implements Comparable<Key> {
		public final Empire a, b;

		public Key(Empire a, Empire b) {
			if (a.id < b.id) {
				this.a = a;
				this.b = b;
			} else {
				this.a = b;
				this.b = a;
			}
		}
		
		@Override
		public int hashCode() {
			return a.id + b.id;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Key)) { return false; }
			Key k = (Key) o;
			return (a == k.a && b == k.b) || (a == k.b && b == k.a);
		}

		@Override
		public int compareTo(Key o) {
			if (a == o.a) {
				return b.id - o.b.id;
			}
			return a.id - o.a.id;
		}
	}
	
	static boolean eq(Boolean a, Boolean b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) { return false; }
		return a.booleanValue() == b.booleanValue();
	}
	
	public static final class CityTransfer {
		public final City city;
		public final boolean aToB;

		public CityTransfer(City city, boolean aToB) {
			this.city = city;
			this.aToB = aToB;
		}
	}
	
	public static int giveMoneyAmount(Empire me, Empire them, HasRelationships m) {
		int amt = StrictMath.min(me.getMoney(), 5 * m.incomeForComparison(me));
		if (amt < 100) {
			return 100;
		} else if (amt < 2000) {
			return (amt / 100) * 100;
		} else {
			return (amt / 1000) * 1000;
		}
	}
	
	public static final class Offer {
		public final Relationship rel;
		public Level newLevel;
		public Boolean newResearchTreaty;
		public Boolean newTradeTreaty;
		public Direction newTribute;
		public int moneyTransferAToB;
		public Boolean submissionAToB;
		public ArrayList<CityTransfer> cityTransfers = new ArrayList<CityTransfer>();
		public Boolean annexationAToB;
		
		public Empire defensivePactWarMainDefender;
		
		public transient boolean announced;
		
		public void clean() {
			if (newLevel == rel.level) {
				newLevel = null;
			} else if (newLevel == Level.WAR) {
				setToWar();
				return;
			}
			if (newTribute == rel.tribute) {
				newTribute = null;
			}
			if (newTradeTreaty != null && newTradeTreaty == rel.tradeTreaty) {
				newTradeTreaty = null;
			}
			if (newResearchTreaty != null && newResearchTreaty == rel.researchTreaty) {
				newResearchTreaty = null;
			}
			if (moneyTransferAToB > 0 && rel.a.getMoney() < moneyTransferAToB) {
				moneyTransferAToB = rel.a.getMoney();
			}
			if (moneyTransferAToB < 0 && rel.b.getMoney() < -moneyTransferAToB) {
				moneyTransferAToB = -rel.b.getMoney();
			}
			for (int i = 0; i < cityTransfers.size(); i++) {
				CityTransfer ct = cityTransfers.get(i);
				if ((ct.aToB && !rel.a.cities.contains(ct.city)) || (!ct.aToB && !rel.b.cities.contains(ct.city))) {
					cityTransfers.remove(i);
					i--;
				}
			}
		}
		
		public boolean isEmpty() {
			return newLevel == null && newTribute == null && newTradeTreaty == null && newResearchTreaty == null && moneyTransferAToB == 0 && submissionAToB == null && cityTransfers.isEmpty() && annexationAToB == null;
		}
		
		@Override
		public Offer clone() {
			Offer o2 = new Offer(rel);
			o2.newLevel = newLevel;
			o2.newResearchTreaty = newResearchTreaty;
			o2.newTradeTreaty = newTradeTreaty;
			o2.newTribute = newTribute;
			o2.moneyTransferAToB = moneyTransferAToB;
			o2.submissionAToB = submissionAToB;
			o2.cityTransfers.addAll(cityTransfers);
			o2.annexationAToB = annexationAToB;
			return o2;
		}
		
		public Offer(Relationship rel) {
			this.rel = rel;
		}
		
		public void setToWar() {
			newLevel = Level.WAR;
			newTribute = Direction.NEITHER;
			newTradeTreaty = false;
			newResearchTreaty = false;
			moneyTransferAToB = 0;
			submissionAToB = null;
			cityTransfers.clear();
			annexationAToB = null;
		}
		
		public JSONObject toJSON() {
			JSONObject o = new JSONObject()
					.put("relA", rel.a.id)
					.put("relB", rel.b.id);
			if (newLevel != null) {
				o.put("level", newLevel.name());
			}
			if (newResearchTreaty != null) {
				o.put("researchTreaty", newResearchTreaty);
			}
			if (newTradeTreaty != null) {
				o.put("tradeTreaty", newTradeTreaty);
			}
			if (newTribute != null) {
				o.put("tribute", newTribute.name());
			}
			if (defensivePactWarMainDefender != null) {
				o.put("defensivePactWarMainDefender", defensivePactWarMainDefender.id);
			}
			o.put("moneyTransferAToB", moneyTransferAToB);
			if (submissionAToB != null) {
				o.put("submissionAToB", submissionAToB.booleanValue());
			}
			if (annexationAToB != null) {
				o.put("annexationAToB", annexationAToB.booleanValue());
			}
			JSONArray a = new JSONArray();
			for (CityTransfer ct : cityTransfers) {
				a.put(ct.city.id);
				a.put(ct.aToB);
			}
			o.put("cityTransfers", a);
			return o;
		}

		public Offer(JSONObject o, WorldMap wm) {
			this(o, wm.getRelationship(wm.getEmpire(o.getInt("relA")), wm.getEmpire(o.getInt("relB"))), wm);
		}
		
		public Offer(JSONObject o, Relationship rel, WorldMap wm) {
			if (o.has("level")) {
				newLevel = Level.valueOf(o.getString("level"));
			}
			if (o.has("researchTreaty"))  {
				newResearchTreaty = o.getBoolean("researchTreaty");
			}
			if (o.has("tradeTreaty")) {
				newTradeTreaty = o.getBoolean("tradeTreaty");
			}
			if (o.has("tribute")) {
				newTribute = Direction.valueOf(o.getString("tribute"));
			}
			if (o.has("defensivePactWarMainDefender")) {
				defensivePactWarMainDefender = wm.getEmpire(o.getInt("defensivePactWarMainDefender"));
			}
			moneyTransferAToB = o.optInt("moneyTransferAToB");
			if (o.has("submissionAToB")) {
				submissionAToB =  Boolean.valueOf(o.getBoolean("submissionAToB"));
			}
			if (o.has("annexationAToB")) {
				annexationAToB = Boolean.valueOf(o.getBoolean("annexationAToB"));
			}
			if (o.has("cityTransfers")) {
				JSONArray a = o.getJSONArray("cityTransfers");
				for (int i = 0; i < a.length(); i += 2) {
					cityTransfers.add(new CityTransfer(wm.getCity(a.getInt(i)), a.getBoolean(i + 1)));
				}
			}
			this.rel = rel;
		}
		
		public boolean getSendingTribute(Empire e) {
			if (e == rel.a) {
				return newTribute == Direction.A_TO_B;
			}
			if (e == rel.b) {
				return newTribute == Direction.B_TO_A;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
	
		public boolean getReceivingTribute(Empire e) {
			if (e == rel.a) {
				return newTribute == Direction.B_TO_A;
			}
			if (e == rel.b) {
				return newTribute == Direction.A_TO_B;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public void setSendingTribute(Empire e, boolean t) {
			if (e == rel.a) {
				newTribute = t ? Direction.A_TO_B : Direction.NEITHER;
				return;
			}
			if (e == rel.b) {
				newTribute = t ? Direction.B_TO_A : Direction.NEITHER;
				return;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public boolean getSubmitting(Empire e) {
			if (e == rel.a) {
				return submissionAToB != null && submissionAToB;
			}
			if (e == rel.b) {
				return submissionAToB != null && !submissionAToB;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public boolean getSubmittedTo(Empire e) {
			if (e == rel.a) {
				return submissionAToB != null && !submissionAToB;
			}
			if (e == rel.b) {
				return submissionAToB != null && submissionAToB;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public void setSubmitting(Empire e) {
			if (e == rel.a) {
				submissionAToB = true;
				return;
			}
			if (e == rel.b) {
				submissionAToB = false;
				return;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public void clearSubmitting() {
			submissionAToB = null;
		}
		
		public boolean getBeingAnnexed(Empire e) {
			if (e == rel.a) {
				return annexationAToB != null && annexationAToB;
			}
			if (e == rel.b) {
				return annexationAToB != null && !annexationAToB;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public boolean getDoingAnnexing(Empire e) {
			if (e == rel.a) {
				return annexationAToB != null && !annexationAToB;
			}
			if (e == rel.b) {
				return annexationAToB != null && annexationAToB;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public void setBeingAnnexed(Empire e) {
			if (e == rel.a) {
				annexationAToB = true;
				return;
			}
			if (e == rel.b) {
				annexationAToB = false;
				return;
			}
			throw new RuntimeException("Asking about Empire " + e.getName() + " in relationship offer between " + rel.a.getName() + " and " + rel.b.getName());
		}
		
		public void clearAnnexation() {
			submissionAToB = null;
		}
		
		public boolean changesSomething() {
			return	(newLevel != null && newLevel != rel.level) ||
					(newTribute != null && newTribute != rel.tribute) ||
					(newTradeTreaty != null && newTradeTreaty != rel.tradeTreaty) ||
					(newResearchTreaty != null && newResearchTreaty != rel.researchTreaty) ||
					moneyTransferAToB != 0 || submissionAToB != null || !cityTransfers.isEmpty() || annexationAToB != null;
		}
		
		public int totalCascadedForceRepCostsAfterGrievances(HasRelationships map, Empire forcer) {
			if (forcer == null) { return 0; }
			int total = Math.max(0, rel.individualForceRepCost(this, forcer) - rel.getGrievances(forcer));
			ArrayList<Offer> casc = cascadeOfferEffects(map, this, forcer);
			for (int i = 0; i < casc.size(); i++) {
				Offer o = casc.get(i);
				if (!o.rel.contains(forcer)) { continue; }
				total += Math.max(0, o.rel.individualForceRepCost(o, forcer) - o.rel.getGrievances(forcer));
			}
			return total;
		}
		
		@Override
		public boolean equals(Object o2) {
			if (o2 == null || !(o2 instanceof Offer)) { return false; }
			Offer o = (Offer) o2;
			boolean sameTradeTreaty = newTradeTreaty == null ? o.newTradeTreaty == null : newTradeTreaty.equals(o.newTradeTreaty);
			boolean sameResearchTreaty = newResearchTreaty == null ? o.newResearchTreaty == null : newResearchTreaty.equals(o.newResearchTreaty);
			return rel == o.rel && newLevel == o.newLevel && newTribute == o.newTribute && sameTradeTreaty && sameResearchTreaty
					&& (submissionAToB == null ? o.submissionAToB == null : submissionAToB.equals(o.submissionAToB)) && moneyTransferAToB == o.moneyTransferAToB
					&& cityTransfers.containsAll(o.cityTransfers) && o.cityTransfers.containsAll(cityTransfers)
					&& (annexationAToB == null ? o.annexationAToB == null : annexationAToB.equals(o.annexationAToB));
		}
		
		@Override
		public int hashCode() {
			int c = 294783;
			if (newLevel != null) {
				c += newLevel.ordinal() + 1;
			}
			c *= 37;
			if (newTribute != null) {
				c += newTribute.ordinal() + 1;
			}
			c *= 37;
			if (newTradeTreaty != null) {
				c += newTradeTreaty ? 1 : 2;
			}
			c *= 37;
			if (newResearchTreaty != null) {
				c += newResearchTreaty ? 1 : 2;
			}
			c *= 37;
			c += moneyTransferAToB;
			c *= 37;
			if (submissionAToB != null) {
				c += submissionAToB ? 1 : 2;
			}
			c *= 37;
			if (annexationAToB != null) {
				c += annexationAToB ? 1 : 2;
			}
			c *= 37;
			for (CityTransfer ct : cityTransfers) {
				c += ct.city.id * 2 + (ct.aToB ? 1 : 0);
				c *= 37;
			}
			return c;
		}

		public boolean hasCityTransfer(City c) {
			for (int i = 0; i < cityTransfers.size(); i++) {
				if (cityTransfers.get(i).city == c) { return true; }
			}
			return false;
		}
		
		public String getHeroStatChangeAppendix(Empire e, WorldMap m, boolean past) {
			if (!rel.contains(e)) { return ""; }
			String text = "";
			if (newLevel != null) {
				if (newLevel.ordinal() > rel.level.ordinal() && !rel.alreadyHadHeroEventFromUpgrade.contains(newLevel)) {
					text = Hero.getStatChangeAppendix(e, HeroEvent.relationshipLevelUpgrade(e, rel.other(e), newLevel), m, past);
				}
				if (newLevel.ordinal() < rel.level.ordinal() && !rel.alreadyHadHeroEventFromDowngrade.contains(newLevel)) {
					text = Hero.getStatChangeAppendix(e, HeroEvent.relationshipLevelDowngrade(e, rel.other(e), newLevel), m, past);
				}
			}
			if (newResearchTreaty != null) {
				if (newResearchTreaty) {
					if (!rel.alreadyHadHeroEventFromResearchTreaty) {
						if (e == rel.a) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.researchTreaty(rel.a, rel.b), m, past);
						}
						if (e == rel.b) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.researchTreaty(rel.b, rel.a), m, past);
						}
					}
				} else {
					if (!rel.alreadyHadHeroEventFromResearchTreatyEnded) {
						if (e == rel.a) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.researchTreatyEnded(rel.a, rel.b), m, past);
						}
						if (e == rel.b) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.researchTreatyEnded(rel.b, rel.a), m, past);
						}
					}
				}
			}
			if (newTradeTreaty != null) {
				if (newTradeTreaty) {
					if (!rel.alreadyHadHeroEventFromTradeTreaty) {
						if (e == rel.a) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.tradeTreaty(rel.a, rel.b), m, past);
						}
						if (e == rel.b) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.tradeTreaty(rel.b, rel.a), m, past);
						}
					}
				} else {
					if (!rel.alreadyHadHeroEventFromTradeTreatyEnded) {
						if (e == rel.a) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.tradeTreatyEnded(rel.a, rel.b), m, past);
						}
						if (e == rel.b) {
							text += Hero.getStatChangeAppendix(e, HeroEvent.tradeTreatyEnded(rel.b, rel.a), m, past);
						}
					}
				}
			}
			if (newTribute != null) {
				switch (newTribute) {
					case A_TO_B:
						if (!rel.alreadyHadHeroEventFromAToBTribute) {
							if (e == rel.a) {
								text += Hero.getStatChangeAppendix(e, HeroEvent.sendTribute(rel.a, rel.b), m, past);
							}
							if (e == rel.b) {
								text += Hero.getStatChangeAppendix(e, HeroEvent.receiveTribute(rel.b, rel.a), m, past);
							}
						}
						break;
					case B_TO_A:
						if (!rel.alreadyHadHeroEventFromBToATribute) {
							if (e == rel.a) {
								text += Hero.getStatChangeAppendix(e, HeroEvent.receiveTribute(rel.a, rel.b), m, past);
							}
							if (e == rel.b) {
								text += Hero.getStatChangeAppendix(e, HeroEvent.sendTribute(rel.b, rel.a), m, past);
							}
						}
						break;
					case NEITHER:
						switch (rel.tribute) {
							case A_TO_B:
								if (!rel.alreadyHadHeroEventFromAToBTributeEnded) {
									if (e == rel.a) {
										text += Hero.getStatChangeAppendix(e, HeroEvent.sendTributeEnded(rel.a, rel.b), m, past);
									}
									if (e == rel.b) {
										text += Hero.getStatChangeAppendix(e, HeroEvent.receiveTributeEnded(rel.b, rel.a), m, past);
									}
								}
								break;
							case B_TO_A:
								if (!rel.alreadyHadHeroEventFromBToATributeEnded) {
									if (e == rel.a) {
										text += Hero.getStatChangeAppendix(e, HeroEvent.receiveTributeEnded(rel.a, rel.b), m, past);
									}
									if (e == rel.b) {
										text += Hero.getStatChangeAppendix(e, HeroEvent.sendTributeEnded(rel.b, rel.a), m, past);
									}
								}
								break;
						}
						break;
				}
			}
			if (getSubmitting(e)) {
				text += Hero.getStatChangeAppendix(e, HeroEvent.demonstrateSubmission(e, rel.other(e)), m, past);
				text += e.getRepChangeHeroAppendix(-EmpireStat.SUBMISSION_REP_LOSS.get(e.bonuses), m, past);
			}
			if (getSubmittedTo(e)) {
				text += Hero.getStatChangeAppendix(e, HeroEvent.receiveSubmission(e, rel.other(e)), m, past);
				text += e.getRepChangeHeroAppendix(EmpireStat.SUBMISSION_REP_GAIN.get(e.bonuses), m, past);
			}
			
			return text;
		}
	}
	
	public static ArrayList<Empire> transitiveDefensiveAlliesAndPactMembers(HasRelationships map, Empire e) {
		ArrayList<Empire> es = new ArrayList<Empire>();
		es.add(e);
		// find all direct allies of e
		ArrayList<Relationship> rels = map.getRelationships(e);
		for (int j = 0; j < rels.size(); j++) {
			Relationship rel = rels.get(j);
			Empire other = rel.other(e);
			if (rel.level == Relationship.Level.ALLIANCE) {
				es.add(other);
			}
		}
		// find all defensive pacts of e and its direct allies
		int esz = es.size();
		for (int i = 0; i < esz; i++) {
			rels = map.getRelationships(es.get(i));
			for (int j = 0; j < rels.size(); j++) {
				Relationship rel = rels.get(j);
				Empire other = rel.other(es.get(i));
				if (rel.level == Relationship.Level.DEFENSIVE_PACT && !es.contains(other)) {
					es.add(other);
				}
			}
		}
		// find all allies of the direct allies and their pact siblings
		esz = es.size();
		for (int i = 0; i < esz; i++) {
			rels = map.getRelationships(es.get(i));
			for (int j = 0; j < rels.size(); j++) {
				Relationship rel = rels.get(j);
				Empire other = rel.other(es.get(i));
				if (rel.level == Relationship.Level.ALLIANCE && !es.contains(other)) {
					es.add(other);
				}
			}
		}
		
		// Finally, remove our own empire as we don't need to return it.
		es.remove(0);
		return es;
	}
	
	public static ArrayList<Offer> cascadeOfferEffects(HasRelationships map, Offer offer, Empire forcer) {
		// NB make sure about the difference between alliances and defensive treaties
		ArrayList<Offer> fx = new ArrayList<Offer>();
		
		// War:
		// Declaring war on someone also declares war on their allies and defensive partners.
		// Declaring war also drags your allies into it.
		// If you declare war on an ally, your mutual allies take their side.

		if (offer.newLevel != null && offer.rel.level != Level.WAR && offer.newLevel == Level.WAR) {
			if (forcer == null) {
				throw new RuntimeException("You can't agree to a war.");
			}
			
			Empire victim = offer.rel.other(forcer);
			
			ArrayList<Empire> attackerAndAllies = new ArrayList<Empire>();
			attackerAndAllies.add(forcer);
			
			ArrayList<Relationship> forcerRels = map.getRelationships(forcer);
			for (int i = 0; i < forcerRels.size(); i++) {
				Relationship rel = forcerRels.get(i);
				if (rel.level == Relationship.Level.ALLIANCE) {
					// This is an ally of the war declarer.
					Empire ally = rel.other(forcer);
					if (ally == victim) { continue; } // If you attack your ally, they will not declare war on themselves.
					// If you attack your ally, your mutual allies will not come to your aid.
					if (map.getRelationship(ally, victim).level == Relationship.Level.ALLIANCE) { continue; }
					// Your non-mutual allies declare war on the victim also.
					Offer warCascade = new Offer(map.getRelationship(ally, victim));
					warCascade.setToWar();
					fx.add(warCascade);
					attackerAndAllies.add(ally);
				}
			}
			
			ArrayList<Empire> victimAllies = transitiveDefensiveAlliesAndPactMembers(map, victim);
			for (int i = 0; i < victimAllies.size(); i++) {
				Empire ally = victimAllies.get(i);
				if (ally == forcer) { continue; } // Don't declare war on yourself for attacking your ally.
				if (attackerAndAllies.contains(ally)) { continue; } // Alliances trump defensive pacts.
				// All other allies of the victim, even when they're also allies of the attacker, declare war on the attacker and their sole allies.
				for (int j = 0; j < attackerAndAllies.size(); j++) {
					Empire enemy = attackerAndAllies.get(j);
					Offer warCascade = new Offer(map.getRelationship(enemy, ally));
					warCascade.setToWar();
					warCascade.defensivePactWarMainDefender = victim;
					fx.add(warCascade);
				}
			}
		} else if (offer.newLevel != null && offer.rel.level != Level.ALLIANCE && offer.newLevel == Level.ALLIANCE) {
			ArrayList<Empire> allianceMembers = new ArrayList<Empire>();
			allianceMembers.add(offer.rel.a);
			allianceMembers.add(offer.rel.b);
			ArrayList<Relationship> rels = map.getRelationships(offer.rel.a);
			for (int i = 0; i < rels.size(); i++) {
				if (rels.get(i).level == Level.ALLIANCE && !allianceMembers.contains(rels.get(i).other(offer.rel.a))) {
					allianceMembers.add(rels.get(i).other(offer.rel.a));
				}
			}
			rels = map.getRelationships(offer.rel.b);
			for (int i = 0; i < rels.size(); i++) {
				if (rels.get(i).level == Level.ALLIANCE && !allianceMembers.contains(rels.get(i).other(offer.rel.b))) {
					allianceMembers.add(rels.get(i).other(offer.rel.b));
				}
			}
			for (int i = 0; i < allianceMembers.size(); i++) {
				Empire a = allianceMembers.get(i);
				for (int j = 0; j < i; j++) { // We only want each relationship once.
					Empire b = allianceMembers.get(j);
					Relationship rel = map.getRelationship(a, b);
					if (rel == offer.rel) { continue; } // Cascade only
					if (rel.level == Level.ALLIANCE) { continue; } // Already allied
					Offer allianceCascade = new Offer(rel);
					allianceCascade.newLevel = Level.ALLIANCE;
					fx.add(allianceCascade);
				}
				ArrayList<Relationship> aRels = map.getRelationships(a);
				for (int j = 0; j < aRels.size(); j++) {
					Relationship rel = aRels.get(j);
					Empire enemy = rel.other(a);
					if (rel.level == Level.WAR) {
						for (int k = 0; k < allianceMembers.size(); k++) {
							Empire otherMember = allianceMembers.get(k);
							if (enemy == otherMember || a == otherMember) { continue; }
							Relationship enemyToOtherMember = map.getRelationship(enemy, otherMember);
							if (enemyToOtherMember.level != Level.WAR) {
								Offer warCascade = new Offer(enemyToOtherMember);
								warCascade.setToWar();
								fx.add(warCascade);
							}
						}
					}
				}
			}
		} else if (offer.newLevel != null && offer.rel.level == Level.ALLIANCE && offer.newLevel != Level.ALLIANCE) {
			// You can't dissolve an alliance bigger than 2 members, you can only leave it.
			// determine alliance size
			int allianceSize = 1;
			ArrayList<Relationship> aRels = map.getRelationships(offer.rel.a);
			for (int i = 0; i < aRels.size(); i++) {
				Relationship aRel = aRels.get(i);
				if (aRel.level == Level.ALLIANCE) {
					allianceSize++;
				}
			}
			if (forcer == null) {
				if (allianceSize > 2) {
					throw new RuntimeException("Can't agree to dissolve a multi-alliance.");
				}
				// No additional cascading needed, as it's an alliance between 2 players only.
			} else if (allianceSize > 2) {
				// Also cancelling alliance with your other allies.
				Empire victim = offer.rel.other(forcer);
				ArrayList<Relationship> forcerRels = map.getRelationships(forcer);
				for (int i = 0; i < forcerRels.size(); i++) {
					Relationship rel = forcerRels.get(i);
					if (rel.level == Relationship.Level.ALLIANCE) {
						Empire ally = rel.other(forcer);
						if (ally == victim) { continue; } // This is what we're cascading, so don't put it in twice.
						Offer dissolveCascade = new Offer(rel);
						dissolveCascade.newLevel = offer.newLevel;
						fx.add(dissolveCascade);
					}
				}
			}
		} else if (offer.rel.level == Level.WAR && offer.newLevel == Level.TRUCE) {
			// NB that other scenarios of going from war to sth else or to truce from something else should be handled in isValid.
			for (int sideI = 0; sideI < 2; sideI++) {
				Empire a = sideI == 0 ? offer.rel.a : offer.rel.b;
				Empire b = sideI == 0 ? offer.rel.b : offer.rel.a;
				ArrayList<Relationship> aRels = map.getRelationships(a);
				for (int i = 0; i < aRels.size(); i++) {
					Relationship aRelToOther = aRels.get(i);
					Relationship otherRelToB = map.getRelationship(aRelToOther.other(a), b);
					if (aRelToOther.level == Level.ALLIANCE) {
						Offer truceCascade = new Offer(otherRelToB);
						truceCascade.newLevel = Level.TRUCE;
						fx.add(truceCascade);
					}
					if (aRelToOther.level == Level.DEFENSIVE_PACT && otherRelToB.level == Level.WAR) {
						if (otherRelToB.defensivePactWarMainDefender == a) {
							// They are in this war because of us, so they also get a truce.
							Offer truceCascade = new Offer(otherRelToB);
							truceCascade.newLevel = Level.TRUCE;
							fx.add(truceCascade);
						}
					}
					// Special case where the ally of A should also get a truce with the ally or pact sibling of B.
					ArrayList<Relationship> bRels = map.getRelationships(b);
					for (int j = 0; j < bRels.size(); j++) {
						Relationship bRelToOther2 = bRels.get(j);
						if (bRelToOther2.other(b) == a) { continue; }
						if (bRelToOther2.other(b) == aRelToOther.other(a)) { continue; }
						Relationship otherRelToOther2 = map.getRelationship(aRelToOther.other(a), bRelToOther2.other(b));
						if (aRelToOther.level == Level.ALLIANCE && otherRelToOther2.level == Level.WAR &&
							((bRelToOther2.level == Level.ALLIANCE) || (bRelToOther2.level == Level.DEFENSIVE_PACT && otherRelToOther2.defensivePactWarMainDefender == b))
						) {
							Offer truceCascade = new Offer(otherRelToOther2);
							truceCascade.newLevel = Level.TRUCE;
							fx.add(truceCascade);
						}
					}
				}
			}
		}
		return fx;
	}
	
	public boolean canMakeChallenge(Empire challenger, HasRelationships m) {
		return challenge == null && getOffer(m) == null && getUltimatum(m) == null && level != Level.WAR && challenger.sendChallengeCooldown == 0;
	}
	
	public void makeChallenge(Challenge c, HasRelationships m) {
		Empire challenger = c.bToA ? b : a;
		Empire target = c.bToA ? a : b;
		if (!canMakeChallenge(challenger, m)) { return; }
		challenger.sendChallengeCooldown = EmpireStat.SEND_CHALLENGE_INTERVAL.get(challenger.bonuses);
		target.timeSinceLastChallenge = 0;
		challenge = c;
	}
	
	public void noChallengeResponse(HasRelationships m, String detail1, String detail2) {
		if (challenge == null) { return; }
		Empire challenger = challenge.bToA ? b : a;
		Empire target = challenge.bToA ? a : b;
		switch (challenge.type) {
			case DELEGATION:
				// Delegation was snubbed, so the challenger gains grievances but loses reputation.
				aGrievancesTowardsB += (challenge.bToA ? -1 : 1) * EmpireStat.DELEGATION_SNUB_GRIEVANCES.get(target.bonuses);
				challenger.changeReputation(-EmpireStat.DELEGATION_SNUB_REP_LOSS.get(challenger.bonuses), m);
				challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.DELEGATION_REJECTED, target, EmpireStat.DELEGATION_SNUB_REP_LOSS.get(challenger.bonuses), 0, EmpireStat.DELEGATION_SNUB_GRIEVANCES.get(target.bonuses), detail1, null));
				target.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.YOU_REJECTED_DELEGATION, challenger, 0, EmpireStat.DELEGATION_SNUB_REP_LOSS.get(challenger.bonuses), EmpireStat.DELEGATION_SNUB_GRIEVANCES.get(target.bonuses), detail1, null));
				break;
			case INSULT:
				// Insult went unanswered, so the target gains grievances but loses reputation.
				aGrievancesTowardsB += (challenge.bToA ? 1 : -1) * EmpireStat.UNANSWERED_INSULT_GRIEVANCES.get(target.bonuses);
				target.changeReputation(-EmpireStat.UNANSWERED_INSULT_REP_LOSS.get(challenger.bonuses), m);
				challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.INSULT_IGNORED, target, 0, EmpireStat.UNANSWERED_INSULT_REP_LOSS.get(challenger.bonuses), EmpireStat.UNANSWERED_INSULT_GRIEVANCES.get(target.bonuses), null, null));
				break;
		}
		challenge = null;
	}
	
	public void challengeResponse(HasRelationships m, String detail1, String detail2) {
		if (challenge == null) { return; }
		Empire challenger = challenge.bToA ? b : a;
		Empire target = challenge.bToA ? a : b;
		switch (challenge.type) {
			case DELEGATION:
				// Delegation was accepted.
				if (aGrievancesTowardsB == 0) {
					challenger.changeReputation(EmpireStat.NO_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), m);
					target.changeReputation(EmpireStat.NO_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(challenger.bonuses), m);
					challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.DELEGATION_ACCEPTED_NO_GRIEVANCES, target, EmpireStat.NO_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), EmpireStat.NO_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(challenger.bonuses), 0, detail1, null));
				} else if (challenge.bToA) {
					// Sender is b, receiver is a.
					if (aGrievancesTowardsB < 0) {
						// B has grievances towards A.
						challenger.changeReputation(EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), m);
						int grievancesChange = StrictMath.min(-aGrievancesTowardsB, EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_GRIEVANCES_LOSS.get(challenger.bonuses));
						aGrievancesTowardsB += grievancesChange;
						challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.DELEGATION_ACCEPTED_SENDER_GRIEVANCES, target, EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), 0, grievancesChange, detail1, null));
					} else {
						// A has grievances towards B.
						challenger.changeReputation(EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), m);
						target.changeReputation(EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(challenger.bonuses), m);
						int grievancesChange = StrictMath.min(aGrievancesTowardsB, EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_GRIEVANCES_LOSS.get(challenger.bonuses));
						aGrievancesTowardsB -= grievancesChange;
						challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.DELEGATION_ACCEPTED_RECEIVER_GRIEVANCES, target, EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(challenger.bonuses), grievancesChange, detail1, null));
					}
				} else {
					// Sender is a, receiver is b.
					if (aGrievancesTowardsB > 0) {
						// A has grievances towards B.
						challenger.changeReputation(EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), m);
						int grievancesChange = StrictMath.min(aGrievancesTowardsB, EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_GRIEVANCES_LOSS.get(challenger.bonuses));;
						aGrievancesTowardsB -= grievancesChange;
						challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.DELEGATION_ACCEPTED_SENDER_GRIEVANCES, target, EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), 0, grievancesChange, detail1, null));
					} else {
						// B has grievances towards A.
						challenger.changeReputation(EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), m);
						target.changeReputation(EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(challenger.bonuses), m);
						int grievancesChange = StrictMath.min(-aGrievancesTowardsB, EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_GRIEVANCES_LOSS.get(challenger.bonuses));
						aGrievancesTowardsB += grievancesChange;
						challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.DELEGATION_ACCEPTED_RECEIVER_GRIEVANCES, target, EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(challenger.bonuses), EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(challenger.bonuses), grievancesChange, detail1, null));
					}
				}
				break;
			case INSULT:
				// Insult was answered, so the challenger gains grievances but loses reputation.
				aGrievancesTowardsB += (challenge.bToA ? -1 : 1) * EmpireStat.ANSWERED_INSULT_GRIEVANCES.get(challenger.bonuses);
				challenger.changeReputation(-EmpireStat.ANSWERED_INSULT_REP_LOSS.get(target.bonuses), m);
				challenger.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.INSULT_RETURNED, target, EmpireStat.ANSWERED_INSULT_REP_LOSS.get(target.bonuses), 0, EmpireStat.ANSWERED_INSULT_GRIEVANCES.get(challenger.bonuses), detail1, detail2));
				target.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.YOU_RETURNED_INSULT, challenger, 0, EmpireStat.ANSWERED_INSULT_REP_LOSS.get(target.bonuses), EmpireStat.ANSWERED_INSULT_GRIEVANCES.get(challenger.bonuses), detail1, detail2));
				break;
		}
		challenge = null;
	}
	
	public static class Challenge {
		public final boolean bToA;
		public final Type type;
		public int timeLeft;
		public boolean responded;
		public String detail1, detail2;
		public transient boolean announced;
		
		public static enum Type {
			DELEGATION(new Img("ui", 272, 368, 16, 16, false), new Img("ui", 272, 368, 16, 16, false), new Img("ui", 368, 512, 16, 16, false), new Img("ui", 144, 416, 16, 16, false)),
			INSULT(new Img("ui", 256, 368, 16, 16, false), new Img("ui", 288, 368, 16, 16, false), new Img("ui", 256, 368, 16, 16, false), new Img("ui", 144, 416, 16, 16, false));
			
			public final Img senderIcon;
			public final Img receiverIcon;
			public final Img responseIcon;
			public final Img noResponseIcon;
			private Type(Img senderIcon, Img receiverIcon, Img responseIcon, Img noResponseIcon) {
				this.senderIcon = senderIcon;
				this.receiverIcon = receiverIcon;
				this.responseIcon = responseIcon;
				this.noResponseIcon = noResponseIcon;
			}
		}
		
		public Challenge(Empire actor, Relationship rel, Type type, int timeLeft, GuardedRandom r) {
			bToA = rel.b == actor;
			this.type = type;
			this.timeLeft = timeLeft;
			switch (type) {
				case DELEGATION:
					int a = r.nextInt(EmpireStat.DELEGATION_NUM.get(actor.bonuses));
					int b = a;
					for (int i = 0; i < 10 && b == a; i++) {
						b = r.nextInt(EmpireStat.DELEGATION_NUM.get(actor.bonuses));
					}
					detail1 = EmpireStat.DELEGATION_PREFIX.get(actor.bonuses) + a;
					detail2 = EmpireStat.DELEGATION_PREFIX.get(actor.bonuses) + b;
					break;
				case INSULT:
					detail1 = EmpireStat.INSULT_1_PREFIX.get(actor.bonuses) + r.nextInt(EmpireStat.INSULT_1_NUM.get(actor.bonuses));
					detail2 = EmpireStat.INSULT_2_PREFIX.get(actor.bonuses) + r.nextInt(EmpireStat.INSULT_2_NUM.get(actor.bonuses));
					break;
			}
		}
		
		public String getDetail(Relationship rel) {
			if (Lang.flavour()) {
				switch (type) {
					case DELEGATION:
						return _t("delegation_text", challenger(rel).getName(), _t(detail1), _t(detail2));
					case INSULT:
						if (SimplePref.SHOW_INSULTS.get()) {
							return _t(detail1) + " " + _t(detail2);
						} else {
							return "***!";
						}
				}
			}
			return "";
		}
		
		public Challenge(JSONObject o) {
			bToA = o.getBoolean("bToA");
			type = Type.valueOf(o.getString("type"));
			timeLeft = o.getInt("timeLeft");
			responded = o.getBoolean("responded");
			detail1 = o.getString("detail1");
			detail2 = o.getString("detail2");
		}
		
		public JSONObject toJSON() {
			return new JSONObject().put("bToA", bToA).put("type", type.name()).put("timeLeft", timeLeft).put("responded", responded).put("detail1", detail1).put("detail2", detail2);
		}
		
		public Empire challenger(Relationship rel) {
			return bToA ? rel.b : rel.a;
		}
		
		public Empire target(Relationship rel) {
			return bToA ? rel.a : rel.b;
		}
	}
	
	public static boolean demandAndThreatOverlap(Offer demand, Offer threat) {
			if (threat.newLevel == Level.WAR) { return false; } // So that you can demand treaty cancellation by threatening war.
			return
					(demand.newLevel != null && demand.newLevel == threat.newLevel) ||
					(demand.newTribute != null && demand.newTribute == threat.newTribute) ||
					(demand.newTradeTreaty != null && demand.newTradeTreaty.equals(threat.newTradeTreaty)) ||
					(demand.newResearchTreaty != null && demand.newResearchTreaty.equals(threat.newResearchTreaty))
					;
		}
	
	public static class Ultimatum {
		public final Empire forcer;
		private final Offer demand;
		private final Offer orElse;
		public int timeLeft;
		public boolean defied;
		
		public transient boolean announced;
		public transient boolean defianceAnnounced;

		public Ultimatum(HasRelationships wm, Relationship rel, Empire forcer, Offer demand, Offer orElse, int timeLeft) {
			this.forcer = forcer;
			this.demand = demand;
			this.orElse = orElse;
			this.timeLeft = timeLeft;
		}
		
		public boolean isValid(HasRelationships map) {
			Offer demand = getDemand();
			Offer orElse = getOrElse();
			return !demand.isEmpty() && !orElse.isEmpty() && demand.rel.isValid(demand, null, map, true) && orElse.rel.isValid(orElse, forcer, map, false) && !demandAndThreatOverlap();
		}
		
		public Offer getOriginalDemand() {
			return demand;
		}
		
		public Offer getOriginalOrElse() {
			return orElse;
		}
		
		public Offer getDemand() {
			Offer d2 = demand.clone();
			d2.clean();
			return d2;
		}
		
		public Offer getOrElse() {
			Offer o2 = orElse.clone();
			o2.clean();
			return o2;
		}
		
		public JSONObject toJSON() {
			return new JSONObject()
					.put("forcer", forcer.id)
					.put("timeLeft", timeLeft)
					.put("defied", defied)
					.put("demand", demand.toJSON())
					.put("orElse", orElse.toJSON());
		}
		
		public Ultimatum(JSONObject o, WorldMap m, Relationship rel) {
			forcer = m.getEmpire(o.getInt("forcer"));
			timeLeft = o.getInt("timeLeft");
			defied = o.getBoolean("defied");
			if (rel == null) {
				rel = m.getRelationship(m.getEmpire(o.getJSONObject("demand").getInt("relA")), m.getEmpire(o.getJSONObject("demand").getInt("relB")));
			}
			demand = new Offer(o.getJSONObject("demand"), rel, m);
			orElse = new Offer(o.getJSONObject("orElse"), rel, m);
			if (!orElse.rel.contains(forcer)) {
				throw new RuntimeException("Ultimatum orElse must involve ultimatum-maker.");
			}
		}

		public Relationship getRelationship() {
			return demand.rel;
		}
		
		private boolean demandAndThreatOverlap() {
			return Relationship.demandAndThreatOverlap(getDemand(), getOrElse());
		}

		public String getInvalidReason(HasRelationships map) {
			if (demandAndThreatOverlap()) {
				return "ultimatum_demand_and_threat_overlap";
			}
			if (defied) {
				return orElse.rel.getInvalidReason(orElse, forcer, map, false);
			}
			String ir = demand.rel.getInvalidReason(demand, null, map, true);
			return ir == null ? orElse.rel.getInvalidReason(orElse, forcer, map, false) : ir;
		}
		
		public boolean isDeFactoWarDeclaration() {
			if (orElse.newLevel != Level.WAR) { return false; }
			if (demand.rel.level != Level.PEACE && demand.newLevel != Level.PEACE) { return false; }
			if (demand.rel.tradeTreaty && demand.newTradeTreaty == null) { return false; }
			if (demand.rel.researchTreaty && demand.newResearchTreaty == null) { return false; }
			if (demand.rel.tribute != Direction.NEITHER && demand.newTribute == null) { return false; }
			return demand.rel.level != Level.PEACE || demand.rel.tradeTreaty || demand.rel.researchTreaty || demand.rel.tribute != Direction.NEITHER;
		}

		public Empire getVictim() {
			return orElse.rel.other(forcer);
		}
	}
}
