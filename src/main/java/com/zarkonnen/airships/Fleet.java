package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Pt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
 import static com.zarkonnen.airships.Lang._t; 
import java.util.HashMap;

public class Fleet implements ShipList, OutPipe.Writer {
	public static final double SPEED_TO_MAP_PX_PER_MS = 0.1 / WorldMap.SCALE_FACTOR;
	public static final double MIN_SPEED = 0.03;

	public MapLocation location;
	public double sx, sy;
	public MapLocation destination;
	public double progress;
	public boolean fleeDestinationNeeded;
	public ArrayList<Airship> actives = new ArrayList<Airship>();
	public ArrayList<Airship> reserve = new ArrayList<Airship>();
	public ArrayList<String> anchoredIDs = new ArrayList<String>();
	
	public Pt interceptPoint;
	public Road road;
	public Fleet interceptTarget;
	public final int id;
	public int supplyUsedForMove;
	public boolean isFriendlyMove;
	public boolean isLimpHome;
	public boolean didIntercept;
	public PlagueLevel spreadingPlague;
	
	public boolean besiege;
	public boolean breakOut;
	public int besiegeForAllyTimeout;
	
	private double supply;
	
	public int supply() {
		return (int) StrictMath.floor(supply);
	}
	
	public void changeSupply(int amount) {
		supply = StrictMath.min(maxSupply(), StrictMath.max(0, supply + amount));
	}
	
	public int maxSupply() {
		int amt = 0;
		for (int i = 0; i < actives.size() + reserve.size(); i++) {
			Airship ship = i < actives.size() ? actives.get(i) : reserve.get(i - actives.size());
			amt += ship.getCachedSupplyCapacity();
		}
		return StrictMath.max(1, amt);
	}
	
	public static int maxSupply(ArrayList<Airship> ships) {
		int amt = 0;
		for (int i = 0; i < ships.size(); i++) {
			Airship ship = ships.get(i);
			amt += ship.getCachedSupplyCapacity();
		}
		return StrictMath.max(1, amt);
	}
	
