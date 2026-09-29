package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.JobBoard.indent;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;

import java.util.ArrayList;

public strictfp class ShipProfilePanel implements UniScreen.InfoPanel {
	static final Clr OVERLAY = new Clr(0, 0, 0, 200);
	final ModuleType[] interesting = {
		ModuleType.ofName("LARGE_KEEL"),
		ModuleType.ofName("TARGETING_COMPUTER"),
		ModuleType.ofName("TELESCOPE")
	};
	
	@Override
	public void draw(MyDraw d, Pt cursor, ScreenMode sm, Hooks hs, UniScreen us) {
		if (us.followShip == null) { return; }
		Airship ship = us.followShip;
		CoatOfArms coa = CoatEditor.getMyStrategicArms();
		if (us.combat != null && us.combat.sideOf(ship) != null) {
			coa = us.combat.sideOf(ship).arms;
		}
		if (us.intent instanceof EditorAttractIntent) {
			EditorAttractIntent eai = (EditorAttractIntent) us.intent;
			coa = eai.coa;
			if (eai.typeToAdd < Loadable.all(ModuleType.class).size()) {
				return;
			}
		}
		int coaScale = 4;
		int x = MyDraw.SIDE_CLEARANCE;
		int y = sm.height - MyDraw.SIDE_CLEARANCE - 41 * coaScale;
		d.rect(OVERLAY, 0, y - MyDraw.UI_SPACING, sm.width, sm.height);
		coa.layout.drawShield(coa, d, x, y, coaScale, Clr.WHITE);
		x += 39 * coaScale + MyDraw.UI_SPACING;
		d.text(ship.getName(), AGame.HUGE_FOUNT, x, y);
		y += AGame.HUGE_FOUNT.height;
		int in = 20;
		ArrayList<String> stats = new ArrayList<String>();
		
		stats.add(indent(_t("Cost_"), in) + ship.getCost());
		stats.add(indent(_t("Weight_"), in) + ship.getWeight());
		stats.add(indent(_t("Speed_"), in) + (int) (ship.getReportedMainMapSpeed() * 3600 / AGame.PX_TO_M));
		stats.add(indent(_t("Service_ceiling_"), in) + ship.realServiceCeiling() / 8);
		for (CrewType ct : Loadable.all(CrewType.class)) {
			if (ship.getQuartered(ct) > 0) {
				stats.add(indent(ct.getPlural() + ": ", in) + ship.getQuartered(ct));
			}
		}
		
		for (ModuleType mt : ModuleCategory.ofName("WEAPONS").getContents()) {
			int n = 0;
			for (Module m : ship.modules) { if (m.type == mt) { n++; } }
			if (n > 0) {
				stats.add(indent(mt.getName() + ": ", in) + n);
			}
		}
		
		for (ModuleType mt : interesting) {
			boolean has = false;
			for (Module m : ship.modules) { if (m.type == mt) { has = true; break; } }
			if (has) {
				stats.add(mt.getName());
			}
		}
		
		int y2 = y;
		int x2 = x;
		for (String stat : stats) {
			//d.borderedText(stat, AGame.FOUNT, Clr.WHITE, Clr.BLACK, x2, y2);
			d.text(stat, AGame.FOUNT, x2, y2);
			y2 += AGame.FOUNT.lineHeight;
			if (y2 + AGame.FOUNT.lineHeight > sm.height - MyDraw.SIDE_CLEARANCE) {
				y2 = y;
				x2 += AGame.FOUNT.displayWidth * 30;
			}
		}
	}

	@Override
	public void tick(Input in, int ms, UniScreen us) {}

	@Override
	public boolean chatEnabled(UniScreen us) {
		return false;
	}

	@Override
	public boolean doScroll(UniScreen us, int scrollAmt, Pt cursor, ScreenMode sm) {
		return false;
	}
	
	@Override public boolean arrowKeysInUse(UniScreen us) { return false; }
}
