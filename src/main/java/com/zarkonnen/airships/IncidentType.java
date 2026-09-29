package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Utils.Pair;
import java.util.ArrayList;
import java.util.EnumSet;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.zarkonnen.catengine.util.Utils.p;
import static com.zarkonnen.airships.Lang._t;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;

public class IncidentType extends Loadable {
	public static class Filter {
		public boolean oneEmpireOnly;
		public boolean adjacent;
		public boolean tradeTreaty;
		public boolean noTradeTreaty;
		public boolean researchTreaty;
		public boolean noResearchTreaty;
		public boolean tributeAToB;
		public boolean tributeBToA;
		public boolean noTribute;
		public EnumSet<Relationship.Level> relationshipLevels;
		public int minRelationshipLevelAge;
		
		public boolean citiesAdjacent;
		public boolean citiesConnectedByLand;
		public boolean citiesConnectedByWater;
		
		public double abMinRelativeStrength;
		public double abMaxRelativeStrength;

		public BonusSet aBonuses = new BonusSet();
		public BonusSet bBonuses = new BonusSet();
		public BonusSet aNotBonuses = new BonusSet();
		public BonusSet bNotBonuses = new BonusSet();
		public int aMinRep = 0;
		public int aMaxRep = 100;
		public int bMinRep = 0;
		public int bMaxRep = 100;
		public int aMoney = 0;
		public int bMoney = 0;
		public int aMaxGrievances = 1000;
		public int aMinGrievances = 0;
		public int bMaxGrievances = 1000;
		public int bMinGrievances = 0;

		public boolean aStillOwned;
		public boolean aCity;
		public boolean aTown;
		public int aMinUnrest;
		public int aMaxUnrest;
		public boolean aOriginallyBelongsToOther;
		public boolean aWithFleet;
		public boolean aWithoutFleet;
		public boolean aWithOtherFleet;
		public boolean aWithoutOtherFleet;
		public HeroType aWithHero;
		public HeroType aWithoutHero;
		public HeroType aCityWithGovernor;
		public boolean aWithSpyFromOther;
		public int aWithMinInfiltrationFromOther = 0;
		public int aWithMaxInfiltrationFromOther = 100;
		public ArrayList<MonsterNestType> aWithNest;
		public boolean aWithEmptyNest;
		public boolean aNestHasRoad;
		public int aTechsForB;
		public int aMinTerritories;
		public CityUpgradeType aHasUpgrade;
		
		public boolean bStillOwned;
		public boolean bCity;
		public boolean bTown;
		public int bMinUnrest;
		public int bMaxUnrest;
		public boolean bOriginallyBelongsToOther;
		public boolean bWithFleet;
		public boolean bWithoutFleet;
		public boolean bWithOtherFleet;
		public boolean bWithoutOtherFleet;
		public HeroType bWithHero;
		public HeroType bWithoutHero;
		public HeroType bCityWithGovernor;
		public boolean bWithSpyFromOther;
		public int bWithMinInfiltrationFromOther = 0;
		public int bWithMaxInfiltrationFromOther = 100;
		public ArrayList<MonsterNestType> bWithNest;
		public boolean bWithEmptyNest;
		public boolean bNestHasRoad;
		public int bTechsForA;
		public int bMinTerritories;
		public CityUpgradeType bHasUpgrade;
		
		public boolean check(Empire a, Empire b, City ac, City bc, MonsterNest an, MonsterNest bn, WorldMap m) {
			return check(a, b, ac, bc, an, bn, m, null);
		}
		
