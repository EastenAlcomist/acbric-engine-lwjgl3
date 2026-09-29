package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Input;


public strictfp class RestockResourcesButton extends UniScreen.Button {
	@Override
	public boolean visible(UniScreen us) {
		return us.combat != null && us.mySide != null && SimplePref.CHEATS.get() && (us.intent instanceof CampaignCombatIntent || us.intent instanceof SingleCombatIntent);
	}

	@Override
	public String text(UniScreen us) {
		return _t("Restock_Cheat");
	}

	@Override
	public void click(Input in, UniScreen us) {
		for (Airship s : us.mySide.getAllShips()) {
			for (Module m : s.getModules()) {
				m.fillUpResources();
			}
		}
		us.combat.usedCheatCommand = true;
		if (us.combat.recording != null) {
			us.combat.recording.expectDivergence = true;
		}
		if (us.intent instanceof CampaignCombatIntent) {
			((CampaignCombatIntent) us.intent).ss.w.usedCheatCommand = true;
		}
	}
}
