package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;


public strictfp class ModuleDebugChrome implements UniScreen.ShipChrome {
	@Override
	public void draw(MyDraw d, Pt cursor, Airship ship, Combat.Side side, int x, int y, int w, int h, ScreenMode sm, UniScreen us) {
		for (Module m : ship.modules) {
			int mx = (int) (x + (ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS) * us.zoom);
			int my = (int) (y + (m.y * AGame.SGS) * us.zoom);
			String text = m.running() ? "R" : m.canRun() ? "C" : m.couldRunIfStaffed() ? "S" : "-";
			d.text(text, AGame.FOUNT, mx, my);
		}
		
		d.text("Lift: " + ship.availableLift(us.combat) + " Service Ceiling: " + ship.availableServiceCeiling(us.combat), AGame.FOUNT, x, y - 20);
	}

	@Override
	public void tick(Input in, int ms, UniScreen us) {}

	@Override
	public boolean textInputOccurring(UniScreen us) {
		return false;
	}
}
