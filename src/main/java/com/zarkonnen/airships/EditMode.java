package com.zarkonnen.airships;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.geom.Polygon;

public enum EditMode implements ScrollBar.ScrollElementAdapter<Object> {
	MODULES("M", "edit_mode_MODULES", false) {
		public final Img FIXER = new Img("ui", 480, 416, 16, 16, false);
		public final Img FIXER_BG = new Img("ui", 496, 416, 16, 16, false);
		public final Img FLIP = new Img("ui", 304, 512, 16, 16, false);
		public final Img FLIP_V = new Img("ui", 336, 512, 16, 16, false);
		
		@Override
		public boolean allowSelect(UniScreen us) {
			return us.intent instanceof EditShipIntent && ((EditShipIntent) us.intent).canSelectModulesAndDecals();
		}
		
		@Override
		public void dragComplete(UniScreen us, Input in, double startX, double startY, double endX, double endY) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!allowSelect(us)) { return; }
			Airship ship = esi.getShip(us);
			EditSelectionOverlay eso = us.shipOverlay(EditSelectionOverlay.class);
			eso.shiftX = 0;
			eso.shiftY = 0;
			eso.fail = false;
			int sx = (int) Math.floor((startX - ship.getX()) / AGame.SGS);
			int sy = (int) Math.floor((startY - ship.getY()) / AGame.SGS);
			Module startM = ship.moduleAt(ship.gridXToWorldX(sx, 1), sy);
			if (startM == null || !esi.selectedModules.contains(startM) || esi.selectedModules.containsAll(ship.modules)) {
				if (endX < startX) {
					double tmp = startX;
					startX = endX;
					endX = tmp;
				}
				if (endY < startY) {
					double tmp = startY;
					startY = endY;
					endY = tmp;
				}
				int smx = ship.gridXToWorldX((int) Math.floor((startX - ship.getX()) / AGame.SGS), 1);
				int smy = (int) Math.floor((startY - ship.getY()) / AGame.SGS);
				int emx = ship.gridXToWorldX((int) Math.floor((endX - ship.getX()) / AGame.SGS), 1);
				int emy = (int) Math.floor((endY - ship.getY()) / AGame.SGS);
				if (emx < smx) {
					int tmp = smx;
					smx = emx;
					emx = tmp;
				}
				HashSet<Module> modules = new HashSet<Module>();
				for (int y = smy; y <= emy; y++) {
					for (int x = smx; x <= emx; x++) {
						Module m = ship.moduleAt(x, y);
						if (m != null) {
							modules.add(m);
						}
					}
				}
				if (!in.keyDown("LSHIFT")) {
					esi.selectedModules.clear();
				}
				esi.selectedModules.addAll(modules);
			} else {
				int ex = (int) Math.floor((endX - ship.getX()) / AGame.SGS);
				int ey = (int) Math.floor((endY - ship.getY()) / AGame.SGS);
				
				int dx = ship.flipped ? sx - ex : ex - sx;
				int dy = ey - sy;
				
				if (dx == 0 && dy == 0) { return; }
								
				ArrayList<MovingModule> mms = new ArrayList<MovingModule>();
				for (Module m : esi.selectedModules) {
					MovingModule mm = new MovingModule(ship, m.x, m.y);
					if (!mm.canAdd(ship, mm.x + dx, mm.y + dy, esi.selectedModules)) {
						return;
					}
					mms.add(mm);
				}
				
				Module anchor = null;
				for (Module m : ship.modules) {
					if (!esi.selectedModules.contains(m)) {
						anchor = m;
						break;
					}
				}
				if (anchor == null) {
					System.out.println("no anchor found");
					return;
				}
				int anchorX = anchor.x;
				int anchorY = anchor.y;
				for (Module m : esi.selectedModules) {
					ship.quickRemoveModule(m);
				}
				ship.repair(/* resetXP */ true);
				dx += anchor.x - anchorX;
				dy += anchor.y - anchorY;
				int[] shift = new int[] { 0, 0 };
				ArrayList<Module> newModules = new ArrayList<Module>();
				for (MovingModule mm : mms) {
					Module m = mm.add(ship, mm.x + dx, mm.y + dy, esi.selectedModules, shift);
					if (m != null) {
						newModules.add(m);
						dx += shift[0];
						dy += shift[1];
					}
				}
				ship.repair(/* resetXP */ true);
				esi.selectedModules.clear();
				esi.selectedModules.addAll(newModules);
				esi.modified(us, true);
			}
		}
		
		@Override
		public boolean showDragRect(UniScreen us, double startX, double startY, double endX, double endY) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			Airship ship = esi.getShip(us);
			EditSelectionOverlay eso = us.shipOverlay(EditSelectionOverlay.class);
			int sx = (int) Math.floor((startX - ship.getX()) / AGame.SGS);
			int sy = (int) Math.floor((startY - ship.getY()) / AGame.SGS);
			Module m = ship.moduleAt(ship.gridXToWorldX(sx, 1), sy);
			if (m == null || !esi.selectedModules.contains(m) || esi.selectedModules.containsAll(ship.modules)) {
				eso.shiftX = 0;
				eso.shiftY = 0;
				eso.fail = false;
				return true;
			} else {
				int ex = (int) Math.floor((endX - ship.getX()) / AGame.SGS);
				int ey = (int) Math.floor((endY - ship.getY()) / AGame.SGS);
				eso.shiftX = ex - sx;
				eso.shiftY = ey - sy;
				eso.fail = false;
				int dx = ship.flipped ? sx - ex : ex - sx;
				int dy = ey - sy;
				for (Module sm : esi.selectedModules) {
					MovingModule mm = new MovingModule(ship, sm.x, sm.y);
					if (!mm.canAdd(ship, mm.x + dx, mm.y + dy, esi.selectedModules)) {
						eso.fail = true;
						break;
					}
				}
				return false;
			}
		}
		
		@Override
		public void tick(UniScreen us, Input in) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!allowSelect(us)) {
				esi.selectedModules.clear();
			}
			Airship ship = esi.getShip(us);
			if (!us.textInputOccurring() && us.tool == UniScreen.NAVIGATE && !esi.selectedModules.isEmpty()) {
				if (in.keyPressed("DELETE") || in.keyPressed("BACK")) {
					for (Module m : esi.selectedModules) {
						ship.removeModule(m);
					}
					esi.selectedModules.clear();
					esi.modified(us, true);
				} else if (Keys.check(in, "edit_duplicate", "H", false)) {
					int selXMin = 100000;
					int selXMax = -100000;
					int selYMin = 100000;
					int selYMax = -100000;
					for (Module m : esi.selectedModules) {
						selXMin = Math.min(m.x, selXMin);
						selXMax = Math.max(m.x + m.type.getW(), selXMax);
						selYMin = Math.min(m.y, selYMin);
						selYMax = Math.max(m.y + m.type.getH(), selYMax);
					}
					int selW = selXMax - selXMin;
					int selH = selYMax - selYMin;
					int[][] candidates = {
						{ 1, 0 },
						{ -1, 0 },
						{ 0, 1 },
						{ 0, -1 },
						{ 1, 1 },
						{ -1, 1 },
						{ 1, -1 },
						{ -1, -1 }
					};
					ArrayList<MovingModule> mms = new ArrayList<MovingModule>();
					for (Module sm : esi.selectedModules) {
						mms.add(new MovingModule(ship, sm.x, sm.y));
					}
					int mmss = mms.size();
					for (int extraD = 0; extraD < 40; extraD++) { lp: for (int ci = 0; ci < candidates.length; ci++) {
						int dx = candidates[ci][0] * (selW + extraD);
						int dy = candidates[ci][1] * (selH + extraD);
						for (int mmi = 0; mmi < mmss; mmi++) {
							MovingModule mm = mms.get(mmi);
							if (!mm.canAdd(ship, mm.x + dx, mm.y + dy, Collections.EMPTY_LIST)) {
								continue lp;
							}
						}
						esi.selectedModules.clear();
						int[] shift = {0, 0};
						for (int mmi = 0; mmi < mmss; mmi++) {
							MovingModule mm = mms.get(mmi);
							Module m = mm.add(ship, mm.x + dx, mm.y + dy, Collections.EMPTY_LIST, shift);
							if (m != null) {
								esi.selectedModules.add(m);
								dx += shift[0];
								dy += shift[1];
							}
						}
						ship.repair(/* resetXP */ true);
						esi.modified(us, true);
						return;
					}}
				}
			}
		}
		
		@Override
		public void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!us.textInputOccurring() && us.tool == UniScreen.NAVIGATE && !esi.selectedModules.isEmpty()) {
				Pt sz = d.textSize(_t("Press_x_to_duplicate_and_backspace_or_delete_to_remove", Keys.getText("edit_duplicate", "H", false)), AGame.FOUNT);
				d.text(_t("Press_x_to_duplicate_and_backspace_or_delete_to_remove", Keys.getText("edit_duplicate", "H", false)), AGame.FOUNT, (int) (sm.width - sz.x - 5), sm.height - sz.y - 3);
			}
			EditSelectionOverlay eso = us.shipOverlay(EditSelectionOverlay.class);
			if ((eso.shiftX != 0 || eso.shiftY != 0) && !esi.selectedModules.isEmpty()) {
				d.state.setCursor("DRAG", null);
			} else if (us.tool == UniScreen.NAVIGATE && cursor.y > MyDraw.TOP_BAR_H && cursor.x > us.panel(EditInfoPanel.class).myWidth) {
				double cx = us.screenToWorldX(cursor.x);
				double cy = us.screenToWorldY(cursor.y);
				Airship ship = esi.getShip(us);
				for (Module m : esi.selectedModules) {
					if (!ship.modules.contains(m)) { continue; }
					double decX = ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
					double decY = ship.getY() + m.y * AGame.SGS;
					if (cx > decX && cy > decY && cx < decX + m.type.getH() * AGame.SGS && cy < decY + m.type.getH() * AGame.SGS) {
						d.state.setCursor("DRAG", null);
						break;
					}
				}
			}
		}
		
		@Override
		public void click(UniScreen us, Input in, Pt p) {
			if (!allowSelect(us)) { return; }
			EditShipIntent esi = (EditShipIntent) us.intent;
			Airship ship = esi.getShip(us);
			int mx = ship.gridXToWorldX((int) Math.floor((p.x - ship.getX()) / AGame.SGS), 1);
			int my = (int) Math.floor((p.y - ship.getY()) / AGame.SGS);
			Module m = ship.moduleAt(mx, my);
			if (m != null) {
				if (in.keyDown("LSHIFT")) {
					if (esi.selectedModules.contains(m)) {
						esi.selectedModules.remove(m);
					} else {
						esi.selectedModules.add(m);
					}
				} else {
					esi.selectedModules.clear();
					esi.selectedModules.add(m);
				}
			} else if (!in.keyDown("LSHIFT")) {
				esi.selectedModules.clear();
			}
		}
		
		private boolean canReplace(Entry ee) {
			Entry<ModuleType> e = (Entry<ModuleType>) ee;
			return e.o.canReplace(e.p.replaceModuleSrc);
		}
		
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			Entry e = (Entry) t;
			if (e.o == null || e.o instanceof ModuleCategory) {
				return 16 + MyDraw.SCROLL_EL_SPACING;
			}
			int baseScale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			final Entry<ModuleType> emt = (Entry<ModuleType>) t;
			double scale = baseScale;
			if (emt.o.getW() > 3) {
				scale = baseScale * 3.0 / emt.o.getW();
			}
			if (e.o instanceof ModuleType && (canFlip((Entry<ModuleType>) e) || canFlipVertically((Entry<ModuleType>) e) || canVary((Entry<ModuleType>) e) || canReplace(e))) {
				return Math.max(
						(int) (((ModuleType) e.o).getH() * AGame.SGS * scale),
						Math.max(AGame.FOUNT.lineHeight, MyDraw.ICON_BUTTON_SZ)
				)+ MyDraw.SCROLL_EL_SPACING;
			}
			return (int) Math.max(AGame.FOUNT.lineHeight, (((ModuleType) e.o).getH() * AGame.SGS * scale)) + MyDraw.SCROLL_EL_SPACING;
		}
		
		@Override
		public void stepSelect(int delta, UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			int currentIndex = -1;
			if (us.tool instanceof PlaceModuleTool) {
				for (int i = 0; i < l.size(); i++) {
					if (!(l.get(i) instanceof Entry)) { continue; }
					Entry e = (Entry) l.get(i);
					if (e.o == ((PlaceModuleTool) us.tool).mt) {
						currentIndex = i;
					}
				}
			}
			int newIndex = (currentIndex + delta + l.size()) % l.size();
			us.tool = new PlaceModuleTool((ModuleType) ((Entry) l.get(newIndex)).o, false);
		}
		
		@Override
		public void selectFirst(UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			for (Object o : l) {
				if (o instanceof Entry && ((Entry) o).o instanceof ModuleType) {
					us.tool = new PlaceModuleTool((ModuleType) ((Entry) o).o, false);
					return;
				}
			}
		}

		@Override
		public void draw(Object t, MyDraw d, int x, int y, int width) {
			int baseScale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			final Entry entry = (Entry) t;
			boolean selected = entry.us.tool instanceof PlaceModuleTool && entry.p.modSel(entry.us) == entry.o;
			if (entry.o instanceof ModuleCategory) {
				final Entry<ModuleCategory> e = (Entry<ModuleCategory>) t;
				final boolean open = e.p.openCategories.contains(e.o);
				d.hook(x, y, width, 16 + MyDraw.SCROLL_EL_SPACING, new Hook(Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						if (open) {
							e.p.openCategories.remove(e.o);
						} else {
							e.p.openCategories.add(e.o);
						}
					}
				});
				d.blit(open ? MyDraw.TRIANGLE_OPEN : MyDraw.TRIANGLE_CLOSED, x + 4, y);
				d.text(e.o.getName(), AGame.FOUNT, x + 4 + MyDraw.TRIANGLE_OPEN.srcWidth + MyDraw.SCROLL_EL_SPACING, y);
			} else {
				d.shift(x, y);
				final Entry<ModuleType> e = (Entry<ModuleType>) t;
				double scale = baseScale;
				if (e.o.getW() > 3) {
					scale = baseScale * 3.0 / e.o.getW();
				}
				d.scale(scale, scale);
				for (ExternalApp ea : e.o.getExternalApps(e.ship.currentBonuses, false, false)) {
					ea.app.draw(d, (e.ship.flipped ? (e.o.getW() - ea.dx - ea.app.width()) : ea.dx) * AGame.SGS, ea.dy * AGame.SGS, 0, e.ship.flipped);
				}
				if (!e.o.isExternal() && e.o.getArmourType() == null) {
					ArmourType backArmour = e.p.armSel(e.us);
					if (backArmour == null) { backArmour = ArmourType.ofName("MED_WOOD"); }
					if (e.o.getArmourType() != null) {
						backArmour = e.o.getArmourType();
					}
					if (backArmour != null && !backArmour.damagedApps.get(e.ship.currentBonuses).isEmpty()) {
						if (e.o.getArmourMask(e.ship.currentBonuses) != null) {
							for (int gy = 0; gy < e.o.getH(); gy++) { for (int gx = 0; gx < e.o.getW(); gx++) {
								TileMask tm = e.o.getTileMasks(e.ship.currentBonuses)[gy][e.ship.flipped ? (e.o.getTileMasks(e.ship.currentBonuses)[0].length - gx - 1) : gx];
								if (e.ship.flipped) {
									tm = tm.flipped;
								}
								backArmour.damagedApps.get(e.ship.currentBonuses).get(0).maskedDrawFallback(d, gx * AGame.SGS, gy * AGame.SGS, AGame.SGS, AGame.SGS, 0, new Clr(91, 91, 91), false, tm);
							}}
						} else {
							for (int gy = 0; gy < e.o.getH(); gy++) { for (int gx = 0; gx < e.o.getW(); gx++) {
								backArmour.damagedApps.get(e.ship.currentBonuses).get(0).draw(d, gx * AGame.SGS, gy * AGame.SGS, 0, new Clr(91, 91, 91), false);
							}}
						}
					}
				}
				if (e.o.weaponAppearance(e.ship.currentBonuses) != null && (e.o.weaponAppearance(e.ship.currentBonuses).barrel != null || e.o.weaponAppearance(e.ship.currentBonuses).barrelAnimation != null)) {
					ModuleType mt = e.o;
					Airship ship = e.ship;
					double angle =
						ship.flipped
						? -mt.getFireArc(e.ship.currentBonuses).getMiddle().radians
						: mt.getFireArc(e.ship.currentBonuses).getMiddle().radians;
					if (mt.isFlipped()) {
						angle += StrictMath.PI;
					}
					WeaponAppearance wa = mt.weaponAppearance(e.ship.currentBonuses);
					Img barrel = ship.flipped ? wa.flippedbarrel : wa.barrel;
					if (wa.barrelAnimation != null) {
						barrel = ship.flipped ? wa.flippedBarrelAnimation.frames.get(0) : wa.barrelAnimation.frames.get(0);
					}
					Pt offset = ship.flipped ? wa.flippedBarrelOffset : wa.barrelOffset;
					if (barrel.machineImgCache != null) {
						((Image) barrel.machineImgCache).setFilter(Image.FILTER_NEAREST);
					}
					d.blit(barrel, (int) offset.x, (int) offset.y, angle);
					//Img barrel = e.o.isFlipped() ? e.o.weaponAppearance().flippedbarrel : e.o.weaponAppearance().barrel;
					//double barrelAngle = e.o.getFireArc().getMiddle().radians;
					//d.blit(barrel, x + e.o.weaponAppearance().barrelOffset.x, y + e.o.weaponAppearance().barrelOffset.y, barrelAngle);
				}
				e.o.getApp(e.ship.currentBonuses, 1).draw(d, 0, 0, 0, e.ship.flipped);
				d.resetTransforms();
				
				//d.drawWoodGrain(6 + x + AGame.SGS * 3 * scale, y, width, e.o.getH() * AGame.SGS * scale + 12 + MyDraw.PROGRESS_BAR_H);
				int hookStart = Math.max(e.p.listTop, y);
				int hookEnd = Math.min(e.p.listBottom, y + e.o.getH() * AGame.SGS + MyDraw.SCROLL_EL_SPACING + 8);
				d.hook(x, y, width, hookEnd - hookStart, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						if (in.clicked() == null && (e.us.tool instanceof PlaceModuleTool) && ((PlaceModuleTool) e.us.tool).dragMode) {
							return;
						}
						e.us.tool = new PlaceModuleTool(e.o, in.clicked() == null);
						e.p.searchSelectionMade = true;
					}
				});
				boolean fixer = e.ship.fixers.contains(e.o);
				String name = e.o.getName();
				int totalCost = e.o.getCost(e.ship.currentBonuses);
				if (!e.o.isExternal()) {
					totalCost += e.o.getW() * e.o.getH() * e.p.lastArmourType.getCost(e.ship.currentBonuses);
				}
				if (e.o.getArmourType() != null) {
					totalCost += e.o.getW() * e.o.getH() * e.o.getArmourType().getCost(e.ship.currentBonuses);
				}
				totalCost = (int) Math.ceil(totalCost * EmpireStat.SHIP_TYPE_COST_MULTIPLIER.get(e.ship.type).get(e.ship.currentBonuses));
				String cost = "$" + totalCost;
				int textMaxW = width - 3 * AGame.SGS * baseScale - MyDraw.SCROLL_EL_SPACING - 4 - (int) d.textSize(cost, AGame.FOUNT).x - MyDraw.UI_SPACING;

				if (d.textSize(name, AGame.FOUNT).x > textMaxW) {
					String name2 = name.substring(0, name.length() - 4);
					while (d.textSize(name2 + "...", AGame.FOUNT).x > textMaxW) {
						name2 = name2.substring(0, name2.length() - 1);
					}
					name = name2 + "...";
				}
				d.text((selected ? MyDraw.SELECTED_C : "") + name, AGame.FOUNT, x + 3 * AGame.SGS * baseScale + MyDraw.SCROLL_EL_SPACING + 4, y);
				boolean explainBonuses = e.us.intent instanceof EditShipIntent && ((EditShipIntent) e.us.intent).getPlayerEmpire() != null;
				if (fixer) {
					int fixerX = x + 3 * AGame.SGS * baseScale + MyDraw.SCROLL_EL_SPACING + 4 + (int) d.textSize(name, AGame.FOUNT).x + MyDraw.SCROLL_EL_SPACING;
					d.blit(FIXER_BG, new Clr(187, 66, 29), fixerX, y);
					d.blit(FIXER, fixerX, y);
					d.tooltip(x, y, width, e.o.getH() * AGame.SGS + MyDraw.SCROLL_EL_SPACING + 4, _t("module_fixer_help") + "\n\n" + e.o.getDescription(e.ship.currentBonuses, e.ship.type, explainBonuses));
				} else {
					d.tooltip(x, y, width, e.o.getH() * AGame.SGS + MyDraw.SCROLL_EL_SPACING + 4, e.o.getDescription(e.ship.currentBonuses, e.ship.type, explainBonuses));
				}
				
				d.text(cost, AGame.FOUNT, x + width - (int) d.textSize(cost, AGame.FOUNT).x, y);
				int x2 = x + width - MyDraw.ICON_BUTTON_SZ - (int) d.textSize("$1000", AGame.FOUNT).x;
				if (canFlip(e)) {
					d.iconButton(x2, y, FLIP, new Runnable() {
						@Override
						public void run() {
							entry.us.tool = new PlaceModuleTool(e.o.getFlippedIfAvailable(), false);
							e.p.showFlippedModules = !e.p.showFlippedModules;
						}
					}, true);
					d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Flip_module"));
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (canFlipVertically(e)) {
					d.iconButton(x2, y, FLIP_V, new Runnable() {
						@Override
						public void run() {
							entry.us.tool = new PlaceModuleTool(e.o.getVerticalFlippedIfAvailable(), false);
							e.p.showVerticallyFlippedModules = !e.p.showVerticallyFlippedModules;
						}
					}, true);
					d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Flip_module_vertically"));
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (canVary(e)) {
					d.iconButton(x2, y, e.o.getVariantGroupHeadOrThis().variantType().icon, new Runnable() {
						@Override
						public void run() {
							ModuleType head = e.o.getVariantGroupHeadOrThis().getSymmetryGroupHead();
							e.p.incrementModuleIndex(head);
							ArrayList<ModuleType> variants = e.o.getVariantGroupHeadOrThis().getVariants();
							entry.us.tool = new PlaceModuleTool(variants.get(e.p.getVariantIndex(head, variants)), false);
						}
					}, true);
					d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("variantType_" + e.o.getVariantGroupHeadOrThis().variantType().name));
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (e.p.replaceModuleSrc != null && e.o.canReplace(e.p.replaceModuleSrc)) {
					d.iconButton(x2, y, REPLACE, new Runnable() {
						@Override
						public void run() {
							e.ship.replace(e.p.replaceModuleSrc, e.o);
							((SingleShipIntent) e.us.intent).modified(e.us, true);
							e.p.replaceModuleSrc = null;
						}
					}, true);
					d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Replace_x", e.p.replaceModuleSrc.getName()));
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
			}
		}
		
		private boolean canVary(Entry<ModuleType> e) {
			return e.o.isVariantGroupMember() && !(e.us.intent instanceof ChallengeEditShipIntent);
		}
		
		private boolean canFlip(Entry<ModuleType> e) {
			if (e.o.getFlippedIfAvailable() == null) { return false; }
			if (e.us.intent instanceof ChallengeEditShipIntent) {
				ChallengeEditShipIntent cesi = (ChallengeEditShipIntent) e.us.intent;
				return cesi.challenge.editTypes.contains(e.o.getFlippedIfAvailable());
			}
			return true;
		}
		
		private boolean canFlipVertically(Entry<ModuleType> e) {
			if (e.o.getVerticalFlippedIfAvailable() == null) { return false; }
			if (e.us.intent instanceof ChallengeEditShipIntent) {
				ChallengeEditShipIntent cesi = (ChallengeEditShipIntent) e.us.intent;
				return cesi.challenge.editTypes.contains(e.o.getVerticalFlippedIfAvailable());
			}
			return true;
		}

		@Override
		public boolean hasRemove() {
			return true;
		}
		
		@Override
		public void doToggleRemove(final UniScreen us, final EditPalettePanel p) {
			final boolean selected = us.tool instanceof PlaceModuleTool && p.modSel(us) == null;
			if (selected) {
				us.tool = UniScreen.NAVIGATE;
			} else {
				us.tool = new PlaceModuleTool(null, false);
			}
		}

		@Override
		public void drawRemove(MyDraw d, int x, int y, int width, final UniScreen us, final EditPalettePanel p) {
			final boolean selected = us.tool instanceof PlaceModuleTool && p.modSel(us) == null;
			d.toggle(x, y, width, _t("Remove_module"), Keys.getText("edit_remove", "K", false), new InputRunnable() {
				@Override
				public void run(Input in) {
					if (selected) {
						us.tool = UniScreen.NAVIGATE;
					} else {
						us.tool = new PlaceModuleTool(null, false);
					}
				}
			}, selected, true);
		}
		
		class ModuleComparator implements Comparator<ModuleType> {
			@Override
			public int compare(ModuleType a, ModuleType b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		}

		@Override
		public List<Object> getList(UniScreen us, EditPalettePanel p, Airship ship) {
			ArrayList l = new ArrayList();
			HashSet<ModuleType> restrict = new HashSet<ModuleType>();
			if (us.intent instanceof ChallengeEditShipIntent) {
				Challenge c = ((ChallengeEditShipIntent) us.intent).challenge;
				restrict = c.editTypes;
			} else {
				restrict.addAll(Loadable.all(ModuleType.class));
			}
			
			EditShipIntent esi = (EditShipIntent) us.intent;
			
			Empire owner = p.owner(us);
						
			if (p.searching && !p.searchField.getText().isEmpty()) {
				ArrayList<ModuleType> mtl = SearchMatcher.match(Loadable.all(ModuleType.class), new ModuleComparator(), p.searchField.getText());
				ArrayList<ModuleType> mtl2 = new ArrayList<ModuleType>();
				for (ModuleType mt : mtl) {
					mt = mt.getVariantGroupHeadOrThis().getSymmetryGroupHead();
					if (!mtl2.contains(mt)) {
						mtl2.add(mt);
					}
				}
				for (ModuleType mt : mtl2) {
					if (owner != null && mt.getRequired() != null && !owner.bonuses.contains[mt.getRequired().ordinal()] && mt.getRequired().getTech() == null) {
						continue;
					}
					if (mt.getRequired() != null && !esi.hasTechBonus(mt.getRequired())) {
						continue;
					}
					
					if (mt.isVariantGroupHead()) {
						mt = mt.getVariants().get(p.getVariantIndex(mt, mt.getVariants()));
					}
					if (p.showFlippedModules && mt.getFlippedIfAvailable() != null) {
						mt = mt.getFlippedIfAvailable();
					}
					if (p.showVerticallyFlippedModules && mt.getVerticalFlippedIfAvailable() != null) {
						mt = mt.getVerticalFlippedIfAvailable();
					}
					if (!mt.availableFor(ship.type)) { continue; }
					if (!restrict.contains(mt)) { continue; }
					if (mt.hidden()) { continue; }
					l.add(new Entry(mt, us, p, ship));
				}
			} else {
				for (ModuleCategory mc : Loadable.all(ModuleCategory.class)) {
					boolean empty = true;
					for (ModuleType mt : mc.getContents()) {
						if (!mt.availableFor(ship.type)) { continue; }
						if (!restrict.contains(mt)) { continue; }
						if (owner != null && mt.getRequired() != null && !owner.bonuses.contains[mt.getRequired().ordinal()] && mt.getRequired().getTech() == null) {
							continue;
						}
						if (mt.getRequired() != null && !esi.hasTechBonus(mt.getRequired())) {
							continue;
						}
						empty = false;
						break;
					}
					if (empty) { continue; }
					l.add(new Entry(mc, us, p, ship));
					if (p.openCategories.contains(mc)) {
						for (ModuleType mt : mc.getContents()) {
							if (mt.isSymmetryGroupMember() && !mt.isSymmetryGroupHead()) { continue; }
							if (mt.isVariantGroupMember() && !mt.isVariantGroupHead()) { continue; }
							if (owner != null && mt.getRequired() != null && !owner.bonuses.contains[mt.getRequired().ordinal()] && mt.getRequired().getTech() == null) {
								continue;
							}
							if (mt.getRequired() != null && !esi.hasTechBonus(mt.getRequired())) {
								continue;
							}
							if (mt.isVariantGroupHead()) {
								mt = mt.getVariants().get(p.getVariantIndex(mt, mt.getVariants()));
							}
							if (p.showFlippedModules && mt.getFlippedIfAvailable() != null) {
								mt = mt.getFlippedIfAvailable();
							}
							if (p.showVerticallyFlippedModules && mt.getVerticalFlippedIfAvailable() != null) {
								mt = mt.getVerticalFlippedIfAvailable();
							}
							if (!mt.availableFor(ship.type)) { continue; }
							if (!restrict.contains(mt)) { continue; }
							if (mt.hidden()) { continue; }
							l.add(new Entry(mt, us, p, ship));
						}
					}
				}
			}
			return l;
		}
		
		private ArrayList<Module> selectedPaintableModules(UniScreen us) {
			ArrayList<Module> l = new ArrayList<Module>();
			EditShipIntent esi = (EditShipIntent) us.intent;
			Airship ship = esi.getShip(us);
			for (Module m : esi.selectedModules) {
				if (m.type.hasColoration() && ship.modules.contains(m)) {
					l.add(m);
				}
			}
			return l;
		}
		
		boolean prevHadBottom;
		
		@Override
		public int bottomHeight(MyDraw d, int width, UniScreen us, EditPalettePanel p) {
			ModuleType t = p.modSel(us);
			// Don't show this in drag mode. It can't be used anyway, and it causes a weird bug where it pops up and breaks click to select.
			if (us.tool instanceof PlaceModuleTool && ((PlaceModuleTool) us.tool).dragMode) {
				if (prevHadBottom) {
					return bh(d, width, us, p);
				} else {
					return 0;
				}
			}

			if ((us.tool instanceof PlaceModuleTool && !(us.intent instanceof ChallengeEditShipIntent) && t != null && t.hasColoration()) ||
				(us.tool == UniScreen.NAVIGATE && !selectedPaintableModules(us).isEmpty()))
			{
				prevHadBottom = true;
				return bh(d, width, us, p);
			} else {
				prevHadBottom = false;		
				return 0;
			}
		}
		
		private int bh(MyDraw d, int width, UniScreen us, EditPalettePanel p) {
			int rowCapacity = StrictMath.max(1, (width + MyDraw.BUTTON_SPACING) / (swatchSize() + MyDraw.BUTTON_SPACING));
			int numRows = (int) StrictMath.ceil((PaintType.values().size() + 1) * 1.0 / rowCapacity);
			return MyDraw.BUTTON_SPACING + (swatchSize() + MyDraw.BUTTON_SPACING) * numRows - MyDraw.BUTTON_SPACING;
		}
		
		@Override
		public void drawBottom(MyDraw d, final int x, int y, int width, final UniScreen us, final EditPalettePanel p) {
			ModuleType t = p.modSel(us);
			// Ensure we don't accidentally try to draw something that is not drawable.
			if (!
				((us.tool instanceof PlaceModuleTool && !(us.intent instanceof ChallengeEditShipIntent) && t != null && t.hasColoration()) ||
				(us.tool == UniScreen.NAVIGATE && !selectedPaintableModules(us).isEmpty()))
				)
			{
				return;
			}
			int bh = bottomHeight(d, width, us, p);
			if (bh > 0) {
				d.hook(x, y, width, bh, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						// Swallow clicks so they don't trigger the hook from a module.
					}
				});
			} else {
				return;
			}
			y += MyDraw.BUTTON_SPACING;
			int x2 = x;
			
			HashSet<PaintType> selectedPaintTypes = new HashSet<PaintType>();
			if (us.tool instanceof PlaceModuleTool) {
				selectedPaintTypes.add(p.moduleAndDecalPaintSel);
			} else {
				for (Module m : selectedPaintableModules(us)) {
					selectedPaintTypes.add(m.externalPaint);
				}
			}
			
			ArrayList<PaintType> pts = PaintType.values();
			for (int pti = -1; pti < pts.size(); pti++) {
				final PaintType paintType = pti == -1 ? null : pts.get(pti);
				if (x2 + swatchSize() > x + width) {
					y += swatchSize() + MyDraw.BUTTON_SPACING;
					x2 = x;
				}
				Clr c;
				if (us.tool instanceof PlaceModuleTool) {
					c = paintType == null ? t.colorationDefault() : t.externalSubColorByPaintIndex[paintType.getPaintType(us.getBestOverallCOA()).ordinal()];
				} else {
					c = paintType == null ? Clr.GREY : paintType.getTint(us.getBestOverallCOA());
				}
				if (c == null) {
					String report = "No paint set. ";
					if (paintType == null) {
						report += "paintType: null";
					} else {
						report += "paintType: " + paintType.name;
					}
					report += " moduleType: " + t.name;
					AirshipGame.report(report);
					
					c = Clr.GREY;
				}
				if (selectedPaintTypes.contains(paintType)) {
					d.rect(c, x2 - MyDraw.BUTTON_SPACING / 2, y - MyDraw.BUTTON_SPACING / 2, swatchSize() + MyDraw.BUTTON_SPACING, swatchSize() + MyDraw.BUTTON_SPACING);
				} else {
					d.rect(c, x2, y, swatchSize(), swatchSize());
				}
				if (paintType != null && paintType.isArmsBased()) {
					d.blit(SHIELD_ICON_OUTLINE, Clr.BLACK, x2 + swatchSize() / 2 - 8, y + swatchSize() / 2 - 8);
					d.blit(SHIELD_ICON, x2 + swatchSize() / 2 - 8, y + swatchSize() / 2 - 8);
				}
				d.hook(x2, y, swatchSize(), swatchSize(), new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						if (us.tool instanceof PlaceModuleTool) {
							p.moduleAndDecalPaintSel = paintType;
						} else {
							for (Module m : selectedPaintableModules(us)) {
								m.externalPaint = paintType;
							}
							((EditShipIntent) us.intent).modified(us, true);
						}
					}
				});
				d.tooltip(x2, y, swatchSize(), swatchSize(), paintType == null ? _t("Base_Colour") : paintType.getName());
				x2 += swatchSize() + MyDraw.BUTTON_SPACING;
			}
		}
	},
	ARMOUR("R", "edit_mode_ARMOUR", true) {	
		private final Img windowImg = new Img("ui", 560, 400, 16, 16, false);
		
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			int scale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			return Math.max(AGame.SGS * scale, MyDraw.ICON_BUTTON_SZ) + MyDraw.SCROLL_EL_SPACING;
		}
		
		@Override
		public void stepSelect(int delta, UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			int currentIndex = -1;
			if (us.tool instanceof PlaceArmourTool) {
				for (int i = 0; i < l.size(); i++) {
					if (!(l.get(i) instanceof Entry)) { continue; }
					Entry e = (Entry) l.get(i);
					if (e.o == ((PlaceArmourTool) us.tool).at) {
						currentIndex = i;
					}
				}
			}
			int newIndex = (currentIndex + delta + l.size()) % l.size();
			us.tool = new PlaceArmourTool((ArmourType) ((Entry) l.get(newIndex)).o);
		}
		
		@Override
		public void selectFirst(UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			us.tool = new PlaceArmourTool((ArmourType) ((Entry) l.get(0)).o);
		}
		
		private boolean canVary(Entry<ArmourType> e) {
			return e.o.isVariantGroupMember();
		}

		@Override
		public void draw(Object t, MyDraw d, int x, int y, int width) {
			int scale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			d.drawWoodGrain(x, y, width, 20 + MyDraw.SCROLL_EL_SPACING);
			final Entry<ArmourType> e = (Entry<ArmourType>) t;
			if (e.o == null) {
				boolean selected = e.us.tool instanceof PlaceArmourTool && ((PlaceArmourTool) e.us.tool).at == null;
				if (windowImg.machineImgCache != null) {
					((Image) windowImg.machineImgCache).setFilter(Image.FILTER_NEAREST);
				}
				d.blit(windowImg, x, y, windowImg.srcWidth * scale, windowImg.srcHeight * scale);
				String name = _t("Place_Windows");
				d.text(selected ? (MyDraw.SELECTED_C + name) : name, AGame.FOUNT, x + AGame.SGS * scale + MyDraw.SCROLL_EL_SPACING + 4, y);
				d.hook(x, y, width, AGame.SGS * scale + MyDraw.UI_SPACING, new Hook(Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						e.us.tool = new PlaceArmourTool(null);
						e.p.searchSelectionMade = true;
					}
				});
				return;
			}
			//d.drawPanel(x, y, 20, 20);
			if (!e.o.damagedApps.get(e.ship.currentBonuses).isEmpty()) {
				e.o.damagedApps.get(e.ship.currentBonuses).get(0).draw(d, x, y, AGame.SGS * scale, AGame.SGS * scale, 0, null, false);
			}
			boolean selected = e.p.armSel(e.us) == e.o;
			String name = e.o.getName();
			d.text(selected ? (MyDraw.SELECTED_C + name) : name, AGame.FOUNT, x + AGame.SGS * scale + MyDraw.SCROLL_EL_SPACING + 4, y);
			String cost = "$" + ((int) Math.ceil(e.o.getCost(e.ship.currentBonuses) * EmpireStat.SHIP_TYPE_COST_MULTIPLIER.get(e.ship.type).get(e.ship.currentBonuses)));
			d.text(cost, AGame.FOUNT, x + width - (int) d.textSize(cost, AGame.FOUNT).x, y);
			d.hook(x, y, width, AGame.SGS * scale + MyDraw.UI_SPACING, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Hook.Type type) {
					e.us.tool = new PlaceArmourTool(e.o);
					e.p.lastArmourType = e.o;
					e.p.searchSelectionMade = true;
				}
			});
			boolean explainBonuses = e.us.intent instanceof EditShipIntent && ((EditShipIntent) e.us.intent).getPlayerEmpire() != null;
			d.tooltip(x, y, width, AGame.SGS * scale + MyDraw.UI_SPACING, e.o.getDescription(e.ship.currentBonuses, explainBonuses));
			int x2 = x + width - MyDraw.ICON_BUTTON_SZ - (int) d.textSize("$1000", AGame.FOUNT).x;
			if (canVary(e)) {
				d.iconButton(x2, y, e.o.getVariantGroupHeadOrThis().variantType.icon, new Runnable() {
					@Override
					public void run() {
						ArmourType head = e.o.getVariantGroupHeadOrThis();
						e.p.incrementArmourIndex(head);
						ArrayList<ArmourType> variants = e.o.getVariantGroupHeadOrThis().variants;
						ArmourType at = variants.get(e.p.getVariantIndex(head, variants));
						e.us.tool = new PlaceArmourTool(at);
						e.p.lastArmourType = at;
					}
				}, true);
				d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("variantType_" + e.o.getVariantGroupHeadOrThis().variantType.name));
				x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			}
			if (e.p.replaceArmourSrc != null) {
				d.iconButton(x2, y, REPLACE, new Runnable() {
					@Override
					public void run() {
						e.ship.replace(e.p.replaceArmourSrc, e.o);
						((SingleShipIntent) e.us.intent).modified(e.us, true);
						e.p.replaceArmourSrc = null;
					}
				}, true);
				d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Replace_x", e.p.replaceArmourSrc.getName()));
				x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			}
		}

		@Override
		public List<Object> getList(UniScreen us, EditPalettePanel p, Airship ship) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			ArrayList<ArmourType> l = new ArrayList<ArmourType>();
			Empire owner = p.owner(us);
			for (ArmourType at : Loadable.all(ArmourType.class)) {
				if (at.hidden) { continue; }
				if (!at.availableFor(ship.type)) { continue; }
				if (at.required != null && owner != null && !owner.bonuses.contains[at.required.ordinal()] && at.required.getTech() == null) {
					continue;
				}
				if (at.required != null && !esi.hasTechBonus(at.required)) {
					continue;
				}
				at = at.getVariantGroupHeadOrThis();
				if (at.isVariantGroupHead()) {
					at = at.getVariants().get(p.getVariantIndex(at, at.getVariants()));
				}
				if (!l.contains(at)) {
					l.add(at);
				}
			}
			if (p.searching) {
				l = SearchMatcher.match(l, null, p.searchField.getText());
			}
			ArrayList l2 = new ArrayList();
			if (!p.searching) {
				l2.add(new Entry<ArmourType>(null, us, p, ship));
			}
			for (ArmourType at : l) {
				l2.add(new Entry<ArmourType>(at, us, p, ship));
			}
			return l2;
		}
		
		@Override
		public void fill(Input in, UniScreen us, EditPalettePanel p, Airship ship) {
			if (p.isEditWindowsTool(us)) {
				for (Tile t : ship.tiles) {
					t.armour.window = false;
				}
			} else {
				for (Tile t : ship.tiles) {
					if (t.module.type.isExternal() || t.module.type.getArmourType() != null) {
						continue;
					}
					t.armour.setType(p.armSel(us));
				}
			}
		}
		
		@Override
		public boolean fillEnabled(UniScreen us, EditPalettePanel p, Airship ship) {
			return p.armSel(us) != null || p.isEditWindowsTool(us);
		}
		
		@Override
		public String fillText(boolean spaceDown, UniScreen us, EditPalettePanel p, Airship ship) {
			if (p.isEditWindowsTool(us)) {
				return _t("Remove_All");
			} else {
				return _t("Fill");
			}
		}

		@Override
		public boolean hasRemove() {
			return false;
		}
		
		@Override
		public void doToggleRemove(final UniScreen us, final EditPalettePanel p) {}

		@Override
		public void drawRemove(MyDraw d, int x, int y, int width, UniScreen us, EditPalettePanel p) {}
		
		@Override
		public int bottomHeight(MyDraw d, int width, UniScreen us, EditPalettePanel p) {
			return 0;
		}
		
		@Override
		public void drawBottom(MyDraw d, int x, int y, int width, UniScreen us, EditPalettePanel p) {
			
		}
	},
	PAINT("P", "edit_mode_PAINT", true) {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			final Entry<PaintType> e = (Entry<PaintType>) t;
			int scale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			return Math.max(e.p.replacePaintSrc != null ? MyDraw.ICON_BUTTON_SZ : 0, AGame.SGS * scale) + MyDraw.SCROLL_EL_SPACING;
		}
		
		@Override
		public void stepSelect(int delta, UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			int currentIndex = -1;
			if (us.tool instanceof PaintArmourTool) {
				for (int i = 0; i < l.size(); i++) {
					if (!(l.get(i) instanceof Entry)) { continue; }
					Entry e = (Entry) l.get(i);
					if (e.o == ((PaintArmourTool) us.tool).pt) {
						currentIndex = i;
					}
				}
			}
			int newIndex = (currentIndex + delta + l.size()) % l.size();
			us.tool = new PaintArmourTool((PaintType) ((Entry) l.get(newIndex)).o);
			p.lastPaintType = (PaintType) ((Entry) l.get(newIndex)).o;
		}
		
		@Override
		public void selectFirst(UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			us.tool = new PaintArmourTool((PaintType) ((Entry) l.get(0)).o);
			p.lastPaintType = (PaintType) ((Entry) l.get(0)).o;
		}

		@Override
		public void draw(Object t, MyDraw d, int x, int y, int width) {
			int scale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			d.drawWoodGrain(x, y, width, AGame.SGS * scale + MyDraw.SCROLL_EL_SPACING);
			final Entry<PaintType> e = (Entry<PaintType>) t;
			d.rect(e.o.getTint(e.us.getBestOverallCOA()), x + 2, y + 2, AGame.SGS * scale, AGame.SGS * scale);
			boolean selected = e.p.paintSel(e.us) == e.o;
			String name = e.o.getName();
			d.text(selected ? (MyDraw.SELECTED_C + name) : name, AGame.FOUNT, x + AGame.SGS * scale + MyDraw.SCROLL_EL_SPACING + 4, y);
			d.hook(x, y, width, AGame.SGS * scale + MyDraw.UI_SPACING, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Hook.Type type) {
					e.us.tool = new PaintArmourTool(e.o);
					e.p.lastPaintType = e.o;
					e.p.searchSelectionMade = true;
				}
			});
			int x2 = x + width - MyDraw.ICON_BUTTON_SZ;
			if (e.p.replacePaintSrc != null) {
				d.iconButton(x2, y, REPLACE, new Runnable() {
					@Override
					public void run() {
						e.ship.replace(e.p.replacePaintSrc, e.o);
						((SingleShipIntent) e.us.intent).modified(e.us, true);
						e.p.replacePaintSrc = null;
					}
				}, true);
				d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Replace_x", e.p.replacePaintSrc.getName()));
				x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			}
		}

		@Override
		public List<Object> getList(UniScreen us, EditPalettePanel p, Airship ship) {
			ArrayList<PaintType> l = new ArrayList<PaintType>();
			l.addAll(PaintType.values());
			if (p.searching) {
				l = SearchMatcher.match(l, null, p.searchField.getText());
			}
			ArrayList l2 = new ArrayList();
			for (PaintType t : l) {
				l2.add(new Entry<PaintType>(t, us, p, ship));
			}
			return l2;
		}
		
		@Override
		public void fill(Input in, UniScreen us, EditPalettePanel p, Airship ship) {
			if (in.keyDown("SPACE")) {
				for (Decal d : ship.decals) {
					if (d.type.hasColoration()) {
						d.paint = p.paintSel(us);
					}
				}
			} else {
				for (Tile t : ship.tiles) {
					if (t.module.type.hasColoration() && t.module.type.isExternal()) {
						t.module.externalPaint = p.paintSel(us);
					}
					if (t.module.type.isExternal()) {
						continue;
					}
					t.armour.paint = p.paintSel(us);
				}
			}
		}
		
		@Override
		public String fillText(boolean spaceDown, UniScreen us, EditPalettePanel p, Airship ship) {
			if (spaceDown) {
				return _t("Fill_Decals");
			} else {
				return _t("Fill");
			}
		}
		
		@Override
		public boolean fillEnabled(UniScreen us, EditPalettePanel p, Airship ship) { return true; }

		@Override
		public boolean hasRemove() {
			return true;
		}
		
		@Override
		public void doToggleRemove(final UniScreen us, final EditPalettePanel p) {
			final boolean selected = us.tool instanceof PaintArmourTool && p.paintSel(us) == null;
			if (selected) {
				us.tool = UniScreen.NAVIGATE;
			} else {
				us.tool = new PaintArmourTool(null);
				p.lastPaintType = null;
			}
		}

		@Override
		public void drawRemove(MyDraw d, int x, int y, int width, final UniScreen us, final EditPalettePanel p) {
			final boolean selected = us.tool instanceof PaintArmourTool && p.paintSel(us) == null;
			d.toggle(x, y, width, _t("Remove_paint"), Keys.getText("edit_remove", "K", false), new InputRunnable() {
				@Override
				public void run(Input in) {
					if (selected) {
						us.tool = UniScreen.NAVIGATE;
					} else {
						us.tool = new PaintArmourTool(null);
						p.lastPaintType = null;
					}
				}
			}, selected, true);
		}
		
		@Override
		public int bottomHeight(MyDraw d, int width, UniScreen us, EditPalettePanel p) {
			return 0;
		}
		
		@Override
		public void drawBottom(MyDraw d, int x, int y, int width, UniScreen us, EditPalettePanel p) {
			
		}
	},
	DECALS("E", "edit_mode_DECALS", false) {
		public final Img FLIP = new Img("ui", 304, 512, 16, 16, false);
		public final Img FLIP_V = new Img("ui", 336, 512, 16, 16, false);
		
		@Override
		public boolean allowSelect(UniScreen us) {
			return us.intent instanceof EditShipIntent && ((EditShipIntent) us.intent).canSelectModulesAndDecals();
		}
		
		@Override
		public void dragComplete(UniScreen us, Input in, double startX, double startY, double endX, double endY) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!allowSelect(us)) { return; }
			Airship ship = esi.getShip(us);
			EditSelectionOverlay eso = us.shipOverlay(EditSelectionOverlay.class);
			eso.shiftX = 0;
			eso.shiftY = 0;
			eso.fail = false;
			int sx = (int) Math.floor((startX - ship.getX()) / AGame.SGS);
			int sy = (int) Math.floor((startY - ship.getY()) / AGame.SGS);
			Decal startD = ship.decalAt(ship.gridXToWorldX(sx, 1), sy);
			if (startD == null || !esi.selectedDecals.contains(startD)) {
				if (endX < startX) {
					double tmp = startX;
					startX = endX;
					endX = tmp;
				}
				if (endY < startY) {
					double tmp = startY;
					startY = endY;
					endY = tmp;
				}
				int smx = ship.gridXToWorldX((int) Math.floor((startX - ship.getX()) / AGame.SGS), 1);
				int smy = (int) Math.floor((startY - ship.getY()) / AGame.SGS);
				int emx = ship.gridXToWorldX((int) Math.floor((endX - ship.getX()) / AGame.SGS), 1);
				int emy = (int) Math.floor((endY - ship.getY()) / AGame.SGS);
				if (emx < smx) {
					int tmp = smx;
					smx = emx;
					emx = tmp;
				}
				HashSet<Decal> decals = new HashSet<Decal>();
				for (int y = smy; y <= emy; y++) {
					for (int x = smx; x <= emx; x++) {
						Decal m = ship.decalAt(x, y);
						if (m != null) {
							decals.add(m);
						}
					}
				}
				if (!in.keyDown("LSHIFT")) {
					esi.selectedDecals.clear();
				}
				esi.selectedDecals.addAll(decals);
			} else {
				int ex = (int) Math.floor((endX - ship.getX()) / AGame.SGS);
				int ey = (int) Math.floor((endY - ship.getY()) / AGame.SGS);
				
				int dx = ship.flipped ? sx - ex : ex - sx;
				int dy = ey - sy;
				
				if (dx == 0 && dy == 0) { return; }
				
				// Check we can move all decals.
				for (Decal dec : esi.selectedDecals) {
					if (ship.decals.contains(dec) && !ship.canAddDecal(dec.type, dec.x + dx, dec.y + dy, dec.layer, esi.selectedDecals)) {
						return;
					}
				}
				
				// Remove decals.
				ship.decals.removeAll(esi.selectedDecals);
				
				// Re-add decals.
				ArrayList<Decal> newDecs = new ArrayList<Decal>();
				for (Decal dec : esi.selectedDecals) {
					Decal newDec = ship.addDecal(dec.type, dec.x + dx, dec.y + dy, dec.layer);
					newDec.paint = dec.paint;
					newDecs.add(newDec);
				}
				
				esi.selectedDecals.clear();
				esi.selectedDecals.addAll(newDecs);
				esi.modified(us, true);
			}
		}
		
		@Override
		public boolean showDragRect(UniScreen us, double startX, double startY, double endX, double endY) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			Airship ship = esi.getShip(us);
			EditSelectionOverlay eso = us.shipOverlay(EditSelectionOverlay.class);
			int sx = (int) Math.floor((startX - ship.getX()) / AGame.SGS);
			int sy = (int) Math.floor((startY - ship.getY()) / AGame.SGS);
			Decal d = ship.decalAt(ship.gridXToWorldX(sx, 1), sy);
			if (d == null || !esi.selectedDecals.contains(d)) {
				eso.shiftX = 0;
				eso.shiftY = 0;
				eso.fail = false;
				return true;
			} else {
				int ex = (int) Math.floor((endX - ship.getX()) / AGame.SGS);
				int ey = (int) Math.floor((endY - ship.getY()) / AGame.SGS);
				int dx = ship.flipped ? sx - ex : ex - sx;
				int dy = ey - sy;
				eso.shiftX = ex - sx;
				eso.shiftY = ey - sy;
				eso.fail = false;
				for (Decal dec : esi.selectedDecals) {
					if (ship.decals.contains(dec) && !ship.canAddDecal(dec.type, dec.x + dx, dec.y + dy, dec.layer, esi.selectedDecals)) {
						eso.fail = true;
						break;
					}
				}
				return false;
			}
		}
		
		@Override
		public void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			Airship ship = esi.getShip(us);
			if (!us.textInputOccurring() && us.tool == UniScreen.NAVIGATE && !esi.selectedDecals.isEmpty()) {
				if (validShift(esi, ship) == null) {
					Pt sz = d.textSize(_t("Press_backspace_or_delete_to_remove"), AGame.FOUNT);
					d.text(_t("Press_backspace_or_delete_to_remove"), AGame.FOUNT, (int) (sm.width - sz.x - 5), sm.height - sz.y - 3);
				} else {
					Pt sz = d.textSize(_t("Press_x_to_duplicate_and_backspace_or_delete_to_remove", Keys.getText("edit_duplicate", "H", false)), AGame.FOUNT);
					d.text(_t("Press_x_to_duplicate_and_backspace_or_delete_to_remove", Keys.getText("edit_duplicate", "H", false)), AGame.FOUNT, (int) (sm.width - sz.x - 5), sm.height - sz.y - 3);
				}
			}
			EditSelectionOverlay eso = us.shipOverlay(EditSelectionOverlay.class);
			if ((eso.shiftX != 0 || eso.shiftY != 0) && !esi.selectedDecals.isEmpty()) {
				d.state.setCursor("DRAG", null);
			} else if (us.tool == UniScreen.NAVIGATE && cursor.y > MyDraw.TOP_BAR_H && cursor.x > us.panel(EditInfoPanel.class).myWidth) {
				double cx = us.screenToWorldX(cursor.x);
				double cy = us.screenToWorldY(cursor.y);
				for (Decal dec : esi.selectedDecals) {
					if (!ship.decals.contains(dec)) { continue; }
					double decX = ship.getX() + ship.gridXToWorldX(dec.x, dec.type.w) * AGame.SGS;
					double decY = ship.getY() + dec.y * AGame.SGS;
					if (cx > decX && cy > decY && cx < decX + dec.type.w * AGame.SGS && cy < decY + dec.type.h * AGame.SGS) {
						d.state.setCursor("DRAG", null);
						break;
					}
				}
			}
		}
		
		@Override
		public void tick(UniScreen us, Input in) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!allowSelect(us)) {
				esi.selectedDecals.clear();
			}
			Airship ship = esi.getShip(us);
			if (!us.textInputOccurring() && us.tool == UniScreen.NAVIGATE && !esi.selectedDecals.isEmpty()) {
				if (in.keyPressed("DELETE") || in.keyPressed("BACK")) {
					ship.decals.removeAll(esi.selectedDecals);
					esi.selectedModules.clear();
					esi.modified(us, true);
				} else if (Keys.check(in, "edit_duplicate", "H", false)) {
					int[] shift = validShift(esi, ship);
					if (shift != null) {
						ArrayList<Decal> decs = new ArrayList<Decal>(esi.selectedDecals);
						esi.selectedDecals.clear();
						for (Decal dec : decs) {
							Decal dec2 = ship.addDecal(dec.type, dec.x + shift[0], dec.y + shift[1], dec.layer);
							dec2.paint = dec.paint;
							esi.selectedDecals.add(dec2);
						}
					}
					esi.modified(us, true);
				}
			}
		}
		
		private int[] validShift(EditShipIntent esi, Airship ship) {
			if (esi.selectedDecals.isEmpty()) { return null; }
			int selXMin = 100000;
			int selXMax = -100000;
			int selYMin = 100000;
			int selYMax = -100000;
			for (Decal dec : esi.selectedDecals) {
				selXMin = Math.min(dec.x, selXMin);
				selXMax = Math.max(dec.x + dec.type.w, selXMax);
				selYMin = Math.min(dec.y, selYMin);
				selYMax = Math.max(dec.y + dec.type.h, selYMax);
			}
			int selW = selXMax - selXMin;
			int selH = selYMax - selYMin;
			int[][] candidates = {
				{ 1, 0 },
				{ -1, 0 },
				{ 0, 1 },
				{ 0, -1 },
				{ 1, 1 },
				{ -1, 1 },
				{ 1, -1 },
				{ -1, -1 }
			};
			for (int extraD = 0; extraD < 4; extraD++) { lp: for (int ci = 0; ci < candidates.length; ci++) {
				int dx = candidates[ci][0] * (selW + extraD);
				int dy = candidates[ci][1] * (selH + extraD);
				for (Decal dec : esi.selectedDecals) {
					if (!ship.canAddDecal(dec.type, dec.x + dx, dec.y + dy, dec.layer, Collections.EMPTY_LIST)) {
						continue lp;
					}
				}
				return new int[] { dx, dy };
			}}
			return null;
		}
		
		@Override
		public void click(UniScreen us, Input in, Pt p) {
			EditShipIntent esi = (EditShipIntent) us.intent;
			if (!allowSelect(us)) { return; }
			Airship ship = esi.getShip(us);
			int mx = ship.gridXToWorldX((int) Math.floor((p.x - ship.getX()) / AGame.SGS), 1);
			int my = (int) Math.floor((p.y - ship.getY()) / AGame.SGS);
			Decal d = ship.decalAt(mx, my);
			if (d != null) {
				if (in.keyDown("LSHIFT")) {
					if (esi.selectedDecals.contains(d)) {
						esi.selectedDecals.remove(d);
					} else {
						esi.selectedDecals.add(d);
					}
				} else {
					if (esi.selectedDecals.size() == 1 && esi.selectedDecals.contains(d) && ship.otherDecalAt(mx, my, d) != null) {
						esi.selectedDecals.clear();
						esi.selectedDecals.add(ship.otherDecalAt(mx, my, d));
					} else {
						esi.selectedDecals.clear();
						esi.selectedDecals.add(d);
					}
				}
			} else if (!in.keyDown("LSHIFT")) {
				esi.selectedDecals.clear();
			}
		}
		
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			Entry e = (Entry) t;
			if (e.o == null || e.o instanceof DecalCategory) {
				return 16 + MyDraw.SCROLL_EL_SPACING;
			}
			int scale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
			Entry<DecalType> e2 = (Entry<DecalType>) t;
			return 
					StrictMath.max(
							MyDraw.ICON_BUTTON_SZ,
							bottom(e2.o) * scale
					) - top(e2.o) * scale +
					MyDraw.SCROLL_EL_SPACING;
		}
		
		private int top(DecalType t) {
			int top = 0;
			int asz = t.apps.size();
			for (int ai = 0; ai < asz; ai++) {
				DecalType.TintedApp a = t.apps.get(ai);
				top = StrictMath.min(top, a.y);
			}
			if (t.armsDetails != null) {
				top = StrictMath.min(top, t.armsDetails.y);
			}
			return top;
		}
		
		private int bottom(DecalType t) {
			int bottom = t.h * AGame.SGS;
			int asz = t.apps.size();
			for (int ai = 0; ai < asz; ai++) {
				DecalType.TintedApp a = t.apps.get(ai);
				bottom = StrictMath.max(bottom, a.y + a.app.height() * AGame.SGS);
			}
			if (t.armsDetails != null) {
				bottom = StrictMath.max(bottom, t.armsDetails.y + t.armsDetails.size);
			}
			return bottom;
		}
		
		@Override
		public void stepSelect(int delta, UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			int currentIndex = -1;
			if (us.tool instanceof PlaceDecalTool) {
				for (int i = 0; i < l.size(); i++) {
					if (!(l.get(i) instanceof Entry)) { continue; }
					Entry e = (Entry) l.get(i);
					if (e.o == ((PlaceDecalTool) us.tool).dt) {
						currentIndex = i;
					}
				}
			}
			int newIndex = (currentIndex + delta + l.size()) % l.size();
			us.tool = new PlaceDecalTool((DecalType) ((Entry) l.get(newIndex)).o);
		}
		
		@Override
		public void selectFirst(UniScreen us, EditPalettePanel p, Airship ship) {
			List<Object> l = getList(us, p, ship);
			if (l.isEmpty()) { return; }
			for (Object o : l) {
				if (o instanceof Decal && ((Entry) o).o instanceof DecalType) {
					us.tool = new PlaceDecalTool((DecalType) ((Entry) l.get(0)).o);
					return;
				}
			}
		}
		
		@Override
		public void draw(Object t, MyDraw d, int x, int y, int width) {
			final Entry entry = (Entry) t;
			if (entry.o instanceof DecalCategory) {
				final Entry<DecalCategory> e = (Entry<DecalCategory>) t;
				final boolean open = e.p.openDecalCategories.contains(e.o);
				d.hook(x, y, width, 16 + MyDraw.SCROLL_EL_SPACING, new Hook(Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						if (open) {
							e.p.openDecalCategories.remove(e.o);
						} else {
							e.p.openDecalCategories.add(e.o);
						}
					}
				});
				d.blit(open ? MyDraw.TRIANGLE_OPEN : MyDraw.TRIANGLE_CLOSED, x + 4, y);
				d.text(e.o.getName(), AGame.FOUNT, x + 4 + MyDraw.TRIANGLE_OPEN.srcWidth + MyDraw.SCROLL_EL_SPACING, y);
			} else {
				int scale = AirshipGame.instance.currentGUIScale == GUIScale.LARGE ? 2 : 1;
				final Entry<DecalType> e = (Entry<DecalType>) t;
				int origY = y;
				y -= top(e.o);
				int h = getHeight(t, d, width);
				boolean selected = e.us.tool instanceof PlaceDecalTool && e.p.decSel(e.us) == e.o;
				d.shift(x, y);
				d.scale(scale, scale);
				PaintType pt = e.p.moduleAndDecalPaintSel;
				Color ambient = TimeOfDay.ofName("DAY").ambient;
				e.o.drawBase(d, 0, 0, 0, false, e.p.coa, "NAME", null, 1.0f, ambient, 1.0f, null, null, pt == null ? null : pt.getPaintType(e.p.coa));
				e.o.drawArmsBase(d, 0, 0, 0, false, e.p.coa, "NAME", null, 1.0f, ambient, 1.0f, null, null);
				e.o.drawCharges(d, 0, 0, 0, false, e.p.coa, "NAME", null, 1.0f, ambient, 1.0f, null, null);
				e.o.drawUnlitCharges(d, 0, 0, 0, false, e.p.coa);
				e.o.drawNonShader(d, 0, 0, 0, false, e.p.coa, "NAME", null);
				if (e.o.flag != null) {
					FlagSpec f = e.o.flag;
					switch (f.type) {
						case ARMS:
							e.p.coa.draw(d, f.x, f.y, f.size);
							break;
						case PENNANT:
							Graphics g = (Graphics) d.frame().nativeRenderer();
							g.setColor(e.p.coa.getFirstColour().tintColor);
							g.fill(new Polygon(new float[] {
								f.x, f.y,
								f.x, f.y + f.size,
								f.x + f.size * 5, f.y + f.size / 2
							}));
							g.setColor(Color.white);
							break;
					}
				}
				d.resetTransforms();
				d.drawWoodGrain(4 + x + AGame.SGS * 3 * scale, y, width, h);
				int hookTop = Math.max(origY, e.p.listTop);
				int hookBottom = Math.min(origY + h, e.p.listBottom);
				d.hook(x, hookTop, width, hookBottom - hookTop, new Hook(Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						e.us.tool = new PlaceDecalTool(e.o);
						e.p.searchSelectionMade = true;
					}
				});
				d.text((selected ? MyDraw.SELECTED_C : "") + e.o.getName(), AGame.FOUNT, x + 3 * AGame.SGS * scale + MyDraw.SCROLL_EL_SPACING + 6, y);
				int x2 = x + width - MyDraw.ICON_BUTTON_SZ;
				if (canFlip(e)) {
					d.iconButton(x2, y, FLIP, new Runnable() {
						@Override
						public void run() {
							e.us.tool = new PlaceDecalTool(e.o.flipped);
							e.p.showFlippedDecals = !e.p.showFlippedDecals;
						}
					}, true);
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (canFlipVertically(e)) {
					d.iconButton(x2, y, FLIP_V, new Runnable() {
						@Override
						public void run() {
							e.us.tool = new PlaceDecalTool(e.o.verticalFlipped);
							e.p.showVerticallyFlippedDecals = !e.p.showVerticallyFlippedDecals;
						}
					}, true);
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (canVary(e)) {
					d.iconButton(x2, y, e.o.getVariantGroupHeadOrThis().variantType.icon, new Runnable() {
						@Override
						public void run() {
							DecalType head = e.o.getVariantGroupHeadOrThis().getSymmetryGroupHead();
							e.p.incrementDecalIndex(head);
							ArrayList<DecalType> variants = e.o.getVariantGroupHeadOrThis().variants;
							entry.us.tool = new PlaceDecalTool(variants.get(e.p.getVariantIndex(head, variants)));
						}
					}, true);
					d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("variantType_" + e.o.getVariantGroupHeadOrThis().variantType.name));
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (e.p.replaceDecalSrc != null && e.o.canReplace(e.p.replaceDecalSrc)) {
					d.iconButton(x2, y, REPLACE, new Runnable() {
						@Override
						public void run() {
							e.ship.replace(e.p.replaceDecalSrc, e.o);
							((SingleShipIntent) e.us.intent).modified(e.us, true);
							e.p.replaceDecalSrc = null;
						}
					}, true);
					d.tooltip(x2, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Replace_x", e.p.replaceDecalSrc.getName()));
					x2 -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
			}
		}
		
		private boolean canFlip(Entry<DecalType> e) {
			return e.o.flipped != null;
		}
		
		private boolean canFlipVertically(Entry<DecalType> e) {
			return e.o.verticalFlipped != null;
		}
		
		private boolean canVary(Entry<DecalType> e) {
			return e.o.isVariantGroupMember();
		}

		@Override
		public List<Object> getList(UniScreen us, EditPalettePanel p, Airship ship) {
			ArrayList l = new ArrayList();		
			if (p.searching && !p.searchField.getText().isEmpty()) {
				ArrayList<DecalType> mtl = SearchMatcher.match(Loadable.all(DecalType.class), null, p.searchField.getText());
				ArrayList<DecalType> mtl2 = new ArrayList<DecalType>();
				for (DecalType mt : mtl) {
					mt = mt.getVariantGroupHeadOrThis().getSymmetryGroupHead();
					if (!mtl2.contains(mt)) {
						mtl2.add(mt);
					}
				}
				for (DecalType t : mtl2) {
					if (t.isVariantGroupHead()) {
						t = t.variants.get(p.getVariantIndex(t, t.getVariants()));
					}
					if (p.showFlippedDecals && t.flipped != null) {
						t = t.flipped;
					}
					if (p.showVerticallyFlippedDecals && t.verticalFlipped != null) {
						t = t.verticalFlipped;
					}
					l.add(new Entry<DecalType>(t, us, p, ship));
				}
			} else {
				for (DecalCategory dc : Loadable.all(DecalCategory.class)) {
					l.add(new Entry(dc, us, p, ship));
					if (p.openDecalCategories.contains(dc)) {
						for (DecalType t : dc.getContents()) {
							if (t.isSymmetryGroupMember() && !t.isSymmetryGroupHead()) { continue; }
							if (t.isVariantGroupMember() && !t.isVariantGroupHead()) { continue; }
							if (t.isVariantGroupHead()) {
								t = t.variants.get(p.getVariantIndex(t, t.variants));
							}
							if (p.showFlippedDecals && t.flipped != null) {
								t = t.flipped;
							}
							if (p.showVerticallyFlippedDecals && t.verticalFlipped != null) {
								t = t.verticalFlipped;
							}
							l.add(new Entry<DecalType>(t, us, p, ship));
						}
					}
				}
			}
			return l;
			
			
			
			/*ArrayList<DecalType> l = new ArrayList<DecalType>();
			l.addAll(Loadable.all(DecalType.class));
			if (p.searching) {
				l = SearchMatcher.match(l, null, p.searchField.getText());
			}
			ArrayList l2 = new ArrayList();
			for (DecalType t : l) {
				if (!l.contains(t.getSymmetryGroupHead())) {
					t = t.getSymmetryGroupHead();
				}
				if (t.isSymmetryGroupMember() && !t.isSymmetryGroupHead()) {
					continue;
				}
				if (p.showFlippedDecals && t.flipped != null) {
					t = t.flipped;
				}
				if (p.showVerticallyFlippedDecals && t.verticalFlipped != null) {
					t = t.verticalFlipped;
				}
				l2.add(new Entry<DecalType>(t, us, p, ship));
			}
			return l2;*/
		}

		@Override
		public boolean hasRemove() {
			return true;
		}
		
		@Override
		public void doToggleRemove(final UniScreen us, final EditPalettePanel p) {
			final boolean selected = us.tool instanceof PlaceDecalTool && p.decSel(us) == null;
			if (selected) {
				us.tool = UniScreen.NAVIGATE;
			} else {
				us.tool = new PlaceDecalTool(null);
			}
		}

		@Override
		public void drawRemove(MyDraw d, int x, int y, int width, final UniScreen us, final EditPalettePanel p) {
			final boolean selected = us.tool instanceof PlaceDecalTool && p.decSel(us) == null;
			d.toggle(x, y, width, _t("Remove_decal"), Keys.getText("edit_remove", "K", false), new InputRunnable() {
				@Override
				public void run(Input in) {
					if (selected) {
						us.tool = UniScreen.NAVIGATE;
					} else {
						us.tool = new PlaceDecalTool(null);
					}
				}
			}, selected, true);	
		}
		
		private ArrayList<Decal> selectedPaintableDecals(UniScreen us) {
			ArrayList<Decal> l = new ArrayList<Decal>();
			EditShipIntent esi = (EditShipIntent) us.intent;
			Airship ship = esi.getShip(us);
			for (Decal dec : esi.selectedDecals) {
				if (dec.type.hasColoration() && ship.decals.contains(dec)) {
					l.add(dec);
				}
			}
			return l;
		}
		
		@Override
		public int bottomHeight(MyDraw d, int width, UniScreen us, EditPalettePanel p) {
			DecalType t = p.decSel(us);
			if ((us.tool instanceof PlaceDecalTool && !(us.intent instanceof ChallengeEditShipIntent) && t != null && t.hasColoration()) ||
				(us.tool == UniScreen.NAVIGATE && !selectedPaintableDecals(us).isEmpty()))
			{
				int rowCapacity = StrictMath.max(1, (width + MyDraw.BUTTON_SPACING) / (swatchSize() + MyDraw.BUTTON_SPACING));
				int numRows = (int) StrictMath.ceil((PaintType.values().size() + 1) * 1.0 / rowCapacity);
				return MyDraw.BUTTON_SPACING + (swatchSize() + MyDraw.BUTTON_SPACING) * numRows - MyDraw.BUTTON_SPACING;
			}
			
			return 0;
		}
		
		@Override
		public void drawBottom(MyDraw d, final int x, int y, int width, final UniScreen us, final EditPalettePanel p) {
			int bh = bottomHeight(d, width, us, p);
			if (bh > 0) {
				d.hook(x, y, width, bh, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						// Swallow clicks so they don't trigger the hook from a module.
					}
				});
			} else {
				return;
			}
			y += MyDraw.BUTTON_SPACING;
			DecalType t = p.decSel(us);
			
			int x2 = x;
			
			HashSet<PaintType> selectedPaintTypes = new HashSet<PaintType>();
			if (us.tool instanceof PlaceDecalTool) {
				selectedPaintTypes.add(p.moduleAndDecalPaintSel);
			} else {
				for (Decal dec : selectedPaintableDecals(us)) {
					selectedPaintTypes.add(dec.paint);
				}
			}
			
			ArrayList<PaintType> pts = PaintType.values();
			for (int pti = -1; pti < pts.size(); pti++) {
				final PaintType paintType = pti == -1 ? null : pts.get(pti);
				if (x2 + swatchSize() > x + width) {
					y += swatchSize() + MyDraw.BUTTON_SPACING;
					x2 = x;
				}
				
				Clr c;
				if (us.tool instanceof PlaceModuleTool) {
					c = paintType == null ? t.colorationDefault() : t.subColorByPaintTypeIndex[paintType.getPaintType(us.getBestOverallCOA()).ordinal()];
				} else {
					c = paintType == null ? Clr.GREY : paintType.getTint(us.getBestOverallCOA());
				}
				
				if (selectedPaintTypes.contains(paintType)) {
					d.rect(c, x2 - MyDraw.BUTTON_SPACING / 2, y - MyDraw.BUTTON_SPACING / 2, swatchSize() + MyDraw.BUTTON_SPACING, swatchSize() + MyDraw.BUTTON_SPACING);
				} else {
					d.rect(c, x2, y, swatchSize(), swatchSize());
				}
				if (paintType != null && paintType.isArmsBased()) {
					d.blit(SHIELD_ICON_OUTLINE, Clr.BLACK, x2 + swatchSize() / 2 - 8, y + swatchSize() / 2 - 8);
					d.blit(SHIELD_ICON, x2 + swatchSize() / 2 - 8, y + swatchSize() / 2 - 8);
				}
				d.hook(x2, y, swatchSize(), swatchSize(), new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						if (us.tool instanceof PlaceDecalTool) {
							p.moduleAndDecalPaintSel = paintType;
						} else {
							for (Decal dec : selectedPaintableDecals(us)) {
								dec.paint = paintType;
							}
							((EditShipIntent) us.intent).modified(us, true);
						}
					}
				});
				d.tooltip(x2, y, swatchSize(), swatchSize(), paintType == null ? _t("Base_Colour") : paintType.getName());
				x2 += swatchSize() + MyDraw.BUTTON_SPACING;
			}
		}
	};
	
	public static final Clr ARMOUR_BACK_TINT = new Clr(64, 64, 64);
	public static final Img SHIELD_ICON = new Img("ui", 384, 432, 16, 16, false);
	public static final Img SHIELD_ICON_OUTLINE = new Img("ui", 400, 432, 16, 16, false);
	
	public String getName() {
		return _t("editmode_" + name());
	}
		
	public final String shortcut;
	public final String shortcutKey;
	public final boolean canFill;
	private EditMode(String shortcut, String shortcutKey, boolean canFill) {
		this.shortcut = shortcut;
		this.shortcutKey = shortcutKey;
		this.canFill = canFill;
	}
	
	@Override
	public abstract int getHeight(Object t, MyDraw d, int availableWidth);

	@Override
	public abstract void draw(Object t, MyDraw d, int x, int y, int width);
	
	public boolean allowSelect(UniScreen us) { return false; }
	public void dragComplete(UniScreen us, Input in, double startX, double startY, double endX, double endY) {}
	public boolean showDragRect(UniScreen us, double startX, double startY, double endX, double endY) { return false; }
	public void click(UniScreen us, Input in, Pt p) {}
	public void tick(UniScreen us, Input in) {}
	public void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {}
	public abstract boolean hasRemove();
	public abstract void doToggleRemove(final UniScreen us, final EditPalettePanel p);
	public abstract void drawRemove(MyDraw d, int x, int y, int width, UniScreen us, EditPalettePanel p);
	public abstract int bottomHeight(MyDraw d, int width, UniScreen us, EditPalettePanel p);
	public abstract void drawBottom(MyDraw d, int x, int y, int width, UniScreen us, EditPalettePanel p);
	
	public void fill(Input in, UniScreen us, EditPalettePanel p, Airship ship) {}
	public boolean fillEnabled(UniScreen us, EditPalettePanel p, Airship ship) { return false; }
	public String fillText(boolean spaceDown, UniScreen us, EditPalettePanel p, Airship ship) { return _t("Fill"); }
	
	public abstract void stepSelect(int delta, UniScreen us, EditPalettePanel p, Airship ship);
	public abstract void selectFirst(UniScreen us, EditPalettePanel p, Airship ship);
	
	public abstract List<Object> getList(UniScreen us, EditPalettePanel p, Airship ship);
	
	public final Img REPLACE = new Img("ui", 0, 416, 16, 16, false);
	
	private static int swatchSize() {
		switch (AirshipGame.instance.currentGUIScale) {
			case SMALL:
				return 16;
			case MEDIUM:
				return 24;
			case LARGE:
				return 32;
		}
		return 24;
	}
	
	private static strictfp class Entry<T> {
		public final T o;
		public final UniScreen us;
		public final EditPalettePanel p;
		public final Airship ship;

		public Entry(T t, UniScreen us, EditPalettePanel p, Airship ship) {
			this.o = t;
			this.us = us;
			this.p = p;
			this.ship = ship;
		}
	}
}
