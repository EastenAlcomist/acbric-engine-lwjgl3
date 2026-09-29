package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;

import java.util.ArrayList;

public class Leg {
	public static class Spec {
		public final boolean back;
		public final double xOffset, yOffset;
		public final double upperLimbLength;
		public final double middleLimbLength;
		public final double lowerLimbLength;
		public final double footWidth;
		public final double footHeight;
		public final double footOffset;
		public final int stepLength;
		public final int maxStepTime;
		public final boolean bendForwards;
		public final Spring spring;
		public final Img upperLeg;
		public final Img middleLeg;
		public final Img lowerLeg;
		public final Img foot;
		public final SoundEffect beginStepSound;
		public final SoundEffect footDownSound;
		public final double minFootY;
		public ArrayList<ModuleType.FragmentImg> upperLegFrag = new ArrayList<ModuleType.FragmentImg>();
		public ArrayList<ModuleType.FragmentImg> middleLegFrag = new ArrayList<ModuleType.FragmentImg>();
		public ArrayList<ModuleType.FragmentImg> lowerLegFrag = new ArrayList<ModuleType.FragmentImg>();
		public ArrayList<ModuleType.FragmentImg> footFrag = new ArrayList<ModuleType.FragmentImg>();

		public Spec(boolean back, double xOffset, double yOffset, double upperLimbLength, double middleLimbLength, double lowerLimbLength, double footWidth, double footHeight, int stepLength, int maxStepTime, boolean bendForwards, Spring spring, Img upperLeg, Img middleLeg, Img lowerLeg, Img foot, SoundEffect beginStepSound, SoundEffect footDownSound, double minFootY, double footOffset) {
			this.back = back;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.upperLimbLength = upperLimbLength;
			this.middleLimbLength = middleLimbLength;
			this.lowerLimbLength = lowerLimbLength;
			this.footWidth = footWidth;
			this.footHeight = footHeight;
			this.stepLength = stepLength;
			this.maxStepTime = maxStepTime;
			this.bendForwards = bendForwards;
			this.spring = spring;
			this.upperLeg = upperLeg;
			this.middleLeg = middleLeg;
			this.lowerLeg = lowerLeg;
			this.foot = foot;
			this.beginStepSound = beginStepSound;
			this.footDownSound = footDownSound;
			this.minFootY = minFootY;
			this.footOffset = footOffset;
		}
	}
	
	public Module module;
	public final Spec spec;
	public double upperRotation;
	public double lowerRotation;
	public Foot foot;
	public double targetFootX = 0, targetFootY = -100000000;
	public double sourceFootX, sourceFootY;
	public double footLerpAmt = 0;
	public double prevStepShipX = -10000000;
	public transient boolean isDown;
	public transient boolean inWater;

	public Leg(Spec spec, Module module) {
		this.spec = spec;
		this.module = module;
		this.foot = new Foot(this, 0, 0, spec.footWidth, spec.footHeight);
		upperRotation = StrictMath.PI / 2 + (spec.bendForwards ? -StrictMath.PI / 4 : StrictMath.PI / 4);
		lowerRotation = StrictMath.PI / 2 - (spec.bendForwards ? -StrictMath.PI / 3 : StrictMath.PI / 3);
	}
	
	public double hipX() {
		double mx = module.ship.getX() + module.ship.gridXToWorldX(module.x, module.type.getW()) * AGame.SGS;
		return module.ship.flipped
				? mx + (module.type.getW() - spec.xOffset) * AGame.SGS
				: mx + spec.xOffset * AGame.SGS;
	}
	
	public double hipY() {
		double my = module.ship.getY() + module.y * AGame.SGS + module.type.getH() * AGame.SGS;
		return my + spec.yOffset * AGame.SGS;
	}
	
	public double footX() {
		return hipX() +
				StrictMath.cos(upperRotation) * spec.upperLimbLength +
				(module.ship.flipped ^ spec.bendForwards ? 1 : -1) * spec.middleLimbLength +
				StrictMath.cos(lowerRotation) * spec.lowerLimbLength;
	}
	
