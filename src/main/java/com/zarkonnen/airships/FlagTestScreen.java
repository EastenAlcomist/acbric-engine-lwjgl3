package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.util.ArrayList;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.shader.ShaderProgram;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.TextureImpl;

public class FlagTestScreen implements Screen {
	Img img = new Img("defeat1280.jpg");
	Img map = new Img("defeat_flagmap");
	Img arms = new Img("test_arms");
	static ShaderProgram shader;
	
	boolean init = true;

	@Override
	public void input(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
	}

	@Override
	public void render(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		if (init) {
			d.blit(img, 0, 0);
			d.blit(map, 0, 0);
			d.blit(arms, 0, 0);
			try {
				shader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough3.vert"),
						AGame.getStaticGameDirectoryPath("data/flagmap.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				Runtime.getRuntime().exit(1);
			}
			init = false;
			return;
		}
		shader.bind();
		shader.setUniform4f("tint", new Color(174, 162, 151));
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ((Image) img.machineImgCache).getTexture().getTextureID());
		shader.setUniform1i("tex", 0);
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ((Image) map.machineImgCache).getTexture().getTextureID());
		shader.setUniform1i("map", 1);
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ((Image) arms.machineImgCache).getTexture().getTextureID());
		shader.setUniform1i("arms", 2);
		
		glBegin(GL_QUADS);
		glTexCoord2d(0, 0);
		glVertex2d(0, 0);
		glTexCoord2d(0, 1);
		glVertex2d(0, 720);
		glTexCoord2d(1, 1);
		glVertex2d(1280, 720);
		glTexCoord2d(1, 0);
		glVertex2d(1280, 0);
		glEnd();
		shader.unbind();
		TextureImpl.bindNone();
	}

	@Override
	public ArrayList<String> music() {
		return null;
	}

	@Override
	public String appearancePostfix() {
		return "";
	}

	@Override
	public boolean alwaysUseAppearancePostfix() {
		return false;
	}
}
