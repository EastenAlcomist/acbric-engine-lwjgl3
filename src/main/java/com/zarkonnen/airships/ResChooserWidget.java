package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import com.zarkonnen.catengine.util.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public strictfp class ResChooserWidget implements ScrollBar.ScrollElementAdapter<ScreenModeEntry>{
	private ScreenMode preferred;
	private ScreenModeEntry selected;
	private static ScreenMode prevApplied;
	private int appliedCooldown;
	private ScreenMode applied;
	private ScreenModeEntry custom;
	private boolean fullscreenSelected;
	private boolean windowedFullscreenSelected;
	private ArrayList<ScreenModeEntry> modes;
	private boolean modesScrolledTo = false;
	private final ScrollBar modeSB = new ScrollBar();
	private final TextField wField = new TextField(false, "0123456789");
	private final TextField hField = new TextField(false, "0123456789");
	private static DisplayMode originalMode;
	
	private static boolean isMac;
	private static boolean isWin;
	
	static {
		isMac = System.getProperty("os.name").contains("Mac");
		isWin = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");
	}
		
	public ResChooserWidget() {
		wField.focus = true;
		hField.focus = false;
	}
	
	public void setCustomMode(int w, int h, boolean fullscreen, boolean windowedFullscreen) {
		setCustomModeNoSetFields(w, h, fullscreen, windowedFullscreen);
		wField.setText("" + w);
		hField.setText("" + h);
	}
	
	public void selectCustomMode() {
		selected = custom;
	}
	
	private void setCustomModeNoSetFields(int w, int h, boolean fullscreen, boolean windowedFullscreen) {
		custom.w = w;
		custom.h = h;
		fullscreenSelected = fullscreen;
		windowedFullscreenSelected = windowedFullscreen;
		custom.windowed = new ScreenMode(w, h, false);
		custom.fullScreen = null;
		custom.windowedFullScreen = new ScreenMode(w, h, false, true);
		for (ScreenModeEntry sme : modes) {
			if (sme.w == w && sme.h == h) {
				custom.windowed = sme.windowed;
				custom.fullScreen = sme.fullScreen;
				custom.windowedFullScreen = sme.windowedFullScreen;
			}
		}
		if (isMac) { custom.windowedFullScreen = null; }
		if (isWin) { custom.fullScreen = null; }
	}
	
	public void setSelected(ScreenMode sm) {
		ScreenModeEntry sme = entryForMode(sm);
		if (sme == null) { return; }
		selected = sme;
		fullscreenSelected = sm.fullscreen;
		windowedFullscreenSelected = sm.fullscreenWindow;
		selectValidMode();
	}
	
	public void selectValidMode() {
		if (fullscreenSelected && selected.fullScreen == null) {
			fullscreenSelected = false;
		} else if (windowedFullscreenSelected && selected.windowedFullScreen == null) {
			windowedFullscreenSelected = false;
		}
		if (!fullscreenSelected && !windowedFullscreenSelected && selected.windowed == null) {
			fullscreenSelected = true;
		}
	}
	
	private void setPreferred(ScreenMode sm) {
		AirshipGame.PREFS.putInt("screenModeW", sm.width);
		AirshipGame.PREFS.putInt("screenModeH", sm.height);
		AirshipGame.PREFS.putBoolean("screenModeF", sm.fullscreen);
		AirshipGame.PREFS.putBoolean("screenModeFW", sm.fullscreenWindow);
		try {
			AirshipGame.PREFS.save();
		} catch (Exception e) {}
		preferred = sm;
	}
	
	public boolean hasAppliedCooldown() {
		return prevApplied != null && appliedCooldown > 0;
	}
	
	public int appliedCooldownSeconds() {
		return (int) Math.ceil(appliedCooldown * 1.0 / 1000);
	}
	
	private void setMode(ScreenMode sm, Input in) throws RuntimeException {
		doSetMode(sm, in);
		
		if (prevApplied != null) {
			appliedCooldown = 10000;
		} else {
			setPreferred(sm);
			appliedCooldown = 0;
			prevApplied = sm;
		}
		applied = sm;
	}
	
	private void doSetMode(ScreenMode sm, Input in) {
		if (sm.fullscreenWindow) {
			if (originalMode != null) {
				in.setMode(new ScreenMode(originalMode.getWidth(), originalMode.getHeight(), false, true));
			} else {
				in.setMode(modes.get(1).windowedFullScreen);
			}
			// 引擎已统一为“显示器原生分辨率无边框全屏”（不再按请求分辨率切换），
			// 因此直接原生渲染（scaleFrom=null），画面清晰不放大模糊。
			AirshipGame.setScaleFrom(null);
		} else {
			in.setMode(sm);
			AirshipGame.setScaleFrom(null);
		}
	}

	private void getModes(Input in) {
		if (originalMode == null) {
			originalMode = Display.getDesktopDisplayMode();
		}
		modes = new ArrayList<ScreenModeEntry>();
		try {
			preferred = new ScreenMode(AirshipGame.PREFS.getInt("screenModeW", 1024), AirshipGame.PREFS.getInt("screenModeH", 768), AirshipGame.PREFS.getBoolean("screenModeF", false), AirshipGame.PREFS.getBoolean("screenModeFW", false));
			if (isWin && preferred.fullscreen) {
				preferred = new ScreenMode(preferred.width, preferred.height, false, true);
			}
		} catch (Exception e) {
			AirshipGame.instance.reportError(_t("Unable_to_load_prefs"), e, null, false);
		}
		ArrayList<ScreenMode> cands = in.modes();
		ScreenMode biggest = null;
		boolean preferredAllowed = preferred != null && !preferred.fullscreen;
		boolean has1024 = false;
		for (ScreenMode c : cands) {
			if (preferred != null && c.width == preferred.width && c.height == preferred.height && c.fullscreen == preferred.fullscreen) {
				preferredAllowed = true;
			}
			if (preferred != null && c.width == preferred.width && c.height == preferred.height && preferred.fullscreenWindow) {
				preferredAllowed = true;
			}
			if (!c.fullscreen || c.width < 800 || c.height < 540) { continue; }
			if (c.width == 1024 && c.height == 768) { has1024 = true; }
			ScreenModeEntry sme = new ScreenModeEntry(c.width, c.height, false);
			if (modes.contains(sme)) {
				sme = modes.get(modes.indexOf(sme));
			} else {
				modes.add(sme);
			}
			sme.fullScreen = c;
			if (!isMac && !(originalMode != null && (c.width > originalMode.getWidth() || c.height > originalMode.getHeight()))) {
				sme.windowedFullScreen = new ScreenMode(c.width, c.height, false, true);
			}
			//if (!(originalMode != null && (c.width > originalMode.getWidth() || c.height >= originalMode.getHeight()))) {
				sme.windowed = new ScreenMode(c.width, c.height, false);
			//}
			if (biggest == null || biggest.width * biggest.height < c.width * c.height) {
				biggest = c;
			}
		}
		/*if (biggest != null) {
			modes.get(modes.indexOf(new ScreenModeEntry(biggest.width, biggest.height, false))).windowed = null;
		}*/
		if (preferred != null && biggest != null && preferred.fullscreenWindow && preferred.width == biggest.width && preferred.height == biggest.height) {
			preferredAllowed = true;
		}
		if (preferred != null && !preferred.fullscreen && !preferred.fullscreenWindow && (preferred.width != biggest.width && preferred.height != biggest.height)) {
			preferredAllowed = true;
		}
		if (!preferredAllowed) {
			preferred = null;
		}
		if (!has1024) {
			ScreenModeEntry sme = new ScreenModeEntry(1024, 768, false);
			sme.windowed = new ScreenMode(1024, 768, false);
			modes.add(sme);
		}
		Collections.sort(modes);
		custom = new ScreenModeEntry(800, 600, true);
		if (preferred != null && !modes.contains(new ScreenModeEntry(preferred.width, preferred.height, false))) {
			setCustomMode(preferred.width, preferred.height, preferred.fullscreen, preferred.fullscreenWindow);
		} else if (biggest != null) {
			if (isWin) {
				setCustomMode(biggest.width, biggest.height, false, true);
			} else {
				setCustomMode(biggest.width, biggest.height, true, false);
			}
		} else {
			setCustomMode(800, 600, false, false);
		}
		
		modes.add(0, custom);
		
		if (preferred == null && originalMode != null) {
			ScreenModeEntry sme = entryForMode(new ScreenMode(originalMode.getWidth(), originalMode.getHeight(), true));
			preferred = sme == null ? null : sme.fullScreen;
		}
		
		if (biggest != null && preferred == null) {
			preferred = biggest;
		}
		
		if (preferred == null) {
			selected = modes.get(1);
			if (isWin) {
				fullscreenSelected = false;
				windowedFullscreenSelected = true;
			} else {
				fullscreenSelected = true;
				windowedFullscreenSelected = false;
			}
		} else {
			selected = entryForMode(preferred);
			if (selected == custom) {
				custom.w = preferred.width;
				custom.h = preferred.height;
			}
			fullscreenSelected = preferred.fullscreen;
			windowedFullscreenSelected = preferred.fullscreenWindow;
		}
				
		selectValidMode();
		
		applied = getSelectedMode();
	}
	
	private ScreenModeEntry entryForMode(ScreenMode sm) {
		for (int i = 1; i < modes.size(); i++) {
			ScreenModeEntry sme = modes.get(i);
			if (sme.w == sm.width && sme.h == sm.height && ((sm.fullscreen && sme.fullScreen != null) || (sm.fullscreenWindow && sme.windowedFullScreen != null) || (!sm.fullscreen && !sm.fullscreenWindow && sme.windowed != null))) {
				return sme;
			}
		}
		return custom;
	}
	
	public int minHeight(MyDraw d) {
		return 100;
	}
	
	public int minWidth(MyDraw d) {
		return scrollBarWidth(d) + MyDraw.UI_SPACING + configWidth(d);
	}
	
	public int configWidth(MyDraw d) {
		int w = 0;
		w = Math.max(w, MyDraw.PANEL_INSET * 4 + (int) d.textSize("99999 X 99999", AGame.FOUNT).x);
		w = Math.max(w, d.tw(_t("fullscreen")));
		w = Math.max(w, d.tw(_t("fullscreen_window")));
		w = Math.max(w, d.tw(_t("window")));
		return w;
	}
	
	public int scrollBarWidth(MyDraw d) {
		if (modes == null) { return 100; }
		int w = 0;
		for (ScreenModeEntry sme : modes) {
			w = Math.max(w, d.tw(mToString(sme)));
		}
		return w + MyDraw.PANEL_INSET * 2 + ScrollBar.SCROLL_BAR_W;
	}
	
	public boolean isValid() {
		ScreenMode m = getSelectedMode();
		if (m == null) { return false; }
		if (m.width < 800 || m.height < 540) { return false; }
		if (m.fullscreen || m.fullscreenWindow) {
			for (ScreenModeEntry sme : modes) {
				if (sme.w == m.width && sme.h == m.height && !sme.custom) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	public boolean hasChanges() {
		return applied != null && !applied.equals(getSelectedMode());
	}
	
	private ScreenMode getSelectedMode() {
		if (selected == null) { return null; }
		if (fullscreenSelected) {
			return selected.fullScreen;
		} else if (windowedFullscreenSelected) {
			return selected.windowedFullScreen;
		} else {
			return selected.windowed;
		}
	}
	
	public boolean hasPreferredMode() {
		return preferred != null;
	}
	
	public void applyPreferred(Input in) throws RuntimeException {
		setMode(preferred, in);
	}
	
	public void apply(Input in) throws RuntimeException {
		setMode(getSelectedMode(), in);
		Display.setResizable(!getSelectedMode().fullscreen && !getSelectedMode().fullscreenWindow);
	}
	
	private final Clr HIDE_UI = new Clr(0, 0, 0, 128);
	
	public void renderAppliedCooldown(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		d.rect(HIDE_UI, 0, 0, sm.width, sm.height);
		int w = Math.max((int) d.textSize(_t("confirm_new_res_settings"), AGame.FOUNT).x, (int) d.textSize("W", AGame.FOUNT).x + MyDraw.UI_SPACING + d.bw(_t("Cancel")) + MyDraw.BUTTON_SPACING + d.bw(_t("OK"))) + MyDraw.WINDOW_INSET * 2;
		int h = AGame.FOUNT.lineHeight + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
		int x = MyDraw.SIDE_CLEARANCE;
		int y = MyDraw.SIDE_CLEARANCE;
		d.drawWindow(x, y, w, h, 19);
		x += MyDraw.WINDOW_INSET; y += MyDraw.WINDOW_INSET;
		w -= MyDraw.WINDOW_INSET * 2;
		d.text(_t("confirm_new_res_settings"), AGame.FOUNT, x, y);
		y += AGame.FOUNT.lineHeight + MyDraw.UI_SPACING;
		d.text("" + appliedCooldownSeconds(), AGame.FOUNT, x, y + MyDraw.BUTTON_H - AGame.FOUNT.lineHeight);
		int bw = d.bw(_t("OK"));
		x += w - bw;
		d.button(x, y, bw, _t("OK"), new Runnable() {
			@Override
			public void run() {
				appliedCooldown = 0;
				setPreferred(applied);
				prevApplied = applied;
			}
		});
		bw = d.bw(_t("Cancel"));
		x -= bw + MyDraw.BUTTON_SPACING;
		d.button(x, y, bw, _t("Cancel"), new InputRunnable() {
			@Override
			public void run(Input in) {
				appliedCooldown = 0;
				setSelected(prevApplied);
				doSetMode(prevApplied, in);
				appliedCooldown = 0;
				applied = prevApplied;
			}
		});
	}
	
	public void inputAppliedCooldown(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
		appliedCooldown -= ms;
		if (Keys.check(in, "ENTER")) {
			appliedCooldown = 0;
			setPreferred(applied);
			prevApplied = applied;
			return;
		}
		if (appliedCooldown <= 0 || Keys.check(in, "ESCAPE")) {
			appliedCooldown = 0;
			setSelected(prevApplied);
			doSetMode(prevApplied, in);
			appliedCooldown = 0;
			applied = prevApplied;
		}
	}
	
	public void render(int x, int y, int w, int h, MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		if (Main.ON_STEAM_DECK) { return; }
		if (modes == null) { return; }
		int sbw = scrollBarWidth(d);
		if (!modesScrolledTo && selected != null) {
			modeSB.scrollTo(false, d, x, y, sbw, h, modes, this, selected);
			modesScrolledTo = true;
		}
		modeSB.draw(d, x, y, sbw, h, modes, this);
		x += sbw + MyDraw.UI_SPACING;
		w -= sbw + MyDraw.UI_SPACING;
		if (selected == custom) {
			int x2 = x;
			int fieldW = MyDraw.PANEL_INSET * 2 + (int) d.textSize("99999", AGame.FOUNT).x;
			wField.render(x2, y, fieldW, d);
			if (!wField.focus) {
				d.hook(x2, y, fieldW, MyDraw.textFieldH(), new Hook(Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						wField.focus = true;
						hField.focus = false;
					}
				});
			}
			x2 += fieldW;
			d.text(" X ", AGame.FOUNT, x2, y + MyDraw.PANEL_INSET);
			x2 += d.textSize(" X ", AGame.FOUNT).x;
			hField.render(x2, y, fieldW, d);
			if (!hField.focus) {
				d.hook(x2, y, fieldW, MyDraw.textFieldH(), new Hook(Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Hook.Type type) {
						wField.focus = false;
						hField.focus = true;
					}
				});
			}
			y += MyDraw.textFieldH() + MyDraw.BUTTON_SPACING;
		}
		if (isWin) {
			d.toggle(x, y, w, _t("fullscreen"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					fullscreenSelected = false;
					windowedFullscreenSelected = true;
				}
			}, windowedFullscreenSelected, selected.windowedFullScreen != null);
		} else {
			d.toggle(x, y, w, _t("fullscreen"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					fullscreenSelected = true;
					windowedFullscreenSelected = false;
				}
			}, fullscreenSelected, selected.fullScreen != null);
		}
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		if (!isMac && !isWin) {
			d.toggle(x, y, w, _t("fullscreen_window"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					fullscreenSelected = false;
					windowedFullscreenSelected = true;
				}
			}, windowedFullscreenSelected, selected.windowedFullScreen != null);
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		}
		d.toggle(x, y, w, _t("window"), null, new InputRunnable() {
			@Override
			public void run(Input in) {
				fullscreenSelected = false;
				windowedFullscreenSelected = false;
			}
		}, !windowedFullscreenSelected && !fullscreenSelected, selected.windowed != null);
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
	}
	
	public void input(int x, int y, int w, int h, Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
		if (Main.ON_STEAM_DECK) { return; }
		if (modes == null) {
			getModes(in);
		}
		modeSB.tick(in, x, y, w, h);
		if (selected == custom) {
			if (in.keyPressed("TAB")) {
				if (wField.focus) {
					wField.focus = false;
					hField.focus = true;
				} else {
					wField.focus = true;
					hField.focus = false;
				}
			}
			(wField.focus ? wField : hField).input(in, cursor, click, ms);
			int ww = 0;
			try {
				ww = Integer.parseInt(wField.getText());
			} catch (Exception e) {}
			int hh = 0;
			try {
				hh = Integer.parseInt(hField.getText());
			} catch (Exception e) {}
			setCustomModeNoSetFields(ww, hh, fullscreenSelected, windowedFullscreenSelected);
			//selectValidMode();
		}
	}
	
	@Override
	public int getHeight(ScreenModeEntry t, MyDraw d, int availableWidth) {
		return MyDraw.BUTTON_H + MyDraw.SCROLL_EL_SPACING;
	}

	@Override
	public void draw(final ScreenModeEntry m, MyDraw d, int x, int y, int width) {
		d.toggle(x, y, width, mToString(m), null, new InputRunnable() {
			@Override
			public void run(Input in) {
				selected = m;
				selectValidMode();
			}
		}, selected == m, true);
	}
	
	public static String mToString(ScreenModeEntry m) {
		return m.custom ? _t("custom_resolution_format") : _t("resolution_format", m.w, m.h);
	}
}
