package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.airships.Combat.Side;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import com.zarkonnen.catengine.util.Utils.Pair;
import org.json.JSONObject;

public class  PlaceShipTool extends UniScreen.Tool {
	@Override public String getLabel() { return _t("Add_" + shipToPlace.type.name()); }
	
	public Airship shipToPlace;
	public boolean cancelPutsBack;
	public boolean flipped;
	public boolean autoFlipped = false;
	public boolean autoFlipInited = false;
	public int sideIndex;
	public Side side;
	public boolean forConstruction;
	public boolean fromReserve;
	public boolean flanking;
	public boolean ignoreServiceCeiling;
	public LandFormation landscapedGround;
	public boolean multiPlace;
	
	public static final int EITHER_SIDE = -1;
	
	public PlaceShipTool(Airship shipToPlace, boolean cancelPutsBack, int sideIndex, boolean ignoreServiceCeiling) {
		this.shipToPlace = shipToPlace;
		this.cancelPutsBack = cancelPutsBack;
		this.sideIndex = sideIndex;
		this.ignoreServiceCeiling = ignoreServiceCeiling;
		flipped = shipToPlace.flipped;
	}
	
	public PlaceShipTool(Airship shipToPlace, boolean cancelPutsBack, Side side, int sideIndex) {
		this.shipToPlace = shipToPlace;
		this.cancelPutsBack = cancelPutsBack;
		this.side = side;
		this.sideIndex = sideIndex;
		flipped = shipToPlace.flipped;
	}
	
	public PlaceShipTool(Airship shipToPlace, boolean cancelPutsBack, Side side, int sideIndex, boolean forConstruction) {
		this.shipToPlace = shipToPlace;
		this.cancelPutsBack = cancelPutsBack;
		this.side = side;
		this.sideIndex = sideIndex;
		this.forConstruction = forConstruction;
		flipped = shipToPlace.flipped;
	}
	
	public PlaceShipTool(Airship shipToPlace, boolean fromReserve) {
		this.shipToPlace = shipToPlace;
		this.fromReserve = fromReserve;
		flipped = shipToPlace.flipped;
	}
	
	public PlaceShipTool(Airship shipToPlace, Side side, int sideIndex, boolean fromReserve, boolean flanking) {
		this.shipToPlace = shipToPlace;
		this.fromReserve = fromReserve;
		this.side = side;
		this.sideIndex = sideIndex;
		this.flanking = flanking;
		flipped = flanking ^ shipToPlace.flipped;
	}
	
	// NB Need to use Airship.overlapsWith!!!
	
	public static boolean intersects(Airship shipToPlace, double x, double y, LandFormation lf) {
		if (!Rect2D.intersects(x, y, shipToPlace.getBBWidth(), shipToPlace.getBBHeight(), lf.getX(), lf.getY(), lf.getBBWidth(), lf.getBBHeight())) {
			return false;
		}
		double ox = shipToPlace.getX();
		double oy = shipToPlace.getY();
		shipToPlace.setX(x);
		shipToPlace.setY(y);
		boolean iSects = shipToPlace.overlapsWith(lf, /* ignoreSoftThings */ false);
		shipToPlace.setX(ox);
		shipToPlace.setY(oy);
		return iSects;
	}
	
	public static boolean intersects(Airship shipToPlace, double x, double y, Airship ship2) {
		if (!Rect2D.intersects(x, y, shipToPlace.getBBWidth(), shipToPlace.getBBHeight(), ship2.getX(), ship2.getY(), ship2.getBBWidth(), ship2.getBBHeight())) {
			return false;
		}
		double ox = shipToPlace.getX();
		double oy = shipToPlace.getY();
		shipToPlace.setX(x);
		shipToPlace.setY(y);
		boolean iSects = shipToPlace.overlapsWith(ship2);
		shipToPlace.setX(ox);
		shipToPlace.setY(oy);
		return iSects;
	}
	
