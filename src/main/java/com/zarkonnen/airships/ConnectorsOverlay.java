package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;


public strictfp class ConnectorsOverlay implements UniScreen.ShipOverlay {
	@Override
	public void draw(MyDraw d, Airship ship, UniScreen us) {
		if (us.hideUI) { return; }
		if (!(us.intent instanceof EditShipIntent) || ((EditShipIntent) us.intent).mode != EditMode.MODULES) { return; }
		Clr c = new Clr(255, 255, 255, 80);
		for (Module m : ship.modules) {
			// L/R connectors
			for (int i = 0; i < m.type.getH(); i++) {
				if (m.type.getLeftDoors()[i] && ship.tileAt(m.x - 1, m.y + i) == null && !m.type.isBackOnly()[i] && ship.tileAt(m.x, m.y + i) != null && ship.tileAt(m.x, m.y + i).canOccupy) {
					if (ship.flipped) {
						d.rect(c,
								ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + AGame.SGS * m.type.getW(),
								ship.getY() + (m.y + i) * AGame.SGS + AGame.SGS - 1,
								6,
								1
						);
					} else {
						d.rect(c,
								ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS - 6,
								ship.getY() + (m.y + i) * AGame.SGS + AGame.SGS - 1,
								6,
								1
						);
					}
				}
				if (m.type.getRightDoors()[i] && ship.tileAt(m.x + m.type.getW(), m.y + i) == null && !m.type.isFrontOnly()[i] && ship.tileAt(m.x + m.type.getW() - 1, m.y + i) != null && ship.tileAt(m.x + m.type.getW() - 1, m.y + i).canOccupy) {
					if (ship.flipped) {
						d.rect(c,
								ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS - 6,
								ship.getY() + (m.y + i) * AGame.SGS + AGame.SGS - 1,
								6,
								1
						);
					} else {
						d.rect(c,
								ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + AGame.SGS * m.type.getW(),
								ship.getY() + (m.y + i) * AGame.SGS + AGame.SGS - 1,
								6,
								1
						);
					}
				}
			}
			// Top/bottom connectors
			for (int i = 0; i < m.type.getW(); i++) {
				if (!m.type.isTopOnly()[i] && m.type.getUpDoors()[i] && ship.tileAt(m.x + i, m.y - 1) == null) {
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 13,
							ship.getY() + m.y * AGame.SGS - 6,
							1,
							6
					);
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 9,
							ship.getY() + m.y * AGame.SGS - 6,
							1,
							6
					);
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 10,
							ship.getY() + m.y * AGame.SGS - 2,
							3,
							1
					);
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 10,
							ship.getY() + m.y * AGame.SGS - 4,
							3,
							1
					);
				}
				if (!m.type.isBottomOnly()[i] && ship.tileAt(m.x + i, m.y + m.type.getH() - 1) != null && ship.tileAt(m.x + i, m.y + m.type.getH() - 1).canOccupy && ship.tileAt(m.x + i, m.y + m.type.getH()) == null) {
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 9,
							ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS,
							1,
							6
					);
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 13,
							ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS,
							1,
							6
					);
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 10,
							ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS + 1,
							3,
							1
					);
					d.rect(c,
							ship.getX() + ship.gridXToWorldX(m.x + i, 1) * AGame.SGS + 10,
							ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS + 3,
							3,
							1
					);
				}
			}
		}
	}

	@Override
	public boolean drawIfOffScreen() {
		return false;
	}
}
