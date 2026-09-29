package com.zarkonnen.airships;

import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;
import static com.zarkonnen.airships.Lang._t;

public class  Appearance {
	public static class Frame extends Img {
		public final int shiftX, shiftY;

		public Frame(String src, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipped, int shiftX, int shiftY) {
			super(src, srcX, srcY, srcWidth, srcHeight, flipped);
			this.shiftX = shiftX;
			this.shiftY = shiftY;
		}
		
		@Override
		public Frame flip() {
			return new Frame(src, srcX, srcY, srcWidth, srcHeight, !flipped, -shiftX, shiftY);
		}
	}
	
	public static final float ROUNDING_FIX = 0.01f;
	
	private static ShaderProgram sp;
	private static ShaderProgram maskedSP;
	public static ShaderProgram lsp;
	private static ShaderProgram maskedBlsp;
	private static ShaderProgram blsp;
	private static ShaderProgram blueprintShader;
	public static ShaderProgram redSolidShader;
	public static ShaderProgram outlineShader;
	private static ShaderProgram subsp;
	private static ShaderProgram sublsp;
	public static boolean shaderLoadFailed = false;
	public static boolean useSimpleGraphics = false;
	public static boolean useLighting = true;
	//public static boolean canUseLighting = true;
	
	private static boolean IS_MAC = System.getProperty("os.name").contains("Mac");
	
	public final SpritesheetBundle spritesheetBundle;
	public ArrayList<Frame> frames = new ArrayList<Frame>();
	public ArrayList<Frame> flippedFrames;
	public int interval = 300;
	private int w = 0;
	private int h = 0;
	private boolean isFlipped = false;
	public HashMap<TileMask, ArrayList<ArrayList<Img>>> rectsForFrames = null;
	public HashMap<TileMask, ArrayList<ArrayList<Img>>> rectsForFlippedFrames = null;
	
	public long checksum() {
		int cs = spritesheetBundle.name.hashCode();
		for (Img img : frames) {
			cs += img.src.hashCode() * 7 + img.srcX * 37 + img.srcY * 193 + img.srcWidth * 401 + img.srcHeight * 1299;
			cs *= 19;
		}
		cs += interval;
		return cs;
	}
	
	public void draw(Draw d, double x, double y, int ms, boolean flipped) {
		draw(d, x, y, 0, 0, ms, null, flipped);
	}
	
	public void draw(Draw d, double x, double y, int ms, boolean flipped, Image[] light, float strength, Color ambient, float ambientSaturation) {
		draw(d, x, y, 0, 0, ms, null, flipped, light, strength, ambient, ambientSaturation);
	}
	
	public void draw(Draw d, double x, double y, int ms, Clr tint, boolean flipped) {
		draw(d, x, y, 0, 0, ms, tint, flipped);
	}
	
	public void draw(Draw d, double x, double y, int ms, Clr tint, boolean flipped, Image[] light, float strength, Color ambient, float ambientSaturation) {
		draw(d, x, y, 0, 0, ms, tint, flipped, light, strength, ambient, ambientSaturation);
	}
	
	public void draw(Draw d, double x, double y, double w, double h, int ms, Clr tint, boolean flipped) {
		draw(d, x, y, w, h, ms, tint, flipped, null, 0, Color.white, 1.0f);
	}
	
	public static String currentPostfix = null;
	private static int switchNum = 0;
	public int mySwitchNum = 0;
	public static int subDebugCount = 12;
	
	public static void switchSpritesheet(String postfix) {
		if (postfix.equals(currentPostfix)) { return; }
		currentPostfix = postfix;
		switchNum++;
		for (SpritesheetBundle ssb : Loadable.all(SpritesheetBundle.class)) {
			ssb.loadPostfix(currentPostfix);
		}
	}
	
	public static void reloadSpritesheets() {
		if (currentPostfix == null) { return; }
		switchNum++;
		for (SpritesheetBundle ssb : Loadable.all(SpritesheetBundle.class)) {
			ssb.loadPostfix(currentPostfix);
			ssb.initBumps();
		}
	}
	
	private static boolean inited;
	
	public static void init() {
		if (Loadable.map.containsKey(SpritesheetBundle.class) && !inited) {
			switchSpritesheet("");
			for (SpritesheetBundle ssb : Loadable.all(SpritesheetBundle.class)) {
				ssb.initBumps();
			}
			inited = true;
		}
	}
	
	public void updateSpritesheet() {
		if (mySwitchNum < switchNum) {
			rectsForFrames = null;
			rectsForFlippedFrames = null;
		}
		if (mySwitchNum < switchNum && spritesheetBundle.bump != null) {
			Image image = spritesheetBundle.getSheet(currentPostfix);
			if (image == null) {
				System.err.println("Unable to switch to spritesheet " + spritesheetBundle.name + currentPostfix + ".");
				mySwitchNum = switchNum;
				return;
			}
			if (flippedFrames == null) {
				flippedFrames = new ArrayList<Frame>(frames.size());
				for (Frame f : frames) {
					flippedFrames.add(f.flip());
				}
			}
			for (Img img : frames) {
				img.machineImgCache = image.getSubImage(img.srcX, img.srcY, img.srcWidth, img.srcHeight);
				img.machineWCache = img.srcWidth;
				img.machineHCache = img.srcHeight;
			}
			for (Img img : flippedFrames) {
				img.machineImgCache = image.getSubImage(img.srcX, img.srcY, img.srcWidth, img.srcHeight).getFlippedCopy(true, false);
				img.machineWCache = img.srcWidth;
				img.machineHCache = img.srcHeight;
			}
			mySwitchNum = switchNum;
		}
	}

	public void drawFallback(Draw d, double x, double y, double w, double h, int ms, Clr tint, boolean flipped) {
		flipped ^= isFlipped;
		if (flipped && flippedFrames == null) {
			flippedFrames = new ArrayList<Frame>(frames.size());
			for (Frame f : frames) {
				flippedFrames.add(f.flip());
			}
		}
		
		flipped ^= frames.get((ms / interval) % frames.size()).flipped;
		
		if (flipped && flippedFrames == null) {
			flippedFrames = new ArrayList<Frame>(frames.size());
			for (Frame f : frames) {
				flippedFrames.add(f.flip());
			}
		}
		
		ArrayList<Frame> fs = flipped ? flippedFrames : frames;
		Frame f = fs.get((ms / interval) % fs.size());
		if (f.machineImgCache != null) {
			((Image) f.machineImgCache).setFilter(Image.FILTER_NEAREST);
		}
		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		d.blit(f, tint, x + f.shiftX * (flipped ? -1 : 1), y + f.shiftY, w, h);
	}
	
