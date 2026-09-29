package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Utils.Pair;

import java.util.ArrayList;
import java.util.Collections;

public strictfp class Tentacle {
	public ArrayList<Segment> segments = new ArrayList<Segment>();
	public double goalX, goalY;
	public double closestDistToTarget = 100000000;
	public int msNotImproved = 0;
	public int backOffMs = 0;
	public int backOffMsAmt = 320;
	public boolean allowAnySegmentToBeAtTarget = false;
	public boolean atTarget = false;
	public boolean flipped = false;
	
	public final TentacleSpec spec;
	
	public boolean needMsUntilNextTarget = true;
	public int msUntilNextTarget = -1;
	public boolean needsInitialTarget = true;
	
	// Crew grabbing variables
	public Crewman targetCrew;
	public Airship targetCrewShip;
	public boolean targetCrewGrabbed;
	
	// Smacking variables
	public Tile smackTile;
	public int smackPhase = 0; // Retreat, smack.

	public Tentacle(TentacleSpec spec) {
		this.spec = spec;
		double suckerDirection = spec.suckerDirection ? 1 : -1;
		for (int i = 0; i < spec.numSegments; i++) {
			double width = spec.baseWidth + (spec.tipWidth - spec.baseWidth) * i / spec.numSegments;
			double length = spec.baseLength + (spec.tipLength - spec.baseLength) * i / spec.numSegments;
			double localRequiredAngleVsTarget = suckerDirection * StrictMath.max(0, StrictMath.min(StrictMath.PI * 0.1, StrictMath.sin(i * 4.0 / spec.numSegments - 0.5) * 0.4));
			double stiffness = spec.baseStiffness + (spec.tipStiffness - spec.baseStiffness) * i / spec.numSegments;
			segments.add(new Segment(width,
					new Muscle(length * stiffness, length),
					new Muscle(length * stiffness, length),
					0, 0, i == 0 ? spec.baseAngle : 0, localRequiredAngleVsTarget));
		}
	}
	
	public boolean canTentacle(Airship ship, Module myModule, Airship target, double myX, double myY, double targetX, double targetY, boolean myFlipped, int inset, double rangeMult) {
		// Find expected base position.
		double baseX = myX + ship.gridXToWorldX(myModule.x, myModule.type.getW(), myFlipped) * AGame.SGS;
		if (myFlipped) {
			baseX += myModule.type.getW() * AGame.SGS;
		}
		baseX += spec.baseXOffset * AGame.SGS * (myFlipped ? -1 : 1);
		double baseY = myY + myModule.y * AGame.SGS + spec.baseYOffset;
		// Calculate range and fire angle.
		double maxRange = (spec.baseLength * 0.5 + spec.tipLength * 0.5) * spec.numSegments * rangeMult;
		Arc fireArc = Arc.centeredRadians(myFlipped ? Direction.flipHorizontal(spec.baseAngle) : spec.baseAngle, StrictMath.PI * 3 / 2);		
		// Calculate distance & angle for the 4 corners of the target.
		double targetLeft = targetX + inset;
		double targetRight = targetX + target.getBBWidth() - inset;
		double targetTop = targetY + inset;
		double targetBottom = targetY + target.getBBHeight() - inset;
		// Top left
		double dSq = (baseX - targetLeft) * (baseX - targetLeft) + (baseY - targetTop) * (baseY - targetTop);
		if (dSq <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetLeft, targetTop))) {
			return true;
		}
		// Bottom right
		dSq = (baseX - targetRight) * (baseX - targetRight) + (baseY - targetBottom) * (baseY - targetBottom);
		if (dSq <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetRight, targetBottom))) {
			return true;
		}
		// Top right
		dSq = (baseX - targetRight) * (baseX - targetRight) + (baseY - targetTop) * (baseY - targetTop);
		if (dSq <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetRight, targetTop))) {
			return true;
		}
		// Bottom left
		dSq = (baseX - targetLeft) * (baseX - targetLeft) + (baseY - targetBottom) * (baseY - targetBottom);
		if (dSq <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetLeft, targetBottom))) {
			return true;
		}
		return false;
	}
	
	public void updateTarget(double goalX, double goalY) {
		this.goalX = goalX;
		this.goalY = goalY;
	}

	public void setTarget(double goalX, double goalY, double speed) {
		this.goalX = goalX;
		this.goalY = goalY;
		backOffMs = 0;
		backOffMsAmt = (int) (320 / spec.speed);
		msNotImproved = 0;
		closestDistToTarget = 100000000;
		atTarget = false;
		// Retract the tentacle if there is a >>90 degree angle between tentacle tip and target?
		Segment tip = segments.get(segments.size() - 1);
		Segment base = segments.get(0);
		Direction baseToTip = Direction.fromTo(base.centerX, base.centerY, tip.centerX, tip.centerY);
		Direction baseToGoal = Direction.fromTo(base.centerX, base.centerY, goalX, goalY);
	}
	
	public void setFlipped(double parentX, double parentY, boolean flipped) {
		if (this.flipped != flipped) {
			segments.get(0).centerX = parentX + spec.baseXOffset * AGame.SGS * (flipped ? -1 : 1);
			segments.get(0).centerY = parentY + spec.baseYOffset * AGame.SGS;
			segments.get(0).angle = flipped ? Direction.flipHorizontal(spec.baseAngle) : spec.baseAngle;
			segments.get(0).angleSin = StrictMath.sin(segments.get(0).angle);
			segments.get(0).angleCos = StrictMath.cos(segments.get(0).angle);
			for (int i = 1; i < segments.size(); i++) {
				Segment s = segments.get(i);
				double l = s.leftM.length;
				s.leftM.length = s.rightM.length;
				s.rightM.length = l;
				s.updatePosition(segments.get(i - 1));
			}
		}
		this.flipped = flipped;
	}
	
	public final void reset(double parentX, double parentY, boolean flipped) {
		this.flipped = flipped;
		segments.get(0).centerX = parentX + spec.baseXOffset * AGame.SGS * (flipped ? -1 : 1);
		segments.get(0).centerY = parentY + spec.baseYOffset * AGame.SGS;
		segments.get(0).angle = flipped ? Direction.flipHorizontal(spec.baseAngle) : spec.baseAngle;
		segments.get(0).angleSin = StrictMath.sin(segments.get(0).angle);
		segments.get(0).angleCos = StrictMath.cos(segments.get(0).angle);
		for (int i = 1; i < segments.size(); i++) {
			Segment s = segments.get(i);
			double curlAmt = 0.95 - i * 0.4 / segments.size();
			s.leftM.length = s.leftM.minLength + (s.leftM.maxLength - s.leftM.minLength) * ((spec.suckerDirection ^ flipped) ? 1.0 : curlAmt);
			s.rightM.length = s.rightM.minLength + (s.rightM.maxLength - s.rightM.minLength) * (!(spec.suckerDirection ^ flipped) ? 1.0 : curlAmt);
			s.updatePosition(segments.get(i - 1));
		}
		needMsUntilNextTarget = true;
		needsInitialTarget = true;
	}
	
	public double crewX(Crewman cm, Airship ship) {
		if (ship != null) {
			return ship.getX() + ship.gridXToWorldX(cm.currentTile.x, 1) * AGame.SGS + AGame.SGS / 2;
		} else {
			return cm.getX() + cm.getBBWidth() / 2;
		}
	}
	
	public double crewY(Crewman cm, Airship ship) {
		if (ship != null) {
			return ship.getY() + cm.currentTile.y * AGame.SGS + AGame.SGS / 2;
		} else {
			return cm.getY() + cm.getBBHeight() / 2;
		}
	}
	
	public void smackBehaviour(Combat c, Airship myShip, Combat.Side mySide, double parentX, double parentY, boolean onViewingSide) {
		if (targetCrew != null) { return; }
		Combat.Side enemySide = c.otherSide(mySide);
		double baseX = segments.get(0).centerX;
		double baseY = segments.get(0).centerY;
		Arc fireArc = Arc.centeredRadians(segments.get(0).angle, StrictMath.PI * 3 / 2);
		double maxRange = (spec.baseLength * 0.5 + spec.tipLength * 0.5) * spec.numSegments * 0.8;
		// Check tile still in range and existant
		if (smackTile != null && !enemySide.ships.contains(smackTile.ship)) {
			smackTile = null;
			allowAnySegmentToBeAtTarget = false;
		}
		if (smackTile != null) {
			double stx = smackTile.ship.getX() + smackTile.ship.gridXToWorldX(smackTile.x, 1) * AGame.SGS + AGame.SGS / 2;
			double sty = smackTile.ship.getY() + smackTile.y * AGame.SGS + AGame.SGS / 2;
			double d2 = (baseX - stx) * (baseX - stx) + (baseY - sty) * (baseY - sty);
			if (d2 > maxRange * maxRange || !fireArc.contains(Direction.radiansFromTo(baseX, baseY, stx, sty))) {
				smackTile = null;
				allowAnySegmentToBeAtTarget = false;
			}
		}
		// If no tt, pick target tile: nice range //, not enterable and no window
		if (smackTile == null && msUntilNextTarget <= 0) {
			ArrayList<Tile> candidates = new ArrayList<Tile>();
			//System.out.println("base " + ((int) baseX) + " " + ((int) baseY));
			for (Airship s : enemySide.ships) {
				for (Tile t : s.tiles) {
					double stx = t.ship.getX() + t.ship.gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2;
					double sty = t.ship.getY() + t.y * AGame.SGS + AGame.SGS / 2;
					//System.out.println("st " + ((int) stx) + " " + ((int) sty));
					double d2 = (baseX - stx) * (baseX - stx) + (baseY - sty) * (baseY - sty);
					if (d2 <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, stx, sty)) && !t.isMaskedEmpty()) {
						candidates.add(t);
					}
				}
			}
			Collections.shuffle(candidates, c.r.getRandom());
			if (!candidates.isEmpty()) {
				smackTile = candidates.get(0);
				smackPhase = 0;
				double tx = baseX + segments.get(0).angleCos * maxRange / 6;
				double ty = baseY - maxRange / 6;
				setTarget(tx, ty, spec.speed * 10);
			} else {
			}
			needMsUntilNextTarget = true;
		}
		if (smackTile != null) {
			// Phase 0: retreat tentacle to point along and above base
			if (smackPhase == 0) {
				if (atTarget || msNotImproved > 1000) {
					smackPhase = 1;
					double stx = smackTile.ship.getX() + smackTile.ship.gridXToWorldX(smackTile.x, 1) * AGame.SGS + AGame.SGS / 2;
					double sty = smackTile.ship.getY() + smackTile.y * AGame.SGS + AGame.SGS / 2;
					setTarget(stx, sty, spec.speed * 10);
					allowAnySegmentToBeAtTarget = true;
					if (spec.attackSound != null) {
						c.play(spec.attackSound, stx, sty, 0, 0, onViewingSide);
					}
					if (spec.attackSprayParticle != null) {
						for (Segment seg : segments) {
							if (AGame.ANIM_R.nextDouble() > spec.attackSprayP) { continue; }
							double sx = seg.centerX + seg.xOffset + (AGame.ANIM_R.nextDouble() - 0.5) * StrictMath.min(seg.getLength(), seg.getWidth());
							double sy = seg.centerY + seg.yOffset + (AGame.ANIM_R.nextDouble() - 0.5) * StrictMath.min(seg.getLength(), seg.getWidth());
							c.particles.add(new Particle(spec.attackSprayParticle, sx, sy));
						}
					}
				} else {
					double tx = baseX + segments.get(0).angleCos * maxRange / 6;
					double ty = baseY - maxRange / 6;
					updateTarget(tx, ty);
				}
			}
			// Phase 1: move tentacle to target. do damage at impact, request new target
			if (smackPhase == 1) {
				if (atTarget) {
					// Smack the tile!
					double stx = smackTile.ship.getX() + smackTile.ship.gridXToWorldX(smackTile.x, 1) * AGame.SGS + AGame.SGS / 2;
					double sty = smackTile.ship.getY() + smackTile.y * AGame.SGS + AGame.SGS / 2;
					smackTile.hit(new Shot(smackTile.ship, stx, sty, myShip, this), c, !onViewingSide, null);
					int dmg = spec.attackBlastDmg + spec.attackPenDmg;
					
					if (dmg < 15) {
						c.play(MiscCombatSound.SMALL_HIT, stx, sty, 0, 0, !onViewingSide);
					} else if (dmg < 40) {
						c.play(MiscCombatSound.MEDIUM_HIT, stx, sty, 0, 0, !onViewingSide);
					} else {
						c.play(MiscCombatSound.LARGE_HIT, stx, sty, 0, 0, !onViewingSide);
					}
					
					c.msSinceInterestingCombatEvent = 0;
					if (spec.attackImpactParticle != null) {
						for (int i = 0; i < spec.numAttackImpactParticles; i++) {
							c.particles.add(new Particle(spec.attackImpactParticle, stx, sty));
						}
					}
					
					double tx = baseX + segments.get(0).angleCos * maxRange / 6;
					double ty = baseY - maxRange / 6;
					setTarget(tx, ty, spec.speed * 10);
					smackTile = null;
					allowAnySegmentToBeAtTarget = false;
				} else if (msNotImproved > 1500) {
					double tx = baseX + segments.get(0).angleCos * maxRange / 6;
					double ty = baseY - maxRange / 6;
					setTarget(tx, ty, spec.speed * 10);
					smackTile = null;
					allowAnySegmentToBeAtTarget = false;
				} else {
					double stx = smackTile.ship.getX() + smackTile.ship.gridXToWorldX(smackTile.x, 1) * AGame.SGS + AGame.SGS / 2;
					double sty = smackTile.ship.getY() + smackTile.y * AGame.SGS + AGame.SGS / 2;
					updateTarget(stx, sty);
					//System.out.println("p1");
				}
			}
		}
	}
	
	public void crewGrabBehaviour(Combat c, Airship myShip, Combat.Side mySide, Module myModule, double parentX, double parentY, boolean onViewingSide) {
		Combat.Side enemySide = c.otherSide(mySide);
		double baseX = segments.get(0).centerX;
		double baseY = segments.get(0).centerY;
		Segment tip = segments.get(segments.size() - 1);
		if (targetCrew != null && targetCrewGrabbed) {
			targetCrew.setX(tip.centerX + tip.xOffset - targetCrew.getBBWidth() / 2);
			targetCrew.setY(tip.centerY + tip.yOffset - targetCrew.getBBHeight() / 2);
			targetCrew.dx = 0;
			targetCrew.dy = 0;
			if (atTarget) {
				boolean alive = targetCrew.alive();
				targetCrew.hp = 0;
				double cx = targetCrew.getX() + targetCrew.getBBWidth() / 2;
				double cy = targetCrew.getY() + targetCrew.getBBHeight() / 2;
				if (alive && !targetCrew.alive() && targetCrew.type.deathSnd != null) {
					c.play(targetCrew.type.deathSnd, cx, cy, 0, 0, onViewingSide);
				}
				if (alive && !targetCrew.alive()) {
					c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("eatenBy " + myModule.type.name, c.otherSide(mySide), cx, cy, targetCrew.type, null));
					c.incStat(targetCrew, "eatenBy " + myModule.type.name);
				}
				if (targetCrew.type.bloodParticleExternal != null) {
					for (int i = 0; i < 20; i++) {
						Particle p = new Particle(targetCrew.type.bloodParticleExternal,
							cx,
							cy);
						c.particles.add(p);
					}
				}
				targetCrew = null;
				targetCrewShip = null;
				atTarget = false;
				targetCrewGrabbed = false;
				needsInitialTarget = true; // This will cause the tentacles to wave around for a bit.
			} else {
				updateTarget(
						parentX + spec.mouthXOffset * AGame.SGS * (flipped ? -1 : 1),
						parentY + spec.mouthYOffset * AGame.SGS);
				return;
			}
		}
		
		// Maybe someone else has the target.
		if (targetCrew != null && targetCrew.grabbed) {
			targetCrew = null;
			targetCrewShip = null;
		}
		
		// Check the target is still there.
		if (targetCrewShip != null && !mySide.ships.contains(targetCrewShip) && !enemySide.ships.contains(targetCrewShip)) {
			targetCrewShip = null;
		}
		if (targetCrew != null) {
			if (targetCrewShip != null) {
				if (!targetCrewShip.crew.contains(targetCrew) && !targetCrewShip.boarders.contains(targetCrew)) {
					targetCrewShip = null;
				}
			}
			if (targetCrewShip == null) {
				if (!enemySide.troops.contains(targetCrew)) {
					targetCrew = null;
				}
			}
		}
		
		Arc fireArc = Arc.centeredRadians(segments.get(0).angle, StrictMath.PI * 3 / 2);
		double maxRange = (spec.baseLength * 0.5 + spec.tipLength * 0.5) * spec.numSegments * 0.8;

		if (targetCrew != null) {			
			// Check the target is still in reach.
			double targetX = crewX(targetCrew, targetCrewShip), targetY = crewY(targetCrew, targetCrewShip);
			
			double dSq = (baseX - targetX) * (baseX - targetX) + (baseY - targetY) * (baseY - targetY);
			if (dSq > maxRange * maxRange) {
				targetCrew = null;
				targetCrewShip = null;
			} else if (!fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetX, targetY))) {
				targetCrew = null;
				targetCrewShip = null;
			}
		}
		if (targetCrew == null && msUntilNextTarget <= 0) {
			ArrayList<Pair<Crewman, Airship>> candidates = new ArrayList<Pair<Crewman, Airship>>();
			for (Crewman cm : enemySide.troops) {
				if (!cm.alive() || cm.grabbed) { continue; }
				double targetX = crewX(cm, null), targetY = crewY(cm, null);
				double dSq = (baseX - targetX) * (baseX - targetX) + (baseY - targetY) * (baseY - targetY);
				if (dSq <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetX, targetY))) {
					candidates.add(new Pair<Crewman, Airship>(cm, null));
				}
			}
			for (Airship s : enemySide.ships) {
				for (Crewman cm : s.crew) {
					if (!cm.grabbed && cm.alive() && (cm.currentTile.enterable() || cm.currentTile.originallyWindow())) {
						double targetX = crewX(cm, s), targetY = crewY(cm, s);
						double dSq = (baseX - targetX) * (baseX - targetX) + (baseY - targetY) * (baseY - targetY);
						if (dSq <= maxRange * maxRange && fireArc.contains(Direction.radiansFromTo(baseX, baseY, targetX, targetY))) {
							candidates.add(new Pair<Crewman, Airship>(cm, s));
						}
					}
				}
			}
			// qqDPS Prevent double-targeting at least within this ship.
			Collections.shuffle(candidates, c.r.getRandom());
			//System.out.println(candidates.size() + " candidates");
			if (!candidates.isEmpty()) {
				targetCrew = candidates.get(0).a;
				targetCrewShip = candidates.get(0).b;
				double targetX = crewX(targetCrew, targetCrewShip), targetY = crewY(targetCrew, targetCrewShip);
				setTarget(targetX, targetY, spec.speed);
			}
			needMsUntilNextTarget = true;
		}
		
		if (targetCrew != null) {
			double targetX = crewX(targetCrew, targetCrewShip), targetY = crewY(targetCrew, targetCrewShip);
			
			if (atTarget) {
				targetCrewGrabbed = true;
				if (targetCrewShip != null) {
					targetCrew.popOut(enemySide, c, 1);
					targetCrewShip = null;
					// Show wall breakage. qqDPS
				}
				if (spec.attackSound != null) {
					c.play(spec.snatchSound, targetX, targetY, 0, 0, onViewingSide);
				}
				
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("grabbedBy " + myModule.type.name, c.otherSide(mySide), targetX, targetY, targetCrew.type, targetCrewShip));

				targetCrew.setX(tip.centerX + tip.xOffset - targetCrew.getBBWidth() / 2);
				targetCrew.setY(tip.centerY + tip.yOffset - targetCrew.getBBHeight() / 2);
				targetCrew.dx = 0;
				targetCrew.dy = 0;
				targetCrew.grabbed = true;
				setTarget(
						parentX + spec.mouthXOffset * AGame.SGS * (flipped ? -1 : 1),
						parentY + spec.mouthYOffset * AGame.SGS,
						spec.speed);
			} else {
				updateTarget(targetX, targetY);
			}
		}
	}
	
	public void deathSpasm(Combat c, Airship myShip, int ms, double parentX, double parentY) {
		segments.get(0).centerX = parentX + spec.baseXOffset * AGame.SGS * (flipped ? -1 : 1);
		segments.get(0).centerY = parentY + spec.baseYOffset * AGame.SGS;
		for (int i = 1; i < segments.size(); i++) {
			if (c.r.nextDouble() < 0.0003 * ms) {
				double amt = 100 * c.r.nextDouble();
				for (int j = StrictMath.max(1, i - 5); j < StrictMath.min(segments.size(), i + 5); j++) {
					segments.get(j).leftM.spasmAmount += amt / ((i - j) * (i - j) + 2);
				}
			}
			segments.get(i).leftM.spasmMove(ms * spec.speed * 2);
			if (c.r.nextDouble() < 0.0003 * ms) {
				double amt = 100 * c.r.nextDouble();
				for (int j = StrictMath.max(1, i - 5); j < StrictMath.min(segments.size(), i + 5); j++) {
					segments.get(j).rightM.spasmAmount += amt / ((i - j) * (i - j) + 2);
				}
			}
			segments.get(i).rightM.spasmMove(ms * spec.speed * 2);
			segments.get(i).updatePosition(segments.get(i - 1));
		}
	}
	
	public void holdStill(Combat c, Airship myShip, int ms, double parentX, double parentY) {
		segments.get(0).centerX = parentX + spec.baseXOffset * AGame.SGS * (flipped ? -1 : 1);
		segments.get(0).centerY = parentY + spec.baseYOffset * AGame.SGS;
		for (int i = 1; i < segments.size(); i++) {
			segments.get(i).updatePosition(segments.get(i - 1));
		}
		Segment tip = segments.get(segments.size() - 1);
		if (targetCrew != null && targetCrewGrabbed) {
			targetCrew.setX(tip.centerX + tip.xOffset - targetCrew.getBBWidth() / 2);
			targetCrew.setY(tip.centerY + tip.yOffset - targetCrew.getBBHeight() / 2);
			targetCrew.dx = 0;
			targetCrew.dy = 0;
		}
	}

	public void tick(Combat c, Airship myShip, Combat.Side mySide, Module myModule, int ms, double parentX, double parentY, boolean onViewingSide) {
		Combat.Side otherS = c.otherSide(mySide);
		if (targetCrew != null) {
			if ((targetCrewShip == null || !targetCrewShip.crew.contains(targetCrew)) && !otherS.troops.contains(targetCrew)) {
				System.out.println("Misplaced grabbed crew");
				targetCrew.grabbed = false;
				targetCrew = null;
				targetCrewShip = null;
				targetCrewGrabbed = false;
			}
		}
		double baseX = parentX + spec.baseXOffset * AGame.SGS * (flipped ? -1 : 1);
		double baseY = parentY + spec.baseYOffset * AGame.SGS;;
		segments.get(0).centerX = baseX;
		segments.get(0).centerY = baseY;
		
		if (needMsUntilNextTarget) {
			msUntilNextTarget = spec.minNextTargetPause + c.r.nextInt(spec.maxNextTargetPause - spec.minNextTargetPause);
			needMsUntilNextTarget = false;
		}
		
		msUntilNextTarget -= ms;
		
		if (needsInitialTarget) {
			if (spec.wavesAround) {
				double randomDir = segments.get(0).angle - 1 + c.r.nextDouble() * 2;
				double maxRange = (spec.baseLength * 0.5 + spec.tipLength * 0.5) * spec.numSegments * 0.8;
				double randomDist = maxRange * 0.3 + 0.7 * c.r.nextDouble() * maxRange;
				setTarget(baseX + StrictMath.cos(randomDir) * randomDist, baseY + StrictMath.sin(randomDir) * randomDist, spec.speed);
			} else {
				setTarget(segments.get(segments.size() - 1).centerX, segments.get(segments.size() - 1).centerY, spec.speed);
			}
			
			needsInitialTarget = false;
		}
		
		if (spec.snatchesCrew) {
			crewGrabBehaviour(c, myShip, mySide, myModule, parentX, parentY, onViewingSide);
		}
		
		if (spec.attacksHull) {
			smackBehaviour(c, myShip, mySide, parentX, parentY, onViewingSide);
		}
		
		if (spec.wavesAround && targetCrew == null && smackTile == null && msUntilNextTarget <= 0) {
			double randomDir = segments.get(0).angle - 1 + c.r.nextDouble() * 2;
			double maxRange = (spec.baseLength * 0.5 + spec.tipLength * 0.5) * spec.numSegments * 0.8;
			double randomDist = maxRange * 0.3 + 0.7 * c.r.nextDouble() * maxRange;
			setTarget(baseX + StrictMath.cos(randomDir) * randomDist, baseY + StrictMath.sin(randomDir) * randomDist, spec.speed);
			needMsUntilNextTarget = true;
		}
				
		for (int i = 1; i < segments.size(); i++) {
			segments.get(i).updatePosition(segments.get(i - 1));
		}
		
		double speed = spec.speed;
		if (smackTile != null && smackPhase == 1) {
			speed *= 10;
		}
		
		for (Segment s : segments) {
			s.leftM.returnToBalance(ms * speed);
			s.rightM.returnToBalance(ms * speed);
		}
		
		for (int i = 1; i < segments.size(); i++) {
			if (backOffMs <= 0) {
				segments.get(i).moveMusclesForTarget(ms * speed, goalX, goalY);
			}
			segments.get(i).updatePosition(segments.get(i - 1));
		}
		
		Segment tip = segments.get(segments.size() - 1);
		double d2 = (tip.centerX - goalX) * (tip.centerX - goalX) + (tip.centerY - goalY) * (tip.centerY - goalY);
		if (backOffMs > 0) {
			backOffMs -= ms;
		} else {
			backOffMs = 0;
		}
		int maxShiftDistSq = (segments.size() - 2) * (segments.size() - 2) * 3;
		double shiftAmt = ms * speed * 0.3;
		if (d2 < maxShiftDistSq) {
			for (int i = 1; i < segments.size(); i++) {
				Segment seg = segments.get(i);
				Segment prev = segments.get(i - 1);
				double localXOffset = seg.xOffset - prev.xOffset;
				double localYOffset = seg.yOffset - prev.yOffset;
				double targetXOffset = (goalX - tip.centerX) / (segments.size() - 1);
				double targetYOffset = (goalY - tip.centerY) / (segments.size() - 1);
				double totalOffsetDifference = StrictMath.abs(localXOffset - targetXOffset) + StrictMath.abs(localYOffset - targetYOffset);
				if (totalOffsetDifference <= shiftAmt) {
					localXOffset = targetXOffset;
					localYOffset = targetYOffset;
				} else {
					localXOffset += shiftAmt * (targetXOffset - localXOffset) / totalOffsetDifference;
					localYOffset += shiftAmt * (targetYOffset - localYOffset) / totalOffsetDifference;
				}
				seg.xOffset = prev.xOffset + localXOffset;
				seg.yOffset = prev.yOffset + localYOffset;
			}
		} else {
			for (int i = 1; i < segments.size(); i++) {
				Segment seg = segments.get(i);
				Segment prev = segments.get(i - 1);
				double localXOffset = seg.xOffset - prev.xOffset;
				double localYOffset = seg.yOffset - prev.yOffset;
				double totalOffset = StrictMath.abs(localXOffset) + StrictMath.abs(localYOffset);
				if (totalOffset <= shiftAmt) {
					localXOffset = 0;
					localYOffset = 0;
				} else {
					localXOffset -= shiftAmt * localXOffset / totalOffset;
					localYOffset -= shiftAmt * localYOffset / totalOffset;
				}
				seg.xOffset = prev.xOffset + localXOffset;
				seg.yOffset = prev.yOffset + localYOffset;
			}
		}
		d2 = (tip.centerX + tip.xOffset - goalX) * (tip.centerX + tip.xOffset - goalX) + (tip.centerY + tip.yOffset - goalY) * (tip.centerY + tip.yOffset - goalY);
		if (d2 < 100) {
			atTarget = true;
		} else {
			if (allowAnySegmentToBeAtTarget) {
				for (Segment seg : segments) {
					double sd2 = (seg.centerX + seg.xOffset - goalX) * (seg.centerX + seg.xOffset - goalX) + (seg.centerY + seg.yOffset - goalY) * (seg.centerY + seg.yOffset - goalY);
					if (sd2 < seg.getWidth() * seg.getLength()) {
						atTarget = true;
						break;
					}
				}
			}
			if (!atTarget) {
				if (d2 < closestDistToTarget - 5000) {
					closestDistToTarget = d2;
					msNotImproved = 0;
				} else {
					if (backOffMs <= 0) {
						msNotImproved += ms;
					}
					if (msNotImproved > 700 / spec.speed + 2000) {
						backOffMs = backOffMsAmt;
						backOffMsAmt *= 2;
						msNotImproved = 0;
						closestDistToTarget = 100000000;
					}
				}
			}
		}
		
		if (spec.tipEmitter != null) {
			Particle.Emitter em = spec.tipEmitter;
			if (AGame.ANIM_R.nextDouble() < em.emitProbability * ms) {
				for (int i = 0; i < em.numParticles; i++) {
					c.particles.add(new Particle(em.t, tip.centerX, tip.centerY));
				}
				if (em.soundEffect != null) {
					c.play(em.soundEffect, tip.centerX, tip.centerY, 0, 0, onViewingSide);
				}
			}
		}
	}
	
	public static class Muscle {
		public final double minLength, maxLength;
		public double length;
		public double spasmAmount;

		public Muscle(double minLength, double maxLength) {
			this.minLength = minLength;
			this.maxLength = maxLength;
			length = maxLength;
		}
		
		private void spasmMove(double msXSpeed) {
			spasmAmount *= StrictMath.pow(0.99, msXSpeed);
			contract(msXSpeed * spasmAmount);
			returnToBalance(msXSpeed);
		}
		
		private void returnToBalance(double msXSpeed) {
			length = length * (1 - 0.00125 * msXSpeed) + maxLength * 0.00125 * msXSpeed;
		}
		
		private void contract(double msXSpeed) {
			length = length * (1 - 0.003125 * msXSpeed) + minLength * 0.003125 * msXSpeed;
		}
	}
	
	public static class Segment {
		public final double baseWidth;
		public final Muscle leftM, rightM;
		public double centerX, centerY, angle, angleSin, angleCos;
		public final double desiredAngleVsTarget;
		public double xOffset, yOffset;

		public Segment(double width, Muscle leftM, Muscle rightM, double x, double y, double angle, double desiredAngleVsTarget) {
			this.baseWidth = width;
			this.leftM = leftM;
			this.rightM = rightM;
			this.centerX = x;
			this.centerY = y;
			this.angle = angle;
			angleSin = StrictMath.sin(angle);
			angleCos = StrictMath.cos(angle);
			this.desiredAngleVsTarget = desiredAngleVsTarget;
		}
		
		public double getWidth() {
			return baseWidth;
		}
		
		public double getLength() {
			return leftM.length / 2 + rightM.length / 2;
		}
		
		public void updatePosition(Segment prev) {
			// Start point.
			double startX = prev.centerX + prev.angleCos * prev.getLength() / 2;
			double startY = prev.centerY + prev.angleSin * prev.getLength() / 2;
			// My angle
			double angleChange = StrictMath.atan2(leftM.length - rightM.length, baseWidth);
			angle = prev.angle + angleChange;
			angleSin = StrictMath.sin(angle);
			angleCos = StrictMath.cos(angle);
			// Midpoint.
			centerX = startX + angleCos * getLength() / 2;
			centerY = startY + angleSin * getLength() / 2;
		}
				
		public void moveMusclesForTarget(double msXSpeed, double tx, double ty) {
			double globalAngleVsTarget = StrictMath.atan2(ty - centerY, tx - centerX);
			double localAngleVsTarget = globalAngleVsTarget - angle;
			Direction reqA = Direction.ofRadians(desiredAngleVsTarget);
			Direction targA = Direction.ofRadians(localAngleVsTarget);
			double angleDiff = targA.minus(reqA).radians;
			if (angleDiff < StrictMath.PI) {
				double amt = StrictMath.min(0.15, angleDiff / StrictMath.PI / 2) * msXSpeed / 16.0;
				rightM.length = rightM.length * (1 - amt) + rightM.minLength * amt;
			} else {
				double amt = StrictMath.min(0.15, (2 - angleDiff / StrictMath.PI) / 2) * msXSpeed / 16.0;
				leftM.length = leftM.length * (1 - amt) + leftM.minLength * amt;
			}
		}
	}
}