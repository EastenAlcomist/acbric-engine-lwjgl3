package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.Leg.Spec;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

public class  Airship extends GridBody implements Comparator<Job>, Cloneable, HasName {
	public static final Clr ARMOUR_BACK = new Clr(90, 90, 90);
	public static final Clr INSIDE = new Clr(70, 70, 70);
	public static final Clr LIT_EDGE = new Clr(255, 255, 255, 30);
	public static final Clr DARK_EDGE = new Clr(0, 0, 0, 70);
	public static final Clr HP_BG = new Clr(40, 40, 40, 128);
	public static final Clr COAL_FG = new Clr(200, 200, 200);
	public static final Clr COAL_REAL_FG = new Clr(20, 20, 20);
	public static final Clr WATER_FG = new Clr(112, 155, 204);
	public static final Clr REPAIR_FG = new Clr(200, 184, 79);
	public static final Clr AMMO_FG = new Clr(187, 66, 29);
	public static final double SPEED_TO_PX_PER_MS = 0.05;
	public static final double DROP_SPEED_PER_MS = 0.1;
	public static final double DRIFT_DOWN_SPEED_PER_MS = 0.4;
	public static final double FALL_SPEED_PER_MS = 0.75;
	public static final int MS_UNTIL_FULL_SPEED = 1500;
	
	public transient Combat CURRENT_COMBAT_DELETEME = null;
	
	public String networkID = "[no network ID]";
	public int version;
	public int chunkSubIDCounter = 1;
	public String designName;
	public BonusSet designRequiredBonuses;
	
	// Belonging
	public int multiplayerControllerID;
	public transient int multiplayerControllerID_tmp;
	public CoatOfArms originalArms;
	public FleetOwnerRef owner;
	
	// Caching
	private transient int cachedCost, cachedCostVersion = -1;
	private transient int cachedMaintenance, cachedMaintenanceVersion = -1;
	private transient int cachedSupplyRequired, cachedSupplyRequiredVersion = -1;
	private transient int cachedSupplyCapacity, cachedSupplyCapacityVersion = -1;
	
	public int getCachedCost() {
		if (cachedCostVersion != version) {
			cachedCostVersion = version;
			cachedCost = getCost();
		}
		return cachedCost;
	}
	
	public int getCachedMaintenance() {
		if (cachedMaintenanceVersion != version) {
			cachedMaintenanceVersion = version;
			cachedMaintenance = maintenanceCost();
		}
		return cachedMaintenance;
	}
	
	public int getCachedSupplyRequired() {
		if (cachedSupplyRequiredVersion != version) {
			cachedSupplyRequiredVersion = version;
			cachedSupplyRequired = getSupplyRequired();
		}
		return cachedSupplyRequired;
	}
	
	public int getCachedSupplyCapacity() {
		if (cachedSupplyCapacityVersion != version) {
			cachedSupplyCapacityVersion = version;
			cachedSupplyCapacity = getSupplyCapacity();
		}
		return cachedSupplyCapacity;
	}
	
	public static enum MoveMode {
		DEFAULT(false, false), FLIP_THEN_MOVE(true, false), MOVE_THEN_FLIP(true, true);
		public final boolean override;
		public final boolean flipAtEnd;

		private MoveMode(boolean override, boolean flipAtEnd) {
			this.override = override;
			this.flipAtEnd = flipAtEnd;
		}
	}
	
	// AI
	public transient int tempNumDesignCounter = 0;
	
	// Direct control
	private transient int directControlID = -1;
	public transient ShipSpeed speedOrder = ShipSpeed.STOP;
	public transient int altitudeOrder;
	
	// Orders
	public boolean focusOnShooting = true;
	public boolean focusOnRepair = false;
	public boolean focusOnFirefighting = false;
	public boolean focusOnMoving = false;
	public MoveMode moveMode = MoveMode.DEFAULT;
	public Pt moveTo = new Pt(0, 0);
	public boolean flipTo = false;
	public boolean ramming = false;
	public boolean grounding = false;
	public boolean sitting = false;
	public int flipMs = 0;
	public int unableToFlipMs = 0;
	public Airship fireAt;
	public Airship board;
	public Airship tetherAt;
	public FireMode fireMode = FireMode.NORMAL;
	public AircraftBehaviourMode aircraftMode = AircraftBehaviourMode.NORMAL;
	public boolean releaseOneUseWeapons, releaseOneUseLift, releaseOneUsePropulsion;
	public int outOfCombatMs = 0;
	public Tile boardExitTargetCache = null;
	public boolean isBonusConstruction;
	public int notUnderCommandMs = 0;
	public boolean switchedReserve = false;
	
	private int canDoPathingIndex = 0;
	private int boarderCanDoPathingIndex = 0;
		
	public int commandPoints = 0;
	public static final int ASSIGN_JOBS_EVERY = 300;
	public int assignJobsMs = ASSIGN_JOBS_EVERY;

