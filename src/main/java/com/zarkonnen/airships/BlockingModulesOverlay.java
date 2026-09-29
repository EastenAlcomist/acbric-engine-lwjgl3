package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;

import java.util.ArrayList;
import org.newdawn.slick.Image;

public class BlockingModulesOverlay implements UniScreen.ShipOverlay {
	final Img crossedOut = new Img("ui", 144, 416, 16, 16, false);
	
	@Override
	public boolean drawIfOffScreen() {
		return false;
	}

	@Override
	public void draw(MyDraw d, Airship ship, UniScreen us) {
		if (!(us.intent instanceof EditShipIntent)) { return; }
		ArrayList<Module> obstructed = ship.getObstructedModules();
		if (crossedOut.machineImgCache != null) {
			((Image) crossedOut.machineImgCache).setFilter(Image.FILTER_NEAREST);
		}
		for (int mi = 0; mi < obstructed.size(); mi++) {
			Module m = obstructed.get(mi);
			for (int yy = 0; yy < m.type.getH(); yy++) { for (int xx = 0; xx < m.type.getW(); xx++) {
				d.blit(crossedOut, Clr.RED, ship.getX() + (m.x + xx) * AGame.SGS, ship.getY() + (m.y + yy) * AGame.SGS);
			}}
		}
	}
	
}
