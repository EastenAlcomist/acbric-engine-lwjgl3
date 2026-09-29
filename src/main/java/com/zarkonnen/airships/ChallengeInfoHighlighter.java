package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;


public strictfp class ChallengeInfoHighlighter implements UniScreen.ShipOverlay {
	private final ChallengeInfoFloat.Rectangle r = new ChallengeInfoFloat.Rectangle();
	private final Clr hint = new Clr(0, 0, 0, 70);
	
	@Override
	public void draw(MyDraw d, Airship ship, UniScreen us) {
		if (!(us.intent instanceof ChallengeEditShipIntent || us.intent instanceof ChallengeCombatIntent)) { return; }
		ChallengeInfoFloat cif = us.findFloat(ChallengeInfoFloat.class);
		if (cif.msg != null && cif.msg.highlightModule != null) {
			for (Module m : ship.modules) {
				if (m.type == cif.msg.highlightModule) {
					r.x = (int) ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
					r.y = (int) ship.getY() + m.y * AGame.SGS;
					r.w = m.type.getW() * AGame.SGS;
					r.h = m.type.getH() * AGame.SGS;
					cif.highlight(d, r);
				}
			}
		}
		
		if (cif.msg != null && cif.msg.highlightMovePosition != null && us.combat != null && !us.combat.sides.get(0).ships.isEmpty() && ship == us.combat.sides.get(0).ships.get(0)) {
			ship.drawOutline(d, cif.msg.highlightMovePosition.x, cif.msg.highlightMovePosition.y, cif.msg.highlightMoveFlip, hint);
		}
	}

	@Override
	public boolean drawIfOffScreen() {
		return true;
	}
}
