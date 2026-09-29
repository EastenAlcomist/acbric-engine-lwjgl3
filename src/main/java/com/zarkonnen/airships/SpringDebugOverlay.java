package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;


public class SpringDebugOverlay implements UniScreen.ShipOverlay {
	@Override
	public boolean drawIfOffScreen() {
		return true;
	}

	@Override
	public void draw(MyDraw d, Airship ship, UniScreen us) {
		if (us.combat == null) { return; }
		d.rect(Clr.RED, ship.getX(), ship.getY(), 3, 3);
		for (Module m : ship.modules) {
			for (Spring spring : m.type.getSprings()) {
				double l = spring.getLength(
						ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS,
						ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS,
						us.combat, ship, new GridBody[] {null});
				Clr c = Clr.WHITE;
				if (l <= spring.minCompressedLength) {
					c = Clr.ORANGE;
				} else if (l < spring.baseLength) {
					c = Clr.YELLOW;
				}
				d.rect(c, ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + spring.xOffset * AGame.SGS - 1, ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS, 2, l);
 			}
			for (Leg leg : m.legs) {
				Spring spring = leg.spec.spring;
				double l = leg.getGroundY(us.combat.landFormations.get(0), us.combat.landFormations, leg.footX()) - leg.hipY();
				Clr c = Clr.WHITE;
				if (l <= spring.minCompressedLength) {
					c = Clr.ORANGE;
				} else if (l < spring.baseLength) {
					c = Clr.YELLOW;
				}
				d.rect(c, leg.targetFootX, leg.hipY(), 2, l);
			}
		}
	}
}
