package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;

public strictfp class Tile implements JSONAble, GridLocation {
	public static final double MIN_CREW_HARM_P = 1.5;
	public static final double PEN_TO_HARM = 0.1;
	public static final double BLAST_TO_HARM = 0.05;
	public static final double DIRECT_TO_HARM = 0.1;
	
	public transient int pathCostTmp;
	public int x, y;
	public ArmourPlate armour;
	public Module module;
	public final boolean[][] hadAdjacentOccupableTile = new boolean[3][3];
	public transient boolean canOccupy;
	public transient boolean[][] adjacent = new boolean[3][3];
	public transient boolean[][] adjacentFullTile = new boolean[3][3];
	public transient boolean[][] adjacentNonEmptyTile = new boolean[3][3];
	public transient Airship ship;
	public transient ArrayList<GridLocation> reachable = new ArrayList<GridLocation>();
	public transient Img[] splinters;
	public transient int testSplashDamage;
	
	public int cheapHash() {
		int h = 7;
		h = h * 31 + x;
		h = h * 31 + y;
		h = h * 31 + armour.hp;
		return h;
	}
	
	@Override
	public GridLocation getRelative(int dx, int dy) {
		return ship.tileAt(x + dx * (ship.flipped ? -1 : 1), y + dy);
	}
	
	@Override
	public ArrayList<GridLocation> reachable() {
		return reachable;
	}
	
	@Override
	public boolean enterable() {
		return canOccupy && (armour.hp <= 0 || armour.type.enterable || module.type.isHatch(ship.currentBonuses) ||
				(!adjacent[0][1] && hadAdjacentOccupableTile[0][1]) ||
				(!adjacent[2][1] && hadAdjacentOccupableTile[2][1]) ||
				(!adjacent[1][0] && hadAdjacentOccupableTile[1][0]) ||
				(!adjacent[1][2] && hadAdjacentOccupableTile[1][2]));
	}
	
	public void setShip(Airship ship) {
		this.ship = ship;
		this.armour.ship = ship;
	}
	
	public boolean isFullySubmerged() {
		return ship.inWater && ship.getY() + y * AGame.SGS > AGame.GROUND_LEVEL;
	}
	
	public boolean isPartiallySubmerged() {
		return ship.inWater && ship.getY() + y * AGame.SGS + AGame.SGS / 2 > AGame.GROUND_LEVEL;
	}
	
	public final boolean isMaskedEmpty() {
		return module.type.getTileMasks(ship.currentBonuses)[y - module.y][x - module.x] == TileMask.EMPTY;
	}
	
	public final TileMask mask() {
		return module.type.getTileMasks(ship.currentBonuses)[y - module.y][x - module.x];
	}
	
	public final void updateCanOccupy() {
		canOccupy = false;
		for (Pair<Integer, Integer> occ : module.type.getCanOccupy()) {
			if (occ.a == x - module.x && occ.b == y - module.y) {
				canOccupy = true;
				break;
			}
		}
	}

	public Tile(Airship ship, Module module, int x, int y) {
		this.ship = ship;
		this.module = module;
		this.x = x;
		this.y = y;
		boolean hasWin = false;
		for (Pair<Integer, Integer> win : module.type.getWindows()) {
			if (win.a == x - module.x && win.b == y - module.y) {
				hasWin = true;
				break;
			}
		}
		updateCanOccupy();
		armour = new ArmourPlate(hasWin, ship);
	}
	
	public boolean originallyWindow() {
		ArrayList<Pair<Integer, Integer>> wins = module.type.getWindows();
		for (int i = 0; i < wins.size(); i++) {
			Pair<Integer, Integer> win = wins.get(i);
			if (win.a == x - module.x && win.b == y - module.y) {
				return true;
			}
		}
		return false;
	}

	private Decal checkDecalDestruction() {
		for (Decal d : ship.decals) {
			if (d.enabled && d.x <= x && d.y <= y && d.x + d.type.w > x && d.y + d.type.h > y) {
				d.enabled = false;
				return d;
			}
		}
		return null;
	}
	
	// type: 0 pen 1 blast 2 direct
	public void splashHit(int type, Combat c, boolean onViewingSide, int dmg, Shot optionalSourceShot, boolean inside) {
		int prevArmourHP = armour.hp;
		double myX = ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + AGame.SGS / 2;
		double myY = ship.getIntY() + y * AGame.SGS + AGame.SGS / 2;
		if (optionalSourceShot != null) {
			if (optionalSourceShot.weapon != null) {
				c.changeModuleStat(optionalSourceShot.weapon.ship, optionalSourceShot.weapon.type, "dmg", dmg);
				optionalSourceShot.weapon.ship.changeStat("dmg", dmg);
				c.changeStat(optionalSourceShot.weapon.ship, "dmg", dmg);
			}
			if (optionalSourceShot.shooterType != null) {
				Combat.Side mySide = c.sideOf(ship);
				if (mySide != null) {
					c.otherSide(mySide).incStat("shipHitsByCrew " + optionalSourceShot.shooterType.name);
					c.otherSide(mySide).changeStat("shipDamageByCrew " + optionalSourceShot.shooterType.name, dmg);
				}
			}
			ship.changeStat("damageTaken", dmg);
			c.changeStat(ship, "damageTaken", dmg);
			ship.otherDamageTaken += dmg;
		}
		if (armour.hp > 0) {
			if (!inside) {
				int absorb = type == 0 ? armour.type.getPenDmgAbsorb(ship.currentBonuses) : type == 1 ? armour.type.getBlastDmgAbsorb(ship.currentBonuses) : 0;
				if (module.hp > 0 && type == 1) {
					absorb += module.getShellBlastAbsorb();
				}
				if (dmg <= absorb) {
					dmg = c.r.nextInt(2 + absorb - dmg) == 1 ? 1 : 0;
				} else {
					dmg -= absorb;
				}
			}
			if (armour.hp > dmg) {
				armour.hp -= dmg;
				if (!inside) { dmg = 0; }
			} else {
				if (!inside) { dmg -= armour.hp; }
				armour.hp = 0;
			}
			armour.hp = StrictMath.max(0, armour.hp);
			if (armour.hp == 0) {
				c.play(armour.type.brokenSound.get(ship.currentBonuses), myX, myY, 0, 0, !onViewingSide);
			} else if (dmg > armour.getMaxHP() / 2) {
				c.play(armour.type.largeHitSound.get(ship.currentBonuses), myX, myY, 0, 0, !onViewingSide);
			}
		} else if (module.hp > 0 && !inside) {
			int absorb = type == 1 ? 0 : module.getShellBlastAbsorb();
			if (dmg <= absorb) {
				dmg = c.r.nextInt(2 + absorb - dmg) == 1 ? 1 : 0;
			} else {
				dmg -= absorb;
			}
		}
		if (armour.hp < armour.getMaxHP() * 0.4) {
			Decal dest = checkDecalDestruction();
			if (dest != null) {
				for (int dx = dest.x; dx < dest.x + dest.type.w; dx++) {
					for (int dy = dest.y; dy < dest.y + dest.type.h; dy++) {
						for (int i = 0; i < 2; i++) {
							c.particles.add(new Particle(ParticleType.ofName("fragment"),
								ship.getIntX() + ship.gridXToWorldX(dx, 1) * AGame.SGS + AGame.SGS / 2,
								ship.getIntY() + dy * AGame.SGS + AGame.SGS / 2));
						}
					}
				}
			}
		}

		for (ModuleType.FragmentImg fragI : armour.getFragments(prevArmourHP, armour.hp)) {
			double speed = AGame.rnd(0.1, 0.25, 0.2, 0.4, 0.8) * 0.3;
			double angle = AGame.ANIM_R.nextDouble() * 2 * StrictMath.PI;
			double dx = speed * StrictMath.cos(angle);
			double dy = speed * StrictMath.sin(angle) - 0.3 * speed;
			c.fragments.add(new Fragment(
					fragI.ssb,
					fragI.img,
					ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + fragI.dx,
					ship.getIntY() + y * AGame.SGS + fragI.dy,
					dx, dy,
					0, AGame.ANIM_R.nextDouble() * 0.02 - 0.01,
					300 + AGame.ANIM_R.nextInt(400),
					/* burnAmt */4
			));
		}

		ParticleType hitParticle = module.type.hitParticle(ship.currentBonuses);
		if (hitParticle != null) {
			int nParticles = StrictMath.min(10, (dmg) / 10) + 1;
			for (int i = 0; i < nParticles; i++) {
				c.particles.add(new Particle(hitParticle, ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS, ship.getIntY() + y * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS));
			}
		}

		if (dmg == 0) { return; }
		if (optionalSourceShot != null) {
			if (optionalSourceShot.weapon != null) {
				c.changeModuleStat(optionalSourceShot.weapon.ship, optionalSourceShot.weapon.type, "moduleDmg", dmg);
				optionalSourceShot.weapon.ship.changeStat("moduleDmg", dmg);
				c.changeStat(optionalSourceShot.weapon.ship, "moduleDmg", dmg);
			}
			if (optionalSourceShot.shooterType != null) {
				Combat.Side mySide = c.sideOf(ship);
				if (mySide != null) {
					c.otherSide(mySide).changeStat("shipModuleDamageByCrew " + optionalSourceShot.shooterType.name, dmg);
				}
			}
		} else {
			ship.changeStat("explosionDamageTaken", dmg);
			c.changeStat(ship, "explosionDamageTaken", dmg);
		}
		ship.changeStat("moduleDamageTaken", dmg);
		c.changeStat(ship, "moduleDamageTaken", dmg);
		int prevModuleHP = module.hp;
		module.doDamage(dmg);
		boolean cheer = module.hp <= 0 && prevModuleHP > 0;

		double flammabilityFromShot = optionalSourceShot == null || optionalSourceShot.weapon == null ? 1 : optionalSourceShot.weapon.type.getSetFireMultiplier(optionalSourceShot.weaponBonuses) * EmpireStat.SHOT_FLAMMABILITY.get(optionalSourceShot.weapon.ship.currentBonuses);
		if (optionalSourceShot != null && optionalSourceShot.shooterType != null) {
			flammabilityFromShot = optionalSourceShot.shooterType.setFireMultiplier;
		}
		int adjFireHP = module.type.getFireHP(ship.currentBonuses) * module.maxHP / module.type.getHp(ship.currentBonuses);
		if (module.hp > 0 && module.hp < adjFireHP * flammabilityFromShot) {
			// So it should be this:
			//double fireRollVs = (pen + blast + direct) * 1.0 / module.maxHP;
			// But we wanna make flammable stuff more flammable yet.
			// The higher fireRollVs, the more likely is fire. So for higher adjFireHP values, it should be bigger.
			// So:
			double fireRollVs = (dmg) * (type == 1 ? 1.5 : 0.5) * adjFireHP / module.maxHP / module.maxHP;
			if (ship.getCaptain() != null) {
				fireRollVs = fireRollVs * (100 + ship.getCaptain().type.flammabilityPercent) / 100.0;
			}
			fireRollVs = fireRollVs * (100 + ship.flammabilityPercentFromMedals) / 100.0 * flammabilityFromShot;
			//System.out.println(module.type.name + " fireRollVs " + fireRollVs);
			if (c.r.nextDouble() <= fireRollVs) {
				//System.out.println("flame");
				module.fire = StrictMath.max(module.fire, Module.INITIAL_FIRE);
				cheer = true;
			}
		}
		if (cheer && optionalSourceShot != null && optionalSourceShot.weapon != null) {
			Crewman op = optionalSourceShot.weapon.findOperator();
			if (op != null) {
				op.shout("directHit");
			}
		}
		double harmPotential = MIN_CREW_HARM_P + dmg * (type == 1 ? BLAST_TO_HARM : PEN_TO_HARM);
		for (int boarders = 0; boarders < 2; boarders++) {
			int csz = (boarders == 1 ? ship.boarders : ship.crew).size();
			for (int ci = 0; ci < csz; ci++) {
				Crewman cm = (boarders == 1 ? ship.boarders : ship.crew).get(ci);
				if (cm.currentTile == this) {
					int harm = (int) StrictMath.floor((inside ? 1 : c.r.nextDouble()) * harmPotential);
					if (module.hp <= 0) {
						harm = cm.type.maxHP * 2;
					}
					cm.hurt(optionalSourceShot, harm, c, onViewingSide);
				}
			}
		}
	}
	
	public void hit(Shot shot, Combat c, boolean onViewingSide, boolean[] crewHitRef) {
		int pen = shot.getPenSplashRadius() == 0 ? shot.getPenDmg() : 0;
		int blast = shot.getBlastSplashRadius() == 0 ? shot.getBlastDmg() : 0;
		int direct = shot.getDirectSplashRadius() == 0 ? shot.getDirectDmg() : 0;
		int total = pen + blast + direct;
		double impactForce = shot.getImpactForce();
		if (impactForce != 0 && ship.type.mobile) {
			double a = Direction.radiansFromTo(shot.sX, shot.sY, shot.tX, shot.tY);
			ship.setxForce(ship.getxForce() + StrictMath.cos(a) * impactForce);
			ship.setyForce(ship.getyForce() + StrictMath.sin(a) * impactForce);
		}
		if (shot.weapon != null) {
			c.incModuleStat(shot.weapon.ship, shot.weapon.type, "hits");
			c.changeModuleStat(shot.weapon.ship, shot.weapon.type, "dmg", total);
			shot.weapon.ship.incStat("hits");
			shot.weapon.ship.changeStat("dmg", total);
			c.incStat(shot.weapon.ship, "hits");
			c.changeStat(shot.weapon.ship, "dmg", total);
		}
		if (shot.shooterType != null) {
			Combat.Side mySide = c.sideOf(ship);
			if (mySide != null) {
				c.otherSide(mySide).incStat("shipHitsByCrew " + shot.shooterType.name);
				c.otherSide(mySide).changeStat("shipDamageByCrew " + shot.shooterType.name, total);
			}
		}
		ship.changeStat("damageTaken", total);
		c.changeStat(ship, "damageTaken", total);
		if (shot.tentacle != null && (shot.tentacle.spec.moduleType.name.equals("MECHANICAL_TENTACLE") || shot.tentacle.spec.moduleType.name.equals("FLIPPED_MECHANICAL_TENTACLE") || shot.tentacle.spec.moduleType.name.equals("MECH_SQUID"))) {
			ship.mechTentacleDamageTaken += total;
		} else if (shot.weaponType != null && (shot.weaponType.name.equals("SAWBLADE") || shot.weaponType.name.equals("FLIPPED_SAWBLADE"))) {
			ship.sawDamageTaken += total;
		} else if (shot.shooterType != null && shot.shooterType.name.equals("air_hussar")) {
			ship.hussarDamageTaken += total;
		} else {
			ship.otherDamageTaken += total;
		}
		int prevArmourHP = armour.hp;
		double myX = ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + AGame.SGS / 2;
		double myY = ship.getIntY() + y * AGame.SGS + AGame.SGS / 2;
		if (!shot.internal) {
			if (armour.hp > 0) {
				boolean plinked = direct == 0;
				int penAbsorb = armour.type.getPenDmgAbsorb(ship.currentBonuses);
				if (module.hp > 0) {
					penAbsorb += module.getShellPenAbsorb();
				}
				if (pen <= penAbsorb) {
					pen = c.r.nextInt(2 + penAbsorb - pen) == 1 ? 1 : 0;
				} else {
					plinked = false;
					pen -= penAbsorb;
				}
				if (armour.hp > pen) {
					armour.hp -= pen;
					pen = 0;
				} else {
					pen -= armour.hp;
					armour.hp = 0;
				}
				
				int blastAbsorb = armour.type.getBlastDmgAbsorb(ship.currentBonuses);
				if (module.hp > 0) {
					blastAbsorb += module.getShellBlastAbsorb();
				}
				if (blast <= blastAbsorb) {
					blast = c.r.nextInt(2 + blastAbsorb - blast) == 1 ? 1 : 0;
				} else {
					plinked = false;
					blast -= blastAbsorb;
				}
				
				if (plinked && shot.weaponType != null && shot.weaponType.getTetherSpec(Bonus.standardSet) == null && !c.plinkedArmoursAndModules.contains(armour.type)) {
					//System.out.println("pl " + plinked + " pen " + pB + " bl " + bB + " dirDmg " + direct + " penDmg " + shot.getPenDmg() + " pdAbs " + armour.type.getPenDmgAbsorb(ship.constructionBonuses));
					c.plinkedArmoursAndModules.add(armour.type);
					c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("plinked " + armour.type.name + " " + shot.weaponType.name, c.otherSide(c.sideOf(ship)), shot.getX(), shot.getY(), null, null, shot.weaponType));
				}
				
				if (armour.hp > blast) {
					armour.hp -= blast;
					blast = 0;
				} else {
					blast -= armour.hp;
					armour.hp = 0;
				}
				
				if (armour.hp > direct) {
					armour.hp -= direct;
					direct = 0;
				} else {
					direct -= armour.hp;
					armour.hp = 0;
				}
								
				armour.hp = StrictMath.max(0, armour.hp);
				if (armour.hp == 0) {
					c.play(armour.type.brokenSound.get(ship.currentBonuses), myX, myY, 0, 0, !onViewingSide);
				} else if (total > armour.getMaxHP() / 2) {
					c.play(armour.type.largeHitSound.get(ship.currentBonuses), myX, myY, 0, 0, !onViewingSide);
				} else {
					c.play(armour.type.smallHitSound.get(ship.currentBonuses), myX, myY, 0, 0, !onViewingSide);
				}
			} else {
				if (module.hp > 0) {
					boolean plinked = direct == 0;
					int penAbsorb = module.getShellPenAbsorb();
					if (pen <= penAbsorb) {
						pen = c.r.nextInt(2 + penAbsorb - pen) == 1 ? 1 : 0;
					} else {
						pen -= penAbsorb;
						plinked = false;
					}
					int blastAbsorb = module.getShellBlastAbsorb();
					if (blast <= blastAbsorb) {
						blast = c.r.nextInt(2 + blastAbsorb - blast) == 1 ? 1 : 0;
					} else {
						blast -= blastAbsorb;
						plinked = false;
					}
					
					if (plinked && shot.weaponType != null && shot.weaponType.getTetherSpec(Bonus.standardSet) == null && !c.plinkedArmoursAndModules.contains(module.type)) {
						c.plinkedArmoursAndModules.add(module.type);
						c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("plinked " + module.type.name + " " + shot.weaponType.name, c.otherSide(c.sideOf(ship)), shot.getX(), shot.getY(), null, null, shot.weaponType));
					}
				}
				total = pen + blast + direct;
				if (total < 15) {
					c.play(MiscCombatSound.SMALL_HIT, myX, myY, 0, 0, !onViewingSide);
				} else if (total < 40) {
					c.play(MiscCombatSound.MEDIUM_HIT, myX, myY, 0, 0, !onViewingSide);
				} else {
					c.play(MiscCombatSound.LARGE_HIT, myX, myY, 0, 0, !onViewingSide);
				}
			}
			if (armour.hp < armour.getMaxHP() * 0.4) {
				Decal dest = checkDecalDestruction();
				if (dest != null) {
					for (int dx = dest.x; dx < dest.x + dest.type.w; dx++) {
						for (int dy = dest.y; dy < dest.y + dest.type.h; dy++) {
							for (int i = 0; i < 2; i++) {
								c.particles.add(new Particle(ParticleType.ofName("fragment"),
									ship.getIntX() + ship.gridXToWorldX(dx, 1) * AGame.SGS + AGame.SGS / 2,
									ship.getIntY() + dy * AGame.SGS + AGame.SGS / 2));
							}
						}
					}
				}
			}

			for (ModuleType.FragmentImg fragI : armour.getFragments(prevArmourHP, armour.hp)) {
				double speed = AGame.rnd(0.1, 0.25, 0.2, 0.4, 0.8) * 0.3;
				double angle = AGame.ANIM_R.nextDouble() * 2 * StrictMath.PI;
				double dx = speed * StrictMath.cos(angle);
				double dy = speed * StrictMath.sin(angle) - 0.3 * speed;
				c.fragments.add(new Fragment(
						fragI.ssb,
						fragI.img,
						ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + fragI.dx,
						ship.getIntY() + y * AGame.SGS + fragI.dy,
						dx, dy,
						0, AGame.ANIM_R.nextDouble() * 0.02 - 0.01,
						300 + AGame.ANIM_R.nextInt(400),
						/* burnAmt */4
				));
			}

			ParticleType hitParticle = module.type.hitParticle(ship.currentBonuses);
			if (hitParticle != null) {
				int nParticles = StrictMath.min(10, (pen + blast) / 10) + 1;
				for (int i = 0; i < nParticles; i++) {
					c.particles.add(new Particle(hitParticle, shot.tX, shot.tY));
				}
			}
		
			if (pen + blast + direct == 0) { return; }
			if (shot.weapon != null) {
				c.changeModuleStat(shot.weapon.ship, shot.weapon.type, "moduleDmg", pen + blast + direct);
				shot.weapon.ship.changeStat("moduleDmg", pen + blast + direct);
				c.changeStat(ship, "moduleDmg", pen + blast + direct);
			}
			if (shot.shooterType != null) {
				Combat.Side mySide = c.sideOf(ship);
				if (mySide != null) {
					c.otherSide(mySide).changeStat("shipModuleDamageByCrew " + shot.shooterType.name, pen + blast + direct);
				}
			}
			ship.changeStat("moduleDamageTaken", pen + blast + direct);
			c.changeStat(ship, "moduleDamageTaken", pen + blast + direct);
			int prevModuleHP = module.hp;
			module.doDamage(pen + blast + direct);
			boolean cheer = module.hp <= 0 && prevModuleHP > 0;

			double flammabilityFromShot = shot.weapon == null ? 1 : shot.weapon.type.getSetFireMultiplier(shot.weaponBonuses) * EmpireStat.SHOT_FLAMMABILITY.get(shot.weapon.ship.currentBonuses);
			if (shot.shooterType != null) {
				flammabilityFromShot = shot.shooterType.setFireMultiplier;
			}
			//System.out.println("ffs " + flammabilityFromShot);
			int adjFireHP = module.type.getFireHP(ship.currentBonuses) * module.maxHP / module.type.getHp(ship.currentBonuses);
			//System.out.println(module.hp + " vs " + adjFireHP * flammabilityFromShot);
			if (module.hp > 0 && module.hp < adjFireHP * flammabilityFromShot) {
				// So it should be this:
				//double fireRollVs = (pen + blast + direct) * 1.0 / module.maxHP;
				// But we wanna make flammable stuff more flammable yet.
				// The higher fireRollVs, the more likely is fire. So for higher adjFireHP values, it should be bigger.
				// So:
				double fireRollVs = (pen + blast + direct) * 1.5 * adjFireHP / module.maxHP / module.maxHP;
				if (ship.getCaptain() != null) {
					fireRollVs = fireRollVs * (100 + ship.getCaptain().type.flammabilityPercent) / 100.0;
				}
				fireRollVs = fireRollVs * (100 + ship.flammabilityPercentFromMedals) / 100.0 * flammabilityFromShot;
				//System.out.println(module.type.name + " fireRollVs " + fireRollVs);
				//System.out.println("fireRollVs " + fireRollVs);
				if (c.r.nextDouble() <= fireRollVs) {
					//System.out.println("flame");
					module.fire = StrictMath.max(module.fire, Module.INITIAL_FIRE);
					cheer = true;
				}
			}
			if (cheer && shot.weapon != null) {
				Crewman op = shot.weapon.findOperator();
				if (op != null) {
					op.shout("directHit");
				}
			}
			// Insta-destroy module if hit.
			if (module.hp <= -module.type.getHp(ship.currentBonuses) / 2) {
				module.hp = module.type.getHp(ship.currentBonuses) * Module.BREAK_APART_HP - 1;
			}
		}
		int sniper = 0;
		if (shot.weaponType != null) {
			sniper = shot.weaponType.getSniperChancePercent(shot.weaponBonuses);
		}
		if (shot.shooterType != null) {
			sniper = shot.shooterType.sniperChancePercent;
		}
		if (sniper > 0 && armour.hp <= 0 && c.r.nextInt(100) < sniper) {
			for (int ci = 0; ci < ship.crew.size(); ci++) {
				Crewman cm = ship.crew.get(ci);
				if (cm.currentTile == this && cm.alive()) {
					int harm = shot.getPenDmg() + shot.getBlastDmg() + shot.getDirectDmg();
					boolean wasAlive = cm.alive();
					cm.hurt(shot, harm, c, onViewingSide);
					if (crewHitRef != null) {
						crewHitRef[0] = true;
						if (wasAlive && !cm.alive() && !cm.type.isMachine) {
							crewHitRef[1] = true;
						}
					}
					if (shot.weapon != null) {
						Crewman op = shot.weapon.findOperator();
						if (op != null) {
							op.shout("sniperHit");
						}
					}
					return;
				}
			}
		} else {
			double harmPotential = MIN_CREW_HARM_P + pen * PEN_TO_HARM + blast * BLAST_TO_HARM + direct * DIRECT_TO_HARM;
			for (int boarders = 0; boarders < 2; boarders++) {
				if ((boarders == 0 && !shot.harmsCrew()) || (boarders == 1 && !shot.harmsBoarders())) {
					continue;
				}
				int csz = (boarders == 1 ? ship.boarders : ship.crew).size();
				for (int ci = 0; ci < csz; ci++) {
					Crewman cm = (boarders == 1 ? ship.boarders : ship.crew).get(ci);
					if (cm.currentTile == this && cm.alive()) {
						int harm = (int) StrictMath.floor(c.r.nextDouble() * harmPotential);
						boolean wasAlive = cm.alive();
						cm.hurt(shot, harm, c, onViewingSide);
						if (crewHitRef != null) {
							crewHitRef[0] = true;
							if (wasAlive && !cm.alive() && !cm.type.isMachine) {
								crewHitRef[1] = true;
							}
						}
						// Only hurt one person with internal shots.
						if (harm > 0 && shot.internal) { return; }
					}
				}
			}
		}
	}

	public int getMoveDelay() {
		return module.type.getMoveDelay(ship.currentBonuses);
	}
	
	public void tick(Combat c, int ms, Airship ship) {
		if (module.fire > 0 && !isMaskedEmpty()) {
			double fireParticlesSpawn = module.fire * ms * AGame.ANIM_R.nextDouble() * 0.003;
			double smokeParticlesSpawn = module.fire * ms * AGame.ANIM_R.nextDouble() * 0.0001;
			int fireParticlesSpawned = StrictMath.min(1, (int) StrictMath.floor(fireParticlesSpawn) + (AGame.ANIM_R.nextDouble() < fireParticlesSpawn % 1 ? 1 : 0));
			int smokeParticlesSpawned = StrictMath.min(1, (int) StrictMath.floor(smokeParticlesSpawn) + (AGame.ANIM_R.nextDouble() < smokeParticlesSpawn % 1 ? 1 : 0));
			for (int i = 0; i < fireParticlesSpawned; i++) {
				c.particles.add(new Particle(ParticleType.ofName("fire"),
						ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + AGame.SGS / 2,
						ship.getIntY() + y * AGame.SGS + AGame.SGS / 2));
			}
			for (int i = 0; i < smokeParticlesSpawned; i++) {
				c.particles.add(new Particle(ParticleType.ofName("fire_smoke"),
						ship.getIntX() + ship.gridXToWorldX(x, 1) * AGame.SGS + AGame.SGS / 2,
						ship.getIntY() + y * AGame.SGS + AGame.SGS / 2));
			}
		}
	}
	
	public Tile(JSONObject o, Airship ship, HashMap<Integer, Module> moduleMapping) {
		x = o.getInt("x");
		y = o.getInt("y");
		armour = new ArmourPlate(o.getJSONObject("armour"), ship);
		if (moduleMapping != null) {
			module = moduleMapping.get(o.getInt("module"));
		} else {
			module = ship.modules.get(o.getInt("module"));
		}
		for (Pair<Integer, Integer> occ : module.type.getCanOccupy()) {
			if (occ.a == x - module.x && occ.b == y - module.y) {
				canOccupy = true;
				break;
			}
		}
		this.ship = ship;
		String adjString = o.optString("hAdj", "000000000");
		for (int i = 0; i < 9; i++) {
			hadAdjacentOccupableTile[i / 3][i % 3] = adjString.charAt(i) != '0';
		}
		if (isMaskedEmpty()) {
			armour.setType(ArmourType.ofName("NONE"));
		}
		if (module.type.getArmourType() != null && armour.type != module.type.getArmourType()) {
			armour.setType(module.type.getArmourType());
		}
	}

	@Override
	public JSONObject toJSON() {
		StringBuilder hAdjString = new StringBuilder();
		for (int hay = 0; hay < 3; hay++) { for (int hax = 0; hax < 3; hax++) {
			hAdjString.append(hadAdjacentOccupableTile[hay][hax] ? "1" : "0");
		}}
		return new JSONObject()
				.put("x", x).put("y", y)
				.put("armour", armour.toJSON())
				.put("module", module.ship.modules.indexOf(module))
				.put("hAdj", hAdjString.toString());
	}

	@Override
	public GridBody body() {
		return ship;
	}

	@Override
	public int worldGridX() {
		return ship.gridXToWorldX(x, 1);
	}

	@Override
	public int worldGridY() {
		return y;
	}

	@Override
	public boolean solid() {
		return ship.tileAt(x, y) != null;
	}
	
	public boolean full() {
		return module.type.getTileMasks(ship.currentBonuses)[y - module.y][x - module.x] == TileMask.FULL;
	}
	
	public int solidCenterX() {
		if (ship.flipped) {
			return AGame.SGS - module.type.getTileMasks(ship.currentBonuses)[y - module.y][x - module.x].solidCenterX;
		} else {
			return module.type.getTileMasks(ship.currentBonuses)[y - module.y][x - module.x].solidCenterX;
		}
	}
	
	public int solidCenterY() {
		return module.type.getTileMasks(ship.currentBonuses)[y - module.y][x - module.x].solidCenterY;
	}

	@Override
	public double worldX() {
		return ship.getX() + ship.gridXToWorldX(x, 1) * AGame.SGS;
	}

	@Override
	public double worldY() {
		return ship.getY() + y * AGame.SGS;
	}
	
	@Override
	public String toString() {
		return x + " " + y + " @ " + ship.getName();
	}
}
