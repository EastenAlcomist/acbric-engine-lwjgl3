package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public abstract strictfp class CombatAchievement {
	public abstract void tick(Combat c, Side mySide);
	
	public static ArrayList<CombatAchievement> get() {
		ArrayList<CombatAchievement> l = new ArrayList<CombatAchievement>();
		l.add(new DestroyByRamming());
		l.add(new ForceDown());
		l.add(new RockLanding());
		l.add(new Capture());
		l.add(new EvadeBoarders());
		l.add(new Broadside());
		l.add(new AirSuperiority());
		l.add(new SawVsSaw());
		l.add(new Hentai());
		l.add(new Sobieski());
		l.add(new CrushDepth());
		l.add(new MileDeepClub());
		return l;
	}
	
	public static strictfp class SawVsSaw extends CombatAchievement {
		ArrayList<Airship> enemySawShips;
		boolean running = !Achievement.isAchieved(Achievement.SHAVING_WITH_FRIENDS);
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			Side enemy = c.otherSide(mySide);
			ModuleType saw = Loadable.hasOfName(ModuleType.class, "SAWBLADE") ? ModuleType.ofName("SAWBLADE") : null;
			ModuleType sawFlipped = Loadable.hasOfName(ModuleType.class, "FLIPPED_SAWBLADE") ? ModuleType.ofName("FLIPPED_SAWBLADE") : null;
			if (saw == null || sawFlipped == null) { return; }
			if (enemySawShips == null) {
				enemySawShips = new ArrayList<Airship>();
				for (Airship s : enemy.getAllShips()) {
					if (s.hasModuleTypeAny(saw, sawFlipped)) {
						enemySawShips.add(s);
					}
				}
			}
			for (Airship s : enemySawShips) {
				if (s.sawDamageTaken > 100 && s.sawDamageTaken >= (s.mechTentacleDamageTaken + s.hussarDamageTaken + s.otherDamageTaken) &&
					(
						(
						!enemy.ships.contains(s) &&
						!mySide.ships.contains(s) &&
						!enemy.reserve.contains(s) &&
						!mySide.reserve.contains(s)
						)
						|| 
						!s.inCombat(c)
					)
				)
				{
					Achievement.achieve(Achievement.SHAVING_WITH_FRIENDS);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class Hentai extends CombatAchievement {
		ArrayList<Airship> enemyKrakens;
		boolean running = !Achievement.isAchieved(Achievement.HENTAI);
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			if (!Loadable.hasOfName(ModuleType.class, "SQUID")) { return; }
			Side enemy = c.otherSide(mySide);
			ModuleType kraken = ModuleType.ofName("SQUID");
			if (enemyKrakens == null) {
				enemyKrakens = new ArrayList<Airship>();
				for (Airship s : enemy.getAllShips()) {
					if (s.hasModuleTypeAny(kraken)) {
						enemyKrakens.add(s);
					}
				}
			}
			for (Airship s : enemyKrakens) {
				if (s.mechTentacleDamageTaken > 100 && s.mechTentacleDamageTaken >= (s.sawDamageTaken + s.hussarDamageTaken + s.otherDamageTaken) &&
					(
						(
						!enemy.ships.contains(s) &&
						!mySide.ships.contains(s) &&
						!enemy.reserve.contains(s) &&
						!mySide.reserve.contains(s)
						)
						|| 
						!s.inCombat(c)
					)
				)
				{
					Achievement.achieve(Achievement.HENTAI);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class Sobieski extends CombatAchievement {
		ArrayList<Airship> enemyShips;
		boolean running = !Achievement.isAchieved(Achievement.SOBIESKI);
		int numDestroyed = 0;
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			Side enemy = c.otherSide(mySide);
			if (enemyShips == null) {
				enemyShips = new ArrayList<Airship>(enemy.getAllShips());
			}
			for (int i = 0; i < enemyShips.size(); i++) {
				Airship s = enemyShips.get(i);
				if (s.type == ShipType.BUILDING) { continue; }
				if (s.hussarDamageTaken > 100 && s.hussarDamageTaken >= (s.sawDamageTaken + s.mechTentacleDamageTaken + s.otherDamageTaken) &&
					(
						(
						!enemy.ships.contains(s) &&
						!mySide.ships.contains(s) &&
						!enemy.reserve.contains(s) &&
						!mySide.reserve.contains(s)
						)
						|| 
						!s.inCombat(c)
					)
				)
				{
					numDestroyed++;
					enemyShips.remove(i);
					i--;
					if (numDestroyed >= 3) {
						Achievement.achieve(Achievement.SOBIESKI);
						running = false;
						return;
					}
				}
			}
		}
	}
	
	public static strictfp class AirSuperiority extends CombatAchievement {
		@Override
		public void tick(Combat c, Side mySide) {
			if (mySide.aircraftDownedByAircraft >= 5) {
				Achievement.achieve(Achievement.AIR_SUPERIORITY);
			}
		}
	}
	
	public static strictfp class DestroyByRamming extends CombatAchievement {
		public static final int RAM_TO_DESTROY_MS = 256;
		public HashMap<Airship, Integer> rammedTimes = new HashMap<Airship, Integer>();
		public boolean running = !Achievement.isAchieved(Achievement.RAMMING);
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			int time = c.time;
			Side enemy = c.otherSide(mySide);
			for (Airship enemyShip : enemy.ships) {
				if (enemyShip.type == ShipType.AIRSHIP) {
					for (Airship rammer : enemyShip.justRammedBy) {
						if (rammer.type != ShipType.AIRSHIP) { continue; }
						if (mySide.ships.contains(rammer)) {
							rammedTimes.put(enemyShip, time);
						}
					}
				}
				enemyShip.justRammedBy.clear();
			}
			for (Map.Entry<Airship, Integer> ramEntry : rammedTimes.entrySet()) {
				if (time - ramEntry.getValue() <= RAM_TO_DESTROY_MS &&
					(
						(
						!enemy.ships.contains(ramEntry.getKey()) &&
						!mySide.ships.contains(ramEntry.getKey()) &&
						!enemy.reserve.contains(ramEntry.getKey()) &&
						!mySide.reserve.contains(ramEntry.getKey())
						)
						|| 
						!ramEntry.getKey().inCombat(c)
					)
				)
				{
					Achievement.achieve(Achievement.RAMMING);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class ForceDown extends CombatAchievement {
		public HashMap<Airship, Integer> collidedTimes = new HashMap<Airship, Integer>();
		public boolean running = !Achievement.isAchieved(Achievement.FORCE_DOWN);
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			int time = c.time;
			Side enemy = c.otherSide(mySide);
			for (Airship enemyShip : enemy.ships) {
				if (enemyShip.type == ShipType.AIRSHIP) {
					for (Airship collider : enemyShip.justCollidedWith) {
						if (collider.type != ShipType.AIRSHIP) { continue; }
						if (mySide.ships.contains(collider)) {
							if (collider.moveTo.y > collider.getY() && collider.getY() + collider.getBBHeight() / 2 < enemyShip.getY())
							collidedTimes.put(enemyShip, time);
						}
					}
				}
				enemyShip.justCollidedWith.clear();
				if (enemyShip.msSinceOnGround <= 64 && collidedTimes.containsKey(enemyShip) && time - collidedTimes.get(enemyShip) < 256) {
					Achievement.achieve(Achievement.FORCE_DOWN);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class RockLanding extends CombatAchievement {
		public boolean running = !Achievement.isAchieved(Achievement.LAND_ROCK);

		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			for (Airship as : mySide.ships) {
				if (as.type == ShipType.AIRSHIP && as.msSinceOnGround < 64 && as.collidedWithFloatingRock && as.msSuspendiumOff > 1024) {
					Achievement.achieve(Achievement.LAND_ROCK);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class Capture extends CombatAchievement {
		public boolean running = !Achievement.isAchieved(Achievement.BOARDING);

		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			for (Airship as : mySide.ships) {
				if (as.type == ShipType.AIRSHIP && as.captured) {
					Achievement.achieve(Achievement.BOARDING);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class EvadeBoarders extends CombatAchievement {
		public boolean running = !Achievement.isAchieved(Achievement.EVADE_BOARDERS);

		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			Side enemy = c.otherSide(mySide);
			for (Crewman cm : enemy.troops) {
				if (cm.attachedTo == null &&
					cm.type.canBoard && !cm.type.hasHook &&
					cm.ultimateBoardTarget != null &&
					cm.dy > 0.75)
				{
					Achievement.achieve(Achievement.EVADE_BOARDERS);
					running = false;
					return;
				}
			}
		}
	}
	
	public static strictfp class Broadside extends CombatAchievement {
		public LinkedList<Integer> heavyCannonTimes = new LinkedList<Integer>();
		public boolean running = !Achievement.isAchieved(Achievement.BROADSIDE);

		@Override
		public void tick(Combat c, Side mySide) {
			if (!running) { return; }
			int time = c.time;
			for (Iterator<Integer> it = heavyCannonTimes.iterator(); it.hasNext();) {
				if (time - it.next() > 512) {
					it.remove();
				}
			}
			for (Airship ship : mySide.ships) {
				for (Module m : ship.modules) {
					if ((m.type == ModuleType.ofName("HV_CANNON") || m.type == ModuleType.ofName("FLIPPED_HV_CANNON")) && m.fired) {
						heavyCannonTimes.add(time);
					}
				}
			}
			if (heavyCannonTimes.size() >= 10) {
				Achievement.achieve(Achievement.BROADSIDE);
				running = false;
			}
		}
	}
	
	public static strictfp class CrushDepth extends CombatAchievement {
		public boolean done;
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!done && mySide.shipGotCrushed) {
				Achievement.achieve(Achievement.A_TITAN_OF_THE_SEAS);
			}
		}	
	}
	
	public static strictfp class MileDeepClub extends CombatAchievement {
		public HashMap<Airship, Integer> collidedTimes = new HashMap<Airship, Integer>();
		public boolean running = !Achievement.isAchieved(Achievement.MILE_DEEP_CLUB);
		
		@Override
		public void tick(Combat c, Side mySide) {
			if (!running || !c.landFormations.get(0).landscapeType.deepWater) { return; }
			int time = c.time;
			Side enemy = c.otherSide(mySide);
			for (Airship enemyShip : enemy.ships) {
				if (enemyShip.type == ShipType.AIRSHIP) {
					for (Airship collider : enemyShip.justCollidedWith) {
						if (collider.type != ShipType.AIRSHIP) { continue; }
						if (mySide.ships.contains(collider)) {
							if (collider.moveTo.y > collider.getY() && collider.getY() + collider.getBBHeight() / 2 < enemyShip.getY()) {
								collidedTimes.put(enemyShip, time);
							}
						}
					}
				}
				enemyShip.justCollidedWith.clear();
				if (enemyShip.getY() > AGame.GROUND_LEVEL && collidedTimes.containsKey(enemyShip) && time - collidedTimes.get(enemyShip) < 256) {
					Achievement.achieve(Achievement.MILE_DEEP_CLUB);
					running = false;
					return;
				}
			}
		}
	}
}