	public double footY() {
		return hipY() + StrictMath.sin(upperRotation) * spec.upperLimbLength + StrictMath.sin(lowerRotation) * spec.lowerLimbLength + spec.footHeight;
	}
	
	public double getGroundY(LandFormation ground, ArrayList<LandFormation> lfs, double x) {
		double y = -Combat.COMBAT_AREA_H;
		if (ground != null) {
			y = StrictMath.max(y, ground.yBoundaryAt(x));
		}
		if (lfs != null) {
			for (LandFormation lf : lfs) {
				y = StrictMath.max(y, lf.yBoundaryAt(x));
			}
		}
		return y;
	}
	
	public boolean legRotation(double footX, double footY, boolean set) {
		footX -= hipX() + (module.ship.flipped ^ spec.bendForwards ? 1 : -1) * spec.middleLimbLength;
		footY -= spec.footHeight + hipY();
		if (footY < spec.minFootY) { return false; }
		double d = StrictMath.sqrt(footX * footX + footY * footY);
		double baseAngle = Direction.radiansFromTo(0, 0, footX, footY);
		double ur = Direction.normalizeRadians(baseAngle + (spec.bendForwards ^ module.ship.flipped ? -1 : 1) * StrictMath.acos((d*d + spec.upperLimbLength*spec.upperLimbLength - spec.lowerLimbLength*spec.lowerLimbLength) / (2 * d * spec.upperLimbLength)));
		double lr = Direction.normalizeRadians(ur - StrictMath.PI + (spec.bendForwards ^ module.ship.flipped ? -1 : 1) * StrictMath.acos((spec.upperLimbLength*spec.upperLimbLength + spec.lowerLimbLength*spec.lowerLimbLength - d*d) / (2 * spec.upperLimbLength * spec.lowerLimbLength)));
		if (!Double.isNaN(ur) && !Double.isNaN(lr)) {
			if (set) {
				upperRotation = ur;
				lowerRotation = lr;
			}
			foot.setX(footX() - spec.footWidth / 2);
			foot.setY(footY() - spec.footHeight);
			return true;
		}
		foot.setX(footX() - spec.footWidth / 2);
		foot.setY(footY() - spec.footHeight);
		return false;
	}
	
	public boolean lerpFoot(int ms, Combat c, boolean onViewingSide) {
		double prevFLA = footLerpAmt;
		double totalLerpMs = StrictMath.max(100, StrictMath.min(((StrictMath.abs(sourceFootX - targetFootX)) / (StrictMath.abs(module.ship.getxSpeed()) + 0.001)) / module.legs.size(), spec.maxStepTime));
		double lerpTime = ms / totalLerpMs;
		footLerpAmt = StrictMath.min(1, footLerpAmt + lerpTime);
		double prevFootX = footX(), prevFootY = footY();
		double footX, footY;
		double apexY = StrictMath.min(sourceFootY, targetFootY) - spec.upperLimbLength / 12 - spec.lowerLimbLength / 12;
		if (footLerpAmt < 0.3) {
			footX = sourceFootX;
			footY = sourceFootY * (1 - footLerpAmt / 0.3) + apexY * footLerpAmt / 0.3;
		} else if (footLerpAmt < 0.8) {
			footX = sourceFootX * (1 - (footLerpAmt - 0.3) / 0.5) + targetFootX * (footLerpAmt - 0.3) / 0.5;
			footY = apexY;
		} else {
			footX = targetFootX;
			footY = apexY * (1 - (footLerpAmt - 0.8) / 0.2) + targetFootY * (footLerpAmt - 0.8) / 0.2;
		}
		boolean legRotationSuccess;
		if (!(legRotationSuccess = legRotation(footX, footY, true))) {
			footY = sourceFootY * (1 - footLerpAmt) + targetFootY * footLerpAmt;
			if (!(legRotationSuccess = legRotation(footX, footY, true))) {
				footY = StrictMath.min(sourceFootY, targetFootY);
				legRotationSuccess = legRotation(footX, footY, true);
			}
		}
		foot.setxSpeed((footX - prevFootX) / ms / 20);
		foot.setySpeed((footY - prevFootY) / ms / 20 + (footLerpAmt == 1 ? AGame.G * ms : 0));
		foot.isDown = footLerpAmt >= 0.8 && legRotationSuccess;
		foot.hasStomped = footLerpAmt == 1 && legRotationSuccess && prevFLA != footLerpAmt ? 3 : 0;
		if (foot.hasStomped == 3 && c != null && spec.footDownSound != null && !(module.type.isSpider() && SimplePref.ARACHNOPHOBIA_MODE.get())) {
			c.play(spec.footDownSound, footX, footY, 0, 0, onViewingSide);
		}
		return footLerpAmt == 1;
	}
	
