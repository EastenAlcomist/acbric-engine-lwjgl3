package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.catengine.util.Pt;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONObject;

public strictfp class  TacticalAI {
	public static final int X_GRID_STEP = 20, Y_GRID_STEP = 30;
	public static final int INSET = 8;
	public static final int MAX_GRID_V_SIZE = 2300 / Y_GRID_STEP;
	public static final double NO = -1000000;
	
	public final int gridVSize;
	public final int gridYAnchor;
	public final double[][][] grid;
	public final boolean[][] ramZones;
	public final int gridHSize;
	
	public final Airship ship;
	public final Combat c;
	public final Side mySide;
	public final Side enemySide;
	int yIndex = 0;
	int ticksUntilCommand = 0;
	int phaseTick = 0;
	int movePhaseTick = 0;
	int sideTickCounter = 0;
	int ticksUntilNextFireModeCheck = 0;
	int ticksRammersOnly = 0;
	Airship lastBoardTarget = null;
	boolean tunfmcSet = false;
	public boolean doesSurrender = true;
	public int baseSurrenderFactor = 8;
	public boolean inited = false;
	
	public AIQuality quality = AIQuality.SMART;
		
	public static boolean fixed = false;
	public static boolean lfs = true;
	
	public int gridXAnchor() {
		return -c.combatAreaW() / 2;
	}
	
	public TacticalAI(Airship ship, Combat c, Side mySide, Side enemySide) {
		gridVSize =
				!ship.type.mobile
					? 0
					: ship.type.onGround
						? 1
						: StrictMath.min(MAX_GRID_V_SIZE, StrictMath.max(0, ship.serviceCeilingWithActivatedAbilitiesAndModules()) / Y_GRID_STEP + 2);
		gridYAnchor = AGame.GROUND_LEVEL - gridVSize * Y_GRID_STEP;
		gridHSize = (c.combatAreaW() - (int) ship.getBBWidth() - 3) / X_GRID_STEP;
		grid = new double[gridVSize][gridHSize][2];
		ramZones = new boolean[gridVSize][gridHSize];
		this.ship = ship;
		this.c = c;
		this.mySide = mySide;
		this.enemySide = enemySide;
		if (mySide == null) {
			throw new RuntimeException("TacticalAI with no mySide");
		}
		if (enemySide == null) {
			throw new RuntimeException("TacticalAI with no enemySide");
		}
	}
	
	private void sideTick() {
		if (sideTickCounter-- > 0) {
			return;
		}
		// Gets executed once per side.
		lp: for (Iterator<Airship> it = mySide.reserve.iterator(); it.hasNext();) {
			Airship res = it.next();
			if (res.nonCombat()) { continue; }
			if (res.owner != null && res.owner.isPlayerControlled) { continue; }
			int proposedX = c.sides.indexOf(mySide) == 0 ? (-c.combatAreaW() / 2 + 1) : (c.combatAreaW() / 2 - res.getWidth() * AGame.SGS - 1);
			if (res.type.onGround) {
				double proposedY = c.landFormations.get(0).getVerticalPosition(res, proposedX, res.flipped, false);
				// Do not stack landships or buildings on top of each other even if they technically fit.
				for (int i = 0; i < mySide.ships.size(); i++) {
					Airship other = mySide.ships.get(i);
					if (other.type.onGround && proposedX < other.getX() + other.getBBWidth() && other.getX() < proposedX + res.getBBWidth()) {
						continue lp;
					}
				}
				if (c.canPlace(ship, mySide.ships, proposedX, (int) proposedY, 40, mySide)) {
					res.setX(proposedX);
					res.setY(proposedY);
					res.setAI(new TacticalAI(res, c, mySide, enemySide));
					mySide.ships.add(res);
					it.remove();
				}
			} else {
				for (double proposedY = AGame.GROUND_LEVEL - res.serviceCeiling(); proposedY < AGame.GROUND_LEVEL; proposedY += AGame.SGS) {
					if (c.canPlace(ship, mySide.ships, proposedX, (int) proposedY, 40, mySide)) {
						res.setX(proposedX);
						res.setY(proposedY);
						res.setAI(new TacticalAI(res, c, mySide, enemySide));
						mySide.ships.add(res);
						it.remove();
						break;
					}
				}
			}
		}
		sideTickCounter = 20;
		checkSurrender();
	}
	
	public void init() {
		calculate();
		ticksUntilCommand = mySide.ships.indexOf(ship) * 3 + 1;
		inited = true;
	}
	
	public void tick() {
		if (quality == AIQuality.INACTIVE) {
			return;
		}
		if (ship.getDirectControlID() != -1 ) {
			return;
		}
		if (!inited) {
			//System.out.println("I didn't get inited!");
			for (Side s : c.sides) {
				for (Airship ship : s.ships) {
					ship.precalcAIValues(c);
				}
			}
			init();
		}
		movePhaseTick++;
		if (mySide.ships.isEmpty() || ship == mySide.ships.get(0)) {
			sideTick();
		}
				
		if (!tunfmcSet) {
			ticksUntilNextFireModeCheck = mySide.ships.indexOf(ship) * 3 + 2;
			tunfmcSet = true;
		}
		
		if (!ship.type.mobile) {
			buildingMove();
			return;
		}
		int ptMult = grid.length == 1 ? 4 : 1;
		if (phaseTick++ % (mySide.ships.size() * ptMult) == mySide.ships.indexOf(ship)) {
			int aiMaxY = aiMaxY();
			int boardXDistMax = boardXDistMax();
			boolean wantsToBoard = wantsToBoard();
			boolean isShortRange = hasShortRangeWeapons();
			boolean hasHighStormAbility = hasHighStormAbility();
			calculateRow(yIndex, wantsToRam(), wantsToBoard, wantsToBoard || hasShortRangeWeapons(), isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility);
			yIndex = (yIndex + 1) % grid.length;
		}
		if (ticksUntilCommand <= 0 && movePhaseTick % mySide.ships.size() == mySide.ships.indexOf(ship)) {
			move();
		} else {
			ticksUntilCommand--;
		}
	}
	
	private void checkSurrender() {
		if (!doesSurrender && !(c.isRaid && mySide == c.sides.get(0))) { return; }
		if (c.isRaid && c.lootAmount == c.maxLootAmount) {
			doSurrender(c);
			return;
		}
		double surrenderFactor = baseSurrenderFactor;
		if (c.isRaid && c.lootAmount == 1) {
			surrenderFactor = 2.5;
		}
		if (c.isRaid && c.lootAmount == 2) {
			surrenderFactor = 1;
		}
		if (c.isRaid && c.lootAmount == 3) {
			surrenderFactor = 0.5;
		}
		int ourStrength = 0;
		int ssz = mySide.ships.size();
		for (int si = 0; si < ssz; si++) {
			Airship s = mySide.ships.get(si);
			if (s.ableToContributeToCombat(c)) {
				ourStrength += s.getCachedCost();
			}
			if (s.preventsSurrender()) {
				return;
			}
		}
		int enemyStrength = 0;
		ssz = enemySide.ships.size();
		for (int si = 0; si < ssz; si++) {
			Airship s = enemySide.ships.get(si);
			if (s.ableToContributeToCombat(c)) {
				enemyStrength += s.getCachedCost();
			}
		}
		
		surrenderFactor /= StrictMath.min(32, StrictMath.max(1, (c.time - 90000) / 150000.0));
		
		if (enemyStrength > ourStrength * surrenderFactor) {
			doSurrender(c);
			return;
		}
		
		boolean doSurrender = !c.isRaid;
		boolean rammersOnly = !c.isRaid;
		for (Airship s : mySide.ships) {
			if (s.hasActiveCrew()) {
				if (s.canShoot() || s.hasActiveFlyers(c) || s.hasActiveBoarders(c)) {
					doSurrender = false;
					rammersOnly = false;
					break;
				}
				if (s.hasRam() && s.canMove()) {
					doSurrender = false;
				}
			}
		}
		
		if (doSurrender) {
			doSurrender(c);
		} else if (rammersOnly) {
			if (ticksRammersOnly++ > 40) {
				doSurrender(c);
			}
		}
	}
	
	public static final int AI_COMMAND = -2;
	
	private void doSurrender(Combat c) {
		c.execCommand(msg("surrender").put("side", c.sides.indexOf(mySide)), AI_COMMAND);
	}
	
	private void buildingMove() {
		if (!ship.readyForCommand()) { return; }
		FireMode bfm = getBestFireMode();
		Airship boardable = getBoardable();
		if (ship.fireMode != bfm) {
			c.execCommand(msg("fireMode")
					.put("id", c.getShipID(ship))
					.put("value", bfm.name()), AI_COMMAND);
		} else if (currentlyHookingHasBoarders()) {
			c.execCommand(msg("cutOwnTethers")
				.put("id", c.getShipID(ship)), AI_COMMAND);
		} else if (boardable != null && ship.board == null && lastBoardTarget != boardable) {
			c.execCommand(msg("board")
				.put("id", c.getShipID(ship))
				.put("target", c.getShipID(boardable)), AI_COMMAND);
			//System.out.println("brd");
			lastBoardTarget = boardable;
		} else if (hasHook() && boardable != null && !currentlyHooking(boardable)) {
			// Hook boardable
			c.execCommand(
					msg("tetherAt")
					.put("id", c.getShipID(ship))
					.put("target", c.getShipID(boardable)), AI_COMMAND);
		} else if (hasHook() && !currentlyHookingValidTarget() && nearestMovableTarget() != null && (!ship.hasCrewQuartersAny(CrewType.boarders) || nearestMovableTarget().boarders.isEmpty())) {
			// Hook for generic use.
			c.execCommand(
					msg("tetherAt")
					.put("id", c.getShipID(ship))
					.put("target", c.getShipID(nearestMovableTarget())), AI_COMMAND);
		}
	}
	
	private boolean has(HeroType.CombatAbility ab) {
		return !ship.usedAbilities.contains(ab) && ship.getCaptain() != null && ship.getCaptain().type.combatAbilities.contains(ab);
	}
	
	public static final int HIGH_STORM_Y = -100;
	
	private boolean fleetHas(HeroType.CombatAbility ab) {
		for (int i = 0; i < mySide.ships.size(); i++) {
			Airship s = mySide.ships.get(i);
			if (!s.usedAbilities.contains(ab) && s.getCaptain() != null && s.getCaptain().type.combatAbilities.contains(ab)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean hasHighStormAbility() {
		if (!fleetHas(HeroType.CombatAbility.HIGH_STORM)) { return false; }
		int myAvgServiceCeiling = 0;
		int myN = 0;
		for (int i = 0; i < mySide.ships.size(); i++) {
			Airship s = mySide.ships.get(i);
			if (s.type == ShipType.AIRSHIP) {
				myAvgServiceCeiling += s.serviceCeiling();
				myN++;
			}
		}
		if (myN > 0) { myAvgServiceCeiling /= myN; }
		int enemyAvgServiceCeiling = 0;
		int enemyN = 0;
		for (int i = 0; i < enemySide.ships.size(); i++) {
			Airship s = enemySide.ships.get(i);
			if (s.type == ShipType.AIRSHIP) {
				enemyAvgServiceCeiling += s.serviceCeiling();
				enemyN++;
			}
		}
		if (enemyN > 0) { enemyAvgServiceCeiling /= enemyN; }
		return myAvgServiceCeiling * 5 / 4 < enemyAvgServiceCeiling;
	}
	
	private int totalDamageTaken() {
		int dmg = 0;
		for (int i = 0; i < mySide.ships.size(); i++) {
			dmg += mySide.ships.get(i).damageTaken[1];
		}
		return dmg;
	}
	
	private int totalDamageMissed() {
		int dmg = 0;
		for (int i = 0; i < mySide.ships.size(); i++) {
			dmg += mySide.ships.get(i).damageMissed[1];
		}
		return dmg;
	}
	
	private double getDamagePotential(Airship e) {
		double damageQuality = 0;
		int emsz = e.modules.size();
		for (int emi = 0; emi < emsz; emi++) {
			Module em = e.modules.get(emi);
			// -ve inset means extra caution re enemy weapons range
			if (em.type.isWeapon() && em.hp > 0 ) {
				if (em.ammoLeft == 0 && !em.type.canResupplyInCombat(e.currentBonuses)) { continue; } // Ignore used-up single-use weapons.
				damageQuality += em.type.approxDPS(e.currentBonuses);
			}
			int tsz = em.tentacles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tentacle t = em.tentacles.get(ti);
				if (em.hp > 0) {
					damageQuality += t.spec.getDPSEquivalent();
				}
			}
		}
		return damageQuality;
	}
	
	private double getDamageQuality(Airship attacker, Airship defender, double ex, double ey, boolean flipped) {
		double x = defender.getX();
		double y = defender.getY();
		double shipW = defender.getBBWidth();
		double shipH = defender.getBBHeight();
		WeatherEffect we = c.timeOfDay.effect;
		double shootToLeftJitterMult = quality == AIQuality.SMART ? we.shootJitterMult * we.shootToLeftJitterMult : 1;
		double shootToRightJitterMult = quality == AIQuality.SMART ? we.shootJitterMult * we.shootToRightJitterMult : 1;
		int emsz = attacker.modules.size();
		double damageQuality = 0;
		double manhattanDist =
				StrictMath.min(StrictMath.abs(x - ex - attacker.getBBWidth()), StrictMath.abs(ex - x - shipW)) +
				StrictMath.min(StrictMath.abs(y - ex - attacker.getBBHeight()), StrictMath.abs(ey - y - shipH));
		boolean ltr = x - attacker.getX() > 0;
		if (ltr) {
			manhattanDist *= shootToRightJitterMult;
		} else {
			manhattanDist *= shootToLeftJitterMult;
		}
		for (int emi = 0; emi < emsz; emi++) {
			Module em = attacker.modules.get(emi);
			// -ve inset means extra caution re enemy weapons range
			if (em.type.isWeapon() && em.hp > 0 && em.canHit(defender, ex, ey, x, y, attacker.flipped ^ flipped, -INSET)) {
				if (em.ammoLeft == 0 && !em.type.canResupplyInCombat(attacker.currentBonuses)) { continue; } // Ignore used-up single-use weapons.
				damageQuality += StrictMath.min(1, em.type.getOptimumRange(attacker.currentBonuses) / manhattanDist) * em.type.approxDPS(attacker.currentBonuses);
			}
			int tsz = em.tentacles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tentacle t = em.tentacles.get(ti);
				if (em.hp > 0 && t.canTentacle(attacker, em, defender, ex, ey, x, y, attacker.flipped ^ flipped, -INSET, 1.05)) {
					damageQuality += t.spec.getDPSEquivalent();
				}
			}
		}
		return damageQuality;
	}
	
	private void move() {
		// Special abilities
		if (c.time > 0) { // 6000
			if (has(HeroType.CombatAbility.AERIAL_ACE)) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.AERIAL_ACE.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.AIR_SUPPORT)) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.AIR_SUPPORT.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.PERSONAL_GUARD)) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.PERSONAL_GUARD.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.HIGH_STORM)) {
				boolean myShipInDanger = false;
				int myAvgServiceCeiling = 0;
				int myN = 0;
				for (int i = 0; i < mySide.ships.size(); i++) {
					Airship s = mySide.ships.get(i);
					if (s.type == ShipType.AIRSHIP) {
						myAvgServiceCeiling += s.serviceCeiling();
						myN++;
						if (s.getY() < HIGH_STORM_Y) {
							myShipInDanger = true;
							break;
						}
					}
				}
				if (!myShipInDanger) {
					if (myN > 0) { myAvgServiceCeiling /= myN; }
					int enemyAvgServiceCeiling = 0;
					int enemyN = 0;
					for (int i = 0; i < enemySide.ships.size(); i++) {
						Airship s = enemySide.ships.get(i);
						if (s.type == ShipType.AIRSHIP) {
							enemyAvgServiceCeiling += s.serviceCeiling();
							enemyN++;
						}
					}
					if (enemyN > 0) { enemyAvgServiceCeiling /= enemyN; }
					if (myAvgServiceCeiling * 5 / 4 < enemyAvgServiceCeiling) {
						c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.HIGH_STORM.name()), AI_COMMAND);
						return;
					}
				}
			}
			if (has(HeroType.CombatAbility.EARTHQUAKE) && !c.landFormations.get(0).landscapeType.deepWater) {
				boolean iHaveGroundStuff = false;
				boolean enemyHasGroundStuff = false;
				for (int i = 0; i < mySide.ships.size(); i++) {
					if (mySide.ships.get(i).type.onGround) {
						iHaveGroundStuff = true;
					}
				}
				for (int i = 0; i < enemySide.ships.size(); i++) {
					if (enemySide.ships.get(i).type.onGround) {
						enemyHasGroundStuff = true;
					}
				}
				if (!iHaveGroundStuff && enemyHasGroundStuff) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.EARTHQUAKE.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.FEAR) && !ship.boarders.isEmpty()) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.FEAR.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.IMPROVISE_MUNITIONS) && ship.getTotalResource(Resource.AMMO) < ship.getAmmoCapacity() / 2) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.IMPROVISE_MUNITIONS.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.SCAVENGE_MATERIALS) && ship.getTotalResource(Resource.REPAIR) < ship.getRepairCapacity() / 2) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.SCAVENGE_MATERIALS.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.SCAVENGE_FUEL) && ship.getTotalResource(Resource.COAL) < ship.getCoalCapacity() / 2) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.SCAVENGE_FUEL.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.ENGINEERING_MIRACLE)) {
				int numImportantDestroyedModules = 0;
				int numImportantModules = 0;
				for (int i = 0; i < ship.modules.size(); i++) {
					Module m = ship.modules.get(i);
					if (m.type.isWeapon() || m.type.getPropulsion(ship.currentBonuses) > 0 || m.type.getLift(ship.currentBonuses) > 0 || m.type.getCoal(ship.currentBonuses) > 0 || m.type.getAmmo(ship.currentBonuses) > 0) {
						numImportantModules++;
						if (m.hp <= 0) {
							numImportantDestroyedModules++;
						}
					}
				}
				if (numImportantDestroyedModules * 5 > numImportantModules) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.ENGINEERING_MIRACLE.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.EXTINGUISH) && ship.fireAmount() > 40) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.EXTINGUISH.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.NECROMANTIC_INCANTATION)) {
				int deadCrew = 0;
				for (int i = 0; i < ship.crew.size(); i++) {
					if (!ship.crew.get(i).alive()) { deadCrew++; }
				}
				if (deadCrew * 5 > ship.crew.size()) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.NECROMANTIC_INCANTATION.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.SMOKESCREEN) && ship.damageTaken[1] * 3 > totalDamageTaken()) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.SMOKESCREEN.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.TAUNT) && ship.damageTaken[1] * 8 < totalDamageTaken()) {
				int canBeShotBy = 0;
				int canShoot = 0;
				for (int ei = 0; ei < enemySide.ships.size(); ei++) {
					Airship e = enemySide.ships.get(ei);
					double damageQuality = getDamageQuality(e, ship, e.getX(), e.getY(), false);
					if (damageQuality > 0) {
						canBeShotBy++;
					}
					double potential = getDamagePotential(e);
					if (potential > 0) {
						canShoot++;
					}
				}
				if (canBeShotBy * 1.3 > canShoot) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.TAUNT.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.SUPERCHARGE_SUSPENDIUM) && ship.damageTakenFromAbove[1] * 2 > ship.damageTaken[1] && !ship.type.onGround) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.SUPERCHARGE_SUSPENDIUM.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.HYSTERICAL_BLINDNESS) && totalDamageTaken() > totalDamageMissed()) {
				c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.HYSTERICAL_BLINDNESS.name()), AI_COMMAND);
				return;
			}
			if (has(HeroType.CombatAbility.DOUBLE_TIME) || has(HeroType.CombatAbility.LAST_STAND)) {
				int effectiveWeapons = 0;
				int totalWeapons = 0;
				for (int i = 0; i < ship.modules.size(); i++) {
					Module m = ship.modules.get(i);
					if (m.hp > 0 && m.type.isWeapon()) {
						totalWeapons++;
						if (m.damageDealt[1] > m.damageNotDealt[1] * 2) {
							effectiveWeapons++;
						}
					}
				}
				if (effectiveWeapons > totalWeapons * 2 / 3) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", has(HeroType.CombatAbility.DOUBLE_TIME) ? HeroType.CombatAbility.DOUBLE_TIME.name() : HeroType.CombatAbility.LAST_STAND.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.BLINDING_GLIMMER) || has(HeroType.CombatAbility.DISARMING_SHOT)) {
				if (totalDamageTaken() > 2 * totalDamageMissed()) {
					Airship bestShip = null;
					double bestDamageQuality = 0;
					for (int ei = 0; ei < enemySide.ships.size(); ei++) {
						Airship e = enemySide.ships.get(ei);
						double damageQuality = getDamageQuality(e, ship, e.getX(), e.getY(), false);
						if (damageQuality > bestDamageQuality) {
							bestShip = e;
							bestDamageQuality = damageQuality;
						}
					}
					if (bestShip != null) {
						c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", has(HeroType.CombatAbility.BLINDING_GLIMMER) ? HeroType.CombatAbility.BLINDING_GLIMMER.name() : HeroType.CombatAbility.DISARMING_SHOT.name()).put("target", c.getShipID(bestShip)), AI_COMMAND);
						return;
					}
				}
			}
			if (has(HeroType.CombatAbility.FLANK)) {
				int boardXDistMax = boardXDistMax();
				boolean wantsToBoard = wantsToBoard();
				boolean isShortRange = hasShortRangeWeapons();
				int bestFlankY = 0;
				double bestFlankYQuality = -1;
				int flankX = c.sides.indexOf(mySide) == 0 ? c.combatAreaW() / 2 - (int) ship.getBBWidth() : -c.combatAreaW() / 2;
				boolean flipped = c.sides.indexOf(mySide) == 0;
				int asc = ship.availableServiceCeiling(null);
				for (int y = AGame.GROUND_LEVEL - asc; y < c.landFormations.get(0).getY() - ship.getBBHeight() - 1; y += 50) {
					double q = calculateNormally(flankX, y, flipped, asc, wantsToBoard, isShortRange, AGame.GROUND_LEVEL, boardXDistMax, hasHighStormAbility(), true);
					if (q > bestFlankYQuality) {
						bestFlankYQuality = q;
						bestFlankY = y;
					}
				}
				if (bestFlankYQuality > 0) {
					boolean hasBetterOnGrid = false;
					lp: for (int y = 0; y < grid.length; y++) { for (int x = 0; x < grid[y].length; x++) {
						if (grid[y][x][0] > bestFlankYQuality || grid[y][x][1] > bestFlankYQuality) {
							hasBetterOnGrid = true;
							break lp;
						}
					}}
					if (!hasBetterOnGrid) {
						c.giveCommand(msg("placeShip")
								.put("side", c.sides.indexOf(mySide))
								.put("ship", ship.networkID)
								.put("x", flankX)
								.put("y", bestFlankY)
								.put("flipped", flipped)
								.put("flanking", true)
						);
						return;
					}
				}
			}
			if (has(HeroType.CombatAbility.CROSSWINDS) || has(HeroType.CombatAbility.PARALYSIS) || has(HeroType.CombatAbility.CRIPPLING_SHOT)) {
				HeroType.CombatAbility ab = 
						has(HeroType.CombatAbility.CROSSWINDS) ? HeroType.CombatAbility.CROSSWINDS
						: has(HeroType.CombatAbility.PARALYSIS) ? HeroType.CombatAbility.PARALYSIS
						: HeroType.CombatAbility.CRIPPLING_SHOT;
				Airship bestVictim = null;
				double bestDamagePotential = 0;
				for (int i = 0; i < enemySide.ships.size(); i++) {
					Airship e = enemySide.ships.get(i);
					if (ab == HeroType.CombatAbility.CRIPPLING_SHOT && e.type != ShipType.AIRSHIP) { continue; }
					if (e.type == ShipType.BUILDING) { continue; }
					double potential = getDamagePotential(e);
					double q = getDamageQuality(e, ship, e.getX(), e.getY(), false);
					//System.out.println(e.getName() + " " + q + " vs potential " + potential);
					if (q < potential * 0.25 && potential > bestDamagePotential) {
						bestVictim = e;
						bestDamagePotential = potential;
					}
				}
				if (bestVictim != null) {
					//System.out.println("do " + ab.name());
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", ab.name()).put("target", c.getShipID(bestVictim)), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.TURNABOUT)) {
				Airship bestVictim = null;
				double bestDamagePotential = 0;
				for (int i = 0; i < enemySide.ships.size(); i++) {
					Airship e = enemySide.ships.get(i);
					if (e.type != ShipType.AIRSHIP) { continue; }
					boolean canMove = e.commandPointsGenerated() > 0 && e.canMove();
					if (c.time < 30000 && canMove) { continue; }
					double potential = getDamagePotential(e);
					double q = getDamageQuality(e, ship, e.getX(), e.getY(), false);
					double fq = getDamageQuality(e, ship, e.getX(), e.getY(), true);
					int mult = canMove ? 4 : 2;
					if (fq * mult < q && potential > bestDamagePotential) {
						bestVictim = e;
						bestDamagePotential = potential;
					}
				}
				if (bestVictim != null) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.TURNABOUT.name()).put("target", c.getShipID(bestVictim)), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.GUST_OF_WIND)) {
				Airship bestVictim = null;
				double bestDamagePotential = 0;
				for (int i = 0; i < enemySide.ships.size(); i++) {
					Airship e = enemySide.ships.get(i);
					if (e.type != ShipType.AIRSHIP) { continue; }
					double dist = StrictMath.sqrt((ship.getX() - e.getX()) * (ship.getX() - e.getX()) + (ship.getY() - e.getY()) * (ship.getY() - e.getY()));
					double newX = StrictMath.max(-c.combatAreaW() / 2, Math.min(c.combatAreaW() / 2 - e.getBBWidth(), e.getX() + (e.getX() - ship.getX()) / dist * 800));
					double newY = StrictMath.max(e.availableServiceCeiling(c), Math.min(AGame.GROUND_LEVEL - e.getBBHeight(), e.getY() + (e.getY() - ship.getY()) / dist * 800));
					double potential = getDamagePotential(e);
					double q = getDamageQuality(e, ship, e.getX(), e.getY(), false);
					double movedQ = getDamageQuality(e, ship, newX, newY, false);
					if (movedQ * 2 < q && potential > bestDamagePotential) {
						bestVictim = e;
						bestDamagePotential = potential;
					}
				}
				if (bestVictim != null) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.GUST_OF_WIND.name()).put("target", c.getShipID(bestVictim)), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.SUDDEN_STORM)) {
				double currentDamageQuality = 0;
				double movedDamageQuality = 0;
				for (int i = 0; i < enemySide.ships.size(); i++) {
					Airship e = enemySide.ships.get(i);
					if (e.type != ShipType.AIRSHIP) { continue; }
					double newX = StrictMath.max(-c.combatAreaW() / 2, Math.min(c.combatAreaW() / 2 - e.getBBWidth(), e.getX() + c.sides.indexOf(mySide) == 0 ? 1000 : -1000));
					currentDamageQuality += getDamageQuality(e, ship, e.getX(), e.getY(), false);
					movedDamageQuality += getDamageQuality(e, ship, newX, e.getY(), false);
				}
				double myCurrentDamageQuality = 0;
				double myMovedDamageQuality = 0;
				for (int i = 0; i < enemySide.ships.size(); i++) {
					Airship e = enemySide.ships.get(i);
					if (e.type != ShipType.AIRSHIP) { continue; }
					double myNewX = StrictMath.max(-c.combatAreaW() / 2, Math.min(c.combatAreaW() / 2 - ship.getBBWidth(), ship.getX() + c.sides.indexOf(mySide) == 0 ? -1000 : 1000));
					myCurrentDamageQuality += getDamageQuality(ship, e, ship.getX(), ship.getY(), false);
					myMovedDamageQuality += getDamageQuality(ship, e, myNewX, ship.getY(), false);
				}
				if (movedDamageQuality * 2 < currentDamageQuality && myMovedDamageQuality * 1.5 > myCurrentDamageQuality) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.SUDDEN_STORM.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.MOMENT_OF_DOUBT)) {
				double totalDamagePotential = 0;
				double currentDamageQuality = 0;
				for (int i = 0; i < enemySide.ships.size(); i++) {
					Airship e = enemySide.ships.get(i);
					if (e.type == ShipType.BUILDING) { continue; }
					totalDamagePotential += getDamagePotential(e);
					currentDamageQuality += getDamageQuality(e, ship, e.getX(), e.getY(), false);
				}
				if (currentDamageQuality * 4 < totalDamagePotential) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.MOMENT_OF_DOUBT.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.EMERGENCY_ORDERS)) {
				boolean allCooldown = true;
				for (int i = 0; i < mySide.ships.size(); i++) {
					Airship s = mySide.ships.get(i);
					if (s.readyForCommand()) { allCooldown = false; break; }
				}
				if (allCooldown) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.EMERGENCY_ORDERS.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.BURST_OF_SPEED) && ship.moveTo != null && StrictMath.abs(ship.moveTo.x - ship.getX()) + StrictMath.abs(ship.moveTo.y - ship.getY()) > 600) {
				int boardXDistMax = boardXDistMax();
				boolean wantsToBoard = wantsToBoard();
				boolean isShortRange = hasShortRangeWeapons();
				boolean hasHighStormAbility = hasHighStormAbility();
				int asc = ship.availableServiceCeiling(null);
				double currentQuality = calculateNormally(ship.getX(), ship.getY(), ship.flipped, asc, wantsToBoard, isShortRange, AGame.GROUND_LEVEL, boardXDistMax, hasHighStormAbility, true);
				double moveToQuality = calculateNormally(ship.moveTo.x, ship.moveTo.y, ship.flipTo, asc, wantsToBoard, isShortRange, AGame.GROUND_LEVEL, boardXDistMax, hasHighStormAbility, true);
				if (currentQuality * 4 < moveToQuality) {
					c.execCommand(msg("doAbility").put("id", c.getShipID(ship)).put("name", HeroType.CombatAbility.BURST_OF_SPEED.name()), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.SINKHOLE) && !c.landFormations.get(0).landscapeType.deepWater) {
				int bestSinkX = 0;
				double bestSinkQuality = -100;
				lp: for (int x = -c.combatAreaW() / 2 + SinkholeTool.SINKHOLE_RANGE; x < c.combatAreaW() / 2 - SinkholeTool.SINKHOLE_RANGE; x += 80) {
					for (int i = 0; i < mySide.ships.size(); i++) {
						Airship s = mySide.ships.get(i);
						if (s.type == ShipType.AIRSHIP) { continue; }
						if (s.getX() < x + SinkholeTool.SINKHOLE_RANGE && s.getX() + s.getBBWidth() > x - SinkholeTool.SINKHOLE_RANGE) {
							continue lp;
						}
					}
					for (int i = 0; i < enemySide.ships.size(); i++) {
						Airship e = enemySide.ships.get(i);
						if (e.type == ShipType.AIRSHIP) { continue; }
						if (!e.inCombat(c)) { continue; }
						if (e.getX() > x - SinkholeTool.SINKHOLE_RANGE && e.getX() + e.getBBWidth() < x + SinkholeTool.SINKHOLE_RANGE) {
							double danger = e.danger(c);
							if (e.type == ShipType.LANDSHIP) {
								danger /= 8;
							}
							if (danger > bestSinkQuality) {
								bestSinkQuality = danger;
								bestSinkX = x;
							}
							continue lp;
						}
					}
				}
				if (bestSinkQuality > -100) {
					c.execCommand(msg("sinkhole").put("id", c.getShipID(ship)).put("x", bestSinkX), AI_COMMAND);
					return;
				}
			}
			if (has(HeroType.CombatAbility.CRASH_ZONE)) {
				int bestCrashX = 0;
				double bestCrashQuality = -100;
				lp: for (int x = -c.combatAreaW() / 2 + Combat.CRASH_ZONE_W / 2; x < c.combatAreaW() / 2 - Combat.CRASH_ZONE_W / 2; x += 80) {
					double crashQuality = 0;
					for (int i = 0; i < mySide.ships.size(); i++) {
						Airship s = mySide.ships.get(i);
						if (s.type != ShipType.AIRSHIP) { continue; }
						double minX = StrictMath.min(s.getX(), s.moveTo.x);
						double maxX = StrictMath.max(s.getX(), s.moveTo.x) + ship.getBBWidth();
						if (minX < x + Combat.CRASH_ZONE_W / 2 && maxX > x - Combat.CRASH_ZONE_W / 2) {
							continue lp;
						}
					}
					for (int i = 0; i < enemySide.ships.size(); i++) {
						Airship e = enemySide.ships.get(i);
						if (e.type != ShipType.AIRSHIP) { continue; }
						if (!e.inCombat(c)) { continue; }
						if (e.getY() + e.getBBHeight() > AGame.GROUND_LEVEL - 200) { continue; }
						double adjX = e.getX() + e.getBBWidth() / 2;
						if (e.moveTo.x < e.getX() - 200) {
							adjX -= 130;
						}
						if (e.moveTo.x > e.getX() + 200) {
							adjX += 130;
						}
						double xDist = StrictMath.abs(adjX - x);
						if (xDist <= Combat.CRASH_ZONE_W / 2) {
							double damagePotential = getDamagePotential(e);
							double damageQuality = getDamageQuality(e, ship, e.getX(), e.getY(), e.flipped);
							if (damageQuality * 3 < damagePotential) {
								double adjPotential = (1 - 0.5 * xDist / (Combat.CRASH_ZONE_W / 2)) * damagePotential;
								crashQuality += adjPotential;
							}
						}
					}
					if (crashQuality > 0 && crashQuality > bestCrashQuality) {
						bestCrashQuality = crashQuality;
						bestCrashX = x;
					}
				}
				if (bestCrashQuality > 0) {
					c.execCommand(msg("crashZone").put("id", c.getShipID(ship)).put("x", bestCrashX), AI_COMMAND);
					return;
				}
			}
		}
		
		if (!ship.readyForCommand()) { return; }
		
		Airship boardable = getBoardable();
		FireMode bfm = getBestFireMode();
		Pt vrp = checkForVerticalRamPossibility();
		ticksUntilCommand = 7 + ship.tiles.size() / 80;
		if (vrp != null) {
			//System.out.println("Vertiramming!");
			c.execCommand(
					msg("ram")
					.put("id", c.getShipID(ship))
					.put("x", vrp.x)
					.put("y", vrp.y)
					.put("flipTo", ship.flipped), AI_COMMAND);
		} else if (!ship.releaseOneUseWeapons && ship.hasOneUseWeapons()) {
			c.execCommand(msg("releaseOneUse")
					.put("id", c.getShipID(ship))
					.put("what", "weapons"), AI_COMMAND);
		} else if (!ship.releaseOneUseLift && ship.hasOneUseLift()) {
			c.execCommand(msg("releaseOneUse")
					.put("id", c.getShipID(ship))
					.put("what", "lift"), AI_COMMAND);
		} else if (!ship.releaseOneUsePropulsion && ship.hasOneUsePropulsion()) {
			c.execCommand(msg("releaseOneUse")
					.put("id", c.getShipID(ship))
					.put("what", "propulsion"), AI_COMMAND);
		} else if (ship.fireMode != bfm) {
			c.execCommand(msg("fireMode")
					.put("id", c.getShipID(ship))
					.put("value", bfm.name()), AI_COMMAND);
			//System.out.println("fm");
		} else if (currentlyHookingHasBoarders()) {
			c.execCommand(msg("cutOwnTethers")
				.put("id", c.getShipID(ship)), AI_COMMAND);
		} else if (boardable != null && ship.board == null && lastBoardTarget != boardable) {
			c.execCommand(msg("board")
				.put("id", c.getShipID(ship))
				.put("target", c.getShipID(boardable)), AI_COMMAND);
			//System.out.println("brd");
			lastBoardTarget = boardable;
		} else if (hasHook() && boardable != null && !currentlyHooking(boardable)) {
			// Hook boardable
			c.execCommand(
					msg("tetherAt")
					.put("id", c.getShipID(ship))
					.put("target", c.getShipID(boardable)), AI_COMMAND);
		} else if (hasHook() && !currentlyHookingValidTarget() && nearestMovableTarget() != null && (!ship.hasCrewQuartersAny(CrewType.boarders) || nearestMovableTarget().boarders.isEmpty())) {
			// Hook for generic use.
			c.execCommand(
					msg("tetherAt")
					.put("id", c.getShipID(ship))
					.put("target", c.getShipID(nearestMovableTarget())), AI_COMMAND);
		} else if (wantsToRam() && canRam()) {
			double x = StrictMath.min(c.combatAreaW() / 2 - ship.getBBWidth(), StrictMath.max(-c.combatAreaW() / 2, ship.getX() + (ship.flipped ? -1500 : 1500)));
			c.execCommand(
					msg("ram")
					.put("id", c.getShipID(ship))
					.put("x", x)
					.put("y", ship.getY())
					.put("flipTo", ship.flipped), AI_COMMAND);
			ticksUntilCommand = (int) StrictMath.abs(StrictMath.ceil((x - ship.getX() / ship.availableSpeed(/* requireLegsAndWheelsTouchingGround */ true)) * (ship.type.onGround ? 4 : 2.4))) / 16 + 8;
			//System.out.println("ram time until command " + (ticksUntilCommand * 16) + " ms");
			//System.out.println("ram");
		} else {
			MoveTo mt = choose();
			if (mt != null) {
				if (StrictMath.abs(ship.moveTo.x - mt.getMoveToPt().x) > 16 || StrictMath.abs(ship.moveTo.y - mt.getMoveToPt().y) > 16 || mt.flipped != ship.flipped) {
					//System.out.println("mt");
					c.execCommand(msg("moveTo").put("id", c.getShipID(ship)).put("x", mt.getMoveToPt().x).put("y", mt.getMoveToPt().y).put("flipTo", mt.flipped), AI_COMMAND);
					ticksUntilCommand = gridVSize * 3 + 1;
				} else {
					ticksUntilCommand = 12;
				}
			}/* else {
				System.out.println("nmt-stuck?");
			}*/
		}
	}
	
	public boolean hasHook() {
		int msz = ship.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			if (m.type.getTetherSpec(ship.currentBonuses) != null) {
				return true;
			}
		}
		return false;
	}

	public boolean currentlyHookingValidTarget() {
		return ship.tetherAt != null && ship.tetherAt.inCombat(c);
	}

	public boolean currentlyHooking(Airship target) {
		return ship.tetherAt == target;
	}
	
	public boolean currentlyHookingHasBoarders() {
		return ship.tetherAt != null && !ship.tetherAt.boarders.isEmpty();
	}

	public Airship nearestMovableTarget() {
		// We may wanna make this smarter and take into account something about where it is?
		double bestD2 = 0;
		Airship best = null;
		for (Airship as : enemySide.ships) {
			if (!as.type.mobile) { continue; }
			if (!as.inCombat(c)) { continue; }
			double d2 =
					(ship.getX() + ship.getBBWidth() / 2 - as.getX() - as.getBBWidth() / 2) * (ship.getX() + ship.getBBWidth() / 2 - as.getX() - as.getBBWidth() / 2) +
					(ship.getY() + ship.getBBHeight()/ 2 - as.getY() - as.getBBHeight() / 2) * (ship.getY() + ship.getBBHeight() / 2 - as.getY() - as.getBBHeight() / 2);
			if (best == null || d2 < bestD2) {
				best = as;
				bestD2 = d2;
			}
		}
		return best;
	}
	
	private Pt checkForVerticalRamPossibility() {
		if (quality != AIQuality.SMART) { return null; }
		// Reqs:
		// - ramming-ready keel and enemy ship between 50 and 200 units below
		// - or: position right on top of enemy has good damage give / take ratio
		// always: we weigh at least 75% of the target
		// always: no friendlies in the way, no other ships in-between
		// if so: specify airship. will put in course straight down so as to trap enemy, but not quite so as to smack into ground.
		
		int ssz = mySide.ships.size();
		for (int si = 0; si < ssz; si++) {
			Airship s = mySide.ships.get(si);
			if (s == ship) { continue; }
			if (s.getY() > ship.getY() && s.getX() < ship.getX() + ship.getBBWidth() && s.getX() + s.getBBWidth() > ship.getX()) {
				//System.out.println("Interference in vertiRam!");
				return null;
			}
		}
		
		boolean hasSmashZone = false;
		int smashZoneStart = 0;
		int smashZoneWidth = 0;
		boolean hasSoftZone = false;
		int softZoneStart = 0;
		int softZoneWidth = 0;
		int msz = ship.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			if (m.y + m.type.getH() != ship.getHeight()) { continue; }
			if (ModuleType.keelsAndRams.contains(m.type)) {
				hasSmashZone = true;
				smashZoneStart = m.x;
				smashZoneWidth = m.type.getW();
			} else {
				if (!hasSoftZone) {
					hasSoftZone = true;
					softZoneStart = m.x;
					softZoneWidth = m.type.getW();
				} else {
					int softZoneEnd = softZoneStart + softZoneWidth;
					softZoneStart = StrictMath.min(m.x, softZoneStart);
					softZoneEnd = StrictMath.max(softZoneEnd, m.x + m.type.getW());
					softZoneWidth = softZoneEnd - softZoneStart;
				}
			}
		}
		
		if (!ship.canShoot()) {
			hasSoftZone = false;
		}
		
		double softStart = ship.getX() + (ship.flipped ? ship.getWidth() - softZoneStart - softZoneWidth : softZoneStart) * AGame.SGS;
		double softEnd = ship.getX() + (ship.flipped ? ship.getWidth() - softZoneStart : (softZoneStart + softZoneWidth)) * AGame.SGS;
		double smashStart = ship.getX() + (ship.flipped ? ship.getWidth() - smashZoneStart - smashZoneWidth : smashZoneStart) * AGame.SGS + 5;
		double smashEnd = ship.getX() + (ship.flipped ? ship.getWidth() - smashZoneStart : (smashZoneStart + smashZoneWidth)) * AGame.SGS - 5;
		
		int tsz = enemySide.ships.size();
		for (int ti = 0; ti < tsz; ti++) {
			Airship target = enemySide.ships.get(ti);
			if (target.getY() < ship.getY() + ship.getBBHeight() + 50) {
				continue;
			}
			if (hasSoftZone && target.getX() < softEnd && target.getX() + target.getBBWidth() > softStart) {
				continue;
			}
			if (hasSmashZone && target.getX() < smashEnd && target.getX() + target.getBBWidth() > smashStart) {
				//System.out.println(ship.name + " could smash " + target.name + "!");
				return new Pt(ship.getX(), AGame.GROUND_LEVEL - ship.getBBHeight() - 10); // qqDPS be cleverer re this.
			}
		}
		return null;
	}
	
	private FireMode getBestFireMode() {
		if (quality != AIQuality.SMART) { return ship.fireMode; }
		if (ticksUntilNextFireModeCheck-- > 0) {
			return ship.fireMode;
		} else {
			ticksUntilNextFireModeCheck = 120;
		}
		WeatherEffect we = c.timeOfDay.effect;
		
		double shootToLeftJitterMult = we.shootJitterMult * we.shootToLeftJitterMult;
		double shootToRightJitterMult = we.shootJitterMult * we.shootToRightJitterMult;
		// Go over each weapon. Find a target for it. If no target can be found, ignore it.
		int rapidVotes = 0;
		int normalVotes = 0;
		int aimedVotes = 0;
		int msz = ship.modules.size();
		int esz = enemySide.ships.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			if (!m.type.isWeapon() || !m.canRun()) { continue; }
			double shortestDist = -100;
			for (int ei = 0; ei < esz; ei++) {
				Airship e = enemySide.ships.get(ei);
				if (m.canHit(e, ship.getX(), ship.getY(), e.getX(), e.getY(), ship.flipped, INSET)) {
					double manhattanDist =
							StrictMath.min(StrictMath.abs(ship.getX() - e.getX() - e.getBBWidth()), StrictMath.abs(e.getX() - ship.getX() - ship.getBBWidth())) +
							StrictMath.min(StrictMath.abs(ship.getY() - e.getY() - e.getBBHeight()), StrictMath.abs(e.getY() - ship.getY() - ship.getBBHeight()));
					boolean ltr = e.getX() - ship.getX() > 0;
					if (ltr) {
						manhattanDist *= shootToRightJitterMult;
					} else {
						manhattanDist *= shootToLeftJitterMult;
					}
					if (shortestDist < -1 || manhattanDist < shortestDist) {
						shortestDist = manhattanDist;
					}
				}
			}
			if (shortestDist < -1) { continue; }
			int optDist = m.type.getOptimumRange(ship.currentBonuses);
			if (shortestDist >= optDist * 2) {
				aimedVotes++;
			} else if (shortestDist <= optDist / 3) {
				rapidVotes++;
			} else {
				normalVotes++;
			}
		}
		switch (ship.fireMode) {
			case AIMED:
				aimedVotes *= 2;
				break;
			case NORMAL:
				normalVotes *= 2;
				break;
			case RAPID:
				rapidVotes *= 2;
				break;
		}
		if (aimedVotes > normalVotes && aimedVotes > rapidVotes) {
			return FireMode.AIMED;
		}
		if (rapidVotes > normalVotes && rapidVotes > aimedVotes) {
			return FireMode.RAPID;
		}
		return FireMode.NORMAL;
	}
	
	public static final int TOO_HIGH = 50, TOO_HIGH_CMD = 50;
	public static final int TOO_LOW = -40, TOO_LOW_CMD = -50;
	
	public double distanceToGround(Airship s) {
		return s.getY() - c.landFormations.get(0).getVerticalPosition(s, (int) s.getX(), s.flipped, /* ignoresoftthings*/ false);
	}
	
	public int boardXDistMax() {
		int dist = 100;
		int tsz = mySide.troops.size();
		for (int ti = 0; ti < tsz; ti++) {
			Crewman cm = mySide.troops.get(ti);
			if (cm.attachedTo == ship && cm.type.canBoard) {
				dist = StrictMath.min(dist, cm.type.assumedJumpDist);
			}
		}
		return dist * 4 / 5;
	}
	
	public Airship getBoardable() {
		ArrayList<CrewType> cts = ship.getBoarderTypes();
		int tsz = mySide.troops.size();
		for (int ti = 0; ti < tsz; ti++) {
			Crewman cm = mySide.troops.get(ti);
			if (cm.attachedTo == ship && cm.alive() && !cts.contains(cm.type)) {
				cts.add(cm.type);
			}
		}
		if (cts.isEmpty()) { return null; }
		int ssz = enemySide.ships.size();
		int ctss = cts.size();
		for (int ctsi = 0; ctsi < ctss; ctsi++) {
			CrewType ct = cts.get(ctsi);
			if (ct.hasHook) {
				shipLoop: for (int si = 0; si < ssz; si++) {
					Airship s = enemySide.ships.get(si);
					if (!s.inCombat(c) || !s.canBeBoarded()) { continue; }
					int bsz = s.boarders.size();
					for (int bi = 0; bi < bsz; bi++) {
						if (s.boarders.get(bi).alive()) {
							continue shipLoop;
						}
					}
					// Manhattan distance.
					double dx = StrictMath.max(0, StrictMath.min(StrictMath.abs(ship.getX() - s.getX() - s.getBBWidth()), StrictMath.abs(s.getX() - ship.getX() - ship.getBBWidth())));
					double dy = StrictMath.max(0, StrictMath.min(StrictMath.abs(ship.getY() - s.getY() - s.getBBHeight()), StrictMath.abs(s.getY() - ship.getY() - ship.getBBHeight())));
					if (dx + dy < ct.hookRopeLength) {
						return s;
					}
					// Alternatively, if we can reach the ground and so can they, this also works.
					if (distanceToGround(ship) < Crewman.MAX_JUMP_DOWN && distanceToGround(s) < ct.hookRopeLength * 0.7) {
						return s;
					}
				}
			} else {
				for (int si = 0; si < ssz; si++) {
					Airship s = enemySide.ships.get(si);
					if (!s.inCombat(c) || !s.canBeBoarded()) { continue; }
					boolean yep =
							s.getX() - (ship.getX() + ship.getBBWidth()) <= ct.assumedJumpDist &&
							ship.getX() - (s.getX() + s.getBBWidth()) <= ct.assumedJumpDist &&
							s.getY() - (ship.getY() + ship.getBBHeight()) <= TOO_HIGH_CMD &&
							ship.getY() - (s.getY() + s.getBBHeight()) <= TOO_LOW_CMD;
					if (yep) { return s; }
					// Alternatively, if we can reach the ground and so can they, this also works.
					if (distanceToGround(ship) < Crewman.MAX_JUMP_DOWN && distanceToGround(s) < 3) {
						return s;
					}
				}
			}
			
		}
		return null;
	}
	
	public double getWorldYFromGridY(int gridX, int gridY, boolean flipped) {
		if (ship.type.onGround) {
			return c.landFormations.get(0).getVerticalPosition(ship, (int) gridXAnchor() + gridX * X_GRID_STEP, flipped, /* ignoreSoftThings */true) - ship.getCachedGroundOffset();
		} else {
			return gridYAnchor + gridY * Y_GRID_STEP;
		}
	}
	
	public final strictfp class  MoveTo {
		public final int gridX, gridY;
		public final boolean flipped;

		public MoveTo(int gridX, int gridY, boolean flipped) {
			this.gridX = gridX;
			this.gridY = gridY;
			this.flipped = flipped;
		}
		
		public Pt getMoveToPt() {
			return new Pt(gridXAnchor() + gridX * X_GRID_STEP, getWorldYFromGridY(gridX, gridY, flipped));
		}
	}
	
	public void calculate() {
		boolean wantsToRam = wantsToRam();
		boolean wantsToBoard = wantsToBoard();
		boolean isShortRange = hasShortRangeWeapons();
		boolean hasHighStormAbility = hasHighStormAbility();
		int aiMaxY = aiMaxY();
		int boardXDistMax = boardXDistMax();
		for (int gridY = 0; gridY < grid.length; gridY++) { 
			calculateRow(gridY, wantsToRam, wantsToBoard, wantsToBoard || isShortRange, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility);
		}
	}
	
	private boolean isNearEnemy(int x, int y) {
		int ssz = enemySide.ships.size();
		int delta = 300;
		for (int si = 0; si < ssz; si++) {
			Airship en = enemySide.ships.get(si);
			//System.out.println((en.x - delta) + " " + (en.y - delta) + " " + (en.getBBWidth() + delta * 2) + " " + (en.getBBHeight() + delta * 2) + " " + x + " " + y + " " + ship.getBBWidth() + " " + ship.getBBHeight());
			if (Rect2D.intersects(en.getX() - delta, en.getY() - delta, en.getBBWidth() + delta * 2, en.getBBHeight() + delta * 2, x, y, ship.getBBWidth(), ship.getBBHeight())) {
				return true;
			}
		}
		return false;
	}
	
	private void calculateRow(int gridY, boolean wantsToRam, boolean wantsToBoard, boolean doShortRangeGrid, boolean isShortRange, int aiMaxY, int boardXDistMax, boolean hasHighStormAbility) {
		int asc = ship.availableServiceCeiling(null);
		for (int gridX = 0; gridX < grid[0].length; gridX++) {
			int x = gridXAnchor() + gridX * X_GRID_STEP;
			int y = (int) getWorldYFromGridY(gridX, gridY, false);
			boolean shortRangeNearEnemy = 
					gridX % 5 == 0 && gridY % 4 == 0
					? true
					: doShortRangeGrid && isNearEnemy(x, y);
			boolean lastRowBeforeServiceCeiling = y >= AGame.GROUND_LEVEL - asc && y - Y_GRID_STEP < AGame.GROUND_LEVEL - asc;
			if (
					(gridX % 5 == 0 || shortRangeNearEnemy) &&
					(gridY % 4 == 0 || wantsToRam || shortRangeNearEnemy || lastRowBeforeServiceCeiling)
			) {
				double result = calculate(x, y, false, asc, wantsToRam, wantsToBoard, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility);
				grid[gridY][gridX][0] = result;
				y = (int) getWorldYFromGridY(gridX, gridY, true);
				result = calculate(x, y, true, asc, wantsToRam, wantsToBoard, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility);
				grid[gridY][gridX][1] = result;
			} else {
				grid[gridY][gridX][0] = NO;
				grid[gridY][gridX][1] = NO;
			}
		}
	}
	
	private void updateRamZones() {
		for (int gridY = 0; gridY < ramZones.length; gridY++) {
			for (int gridX = 0; gridX < ramZones[0].length; gridX++) {
				ramZones[gridY][gridX] = false;
			}
		}
		int esz = enemySide.ships.size();
		for (int ei = 0; ei < esz; ei++) {
			Airship enemy = enemySide.ships.get(ei);
			int msz = enemy.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = enemy.modules.get(mi);
				if (m.type.isRam()) {
					double startY = enemy.getY() + m.y * AGame.SGS;
					double endY = enemy.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS;
					double startX = enemy.getX() + enemy.getBBWidth() / 2;
					int startGridY = (int) StrictMath.max(0, StrictMath.floor((startY - gridYAnchor) / Y_GRID_STEP));
					int endGridY = (int) StrictMath.min(grid.length - 1, StrictMath.ceil((endY - gridYAnchor) / Y_GRID_STEP));
					int startGridX = (int) StrictMath.max(0, StrictMath.min(grid[0].length - 1, (startX - gridXAnchor()) / X_GRID_STEP));
					if (enemy.flipped) {
						for (int gy = startGridY; gy <= endGridY; gy++) {
							for (int gx = startGridX; gx >= 0; gx--) {
								ramZones[gy][gx] = true;
							}
						}
					} else {
						for (int gy = startGridY; gy <= endGridY; gy++) {
							for (int gx = startGridX; gx < grid[0].length; gx++) {
								ramZones[gy][gx] = true;
							}
						}
					}
				}
			}
		}
	}
	
	private boolean inRamZone(int gx, int gy) {
		int myGW = (int) StrictMath.ceil(ship.getBBWidth() / X_GRID_STEP);
		int myGH = (int) StrictMath.ceil(ship.getBBHeight() / Y_GRID_STEP);
		for (int yy = gy; yy < gy + myGH; yy++) { for (int xx = gx; xx < gx + myGW; xx++) {
			if (yy >= 0 && yy < ramZones.length && xx >= 0 && xx < ramZones[0].length && ramZones[yy][xx]) {
				return true;
			}
		}}
		return false;
	}
	
	public MoveTo choose() {
		boolean careAboutRamZones = quality == AIQuality.SMART && !ship.hasRam();
		if (careAboutRamZones) {
			updateRamZones();
		}
		int myGY = (int) StrictMath.max(0, StrictMath.floor((ship.getY() - gridYAnchor) / Y_GRID_STEP));
		int myGX = (int) StrictMath.max(0, StrictMath.min(grid[0].length - 1, (ship.getX() - gridXAnchor()) / X_GRID_STEP));
		boolean currentlyInRamZone = false;
		if (careAboutRamZones) {
			if (inRamZone(myGX, myGY)) {
				currentlyInRamZone = true;
			}
		}
		double best = NO;
		int bestY = 0;
		int bestX = 0;
		boolean bestFlipped = false;
		for (int gridY = 0; gridY < grid.length; gridY++) { for (int gridX = 0; gridX < grid[0].length; gridX++) {
			double q = grid[gridY][gridX][0];
			if (q != NO) {
				q += (!ship.flipped ? 0.001 : 0);
				if (currentlyInRamZone && StrictMath.abs(gridX - myGX) > 3) {
					q *= 0.3;
				}
				if (careAboutRamZones && inRamZone(gridX, gridY)) {
					q -= 1000;
				}
				if (q > best) {
					bestX = gridX;
					bestY = gridY;
					bestFlipped = false;
					best = q;
				}
			}
			q = grid[gridY][gridX][1];
			if (q != NO) {
				q += (ship.flipped ? 0.001 : 0);
				if (currentlyInRamZone && StrictMath.abs(gridX - myGX) > 3) {
					q *= 0.3;
				}
				if (careAboutRamZones && inRamZone(gridX, gridY)) {
					q -= 1000;
				}
				if (q > best) {
					bestX = gridX;
					bestY = gridY;
					bestFlipped = true;
					best = q;
				}
			}
		}}
		double currentValue = calculate(ship.getX(), ship.getY(), ship.flipped, ship.availableServiceCeiling(null), wantsToRam(), wantsToBoard(), hasShortRangeWeapons(), aiMaxY(), boardXDistMax(), hasHighStormAbility()) + 0.1;
		if (currentlyInRamZone) {
			currentValue -= 1000;
		}
		if (best > NO && best > currentValue) {
			return new MoveTo(bestX, bestY, bestFlipped);
		} else {
			return null;
		}
	}
	
	private double awayFromCurrentPosition(double x, boolean pointingLeft, double optimalDistance, double value) {
		double distance = ship.flipped ? x - ship.getX() : ship.getX() - x;
		double distanceFromOptimalDistance = StrictMath.abs(distance - optimalDistance);
		return value + 10 * optimalDistance - 10 * distanceFromOptimalDistance + (pointingLeft == ship.flipped ? 10 : 0);
	}
	
	private double calculate(double x, double y, boolean pointingLeft, int asc, boolean wantsToRam, boolean wantsToBoard, boolean isShortRange, int aiMaxY, int boardXDistMax, boolean hasHighStormAbility) {
		if (!c.landFormations.isEmpty() && c.landFormations.get(0).landscapeType.hasWater && y + ship.getBBHeight() > AGame.GROUND_LEVEL) { return NO; }
		if (ship.type.onGround && ship.moveTo != null && StrictMath.abs(ship.moveTo.x - ship.getX()) > 50 && ship.msSinceLastXMove > 500) {
			return awayFromCurrentPosition(x, pointingLeft, 100, calculateNormally(x, y, pointingLeft, asc, wantsToBoard, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility, false));
		}
		return wantsToRam
				? calculateRamSpot(x, y, pointingLeft, asc, wantsToBoard, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility)
				: calculateNormally(x, y, pointingLeft, asc, wantsToBoard, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility, false);
	}
	
	private boolean wantsToRam() {
		return (!ship.isArmedOrHasTroops() && quality != AIQuality.STUPID) || ship.hasRam();
	}
	
	private boolean wantsToBoard() {
		if (ship.hasCrewTypeAny(CrewType.boarders)) { return true; }
		int tsz = mySide.troops.size();
		for (int ti = 0; ti < tsz; ti++) {
			Crewman cm = mySide.troops.get(ti);
			if (cm.attachedTo == ship && cm.type.canBoard && cm.alive()) {
				return true;
			}
		}
		return false;
	}
	
	private int aiMaxY() {
		int aiMaxY = 10000;
		int msz = ship.modules.size();
		for (int i = 0; i < msz; i++) {
			aiMaxY = StrictMath.min(ship.modules.get(i).type.aiMaxY(), aiMaxY);
		}
		return aiMaxY;
	}
	
	private boolean hasShortRangeWeapons() {
		int msz = ship.modules.size();
		for (int i = 0; i < msz; i++) {
			int mxr = ship.modules.get(i).type.getMaxXRange(ship.currentBonuses);
			int mr = ship.modules.get(i).type.getMaxRange(ship.currentBonuses);
			if ((mxr > 0 && mxr < 500) || (mr > 0 && mr < 500)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean canRam() {
		FindClosestResult fcr = new FindClosestResult();
		findClosestRock(fcr, ship.getX(), ship.getY(), ship.flipped);
		if (fcr.invalidMove) { return false; }
		findRammableShip(fcr, ship.getX(), ship.getY(), ship.flipped);
		if (fcr.invalidMove) { return false; }
		return fcr.dist > 100 && fcr.dist < 1000 && enemySide.ships.contains(fcr.xClosest);
	}
	
	static strictfp class FindClosestResult {
		boolean invalidMove = false;
		Body xClosest;
		double dist = 0;
	}
	
	private void findClosestRock(FindClosestResult fcr, double x, double y, boolean pointingLeft) {
		double shipW = ship.getBBWidth();
		double shipH = ship.getBBHeight();				
		double lowX = StrictMath.min(ship.getX(), x) - 1;
		double highX = StrictMath.max(ship.getX() + shipW, x + shipW) + 1;
		double mvW = highX - lowX;
		double lowY = StrictMath.min(ship.getY(), y) - 1;
		double highY = StrictMath.max(ship.getY() + shipH, y + shipH) + 1;
		double mvH = highY - lowY;
		
		int lfsz = c.landFormations.size();
		int start = ship.type.onGround ? 1 : 0;
		for (int lfi = start; lfi < lfsz; lfi++) {
			LandFormation lf = c.landFormations.get(lfi);
			if (y + shipH > lf.getY() && y < lf.getY() + lf.getBBHeight()) {
				if (pointingLeft) {
					if (lf.getX() < x) {
						double d = x - lf.getX() - lf.getBBWidth();
						if (fcr.xClosest == null || d < fcr.dist) {
							fcr.xClosest = lf;
							fcr.dist = d;
						}
					}
				} else {
					if (lf.getX() > x) {
						double d = lf.getX() - x - ship.getBBWidth();
						if (fcr.xClosest == null || d < fcr.dist) {
							fcr.xClosest = lf;
							fcr.dist = d;
						}
					}
				}
			}
			if (!Rect2D.intersects(ship.getX() - 2, ship.getY() - 2, shipW + 4, shipH + 4, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())) {
				int widen = Rect2D.intersects(ship.getX() - 20, ship.getY() - 20, shipW + 40, shipH + 40, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())
						? 0
						: 5;
				if (Rect2D.intersects(lowX - widen, lowY - widen, mvW + widen * 2, mvH + widen * 2, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())) {
					fcr.invalidMove = true;
					return;
				}
			}
		}
	}
	
	private void findClosestShip(FindClosestResult fcr, double x, double y, boolean pointingLeft, double areaY, double areaH) {
		double shipW = ship.getBBWidth();
		double shipH = ship.getBBHeight();				
		double lowX = StrictMath.min(ship.getX(), x) - 1;
		double highX = StrictMath.max(ship.getX() + shipW, x + shipW) + 1;
		double mvW = highX - lowX;
		double lowY = ship.type.onGround ? (y - 1) : (StrictMath.min(ship.getY(), y) - 1);
		double highY = ship.type.onGround ? (y + shipH + 1) : (StrictMath.max(ship.getY() + shipH, y + shipH) + 1);
		double mvH = highY - lowY;
		
		int ssz = c.sides.size();
		for (int si = 0; si < ssz; si++) {
			Side s = c.sides.get(si);
			int osz = s.ships.size();
			for (int oi = 0; oi < osz; oi++) {
				Airship other = s.ships.get(oi);
				if (other == ship) { continue; }
				if (areaY > other.getY() && areaY + areaH < other.getY() + other.getBBHeight()) {
					if (pointingLeft) {
						if (other.getX() < x) {
							double d = x - other.getX() - other.getBBWidth();
							if (fcr.xClosest == null || d < fcr.dist) {
								fcr.xClosest = other;
								fcr.dist = d;
							}
						}
					} else {
						if (other.getX() > x) {
							double d = other.getX() - x - ship.getBBWidth();
							if (fcr.xClosest == null || d < fcr.dist) {
								fcr.xClosest = other;
								fcr.dist = d;
							}
						}
					}
				}
				if (!Rect2D.intersects(ship.getX() - 2, ship.getY() - 2, shipW + 4, shipH + 4, other.getX(), other.getY(), other.getBBWidth(), other.getBBHeight())) {
					int widen = Rect2D.intersects(ship.getX() - 20, ship.getY() - 20, shipW + 40, shipH + 40, other.getX(), other.getY(), other.getBBWidth(), other.getBBHeight())
						? 0
						: 5;
					if (Rect2D.intersects(lowX - widen, lowY - widen, mvW + widen * 2, mvH + widen * 2, other.getX(), other.getY(), other.getBBWidth(), other.getBBHeight())) {
						fcr.invalidMove = true;
						return;
					}
				}
			}
		}
	}
	
	// returns ram quality
	private double findRammableShip(FindClosestResult fcr, double x, double y, boolean pointingLeft) {
		if (ship.type.onGround) {
			fcr.xClosest = null;
			fcr.dist = 0;
			fcr.invalidMove = false;
			for (int si = 0; si < c.sides.size(); si++) {
				Side side = c.sides.get(si);
				for (int ai = 0; ai < side.ships.size(); ai++) {
					Airship other = side.ships.get(ai);
					if (other == ship) { continue; }
					if (!other.type.onGround && !other.grounded()) { continue; }
					if (pointingLeft != (other.getX() < ship.getX())) { continue; }
					double dist = Math.abs(other.getX() - ship.getX());
					if (dist < fcr.dist || fcr.xClosest == null) {
						fcr.xClosest = other;
						fcr.dist = dist;
						fcr.invalidMove = side == mySide;
					}
				}
			}
			return 1;
		} else if (ship.hasRam()) {
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if (m.type.isRam()) {
					double my = y + m.y * AGame.SGS;
					double mh = m.type.getH() * AGame.SGS;
					findClosestShip(fcr, x, y, pointingLeft, my, mh);
					if (!fcr.invalidMove && fcr.xClosest != null) {
						double ramYCenter = my + mh / 2;
						// Give full ram quality if we're aligned with somewhere near the center of the target.
						return ramYCenter > fcr.xClosest.getY() + fcr.xClosest.getBBHeight() / 4 && ramYCenter < fcr.xClosest.getY() + fcr.xClosest.getBBHeight() * 3 / 4 ? 1 : 0.5;
					}
				}
			}
		} else {
			findClosestShip(fcr, x, y, pointingLeft, y, ship.getBBHeight());
			return 1;
		}
		return 1;
	}
	
	private double calculateRamSpot(double x, double y, boolean pointingLeft, int asc, boolean wantsToBoard, boolean isShortRange, int aiMaxY, int boardXDistMax, boolean hasHighStormAbility) {
		if (y < AGame.GROUND_LEVEL - asc && !ship.type.onGround) { return NO; }
		
		FindClosestResult fcr = new FindClosestResult();
		if (!ship.type.onGround) {
			findClosestRock(fcr, x, y, pointingLeft);
			if (fcr.invalidMove) { return NO; }
		}
		double ramQuality = findRammableShip(fcr, x, y, pointingLeft);
		if (fcr.invalidMove) { return NO; }
		
		int ramDist = ship.type.onGround ? 200 : 800;
		if (enemySide.ships.contains(fcr.xClosest)) {
			return (ramDist - StrictMath.abs(fcr.dist - ramDist)) * ramQuality * (ship.type.onGround ? 4 : 1);
		}
				
		// No ramming opportunity here, but we don't want it to run away and hide.
		return StrictMath.min(ramDist * 0.4, calculateNormally(x, y, pointingLeft, asc, wantsToBoard, isShortRange, aiMaxY, boardXDistMax, hasHighStormAbility, false));
	}
	
	private double calculateNormally(double x, double y, boolean flipped, int asc, boolean wantsToBoard, boolean isShortRange, int aiMaxY, int boardXDistMax, boolean hasHighStormAbility, boolean teleport) {
		// Can we move there?
		if (y > aiMaxY) { return NO; }
		if (!ship.type.onGround && y < AGame.GROUND_LEVEL - asc) {
			return NO;
		}
		if (ship.type == ShipType.AIRSHIP && hasHighStormAbility && y < HIGH_STORM_Y) {
			return NO;
		}
		if (ship.type == ShipType.AIRSHIP && c.timeOfDay.effect.lightningChance > 0.0005 && y < c.timeOfDay.effect.maxLightningY) {
			return NO;
		}
				
		double shipW = ship.getBBWidth();
		double shipH = ship.getBBHeight();
		
		double boardMult = 1;
		double belowMult = 1;
		double freeXMult = 1;
		double freeYMult = 1;
		double freeXBonus = 0.2;
		double freeYBonus = 0.2;
		double rockInWayMult = 1;
		
		if (teleport) {
			for (int i = 1; i < c.landFormations.size(); i++) {
				LandFormation lf = c.landFormations.get(i);
				if (Rect2D.intersects(x, y, shipW, shipH, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())) {
					return NO;
				}
			}
			for (int si = 0; si < 2; si++) {
				Side side = c.sides.get(si);
				for (int i = 0; i < side.ships.size(); i++) {
					Airship other = side.ships.get(i);
					if (other == ship) { continue; }
					if (Rect2D.intersects(x, y, shipW, shipH, other.getX(), other.getY(), other.getBBWidth(), other.getBBHeight())) {
						return NO;
					}
				}
			}
		} else {
			
			// Would we collide with another ship?
			// Also, would we be below another ship.
			
			double lowX = StrictMath.min(ship.getX(), x) - 1;
			double highX = StrictMath.max(ship.getX() + shipW, x + shipW) + 1;
			double mvW = highX - lowX;
			double lowY = StrictMath.min(ship.getY(), y) - 1;
			double highY = StrictMath.max(ship.getY() + shipH, y + shipH) + 1;
			double mvH = highY - lowY;
			
			// Would we go through a drop zone?
			for (int i = 0; i < c.crashZones.size(); i++) {
				Combat.CrashZone cz = c.crashZones.get(i);
				// Are we already in it?
				if (ship.getX() + ship.getBBWidth() > cz.cx - Combat.CRASH_ZONE_W / 2 && ship.getX() < cz.cx + Combat.CRASH_ZONE_W / 2) {
					continue;
				}
				if (lowX < cz.cx + Combat.CRASH_ZONE_W / 2 && highX > cz.cx - Combat.CRASH_ZONE_W / 2) {
					return NO;
				}
			}

			// Loop over rock obstacles.
			if (lfs) {
				int lfsz = c.landFormations.size();
				int start = ship.type.onGround ? 1 : 0;
				for (int lfi = start; lfi < lfsz; lfi++) {
					LandFormation lf = c.landFormations.get(lfi);
					if (lf.immobile) {
						if (isShortRange) {
							for (double probeX = x; probeX < x + shipW; probeX += AGame.SGS) {
								if (lf.yBoundaryAt(probeX) < y + shipH) {
									return NO;
								}
							}
						} else {
							for (double probeX = lowX; probeX < highX + AGame.SGS; probeX += AGame.SGS) {
								if (lf.yBoundaryAt(probeX) < highY) {
									return NO;
								}
							}
						}
					} else {
						if (!Rect2D.intersects(ship.getX() - 2, ship.getY() - 2, shipW + 4, shipH + 4, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())) {
							int widen = Rect2D.intersects(ship.getX() - 20, ship.getY() - 20, shipW + 40, shipH + 40, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())
								? 0
								: 5;
							if (Rect2D.intersects(lowX - widen, lowY - widen, mvW + widen * 2, mvH + widen * 2, lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())) {
								if (wantsToBoard) {
									rockInWayMult = 0.1;
								} else {
									return NO;
								}
							}
							// Ideally, we want a position where we can move horizontally and vertically.
							if (y < lf.getY() + lf.getBBHeight() + 12 && lf.getY() < y + shipH + 12) {
								double dist = x < lf.getX() ? StrictMath.max(100, x + shipW - lf.getX()) : StrictMath.max(100, lf.getX() + lf.getBBWidth() - x);
								freeXMult = 1 - 20 / dist;
								freeXBonus = StrictMath.min(0.2, dist / 4000);
							}
							if (x < lf.getX() + lf.getBBWidth() + 3 && lf.getX() < x + shipW + 3) {
								freeYMult = 0.8;
								freeYBonus = 0;
							}
						}
					}
				}
			}

			highY += ship.getCachedGroundOffset();

			double shipHForPreIntersect = shipH + ship.getCachedGroundOffset();

			int ssz = c.sides.size();
			for (int si = 0; si < ssz; si++) {
				Side s = c.sides.get(si);
				int osz = s.ships.size();
				for (int oi = 0; oi < osz; oi++) {
					Airship other = s.ships.get(oi);
					if (other == ship) { continue; }
					boolean wouldBeMovingAwayFromOther = StrictMath.signum(x - other.getX()) == StrictMath.signum(ship.getX() - other.getX());
					double otherW = other.getBBWidth();
					double otherH = other.getBBHeight();
					boolean considerMove = (s == mySide && other.aiTmpCanMove && other.aiTmpAvailableLift > 0);
					double otherLowX = considerMove ? StrictMath.min(other.getX(), other.moveTo.x) : other.getX();
					double otherLowY = considerMove ? StrictMath.min(other.getY(), other.moveTo.y) : other.getY();
					double otherHighX = considerMove ? StrictMath.max(other.getX() + otherW, other.moveTo.x + otherW) : other.getX() + otherW;
					double otherHighY = considerMove ? StrictMath.max(other.getY() + otherH, other.moveTo.y + otherH) : other.getY() + otherH;
					otherHighY += other.getCachedGroundOffset();
					otherW = otherHighX - otherLowX;
					otherH = otherHighY - otherLowY;
					double otherBBHeightForPreIntersect = other.getBBHeight() + other.getCachedGroundOffset();
					if (!wouldBeMovingAwayFromOther || !Rect2D.intersects(ship.getX() - 2, ship.getY() - 2, shipW + 4, shipHForPreIntersect + 4, other.getX(), other.getY(), other.getBBWidth(), otherBBHeightForPreIntersect)) {
						int widen = Rect2D.intersects(ship.getX() - 20, ship.getY() - 20, shipW + 40, shipH + 40, other.getX(), other.getY(), other.getBBWidth(), other.getBBHeight())
							? 0
							: 5;
						if (Rect2D.intersects(lowX - widen, lowY - widen, mvW + widen * 2, mvH + widen * 2, otherLowX, otherLowY, otherW, otherH)) {
							return NO;
						}
					}
					// If we intersect with the shadow of another ship, this is not good.
					if (Rect2D.intersects(x, y, shipW, shipH, otherLowX, otherLowY, otherW, 10000)) {
						belowMult = 0.9;
					}
					// Ideally, we want a position where we can move horizontally and vertically.
					if (y < otherHighY + 5 && otherLowY < y + shipH + 5) {
						/*freeXMult = 0.8;
						freeXBonus = 0;*/
						double dist = x < other.getX() ? StrictMath.max(100, x + shipW - other.getX()) : StrictMath.max(100, other.getX() + otherW - x);
						freeXMult = 1 - 20 / dist;
						freeXBonus = StrictMath.min(0.2, dist / 4000);
					}
					if (x < otherHighX && otherLowX < x + shipW) {
						freeYMult = 0.8;
						freeYBonus = 0;
					}
					// If this lets us board them, yay.
					if (boardXDistMax < 50) {
						// We want to be able to drop.
						if (wantsToBoard &&
							other.getX() - (x + shipW) <= -64 &&
							x - (other.getX() + otherW) <= -64 &&
							other.getY() - (y + shipH) <= TOO_HIGH &&
							y + shipH < other.getY())
						{
							boardMult = 10;
						}
					} else {
						if (wantsToBoard &&
							other.getX() - (x + shipW) <= boardXDistMax &&
							x - (other.getX() + otherW) <= boardXDistMax &&
							other.getY() - (y + shipH) <= TOO_HIGH &&
							y - (other.getY() + otherH) <= TOO_LOW)
						{
							boardMult = 10;
						}
					}
				}
			}
		}
				
		int esz = enemySide.ships.size();
		
		double optimumRangeMult = 1.0;
		if (ship.type == ShipType.LANDSHIP) {
			double div = 1;
			int ssz = mySide.ships.size();
			for (int si = 0; si < ssz; si++) {
				if (mySide.ships.get(si).type == ShipType.LANDSHIP) {
					div++;
				}
			}
			optimumRangeMult = 2.0 / div;
		}
		
		WeatherEffect we = c.timeOfDay.effect;
		
		double shootToLeftJitterMult = quality == AIQuality.SMART ? we.shootJitterMult * we.shootToLeftJitterMult : 1;
		double shootToRightJitterMult = quality == AIQuality.SMART ? we.shootJitterMult * we.shootToRightJitterMult : 1;
		
		// Can we shoot an enemy?
		double shootQuality = 0.1;
		double bestShootShipQuality = 0;
		int msz = ship.modules.size();
		boolean hasWeapons = false;
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			if ((m.type.isWeapon() || !m.tentacles.isEmpty()) && m.canRun()) {
				hasWeapons = true;
				for (int ei = 0; ei < esz; ei++) {
					Airship e = enemySide.ships.get(ei);
					double shipShootQuality = 0;
					int tsz = m.tentacles.size();
					
					for (int ti = 0; ti < tsz; ti++) {
						Tentacle t = m.tentacles.get(ti);
						if (t.canTentacle(ship, m, e, x, y, e.getX(), e.getY(), flipped, INSET, 0.8)) {
							shipShootQuality += e.dangerCache * (t.canTentacle(ship, m, e, x, y, e.getX(), e.getY(), flipped, INSET + 10, 0.5) ? 1 : 0.5) * t.spec.getDPSEquivalent() / 10;
						}
					}
					if (m.type.isWeapon() && (isShortRange ? m.canHitCloseUp(e, x, y, e.getX(), e.getY(), flipped, INSET) : m.canHit(e, x, y, e.getX(), e.getY(), flipped, INSET))) {
						if (m.ammoLeft == 0 && !m.type.canResupplyInCombat(ship.currentBonuses)) { continue; } // Ignore used-up single-use weapons.
						double qMult = m.canHitWell(e, x, y, e.getX(), e.getY(), flipped, INSET + 20) ? 1 : 0.5;
						if (y + ship.getBBHeight() < e.getY()) {
							qMult *= 1.5;
						} else if (y < e.getY()) {
							qMult *= 1.2;
						}
						double manhattanDist =
								StrictMath.min(StrictMath.abs(x - e.getX() - e.getBBWidth()), StrictMath.abs(e.getX() - x - shipW)) +
								StrictMath.min(StrictMath.abs(y - e.getY() - e.getBBHeight()), StrictMath.abs(e.getY() - y - shipH));
						boolean ltr = e.getX() - x > 0;
						if (ltr) {
							manhattanDist *= shootToRightJitterMult;
						} else {
							manhattanDist *= shootToLeftJitterMult;
						}
						qMult *= StrictMath.max(0.01, StrictMath.min(1, m.type.getOptimumRange(ship.currentBonuses) * optimumRangeMult / manhattanDist));
						shipShootQuality += e.dangerCache * qMult * m.type.approxDPS(ship.currentBonuses) / 30;
					}
					
					shootQuality += shipShootQuality / 10;
					if (e.tauntTime > 0) {
						shootQuality += 10000;
					}
					bestShootShipQuality = StrictMath.max(bestShootShipQuality, shipShootQuality);
				}
			}
		}
		shootQuality += bestShootShipQuality;
				
		// Can an enemy shoot us?
		double damageQuality = 5;
		// Note that these are distances to corners, which is a bit dubious.
		double closestEnemyDist = 100000;
		double closestEnemyXDist = 100000;
		int myCommandCooldown = ship.commandPointsRequired() / (ship.commandPointsGenerated() + 1);
		double mySpeed = ship.getSpeed();
		for (int ei = 0; ei < esz; ei++) {
			Airship e = enemySide.ships.get(ei);
			int eCPG = e.commandPointsGenerated();
			double eSpd = e.getSpeed();
			boolean isFaster = e.type.mobile && eSpd > 0 && ((eSpd > mySpeed * 1.2) || (e.commandPointsRequired() / (eCPG + 1) < myCommandCooldown));
			int emsz = e.modules.size();
			double manhattanDist =
					StrictMath.min(StrictMath.abs(x - e.getX() - e.getBBWidth()), StrictMath.abs(e.getX() - x - shipW)) +
					StrictMath.min(StrictMath.abs(y - e.getY() - e.getBBHeight()), StrictMath.abs(e.getY() - y - shipH));
			closestEnemyDist = StrictMath.min(manhattanDist, closestEnemyDist);
			closestEnemyXDist = StrictMath.min(StrictMath.min(StrictMath.abs(x - e.getX() - e.getBBWidth()), StrictMath.abs(e.getX() - x - shipW)), closestEnemyXDist);
			if (quality != AIQuality.STUPID && !isFaster) {
				boolean ltr = x - e.getX() > 0;
				if (ltr) {
					manhattanDist *= shootToRightJitterMult;
				} else {
					manhattanDist *= shootToLeftJitterMult;
				}
				for (int emi = 0; emi < emsz; emi++) {
					Module em = e.modules.get(emi);
					// -ve inset means extra caution re enemy weapons range
					if (em.type.isWeapon() && em.hp > 0 && em.canHit(ship, e.getX(), e.getY(), x, y, e.flipped, -INSET)) {
						if (em.ammoLeft == 0 && !em.type.canResupplyInCombat(e.currentBonuses)) { continue; } // Ignore used-up single-use weapons.
						damageQuality += StrictMath.min(1, em.type.getOptimumRange(e.currentBonuses) / manhattanDist) * em.type.approxDPS(e.currentBonuses);
					}
					int tsz = em.tentacles.size();
					for (int ti = 0; ti < tsz; ti++) {
						Tentacle t = em.tentacles.get(ti);
						if (em.hp > 0 && t.canTentacle(e, em, ship, e.getX(), e.getY(), x, y, e.flipped, -INSET, 1.05)) {
							damageQuality += t.spec.getDPSEquivalent();
						}
					}
				}
			}
		}
		
		double closerBonus = (hasWeapons || wantsToBoard)
				? 1000.0 / (closestEnemyDist + 1000)
				: closestEnemyXDist / 10 - y / 4;
		
		double dist = StrictMath.abs(x - ship.getX()) + StrictMath.abs(y - ship.getY());
		
		return (shootQuality / damageQuality * rockInWayMult * belowMult * freeXMult * freeYMult) * StrictMath.max(0.25, 1 - dist * 0.0003) + boardMult + freeXBonus * 3 + freeYBonus * 3 + closerBonus * 3;
	}
}
