package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;


import static com.zarkonnen.airships.Lang._t;


public strictfp class DestroyTool extends UniScreen.Tool {
	@Override
	public void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {
		d.state.setCursor("FULL_TARGET", null);
	}

	@Override
	public boolean click(Input in, Pt click, ScreenMode sm, UniScreen us) {
		Airship ship = getShip(click, us);
		if (ship != null) {
			for (Module m : ship.modules) {
				m.hp = -m.getMaxHP();
				m.explodeFuze = 1;
			}
			us.combat.usedCheatCommand = true;
			if (us.combat.recording != null) {
				us.combat.recording.expectDivergence = true;
			}
			if (us.intent instanceof CampaignCombatIntent) {
				((CampaignCombatIntent) us.intent).ss.w.usedCheatCommand = true;
			}
		}
		return true;
	}
	
	@Override public String getLabel() { return _t("Destroy_Cheat"); }

	@Override
	public boolean mouseDown(Input in, Pt mouseDown, ScreenMode sm, UniScreen us) {
		return true;
	}
	
	private Airship getShip(Pt mouseDown, UniScreen us) {
		if (us.combat == null) { return null; }
		double worldX = us.screenToWorldX(mouseDown.x);
		double worldY = us.screenToWorldY(mouseDown.y);
		for (Side s : us.combat.sides) {
			for (Airship ship : s.ships) {
				int tileX = (int) ((ship.flipped
						? ship.getWidth() * AGame.SGS - (worldX - ship.getIntX())
						: worldX - ship.getIntX()) / AGame.SGS);
				int tileY = (int) ((worldY - ship.getIntY()) / AGame.SGS);
				Tile t = ship.tileAt(tileX, tileY);
				if (t != null) {
					return ship;
				}
			}
		}
		return null;
	}

	@Override
	public void tick(Input in, int ms, UniScreen us) {}

	@Override
	public boolean rightClick(Input in, Pt click, ScreenMode sm, UniScreen us) { return true; }
}
