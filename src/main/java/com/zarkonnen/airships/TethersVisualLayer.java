package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;

import java.util.HashSet;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

public class TethersVisualLayer implements UniScreen.VisualLayer {
	@Override
	public void tick(Input in, int ms, UniScreen us) {}
	
	private final HashSet<SpritesheetBundle> additionalSSBs = new HashSet<SpritesheetBundle>();
	
	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (us.combat == null) { return; }
		TimeOfDay tod = us.getTimeOfDay();
		Color ambient = us.ambient(tod);
		Graphics g = (Graphics) d.frame().nativeRenderer();
		for (Combat.Side s : us.combat.sides) {
			for (Airship ship : s.ships) {
				for (Module m : ship.modules) {
					Tether t = m.tether;
					if (t != null) {
						Color c = t.spec.color;
						float red = c.r * ambient.r;
						float green = c.g * ambient.g;
						float blue = c.b * ambient.b;
						float grey = (red + green + blue) / 3;
						c = new Color(
								red * tod.ambientSaturation + grey * (1 - tod.ambientSaturation) + (tod.light[0].r + tod.light[1].r + tod.light[2].r + tod.light[3].r) / 4 * us.lightStrength(tod),
								green * tod.ambientSaturation + grey * (1 - tod.ambientSaturation) + (tod.light[0].g + tod.light[1].g + tod.light[2].g + tod.light[3].g) / 4 * us.lightStrength(tod),
								blue * tod.ambientSaturation + grey * (1 - tod.ambientSaturation) + (tod.light[0].b + tod.light[1].b + tod.light[2].b + tod.light[3].b) / 4 * us.lightStrength(tod),
								c.a);
						g.setColor(c);
						float lw = StrictMath.max(1, (float) (t.spec.width * us.zoom));
						g.setLineWidth(lw);
						float x1 = (float) (t.ta.ship.getX() + t.ta.ship.gridXToWorldX(t.ta.module.x, t.ta.module.type.getW()) * AGame.SGS + (t.ta.ship.flipped ? t.ta.module.type.getW() * AGame.SGS - t.spec.anchorX * AGame.SGS : t.spec.anchorX * AGame.SGS));
						float y1 = (float) (t.ta.ship.getY() + t.ta.module.y * AGame.SGS + t.spec.anchorY * AGame.SGS);
						float x2 = (float) (t.tb.ship.getX() + t.tb.ship.gridXToWorldX(t.tb.x, 1) * AGame.SGS + AGame.SGS / 2);
						float y2 = (float) (t.tb.ship.getY() + t.tb.y * AGame.SGS + AGame.SGS / 2); 
						g.drawLine(x1, y1, x2, y2);
						g.setLineWidth(1);
						float r = (float) t.spec.width / 2;
						g.fillOval(x1 - r, y1 - r, r * 2, r * 2);
						g.fillOval(x2 - r, y2 - r, r * 2, r * 2);
					}
				}
			}
		}
		SpritesheetBundle baseSSB = SpritesheetBundle.ofName("spritesheet");
		additionalSSBs.clear();
		RotatingShader.lockShader(baseSSB, d, us.lightingMap, us.lightStrength(tod), us.ambient(tod), tod.ambientSaturation);
		for (Combat.Side s : us.combat.sides) {
			for (Airship ship : s.ships) {
				for (Module m : ship.modules) {
					Tether t = m.tether;
					if (t != null) {
						if (t.shotAppearance == null) {
							continue;
						}
						if (!t.shotAppearance.src.equals(baseSSB.name)) {
							additionalSSBs.add(SpritesheetBundle.ofName(t.shotAppearance.src));
							continue;
						}
						double sx = t.tb.ship.getX() + t.tb.ship.gridXToWorldX(t.tb.x, 1) * AGame.SGS + AGame.SGS / 2 - t.shotAppearance.srcWidth / 2;
						double sy = t.tb.ship.getY() + t.tb.y * AGame.SGS + AGame.SGS / 2 - t.shotAppearance.srcHeight / 2;
						RotatingShader.draw(t.shotAppearance, d, sx, sy, t.shotAngle, false, false, us.lightingMap, us.lightStrength(tod), us.ambient(tod), us.ambientSaturation(tod), us.ambientTint(tod));
					}
				}
			}
		}
		RotatingShader.unlockShader();
		for (SpritesheetBundle ssb : additionalSSBs) {
			RotatingShader.lockShader(ssb, d, us.lightingMap, us.lightStrength(tod), us.ambient(tod), tod.ambientSaturation);
			for (Combat.Side s : us.combat.sides) {
				for (Airship ship : s.ships) {
					for (Module m : ship.modules) {
						Tether t = m.tether;
						if (t != null) {
							if (t.shotAppearance == null) {
								continue;
							}
							if (!t.shotAppearance.src.equals(ssb.name)) {
								continue;
							}
							double sx = t.tb.ship.getX() + t.tb.ship.gridXToWorldX(t.tb.x, 1) * AGame.SGS + AGame.SGS / 2 - t.shotAppearance.srcWidth / 2;
							double sy = t.tb.ship.getY() + t.tb.y * AGame.SGS + AGame.SGS / 2 - t.shotAppearance.srcHeight / 2;
							RotatingShader.draw(t.shotAppearance, d, sx, sy, t.shotAngle, false, false, us.lightingMap, us.lightStrength(tod), us.ambient(tod), us.ambientSaturation(tod), us.ambientTint(tod));
						}
					}
				}
			}
			RotatingShader.unlockShader();
		}
		