	public boolean findStepTarget(LandFormation ground, ArrayList<LandFormation> lfs) {
		if (module.ship.getxSpeed() > 0) {
			for (double newX = StrictMath.max(hipX() + (module.ship.flipped ^ spec.bendForwards ? 1 : -1) * (spec.middleLimbLength + spec.footOffset), targetFootX) + spec.stepLength * 2; newX > hipX() + (module.ship.flipped ^ spec.bendForwards ? 1 : -1) * (spec.middleLimbLength + spec.footOffset); newX -= 4) {
				if (spec.footOffset > 0) {
					if (module.ship.flipped ^ spec.bendForwards && newX < hipX() + spec.footOffset) {
						continue;
					}
					if (!(module.ship.flipped ^ spec.bendForwards) && newX > hipX() - spec.footOffset) {
						continue;
					}
				} else {
					// Do not step further than the final resting place of your hip.
					if (module.ship.moveTo != null && newX > hipX() - module.ship.getX() + module.ship.moveTo.x) {
						continue;
					}
				}
				double newY = getGroundY(ground, lfs, newX);
				if (module.legs.size() == 2) {
					Leg other = module.legs.get((module.legs.indexOf(this) + 1) % 2);
					if (StrictMath.abs(other.footX() - newX) > spec.stepLength * 2 && StrictMath.abs(other.footX() - newX) <= spec.stepLength * 2) {
						continue;
					}
					if (other.footX() < other.hipX() - spec.stepLength * 2 && newX > hipX() - spec.stepLength) {
						continue;
					}
					if (other.footX() < other.hipX() - spec.stepLength * 2 && footX() > hipX() - spec.stepLength * 1.9) {
						return false;
					}
				}
				if (legRotation(newX, newY, false)) {
					sourceFootX = footX();
					sourceFootY = footY();
					targetFootX = newX;
					//System.out.println("> tfx vs hip " + Math.abs(targetFootX - hipX()));
					targetFootY = newY;
					footLerpAmt = 0;
					prevStepShipX = module.ship.getX();
					return true;
				}
			}
		} else if (module.ship.getxSpeed() < 0) {
			for (double newX = StrictMath.min(hipX() + (module.ship.flipped ^ spec.bendForwards ? 1 : -1) * (spec.middleLimbLength + spec.footOffset), targetFootX) - spec.stepLength * 2; newX < hipX() + (module.ship.flipped ^ spec.bendForwards ? 1 : -1) * (spec.middleLimbLength + spec.footOffset); newX += 4) {
				if (spec.footOffset > 0) {
					if (module.ship.flipped ^ spec.bendForwards && newX < hipX() + spec.footOffset) {
						continue;
					}
					if (!(module.ship.flipped ^ spec.bendForwards) && newX > hipX() - spec.footOffset) {
						continue;
					}
				} else {
					// Do not step further than the final resting place of your hip.
					if (module.ship.moveTo != null && newX < hipX() - module.ship.getX() + module.ship.moveTo.x) {
						continue;
					}
				}
				double newY = getGroundY(ground, lfs, newX);
				if (module.legs.size() == 2) {
					Leg other = module.legs.get((module.legs.indexOf(this) + 1) % 2);
					if (StrictMath.abs(other.footX() - newX) > spec.stepLength * 2 && StrictMath.abs(other.footX() - newX) <= spec.stepLength * 2) {
						continue;
					}
					if (other.footX() > other.hipX() + spec.stepLength * 2 && newX < hipX() + spec.stepLength) {
						continue;
					}
					if (other.footX() > other.hipX() + spec.stepLength * 2 && footX() < hipX() + spec.stepLength * 1.9) {
						return false;
					}
				}
				if (legRotation(newX, newY, false)) {
					sourceFootX = footX();
					sourceFootY = footY();
					targetFootX = newX;
					//System.out.println("< tfx vs hip " + Math.abs(targetFootX - hipX()));
					targetFootY = newY;
					footLerpAmt = 0;
					prevStepShipX = module.ship.getX();
					return true;
				}
			}
		}
		return false;
	}
	
