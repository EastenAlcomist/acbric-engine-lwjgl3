package com.zarkonnen.airships;

import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.util.ArrayList;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.ScrollBar.ScrollElementAdapter;
import java.util.Arrays;

public class AchievementsScreen implements Screen, ScrollElementAdapter<Achievement> {
	public AirshipGame g;
	public ScrollBar sb = new ScrollBar();

	public AchievementsScreen(AirshipGame g) {
		this.g = g;
	}
	
	@Override
	public void input(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
		if (Keys.check(in, "ESCAPE") || Keys.check(in, "leave", "L", false)) {
			g.s = new MainMenu(g, MainMenu.Submenu.SETTINGS_AND_INFO);
		}
		sb.tick(in, 0, 0, in.mode().width, in.mode().height);
	}

	@Override
	public void render(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		d.drawBG(MyDraw.SCREEN_BG, sm);
		d.drawTopBar(sm);
		d.button(sm.width - MyDraw.SIDE_CLEARANCE - d.bw(_t("Leave")), MyDraw.TOP_BAR_INSET, d.bw(_t("Leave")), _t("Leave"), Keys.getText("leave", "L", false), new Runnable() {
			@Override
			public void run() {
				g.s = new MainMenu(g, MainMenu.Submenu.SETTINGS_AND_INFO);
			}
		});
		int maxW = 200;
		for (Achievement a : Achievement.values()) {
			maxW = Math.max(maxW, (int) Math.max(d.textSize(a.displayName, AGame.BIG_FOUNT).x, d.textSize(a.desc, AGame.FOUNT).x) + 64 + MyDraw.UI_SPACING * 2 + MyDraw.PANEL_BORDER_W * 2);
		}
		
		d.text(_t("Achievements"), AGame.BIG_FOUNT, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_INSET);
		
		int w = maxW + ScrollBar.SCROLL_BAR_W;
		int x = sm.width / 2 - w / 2;
		int y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		int h = sm.height - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING - MyDraw.SIDE_CLEARANCE;
		sb.draw(d, x, y, w, h, Arrays.asList(Achievement.values()), this);
	}

	@Override
	public ArrayList<String> music() {
		return AGame.NO_MUSIC;
	}

	@Override
	public String appearancePostfix() {
		return "DAY";
	}
	
	@Override
	public boolean alwaysUseAppearancePostfix() { return false; }

	@Override
	public int getHeight(Achievement t, MyDraw d, int availableWidth) {
		return Math.max(64 + MyDraw.PANEL_BORDER_W * 2, AGame.BIG_FOUNT.lineHeight + AGame.FOUNT.lineHeight) + MyDraw.SCROLL_EL_SPACING;
	}

	@Override
	public void draw(Achievement a, MyDraw d, int x, int y, int width) {
		boolean ia = Achievement.isAchieved(a);
		d.blit(ia ? a.logo : a.offLogo, x + MyDraw.PANEL_BORDER_W, y + MyDraw.PANEL_BORDER_W);
		d.drawPanelBorder(x, y, 64 + MyDraw.PANEL_BORDER_W * 2, 64 + MyDraw.PANEL_BORDER_W * 2);
		d.text((ia ? MyDraw.SELECTED_C : "") + a.displayName, AGame.BIG_FOUNT, x + 64 + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING, y);
		d.text((ia ? MyDraw.SELECTED_C : "") + a.desc, AGame.FOUNT, x + 64 + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING, y + AGame.BIG_FOUNT.lineHeight);
	}
}