		public boolean check(Empire a, Empire b, City ac, City bc, MonsterNest an, MonsterNest bn, WorldMap m, String[] reason) {
			if (oneEmpireOnly != (b == null)) {
				if (reason != null) {
					reason[0] = "oneEmpireOnly is set but empire B exists";
				}
				return false;
			}
			// Empire A and its bits
			if (m.toggles.contains(ConquestToggle.REPUTATION)) {
				if (a.getReputation() < aMinRep || a.getReputation() > aMaxRep) {
					if (reason != null) {
						reason[0] = "Empire A reputation of " + a.getReputation() + " is not between " + aMinRep + " and " + aMaxRep;
					}
					return false;
				}
			}
			if (!a.bonuses.containsAll(aBonuses)) {
				if (reason != null) {
					reason[0] = "Empire A is missing bonuses " + Airship.nameList(aBonuses.clone().removeAll(a.bonuses).list(), Lang.currentLocale);
				}
				return false;
			}
			if (a.bonuses.intersects(aNotBonuses)) {
				if (reason != null) {
					reason[0] = "Empire A has blocker bonuses " + Airship.nameList(aNotBonuses.clone().retainAll(a.bonuses).list(), Lang.currentLocale);
				}
				return false;
			}
			if (aMoney > a.getMoney() && a.playerControlled) {
				if (reason != null) {
					reason[0] = "Empire A only has $" + a.getMoney() + " but needs $" + aMoney;
				}
				return false;
			}
			
			if (aWithHero != null) {
				boolean has = false;
				for (Hero h : Hero.getHeroes(a, m)) {
					if (h.hired && h.type == aWithHero) {
						has = true;
						break;
					}
				}
				if (!has) {
					if (reason != null) {
						reason[0] = "Empire A does not have hero " + aWithHero.name;
					}
					return false;
				}
			}
			if (aWithoutHero != null) {
				for (Hero h : Hero.getHeroes(a, m)) {
					if (h.type == aWithoutHero) {
						if (reason != null) {
							reason[0] = "Empire A has hero " + aWithHero.name;
						}
						return false;
					}
				}
			}
			
			if (aMinTerritories > 0 && a.cities.size() < aMinTerritories) {
				if (reason != null) {
					reason[0] = "Empire A needs at least " + aMinTerritories + " towns and cities, but it only has " + a.cities.size();
				}
				return false;
			}
			
			if (ac == null) {
				if (aTown || aCity) {
					if (reason != null) {
						reason[0] = "There is no city A";
					}
					return false;
				}
			} else {
				if (ac.isTown && !aTown && aCity) {
					if (reason != null) {
						reason[0] = "City A is a town and shouldn't be";
					}
					return false;
				}
				if (!ac.isTown && !aCity && aTown) {
					if (reason != null) {
						reason[0] = "City A isn't a town and should be";
					}
					return false;
				}
				if (aStillOwned && !a.cities.contains(ac)) {
					if (reason != null) {
						reason[0] = "Empire A does not own City A (anymore)";
					}
					return false;
				}
				if (aMinUnrest > 0 || aMaxUnrest < 100) {
					int ur = ac.unrest(m, null);
					if (ur < aMinUnrest || ur > aMaxUnrest) {
						if (reason != null) {
							reason[0] = "City A unrest of " + ur + " is not between " + aMinUnrest + " and " + aMaxUnrest;
						}
						return false;
					}
				}
				if (aOriginallyBelongsToOther && ac.originalEmpire != b) {
					if (reason != null) {
						reason[0] = "City A should originally belong to Empire B, but it does not";
					}
					return false;
				}
				Fleet fl = m.getGarrison(ac);
				if (aWithFleet && fl == null) {
					if (reason != null) {
						reason[0] = "City A should have a garrison fleet but does not";
					}
					return false;
				}
				if (aWithoutFleet && fl != null) {
					if (reason != null) {
						reason[0] = "City A should not have a garrison fleet but it does";
					}
					return false;
				}
				Fleet otherFleet = m.getFleetAt(b, ac);
				if (aWithOtherFleet && otherFleet == null) {
					if (reason != null) {
						reason[0] = "City A should have a fleet from Empire B but does not";
					}
					return false;
				}
				if (aWithoutOtherFleet && otherFleet != null) {
					if (reason != null) {
						reason[0] = "City A should not have a fleet from Empire B but it does";
					}
					return false;
				}
				if (aWithSpyFromOther) {
					Spy s = b.getSpyFor(ac);
					if (s == null || s.infiltrationTimeout > 0) {
						if (reason != null) {
							reason[0] = "City A should have a spy from Empire B but does not";
						}
						return false;
					}
					if (s.networkLevel < aWithMinInfiltrationFromOther) {
						if (reason != null) {
							reason[0] = "The spy from Empire B in City A needs a network level of " + aWithMinInfiltrationFromOther + " but only has " + s.networkLevel;
						}
						return false;
					}
					if (s.networkLevel > aWithMaxInfiltrationFromOther) {
						if (reason != null) {
							reason[0] = "The spy from Empire B in City A needs a max network level of " + aWithMaxInfiltrationFromOther + " but it has " + s.networkLevel;
						}
						return false;
					}
				}
				Hero gov = Hero.get(ac, m);
				if (aCityWithGovernor != null && (gov == null || gov.type != aCityWithGovernor)) {
					if (reason != null) {
						reason[0] = "City A needs to have governor " + aCityWithGovernor.name;
					}
					return false;
				}
				if (aHasUpgrade != null && !ac.upgrades.contains(aHasUpgrade)) {
					if (reason != null) {
						reason[0] = "City A needs upgrade " + aHasUpgrade.name;
					}
					return false;
				}
			}

			if (an == null) {
				if (aWithEmptyNest || aWithNest != null) {
					if (reason != null) {
						reason[0] = "Nest A does not exist";
					}
					return false;
				}
			} else {
				if (aNestHasRoad && !m.roads[an.y][an.x]) {
					if (reason != null) {
						reason[0] = "Nest A does not have a road";
					}
					return false;
				}
				if (aWithEmptyNest && an.type != null) {
					if (reason != null) {
						reason[0] = "Nest A is not empty";
					}
					return false;
				}
				if (aWithNest != null && !aWithNest.contains(an.type)) {
					if (reason != null) {
						reason[0] = "Nest A has wrong type (" + (an.type == null ? "unoccupied" : an.type.name) + ")";
					}
					return false;
				}
				if (aWithNest != null && an.upgrading) {
					if (reason != null) {
						reason[0] = "Nest A is currently upgrading";
					}
					return false;
				}
			}
			if (!oneEmpireOnly) {
				if (b == null) { return false; } // Not needed logically but NetBeans stops complaining.
				if (bMoney > b.getMoney() && b.playerControlled) {
					if (reason != null) {
						reason[0] = "Empire B only has $" + b.getMoney() + " but needs $" + bMoney;
					}
					return false;
				}
				if (adjacent && !m.isAdjacent(a, b)) {
					if (reason != null) {
						reason[0] = "Empires A and B should be adjacent but aren't";
					}
					return false;
				}
				Relationship rel = m.getRelationship(a, b);
				if (rel.levelAge < minRelationshipLevelAge) {
					if (reason != null) {
						reason[0] = "The age of the relationship between A and B needs to be " + minRelationshipLevelAge + " but is only " + rel.levelAge;
					}
					return false;
				}
				if (tradeTreaty && !rel.tradeTreaty) {
					if (reason != null) {
						reason[0] = "Empires A and B need to have a trade treaty but don't";
					}
					return false;
				}
				if (noTradeTreaty && rel.tradeTreaty) {
					if (reason != null) {
						reason[0] = "Empires A and B need to not have a trade treaty but they do";
					}
					return false;
				}
				if (researchTreaty && !rel.researchTreaty) {
					if (reason != null) {
						reason[0] = "Empires A and B need to have a research treaty but don't";
					}
					return false;
				}
				if (noResearchTreaty && rel.researchTreaty) {
					if (reason != null) {
						reason[0] = "Empires A and B need to not have a research treaty but they do";
					}
					return false;
				}
				if (tributeAToB && !rel.getSendingTribute(a)) {
					if (reason != null) {
						reason[0] = "Empire A needs to be sending tribute to B, but it isn't";
					}
					return false;
				}
				if (tributeBToA && !rel.getSendingTribute(b)) {
					if (reason != null) {
						reason[0] = "Empire B needs to be sending tribute to A, but it isn't";
					}
					return false;
				}
				if (noTribute && rel.hasTribute()) {
					if (reason != null) {
						reason[0] = "Empires A and B need to have no tribute relationship, but they do";
					}
					return false;
				}
				if (relationshipLevels != null && !relationshipLevels.contains(rel.level)) {
					if (reason != null) {
						reason[0] = "Empires A and B have relationship " + rel.level.getName() + " but need one of " + Airship.nameList(new ArrayList<Relationship.Level>(relationshipLevels), Lang.currentLocale);
					}
					return false;
				}
				if (rel.getGrievances(a) > aMaxGrievances) {
					if (reason != null) {
						reason[0] = "Empire A can have a maximum of " + aMaxGrievances + " vs B, but it has " + rel.getGrievances(a);
					}
					return false;
				}
				if (rel.getGrievances(a) < aMinGrievances) {
					if (reason != null) {
						reason[0] = "Empire A must have a minimum of " + aMinGrievances + " vs B, but it has " + rel.getGrievances(a);
					}
					return false;
				}
				if (rel.getGrievances(b) > bMaxGrievances) {
					if (reason != null) {
						reason[0] = "Empire B can have a maximum of " + bMaxGrievances + " vs A, but it has " + rel.getGrievances(b) ;
					}
					return false;
				}
				if (rel.getGrievances(b) < bMinGrievances) {
					if (reason != null) {
						reason[0] = "Empire B must have a minimum of " + bMinGrievances + " vs A, but it has " + rel.getGrievances(b);
					}
					return false;
				}
				// Empire B and its bits
				if (m.toggles.contains(ConquestToggle.REPUTATION)) {
					if (b.getReputation() < bMinRep || b.getReputation() > bMaxRep) {
						if (reason != null) {
							reason[0] = "Empire B reputation of " + b.getReputation() + " is not between " + bMinRep + " and " + bMaxRep;
						}
						return false;
					}
				}
				if (!b.bonuses.containsAll(bBonuses)) {
					if (reason != null) {
						reason[0] = "Empire B is missing bonuses " + Airship.nameList(bBonuses.clone().removeAll(b.bonuses).list(), Lang.currentLocale);
					}
					return false;
				}
				if (b.bonuses.intersects(bNotBonuses)) {
					if (reason != null) {
						reason[0] = "Empire B has blocker bonuses " + Airship.nameList(bNotBonuses.clone().retainAll(b.bonuses).list(), Lang.currentLocale);
					}
					return false;
				}
				
				if (bWithHero != null) {
					boolean has = false;
					for (Hero h : Hero.getHeroes(b, m)) {
						if (h.hired && h.type == bWithHero) {
							has = true;
							break;
						}
					}
					if (!has) {
						if (reason != null) {
							reason[0] = "Empire B does not have hero " + bWithHero.name;
						}
						return false;
					}
				}
				if (bWithoutHero != null) {
					for (Hero h : Hero.getHeroes(b, m)) {
						if (h.type == bWithoutHero) {
							if (reason != null) {
								reason[0] = "Empire B has hero " + bWithHero.name;
							}
							return false;
						}
					}
				}
				
				if (bMinTerritories > 0 && b.cities.size() < bMinTerritories) {
					if (reason != null) {
						reason[0] = "Empire B needs at least " + bMinTerritories + " towns and cities, but it only has " + b.cities.size();
					}
					return false;
				}
				
				if (bc == null) {
					if (bTown || bCity) {
						if (reason != null) {
							reason[0] = "There is no city B";
						}
						return false;
					}
				} else {
					if (bc.isTown && !bTown && bCity) {
						if (reason != null) {
							reason[0] = "City B is a town and shouldn't be";
						}
						return false;
					}
					if (!bc.isTown && !bCity && bTown) {
						if (reason != null) {
							reason[0] = "City B isn't a town and should be";
						}
						return false;
					}
					if (bStillOwned && !b.cities.contains(bc)) {
						if (reason != null) {
							reason[0] = "Empire B does not own City B (anymore)";
						}
						return false;
					}
					if (bMinUnrest > 0 || bMaxUnrest < 100) {
						int ur = bc.unrest(m, null);
						if (ur < bMinUnrest || ur > bMaxUnrest) {
							if (reason != null) {
								reason[0] = "City B unrest of " + ur + " is not between " + bMinUnrest + " and " + bMaxUnrest;
							}
							return false;
						}
					}
					if (bOriginallyBelongsToOther && bc.originalEmpire != a) {
						if (reason != null) {
							reason[0] = "City B should originally belong to Empire A, but it does not";
						}
						return false;
					}
					Fleet fl = m.getGarrison(bc);
					if (bWithFleet && fl == null) {
						if (reason != null) {
							reason[0] = "City B should have a garrison fleet but does not";
						}
						return false;
					}
					if (bWithoutFleet && fl != null) {
						if (reason != null) {
							reason[0] = "City B should not have a garrison fleet but it does";
						}
						return false;
					}
					Fleet otherFleet = m.getFleetAt(a, bc);
					if (bWithOtherFleet && otherFleet == null) {
						if (reason != null) {
							reason[0] = "City B should have a fleet from Empire A but does not";
						}
						return false;
					}
					if (bWithoutOtherFleet && otherFleet != null) {
						if (reason != null) {
							reason[0] = "City B should not have a fleet from Empire A but it does";
						}
						return false;
					}
					if (bWithSpyFromOther) {
						Spy s = a.getSpyFor(bc);
						if (s == null || s.infiltrationTimeout > 0) {
							if (reason != null) {
								reason[0] = "City B should have a spy from Empire A but does not";
							}
							return false;
						}
						if (s.networkLevel < bWithMinInfiltrationFromOther) {
							if (reason != null) {
								reason[0] = "The spy from Empire A in City B needs a network level of " + bWithMinInfiltrationFromOther + " but only has " + s.networkLevel;
							}
							return false;
						}
						if (s.networkLevel > bWithMaxInfiltrationFromOther) {
							if (reason != null) {
								reason[0] = "The spy from Empire A in City B needs a max network level of " + bWithMaxInfiltrationFromOther + " but it has " + s.networkLevel;
							}
							return false;
						}
					}
					Hero gov = Hero.get(bc, m);
					if (bCityWithGovernor != null && (gov == null || gov.type != bCityWithGovernor)) {
						if (reason != null) {
							reason[0] = "City B needs to have governor " + bCityWithGovernor.name;
						}
						return false;
					}
					if (ac != null) {
						if (citiesAdjacent && !ac.isPhysicallyAdjacentTo(bc, m)) {
							if (reason != null) {
								reason[0] = "Cities A and B are not adjacent.";
							}
							return false;
						}
						if (citiesConnectedByLand && !m.connectedDirectlyByLand(ac, bc)) {
							if (reason != null) {
								reason[0] = "Cities A and B are not connected directly by road.";
							}
							return false;
						}
						if (citiesConnectedByWater && !m.connectedDirectlyBySea(ac, bc)) {
							if (reason != null) {
								reason[0] = "Cities A and B are not connected directly by a sea lane.";
							}
							return false;
						}
					}
					if (bHasUpgrade != null && !bc.upgrades.contains(bHasUpgrade)) {
						if (reason != null) {
							reason[0] = "City B needs upgrade " + bHasUpgrade.name;
						}
						return false;
					}
				}
				
				if (bn == null) {
					if (bWithEmptyNest || bWithNest != null) {
						if (reason != null) {
							reason[0] = "Nest B does not exist";
						}
						return false;
					}
				} else {
					if (bNestHasRoad && !m.roads[bn.y][bn.x]) {
						if (reason != null) {
							reason[0] = "Nest B does not have a road";
						}
						return false;
					}
					if (bWithEmptyNest && bn.type != null) {
						if (reason != null) {
							reason[0] = "Nest B is not empty";
						}
						return false;
					}
					if (bWithNest != null && !bWithNest.contains(bn.type)) {
						if (reason != null) {
							reason[0] = "Nest B has wrong type (" + (bn.type == null ? "unoccupied" : bn.type.name) + ")";
						}
						return false;
					}
					if (bWithNest != null && bn.upgrading) {
						if (reason != null) {
							reason[0] = "Nest A is currently upgrading";
						}
						return false;
					}
				}
				
				if (aTechsForB > 0) {
					int numTechs = 0;
					lp: for (int i = 0; i < a.techs.size(); i++) {
						Tech.Choice t = a.techs.get(i);
						if (!t.tech.visible(b)) { continue; }
						for (int j = 0; j < t.tech.choices.size(); j++) {
							Tech.Choice c = t.tech.choices.get(j);
							if (b.techs.contains(c)) {
								continue lp;
							}
						}
						numTechs++;
					}
					if (numTechs < aTechsForB) {
						if (reason != null) {
							reason[0] = "Empire A needs at least " + aTechsForB + " techs that B doesn't have, but it only has " + numTechs;
						}
						return false;
					}
				}
				
				if (bTechsForA > 0) {
					int numTechs = 0;
					lp: for (int i = 0; i < b.techs.size(); i++) {
						Tech.Choice t = b.techs.get(i);
						if (!t.tech.visible(a)) { continue; }
						for (int j = 0; j < t.tech.choices.size(); j++) {
							Tech.Choice c = t.tech.choices.get(j);
							if (a.techs.contains(c)) {
								continue lp;
							}
						}
						numTechs++;
					}
					if (numTechs < bTechsForA) {
						if (reason != null) {
							reason[0] = "Empire B needs at least " + bTechsForA + " techs that A doesn't have, but it only has " + numTechs;
						}
						return false;
					}
				}
								
				if (abMinRelativeStrength != 0) {
					if ((m.getFleetStrength(a) + 1000.0) / (m.getFleetStrength(b) + 1000.0) < abMinRelativeStrength) {
						if (reason != null) {
							reason[0] = "The fleet strength of A / B needs to be at least " + abMinRelativeStrength + ", but is " + (m.getFleetStrength(a) + 1000.0) / (m.getFleetStrength(b) + 1000.0);
						}
						return false;
					}
				}
				if (abMaxRelativeStrength != 0) {
					if ((m.getFleetStrength(a) + 1000.0) / (m.getFleetStrength(b) + 1000.0) > abMaxRelativeStrength) {
						if (reason != null) {
							reason[0] = "The fleet strength of A / B needs to be at most " + abMaxRelativeStrength + ", but is " + (m.getFleetStrength(a) + 1000.0) / (m.getFleetStrength(b) + 1000.0);
						}
						return false;
					}
				}
			}
						
			return true;
		}
		
