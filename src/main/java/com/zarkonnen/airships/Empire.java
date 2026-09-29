package com.zarkonnen.airships;

import com.zarkonnen.airships.Hero.StatChange;
import com.zarkonnen.airships.HeroType.Stat;
import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.Lang.localeT;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.Tincture;
import com.zarkonnen.airships.Reward;
import com.zarkonnen.catengine.util.Clr;
import java.io.IOException;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;

public class Empire implements FleetOwner, OutPipe.Writer {
	public static final int MS_PER_INCOME = WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 2;

	public static enum MessageType {
		ENEMY_LANDSHIPS_CAUGHT,
		ENEMY_REMAINING_LANDSHIPS_DESTROYED,
		ENEMY_ABANDONED_LANDSHIPS,
		OWN_LANDSHIPS_CAUGHT
	}
	
	public static class Message {
		public MessageType type;
		public City c;
		public String text;

		public Message(MessageType type, City c, String text) {
			this.type = type;
			this.c = c;
			this.text = text;
		}
	}
	
	public static enum ReputationLevel implements HasName {
		LOVED(80), LIKED(60), TOLERATED(40), DISLIKED(20), HATED(0);
		public final int minimum;

		private ReputationLevel(int minimum) {
			this.minimum = minimum;
		}

		@Override
		public String getName() {
			return _t(name() + "_rep");
		}
		
		public static ReputationLevel getLevel(int reputation) {
			for (ReputationLevel rl : ReputationLevel.values()) {
				if (reputation >= rl.minimum) {
					return rl;
				}
			}
			return ReputationLevel.HATED;
		}
	}
	
	@Override
	public String getNextShipID() {
		return "e" + id + "#" + shipIDCounter++;
	}
	
