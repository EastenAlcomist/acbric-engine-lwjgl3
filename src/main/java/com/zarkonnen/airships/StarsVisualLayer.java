package com.zarkonnen.airships;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import org.newdawn.slick.opengl.TextureImpl;

public /*strictfp*/ class StarsVisualLayer implements UniScreen.VisualLayer {
	ArrayList<WeatherVisualLayer.Drop> stars = new ArrayList<WeatherVisualLayer.Drop>();
	
	public static final Clr RAIN_C = new Clr(191, 191, 191, 128);
	public static final Clr DUST_C = new Clr(51, 26, 8, 128);
	public static final Clr STAR_C = new Clr(255, 255, 255, 128);
	public static final Clr STAR_C_BRIGHT = new Clr(255, 255, 255, 192);
	
	private int ticksDone = 0;
			
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (ticksDone++ < 3) { return; }
		if (!us.isTimeMoving()) { ms = 0; }
		if (stars.isEmpty()) {
			stars.ensureCapacity(7000);
			for (int i = 0; i < 7000; i++) {
				stars.add(new WeatherVisualLayer.Drop(
							AGame.ANIM_R.nextInt(Combat.COMBAT_AREA_W_NEW) - Combat.COMBAT_AREA_W_NEW / 2,
							AGame.ANIM_R.nextInt(3500 + 10 * AGame.SGS) + AGame.GROUND_LEVEL - 3500,
							0.1 + AGame.ANIM_R.nextDouble(),
							AGame.ANIM_R.nextInt(20)));
			}
		}
	}

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (SimplePref.REDUCED_VISUAL_NOISE.get()) { return; }
		TimeOfDay tod = us.getTimeOfDay();
		int numStars;
		if (us.prevTOD != null && us.prevTOD != tod) {
			numStars = (int) (us.prevTOD.effect.numStars * (1 - us.todMix) + tod.effect.numStars * us.todMix);
		} else {
			numStars = tod.effect.numStars;
		}
		if (numStars == 0) { return; }
		TextureImpl.bindNone();
		glBegin(GL_QUADS);
		double sz = 1.0 / us.zoom;
		for (int i = 0; i < stars.size(); i++) {
			WeatherVisualLayer.Drop star = stars.get(i);
			if (Rect2D.contains(cropX, cropY, cropW, cropH, star.x, star.y)) {
				float magnitude = (ticksDone / 20) % 20 == star.dy ? ((float) (Math.min(0.5, 0.5 * us.zoom) * star.dx)) : (float) (Math.min(0.75, 0.75 * us.zoom) * star.dx);
				magnitude *= numStars;
				magnitude /= 5000;
				if (magnitude < 0.1) { continue; }
				glColor4f(1, 1, 1, magnitude);
				glVertex2d(star.x + sz, star.y);
				glVertex2d(star.x + sz, star.y + sz);
				glVertex2d(star.x, star.y + sz);
				glVertex2d(star.x, star.y);
			}
		}
		glEnd();
	}
}
