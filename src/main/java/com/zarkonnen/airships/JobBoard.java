package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;

import java.util.ArrayList;
import java.util.Collections;

public strictfp class JobBoard implements UniScreen.ShipChrome {
	public static String indent(String s, int amt) {
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < amt) { sb.append(" "); }
		return sb.toString();
	}
	
	int skip = 0;

	@Override
	public void draw(MyDraw d, Pt cursor, Airship ship, Combat.Side side, int x, int y, int w, int h, ScreenMode sm, UniScreen us) {
		if (us.selectedShip != ship) { return; }
		StringBuilder sb = new StringBuilder();
		sb.append("[bg=000000]\n");
		int busyCrew = 0;
		int activeCrew = 0;
		for (Crewman cm : ship.crew) {
			if (cm.job != null) {
				busyCrew++;
			}
			if (cm.active()) {
				activeCrew++;
			}
		}
		sb.append("Busy: ").append(busyCrew).append(" Active: ").append(activeCrew).append("\n");
		sb.append(indent("Name", 20));
		sb.append(indent("Module", 30));
		sb.append(indent("P", 5));
		sb.append(indent("C", 2));
		sb.append("\n");
		ArrayList<Job> jobs = new ArrayList<Job>();
		for (Module m : ship.modules) {
			//if (!m.type.isWeapon()) { continue; }
			for (Job j : m.jobs()) {
				if (j.active()) {// && !(j instanceof Module.StaffJob || j instanceof Module.GuardJob || j instanceof Module.ReadyJob || j instanceof Module.FixedGuardJob)) {
					jobs.add(j);
				}
			}
		}
		Collections.sort(jobs, ship);
		int s = skip;
		for (Job j : jobs) {
			if (s > 0) {
				s--;
				continue;
			}
			boolean hasCrew = false;
			boolean crewIsInactive = false;
			Crewman crew = null;
			for (Crewman cm : ship.crew) {
				if (cm.job == j) {
					hasCrew = true;
					if (!cm.active()) {
						crewIsInactive = true;
					}
					crew = cm;
					break;
				}
			}
			sb.append(hasCrew && !crewIsInactive ? "[ffffff]" : MyDraw.ERROR_C);
			sb.append(indent(j.getClass().getSimpleName(), 20));
			sb.append(indent(j.module().type.getName(), 30));
			sb.append(indent("" + ((int) (j.priority() * 10)), 5));
			sb.append(indent(hasCrew ? crewIsInactive ? "I" : "Y" : "N", 2));
			
			if (crew != null) {
				sb.append("\n");
				sb.append("@").append(crew.currentTile.module.type.getName());
				if (crew.target == crew.currentTile.module) {
					sb.append(" @m");
					/*int c = crew.type.pickupMs;
					sb.append(" c=").append(c);
					if (crew.currentTile.module.fire > 0) { c *= crew.type.pickupFireMult; }
					c *= 1 + ((crew.currentTile.module.getMaxHP() - crew.currentTile.module.hp) * crew.type.pickupDmgMaxMalus / crew.currentTile.module.getMaxHP());
					sb.append(" PUC=").append(c);*/
				}
				if (crew.boarderTargetTile == crew.currentTile) {
					sb.append(" @t");
				} else {
					if (crew.boarderTargetTile != null) {
						sb.append(" tt=").append(crew.boarderTargetTile.x).append(" ").append(crew.boarderTargetTile.y);
					} else {
						sb.append(" tt=null");
					}
					sb.append(" ct=").append(crew.currentTile.x).append(" ").append(crew.currentTile.y);
				}
				sb.append("\nm=").append(crew.msSinceMoved);
				sb.append(" c=").append(crew.carrying);
				if (crew.movingTowards != null) {
					sb.append(" ->").append(crew.movingTowards.module.type.getName());
				}
				if (crew.target != null) {
					sb.append(" =>").append(crew.target.type.getName());
				}
			}
			
			sb.append("\n");
		}
		d.text(sb.toString(), AGame.FOUNT, 10, 50);
	}

	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (in.keyPressed("2")) {
			skip += 10;
		}
		if (in.keyPressed("1")) {
			skip -= 10;
		}
	}

	@Override
	public boolean textInputOccurring(UniScreen us) {
		return false;
	}
}