	public String getPlayerNames() {
		if (playerNames.size() == 1) {
			return playerNames.get(0);
		}
		StringBuilder sb = new StringBuilder();
		for (String n : playerNames) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(n);
		}
		return sb.toString();
	}
	
	public final int id;
	public int shipIDCounter = 1;
	public int smartAccum = 0;
	public int fleetSmartIndex = 0;
	public String name;
	public ArrayList<String> playerNames = new ArrayList<String>();
	public CoatOfArms arms;
	public BonusSet bonuses = new BonusSet();
	public BonusSet rewardedBonuses = new BonusSet();
	private int money;
	private int kestrelNetworkHashIn; // Used to store hashed money values to defeat cheat engine.
	private int kestrelNetworkHashOut;
	private int reputation = 50;
	private int repFromCityUpgrades;
	private int repDecayCounter = 0;
	public ArrayList<City> cities = new ArrayList<City>();
	private ArrayList<Fleet> fleets = new ArrayList<Fleet>();
	public ArrayList<Spy> spies = new ArrayList<Spy>();
	public transient ArrayList<SpyActionResult> spyActionResults = new ArrayList<SpyActionResult>();
	public int moneyMs;
	public int warConsiderDelay;
	public HashMap<MapLocation, Double> locAggressiveness = new HashMap<MapLocation, Double>();
	public double aggressiveness;
	public int spyActionsDone = 0;
	public int numAllianceMembers = 1;
	public transient Reward rewardGiven = null;
	public transient String rewardExtraInfo = "";
	public transient ArrayList<Empire> rewardWinnerEmpires = null;
	public transient String[] rewardDetails = { "?", "?", "?" };
	public int timeSinceRaided = 120000;
	public int timeSinceSpiedUpon = 120000;
	public boolean playerControlled;
	public ArrayList<MapLocation> aiTargets;
	public ArrayList<City> citiesForAITargets;
	public int warHashForAITargets; // Hash of which empires we're at war with.
	public ArrayList<Tech.Choice> techs = new ArrayList<Tech.Choice>();
	public Tech.Choice research;
	public ArrayList<Tech.Choice> researchQueue = new ArrayList<Tech.Choice>();
	public int researchPoints;
	public int unassignedResearchPoints;
	public final ConstructionStrategy constructionStrategy;
	public HashMap<Tech.Choice, Integer> partialResearchPoints = new HashMap<Tech.Choice, Integer>();
	public int deaths;
	public String prefix;
	public int spyActionFailAccumulator;
	public int spyDefenceFailAccumulator;
	public ArrayList<Airship> expedition;
	public int expeditionTime;
	public City expeditionDepartureCity;
	public EraModifier expeditionEraModifier;
	public int timeAtPeace = 0;
	public DiplomacyAI diplomacyAI;
	public int sendChallengeCooldown;
	public int timeSinceLastChallenge;
	public Clr mapClr;
	public Color mapColor, mapColor2;
	public ArrayList<BesiegeRequest> besiegeRequests = new ArrayList<BesiegeRequest>();
	public ArrayList<String> previousIncidents = new ArrayList<String>();
	private HashMap<String, Integer> incidentPeriodMultipliers = new HashMap<String, Integer>();
	public transient HashMap<IncidentType.Event, ArrayList<Tech.Choice>> techsBoosted = new HashMap<IncidentType.Event, ArrayList<Tech.Choice>>();
	public transient String heroAppendix;
	public transient String incidentNotice;
	public transient MapLocation incidentNoticePin;
	public transient Empire incidentNoticeEmpire;
	public int timeSinceHeroAppeared;
	
	public boolean easySpyCheat;
	
	public int allianceBreakings = 0;
	public boolean hasLostTerritory = false;
	public boolean hasBuiltAirship = false;
	public boolean hasBuiltFleshcracker = false;
	public boolean hasBuiltMechSquid = false;
	public int bioNestsCleared = 0;
	public int coronationsSabotaged = 0;
	
	public int aiConstructionSpent;
	public int aiUpgradesSpent = 250; // To prevent all AIs building an upgrade second.
	
	public boolean doNotAttackCheat;
	public boolean landshipFocus;
	
	public transient ArrayList<Message> messages = new ArrayList<Message>();
	public transient boolean coronationMessageSeen;
	public transient City coronationFailedDueToLostCityCity;
	public transient Empire coronationFailedDueToLostCityEmpire;
	public transient City coronationFailedDueToNoLongerReadyCity;
	public transient City coronationIntentionallyCancelledCity;
	public transient City coronationWarningCity;
	public transient City coronationStartedCity;
	public transient boolean repChanged;
	
	public transient boolean finalRitualMessageSeen;
	public transient City finalRitualFailedDueToLostCityCity;
	public transient Empire finalRitualFailedDueToLostCityEmpire;
	public transient City finalRitualIntentionallyCancelledCity;
	public transient City finalRitualWarningCity;
	public transient City finalRitualStartedCity;
	public boolean ritualSitesVisibleMessageSeen;
	
	public boolean previouslyAwardedClaim;
	
	public transient Tech.Choice researchedTech;
	public transient Utils.Pair<ArrayList<CityClaim>, Empire> conquerorInfo;
	
	public transient ArrayList<Airship> aiAvailableShips;
	public transient BonusSet aiAvailableShipsBonuses;
	public transient ArrayList<Airship> aiAvailableLandships;
	public transient BonusSet aiAvailableLandshipsBonuses;
	public transient ArrayList<Airship> aiAvailableBuildings;
	public transient BonusSet aiAvailableBuildingsBonuses;
	public transient int aiMostExpensiveConstructionCost;
	
	public transient int cachedCityIncome = -1;
	
	// Hero stuff
	public transient ArrayList<Pair<Hero, HeroEvent.Hook>> newRecruits = new ArrayList<Pair<Hero, HeroEvent.Hook>>();
	public transient ArrayList<HeroStatNotice> heroStatEvents = new ArrayList<HeroStatNotice>();
	public transient LinkedList<Hero.StatChange> heroStatChanges = new LinkedList<Hero.StatChange>();
	public transient LinkedList<HeroComment> heroComments = new LinkedList<HeroComment>();
	public transient HashSet<String> usedComments = new HashSet<String>(); // OK to use hashset because strictly local.
	public transient ArrayList<Pair<Hero, Integer>> heroInjuryNotices = new ArrayList<Pair<Hero, Integer>>();
	public transient ArrayList<Hero> heroRecoveryNotices = new ArrayList<Hero>();
	public int timeSinceLastIncident;
	
	public ArrayList<Medal> medalDesigns = new ArrayList<Medal>();
	public ArrayList<ArrayList<String>> medalAwards = new ArrayList<ArrayList<String>>();
	
	// Separate list of techs so choice techs cannot spawn multiple hero events.
	public ArrayList<Tech.Choice> techEverResearched = new ArrayList<Tech.Choice>();
	
	// Returns a mixture of Medals for real medals and Integers for medals that could be awarded.
	public ArrayList getMedalsAndAvailableMedals(Airship ship) {
		int maxAvailableMedalLevel = CrewExperienceLevel.getMaxAvailableMedalLevel(ship.crewExperience);
		ArrayList l = new ArrayList();
		lp: for (int level = 1; level <= CrewExperienceLevel.getMaxMedalLevel(bonuses); level++) {
			for (Medal m : ship.medals) {
				if (m.level == level) {
					l.add(m);
					continue lp;
				}
			}
			if (level <= maxAvailableMedalLevel && getMedalAwards(level).size() < CrewExperienceLevel.getNumMedals(level)) {
				l.add(Integer.valueOf(level));
			}
		}
		return l;
	}
	
	public ArrayList<Integer> getAvailableMedals(Airship ship) {
		int maxAvailableMedalLevel = CrewExperienceLevel.getMaxAvailableMedalLevel(ship.crewExperience);
		ArrayList<Integer>  l = new ArrayList<Integer> ();
		lp: for (int level = 1; level <= CrewExperienceLevel.getMaxMedalLevel(bonuses); level++) {
			for (Medal m : ship.medals) {
				if (m.level == level) {
					continue lp;
				}
			}
			if (level <= maxAvailableMedalLevel && getMedalAwards(level).size() < CrewExperienceLevel.getNumMedals(level)) {
				l.add(Integer.valueOf(level));
			}
		}
		return l;
	}
	
	public Medal getMedalDesign(int level) {
		if (medalDesigns.size() < level) {
			return null;
		}
		return medalDesigns.get(level - 1);
	}
	
	public void setMedalDesign(int level, Medal m) {
		while (medalDesigns.size() < level) {
			medalDesigns.add(null);
		}
		medalDesigns.set(level - 1, m);
	}
	
	public void awardMedal(Airship ship, Medal m) {
		while (medalAwards.size() < m.level) {
			medalAwards.add(new ArrayList<String>());
		}
		medalAwards.get(m.level - 1).add(ship.getName());
		ship.giveMedal(m);
	}
	
	public ArrayList<String> getMedalAwards(int level) {
		if (medalAwards.size() < level) {
			return new ArrayList<String>();
		} else {
			return medalAwards.get(level - 1);
		}
	}
	
	public static class HeroComment {
		public final Hero hero;
		public final String comment;
		public final StatChange statChange;

		public HeroComment(Hero hero, String comment, StatChange statChange) {
			this.hero = hero;
			this.comment = comment;
			this.statChange = statChange;
		}
	}
	
	@Override
	public boolean isPlayerControlled() { return playerControlled; }
	
	public ArrayList<Empire> getHumanAllies(WorldMap m) {
		ArrayList<Empire> as = new ArrayList<Empire>();
		for (Relationship rel : m.getRelationships(this)) {
			if (rel.level == Relationship.Level.ALLIANCE && rel.other(this).playerControlled) {
				as.add(rel.other(this));
			}
		}
		return as;
	}
	
	public int getIncidentPeriodMultiplier(IncidentType t) {
		return incidentPeriodMultipliers.containsKey(t.name) ? incidentPeriodMultipliers.get(t.name) : 1;
	}
	
	public void increaseIncidentPeriodMultiplier(IncidentType t) {
		incidentPeriodMultipliers.put(t.name, getIncidentPeriodMultiplier(t) * 2);
	}
	
	public boolean has(CityUpgradeType cut) {
		for (int i = 0; i < cities.size(); i++) {
			if (cities.get(i).upgrades.contains(cut)) {
				return true;
			}
		}
		return false;
	}
	
	public int getResearchPoints(Tech.Choice c) {
		if (c == research) {
			return researchPoints;
		} else {
			return partialResearchPoints.containsKey(c) ? partialResearchPoints.get(c) : 0;
		}
	}
	/*	
	calc supply amount for selected ships
	multiply with base cost + distance
	multiply with modifier
	*/
	
	public boolean has(Airship ship) {
		if (expedition != null && expedition.contains(ship)) { return true; }
		for (int i = 0; i < fleets.size(); i++) {
			if (fleets.get(i).actives.contains(ship)) { return true; }
			if (fleets.get(i).reserve.contains(ship)) { return true; }
		}
		for (int i = 0; i < cities.size(); i++) {
			if (cities.get(i).defences.contains(ship)) { return true; }
		}
		return false;
	}
	
	public int moveSupplyCostShips(ArrayList<Airship> ships) {
		int supplyAmount = 0;
		for (int i = 0; i < ships.size(); i++) {
			if (ships.get(i).type == ShipType.AIRSHIP) {
				supplyAmount += StrictMath.max(EmpireStat.MIN_SHIP_SUPPLY_COST.get(bonuses), ships.get(i).getCachedSupplyRequired());
			}
			
		}
		return supplyAmount;
	}
	
	public int moveSupplyCostLandships(ArrayList<Airship> ships) {
		int supplyAmount = 0;
		for (int i = 0; i < ships.size(); i++) {
			if (ships.get(i).type == ShipType.LANDSHIP) {
				supplyAmount += StrictMath.max(EmpireStat.MIN_SHIP_SUPPLY_COST.get(bonuses), ships.get(i).getCachedSupplyRequired());
			}
		}
		return supplyAmount;
	}
	
	public double distance(Fleet f, MapLocation target, Pt icept, WorldMap m) {
		double dist = 0;
		if (target != null) {
			dist = StrictMath.sqrt((f.realX(m) - target.x) * (f.realX(m) - target.x) + (f.realY(m) - target.y) * (f.realY(m) - target.y));
		} else {
			dist = StrictMath.sqrt((f.realX(m) - icept.x) * (f.realX(m) - icept.x) + (f.realY(m) - icept.y) * (f.realY(m) - icept.y));
		}
		dist *= EmpireStat.SUPPLY_PER_DIST.get(bonuses);
		return StrictMath.ceil(dist * 5) / 5;
	}
	
	private int distance(Pt fp, MapLocation target) {
		double dist = StrictMath.sqrt((fp.x - target.x) * (fp.x - target.x) + (fp.y - target.y) * (fp.y - target.y));
		dist *= EmpireStat.SUPPLY_PER_DIST.get(bonuses);
		return (int) StrictMath.ceil(dist);
	}
	
	// * EmpireStat.LANDSHIP_MOVE_SUPPLY_MULT.get(bonuses)
	// EmpireStat.FRIENDLY_TO_FRIENDLY_CITY_SUPPLY_MULT.get(bonuses);
	// EmpireStat.NON_ADJACENT_MOVE_SUPPLY_MULT.get(bonuses)
	// EmpireStat.FROM_UNPACIFIED_CITY_MOVE_SUPPLY_MULT.get(bonuses);
	
	public City getCapital() {
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			if (!c.isTown && c.originalEmpire == this) { return c; }
		}
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			if (!c.isTown) { return c; }
		}
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			if (c.originalEmpire == this) { return c; }
		}
		return cities.get(0);
	}
	
	public boolean adjacentTo(Empire e2, WorldMap map) {
		for (int i = 0; i < cities.size(); i++) {
			if (e2.isAdjacent(cities.get(i), null, map)) { return true; }
		}
		return false;
	}
	
	public boolean isAdjacent(MapLocation target, Pt icept, WorldMap map) {
		City adjTestCity;
		if (target instanceof City) {
			adjTestCity = (City) target;
		} else if (target instanceof MonsterNest) {
			MonsterNest mn = (MonsterNest) target;
			adjTestCity = map.getCity(map.cityOwnership[mn.y][mn.x]);
		} else {
			adjTestCity = map.getCity(map.cityOwnership[(int) StrictMath.floor(icept.y)][(int) StrictMath.floor(icept.x)]);
		}
		if (adjTestCity != null) {
			for (int ci = 0; ci < cities.size(); ci++) {
				if (cities.get(ci).takeoverNeeded || cities.get(ci).takeoverMethod != null) { continue; }
				if (cities.get(ci).isAdjacentTo(adjTestCity, map)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isInTerritory(Fleet f, MapLocation target, Pt icept, WorldMap map) {
		return target != null && ((cities.contains(f.location) || (f.location instanceof MonsterNest && cities.contains(((MonsterNest) f.location).city(map))) || (f.location == null && f.isFriendlyMove)) && (cities.contains(target) || (target instanceof MonsterNest && cities.contains(((MonsterNest) target).city(map)))));
	}
	
	public int rebate(Fleet f) {
		if (f.location == null && f.transitDistance() > 0) {
			return (int) (StrictMath.min(1, 1 - f.progress / (f.transitDistance() + 1)) * f.supplyUsedForMove);
		}
		return 0;
	}
	
	public boolean isLimpHome(Fleet f, ArrayList<Airship> ships, MapLocation target, WorldMap map) {
		return map.toggles.contains(ConquestToggle.SUPPLY) && isFleeToClosestCity(f, target, map) && moveSupplyCost(f, ships, target, null, map, null, false) > f.supply();
	}
	
	public City closestFriendlyCity(Fleet f, WorldMap m) {
		return closestFriendlyCity(f.realX(m), f.realY(m), m, f.fleeDestinationNeeded ? f.location : null);
	}
	
	private City closestFriendlyCity(double x, double y, WorldMap m, MapLocation except) {
		City closest = null;
		double closestDsq = 0;
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (c == except) { continue; }
			double dsq = (x - c.x) * (x - c.x) + (y - c.y) * (y - c.y);
			if (closest == null || dsq < closestDsq) {
				closest = c;
				closestDsq = dsq;
			}
		}
		if (m.toggles.contains(ConquestToggle.DIPLOMACY)) {
			ArrayList<Relationship> rels = m.getRelationships(this);
			for (int i = 0; i < rels.size(); i++) {
				Relationship rel = rels.get(i);
				if (rel.level.ordinal() >= Relationship.Level.NON_AGGRESSION_PACT.ordinal()) {
					Empire other = rel.other(this);
					for (int ci = 0; ci < other.cities.size(); ci++) {
						City c = other.cities.get(ci);
						double dsq = (x - c.x) * (x - c.x) + (y - c.y) * (y - c.y);
						if (closest == null || dsq < closestDsq) {
							closest = c;
							closestDsq = dsq;
						}
					}
				}
			}
		}
		return closest;
	}
	
	private City closestFriendlyCity(MapLocation loc, WorldMap m) {
		return closestFriendlyCity(loc.x, loc.y, m, null);
	}
	
	private City closestFriendlyCity(Pt pt, WorldMap m) {
		return closestFriendlyCity(pt.x, pt.y, m, null);
	}
	
	private boolean isFleeToClosestCity(Fleet f, MapLocation target, WorldMap m) {
		return f.fleeDestinationNeeded && closestFriendlyCity(f, m) == target;
	}
	
	public int moveAndReturnSupplyCost(Fleet f, ArrayList<Airship> ships, MapLocation target, Pt icept, WorldMap map) {
		if (!map.toggles.contains(ConquestToggle.SUPPLY)) { return 0; }
		int cost = moveSupplyCost(f, ships, target, icept, map);
		if (target == null || (!cities.contains(target) && !(target instanceof MonsterNest && ((MonsterNest) target).type == null))) {
			City closestCity = target == null ? closestFriendlyCity(icept, map) : closestFriendlyCity(target, map);
			cost += moveSupplyCost(f, ships, closestCity, null, map, target == null ? icept : target.pt, false);
		}
		return cost;
	}
	
	public int moveSupplyCost(Fleet f, ArrayList<Airship> ships, MapLocation target, Pt icept, WorldMap map) {
		return moveSupplyCost(f, ships, target, icept, map, null, true);
	}
	
	private int moveSupplyCost(Fleet f, ArrayList<Airship> ships, MapLocation target, Pt icept, WorldMap map, Pt fleetPt, boolean applyFleeAndUnusedSuppliesRebate) {
		if (!map.toggles.contains(ConquestToggle.SUPPLY)) { return 0; }
		int supplyAmount = moveSupplyCostShips(ships) + (int) StrictMath.ceil(moveSupplyCostLandships(ships) * EmpireStat.LANDSHIP_MOVE_SUPPLY_MULT.get(bonuses));
		double dist = fleetPt == null ? distance(f, target, icept, map) : distance(fleetPt, target);
		int cost = (int) StrictMath.ceil(supplyAmount * dist);
		if (!isAdjacent(target, icept, map)) {
			cost *= EmpireStat.NON_ADJACENT_MOVE_SUPPLY_MULT.get(bonuses);
		}/* else if (isInTerritory(f, target, icept, map)) {
			cost *= EmpireStat.FRIENDLY_TO_FRIENDLY_CITY_SUPPLY_MULT.get(bonuses);
		}*/
		if (f.location == null && !f.fleeDestinationNeeded && !cities.contains(target)) {
			cost += supplyAmount * EmpireStat.CHANGE_COURSE_SUPPLY_PENALTY.get(bonuses);
		}
		if (applyFleeAndUnusedSuppliesRebate) {
			cost -= StrictMath.min(cost, rebate(f));
			if (isFleeToClosestCity(f, target, map)) {
				cost = StrictMath.min(cost, f.supply());
			}
		}
		return cost;
	}
	
	public String moveSupplyCostExplanation(Fleet f, ArrayList<Airship> ships, MapLocation target, Pt icept, WorldMap map) {
		StringBuilder sb = new StringBuilder();
		int mShips = moveSupplyCostShips(ships);
		if (mShips != 0) {
			sb.append(_t("Airships_supply_")).append(mShips).append("\n");
		}
		int mLandshipsBase = moveSupplyCostLandships(ships);
		int mLandships = (int) StrictMath.ceil(mLandshipsBase * EmpireStat.LANDSHIP_MOVE_SUPPLY_MULT.get(bonuses));
		if (mLandships != 0) {
			sb.append(_t("Landships_supply_")).append(mLandships).append("\n");
			if (mLandships != mLandshipsBase) {
				sb.append("    ").append(_t("Base_Cost_")).append(mLandshipsBase).append("\n");
				sb.append("    x ").append(EmpireStat.LANDSHIP_MOVE_SUPPLY_MULT.get(bonuses)).append("\n");
			}
		}
		int supplyAmount = mLandships + mShips;
		if (mLandships != 0 && mShips != 0) {
			sb.append(_t("Total_supply_")).append(supplyAmount).append("\n");
		}
		double dist = distance(f, target, icept, map);
		sb.append(_t("Distance_")).append(dist).append("\n");
		int cost = (int) StrictMath.ceil(supplyAmount * dist);
		sb.append(_t("DistanceXSupply_")).append(cost).append("\n");
		if (!isAdjacent(target, icept, map)) {
			sb.append("x ").append(EmpireStat.NON_ADJACENT_MOVE_SUPPLY_MULT.get(bonuses)).append(" ").append(_t("Not_Adjacent_to_Safe_Territory")).append("\n");
			cost *= EmpireStat.NON_ADJACENT_MOVE_SUPPLY_MULT.get(bonuses);
		}/* else if (isInTerritory(f, target, icept, map)) {
			sb.append("x ").append(EmpireStat.FRIENDLY_TO_FRIENDLY_CITY_SUPPLY_MULT.get(bonuses)).append(" In Friendly Territory\n");
			cost *= EmpireStat.FRIENDLY_TO_FRIENDLY_CITY_SUPPLY_MULT.get(bonuses);
		}*/
		if (f.location == null && !f.fleeDestinationNeeded && EmpireStat.CHANGE_COURSE_SUPPLY_PENALTY.get(bonuses) != 0 && !cities.contains(target)) {
			int amt = (int) (supplyAmount * EmpireStat.CHANGE_COURSE_SUPPLY_PENALTY.get(bonuses));
			cost += amt;
			sb.append("+ ").append(amt).append(" ").append(_t("Changing_Course")).append("\n");
		}
		int rebate = StrictMath.min(cost, rebate(f));
		if (rebate != 0) {
			cost -= rebate;
			sb.append("- ").append(rebate).append(" ").append(_t("Unused_Supplies_in_Fleet")).append("\n");
		}
		if (cost > f.supply() && isFleeToClosestCity(f, target, map)) {
			sb.append("- ").append(cost - f.supply()).append(" ").append(_t("Limping_Home")).append("\n");
			sb.append("    ").append(_t("limping_home_notice")).append("\n");
			cost = StrictMath.min(cost, f.supply());
		}
		sb.append(_t("Total_Supply_Cost_")).append(cost);
		return sb.toString();
	}

	public transient ArrayList<UltimatumNotice> ultimatumNotices = new ArrayList<UltimatumNotice>();
	public transient ArrayList<DiplomacyNotice> diplomacyNotices = new ArrayList<DiplomacyNotice>();
		
	public boolean isCoronationReady(WorldMap wm) {
		return isCoronationReady(wm, 0, 0);
	}
	
	public boolean isCoronating() {
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (c.coronation) { return true; }
		}
		return false;
	}
	
	public boolean isCrowned(WorldMap wm) {
		if (!isCoronationReady(wm)) { return false; }
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (c.coronation && c.coronationProgress >= EmpireStat.CORONATION_TIME.get(bonuses())) { return true; }
		}
		return false;
	}
	
	public Hero getCoronationEnablingHero(WorldMap wm) {
		if (!wm.toggles.contains(ConquestToggle.HERO_VICTORY)) { return null; }
		for (Hero h : Hero.getHeroes(this, wm)) {
			if (!h.hired) { continue; }
			for (Stat s : h.type.stats) {
				if (s.coronationOn100 && h.stats.get(s.name) >= 100) {
					return h;
				} 
			}
		}
		return null;
	}
	
	public boolean isCoronationReady(WorldMap wm, int repMargin, int cityMargin) {
		if (!wm.toggles.contains(ConquestToggle.CORONATION)) { return false; }
		if (!EmpireStat.CAN_CORONATE.get(bonuses)) { return false; }
		boolean skipRepCheck = false;
		if (getCoronationEnablingHero(wm) == null && getReputation() < Math.min(100, repMargin + EmpireStat.CORONATION_REPUTATION.get(bonuses()))) { return false; }
		int myCities = 0;
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (!c.isTown) {
				myCities++;
			}
		}
		return myCities >= wm.requiredCitiesForCoronation() + cityMargin;
	}
	
	public boolean hasEnoughCitiesForCoronation(WorldMap wm) {
		if (!wm.toggles.contains(ConquestToggle.CORONATION)) { return false; }
		if (!EmpireStat.CAN_CORONATE.get(bonuses)) { return false; }
		int myCities = 0;
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (!c.isTown) {
				myCities++;
			}
		}
		return myCities >= wm.requiredCitiesForCoronation();
	}
	
	public int getCoronationCost(WorldMap wm) {
		return wm.requiredCitiesForCoronation() * EmpireStat.CORONATION_COST_PER_CITY.get(bonuses());
	}
	
	public int getFinalRitualCost(WorldMap wm) {
		return wm.numRitualSites() * EmpireStat.RITUAL_COST_PER_SITE.get(bonuses());
	}
		
	public boolean isDoingFinalRitual() {
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (c.finalRitual) { return true; }
		}
		return false;
	}
	
	public boolean hasCompletedFinalRitual(WorldMap wm) {
		if (!isFinalRitualReady(wm)) { return false; }
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (c.finalRitual && c.finalRitualProgress >= EmpireStat.FINAL_RITUAL_TIME.get(bonuses())) { return true; }
		}
		return false;
	}
	
	public boolean isFinalRitualReady(WorldMap wm) {
		if (!wm.toggles.contains(ConquestToggle.CORONATION)) { return false; }
		if (!EmpireStat.CAN_DO_FINAL_RITUAL.get(bonuses)) { return false; }
		for (City c : wm.cities()) {
			if (c.isRitualSite && !cities.contains(c)) { return false; }
		}
		return true;
	}
	
	public void cancelFinalRitual(City dueToCity, boolean cancelledIntentionally, WorldMap map) {
		if (!dueToCity.isRitualSite) { return; }
		if (!isDoingFinalRitual()) { return; }
		for (City c : cities) {
			c.finalRitual = false;
			c.finalRitualProgress = 0;
			c.finalRitualWarningSent = false;
			c.finalRitualStartedSent = false;
			c.prevFinalRitualSabotages = 0;
		}
		if (cancelledIntentionally) {
			for (Empire e : map.empires) {
				if (e == this) { continue; }
				e.finalRitualIntentionallyCancelledCity = dueToCity;
			}
			finalRitualMessageSeen = true;
		} else {
			for (Empire e : map.empires) {
				e.finalRitualFailedDueToLostCityCity = dueToCity;
				e.finalRitualFailedDueToLostCityEmpire = this;
			}
		}
	}
	
	public boolean isThisLikedOrMore(ReputationLevel lvl) {
		return reputation >= lvl.minimum;
	}
	
	public boolean isThisHatedOrMore(ReputationLevel lvl) {
		return reputation < lvl.minimum + 20;
	}
	
	public int getReputation() {
		return reputation;
	}
	
	public ReputationLevel getReputationLevel() {
		return ReputationLevel.getLevel(reputation);
	}
	
	public final void setReputation(int reputation, HasRelationships wm) {
		if (wm.toggles().contains(ConquestToggle.REPUTATION)) {
			reputation = Math.max(0, Math.min(100, reputation));
			this.reputation = reputation;
			for (ReputationLevel rl : ReputationLevel.values()) {
				if (Loadable.hasOfName(Bonus.class, rl.name())) {
					bonuses.remove(Bonus.ofName(rl.name()));
				}
			}
			if (Loadable.hasOfName(Bonus.class, getReputationLevel().name())) {
				bonuses.add(Bonus.ofName(getReputationLevel().name()));
			}
		} else {
			this.reputation = 50;
		}
	}
	
	public void changeReputation(int delta, HasRelationships wm) {
		if (wm.toggles().contains(ConquestToggle.REPUTATION)) {
			ReputationLevel oldLevel = getReputationLevel();
			setReputation(reputation + delta, wm);
			if (delta != 0) { repChanged = true; }
			// Rep levels go from LOVED to HATED, so these ordinal comparisons are the right way around.
			if (wm instanceof WorldMap && getReputationLevel().ordinal() < oldLevel.ordinal()) {
				Hero.processHeroEvent(HeroEvent.repLevelUpgrade(this, getReputationLevel()), (WorldMap) wm);
			}
			if (wm instanceof WorldMap && getReputationLevel().ordinal() > oldLevel.ordinal()) {
				Hero.processHeroEvent(HeroEvent.repLevelDowngrade(this, getReputationLevel()), (WorldMap) wm);
			}
		}
	}
	
	public String getRepChangeHeroAppendix(int change, WorldMap wm, boolean past) {
		if (!wm.toggles.contains(ConquestToggle.REPUTATION)) { return ""; }
		ReputationLevel oldLevel = getReputationLevel();
		ReputationLevel newLevel = ReputationLevel.getLevel(reputation + change);
		// Rep levels go from LOVED to HATED, so these ordinal comparisons are the right way around.
		if (newLevel.ordinal() < oldLevel.ordinal()) {
			return Hero.getStatChangeAppendix(this, HeroEvent.repLevelUpgrade(this, newLevel), wm, past);
		}
		if (newLevel.ordinal() > oldLevel.ordinal()) {
			return Hero.getStatChangeAppendix(this, HeroEvent.repLevelDowngrade(this, newLevel), wm, past);
		}
		return "";
	}
	
	public int getStrength(WorldMap wm) {
		int str = getMoney();
		for (Fleet f : fleets) {
			str += WorldMap.cost(f);
		}
		for (City c : cities) {
			str += WorldMap.cost(c.defences);
		}
		str += StrictMath.max(0, incomeBalance(wm) * 25);
		return str;
	}
	
	private static class SimpleArms {
		public final String specialCharge;
		public final Charge charge;
		public final Tincture chargeT;
		public final Tincture bgT;

		public SimpleArms(String specialCharge, Charge charge, Tincture chargeT, Tincture bgT) {
			this.specialCharge = specialCharge;
			this.charge = charge;
			this.chargeT = chargeT;
			this.bgT = bgT;
		}
	}
	
	public static CoatOfArms mergedArms(List<FleetOwner> l, GuardedRandom r) {
		ArrayList<CoatOfArms> coas = new ArrayList<CoatOfArms>();
		for (FleetOwner fo : l) { coas.add(fo.getArms()); }
		return mergedArms(coas, r);
	}
	
	public static CoatOfArms mergedArms(ArrayList<CoatOfArms> l, GuardedRandom r) {
		if (l.isEmpty() || l.size() > 4) {
			return CoatOfArms.getRandom(r, HeraldicStyle.ofName("alliance"));
		}
		if (l.size() == 1) { return l.get(0); }
		
		if (l.size() == 2) {
			CoatOfArms a = l.get(0);
			CoatOfArms b = l.get(1);
			// If they're identical, just return the one arms.
			if (a.equals(b)) { return a; }
			// if both have arms with charges, we do per pale
			if (a.layout.charges > 0 && b.layout.charges > 0) {
				CoatOfArms coa = new CoatOfArms();
				coa.setLayout(ArmsLayout.valueOf("PARTY_PER_PALE"));
				if (a.specialCharge[0] != null) {
					coa.setSpecialCharge(a.specialCharge[0], 0);
				} else {
					coa.setCharge(a.charge[0], 0);
				}
				if (b.specialCharge[0] != null) {
					coa.setSpecialCharge(b.specialCharge[0], 1);
				} else {
					coa.setCharge(b.charge[0], 1);
				}
				// if both have arms with charges and the same background, do per pale but switch the tinctures of the 2nd
				if (a.tincture[0] == b.tincture[0]) {
					coa.setTincture(a.tincture[0], CoatOfArms.TinctureSlot.TINCTURE0);
					coa.setTincture(b.chargeT[0], CoatOfArms.TinctureSlot.TINCTURE1);
					coa.setTincture(a.chargeT[0], CoatOfArms.TinctureSlot.CHARGE0);
					coa.setTincture(b.tincture[0], CoatOfArms.TinctureSlot.CHARGE1);
				} else {
					coa.setTincture(a.tincture[0], CoatOfArms.TinctureSlot.TINCTURE0);
					coa.setTincture(b.tincture[0], CoatOfArms.TinctureSlot.TINCTURE1);
					coa.setTincture(a.chargeT[0], CoatOfArms.TinctureSlot.CHARGE0);
					coa.setTincture(b.chargeT[0], CoatOfArms.TinctureSlot.CHARGE1);
				}
				return coa;
			}
			
			// we use the layout of the one with the colors of the other
			// but we prefer using the layout (and charges) of the one with charges, if any
			if (b.layout.charges > 0) {
				a = l.get(1);
				b = l.get(0);
			}
			CoatOfArms coa = new CoatOfArms(a.toJSON());
			for (int i = 0; i < coa.layout.tinctures; i++) {
				coa.setTincture(b.tincture[i % b.layout.tinctures], CoatOfArms.TinctureSlot.tinctureOf(i));
			}
			for (int i = 0; i < coa.layout.charges; i++) {
				coa.setTincture(b.tincture[(i + 1) % b.layout.tinctures], CoatOfArms.TinctureSlot.chargeTOf(i));
			}

			return coa;			
		}
		
		// 3-4 arms
		CoatOfArms coa = new CoatOfArms();
		coa.setLayout(ArmsLayout.valueOf("QUARTERLY"));
		
		for (int i = 0; i < 4; i++) {
			CoatOfArms a = l.get(i % l.size());
			coa.setTincture(a.tincture[0], CoatOfArms.TinctureSlot.tinctureOf(i));
			if (a.layout.charges > 0) {
				if (a.specialCharge[0] != null) {
					coa.setSpecialCharge(a.specialCharge[0], i);
				} else {
					coa.setCharge(a.charge[0], i);
				}
				coa.setTincture(a.chargeT[0], CoatOfArms.TinctureSlot.chargeTOf(i));
			} else {
				coa.setCharge(Charge.ofName("ESTOILE"), i);
				if (a.layout.tinctures > 1) {
					coa.setTincture(a.tincture[1], CoatOfArms.TinctureSlot.chargeTOf(i));
				}
			}
		}
		
		return coa;
	}
	
	public static <T extends FleetOwner> String nameList(List<T> l, Locale lang) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < l.size(); i++) {
			if (i == l.size() - 1 && l.size() > 1) {
				if (l.size() > 2) {
					sb.append(",");
				}
				sb.append(" ").append(Lang.localeT(lang, "list_and"));
			} else if (i > 0) {
				sb.append(", ");
			}
			T t = l.get(i);
			sb.append(t.getNameInLocale(lang));
		}
		return sb.toString();
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getNameOrTranslationKey() {
		return name;
	}
	
	@Override
	public String getNameInLocale(Locale lang) {
		return name;
	}

	public int numFullCities() {
		int n = 0;
		int csz = cities.size();
		for (int i = 0; i < csz; i++) {
			if (!cities.get(i).isTown) {
				n++;
			}
		}
		return n;
	}
	
	@Override
	public boolean nameIsTranslationKey() {
		return false;
	}

	@Override
	public CoatOfArms getArms() {
		return arms;
	}

	@Override
	public ArrayList<Fleet> getFleets() {
		return fleets;
	}
	
	public Fleet fleetAt(MapLocation loc) {
		for (Fleet f : fleets) {
			if (f.location == loc) { return f; }
		}
		return null;
	}
	
	public Tincture getMainTincture() {
		Tincture[] ts = arms.getRoundelTinctures();
		int ti = 0;
		while (ti < ts.length - 1 && ts[ti].metal) {
			ti++;
		}
		return ts[ti];
	}
	
	public Tincture getSecondaryTincture() {
		return arms.getMapTincture();
	}
	
	public Spy getSpyFor(City c) {
		int ssz = spies.size();
		for (int si = 0; si < ssz; si++) {
			Spy s = spies.get(si);
			if (s.location == c) { return s; }
		}
		return null;
	}
	
	public static Empire stub(int id, String name) {
		return new Empire(id, name);
	}
	
	private Empire(int id, String name) {
		this.id = id;
		this.name = name;
		playerNames.add(name);
		constructionStrategy = null;
		reputation = 50;
	}


	public Empire(int id, String name, CoatOfArms arms, ArrayList<String> playerNames, Bonus bonus, int money, double aggressiveness, ConstructionStrategy constructionStrategy, DiplomacyPersonality dp, WorldMap wm) {
		this.id = id;
		this.name = name;
		this.arms = arms;
		this.playerNames = playerNames;
		this.playerControlled = !playerNames.isEmpty();
		bonuses.add(bonus);
		bonuses.add(Bonus.ofName("CONQUEST"));
		setMoney(money);
		setReputation(EmpireStat.STARTING_REPUTATION.get(bonuses), wm);
		this.aggressiveness = aggressiveness;
		warConsiderDelay = 30000;
		this.constructionStrategy = constructionStrategy;
		diplomacyAI = new DiplomacyAI(dp);
	}
	
	@Override
	public AIQuality getAIQuality(WorldMap m) {
		return m.difficulty.enemyAI;
	}
	
	public Fleet fleet(Airship s) {
		for (Fleet f : getFleets()) {
			if (f.actives.contains(s) || f.reserve.contains(s)) {
				return f;
			}
		}
		return null;
	}
	
	public void scrap(Airship askForScrap, WorldMap wm) {
		wm.clearHeroFrom(askForScrap);
		for (Fleet f : getFleets()) {
			if (f.actives.contains(askForScrap)) {
				f.actives.remove(askForScrap);
				setMoney(getMoney() + askForScrap.getCost() / 8);
				if (f.actives.isEmpty() && f.reserve.isEmpty()) {
					getFleets().remove(f);
					f.broadcastDestroyed(wm);
				}
				return;
			}
			if (f.reserve.contains(askForScrap)) {
				f.reserve.remove(askForScrap);
				setMoney(getMoney() + askForScrap.getCost() / 8);
				if (f.actives.isEmpty() && f.reserve.isEmpty()) {
					getFleets().remove(f);
					f.broadcastDestroyed(wm);
				}
				return;
			}
		}
		for (City c : cities) {
			if (c.hasDefence(askForScrap)) {
				c.removeDefence(askForScrap);
				setMoney(getMoney() + askForScrap.getCost() / 8);
				return;
			}
		}
	}
	
	public void remove(Airship remove, WorldMap wm) {
		wm.clearHeroFrom(remove);
		for (Fleet f : getFleets()) {
			if (f.actives.contains(remove)) {
				f.actives.remove(remove);
				if (f.actives.isEmpty() && f.reserve.isEmpty()) {
					getFleets().remove(f);
					f.broadcastDestroyed(wm);
				}
				return;
			}
			if (f.reserve.contains(remove)) {
				f.reserve.remove(remove);
				if (f.actives.isEmpty() && f.reserve.isEmpty()) {
					getFleets().remove(f);
					f.broadcastDestroyed(wm);
				}
				return;
			}
		}
		for (City c : cities) {
			if (c.hasDefence(remove)) {
				c.removeDefence(remove);
				return;
			}
		}
	}
	
	public void replace(Airship a, Airship b, WorldMap wm) {
		if (b == null) {
			remove(a, wm);
			return;
		}
		for (Fleet f : getFleets()) {
			if (f.actives.contains(a)) {
				f.actives.set(f.actives.indexOf(a), b);
				return;
			}
			if (f.reserve.contains(a)) {
				f.reserve.set(f.reserve.indexOf(a), b);
				return;
			}
		}
		for (City c : cities) {
			if (c.hasDefence(a)) {
				c.replaceDefence(a, b);
				return;
			}
		}
	}

	public Empire(JSONObject o, WorldMap wm, HashMap<Integer, LandBlockType>[] mappingRef, InPipe ip) throws IOException {
		id = o.getInt("id");
		name = o.getString("name");
		smartAccum = o.optInt("smartAccum", 0);
		fleetSmartIndex = o.optInt("fleetSmartIndex", 0);
		if (o.optString("playerName", null) != null) {
			playerNames.add(o.getString("playerName"));
		}
		if (o.has("playerNames")) {
			JSONArray a = o.getJSONArray("playerNames");
			for (int i = 0; i < a.length(); i++) {
				playerNames.add(a.getString(i));
			}
		}
		arms = new CoatOfArms(o.getJSONObject("arms"));
		setMoney(o.getInt("money"));
		moneyMs = o.getInt("moneyMs");
		warConsiderDelay = o.optInt("warConsiderDelay", 30000);
		aggressiveness = o.optDouble("aggressiveness", 1.0);
		reputation = o.optInt("reputation", 50);
		repFromCityUpgrades = o.optInt("repFromCityUpgrades", 0);
		repDecayCounter = o.optInt("repDecayCounter", 0);
		moneyMs = o.getInt("moneyMs");
		warConsiderDelay = o.optInt("warConsiderDelay", 30000);
		aggressiveness = o.optDouble("aggressiveness", 1.0);
		numAllianceMembers = o.optInt("numAllianceMembers", 1);
		timeSinceRaided = o.optInt("timeSinceRaided", 120000);
		timeSinceSpiedUpon = o.optInt("timeSinceSpiedUpon", 120000);
		shipIDCounter = o.optInt("shipIDCounter", 1);
		playerControlled = o.getBoolean("playerControlled");
		research = o.has("researchTech") ? Tech.ofName(o.getString("researchTech")).getChoice(o.getString("researchChoice")) : null;
		researchPoints = StrictMath.max(0, o.optInt("researchPoints", 0));
		unassignedResearchPoints = o.optInt("unassignedResearchPoints", 0);
		constructionStrategy = ConstructionStrategy.ofName(o.getString("constructionStrategy"));
		deaths = o.optInt("deaths", 0);
		prefix = o.optString("prefix", null);
		aiConstructionSpent = o.optInt("aiConstructionSpent", 0);
		aiUpgradesSpent = o.optInt("aiUpgradesSpent", 0);
		spyActionFailAccumulator = o.optInt("spyActionFailAccumulator", 0);
		spyDefenceFailAccumulator = o.optInt("spyDefenceFailAccumulator", 0);
		doNotAttackCheat = o.optBoolean("doNotAttackCheat", false);
		timeAtPeace = o.optInt("timeAtPeace", 0);
		ritualSitesVisibleMessageSeen = false;//o.optBoolean("ritualSitesVisibleMessageSeen", true);
		previouslyAwardedClaim = o.optBoolean("previouslyAwardedClaim", false);
		diplomacyAI = new DiplomacyAI(DiplomacyPersonality.ofName(o.optString("diplomacyPersonality", "default")));
		landshipFocus = o.optBoolean("landshipFocus", false);
		sendChallengeCooldown = o.optInt("sendChallengeCooldown", 0);
		timeSinceLastChallenge = o.optInt("timeSinceLastChallenge", 0);
		allianceBreakings = o.optInt("allianceBreakings", 0);
		hasLostTerritory = o.optBoolean("hasLostTerritory", false);
		hasBuiltAirship = o.optBoolean("hasBuiltAirship", false);
		hasBuiltFleshcracker = o.optBoolean("hasBuiltFleshcracker", false);
		hasBuiltMechSquid = o.optBoolean("hasBuiltMechSquid", false);
		bioNestsCleared = o.optInt("bioNestsCleared", 0);
		coronationsSabotaged = o.optInt("coronationsSabotaged", 0);
		easySpyCheat = o.optBoolean("easySpyCheat", false);
		timeSinceLastIncident = o.optInt("timeSinceLastIncident", 0);
		timeSinceHeroAppeared = o.optInt("timeSinceHeroAppeared", 0);
		if (o.has("additionalBoni")) {
			JSONArray a = o.getJSONArray("additionalBoni");
			for (int i = 0; i < a.length(); i++) {
				bonuses.add(Bonus.ofNameOrNone(a.getString(i)));
			}
		}
		bonuses.add(Bonus.ofName("CONQUEST"));
		if (o.has("rewardedBonuses")) {
			JSONArray a = o.getJSONArray("rewardedBonuses");
			for (int i = 0; i < a.length(); i++) {
				rewardedBonuses.add(Bonus.ofNameOrNone(a.getString(i)));
			}
		}
		JSONArray a = o.getJSONArray("cities");
		for (int i = 0; i < a.length(); i++) {
			cities.add(new City(a.getJSONObject(i), mappingRef, ip, bonuses));
		}
		a = o.getJSONArray("fleets");
		for (int i = 0; i < a.length(); i++) {
			fleets.add(new Fleet(a.getJSONObject(i), wm, ip, bonuses));
		}
		if (o.has("techs")) {
			a = o.getJSONArray("techs");
			for (int i = 0; i < a.length(); i++) {
				JSONArray el = a.getJSONArray(i);
				techs.add(Tech.ofName(el.getString(0)).getChoice(el.getString(1)));
			}
		}
		if (o.has("techEverResearched")) {
			a = o.getJSONArray("techEverResearched");
			for (int i = 0; i < a.length(); i++) {
				JSONArray el = a.getJSONArray(i);
				techEverResearched.add(Tech.ofName(el.getString(0)).getChoice(el.getString(1)));
			}
		}
		if (o.has("researchQueue")) {
			a = o.getJSONArray("researchQueue");
			for (int i = 0; i < a.length(); i++) {
				JSONArray el = a.getJSONArray(i);
				researchQueue.add(Tech.ofName(el.getString(0)).getChoice(el.getString(1)));
			}
		}
		if (o.has("partialResearchPoints")) {
			JSONObject prp = o.getJSONObject("partialResearchPoints");
			for (Object k : prp.keySet()) {
				Tech.Choice c = Tech.choiceOfName((String) k);
				partialResearchPoints.put(c, prp.getInt((String) k));
			}
		}
				
		if (o.has("expedition")) {
			expeditionTime = o.getInt("expeditionTime");
			a = o.getJSONArray("expedition");
			expedition = new ArrayList<Airship>();
			for (int i = 0; i < a.length(); i++) {
				expedition.add(new Airship(ip.read(a.getString(i)), bonuses));
			}
			expeditionEraModifier = EraModifier.ofName(o.getString("expeditionEraModifier"));
		}
		
		if (o.has("diplomacyPersonality")) {
			diplomacyAI = new DiplomacyAI(DiplomacyPersonality.ofName(o.getString("diplomacyPersonality")));
			diplomacyAI.diplomacyCooldown = o.optInt("diplomacyCooldown", 0);
			diplomacyAI.accedeToUltimatumCooldown = o.optInt("accedeToUltimatumCooldown", 0);
		}
		
		if (o.has("mapClr")) {
			mapClr = new Clr(o.getJSONObject("mapClr").getInt("r"), o.getJSONObject("mapClr").getInt("g"), o.getJSONObject("mapClr").getInt("b"));
		} else {
			mapClr = arms.getMapColors().get(0);
		}
		
		if (o.has("incidentPeriodMultipliers")) {
			a = o.getJSONArray("incidentPeriodMultipliers");
			for (int i = 0; i < a.length(); i += 2) {
				incidentPeriodMultipliers.put(a.getString(i), a.getInt(i + 1));
			}
		}
		if (o.has("previousIncidents")) {
			a = o.getJSONArray("previousIncidents");
			for (int i = 0; i < a.length(); i++) {
				previousIncidents.add(a.getString(i));
			}
		}
		
		mapColor = toColor(mapClr, 50);
		mapColor2 = toColor(mapClr, 30);
		
		/*
		a = new JSONArray();
		for (Medal md : medalDesigns) {
			if (md == null) {
				a.put(new JSONObject());
			} else {
				a.put(md.toJSON());
			}
		}
		o.put("medalDesigns", a);
		a = new JSONArray();
		for (ArrayList<String> lma : medalAwards) {
			JSONArray a2 = new JSONArray();
			for (String s : lma) { a2.put(s); }
			a.put(a2);
		}
		o.put("medalAwards", a);
		*/
		if (o.has("medalDesigns")) {
			a = o.getJSONArray("medalDesigns");
			for (int i = 0; i < a.length(); i++) {
				JSONObject o2 = a.getJSONObject(i);
				if (o2.has("charge")) {
					medalDesigns.add(new Medal(o2));
				} else {
					medalDesigns.add(null);
				}
			}
		}
		if (o.has("medalAwards")) {
			a = o.getJSONArray("medalAwards");
			for (int i = 0; i < a.length(); i++) {
				JSONArray a2 = a.getJSONArray(i);
				ArrayList<String> l = new ArrayList<String>();
				for (int j = 0; j < a2.length(); j++) {
					l.add(a2.getString(j));
				}
				medalAwards.add(l);
			}
		}
	}
	
	public static Color toColor(Clr c, int a) {
		return new Color(c.r, c.g, c.b, a);
	}

	public void finish(JSONObject o, WorldMap m) {
		JSONArray a = o.getJSONArray("cities");
		for (int i = 0; i < a.length(); i++) {
			cities.get(i).finish(a.getJSONObject(i), m);
		}
		if (o.has("expeditionDepartureCity")) {
			expeditionDepartureCity = m.getCity(o.getInt("expeditionDepartureCity"));
		}
		a = o.optJSONArray("spies");
		if (a != null) {
			for (int i = 0; i < a.length(); i++) {
				Spy spy = new Spy(a.getJSONObject(i));
				spy.finish(a.getJSONObject(i), m);
				spies.add(spy);
			}
		}
		a = o.optJSONArray("cityAggressiveness");
		if (a != null) {
			for (int i = 0; i < a.length(); i++) {
				JSONObject entry = a.getJSONObject(i);
				MapLocation ml = m.getMapLocation(entry.getInt("city"));
				if (ml == null) {
					AirshipGame.report("null MapLocation while loading, id was " + entry.getInt("city"));
				} else {
					locAggressiveness.put(ml, entry.getDouble("multiplier"));
				}
			}
		}
		if (o.has("aiTargets")) {
			a = o.getJSONArray("aiTargets");
			aiTargets = new ArrayList<MapLocation>();
			for (int i = 0; i < a.length(); i++) {
				aiTargets.add(m.getMapLocation(a.getInt(i)));
			}
			a = o.getJSONArray("citiesForAITargets");
			citiesForAITargets = new ArrayList<City>();
			for (int i = 0; i < a.length(); i++) {
				citiesForAITargets.add(m.getCity(a.getInt(i)));
			}
			warHashForAITargets = o.optInt("warHashForAITargets", 0);
		}
		if (o.has("diplomacyRecords")) {
			JSONArray records = o.getJSONArray("diplomacyRecords");
			for (int i = 0; i < records.length(); i++) {
				DiplomacyAI.EmpireRecord rec = diplomacyAI.rec(records.getJSONObject(i), m);
				diplomacyAI.records.put(rec.e, rec);
			}
		}
		if (o.has("activeEmpire")) {
			diplomacyAI.activeEmpire = m.getEmpire(o.getInt("activeEmpire"));
		}
		if (o.has("besiegeReqs")) {
			a = o.getJSONArray("besiegeReqs");
			for (int i = 0; i < a.length(); i++) {
				besiegeRequests.add(new BesiegeRequest(a.getJSONObject(i), m));
			}
		}
	}
	
	public void finishFleets(JSONObject o, WorldMap m) {
		JSONArray a = o.getJSONArray("fleets");
		for (int i = 0; i < a.length(); i++) {
			getFleets().get(i).finish(a.getJSONObject(i), m);
		}
	}

	public JSONObject toJSON(WorldMap m, OutPipe op) {
		JSONObject o = new JSONObject()
				.put("id", id)
				.put("name", name)
				.put("smartAccum", smartAccum)
				.put("fleetSmartIndex", fleetSmartIndex)
				.put("arms", arms.toJSON())
				.put("money", getMoney())
				.put("moneyMs", moneyMs)
				.put("reputation", reputation)
				.put("repFromCityUpgrades", repFromCityUpgrades)
				.put("aggressiveness", aggressiveness)
				.put("warConsiderDelay", warConsiderDelay)
				.put("constructionStrategy", constructionStrategy.name)
				.put("numAllianceMembers", numAllianceMembers)
				.put("timeSinceRaided", timeSinceRaided)
				.put("timeSinceSpiedUpon", timeSinceSpiedUpon)
				.put("shipIDCounter", shipIDCounter)
				.put("playerControlled", playerControlled)
				.put("researchPoints", researchPoints)
				.put("unassignedResearchPoints", unassignedResearchPoints)
				.put("deaths", deaths)
				.put("aiConstructionSpent", aiConstructionSpent)
				.put("aiUpgradesSpent", aiUpgradesSpent)
				.put("spyActionFailAccumulator", spyActionFailAccumulator)
				.put("spyDefenceFailAccumulator", spyDefenceFailAccumulator)
				.put("repDecayCounter", repDecayCounter)
				.put("previouslyAwardedClaim", previouslyAwardedClaim)
				.put("doNotAttackCheat", doNotAttackCheat)
				.put("landshipFocus", landshipFocus)
				.put("timeAtPeace", timeAtPeace)
				//.put("ritualSitesVisibleMessageSeen", ritualSitesVisibleMessageSeen)
				.put("sendChallengeCooldown", sendChallengeCooldown)
				.put("timeSinceLastChallenge", timeSinceLastChallenge)
				.put("allianceBreakings", allianceBreakings)
				.put("hasLostTerritory", hasLostTerritory)
				.put("hasBuiltAirship", hasBuiltAirship)
				.put("hasBuiltFleshcracker", hasBuiltFleshcracker)
				.put("hasBuiltMechSquid", hasBuiltMechSquid)
				.put("bioNestsCleared", bioNestsCleared)
				.put("coronationsSabotaged", coronationsSabotaged)
				.put("easySpyCheat", easySpyCheat)
				.put("timeSinceLastIncident", timeSinceLastIncident)
				.put("timeSinceHeroAppeared", timeSinceHeroAppeared);
		
		o.put("mapClr", new JSONObject().put("r", mapClr.r).put("g", mapClr.g).put("b", mapClr.b));
		
		if (diplomacyAI != null) {
			o.put("diplomacyPersonality", diplomacyAI.personality.name);
			o.put("diplomacyCooldown", diplomacyAI.diplomacyCooldown);
			o.put("accedeToUltimatumCooldown", diplomacyAI.accedeToUltimatumCooldown);
			JSONArray records = new JSONArray();
			ArrayList<Empire> keys = new ArrayList<Empire>(diplomacyAI.records.keySet());
			Collections.sort(keys);
			for (Empire e : keys) {
				if (m.empires.contains(e)) {
					records.put(diplomacyAI.records.get(e).toJSON());
				}
			}
			o.put("diplomacyRecords", records);
			if (diplomacyAI.activeEmpire != null) {
				o.put("activeEmpire", diplomacyAI.activeEmpire.id);
			}
		}

		if (prefix != null) { o.put("prefix", prefix); }
		
		ArrayList<String> ks = new ArrayList<String>(incidentPeriodMultipliers.keySet());
		Collections.sort(ks);
		JSONArray a = new JSONArray();
		o.put("incidentPeriodMultipliers", a);
		for (String k : ks) {
			a.put(k);
			a.put(incidentPeriodMultipliers.get(k));
		}
		a = new JSONArray();
		o.put("previousIncidents", a);
		for (String s : previousIncidents) {
			a.put(s);
		}

		JSONObject prp = new JSONObject();
		ArrayList<Tech.Choice> partials = new ArrayList<Tech.Choice>(partialResearchPoints.keySet());
		Collections.sort(partials);
		for (Tech.Choice p : partials) {
			prp.put(p.name, partialResearchPoints.get(p));
		}
		o.put("partialResearchPoints", prp);
		
		a = new JSONArray();
		o.put("playerNames", a);
		for (String n : playerNames) {
			a.put(n);
		}

		if (research != null) {
			o.put("researchTech", research.tech.name);
			o.put("researchChoice", research.name);
		}
		a = new JSONArray();
		o.put("cities", a);
		for (City c : cities) {
			a.put(c.toJSON(m, op));
		}
		a = new JSONArray();
		o.put("fleets", a);
		for (Fleet f : getFleets()) {
			a.put(f.toJSON(m, op));
		}
		a = new JSONArray();
		o.put("techs", a);
		for (Tech.Choice t : techs) {
			JSONArray el = new JSONArray();
			el.put(t.tech.name);
			el.put(t.name);
			a.put(el);
		}
		a = new JSONArray();
		o.put("techEverResearched", a);
		for (Tech.Choice t : techEverResearched) {
			JSONArray el = new JSONArray();
			el.put(t.tech.name);
			el.put(t.name);
			a.put(el);
		}
		a = new JSONArray();
		o.put("researchQueue", a);
		for (Tech.Choice t : researchQueue) {
			JSONArray el = new JSONArray();
			el.put(t.tech.name);
			el.put(t.name);
			a.put(el);
		}
		a = new JSONArray();
		o.put("spies", a);
		for (Spy s : spies) {
			a.put(s.toJSON(m));
		}
		a = new JSONArray();
		o.put("cityAggressiveness", a);
		ArrayList<MapLocation> aggL = new ArrayList<MapLocation>(locAggressiveness.keySet());
		Collections.sort(aggL, new MapLocationCmp());
		for (MapLocation l : aggL) {
			a.put(new JSONObject().put("city", l.id).put("multiplier", locAggressiveness.get(l)));
		}
		a = new JSONArray();
		o.put("additionalBoni", a);
		for (Bonus b : bonuses.list()) {
			a.put(b.name());
		}
		a = new JSONArray();
		o.put("rewardedBonuses", a);
		for (Bonus b : rewardedBonuses.list()) {
			a.put(b.name());
		}
		if (aiTargets != null) {
			a = new JSONArray();
			o.put("aiTargets", a);
			for (MapLocation c : aiTargets) {
				a.put(c.id);
			}
			a = new JSONArray();
			o.put("citiesForAITargets", a);
			for (City c : citiesForAITargets) {
				a.put(c.id);
			}
			o.put("warHashForAITargets", warHashForAITargets);
		}
		if (expedition != null) {
			a = new JSONArray();
			for (Airship s : expedition) {
				a.put(s.networkID);
				op.register(this, s.networkID, s.version);
			}
			o.put("expedition", a);
			o.put("expeditionTime", expeditionTime);
			o.put("expeditionEraModifier", expeditionEraModifier.name);
			if (expeditionDepartureCity != null) {
				o.put("expeditionDepartureCity", expeditionDepartureCity.id);
			}
		}
		a = new JSONArray();
		for (BesiegeRequest br : besiegeRequests) {
			a.put(br.toJSON());
		}
		o.put("besiegeReqs", a);
		
		a = new JSONArray();
		for (Medal md : medalDesigns) {
			if (md == null) {
				a.put(new JSONObject());
			} else {
				a.put(md.toJSON());
			}
		}
		o.put("medalDesigns", a);
		a = new JSONArray();
		for (ArrayList<String> lma : medalAwards) {
			JSONArray a2 = new JSONArray();
			for (String s : lma) { a2.put(s); }
			a.put(a2);
		}
		o.put("medalAwards", a);
		
		return o;
	}
	
	@Override
	public JSONObject write(String writeID) {
		if (expedition != null) {
			for (Airship ship : expedition) {
				if (writeID.equals(ship.networkID)) {
					return ship.toJSON(null, /* storeBonuses */ false);
				}
			}
		}
		throw new RuntimeException(writeID);
	}

	@Override
	public BonusSet bonuses() {
		return bonuses;
	}

	public boolean hasBonus(Bonus b) {
		return bonuses.contains[b.ordinal()];
	}
	
	@Override
	public int globalSupply() {
		int sup = 0;
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			for (int j = 0; j < c.upgrades.size(); j++) {
				sup += c.upgrades.get(j).globalSupply.get(bonuses);
			}
		}
		return sup;
	}
	
	public int totalIncome(WorldMap wm) {
		return EmpireStat.EMPIRE_BASE_INCOME.get(bonuses) + cityIncome(wm) + tradeIncome(wm) + tributeIncome(wm);
	}
	
	public int incomeBalance(WorldMap wm) {
		return totalIncome(wm) - totalFleetCosts() - totalDefencesCost(wm) - totalUpgradeMaintenanceCosts() - coronationCost(wm) - finalRitualCost(wm) - tributeCost(wm) - heroesCost(wm);
	}
	
	public int nonConstructionIncomeBalance(WorldMap wm) {
		return totalIncome(wm) - totalUpgradeMaintenanceCosts() - coronationCost(wm) - finalRitualCost(wm) - tributeCost(wm) - heroesCost(wm);
	}
	
	public int totalUpgradeMaintenanceCosts() {
		int cost = 0;
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			for (int j = 0; j < c.upgrades.size(); j++) {
				cost += c.upgrades.get(j).maintenance.get(bonuses);
			}
		}
		return cost;
	}
	
	public int researchOutput(WorldMap wm, StringBuilder explain, boolean includeResearchFromTreaties) {
		int r = EmpireStat.RESEARCH.get(bonuses);
		ArrayList<CityUpgradeType> cuts = explain != null ? Loadable.all(CityUpgradeType.class) : null;
		int[] cutNums = explain != null ? new int[cuts.size()] : null;
		ArrayList<Edict> edicts = explain != null ? Loadable.all(Edict.class) : null;
		int[] edictNums = explain != null ? new int[edicts.size()] : null;
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			if (c.takeoverNeeded || c.takeoverMethod != null) { continue; }
			for (int j = 0; j < c.upgrades.size(); j++) {
				if (c.upgrades.get(j).research.get(bonuses) != 0) {
					r += c.upgrades.get(j).research.get(bonuses);
					if (explain != null) {
						cutNums[cuts.indexOf(c.upgrades.get(j))]++;
					}
				}
			}
			Hero h = Hero.get(c, wm);
			if (h != null) {
				r += h.type.research;
			}
			if (c.edict != null) {
				r += c.edict.research;
				if (explain != null) {
					edictNums[edicts.indexOf(c.edict)]++;
				}
			}
		}
		if (explain != null) {
			explain.append(_t("research_base_")).append(EmpireStat.RESEARCH.get(bonuses));
			for (int i = 0; i < cuts.size(); i++) {
				CityUpgradeType cut = cuts.get(i);
				if (cut.research.get(bonuses) != 0 && cutNums[i] != 0) {
					explain.append("\n").append(cut.getName()).append(cut.research.get(bonuses) > 0 ? ": +" : ": ").append(cut.research.get(bonuses) * cutNums[i]);
				}
			}
			for (City c : cities) {
				Hero h = Hero.get(c, wm);
				if (h != null && h.type.research != 0) {
					explain.append("\n").append(h.getName()).append(h.type.research > 0 ? ": +" : ": ").append(h.type.research);
				}
			}
			for (int i = 0; i < edicts.size(); i++) {
				Edict edict = edicts.get(i);
				if (edict.research != 0 && edictNums[i] != 0) {
					explain.append("\n").append(edict.getName()).append(edict.research > 0 ? ": +" : ": ").append(edict.research * edictNums[i]);
				}
			}
			if (wm.techSpeed.speedMultiplier != 1) {
				explain.append("\n").append(_t("Tech_speed_")).append(_t("techspeedsetting_" + wm.techSpeed.name)).append(": x").append(wm.techSpeed.speedMultiplier);
			}
		}
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			if (c.takeoverNeeded || c.takeoverMethod != null) { continue; }
			int cityResearch = 0;
			for (int j = 0; j < c.upgrades.size(); j++) {
				if (c.upgrades.get(j).research.get(bonuses) != 0) {
					cityResearch += c.upgrades.get(j).research.get(bonuses);
				}
			}
			Hero h = Hero.get(c, wm);
			if (h != null && h.type.researchPercent != 0) {
				int amt = cityResearch * h.type.researchPercent / 100;
				r += amt;
				if (amt != 0 && explain != null) {
					explain.append("\n").append(h.getName()).append(": ").append(amt > 0 ? "+" : "").append(Math.abs(amt));
				}
			}
		}
		r = (int) (r * wm.techSpeed.speedMultiplier);
		if (!playerControlled) {
			r = (int) (r * wm.difficulty.aiResearchMultiplier);
		}
		if (includeResearchFromTreaties) {
			for (Relationship rel : wm.getRelationships(this)) {
				if (rel.researchTreaty) {
					int amt = rel.other(this).researchOutput(wm, null, false) * EmpireStat.RESEARCH_TREATY_RESEARCH_GAINED_PERCENTAGE.get(bonuses) / 100;
					if (amt > 0) {
						r += amt;
						if (explain != null) {
							explain.append("\n").append(_t("research_treaty_with_x_", rel.other(this).getName())).append(amt);
						}
					}
				}
			}
		}
		if (explain != null) { explain.append("\n").append(_t("research_total_")).append(r); }
		return r;
	}
	
	public int totalFleetCosts() {
		int c = 0;
		for (Fleet f : getFleets()) {
			c += f.maintenanceCost();
		}
		return c;
	}
	
	public int totalDefencesCost(WorldMap m) {
		int cost = 0;
		for (City c : cities) {
			cost += c.defencesMaintenanceCost(this, m, null);
		}
		return cost;
	}
	
	public int coronationCost(WorldMap wm) {
		if (isCoronating()) {
			return getCoronationCost(wm);
		}
		return 0;
	}
	
	public int finalRitualCost(WorldMap wm) {
		if (isDoingFinalRitual()) {
			return getFinalRitualCost(wm);
		}
		return 0;
	}
	
	public int cityIncome(WorldMap wm) {
		int in = 0;
		for (City c : cities) {
			in += c.adjustedIncome(wm, null);
		}
		if (!playerControlled) {
			in *= wm.difficulty.aiIncomeMultiplier(wm.age);
		}
		return in;
	}
	
	private int forTradeCityIncome(WorldMap wm) {
		int in = 0;
		for (City c : cities) {
			in += c.adjustedIncome(wm, null);
		}
		return in;
	}
	
	public int tradeIncome(WorldMap wm) {
		int in = 0;
		for (Relationship rel : wm.getRelationships(this)) {
			if (rel.tradeTreaty) {
				in += rel.other(this).forTradeCityIncome(wm) * EmpireStat.TRADE_TREATY_INCOME_GAINED_PERCENTAGE.get(bonuses) / 100;
			}
			if (rel.level == Relationship.Level.DEFENSIVE_PACT) {
				in += rel.other(this).forTradeCityIncome(wm) * EmpireStat.DEFENSIVE_PACT_INCOME_GAINED_PERCENTAGE.get(bonuses) / 100;
			}
			if (rel.level == Relationship.Level.ALLIANCE) {
				in += rel.other(this).forTradeCityIncome(wm) * EmpireStat.ALLIANCE_INCOME_GAINED_PERCENTAGE.get(bonuses) / 100;
			}
		}
		return in;
	}
	
	public int tributeIncome(WorldMap wm) {
		int in = 0;
		for (Relationship rel : wm.getRelationships(this)) {
			if (rel.getReceivingTribute(this)) {
				in += rel.other(this).cityIncome(wm) * EmpireStat.TRIBUTE_INCOME_PAID_PERCENTAGE.get(bonuses) / 100;
			}
		}
		return in;
	}
	
	public int tributeCost(WorldMap wm) {
		int base = cityIncome(wm);
		int cost = 0;
		for (Relationship rel : wm.getRelationships(this)) {
			if (rel.getSendingTribute(this)) {
				cost += base * EmpireStat.TRIBUTE_INCOME_PAID_PERCENTAGE.get(rel.other(this).bonuses) / 100;
			}
		}
		return cost;
	}
	
	public int heroesCost(WorldMap wm) {
		int cost = 0;
		for (Hero h : Hero.getHeroes(this, wm)) {
			if (h.hired) {
				cost += h.type.maintenance;
			}
		}
		return cost;
	}
	
	public int expeditionCost(ArrayList<Airship> ships) {
		int mc = 0;
		for (int i = 0; i < ships.size(); i++) {
			mc += ships.get(i).maintenanceCost();
		}
		return mc * EmpireStat.EXPEDITION_MS.get(bonuses) / MS_PER_INCOME;
	}
	
	public int expeditionStrength(ArrayList<Airship> ships, WorldMap m, StringBuilder sb) {
		int cost = 0;
		ArrayList<Hero> heroes = new ArrayList<Hero>();
		for (Airship s : ships) {
			Hero h = Hero.get(s, m);
			if (h != null && h.type.expeditionStrengthPercent != 0) {
				heroes.add(h);
			}
			cost += s.getCost();
		}
		
		if (!heroes.isEmpty()) {
			if (sb != null) {
				sb.append("\n\n").append(_t("hero_expeditionNotice", Airship.nameList(heroes, Lang.currentLocale)));
				sb.append("\n\n").append(_t("AIRSHIP_plural")).append(": ").append(cost);
			}
			int percentage = 100;
			for (Airship s : ships) {
				Hero h = Hero.get(s, m);
				if (h != null && h.type.expeditionStrengthPercent != 0) {
					percentage += h.type.expeditionStrengthPercent;
					if (sb != null) {
						sb.append("\n").append(h.getName()).append(h.type.expeditionStrengthPercent > 0 ? ": +" : ": ").append(h.type.expeditionStrengthPercent).append("%");
					}
				}
			}
			cost = cost * percentage / 100;
			if (sb != null) { sb.append("\n").append(_t("quality_total")).append(cost); }
		}
		
		return cost;
	}
	
	public void sendExpedition(ArrayList<Airship> ships, Fleet f, WorldMap m) {
		if (expedition != null) { return; }
		if (ships.containsAll(f.actives) && ships.containsAll(f.reserve)) {
			fleets.remove(f);
			f.broadcastDestroyed(m);
		} else {
			f.actives.removeAll(ships);
			f.reserve.removeAll(ships);
		}
		expedition = ships;
		expeditionTime = 0;
		expeditionDepartureCity = f.location instanceof City ? (City) f.location : null;
		expeditionEraModifier = m.eraModifier;
		setMoney(getMoney() - expeditionCost(ships));
	}

	public void tick(int ms, WorldMap map) {
		// Update bonuses given by city upgrades and world.
		ArrayList<CityUpgradeType> cuts = Loadable.all(CityUpgradeType.class);
		for (int i = 0; i < cuts.size(); i++) {
			if (cuts.get(i).gives != null) {
				bonuses.set(cuts.get(i).gives.ordinal(), false);
			}
			
			Tech.Choice tech = cuts.get(i).givesTech;
			if (tech != null && !techs.contains(tech) && has(cuts.get(i))) {
				if (EHeroes.it.enabled) {
					HeroEvent evt = HeroEvent.techResearched(this, tech);
					heroAppendix += Hero.getStatChangeAppendix(this, evt, map, true);
					Hero.processHeroEvent(evt, map);
				}
				if (research == tech) {
					researchPoints += research.cost(this, map);
					checkResearchComplete(map);
				} else {
					techs.add(tech);
					bonuses.addAll(tech.bonuses);
					researchedTech = tech;
				}
			}
		}
		ArrayList<HeroType> hts = Loadable.all(HeroType.class);
		for (int i = 0; i < hts.size(); i++) {
			if (hts.get(i).bonus != null) {
				bonuses.set(hts.get(i).bonus.ordinal(),  false);
			}
		}
		// Add back in rewarded bonuses, to make sure.
		bonuses.addAll(rewardedBonuses);
		
		timeSinceLastIncident += ms;
		timeSinceHeroAppeared += ms;
		
		for (int ci = 0; ci < cities.size(); ci++) {
			City c = cities.get(ci);
			if (c.takeoverNeeded || c.takeoverMethod != null) { continue; }
			for (int ui = 0; ui < c.upgrades.size(); ui++) {
				CityUpgradeType cut = c.upgrades.get(ui);
				if (cut.gives != null) {
					bonuses.set(cut.gives.ordinal(), true);
				}
			}
		}
		ArrayList<Hero> hs = Hero.getHeroes(this, map);
		for (int i = 0; i < hs.size(); i++) {
			if (hs.get(i).hired && hs.get(i).type.bonus != null) {
				bonuses.set(hs.get(i).type.bonus.ordinal(), true);
			}
		}
		for (int i = 0; i < hs.size(); i++) {
			Hero h = hs.get(i);
			for (int j = 0; j < h.type.stats.size(); j++) {
				Stat stat = h.type.stats.get(j);
				if ((stat.loseOn0 && h.stats.get(stat.name) == 0) || (stat.loseOn100 && h.stats.get(stat.name) == 100)) {
					HashMap<City, Empire> newOwners = new HashMap<City, Empire>();
					for (City c : cities) {
						Empire newOwner = null;
						double closest = 0;
						for (City c2 : map.cities()) {
							if (cities.contains(c2)) { continue; }
							double dist = c.dist(c2);
							if (newOwner == null || dist < closest) {
								newOwner = map.owner(c2);
								closest = dist;
							}
						}
						newOwners.put(c, newOwner);
					}
					for (City c : new ArrayList<City>(cities)) {
						if (newOwners.containsKey(c)) {
							newOwners.get(c).cities.add(c);
							cities.remove(c);
							c.constructing.clear();
							c.takeoverAmount = 0;
							c.takeoverMethod = null;
							c.cancelCoronation((Empire) this, /* intentionally */ false);
						}
					}
					map.clearPathCaches();
					map.isAdjacents.clear();
					return;
				}
			}
		}
		
		ArrayList<StrategicEra> eras = Loadable.all(StrategicEra.class);
		for (int i = 0; i < eras.size(); i++) {
			bonuses.set(eras.get(i).bonus.ordinal(), false);
		}
		if (!map.era.bonus.name.equals("NO_BONUS")) {
			bonuses.set(map.era.bonus.ordinal(), true);
		}
		ArrayList<EraModifier> modifiers = Loadable.all(EraModifier.class);
		for (int i = 0; i < modifiers.size(); i++) {
			bonuses.set(modifiers.get(i).bonus.ordinal(), false);
		}
		if (!map.eraModifier.name.equals("NO_BONUS")) {
			bonuses.set(map.eraModifier.bonus.ordinal(), true);
		}
		
		bonuses.set(Bonus.ofName("BROKE").ordinal(), money <= 0);
		
		// Check besiege requests for fulfilment and validity
		lp: for (int i = 0; i < besiegeRequests.size(); i++) {
			BesiegeRequest req = besiegeRequests.get(i);
			for (int j = 0; j < fleets.size(); j++) {
				Fleet f = fleets.get(j);
				if (f.destination == req.city && f.besiege) {
					req.from.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.BESIEGE_REQUEST_GRANTED, this, req.city));
					besiegeRequests.remove(i);
					i--;
					continue lp;
				}
			}
			if (!map.empires.contains(req.from) || map.getRelationship(this, req.from).level != Relationship.Level.ALLIANCE || map.getRelationship(this, map.owner(req.city)).level != Relationship.Level.WAR) {
				besiegeRequests.remove(i);
				i--;
			}
		}
		
		if (map.toggles.contains(ConquestToggle.REPUTATION)) {
			// Check if our rep changed due to city upgrades.
			int upgradesRep = 0;
			for (City c : cities) {
				for (CityUpgradeType cut : cuts) {
					if (c.upgrades.contains(cut)) {
						upgradesRep += cut.reputation.get(bonuses);
					}
				}
			}
			int change = upgradesRep - repFromCityUpgrades;
			if (change != 0) {
				changeReputation(change, map);
			}
			repFromCityUpgrades = upgradesRep;
			
			repDecayCounter += ms;
			int repDecayEvery = EmpireStat.TIME_PER_REP_DECAY.get(bonuses());
			if (repDecayCounter >= repDecayEvery) {
				repDecayCounter -= repDecayEvery;
				if (reputation < 19) {
					reputation++;
				} else if (reputation > 19 && reputation < 39) {
					reputation++;
				} else if (reputation > 39 && reputation < 50) {
					reputation++;
				} else if (reputation > 50 && reputation < 60) {
					reputation--;
				} else if (reputation > 60 && reputation < 80) {
					reputation--;
				} else if (reputation > 80) {
					reputation--;
				}
			}
		}
		
		sendChallengeCooldown = StrictMath.max(0, sendChallengeCooldown - ms);
		timeSinceLastChallenge += ms;
		timeAtPeace += ms;
		for (Relationship rel : map.getRelationships(this)) {
			if (rel.level == Relationship.Level.WAR) {
				timeAtPeace = 0;
				break;
			}
		}
		
		if (expedition != null) {
			expeditionTime += ms;
			if (expeditionTime >= EmpireStat.EXPEDITION_MS.get(bonuses)) {
				int winningFleetCost = expeditionStrength(expedition, map, null);
				City returnTo = cities.contains(expeditionDepartureCity) ? expeditionDepartureCity : getCapital();
				Fleet f = map.getGarrison(returnTo);
				if (f == null) {
					f = new Fleet(returnTo, map);
					fleets.add(f);
				}
				f.reserve.addAll(expedition);
				returnTo.layoutGarrison(f);
				expedition = null;
				Reward.giveRewardTo(expeditionEraModifier.expeditions.get(bonuses), this, map, f, winningFleetCost, "");
			}
		}

		timeSinceRaided += ms;
		timeSinceSpiedUpon += ms;
		int csz = cities.size();
		for (int ci = 0; ci < csz; ci++) {
			City c = cities.get(ci);
			c.tick(ms, this, map);
		}
		
		moneyMs += ms;
		if (moneyMs >= MS_PER_INCOME) {
			moneyMs -= MS_PER_INCOME;
			setMoney(getMoney() + incomeBalance(map));
		}
		if (research != null) {
			researchPoints += researchOutput(map, null, true) * ms;
			checkResearchComplete(map);
			if (research != null) {
				if (researchPoints >= research.cost(this, map)) {
					for (Tech.Choice c : research.tech.choices) {
						techs.remove(c);
						bonuses.removeAll(c.bonuses);
					}
					bonuses.addAll(research.bonuses);
					techs.add(research);
					researchPoints = 0;
					researchedTech = research;
					research = null;
				}
			}
		}
		if (getMoney() < 0) { setMoney(0); }
		
		if (isCoronating() && !isCoronationReady(map)) {
			for (int ci = 0; ci < csz; ci++) {
				City c = cities.get(ci);
				c.cancelCoronation(null, /* intentionally */ false);
			}
		}
		int fsz = fleets.size();
		for (int fi = 0; fi < fsz; fi++) {
			Fleet f = fleets.get(fi);
			if (f.tick(ms, this, map)) {
				fleets.remove(fi);
				fi--;
				fsz--;
				f.broadcastDestroyed(map);
			}
		}
		int maxSpies = EmpireStat.MAX_SPIES.get(bonuses);
		while (spies.size() > maxSpies) {
			Spy least = spies.get(0);
			for (int si = 0; si < spies.size(); si++) {
				if (spies.get(si).networkLevel <= least.networkLevel) {
					least = spies.get(si);
				}
			}
			spies.remove(least);
		}
		int ssz = spies.size();
		for (int si = 0; si < ssz; si++) {
			Spy s = spies.get(si);
			if (cities.contains(s.location)) {
				spies.remove(si);
				si--;
				ssz--;
			} else {
				s.tick(ms, this, map);
			}
		}
		warConsiderDelay -= ms;
	}
	
	public void checkResearchComplete(WorldMap map) {
		/*for (Tech t : Loadable.all(Tech.class)) {
			for (Tech.Choice c : t.choices) {
				if (partialResearchPoints.containsKey(c)) {
					partialResearchPoints.put(c, StrictMath.min(partialResearchPoints.get(c), c.cost(this, map)));
				}
			}
		}*/
		while (research != null && researchPoints >= research.cost(this, map)) {
			for (Tech.Choice c : research.tech.choices) {
				techs.remove(c);
				bonuses.removeAll(c.bonuses);
			}
			bonuses.addAll(research.bonuses);
			techs.add(research);
			researchPoints -= research.cost(this, map);
			unassignedResearchPoints += researchPoints;
			researchPoints = 0;
			partialResearchPoints.remove(research);
			researchedTech = research;
			if (!techEverResearched.contains(research)) {
				Hero.processHeroEvent(HeroEvent.techResearched(this, research), map);
				techEverResearched.add(research);
			}
			research = null;
			if (!researchQueue.isEmpty()) {
				research = researchQueue.get(0);
				researchQueue.remove(0);
				researchPoints = partialResearchPoints.containsKey(research) ? partialResearchPoints.get(research) : 0;
				researchPoints += unassignedResearchPoints;
				unassignedResearchPoints = 0;
			}
		}
		// Maybe we acquired a tech in our queue.
		for (int i = 0; i < researchQueue.size(); i++) {
			if (techs.contains(researchQueue.get(i))) {
				partialResearchPoints.remove(researchQueue.get(i));
				researchQueue.remove(i);
				i--;
			}
		}
	}

	public void addShipAt(Airship ship, City location, WorldMap wm) {
		for (Fleet f : getFleets()) {
			if (f.location == location) {
				f.reserve.add(ship);
				location.layoutGarrison(f);
				return;
			}
		}
		Fleet f = new Fleet(location, wm);
		f.reserve.add(ship);
		getFleets().add(f);
		location.layoutGarrison(f);
	}
	
	public boolean isAllResearchDone(WorldMap wm) {
		if (wm.techSpeed.speedMultiplier == 0) { return true; }
		ArrayList<Tech> allTechs = Loadable.all(Tech.class);
		int atsz = allTechs.size();
		lp: for (int ati = 0; ati < atsz; ati++) {
			Tech at = allTechs.get(ati);
			if (!at.visible(this)) { continue; }
			int tsz = techs.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tech.Choice c = techs.get(ti);
				if (c.tech == at) {
					continue lp;
				}
			}
			return false;
		}
		return true;
	}

	public final int getMoney() {
		return (kestrelNetworkHashIn ^ kestrelNetworkHashOut) + money - money;
	}

	public final void setMoney(int money) {
		this.money = money;
		kestrelNetworkHashIn = AGame.ANIM_R.nextInt();
		kestrelNetworkHashOut = money ^ kestrelNetworkHashIn;
	}
	
	// Some spy checks, may get moved
	public boolean spyCanSeeInsides(City c, WorldMap m) {
		if (EmpireStat.CAN_SEE_EVERYTHING.get(bonuses)) { return true; }
		Spy localSpy = getSpyFor(c);
		if (localSpy != null && localSpy.networkLevel >= EmpireStat.SPY_CAN_SEE_LOCAL_INSIDES.get(bonuses)) {
			return true;
		}
		Empire victim = m.owner(c);
		for (int i = 0; i < victim.cities.size(); i++) {
			City c2 = victim.cities.get(i);
			if (c2.isTown) { continue; }
			Spy remoteSpy = getSpyFor(c2);
			if (remoteSpy != null && remoteSpy.networkLevel >= EmpireStat.SPY_CAN_SEE_ALL_INSIDES.get(bonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean spyCanViewCity(City c, WorldMap m) {
		if (EmpireStat.CAN_SEE_EVERYTHING.get(bonuses)) { return true; }
		Spy localSpy = getSpyFor(c);
		if (localSpy != null && localSpy.infiltrationTimeout <= 0) {
			return true;
		}
		Empire victim = m.owner(c);
		for (int i = 0; i < victim.cities.size(); i++) {
			City c2 = victim.cities.get(i);
			if (c2.isTown) { continue; }
			Spy remoteSpy = getSpyFor(c2);
			if (remoteSpy != null && remoteSpy.networkLevel >= EmpireStat.SPY_CAN_VIEW_ALL_CITIES.get(bonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canInspectFleets(Empire victim, WorldMap m) {
		if (EmpireStat.CAN_SEE_EVERYTHING.get(bonuses)) { return true; }
		for (int i = 0; i < victim.cities.size(); i++) {
			City c2 = victim.cities.get(i);
			if (c2.isTown) { continue; }
			Spy remoteSpy = getSpyFor(c2);
			if (remoteSpy != null && remoteSpy.networkLevel >= EmpireStat.SPY_CAN_INSPECT_FLEETS.get(bonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public String getRepInfo(CampaignWorld w) {
		StringBuilder sb = new StringBuilder();
		ReputationLevel lvl = getReputationLevel();
		int rep = getReputation();
		sb.append(_t("bonus_" + lvl.name() + "_desc"));
		int unrest = EmpireStat.CITY_REP_UNREST.get(bonuses);
		if (unrest != 0) {
			sb.append(_t("rep_info_unrest", (unrest > 0 ? "+": "") + unrest));
		}
		if (lvl == ReputationLevel.LOVED && EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(bonuses) != 0) {
			sb.append(_t("rep_info_conquest_rep_loss", EmpireStat.CONQUER_LOVED_CITY_REP_LOSS.get(bonuses)));
		}
		if (lvl == ReputationLevel.HATED && EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(bonuses) != 0) {
			sb.append(_t("rep_info_conquest_rep_gain", EmpireStat.CONQUER_HATED_CITY_REP_GAIN.get(bonuses)));
		}
		BonusSet justRep = new BonusSet();
		justRep.set(Bonus.ofName(lvl.name()).ordinal(), true);
		int spyOffense = EmpireStat.ESPIONAGE_SUCCESS_PERCENTAGE_BONUS.get(justRep);
		if (spyOffense != 0) {
			sb.append(_t("rep_info_spy_offense", (spyOffense > 0 ? "+": "") + spyOffense));
		}
		int spyDefense = EmpireStat.ESPIONAGE_DEFENCE_PERCENTAGE_BONUS.get(justRep);
		if (spyDefense != 0) {
			sb.append(_t("rep_info_spy_defence", (spyDefense > 0 ? "+": "") + spyDefense));
		}
		TakeoverMethod gentle = TakeoverMethod.ofName("GENTLE");
		if (gentle.timeTaken.get(justRep) > gentle.timeTaken.get(BonusSet.empty())) {
			sb.append(_t("rep_info_slower_gentle_takeovers", gentle.timeTaken.get(justRep) * 1.0 / gentle.timeTaken.get(BonusSet.empty())));
		} else if (gentle.timeTaken.get(justRep) < gentle.timeTaken.get(BonusSet.empty())) {
			sb.append(_t("rep_info_faster_gentle_takeovers", gentle.timeTaken.get(BonusSet.empty()) * 1.0 / gentle.timeTaken.get(justRep)));
		}
		if (lvl == ReputationLevel.LIKED || lvl == ReputationLevel.LOVED) {
			sb.append(_t("rep_info_trust"));
		}
		if (lvl == ReputationLevel.DISLIKED || lvl == ReputationLevel.HATED) {
			sb.append(_t("rep_info_distrust"));
		}
		if (w.has(ConquestToggle.DIPLOMACY) && rep < EmpireStat.MIN_ULTIMATUM_REPUTATION.get(bonuses)) {
			sb.append(_t("rep_info_no_ultimatums"));
		}
		if (w.has(ConquestToggle.CORONATION) && rep >= EmpireStat.CORONATION_REPUTATION.get(bonuses) && EmpireStat.CAN_CORONATE.get(bonuses)) {
			sb.append(_t("rep_info_can_coronate", w.map.requiredCitiesForCoronation()));
		}
		return sb.toString();
	}
	
	public ArrayList<CityUpgradeType> desiredUpgrades(HasRelationships m) {
		ArrayList<CityUpgradeType> l = new ArrayList<CityUpgradeType>();
		if (m instanceof WorldMap) {
			WorldMap wm = (WorldMap) m;
			if (wm.eraModifier != null && wm.eraModifier.eraStartSpawnUpgrade != null) {
				l.add(wm.eraModifier.eraStartSpawnUpgrade);
			}
			if (wm.eraModifier != null && wm.eraModifier.eraEndsWhenUpgradesBuilt != null) {
				l.add(wm.eraModifier.eraEndsWhenUpgradesBuilt);
			}
		}
		if (diplomacyAI != null) {
			l.addAll(diplomacyAI.personality.desiredSpecials);
		}
		if (constructionStrategy != null) {
			l.addAll(constructionStrategy.desiredSpecials);
		}
		return l;
	}
	
	public Fleet canHelpAllyBesiegeFleet(City target, CampaignWorld w) {
		if (w.map.getRelationship(this, w.map.owner(target)).level != Relationship.Level.WAR) { return null; }
		Fleet bestFleet = null;
		int bestFleetStrength = 0;
		for (int fi = 0; fi < fleets.size(); fi++) {
			Fleet f = fleets.get(fi);
			if (f.location == null || f.besiege) { continue; }
			if (!f.hasCombatantShips()) { continue; }
			if (!f.canTravelTo(target, w.map, this)) { continue; }
			if (w.has(ConquestToggle.SUPPLY) && moveSupplyCost(f, f.getAllShips(), target, null, w.map) > f.supply()) { continue; }
			if (!playerControlled && FleetAI.fleetLocationBeingInvaded(w.map, this, f)) { continue; }
			int strength = f.totalCost();
			if (bestFleet == null || strength > bestFleetStrength) {
				bestFleet = f;
				bestFleetStrength = strength;
			}
		}
		return bestFleet;
	}
	
	public void helpAllyBesiege(Empire ally, Fleet f, City target, CampaignWorld w) {
		if (playerControlled) {
			besiegeRequests.add(new BesiegeRequest(ally, target));
		} else {
			if (f.travelTo(target, w.map, true)) {
				f.besiegeForAllyTimeout = WorldMap.MS_PER_DAY * WorldMap.DAYS_PER_WEEK * 3;
				if (ally.playerControlled) {
					ally.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.BESIEGE_REQUEST_GRANTED, this, target));
				}
			}
		}
	}
	
	public ArrayList<Empire> canHelpBesiegeAllies(City target, CampaignWorld w) {
		ArrayList<Empire> l = new ArrayList<Empire>();
		for (Relationship rel : w.map.getRelationships(this)) {
			if (rel.level == Relationship.Level.ALLIANCE && rel.other(this).canHelpAllyBesiegeFleet(target, w) != null) {
				l.add(rel.other(this));
			}
		}
		return l;
	}
	
	public void askAlliesToHelpBesiege(City target, CampaignWorld w) {
		for (Relationship rel : w.map.getRelationships(this)) {
			if (rel.level != Relationship.Level.ALLIANCE) { continue; }
			Fleet f = rel.other(this).canHelpAllyBesiegeFleet(target, w);
			if (f != null) {
				rel.other(this).helpAllyBesiege(this, f, target, w);
			}
		}
	}
	
	public String getTooltip(Empire viewpoint, WorldMap m) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("\n");
		if (playerControlled) {
			sb.append(_t("player_controlled_empire"));
		} else {
			sb.append(_t("personality_" + diplomacyAI.personality.name));
		}
		if (m.toggles.contains(ConquestToggle.REPUTATION)) {
			sb.append("\n").append(_t("Rep_")).append(reputation).append(" ").append(_t("_" + getReputationLevel().name() + "_rep_"));
		}
		if (viewpoint != null && m.toggles.contains(ConquestToggle.DIPLOMACY)) {
			Relationship rel = m.getRelationship(this, viewpoint);
			sb.append("\n").append(_t("relationship_" + rel.level.name()));
			if (rel.tradeTreaty) {
				sb.append("\n").append(_t("Trade_Treaty"));
			}
			if (rel.researchTreaty) {
				sb.append("\n").append(_t("Research_Treaty"));
			}
			if (rel.getReceivingTribute(this)) {
				sb.append("\n").append(_t("Receiving_Tribute"));
			}
			if (rel.getSendingTribute(this)) {
				sb.append("\n").append(_t("Paying_Tribute"));
			}
			if (rel.getGrievances(viewpoint) > 0) {
				sb.append("\n").append(_t("x_grievances_towards_them", rel.getGrievances(viewpoint)));
			}
			if (rel.getGrievances(this) > 0) {
				sb.append("\n").append(_t("x_grievances_from_them", rel.getGrievances(this)));
			}
		}
		return sb.toString();
	}
	
	public void makeComment(Hero h, String c, StatChange statChange) {
		if (c != null && !usedComments.contains(c)) {
			usedComments.add(c);
			heroComments.add(new HeroComment(h, c, statChange));
		} else if (statChange != null) {
			heroStatChanges.add(statChange);
		}
	}
	
	public Hero riseToPowerHero(WorldMap m) {
		if (!m.toggles.contains(ConquestToggle.HERO_VICTORY)) { return null; }
		int mostPower = 0;
		Hero most = null;
		for (Hero h : Hero.getHeroes(this, m)) {
			if (!h.hired) { continue; }
			for (Stat s : h.type.stats) {
				if ((s.name.equals("power") && s.winOn100) && (most == null || h.stats.get("power") > mostPower)) {
					most = h;
					mostPower = h.stats.get("power");
				}
			}
		}
		return most;
	}
	
	public void autoAssignHeroes(WorldMap m) {
		ArrayList<Airship> suitableShips = new ArrayList<Airship>();
		for (Fleet f : fleets) {
			for (Airship s : f.getAllCombatantShips()) {
				if (!s.isBonusConstruction) {
					suitableShips.add(s);
				}
			}
		}
		Collections.sort(suitableShips, new Comparator<Airship>() {
			@Override
			public int compare(Airship o1, Airship o2) {
				return o2.getCost() - o1.getCost();
			}
		});
		for (Hero h : Hero.getHeroes(HeroType.Role.CAPTAIN, true, this, m)) {
			if (h.getInShip() == null) {
				for (Airship s : suitableShips) {
					if (Hero.get(s, m) == null) {
						h.setInShip(s);
						break;
					}
				}
			}
		}
		lp: for (Hero h : Hero.getHeroes(HeroType.Role.GOVERNOR, true, this, m)) {
			if (h.getInCity() == null) {
				for (City c : cities) {
					if (!c.isTown && Hero.get(c, m) == null) {
						h.setInCity(c);
						continue lp;
					}
				}
				for (City c : cities) {
					if (Hero.get(c, m) == null) {
						h.setInCity(c);
						continue lp;
					}
				}
			}
		}
	}
	
	public boolean toBeAssignedToConstruction(Hero h) {
		for (City c : cities) {
			for (MapLocation.ConstructionEntry ce : c.constructing) {
				if (h.id == ce.heroIDToAttach) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public int compareTo(Object o) {
		if (o instanceof Empire) {
			return id - ((Empire) o).id;
		}
		if (o instanceof MonsterNest) {
			return id - ((MonsterNest) o).id;
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public FleetOwnerRef getRef() {
		return new FleetOwnerRef(FleetOwnerRef.Type.EMPIRE, id, playerControlled);
	}
}
