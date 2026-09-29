package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Clr;


public strictfp class ExplosionDamageOverlay implements EditorOverlayType {
	private Airship ship;
	
	@Override
	public String name() {
		return _t("Explosion_damage");
	}

	@Override
	public String explanation() {
		return _t("Explosion_damage_explanation");
	}

	@Override
	public void update(Airship ship, boolean forced, Tile hoverTile) {
		this.ship = ship;
	}
	
	@Override
	public String globalInfo() {
		return null;
	}
	
	public int explosionDamageAt(Tile t, Tile hoverTile) {
		if (t.module.x != t.x || t.module.y != t.y) { return t.module.testExplosionDamageTmp; }
		// Loop over all explody modules
		// calculate the total damage done to this one if it exploded
		// pick the biggest number
		int dmg = 0;
		Module me = t.module;
		int msz = ship.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			if (hoverTile != null && hoverTile.module != m) { continue; }
			if (m.type.getExplodeDmg(ship.currentBonuses) == 0 || m.type.getExplodeRadius(ship.currentBonuses) <= 0) { continue; }
			if (m == me) { continue; }
			int damageFromM = 0;
			for (int ty = me.y; ty < me.y + me.type.getH(); ty++) {
				for (int tx = me.x; tx < me.x + me.type.getW(); tx++) {
					ship.tileAt(tx, ty).testSplashDamage = 0;
				}
			}
			m.testExplosionSplashDamage();
			for (int ty = me.y; ty < me.y + me.type.getH(); ty++) {
				for (int tx = me.x; tx < me.x + me.type.getW(); tx++) {
					damageFromM += ship.tileAt(tx, ty).testSplashDamage;
				}
			}
			
			dmg = StrictMath.max(damageFromM, dmg);
		}
		
		me.testExplosionDamageTmp = dmg;
		
		return dmg;
	}

	@Override
	public Clr overlayColor(Tile t, Tile hoverTile) {
		if (hoverTile != null && hoverTile.module.type.getExplodeDmg(ship.currentBonuses) == 0) {
			hoverTile = null;
		}
		if ((hoverTile == null || t.module == hoverTile.module) && t.module.type.getExplodeDmg(ship.currentBonuses) > 0) {
			return new Clr(200, 184, 79, 180);
		}
		int dmg = explosionDamageAt(t, hoverTile);
		if (dmg >= t.module.getMaxHP()) {
			return new Clr(255, 0, 0, 180);
		}
		if (dmg * 100 / t.module.getMaxHP() < 1) {
			return new Clr(0, 0, 0, 150);
		}
		int transparency = dmg * 150 / t.module.getMaxHP() + 70;
		return new Clr(transparency, 0, 0, 150);
	}

	@Override
	public String info(Tile t, Tile hoverTile) {
		if (t.module.x != t.x || t.module.y != t.y) { return ""; }
		if (hoverTile != null && hoverTile.module.type.getExplodeDmg(ship.currentBonuses) == 0) {
			hoverTile = null;
		}
		if ((hoverTile == null || t.module == hoverTile.module) && t.module.type.getExplodeDmg(ship.currentBonuses) > 0) {
			return "";
		}
		int percent = explosionDamageAt(t, hoverTile) * 100 / t.module.getMaxHP();
		return percent < 1 ? "" : (percent + "%");
	}
	
	@Override
	public void draw(MyDraw d, Tile t, Airship ship, int tx, int ty, double zoom, int pass, Tile hoverTile) {
		/*int msz = ship.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			if (m.type.getExplodeDmg(ship.currentBonuses) == 0 || m.type.getExplodeRadius(ship.currentBonuses) <= 0) { continue; }
			d.rect(Clr.WHITE, ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2 - 1,
				ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2 - 1, 3, 3);
		}*/
	}
	
	@Override
	public boolean showTooltips() { return false; }
}
