package com.zarkonnen.airships;

import static com.zarkonnen.airships.Combat.COMBAT_AREA_W_NEW;
import static com.zarkonnen.airships.Combat.COMBAT_AREA_W_OLD;
import com.zarkonnen.catengine.util.Utils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import java.io.IOException;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import java.util.HashMap;

public abstract class MapLocation implements OutPipe.Writer {
	public int landscapeVersion = 1;
	public int id;
	public int constructionEntryIDCounter = 1;
	public int constructionTargetID = 1;
	public final int x, y;
	public final ArrayList<ConstructionEntry> constructing = new ArrayList<ConstructionEntry>();
	public Airship constructionTarget;
	protected ArrayList<Airship> defences = new ArrayList<Airship>();
	public transient LinkedList<Message> messages = new LinkedList<Message>();
	public LandFormation ground;
	public ArrayList<LandFormation> floaters = new ArrayList<LandFormation>();
	public transient boolean hasSeaRoute = false;
	public transient WorldMap.NavNode navNode = null;
	public boolean coastal = false;
	public final Pt pt;
	
	public static final int REORDER_UP = -1;
	public static final int REORDER_DOWN = 1;
	public static final int REORDER_TOP = -100;
	public static final int REORDER_BOTTOM = 100;
	
	public transient int textX, textY, textW, textH;
	public transient boolean textDrawn;
	public transient int infoX, infoY, infoW, infoH;
	public transient boolean infoDrawn;
	
	protected static final int MAP_MSG_H = 120;
	protected static final int MAP_MSG_TICK_LIFE = 800;
	
	public double dist(MapLocation ml2) {
		return StrictMath.sqrt((x - ml2.x) * (x - ml2.x) + (y - ml2.y) * (y - ml2.y));
	}
	
	public int combatAreaW() {
		if (ground.getX() > -COMBAT_AREA_W_NEW / 2 + 100) {
			return COMBAT_AREA_W_OLD;
		} else {
			return COMBAT_AREA_W_NEW;
		}
	}
	
	public static class ConstructionEntry {
		public static enum Type {
			CONSTRUCTION,
			REFIT,
			REPAIR,
			CITY_UPGRADE
		}
		
		public final Airship ship;
		public final Airship original;
		public final CityUpgradeType upgrade;
		public final int cost;
		public final Type type;
		public final int id;
		public final int heroIDToAttach;
		public int progress;

		public ConstructionEntry(Airship ship, int cost, int id, int progress) {
			this.ship = ship;
			this.original = null;
			this.cost = cost;
			this.type = Type.CONSTRUCTION;
			this.id = id;
			this.upgrade = null;
			this.progress = progress;
			heroIDToAttach = -1;
		}
		
		public ConstructionEntry(Airship ship, Airship original, int cost, Type type, int id, int progress) {
			this.ship = ship;
			this.original = original;
			this.cost = cost;
			this.type = type;
			this.id = id;
			this.upgrade = null;
			this.progress = progress;
			heroIDToAttach = original.getCaptain() != null ? original.getCaptain().id : -1;
		}
		
		public ConstructionEntry(CityUpgradeType upgrade, int cost, int id, int progress) {
			ship = null;
			original = null;
			this.cost = cost;
			type = Type.CITY_UPGRADE;
			this.id = id;
			this.upgrade = upgrade;
			this.progress = progress;
			heroIDToAttach = -1;
		}
		
		public ConstructionEntry(JSONObject o, BonusSet ownerBonuses) {
			ship = o.has("ship") ? new Airship(o.getJSONObject("ship"), ownerBonuses) : null;
			original = o.has("original") ? new Airship(o.getJSONObject("original"), ownerBonuses) : null;
			cost = o.getInt("cost");
			type = Type.valueOf(o.getString("type"));
			id = o.optInt("id");
			upgrade = o.has("upgrade") ? CityUpgradeType.ofName(o.getString("upgrade")) : null;
			heroIDToAttach = o.optInt("heroIDToAttach", -1);
			// NB progress is mainly stored in the map location because it changes constantly, so this is just for backwards combatibility.
			progress = o.optInt("progress");
		}
		
