package com.zarkonnen.airships;

import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.util.Clr;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import com.zarkonnen.catengine.lwjgl3.GLCompat;

public strictfp class Particle {
	public ParticleType type;
	public double x, y, dx, dy;
	public double direction;
	public int life, lifespan;
	public final ParticlePictureType.Pic pic;
	public double startSize, endSize;
	public double scale = 1.0;
	
	public Particle(ParticleType type, double x, double y) {
		this(type, x, y, 1.0);
	}
	
	public Particle(ParticleType type, double x, double y, double scale) {
		this.type = type;
		this.scale = scale;
		direction = AGame.ANIM_R.nextDouble() * StrictMath.PI * 2;
		double dirDist = AGame.ANIM_R.nextDouble();
		this.x = x + StrictMath.cos(direction) * type.offset * dirDist * scale;
		this.y = y + StrictMath.sin(direction) * type.offset * dirDist * scale;
		if (type.directional) {
			double speed = (AGame.ANIM_R.nextDouble() * 0.4 + 0.8) * (type.maxDx);
			dx = StrictMath.cos(direction) * speed;
			dy = StrictMath.sin(direction) * speed;
		} else {
			dx = type.minDx + AGame.ANIM_R.nextDouble() * (type.maxDx - type.minDx);
			dy = type.minDy + AGame.ANIM_R.nextDouble() * (type.maxDy - type.minDy);
		}
		lifespan = type.minLifespan;
		if (type.maxLifespan > type.minLifespan) {
			lifespan += AGame.ANIM_R.nextInt(type.maxLifespan - type.minLifespan);
		}
		life = lifespan;
		pic = type.pt == null ? null : type.pt.pictures.get(AGame.ANIM_R.nextInt(type.pt.pictures.size()));
		startSize = (type.startSize * 1.0 * (0.8 + AGame.ANIM_R.nextDouble() * 0.5)) * scale;
		endSize = type.endSize * scale;
	}
	
	public Particle(ParticleType type, double x, double y, double dx, double dy) {
		this(type, x, y, dx, dy, 1.0);
	}
	
	public Particle(ParticleType type, double x, double y, double dx, double dy, double scale) {
		direction = Direction.radiansFromTo(0, 0, dx, dy);
		this.type = type;
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		this.scale = scale;
		lifespan = type.minLifespan;
		if (type.maxLifespan > type.minLifespan) {
			lifespan += AGame.ANIM_R.nextInt(type.maxLifespan - type.minLifespan);
		}
		life = lifespan;
		pic = type.pt == null ? null : type.pt.pictures.get(AGame.ANIM_R.nextInt(type.pt.pictures.size()));
		startSize = type.startSize * 1.0 * (0.8 + AGame.ANIM_R.nextDouble() * 0.5) * scale;
		endSize = type.endSize * scale;
	}
	
	public boolean tick(int ms, double wind, Combat c) {
		if (y < -300 && life < 1500 && type.accumulates) {
			dy *= 1.0 + (y + 300) * 0.0001;
			dx *= 0.9995 + StrictMath.abs(dy) * 0.01;
			if (endSize < 80) {
				endSize *= 1.0006;
			}
			/*if (variant % 4 == 1) {
				life -= AGame.ANIM_R.nextInt(9) == 1 ? StrictMath.max(1, ms / 16) : 0;
			} else {*/
				life -= ms;
			//}
		} else {
			dy += (type.grav * ms) / 16;
			life -= ms;
		}
		dx += wind * (type.windMult * ms) / 16;
		y += (dy * ms) / 16;
		x += (dx * ms) / 16;
		if (life > 0 && type.stickSpeed > 0 && (dx*dx) + (dy*dy) < type.stickSpeed * type.stickSpeed) {
			//double sz = startSize + 1.0 * (type.endSize - startSize) * (lifespan - life) / lifespan;
			int bsz = c.physics.bodies.size();
			for (int bi = 0; bi < bsz; bi++) {
				Body b = c.physics.bodies.get(bi);
				if (b == c.landFormations.get(0)) { continue; }
				//if (Rect2D.intersects(b.x, b.y, b.getBBWidth(), b.getBBHeight(), x, y, sz, sz)) {
				if (b.canParticleStick(x, y)) {
					b.stuckParticles.add(this);
					x -= b.getX();
					y -= b.getY();
					lifespan = type.maxLifespan * 64;
					life *= 64;
					return true;
				}
			}
		}
		return life <= 0;
	}
	
	public void drawAsPicture(Draw d, double xShift, double yShift) {
		double sz = startSize + 1.0 * (endSize - startSize) * (lifespan - life) / lifespan;
		int index = Math.min((type.gradient.length - 1) * (lifespan - life) / lifespan, type.gradient.length);
		float[] c = type.gradient[index];
		GLCompat.glColor4f(c[0], c[1], c[2], c[3]);
		double srcX = pic.x * 1.0 / pic.ssb.size;
		double srcY = pic.y * 1.0 / pic.ssb.size;
		double srcX2 = srcX + pic.w * 1.0 / pic.ssb.size;
		double srcY2 = srcY + pic.h * 1.0 / pic.ssb.size;
		if (type.directional) {
			double cos = StrictMath.cos(direction);
			double sin = StrictMath.sin(direction);
			double w2 = sz * 0.5;
			double h2 = sz * 0.5 * pic.h / pic.w;
			double x2 = x;
			double y2 = y;

			double topLeftX = x2 - w2 * cos + h2 * sin;
			double topLeftY = y2 - w2 * sin - h2 * cos;

			double bottomLeftX = x2 - w2 * cos - h2 * sin;
			double bottomLeftY = y2 - w2 * sin + h2 * cos;

			double bottomRightX = x2 + w2 * cos - h2 * sin;
			double bottomRightY = y2 + w2 * sin + h2 * cos;

			double topRightX = x2 + w2 * cos + h2 * sin;
			double topRightY = y2 + w2 * sin - h2 * cos;

			glTexCoord2d(srcX, srcY);
			glVertex2d(topLeftX, topLeftY);
			glTexCoord2d(srcX, srcY2);
			glVertex2d(bottomLeftX, bottomLeftY);
			glTexCoord2d(srcX2, srcY2);
			glVertex2d(bottomRightX, bottomRightY);
			glTexCoord2d(srcX2, srcY);
			glVertex2d(topRightX, topRightY);
		} else {
			double w2 = sz * 0.5;
			double h2 = sz * 0.5 * pic.h / pic.w;
			glTexCoord2d(srcX, srcY);
			glVertex2d(x + xShift - w2, y + yShift - h2);
			glTexCoord2d(srcX, srcY2);
			glVertex2d(x + xShift - w2, y + yShift + h2);
			glTexCoord2d(srcX2, srcY2);
			glVertex2d(x + xShift + w2, y + yShift + h2);
			glTexCoord2d(srcX2, srcY);
			glVertex2d(x + xShift + w2, y + yShift - h2);
		}
	}
	
	public void drawAsShadedPicture(Draw d) {
		double w = startSize + 1.0 * (endSize - startSize) * (lifespan - life) / lifespan;
		double h = w * pic.h / pic.w;
		Clr c = type.tintGradient[(type.tintGradient.length - 1) * (lifespan - life) / lifespan];
		GLCompat.glVertexAttrib4f(Appearance.lsp.getAttributeID("tint"), c.r / 255.0f, c.g / 255.0f, c.b / 255.0f, c.a / 255.0f);
		double srcX = pic.x;
		double srcY = pic.y;
		double srcX2 = pic.x + pic.w;
		double srcY2 = pic.y + pic.h;
		glTexCoord2d(srcX, srcY);
		glVertex2d(x - w / 2, y - h / 2);
		glTexCoord2d(srcX, srcY2);
		glVertex2d(x - w / 2, y + h / 2);
		glTexCoord2d(srcX2, srcY2);
		glVertex2d(x + w / 2, y + h / 2);
		glTexCoord2d(srcX2, srcY);
		glVertex2d(x + w / 2, y - h / 2);
	}
	
	public static strictfp class Emitter {
		public ParticleType t;
		public double emitProbability;
		public int numParticles;
		public SoundEffect soundEffect;
		
		public Emitter(ParticleType t, double emitProbability, int numParticles, SoundEffect soundEffect) {
			this.t = t;
			this.emitProbability = emitProbability;
			this.numParticles = numParticles;
			this.soundEffect = soundEffect;
		}
	}
}
