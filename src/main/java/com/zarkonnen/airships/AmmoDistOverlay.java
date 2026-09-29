package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Clr;

import java.util.ArrayList;
import java.util.HashMap;

public strictfp class AmmoDistOverlay implements EditorOverlayType {
	public Airship ship;
	private final HashMap<Module, Integer> values = new HashMap<Module, Integer>();
	public int nModules = 0;
	
	@Override
	public String name() {
		return _t("Ammo_distance");
	}

	@Override
	public String explanation() {
		return _t("Ammo_distance_explanation");
	}
	
	@Override
	public String globalInfo() {
		return null;
	}

	@Override
	public void update(Airship ship, boolean forced, com.zarkonnen.airships.Tile hoverTile) {
		if (forced || ship != this.ship || (ship != null && this.ship != null && ship.modules.size() != nModules)) {
			nModules = ship.modules.size();
			this.ship = ship;
			values.clear();
			if (this.ship == null) { return; }
			for (Module tm : ship.modules) {
				if (tm.type.getAmmo(ship.currentBonuses) > 0) {
					values.put(tm, -2);
					continue;
				}
				if (tm.type.getReload(ship.currentBonuses) == 0) { continue; }
				if (tm.type.getClip(ship.currentBonuses) == 0) { continue; } // Ammoless weapons
				int leastDist = Integer.MAX_VALUE;
				for (int dy = 0; dy < tm.type.getH(); dy++) {
					for (int dx = 0; dx < tm.type.getW(); dx++) {
						Tile t2 = ship.tileAt(tm.x + dx, tm.y + dy);
						for (Module m : ship.modules) {
							if (m.type.getAmmo(ship.currentBonuses) > 0) {
								ArrayList<Tile> path = ship.getPath(t2, m);
								if (path != null) {
									int dist = 0;
									for (Tile pt : path) {
										dist += pt.module.type.getMoveDelay(ship.currentBonuses);
									}
									leastDist = StrictMath.min(dist, leastDist);
								}
							}
						}
					}
				}
				values.put(tm, leastDist == Integer.MAX_VALUE ? -1 : leastDist);
			}
		}
	}
	
	private int value(Tile t) {
		if (ship == null) {
			return Integer.MAX_VALUE;
		}
		return values.containsKey(t.module) ? values.get(t.module) : -1;
	}

	@Override
	public Clr overlayColor(Tile t, Tile hoverTile) {
		int v = value(t);
		return v == Integer.MAX_VALUE ? new Clr(255, 0, 0, 200)
				: v == -1 ? new Clr(0, 0, 0, 200)
				: v == -2 ? new Clr(50, 255, 50, 200)
				: new Clr(StrictMath.max(0, StrictMath.min(v / 50 - 100, 255)), 255 - StrictMath.min(255, v / 30), 0, 150);
	}

	@Override
	public String info(Tile t, Tile hoverTile) {
		if (t.module.x == t.x && t.module.y == t.y) {
			int v = value(t);
			return v == Integer.MAX_VALUE || v < 0 ? null : (v < 1000 ? "<1" : "" + (v / 1000));
		} else {
			return null;
		}
	}
	
	@Override
	public void draw(MyDraw d, Tile t, Airship ship, int tx, int ty, double zoom, int pass, Tile hoverTile) {}
	
	@Override
	public boolean showTooltips() { return true; }
}