		public boolean check(Incident in, WorldMap m) {
			return check(in.a, in.b, in.aCity, in.bCity, in.aNest, in.bNest, m, null);
		}
		
		public boolean check(Incident in, WorldMap m, String[] reason) {
			return check(in.a, in.b, in.aCity, in.bCity, in.aNest, in.bNest, m, reason);
		}
		
		public Filter(JSONObject o, boolean oneEmpireOnlyDefault) {
			oneEmpireOnly = o.optBoolean("oneEmpireOnly", oneEmpireOnlyDefault);
			adjacent = o.optBoolean("adjacent", false);
			tradeTreaty = o.optBoolean("tradeTreaty", false);
			noTradeTreaty = o.optBoolean("noTradeTreaty", false);
			researchTreaty = o.optBoolean("researchTreaty", false);
			noResearchTreaty = o.optBoolean("noResearchTreaty", false);
			tributeAToB = o.optBoolean("tributeAToB", false);
			tributeBToA = o.optBoolean("tributeBToA", false);
			noTribute = o.optBoolean("noTribute", false);
			citiesAdjacent = o.optBoolean("citiesAdjacent", false);
			citiesConnectedByLand = o.optBoolean("citiesConnectedByLand", false);
			citiesConnectedByWater = o.optBoolean("citiesConnectedByWater", false);
			aCity = o.optBoolean("aCity", false);
			aTown = o.optBoolean("aTown", false);
			aOriginallyBelongsToOther = o.optBoolean("aOriginallyBelongsToOther", false);
			aWithEmptyNest = o.optBoolean("aWithEmptyNest", false);
			aNestHasRoad = o.optBoolean("aNestHasRoad", false);
			aWithFleet = o.optBoolean("aWithFleet", false);
			aWithoutFleet = o.optBoolean("aWithoutFleet", false);
			aWithOtherFleet = o.optBoolean("aWithOtherFleet", false);
			aWithoutOtherFleet = o.optBoolean("aWithoutOtherFleet", false);
			bCity = o.optBoolean("bCity", false);
			bTown = o.optBoolean("bTown", false);
			bOriginallyBelongsToOther = o.optBoolean("bOriginallyBelongsToOther", false);
			bWithEmptyNest = o.optBoolean("bWithEmptyNest", false);
			bNestHasRoad = o.optBoolean("bNestHasRoad", false);
			bWithFleet = o.optBoolean("bWithFleet", false);
			bWithoutFleet = o.optBoolean("bWithoutFleet", false);
			bWithOtherFleet = o.optBoolean("bWithOtherFleet", false);
			bWithoutOtherFleet = o.optBoolean("bWithoutOtherFleet", false);
			aMinRep = o.optInt("aMinRep", 0);
			aMaxRep = o.optInt("aMaxRep", 100);
			bMinRep = o.optInt("bMinRep", 0);
			bMaxRep = o.optInt("bMaxRep", 100);
			aWithMinInfiltrationFromOther = o.optInt("aWithMinInfiltrationFromOther", 0);
			aWithMaxInfiltrationFromOther = o.optInt("aWithMaxInfiltrationFromOther", 100);
			bWithMinInfiltrationFromOther = o.optInt("bWithMinInfiltrationFromOther", 0);
			bWithMaxInfiltrationFromOther = o.optInt("bWithMaxInfiltrationFromOther", 100);
			aMinUnrest = o.optInt("aMinUnrest", 0);
			aMaxUnrest = o.optInt("aMaxUnrest", 100);
			bMinUnrest = o.optInt("bMinUnrest", 0);
			bMaxUnrest = o.optInt("bMaxUnrest", 100);
			aWithSpyFromOther = o.optBoolean("aWithSpyFromOther", false);
			bWithSpyFromOther = o.optBoolean("bWithSpyFromOther", false);
			aMoney = o.optInt("aMoney", 0);
			bMoney = o.optInt("bMoney", 0);
			aStillOwned = o.optBoolean("aStillOwned", false);
			bStillOwned = o.optBoolean("bStillOwned", false);
			aTechsForB = o.optInt("aTechsForB", 0);
			bTechsForA = o.optInt("bTechsForA", 0);
			if (aStillOwned) { aCity = true; aTown = true; }
			if (bStillOwned) { bCity = true; bTown = true; }
			minRelationshipLevelAge = o.optInt("minRelationshipLevelAge", 0);
			abMinRelativeStrength = o.optDouble("abMinRelativeStrength", 0);
			abMaxRelativeStrength = o.optDouble("abMaxRelativeStrength", 0);
			aMaxGrievances = o.optInt("aMaxGrievances", 1000);
			aMinGrievances = o.optInt("aMinGrievances", 0);
			bMaxGrievances = o.optInt("bMaxGrievances", 1000);
			bMinGrievances = o.optInt("bMinGrievances", 0);
			
			aHasUpgrade = o.has("aHasUpgrade") ? CityUpgradeType.ofName(o.getString("aHasUpgrade")) : null;
			bHasUpgrade = o.has("bHasUpgrade") ? CityUpgradeType.ofName(o.getString("bHasUpgrade")) : null;
			
			if (o.has("relationship")) {
				relationshipLevels = EnumSet.noneOf(Relationship.Level.class);
				JSONArray a = o.getJSONArray("relationship");
				for (int i = 0; i < a.length(); i++) {
					relationshipLevels.add(Relationship.Level.valueOf(a.getString(i)));
				}
			}
			
			if (o.has("notRelationship")) {
				relationshipLevels = EnumSet.allOf(Relationship.Level.class);
				JSONArray a = o.getJSONArray("notRelationship");
				for (int i = 0; i < a.length(); i++) {
					relationshipLevels.remove(Relationship.Level.valueOf(a.getString(i)));
				}
			}
			
			if (o.has("aBonuses")) {
				JSONArray a = o.getJSONArray("aBonuses");
				for (int i = 0; i < a.length(); i++) {
					aBonuses.set(Bonus.ofName(a.getString(i)).ordinal(), true);
				}
			}
			if (o.has("bBonuses")) {
				JSONArray a = o.getJSONArray("bBonuses");
				for (int i = 0; i < a.length(); i++) {
					bBonuses.set(Bonus.ofName(a.getString(i)).ordinal(), true);
				}
			}
			if (o.has("aNotBonuses")) {
				JSONArray a = o.getJSONArray("aNotBonuses");
				for (int i = 0; i < a.length(); i++) {
					aNotBonuses.set(Bonus.ofName(a.getString(i)).ordinal(), true);
				}
			}
			if (o.has("bNotBonuses")) {
				JSONArray a = o.getJSONArray("bNotBonuses");
				for (int i = 0; i < a.length(); i++) {
					bNotBonuses.set(Bonus.ofName(a.getString(i)).ordinal(), true);
				}
			}
			if (o.has("aWithHero")) {
				aWithHero = HeroType.ofName(o.getString("aWithHero"));
			}
			if (o.has("bWithHero")) {
				bWithHero = HeroType.ofName(o.getString("bWithHero"));
			}
			if (o.has("aWithoutHero")) {
				aWithoutHero = HeroType.ofName(o.getString("aWithoutHero"));
			}
			if (o.has("bWithoutHero")) {
				bWithoutHero = HeroType.ofName(o.getString("bWithoutHero"));
			}
			if (o.has("aCityWithGovernor")) {
				aCityWithGovernor = HeroType.ofName(o.getString("aCityWithGovernor"));
			}
			if (o.has("bCityWithGovernor")) {
				bCityWithGovernor = HeroType.ofName(o.getString("bCityWithGovernor"));
			}
			if (o.has("aWithNest")) {
				aWithNest = new ArrayList<MonsterNestType>();
				JSONArray a = o.getJSONArray("aWithNest");
				for (int i = 0; i < a.length(); i++) {
					aWithNest.add(MonsterNestType.ofName(a.getString(i)));
				}
			}
			if (o.has("bWithNest")) {
				bWithNest = new ArrayList<MonsterNestType>();
				JSONArray a = o.getJSONArray("bWithNest");
				for (int i = 0; i < a.length(); i++) {
					bWithNest.add(MonsterNestType.ofName(a.getString(i)));
				}
			}
		}
	}
	
