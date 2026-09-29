package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public abstract class  Body extends PhysicsRect {
	public transient double newX, oldX;
	private double xSpeed;
	public transient double postCollideXSpeed;
	private transient double xForce;
	public transient double groupXSpeed;
	public transient double newY, oldY;
	private double ySpeed;
	public transient double postCollideYSpeed;
	private transient double yForce;
	public transient double groupYSpeed;
	public transient double lastExertedXForce, lastExertedYForce, exertedXForce, exertedYForce;
	public transient ArrayList<Body> colliderGroup;
	public transient HashSet<Body> colliderGroupSet;
	
	public transient ArrayList<Particle> stuckParticles = new ArrayList<Particle>();
	public void particlesTick(int ms, Combat c) {
		double dripSpeed = c.timeOfDay.effect.particleDripSpeed;
		for (Iterator<Particle> it = stuckParticles.iterator(); it.hasNext();) {
			Particle p = it.next();
			p.life -= ms;
			if (p.life <= 0) {
				it.remove();
				return;
			}
			if (!p.type.dissolveWhenStuck) {
				p.y += dripSpeed * ms;
				if (p.y > getBBHeight()) {
					p.dx = 0;
					p.dy = 0.02;
					p.x += getX();
					p.y += getY();
					p.life /= 64;
					p.lifespan = p.type.maxLifespan;
					c.particles.add(p);
					it.remove();
				}
			}
		}
	}
	
	public abstract boolean canParticleStick(double x, double y);
	public void removeUnstuckParticles() {
		for (Iterator<Particle> it = stuckParticles.iterator(); it.hasNext();) {
			Particle p = it.next();
			if (!canParticleStick(getX() + p.x, getY() + p.y)) {
				it.remove();
			}
		} 
	}
	
	public abstract int getCollisionMass();
	public abstract int getMass();
	public abstract boolean isImmobile();
	public abstract boolean removeMe(Combat c);
	public abstract double elasticity();
	public abstract double horizontalAirFriction(boolean positiveX);
	public abstract double verticalAirFriction(boolean positiveY);
	
	public abstract boolean collidesWith(PhysicsRect b2);
	public abstract void doCollision(Body b2, double hitEnergy, Combat combat, boolean atSpeed);
	
	public abstract boolean isAtSpeed();

	public final double getxSpeed() {
		return xSpeed;
	}

	public final void setxSpeed(double xSpeed) {
		if (Double.isNaN(xSpeed)) { throw new IllegalArgumentException("xSpeed is NaN"); }
		if (Double.isInfinite(xSpeed)) { throw new IllegalArgumentException("xSpeed is infinite"); }
		this.xSpeed = xSpeed;
	}

	public final double getySpeed() {
		return ySpeed;
	}

	public final void setySpeed(double ySpeed) {
		if (Double.isNaN(ySpeed)) { throw new IllegalArgumentException("ySpeed is NaN"); }
		if (Double.isInfinite(ySpeed)) { throw new IllegalArgumentException("ySpeed is infinite"); }
		this.ySpeed = ySpeed;
	}

	public double getxForce() {
		return xForce;
	}

	public void setxForce(double xForce) {
		if (Double.isNaN(xForce)) { throw new IllegalArgumentException("xForce is NaN"); }
		if (Double.isInfinite(xForce)) { throw new IllegalArgumentException("xForce is infinite"); }
		this.xForce = xForce;
	}

	public double getyForce() {
		return yForce;
	}

	public void setyForce(double yForce) {
		if (Double.isNaN(yForce)) { throw new IllegalArgumentException("yForce is NaN"); }
		if (Double.isInfinite(yForce)) { throw new IllegalArgumentException("yForce is infinite"); }
		this.yForce = yForce;
	}
}