	public static final int[][] ADJ = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};

	private String name;
	
	public ArrayList<Module> modules = new ArrayList<Module>();
	public ArrayList<Tile> tiles = new ArrayList<Tile>();
	public ArrayList<Crewman> crew = new ArrayList<Crewman>();
	public ArrayList<Crewman> boarders = new ArrayList<Crewman>();
	public ArrayList<Decal> decals = new ArrayList<Decal>();
	private int w, h;
	
	public boolean captured = false;
	public int msSinceLastXMove = 0;

	public boolean flipped = false;
	public int braking = 0;
	public int hasBraked = 0;
	public transient boolean showingOutside = false;
	public transient int weight = 0;
	
	public transient boolean cachedGroundOffsetCalculated = false;
	public transient double cachedGroundOffset;
	
	public int timeMovingInSameXDirection = 0;
	public boolean movingLeft = false;
	public int popOutCooldown = 0;
		
	private Airship originalDesign;
	
	public Pt lastPlaced;
	public boolean lastPlacedFlipped;
	
	// Aerodynamics
	public transient double frontDrag, backDrag, bottomDrag, topDrag;
	public boolean inWater;
	
	// Creaking
	private transient double prevDx = 0, prevDy = 0;
	public transient double mechStress;
	
	// Tracking things for shouts
	public transient int fallingTime = 0;
	public transient double explosionAmount = 0;
	public transient double moduleLossAmount = 0;
	public transient boolean biggestInFleet = false;
	public transient boolean reportFootingLoss = false;
	public transient boolean footingLossReported = false;
	
	public transient double flagFlipAccum;
	public transient double smoothedXSpeed, smoothedYSpeed;
	
	public transient boolean hasHadSpringFriction = false;
	
	private int originalAmmoCapacity = -1;
	private int originalCoalCapacity = -1;
	private int originalWaterCapacity = -1;
	private int originalRepairCapacity = -1;
	private int originalAllQuartered = -1;
	
	public transient HashSet<ModuleType> fixers = new HashSet<ModuleType>();
	
	public HashMap<String, Integer> combatStats = new HashMap<String, Integer>();
	public transient HashMap<String, Integer> prevCombatStats = new HashMap<String, Integer>();
	
	private transient ArrayList<Module> obstructedModules = null;
	private transient Boolean isFullyConnectedInEditor = null;
		
	// Stats for achievements
	public int mechTentacleDamageTaken = 0;
	public int hussarDamageTaken = 0;
	public int sawDamageTaken = 0;
	public int otherDamageTaken = 0;
	
	// Heroes
	public HeroType captainType; // For single combats.
	private transient Hero captain;
	public EnumSet<HeroType.CombatAbility> usedAbilities = EnumSet.noneOf(HeroType.CombatAbility.class);
	public int smokescreenTime = 0;
	public static final int SMOKESCREEN_TIME = 60000;
	public static final int SMOKESCREEN_INACCURACY_MULT = 2;
	public static final int SMOKESCREEN_INACCURACY_FIXED = 30;
	public int burstOfSpeedTime = 0;
	public static final int BURST_OF_SPEED_TIME = 60000;
	public static final int BURST_OF_SPEED_MULT = 3;
	public static final int BURST_OF_SPEED_MOVE_CMD_DIV = 10;
	public int superchargeSuspendiumTime = 0;
	public static final int SUPERCHARGE_SUSPENDIUM_TIME = 60000;
	public static final int SUPERCHARGE_SUSPENDIUM_MULT = 2;
	public int doubleTimeTime = 0;
	public static final int DOUBLE_TIME_TIME = 60000;
	public static final double DOUBLE_TIME_SPEED_MULT = 1.5;
	public static final int DOUBLE_TIME_RELOAD_MULT = 2;
	public int fearTime = 0;
	public static final int FEAR_TIME = 60000;
	public static final double FEAR_SPEED_MULT = 0.2;
	public static final double WADING_SPEED_MULT = 0.5;
	public static final int FEAR_RELOAD_TIME_MULT = 4;
	public int tauntTime = 0;
	public static final int TAUNT_TIME = 60000;
	public int glimmerTime = 0;
	public static final int GLIMMER_JITTER_MULT = 4;
	public static final int GLIMMER_TIME = 60000;
	public int crosswindsTime = 0;
	public static final int CROSSWINDS_PROPULSION_DIV = 20;
	public static final int CROSSWINDS_TIME = 60000;
	public boolean cripplingShotTarget;
	public static final int CRIPPLING_SHOT_INACCURACY_DIV = 3;
	public boolean disarmingShotTarget;
	public static final int DISARMING_SHOT_INACCURACY_DIV = 3;
	public int paralysisTime = 0;
	public static final int PARALYSIS_TIME = 40000;
	public int gustOfWindTime = 0;
	public static final int GUST_OF_WIND_TIME = 10000;
	public double gustOfWindDX = 0;
	public double gustOfWindDY = 0;
	public int holdOnTime = 0;
	public static final int HOLD_ON_TIME = 30000;
	public int suddenStormTime = 0;
	public static final int SUDDEN_STORM_TIME = 15000;
	public double suddenStormDX = 0;
	public int momentOfDoubtTime = 0;
	public static final int MOMENT_OF_DOUBT_TIME = 8000;
	public int blindnessTime = 0;
	public static final int BLINDNESS_JITTER_MULT = 6;
	public static final int BLINDNESS_TIME = 10000;
	// Stats for combat abilities
	public int[] damageTaken = {0, 0};
	public int[] damageTakenFromAbove = {0, 0};
	public int[] damageMissed = {0, 0};
	
	// XP and medals
	public double crewExperience = 0;
	public int fireRatePercentFromMedals;
	public int accuracyPercentFromMedals;
	public int crewSpeedPercentFromMedals;
	public int flammabilityPercentFromMedals;
	public int explosionRiskPercentFromMedals;
	public int commandCooldownPercentFromMedals;
	public int repairAmountPercentFromMedals;
	public int firefightAmountPercentFromMedals;
	public int propulsionPercentFromMedals;
	public int liftPercentFromMedals;
	public int fleetSpeedPercentFromMedals;
	public int armourRepairPercentFromMedals;
	public boolean surpriseAttackFromMedals;
	public ArrayList<Medal> medals = new ArrayList<Medal>();
	
	public void giveMedal(Medal m) {
		medals.add(m);
		fireRatePercentFromMedals += m.effect.fireRatePercent;
		accuracyPercentFromMedals += m.effect.accuracyPercent;
		crewSpeedPercentFromMedals += m.effect.crewSpeedPercent;
		flammabilityPercentFromMedals += m.effect.flammabilityPercent;
		explosionRiskPercentFromMedals += m.effect.explosionRiskPercent;
		commandCooldownPercentFromMedals += m.effect.commandCooldownPercent;
		repairAmountPercentFromMedals += m.effect.repairAmountPercent;
		firefightAmountPercentFromMedals += m.effect.firefightAmountPercent;
		propulsionPercentFromMedals += m.effect.propulsionPercent;
		liftPercentFromMedals += m.effect.liftPercent;
		fleetSpeedPercentFromMedals += m.effect.fleetSpeedPercent;
		armourRepairPercentFromMedals += m.effect.armourRepairPercent;
		surpriseAttackFromMedals = surpriseAttackFromMedals || m.effect.surpriseAttack;
		version++;
	}
	
	public void clearMedals() {
		medals.clear();
		fireRatePercentFromMedals = 0;
		accuracyPercentFromMedals = 0;
		crewSpeedPercentFromMedals = 0;
		flammabilityPercentFromMedals = 0;
		explosionRiskPercentFromMedals = 0;
		commandCooldownPercentFromMedals = 0;
		repairAmountPercentFromMedals = 0;
		firefightAmountPercentFromMedals = 0;
		propulsionPercentFromMedals = 0;
		liftPercentFromMedals = 0;
		fleetSpeedPercentFromMedals = 0;
		armourRepairPercentFromMedals = 0;
		surpriseAttackFromMedals = false;
		version++;
	}
	
	public int armourRepairPercent() {
		int p = armourRepairPercentFromMedals;
		Hero capt = getCaptain();
		if (capt != null) {
			p += capt.type.armourRepairPercent;
		}
		return p;
	}
	
	public int fireAmount() {
		int amt = 0;
		for (int i = 0; i < modules.size(); i++) {
			amt += modules.get(i).fire;
		}
		return amt;
	}
	
	public boolean containsModule(Module m) {
		if (m == null) { return false; }
		Tile t = tileAt(m.x, m.y);
		return t != null && t.module == m;
	}
	
	public boolean containsTile(Tile t) {
		return t != null && t == tileAt(t.x, t.y);
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = AGame.makeFileSafe(name);
	}
	
	public boolean isPermanentlyNotUnderCommand() {
		return notUnderCommandMs > 500;
	}
	
	public void updateOriginalDesign() {
		originalDesign = null;
		originalDesign = this.clone();
		originalDesign.setX(0);
		originalDesign.setY(0);
	}
	
	public Airship getOriginalDesign() {
		return originalDesign;
	}
	
	public double getCachedGroundOffset() {
		if (!cachedGroundOffsetCalculated) {
			cachedGroundOffset = groundOffset();
			cachedGroundOffsetCalculated = true;
		}
		return cachedGroundOffset;
	}
	
	public void replaceSingleMultiplayerControllerID(int original, int replacement) {
		if (multiplayerControllerID == original) {
			multiplayerControllerID = replacement;
			for (int ci = 0; ci < crew.size(); ci++) {
				Crewman c = crew.get(ci);
				c.multiplayerControllerID = replacement;
			}
		}
		for (int bi = 0; bi < boarders.size(); bi++) {
			Crewman b = boarders.get(bi);
			if (b.multiplayerControllerID == original) {
				b.multiplayerControllerID = replacement;
			}
		}
	}
	
	public void setFlipped(boolean fl2, Combat c) {
		if (fl2 != flipped) {
			flipped = fl2;
			for (Module m : modules) {
				m.weaponAngle = Direction.flipHorizontal(m.weaponAngle);
				m.setTentaclesFlipped(fl2);
			}
			for (Particle p : stuckParticles) {
				p.x = getBBWidth() - p.x;
			}
			if (c != null) {
				ArrayList<LandFormation> floaters = new ArrayList<LandFormation>(c.landFormations);
				floaters.remove(0);
				for (Module m : modules) {
					m.resetLegs(c.landFormations.get(0), floaters);
				}
				for (Side s : c.sides) {
					for (Crewman cm : s.troops) {
						if (cm.attachedTo == this) {
							cm.setX(getX() + (getBBWidth() - (cm.getX() - getX())));
						}
					}
				}
			}
		}
		flipped = fl2;
	}
	
	public int msSinceOnGround;
	public LandFormation lastGrounded;
	
	public boolean enginesRunning;
	public boolean suspendiumRunning;

	public ShipType type;

	public final BonusSet currentBonuses = new BonusSet();
	private final BonusSet baseBonuses = new BonusSet();
	
	public HashMap<Tile, HashMap<Module, ArrayList<Tile>>> paths = new HashMap<Tile, HashMap<Module, ArrayList<Tile>>>();
	private HashMap<Tile, HashMap<Tile, ArrayList<Tile>>> tilePaths = new HashMap<Tile, HashMap<Tile, ArrayList<Tile>>>();
	private int crashSize = 0;
	private transient int crashCooldown = 0;

	private transient Tile[][] tileGrid = null;
	
	public boolean aiControl;
	
	public transient TacticalAI ai;
	public transient  boolean aiTmpCanMove;
	public transient int aiTmpAvailableLift;
	
	public transient boolean fireOrderSpoken;
	public transient boolean ramOrderSpoken;
	public transient boolean aircraftOrderSpoken;
	public transient ShipSpeed speedOrderSpoken = ShipSpeed.STOP;
	
	public transient boolean wasReadyForCommand = true;
	public transient int msUntilNextSailFlap = 0;
	public transient int minMsUntilNextSailFlap = 0;
	public transient boolean sailFlapManaged = false;
	
	// Stuff for achievements
	public transient ArrayList<Airship> justRammedBy = new ArrayList<Airship>();
	public transient ArrayList<Airship> justCollidedWith = new ArrayList<Airship>();
	public transient boolean collidedWithFloatingRock;
	public transient int msSuspendiumOff;
	public transient boolean crushed;
	
	private static boolean reportBadDouble = true;
	private double cleanDouble(String name, double d) {
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			if (reportBadDouble) {
				reportBadDouble = false;
				AirshipGame.instance.reportError("Bad double value for " + name + " in ship", null, null, false, true);
			}
			return 0;
		}
		return d;
	}

	public JSONObject toJSON(Combat combat) {
		return toJSON(combat, true);
	}
	
	public JSONObject toJSON(Combat combat, boolean storeBonuses) {
		JSONObject o = new JSONObject()
				.put("version", version)
				.put("name", AGame.makeFileSafe(getName()))
				.put("type", type.name())
				.put("x", cleanDouble("x", getX())).put("y", cleanDouble("y", getY())).put("w", w).put("h", h).put("flipped", flipped)
				.put("xSpeed", cleanDouble("xSpeed", getxSpeed())).put("ySpeed", cleanDouble("ySpeed", getySpeed()))
				.put("assignJobsMs", assignJobsMs).put("commandPoints", commandPoints)
				.put("focusOnShooting", focusOnShooting)
				.put("focusOnFirefighting", focusOnFirefighting)
				.put("focusOnRepair", focusOnRepair)
				.put("focusOnMoving", focusOnMoving)
				.put("fireMode", fireMode.name())
				.put("aircraftMode", aircraftMode.name())
				.put("flipTo", flipTo)
				.put("flipMs", flipMs)
				.put("unableToFlipMs", unableToFlipMs)
				.put("msSinceOnGround", msSinceOnGround)
				.put("captured", captured)
				.put("networkID", networkID)
				.put("chunkSubIDCounter", chunkSubIDCounter)
				.put("sitting", sitting)
				.put("grounding", grounding)
				.put("msSinceLastXMove", msSinceLastXMove)
				.put("braking", braking)
				.put("hasBraked", hasBraked)
				.put("timeMovingInSameXDirection", timeMovingInSameXDirection)
				.put("movingLeft", movingLeft)
				.put("enginesRunning", enginesRunning)
				.put("suspendiumRunning", suspendiumRunning)
				.put("outOfCombatMs", outOfCombatMs)
				.put("notUnderCommandMs", notUnderCommandMs)
				.put("boardExitTargetCache", tiles.indexOf(boardExitTargetCache))
				.put("popOutCooldown", popOutCooldown)
				.put("canDoPathingIndex", canDoPathingIndex)
				.put("boarderCanDoPathingIndex", boarderCanDoPathingIndex)
				.put("originalAmmoCapacity", originalAmmoCapacity)
				.put("originalCoalCapacity", originalCoalCapacity)
				.put("originalWaterCapacity", originalWaterCapacity)
				.put("originalRepairCapacity", originalRepairCapacity)
				.put("originalAllQuartered", originalAllQuartered)
				.put("isBonusConstruction", isBonusConstruction)
				.put("releaseOneUseWeapons", releaseOneUseWeapons)
				.put("releaseOneUseLift", releaseOneUseLift)
				.put("releaseOneUsePropulsion", releaseOneUsePropulsion)
				.put("switchedReserve", switchedReserve)
				.put("aiControl", aiControl)
				.put("smokescreenTime", smokescreenTime)
				.put("burstOfSpeedTime", burstOfSpeedTime)
				.put("superchargeSuspendiumTime", superchargeSuspendiumTime)
				.put("doubleTimeTime", doubleTimeTime)
				.put("fearTime", fearTime)
				.put("tauntTime", tauntTime)
				.put("glimmerTime", glimmerTime)
				.put("crosswindsTime", crosswindsTime)
				.put("paralysisTime", paralysisTime)
				.put("cripplingShotTarget", cripplingShotTarget)
				.put("disarmingShotTarget", disarmingShotTarget)
				.put("gustOfWindTime", gustOfWindTime)
				.put("gustOfWindDX", gustOfWindDX)
				.put("gustOfWindDY", gustOfWindDY)
				.put("holdOnTime", holdOnTime)
				.put("crewExperience", crewExperience)
				.put("suddenStormTime", suddenStormTime)
				.put("suddenStormDX", suddenStormDX)
				.put("momentOfDoubtTime", momentOfDoubtTime)
				.put("blindnessTime", blindnessTime)
				.put("fireRatePercentFromMedals", fireRatePercentFromMedals)
				.put("accuracyPercentFromMedals", accuracyPercentFromMedals)
				.put("crewSpeedPercentFromMedals", crewSpeedPercentFromMedals)
				.put("flammabilityPercentFromMedals", flammabilityPercentFromMedals)
				.put("explosionRiskPercentFromMedals", explosionRiskPercentFromMedals)
				.put("commandCooldownPercentFromMedals", commandCooldownPercentFromMedals)
				.put("repairAmountPercentFromMedals", repairAmountPercentFromMedals)
				.put("firefightAmountPercentFromMedals", firefightAmountPercentFromMedals)
				.put("propulsionPercentFromMedals", propulsionPercentFromMedals)
				.put("liftPercentFromMedals", liftPercentFromMedals)
				.put("fleetSpeedPercentFromMedals", fleetSpeedPercentFromMedals)
				.put("armourRepairPercentFromMedals", armourRepairPercentFromMedals)
				.put("surpriseAttackFromMedals", surpriseAttackFromMedals)
				.put("inWater", inWater);
		if (captainType != null && combat != null) { o.put("captainType", captainType.name); }
		if (mechTentacleDamageTaken != 0) { o.put("mechTentacleDamageTaken", mechTentacleDamageTaken); }
		if (hussarDamageTaken != 0) { o.put("hussarDamageTaken", hussarDamageTaken); }
		if (sawDamageTaken != 0) { o.put("sawDamageTaken", sawDamageTaken); }
		if (otherDamageTaken != 0) { o.put("otherDamageTaken", otherDamageTaken); }
		if (!usedAbilities.isEmpty()) {
			JSONArray a = new JSONArray();
			for (HeroType.CombatAbility ab : HeroType.CombatAbility.values()) {
				if (usedAbilities.contains(ab)) {
					a.put(ab.name());
				}
			}
			o.put("usedAbilities", a);
		}
		ArrayList<Mod> usedMods = getUsedMods();
		JSONArray uma = new JSONArray();
		o.put("usedMods", uma);
		for (Mod m : usedMods) {
			uma.put(new JSONObject().put("id", m.id).put("en", m.getIdeallyEnglishName()));
		}
		if (combat != null) {
			o.put("multiplayerControllerID", multiplayerControllerID);
			if (owner != null) {
				o.put("owner", owner.toJSON());
			}
			if (originalArms != null) {
				o.put("originalArms", originalArms.toJSON());
			}
		}
		if (lastPlaced != null) {
			o.put("lastPlacedX", lastPlaced.x);
			o.put("lastPlacedY", lastPlaced.y);
			o.put("lastPlacedFlipped", lastPlacedFlipped);
		}
		if (designName != null) {
			o.put("designName", designName);
		}
		if (moveTo != null) {
			o.put("moveToX", moveTo.x).put("moveToY", moveTo.y).put("ramming", ramming);
			o.put("moveMode", moveMode.name());
		}
		if (originalDesign != null) {
			o.put("originalDesign", originalDesign.toJSON(null, storeBonuses));
		}
		if (combat != null &&
			jumpPointCache != null &&
			jumpPointCacheTarget != null)
		{
			o.put("jumpPointCacheX", jumpPointCache.gridX);
			o.put("jumpPointCacheY", jumpPointCache.gridY);
			if (combat.landFormations.contains(jumpPointCacheTarget)) {
				o.put("jumpPointCacheTargetLF", combat.landFormations.indexOf(jumpPointCacheTarget));
			} else {
				for (Side s : combat.sides) {
					if (s.ships.contains(jumpPointCacheTarget)) {
						o.put("jumpPointCacheTargetSide", combat.sides.indexOf(s));
						o.put("jumpPointCacheTargetShip", s.ships.indexOf(jumpPointCacheTarget));
					}
				}
			}
		}
		if (combat != null &&
			fallPointCache != null &&
			fallPointCacheTarget != null)
		{
			o.put("fallPointCacheX", fallPointCache.gridX);
			o.put("fallPointCacheY", fallPointCache.gridY);
			if (combat.landFormations.contains(fallPointCacheTarget)) {
				o.put("fallPointCacheTargetLF", combat.landFormations.indexOf(fallPointCacheTarget));
			} else {
				for (Side s : combat.sides) {
					if (s.ships.contains(fallPointCacheTarget)) {
						o.put("fallPointCacheTargetSide", combat.sides.indexOf(s));
						o.put("fallPointCacheTargetShip", s.ships.indexOf(fallPointCacheTarget));
					}
				}
			}
		}
		if (combat != null && fireAt != null) {
			int fireAtSideIndex = -1;
			int fireAtShipIndex = -1;
			for (Side side : combat.sides) {
				for (Airship ship : side.ships) {
					if (ship == fireAt) {
						fireAtSideIndex = combat.sides.indexOf(side);
						fireAtShipIndex = side.ships.indexOf(ship);
					}
				}
			}
			if (fireAtShipIndex != -1) {
				o.put("fireAtSideIndex", fireAtSideIndex).put("fireAtShipIndex", fireAtShipIndex);
			}
		}
		if (combat != null && board != null) {
			int boardSideIndex = -1;
			int boardShipIndex = -1;
			for (Side side : combat.sides) {
				for (Airship ship : side.ships) {
					if (ship == board) {
						boardSideIndex = combat.sides.indexOf(side);
						boardShipIndex = side.ships.indexOf(ship);
					}
				}
			}
			if (boardShipIndex != -1) {
				o.put("boardSideIndex", boardSideIndex).put("boardShipIndex", boardShipIndex);
			}
		}
		if (combat != null && tetherAt != null) {
			int tetherAtSideIndex = -1;
			int tetherAtShipIndex = -1;
			for (Side side : combat.sides) {
				for (Airship ship : side.ships) {
					if (ship == tetherAt) {
						tetherAtSideIndex = combat.sides.indexOf(side);
						tetherAtShipIndex = side.ships.indexOf(ship);
					}
				}
			}
			if (tetherAtShipIndex != -1) {
				o.put("tetherAtSideIndex", tetherAtSideIndex).put("tetherAtShipIndex", tetherAtShipIndex);
			}
		}
		if (combat != null && lastGrounded != null && combat.landFormations.contains(lastGrounded)) {
			o.put("lastGrounded", combat.landFormations.indexOf(lastGrounded));
		}
		JSONArray a = new JSONArray();
		o.put("modules", a);
		for (Module m : modules) { a.put(m.toJSON(combat)); }
		a = new JSONArray();
		o.put("tiles", a);
		for (Tile t : tiles) { a.put(t.toJSON()); }
		a = new JSONArray();
		o.put("crew", a);
		for (Crewman c : crew) { a.put(c.toJSON(combat)); }
		a = new JSONArray();
		o.put("boarders", a);
		for (Crewman c : boarders) { a.put(c.toJSON(combat)); }
		a = new JSONArray();
		o.put("decals", a);
		for (Decal d : decals) { a.put(d.toJSON()); }
		a = new JSONArray();
		o.put("carrying", a);
		a = new JSONArray();
		o.put("constructionBonuses", a);
		if (storeBonuses) {
			for (Bonus b : baseBonuses.list()) {
				a.put(b.name());
			}
		}
		a = new JSONArray();
		o.put("currentBonuses", a);
		if (storeBonuses) {
			for (Bonus b : baseBonuses.list()) {
				a.put(b.name());
			}
		}
		a = new JSONArray();
		ArrayList<String> keys = new ArrayList<String>(combatStats.keySet());
		Collections.sort(keys);
		for (String k : keys) {
			a.put(k);
			a.put(combatStats.get(k));
		}
		o.put("combatStats", a);
		a = new JSONArray();
		a.put(damageTaken[0]).put(damageTaken[1]).put(damageTakenFromAbove[0]).put(damageTakenFromAbove[1]).put(damageMissed[0]).put(damageMissed[1]);
		o.put("damageStats", a);
		
		a = new JSONArray();
		for (Medal m : medals) {
			a.put(m.toJSON());
		}
		o.put("medals", a);
		return o;
	}
	
	public Airship(JSONObject o) { this(o, false, null); }
	
	public Airship(JSONObject o, boolean allowPartialLoad) { this(o, allowPartialLoad, null); }
	
	public Airship(JSONObject o, BonusSet overrideBaseBonuses) { this(o, false, overrideBaseBonuses); }
	
	public Airship(JSONObject o, boolean allowPartialLoad, BonusSet overrideBaseBonuses) {
		version = o.optInt("version", 0);
		name = o.getString("name");
		designName = o.optString("designName", null);
		networkID = o.optString("networkID", "[no network ID]");
		chunkSubIDCounter = o.optInt("chunkSubIDCounter", 1);
		type = ShipType.valueOf(o.optString("type", ShipType.AIRSHIP.name()));
		setX(o.getDouble("x")); setY(o.getDouble("y")); w = o.getInt("w"); h = o.getInt("h"); flipped = o.getBoolean("flipped");
		setxSpeed(o.optDouble("xSpeed", 0)); setySpeed(o.optDouble("ySpeed", 0));
		assignJobsMs = o.optInt("assignJobsMs", ASSIGN_JOBS_EVERY); commandPoints = o.optInt("commandPoints", 0);
		focusOnShooting = o.optBoolean("focusOnShooting", false);
		focusOnFirefighting = o.optBoolean("focusOnFirefighting", false);
		focusOnRepair = o.optBoolean("focusOnRepair", false);
		focusOnMoving = o.optBoolean("focusOnMoving", false);
		fireMode = FireMode.valueOf(o.optString("fireMode", FireMode.NORMAL.name()));
		aircraftMode = AircraftBehaviourMode.valueOf(o.optString("aircraftMode", AircraftBehaviourMode.NORMAL.name()));
		flipTo = o.optBoolean("flipTo", o.getBoolean("flipped"));
		flipMs = o.optInt("flipMs", 0);
		unableToFlipMs = o.optInt("unableToFlipMs", 0);
		msSinceOnGround = o.optInt("msSinceOnGround", 10000);
		captured = o.optBoolean("captured", false);
		sitting = o.optBoolean("sitting", false);
		grounding = o.optBoolean("grounding", false);
		msSinceLastXMove = o.optInt("msSinceLastXMove", 0);
		braking = o.optInt("braking", 0);
		hasBraked = o.optInt("hasBraked", 0);
		timeMovingInSameXDirection = o.optInt("timeMovingInSameXDirection", 0);
		movingLeft = o.optBoolean("movingLeft", false);
		enginesRunning = o.optBoolean("enginesRunning", false);
		suspendiumRunning = o.optBoolean("suspendiumRunning", false);
		outOfCombatMs = o.optInt("outOfCombatMs", 0);
		notUnderCommandMs = o.optInt("notUnderCommandMs", 0);
		popOutCooldown = o.optInt("popOutCooldown", 0);
		canDoPathingIndex = o.optInt("canDoPathingIndex", 0);
		boarderCanDoPathingIndex = o.optInt("boarderCanDoPathingIndex", 0);
		originalAmmoCapacity = o.optInt("originalAmmoCapacity", -1);
		originalCoalCapacity = o.optInt("originalCoalCapacity", -1);
		originalWaterCapacity = o.optInt("originalWaterCapacity", -1);
		originalRepairCapacity = o.optInt("originalRepairCapacity", -1);
		originalAllQuartered = o.optInt("originalAllQuartered", -1);
		isBonusConstruction = o.optBoolean("isBonusConstruction", false);
		releaseOneUseWeapons = o.optBoolean("releaseOneUseWeapons", false);
		releaseOneUseLift = o.optBoolean("releaseOneUseLift", false);
		releaseOneUsePropulsion = o.optBoolean("releaseOneUsePropulsion", false);
		switchedReserve = o.optBoolean("switchedReserve", false);
		aiControl = o.optBoolean("aiControl", false);
		mechTentacleDamageTaken = o.optInt("mechTentacleDamageTaken", 0);
		hussarDamageTaken = o.optInt("hussarDamageTaken", 0);
		sawDamageTaken = o.optInt("sawDamageTaken", 0);
		otherDamageTaken = o.optInt("otherDamageTaken", 0);
		fireRatePercentFromMedals = o.optInt("fireRatePercentFromMedals", 0);
		accuracyPercentFromMedals = o.optInt("accuracyPercentFromMedals", 0);
		crewSpeedPercentFromMedals = o.optInt("crewSpeedPercentFromMedals", 0);
		flammabilityPercentFromMedals = o.optInt("flammabilityPercentFromMedals", 0);
		explosionRiskPercentFromMedals = o.optInt("explosionRiskPercentFromMedals", 0);
		commandCooldownPercentFromMedals = o.optInt("commandCooldownPercentFromMedals", 0);
		repairAmountPercentFromMedals = o.optInt("repairAmountPercentFromMedals", 0);
		firefightAmountPercentFromMedals = o.optInt("firefightAmountPercentFromMedals", 0);
		propulsionPercentFromMedals = o.optInt("propulsionPercentFromMedals", 0);
		liftPercentFromMedals = o.optInt("liftPercentFromMedals", 0);
		fleetSpeedPercentFromMedals = o.optInt("fleetSpeedPercentFromMedals", 0);
		armourRepairPercentFromMedals = o.optInt("armourRepairPercentFromMedals", 0);
		surpriseAttackFromMedals = o.optBoolean("surpriseAttackFromMedals", false);
		inWater = o.optBoolean("inWater", false);
		if (o.has("captainType") && Loadable.hasOfName(HeroType.class, o.getString("captainType"))) {
			captainType = HeroType.ofName(o.getString("captainType"));
			captain = new Hero(captainType, null);
		}
		if (o.has("originalDesign")) {
			originalDesign = new Airship(o.getJSONObject("originalDesign"), allowPartialLoad, overrideBaseBonuses);
		}
		if (o.has("moveToX")) {
			moveTo = new Pt(o.getDouble("moveToX"), o.getDouble("moveToY"));
			moveMode = MoveMode.valueOf(o.optString("moveMode", MoveMode.DEFAULT.name()));
		}
		if (o.has("lastPlacedX")) {
			lastPlaced = new Pt(o.getDouble("lastPlacedX"), o.getDouble("lastPlacedY"));
			lastPlacedFlipped = o.getBoolean("lastPlacedFlipped");
		}
		ramming = o.optBoolean("ramming", false);
		JSONArray a;
		if (o.has("usedAbilities")) {
			a = o.getJSONArray("usedAbilities");
			for (int i = 0; i < a.length(); i++) {
				usedAbilities.add(HeroType.CombatAbility.valueOf(a.getString(i)));
			}
		}
		smokescreenTime = o.optInt("smokescreenTime", 0);
		burstOfSpeedTime = o.optInt("burstOfSpeedTime", 0);
		superchargeSuspendiumTime = o.optInt("superchargeSuspendiumTime", 0);
		doubleTimeTime = o.optInt("doubleTimeTime", 0);
		fearTime = o.optInt("fearTime", 0);
		tauntTime = o.optInt("tauntTime", 0);
		glimmerTime = o.optInt("glimmerTime", 0);
		crosswindsTime = o.optInt("crosswindsTime", 0);
		paralysisTime = o.optInt("paralysisTime", 0);
		cripplingShotTarget = o.optBoolean("cripplingShotTarget", false);
		disarmingShotTarget = o.optBoolean("disarmingShotTarget", false);
		gustOfWindTime = o.optInt("gustOfWindTime", 0);
		gustOfWindDX = o.optDouble("gustOfWindDX", 0);
		gustOfWindDY = o.optDouble("gustOfWindDY", 0);
		holdOnTime = o.optInt("holdOnTime", 0);
		crewExperience = o.optDouble("crewExperience", 0);
		suddenStormTime = o.optInt("suddenStormTime", 0);
		suddenStormDX = o.optDouble("suddenStormDX", 0);
		momentOfDoubtTime = o.optInt("momentOfDoubtTime", 0);
		blindnessTime = o.optInt("blindnessTime", 0);
		if (o.has("medals")) {
			a = o.getJSONArray("medals");
			for (int i = 0; i < a.length(); i++) {
				medals.add(new Medal(a.getJSONObject(i)));
			}
		}
		if (overrideBaseBonuses != null) {
			baseBonuses.addAll(overrideBaseBonuses);
		} else {
			if (o.has("currentBonuses")) {
				a = o.getJSONArray("currentBonuses");
				for (int i = 0; i < a.length(); i++) {
					if (Loadable.hasOfName(Bonus.class, a.getString(i))) {
						baseBonuses.add(Bonus.ofNameOrNone(a.getString(i)));
					}
				}
			}
		}
		if (allowPartialLoad) {
			HashMap<Integer, Module> moduleMapping = new HashMap<>();
			a = o.getJSONArray("modules");
			for (int i = 0; i < a.length(); i++) { 
				try {
					Module m = new Module(a.getJSONObject(i), this);
					moduleMapping.put(i, m);
					modules.add(m);
				} catch (Loadable.NotFoundException e) {
					// Don't load this module then.
				}
			}
			a = o.getJSONArray("tiles");
			for (int i = 0; i < a.length(); i++) {
				JSONObject to = a.getJSONObject(i);
				if (moduleMapping.containsKey(to.getInt("module"))) {
					try {
						tiles.add(new Tile(to, this, moduleMapping));
					} catch (Loadable.NotFoundException e) {
						// If we can't find the tile, we create a synthetic tile.
						tiles.add(new Tile(this, moduleMapping.get(to.getInt("module")), to.getInt("x"), to.getInt("y")));
					}
				}
			}
			// Don't load crew or boarders, because they will be regenerated.
			if (o.has("decals")) {
				a = o.getJSONArray("decals");
				for (int i = 0; i < a.length(); i++) {
					try {
						decals.add(new Decal(a.getJSONObject(i)));
					} catch (Loadable.NotFoundException e) { /* ignore */ }
				}
			}
		} else {
			a = o.getJSONArray("modules");
			for (int i = 0; i < a.length(); i++) { modules.add(new Module(a.getJSONObject(i), this)); }
			a = o.getJSONArray("tiles");
			for (int i = 0; i < a.length(); i++) { tiles.add(new Tile(a.getJSONObject(i), this, null)); }
			a = o.getJSONArray("crew");
			for (int i = 0; i < a.length(); i++) { crew.add(new Crewman(a.getJSONObject(i), this, null)); }
			if (o.has("boarders")) {
				a = o.getJSONArray("boarders");
				for (int i = 0; i < a.length(); i++) { boarders.add(new Crewman(a.getJSONObject(i), null, this)); }
			}
			if (o.has("decals")) {
				a = o.getJSONArray("decals");
				for (int i = 0; i < a.length(); i++) { decals.add(new Decal(a.getJSONObject(i))); }
			}
		}
		if (o.optInt("boardExitTargetCache", -1) != -1) {
			boardExitTargetCache = tiles.get(o.getInt("boardExitTargetCache"));
		}
		if (o.has("combatStats")) {
			a = o.getJSONArray("combatStats");
			for (int i = 0; i < a.length(); i += 2) {
				combatStats.put(a.getString(i), a.getInt(i + 1));
			}
		}
		if (o.has("damageStats")) {
			a = o.getJSONArray("damageStats");
			damageTaken[0] = a.getInt(0);
			damageTaken[1] = a.getInt(1);
			damageTakenFromAbove[0] = a.getInt(2);
			damageTakenFromAbove[1] = a.getInt(3);
			damageMissed[0] = a.getInt(4);
			damageMissed[1] = a.getInt(5);
		}
		recalculateBonuses();
		if (allowPartialLoad) {
			layout();
			repair(/* resetXP */ false);
		} else {
			checkAndRepairTilesAndCrew();
			layout();	
			clearInvalidDecals();
			recalcHPs();
		}
		resetTentacles();
	}
	
	public void clearTargeting() {
		for (int i = 0; i < modules.size(); i++) {
			modules.get(i).prevTarget = null;
			modules.get(i).prevTargetShip = null;
		}
	}

	public void finishWithCombat(JSONObject o, Combat combat) {
		multiplayerControllerID = o.optInt("multiplayerControllerID", 0);
		if (o.has("owner")) {
			owner = new FleetOwnerRef(o.getJSONObject("owner"));
		}
		if (o.has("originalArms")) {
			originalArms = new CoatOfArms(o.getJSONObject("originalArms"));
		}
		JSONArray a = o.getJSONArray("modules");
		for (int i = 0; i < a.length(); i++) {
			modules.get(i).finishWithCombat(a.getJSONObject(i), combat);
		}
		if (o.has("fireAtSideIndex")) {
			fireAt = combat.sides.get(o.getInt("fireAtSideIndex")).ships.get(o.getInt("fireAtShipIndex"));
		}
		if (o.has("boardSideIndex")) {
			board = combat.sides.get(o.getInt("boardSideIndex")).ships.get(o.getInt("boardShipIndex"));
		}
		if (o.has("tetherAtSideIndex") && o.has("tetherAtShipIndex")) {
			tetherAt = combat.sides.get(o.getInt("tetherAtSideIndex")).ships.get(o.getInt("tetherAtShipIndex"));
		}
		a = o.getJSONArray("crew");
		for (int i = 0; i < a.length(); i++) { crew.get(i).finishLoadingWithCombat(a.getJSONObject(i), combat); }
		if (o.has("boarders")) {
			a = o.getJSONArray("boarders");
			for (int i = 0; i < a.length(); i++) { boarders.get(i).finishLoadingWithCombat(a.getJSONObject(i), combat); }
		}
		if (o.has("lastGrounded")) {
			lastGrounded = combat.landFormations.get(o.getInt("lastGrounded"));
		}
		if (o.has("jumpPointCacheTargetLF")) {
			jumpPointCacheTarget = combat.landFormations.get(o.getInt("jumpPointCacheTargetLF"));
			jumpPointCache = new GridRef(o.getInt("jumpPointCacheX"), o.getInt("jumpPointCacheY"), this);
		}
		if (o.has("jumpPointCacheTargetSide")) {
			jumpPointCacheTarget = combat.sides.get(o.getInt("jumpPointCacheTargetSide")).ships.get(o.getInt("jumpPointCacheTargetShip"));
			jumpPointCache = new GridRef(o.getInt("jumpPointCacheX"), o.getInt("jumpPointCacheY"), this);
		}
		if (o.has("fallPointCacheTargetLF")) {
			fallPointCacheTarget = combat.landFormations.get(o.getInt("fallPointCacheTargetLF"));
			fallPointCache = new GridRef(o.getInt("fallPointCacheX"), o.getInt("fallPointCacheY"), this);
		}
		if (o.has("fallPointCacheTargetSide")) {
			fallPointCacheTarget = combat.sides.get(o.getInt("fallPointCacheTargetSide")).ships.get(o.getInt("fallPointCacheTargetShip"));
			fallPointCache = new GridRef(o.getInt("fallPointCacheX"), o.getInt("fallPointCacheY"), this);
		}
	}

	public Airship(ShipType type, Locale l, GuardedRandom r) {
		this.type = type;
		name = ConstructionName.getName(null, null, l, type == ShipType.BUILDING, r);
	}
	
	public Airship(ShipType type) {
		this.type = type;
		name = "?";
	}
	
	public int gridXToWorldX(int xVal, int spriteW) {
		return flipped ? w - xVal - spriteW : xVal;
	}

	public int gridXToWorldX(int xVal, int spriteW, boolean fl) {
		return fl ? w - xVal - spriteW : xVal;
	}

	public void drawOutline(Draw d, double x, double y, boolean oFlipped, Clr c) {
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			d.rect(c, x + gridXToWorldX(t.x, 1, oFlipped) * AGame.SGS, y + t.y * AGame.SGS, AGame.SGS, AGame.SGS);
		}
	}
	
	static String nt = "[bb421d]" + _t("No_target_available");
	static String nba = "[bb421d]" + _t("No_arc_available");

	public void drawFireArcs(Draw d, double scrollX, double scrollY, double zoom, boolean showValids) {
		boolean unavailableShown = false;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.isWeapon() && (showValids || (m.noValidTarget && (m.type.getTetherSpec(currentBonuses) == null || m.ship.tetherAt != null))) && m.hp > 0) {
				if (!showValids && m.noArcFound) {
					Pt mz = m.fireFrom();
					int mX = (int) ((mz.x + scrollX) * zoom);
					int mY = (int) ((mz.y + scrollY) * zoom);
					int xOffset = 10;
					/*if (m.type.isBackOnly() != flipped) { // qqDPS???
						xOffset = -10 - (int) d.textSize(nt, AGame.FOUNT).x;
					}*/
					d.text(nba, AGame.FOUNT, mX + xOffset, mY - 5);
				} else {
					drawFireArc(m, d, scrollX, scrollY, zoom, showValids || !m.noValidTarget);
					if (!showValids && m.noValidTarget && !unavailableShown) {
						Pt mz = m.fireFrom();
						int mX = (int) ((mz.x + scrollX) * zoom);
						int mY = (int) ((mz.y + scrollY) * zoom);
						int xOffset = 10;
						/*if (m.type.isBackOnly() != flipped) { // qqDPS???
							xOffset = -10 - (int) d.textSize(nt, AGame.FOUNT).x;
						}*/
						d.text(nt, AGame.FOUNT, mX + xOffset, mY - 5);
						unavailableShown = true;
					}
				}
			}
		}
	}

	public static Color FIRE_ARC_ARC = new Color(255, 255, 255, 120);
	public static Color FIRE_ARC_LINE = new Color(255, 255, 255, 180);
	public static Color FIRE_ARC_DISABLED_ARC = new Color(187, 66, 29, 120);
	public static Color FIRE_ARC_DISABLED_LINE = new Color(187, 66, 29);
	
	public void drawFireArc(Module m, Draw d, double scrollX, double scrollY, double zoom, boolean enabled) {
		Pt mz = m.fireFrom();
		double mX = (mz.x + scrollX) * zoom;
		double mY = (mz.y + scrollY) * zoom;
		mz = new Pt(mX, mY);
		Arc arc = m.type.getFireArc(currentBonuses);
		if (flipped) { arc = arc.flipHorizontal(); }
		Graphics g = (Graphics) d.frame().nativeRenderer();
		if (m.type.getFireArc(currentBonuses).sizeRadians >= Math.PI * 2 - 0.01) {
			g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
			g.drawArc((float) (mX - 60), (float) (mY - 60), 120, 120, 0, 360);
			float maxR = (float) (m.type.getMaxRange(currentBonuses) * zoom);
			if (maxR != 0) {
				g.drawArc((float) (mX - maxR), (float) (maxR - 60), maxR * 2, maxR * 2, 0, 360);
			}
			maxR = (float) (m.type.getMaxXRange(currentBonuses) * zoom);
			if (maxR != 0) {
				g.drawLine((float) mX - maxR, (float) mY - 60, (float) mX - maxR, (float) mY + 60);
				g.drawLine((float) mX + maxR, (float) mY - 60, (float) mX + maxR, (float) mY + 60);
			}
			maxR = (float) (m.type.getMaxUpRange(currentBonuses) * zoom);
			if (maxR != 0) {
				g.drawLine((float) mX - 60, (float) mY - maxR, (float) mX + 60, (float) mY - maxR);
			}
			
			float minR = (float) (m.type.getMinXRange(currentBonuses) * zoom);
			if (minR != 0) {
				g.drawLine((float) mX - minR, (float) mY - 60, (float) mX - minR, (float) mY + 60);
				g.drawLine((float) mX + minR, (float) mY - 60, (float) mX + minR, (float) mY + 60);
				g.drawLine((float) mX - 60, (float) mY, (float) mX - minR, (float) mY);
				g.drawLine((float) mX + 60, (float) mY, (float) mX + minR, (float) mY);
			}
		} else if (m.type.getMaxUpRange(currentBonuses) > 0 && m.type.getMaxXRange(currentBonuses) > 0) {
			if (m.type.isFlipped()) {
				double upRange = m.type.getMaxUpRange(currentBonuses) * zoom;
				double xRange = m.type.getMaxXRange(currentBonuses) * zoom;
				double backAmt = upRange / StrictMath.tan(flipped ? Direction.flipHorizontal(arc.from.radians) : arc.to.radians);
				Pt intersect = new Pt(mz.x + (flipped ? backAmt : -backAmt), mz.y - upRange);
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				// Line from muzzle to intersect
				g.drawLine((float) mX, (float) mY, (float) intersect.x, (float) intersect.y);
				// Line from muzzle to other end
				Pt end = (flipped ? arc.to : arc.from).fromBy(mz, 60);
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
				end = (flipped ? arc.to : arc.from).fromBy(mz, 80);
				g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
				// Line from intersect to end
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				g.drawLine((float) (intersect.x), (float) (mY - upRange), (float) (mX + (flipped ? xRange : -xRange)), (float) (mY - upRange));
				g.drawLine((float) (mX + (flipped ? xRange : -xRange)), (float) (mY - upRange), (float) (mX + (flipped ? xRange : -xRange)), (float) (mY + 60));
				g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
				g.drawLine((float) (mX + (flipped ? xRange : -xRange)), (float) (mY - upRange), (float) (mX + (flipped ? xRange : -xRange)), (float) (mY + 80));
			} else {
				double upRange = m.type.getMaxUpRange(currentBonuses) * zoom;
				double xRange = m.type.getMaxXRange(currentBonuses) * zoom;
				double backAmt = upRange / StrictMath.tan(flipped ? Direction.flipHorizontal(arc.to.radians) : arc.from.radians);
				Pt intersect = new Pt(mz.x + (flipped ? backAmt : -backAmt), mz.y - upRange);
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				// Line from muzzle to intersect
				g.drawLine((float) mX, (float) mY, (float) intersect.x, (float) intersect.y);
				// Line from muzzle to other end
				Pt end = (flipped ? arc.from : arc.to).fromBy(mz, 60);
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
				end = (flipped ? arc.from : arc.to).fromBy(mz, 80);
				g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
				// Line from intersect to end
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				g.drawLine((float) (intersect.x), (float) (mY - upRange), (float) (mX + (flipped ? -xRange : xRange)), (float) (mY - upRange));
				g.drawLine((float) (mX + (flipped ? -xRange : xRange)), (float) (mY - upRange), (float) (mX + (flipped ? -xRange : xRange)), (float) (mY + 60));
				g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
				g.drawLine((float) (mX + (flipped ? -xRange : xRange)), (float) (mY - upRange), (float) (mX + (flipped ? -xRange : xRange)), (float) (mY + 80));
			}
		} else if (m.type.getMaxUpRange(currentBonuses) > 0) {
			if (m.type.getMaxUpRange(currentBonuses) > 200) {
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				double minX = mz.x, maxX = mz.x;
				Pt start = arc.from.fromBy(mz, 180);
				minX = StrictMath.min(minX, start.x);
				maxX = StrictMath.max(maxX, start.x);
				g.drawLine((float) mX, (float) mY, (float) start.x, (float) start.y);
				Pt end = arc.to.fromBy(mz, 180);
				minX = StrictMath.min(minX, end.x);
				maxX = StrictMath.max(maxX, end.x);
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
				g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
				start = arc.from.fromBy(mz, 200);
				g.drawLine((float) mX, (float) mY, (float) start.x, (float) start.y);
				end = arc.to.fromBy(mz, 200);
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
				g.drawArc((float) (mX - 90), (float) (mY - 90), 180, 180, (float) arc.from.getDegrees(), (float) arc.to.getDegrees());
				if (m.type.getMaxRange(currentBonuses) > 0) {
					double maxRange = m.type.getMaxRange(currentBonuses) * zoom;
					g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
					g.drawArc((float) (mX - maxRange), (float) (mY - maxRange), (float) maxRange * 2, (float) maxRange * 2, (float) arc.from.getDegrees(), (float) arc.to.getDegrees());
				}
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				g.drawLine((float) minX, (float) (mz.y - m.type.getMaxUpRange(currentBonuses) * zoom), (float) maxX, (float) (mz.y - m.type.getMaxUpRange(currentBonuses) * zoom));
			} else {
				if (m.type.isFlipped()) {
					double upRange = m.type.getMaxUpRange(currentBonuses) * zoom;
					double xRange = upRange + 100 * zoom;
					double backAmt = upRange / StrictMath.tan(flipped ? Direction.flipHorizontal(arc.from.radians) : arc.to.radians);
					Pt intersect = new Pt(mz.x + (flipped ? backAmt : -backAmt), mz.y - upRange);
					g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
					// Line from muzzle to intersect
					g.drawLine((float) mX, (float) mY, (float) intersect.x, (float) intersect.y);
					// Line from muzzle to other end
					Pt end = (flipped ? arc.to : arc.from).fromBy(mz, 60);
					g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
					end = (flipped ? arc.to : arc.from).fromBy(mz, 80);
					g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
					g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
					// Line from intersect to end
					g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
					g.drawLine((float) (intersect.x), (float) (mY - upRange), (float) (mX + (flipped ? xRange : -xRange)), (float) (mY - upRange));				//g.drawLine((float) (mX + (flipped ? xRange : -xRange)), (float) (mY - upRange), (float) (mX + (flipped ? xRange : -xRange)), (float) (mY + 80));
				} else {
					double upRange = m.type.getMaxUpRange(currentBonuses) * zoom;
					double xRange = upRange + 100 * zoom;
					double backAmt = upRange / StrictMath.tan(flipped ? Direction.flipHorizontal(arc.to.radians) : arc.from.radians);
					Pt intersect = new Pt(mz.x + (flipped ? backAmt : -backAmt), mz.y - upRange);
					g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
					// Line from muzzle to intersect
					g.drawLine((float) mX, (float) mY, (float) intersect.x, (float) intersect.y);
					// Line from muzzle to other end
					Pt end = (flipped ? arc.from : arc.to).fromBy(mz, 60);
					g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
					end = (flipped ? arc.from : arc.to).fromBy(mz, 80);
					g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
					g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
					// Line from intersect to end
					g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
					g.drawLine((float) (intersect.x), (float) (mY - upRange), (float) (mX + (flipped ? -xRange : xRange)), (float) (mY - upRange));
				}
			}
		} else if (m.type.getMaxXRange(currentBonuses) > 0) {
			double xRange = m.type.getMaxXRange(currentBonuses) * zoom;
			if (flipped ^ m.type.isFlipped()) { xRange = -xRange; }
			g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
			// Assume pointing forward for now.
			double yStart = -90;
			double yEnd = 90;
			boolean useYStart = false;
			boolean useYEnd = false;
			if (arc.from.radians != StrictMath.PI / 2 && arc.from.radians != StrictMath.PI * 3 / 2) {
				yStart = StrictMath.tan(arc.from.radians) * xRange;
				useYStart = true;
			}
			if (arc.to.radians != StrictMath.PI / 2 && arc.to.radians != StrictMath.PI * 3 / 2) {
				yEnd = StrictMath.tan(arc.to.radians) * xRange;
				useYEnd = true;
			}
			g.drawLine((float) (mX + xRange), (float) (mY + yStart), (float) (mX + xRange), (float) (mY + yEnd));
			Pt start = useYStart ? new Pt(mz.x + xRange, mz.y + yStart) : arc.from.fromBy(mz, 180);
			g.drawLine((float) mX, (float) mY, (float) start.x, (float) start.y);
			Pt end = useYEnd ? new Pt(mz.x + xRange, mz.y + yEnd) : arc.to.fromBy(mz, 180);
			g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
			g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
			start = arc.from.fromBy(mz, 200);
			if (!useYStart) {
				g.drawLine((float) mX, (float) mY, (float) start.x, (float) start.y);
			}
			end = arc.to.fromBy(mz, 200);
			if (!useYEnd) {
				g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
			}
			g.drawArc((float) (mX - 90), (float) (mY - 90), 180, 180, (float) arc.from.getDegrees(), (float) arc.to.getDegrees());
		} else if (m.type.getMinXRange(currentBonuses) > 0) {
			boolean doFlip = flipped ^ m.type.isFlipped();
			double xRange = m.type.getMinXRange(currentBonuses) * zoom;
			double flipMult = doFlip ? -1 : 1;
			double rangeStartX = mX + xRange * flipMult;
			double rangeStartYFrom = mY + StrictMath.tan(arc.from.radians) * xRange * flipMult;
			double rangeStartYTo = mY + StrictMath.tan(arc.to.radians) * xRange * flipMult;
			double rangeExtendX = mX + (xRange + 90) * flipMult;
			double rangeExtendYFrom = mY + StrictMath.tan(arc.from.radians) * (xRange + 90) * flipMult;
			double rangeExtendYTo = mY + StrictMath.tan(arc.to.radians) * (xRange + 90) * flipMult;
			double rangeExtend2X = mX + (xRange + 110) * flipMult;
			double rangeExtend2YFrom = mY + StrictMath.tan(arc.from.radians) * (xRange + 110) * flipMult;
			double rangeExtend2YTo = mY + StrictMath.tan(arc.to.radians) * (xRange + 110) * flipMult;
			g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
			g.drawLine((float) rangeStartX, (float) rangeStartYFrom, (float) rangeExtendX, (float) rangeExtendYFrom);
			g.drawLine((float) rangeStartX, (float) rangeStartYTo, (float) rangeExtendX, (float) rangeExtendYTo);
			g.drawLine((float) rangeStartX, (float) rangeStartYFrom, (float) rangeStartX, (float) rangeStartYTo);
			g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
			g.drawLine((float) rangeStartX, (float) rangeStartYFrom, (float) rangeExtend2X, (float) rangeExtend2YFrom);
			g.drawLine((float) rangeStartX, (float) rangeStartYTo, (float) rangeExtend2X, (float) rangeExtend2YTo);
			g.drawLine((float) mX, (float) mY, (float) rangeStartX, (float) (rangeStartYFrom / 2 + rangeStartYTo / 2));
		} else {
			g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
			Pt start = arc.from.fromBy(mz, 180);
			g.drawLine((float) mX, (float) mY, (float) start.x, (float) start.y);
			Pt end = arc.to.fromBy(mz, 180);
			g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
			g.setColor(enabled ? FIRE_ARC_ARC : FIRE_ARC_DISABLED_ARC);
			start = arc.from.fromBy(mz, 200);
			g.drawLine((float) mX, (float) mY, (float) start.x, (float) start.y);
			end = arc.to.fromBy(mz, 200);
			g.drawLine((float) mX, (float) mY, (float) end.x, (float) end.y);
			g.drawArc((float) (mX - 90), (float) (mY - 90), 180, 180, (float) arc.from.getDegrees(), (float) arc.to.getDegrees());
			if (m.type.getMaxRange(currentBonuses) > 0) {
				double maxRange = m.type.getMaxRange(currentBonuses) * zoom;
				g.setColor(enabled ? FIRE_ARC_LINE : FIRE_ARC_DISABLED_LINE);
				g.drawArc((float) (mX - maxRange), (float) (mY - maxRange), (float) maxRange * 2, (float) maxRange * 2, (float) arc.from.getDegrees(), (float) arc.to.getDegrees());
			}
		}
	}
	
	public void drawAsBlueprint(MyDraw d, double x, double y, int ms, boolean outside, float intensity) {
		if (outside) {
			int tsz = tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = tiles.get(ti);
				if (t.armour.type == ArmourType.ofName("NONE")) {
					continue;
				}
				t.armour.getApp().drawAsBlueprint(
						d, x + gridXToWorldX(t.x, 1) * AGame.SGS, y + t.y * AGame.SGS,
						AGame.SGS, AGame.SGS,
						ms, flipped, intensity);
			}
		} else {
			int msz = modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = modules.get(mi);
				m.type.drawAsBlueprint(d,
							x + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + m.y * AGame.SGS,
							m.time + m.animOffset,
							flipped,
							m.variant, currentBonuses,
							intensity);
				WeaponAppearance wa = m.type.weaponAppearance(currentBonuses);
				if (wa != null && !Appearance.useSimpleGraphics) {
					Img barrel = flipped ? wa.flippedbarrel : wa.barrel;
					if (wa.barrelAnimation != null) {
						barrel = flipped ? wa.flippedBarrelAnimation.frames.get(0) : wa.barrelAnimation.frames.get(0);
					}
					Pt offset = flipped ? wa.flippedBarrelOffset : wa.barrelOffset;
					if (barrel != null && m.hp > 0) {
						Appearance.drawAsBlueprint(barrel, d,
								x + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + offset.x,
								y + m.y * AGame.SGS + offset.y,
								barrel.srcWidth,
								barrel.srcHeight,
								flipped != m.type.isFlipped(),
								intensity);
					}
				}
			}
		}
	}
	
	private final transient HashSet<SpritesheetBundle> additionalSSBs = new HashSet<SpritesheetBundle>();
	private final transient HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs = new HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>>();
	
	public void draw(MyDraw d,
			double x, double y,
			int ms, boolean outside, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, boolean showHPBarsAndDoors,
			Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint)
	{
		for (UniScreen.ShipLayer l : ShipLayers.ALL) {
			if (!l.doDraw(1.0)) { continue; }
			additionalSSBs.clear();
			additionalSSBPairs.clear();
			SpritesheetBundle baseSSB = l.getBaseSSB();
			SpritesheetBundle baseSSB2 = l.getBaseSSB2();
			if (baseSSB2 == null) {
				l.lockShader(baseSSB, null, d, 1.0, light, lightStrength, ambient, ambientSaturation);
				l.draw(this, null, 0, d, outside, 1.0, x, y, ms, displayStatusIcons, showDecals, coa, coa, showHPBarsAndDoors, light, lightStrength, ambient, ambientSaturation, ambientTint, baseSSB, additionalSSBs, null, null);
				l.unlockShader(light, 1.0);
				for (SpritesheetBundle ssb : additionalSSBs) {
					l.lockShader(ssb, null, d, 1.0, light, lightStrength, ambient, ambientSaturation);
					l.draw(this, null, 0, d, outside, 1.0, x, y, ms, displayStatusIcons, showDecals, coa, coa, showHPBarsAndDoors, light, lightStrength, ambient, ambientSaturation, ambientTint, ssb, null, null, null);
					l.unlockShader(light, 1.0);
				}
			} else {
				l.lockShader(baseSSB, baseSSB2, d, 1.0, light, lightStrength, ambient, ambientSaturation);
				l.draw(this, null, 0, d, outside, 1.0, x, y, ms, displayStatusIcons, showDecals, coa, coa, showHPBarsAndDoors, light, lightStrength, ambient, ambientSaturation, ambientTint, baseSSB, null, baseSSB2, additionalSSBPairs);
				l.unlockShader(light, 1.0);
				for (Utils.Pair<SpritesheetBundle, SpritesheetBundle> ssbp : additionalSSBPairs) {
					l.lockShader(ssbp.a, ssbp.b, d, 1.0, light, lightStrength, ambient, ambientSaturation);
					l.draw(this, null, 0, d, outside, 1.0, x, y, ms, displayStatusIcons, showDecals, coa, coa, showHPBarsAndDoors, light, lightStrength, ambient, ambientSaturation, ambientTint, ssbp.a, null, ssbp.b, null);
					l.unlockShader(light, 1.0);
				}
			}
			
			if (RotatingShader.shaderLocked) {
				AirshipGame.instance.reportError("Rotating shader still locked after " + l.getClass().getSimpleName(), null, null, false, true);
				RotatingShader.unlockShader();
			}
			
			Appearance.unlockMaskedBevelledShader(light != null);
			Appearance.unlockShader(light != null);
			Appearance.unlockSubShader(light != null);
			RotatingShader.unlockShader();
			RotatingColoringShader.unlockShader();
		}
	}

	public static final Clr DOOR_FREE = new Clr(180, 255, 180);
	public static final Clr DOOR_PRESENT = Clr.fromHex("4d370d");
	//private static final Clr DOOR_BLOCKED = new Clr(255, 200, 200);
	public static final Clr DISCONNECTED = new Clr(200, 30, 30, 90);
	
	public boolean hasInaccessibleModules() {
		return pathChunks(null).size() > 1 || chunks(null).size() > 1;
	}
	
	public void hookModuleTooltips(MyDraw d, double x, double y, double zoom, final boolean explain) {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			final Module m = modules.get(mi);
			d.tooltip((x + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS) * zoom,
					(y + m.y * AGame.SGS) * zoom,
					m.type.getW() * AGame.SGS * zoom,
					m.type.getH() * AGame.SGS * zoom,
					new MyDraw.Tooltip() {
						@Override
						public String get() {
							return m.type.getDescription(currentBonuses, type, explain);
						}
					}
			);
		}
	}

	public int getCost() {
		int c = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			c += m.type.getCost(currentBonuses);
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			c += t.armour.type.getCost(currentBonuses);
		}
		return (int) (c * EmpireStat.SHIP_TYPE_COST_MULTIPLIER.get(type).get(currentBonuses));
	}
	
	public int getResourceAdjustedStrength() {
		int str = getCost();
		int coalCap = getCoalCapacity();
		if (coalCap > 0) {
			int coal = getTotalResource(Resource.COAL);
			if (coal < coalCap * 0.7) {
				str = (int) (str * coal / (coalCap * 0.7));
			}
		}
		
		int ammoCap = getAmmoCapacity();
		if (ammoCap > 0) {
			int ammo = getTotalResource(Resource.AMMO);
			if (ammo < ammoCap * 0.7) {
				str = (int) (str * ammo / (ammoCap * 0.7));
			}
		}
		
		int waterCap = getWaterCapacity();
		if (waterCap > 0) {
			str = (int) (str * 0.75 + str * 0.25 * getTotalResource(Resource.WATER) / waterCap);
		}
		
		int repairCap = getRepairCapacity();
		if (repairCap > 0) {
			str = (int) (str * 0.75 + str * 0.25 * getTotalResource(Resource.REPAIR) / repairCap);
		}
		
		str *= (1 + crewExperience);
		
		return str;
	}
	
	public int getRefitCostFrom(Airship from, boolean detailedCalculation) {
		if (!detailedCalculation) {
			return tiles.size() * 4;
		}
		HashMap<ModuleType, Integer> myMCount = new HashMap<ModuleType, Integer>();
		for (ModuleType mt : Loadable.all(ModuleType.class)) {
			myMCount.put(mt, 0);
		}
		for (Module m : modules) { myMCount.put(m.type, myMCount.get(m.type) + 1); }
		HashMap<ArmourType, Integer> myACount = new HashMap<ArmourType, Integer>();
		for (ArmourType at : Loadable.all(ArmourType.class)) {
			myACount.put(at, 0);
		}
		for (Tile t : tiles) { myACount.put(t.armour.type, myACount.get(t.armour.type) + 1); }
		HashMap<ModuleType, Integer> oMCount = new HashMap<ModuleType, Integer>();
		for (ModuleType mt : Loadable.all(ModuleType.class)) {
			oMCount.put(mt, 0);
		}
		for (Module m : from.modules) { oMCount.put(m.type, oMCount.get(m.type) + 1); }
		HashMap<ArmourType, Integer> oACount = new HashMap<ArmourType, Integer>();
		for (ArmourType at : Loadable.all(ArmourType.class)) {
			oACount.put(at, 0);
		}
		for (Tile t : from.tiles) { oACount.put(t.armour.type, oACount.get(t.armour.type) + 1); }
		
		int cost = 1;
		for (ModuleType mt : Loadable.all(ModuleType.class)) {
			int m = myMCount.get(mt);
			int o = oMCount.get(mt);
			if (m > o) {
				cost += (m - o) * mt.getCost(currentBonuses);
			} else {
				cost += (m - o) * mt.getCost(currentBonuses) / 4;
			}
		}
		for (ArmourType at : Loadable.all(ArmourType.class)) {
			int m = myACount.get(at);
			int o = oACount.get(at);
			if (m > o) {
				cost += (m - o) * at.getCost(currentBonuses);
			} else {
				cost += (m - o) * at.getCost(currentBonuses) / 4;
			}
		}
		int amt = (int) (cost * EmpireStat.SHIP_TYPE_COST_MULTIPLIER.get(type).get(currentBonuses));
		return amt > 0 ? amt : amt / 8;
	}
	
	public int getRequiredCrew() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getCrew(currentBonuses);
		}
		return n;
	}
	
	public int getRequiredGuards() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getFixedGuards(currentBonuses);
		}
		return n;
	}
	
	public boolean hasCrewType(CrewType ct) {
		int csz = crew.size();
		for (int i = 0; i < csz; i++) {
			if (crew.get(i).type == ct) { return true; }
		}
		return false;
	}
	
	public boolean hasCrewTypeAny(Collection<CrewType> cts) {
		int csz = crew.size();
		for (int i = 0; i < csz; i++) {
			if (cts.contains(crew.get(i).type)) { return true; }
		}
		return false;
	}
	
	public boolean hasCrewQuartersAny(Collection<CrewType> cts) {
		int msz = modules.size();
		for (int i = 0; i < msz; i++) {
			if (cts.contains(modules.get(i).type.getQuartersType(currentBonuses))) { return true; }
		}
		return false;
	}
	
	public boolean canFly() {
		return !type.onGround;//!type.onGround || serviceCeiling() < AGame.GROUND_LEVEL;
	}
	
	public ArrayList<CrewType> getBoarderTypes() {
		ArrayList<CrewType> cts = new ArrayList<CrewType>();
		int csz = crew.size();
		for (int i = 0; i < csz; i++) {
			Crewman cm = crew.get(i);
			if (cm.type.canBoard && !cts.contains(cm.type)) {
				cts.add(cm.type);
			}
		}
		return cts;
	}
	
	public boolean canBeBoarded() {
		boolean hasInteriorSpaces = false;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getPreventsBoarding()) {
				return false;
			}
			hasInteriorSpaces = hasInteriorSpaces || m.type.getOccupableTileCount() > 0;
		}
		return hasInteriorSpaces;
	}
	
	public boolean nonCombat() {
		return modules.size() == 1 && modules.get(0).type.nonCombat();
		/*int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.nonCombat()) { return true; }
		}
		return false;*/
	}
	
	public boolean preventsSurrender() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getPreventsSurrender()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasActiveCrew() {
		int csz = crew.size();
		for (int i = 0; i < csz; i++) {
			if (crew.get(i).active()) { return true; }
		}
		int msz = modules.size();
		for (int i = 0; i < msz; i++) {
			if (modules.get(i).type.getCountsAsActiveCrew()) { return true; }
		}
		return false;
	}
	
	public boolean inCombat(Combat c) {
		return hasActiveCrew() && (canShoot() || (canMove() && canGenerateCommands()) || hasActiveFlyers(c) || hasActiveBoarders(c));
	}
	
	public boolean ableToContributeToCombat(Combat c) {
		if (hasActiveCrew() && ((canMove() && canGenerateCommands()) || hasActiveFlyers(c) || hasActiveBoarders(c))) { return true; }
		boolean hasAmmo = getTotalResource(Resource.AMMO) > 0;
		boolean hasCoal = getTotalResource(Resource.COAL) > 0;
		// Otherwise, can it actually shoot anything?
		Combat.Side otherSide = c.otherSide(c.sideOf(this));
		if (otherSide == null) { return false; }
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.hp <= 0 || !m.type.isWeapon() || (!hasAmmo && m.ammoLeft == 0 && m.type.getClip(currentBonuses) > 0) || (!hasCoal && m.msUntilCoal == 0 && m.type.getCoalReload(currentBonuses) > 0) || !m.somewhatStaffed()) { continue; }
			for (int ei = 0; ei < otherSide.ships.size(); ei++) {
				Airship enemy = otherSide.ships.get(ei);
				if (m.canHit(enemy, getX(), getY(), enemy.getX(), enemy.getY(), flipped, TacticalAI.INSET)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean hasFlyers() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getQuartersType(currentBonuses) != null && m.type.getQuartersType(currentBonuses).canFly) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasActiveFlyers(Combat c) {
		if (!hasFlyers()) { return false; }
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = crew.get(ci);
			if (cm.alive() && cm.type.canFly) { return true; }
		}
		Side s = c.sideOf(this);
		if (s == null) { return false; }
		int tsz = s.troops.size();
		for (int ti = 0; ti < tsz; ti++) {
			Crewman t = s.troops.get(ti);
			if (t.alive() && t.type.canFly) { return true; }
		}
		return false;
	}
	
	public boolean hasActiveBoarders(Combat c) {
		boolean hasBoarders = false;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getQuartersType(currentBonuses) != null && m.type.getQuartersType(currentBonuses).canBoard) {
				hasBoarders = true;
				break;
			}
		}
		if (!hasBoarders) { return false; }
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = crew.get(ci);
			if (cm.alive() && cm.type.canBoard) { return true; }
		}
		Side s = c.sideOf(this);
		if (s == null) { return false; }
		int tsz = s.troops.size();
		for (int ti = 0; ti < tsz; ti++) {
			if (s.troops.get(ti).type.canBoard) {
				return true;
			}
		}
		s = c.otherSide(s);
		int ssz = s.ships.size();
		for (int si = 0; si < ssz; si++) {
			Airship ship = s.ships.get(si);
			int bsz = ship.boarders.size();
			for (int bi = 0; bi < bsz; bi++) {
				if (ship.boarders.get(bi).active()) { return true; }
			}
		}
		return false;
	}
	
	public boolean hasModuleTypeAny(ModuleType... mts) {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			for (int mti = 0; mti < mts.length; mti++) {
				if (m.type == mts[mti]) { return true; }
			}
		}
		return false;
	}
	
	public boolean hasModuleTypeAny(Collection<ModuleType> mts) {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (mts.contains(m.type)) { return true; }
		}
		return false;
	}
	
	public boolean isBeingBoarded() {
		int bsz = boarders.size();
		for (int bi = 0; bi < bsz; bi++) {
			if (boarders.get(bi).active()) { return true; }
		}
		return false;
	}

	public int getRecommendedCrew() {
		int n = 2;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getRecommendedCrew(currentBonuses);
		}
		return n;
	}
	
	public int getQuartered(CrewType ct) {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getQuartersType(currentBonuses) == ct) {
				n += m.type.getQuarters(currentBonuses);
			}
		}
		return n;
	}
	
	public int getQuartered(Collection<CrewType> cts) {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (cts.contains(m.type.getQuartersType(currentBonuses))) {
				n += m.type.getQuarters(currentBonuses);
			}
		}
		return n;
	}
	
	public int getAllQuartered() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getQuarters(currentBonuses);
		}
		return n;
	}

	public void updateWeight() {
		weight = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			weight += m.type.getWeight(currentBonuses);
			if (m.type.isWeapon() && !m.type.canResupplyInCombat(currentBonuses) && m.ammoLeft == 0) {
				weight -= m.type.getFiredWeightDecrease(currentBonuses);
			}
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			weight += t.armour.type.getWeight(currentBonuses);
		}
	}

	public int getWeight() {
		return weight;
	}
	
	public int maintenanceCost() {
		double c = 0;
		int msz = modules.size();
		double mcps = EmpireStat.MAINTENANCE_COST_PER_SUPPLY.get(currentBonuses);
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			c += m.type.maintenanceCost(currentBonuses, type.supplyCostsMaintenance, mcps);
		}
		c *= EmpireStat.SHIP_TYPE_MAINTENANCE_MULTIPLIER.get(type).get(currentBonuses);
		return c == 0 ? 1 : (int) Math.ceil(c);
	}
	
	public int getSupplyProvided() {
		int s = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			s += m.type.getSupplyProvided(currentBonuses);
		}
		return s;
	}
	
	public int getSupplyRequired() {
		int s = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			s += m.type.getSupplyRequired(currentBonuses);
		}
		return s;
	}
	
	public int getExtraSupplyCapacity() {
		int s = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			s += m.type.getExtraSupplyCapacity(currentBonuses);
		}
		return s;
	}
	
	public int getSupplyCapacity() {
		int sup = StrictMath.max(EmpireStat.MIN_SHIP_SUPPLY_COST.get(currentBonuses), getSupplyRequired()) * EmpireStat.SHIP_SUPPLY_MULT.get(currentBonuses);
		if (type == ShipType.LANDSHIP) {
			sup *= EmpireStat.LANDSHIP_MOVE_SUPPLY_MULT.get(currentBonuses);
		}
		sup += getExtraSupplyCapacity() * EmpireStat.SHIP_SUPPLY_MULT.get(currentBonuses);
		return sup;
	}
	
	public int getLift() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (!m.type.canResupplyInCombat(currentBonuses)) { continue; }
			n += m.type.getLift(currentBonuses);
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.armour != null) {
				n += t.armour.type.getLift(currentBonuses);
			}
		}
		if (captain != null) {
			n = n * (100 + captain.type.liftPercent) / 100;
		}
		n = n * (100 + liftPercentFromMedals) / 100;
		if (superchargeSuspendiumTime > 0) {
			n *= SUPERCHARGE_SUSPENDIUM_MULT;
		}
		return n;
	}
	
	public int liftWithActivatedAbilitiesAndModules() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getLift(currentBonuses);
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.armour != null) {
				n += t.armour.type.getLift(currentBonuses);
			}
		}
		n = n * (100 + liftPercentFromMedals) / 100;
		if (captain != null) {
			n = n * (100 + captain.type.liftPercent) / 100;
			if (captain.type.combatAbilities.contains(HeroType.CombatAbility.SUPERCHARGE_SUSPENDIUM)) {
				n *= SUPERCHARGE_SUSPENDIUM_MULT;
			}
		}
		return n;
	}
	
	public int getAmmoCapacity() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getAmmo(currentBonuses);
		}
		return n;
	}
	
	public int getCoalCapacity() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getCoal(currentBonuses);
		}
		return n;
	}
	
	public boolean requiresCoal() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getCoalReload(currentBonuses) > 0 && m.type.canResupplyInCombat(currentBonuses)) { return true; }
		}
		return false;
	}

	public int getWaterCapacity() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getWater(currentBonuses);
		}
		return n;
	}

	public int getRepairCapacity() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getRepair(currentBonuses);
		}
		return n;
	}

	public int getTotalResource(Resource r) {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.getResource(r);
		}
		return n;
	}
	
	public int getTotalResourceCapacity(Resource r) {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			n += m.type.getResourceCapacity(r, currentBonuses);
		}
		return n;
	}

	public int getWidth() {
		return w;
	}
	
	public int getHeight() {
		return h;
	}
	
	public void groundShip() {
		moveTo = new Pt(getIntX(), AGame.GROUND_LEVEL * 2 + 100);
		grounding = true;
		ramming = false;
	}
	
	public boolean abandonShip(Combat c) {
		if (!c.canAbandonShip(this)) { return false; }
		sitting = true;
		legBalanceFactor = 0;
		LandFormation lf = (lastGrounded == null || msSinceOnGround > 99) ? c.landFormations.get(0) : lastGrounded;
		int btGridXStart = StrictMath.max(0, (int) StrictMath.floor((getX() - lf.getX()) / AGame.SGS) - 4);
		int btGridXRange = StrictMath.max(1, StrictMath.min(lf.getGridWidth() - btGridXStart, w + 8));
		int crewN = 0;
		for (Crewman cm : crew) {
			if (cm.job != null && cm.job.isCaptain()) {
				cm.doShout(cm.pickShout("abandonShip"));
			}
			cm.abandonJob("abandonShip");
			cm.ultimateBoardTarget = lf;
			cm.proximateBoardTarget = null;
			int btGridX = btGridXStart + crewN++ % btGridXRange;
			int btGridY = lf.firstSolidBlockYAt(btGridX);
			cm.walkToGR = new GridRef(btGridX, btGridY, lf);
		}
		return true;
	}

	public int getIntX() {
		return (int) getX();
	}

	public int getIntY() {
		return (int) getY();
	}

	public boolean grounded() {
		return msSinceOnGround < 100 && moveTo.y + getHeight() * AGame.SGS > AGame.GROUND_LEVEL * 2;
	}

	public boolean uselessPostRepair() {
		return uselessReasonPostRepair() != null;
	}
	
	public String uselessReasonPostRepair() {
		if (type == ShipType.BUILDING) {
			if (originalDesign != null) {
				return null;
			}
			if (getCost() < 200 && !canShoot()) {
				return _t("Disarmed");
			}
		}
		if (getRequiredCrew() > 0) {
			if (currentWorkingCrew() == 0) {
				return _t("No_crew");
			}
		}
		if (type.mobile) {
			if (availableSpeed(/* requireLegsAndWheelsTouchingGround */ false) == 0) {
				return _t("Immobile");
			}
			if (getCoalCapacity() == 0 && availablePropulsionNoFuel(/* requireLegsAndWheelsTouchingGround */ false) == 0) {
				return _t("No_Coal");
			}
		}
		if (!type.onGround) {
			if (availableServiceCeiling(null) - getHeight() * AGame.SGS <= 0) {
				return _t("Grounded");
			}
			if (getCoalCapacity() == 0 && availableServiceCeilingNoFuel() - getHeight() * AGame.SGS <= 0) {
				return _t("No_Coal");
			}
		}
		return null;
	}
	
	public String harmlessReason() {
		if (isArmedOrHasTroops()) {
			if (!canShoot() && !canMove()) {
				return _t("No_Ammo");
			} else {
				return null;
			}
		} else {
			if (!canMove()) {
				return _t("Disarmed");
			} else {
				return null;
			}
		}
	}

	public double getStructuralStressHPMultiplier() {
		int stress = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			stress += m.type.getStructuralStressAmount(currentBonuses);
		}
		return 1.0 - StrictMath.max(0, StrictMath.min(GameSetting.maxStructuralStressHPPenalty, (stress - GameSetting.minStructuralStress) * 1.0 / GameSetting.structuralStressPerHPPenalty));
	}
	
	public int getBonusHPPerTile() {
		int sz = 0;
		int bonus = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			sz += m.type.getW() * m.type.getH();
			bonus += m.hp > 0 ? m.type.getShipHPBonus(currentBonuses) : 0;
		}
		return sz == 0 ? 0 : bonus / sz;
	}

	private transient HashMap<String, ArrayList<ModuleType>> shipwideTypes = new HashMap<String, ArrayList<ModuleType>>();
	
	public double getShipwideModifier(String name) {
		double modifier = 0;
		ArrayList<ModuleType> bonusTypes = shipwideTypes.get(name);
		if (bonusTypes == null) {
			bonusTypes = new ArrayList<ModuleType>();
			shipwideTypes.put(name, bonusTypes);
			moduleLoop: for (int mi = 0; mi < modules.size(); mi++) {
				ModuleType mt = modules.get(mi).type;
				ShipwideModifier swm = mt.shipwideModifiers.get(name);
				if (swm.value.get(currentBonuses) != 0) {
					for (int abti = 0; abti < bonusTypes.size(); abti++) {
						ModuleType abt = bonusTypes.get(abti);
						if (abt == mt) {
							continue moduleLoop;
						}
						if (swm.doesNotStackWith.contains(abt)) {
							if (swm.value.get(currentBonuses) > abt.shipwideModifiers.get(name).value.get(currentBonuses)) {
								bonusTypes.set(abti, mt);
							}
							continue moduleLoop;
						}
					}
					bonusTypes.add(mt);
				}
			}
		}
		for (int abti = 0; abti < bonusTypes.size(); abti++) {
			modifier += bonusTypes.get(abti).shipwideModifiers.get(name).value.get(currentBonuses);
		}
		return modifier;
	}
	
	public double getInaccuracyMultiplier() {
		return StrictMath.max(0.1, 1 - getShipwideModifier("accuracyBonus")) * type.jitterMult;
	}
	
	public double totalFleetCommandBonus() {
		return getShipwideModifier("fleetCommandBonus");
	}

	public void precalcAIValues(Combat c) {
		aiTmpCanMove = canMove();
		aiTmpAvailableLift = availableLift(c);
		dangerCache = danger(c);
	}

	public int getCrewCount() {
		int n = 0;
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = crew.get(ci);
			if (cm.hp > 0) {
				n++;
			}
		}
		return n;
	}
	
	public int getActiveCrewCount() {
		int n = 0;
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = crew.get(ci);
			if (cm.active()) {
				n++;
			}
		}
		return n;
	}
	
	public void resetTentacles() {
		for (Module m : modules) {
			m.resetTentacles();
		}
	}

	public void initWheelsLegsAndTentacles(LandFormation ground, ArrayList<LandFormation> floaters, ShipList ships) {
		for (Module m : modules) {
			m.initWheels(ground, floaters, ships, /* force */ true);
			m.resetLegs(ground, floaters);
			m.resetTentacles();
		}
	}

	@Override
	public int getGridWidth() {
		return w;
	}

	@Override
	public int getGridHeight() {
		return h;
	}

	@Override
	public boolean solidAt(int x, int y) {
		Tile t = tileAt(gridXToWorldX(x, 1), y);
		return t != null && !t.isMaskedEmpty();
	}
	
	@Override
	public boolean fullAt(int x, int y) {
		Tile t = tileAt(gridXToWorldX(x, 1), y);
		return t != null && t.full();
	}
	
	@Override
	public boolean enterableAt(int x, int y) {
		Tile t = tileAt(gridXToWorldX(x, 1), y);
		return t != null && t.enterable();
	}
	
	@Override
	public double yBoundaryAt(double worldX) {
		int gridX = gridXToWorldX((int) StrictMath.floor((worldX - getX()) / AGame.SGS), 1);
		if (gridX < 0 || gridX >= w) { return getY(); } // qqDPS
		for (int gy = 0; gy < h; gy++) {
			Tile t = tileAt(gridX, gy);
			if (t != null && !t.isMaskedEmpty()) {
				return getY() + gy * AGame.SGS;
			}
		}
		return getY(); // qqDPS
	}
	
	@Override
	public int firstSolidBlockYAt(int gx) {
		if (gx < 0 || gx >= w) { return -1; }
		for (int gy = 0; gy < h; gy++) {
			if (tileAt(gx, gy) != null) {
				return gy;
			}
		}
		return -1; // qqDPS
	}
	
	@Override
	public ArrayList<GridLocation> reachable(int x, int y) {
		Tile t = tileAt(gridXToWorldX(x, 1), y);
		return t == null ? null : t.reachable;
	}
	
	@Override
	public void clearReachable(int x, int y) {
		Tile t = tileAt(gridXToWorldX(x, 1), y);
		if (t != null) {
			t.reachable.clear();
		}
	}
	
	@Override
	public boolean concaveAt(int x, int y) {
		Tile t = tileAt(gridXToWorldX(x, 1), y);
		if (t == null || t.isMaskedEmpty()) {
			return false;
		}
		return
				(!t.adjacentFullTile[0][0] && t.adjacentNonEmptyTile[1][0] && t.adjacentNonEmptyTile[0][1]) ||
				(!t.adjacentFullTile[0][1] && t.adjacentNonEmptyTile[0][0] && t.adjacentNonEmptyTile[0][2]) ||
				(!t.adjacentFullTile[0][2] && t.adjacentNonEmptyTile[0][1] && t.adjacentNonEmptyTile[1][2]) ||
				(!t.adjacentFullTile[1][2] && t.adjacentNonEmptyTile[0][2] && t.adjacentNonEmptyTile[2][2]) ||
				(!t.adjacentFullTile[2][2] && t.adjacentNonEmptyTile[1][2] && t.adjacentNonEmptyTile[2][1]) ||
				(!t.adjacentFullTile[2][1] && t.adjacentNonEmptyTile[2][2] && t.adjacentNonEmptyTile[2][0]) ||
				(!t.adjacentFullTile[2][0] && t.adjacentNonEmptyTile[2][1] && t.adjacentNonEmptyTile[1][0]) ||
				(!t.adjacentFullTile[1][0] && t.adjacentNonEmptyTile[2][0] && t.adjacentNonEmptyTile[0][0]);
	}

	@Override
	public GridLocation locationAt(int x, int y) {
		return tileAt(gridXToWorldX(x, 1), y);
	}
	
	@Override
	public boolean isAtSpeed() {
		return type.mobile;
	}

	public void setOwner(int id, FleetOwnerRef owner, CoatOfArms arms) {
		this.multiplayerControllerID = id;
		this.owner = owner;
		this.originalArms = arms;
		for (Crewman c : crew) {
			c.multiplayerControllerID = id;
			c.owner = owner;
		}
	}
	
	private static final class  YCmp implements Comparator<Module> {
		@Override
		public int compare(Module t, Module t1) {
			return t1.y * 10000 - t.y * 10000 + t1.x - t.x;
		}
	}

	private Tile unassignedPathingTile() {
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.canOccupy && t.pathCostTmp == 0) { return t; }
		}
		return null;
	}
	
	public boolean isPathingFullyConnected() {
		return pathChunks(null).size() < 2;
	}
	
	public ArrayList<ArrayList<Module>> pathChunks(Module ignore) {
		ArrayList<ArrayList<Module>> chunks = new ArrayList<ArrayList<Module>>();
		for (Tile t : tiles) {
			t.pathCostTmp = t.module == ignore ? -1 : 0;
		}
		int chunkID = 0;
		Tile unassignedTile;
		while ((unassignedTile = unassignedPathingTile()) != null) {
			LinkedList<Tile> tileQ = new LinkedList<Tile>();
			ArrayList<Module> chunk = new ArrayList<Module>();
			unassignedTile.pathCostTmp = ++chunkID;
			tileQ.add(unassignedTile);

			while (!tileQ.isEmpty()) {
				Tile t = tileQ.pollFirst();
				if (!chunk.contains(t.module)) {
					chunk.add(t.module);
				}
				for (int[] adj : ADJ) {
					if (adj[0] == -1 && adj[1] == 0 && t.x == t.module.x) {
						// Left
						if (!t.module.type.getLeftDoors()[t.y - t.module.y]) { continue; }
					} else if (adj[0] == 1 && adj[1] == 0 && t.x == t.module.x + t.module.type.getW() - 1) {
						// Right
						if (!t.module.type.getRightDoors()[t.y - t.module.y]) { continue; }
					} else if (adj[0] == 0 && adj[1] == -1 && t.y == t.module.y) {
						// Up
						if (!t.module.type.getUpDoors()[t.x - t.module.x]) { continue; }
					}
					Tile t2 = tileAt(t.x + adj[0], t.y + adj[1]);
					if (t2 != null && t2.canOccupy && t2.pathCostTmp == 0) {
						if (t2.module != t.module) {
							// Check there is a door leading into this tile.
							if (adj[0] == -1 && adj[1] == 0) {
								// From left into right
								if (!t2.module.type.getRightDoors()[t2.y - t2.module.y]) { continue; }
							} else if (adj[0] == 1 && adj[1] == 0) {
								// From right into left
								if (!t2.module.type.getLeftDoors()[t2.y - t2.module.y]) { continue; }
							} else if (adj[0] == 0 && adj[1] == 1) {
								// From top into bottom
								if (!t2.module.type.getUpDoors()[t2.x - t2.module.x]) { continue; }
							}
						}
						t2.pathCostTmp = chunkID;
						tileQ.add(t2);
					}
				}
			}
			chunks.add(new ArrayList<Module>(chunk));
		}
		return chunks;
	}
	
	public boolean isFullyConnectedInEditor() {
		if (isFullyConnectedInEditor == null) {
			isFullyConnectedInEditor = chunks(null).size() < 2;
		}
		return isFullyConnectedInEditor;
	}
	
	private Tile unassignedTile() {
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.pathCostTmp == 0) { return t; }
		}
		return null;
	}

	public ArrayList<ArrayList<Module>> chunks(Module ignore) {
		ArrayList<ArrayList<Module>> chunks = new ArrayList<ArrayList<Module>>();
		for (Tile t : tiles) {
			t.pathCostTmp = t.module == ignore ? -1 : 0;
		}
		int chunkID = 0;
		Tile unassignedTile;
		while ((unassignedTile = unassignedTile()) != null) {
			LinkedList<Tile> tileQ = new LinkedList<Tile>();
			ArrayList<Module> chunk = new ArrayList<Module>();
			HashSet<Module> chunkSet = new HashSet<Module>();
			unassignedTile.pathCostTmp = ++chunkID;
			tileQ.add(unassignedTile);

			while (!tileQ.isEmpty()) {
				Tile t = tileQ.pollFirst();
				if (!chunkSet.contains(t.module)) {
					chunk.add(t.module);
					chunkSet.add(t.module);
				}
				for (int[] adj : ADJ) {
					Tile t2 = tileAt(t.x + adj[0], t.y + adj[1]);
					if (t2 != null && t2.pathCostTmp == 0) {
						t2.pathCostTmp = chunkID;
						tileQ.add(t2);
					}
				}
			}
			chunks.add(new ArrayList<Module>(chunk));
		}
		return chunks;
	}

	private void sanityCheck() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.ship != this) {
				System.err.println("Module on wrong ship.");
			}
			for (int ty = m.y; ty < m.y + m.type.getH(); ty++) {
				for (int tx = m.x; tx < m.x + m.type.getW(); tx++) {
					if (tileAt(tx, ty) == null) {
						//throw new RuntimeException("Tiles!");
						System.err.println("Module " + m.type.getName() + " not fully covered with tiles.");
					}
				}
			}
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (!modules.contains(t.module)) {
				System.err.println("Tile's module " + t.module.type.getName() + " is not in ship.");
			}
			if (t.x < t.module.x || t.y < t.module.y || t.x >= t.module.x + t.module.type.getW() || t.y >= t.module.y + t.module.type.getH()) {
				System.err.println("Tile not on top of its module " + t.module.type.getName() + ".");
			}
		}
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = crew.get(ci);
			if (cm.ship != this) {
				System.err.println("Crewman on wrong ship.");
			}
			if (!tiles.contains(cm.currentTile)) {
				System.err.println("Crewman's tile (" + cm.currentTile.module.type.getName() + ") is not in ship.");
			}
			if (!modules.contains(cm.currentTile.module)) {
				System.err.println("Crewman's module " + cm.currentTile.module.type.getName() + " is not in ship.");
			}
		}
	}

	private void splitIfNeeded(Combat combat, boolean onViewingSide) {
		regenerateTileGrid();
		ArrayList<ArrayList<Module>> chunks = chunks(null);
		/*for (ArrayList<Module> ch : chunks) {
			for (Module m : ch) {
				if (!modules.contains(m)) {
					System.err.println(m.type.name + " not in ship");
				}
				boolean found = false;
				for (Tile t : tiles) {
					if (t.module == m) {
						found = true;
						break;
					}
				}
				if (!found) {
					System.err.println(m.type.name + " not covered!");
				}
			}
		}
		for (ArrayList<Module> c1 : chunks) {
			for (ArrayList<Module> c2 : chunks) {
				if (c1 == c2) { continue; }
				for (Module m : c2) {
					if (c1.contains(m)) {
						System.err.println("CHUNK OVERLAP OF " + m.type.getName());
					}
				}
			}
		}*/
		if (chunks.size() < 2) { return; }
		for (Iterator<ArrayList<Module>> it = chunks.iterator(); it.hasNext();) {
			ArrayList<Module> ms = it.next();
			if (ms.size() == 1) {
				destroyModule(ms.get(0), combat, true, onViewingSide);
				moduleLossAmount++;
				it.remove();
			}
		}
		if (chunks.size() < 2) { return; }
		ArrayList<Module> remainingShip = null;
		int mostW = 0;
		for (ArrayList<Module> chunk : chunks) {
			int weight = 0;
			for (Module m : chunk) {
				if (m.hp > -m.type.getHp(currentBonuses) / 2) { weight += m.type.getWeight(currentBonuses); }
			}
			if (remainingShip == null || mostW < weight) {
				remainingShip = chunk;
				mostW = weight;
			}
		}
		Side mySide = mySide(combat);
		for (ArrayList<Module> chunk : chunks) {
			if (chunk == remainingShip) { continue; }
			Airship cs = new Airship(type);
			cs.setBaseBonuses(baseBonuses);
			cs.setName("Fragment of " + getName());
			cs.networkID = networkID + "." + chunkSubIDCounter;
			chunkSubIDCounter++;
			while (combat.getShip(cs.networkID) != null) {
				cs.networkID = networkID + "." + chunkSubIDCounter;
				chunkSubIDCounter++;
			}
			cs.multiplayerControllerID = multiplayerControllerID;
			cs.owner = owner;
			cs.originalArms = originalArms;
			cs.setX(getIntX());//int) (getX() - (myCOM.x - com.x) * 10);
			cs.setY(getIntY());//(int) (getY() - (myCOM.y - com.y) * 10);
			cs.w = w;
			cs.h = h;
			cs.setxSpeed(getxSpeed());
			cs.setySpeed(getySpeed());
			cs.flipped = flipped;
			cs.moduleLossAmount = moduleLossAmount;
			cs.fallingTime = fallingTime;
			cs.explosionAmount = explosionAmount;
			cs.originalAllQuartered = originalAllQuartered;
			cs.originalAmmoCapacity = originalAmmoCapacity;
			cs.originalCoalCapacity = originalCoalCapacity;
			cs.originalRepairCapacity = originalRepairCapacity;
			cs.originalWaterCapacity = originalWaterCapacity;
			if (remainingShip != null) {
				cs.moduleLossAmount += remainingShip.size();
				moduleLossAmount += chunk.size();
			}
			for (Module m : chunk) {
				cs.modules.add(m);
				m.ship = cs;
				boolean addedTileForModule = false;
				for (Tile t : new ArrayList<Tile>(tiles)) {
					if (t.module == m) {
						cs.tiles.add(t);
						tiles.remove(t);
						t.setShip(cs);
						addedTileForModule = true;
					}
				}
				/*if (!addedTileForModule) {
					System.out.println("DIDN'T ADD TILE FOR MODULE");
				}*/
				for (Crewman cm : new ArrayList<Crewman>(crew)) {
					if (cm.currentTile.module == m) {
						cs.crew.add(cm);
						cm.ship = cs;
						crew.remove(cm);
					}
				}
				for (Crewman cm : new ArrayList<Crewman>(boarders)) {
					if (cm.currentTile.module == m) {
						cs.boarders.add(cm);
						cm.ship = cs;
						boarders.remove(cm);
					}
				}
				//destroyModule(m, combat, false);
				modules.remove(m);
				shipwideTypes.clear();
			}
			for (Particle p : stuckParticles) {
				Particle p2 = new Particle(p.type, p.x - getX() + cs.getX(), p.y - getY() + cs.getY(), p.dx, p.dy);
				p2.life = p.life;
				p2.lifespan = p.lifespan;
				cs.stuckParticles.add(p2);
			}
			mySide.ships.add(cs);
			cs.sanityCheck();
			cs.layout();
			cs.clearInvalidDecals();
			//cs.calcPaths();
			//cs.calcTilePaths();
			cs.recalcHPs();
			cs.assignJobs();
			cs.sanityCheck();
			cs.removeUnstuckParticles();
		}
		layout();
		clearInvalidDecals();
		paths.clear();
		tilePaths.clear();
		recalcHPs();
		//calcPaths();
		//calcTilePaths();
		assignJobs();
		//System.out.println("Split time cost: " + (System.nanoTime() - start) / 1000000l + " ms");
		crashSize = 2;
		sanityCheck();
		removeUnstuckParticles();
	}

	private Side mySide(Combat combat) {
		int ssz = combat.sides.size();
		for (int si = 0; si < ssz; si++) {
			Side s = combat.sides.get(si);
			if (s.ships.contains(this)) { return s; }
		}
		return null;
	}

	public void destroyModule(Module m, Combat combat, boolean violently, boolean onViewingSide) {
		if (violently && m.type.destructionSound(currentBonuses) == null) { crashSize = 3; }
		if (m.type.destructionSound(currentBonuses) != null && !m.type.playDestructionSoundAtStartOfDestruction()) {
			double mCx = getX() + gridXToWorldX(m.x, m.type.getW()) + m.type.getW() * AGame.SGS / 2;
			double mCy = getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2;
			combat.play(m.type.destructionSound(currentBonuses), mCx, mCy, getxSpeed(), getySpeed(), onViewingSide);
		}
		m.dealDestructionDamage();
		m.throwFragments(combat, /*explosively*/ false);
		modules.remove(m);
		shipwideTypes.clear();
		
		for (Iterator<Tile> tit = tiles.iterator(); tit.hasNext();) {
			if (tit.next().module == m) {
				tit.remove();
			}
		}
		
		for (Iterator<Crewman> cit = crew.iterator(); cit.hasNext();) {
			Crewman cm = cit.next();
			if ((!cm.beingCarried() && cm.currentTile.module == m) || (cm.carrier() != null && cm.carrier().currentTile.module == m)) {
				if (cm.alive()) {
					combat.incStat(this, "crewKilledByModuleDestruction");
				}
				cit.remove();
			}
		}
		for (Iterator<Crewman> bit = boarders.iterator(); bit.hasNext();) {
			Crewman b = bit.next();
			if ((!b.beingCarried() && b.currentTile.module == m) || (b.carrier() != null && b.carrier().currentTile.module == m)) {
				if (b.alive()) {
					combat.incStat(this, "crewKilledByModuleDestruction");
				}
				bit.remove();
			}
		}
		removeUnstuckParticles();
	}
	
	public void moveTimeForwardForFunctioningInEditorModules(int ms) {
		boolean hasCoal = getCoalCapacity() > 0;
		boolean hasAmmo = getAmmoCapacity() > 0;
		for (Module m : modules) {
			if (m.type.getCrew(currentBonuses) > 0) {
				boolean pseudoCrewed = false;
				for (Crewman cm : crew) {
					if (cm.currentTile.module == m) {
						pseudoCrewed = true;
						break;
					}
				}
				if (!pseudoCrewed) {
					continue;
				}
			}
			if (m.type.isWeapon() && !hasAmmo) {
				continue;
			}
			if (m.type.getCoalReload(currentBonuses) > 0 && !hasCoal) {
				continue;
			}
			m.time += ms;
		}
	}
	
	public boolean canFlipSafely(Combat combat) {
		boolean originalFlipped = flipped;
		flipped = !flipped;
		ArrayList<Body> bodies = combat.physics.bodies;
		int bsz = bodies.size();
		for (int bi = 0; bi < bsz; bi++) {
			Body b = bodies.get(bi);
			if (b == this) { continue; }
			if (b instanceof WheelBody && ((WheelBody) b).wheel.m.ship == this) { continue; }
			if (b instanceof Foot && ((Foot) b).leg.module.ship == this) { continue; }
			if (Rect2D.intersects(getX(), getY(), getBBWidth(), getBBHeight(), b.getX(), b.getY(), b.getBBWidth(), b.getBBHeight()) &&
				collidesWith(b, /* ignoreSoftThings */ true))
			{
				flipped = originalFlipped;
				return false;
			}
		}
		flipped = originalFlipped;
		return true;
	}
	
	private void creak(int ms, Combat combat, boolean onViewingSide) {
		boolean doesCreak = false;
		int msz = modules.size();
		for (int i = 0; i < msz; i++) {
			if (modules.get(i).type.doesCreak()) {
				doesCreak = true;
			}
			if (modules.get(i).type.preventsCreak()) {
				doesCreak = false;
				break;
			}
		}
		if (!doesCreak) {
			return;
		}
		double mult = type.onGround ? 0.3 : 1.5;
		mechStress += (prevDx - getxSpeed()) * (prevDx - getxSpeed()) * StrictMath.abs(prevDx - getxSpeed()) * 10000 * getWeight() * mult;
		prevDx = getxSpeed();
		double msDelta = 0;
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp <= 0 || m.fire > 0) {
				msDelta += ms * 0.003;
			}
		}
		mechStress += StrictMath.min(ms * 0.25, msDelta);
		
		if (AGame.ANIM_R.nextInt(2000) + 100 < mechStress) {
			combat.play(MiscCombatSound.CREAK, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, getxSpeed(), getySpeed(), onViewingSide);
			mechStress = 0;
		}
	}
		
	public boolean tick(int ms, Combat combat, boolean won, boolean lost, boolean onViewingSide, double fleetCommandBonus,
			double fleetFireRateMult, double fleetAccuracyMult, double fleetCrewSpeedMult, double fleetFlammabilityMult, double fleetExplosionRiskMult, double fleetCommandCooldownMult, double fleetRepairAmountMult, double fleetFirefightAmountMult) {
		recalculateBonuses();
		sailFlapManaged = false;
		
		if (fireAt != null && combat.sideOf(this) != null && !combat.otherSide(combat.sideOf(this)).ships.contains(fireAt)) {
			fireAt = null;
		}
		
		boolean inCombat = inCombat(combat);
		
		hasHadSpringFriction = false;
						
		CURRENT_COMBAT_DELETEME = combat;
		
		// Abilities
		if (smokescreenTime > 0) {
			smokescreenTime -= ms;
			ParticleType smoke = ParticleType.ofName("smokescreen");
			for (int ti = 0; ti < tiles.size(); ti++) {
				Tile t = tiles.get(ti);
				if (t.armour.window || t.module.type.getSupplyProvided(currentBonuses) > 0 || t.module.type.getCoalReload(currentBonuses) > 0) {
					if (AGame.ANIM_R.nextInt(2500) < ms) {
						combat.particles.add(new Particle(smoke, getX() + gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2, getY() + t.y * AGame.SGS + AGame.SGS / 2));
					}
				}
			}
		}
		if (burstOfSpeedTime > 0) { burstOfSpeedTime -= ms; }
		if (superchargeSuspendiumTime > 0) { superchargeSuspendiumTime -= ms; }
		if (doubleTimeTime > 0) { doubleTimeTime -= ms; }
		if (fearTime > 0) { fearTime -= ms; }
		if (tauntTime > 0) { tauntTime -= ms; }
		if (glimmerTime > 0) { glimmerTime -= ms; }
		if (blindnessTime > 0) { blindnessTime -= ms; }
		if (crosswindsTime > 0) { crosswindsTime -= ms; }
		if (paralysisTime > 0) { paralysisTime -= ms; }
		if (momentOfDoubtTime > 0) { momentOfDoubtTime -= ms; }
		if (cripplingShotTarget && !hasWorkingPropulsion()) { cripplingShotTarget = false; }
		if (disarmingShotTarget && !hasWorkingWeapons()) { disarmingShotTarget = false; }
		if (gustOfWindTime > 0) {
			gustOfWindTime -= ms;
			double forceMult = 0.0004;
			if (getX() < -combat.combatAreaW() / 2 + 700 || getX() + getBBWidth() > combat.combatAreaW() / 2 - 700) {
				forceMult /= 5;
			}
			setxForce(getxForce() + gustOfWindDX * getMass() * forceMult);
			setyForce(getyForce() + gustOfWindDY * getMass() * forceMult / 5);
		}
		if (suddenStormTime > 0) {
			suddenStormTime -= ms;
			double forceMult = 0.0004;
			if (getX() < -combat.combatAreaW() / 2 + 700 || getX() + getBBWidth() > combat.combatAreaW() / 2 - 700) {
				forceMult /= 5;
			}
			setxForce(getxForce() + suddenStormDX * getMass() * forceMult);
		}
		if (holdOnTime > 0) {
			holdOnTime -= ms;
			if (holdOnTime <= 0) {
				for (Module m : modules) {
					m.stopHoldingOn();
				}
				for (Crewman m : crew) {
					m.stopHoldingOn();
				}
			}
		}
		
		if (!generatesCommandPoints()) {
			notUnderCommandMs += ms;
		} else {
			notUnderCommandMs = 0;
		}
		
		if (inCombat) {
			outOfCombatMs = 0;
		} else {
			outOfCombatMs += ms;
		}
		
		double smoothSpeedMixFactor = 0.95;
		smoothedXSpeed = smoothedXSpeed * smoothSpeedMixFactor + getxSpeed() * (1 - smoothSpeedMixFactor);
		smoothedYSpeed = smoothedYSpeed * smoothSpeedMixFactor + getySpeed() * (1 - smoothSpeedMixFactor);
		
		if (!name.contains("Fragment")) {
			if (explosionAmount > 700) {
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("manyExplosionsMyShip", combat.sideOf(this), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("manyExplosionsEnemyShip", combat.otherSide(combat.sideOf(this)), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
				explosionAmount = 0;
			}

			if (moduleLossAmount > 8 && biggestInFleet && !inCombat) {
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("myFlagshipDestroyed", combat.sideOf(this), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyFlagshipDestroyed", combat.otherSide(combat.sideOf(this)), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
				moduleLossAmount = 0;
			}

			if (moduleLossAmount > 16 && explosionAmount > 300 && !biggestInFleet && !inCombat) {
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("myShipDestroyedSpectacularly", combat.sideOf(this), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyShipDestroyedSpectacularly", combat.otherSide(combat.sideOf(this)), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
				moduleLossAmount = 0;
			}
			
			if (!type.onGround && generatesCommandPoints() && getTotalResource(Resource.COAL) <= getCoalCapacity() / 12 && availableServiceCeilingNoFuel() <= 0 && availableServiceCeiling(null) > 0) {
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("imminentShipCrashNoFuel", combat.sideOf(this), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
			}
		}
		
		explosionAmount *= StrictMath.pow(0.9996, ms);
		moduleLossAmount *= StrictMath.pow(0.9996, ms);
		popOutCooldown -= ms;
		
		if (boarders.size() > 7) {
			int active = 0;
			int bsz = boarders.size();
			for (int bi = 0; bi < bsz; bi++) {
				if (boarders.get(bi).active()) {
					active++;
				}
			}
			if (active < bsz * 0.6) {
				combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("toughFighting", combat.otherSide(combat.sideOf(this)), getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
			}
		}
		
		creak(ms, combat, onViewingSide);
		
		if (StrictMath.abs(getxSpeed()) > 0.001) {
			msSinceLastXMove = 0;
			if (getxSpeed() > 0) {
				if (movingLeft) {
					timeMovingInSameXDirection = 0;
				} else {
					timeMovingInSameXDirection += ms;
				}
				movingLeft = false;
			} else {
				if (!movingLeft) {
					timeMovingInSameXDirection = 0;
				} else {
					timeMovingInSameXDirection += ms;
				}
				movingLeft = true;
			}
		} else {
			msSinceLastXMove += ms;
		}
		
		//#SpikeProfiler.start("updateBalancingOnLegs");
		updateBalancingOnLegs(ms);
		//#SpikeProfiler.end("updateBalancingOnLegs");
		
		if (board != null && combat != null) {
			//#SpikeProfiler.start("board");
			boolean friendly = combat.sideOf(this) == combat.sideOf(board);
			int csz = crew.size();
			for (int ci = 0; ci < csz; ci++) {
				Crewman cm = crew.get(ci);
				if (friendly || (cm.type.canBoard && !(cm.job instanceof Module.FixedGuardJob))) {
					cm.abandonJob("off to board");
					cm.ultimateBoardTarget = board;
					cm.proximateBoardTarget = null;
				}
			}
			Side mySide = combat.sideOf(this);
			csz = mySide.troops.size();
			for (int ci = 0; ci < csz; ci++) {
				Crewman cm = mySide.troops.get(ci);
				if (cm.attachedTo == this) {
					cm.ultimateBoardTarget = board;
					cm.proximateBoardTarget = null;
				}
			}
			board = null;
			//#SpikeProfiler.end("board");
		}
		
		msSinceOnGround += ms;

		// Deleting damaged modules
		//#SpikeProfiler.start("deletingDamagedModules");
		boolean removed = false;
		ArrayList<Module> removeables = new ArrayList<Module>(modules);
		//Collections.sort(removeables, new YCmp());
		int rmsz = removeables.size();
		boolean destroyEntire = false;
		for (int rmi = 0; rmi < rmsz; rmi++) {
			Module m = removeables.get(rmi);
			if (m.hp <= m.type.getDestroyedHP(currentBonuses) && isAtEdge(m)) {
				if (combat != null) {
					combat.msSinceInterestingCombatEvent = 0;
				}
				if (m.type.getDestructionLength(currentBonuses) != 0) {
					if (m.destructionTime == 0) {
						m.animOffset = -m.time;
						if (m.type.destructionSound(currentBonuses) != null && m.type.playDestructionSoundAtStartOfDestruction()) {
							double mCx = getX() + gridXToWorldX(m.x, m.type.getW()) + m.type.getW() * AGame.SGS / 2;
							double mCy = getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2;
							combat.play(m.type.destructionSound(currentBonuses), mCx, mCy, getxSpeed(), getySpeed(), onViewingSide);
						}
					}
					m.destructionTime += ms;
					if (m.destructionTime >= m.type.getDestructionLength(currentBonuses)) {
						removed = true;
						destroyModule(m, combat, true, onViewingSide);
						moduleLossAmount += m.type.getW() * m.type.getH();
						if (m.type.getDestroyEntireShipOnDestruction()) {
							destroyEntire = true;
						}
					}
				} else {
					m.hp -= ms;
					if (m.type.getInstantlyDestroyed() || m.hp <= m.type.getHp(currentBonuses) * Module.BREAK_APART_HP) {
						removed = true;
						destroyModule(m, combat, true, onViewingSide);
						moduleLossAmount += m.type.getW() * m.type.getH();
						if (m.type.getDestroyEntireShipOnDestruction()) {
							destroyEntire = true;
						}
					}
				}
			}
		}
		
		if (destroyEntire) {
			int msz = modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = modules.get(mi);
				m.hp = StrictMath.min(0, m.hp);
			}
		}
		
		if (removed) {
			//#SpikeProfiler.start("reLayout");
			recalcHPs();
			splitIfNeeded(combat, onViewingSide);
			layout();
			clearInvalidDecals();
			paths.clear();
			tilePaths.clear();
			//#SpikeProfiler.end("reLayout");
			return modules.isEmpty(); // Passed-in info is stale now, don't continue.
		}
		//#SpikeProfiler.end("deletingDamagedModules");
		if (modules.isEmpty()) { return true; }
		if (combat != null && combat.instantCommandRegeneration) {
			commandPoints = commandPointsRequired();
		} else {
			commandPoints = StrictMath.max(0, StrictMath.min(commandPointsRequired(), (int) (commandPoints + commandPointsGenerated() * ms * (1 + fleetCommandBonus) / (fleetCommandCooldownMult + 0.01))));
		}
		//#SpikeProfiler.start("modules");
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			//#SpikeProfiler.start(m.type.getName());
			m.tick(ms, combat, onViewingSide, fleetFireRateMult, fleetAccuracyMult, fleetFlammabilityMult, fleetExplosionRiskMult, fleetCommandCooldownMult);
			//#SpikeProfiler.end(m.type.getName());
		}
		//#SpikeProfiler.endStart("modules", "tiles");
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			tiles.get(ti).tick(combat, ms, this);
		}
		//#SpikeProfiler.end("tiles");
		assignJobsMs += ms;
		if (assignJobsMs >= ASSIGN_JOBS_EVERY) {
			//#SpikeProfiler.start("assignJobs");
			assignJobsMs -= ASSIGN_JOBS_EVERY;
			assignJobs();
			//#SpikeProfiler.end("assignJobs");
		}
		//#SpikeProfiler.start("crew");
		int csz = crew.size();
		Side shipSide = combat.sideOf(this);
		ArrayList<Crewman> cr = new ArrayList<Crewman>(crew);
		for (int ci = 0; ci < csz; ci++) {
			Crewman cm = cr.get(ci);
			cm.tick(ms, combat, shipSide, won, lost, onViewingSide, ci == canDoPathingIndex,
					fleetCrewSpeedMult, fleetRepairAmountMult, fleetFirefightAmountMult);
		}
		canDoPathingIndex = csz == 0 ? 0 : (canDoPathingIndex + 1) % csz;
		//#SpikeProfiler.endStart("crew", "boarders");
		int bsz = boarders.size();
		cr = new ArrayList<Crewman>(boarders);
		for (int bi = 0; bi < bsz; bi++) {
			Crewman cm = cr.get(bi);
			cm.tick(ms, combat, shipSide, won, lost, !onViewingSide, bi == boarderCanDoPathingIndex, 1, 1, 1);
		}
		boarderCanDoPathingIndex = bsz == 0 ? 0 : (boarderCanDoPathingIndex + 1) % bsz;
		//#SpikeProfiler.endStart("boarders", "collisionAvoidance");
		
		double maxSpeed = getMaxXSpeed();
		if (getxSpeed() < -maxSpeed) {
			setxSpeed(-maxSpeed);
		}
		if (getxSpeed() > maxSpeed) {
			setxSpeed(maxSpeed);
		}
		
		if (getDirectControlID() != -1) {
			// Horizontal speed
			setxForce(getxForce() + availablePropulsion(/* requireLegsAndWheelsTouchingGround */ true) * speedOrder.powerMult);
			enginesRunning = speedOrder.powerMult != 0;
			// Flipping
			if (flipped != flipTo) {
				if (availableTurningCost() > 0 && canFlipSafely(combat)) {
					enginesRunning = true;
					//xForce = 0;
					flipMs += ms;
					if (flipMs >= availableTurningCost()) {
						setFlipped(flipTo, combat);
						flipMs = 0;
					}
				} else {
					unableToFlipMs += ms;
					if (unableToFlipMs >= 3000) { // Eventually just give up.
						flipTo = flipped;
						unableToFlipMs = 0;
					}
				}
			}
			// Lift
			double adjS = StrictMath.abs(getxSpeed()) * (StrictMath.abs(altitudeOrder - getY()) + 10) / (StrictMath.abs(altitudeOrder - getX()) + 10) * 1.1;
			setyForce(getyForce() - StrictMath.max(0, StrictMath.min(
					availableSuspendiumForce(combat),
					suspendiumForceForY(altitudeOrder, false, 0, adjS, combat) + lastExertedYForce
				)));
			if (type.onGround) {
				moveTo = new Pt(getX() + getxSpeed() * 5000, getY());
			}
		} else {
			// Collision avoidance
			braking -= ms;
			hasBraked -= ms;
			int futureTest = type.onGround ? 80 : 500;
			double futureYXAngle = type.onGround ? -1 : 0;
			if (braking <= 0 && moveTo != null && (StrictMath.abs(moveTo.x - getX()) > 5 || StrictMath.abs(moveTo.y - getY()) > 5)) {
				boolean brake = false;
				ArrayList<Body> bs = combat.physics.bodies;
				int bodsz = bs.size();
				for (int bi = 0; bi < bodsz; bi++) {
					Body b = bs.get(bi);
					if (b == this) { continue; }
					if (b instanceof LandFormation && grounding) { continue; }
					if (b instanceof Foot && ((Foot) b).leg.module.ship == this) { continue; }
					if (b instanceof WheelBody && ((WheelBody) b).wheel.m.ship == this) { continue; }
					double box = b.getX(), boy = b.getY();
					b.setX(b.getX() + b.getxSpeed() * futureTest);
					b.setY(b.getY() + b.getySpeed() * futureTest);

					if (!Rect2D.intersects(getX(), getY(), getBBWidth(), getBBHeight(), b.getX(), b.getY(), b.getBBWidth(), b.getBBHeight()) ||
						!this.collidesWith(b, true))
					{
						double ox = getX(), oy = getY();
						setX(getX() + getxSpeed() * futureTest);
						setY(getY() + getySpeed() * futureTest + StrictMath.abs(getxSpeed() * futureTest) * futureYXAngle);
						if (type.onGround && b instanceof LandFormation) {
							setY(((LandFormation) b).getVerticalPosition(this, (int) getX(), flipped, /* ignoreSoftThings*/ true) - groundOffset());
						}
						if (Rect2D.intersects(getX(), getY(), getBBWidth(), getBBHeight(), b.getX(), b.getY(), b.getBBWidth(), b.getBBHeight()) &&
							this.collidesWith(b, true))
						{
							// Attempt to estimate the collision damage.
							double speedDeltaSquared = ((getxSpeed() - b.getxSpeed()) * (getxSpeed() - b.getxSpeed()) + (getySpeed() - b.getySpeed()) * (getySpeed() - b.getySpeed()));
							double collisionForce = speedDeltaSquared * (getCollisionMass() + b.getCollisionMass());
							if (collisionForce > 100) {
								brake = true;
							}
							if (collisionForce > 1000) {
								if (fallingTime > 1500 && b instanceof LandFormation && ((LandFormation) b).immobile) {
									combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("aboutToCrash", shipSide, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
								} else if (ramming) {
									combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("aboutToRam", shipSide, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
								} else {
									combat.exceptionalCombatEvents.add(new ExceptionalCombatEvent("aboutToCollide", shipSide, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, null, this));
								}
							}
						}
						setX(ox);
						setY(oy);
					}
					b.setX(box);
					b.setY(boy);
					if (brake) { break; }
				}
				if (type == ShipType.LANDSHIP && !ramming) {
					boolean hasLegs = false;
					msz = modules.size();
					for (int mi = 0; mi < msz; mi++) {
						if (modules.get(mi).legs.size() > 2) {
							hasLegs = true;
							break;
						}
					}
					int ssz = shipSide.ships.size();
					for (int si = 0; si < ssz; si++) {
						Airship other = shipSide.ships.get(si);
						if (other == this || !other.type.onGround) {
							continue;
						}
						boolean otherHasLegs = false;
						msz = other.modules.size();
						for (int mi = 0; mi < msz; mi++) {
							if (other.modules.get(mi).legs.size() > 2) {
								otherHasLegs = true;
								break;
							}
						}
						if (!hasLegs && !otherHasLegs) {
							continue;
						}
						//if (collidesWith(other)) { continue; }
						double myX = getX() + getxSpeed() * futureTest;
						double myY = combat.landFormations.get(0).getVerticalPosition(this, (int) getX(), flipped, /* ignoreSoftThings*/ true) - groundOffset();
						double myW = getBBWidth();
						double myH = getBBHeight();
						double ow = other.getBBWidth();
						double oh = other.getBBHeight();
						if (hasLegs) {
							if (!Rect2D.intersects(getX() - myW / 2, getY(), myW * 2, myH + myW / 4, other.getX(), other.getX(), ow, oh) && Rect2D.intersects(myX - myW / 2, myY, myW * 2, myH + myW / 4, other.getX(), other.getY(), ow, oh)) {
								brake = true;
								//System.out.println("I have legs and I am braking.");
								break;
							}
						}
						if (otherHasLegs) {
							if (!Rect2D.intersects(getX(), getY(), myW, myH, other.getX() - ow / 2, other.getY(), ow * 2, oh + ow / 4) && Rect2D.intersects(myX, myY, myW, myH, other.getX() - ow / 2, other.getY(), ow * 2, oh + ow / 4)) {
								brake = true;
								//System.out.println("My buddy has legs and I am braking");
								break;
							}
						}
					}
				}
				if (brake && !ramming) {
					moveTo = new Pt(getX(), getY());
					flipTo = flipped;
					commandPoints = StrictMath.min(commandPointsRequired(), commandPoints + commandPointsRequired() / 2);
					braking = 500;
					hasBraked = 8000;
				}
			}

			//#SpikeProfiler.endStart("collisionAvoidance", "movement");

			if (moveTo != null) {
				boolean moveDirection = moveTo.x < getX();
				boolean needToFlip = flipTo != flipped;
				boolean flipAtEnd = moveMode.override ? moveMode.flipAtEnd : moveDirection == flipped;
				boolean nearEnd = StrictMath.abs(moveTo.x - getX()) < 80;
				boolean doFlip = needToFlip && (!flipAtEnd || nearEnd);
				if (doFlip) {
					if (availableTurningCost() > 0 && canFlipSafely(combat)) {
						enginesRunning = true;
						//xForce = 0;
						flipMs += ms;
						if (flipMs >= availableTurningCost()) {
							setFlipped(flipTo, combat);
							flipMs = 0;
						}
					} else {
						unableToFlipMs += ms;
						if (unableToFlipMs >= 3000) { // Eventually just give up.
							flipTo = flipped;
							unableToFlipMs = 0;
						}
					}
					/*System.out.println(name);
					System.out.println("1leyf " + lastExertedYForce);
					System.out.println("1SF " + availableSuspendiumForce());
					System.out.println("1SFFY " + suspendiumForceForY(moveTo.y, false, 0, 0));*/
					setyForce(getyForce() - StrictMath.max(0, StrictMath.min(
							availableSuspendiumForce(combat),
							suspendiumForceForY(moveTo.y, false, 0, 0, combat) + lastExertedYForce
					)));
				} else {
					double ef = (grounded() && !type.onGround) ? 0 : engineForceForX(moveTo.x, combat);
					setxForce(getxForce() + ef);
					enginesRunning = ef != 0;
					double adjF = ef * (StrictMath.abs(moveTo.y - getY()) + 10) / (StrictMath.abs(moveTo.x - getX()) + 10) * 1.8;
					double adjS = StrictMath.abs(getxSpeed()) * (StrictMath.abs(moveTo.y - getY()) + 10) / (StrictMath.abs(moveTo.x - getX()) + 10) * 1.1;
					/*System.out.println(name);
					System.out.println("2leyf " + lastExertedYForce);
					System.out.println("2SF " + availableSuspendiumForce());
					System.out.println("1SFFY " + suspendiumForceForY(moveTo.y, ef != 0, StrictMath.abs(adjF), adjS));*/
					setyForce(getyForce() - StrictMath.max(0, StrictMath.min(
							availableSuspendiumForce(combat),
							suspendiumForceForY(moveTo.y, ef != 0, StrictMath.abs(adjF), adjS, combat) + lastExertedYForce
					)));
				}
			} else {
				//xForce = 0;
				enginesRunning = false;
				setyForce(getyForce() - availableSuspendiumForce(combat));
			}
		}
		
		suspendiumRunning = !grounded();
		if (suspendiumRunning) {
			msSuspendiumOff = 0;
		} else {
			msSuspendiumOff += ms;
		}
		
		//#SpikeProfiler.end("movement");
		crashCooldown -= ms;
		if (crashCooldown < 0) { crashCooldown = 0; }
		if (crashSize == 3) {
			if (crashCooldown == 0) {
				combat.play(MiscCombatSound.LARGE_CRASH, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, getxSpeed(), getySpeed(), onViewingSide);
				crashCooldown = 3000 + AGame.ANIM_R.nextInt(2500);
			} else {
				crashSize--;
			}
		}
		if (crashSize == 2) {
			if (crashCooldown < 800) {
				combat.play(MiscCombatSound.MEDIUM_CRASH, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, getxSpeed(), getySpeed(), onViewingSide);
				crashCooldown = 2000 + AGame.ANIM_R.nextInt(2000);
			} else {
				crashSize--;
			}
		}
		if (crashSize == 1 && crashCooldown < 1200) {
			combat.play(MiscCombatSound.SMALL_CRASH, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, getxSpeed(), getySpeed(), onViewingSide);
			crashCooldown = 1500 + AGame.ANIM_R.nextInt(1000);
		}
		crashSize = 0;
		
		if (getxSpeed() > 0.3) {
			fallingTime += ms;
		} else {
			fallingTime = 0;
		}
		
		if (combat.landFormations.get(0).landscapeType.hasWater) {
			// If it's a monster or some small fragment with interior spaces that has gone underwater, destroy it.
			if (getY() > AGame.GROUND_LEVEL && !canBeBoarded()) {
				for (int i = 0; i < modules.size(); i++) {
					modules.get(i).hp = -modules.get(i).type.getHp(currentBonuses);
				}
			}
			if (combat.landFormations.get(0).landscapeType.deepWater && getY() > Combat.CRUSH_DEPTH) {
				double volume = Math.min(8, 0.5 + 0.15 * Math.sqrt(getWeight()));
				combat.play(MiscCombatSound.DEPTH_CRUSH, getX() + getBBWidth() / 2, getY() + getBBHeight() / 2, 0, 0, volume, true);
				for (Module m : modules) {
					m.throwCrushFragments(combat);
				}
				if (combat.sideOf(this) != null && !name.contains("Fragment") && modules.size() > 5 && canBeBoarded()) {
					combat.sideOf(this).shipGotCrushed = true;
				}
				crushed = true;
				return true;
			}
			// Water particles
			ParticleType water = ParticleType.ofName("waterSplash");
			boolean nowInWater = getY() + getBBHeight() > AGame.GROUND_LEVEL + 1;
			boolean newlyInWater = nowInWater && !inWater;
			inWater = nowInWater;
			if (newlyInWater) {
				double volume = Math.min(8, 0.5 + 0.2 * (Math.abs(getxSpeed()) + Math.abs(getySpeed())) * Math.sqrt(getWeight()));
				combat.play(MiscCombatSound.SPLASH, getX() + getBBWidth() / 2, getY() + getBBHeight(), 0, 0, volume, true);
			}
			if (inWater && getySpeed() > 0.03) {
				double yAmt = getySpeed() + (newlyInWater ? 100 : 0);
				for (int gx = 0; gx < getGridWidth(); gx++) {
					for (int gy = getGridHeight() - 1; gy >= 0; gy--) {
						if (getY() + gy * AGame.SGS + AGame.SGS < AGame.GROUND_LEVEL) {
							break;
						}
						if (solidAt(gx, gy)) {
							if (AGame.ANIM_R.nextDouble() < 0.05 * yAmt * ms) {
								combat.particles.add(new Particle(water, getX() + gx * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS, getY() + gy * AGame.SGS + AGame.SGS));
							}
							break;
						}
					}
				}
			}
			if (inWater && getxSpeed() > 0) {
				double xAmt = getxSpeed();
				for (int gy = getGridHeight() - 1; gy >= 0; gy--) {
					if (getY() + gy * AGame.SGS + AGame.SGS < AGame.GROUND_LEVEL) {
						break;
					}
					for (int gx = getGridWidth()- 1; gx >= 0; gx--) {
						if (solidAt(gx, gy)) {
							if (AGame.ANIM_R.nextDouble() < 0.15 * xAmt * ms) {
								combat.particles.add(new Particle(water, getX() + gx * AGame.SGS + AGame.SGS, getY() + gy * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS));
							}
							break;
						}
					}
				}
			}
			if (inWater && getxSpeed() < 0) {
				double xAmt = -getxSpeed();
				for (int gy = getGridHeight() - 1; gy >= 0; gy--) {
					if (getY() + gy * AGame.SGS + AGame.SGS < AGame.GROUND_LEVEL) {
						break;
					}
					for (int gx = 0; gx < getGridWidth(); gx++) {
						if (solidAt(gx, gy)) {
							if (AGame.ANIM_R.nextDouble() < 0.15 * xAmt * ms) {
								combat.particles.add(new Particle(water, getX() + gx * AGame.SGS, getY() + gy * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS));
							}
							break;
						}
					}
				}
			}
		}
		
		updateWeight();
		
		return false;
	}

	@Override
	public boolean removeMe(Combat c) {
		return modules.isEmpty();
	}

	@Override
	public double elasticity() { return 0.5; }
	
	@Override
	public double horizontalAirFriction(boolean positiveX) {
		return 0.001 + ((positiveX ^ flipped) ? frontDrag : backDrag) * 0.002 / 5;
	}
	
	public double frontAirFriction() {
		return 0.001 + frontDrag * 0.002 / 5;
	}
	
	@Override
	public double verticalAirFriction(boolean positiveY) {
		return 0.001 + (positiveY ? bottomDrag : topDrag) * 0.002 / 5;
	}
	
	@Override
	public int getCollisionMass() {
		return StrictMath.max(getWeight(), 10);
	}

	@Override
	public int getMass() {
		return StrictMath.max(getWeight(), 10);
	}

	@Override
	public boolean isImmobile() {
		return false;
	}

	@Override
	public double getBBWidth() {
		return getWidth() * AGame.SGS;
	}

	@Override
	public double getBBHeight() {
		return getHeight() * AGame.SGS;
	}

	@Override
	public boolean collidesWith(PhysicsRect b2) {
		if (b2 instanceof Foot && (((Foot) b2).leg.module.ship == this || !((Foot) b2).isDown)) {
			return false;
		}
		if (b2 instanceof WheelBody) {
			if (((WheelBody) b2).wheel.m.ship == this) { return false; }
			return overlapsWith((WheelBody) b2);
		}
		if (b2 instanceof Airship) {
			return overlapsWith((Airship) b2);
		}
		if (b2 instanceof LandFormation) {
			return overlapsWith((LandFormation) b2, /* ignoreSoftThings*/ false);
		}
		if (b2 instanceof Crewman) {
			return overlapsWith((Crewman) b2);
		}
		return overlapsWith(b2);
	}
	
	public boolean collidesWith(PhysicsRect b2, boolean ignoreSoftThings) {
		if (b2 instanceof Airship) {
			return overlapsWith((Airship) b2);
		}
		if (b2 instanceof LandFormation) {
			return overlapsWith((LandFormation) b2, ignoreSoftThings);
		}
		return overlapsWith(b2);
	}

	@Override
	public void doCollision(Body b2, double hitEnergy, Combat combat, boolean atSpeed) {
		ArrayList<OverlappingTile> overlaps;
		if (b2 instanceof WheelBody) {
			overlaps = overlaps((WheelBody) b2);
		} else if (b2 instanceof Airship) {
			overlaps = overlaps((Airship) b2);
		} else if (b2 instanceof LandFormation) {
			overlaps = overlaps((LandFormation) b2);
			hitEnergy *= type.landCollisionDamageMult;
		} else {
			overlaps = overlaps(b2);
		}
		if (overlaps.isEmpty()) { return; }
		if (b2 instanceof Foot && combat.sideOf(this) == combat.sideOf(((Foot) b2).leg.module.ship)) {
			hitEnergy /= 4;
		}
		collideWith(overlaps, hitEnergy, combat, b2 instanceof LandFormation ? (LandFormation) b2 : null, b2 instanceof Airship ? (Airship) b2 : null, (b2 instanceof LandFormation && !b2.isImmobile()));
	}
	
	public void doEarthquake(double averageTileDamage, Combat combat) {
		ArrayList<OverlappingTile> overlaps = new ArrayList<OverlappingTile>();
		for (Tile t : tiles) {
			int distanceFromBottom = h - t.y;
			if (distanceFromBottom > 12) { continue; }
			overlaps.add(new OverlappingTile(t, 2 / (distanceFromBottom + 1)));
		}
		collideWith(overlaps, averageTileDamage * overlaps.size(), combat, null, null, false);
	}
	
	public boolean overlapsWith(WheelBody b2) {
		int tileW = getWidth();
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			double tx = getX() + (flipped ? (tileW - t.x - 1) : t.x) * AGame.SGS;
			double ty = getY() + t.y * AGame.SGS;
			if (b2.intersectsWithRect(tx, ty, AGame.SGS, AGame.SGS)) {
				if (t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x] == TileMask.FULL) { // qqDPS Garbage copout re circle isect
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean overlapsWith(LandFormation b2, boolean ignoreSoftThings) {
		int tileW = getWidth();
		double lfx = b2.getX(), lfy = b2.getY();
		int lfgw = b2.grid[0].length, lfgh = b2.grid.length;
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			TileMask tm = t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x];
			if (tm == TileMask.EMPTY) {
				continue;
			}
			double tx = getX() + (flipped ? (tileW - t.x - 1) : t.x) * AGame.SGS;
			double ty = getY() + t.y * AGame.SGS;
			// The tile can intersect with at most 4 tiles in the land formation grid, one for each corner.
			// We can calculate the positions of the point corners, then the indexes of the LF grid tiles.
			// Then, checking they're in-gamut, ask them directly if they're solid.
			// In fact, we can cheat further: we can just find the top-left tile and then assume that the 3
			// adjacent tiles also collide. Which is true except in the boring case of perfect tile align.
			int lfgx = (int) StrictMath.floor((tx - lfx) / AGame.SGS);
			int lfgy = (int) StrictMath.floor((ty - lfy) / AGame.SGS);

			int blockX = (int) lfx + lfgx * AGame.SGS;
			int blockY = (int) lfy + lfgy * AGame.SGS;
			if (lfgy >= 0 && lfgy < lfgh && lfgx >= 0 && lfgx < lfgw &&
					(ignoreSoftThings ? b2.grid[lfgy][lfgx].damageMultiplier > 0.4 : b2.grid[lfgy][lfgx].solid))
			{
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX, blockY)) {
					return true;
				}
			}
			if ((lfgy + 1) >= 0 && (lfgy + 1) < lfgh && (lfgx + 1) >= 0 && (lfgx + 1) < lfgw &&
					(ignoreSoftThings ? b2.grid[lfgy + 1][lfgx + 1].damageMultiplier > 0.4 : b2.grid[lfgy + 1][lfgx + 1].solid))
			{
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX + AGame.SGS, blockY + AGame.SGS)) {
					return true;
				}
			}
			if ((lfgy + 1) >= 0 && (lfgy + 1) < lfgh && lfgx >= 0 && lfgx < lfgw &&
					(ignoreSoftThings ? b2.grid[lfgy + 1][lfgx].damageMultiplier > 0.4 : b2.grid[lfgy + 1][lfgx].solid))
			{
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX, blockY + AGame.SGS)) {
					return true;
				}
			}
			if (lfgy >= 0 && lfgy < lfgh && (lfgx + 1) >= 0 && (lfgx + 1) < lfgw &&
					(ignoreSoftThings ? b2.grid[lfgy][lfgx + 1].damageMultiplier > 0.4 : b2.grid[lfgy][lfgx + 1].solid))
			{
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX + AGame.SGS, blockY)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private ArrayList<OverlappingTile> overlaps(WheelBody b2) {
		ArrayList<OverlappingTile> overlaps = new ArrayList<OverlappingTile>();
		int tileW = getWidth();
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			double tx = getX() + (flipped ? (tileW - t.x - 1) : t.x) * AGame.SGS;
			double ty = getY() + t.y * AGame.SGS;
			if (b2.intersectsWithRect(tx, ty, AGame.SGS, AGame.SGS) && t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x] == TileMask.FULL) {
				overlaps.add(new OverlappingTile(t, 0.3));
			}
		}
		return overlaps;
	}
	
	public ArrayList<OverlappingTile> overlaps(LandFormation b2) {
		ArrayList<OverlappingTile> overlaps = new ArrayList<OverlappingTile>();
		int tileW = getWidth();
		double lfx = b2.getX(), lfy = b2.getY();
		int lfgw = b2.grid[0].length, lfgh = b2.grid.length;
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			TileMask tm = t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x];
			if (tm == TileMask.EMPTY) {
				continue;
			}
			double tx = getX() + (flipped ? (tileW - t.x - 1) : t.x) * AGame.SGS;
			double ty = getY() + t.y * AGame.SGS;
			// The tile can intersect with at most 4 tiles in the land formation grid, one for each corner.
			// We can calculate the positions of the point corners, then the indexes of the LF grid tiles.
			// Then, checking they're in-gamut, ask them directly if they're solid.
			// In fact, we can cheat further: we can just find the top-left tile and then assume that the 3
			// adjacent tiles also collide. Which is true except in the boring case of perfect tile align.
			int lfgx = (int) StrictMath.floor((tx - lfx) / AGame.SGS);
			int lfgy = (int) StrictMath.floor((ty - lfy) / AGame.SGS);
			
			int blockX = (int) lfx + lfgx * AGame.SGS;
			int blockY = (int) lfy + lfgy * AGame.SGS;

			double damageMultiplier = 0.0;
			
			
			if (lfgy >= 0 && lfgy < lfgh && lfgx >= 0 && lfgx < lfgw && b2.grid[lfgy][lfgx].solid) {
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX, blockY)) {
					damageMultiplier = StrictMath.max(damageMultiplier, b2.grid[lfgy][lfgx].damageMultiplier);
				}
			}
			
			if ((lfgy + 1) >= 0 && (lfgy + 1) < lfgh && (lfgx + 1) >= 0 && (lfgx + 1) < lfgw && b2.grid[lfgy + 1][lfgx + 1].solid) {
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX + AGame.SGS, blockY + AGame.SGS)) {
					damageMultiplier = StrictMath.max(damageMultiplier, b2.grid[lfgy + 1][lfgx + 1].damageMultiplier);
				}
			}
			
			if ((lfgy + 1) >= 0 && (lfgy + 1) < lfgh && lfgx >= 0 && lfgx < lfgw && b2.grid[lfgy + 1][lfgx].solid) {
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX, blockY + AGame.SGS)) {
					damageMultiplier = StrictMath.max(damageMultiplier, b2.grid[lfgy + 1][lfgx].damageMultiplier);
				}
			}
			
			if (lfgy >= 0 && lfgy < lfgh && (lfgx + 1) >= 0 && (lfgx + 1) < lfgw && b2.grid[lfgy][lfgx + 1].solid) {
				if (tm.intersectsBlock((int) tx, (int) ty, flipped, blockX + AGame.SGS, blockY)) {
					damageMultiplier = StrictMath.max(damageMultiplier, b2.grid[lfgy][lfgx + 1].damageMultiplier);
				}
			}
			
			if (damageMultiplier > 0) {
				overlaps.add(new OverlappingTile(t, damageMultiplier));
			}
			
			/*if ((lfgy >= 0 && lfgy < lfgh && lfgx >= 0 && lfgx < lfgw && b2.grid[lfgy][lfgx].solid) ||
				((lfgy + 1) >= 0 && (lfgy + 1) < lfgh && (lfgx + 1) >= 0 && (lfgx + 1) < lfgw && b2.grid[lfgy + 1][lfgx + 1].solid) ||
				((lfgy + 1) >= 0 && (lfgy + 1) < lfgh && lfgx >= 0 && lfgx < lfgw && b2.grid[lfgy + 1][lfgx].solid) ||
				(lfgy >= 0 && lfgy < lfgh && (lfgx + 1) >= 0 && (lfgx + 1) < lfgw && b2.grid[lfgy][lfgx + 1].solid))
			{
				overlaps.add(t);
			}*/
		}
		return overlaps;
	}

	public boolean overlapsWith(Airship b) {
		int tileW = getWidth();
		int bTileW = b.getWidth();
		double bx = b.getX(), by = b.getY();
		int tsz = tiles.size();
		
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			TileMask tm = t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x];
			if (tm == TileMask.EMPTY) {
				continue;
			}
			
			double tx = flipped ? getX() + (tileW - t.x - 1) * AGame.SGS : (getX() + t.x * AGame.SGS);
			double ty = getY() + t.y * AGame.SGS;

			//int btx = b.flipped ? (bTileW - (int) StrictMath.floor((tx - bx) / AGame.SGS) - 1) : ((int) StrictMath.floor((tx - bx) / AGame.SGS));
			int btx;
			if (b.flipped) {
				btx = bTileW - 2 - (int) StrictMath.floor((tx - bx) / AGame.SGS);
			} else {
				btx = (int) StrictMath.floor((tx - bx) / AGame.SGS);
			}
			int bty = (int) StrictMath.floor((ty - by) / AGame.SGS);
			
			// tileAt takes care of bounds checking for us. Thanks, tileAt!
			
			Tile t2 = b.tileAt(btx, bty);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					return true;
				}
			}
			t2 = b.tileAt(btx + 1, bty + 1);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					return true;
				}
			}
			t2 = b.tileAt(btx + 1, bty);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					return true;
				}
			}
			t2 = b.tileAt(btx, bty + 1);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public class OverlappingTile {
		public final Tile tile;
		public double dmgMultiplier;

		public OverlappingTile(Tile tile, double dmgMultiplier) {
			this.tile = tile;
			this.dmgMultiplier = dmgMultiplier;
		}
	}

	public ArrayList<OverlappingTile> overlaps(Airship b) {
		ArrayList<OverlappingTile> overlaps = new ArrayList<OverlappingTile>();
		
		int tileW = getWidth();
		int bTileW = b.getWidth();
		double bx = b.getX(), by = b.getY();
		int tsz = tiles.size();
		
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			TileMask tm = t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x];
			if (tm == TileMask.EMPTY) {
				continue;
			}
			
			double tx = flipped ? getX() + (tileW - t.x - 1) * AGame.SGS : (getX() + t.x * AGame.SGS);
			double ty = getY() + t.y * AGame.SGS;

			int btx;
			if (b.flipped) {
				btx = bTileW - 2 - (int) StrictMath.floor((tx - bx) / AGame.SGS);
			} else {
				btx = (int) StrictMath.floor((tx - bx) / AGame.SGS);
			}
			int bty = (int) StrictMath.floor((ty - by) / AGame.SGS);
			
			// tileAt takes care of bounds checking for us. Thanks, tileAt!
			int nTiles = 0;
			double dmgMult = 0;
			Tile t2 = b.tileAt(btx, bty);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(b.currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					nTiles++;
					dmgMult += t2.module.type.getHardness(b.currentBonuses);
				}
			}
			t2 = b.tileAt(btx + 1, bty);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(b.currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					nTiles++;
					dmgMult += t2.module.type.getHardness(b.currentBonuses);
				}
			}
			t2 = b.tileAt(btx, bty + 1);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(b.currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					nTiles++;
					dmgMult += t2.module.type.getHardness(b.currentBonuses);
				}
			}
			t2 = b.tileAt(btx + 1, bty + 1);
			if (t2 != null) {
				TileMask tm2 = t2.module.type.getTileMasks(b.currentBonuses)[t2.y - t2.module.y][t2.x - t2.module.x];
				int t2X = (int) bx + b.gridXToWorldX(t2.x, 1) * AGame.SGS;
				int t2Y = (int) by + t2.y * AGame.SGS;
				if (tm.intersects((int) tx, (int) ty, flipped, tm2, t2X, t2Y, b.flipped)) {
					nTiles++;
					dmgMult += t2.module.type.getHardness(b.currentBonuses);
				}
			}
			if (nTiles > 0) {
				overlaps.add(new OverlappingTile(t, dmgMult / nTiles * t.module.type.getCollisionDamageReceivedMult(currentBonuses)));
			}
		}
		return overlaps;
	}

	public boolean overlapsWith(PhysicsRect b2) {
		// Map the rect's borders onto ship tiles and just look at those.
		int top = (int) StrictMath.floor((b2.getY() - getY()) / AGame.SGS);
		int bottom = (int) StrictMath.floor((b2.getY() + b2.getBBHeight() - getY()) / AGame.SGS);
		int left = (int) StrictMath.floor((b2.getX() - getX()) / AGame.SGS);
		int right = (int) StrictMath.floor((b2.getX() + b2.getBBWidth() - getX()) / AGame.SGS);
		
		if (top < 0 && left < 0 && bottom >= h && right >= w) {
			return true;
		}
		
		int ot = top, ob = bottom, ol = left, or = right;
		top = StrictMath.max(StrictMath.min(ob, 0), top); 
		bottom = StrictMath.min(StrictMath.max(ot, h - 1), bottom);
		left = StrictMath.max(StrictMath.min(or, 0), left);
		right = StrictMath.min(StrictMath.max(ol, w - 1), right);
		
		for (int xx = left; xx <= right; xx++) {
			int gx = gridXToWorldX(xx, 1);
			Tile topTile = tileAt(gx, top);
			if (topTile != null) {
				TileMask topTileMask = topTile.module.type.getTileMasks(currentBonuses)[topTile.y - topTile.module.y][topTile.x - topTile.module.x];
				if (topTileMask.intersectsRect((int) getX() + xx * AGame.SGS, (int) getY() + top * AGame.SGS, flipped, (int) b2.getX(), (int) b2.getY(), (int) b2.getBBWidth(), (int) b2.getBBHeight())) {
					return true;
				}
			}
			Tile bottomTile = tileAt(gx, bottom);
			if (bottomTile != null) {
				TileMask bottomTileMask = bottomTile.module.type.getTileMasks(currentBonuses)[bottomTile.y - bottomTile.module.y][bottomTile.x - bottomTile.module.x];
				if (bottomTileMask.intersectsRect((int) getX() + xx * AGame.SGS, (int) getY() + bottom * AGame.SGS, flipped, (int) b2.getX(), (int) b2.getY(), (int) b2.getBBWidth(), (int) b2.getBBHeight())) {
					return true;
				}
			}
		}
		
		int leftG = gridXToWorldX(left, 1);
		int rightG = gridXToWorldX(right, 1);
		for (int yy = top; yy <= bottom; yy++) {
			Tile leftTile = tileAt(leftG, yy);
			if (leftTile != null) {
				TileMask leftTileMask = leftTile.module.type.getTileMasks(currentBonuses)[leftTile.y - leftTile.module.y][leftTile.x - leftTile.module.x];
				if (leftTileMask.intersectsRect((int) getX() + left * AGame.SGS, (int) getY() + yy * AGame.SGS, flipped, (int) b2.getX(), (int) b2.getY(), (int) b2.getBBWidth(), (int) b2.getBBHeight())) {
					return true;
				}
			}
			Tile rightTile = tileAt(rightG, yy);
			if (rightTile != null) {
				TileMask rightTileMask = rightTile.module.type.getTileMasks(currentBonuses)[rightTile.y - rightTile.module.y][rightTile.x - rightTile.module.x];
				if (rightTileMask.intersectsRect((int) getX() + right * AGame.SGS, (int) getY() + yy * AGame.SGS, flipped, (int) b2.getX(), (int) b2.getY(), (int) b2.getBBWidth(), (int) b2.getBBHeight())) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean overlapsWith(Crewman b2) {
		// Map the rect's borders onto ship tiles and just look at those.
		int top = (int) StrictMath.floor((b2.getY() - getY()) / AGame.SGS);
		int bottom = (int) StrictMath.floor((b2.getY() + b2.getBBHeight() - getY()) / AGame.SGS);
		int left = (int) StrictMath.floor((b2.getX() - getX()) / AGame.SGS);
		int right = (int) StrictMath.floor((b2.getX() + b2.getBBWidth() - getX()) / AGame.SGS);
		
		if (top < 0 && left < 0 && bottom >= h && right >= w) {
			return true;
		}
		
		int ot = top, ob = bottom, ol = left, or = right;
		top = StrictMath.max(StrictMath.min(ob, 0), top); 
		bottom = StrictMath.min(StrictMath.max(ot, h - 1), bottom);
		left = StrictMath.max(StrictMath.min(or, 0), left);
		right = StrictMath.min(StrictMath.max(ol, w - 1), right);
		
		for (int xx = left; xx <= right; xx++) {
			int gx = gridXToWorldX(xx, 1);
			Tile topTile = tileAt(gx, top);
			if (topTile != null) {
				TileMask topTileMask = topTile.module.type.getTileMasks(currentBonuses)[topTile.y - topTile.module.y][topTile.x - topTile.module.x];
				if (topTileMask != TileMask.EMPTY) {
					return true;
				}
			}
			Tile bottomTile = tileAt(gx, bottom);
			if (bottomTile != null) {
				TileMask bottomTileMask = bottomTile.module.type.getTileMasks(currentBonuses)[bottomTile.y - bottomTile.module.y][bottomTile.x - bottomTile.module.x];
				if (bottomTileMask != TileMask.EMPTY) {
					return true;
				}
			}
		}
		
		int leftG = gridXToWorldX(left, 1);
		int rightG = gridXToWorldX(right, 1);
		for (int yy = top; yy <= bottom; yy++) {
			Tile leftTile = tileAt(leftG, yy);
			if (leftTile != null) {
				TileMask leftTileMask = leftTile.module.type.getTileMasks(currentBonuses)[leftTile.y - leftTile.module.y][leftTile.x - leftTile.module.x];
				if (leftTileMask != TileMask.EMPTY) {
					return true;
				}
			}
			Tile rightTile = tileAt(rightG, yy);
			if (rightTile != null) {
				TileMask rightTileMask = rightTile.module.type.getTileMasks(currentBonuses)[rightTile.y - rightTile.module.y][rightTile.x - rightTile.module.x];
				if (rightTileMask != TileMask.EMPTY) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public ArrayList<OverlappingTile> overlaps(Body b2) {
		ArrayList<OverlappingTile> overlaps = new ArrayList<OverlappingTile>();
		int tileW = getWidth();
		int bx = (int) b2.getX(), by = (int) b2.getY(), bw = (int) b2.getBBWidth(), bh = (int) b2.getBBHeight();
		if (flipped) {
			int tsz = tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = tiles.get(ti);
				TileMask tm = t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x];
				if (tm == TileMask.EMPTY) {
					continue;
				}
				double tx = getX() + (tileW - t.x - 1) * AGame.SGS;
				double ty = getY() + t.y * AGame.SGS;
				if (tm.intersectsRect((int) tx, (int) ty, flipped, bx, by, bw, bh)) {
					overlaps.add(new OverlappingTile(t, 1.0));
				}
			}
		} else {
			int tsz = tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = tiles.get(ti);
				TileMask tm = t.module.type.getTileMasks(currentBonuses)[t.y - t.module.y][t.x - t.module.x];
				if (tm == TileMask.EMPTY) {
					continue;
				}
				double tx = getX() + t.x * AGame.SGS;
				double ty = getY() + t.y * AGame.SGS;
				if (tm.intersectsRect((int) tx, (int) ty, flipped, bx, by, bw, bh)) {
					overlaps.add(new OverlappingTile(t, 1.0));
				}
			}
		}

		return overlaps;
	}

	private void collideWith(ArrayList<OverlappingTile> overlaps, double hitEnergy, Combat combat, LandFormation withLF, Airship withShip, boolean groundIsFloatingRock) {
		if (overlaps.isEmpty()) { return; }
		int dmg = (int) ((hitEnergy) / overlaps.size());
		if (withLF != null) {
			msSinceOnGround = 0;
			lastGrounded = withLF;
			collidedWithFloatingRock = groundIsFloatingRock;
		}
		crashSize = dmg > 20 ? 2 : dmg > 10 ? 1 : crashSize;
		if (crashSize == 2 && withShip != null) {
			justRammedBy.add(withShip);
		}
		if (withShip != null) {
			justCollidedWith.add(withShip);
		}
		if (dmg > 0) {
			int osz = overlaps.size();
			for (int ti = 0; ti < osz; ti++) {
				OverlappingTile ot = overlaps.get(ti);
				Tile t = ot.tile;
				int aDmg = StrictMath.max(0, (int) (dmg * ot.dmgMultiplier));
				if (!type.mobile && type.onGround || (t.armour.hp == t.armour.getMaxHP())) {
					aDmg -= t.armour.type.getBlastDmgAbsorb(currentBonuses) + t.armour.type.getPenDmgAbsorb(currentBonuses) + t.module.getShellPenAbsorb() + t.module.getShellBlastAbsorb() + 3;
				}
				if (aDmg <= 0) {
					continue;
				}
				combat.changeStat(this, "collisionDamage", aDmg);
				changeStat("collisionDamage", aDmg);
				t.module.doDamage(aDmg);
				// Insta-destroy module if hit.
				if (t.module.hp <= -t.module.type.getHp(currentBonuses) / 2) {
					t.module.hp = t.module.type.getHp(currentBonuses) * Module.BREAK_APART_HP - 1;
				}
				int prevHP = t.armour.hp;
				t.armour.hp = StrictMath.max(0, t.armour.hp - dmg / 4);
				for (ModuleType.FragmentImg fragI : t.armour.getFragments(prevHP, t.armour.hp)) {
					double speed = AGame.rnd(0.1, 0.25, 0.2, 0.4, 0.8) * 0.3;
					double angle = AGame.ANIM_R.nextDouble() * 2 * StrictMath.PI;
					double dx = speed * StrictMath.cos(angle);
					double dy = speed * StrictMath.sin(angle) - 0.3 * speed;
					combat.fragments.add(new Fragment(fragI.ssb, fragI.img,
							getIntX() + gridXToWorldX(t.x, 1) * AGame.SGS + fragI.dx,
							getIntY() + t.y * AGame.SGS + fragI.dy,
							dx, dy,
							0, AGame.ANIM_R.nextDouble() * 0.02 - 0.01,
							500 + AGame.ANIM_R.nextInt(1200),
							/* burnAmt */4
					));
				}
				if (withLF != null && t.y == h - 1 && dmg > 3) {
					if (AGame.ANIM_R.nextInt(3) == 0 ) {
						combat.particles.add(new Particle(ParticleType.ofName("dust"),
						getIntX() + gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2,
						getIntY() + t.y * AGame.SGS + AGame.SGS / 2));
					}
				}
			}
		}
	}

	private boolean isValidDecalLocation(DecalType type, int x, int y) {
		for (int dy = 0; dy < type.h; dy++) {
			for (int dx = 0; dx < type.w; dx++) {
				Tile t = tileAt(x + dx, y + dy);
				if (t == null) {// || t.armour.window) {
					return false;
				}
			}
		}
		return true;
	}

	private void clearInvalidDecals() {
		for (Iterator<Decal> it = decals.iterator(); it.hasNext();) {
			Decal d = it.next();
			if (!isValidDecalLocation(d.type, d.x, d.y)) {
				it.remove();
			}
		}
	}

	public boolean canAddDecal(DecalType type, int x, int y, int layer, Collection<Decal> ignore) {
		return isValidDecalLocation(type, x, y) && decalHasNoOverlaps(type, x, y, layer, ignore);
	}
	
	public boolean decalHasNoOverlaps(DecalType type, int x, int y, int layer, Collection<Decal> ignore) {
		for (Decal d : decals) {
			if (ignore.contains(d)) { continue; }
			if (d.layer == layer && x < d.x + d.type.w && d.x < x + type.w && y < d.y + d.type.h && d.y < y + type.h) {
				return false;
			}
		}
		return true;
	}

	public Decal addDecal(DecalType type, int x, int y, int layer) {
		Decal d = new Decal(type, x, y, layer);
		decals.add(d);
		return d;
	}

	public DecalType removeDecalAt(int x, int y, int preferredLayer) {
		for (Decal d : decals) {
			if (d.x <= x && d.y <= y && d.x + d.type.w > x && d.y + d.type.h > y && d.layer == preferredLayer) {
				decals.remove(d);
				return d.type;
			}
		}
		for (Decal d : decals) {
			if (d.x <= x && d.y <= y && d.x + d.type.w > x && d.y + d.type.h > y) {
				decals.remove(d);
				return d.type;
			}
		}
		return null;
	}
	
	public Module moduleThatObstructsFront(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		final int checkX = placeX + type.getW();
		for (int moduleSpaceY = 0; moduleSpaceY < type.getH(); moduleSpaceY++) {
			if (!type.isFrontOnly()[moduleSpaceY]) { continue; }
			int checkY = placeY + moduleSpaceY;
			for (Module m : modules) {
				if (ignore.contains(m)) { continue; }
				if (m.x >= checkX && m.y <= checkY && m.y + m.type.getH() > checkY) {
					if (best == null || m.x < best.x || (m.x == best.x && m.y < best.y)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleWhoseFrontIsObstructed(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		for (Module m : modules) {
			if (ignore.contains(m)) { continue; }
			final int checkX = m.x + m.type.getW();
			for (int moduleSpaceY = 0; moduleSpaceY < m.type.getH(); moduleSpaceY++) {
				if (!m.type.isFrontOnly()[moduleSpaceY]) { continue; }
				int checkY = m.y + moduleSpaceY;
				if (placeX >= checkX && placeY <= checkY && placeY + type.getH() > checkY) {
					if (best == null || m.x > best.x || (m.x == best.x && m.y < best.y)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleThatObstructsBack(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		final int checkX = placeX;
		for (int moduleSpaceY = 0; moduleSpaceY < type.getH(); moduleSpaceY++) {
			if (!type.isBackOnly()[moduleSpaceY]) { continue; }
			int checkY = placeY + moduleSpaceY;
			for (Module m : modules) {
				if (ignore.contains(m)) { continue; }
				if (m.x < checkX && m.y <= checkY && m.y + m.type.getH() > checkY) {
					if (best == null || m.x > best.x || (m.x == best.x && m.y < best.y)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleWhoseBackIsObstructed(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		for (Module m : modules) {
			if (ignore.contains(m)) { continue; }
			final int checkX = m.x;
			for (int moduleSpaceY = 0; moduleSpaceY < m.type.getH(); moduleSpaceY++) {
				if (!m.type.isBackOnly()[moduleSpaceY]) { continue; }
				int checkY = m.y + moduleSpaceY;
				if (placeX < checkX && placeY <= checkY && placeY + type.getH() > checkY) {
					if (best == null || m.x < best.x || (m.x == best.x && m.y < best.y)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleThatObstructsBottom(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		final int checkY = placeY + type.getH();
		for (int moduleSpaceX = 0; moduleSpaceX < type.getW(); moduleSpaceX++) {
			if (!type.isBottomOnly()[moduleSpaceX]) { continue; }
			int checkX = placeX + moduleSpaceX;
			for (Module m : modules) {
				if (ignore.contains(m)) { continue; }
				if (m.y >= checkY && m.x <= checkX && m.x + m.type.getW() > checkX) {
					if (best == null || m.y < best.y || (m.y == best.y && m.x < best.x)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleWhoseBottomIsObstructed(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		for (Module m : modules) {
			if (ignore.contains(m)) { continue; }
			final int checkY = m.y + m.type.getH();
			for (int moduleSpaceX = 0; moduleSpaceX < m.type.getW(); moduleSpaceX++) {
				if (!m.type.isBottomOnly()[moduleSpaceX]) { continue; }
				int checkX = m.x + moduleSpaceX;
				if (placeY >= checkY && placeX <= checkX && placeX + type.getW() > checkX) {
					if (best == null || m.y > best.y || (m.y == best.y && m.x < best.x)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleThatObstructsTop(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		final int checkY = placeY;
		for (int moduleSpaceX = 0; moduleSpaceX < type.getW(); moduleSpaceX++) {
			if (!type.isTopOnly()[moduleSpaceX]) { continue; }
			int checkX = placeX + moduleSpaceX;
			for (Module m : modules) {
				if (ignore.contains(m)) { continue; }
				if (m.y < checkY && m.x <= checkX && m.x + m.type.getW() > checkX) {
					if (best == null || m.y > best.y || (m.y == best.y && m.x < best.x)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public Module moduleWhoseTopIsObstructed(ModuleType type, final int placeX, final int placeY, Collection<Module> ignore) {
		Module best = null;
		for (Module m : modules) {
			if (ignore.contains(m)) { continue; }
			final int checkY = m.y;
			for (int moduleSpaceX = 0; moduleSpaceX < m.type.getW(); moduleSpaceX++) {
				if (!m.type.isTopOnly()[moduleSpaceX]) { continue; }
				int checkX = m.x + moduleSpaceX;
				if (placeY < checkY && placeX <= checkX && placeX + type.getW() > checkX) {
					if (best == null || m.y < best.y || (m.y == best.y && m.x < best.x)) {
						best = m;
					}
				}
			}
		}
		
		return best;
	}
	
	public boolean noModuleOverlapsOrObstructions(ModuleType type, int x, int y, boolean checkObstructions) {
		return noModuleOverlapsOrObstructions(type, x, y, checkObstructions, Collections.EMPTY_LIST);
	}
	
	public boolean noModuleOverlapsOrObstructions(ModuleType type, int x, int y, boolean checkObstructions, Collection<Module> ignore) {
		if (modules.isEmpty()) { return true; }
		// Check no modules overlap.
		for (Module m : modules) {
			if (ignore.contains(m)) { continue; }
			int aLeftEdge = x;
			int aRightEdge = x + type.getW();
			int aTopEdge = y;
			int aBottomEdge = y + type.getH();
			
			int bLeftEdge = m.x;
			int bRightEdge = m.x + m.type.getW();
			int bTopEdge = m.y;
			int bBottomEdge = m.y + m.type.getH();
			
			// Overlap
			if (aRightEdge > bLeftEdge && aLeftEdge < bRightEdge &&
				aBottomEdge > bTopEdge && aTopEdge < bBottomEdge)
			{
				return false;
			}
		}
		if (checkObstructions) {
			return
					moduleThatObstructsFront(type, x, y, ignore) == null && moduleWhoseFrontIsObstructed(type, x, y, ignore) == null &&
					moduleThatObstructsBack(type, x, y, ignore) == null && moduleWhoseBackIsObstructed(type, x, y, ignore) == null &&
					moduleThatObstructsBottom(type, x, y, ignore) == null && moduleWhoseBottomIsObstructed(type, x, y, ignore) == null &&
					moduleThatObstructsTop(type, x, y, ignore) == null && moduleWhoseTopIsObstructed(type, x, y, ignore) == null;
		} else {
			return true;
		}
	}

	public boolean canAddModule(ModuleType type, int x, int y) {
		if (modules.isEmpty()) { return true; }
		if (!noModuleOverlapsOrObstructions(type, x, y, /* checkObstructions */ true)) { return false; }
		boolean borders = false;
		boolean passableTile = false;
		for (Module m : modules) {
			// Vertical bordering
			if (m.x + m.type.getW() > x && m.x < x + type.getW() &&
				(m.y + m.type.getH() == y || m.y == y + type.getH())
			)
			{
				borders = true;
			}
			
			// Horizontal bordering
			if (m.y + m.type.getH() > y && m.y < y + type.getH() &&
				(m.x + m.type.getW() == x || m.x == x + type.getW())
			)
			{
				borders = true;
			}
			
			// Passable tile
			if (type.isOccupable() && isOccupable()) {
				lp: for (int ady = 0; ady < type.getH(); ady++) { for (int adx = 0; adx < type.getW(); adx++) {
					if (!type.canOccupy(adx, ady)) { continue; }
					for (int bdy = 0; bdy < m.type.getH(); bdy++) { for (int bdx = 0; bdx < m.type.getW(); bdx++) {
						if (!m.type.canOccupy(bdx, bdy)) { continue; }
						for (int[] adj : ADJ) {
							if (x + adx + adj[0] == m.x + bdx && y + ady + adj[1] == m.y + bdy) {
								passableTile = true;
								break lp;
							}
						}
					}}
				}}
			} else {
				passableTile = true;
			}
		}
		if (passableTile && type.isOccupable() && isOccupable()) {
			passableTile = false;
			boolean[] leftDoors = type.getLeftDoors();
			for (int i = 0; i < leftDoors.length; i++) {
				if (leftDoors[i]) {
					Tile toTheLeft = tileAt(x - 1, y + i);
					if (toTheLeft != null) {
						passableTile = toTheLeft.module.type.getRightDoors()[i - toTheLeft.module.y + y];
						if (passableTile) { break; }
					}
				}
			}
			if (!passableTile) {
				boolean[] rightDoors = type.getRightDoors();
				for (int i = 0; i < rightDoors.length; i++) {
					if (rightDoors[i]) {
						Tile toTheRight = tileAt(x + type.getW(), y + i);
						if (toTheRight != null) {
							passableTile = toTheRight.module.type.getLeftDoors()[i - toTheRight.module.y + y];
							if (passableTile) { break; }
						}
					}
				}
			}
			if (!passableTile) {
				boolean[] upDoors = type.getUpDoors();
				for (int i = 0; i < upDoors.length; i++) {
					if (upDoors[i]) {
						Tile up = tileAt(x + i, y - 1);
						passableTile = up != null;
						if (passableTile) { break; }
					}
				}
			}
			if (!passableTile) {
				for (int i = 0; i < type.getW(); i++) {
					Tile down = tileAt(x + i, y + type.getH());
					if (down != null) {
						passableTile = down.module.type.getUpDoors()[i - down.module.x + x];
						if (passableTile) { break; }
					}
				}
			}
		}
		return borders && passableTile;
	}
	
	public Module addModule(ModuleType type, int x, int y, ArmourType at, PaintType pt) {
		if (type.isExternal()) {
			at = ArmourType.ofName("NONE");
		}
		if (type.getArmourType() != null) {
			at = type.getArmourType();
		}
		Module m = new Module(this, type, x, y);
		modules.add(m);
		//System.out.println("adding " + m);
		m.hp = 1;
		m.maxHP = 1;
		//System.out.println("addModule paint " + pt);
		for (int dy = 0; dy < type.getH(); dy++) {
			for (int dx = 0; dx < type.getW(); dx++) {
				Tile t = new Tile(this, m, x + dx, y + dy);
				if (t.isMaskedEmpty()) {
					t.armour.setType(ArmourType.ofName("NONE"));
				} else {
					t.armour.setType(at);
					t.armour.paint = pt;
				}
				tiles.add(t);
				//System.out.println("adding tile " + t + " at " + t.x + " " + t.y);
			}
		}
		layout();
		clearInvalidDecals();
		resetCrew();
		paths.clear();
		tilePaths.clear();
		//calcPaths();
		//calcTilePaths();
		repair(/* resetXP */ true);
		shipwideTypes.clear();
		m.resetTentacles();
		return m;
	}
	
	public Module quickAddModule(ModuleType type, int x, int y) {
		ArmourType at = ArmourType.ofName("NONE");
		Module m = new Module(this, type, x, y);
		modules.add(m);
		//System.out.println("adding " + m);
		m.hp = 1;
		for (int dy = 0; dy < type.getH(); dy++) {
			for (int dx = 0; dx < type.getW(); dx++) {
				Tile t = new Tile(this, m, x + dx, y + dy);
				t.armour.setType(at);
				tiles.add(t);
			}
		}
		layout();
		shipwideTypes.clear();
		m.resetTentacles();
		return m;
	}

	public boolean isOccupable() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.isOccupable()) { return true; }
		}
		return false;
	}
	
	public void resetWeaponBarrels() {
		for (Module m : modules) { m.resetWeaponBarrels(); }
	}
	
	public boolean repair(boolean resetXP) {
		return repair(resetXP, true);
	}

	public boolean repair(boolean resetXP, boolean forceResetCrew) {
		version++;
		boolean repaired = false;
		aiControl = false;
		releaseOneUseLift = false;
		releaseOneUsePropulsion = false;
		releaseOneUseWeapons = false;
		switchedReserve = false;
		setDirectControl(-1);
		if (resetXP) {
			crewExperience = EmpireStat.CREW_STARTING_XP.get(currentBonuses);
			clearMedals();
		}
		paths.clear();
		tilePaths.clear();
		combatStats.clear();
		for (Module m : modules) {
			repaired = m.repairFully() | repaired;
		}
		recalculateBonuses();
		recalcHPs();
		for (Tile t : tiles) {
			repaired = t.armour.repair() | repaired;
		}
		for (Module m : modules) {
			repaired = m.repairFully() | repaired;
		}
		for (Decal d : decals) {
			d.enabled = true;
		}
		fireAt = null;
		board = null;
		assignJobsMs = ASSIGN_JOBS_EVERY;
		moveTo = new Pt(0, 0);
		flipTo = flipped;
		moveMode = MoveMode.DEFAULT;
		sitting = false;
		focusOnFirefighting = false;
		focusOnRepair = false;
		focusOnMoving = false;
		focusOnShooting = true;
		msSinceOnGround = 10000;
		hasBraked = 0;
		braking = 0;
		setxSpeed(0);
		setySpeed(0);
		setxForce(0);
		setyForce(0);
		flipMs = 0;
		unableToFlipMs = 0;
		captured = false;
		if (forceResetCrew || designedWorkingCrew() >= currentWorkingCrew()) {
			resetCrew();
			assignJobs();
			softResetCrew(); // Needed to have consistent crew job assignments either way.
		} else {
			softResetCrew();
		}
		commandPoints = commandPointsRequired();
		chunkSubIDCounter = 1;
		stuckParticles.clear();
		grounding = false;
		msSinceLastXMove = 0;
		timeMovingInSameXDirection = 0;
		movingLeft = false;
		lastGrounded = null;
		enginesRunning = false;
		suspendiumRunning = false;
		fallingTime = 0;
		inWater = false;
		explosionAmount = 0;
		moduleLossAmount = 0;
		biggestInFleet = false;
		outOfCombatMs = 0;
		notUnderCommandMs = 0;
		popOutCooldown = 0;
		mechTentacleDamageTaken = 0;
		hussarDamageTaken = 0;
		sawDamageTaken = 0;
		otherDamageTaken = 0;
		clearInvalidDecals();
		calcOriginalAdjacency();
		updateWeight();
		originalAmmoCapacity = getAmmoCapacity();
		originalCoalCapacity = getCoalCapacity();
		originalWaterCapacity = getWaterCapacity();
		originalRepairCapacity = getRepairCapacity();
		originalAllQuartered = getAllQuartered();
		clearAbilities();
		damageTaken = new int[] {0, 0};
		damageTakenFromAbove = new int[] {0, 0};
		damageMissed = new int[] {0, 0};
		fixers.clear();
		for (ModuleType mt : Loadable.all(ModuleType.class)) {
			if (ShipEditorUtils.isModuleAFixer(this, mt, currentBonuses)) {
				fixers.add(mt);
			}
		}
		if (uselessPostRepair() && !focusOnMoving) {
			focusOnFirefighting = false;
			focusOnRepair = false;
			focusOnShooting = false;
			focusOnMoving = true;
			softResetCrew();
		}
		calcConcavePoints();
		return repaired;
	}
	
	private void clearAbilities() {
		usedAbilities.clear();
		smokescreenTime = 0;
		burstOfSpeedTime = 0;
		superchargeSuspendiumTime = 0;
		doubleTimeTime = 0;
		fearTime = 0;
		tauntTime = 0;
		glimmerTime = 0;
		crosswindsTime = 0;
		cripplingShotTarget = false;
		disarmingShotTarget = false;
		paralysisTime = 0;
		momentOfDoubtTime = 0;
		gustOfWindTime = 0;
		suddenStormTime = 0;
		gustOfWindDX = 0;
		gustOfWindDY = 0;
		holdOnTime = 0;
	}
	
	public void storeStats() {
		prevCombatStats.clear();
		prevCombatStats.putAll(combatStats);
	}
	
	public void repairPartially() {
		if (!type.mobile) {
			repair(/* resetXP */ false);
			return;
		}
		version++;
		aiControl = false;
		releaseOneUseLift = false;
		releaseOneUsePropulsion = false;
		releaseOneUseWeapons = false;
		switchedReserve = false;
		setDirectControl(-1);
		paths.clear();
		tilePaths.clear();
		combatStats.clear();
		for (Module m : modules) {
			m.repairPartially();
		}
		recalculateBonuses();
		recalcHPs();
		for (Module m : modules) {
			m.repairPartially();
		}
		fireAt = null;
		board = null;
		assignJobsMs = ASSIGN_JOBS_EVERY;
		moveTo = new Pt(0, 0);
		flipTo = flipped;
		moveMode = MoveMode.DEFAULT;
		sitting = false;
		focusOnFirefighting = false;
		focusOnRepair = false;
		focusOnMoving = false;
		focusOnShooting = true;
		msSinceOnGround = 10000;
		hasBraked = 0;
		braking = 0;
		setxSpeed(0);
		setySpeed(0);
		setxForce(0);
		setyForce(0);
		flipMs = 0;
		unableToFlipMs = 0;
		captured = false;
		softResetCrew();
		commandPoints = commandPointsRequired();
		chunkSubIDCounter = 1;
		stuckParticles.clear();
		grounding = false;
		msSinceLastXMove = 0;
		timeMovingInSameXDirection = 0;
		movingLeft = false;
		lastGrounded = null;
		enginesRunning = false;
		suspendiumRunning = false;
		fallingTime = 0;
		explosionAmount = 0;
		moduleLossAmount = 0;
		biggestInFleet = false;
		outOfCombatMs = 0;
		popOutCooldown = 0;
		mechTentacleDamageTaken = 0;
		hussarDamageTaken = 0;
		sawDamageTaken = 0;
		otherDamageTaken = 0;
		calcOriginalAdjacency();
		updateWeight();
		originalAmmoCapacity = getAmmoCapacity();
		originalCoalCapacity = getCoalCapacity();
		originalWaterCapacity = getWaterCapacity();
		originalRepairCapacity = getRepairCapacity();
		originalAllQuartered = getAllQuartered();
		clearAbilities();
		damageTaken = new int[] {0, 0};
		damageTakenFromAbove = new int[] {0, 0};
		damageMissed = new int[] {0, 0};
		fixers.clear();
		for (ModuleType mt : Loadable.all(ModuleType.class)) {
			if (ShipEditorUtils.isModuleAFixer(this, mt, currentBonuses)) {
				fixers.add(mt);
			}
		}
		if (uselessPostRepair() && !focusOnMoving) {
			focusOnFirefighting = false;
			focusOnRepair = false;
			focusOnShooting = false;
			focusOnMoving = true;
			softResetCrew();
		}
		calcConcavePoints();
	}

	private void softResetCrew() {
		canDoPathingIndex = 0;
		boarderCanDoPathingIndex = 0;
		boarders.clear();
		for (int i = 0; i < crew.size(); i++) {
			Crewman c = crew.get(i);
			if (!c.alive() || c.tempSpawned) {
				crew.remove(i);
				i--;
			} else {
				c.softReset();
				if (!tiles.contains(c.spawnedAtTile)) {
					c.spawnedAtTile = null;
				}
				if (c.spawnedAtTile != null) {
					c.currentTile = c.spawnedAtTile;
				}
			}
		}

		LinkedList<Job> jobs = new LinkedList<Job>();
		for (Module m : modules) {
			for (Job j : m.jobs()) {
				if (j.active()) {
					jobs.add(j);
				}
			}
		}
		Collections.sort(jobs, this);
		ArrayList<Crewman> c2 = new ArrayList<Crewman>(crew);
		Collections.sort(c2, new Comparator<Crewman>() {
			@Override
			public int compare(Crewman o1, Crewman o2) {
				return Double.compare(o2.type.doesWork ? o2.type.crewEffectiveness : 0, o1.type.doesWork ? o1.type.crewEffectiveness : 0);
			}
		});

		for (int ci = 0; ci < c2.size(); ci++) {
			Crewman c = c2.get(ci);
			for (int ji = 0; ji < jobs.size(); ji++) {
				Job j = jobs.get(ji);
				if (j.requiredType(c.type)) {
					c.job = j;
					c.movingTowards = null;
					c.currentTile = getMostEmptyTile(j.module(), c);
					jobs.remove(ji);
					break;
				}
			}
		}
	}
	
	public boolean canRemoveModuleAt(int x, int y) {
		if (modules.size() == 1) { return true; }
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.x <= x && m.y <= y && m.x + m.type.getW() > x && m.y + m.type.getH() > y) {
				return isAtEdge(m) && canRemoveCleanly(m);
			}
		}
		return true;
	}
	
	private boolean isAtEdge(Module m) {
		/*for (int ty = m.y - 1; ty < m.y + m.type.getH() + 1; ty++) {
			for (int tx = m.x - 1; tx < m.x + m.type.getW() + 1; tx++) {
				if (tileAt(tx, ty) == null) { return true; }
			}
		}*/
		// Top/bottom
		for (int tx = m.x; tx < m.x + m.type.getW(); tx++) {
			if (tileAt(tx, m.y - 1) == null || tileAt(tx, m.y + m.type.getH()) == null) {
				return true;
			}
		}
		for (int ty = m.y; ty < m.y + m.type.getH(); ty++) {
			if (tileAt(m.x - 1, ty) == null || tileAt(m.x + m.type.getW(), ty) == null) {
				return true;
			}
		}
		return false;
	}

	private boolean canRemoveCleanly(Module m) {
		return pathChunks(m).size() == 1;
	}
	
	public Module removeModuleAt(int x, int y) {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.x <= x && m.y <= y && m.x + m.type.getW() > x && m.y + m.type.getH() > y) {
				removeModule(m);
				repair(/* resetXP */ true);
				layout();
				return m;
			}
		}
		return null;
	}
	
	public ModuleType moduleTypeAt(int x, int y) {
		Module m = moduleAt(x, y);
		return m == null ? null : m.type;
	}
	
	public Module moduleAt(int x, int y) {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.x <= x && m.y <= y && m.x + m.type.getW() > x && m.y + m.type.getH() > y) {
				return m;
			}
		}
		return null;
	}
	
	public Decal decalAt(int x, int y) {
		int dsz = decals.size();
		for (int mi = 0; mi < dsz; mi++) {
			Decal d = decals.get(mi);
			if (d.x <= x && d.y <= y && d.x + d.type.w > x && d.y + d.type.h > y) {
				return d;
			}
		}
		return null;
	}
	
	public Decal otherDecalAt(int x, int y, Decal first) {
		int dsz = decals.size();
		for (int mi = 0; mi < dsz; mi++) {
			Decal d = decals.get(mi);
			if (d != first && d.x <= x && d.y <= y && d.x + d.type.w > x && d.y + d.type.h > y) {
				return d;
			}
		}
		return null;
	}
	
	public void clear() {
		modules.clear();
		tiles.clear();
		crew.clear();
		boarders.clear();
		decals.clear();
		layout();
		resetCrew();
		paths.clear();
		tilePaths.clear();
		assignJobs();
		recalcHPs();
	}
	
	public void quickRemoveModule(Module m) {
		modules.remove(m);
		for (Iterator<Tile> it = tiles.iterator(); it.hasNext();) {
			if (it.next().module == m) {
				it.remove();
			}
		}
		layout();
		shipwideTypes.clear();
	}
	
	public void removeModule(Module m) {
		modules.remove(m);
		for (Iterator<Tile> it = tiles.iterator(); it.hasNext();) {
			if (it.next().module == m) {
				it.remove();
			}
		}
		layout();
		clearInvalidDecals();
		resetCrew();
		//calcPaths();
		//calcTilePaths();
		paths.clear();
		tilePaths.clear();
		assignJobs();
		recalcHPs();
		shipwideTypes.clear();
	}

	private void recalcHPs() {
		int bonusHPPerTile = getBonusHPPerTile();
		double structuralStressHPMultiplier = getStructuralStressHPMultiplier();
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			m.calcMaxHP(bonusHPPerTile, structuralStressHPMultiplier);
		}
	}
	
	public void layout() {
		double oldX = getX(), oldY = getY();
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			minX = StrictMath.min(minX, m.x);
			minY = StrictMath.min(minY, m.y);
			maxX = StrictMath.max(maxX, m.x + m.type.getW());
			maxY = StrictMath.max(maxY, m.y + m.type.getH());
		}
		int xShift = -minX;
		int yShift = -minY;
		int flippedXShift = w - maxX;
		w = maxX - minX;
		h = maxY - minY;
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			m.x += xShift;
			m.y += yShift;
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			t.x += xShift;
			t.y += yShift;
		}
		int dsz = decals.size();
		for (int di = 0; di < dsz; di++) {
			Decal d = decals.get(di);
			d.x += xShift;
			d.y += yShift;
		}
		if (flipped) {
			setX(getX() + flippedXShift * AGame.SGS);
		} else {
			setX(getX() - xShift * AGame.SGS);
		}
		setY(getY() - yShift * AGame.SGS);
		
		for (Particle p : stuckParticles) {
			p.x = p.x + oldX - getX();
			p.y = p.y + oldY - getY();
		}
		
		regenerateTileGrid();
		calcAdjacency();
		removeUnstuckParticles();
		updateWeight();
		shipwideTypes.clear();
		obstructedModules = null;
		isFullyConnectedInEditor = null;
		//long t = System.currentTimeMillis();
		frontDrag = Aerodynamics.drag(null, this, Aerodynamics.Dir.FROM_FRONT, false) / 2 + Aerodynamics.drag(null, this, Aerodynamics.Dir.FROM_BACK, false) / 2;
		backDrag = frontDrag;
		bottomDrag = Aerodynamics.drag(null, this, Aerodynamics.Dir.FROM_BOTTOM, false) / 2 + Aerodynamics.drag(null, this, Aerodynamics.Dir.FROM_TOP, false) / 2;
		topDrag = bottomDrag;
		//System.out.println("Drag recalc took " + (System.currentTimeMillis() - t) + "ms");
	}
	
	public ArrayList<Module> getObstructedModules() {
		if (obstructedModules == null) {
			obstructedModules = new ArrayList<Module>();
			ArrayList<Module> ignore = new ArrayList<Module>();
			for (int mi = 0; mi < modules.size(); mi++) {
				Module m = modules.get(mi);
				ignore.clear();
				ignore.add(m);
				Module obs = moduleWhoseFrontIsObstructed(m.type, m.x, m.y, ignore);
				if (obs == null) { obs = moduleWhoseBackIsObstructed(m.type, m.x, m.y, ignore); }
				if (obs == null) { obs = moduleWhoseTopIsObstructed(m.type, m.x, m.y, ignore); }
				if (obs == null) { obs = moduleWhoseBottomIsObstructed(m.type, m.x, m.y, ignore); }
				if (obs == null) { continue; }
				obstructedModules.add(m);
			}
		}
		return obstructedModules;
	}
	
	private void checkAndRepairTilesAndCrew() {
		// Check for tiles having wrong module.
		ArrayList<Tile> tiles2 = new ArrayList<Tile>(tiles);
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			for (int my = 0; my < m.type.getH(); my++) {
				for (int mx = 0; mx < m.type.getW(); mx++) {
					int ty = m.y + my;
					int tx = m.x + mx;
					int tsz = tiles2.size();
					boolean found = false;
					for (int ti = 0; ti < tsz; ti++) {
						Tile t = tiles2.get(ti);
						if (t.x == tx && t.y == ty) {
							if (t.module != m) {
								System.err.println(getName() + ": tile has wrong module");
								t.module = m;
							}
							tiles2.remove(ti);
							found = true;
							break;
						}
					}
					if (!found) {
						System.err.println(getName() + ": missing tile for module");
						ArmourType at = EditPalettePanel.guessArmourType(this);
						if (m.type.isExternal()) {
							at = ArmourType.ofName("NONE");
						}
						if (m.type.getArmourType() != null) {
							at = m.type.getArmourType();
						}
						Tile t = new Tile(this, m, tx, ty);
						t.armour.setType(at);
						tiles.add(t);
					}
				}
			}
		}
		
		if (!tiles2.isEmpty()) {
			System.err.println(getName() + ": found " + tiles2.size() + " orphaned tiles.");
			int numExpectedTiles = 0;
			for (Module m : modules) {
				numExpectedTiles += m.type.getW() * m.type.getH();
			}
			System.err.println(getName() + ": num expected tiles " + numExpectedTiles + " num tiles " + tiles.size());
			for (Tile t : tiles2) {
				System.err.println(t.x + " " + t.y + " " + t.module.type.name);
			}
			tiles.removeAll(tiles2);
		}
		
		for (int i = 0; i < 2; i++) {
			ArrayList<Crewman> cs = i == 0 ? crew : boarders;
			int csz = cs.size();
			for (int ci = 0; ci < csz; ci++) {
				Crewman c = cs.get(ci);
				if (!tiles.contains(c.currentTile)) {
					System.err.println(getName() + ": orphaned crewman on " + c.currentTile.x + " " + c.currentTile.y + " " + c.currentTile.module.type.name);
					int tsz = tiles2.size();
					boolean found = false;
					for (int ti = 0; ti < tsz; ti++) {
						Tile t = tiles.get(ti);
						if (t.x == c.currentTile.x && t.y == c.currentTile.y) {
							System.err.println(getName() + ": relocated to correct tile");
							c.currentTile = t;
							if (c.injuredCarried != null) {
								c.injuredCarried.currentTile = t;
							}
							found = true;
							c.abandonJob("orphaned");
							break;
						}
					}
					if (!found) {
						for (int ti = 0; ti < tsz; ti++) {
							Tile t = tiles.get(ti);
							if (t.canOccupy) {
								System.err.println(getName() + ": relocated to safe tile " + t.x + " " + t.y + " " + t.module.type.name);
								c.currentTile = t;
								if (c.injuredCarried != null) {
									c.injuredCarried.currentTile = t;
								}
								found = true;
								c.abandonJob("orphaned");
								break;
							}
						}
					}
					if (!found) {
						System.err.println(getName() + ": all crew deleted due to no safe tiles");
						cs.clear();
						break;
					}
				}
			}
		}
	}

	private void regenerateTileGrid() {
		if (tileGrid == null || tileGrid.length != h || tileGrid[0].length != w) {
			tileGrid = new Tile[h][w];
		} else {
			for (int y = 0; y < tileGrid.length; y++) {
				Arrays.fill(tileGrid[y], null);
			}
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			tileGrid[t.y][t.x] = t;
		}
	}
	
	private void calcAdjacency() {
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t1 = tiles.get(ti);
			for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
				Tile t = tileAt(t1.x + dx, t1.y + dy);
				t1.adjacent[dy + 1][dx + 1] = t != null;
				t1.adjacentFullTile[dy + 1][dx + 1] = t != null && t.full();
				t1.adjacentNonEmptyTile[dy + 1][dx + 1] = t != null && !t.isMaskedEmpty();
			}}
		}
		clearConcavePoints();
		// Clear hook boundary cache.
		topHookBoundaries = null; bottomHookBoundaries = null; leftHookBoundaries = null; rightHookBoundaries = null;
	}
	
	private void calcOriginalAdjacency() {
		if (!isPathingFullyConnected()) { return; }
		boolean hasDoor = false;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			if (modules.get(mi).type.isHatch(currentBonuses)) {
				hasDoor = true;
				break;
			}
		}
		if (!hasDoor) { return; }
		// Adjacency, the bad way.
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			final Tile t1 = tiles.get(ti);
			for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
				boolean adj = false;
				for (int ti2 = 0; ti2 < tsz; ti2++) {
					Tile t2 = tiles.get(ti2);
					if (t2.canOccupy && t2.x == t1.x + dx && t2.y == t1.y + dy) {
						adj = true;
						break;
					}
				}
				t1.hadAdjacentOccupableTile[dy + 1][dx + 1] = adj;
			}}
		}
	}
	
	private void resetCrew() {
		int startingNumberOfCrew = 0;
		for (int i = 0; i < crew.size(); i++) {
			Crewman c = crew.get(i);
			if (!c.type.canBoard && !c.type.canFly && !c.type.isMachine && c.active()) {
				startingNumberOfCrew++;
			}
		}
		canDoPathingIndex = 0;
		boarderCanDoPathingIndex = 0;
		crew.clear();
		boarders.clear();
		
		// Maintain a map of crew types to amounts still to be placed.
		HashMap<CrewType, Integer> crewRemaining = new HashMap<CrewType, Integer>();
		for (CrewType ct : Loadable.all(CrewType.class)) {
			crewRemaining.put(ct, getQuartered(ct));
		}
		
		for (Module m : modules) { m.numCrewTmp = 0; m.numGuardsTmp = 0; }
		
		// Place initial workers: Sort by worker competence and place.
		lp: for (CrewType ct : CrewType.allWorkersByAbility) {
			while (crewRemaining.get(ct) > 0) {
				boolean allFull = true;
				for (Module m : modules) {
					if (m.type.getCrew(currentBonuses) > m.numCrewTmp) {
						m.numCrewTmp++;
						allFull = false;
						Crewman cm = new Crewman(this, getMostEmptyTile(m, null), ct);
						cm.multiplayerControllerID = multiplayerControllerID;
						cm.owner = owner;
						crew.add(cm);
						crewRemaining.put(ct, crewRemaining.get(ct) - 1);
						if (crewRemaining.get(ct) == 0) {
							continue lp;
						}
					}
				}
				if (allFull) {
					break lp;
				}
			}
		}
		
		// Place mandatory guards: Units who guard but don't work go first, then others.
		lp: for (CrewType ct : CrewType.allGuardsByAbility) {
			while (crewRemaining.get(ct) > 0) {
				boolean allFull = true;
				for (Module m : modules) {
					if (m.type.getRecommendedGuards(currentBonuses) > m.numGuardsTmp) {
						m.numGuardsTmp++;
						allFull = false;
						Crewman cm = new Crewman(this, getMostEmptyTile(m, null), ct);
						cm.multiplayerControllerID = multiplayerControllerID;
						cm.owner = owner;
						crew.add(cm);
						crewRemaining.put(ct, crewRemaining.get(ct) - 1);
						if (crewRemaining.get(ct) == 0) {
							continue lp;
						}
					}
				}
				if (allFull) {
					break lp;
				}
			}
		}
		
		// Place guards in barracks: Place any remaining nonworker guards in their barracks.
		lp: for (CrewType ct : CrewType.nonWorkerGuards) {
			while (crewRemaining.get(ct) > 0) {
				boolean allFull = true;
				for (Module m : modules) {
					if (m.type.getQuartersType(currentBonuses) == ct) {
						m.numCrewTmp++;
						allFull = false;
						Crewman cm = new Crewman(this, getMostEmptyTile(m, null), ct);
						cm.multiplayerControllerID = multiplayerControllerID;
						cm.owner = owner;
						crew.add(cm);
						crewRemaining.put(ct, crewRemaining.get(ct) - 1);
						if (crewRemaining.get(ct) == 0) {
							continue lp;
						}
					}
				}
				if (allFull) {
					break lp;
				}
			}
		}
		
		// Place optional workers: Place remaining workers and worker guards in modules.
		lp: for (CrewType ct : CrewType.allWorkersByAbility) {
			while (crewRemaining.get(ct) > 0) {
				boolean allFull = true;
				for (Module m : modules) {
					if (m.type.getCrew(currentBonuses) + m.type.getOptionalCrew(currentBonuses) > m.numCrewTmp) {
						m.numCrewTmp++;
						allFull = false;
						Crewman cm = new Crewman(this, getMostEmptyTile(m, null), ct);
						cm.multiplayerControllerID = multiplayerControllerID;
						cm.owner = owner;
						crew.add(cm);
						crewRemaining.put(ct, crewRemaining.get(ct) - 1);
						if (crewRemaining.get(ct) == 0) {
							continue lp;
						}
					}
				}
				if (allFull) {
					break lp;
				}
			}
		}
		
		// Place remaining guards in modules that need guarding, but leave the boarders alone because they should be ready in their quarters.
		int extraGuards = 0;
		boolean progress = true;
		while (progress) {
			progress = false;
			extraGuards++;
			lp: for (CrewType ct : CrewType.nonBoarderGuardsByAbility) {
				if (crewRemaining.get(ct) > 0) {
					for (Module m : modules) {
						int recGuards = m.type.getRecommendedGuards(currentBonuses);
						if ((recGuards == 0 ? 0 : recGuards + extraGuards) > m.numGuardsTmp) {
							//System.out.println("placing extra " + ct.getName() + " in " + m.type.getName() + " @ " + extraGuards);
							progress = true;
							m.numGuardsTmp++;
							Crewman cm = new Crewman(this, getMostEmptyTile(m, null), ct);
							cm.multiplayerControllerID = multiplayerControllerID;
							cm.owner = owner;
							crew.add(cm);
							crewRemaining.put(ct, crewRemaining.get(ct) - 1);
							if (crewRemaining.get(ct) == 0) {
								continue lp;
							}
						}
					}
				}
			}
		}
		
		// Place any remaining crew in their quarters.
		lp: for (CrewType ct : Loadable.all(CrewType.class)) {
			while (crewRemaining.get(ct) > 0) {
				for (Module m : modules) {
					if (m.type.getQuartersType(currentBonuses) == ct) {
						m.numCrewTmp++;
						Crewman cm = new Crewman(this, getFixedQuartersTile(m, crewRemaining.get(ct)), ct);
						cm.multiplayerControllerID = multiplayerControllerID;
						cm.owner = owner;
						crew.add(cm);
						crewRemaining.put(ct, crewRemaining.get(ct) - 1);
						if (crewRemaining.get(ct) == 0) {
							continue lp;
						}
					}
				}
			}
		}
		
		// Update XP.
		int currentNumberOfCrew = 0;
		for (int i = 0; i < crew.size(); i++) {
			Crewman c = crew.get(i);
			if (!c.type.canBoard && !c.type.canFly && !c.type.isMachine && c.active()) {
				currentNumberOfCrew++;
			}
		}
		if (currentNumberOfCrew > startingNumberOfCrew) {
			crewExperience = (crewExperience * startingNumberOfCrew + EmpireStat.CREW_STARTING_XP.get(currentBonuses) * (currentNumberOfCrew - startingNumberOfCrew)) / currentNumberOfCrew;
		}
	}
	
	public void transferExperienceAndMedals(Airship source) {
		int myCrew = 0;
		for (int i = 0; i < crew.size(); i++) {
			Crewman c = crew.get(i);
			if (!c.type.canBoard && !c.type.canFly && !c.type.isMachine && c.active()) {
				myCrew++;
			}
		}
		int theirCrew = 0;
		for (int i = 0; i < source.crew.size(); i++) {
			Crewman c = source.crew.get(i);
			if (!c.type.canBoard && !c.type.canFly && !c.type.isMachine && c.active()) {
				theirCrew++;
			}
		}
		if (myCrew > theirCrew) {
			crewExperience = (source.crewExperience * theirCrew + EmpireStat.CREW_STARTING_XP.get(currentBonuses) * (myCrew - theirCrew)) / myCrew;
		} else {
			crewExperience = source.crewExperience;
		}
		clearMedals();
		for (Medal m : source.medals) {
			giveMedal(m);
		}
	}

	public boolean hasPath(Tile src, Tile target) {
		return tilePaths.containsKey(src) && tilePaths.get(src).containsKey(target);
	}
	
	public ArrayList<Tile> getPath(Tile src, Tile target) {
		if (!tilePaths.containsKey(src) || !tilePaths.get(src).containsKey(target)) {
			//#SpikeProfiler.start("tilepaths");
			calcTilePathsForTarget(target);
			//#SpikeProfiler.end("tilepaths");
		}
		return tilePaths.containsKey(src) ? tilePaths.get(src).get(target) : null;
	}
	
	public boolean hasPath(Tile src, Module target) {
		return paths.containsKey(src) && paths.get(src).containsKey(target);
	}

	public ArrayList<Tile> getPath(Tile src, Module target) {
		if (!paths.containsKey(src) || !paths.get(src).containsKey(target)) {
			//#SpikeProfiler.start("modulepaths");
			calcPathsForTarget(target);
			//#SpikeProfiler.end("modulepaths");
		}
		return paths.containsKey(src) ? paths.get(src).get(target) : null;
	}

	private void calcTilePathsForTarget(Tile target) {
		if (!target.canOccupy) { return; }
		LinkedList<Tile> tileQ = new LinkedList<Tile>();
		HashSet<Tile> tileQSet = new HashSet<Tile>();

		for (Tile t : tiles) {
			t.pathCostTmp = Integer.MAX_VALUE / 4;
		}
		target.pathCostTmp = 0;
		tileQ.add(target);
		tileQSet.add(target);
		while (!tileQ.isEmpty()) {
			Tile t = tileQ.pollFirst();
			tileQSet.remove(t);
			//int cost = t.pathCostTmp + t.getMoveDelay();
			for (int[] adj : ADJ) {
				if (adj[0] == -1 && adj[1] == 0 && t.x == t.module.x) {
					// Left
					if (!t.module.type.getLeftDoors()[t.y - t.module.y]) { continue; }
				} else if (adj[0] == 1 && adj[1] == 0 && t.x == t.module.x + t.module.type.getW() - 1) {
					// Right
					if (!t.module.type.getRightDoors()[t.y - t.module.y]) { continue; }
				} else if (adj[0] == 0 && adj[1] == -1 && t.y == t.module.y) {
					// Up
					if (!t.module.type.getUpDoors()[t.x - t.module.x]) { continue; }
				}
				Tile t2 = tileAt(t.x + adj[0], t.y + adj[1]);
				//if (t2 != null && t2.canOccupy && t2.pathCostTmp > cost) {
				if (t2 != null && t2.canOccupy && t2.pathCostTmp > t.pathCostTmp + t2.getMoveDelay()) {
					if (t2.module != t.module) {
						// Check there is a door leading into this tile.
						if (adj[0] == -1 && adj[1] == 0) {
							// From left into right
							if (!t2.module.type.getRightDoors()[t2.y - t2.module.y]) { continue; }
						} else if (adj[0] == 1 && adj[1] == 0) {
							// From right into left
							if (!t2.module.type.getLeftDoors()[t2.y - t2.module.y]) { continue; }
						} else if (adj[0] == 0 && adj[1] == 1) {
							// From top into bottom
							if (!t2.module.type.getUpDoors()[t2.x - t2.module.x]) { continue; }
						}
					}
					//t2.pathCostTmp = cost;
					t2.pathCostTmp = t.pathCostTmp + t2.getMoveDelay();
					if (!tileQSet.contains(t2)) {
						tileQ.add(t2);
						tileQSet.add(t2);
					}
				}
			}
		}

		for (Tile source : tiles) {
			if (!source.canOccupy) { continue; }
			ArrayList<Tile> path = new ArrayList<Tile>();
			Tile t = source;
			while (t.pathCostTmp > 0) {
				Tile best = null;
				int lowest = t.pathCostTmp;
				for (int[] adj : ADJ) {
					// Check if there is a door leading out of this tile.
					if (adj[0] == -1 && adj[1] == 0 && t.x == t.module.x) {
						// Left
						if (!t.module.type.getLeftDoors()[t.y - t.module.y]) { continue; }
					} else if (adj[0] == 1 && adj[1] == 0 && t.x == t.module.x + t.module.type.getW() - 1) {
						// Right
						if (!t.module.type.getRightDoors()[t.y - t.module.y]) { continue; }
					} else if (adj[0] == 0 && adj[1] == -1 && t.y == t.module.y) {
						// Up
						if (!t.module.type.getUpDoors()[t.x - t.module.x]) { continue; }
					}

					Tile t2 = tileAt(t.x + adj[0], t.y + adj[1]);
					if (t2 != null && t2.canOccupy && t2.pathCostTmp < lowest) {
						if (t2.module != t.module) {
							// Check there is a door leading into this tile.
							if (adj[0] == -1 && adj[1] == 0) {
								// From left into right
								if (!t2.module.type.getRightDoors()[t2.y - t2.module.y]) { continue; }
							} else if (adj[0] == 1 && adj[1] == 0) {
								// From right into left
								if (!t2.module.type.getLeftDoors()[t2.y - t2.module.y]) { continue; }
							} else if (adj[0] == 0 && adj[1] == 1) {
								// From top into bottom
								if (!t2.module.type.getUpDoors()[t2.x - t2.module.x]) { continue; }
							}
						}
						best = t2;
						lowest = t2.pathCostTmp;
					}
				}
				if (best == null) {
					path = null;
					break;
				}
				t = best;
				path.add(best);
			}
			if (!tilePaths.containsKey(source)) {
				tilePaths.put(source, new HashMap<Tile, ArrayList<Tile>>());
			}
			tilePaths.get(source).put(target, path);
		}
	}

	private void calcPathsForTarget(Module target) {
		LinkedList<Tile> tileQ = new LinkedList<Tile>();
		HashSet<Tile> tileQSet = new HashSet<Tile>();
		for (Tile t : tiles) {
			if (t.module == target && t.canOccupy) {
				t.pathCostTmp = 0;
				tileQ.add(t);
				tileQSet.add(t);
			} else {
				t.pathCostTmp = Integer.MAX_VALUE / 4;
			}
		}
		while (!tileQ.isEmpty()) {
			Tile t = tileQ.pollFirst();
			tileQSet.remove(t);
			//int cost = t.pathCostTmp + t.getMoveDelay();
			for (int[] adj : ADJ) {
				if (adj[0] == -1 && adj[1] == 0 && t.x == t.module.x) {
					// Left
					if (!t.module.type.getLeftDoors()[t.y - t.module.y]) { continue; }
				} else if (adj[0] == 1 && adj[1] == 0 && t.x == t.module.x + t.module.type.getW() - 1) {
					// Right
					if (!t.module.type.getRightDoors()[t.y - t.module.y]) { continue; }
				} else if (adj[0] == 0 && adj[1] == -1 && t.y == t.module.y) {
					// Up
					if (!t.module.type.getUpDoors()[t.x - t.module.x]) { continue; }
				}

				Tile t2 = tileAt(t.x + adj[0], t.y + adj[1]);
				//if (t2 != null && t2.canOccupy && t2.pathCostTmp > cost) {
				if (t2 != null && t2.canOccupy && t2.pathCostTmp > t.pathCostTmp + t2.getMoveDelay()) {
					if (t2.module != t.module) {
						// Check there is a door leading into this tile.
						if (adj[0] == -1 && adj[1] == 0) {
							// From left into right
							if (!t2.module.type.getRightDoors()[t2.y - t2.module.y]) { continue; }
						} else if (adj[0] == 1 && adj[1] == 0) {
							// From right into left
							if (!t2.module.type.getLeftDoors()[t2.y - t2.module.y]) { continue; }
						} else if (adj[0] == 0 && adj[1] == 1) {
							// From top into bottom
							if (!t2.module.type.getUpDoors()[t2.x - t2.module.x]) { continue; }
						}
					}

					//t2.pathCostTmp = cost;
					t2.pathCostTmp = t.pathCostTmp + t2.getMoveDelay();
					if (!tileQSet.contains(t2)) {
						tileQ.add(t2);
						tileQSet.add(t2);
					}
				}
			}
		}

		for (Tile source : tiles) {
			if (!source.canOccupy) { continue; }
			ArrayList<Tile> path = new ArrayList<Tile>();
			Tile t = source;
			while (t.pathCostTmp > 0) {
				Tile best = null;
				int lowest = t.pathCostTmp;
				for (int[] adj : ADJ) {
					// Check if there is a door leading out of this tile.
					if (adj[0] == -1 && adj[1] == 0 && t.x == t.module.x) {
						// Left
						if (!t.module.type.getLeftDoors()[t.y - t.module.y]) { continue; }
					} else if (adj[0] == 1 && adj[1] == 0 && t.x == t.module.x + t.module.type.getW() - 1) {
						// Right
						if (!t.module.type.getRightDoors()[t.y - t.module.y]) { continue; }
					} else if (adj[0] == 0 && adj[1] == -1 && t.y == t.module.y) {
						// Up
						if (!t.module.type.getUpDoors()[t.x - t.module.x]) { continue; }
					}

					Tile t2 = tileAt(t.x + adj[0], t.y + adj[1]);
					if (t2 != null && t2.canOccupy && t2.pathCostTmp < lowest) {
						if (t2.module != t.module) {
							// Check there is a door leading into this tile.
							if (adj[0] == -1 && adj[1] == 0) {
								// From left into right
								if (!t2.module.type.getRightDoors()[t2.y - t2.module.y]) { continue; }
							} else if (adj[0] == 1 && adj[1] == 0) {
								// From right into left
								if (!t2.module.type.getLeftDoors()[t2.y - t2.module.y]) { continue; }
							} else if (adj[0] == 0 && adj[1] == 1) {
								// From top into bottom
								if (!t2.module.type.getUpDoors()[t2.x - t2.module.x]) { continue; }
							}
						}

						best = t2;
						lowest = t2.pathCostTmp;
					}
				}
				if (best == null) {
					path = null;
					break;
				}
				t = best;
				path.add(best);
			}
			if (!paths.containsKey(source)) {
				paths.put(source, new HashMap<Module, ArrayList<Tile>>());
			}
			paths.get(source).put(target, path);
		}
	}
	
	public double fastResolveBoardingResult() {
		int attack = 0;
		for (int i = 0; i < boarders.size(); i++) {
			Crewman b = boarders.get(i);
			if (b.alive()) {
				attack += b.type.boardingCombatStrength();
			}
		}
		int defense = 0;
		for (int i = 0; i < crew.size(); i++) {
			Crewman c = crew.get(i);
			if (c.type.doesGuard && c.alive()) {
				defense += c.type.boardingCombatStrength();
			}
		}
		return attack * 1.0 / defense;
	}
	
	/** @return 0: no, 1: switch sides, 2: destroyed */
	public int fastResolveBoarding() {
		if (fastResolveBoardingResult() > 1) {
			for (int i = 0; i < boarders.size(); i++) {
				Crewman b = boarders.get(i);
				if (b.alive() && b.type.doesWork) {
					return switchSides() ? 1 : 2;
				}
			}
			return 2;
		}
		return 0;
	}
	
	public boolean shouldSwitchSides() {
		int msz = modules.size();
		int bsz = boarders.size();
		int csz = crew.size();
		if (bsz == 0) { return false; }
		boolean hasCrewInCommandCenter = false;
		boolean hasBoarderInCommandCenter = false;
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp <= 0 || m.type.getCommand(currentBonuses) <= 0) { continue; }
			if (!hasCrewInCommandCenter) {
				for (int ci = 0; ci < csz; ci++) {
					Crewman c = crew.get(ci);
					if (c.currentTile.module == m && c.type.doesWork && c.active()) {
						hasCrewInCommandCenter = true;
						break;
					}
				}
			}
			if (!hasBoarderInCommandCenter) {
				for (int bi = 0; bi < bsz; bi++) {
					Crewman b = boarders.get(bi);
					if (b.currentTile.module == m && b.type.doesWork && b.active()) {
						hasBoarderInCommandCenter = true;
						break;
					}
				}
			}
		}
		if (hasBoarderInCommandCenter && !hasCrewInCommandCenter) {
			return true;
		}
		if (hasCrewInCommandCenter) { return false; }
		// No-one in command? Let's try the alternate condition.
		for (int i = 0; i < csz; i++) {
			if ((crew.get(i).type.doesGuard || crew.get(i).type.canBoard) && crew.get(i).active()) { return false; }
		}
		for (int bi = 0; bi < bsz; bi++) {
			Crewman b = boarders.get(bi);
			if (b.type.doesWork && b.active()) { return true; }
		}
		return false;
	}
	
	public boolean switchSides() {
		if (owner == null) {
			VoteCounter<Integer> onBridgeVotes = new VoteCounter<Integer>();
			VoteCounter<Integer> onShipVotes = new VoteCounter<Integer>();
			int bsz = boarders.size();
			for (int bi = 0; bi < bsz; bi++) {
				Crewman b = boarders.get(bi);
				if (b.active()) {
					Module m = b.currentTile.module;
					if (m.hp > 0 && m.type.getCommand(BonusSet.empty()) > 0) {
						onBridgeVotes.count(b.multiplayerControllerID);
					}
					onShipVotes.count(b.multiplayerControllerID);
				}
			}

			multiplayerControllerID =
					!onBridgeVotes.isEmpty()
					? onBridgeVotes.winner()
					: !onShipVotes.isEmpty()
					? onShipVotes.winner()
					: 0;
		} else {
			VoteCounter<FleetOwnerRef> onBridgeVotes = new VoteCounter<FleetOwnerRef>();
			VoteCounter<FleetOwnerRef> onShipVotes = new VoteCounter<FleetOwnerRef>();
			int bsz = boarders.size();
			for (int bi = 0; bi < bsz; bi++) {
				Crewman b = boarders.get(bi);
				if (b.active() && b.owner != null) {
					Module m = b.currentTile.module;
					if (m.hp > 0 && m.type.getCommand(BonusSet.empty()) > 0) {
						onBridgeVotes.count(b.owner);
					}
					onShipVotes.count(b.owner);
				}
			}

			FleetOwnerRef mpo =
					!onBridgeVotes.isEmpty()
					? (FleetOwnerRef) onBridgeVotes.winner()
					: !onShipVotes.isEmpty()
					? (FleetOwnerRef) onShipVotes.winner()
					: null;
			if (mpo != null) {
				owner = mpo;
				multiplayerControllerID = 0;
			} else {
				return false;
			}
		}
				
		ArrayList<Crewman> oldBoarders = new ArrayList<Crewman>();
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			Crewman c = crew.get(ci);
			c.abandonJob("switching sides");
			if (c.type.canBoard || c.type.doesGuard) {
				oldBoarders.add(c);
				c.ship = null;
				c.boardingShip = this;
			} else {
				c.occupied = !c.occupied;
			}
		}
		int bsz = boarders.size();
		for (int bi = 0; bi < bsz; bi++) {
			Crewman b = boarders.get(bi);
			b.multiplayerControllerID = multiplayerControllerID;
			b.owner = owner;
			b.abandonJob("switching sides (boarder)");
			b.ship = this;
			b.boardingShip = null;
		}
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			modules.get(mi).prevTargetShip = null; // Reset targeting!
		}
		crew.removeAll(oldBoarders);
		crew.addAll(boarders);
		boarders.clear();
		boarders.addAll(oldBoarders);
		captured = !captured;
		focusOnShooting = true;
		focusOnRepair = false;
		focusOnFirefighting = false;
		focusOnMoving = false;
		moveMode = MoveMode.DEFAULT;
		fireAt = null;
		board = null;
		tetherAt = null;
		fireMode = FireMode.NORMAL;
		aircraftMode = AircraftBehaviourMode.NORMAL;
		directControlID = -1;
		setCaptainOnly(null);
		clearMedals();
		assignJobs();
		
		return true;
	}
	
	private void assignJobs() {
		// Clear any inactive or unsuitable jobs.
		int csz = crew.size();
		// Gather all active jobs not being served.
		ArrayList<Job> jobs = new ArrayList<Job>();
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			ArrayList<Job> mjobs = m.jobs();
			int jsz = mjobs.size();
			lp: for (int ji = 0; ji < jsz; ji++) {
				Job j = mjobs.get(ji);
				if (j.active()) {
					for (int ci = 0; ci < csz; ci++) {
						Crewman c = crew.get(ci);
						if (c.job == j) { continue lp; }
					}
					jobs.add(j);
				}
			}
		}
		// Sort by priority.
		Collections.sort(jobs, this);
		int jsz = jobs.size();
		for (int ji = 0; ji < jsz; ji++) {
			Job job = jobs.get(ji);
			//System.out.println("checking " + job.getClass().getSimpleName() + " " + job.module().type.name);
			
			// Find the module or crewman the job has to go to.
			ArrayList<Module> targetModules = new ArrayList<Module>();
			if (job.resource() == null) {
				targetModules.add(job.module());
			} else {
				for (int mi = 0; mi < msz; mi++) {
					Module m = modules.get(mi);
					if (m.getResource(job.resource()) > 0) {
						targetModules.add(m);
					}
				}
			}
			ArrayList<Crewman> targetCrew = new ArrayList<Crewman>();
			if (job.resource() == Resource.INJURED || job.resource() == Resource.DEAD) {
				for (int ci = 0; ci < csz; ci++) {
					Crewman c = crew.get(ci);
					if (c.needsRescue(job.resource() == Resource.DEAD)) {
						targetCrew.add(c);
					}
				}
			}
			if (targetModules.isEmpty() && targetCrew.isEmpty()) {
				continue;
			}
			Crewman best = null;
			Crewman targetCM = null;
			int leastdist = 0;
			// NB that assignment to marines last works because marines are added last!
			for (int attempt = 0; attempt < 2; attempt++) {
				for (int ci = 0; ci < csz; ci++) {
					Crewman c = crew.get(ci);
					//System.out.println("Considering crewman " + c.type.name + " at " + c.currentTile.module.type.name);
					if (!c.active()) { continue; }
					int d = Integer.MAX_VALUE;
					boolean suitable =
							c.ultimateBoardTarget == null
							&&
							job.requiredType(c.type)
							&&
							(!c.occupied || !job.requiredUnoccupied())
							&&
							(
							attempt == 0
								? (c.job == null)
								: (c.job != null && c.job.priority() < job.priority() * 0.7)
							);
					//System.out.println("suitable: " + suitable);
					if (suitable) {
						int tmsz = targetModules.size();
						for (int tmi = 0; tmi < tmsz; tmi++) {
							Module m = targetModules.get(tmi);
							//System.out.println("targetModule " + m.type.name);
							//d = StrictMath.min(d, paths.get(c.tile).get(m).size());
							ArrayList<Tile> path = getPath(c.currentTile, m);
							if (path == null) { continue; }
							ArrayList<Tile> modulePath = getPath(path.isEmpty() ? c.currentTile : path.get(path.size() - 1), job.module());
							if (modulePath == null) { continue; }
							//System.out.println("path " + path.size() + ", " + "modulePath " + modulePath.size());
							d = StrictMath.min(d,
									path.size() +
									modulePath.size());

						}
						int tcsz = targetCrew.size();
						for (int tci = 0; tci < tcsz; tci++) {
							Crewman tc = targetCrew.get(tci);
							ArrayList<Tile> pathTo = getPath(c.currentTile, tc.currentTile);
							if (pathTo == null) { continue; }
							ArrayList<Tile> pathFrom = getPath(tc.currentTile, job.module());
							if (pathFrom == null) { continue; }
							int myD = pathTo.size() + pathFrom.size();
							if (myD < d) {
								d = myD;
								targetCM = tc;
							}
						}
						if (d != Integer.MAX_VALUE) {
							int dMult = 2 + (c.job == null ? 0 : (int) c.job.priority());
							d = (int) (((d + 0.5) * 10 * dMult) * c.type.maxHP / c.hp / c.type.crewEffectiveness);
							//System.out.println("d = " + d);
							if (best == null || d < leastdist) {
								//System.out.println("best!");
								best = c;
								leastdist = d;
							}
						}
					}
				}
			}
			if (best != null) {
				/*System.out.println("new job " + job.getClass().getSimpleName() + " p " + job.priority());
				if (best.job != null) {
					System.out.println("old job " + best.job.getClass().getSimpleName() + " p " + best.job.priority());
				}*/
				best.abandonJob("as - assigning job " + job.getClass().getSimpleName());
				best.job = job;
				best.headingFor = targetCM;
				//System.out.println("assigning " + job.getClass().getSimpleName() + " " + job.module().type.name + " to " + best.type.name + " at " + best.currentTile.module.type.name);
				//System.out.println("Assigning " + job);
			}
		}
		
		//System.out.println("---");
	}
	
	public Tile getFixedQuartersTile(Module m, int n) {
		ArrayList<Utils.Pair<Integer, Integer>> hps = m.type.getHangarPositions();
		if (hps.isEmpty()) {
			return getMostEmptyTile(m, null);
		}
		Utils.Pair<Integer, Integer> offset = hps.get(n % hps.size());
		Tile t = tileAt(m.x + offset.a, m.y + offset.b);
		if (t != null && t.module == m) {
			return t;
		} else {
			return getMostEmptyTile(m, null);
		}
	}
	
	public Tile getMostEmptyTile(Module m, Crewman cmToPlace) {
		Tile t = null;
		int minOccupancy = 0;
		for (int dy = m.type.getH() - 1; dy >= 0; dy--) { for (int dx = 0; dx < m.type.getW(); dx++) {
			Tile t2 = tileAt(m.x + dx, m.y + dy);
			if (t2 == null) {
				throw new RuntimeException("The shape of the " + m.type.getName() + " module has changed since this ship was created. It is no longer valid.");
			}
			if (t2.canOccupy) {
				int occupancy = 0;
				for (Crewman c : crew) {
					if (c == cmToPlace) { break; }
					occupancy += c.currentTile == t2 ? 1 : 0;
				}
				if (t == null || occupancy < minOccupancy) {
					t = t2;
					minOccupancy = occupancy;
				}
			}
		}}
		if (t == null) {
			throw new RuntimeException("Unable to correctly place crew members.");
		}
		return t;
	}
	
	public Tile tileAt(int x, int y) {
		if (tileGrid == null) {
			for (Tile t : tiles) {
				if (t.x == x && t.y == y) { return t; }
			}
			return null;
		} else {
			if (x < 0 || y < 0 || x >= w || y >= h) {
				return null;
			} else {
				return tileGrid[y][x];
			}
		}
	}
	
	public Tile hit(Shot shot, Combat c, boolean onViewingSide, boolean[] crewHitRef) {
		int tileX = (int) ((flipped
				? getWidth() * AGame.SGS - (shot.tX - getIntX())
				: shot.tX - getIntX()) / AGame.SGS);
		int tileY = (int) ((shot.tY - getIntY()) / AGame.SGS);
		Tile t = tileAt(tileX, tileY);
		if (t != null && !t.isMaskedEmpty()) {
			t.hit(shot, c, onViewingSide, crewHitRef);
			if (shot.weapon != null && shot.weapon.type.destroySuspendiumInRadius() > 0) {
				destroySuspendiumInRadius(c, onViewingSide, tileX, tileY, shot.weapon.type.destroySuspendiumInRadius());
			}
			return t;
		}
		return null;
	}
	
	public boolean usesSuspendium() {
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.usesSuspendium()) { return true; }
		}
		return false;
	}
	
	public void destroySuspendiumInRadius(Combat c, boolean onViewingSide, int tx, int ty, int r) {
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				if (dy * dy + dx * dx > r * r) { continue; }
				int x = tx + dx;
				int y = ty + dy;
				Tile t = tileAt(x, y);
				if (t == null) { continue; }
				Module m = t.module;
				if (m.hp <= 0 || !m.type.usesSuspendium()) { continue; }
				m.explode(c, showingOutside);
				int eDmg = m.explodeDmg();
				if (eDmg > 0) {
					c.doSplashDmg(
								1, // blast damage
								getX() + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2,
								getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2,
								eDmg, m.type.getExplodeRadius(currentBonuses), null, this, null, null);
				}
				m.hp = m.type.getHp(currentBonuses) * Module.BREAK_APART_HP - 1;
			}
		}
	}
	
	// type: 0 pen 1 blast 2 direct
	public void splashHit(int type, Combat c, double centerX, double centerY, int damage, int splashRadius, boolean onViewingSide, Shot optionalSourceShot, boolean inside) {
		if (splashRadius < AGame.SGS) {
			splashRadius = AGame.SGS;
		}
		int rSq = splashRadius * splashRadius;
		
		// OK, go over each tile that could be affected. Calculate its position, and use that.
		int centerTileX = (int) ((flipped
					? getWidth() * AGame.SGS - (centerX - getIntX())
					: centerX - getIntX()) / AGame.SGS);
		int centerTileY = (int) ((centerY - getIntY()) / AGame.SGS);
		
		int range = (int) StrictMath.ceil(splashRadius / AGame.SGS);
		for (int yy = -range; yy <= range; yy++) {
			for (int xx = -range; xx <= range; xx++) {
				Tile t = tileAt(centerTileX + xx, centerTileY + yy);
				if (t == null || t.isMaskedEmpty()) { continue; }
				int tileX = getIntX() + gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2;
				int tileY = getIntY() + t.y * AGame.SGS + AGame.SGS / 2;
				double dSq = (centerX - tileX) * (centerX - tileX) + (centerY - tileY) * (centerY - tileY);
				if (dSq > rSq) { continue; }
				double d = StrictMath.sqrt(dSq);
				int actualDamage = (int) (damage * (splashRadius - d) / splashRadius);
				if (c == null) {
					t.testSplashDamage = actualDamage;
				} else {
					if (actualDamage == 0) { continue; }
					t.splashHit(type, c, onViewingSide, actualDamage, optionalSourceShot, inside);
				}
			}
		}
	}

	public ArrayList<Module> getModules() {
		return modules;
	}

	@Override
	public int compare(Job j1, Job j2) {
		return Double.compare(j2.priority(), j1.priority());
	}

	public ArrayList<Crewman> getCrew() {
		return crew;
	}
	
	public transient double dangerCache = 0;

	public double danger(Combat c) {
		if (!inCombat(c)) { return 0; }
		double q = canMove() ? getWeight() * getSpeed() * 0.7 : 1;
		boolean haf = hasActiveFlyers(c);
		boolean hab = hasActiveBoarders(c);
		int emsz = modules.size();
		for (int emi = 0; emi < emsz; emi++) {
			Module em = modules.get(emi);
			CrewType qt = em.type.getQuartersType(currentBonuses);
			if (qt != null && ((qt.canBoard && (hab || em.hp >= 0)) || (qt.canFly && (haf || em.hp >= 0)))) {
				q += qt.totalDamage() * 1000 * em.type.getQuarters(currentBonuses) / qt.weaponReload.get(currentBonuses);
			}
			if (em.hp <= 0) { continue; }
			if (em.type.isWeapon()) {
				q += em.type.approxDPS(currentBonuses);
				if (em.type.destroySuspendiumInRadius() > 0) {
					q += 50;
				}
			}
			if (!em.type.getTentacleSpecs().isEmpty()) {
				int tss = em.type.getTentacleSpecs().size();
				for (int tsi = 0; tsi < tss; tsi++) {
					q += em.type.getTentacleSpecs().get(tsi).getDPSEquivalent();
				}
			}
		}
		return q;
	}
	
	public void commandGiven() {
		commandPoints = 0;
		wasReadyForCommand = false;
	}

	public boolean readyForCommand() {
		return paralysisTime <= 0 && momentOfDoubtTime <= 0 && commandPoints >= commandPointsRequired() && generatesCommandPoints();
	}
	
	public int designedWorkingCrew() {
		int n = 0;
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.type.getQuarters(currentBonuses) > 0 && m.type.getQuartersType(currentBonuses).doesWork) {
				n += m.type.getQuarters(currentBonuses);
			}
		}
		return n;
	}
	
	public int currentWorkingCrew() {
		int n = 0;
		for (int ci = 0; ci < crew.size(); ci++) {
			Crewman cm = crew.get(ci);
			if (cm.alive() && cm.type.doesWork) {
				n++;
			}
		}
		return n;
	}
	
	public int commandPointsRequired() {
		int cps = 700;
		for (int ci = 0; ci < crew.size(); ci++) {
			Crewman cm = crew.get(ci);
			if (cm.alive() && cm.type.doesWork) {
				cps += cm.type.commandPointsRequired;
			}
		}
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			cps += m.type.getW() * m.type.getH() * 5 + m.type.getExtraCommandPointsRequired(currentBonuses);
		}
		cps = cps * Combat.TICK_LENGTH;
		if (captain != null) {
			cps = cps * (100 + captain.type.commandCooldownPercent) / 100;
		}
		cps = cps * (100 + commandCooldownPercentFromMedals) / 100;
		return StrictMath.max(100, cps);
	}
	
	public boolean generatesCommandPoints() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getCommand(currentBonuses) > 0 && m.running()) {
				return true;
			}
		}
		return false;
	}
	
	public int commandPointsGenerated() {
		double cp = 0;
		int minGen = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getCommand(currentBonuses) > 0 && m.running()) {
				minGen = 1;
				cp += m.type.getCommand(currentBonuses) * m.staffProportion();
			}
		}
		int cps = (int) StrictMath.ceil(StrictMath.log(cp * 1.5) * 4);
		return StrictMath.max(minGen, cps);
	}
	
	public boolean canGiveAircraftCommands() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.canGivePlaneCommands(currentBonuses) && m.somewhatStaffed()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasOneUseWeapons() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.isWeapon() && !m.type.canResupplyInCombat(currentBonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasOneUseLift() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.getLift(currentBonuses) > 0&& !m.type.canResupplyInCombat(currentBonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasOneUsePropulsion() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.getPropulsion(currentBonuses) > 0&& !m.type.canResupplyInCombat(currentBonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canGenerateCommands() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.getCommand(currentBonuses) > 0) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canFirefight() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.getResource(Resource.WATER) > 0 && m.hp > 0) { return true; }
		}
		return false;
	}
	
	public boolean canRepair() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.getResource(Resource.REPAIR) > 0 && m.hp > 0) { return true; }
		}
		return false;
	}
	
	public boolean canMove() {
		boolean propel = false;
		int msz = modules.size();
		if (!type.onGround && availableServiceCeiling(null) <= 0) {
			return false;
		}
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getPropulsion(currentBonuses) > 0 && m.hp > 0) {
				propel = true;
				if (m.type.getCoalReload(currentBonuses) == 0) {
					return true;
				}
			}
		}
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.getResource(Resource.COAL) > 0 && m.hp > 0) { return propel; }
		}
		return false;
	}

	public boolean isArmedCountingDamagedWeapons() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.isWeapon()) { return true; }
		}
		return false;
	}
	
	public boolean hasRam() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.isRam()) {
				return true;
			}
		}
		return false;
	}

	public boolean isArmedOrHasTroops() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.isWeapon() && m.hp > 0) {
				return true;
			}
			if (m.type.getQuartersType(currentBonuses) != null && (m.type.getQuartersType(currentBonuses).canBoard || m.type.getQuartersType(currentBonuses).canFly)) {
				return true;
			}
			if (m.hp > 0 && !m.tentacles.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean requiresAmmo() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.isWeapon() && m.hp > 0 && m.type.getClip(currentBonuses) > 0 && m.type.canResupplyInCombat(currentBonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canShoot() {
		boolean hasCoal = getTotalResource(Resource.COAL) > 0;
		boolean hasAmmo = getTotalResource(Resource.AMMO) > 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if ((m.type.isWeapon() || !m.type.getTentacleSpecs().isEmpty()) && m.hp > 0) {
				if ((m.type.getCoalReload(currentBonuses) == 0 || hasCoal || m.msUntilCoal > 0) && (m.type.getClip(currentBonuses) == 0 || (hasAmmo && m.type.canResupplyInCombat(currentBonuses)) || m.ammoLeft > 0)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public int availableLift(Combat c) {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.hasLift() && m.canRun()) {
				if (c != null) {
					int czStrength = c.inCrashZone(getX() + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2);
					switch (czStrength) {
						case 2:
							n += m.type.getLift(currentBonuses) * Combat.CRASH_ZONE_LIFT_MULT;
							break;
						case 1:
							n += m.type.getLift(currentBonuses) * Combat.CRASH_ZONE_HALF_STRENGTH_LIFT_MULT;
							break;
						default:
							n += m.type.getLift(currentBonuses);
							break;
					}
				} else {
					n += m.type.getLift(currentBonuses);
				}
			}
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.armour != null && t.armour.hp > 0) {
				int lift = t.armour.type.getLift(currentBonuses);
				if (lift != 0) {
					if (c != null) {
						int czStrength = c.inCrashZone(getX() + gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2);
						switch (czStrength) {
							case 2:
								n += lift * Combat.CRASH_ZONE_LIFT_MULT;
								break;
							case 1:
								n += lift * Combat.CRASH_ZONE_HALF_STRENGTH_LIFT_MULT;
								break;
							default:
								n += lift;
								break;
						}
					} else {
						n += lift;
					}
				}
			}
		}
		n = n * (100 + liftPercentFromMedals) / 100;
		if (captain != null) {
			n = n * (100 + captain.type.liftPercent) / 100;
		}
		if (superchargeSuspendiumTime > 0) {
			n *= SUPERCHARGE_SUSPENDIUM_MULT;
		}
		return n;
	}
	
	public int liftWithActivatedOneUseLifts() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (!m.type.canResupplyInCombat(currentBonuses) && (!releaseOneUseLift || m.msUntilCoal <= 0)) { continue; }
			n += m.type.getLift(currentBonuses);
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.armour != null) {
				n += t.armour.type.getLift(currentBonuses);
			}
		}
		n = n * (100 + liftPercentFromMedals) / 100;
		if (captain != null) {
			n = n * (100 + captain.type.liftPercent) / 100;
		}
		if (superchargeSuspendiumTime > 0) {
			n *= SUPERCHARGE_SUSPENDIUM_MULT;
		}
		return n;
	}
	
	public int availableLiftNoFuel() {
		int n = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.type.getCoalReload(currentBonuses) == 0 && m.type.hasLift() && m.hp > 0) { n += m.type.getLift(currentBonuses); }
		}
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			Tile t = tiles.get(ti);
			if (t.armour != null && t.armour.hp > 0) {
				n += t.armour.type.getLift(currentBonuses);
			}
		}
		n = n * (100 + liftPercentFromMedals) / 100;
		if (captain != null) {
			n = n * (100 + captain.type.liftPercent) / 100;
		}
		if (superchargeSuspendiumTime > 0) {
			n *= SUPERCHARGE_SUSPENDIUM_MULT;
		}
		return n;
	}

	public double availableSuspendiumForce(Combat c) {
		double distanceFromFloor = StrictMath.pow(StrictMath.max(1, AGame.GROUND_LEVEL - getY()), AGame.LIFT_Y_EXPONENT) / AGame.LIFT_Y_DIVIDER;
		return availableLift(c) * AGame.HEIGHT_FOR_DOUBLE_LIFT * 2 / (AGame.HEIGHT_FOR_DOUBLE_LIFT + distanceFromFloor) * AGame.G;
	}

	public double engineForceForX(double tx, Combat c) {
		int startSlowingDown = type.onGround ? 10 : (int) (StrictMath.min(1200, 10 + 210 / getSpeed() * (StrictMath.abs(getxSpeed()) + 0.01) / getSpeed()));
				
		if (type.onGround && StrictMath.abs(tx - getX()) < 12) {
			return 0;
		}
		
		if (StrictMath.abs(getX() - tx) > availableSpeed(/* requireLegsAndWheelsTouchingGround */ true) * startSlowingDown) {
			if (tx > getX()) {
				return availablePropulsion(/* requireLegsAndWheelsTouchingGround */ true);
			} else {
				return -availablePropulsion(/* requireLegsAndWheelsTouchingGround */ true);
			}
		}
		// Because we're crap at mathematics, let's just numerically integrate
		// to get the place the ship would end up with no further acceleration.
		// NB we're not crap at maths, this is an unsolved calculus problem.
		double simX = getX();
		double speedEpsilon = 0.001;
		double simXSpeed = getxSpeed();
		int ms = 64;
		int ticks = 0;
		double horizontalAirFriction = horizontalAirFriction(tx > getX()) * (c == null ? 1 : frictionMult(c));
		while (StrictMath.abs(simXSpeed) > speedEpsilon && ticks++ < 100) {
			double airSlowdownX = simXSpeed * simXSpeed * horizontalAirFriction * ms;
			if (simXSpeed > 0) { simXSpeed = Math.max(0, simXSpeed - airSlowdownX); }
			if (simXSpeed < 0) { simXSpeed = Math.min(0, simXSpeed + airSlowdownX); }
			simX += simXSpeed * ms;
		}
		double posEpsilon = 3;
		if (StrictMath.abs(simX - tx) < posEpsilon) {
			return 0;
		} else {
			if (tx > simX) {
				return availablePropulsion(/* requireLegsAndWheelsTouchingGround */ true);
			} else {
				return -availablePropulsion(/* requireLegsAndWheelsTouchingGround */ true);
			}
		}
	}

	public double suspendiumForceForY(double ty, boolean hasPreferredDelta, double preferredDelta, double maxSpeed, Combat c) {
		double f;
		if (StrictMath.abs(getySpeed()) < StrictMath.max(maxSpeed, 0.15)) {
			if (ty > getY()) {
				if (hasPreferredDelta) {
					f = StrictMath.max(0, getMass() * AGame.G - preferredDelta);
				} else {
					f = getMass() * AGame.G * 0.88;
				}
			} else {
				if (hasPreferredDelta) {
					f = getMass() * AGame.G + preferredDelta;
				} else {
					f = getMass() * AGame.G / 0.88;
				}
			}
		} else {
			f = getMass() * AGame.G;
		}
		return StrictMath.min(availableSuspendiumForce(c), f);
	}
	
	public int availableServiceCeiling(Combat c) {
		if (getMass() == 0) { return 0; }
		return (int) StrictMath.pow(StrictMath.max(1, (availableLift(c) * AGame.HEIGHT_FOR_DOUBLE_LIFT * 2 / getMass() - AGame.HEIGHT_FOR_DOUBLE_LIFT) * AGame.LIFT_Y_DIVIDER), 1 / AGame.LIFT_Y_EXPONENT);
	}
	
	public int availableServiceCeilingNoFuel() {
		if (getMass() == 0) { return 0; }
		return (int) StrictMath.pow(StrictMath.max(1, (availableLiftNoFuel() * AGame.HEIGHT_FOR_DOUBLE_LIFT * 2 / getMass() - AGame.HEIGHT_FOR_DOUBLE_LIFT) * AGame.LIFT_Y_DIVIDER), 1 / AGame.LIFT_Y_EXPONENT);
	}

	public int realServiceCeiling() {
		return serviceCeiling() - getHeight() * AGame.SGS;
	}

	public int serviceCeiling() {
		// The point at which suspendium force equals gravity force.
		// fg = g * m
		// fsusp = lift * HEIGHT_FOR_DOUBLE_LIFT * 2 / (HEIGHT_FOR_DOUBLE_LIFT + y) * g
		// g * m = lift * HEIGHT_FOR_DOUBLE_LIFT * 2 / (HEIGHT_FOR_DOUBLE_LIFT + y) * g // solve for y
		// m = lift * HEIGHT_FOR_DOUBLE_LIFT * 2 / (HEIGHT_FOR_DOUBLE_LIFT + y)
		// HEIGHT_FOR_DOUBLE_LIFT + y = (lift * HEIGHT_FOR_DOUBLE_LIFT * 2) / m
		// y = lift * HEIGHT_FOR_DOUBLE_LIFT * 2 / m - HEIGHT_FOR_DOUBLE_LIFT
		if (getMass() == 0) { return 0; }
		return (int) StrictMath.pow(Math.max(1, (getLift() * AGame.HEIGHT_FOR_DOUBLE_LIFT * 2 / getMass() - AGame.HEIGHT_FOR_DOUBLE_LIFT) * AGame.LIFT_Y_DIVIDER), 1 / AGame.LIFT_Y_EXPONENT);
	}
	
	public int serviceCeilingWithActivatedOneUseLifts() {
		if (getMass() == 0) { return 0; }
		return (int) StrictMath.pow(StrictMath.max(1, (liftWithActivatedOneUseLifts() * AGame.HEIGHT_FOR_DOUBLE_LIFT * 2 / getMass() - AGame.HEIGHT_FOR_DOUBLE_LIFT) * AGame.LIFT_Y_DIVIDER), 1 / AGame.LIFT_Y_EXPONENT);
	}
	
	public int serviceCeilingWithActivatedAbilitiesAndModules() {
		if (getMass() == 0) { return 0; }
		return (int) StrictMath.pow(StrictMath.max(1, (liftWithActivatedAbilitiesAndModules() * AGame.HEIGHT_FOR_DOUBLE_LIFT * 2 / getMass() - AGame.HEIGHT_FOR_DOUBLE_LIFT) * AGame.LIFT_Y_DIVIDER), 1 / AGame.LIFT_Y_EXPONENT);
	}

	public int turningCost() {
		int as = (int) StrictMath.ceil(getSpeed());
		if (as == 0) { return 0; }
		return 5 + w * 15 + 500 / as + getWeight() / 10;
	}

	public int availableTurningCost() {
		int as = (int) StrictMath.ceil(availableSpeed(/* requireLegsAndWheelsTouchingGround */ true));
		if (as == 0) { return 0; }
		return 5 + w * 15 + 500 / as + getWeight() / 10;
	}
	
	public boolean hasWorkingWeapons() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.isWeapon()) { return true; }
		}
		return false;
	}
	
	public boolean hasWorkingPropulsion() {
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.getPropulsion(currentBonuses) > 0) { return true; }
		}
		return false;
	}

	public double getPropulsion() {
		double prop = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (!m.type.canResupplyInCombat(currentBonuses)) { continue; }
			prop += m.type.getPropulsion(currentBonuses);
		}
		if (captain != null) {
			prop = prop * (100 + captain.type.propulsionPercent) / 100;
		}
		prop = prop * (100 + propulsionPercentFromMedals) / 100;
		if (burstOfSpeedTime > 0) {
			prop *= BURST_OF_SPEED_MULT;
		}
		if (crosswindsTime > 0) {
			prop /= CROSSWINDS_PROPULSION_DIV;
		}
		return prop;
	}

	public double availablePropulsion(boolean requireLegsAndWheelsTouchingGround) {
		double prop = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.canRun()) {
				double p = m.type.getPropulsion(currentBonuses);
				if (requireLegsAndWheelsTouchingGround) {
					if (!m.legs.isEmpty()) {
						int downLegs = 0;
						for (int li = 0; li < m.legs.size(); li++) {
							if (m.legs.get(li).foot.isDown) {
								downLegs++;
							}
						}
						if (downLegs == 0) {
							p = 0;
						} else if (downLegs < m.legs.size() - 1) {
							p /= 2;
						}
					}
					if (!m.wheels.isEmpty()) {
						int downWheels = 0;
						for (int li = 0; li < m.wheels.size(); li++) {
							if (m.wheels.get(li).onGround) {
								downWheels++;
							}
						}
						if (downWheels == 0) {
							p = 0;
						} else if (downWheels < m.wheels.size() / 2) {
							p /= 2;
						}
					}
				}
				prop += p;
			}
		}
		if (captain != null) {
			prop = prop * (100 + captain.type.propulsionPercent) / 100;
		}
		prop = prop * (100 + propulsionPercentFromMedals) / 100;
		if (burstOfSpeedTime > 0) {
			prop *= BURST_OF_SPEED_MULT;
		}
		if (crosswindsTime > 0) {
			prop /= CROSSWINDS_PROPULSION_DIV;
		}
		return prop;
	}
	
	public double availablePropulsionNoFuel(boolean requireLegsAndWheelsTouchingGround) {
		double prop = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			if (m.canRun() && m.type.getCoalReload(currentBonuses) == 0) {
				double p = m.type.getPropulsion(currentBonuses);
				if (requireLegsAndWheelsTouchingGround) {
					if (!m.legs.isEmpty()) {
						int downLegs = 0;
						for (int li = 0; li < m.legs.size(); li++) {
							if (m.legs.get(li).foot.isDown) {
								downLegs++;
							}
						}
						if (downLegs == 0) {
							p = 0;
						} else if (downLegs < m.legs.size() - 1) {
							p /= 2;
						}
					}
					if (!m.wheels.isEmpty()) {
						int downWheels = 0;
						for (int li = 0; li < m.wheels.size(); li++) {
							if (m.wheels.get(li).onGround) {
								downWheels++;
							}
						}
						if (downWheels == 0) {
							p = 0;
						} else if (downWheels < m.wheels.size() / 2) {
							p /= 2;
						}
					}
				}
				prop += p;
			}
		}
		if (captain != null) {
			prop = prop * (100 + captain.type.propulsionPercent) / 100;
		}
		prop = prop * (100 + propulsionPercentFromMedals) / 100;
		if (burstOfSpeedTime > 0) {
			prop *= BURST_OF_SPEED_MULT;
		}
		if (crosswindsTime > 0) {
			prop /= CROSSWINDS_PROPULSION_DIV;
		}
		return prop;
	}
	
	public double getMaxXSpeed() {
		double s = 10000;
		for (int i = 0; i < modules.size(); i++) {
			s = StrictMath.min(modules.get(i).type.maxXSpeed(currentBonuses), s);
		}
		return s;
	}

	public double getSpeed() {
		// Top speed: the point at which air resistance acceleration matches engine acceleration
		// ae = propulsion / mass
		// aair = speed * speed * airfriction
		// speed * speed * airfriction = propulsion / mass
		// speed * speed = propulsion / weight / airfriction
		// speed = (propulsion / mass / airfriction) ^ 0.5
		return StrictMath.min(getMaxXSpeed(), StrictMath.sqrt(getPropulsion() / getMass() / frontAirFriction()));
	}
	
	public double getSpeedNoFriction() {
		return StrictMath.min(getMaxXSpeed(), StrictMath.sqrt(getPropulsion() / getMass() / 0.0014));
	}
	
	public double getMainMapSpeed(BonusSet bonuses) {
		return getSpeed() * (type.onGround ? EmpireStat.LANDSHIP_MAP_SPEED_MULT.get(bonuses) : EmpireStat.AIRSHIP_MAP_SPEED_MULT.get(bonuses));
	}
	
	public double getReportedMainMapSpeed() {
 		return getSpeed() / (type.onGround ? 3 : 1);
 	}

	public double availableSpeed(boolean requireLegsAndWheelsTouchingGround) {
		return StrictMath.min(getMaxXSpeed(), StrictMath.sqrt(availablePropulsion(requireLegsAndWheelsTouchingGround) / getMass() / frontAirFriction()));
	}

	public boolean isBuildable(BonusSet boni) {
		for (Module m : modules) {
			if (m.type.getRequired() != null && !boni.contains[m.type.getRequired().ordinal()]) {
				//System.out.println("no " + m.type.getRequired().name);
				return false;
			}
		}
		for (Tile t : tiles) {
			if (t.armour.type.required != null && !boni.contains[t.armour.type.required.ordinal()]) {
				//System.out.println("no " + t.armour.type.required.name);
				return false;
			}
		}
		return true;
	}
	
	public BonusSet getRequiredBonuses() {
		BonusSet boni = new BonusSet();
		for (Module m : modules) {
			if (m.type.getRequired() != null) {
				boni.add(m.type.getRequired());
			}
		}
		for (Tile t : tiles) {
			if (t.armour.type.required != null && !boni.contains[t.armour.type.required.ordinal()]) {
				boni.add(t.armour.type.required);
			}
		}
		return boni;
	}
	
	public double groundOffset() {
		double totalSpringConstant = 0;
		int springLength = 0; // qqDPS can't deal with complex scenarios!
		for (Module m : modules) {
			for (Spring s : m.type.getSprings()) {
				totalSpringConstant += s.k;
				springLength = s.baseLength;
			}
			for (Spec s : m.type.getLegSpecs()) {
				totalSpringConstant += s.spring.k;
				springLength = s.spring.baseLength;
			}
		}
		if (totalSpringConstant == 0) { return 0; }
		// Spring force must equal gravity force.
		// weight * gravity = compression * spring constant
		// compression = (weight * gravity) / spring constant
		// offset = length - compression
		return StrictMath.max(0, springLength - getMass() * AGame.G / totalSpringConstant) * 0.75; // qqDPS fudge factor
	}
	
	public int maxCarryWeight() {
		double maxUpwardsForce = 0;
		for (Module m : modules) {
			if (m.type.getLegSpecs().size() > 1) {
				double legForce = 0;
				for (Leg.Spec ls : m.type.getLegSpecs()) {
					legForce += ls.spring.getMaxForce();
				}
				legForce *= (m.type.getLegSpecs().size() - 1.0) / m.type.getLegSpecs().size() * 0.75;
				maxUpwardsForce += legForce;
			}
			for (Spring spr : m.type.getSprings()) {
				maxUpwardsForce += spr.getMaxForce();
			}
		}
		return (int) (maxUpwardsForce / AGame.G * 0.6);
	}
	
	public int optimalCarryWeight() {
		return (int) (maxCarryWeight() * 0.85);
	}
	
	public transient double legBalanceFactor = 1.0;
	
	public void updateBalancingOnLegs(int ms) {
		// Basically, the more all the legs are off to the side of their hips, the worse it is.
		double lowestHipRelativeX = 100000;
		double highestHipRelativeX = -100000;
		double legHeightAccumulator = 0;
		int numLegs = 0;
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = modules.get(mi);
			int lsz = m.legs.size();
			for (int li = 0; li < lsz; li++) {
				Leg l = m.legs.get(li);
				if (l.isDown) {
					numLegs++;
					legHeightAccumulator += l.spec.upperLimbLength + l.spec.lowerLimbLength + l.spec.footHeight;
					double hipRelativeX = l.footX() - l.hipX();
					lowestHipRelativeX = StrictMath.min(lowestHipRelativeX, hipRelativeX);
					highestHipRelativeX = StrictMath.max(highestHipRelativeX, hipRelativeX);
				}
			}
		}
		
		if (numLegs == 0) {
			legBalanceFactor *= StrictMath.pow(0.999, ms);
			return;
		}
		
		if (lowestHipRelativeX <= 0 && highestHipRelativeX >= 0) {
			legBalanceFactor = 1.0;
		} else {
			double bestBalance = StrictMath.min(StrictMath.abs(lowestHipRelativeX), StrictMath.abs(highestHipRelativeX));
			double legHeight = legHeightAccumulator / numLegs;
			legBalanceFactor = StrictMath.max(0, 1.0 - bestBalance / legHeight);
		}
	}
	
	@Override
	public boolean canParticleStick(double px, double py) {
		if (!Rect2D.contains(getX(), getY(), getBBWidth(), getBBHeight(), px, py)) {
			return false;
		}
		int gx = (int) StrictMath.floor((px - getX()) / AGame.SGS);
		int gy = (int) StrictMath.floor((py - getY()) / AGame.SGS);
		Tile t = tileAt(gridXToWorldX(gx, 1), gy);
		if (t == null || !t.module.type.canParticlesStick()) { return false; }
		TileMask m = t.mask();
		return m == null || m.containsPoint(gx * AGame.SGS, gy * AGame.SGS, flipped, (int) Math.round(px - getX()), (int) Math.round(py - getY()));
	}
	
	public ArrayList<Mod> getUsedMods() {
		ArrayList<Mod> mods = new ArrayList<Mod>();
		for (Module m : modules) {
			if (m.type.sourceMod != null && !mods.contains(m.type.sourceMod)) {
				mods.add(m.type.sourceMod);
			}
		}
		for (Tile t : tiles) {
			if (t.armour.type.sourceMod != null && !mods.contains(t.armour.type.sourceMod)) {
				mods.add(t.armour.type.sourceMod);
			}
		}
		for (Decal d : decals) {
			if (d.type.sourceMod != null && !mods.contains(d.type.sourceMod)) {
				mods.add(d.type.sourceMod);
			}
		}
		return mods;
	}
	
	@Override
	public Airship clone() {
		return new Airship(this.toJSON(null, true), null);
	}
	
	public int cheapHash() {
		int h = 7;
		h = h * 31 + Double.valueOf(getX()).hashCode();
		h = h * 31 + Double.valueOf(getY()).hashCode();
		h = h * 31 + Double.valueOf(getxSpeed()).hashCode();
		h = h * 31 + Double.valueOf(getySpeed()).hashCode();
		int tsz = tiles.size();
		for (int ti = 0; ti < tsz; ti++) {
			h = h * 37 + tiles.get(ti).cheapHash();
		}
		int msz = modules.size();
		for (int mi = 0; mi < msz; mi++) {
			h = h * 41 + modules.get(mi).cheapHash();
		}
		int csz = crew.size();
		for (int ci = 0; ci < csz; ci++) {
			h = h * 29 + crew.get(ci).cheapHash();
		}
		int bsz = boarders.size();
		for (int bi = 0; bi < bsz; bi++) {
			h = h * 43 + boarders.get(bi).cheapHash();
		}
		return h;
	}

	public int getOriginalAmmoCapacity() {
		return originalAmmoCapacity == -1 ? getAmmoCapacity() : originalAmmoCapacity;
	}

	public int getOriginalCoalCapacity() {
		return originalCoalCapacity == -1 ? getCoalCapacity() : originalCoalCapacity;
	}

	public int getOriginalWaterCapacity() {
		return originalWaterCapacity == -1 ? getWaterCapacity() : originalWaterCapacity;
	}

	public int getOriginalRepairCapacity() {
		return originalRepairCapacity == -1 ? getRepairCapacity() : originalRepairCapacity;
	}

	public int getOriginalAllQuartered() {
		return originalAllQuartered == -1 ? getAllQuartered() : originalAllQuartered;
	}
	
	public void setStat(String key, int value) {
		combatStats.put(key, value);
	}
	
	public int getStat(String key) {
		if (prevCombatStats.isEmpty()) { return getCurrentStat(key); }
		return prevCombatStats.containsKey(key) ? prevCombatStats.get(key) : 0;
	}
	
	public int getCurrentStat(String key) {
		return combatStats.containsKey(key) ? combatStats.get(key) : 0;
	}
	
	public void changeStat(String key, int value) {
		combatStats.put(key, getCurrentStat(key) + value);
	}
	
	public void incStat(String key) {
		changeStat(key, 1);
	}
	
	public void replace(ModuleType src, ModuleType trg) {
		if (!trg.canReplace(src)) { return; }
		for (int i = 0; i < modules.size(); i++) {
			Module m = modules.get(i);
			if (m.type == src) {
				Module m2 = new Module(this, trg, m.x, m.y);
				modules.set(i, m2);
				for (int ti = 0; ti < tiles.size(); ti++) {
					Tile t = tiles.get(ti);
					if (t.module == m) {
						t.module = m2;
						if (t.isMaskedEmpty()) {
							t.armour.setType(ArmourType.ofName("NONE"));
						} else if (trg.getArmourType() != null) {
							t.armour.type = trg.getArmourType();
						} else if (trg.isExternal()) {
							t.armour.setType(ArmourType.ofName("NONE"));
						} else if (src.getArmourType() != null || src.isExternal()) {
							t.armour.type = ArmourType.ofName("LT_WOOD");
						}
						t.updateCanOccupy();
					}
				}
			}
		}
		layout();
		clearInvalidDecals();
		resetCrew();
		paths.clear();
		tilePaths.clear();
		//calcPaths();
		//calcTilePaths();
		repair(/* resetXP */ true);
		shipwideTypes.clear();
	}
	
	public void replace(DecalType src, DecalType trg) {
		if (!trg.canReplace(src)) { return; }
		for (Decal d : decals) {
			if (d.type == src) {
				d.type = trg;
			}
		}
	}
	
	public void replace(ArmourType src, ArmourType trg) {
		for (Tile t : tiles) {
			if (t.module.type.isExternal() || t.module.type.getArmourType() != null) { continue; }
			if (t.armour.type == src) {
				t.armour.type = trg;
			}
		}
		repair(/* resetXP */ true);
	}
	
	public void replace(PaintType src, PaintType trg) {
		for (Tile t : tiles) {
			if (t.armour.paint == src) {
				t.armour.paint = trg;
			}
		}
		for (Module m : modules) {
			if (m.externalPaint == src) {
				m.externalPaint = trg;
			}
		}
		for (Decal d : decals) {
			if (d.paint == src) {
				d.paint = trg;
			}
		}
	}
	
	public BonusSet getBaseBonuses() {
		return baseBonuses.clone();
	}

	public TacticalAI getAI() {
		return ai;
	}

	public void setAI(TacticalAI ai) {
		this.ai = ai;
	}
	
	public void halveResources() {
		for (int i = 0; i < modules.size(); i++) {
			Module m = modules.get(i);
			for (Resource r : Resource.values()) {
				if (m.getResource(r) > 0) {
					m.setResource(r, StrictMath.max(1, m.getResource(r) / 2));
				}
			}
		}
	}
	
	public void setBaseBonuses(BonusSet bonuses) {
		if (bonuses.equals(baseBonuses)) { return; }
		baseBonuses.clear();
		baseBonuses.addAll(bonuses);
		recalculateBonuses();
	}
	
	private void recalculateBonuses() {
		currentBonuses.clear();
		currentBonuses.addAll(baseBonuses);
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.type.providesBonus() != null && (m.hp > 0 || m.type.providesBonusWhenDestroyed())) {
				currentBonuses.add(m.type.providesBonus());
			}
		}
		if (getCaptain() != null && getCaptain().type.shipBonus != null) {
			currentBonuses.set(getCaptain().type.shipBonus.ordinal(), true);
		}
		cachedCostVersion = -1;
		cachedMaintenanceVersion = -1;
		cachedSupplyCapacityVersion = -1;
		cachedSupplyRequiredVersion = -1;
	}
	
	public static String nameList(List<? extends HasName> l, Locale lang) {
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
			HasName t = l.get(i);
			sb.append(t.getName());
		}
		return sb.toString();
	}

	public int getDirectControlID() {
		return directControlID;
	}

	// Set to -1 to clear.
	public void setDirectControl(int directControlID) {
		this.directControlID = directControlID;
		if (directControlID != -1) {
			speedOrder = ShipSpeed.STOP;
			speedOrderSpoken = ShipSpeed.STOP;
			altitudeOrder = (int) getY();
		} else {
			moveTo = new Pt(getX(), getY());
			flipTo = flipped;
			for (int i = 0; i < modules.size(); i++) {
				modules.get(i).fireMode = Module.DirectControlFireMode.AUTO;
				modules.get(i).targetTroopOverride = null;
			}
		}
	}

	// Hero stuff
	public Hero getCaptain() {
		return captain;
	}

	public void setCaptain(Hero captain) {
		if (this.captain != captain) { version++; }
		this.captain = captain;
		this.captainType = captain == null ? null : captain.type;
		recalculateBonuses();
		repair(/* resetXP */ false);
	}
	
	public void setCaptainOnly(Hero captain) {
		if (this.captain != captain) { version++; }
		this.captain = captain;
		this.captainType = captain == null ? null : captain.type;
		recalculateBonuses();
	}
	
	public boolean canResurrect() {
		for (int ci = 0; ci < crew.size(); ci++) {
			if (crew.get(ci).hp == 0) { return true; }
		}
		return false;
	}
	
	public boolean resurrect(Combat c) {
		boolean didSomething = false;
		for (int ci = 0; ci < crew.size(); ci++) {
			didSomething = crew.get(ci).resurrect(c) | didSomething;
		}
		return didSomething;
	}
	
	public boolean canDoEngineeringMiracle() {
		if (holdOnTime > 0) { return false; }
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.hp <= 0) {
				return true;
			}
		}
		return false;
	}
	
	public boolean engineeringMiracle(Combat c) {
		if (holdOnTime > 0) { return false; }
		ParticleType spark = ParticleType.ofName("gain_spark");
		boolean didSomething = false;
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.hp <= 0) {
				m.hp = m.getMaxHP() / 5;
				m.lowestHP = m.hp;
				m.fire = 0;
				didSomething = true;
				for (int i = 0; i < 7; i++) {
					c.particles.add(new Particle(spark, getX() + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + AGame.ANIM_R.nextInt(m.type.getW() * AGame.SGS), getY() + m.y * AGame.SGS + AGame.ANIM_R.nextInt(m.type.getW() * AGame.SGS)));
				}
			}
		}
		return didSomething;
	}
	
	public boolean canExtinguish() {
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.fire > 0) {
				return true;
			}
		}
		return false;
	}
	
	public boolean extinguish(Combat c) {
		ParticleType spark = ParticleType.ofName("necromancy");
		boolean didSomething = false;
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.fire > 0) {
				m.fire = 0;
				didSomething = true;
				for (int i = 0; i < 7; i++) {
					c.particles.add(new Particle(spark, getX() + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + AGame.ANIM_R.nextInt(m.type.getW() * AGame.SGS), getY() + m.y * AGame.SGS + AGame.ANIM_R.nextInt(m.type.getW() * AGame.SGS)));
				}
			}
		}
		return didSomething;
	}
	
	public boolean canImproviseResource(Resource r) {
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			if (m.hp > 0 && m.type.getResourceCapacity(r, currentBonuses) > 0 && m.getResource(r) < m.type.getResourceCapacity(r, currentBonuses)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean improviseResource(Resource r, Combat c) {
		boolean didSomething = false;
		ParticleType spark = ParticleType.ofName("gain_spark");
		for (int mi = 0; mi < modules.size(); mi++) {
			Module m = modules.get(mi);
			int maxAmt = m.type.getResourceCapacity(r, currentBonuses);
			int oldAmt = m.getResource(r);
			if (m.hp > 0 && maxAmt > 0 && oldAmt < maxAmt) {
				didSomething = true;
				int newAmt = StrictMath.min(maxAmt, m.getResource(r) + maxAmt * 2 / 5);
				m.setResource(r, newAmt);
				int difference = newAmt - oldAmt;
				for (int i = 0; i < difference * 2; i++) {
					c.particles.add(new Particle(spark, getX() + gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + AGame.ANIM_R.nextInt(m.type.getW() * AGame.SGS), getY() + m.y * AGame.SGS + AGame.ANIM_R.nextInt(m.type.getW() * AGame.SGS)));
				}
			}
		}
		return didSomething;
	}
	
	public double getHeightToBaseRatio() {
		int maxW = 0;
		int highestWY = 0;
		for (int y = h -1; y >= 0; y--) {
			int localW = 0;
			for (int x = 0; x < w; x++) {
				if (tileAt(x, h - 1) != null && tileAt(x, h - 1).solid()) {
					localW++;
				}
			}
			if (localW >= maxW) {
				maxW = localW;
				highestWY = y;
			}
		}
		int maxBaseW = 1;
		for (int y = h - 1; y >= highestWY; y--) {
			int baseW = 1;
			for (int x = 0; x < w; x++) {
				if (tileAt(x, y) != null && tileAt(x, y).solid()) {
					baseW++;
				}
			}
			if (baseW > maxBaseW) { maxBaseW = baseW; }
		}
		return h * 1.0 / maxBaseW;
	}
	
	public void addCaptainsGuard(Combat c) {
		if (captain != null && captain.type.guardCrewType != null) {
			Module guardModule = null;
			for (Module m : modules) {
				if (m.type.getOccupableTileCount() == 0) { continue; }
				if (m.type.getCommand(currentBonuses) > 0 && (guardModule == null || m.type.getCommand(currentBonuses) > guardModule.type.getCommand(currentBonuses))) {
					guardModule = m;
				}
			}
			if (guardModule == null) {
				for (Module m : modules) {
					if (m.type.getOccupableTileCount() == 0) { continue; }
					if (m.type.getQuarters(currentBonuses) > 0) {
						guardModule = m;
						break;
					}
				}
			}
			if (guardModule == null) {
				for (Module m : modules) {
					if (m.type.getOccupableTileCount() == 0) { continue; }
					guardModule = m;
					break;
				}
			}
			if (guardModule != null) {
				ParticleType pt = ParticleType.ofName("gain_spark");
				for (int i = 0; i < captain.type.numGuards; i++) {
					Crewman cm = new Crewman(this, getMostEmptyTile(guardModule, null), captain.type.guardCrewType);
					cm.multiplayerControllerID = multiplayerControllerID;
					cm.owner = owner;
					cm.tempSpawned = true;
					crew.add(cm);
					double gameX = getIntX() + gridXToWorldX(cm.currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
					double gameY = getIntY() + cm.currentTile.y * AGame.SGS + AGame.SGS / 2;
					for (int j = 0; j < 5; j++) {
						c.particles.add(new Particle(pt, gameX, gameY));
					}
				}
			}
		}
	}
}