	public static class Event {
		// At top level, what's needed to trigger the event. In an option, what's needed for the option.
		// If no options are valid, the incident is cancelled.
		public Filter filter;
		public String title;
		public String text;
		public boolean forB;
		public String noticeTitle;
		public String noticeText;
		
		public boolean hideOtherEmpire; // Make it look as if we were empire A and forB = false.
		public int grievances;
		public int otherGrievances;
		public int rep;
		public int otherRep;
		public int money;
		public int otherMoney;
		public Tech.Choice tech;
		public Bonus bonus;
		public Bonus otherBonus;
		public int research;
		public int changeInfiltrationLevel;
		public boolean removeSpy;
		public boolean upgradeMyNest;
		public boolean instantlyUpgradeMyNest;
		public MonsterNestType spawnMyNest;
		public boolean removeMyNest;
		public boolean removeOtherNest;
		public Relationship.Level relationshipLevel;
		public String warDeclarer;
		public boolean tradeTreaty;
		public boolean cancelTradeTreaty;
		public boolean researchTreaty;
		public boolean cancelResearchTreaty;
		public boolean tributeAToB;
		public boolean tributeBToA;
		public boolean cancelTributeAToB;
		public boolean cancelTributeBToA;
		public boolean gainCity;
		public boolean gainGarrisonWithCity;
		public boolean loseCity;
		public boolean loseGarrisonWithCity;
		public boolean techBoost;
		public boolean fleeFleetAtCity;
		public boolean fleeFleetAtOtherCity;
		public boolean fleeOtherFleetAtCity;
		public boolean fleeOtherFleetAtOtherCity;
		public boolean globalUnrest;
		public int unrest;
		public int unrestTimeout;
		public String unrestReason;
		public boolean otherGlobalUnrest;
		public int otherUnrest;
		public int otherUnrestTimeout;
		public String otherUnrestReason;
		public int cityIncomeChangePercent;
		public int otherCityIncomeChangePercent;
		public HeroType gainHeroFromTemplate;
		public int cityEconomicDamage;
		public int otherCityEconomicDamage;
		
		public String addConstructionName;
		public Airship addConstruction;
		public int numAddConstructions;
		public boolean addConstructionIsBonusConstruction;
		public String otherAddConstructionName;
		public Airship otherAddConstruction;
		public int otherNumAddConstructions;
		public boolean otherAddConstructionIsBonusConstruction;
		
		// Tags for hero reactions
		public ArrayList<String> tags = new ArrayList<String>();
		public ArrayList<String> otherTags = new ArrayList<String>();
		
		public ArrayList<Option> options = new ArrayList<Option>();
		
		public String mapPin; // One of aCity, bCity, aNest, bNest
				
		public Event(JSONObject o) {
			// By default, we alternate between messages for A and B unless there is no empire B.
			this(o, false, !(o.has("filter") && o.getJSONObject("filter").optBoolean("oneEmpireOnly", false)), false);
		}
		
		public Event(JSONObject o, boolean forB, boolean alternateForB, boolean oneEmpireOnlyDefault) {
			if (o.has("filter")) {
				filter = new Filter(o.getJSONObject("filter"), oneEmpireOnlyDefault);
				oneEmpireOnlyDefault = filter.oneEmpireOnly;
			}
			title = o.getString("title");
			text = o.getString("text");
			noticeTitle = o.optString("noticeTitle", null);
			noticeText = o.optString("noticeText", null);
			this.forB = o.optBoolean("forB", forB);
			if (o.has("forB") && o.getBoolean("forB") == forB) {
				System.out.println(title + ": pointless forB = " + forB);
			}
			hideOtherEmpire = o.optBoolean("hideOtherEmpire", false);
			grievances = o.optInt("grievances", 0);
			otherGrievances = o.optInt("otherGrievances", 0);
			rep = o.optInt("rep", 0);
			otherRep = o.optInt("otherRep", 0);
			money = o.optInt("money", 0);
			otherMoney = o.optInt("otherMoney", 0);
			if (o.has("tech")) {
				tech = Tech.choiceOfName(o.getString("tech"));
			}
			if (o.has("bonus")) {
				bonus = Bonus.ofName(o.getString("bonus"));
			}
			if (o.has("otherBonus")) {
				otherBonus = Bonus.ofName(o.getString("otherBonus"));
			}
			research = o.optInt("research", 0);
			changeInfiltrationLevel = o.optInt("changeInfiltrationLevel", 0);
			if (o.has("spawnMyNest")) {
				spawnMyNest = MonsterNestType.ofName(o.getString("spawnMyNest"));
			}
			if (o.has("relationshipLevel")) {
				relationshipLevel = Relationship.Level.valueOf(o.getString("relationshipLevel"));
				if (relationshipLevel == Relationship.Level.WAR) {
					warDeclarer = o.getString("warDeclarer");
					//System.out.println((this.forB ? "b: " : "a: ") + text + " wd: " + warDeclarer);
				}
			}
			removeSpy = o.optBoolean("removeSpy", false);
			upgradeMyNest = o.optBoolean("upgradeMyNest", false);
			instantlyUpgradeMyNest = o.optBoolean("instantlyUpgradeMyNest", false);
			removeMyNest = o.optBoolean("removeMyNest", false);
			removeOtherNest = o.optBoolean("removeOtherNest", false);
			/*if (removeMyNest && this.forB) {
				System.out.println(title + ": removeMyNest forB");
			}
			if (removeOtherNest && !this.forB) {
				System.out.println(title + ": removeOtherMyNest forA");
			}*/
			tradeTreaty = o.optBoolean("tradeTreaty", false);
			cancelTradeTreaty = o.optBoolean("cancelTradeTreaty", false);
			researchTreaty = o.optBoolean("researchTreaty", false);
			cancelResearchTreaty = o.optBoolean("cancelResearchTreaty", false);
			tributeAToB = o.optBoolean("tributeAToB", false);
			tributeBToA = o.optBoolean("tributeBToA", false);
			cancelTributeAToB = o.optBoolean("cancelTributeAToB", false);
			cancelTributeBToA = o.optBoolean("cancelTributeBToA", false);
			gainCity = o.optBoolean("gainCity", false);
			gainGarrisonWithCity = o.optBoolean("gainGarrisonWithCity", false);
			loseCity = o.optBoolean("loseCity", false);
			loseGarrisonWithCity = o.optBoolean("loseGarrisonWithCity", false);
			fleeFleetAtCity = o.optBoolean("fleeFleetAtCity", false);
			fleeFleetAtOtherCity = o.optBoolean("fleeFleetAtOtherCity", false);
			fleeOtherFleetAtCity = o.optBoolean("fleeOtherFleetAtCity", false);
			fleeOtherFleetAtOtherCity = o.optBoolean("fleeOtherFleetAtOtherCity", false);
			if (o.has("options")) {
				JSONArray a = o.getJSONArray("options");
				for (int i = 0; i < a.length(); i++) {
					options.add(new Option(a.getJSONObject(i), forB ^ alternateForB, alternateForB, oneEmpireOnlyDefault));
				}
			} else {
				options.add(new Option());
			}
			mapPin = o.optString("mapPin", null);
			techBoost = o.optBoolean("techBoost", false);
			globalUnrest = o.optBoolean("globalUnrest", false);
			unrest = o.optInt("unrest", 0);
			unrestTimeout = o.optInt("unrestTimeout", 0);
			unrestReason = o.optString("unrestReason", null);
			otherGlobalUnrest = o.optBoolean("otherGlobalUnrest", false);
			otherUnrest = o.optInt("otherUnrest", 0);
			otherUnrestTimeout = o.optInt("otherUnrestTimeout", 0);
			otherUnrestReason = o.optString("otherUnrestReason", null);
			cityIncomeChangePercent = o.optInt("cityIncomeChangePercent", 0);
			otherCityIncomeChangePercent = o.optInt("otherCityIncomeChangePercent", 0);
			cityEconomicDamage = o.optInt("cityEconomicDamage", 0);
			otherCityEconomicDamage = o.optInt("otherCityEconomicDamage", 0);
			if (o.has("gainHeroFromTemplate")) {
				gainHeroFromTemplate = HeroType.ofName(o.getString("gainHeroFromTemplate"));
			}
			if (o.has("tags")) {
				JSONArray a = o.getJSONArray("tags");
				for (int i = 0; i < a.length(); i++) {
					tags.add(a.getString(i));
				}
			}
			if (o.has("otherTags")) {
				JSONArray a = o.getJSONArray("otherTags");
				for (int i = 0; i < a.length(); i++) {
					otherTags.add(a.getString(i));
				}
			}
			
			addConstructionName = o.optString("addConstruction", null);
			numAddConstructions = o.optInt("numAddConstructions", 1);
			addConstructionIsBonusConstruction = o.optBoolean("addConstructionIsBonusConstruction", false);
			otherAddConstructionName = o.optString("otherAddConstruction", null);
			otherNumAddConstructions = o.optInt("otherNumAddConstructions", 1);
			otherAddConstructionIsBonusConstruction = o.optBoolean("otherAddConstructionIsBonusConstruction", false);
		}
		
