package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;

import java.util.ArrayList;

public class LegDebugLayer implements UniScreen.VisualLayer {
	public Leg leg;
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (us.combat == null) { return; }
		if (leg == null) {
			lp: for (Combat.Side side : us.combat.sides) {
				for (Airship ship : side.ships) {
					for (Module m : ship.modules) {
						for (Leg l : m.legs) {
							leg = l;
							break lp;
						}
					}
				}
			}
		}
		if (in.keyPressed("X")) {
			ArrayList<Leg> legs = new ArrayList<Leg>();
			for (Combat.Side side : us.combat.sides) {
				for (Airship ship : side.ships) {
					for (Module m : ship.modules) {
						for (Leg l : m.legs) {
							legs.add(l);
						}
					}
				}
			}
			if (legs.size() > 1) {
				leg = legs.get((legs.indexOf(leg) + 1) % legs.size());
			}
		}
	}

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		/*if (us.combat == null) { return; }
		if (leg == null) { return; }
		for (double x = leg.hipX() - 400; x < leg.hipX() + 400; x += 4) {
			double y = leg.getGroundY(null, us.combat.landFormations, x);
			d.rect(leg.legRotation(x, y, false) ? Clr.WHITE : Clr.RED, x, y, 4, 4);
		}*/
	}
}
