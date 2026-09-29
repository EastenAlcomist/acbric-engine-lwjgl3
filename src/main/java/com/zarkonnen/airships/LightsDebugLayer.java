package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.catengine.Input;
import org.newdawn.slick.Graphics;


public strictfp class LightsDebugLayer implements UniScreen.VisualLayer {
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		time += ms;
	}
	
	int time = 0;

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		//if ((time / 2000) % 2 == 0) { return; }
		if (2 * 2 == 4) { return; }
		Graphics g = (Graphics) d.frame().nativeRenderer();
		g.scale(AGame.LIGHTMAP_DOWNSCALE, AGame.LIGHTMAP_DOWNSCALE);
		g.drawImage(us.lightingMap[0], 0, 0);
		g.resetTransform();
		/*if (us.combat != null) {
			for (Side s : us.combat.sides) {
				for (Airship as : s.ships) {
					draw(d, as);
				}
			}
		}*/
		/*g.setDrawMode(Graphics.MODE_ADD);
		if (us.combat != null) {
			for (Side s : us.combat.sides) {
				for (Airship as : s.ships) {
					draw(d, as);
				}
			}
			for (Particle p : us.combat.particles) {
				if (p.type.lightClr != null) {
					double r = p.type.lightRadius * (p.life * 1.0 / p.lifespan);
					d.rect(p.type.lightClr, p.x - r / 4, p.y - r / 4, r / 2, r / 2);
					d.rect(p.type.lightClr, p.x - r, p.y - r, r * 2, r * 2);
				}
			}
		}
		if (us.setupFleet != null) {
			for (Airship as : us.setupFleet) {
				draw(d, as);
			}
		}
		// Ignore other options just now. qqDPS
		g.setDrawMode(Graphics.MODE_NORMAL);*/
	}
	
	public void draw(MyDraw d, Airship sh) {
		for (Module m : sh.modules) {
			if (m.running()) {
				for (ModuleType.ModuleLightSource mls : m.type.getLights(sh.currentBonuses)) {
					double r = mls.radius;
					double mx = sh.getX() + sh.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
					if (sh.flipped) {
						mx += (m.type.getW() - mls.xOffset) * AGame.SGS;
					} else {
						mx += mls.xOffset * AGame.SGS;
					}
					double my = sh.getY() + m.y * AGame.SGS + mls.yOffset * AGame.SGS;
					d.rect(mls.clr, mx - 2, my - 2, 5, 5);
				}
			}
		}
	}
}