		public boolean createsHero() {
			if (gainHeroFromTemplate != null) {
				return true;
			}
			for (int i = 0; i < options.size(); i++) {
				Option o = options.get(i);
				if (o.event != null && o.event.createsHero()) {
					return true;
				}
			}
			return false;
		}
		
		public Airship getAddConstruction() {
			if (addConstructionName == null) { return null; }
			if (addConstruction == null) {
				addConstruction = getConstruction(addConstructionName, addConstructionIsBonusConstruction);
			}
			return addConstruction;
		}
		
		public Airship getOtherAddConstruction() {
			if (otherAddConstructionName == null) { return null; }
			if (otherAddConstruction == null) {
				otherAddConstruction = getConstruction(otherAddConstructionName, otherAddConstructionIsBonusConstruction);
			}
			return otherAddConstruction;
		}
		
		private Airship getConstruction(String name, boolean isBonus) {
			ArrayList<Mod> mods = Mod.getEnabledMods();
			for (int i = mods.size() - 1; i >= 0; i--) {
				File f = new File(new File(mods.get(i).dir, "bonusConstructions"), name + ".json");
				if (f.exists()) {
					try {
						Airship s = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
						s.isBonusConstruction = isBonus;
						return s;
					} catch (IOException e2) {
						e2.printStackTrace();
					} catch (JSONException e2) {
						e2.printStackTrace();
					}
				}
			}
			ArrayList<Expansion> exp = Expansion.enableds();
			for (int i = exp.size() - 1; i >= 0; i--) {
				File f = new File(new File(exp.get(i).getDataDir(), "bonusConstructions"), name + ".json");
				if (f.exists()) {
					try {
						Airship s = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
						s.isBonusConstruction = isBonus;
						return s;
					} catch (IOException e2) {
						e2.printStackTrace();
					} catch (JSONException e2) {
						e2.printStackTrace();
					}
				}
			}
			File f = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "bonusConstructions"), name + ".json");
			if (f.exists()) {
				try {
					Airship s = new Airship(new JSONObject(FileUtils.readFileToString(f, "UTF-8")));
					s.isBonusConstruction = isBonus;
					return s;
				} catch (IOException e2) {
					e2.printStackTrace();
				} catch (JSONException e2) {
					e2.printStackTrace();
				}
			}
			
