package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.airships.ShapeUtils.P;
import static com.zarkonnen.airships.StrategicScreen.INK_C;
import static com.zarkonnen.airships.StrategicScreen.TOO_CLOSE;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Rect;
import com.zarkonnen.catengine.util.ScreenMode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.opengl.TextureImpl;

public class SaveHelperWidget implements FileScreen.HelperWidget {
	private SavePreviewInfo currentInfo;
	private List<String> currentPath;
	
	public FileScreen.Backend backend;

	public SaveHelperWidget(FileScreen.Backend backend) {
		this.backend = backend;
	}

	@Override
	public void setSelectedFile(List<String> path) {
		if (path == null || (!path.isEmpty() && path.get(path.size() - 1).equals(".."))) {
			currentPath = null;
			currentInfo = null;
		} else if (!path.equals(currentPath)) {
			currentPath = path;
			currentInfo = null;
			File f = backend.getRawFile(path);
			if (f.isDirectory()) {
				try {
					IODirectory iod = new IODirectory(f, null);
					currentInfo = new SavePreviewInfo(iod.read("savePreviewFixed"), iod.read("savePreviewVariable"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void clearCacheFor(List<String> path) {
		if (path.equals(currentPath)) { currentPath = null; }
	}

	@Override
	public void tick(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms, ArrayList<String> dirPath) {}

	@Override
	public void render(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor, int x, int y, int w, int h) {
		if (currentInfo == null) { return; }
		String empireName = currentInfo.player == null ? "Spectator" : currentInfo.player.name;
		Rect nameSz = d.textSize(empireName, AGame.BIGGER_FOUNT, 0, 0, w - CoatOfArms.SHIELD_W - MyDraw.UI_SPACING);
		int sz = Math.min(w, h - Math.max(CoatOfArms.SHIELD_H, (int) nameSz.height) - MyDraw.UI_SPACING) - MyDraw.WINDOW_INSET * 2;
		d.drawWindow(x, y, sz + MyDraw.WINDOW_INSET * 2, sz + MyDraw.WINDOW_INSET * 2, 8);
		d.rect(StrategicScreen.PARCHMENT, x + MyDraw.WINDOW_INSET, y + MyDraw.WINDOW_INSET, sz, sz);
		
		d.shift(x + MyDraw.WINDOW_INSET, y + MyDraw.WINDOW_INSET);
		d.scale(sz * 1.0 / currentInfo.mapSize, sz * 1.0 / currentInfo.mapSize);
		
		TextureImpl.bindNone();
		glBegin(GLCompat.GL_TRIANGLES);
		for (SavePreviewInfo.EmpireEntry ee : currentInfo.empires) {
			for (SavePreviewInfo.Area area : ee.cities) {
				if (area.polygon == null) {
					area.polygon = new ShapeUtils.TrianglesArea(area);
				}
				Color c = ee.c;
				glColor4f(c.r, c.g, c.b, ee == currentInfo.player ? 1f : 0.33f);
				area.polygon.draw();
			}
		}
		glEnd();
		glColor3f(1.0f, 1.0f, 1.0f);
		TextureImpl.bindNone();
		
		d.resetTransforms();
		
		Graphics g = (Graphics) d.frame().nativeRenderer();
		
		g.setLineWidth(2);
		g.setColor(INK_C);
		
		for (SavePreviewInfo.Area area : currentInfo.land) {
			for (int i = 0; i < area.points.size(); i++) {
				P a = area.points.get(i);
				P b = area.points.get((i + 1) % area.points.size());
				
				if (a.x < TOO_CLOSE || a.x > currentInfo.mapSize - TOO_CLOSE || a.y < TOO_CLOSE || a.y > currentInfo.mapSize - TOO_CLOSE) {
					continue;
				}
				if (b.x < TOO_CLOSE || b.x > currentInfo.mapSize - TOO_CLOSE || b.y < TOO_CLOSE || b.y > currentInfo.mapSize - TOO_CLOSE) {
					continue;
				}
				
				double ax = a.x * sz / currentInfo.mapSize + x + MyDraw.WINDOW_INSET;
				double ay = a.y * sz / currentInfo.mapSize + y + MyDraw.WINDOW_INSET;
				double bx = b.x * sz / currentInfo.mapSize + x + MyDraw.WINDOW_INSET;
				double by = b.y * sz / currentInfo.mapSize + y + MyDraw.WINDOW_INSET;
				g.drawLine((float) ax, (float) ay, (float) bx, (float) by);
			}
		}
		
		for (SavePreviewInfo.EmpireEntry ee : currentInfo.empires) {
			CoatOfArms coa = new CoatOfArms(ee.coa);
			for (SavePreviewInfo.Area area : ee.cities) {
				/*double cx = 0;
				double cy = 0;
				for (ShapeUtils.P p : area.points) {
					cx += p.x;
					cy += p.y;
				}
				cx /= area.points.size();
				cy /= area.points.size();*/
				double cx = area.cx * sz / currentInfo.mapSize + x + MyDraw.WINDOW_INSET;
				double cy = area.cy * sz / currentInfo.mapSize + y + MyDraw.WINDOW_INSET;
				d.rect(StrategicScreen.INK, (int) cx - 9, (int) cy - 9, 18, 18);
				coa.draw(d, (int) cx - 8, (int) cy - 8, 16);
			}
		}
		
		CoatOfArms coa = currentInfo.player == null ? CoatOfArms.spectatorArms() : new CoatOfArms(currentInfo.player.coa);
		coa.layout.drawShield(coa, d, x, y + sz + MyDraw.WINDOW_INSET * 2 + MyDraw.UI_SPACING, 1, MyDraw.SELECTED);
		d.text(MyDraw.TITLE_C + empireName, AGame.BIGGER_FOUNT, x + CoatOfArms.SHIELD_W + MyDraw.UI_SPACING, y + sz + MyDraw.WINDOW_INSET * 2 + MyDraw.UI_SPACING);
	}

	@Override
	public void close() {}

	@Override
	public boolean blocking() {
		return false;
	}

	@Override
	public boolean popUpdateNeeded() {
		return false;
	}

	@Override
	public List<FileScreen.Button> getButtons() {
		return Collections.emptyList();
	}
	
}
