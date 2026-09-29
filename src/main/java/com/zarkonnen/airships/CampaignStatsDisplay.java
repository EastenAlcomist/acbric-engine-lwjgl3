package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.airships.CampaignWorld.Speed;
import com.zarkonnen.airships.HistoryStats.Emp;
import com.zarkonnen.airships.HistoryStats.Emp.Record;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.ShapeUtils.Area;
import com.zarkonnen.airships.ShapeUtils.P;
import static com.zarkonnen.airships.WorldMap.MS_PER_DAY;
import com.zarkonnen.catengine.Hook;

import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import javax.imageio.ImageIO;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.opengl.TextureImpl;

public class CampaignStatsDisplay implements ScrollBar.ScrollElementAdapter<Emp> {
	public Mode mode = Mode.TERRITORY;
	public HashSet<Integer> disabledEmpireIds = new HashSet<Integer>();
	public ScrollBar empsSB = new ScrollBar();
	public IntRect empsSBR = new IntRect();
	public Emp hoverEmp;
	public Emp newHoverEmp;
	public int replayTime = 0;
	public boolean replaying = false;
	public BufferedImage hoverLookup;
	public int graphX, graphY;
	public Mode hoverLookupRenderedMode = null;

	@Override
	public int getHeight(Emp t, MyDraw d, int availableWidth) {
		return Math.max(32, MyDraw.BUTTON_H) + MyDraw.SCROLL_EL_SPACING;
	}

	@Override
	public void draw(final Emp t, MyDraw d, int x, int y, int width) {
		Record r = t.records.get(t.records.size() - 1);
		Color c = r.arms.getMixedColor();
		d.rect(new Clr(c.getRed(), c.getGreen(), c.getBlue()), x, y, 6, 32);
		if (t == hoverEmp) {
			d.rect(MyDraw.SELECTED, x + MyDraw.BUTTON_SPACING + 6 - 1, y - 1, 34, 34);
		}
		r.arms.draw(d, x + MyDraw.BUTTON_SPACING + 6, y, 32);
		d.toggle(x + 32 + MyDraw.BUTTON_SPACING + 6 + MyDraw.BUTTON_SPACING, y, width - 32 - MyDraw.BUTTON_SPACING * 2 - 6, r.name, null, new InputRunnable() {
			@Override
			public void run(Input in) {
				if (disabledEmpireIds.contains(t.id)) {
					disabledEmpireIds.remove(t.id);
				} else {
					disabledEmpireIds.add(t.id);
				}
				hoverLookupRenderedMode = null;
			}
		}, !disabledEmpireIds.contains(t.id), true);
		d.hook(x, y, width, Math.max(32, MyDraw.BUTTON_H), new Hook(Hook.Type.HOVER) {
			@Override
			public void run(Input input, Pt pt, Hook.Type type) {
				newHoverEmp = t;
			}
		});
	}
	
	public enum Mode {
		TERRITORY, FLEET_STRENGTH, INCOME, MONEY, REP, TECH, DEATHS, REPLAY
	}
	
	public int lineW(Emp emp) {
		switch (AirshipGame.instance.currentGUIScale) {
			case LARGE:
				return emp == hoverEmp ? 6 : 3;
			case MEDIUM:
				return emp == hoverEmp ? 4 : 2;
			case SMALL:
				return emp == hoverEmp ? 2 : 1;
		}
		return 2;
	}
	
