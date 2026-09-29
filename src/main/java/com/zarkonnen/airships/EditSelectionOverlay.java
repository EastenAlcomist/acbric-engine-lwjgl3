package com.zarkonnen.airships;

import com.zarkonnen.airships.UniScreen.ShipOverlay;
import com.zarkonnen.catengine.util.Clr;


public strictfp class EditSelectionOverlay implements ShipOverlay {
	public int shiftX, shiftY;
	public boolean fail;
	
	@Override
	public boolean drawIfOffScreen() {
		return true;
	}

	@Override
	public void draw(MyDraw d, Airship ship, UniScreen us) {
		if (!(us.intent instanceof EditShipIntent)) { return; }
		if (us.tool != UniScreen.NAVIGATE) { return; }
		EditShipIntent esi = (EditShipIntent) us.intent;
		if (esi.mode == EditMode.MODULES) {
			for (Module m : esi.selectedModules) {
				if (ship.containsModule(m)) {
					if (fail) {
						m.type.drawAsRedOutline(d, ship.getIntX() + (ship.gridXToWorldX(m.x, m.type.getW()) + shiftX) * AGame.SGS, ship.getIntY() + (m.y + shiftY) * AGame.SGS, m.time + m.animOffset, ship.flipped, m.variant, ship.currentBonuses);
					} else {
						m.type.drawAsBlueprint(d, ship.getIntX() + (ship.gridXToWorldX(m.x, m.type.getW()) + shiftX) * AGame.SGS, ship.getIntY() + (m.y + shiftY) * AGame.SGS, m.time + m.animOffset, ship.flipped, m.variant, ship.currentBonuses, 1);
					}
				}
			}
		}
		if (esi.mode == EditMode.DECALS) {
			for (Decal dec : esi.selectedDecals) {
				if (ship.decals.contains(dec)) {
					boolean aes;
					if (fail) {
						aes = dec.type.drawAsOutline(d, ship.getIntX() + (ship.gridXToWorldX(dec.x, dec.type.imgW) + shiftX) * AGame.SGS, ship.getIntY() + (dec.y + shiftY) * AGame.SGS, us.time, ship.flipped, Clr.RED);
					} else {
						aes = dec.type.drawAsOutline(d, ship.getIntX() + (ship.gridXToWorldX(dec.x, dec.type.imgW) + shiftX) * AGame.SGS, ship.getIntY() + (dec.y + shiftY) * AGame.SGS, us.time, ship.flipped, Clr.WHITE);
					}
					if (aes) {
						d.rect(fail ? new Clr(255, 0, 0, 128) : new Clr(255, 255, 255, 128), ship.getIntX() + (ship.gridXToWorldX(dec.x, dec.type.w) + shiftX) * AGame.SGS, ship.getIntY() + (dec.y + shiftY) * AGame.SGS, dec.type.w * AGame.SGS, dec.type.h * AGame.SGS);
					}
				}
			}
		}
	}
}