	public void findFootTarget(LandFormation ground, ArrayList<LandFormation> lfs) {
		if (module.legs.size() > 2) {
			// We want to be in the middle of the comfortable range.
			ArrayList<Double> availableXs = new ArrayList<Double>();
			double searchStart = (spec.bendForwards ^ module.ship.flipped) ? (hipX() + spec.middleLimbLength + spec.footOffset) : (hipX() - spec.upperLimbLength - spec.lowerLimbLength - spec.middleLimbLength);
			double searchEnd = (spec.bendForwards ^ module.ship.flipped) ? (hipX() + spec.upperLimbLength + spec.lowerLimbLength + spec.middleLimbLength) : (hipX() - spec.middleLimbLength - spec.footOffset);
			for (double newX = searchStart; newX < searchEnd; newX += 4) {
				double newY = getGroundY(ground, lfs, newX);
				if (legRotation(newX, newY, false)) {
					availableXs.add(newX);
				}
			}
			if (!availableXs.isEmpty()) {
				sourceFootX = footX();
				sourceFootY = footY();
				targetFootX = availableXs.get(availableXs.size() / 2);
				//System.out.println("fft tfx vs hip " + Math.abs(targetFootX - hipX()));
				targetFootY = getGroundY(ground, lfs, targetFootX);
				footLerpAmt = 0;
			}
		} else {
			// We want to be as close underneath the hip as possible.
			if (spec.bendForwards ^ module.ship.flipped) {
				for (double newX = hipX() + spec.middleLimbLength; newX < hipX() + spec.upperLimbLength + spec.lowerLimbLength + spec.middleLimbLength; newX += 4) {
					double newY = getGroundY(ground, lfs, newX);
					if (legRotation(newX, newY, false)) {
						sourceFootX = footX();
						sourceFootY = footY();
						targetFootX = newX;
						targetFootY = newY;
						footLerpAmt = 0;
						break;
					}
				}
			} else {
				for (double newX = hipX() - spec.middleLimbLength; newX > hipX() - spec.upperLimbLength - spec.lowerLimbLength - spec.middleLimbLength; newX -= 4) {
					double newY = getGroundY(ground, lfs, newX);
					if (legRotation(newX, newY, false)) {
						sourceFootX = footX();
						sourceFootY = footY();
						targetFootX = newX;
						targetFootY = newY;
						footLerpAmt = 0;
						break;
					}
				}
			}
		}
	}
	
	public void reset(LandFormation ground, ArrayList<LandFormation> lfs) {
		findFootTarget(ground, lfs);
		sourceFootX = targetFootX;
		sourceFootY = targetFootY;
		footLerpAmt = 1;
		legRotation(targetFootX, targetFootY, true);
	}
	
