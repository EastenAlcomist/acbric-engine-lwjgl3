package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class DiplomacyPersonality extends Loadable {
	public int weight = 1;
	public Bonus forBonus = null;
	public BonusableValue<Integer> bonusOpinion = BonusableValue.of(0);
	
	public double reputationLossSensitivity = 3;
	public double doingBadlyReputationLossSensitivity = 0.2;
	public double aboutToConquerReputationLossSensitivity = 1;
	public double aboutToCoronateReputationLossSensitivity = 8;
	public double ultimatumQualityDifferenceToLikelihoodPercent = 2;
	public int repLossWouldCancelCoronationPenalty = 999;
	public double grievancesToForcedActionMultiplier = 1;
	public double enforceThreatQualityToPercentageMultiplier = 4;
	public int minWeeksAtPeaceForRisingAggression = 30;
	public double risingAggressionPerWeekAtPeace = 0.25;
	public int maxRisingAggressionBonus = 30;
	public int newWarExtraAggression = 100;
	public int makeUltimatumBaseline = 5;
	public int agreeToUltimatumBaseline = -10;
	public int enforceUltimatumBaseline = 0;
	public int warBaseline = 0;
	public int peaceBaseline = 0;
	public int nonAggressionPactBaseline = -10;
	public int defensivePactBaseline = -30;
	public int allianceBaseline = -45;
	public int giveTributeBaseline = -35;
	public int receiveTributeBaseline = 1;
	public int tradeTreatyBaseline = -8;
	public int researchTreatyBaseline = -20;
	public int defensivePactSimilarStrengthBonus = 12;
	public int allianceSimilarStrengthBonus = 15;
	public int allianceSharedWarsBonus = 20;
	public int nonAggressionPactLongRelationshipMaxBonus = 10;
	public int defensivePactLongRelationshipMaxBonus = 15;
	public int allianceLongRelationshipMaxBonus = 30;
	public int tradeTreatyLongRelationshipMaxBonus = 10;
	public int researchTreatyLongRelationshipMaxBonus = 10;
	public double nonAggressionPactNeighbourThreatMultiplier = 0.2;
	public double defensivePactNeighbourThreatMultiplier = 0.5;
	public double allianceNeighbourThreatMultiplier = 0.35;
	public double declareWarOnLowRepMultiplier = 0.25;
	public double avoidNonAggressionPactWithLowRepMultiplier = 0.3;
	public double defensivePactPerRepMultiplier = 0.5;
	public double alliancePerRepMultiplier = 0.7;
	public double avoidTributeFromVeryLowRepMultiplier = 0.7;
	public double avoidTradeTreatyFromVeryLowRepMultiplier = 0.3;
	public double avoidResearchTreatyFromVeryLowRepMultiplier = 0.5;
	public int avoidNonAggressionPactPerOtherPact = 3;
	public int avoidDefensivePactPerOtherPact = 5;
	public int avoidAlliancePerOtherAlliance = 15;
	public int avoidAllianceWhenAboutToWin = 30;
	public int wantAllianceWhenOtherAboutToWin = 50;
	public int inTheWayOfVictoryWarBonus = 25;
	public int avoidTreatiesWhenAboutToConquerAll = 30;
	public double ownStrengthEstimationMultiplier = 1.1;
	public double tributeRelativeStrengthMult = 0.5;
	public int avoidGivingTributePerOtherTribute = 20;
	public int receiveTributeRelativeIncomeBonus = 7;
	public int tradeTreatyRelativeIncomeBonus = 5;
	public int researchTreatyRelativeResearchBonus = 5;
	public int researchTreatyPerBoostedTechBonus = 3;
	public double compromiseOtherSideIsHappyFactor = 0;
	public double compromiseUnfairToMeFactor = 0.5;
	public double compromiseUnfairToThemFactor = 0.2;
	public double compromiseRelativeStrengthFactor = 0.3;
	public int aboutToCoronateWarBonus = 20;
	public int aboutToDoFinalRitualWarBonus = 80;
	public int ritualSitesWarBonus = 0;
	public int desireSpecialWarBonus = 8;
	public double cityUpgradesTargetSpendProportion = 0.4;
	public int unfairOfferForHumansBonus = 5; // This should arguably go into the difficulty level.
	public int minimalOfferUpside = 3;
	public int minimalForceUpside = 3;
	public int recentAccededToUltimatumPenalty = 40;
	public int accedeToUltimatumCooldown = 145600;
	
	public double giveMoneyFactor = 2;
	public double receiveMoneyFactor = 0.5;
	public int maxReceiveMoneyBonus = 6;
	
	public double warRelativeStrengthFactor = 1;
	public double peaceRelativeStrengthFactor = 0.3;
	public double nonAggressionPactOtherIsWeakerFactor = 0.4;
	public double nonAggressionPactOtherIsStrongerFactor = 0.7;
	
	public int warUnrestTolerance = 10;
	public double extraUnrestAvoidWarFactor = 0.3;
	public int maxConquestUnrest = 20;
	public boolean attackOthersMonsterNests = false;
	
	public double defensivePactOtherIsStrongerFactor = 0.5;
	public double allianceOtherIsStrongerFactor = 0.5;
	public double avoidAngeringStrongerEmpireFactor = 0.02;
	
	public int minUltimatumVersusThreatTheirQualityDifference = 10;
	public int minUltimatumVersusThreatMyQualityDifference = 5;
	
	public double targetResearchFromUpgradesPerYear = 3;
	public double targetMaxProductionPerYear = 0.5;
	public double targetGlobalSupplyBonusPerYear = 0.4;
	
	public double allyStrengthEstimationFactor = 0.7;
	
	public String assumeOtherEmpiresHaveThisPersonality;
	
	public TakeoverMethod takeoverMethod = null;
	
	public int insultBackBaseline = 50;
	public int aboutToCoronateInsultBackBonus = 50;
	public int allowInsultToGetWantedGrievancesBonus = 35;
	public int allowInsultToPreventUnwantedGrievancesBonus = 35;
	public int acceptDelegationBaseline = 75;
	public int acceptDelegationWithGrievancesBaseline = 50;
	public int rejectDelegationToPreventCoronationPenalty = 60;
	public int acceptDelegationDuringCoronationBonus = 20;
	public int acceptDelegationInPositiveRelationshipBonus = 20;
	
	public int sendInsultBaseline = -4;
	public int sendDelegationBaseline = -4;
	
	public int townSendBaseline = -25;
	public int citySendBaseline = -50;
	public int townReceiveBaseline = 20;
	public int cityReceiveBaseline = 40;
	public int cityTransferOnlyCityMalus = 999;
	public double cityTransferIncomeMultiplier = 0.25;
	public int cityTransferDesiredBonus = 20;
	public int cityTransferSpecialBonus = 10;
	public int cityTransferRitualSiteBonus = 0;
	public int cityTransferBelongsToReceiverBonus = 10;
	public int cityTransferBelongsToSenderMalus = 10;
	public int cityTransferDisconnectedFromCapitalBonus = 15;
	public int cityTransferDisconnectedFromCapitalMalus = 15;
	public int cityTransferWhenCoronatingMalus = 999;
	public int cityTransferEnoughCitiesForCoronationMalus = 50;
	public double reputationGainSensitivity = 1.5;
	public double enoughCitiesForCoronationReputationGainSensitivity = 2.5;
	
	public int giveSubmissionBaseline = -10;
	public int receiveSubmissionBaseline = 3;
	
	public int maxNonAggressionPactQuality = 30;
	public int maxDefensivePactQuality = 45;
	public int maxAllianceQuality = 60;
	public int maxReceiveTributeQuality = 30;
	public int maxTradeTreatyQuality = 30;
	public int maxResearchTreatyQuality = 30;
	public int maxWarQuality = 100;
	public int minWarQuality = -150;
	
	public int territoryUltimatumMalus = -90;
	public int territoryLossMalus = -40;
	
	public int nonAggressionPactLoyalty = 5;
	public int defensivePactLoyalty = 15;
	public int allianceLoyalty = 25;
	
	public ArrayList<CityUpgradeType> desiredSpecials = new ArrayList<CityUpgradeType>();
	
	public int incidentsHonest = 0;
	public int incidentsJust = 0;
	public int incidentsAggressive = 0;
	
	public int doAnnexBase = 20;
	public int doAnnexPerGrievance = -3;
	public int becomeAnnexedBase = -8;
	public int becomeAnnexedPerSize = 3;
	public int becomeAnnexedPerGrievance = -5;
	public double becomeAnnexedPerRep = 0.25;
	public int becomeAnnexedNotAdjacent = -15;
	public int becomeAnnexedSharedGrievances = 3;
	public int becomeAnnexedDefensivePact = 10;
	public int becomeAnnexedAlliance = 25;
	
	public DiplomacyPersonality(JSONObject o) {
		super(o.getString("name"));
		weight = o.optInt("weight", weight);
		if (o.has("forBonus")) {
			forBonus = Bonus.ofName(o.getString("forBonus"));
		}
		incidentsHonest = o.optInt("incidentsHonest", incidentsHonest);
		incidentsJust = o.optInt("incidentsJust", incidentsJust);
		incidentsAggressive = o.optInt("incidentsAggressive", incidentsAggressive);
		maxWarQuality = o.optInt("maxWarQuality", maxWarQuality);
		minWarQuality = o.optInt("minWarQuality", minWarQuality);
		maxNonAggressionPactQuality = o.optInt("maxNonAggressionPactQuality", maxNonAggressionPactQuality);
		maxDefensivePactQuality = o.optInt("maxDefensivePactQuality", maxDefensivePactQuality);
		maxAllianceQuality = o.optInt("maxAllianceQuality", maxAllianceQuality);
		maxReceiveTributeQuality = o.optInt("maxReceiveTributeQuality", maxReceiveTributeQuality);
		maxTradeTreatyQuality = o.optInt("maxTradeTreatyQuality", maxTradeTreatyQuality);
		maxResearchTreatyQuality = o.optInt("maxResearchTreatyQuality", maxResearchTreatyQuality);
		territoryUltimatumMalus = o.optInt("territoryUltimatumMalus", territoryUltimatumMalus);
		territoryLossMalus = o.optInt("territoryLossMalus", territoryLossMalus);
		recentAccededToUltimatumPenalty = o.optInt("recentAccededToUltimatumPenalty", recentAccededToUltimatumPenalty);
		accedeToUltimatumCooldown = o.optInt("accedeToUltimatumCooldown", accedeToUltimatumCooldown);
		bonusOpinion = BonusableValue.intFromJSON(o, "bonusOpinion", 0);
		takeoverMethod = TakeoverMethod.ofName(o.optString("takeoverMethod", "GENTLE"));
		ultimatumQualityDifferenceToLikelihoodPercent = o.optDouble("ultimatumQualityDifferenceToLikelihoodPercent", ultimatumQualityDifferenceToLikelihoodPercent);
		reputationLossSensitivity = o.optDouble("reputationLossSensitivity", reputationLossSensitivity);
		doingBadlyReputationLossSensitivity = o.optDouble("doingBadlyReputationLossSensitivity", doingBadlyReputationLossSensitivity);
		aboutToConquerReputationLossSensitivity = o.optDouble("aboutToConquerReputationLossSensitivity", aboutToConquerReputationLossSensitivity);
		aboutToCoronateReputationLossSensitivity = o.optDouble("aboutToCoronateReputationLossSensitivity", aboutToCoronateReputationLossSensitivity);
		repLossWouldCancelCoronationPenalty = o.optInt("repLossWouldCancelCoronationPenalty", repLossWouldCancelCoronationPenalty);
		grievancesToForcedActionMultiplier = o.optDouble("grievancesToForcedActionMultiplier", grievancesToForcedActionMultiplier);
		enforceThreatQualityToPercentageMultiplier = o.optDouble("enforceThreatQualityToPercentageMultiplier", enforceThreatQualityToPercentageMultiplier);
		minWeeksAtPeaceForRisingAggression = o.optInt("minWeeksAtPeaceForRisingAggression", minWeeksAtPeaceForRisingAggression);
		risingAggressionPerWeekAtPeace = o.optDouble("risingAggressionPerWeekAtPeace", risingAggressionPerWeekAtPeace);
		maxRisingAggressionBonus = o.optInt("maxRisingAggressionBonus", maxRisingAggressionBonus);
		newWarExtraAggression = o.optInt("newWarExtraAggression", newWarExtraAggression);
		makeUltimatumBaseline = o.optInt("makeUltimatumBaseline", makeUltimatumBaseline);
		agreeToUltimatumBaseline = o.optInt("agreeToUltimatumBaseline", agreeToUltimatumBaseline);
		enforceUltimatumBaseline = o.optInt("enforceUltimatumBaseline", enforceUltimatumBaseline);
		warBaseline = o.optInt("warBaseline", warBaseline);
		nonAggressionPactLongRelationshipMaxBonus = o.optInt("nonAggressionPactLongRelationshipMaxBonus", nonAggressionPactLongRelationshipMaxBonus);
		defensivePactLongRelationshipMaxBonus = o.optInt("defensivePactLongRelationshipMaxBonus", defensivePactLongRelationshipMaxBonus);
		allianceLongRelationshipMaxBonus = o.optInt("allianceLongRelationshipMaxBonus", allianceLongRelationshipMaxBonus);
		tradeTreatyLongRelationshipMaxBonus = o.optInt("tradeTreatyLongRelationshipMaxBonus", tradeTreatyLongRelationshipMaxBonus);
		researchTreatyLongRelationshipMaxBonus = o.optInt("researchTreatyLongRelationshipMaxBonus", researchTreatyLongRelationshipMaxBonus);
		peaceBaseline = o.optInt("peaceBaseline", peaceBaseline);
		nonAggressionPactBaseline = o.optInt("nonAggressionPactBaseline", nonAggressionPactBaseline);
		defensivePactBaseline = o.optInt("defensivePactBaseline", defensivePactBaseline);
		allianceBaseline = o.optInt("allianceBaseline", allianceBaseline);
		giveTributeBaseline = o.optInt("giveTributeBaseline", giveTributeBaseline);
		receiveTributeBaseline = o.optInt("receiveTributeBaseline", receiveTributeBaseline);
		tradeTreatyBaseline = o.optInt("tradeTreatyBaseline", tradeTreatyBaseline);
		researchTreatyBaseline = o.optInt("researchTreatyBaseline", researchTreatyBaseline);
		defensivePactSimilarStrengthBonus = o.optInt("defensivePactSimilarStrengthBonus", defensivePactSimilarStrengthBonus);
		allianceSimilarStrengthBonus = o.optInt("allianceSimilarStrengthBonus", allianceSimilarStrengthBonus);
		allianceSharedWarsBonus = o.optInt("allianceSharedWarsBonus", allianceSharedWarsBonus);
		nonAggressionPactNeighbourThreatMultiplier = o.optDouble("nonAggressionPactNeighbourThreatMultiplier", nonAggressionPactNeighbourThreatMultiplier);
		defensivePactNeighbourThreatMultiplier = o.optDouble("defensivePactNeighbourThreatMultiplier", defensivePactNeighbourThreatMultiplier);
		allianceNeighbourThreatMultiplier = o.optDouble("allianceNeighbourThreatMultiplier", allianceNeighbourThreatMultiplier);
		declareWarOnLowRepMultiplier = o.optDouble("declareWarOnLowRepMultiplier", declareWarOnLowRepMultiplier);
		avoidNonAggressionPactWithLowRepMultiplier = o.optDouble("avoidNonAggressionPactWithLowRepMultiplier", avoidNonAggressionPactWithLowRepMultiplier);
		defensivePactPerRepMultiplier = o.optDouble("defensivePactPerRepMultiplier", defensivePactPerRepMultiplier);
		alliancePerRepMultiplier = o.optDouble("alliancePerRepMultiplier", alliancePerRepMultiplier);
		avoidTributeFromVeryLowRepMultiplier = o.optDouble("avoidTributeFromVeryLowRepMultiplier", avoidTributeFromVeryLowRepMultiplier);
		avoidTradeTreatyFromVeryLowRepMultiplier = o.optDouble("avoidTradeTreatyFromVeryLowRepMultiplier", avoidTradeTreatyFromVeryLowRepMultiplier);
		avoidResearchTreatyFromVeryLowRepMultiplier = o.optDouble("avoidResearchTreatyFromVeryLowRepMultiplier", avoidResearchTreatyFromVeryLowRepMultiplier);
		avoidNonAggressionPactPerOtherPact = o.optInt("avoidNonAggressionPactPerOtherPact", avoidNonAggressionPactPerOtherPact);
		avoidDefensivePactPerOtherPact = o.optInt("avoidDefensivePactPerOtherPact", avoidDefensivePactPerOtherPact);
		avoidAlliancePerOtherAlliance = o.optInt("avoidAlliancePerOtherAlliance", avoidAlliancePerOtherAlliance);
		avoidAllianceWhenAboutToWin = o.optInt("avoidAllianceWhenAboutToWin", avoidAllianceWhenAboutToWin);
		wantAllianceWhenOtherAboutToWin = o.optInt("wantAllianceWhenOtherAboutToWin", wantAllianceWhenOtherAboutToWin);
		inTheWayOfVictoryWarBonus = o.optInt("inTheWayOfVictoryWarBonus", inTheWayOfVictoryWarBonus);
		ownStrengthEstimationMultiplier = o.optDouble("ownStrengthEstimationMultiplier", ownStrengthEstimationMultiplier);
		avoidTreatiesWhenAboutToConquerAll = o.optInt("avoidTreatiesWhenAboutToConquerAll", avoidTreatiesWhenAboutToConquerAll);
		tributeRelativeStrengthMult = o.optDouble("tributeRelativeStrengthMult", tributeRelativeStrengthMult);
		avoidGivingTributePerOtherTribute = o.optInt("avoidGivingTributePerOtherTribute", avoidGivingTributePerOtherTribute);
		receiveTributeRelativeIncomeBonus = o.optInt("receiveTributeRelativeIncomeBonus", receiveTributeRelativeIncomeBonus);
		tradeTreatyRelativeIncomeBonus = o.optInt("tradeTreatyRelativeIncomeBonus", tradeTreatyRelativeIncomeBonus);
		researchTreatyRelativeResearchBonus = o.optInt("researchTreatyRelativeResearchBonus", researchTreatyRelativeResearchBonus);
		researchTreatyPerBoostedTechBonus = o.optInt("researchTreatyPerBoostedTechBonus", researchTreatyPerBoostedTechBonus);
		compromiseOtherSideIsHappyFactor = o.optDouble("compromiseOtherSideIsHappyFactor", compromiseOtherSideIsHappyFactor);
		compromiseUnfairToMeFactor = o.optDouble("compromiseUnfairToMeFactor", compromiseUnfairToMeFactor);
		compromiseUnfairToThemFactor = o.optDouble("compromiseUnfairToThemFactor", compromiseUnfairToThemFactor);
		compromiseRelativeStrengthFactor = o.optDouble("compromiseRelativeStrengthFactor", compromiseRelativeStrengthFactor);
		unfairOfferForHumansBonus = o.optInt("unfairOfferForHumansBonus", unfairOfferForHumansBonus);
		warRelativeStrengthFactor = o.optDouble("warRelativeStrengthFactor", warRelativeStrengthFactor);
		peaceRelativeStrengthFactor = o.optDouble("peaceRelativeStrengthFactor", peaceRelativeStrengthFactor);
		nonAggressionPactOtherIsWeakerFactor = o.optDouble("nonAggressionPactRelativeStrengthFactor", nonAggressionPactOtherIsWeakerFactor);
		nonAggressionPactOtherIsStrongerFactor = o.optDouble("nonAggressionPactOtherIsStrongerFactor", nonAggressionPactOtherIsStrongerFactor);
		defensivePactOtherIsStrongerFactor = o.optDouble("defensivePactOtherIsStrongerFactor", defensivePactOtherIsStrongerFactor);
		allianceOtherIsStrongerFactor = o.optDouble("allianceOtherIsStrongerFactor", allianceOtherIsStrongerFactor);
		avoidAngeringStrongerEmpireFactor = o.optDouble("avoidAngeringStrongerEmpireFactor", avoidAngeringStrongerEmpireFactor);
		minUltimatumVersusThreatTheirQualityDifference = o.optInt("minUltimatumVersusThreatQualityDifference", minUltimatumVersusThreatTheirQualityDifference);
		minUltimatumVersusThreatMyQualityDifference = o.optInt("minUltimatumVersusThreatMyQualityDifference", minUltimatumVersusThreatMyQualityDifference);
		allyStrengthEstimationFactor = o.optDouble("allyStrengthEstimationFactor", allyStrengthEstimationFactor);
		aboutToCoronateWarBonus = o.optInt("aboutToCoronateWarBonus", aboutToCoronateWarBonus);
		aboutToDoFinalRitualWarBonus = o.optInt("aboutToDoFinalRitualWarBonus", aboutToDoFinalRitualWarBonus);
		ritualSitesWarBonus = o.optInt("ritualSitesWarBonus", ritualSitesWarBonus);
		warUnrestTolerance = o.optInt("warUnrestTolerance", warUnrestTolerance);
		extraUnrestAvoidWarFactor = o.optDouble("extraUnrestAvoidWarFactor", extraUnrestAvoidWarFactor);
		maxConquestUnrest = o.optInt("maxConquestUnrest", maxConquestUnrest);
		attackOthersMonsterNests = o.optBoolean("attackOthersMonsterNests", attackOthersMonsterNests);
		targetResearchFromUpgradesPerYear = o.optDouble("targetResearchFromUpgradesPerYear", targetResearchFromUpgradesPerYear);
		targetMaxProductionPerYear = o.optDouble("targetMaxProductionPerYear", targetMaxProductionPerYear);
		targetGlobalSupplyBonusPerYear = o.optDouble("targetGlobalSupplyBonusPerYear", targetGlobalSupplyBonusPerYear);
		cityUpgradesTargetSpendProportion = o.optDouble("cityUpgradesTargetSpendProportion", cityUpgradesTargetSpendProportion);
		minimalOfferUpside = o.optInt("minimalOfferUpside", minimalOfferUpside);
		minimalForceUpside = o.optInt("minimalForceUpside", minimalForceUpside);
		assumeOtherEmpiresHaveThisPersonality = o.optString("assumeOtherEmpiresHaveThisPersonality", assumeOtherEmpiresHaveThisPersonality);
		insultBackBaseline = o.optInt("insultBackBaseline", insultBackBaseline);
		aboutToCoronateInsultBackBonus = o.optInt("aboutToCoronateInsultBackBonus", aboutToCoronateInsultBackBonus);
		allowInsultToGetWantedGrievancesBonus = o.optInt("allowInsultToGetWantedGrievancesBonus", allowInsultToGetWantedGrievancesBonus);
		allowInsultToPreventUnwantedGrievancesBonus = o.optInt("allowInsultToPreventUnwantedGrievancesBonus", allowInsultToPreventUnwantedGrievancesBonus);
		acceptDelegationBaseline = o.optInt("acceptDelegationBaseline", acceptDelegationBaseline);
		acceptDelegationWithGrievancesBaseline = o.optInt("acceptDelegationWithGrievancesBaseline", acceptDelegationWithGrievancesBaseline);
		rejectDelegationToPreventCoronationPenalty = o.optInt("rejectDelegationToPreventCoronationPenalty", rejectDelegationToPreventCoronationPenalty);
		acceptDelegationDuringCoronationBonus = o.optInt("acceptDelegationDuringCoronationBonus", acceptDelegationDuringCoronationBonus);
		acceptDelegationInPositiveRelationshipBonus = o.optInt("acceptDelegationInPositiveRelationshipBonus", acceptDelegationInPositiveRelationshipBonus);
		sendInsultBaseline = o.optInt("sendInsultBaseline", sendInsultBaseline);
		sendDelegationBaseline = o.optInt("sendDelegationBaseline", sendDelegationBaseline);
		if (o.has("desiredSpecials")) {
			JSONArray a = o.getJSONArray("desiredSpecials");
			for (int i = 0; i < a.length(); i++) {
				desiredSpecials.add(CityUpgradeType.ofName(a.getString(i)));
			}
		}
		desireSpecialWarBonus = o.optInt("desireSpecialWarBonus", desireSpecialWarBonus);
		giveMoneyFactor = o.optDouble("giveMoneyFactor", giveMoneyFactor);
		receiveMoneyFactor = o.optDouble("receiveMoneyFactor", receiveMoneyFactor);
		maxReceiveMoneyBonus = o.optInt("maxReceiveMoneyBonus", maxReceiveMoneyBonus);
		townSendBaseline = o.optInt("townSendBaseline", townSendBaseline);
		citySendBaseline = o.optInt("citySendBaseline", citySendBaseline);
		townReceiveBaseline = o.optInt("townReceiveBaseline", townReceiveBaseline);
		cityReceiveBaseline = o.optInt("cityReceiveBaseline", cityReceiveBaseline);
		cityTransferOnlyCityMalus = o.optInt("cityTransferOnlyCityMalus", cityTransferOnlyCityMalus);
		cityTransferIncomeMultiplier = o.optDouble("cityTransferIncomeMultiplier", cityTransferIncomeMultiplier);
		cityTransferDesiredBonus = o.optInt("cityTransferDesiredBonus", cityTransferDesiredBonus);
		cityTransferRitualSiteBonus = o.optInt("cityTransferRitualSiteBonus", cityTransferRitualSiteBonus);
		cityTransferSpecialBonus = o.optInt("cityTransferSpecialBonus", cityTransferSpecialBonus);
		cityTransferBelongsToReceiverBonus = o.optInt("cityTransferBelongsToReceiverBonus", cityTransferBelongsToReceiverBonus);
		cityTransferBelongsToSenderMalus = o.optInt("cityTransferBelongsToSenderMalus", cityTransferBelongsToSenderMalus);
		cityTransferDisconnectedFromCapitalBonus = o.optInt("cityTransferDisconnectedFromCapitalBonus", cityTransferDisconnectedFromCapitalBonus);
		cityTransferDisconnectedFromCapitalMalus = o.optInt("cityTransferDisconnectedFromCapitalMalus", cityTransferDisconnectedFromCapitalMalus);
		cityTransferWhenCoronatingMalus = o.optInt("cityTransferWhenCoronatingMalus", cityTransferWhenCoronatingMalus);
		cityTransferEnoughCitiesForCoronationMalus = o.optInt("cityTransferEnoughCitiesForCoronationMalus", cityTransferEnoughCitiesForCoronationMalus);
		reputationGainSensitivity = o.optDouble("reputationGainSensitivity", reputationGainSensitivity);
		enoughCitiesForCoronationReputationGainSensitivity = o.optDouble("enoughCitiesForCoronationReputationGainSensitivity", enoughCitiesForCoronationReputationGainSensitivity);
		giveSubmissionBaseline = o.optInt("giveSubmissionBaseline", giveSubmissionBaseline);
		receiveSubmissionBaseline = o.optInt("receiveSubmissionBaseline", receiveSubmissionBaseline);
		nonAggressionPactLoyalty = o.optInt("nonAggressionPactLoyalty", nonAggressionPactLoyalty);
		defensivePactLoyalty = o.optInt("defensivePactLoyalty", defensivePactLoyalty);
		allianceLoyalty = o.optInt("allianceLoyalty", allianceLoyalty);
		doAnnexBase = o.optInt("doAnnexBase", doAnnexBase);
		doAnnexPerGrievance = o.optInt("doAnnexPerGrievance", doAnnexPerGrievance);
		becomeAnnexedBase = o.optInt("becomeAnnexedBase", becomeAnnexedBase);
		becomeAnnexedPerSize = o.optInt("becomeAnnexedPerSize", becomeAnnexedPerSize);
		becomeAnnexedPerGrievance = o.optInt("becomeAnnexedPerGrievance", becomeAnnexedPerGrievance);
		becomeAnnexedPerRep = o.optDouble("becomeAnnexedPerRep", becomeAnnexedPerRep);
		becomeAnnexedNotAdjacent = o.optInt("becomeAnnexedNotAdjacent", becomeAnnexedNotAdjacent);
		becomeAnnexedSharedGrievances = o.optInt("becomeAnnexedSharedGrievances", becomeAnnexedSharedGrievances);
		becomeAnnexedDefensivePact = o.optInt("becomeAnnexedDefensivePact", becomeAnnexedDefensivePact);
		becomeAnnexedAlliance = o.optInt("becomeAnnexedAlliance", becomeAnnexedAlliance);
	}
	
	public DiplomacyPersonality() {
		super("default");
	}
		
	public DiplomacyPersonality assumeOtherPersonality(Empire e) {
		if (assumeOtherEmpiresHaveThisPersonality != null) {
			return ofName(assumeOtherEmpiresHaveThisPersonality);
		} else {
			return ofName("human");//new DiplomacyPersonality(e);
		}
	}
	
	private DiplomacyPersonality(Empire basedOnRep) {
		super("default-assumed");
		reputationLossSensitivity *= basedOnRep.getReputation() / 50.0;
	}
	
	public static DiplomacyPersonality ofName(String name) {
		return ofName(DiplomacyPersonality.class, name);
	}
	
	public static DiplomacyPersonality pick(Bonus bonus, GuardedRandom r) {
		ArrayList<DiplomacyPersonality> l = new ArrayList<DiplomacyPersonality>();
		ArrayList<DiplomacyPersonality> all = new ArrayList<DiplomacyPersonality>();
		for (DiplomacyPersonality p : all(DiplomacyPersonality.class)) {
			if (p.forBonus == null) {
				all.add(p);
			} else if (p.forBonus == bonus) {
				l.add(p);
			}
		}
		if (l.isEmpty()) {
			l = all;
		}
		int totalWeight = 0;
		for (DiplomacyPersonality p : l) {
			totalWeight += p.weight;
		}
		int roll = r.nextInt(totalWeight);
		for (DiplomacyPersonality p : l) {
			roll -= p.weight;
			if (roll < 0) {
				return p;
			}
		}
		System.out.println("Failed to roll for diplo personality correctly.");
		return DiplomacyPersonality.ofName("default");
	}
}
