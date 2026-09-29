package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;


public class ShootQualityOverlay implements EditorOverlayType {
	public ModuleType weapon = ModuleType.ofName("CANNON");
	
	@Override
	public String name() {
		return weapon.getName() + " Shoot Quality";
	}

	@Override
	public String explanation() {
		return "Shows how much a " + weapon.getName() + " wants to shoot each tile.";
	}

	@Override
	public void update(Airship ship, boolean force, Tile hoverTile) {}

	@Override
	public Clr overlayColor(Tile t, Tile hoverTile) {
		Module m = new Module(new Airship(ShipType.AIRSHIP), weapon, 0, 0);
		int amt = (int) Math.log(m.quality(t));
		return new Clr(0, 255, 0, Math.max(0, Math.min(255, amt * 255 / ((int) Math.log(5000)))));
	}

	@Override
	public String info(Tile t, Tile hoverTile) {
		Module m = new Module(new Airship(ShipType.AIRSHIP), weapon, 0, 0);
		int amt = m.quality(t);
		return "" + amt;
	}

	@Override
	public void draw(MyDraw d, Tile t, Airship ship, int tx, int ty, double zoom, int pass, Tile hoverTile) {
	}

	@Override
	public boolean showTooltips() {
		return true;
	}

	@Override
	public String globalInfo() {
		return null;
	}
	
}