	// returns true when the step is completed
	public boolean tick(int ms, LandFormation ground, ArrayList<LandFormation> lfs, boolean doStep, Combat c, boolean powered, boolean onViewingSide) {
		boolean completed = tick2(ms, ground, lfs, doStep, c, powered, onViewingSide);
		if (lfs != null && !lfs.isEmpty() && lfs.get(0).landscapeType.hasWater) {
			boolean nowInWater = foot.getY() + foot.getBBHeight() > AGame.GROUND_LEVEL + 1;
			if (nowInWater && !inWater) {
				double volume = Math.min(4, 0.4 + Math.sqrt(module.ship.getWeight()) * 0.025);
				c.play(MiscCombatSound.SMALL_SPLASH, foot.getX() + foot.getBBWidth() / 2, foot.getY() + foot.getBBHeight(), 0, 0, volume, true);
				ParticleType water = ParticleType.ofName("waterSplash");
				for (int i = 0; i < foot.getBBWidth() / 3 + 1; i++) {
					c.particles.add(new Particle(water, foot.getX() + AGame.ANIM_R.nextDouble() * foot.getBBWidth(), AGame.GROUND_LEVEL + AGame.ANIM_R.nextDouble() * AGame.SGS / 2));
				}
			}
			inWater = nowInWater;
		}
		return completed;
	}
	
	private boolean tick2(int ms, LandFormation ground, ArrayList<LandFormation> lfs, boolean doStep, Combat c, boolean powered, boolean onViewingSide) {
		double yForTargetFootX = getGroundY(ground, lfs, targetFootX);
		if (doStep && footLerpAmt == 1 && StrictMath.abs(prevStepShipX - module.ship.getX()) > 8) {
			if (findStepTarget(ground, lfs) && c != null && spec.beginStepSound != null && !(module.type.isSpider() && SimplePref.ARACHNOPHOBIA_MODE.get())) {
				c.play(spec.beginStepSound, hipX(), hipY(), 0, 0, onViewingSide);
			}
		} else if (StrictMath.abs(yForTargetFootX - targetFootY) > 1 || !legRotation(targetFootX, targetFootY, false)) {
			findFootTarget(ground, lfs);
		}
		if (isDown = lerpFoot(ms, c, onViewingSide)) {
			double springDist = getGroundY(ground, lfs, footX()) - hipY();
				// OK, so we modify this to account for the fact that if the foot is not underneath the hip, it can't reach as far.
			double hDist = StrictMath.max(0, StrictMath.abs(footX() - hipX()) - spec.middleLimbLength * 1.5 - spec.footOffset);//StrictMath.min(StrictMath.abs(footY() - hipY()), StrictMath.min(spec.stepLength * 2, StrictMath.abs(footX() - hipX())));
			double totalDist = StrictMath.sqrt(springDist * springDist + hDist * hDist);
			if (totalDist <= spec.spring.baseLength) {
				/*System.out.println("sd " + springDist);
				System.out.println("sf " + spec.spring.getForce(springDist));
				System.out.println("lbf " + module.ship.legBalanceFactor);*/
				double springForce = StrictMath.max(0, spec.spring.getForce(springDist) * module.ship.legBalanceFactor);
				if (module.ship.legBalanceFactor < 0.65) {
					module.ship.reportFootingLoss = true;
				}
				if (!powered) {
					springForce *= 0.25;
				}
				module.ship.setyForce(module.ship.getyForce() - springForce);
				
				double fMult = module.ship.hasHadSpringFriction ? 0.1 : 1;
				module.ship.hasHadSpringFriction = true;
				double xFriction = StrictMath.pow(1 - spec.spring.xFriction * fMult, ms);
				module.ship.setxSpeed(module.ship.getxSpeed() * xFriction);
				double yFriction = StrictMath.pow(1 - spec.spring.yFriction * fMult, ms);
				module.ship.setySpeed(module.ship.getySpeed() * yFriction);
			}
			return doStep;
		}
		return false;
	}
}
