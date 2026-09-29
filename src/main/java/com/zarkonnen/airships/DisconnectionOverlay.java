package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;

import java.util.ArrayList;

public strictfp class DisconnectionOverlay implements UniScreen.ShipOverlay {
	private final PathingOverlay eot = new PathingOverlay();
	public boolean active = false;
	public static final Clr OVERLAY_C = new Clr(255, 0, 0, 63);
	
	@Override
	public void draw(MyDraw d, Airship ship, UniScreen us) {
		active = false;
		if (us.shipOverlay(EditorOverlay.class).active) { return; }
		if (us.hideUI) { return; }
		if (!(us.intent instanceof EditShipIntent) || ((EditShipIntent) us.intent).mode != EditMode.MODULES) { return; }
		ArrayList<ArrayList<Module>> chunks = ship.chunks(null);
		if (chunks.size() > 1) {
			// something
			ArrayList<Module> largest = null;
			int csz = chunks.size();
			for (int ci = 0; ci < csz; ci++) {
				ArrayList<Module> c = chunks.get(ci);
				if (largest == null || largest.size() < c.size()) {
					largest = c;
				}
			}
			
			for (int ci = 0; ci < csz; ci++) {
				ArrayList<Module> chunk = chunks.get(ci);
				if (chunk == largest) { continue; }
				int msz = chunk.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = chunk.get(mi);
					d.rect(OVERLAY_C, ship.getIntX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS, ship.getIntY() + m.y * AGame.SGS, m.type.getW() * AGame.SGS, m.type.getH() * AGame.SGS);
				}
			}
			return;
		}
		chunks = ship.pathChunks(null);
		if (chunks.size() > 1) {
			active = true;
			
			int lineWidth = 2;
			switch (AirshipGame.instance.currentGUIScale) {
				case SMALL: lineWidth = 1; break;
				case MEDIUM: lineWidth = 2; break;
				case LARGE: lineWidth = 4; break;
			}

			ArrayList<Module> largest = null;
			int csz = chunks.size();
			for (int ci = 0; ci < csz; ci++) {
				ArrayList<Module> c = chunks.get(ci);
				if (largest == null || largest.size() < c.size()) {
					largest = c;
				}
			}
			int tsz = ship.tiles.size();
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				if (!t.canOccupy) { continue; }
				int tx = ship.getIntX() + ship.gridXToWorldX(t.x, 1) * AGame.SGS;
				int ty = ship.getIntY() + t.y * AGame.SGS;
				eot.draw(d, t, ship, tx, ty, us.zoom, 0, largest.contains(t.module) ? Clr.BLACK : Clr.RED);
				if (!largest.contains(t.module) && t.module.type.getOccupableTileCount() < 2) {
					double w = 3.0 * lineWidth / us.zoom;
					d.rect(Clr.RED, tx + AGame.SGS / 4, ty + AGame.SGS / 2 - w / 2, AGame.SGS / 2, w);
					d.rect(Clr.RED, tx + AGame.SGS / 2 - w / 2, ty + AGame.SGS / 4, w, AGame.SGS / 2);
				}
			}
			for (int ti = 0; ti < tsz; ti++) {
				Tile t = ship.tiles.get(ti);
				if (!t.canOccupy) { continue; }
				int tx = ship.getIntX() + ship.gridXToWorldX(t.x, 1) * AGame.SGS;
				int ty = ship.getIntY() + t.y * AGame.SGS;
				eot.draw(d, t, ship, tx, ty, us.zoom, 1, largest.contains(t.module) ? Clr.BLACK : Clr.RED);
				if (!largest.contains(t.module) && t.module.type.getOccupableTileCount() < 2) {
					double w = 1.0 * lineWidth / us.zoom;
					d.rect(Clr.WHITE, tx + AGame.SGS / 4, ty + AGame.SGS / 2 - w / 2, AGame.SGS / 2, w);
					d.rect(Clr.WHITE, tx + AGame.SGS / 2 - w / 2, ty + AGame.SGS / 4, w, AGame.SGS / 2);
				}
			}
		}
	}	
	
	@Override
	public boolean drawIfOffScreen() {
		return false;
	}
}
