package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import org.newdawn.slick.Graphics;


public strictfp class FireOrdersChrome implements UniScreen.ShipChrome {
	public Img reticle = new Img("ui", 19, 274, 19, 19, false);
	public Img reticleOnce = new Img("ui", 96, 274, 19, 19, false);
	public Img reticleBig = new Img("ui", 81, 299, 31, 31, false);
	public Img reticleOnceBig = new Img("ui", 236, 299, 31, 31, false);
	public Img noTarget = new Img("ui", 688, 416, 16, 16, false);
	public Img shotReticle = new Img("ui", 39, 339, 19, 19, false);
	public Img shotReticleBig = new Img("ui", 81, 333, 31, 31, false);
	
	@Override
	public void draw(MyDraw d, Pt cursor, Airship ship, Combat.Side side, int x, int y, int w, int h, ScreenMode sm, UniScreen us) {
		if (ship.cripplingShotTarget || ship.disarmingShotTarget) {
			for (int i = 0; i < ship.modules.size(); i++) {
				Module m = ship.modules.get(i);
				if (m.hp <= 0) { continue; }
				if ((ship.cripplingShotTarget && m.type.getPropulsion(ship.currentBonuses) != 0) || (ship.disarmingShotTarget && m.type.isWeapon())) {
					if (AirshipGame.instance.currentGUIScale == GUIScale.SMALL) {
						d.blit(shotReticle,
							(int) us.worldToScreenX(ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2) - 10,
							(int) us.worldToScreenY(ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2) - 10
						);
					} else {
						d.blit(shotReticleBig,
							(int) us.worldToScreenX(ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2) - 16,
							(int) us.worldToScreenY(ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2) - 16
						);
					}
				}
			}
		}
		Airship dcShip = DirectControlPanel.getShip(us);
		if (dcShip == null) { return; }
		Combat.Side enemy = us.combat.otherSide(us.mySide);
		if (dcShip == ship) {
			for (int i = 0; i < dcShip.modules.size(); i++) {
				Module m = dcShip.modules.get(i);
				if (m.targetTroopOverride != null && enemy.troops.contains(m.targetTroopOverride)) {
					d.blit(reticle,
						(int) us.worldToScreenX(m.targetTroopOverride.getX() + m.targetTroopOverride.getBBWidth() / 2) - 10,
						(int) us.worldToScreenY(m.targetTroopOverride.getY() + m.targetTroopOverride.getBBHeight() / 2) - 10
					);
					continue;
				}
				/*if (m.fireMode == Module.DirectControlFireMode.AUTO) { continue; }
				if (m.prevTargetShip == null || m.prevTarget == null || !m.prevTargetShip.tiles.contains(m.prevTarget) || !enemy.ships.contains(m.prevTargetShip)) {
					d.blit(noTarget, MyDraw.SELECTED,
						(int) us.worldToScreenX(ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2) - 8,
						(int) us.worldToScreenY(ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2) - 8
					);
				}*/
			}
		} else {
			/*Graphics g = (Graphics) d.frame().nativeRenderer();
			g.setLineWidth(2);
			
			for (int i = 0; i < dcShip.modules.size(); i++) {
				Module m = dcShip.modules.get(i);
				if (m.fireMode == Module.DirectControlFireMode.AUTO) { continue; }
				if (m.prevTargetShip != ship || m.prevTarget == null || !m.prevTargetShip.tiles.contains(m.prevTarget) || !enemy.ships.contains(m.prevTargetShip) ) { continue; }
				
			}
			g.setLineWidth(1);*/
			for (int i = 0; i < dcShip.modules.size(); i++) {
				Module m = dcShip.modules.get(i);
				if (m.fireMode == Module.DirectControlFireMode.AUTO) { continue; }
				if (m.prevTargetShip != ship || m.prevTarget == null || !m.prevTargetShip.tiles.contains(m.prevTarget) || !enemy.ships.contains(m.prevTargetShip) ) { continue; }
				if (AirshipGame.instance.currentGUIScale == GUIScale.SMALL) {
					d.blit(m.fireMode == Module.DirectControlFireMode.ONCE ? reticleOnce : reticle,
						(int) us.worldToScreenX(ship.getX() + ship.gridXToWorldX(m.prevTarget.x, 1) * AGame.SGS + AGame.SGS / 2) - 10,
						(int) us.worldToScreenY(ship.getY() + m.prevTarget.y * AGame.SGS + AGame.SGS / 2) - 10
					);
				} else {
					d.blit(m.fireMode == Module.DirectControlFireMode.ONCE ? reticleOnceBig : reticleBig,
						(int) us.worldToScreenX(ship.getX() + ship.gridXToWorldX(m.prevTarget.x, 1) * AGame.SGS + AGame.SGS / 2) - 16,
						(int) us.worldToScreenY(ship.getY() + m.prevTarget.y * AGame.SGS + AGame.SGS / 2) - 16
					);
				}
			}
		}
	}

	@Override
	public void tick(Input in, int ms, UniScreen us) {}

	@Override
	public boolean textInputOccurring(UniScreen us) {
		return false;
	}
	
}