	public static boolean canPlace(Airship shipToPlace, double x, double y, boolean flipped, Combat c, boolean ignoreServiceCeiling, LandFormation landscaped, int sideToRestrictTo) {
		if (landscaped == null) {
			y = getPlacement(shipToPlace, x, y, flipped, c.landFormations.get(0), c, ignoreServiceCeiling, sideToRestrictTo).y;
		}
		for (Side s : c.sides) {
			for (Airship ship : s.ships) {
				if (ship == shipToPlace) { continue; }
				if (intersects(shipToPlace, x, y, ship)) {
					return false;
				}
			}
		}
		for (LandFormation lf : c.landFormations) {
			if (lf != landscaped && intersects(shipToPlace, x, y, lf)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean canPlace(Airship shipToPlace, double x, double y, boolean flipped, int sideIndex, UniScreen us, boolean ignoreServiceCeiling, LandFormation landscaped, int landscapeSideToRestrictTo) {
		if (landscaped == null) {
			y = PlaceShipTool.getPlacement(shipToPlace, x, y, flipped, us, ignoreServiceCeiling).y;
		}
		if (us.intent instanceof RestrictsShipPlacement) {
			if (sideIndex == EITHER_SIDE) {
				boolean allowed = false;
				for (int si = 0; si < 2; si++) {
					Rect2D r = ((RestrictsShipPlacement) us.intent).placementLimits(si, us);
					if (r == null || r.contains(x, y, shipToPlace.getBBWidth(), shipToPlace.getBBHeight())) {
						allowed = true;
					}
				}
				if (!allowed) { return false; }
			} else {
				Rect2D r = ((RestrictsShipPlacement) us.intent).placementLimits(sideIndex, us);
				if (r != null && !r.contains(x, y, shipToPlace.getBBWidth(), shipToPlace.getBBHeight())) {
					return false;
				}
			}
		}
		Airship ignore = (us.intent instanceof SingleShipIntent) ? ((SingleShipIntent) us.intent).getIgnoredOriginalShip(us) : null;
		if (us.combat != null) {
			return canPlace(shipToPlace, x, y, flipped, us.combat, ignoreServiceCeiling, landscaped, landscapeSideToRestrictTo);
		} else if (us.city != null) {
			for (Airship bld : us.city.getDefences()) {
				if (bld == shipToPlace || bld == ignore) { continue; }
				if (intersects(shipToPlace, x, y, bld)) {
					return false;
				}
			}
			for (MapLocation.ConstructionEntry ce : us.city.constructing) {
				Airship c = ce.ship;
				if (c != null && c != shipToPlace && c != ignore) {
					if (intersects(shipToPlace, x, y, c)) {
						return false;
					}
				}
			}
			Fleet garrison = us.wm.getGarrison(us.city);
			if (garrison != null) {
				for (Airship ship : garrison.actives) {
					if (ship == shipToPlace || ship == ignore) { continue; }
					if (intersects(shipToPlace, x, y, ship)) {
						return false;
					}
				}
			}
			if (shipToPlace.canFly() && Rect2D.intersects(x, y, shipToPlace.getBBWidth(), shipToPlace.getBBHeight(), us.city.ground.getX(), us.city.ground.getY(), us.city.ground.getBBWidth(), us.city.ground.getBBHeight())) {
				return false;
			}
			for (LandFormation lf : us.city.floaters) {
				if (intersects(shipToPlace, x, y, lf)) {
					return false;
				}
			}
		} else if (us.intent instanceof MultiplayerSetupIntent) {
			for (Airship ship : ((MultiplayerSetupIntent) us.intent).getSideShips(sideIndex)) {
				if (ship == shipToPlace || ship == ignore) { continue; }
				if (intersects(shipToPlace, x, y, ship)) {
					return false;
				}
			}
			if (us.setupGround != null && landscaped != us.setupGround && shipToPlace.canFly() && intersects(shipToPlace, x, y, us.setupGround)) {
				return false;
			}
			if (us.setupFloaters != null) {
				for (LandFormation lf : us.setupFloaters) {
					if (intersects(shipToPlace, x, y, lf)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void placeShip(Placement pl, UniScreen us, Input in) { // qqDPS This is a mess re SP/MP
		double volume = StrictMath.max(0.1, StrictMath.min(1.0, shipToPlace.getWeight() / 5000.0));
		in.play("place", 1.0 / volume, volume * us.g.volume, 0, 0);
		
		if (us.intent instanceof MultiplayerCampaignCombatSetupIntent || us.intent instanceof MultiplayerCampaignCombatIntent || us.intent instanceof CombatIntent || us.intent instanceof MultiplayerCombatIntent) {
			us.combat.giveCommand(msg("placeShip")
					.put("side", us.combat.sides.indexOf(us.mySide))
					.put("ship", pl.shipToPlace.networkID)
					.put("x", pl.x)
					.put("y", pl.y)
					.put("flipped", pl.flipped)
					.put("flanking", flanking)
			);
			if (fromReserve) {
				us.panel(ReservePanel.class).visible = false;
			}
			return;
		}
		
		if (us.intent instanceof DefencesIntent) {
			CampaignWorld w = ((DefencesIntent) us.intent).ss.w;
			if (forConstruction) {
				if (cancelPutsBack) {
					JSONObject msg = w.shipMsg("placeConstructionShip", pl.shipToPlace)
						.put("city", us.city.id)
						.put("x", pl.x)
						.put("y", pl.y)
						.put("flipped", pl.flipped);
					if (pl.landscapeTarget != null) {
						msg.put("landscapeGY", pl.landscapeGY);
						msg.put("landscapeCost", pl.landscapeCost);
					}
					w.giveCommand(msg);
				} else {
					pl.shipToPlace.setX(pl.x);
					pl.shipToPlace.setY(pl.y);
					pl.shipToPlace.moveTo = new Pt(pl.x, pl.y);
					pl.shipToPlace.setFlipped(pl.flipped, null);
					pl.shipToPlace.flipTo = pl.flipped;
					pl.shipToPlace.lastPlaced = new Pt(pl.x, pl.y);
					pl.shipToPlace.lastPlacedFlipped = pl.flipped;
					pl.shipToPlace.version++;
					JSONObject msg = msg("buildShip").put("ship", Compression.compressToString(pl.shipToPlace.toJSON(null).toString())).put("city", us.city.id);
					if (pl.landscapeTarget != null) {
						msg.put("landscapeGY", pl.landscapeGY);
						msg.put("landscapeCost", pl.landscapeCost);
					}
					if (!w.giveCommandWithSizeCheck(msg)) {
						us.g.showError(_t("sent_construction_too_large"));
						return;
					}
				}
			} else {
				JSONObject msg = w.shipMsg("placeDefencesShip", pl.shipToPlace)
						.put("city", us.city.id)
						.put("x", pl.x)
						.put("y", pl.y)
						.put("flipped", pl.flipped);
				if (pl.landscapeTarget != null) {
					msg.put("landscapeGY", pl.landscapeGY);
					msg.put("landscapeCost", pl.landscapeCost);
				}
				w.giveCommand(msg);
			}
			return;
		}
		
		if (us.intent instanceof StrategicEditShipIntent) {
			StrategicEditShipIntent sesi = (StrategicEditShipIntent) us.intent;
			if (pl.shipToPlace != us.standaloneEditShip) {
				// We might move another ship while editing one.
				JSONObject msg = sesi.ss.w.shipMsg("placeDefencesShip", pl.shipToPlace)
						.put("city", us.city.id)
						.put("x", pl.x)
						.put("y", pl.y)
						.put("flipped", pl.flipped);
				if (pl.landscapeTarget != null) {
					msg.put("landscapeGY", pl.landscapeGY);
					msg.put("landscapeCost", pl.landscapeCost);
				}
				sesi.ss.w.giveCommand(msg);
				return;
			}
		}
		if (us.hasSetupFleet()) {
			if (us.setupGround == null || us.setupFloaters == null) { return; }
			/*pl.shipToPlace.initWheelsLegsAndTentacles(us.setupGround, us.setupFloaters, new ShipArrayList(us.getSetupFleet()));
			if (!us.setupFleet.contains(pl.shipToPlace)) {
				us.getSetupFleet().add(pl.shipToPlace);
			}*/
			PlayerInfo pi = ((MultiplayerSetupIntent) us.intent).placingInfo();
			if (pi == null || pi.isSpectator()) {
				return;
			}
			if (cancelPutsBack) {
				us.g.sendMessage(msg("moveSetupShip")
						.put("player", pi.id)
						.put("shipID", pl.shipToPlace.networkID)
						.put("x", pl.x)
						.put("y", pl.y)
						.put("flipped", pl.flipped)
				);
			} else {
				pl.shipToPlace.networkID = us.g.playerID() + "." + (((MultiplayerSetupIntent) us.intent).shipNetworkIDCounter++);
				pl.shipToPlace.chunkSubIDCounter = 1;
				us.g.sendMessage(msg("addNewSetupShip")
						.put("player", pi.id)
						.put("ship", Compression.compressToString(pl.shipToPlace.toJSON(null).toString()))
						.put("x", pl.x)
						.put("y", pl.y)
						.put("flipped", pl.flipped)
				);
			}
			return;
		}
		
		pl.shipToPlace.setX(pl.x);
		pl.shipToPlace.setY(pl.y);
		pl.shipToPlace.moveTo = new Pt(pl.x, pl.y);
		pl.shipToPlace.setFlipped(pl.flipped, null);
		pl.shipToPlace.flipTo = pl.flipped;
		pl.shipToPlace.lastPlaced = new Pt(pl.x, pl.y);
		pl.shipToPlace.lastPlacedFlipped = pl.flipped;
		pl.shipToPlace.version++;
		pl.shipToPlace.resetWeaponBarrels();
		if (side != null) {
			pl.shipToPlace.originalArms = side.arms;
		}
		
		if (us.intent instanceof EditMissionIntent) {
			((EditMissionIntent) us.intent).modified();
		}
		
		if (pl.landscapeTarget != null) {
			if (us.intent instanceof StrategicEditShipIntent) {
				StrategicEditShipIntent sesi = (StrategicEditShipIntent) us.intent;
				JSONObject msg = msg("doLandscapingForShip")
						.put("ship", Compression.compressToString(pl.shipToPlace.toJSON(null).toString()))
						.put("city", us.city.id)
						.put("x", pl.x)
						.put("y", pl.y)
						.put("flipped", pl.flipped)
						.put("landscapeGY", pl.landscapeGY)
						.put("landscapeCost", pl.landscapeCost);
				sesi.ss.w.giveCommandWithSizeCheck(msg);
			} else {
				if (us.intent instanceof HasStrategicScreen) {
					StrategicScreen ss = ((HasStrategicScreen) us.intent).getStrategicScreen();
					if (ss.w.isMultiplayer()) {
						AirshipGame.report("local landscape editing in MP, intent " + us.intent.getClass().getSimpleName());
						System.out.println("!!! local landscape editing in MP !!!");
						return;
					}
				}
				pl.landscapeTarget.doLandscapingForBuilding(pl.shipToPlace, pl.x, pl.landscapeGY, pl.flipped);
				if (us.city != null) {
					us.city.landscapeVersion++; // Moving things while in refit mode, for example.
				}
			}
		}
		
		if (us.combat != null) {
			pl.shipToPlace.initWheelsLegsAndTentacles(null, us.combat.landFormations, us.combat);
		}
		
		if (us.combat != null && (side != null || sideIndex == EITHER_SIDE)) {
			Side targetSide =
					sideIndex == EITHER_SIDE
					? us.combat.sides.get(pl.x < 0 ? 0 : 1)
					: side;
			Side sourceSide = us.combat.sideOf(shipToPlace);
			if (sourceSide != targetSide && sourceSide != null) {
				sourceSide.ships.remove(pl.shipToPlace);
			}
			if (sourceSide != null) {
				sourceSide.reserve.remove(pl.shipToPlace);
			}
			if (!targetSide.ships.contains(pl.shipToPlace)) {
				targetSide.ships.add(pl.shipToPlace);
			}
		/*} else if (us.setupFleet != null) {
			if (us.intent instanceof MultiplayerSetupIntent) {
				((MultiplayerSetupIntent) us.intent).landscapingCost += pl.landscapeCost;
			} else {
				throw new RuntimeException("setupfleet but no MPSI");
			}
			if (us.setupGround == null || us.setupFloaters == null) { return; }
			pl.shipToPlace.initWheelsLegsAndTentacles(us.setupGround, us.setupFloaters, new ShipArrayList(us.setupFleet));
			if (!us.setupFleet.contains(pl.shipToPlace)) {
				us.setupFleet.add(pl.shipToPlace);
			}*/ // MERGEME: assuming setupFleet is indeed toast then this is probably OK to leave out because of the hasSetupFleet code further up
			// it is toast!
		} else if (us.city != null && !(us.intent instanceof RefitFromDefencesIntent)) {
			System.out.println("placing in non defences intent city???");
		}
	}

	public static class Placement {
		public final Airship shipToPlace;
		public final double x, y;
		public final boolean flipped;
		public final LandFormation landscapeTarget;
		public final int landscapeGY;
		public final int landscapeCost;
		public final int landscapeSideToRestrictTo;
		public final boolean succeeded;

		public Placement(Airship shipToPlace, double x, double y, boolean flipped, LandFormation landscapeTarget, int landscapeGY, int landscapeCost, int landscapeSideToRestrictTo) {
			this.shipToPlace = shipToPlace;
			this.x = x;
			this.y = y;
			this.flipped = flipped;
			this.landscapeTarget = landscapeTarget;
			this.landscapeGY = landscapeGY;
			this.landscapeCost = landscapeCost;
			this.landscapeSideToRestrictTo = landscapeSideToRestrictTo;
			succeeded = true;
		}

		public Placement(Airship shipToPlace, double x, double y, boolean flipped) {
			this.shipToPlace = shipToPlace;
			this.x = x;
			this.y = y;
			this.flipped = flipped;
			landscapeTarget = null;
			landscapeGY = -1;
			landscapeCost = 0;
			landscapeSideToRestrictTo = -1;
			succeeded = true;
		}
		
		public Placement(Airship shipToPlace, double x, double y, boolean flipped, boolean succeeded) {
			this.shipToPlace = shipToPlace;
			this.x = x;
			this.y = y;
			this.flipped = flipped;
			this.succeeded = succeeded;
			landscapeTarget = null;
			landscapeGY = -1;
			landscapeCost = 0;
			landscapeSideToRestrictTo = -1;
		}
	}
	
	public static Placement getPlacement(Airship shipToPlace, double x, double y, boolean flipped, UniScreen us, boolean ignoreServiceCeiling) {
		int sideToRestrictTo = -1;
		if (us.combat != null) {
			sideToRestrictTo = us.combat.sides.indexOf(us.mySide);
			if (us.intent instanceof SingleCombatSetupIntent) {
				sideToRestrictTo = x > 0 ? 1 : 0;
			}
			return getPlacement(shipToPlace, x, y, flipped, us.combat.landFormations.get(0), us.combat, ignoreServiceCeiling, sideToRestrictTo);
		}
		if (us.city != null) {
			sideToRestrictTo = 1;
			ShipArrayList l = new ShipArrayList(us.city.getDefences());
			for (MapLocation.ConstructionEntry ce : us.city.constructing) {
				if (ce.ship != null) {
					l.add(ce.ship);
				}
			}
			l.remove(shipToPlace);
			return getPlacement(shipToPlace, x, y, flipped, us.city.ground, l, ignoreServiceCeiling, sideToRestrictTo);
		}
		if (us.setupGround != null) {
			if (us.intent instanceof MultiplayerSetupIntent) {
				PlayerInfo pi = ((MultiplayerSetupIntent) us.intent).placingInfo();
				sideToRestrictTo = pi != null ? pi.side : -1;
			}
			return getPlacement(shipToPlace, x, y, flipped, us.setupGround, new ShipArrayList(us.getSetupFleet()), ignoreServiceCeiling, sideToRestrictTo);
		}
		return getPlacement(shipToPlace, x, y, flipped, (LandFormation) null, null, ignoreServiceCeiling, sideToRestrictTo);
	}
	
	public static Placement getPlacement(Airship shipToPlace, double x, double y, boolean flipped, LandFormation ground, ShipList otherShips, boolean ignoreServiceCeiling, int sideToRestrictTo) {
		if (!shipToPlace.canFly()) {
			if (ground != null) {
				if (shipToPlace.type.mobile) {
					y = ground.getVerticalPosition(shipToPlace, (int) x, flipped, /* ignoreSoftThings*/ false) - shipToPlace.groundOffset();
				} else {
					Pair<Integer, Integer> gyAndCost = ground.getLandscapingPlacementGYandCost(shipToPlace, x, flipped, otherShips, sideToRestrictTo);
					if (gyAndCost == null) {
						y = AGame.GROUND_LEVEL - shipToPlace.getBBHeight() - shipToPlace.groundOffset();
						return new Placement(shipToPlace, x, y, flipped, false);
					}
					return new Placement(shipToPlace, x, ground.getY() + AGame.SGS * gyAndCost.a - shipToPlace.getBBHeight(), flipped, ground, gyAndCost.a, gyAndCost.b, sideToRestrictTo);
				}
			} else {
				y = AGame.GROUND_LEVEL - shipToPlace.getBBHeight() - shipToPlace.groundOffset();
			}
		}
		if (shipToPlace.canFly() && !ignoreServiceCeiling) {
			y = StrictMath.max(y, AGame.GROUND_LEVEL - shipToPlace.availableServiceCeiling(null));
		}
		return new Placement(shipToPlace, x, y, flipped);
	}
	
	@Override
	public void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {
		Rect2D limits = null;
		if (us.intent instanceof RestrictsShipPlacement) {
			limits = ((RestrictsShipPlacement) us.intent).placementLimits(sideIndex, us);
		}
		double x = us.screenToWorldX(cursor.x);
		Placement pl = getPlacement(shipToPlace, x, us.screenToWorldY(cursor.y), flipped ^ autoFlipped, us, ignoreServiceCeiling);
		double y = pl.y;
		if (fromReserve && (us.intent instanceof CampaignCombatIntent || us.intent instanceof MultiplayerCampaignCombatIntent)) {
			if (sideIndex == 0) {
				x = -us.combatAreaW() / 2;
			} else {
				x = us.combatAreaW() / 2 - shipToPlace.getBBWidth();
			}
		}
		if (flanking) {
			if (sideIndex == 0) {
				x = us.combatAreaW() / 2 - shipToPlace.getBBWidth();
			} else {
				x = -us.combatAreaW() / 2;
			}
		}
		boolean canPlace = pl.succeeded && canPlace(shipToPlace, x, y, flipped ^ autoFlipped, sideIndex, us, ignoreServiceCeiling, pl.landscapeTarget, pl.landscapeSideToRestrictTo);
		if (shipToPlace.type == ShipType.BUILDING && canPlace && pl.landscapeTarget != null) {
			landscapedGround = pl.landscapeTarget.faithfulClone();
			landscapedGround.doLandscapingForBuilding(shipToPlace, pl.x, pl.landscapeGY, pl.flipped);
		} else {
			landscapedGround = null;
		}
		d.rect(new Clr(255, 255, 255, 1), -10, -10, 1, 1); // qqDPS rect reset
		d.scale(us.zoom, us.zoom);
		d.shift(us.adjScrollX, us.adjScrollY);
		if (limits != null) {
			d.rect(Clr.WHITE, limits.x - 2, limits.y - 2, limits.w + 4, 2);
			d.rect(Clr.WHITE, limits.x - 2, limits.y + limits.h, limits.w + 4, 2);
			d.rect(Clr.WHITE, limits.x - 2, limits.y - 2, 2, limits.h + 4);
			d.rect(Clr.WHITE, limits.x + limits.w, limits.y - 2, 2, limits.h + 4);
		}
		shipToPlace.drawOutline(d, x, y, flipped ^ autoFlipped, canPlace ? Clr.WHITE : Clr.RED);
		if (pl.landscapeCost > 0) {
			d.state.cursorText = "[333333]$" + pl.landscapeCost;
			if (!us.textInputOccurring()) {
				d.state.cursorText += "\n" + _t("F_to_flip") + "\n" + _t("Press_x_to_cancel", "ESCAPE");
			}
		} else {
			if (!us.textInputOccurring()) {
				d.state.cursorText = "[333333]" + _t("F_to_flip") + "\n" + _t("Press_x_to_cancel", "ESCAPE");
			}
		}
		d.resetTransforms();
		double sx = shipToPlace.getX();
		double sy = shipToPlace.getY();
		boolean sf = shipToPlace.flipped;
		shipToPlace.setX(x);
		shipToPlace.setY(y);
		shipToPlace.setFlipped(flipped ^ autoFlipped, null);
		shipToPlace.drawFireArcs(d, us.adjScrollX, us.adjScrollY, us.zoom, true);
		shipToPlace.setX(sx);
		shipToPlace.setY(sy);
		shipToPlace.setFlipped(sf, null);
		if (shipToPlace.canFly() && !ignoreServiceCeiling) {
			int sc = (int) (us.worldToScreenY(AGame.GROUND_LEVEL - shipToPlace.serviceCeiling()));
			d.rect(Clr.WHITE, 0, sc, sm.width, 2);
			d.rect(Clr.WHITE, 0, sc + 4, sm.width, 1);
			d.text(_t("Service_Ceiling"), AGame.BIG_FOUNT, 5, sc - 20);
		}
	}

	@Override
	public boolean click(Input in, Pt click, ScreenMode sm, UniScreen us) {
		CommandButtonsPanel cbp = us.panel(CommandButtonsPanel.class);
		if (us.intent instanceof CombatIntent && click.x > cbp.panelX && click.x < cbp.panelX + cbp.panelW && click.y > sm.height - cbp.panelH) { return false; }
		double x = us.screenToWorldX(click.x);
		if (fromReserve && (us.intent instanceof CampaignCombatIntent || us.intent instanceof MultiplayerCampaignCombatIntent)) {
			if (sideIndex == 0) {
				x = -us.combatAreaW() / 2;
			} else {
				x = us.combatAreaW() / 2 - shipToPlace.getBBWidth();
			}
		}
		if (flanking) {
			if (sideIndex == 0) {
				x = us.combatAreaW() / 2 - shipToPlace.getBBWidth();
			} else {
				x = -us.combatAreaW() / 2;
			}
		}
		Airship multiPlaceShip = multiPlace ? shipToPlace.clone() : null;
		Placement pl = getPlacement(shipToPlace, x, us.screenToWorldY(click.y), flipped ^ autoFlipped, us, ignoreServiceCeiling);
		double y = pl.y;
		if (pl.succeeded && canPlace(shipToPlace, x, y, flipped ^ autoFlipped, sideIndex, us, ignoreServiceCeiling, pl.landscapeTarget, pl.landscapeSideToRestrictTo)) {
			placeShip(pl, us, in);
			if (multiPlace) {
				shipToPlace = multiPlaceShip;
				multiPlaceShip.repair(/* resetXP */ true); // Reset crew assignments to make sure the pathing is cached right.
			} else {
				us.tool = UniScreen.NAVIGATE;
			}
			return true;
		}
		return false;
	}

	public void cancel(UniScreen us, Input in) {
		if (cancelPutsBack) {
			placeShip(new Placement(shipToPlace, shipToPlace.getX(), shipToPlace.getY(), shipToPlace.flipped), us, in);
		}
		us.tool = UniScreen.NAVIGATE;
	}
	
	@Override
	public boolean rightClick(Input in, Pt click, ScreenMode sm, UniScreen us) {
		cancel(us, in);
		return true;
	}
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (!us.textInputOccurring() && Keys.check(in, "place_tool_flip", "F", false)) {
			flipped = !flipped;
		}
		autoFlipped = sideIndex == EITHER_SIDE && us.screenToWorldX(in.cursor().x) > 0;
		if (!autoFlipInited) { // If we start out on the right side, compensate for autoflip.
			if (autoFlipped) { flipped = !flipped; }
			autoFlipInited = true;
		}
		if (Keys.check(in, "ESCAPE")) {
			if (cancelPutsBack) {
				placeShip(new Placement(shipToPlace, shipToPlace.getX(), shipToPlace.getY(), shipToPlace.flipped), us, in);
			}
			us.tool = UniScreen.NAVIGATE;
		}
	}
}