	public void render(WorldMap map, HistoryStats stats, BonusSet bonuses, MyDraw d, int x, int y, int w, int h) {
		for (ShapeUtils.Area area : map.cityOwnershipAreas) {
			City city = map.getCity(area.identifier);
			if (city.territoryPolygon == null) {
				city.territoryPolygon = new ShapeUtils.TrianglesArea(area);
			}
		}
		
		int modesW = 0;
		for (Mode m : Mode.values()) {
			modesW = Math.max(modesW, d.tw(_t("stats_" + m.name())));
		}
		int empiresW = 0;
		for (Emp e : stats.empires) {
			String name = e.records.get(e.records.size() - 1).name;
			empiresW = Math.max(empiresW, d.tw(name.substring(0, Math.min(20, name.length()))) + 32 + MyDraw.BUTTON_SPACING * 2 + 6);
		}
		
		int y2 = y;
		for (final Mode m : Mode.values()) {
			d.toggle(x, y2, modesW, _t("stats_" + m.name()), "" + (m.ordinal() + 1), new InputRunnable() {
				@Override
				public void run(Input in) {
					mode = m;
				}
			}, m == mode, m != Mode.REP || map.toggles.contains(ConquestToggle.REPUTATION));
			y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		}
		
		x += modesW + MyDraw.UI_SPACING;
		int graphW = w - modesW - MyDraw.UI_SPACING * 2 - MyDraw.PANEL_INSET * 2 - ScrollBar.SCROLL_BAR_W - MyDraw.PANEL_INSET - empiresW;
		int graphH = h - MyDraw.PANEL_INSET * 2;
		if (mode == Mode.REPLAY) {
			int xAfterwards = x + MyDraw.PANEL_INSET + graphW + MyDraw.PANEL_INSET + MyDraw.UI_SPACING;
			int mapGraphicalSize = Math.min(graphW, graphH - MyDraw.ICON_BUTTON_SZ - MyDraw.BUTTON_SPACING);
			//x += (graphW - mapGraphicalSize - MyDraw.PANEL_INSET * 2) / 2;
			d.drawPanel(x, y, mapGraphicalSize + MyDraw.PANEL_INSET * 2, mapGraphicalSize + MyDraw.PANEL_INSET * 2, mode.ordinal());
			
			d.rect(StrategicScreen.PARCHMENT, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, mapGraphicalSize, mapGraphicalSize);
		
			d.shift(x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET);
			int mapSize = map.water.length;
			d.scale(mapGraphicalSize * 1.0 / mapSize, mapGraphicalSize * 1.0 / mapSize);

			TextureImpl.bindNone();
			glBegin(GLCompat.GL_TRIANGLES);
			for (Emp ee : stats.empires) {
				Record rec = ee.getRecordForAge(replayTime);
				if (rec == null) { continue; }
				for (City city : rec.territory) {
					Color c = rec.arms.getMixedColor();
					glColor4f(c.r, c.g, c.b, ee == hoverEmp ? 1f : 0.66f);
					city.territoryPolygon.draw();
				}
			}
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			TextureImpl.bindNone();

			d.resetTransforms();

			Graphics g = (Graphics) d.frame().nativeRenderer();

			g.setLineWidth(2);
			g.setColor(StrategicScreen.INK_C);

			for (Area area : map.landBoundaries) {
				for (int i = 0; i < area.points.size(); i++) {
					P a = area.points.get(i);
					P b = area.points.get((i + 1) % area.points.size());

					if (a.x < StrategicScreen.TOO_CLOSE || a.x > mapSize - StrategicScreen.TOO_CLOSE || a.y < StrategicScreen.TOO_CLOSE || a.y > mapSize - StrategicScreen.TOO_CLOSE) {
						continue;
					}
					if (b.x < StrategicScreen.TOO_CLOSE || b.x > mapSize - StrategicScreen.TOO_CLOSE || b.y < StrategicScreen.TOO_CLOSE || b.y > mapSize - StrategicScreen.TOO_CLOSE) {
						continue;
					}

					double ax = a.x * mapGraphicalSize / mapSize + x + MyDraw.PANEL_INSET;
					double ay = a.y * mapGraphicalSize / mapSize + y + MyDraw.PANEL_INSET;
					double bx = b.x * mapGraphicalSize / mapSize + x + MyDraw.PANEL_INSET;
					double by = b.y * mapGraphicalSize / mapSize + y + MyDraw.PANEL_INSET;
					g.drawLine((float) ax, (float) ay, (float) bx, (float) by);
				}
			}

			for (Emp ee : stats.empires) {
				Record rec = ee.getRecordForAge(replayTime);
				if (rec == null) { continue; }
				for (City city : rec.territory) {
					double cx = city.x * mapGraphicalSize / mapSize + x + MyDraw.PANEL_INSET;
					double cy = city.y * mapGraphicalSize / mapSize + y + MyDraw.PANEL_INSET;
					d.rect(StrategicScreen.INK, (int) cx - 9, (int) cy - 9, 18, 18);
					rec.arms.draw(d, (int) cx - 8, (int) cy - 8, 16);
					d.tooltip( (int) cx - 8, (int) cy - 8, 16, 16, city.name + " (" + rec.name + ")");
				}
			}
			
			final int maxAge = stats.maxAge();
			y2 = y + mapGraphicalSize + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			d.iconButton(x, y2, replaying ? Speed.STOP.icon : Speed.NORMAL.icon, new Runnable() {
				@Override
				public void run() {
					if (replaying) {
						replaying = false;
					} else {
						replaying = true;
						if (replayTime == maxAge) {
							replayTime = 0;
						}
					}
				}
			}, true);
			int x2 = x + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			final int barW = mapGraphicalSize + MyDraw.PANEL_INSET * 2 - MyDraw.ICON_BUTTON_SZ - MyDraw.BUTTON_SPACING;
			d.progressBar(x2, y2, barW, MyDraw.ICON_BUTTON_SZ, replayTime * 1.0 / maxAge);
			d.hook(x2, y2, barW, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input input, Pt pt, Hook.Type type) {
					replayTime = (int) (maxAge * pt.x / barW);
				}
			});
			x = xAfterwards;
		} else {
			d.drawPanel(x, y, graphW + MyDraw.PANEL_INSET * 2, h, mode.ordinal());
			x += MyDraw.PANEL_INSET;
			y2 = y + MyDraw.PANEL_INSET;
			d.rect(Clr.BLACK, x, y2, graphW, graphH);
			graphX = x;
			graphY = y2;
			if (hoverLookup == null || hoverLookup.getWidth() != graphW || hoverLookup.getHeight() != graphH) {
				hoverLookup = new BufferedImage(graphW, graphH, BufferedImage.TYPE_INT_RGB);
				hoverLookupRenderedMode = null;
			}
			Graphics2D hg = hoverLookupRenderedMode != mode ? hoverLookup.createGraphics() : null;
			hoverLookupRenderedMode = mode;
			if (hg != null) {
				hg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				hg.setColor(java.awt.Color.BLACK);
				hg.fillRect(0, 0, graphW, graphH);
			}
			int msPerMonth = MS_PER_DAY * EmpireStat.DAYS_PER_MONTH.get(bonuses);
			int msPerYear = msPerMonth * EmpireStat.MONTHS.get(bonuses).size();
			int maxAge = stats.maxAge();
			int minYear = (EmpireStat.STARTING_DAY.get(bonuses) * MS_PER_DAY) / msPerYear;
			int maxYear = (maxAge + EmpireStat.STARTING_DAY.get(bonuses) * MS_PER_DAY) / msPerYear + 1;
			maxAge = (maxAge / msPerYear + 1) * msPerYear;
			int yearIncSize = indicatorIncrement(maxYear - minYear, 5);
			int firstYearStopMapAge = minYear * msPerYear - EmpireStat.STARTING_DAY.get(bonuses) * MS_PER_DAY;
			int maxValue = 1;
			int minValue = 0;
			for (Emp e : stats.empires) {
				if (disabledEmpireIds.contains(e.id)) { continue; }
				for (Record r : e.records) {
					maxValue = Math.max(maxValue, r.get(mode));
					minValue = Math.min(minValue, r.get(mode));
				}
			}
			int incSize = indicatorIncrement(maxValue - minValue, 5);
			int max = 0;
			while (max < maxValue * 1.1) {
				max += incSize;
			}
			int incNumber = 0;
			int min = 0;
			while (min > minValue * 1.1) {
				min -= incSize;
				incNumber--;
			}
			Graphics g = (Graphics) d.frame().nativeRenderer();
			for (Emp e : stats.empires) {
				if (disabledEmpireIds.contains(e.id)) { continue; }
				g.setLineWidth(lineW(e));
				Color c = e.records.get(e.records.size() - 1).arms.getMixedColor();
				g.setColor(c);
				if (hg != null) {
					hg.setStroke(new BasicStroke(lineW(e) * 2 + 2));
					hg.setColor(new java.awt.Color(e.id + 1, e.id + 1, e.id + 1));
				}
				for (int i = 0; i < e.records.size() - 1; i++) {
					Record a = e.records.get(i);
					Record b = e.records.get(i + 1);
					int xa = (int) (x + ((long) a.mapAge * graphW) / maxAge);
					int xb = (int) (x + ((long) b.mapAge * graphW) / maxAge);
					int ya = (int) (y2 + graphH - ((long) a.get(mode) - min) * graphH / (max - min));
					int yb = (int) (y2 + graphH - ((long) b.get(mode) - min) * graphH / (max - min));
					g.drawLine(xa, ya, xb, yb);
					if (hg != null) { hg.drawLine(xa - x, ya - y2, xb - x, yb - y2); }
				}
			}
			g.setLineWidth(1);
			if (hg != null) { hg.setStroke(new BasicStroke(1)); }
			
			for (Emp e : stats.empires) {
				if (disabledEmpireIds.contains(e.id)) { continue; }
				Record rec = e.records.get(e.records.size() - 1);
				if (rec.mergedInto != -1) {
					for (Emp me : stats.empires) {
						if (me.id == rec.mergedInto) {
							Record mergedInto = me.records.get(me.records.size() - 1);
							int mx = (int) (x + ((long) rec.mapAge * graphW) / maxAge);
							int my = (int) (y2 + graphH - ((long) rec.get(mode) - min) * graphH / (max - min));
							g.setColor(mergedInto.arms.getMixedColor());
							int lw = lineW(e) * 2;
							g.fillOval(mx - lw, my - lw, lw * 2, lw * 2);
							d.tooltip(mx - lw, my - lw, lw * 2, lw * 2, _t("allied_with_x", mergedInto.name));
							if (hg != null) {
								hg.setColor(new java.awt.Color(me.id + 1, me.id + 1, me.id + 1));
								hg.fillOval(mx - x - lw - 2, my - y2 - lw - 2, lw * 2 + 4, lw * 2 + 4);
							}
						}
					}
				}
			}

			while (true) {
				int notchY = (int) (y2 + graphH - (((long) incSize * incNumber - min) * graphH / (max - min)) - 1);
				if (notchY > y2 + graphH) { incNumber++; continue; }
				if (notchY < y2) { break; }
				d.rect(Clr.WHITE, x, notchY, 9, 3);
				d.text("" + (incSize * incNumber), AGame.FOUNT, x + MyDraw.BUTTON_SPACING, notchY - AGame.FOUNT.lineHeight);
				incNumber++;
			}

			incNumber = 0;
			while (true) {
				int notchX = (int) (x + ((firstYearStopMapAge + (long) incNumber * yearIncSize * msPerYear) * graphW) / maxAge);
				if (notchX < x) { incNumber++; continue; }
				if (notchX + d.textSize("" + (yearIncSize * incNumber + minYear), AGame.FOUNT).x + 1 > x + graphW) { break; }
				d.rect(Clr.WHITE, notchX - 1, y2 + graphH - 9, 3, 9);
				d.text("" + (yearIncSize * incNumber + minYear), AGame.FOUNT, notchX, y2 + graphH - 9 - AGame.FOUNT.lineHeight);
				incNumber++;
			}
			
			if (hoverEmp != null) {
				Record r = hoverEmp.records.get(hoverEmp.records.size() - 1);
				d.text(MyDraw.SELECTED_C + r.name, AGame.FOUNT, x + graphW / 2 - (int) d.textSize(r.name, AGame.FOUNT).x / 2, y2 + MyDraw.UI_SPACING);
			}

			x += graphW + MyDraw.PANEL_INSET + MyDraw.UI_SPACING;
		}
		empsSBR.x = x;
		empsSBR.y = y;
		empsSBR.w = empiresW + MyDraw.PANEL_INSET + ScrollBar.SCROLL_BAR_W;
		empsSBR.h = h;
		empsSB.draw(d, x, y, empsSBR.w, h, stats.empires, this);
	}
	
	public void input(HistoryStats stats, Input in) {
		if (mode != Mode.REPLAY) {
			empsSB.tick(in, empsSBR.x, empsSBR.y, empsSBR.w, empsSBR.h);
		}
		if (in.cursor() != null && hoverLookup != null) {
			int gcx = (int) in.cursor().x - graphX;
			int gcy = (int) in.cursor().y - graphY;
			if (gcx >= 0 && gcy >= 0 && gcx < hoverLookup.getWidth() && gcy < hoverLookup.getHeight()) {
				java.awt.Color c = new java.awt.Color(hoverLookup.getRGB(gcx, gcy));
				int id = c.getRed() - 1;
				for (Emp e : stats.empires) {
					if (e.id == id) {
						newHoverEmp = e;
						break;
					}
				}
			}
		}
		hoverEmp = newHoverEmp;
		newHoverEmp = null;
		if (mode == Mode.REPLAY && in.keyPressed("SPACE")) {
			replaying = !replaying;
		}
		for (Mode m : Mode.values()) {
			if (in.keyPressed("" + (m.ordinal() + 1))) {
				mode = m;
				replaying = false;
			}
		}
		if (replaying) {
			replayTime += in.msDelta() * 30;
			if (replayTime >= stats.maxAge()) {
				replayTime = stats.maxAge();
				replaying = false;
			}
		}
	}
	
	public static int indicatorIncrement(int max, int targetNumberOfIncrements) {
		int increment = 1;
		while (max > increment * targetNumberOfIncrements * 10) {
			increment *= 10;
		}
		while (max > increment * targetNumberOfIncrements * 1.4) {
			increment *= 2;
		}
		return increment;
	}
}
