package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import static com.zarkonnen.airships.Spring.lengthTo;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Pt;

import java.util.ArrayList;

public strictfp class  Wheel {
	public static strictfp class  Spec {
		public final double xOffset;
		public final double maxYOffset;
		public final double radius;
		
		public final Pt leadStart, leadEndOffset;
		public final int appX, appY;
		public final Appearance app;
		
		public final boolean randomPhase;
		
		public final Img wheel;
		public final Img lowerLink;
		public final Img upperLink;
		public final double segmentStride;
		public ArrayList<ModuleType.FragmentImg> wheelFrag = new ArrayList<ModuleType.FragmentImg>();
		public ArrayList<ModuleType.FragmentImg> lowerLinkFrag = new ArrayList<ModuleType.FragmentImg>();
		public ArrayList<ModuleType.FragmentImg> upperLinkFrag = new ArrayList<ModuleType.FragmentImg>();

		public Spec(double xOffset, double maxYOffset, double radius, boolean randomPhase, Img wheel, Img lowerLink, Img upperLink, double segmentStride) {
			this.xOffset = xOffset;
			this.maxYOffset = maxYOffset;
			this.radius = radius;
			this.wheel = wheel;
			this.lowerLink = lowerLink;
			this.upperLink = upperLink;
			this.segmentStride = segmentStride;
			this.randomPhase = randomPhase;
			
			leadStart = null;
			leadEndOffset = null;
			app = null;
			appX = 0;
			appY = 0;
		}
		
		public Spec(double xOffset, double maxYOffset, double radius, boolean randomPhase, Appearance app, int appX, int appY, Pt leadStart, Pt leadEndOffset) {
			this.xOffset = xOffset;
			this.maxYOffset = maxYOffset;
			this.radius = radius;
			this.wheel = null;
			this.lowerLink = null;
			this.upperLink = null;
			this.segmentStride = 0;
			this.randomPhase = randomPhase;
			
			this.leadStart = leadStart;
			this.leadEndOffset = leadEndOffset;
			this.app = app;
			this.appX = appX;
			this.appY = appY;
		}
	}
	
	public final Spec spec;
	public double yOffset;
	public double phase;
	public boolean onGround;
	public Module m;
	public WheelBody body;
	public transient boolean inWater;

	public Wheel(Spec spec, Module m) {
		this.spec = spec;
		this.m = m;
		body = new WheelBody(this, 0, 0, spec.radius);
		if (spec.randomPhase) {
			phase = AGame.ANIM_R.nextDouble() * Math.PI * 2;
		}
	}
	
	public static final double SINCOS45 = 0.707;
	
	public static double lengthTo3Probe(LandFormation lf, double x, double y, double r) {
		return StrictMath.min(lengthTo(lf, x, y),
				StrictMath.min(
						lengthTo(lf, x + r * SINCOS45, y) + r * (1 - SINCOS45),
						lengthTo(lf, x - r * SINCOS45, y) + r * (1 - SINCOS45)));
	}
	
	public double lengthTo3Probe(Airship ship, double x, double y, double r) {
		return StrictMath.min(lengthTo(ship, x, y),
				StrictMath.min(
						lengthTo(ship, x + r * SINCOS45, y) + r * (1 - SINCOS45),
						lengthTo(ship, x - r * SINCOS45, y) + r * (1 - SINCOS45)));
	}
	
	public void setBodyPosition(double mx, double my) {
		body.setX(mx + spec.xOffset * AGame.SGS - spec.radius);
		body.setY(my + yOffset - spec.radius);
		body.setxSpeed(0);//myShip.xSpeed;
		body.setySpeed(m.ship.getySpeed());// < 0 ? StrictMath.max(myShip.ySpeed, -AGame.G * Combat.TICK_LENGTH * 1.5) : StrictMath.min(myShip.ySpeed, AGame.G * Combat.TICK_LENGTH * 1.5);
		body.setxForce(0);
		body.setyForce(0);
	}
	
	public void calcYOffset(double x, double y, UniScreen us) {
		double l = spec.maxYOffset + spec.radius + 0.000001;
		x += spec.xOffset * AGame.SGS;
		if (us.combat != null) {
			for (LandFormation lf : us.combat.landFormations) {
				l = StrictMath.min(l, lengthTo3Probe(lf, x, y, spec.radius));
			}
			for (Side s : us.combat.sides) {
				for (Airship ship : s.ships) {
					if (ship == m.ship) { continue; }
					l = StrictMath.min(l, lengthTo3Probe(ship, x, y, spec.radius));
				}
			}
		} else if (us.city != null) {
			l = StrictMath.min(l, lengthTo(us.city.ground, x, y));
			for (LandFormation lf : us.city.floaters) {
				l = StrictMath.min(l, lengthTo3Probe(lf, x, y, spec.radius));
			}
			Fleet garrison = us.wm.getGarrison(us.city);
			for (Airship ship : garrison.actives) {
				if (ship == m.ship) { continue; }
				l = StrictMath.min(l, lengthTo3Probe(ship, x, y, spec.radius));
			}
		} else if (us.setupGround != null) {
			l = StrictMath.min(l, lengthTo(us.setupGround, x, y));
			for (LandFormation lf : us.setupFloaters) {
				l = StrictMath.min(l, lengthTo3Probe(lf, x, y, spec.radius));
			}
			for (Airship ship : us.getSetupFleet()) {
				if (ship == m.ship) { continue; }
				l = StrictMath.min(l, lengthTo3Probe(ship, x, y, spec.radius));
			}
		}
		double newOffset = StrictMath.min(spec.maxYOffset, l - spec.radius);
		if (StrictMath.abs(yOffset - newOffset) <= spec.radius / 10) {
			yOffset = newOffset;
		} else {
			if (newOffset > yOffset) {
				yOffset += spec.radius / 10;
			} else {
				yOffset -= spec.radius / 10;
			}
		}
		onGround = yOffset < spec.maxYOffset;
	}
	
	public void calcYOffset(double x, double y, Combat c) {
		double l = spec.maxYOffset + spec.radius + 0.000001;
		x += spec.xOffset * AGame.SGS;
		double probeMin = 10000;
		for (LandFormation lf : c.landFormations) {
			probeMin = StrictMath.min(probeMin, lengthTo3Probe(lf, x, y, spec.radius));
		}
		for (int si = 0; si < c.sides.size(); si++) {
			Side s = c.sides.get(si);
			for (int i = 0; i < s.ships.size(); i++) {
				Airship ship = s.ships.get(i);
				if (ship == m.ship) { continue; }
				probeMin = StrictMath.min(probeMin, lengthTo3Probe(ship, x, y, spec.radius));
			}
		}
		l = StrictMath.min(l, probeMin);
		double newOffset = StrictMath.min(spec.maxYOffset, l - spec.radius);
		if (StrictMath.abs(yOffset - newOffset) <= spec.radius / 10) {
			yOffset = newOffset;
		} else {
			if (newOffset > yOffset) {
				yOffset += spec.radius / 10;
			} else {
				yOffset -= spec.radius / 10;
			}
		}
		onGround = probeMin < spec.maxYOffset + spec.radius + 1;
	}
}
