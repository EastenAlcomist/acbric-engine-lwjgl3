package com.zarkonnen.airships;

import static com.zarkonnen.airships.Airship.WATER_FG;
import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.airships.HeroType.CombatAbility;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;


import static com.zarkonnen.airships.Lang._t;

public strictfp class ShipStatusChrome implements UniScreen.ShipChrome {
	//private final Img DEFEATED = new Img("ui", 9 * 16, 26 * 16, 16, 16, false);
	private final Img UNDER_COMMAND = new Img("spritesheet", 2 * 16, 24 * 16, 16, 16, false);
	private final Img COAL = new Img("ui", 12 * 16, 26 * 16, 16, 16, false);
	private final Img WATER = new Img("ui", 10 * 16, 26 * 16, 16, 16, false);
	private final Img AMMO = new Img("ui", 11 * 16, 26 * 16, 16, 16, false);
	private final Img REPAIR = new Img("ui", 3 * 16, 26 * 16, 16, 16, false);
	private final Img READY = new Img("ui", 7 * 16, 32 * 16, 16, 16, false);
	private final Img ALLY = new Img("ui", 20, 590, 18, 18, false);
	private final Img ENEMY = new Img("ui", 0, 592, 18, 18, false);
	private final Img OWNED = new Img("ui", 139, 634, 18, 18, false);
	private final Img heroIcon = new Img("heroes", 48, 0, 16, 16, false);
	private final Img AI_CONTROL = new Img("ui", 640, 400, 16, 16, false);
	private final Img drop = new Img("spritesheet", 544, 400, 16, 16, false);
	private final Img dropBorder = new Img("spritesheet", 560, 400, 16, 16, false);
	
	@Override
	public void draw(MyDraw d, Pt cursor, Airship ship, Side side, int x, int y, int w, int h, ScreenMode sm, UniScreen us) {
		if (us.hideUI) { return; }
		y -= 20;
		if (us.intent instanceof MultiplayerSetupIntent) {
			MultiplayerSetupIntent mpsi = (MultiplayerSetupIntent) us.intent;
			if (mpsi.getMySideShips().contains(ship)) {
				if (mpsi.getMySetupShips().contains(ship)) {
					d.blit(OWNED, x - 1, y - 1);
					d.tooltip(x, y, 16, 16, _t("your_ship_or_building"));
				} else {
					d.blit(ALLY, x - 1, y - 1);
					d.tooltip(x, y, 16, 16, _t("Ally"));
				}
			} else {
				d.blit(ENEMY, x - 1, y - 1);
				d.tooltip(x, y, 16, 16, _t("Enemy"));
			}
			return;
		}
		if (us.intent instanceof CombatSetupIntent && us.mySide != null) {
			CombatSetupIntent csi = (CombatSetupIntent) us.intent;
			if (us.mySide.ships.contains(ship)) {
				if (csi.isShipPlayerControlled(us, ship)) {
					d.blit(OWNED, x - 1, y - 1);
					d.tooltip(x, y, 16, 16, _t("your_ship_or_building"));
				} else {
					d.blit(ALLY, x - 1, y - 1);
					d.tooltip(x, y, 16, 16, _t("Ally"));
				}
			} else {
				d.blit(ENEMY, x - 1, y - 1);
				d.tooltip(x, y, 16, 16, _t("Enemy"));
			}
			return;
		}

		if (!(us.intent instanceof SingleCombatIntent || us.intent instanceof MultiplayerCombatIntent || us.intent instanceof MultiplayerCampaignCombatIntent || us.intent instanceof CampaignCombatIntent)) { return; }
		
		boolean myShip = (side == null || side == us.mySide);
		
		boolean defeated = us.combat != null && !ship.inCombat(us.combat);
		
		boolean partiallySubmerged = false;
		if (myShip) {
			// Water status
			for (Module m : ship.modules) {
				if (m.isDisabledDueToSubmerged()) {
					d.blit(dropBorder, MyDraw.DARK_BG, (int) (x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS * us.zoom), (int) (20 + y + m.y * AGame.SGS * us.zoom));
					d.blit(drop, WATER_FG, (int) (x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS * us.zoom), (int) (20 + y + m.y * AGame.SGS * us.zoom));
					partiallySubmerged = true;
				}
			}
		}
		
		if (us.mySide != null) {// && (us.intent instanceof MultiplayerCombatIntent || us.intent instanceof MultiplayerCampaignCombatIntent || us.intent instanceof CampaignCombatIntent)) {
			if (side == us.mySide) {
				if (((CombatIntent) us.intent).isShipPlayerControlled(us, ship)) {
					if (!defeated) {
						d.blit(OWNED, x - 1, y - 1);
						d.tooltip(x, y, 16, 16, _t("your_ship_or_building"));
						x += 20;
					}
					myShip = true;
				} else {
					if (!defeated) {
						d.blit(ALLY, x - 1, y - 1);
						d.tooltip(x, y, 16, 16, _t("Ally"));
						x += 20;
					}
					myShip = false;
				}
			} else {
				if (!defeated) {
					d.blit(ENEMY, x - 1, y - 1);
					d.tooltip(x, y, 16, 16, _t("Enemy"));
					x += 20;
				}
				myShip = false;
			}
		}
		
		if (EHeroes.it.enabled && myShip && !ship.isBonusConstruction) {
			CrewExperienceLevel cel = CrewExperienceLevel.getLevel(ship.crewExperience);
			d.borderedBlit(cel.icon, MyDraw.TITLE, MyDraw.DARK_BG, x, y);
			d.tooltip(x, y, 16, 16, cel.getName() + "\n\n" + _t("crew_xp_explanation", (ship.crewExperience >= 0 ? "+" : "") + (int) (ship.crewExperience * 100)));
			x += 20;
		}
		
		if (ship.getCaptain() != null) {
			d.borderedBlit(heroIcon, MyDraw.TITLE, MyDraw.DARK_BG, x, y);
			d.tooltip(x, y, 16, 16, ship.getCaptain().getDetails(false));
			x += 20;
		}
		
		if (partiallySubmerged) {
			d.borderedBlit(drop, WATER_FG, MyDraw.DARK_BG, x, y);
			d.tooltip(x, y, 16, 16, _t("submerged_modules_notice"));
			x += 20;
		}
		
		// Abilities
		if (ship.smokescreenTime > 0) {
			d.borderedBlit(CombatAbility.SMOKESCREEN.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_SMOKESCREEN") + "\n\n" + _t("ability_desc_SMOKESCREEN"));
			x += 20;
		}
		if (ship.burstOfSpeedTime > 0) {
			d.borderedBlit(CombatAbility.BURST_OF_SPEED.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_BURST_OF_SPEED") + "\n\n" + _t("ability_desc_BURST_OF_SPEED"));
			x += 20;
		}
		if (ship.superchargeSuspendiumTime > 0) {
			d.borderedBlit(CombatAbility.SUPERCHARGE_SUSPENDIUM.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_SUPERCHARGE_SUSPENDIUM") + "\n\n" + _t("ability_desc_SUPERCHARGE_SUSPENDIUM"));
			x += 20;
		}
		if (ship.doubleTimeTime > 0) {
			d.borderedBlit(CombatAbility.DOUBLE_TIME.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_DOUBLE_TIME") + "\n\n" + _t("ability_desc_DOUBLE_TIME"));
			x += 20;
		}
		if (ship.fearTime > 0) {
			d.borderedBlit(CombatAbility.FEAR.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_FEAR") + "\n\n" + _t("ability_desc_FEAR"));
			x += 20;
		}
		if (ship.tauntTime > 0) {
			d.borderedBlit(CombatAbility.TAUNT.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_TAUNT") + "\n\n" + _t("ability_desc_TAUNT"));
			x += 20;
		}
		if (ship.glimmerTime > 0) {
			d.borderedBlit(CombatAbility.BLINDING_GLIMMER.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_BLINDING_GLIMMER") + "\n\n" + _t("ability_desc_BLINDING_GLIMMER"));
			x += 20;
		}
		if (ship.blindnessTime > 0) {
			d.borderedBlit(CombatAbility.HYSTERICAL_BLINDNESS.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_HYSTERICAL_BLINDNESS") + "\n\n" + _t("ability_desc_HYSTERICAL_BLINDNESS"));
			x += 20;
		}
		if (ship.crosswindsTime > 0) {
			d.borderedBlit(CombatAbility.CROSSWINDS.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_CROSSWINDS") + "\n\n" + _t("ability_desc_CROSSWINDS"));
			x += 20;
		}
		if (ship.paralysisTime > 0) {
			d.borderedBlit(CombatAbility.PARALYSIS.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_PARALYSIS") + "\n\n" + _t("ability_desc_PARALYSIS"));
			x += 20;
		}
		if (ship.momentOfDoubtTime > 0) {
			d.borderedBlit(CombatAbility.MOMENT_OF_DOUBT.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_MOMENT_OF_DOUBT") + "\n\n" + _t("ability_desc_MOMENT_OF_DOUBT"));
			x += 20;
		}
		if (ship.gustOfWindTime > 0) {
			d.borderedBlit(CombatAbility.GUST_OF_WIND.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_GUST_OF_WIND") + "\n\n" + _t("ability_desc_GUST_OF_WIND"));
			x += 20;
		}
		if (ship.suddenStormTime > 0) {
			d.borderedBlit(CombatAbility.SUDDEN_STORM.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_SUDDEN_STORM") + "\n\n" + _t("ability_desc_SUDDEN_STORM"));
			x += 20;
		}
		if (ship.holdOnTime > 0) {
			d.borderedBlit(CombatAbility.LAST_STAND.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_LAST_STAND") + "\n\n" + _t("ability_desc_LAST_STAND"));
			x += 20;
		}
		if (ship.cripplingShotTarget) {
			d.borderedBlit(CombatAbility.CRIPPLING_SHOT.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_CRIPPLING_SHOT") + "\n\n" + _t("ability_desc_CRIPPLING_SHOT"));
			x += 20;
		}
		if (ship.disarmingShotTarget) {
			d.borderedBlit(CombatAbility.DISARMING_SHOT.icon, MyDraw.DARK_BG, MyDraw.SELECTED, x, y);
			d.tooltip(x, y, 16, 16, _t("ability_DISARMING_SHOT") + "\n\n" + _t("ability_desc_DISARMING_SHOT"));
			x += 20;
		}
		
		if (myShip && !defeated && ship.readyForCommand() && ship.getDirectControlID() != -1) {
			d.borderedBlit(READY, Clr.GREEN, Clr.WHITE, x, y);
			d.tooltip(x, y, 16, 16, _t("Ready_for_command"));
			x += 20;
		}
		
		if (myShip && ship.aiControl) {
			d.borderedBlit(AI_CONTROL, MyDraw.TITLE, MyDraw.DARK_BG, x, y);
			d.tooltip(x, y, 16, 16, _t("AI_Control"));
			x += 20;
		}

		if (myShip && !defeated) {
			if (!ship.generatesCommandPoints()) {
				d.borderedBlit(UNDER_COMMAND, Clr.RED, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("Not_under_command"));
				x += 20;
			}
			int cc = ship.getCoalCapacity();
			int ct = ship.getTotalResource(Resource.COAL);
			if (cc > 0 && ct == 0) {
				d.borderedBlit(COAL, Clr.RED, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("Out_of_COAL"));
				x += 20;
			} else if (cc > 0 && ct < cc * 0.2) {
				d.borderedBlit(COAL, Clr.ORANGE, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_COAL_left", ct, cc));
				x += 20;
			} else if (cc > 0 && ct < cc * 0.4) {
				d.borderedBlit(COAL, Clr.YELLOW, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_COAL_left", ct, cc));
				x += 20;
			}
			int wc = ship.getWaterCapacity();
			int wt = ship.getTotalResource(Resource.WATER);
			if (wc > 0 && wt == 0) {
				d.borderedBlit(WATER, Clr.RED, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("Out_of_WATER"));
				x += 20;
			} else if (wc > 0 && wt < wc * 0.2) {
				d.borderedBlit(WATER, Clr.ORANGE, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_WATER_left", wt, wc));
				x += 20;
			} else if (wc > 0 && wt < wc * 0.4) {
				d.borderedBlit(WATER, Clr.YELLOW, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_WATER_left", wt, wc));
				x += 20;
			}
			int rc = ship.getRepairCapacity();
			int rt = ship.getTotalResource(Resource.REPAIR);
			if (rc > 0 && rt == 0) {
				d.borderedBlit(REPAIR, Clr.RED, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("Out_of_REPAIR"));
				x += 20;
			} else if (rc > 0 && rt < rc * 0.2) {
				d.borderedBlit(REPAIR, Clr.ORANGE, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_REPAIR_left", rt, rc));
				x += 20;
			} else if (rc > 0 && rt < rc * 0.4) {
				d.borderedBlit(REPAIR, Clr.YELLOW, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_REPAIR_left", rt, rc));
				x += 20;
			}
			int ac = ship.getAmmoCapacity();
			int at = ship.getTotalResource(Resource.AMMO);
			if (ac > 0 && at == 0) {
				d.borderedBlit(AMMO, Clr.RED, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("Out_of_AMMO"));
				x += 20;
			} else if (ac > 0 && at < ac * 0.2) {
				d.borderedBlit(AMMO, Clr.ORANGE, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_AMMO_left", at, ac));
				x += 20;
			} else if (ac > 0 && at < ac * 0.4) {
				d.borderedBlit(AMMO, Clr.YELLOW, Clr.WHITE, x, y);
				d.tooltip(x, y, 16, 16, _t("x_y_AMMO_left", at, ac));
				x += 20;
			}
		}
	}
	
	@Override public void tick(Input in, int ms, UniScreen us) {}

	@Override public boolean textInputOccurring(UniScreen us) { return false; }
}
