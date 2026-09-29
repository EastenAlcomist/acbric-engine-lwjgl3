package com.zarkonnen.airships;

import com.codedisaster.steamworks.SteamUtils;
import com.zarkonnen.airships.CoatEditor.IntRect;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hook.Type;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import com.zarkonnen.catengine.util.Rect;
import java.util.HashMap;

public strictfp class TextField {
	public String help = "";
	private String text = "";
	private int insertionPoint = 0;
	public boolean focus = true;
	public boolean password = false;
	public boolean useUpDown = true;
	private int timeSinceReset = 0;
	private int msSinceKeyPress = 0;
	public String filter = AGame.ALPHABET;
	public boolean multiLine = false;
	public boolean uppercase;
	public boolean showValid = true;
	public boolean pasteButton = false;
	public boolean canMoveInsertionPoint = true;
	public int maxLength = -1;
	private Pt lastClickPt = null;
	private int moveCursorVertical = 0;
	private final IntRect lastDrawn = new IntRect();
	boolean makePink = false;
	
	private final Img CLIPBOARD = new Img("ui", 13 * 16, 26 * 16, 16, 16, false);
	
	private static final Clr INVALID = new Clr(80, 20, 20);

	public TextField(boolean password, String filter, int maxLength) {
		this.password = password;
		this.filter = filter;
		this.maxLength = maxLength;
	}
	
	public TextField(boolean password, String filter) {
		this.password = password;
		this.filter = filter;
	}
	
	public TextField(boolean password) {
		this.password = password;
	}
	
	public TextField() {}
	
	public TextField(int maxLength) {
		this.maxLength = maxLength;
	}

	public void reset() {
		timeSinceReset = 0;
	}
	
	public char filter(char c) {
		if (c <= 0 || c == '\t') { return 0; }
		if (filter != null && !filter.contains(Character.toString(c))) {
			return 0;
		}
		if (uppercase) {
			return Character.toUpperCase(c);
		}
		return c;
	}
	
	public String filter(String input) {
		if (input.isEmpty()) { return input; }
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '\r') {
				c = '\n';
			}
			if (c == '\n') {
				if (multiLine) {
					sb.append(c);
				}
			} else {
				c = filter(c);
				if (c != 0) {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	public void input(Input in, Pt cursor, Pt click, int ms) {
		if (click != null && lastDrawn.contains(click) && Main.ON_STEAM_DECK) {
			SteamBackend.utils.showFloatingGamepadTextInput(
					multiLine ? SteamUtils.FloatingGamepadTextInputMode.ModeMultipleLines : SteamUtils.FloatingGamepadTextInputMode.ModeSingleLine, 
					lastDrawn.x, lastDrawn.y, lastDrawn.w, lastDrawn.h);
		}
		timeSinceReset += ms;
		msSinceKeyPress += ms;
		String input = filter(AirshipGame.getTypedText(in));
		if (focus) {
			if (in.keyPressed("V") && (in.keyDown("LCONTROL") || in.keyDown("RCONTROL") || in.keyDown("LMETA") || in.keyDown("RMETA")) && AGame.getClipboardString() != null) {
				input = filter(AGame.getClipboardString());
				if (input != null && (maxLength == -1 || text.length() + input.length() <= maxLength)) {
					text = text.substring(0, insertionPoint) + input + text.substring(insertionPoint);
					insertionPoint = StrictMath.min(text.length(), insertionPoint + input.length());
				}
			} else {
				if (timeSinceReset > 100) {
					if (canMoveInsertionPoint && in.keyDown("LEFT") && msSinceKeyPress >= 125) {
						insertionPoint = StrictMath.max(0, insertionPoint - 1);
						msSinceKeyPress = 0;
						timeSinceReset = 0;
					} else if (canMoveInsertionPoint && in.keyDown("RIGHT") && msSinceKeyPress >= 125) {
						insertionPoint = StrictMath.min(text.length(), insertionPoint + 1);
						msSinceKeyPress = 0;
						timeSinceReset = 0;
					} else if (useUpDown && canMoveInsertionPoint && in.keyDown("UP") && msSinceKeyPress >= 125) {
						msSinceKeyPress = 0;
						timeSinceReset = 0;
						moveCursorVertical = -1;
					} else if (useUpDown && canMoveInsertionPoint && in.keyDown("DOWN") && msSinceKeyPress >= 125) {
						msSinceKeyPress = 0;
						timeSinceReset = 0;
						moveCursorVertical = 1;
					} else if ("BACK".equals(in.lastKeyPressed())) {
						if (!text.isEmpty() && msSinceKeyPress >= 125) {
							text = text.substring(0, StrictMath.max(0, insertionPoint - 1)) + text.substring(insertionPoint);
							insertionPoint = StrictMath.max(0, insertionPoint - 1);
							msSinceKeyPress = 0;
							timeSinceReset = 0;
						}
					} else if (!input.isEmpty() && (maxLength == -1 || text.length() + input.length() <= maxLength)) {
						text = text.substring(0, insertionPoint) + input + text.substring(insertionPoint);
						insertionPoint = StrictMath.min(text.length(), insertionPoint + input.length());
						msSinceKeyPress = 0;
					}
				}
			}
			lastClickPt = click;
		} else {
			lastClickPt = null;
		}
	}
	
	public String displayTextPreFormatter()  {
		if (text.length() < 2) { return text; }
		if (password) {
			return text.substring(0, text.length() - 1).replaceAll(".", "*") + text.substring(text.length() - 1);
		} else {
			return text;
		}
	}
	
	public String displayText() {
		return displayTextPreFormatter();
	}
	
	private Pt insertionMarker(int x, int y, int w, int h, MyDraw d) {
		Rect r = d.charLocation(displayTextPreFormatter(), insertionPoint - 1, AGame.FOUNT, w - MyDraw.PANEL_INSET * 2, 10000, 0);
		return new Pt(r.x + r.width + x + MyDraw.PANEL_INSET, r.y + y + MyDraw.PANEL_INSET);
	}
	
	private void insertClick(int x, int y, int w, int h, MyDraw d) {
		Pt gotoPt = null;
		if (lastClickPt != null && lastDrawn.contains(lastClickPt)) {
			gotoPt = lastClickPt;
		} else if (moveCursorVertical != 0) {
			gotoPt = insertionMarker(x, y, w, h, d);
			gotoPt = new Pt(gotoPt.x, gotoPt.y + moveCursorVertical * AGame.FOUNT.lineHeight + AGame.FOUNT.lineHeight / 2);
		}
		moveCursorVertical = 0;
		if (canMoveInsertionPoint && gotoPt != null) {
			String t = displayTextPreFormatter();
			Pt relP = new Pt(gotoPt.x - x - MyDraw.PANEL_INSET, gotoPt.y - y - MyDraw.PANEL_INSET);
			if (relP.y < 0) {
				insertionPoint = 0;
			} else {
				boolean found = false;
				for (int i = 0; i < t.length(); i++) {
					Rect r = d.charLocation(displayTextPreFormatter(), i, AGame.FOUNT, w - MyDraw.PANEL_INSET * 2, 10000, 0);
					if (r.contains(relP)) {
						if (relP.x > r.x + r.width / 2) {
							insertionPoint = StrictMath.min(i + 1, text.length());
						} else {
							insertionPoint = i;
						}
						found = true;
						break;
					}
				}
				if (!found) {
					insertionPoint = text.length();
				}
			}
		}
		lastClickPt = null;
		lastDrawn.x = x; lastDrawn.y = y; lastDrawn.w = w; lastDrawn.h = h;
	}
	
	public void render(int x, int y, int w, MyDraw d) {
		insertClick(x, y, w, MyDraw.textFieldH(), d);
		d.drawPanel(x, y, w, MyDraw.textFieldH(), -1);
		d.rect(showValid ? Clr.DARK_GREY : INVALID, x + 2, y + 2, w - 4, MyDraw.textFieldH() - 4);
		if (text.isEmpty()) {
			d.text("[GREY]" + help, AGame.FOUNT, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET);
		}
		String inTxt = displayText();
		d.text(inTxt, AGame.FOUNT, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, w - MyDraw.PANEL_INSET * 2, MyDraw.textFieldH(), 0, false);
		if (pasteButton && AGame.getClipboardString() != null) {
			d.blit(CLIPBOARD, Clr.LIGHT_GREY, x + w - 19, y + 4);
			d.hook(x + w - 19, y + 4, 16, 16, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Type type) {
					setText(getText() + AGame.getClipboardString());
				}
			});
			d.tooltip(x + w - 19, y + 4, 16, 16, _t("Paste"));
		}
		if (focus && ((timeSinceReset) / 700) % 2 == 0) {
			Pt p = insertionMarker(x, y, w, MyDraw.textFieldH(), d);
			d.rect(Clr.WHITE, p.x, p.y, 1, AGame.FOUNT.lineHeight);
		}
	}
	
	public int multilineHeight(int w, MyDraw d) {
		int textHeight = (int) d.textSize(displayText(), AGame.FOUNT, w - MyDraw.PANEL_INSET * 2, 10000, 0, false).y;
		return textHeight + MyDraw.PANEL_INSET * 2 - 2;
	}
	
	public void renderMultilineIfNeeded(int x, int y, int w, MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		int h = multilineHeight(w, d);
		if (h <= MyDraw.textFieldH()) {
			render(x, y, w, d);
		} else {
			render(x, y, w, h, d, sm, hs, cursor);
		}
	}
	
	public void render(int x, int y, int w, int h, MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		insertClick(x, y, w, h, d);
		d.drawPanel(x, y, w, h, -1);
		d.rect(showValid ? Clr.DARK_GREY : INVALID, x + 2, y + 2, w - 4, h - 4);
		if (text.isEmpty()) {
			d.text("[GREY]" + help, AGame.FOUNT, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, w - MyDraw.PANEL_INSET * 2, h - MyDraw.PANEL_INSET, 0, true);
		}
		String inTxt = displayText();
		d.text(inTxt, AGame.FOUNT, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, w - MyDraw.PANEL_INSET * 2, h - MyDraw.PANEL_INSET, 0, false);
		if (focus && ((timeSinceReset) / 700) % 2 == 0) {
			Pt p = insertionMarker(x, y, w, h, d);
			d.rect(Clr.WHITE, p.x, p.y, 1, AGame.FOUNT.lineHeight);
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		insertionPoint = text.length();
	}
}