	public static boolean canFly(ArrayList<Airship> ships) {
		for (Airship s : ships) {
			if (s.type.onGround) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canFly() {
		return canFly(actives) && canFly(reserve);
	}
	
	public boolean groundOnly() {
		for (Airship s : actives) {
			if (!s.type.onGround) { return false; }
		}
		for (Airship s : reserve) {
			if (!s.type.onGround) { return false; }
		}
		return true;
	}

	public boolean inTransit() {
		return destination != null || interceptPoint != null || road != null;
	}
	
	public int intX() {
		if (inTransit()) {
			if (road != null) {
				return road.intPath.get(StrictMath.min(road.intPath.size() - 1 , (int) StrictMath.floor(progress)))[0];
			} else {
				double result;
				if (interceptPoint != null) {
					result = sx + (interceptPoint.x - sx) * StrictMath.min(1.0, progress / transitDistance());
				} else {
					result = sx + (destination.x - sx) * StrictMath.min(1.0, progress / transitDistance());
				}
				if (Double.isNaN(result) || Double.isNaN(result)) {
					return (int) sx;
				}
				return (int) result;
			}
		} else if (location == null) {
			return (int) sx;
		} else {
			return location.x;
		}
	}
	
	public int intY() {
		if (inTransit()) {
			if (road != null) {
				return road.intPath.get(StrictMath.min(road.intPath.size() - 1 , (int) StrictMath.floor(progress)))[1];
			} else {
				double result;
				if (interceptPoint != null) {
					result = sy + (interceptPoint.y - sy) * StrictMath.min(1.0, progress / transitDistance());
				} else {
					result = sy + (destination.x - sy) * StrictMath.min(1.0, progress / transitDistance());
				}
				if (Double.isNaN(result) || Double.isNaN(result)) {
					return (int) sy;
				}
				return (int) result;
			}
		} else if (location == null) {
			return (int) sy;
		} else {
			return location.y;
		}
	}
	
	public double xAtProgress(double p, WorldMap m) {
		if (inTransit()) {
			if (road != null) {
				double ri = p;
				double mixture = ri % 1;
				int[] pa = road.intPath.get(StrictMath.min(road.intPath.size() - 1,(int) StrictMath.floor(ri)));
				int[] pb = road.intPath.get(StrictMath.min(road.intPath.size() - 1, (int) StrictMath.ceil(ri)));
				double xa = m.roadXs[pa[1]][pa[0]];
				double xb = m.roadXs[pb[1]][pb[0]];
				return (1 - mixture) * xa + mixture * xb;
			} else {
				double result;
				if (interceptPoint != null) {
					result = sx + (interceptPoint.x - sx) * StrictMath.min(1.0, p / transitDistance());
				} else {
					result = sx + (destination.x - sx) * StrictMath.min(1.0, p / transitDistance());
				}
				if (Double.isNaN(result) || Double.isNaN(result)) {
					return sx;
				}
				return result;
			}
		} else if (location == null) {
			return sx;
		} else {
			return location.x;
		}
	}
	
	public double yAtProgress(double p, WorldMap m) {
		if (inTransit()) {
			if (road != null) {
				double ri = p;
				double mixture = ri % 1;
				int[] pa = road.intPath.get(StrictMath.min(road.intPath.size() - 1,(int) StrictMath.floor(ri)));
				int[] pb = road.intPath.get(StrictMath.min(road.intPath.size() - 1, (int) StrictMath.ceil(ri)));
				double ya = m.roadYs[pa[1]][pa[0]];
				double yb = m.roadYs[pb[1]][pb[0]];
				return (1 - mixture) * ya + mixture * yb;
			} else {
				double result;
				if (interceptPoint != null) {
					result = sy + (interceptPoint.y - sy) * StrictMath.min(1.0, p / transitDistance());
				} else {
					result = sy + (destination.y - sy) * StrictMath.min(1.0, p / transitDistance());
				}
				if (Double.isNaN(result) || Double.isNaN(result)) {
					return sy;
				}
				return result;
			}
		} else if (location == null) {
			return sy;
		} else {
			return location.y;
		}
	}

	public double transitDistance() {
		if (destination == null && interceptTarget == null) { return 0; }
		if (interceptPoint != null) {
			return StrictMath.sqrt((sx - interceptPoint.x) * (sx - interceptPoint.x) + (sy - interceptPoint.y) * (sy - interceptPoint.y));
		}
		if (road != null) { return road.intPath.size() - 1; }
		return StrictMath.sqrt((sx - destination.x) * (sx - destination.x) + (sy - destination.y) * (sy - destination.y));
	}

	public double realX(WorldMap m) {
		return xAtProgress(progress, m);
	}

	public double realY(WorldMap m) {
		return yAtProgress(progress, m);
	}
	
	public int maintenanceCost() {
		int c = 0;
		for (Airship ship : actives) {
			c += ship.maintenanceCost();
		}
		for (Airship ship : reserve) {
			c += ship.maintenanceCost();
		}
		return c;
	}
	
	public static double speed(ArrayList<Airship> ships, BonusSet ownerBonuses, WorldMap m) {
		int percentFromCaptains = 0;
		int percentFromMedals = 0;
		double speed = 1.5;
		
		for (Airship s : ships) {
			speed = StrictMath.min(speed, s.getMainMapSpeed(ownerBonuses));
			Hero capt = Hero.get(s, m);
			if (capt != null) {
				percentFromCaptains = StrictMath.max(percentFromCaptains, capt.type.fleetSpeedPercent);
			}
			percentFromMedals = StrictMath.max(percentFromMedals, s.fleetSpeedPercentFromMedals);
		}
		return StrictMath.max(MIN_SPEED, speed * (100 + percentFromMedals + percentFromCaptains) / 100);
	}

	public double speed(BonusSet ownerBonuses, WorldMap m) {
		if (isLimpHome) { return MIN_SPEED; }
		return StrictMath.min(speed(actives, ownerBonuses, m), speed(reserve, ownerBonuses, m));
	}
	
	public Airship getShip(String networkID) {
		for (Airship s : actives) {
			if (s.networkID.equals(networkID)) {
				return s;
			}
		}
		for (Airship s : reserve) {
			if (s.networkID.equals(networkID)) {
				return s;
			}
		}
		return null;
	}

	public Fleet(MapLocation location, WorldMap wm) {
		this.location = location;
		this.sx = location.x;
		this.sy = location.y;
		id = wm.fleetIDCounter++;
		if (location instanceof City) {
			City c = (City) location;
			if (c.plagueLevel != null && c.plagueLevel.spreadsAs != null) {
				spreadingPlague = PlagueLevel.ofName(c.plagueLevel.spreadsAs);
			}
		}
	}
	
	private Fleet(WorldMap wm) {
		id = wm.fleetIDCounter++;
	}
	
	public Fleet split(WorldMap wm, ArrayList<Airship> shipsToTransfer, int supplyToTransfer) {
		Fleet f2 = new Fleet(wm);
		f2.actives.addAll(this.actives);
		f2.destination = this.destination;
		f2.fleeDestinationNeeded = this.fleeDestinationNeeded;
		f2.interceptPoint = this.interceptPoint;
		f2.interceptTarget = this.interceptTarget;
		f2.location = this.location;
		f2.progress = this.progress;
		f2.reserve.addAll(this.reserve);
		f2.sx = this.sx;
		f2.sy = this.sy;
		f2.road = this.road;
		
		f2.actives.retainAll(shipsToTransfer);
		f2.reserve.retainAll(shipsToTransfer);
		
		supplyToTransfer = StrictMath.max(0, StrictMath.min(supply(), StrictMath.min(f2.maxSupply(), supplyToTransfer)));
		
		f2.supply = supplyToTransfer;
		actives.removeAll(shipsToTransfer);
		reserve.removeAll(shipsToTransfer);
		changeSupply(-supplyToTransfer);
		f2.spreadingPlague = spreadingPlague;
		
		return f2;
	}

	public Fleet(JSONObject o, WorldMap wm, InPipe ip, BonusSet ownerBonuses) throws IOException {
		sx = o.getDouble("sx");
		sy = o.getDouble("sy");
		progress = o.optDouble("progress", 0);
		fleeDestinationNeeded = o.getBoolean("fleeDestinationNeeded");
		besiege = o.optBoolean("besiege", false);
		breakOut = o.optBoolean("breakOut", false);
		id = o.getInt("id");
		besiegeForAllyTimeout = o.optInt("besiegeForAllyTimeout", 0);
		JSONArray a;
		a = o.getJSONArray("activesIDs");
		for (int i = 0; i < a.length(); i++) {
			actives.add(new Airship(ip.read(a.getString(i)), ownerBonuses));
		}
		a = o.getJSONArray("reserveIDs");
		for (int i = 0; i < a.length(); i++) {
			reserve.add(new Airship(ip.read(a.getString(i)), ownerBonuses));
		}
		if (o.has("interceptPointX")) {
			interceptPoint = new Pt(o.getDouble("interceptPointX"), o.getDouble("interceptPointY"));
		}
		isLimpHome = o.getBoolean("isLimpHome");
		didIntercept = o.optBoolean("didIntercept", false);
		isFriendlyMove = o.getBoolean("isFriendlyMove");
		supplyUsedForMove = o.getInt("supplyUsedForMove");
		supply = o.getDouble("supply");
		
		if (o.has("spreadingPlague")) {
			spreadingPlague = PlagueLevel.ofName(o.getString("spreadingPlague"));
		}
		a = o.getJSONArray("anchoredIDs");
		for (int i = 0; i < a.length(); i++) {
			anchoredIDs.add(a.getString(i));
		}
	}

	public void finish(JSONObject o, WorldMap m) {
		if (o.has("locationID")) {
			location = m.getMapLocation(o.getInt("locationID"));
		}
		if (o.has("destinationID")) {
			destination = m.getMapLocation(o.getInt("destinationID"));
		}
		if (o.has("interceptTarget")) {
			interceptTarget = m.getFleet(o.getInt("interceptTarget"));
		}
		if (o.has("road")) {
			road = new Road(o.getJSONObject("road"));
		}
		if (o.has("usingRoad")) {
			road = m.oldRoads.get(o.getInt("usingRoad"));
		}
	}
	
	private static double guardNaN(double v) {
		return Double.isNaN(v) || Double.isInfinite(v) ? 0 : v;
	}

	public JSONObject toJSON(WorldMap m, OutPipe op) {
		JSONObject o = new JSONObject()
				.put("id", id)
				.put("sx", guardNaN(sx))
				.put("sy", guardNaN(sy))
				.put("fleeDestinationNeeded", fleeDestinationNeeded)
				.put("progress", guardNaN(progress))
				.put("besiege", besiege)
				.put("breakOut", breakOut)
				.put("supplyUsedForMove", supplyUsedForMove)
				.put("isFriendlyMove", isFriendlyMove)
				.put("isLimpHome", isLimpHome)
				.put("supply", supply)
				.put("besiegeForAllyTimeout", besiegeForAllyTimeout)
				.put("didIntercept", didIntercept);
		if (spreadingPlague != null) {
			o.put("spreadingPlague", spreadingPlague.name);
		}
		if (location != null) {
			o.put("locationID", location.id);
		}
		if (destination != null) {
			o.put("destinationID", destination.id);
		}
		if (road != null) {
			o.put("road", road.toJSON(m));
		}
		if (interceptPoint != null) {
			o.put("interceptPointX", interceptPoint.x);
			o.put("interceptPointY", interceptPoint.y);
		}
		if (interceptTarget != null) {
			o.put("interceptTarget", interceptTarget.id);
		}
		JSONArray a = new JSONArray();
		o.put("activesIDs", a);
		for (Airship ship : actives) {
			op.register(this, ship.networkID, ship.version);
			a.put(ship.networkID);
		}
		a = new JSONArray();
		o.put("reserveIDs", a);
		for (Airship ship : reserve) {
			op.register(this, ship.networkID, ship.version);
			a.put(ship.networkID);
		}
		a = new JSONArray();
		o.put("anchoredIDs", a);
		for (String id : anchoredIDs) {
			a.put(id);
		}
		return o;
	}
	
	@Override
	public JSONObject write(String writeID) {
		for (Airship ship : actives) {
			if (writeID.equals(ship.networkID)) {
				return ship.toJSON(null, /* storeBonuses */ false);
			}
		}
		for (Airship ship : reserve) {
			if (writeID.equals(ship.networkID)) {
				return ship.toJSON(null, /* storeBonuses */ false);
			}
		}
		throw new RuntimeException(writeID);
	}
	
	public boolean canTravelTo(MapLocation destination, WorldMap wm, FleetOwner owner) {
		return this.destination == destination || canFly() || hasRoadPath(destination, wm, owner);
	}
	
	public int timeTo(MapLocation newDestination, WorldMap wm, boolean limp) {
		double d = distanceTo(newDestination, wm);
		if (d < 0) { return -1; }
		return (int) (d / ((limp ? MIN_SPEED : speed(wm.owner(this).bonuses(), wm)) * SPEED_TO_MAP_PX_PER_MS));
	}
	
	public int timeTo(MapLocation newDestination, WorldMap wm, ArrayList<Airship> ships, boolean limp) {
		if (ships == null || ships.isEmpty()) { return timeTo(newDestination, wm, limp); }
		double d = distanceTo(newDestination, wm);
		if (d < 0) { return -1; }
		return (int) (d / ((limp ? MIN_SPEED : speed(ships, wm.owner(this).bonuses(), wm)) * SPEED_TO_MAP_PX_PER_MS));
	}
	
	public boolean canTravelTo(MapLocation destination, WorldMap wm, FleetOwner owner, ArrayList<Airship> ships) {
		return this.destination == destination || canFly(ships) || getRoadPath(destination, wm) != null;
	}
	
	public boolean canTravelAnywhereElse(WorldMap wm) {
		if (canFly()) { return true; }
		FleetOwner owner = wm.owner(this);
		for (Empire e : wm.empires) {
			for (City c : e.cities) {
				if (c == location) { continue; }
				if (canTravelTo(c, wm, owner)) {
					return true;
				}
			}
		}
		for (MonsterNest n : wm.nests) {
			if (n == location) { continue; }
			if (canTravelTo(n, wm, owner)) {
				return true;
			}
		}
		return false;
	}
	
	public int travelTimeLeft(BonusSet bs, WorldMap wm) {
		return (int) ((transitDistance() - progress) / (speed(bs, wm) * SPEED_TO_MAP_PX_PER_MS));
	}
	
	public double distanceTo(MapLocation newDestination, WorldMap wm) {
		if (!canFly()) {
			Road it = getRoadPath(newDestination, wm);
			return it == null ? -1 : it.intPath.size();
		}
		return StrictMath.sqrt((realX(wm) - newDestination.x) * (realX(wm) - newDestination.x) + (realY(wm) - newDestination.y) * (realY(wm) - newDestination.y));
	}

	public boolean travelTo(MapLocation destination, WorldMap wm, boolean besiege) {
		FleetOwner fleetOwner = wm.owner(this);
		int supplyCost = 0;
		boolean isLimp = false;
		boolean friendly = false;
		if (fleetOwner instanceof Empire && wm.toggles.contains(ConquestToggle.SUPPLY)) {
			Empire e = (Empire) fleetOwner;
			supplyCost = e.moveSupplyCost(this, getAllShips(), destination, null, wm);
			isLimp = e.isLimpHome(this, getAllShips(), destination, wm);
			friendly = e.cities.contains(location) && e.cities.contains(destination);
			changeSupply(-supplyCost);
		}
		if (doTravelTo(destination, wm, besiege)) {
			supplyUsedForMove = supplyCost;
			isFriendlyMove = friendly;
			isLimpHome = isLimp && (fleetOwner instanceof Empire && ((Empire) fleetOwner).playerControlled);
			FleetOwner destOwner = wm.owner(destination);
			if (fleetOwner != destOwner && !(destOwner instanceof MonsterNest) && !(fleetOwner instanceof MonsterNest)) {
				Relationship rel = wm.getRelationship((Empire) destOwner, (Empire) fleetOwner);
				if (rel.level == Relationship.Level.TRUCE || rel.level == Relationship.Level.PEACE) {
					Relationship.Offer warOffer = new Relationship.Offer(rel);
					warOffer.setToWar();
					if (!rel.force(warOffer, (Empire) fleetOwner, wm, true)) {
						stopAndAskForHelp(wm);
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean doTravelTo(MapLocation destination, WorldMap wm, boolean besiege) {
		if (canFly()) {
			sx = realX(wm);
			sy = realY(wm);
			road = null;
			this.destination = destination;
			location = null;
			progress = 0;
			interceptPoint = null;
			interceptTarget = null;
			fleeDestinationNeeded = false;
			anchoredIDs.clear();
			broadcastChangedDirection(wm);
			this.besiege = besiege;
			clearPathCache();
			return true;
		} else {
			Road it = getRoadPath(destination, wm);
			if (it != null) {
				road = it;
				this.destination = destination;
				location = null;
				progress = 0;
				interceptPoint = null;
				interceptTarget = null;
				fleeDestinationNeeded = false;
				anchoredIDs.clear();
				broadcastChangedDirection(wm);
				this.besiege = besiege;
				clearPathCache();
				return true;
			} else {
				return false;
			}			
		}
	}
	
	public void stopAndAskForHelp(WorldMap wm) {
		if (realX(wm) == 0 && realY(wm) == 0) {
			AirshipGame.instance.reportError("StopAndAskForHelp with realx/y of 0", null, null, false, true);
		}
		if (location == null && road == null) {
			sx = realX(wm);
			sy = realY(wm);
			location = null;
			progress = 0;
		}
		destination = null;
		interceptPoint = null;
		interceptTarget = null;
		fleeDestinationNeeded = true;
	}
	
	public boolean checkInterceptTargetValid(WorldMap map) {
		if (interceptTarget != null && map.owner(interceptTarget) == null) {
			stopAndAskForHelp(map);
			return true;
		}
		return false;
	}

	public void repair(FleetOwner owner) {
		int repaired = 0;
		Airship singleRepaired = null;
		for (Airship s : getAllShips()) {
			if (s.repair(/* resetXP */ false, /* forceResetCrew */ false)) {
				repaired++;
				singleRepaired = s;
			}
		}
		if (repaired > 0 && location != null) {
			if (repaired > 1) {
				location.addMessage(owner, MapLocation.MessageType.REARM, _t("x_ships_repaired_and_rearmed", repaired));
			} else {
				location.addMessage(owner, MapLocation.MessageType.REARM, _t("ship_x_repaired_and_rearmed", singleRepaired.getName()));
			}
		}
	}
	
	public int resupplyAmount(BonusSet bonuses) {
		if (fleeDestinationNeeded) { return 0; }
		if (location instanceof City) {
			City c = ((City) location);
			int amt = c.isTown ? EmpireStat.RESUPPLY_TOWN.get(bonuses) : EmpireStat.RESUPPLY_CITY.get(bonuses);
			if (c.takeoverMethod != null) {
				amt *= c.takeoverMethod.supplyMultiplier.get(bonuses);
			}
			return amt;
		}
		if (location instanceof MonsterNest) {
			return EmpireStat.RESUPPLY_NEST.get(bonuses);
		}
		return 0;
	}
	
	public void removeDuplicatesAndFixDuplicateIDs(FleetOwner owner) {
		for (int i = 0; i < actives.size(); i++) {
			Airship ship = actives.get(i);
			for (int j = 0; j < actives.size(); j++) {
				if (i == j) { continue; }
				if (ship == actives.get(j)) {
					actives.remove(j);
					j--;
				}
			}
			for (int j = 0; j < reserve.size(); j++) {
				if (ship == reserve.get(j)) {
					reserve.remove(j);
					j--;
				}
			}
		}
		for (int i = 0; i < reserve.size(); i++) {
			Airship ship = reserve.get(i);
			for (int j = 0; j < actives.size(); j++) {
				if (ship == actives.get(j)) {
					System.out.println("deduping ship");
					actives.remove(j);
					j--;
				}
			}
			for (int j = 0; j < reserve.size(); j++) {
				if (i == j) { continue; }
				if (ship == reserve.get(j)) {
					System.out.println("deduping ship");
					reserve.remove(j);
					j--;
				}
			}
		}
		
		
		for (int i = 0; i < actives.size(); i++) {
			Airship ship = actives.get(i);
			for (int j = 0; j < actives.size(); j++) {
				if (i == j) { continue; }
				if (ship.networkID.equals(actives.get(j).networkID)) {
					System.out.println("deduping network ID");
					actives.get(j).networkID = owner.getNextShipID();
				}
			}
			for (int j = 0; j < reserve.size(); j++) {
				if (ship.networkID.equals(reserve.get(j).networkID)) {
					System.out.println("deduping network ID");
					reserve.get(j).networkID = owner.getNextShipID();
				}
			}
		}
		
		for (int i = 0; i < reserve.size(); i++) {
			Airship ship = reserve.get(i);
			for (int j = 0; j < actives.size(); j++) {
				if (ship.networkID.equals(actives.get(j).networkID)) {
					System.out.println("deduping network ID");
					actives.get(j).networkID = owner.getNextShipID();
				}
			}
			for (int j = 0; j < reserve.size(); j++) {
				if (i == j) { continue; }
				if (ship.networkID.equals(reserve.get(j).networkID)) {
					System.out.println("deduping network ID");
					reserve.get(j).networkID = owner.getNextShipID();
				}
			}
		}
	}
	
	public void fixOffroad(WorldMap map) {
		// If we're slightly off-road, clean it up.
		if (fleeDestinationNeeded && !map.roads[intY()][intX()] && !canTravelAnywhereElse(map) && !getAllShipsOfType(ShipType.LANDSHIP).isEmpty()) {
			lp: for (int y = -1; y < 2; y++) {
				for (int x = -1; x < 2; x++) {
					int x2 = intX() + x;
					int y2 = intY() + y;
					if (map.roads[y2][x2]) {
						int ownership = map.cityOwnership[y2][x2];
						if (ownership != -1) {
							City c = map.getCity(ownership);
							if (c != null && c.x == x2 && c.y == y2) {
								continue; // Don't fix up by going into a city, that causes trouble.
							}
						}
						sx = x2;
						sy = y2;
						clearPathCache();
						break lp;
					}
				}
			}
		}
	}
	
	public boolean tick(int ms, FleetOwner owner, WorldMap map) {
		if (checkInterceptTargetValid(map)) {
			return false;
		}
		
		fixOffroad(map);
		
		if (!fleeDestinationNeeded && location != null) {
			supply = StrictMath.min(supply + location.getResupplySpeed(owner, map, null) * ms * 1.0 / Empire.MS_PER_INCOME, maxSupply());
		}
				
		// Get rid of old anchor values.
		lp: for (int i = 0; i < anchoredIDs.size(); i++) {
			String aid = anchoredIDs.get(i);
			for (int j = 0; j < actives.size(); j++) {
				if (actives.get(j).networkID.equals(aid)) {
					continue lp;
				}
			}
			for (int j = 0; j < reserve.size(); j++) {
				if (reserve.get(j).networkID.equals(aid)) {
					continue lp;
				}
			}
			anchoredIDs.remove(i);
			i--;
		}
		
		removeDuplicatesAndFixDuplicateIDs(owner);
		
		if (owner instanceof Empire && map.toggles.contains(ConquestToggle.DIPLOMACY)) {
			if (location != null) {
				FleetOwner locOwner = map.owner(location);
				if (locOwner instanceof Empire && locOwner != owner) {
					Relationship rel = map.getRelationship((Empire) owner, (Empire) locOwner);
					if (rel.level == Relationship.Level.PEACE || rel.level == Relationship.Level.TRUCE || (rel.level == Relationship.Level.WAR && !canFight())) {
						stopAndAskForHelp(map);
					}
				}
			} else if (destination instanceof MonsterNest && ((MonsterNest) destination).type == null) {
				stopAndAskForHelp(map);
			} else if (destination != null) {
				FleetOwner destOwner = map.owner(destination);
				if (destOwner instanceof Empire && destOwner != owner) {
					Relationship rel = map.getRelationship((Empire) owner, (Empire) destOwner);
					if (rel.level == Relationship.Level.PEACE || rel.level == Relationship.Level.TRUCE || (rel.level == Relationship.Level.WAR && isLimpHome)) {
						stopAndAskForHelp(map);
					}
				}
			}
		}
		
		if (fleeDestinationNeeded && location != null) {
			boolean hasLandships = false;
			boolean hasAirships = false;
			for (Airship s : actives) {
				if (s.type == ShipType.LANDSHIP) {
					hasLandships = true;
				} else {
					hasAirships = true;
				}
			}
			for (Airship s : reserve) {
				if (s.type == ShipType.LANDSHIP) {
					hasLandships = true;
				} else {
					hasAirships = true;
				}
			}
			if (hasLandships && !map.roads[intY()][intX()]) {
				if (hasAirships) {
					for (Iterator<Airship> it = actives.iterator(); it.hasNext();) {
						if (it.next().type == ShipType.LANDSHIP) {
							it.remove();
						}
					}
					for (Iterator<Airship> it = reserve.iterator(); it.hasNext();) {
						if (it.next().type == ShipType.LANDSHIP) {
							it.remove();
						}
					}
				} else {
					broadcastDestroyed(map);
					return true;
				}
			}
		}
		
		if (destination != null) {
			progress += ms * speed(owner.bonuses(), map) * SPEED_TO_MAP_PX_PER_MS;
			clearPathCache();
			if (progress >= transitDistance()) {
				location = destination;
				if (location instanceof City) {
					City c = (City) location;
					if (spreadingPlague != null && (c.plagueLevel == null || c.plagueLevel.sort < spreadingPlague.sort)) {
						BonusSet bs = map.owner(c).bonuses;
						if ((c.plagueLevel == null || !c.plagueLevel.visible.get(bs)) && spreadingPlague.visible.get(bs)) {
							c.addMessage(null, MapLocation.MessageType.PLAGUE, _t("cityInfected", c.getDisplayName()));
						}
						c.plagueLevel = spreadingPlague;
						//System.out.println("Fleet infects " + c.getDisplayName());
					}
					if (c.plagueLevel != null && c.plagueLevel.spreadsAs != null && (spreadingPlague == null || PlagueLevel.ofName(c.plagueLevel.spreadsAs).sort > spreadingPlague.sort)) {
						spreadingPlague = PlagueLevel.ofName(c.plagueLevel.spreadsAs);
						//System.out.println("Fleet infected at " + c.getDisplayName());
					}
				}
				destination = null;
				road = null;
				isLimpHome = false;
				didIntercept = false;
				if (map.isFriendly(owner, location, /* forCapitalConnection */ false) && location instanceof City && ((City) location).takeoverMethod == null) {
					repair(owner);
				}
				for (Fleet f : owner.getFleets()) {
					if (f != this && f.location == location) {
						f.actives.addAll(actives);
						f.reserve.addAll(reserve);
						f.supply = StrictMath.min(f.supply + supply, f.maxSupply());
						f.anchoredIDs.addAll(anchoredIDs);
						location.layoutGarrison(map);
						broadcastDestroyed(map);
						return true;
					}
				}
				broadcastArrived(map);
				location.layoutGarrison(map);
			}
		}
		if (interceptPoint != null && !fleeDestinationNeeded) {
			progress += ms * speed(owner.bonuses(), map) * SPEED_TO_MAP_PX_PER_MS;
		}
		return false;
	}

	public boolean containsAny(Fleet oldF) {
		for (Airship s : actives) {
			if (oldF.actives.contains(s) || oldF.reserve.contains(s)) { return true; }
		}
		for (Airship s : reserve) {
			if (oldF.actives.contains(s) || oldF.reserve.contains(s)) { return true; }
		}
		return false;
	}
	
	public boolean doIntercept(Fleet target, WorldMap wm) {
		Pt icept = getFlightIntercept(target, null, wm);
		if (icept == null) { return false; }
		FleetOwner owner = wm.owner(this);
		int supplyCost = 0;
		if (owner instanceof Empire && wm.toggles.contains(ConquestToggle.SUPPLY)) {
			Empire e = (Empire) owner;
			supplyCost = e.moveSupplyCost(this, getAllShips(), null, icept, wm);
			if (supplyCost > supply()) {
				return false;
			}
			changeSupply(-supplyCost);
		}
		isLimpHome = false;
		anchoredIDs.clear();
		sx = realX(wm);
		sy = realY(wm);
		progress = 0;
		location = null;
		road = null;
		destination = null;
		interceptPoint = icept;
		interceptTarget = target;
		fleeDestinationNeeded = false;
		supplyUsedForMove = supplyCost;
		isFriendlyMove = false;
		didIntercept = true;
		return true;
	}
	
	public void broadcastChangedDirection(WorldMap wm) {
		for (Empire e : wm.empires) { for (Fleet f : e.getFleets()) {
			f.fleetChangedDirection(this, wm);
		}}
	}
	
	public void fleetChangedDirection(Fleet fl, WorldMap wm) {
		if (fl == interceptTarget) {
			if (!doIntercept(interceptTarget, wm)) {
				stopAndAskForHelp(wm);
			}
			broadcastStopped(wm);
		}
	}
	
	public void broadcastArrived(WorldMap wm) {
		for (Empire e : wm.empires) { for (Fleet f : e.getFleets()) {
			f.fleetArrived(this, wm);
		}}
	}
	
	public void fleetArrived(Fleet fl, WorldMap wm) {
		if (fl == interceptTarget) {
			stopAndAskForHelp(wm);
			broadcastStopped(wm);
		}
	}
	
	public void broadcastDestroyed(WorldMap wm) {
		for (Empire e : wm.empires) { for (Fleet f : e.getFleets()) {
			f.fleetDestroyed(this, wm);
		}}
	}
	
	public void fleetDestroyed(Fleet fl, WorldMap wm) {
		if (fl == interceptTarget) {
			stopAndAskForHelp(wm);
			broadcastStopped(wm);
		}
	}
	
	public void broadcastStopped(WorldMap wm) {
		for (Empire e : wm.empires) { for (Fleet f : e.getFleets()) {
			f.fleetStopped(this, wm);
		}}
	}
	
	public void fleetStopped(Fleet fl, WorldMap wm) {
		if (fl == interceptTarget) {
			stopAndAskForHelp(wm);
		}
	}

	boolean needsRepairs() {
		for (int i = 0; i < actives.size(); i++) {
			Airship ship = actives.get(i);
			if (ship.getOriginalDesign() != null && ship.getOriginalDesign().modules.size() != ship.modules.size()) {
				return true;
			}
		}
		for (int i = 0; i < reserve.size(); i++) {
			Airship ship = reserve.get(i);
			if (ship.getOriginalDesign() != null && ship.getOriginalDesign().modules.size() != ship.modules.size()) {
				return true;
			}
		}
		return false;
	}

	public void removeLandships() {
		for (int si = 0; si < actives.size(); si++) {
			if (actives.get(si).type == ShipType.LANDSHIP) {
				actives.remove(si);
				si--;
			}
		}
		for (int si = 0; si < reserve.size(); si++) {
			if (reserve.get(si).type == ShipType.LANDSHIP) {
				reserve.remove(si);
				si--;
			}
		}
	}
	
	public void clearPathCache() {
		// Needs clearing when something changes relating to pathing: the fleet moves, or maploc ownership changes, or a relationship level changes.
		pathCache.clear();
		canTravelCache.clear();
	}
	
	private transient HashMap<MapLocation, Road> pathCache = new HashMap<MapLocation, Road>();
	private transient HashMap<MapLocation, Boolean> canTravelCache = new HashMap<MapLocation, Boolean>();
	
	public boolean hasRoadPath(MapLocation target, WorldMap wm, FleetOwner owner) {
		if (canTravelCache.containsKey(target)) {
			return canTravelCache.get(target);
		}
		if (pathCache.containsKey(target)) {
			return pathCache.get(target) != null;
		}
		boolean has = wm.hasConnection(intX(), intY(), target, owner, true, false, false);
		canTravelCache.put(target, has);
		return has;
	}
	
	public Road getRoadPath(MapLocation target, WorldMap wm) {
		if (pathCache.containsKey(target)) {
			return pathCache.get(target);
		}
		Road path = wm.getConnection(intX(), intY(), target, wm.owner(this), true, false, false);
		pathCache.put(target, path);
		return path;
	}
	
	public Pt getFlightIntercept(Fleet target, ArrayList<Airship> shipsToSend, final WorldMap wm) {
		if (shipsToSend == null) {
			shipsToSend = new ArrayList<Airship>();
			shipsToSend.addAll(actives);
			shipsToSend.addAll(reserve);
		}
		
		if (target == null) { return null; }
		if (target.location != null) { return null; }
		if (target.destination == null) { return null; }
		if (!canFly(shipsToSend)) { return null; }
		
		BonusSet myBonuses = wm.owner(this).bonuses();
		FleetOwner tOwner = wm.owner(target);
		if (tOwner == null) { return null; }
		BonusSet targetBonuses = tOwner.bonuses();
		
		if (target.road == null) {
			double aStartX = realX(wm);
			double aStartY = realY(wm);
			double bStartX = target.realX(wm);
			double bStartY = target.realY(wm);
			double aSpeed = speed(shipsToSend, myBonuses, wm) * SPEED_TO_MAP_PX_PER_MS;
			double bSpeed = target.speed(targetBonuses, wm) * SPEED_TO_MAP_PX_PER_MS;
			double bDist = target.transitDistance() - target.progress;
			double bVelocityX = target.destination.x - target.sx;
			double bVelocityY = target.destination.y - target.sy;
			double vLength = StrictMath.sqrt(bVelocityX*bVelocityX + bVelocityY*bVelocityY);
			bVelocityX = bVelocityX / vLength * bSpeed;
			bVelocityY = bVelocityY / vLength * bSpeed;

			double dx = bStartX - aStartX;
			double dy = bStartY - aStartY;

			double t = -1;

			if (speed(myBonuses, wm) == target.speed(targetBonuses, wm)) {
				double angle = StrictMath.atan2(bVelocityY, bVelocityX) - StrictMath.atan2(-dy, -dx);
				if (angle >= 0) {
					double hyp = (StrictMath.sqrt(dx*dx + dy*dy) / 2 / StrictMath.cos(angle));
					t = hyp / StrictMath.sqrt(bVelocityX*bVelocityX + bVelocityY*bVelocityY);
				}
			} else {
				double tPlus = (-2*(dx*bVelocityX + dy*bVelocityY) + StrictMath.sqrt(4*(dx*bVelocityX + dy*bVelocityY) * (dx*bVelocityX + dy*bVelocityY) - 4*(-aSpeed*aSpeed + bVelocityX*bVelocityX + bVelocityY*bVelocityY) * (dx*dx + dy*dy))) / (2*(-aSpeed*aSpeed + bVelocityX*bVelocityX + bVelocityY*bVelocityY));
				double tMinus = (-2*(dx*bVelocityX + dy*bVelocityY) - StrictMath.sqrt(4*(dx*bVelocityX + dy*bVelocityY) * (dx*bVelocityX + dy*bVelocityY) - 4*(-aSpeed*aSpeed + bVelocityX*bVelocityX + bVelocityY*bVelocityY) * (dx*dx + dy*dy))) / (2*(-aSpeed*aSpeed + bVelocityX*bVelocityX + bVelocityY*bVelocityY));
				t = Double.isNaN(tPlus) || Double.isInfinite(tPlus) || tPlus < 0 ? tMinus : StrictMath.min(tMinus, tPlus);
			}
			//System.out.println(t);

			//System.out.println((-2*(dx*bVelocityX + dy*bVelocityY) - StrictMath.sqrt(4*(dx*bVelocityX + dy*bVelocityY) * (dx*bVelocityX + dy*bVelocityY) - 4*(-aSpeed*aSpeed + bVelocityX*bVelocityX + bVelocityY*bVelocityY) * (dx*dx + dy*dy))));
			//System.out.println(2*(-aSpeed*aSpeed + bVelocityX*bVelocityX + bVelocityY*bVelocityY));

			//System.out.println(2*(-aSpeed*aSpeed));

			if (t >= 0 && t < (bDist / bSpeed)) {
				double interceptX = bStartX + t * bVelocityX;
				double interceptY = bStartY + t * bVelocityY;
				return new Pt(interceptX, interceptY);
			}
		} else {
			// Air-on-road interception.
			double tProgress = target.progress;
			double tDistance = target.transitDistance();
			double tSpeed = target.speed(myBonuses, wm) * SPEED_TO_MAP_PX_PER_MS;
			double mySpeed = speed(shipsToSend, targetBonuses, wm) * SPEED_TO_MAP_PX_PER_MS;
			double myX = realX(wm);
			double myY = realY(wm);
			double myRadius = 0;
			
			while(true) {
				tProgress += tSpeed;
				if (tProgress >= tDistance) { break; }
				myRadius += mySpeed;
				double tX = target.xAtProgress(tProgress, wm);
				double tY = target.yAtProgress(tProgress, wm);
				if ((tX - myX) * (tX - myX) + (tY - myY) * (tY - myY) <= myRadius * myRadius) {
					return new Pt(tX, tY);
				}
			}
		}
		return null;
	}
	
	public boolean canIntercept(Fleet target, ArrayList<Airship> shipsToSend, WorldMap wm) {
		return getFlightIntercept(target, shipsToSend, wm) != null;
	}
	
	public ArrayList<Airship> getAllShips() {
		ArrayList<Airship> l = new ArrayList<Airship>();
		l.addAll(actives);
		l.addAll(reserve);
		return l;
	}
	
	public ArrayList<Airship> getAllNonAnchoredShips() {
		ArrayList<Airship> l = new ArrayList<Airship>();
		for (Airship s : actives) {
			if (!anchoredIDs.contains(s.networkID)) {
				l.add(s);
			}
		}
		for (Airship s : reserve) {
			if (!anchoredIDs.contains(s.networkID)) {
				l.add(s);
			}
		}
		return l;
	}
	
	public ArrayList<Airship> getAllShipsOfType(ShipType t) {
		ArrayList<Airship> l = new ArrayList<Airship>();
		for (Airship s : actives) {
			if (s.type == t) {
				l.add(s);
			}
		}
		for (Airship s : reserve) {
			if (s.type == t) {
				l.add(s);
			}
		}
		return l;
	}
	
	public ArrayList<Airship> getAllCombatantShips() {
		ArrayList<Airship> l = new ArrayList<Airship>();
		for (Airship s : actives) {
			if (!s.nonCombat()) {
				l.add(s);
			}
		}
		for (Airship s : reserve) {
			if (!s.nonCombat()) {
				l.add(s);
			}
		}
		return l;
	}
	
	public boolean canFight() {
		for (Airship s : actives) {
			if (!s.nonCombat()) {
				return true;
			}
		}
		for (Airship s : reserve) {
			if (!s.nonCombat()) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Airship> getAllNonCombatantShips() {
		ArrayList<Airship> l = new ArrayList<Airship>();
		for (Airship s : actives) {
			if (s.nonCombat()) {
				l.add(s);
			}
		}
		for (Airship s : reserve) {
			if (s.nonCombat()) {
				l.add(s);
			}
		}
		return l;
	}
	
	public boolean hasCombatantShips() {
		int asz = actives.size();
		for (int i = 0; i < asz; i++) {
			if (!actives.get(i).nonCombat()) { return true; }
		}
		int rsz = reserve.size();
		for (int i = 0; i < rsz; i++) {
			if (!reserve.get(i).nonCombat()) { return true; }
		}
		return false;
	}

	public int totalCost() {
		int cost = 0;
		int asz = actives.size();
		for (int i = 0; i < asz; i++) {
			cost += actives.get(i).getCost();
		}
		int rsz = reserve.size();
		for (int i = 0; i < rsz; i++) {
			cost += reserve.get(i).getCost();
		}
		return cost;
	}
	
	public int totalCachedCost() {
		int cost = 0;
		int asz = actives.size();
		for (int i = 0; i < asz; i++) {
			cost += actives.get(i).getCachedCost();
		}
		int rsz = reserve.size();
		for (int i = 0; i < rsz; i++) {
			cost += reserve.get(i).getCachedCost();
		}
		return cost;
	}
	
	@Override
	public int activesSize() {
		return actives.size();
	}
	
	@Override
	public int totalSize() {
		return actives.size() + reserve.size();
	}

	@Override
	public Airship get(int index) {
		return index >= actives.size() ? reserve.get(index - actives.size()) : actives.get(index);
	}
}
