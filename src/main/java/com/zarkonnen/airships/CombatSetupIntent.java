package com.zarkonnen.airships;

import com.zarkonnen.airships.UniScreen.Intent;

public interface CombatSetupIntent extends Intent {
	public boolean isShipPlayerControlled(UniScreen us, Airship ship);
}