		public JSONObject toJSON() {
			JSONObject o = new JSONObject().put("cost", cost).put("type", type.name()).put("id", id);
			if (ship != null) {
				o.put("ship", ship.toJSON(null, /* storeBonuses */ false));
			}
			if (original != null) {
				o.put("original", original.toJSON(null, /* storeBonuses */ false));
			}
			if (upgrade != null) {
				o.put("upgrade", upgrade.name);
			}
			if (heroIDToAttach != -1) {
				o.put("heroIDToAttach", heroIDToAttach);
			}
			// NB progress is stored in the map location because it changes constantly.
			return o;
		}
		
		public String desc() {
			switch (type) {
				case CITY_UPGRADE:
					return upgrade.getName();
				case REFIT:
					return ship.getName() + " " + _t("Refit");
				case REPAIR:
					return ship.getName() + " " + _t("Repair");
			}
			return ship.getName(); 
		}
		
		public String getName() {
			return ship == null ? upgrade.getName() : ship.getName();
		}
	}
	
	public MapLocation(int id, int x, int y) {
		this.id = id;
		this.x = x;
		this.y = y;
		pt = new Pt(x, y);
	}
	
	public MapLocation(JSONObject o, HashMap<Integer, LandBlockType>[] mappingRef, InPipe ip, BonusSet ownerBonuses) throws IOException {
		id = o.getInt("id");
		x = o.getInt("x");
		y = o.getInt("y");
		coastal = o.optBoolean("coastal", false);
		pt = new Pt(x, y);
		int firstItemConstructionProgress = o.optInt("constructionProgress", 0);
		constructionEntryIDCounter = o.optInt("constructionEntryIDCounter", 1);
		landscapeVersion = o.optInt("landscapeVersion", 1);
		constructionTargetID = o.optInt("constructionTargetID");
		
		if (o.has("_constructingList")) {
			JSONArray l = o.getJSONArray("_constructingList");
			for (int i = 0; i < l.length(); i++) {
				constructing.add(new ConstructionEntry(new JSONObject(Compression.decompressFromString(l.getString(i))), ownerBonuses));
			}
		} else if (o.has("constructingList")) {
			JSONArray l = o.getJSONArray("constructingList");
			for (int i = 0; i < l.length(); i++) {
				constructing.add(new ConstructionEntry(l.getJSONObject(i), ownerBonuses));
			}
		} else if (o.has("constructing")) {
			constructing.add(new ConstructionEntry(
					new Airship(o.getJSONObject("constructing"), ownerBonuses),
					o.getInt("constructionCost"),
					constructionEntryIDCounter++,
					firstItemConstructionProgress));
		} else if (o.has("constructingIDList") && ip != null) {
			JSONArray l = o.getJSONArray("constructingIDList");
			for (int i = 0; i < l.length(); i++) {
				constructing.add(new ConstructionEntry(ip.read(l.getString(i)), ownerBonuses));
			}
		}
		
		if (o.has("constructionProgresses")) {
			JSONArray progs = o.getJSONArray("constructionProgresses");
			for (int i = 0; i < progs.length(); i++) {
				if (i < constructing.size()) {
					constructing.get(i).progress = progs.getInt(i);
				}
			}
		}
		if (o.has("constructionProgress") && !constructing.isEmpty()) {
			constructing.get(0).progress = o.getInt("constructionProgress");
		}
		
		if (o.has("_constructionTarget")) {
			constructionTarget = new Airship(new JSONObject(Compression.decompressFromString(o.getString("_constructionTarget"))), ownerBonuses);
		} else if (o.has("constructionTarget")) {
			constructionTarget = new Airship(o.getJSONObject("constructionTarget"), ownerBonuses);
		} else if (o.has("constructionTargetCost") && ip != null) {
			constructionTarget = new Airship(ip.read(id + "_constructionTarget_" + constructionTargetID), ownerBonuses);
		}
		
		if (o.has("_defences")) {
			JSONArray a = o.getJSONArray("_defences");
			for (int i = 0; i < a.length(); i++) {
				defences.add(new Airship(new JSONObject(Compression.decompressFromString(a.getString(i))), ownerBonuses));
			}
		} else if (o.has("defencesIDs") && ip != null) {
			JSONArray a = o.getJSONArray("defencesIDs");
			for (int i = 0; i < a.length(); i++) {
				defences.add(new Airship(ip.read(a.getString(i)), ownerBonuses));
			}
		} else {
			JSONArray a = o.getJSONArray("defences");
			for (int i = 0; i < a.length(); i++) {
				defences.add(new Airship(a.getJSONObject(i), ownerBonuses));
			}
		}
		
		if (o.has("_ground")) {
			JSONArray a = o.getJSONArray("_floaters");
			for (int i = 0; i < a.length(); i++) {
				floaters.add(new LandFormation(new JSONObject(Compression.decompressFromString(a.getString(i))), mappingRef));
			}
			ground = new LandFormation(new JSONObject(Compression.decompressFromString(o.getString("_ground"))), mappingRef);
		} else if (o.has("ground")) {
			JSONArray a = o.getJSONArray("floaters");
			for (int i = 0; i < a.length(); i++) {
				floaters.add(new LandFormation(a.getJSONObject(i), mappingRef));
			}
			ground = new LandFormation(o.getJSONObject("ground"), mappingRef);
		} else if (ip != null) {
			JSONObject landscape = ip.read(id + "_landscape");
			ground = new LandFormation(landscape.getJSONObject("ground"), mappingRef);
			JSONArray a = landscape.getJSONArray("floaters");
			for (int i = 0; i < a.length(); i++) {
				floaters.add(new LandFormation(a.getJSONObject(i), mappingRef));
			}
		} else {
			generateLand(new GuardedRandom(id * 120821 + 1928), Loadable.all(CombatBackgroundFlavor.class).get(0));
		}
	}
	
