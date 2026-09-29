package com.zarkonnen.airships;

import static com.zarkonnen.airships.AnimationType.*;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;

import java.util.ArrayList;
import java.util.HashSet;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public strictfp class CrewAnimator {
	public Crewman cm;
	public int time;
	public int ticksSinceClimb;
	public int ticksFalling = 0;
	public AnimationType prevAnimation = STANDING;
	public int variant = -1;
	
	public AnimationType specialAnim;
	public int specialDuration;
	public Resource specialResource;
	
	public boolean lastFlipped;
	public transient int animOffset = AGame.ANIM_R.nextInt(20000);

	public CrewAnimator(Crewman cm) {
		this.cm = cm;
	}
	
	public void tick(int ms, Combat c, double x, double y, boolean flipped, boolean carried) {
		if (variant == -1) {
			variant = AGame.ANIM_R.nextInt(cm.type.animLooks.length);
		}
		time += ms;
		specialDuration -= ms;
		ticksSinceClimb += 1;
		
		if (c != null) {
			if (cm.alive()) {
				determineAnimation(r_anim, r_carrying, flipped, carried);
			}
			AnimationAppearance look = cm.type.animLooks[variant];
			
			if (look.bundle != null) {
				AnimationBundle.Animation anim = cm.type.animLooks[variant].bundle.animations.get(r_anim[0]);
				if (anim == null) {
					anim = cm.type.animLooks[variant].bundle.animations.get(STANDING);
				}

				if (anim == null && (r_anim[0] == FLY_DEAD_LEFT || r_anim[0] == FLY_INJURED_LEFT)) {
					anim = cm.type.animLooks[variant].bundle.animations.get(FLY_LEFT);
				}
				if (anim == null && (r_anim[0] == FLY_DEAD_RIGHT || r_anim[0] == FLY_INJURED_RIGHT)) {
					anim = cm.type.animLooks[variant].bundle.animations.get(FLY_RIGHT);
				}
				if (anim == null) {
					//System.out.println("No anim found in bundle " + animLook.bundle.name + " for " + type.name());
					anim = cm.type.animLooks[variant].bundle.animations.get(STANDING);
				}
				if (anim == null) {
					//System.err.println("Unable to find animation " + r_anim[0] + " for crewType " + cm.type.name + ".");
					return;
				}

				for (int bodyPartIndex = 0; bodyPartIndex < anim.parts.length; bodyPartIndex++) {
					ArrayList<AnimationAppearance.BodyPartEmitter> ems = (cm.alive() ? look.emitters : look.deadEmitters).get(anim.side)[bodyPartIndex];
					if (ems != null) {
						for (int emI = 0; emI < ems.size(); emI++) {
							AnimationAppearance.BodyPartEmitter em = ems.get(emI);
							if (AGame.ANIM_R.nextDouble() < em.freq * ms) {
								AnimationBundle.Animation.Part p = anim.parts[bodyPartIndex];
								if (p == null) { continue; }
								double px = x
										+ p.x
										+ StrictMath.cos(2 * StrictMath.PI * (time + p.rotationOffset) / p.rotationPeriod) * p.rotationWidth;
								double py = y
										+ p.y
										+ StrictMath.sin(2 * StrictMath.PI * (time + p.rotationOffset) / p.rotationPeriod) * p.rotationHeight;
								double angle = p.waveStartAngle + (p.waveEndAngle - p.waveStartAngle) * (0.5 - 0.5 * StrictMath.cos((time + p.waveOffset) * 2 * StrictMath.PI / p.wavePeriod));
								px += StrictMath.cos(angle) * em.x - StrictMath.sin(angle) * em.y;
								py += StrictMath.sin(angle) * em.x + StrictMath.cos(angle) * em.y;
								c.particles.add(new Particle(em.type, px, py, em.scale));
							}
						}
					}
				}
			} else if (look.frameAnimations != null) {
				AnimationType type = r_anim[0];
				CrewFrameAnimation anim = look.frameAnimations.get(r_anim[0]);
				if (anim == null && (type == FLY_DEAD_LEFT || type == FLY_INJURED_LEFT)) {
					anim = look.frameAnimations.get(FLY_LEFT);
				}
				if (anim == null && (type == FLY_DEAD_RIGHT || type == FLY_INJURED_RIGHT)) {
					anim = look.frameAnimations.get(FLY_RIGHT);
				}
				if (anim == null && type == CARRIED) {
					anim = look.frameAnimations.get(INJURED);
				}
				if (anim == null) {
					//System.out.println("No anim found in bundle " + animLook.bundle.name + " for " + type.name());
					anim = look.frameAnimations.get(STANDING);
				}
				if (anim == null) {
					System.err.println("Unable to find frame animation " + type + " for crewType " + cm.type.name + ".");
					return;
				}
				for (int i = 0; i < anim.emitters.size(); i++) {
					CrewFrameAnimation.Emitter em = anim.emitters.get(i);
					if (AGame.ANIM_R.nextDouble() < em.freq * ms) {
						c.particles.add(new Particle(em.type, x + em.x, y + em.y, em.scale));
					}
				}
			}
		}
	}
	
	public void animate(AnimationType anim, Resource res) {
		if (variant == -1) {
			variant = AGame.ANIM_R.nextInt(cm.type.animLooks.length);
		}
		if (cm.type.animLooks[variant].bundle == null) {
			if (cm.type.animLooks[variant].frameAnimations.containsKey(anim)) {
				time = 0;
				specialDuration = cm.type.animLooks[variant].frameAnimations.get(anim).getLengthOr0IfLooping();
				specialAnim = anim;
				specialResource = res;
			}
		} else {
			if (cm.type.animLooks[variant].bundle.animations.containsKey(anim)) {
				time = 0;
				specialDuration = cm.type.animLooks[variant].bundle.animations.get(anim).length;
				specialAnim = anim;
				specialResource = res;
			}
		}
	}
	
	public void continueAnimating(AnimationType anim, Resource res) {
		if (specialAnim != anim) {
			animate(anim, res);
		}
	}
	
	public void stopAnimating() {
		specialDuration = 0;
		specialAnim = null;
		specialResource = null;
	}
	
	private final AnimationType[] r_anim = new AnimationType[1];
	private final Resource[] r_carrying = new Resource[1];
	
	private void determineAnimation(AnimationType[] r_anim, Resource[] r_carrying, boolean flipped, boolean carried) {
		if (carried) {
			r_anim[0] = CARRIED;
			r_carrying[0] = null;
			prevAnimation = CARRIED;
			return;
		}
		AnimationType anim = STANDING;
		Resource carrying = cm.carrying;
		if (cm.type.underwaterGraceTime > 0 && cm.drowningProgress >= cm.type.underwaterGraceTime && cm.alive()) {
			anim = DROWNING;
			r_anim[0] = anim;
			r_carrying[0] = carrying;
			prevAnimation = anim;
			return;
		}
		if (cm.underwater && !cm.active()) {
			anim = DROWNED;
			r_anim[0] = anim;
			r_carrying[0] = carrying;
			prevAnimation = anim;
			return;
		}
		if (cm.type.canFly && !((cm.ship != null || cm.boardingShip != null) && cm.type.canWalk)) {
			if (cm.active()) {
				if ((cm.ship == null && cm.boardingShip == null) || !cm.type.canWalk) {
					if (cm.hp <= cm.type.maxHP / 2) {
						anim = cm.dx < 0 ? FLY_INJURED_LEFT : FLY_INJURED_RIGHT;
					} else {
						anim = cm.dx < 0 ? FLY_LEFT : FLY_RIGHT;
					}
				} else {
					if (cm.mvDx > 0.001) {
						anim = WALK_RIGHT;
					} else if (cm.mvDx < -0.001) {
						anim = WALK_LEFT;
					} else {
						anim = STANDING;
					}
				}
			} else {
				if (!cm.alive()) {
					if (cm.dx > 0.001) {
						anim = FLY_DEAD_RIGHT;
					} else {
						anim = FLY_DEAD_LEFT;
					}
				} else {
					if (cm.dx > 0.001) {
						anim = FLY_INJURED_RIGHT;
					} else {
						anim = FLY_INJURED_LEFT;
					}
				}
			}
			r_anim[0] = anim;
			r_carrying[0] = carrying;
			prevAnimation = anim;
			return;
		}
		if (cm.grabbed) {
			anim = FALL;
		} else if (specialDuration > 0) {
			anim = specialAnim;
			carrying = specialResource;
		} else if (!cm.active()) {
			if (!cm.alive()) {
				anim = DEAD;
			} else {
				anim = INJURED;
			}
		} else {
			if (cm.injuredCarried != null) {
				carrying = Resource.INJURED;
			}
			if (cm.ship == null && cm.boardingShip == null) {
				if (cm.attachedTo == null) {
					if (cm.dy > 0.4 || (cm.dy >= 0 && StrictMath.abs(cm.dx) < 0.01)) {
						ticksFalling++;
						if (ticksFalling > 5) {
							anim = FALL;
						} else {
							anim = prevAnimation;
						}
					} else {
						anim = cm.dx < 0 ? JUMP_LEFT : JUMP_RIGHT;
					}
				} else if (cm.standing) {
					ticksFalling = 0;
					if (cm.mvDx > 0.001) {
						anim = WALK_RIGHT;
					} else if (cm.mvDx < -0.001) {
						anim = WALK_LEFT;
					} else {
						anim = STANDING;
					}
				} else {
					ticksFalling = 0;
					if (cm.mvDx == 0 && cm.mvDy == 0) {
						anim = HANG_IN_THERE;
					} else if (StrictMath.abs(cm.mvDx) > StrictMath.abs(cm.mvDy)) {
						anim = CLIMB_SIDEWAYS;
					} else {
						anim = CLIMB;
					}
				}
			} else {
				boolean armed =
						cm.boardingShip != null ||
						cm.job instanceof Module.FixedGuardJob ||
						cm.job instanceof Module.GuardJob ||
						((cm.type.canBoard || cm.type.doesGuard) && !cm.ship.boarders.isEmpty());
				// Give, shoot, repair: override.
				// Climb/walk: active if movingTowards != null.
				if (cm.movingTowards != null) {
					if (cm.movingTowards.y != cm.currentTile.y) {
						anim = CLIMB;
						ticksSinceClimb = 0;
					} else {
						if (ticksSinceClimb < 2) {
							anim = CLIMB;
						} else {
							lastFlipped = !(cm.movingTowards.x > cm.currentTile.x != flipped);
							if (armed) {
								anim = lastFlipped ? WALK_LEFT_ARMED : WALK_RIGHT_ARMED;
							} else {
								anim = lastFlipped ? WALK_LEFT : WALK_RIGHT;
							}
						}
					}
				} else {
					// Stand with direction: if job is crew job, stand in direction of ship.
					if (cm.job != null && cm.job instanceof Module.StaffJob) {
						anim = flipped ? STANDING_LEFT : STANDING_RIGHT;
					} else {
						anim = armed ? STANDING_ARMED : STANDING;
					}
				}
			}
		}
		r_anim[0] = anim;
		r_carrying[0] = carrying;
		prevAnimation = anim;
	}
	
	public void draw(Draw d, double x, double y, boolean flipped, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, float[][] coaColors, boolean carried) {
		if (variant == -1) {
			variant = AGame.ANIM_R.nextInt(cm.type.animLooks.length);
		}
		determineAnimation(r_anim, r_carrying, flipped, carried);
		draw(d, x, y, cm.type, r_anim[0], cm.type.animLooks[variant], time, animOffset, r_carrying[0], light, lightStrength, ambient, ambientSaturation, ambientTint, ssb, additionalSSBs, coaColors);
	}
	
	public static void draw(Draw d, double x, double y, CrewType crewType, AnimationType type, AnimationAppearance animLook, int time, int animOffset, Resource carrying, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint, SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, float[][] coaColors)
	{
		boolean resDrawn = false;
		if (animLook.bundle == null) {
			CrewFrameAnimation anim = animLook.frameAnimations.get(type);
			if (anim == null && (type == FLY_DEAD_LEFT || type == FLY_INJURED_LEFT)) {
				anim = animLook.frameAnimations.get(FLY_LEFT);
			}
			if (anim == null && (type == FLY_DEAD_RIGHT || type == FLY_INJURED_RIGHT)) {
				anim = animLook.frameAnimations.get(FLY_RIGHT);
			}
			if (anim == null && type == CARRIED) {
				anim = animLook.frameAnimations.get(INJURED);
			}
			if (anim == null) {
				//System.out.println("No anim found in bundle " + animLook.bundle.name + " for " + type.name());
				anim = animLook.frameAnimations.get(STANDING);
			}
			if (anim == null) {
				System.err.println("Unable to find frame animation " + type + " for crewType " + crewType.name + ".");
				return;
			}
			float[] srcA = crewType == null ? CrewType.NO_COLOUR : crewType.recolorOriginalA;
			float[] srcB = crewType == null ? CrewType.NO_COLOUR : crewType.recolorOriginalB;
			float[] trgA = crewType == null ? srcA : coaColors[crewType.recolorReplacementA.armsColorIndex];
			float[] trgB = crewType == null ? srcB : coaColors[crewType.recolorReplacementB.armsColorIndex];
			
			int frameIndex = anim.loop
					? ((time + animOffset) % (anim.interval * anim.frames.length) / anim.interval)
					: StrictMath.min(anim.frames.length - 1, time / anim.interval);
			Img img = anim.frames[frameIndex];
			if (ssb != null && !img.src.equals(ssb.name)) {
				if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, img.src)) {
					additionalSSBs.add(SpritesheetBundle.ofName(img.src));
				}
				return;
			}
			double px = x + anim.dx;
			double py = y + anim.dy;
			double angle = 0;
			if (img.machineImgCache != null) {
				((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			//RotatingShader.draw(img, d, px, py, angle, false, light, lightStrength, ambient, ambientSaturation, ambientTint);

			RotatingColoringShader.draw(SpritesheetBundle.ofName(img.src), img, d, px, py, angle, 1.0, img.flipped, false, light, lightStrength, ambient, ambientSaturation, ambientTint,
					srcA, trgA, srcB, trgB);
		} else {
			AnimationBundle.Animation anim = animLook.bundle.animations.get(type);
			if (anim == null && (type == FLY_DEAD_LEFT || type == FLY_INJURED_LEFT)) {
				anim = animLook.bundle.animations.get(FLY_LEFT);
			}
			if (anim == null && (type == FLY_DEAD_RIGHT || type == FLY_INJURED_RIGHT)) {
				anim = animLook.bundle.animations.get(FLY_RIGHT);
			}
			if (anim == null && type == CARRIED) {
				anim = animLook.bundle.animations.get(INJURED);
			}
			if (anim == null) {
				//System.out.println("No anim found in bundle " + animLook.bundle.name + " for " + type.name());
				anim = animLook.bundle.animations.get(STANDING);
			}
			if (anim == null) {
				//System.err.println("Unable to find animation " + type + " for crewType " + crewType.name + ".");
				return;
			}
			float[] srcA = crewType == null ? CrewType.NO_COLOUR : crewType.recolorOriginalA;
			float[] srcB = crewType == null ? CrewType.NO_COLOUR : crewType.recolorOriginalB;
			float[] trgA = crewType == null ? srcA : coaColors[crewType.recolorReplacementA.armsColorIndex];
			float[] trgB = crewType == null ? srcB : coaColors[crewType.recolorReplacementB.armsColorIndex];
			for (int i = 0; i < anim.parts.length; i++) {
				AnimationBundle.Animation.Part p = anim.parts[i];
				if (p != null && animLook.images.get(anim.side)[i] != null) {
					if (anim.bodyPlan.isSpider && SimplePref.ARACHNOPHOBIA_MODE.get() && anim.bodyPlan.partNames.get(anim.side).get(i).contains("leg")) {
						continue;
					}
					Img img = animLook.images.get(anim.side)[i];
					double px = x
							+ p.x
							+ StrictMath.cos(2 * StrictMath.PI * (time + p.rotationOffset) / p.rotationPeriod) * p.rotationWidth;
					double py = y
							+ p.y
							+ StrictMath.sin(2 * StrictMath.PI * (time + p.rotationOffset) / p.rotationPeriod) * p.rotationHeight;
					double angle = p.waveStartAngle + (p.waveEndAngle - p.waveStartAngle) * (0.5 - 0.5 * StrictMath.cos((time + p.waveOffset) * 2 * StrictMath.PI / p.wavePeriod));
					
					if (ssb != null && !img.src.equals(ssb.name)) {
						if (additionalSSBs != null && Loadable.hasOfName(SpritesheetBundle.class, img.src)) {
							additionalSSBs.add(SpritesheetBundle.ofName(img.src));
						}
					} else {
						if (img.machineImgCache != null) {
							((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
						}
						RotatingColoringShader.draw(SpritesheetBundle.ofName(img.src), img, d, px, py, angle, 1.0, false, false, light, lightStrength, ambient, ambientSaturation, ambientTint,
								srcA, trgA, srcB, trgB);
					}
					if (carrying != null && p.holdsResource && carrying != Resource.INJURED) {
						if (ssb != null && carrying.carryApp.spritesheetBundle != ssb) {
							if (additionalSSBs != null) {
								additionalSSBs.add(carrying.carryApp.spritesheetBundle);
							}
						} else {
							double endX = px + img.srcWidth / 2 - StrictMath.sin(angle) * img.srcHeight / 2;
							double endY = py + img.srcHeight / 2 + StrictMath.cos(angle) * img.srcHeight / 2;
							RotatingColoringShader.draw(SpritesheetBundle.ofName(carrying.carryApp.frames.get(0).src), carrying.carryApp.frames.get(0), d, endX - 12, endY - 9, 0, 1, false, false, light, lightStrength, ambient, ambientSaturation, ambientTint, CrewType.NO_COLOUR, CrewType.NO_COLOUR, CrewType.NO_COLOUR, CrewType.NO_COLOUR);
						}
						resDrawn = true;
					}
				}
			}
		}
		if (!resDrawn && carrying != null && carrying != Resource.INJURED) {
			if (ssb != null && carrying.carryApp.spritesheetBundle != ssb) {
				if (additionalSSBs != null) {
					additionalSSBs.add(carrying.carryApp.spritesheetBundle);
				}
			} else {
				Img img = carrying.carryApp.frames.get(0);
				RotatingColoringShader.draw(SpritesheetBundle.ofName(img.src), img, d, x - 10, y - 5, 0, 1, false, false, light, lightStrength, ambient, ambientSaturation, ambientTint, CrewType.NO_COLOUR, CrewType.NO_COLOUR, CrewType.NO_COLOUR, CrewType.NO_COLOUR);
			}
		}
	}
}
