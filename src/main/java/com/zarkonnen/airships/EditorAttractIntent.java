package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.ScreenMode;

import java.util.ArrayList;

public strictfp class EditorAttractIntent implements UniScreen.Intent, SingleShipIntent {
	public Airship sourceShip;
	public int time = 2000;
	public int typeToAdd = -1;
	public CoatOfArms coa = CoatOfArms.getRandom(AGame.ANIM_R, HeraldicStyle.ofName("player"));

	public EditorAttractIntent(Airship sourceShip) {
		this.sourceShip = sourceShip;
	}
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		ScreenMode sm = in.mode();
		us.zoom = StrictMath.min(
				sm.width / (sourceShip.getBBWidth() + 100.0),
				sm.height / (sourceShip.getBBHeight() + 100.0)
		);
		time += ms;
		ArrayList<ModuleType> mts = Loadable.all(ModuleType.class);
		if (time > 4000) {
			if (typeToAdd < mts.size()) {
				time -= 4000;
				boolean thingsToAdd;
				do {
					thingsToAdd = false;
					if (typeToAdd >= mts.size()) {
						return;
					} else {
						typeToAdd++;
						for (Module m : sourceShip.modules) {
							if (mts.indexOf(m.type) == typeToAdd) {
								thingsToAdd = true;
								break;
							}
						}
					}
				} while (!thingsToAdd);
				for (Module m : sourceShip.modules) {
					if (mts.indexOf(m.type) == typeToAdd) {
						int dx = (int) ((us.standaloneEditShip.getX() - sourceShip.getX()) / AGame.SGS);
						int dy = (int) ((us.standaloneEditShip.getY() - sourceShip.getY()) / AGame.SGS);
						us.standaloneEditShip.addModule(m.type, m.x - dx, m.y - dy, sourceShip.tileAt(m.x, m.y).armour.type, null);
					}
				}
			} else {
				us.standaloneEditShip = sourceShip;
				if (time > 10000) {
					MainMenu mm = new MainMenu(us.g, MainMenu.Submenu.MAIN);
					mm.idleTime = 8000;
					us.g.s = mm;
				}
			}
		}
	}

	@Override
	public boolean showOutside() {
		return time > 6000;
	}

	@Override
	public boolean showDecals() {
		return true;
	}

	@Override
	public boolean drawAsBlueprint() {
		return true;//time <= 6000;
	}

	@Override
	public Airship getShip(UniScreen us) {
		return us.standaloneEditShip;
	}

	@Override
	public void setShip(UniScreen us, Airship ship) {
	}
	
	@Override
	public void justSaved(UniScreen us) {}

	@Override
	public void modified(UniScreen us, boolean big) {}

	@Override
	public ShipType type(UniScreen us) {
		return ShipType.AIRSHIP;
	}
	@Override
	public boolean allowMultiSelect() { return false; }

	@Override
	public Airship getIgnoredOriginalShip(UniScreen us) {
		return null;
	}
}