	public JSONObject toJSON(OutPipe op) {
		JSONObject o = new JSONObject()
				.put("id", id)
				.put("x", x)
				.put("y", y)
				.put("constructionEntryIDCounter", constructionEntryIDCounter)
				.put("landscapeVersion", landscapeVersion)
				.put("coastal", coastal);
		op.register(this, id + "_landscape", landscapeVersion);
		
		JSONArray constructingIDList = new JSONArray();
		for (ConstructionEntry c : constructing) {
			op.register(this, id + "_constructionQueue_" + c.id, c.ship == null ? 1 : c.ship.version);
			constructingIDList.put(id + "_constructionQueue_" + c.id);
		}
		o.put("constructingIDList", constructingIDList);
		if (getConstructionTarget() != null) {
			op.register(this, id + "_constructionTarget_" + constructionTargetID, 1);
			o.put("constructionTargetID", constructionTargetID);
		}
		JSONArray a = new JSONArray();
		o.put("defencesIDs", a);
		for (Airship ship : defences) {
			op.register(this, ship.networkID, ship.version);
			a.put(ship.networkID);
		}
		if (!constructing.isEmpty()) {
			JSONArray progs = new JSONArray();
			o.put("constructionProgresses", progs);
			for (ConstructionEntry c : constructing) {
				progs.put(c.progress);
			}
		}
		return o;
	}
	