		for (Shot s : us.combat.shots) {
			if (s.weapon != null && s.weapon.hp > 0 && s.weapon.type.getTetherSpec(s.weaponBonuses) != null) {
				Tether.Spec tspec = s.weapon.type.getTetherSpec(s.weaponBonuses);
				float lw = StrictMath.max(1, (float) (tspec.width * us.zoom));
				Color c = tspec.color;
				float red = c.r * ambient.r;
				float green = c.g * ambient.g;
				float blue = c.b * ambient.b;
				float grey = (red + green + blue) / 3;
				c = new Color(
						red * tod.ambientSaturation + grey * (1 - tod.ambientSaturation) + (tod.light[0].r + tod.light[1].r + tod.light[2].r + tod.light[3].r) / 4 * us.lightStrength(tod),
						green * tod.ambientSaturation + grey * (1 - tod.ambientSaturation) + (tod.light[0].g + tod.light[1].g + tod.light[2].g + tod.light[3].g) / 4 * us.lightStrength(tod),
						blue * tod.ambientSaturation + grey * (1 - tod.ambientSaturation) + (tod.light[0].b + tod.light[1].b + tod.light[2].b + tod.light[3].b) / 4 * us.lightStrength(tod),
						c.a);
				g.setColor(c);
				g.setLineWidth(lw);
				float x1 = (float) (s.weapon.ship.getX() + s.weapon.ship.gridXToWorldX(s.weapon.x, s.weapon.type.getW()) * AGame.SGS + (s.weapon.ship.flipped ? s.weapon.type.getW() * AGame.SGS - tspec.anchorX * AGame.SGS : tspec.anchorX * AGame.SGS));
				float y1 = (float) (s.weapon.ship.getY() + s.weapon.y * AGame.SGS + tspec.anchorY * AGame.SGS);
				float x2 = (float) s.getX();
				float y2 = (float) s.getY();
				g.drawLine(x1, y1, x2, y2);
				g.setLineWidth(1);
				float r = (float) tspec.width / 2;
				g.fillOval(x1 - r, y1 - r, r * 2, r * 2);
				g.fillOval(x2 - r, y2 - r, r * 2, r * 2);
			}
		}
	}
}
