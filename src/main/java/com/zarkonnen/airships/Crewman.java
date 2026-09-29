package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.SpikeProfiler;
import com.zarkonnen.catengine.util.Utils.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public strictfp class  Crewman extends PhysicsRect {
	public static final int SHOUT_LENGTH = 1800;
	public static final int BIG_SHOUT_LENGTH = 1800;
	public static final int SHOUT_MIN_DELAY = 4000;
	public static final int SHOUT_MAX_DELAY = 9000;
	public static final int RECALC_JUMP_POINT_WAIT = 300;
	public static final int NEW_THROW_WAIT = 3000;
	public static final double MAX_JUMP_DOWN = 120;
	public static final double JUMP_STRAIGHT_DOWN = 128;
	public static final double ASSUMED_JUMP_DIST = 50;
	
	public int multiplayerControllerID;
	public transient int multiplayerControllerID_tmp;
	public FleetOwnerRef owner;
	
	public CrewType type;
	public int hp = 5;
	public int holdOnHp = 0;
	public int repairMs = 0;
	public transient int oldHP = 5;

	public Airship ship;
	public Airship boardingShip;
	public boolean occupied = false;
	public boolean tempSpawned;
	
	public Tile currentTile;
	public Tile movingTowards;
	public Module target;
	public Tile boarderTargetTile;
	public Tile meleeTargetTile;
	public Crewman headingFor;
	
	public Tile spawnedAtTile;
	
	public int msSinceMoved;
	public int giveResourceWait;
	public int weaponReload;
	public int msUntilRecalcJumpPoint;
	public int msUntilRecalcWalkPoint;

	public Job job;
	public Resource carrying;
	public Crewman injuredCarried;
	
	public int drowningProgress;
	public transient boolean underwater;
	
	public transient String shout;
	public transient int shoutOriginX, shoutOriginY, shoutX, shoutY, shoutW, shoutH;
	public transient int shoutMs;
	public transient int shoutCooldown;
	public transient int initialShoutCooldown;
	public transient boolean bigShout;
	//public transient Tile shoutTile;
	public transient int fallingTime;	
	public transient boolean standing;
	
	public transient double lastCrewSpeedMult = 1;
	
	public transient CrewAnimator anim = new CrewAnimator(this);
	
	// The tile to navigate to when looking for a place to leave the ship.
	public Tile boardExitTarget;
	
	// Outside movement info
	public double mvXOffset = 0;
	public double dx, dy, mvDx, mvDy, trackingDx;
	public Body attachedTo;
	public Body ignoring;
	public GridBody ultimateBoardTarget;
	public GridBody proximateBoardTarget;
	public GridRef walkToTargetGR;
	public GridRef walkToGR;
	public GridRef jumpSourceGR;
	public GridRef entryPoint;
	public Body dispersed;
	public boolean grabbed;
	public int timeSinceLaunch;
	
	public OutsideBodyPath outsideBodyPath;
	
	public Pt strafeTo;
	public Airship attackTarget;
	public Tile attackTargetTile;
	public Crewman interceptTarget;
	public Tile poppedOutOfTile;
	public boolean leaveOnEmptyAmmo;
	
	// Hooking variables.
	public boolean hookLaunched;
	public GridRef hookedGR;
	public boolean winching;
	public Pt hookSource;
	public Pt hookTarget;
	public GridBody hookTargetBody;
	public double hookProgress;
	public double hookDist;
	public int newThrowWait = 0;
	
	// Shooting
	public Crewman shootTarget;
	public int shootAccumulator;
	public int ammo;
	public int rearmAccumulator;
	
	public void softReset() {
		hp = type.maxHP;
		abandonJob("reset");
		shout = null;
		anim.stopAnimating();
		ammo = type.ammoCapacity;
		oldHP = hp;
		attackTarget = null;
		attackTargetTile = null;
		attachedTo = null;
		bigShout = false;
		boardExitTarget = null;
		boarderTargetTile = null;
		boardingShip = null;
		carrying = null;
		dispersed = null;
		dx = 0;
		dy = 0;
		entryPoint = null;
		fallingTime = 0;
		giveResourceWait = 0;
		grabbed = false;
		headingFor = null;
		hookDist = 0;
		hookLaunched = false;
		hookProgress = 0;
		hookSource = null;
		hookTarget = null;
		hookTargetBody = null;
		hookedGR = null;
		ignoring = null;
		initialShoutCooldown = 0;
		injuredCarried = null;
		interceptTarget = null;
		isSmart = false;
		jumpSourceGR = null;
		meleeTargetTile = null;
		movingTowards = null;
		msSinceMoved = 0;
		msUntilRecalcJumpPoint = 0;
		msUntilRecalcWalkPoint = 0;
		mvDx = 0;
		mvDy = 0;
		mvXOffset = 0;
		newThrowWait = 0;
		occupied = false;
		outsideBodyPath = null;
		poppedOutOfTile = null;
		proximateBoardTarget = null;
		rearmAccumulator = 0;
		repairMs = 0;
		shootAccumulator = 0;
		shootTarget = null;
		shout = null;
		shoutCooldown = 0;
		shoutH = 0;
		shoutMs = 0;
		shoutOriginX = 0;
		shoutOriginY = 0;
		shoutW = 0;
		shoutH = 0;
		shoutY = 0;
		standing = false;
		strafeTo = null;
		leaveOnEmptyAmmo = false;
		target = null;
		timeSinceLaunch = 0;
		trackingDx = 0;
		ultimateBoardTarget = null;
		weaponReload = 0;
		walkToTargetGR = null;
		walkToGR = null;
		jumpSourceGR = null;
		entryPoint = null;
		winching = false;
		newThrowWait = 0;
		drowningProgress = 0;
		underwater = false;
	}
	
	public int cheapHash() {
		int h = 7;
		h = h * 31 + shootAccumulator;
		h = h * 31 + hp;
		h = h * 31 + repairMs;
		h = h * 31 + weaponReload;
		if (ship == null && boardingShip == null) {
			h = h * 31 + new Double(getX()).hashCode();
			h = h * 31 + new Double(getY()).hashCode();
			h = h * 31 + new Double(dx).hashCode();
			h = h * 31 + new Double(dy).hashCode();
			h = h * 31 + new Double(mvDx).hashCode();
			h = h * 31 + new Double(mvDy).hashCode();
			h = h * 31 + new Double(trackingDx).hashCode();
			h = h * 31 + new Double(mvXOffset).hashCode();
		} else {
			h = h * 31 + msSinceMoved;
		}
		return h;
	}
	
	public Crewman(Airship ship, Tile tile, CrewType type) {
		this.type = type;
		this.ship = ship;
		this.currentTile = tile;
		this.spawnedAtTile = tile;
		hp = type.maxHP;
		oldHP = type.maxHP;
		shoutCooldown = AGame.ANIM_R.nextInt(SHOUT_MAX_DELAY - SHOUT_MIN_DELAY) + SHOUT_MIN_DELAY;
		initialShoutCooldown = SHOUT_MIN_DELAY;
		ammo = type.ammoCapacity;
	}
	
	private static double guardNaN(double v) {
		return Double.isNaN(v) || Double.isInfinite(v) ? 0 : v;
	}
	
	private JSONObject toJSON(OutsideBodyPath obp, Combat c) {
		JSONObject o = new JSONObject().put("nextWaypointIndex", obp.nextWaypointIndex);
		JSONArray a = new JSONArray();
		o.put("waypoints", a);
		for (GridLocation gl : obp.waypoints) {
			if (!exists(gl.body(), c)) { return null; }
			JSONObject glO = new JSONObject();
			if (gl instanceof GridRef) {
				putGridRef(glO, "gridRef", (GridRef) gl, c);
			} else if (gl instanceof Tile) {
				Tile t = (Tile) gl;
				if (!t.solid()) { return null; }
				putBody(glO, "tile", t.ship, c);
				glO.put("tileX", t.x);
				glO.put("tileY", t.y);
			}
			a.put(glO);
		}
		return o;
	}
	
	public JSONObject toJSON(Combat c) {
		JSONObject o = new JSONObject()
				.put("msSinceMoved", msSinceMoved)
				.put("giveResourceWait", giveResourceWait)
				.put("weaponReload", weaponReload)
				.put("hp", hp)
				.put("type", type.name)
				.put("occupied", occupied)
				.put("msUntilRecalcJumpPoint", msUntilRecalcJumpPoint)
				.put("msUntilRecalcWalkPoint", msUntilRecalcWalkPoint)
				.put("mvXOffset", mvXOffset)
				.put("shootAccumulator", shootAccumulator)
				.put("timeSinceLaunch", timeSinceLaunch)
				.put("repairMs", repairMs)
				.put("ammo", ammo)
				.put("rearmAccumulator", rearmAccumulator)
				.put("holdOnHp", holdOnHp)
				.put("leaveOnEmptyAmmo", leaveOnEmptyAmmo)
				.put("tempSpawned", tempSpawned);
		if (drowningProgress > 0) { o.put("drowningProgress", drowningProgress); }
		if (outsideBodyPath != null && c != null) {
			JSONObject obp = toJSON(outsideBodyPath, c);
			if (obp != null) {
				o.put("outsideBodyPath", obp);
			}
		}
		if (shootTarget != null) {
			if (boardingShip != null && boardingShip.crew.contains(shootTarget)) {
				o.put("boardingShipCrewShootTarget", boardingShip.crew.indexOf(shootTarget));
			}
			if (ship != null && ship.boarders.contains(shootTarget)) {
				o.put("shipBoardersShootTarget", ship.boarders.indexOf(shootTarget));
			}
		}
		if (strafeTo != null) {
			o.put("strafeToX", strafeTo.x);
			o.put("strafeToY", strafeTo.y);
		}
		if (attackTarget != null && c != null) {
			Side s = c.sideOf(attackTarget);
			if (s != null) {
				o.put("attackTargetSide", c.sides.indexOf(s));
				o.put("attackTargetIndex", s.ships.indexOf(attackTarget));
				if (attackTargetTile != null && attackTarget.tiles.indexOf(attackTargetTile) != -1) {
					o.put("attackTargetTileIndex", attackTarget.tiles.indexOf(attackTargetTile));
				}
			}
		}
		if (carrying != null) {
			o.put("carrying", carrying.name());
		}
		if (ship != null) {
			o.put("tile", ship.tiles.indexOf(currentTile));
			if (target != null) {
				o.put("target", ship.modules.indexOf(target));
			}
			if (boarderTargetTile != null) {
				o.put("targetTile", ship.tiles.indexOf(boarderTargetTile));
			}
			if (meleeTargetTile != null) {
				o.put("meleeTargetTile", ship.tiles.indexOf(meleeTargetTile));
			}
			if (job != null && ship.modules.contains(job.module())) {
				o.put("jobModule", ship.modules.indexOf(job.module()));
				o.put("jobIndex", job.module().jobs().indexOf(job));
			}
			if (injuredCarried != null && ship.crew.contains(injuredCarried)) {
				o.put("injuredCarried", ship.crew.indexOf(injuredCarried));
			}
			if (headingFor != null && ship.crew.contains(headingFor)) {
				o.put("headingFor", ship.crew.indexOf(headingFor));
			}
			if (movingTowards != null && ship.tiles.contains(movingTowards)) {
				o.put("movingTowards", ship.tiles.indexOf(movingTowards));
			}
			if (boardExitTarget != null && ship.tiles.contains(boardExitTarget)) {
				o.put("boardExitTarget", ship.tiles.indexOf(boardExitTarget));
			}
			if (spawnedAtTile != null && ship.tiles.contains(spawnedAtTile)) {
				o.put("spawnedAtTileIndex", ship.tiles.indexOf(spawnedAtTile));
			}
		}
		if (boardingShip != null) {
			o.put("tile", boardingShip.tiles.indexOf(currentTile));
			if (target != null) {
				o.put("target", boardingShip.modules.indexOf(target));
			}
			if (boarderTargetTile != null) {
				o.put("targetTile", boardingShip.tiles.indexOf(boarderTargetTile));
			}
			if (meleeTargetTile != null) {
				o.put("meleeTargetTile", boardingShip.tiles.indexOf(meleeTargetTile));
			}
			if (movingTowards != null && boardingShip.tiles.contains(movingTowards)) {
				o.put("movingTowards", boardingShip.tiles.indexOf(movingTowards));
			}
			if (boardExitTarget != null && boardingShip.tiles.contains(boardExitTarget)) {
				o.put("boardExitTarget", boardingShip.tiles.indexOf(boardExitTarget));
			}
		}
		if (c != null && isOutside()) {
			o.put("x", guardNaN(getX())).put("y", guardNaN(getY())).put("dx", guardNaN(dx)).put("dy", guardNaN(dy)).put("mvDx", guardNaN(mvDx)).put("mvDy", guardNaN(mvDy));
			if (attachedTo != null && c.physics.bodies.contains(attachedTo)) {
				o.put("attachedToIndex", c.physics.bodies.indexOf(attachedTo));
			}
			if (ignoring != null && c.physics.bodies.contains(ignoring)) {
				o.put("ignoringIndex", c.physics.bodies.indexOf(ignoring));
			}
			if (dispersed != null && c.physics.bodies.contains(dispersed)) {
				o.put("dispersedIndex", c.physics.bodies.indexOf(dispersed));
			}
			putBody(o, "proximateBoardTarget", proximateBoardTarget, c);
			putBody(o, "boardTarget", ultimateBoardTarget, c);
			putGridRef(o, "jumpSourceTile", jumpSourceGR, c);
			putGridRef(o, "entryPoint", entryPoint, c);
			putGridRef(o, "hookedTile", hookedGR, c);
			putGridRef(o, "walkToTargetGR", walkToTargetGR, c);
			putGridRef(o, "walkToGR", walkToGR, c);
			o.put("hookLaunched", hookLaunched);
			o.put("winching", winching);
			if (hookSource != null) {
				o.put("hookSourceX", hookSource.x);
				o.put("hookSourceY", hookSource.y);
			}
			if (hookTarget != null) {
				o.put("hookTargetX", hookTarget.x);
				o.put("hookTargetY", hookTarget.y);
			}
			putBody(o, "hookTarget", hookTargetBody, c);
			o.put("hookProgress", hookProgress);
			o.put("hookDist", hookDist);
			o.put("newThrowWait", newThrowWait);
			if (poppedOutOfTile != null && poppedOutOfTile.ship.tiles.contains(poppedOutOfTile) && exists(poppedOutOfTile.ship, c)) {
				putBody(o, "homeTile", poppedOutOfTile.ship, c);
				o.put("homeTileIndex", poppedOutOfTile.ship.tiles.indexOf(poppedOutOfTile));
			}
			if (spawnedAtTile != null && spawnedAtTile.ship.tiles.contains(spawnedAtTile) && exists(spawnedAtTile.ship, c)) {
				putBody(o, "spawnedAtTile", spawnedAtTile.ship, c);
				o.put("spawnedAtTileIndex", spawnedAtTile.ship.tiles.indexOf(spawnedAtTile));
			}
			if (interceptTarget != null) {
				int ssz = c.sides.size();
				for (int si = 0; si < ssz; si++) {
					if (c.sides.get(si).troops.contains(interceptTarget)) {
						o.put("interceptTargetSideIndex", si);
						o.put("interceptTargetIndex", c.sides.get(si).troops.indexOf(interceptTarget));
					}
				}
			}
		}
		return o;
	}
	
	private void putBody(JSONObject o, String name, GridBody gb, Combat c) {
		if (gb != null && c != null) {
			if (gb instanceof Airship) {
				Airship s = (Airship) gb;
				Side side = c.sideOf(s);
				if (side != null) {
					o.put(name + "SideIndex", c.sides.indexOf(side));
					o.put(name + "ShipIndex", side.ships.indexOf(gb));
				}
			}
			if (gb instanceof LandFormation && c.landFormations.indexOf(gb) != -1) {
				o.put(name + "LFIndex", c.landFormations.indexOf(gb));
			}
		}
	}
	
	private void putGridRef(JSONObject o, String name, GridRef gr, Combat c) {
		if (gr != null && c != null) {
			putBody(o, name, gr.body, c);
			o.put(name + "X", gr.gridX);
			o.put(name + "Y", gr.gridY);
		}
	}
	
	public Crewman(JSONObject o, Airship ship, Airship boardingShip) {
		this.ship = ship;
		this.boardingShip = boardingShip;
		if (ship != null) {
			currentTile = ship.tiles.get(o.getInt("tile"));
			if (o.has("target")) {
				target = ship.modules.get(o.getInt("target"));
			}
			if (o.has("targetTile") && o.getInt("targetTile") != -1) {
				boarderTargetTile = ship.tiles.get(o.getInt("targetTile"));
			}
			if (o.has("meleeTargetTile")) {
				meleeTargetTile = ship.tiles.get(o.getInt("meleeTargetTile"));
			}
			if (o.has("jobModule") && ship.modules.size() > o.getInt("jobModule") && ship.modules.get(o.getInt("jobModule")).jobs().size() > o.getInt("jobIndex")) {
				job = ship.modules.get(o.getInt("jobModule")).jobs().get(o.getInt("jobIndex"));
			}
			if (o.has("movingTowards")) {
				movingTowards = ship.tiles.get(o.getInt("movingTowards"));
			}
			if (o.has("boardExitTarget")) {
				boardExitTarget = ship.tiles.get(o.getInt("boardExitTarget"));
			}
		}
		if (boardingShip != null) {
			currentTile = boardingShip.tiles.get(o.getInt("tile"));
			if (o.has("target")) {
				target = boardingShip.modules.get(o.getInt("target"));
			}
			if (o.has("targetTile") && o.getInt("targetTile") != -1) {
				boarderTargetTile = boardingShip.tiles.get(o.getInt("targetTile"));
			}
			if (o.has("meleeTargetTile")) {
				meleeTargetTile = boardingShip.tiles.get(o.getInt("meleeTargetTile"));
			}
			if (o.has("movingTowards")) {
				movingTowards = boardingShip.tiles.get(o.getInt("movingTowards"));
			}
			if (o.has("boardExitTarget")) {
				boardExitTarget = boardingShip.tiles.get(o.getInt("boardExitTarget"));
			}
		}
		repairMs = o.optInt("repairMs", 0);
		mvXOffset = o.optDouble("mvXOffset", 0);
		msSinceMoved = o.getInt("msSinceMoved");
		weaponReload = o.optInt("weaponReload", 0);
		shootAccumulator = o.optInt("shootAccumulator", 0);
		msUntilRecalcJumpPoint = o.optInt("msUntilRecalcJumpPoint", 0);
		msUntilRecalcWalkPoint = o.optInt("msUntilRecalcWalkPoint", 0);
		timeSinceLaunch = o.optInt("timeSinceLaunch", 0);
		ammo = o.optInt("ammo", 0);
		rearmAccumulator = o.optInt("rearmAccumulator", 0);
		hp = o.getInt("hp");
		holdOnHp = o.optInt("holdOnHp", 0);
		leaveOnEmptyAmmo = o.optBoolean("leaveOnEmptyAmmo", false);
		oldHP = hp;
		tempSpawned = o.optBoolean("tempSpawned", false);
		drowningProgress = o.optInt("drowningProgress", 0);
		if (o.has("giveResourceWait")) {
			giveResourceWait = o.getInt("giveResourceWait");
		}
		if (o.has("carrying")) {
			carrying = Resource.valueOf(o.getString("carrying"));
		}
		CrewType ct = null;
		String[] typeNames = {
			o.optString("type", "sailor").toLowerCase(Locale.ENGLISH),
			o.optString("type", "sailor")
		};
		for (String typeName : typeNames) {
			if (ship != null) {
				for (Substitution sub : Loadable.all(Substitution.class)) {
					//System.out.println("subby " + sub.fromCrew + " " + sub.toCrew);
					if (typeName.equals(sub.fromCrew) && sub.forTypes.contains(ship.type)) {
						typeName = sub.toCrew;
					}
				}
			}
			try {
				ct = CrewType.ofName(typeName);
				break;
			} catch (Loadable.NotFoundException e) {
				if (typeName.equals(typeNames[typeNames.length - 1])) {
					throw e;
				}
			}
		}
		type = ct;
		occupied = o.optBoolean("occupied", false);
		setX(o.optDouble("x", 0));
		setY(o.optDouble("y", 0));
		dx = o.optDouble("dx", 0);
		dy = o.optDouble("dy", 0);
		mvDx = o.optDouble("mvDx", 0);
		mvDy = o.optDouble("mvDy", 0);
		if (o.has("spawnedAtTileIndex") && !o.has("spawnedAtTile") && ship != null) {
			spawnedAtTile = ship.tiles.get(o.getInt("spawnedAtTileIndex"));
		}
	}
		
	public void finishLoadingWithCombat(JSONObject o, Combat c) {
		if (ship != null) {
			if (o.has("injuredCarried")) {
				injuredCarried = ship.crew.get(o.getInt("injuredCarried"));
			}
			if (o.has("headingFor")) {
				headingFor = ship.crew.get(o.getInt("headingFor"));
			}
		}
		if (c != null) {
			if (o.has("attachedToIndex")) {
				attachedTo = c.physics.bodies.get(o.getInt("attachedToIndex"));
			}
			if (o.has("ignoringIndex")) {
				ignoring = c.physics.bodies.get(o.getInt("ignoringIndex"));
			}
			
			if (o.has("attackTargetSide")) {
				attackTarget = c.sides.get(o.getInt("attackTargetSide")).ships.get(o.getInt("attackTargetIndex"));
				if (o.has("attackTargetTileIndex")) {
					attackTargetTile = attackTarget.tiles.get(o.getInt("attackTargetTileIndex"));
				}
			}
			if (o.has("strafeToX")) {
				strafeTo = new Pt(o.getDouble("strafeToX"), o.getDouble("strafeToY"));
			}
			
			dispersed = getBody(o, "dispersed", c);
			proximateBoardTarget = getBody(o, "proximateBoardTarget", c);
			ultimateBoardTarget = getBody(o, "boardTarget", c);
			jumpSourceGR = getGridRef(o, "jumpSourceTile", c);
			walkToTargetGR = getGridRef(o, "walkToTargetGR", c);
			walkToGR = getGridRef(o, "walkToGR", c);
			entryPoint = getGridRef(o, "entryPoint", c);
			hookedGR = getGridRef(o, "hookedTile", c);
			hookLaunched = o.optBoolean("hookLaunched");
			winching = o.optBoolean("winching");
			if (o.has("hookSourceX")) {
				hookSource = new Pt(o.getDouble("hookSourceX"), o.getDouble("hookSourceY"));
			}
			if (o.has("hookTargetX")) {
				hookTarget = new Pt(o.getDouble("hookTargetX"), o.getDouble("hookTargetY"));
			}
			hookTargetBody = getBody(o, "hookTarget", c);
			hookProgress = o.optDouble("hookProgress", 0);
			hookDist = o.optDouble("hookDist", 0);
			newThrowWait = o.optInt("newThrowWait", 0);
			
			if (o.has("outsideBodyPath")) {
				JSONObject obpO = o.getJSONObject("outsideBodyPath");
				JSONArray wps = obpO.getJSONArray("waypoints");
				ArrayList<GridLocation> gls = new ArrayList<GridLocation>();
				for (int i = 0; i < wps.length(); i++) {
					JSONObject wpO = wps.getJSONObject(i);
					if (wpO.has("gridRefX")) {
						gls.add(getGridRef(wpO, "gridRef", c));
					} else {
						gls.add(((Airship) getBody(wpO, "tile", c)).tileAt(wpO.getInt("tileX"), wpO.getInt("tileY")));
					}
				}
				outsideBodyPath = new OutsideBodyPath(gls);
				outsideBodyPath.nextWaypointIndex = obpO.getInt("nextWaypointIndex");
			}
			
			if (o.has("homeTileIndex")) {
				poppedOutOfTile = ((Airship) getBody(o, "homeTile", c)).tiles.get(o.getInt("homeTileIndex"));
			}
			
			if (o.has("spawnedAtTileIndex") && o.has("spawnedAtTile")) {
				spawnedAtTile = ((Airship) getBody(o, "spawnedAtTile", c)).tiles.get(o.getInt("spawnedAtTileIndex"));
			}
			
			if (o.has("interceptTargetIndex")) {
				interceptTarget = c.sides.get(o.getInt("interceptTargetSideIndex")).troops.get(o.getInt("interceptTargetIndex"));
			}
		}		
		
		if (o.has("boardingShipCrewShootTarget") && boardingShip != null) {
			shootTarget = boardingShip.crew.get(o.getInt("boardingShipCrewShootTarget"));
		}
		if (o.has("shipBoardersShootTarget") && ship != null) {
			shootTarget = ship.boarders.get(o.getInt("shipBoardersShootTarget"));
		}
	}
	
	private GridBody getBody(JSONObject o, String name, Combat c) {
		if (c != null && o.has(name + "SideIndex")) {
			Side side = c.sides.get(o.getInt(name + "SideIndex"));
			return side.ships.get(o.getInt(name + "ShipIndex"));
		}
		if (c != null && o.has(name + "LFIndex")) {
			return c.landFormations.get(o.getInt(name + "LFIndex"));
		}
		return null;
	}
	
	private GridRef getGridRef(JSONObject o, String name, Combat c) {
		GridBody gb = getBody(o, name, c);
		if (gb != null) {
			return new GridRef(o.getInt(name + "X"), o.getInt(name + "Y"), gb);
		}
		return null;
	}
	
	public boolean ignores(Body b) {
		if (b.isImmobile()) { return false; }
		if (b == ignoring) { return true; }
		return attachedTo != null && attachedTo.isImmobile() && b != attachedTo && b != ultimateBoardTarget && b != proximateBoardTarget;
	}
	
	public Crewman carrier() {
		Airship sh = ship == null ? boardingShip : ship;
		if (sh == null) { return null; }
		int csz = sh.crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = sh.crew.get(ci);
			if (cm.injuredCarried == this) {
				return cm;
			}
		}
		return null;
	}
	
	public boolean beingCarried() {
		Airship sh = ship == null ? boardingShip : ship;
		if (sh == null) { return false; }
		int csz = sh.crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = sh.crew.get(ci);
			if (cm.injuredCarried == this) {
				return true;
			}
		}
		return false;
	}
	
	public boolean needsRescue(boolean canBeDead) {
		if (!((canBeDead || alive()) && !active() && !(currentTile.module.type.getSickbay(currentTile.module.ship.currentBonuses) > 0 && currentTile.module.hp > 0 && currentTile.module.somewhatStaffed()))) {
			return false;
		}
		int csz = ship.crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = ship.crew.get(ci);
			if (cm.headingFor == this || cm.injuredCarried == this) { return false; }
		}
		return true;
	}
	
	public boolean active() {
		return hp >= type.minWorkingHP;
	}
	
	public boolean alive() {
		return hp > 0;
	}
	
	public double speed(Tile targetTile, double fleetCrewSpeedMult) {
  		double s = 1.0 * hp / type.maxHP;
		if (carrying != null) { s *= type.carrySpeedMult; }
		if (targetTile.y < currentTile.y) {
			s *= type.goingUpSpeedMult;
		}
		if (targetTile.module.fire > 0) {
			s *= type.fireSpeedMult;
		}
		if (ship != null && ship.getCaptain() != null) {
			s = s * (100 + ship.getCaptain().type.crewSpeedPercent) / 100.0;
		}
		if (ship != null) {
			s = s * (100 + ship.crewSpeedPercentFromMedals) / 100.0;
		}
		if (ship != null) {
			s *= (1 + ship.crewExperience);
		}
		if (ship != null && (ship.doubleTimeTime > 0 || ship.holdOnTime > 0)) {
			s *= Airship.DOUBLE_TIME_SPEED_MULT;
		}
		if (boardingShip != null && boardingShip.fearTime > 0) {
			s *= Airship.FEAR_SPEED_MULT;
		}
		if (currentTile.isPartiallySubmerged()) {
			s *= Airship.WADING_SPEED_MULT;
		}
		return s * type.insideSpeed * fleetCrewSpeedMult;
	}
	
	public void sanityCheck(Combat c) {
		/*if (carrying != Resource.INJURED && injuredCarried != null) {
			System.out.println("Carrying person but carrying resource: " + carrying.name() + " with job " + job + ".");
		}
		if (target != null && job == null) {
			System.out.println("Target without job.");
			System.out.println(target.type.getName());
			System.out.println(carrying);
		}
		if (target != null && headingFor != null) {
			System.out.println("Both module and CM target.");
		}*/
	}
	
	private void dropCarried() {
		carrying = null;
		if (injuredCarried != null) {
			injuredCarried.currentTile = currentTile;
			injuredCarried.msSinceMoved = 0;
			injuredCarried = null;
		}
	}
	
	public void abandonJob(String cause) {
		/*if (job != null) {
			System.out.println(cause + " " + ship.crew.indexOf(this) + " " + job.getClass().getSimpleName());
		}*/
		target = null;
		boarderTargetTile = null;
		headingFor = null;
		job = null;
		dropCarried();
	}
	
	private void animTick(int ms, Combat c, double fleetCrewSpeedMult) {
		Airship sh = ship == null ? boardingShip : ship;
		double tx;
		double ty;
		boolean flipped = false;
		if (sh != null) {
			tx = sh.getX() + sh.gridXToWorldX(currentTile.x, 1) * AGame.SGS;
			ty = sh.getY() + currentTile.y * AGame.SGS;
			Tile t2 = movingTowards;
			if (t2 != null) {
				if (sh.flipped) {
					tx -= (t2.x - currentTile.x) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS;
				} else {
					tx += (t2.x - currentTile.x) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS;
				}
				ty += (t2.y - currentTile.y) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS;
			}
			tx += AGame.SGS / 2 - getBBWidth() / 2;
			ty += AGame.SGS - 1 - getBBHeight();
			flipped = sh.flipped;
		} else {
			tx = getX();
			ty = getY();
		}
		anim.tick(ms, c, tx, ty, flipped, beingCarried());
	}
	
	public void tick(int ms, Combat c, Side shipSide, boolean won, boolean lost, boolean onViewingSide, boolean canDoPathing, double fleetCrewSpeedMult, double fleetRepairAmountMult, double fleetFirefightAmountMult) {
		//#SpikeProfiler.start("anim");
		lastCrewSpeedMult = fleetCrewSpeedMult;
		animTick(ms, c, fleetCrewSpeedMult);
		//#SpikeProfiler.endStart("anim", "tick");
		if (ship != null) {
			if (crewTick(ms, c, shipSide, won, lost, onViewingSide, canDoPathing, fleetCrewSpeedMult, fleetRepairAmountMult, fleetFirefightAmountMult)) { return; }
		}
		if (boardingShip != null) {
			if (boarderTick(ms, c, shipSide, won, lost, canDoPathing)) { return; }
		}
		//#SpikeProfiler.endStart("tick", "shoot");
		shoot(c, ms, onViewingSide, fleetCrewSpeedMult);
		//#SpikeProfiler.end("shoot");
		if (oldHP >= type.minWorkingHP && hp < type.minWorkingHP) {
			anim.animate(AnimationType.COLLAPSE, null);
		} else if (active()) {
			if (!occupied && ship != null && anim.specialDuration <= 0 && won) {
				anim.animate(AnimationType.HAPPY, null);
			}
			if (!occupied && ship != null && anim.specialDuration <= 0 && lost) {
				anim.animate(AnimationType.SAD, null);
			}
		}
		oldHP = hp;
	}
	
	public void shoot(Combat c, int ms, boolean onViewingSide, double fleetCrewSpeedMult) {
		weaponReload -= ms;
		if (!active() || weaponReload > 0) { return; }
		if (!type.canBoard && ship == null) { return; }
		if (type.shootsShips && type.shootTroopsRange <= 0) { return; }
		if (occupied) { return; }
		boolean interesting = interesting(currentTile.module);
		// Unthreatened uninjured sailors don't shoot.
		/*if (!type.canBoard && !type.doesGuard && hp == type.maxHP && !interesting) {
			return;
		}*/
		if (shootTarget != null) {
			if (ship != null) {
				if (!shootTarget.active() ||
					shootTarget.currentTile == null ||
					shootTarget.currentTile.module != currentTile.module ||
					!ship.boarders.contains(shootTarget))
				{
					shootTarget = null;
					shootAccumulator = 0;
					//System.out.println("target left, stop anim");
					anim.stopAnimating();
				}
			} else if (boardingShip != null) {
				if (!shootTarget.active() ||
					shootTarget.currentTile == null ||
					shootTarget.currentTile.module != currentTile.module ||
					!boardingShip.crew.contains(shootTarget))
				{
					shootTarget = null;
					shootAccumulator = 0;
					//System.out.println("target left, stop anim");
					anim.stopAnimating();
				}
			}
		}
		if (shootTarget == null) {
			// Find a target.
			Crewman victim = null;
			if (ship != null) {
				int bsz = ship.boarders.size();
				for (int bi = 0; bi < bsz; bi++) {
					Crewman b = ship.boarders.get(bi);
					if (b.active() && b.currentTile.module == currentTile.module) {
						if (interesting || b.type.canBoard || b.type.doesGuard || b.hp < type.maxHP) {
							if (type.meleeAttack && !meleeAdjacent(currentTile, b.currentTile)) { continue; }
							victim = b;
							break;
						}
					}
				}
			}
			if (boardingShip != null) {
				int csz = boardingShip.crew.size();
				for (int ci = 0; ci < csz; ci++) {
					Crewman cm = boardingShip.crew.get(ci);
					if (cm.active() && cm.currentTile.module == currentTile.module) {
						if ((!type.doesWork && !boardingShip.captured) || interesting || cm.type.canBoard || cm.type.doesGuard) {
							if (type.meleeAttack && !meleeAdjacent(currentTile, cm.currentTile)) { continue; }
							victim = cm;
							break;
						}
					}
				}
			}
			if (victim == null) { return; }
			shootTarget = victim;
			shootAccumulator = 0;
			//System.out.println("found new target");
		}
		
		if (shootTarget == null) {
			shootAccumulator = 0;
			return;
		}
		
		Airship sh = ship == null ? boardingShip : ship;
		anim.continueAnimating(shootTarget.currentTile.x > currentTile.x != sh.flipped ? AnimationType.SHOOT_RIGHT : AnimationType.SHOOT_LEFT, null);
		shootAccumulator += ms;
		
		//System.out.println("aiming " + shootAccumulator);
		
		if (shootAccumulator >= type.aimTime) {
			double sx = sh.getX() + sh.gridXToWorldX(currentTile.x, 1) * AGame.SGS + getBBWidth() / 2;
			double sy = sh.getY() + currentTile.y * AGame.SGS + type.barrelY;
			if (type.attackSnd != null) {
				c.play(type.attackSnd, sx, sy, 0, 0, onViewingSide);
			}
			Tile t2 = movingTowards;
			if (t2 != null) {
				if (sh.flipped) {
					sx -= (t2.x - currentTile.x) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS;
				} else {
					sx += (t2.x - currentTile.x) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS;
				}
				sy += (t2.y - currentTile.y) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS;
			}

			double tx = sh.getX() + sh.gridXToWorldX(shootTarget.currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
			double ty = sh.getY() + shootTarget.currentTile.y * AGame.SGS + AGame.SGS / 2;

			sx += (tx < sx ? getBBWidth() - type.barrelX : type.barrelX);
			
			if (type.attackParticle != null) {
				c.particles.add(new Particle(type.attackParticle, sx, sy));
			}
			
			//System.out.println(type.name + " o? " + occupied + " b? " + (boardingShip != null) + " c? " + (ship != null) + " shooting " + shootTarget.type.name + " o? " + shootTarget.occupied + " b? " + (shootTarget.boardingShip != null) + " c? " + (shootTarget.ship != null));

			for (int i = 0; i < type.numShots; i++) {
				c.shots.add(new Shot(
						sh, tx + (c.r.nextDouble() * 2 - 1) * AGame.SGS * type.inaccuracy, ty + (c.r.nextDouble() * 2 - 1) * AGame.SGS * type.inaccuracy, this.type, sx, sy,
						/* internal */ true, ship != null));
				c.incStat(this, "crewShotsFired " + type.name);
			}
			c.msSinceInterestingCombatEvent = 0;
			weaponReload = weaponReload(c);
			shootAccumulator = 0;
			anim.continueAnimating(shootTarget.currentTile.x > currentTile.x != sh.flipped ? AnimationType.SHOOT_RIGHT : AnimationType.SHOOT_LEFT, null);
		}
	}
	
	public int weaponReload(Combat c) {
		Side mySide = c.sideOf(boardingShip == null ? ship : boardingShip);
		int rel = type.weaponReload.get(mySide == null ? new BonusSet() : mySide.bonuses);
		if (ship != null) {
			rel *= 0.75;
		}
		if (boardingShip != null && boardingShip.fearTime > 0) {
			rel *= Airship.FEAR_RELOAD_TIME_MULT;
		}
		
		return rel;
	}

	public boolean interesting(Module m) {
		if (m == null) { return false; }
		Airship sh = ship == null ? boardingShip : ship;
		if (m.type.isWeapon() || m.type.getCommand(sh.currentBonuses) > 0 || m.type.getPropulsion(sh.currentBonuses) > 0) {
			int csz = sh.crew.size();
			for (int ci = 0; ci < csz; ci++) {
				Crewman cm = sh.crew.get(ci);
				if (cm.active() && cm.currentTile.module == m) {
					return true;
				}
			}
		} else {
			int csz = sh.crew.size();
			for (int ci = 0; ci < csz; ci++) {
				Crewman cm = sh.crew.get(ci);
				if ((cm.type.canBoard || cm.type.doesGuard) && cm.active() && cm.currentTile.module == m) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean meleeAdjacent(Tile a, Tile b) {
		return a.module == b.module && a.y == b.y && StrictMath.abs(a.x - b.x) < 2;
	}
	
	private void planBoardingShipExit(Combat c, Combat.Side shipSide) {
		// Find another enemy ship to board, or then a friendly ship, or then the ground.
		Airship closest = null;
		double bestd2 = 0;
		for (Airship s : shipSide.ships) {
			if (s == boardingShip) { continue; }
			if (!s.inCombat(c)) { continue; }
			double d2 =
					(boardingShip.getX() + boardingShip.getBBWidth() / 2 - s.getX() - s.getBBWidth() / 2) * (boardingShip.getX() + boardingShip.getBBWidth() / 2 - s.getX() - s.getBBWidth() / 2) +
					(boardingShip.getY() + boardingShip.getBBHeight() / 2 - s.getY() - s.getBBHeight() / 2) * (boardingShip.getY() + boardingShip.getBBHeight() / 2 - s.getY() - s.getBBHeight() / 2);
			if (closest == null || d2 < bestd2) {
				closest = s;
				bestd2 = d2;
			}
		}
		
		if (closest != null) {
			ultimateBoardTarget = closest;
		}
	}
		
	public boolean boarderTick(int ms, Combat c, Combat.Side shipSide, boolean won, boolean lost, boolean canDoPathing) {
		if (drowningTick(c, ms)) {
			return false;
		}
		
		if (!type.canBoard && !type.doesGuard) { return false; }
		if (boarderTargetTile != null && !boardingShip.tiles.contains(boarderTargetTile)) {
			boarderTargetTile = null;
		}
		if (meleeTargetTile != null && !boardingShip.tiles.contains(meleeTargetTile)) {
			meleeTargetTile = null;
		}
		
		boarderShout(ms, c);
		
		if (!active()) { return false; }
				
		// if the current tile is in a LOI, do not worry
		// otherwise, find the closest LOI and set pathing target to it
		
		// Path to leave ship if boarding.
		if (ultimateBoardTarget != null && ultimateBoardTarget != boardingShip) { // qqDPS ship->boardingShip
			if (currentTile.enterable()) {
				if (canPopOut()) {
					popOut(c.otherSide(c.sideOf(boardingShip)), c, 1);
					return true;
				}
				return false; // Have to wait.
			}
			if (boardExitTarget != null && (!boardExitTarget.enterable() || !boardingShip.containsTile(boardExitTarget))) {
				boardExitTarget = null;
			}
			if (boardExitTarget == null) {
				int tsz = boardingShip.tiles.size();
				int closestDist = 0;
				for (int ti = 0; ti < tsz; ti++) {
					Tile t = boardingShip.tiles.get(ti);
					if (t.enterable()) {
						ArrayList<Tile> path = boardingShip.getPath(currentTile, t);
						if (path != null && (boardExitTarget == null || path.size() < closestDist)) {
							boardExitTarget = t;
							closestDist = path.size();
						}
					}
				}
			}
			if (boardExitTarget != null) {
				ArrayList<Tile> path = boardingShip.getPath(currentTile, boardExitTarget);
				if (path != null) {
					boarderTargetTile = path.get(0);
					msSinceMoved += ms;
				}
			}
		} else if (type.meleeAttack) {
			target = null;
			// Check if our current target tile is in a boring place or not in
			// a place where we can bite anyone. If so, we want a new one.
			if (meleeTargetTile != null) {
				if (!boardingShip.tiles.contains(meleeTargetTile) || !interesting(meleeTargetTile.module)) {
					meleeTargetTile = null;
					boarderTargetTile = null;
				} else {
					boolean found = false;
					int csz = boardingShip.crew.size();
					for (int ci = 0; ci < csz; ci++) {
						Crewman cm = boardingShip.crew.get(ci);
						if (cm.active() && (!type.doesWork || (cm.type.doesGuard || cm.type.canBoard || cm.hp < cm.type.maxHP))) {
							if (meleeAdjacent(cm.currentTile, meleeTargetTile)) {
								found = true;
								break;
							}
						}
					}
					if (!found) {
						boarderTargetTile = null;
						meleeTargetTile = null;
					}
				}
			}
			
			if (meleeTargetTile == null) {
				// Find something to bite!
				int best = 0;
				int csz = boardingShip.crew.size();
				for (int ci = 0; ci < csz; ci++) {
					Crewman cm = boardingShip.crew.get(ci);
					if (!cm.active()) { continue; }
					/*if (type.doesWork && !(cm.type.doesGuard || cm.type.canBoard || cm.hp < cm.type.maxHP)) {
						continue;
					}*/
					if (!interesting(cm.currentTile.module)) {
						continue;
					}
					// Up to three tile candidates for melee - left, right, same tile.
					for (int xx = -1; xx < 2; xx++) {
						Tile tt = boardingShip.tileAt(cm.currentTile.x + xx, cm.currentTile.y);
						if (tt == null || tt.module != cm.currentTile.module) { continue; }
						ArrayList<Tile> path = boardingShip.getPath(currentTile, tt);
						// Bias away from going same place as others.
						if (path == null) { continue; }
						int dist = path.size();
						int bsz = boardingShip.boarders.size();
						for (int bi = 0; bi < bsz; bi++) {
							Crewman b = boardingShip.boarders.get(bi);
							if (b != this && (b.meleeTargetTile != null && b.meleeTargetTile.module == tt.module) || (b.target != null && b.target == tt.module)) {
								//System.out.println("d4");
								dist *= 4;
								break;
							}
						}
						if ((meleeTargetTile == null || dist < best)) {
							meleeTargetTile = tt;
							boarderTargetTile = path.isEmpty() ? tt : path.get(0); // Uh shouldn't that be the 0th element of the path?
							best = dist;
						}
					}
				}
				/*if (meleeTargetTile != null) {
					System.out.println(type.name + " -> " + meleeTargetTile.module.type.name + " " + meleeTargetTile.x + " " + meleeTargetTile.y + " of " + ntt);
				}*/
			}
			
			if (meleeTargetTile == null) {
				//System.out.println("no TT found yet, looking for command modules");
				int best = 0;
				int msz = boardingShip.modules.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = boardingShip.modules.get(mi);
					if (m.type.getCommand(boardingShip.currentBonuses) > 0 && m.hp > 0) {
						ArrayList<Tile> path = boardingShip.getPath(currentTile, m);
						if (path != null && (target == null || best > path.size())) {
							target = m;
							best = path.size();
						}
					}
				}
				
				/*if (target != null) {
					System.out.println(type.name + " => " + target.type.name);
				}*/
				
				// Tile pathing.
				boarderTargetTile = null;
				if (target != null && currentTile.module != target) {
					ArrayList<Tile> path = boardingShip.getPath(currentTile, target);
					if (path == null) {
						boarderTargetTile = null;
						meleeTargetTile = null;
					} else {
						boarderTargetTile = path.get(0);
						meleeTargetTile = path.get(path.size() - 1);
					}
				}
				target = null;
			} else {
				if (currentTile != meleeTargetTile && currentTile == boarderTargetTile) {
					ArrayList<Tile> path = boardingShip.getPath(currentTile, meleeTargetTile);
					if (path != null && !path.isEmpty()) {
						boarderTargetTile = path.get(0);
					}
				}
			}
			
			if (boarderTargetTile == null && !type.doesWork) {
				planBoardingShipExit(c, shipSide);
			}
		} else {
			if (interesting(currentTile.module)) {
				target = null;
			} else if (!interesting(target)) {
				target = null;
				int best = 0;
				int msz = boardingShip.modules.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = boardingShip.modules.get(mi);
					if (interesting(m)) {
						ArrayList<Tile> path = boardingShip.getPath(currentTile, m);
						if (path != null) {
							// Bias away from going same place as others.
							int dist = path.size();
							int bsz = boardingShip.boarders.size();
							for (int bi = 0; bi < bsz; bi++) {
								Crewman b = boardingShip.boarders.get(bi);
								if (b != this && b.target == m) {
									dist *= 2;
									break;
								}
							}
							if ((target == null || best > dist)) {
								target = m;
								best = dist;
							}
						}
					}
				}
			}
			// Path to command centers once out of ideas to facilitate takeover.
			if (target == null && type.doesWork) {
				int best = 0;
				int msz = boardingShip.modules.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = boardingShip.modules.get(mi);
					if (m.type.getCommand(boardingShip.currentBonuses) > 0 && m.hp > 0) {
						ArrayList<Tile> path = boardingShip.getPath(currentTile, m);
						if (path != null && (target == null || best > path.size())) {
							target = m;
							best = path.size();
						}
					}
				}
			}

			// Still nothing? Find enemy troops to shoot.
			if (target == null) {
				int csz = boardingShip.crew.size();
				int best = 0;
				for (int ci = 0; ci < csz; ci++) {
					Crewman cm = boardingShip.crew.get(ci);
					if (!(cm.type.canBoard || cm.type.doesGuard) || !cm.active()) { continue; }
					Module m = cm.currentTile.module;
					ArrayList<Tile> path = boardingShip.getPath(currentTile, m);
					if (path != null && (target == null || best > path.size())) {
						target = m;
						best = path.size();
					}
				}
			}

			// Still nothing? Just murder randomly.
			if (target == null) {
				int csz = boardingShip.crew.size();
				int best = 0;
				for (int ci = 0; ci < csz; ci++) {
					Crewman cm = boardingShip.crew.get(ci);
					if (!cm.active()) { continue; }
					Module m = cm.currentTile.module;
					ArrayList<Tile> path = boardingShip.getPath(currentTile, m);
					if (path != null && (target == null || best > path.size())) {
						target = m;
						best = path.size();
					}
				}
			}

			// Tile pathing.
			boarderTargetTile = null;
			if (target != null && currentTile.module != target) {
				ArrayList<Tile> path = boardingShip.getPath(currentTile, target);
				if (path == null) {
					target = null;
					boarderTargetTile = null;
				} else {
					boarderTargetTile = path.get(0);
				}
			}
			
			if (boarderTargetTile == null && !type.doesWork) {
				planBoardingShipExit(c, shipSide);
			}
		}
		
		if (weaponReload > 0) {
			ms *= type.reloadSlowdown;
			// Move slower while reloading. Both realistic and prevents blundering into enemies unready.
		}
		
		if (movingTowards != null && !boardingShip.tiles.contains(movingTowards)) {
			movingTowards = null;
		}
		
		if (boarderTargetTile == currentTile) {
			movingTowards = null;
		} else {
			// May have to back out of previous movement target.
			if (boarderTargetTile != movingTowards) {
				msSinceMoved -= 2 * ms;
				if (msSinceMoved <= 0) {
					msSinceMoved *= -1;
					movingTowards = boarderTargetTile;
				}
			} else if (shootAccumulator == 0) {
				msSinceMoved += ms;
			}

			// Moving.
			if (movingTowards != null) {
				if (msSinceMoved >= currentTile.getMoveDelay() / speed(movingTowards, 1)) {
					msSinceMoved -= currentTile.getMoveDelay() / speed(movingTowards, 1);
					currentTile = movingTowards;
				}
			}
		}
		
		return false;
	}
	
	private boolean interceptTroops(Combat c) {
		if (type.isAircraft() && poppedOutOfTile != null && exists(poppedOutOfTile.ship, c) && poppedOutOfTile.ship.containsTile(poppedOutOfTile)) {
			switch (poppedOutOfTile.ship.aircraftMode) {
				case ATTACK: return false;
				case GUARD: return type.shootTroopsRange > 0;
				case INTERCEPT: return type.shootTroopsRange > 0;
			}
		}
		return type.interceptTroops;
	}
	
	public boolean resurrect(Combat c) {
		int currentHP = ship != null && ship.holdOnTime > 0 ? holdOnHp : hp;
		if (currentHP == 0) {
			double gameX = ship.getIntX() + ship.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
			double gameY = ship.getIntY() + currentTile.y * AGame.SGS + AGame.SGS / 2;
			ParticleType pt = ParticleType.ofName("necromancy");
			c.incStat(currentTile.ship, "resurrections");
			currentTile.ship.incStat("resurrections");
			holdOnHp = type.maxHP;
			hp = type.maxHP;
			for (int i = 0; i < 5; i++) {
				c.particles.add(new Particle(pt, gameX, gameY));
			}
			return true;
		}
		return false;
	}
	
	private boolean drowningTick(Combat c, int ms) {
		underwater = currentTile.isFullySubmerged();
		if (type.underwaterGraceTime > 0 && currentTile != null && currentTile.isFullySubmerged()) {
			drowningProgress += ms;
			if (drowningProgress >= type.underwaterGraceTime) {
				abandonJob("drowning");
			}
			if (drowningProgress >= type.underwaterGraceTime + type.drowningTime && alive()) {
				hp = 0;
				c.incStat(this, "crewDrowned");
				return true;
			}
			if (drowningProgress >= type.underwaterGraceTime) {
				return true;
			}
		} else {
			drowningProgress = 0;
		}
		return false;
	}
		
	public boolean crewTick(int ms, Combat c, Side mySide, boolean won, boolean lost, boolean onViewingSide, boolean canDoPathing, double fleetCrewSpeedMult, double fleetRepairAmountMult, double fleetFirefightAmountMult) {
		if (injuredCarried != null) {
			injuredCarried.currentTile = currentTile;
		}
		
		if (drowningTick(c, ms)) {
			return false;
		}
		
		double gameX = ship.getIntX() + ship.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
		double gameY = ship.getIntY() + currentTile.y * AGame.SGS + AGame.SGS / 2;

		//#SpikeProfiler.start("shout");
		shout(ms, c, won, lost);
		//#SpikeProfiler.end("shout");
		
		// Combat abilities
		if (ship != null && ship.holdOnTime > 0 && AGame.ANIM_R.nextDouble() < 0.001 * ms) {
			c.particles.add(new Particle(ParticleType.ofName("hold_on_spark"), gameX, gameY));
		}
		if (ship != null && ship.paralysisTime > 0 && currentTile != null && currentTile.module.type.getCommand(ship.currentBonuses) > 0 && AGame.ANIM_R.nextDouble() < 0.002 * ms) {
			c.particles.add(new Particle(ParticleType.ofName("paralysis_spark"), gameX, gameY));
		}
		if (boardingShip != null && boardingShip.fearTime > 0 && AGame.ANIM_R.nextDouble() < 0.002 * ms) {
			c.particles.add(new Particle(ParticleType.ofName("fear_spark"), gameX, gameY));
		}
		
		// Fire and healing
		int currentHP = ship != null && ship.holdOnTime > 0 ? holdOnHp : hp;

		if (currentTile.module.fire > 0 && c.r.nextDouble() < currentTile.module.fire * type.fireHarmPMs * ms && currentHP > 0) {
			boolean wasAlive = alive();
			hp = StrictMath.max(0, hp - 1);
			if (wasAlive && !alive()) {
				c.incStat(currentTile.ship, "crewBurned");
			}
			if (type.coughSnd != null) {
				c.play(type.coughSnd, gameX, gameY, 0, 0, onViewingSide);
			}
		}
		if ((currentTile.module.type.necromancy(currentTile.module.ship.currentBonuses) || alive()) && currentHP < type.maxHP) {
			if (ship != null && ship.holdOnTime > 0) {
				holdOnHp = StrictMath.max(0, holdOnHp);
			} else {
				hp = StrictMath.max(0, hp);
			}
			int msForRepair = ms;
			if (type.isAircraft()) {
				msForRepair *= 1 + currentTile.module.ship.getShipwideModifier("planeRepairBonus");
			}
			repairMs += msForRepair;
			if ((currentTile.module.fullyStaffed() &&
				currentTile.module.type.getSickbay(currentTile.module.ship.currentBonuses) > 0 &&
				c.r.nextDouble() < currentTile.module.type.getSickbay(currentTile.module.ship.currentBonuses) * ms * type.sickbayHealPMs)
				||
				(type.msPerHPRepaired > 0 && repairMs >= type.msPerHPRepaired)
			) {
				repairMs = 0;
				ParticleType pt = ParticleType.ofName(hp == 0 ? "necromancy" : "healing");
				if (hp == 0) {
					c.incStat(currentTile.ship, "resurrections");
					currentTile.ship.incStat("resurrections");
				} else {
					c.incStat(currentTile.ship, "crewHPHealed");
					currentTile.ship.incStat("crewHPHealed");
				}
				if (ship != null && ship.holdOnTime > 0) {
					holdOnHp++;
				} else {
					hp++;
				}
				for (int i = 0; i < 5; i++) {
					c.particles.add(new Particle(pt, gameX, gameY));
				}
			}
		}
		if (alive() && type.ammoCapacity > 0 && ammo < type.ammoCapacity) {
			int msForRearm = ms;
			if (type.isAircraft()) {
				msForRearm *= 1 + currentTile.module.ship.getShipwideModifier("planeRearmBonus");
			}
			rearmAccumulator += msForRearm;
			if (rearmAccumulator >= type.rearmTime) {
				ammo = type.ammoCapacity;
				rearmAccumulator = 0;
			}
		}
		
		giveResourceWait -= ms;
		if (giveResourceWait > 0) { return false; }
		
		if (job != null) {
			//#SpikeProfiler.start("job");
			if (!ship.containsModule(job.module()) || !active() || !job.active() || (headingFor != null && !headingFor.alive())) {
				abandonJob("module " + ship.containsModule(job.module()) + " active " + active() + " jobactive " + job.active() + " headingForDead " + (headingFor != null && !headingFor.alive()));
				return false;
			}
			
			if (job.resource() != null || currentTile.module != job.module()) {
				msSinceMoved += ms;
			}
			
			// Pathing to resource source.
			if (job.resource() != null && job.resource() != carrying && job.resource() != Resource.INJURED) {
				//#SpikeProfiler.start("pathing");
				int closest = 0;
				Module best = null;
				int msz = ship.modules.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = ship.modules.get(mi);
					if (m.getResource(job.resource()) > 0) {
						if (!ship.hasPath(currentTile, m) && !canDoPathing) { continue; }
						ArrayList<Tile> path = ship.getPath(currentTile, m);
						if (path == null) { continue; }
						if (!ship.hasPath(path.isEmpty() ? currentTile : path.get(path.size() - 1), job.module()) && !canDoPathing) { continue; }
						ArrayList<Tile> modulePath = ship.getPath(path.isEmpty() ? currentTile : path.get(path.size() - 1), job.module());
						if (modulePath == null) { continue; }
						int d = path.size() + modulePath.size();
						if (best == null || d < closest) {
							best = m;
							closest = d;
						}
					}
				}
				target = best;
				if (target == null && !canDoPathing) { return false; }
				//#SpikeProfiler.end("pathing");
			} else {
				target = null;
			}
			
			// Pathing to job target.
			if (job.resource() == null || job.resource() == carrying) {
				target = job.module();
			}
			//#SpikeProfiler.end("job");
		}
		
		// Just plain no job? Properly abandon it.
		if (job == null) {
			abandonJob("no job");
		}
		
		// Unable to reach target? Abandon job!
		if ((target != null && !ship.containsModule(target)) || (headingFor != null && !ship.containsTile(headingFor.currentTile))) {
			abandonJob("target not in ship");
		}
		
		// Tile pathing.
		Tile targetTile = null;
		
		// Path to leave ship if boarding or attacking ships.
		if ((ultimateBoardTarget != null && ultimateBoardTarget != ship) || ((type.shootsShips || interceptTroops(c)) && !(type.isAircraft() && currentTile.ship.aircraftMode == AircraftBehaviourMode.STAY_PUT) && (hp == type.maxHP || type.msPerHPRepaired == 0) && (ammo == type.ammoCapacity))) {
			//#SpikeProfiler.start("exitpathing");
			if (currentTile.enterable() || !type.canWalk) {
				if (canPopOut()) {
					popOut(mySide, c, fleetCrewSpeedMult);
					return true;
				}
				return false; // Have to wait.
			}
			if (boardExitTarget != null && (!boardExitTarget.enterable() || !ship.containsTile(boardExitTarget))) {
				if (boardExitTarget == ship.boardExitTargetCache) {
					ship.boardExitTargetCache = null;
				}
				boardExitTarget = null;
			}
			if (boardExitTarget == null) {
				if (ship.boardExitTargetCache != null) {
					if (ship.boardExitTargetCache.enterable() && ship.containsTile(ship.boardExitTargetCache)) {
						boardExitTarget = ship.boardExitTargetCache;
					} else {
						ship.boardExitTargetCache = null;
					}
				}
			}
			if (boardExitTarget == null) {
				int tsz = ship.tiles.size();
				int closestDist = 0;
				for (int ti = 0; ti < tsz; ti++) {
					Tile t = ship.tiles.get(ti);
					if (t.enterable()) {
						if (!canDoPathing && !ship.hasPath(currentTile, t)) {
							continue;
						}
						ArrayList<Tile> path = ship.getPath(currentTile, t);
						if (path != null && (boardExitTarget == null || path.size() < closestDist)) {
							boardExitTarget = t;
							ship.boardExitTargetCache = t;
							closestDist = path.size();
						}
					}
				}
			}
			if (boardExitTarget != null) {
				if (canDoPathing || ship.hasPath(currentTile, boardExitTarget)) {
					ArrayList<Tile> path = ship.getPath(currentTile, boardExitTarget);
					if (path != null) {
						targetTile = path.get(0);
						msSinceMoved += ms;
					}
				}
			}
			//#SpikeProfiler.end("exitpathing");
		} else if (target != null && currentTile.module != target) {
			//#SpikeProfiler.start("pathing");
			if (!canDoPathing && !ship.hasPath(currentTile, target)) {
				return false;
			}
			ArrayList<Tile> path = ship.getPath(currentTile, target);
			if (path == null) {
				abandonJob("no path to target");
			} else if (path.isEmpty()) {
				String shipJSON = null;
				try {
					shipJSON = ship.toJSON(null).toString(4);
				} catch (Throwable t) {
					// Ignore
				}
				AirshipGame.instance.reportError("empty path: tile in " + currentTile.module.type.name + "#" + ship.modules.indexOf(currentTile.module) + " at " + currentTile.x + " " + currentTile.y + " pathing to " + target.type.name + "#" + ship.modules.indexOf(target) + " at " + target.x + " " + target.y, null, shipJSON, false, true);
				abandonJob("pathing to target broken");
			} else {
				targetTile = path.get(0);
			}
			//#SpikeProfiler.end("pathing");
		}
		
		// Path to crewman to rescue.
		if (headingFor != null && currentTile != headingFor.currentTile) {
			//#SpikeProfiler.start("rescuepathing");
			if (!canDoPathing && !ship.hasPath(currentTile, headingFor.currentTile)) {
				return false;
			}
			ArrayList<Tile> pathTo = ship.getPath(currentTile, headingFor.currentTile);
			if (pathTo == null) {
				abandonJob("no path to crewman");
			} else {
				targetTile = pathTo.get(0);
			}
			//#SpikeProfiler.end("rescuepathing");
		}
		
		//#SpikeProfiler.start("moving");
		if (weaponReload > 0) {
			ms *= type.reloadSlowdown;
			// Move slower while reloading. Both realistic and prevents blundering into enemies unready.
		}
		
		if (movingTowards != null && !ship.tiles.contains(movingTowards)) {
			movingTowards = null;
		}
		
		if (type.canWalk) {
			// May have to back out of previous movement target.
			if (targetTile != movingTowards && shootAccumulator == 0) {
				msSinceMoved -= 2 * ms;
				if (msSinceMoved <= 0) {
					msSinceMoved *= -1;
					movingTowards = targetTile;
				}
			}

			// Moving.
			if (movingTowards != null) {
				if (msSinceMoved >= currentTile.getMoveDelay() / speed(movingTowards, fleetCrewSpeedMult)) {
					msSinceMoved -= currentTile.getMoveDelay() / speed(movingTowards, fleetCrewSpeedMult);
					currentTile = movingTowards;
				}
			}
		}
		//#SpikeProfiler.end("moving");
		
		if (job != null) {
			//#SpikeProfiler.start("dojob");
			// Picking up resources.
			if (job.resource() != null && carrying != job.resource() && currentTile.module.getResource(job.resource()) > 0 && msSinceMoved >= pickupCost()) {
				//#SpikeProfiler.start("pickup");
				msSinceMoved -= pickupCost();
				currentTile.module.takeResource(job.resource(), c);
				int cap = ship.getTotalResourceCapacity(job.resource());
				int left = ship.getTotalResource(job.resource());
				if (left == 0) {
					doShout(pickShout("outOf" + job.resource().name()));
				} else if (left < 0.05 * cap) {
					doShout(pickShout("veryLow" + job.resource().name()));
				} else if (left < 0.2 * cap) {
					doShout(pickShout("low" + job.resource().name()));
				}
				dropCarried();
				carrying = job.resource();
				//#SpikeProfiler.end("pickup");
			}
			
			// Pick up injured.
			if (job.resource() == Resource.INJURED && headingFor != null && headingFor.currentTile == currentTile && msSinceMoved >= pickupCost()) {
				dropCarried();
				msSinceMoved -= pickupCost();
				carrying = Resource.INJURED;
				injuredCarried = headingFor;
				injuredCarried.abandonJob("picked up");
				headingFor = null;
			}
			
			// Drop off resource.
			if (job.resource() != null && job.resource() == carrying && currentTile.module == job.module()) {
				//#SpikeProfiler.start("dropoff");
				boolean repairing = carrying == Resource.REPAIR && currentTile.module.hp <= 0 && currentTile.module.type != ModuleType.ofName("CORRIDOR");
				currentTile.module.giveResource(carrying, c, onViewingSide, job instanceof Module.AmmoJob ? ((Module.AmmoJob) job).n - 1 : 0,
						fleetRepairAmountMult, fleetFirefightAmountMult);
				if (repairing && currentTile.module.hp > 0) {
					shout("repaired");
				}
				giveResourceWait = giveCost();
				if (carrying == Resource.REPAIR) {
					anim.animate(AnimationType.REPAIR, null);
				} else {
					anim.animate(anim.lastFlipped ? AnimationType.GIVE_LEFT : AnimationType.GIVE_RIGHT, injuredCarried == null ? carrying : Resource.INJURED);
				}
				if (carrying == Resource.WATER) {
					double mx = ship.getX() + ship.gridXToWorldX(currentTile.module.x, currentTile.module.type.getW()) * AGame.SGS + currentTile.module.type.getW() * AGame.SGS / 2;
					double my = ship.getY() + currentTile.module.y * AGame.SGS + currentTile.module.type.getH() * AGame.SGS / 2;
					for (int i = 0; i < 16; i++) {
						c.particles.add(new Particle(ParticleType.ofName("water"), mx, my));
					}
				}
				abandonJob("drop off injured crewman"); // This also drops the injured CM.
				//#SpikeProfiler.end("dropoff");
			}
			//#SpikeProfiler.end("dojob");
		}
		
		//#SpikeProfiler.start("sanity");
		sanityCheck(c);
		//#SpikeProfiler.end("sanity");
		return false;
	}

	private int giveCost() {
		if (carrying == Resource.REPAIR) {
			return type.repairTime;
		}
		return 500;
	}
	
	private int pickupCost() {
		int c = type.pickupMs;
		if (currentTile.module.fire > 0) { c *= type.pickupFireMult; }
		c *= 1 + ((currentTile.module.getMaxHP() - currentTile.module.hp) * type.pickupDmgMaxMalus / currentTile.module.getMaxHP());
		return c;
	}
	
	public void simpleDraw(Draw d, double tx, double ty, int ms, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, float[][] coaColors) {
		if (beingCarried()) { return; }
		float[] srcA = type.recolorOriginalA;
		float[] srcB = type.recolorOriginalB;
		float[] trgA = coaColors == null ? srcA : coaColors[type.recolorReplacementA.armsColorIndex];
		float[] trgB = coaColors == null ? srcB : coaColors[type.recolorReplacementB.armsColorIndex];
		Img img = type.simpleLook;
		if (type.simpleLookSpider != null && SimplePref.ARACHNOPHOBIA_MODE.get()) {
			img = type.simpleLookSpider;
		}
		if (ssb != null && !img.src.equals(ssb.name)) {
			if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, img.src)) {
				additionalSSBs.add(SpritesheetBundle.ofName(img.src));
			}
			return;
		}
		Airship sh = ship == null ? boardingShip : ship;
		Tile t2 = movingTowards;
		if (t2 != null) {
			if (sh.flipped) {
				tx -= (t2.x - currentTile.x) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, lastCrewSpeedMult) * AGame.SGS;
			} else {
				tx += (t2.x - currentTile.x) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2,  lastCrewSpeedMult) * AGame.SGS;
			}
			ty += (t2.y - currentTile.y) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2,  lastCrewSpeedMult) * AGame.SGS;
		}
		ty += AGame.SGS - img.srcHeight;
		tx += AGame.SGS / 2 - img.srcWidth / 2;
		RotatingColoringShader.draw(SpritesheetBundle.ofName(img.src), img, d, tx, ty, 0, 1, false, false, light, lightStrength, ambient, ambientSaturation, ambientTint, srcA, trgA, srcB, trgB);
	}
	
	public void draw(Draw d, double tx, double ty, int ms, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, float[][] coaColors, Crewman carrier) {
		Airship sh = ship == null ? boardingShip : ship;
		Crewman rel = carrier == null ? this : carrier;
		Tile t2 = rel.movingTowards;
		if (t2 != null) {
			if (sh.flipped) {
				tx -= (t2.x - rel.currentTile.x) * 1.0 * rel.msSinceMoved / rel.currentTile.getMoveDelay() * rel.speed(t2,  lastCrewSpeedMult) * AGame.SGS;
			} else {
				tx += (t2.x - rel.currentTile.x) * 1.0 * rel.msSinceMoved / rel.currentTile.getMoveDelay() * rel.speed(t2,  lastCrewSpeedMult) * AGame.SGS;
			}
			ty += (t2.y - rel.currentTile.y) * 1.0 * rel.msSinceMoved / rel.currentTile.getMoveDelay() * rel.speed(t2, lastCrewSpeedMult) * AGame.SGS;
		}
		anim.draw(d, tx + AGame.SGS / 2 - getBBWidth() / 2, ty + AGame.SGS - 1 - getBBHeight(), sh.flipped, light, lightStrength, ambient, ambientSaturation, ambientTint, ssb, additionalSSBs, coaColors, carrier != null);
	}
	
	// Outside behaviour
	public boolean isOutside() {
		return ship == null && boardingShip == null;
	}
	
	public boolean canPopOut() {
		return (currentTile.enterable() || !type.canWalk) && (
				(ship != null && ship.popOutCooldown <= 0) ||
				(boardingShip != null && boardingShip.popOutCooldown <= 0)
		);
	}
	
	public void popOut(Side mySide, Combat c, double fleetCrewSpeedMult) {
		Airship sh = ship == null ? boardingShip : ship;
		Tile t2 = movingTowards;
		setX(sh.getX() + sh.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2);
		setY(sh.getY() + currentTile.y * AGame.SGS + AGame.SGS - getBBHeight());
		if (t2 != null) {
			int moveDelay = currentTile.getMoveDelay();
			double speed = speed(t2, fleetCrewSpeedMult);
			double shift = (t2.x - currentTile.x) * 1.0 * msSinceMoved / moveDelay * speed * AGame.SGS;
			if (moveDelay == 0 || Double.isInfinite(speed) || Double.isNaN(speed) || Double.isInfinite(shift) || Double.isNaN(shift)) {
				AirshipGame.instance.reportError("Bad popOut: speed " + speed + ", moveDelay " + moveDelay + ", shift " + shift + ", type " + type.name + ", currentTileIndex" + sh.tiles.indexOf(currentTile) + ", currentTileMT " + currentTile.module.type.name + ", t2Index" + sh.tiles.indexOf(t2) + ", t2MT " + t2.module.type.name, null, ship.toJSON(null).toString(), false, true);
			} else {
				if (sh.flipped) {
					setX(getX() - shift);
				} else {
					setX(getX() + shift);
				}
				setY(getY() + (t2.y - currentTile.y) * 1.0 * msSinceMoved / currentTile.getMoveDelay() * speed(t2, fleetCrewSpeedMult) * AGame.SGS);
			}
		}
		int delay = type.popOutDelayMin;
		if (type.popOutDelayRange > 0) {
			delay += c.r.nextInt(type.popOutDelayRange);
		}
		if (type.isAircraft()) {
			delay /= StrictMath.max(0.1, 1 + sh.getShipwideModifier("planeLaunchSpeedBonus"));
		}
		sh.popOutCooldown = delay;
		sh.crew.remove(this);
		sh.boarders.remove(this);
		poppedOutOfTile = currentTile;
		//System.out.println("popOut tile " + poppedOutOfTile.module.type.name);
		currentTile = null;
		movingTowards = null;
		attachedTo = sh;
		ship = null;
		boardingShip = null;
		boarderTargetTile = null;
		meleeTargetTile = null;
		mySide.troops.add(this);
		msUntilRecalcJumpPoint = c.r.nextInt(RECALC_JUMP_POINT_WAIT);
		
		shout = null;
		attackTarget = null;
		attackTargetTile = null;
		bigShout = false;
		boardExitTarget = null;
		boarderTargetTile = null;
		boardingShip = null;
		carrying = null;
		dispersed = null;
		dx = 0;
		dy = 0;
		entryPoint = null;
		fallingTime = 0;
		giveResourceWait = 0;
		grabbed = false;
		headingFor = null;
		hookDist = 0;
		hookLaunched = false;
		hookProgress = 0;
		hookSource = null;
		hookTarget = null;
		hookTargetBody = null;
		hookedGR = null;
		initialShoutCooldown = 0;
		injuredCarried = null;
		interceptTarget = null;
		isSmart = false;
		jumpSourceGR = null;
		meleeTargetTile = null;
		movingTowards = null;
		msSinceMoved = 0;
		msUntilRecalcJumpPoint = 0;
		msUntilRecalcWalkPoint = 0;
		mvDx = 0;
		mvDy = 0;
		mvXOffset = 0;
		newThrowWait = 0;
		outsideBodyPath = null;
		proximateBoardTarget = null;
		rearmAccumulator = 0;
		repairMs = 0;
		shootAccumulator = 0;
		shootTarget = null;
		shout = null;
		shoutCooldown = 0;
		shoutH = 0;
		shoutMs = 0;
		shoutOriginX = 0;
		shoutOriginY = 0;
		shoutW = 0;
		shoutH = 0;
		shoutY = 0;
		standing = false;
		strafeTo = null;
		target = null;
		timeSinceLaunch = 0;
		trackingDx = 0;
		//ultimateBoardTarget = null;
		weaponReload = 0;
		walkToTargetGR = null;
		walkToGR = null;
		jumpSourceGR = null;
		entryPoint = null;
		winching = false;
		newThrowWait = 0;
		
		if (type.canFly) {
			attachedTo = null;
			mvDx = 0;
			mvDy = 0;
			trackingDx = 0;
			dx = (type.launchMinXSpeed + c.r.nextDouble() * (type.launchMaxXSpeed - type.launchMinXSpeed)) * (sh.flipped ? -1 : 1);
			trackingDx = dx;
			dy = type.launchMinYSpeed + c.r.nextDouble() * (type.launchMaxYSpeed - type.launchMinYSpeed);
			timeSinceLaunch = 0;
			if (type.launchSnd != null) {
				c.play(type.launchSnd, getX(), getY(), dx, dy, false/*qqDPS*/);
			}
		}
	}
	
	public boolean canPopIn(Airship target) {
		return popInTile(target) != null;
	}
		
	public Tile popInTile(Airship target) {
		if (!Rect2D.intersects(getX(), getY(), getBBWidth(), getBBHeight(), target.getX(), target.getY(), target.getBBWidth(), target.getBBHeight())) { return null; }
		for (int yOffset = 0; yOffset < 2; yOffset++) { for (int xOffset = 0; xOffset < 2; xOffset++) {
			double ox = getX() + getBBWidth() * xOffset - target.getX();
			double oy = getY() + getBBHeight() * yOffset - target.getY();
			int tgx;
			if (target.flipped) {
				tgx = (int) (target.getBBWidth() - ox) / AGame.SGS; // qqDPS +-1?
			} else {
				tgx = (int) (ox / AGame.SGS);
			}
			tgx = StrictMath.min(target.getWidth() - 1, StrictMath.max(0, tgx));
			int tgy = StrictMath.min(target.getHeight() - 1, StrictMath.max(0, (int) (oy / AGame.SGS)));
			Tile t = target.tileAt(tgx, tgy);
			if (type.canWalk) {
				if (t != null && t.canOccupy && t.enterable()) { return t; }
			} else {
				if (t != null && t == poppedOutOfTile) { return t; }
			}
		}}
		return null;
	}
	
	public boolean popIn(Airship target, boolean asBoarder, Combat c) {
		Tile t = popInTile(target);
		if (t == null) { return false; }
		if (asBoarder) {
			boardingShip = target;
			if (target.boarders.isEmpty()) {
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("boardingWith " + type.name, c.otherSide(c.sideOf(target)), getX(), getY(), type, boardingShip));
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("boardedBy " + type.name, c.sideOf(target), getX(), getY(), type, boardingShip));
			}
			target.boarders.add(this);
		} else {
			ship = target;
			target.crew.add(this);
		}
		currentTile = t;
		attachedTo = null;
		ignoring = null;
		ultimateBoardTarget = null;
		proximateBoardTarget = null;
		boarderTargetTile = null;
		meleeTargetTile = null;
		dx = 0;
		dy = 0;
		return true;
	}
	
	public boolean instantlyReturnHome(Combat c, Side side, boolean sideWon) {
		if (!sideWon && !type.canFly && !(side.ships.contains(attachedTo) || side.reserve.contains(attachedTo))) {
			//System.out.println("Lost nonflying non-attached-to-friendly nonwinning crew of type " + type.name);
			return false;
		}
		Tile homeTile = null;
		if (poppedOutOfTile != null && poppedOutOfTile.ship.tiles.contains(poppedOutOfTile) && exists(poppedOutOfTile.ship, c)) {
			homeTile = poppedOutOfTile;
		}
		if (spawnedAtTile != null && spawnedAtTile.ship.tiles.contains(spawnedAtTile) && exists(spawnedAtTile.ship, c)) {
			homeTile = spawnedAtTile;
		}
		if (homeTile != null) {
			//System.out.println("instantly return to home tile " + homeTile.module.type.name);
			homeTile.ship.crew.add(this);
			ship = homeTile.ship;
			currentTile = homeTile;
			attachedTo = null;
			ignoring = null;
			ultimateBoardTarget = null;
			proximateBoardTarget = null;
			dx = 0;
			dy = 0;
			softReset();
			return true;
		}
		return false;
	}
	
	public void drop() {
		ignoring = attachedTo;
		attachedTo = null;
		jumpSourceGR = null;
	}
	
	public void jump(double angle, double speed) {
		drop();
		dx += StrictMath.cos(angle) * speed;
		dy -= StrictMath.sin(angle) * speed;
		mvDx = 0;
		mvDy = 0;
	}
	
	public GridRef findJumpPoint(Side side, boolean jumpDownOnly) {
		if (!(attachedTo instanceof GridBody)) { return null; }
		GridBody att = (GridBody) attachedTo;
		if (att instanceof LandFormation && att.isImmobile()) { // No jumping from the ground.
			return null;
		}
		double[] angleAndStrengthRef = new double[2];
		if (jumpDownOnly) {
			if (att.fallPointCacheTarget == proximateBoardTarget && att.fallPointCache != null) {
				if (att.fallPointCache.solid()) {
					double tx = att.getX() + att.fallPointCache.gridX * AGame.SGS;
					double ty = att.getY() + att.fallPointCache.gridY * AGame.SGS - getBBHeight() + 0.5;
					findJumpAngleAndStrength(tx, ty, null, angleAndStrengthRef);
					double angle = angleAndStrengthRef[0];
					if (angle == JUMP_STRAIGHT_DOWN) {
						//System.out.println("fpc");
						return att.fallPointCache;
					}
				}
			}
		} else {
			if (att.jumpPointCacheTarget == proximateBoardTarget && att.jumpPointCache != null) {
				if (att.jumpPointCache.solid()) {
					double tx = att.getX() + att.jumpPointCache.gridX * AGame.SGS;
					double ty = att.getY() + att.jumpPointCache.gridY * AGame.SGS - getBBHeight() + 0.5;
					findJumpAngleAndStrength(tx, ty, null, angleAndStrengthRef);
					double angle = angleAndStrengthRef[0];
					if (!Double.isNaN(angle)) {
						//System.out.println("jpc");
						return att.jumpPointCache;
					}
				}
			}
		}
		GridRef best = null;
		double shortestDsq = 0;
		double lowestJumpStrengthAdjusted = 0;
		for (int tgy = 0; tgy < att.getGridHeight(); tgy++) {
			lp: for (int tgx = 0; tgx < att.getGridWidth(); tgx++) {
				if (!att.solidAt(tgx, tgy)) { continue; }
				double tx = att.getX() + tgx * AGame.SGS;
				double ty = att.getY() + tgy * AGame.SGS - getBBHeight() + 0.5;
				findJumpAngleAndStrength(tx, ty, null, angleAndStrengthRef);
				double angle = angleAndStrengthRef[0];
				double adjustedJumpStrength = StrictMath.max(angleAndStrengthRef[1] - type.jumpStrength / 4, 0);
				if (jumpDownOnly ? angle == JUMP_STRAIGHT_DOWN : !Double.isNaN(angle)) {
					double dsq = ((tx - getX()) * (tx - getX()) + (ty - getY()) * (ty - getY()));
					if (best == null || adjustedJumpStrength < lowestJumpStrengthAdjusted || (adjustedJumpStrength == lowestJumpStrengthAdjusted && dsq < shortestDsq)) {
						best = new GridRef(tgx, tgy, att);
						shortestDsq = dsq;
						lowestJumpStrengthAdjusted = adjustedJumpStrength;
					}
				}
			}
		}
		if (jumpDownOnly) {
			att.fallPointCache = best;
			att.fallPointCacheTarget = proximateBoardTarget;
		} else {
			att.jumpPointCache = best;
			att.jumpPointCacheTarget = proximateBoardTarget;
		}
		return best;
	}
	
	public void findJumpAngleAndStrength(double sourceX, double sourceY, GridRef[] tt, double[] angleAndStrengthRef) {
		angleAndStrengthRef[0] = Double.NaN;
		if (checkCanJumpStraightDown(sourceX, sourceY, tt)) {
			//System.out.println("checkCanJumpStraightDown true");
			angleAndStrengthRef[0] = JUMP_STRAIGHT_DOWN;
			angleAndStrengthRef[1] = 0;
			return;
		}
		double jumpStrength = 0.01;
		while (jumpStrength < type.jumpStrength) {
			jumpStrength = StrictMath.min(type.jumpStrength, jumpStrength * 2);
			angleAndStrengthRef[1] = jumpStrength;
			angleAndStrengthRef[0] = findJumpAngle(sourceX, sourceY, tt, jumpStrength);
			if (!Double.isNaN(angleAndStrengthRef[0])) {
				return;
			}
		}
	}
	
	public boolean checkCanJumpStraightDown(double sourceX, double sourceY, GridRef[] tt) {
		//System.out.println("pbt " + proximateBoardTarget);
		if (sourceY > proximateBoardTarget.getY() || sourceY < proximateBoardTarget.getY() - MAX_JUMP_DOWN) { return false; }
		int gx = (int) ((sourceX + getBBWidth() / 2 - proximateBoardTarget.getX()) / AGame.SGS);
		if (gx <= 0 || gx >= proximateBoardTarget.getGridWidth() - 1) {
			return false;
		}
		int gy = proximateBoardTarget.firstSolidBlockYAt(gx);
		//System.out.println("g " + gx + " " + gy);
		if (gy != -1 && tt != null) {
			tt[0] = new GridRef(gx, gy, proximateBoardTarget);
		}
		return gy != -1;
	}
	
	/* Returns NaN if no angle findable. */
	public double findJumpAngle(double sourceX, double sourceY, GridRef[] tt, double jumpStrength) {
		int bw = proximateBoardTarget.getGridWidth();
		int bh = proximateBoardTarget.getGridHeight();
		double bestAngle = Double.NaN;
		for (int borderIndex = 0; borderIndex < bw + bh; borderIndex++) {
			int tgx = borderIndex < bw ? borderIndex : (sourceX < proximateBoardTarget.getX() ? 0 : bw - 1);
			int tgy = borderIndex < bw ? (sourceY < proximateBoardTarget.getY() + proximateBoardTarget.getBBHeight() ? proximateBoardTarget.firstSolidBlockYAt(tgx) : bh - 1) : borderIndex - bw;
			if (proximateBoardTarget.solidAt(tgx, tgy)) {
				double angle = findJumpAngle(sourceX, sourceY, proximateBoardTarget.getX() + tgx * AGame.SGS + AGame.SGS / 2, proximateBoardTarget.getY() + tgy * AGame.SGS - getBBHeight() + 0.5, jumpStrength);
				if (!Double.isNaN(angle)) {
					if (tt != null) { tt[0] = new GridRef(tgx, tgy, proximateBoardTarget); }
					bestAngle = angle;
					if (angle == JUMP_STRAIGHT_DOWN) {
						return angle;
					}
				}
			}
		}
		return bestAngle;
	}
	
	public static double findJumpAngle(double sourceX, double sourceY, double targetX, double targetY, double v) {
		double y = sourceY - targetY; // Flip coords
		if (y < -MAX_JUMP_DOWN) { return Double.NaN; }
		double x = StrictMath.abs(targetX - sourceX);
		if (y <= 0 && x < AGame.SGS / 2) {
			return JUMP_STRAIGHT_DOWN;
		}
		double g = AGame.G;
		// Formula from https://en.wikipedia.org/wiki/Trajectory_of_a_projectile#Angle_required_to_hit_coordinate_.28x.2Cy.29
		double a = StrictMath.atan((v*v + StrictMath.sqrt(v*v*v*v - g*(g*x*x + 2*y*v*v))) / (g*x));
		double b = StrictMath.atan((v*v - StrictMath.sqrt(v*v*v*v - g*(g*x*x + 2*y*v*v))) / (g*x));
		double angle = Double.isNaN(a) ? b : a;
		if (Double.isNaN(angle)) {
			return angle;
		} else {
			return targetX < sourceX ? Direction.flipHorizontal(angle) : angle;
		}
	}
	
	public GridRef findHookPoint(Side side) {
		if (!(attachedTo instanceof GridBody)) { return null; }
		if (proximateBoardTarget.isImmobile()) { return null; } // Don't grapple the ground.
		GridBody att = (GridBody) attachedTo;
		GridRef best = null;
		double shortestDsq = 0;
		int minX = StrictMath.max(0, (int) StrictMath.floor((proximateBoardTarget.getX() - type.hookRopeLength - att.getX()) / AGame.SGS));
		int maxX = StrictMath.min(att.getGridWidth(), 1 + (int) StrictMath.ceil((proximateBoardTarget.getX() + proximateBoardTarget.getBBWidth() + type.hookRopeLength - att.getX()) / AGame.SGS));
		if (att instanceof LandFormation && ((LandFormation) att).immobile) {
			// Don't grapple buildings from the ground.
			if (proximateBoardTarget instanceof Airship && ((Airship) proximateBoardTarget).type == ShipType.BUILDING) { return null; }
			LandFormation ground = (LandFormation) att;
			// We're on the ground. Special case because we can't go underground.
			lp: for (int tgx = minX; tgx < maxX; tgx++) {
				int tgy = ground.heightMap[tgx];
				double tx = attachedTo.getX() + tgx * AGame.SGS;
				double ty = attachedTo.getY() + tgy * AGame.SGS - getBBHeight() + 0.5;
				if (checkHookFrom(tx, ty, null, null)) {
					int csz = side.troops.size();
					for (int ci = 0; ci < csz; ci++) {
						GridRef jsgr = side.troops.get(ci).jumpSourceGR;
						if (jsgr != null && jsgr.gridX == tgx && jsgr.gridY == tgy) { continue lp; }
					}
					double dsq = (tx - getX()) * (tx - getX()) + (ty - getY()) * (ty - getY());
					if (best == null || dsq < shortestDsq) {
						best = new GridRef(tgx, tgy, att);
						shortestDsq = dsq;
					}
				}
			}
			return best;
		} else {
			int minY = StrictMath.max(0, (int) StrictMath.floor((proximateBoardTarget.getY() - type.hookRopeLength - att.getY()) / AGame.SGS));
			int maxY = StrictMath.min(att.getGridHeight(), 1 + (int) StrictMath.ceil((proximateBoardTarget.getY() + proximateBoardTarget.getBBHeight() + type.hookRopeLength - att.getY()) / AGame.SGS));
			for (int tgy = minY; tgy < maxY; tgy++) {
				lp: for (int tgx = minX; tgx < maxX; tgx++) {
					if (!att.solidAt(tgx, tgy)) { continue; }
					double tx = attachedTo.getX() + tgx * AGame.SGS;
					double ty = attachedTo.getY() + tgy * AGame.SGS - getBBHeight() + 0.5;
					if (checkHookFrom(tx, ty, null, null)) {
						int csz = side.troops.size();
						for (int ci = 0; ci < csz; ci++) {
							GridRef jsgr = side.troops.get(ci).jumpSourceGR;
							if (jsgr != null && jsgr.gridX == tgx && jsgr.gridY == tgy) { continue lp; }
						}
						double dsq = (tx - getX()) * (tx - getX()) + (ty - getY()) * (ty - getY());
						if (best == null || dsq < shortestDsq) {
							best = new GridRef(tgx, tgy, att);
							shortestDsq = dsq;
						}
					}
				}
			}
			return best;
		}
	}
		
	public boolean checkHookFrom(double sourceX, double sourceY, Side findHookTargetForSide, Pt[] out_BestPt) {
		if (proximateBoardTarget.isImmobile()) { return false; } // Don't hook to the ground.
		
		int bw = proximateBoardTarget.getGridWidth();
		int bh = proximateBoardTarget.getGridHeight();
		
		if (proximateBoardTarget.topHookBoundaries == null) {
			proximateBoardTarget.topHookBoundaries = new int[bw];
			proximateBoardTarget.bottomHookBoundaries = new int[bw];
			proximateBoardTarget.leftHookBoundaries = new int[bh];
			proximateBoardTarget.rightHookBoundaries = new int[bh];
		}
		
		boolean foundBestPt = false;
		boolean bestEnterable = false;
		double bestPtX = 0, bestPtY = 0;
		double bestDsq = 0;

		lp: for (int borderIndex = 0; borderIndex < bw + bh; borderIndex++) {
			int tgx;
			int tgy;
			if (borderIndex < bw) {
				// Looking vertically for a place with a specific x-position. Move inwards until we get a fully solid place to grapple to.
				tgx = borderIndex;
				if (sourceY < proximateBoardTarget.getY() + proximateBoardTarget.getBBHeight()) {
					// We're above the target.
					if (proximateBoardTarget.topHookBoundaries[tgx] != 0) {
						if (proximateBoardTarget.topHookBoundaries[tgx] == -1) {
							continue;
						} else {
							tgy = proximateBoardTarget.topHookBoundaries[tgx] - 1;
						}
					} else {
						tgy = 0;
						proximateBoardTarget.topHookBoundaries[tgx] = -1;
						while (!proximateBoardTarget.fullAt(tgx, tgy)) {
							tgy++;
							if (tgy >= bh) { continue lp; }
						}
						proximateBoardTarget.topHookBoundaries[tgx] = tgy + 1;
					}
				} else {
					// We're below the target.
					if (proximateBoardTarget.bottomHookBoundaries[tgx] != 0) {
						if (proximateBoardTarget.bottomHookBoundaries[tgx] == -1) {
							continue;
						} else {
							tgy = proximateBoardTarget.bottomHookBoundaries[tgx] - 1;
						}
					} else {
						tgy = bh - 1;
						proximateBoardTarget.bottomHookBoundaries[tgx] = -1;
						while (!proximateBoardTarget.fullAt(tgx, tgy)) {
							tgy--;
							if (tgy < 0) { continue lp; }
						}
						proximateBoardTarget.bottomHookBoundaries[tgx] = tgy + 1;
					}
				}
			} else {
				// Looking horizontally for a place with a specific y-position. Move inwards until we get a fully solid place to grapple to.
				tgy = borderIndex - bw;
				if (sourceX < proximateBoardTarget.getX()) {
					int[] boundaries = (proximateBoardTarget instanceof Airship && ((Airship) proximateBoardTarget).flipped)
							? proximateBoardTarget.rightHookBoundaries
							: proximateBoardTarget.leftHookBoundaries;
	
					if (boundaries[tgy] != 0) {
						if (boundaries[tgy] == -1) {
							continue;
						} else {
							tgx = boundaries[tgy] - 1;
						}
					} else {
						tgx = 0;
						boundaries[tgy] = -1;
						while (!proximateBoardTarget.fullAt(tgx, tgy)) {
							tgx++;
							if (tgx >= bw) { continue lp; }
						}
						boundaries[tgy] = tgx + 1;
					}
				} else {
					int[] boundaries = (proximateBoardTarget instanceof Airship && ((Airship) proximateBoardTarget).flipped)
							? proximateBoardTarget.leftHookBoundaries
							: proximateBoardTarget.rightHookBoundaries;
					if (boundaries[tgy] != 0) {
						if (boundaries[tgy] == -1) {
							continue;
						} else {
							tgx = boundaries[tgy] - 1;
						}
					} else {
						tgx = bw - 1;
						boundaries[tgy] = -1;
						while (!proximateBoardTarget.fullAt(tgx, tgy)) {
							tgx--;
							if (tgx < 0) { continue lp; }
						}
						boundaries[tgy] = tgx + 1;
					}
				}
			}
			if (proximateBoardTarget.fullAt(tgx, tgy)) {
				GridLocation gl = proximateBoardTarget.locationAt(tgx, tgy);
				double tx = proximateBoardTarget.getX() + tgx * AGame.SGS;
				double ty = proximateBoardTarget.getY() + tgy * AGame.SGS;
				double dsq = (sourceX - tx) * (sourceX - tx) + (sourceY - ty) * (sourceY - ty);
				if (dsq < type.hookRopeLength * type.hookRopeLength) {
					if (findHookTargetForSide != null) {
						boolean enterable = proximateBoardTarget.enterableAt(tgx, tgy);
						if (!foundBestPt || dsq < bestDsq || (enterable && !bestEnterable)) {
							int csz = findHookTargetForSide.troops.size();
							for (int ci = 0; ci < csz; ci++) {
								GridRef jsgr = findHookTargetForSide.troops.get(ci).jumpSourceGR;
								if (jsgr != null && jsgr.gridX == tgx && jsgr.gridY == tgy) { continue lp; }
							}

							if (gl instanceof Tile && !((Tile) gl).full()) {
								Tile t = (Tile) gl;
								bestPtX = tx + t.solidCenterX();
								bestPtY = ty + t.solidCenterY();
							} else {
								bestPtX = tx + AGame.SGS / 2;
								bestPtY = ty + AGame.SGS / 2;
							}
							foundBestPt = true;
							bestEnterable = enterable;
							bestDsq = dsq;
						}
					} else {
						return true;
					}
				}
			}
		}
		if (findHookTargetForSide != null) {
			// Compensate for velocity
			if (!foundBestPt) { return false; }
			double dist = StrictMath.sqrt((sourceX - bestPtX) * (sourceX - bestPtX) + (sourceY - bestPtY) * (sourceY - bestPtY));
			double travelTime = dist / type.hookSpeed;
			out_BestPt[0] = new Pt(bestPtX + proximateBoardTarget.getxSpeed() * travelTime * 0.95, bestPtY);
			return true;
		} else {
			return false;
		}
	}
	
	public GridRef findEntryPoint(boolean hasWater) {
		GridRef best = null;
		double bestDsq = 0;
		for (int tgy = 0; tgy < ultimateBoardTarget.getGridHeight(); tgy++) { for (int tgx = 0; tgx < ultimateBoardTarget.getGridWidth(); tgx++) {
			if (ultimateBoardTarget.enterableAt(tgx, tgy)) {
				double tx = ultimateBoardTarget.getX() + tgx * AGame.SGS;
				double ty = ultimateBoardTarget.getY() + tgy * AGame.SGS - getBBHeight() + 0.5;
				if (hasWater && ty >= AGame.GROUND_LEVEL) {
					continue;
				}
				double dsq = (getX() - tx) * (getX() - tx) + (getY() - ty) * (getY() - ty);
				if (best == null || dsq < bestDsq) {
					best = new GridRef(tgx, tgy, ultimateBoardTarget);
					bestDsq = dsq;
				}
			}
		}}
		
		return best;
	}
	
	private GridRef findWalkTo() {
		// Search from where we are. If at any point we encounter a body of water, we stop searching.
		if (!(attachedTo instanceof GridBody)) { return null; }
		GridBody att = (GridBody) attachedTo;
		int attW = att.getGridWidth();
		int currentGridX = (int) (StrictMath.floor((getX() - att.getX()) / AGame.SGS));
		boolean hasWater = att instanceof LandFormation && ((LandFormation) att).immobile && ((LandFormation) att).landscapeType.hasWater;
		for (int xOffset = 0; currentGridX + xOffset < attW; xOffset++) {
			int gx = currentGridX + xOffset;
			int gy = att.firstSolidBlockYAt(gx);
			if (hasWater && att.getY() + gy * AGame.SGS >= AGame.GROUND_LEVEL) {
				break;
			}
			if (canBoardFromGridPos(gx, gy)) {
				return new GridRef(gx, gy, att);
			}
		}
		for (int xOffset = 0; currentGridX - xOffset >= 0; xOffset++) {
			int gx = currentGridX - xOffset;
			int gy = att.firstSolidBlockYAt(gx);
			if (hasWater && att.getY() + gy * AGame.SGS >= AGame.GROUND_LEVEL) {
				break;
			}
			if (canBoardFromGridPos(gx, gy)) {
				return new GridRef(gx, gy, att);
			}
		}
		return null;
	}
	
	private boolean walkToIsValidForBoarding() {
		return walkToTargetGR != null && canBoardFromGridPos(walkToTargetGR.gridX, walkToTargetGR.gridY);
	}
	
	private boolean canBoardFromGridPos(int gx, int gy) {
		if (proximateBoardTarget == null) { return false; }
		// Probe with the center of our location.
		double crewWorldX = attachedTo.getX() + gx * AGame.SGS + getBBWidth() / 2;
		double crewWorldY = attachedTo.getY() + gy * AGame.SGS - getBBHeight() / 2 + 0.5;
		return canSwitchToBoardTarget(crewWorldX, crewWorldY);
	}
	
	private GridRef disperse(Combat c, GridBody att) {
		int currentGridX = (int) (StrictMath.floor((getX() - att.getX()) / AGame.SGS));
		int newGridX = currentGridX + c.r.nextInt(7) - 4;
		int newGridY = att.firstSolidBlockYAt(newGridX);
		if (att.solidAt(newGridX, newGridY)) {
			return new GridRef(newGridX, newGridY, att);
		}
		return null;
	}
	
	private boolean canSwitchToBoardTarget(double wx, double wy) {
		if (attachedTo == proximateBoardTarget) { return false; }
		int btGx = (int) StrictMath.floor((wx - proximateBoardTarget.getX()) / AGame.SGS);
		int btGy = (int) StrictMath.floor((wy - proximateBoardTarget.getY()) / AGame.SGS);
		return proximateBoardTarget.solidAt(btGx, btGy);
	}
	
	private void switchToBoardTarget() {
		ignoring = attachedTo;
		attachedTo = proximateBoardTarget;
		walkToTargetGR = null;
	}

	public boolean exists(Crewman cm, Combat c) {
		int ssz = c.sides.size();
		for (int si = 0; si < ssz; si++) {
			if (c.sides.get(si).troops.contains(cm)) { return true; }
		}
		return false;
	}
	
	public boolean exists(Body b, Combat c) {
		return b != null && (c.physics == null || c.physics.bodies.contains(b));
	}
	
	public boolean solidAndExists(GridRef gr, Combat c) {
		return gr != null && gr.solid() && exists(gr.body, c);
	}
	
	public boolean isSmart = false;
	
	private boolean checkGunAngle(double tx, double ty) {
		if (type.bombs) { return true; }
		double srcX = getX();
		double srcY = getY() + type.barrelY;
		if (tx > srcX) {
			srcX += type.barrelX;
		} else {
			srcX += getBBWidth() - type.barrelX;
		}
		if (tx > srcX != dx + mvDx > 0) { return false; }
		return StrictMath.abs(srcX - tx) > StrictMath.abs(srcY - ty);
	}
	
	public void outsideShootingTick(int ms, Combat c, Side side, boolean onViewingSide) {
		LandFormation gnd = c.landFormations.get(0);
		weaponReload -= ms;
		if (	attackTarget == null ||
				c.sideOf(attackTarget) == null ||
				c.sideOf(attackTarget) == side ||
				!exists(attackTarget, c) ||
				attackTarget.dangerCache <= 0 ||
				(gnd.landscapeType.hasWater && attackTarget.getY() >= AGame.GROUND_LEVEL)
		) {
			attackTarget = null;
		}
		if (interceptTarget == null || !exists(interceptTarget, c) || (gnd.landscapeType.hasWater && interceptTarget.getY() >= AGame.GROUND_LEVEL)) {
			interceptTarget = null;
		}
		if (!active() || weaponReload > 0) { return; }
		if (	type.shootsShips &&
				poppedOutOfTile != null &&
				poppedOutOfTile.ship.containsTile(poppedOutOfTile) &&
				exists(poppedOutOfTile.ship, c) &&
				poppedOutOfTile.ship.fireAt != null &&
				exists(poppedOutOfTile.ship.fireAt, c) &&
				c.sideOf(poppedOutOfTile.ship.fireAt) != side &&
				poppedOutOfTile.ship.fireAt.dangerCache > 0 &&
				!(gnd.landscapeType.hasWater && poppedOutOfTile.ship.fireAt.getY() >= AGame.GROUND_LEVEL)
		) {
			attackTarget = poppedOutOfTile.ship.fireAt;
		}
		if (attackTarget == null && type.shootsShips) {
			Side otherS = c.otherSide(side);
			if (ultimateBoardTarget instanceof Airship) {
				attackTarget = (Airship) ultimateBoardTarget;
			} else {
				double minDist = 0;
				int esz = otherS.ships.size();
				for (int ei = 0; ei < esz; ei++) {
					Airship e = otherS.ships.get(ei);
					if (e.dangerCache <= 0 || (gnd.landscapeType.hasWater && e.getY() >= AGame.GROUND_LEVEL)) { continue; }
					double dSq =
							(e.getX() + e.getBBWidth() / 2 - getX()) * (e.getX() + e.getBBWidth() / 2 - getX()) +
							(e.getY() + e.getBBHeight() / 2 - getY()) * (e.getY() + e.getBBHeight() / 2 - getY());
					if (attackTarget == null || dSq < minDist) {
						attackTarget = e;
						minDist = dSq;
					}
				}
			}
		}
		
		if (type.shootTroopsRange > 0 && !type.bombs) {
			Crewman troopTarget = null;
			Crewman myInterceptTarget = null;
			double minDistSq = type.shootTroopsRange * type.shootTroopsRange;
			double minInterceptDistSq = 0;
			ArrayList<Crewman> enemyTroops = c.otherSide(side).troops;
			int tsz = enemyTroops.size();
			for (int ti = 0; ti < tsz; ti++) {
				Crewman t = enemyTroops.get(ti);
				if (dx + mvDx > 0 != t.getX() > getX() && (interceptTarget != null || !interceptTroops(c))) { continue; } // We want to at least face towards them.
				if (gnd.landscapeType.hasWater && t.getY() >= AGame.GROUND_LEVEL) { continue; }
				double dSq = (getX() - t.getX()) * (getX() - t.getX()) + (getY() - t.getY()) * (getY() - t.getY());
				if (myInterceptTarget == null || dSq < minInterceptDistSq) {
					myInterceptTarget = t;
					minInterceptDistSq = dSq;
				}
				if (dx + mvDx > 0 == t.getX() > getX() && dSq < minDistSq) {
					troopTarget = t;
					minDistSq = dSq;
				}
			}
			if (interceptTroops(c)) {
				interceptTarget = myInterceptTarget;
			}
			if (troopTarget != null) {
				double srcX = getX();
				double srcY = getY() + type.barrelY;
				double targetX = troopTarget.getX() + troopTarget.getBBWidth() / 2;
				double targetY = troopTarget.getY() + troopTarget.getBBHeight() / 2;
				
				if (checkGunAngle(targetX, targetY)) {
					if (targetX > srcX) {
						srcX += type.barrelX;
					} else {
						srcX += getBBWidth() - type.barrelX;
					}

					double dist = StrictMath.sqrt((srcX - targetX) * (srcX - targetX) + (srcY - targetY) * (srcY - targetY)) + 1;
					double travelTime = dist / type.shotSpeed;

					if (!side.outsideCrewWeaponFiredVsCrew.contains(type)) {
						side.outsideCrewWeaponFiredVsCrew.add(type);
						c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("outsideCrewWeaponFired " + type.name + " " + troopTarget.type.name, side, getX(), getY(), troopTarget.type, null));
						c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("attackedByOutsideCrew " + troopTarget.type.name + " " + type.name, c.otherSide(side), getX(), getY(), type, null));
					}

					if (type.ammoCapacity > 0) {
						ammo--;
					}
					for (int i = 0; i < type.numShots; i++) {
						double tx = targetX + troopTarget.dx * travelTime + dist * type.inaccuracy * c.getInaccuracyMultiplier() * AGame.SGS * RandUtils.nextGaussian(c.r);
						double ty = targetY + troopTarget.dy * travelTime + dist * type.inaccuracy * c.getInaccuracyMultiplier() * AGame.SGS * RandUtils.nextGaussian(c.r);
						Shot shot = new Shot(
								troopTarget,
								tx,
								ty,
								this.type,
								srcX,
								srcY);
						c.shots.add(shot);
						if (type.shotSpeed >= 0.5 && type.shot != null) { // qqDPS
							c.trails.add(new Trail(shot, type.shot.srcHeight * 0.5 + 1));
						}
					}
					c.msSinceInterestingCombatEvent = 0;
					c.incStat(this, "crewShotsFired " + type.name);
					weaponReload = weaponReload(c);
					if (type.attackParticle != null) {
						c.particles.add(new Particle(type.attackParticle, srcX, srcY));
					}
					if (type.attackSnd != null) {
						c.play(type.attackSnd, srcX, srcY, 0, 0, onViewingSide);
					}
					if (type.dieOnEmptyAmmo && ammo <= 0) {
						hurt(null, hp, c, onViewingSide);
					}
					return;
				}
			}
		}
		
		if (attackTarget != null) {
			double srcX = getX();
			if (dx > 0 || mvDx > 0) {
				srcX += type.barrelX;
			} else {
				srcX += getBBWidth() - type.barrelX;
			}
			double srcY = getY() + type.barrelY;
			double targetX = attackTarget.getX() + attackTarget.getBBWidth() / 2;
			double targetY = attackTarget.getY() + attackTarget.getBBHeight() / 2;
			boolean targetFound = false;

			if (type.bombs) {
				targetX = getX();
				targetY = StrictMath.min(attackTarget.getY() + attackTarget.getBBHeight() - AGame.SGS / 2, attackTarget.yBoundaryAt(targetX) + AGame.SGS * 2);
				targetFound = srcX > attackTarget.getX() + attackTarget.getBBWidth() * 0.2 && srcX < attackTarget.getX() + attackTarget.getBBWidth() * 0.8 && targetY > srcY;
			} else {
				if (type.aimForCenter) {
					boolean foundGoodTargetTooFarAway = false;
					lp: for (int targetGridSize = 5; targetGridSize > 0; targetGridSize--) {
						int anchorX = attackTarget.getWidth() / 2;
						int anchorY = attackTarget.getHeight() / 2;
						int loopWidth = 0;
						int loopHeight = 0;
						while (true) {
							if (foundGoodTargetTooFarAway && (loopWidth * 3 > attackTarget.getWidth() || loopHeight * 3 > attackTarget.getHeight())) {
								// This is to stop the plane from just shooting the first thing it gets into range of.
								// If there's a juicier target further in, it stops.
								break lp;
							}
							int gx = anchorX, gy = anchorY;
							for (int i = 0; i < loopWidth * 2 + loopHeight * 2 + 1; i++) {
								//System.out.println(gx + " " + gy + " @ " + targetGridSize);
								boolean solid = true;
								targetY = attackTarget.getY() + (gy + targetGridSize / 2) * AGame.SGS;
								if (!(gnd.landscapeType.hasWater && targetY >= AGame.GROUND_LEVEL)) {
									grid: for (int yOff = 0; yOff < targetGridSize; yOff++) {
										for (int xOff = 0; xOff < targetGridSize; xOff++) {
											Tile t = attackTarget.tileAt(gx + xOff - targetGridSize / 2, gy + yOff - targetGridSize / 2);
											/*if (t != null && dx == targetGridSize / 2 && dy == targetGridSize / 2) {
												t.armour.paint = PaintType.values()[anchorX % PaintType.values().length];
											}*/
											if (t == null || !t.solid()) {
												solid = false;
												break grid;
											}
										}
									}
									if (solid) {
										targetX = attackTarget.getX() + attackTarget.gridXToWorldX(gx + targetGridSize / 2, 1) * AGame.SGS;
										double d = Math.sqrt((srcX - targetX) * (srcX - targetX) + (srcY - targetY) * (srcY - targetY));
										double travelTime = d / type.shotSpeed;
										targetX += attackTarget.getxSpeed() * travelTime * 0.75;
										targetY += attackTarget.getySpeed() * travelTime * 0.3;
										targetFound = checkGunAngle(targetX, targetY) && targetY > srcY && Math.abs(targetX - srcX) <= type.maxRange;
										if (targetFound) {
											//System.out.println("found " + (targetX - attackTarget.getX()) + " " + (targetY - attackTarget.getY()));
											//System.out.println("t @ " + gx + " " + gy);
											break lp;
										} else {
											foundGoodTargetTooFarAway = true;
											//System.out.println("fgttfa");
										}
									}
								}
								if (i < loopWidth) {
									gx++;
								} else if (i < loopWidth + loopHeight) {
									gy++;
								} else if (i < loopWidth * 2 + loopHeight) {
									gx--;
								} else {
									gy--;
								}
							}
							// make a loop with top left anchorX/anchorY and loopWidth/loopHeight
								// check if solid
							// move anchorX/anchorY by -1/-1 and loopWidth/loopHeight by 2/2
							// exit if anchor x/y and loops have all touched the sides
							boolean anchorXBumped = false, anchorYBumped = false, loopWidthBumped = false, loopHeightBumped = false;
							if (anchorX <= 0) {
								anchorXBumped = true;
								//System.out.println("axb");
							} else {
								anchorX--;
							}
							if (anchorY <= 0) {
								anchorYBumped = true;
								//System.out.println("ayb");
							} else {
								anchorY--;
							}
							if (anchorX + loopWidth + 2 + targetGridSize > attackTarget.getWidth()) {
								loopWidthBumped = true;
								//System.out.println("lwb");
							} else {
								loopWidth += 2;
							}
							if (anchorY + loopHeight + 2 + targetGridSize > attackTarget.getHeight()) {
								loopHeightBumped = true;
								//System.out.println("lhb");
							} else {
								loopHeight += 2;
							}
							if (anchorXBumped && anchorYBumped && loopWidthBumped && loopHeightBumped) {
								continue lp;
							}
						}
					}
				} else {
					if (type.canFly) {
						// Strafing.
						if (srcY > attackTarget.getY() && srcY < attackTarget.getY() + attackTarget.getBBHeight()) {
							targetY = srcY;
						} else if (srcY < attackTarget.getY()) {
							targetY = attackTarget.getY() + attackTarget.getBBHeight() / 4;
						} else if (srcY > attackTarget.getY() + attackTarget.getBBHeight()) {
							targetY = attackTarget.getY() + attackTarget.getBBHeight() * 3 / 4;
						}
						
						if (gnd.landscapeType.hasWater) {
							targetY = StrictMath.min(targetY, AGame.GROUND_LEVEL - 1);
						}

						int direction = dx < 0 ? -1 : 1;
						int gy = (int) ((targetY - attackTarget.getY()) / AGame.SGS);

						for (double strafeDist = type.minRange; strafeDist < type.maxRange; strafeDist += AGame.SGS) {
							targetX = srcX + strafeDist * direction;
							int gx = (int) ((targetX - attackTarget.getX()) / AGame.SGS);
							if (checkGunAngle(targetX, targetY) && attackTarget.solidAt(gx, gy)) {
								targetFound = true;
								break;
							}
						}
					} else {
						if (attackTargetTile != null && !attackTarget.tiles.contains(attackTargetTile)) {
							attackTargetTile = null;
						}
						if (attackTargetTile != null) {
							int tX = attackTarget.getIntX() + attackTarget.gridXToWorldX(attackTargetTile.x, 1) * AGame.SGS + AGame.SGS / 2;
							int tY = attackTarget.getIntY() + attackTargetTile.y * AGame.SGS + AGame.SGS / 2;
							if (	(tX - srcX) * (tX - srcX) + (tY - srcY) * (tY - srcY) > type.maxRange * type.maxRange ||
									(gnd.landscapeType.hasWater && tY >= AGame.GROUND_LEVEL)
							) {
								attackTargetTile = null;
							} else {
								targetX = tX;
								targetY = tY;
								targetFound = true;
							}
						}
						if (attackTargetTile == null) {
							int bestQuality = -1;
							for (int ti = 0; ti < attackTarget.tiles.size(); ti++) {
								Tile t = attackTarget.tiles.get(ti);
								int tX = attackTarget.getIntX() + attackTarget.gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2;
								int tY = attackTarget.getIntY() + t.y * AGame.SGS + AGame.SGS / 2;
								if (gnd.landscapeType.hasWater && tY >= AGame.GROUND_LEVEL) { continue; }
								if ((tX - srcX) * (tX - srcX) + (tY - srcY) * (tY - srcY) <= type.maxRange * type.maxRange) {
									int q = quality(t);
									if (q > bestQuality) {
										bestQuality = q;
										attackTargetTile = t;
										targetX = tX;
										targetY = tY;
										targetFound = true;
									}
								}
							}
						}
					}
				}
			}
			
			if (!targetFound) { return; }
			
			if (!side.outsideCrewWeaponFiredVsShip.contains(type)) {
				side.outsideCrewWeaponFiredVsShip.add(type);
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("outsideCrewWeaponFiredVsShip " + type.name, side, getX(), getY(), null, null));
			}
						
			double dist = StrictMath.sqrt((targetX - srcX) * (targetX - srcX) + (targetY - srcY) * (targetY - srcY));
			if (type.ammoCapacity > 0) {
				ammo--;
			}
			
			//System.out.println("shooting at " + targetX + "/" + targetY + " vs " + attackTarget.getX() + "/" + attackTarget.getY() + " to " + (attackTarget.getX() + attackTarget.getBBWidth()) + "/" + (attackTarget.getY() + attackTarget.getBBHeight()));
			if (!type.aimForCenter) {
				double travelTime = dist / type.shotSpeed;
				targetX += attackTarget.getxSpeed() * travelTime;
			}
			//System.out.println("adjusted at " + targetX + "/" + targetY + " vs " + attackTarget.getX() + "/" + attackTarget.getY() + " to " + (attackTarget.getX() + attackTarget.getBBWidth()) + "/" + (attackTarget.getY() + attackTarget.getBBHeight()));
			for (int i = 0; i < type.numShots; i++) {
				Shot shot = new Shot(
						attackTarget,
						targetX + (dist * type.inaccuracy * AGame.SGS + type.baseInaccuracy) * RandUtils.nextGaussian(c.r),
						targetY + (dist * type.inaccuracy * AGame.SGS + type.baseInaccuracy) * RandUtils.nextGaussian(c.r),
						this.type,
						srcX,
						srcY,
						/* internal */ false,
						/* vsBoarders */ false /* ignored anyway */);
				c.shots.add(shot);
				if (type.shotSpeed >= 0.5 && type.shot != null) { // qqDPS
					c.trails.add(new Trail(shot, type.shot.srcHeight * 0.5 + 1));
				}
			}
			//System.out.println("shot offset " + (shot.tX - targetX) + ", " + (shot.tY - targetY) + " dist " + dist + " xdist " + Math.abs(attackTarget.getX() + attackTarget.getBBWidth() / 2 - getX()));
			c.msSinceInterestingCombatEvent = 0;
			c.incStat(this, "crewShotsFired " + type.name);
			
			weaponReload = weaponReload(c);
			if (type.attackParticle != null) {
				c.particles.add(new Particle(type.attackParticle, srcX, srcY));
			}
			if (type.attackSnd != null) {
				c.play(type.attackSnd, srcX, srcY, 0, 0, onViewingSide);
			}
			if (type.dieOnEmptyAmmo && ammo <= 0) {
				hurt(null, hp, c, onViewingSide);
			}
		}
	}
	
	public int quality(Tile t) {
		int q = 1000 / (StrictMath.max(0, t.armour.hp) + 20);
		int afterArmorDamage =
				t.armour.hp > 0
				? StrictMath.max(1, type.penDmg - t.armour.type.penDmgAbsorb.get(t.ship.currentBonuses)) + StrictMath.max(1, type.blastDmg - t.armour.type.blastDmgAbsorb.get(t.ship.currentBonuses)) + type.directDmg
				: type.penDmg + type.blastDmg + type.directDmg;
		q *= afterArmorDamage;
		q *= t.module.type.getTargetAttractivenessMult(t.ship.currentBonuses);
		if (t.module.type.getCoalReload(t.ship.currentBonuses) > 0 && t.module.running()) {
			q *= 10;
		} else if (ModuleType.interesting.contains(t.module.type)) {
			q *= 10;
		}
		if (t.module.type.isExternal()) {
			q /= 4;
		}
		if (!t.full()) {
			q /= 4;
		}
		if (t.module.fire > 0) {
			q /= 8;
		}
		if (t.module.hp <= 0) {
			q /= 20;
		}
		return q + 1;
	}
		
	public boolean outsideFlyingTick(int ms, Combat c, Side side, boolean onViewingSide) {
		LandFormation gnd = c.landFormations.get(0);
		if (ultimateBoardTarget != null && (!exists(ultimateBoardTarget, c) || (gnd.landscapeType.hasWater && ultimateBoardTarget.getY() >= AGame.GROUND_LEVEL))) {
			ultimateBoardTarget = null;
			Side enemySide = c.otherSide(side);
			double closestDsq = 0;
			for (int i = 0; i < enemySide.ships.size(); i++) {
				Airship enemy = enemySide.ships.get(i);
				if (enemy.canBeBoarded() && !(gnd.landscapeType.hasWater && enemy.getY() >= AGame.GROUND_LEVEL)) {
					double dsq = (enemy.getX() - getX()) * (enemy.getX() - getX()) + (enemy.getY() - getY()) * (enemy.getY() - getY());
					if (ultimateBoardTarget == null || dsq < closestDsq) {
						ultimateBoardTarget = enemy;
						closestDsq = dsq;
					}
				}
			}
		}
		if (!exists(attackTarget, c) || (attackTarget != null && gnd.landscapeType.hasWater && attackTarget.getY() >= AGame.GROUND_LEVEL)) {
			attackTarget = null;
		}
		if (gnd.landscapeType.hasWater && interceptTarget != null && interceptTarget.getY() >= AGame.GROUND_LEVEL) {
			interceptTarget = null;
		}
		timeSinceLaunch += ms;
		if (active()) {
			if (timeSinceLaunch >= type.launchLength && (type.shootsShips || interceptTroops(c)) && (type.ammoCapacity == 0 || ammo > 0)) {
				outsideShootingTick(ms, c, side, onViewingSide);
			}
			if (!solidAndExists(entryPoint, c) || !entryPoint.enterable() || (gnd.landscapeType.hasWater && entryPoint.worldY() >= AGame.GROUND_LEVEL)) {
				entryPoint = null;
			}
			if (entryPoint != null && entryPoint.body != ultimateBoardTarget) {
				entryPoint = null;
			}
			if (entryPoint == null && ultimateBoardTarget != null && ultimateBoardTarget instanceof Airship) {
				entryPoint = findEntryPoint(gnd.landscapeType.hasWater);
			}
			double targetX = getX(), targetY = getY();
			boolean doMove = false;
			boolean landing = false;
			if (leaveOnEmptyAmmo && type.ammoCapacity > 0 && ammo == 0) {
				targetY = 0;
				if (c.sides.indexOf(side) == 0) {
					targetX = -c.combatAreaW() / 2 - 200;
					if (getX() <= -c.combatAreaW() / 2 - 100) {
						return true;
					}
				} else {
					targetX = c.combatAreaW() / 2 + 200;
					if (getX() >= c.combatAreaW() / 2 + 100) {
						return true;
					}
				}
				doMove = true;
			} else if (poppedOutOfTile != null && poppedOutOfTile.ship.containsTile(poppedOutOfTile) && side.ships.contains(poppedOutOfTile.ship) && (hp <= type.returnToRepairHP || (type.ammoCapacity > 0 && ammo <= 0) || (poppedOutOfTile.ship.canGiveAircraftCommands() && poppedOutOfTile.ship.aircraftMode == AircraftBehaviourMode.STAY_PUT))) {
				if (popIn(poppedOutOfTile.ship, false, c)) {
					return true;
				}
				targetX = poppedOutOfTile.worldX();
				targetY = poppedOutOfTile.worldY();
				doMove = true;
				landing = true;
			} else if (entryPoint != null) {
				Airship boardTargetShip = (Airship) ultimateBoardTarget;
				boolean asBoarder = side != c.sideOf(boardTargetShip);
				if (popIn(boardTargetShip, asBoarder, c)) {
					return true;
				}

				targetX = entryPoint.worldX();
				targetY = entryPoint.worldY();
				doMove = true;
			} else if (interceptTroops(c) && interceptTarget != null) {
				doMove = true;
				targetX = interceptTarget.getX() + interceptTarget.getBBWidth() / 2;
				targetY = interceptTarget.getY() + interceptTarget.getBBHeight() / 2;
				/*if (targetX > x) {
					targetX += type.strafeOvershoot;
				} else {
					targetX -= type.strafeOvershoot;
				}*/
			} else if (type.shootsShips && attackTarget != null) {
				if (strafeTo != null &&
					(
						(
							((strafeTo.x - getX()) * (strafeTo.x - getX()) + (strafeTo.y - getY()) * (strafeTo.y - getY()) < AGame.SGS * AGame.SGS * 9) ||
							strafeTo.x < attackTarget.getX() - type.strafeOvershoot * 2 ||
							strafeTo.x > attackTarget.getX() + attackTarget.getBBWidth() + type.strafeOvershoot * 2 ||
							strafeTo.y > attackTarget.getY() + attackTarget.getBBHeight()
						)
						||
						(
							gnd.landscapeType.hasWater && strafeTo.y >= AGame.GROUND_LEVEL
						)
					)
				) {
					strafeTo = null;
				}
				if (strafeTo == null) {
					double strafeX =
							getX() + getBBWidth() / 2 < attackTarget.getX() + attackTarget.getBBWidth() / 2
							? attackTarget.getX() + attackTarget.getBBWidth() + type.strafeOvershoot
							: attackTarget.getX() - type.strafeOvershoot;
					double highestLandscapePoint = gnd.landscapeType.hasWater ? AGame.GROUND_LEVEL : 10000;
					if (strafeX > getX()) {
						for (double strafePos = getX(); strafePos < strafeX; strafePos += AGame.SGS) {
							highestLandscapePoint = StrictMath.min(highestLandscapePoint, gnd.solidYBoundaryAt(strafePos));
						}
					} else {
						for (double strafePos = getX(); strafePos > strafeX; strafePos -= AGame.SGS) {
							highestLandscapePoint = StrictMath.min(highestLandscapePoint, gnd.solidYBoundaryAt(strafePos));
						}
					}
					double strafeMin = type.bombs ? (attackTarget.getY() - type.maxRange + AGame.SGS) : (attackTarget.getY() - AGame.SGS * 2);
					double strafeMax = type.bombs ? StrictMath.min(StrictMath.min(attackTarget.getY() - type.maxRange / 2 + AGame.SGS, highestLandscapePoint - AGame.SGS * 3), attackTarget.getY() - AGame.SGS * 3) : (StrictMath.min(highestLandscapePoint - AGame.SGS * 3, attackTarget.getY() + attackTarget.getBBHeight() - AGame.SGS));
					double strafeY = strafeMax;
					if (strafeMax > strafeMin) {
						strafeY = strafeMin + c.r.nextDouble() * (strafeMax - strafeMin);
					}
					strafeTo = new Pt(strafeX, strafeY);
				}
				if (strafeTo != null) {
					doMove = true;
					targetX = strafeTo.x;
					targetY = strafeTo.y;
				}
			} else if (timeSinceLaunch > type.launchLength) {
				// Idle behaviour
				if (strafeTo != null && (strafeTo.x - getX()) * (strafeTo.x - getX()) + (strafeTo.y - getY()) * (strafeTo.y - getY()) < AGame.SGS * AGame.SGS * 9) {
					strafeTo = null;
				}
				if (strafeTo == null) {
					Airship mom = null;
					double strafeX = dx > 0 ? getX() - type.strafeOvershoot * 2 : getX() + type.strafeOvershoot * 2;
					if (poppedOutOfTile != null && poppedOutOfTile.ship.containsTile(poppedOutOfTile) && side.ships.contains(poppedOutOfTile.ship) && poppedOutOfTile.ship.getY() < AGame.GROUND_LEVEL) {
						mom = poppedOutOfTile.ship;
						strafeX = mom.getX() + mom.getBBWidth() / 2 > getX() ? (mom.getX() + mom.getBBWidth() + type.strafeOvershoot) : (mom.getX() - type.strafeOvershoot);
					}
					double highestLandscapePoint = gnd.landscapeType.hasWater ? AGame.GROUND_LEVEL : 10000;
					if (strafeX > getX()) {
						for (double strafePos = getX(); strafePos < strafeX; strafePos += AGame.SGS) {
							highestLandscapePoint = StrictMath.min(highestLandscapePoint, gnd.solidYBoundaryAt(strafePos));
						}
					} else {
						for (double strafePos = getX(); strafePos > strafeX; strafePos -= AGame.SGS) {
							highestLandscapePoint = StrictMath.min(highestLandscapePoint, gnd.solidYBoundaryAt(strafePos));
						}
					}
					double strafeY = (mom == null ? highestLandscapePoint : StrictMath.min(mom.getY(), highestLandscapePoint)) - AGame.SGS * 3;
					strafeTo = new Pt(strafeX, strafeY);
					//System.out.println("idle " + strafeX + " " + strafeY + " | " + mom);
				}
				if (strafeTo != null) {
					doMove = true;
					targetX = strafeTo.x;
					targetY = strafeTo.y;
				}
			}
			
			if (doMove &&
					type.isAircraft() && type.shootTroopsRange > 0 &&
					poppedOutOfTile != null && side.ships.contains(poppedOutOfTile.ship) && poppedOutOfTile.ship.containsTile(poppedOutOfTile) &&
					poppedOutOfTile.ship.aircraftMode == AircraftBehaviourMode.GUARD &&
					(
						StrictMath.abs(targetX - poppedOutOfTile.ship.getX() - poppedOutOfTile.ship.getBBWidth() / 2) > poppedOutOfTile.ship.getBBWidth() / 2 + type.guardRange ||
						StrictMath.abs(targetY - poppedOutOfTile.ship.getY() - poppedOutOfTile.ship.getBBHeight() / 2) > poppedOutOfTile.ship.getBBHeight() / 2 + type.guardRange
					)
			) {
				// It's on guard duty and more than 400px away from the mothership's boundaries.
				targetX = poppedOutOfTile.ship.getX() + poppedOutOfTile.ship.getBBWidth() / 2;
				targetY = poppedOutOfTile.ship.getY() + poppedOutOfTile.ship.getBBHeight() / 2;
			}

			if (doMove) {
				double trackingDxPrev = trackingDx;
				double rx = getX() + (dx * StrictMath.abs(dx) / type.airXAcceleration) * (1 - type.airOvershoot);
				double ry = getY() + (dy * StrictMath.abs(dy) / (dy > 0 ? type.airUpAcceleration : type.airDownAcceleration)) * (1 - type.airOvershoot);
				double accelStrength = type.launchLength <= 0 ? 1 : StrictMath.min(1, 1.0 * timeSinceLaunch / type.launchLength);
				double rGroundBoundary = gnd.solidYBoundaryAt(rx);
				double groundBoundary = gnd.solidYBoundaryAt(getX());
				if (gnd.landscapeType.hasWater) {
					rGroundBoundary = StrictMath.min(rGroundBoundary, AGame.GROUND_LEVEL);
					groundBoundary = StrictMath.min(groundBoundary, AGame.GROUND_LEVEL);
				}
				double bbH = getBBHeight();
				boolean hardStop = !landing && getY() > groundBoundary - bbH;
				boolean pullUp = !landing && (ry > rGroundBoundary - bbH - AGame.SGS * 2 || getY() > groundBoundary - bbH - AGame.SGS * 2);
				boolean noDown = ry > rGroundBoundary - bbH - AGame.SGS * 3 || getY() > groundBoundary - bbH - AGame.SGS * 3;
				if (StrictMath.abs(getX() - targetX) / type.airXTopSpeed < StrictMath.abs(getY() - targetY) / type.airDownTopSpeed) {
					pullUp = false;
					noDown = false;
					if (StrictMath.abs(getX() - targetX) < AGame.SGS) {
						hardStop = false;
					}
				}
				if (targetX > rx) {
					trackingDx += type.airXAcceleration * ms * accelStrength;
				} else if (targetX < rx) {
					trackingDx -= type.airXAcceleration * ms * accelStrength;
				}
				if (pullUp) {
					dy -= type.airUpAcceleration * ms * accelStrength;
				} else if (targetY < ry) {
					dy -= type.airUpAcceleration * ms * accelStrength;
				} else if (targetY > ry && !noDown) {
					dy += type.airDownAcceleration * ms * accelStrength;
				}
				if (hardStop) {
					dy = StrictMath.min(0, dy);
				}
				if (trackingDx > 0 && trackingDxPrev <= 0) {
					trackingDx = StrictMath.max(trackingDx, type.airXMinSpeed);
				}
				if (trackingDx < 0 && trackingDxPrev >= 0) {
					trackingDx = StrictMath.min(trackingDx, -type.airXMinSpeed);
				}
			}
			if (trackingDx > type.airXTopSpeed) {
				trackingDx = type.airXTopSpeed;
			}
			if (trackingDx < -type.airXTopSpeed) {
				trackingDx = -type.airXTopSpeed;
			}
			dx = trackingDx;
			if (dx < 0 && dx > -type.airXMinSpeed) {
				dx = -type.airXMinSpeed;
			}
			if (dx > 0 && dx < type.airXMinSpeed) {
				dx = type.airXMinSpeed;
			}
			if (dy > type.airDownTopSpeed) {
				dy = type.airDownTopSpeed;
			}
			if (dy < -type.airUpTopSpeed) {
				dy = -type.airUpTopSpeed;
			}
		} else {
			// Not active
			if (type.crashesOnDeath && ship == null && boardingShip == null) {
				if (!side.outsideCrewCrashing.contains(type)) {
					side.outsideCrewCrashing.add(type);
					c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("outsideCrewCrashing " + type.name, side, getX(), getY(), type, null));
				}
				dy += c.physics.gravity * ms;
				dx += (dx < 0 ? -0.3 : 0.3) * c.physics.gravity * ms;
				if (gnd.landscapeType.hasWater && getY() + getBBHeight() / 2 > AGame.GROUND_LEVEL) {
					// Splash
					explode(c, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, onViewingSide, true);
					ParticleType splash = ParticleType.ofName("waterSplash");
					for (int i = 0; i < 20; i++) {
						c.particles.add(new Particle(splash, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2));
					}
					c.play(MiscCombatSound.SMALL_SPLASH, getX() + getBBWidth() / 2, getY() + getBBHeight(), 0, 0, 3, true);
					return true;
				} else if (getY() + getBBHeight() / 2 > gnd.yBoundaryAt(getX() + getBBWidth() / 2)) {
					// Splode
					explode(c, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, onViewingSide, false);
					return true;
				} else {
					return false;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	public boolean outsideTick(int ms, Combat c, Side side, boolean onViewingSide) {
		if (type.canFly) {
			animTick(ms, c, 1);
			return outsideFlyingTick(ms, c, side, onViewingSide);
		}
		
		LandFormation gnd = c.landFormations.get(0);
		if (gnd.landscapeType.hasWater && getY() > AGame.GROUND_LEVEL) {
			underwater = true;
			ParticleType splash = ParticleType.ofName("waterSplash");
			if (drowningProgress == 0) {
				// Splash
				for (int i = 0; i < 20; i++) {
					c.particles.add(new Particle(splash, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2));
				}
				c.play(MiscCombatSound.SMALL_SPLASH, getX() + getBBWidth() / 2, getY() + getBBHeight(), 0, 0, 1.2, true);
				attachedTo = null;
				dx /= 20;
				dy /= 20;
				if (type.drowningTime == 0) {
					if (type.explosionSize > 0) {
						explode(c, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, onViewingSide, false);
					}
					return true;
				} else {
					drowningProgress = type.underwaterGraceTime;
				}
			} else {
				drowningProgress += ms;
				if (drowningProgress >= type.underwaterGraceTime + type.drowningTime) {
					hp = 0;
				} else if (AGame.ANIM_R.nextInt(1000) < ms) {
					c.particles.add(new Particle(splash, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2));
				}
				if (getY() >= Combat.CRUSH_DEPTH) {
					return true;
				} else {
					return false;
				}
			}
		} else {
			underwater = false;
			drowningProgress = 0;
		}
		
		if (type.shootsShips || interceptTroops(c)) {
			outsideShootingTick(ms, c, side, onViewingSide);
		}
		
		//#SpikeProfiler.start("outsideTick");
		if (mvXOffset == 0) {
			mvXOffset = c.r.nextDouble() * AGame.SGS / 2 - AGame.SGS / 4;
		}
		
		// If the place we've been told to board has gone away, clear it as a target.
		if (ultimateBoardTarget != null && !exists(ultimateBoardTarget, c)) {
			ultimateBoardTarget = null;
			proximateBoardTarget = null;
		}
		msUntilRecalcJumpPoint -= ms;
		if (msUntilRecalcJumpPoint < -100) {
			msUntilRecalcJumpPoint = RECALC_JUMP_POINT_WAIT;
		}
		msUntilRecalcWalkPoint -= ms;
		if (msUntilRecalcWalkPoint < -100) {
			msUntilRecalcWalkPoint = RECALC_JUMP_POINT_WAIT;
		}
		//#SpikeProfiler.start("anim");
		animTick(ms, c, 1);
		//#SpikeProfiler.end("anim");
		shoutCooldown -= ms;
		initialShoutCooldown -= ms;
		shoutMs -= ms;
		if (shoutMs <= 0) {
			shout = null;
		}
		mvDx = 0;
		mvDy = 0;
		
		if (grabbed) { return !active(); }
		
		//#SpikeProfiler.start("pre");
		// If we've landed on the PBT, we can clear the hook.
		if (attachedTo != null && attachedTo == proximateBoardTarget && (!hookLaunched || hookProgress == 0)) {
			proximateBoardTarget = null;
			hookLaunched = false;
		}
		
		// If we have an UBT and are on a GridBody, we find its PBT if possible
		if (!hookLaunched) {
			if (ultimateBoardTarget != null && attachedTo instanceof GridBody) {
				if (!c.bodyPathing.connected((GridBody) attachedTo, proximateBoardTarget, type.assumedJumpDist, type.hasHook ? type.hookRopeLength : BodyPathing.CANNOT_HOOK, c)) {
					proximateBoardTarget = null;
				}
				if (proximateBoardTarget == null) {
					proximateBoardTarget = c.bodyPathing.getNextInPath((GridBody) attachedTo, ultimateBoardTarget, type.assumedJumpDist, type.hasHook ? type.hookRopeLength : BodyPathing.CANNOT_HOOK, c);
				}
			} else {
				proximateBoardTarget = null;
			}
		}
		//#SpikeProfiler.end("pre");
		
		// Clear jump source grid refs on wrong body.
		if (attachedTo != null && jumpSourceGR != null && jumpSourceGR.body != attachedTo) {
			jumpSourceGR = null;
		}
		
		// Are we on the Ultimate Board Target?
		if (attachedTo == ultimateBoardTarget && ultimateBoardTarget != null && (!hookLaunched || hookProgress == 0)) {
			//#SpikeProfiler.start("onBoardTarget");
			hookLaunched = false;
			if (ultimateBoardTarget instanceof Airship) {
				// Try to enter the UBT.
				Airship boardTargetShip = (Airship) ultimateBoardTarget;
				boolean asBoarder = side != c.sideOf(boardTargetShip);
				if (canPopIn(boardTargetShip) && popIn(boardTargetShip, asBoarder, c)) {
					return true;
				}
				if (!solidAndExists(entryPoint, c) || !entryPoint.enterable()) {
					entryPoint = null;
				}
				if (entryPoint != null && entryPoint.body != ultimateBoardTarget) {
					entryPoint = null;
				}
				if (entryPoint == null) {
					//#SpikeProfiler.start("findEntryPoint");
					entryPoint = findEntryPoint(gnd.landscapeType.hasWater);
					//#SpikeProfiler.end("findEntryPoint");
				}
				if (entryPoint != null) {
					moveTowardsOutside(entryPoint);
				}
			} else {
				ultimateBoardTarget = null;
			}
			//#SpikeProfiler.end("onBoardTarget");
		} else if (hookLaunched) {
			// We've fired a hook.
			//#SpikeProfiler.start("hookLaunched");
			// Has the hook landed?
			if (hookedGR != null) {
				if (!solidAndExists(hookedGR, c)) {
					hookLaunched = false;
					ignoring = null;
					hookedGR = null;
				} else if (!winching) {
					// If it's landed but we're not winching yet, let go of where
					// we were, and start winching.
					drop();
					winching = true;
				} else {
					// Winch until we get to solid ground.
					hookProgress -= type.winchSpeed * ms;
					if (hookProgress < 0) {
						hookProgress = 0;
						//hookLaunched = false; // If in doubt, hang in there forever.
						ignoring = null;
					}
					// And we should eventually collide with *something*.
				}
			} else {
				// The hook is travelling outwards.
				hookProgress += type.hookSpeed * ms;
				if (hookProgress >= hookDist) {
					// Collide and see if it worked.
					int tileX = (int) ((hookTarget.x - hookTargetBody.getX()) / AGame.SGS);
					int tileY = (int) ((hookTarget.y - hookTargetBody.getY()) / AGame.SGS);
					GridRef hitGR = new GridRef(tileX, tileY, hookTargetBody);
					if (solidAndExists(hitGR, c)) {
						if (type.hookHitSnd != null) {
							c.play(type.hookHitSnd, hookTarget.x, hookTarget.y, 0, 0, onViewingSide);
						}
						double tx = hookTargetBody.getX() + hitGR.gridX * AGame.SGS;
						double ty = hookTargetBody.getY() + hitGR.gridY * AGame.SGS;
						hookedGR = hitGR;
						hookProgress = StrictMath.sqrt((getX() - hookTarget.x) * (getX() - hookTarget.x) + (getY() - hookTarget.y) * (getY() - hookTarget.y));
						hookDist = hookProgress;
						hookTarget = new Pt(hookTarget.x - tx, hookTarget.y - ty);
					} else {
						// The hook missed. Retry later.
						hookLaunched = false;
						newThrowWait = NEW_THROW_WAIT;
					}
				}
			}
			//#SpikeProfiler.end("hookLaunched");
		} else if (proximateBoardTarget != null && attachedTo != null) {
			// We have a PBT.
			//#SpikeProfiler.start("attached");
			GridRef[] tt = new GridRef[1];
			double[] angleAndStrengthRef = new double[2];
			//#SpikeProfiler.start("findJumpAngle");
			if (isSmart) {
				findJumpAngleAndStrength(getX(), getY(), tt, angleAndStrengthRef);
				//System.out.println("jas " + angleAndStrengthRef[0] + " " + angleAndStrengthRef[1]);
			}
			//#SpikeProfiler.end("findJumpAngle");
			boolean walking = false;
			// Can we just walk there?
			if (attachedTo instanceof GridBody) {
				// And we're on a grid body. Maybe we can just walk to our target?
				//#SpikeProfiler.start("moveOnGridBody");
				if (!solidAndExists(walkToTargetGR, c)) {
					walkToTargetGR = null;
				}
				if (isSmart && jumpSourceGR == null && walkToTargetGR == null && msUntilRecalcWalkPoint <= 0) {
					walkToTargetGR = findWalkTo();
					msUntilRecalcWalkPoint = RECALC_JUMP_POINT_WAIT;
				}
				if (walkToTargetGR != null && walkToIsValidForBoarding()) {
					walkToGR = null;
					moveTowardsOutside(walkToTargetGR);
					walking = true;
				} else {
					walkToTargetGR = null;
					// Just climb down if possible.
					if (attachedTo instanceof Airship && ((Airship) attachedTo).type.onGround) {
						mvDx = 0;
						mvDy = type.outsideSpeed;
						return !active();
					}
				}
				
				if (canSwitchToBoardTarget(getX(), getY())) {
					// If we're overlapping with eg the target building, we can detach from
					// the current location and attach ourselves to it.
					switchToBoardTarget();
					walkToTargetGR = null;
					walking = true;
				}
				//#SpikeProfiler.end("moveOnGridBody");
			}
			// We can't just walk there.
			if (!walking) {
				if (type.hasHook) {
					//#SpikeProfiler.start("hook");
					if (isSmart && angleAndStrengthRef[0] == JUMP_STRAIGHT_DOWN) {
						drop();
						dx = 0;
					} else if (newThrowWait > 0) {
						newThrowWait -= ms;
					} else {
						Pt[] out_BestPt = { null };
						if (checkHookFrom(getX(), getY(), side, out_BestPt)) {
							hookTarget = out_BestPt[0];
							hookLaunched = true;
							winching = false;
							hookedGR = null;
							hookSource = new Pt(getX(), getY());
							hookDist = 1 + StrictMath.sqrt((hookSource.x - hookTarget.x) * (hookSource.x - hookTarget.x) + (hookSource.y - hookTarget.y) * (hookSource.y - hookTarget.y));
							hookTargetBody = proximateBoardTarget;
							hookProgress = 0;
							if (type.hookLaunchSnd != null) {
								c.play(type.hookLaunchSnd, getX(), getY(), 0, 0, onViewingSide);
							}
							//System.out.println("hk");
						} else if (attachedTo instanceof GridBody) {
							// We can't hook from where we are, but maybe we can move somewhere?
							GridBody att = (GridBody) attachedTo;
							if (solidAndExists(jumpSourceGR, c)) {
								double tx = att.getX() + jumpSourceGR.gridX * AGame.SGS;
								double ty = att.getY() + jumpSourceGR.gridY * AGame.SGS;
								//#SpikeProfiler.start("findJumpAngle2");
								findJumpAngleAndStrength(tx, ty, null, angleAndStrengthRef);
								//#SpikeProfiler.end("findJumpAngle2");
								if (Double.isNaN(angleAndStrengthRef[0]) && !checkHookFrom(tx, ty, null, null)) {
									jumpSourceGR = null;
									msUntilRecalcJumpPoint = 0; // Target may be moving and needing instant/constant recalc.
								}
							} else {
								jumpSourceGR = null;
							}
							if (jumpSourceGR == null && msUntilRecalcJumpPoint <= 0) { // qqDPS Restrict to recalcing occasionally.
								//#SpikeProfiler.start("findHookPoint");
								jumpSourceGR = findHookPoint(side);
								//#SpikeProfiler.end("findHookPoint");
								if (jumpSourceGR == null) {
									//#SpikeProfiler.start("findJumpPoint");
									jumpSourceGR = isSmart ? findJumpPoint(side, /* downOnly*/ true) : null;
									//#SpikeProfiler.end("findJumpPoint");
								}
								msUntilRecalcJumpPoint = RECALC_JUMP_POINT_WAIT;
							}
							if (jumpSourceGR != null) {
								moveTowardsOutside(jumpSourceGR);
							}
						}
					}
					//#SpikeProfiler.end("hook");
				} else {
					//#SpikeProfiler.start("jump");
					// No hook, want to figure out if jump possible.
					if (isSmart && !Double.isNaN(angleAndStrengthRef[0])) {
						if (angleAndStrengthRef[0] == JUMP_STRAIGHT_DOWN) {
							drop();
							dx = 0;
						} else {
							jump(angleAndStrengthRef[0], angleAndStrengthRef[1]);
						}
					} else if (attachedTo instanceof GridBody) {
						// Can't jump from where we are, but maybe we can move somewhere?
						if (isSmart) {
							if (solidAndExists(jumpSourceGR, c)) {
								//#SpikeProfiler.start("findJumpAngle2");
								findJumpAngleAndStrength(jumpSourceGR.worldX(), jumpSourceGR.worldY(), null, angleAndStrengthRef);
								//#SpikeProfiler.end("findJumpAngle2");
								if (Double.isNaN(angleAndStrengthRef[0])) {
									jumpSourceGR = null;
									msUntilRecalcJumpPoint = 0; // Target may be moving and needing instant/constant recalc.
								}
							} else {
								jumpSourceGR = null;
							}
							if (jumpSourceGR == null && msUntilRecalcJumpPoint <= 0) { // qqDPS Restrict to recalcing occasionally.
								//#SpikeProfiler.start("findJumpPoint");
								jumpSourceGR = findJumpPoint(side, /*jumpDownOnly*/ false);
								//#SpikeProfiler.end("findJumpPoint");
								//System.out.println("Recalcing jump point: " + jumpSourceGR);
								msUntilRecalcJumpPoint = RECALC_JUMP_POINT_WAIT;
							}
						}
						if (jumpSourceGR != null) {
							//System.out.println(type.name + " jsgr");
							moveTowardsOutside(jumpSourceGR);
						}
					}
					//#SpikeProfiler.end("jump");
				}
			}
			//#SpikeProfiler.end("attached");
		}
		if (!solidAndExists(walkToGR, c)) {
			walkToGR = null;
		}
		// if we are just standing around, disperse.
		if (attachedTo instanceof GridBody && dispersed != attachedTo && jumpSourceGR == null && walkToTargetGR == null && proximateBoardTarget == null && attachedTo != ultimateBoardTarget) {
			//#SpikeProfiler.start("disperse");
			GridRef dis = disperse(c, (GridBody) attachedTo);
			if (dis != null) {
				walkToGR = dis;
				dispersed = attachedTo;
			}
			//#SpikeProfiler.end("disperse");
		}
		if (attachedTo instanceof GridBody && walkToTargetGR == null && jumpSourceGR == null && attachedTo != ultimateBoardTarget) {
			//#SpikeProfiler.start("moveOutside");
			GridBody att = (GridBody) attachedTo;
			// If we have nothing else to do, go stand on top of the current GridBody.
			if (walkToGR == null && msUntilRecalcWalkPoint <= 0 && msUntilRecalcJumpPoint <= 0) {
				int gx = (int) StrictMath.floor((getX() - attachedTo.getX()) / AGame.SGS);
				walkToGR = new GridRef(gx, att.firstSolidBlockYAt(gx), att);
				msUntilRecalcWalkPoint = RECALC_JUMP_POINT_WAIT;
			}

			if (walkToGR != null && walkToGR.body == attachedTo) {
				moveTowardsOutside(walkToGR);
			}
			//#SpikeProfiler.end("moveOutside");
		}
		//#SpikeProfiler.end("outsideTick");
		return !active();
	}
	
	private void moveTowardsOutside(GridRef gr) {
		double tx = gr.worldX() + mvXOffset + AGame.SGS / 2 - getBBWidth() / 2;
		double ty = gr.worldY() - getBBHeight() + 0.5;
		double d = StrictMath.abs(getX() - tx) + StrictMath.abs(getY() - ty);
		// Walking on ground
		if (attachedTo != null && attachedTo.isImmobile() && attachedTo instanceof LandFormation) {
			LandFormation lf = (LandFormation) attachedTo;
			//System.out.println("groundWalk");
			if (getX() - tx < -1) {
				double leadingY = lf.yBoundaryAt(getX() + getBBWidth() * 0.9) - getBBHeight() + 0.5;
				if (leadingY < getY() - 0.1) {// && lf.solidAt((int) StrictMath.floor((x - attachedTo.x) / AGame.SGS) - 1, (int) StrictMath.floor((y - attachedTo.y) / AGame.SGS))) {
					mvDx = 0;
					mvDy = -type.outsideSpeed;
					//System.out.println("rightUp");
				} else {
					mvDx = type.outsideSpeed;
					mvDy = 0;
					//System.out.println("right");
				}
			} else if (getX() - tx > 1) {
				double leadingY = lf.yBoundaryAt(getX() + getBBWidth() * 0.1) - getBBHeight() + 0.5;
				if (leadingY < getY() - 0.1) {// && lf.solidAt((int) StrictMath.floor((x - attachedTo.x) / AGame.SGS) + 1, (int) StrictMath.floor((y - attachedTo.y) / AGame.SGS))) {
					mvDx = 0;
					mvDy = -type.outsideSpeed;
					//System.out.println("leftUp");
				} else {
					mvDx = -type.outsideSpeed;
					mvDy = 0;
					//System.out.println("left");
				}
			} else {
				mvDx = 0;
				/*if (ty < y - 0.1) {
					mvDy = -OUTSIDE_SPEED;
				}*/
				mvDy = 0;
			}
		} else if (attachedTo instanceof GridBody) {
			// We can haz pathing.
			//System.out.println("pathing");
			GridBody att = (GridBody) attachedTo;
			GridLocation src = att.locationAtWorldCoords(getX() + getBBWidth() / 2, getY() + getBBHeight() / 2);
			if (src == null) {
				src = att.locationAtWorldCoords(getX(), getY() + getBBHeight());
				if (src == null) {
					src = att.locationAtWorldCoords(getX() + getBBWidth(), getY() + getBBHeight());
				}
				if (src == null) {
					src = att.locationAtWorldCoords(getX(), getY());
				}
				if (src == null) {
					src = att.locationAtWorldCoords(getX() + getBBWidth(), getY());
				}
			}
			if (src == null || src.reachable() == null) {
				//System.err.println("cannot find foothold");
			} else {
				if (outsideBodyPath == null || !outsideBodyPath.endsAt(gr)) {
					outsideBodyPath = att.getPath(src, gr);
				}
				if (outsideBodyPath != null) {
					double xDist = outsideBodyPath.nextWaypoint().worldX() + AGame.SGS / 2 - (getX() + getBBWidth() / 2);
					double yDist = outsideBodyPath.nextWaypoint().worldY() + AGame.SGS / 2 - (getY() + getBBHeight()/ 2);
					if (StrictMath.abs(xDist) > 1) {
						if (StrictMath.abs(yDist) > 1) {
							mvDx = xDist > 0 ? type.outsideSpeed / 1.41 : -type.outsideSpeed / 1.41;
							mvDy = yDist > 0 ? type.outsideSpeed / 1.41 : -type.outsideSpeed / 1.41;
						} else {
							mvDx = xDist > 0 ? type.outsideSpeed : -type.outsideSpeed;
							mvDy = 0;
						}
					} else {
						mvDx = 0;
						if (StrictMath.abs(yDist) > 1) {
							mvDy = yDist > 0 ? type.outsideSpeed : -type.outsideSpeed;
						} else {
							outsideBodyPath.nextWaypointIndex++;
						}
					}
				}
			}
		} else {
			// All other cases.
			//System.out.println("outsideWalk");
			if (d < 0.5) {
				mvDx = 0;
				mvDy = 0;
			} else {
				mvDx = type.outsideSpeed * (tx - getX()) / d;
				mvDy = type.outsideSpeed * (ty - getY()) / d;
			}
		}
	}

	@Override
	public double getBBWidth() {
		return 
				type.animLooks[0].bundle == null
				? type.animLooks[0].frameAnimationBoundingBoxWidth
				: type.animLooks[0].bundle.width;
	}

	@Override
	public double getBBHeight() {
		return 
				type.animLooks[0].bundle == null
				? type.animLooks[0].frameAnimationBoundingBoxHeight
				: type.animLooks[0].bundle.height;
	}
	
	// Shouting
	public String pickShout(String shoutType) {
		if (type.shouts == null) { return null; }
		ArrayList<String> l = type.shouts.shouts.get(shoutType);
		return l == null || l.isEmpty() ? null : l.get(AGame.ANIM_R.nextInt(l.size()));
	}
	
	public void shout(String type) {
		if (shoutCooldown <= 0) {
			doShout(pickShout(type));
		}
	}
	
	public void doShout(String sh) {
		if (sh == null) { return; }
		doShoutExplicitString(_t(sh));
	}
	
	public void doShoutExplicitString(String sh) {
		if (sh == null) { return; }
		if (initialShoutCooldown > 0) { return; }
		if (ship == null && boardingShip == null) { return; }
		bigShout = false;
		shout = sh;
		shoutCooldown = AGame.ANIM_R.nextInt(SHOUT_MAX_DELAY - SHOUT_MIN_DELAY) + SHOUT_MIN_DELAY;
		shoutMs = SHOUT_LENGTH;
		Airship myShip = ship == null ? boardingShip : ship;
		if (myShip != null) {
			shoutOriginX = (int) (myShip.getX() + myShip.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2);
			shoutOriginY = (int) (myShip.getY() + currentTile.y * AGame.SGS + AGame.SGS / 2);
		} else {
			shoutOriginX = (int) getX();
			shoutOriginY = (int) getY();
		}
		shoutW = 0;
	}
	
	private void bigShout(String sh) {
		if (sh == null) { return; }
		if (initialShoutCooldown > 0) { return; }
		shout = _t(sh).toUpperCase(Locale.ENGLISH);
		bigShout = true;
		shoutCooldown = AGame.ANIM_R.nextInt(SHOUT_MAX_DELAY - SHOUT_MIN_DELAY) + SHOUT_MIN_DELAY;
		shoutMs = BIG_SHOUT_LENGTH;
		Airship myShip = ship == null ? boardingShip : ship;
		if (myShip != null) {
			shoutOriginX = (int) (myShip.getX() + ship.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2);
			shoutOriginY = (int) (myShip.getY() + currentTile.y * AGame.SGS + AGame.SGS / 2);
		} else {
			shoutOriginX = (int) getX();
			shoutOriginY = (int) getY();
		}
		shoutW = 0;
	}
	
	private void boarderShout(int ms, Combat c) {
		shoutCooldown -= ms;
		shoutMs -= ms;
		if (shoutMs <= 0) {
			shout = null;
			if (active()) {
				if (currentTile.module.type.getCommand(boardingShip.currentBonuses) > 0) {
					doShout(pickShout("boarderReachedBridge"));
				} else {
					doShout(pickShout("boarder"));
				}
			} else if (hp > 0) {
				doShout(pickShout("injured"));
			}
		}
	}
	
	private void shout(int ms, Combat c, boolean won, boolean lost) {
		shoutCooldown -= ms;
		initialShoutCooldown -= ms;
		shoutMs -= ms;
		if (ship.getySpeed() > 0.3) {
			fallingTime += ms;
		} else {
			fallingTime = 0;
		}
		if (shoutMs <= 0) {
			shout = null;
			shoutMs = 160;
			//#SpikeProfiler.start("pickShout");
			Module jobM = job == null ? null : job.module();
			boolean isCaptain = job != null && job.isCaptain();
			if (ship.braking > 0 && isCaptain) {
				doShout(pickShout("braking"));
			} else if (c.time > 1000 && currentTile.module == jobM && currentTile.module.type.hasLift() && currentTile.module.type.getCoalReload(BonusSet.empty()) > 0 && currentTile.module.msUntilCoal <= 0 && currentTile.module.type.canResupplyInCombat(currentTile.ship.currentBonuses)) {
				bigShout(pickShout("suspOffline"));
			} else if (c.time > 3000 && currentTile.module == jobM && currentTile.module.type.getCoalReload(BonusSet.empty()) > 0 && currentTile.module.msUntilCoal > 0 && currentTile.module.msUntilCoal <= StrictMath.max(500, currentTile.module.type.getCoalReload(BonusSet.empty()) / 20) && currentTile.module.type.canResupplyInCombat(currentTile.ship.currentBonuses)) {
				bigShout(pickShout("needCoal"));
			} else if (isCaptain && !ship.footingLossReported && ship.reportFootingLoss && initialShoutCooldown <= 0) {
				ship.footingLossReported = true;
				doShout(pickShout("footingLoss"));
			} else if (shoutCooldown <= 0) {
				if (hp < type.minWorkingHP && hp > 0) {
					doShout(pickShout("injured"));
				} else if (currentTile.module.fire > 0) {
					if (currentTile.module.type.getExplodeHP(currentTile.module.ship.currentBonuses) > 0 && currentTile.module.hp < currentTile.module.type.getExplodeHP(currentTile.module.ship.currentBonuses) * currentTile.module.maxHP / currentTile.module.type.getHp(currentTile.module.ship.currentBonuses)) {
						bigShout(pickShout("aboutToExplode"));
					} else {
						doShout(pickShout("fire"));
					}
				} else if (isCaptain && ship.getDirectControlID() != -1 && ship.speedOrderSpoken != ship.speedOrder) {
					ship.speedOrderSpoken = ship.speedOrder;
					switch (ship.speedOrder) {
						case FULL_SPEED_LEFT:
							doShout(pickShout(ship.flipped ? "directFullSpeedAhead" : "directFullReverse"));
							break;
						case FULL_SPEED_RIGHT:
							doShout(pickShout(!ship.flipped ? "directFullSpeedAhead" : "directFullReverse"));
							break;
						case STOP:
							doShout(pickShout("directHalt"));
							break;
					}
				} else if (isCaptain && ship.ramming && !ship.ramOrderSpoken) {
					ship.ramOrderSpoken = true;
					doShout(pickShout("ramming"));
				} else if (isCaptain && ship.fireMode == FireMode.HOLD && !ship.fireOrderSpoken) {
					ship.fireOrderSpoken = true;
					doShout(pickShout("holdFire"));
				} else if (isCaptain && ship.fireMode == FireMode.AIMED && !ship.fireOrderSpoken) {
					ship.fireOrderSpoken = true;
					doShout(pickShout("aimedFire"));
				} else if (isCaptain && ship.fireMode == FireMode.RAPID && !ship.fireOrderSpoken) {
					ship.fireOrderSpoken = true;
					doShout(pickShout("rapidFire"));
				} else if (isCaptain && ship.aircraftMode == AircraftBehaviourMode.STAY_PUT && !ship.aircraftOrderSpoken) {
					ship.aircraftOrderSpoken = true;
					doShout(pickShout("aircraftStayPut"));
				} else if (isCaptain && ship.aircraftMode == AircraftBehaviourMode.GUARD && !ship.aircraftOrderSpoken) {
					ship.aircraftOrderSpoken = true;
					doShout(pickShout("aircraftGuard"));
				} else if (isCaptain && ship.aircraftMode == AircraftBehaviourMode.INTERCEPT && !ship.aircraftOrderSpoken) {
					ship.aircraftOrderSpoken = true;
					doShout(pickShout("aircraftIntercept"));
				} else if (isCaptain && ship.aircraftMode == AircraftBehaviourMode.ATTACK && !ship.aircraftOrderSpoken) {
					ship.aircraftOrderSpoken = true;
					doShout(pickShout("aircraftAttack"));
				} else if (c.time > 3000 && currentTile.module == jobM && currentTile.module.type.getClip(currentTile.module.ship.currentBonuses) > 1 && currentTile.module.ammoLeft == 0 && ship.getTotalResource(Resource.AMMO) > 0 && currentTile.module.type.canResupplyInCombat(currentTile.ship.currentBonuses)) {
					doShout(pickShout("needAmmo"));
				} else if (currentTile.module.hp < currentTile.module.maxHP / 2 && !currentTile.module.damageReported) {
					currentTile.module.damageReported = true;
					doShout(pickShout("damageReport"));
				} else if (currentTile.armour.hp <= 0 && !currentTile.armour.window && currentTile.armour.type != ArmourType.ofName("NONE") && !currentTile.module.armourDamageReported && currentTile.module.type != ModuleType.ofName("CORRIDOR")) {
					currentTile.module.armourDamageReported = true;
					doShout(pickShout("hullBreach"));
				} else if (fallingTime > 500) {
					doShout(pickShout("falling"));
				} else if (!occupied && won && AGame.ANIM_R.nextInt(80) == 0) {
					doShout(pickShout("victory"));
				} else if (!occupied && lost && AGame.ANIM_R.nextInt(80) == 0) {
					doShout(pickShout("defeat"));
				} else if (!ship.boarders.isEmpty()) {
					int bsz = ship.boarders.size();
					for (int bi = 0; bi < bsz; bi++) {
						if (ship.boarders.get(bi).active() && ship.boarders.get(bi).currentTile.module == currentTile.module) {
							doShout(pickShout("boarders"));
							break;
						}
					}
				}
				//#SpikeProfiler.start("" + shout);
				//#SpikeProfiler.end("" + shout);
				//#SpikeProfiler.end("pickShout");
			}
		}
	}
	
	public void hurt(Shot shot, int harm, Combat c, boolean onViewingSide) {
		boolean alive = alive();
		if (ship != null && ship.holdOnTime > 0) {
			holdOnHp = StrictMath.max(0, holdOnHp - harm);
		} else {
			hp = StrictMath.max(0, hp - harm);
		}
		if (alive) {
			if (shot != null && shot.sX < shot.tX) {
				anim.animate(AnimationType.SHOT_FROM_LEFT, null);
			} else {
				anim.animate(AnimationType.SHOT_FROM_RIGHT, null);
			}
		}
		// Need to calc our position differently
		int cx;
		int cy;
		if (ship != null && currentTile != null) {
			cx = ship.getIntX() + ship.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
			cy = ship.getIntY() + currentTile.y * AGame.SGS + AGame.SGS / 2;
		} else if (boardingShip != null && currentTile != null) {
			cx = boardingShip.getIntX() + boardingShip.gridXToWorldX(currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
			cy = boardingShip.getIntY() + currentTile.y * AGame.SGS + AGame.SGS / 2;
		} else {
			cx = (int) (getX() + getBBWidth() / 2);
			cy = (int) (getY() + getBBHeight() / 2);
		}
		
		if (alive && !alive() && type.deathSnd != null) {
			if (ship != null) {
				c.play(type.deathSnd, cx, cy, ship.getxSpeed(), ship.getySpeed(), onViewingSide);
			} else if (boardingShip != null) {
				c.play(type.deathSnd, cx, cy, boardingShip.getxSpeed(), boardingShip.getySpeed(), onViewingSide);
			} else {
				c.play(type.deathSnd, cx, cy, dx, dy, onViewingSide);
			}
		}
		if (alive && !alive() && shot != null && shot.shooterType != null && (ship != null || boardingShip != null)) {
			c.exceptionalCombatEvents.add(new ExceptionalCombatEvent(
					"killedBy " + shot.shooterType.name,
					boardingShip != null ? c.otherSide(c.sideOf(boardingShip)) : c.sideOf(ship),
					cx,
					cy,
					type, boardingShip != null ? boardingShip : ship));
		}
		if (alive && !alive() && shot != null) {
			if (shot.shooterType != null) {
				c.incStat(this, "crewKilledByCrew " + shot.shooterType.name);
			} else if (shot.weaponType != null) {
				c.incStat(this, "crewKilledByShot");
			}
		}
		if (alive() && harm > 0 && shot != null) {
			shout("hit");
		}
		if ((ship != null || boardingShip != null) && type.bloodParticle != null) {
			int parts = StrictMath.min(10, harm * 4);
			for (int i = 0; i < parts; i++) {
				c.particles.add(new Particle(type.bloodParticle,
					cx,
					cy));
			}
		} else if (type.bloodParticleExternal != null) {
			int parts = StrictMath.min(10, harm * 4);
			for (int i = 0; i < parts; i++) {
				c.particles.add(new Particle(type.bloodParticleExternal,
					cx,
					cy));
			}
		}
		if (alive && !alive() && type.explosionSize > 0 && !(type.crashesOnDeath && ship == null && boardingShip == null)) {
			explode(c, cx, cy, onViewingSide, false);
		}
	}
	
	public void explode(Combat c, double explodeX, double explodeY, boolean onViewingSide, boolean mute) {
		int n = (int) (12 * type.explosionSize);
		c.blasts.add(new Blast(explodeX, explodeY, type.explosionSize * 15, type.explosionSize * 10));

		int parts = 8;
		for (int i = 0; i < parts; i++) {
			c.particles.add(new Particle(ParticleType.ofName("explode_backs"),
					explodeX, explodeY, type.explosionSize));
		}
		parts = type.explosionSize > 0.5 ? 1 : 0;
		for (int i = 0; i < parts; i++) {
			c.particles.add(new Particle(ParticleType.ofName("shockwave"),
					explodeX, explodeY, type.explosionSize));
		}
		parts = n;
		for (int i = 0; i < parts; i++) {
			c.particles.add(new Particle(ParticleType.ofName("small_soot"),
					explodeX, explodeY));
			c.particles.add(new Particle(ParticleType.ofName("large_soot"),
					explodeX, explodeY));
		}
		if (!SimplePref.REDUCED_FLASHING.get()) {
			parts = 1;
			for (int i = 0; i < parts; i++) {
				c.particles.add(new Particle(ParticleType.ofName("explode"),
						explodeX,
						explodeY,
						type.explosionSize));
			}
		}
		parts = 12;
		for (int i = 0; i < parts; i++) {
			c.particles.add(new Particle(ParticleType.ofName("explode_bits"),
					explodeX, explodeY, type.explosionSize));
		}
		if (type.explosionSize <= 0.5) {
			c.play(MiscCombatSound.SMALL_SHOT_EXPLOSION, explodeX, explodeY, 0, 0, onViewingSide);
		} else {
			c.play(MiscCombatSound.SHOT_EXPLOSION, explodeX, explodeY, 0, 0, onViewingSide);
		}
	}

	public Crewman hit(Shot shot, Combat c, boolean onViewingSide) {
		if (shot.tX < getX() || shot.tY < getY() || shot.tX > getX() + getBBWidth() || shot.tY > getY() + getBBHeight()) {
			return null;
		}
		hurt(shot, shot.getPenDmg() + shot.getBlastDmg() + shot.getDirectDmg(), c, onViewingSide);
		c.msSinceInterestingCombatEvent = 0;
		return this;
	}
	
	public void startHoldingOn() {
		holdOnHp = hp;
	}
	
	public void stopHoldingOn() {
		hp = holdOnHp;
	}
}