	@Override
	public JSONObject write(String writeID) {
		if (writeID.equals(id + "_landscape")) {
			JSONObject o = new JSONObject();
			o.put("ground", ground.toJSON());
			JSONArray a = new JSONArray();
			o.put("floaters", a);
			for (LandFormation f : floaters) {
				a.put(f.toJSON());
			}
			return o;
		}
		if (getConstructionTarget() != null && writeID.equals(id + "_constructionTarget_" + constructionTargetID)) {
			return getConstructionTarget().toJSON(null, /* storeBonuses */ false);
		}
		for (ConstructionEntry c : constructing) {
			if (writeID.equals(id + "_constructionQueue_" + c.id)) {
				return c.toJSON();
			}
		}
		for (Airship ship : defences) {
			if (writeID.equals(ship.networkID)) {
				return ship.toJSON(null, /* storeBonuses */ false);
			}
		}
		throw new RuntimeException(writeID);
	}
	
	public abstract String getDisplayName();
	
	public void addDefence(Airship building) {
		if (building.type.mobile) {
			throw new RuntimeException("The defence construction " + building.getName() + " is mobile!");
		}
		building.initWheelsLegsAndTentacles(ground, floaters, new ShipArrayList(defences));
		defences.add(building);
	}
	
	public void removeDefence(Airship s) {
		defences.remove(s);
	}
	
	public void retainDefences(ArrayList<Airship> retain) {
		defences.retainAll(retain);
	}
	
	public ArrayList<Airship> getDefences() {
		return new ArrayList<Airship>(defences);
	}
	
	public void clearDefences() {
		defences.clear();
	}
	
	public boolean hasDefence(Airship ship) {
		return defences.contains(ship);
	}
	
	public void replaceDefence(Airship original, Airship newShip) {
		defences.set(defences.indexOf(original), newShip);
	}
	
	public final void generateLand(GuardedRandom r, LandscapeType lt) {
		Utils.Pair<LandFormation, List<LandFormation>> p = LandFormation.generate(r, false, lt);
		ground = p.a;
		floaters.clear();
		floaters.addAll(p.b);
		landscapeVersion++;
	}
	
	public final void generateLand(GuardedRandom r, CombatBackgroundFlavor cbf) {
		generateLand(r, cbf.getLandscapeType(r));
	}
	
	public static enum MessageType {
		LOST_SHIP(StrategicScreen.STRONG_RED_INK),
		CONQUEST(StrategicScreen.STRONG_RED_INK),
		REPEL,
		CLEAR_NEST,
		RAID,
		REVOLT(StrategicScreen.STRONG_RED_INK),
		ECON_RECOVERY,
		MONSTER_OCCUPATION(StrategicScreen.STRONG_RED_INK),
		REARM,
		PLAGUE(StrategicScreen.PLAGUE),
		NEW_UPGRADE,
		STRIKE(StrategicScreen.STRONG_RED_INK),;
		public final Clr clr;
		private MessageType() { clr = StrategicScreen.INK; }
		private MessageType(Clr clr) { this.clr = clr; }
	}
	
	public static class Message {
		MessageType type;
		String text;
		int age;
		HashMap<Empire, String> altTexts;
		FleetOwner owner;

		public Message(FleetOwner owner, MessageType type, String text, HashMap<Empire, String> altTexts) {
			this.owner = owner;
			this.type = type;
			this.text = text;
			this.age = 0;
			this.altTexts = altTexts;
		}
		
		public String getText(Empire e) {
			return altTexts.containsKey(e) ? altTexts.get(e) : text;
		}
	}
	
	public void addMessage(FleetOwner owner, MessageType type, String text) {
		messages.add(0, new Message(owner, type, text, new HashMap<Empire, String>()));
	}
	
	public void addMessage(FleetOwner owner, MessageType type, String text, HashMap<Empire, String> altTexts) {
		messages.add(0, new Message(owner, type, text, altTexts));
	}
	
	protected void tickMessages() {
		int msz = messages.size();
		for (int mi = 0; mi < msz; mi++) {
			Message mm = messages.get(mi);
			mm.age++;
			if (mm.age >= MAP_MSG_TICK_LIFE) {
				messages.remove(mi);
				mi--;
				msz--;
			}
		}
	}
	
	public ShipList shipList(WorldMap wm) {
		return new ShipArrayList(defences);
	}
	
