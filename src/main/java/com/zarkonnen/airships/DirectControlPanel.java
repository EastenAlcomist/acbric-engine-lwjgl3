package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import static com.zarkonnen.airships.CommandButtonsPanel.cmdTooltip;
import static com.zarkonnen.airships.CommandButtonsPanel.doAbility;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.Loop;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.json.JSONArray;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

public strictfp class DirectControlPanel implements UniScreen.InfoPanel {
	public final Clr BRIGHT = Clr.fromHex("f2e59c");
	public final Color BRIGHT_C = new Color(BRIGHT.r, BRIGHT.g, BRIGHT.b);
	public final Clr GOLD = Clr.fromHex("c5b86f");
	public final Color GOLD_C = new Color(GOLD.r, GOLD.g, GOLD.b);
	public final Clr ICONS = Clr.fromHex("483f33");
	public final Color ICONS_C = new Color(ICONS.r, ICONS.g, ICONS.b);
	public final Color ICONS_C_LIGHT = new Color(78, 69, 57);
	private final Img DIRECT_CONTROL = new Img("ui", 32, 384, 16, 16, false);
	private final Img UP = new Img("ui", 208, 512, 16, 16, false);
	private final Img DOWN = new Img("ui", 192, 512, 16, 16, false);
	private final Img FLIP = new Img("ui", 304, 512, 16, 16, false);
	
	private final Img FIREFIGHT = new Img("ui", 2 * 16, 26 * 16, 16, 16, false);
	private final Img REPAIR = new Img("ui", 3 * 16, 26 * 16, 16, 16, false);
	private final Img MOVEFOCUS = new Img("ui", 14 * 16, 26 * 16, 16, 16, false);
	private final Img SHOOT = new Img("ui", 4 * 16, 26 * 16, 16, 16, false);
	private final Img HOLD_F = new Img("ui", 96, 512, 16, 16, false);
	private final Img AIMED_F = new Img("ui", 5 * 16, 26 * 16, 16, 16, false);
	private final Img NORMAL_F = new Img("ui", 6 * 16, 26 * 16, 16, 16, false);
	private final Img RAPID_F = new Img("ui", 7 * 16, 26 * 16, 16, 16, false);
	private final Img BOARD = new Img("ui", 15 * 16, 26 * 16, 16, 16, false);
	private final Img TETHER = new Img("ui", 28 * 16, 26 * 16, 16, 16, false);
	private final Img CUT_TETHER = new Img("ui", 29 * 16, 26 * 16, 16, 16, false);
	
	private final Img ONE_USE_WEAPONS = new Img("ui", 384, 448, 16, 16, false);
	private final Img ONE_USE_PROPULSION = new Img("ui", 384 + 16, 448, 16, 16, false);
	private final Img ONE_USE_LIFT = new Img("ui", 384 + 32, 448, 16, 16, false);
	
	ShipSpeed targetSpeed;
	int newTargetAltitude;
	int targetAltitude;
	boolean targetFlip;
	Airship prevShip;
	int msSinceAltitudeAdjustment;
	double zoom = 0.5;
	int relinquishControlCooldown = 0;
	double altLoopAmount = 0;
	int lastMs = 0;
	String nextTooltip, commandTooltip;
	HashSet<ModuleType> disabledWeapons = new HashSet<ModuleType>();
	double cameraMix = 0;
	int oldScrollX, oldScrollY;
	double oldZoom;
	Pt targetSoundPt;
	
	private boolean canTether(Airship s) {
		int msz = s.modules.size();
		for (int i = 0; i < msz; i++) {
			if (s.modules.get(i).type.getTetherSpec(s.currentBonuses) != null) {
				return true;
			}
		}
		return false;
	}
	
	private boolean canCutTether(Airship s) {
		int msz = s.modules.size();
		for (int i = 0; i < msz; i++) {
			if (s.modules.get(i).tether != null) {
				return true;
			}
		}
		return false;
	}
	
	public static Airship getShip(UniScreen us) {
		if (!(us.intent instanceof CombatIntent)) { return null; }
		CombatIntent ci = (CombatIntent) us.intent;
		if (ci.spectate()) { return null; }
		for (Combat.Side side : us.combat.sides) {
			for (Airship ship : side.ships) {
				if (ci.isShipPlayerControlled(us, ship) && ((us.g.playerID() == 0 && ship.getDirectControlID() == 1) || ship.getDirectControlID() == us.g.playerID())) {
					return ship;
				}
			}
		}
		return null;
	}

	@Override
	public void draw(MyDraw d, Pt cursor, ScreenMode sm, Hooks hs, final UniScreen us) {
		if (!(us.intent instanceof CombatIntent) || us.intent instanceof PlaybackIntent) { return; }
		if (us.intent instanceof CombatIntent && ((CombatIntent) us.intent).spectate()) { return; }
		if (us.combat.startCountdown > 0) { return; }
		if (us.hideUI) { return; }
		final Airship ship = getShip(us);
		if (ship == null) { return; }
		final Combat combat = us.combat;
		
		if (us.tool == UniScreen.NAVIGATE) {
			d.hook(0, 0, sm.width, sm.height, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_2_CLICKED) {
				@Override
				public void run(Input input, Pt clicked, Hook.Type type) {
					double clickX = us.screenToWorldX(clicked.x);
					double clickY = us.screenToWorldY(clicked.y);
					int crewTargetIndex = -1;
					Crewman crewTarget = null;

					for (Crewman enemy : us.combat.otherSide(us.mySide).troops) {
						crewTargetIndex++;
						if (enemy.hp > 0 && clickX >= enemy.getX() && clickY >= enemy.getY() && clickX <= enemy.getX() + enemy.getBBWidth() && clickY <= enemy.getY() + enemy.getBBHeight()) {
							crewTarget = enemy;
							break;
						}
					}

					if (crewTarget != null) {
						JSONArray moduleList = new JSONArray();
						for (int i = 0; i < ship.modules.size(); i++) {
							Module m = ship.modules.get(i);
							if (!m.type.isWeapon() || m.hp <= 0) { continue; }
							int str = m.type.getShootTroopsRange(ship.currentBonuses);
							if (str <= 0) { continue; }
							if (disabledWeapons.contains(m.type.getVariantGroupHeadOrThis().getSymmetryGroupHead())) { continue; }
							Pt mz = m.fireFrom();
							if (m.canHit(mz.x, mz.y, (int) clickX, (int) clickY)) {// && ((mz.x - clickX) * (mz.x - clickX) + (mz.y - clickY) * (mz.y - clickY)) < str * str) {
								moduleList.put(i);
							}
						}
						if (moduleList.length() > 0) {
							targetSoundPt = clicked;
							combat.giveCommand(msg("fireOrder")
									.put("id", combat.getShipID(ship))
									.put("crewTarget", crewTargetIndex)
									.put("modules", moduleList));
						}
					} else {
						Airship target = null;
						Tile tt = null;
						for (Airship enemy : us.combat.otherSide(us.mySide).ships) {
							int gx = enemy.gridXToWorldX((int) ((clickX - enemy.getX()) / AGame.SGS), 1);
							int gy = (int) ((clickY - enemy.getY()) / AGame.SGS);
							tt = enemy.tileAt(gx, gy);
							if (tt != null) {
								target = enemy;
								break;
							}
						}
						if (target != null) {
							JSONArray moduleList = new JSONArray();
							for (int i = 0; i < ship.modules.size(); i++) {
								Module m = ship.modules.get(i);
								if (!m.type.isWeapon() || m.hp <= 0) { continue; }
								if (disabledWeapons.contains(m.type.getVariantGroupHeadOrThis().getSymmetryGroupHead())) { continue; }
								Pt mz = m.fireFrom();
								if (m.canHit(combat, target, tt, mz.x, mz.y)) {
									moduleList.put(i);
								}
							}
							if (moduleList.length() > 0) {
								targetSoundPt = clicked;
								combat.giveCommand(msg("fireOrder")
										.put("id", combat.getShipID(ship))
										//.put("mode", type == Type.MOUSE_2_CLICKED ? Module.DirectControlFireMode.ONCE.name() : Module.DirectControlFireMode.REPEAT.name())
										.put("mode", Module.DirectControlFireMode.REPEAT.name())
										.put("target", combat.getShipID(target))
										.put("modules", moduleList)
										.put("gx", tt.x).put("gy", tt.y));
							}
						}
					}
				}
			});

			d.state.setCursor("EMPTY_TARGET", null);
			if (cursor != null) {
				double cx = us.screenToWorldX(cursor.x);
				double cy = us.screenToWorldY(cursor.y);
				for (int i = 0; i < ship.modules.size(); i++) {
					Module m = ship.modules.get(i);
					if (!m.type.isWeapon() || m.hp <= 0) { continue; }
					if (disabledWeapons.contains(m.type.getVariantGroupHeadOrThis().getSymmetryGroupHead())) { continue; }
					Pt mz = m.fireFrom();
					if (m.canHit(mz.x, mz.y, (int) cx, (int) cy)) {
						ship.drawFireArc(m, d, us.adjScrollX, us.adjScrollY, us.zoom, true);
						if (m.somewhatStaffed() && m.type.guidanceSystem(ship.currentBonuses) == null && us.combat.speed != CombatSpeed.STOP) {
							m.weaponAngle = Direction.radiansFromTo(mz.x, mz.y, cx, cy);
						}
						d.state.setCursor("FULL_TARGET", null);
					}
				}
			}
		}
		
		if (!ship.type.onGround) {
			int sc = (int) (us.worldToScreenY(newTargetAltitude));
			d.rect(Clr.WHITE, 0, sc, 20, 2);
			d.rect(Clr.WHITE, 0, sc + 4, 10, 1);
			d.rect(Clr.WHITE, sm.width - 20, sc, 20, 2);
			d.rect(Clr.WHITE, sm.width - 10, sc + 4, 10, 1);
			d.text(_t("Target_Altitude"), AGame.BIG_FOUNT, 5, sc - AGame.BIG_FOUNT.lineHeight);
		}
		
		// Control panel
		final int teleW;
		switch (AirshipGame.instance.currentGUIScale) {
			case SMALL:
				teleW = 120;
				break;
			case MEDIUM:
				teleW = 180;
				break;
			case LARGE:
				teleW = 240;
				break;
			default:
				teleW = 120;
				break;
		}
		int teleBorder = teleW / 40;
		int leverW = teleW / 12;
		int leverL = teleW;
		int altW = (int) d.textSize("999m", AGame.FOUNT).x;
		int numCommandButtons = 11;
		int buttonsW = (MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING) * numCommandButtons + MyDraw.ICON_BUTTON_SZ * 3 + MyDraw.BUTTON_SPACING * 2 + altW + MyDraw.UI_SPACING * 3
				+ MyDraw.UI_SPACING * 2 - MyDraw.BUTTON_SPACING * 2;// Adjust for button grouping
		int w = buttonsW + teleW / 2 + MyDraw.WINDOW_INSET;
		int h = Math.max(teleW / 2, MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING * 2 + AGame.FOUNT.lineHeight * 2 + MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight) + MyDraw.SIDE_CLEARANCE;
		int x = sm.width / 2 - w / 2;
		int y = sm.height - h;
		
		// Weapon group window
		ArrayList<WeaponGroup> gs = getWeaponGroups(ship);
		int wgRows = (h - MyDraw.SIDE_CLEARANCE) / AGame.FOUNT.lineHeight;
		int wgColW = 0;
		int wgRow = 0;
		int wgTotalW = 0;
		for (int i = 0; i < gs.size(); i++) {
			if (wgRow == wgRows) {
				wgTotalW += wgColW + MyDraw.UI_SPACING;
				wgColW = 0;
				wgRow = 0;
			}
			String txt = (i + 1) + ": " + gs.get(i).type.getName();
			if (gs.get(i).quantity > 1) {
				txt += " x" + gs.get(i).quantity;
			}
			wgColW = Math.max(wgColW, (int) d.textSize(txt, AGame.FOUNT).x);
			wgRow++;
		}
		if (wgRow > 0) {
			wgTotalW += wgColW + MyDraw.UI_SPACING;
		}
		int wgX = x - wgTotalW - MyDraw.WINDOW_INSET;
		if (wgX < MyDraw.UI_SPACING) {
			x += (MyDraw.UI_SPACING - wgX);
			wgX = MyDraw.UI_SPACING;
		}
		d.drawPanel(wgX, y, wgTotalW + MyDraw.WINDOW_INSET * 2, h + MyDraw.WINDOW_INSET * 2, 11);
		d.tooltip(wgX, y, wgTotalW + MyDraw.WINDOW_INSET * 2, h + MyDraw.WINDOW_INSET * 2, _t("weapon_groups_tooltip"));
		wgX += MyDraw.WINDOW_INSET;
		wgColW = 0;
		wgRow = 0;
		for (int i = 0; i < gs.size(); i++) {
			final ModuleType t = gs.get(i).type;
			if (wgRow == wgRows) {
				wgX += wgColW + MyDraw.UI_SPACING;
				wgColW = 0;
				wgRow = 0;
			}
			String txt = (i + 1) + ": " + gs.get(i).type.getName();
			if (gs.get(i).quantity > 1) {
				txt += " x" + gs.get(i).quantity;
			}
			if (disabledWeapons.contains(gs.get(i).type)) {
				txt = "[888888]" + txt;
			}
			int tw = (int) d.textSize(txt, AGame.FOUNT).x;
			d.hook(wgX, y + MyDraw.WINDOW_INSET + wgRow * AGame.FOUNT.lineHeight, tw, AGame.FOUNT.lineHeight, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input input, Pt pt, Hook.Type type) {
					if (disabledWeapons.contains(t)) {
						disabledWeapons.remove(t);
					} else {
						disabledWeapons.add(t);
					}
				}
			});
			wgColW = Math.max(wgColW, tw);
			d.text(txt, AGame.FOUNT, wgX, y + MyDraw.WINDOW_INSET + wgRow * AGame.FOUNT.lineHeight);
			wgRow++;
		}
		
		// Combat Abilities
		if (ship.getCaptain() != null && !ship.getCaptain().type.combatAbilities.isEmpty()) {
			HeroType capt = ship.getCaptain().type;
			d.drawPanel(x, y - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_SPACING - AGame.FOUNT.lineHeight - MyDraw.ICON_BUTTON_SZ, capt.combatAbilities.size() * (MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING) - MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 2, MyDraw.PANEL_INSET * 2 + MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING + AGame.FOUNT.lineHeight, 23);
			int abX = x + MyDraw.PANEL_INSET;
			int abY = y - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ - AGame.FOUNT.lineHeight;
			for (final HeroType.CombatAbility ab : capt.combatAbilities) {
				d.iconButton(abX, abY, ab.icon, new Runnable() {
					@Override
					public void run() {
						doAbility(us, us.selectedShip, ab);
					}
				}, CommandButtonsPanel.canDoAbility(us, ship, ab));
				d.tooltip(abX, abY, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, ab.getName() + "\n\n" + ab.getDesc(capt));
				if (Keys.get(ab.name(), ab.shortcut) != null) {
					shortcutHint(d, abX, abY, Keys.getText(ab.name(), ab.shortcut, true));
				}
				abX += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			}
		}
		
		int extraPanelX = x + w + teleW / 2 + MyDraw.UI_SPACING;
		
		if (ship.canGiveAircraftCommands()) {
			AircraftBehaviourMode[] planeModes = AircraftBehaviourMode.values();
			int panelW = MyDraw.ICON_BUTTON_SZ * planeModes.length + MyDraw.BUTTON_SPACING * (planeModes.length - 1) + MyDraw.PANEL_INSET * 2;
			d.drawPanel(extraPanelX, y, panelW, MyDraw.ICON_BUTTON_SZ + AGame.FOUNT.lineHeight + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 2, 19);
			int xx = extraPanelX + MyDraw.PANEL_INSET;
			int yy = y + MyDraw.PANEL_INSET;
			for (int i = 0; i < planeModes.length; i++) {
				final AircraftBehaviourMode mode = planeModes[i];
				d.iconToggle(xx, yy, mode.img, new Runnable() {
					@Override
					public void run() {
						planeMode(combat, ship, mode);
					}
				}, ship.aircraftMode == mode, true);
				tooltipHook(d, xx, yy, "aircraft_" + mode.name(), Keys.getText(mode.name(), mode.defaultShortcut, false));
				shortcutHint(d, xx, yy, Keys.getText(mode.name(), mode.defaultShortcut, false));
				xx += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			//extraPanelX += panelW + MyDraw.UI_SPACING;
		}
		
		boolean oneUseWeapons = !ship.releaseOneUseWeapons && ship.hasOneUseWeapons();
		boolean oneUseLift = !ship.releaseOneUseLift && ship.hasOneUseLift();
		boolean oneUsePropulsion = !ship.releaseOneUsePropulsion && ship.hasOneUsePropulsion();
		if (oneUseWeapons || oneUseLift || oneUsePropulsion) {
			int buttons = (oneUseWeapons ? 1 : 0) + (oneUseLift ? 1 : 0) + (oneUsePropulsion ? 1 : 0);
			int yy = y + MyDraw.ICON_BUTTON_SZ + AGame.FOUNT.lineHeight + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING ;
			int pW = MyDraw.ICON_BUTTON_SZ * buttons + MyDraw.BUTTON_SPACING * (buttons - 1) + MyDraw.PANEL_INSET * 2;
			d.drawPanel(extraPanelX, yy, pW, MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING + AGame.FOUNT.lineHeight, 4);
			int xx = extraPanelX + MyDraw.PANEL_INSET;
			yy += MyDraw.PANEL_INSET;
			if (oneUseWeapons) {
				d.iconButton(xx, yy, ONE_USE_WEAPONS, new Runnable() {
					@Override
					public void run() {
						oneUseWeapons(combat, ship);
					}
				}, true);
				tooltipHook(d, xx, yy, "releaseOneUseWeapons", Keys.getText("releaseOneUseWeapons", "COMMA", false));
				shortcutHint(d, xx, yy, Keys.getText("releaseOneUseWeapons", "COMMA", false));
				xx += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			
			if (oneUseLift) {
				d.iconButton(xx, yy, ONE_USE_LIFT, new Runnable() {
					@Override
					public void run() {
						oneUseLift(combat, ship);
					}
				}, true);
				tooltipHook(d, xx, yy, "releaseOneUseLift", Keys.getText("releaseOneUseLift", "PERIOD", true));
				shortcutHint(d, xx, yy, Keys.getText("releaseOneUseLift", "PERIOD", true));
				xx += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			
			if (oneUsePropulsion) {
				d.iconButton(xx, yy, ONE_USE_PROPULSION, new Runnable() {
					@Override
					public void run() {
						oneUsePropulsion(combat, ship);
					}
				}, true);
				tooltipHook(d, xx, yy, "releaseOneUsePropulsion", Keys.getText("releaseOneUsePropulsion", "SLASH", true));
				shortcutHint(d, xx, yy, Keys.getText("releaseOneUsePropulsion", "SLASH", true));
				xx += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
		}
		
		d.drawWindow(x, y, w, h + MyDraw.WINDOW_INSET * 2, 7);
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.WINDOW_INSET;
		w -= MyDraw.WINDOW_INSET * 2;
		
		if (us.tool != UniScreen.NAVIGATE) {
			d.text(us.tool.getLabel(), AGame.BIG_FOUNT, x, y);
			d.button(x, y + AGame.BIG_FOUNT.height, d.bw(_t("Cancel"), "ESCAPE"), _t("Cancel"), "ESCAPE", new Runnable() {
				@Override
				public void run() {
					us.tool = UniScreen.NAVIGATE;
				}
			});
			x += buttonsW;
		} else {
			if (commandTooltip != null) {
				d.text(commandTooltip, AGame.BIG_FOUNT, x, y + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING);
			}

			CommandButtonsPanel.renderShipQuantities(d, ship, x, y + AGame.BIG_FOUNT.lineHeight - AGame.FOUNT.lineHeight, buttonsW);

			d.iconButton(x, y, DIRECT_CONTROL, new Runnable() {
				@Override
				public void run() {
					relinquishControl(ship, combat);
				}
			}, true);
			tooltipHook(d, x, y, "Relinquish_Direct_Control", Keys.getText("direct_control", "C", false));
			shortcutHint(d, x, y, Keys.getText("direct_control", "C", false));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;

			d.iconButton(x, y, BOARD, new Runnable() {
				@Override
				public void run() {
					us.selectedShip = ship;
					us.selectedShips.clear();
					us.tool = new BoardCommandTool(false);
				}
			}, BoardCommandTool.canBoard(ship, us.mySide));
			tooltipHook(d, x, y, "Board_ship_", Keys.getText("board_ship", "B", false));
			shortcutHint(d, x, y, Keys.getText("board_ship", "B", false));
			x += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			d.iconButton(x, y, TETHER, new Runnable() {
				@Override
				public void run() {
					us.selectedShip = ship;
					us.selectedShips.clear();
					us.tool = new TetherCommandTool();
				}
			}, canTether(ship));
			tooltipHook(d, x, y, "Launch_tether_at_", Keys.getText("tether_ship", "J", false));
			shortcutHint(d, x, y, Keys.getText("tether_ship", "J", false));
			x += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			d.iconButton(x, y, CUT_TETHER, new Runnable() {
				@Override
				public void run() {
					cutTether(us.combat, ship);
				}
			}, canCutTether(ship));
			tooltipHook(d, x, y, "Cut_tethers", Keys.getText("cut_tethers", "K", false));
			shortcutHint(d, x, y, Keys.getText("cut_tethers", "K", false));
			x += MyDraw.BUTTON_H + MyDraw.UI_SPACING;

			d.iconToggle(x, y, FIREFIGHT, new Runnable() {
				@Override
				public void run() {
					focusOnFirefighting(combat, ship);
				}
			}, ship.focusOnFirefighting, ship.canFirefight());
			tooltipHook(d, x, y, "Focus_on_firefighting", Keys.getText("focus_on_firefighting", "F", false));
			shortcutHint(d, x, y, Keys.getText("focus_on_firefighting", "F", false));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;

			d.iconToggle(x, y, REPAIR, new Runnable() {
				@Override
				public void run() {
					focusOnRepair(combat, ship);
				}
			}, ship.focusOnRepair, ship.canRepair());
			tooltipHook(d, x, y, "Focus_on_repair", Keys.getText("focus_on_repair", "E", false));
			shortcutHint(d, x, y, Keys.getText("focus_on_repair", "E", false));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;

			d.iconToggle(x, y, MOVEFOCUS, new Runnable() {
				@Override
				public void run() {
					focusOnMoving(us.combat, ship);
				}
			}, ship.focusOnMoving, ship.canMove());
			tooltipHook(d, x, y, "Focus_on_moving", Keys.getText("focus_on_moving", "V", false));
			shortcutHint(d, x, y, Keys.getText("focus_on_moving", "V", false));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;

			d.iconToggle(x, y, SHOOT, new Runnable() {
				@Override
				public void run() {
					focusOnShooting(us.combat, ship);
				}
			}, ship.focusOnShooting, ship.canShoot());
			tooltipHook(d, x, y, "Focus_on_shooting", Keys.getText("focus_on_shooting", "O", false));
			shortcutHint(d, x, y, Keys.getText("focus_on_shooting", "O", false));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;

			d.iconToggle(x, y, HOLD_F, new Runnable() {
				@Override
				public void run() {
					fireMode(us.combat, ship, FireMode.HOLD);
				}
			}, ship.fireMode == FireMode.HOLD, ship.canShoot());
			tooltipHook(d, x, y, "Hold_fire", Keys.getText("hold_fire", "H", false));
			shortcutHint(d, x, y, Keys.getText("hold_fire", "H", false));
			x += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			d.iconToggle(x, y, AIMED_F, new Runnable() {
				@Override
				public void run() {
					fireMode(us.combat, ship, FireMode.AIMED);
				}
			}, ship.fireMode == FireMode.AIMED, ship.canShoot());
			tooltipHook(d, x, y, "Aimed_fire", Keys.getText("aimed_fire", "I", false));
			shortcutHint(d, x, y, Keys.getText("aimed_fire", "I", false));
			x += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			d.iconToggle(x, y, NORMAL_F, new Runnable() {
				@Override
				public void run() {
					fireMode(us.combat, ship, FireMode.NORMAL);
				}
			}, ship.fireMode == FireMode.NORMAL, ship.canShoot());
			tooltipHook(d, x, y, "Normal_fire", Keys.getText("normal_fire", "N", false));
			shortcutHint(d, x, y, Keys.getText("normal_fire", "N", false));
			x += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			d.iconToggle(x, y, RAPID_F, new Runnable() {
				@Override
				public void run() {
					fireMode(us.combat, ship, FireMode.RAPID);
				}
			}, ship.fireMode == FireMode.RAPID, ship.canShoot());
			tooltipHook(d, x, y, "Rapid_fire", Keys.getText("rapid_fire", "P", false));
			shortcutHint(d, x, y, Keys.getText("rapid_fire", "P", false));

			x += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;

			if (!ship.type.onGround) {
				d.text(((-targetAltitude + AGame.GROUND_LEVEL) / AGame.PX_TO_M) + "m", AGame.FOUNT, x, y);
				x += altW + MyDraw.UI_SPACING;
				d.iconButton(x, y, UP, new Runnable() {
					@Override
					public void run() {
						// Do actually nothing
					}
				}, true);
				d.hook(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						newTargetAltitude -= lastMs / 4;
						altLoopAmount += lastMs * 0.01;
					}
				});
				tooltipHook(d, x, y, "Increase_Target_Altitude", "W");
				shortcutHint(d, x, y, "W");
				x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				d.iconButton(x, y, DOWN, new Runnable() {
					@Override
					public void run() {
						// Do actually nothing
					}
				}, true);
				d.hook(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						newTargetAltitude += lastMs / 4;
						altLoopAmount += lastMs * 0.01;
					}
				});
				tooltipHook(d, x, y, "Decrease_Target_Altitude", "W");
				shortcutHint(d, x, y, "S");
				x += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
			}
		}
		if (ship.type.mobile) {
			x = sm.width / 2 - w / 2 + buttonsW;
			y -= teleW / 2 + MyDraw.WINDOW_INSET;
			Graphics g = (Graphics) d.frame().nativeRenderer();
			final float segDegrees = 44;
			if (targetSpeed != null) {
				float leverAngle = (-90 - ShipSpeed.values().length * segDegrees / 2 + targetSpeed.ordinal() * segDegrees + segDegrees / 2);
				g.translate(x + teleW / 2, y + teleW / 2);
				g.rotate(0, 0, leverAngle);
				g.setColor(GOLD_C);
				g.fillRect(0, -leverW / 2, leverL, leverW);
				g.setColor(BRIGHT_C);
				g.fillRect(0, -leverW / 2 + 2, leverL - 2, leverW - 4);
				g.setColor(ICONS_C);
				g.fillRoundRect(leverL * 3 / 4, -leverW, leverL / 4, leverW * 2, leverW / 4);
				g.setColor(ICONS_C_LIGHT);
				g.fillRoundRect(leverL * 3 / 4 + 2, -leverW + 2, leverL / 4 - 4, leverW * 2 - 4, leverW / 4);
				g.resetTransform();
				int tw = (int) d.textSize("A D", AGame.FOUNT).x + MyDraw.PANEL_BORDER_W * 2 + 4;
				int th = AGame.FOUNT.lineHeight + MyDraw.PANEL_BORDER_W;
				g.setColor(GOLD_C);
				//g.fillRect(x + teleW / 2 - tw / 2, y - th, tw, th + teleW / 3);
				g.fillRoundRect(x + teleW / 2 - tw / 2, y - th, tw, th + teleW / 3, MyDraw.PANEL_BORDER_W);
				g.setColor(BRIGHT_C);
				//g.fillRect(x + teleW / 2 - tw / 2 + 2, y - th + 2, tw - 4, th + teleW / 3);
				g.fillRoundRect(x + teleW / 2 - tw / 2 + 2, y - th + 2, tw - 4, th + teleW / 3, MyDraw.PANEL_BORDER_W);
				if (!Main.ON_STEAM_DECK) {
					d.text("[483f33]A", AGame.FOUNT, x + teleW / 2 - tw / 2 + MyDraw.PANEL_BORDER_W + 2, y - th + MyDraw.PANEL_BORDER_W);
					d.text("[483f33]D", AGame.FOUNT, x + teleW / 2 + tw / 2 - MyDraw.PANEL_BORDER_W - 2 - (int) d.textSize("D", AGame.FOUNT).x, y - th + MyDraw.PANEL_BORDER_W);
				}
				//d.text(MyDraw.SELECTED_C + "AD", AGame.FOUNT, x + teleW / 2 - (int) d.textSize("AD", AGame.FOUNT).x / 2 + (int) (Math.cos(Math.PI * leverAngle / 180) * (leverL - AGame.FOUNT.lineHeight)), y + teleW / 2 - AGame.FOUNT.lineHeight / 2 + (int) (Math.sin(Math.PI * leverAngle / 180) * (leverL  - AGame.FOUNT.lineHeight)));
				d.tooltip(x + teleW / 2 - tw / 2, y - th, tw, th, _t("direct_control_speed_tooltip"));
			}
			g.setColor(GOLD_C);
			g.fillOval(x - 1, y - 1, teleW + 2, teleW + 2);
			g.setColor(BRIGHT_C);
			g.fillOval(x, y, teleW, teleW);
			for (ShipSpeed speed : ShipSpeed.values()) {
				g.setColor(GOLD_C);
				float segStart = -90 - ShipSpeed.values().length * segDegrees / 2 + speed.ordinal() * segDegrees;
				double segMiddle = (segStart + segDegrees / 2) * Math.PI / 180;
				// Are we in the right place with the cursor?
				if (cursor != null) {
					double cursorAngle = Math.atan2(cursor.y - (y + teleW / 2), cursor.x - (x + teleW / 2));
					double cursorDsq = (cursor.y - (y + teleW / 2)) * (cursor.y - (y + teleW / 2)) + (cursor.x - (x + teleW / 2)) * (cursor.x - (x + teleW / 2));
					if (Direction.distance(Direction.normalizeRadians(cursorAngle), Direction.normalizeRadians(segMiddle)) < segDegrees * Math.PI / 180 / 2 && cursorDsq < teleW * teleW / 4) {
						g.setColor(Color.white);
					}
				}
				double xOffset = Math.cos((segStart + segDegrees / 2) * Math.PI / 180) * teleBorder;
				double yOffset = Math.sin((segStart + segDegrees / 2) * Math.PI / 180) * teleBorder;
				g.fillArc(x + teleBorder * 2 + (float) xOffset, y + teleBorder * 2 + (float) yOffset, teleW - teleBorder * 4, teleW - teleBorder * 4, segStart, segStart + segDegrees);
				d.blit(speed.icon, ICONS,
						x + teleW / 2 + (int) (Math.cos(segMiddle) * teleW / 3) - 8,
						y + teleW / 2 + (int) (Math.sin(segMiddle) * teleW / 3) - 8
				);
			}
			d.hook(x, y, teleW, teleW, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input input, Pt click, Hook.Type type) {
					for (ShipSpeed speed : ShipSpeed.values()) {
						float segStart = -90 - ShipSpeed.values().length * segDegrees / 2 + speed.ordinal() * segDegrees;
						double segMiddle = (segStart + segDegrees / 2) * Math.PI / 180;
						double cursorAngle = Math.atan2(click.y - (teleW / 2), click.x - (teleW / 2));
						double cursorDsq = (click.y - (teleW / 2)) * (click.y - (teleW / 2)) + (click.x - (teleW / 2)) * (click.x - (teleW / 2));
						if (Direction.distance(Direction.normalizeRadians(cursorAngle), Direction.normalizeRadians(segMiddle)) < segDegrees * Math.PI / 180 / 2 && cursorDsq < teleW * teleW / 4) {
							setSpeed(input, us, ship, speed);
						}
					}
				}
			});
			x += teleW / 2 - MyDraw.ICON_BUTTON_SZ / 2;
			y += teleW * 2 / 3;
			d.iconButton(x, y, FLIP, new Runnable() {
				@Override
				public void run() {
					flip(ship, combat);
				}
			}, true, ship.flipTo != ship.flipped);
			tooltipHook(d, x, y, "Flip", Keys.getText("direct_flip", "Q", false));
			shortcutHint2(d, x, y, Keys.getText("direct_flip", "Q", false));
		}
	}
	
	private void tooltipHook(MyDraw d, int x, int y, final String key, final String shortcut) {
		d.hook(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.HOVER) {
			@Override
			public void run(Input in, Pt p, Hook.Type type) {
				nextTooltip = cmdTooltip(key, Main.ON_STEAM_DECK ? null : shortcut);
			}
		});
	}
	
	private void shortcutHint(MyDraw d, int x, int y, String s) {
		if (Main.ON_STEAM_DECK) {
			s = null;
		}
		if (s == null) { return; }
		if (commandTooltip == null) {
			d.text(MyDraw.SELECTED_C + s, AGame.FOUNT, x + MyDraw.BUTTON_H / 2 - (int) (d.textSize(s, AGame.FOUNT).x) / 2 - 1, y + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING);
		}
	}
	
	private void shortcutHint2(MyDraw d, int x, int y, String s) {
		if (Main.ON_STEAM_DECK) {
			s = null;
		}
		if (s == null) { return; }
		d.text("[483f33]" + s, AGame.FOUNT, x + MyDraw.BUTTON_H / 2 - (int) (d.textSize(s, AGame.FOUNT).x) / 2 - 1, y + MyDraw.BUTTON_H - 3);
	}
	
	private void exit(UniScreen us) {
		CombatSoundEffects cse = us.overlay(CombatSoundEffects.class);
		cameraMix = 0;
		if (cse.altitudeLoop != null) {
			cse.altitudeLoop.stop();
			cse.altitudeLoop = null;
		}
	}

	@Override
	public void tick(Input in, int ms, UniScreen us) {
		us.overrideCamera = false;
		relinquishControlCooldown -= ms;
		msSinceAltitudeAdjustment += ms;
		lastMs = ms;
		commandTooltip = nextTooltip;
		nextTooltip = null;
		if (!(us.intent instanceof CombatIntent) || us.intent instanceof PlaybackIntent) { exit(us); return; }
		if (us.intent instanceof CombatIntent && ((CombatIntent) us.intent).spectate()) { exit(us); return; }
		if (us.combat.startCountdown > 0) { exit(us); return; }
		Airship ship = getShip(us);
		if (ship == null) { prevShip = null; exit(us); return; }
		if (ship != prevShip) {
			if (prevShip != null) {
				cameraMix = 1;
			}
			prevShip = ship;
			targetAltitude = ship.altitudeOrder;
			newTargetAltitude = ship.altitudeOrder;
			targetFlip = ship.flipped;
			targetSpeed = ShipSpeed.STOP;
			disabledWeapons.clear();
		}
		
		if (targetSoundPt != null) {
			ScreenMode sm = in.mode();
			in.play("target", 1, AirshipGame.instance.volume, (targetSoundPt.x - sm.width / 2) / (sm.width / 2), (targetSoundPt.y - sm.height / 2) / (sm.height / 2));
			targetSoundPt = null;
		}
		
		Combat combat = us.combat;
		
		ArrayList<WeaponGroup> gs = getWeaponGroups(ship);
		ArrayList<ModuleType> gTypes = new ArrayList<ModuleType>();
		for (int i = 0; i < gs.size(); i++) {
			ModuleType t = gs.get(i).type;
			gTypes.add(t);
			if (in.keyPressed("" + (i + 1))) {
				if (disabledWeapons.contains(t)) {
					disabledWeapons.remove(t);
				} else {
					disabledWeapons.add(t);
				}
			}
		}
		disabledWeapons.retainAll(gTypes);
		
		/*if (Keys.check(in, "direct_module_autofire", "COMMA", false)) {
			double cursorX = us.screenToWorldX(in.cursor().x);
			double cursorY = us.screenToWorldY(in.cursor().y);
			JSONArray moduleList = new JSONArray();
			for (int i = 0; i < ship.modules.size(); i++) {
				Module m = ship.modules.get(i);
				if (!m.type.isWeapon() || m.hp <= 0) { continue; }
				Pt mz = m.fireFrom();
				if (m.canHit(mz.x, mz.y, (int) cursorX, (int) cursorY)) {
					moduleList.put(i);
				}
			}
			if (moduleList.length() > 0) {
				combat.giveCommand(msg("fireOrder")
						.put("id", combat.getShipID(ship))
						.put("mode", Module.DirectControlFireMode.AUTO.name())
						.put("modules", moduleList));
			}
		}*/
		
		ScreenMode sm = in.mode();
		us.overrideCamera = true;
		
		if (cameraMix == 0) {
			oldScrollX = us.scrollX;
			oldScrollY = us.scrollY;
			oldZoom = us.zoom;
		}
		
		cameraMix = Math.min(1, cameraMix + ms / 200.0);
		
		double mix = cameraMix < 0.5 ? 2 * cameraMix * cameraMix : 1 - Math.pow(-2 * cameraMix + 2, 2) / 2;
		
		us.scrollX = (int) -(ship.getX() + ship.getBBWidth() / 2);
		us.scrollX = (int) (mix * us.scrollX + (1 - mix) * oldScrollX);
		us.scrollY = (int) -(ship.getY() + ship.getBBHeight() / 2);
		us.scrollY = (int) (mix * us.scrollY + (1 - mix) * oldScrollY);
		
		if (cameraMix < 1) {
			zoom = mix * 0.5 + (1 - mix) * oldZoom;
		} else {
			if (Keys.checkDown(in, "zoom_in", "ADD", false)) {
				zoom = zoom * (1 + AirshipGame.zoomSpeed * 0.01);
			}
			if (Keys.checkDown(in, "zoom_out", "SUBTRACT", false)) {
				zoom = zoom / (1 + AirshipGame.zoomSpeed * 0.01);
			}

			zoom = zoom * StrictMath.pow((1 + AirshipGame.mouseWheelZoomSpeed * 0.0001), in.scrollAmount());
		}
		
		if (zoom > 16.1) {
			zoom = 16.1;
		}

		if (zoom < 0.49 * sm.width / 1536) { zoom = 0.49 * sm.width / 1536; }
		us.zoom = zoom;
				
		us.adjScrollX = us.scrollX + (int) (sm.width / 2 / zoom) + (int) (us.vibration * (AGame.ANIM_R.nextDouble() - 0.5));
		us.adjScrollY = us.scrollY + (int) (sm.height / 2 / zoom) + (int) (us.vibration * (AGame.ANIM_R.nextDouble() - 0.5));

		while (us.screenToWorldX(0) <= -combat.combatAreaW() / 2) {
			us.scrollX--;
			us.adjScrollX = us.scrollX + (int) (sm.width / 2 / zoom);
		}
		while (us.screenToWorldX(sm.width) >= combat.combatAreaW() / 2) {
			us.scrollX++;
			us.adjScrollX = us.scrollX + (int) (sm.width / 2 / zoom);
		}
		while (us.screenToWorldY(0) <= AGame.GROUND_LEVEL - 3500) {
			us.scrollY--;
			us.adjScrollY = us.scrollY + (int) (sm.height / 2 / zoom);
		}
		while (us.screenToWorldY(sm.height) >= AGame.GROUND_LEVEL + 500) {
			us.scrollY++;
			us.adjScrollY = us.scrollY + (int) (sm.height / 2 / zoom);
		}
		
		if (us.textInputOccurring()) { return; }
		if (ship.type.mobile) {
			if ((in.keyPressed("D") || in.keyPressed("RIGHT")) && targetSpeed != ShipSpeed.FULL_SPEED_RIGHT) {
				setSpeed(in, us, ship, ShipSpeed.values()[targetSpeed.ordinal() + 1]);
			}
			if ((in.keyPressed("A") || in.keyPressed("LEFT")) && targetSpeed != ShipSpeed.FULL_SPEED_LEFT) {
				setSpeed(in, us, ship, ShipSpeed.values()[targetSpeed.ordinal() - 1]);
			}
			if (Keys.check(in, "direct_flip", "Q", false)) {
				flip(ship, combat);
			}
		}
		if (!ship.type.onGround) {
			if (in.keyDown("W") || in.keyDown("UP")) {
				newTargetAltitude -= ms / 4;
				altLoopAmount += ms * 0.01;
			}
			if (in.keyDown("S") || in.keyDown("DOWN")) {
				newTargetAltitude += ms / 4;
				altLoopAmount += ms * 0.01;
			}
		}
		int minTargetAltitude = AGame.GROUND_LEVEL - ship.availableServiceCeiling(us.combat);
		int maxTargetAltitude = AGame.GROUND_LEVEL;
		if (newTargetAltitude <= minTargetAltitude || newTargetAltitude >= maxTargetAltitude) {
			altLoopAmount = 0;
		}
		newTargetAltitude = Math.max(minTargetAltitude, Math.min(maxTargetAltitude, newTargetAltitude));
		altLoopAmount -= ms * 0.005;
		CombatSoundEffects cse = us.overlay(CombatSoundEffects.class);
		if (altLoopAmount <= 0) {
			altLoopAmount = 0;
			if (cse.altitudeLoop != null) {
				cse.altitudeLoop.stop();
				cse.altitudeLoop = null;
			}
		} else {
			if (altLoopAmount > 1) { altLoopAmount = 1; }
			if (cse.altitudeLoop == null) {
				cse.altitudeLoop = in.loop("grind", 1, altLoopAmount * us.g.volume * 0.6, 0, 0);
			} else {
				cse.altitudeLoop.setVolume((float) (altLoopAmount * us.g.volume * 0.6));
			}
		}
		if (newTargetAltitude != targetAltitude && (msSinceAltitudeAdjustment >= 128)) {
			combat.giveCommand(msg("altitudeOrder").put("id", combat.getShipID(ship)).put("altitude", newTargetAltitude));
			targetAltitude = newTargetAltitude;
			msSinceAltitudeAdjustment = 0;
		}
		
		if (ship.canGiveAircraftCommands()) {
			for (AircraftBehaviourMode mode : AircraftBehaviourMode.values()) {
				if (Keys.check(in, mode.name(), mode.defaultShortcut, false)) {
					planeMode(combat, ship, mode);
				}
			}
		}
		
		if (us.tool != UniScreen.NAVIGATE) { return; }
		
		if (relinquishControlCooldown <= 0 && Keys.check(in, "direct_control", "C", false)) {
			relinquishControl(ship, combat);
		}
		
		if (Keys.check(in, "board_ship", "B", false) && BoardCommandTool.canBoard(ship, us.mySide)) {
			us.selectedShip = ship;
			us.selectedShips.clear();
			us.tool = new BoardCommandTool(false);
		}
		
		if (Keys.check(in, "tether_ship", "J", false) && canTether(ship)) {
			us.selectedShip = ship;
			us.selectedShips.clear();
			us.tool = new TetherCommandTool();
		}

		if (Keys.check(in, "cut_tethers", "K", false) && canCutTether(ship)) {
			cutTether(us.combat, ship);
		}
		
		if (Keys.check(in, "focus_on_firefighting", "F", false) && ship.canFirefight()) {
			focusOnFirefighting(combat, ship);
		}
		
		if (Keys.check(in, "focus_on_repair", "E", false) && ship.canRepair()) {
			focusOnRepair(combat, ship);
		}
		
		if (Keys.check(in, "focus_on_moving", "V", false) && ship.canMove()) {
			focusOnMoving(combat, ship);
		}
		
		if (Keys.check(in, "focus_on_shooting", "O", false) && ship.canShoot()) {
			focusOnShooting(combat, ship);
		}
		
		if (Keys.check(in, "hold_fire", "H", false) && ship.canShoot()) {
			fireMode(combat, ship, FireMode.HOLD);
		}
		
		if (Keys.check(in, "aimed_fire", "I", false) && ship.canShoot()) {
			fireMode(combat, ship, FireMode.AIMED);
		}
		
		if (Keys.check(in, "normal_fire", "N", false) && ship.canShoot()) {
			fireMode(combat, ship, FireMode.NORMAL);
		}
		
		if (Keys.check(in, "rapid_fire", "P", false) && ship.canShoot()) {
			fireMode(combat, ship, FireMode.RAPID);
		}
		
		if (ship.getCaptain() != null) {
			for (HeroType.CombatAbility ab : ship.getCaptain().type.combatAbilities) {
				if (CommandButtonsPanel.canDoAbility(us, ship, ab) && Keys.check(in, ab.name(), ab.shortcut, true)) {
					doAbility(us, ship, ab);
				}
			}
		}
		
		if (!ship.releaseOneUseWeapons && ship.hasOneUseWeapons() && Keys.check(in, "oneUseWeapons", "COMMA", false)) {
			oneUseWeapons(combat, ship);
		}
		if (!ship.releaseOneUseLift && ship.hasOneUseLift()&& Keys.check(in, "oneUseLift", "PERIOD", false)) {
			oneUseWeapons(combat, ship);
		}
		if (!ship.releaseOneUsePropulsion && ship.hasOneUsePropulsion()&& Keys.check(in, "oneUsePropulsion", "SLASH", false)) {
			oneUseWeapons(combat, ship);
		}
	}
	
	private void relinquishControl(Airship ship, Combat combat) {
		combat.giveCommand(msg("setDirectControl").put("id", combat.getShipID(ship)).put("controllerID", -1));
	}
	
	private void setSpeed(Input in, UniScreen us, Airship ship, ShipSpeed speed) {
		targetSpeed = speed;
		us.combat.giveCommand(msg("speedOrder").put("id", us.combat.getShipID(ship)).put("speed", targetSpeed.name()));
		in.play("clack", 1.0, 1.0 * us.g.volume, 0, 0);
	}
	
	private void flip(Airship ship, Combat combat) {
		targetFlip = !targetFlip;
		combat.giveCommand(msg("flipOrder").put("id", combat.getShipID(ship)).put("flipTo", targetFlip));
	}
	
	@Override
	public boolean chatEnabled(UniScreen us) {
		return false;
	}

	@Override
	public boolean doScroll(UniScreen us, int scrollAmt, Pt cursor, ScreenMode sm) {
		return false;
	}

	@Override
	public boolean arrowKeysInUse(UniScreen us) {
		return false;
	}
	
	private void focusOnShooting(Combat combat, Airship s) {
		combat.giveCommand(msg("focusOnShooting").put("id", combat.getShipID(s)));
	}

	private void focusOnRepair(Combat combat, Airship s) {
		combat.giveCommand(msg("focusOnRepair").put("id", combat.getShipID(s)));
	}

	private void focusOnFirefighting(Combat combat, Airship s) {
			combat.giveCommand(msg("focusOnFirefighting").put("id", combat.getShipID(s)));
	}
	
	private void focusOnMoving(Combat combat, Airship s) {
		combat.giveCommand(msg("focusOnMoving").put("id", combat.getShipID(s)));
	}
	
	private void cutTether(Combat combat, Airship s) {
		combat.giveCommand(msg("cutOwnTethers").put("id", combat.getShipID(s)));
	}

	private void fireMode(Combat combat, Airship s, FireMode m) {
		combat.giveCommand(msg("fireMode").put("id", combat.getShipID(s)).put("value", m.name()));
	}
	
	private void planeMode(Combat combat, Airship s, AircraftBehaviourMode m) {
		combat.giveCommand(msg("aircraftMode").put("id", combat.getShipID(s)).put("value", m.name()));
	}
	
	private void oneUseWeapons(Combat combat, Airship s) {
		combat.giveCommand(msg("releaseOneUse").put("id", combat.getShipID(s)).put("what", "weapons"));
	}
	
	private void oneUseLift(Combat combat, Airship s) {
		combat.giveCommand(msg("releaseOneUse").put("id", combat.getShipID(s)).put("what", "lift"));
	}
	
	private void oneUsePropulsion(Combat combat, Airship s) {
		combat.giveCommand(msg("releaseOneUse").put("id", combat.getShipID(s)).put("what", "propulsion"));
	}
	
	private static class WeaponGroup implements Comparable<WeaponGroup> {
		final ModuleType type;
		int quantity = 1;

		public WeaponGroup(ModuleType type) {
			this.type = type;
		}

		@Override
		public int compareTo(WeaponGroup t) {
			if (t.quantity == quantity) {
				return type.compareTo(t.type);
			}
			return t.quantity - quantity; // More weapons -> earlier in list
		}
	}
	
	private ArrayList<WeaponGroup> getWeaponGroups(Airship ship) {
		ArrayList<WeaponGroup> gs = new ArrayList<WeaponGroup>();
		HashMap<String, WeaponGroup> map = new HashMap<String, WeaponGroup>();
		for (int i = 0; i < ship.modules.size(); i++) {
			ModuleType t = ship.modules.get(i).type;
			if (!t.isWeapon()) { continue; }
			t = t.getVariantGroupHeadOrThis().getSymmetryGroupHead();
			if (!map.containsKey(t.name)) {
				WeaponGroup g = new WeaponGroup(t);
				gs.add(g);
				map.put(t.name, g);
			} else {
				map.get(t.name).quantity++;
			}
		}
		Collections.sort(gs);
		while (gs.size() > 9) {
			gs.remove(gs.size() - 1);
		}
		return gs;
	}
}
