package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import org.json.JSONArray;
import org.json.JSONObject;


public strictfp class  Shot {	
	public static final int MIN_DMG = 1;
	
	public Airship target;
	public Crewman targetTroop;
	public double tX, tY;
	public double sX, sY;
	public int time;
	public int travelTime;
	
	public int delay;
	
	public boolean internal;
	public boolean vsBoarders;
	public CrewType shooterType;
	
	public ModuleType weaponType;
	public BonusSet weaponBonuses = BonusSet.empty();
	public Tentacle tentacle;
	public Module weapon;
	
	public boolean isArcing;
	public double arcingX;
	public double arcingY;
	public double arcingDx;
	public double arcingDy;
	public double arcingG;
	
	// Guided missile stuff
	public double dX, dY, a; // angle
	// sx/sy is used as the position
	// tx/ty is used as an offset to the actual target
	
	public transient boolean done;
	
	public final boolean isGuided() {
		return weaponType != null && weaponType.guidanceSystem(weaponBonuses) != null;
	}
	
	public boolean fuseTriggered() {
		if (!isGuided()) { return false; }
		GuidanceSystemInfo gsi = weaponType.guidanceSystem(weaponBonuses);
		if (target != null) {
			double dSq = (target.getX() + tX - sX) * (target.getX() + tX - sX) + (target.getY() + tY - sY) * (target.getY() + tY - sY);
			return dSq < gsi.proximityFuseDistance * gsi.proximityFuseDistance;
		}
		if (targetTroop != null) {
			double dSq = (targetTroop.getX() + tX - sX) * (targetTroop.getX() + tX - sX) + (targetTroop.getY() + tY - sY) * (targetTroop.getY() + tY - sY);
			return dSq < gsi.proximityFuseDistance * gsi.proximityFuseDistance;
		}
		return false;
	}
	
	public JSONObject toJSON(Combat c) {
		JSONObject o = new JSONObject()
				.put("tX", tX).put("tY", tY)
				.put("sX", sX).put("sY", sY)
				.put("dX", dX).put("dY", dY).put("a", a)
				.put("time", time)
				.put("travelTime", travelTime)
				.put("internal", internal)
				.put("vsBoarders", vsBoarders)
				;
		if (delay != 0) {
			o.put("delay", delay);
		}
		if (target != null) {
			Combat.Side targetSide = c.sideOf(target);
			if (targetSide != null) {
				o.put("targetSideIndex", c.sides.indexOf(targetSide));
				o.put("targetShipIndex", targetSide.ships.indexOf(target));
			}
		}
		if (targetTroop != null) {
			for (int si = 0; si < c.sides.size(); si++) {
				Combat.Side side = c.sides.get(si);
				int ti = side.troops.indexOf(targetTroop);
				if (ti != -1) {
					o.put("targetTroopSideIndex", si);
					o.put("targetTroopIndex", ti);
				}
			}
		}
		if (weapon != null) {
			Combat.Side weaponSide = c.sideOf(weapon.ship);
			if (weaponSide != null) {
				o.put("weaponSideIndex", c.sides.indexOf(weaponSide));
				o.put("weaponShipIndex", weaponSide.ships.indexOf(weapon.ship));
				o.put("weaponIndex", weapon.ship.modules.indexOf(weapon));
			}
		}
		if (weaponType != null) {
			o.put("weaponType", weaponType.name);
		}
		if (weaponBonuses != null) {
			JSONArray a = new JSONArray();
			o.put("weaponBonuses", a);
			for (Bonus b : weaponBonuses.list()) {
				a.put(b.name());
			}
		}
		if (shooterType != null) {
			o.put("shooterType", shooterType.name);
		}
		if (isArcing) {
			o.put("isArcing", true);
			o.put("arcingDx", arcingDx);
			o.put("arcingDy", arcingDy);
			o.put("arcingX", arcingX);
			o.put("arcingY", arcingY);
			o.put("arcingG", arcingG);
		}
		return o;
	}
	
	public Shot(JSONObject o, Combat c) {
		tX = o.getDouble("tX"); tY = o.getDouble("tY");
		sX = o.getDouble("sX"); sY = o.getDouble("sY");
		dX = o.optDouble("dX", 0); dY = o.optDouble("dY", 0); a = o.optDouble("a", 0); 
		time = o.getInt("time");
		travelTime = o.getInt("travelTime");
		delay = o.optInt("delay", 0);
		internal = o.getBoolean("internal");
		vsBoarders = o.getBoolean("vsBoarders");
		if (o.has("targetSideIndex")) {
			target = c.sides.get(o.getInt("targetSideIndex")).ships.get(o.getInt("targetShipIndex"));
		}
		if (o.has("targetTroopIndex")) {
			targetTroop = c.sides.get(o.getInt("targetTroopSideIndex")).troops.get(o.getInt("targetTroopIndex"));
		}
		if (o.has("weaponType")) {
			weaponType = ModuleType.ofName(o.getString("weaponType"));
		}
		if (o.has("weaponSideIndex")) {
			weapon = c.sides.get(o.getInt("weaponSideIndex")).ships.get(o.getInt("weaponShipIndex")).modules.get(o.getInt("weaponIndex"));
		}
		if (o.has("weaponBonuses")) {
			weaponBonuses = new BonusSet();
			JSONArray a = o.getJSONArray("weaponBonuses");
			for (int i = 0; i < a.length(); i++) {
				weaponBonuses.add(Bonus.ofName(a.getString(i)));
			}
		}
		if (o.has("shooterType")) {
			shooterType = CrewType.ofName(o.getString("shooterType"));
		}
		if (o.optBoolean("isArcing", false)) {
			isArcing = true;
			arcingDx = o.getDouble("arcingDx");
			arcingDy = o.getDouble("arcingDy");
			arcingX = o.getDouble("arcingX");
			arcingY = o.getDouble("arcingY");
			arcingG = o.getDouble("arcingG");
		}
	}
	
	public void makeArcing(double arcingDx, double arcingDy, int travelTime, double arcingG) {
		isArcing = true;
		this.arcingDx = arcingDx;
		this.arcingDy = arcingDy;
		this.arcingG = arcingG;
		arcingX = sX;
		arcingY = sY;
		this.travelTime = travelTime;
	}
	
	public Shot(Airship target, double tX, double tY, Airship source, Tentacle tentacle) {
		this.target = target;
		this.tX = tX;
		this.tY = tY;
		this.tentacle = tentacle;
		this.sX = tentacle.segments.get(0).centerX;
		this.sY = tentacle.segments.get(0).centerY;
		travelTime = 1;
	}

	public Shot(Airship target, double tX, double tY, Airship source, double sX, double sY, Module weapon, double shotSpeedMult) {
		this.target = target;
		this.tX = tX;
		this.tY = tY;
		this.weaponBonuses = source.currentBonuses;
		this.sX = sX;
		this.sY = sY;
		this.weapon = weapon;
		this.weaponType = weapon.type;
		double dist = StrictMath.sqrt((tX - sX) * (tX - sX) + (tY - sY) * (tY - sY));
		travelTime = (int) (dist / weapon.type.getShotSpeed(source.currentBonuses) / shotSpeedMult);
		if (isGuided()) {
			this.tX -= target.getX();
			this.tY -= target.getY();
			a = weapon.weaponAngle;
			dX = StrictMath.cos(a) * weaponType.guidanceSystem(weaponBonuses).launchSpeed + source.getxSpeed();
			dY = StrictMath.sin(a) * weaponType.guidanceSystem(weaponBonuses).launchSpeed + source.getySpeed();
		}
	}
	
	public Shot(Airship target, double tX, double tY, CrewType shooterType, double sX, double sY, boolean internal, boolean vsBoarders) {
		this.target = target;
		this.tX = tX;
		this.tY = tY;
		this.shooterType = shooterType;
		this.sX = sX;
		this.sY = sY;
		this.internal = internal;
		this.vsBoarders = vsBoarders;
		double dist = StrictMath.sqrt((tX - sX) * (tX - sX) + (tY - sY) * (tY - sY));
		travelTime = (int) (dist / shooterType.shotSpeed);
	}
	
	public Shot(Crewman targetTroop, double tX, double tY, CrewType shooterType, double sX, double sY) {
		this.targetTroop = targetTroop;
		this.tX = tX;
		this.tY = tY;
		this.shooterType = shooterType;
		this.sX = sX;
		this.sY = sY;
		double dist = StrictMath.sqrt((tX - sX) * (tX - sX) + (tY - sY) * (tY - sY));
		travelTime = (int) (dist / shooterType.shotSpeed);
	}
	
	public Shot(Crewman targetTroop, double tX, double tY, Airship source, double sX, double sY, Module weapon, double shotSpeedMult) {
		this.targetTroop = targetTroop;
		this.tX = tX;
		this.tY = tY;
		this.weaponBonuses = source.currentBonuses;
		this.sX = sX;
		this.sY = sY;
		this.weapon = weapon;
		this.weaponType = weapon.type;
		double dist = StrictMath.sqrt((tX - sX) * (tX - sX) + (tY - sY) * (tY - sY));
		travelTime = (int) (dist / weapon.type.getShotSpeed(source.currentBonuses) / shotSpeedMult);
		if (isGuided()) {
			this.tX -= target.getX();
			this.tY -= target.getY();
			a = weapon.weaponAngle;
			dX = StrictMath.cos(a) * weaponType.guidanceSystem(weaponBonuses).launchSpeed + source.getxSpeed();
			dY = StrictMath.sin(a) * weaponType.guidanceSystem(weaponBonuses).launchSpeed + source.getySpeed();
		}
	}
	
	public boolean tick(int ms, Combat c, boolean onViewingSide) {
		if (delay > 0) {
			delay -= ms;
			if (delay >= 0) {
				return false;
			} else {
				ms = -delay;
				delay = 0;
			}
		}
		if (target != null && c.sideOf(target) == null) {
			done = true;
			return true;
		}
		boolean lockLost = false;
		if (isGuided()) {
			GuidanceSystemInfo gsi = weaponType.guidanceSystem(weaponBonuses);
			PhysicsRect tr = target == null ? targetTroop : target;
			if (time >= gsi.launchLength) {
				double angleToTarget = StrictMath.atan2(tr.getY() + tY - sY, tr.getX() + tX - sX);
				double angleDifference = a - angleToTarget;
				angleDifference = Direction.normalizeRadians(angleDifference);
				// Guaranteed to be in the 0 to 2PI range now.
				if (angleDifference < Math.PI) {
					if (angleDifference < gsi.turnSpeed * ms) {
						a = angleToTarget;
					} else {
						a -= gsi.turnSpeed * ms;
					}
				} else {
					if (Math.PI * 2 - angleDifference < gsi.turnSpeed * ms) {
						a = angleToTarget;
					} else {
						a += gsi.turnSpeed * ms;
					}
				}
				lockLost = time >= gsi.missileLockTime && (angleDifference < Math.PI ? (angleDifference > gsi.missileLockLossAngle) : (angleDifference < Math.PI * 2 - gsi.missileLockLossAngle));
			}
			dX += StrictMath.cos(a) * gsi.acceleration * ms;
			dY += StrictMath.sin(a) * gsi.acceleration * ms;
			double speedSquared = dX * dX + dY * dY;
			if (speedSquared > gsi.topSpeed * gsi.topSpeed) {
				double speed = StrictMath.sqrt(speedSquared);
				dX *= gsi.topSpeed / speed;
				dY *= gsi.topSpeed / speed;
			}
			sX += dX * ms;
			sY += dY * ms;
		}
		Particle.Emitter em = null;
		WeaponAppearance wapp = weaponType == null ? null : weaponType.weaponAppearance(weaponBonuses);
		if (wapp != null) {
			em = wapp.shotEmitter;
		}
		if (shooterType != null) {
			em = shooterType.shotEmitter;
		}
		if (em != null) {
			if (AGame.ANIM_R.nextDouble() < em.emitProbability * ms) {
				double px = getX();
				double py = getY();
				
				double xSpeed = isGuided() ? dX : ((tX - sX) / travelTime);
				double ySpeed = isGuided() ? dY : ((tY - sY) / travelTime);
				
				for (int i = 0; i < em.numParticles; i++) {
					c.particles.add(new Particle(em.t, px, py));
				}
				if (em.soundEffect != null) {
					c.play(em.soundEffect, px, py, xSpeed, ySpeed, onViewingSide);
				}
			}
		}
		WeaponAppearance.ShotExhaustEmitter ex = null;
		if (wapp != null) {
			ex = wapp.exhaust;
		}
		if (shooterType != null) {
			ex = shooterType.exhaust;
		}
		if (ex != null) {
			ex.emit(this, ms, c.particles);
		}
		time += ms;
		if (isArcing) {
			arcingDy += arcingG * ms;
			arcingX += arcingDx * ms;
			arcingY += arcingDy * ms;
		}
		if (time >= travelTime || lockLost || fuseTriggered()) {
			/*if (isArcing) {
				System.out.println("hit, with deviation of " + (arcingX - tX) + " / " + (arcingY - tY));
			}*/
			if (isGuided()) {
				tX = sX;
				tY = sY;
			}
			boolean[] crewHitRef = new boolean[2]; // 0 is whether crew was hit, 1 is whether bio crew was killed
			Tile hitTile = target == null ? null : target.hit(this, c, !onViewingSide, crewHitRef);
			if (target != null) {
				int dmg = getBlastDmg() + getPenDmg() + getDirectDmg();
				if (hitTile != null) {
					target.damageTaken[0] += dmg;
					if (weapon != null) {
						weapon.damageDealt[0] += dmg;
					}
					if (sY < tY) {
						target.damageTakenFromAbove[0] += dmg;
					}
				} else {
					target.damageMissed[0] += dmg;
					if (weapon != null) {
						weapon.damageNotDealt[0] += dmg;
					}
				}
			}
			Combat.Side immuneCrewSide = null;
			if (target != null) {
				immuneCrewSide = c.otherSide(c.sideOf(target));
			}
			if (getBlastSplashRadius() > 0) {
				c.doSplashDmg(1, tX, tY, getBlastDmg(), getBlastSplashRadius(), this, null, immuneCrewSide, splashFriendlyFire() ? null : immuneCrewSide);
			}
			if (getPenSplashRadius() > 0) {
				c.doSplashDmg(0, tX, tY, getPenDmg(), getPenSplashRadius(), this, null, immuneCrewSide, splashFriendlyFire() ? null : immuneCrewSide);
			}
			if (getDirectSplashRadius()> 0) {
				c.doSplashDmg(2, tX, tY, getDirectDmg(), getDirectSplashRadius(), this, null, immuneCrewSide, splashFriendlyFire() ? null : immuneCrewSide);
			}
			Crewman hitTroop = targetTroop == null ? null : targetTroop.hit(this, c, !onViewingSide);
			if (hitTroop != null && hitTroop.type.canFly && !hitTroop.type.canWalk && shooterType != null && shooterType.canFly && !shooterType.canWalk && hitTroop.hp <= 0) {
				Combat.Side targetSide = c.sideOf(hitTroop);
				if (targetSide != null) {
					Combat.Side shooterSide = c.otherSide(targetSide);
					if (shooterSide != null) {
						shooterSide.aircraftDownedByAircraft++;
					}
				}
			}
			double explosionSize = 0;
			int nonExplodeParticles = 0;
			int dmg = getBlastDmg() + getPenDmg() + getDirectDmg();
			if ((hitTile != null || hitTroop != null) && getBlastDmg() > 0) {
				nonExplodeParticles = dmg / 3 + 3;
			}
			int explodeX = (int) tX;
			int explodeY = (int) tY;
			if (weaponType != null) {
				CrewType spawn = weaponType.spawnCrewOnImpact(weaponBonuses);
				explosionSize = wapp.missExplosionSize;
				if (hitTile != null) {
					c.msSinceInterestingCombatEvent = 0;
					if (weapon != null && c.sideOf(weapon.ship) != null) {
						Tile srcTile = weapon.ship.tileAt(weapon.x, weapon.y); // qqDPS Bad to use top left always.
						if (srcTile != null && weapon.type.getTetherSpec(weaponBonuses) != null) {
							double sAngle = isGuided() ? a : StrictMath.atan2((tY - sY), (tX - sX));
							Img[] imgs = wapp.shot;
							Img img = imgs == null ? null : imgs[(time / wapp.shotAnimationInterval) % imgs.length];
							weapon.tether = new Tether(weapon.type.getTetherSpec(weaponBonuses), srcTile, hitTile, img, sAngle);
						}
					}
					explosionSize = wapp.hitExplosionSize;
					if (spawn != null) {
						boolean inside = hitTile.canOccupy && ((weaponType.spawnCrewInsideIfArmourPierced(weaponBonuses) && hitTile.armour.hp <= 0) || weaponType.alwaysSpawnCrewInside(weaponBonuses));
						int num = weaponType.spawnNumCrewOnImpact(weaponBonuses);
						for (int ci = 0; ci < num; ci++) {
							Crewman spawned = new Crewman(target, hitTile, spawn);
							if (inside) {
								target.boarders.add(spawned);
								spawned.ship = null;
								spawned.boardingShip = target;
							} else {
								spawned.ship = null;
								spawned.attachedTo = spawn.canFly ? null : target;
								spawned.setX(tX);
								spawned.setY(tY);
								spawned.ultimateBoardTarget = target;
								c.otherSide(c.sideOf(target)).troops.add(spawned);
							}
						}
					}
				}
				if ((hitTile != null || hitTroop != null) && weaponType.getHitSound(weaponBonuses) != null) {
					double xSpeed = isGuided() ? dX : ((tX - sX) / travelTime);
					double ySpeed = isGuided() ? dY : ((tY - sY) / travelTime);
					c.play(weaponType.getHitSound(weaponBonuses), tX, tY, xSpeed, ySpeed, onViewingSide);
				}
				if (hitTile == null && weaponType.spawnCrewOnMiss(weaponBonuses)) {
					int num = weaponType.spawnNumCrewOnImpact(weaponBonuses);
					for (int ci = 0; ci < num; ci++) {
						Crewman spawned = new Crewman(null, null, spawn);
						spawned.setX(tX);
						spawned.setY(tY);
						c.otherSide(c.sideOf(target)).troops.add(spawned);
					}
				}
			} else if (shooterType != null) {
				explosionSize = hitTile == null ? shooterType.missExplosionSize : shooterType.hitExplosionSize;
				if (shooterType.spawnCrewOnImpact != null) {
					if (hitTroop != null && !(shooterType.spawnCrewOnKillBiologicalOnly && hitTroop.alive())) {
						//System.out.println(shooterType.name + " hitTroop " + hitTroop.type.name);
						// We hit an external troop.
						for (int ci = 0; ci < shooterType.spawnNumCrewOnImpact; ci++) {
							Crewman spawned = new Crewman(null, null, CrewType.ofName(shooterType.spawnCrewOnImpact));
							spawned.setX(tX);
							spawned.setY(tY);
							if (target != null) {
								c.otherSide(c.sideOf(target)).troops.add(spawned);
							} else if (targetTroop != null) {
								c.otherSide(c.sideOf(targetTroop)).troops.add(spawned);
							}
						}
					} else if (crewHitRef[0] && internal && !(shooterType.spawnCrewOnKillBiologicalOnly && !crewHitRef[1])) {
						//System.out.println(shooterType.name + " hitCrew");
						// We hit another crew from the inside.
						for (int ci = 0; ci < shooterType.spawnNumCrewOnImpact; ci++) {
							Crewman spawned = new Crewman(hitTile.ship, hitTile, CrewType.ofName(shooterType.spawnCrewOnImpact));
							if (vsBoarders) {
								hitTile.ship.crew.add(spawned);
							} else {
								spawned.ship = null;
								spawned.boardingShip = hitTile.ship;
								hitTile.ship.boarders.add(spawned);
							}
						}
					} else if (hitTile != null && !internal) {
						//System.out.println(shooterType.name + " hitTile");
						// We hit a tile from the outside.
						boolean inside = hitTile.canOccupy && ((shooterType.spawnCrewInsideIfArmourPierced && hitTile.armour.hp <= 0) || shooterType.alwaysSpawnCrewInside);
						for (int ci = 0; ci < shooterType.spawnNumCrewOnImpact; ci++) {
							Crewman spawned = new Crewman(target, hitTile, CrewType.ofName(shooterType.spawnCrewOnImpact));
							if (inside) {
								spawned.boardingShip = target;
								spawned.ship = null;
								target.boarders.add(spawned);
							} else {
								spawned.ship = null;
								spawned.attachedTo = CrewType.ofName(shooterType.spawnCrewOnImpact).canFly ? null : target;
								spawned.setX(tX);
								spawned.setY(tY);
								spawned.ultimateBoardTarget = target;
								c.otherSide(c.sideOf(target)).troops.add(spawned);
							}
						}
					} else if (shooterType.spawnCrewOnMiss && !internal) {
						//System.out.println(shooterType.name + " hitNothing");
						// We hit nothing.
						for (int ci = 0; ci < shooterType.spawnNumCrewOnImpact; ci++) {
							Crewman spawned = new Crewman(null, null, CrewType.ofName(shooterType.spawnCrewOnImpact));
							spawned.setX(tX);
							spawned.setY(tY);
							if (target != null) {
								c.otherSide(c.sideOf(target)).troops.add(spawned);
							} else if (targetTroop != null) {
								c.otherSide(c.sideOf(targetTroop)).troops.add(spawned);
							}
						}
					}
				}
			}
			if (explosionSize != 0) {
				int n = (int) (12 * explosionSize);

				c.blasts.add(new Blast(explodeX, explodeY, explosionSize * 15, explosionSize * 10));

				int parts = 8;
				for (int i = 0; i < parts; i++) {
					c.particles.add(new Particle(ParticleType.ofName("explode_backs"),
							explodeX, explodeY, explosionSize));
				}
				parts = explosionSize > 0.5 ? 1 : 0;
				for (int i = 0; i < parts; i++) {
					c.particles.add(new Particle(ParticleType.ofName("shockwave"),
							explodeX, explodeY, explosionSize));
				}
				parts = n;
				for (int i = 0; i < parts; i++) {
					c.particles.add(new Particle(ParticleType.ofName("small_soot"),
							explodeX, explodeY));
					c.particles.add(new Particle(ParticleType.ofName("large_soot"),
							explodeX, explodeY));
				}
				if (!SimplePref.REDUCED_FLASHING.get()) {
					parts = 1;
					for (int i = 0; i < parts; i++) {
						c.particles.add(new Particle(ParticleType.ofName("explode"),
								explodeX,
								explodeY,
								explosionSize));
					}
				}
				parts = 12;
				for (int i = 0; i < parts; i++) {
					c.particles.add(new Particle(ParticleType.ofName("explode_bits"),
							explodeX, explodeY, explosionSize));
				}
				if (explosionSize <= 0.5) {
					c.play(MiscCombatSound.SMALL_SHOT_EXPLOSION, explodeX, explodeY, 0, 0, onViewingSide);
				} else if (explosionSize <= 1.2) {
					c.play(MiscCombatSound.SHOT_EXPLOSION, explodeX, explodeY, 0, 0, onViewingSide);
				} else {
					c.play(MiscCombatSound.MODULE_EXPLOSION, explodeX, explodeY, 0, 0, onViewingSide);
				}
			}
			if (hitTile != null || hitTroop != null) {
				ParticleType pt;
				if (wapp != null) {
					pt = wapp.impactParticle;
					if (wapp.numImpactParticles != 0) {
						nonExplodeParticles = wapp.numImpactParticles;
					}
				} else {
					pt = ParticleType.ofName("impact");
				}
				for (int i = 0; i < nonExplodeParticles; i++) {
					c.particles.add(new Particle(pt, explodeX, explodeY));
				}
			}
			
			done = true;
			return true;
		}
		if (c.landFormations.get(0).landscapeType.hasWater && getY() > AGame.GROUND_LEVEL) {
			if (c.landFormations.get(0).yBoundaryAt(getX()) > AGame.GROUND_LEVEL) {
				int dmg = getPenDmg() + getBlastDmg() + getDirectDmg();
				double volume = Math.min(2, dmg / 40.0);
				if (volume > 0.1) {
					c.play(MiscCombatSound.SMALL_SPLASH, getX(), getY(), 0, 0, volume, true);
				}
				ParticleType water = ParticleType.ofName("waterSplash");
				for (int i = 0; i < Math.min(20, dmg / 5 + 1); i++) {
					c.particles.add(new Particle(water, getX(), AGame.GROUND_LEVEL + AGame.ANIM_R.nextDouble() * AGame.SGS / 2));
				}
			}
			return true;
		}
		return false;
	}
	
	public double getX() {
		if (isArcing) {
			return arcingX;
		}
		if (isGuided()) {
			return sX;
		}
		return sX + ((tX - sX) * time / travelTime);
	}
	
	public double getY() {
		if (isArcing) {
			return arcingY;
		}
		if (isGuided()) {
			return sY;
		}
		return sY + ((tY - sY) * time / travelTime);
	}
	
	public double getAngle() {
		if (isArcing) {
			return Math.atan2(arcingDy, arcingDx);
		} else if (isGuided()){
			return a;
		} else {
			return Math.atan2((tY - sY), (tX - sX));
		}
	}

	public int getPenDmg() {
		return
				weaponType == null
				? tentacle == null
					? shooterType == null
						? 0
						: shooterType.penDmg
					: tentacle.spec.attackPenDmg
				: weaponType.getPenDmg(weaponBonuses);
	}

	public int getBlastDmg() {
		return
				weaponType == null
				? tentacle == null
					? shooterType == null
						? 0
						: shooterType.blastDmg
					: tentacle.spec.attackBlastDmg
				: weaponType.getBlastDmg(weaponBonuses);
	}
	
	public int getBlastSplashRadius() {
		return weaponType != null
				? weaponType.getBlastSplashRadius(weaponBonuses)
				: shooterType != null
				? shooterType.blastSplashRadius
				: 0;
	}
	
	public int getPenSplashRadius() {
		return weaponType != null
				? weaponType.getPenSplashRadius(weaponBonuses)
				: shooterType != null
				? shooterType.penSplashRadius
				: 0;
	}
	
	public int getDirectSplashRadius() {
		return weaponType != null
				? weaponType.getDirectSplashRadius(weaponBonuses)
				: shooterType != null
				? shooterType.directSplashRadius
				: 0;
	}
	
	public boolean splashFriendlyFire() {
		return weaponType == null || weaponType.splashFriendlyFire(weaponBonuses);
	}
	
	public double getImpactForce() {
		return weaponType == null ? 0 : weaponType.getImpactForce(weaponBonuses);
	}
	
	public int getDirectDmg() {
		return
				weaponType == null
				? tentacle == null
					? shooterType == null
						? 0
						: shooterType.directDmg
					: tentacle.spec.attackDirectDmg
				: weaponType.getDirectDmg(weaponBonuses);
	}

	public boolean harmsCrew() {
		return !internal || !vsBoarders;
	}

	public boolean harmsBoarders() {
		return !internal || vsBoarders;
	}
}
