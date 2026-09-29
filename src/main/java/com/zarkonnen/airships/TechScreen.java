package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.util.ArrayList;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.util.Clr;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;

public strictfp class TechScreen implements Screen, CanMoveAround {
	public static int TECH_ICON_SIZE = 64;
	
	public static int TECH_TIER_GAP_ZOOMED_OUT = 64;
	public static int SMALL_TECH_TIER_GAP_ZOOMED_OUT = 64;
	public static int MEDIUM_TECH_TIER_GAP_ZOOMED_OUT = 64;
	public static int LARGE_TECH_TIER_GAP_ZOOMED_OUT = 64;
	
	public static int TECH_TIER_GAP = 128;
	public static int SMALL_TECH_TIER_GAP = 128;
	public static int MEDIUM_TECH_TIER_GAP = 128;
	public static int LARGE_TECH_TIER_GAP = 128;
	
	public static int TECH_ROW_GAP = 32;
	public static int SMALL_TECH_ROW_GAP = 32;
	public static int MEDIUM_TECH_ROW_GAP = 32;
	public static int LARGE_TECH_ROW_GAP = 32;
	
	public static int TECH_LANE_DIST = 12;
	public static int SMALL_TECH_LANE_DIST = 12;
	public static int MEDIUM_TECH_LANE_DIST = 12;
	public static int LARGE_TECH_LANE_DIST = 12;
	
	public static Img TECH_ICON = new Img("ui",  480, 480, 32, 32, false);
	public static Img TECH_ICON_ACTIVE = new Img("ui",  512, 480, 32, 32, false);
	public static Clr SELECTED_TECH = Clr.fromHex("f5edb5");
	public static String SELECTED_TECH_C = "[f5edb5]";
	public static Clr AVAILABLE_TECH = Clr.fromHex("f5e474");
	public static Clr UNAVAILABLE_TECH = Clr.fromHex("c8b84f");
	public static String UNAVAILABLE_TECH_C = "[c8b84f]";
	public static Img ZOOM_IN = new Img("ui", 384, 512, 16, 16, false);
	public static Img ZOOM_OUT = new Img("ui", 384 + 16, 512, 16, 16, false);
	
	private boolean dragging = false;
	private int dragStartX, dragStartY;
	private int dragStartScrollX, dragStartScrollY;
	
	private final int[] tierWidths = new int[Tech.layout.length];
	private final int[] rowHeights = new int[Tech.layout[0].length];
	private int totalWidth, totalHeight;
	private int smWidth, smHeight;
	private int sidePanelW = 0;
	
	public boolean zoomedOut;
	
	private Tech hoverTech;
	private Tech focusOn;
	
	public final StrategicScreen ss;
	
	public static ShaderProgram reToneShader;
	
	private int techTierGap() {
		return zoomedOut ? TECH_TIER_GAP_ZOOMED_OUT : TECH_TIER_GAP;
	}

	public TechScreen(StrategicScreen ss) {
		this.ss = ss;
	}
	
	private void focusOnCenter() {
		Empire e = ss.w.player;	
		ArrayList<Tech> techs = Loadable.all(Tech.class);
		focusOn = null;
		int bestDSq = 0;
		for (Tech t : techs) {
			if (!t.visible(e)) { continue; }
			int tx = t.x + t.w / 2 + ss.techScreenScrollX;
			int ty = t.y + t.h / 2 + ss.techScreenScrollY;
			int centerX = (smWidth - sidePanelW) / 2;
			int centerY = (smHeight) / 2;
			int dSq = (tx - centerX) * (tx - centerX) + (ty - centerY) * (ty - centerY);
			if (focusOn == null || dSq < bestDSq) {
				focusOn = t;
				bestDSq = dSq;
			}
		}
	}
	
	private void zoomOut() {
		if (!zoomedOut) {
			zoomedOut = true;
			focusOnCenter();
			totalWidth = 0;
		}
	}
	
	private void zoomIn() {
		if (zoomedOut) {
			zoomedOut = false;
			focusOnCenter();
			totalWidth = 0;
		}
	}
	
	private transient int moveAroundDx, moveAroundDy;
	
	@Override
	public void moveAround(int dx, int dy) {
		moveAroundDx = dx;
		moveAroundDy = dy;
	}
	
	@Override
	public void input(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
		Pt mouseDown = in.mouseDown();
		if (mouseDown != null && in.mouseDownButton() > 1) {
			if (dragging) {
				ss.techScreenScrollX = (int) (dragStartScrollX + (mouseDown.x - dragStartX));
				ss.techScreenScrollY = (int) (dragStartScrollY + (mouseDown.y - dragStartY));
			} else {
				dragStartX = (int) mouseDown.x;
				dragStartY = (int) mouseDown.y;
				dragStartScrollX = ss.techScreenScrollX;
				dragStartScrollY = ss.techScreenScrollY;
				dragging = true;
			}
		} else {
			dragging = false;
		}
		
		if (SimplePref.SIDE_BUMP_TO_SCROLL.get()) {
			if (moveAroundDy < 0 || in.keyDown("UP") || Keys.checkDown(in, "up", "W", false) || cursor.y < 2) { ss.techScreenScrollY += AirshipGame.scrollSpeed; }
			if (moveAroundDy > 0 || in.keyDown("DOWN") || Keys.checkDown(in, "down", "S", false) || cursor.y > in.mode().height - 2) { ss.techScreenScrollY -= AirshipGame.scrollSpeed; }
			if (moveAroundDx < 0 || in.keyDown("LEFT") || Keys.checkDown(in, "left", "A", false) || cursor.x < 2) { ss.techScreenScrollX += AirshipGame.scrollSpeed; }
			if (moveAroundDx > 0 || in.keyDown("RIGHT") || Keys.checkDown(in, "right", "D", false) || cursor.x > in.mode().width - 2) { ss.techScreenScrollX -= AirshipGame.scrollSpeed; }
		} else {
			if (moveAroundDy < 0 || in.keyDown("UP") || Keys.checkDown(in, "up", "W", false)) { ss.techScreenScrollY += AirshipGame.scrollSpeed; }
			if (moveAroundDy > 0 || in.keyDown("DOWN") || Keys.checkDown(in, "down", "S", false)) { ss.techScreenScrollY -= AirshipGame.scrollSpeed; }
			if (moveAroundDx < 0 || in.keyDown("LEFT") || Keys.checkDown(in, "left", "A", false)) { ss.techScreenScrollX += AirshipGame.scrollSpeed; }
			if (moveAroundDx > 0 || in.keyDown("RIGHT") || Keys.checkDown(in, "right", "D", false)) { ss.techScreenScrollX -= AirshipGame.scrollSpeed; }
		}
		
		if (Keys.check(in, "zoom_out", "SUBTRACT", false) || in.scrollAmount() < 0) { zoomOut(); }
		if (Keys.check(in, "zoom_in", "ADD", false) || in.scrollAmount() > 0) { zoomIn(); }
		
		int minScrollX = StrictMath.min(MyDraw.SIDE_CLEARANCE, -totalWidth - MyDraw.SIDE_CLEARANCE + smWidth - sidePanelW - MyDraw.SIDE_CLEARANCE - MyDraw.WINDOW_INSET);
		ss.techScreenScrollX = StrictMath.max(minScrollX, StrictMath.min(MyDraw.SIDE_CLEARANCE, ss.techScreenScrollX));
		int minScrollY = StrictMath.min(MyDraw.SIDE_CLEARANCE, -totalHeight - MyDraw.SIDE_CLEARANCE + smHeight);
		ss.techScreenScrollY = StrictMath.max(minScrollY, StrictMath.min(MyDraw.UI_SPACING + MyDraw.TOP_BAR_H, ss.techScreenScrollY));
		
		if (Keys.check(in, "ESCAPE") || Keys.check(in, "leave", "L", false)) {
			ss.g.s = ss;
		}
		
		ss.backgroundTick(ms);
	}
	
	public static void loadShader() {
		if (reToneShader == null && !Appearance.shaderLoadFailed && !Appearance.useSimpleGraphics) {
			try {
				reToneShader = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/passthrough.vert"),
						AGame.getStaticGameDirectoryPath("data/retone.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				Appearance.shaderLoadFailed = true;
			}
		}
	}
	
	public static void reToneBlit(MyDraw d, int x, int y, Img img, Clr dark, Clr light) {
		loadShader();
		
		if (reToneShader == null || img.machineImgCache == null || Appearance.shaderLoadFailed) {
			d.blit(img, x, y);
			return;
		}
		reToneShader.bind();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ((Image) img.machineImgCache).getTexture().getTextureID());
		reToneShader.setUniform1i("tex", 0);
		reToneShader.setUniform1f("texSize", ((Image) img.machineImgCache).getTexture().getTextureHeight());
		reToneShader.setUniform3f("darkest", 20 / 255.0f, 20 / 255.0f, 20 / 255.0f);
		reToneShader.setUniform3f("dark", dark.r / 255.0f, dark.g / 255.0f, dark.b / 255.0f);
		reToneShader.setUniform3f("light", light.r / 255.0f, light.g / 255.0f, light.b / 255.0f);
		glBegin(GL_QUADS);
		glTexCoord2d(img.srcX, img.srcY);
		glVertex2d(x, y);
		glTexCoord2d(img.srcX, img.srcY + img.srcHeight);
		glVertex2d(x, y + img.srcHeight);
		glTexCoord2d(img.srcX + img.srcWidth, img.srcY + img.srcHeight);
		glVertex2d(x + img.srcWidth, y + img.srcHeight);
		glTexCoord2d(img.srcX + img.srcWidth, img.srcY);
		glVertex2d(x + img.srcWidth, y);
		//System.out.println(x + ", " + y + ", " + img.srcX + ", " + img.srcY + ", " + img.srcWidth + ", " + img.srcHeight);
		glEnd();
		reToneShader.unbind();
		TextureImpl.bindNone();
	}

	@Override
	public void render(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		loadShader();
		final Empire e = ss.w.player;
		
		d.drawBG(MyDraw.SCREEN_BG, sm);
		ArrayList<Tech> techs = Loadable.all(Tech.class);
		smWidth = sm.width; smHeight = sm.height;
		
		int altSeparation = AGame.FOUNT.lineHeight + MyDraw.PANEL_INSET;
		
		if (totalWidth == 0) {
			totalHeight = 0;
			for (int i = 0; i < tierWidths.length; i++) {
				tierWidths[i] = 0;
			}
			for (int i = 0; i < rowHeights.length; i++) {
				rowHeights[i] = 0;
			}
			for (Tech t : techs) {
				if (!t.visible(e)) { continue; }
				int tw = 0;
				
				if (zoomedOut) {
					tw = TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2;
				} else {
					for (Tech.Choice c : t.choices) {
						tw = StrictMath.max(tw, (int) d.textSize(_t("tech_" + c.name), AGame.FOUNT).x);
						for (Bonus b : c.bonuses.list()) {
							tw = StrictMath.max(tw, (int) d.textSize(b.getDesc(), AGame.FOUNT).x);
						}
					}

					tw += TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2 + MyDraw.PANEL_INSET * 2;
				}

				tierWidths[t.tier - Tech.getMinTier()] = StrictMath.max(tierWidths[t.tier - Tech.getMinTier()], tw);

				int rh = 0;
				for (Tech.Choice c : t.choices) {
					if (zoomedOut) {
						rh += TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2;
					} else {
						rh += StrictMath.max(TECH_ICON_SIZE, AGame.BIG_FOUNT.lineHeight + c.bonuses.list().size() * AGame.FOUNT.lineHeight + (e.getResearchPoints(c) > 0 || c == e.research ? 6 + MyDraw.BUTTON_SPACING : 0)) + MyDraw.PANEL_INSET * 2;
					}
				}
				rh += altSeparation * (t.choices.size() - 1);

				rowHeights[t.row] = StrictMath.max(rowHeights[t.row], rh);
			}
			for (int w : tierWidths) {
				totalWidth += w;
			}
			totalWidth += techTierGap() * (Tech.layout.length - 1);
			
			for (int h : rowHeights) {
				totalHeight += h;
			}
			totalHeight += TECH_ROW_GAP * (Tech.layout[0].length - 1);
			
			// Layout
			int x = 0;
			for (int column = 0; column < Tech.layout.length; column++) {
				int y = 0;
				for (int row = 0; row < Tech.layout[0].length; row++) {
					Tech t = Tech.layout[column][row];
					if (t != null && t.visible(e)) {
						t.x = x;
						t.y = y;
						t.w = tierWidths[column];
						t.h = 0;
						for (Tech.Choice c : t.choices) {
							if (zoomedOut) {
								t.h += TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2;
							} else {
								t.h += StrictMath.max(TECH_ICON_SIZE, AGame.BIG_FOUNT.lineHeight + c.bonuses.list().size() * AGame.FOUNT.lineHeight + (e.getResearchPoints(c) > 0 || c == e.research ? 6 + MyDraw.BUTTON_SPACING : 0)) + MyDraw.PANEL_INSET * 2;
							}
						}
						t.h += altSeparation * (t.choices.size() - 1);
						t.y += (rowHeights[row] - t.h) / 2;
					}
					y += rowHeights[row] + TECH_ROW_GAP;
				}
				x += tierWidths[column] + techTierGap();
			}
		}
		
		if (focusOn != null) {
			ss.techScreenScrollX = -(focusOn.x + focusOn.w / 2) + sm.width * 7 / 8 / 2;
			ss.techScreenScrollY = -(focusOn.y + focusOn.h / 2) + sm.height / 2;
			focusOn = null;
		}
		
		int orW = (int) d.textSize(_t("tech_OR"), AGame.FOUNT).x + MyDraw.PANEL_INSET * 2;
		
		// Connectors
		Graphics g = (Graphics) d.frame().nativeRenderer();
		int tsz = techs.size();
		for (int hoverLp = 0; hoverLp < 2; hoverLp++) {
			for (int lp = hoverLp; lp < 2; lp++) {
				for (int ti = 0; ti < tsz; ti++) {
					Tech t = techs.get(ti);
					if (!t.visible(e)) { continue; }
					int dsz = t.dependencies.size();
					for (int di = 0; di < dsz; di++) {
						int col = t.tier - Tech.getMinTier();
						Tech dep = t.dependencies.get(di);
						boolean doDraw = !(hoverLp == 1 && t != hoverTech && dep != hoverTech);
						Clr c = MyDraw.SELECTED;
						boolean hasResearchedDep = false;
						for (int i = 0; i < dep.choices.size(); i++) {
							if (e.techs.contains(dep.choices.get(i))) {
								hasResearchedDep = true;
							}
						}
						if (!hasResearchedDep) {
							c = MyDraw.DARK_BG;
							if (t == hoverTech || dep == hoverTech) {
								c = AVAILABLE_TECH.mix(0.5, MyDraw.DARK_BG);
							}
						} else {
							if (t == hoverTech || dep == hoverTech) {
								c = Clr.WHITE;
							}
						}
						if (lp == 0) {
							c = Clr.BLACK;
						}
						g.setColor(new Color(c.r, c.g, c.b));
						int w = lp == 0 ? 6 : 2;
						g.setLineWidth(w);
						if (doDraw) {
							g.drawLine(
									dep.x + ss.techScreenScrollX + tierWidths[dep.tier] / 2,
									dep.y + dep.h / 2 + ss.techScreenScrollY,
									dep.x + ss.techScreenScrollX + tierWidths[dep.tier] + 8,
									dep.y + dep.h / 2 + ss.techScreenScrollY);
							g.drawLine(
									dep.x + ss.techScreenScrollX + tierWidths[dep.tier] + 8,
									dep.y + dep.h / 2 + ss.techScreenScrollY,
									t.x + ss.techScreenScrollX - 8,
									t.y + t.h / 2 + ss.techScreenScrollY);
							g.drawLine(
									t.x + ss.techScreenScrollX - 8,
									t.y + t.h / 2 + ss.techScreenScrollY,
									t.x + ss.techScreenScrollX + tierWidths[t.tier] / 2,
									t.y + t.h / 2 + ss.techScreenScrollY);
						}
						/*int shift = lp == 0 ? -2 : 0;
						if (Rect2D.intersects(
								dep.x + ss.techScreenScrollX + shift,
								StrictMath.min(dep.y + dep.h / 2 + ss.techScreenScrollY + shift, t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST + ss.techScreenScrollY + shift),
								t.x - techTierGap() / 4 + lane * TECH_LANE_DIST + techTierGap() / 4 + t.w / 2 - lane * TECH_LANE_DIST - dep.x,
								StrictMath.abs(dep.y + dep.h / 2 + ss.techScreenScrollY + shift - (t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST + ss.techScreenScrollY + shift)),
								0,
								0,
								sm.width,
								sm.height
						))
						{
							if (doDraw) {
								d.rect(c, dep.x + ss.techScreenScrollX + shift + tierWidths[dep.tier] / 2, dep.y + dep.h / 2 + ss.techScreenScrollY + shift, (t.x - dep.x - techTierGap() / 4 + w) + lane * TECH_LANE_DIST - tierWidths[dep.tier] / 2, w);
							}
							if (dep.y + dep.h / 2 < t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST - (t.dependencies.size() - 1) * TECH_LANE_DIST / 2) {
								if (doDraw) {d.rect(c, t.x + ss.techScreenScrollX - techTierGap() / 4 + lane * TECH_LANE_DIST + shift, dep.y + dep.h / 2 + ss.techScreenScrollY + shift, w, t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST - (t.dependencies.size() - 1) * TECH_LANE_DIST / 2 - dep.y - dep.h / 2); }
							} else if (dep.y + dep.h / 2 > t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST - (t.dependencies.size() - 1) * TECH_LANE_DIST / 2) {
								int nextTechIndex = techs.indexOf(dep) + 1;
								while (nextTechIndex < techs.size() && techs.get(nextTechIndex).tier != dep.tier) { nextTechIndex++; }
								if (nextTechIndex < techs.size()) {
									Tech nextTech = techs.get(nextTechIndex);
									if (nextTech.tier == dep.tier) {
										for (int ti2 = 0; ti2 < tsz; ti2++) {
											Tech conflicter = techs.get(ti2);
											if (conflicter == t) { continue; }
											if (conflicter.dependencies.contains(nextTech)) {
												hLanes[col][Math.min(hLanes[col].length - 1, t.row + 1)]++;
												break;
											}
										}
									}
								}
								if (doDraw) { d.rect(c, t.x + ss.techScreenScrollX - techTierGap() / 4 + lane * TECH_LANE_DIST + shift, t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST - (t.dependencies.size() - 1) * TECH_LANE_DIST / 2 + ss.techScreenScrollY + shift, w, dep.y + dep.h / 2 - t.y - t.h / 2 - hLanes[col][t.row] * TECH_LANE_DIST + (t.dependencies.size() - 1) * TECH_LANE_DIST / 2); }
							}
							if (doDraw) { d.rect(c, t.x + ss.techScreenScrollX - techTierGap() / 4 + lane * TECH_LANE_DIST + shift, t.y + t.h / 2 + hLanes[col][t.row] * TECH_LANE_DIST - (t.dependencies.size() - 1) * TECH_LANE_DIST / 2 + ss.techScreenScrollY + shift, techTierGap() / 4 + t.w / 2 - lane * TECH_LANE_DIST, w); }
						}
						hLanes[col][t.row]++;*/
					}
				}
			}
		}
		
		// Boxes
		hoverTech = null;
		for (int ti = 0; ti < tsz; ti++) {
			Tech t = techs.get(ti);
			if (!t.visible(e)) { continue; }
			int csz = t.choices.size();
			int y = t.y + ss.techScreenScrollY;
			int x = t.x + ss.techScreenScrollX;
			for (int ci = 0; ci < csz; ci++) {
				final Tech.Choice c = t.choices.get(ci);
				String timeCost = "?";
				int researchPoints = StrictMath.min(c.cost(e, ss.w.map), e.getResearchPoints(c));
				if (e.researchOutput(ss.w.map, null, true) > 0) {
					timeCost = ss.w.describeTime((c.cost(e, ss.w.map) - researchPoints) / e.researchOutput(ss.w.map, null, true));
				}
				int ch = StrictMath.max(TECH_ICON_SIZE, AGame.BIG_FOUNT.lineHeight + c.bonuses.list().size() * AGame.FOUNT.lineHeight + (researchPoints > 0 || c == e.research ? 6 + MyDraw.BUTTON_SPACING : 0)) + MyDraw.PANEL_INSET * 2;
				if (zoomedOut) {
					ch = TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2;
				}
				if (Rect2D.intersects(x, y, t.w, ch, 0, 0, sm.width, sm.height)) {
					if (!zoomedOut) {
						d.drawPanel(x + TECH_ICON_SIZE / 2, y, t.w - TECH_ICON_SIZE / 2, ch, (e.techs.contains(c) || e.research == c) ? 14 : -1);
						if (e.techs.contains(c)) {
							d.rect(new Clr(245, 237, 181, 25), x + TECH_ICON_SIZE / 2 + 2, y + 2, t.w - TECH_ICON_SIZE / 2 - 4, ch - 4);
						}
					}
					d.drawPanel(x, y, TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2, TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2, (e.techs.contains(c) || e.research == c) ? 14 : -1);
					d.highlight("tech-" + c.name, ss.g, x, y, TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2, TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2);
					String tint = "";
					if (e.techs.contains(c)) {
						d.blit(c.getImg(), x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET);
					} else if (e.research == c) {
						if (reToneShader == null) {
							d.blit(TECH_ICON_ACTIVE, SELECTED_TECH, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET);
						} else {
							reToneBlit(d, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, c.getImg(), SELECTED_TECH, SELECTED_TECH);
						}
						tint = SELECTED_TECH_C;
						d.hook(x, y, t.w, ch, new Hook("setResearch", Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
							@Override
							public void run(Input in, Pt p, Hook.Type type) {
								ss.w.giveCommand(msg("setResearch").put("empire", ss.w.player.id).put("tech", c.tech.name).put("choice", c.name));
							}
						});
						
						String tt = _t("click_to_research") + "\n" + timeCost;
						if (!ss.w.player.techEverResearched.contains(c)) {
							String heroFx = Hero.getStatChangeAppendix(ss.w.player, HeroEvent.techResearched(ss.w.player, c), ss.w.map, false);
							if (!heroFx.isEmpty()) {
								tt += "\n" + heroFx;
							}
						}
						d.tooltip(x, y, t.w, ch, tt);
					} else if (c.tech.available(e)) {
						boolean hilit = d.cursor().x < sm.width - sidePanelW - MyDraw.WINDOW_INSET - MyDraw.SIDE_CLEARANCE && Rect2D.contains(x, y, t.w, ch, d.cursor().x, d.cursor().y);
						if (reToneShader == null) {
							d.blit(TECH_ICON, hilit ? SELECTED_TECH : AVAILABLE_TECH, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET);
						} else {
							reToneBlit(d, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, c.getImg(), hilit ? AVAILABLE_TECH : UNAVAILABLE_TECH, hilit ? SELECTED_TECH : AVAILABLE_TECH);
						}
						d.hook(x, y, t.w, ch, new Hook("setResearch", Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
							@Override
							public void run(Input in, Pt p, Hook.Type type) {
								ss.w.giveCommand(msg("setResearch").put("empire", ss.w.player.id).put("tech", c.tech.name).put("choice", c.name));
							}
						});
						d.hook(x, y, t.w, ch, new Hook("addResearch", Hook.Type.MOUSE_2_CLICKED, Hook.Type.TEST) {
							@Override
							public void run(Input in, Pt p, Hook.Type type) {
								ss.w.giveCommand(msg("addResearch").put("empire", ss.w.player.id).put("tech", c.tech.name).put("choice", c.name));
							}
						});
						String tt = _t("click_to_research_right_click_to_queue") + "\n" + timeCost;
						if (!ss.w.player.techEverResearched.contains(c)) {
							String heroFx = Hero.getStatChangeAppendix(ss.w.player, HeroEvent.techResearched(ss.w.player, c), ss.w.map, false);
							if (!heroFx.isEmpty()) {
								tt += "\n" + heroFx;
							}
						}
						d.tooltip(x, y, t.w, ch, tt);
					} else {
						boolean hilit = d.cursor().x < sm.width - sidePanelW - MyDraw.WINDOW_INSET - MyDraw.SIDE_CLEARANCE && Rect2D.contains(x, y, t.w, ch, d.cursor().x, d.cursor().y);
						tint = UNAVAILABLE_TECH_C;
						if (reToneShader == null) {
							d.blit(TECH_ICON, hilit ? UNAVAILABLE_TECH : MyDraw.DARK_BG, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET);
						} else {
							reToneBlit(d, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, c.getImg(), MyDraw.DARK_BG, hilit ? UNAVAILABLE_TECH : MyDraw.DARK_BG);
						}
						d.hook(x, y, t.w, ch, new Hook("setResearch", Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
							@Override
							public void run(Input in, Pt p, Hook.Type type) {
								ss.w.giveCommand(msg("setResearch").put("empire", ss.w.player.id).put("tech", c.tech.name).put("choice", c.name));
							}
						});
						d.hook(x, y, t.w, ch, new Hook("addResearch", Hook.Type.MOUSE_2_CLICKED, Hook.Type.TEST) {
							@Override
							public void run(Input in, Pt p, Hook.Type type) {
								ss.w.giveCommand(msg("addResearch").put("empire", ss.w.player.id).put("tech", c.tech.name).put("choice", c.name));
							}
						});
						d.tooltip(x, y, t.w, ch, _t("prerequisites_not_met") + "\n" + timeCost);
					}
					if (!e.researchQueue.isEmpty()) {
						if (e.research == c) {
							d.heavilyBorderedText("1", AGame.FOUNT, AGame.FOUNT_OUTLINE, Clr.WHITE, MyDraw.DARK_BG, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, 10000);
						} else {
							int qIndex = e.researchQueue.indexOf(c);
							if (qIndex != -1) {
								d.heavilyBorderedText("" + (qIndex + 2), AGame.FOUNT, AGame.FOUNT_OUTLINE, Clr.WHITE, MyDraw.DARK_BG, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, 10000);
							}
						}
					}
					if (d.cursor().x < sm.width - sidePanelW - MyDraw.WINDOW_INSET - MyDraw.SIDE_CLEARANCE && Rect2D.contains(x, y, t.w, ch, d.cursor().x, d.cursor().y)) {
						hoverTech = c.tech;
					}
					if (!zoomedOut) {
						int x2 = x + TECH_ICON_SIZE + MyDraw.PANEL_INSET * 3;
						int y2 = y;
						y2 += MyDraw.PANEL_INSET;
						d.text(tint + _t("tech_" + c.name), AGame.BIG_FOUNT, x2, y2);
						y2 += AGame.BIG_FOUNT.lineHeight;
						int bsz = c.bonusList.size();
						for (int bi = 0; bi < bsz; bi++) {
							d.text(tint + c.bonusList.get(bi).getDesc(), AGame.FOUNT, x2, y2);
							y2 += AGame.FOUNT.lineHeight;
						}
						if (researchPoints > 0 || e.research == c) {
							d.rect(e.research == c ? SELECTED_TECH : MyDraw.SELECTED, x2, y + ch - MyDraw.PANEL_INSET - 6, t.w - TECH_ICON_SIZE - MyDraw.PANEL_INSET * 4, 6);
							d.rect(MyDraw.DARK_BG, x2 + 1, y + ch - MyDraw.PANEL_INSET - 6 + 1, t.w - TECH_ICON_SIZE - MyDraw.PANEL_INSET * 4 - 2, 4);
							d.rect(e.research == c ? SELECTED_TECH : MyDraw.SELECTED, x2 + 2, y + ch - MyDraw.PANEL_INSET - 6 + 2, researchPoints * 1.0 * (t.w - TECH_ICON_SIZE - MyDraw.PANEL_INSET * 4 - 4) / c.cost(e, ss.w.map), 2);
						}
					} else {
						if (researchPoints > 0 || e.research == c) {
							d.rect(e.research == c ? SELECTED_TECH : MyDraw.SELECTED, x + MyDraw.PANEL_INSET, y + ch - MyDraw.PANEL_INSET - 6, TECH_ICON_SIZE, 6);
							d.rect(MyDraw.DARK_BG, x + MyDraw.PANEL_INSET + 1, y + ch - MyDraw.PANEL_INSET - 6 + 1, TECH_ICON_SIZE - 2, 4);
							d.rect(e.research == c ? SELECTED_TECH : MyDraw.SELECTED, x + MyDraw.PANEL_INSET + 2, y + ch - MyDraw.PANEL_INSET - 6 + 2, researchPoints * 1.0 * (TECH_ICON_SIZE - 4) / c.cost(e, ss.w.map), 2);
						}
						StringBuilder info = new StringBuilder();
						info.append(_t("tech_" + c.name));
						int bsz = c.bonusList.size();
						for (int bi = 0; bi < bsz; bi++) {
							info.append("\n").append(c.bonusList.get(bi).getDesc());
						}
							if (!e.techs.contains(c) && e.research != c) {
							if (c.tech.available(e)) {
								info.append("\n").append(_t("click_to_research"));
								if (!ss.w.player.techEverResearched.contains(c)) {
									String heroFx = Hero.getStatChangeAppendix(ss.w.player, HeroEvent.techResearched(ss.w.player, c), ss.w.map, false);
									if (!heroFx.isEmpty()) {
										info.append("\n").append(heroFx);
									}
								}
							} else {
								info.append("\n").append(_t("prerequisites_not_met"));
							}
						} 
						d.tooltip(x, y, t.w, ch, info.toString());
						if (hoverTech == c.tech) {
							d.iconButton(x + TECH_ICON_SIZE + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, y + TECH_ICON_SIZE + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, ZOOM_IN, new Runnable() {
								@Override
								public void run() {
									zoomIn();
									focusOn = c.tech;
								}
							}, true);
						}
						d.tooltip(x + TECH_ICON_SIZE + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, y + TECH_ICON_SIZE + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Zoom_in"));
					}
					if (!e.techs.contains(c)) {
						ArrayList<Empire> coResearchers = c.coResearchers(e, ss.w.map);
						if (!coResearchers.isEmpty()) {
							d.blit(DiplomacyWindow.researchTreaty, x + t.w - MyDraw.PANEL_INSET - DiplomacyWindow.ICON_SIZE, y + ch - MyDraw.PANEL_INSET - DiplomacyWindow.ICON_SIZE);
							d.tooltip(x + t.w - MyDraw.PANEL_INSET - DiplomacyWindow.ICON_SIZE, y + ch - MyDraw.PANEL_INSET - DiplomacyWindow.ICON_SIZE, DiplomacyWindow.ICON_SIZE, DiplomacyWindow.ICON_SIZE,
									_t("reduced_tech_cost", EmpireStat.RESEARCH_COST_DECREASE_PERCENT_FOR_TREATY.explain(e.bonuses, FormatUtils.INT_PERCENTAGE), Empire.nameList(coResearchers, Lang.currentLocale)));
						}
					}
				}
				y += ch + altSeparation;
			}
			y = t.y + ss.techScreenScrollY;
			for (int ci = 0; ci < csz; ci++) {
				Tech.Choice c = t.choices.get(ci);
				String tint = c.tech.available(e) ? "" : UNAVAILABLE_TECH_C;
				int ch = StrictMath.max(TECH_ICON_SIZE, AGame.BIG_FOUNT.lineHeight + c.bonuses.list().size() * AGame.FOUNT.lineHeight) + MyDraw.PANEL_INSET * 2;
				if (zoomedOut) {
					ch = TECH_ICON_SIZE + MyDraw.PANEL_INSET * 2;
				}
				y += ch + altSeparation;
				if (ci < csz - 1 && Rect2D.intersects(x + t.w / 2 - orW / 2, y - altSeparation / 2 - AGame.FOUNT.lineHeight / 2 - MyDraw.PANEL_INSET, orW, MyDraw.PANEL_INSET * 2 + AGame.FOUNT.lineHeight, 0, 0, sm.width, sm.height)) {
					d.drawPanel(x + t.w / 2 - orW / 2, y - altSeparation / 2 - AGame.FOUNT.lineHeight / 2 - MyDraw.PANEL_INSET, orW, MyDraw.PANEL_INSET * 2 + AGame.FOUNT.lineHeight, -1);
					d.text(tint + _t("tech_OR"), AGame.FOUNT, x + t.w / 2 - orW / 2 + MyDraw.PANEL_INSET, y - altSeparation / 2 - AGame.FOUNT.lineHeight / 2);
				}
			}
		}
		
		d.drawTopBar(sm);
		
		int bw = d.bw(_t("Leave"));
		int x2 = sm.width - bw - MyDraw.SIDE_CLEARANCE;
		d.button(x2, MyDraw.TOP_BAR_INSET, bw, _t("Leave"), "L", new InputRunnable() {
			@Override
			public void run(Input in) {
				ss.g.s = ss;
			}
		});
		d.text(_t("Research"), AGame.BIG_FOUNT, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_INSET);
		
		d.iconToggle(MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_H + MyDraw.UI_SPACING, ZOOM_OUT, new Runnable() {
			@Override
			public void run() {
				if (zoomedOut) {
					zoomIn();
				} else {
					zoomOut();
				}
			}
		}, zoomedOut, true);
		d.tooltip(MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_H + MyDraw.UI_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Zoom"));
		
		sidePanelW = sm.width / 8;
		sidePanelW = StrictMath.max(sidePanelW, TECH_ICON_SIZE + MyDraw.UI_SPACING + (int) d.textSize(_t("Select_Research"), AGame.BIG_FOUNT).x);
		int x = sm.width - (sidePanelW + MyDraw.WINDOW_INSET + MyDraw.SIDE_CLEARANCE);
		int y = MyDraw.TOP_BAR_H;
		d.drawRightSideWindow(sm, x, y);
		d.hook(x, y, sidePanelW + MyDraw.WINDOW_INSET + MyDraw.SIDE_CLEARANCE, sm.height, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_2_CLICKED, Hook.Type.HOVER) {
			@Override
			public void run(Input in, Pt p, Hook.Type type) {
				// Ignore
			}
		});
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.WINDOW_INSET;
		int y2 = y;
		int col2W = sidePanelW - TECH_ICON_SIZE - MyDraw.UI_SPACING * 2;
		int col2X = x + TECH_ICON_SIZE + MyDraw.UI_SPACING;
		if (e.research == null) {
			d.blit(TECH_ICON, MyDraw.SELECTED, x, y);
			d.text(_t("Select_Research"), AGame.BIG_FOUNT, col2X, y2, col2W);
			y += StrictMath.max(TECH_ICON_SIZE, d.textSize(_t("Select_Research"), AGame.BIG_FOUNT, 0, 0, col2W).height) + MyDraw.UI_SPACING;
			d.progressBar(x, y, sidePanelW, 0);
		} else {
			d.text(_t("Researching_x", _t("tech_" + e.research.name)), AGame.BIG_FOUNT, x, y, sidePanelW);
			y += d.textSize(_t("Researching_x", _t("tech_" + e.research.name)), AGame.BIG_FOUNT, 0, 0, sidePanelW).height + MyDraw.UI_SPACING;
			y2 = y;
			d.blit(e.research.getImg(), x, y);
			
			int bsz = e.research.bonusList.size();
			for (int bi = 0; bi < bsz; bi++) {
				d.text(e.research.bonusList.get(bi).getDesc(), AGame.FOUNT, col2X, y2, col2W);
				y2 += d.textSize(e.research.bonusList.get(bi).getDesc(), AGame.FOUNT, 0, 0, col2W).height;
			}
			y = StrictMath.max(y2, y + TECH_ICON_SIZE) + MyDraw.UI_SPACING;
			d.progressBar(x, y, sidePanelW, e.researchPoints * 1.0 / e.research.cost(e, ss.w.map));
		}
		StringBuilder re = new StringBuilder();
		int res = e.researchOutput(ss.w.map, re, true);
		y += MyDraw.PROGRESS_BAR_H + MyDraw.UI_SPACING;
		if (e.research != null && res > 0) {
			String timeCost = ss.w.describeTime((e.research.cost(e, ss.w.map) - e.getResearchPoints(e.research)) / res);
			d.text(timeCost, AGame.FOUNT, x, y, sidePanelW);
			y += d.textSize(timeCost, AGame.FOUNT, x, y, sidePanelW).height + MyDraw.UI_SPACING;
		} else {
			y += MyDraw.UI_SPACING;
		}
		String resExplain = _t("Research") + ":\n" + re.toString();
		d.text(resExplain, AGame.FOUNT, x, y, sidePanelW);
		y += (int) d.textSize(resExplain, AGame.FOUNT, x, y, sidePanelW).height + MyDraw.UI_SPACING;
		d.text(_t("Research_Queue"), AGame.BIG_FOUNT, x, y);
		y += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
		if (e.research != null) {
			d.text("1: " + _t("tech_" + e.research.name), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
			if (res > 0) {
				String timeCost = ss.w.describeTime((e.research.cost(e, ss.w.map) - e.getResearchPoints(e.research)) / res);
				d.text(timeCost, AGame.FOUNT, x + MyDraw.UI_SPACING * 2, y, sidePanelW - MyDraw.UI_SPACING * 2);
				y += d.textSize(timeCost, AGame.FOUNT, x + MyDraw.UI_SPACING * 2, y, sidePanelW - MyDraw.UI_SPACING * 2).height;
			}
			int n = 1;
			for (Tech.Choice c : e.researchQueue) {
				n++;
				d.text(n + ": " + _t("tech_" + c.name), AGame.FOUNT, x, y);
				y += AGame.FOUNT.lineHeight;
				if (res > 0) {
					String timeCost = ss.w.describeTime((c.cost(e, ss.w.map) - e.getResearchPoints(c)) / res);
					d.text(timeCost, AGame.FOUNT, x + MyDraw.UI_SPACING * 2, y, sidePanelW - MyDraw.UI_SPACING * 2);
					y += d.textSize(timeCost, AGame.FOUNT, x + MyDraw.UI_SPACING * 2, y, sidePanelW - MyDraw.UI_SPACING * 2).height;
				}
			}
		} else {
			d.text(_t("_empty_"), AGame.FOUNT, x, y);
		}
		
		if (ss.showNotice()) {
			int noticeH = ss.noticeHeight(d, sidePanelW);
			y = sm.height - noticeH - MyDraw.SIDE_CLEARANCE;
			ss.renderNotice(d, sm, hs, cursor, x, y, sidePanelW, noticeH);
		}
	}

	@Override
	public ArrayList<String> music() {
		return null;
	}

	@Override
	public String appearancePostfix() {
		return "DAY";
	}

	@Override
	public boolean alwaysUseAppearancePostfix() {
		return false;
	}
}
