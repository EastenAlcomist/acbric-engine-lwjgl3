package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;

import java.util.ArrayList;
import java.util.Collections;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public strictfp class PlaceModuleTool extends PlaceTool {
	@Override public String getLabel() { return _t("Modules"); }
	
	public static final Img REMOVE = new Img("ui", 16 * 4, 16 * 16, 16, 16, false);
	
	public ModuleType mt;
	public int ms = 0;
	public boolean dragMode;
	public boolean selectionIsDetachedFromPanel;
	
	public static final Clr NO_OBSTRUCTION = new Clr(90, 255, 90, 100);
	public static final Clr OBSTRUCTED = new Clr(255, 90, 90, 100);
	public static final Clr OBSTRUCTED_BORDER = new Clr(255, 25, 25);
	
	public PlaceModuleTool(ModuleType mt, boolean dragMode) {
		this.mt = mt;
		this.dragMode = dragMode;
	}
	
	private boolean checkLimits(ModuleType mt, UniScreen us) {
		Airship ship = ship(us);
		return mt.getQuarters(ship.currentBonuses) + ship.getAllQuartered() <= ShipEditorUtils.MAX_CREW && mt.getCost(ship.currentBonuses) + ship.getCost() <= ShipEditorUtils.MAX_COST;
	}
	
	private void drawHorizontalBeam(MyDraw d, UniScreen us, Clr inner, Clr outer, double x, double y, double w, double h) {
		if (w == 0) {
			x -= 2;
			w = 4;
		}
		d.rect(inner, x, y + 2, w, h - 4);
		d.rect(outer, x, y + 2, w, 1 / us.zoom);
		d.rect(outer, x, y + h - 2 - 1 / us.zoom, w, 1 / us.zoom);
	}
	
	private void drawVerticalBeam(MyDraw d, UniScreen us, Clr inner, Clr outer, double x, double y, double w, double h) {
		if (h == 0) {
			y -= 2;
			h = 4;
		}
		d.rect(inner, x + 2, y, w - 4, h);
		d.rect(outer, x + 2, y, 1 / us.zoom, h);
		d.rect(outer, x + w - 2 - 1 / us.zoom, y, 1 / us.zoom, h);
	}
	
	private boolean canFlip(UniScreen us) {
		if (mt.getFlippedIfAvailable() == null) { return false; }
		if (us.intent instanceof ChallengeEditShipIntent) {
			ChallengeEditShipIntent cesi = (ChallengeEditShipIntent) us.intent;
			return cesi.challenge.editTypes.contains(mt.getFlippedIfAvailable());
		}
		return true;
	}
	
	private boolean hasVariant(UniScreen us) {
		return mt != null && mt.isVariantGroupMember() && !(us.intent instanceof ChallengeEditShipIntent);
	}
	
	private boolean canFlipVertically(UniScreen us) {
		if (mt.getVerticalFlippedIfAvailable() == null) { return false; }
		if (us.intent instanceof ChallengeEditShipIntent) {
			ChallengeEditShipIntent cesi = (ChallengeEditShipIntent) us.intent;
			return cesi.challenge.editTypes.contains(mt.getVerticalFlippedIfAvailable());
		}
		return true;
	}

	@Override
	public void scaledDraw(MyDraw d, double x, double y, int modX, int modY, UniScreen us) {
		if (mt != null) {
			if (hasVariant(us)) {
				if (canFlip(us)) {
					if (canFlipVertically(us)) {
						d.state.cursorText = _t("Press_x_to_flip_module_c_to_flip_vertically_comma_for_variant", Keys.getText("module_flip", "X", false), Keys.getText("module_flip_vertical", "C", false), Keys.getText("module_variant", "N", false));
					} else {
						d.state.cursorText = _t("Press_x_to_flip_module_comma_for_variant", Keys.getText("module_flip", "X", false), Keys.getText("module_variant", "N", false));
					}
				} else if (canFlipVertically(us)) {
					d.state.cursorText = _t("Press_c_to_flip_module_vertically_comma_for_variant", Keys.getText("module_flip_vertical", "C", false), Keys.getText("module_variant", "N", false));
				} else {
					d.state.cursorText = _t("Press_comma_for_variant", Keys.getText("module_variant", "N", false));
				}
			} else {
				if (canFlip(us)) {
					if (canFlipVertically(us)) {
						d.state.cursorText = _t("Press_x_to_flip_module_c_to_flip_vertically", Keys.getText("module_flip", "X", false), Keys.getText("module_flip_vertical", "C", false));
					} else {
						d.state.cursorText = _t("Press_x_to_flip_module", Keys.getText("module_flip", "X", false));
					}
				} else if (canFlipVertically(us)) {
					d.state.cursorText = _t("Press_c_to_flip_module_vertically", Keys.getText("module_flip_vertical", "C", false));
				}
			}
			for (ExternalApp ea : mt.getExternalApps(bonuses(us), false, false)) {
				if (ea.dx == 0 && ea.dy == 0 && ea.app.width() == mt.getApp(bonuses(us), 1).width() && ea.app.height() == mt.getApp(bonuses(us), 1).height()) {
					ea.app.draw(d, x + ea.dx * AGame.SGS, y + ea.dy * AGame.SGS, 0, ship(us).flipped);
				}
			}
			Airship ship = ship(us);
			
			boolean modPermitted = checkLimits(mt, us) && noOverlapsOrObstructions(ship, us, mt, /*modX*/ship.gridXToWorldX(modX, mt.getW()), modY);
			int flippedModX = ship.gridXToWorldX(modX, mt.getW());

			// Obstruction beams.
			if (checkLimits(mt, us) && noOverlaps(ship, us, mt, /*modX*/ship.gridXToWorldX(modX, mt.getW()), modY)) {
				boolean isObstructing = false;
				Module obstructing = ship.moduleWhoseFrontIsObstructed(mt, flippedModX, modY, Collections.EMPTY_LIST);
				if (obstructing != null) {
					int obX = ship.gridXToWorldX(obstructing.x, obstructing.type.getW());
					isObstructing = true;
					for (int i = 0; i < obstructing.type.getH(); i++) {
						if (!obstructing.type.isFrontOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < obstructing.type.getH() && obstructing.type.isFrontOnly()[i + extent]) {
							extent++;
						}
						if (ship.flipped) {
							drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
									ship.getX() + (modX + mt.getW()) * AGame.SGS,
									ship.getY() + (obstructing.y + i) * AGame.SGS,
									(ship.getWidth() - modX - mt.getW() - obstructing.x - obstructing.type.getW()) * AGame.SGS,
									extent * AGame.SGS);
						} else {
							drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
									ship.getX() + (obX + obstructing.type.getW()) * AGame.SGS,
									ship.getY() + (obstructing.y + i) * AGame.SGS ,
									(flippedModX - obX - obstructing.type.getW()) * AGame.SGS,
									extent * AGame.SGS);
						}
						i += extent - 1;
					}
				}
				obstructing = ship.moduleWhoseBackIsObstructed(mt, flippedModX, modY, Collections.EMPTY_LIST);
				if (obstructing != null) {
					isObstructing = true;
					int obX = ship.gridXToWorldX(obstructing.x, obstructing.type.getW());
					for (int i = 0; i < obstructing.type.getH(); i++) {
						if (!obstructing.type.isBackOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < obstructing.type.getH() && obstructing.type.isBackOnly()[i + extent]) {
							extent++;
						}
						if (ship.flipped) {
							drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
									ship.getX() + (obX + obstructing.type.getW()) * AGame.SGS,
									ship.getY() + (obstructing.y + i) * AGame.SGS,
									(ship.getWidth() - flippedModX - obstructing.type.getW() - obX - mt.getW()) * AGame.SGS,
									extent * AGame.SGS);

						} else {
							drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
									ship.getX() + (mt.getW() + flippedModX) * AGame.SGS,
									ship.getY() + (obstructing.y + i) * AGame.SGS,
									(obX - mt.getW() - flippedModX) * AGame.SGS,
									extent * AGame.SGS);
						}
						i += extent - 1;
					}
				}
				obstructing = ship.moduleWhoseBottomIsObstructed(mt, flippedModX, modY, Collections.EMPTY_LIST);
				if (obstructing != null) {
					isObstructing = true;
					int obX = ship.gridXToWorldX(obstructing.x, obstructing.type.getW());
					for (int i = 0; i < obstructing.type.getW(); i++) {
						if (!obstructing.type.isBottomOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < obstructing.type.getW() && obstructing.type.isBottomOnly()[i + extent]) {
							extent++;
						}
						int beamX = ship.flipped ? obX - i + obstructing.type.getW() - extent : obX + i;
						drawVerticalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
								ship.getX() + beamX * AGame.SGS,
								ship.getY() + (obstructing.y + obstructing.type.getH()) * AGame.SGS,
								extent * AGame.SGS,
								(modY - obstructing.y - obstructing.type.getH()) * AGame.SGS);
						i += extent - 1;
					}
				}
				obstructing = ship.moduleWhoseTopIsObstructed(mt, flippedModX, modY, Collections.EMPTY_LIST);
				if (obstructing != null) {
					isObstructing = true;
					int obX = ship.gridXToWorldX(obstructing.x, obstructing.type.getW());
					for (int i = 0; i < obstructing.type.getW(); i++) {
						if (!obstructing.type.isTopOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < obstructing.type.getW() && obstructing.type.isTopOnly()[i + extent]) {
							extent++;
						}
						int beamX = ship.flipped ? obX - i + obstructing.type.getW() - extent : obX + i;
						drawVerticalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
								ship.getX() + beamX * AGame.SGS,
								ship.getY() + (modY + mt.getH()) * AGame.SGS,
								extent * AGame.SGS,
								(obstructing.y - modY - mt.getH()) * AGame.SGS);
						i += extent - 1;
					}
				}
				if (!isObstructing) {
					obstructing = ship.moduleThatObstructsFront(mt, flippedModX, modY, Collections.EMPTY_LIST);
					for (int i = 0; i < mt.getH(); i++) {
						if (!mt.isFrontOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < mt.getH() && mt.isFrontOnly()[i + extent]) {
							extent++;
						}
						if (obstructing == null) {
							if (ship.flipped) {
								drawHorizontalBeam(d, us, NO_OBSTRUCTION, Clr.WHITE,
										ship.getX() + modX * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										-1000 * AGame.SGS,
										extent * AGame.SGS);

							} else {
								drawHorizontalBeam(d, us, NO_OBSTRUCTION, Clr.WHITE,
										ship.getX() + (mt.getW() + flippedModX) * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										1000 * AGame.SGS,
										extent * AGame.SGS);
							}
						} else {
							int obX = ship.gridXToWorldX(obstructing.x, obstructing.type.getW());
							if (ship.flipped) {
								drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
										ship.getX() + (obX + obstructing.type.getW()) * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										(ship.getWidth() - flippedModX - obstructing.type.getW() - obX - mt.getW()) * AGame.SGS,
										extent * AGame.SGS);

							} else {
								drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
										ship.getX() + (mt.getW() + flippedModX) * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										(obX - mt.getW() - flippedModX) * AGame.SGS,
										extent * AGame.SGS);
							}
						}
						i += extent - 1;
					}
					obstructing = ship.moduleThatObstructsBack(mt, flippedModX, modY, Collections.EMPTY_LIST);
					for (int i = 0; i < mt.getH(); i++) {
						if (!mt.isBackOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < mt.getH() && mt.isBackOnly()[i + extent]) {
							extent++;
						}
						if (obstructing == null) {
							if (ship.flipped) {
								drawHorizontalBeam(d, us, NO_OBSTRUCTION, Clr.WHITE,
										ship.getX() + (mt.getW() + modX) * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										1000 * AGame.SGS,
										extent * AGame.SGS);
							} else {
								drawHorizontalBeam(d, us, NO_OBSTRUCTION, Clr.WHITE,
										ship.getX() + modX * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										-1000 * AGame.SGS,
										extent * AGame.SGS);
							}
						} else {
							int obX = ship.gridXToWorldX(obstructing.x, obstructing.type.getW());
							if (ship.flipped) {
								drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
										ship.getX() + (mt.getW() - flippedModX) * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										(obX - mt.getW() + flippedModX) * AGame.SGS,
										extent * AGame.SGS);
							} else {
								drawHorizontalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
										ship.getX() + (obX + obstructing.type.getW()) * AGame.SGS,
										ship.getY() + (modY + i) * AGame.SGS,
										(flippedModX - obX - obstructing.type.getW()) * AGame.SGS,
										extent * AGame.SGS);
							}
						}
						i += extent - 1;
					}
					obstructing = ship.moduleThatObstructsBottom(mt, flippedModX, modY, Collections.EMPTY_LIST);
					for (int i = 0; i < mt.getW(); i++) {
						if (!mt.isBottomOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < mt.getW() && mt.isBottomOnly()[i + extent]) {
							extent++;
						}
						int beamX = ship.flipped ? modX - i + mt.getW() - extent : modX + i;
						if (obstructing == null) {
							drawVerticalBeam(d, us, NO_OBSTRUCTION, Clr.WHITE,
									ship.getX() + beamX * AGame.SGS,
									ship.getY() + (modY + mt.getH()) * AGame.SGS,
									extent * AGame.SGS,
									1000 * AGame.SGS);
						} else {
							drawVerticalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
									ship.getX() + beamX * AGame.SGS,
									ship.getY() + (modY + mt.getH()) * AGame.SGS,
									extent * AGame.SGS,
									(obstructing.y - modY - mt.getH()) * AGame.SGS);
						}
						i += extent - 1;
					}
					obstructing = ship.moduleThatObstructsTop(mt, flippedModX, modY, Collections.EMPTY_LIST);
					for (int i = 0; i < mt.getW(); i++) {
						if (!mt.isTopOnly()[i]) { continue; }
						int extent = 1;
						while (i + extent < mt.getW() && mt.isTopOnly()[i + extent]) {
							extent++;
						}
						int beamX = ship.flipped ? modX - i + mt.getW() - extent : modX + i;
						if (obstructing == null) {
							drawVerticalBeam(d, us, NO_OBSTRUCTION, Clr.WHITE,
									ship.getX() + beamX * AGame.SGS,
									ship.getY() + (modY - 1000) * AGame.SGS,
									extent * AGame.SGS,
									1000 * AGame.SGS);
						} else {
							drawVerticalBeam(d, us, OBSTRUCTED, OBSTRUCTED_BORDER,
									ship.getX() + beamX * AGame.SGS,
									ship.getY() + (obstructing.y + obstructing.type.getH()) * AGame.SGS,
									extent * AGame.SGS,
									(modY - obstructing.y - obstructing.type.getH()) * AGame.SGS);
						}
						i += extent - 1;
					}
				}
			}
			
			//boolean modGood = modPermitted && ship.canAddModule(mt, ship.gridXToWorldX(modX, mt.getW()), modY);
			WeaponAppearance wa = mt.weaponAppearance(bonuses(us));
			if (wa != null && (wa.barrel != null || wa.barrelAnimation != null)) {
				double angle =
						ship.flipped
						? -mt.getFireArc(bonuses(us)).getMiddle().radians
						: mt.getFireArc(bonuses(us)).getMiddle().radians;
				if (mt.isFlipped()) {
					angle += StrictMath.PI;
				}
				if (wa.ignoreBarrelRotation) {
					angle = 0;
				}
				
				Img barrel = ship.flipped ? wa.flippedbarrel : wa.barrel;
				if (wa.barrelAnimation != null) {
					barrel = ship.flipped ? wa.flippedBarrelAnimation.frames.get(0) : wa.barrelAnimation.frames.get(0);
				}
				if (barrel.machineImgCache != null) {
					((Image) barrel.machineImgCache).setFilter(Image.FILTER_NEAREST);
				}
				Pt offset = ship.flipped ? wa.flippedBarrelOffset : wa.barrelOffset;
				double bx = x + offset.x;
				double by = y + offset.y;
				if (modPermitted) {
					d.blit(barrel, null, bx, by, angle);
				} else {
					RotatingShader.drawAsRedOutline(SpritesheetBundle.ofName(barrel.src), barrel, d, bx, by, angle, 1, false);
				}
			}
			
			if (modPermitted) {
				mt.draw(d, x, y, ms, null, ship.flipped, 0, bonuses(us), 1, null, 1.0f, Color.white, 1.0f);
			} else {
				mt.drawAsRedOutline(d, x, y, ms, ship.flipped, 0, bonuses(us));
			}
			
			int lineWidth = 2;
			switch (AirshipGame.instance.currentGUIScale) {
				case SMALL: lineWidth = 1; break;
				case MEDIUM: lineWidth = 2; break;
				case LARGE: lineWidth = 4; break;
			}
			Clr c = new Clr(255, 255, 255, 80);
			if (modPermitted) {
				for (int i = 0; i < mt.getH(); i++) {
					if (mt.getLeftDoors()[i] && !mt.isBackOnly()[i] && mt.canOccupy(0, i)) {
						Tile adjTile = ship.tileAt(flippedModX - 1, modY + i);
						if (adjTile == null) {
							if (ship.flipped) {
								d.rect(c,
										ship.getX() + modX * AGame.SGS + AGame.SGS * mt.getW(),
										ship.getY() + (modY + i) * AGame.SGS + AGame.SGS - 1,
										6,
										1
								);
							} else {
								d.rect(c,
										ship.getX() + ship.gridXToWorldX(modX, mt.getW()) * AGame.SGS - 6,
										ship.getY() + (modY + i) * AGame.SGS + AGame.SGS - 1,
										6,
										1
								);
							}
						} else if (modPermitted && adjTile.canOccupy && !adjTile.module.type.isFrontOnly()[adjTile.y - adjTile.module.y] && adjTile.module.type.getRightDoors()[adjTile.y - adjTile.module.y] && adjTile.module.type.canOccupy(adjTile.module.type.getW() - 1, modY - adjTile.module.y + i)) {
							if (ship.flipped) {
								d.rect(Clr.BLACK,
									ship.getX() + modX * AGame.SGS - AGame.SGS / 2 + AGame.SGS * mt.getW(),
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 1.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									3 * lineWidth / us.zoom
								);
								d.rect(Clr.WHITE,
									ship.getX() + modX * AGame.SGS - AGame.SGS / 2 + AGame.SGS * mt.getW(),
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 0.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									1 * lineWidth / us.zoom
								);
							} else {
								d.rect(Clr.BLACK,
									ship.getX() + modX * AGame.SGS - AGame.SGS / 2,
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 1.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									3 * lineWidth / us.zoom
								);
								d.rect(Clr.WHITE,
									ship.getX() + modX * AGame.SGS - AGame.SGS / 2,
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 0.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									1 * lineWidth / us.zoom
								);
							}
						}
					}
					if (mt.getRightDoors()[i] && !mt.isFrontOnly()[i] && mt.canOccupy(mt.getW() - 1, i)) {
						Tile adjTile = ship.tileAt(flippedModX + mt.getW(), modY + i);
						if (adjTile == null) {
							if (ship.flipped) {
								d.rect(c,
										ship.getX() + modX * AGame.SGS - 6,
										ship.getY() + (modY + i) * AGame.SGS + AGame.SGS - 1,
										6,
										1
								);
							} else {
								d.rect(c,
										ship.getX() + ship.gridXToWorldX(modX, mt.getW()) * AGame.SGS + AGame.SGS * mt.getW(),
										ship.getY() + (modY + i) * AGame.SGS + AGame.SGS - 1,
										6,
										1
								);
							}
						} else if (modPermitted && adjTile.canOccupy && !adjTile.module.type.isBackOnly()[adjTile.y - adjTile.module.y] && adjTile.module.type.getLeftDoors()[adjTile.y - adjTile.module.y] && adjTile.module.type.canOccupy(0, modY - adjTile.module.y + i)) {
							if (ship.flipped) {
								d.rect(Clr.BLACK,
									ship.getX() + (modX) * AGame.SGS - AGame.SGS / 2,
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 1.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									3 * lineWidth / us.zoom
								);
								d.rect(Clr.WHITE,
									ship.getX() + (modX) * AGame.SGS - AGame.SGS / 2,
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 0.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									1 * lineWidth / us.zoom
								);
							} else {
								d.rect(Clr.BLACK,
									ship.getX() + (modX + mt.getW()) * AGame.SGS - AGame.SGS / 2,
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 1.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									3 * lineWidth / us.zoom
								);
								d.rect(Clr.WHITE,
									ship.getX() + (modX + mt.getW()) * AGame.SGS - AGame.SGS / 2,
									ship.getY() + modY * AGame.SGS + AGame.SGS / 2 - 0.5 * lineWidth / us.zoom + i * AGame.SGS,
									AGame.SGS,
									1 * lineWidth / us.zoom
								);
							}
						}
					}
				}
				// Top/bottom connectors
				for (int i = 0; i < mt.getW(); i++) {
					if (!mt.isTopOnly()[i] && mt.getUpDoors()[i] && mt.canOccupy(i, 0)) {
						Tile adjTile = ship.tileAt(flippedModX + i, modY - 1);
						if (adjTile == null) {
							if (ship.flipped) {
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 13 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS - 6,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 9 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS - 6,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS - 2,
										3,
										1
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS - 4,
										3,
										1
								);
							} else {
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 13 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS - 6,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 9 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS - 6,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS - 2,
										3,
										1
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS - 4,
										3,
										1
								);
							}
						} else if (adjTile.canOccupy) {
							if (ship.flipped) {
								d.rect(Clr.BLACK,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * (mt.getW() - i - 1) - 1.5 * lineWidth / us.zoom,
									ship.getY() + modY * AGame.SGS - AGame.SGS / 2,
									3 * lineWidth / us.zoom,
									AGame.SGS
								);
								d.rect(Clr.WHITE,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * (mt.getW() - i - 1) - 0.5 * lineWidth / us.zoom,
									ship.getY() + modY * AGame.SGS - AGame.SGS / 2,
									1 * lineWidth / us.zoom,
									AGame.SGS
								);
							} else {
								d.rect(Clr.BLACK,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * i - 1.5 * lineWidth / us.zoom,
									ship.getY() + modY * AGame.SGS - AGame.SGS / 2,
									3 * lineWidth / us.zoom,
									AGame.SGS
								);
								d.rect(Clr.WHITE,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * i - 0.5 * lineWidth / us.zoom,
									ship.getY() + modY * AGame.SGS - AGame.SGS / 2,
									1 * lineWidth / us.zoom,
									AGame.SGS
								);
							}
						}
					}
					if (!mt.isBottomOnly()[i] && mt.canOccupy(i, mt.getH() - 1)) {
						Tile adjTile = ship.tileAt(flippedModX + i, modY + mt.getH());
						if (adjTile == null) {
							if (ship.flipped) {
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 9 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 13 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS + 1,
										3,
										1
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * (mt.getW() - i - 1),
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS + 3,
										3,
										1
								);
							} else {
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 9 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 13 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS,
										1,
										6
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS + 1,
										3,
										1
								);
								d.rect(c,
										ship.getX() + modX * AGame.SGS + 10 + AGame.SGS * i,
										ship.getY() + modY * AGame.SGS + mt.getH() * AGame.SGS + 3,
										3,
										1
								);
							}
						} else if (adjTile.module.type.getUpDoors()[adjTile.x - adjTile.module.x]) {
							if (ship.flipped) {
								d.rect(Clr.BLACK,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * (mt.getW() - i - 1) - 1.5 * lineWidth / us.zoom,
									ship.getY() + (modY + mt.getH()) * AGame.SGS - AGame.SGS / 2,
									3 * lineWidth / us.zoom,
									AGame.SGS
								);
								d.rect(Clr.WHITE,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * (mt.getW() - i - 1) - 0.5 * lineWidth / us.zoom,
									ship.getY() + (modY + mt.getH()) * AGame.SGS - AGame.SGS / 2,
									1 * lineWidth / us.zoom,
									AGame.SGS
								);
							} else {
								d.rect(Clr.BLACK,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * i - 1.5 * lineWidth / us.zoom,
									ship.getY() + (modY + mt.getH()) * AGame.SGS - AGame.SGS / 2,
									3 * lineWidth / us.zoom,
									AGame.SGS
								);
								d.rect(Clr.WHITE,
									ship.getX() + modX * AGame.SGS + AGame.SGS / 2 + AGame.SGS * i - 0.5 * lineWidth / us.zoom,
									ship.getY() + (modY + mt.getH()) * AGame.SGS - AGame.SGS / 2,
									1 * lineWidth / us.zoom,
									AGame.SGS
								);
							}
						}
					}
				}
			}
		} else {
			if (REMOVE.machineImgCache != null) {
				((Image) REMOVE.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			d.blit(REMOVE, x, y);
		}
	}
	
	public boolean noOverlaps(Airship shipToPlace, UniScreen us, ModuleType mt, int modX, int modY) {
		if (!shipToPlace.noModuleOverlapsOrObstructions(mt, modX, modY, /* checkObstructions */ false)) {
			return false;
		}
		int worldX = shipToPlace.getIntX() + modX * AGame.SGS;
		int worldY = shipToPlace.getIntY() + modY * AGame.SGS;
		int worldW = mt.getW() * AGame.SGS;
		int worldH = mt.getH() * AGame.SGS;
		Airship ignore = (us.intent instanceof SingleShipIntent) ? ((SingleShipIntent) us.intent).getIgnoredOriginalShip(us) : null;
		if (us.combat != null) {
			for (Combat.Side side : us.combat.sides) {
				for (Airship ship : side.ships) {
					if (ship == ignore) { continue; }
					if (ship != shipToPlace && overlaps(ship, worldX, worldY, worldW, worldH)) { return false; }
				}
			}
		} else if (us.city != null) {
			Fleet garrison = us.wm.getGarrison(us.city);
			for (Airship bld : us.city.getDefences()) {
				if (bld == ignore) { continue; }
				if (bld != shipToPlace && overlaps(bld, worldX, worldY, worldW, worldH)) { return false; }
			}
			if (garrison != null) {
				for (Airship ship : garrison.actives) {
					if (ship == ignore) { continue; }
					if (ship != shipToPlace && overlaps(ship, worldX, worldY, worldW, worldH)) { return false; }
				}
			}
		} else if (us.getSetupFleet() != null) {
			for (Airship ship : us.getSetupFleet()) {
				if (ship == ignore) { continue; }
				if (ship != shipToPlace && overlaps(ship, worldX, worldY, worldW, worldH)) { return false; }
			}
		}
		return true;
	}
	
	public boolean noOverlapsOrObstructions(Airship shipToPlace, UniScreen us, ModuleType mt, int modX, int modY) {
		if (!shipToPlace.noModuleOverlapsOrObstructions(mt, modX, modY, /* checkObstructions */ true)) {
			return false;
		}
		int worldX = shipToPlace.getIntX() + modX * AGame.SGS;
		int worldY = shipToPlace.getIntY() + modY * AGame.SGS;
		int worldW = mt.getW() * AGame.SGS;
		int worldH = mt.getH() * AGame.SGS;
		if (us.intent instanceof RestrictsShipPlacement) {
			int realWorldX = worldX;
			if (shipToPlace.flipped) {
				realWorldX = shipToPlace.getIntX() + shipToPlace.getWidth() * AGame.SGS - modX * AGame.SGS - mt.getW() * AGame.SGS;
			}
			RestrictsShipPlacement rss = (RestrictsShipPlacement) us.intent;
			if (!rss.placementLimits(1, us).contains(realWorldX, worldY, worldW, worldH)) {
				return false;
			}
		}
		Airship ignore = (us.intent instanceof SingleShipIntent) ? ((SingleShipIntent) us.intent).getIgnoredOriginalShip(us) : null;
		if (us.combat != null) {
			for (Combat.Side side : us.combat.sides) {
				for (Airship ship : side.ships) {
					if (ship == ignore) { continue; }
					if (ship != shipToPlace && overlaps(ship, worldX, worldY, worldW, worldH)) { return false; }
				}
			}
		} else if (us.city != null) {
			Fleet garrison = us.wm.getGarrison(us.city);
			for (Airship bld : us.city.getDefences()) {
				if (bld == ignore) { continue; }
				if (bld != shipToPlace && overlaps(bld, worldX, worldY, worldW, worldH)) { return false; }
			}
			if (garrison != null) {
				for (Airship ship : garrison.actives) {
					if (ship == ignore) { continue; }
					if (ship != shipToPlace && overlaps(ship, worldX, worldY, worldW, worldH)) { return false; }
				}
			}
		} else if (us.getSetupFleet() != null) {
			for (Airship ship : us.getSetupFleet()) {
				if (ship == ignore) { continue; }
				if (ship != shipToPlace && overlaps(ship, worldX, worldY, worldW, worldH)) { return false; }
			}
		}
		return true;
	}
	
	private boolean overlaps(Airship ship, int worldX, int worldY, int worldW, int worldH) {
		return Rect2D.intersects(ship.getIntX(), ship.getIntY(), ship.getBBWidth(), ship.getBBHeight(), worldX, worldY, worldW, worldH);
	}
	
	@Override
	public void unscaledDraw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {
		Pt sz = d.textSize(_t("Left_click_to_place_right_click_to_pick_up"), AGame.FOUNT);
		d.text(_t("Left_click_to_place_right_click_to_pick_up"), AGame.FOUNT, (int) (sm.width - sz.x - 5), sm.height - sz.y - 3);
	}

	@Override
	public boolean click(Input in, Pt click, ScreenMode sm, UniScreen us) {
		if (!dragMode) {
			return doClick(in, click, sm, us);
		}
		return true;
	}
		
	public boolean doClick(Input in, Pt click, ScreenMode sm, UniScreen us) {
		if (absorbedClick(click, sm, us)) { return false; }
		if (ms < 100) { return true; }
		Airship ship = ship(us);
		int[] sp = localShipPt(click, us, mt == null ? 1 : mt.getW());
		if (mt == null) {
			ModuleType remMt = null;
			Tile t = ship.tileAt(sp[0], sp[1]);
			if (t != null) {
				remMt = t.module.type;
			}
			if (us.intent instanceof ChallengeEditShipIntent &&
				!((ChallengeEditShipIntent) us.intent).challenge.editTypes.contains(remMt))
			{
				return true;
			}
			if (ship.removeModuleAt(sp[0], sp[1]) != null) {
				in.play("remove", 1.0, 1.0 * us.g.volume, 0, 0);
				modified(us, /* big */ true);
				if (us.intent instanceof EditShipIntent) {
					EditShipIntent esi = (EditShipIntent) us.intent;
					if (!esi.hasRemovedModule) {
						Analytics.report("hasRemovedModule");
						esi.hasRemovedModule = true;
					}
				}
				return true;
			}
		} else {
			if (noOverlapsOrObstructions(ship, us, mt, sp[0], sp[1])) {
				Module m = ship.addModule(mt, sp[0], sp[1], us.panel(EditPalettePanel.class).lastArmourType, us.panel(EditPalettePanel.class).lastPaintType);
				if (mt.hasColoration()) {
					m.externalPaint = us.panel(EditPalettePanel.class).moduleAndDecalPaintSel;
				}
				double volume = StrictMath.min(1.0, StrictMath.sqrt(mt.getW() * mt.getH() * 0.2));
				in.play("place", 1.0 / volume, volume * us.g.volume, 0, 0);
				modified(us, /* big */ true);
				if (us.intent instanceof EditShipIntent) {
					EditShipIntent esi = (EditShipIntent) us.intent;
					if (!esi.hasPlacedModule) {
						Analytics.report("hasPlacedModule");
						esi.hasPlacedModule = true;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public int ticksSinceRightClick = 0;
		
	@Override
	public boolean rightClick(Input in, Pt click, ScreenMode sm, UniScreen us) {
		if (absorbedClick(click, sm, us)) { return false; }
		if (ms < 100) { return true; }
		Airship ship = ship(us);
		int[] sp = localShipPt(click, us, 1);
		if (us.intent instanceof ChallengeEditShipIntent &&
			!((ChallengeEditShipIntent) us.intent).challenge.editTypes.contains(ship.moduleTypeAt(sp[0], sp[1])))
		{
			return true;
		}
		Module victim = ship.removeModuleAt(sp[0], sp[1]);
		if (victim != null) {
			mt = victim.type;
			selectionIsDetachedFromPanel = true;
			in.play("paper_lift", 1.0, 1.0 * us.g.volume, 0, 0);
			modified(us, /* big */ true);
			if (us.intent instanceof EditShipIntent) {
				EditShipIntent esi = (EditShipIntent) us.intent;
				if (!esi.hasRemovedModule) {
					Analytics.report("hasRemovedModule");
					esi.hasRemovedModule = true;
				}
			}
		} else {
			if (ticksSinceRightClick > 2) {
				us.tool = UniScreen.NAVIGATE;
			}
		}
		ticksSinceRightClick = 0;
		return true;
	}

	@Override
	public boolean mouseDown(Input in, Pt click, ScreenMode sm, UniScreen us) {
		if (in.mouseDownButton() == 1) {
			return ship(us).modules.size() < 1 ? false : click(in, click, sm, us);
		} else {
			//return rightClick(in, click, sm, us);
			return super.mouseDown(in, click, sm, us);
		}
	}
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (us.intent instanceof EditShipIntent) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!esi.hasSelectedModule) {
				Analytics.report("hasSelectedModule");
				esi.hasSelectedModule = true;
			}
		}
		this.ms += ms;
		ticksSinceRightClick++;
		if (dragMode && in.mouseDown() == null) {
			doClick(in, in.cursor(), in.mode(), us);
			us.tool = UniScreen.NAVIGATE;
		}
		if (mt != null && !us.textInputOccurring()) {
			if (mt == panelTypeForCurrentType(us) && !selectionIsDetachedFromPanel) {
				// Our selection matches the palette panel, so we can change both in unison.
				if (Keys.check(in, "module_flip", "X", false) && canFlip(us)) {
					us.panel(EditPalettePanel.class).showFlippedModules = !us.panel(EditPalettePanel.class).showFlippedModules;
					mt = panelTypeForCurrentType(us);
				}
				if (Keys.check(in, "module_flip_vertical", "C", false) && canFlipVertically(us) && !us.textInputOccurring()) {
					us.panel(EditPalettePanel.class).showVerticallyFlippedModules = !us.panel(EditPalettePanel.class).showVerticallyFlippedModules;
					mt = panelTypeForCurrentType(us);
				}
				if (Keys.check(in, "module_variant", "N", false) && hasVariant(us) && !us.textInputOccurring()) {
					us.panel(EditPalettePanel.class).incrementModuleIndex(mt.getVariantGroupHeadOrThis().getSymmetryGroupHead());
					mt = panelTypeForCurrentType(us);
				}
			} else {
				if (Keys.check(in, "module_flip", "X", false) && mt.getFlippedIfAvailable() != null) {
					mt = mt.getFlippedIfAvailable();
				}
				if (Keys.check(in, "module_flip_vertical", "C", false) && mt.getVerticalFlippedIfAvailable() != null) {
					mt = mt.getVerticalFlippedIfAvailable();
				}
				if (Keys.check(in, "module_variant", "N", false) && hasVariant(us) && mt.getVariantGroupHeadOrThis().isVariantGroupHead()) {
					ModuleType vgh = mt.getVariantGroupHeadOrThis();
					int newIndex = (vgh.getVariants().indexOf(mt) + 1) % vgh.getVariants().size();
					if (newIndex > -1 && newIndex < vgh.getVariants().size()) {
						mt = vgh.getVariants().get(newIndex);
					}
				}
			}
		}
	}
	
	private ModuleType panelTypeForCurrentType(UniScreen us) {
		EditPalettePanel p = us.panel(EditPalettePanel.class);
		ModuleType mt2 = mt.getVariantGroupHeadOrThis().getSymmetryGroupHead();
		if (mt2.isVariantGroupHead()) {
			mt2 = mt2.getVariants().get(p.getVariantIndex(mt2, mt2.getVariants()));
		}
		if (p.showFlippedModules && mt2.getFlippedIfAvailable() != null) {
			mt2 = mt2.getFlippedIfAvailable();
		}
		if (p.showVerticallyFlippedModules && mt2.getVerticalFlippedIfAvailable() != null) {
			mt2 = mt2.getVerticalFlippedIfAvailable();
		}
		return mt2;
	}
}