	public void layoutGarrison(WorldMap wm) {
		Fleet fl = wm.getGarrison(this);
		if (fl == null) { return; }
		layoutGarrison(fl);
	}

	public void layoutGarrison(Fleet fl) {
		ArrayList<Airship> ships = new ArrayList<Airship>(fl.actives);
		fl.actives.clear();
		for (Airship ship : ships) {
			if (!ship.nonCombat() && (canPlace(ship, ship.getIntX(), ship.getIntY(), 32, fl, null) || placeShip(ship, fl))) {
				fl.actives.add(ship);
			} else {
				fl.reserve.add(ship);
			}
		}
		ships = new ArrayList<Airship>(fl.reserve);
		fl.reserve.clear();
		for (Airship ship : ships) {
			if (!ship.nonCombat() && (canPlace(ship, ship.getIntX(), ship.getIntY(), 32, fl, null) || placeShip(ship, fl))) {
				fl.actives.add(ship);
			} else {
				fl.reserve.add(ship);
			}
		}
	}
	
	public boolean placeShip(Airship ship, Fleet garrison) {
		if (ship.type.onGround) {
			if (ship.type.mobile) {
				for (int sx = Combat.EXCLUSION_ZONE_W / 2; sx < combatAreaW() / 2; sx += 32) {
					PlaceShipTool.Placement pl = PlaceShipTool.getPlacement(ship, sx, AGame.GROUND_LEVEL - ship.getBBHeight() - ship.groundOffset(), ship.flipped, ground, new ShipArrayList(defences), false, 1);
					if (pl.succeeded && canPlace(ship, sx, (int) pl.y, 32, garrison, pl.landscapeTarget)) {
						ship.setX(sx);
						ship.setY((int) pl.y);
						ship.resetTentacles();
						ship.version++;
						return true;
					}
				}
			} else {
				for (int sx = combatAreaW() / 4; sx < combatAreaW() / 2; sx += 32) {
					PlaceShipTool.Placement pl = PlaceShipTool.getPlacement(ship, sx, AGame.GROUND_LEVEL - ship.getBBHeight() - ship.groundOffset(), ship.flipped, ground, new ShipArrayList(defences), false, 1);
					if (pl.succeeded && canPlace(ship, sx, (int) pl.y, 32, garrison, pl.landscapeTarget)) {
						if (pl.landscapeTarget != null) {
							pl.landscapeTarget.doLandscapingForBuilding(ship, pl.x, pl.landscapeGY, ship.flipped);
							landscapeVersion++;
						}
						ship.setX(sx);
						ship.setY((int) pl.y);
						ship.resetTentacles();
						ship.version++;
						return true;
					}
				}
				for (int sx = combatAreaW() / 4; sx >= Combat.EXCLUSION_ZONE_W / 2; sx -= 32) {
					PlaceShipTool.Placement pl = PlaceShipTool.getPlacement(ship, sx, AGame.GROUND_LEVEL - ship.getBBHeight() - ship.groundOffset(), ship.flipped, ground, new ShipArrayList(defences), false, 1);
					if (pl.succeeded && canPlace(ship, sx, (int) pl.y, 32, garrison, pl.landscapeTarget)) {
						if (pl.landscapeTarget != null) {
							pl.landscapeTarget.doLandscapingForBuilding(ship, pl.x, pl.landscapeGY, ship.flipped);
							landscapeVersion++;
						}
						ship.setX(sx);
						ship.setY((int) pl.y);
						ship.resetTentacles();
						ship.version++;
						return true;
					}
				}
			}
		} else {
			for (int sy = AGame.GROUND_LEVEL - ship.serviceCeiling(); sy < AGame.GROUND_LEVEL; sy++) {
				for (int sx = Combat.EXCLUSION_ZONE_W / 2; sx < combatAreaW() / 2; sx += 32) {
					if (canPlace(ship, sx, sy, 32, garrison, null)) {
						ship.setX(sx);
						ship.setY(sy);
						ship.resetTentacles();
						ship.version++;
						return true;
					}
				}
			}
		}

		return false;
	}
	