			return null;
		}
		
		private String[] subs(Incident in, boolean other) {
			if (forB ^ other) {
				return new String[] {
					in.b == null ? "?" : in.b.getName(),
					in.a == null ? "?" : in.a.getName(),
					in.bCity == null ? "?" : in.bCity.getDisplayName(),
					in.aCity == null ? "?" : in.aCity.getDisplayName(),
					in.bNest == null ? "?" : in.bNest.getDisplayName(),
					in.aNest == null ? "?" : in.aNest.getDisplayName()
				};
			} else {
				return new String[] {
					in.a == null ? "?" : in.a.getName(),
					in.b == null ? "?" : in.b.getName(),
					in.aCity == null ? "?" : in.aCity.getDisplayName(),
					in.bCity == null ? "?" : in.bCity.getDisplayName(),
					in.aNest == null ? "?" : in.aNest.getDisplayName(),
					in.bNest == null ? "?" : in.bNest.getDisplayName()
				};
			}
		}
		
		public String getTitle(Incident in, Empire e) {
			return _t(title, (Object[]) subs(in, false));
		}
		
		public String getNoticeTitle(Incident in, Empire e) {
			return _t(noticeTitle, (Object[]) subs(in, true));
		}
		
		public String getText(Incident in, Empire e, CampaignWorld cw) {
			StringBuilder sb = new StringBuilder();
			Empire other = e == in.a ? in.b : in.a;
			City otherCity = e == in.a ? in.bCity : in.aCity;
			City city = e == in.a ? in.aCity : in.bCity;
			MonsterNest myNest = e == in.a ? in.aNest : in.bNest;
			if (cityIncomeChangePercent > 0) {
				sb.append("\n").append(_t("city_income_change", "+ " + cityIncomeChangePercent + "%", city.getDisplayName()));
			}
			if (cityIncomeChangePercent < 0) {
				sb.append("\n").append(_t("city_income_change", "- " + -cityIncomeChangePercent + "%", city.getDisplayName()));
			}
			if (cityEconomicDamage > 0) {
				sb.append("\n+ ").append(cityEconomicDamage).append(" ").append(_t("econ_dmg_change", city.getDisplayName()));
			}
			if (cityEconomicDamage < 0) {
				sb.append("\n- ").append(-cityEconomicDamage).append(" ").append(_t("econ_dmg_change", city.getDisplayName()));
			}
			if (grievances != 0) {
				sb.append("\n").append(_t("x_grievances_towards_y", grievances > 0 ? "+ " + grievances : "- " + -grievances, other.getName()));
			}
			if (otherGrievances != 0) {
				sb.append("\n").append(_t("x_grievances_from_y", otherGrievances > 0 ? "+ " + otherGrievances : "- " + -otherGrievances, other.getName()));
			}
			if (rep > 0) {
				sb.append("\n+ ").append(rep).append(" ").append(_t("Reputation"));
			}
			if (rep < 0) {
				sb.append("\n- ").append(-rep).append(" ").append(_t("Reputation"));
			}
			if (otherRep > 0) {
				sb.append("\n+ ").append(otherRep).append(" ").append(_t("reputation_for_x", other.getName()));
			}
			if (otherRep < 0) {
				sb.append("\n- ").append(-otherRep).append(" ").append(_t("reputation_for_x", other.getName()));
			}
			if (money > 0) {
				sb.append("\n+ $").append(money);
			}
			if (money < 0) {
				sb.append("\n- $").append(-money);
			}
			if (research > 0) {
				sb.append("\n+ ").append(research).append(" ").append(_t("Research"));
			}
			if (bonus != null) {
				sb.append("\n+ ").append(bonus.getName());
				if (!bonus.getName().equals(bonus.getDesc())) {
					sb.append(": ").append(bonus.getDesc());
				}
				for (CityUpgradeType cut : all(CityUpgradeType.class)) {
					if (cut.requires.contains[bonus.ordinal()]) {
						sb.append("\n  ").append(_t(cut.forTown ? "enables_town_upgrade_x" : "enables_city_upgrade_x", cut.getName()));
					}
				}
			}
			if (tech != null) {
				sb.append("\n+ ").append(_t("Technology_")).append(_t("tech_" + tech.name));
			}
			if (spawnMyNest != null) {
				sb.append("\n+ ").append(spawnMyNest.getName());
			}
			if (removeMyNest || removeOtherNest) {
				sb.append("\n").append(_t("nest_cleared"));
			}
			if (upgradeMyNest && myNest != null && myNest.type != null) {
				sb.append("\n").append(_t("nest_upgrading", myNest.getName()));
			}
			if (instantlyUpgradeMyNest && myNest != null && myNest.type != null) {
				sb.append("\n").append(_t("nest_upgraded", myNest.getName()));
			}
			if (removeSpy && otherCity != null) {
				sb.append("\n").append(_t("spy_removed_from_x", otherCity.getDisplayName()));
			}
			if (changeInfiltrationLevel != 0 && otherCity != null) {
				sb.append("\n").append(_t("x_spy_level_change_at_y", (changeInfiltrationLevel > 0 ? ("+ " + changeInfiltrationLevel) : ("- " + (-changeInfiltrationLevel))), otherCity.getDisplayName()));
			}
			if (tradeTreaty) {
				sb.append("\n").append(_t("Trade_Treaty_with", other.getName()));
			}
			if (cancelTradeTreaty) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Trade_Treaty_with", other.getName()));
			}
			if (researchTreaty) {
				sb.append("\n").append(_t("Research_Treaty_with", other.getName()));
			}
			if (cancelResearchTreaty) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Research_Treaty_with", other.getName()));
			}
			if ((tributeAToB && e == in.a) || (tributeBToA && e == in.b)) {
				sb.append("\n").append(_t("Paying_Tribute_to", other.getName()));
			}
			if ((tributeAToB && e == in.b) || (tributeBToA && e == in.a)) {
				sb.append("\n").append(_t("Receiving_Tribute_from", other.getName()));
			}
			if ((cancelTributeAToB && e == in.a) || (cancelTributeBToA && e == in.b)) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Paying_Tribute_to", other.getName()));
			}
			if ((cancelTributeAToB && e == in.b) || (cancelTributeBToA && e == in.a)) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Receiving_Tribute_from", other.getName()));
			}
			if (relationshipLevel != null) {
				sb.append("\n").append(_t("relationship_with_" + relationshipLevel.name(), other.getName()));
			}
			if (gainCity) {
				sb.append("\n").append(_t("Receive_city_x", otherCity.getDisplayName()));
			}
			if (loseCity) {
				sb.append("\n").append(_t("Give_city_x", city.getDisplayName()));
			}
			if (getAddConstruction() != null) {
				if (numAddConstructions == 1) {
					sb.append("\n+ ").append(getAddConstruction().getName());
				} else {
					sb.append("\n+ ").append(numAddConstructions).append("x ").append(getAddConstruction().getName());
				}
			}
			if (techBoost) {
				sb.append("\n").append(_t("research_boosted"));
				if (e.techsBoosted.containsKey(this)) {
					for (Tech.Choice c : e.techsBoosted.get(this)) {
						sb.append("\n+ 30% ").append(c.getName());
					}
				}
			}
			if (unrestReason != null) {
				if (globalUnrest) {
					sb.append("\n").append(_t("global_unrest", _t(unrestReason), (unrest > 0 ? "+" : "") + unrest, cw.describeTime(unrestTimeout)));
				} else {
					sb.append("\n").append(_t("local_unrest", _t(unrestReason), (unrest > 0 ? "+" : "") + unrest, city.getDisplayName(), cw.describeTime(unrestTimeout)));
				}
			}
			if (gainHeroFromTemplate != null) {
				sb.append("\n+ ").append(_t("hero_type_" + gainHeroFromTemplate.name));
			}
			if (e.heroAppendix != null) {
				sb.append(e.heroAppendix);
			}
			if (sb.length() == 0) {
				return _t(text, (Object[]) subs(in, false));
			} else {
				return _t(text, (Object[]) subs(in, false)) + "\n" + sb.toString();
			}
		}
		
		public String getNoticeText(Incident in, Empire e, String additionalEffects) {
			StringBuilder sb = new StringBuilder(additionalEffects);
			Empire other = e == in.a ? in.b : in.a;
			City city = e == in.a ? in.aCity : in.bCity;
			City otherCity = e == in.a ? in.bCity : in.aCity;
			MonsterNest otherNest = e == in.a ? in.bNest : in.aNest;
			if (otherCityIncomeChangePercent > 0) {
				sb.append("\n").append(_t("city_income_change", "+ " + otherCityIncomeChangePercent + "%", city.getDisplayName()));
			}
			if (otherCityIncomeChangePercent < 0) {
				sb.append("\n").append(_t("city_income_change", "- " + -otherCityIncomeChangePercent + "%", city.getDisplayName()));
			}
			if (otherCityEconomicDamage > 0) {
				sb.append("\n+ ").append(otherCityEconomicDamage).append(" ").append(_t("econ_dmg_change", city.getDisplayName()));
			}
			if (otherCityEconomicDamage < 0) {
				sb.append("\n- ").append(-otherCityEconomicDamage).append(" ").append(_t("econ_dmg_change", city.getDisplayName()));
			}
			if (otherGrievances != 0) {
				sb.append("\n").append(_t("x_grievances_towards_y", otherGrievances > 0 ? "+ " + otherGrievances : "- " + -otherGrievances, other.getName()));
			}
			if (grievances != 0) {
				sb.append("\n").append(_t("x_grievances_from_y", grievances > 0 ? "+ " + grievances : "- " + -grievances, other.getName()));
			}
			if (otherRep > 0) {
				sb.append("\n+ ").append(otherRep).append(" ").append(_t("Reputation"));
			}
			if (otherRep < 0) {
				sb.append("\n- ").append(-otherRep).append(" ").append(_t("Reputation"));
			}
			if (rep > 0) {
				sb.append("\n+ ").append(rep).append(" ").append(_t("reputation_for_x", other.getName()));
			}
			if (rep < 0) {
				sb.append("\n- ").append(-rep).append(" ").append(_t("reputation_for_x", other.getName()));
			}
			if (otherMoney > 0) {
				sb.append("\n+ $").append(otherMoney);
			}
			if (otherMoney < 0) {
				sb.append("\n- $").append(-otherMoney);
			}
			if (otherBonus != null) {
				sb.append("\n+ ").append(otherBonus.getName());
				if (!otherBonus.getName().equals(otherBonus.getDesc())) {
					sb.append(": ").append(otherBonus.getDesc());
				}
				for (CityUpgradeType cut : all(CityUpgradeType.class)) {
					if (cut.requires.contains[otherBonus.ordinal()]) {
						sb.append("\n  ").append(_t(cut.forTown ? "enables_town_upgrade_x" : "enables_city_upgrade_x", cut.getName()));
					}
				}
			}
			if (removeMyNest || removeOtherNest) {
				sb.append("\n").append(_t("nest_cleared"));
			}
			if (upgradeMyNest && otherNest != null && otherNest.type != null) {
				sb.append("\n").append(_t("nest_upgrading", otherNest.getName()));
			}
			if (instantlyUpgradeMyNest && otherNest != null && otherNest.type != null) {
				sb.append("\n").append(_t("nest_upgraded", otherNest.getName()));
			}
			if (removeSpy && otherCity != null) {
				sb.append("\n").append(_t("spy_removed_from_x", otherCity.getDisplayName()));
			}
			if (tradeTreaty) {
				sb.append("\n").append(_t("Trade_Treaty_with", other.getName()));
			}
			if (cancelTradeTreaty) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Trade_Treaty_with", other.getName()));
			}
			if (researchTreaty) {
				sb.append("\n").append(_t("Research_Treaty_with", other.getName()));
			}
			if (cancelResearchTreaty) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Research_Treaty_with", other.getName()));
			}
			if ((tributeAToB && e == in.a) || (tributeBToA && e == in.b)) {
				sb.append("\n").append(_t("Paying_Tribute_to", other.getName()));
			}
			if ((tributeAToB && e == in.b) || (tributeBToA && e == in.a)) {
				sb.append("\n").append(_t("Receiving_Tribute_from", other.getName()));
			}
			if ((cancelTributeAToB && e == in.a) || (cancelTributeBToA && e == in.b)) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Paying_Tribute_to", other.getName()));
			}
			if ((cancelTributeAToB && e == in.b) || (cancelTributeBToA && e == in.a)) {
				sb.append("\n").append(_t("cancelled_")).append(_t("Receiving_Tribute_from", other.getName()));
			}
			if (relationshipLevel != null) {
				sb.append("\n").append(_t("relationship_with_" + relationshipLevel.name(), other.getName()));
			}
			if (gainCity) {
				sb.append("\n").append(_t("Give_city_x", city.getDisplayName()));
			}
			if (loseCity) {
				sb.append("\n").append(_t("Receive_city_x", otherCity.getDisplayName()));
			}
			if (getOtherAddConstruction() != null) {
				if (otherNumAddConstructions == 1) {
					sb.append("\n+ ").append(getOtherAddConstruction().getName());
				} else {
					sb.append("\n+ ").append(otherNumAddConstructions).append("x ").append(getOtherAddConstruction().getName());
				}
			}
			if (otherUnrestReason != null) {
				if (otherGlobalUnrest) {
					sb.append("\n").append(_t("global_unrest", _t(otherUnrestReason), (otherUnrest > 0 ? "+" : "") + otherUnrest, WorldMap.describeTime(otherUnrestTimeout, e.bonuses)));
				} else {
					sb.append("\n").append(_t("local_unrest", _t(otherUnrestReason), (otherUnrest > 0 ? "+" : "") + otherUnrest, otherCity.getDisplayName(), WorldMap.describeTime(otherUnrestTimeout, e.bonuses)));
				}
			}
			if (sb.length() == 0) {
				return _t(noticeText, (Object[]) subs(in, true));
			} else {
				return _t(noticeText, (Object[]) subs(in, true)) + "\n" + sb.toString();
			}
		}
		
		public ArrayList<EventOptionEntry> getOptions(Incident in, Empire e, WorldMap m) {
			ArrayList<EventOptionEntry> l = new ArrayList<EventOptionEntry>();
			for (int i = 0; i < options.size(); i++) {
				String[] ref = SimplePref.EDITOR_TOOLS.get() ? new String[] { null } : null;
				l.add(new EventOptionEntry(
						_t(options.get(i).label, (Object[]) subs(in, false)),
						i,
						options.get(i).event == null || options.get(i).event.filter == null || options.get(i).event.filter.check(in, m, ref),
						ref == null ? null : ref[0]));
			}
			return l;
		}
	}
	
	public static final class EventOptionEntry {
		public final String text;
		public final String debugText;
		public final int index;
		public final boolean enabled;

		public EventOptionEntry(String text, int index, boolean enabled, String debugText) {
			this.text = text;
			this.index = index;
			this.enabled = enabled;
			this.debugText = debugText;
		}
	}
	
	public static class Option {
		public String label;
		public Event event;
		
		public Option() {
			label = "OK";
			event = null;
		}
		
		public Option(JSONObject o, boolean forB, boolean alternateForB, boolean oneEmpireOnlyDefault) {
			label = o.getString("label");
			if (o.has("text")) {
				event = new Event(o, forB, alternateForB, oneEmpireOnlyDefault);
			}
			aiBonus = o.has("aiBonus") ? Bonus.ofName(o.getString("aiBonus")) : null;
			aiOtherBonus = o.has("aiOtherBonus") ? Bonus.ofName(o.getString("aiOtherBonus")) : null;
			aiBase = o.optInt("aiBase", 0);
			aiAboutToWin = o.optInt("aiAboutToWin", 0);
			aiOtherAboutToWin = o.optInt("aiOtherAboutToWin", 0);
			aiBonusAmount = o.optInt("aiBonusAmount", 0);
			aiOtherBonusAmount = o.optInt("aiOtherBonusAmount", 0);
			aiOtherRep = o.optDouble("aiOtherRep", 0);
			aiRepLossSensitivity = o.optDouble("aiRepLossSensitivity", 0);
			aiWarScore = o.optDouble("aiWarScore", 0);
			aiPeaceScore = o.optDouble("aiPeaceScore", 0);
			aiAllianceScore = o.optDouble("aiAllianceScore", 0);
			aiGrievances = o.optDouble("aiGrievances", 0);
			aiOtherGrievances = o.optDouble("aiOtherGrievances", 0);
			aiRelativeIncome = o.optDouble("aiRelativeIncome", 0);
			aiRelativeStrength = o.optDouble("aiRelativeStrength", 0);
			aiHonest = o.optDouble("aiHonest", 0);
			aiJust = o.optDouble("aiJust", 0);
			aiAggressive = o.optDouble("aiAggressive", 0);
			aiWar = o.optInt("aiWar", 0);
			aiTruce = o.optInt("aiTruce", 0);
			aiPeace = o.optInt("aiPeace", 0);
			aiNonAggressionPact = o.optInt("aiNonAggressionPact", 0);
			aiDefensivePact = o.optInt("aiDefensivePact", 0);
			aiAlliance = o.optInt("aiAlliance", 0);
		}
		
		public Bonus aiBonus;
		public Bonus aiOtherBonus;
		public int aiBase;
		public int aiAboutToWin;
		public int aiOtherAboutToWin;
		public int aiBonusAmount;
		public int aiOtherBonusAmount;
		public double aiOtherRep;
		public double aiRepLossSensitivity;
		public double aiWarScore;
		public double aiPeaceScore;
		public double aiAllianceScore;
		public double aiGrievances;
		public double aiOtherGrievances;
		public double aiRelativeIncome;
		public double aiRelativeStrength;
		public double aiHonest;
		public double aiJust;
		public double aiAggressive;
		public int aiWar;
		public int aiTruce;
		public int aiPeace;
		public int aiNonAggressionPact;
		public int aiDefensivePact;
		public int aiAlliance;
		
		public int evaluate(Empire me, Empire other, WorldMap wm) {
			DiplomacyAI ai = me.diplomacyAI;
			DiplomacyPersonality p = me.diplomacyAI.personality;
			int amt = aiBase;
			if (aiBonus != null && me.bonuses.contains[aiBonus.ordinal()]) {
				amt += aiBonusAmount;
			}
			if (other != null && aiOtherBonus != null && other.bonuses.contains[aiOtherBonus.ordinal()]) {
				amt += aiOtherBonusAmount;
			}
			if (aiAboutToWin != 0 && wm.isAboutToWin(me)) {
				amt += aiAboutToWin;
			}
			if (aiOtherAboutToWin != 0 && wm.isAboutToWin(other)) {
				amt += aiOtherAboutToWin;
			}
			if (wm.toggles.contains(ConquestToggle.REPUTATION)) {
				if (other != null) {
					amt += aiOtherRep * other.getReputation();
				}
				amt += aiRepLossSensitivity * me.diplomacyAI.personality.reputationLossSensitivity;
			}
			if (other != null && aiWarScore != 0) {
				amt += aiWarScore * ai.warQuality(me, other, wm, null, false, true);
			}
			if (other != null && aiPeaceScore != 0) {
				amt += aiPeaceScore * ai.peaceQuality(me, other, wm, null);
			}
			if (other != null && aiAllianceScore != 0) {
				amt += aiAllianceScore * ai.allianceQuality(me, other, wm, null);
			}
			if (other != null && aiGrievances != 0) {
				amt += aiGrievances * wm.getRelationship(me, other).getGrievances(me);
			}
			if (other != null && aiOtherGrievances != 0) {
				amt += aiOtherGrievances * wm.getRelationship(me, other).getGrievances(other);
			}
			if (other != null && aiRelativeIncome != 0) {
				amt += aiRelativeIncome * (wm.incomeForComparison(me) + 20) / (wm.incomeForComparison(other) + 20);
			}
			if (other != null && aiRelativeStrength != 0) {
				amt += aiRelativeStrength * (wm.getFleetStrength(me) + 1000.0) / (wm.getFleetStrength(other) + 1000.0);
			}
			amt += aiHonest * p.incidentsHonest;
			amt += aiJust * p.incidentsJust;
			amt += aiAggressive * p.incidentsAggressive;
			if (other != null) {
				Relationship rel = wm.getRelationship(me, other);
				switch (rel.level) {
					case WAR:
						amt += aiWar;
						break;
					case TRUCE:
						amt += aiTruce;
						break;
					case PEACE:
						amt += aiPeace;
						break;
					case NON_AGGRESSION_PACT:
						amt += aiNonAggressionPact;
						break;
					case DEFENSIVE_PACT:
						amt += aiDefensivePact;
						break;
					case ALLIANCE:
						amt += aiAlliance;
						break;
				}
			}
			return amt;
		}
	}
	
	public IncidentType(JSONObject o) {
		super(o.getString("name"));
		event = new Event(o);
		//minTimeSinceLastIncident = o.optInt("minTimeSinceLastIncident", 0);
		pattern = o.optInt("pattern", -1);
		enabled = o.optBoolean("enabled", true);
		globalOnceOnly = o.optBoolean("globalOnceOnly", false);
		repeat = o.optBoolean("repeat", false);
		if (o.has("frequency")) {
			frequency = Frequency.valueOf(o.getString("frequency"));
		}
		minTimeSinceLastIncident = frequency.interval;
		
		if (o.has("alsoUseUpIncidents")) {
			JSONArray a = o.getJSONArray("alsoUseUpIncidents");
			for (int i = 0; i < a.length(); i++) {
				alsoUseUpIncidents.add(a.getString(i));
			}
		}
	}
	
	public static enum Frequency {
		TEST(0), HIGH(11200), MEDIUM(44800), LOW(89600), VERY_LOW(134400);
		public final int interval;
		private Frequency(int interval) { this.interval = interval * 5 / 4; }
	}
	
	public boolean enabled;
	public boolean globalOnceOnly;
	public boolean repeat;
	public Event event;
	public int minTimeSinceLastIncident;
	public int pattern;
	public ArrayList<String> alsoUseUpIncidents = new ArrayList<String>();
	public Frequency frequency = Frequency.MEDIUM;
	
	public static IncidentType ofName(String name) {
		return ofName(IncidentType.class, name);
	}
	
	public static Incident get(int id, WorldMap wm) {
		for (int i = 0; i < wm.incidents.size(); i++) {
			Incident in = wm.incidents.get(i);
			if (in.id == id) { return in; }
		}
		return null;
	}
	
	public static Incident get(Empire e, WorldMap wm) {
		for (int i = 0; i < wm.incidents.size(); i++) {
			Incident in = wm.incidents.get(i);
			if (!in.isValid(wm)) { continue; }
			if (in.a == e || in.b == e) { return in; }
		}
		return null;
	}
	
	public static boolean inIncident(Empire e, WorldMap wm) {
		return get(e, wm) != null;
	}
		
	public static void tickIncidents(int ms, WorldMap wm) {
		if (wm.incidentFrequency.frequencyMultiplier == 0) { return; }
		ArrayList<IncidentType> ts = all(IncidentType.class);
		if (wm.incCounts.isEmpty()) {
			for (IncidentType t : ts) {
				wm.incCounts.put(t.name, 0);
			}
		}
		for (int i = 0; i < wm.incidents.size(); i++) {
			if (!wm.incidents.get(i).isValid(wm) || wm.incidents.get(i).tick(ms, wm)) {
				wm.incidents.remove(i);
				i--;
			}
		}
		if (ts.isEmpty() || !wm.toggles.contains(ConquestToggle.DIPLOMACY)) { return; }
		wm.incidentEmpireCounter++;
		if (wm.incidentEmpireCounter >= wm.empires.size()) {
			wm.incidentEmpireCounter = 0;
			wm.incidentTypeCounter = (wm.incidentTypeCounter + 1) % ts.size();
		}
		Empire e = wm.empires.get(wm.incidentEmpireCounter);
		IncidentType it = ts.get(wm.incidentTypeCounter);
		if (!it.enabled) { return; }
		if (wm.heroFrequency.frequencyMultiplier <= 0 && it.event.createsHero()) { return; }
		if (it.globalOnceOnly && wm.onceOnlyIncidentNames.contains(it.name)) { return; }
		//if (e.timeSinceLastIncident < it.minTimeSinceLastIncident * e.getIncidentPeriodMultiplier(it) || inIncident(e, wm)) { return; }
		ArrayList<Incident> possibleIncidents = it.getPossibleIncidents(e, wm, wm.incidentIDCounter);
		for (Incident in : possibleIncidents) {
			wm.incCounts.put(in.type.name, wm.incCounts.get(in.type.name) + 1);
		}
		/*if (wm.age > wm.lastIncCountSummary + 10000) {
			wm.lastIncCountSummary = wm.age;
			System.out.println("--- Inc Counts @ " + wm.age + " ---");
			for (IncidentType t : ts) {
				System.out.println(t.name + " " + wm.incCounts.get(t.name));
			}
		}*/
		if (e.timeSinceLastIncident < it.minTimeSinceLastIncident * e.getIncidentPeriodMultiplier(it) / wm.incidentFrequency.frequencyMultiplier || inIncident(e, wm)) { return; }
		// qqDPS USed to simulate check that should be in getPossibleIncidents
		for (int i = 0; i < possibleIncidents.size(); i++) {
			Incident in = possibleIncidents.get(i);
			if (in.b != null && in.b.timeSinceLastIncident < in.type.minTimeSinceLastIncident * in.b.getIncidentPeriodMultiplier(in.type) / wm.incidentFrequency.frequencyMultiplier) {
				possibleIncidents.remove(i);
				i--;
			}
		}
		if (possibleIncidents.isEmpty()) { return; }
		wm.incidentIDCounter++;
		Incident in;
		if (possibleIncidents.size() == 1) {
			in = possibleIncidents.get(0);
		} else {
			in = possibleIncidents.get(wm.r.nextInt(possibleIncidents.size()));
		}
		in.applyCurrentEvent(wm);
		if (it.globalOnceOnly) {
			wm.onceOnlyIncidentNames.add(it.name);
		}
		in.a.timeSinceLastIncident = 0;
		in.a.increaseIncidentPeriodMultiplier(in.type);
		if (in.b != null) {
			in.b.timeSinceLastIncident = 0;
			in.b.increaseIncidentPeriodMultiplier(in.type);
			if (!in.type.repeat) {
				wm.getRelationship(in.a, in.b).previousIncidents.add(in.type.name);
				wm.getRelationship(in.a, in.b).previousIncidents.addAll(in.type.alsoUseUpIncidents);
			}
		} else if (!in.type.repeat) {
			in.a.previousIncidents.add(in.type.name);
		}
		wm.incidents.add(in);
	}
	
	// I am so so sorry. It was the only way.
	public ArrayList<Incident> getPossibleIncidents(Empire a, WorldMap wm, int nextID) {
		ArrayList<Incident> l = new ArrayList<Incident>();
		if (event.filter.oneEmpireOnly) {
			if (a.previousIncidents.contains(name)) {
				return l;
			}
			if (event.filter.aCity || event.filter.aTown) {
				for (int aci = 0; aci < a.cities.size(); aci++) {
					City ac = a.cities.get(aci);
					if (event.filter.aWithEmptyNest || event.filter.aWithNest != null) {
						for (int ani = 0; ani < wm.nests.size(); ani++) {
							MonsterNest an = wm.nests.get(ani);
							if (wm.cityOwnership[an.y][an.x] != ac.id) { continue; }
							if (event.filter.check(a, null, ac, null, an, null, wm)) {
								l.add(new Incident(nextID, this, a, null, ac, null, an, null));
							}
						}
					} else {
						if (event.filter.check(a, null, ac, null, null, null, wm)) {
							l.add(new Incident(nextID, this, a, null, ac, null, null, null));
						}
					}
				}
			} else {
				if (event.filter.aWithEmptyNest || event.filter.aWithNest != null) {
					for (int ani = 0; ani < wm.nests.size(); ani++) {
						MonsterNest an = wm.nests.get(ani);
						if (wm.cityOwnership[an.y][an.x] == -1 || !a.cities.contains(wm.getCity(wm.cityOwnership[an.y][an.x]))) { continue; }
						if (event.filter.check(a, null, null, null, an, null, wm)) {
							l.add(new Incident(nextID, this, a, null, null, null, an, null));
						}
					}
				} else {
					if (event.filter.check(a, null, null, null, null, null, wm)) {
						l.add(new Incident(nextID, this, a, null, null, null, null, null));
					}
				}
			}
		} else {
			for (int bi = 0; bi < wm.empires.size(); bi++) {
				Empire b = wm.empires.get(bi);
				if (!a.playerControlled && !b.playerControlled) { continue; } // For now don't do AI-AI incidents.
				if (a == b) {
					continue;
				}
				/*if (b.timeSinceLastIncident < minTimeSinceLastIncident * b.getIncidentPeriodMultiplier(this) || inIncident(b, wm)) {
					continue;
				}*/
				if (wm.getRelationship(a, b).getUltimatum(wm) != null) {
					continue;
				}
				if (wm.getRelationship(a, b).previousIncidents.contains(name)) {
					continue;
				}
				if (event.filter.aCity || event.filter.aTown) {
					for (int aci = 0; aci < a.cities.size(); aci++) {
						City ac = a.cities.get(aci);
						if (event.filter.bCity || event.filter.bTown) {
							for (int bci = 0; bci < b.cities.size(); bci++) {
								City bc = b.cities.get(bci);
								if (event.filter.aWithEmptyNest || event.filter.aWithNest != null) {
									for (int ani = 0; ani < wm.nests.size(); ani++) {
										MonsterNest an = wm.nests.get(ani);
										if (wm.cityOwnership[an.y][an.x] != ac.id) { continue; }
										if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
											for (int bni = 0; bni < wm.nests.size(); bni++) {
												MonsterNest bn = wm.nests.get(bni);
												if (wm.cityOwnership[bn.y][bn.x] != bc.id) { continue; }
												if (event.filter.check(a, b, ac, bc, an, bn, wm)) {
													l.add(new Incident(nextID, this, a, b, ac, bc, an, bn));
												}
											}
										} else {
											if (event.filter.check(a, b, ac, bc, an, null, wm)) {
												l.add(new Incident(nextID, this, a, b, ac, bc, an, null));
											}
										}
									}
								} else {
									if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
										for (int bni = 0; bni < wm.nests.size(); bni++) {
											MonsterNest bn = wm.nests.get(bni);
											if (wm.cityOwnership[bn.y][bn.x] != bc.id) { continue; }
											if (event.filter.check(a, b, ac, bc, null, bn, wm)) {
												l.add(new Incident(nextID, this, a, b, ac, bc, null, bn));
											}
										}
									} else {
										if (event.filter.check(a, b, ac, bc, null, null, wm)) {
											l.add(new Incident(nextID, this, a, b, ac, bc, null, null));
										}
									}
								}
							}
						} else {
							// B without town/city, but may still have monster nests of course.
							if (event.filter.aWithEmptyNest || event.filter.aWithNest != null) {
								for (int ani = 0; ani < wm.nests.size(); ani++) {
									MonsterNest an = wm.nests.get(ani);
									if (wm.cityOwnership[an.y][an.x] != ac.id) { continue; }
									if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
										for (int bni = 0; bni < wm.nests.size(); bni++) {
											MonsterNest bn = wm.nests.get(bni);
											if (wm.cityOwnership[bn.y][bn.x] == -1 || !b.cities.contains(wm.getCity(wm.cityOwnership[bn.y][bn.x]))) { continue; }
											if (event.filter.check(a, b, ac, null, an, bn, wm)) {
												l.add(new Incident(nextID, this, a, b, ac, null, an, bn));
											}
										}
									} else {
										if (event.filter.check(a, b, ac, null, an, null, wm)) {
											l.add(new Incident(nextID, this, a, b, ac, null, an, null));
										}
									}
								}
							} else {
								if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
									for (int bni = 0; bni < wm.nests.size(); bni++) {
										MonsterNest bn = wm.nests.get(bni);
										if (wm.cityOwnership[bn.y][bn.x] == -1 || !b.cities.contains(wm.getCity(wm.cityOwnership[bn.y][bn.x]))) { continue; }
										if (event.filter.check(a, b, ac, null, null, bn, wm)) {
											l.add(new Incident(nextID, this, a, b, ac, null, null, bn));
										}
									}
								} else {
									if (event.filter.check(a, b, ac, null, null, null, wm)) {
										l.add(new Incident(nextID, this, a, b, ac, null, null, null));
									}
								}
							}
						}
					}
				} else {
					if (event.filter.bCity || event.filter.bTown) {
						for (int bci = 0; bci < b.cities.size(); bci++) {
							City bc = b.cities.get(bci);
							if (event.filter.aWithEmptyNest || event.filter.aWithNest != null) {
								for (int ani = 0; ani < wm.nests.size(); ani++) {
									MonsterNest an = wm.nests.get(ani);
									if (wm.cityOwnership[an.y][an.x] == -1 || !a.cities.contains(wm.getCity(wm.cityOwnership[an.y][an.x]))) { continue; }
									if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
										for (int bni = 0; bni < wm.nests.size(); bni++) {
											MonsterNest bn = wm.nests.get(bni);
											if (wm.cityOwnership[bn.y][bn.x] != bc.id) { continue; }
											if (event.filter.check(a, b, null, bc, an, bn, wm)) {
												l.add(new Incident(nextID, this, a, b, null, bc, an, bn));
											}
										}
									} else {
										if (event.filter.check(a, b, null, bc, an, null, wm)) {
											l.add(new Incident(nextID, this, a, b, null, bc, an, null));
										}
									}
								}
							} else {
								if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
									for (int bni = 0; bni < wm.nests.size(); bni++) {
										MonsterNest bn = wm.nests.get(bni);
										if (wm.cityOwnership[bn.y][bn.x] != bc.id) { continue; }
										if (event.filter.check(a, b, null, bc, null, bn, wm)) {
											l.add(new Incident(nextID, this, a, b, null, bc, null, bn));
										}
									}
								} else {
									if (event.filter.check(a, b, null, bc, null, null, wm)) {
										l.add(new Incident(nextID, this, a, b, null, bc, null, null));
									}
								}
							}
						}
					} else {
						// B without town/city, but may still have monster nests of course.
						if (event.filter.aWithEmptyNest || event.filter.aWithNest != null) {
							for (int ani = 0; ani < wm.nests.size(); ani++) {
								MonsterNest an = wm.nests.get(ani);
								if (wm.cityOwnership[an.y][an.x] == -1 || !a.cities.contains(wm.getCity(wm.cityOwnership[an.y][an.x]))) { continue; }
								if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
									for (int bni = 0; bni < wm.nests.size(); bni++) {
										MonsterNest bn = wm.nests.get(bni);
										if (wm.cityOwnership[bn.y][bn.x] == -1 || !b.cities.contains(wm.getCity(wm.cityOwnership[bn.y][bn.x]))) { continue; }
										if (event.filter.check(a, b, null, null, an, bn, wm)) {
											l.add(new Incident(nextID, this, a, b, null, null, an, bn));
										}
									}
								} else {
									if (event.filter.check(a, b, null, null, an, null, wm)) {
										l.add(new Incident(nextID, this, a, b, null, null, an, null));
									}
								}
							}
						} else {
							if (event.filter.bWithEmptyNest || event.filter.bWithNest != null) {
								for (int bni = 0; bni < wm.nests.size(); bni++) {
									MonsterNest bn = wm.nests.get(bni);
									if (wm.cityOwnership[bn.y][bn.x] == -1 || !b.cities.contains(wm.getCity(wm.cityOwnership[bn.y][bn.x]))) { continue; }
									if (event.filter.check(a, b, null, null, null, bn, wm)) {
										l.add(new Incident(nextID, this, a, b, null, null, null, bn));
									}
								}
							} else {
								if (event.filter.check(a, b, null, null, null, null, wm)) {
									l.add(new Incident(nextID, this, a, b, null, null, null, null));
								}
							}
						}
					}
				}
			}
		}
		return l;
	}
}