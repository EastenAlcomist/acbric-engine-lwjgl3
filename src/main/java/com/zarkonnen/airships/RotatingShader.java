package com.zarkonnen.airships;

import static com.zarkonnen.airships.Appearance.shaderLoadFailed;
import static com.zarkonnen.airships.Appearance.useLighting;
import static com.zarkonnen.airships.Appearance.useSimpleGraphics;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;

public strictfp class RotatingShader {
	private static ShaderProgram lsp;
	public static boolean shaderLoadFailed = false;
	public static boolean shaderLocked = false;
	
	public static void lockShader(SpritesheetBundle ssb, Draw d, Image[] light, float lightStrength, Color ambient, float ambientSaturation) {
		if (ssb.bumpTex == null) {
			light = null;
		}
		if (ssb.getTex(Appearance.currentPostfix) == null) {
			return;
		}
		
		if (shaderLoadFailed || light == null || Appearance.useSimpleGraphics) { return; }
		
		if (lsp == null) {
			try {
				lsp = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/rotated_litfrag.vert"),
						AGame.getStaticGameDirectoryPath("data/rotated_litfrag.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}

		lsp.bind();
		
		lsp.setUniform1f("strength", lightStrength);
		lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
		lsp.setUniform1f("screenHeight", d.frame().mode().height);
		lsp.setUniform4f("ambient", ambient);
		lsp.setUniform1f("ambientSaturation", ambientSaturation);
		lsp.setUniform1f("texSize", ssb.size);

		glActiveTexture(GL_TEXTURE1);
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
		glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(Appearance.currentPostfix).getTextureID());
		lsp.setUniform1i("tex", 0);
		
		lsp.enableVertexAttribute("flipped");
		lsp.enableVertexAttribute("angle");
		lsp.enableVertexAttribute("tint");
		
		glColor3f(1.0f, 1.0f, 1.0f);
		glBegin(GL_QUADS);
		shaderLocked = true;
	}
		
	public static void unlockShader() {
		if (shaderLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			lsp.unbind();
			TextureImpl.bindNone();
			shaderLocked = false;
		}
	}
	
	public static void draw(Img img, Draw d, double x, double y, double angle, double scale, boolean flipped, boolean flipImage, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint) {
		if (!Loadable.hasOfName(SpritesheetBundle.class, img.src)) { return; }
		draw(SpritesheetBundle.ofName(img.src), img, d, x, y, angle, scale, flipped, flipImage, light, lightStrength, ambient, ambientSaturation, ambientTint);
	}
	
	public static void draw(Img img, Draw d, double x, double y, double angle, boolean flipped, boolean flipImage, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint) {
		if (!Loadable.hasOfName(SpritesheetBundle.class, img.src)) { return; }
		draw(SpritesheetBundle.ofName(img.src), img, d, x, y, angle, 1.0, flipped, flipImage, light, lightStrength, ambient, ambientSaturation, ambientTint);
	}

	public static void draw(SpritesheetBundle ssb, Img img, Draw d, double x, double y, double angle, boolean flipped, boolean flipImage, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint) {
		draw(ssb, img, d, x, y, angle, 1.0, flipped, flipImage, light, lightStrength, ambient, ambientSaturation, ambientTint);
	}
	
	public static void draw(SpritesheetBundle ssb, Img img, Draw d, double x, double y, double angle, double scale, boolean flipped, boolean flipImage, Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientTint) {
		if (
				Appearance.shaderLocked ||
				Appearance.subShaderLocked ||
				Appearance.maskedBevelledLocked ||
				//RotatingShader.shaderLocked ||
				RotatingColoringShader.shaderLocked
		) {
			AirshipGame.instance.reportError("RotatingShader.draw mis-lock" +
					" Appearance.shaderLocked=" + Appearance.shaderLocked +
					" Appearance.subShaderLocked=" + Appearance.subShaderLocked +
					" Appearance.bevelledLocked=" + Appearance.maskedBevelledLocked +
					" RotatingShader.shaderLocked=" + RotatingShader.shaderLocked +
					" RotatingColoringShader.shaderLocked=" + RotatingColoringShader.shaderLocked,
					null, null, false, true);
		}
		
		if (ssb.bumpTex == null) {
			light = null;
		}
		
		if (shaderLoadFailed || Appearance.useSimpleGraphics || light == null || ssb.getTex(Appearance.currentPostfix) == null) {
			if (ssb.getTex(Appearance.currentPostfix) != null) {
				img = new Img(img.src + Appearance.currentPostfix, img.srcX, img.srcY, img.srcWidth, img.srcHeight, img.flipped);
			}
			d.blit(img, ambientTint, x, y, img.srcWidth * scale, img.srcHeight * scale, angle);
			if (img.machineImgCache != null) {
				((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			return;
		}

		if (lsp == null) {
			try {
				lsp = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/rotated_litfrag.vert"),
						AGame.getStaticGameDirectoryPath("data/rotated_litfrag.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}

		if (!shaderLocked) {
			lsp.bind();			
			lsp.setUniform1f("strength", lightStrength);
			lsp.setUniform2f("lightSize", light[0].getTexture().getTextureWidth(), light[0].getTexture().getTextureHeight());
			lsp.setUniform1f("screenHeight", d.frame().mode().height);
			lsp.setUniform4f("ambient", ambient);
			lsp.setUniform1f("ambientSaturation", ambientSaturation);
			lsp.setUniform1f("texSize", ssb.size);

			glActiveTexture(GL_TEXTURE1);
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
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(Appearance.currentPostfix).getTextureID());
			lsp.setUniform1i("tex", 0);

			lsp.enableVertexAttribute("flipped");
			lsp.enableVertexAttribute("angle");
			lsp.enableVertexAttribute("tint");

			glColor3f(1.0f, 1.0f, 1.0f);
		}
		
		double w = img.srcWidth * scale;
		double h = img.srcHeight * scale;
		
		GLCompat.glVertexAttrib4f(lsp.getAttributeID("tint"), 1.0f, 1.0f, 1.0f, 1.0f);
		GLCompat.glVertexAttrib1f(lsp.getAttributeID("flipped"), flipped ? (float) 1 : (float) 0);
		GLCompat.glVertexAttrib1f(lsp.getAttributeID("angle"), (float) angle);

		if (!shaderLocked) {
			glBegin(GL_QUADS);
		}

		/*d.shift(x + w * 0.5, y + h * 0.5);
		d.rotate(angle * 180 / StrictMath.PI);
		d.shift(-w * 0.5, -h * 0.5);

		-- Draw here.

		d.shift(w * 0.5, h * 0.5);
		d.rotate(-angle * 180 / StrictMath.PI);
		d.shift(-x - w * 0.5, -y - h * 0.5);*/

		double cos = StrictMath.cos(angle);
		double sin = StrictMath.sin(angle);
		double w2 = w * 0.5;
		double h2 = h * 0.5;
		double x2 = x + w2;
		double y2 = y + h2;

		// position is really -w2 and -h2 here, etc.
		double topLeftX = x2 - w2 * cos + h2 * sin;
		double topLeftY = y2 - w2 * sin - h2 * cos;

		double bottomLeftX = x2 - w2 * cos - h2 * sin;
		double bottomLeftY = y2 - w2 * sin + h2 * cos;

		double bottomRightX = x2 + w2 * cos - h2 * sin;
		double bottomRightY = y2 + w2 * sin + h2 * cos;

		double topRightX = x2 + w2 * cos + h2 * sin;
		double topRightY = y2 + w2 * sin - h2 * cos;

		if (flipImage ^ img.flipped) {
			glTexCoord2d(img.srcX, img.srcY);
			glVertex2d(topRightX, topRightY);
			glTexCoord2d(img.srcX, img.srcY + img.srcHeight);
			glVertex2d(bottomRightX, bottomRightY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY + img.srcHeight);
			glVertex2d(bottomLeftX, bottomLeftY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY);
			glVertex2d(topLeftX, topLeftY);
		} else {
			glTexCoord2d(img.srcX, img.srcY);
			glVertex2d(topLeftX, topLeftY);
			glTexCoord2d(img.srcX, img.srcY + img.srcHeight);
			glVertex2d(bottomLeftX, bottomLeftY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY + img.srcHeight);
			glVertex2d(bottomRightX, bottomRightY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY);
			glVertex2d(topRightX, topRightY);
		}

		if (!shaderLocked) {
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			lsp.unbind();
			TextureImpl.bindNone();
		}
	}
	
	public static void drawAsRedOutline(SpritesheetBundle ssb, Img img, Draw d, double x, double y, double angle, double scale, boolean flipped) {
		if (Appearance.shaderLoadFailed || Appearance.useSimpleGraphics || !Appearance.useLighting) {
			d.blit(img, Clr.RED, x, y, img.srcWidth * scale, img.srcHeight * scale, angle);
			return;
		}
		
		if (Appearance.redSolidShader == null) {
			try {
				Appearance.redSolidShader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
						AGame.getStaticGameDirectoryPath("data/redoutline.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				shaderLoadFailed = true;
				return;
			}
		}

		Appearance.redSolidShader.bind();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex(Appearance.currentPostfix).getTextureID());
		Appearance.redSolidShader.setUniform1i("tex", 0);
		
		double w = img.srcWidth * scale;
		double h = img.srcHeight * scale;
		
		glBegin(GL_QUADS);

		double cos = StrictMath.cos(angle);
		double sin = StrictMath.sin(angle);
		double w2 = w * 0.5;
		double h2 = h * 0.5;
		double x2 = x + w2;
		double y2 = y + h2;

		// position is really -w2 and -h2 here, etc.
		double topLeftX = x2 - w2 * cos + h2 * sin;
		double topLeftY = y2 - w2 * sin - h2 * cos;

		double bottomLeftX = x2 - w2 * cos - h2 * sin;
		double bottomLeftY = y2 - w2 * sin + h2 * cos;

		double bottomRightX = x2 + w2 * cos - h2 * sin;
		double bottomRightY = y2 + w2 * sin + h2 * cos;

		double topRightX = x2 + w2 * cos + h2 * sin;
		double topRightY = y2 + w2 * sin - h2 * cos;

		if (/*flipped ^ */img.flipped) {
			glTexCoord2d(img.srcX, img.srcY);
			glVertex2d(topRightX, topRightY);
			glTexCoord2d(img.srcX, img.srcY + img.srcHeight);
			glVertex2d(bottomRightX, bottomRightY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY + img.srcHeight);
			glVertex2d(bottomLeftX, bottomLeftY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY);
			glVertex2d(topLeftX, topLeftY);
		} else {
			glTexCoord2d(img.srcX, img.srcY);
			glVertex2d(topLeftX, topLeftY);
			glTexCoord2d(img.srcX, img.srcY + img.srcHeight);
			glVertex2d(bottomLeftX, bottomLeftY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY + img.srcHeight);
			glVertex2d(bottomRightX, bottomRightY);
			glTexCoord2d(img.srcX + img.srcWidth, img.srcY);
			glVertex2d(topRightX, topRightY);
		}

		glEnd();
		glColor3f(1.0f, 1.0f, 1.0f);
		Appearance.redSolidShader.unbind();
		TextureImpl.bindNone();
	}
}