	public boolean canPlace(Airship ship, int x, int y, int spacing, Fleet garrison, LandFormation landscaped) {
		if (x < Combat.EXCLUSION_ZONE_W / 2) {
			//System.out.println("Cannot place " + ship.name + ": too far left.");
			return false;
		}
		if (y < AGame.GROUND_LEVEL - Combat.COMBAT_AREA_H) {
			//System.out.println("Cannot place " + ship.name + ": too far up.");
			return false;
		}
		int w = ship.getWidth() * AGame.SGS;
		if (x + w > combatAreaW() / 2 - Combat.OUTER_ZONE_W) {
			//System.out.println("Cannot place " + ship.name + ": too far right.");
			return false;
		}
		int h = ship.getHeight() * AGame.SGS;
		if (y + h > AGame.GROUND_LEVEL) {
			//System.out.println("Cannot place " + ship.name + ": too far down.");
			return false;
		}
		for (ConstructionEntry ce : constructing) {
			Airship c = ce.ship;
			if (c == null) { continue; }
			int x2 = c.getIntX();
			int w2 = c.getWidth() * AGame.SGS;
			int y2 = c.getIntY();
			int h2 = c.getHeight() * AGame.SGS;
			if (x2 + w2 + spacing > x && x + w + spacing > x2 && y2 + h2 + spacing > y && y + h + spacing > y2) {
				//System.out.println("Cannot place " + ship.name + ": intersects with construction.");
				return false;
			}
		}
		for (Airship s2 : defences) {
			int x2 = s2.getIntX();
			int w2 = s2.getWidth() * AGame.SGS;
			int y2 = s2.getIntY();
			int h2 = s2.getHeight() * AGame.SGS;
			if (x2 + w2 + spacing > x && x + w + spacing > x2 && y2 + h2 + spacing > y && y + h + spacing > y2) {
				//System.out.println("Cannot place " + ship.name + ": intersects with defences.");
				return false;
			}
		}
		if (garrison != null) {
			for (Airship s2 : garrison.actives) {
				int x2 = s2.getIntX();
				int w2 = s2.getWidth() * AGame.SGS;
				int y2 = s2.getIntY();
				int h2 = s2.getHeight() * AGame.SGS;
				if (x2 + w2 + spacing > x && x + w + spacing > x2 && y2 + h2 + spacing > y && y + h + spacing > y2) {
					//System.out.println("Cannot place " + ship.name + ": intersects with garrison.");
					return false;
				}
			}
		}
		double ox = ship.getX();
		double oy = ship.getY();
		ship.setX(x);
		ship.setY(y);
		if (ground != landscaped && ship.overlapsWith(ground, /* ignoreSoftThings */ false)) {
			ship.setX(ox);
			ship.setY(oy);
			//System.out.println("Cannot place " + ship.name + ": intersects with floater.");
			return false;
		}
		for (LandFormation lf : floaters) {
			if (ship.overlapsWith(lf, /* ignoreSoftThings */ false)) {
				ship.setX(ox);
				ship.setY(oy);
				//System.out.println("Cannot place " + ship.name + ": intersects with ground.");
				return false;
			}
		}
		return true;
	}

	public Airship getConstructionTarget() {
		return constructionTarget;
	}

	public void setConstructionTarget(Airship constructionTarget) {
		this.constructionTarget = constructionTarget;
		constructionTargetID++;
	}
	
	public abstract int getResupplySpeed(FleetOwner e, WorldMap m, StringBuilder explain);
	
	public boolean canSeeFleetsHere(WorldMap m, Empire viewer) {
		ArrayList<Fleet> fs = viewer.getFleets();
		for (int i = 0; i < fs.size(); i++) {
			Fleet f = fs.get(i);
			if (f.location == this) { return true; }
		}
		return false;
	}
}
