package com.zarkonnen.airships;

import static com.zarkonnen.airships.Airship.AMMO_FG;
import static com.zarkonnen.airships.Airship.ARMOUR_BACK;
import static com.zarkonnen.airships.Airship.COAL_FG;
import static com.zarkonnen.airships.Airship.REPAIR_FG;
import static com.zarkonnen.airships.Airship.WATER_FG;
import static com.zarkonnen.airships.Appearance.currentPostfix;
import static com.zarkonnen.airships.Appearance.lsp;
import static com.zarkonnen.airships.Appearance.shaderLoadFailed;
import com.zarkonnen.airships.WeaponAppearance.Shell;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;

public strictfp class ShipLayers {
	public static strictfp class DamagedBackExternals implements UniScreen.ShipLayer {
		private final int drawPriority;
		private final int drawPriority2;
		
		public DamagedBackExternals(int drawPriority, int drawPriority2) {
			this.drawPriority = drawPriority;
			this.drawPriority2 = drawPriority2;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			if (ssb.getDamagedVersion() != null) {
				Appearance.lockSubShader(ssb.getDamagedVersion(), d, light, lightStrength, ambient, ambientSaturation);
			} else {
				Appearance.lockSubShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
			}
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			ship.showingOutside = outside;
			//if (outside) { return; }
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if (m.hp <= 0 && !m.type.hasSpecificDestroyedExternalAppearances() && (m.type.getExternalDrawPriority() == drawPriority || m.type.getExternalDrawPriority() == drawPriority2)) {
					if (m.type.getExternalDrawPriority() != -1 && outside) { continue; }
					m.type.drawExternal(d,
							x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + (m.y) * AGame.SGS,
							m.time + m.animOffset,
							ship.flipped,
							m.variant, ship.currentBonuses,
							light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs,
							false,
							false,
							m.externalPaint == null ? null : m.externalPaint.getPaintType(coa));
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockSubShader(light != null);
		}	

		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}

		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class BackExternals implements UniScreen.ShipLayer {
		private final int drawPriority;
		private final int drawPriority2;

		public BackExternals(int drawPriority, int drawPriority2) {
			this.drawPriority = drawPriority;
			this.drawPriority2 = drawPriority2;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockSubShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			//if (!outside) {
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if ((m.hp > 0 || m.type.hasSpecificDestroyedExternalAppearances()) && (m.type.getExternalDrawPriority() == drawPriority || m.type.getExternalDrawPriority() == drawPriority2)) {
					if (m.type.getExternalDrawPriority() != -1 && outside) { continue; }
					m.type.drawExternal(d,
							x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + (m.y) * AGame.SGS,
							m.time + m.animOffset,
							ship.flipped,
							m.variant, ship.currentBonuses,
							light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs,
							m.hp < m.maxHP / 2,
							m.hp <= 0,
							m.externalPaint == null ? null : m.externalPaint.getPaintType(coa));
				}
			}
			//}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockSubShader(light != null);
		}	

		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Splinters implements UniScreen.ShipLayer {
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			RotatingShader.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			int tsz = ship.tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				if (t.module.type.isExternal() || !t.module.type.hasGenericDestructionFragments()) {
					continue;
				}
				double tx = x + ship.gridXToWorldX(t.x, 1) * AGame.SGS;
				double ty = y + t.y * AGame.SGS;
				if (t.splinters == null) {
					t.splinters = new Img[4];
					ArrayList<Splinter> splinters = Loadable.all(Splinter.class);
					t.splinters[0] = splinters.get(AGame.ANIM_R.nextInt(splinters.size())).img;
					t.splinters[1] = splinters.get(AGame.ANIM_R.nextInt(splinters.size())).img;
					t.splinters[2] = splinters.get(AGame.ANIM_R.nextInt(splinters.size())).img;
					t.splinters[3] = splinters.get(AGame.ANIM_R.nextInt(splinters.size())).img;
				}
				if (!t.adjacent[0][1] && t.hadAdjacentOccupableTile[0][1]) {
					RotatingShader.draw(ssb, t.splinters[0], d, tx, ty - t.splinters[0].srcHeight, 0.0, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr);
				}
				if (!t.adjacent[2][1] && t.hadAdjacentOccupableTile[2][1]) {
					RotatingShader.draw(ssb, t.splinters[0], d, tx, ty + AGame.SGS, StrictMath.PI, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr);
				}
				if (!t.adjacent[1][ship.flipped ? 2 : 0] && t.hadAdjacentOccupableTile[1][ship.flipped ? 2 : 0]) {
					RotatingShader.draw(ssb, t.splinters[0], d, tx - t.splinters[0].srcWidth, ty, StrictMath.PI * 3 / 2, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr);
				}
				if (!t.adjacent[1][ship.flipped ? 0 : 2] && t.hadAdjacentOccupableTile[1][ship.flipped ? 0 : 2]) {
					RotatingShader.draw(ssb, t.splinters[0], d, tx + AGame.SGS, ty, StrictMath.PI / 2, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			RotatingShader.unlockShader();
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class BackArmour implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockMaskedBevelledShader(ssb, ssb2, d, light, lightStrength, ambient, ambientSaturation, /* concave */ true);
		}

		final int[][] patch9 = {{1,1,1},{1,1,1},{1,1,1},};
		
		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			final float armourBackFalloff = outside ? 0.1f : 0.25f;
			final ArmourType none = ArmourType.ofName("NONE");
			int tsz = ship.tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				if (t.armour.type.hidden || t.armour.type == none || t.module.type.hasSolidAppearance) {
					continue;
				}
				if (outside && !t.armour.window && t.armour.hp == t.armour.getMaxHP()) {
					continue;
				}
				Appearance app = outside ? (t.armour.type.damagedApps.get(ship.currentBonuses).isEmpty() ? ArmourPlate.noApp : t.armour.type.damagedApps.get(ship.currentBonuses).get(0)) : t.armour.getApp();
				TileMask tileMask = t.module.type.getTileMasks(ship.currentBonuses)[t.y - t.module.y][t.x - t.module.x];
				if (tileMask == TileMask.EMPTY) {
					continue;
				}
				Appearance moduleApp = t.module.type.getApp(ship.currentBonuses, 1);
				if (app.spritesheetBundle != ssb || moduleApp.spritesheetBundle != ssb2) {
					if (additionalSSBPairs != null) {
						additionalSSBPairs.add(new Utils.Pair<SpritesheetBundle, SpritesheetBundle>(app.spritesheetBundle, moduleApp.spritesheetBundle));
					}
					continue;
				}
				float brightnessMult = !outside && t.module.burntOut ? 0.2f : 1.0f;
				patch9[0][0] = 1; patch9[0][1] = 1; patch9[0][2] = 1;
				patch9[1][0] = 1; patch9[1][1] = 1; patch9[1][2] = 1;
				patch9[2][0] = 1; patch9[2][1] = 1; patch9[2][2] = 1;
				if (!outside && t.armour.hp == 0) {
					for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
						if (dx == 0 || dy == 0) {
							if (t.adjacent[dy + 1][dx + 1] && ship.tileAt(t.x + dx, t.y + dy).armour.hp == 0) {
								patch9[dy + 1][dx + 1] = 0;
							}
						} else {
							if (t.adjacent[dy + 1][dx + 1] && ship.tileAt(t.x + dx, t.y + dy).armour.hp == 0 &&
								(
									(t.adjacent[dy + 1][1] && ship.tileAt(t.x, t.y + dy).armour.hp == 0) ||
									(t.adjacent[1][dx + 1] && ship.tileAt(t.x + dx, t.y).armour.hp == 0)
								)
							)
							{
								patch9[dy + 1][dx + 1] = 0;
							}
						}
					}}
				}
				
				Img mask = t.module.type.getArmourMask(ship.currentBonuses);
				TileMask useTileMask = null;
				int maskX = 0, maskY = 0;
				if (mask != null) {
					if (Appearance.useSimpleGraphics) {
						useTileMask = tileMask;
						if (ship.flipped) {
							useTileMask = useTileMask.flipped;
						}
					}
					
					if (t.module.type.isFlipped()) {
						maskX = mask.srcX + (t.module.type.getW() - (t.x - t.module.x) - 1) * AGame.SGS;
					} else {
						maskX = mask.srcX + (t.x - t.module.x) * AGame.SGS;
					}
					maskY = mask.srcY + (t.y - t.module.y) * AGame.SGS;
				}
				
				app.drawMaskedBevelled(d,
						x + ship.gridXToWorldX(t.x, 1) * AGame.SGS, y + t.y * AGame.SGS,
						AGame.SGS, AGame.SGS,
						ms, Clr.DARK_GREY, ship.flipped ^ t.module.type.isFlipped(),
						light, lightStrength * armourBackFalloff * brightnessMult, ambient,
						ambientSaturation,
						t.adjacent[0][1] ? 0 : 1, t.adjacent[2][1] ? 0 : 1, t.adjacent[1][t.module.type.isFlipped() ? 2 : 0] ? 0 : 1, t.adjacent[1][t.module.type.isFlipped() ? 0 : 2] ? 0 : 1,
						true, // concave
						patch9,
						null, 0.36f,
						mask != null, maskX, maskY, useTileMask, ssb2);
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockMaskedBevelledShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}	
	
	public static strictfp class ModuleBacks implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				Appearance back = m.type.getBack(ship.currentBonuses);
				if (back != null) {
					if (back.spritesheetBundle != ssb) {
						if (additionalSSBs != null) {
							additionalSSBs.add(back.spritesheetBundle);
						}
						continue;
					}
					m.type.drawBack(d,
						x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
						y + m.y * AGame.SGS,
						m.time + m.animOffset,
						null,
						ship.flipped,
						m.variant, ship.currentBonuses,
						light, lightStrength, ambient, ambientSaturation);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockShader(light != null);
		}	

		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Barrels implements UniScreen.ShipLayer {
		public final boolean external;

		public Barrels(boolean external) {
			this.external = external;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			RotatingColoringShader.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (external && !outside) { return; }
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				WeaponAppearance wa = m.type.weaponAppearance(ship.currentBonuses);
				if (wa != null) {
					PaintType subPaintType = m.externalPaint == null ? null : m.externalPaint.getPaintType(coa);
					Clr subSrc = null;
					Clr subTrg = null;
					if (wa.useSubColorForBarrel && m.type.externalSubColor != null) {
						subSrc = m.type.externalSubColor;
						subTrg = subPaintType == null ? subSrc : m.type.externalSubColorByPaintIndex[subPaintType.ordinal()];
					}
					float[] subSrcF = subSrc == null ? new float[] {0, 0, 0} : new float[] {subSrc.r / 255.0f, subSrc.g / 255.0f, subSrc.b / 255.0f};
					float[] subTrgF = subTrg == null ? new float[] {0, 0, 0} : new float[] {subTrg.r / 255.0f, subTrg.g / 255.0f, subTrg.b / 255.0f};
					if (wa.onlyShowBarrelIfLoaded && m.ammoLeft == 0) { continue; }
					Img barrel =
							external
							?
							ship.flipped ? wa.externalFlippedbarrel : wa.externalBarrel
							:
							ship.flipped ? wa.flippedbarrel : wa.barrel;
					if (external) {
						if (wa.externalBarrelAnimation != null) {
							WeaponAppearance.BarrelAnimation eba = ship.flipped ? wa.externalFlippedBarrelAnimation : wa.externalBarrelAnimation;
							barrel = eba.frames.get((m.externalBarrelAnimationOffset / eba.interval) % eba.frames.size());
						}
					} else {
						if (wa.barrelAnimation != null) {
							WeaponAppearance.BarrelAnimation ba = ship.flipped ? wa.flippedBarrelAnimation : wa.barrelAnimation;
							barrel = ba.frames.get((m.barrelAnimationOffset / ba.interval) % ba.frames.size());
						}
					}
					Img mainBarrel = barrel;
					
					if (barrel == null) { continue; }
					
					if (!external && m.ammoLeft == 0 && wa.barrelLoadStages != null) {
						Img[] stages = ship.flipped ? wa.flippedBarrelLoadStages : wa.barrelLoadStages;
						if (m.numAmmoForClip() < stages.length) {
							barrel = stages[m.numAmmoForClip()];
						}
					}
					
					if (!barrel.src.equals(ssb.name)) {
						if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, barrel.src)) {
							additionalSSBs.add(SpritesheetBundle.ofName(barrel.src));
						}
						continue;
					}
					Pt offset = ship.flipped ? wa.flippedBarrelOffset : wa.barrelOffset;
					double angle = (ship.flipped ^ m.type.isFlipped()) ? m.weaponAngle + StrictMath.PI : m.weaponAngle;
					if (wa.ignoreBarrelRotation) {
						angle = 0;
					}
					double recoilX = StrictMath.cos(m.weaponAngle) * m.recoil;
					double recoilY = StrictMath.sin(m.weaponAngle) * m.recoil;
					if (m.hp > 0) {
						double bx = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + offset.x - recoilX + mainBarrel.srcWidth * 0.5 - barrel.srcWidth * 0.5;
						double by = y + m.y * AGame.SGS + offset.y - recoilY + mainBarrel.srcHeight * 0.5 - barrel.srcHeight * 0.5;
						RotatingColoringShader.draw(ssb, barrel, d, bx, by, angle, 1, barrel.flipped, false, light, lightStrength, ambient, ambientSaturation, ambientClr,
								subSrcF, subTrgF, subSrcF, subTrgF);
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			RotatingColoringShader.unlockShader();
		}	
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Modules implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			//if (outside && scale <= 0.75) { return; }
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if (!outside && !m.type.drawAppearanceInside()) { continue; }
				if (outside && scale <= 0.75 && !m.hasInvisibleArmour()) { continue; }
				if (!displayStatusIcons || m.hp > 0) {
					double res = m.visibleResourceLevel();
					if (m.type.getApp(ship.currentBonuses, res).spritesheetBundle != ssb) {
						if (additionalSSBs != null) {
							additionalSSBs.add(m.type.getApp(ship.currentBonuses, res).spritesheetBundle);
						}
						continue;
					}
					m.type.draw(d,
							x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + m.y * AGame.SGS,
							m.time + m.animOffset,
							ship.flipped,
							m.variant, ship.currentBonuses, res,
							light, lightStrength, ambient, ambientSaturation);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockShader(light != null);
		}	
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DamagedModules implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			if (ssb.getDamagedVersion() != null) {
				Appearance.lockShader(ssb.getDamagedVersion(), d, light, lightStrength, ambient, ambientSaturation);
			} else {
				Appearance.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
			}
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			//if (outside && scale <= 0.75) { return; }
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if (!outside && !m.type.drawAppearanceInside()) { continue; }
				if (outside && scale <= 0.75 && !m.hasInvisibleArmour()) { continue; }
				if (displayStatusIcons && m.hp <= 0) {
					float brightnessMult = m.burntOut ? 0.5f : 1.0f;
					double res = m.visibleResourceLevel();
					Appearance app = m.type.getApp(ship.currentBonuses, res);
					if (app.spritesheetBundle != ssb) {
						if (additionalSSBs != null) {
							additionalSSBs.add(app.spritesheetBundle);
						}
						continue;
					}
					m.type.draw(d,
							x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + m.y * AGame.SGS,
							m.time + m.animOffset,
							m.burntOut ? Clr.GREY : null,
							ship.flipped,
							m.variant, ship.currentBonuses, res,
							light, lightStrength * brightnessMult, ambient,
							ambientSaturation * brightnessMult);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockShader(light != null);
		}	
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DetailedCrew implements UniScreen.ShipLayer {
		private final float[][] coaColors = new float[5][3];
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			RotatingColoringShader.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			//if (outside && scale < 0.75) { return; }
			coaColors[0] = coa.getTincture(0).replacementValue;
			coaColors[1] = coa.getTincture(1).replacementValue;
			coaColors[2] = coa.getTincture(2).replacementValue;
			coaColors[3] = coa.getFirstColour().replacementValue;
			coaColors[4] = coa.getFirstMetal().replacementValue;
			int csz = ship.crew.size();
			for (int carried = 0; carried <= 1; carried++) {
				for (int ci = 0; ci < csz; ci++) {
					Crewman c = ship.crew.get(ci);
					Crewman carrier = c.carrier();
					boolean arachnophobia = c.type.simpleLookSpider != null && SimplePref.ARACHNOPHOBIA_MODE.get();
					if (carried == 1 && carrier != null && scale >= 1) {
						c.draw(d, x + ship.gridXToWorldX(carrier.currentTile.x, 1) * AGame.SGS - 1, y + carrier.currentTile.y * AGame.SGS - 5, ms, light, lightStrength, ambient, ambientSaturation, ambientClr, ssb, additionalSSBs, coaColors, carrier);
					} else if (carrier == null) {
						if (scale >= 1 && !arachnophobia) {
							c.draw(d, x + ship.gridXToWorldX(c.currentTile.x, 1) * AGame.SGS, y + c.currentTile.y * AGame.SGS, ms, light, lightStrength, ambient, ambientSaturation, ambientClr, ssb, additionalSSBs, coaColors, null);
						} else if ((arachnophobia && scale >= 1) || !outside || scale >= 0.75 || c.currentTile.module.type.isExternal()) {
							c.simpleDraw(d, x + ship.gridXToWorldX(c.currentTile.x, 1) * AGame.SGS, y + c.currentTile.y * AGame.SGS, ms, light, lightStrength, ambient, ambientSaturation, ambientClr, ssb, additionalSSBs, coaColors);
						}
					}
				}
			}
			coaColors[0] = enemyCOA.getTincture(0).replacementValue;
			coaColors[1] = enemyCOA.getTincture(1).replacementValue;
			coaColors[2] = enemyCOA.getTincture(2).replacementValue;
			coaColors[3] = enemyCOA.getFirstColour().replacementValue;
			coaColors[4] = enemyCOA.getFirstMetal().replacementValue;
			int bsz = ship.boarders.size();
			for (int bi = 0; bi <  bsz; bi++) {
				Crewman b = ship.boarders.get(bi);
				boolean arachnophobia = b.type.simpleLookSpider != null && SimplePref.ARACHNOPHOBIA_MODE.get();
				if (scale >= 1 && !arachnophobia) {
					b.draw(d, x + ship.gridXToWorldX(b.currentTile.x, 1) * AGame.SGS, y + b.currentTile.y * AGame.SGS, ms, light, lightStrength, ambient, ambientSaturation, ambientClr, ssb, additionalSSBs, coaColors, null);
				} else if ((arachnophobia && scale >= 1) || !outside || scale >= 0.75 || b.currentTile.module.type.isExternal()) {
					b.simpleDraw(d, x + ship.gridXToWorldX(b.currentTile.x, 1) * AGame.SGS, y + b.currentTile.y * AGame.SGS, ms, light, lightStrength, ambient, ambientSaturation, ambientClr, ssb, additionalSSBs, coaColors);
				}		
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			RotatingColoringShader.unlockShader();
		}	
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static final Clr BAR_BG = Clr.fromHex("141414");
	public static final Clr BAR_BORDER = Clr.fromHex("726534");
	public static final Clr MILD_DMG = Clr.fromHex("d9cbb1");
	public static final Clr MED_DMG = Clr.fromHex("d59b7f");
	public static final Clr HV_DMG = Clr.fromHex("bb421d");
	public static final Clr RELOAD = Clr.fromHex("c8b84f");
	public static final Clr READY = Clr.fromHex("6e9511");
	public static final Clr AMMO_RELOAD = Clr.fromHex("7b421b");
	
	public static strictfp class HPBars implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			double px = 1.0 / scale;
			if (!outside && showHPBarsAndDoors) {
				int msz = ship.modules.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = ship.modules.get(mi);
					if (displayStatusIcons && m.hp < m.getMaxHP() / 2 && m.hp > 0) {
						double barX = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + 3 * px;
						double barW = m.type.getW() * AGame.SGS - 6 * px;
						double barH = (scale > 2.5 ? 6 : 2) * px;
						double barY = y + (m.y + m.type.getH()) * AGame.SGS - barH - 3 * px - 1;
						d.rect(BAR_BORDER, barX - 2 * px, barY - 2 * px, barW + 4 * px, barH + 4 * px);
						d.rect(BAR_BG, barX - 1 * px, barY - 1 * px, barW + 2 * px, barH + 2 * px);
						Clr c = MILD_DMG;
						if (m.hp < m.getMaxHP() / 2) {
							c = MED_DMG;
						}
						if (m.hp < m.getMaxHP() / 4) {
							c = HV_DMG;
						}
						d.rect(c, barX + barW * m.getMaxRepairToHP() / m.getMaxHP() - px, barY, px, barH);
						d.rect(c, barX, barY, barW * m.hp / m.getMaxHP(), barH);
					}
					double barX = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + 3 * px;
					double barH = m.type.getH() * AGame.SGS - (scale > 2.5 ? 6 : 2) * px - 1 - 11 * px;
					double barY = y + m.y * AGame.SGS + 3 * px;
					double barW = (scale > 2.5 ? 6 : 2) * px;
					if (displayStatusIcons && m.type.getCoal(ship.currentBonuses) > 0) {
						d.rect(BAR_BORDER, barX - 2 * px, barY - 2 * px, barW + 4 * px, barH + 4 * px);
						d.rect(BAR_BG, barX - 1 * px, barY - 1 * px, barW + 2 * px, barH + 2 * px);
						d.rect(COAL_FG, barX, barY + barH - barH * m.getResource(Resource.COAL) / m.type.getCoal(ship.currentBonuses), barW, barH * m.getResource(Resource.COAL) / m.type.getCoal(ship.currentBonuses));
					}
					if (displayStatusIcons && m.type.getWater(ship.currentBonuses) > 0) {
						d.rect(BAR_BORDER, barX - 2 * px, barY - 2 * px, barW + 4 * px, barH + 4 * px);
						d.rect(BAR_BG, barX - 1 * px, barY - 1 * px, barW + 2 * px, barH + 2 * px);
						d.rect(WATER_FG, barX, barY + barH - barH * m.getResource(Resource.WATER) / m.type.getWater(ship.currentBonuses), barW, barH * m.getResource(Resource.WATER) / m.type.getWater(ship.currentBonuses));
					}
					if (displayStatusIcons && m.type.getRepair(ship.currentBonuses) > 0) {
						d.rect(BAR_BORDER, barX - 2 * px, barY - 2 * px, barW + 4 * px, barH + 4 * px);
						d.rect(BAR_BG, barX - 1 * px, barY - 1 * px, barW + 2 * px, barH + 2 * px);
						d.rect(REPAIR_FG, barX, barY + barH - barH * m.getResource(Resource.REPAIR) / m.type.getRepair(ship.currentBonuses), barW, barH * m.getResource(Resource.REPAIR) / m.type.getRepair(ship.currentBonuses));
					}
					if (displayStatusIcons && m.type.getAmmo(ship.currentBonuses) > 0) {
						d.rect(BAR_BORDER, barX - 2 * px, barY - 2 * px, barW + 4 * px, barH + 4 * px);
						d.rect(BAR_BG, barX - 1 * px, barY - 1 * px, barW + 2 * px, barH + 2 * px);
						d.rect(AMMO_FG, barX, barY + barH - barH * m.getResource(Resource.AMMO) / m.type.getAmmo(ship.currentBonuses), barW, barH * m.getResource(Resource.AMMO) / m.type.getAmmo(ship.currentBonuses));
					}
					
					if (displayStatusIcons && m.type.getReload(ship.currentBonuses) != 0 && m.hp > 0 && (m.ammoLeft > 0 || m.type.canResupplyInCombat(ship.currentBonuses))) {
						barW = 4;
						barX = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS
								+ 
								(!(m.type.isFlipped() ^ ship.flipped)
								? (m.type.getW() * AGame.SGS - barW - px * 3)
								: (px * 3));
						barH = (scale > 2.5 ? 6 : 2) * px;
						d.rect(BAR_BORDER, barX - 2 * px, barY - 2 * px, barW + 4 * px, barH + 4 * px);
						d.rect(BAR_BG, barX - 1 * px, barY - 1 * px, barW + 2 * px, barH + 2 * px);
						Clr c = m.shootAccumulator >= m.type.getReload(ship.currentBonuses) && m.hp > 0 && m.somewhatStaffed()
								? READY : RELOAD;
						double amt = (barW * StrictMath.min(m.shootAccumulator, m.type.getReload(ship.currentBonuses))) / m.type.getReload(ship.currentBonuses);
						if (m.clipReloadCooldown > 0) {
							c = AMMO_RELOAD;
							amt = (barW * (m.type.getClipReloadTime(ship.currentBonuses) - m.clipReloadCooldown)) / m.type.getClipReloadTime(ship.currentBonuses);
						}
						d.rect(c, barX, barY, amt, barH);
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Armour implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockMaskedBevelledShader(ssb, ssb2, d, light, lightStrength, ambient, ambientSaturation, /* concave */ false);
		}
		
		final int[][] patch9 = {{1,1,1},{1,1,1},{1,1,1},};

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (!outside) { return; }
			int tsz = ship.tiles.size();
			ArmourType none = ArmourType.ofName("NONE");
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				
				if (t.armour.type.hidden || t.armour.type == none) {
					continue;
				}
				Appearance app = outside ? (t.armour.type.damagedApps.get(ship.currentBonuses).isEmpty() ? ArmourPlate.noApp : t.armour.type.damagedApps.get(ship.currentBonuses).get(0)) : t.armour.getApp();
				TileMask tileMask = t.module.type.getTileMasks(ship.currentBonuses)[t.y - t.module.y][t.x - t.module.x];
				if (tileMask == TileMask.EMPTY) {
					continue;
				}
				Appearance moduleApp = t.module.type.getApp(ship.currentBonuses, 1);
				if (app.spritesheetBundle != ssb || moduleApp.spritesheetBundle != ssb2) {
					if (additionalSSBPairs != null) {
						additionalSSBPairs.add(new Utils.Pair<SpritesheetBundle, SpritesheetBundle>(app.spritesheetBundle, moduleApp.spritesheetBundle));
					}
					continue;
				}
								
				patch9[0][0] = 1; patch9[0][1] = 1; patch9[0][2] = 1;
				patch9[1][0] = 1; patch9[1][1] = 1; patch9[1][2] = 1;
				patch9[2][0] = 1; patch9[2][1] = 1; patch9[2][2] = 1;
				if (t.armour.hp == 0) {
					for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
						if (dx == 0 || dy == 0) {
							if (t.adjacent[dy + 1][dx + 1] && ship.tileAt(t.x + dx, t.y + dy).armour.hp == 0) {
								patch9[dy + 1][dx + 1] = 0;
							}
						} else {
							if (t.adjacent[dy + 1][dx + 1] && ship.tileAt(t.x + dx, t.y + dy).armour.hp == 0 &&
								(
									(t.adjacent[dy + 1][1] && ship.tileAt(t.x, t.y + dy).armour.hp == 0) ||
									(t.adjacent[1][dx + 1] && ship.tileAt(t.x + dx, t.y).armour.hp == 0)
								)
							)
							{
								patch9[dy + 1][dx + 1] = 0;
							}
						}
					}}
				}
				
				Img mask = t.module.type.getArmourMask(ship.currentBonuses);
				TileMask useTileMask = null;
				int maskX = 0, maskY = 0;
				if (mask != null) {
					if (Appearance.useSimpleGraphics) {
						useTileMask = tileMask;
						if (ship.flipped) {
							useTileMask = useTileMask.flipped;
						}
					}
					
					if (t.module.type.isFlipped()) {
						maskX = mask.srcX + (t.module.type.getW() - (t.x - t.module.x) - 1) * AGame.SGS;
					} else {
						maskX = mask.srcX + (t.x - t.module.x) * AGame.SGS;
					}
					maskY = mask.srcY + (t.y - t.module.y) * AGame.SGS;
				}
												
				t.armour.getApp().drawMaskedBevelled(d,
						x + ship.gridXToWorldX(t.x, 1) * AGame.SGS, y + t.y * AGame.SGS,
						AGame.SGS, AGame.SGS,
						ms, null, ship.flipped ^ t.module.type.isFlipped(),
						light, lightStrength, ambient, ambientSaturation,
						t.adjacent[0][1] ? 0 : 1, t.adjacent[2][1] ? 0 : 1, t.adjacent[1][t.module.type.isFlipped() ? 2 : 0] ? 0 : 1, t.adjacent[1][t.module.type.isFlipped() ? 0 : 2] ? 0 : 1,
						/* concave */ false,
						patch9,
						t.armour.paint == null ? null : t.armour.paint.getTint(coa), t.armour.paint == null ? 0.36f : t.armour.paint.getPaintType(coa).shiny(),
						mask != null, maskX, maskY, useTileMask, ssb2);
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockMaskedBevelledShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Paint implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (!outside) { return; }
			if (light != null) { return; }
			ArmourType none = ArmourType.ofName("NONE");
			int tsz = ship.tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				if (t.armour.paint != null && t.armour.type != none && t.module.type.getTileMasks(ship.currentBonuses)[t.y - t.module.y][t.x - t.module.x] == TileMask.FULL) {
					d.rect(
							t.armour.paint.getTint(coa),
							x + ship.gridXToWorldX(t.x, 1) * AGame.SGS,
							y + t.y * AGame.SGS,
							AGame.SGS,
							AGame.SGS);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {}
		
		@Override
		public boolean doDraw(double scale) {
			return Appearance.useSimpleGraphics || Appearance.shaderLoadFailed;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class TileDebug implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			int tsz = ship.tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				if (t.full()) { continue; }
				d.rect(Clr.RED, x + ship.gridXToWorldX(t.x, 1) * AGame.SGS + t.solidCenterX(),
							y + t.y * AGame.SGS + t.solidCenterY(), 1, 1);
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DecalsBases implements UniScreen.ShipLayer {
		private final int insideDrawPriorityFirst, insideDrawPrioritySecond, outsideDrawPriority;

		public DecalsBases(int insideDrawPriorityFirst, int insideDrawPrioritySecond, int outsideDrawPriority) {
			this.insideDrawPriorityFirst = insideDrawPriorityFirst;
			this.insideDrawPrioritySecond = insideDrawPrioritySecond;
			this.outsideDrawPriority = outsideDrawPriority;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockSubShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (showDecals) {
				int dp = outside ? outsideDrawPriority : insideDrawPriorityFirst;
				int dsz = ship.decals.size();
				for (int di = 0; di < dsz; di++) {
					Decal dec = ship.decals.get(di);
					if (!dec.enabled || dec.layer != dp) { continue; } 
					dec.type.drawBase(d,
							x + ship.gridXToWorldX(dec.x, dec.type.imgW) * AGame.SGS,
							y + dec.y * AGame.SGS,
							ms + dec.animOffset, ship.flipped, coa, ship.getName(), light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs,
							dec.paint == null ? null : dec.paint.getPaintType(coa));
				}
				if (!outside) {
					dp = insideDrawPrioritySecond;
					for (int di = 0; di < dsz; di++) {
						Decal dec = ship.decals.get(di);
						if (!dec.enabled || dec.layer != dp) { continue; } 
						dec.type.drawBase(d,
								x + ship.gridXToWorldX(dec.x, dec.type.imgW) * AGame.SGS,
								y + dec.y * AGame.SGS,
								ms + dec.animOffset, ship.flipped, coa, ship.getName(), light, lightStrength, ambient, ambientSaturation,
								ssb, additionalSSBs,
								dec.paint == null ? null : dec.paint.getPaintType(coa));
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockSubShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("heraldry"); // Must be spritesheet, or change alongside dec.type.drawBase
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DecalArmsBases implements UniScreen.ShipLayer {
		private final int insideDrawPriorityFirst, insideDrawPrioritySecond, outsideDrawPriority;

		public DecalArmsBases(int insideDrawPriorityFirst, int insideDrawPrioritySecond, int outsideDrawPriority) {
			this.insideDrawPriorityFirst = insideDrawPriorityFirst;
			this.insideDrawPrioritySecond = insideDrawPrioritySecond;
			this.outsideDrawPriority = outsideDrawPriority;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (showDecals) {
				int dp = outside ? outsideDrawPriority : insideDrawPriorityFirst;
				int dsz = ship.decals.size();
				for (int di = 0; di < dsz; di++) {
					Decal dec = ship.decals.get(di);
					if (!dec.enabled || dec.layer != dp) { continue; } 
					dec.type.drawArmsBase(d,
							x + ship.gridXToWorldX(dec.x, dec.type.w) * AGame.SGS,
							y + dec.y * AGame.SGS,
							ms + dec.animOffset, ship.flipped, coa, ship.getName(), light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs);
				}
				if (!outside) {
					dp = insideDrawPrioritySecond;
					for (int di = 0; di < dsz; di++) {
						Decal dec = ship.decals.get(di);
						if (!dec.enabled || dec.layer != dp) { continue; } 
						dec.type.drawArmsBase(d,
								x + ship.gridXToWorldX(dec.x, dec.type.w) * AGame.SGS,
								y + dec.y * AGame.SGS,
								ms + dec.animOffset, ship.flipped, coa, ship.getName(), light, lightStrength, ambient, ambientSaturation,
								ssb, additionalSSBs);
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("heraldry"); // Must be spritesheet, or change alongside dec.type.drawBase
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DecalsCharges implements UniScreen.ShipLayer {
		
		public final int layer;

		public DecalsCharges(int layer) {
			this.layer = layer;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (!outside) { return; }
			if (showDecals) {
				int dsz = ship.decals.size();
				for (int di = 0; di < dsz; di++) {
					Decal dec = ship.decals.get(di);
					if (!dec.enabled || dec.layer != layer) { continue; }
					dec.type.drawCharges(d,
							x + ship.gridXToWorldX(dec.x, dec.type.w) * AGame.SGS,
							y + dec.y * AGame.SGS,
							ms, ship.flipped, coa, ship.getName(), light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DecalsNonShader implements UniScreen.ShipLayer {
		
		public final int layer;

		public DecalsNonShader(int layer) {
			this.layer = layer;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (!outside) { return; }
			if (showDecals) {
				int dsz = ship.decals.size();
				for (int di = 0; di < dsz; di++) {
					Decal dec = ship.decals.get(di);
					if (!dec.enabled || dec.layer != layer) { continue; }
					dec.type.drawUnlitCharges(d,
							x + ship.gridXToWorldX(dec.x, dec.type.w) * AGame.SGS,
							y + dec.y * AGame.SGS,
							ms, ship.flipped, coa);
					dec.type.drawNonShader(d,
							x + ship.gridXToWorldX(dec.x, dec.type.w) * AGame.SGS,
							y + dec.y * AGame.SGS,
							ms, ship.flipped, coa, ship.getName(), ambientClr);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Externals implements UniScreen.ShipLayer {
		private final int drawPriority;
		
		public Externals(int drawPriority) {
			this.drawPriority = drawPriority;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockSubShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if ((m.hp > 0 || m.type.hasSpecificDestroyedExternalAppearances()) && m.type.getExternalDrawPriority() == drawPriority) {
					if (!outside && !m.type.drawExternalsWhenInside()) { continue; }
					m.type.drawExternal(d,
							x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + (m.y) * AGame.SGS,
							m.time + m.animOffset,
							ship.flipped,
							m.variant, ship.currentBonuses,
							light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs,
							m.hp < m.maxHP / 2, m.hp <= 0,
							m.externalPaint == null ? null : m.externalPaint.getPaintType(coa));
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockSubShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class DamagedExternals implements UniScreen.ShipLayer {
		private final int drawPriority;
		
		public DamagedExternals(int drawPriority) {
			this.drawPriority = drawPriority;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			if (ssb.getDamagedVersion() != null) {
				Appearance.lockSubShader(ssb.getDamagedVersion(), d, light, lightStrength, ambient, ambientSaturation);
			} else {
				Appearance.lockSubShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
			}
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (!outside) { return; }
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if (m.hp <= 0 && m.type.getExternalDrawPriority() == drawPriority && !m.type.hasSpecificDestroyedExternalAppearances()) {
					m.type.drawExternal(d,
							x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
							y + (m.y) * AGame.SGS,
							m.time + m.animOffset,
							ship.flipped,
							m.variant, ship.currentBonuses,
							light, lightStrength, ambient, ambientSaturation,
							ssb, additionalSSBs,
							false, false,
							m.externalPaint == null ? null : m.externalPaint.getPaintType(coa));
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockSubShader(light != null);
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Flags implements UniScreen.ShipLayer {
		private static boolean flagShaderLoadFailed = false;
		
		private final int layer;

		public Flags(int layer) {
			this.layer = layer;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			shaderLocked = false;
			if (Appearance.useSimpleGraphics || shaderLoadFailed || flagShaderLoadFailed || !flagsDrawn) {
				return;
			}
			if (light != null) {
				if (lsp == null) {
					try {
						lsp = ShaderProgram.loadProgram(
								AGame.getStaticGameDirectoryPath("data/flag.vert"),
								AGame.getStaticGameDirectoryPath("data/litflag.frag"));
					} catch (Exception e) {
						AirshipGame.instance.reportError("Flag shader load failed", e, null, false, true);
						flagShaderLoadFailed = true;
						return;
					}
				}
				lsp.bind();

				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, flagsImg.getTexture().getTextureID());
				lsp.setUniform1i("tex", 0);
				
				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
				lsp.setUniform1i("lightFromLeft", 2);

				glActiveTexture(GL_TEXTURE3);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
				lsp.setUniform1i("lightFromRight", 3);

				lsp.enableVertexAttribute("flagSize");
				lsp.enableVertexAttribute("texOffset");
				lsp.enableVertexAttribute("awind");
				lsp.enableVertexAttribute("at");
				lsp.enableVertexAttribute("ayShift");
				
				lsp.setUniform1f("strength", lightStrength);
				lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
				lsp.setUniform1f("screenHeight", d.frame().mode().height);
				lsp.setUniform4f("ambient", ambient);
				lsp.setUniform1f("ambientSaturation", ambientSaturation);

				glBegin(GL_QUADS);
			} else {
				if (sp == null) {
					try {
						sp = ShaderProgram.loadProgram(
								AGame.getStaticGameDirectoryPath("data/flag.vert"),
								AGame.getStaticGameDirectoryPath("data/flag.frag"));
					} catch (Exception e) {
						AirshipGame.instance.reportError("Flag shader load failed", e, null, false, true);
						flagShaderLoadFailed = true;
						return;
					}
				}
				sp.bind();

				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, flagsImg.getTexture().getTextureID());
				sp.setUniform1i("tex", 0);

				sp.enableVertexAttribute("flagSize");
				sp.enableVertexAttribute("texOffset");
				sp.enableVertexAttribute("awind");
				sp.enableVertexAttribute("at");
				sp.enableVertexAttribute("ayShift");

				glBegin(GL_QUADS);
			}
			shaderLocked = true;
		}
		
		private static ShaderProgram sp;
		private static ShaderProgram lsp;
		
		private static boolean flagsDrawn = false;
		private static boolean flagsDrawnFailed = false;
		private static Image flagsImg;
		private static CoatOfArms currentCOA0, currentCOA1;
		
		private boolean shaderLocked = false;
				
		public static void updateFlags(MyDraw d, CoatOfArms coa0, CoatOfArms coa1) {
			if ((coa0 != null && !coa0.equals(currentCOA0)) || (coa1 != null && !coa1.equals(currentCOA1))) {
				if (flagsImg == null) {
					try {
						flagsImg = new Image(64, 64);
					} catch (SlickException e) {
						flagsDrawnFailed = true;
						AirshipGame.instance.reportError("Flags image could not be created.", e, null, false, true);
						return;
					}
				}
				
				if (coa0 == null) {
					return;
				}
				
				if (coa1 == null) {
					coa1 = coa0;
				}
				
				d.resetTransforms();
				coa0.draw(d, 0, 0, 32);
				coa1.draw(d, 32, 0, 32);
				((Graphics) d.frame().nativeRenderer()).copyArea(flagsImg, 0, 0);
				currentCOA0 = coa0;
				currentCOA1 = coa1;
				flagsDrawn = true;
			}
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (Appearance.useSimpleGraphics || shaderLoadFailed || flagShaderLoadFailed || flagsDrawnFailed || !shaderLocked || !flagsDrawn) {
				int dsz = ship.decals.size();
				for (int di = 0; di < dsz; di++) {
					Decal m = ship.decals.get(di);
					if (!m.enabled || m.layer != layer) { continue; }
					if (m.type.flag != null && m.type.flag.type == FlagSpec.Type.ARMS) {
						FlagSpec f = m.type.flag;
						double flagX = x + ship.gridXToWorldX(m.x, m.type.w) * AGame.SGS + (ship.flipped ? (m.type.w * AGame.SGS - f.x) : f.x);
						double flagY = y + m.y * AGame.SGS + f.y;
						coa.draw(d, flagX, flagY, f.size);
					}
				}
				return;
			}
			
			double wind = (weather == null ? 0 : weather.wind) - ship.smoothedXSpeed;
			wind += StrictMath.abs(ship.smoothedYSpeed) * StrictMath.signum(wind) * 0.5;
			wind *= 50;
			if (StrictMath.abs(wind) < 1) {
				double sig = StrictMath.signum(ship.flagFlipAccum);
				ship.flagFlipAccum += wind * ms * 0.001;
				if (StrictMath.signum(ship.flagFlipAccum) != sig) {
					ship.flagFlipAccum = StrictMath.signum(ship.flagFlipAccum);
				}
				wind = ship.flagFlipAccum > 0 ? 1 : -1;
			} else {
				ship.flagFlipAccum = StrictMath.signum(wind);
				if (wind > 0) {
					wind = StrictMath.min(20, StrictMath.max(1, wind));
				} else if (wind < 0) {
					wind = StrictMath.max(-20, StrictMath.min(-1, wind));
				}
			}
						
			double yShift = StrictMath.max(-0.5, StrictMath.min(0.5, -ship.getySpeed() * 5));
			
			ShaderProgram p = light == null ? sp : lsp;
			
			int dsz = ship.decals.size();
			for (int di = 0; di < dsz; di++) {
				Decal m = ship.decals.get(di);
				if (!m.enabled || m.layer != layer) { continue; }
				if (m.type.flag != null && m.type.flag.type == FlagSpec.Type.ARMS) {
					FlagSpec f = m.type.flag;
					double flagX = x + ship.gridXToWorldX(m.x, m.type.w) * AGame.SGS + (ship.flipped ? (m.type.w * AGame.SGS - f.x) : f.x);
					double flagY = y + m.y * AGame.SGS + f.y;
					GLCompat.glVertexAttrib1f(p.getAttributeID("flagSize"), f.size);
					GLCompat.glVertexAttrib2f(p.getAttributeID("texOffset"), side * 0.5f, 0);
					GLCompat.glVertexAttrib1f(p.getAttributeID("awind"), (float) wind);
					GLCompat.glVertexAttrib1f(p.getAttributeID("at"), (m.animOffset + ms) * -0.01f * StrictMath.signum((float) wind));
					GLCompat.glVertexAttrib1f(p.getAttributeID("ayShift"), (float) yShift);
					glTexCoord2d(-f.size, f.size);
					glVertex2d(flagX - f.size, flagY - f.size);
					glTexCoord2d(-f.size, -2 * f.size);
					glVertex2d(flagX - f.size, flagY + 2 * f.size);
					glTexCoord2d(f.size, -2 * f.size);
					glVertex2d(flagX + f.size, flagY + 2 * f.size);
					glTexCoord2d(f.size, f.size);
					glVertex2d(flagX + f.size, flagY - f.size);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			if (Appearance.useSimpleGraphics || shaderLoadFailed || !shaderLocked) {
				return;
			}
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			(light == null ? sp : lsp).unbind();
			TextureImpl.bindNone();
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	public static strictfp class Pennants implements UniScreen.ShipLayer {	
		private static boolean pennantShaderLoadFailed = false;
		
		private final int layer;

		public Pennants(int layer) {
			this.layer = layer;
		}
		
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			shaderLocked = false;
			if (Appearance.useSimpleGraphics || shaderLoadFailed || pennantShaderLoadFailed) {
				return;
			}
			if (light != null) {
				if (lsp == null) {
					try {
						lsp = ShaderProgram.loadProgram(
								AGame.getStaticGameDirectoryPath("data/pennant.vert"),
								AGame.getStaticGameDirectoryPath("data/litpennant.frag"));
					} catch (Exception e) {
						AirshipGame.instance.reportError("Pennant shader load failed", e, null, false, true);
						pennantShaderLoadFailed = true;
						return;
					}
				}
				lsp.bind();

				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
				lsp.setUniform1i("lightFromLeft", 2);

				glActiveTexture(GL_TEXTURE3);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
				lsp.setUniform1i("lightFromRight", 3);

				lsp.enableVertexAttribute("flagSize");
				lsp.enableVertexAttribute("acoord");
				lsp.enableVertexAttribute("awind");
				lsp.enableVertexAttribute("at");
				lsp.enableVertexAttribute("ayShift");
				
				lsp.setUniform1f("strength", lightStrength);
				lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
				lsp.setUniform1f("screenHeight", d.frame().mode().height);
				lsp.setUniform4f("ambient", ambient);
				lsp.setUniform1f("ambientSaturation", ambientSaturation);

				glBegin(GL_QUADS);
			} else {
				if (sp == null) {
					try {
						sp = ShaderProgram.loadProgram(
								AGame.getStaticGameDirectoryPath("data/pennant.vert"),
								AGame.getStaticGameDirectoryPath("data/pennant.frag"));
					} catch (Exception e) {
						AirshipGame.instance.reportError("Flag shader load failed", e, null, false, true);
						pennantShaderLoadFailed = true;
						return;
					}
				}
				sp.bind();
				
				sp.setUniform4f("ambient", ambient);

				sp.enableVertexAttribute("flagSize");
				sp.enableVertexAttribute("acoord");
				sp.enableVertexAttribute("awind");
				sp.enableVertexAttribute("at");
				sp.enableVertexAttribute("ayShift");

				glBegin(GL_QUADS);
			}
			shaderLocked = true;
		}
		
		private static ShaderProgram sp;
		private static ShaderProgram lsp;
		
		private boolean shaderLocked = false;

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			if (Appearance.useSimpleGraphics || shaderLoadFailed || pennantShaderLoadFailed) {
				int dsz = ship.decals.size();
				for (int di = 0; di < dsz; di++) {
					Decal m = ship.decals.get(di);
					if (!m.enabled || m.layer != layer) { continue; }
					if (m.type.flag != null && m.type.flag.type == FlagSpec.Type.PENNANT) {
						FlagSpec f = m.type.flag;
						double flagX = x + ship.gridXToWorldX(m.x, m.type.w) * AGame.SGS + (ship.flipped ? (m.type.w * AGame.SGS - f.x) : f.x);
						double flagY = y + m.y * AGame.SGS + f.y;
						d.rect(coa.getFirstColour().tint, flagX, flagY, f.size, f.size); // qqDPS clr
					}
				}
				return;
			}
			
			double wind = (weather == null ? 0 : weather.wind) - ship.smoothedXSpeed;
			wind += StrictMath.abs(ship.smoothedYSpeed) * StrictMath.signum(wind) * 0.5;
			wind *= 50;
			if (StrictMath.abs(wind) < 1) {
				double sig = StrictMath.signum(ship.flagFlipAccum);
				ship.flagFlipAccum += wind * ms * 0.001;
				if (StrictMath.signum(ship.flagFlipAccum) != sig) {
					ship.flagFlipAccum = StrictMath.signum(ship.flagFlipAccum);
				}
				wind = ship.flagFlipAccum > 0 ? 1 : -1;
			} else {
				ship.flagFlipAccum = StrictMath.signum(wind);
				if (wind > 0) {
					wind = StrictMath.min(20, StrictMath.max(1, wind));
				} else if (wind < 0) {
					wind = StrictMath.max(-20, StrictMath.min(-1, wind));
				}
			}
			
			double yShift = StrictMath.max(-0.5, StrictMath.min(0.5, -ship.getySpeed() * 5));
			
			ShaderProgram p = light == null ? sp : lsp;
			
			Color tint = coa.getFirstColour().tintColor;
			
			int dsz = ship.decals.size();
			for (int di = 0; di < dsz; di++) {
				Decal m = ship.decals.get(di);
				if (!m.enabled || m.layer != layer) { continue; }
				if (m.type.flag != null && m.type.flag.type == FlagSpec.Type.PENNANT) {
					FlagSpec f = m.type.flag;
					double flagX = x + ship.gridXToWorldX(m.x, m.type.w) * AGame.SGS + (ship.flipped ? (m.type.w * AGame.SGS - f.x) : f.x);
					double flagY = y + m.y * AGame.SGS + f.y;
					GLCompat.glVertexAttrib4f(p.getAttributeID("tint"), tint.r, tint.g, tint.b, tint.a);
					GLCompat.glVertexAttrib1f(p.getAttributeID("flagSize"), f.size);
					GLCompat.glVertexAttrib1f(p.getAttributeID("awind"), (float) wind);
					GLCompat.glVertexAttrib1f(p.getAttributeID("at"), (m.animOffset + ms) * -0.01f * StrictMath.signum((float) wind));
					GLCompat.glVertexAttrib1f(p.getAttributeID("ayShift"), (float) yShift);
					GLCompat.glVertexAttrib2f(p.getAttributeID("acoord"), -f.size, f.size);
					glVertex2d(flagX - f.size * 4, flagY - f.size);
					GLCompat.glVertexAttrib2f(p.getAttributeID("acoord"), -f.size, -2 * f.size);
					glVertex2d(flagX - f.size * 4, flagY + 2 * f.size);
					GLCompat.glVertexAttrib2f(p.getAttributeID("acoord"), f.size, -2 * f.size);
					glVertex2d(flagX + f.size * 4, flagY + 2 * f.size);
					GLCompat.glVertexAttrib2f(p.getAttributeID("acoord"), f.size, f.size);
					glVertex2d(flagX + f.size * 4, flagY - f.size);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			if (Appearance.useSimpleGraphics || shaderLoadFailed || !shaderLocked) {
				return;
			}
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			(light == null ? sp : lsp).unbind();
			TextureImpl.bindNone();
			//lsp = null;
			//sp = null;
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return false;
		}
	}
	
	static strictfp class CurvedSegment {
		Wheel w;
		double startAngle, endAngle;
		boolean endSegment;

		public CurvedSegment(Wheel w, double startAngle, double endAngle, boolean endSegment) {
			this.w = w;
			this.startAngle = startAngle;
			this.endAngle = endAngle;
			this.endSegment = endSegment;
		}
	}
	
	static strictfp class StraightSegment {
		double startX, startY, endX, endY;

		public StraightSegment(double startX, double startY, double endX, double endY) {
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
		}
	}
	
	static ArrayList<?> getTrackSegments(Airship ship, Module m) {
		ArrayList segs = new ArrayList();
		segs.add(getLeftmostCurvedSegment(m.wheels.get(0), m.wheels.get(1)));
		for (int wi = 0; wi < m.wheels.size() - 1; wi++) {
			segs.add(getStraightSegment(m.wheels.get(wi), m.wheels.get(wi + 1)));
			if (wi < m.wheels.size() - 2) {
				segs.add(getCurvedSegment(m.wheels.get(wi), m.wheels.get(wi + 1), m.wheels.get(wi + 2)));
			}
		}
		segs.add(getRightmostCurvedSegment(m.wheels.get(m.wheels.size() - 2), m.wheels.get(m.wheels.size() - 1)));
		for (int wi = m.wheels.size() - 1; wi >= 1; wi--) {
			segs.add(getStraightSegment(m.wheels.get(wi), m.wheels.get(wi - 1)));
			if (wi >= 2) {
				segs.add(getCurvedSegment(m.wheels.get(wi), m.wheels.get(wi - 1), m.wheels.get(wi - 2)));
			}
		}
		for (int si = 0; si < segs.size() - 2; si++) {
			if (segs.get(si) instanceof StraightSegment && segs.get(si + 1) instanceof CurvedSegment && !((CurvedSegment) segs.get(si + 1)).endSegment && segs.get(si + 2) instanceof StraightSegment) {
				StraightSegment seg1 = (StraightSegment) segs.get(si);
				StraightSegment seg2 = (StraightSegment) segs.get(si + 2);
				/*Pt intersect = Ln.intersection(seg1.startX, seg1.startY, seg1.endX, seg1.endY, seg2.startX, seg2.startY, seg2.endX, seg2.endY);
				if (intersect != null) {
					seg1.endX = intersect.x;
					seg1.endY = intersect.y;
					seg2.startX = intersect.x;
					seg2.startY = intersect.y;
				}*/
				if (seg2.endX > seg1.endX) {
					if (seg1.endX > seg2.startX) {
						double iSectX = (seg1.endX + seg2.startX) / 2;
						double iSectY = (seg1.endY + seg2.startY) / 2;
						seg1.endX = iSectX;
						seg1.endY = iSectY;
						seg2.startX = iSectX;
						seg2.startY = iSectY;
					}
				} else {
					if (seg2.endX > seg1.startX) {
						double iSectX = (seg2.endX + seg1.startX) / 2;
						double iSectY = (seg2.endY + seg1.startY) / 2;
						seg2.endX = iSectX;
						seg2.endY = iSectY;
						seg1.startX = iSectX;
						seg1.startY = iSectY;
					}
				}
			}
		}
		return segs;
	}
	
	static CurvedSegment getLeftmostCurvedSegment(Wheel w, Wheel to) {
		double wx = w.spec.xOffset * AGame.SGS;
		double wy = w.yOffset;
		double tox = to.spec.xOffset * AGame.SGS;
		double toy = to.yOffset;
		double wToToAngle = Direction.radiansFromTo(wx, wy, tox, toy);
		double startAngle = Direction.normalizeRadians(wToToAngle + StrictMath.PI / 2);
		return new CurvedSegment(w, startAngle, startAngle + StrictMath.PI, true);
	}
	
	static CurvedSegment getCurvedSegment(Wheel from, Wheel w, Wheel to) {
		double fromx = from.spec.xOffset * AGame.SGS;
		double fromy = from.yOffset;
		double wx = w.spec.xOffset * AGame.SGS;
		double wy = w.yOffset;
		double tox = to.spec.xOffset * AGame.SGS;
		double toy = to.yOffset;
		
		double fromToWAngle = Direction.radiansFromTo(fromx, fromy, wx, wy);
		double wToToAngle = Direction.radiansFromTo(wx, wy, tox, toy);
		
		double startAngle = Direction.normalizeRadians(fromToWAngle + StrictMath.PI * 3 / 2);
		double endAngle = Direction.normalizeRadians(wToToAngle + StrictMath.PI * 3 / 2);
		/*if (startAngle > endAngle + StrictMath.PI) {
			endAngle += StrictMath.PI * 2;
		}*/
		return new CurvedSegment(w, startAngle, endAngle, false);
	}
	
	static CurvedSegment getRightmostCurvedSegment(Wheel from, Wheel w) {
		double fromx = from.spec.xOffset * AGame.SGS;
		double fromy = from.yOffset;
		double wx = w.spec.xOffset * AGame.SGS;
		double wy = w.yOffset;
		double fromToWAngle = Direction.radiansFromTo(fromx, fromy, wx, wy);
		double startAngle = Direction.normalizeRadians(fromToWAngle + StrictMath.PI * 3 / 2);
		return new CurvedSegment(w, startAngle, startAngle + StrictMath.PI, true);
	}
	
	static StraightSegment getStraightSegment(Wheel from, Wheel to) {
		double fromx = from.spec.xOffset * AGame.SGS;
		double fromy = from.yOffset;
		double tox = to.spec.xOffset * AGame.SGS;
		double toy = to.yOffset;
		double fromToToAngle = Direction.radiansFromTo(fromx, fromy, tox, toy);
		double startAngle = fromToToAngle + StrictMath.PI * 3 / 2;
		double endAngle = fromToToAngle + StrictMath.PI * 3 / 2;
		return new StraightSegment(
				fromx + StrictMath.cos(startAngle) * from.spec.radius,
				fromy + StrictMath.sin(startAngle) * from.spec.radius,
				tox + StrictMath.cos(endAngle) * to.spec.radius,
				toy + StrictMath.sin(endAngle) * to.spec.radius
		);
	}
	
	public static final strictfp class AnimatedWheels implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			Appearance.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules2");
		}

		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}

		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public void draw(Airship ship, WeatherEffect we, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			Graphics g = (Graphics) d.frame().nativeRenderer();
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				double mx = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
				double my = y + m.y * AGame.SGS + m.type.getH() * AGame.SGS;
				if (m.hp > 0) {
					int wsz = m.wheels.size();
					for (int wi = 0; wi < wsz; wi++) {
						Wheel w = m.wheels.get(wi);
						if (w.spec.app != null) {
							if (w.spec.app.spritesheetBundle != ssb) {
								if (additionalSSBs != null) {
									additionalSSBs.add(w.spec.app.spritesheetBundle);
								}
								continue;
							}
							/*g.setColor(new Color(0, 255, 0, 180));
								g.fillOval(
										(float) (mx + w.spec.xOffset * AGame.SGS - w.spec.radius),
										(float) (my + w.yOffset - w.spec.radius),
										(float) w.spec.radius * 2,
										(float) w.spec.radius * 2);*/
							w.spec.app.draw(d,
									mx + w.spec.xOffset * AGame.SGS + w.spec.appX,
									my + w.yOffset + w.spec.appY,
									(int) (Direction.normalizeRadians(w.phase) / Math.PI * 180), ship.flipped,
									light, lightStrength, ambient, ambientSaturation);
						}
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			Appearance.unlockShader(light != null);
		}

		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return true;
		}
	}
	
	public static final strictfp class AnimatedWheelLeads implements UniScreen.ShipLayer {
		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {}

		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules2");
		}

		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}

		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public void draw(Airship ship, WeatherEffect we, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			Graphics g = (Graphics) d.frame().nativeRenderer();
			int msz = ship.modules.size();
			g.setColor(new Color(ambientClr.r / 2 + 40, ambientClr.g / 2 + 40, ambientClr.b / 2 + 40));
			float lw = StrictMath.max(1, (float) (1 * scale));
			g.setLineWidth(lw);
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				double mx = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
				double mStartY = y + m.y * AGame.SGS;
				double my = y + m.y * AGame.SGS + m.type.getH() * AGame.SGS;
				if (m.hp > 0) {
					int wsz = m.wheels.size();
					for (int wi = 0; wi < wsz; wi++) {
						Wheel w = m.wheels.get(wi);
						if (w.spec.app != null && w.spec.leadStart != null) {
							float lsx = (float) (mx + (ship.flipped ? (m.type.getW() * AGame.SGS - w.spec.leadStart.x) : w.spec.leadStart.x));
							float lsy = (float) (mStartY + w.spec.leadStart.y);
							float lex = (float) (mx + w.spec.xOffset * AGame.SGS + w.spec.appX + (ship.flipped ? w.spec.app.width() * AGame.SGS - w.spec.leadEndOffset.x : w.spec.leadEndOffset.x));
							float ley = (float) (my + w.yOffset + w.spec.appY + w.spec.leadEndOffset.y);
							g.drawLine(lsx, lsy, lex, ley);
						}
					}
				}
			}
			g.setLineWidth(1);
		}

		@Override
		public void unlockShader(Image[] light, double scale) {}

		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return true;
		}
	}
		
	public static final strictfp class TracksAndLegs implements UniScreen.ShipLayer {
		public final boolean backLegs;

		public TracksAndLegs(boolean backLegs) {
			this.backLegs = backLegs;
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			RotatingColoringShader.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
			//Graphics g = (Graphics) d.frame().nativeRenderer();
			
			if (backLegs) {
				lightStrength *= 0.75f;
				ambient = new Color(ambientClr.r * 3 / 4, ambientClr.g * 3 / 4, ambientClr.b * 3 / 4, ambientClr.a);
				ambientClr = new Clr(ambientClr.r * 3 / 4, ambientClr.g * 3 / 4, ambientClr.b * 3 / 4, ambientClr.a);
			}
			
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				double mx = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
				double my = y + m.y * AGame.SGS + m.type.getH() * AGame.SGS;
				
				PaintType subPaintType = m.externalPaint == null ? null : m.externalPaint.getPaintType(coa);
				Clr subSrc = m.type.externalSubColor;
				Clr subTrg = null;
				if (m.type.externalSubColor != null) {
					subTrg = subPaintType == null ? subSrc : m.type.externalSubColorByPaintIndex[subPaintType.ordinal()];
				}
				float[] subSrcF = subSrc == null ? new float[] {0, 0, 0} : new float[] {subSrc.r / 255.0f, subSrc.g / 255.0f, subSrc.b / 255.0f};
				float[] subTrgF = subTrg == null ? new float[] {0, 0, 0} : new float[] {subTrg.r / 255.0f, subTrg.g / 255.0f, subTrg.b / 255.0f};
				
				if (m.hp > 0) {
					if (!backLegs) {
						int wsz = m.wheels.size();
						for (int wi = 0; wi < wsz; wi++) {
							Wheel w = m.wheels.get(wi);
							if (w.spec.wheel != null) {
								if (!w.spec.wheel.src.equals(ssb.name)) {
									if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, w.spec.wheel.src)) {
										additionalSSBs.add(SpritesheetBundle.ofName(w.spec.wheel.src));
									}
								} else {
									if (w.spec.wheel.machineImgCache != null) {
										((Image) w.spec.wheel.machineImgCache).setFilter(Image.FILTER_NEAREST);
									}
									RotatingColoringShader.draw(ssb, w.spec.wheel, d,
											mx + w.spec.xOffset * AGame.SGS - w.spec.wheel.srcWidth / 2,
											my + w.yOffset - w.spec.wheel.srcHeight / 2,
											w.phase, 1, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr,
											subSrcF, subTrgF, subSrcF, subTrgF);
								}
							}
						}

						if (!m.wheels.isEmpty()) {
							Wheel.Spec ws = m.wheels.get(0).spec;
							if (ws.upperLink != null) {
								if (!ws.upperLink.src.equals(ssb.name)) {
									if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, ws.upperLink.src)) {
										additionalSSBs.add(SpritesheetBundle.ofName(ws.upperLink.src));
									}
								} else {
									double segmentStride = ws.segmentStride;
									double strideRadians = segmentStride / m.wheels.get(0).spec.radius;
									ArrayList segs = getTrackSegments(ship, m);
									for (int pass = 0; pass < 2; pass++) {
										double value = m.wheels.get(0).phase * segmentStride / strideRadians;
										if (value < 0) {
											value += StrictMath.ceil(-value / segmentStride / 2) * segmentStride * 2;
										}
										value %= segmentStride * 2;
										boolean flipAlt = value >= segmentStride;
										value %= segmentStride;
										boolean second = pass == 1;
										boolean alternation = second ^ flipAlt;
										int segsS = segs.size();
										for (int segI = 0; segI < segsS; segI++) {
											Object seg = segs.get(segI);
											if (seg instanceof StraightSegment) {
												StraightSegment ss = (StraightSegment) seg;
												double dist = StrictMath.sqrt((ss.startX - ss.endX) * (ss.startX - ss.endX) + (ss.startY - ss.endY) * (ss.startY - ss.endY));
												double angle = Direction.radiansFromTo(ss.startX, ss.startY, ss.endX, ss.endY);
												for (; value < dist; value += segmentStride) {
													alternation = !alternation;
													if (alternation) { continue; }
													Img lnk = second ? ws.upperLink : ws.lowerLink;
													if (lnk.machineImgCache != null) {
														((Image) lnk.machineImgCache).setFilter(Image.FILTER_NEAREST);
													}
													RotatingColoringShader.draw(ssb, lnk, d,
															mx + ss.startX + (ss.endX - ss.startX) * value / dist - lnk.srcWidth / 2,
															my + ss.startY + (ss.endY - ss.startY) * value / dist - lnk.srcHeight / 2,
															angle + StrictMath.PI, 1, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr,
															subSrcF, subTrgF, subSrcF, subTrgF);
												}

												value -= dist;
											}
											if (seg instanceof CurvedSegment) {
												CurvedSegment cs = (CurvedSegment) seg;
												if (cs.startAngle >= cs.endAngle) { continue; }
												Wheel w = cs.w;
												value = cs.startAngle + value * strideRadians / segmentStride;
												for (; value < cs.endAngle; value += strideRadians) {
													alternation = !alternation;
													if (alternation) { continue; }
													Img lnk = second ? ws.upperLink : ws.lowerLink;
													if (lnk.machineImgCache != null) {
														((Image) lnk.machineImgCache).setFilter(Image.FILTER_NEAREST);
													}
													RotatingColoringShader.draw(ssb, lnk, d,
															mx + w.spec.xOffset * AGame.SGS + StrictMath.cos(value) * w.spec.radius - lnk.srcWidth / 2,
															my + w.yOffset + StrictMath.sin(value) * w.spec.radius - lnk.srcHeight / 2,
															value + StrictMath.PI * 3 / 2, 1, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr,
															subSrcF, subTrgF, subSrcF, subTrgF);
												}
												value = (value - cs.endAngle) * segmentStride / strideRadians;
											}
										}
									}
								}
							}

							/*for (Wheel w : m.wheels) {
								g.setColor(new Color(255, 0, 0, 180));
								g.fillOval(
										(float) (mx + w.spec.xOffset * AGame.SGS - w.spec.radius),
										(float) (my + w.yOffset - w.spec.radius),
										(float) w.spec.radius * 2,
										(float) w.spec.radius * 2);
							}*/
						}
					}
				}
				
				if (m.type.isSpider() && SimplePref.ARACHNOPHOBIA_MODE.get()) { continue; }
				int lsz = m.legs.size();
				for (int li = 0; li < lsz; li++) {
					Leg l = m.legs.get(li);
					if (l.spec.back != backLegs) { continue; }
					double hipX =
							ship.flipped
							? mx + (m.type.getW() - l.spec.xOffset) * AGame.SGS
							: mx + l.spec.xOffset * AGame.SGS;
					if (l.spec.upperLeg != null) {
						if (!l.spec.upperLeg.src.equals(ssb.name)) {
							if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, l.spec.upperLeg.src)) {
								additionalSSBs.add(SpritesheetBundle.ofName(l.spec.upperLeg.src));
							}
						} else {
							Img ul = (light == null || Appearance.useSimpleGraphics) && ship.flipped ? l.spec.upperLeg.flip() : l.spec.upperLeg;
							if (ul.machineImgCache != null) {
								((Image) ul.machineImgCache).setFilter(Image.FILTER_NEAREST);
							}
							
							RotatingColoringShader.draw(ssb, ul, d,
									hipX - l.spec.upperLeg.srcWidth / 2,
									my + l.spec.yOffset * AGame.SGS - l.spec.upperLeg.srcHeight / 2,
									l.upperRotation + (ship.flipped ? Math.PI : 0), 1, ship.flipped, ship.flipped, light, lightStrength, ambient, ambientSaturation, ambientClr,
									subSrcF, subTrgF, subSrcF, subTrgF);
						}
						if (l.spec.middleLeg != null) {
							if (!l.spec.middleLeg.src.equals(ssb.name)) {
								if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, l.spec.middleLeg.src)) {
									additionalSSBs.add(SpritesheetBundle.ofName(l.spec.middleLeg.src));
								}
							} else {
								Img ml = (light == null || Appearance.useSimpleGraphics) && ship.flipped ? l.spec.middleLeg.flip() : l.spec.middleLeg;
								if (ml.machineImgCache != null) {
									((Image) ml.machineImgCache).setFilter(Image.FILTER_NEAREST);
								}
								RotatingColoringShader.draw(ssb, ml, d,
										hipX + StrictMath.cos(l.upperRotation) * l.spec.upperLimbLength - (ship.flipped ^ l.spec.bendForwards ? l.spec.middleLeg.srcWidth / 2 - l.spec.middleLimbLength / 2 : l.spec.middleLimbLength / 2 + l.spec.middleLeg.srcWidth / 2),
										my + l.spec.yOffset * AGame.SGS + StrictMath.sin(l.upperRotation) * l.spec.upperLimbLength - l.spec.middleLeg.srcHeight / 2,
										ship.flipped ^ l.spec.bendForwards ? 0 : StrictMath.PI + (ship.flipped ? Math.PI : 0), 1, ship.flipped, ship.flipped, light, lightStrength, ambient, ambientSaturation, ambientClr,
										subSrcF, subTrgF, subSrcF, subTrgF);
							}
						}
						if (!l.spec.lowerLeg.src.equals(ssb.name)) {
							if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, l.spec.lowerLeg.src)) {
								additionalSSBs.add(SpritesheetBundle.ofName(l.spec.lowerLeg.src));
							}
						} else {
							Img ll = (light == null || Appearance.useSimpleGraphics) && ship.flipped ? l.spec.lowerLeg.flip() : l.spec.lowerLeg;
							if (ll.machineImgCache != null) {
								((Image) ll.machineImgCache).setFilter(Image.FILTER_NEAREST);
							}
							RotatingColoringShader.draw(ssb, ll, d,
									hipX + StrictMath.cos(l.upperRotation) * l.spec.upperLimbLength + (ship.flipped ^ l.spec.bendForwards ? l.spec.middleLimbLength : -l.spec.middleLimbLength) - l.spec.lowerLeg.srcWidth / 2,
									my + l.spec.yOffset * AGame.SGS + StrictMath.sin(l.upperRotation) * l.spec.upperLimbLength - l.spec.lowerLeg.srcHeight / 2,
									l.lowerRotation + (ship.flipped ? Math.PI : 0), 1, ship.flipped, ship.flipped, light, lightStrength, ambient, ambientSaturation, ambientClr,
									subSrcF, subTrgF, subSrcF, subTrgF);
						}
						if (!l.spec.foot.src.equals(ssb.name)) {
							if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, l.spec.foot.src)) {
								additionalSSBs.add(SpritesheetBundle.ofName(l.spec.foot.src));
							}
						} else {
							Img ft = (light == null || Appearance.useSimpleGraphics) && ship.flipped ? l.spec.foot.flip() : l.spec.foot;
							if (ft.machineImgCache != null) {
								((Image) ft.machineImgCache).setFilter(Image.FILTER_NEAREST);
							}
							RotatingColoringShader.draw(ssb, ft, d,
									hipX + StrictMath.cos(l.upperRotation) * l.spec.upperLimbLength + (ship.flipped ^ l.spec.bendForwards ? l.spec.middleLimbLength : -l.spec.middleLimbLength) + StrictMath.cos(l.lowerRotation) * l.spec.lowerLimbLength - l.spec.foot.srcWidth / 2,
									my + l.spec.yOffset * AGame.SGS + StrictMath.sin(l.upperRotation) * l.spec.upperLimbLength + StrictMath.sin(l.lowerRotation) * l.spec.lowerLimbLength - l.spec.foot.srcHeight / 2,
									ship.flipped ? -StrictMath.PI / 2 : StrictMath.PI / 2, 1, ship.flipped, ship.flipped, light, lightStrength, ambient, ambientSaturation, ambientClr,
									subSrcF, subTrgF, subSrcF, subTrgF);
						}
					} else {
						d.shift(mx + l.spec.xOffset * AGame.SGS, my + l.spec.yOffset * AGame.SGS);
						d.rotate(l.upperRotation * 180 / StrictMath.PI);
						d.rect(Clr.RED,
								-l.spec.upperLimbLength / 8,
								-l.spec.upperLimbLength / 8,
								l.spec.upperLimbLength,
								l.spec.upperLimbLength / 4);
						d.rotate(-l.upperRotation * 180 / StrictMath.PI);
						d.shift(StrictMath.cos(l.upperRotation) * l.spec.upperLimbLength, StrictMath.sin(l.upperRotation) * l.spec.upperLimbLength);
						d.rotate(l.lowerRotation * 180 / StrictMath.PI);
						d.rect(Clr.RED,
								-l.spec.lowerLimbLength / 8,
								-l.spec.lowerLimbLength / 8,
								l.spec.lowerLimbLength,
								l.spec.lowerLimbLength / 4);
						d.rotate(-l.lowerRotation * 180 / StrictMath.PI);
						d.shift(-StrictMath.cos(l.upperRotation) * l.spec.upperLimbLength, -StrictMath.sin(l.upperRotation) * l.spec.upperLimbLength);
						d.shift(-mx - l.spec.xOffset * AGame.SGS, -my - l.spec.yOffset * AGame.SGS);
					}
					//d.rect(Clr.RED, l.foot.x - 1, l.foot.y - 1, l.foot.w + 2, l.foot.h + 2);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			RotatingColoringShader.unlockShader();
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("wheels_and_legs");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return true;
		}
	}
	
	public static final class Tentacles implements UniScreen.ShipLayer {
		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			RotatingColoringShader.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {			
			int msz = ship.modules.size();
			double xShift = x - ship.getX();
			double yShift = y - ship.getY();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if ((m.hp > 0 || m.type.tentacleDeathSpasms()) && !m.tentacles.isEmpty()) {
					PaintType subPaintType = m.externalPaint == null ? null : m.externalPaint.getPaintType(coa);
					Clr subSrc = m.type.externalSubColor;
					Clr subTrg = null;
					if (m.type.externalSubColor != null) {
						subTrg = subPaintType == null ? subSrc : m.type.externalSubColorByPaintIndex[subPaintType.ordinal()];
					}
					float[] subSrcF = subSrc == null ? new float[] {0, 0, 0} : new float[] {subSrc.r / 255.0f, subSrc.g / 255.0f, subSrc.b / 255.0f};
					float[] subTrgF = subTrg == null ? new float[] {0, 0, 0} : new float[] {subTrg.r / 255.0f, subTrg.g / 255.0f, subTrg.b / 255.0f};
					int tsz = m.tentacles.size();
					for (int ti = 0; ti < tsz; ti++) {
						Tentacle t = m.tentacles.get(ti);
						int ssz = t.segments.size();
						for (int si = 0; si < ssz; si++) {
							Tentacle.Segment seg = t.segments.get(si);
							Img segImg = t.flipped ? t.spec.segmentImgVerticallyFlipped : t.spec.segmentImg;
							if (t.spec.segmentImgOverride[si] != null) {
								segImg = t.flipped ? t.spec.segmentImgOverrideVerticallyFlipped[si] : t.spec.segmentImgOverride[si];
							}
							if (!segImg.src.equals(ssb.name)) {
								if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, segImg.src)) {
									additionalSSBs.add(SpritesheetBundle.ofName(segImg.src));
								}
							} else {
								if (segImg.machineImgCache != null) {
									((Image) segImg.machineImgCache).setFilter(Image.FILTER_NEAREST);
								}
								double xScale = seg.getLength() * 1.0 / t.spec.imgLength;
								double yScale = seg.getWidth() * 1.0 / segImg.srcHeight;
								double tentScale = xScale / 2 + yScale / 2;
								RotatingColoringShader.draw(ssb, segImg, d,
										xShift + seg.centerX + seg.xOffset - segImg.srcWidth * tentScale * 0.5, yShift + seg.centerY + seg.yOffset - segImg.srcHeight * tentScale * 0.5, seg.angle, tentScale, false, false, light, lightStrength, ambient, ambientSaturation, ambientClr,
										subSrcF, subTrgF, subSrcF, subTrgF);
							}
						}
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			RotatingColoringShader.unlockShader();
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("monsters");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return true;
		}
	}
	
	public static final class TentacleInfo implements UniScreen.ShipLayer {
		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {			
			for (Module m : ship.modules) {
				if (m.hp > 0) {
					for (Tentacle t : m.tentacles) {
						d.rect(Clr.RED, t.goalX - 2, t.goalY - 2, 5, 5);
					}
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("spritesheet");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return true;
		}
	}
	
	public static final class Shells implements UniScreen.ShipLayer {
		public final boolean outsideLayer;

		public Shells(boolean outside) {
			this.outsideLayer = outside;
		}
		
		@Override
		public boolean doDraw(double scale) {
			return true;
		}

		@Override
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
			RotatingColoringShader.lockShader(ssb, d, light, lightStrength, ambient, ambientSaturation);
		}

		@Override
		public void draw(final Airship ship, WeatherEffect weather, int side, MyDraw d, boolean outside, double scale, double x, double y, int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {			
			int msz = ship.modules.size();
			for (int mi = 0; mi < msz; mi++) {
				Module m = ship.modules.get(mi);
				if (m.hp <= 0 || !m.type.isWeapon()) { continue; }
				WeaponAppearance wa = m.type.weaponAppearance(ship.currentBonuses);
				
				if (wa.shells.isEmpty()) { continue; }
				PaintType subPaintType = m.externalPaint == null ? null : m.externalPaint.getPaintType(coa);
				Clr subSrc = m.type.externalSubColor;
				Clr subTrg = null;
				if (m.type.externalSubColor != null) {
					subTrg = subPaintType == null ? subSrc : m.type.externalSubColorByPaintIndex[subPaintType.ordinal()];
				}
				float[] subSrcF = subSrc == null ? new float[] {0, 0, 0} : new float[] {subSrc.r / 255.0f, subSrc.g / 255.0f, subSrc.b / 255.0f};
				float[] subTrgF = subTrg == null ? new float[] {0, 0, 0} : new float[] {subTrg.r / 255.0f, subTrg.g / 255.0f, subTrg.b / 255.0f};
				
				for (int si = 0; si < wa.shells.size(); si++) {
					Shell shell = wa.shells.get(si);
					if (ship.flipped) {
						shell = shell.flipped(m.type.getW());
					}
					Img img = null;
					if (outsideLayer) {
						if (outside) {
							img = shell.img;
						} else if (shell.internalImg == null) {
							img = shell.img;
						}
					} else {
						img = shell.internalImg;
					}
					if (img == null) { continue; }
					if (!img.src.equals(ssb.name)) {
						if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, img.src)) {
							additionalSSBs.add(SpritesheetBundle.ofName(img.src));
						}
						continue;
					}
					if (img.machineImgCache != null) {
						((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
					}
					double openness = shell.getOpenness(m);
					double shellA = openness * shell.openAngle;
					double shellX = x + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + shell.x + openness * shell.openShiftX;
					double shellY = y + m.y * AGame.SGS + shell.y + openness * shell.openShiftY;
					
					double pivotOffsetX = shell.pivotX - img.srcWidth / 2;
					double pivotOffsetY = shell.pivotY - img.srcHeight / 2;
					shellX -= Math.cos(shellA) * pivotOffsetX - Math.sin(shellA) * pivotOffsetY;
					shellY -= Math.sin(shellA) * pivotOffsetX + Math.cos(shellA) * pivotOffsetY;
					shellX += pivotOffsetX;
					shellY += pivotOffsetY;
					
					RotatingColoringShader.draw(ssb, light != null && !Appearance.useSimpleGraphics && img.flipped ? img.flip() : img, d,
							shellX, shellY, shellA, 1, img.flipped, img.flipped, light, lightStrength, ambient, ambientSaturation, ambientClr,
							subSrcF, subTrgF, subSrcF, subTrgF);
				}
			}
		}

		@Override
		public void unlockShader(Image[] light, double scale) {
			RotatingColoringShader.unlockShader();
		}
		
		@Override
		public SpritesheetBundle getBaseSSB() {
			return SpritesheetBundle.ofName("modules");
		}
		
		@Override
		public SpritesheetBundle getBaseSSB2() {
			return null;
		}
		
		@Override
		public boolean drawEvenIfShipOutsideCropRect() {
			return true;
		}
	}
	
	public static final ArrayList<UniScreen.ShipLayer> ALL = new ArrayList<UniScreen.ShipLayer>(Arrays.asList(new UniScreen.ShipLayer[] {
		new TracksAndLegs(true),
		new Shells(false),
		new DamagedBackExternals(-1, 2),
		new BackExternals(-1, 2),
		new Pennants(0),
		new Flags(0),
		new DecalArmsBases(1, 0, 0),
		new DecalsBases(1, 0, 0),
		new DecalsCharges(0),
		new DecalsNonShader(0),
		new DamagedBackExternals(0, 1),
		new BackExternals(0, 1),
		new Splinters(),
		new BackArmour(),
		new ModuleBacks(),
		new Barrels(false),
		new Modules(),
		new DamagedModules(),
		new DetailedCrew(),
		new HPBars(),
		new Armour(),
		new Paint(),
		new Externals(0),
		new DamagedExternals(0),
		new DecalsBases(-1, -1, 1),
		new DecalArmsBases(-1, -1, 1),
		new DecalsCharges(1),
		new DecalsNonShader(1),
		new Barrels(true),
		new Externals(1),
		new DamagedExternals(1),
		new Flags(1),
		new Pennants(1),
		new Tentacles(),
		new Externals(2),
		new DamagedExternals(2),
		new Shells(true),
		new AnimatedWheels(),
		new AnimatedWheelLeads(),
		new TracksAndLegs(false)
	}));
}
