package com.zarkonnen.airships;

import java.util.List;

public class CityClaim {
	public final City city;
	public final Empire claimant;
	public final boolean originalOwner;
	public final int distanceToNearestTown;
	public final int fleetStrength;
	public final boolean previouslyAwardedClaim;
	
	public CityClaim(City city, Empire claimant, List<Fleet> involvedFleets, WorldMap m) {
		this.city = city;
		this.claimant = claimant;
		originalOwner = city.originalEmpire == claimant;
		int bestDist = 1000;
		for (City c2 : claimant.cities) {
			bestDist = Math.min(bestDist, (int) Math.sqrt((city.x - c2.x) * (city.x - c2.x) + (city.y - c2.y) * (city.y - c2.y)));
		}
		distanceToNearestTown = bestDist;
		int fs = 0;
		for (Fleet f : involvedFleets) {
			if (claimant.getFleets().contains(f)) {
				for (Airship s : f.actives) {
					fs += s.getCost();
				}
				for (Airship s : f.reserve) {
					fs += s.getCost();
				}
			}
		}
		fleetStrength = fs;
		previouslyAwardedClaim = claimant.previouslyAwardedClaim;
	}
	
	public int originalOwnerStrength() {
		return originalOwner ? EmpireStat.ORIGINAL_OWNER_CLAIM_STRENGTH.get(claimant.bonuses) : 0;
	}
	
	public int cityDistanceStrength() {
		return Math.max(0, EmpireStat.DISTANCE_TO_NEAREST_TOWN_CLAIM_BASE.get(claimant.bonuses) - (int) (distanceToNearestTown * EmpireStat.DISTANCE_TO_NEAREST_TOWN_CLAIM_MULTIPLIER.get(claimant.bonuses)));
	}
	
	public int fleetStrengthStrength() {
		return Math.min(EmpireStat.MAX_FLEET_STRENGTH_CLAIM_STRENGTH.get(claimant.bonuses), 1 + fleetStrength / EmpireStat.FLEET_STRENGTH_CLAIM_DIVIDER.get(claimant.bonuses));
	}
	
	public int previouslyAwardedClaimStrength() {
		return previouslyAwardedClaim ? EmpireStat.PREVIOUSLY_AWARDED_CLAIM_STRENGTH.get(claimant.bonuses) : 0;
	}
	
	public String explainOriginalOwnerStrength() {
		return originalOwner ? EmpireStat.ORIGINAL_OWNER_CLAIM_STRENGTH.explain(claimant.bonuses) : null;
	}
	
	public String explainPreviouslyAwardedClaimStrength() {
		return previouslyAwardedClaim ? EmpireStat.PREVIOUSLY_AWARDED_CLAIM_STRENGTH.explain(claimant.bonuses) : null;
	}
	
	public int strength() {
		return originalOwnerStrength() + cityDistanceStrength() + fleetStrengthStrength() + previouslyAwardedClaimStrength();
	}
}