	public void maskedDrawFallback(Draw d, double x, double y, double w, double h, int ms, Clr tint, boolean flipped, TileMask tileMask) {
		flipped ^= isFlipped;
		if (flipped && flippedFrames == null) {
			flippedFrames = new ArrayList<Frame>(frames.size());
			for (Frame f : frames) {
				flippedFrames.add(f.flip());
			}
		}
		
		ArrayList<Frame> fs = flipped ? flippedFrames : frames;
		int frameIndex = (ms / interval) % fs.size();
		Frame f = fs.get(frameIndex);
		
		Image image = spritesheetBundle.getSheet(currentPostfix);
		if (image == null) {
			image = spritesheetBundle.getSheet(null);
		}
		if (image == null) {
			return;
		}
		
		if (rectsForFrames == null) {
			rectsForFrames = new HashMap<TileMask, ArrayList<ArrayList<Img>>>();
			rectsForFlippedFrames = new HashMap<TileMask, ArrayList<ArrayList<Img>>>();
		}
		
		HashMap<TileMask, ArrayList<ArrayList<Img>>> rff = flipped ? rectsForFlippedFrames : rectsForFrames;
		
		if (!rff.containsKey(tileMask)) {
			ArrayList<ArrayList<Img>> frameRects = new ArrayList<ArrayList<Img>>();
			for (int fi = 0; fi < fs.size(); fi++) {
				Frame currentF = fs.get(fi);
				ArrayList<Img> imgs = new ArrayList<Img>();
				int rsz = tileMask.rects.size();
				for (int ri = 0; ri < rsz; ri++) {
					TileMask.Rect r = tileMask.rects.get(ri);
					Img img = new Img(currentF.src, currentF.srcX + r.x, currentF.srcY + r.y, r.w, r.h, currentF.flipped);
					img.machineImgCache = image.getSubImage(img.srcX, img.srcY, img.srcWidth, img.srcHeight);
					img.machineWCache = img.srcWidth;
					img.machineHCache = img.srcHeight;
					imgs.add(img);
				}
				frameRects.add(imgs);
			}
			rff.put(tileMask, frameRects);
		}
		
		ArrayList<Img> imgs = rff.get(tileMask).get(frameIndex);
		int isz = imgs.size();
		for (int ii = 0; ii < isz; ii++) {
			Img img = imgs.get(ii);
			if (img.machineImgCache != null) {
				((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			d.blit(img, tint, x + f.shiftX * (flipped ? -1 : 1) + img.srcX - f.srcX, y + f.shiftY + img.srcY - f.srcY);
		}
	}
	
	public void drawAsRedSolid(Draw d, double x, double y, int ms, boolean flipped) {
		drawAsRedSolid(d, x, y, 0, 0, ms, flipped);
	}
	
	public void drawAsRedSolid(Draw d, double x, double y, double w, double h, int ms, boolean flipped) {
		updateSpritesheet();
		
		if (shaderLoadFailed || useSimpleGraphics || !useLighting) {
			drawFallback(d, x, y, w, h, ms, Clr.RED, flipped);
			return;
		}
		
		if (redSolidShader == null) {
			try {
				redSolidShader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
						AGame.getStaticGameDirectoryPath("data/redoutline.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}

		redSolidShader.bind();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
		redSolidShader.setUniform1i("tex", 0);
		redSolidShader.setUniform1f("texSize", spritesheetBundle.size);
		
		Frame f = frames.get((ms / interval) % frames.size());
		
		x += f.shiftX * (flipped ? -1 : 1);
		y += f.shiftY;
		
		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		
		glBegin(GL_QUADS);
		if (flipped ^ isFlipped ^ f.flipped) {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		glEnd();
		redSolidShader.unbind();
		TextureImpl.bindNone();
	}
	
	public void drawAsOutline(Draw d, double x, double y, double w, double h, int ms, boolean flipped, Clr c) {
		updateSpritesheet();
		
		if (shaderLoadFailed || useSimpleGraphics || !useLighting) {
			drawFallback(d, x, y, w, h, ms, Clr.RED, flipped);
			return;
		}
		
		if (outlineShader == null) {
			try {
				outlineShader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough2.vert"),
						AGame.getStaticGameDirectoryPath("data/outline.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}

		outlineShader.bind();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
		outlineShader.setUniform1i("tex", 0);
		outlineShader.setUniform1f("texSize", spritesheetBundle.size);
		
		Frame f = frames.get((ms / interval) % frames.size());
		
		x += f.shiftX * (flipped ? -1 : 1);
		y += f.shiftY;
		
		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		
		glBegin(GL_QUADS);
		glColor4f(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f);
		if (flipped ^ isFlipped ^ f.flipped) {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		glEnd();
		outlineShader.unbind();
		TextureImpl.bindNone();
	}
	
	public void drawAsBlueprint(Draw d, double x, double y, int ms, boolean flipped, float intensity) {
		drawAsBlueprint(d, x, y, 0, 0, ms, flipped, intensity);
	}
	
	public static void drawAsBlueprint(Img img, Draw d, double x, double y, double w, double h, boolean flipped, float intensity) {
		if (img instanceof Frame) {
			x += ((Frame) img).shiftX * (flipped ? -1 : 1);
			x += ((Frame) img).shiftY;
		}
		if (shaderLoadFailed || useSimpleGraphics) {
			d.blit(img, x, y, w, h);
			return;
		}
			
		if (blueprintShader == null) {
			try {
				blueprintShader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
						AGame.getStaticGameDirectoryPath("data/blueprint.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}
		
		if (!Loadable.hasOfName(SpritesheetBundle.class, img.src)) { return; }

		blueprintShader.bind();
		blueprintShader.setUniform1f("intensity", intensity);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, SpritesheetBundle.ofName(img.src).getTex(currentPostfix).getTextureID());
		blueprintShader.setUniform1i("tex", 0);
		blueprintShader.setUniform1f("texSize", SpritesheetBundle.ofName(img.src).size);
				
		glBegin(GL_QUADS);
		if (flipped) {
			glTexCoord2d(img.srcX + ROUNDING_FIX, img.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(img.srcX + ROUNDING_FIX, img.srcY + img.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(img.srcX + img.srcWidth - ROUNDING_FIX, img.srcY + img.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(img.srcX + img.srcWidth - ROUNDING_FIX, img.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(img.srcX + ROUNDING_FIX, img.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(img.srcX + ROUNDING_FIX, img.srcY + img.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(img.srcX + img.srcWidth - ROUNDING_FIX, img.srcY + img.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(img.srcX + img.srcWidth - ROUNDING_FIX, img.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		glEnd();
		blueprintShader.unbind();
		TextureImpl.bindNone();
	}
	
	public void drawAsBlueprint(Draw d, double x, double y, double w, double h, int ms, boolean flipped, float intensity) {
		updateSpritesheet();
		
		if (shaderLoadFailed || useSimpleGraphics || !useLighting) {
			drawFallback(d, x, y, w, h, ms, null, flipped);
			return;
		}
		
		if (blueprintShader == null) {
			try {
				blueprintShader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
						AGame.getStaticGameDirectoryPath("data/blueprint.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}

		blueprintShader.bind();
		blueprintShader.setUniform1f("intensity", intensity);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
		blueprintShader.setUniform1i("tex", 0);
		blueprintShader.setUniform1f("texSize", spritesheetBundle.size);
		
		Frame f = frames.get((ms / interval) % frames.size());
		
		x += f.shiftX * (flipped ? -1 : 1);
		y += f.shiftY;
		
		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		
		glBegin(GL_QUADS);
		if (flipped ^ isFlipped ^ f.flipped) {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		glEnd();
		blueprintShader.unbind();
		TextureImpl.bindNone();
	}
	
	public static boolean shaderLocked;
	private static boolean lockedWithLight;
	
	public static void lockShader(SpritesheetBundle ssb, Draw d, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		if (ssb.bumpTex == null) {
			light = null;
		}
		if (ssb.getTex(currentPostfix) == null) {
			return;
		}
		
		if (shaderLoadFailed || useSimpleGraphics) { return; }
		
		lockedWithLight = light != null;
		
		if (light != null) {
			if (lsp == null) {
				try {
					lsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			lsp.bind();
			
			glActiveTexture(GL_TEXTURE1);
			/*if (ssb.bumpTex == null) {
				System.out.println("bump map missing: " + ssb.bump);
			}*/
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.bumpTex.getTextureID());
			lsp.setUniform1i("bump", 1);

			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
			lsp.setUniform1i("lightFromLeft", 2);

			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
			lsp.setUniform1i("lightFromTop", 3);

			glActiveTexture(GL_TEXTURE4);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
			lsp.setUniform1i("lightFromRight", 4);

			glActiveTexture(GL_TEXTURE5);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
			lsp.setUniform1i("lightFromBottom", 5);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			lsp.setUniform1i("tex", 0);

			//lsp.setUniform1f("strength", lightStrength);
			lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
			lsp.setUniform1f("screenHeight", d.frame().mode().height);
			lsp.setUniform4f("ambient", ambient);
			lsp.setUniform1f("ambientSaturation", ambientSaturation);
			lsp.setUniform1f("texSize", ssb.size);
			
			lsp.enableVertexAttribute("flipped");
			lsp.enableVertexAttribute("tint");
			lsp.enableVertexAttribute("strength");
		} else {
			if (sp == null) {
				try {
					sp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
							AGame.getStaticGameDirectoryPath("data/frag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			sp.bind();
			
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			sp.setUniform1i("tex", 0);
			sp.enableVertexAttribute("tint");
			sp.setUniform1f("texSize", ssb.size);
		}
		glBegin(GL_QUADS);
		shaderLocked = true;
	}
	
	public static void unlockShader(boolean light) {
		if (shaderLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (lockedWithLight) {
				lsp.unbind();
			} else {
				sp.unbind();
			}
			TextureImpl.bindNone();
			shaderLocked = false;
		}
	}
	
	public static void dealWithBadDrivers() {
		useSimpleGraphics = true;
		AirshipGame.instance.saveSettings();
		AirshipGame.instance.showError(_t("bad_amd_driver_info"));
	}
	
	public void draw(Draw d, double x, double y, double w, double h, int ms, Clr tint, boolean flipped, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		if (
				//Appearance.shaderLocked ||
				Appearance.subShaderLocked ||
				Appearance.maskedBevelledLocked ||
				RotatingShader.shaderLocked ||
				RotatingColoringShader.shaderLocked
		) {
			AirshipGame.instance.reportError("Appearance.draw mis-lock" +
					" Appearance.shaderLocked=" + Appearance.shaderLocked +
					" Appearance.subShaderLocked=" + Appearance.subShaderLocked +
					" Appearance.bevelledLocked=" + Appearance.maskedBevelledLocked +
					" RotatingShader.shaderLocked=" + RotatingShader.shaderLocked +
					" RotatingColoringShader.shaderLocked=" + RotatingColoringShader.shaderLocked,
					null, null, false, true);
		}
		
		updateSpritesheet();
		
		if (spritesheetBundle.bumpTex == null) {
			light = null;
		}
		
		if (SimplePref.REDUCED_VISUAL_NOISE.get()) {
			ms = 0;
		}
		
		if (shaderLoadFailed || useSimpleGraphics || spritesheetBundle.getTex(currentPostfix) == null) {
			drawFallback(d, x, y, w, h, ms, tint, flipped);
			return;
		}
		
		if (light != null) {
			if (lsp == null) {
				try {
					lsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			if (!shaderLocked) {
				lsp.bind();
				
				glActiveTexture(GL_TEXTURE1);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.bumpTex.getTextureID());
				lsp.setUniform1i("bump", 1);

				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
				lsp.setUniform1i("lightFromLeft", 2);

				glActiveTexture(GL_TEXTURE3);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
				lsp.setUniform1i("lightFromTop", 3);

				glActiveTexture(GL_TEXTURE4);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
				lsp.setUniform1i("lightFromRight", 4);

				glActiveTexture(GL_TEXTURE5);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
				lsp.setUniform1i("lightFromBottom", 5);

				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
				lsp.setUniform1i("tex", 0);
				
				//lsp.setUniform1f("strength", lightStrength);
				lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
				lsp.setUniform1f("screenHeight", d.frame().mode().height);
				lsp.setUniform4f("ambient", ambient);
				lsp.setUniform1f("ambientSaturation", ambientSaturation);
				lsp.setUniform1f("texSize", spritesheetBundle.size);
				
				lsp.enableVertexAttribute("flipped");
				lsp.enableVertexAttribute("tint");
				lsp.enableVertexAttribute("strength");
			}
		} else {
			if (sp == null) {
				try {
					sp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
							AGame.getStaticGameDirectoryPath("data/frag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			if (!shaderLocked) {
				try {
					sp.bind();
				} catch (NullPointerException npe) {
					dealWithBadDrivers();
					return;
				}
				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
				sp.setUniform1i("tex", 0);
				sp.enableVertexAttribute("tint");
				sp.setUniform1f("texSize", spritesheetBundle.size);
			}
		}
		
		Frame f = frames.get((ms / interval) % frames.size());

		x += f.shiftX * (flipped ? -1 : 1);
		y += f.shiftY;

		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}

		if (!shaderLocked) {
			glBegin(GL_QUADS);
		}
				
		if (light != null) {
			GLCompat.glVertexAttrib1f(lsp.getAttributeID("flipped"), flipped ^ isFlipped ^ f.flipped ? (float) 1 : (float) 0);
			if (tint == null) {
				GLCompat.glVertexAttrib4f(lsp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				GLCompat.glVertexAttrib4f(lsp.getAttributeID("tint"), tint.r / 255.0f, tint.g / 255.0f, tint.b / 255.0f, 1.0f);
			}
			GLCompat.glVertexAttrib1f(lsp.getAttributeID("strength"), lightStrength);
		} else {
			if (tint == null) {
				GLCompat.glVertexAttrib4f(sp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				GLCompat.glVertexAttrib4f(sp.getAttributeID("tint"), tint.r / 255.0f, tint.g / 255.0f, tint.b / 255.0f, 1.0f);
			}
		}
		
		if (flipped ^ isFlipped ^ f.flipped) {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		if (!shaderLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (light != null) {
				lsp.unbind();
			} else {
				sp.unbind();
			}
			TextureImpl.bindNone();
		}
	}
	
	public static boolean subShaderLocked;
	
	public static void lockSubShader(SpritesheetBundle ssb, Draw d, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		if (ssb.bumpTex == null) {
			light = null;
		}
		if (ssb.getTex(currentPostfix) == null) {
			return;
		}
		if (ssb.getTex("") == null) {
			return;
		}
		
		if (shaderLoadFailed || useSimpleGraphics) { return; }
		
		lockedWithLight = light != null;
		
		if (light != null) {
			if (sublsp == null) {
				try {
					sublsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/sub_litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/sub_litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			sublsp.bind();
			
			glActiveTexture(GL_TEXTURE1);
			/*if (ssb.bumpTex == null) {
				System.out.println("bump map missing: " + ssb.bump);
			}*/
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.bumpTex.getTextureID());
			sublsp.setUniform1i("bump", 1);

			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
			sublsp.setUniform1i("lightFromLeft", 2);

			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
			sublsp.setUniform1i("lightFromTop", 3);

			glActiveTexture(GL_TEXTURE4);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
			sublsp.setUniform1i("lightFromRight", 4);

			glActiveTexture(GL_TEXTURE5);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
			sublsp.setUniform1i("lightFromBottom", 5);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			sublsp.setUniform1i("tex", 0);

			//lsp.setUniform1f("strength", lightStrength);
			sublsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
			sublsp.setUniform1f("screenHeight", d.frame().mode().height);
			sublsp.setUniform4f("ambient", ambient);
			sublsp.setUniform1f("ambientSaturation", ambientSaturation);
			sublsp.setUniform1f("texSize", ssb.size);
			
			sublsp.enableVertexAttribute("flipped");
			sublsp.enableVertexAttribute("tint");
			sublsp.enableVertexAttribute("strength");
			sublsp.enableVertexAttribute("srcA");
			sublsp.enableVertexAttribute("trgA");
		} else {
			if (subsp == null) {
				try {
					subsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/sub_frag.vert"),
							AGame.getStaticGameDirectoryPath("data/sub_frag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			subsp.bind();
			
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex("").getTextureID());
			subsp.setUniform1i("refTex", 1);
			
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			subsp.setUniform1i("tex", 0);
			
			subsp.setUniform1f("texSize", ssb.size);
			subsp.setUniform4f("ambient", ambient);
			
			subsp.enableVertexAttribute("tint");
			subsp.enableVertexAttribute("srcA");
			subsp.enableVertexAttribute("trgA");
		}
		glBegin(GL_QUADS);
		subShaderLocked = true;
	}
	
	public static void unlockSubShader(boolean light) {
		if (subShaderLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (lockedWithLight) {
				sublsp.unbind();
			} else {
				subsp.unbind();
			}
			TextureImpl.bindNone();
			subShaderLocked = false;
		}
	}
	
	public void drawSub(Draw d, double x, double y, double w, double h, int ms, Clr tint, boolean flipped, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr src, Clr trg, float trgShiny) {
		if (
				Appearance.shaderLocked ||
				//Appearance.subShaderLocked ||
				Appearance.maskedBevelledLocked ||
				RotatingShader.shaderLocked ||
				RotatingColoringShader.shaderLocked
		) {
			AirshipGame.instance.reportError("Appearance.drawSub mis-lock" +
					" Appearance.shaderLocked=" + Appearance.shaderLocked +
					" Appearance.subShaderLocked=" + Appearance.subShaderLocked +
					" Appearance.bevelledLocked=" + Appearance.maskedBevelledLocked +
					" RotatingShader.shaderLocked=" + RotatingShader.shaderLocked +
					" RotatingColoringShader.shaderLocked=" + RotatingColoringShader.shaderLocked,
					null, null, false, true);
		}
		
		updateSpritesheet();
		
		if (spritesheetBundle.bumpTex == null) {
			light = null;
		}
		
		if (SimplePref.REDUCED_VISUAL_NOISE.get()) {
			ms = 0;
		}
		
		if (shaderLoadFailed || useSimpleGraphics || spritesheetBundle.getTex(currentPostfix) == null) {
			drawFallback(d, x, y, w, h, ms, tint, flipped);
			return;
		}
		
		if (light != null) {
			if (sublsp == null) {
				try {
					sublsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/sub_litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/sub_litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			if (!subShaderLocked) {
				sublsp.bind();
				
				glActiveTexture(GL_TEXTURE1);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.bumpTex.getTextureID());
				sublsp.setUniform1i("bump", 1);

				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
				sublsp.setUniform1i("lightFromLeft", 2);

				glActiveTexture(GL_TEXTURE3);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
				sublsp.setUniform1i("lightFromTop", 3);

				glActiveTexture(GL_TEXTURE4);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
				sublsp.setUniform1i("lightFromRight", 4);

				glActiveTexture(GL_TEXTURE5);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
				sublsp.setUniform1i("lightFromBottom", 5);

				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
				lsp.setUniform1i("tex", 0);
				
				//lsp.setUniform1f("strength", lightStrength);
				sublsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
				sublsp.setUniform1f("screenHeight", d.frame().mode().height);
				sublsp.setUniform4f("ambient", ambient);
				sublsp.setUniform1f("ambientSaturation", ambientSaturation);
				sublsp.setUniform1f("texSize", spritesheetBundle.size);
				
				sublsp.enableVertexAttribute("flipped");
				sublsp.enableVertexAttribute("tint");
				sublsp.enableVertexAttribute("strength");
				sublsp.enableVertexAttribute("srcA");
				sublsp.enableVertexAttribute("trgA");
			}
		} else {
			if (subsp == null) {
				try {
					subsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/sub_frag.vert"),
							AGame.getStaticGameDirectoryPath("data/sub_frag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			if (!subShaderLocked) {
				subsp.bind();
				glActiveTexture(GL_TEXTURE1);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex("").getTextureID());
				subsp.setUniform1i("refTex", 1);
				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
				subsp.setUniform1i("tex", 0);
				subsp.setUniform1f("texSize", spritesheetBundle.size);
				subsp.setUniform4f("ambient", ambient);
				subsp.enableVertexAttribute("tint");
				subsp.enableVertexAttribute("srcA");
				subsp.enableVertexAttribute("trgA");
			}
		}
		
		Frame f = frames.get((ms / interval) % frames.size());

		x += f.shiftX * (flipped ? -1 : 1);
		y += f.shiftY;

		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		if (Appearance.subDebugCount > 0) {
			Appearance.subDebugCount--;
			System.err.println("SUB-DEBUG ssb=" + spritesheetBundle.name + " size=" + spritesheetBundle.size
				+ " f=(" + f.srcX + "," + f.srcY + "," + f.srcWidth + "," + f.srcHeight + ")"
				+ " w=" + w + " h=" + h + " x=" + x + " y=" + y
				+ " texU=" + (f.srcX / (double) spritesheetBundle.size) + ".." + ((f.srcX + f.srcWidth) / (double) spritesheetBundle.size)
				+ " texV=" + (f.srcY / (double) spritesheetBundle.size) + ".." + ((f.srcY + f.srcHeight) / (double) spritesheetBundle.size));
		}

		if (!subShaderLocked) {
			glBegin(GL_QUADS);
		}
				
		if (light != null) {
			GLCompat.glVertexAttrib1f(sublsp.getAttributeID("flipped"), flipped ^ isFlipped ^ f.flipped ? (float) 1 : (float) 0);
			if (tint == null) {
				GLCompat.glVertexAttrib4f(sublsp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				GLCompat.glVertexAttrib4f(sublsp.getAttributeID("tint"), tint.r / 255.0f, tint.g / 255.0f, tint.b / 255.0f, 1.0f);
			}
			GLCompat.glVertexAttrib1f(sublsp.getAttributeID("strength"), lightStrength);
			if (src == null) {
				GLCompat.glVertexAttrib3f(sublsp.getAttributeID("srcA"), -1.0f, -1.0f, -1.0f);
				GLCompat.glVertexAttrib4f(sublsp.getAttributeID("trgA"), 1.0f, 0f, 1.0f, 0.36f);
			} else {
				GLCompat.glVertexAttrib3f(sublsp.getAttributeID("srcA"), src.r / 255.0f, src.g / 255.0f, src.b / 255.0f);
				GLCompat.glVertexAttrib4f(sublsp.getAttributeID("trgA"), trg.r / 255.0f, trg.g / 255.0f, trg.b / 255.0f, trgShiny);
			}
		} else {
			if (tint == null) {
				GLCompat.glVertexAttrib4f(subsp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				GLCompat.glVertexAttrib4f(subsp.getAttributeID("tint"), tint.r / 255.0f, tint.g / 255.0f, tint.b / 255.0f, 1.0f);
			}
			if (src == null) {
				GLCompat.glVertexAttrib3f(subsp.getAttributeID("srcA"), -1.0f, -1.0f, -1.0f);
				GLCompat.glVertexAttrib4f(subsp.getAttributeID("trgA"), 1.0f, 0f, 1.0f, 0.36f);
			} else {
				GLCompat.glVertexAttrib3f(subsp.getAttributeID("srcA"), src.r / 255.0f, src.g / 255.0f, src.b / 255.0f);
				GLCompat.glVertexAttrib4f(subsp.getAttributeID("trgA"), trg.r / 255.0f, trg.g / 255.0f, trg.b / 255.0f, trgShiny);
			}
		}
		
		if (flipped ^ isFlipped ^ f.flipped) {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		if (!subShaderLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (light != null) {
				sublsp.unbind();
			} else {
				subsp.unbind();
			}
			TextureImpl.bindNone();
		}
	}
	
	public static boolean maskedBevelledLocked;
	
	public static void lockMaskedBevelledShader(SpritesheetBundle ssb, SpritesheetBundle maskSSB, Draw d, Image[] light, float lightStrength, Color ambient, float ambientSaturation, boolean concave) {
		if (ssb.bumpTex == null || maskSSB.bumpTex == null) {
			light = null;
		}
		if (ssb.getTex(currentPostfix) == null) {
			return;
		}
		
		if (shaderLoadFailed || useSimpleGraphics) { return; }
		
		if (light != null) {
			if (maskedBlsp == null) {
				try {
					maskedBlsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/bevelled_masked_litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/bevelled_masked_litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			maskedBlsp.bind();
			
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.bumpTex.getTextureID());
			maskedBlsp.setUniform1i("bump", 1);

			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[concave ? 2 : 0].getTexture().getTextureID());
			maskedBlsp.setUniform1i("lightFromLeft", 2);

			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
			maskedBlsp.setUniform1i("lightFromTop", 3);

			glActiveTexture(GL_TEXTURE4);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[concave ? 0 : 2].getTexture().getTextureID());
			maskedBlsp.setUniform1i("lightFromRight", 4);

			glActiveTexture(GL_TEXTURE5);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
			maskedBlsp.setUniform1i("lightFromBottom", 5);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			maskedBlsp.setUniform1i("tex", 0);
			
			glActiveTexture(GL_TEXTURE6);
			glBindTexture(GLCompat.GL_TEXTURE_2D, maskSSB.getTex(currentPostfix).getTextureID());
			maskedBlsp.setUniform1i("mask", 6);
			
			glActiveTexture(GL_TEXTURE7);
			glBindTexture(GLCompat.GL_TEXTURE_2D, maskSSB.bumpTex.getTextureID());
			maskedBlsp.setUniform1i("maskBump", 7);
			
			maskedBlsp.setUniform1f("strength", lightStrength);
			maskedBlsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
			maskedBlsp.setUniform1f("screenHeight", d.frame().mode().height);
			maskedBlsp.setUniform4f("ambient", ambient);
			maskedBlsp.setUniform1f("ambientSaturation", ambientSaturation);
			maskedBlsp.setUniform2f("texSize", ssb.size, maskSSB.size);
			
			maskedBlsp.enableVertexAttribute("globalTexCoord");
			maskedBlsp.enableVertexAttribute("flipped_concave");
			maskedBlsp.enableVertexAttribute("bevel");
			maskedBlsp.enableVertexAttribute("t");
			maskedBlsp.enableVertexAttribute("m");
			if (!IS_MAC) { // qqDPS Heinous hack to get around Mac OS X's broken OpenGL implementation?
				maskedBlsp.enableVertexAttribute("b");
			}
			maskedBlsp.enableVertexAttribute("tint");
			maskedBlsp.enableVertexAttribute("paint");
			maskedBlsp.enableVertexAttribute("maskOffsetAndEnabled");
		} else {
			if (maskedSP == null) {
				try {
					maskedSP = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/masked.vert"),
							AGame.getStaticGameDirectoryPath("data/masked.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			maskedSP.bind();
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			maskedSP.setUniform1i("tex", 0);
			
			glActiveTexture(GL_TEXTURE6);
			glBindTexture(GLCompat.GL_TEXTURE_2D, maskSSB.getTex(currentPostfix).getTextureID());
			maskedSP.setUniform1i("mask", 6);
			
			maskedSP.setUniform2f("texSize", ssb.size, maskSSB.size);
						
			maskedSP.enableVertexAttribute("maskOffsetAndEnabled");
			maskedSP.enableVertexAttribute("globalTexCoord");
			maskedSP.enableVertexAttribute("paint");
			maskedSP.enableVertexAttribute("tint");
		}
		glBegin(GL_QUADS);
		maskedBevelledLocked = true;
	}
	
	public static void unlockMaskedBevelledShader(boolean light) {
		if (maskedBevelledLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (light) {
				maskedBlsp.unbind();
			} else {
				maskedSP.unbind();
			}
			TextureImpl.bindNone();
			maskedBevelledLocked = false;
		}
	}
		
	public void drawMaskedBevelled(Draw d, double x, double y, double w, double h, int ms, Clr tint, boolean flipped, Image[] light, float lightStrength, Color ambient, float ambientSaturation,
			int top, int bottom, int left, int right, boolean concave, int[][] patch9, Clr paint, float paintShiny, boolean mask, int maskX, int maskY, TileMask tileMask, SpritesheetBundle maskSSB)
	{
		if (
				Appearance.shaderLocked ||
				Appearance.subShaderLocked ||
				//Appearance.bevelledLocked ||
				RotatingShader.shaderLocked ||
				RotatingColoringShader.shaderLocked
		) {
			AirshipGame.instance.reportError("Appearance.drawBevelled mis-lock" +
					" Appearance.shaderLocked=" + Appearance.shaderLocked +
					" Appearance.subShaderLocked=" + Appearance.subShaderLocked +
					" Appearance.bevelledLocked=" + Appearance.maskedBevelledLocked +
					" RotatingShader.shaderLocked=" + RotatingShader.shaderLocked +
					" RotatingColoringShader.shaderLocked=" + RotatingColoringShader.shaderLocked,
					null, null, false, true);
		}
		
		updateSpritesheet();
		
		if (spritesheetBundle.bumpTex == null) {
			light = null;
		}
		if (maskSSB.bumpTex == null) {
			light = null;
		}
		
		if (SimplePref.REDUCED_VISUAL_NOISE.get()) {
			ms = 0;
		}
		
		if (shaderLoadFailed || useSimpleGraphics || spritesheetBundle.getTex(currentPostfix) == null) {
			if (tileMask != null && tileMask != TileMask.FULL) {
				maskedDrawFallback(d, x, y, w, h, ms, tint, flipped, tileMask);
			} else {
				drawFallback(d, x, y, w, h, ms, tint, flipped);
			}
			return;
		}
		
		if (light != null) {
			if (maskedBlsp == null) {
				try {
					maskedBlsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/bevelled_masked_litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/bevelled_masked_litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			if (!maskedBevelledLocked) {
				maskedBlsp.bind();
				
				glActiveTexture(GL_TEXTURE1);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.bumpTex.getTextureID());
				maskedBlsp.setUniform1i("bump", 1);

				glActiveTexture(GL_TEXTURE2);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[concave ? 2 : 0].getTexture().getTextureID());
				maskedBlsp.setUniform1i("lightFromLeft", 2);

				glActiveTexture(GL_TEXTURE3);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
				maskedBlsp.setUniform1i("lightFromTop", 3);

				glActiveTexture(GL_TEXTURE4);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[concave ? 0 : 2].getTexture().getTextureID());
				maskedBlsp.setUniform1i("lightFromRight", 4);

				glActiveTexture(GL_TEXTURE5);
				glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
				maskedBlsp.setUniform1i("lightFromBottom", 5);

				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
				maskedBlsp.setUniform1i("tex", 0);
				
				glActiveTexture(GL_TEXTURE6);
				glBindTexture(GLCompat.GL_TEXTURE_2D, maskSSB.getTex(currentPostfix).getTextureID());
				maskedBlsp.setUniform1i("mask", 6);

				glActiveTexture(GL_TEXTURE7);
				glBindTexture(GLCompat.GL_TEXTURE_2D, maskSSB.bumpTex.getTextureID());
				maskedBlsp.setUniform1i("maskBump", 7);
				
				maskedBlsp.setUniform1f("strength", lightStrength);
				maskedBlsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
				maskedBlsp.setUniform1f("screenHeight", d.frame().mode().height);
				maskedBlsp.setUniform4f("ambient", ambient);
				maskedBlsp.setUniform1f("ambientSaturation", ambientSaturation);
				maskedBlsp.setUniform2f("texSize", spritesheetBundle.size, maskSSB.size);
				
				maskedBlsp.enableVertexAttribute("globalTexCoord");
				maskedBlsp.enableVertexAttribute("bevel");
				maskedBlsp.enableVertexAttribute("t");
				maskedBlsp.enableVertexAttribute("m");
				if (!IS_MAC) {
					maskedBlsp.enableVertexAttribute("b");
				}
				maskedBlsp.enableVertexAttribute("tint");
				maskedBlsp.enableVertexAttribute("paint");
				maskedBlsp.enableVertexAttribute("flipped_concave");
				maskedBlsp.enableVertexAttribute("maskOffsetAndEnabled");
			}
		} else {
			if (maskedSP == null) {
				try {
					maskedSP = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/masked.vert"),
							AGame.getStaticGameDirectoryPath("data/masked.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			if (!maskedBevelledLocked) {
				maskedSP.bind();
				glActiveTexture(GL_TEXTURE0);
				glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
				maskedSP.setUniform1i("tex", 0);
				
				glActiveTexture(GL_TEXTURE6);
				glBindTexture(GLCompat.GL_TEXTURE_2D, maskSSB.getTex(currentPostfix).getTextureID());
				maskedSP.setUniform1i("mask", 6);
				
				maskedSP.setUniform2f("texSize", spritesheetBundle.size, maskSSB.size);

				maskedSP.enableVertexAttribute("maskOffsetAndEnabled");
				maskedSP.enableVertexAttribute("globalTexCoord");
				maskedSP.enableVertexAttribute("paint");
				maskedSP.enableVertexAttribute("tint");
			}
		}
		
		Frame f = frames.get((ms / interval) % frames.size());
		
		x += f.shiftX * (flipped ? -1 : 1);
		y += f.shiftY;
		
		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		
		if (light != null) {
			GLCompat.glVertexAttrib2f(maskedBlsp.getAttributeID("flipped_concave"), flipped ^ isFlipped ^ f.flipped ? -1f : 1f, flipped ^ concave ? 1f : 0f);
			GLCompat.glVertexAttrib4f(maskedBlsp.getAttributeID("bevel"), top, bottom, left, right);
			GLCompat.glVertexAttrib3f(maskedBlsp.getAttributeID("t"), patch9[0][0], patch9[0][1], patch9[0][2]);
			GLCompat.glVertexAttrib3f(maskedBlsp.getAttributeID("m"), patch9[1][0], patch9[1][1], patch9[1][2]);
			GLCompat.glVertexAttrib3f(maskedBlsp.getAttributeID("b"), patch9[2][0], patch9[2][1], patch9[2][2]);
			if (tint == null) {
				GLCompat.glVertexAttrib4f(maskedBlsp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				GLCompat.glVertexAttrib4f(maskedBlsp.getAttributeID("tint"), tint.r / 255.0f, tint.g / 255.0f, tint.b / 255.0f, 1.0f);
			}
			if (paint == null) {
				GLCompat.glVertexAttrib4f(maskedBlsp.getAttributeID("paint"), 0.0f, 0f, 0f, 0.0f);
			} else {
				GLCompat.glVertexAttrib4f(maskedBlsp.getAttributeID("paint"), paint.r / 255f, paint.g / 255f, paint.b / 255f, paint.a / 255f);
			}
			GLCompat.glVertexAttrib4f(maskedBlsp.getAttributeID("maskOffsetAndEnabled"), maskX - f.srcX, maskY - f.srcY, mask ? 1 : -1, paintShiny);
		} else {
			if (tint == null) {
				GLCompat.glVertexAttrib4f(maskedSP.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
			} else {
				GLCompat.glVertexAttrib4f(maskedSP.getAttributeID("tint"), tint.r / 255.0f, tint.g / 255.0f, tint.b / 255.0f, 1.0f);
			}
			GLCompat.glVertexAttrib4f(maskedSP.getAttributeID("maskOffsetAndEnabled"), maskX - f.srcX, maskY - f.srcY, mask ? 1 : -1, paintShiny);
			if (paint == null) {
				GLCompat.glVertexAttrib4f(maskedSP.getAttributeID("paint"), 0.0f, 0f, 0f, 0.0f);
			} else {
				GLCompat.glVertexAttrib4f(maskedSP.getAttributeID("paint"), paint.r / 255f, paint.g / 255f, paint.b / 255f, paint.a / 255f);
			}
		}
		
		if (!maskedBevelledLocked) {
			glBegin(GL_QUADS);
		}
		
		ShaderProgram p = light == null ? maskedSP : maskedBlsp;
		
		if (flipped ^ isFlipped ^ f.flipped) {
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			GLCompat.glVertexAttrib2f(p.getAttributeID("globalTexCoord"), f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		if (!maskedBevelledLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (light != null) {
				maskedBlsp.unbind();
			} else {
				maskedSP.unbind();
			}
			TextureImpl.bindNone();
		}
	}
	
	public static boolean bevelledLocked;
	
	public static void lockBevelledShader(SpritesheetBundle ssb, Draw d, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		if (ssb.bumpTex == null) {
			light = null;
		}
		if (ssb.getTex(currentPostfix) == null) {
			return;
		}
		
		if (shaderLoadFailed || useSimpleGraphics) { return; }
		
		if (light != null) {
			if (blsp == null) {
				try {
					blsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/bevelled_litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/bevelled_litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			blsp.bind();
			
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.bumpTex.getTextureID());
			blsp.setUniform1i("bump", 1);

			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
			blsp.setUniform1i("lightFromLeft", 2);

			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
			blsp.setUniform1i("lightFromTop", 3);

			glActiveTexture(GL_TEXTURE4);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
			blsp.setUniform1i("lightFromRight", 4);

			glActiveTexture(GL_TEXTURE5);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
			blsp.setUniform1i("lightFromBottom", 5);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			blsp.setUniform1i("tex", 0);
			
			blsp.setUniform1f("strength", lightStrength);
			blsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
			blsp.setUniform1f("screenHeight", d.frame().mode().height);
			blsp.setUniform4f("ambient", ambient);
			blsp.setUniform1f("ambientSaturation", ambientSaturation);
			blsp.setUniform1f("texSize", ssb.size);
			
			blsp.enableVertexAttribute("flipped");
			blsp.enableVertexAttribute("bevel");
		} else {
			if (sp == null) {
				try {
					sp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
							AGame.getStaticGameDirectoryPath("data/frag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			sp.bind();
			
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(currentPostfix).getTextureID());
			sp.setUniform1i("tex", 0);
			sp.enableVertexAttribute("tint");
			sp.setUniform1f("texSize", ssb.size);
		}
		glBegin(GL_QUADS);
		bevelledLocked = true;
	}
	
	public static void unlockBevelledShader(boolean light) {
		if (bevelledLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (light) {
				blsp.unbind();
			} else {
				sp.unbind();
			}
			TextureImpl.bindNone();
			bevelledLocked = false;
		}
	}
		
	public void drawBevelled(Draw d, double x, double y, double w, double h, int ms, Image[] light, float lightStrength, Color ambient, float ambientSaturation,
			int top, int bottom, int left, int right)
	{
		if (
				Appearance.shaderLocked ||
				Appearance.subShaderLocked ||
				Appearance.maskedBevelledLocked ||
				RotatingShader.shaderLocked ||
				RotatingColoringShader.shaderLocked
		) {
			AirshipGame.instance.reportError("Appearance.drawBevelled mis-lock" +
					" Appearance.shaderLocked=" + Appearance.shaderLocked +
					" Appearance.subShaderLocked=" + Appearance.subShaderLocked +
					" Appearance.maskedBevelledLocked=" + Appearance.maskedBevelledLocked +
					" RotatingShader.shaderLocked=" + RotatingShader.shaderLocked +
					" RotatingColoringShader.shaderLocked=" + RotatingColoringShader.shaderLocked,
					null, null, false, true);
		}
		
		updateSpritesheet();
		
		if (spritesheetBundle.bumpTex == null) {
			light = null;
		}
		
		if (SimplePref.REDUCED_VISUAL_NOISE.get()) {
			ms = 0;
		}
		
		if (shaderLoadFailed || useSimpleGraphics || spritesheetBundle.getTex(currentPostfix) == null) {
			drawFallback(d, x, y, w, h, ms, null, false);
			return;
		}
		
		if (light != null) {
			if (blsp == null) {
				try {
					blsp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/bevelled_litfrag.vert"),
							AGame.getStaticGameDirectoryPath("data/bevelled_litfrag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			blsp.bind();
			
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.bumpTex.getTextureID());
			blsp.setUniform1i("bump", 1);

			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[0].getTexture().getTextureID());
			blsp.setUniform1i("lightFromLeft", 2);

			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[1].getTexture().getTextureID());
			blsp.setUniform1i("lightFromTop", 3);

			glActiveTexture(GL_TEXTURE4);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[2].getTexture().getTextureID());
			blsp.setUniform1i("lightFromRight", 4);

			glActiveTexture(GL_TEXTURE5);
			glBindTexture(GLCompat.GL_TEXTURE_2D, light[3].getTexture().getTextureID());
			blsp.setUniform1i("lightFromBottom", 5);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
			blsp.setUniform1i("tex", 0);
			
			blsp.setUniform1f("strength", lightStrength);
			blsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
			blsp.setUniform1f("screenHeight", d.frame().mode().height);
			blsp.setUniform4f("ambient", ambient);
			blsp.setUniform1f("ambientSaturation", ambientSaturation);
			blsp.setUniform1f("texSize", spritesheetBundle.size);
			
			blsp.enableVertexAttribute("flipped");
			blsp.enableVertexAttribute("bevel");
		} else {
			if (sp == null) {
				try {
					sp = ShaderProgram.loadProgram(
							AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
							AGame.getStaticGameDirectoryPath("data/frag.frag"));
				} catch (Exception e) {
					e.printStackTrace();
					shaderLoadFailed = true;
					return;
				}
			}

			sp.bind();
			
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, spritesheetBundle.getTex(currentPostfix).getTextureID());
			sp.setUniform1i("tex", 0);
			sp.enableVertexAttribute("tint");
			sp.setUniform1f("texSize", spritesheetBundle.size);
		}		
		
		Frame f = frames.get((ms / interval) % frames.size());
		
		x += f.shiftX;
		y += f.shiftY;
		
		if (w == 0 && h == 0) {
			w = f.srcWidth;
			h = f.srcHeight;
		}
		
		if (light != null) {
			GLCompat.glVertexAttrib1f(blsp.getAttributeID("flipped"), isFlipped ^ f.flipped ? 1f : 0f);
			GLCompat.glVertexAttrib4f(blsp.getAttributeID("bevel"), top, bottom, left, right);
		} else {
			GLCompat.glVertexAttrib4f(sp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
		}
		
		if (!bevelledLocked) {
			glBegin(GL_QUADS);
		}
				
		if (isFlipped ^ f.flipped) {
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			//glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			//glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			//glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			//glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		} else {
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
			glTexCoord2d(f.srcX + ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + f.srcHeight - ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
			glTexCoord2d(f.srcX + f.srcWidth - ROUNDING_FIX, f.srcY + ROUNDING_FIX);
			glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		}
		if (!bevelledLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			if (light != null) {
				blsp.unbind();
			} else {
				sp.unbind();
			}
			TextureImpl.bindNone();
		}
	}
	
	public Appearance frame(int x, int y) {
		return frame(x, y, 1, 1);
	}
	
	public Appearance frame(int x, int y, int w, int h) {
		Frame f = new Frame(spritesheetBundle.name, x * AGame.SGS, y * AGame.SGS, w * AGame.SGS, h * AGame.SGS, false, 0, 0);
		frames.add(f);
		this.w = StrictMath.max(this.w, w);
		this.h = StrictMath.max(this.h, h);
		return this;
	}
	
	public Appearance frame(int x, int y, int w, int h, boolean flipped) {
		return frame(x, y, w, h, flipped, 0, 0);
	}
	
	public Appearance frame(int x, int y, int w, int h, boolean flipped, int shiftX, int shiftY) {
		Frame f = new Frame(spritesheetBundle.name, x * AGame.SGS, y * AGame.SGS, w * AGame.SGS, h * AGame.SGS, flipped, shiftX, shiftY);
		frames.add(f);
		this.w = StrictMath.max(this.w, w);
		this.h = StrictMath.max(this.h, h);
		return this;
	}

	public Appearance flip() {
		Appearance a2 = new Appearance(spritesheetBundle);
		a2.frames = frames;
		a2.isFlipped = !isFlipped;
		a2.interval = interval;
		a2.w = w;
		a2.h = h;
		return a2;
	}
	
	public Appearance interval(int interval) {
		this.interval = interval;
		if (this.interval <= 0) {
			this.interval = 300;
		}
		return this;
	}
	
	public static Appearance app(SpritesheetBundle spritesheetBundle) { return new Appearance(spritesheetBundle); }
	public static Appearance app(SpritesheetBundle spritesheetBundle, int x, int y) { return new Appearance(spritesheetBundle, x, y); }
	public static Appearance app(SpritesheetBundle spritesheetBundle, int x, int y, int w, int h) { return new Appearance(spritesheetBundle, x, y, w, h); }
	
	public Appearance(SpritesheetBundle spritesheetBundle) { this.spritesheetBundle = spritesheetBundle; }
	public Appearance(SpritesheetBundle spritesheetBundle, int x, int y, int w, int h) {
		this.spritesheetBundle = spritesheetBundle;
		frame(x, y, w, h);
	}
	
	public Appearance(SpritesheetBundle spritesheetBundle, int x, int y) {
		this.spritesheetBundle = spritesheetBundle;
		frame(x, y);
	}
	
	public Appearance(JSONObject o) {
		spritesheetBundle = SpritesheetBundle.ofName(o.getString("src"));
		if (spritesheetBundle == null) {
			throw new RuntimeException("No spritesheet bundle of name \"" + o.getString("src") + "\"");
		}
		if (o.has("x")) {
			frame(o.getInt("x"), o.getInt("y"), o.optInt("w", 1), o.optInt("h", 1), o.optBoolean("flipped", false), o.optInt("shiftX", 0), o.optInt("shiftY", 0));
		} else {
			JSONArray framesA = o.getJSONArray("frames");
			for (int i = 0; i < framesA.length(); i++) {
				JSONObject f = framesA.getJSONObject(i);
				frame(f.getInt("x"), f.getInt("y"), f.optInt("w", 1), f.optInt("h", 1), f.optBoolean("flipped", false), f.optInt("shiftX", 0), f.optInt("shiftY", 0));
			}
		}
		interval = o.optInt("interval", 300);
		if (interval <= 0) {
			interval = 300;
		}
	}

	public int width() { return w; }
	public int height() { return h; }
	
	public Appearance upToX(int x) {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX, f.srcY, x * AGame.SGS, f.srcHeight, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}
	
	public Appearance fromX(int x) {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX + x * AGame.SGS, f.srcY, f.srcWidth - x * AGame.SGS, f.srcHeight, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}

	public Appearance leftSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX, f.srcY, f.srcWidth / 2, f.srcHeight, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}

	public Appearance rightSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX + f.srcWidth / 2, f.srcY, f.srcWidth / 2, f.srcHeight, false, 0, 0));
		}
		s.interval = interval;
		return s;
	}
	
	public Appearance topSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX, f.srcY, f.srcWidth, f.srcHeight / 2, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}

	public Appearance bottomSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX, f.srcY + f.srcHeight / 2, f.srcWidth, f.srcHeight / 2, false, 0, 0));
		}
		s.interval = interval;
		return s;
	}
	
	public Appearance topLeftSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX, f.srcY, f.srcWidth / 2, f.srcHeight / 2, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}
	
	public Appearance topRightSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX + f.srcWidth / 2, f.srcY, f.srcWidth / 2, f.srcHeight / 2, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}
	
	public Appearance bottomRightSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX + f.srcWidth / 2, f.srcY + f.srcHeight / 2, f.srcWidth / 2, f.srcHeight / 2, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}
	
	public Appearance bottomLeftSide() {
		Appearance s = new Appearance(spritesheetBundle);
		for (Img f : frames) {
			s.frames.add(new Frame(spritesheetBundle.name, f.srcX, f.srcY + f.srcHeight / 2, f.srcWidth / 2, f.srcHeight / 2, false, 0, 0));
		}
		s.interval = interval;
		s.isFlipped = isFlipped;
		return s;
	}

	public int getInterval() {
		return interval;
	}
	
	public static class FromJSON implements BonusableValue.FromJSON<Appearance> {
		@Override
		public Appearance construct(JSONObject o, BonusSet b) {
			return new Appearance(o);
		}
	}
}