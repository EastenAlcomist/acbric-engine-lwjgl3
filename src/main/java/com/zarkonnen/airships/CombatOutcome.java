package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;

public class CombatOutcome {
	public static enum CombatOutcomeType {
		VICTORY,
		DEFEAT,
		DRAW,
		UNCONTESTED_VICTORY,
		UNCONTESTED_DEFEAT,
		RAID_COMPLETE,
		RAID_REPELLED;
		
		public String getName() {
			return _t(name());
		}
	}
	
	public final CombatOutcomeType type;
	public final int x, y;
	public final MapLocation loc;
	public final String opponentName;

	public CombatOutcome(CombatOutcomeType type, int x, int y, MapLocation loc, String opponentName) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.loc = loc;
		this.opponentName = opponentName;
	}
}
