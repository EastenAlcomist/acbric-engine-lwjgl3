package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import static com.zarkonnen.airships.Appearance.ROUNDING_FIX;
import static com.zarkonnen.airships.Appearance.currentPostfix;
import static com.zarkonnen.airships.Appearance.lsp;
import static com.zarkonnen.airships.Appearance.shaderLocked;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Fount;
import com.zarkonnen.catengine.Frame;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hook.Type;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Rect;
import com.zarkonnen.catengine.util.ScreenMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.GL_QUADS;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.shader.ShaderProgram;

public class MyDraw extends Draw {
	public static Clr SELECTED = Clr.fromHex("c8b84f");
	public static Clr DARK_BG = Clr.fromHex("613f2d");
	public int FUCK_NETBEANS = 9;
	public static String ERROR_C = "[" + new Clr(255, 153, 153).toString() + "]";
	
	public static Clr TITLE = Clr.fromHex("f5edb5");
	public static String TITLE_C = "[f5edb5]";
	public static String SELECTED_C = "[c8b84f]";
	
	public static int BUTTON_SPACING = 3;
	public static int SMALL_BUTTON_SPACING = 3;
	public static int MEDIUM_BUTTON_SPACING = 3;
	public static int LARGE_BUTTON_SPACING = 3;
	
	public static int UI_SPACING = 12;
	public static int SMALL_UI_SPACING = 12;
	public static int MEDIUM_UI_SPACING = 12;
	public static int LARGE_UI_SPACING = 12;
	
	public static Img TRIANGLE_CLOSED = new Img("ui", 144 + 16, 512, 16, 16, false);
	public static Img SMALL_TRIANGLE_CLOSED = new Img("ui", 144 + 16, 512, 16, 16, false);
	public static Img MEDIUM_TRIANGLE_CLOSED = new Img("ui", 144 + 16, 512, 16, 16, false);
	public static Img LARGE_TRIANGLE_CLOSED = new Img("ui", 144 + 16, 512, 16, 16, false);
	
	public static Img TRIANGLE_OPEN = new Img("ui", 144 + 32, 512, 16, 16, false);
	public static Img SMALL_TRIANGLE_OPEN = new Img("ui", 144 + 32, 512, 16, 16, false);
	public static Img MEDIUM_TRIANGLE_OPEN = new Img("ui", 144 + 32, 512, 16, 16, false);
	public static Img LARGE_TRIANGLE_OPEN = new Img("ui", 144 + 32, 512, 16, 16, false);	
	
	public static int PROGRESS_BAR_H = 24;
	public static int SMALL_PROGRESS_BAR_H = 24;
	public static int MEDIUM_PROGRESS_BAR_H = 24;
	public static int LARGE_PROGRESS_BAR_H = 24;
	
	public static Clr PROGRESS_BAR_INSIDE = Clr.fromHex("f5edb5");
	public static Clr PROGRESS_BAR_DEEP_INSIDE = Clr.fromHex("e2daa6");
	public static Clr PROGRESS_BAR_USAGE = Clr.fromHex("ffffff");
	public static Clr PROGRESS_BAR_INSUFFICIENT = new Clr(255, 153, 153);
	
	public static int SCROLL_EL_SPACING = 6;
	public static int SMALL_SCROLL_EL_SPACING = 6;
	public static int MEDIUM_SCROLL_EL_SPACING = 6;
	public static int LARGE_SCROLL_EL_SPACING = 6;
	
	public static int SIDE_CLEARANCE = 10;
	public static int SMALL_SIDE_CLEARANCE = 10;
	public static int MEDIUM_SIDE_CLEARANCE = 10;
	public static int LARGE_SIDE_CLEARANCE = 10;
	
	public static int TOP_BAR_H = 43;
	public static int SMALL_TOP_BAR_H = 43;
	public static int MEDIUM_TOP_BAR_H = 43;
	public static int LARGE_TOP_BAR_H = 43;
	
	public static int TOP_BAR_INSET = 11;
	public static int SMALL_TOP_BAR_INSET = 11;
	public static int MEDIUM_TOP_BAR_INSET = 11;
	public static int LARGE_TOP_BAR_INSET = 11;

	public static Img TOP_BAR_TOP_SEGMENT = new Img("ui", 18, 827, 66, 8, false);
	public static Img SMALL_TOP_BAR_TOP_SEGMENT = new Img("ui", 18, 827, 66, 8, false);
	public static Img MEDIUM_TOP_BAR_TOP_SEGMENT = new Img("ui", 18, 827, 66, 8, false);
	public static Img LARGE_TOP_BAR_TOP_SEGMENT = new Img("ui", 18, 827, 66, 8, false);
	
	public static Img TOP_BAR_BOTTOM_SEGMENT = new Img("ui", 18, 836, 66, 8, false);
	public static Img SMALL_TOP_BAR_BOTTOM_SEGMENT = new Img("ui", 18, 836, 66, 8, false);
	public static Img MEDIUM_TOP_BAR_BOTTOM_SEGMENT = new Img("ui", 18, 836, 66, 8, false);
	public static Img LARGE_TOP_BAR_BOTTOM_SEGMENT = new Img("ui", 18, 836, 66, 8, false);
	
	public static Img RHS_WINDOW_SIDE_SEGMENT = new Img("ui", 1, 828, 8, 60, false);
	public static Img SMALL_RHS_WINDOW_SIDE_SEGMENT = new Img("ui", 1, 828, 8, 60, false);
	public static Img MEDIUM_RHS_WINDOW_SIDE_SEGMENT = new Img("ui", 1, 828, 8, 60, false);
	public static Img LARGE_RHS_WINDOW_SIDE_SEGMENT = new Img("ui", 1, 828, 8, 60, false);

	public static Img LHS_WINDOW_SIDE_SEGMENT = new Img("ui", 10, 828, 8, 60, false);
	public static Img SMALL_LHS_WINDOW_SIDE_SEGMENT = new Img("ui", 10, 828, 8, 60, false);
	public static Img MEDIUM_LHS_WINDOW_SIDE_SEGMENT = new Img("ui", 10, 828, 8, 60, false);
	public static Img LARGE_LHS_WINDOW_SIDE_SEGMENT = new Img("ui", 10, 828, 8, 60, false);
	
	public static int PANEL_INSET = 6;
	public static int SMALL_PANEL_INSET = 6;
	public static int MEDIUM_PANEL_INSET = 6;
	public static int LARGE_PANEL_INSET = 6;
	
	public static int PANEL_BORDER_W = 2;
	public static int SMALL_PANEL_BORDER_W = 2;
	public static int MEDIUM_PANEL_BORDER_W = 2;
	public static int LARGE_PANEL_BORDER_W = 2;

	public static Img PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	public static Img SMALL_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	public static Img MEDIUM_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	public static Img LARGE_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	
	public static Img PANEL_TR = new Img("ui", 55, 856, 5, 5, false);
	public static Img SMALL_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);
	public static Img MEDIUM_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);
	public static Img LARGE_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);

	public static Img PANEL_BL = new Img("ui", 49, 862, 5, 5, false);
	public static Img SMALL_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);
	public static Img MEDIUM_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);
	public static Img LARGE_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);

	public static Img PANEL_BR = new Img("ui", 55, 862, 5, 5, false);
	public static Img SMALL_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);
	public static Img MEDIUM_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);
	public static Img LARGE_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);

	public static Img PANEL_L = new Img("ui", 39, 846, 4, 4, false);
	public static Img SMALL_PANEL_L = new Img("ui", 39, 846, 4, 4, false);
	public static Img MEDIUM_PANEL_L = new Img("ui", 39, 846, 4, 4, false);
	public static Img LARGE_PANEL_L = new Img("ui", 39, 846, 4, 4, false);

	public static Img PANEL_R = new Img("ui", 44, 846, 4, 4, false);
	public static Img SMALL_PANEL_R = new Img("ui", 44, 846, 4, 4, false);
	public static Img MEDIUM_PANEL_R = new Img("ui", 44, 846, 4, 4, false);
	public static Img LARGE_PANEL_R = new Img("ui", 44, 846, 4, 4, false);

	public static Img PANEL_T = new Img("ui", 49, 846, 4, 4, false);
	public static Img SMALL_PANEL_T = new Img("ui", 49, 846, 4, 4, false);
	public static Img MEDIUM_PANEL_T = new Img("ui", 49, 846, 4, 4, false);
	public static Img LARGE_PANEL_T = new Img("ui", 49, 846, 4, 4, false);

	public static Img PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	public static Img SMALL_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	public static Img MEDIUM_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	public static Img LARGE_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	
	public static Img S_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	public static Img SMALL_S_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	public static Img MEDIUM_S_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	public static Img LARGE_S_PANEL_TL = new Img("ui", 49, 856, 5, 5, false);
	
	public static Img S_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);
	public static Img SMALL_S_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);
	public static Img MEDIUM_S_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);
	public static Img LARGE_S_PANEL_TR = new Img("ui", 55, 856, 5, 5, false);

	public static Img S_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);
	public static Img SMALL_S_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);
	public static Img MEDIUM_S_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);
	public static Img LARGE_S_PANEL_BL = new Img("ui", 49, 862, 5, 5, false);

	public static Img S_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);
	public static Img SMALL_S_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);
	public static Img MEDIUM_S_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);
	public static Img LARGE_S_PANEL_BR = new Img("ui", 55, 862, 5, 5, false);

	public static Img S_PANEL_L = new Img("ui", 39, 846, 4, 4, false);
	public static Img SMALL_S_PANEL_L = new Img("ui", 39, 846, 4, 4, false);
	public static Img MEDIUM_S_PANEL_L = new Img("ui", 39, 846, 4, 4, false);
	public static Img LARGE_S_PANEL_L = new Img("ui", 39, 846, 4, 4, false);

	public static Img S_PANEL_R = new Img("ui", 44, 846, 4, 4, false);
	public static Img SMALL_S_PANEL_R = new Img("ui", 44, 846, 4, 4, false);
	public static Img MEDIUM_S_PANEL_R = new Img("ui", 44, 846, 4, 4, false);
	public static Img LARGE_S_PANEL_R = new Img("ui", 44, 846, 4, 4, false);

	public static Img S_PANEL_T = new Img("ui", 49, 846, 4, 4, false);
	public static Img SMALL_S_PANEL_T = new Img("ui", 49, 846, 4, 4, false);
	public static Img MEDIUM_S_PANEL_T = new Img("ui", 49, 846, 4, 4, false);
	public static Img LARGE_S_PANEL_T = new Img("ui", 49, 846, 4, 4, false);

	public static Img S_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	public static Img SMALL_S_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	public static Img MEDIUM_S_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	public static Img LARGE_S_PANEL_B = new Img("ui", 49, 851, 4, 4, false);
	
	public static int WINDOW_INSET = 12;
	public static int SMALL_WINDOW_INSET = 12;
	public static int MEDIUM_WINDOW_INSET = 12;
	public static int LARGE_WINDOW_INSET = 12;

	public static Clr WIN_SHADOW = new Clr(0, 0, 0, 91);
	
	public static Img WIN_TL = new Img("ui", 19, 845, 9, 9, false);
	public static Img SMALL_WIN_TL = new Img("ui", 19, 845, 9, 9, false);
	public static Img MEDIUM_WIN_TL = new Img("ui", 19, 845, 9, 9, false);
	public static Img LARGE_WIN_TL = new Img("ui", 19, 845, 9, 9, false);

	public static Img WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	public static Img SMALL_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	public static Img MEDIUM_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	public static Img LARGE_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	
	public static Img WIN_BL = new Img("ui", 19, 855, 9, 9, false);
	public static Img SMALL_WIN_BL = new Img("ui", 19, 855, 9, 9, false);
	public static Img MEDIUM_WIN_BL = new Img("ui", 19, 855, 9, 9, false);
	public static Img LARGE_WIN_BL = new Img("ui", 19, 855, 9, 9, false);

	public static Img WIN_BR = new Img("ui", 29, 855, 9, 9, false);
	public static Img SMALL_WIN_BR = new Img("ui", 29, 855, 9, 9, false);
	public static Img MEDIUM_WIN_BR = new Img("ui", 29, 855, 9, 9, false);
	public static Img LARGE_WIN_BR = new Img("ui", 29, 855, 9, 9, false);

	public static Img WIN_L = new Img("ui", 1, 828, 8, 6, false);
	public static Img SMALL_WIN_L = new Img("ui", 1, 828, 8, 6, false);
	public static Img MEDIUM_WIN_L = new Img("ui", 1, 828, 8, 6, false);
	public static Img LARGE_WIN_L = new Img("ui", 1, 828, 8, 6, false);

	public static Img WIN_R = new Img("ui", 10, 828, 8, 6, false);
	public static Img SMALL_WIN_R = new Img("ui", 10, 828, 8, 6, false);
	public static Img MEDIUM_WIN_R = new Img("ui", 10, 828, 8, 6, false);
	public static Img LARGE_WIN_R = new Img("ui", 10, 828, 8, 6, false);

	public static Img WIN_T = new Img("ui", 18, 827, 6, 8, false);
	public static Img SMALL_WIN_T = new Img("ui", 18, 827, 6, 8, false);
	public static Img MEDIUM_WIN_T = new Img("ui", 18, 827, 6, 8, false);
	public static Img LARGE_WIN_T = new Img("ui", 18, 827, 6, 8, false);

	public static Img WIN_B = new Img("ui", 18, 836, 6, 8, false);
	public static Img SMALL_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	public static Img MEDIUM_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	public static Img LARGE_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	
	public static Img S_WIN_TL = new Img("ui", 19, 845, 9, 9, false);
	public static Img SMALL_S_WIN_TL = new Img("ui", 19, 845, 9, 9, false);
	public static Img MEDIUM_S_WIN_TL = new Img("ui", 19, 845, 9, 9, false);
	public static Img LARGE_S_WIN_TL = new Img("ui", 19, 845, 9, 9, false);

	public static Img S_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	public static Img SMALL_S_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	public static Img MEDIUM_S_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	public static Img LARGE_S_WIN_TR = new Img("ui", 29, 845, 9, 9, false);
	
	public static Img S_WIN_BL = new Img("ui", 19, 855, 9, 9, false);
	public static Img SMALL_S_WIN_BL = new Img("ui", 19, 855, 9, 9, false);
	public static Img MEDIUM_S_WIN_BL = new Img("ui", 19, 855, 9, 9, false);
	public static Img LARGE_S_WIN_BL = new Img("ui", 19, 855, 9, 9, false);

	public static Img S_WIN_BR = new Img("ui", 29, 855, 9, 9, false);
	public static Img SMALL_S_WIN_BR = new Img("ui", 29, 855, 9, 9, false);
	public static Img MEDIUM_S_WIN_BR = new Img("ui", 29, 855, 9, 9, false);
	public static Img LARGE_S_WIN_BR = new Img("ui", 29, 855, 9, 9, false);

	public static Img S_WIN_L = new Img("ui", 1, 828, 8, 6, false);
	public static Img SMALL_S_WIN_L = new Img("ui", 1, 828, 8, 6, false);
	public static Img MEDIUM_S_WIN_L = new Img("ui", 1, 828, 8, 6, false);
	public static Img LARGE_S_WIN_L = new Img("ui", 1, 828, 8, 6, false);

	public static Img S_WIN_R = new Img("ui", 10, 828, 8, 6, false);
	public static Img SMALL_S_WIN_R = new Img("ui", 10, 828, 8, 6, false);
	public static Img MEDIUM_S_WIN_R = new Img("ui", 10, 828, 8, 6, false);
	public static Img LARGE_S_WIN_R = new Img("ui", 10, 828, 8, 6, false);

	public static Img S_WIN_T = new Img("ui", 18, 827, 6, 8, false);
	public static Img SMALL_S_WIN_T = new Img("ui", 18, 827, 6, 8, false);
	public static Img MEDIUM_S_WIN_T = new Img("ui", 18, 827, 6, 8, false);
	public static Img LARGE_S_WIN_T = new Img("ui", 18, 827, 6, 8, false);

	public static Img S_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	public static Img SMALL_S_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	public static Img MEDIUM_S_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	public static Img LARGE_S_WIN_B = new Img("ui", 18, 836, 6, 8, false);
	
	public static int BUTTON_H = 24;
	public static int SMALL_BUTTON_H = 24;
	public static int MEDIUM_BUTTON_H = 24;
	public static int LARGE_BUTTON_H = 24;

	public static Img BUTTON_DIS_START = new Img("ui", 1, 889, 17, 26, false);
	public static Img SMALL_BUTTON_DIS_START = new Img("ui", 1, 889, 17, 26, false);
	public static Img MEDIUM_BUTTON_DIS_START = new Img("ui", 1, 889, 17, 26, false);
	public static Img LARGE_BUTTON_DIS_START = new Img("ui", 1, 889, 17, 26, false);

	public static Img BUTTON_DIS_MIDDLE = new Img("ui", 18, 889, 12, 26, false);
	public static Img SMALL_BUTTON_DIS_MIDDLE = new Img("ui", 18, 889, 12, 26, false);
	public static Img MEDIUM_BUTTON_DIS_MIDDLE = new Img("ui", 18, 889, 12, 26, false);
	public static Img LARGE_BUTTON_DIS_MIDDLE = new Img("ui", 18, 889, 12, 26, false);

	public static Img BUTTON_DIS_END = new Img("ui", 86, 889, 17, 26, false);
	public static Img SMALL_BUTTON_DIS_END = new Img("ui", 86, 889, 17, 26, false);
	public static Img MEDIUM_BUTTON_DIS_END = new Img("ui", 86, 889, 17, 26, false);
	public static Img LARGE_BUTTON_DIS_END = new Img("ui", 86, 889, 17, 26, false);
	
	public static Img BUTTON_START = new Img("ui", 1, 916, 17, 26, false);
	public static Img SMALL_BUTTON_START = new Img("ui", 1, 916, 17, 26, false);
	public static Img MEDIUM_BUTTON_START = new Img("ui", 1, 916, 17, 26, false);
	public static Img LARGE_BUTTON_START = new Img("ui", 1, 916, 17, 26, false);

	public static Img BUTTON_MIDDLE = new Img("ui", 18, 916, 12, 26, false);
	public static Img SMALL_BUTTON_MIDDLE = new Img("ui", 18, 916, 12, 26, false);
	public static Img MEDIUM_BUTTON_MIDDLE = new Img("ui", 18, 916, 12, 26, false);
	public static Img LARGE_BUTTON_MIDDLE = new Img("ui", 18, 916, 12, 26, false);

	public static Img BUTTON_END = new Img("ui", 86, 916, 17, 26, false);
	public static Img SMALL_BUTTON_END = new Img("ui", 86, 916, 17, 26, false);
	public static Img MEDIUM_BUTTON_END = new Img("ui", 86, 916, 17, 26, false);
	public static Img LARGE_BUTTON_END = new Img("ui", 86, 916, 17, 26, false);
	
	public static Img BUTTON_LIT_START = new Img("ui", 1, 943, 17, 26, false);
	public static Img SMALL_BUTTON_LIT_START = new Img("ui", 1, 943, 17, 26, false);
	public static Img MEDIUM_BUTTON_LIT_START = new Img("ui", 1, 943, 17, 26, false);
	public static Img LARGE_BUTTON_LIT_START = new Img("ui", 1, 943, 17, 26, false);

	public static Img BUTTON_LIT_MIDDLE = new Img("ui", 18, 943, 12, 26, false);
	public static Img SMALL_BUTTON_LIT_MIDDLE = new Img("ui", 18, 943, 12, 26, false);
	public static Img MEDIUM_BUTTON_LIT_MIDDLE = new Img("ui", 18, 943, 12, 26, false);
	public static Img LARGE_BUTTON_LIT_MIDDLE = new Img("ui", 18, 943, 12, 26, false);

	public static Img BUTTON_LIT_END = new Img("ui", 86, 943, 17, 26, false);
	public static Img SMALL_BUTTON_LIT_END = new Img("ui", 86, 943, 17, 26, false);
	public static Img MEDIUM_BUTTON_LIT_END = new Img("ui", 86, 943, 17, 26, false);
	public static Img LARGE_BUTTON_LIT_END = new Img("ui", 86, 943, 17, 26, false);
	
	public static Img TOGGLE_OFF_START = new Img("ui", 1, 970, 17, 26, false);
	public static Img SMALL_TOGGLE_OFF_START = new Img("ui", 1, 970, 17, 26, false);
	public static Img MEDIUM_TOGGLE_OFF_START = new Img("ui", 1, 970, 17, 26, false);
	public static Img LARGE_TOGGLE_OFF_START = new Img("ui", 1, 970, 17, 26, false);

	public static Img TOGGLE_OFF_MIDDLE = new Img("ui", 18, 970, 12, 26, false);
	public static Img SMALL_TOGGLE_OFF_MIDDLE = new Img("ui", 18, 970, 12, 26, false);
	public static Img MEDIUM_TOGGLE_OFF_MIDDLE = new Img("ui", 18, 970, 12, 26, false);
	public static Img LARGE_TOGGLE_OFF_MIDDLE = new Img("ui", 18, 970, 12, 26, false);

	public static Img TOGGLE_OFF_END = new Img("ui", 86, 970, 17, 26, false);
	public static Img SMALL_TOGGLE_OFF_END = new Img("ui", 86, 970, 17, 26, false);
	public static Img MEDIUM_TOGGLE_OFF_END = new Img("ui", 86, 970, 17, 26, false);
	public static Img LARGE_TOGGLE_OFF_END = new Img("ui", 86, 970, 17, 26, false);
	
	public static Img TOGGLE_ON_START = new Img("ui", 1, 997, 17, 26, false);
	public static Img SMALL_TOGGLE_ON_START = new Img("ui", 1, 997, 17, 26, false);
	public static Img MEDIUM_TOGGLE_ON_START = new Img("ui", 1, 997, 17, 26, false);
	public static Img LARGE_TOGGLE_ON_START = new Img("ui", 1, 997, 17, 26, false);

	public static Img TOGGLE_ON_MIDDLE = new Img("ui", 18, 997, 12, 26, false);
	public static Img SMALL_TOGGLE_ON_MIDDLE = new Img("ui", 18, 997, 12, 26, false);
	public static Img MEDIUM_TOGGLE_ON_MIDDLE = new Img("ui", 18, 997, 12, 26, false);
	public static Img LARGE_TOGGLE_ON_MIDDLE = new Img("ui", 18, 997, 12, 26, false);

	public static Img TOGGLE_ON_END = new Img("ui", 86, 997, 17, 26, false);
	public static Img SMALL_TOGGLE_ON_END = new Img("ui", 86, 997, 17, 26, false);
	public static Img MEDIUM_TOGGLE_ON_END = new Img("ui", 86, 997, 17, 26, false);
	public static Img LARGE_TOGGLE_ON_END = new Img("ui", 86, 997, 17, 26, false);
	
	public static Img TOGGLE_OFF_LIT_START = new Img("ui", 105, 970, 17, 26, false);
	public static Img SMALL_TOGGLE_OFF_LIT_START = new Img("ui", 105, 970, 17, 26, false);
	public static Img MEDIUM_TOGGLE_OFF_LIT_START = new Img("ui", 105, 970, 17, 26, false);
	public static Img LARGE_TOGGLE_OFF_LIT_START = new Img("ui", 105, 970, 17, 26, false);

	public static Img TOGGLE_OFF_LIT_MIDDLE = new Img("ui", 122, 970, 12, 26, false);
	public static Img SMALL_TOGGLE_OFF_LIT_MIDDLE = new Img("ui", 122, 970, 12, 26, false);
	public static Img MEDIUM_TOGGLE_OFF_LIT_MIDDLE = new Img("ui", 122, 970, 12, 26, false);
	public static Img LARGE_TOGGLE_OFF_LIT_MIDDLE = new Img("ui", 122, 970, 12, 26, false);

	public static Img TOGGLE_OFF_LIT_END = new Img("ui", 190, 970, 17, 26, false);
	public static Img SMALL_TOGGLE_OFF_LIT_END = new Img("ui", 190, 970, 17, 26, false);
	public static Img MEDIUM_TOGGLE_OFF_LIT_END = new Img("ui", 190, 970, 17, 26, false);
	public static Img LARGE_TOGGLE_OFF_LIT_END = new Img("ui", 190, 970, 17, 26, false);
	
	public static Img TOGGLE_ON_LIT_START = new Img("ui", 105, 997, 17, 26, false);
	public static Img SMALL_TOGGLE_ON_LIT_START = new Img("ui", 105, 997, 17, 26, false);
	public static Img MEDIUM_TOGGLE_ON_LIT_START = new Img("ui", 105, 997, 17, 26, false);
	public static Img LARGE_TOGGLE_ON_LIT_START = new Img("ui", 105, 997, 17, 26, false);

	public static Img TOGGLE_ON_LIT_MIDDLE = new Img("ui", 122, 997, 12, 26, false);
	public static Img SMALL_TOGGLE_ON_LIT_MIDDLE = new Img("ui", 122, 997, 12, 26, false);
	public static Img MEDIUM_TOGGLE_ON_LIT_MIDDLE = new Img("ui", 122, 997, 12, 26, false);
	public static Img LARGE_TOGGLE_ON_LIT_MIDDLE = new Img("ui", 122, 997, 12, 26, false);

	public static Img TOGGLE_ON_LIT_END = new Img("ui", 190, 997, 17, 26, false);
	public static Img SMALL_TOGGLE_ON_LIT_END = new Img("ui", 190, 997, 17, 26, false);
	public static Img MEDIUM_TOGGLE_ON_LIT_END = new Img("ui", 190, 997, 17, 26, false);
	public static Img LARGE_TOGGLE_ON_LIT_END = new Img("ui", 190, 997, 17, 26, false);
	
	public static Img SLIDER_KNOB = new Img("ui", 303, 832, 8, 8, false);
	public static Img SMALL_SLIDER_KNOB = new Img("ui", 303, 832, 8, 8, false);
	public static Img MEDIUM_SLIDER_KNOB = new Img("ui", 303, 832, 8, 8, false);
	public static Img LARGE_SLIDER_KNOB = new Img("ui", 303, 832, 8, 8, false);
	
	public static int BUTTON_EXTRA_W = 40;
	public static int SMALL_BUTTON_EXTRA_W = 40;
	public static int MEDIUM_BUTTON_EXTRA_W = 40;
	public static int LARGE_BUTTON_EXTRA_W = 40;

	public static int TOGGLE_EXTRA_W = 60;
	public static int SMALL_TOGGLE_EXTRA_W = 60;
	public static int MEDIUM_TOGGLE_EXTRA_W = 60;
	public static int LARGE_TOGGLE_EXTRA_W = 60;
	
	public static int BIG_BUTTON_H = 44;
	public static int SMALL_BIG_BUTTON_H = 44;
	public static int MEDIUM_BIG_BUTTON_H = 44;
	public static int LARGE_BIG_BUTTON_H = 44;

	public static Img BB_TL = new Img("ui", 1, 916, 15, 4, false);
	public static Img SMALL_BB_TL = new Img("ui", 1, 916, 15, 4, false);
	public static Img MEDIUM_BB_TL = new Img("ui", 1, 916, 15, 4, false);
	public static Img LARGE_BB_TL = new Img("ui", 1, 916, 15, 4, false);

	public static Img BB_T = new Img("ui", 16, 916, 12, 4, false);
	public static Img SMALL_BB_T = new Img("ui", 16, 916, 12, 4, false);
	public static Img MEDIUM_BB_T = new Img("ui", 16, 916, 12, 4, false);
	public static Img LARGE_BB_T = new Img("ui", 16, 916, 12, 4, false);

	public static Img BB_TR = new Img("ui", 87, 916, 16, 4, false);
	public static Img SMALL_BB_TR = new Img("ui", 87, 916, 16, 4, false);
	public static Img MEDIUM_BB_TR = new Img("ui", 87, 916, 16, 4, false);
	public static Img LARGE_BB_TR = new Img("ui", 87, 916, 16, 4, false);

	public static Img BB_R = new Img("ui", 87, 920, 16, 4, false);
	public static Img SMALL_BB_R = new Img("ui", 87, 920, 16, 4, false);
	public static Img MEDIUM_BB_R = new Img("ui", 87, 920, 16, 4, false);
	public static Img LARGE_BB_R = new Img("ui", 87, 920, 16, 4, false);

	public static Img BB_L = new Img("ui", 1, 920, 15, 4, false);
	public static Img SMALL_BB_L = new Img("ui", 1, 920, 15, 4, false);
	public static Img MEDIUM_BB_L = new Img("ui", 1, 920, 15, 4, false);
	public static Img LARGE_BB_L = new Img("ui", 1, 920, 15, 4, false);

	public static Img BB_BL = new Img("ui", 1, 938, 15, 4, false);
	public static Img SMALL_BB_BL = new Img("ui", 1, 938, 15, 4, false);
	public static Img MEDIUM_BB_BL = new Img("ui", 1, 938, 15, 4, false);
	public static Img LARGE_BB_BL = new Img("ui", 1, 938, 15, 4, false);

	public static Img BB_B = new Img("ui", 16, 938, 12, 4, false);
	public static Img SMALL_BB_B = new Img("ui", 16, 938, 12, 4, false);
	public static Img MEDIUM_BB_B = new Img("ui", 16, 938, 12, 4, false);
	public static Img LARGE_BB_B = new Img("ui", 16, 938, 12, 4, false);

	public static Img BB_BR = new Img("ui", 87, 938, 16, 4, false);
	public static Img SMALL_BB_BR = new Img("ui", 87, 938, 16, 4, false);
	public static Img MEDIUM_BB_BR = new Img("ui", 87, 938, 16, 4, false);
	public static Img LARGE_BB_BR = new Img("ui", 87, 938, 16, 4, false);
	
	public static Img HBB_TL = new Img("ui", 1, 916 + 27, 15, 4, false);
	public static Img SMALL_HBB_TL = new Img("ui", 1, 916 + 27, 15, 4, false);
	public static Img MEDIUM_HBB_TL = new Img("ui", 1, 916 + 27, 15, 4, false);
	public static Img LARGE_HBB_TL = new Img("ui", 1, 916 + 27, 15, 4, false);

	public static Img HBB_T = new Img("ui", 16, 916 + 27, 12, 4, false);
	public static Img SMALL_HBB_T = new Img("ui", 16, 916 + 27, 12, 4, false);
	public static Img MEDIUM_HBB_T = new Img("ui", 16, 916 + 27, 12, 4, false);
	public static Img LARGE_HBB_T = new Img("ui", 16, 916 + 27, 12, 4, false);

	public static Img HBB_TR = new Img("ui", 87, 916 + 27, 16, 4, false);
	public static Img SMALL_HBB_TR = new Img("ui", 87, 916 + 27, 16, 4, false);
	public static Img MEDIUM_HBB_TR = new Img("ui", 87, 916 + 27, 16, 4, false);
	public static Img LARGE_HBB_TR = new Img("ui", 87, 916 + 27, 16, 4, false);

	public static Img HBB_R = new Img("ui", 87, 920 + 27, 16, 4, false);
	public static Img SMALL_HBB_R = new Img("ui", 87, 920 + 27, 16, 4, false);
	public static Img MEDIUM_HBB_R = new Img("ui", 87, 920 + 27, 16, 4, false);
	public static Img LARGE_HBB_R = new Img("ui", 87, 920 + 27, 16, 4, false);

	public static Img HBB_L = new Img("ui", 1, 920 + 27, 15, 4, false);
	public static Img SMALL_HBB_L = new Img("ui", 1, 920 + 27, 15, 4, false);
	public static Img MEDIUM_HBB_L = new Img("ui", 1, 920 + 27, 15, 4, false);
	public static Img LARGE_HBB_L = new Img("ui", 1, 920 + 27, 15, 4, false);

	public static Img HBB_BL = new Img("ui", 1, 938 + 27, 15, 4, false);
	public static Img SMALL_HBB_BL = new Img("ui", 1, 938 + 27, 15, 4, false);
	public static Img MEDIUM_HBB_BL = new Img("ui", 1, 938 + 27, 15, 4, false);
	public static Img LARGE_HBB_BL = new Img("ui", 1, 938 + 27, 15, 4, false);

	public static Img HBB_B = new Img("ui", 16, 938 + 27, 12, 4, false);
	public static Img SMALL_HBB_B = new Img("ui", 16, 938 + 27, 12, 4, false);
	public static Img MEDIUM_HBB_B = new Img("ui", 16, 938 + 27, 12, 4, false);
	public static Img LARGE_HBB_B = new Img("ui", 16, 938 + 27, 12, 4, false);

	public static Img HBB_BR = new Img("ui", 87, 938 + 27, 16, 4, false);
	public static Img SMALL_HBB_BR = new Img("ui", 87, 938 + 27, 16, 4, false);
	public static Img MEDIUM_HBB_BR = new Img("ui", 87, 938 + 27, 16, 4, false);
	public static Img LARGE_HBB_BR = new Img("ui", 87, 938 + 27, 16, 4, false);
	
	public static Clr BB_CENTER = Clr.fromHex("c8b84f");
	public static Clr HBB_CENTER = Clr.fromHex("f5edb5");
	
	public static Img ICON_BUTTON = new Img("ui", 170, 852, 26, 26, false);
	public static Img SMALL_ICON_BUTTON = new Img("ui", 170, 852, 26, 26, false);
	public static Img MEDIUM_ICON_BUTTON = new Img("ui", 170, 852, 26, 26, false);
	public static Img LARGE_ICON_BUTTON = new Img("ui", 170, 852, 26, 26, false);

	public static Img ICON_BUTTON_LIT = new Img("ui", 143, 852, 26, 26, false);
	public static Img SMALL_ICON_BUTTON_LIT = new Img("ui", 143, 852, 26, 26, false);
	public static Img MEDIUM_ICON_BUTTON_LIT = new Img("ui", 143, 852, 26, 26, false);
	public static Img LARGE_ICON_BUTTON_LIT = new Img("ui", 143, 852, 26, 26, false);

	public static Img ICON_TOGGLE_OFF = new Img("ui", 197, 852, 26, 26, false);
	public static Img SMALL_ICON_TOGGLE_OFF = new Img("ui", 197, 852, 26, 26, false);
	public static Img MEDIUM_ICON_TOGGLE_OFF = new Img("ui", 197, 852, 26, 26, false);
	public static Img LARGE_ICON_TOGGLE_OFF = new Img("ui", 197, 852, 26, 26, false);

	public static Img ICON_TOGGLE_ON = new Img("ui", 224, 852, 26, 26, false);
	public static Img SMALL_ICON_TOGGLE_ON = new Img("ui", 224, 852, 26, 26, false);
	public static Img MEDIUM_ICON_TOGGLE_ON = new Img("ui", 224, 852, 26, 26, false);
	public static Img LARGE_ICON_TOGGLE_ON = new Img("ui", 224, 852, 26, 26, false);

	public static Img ICON_BUTTON_DISABLED = new Img("ui", 143, 879, 26, 26, false);
	public static Img SMALL_ICON_BUTTON_DISABLED = new Img("ui", 143, 879, 26, 26, false);
	public static Img MEDIUM_ICON_BUTTON_DISABLED = new Img("ui", 143, 879, 26, 26, false);
	public static Img LARGE_ICON_BUTTON_DISABLED = new Img("ui", 143, 879, 26, 26, false);
	
	public static Clr ICON_TINT = Clr.fromHex("4a3f2f");
	public static Clr LIT_ICON_TINT = Clr.fromHex("6c6149");
	
	public static int ICON_BUTTON_SZ = 24;
	public static int SMALL_ICON_BUTTON_SZ = 24;
	public static int MEDIUM_ICON_BUTTON_SZ = 24;
	public static int LARGE_ICON_BUTTON_SZ = 24;
	
	public static Clr TOGGLE_OFF_INSIDE = Clr.fromHex("665b2e");
	
	public static Img WIN_BG = new Img("ui", 0, 0, 256, 256, false);
	public static Img DARK_WIN_BG = new Img("ui", 256, 0, 256, 256, false);
	public static Clr DESK = Clr.fromHex("341e12");
	public static Img SCREEN_BG = new Img("cast_iron", 0, 0, 800, 600, false);
	
	public static int BUTTON_TEXT_Y_OFFSET = 2;
	public static int SMALL_BUTTON_TEXT_Y_OFFSET = 2;
	public static int MEDIUM_BUTTON_TEXT_Y_OFFSET = 2;
	public static int LARGE_BUTTON_TEXT_Y_OFFSET = 2;

	public static String BUTTON_TEXT_COLOR = "[463a26]";
	public static String BUTTON_HOTKEY_COLOR = "[726534]";
	
	public static int BIG_BUTTON_TEXT_Y_OFFSET = -1;
	public static int SMALL_BIG_BUTTON_TEXT_Y_OFFSET = -1;
	public static int MEDIUM_BIG_BUTTON_TEXT_Y_OFFSET = -1;
	public static int LARGE_BIG_BUTTON_TEXT_Y_OFFSET = -1;

	public static Clr UI_GLOW_COLOR = new Clr(255, 255, 255, 37);
	public static Clr GOLD_BUTTON_GLOW = new Clr(245, 237, 181, 80);
	
	public static int textFieldH() {
		return AGame.FOUNT.height + PANEL_INSET * 2 - 2;
	}
	
	
	public static class State {
		Cursor cursorAppearance = null;
		String cursorText = null;
		String tooltipText = "";
		String newtooltipText = "";
		int maxTooltipX = -1;
		int ticksSinceMouseMoved = 0;
		int time = 0;
		public IntRect hookClipRect;
		public HashMap<UIGlowRect, UIGlowRect> glowRects = new HashMap<UIGlowRect, UIGlowRect>();
		public State() {
			resetCursor();
		}
		public void addGlowRect(int x, int y, int w, int h) {
			if (hookClipRect != null) {
				w = Math.min(x + w, hookClipRect.x + hookClipRect.w) - Math.max(hookClipRect.x, x);
				h = Math.min(y + h, hookClipRect.y + hookClipRect.h) - Math.max(hookClipRect.y, y);
				if (w <= 0 || h <= 0) {
					return;
				}
				x = Math.max(hookClipRect.x, x);
				y = Math.max(hookClipRect.y, y);
			}
			UIGlowRect r = new UIGlowRect(x, y, w, h);
			if (glowRects.containsKey(r)) {
				r = glowRects.get(r);
				r.confirmed = true;
			} else {
				glowRects.put(r, r);
			}
		}
		public void clearGlowRects() {
			glowRects.clear();
		}
		public void removeIntersectingGlowRects(int x, int y, int w, int h) {
			for (Iterator<UIGlowRect> it = glowRects.keySet().iterator(); it.hasNext();) {
				UIGlowRect gr = it.next();
				if (Rect2D.intersects(gr.x, gr.y, gr.w, gr.h, x, y, w, h)) {
					it.remove();
				}
			}
		}
		public void resetCursor() {
			try {
				cursorText = null;
				cursorAppearance = Cursor.ofName("POINTER");
			} catch (Exception e) {
				cursorAppearance = null;
			}
		}
		public void setCursor(String cursorName, String cursorText) {
			this.cursorAppearance = Cursor.ofName(cursorName);
			this.cursorText = cursorText;
		}
		int msSinceLastClick = 0;
		Pt cursor = new Pt(0, 0);
		public void tick(int ms, Pt cursor) {
			time += ms;
			msSinceLastClick += ms;
			if (!tooltipText.equals(newtooltipText)) {
				if (this.cursor == null || cursor == null || !this.cursor.equals(cursor)) {
					ticksSinceMouseMoved = 0;
				}
				tooltipText = newtooltipText;
			}
			newtooltipText = "";
			this.cursor = cursor;
			ticksSinceMouseMoved++;
		}
		public boolean canClick() { return msSinceLastClick > 100; }
		public void hasClicked() { msSinceLastClick = 0; }
	}
	
	public State state;
	public Integration integration;
		
	public MyDraw(Frame f, State state, Integration integration) {
		super(f);
		this.state = state;
		this.integration = integration;
	}

	public MyDraw(Frame f, Hooks hs, State state, Integration integration) {
		super(f, hs);
		this.state = state;
		this.integration = integration;
	}
	
	public void highlight(String key, AirshipGame g, int x, int y, int w, int h) {
		g.highlights.put(key, new IntRect(x, y, w, h));
	}
	
	public void renderHighlights(AirshipGame g) {
		for (String key : g.highlights.keySet()) {
			Pt start = g.highlitStart(key);
			if (start == null) { continue; }
			IntRect r = g.highlights.get(key);
			Graphics gfx = (Graphics) frame().nativeRenderer();
			double dx = r.x + r.w / 2 - start.x;
			double dy = r.y + r.h / 2 - start.y;
			double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
			dx /= dist;
			dy /= dist;
			double rot90Dx = dy;
			double rot90Dy = -dx;
			gfx.setColor(Color.black);
			gfx.setLineWidth(4);
			gfx.drawLine((float) start.x, (float) start.y, r.x + r.w / 2 - (float) dx * 4, r.y + r.h / 2 - (float) dy * 4);
			Polygon blackHead = new Polygon(new float[] {
				r.x + r.w / 2, r.y + r.h / 2,
				r.x + r.w / 2 - (float) dx * 16 + (float) rot90Dx * 8, r.y + r.h / 2 - (float) dy * 16 + (float) rot90Dy * 8,
				r.x + r.w / 2 - (float) dx * 16 - (float) rot90Dx * 8, r.y + r.h / 2 - (float) dy * 16 - (float) rot90Dy * 8
			});
			gfx.fill(blackHead);
			gfx.setColor(Color.white);
			gfx.setLineWidth(2);
			gfx.drawLine((float) start.x, (float) start.y, r.x + r.w / 2 - (float) dx * 4, r.y + r.h / 2 - (float) dy * 4);
			Polygon whiteHead = new Polygon(new float[] {
				r.x + r.w / 2 - (float) dx * 2, r.y + r.h / 2 - (float) dy * 2,
				r.x + r.w / 2 - (float) dx * 15 + (float) rot90Dx * 6, r.y + r.h / 2 - (float) dy * 15 + (float) rot90Dy * 6,
				r.x + r.w / 2 - (float) dx * 15 - (float) rot90Dx * 6, r.y + r.h / 2 - (float) dy * 15 - (float) rot90Dy * 6
			});
			gfx.fill(whiteHead);
			gfx.setLineWidth(1);
		}
		g.highlights.clear();
		/*rect(Clr.WHITE, x - 3, y - 3, w + 6, 3);
		rect(Clr.WHITE, x - 3, y + h, w + 6, 3);
		rect(Clr.WHITE, x - 3, y - 3, 3, h + 6);
		rect(Clr.WHITE, x + w, y - 3, 3, h + 6);
		
		rect(Clr.BLACK, x - 2, y - 2, w + 4, 1);
		rect(Clr.BLACK, x - 2, y + h + 1, w + 4, 1);
		rect(Clr.BLACK, x - 2, y - 2, 1, h + 4);
		rect(Clr.BLACK, x + w + 1, y - 2, 1, h + 4);*/
	}
	
	public void drawBG(Img img, ScreenMode sm) {
		double imgAspect = img.srcWidth * 1.0 / img.srcHeight;
		double screenAspect = sm.width * 1.0 / sm.height;
		if (screenAspect >= imgAspect) {
			// Screen wider than image, cut off top/bottom of image.
			blit(img, 0, sm.height / 2 - sm.height * screenAspect / imgAspect / 2, sm.width, sm.height * screenAspect / imgAspect);
		} else {
			// Screen taller than image, cut off left/right of image.
			blit(img, sm.width / 2 - sm.width * imgAspect / screenAspect / 2, 0, sm.width * imgAspect / screenAspect, sm.height);
		}
	}
	
	public void progressBar(int x, int y, int w, double progress) {
		drawPanel(x, y, w, PROGRESS_BAR_H, -1);
		rect(PROGRESS_BAR_INSIDE, x + 2, y + 2, (int) ((w - 4) * progress), PROGRESS_BAR_H - 4);
		if ((int) ((w - 4) * progress) > 4) {
			rect(PROGRESS_BAR_DEEP_INSIDE, x + 4, y + 4, (int) ((w - 4) * progress) - 4, PROGRESS_BAR_H - 8);
		}
	}
	
	public void resourceNeededBar(int x, int y, int w, double resource, double needed) {
		drawPanel(x, y, w, PROGRESS_BAR_H, -1);
		rect(PROGRESS_BAR_INSIDE, x + 2, y + 2, (int) ((w - 4) * resource), PROGRESS_BAR_H - 4);
		if (needed <= resource) {
			if ((int) ((w - 4) * resource) > 4) {
				rect(PROGRESS_BAR_DEEP_INSIDE, x + 4, y + 4, (int) ((w - 4) * resource) - 4, PROGRESS_BAR_H - 8);
			}
			if ((int) ((w - 4) * needed) > 4) {
				rect(PROGRESS_BAR_USAGE, x + 4, y + 4, (int) ((w - 4) * needed) - 4, PROGRESS_BAR_H - 8);
			}
		} else {
			if ((int) ((w - 4) * resource) > 4) {
				rect(PROGRESS_BAR_INSUFFICIENT, x + 4, y + 4, (int) ((w - 4) * resource) - 4, PROGRESS_BAR_H - 8);
			}
			
			if ((int) ((w - 4) * needed) > 4) {
				rect(PROGRESS_BAR_USAGE, x + (int) ((w - 4) * needed), y + 4, 2, PROGRESS_BAR_H - 8);
			}
		}
	}
	
	public void progressBar(int x, int y, int w, int h, double progress) {
		progress = Math.min(1, Math.max(0, progress));
		drawPanel(x, y, w, h, -1);
		rect(PROGRESS_BAR_INSIDE, x + 2, y + 2, (int) ((w - 4) * progress), h - 4);
		if ((int) ((w - 4) * progress) > 4) {
			rect(PROGRESS_BAR_DEEP_INSIDE, x + 4, y + 4, (int) ((w - 4) * progress) - 4, h - 8);
		}
	}
	
	public void progressBar(int x, int y, int w, int h, double progress, Clr inside) {
		progress = Math.min(1, Math.max(0, progress));
		drawPanel(x, y, w, h, -1);
		rect(PROGRESS_BAR_INSIDE, x + 2, y + 2, (int) ((w - 4) * progress), h - 4);
		if ((int) ((w - 4) * progress) > 4) {
			rect(inside, x + 4, y + 4, (int) ((w - 4) * progress) - 4, h - 8);
		}
	}
	
	public void drawWoodGrain(final int x, final int y, final int w, final int h) {
		if (WIN_BG.machineImgCache == null) {
			blit(WIN_BG, -500, -500);
			return;
		}
		Image wg = (Image) WIN_BG.machineImgCache;
		if (wg == null) { return; }
		int y2 = y;
		while (y2 < y + h) {
			int h2 = StrictMath.min(256, y + h - y2);
			int x2 = x;
			while (x2 < x + w) {
				int w2 = StrictMath.min(256, x + w - x2);
				wg.draw(x2, y2, x2 + w2, y2 + h2, 0, 0, w2, h2);
				//rect(Clr.RED, x2, y2, w2, h2);
				x2 += 256;
			}
			y2 += 256;
		}
	}
	
	public void drawShadowedPanel(int x, int y, int w, int h) {
		drawWoodGrain(x + 3, y + 3, w - 6, h - 6);
		x -= S_PANEL_L.srcWidth - PANEL_L.srcWidth;
		y -= S_PANEL_T.srcHeight - PANEL_T.srcHeight;
		w += S_PANEL_L.srcWidth - PANEL_L.srcWidth + S_PANEL_R.srcWidth - PANEL_R.srcWidth;
		h += S_PANEL_T.srcHeight - PANEL_T.srcHeight + S_PANEL_B.srcHeight - PANEL_B.srcHeight;
		int x2 = x + S_PANEL_TL.srcWidth;
		while (x2 < x + w - S_PANEL_TR.srcWidth) {
			blit(S_PANEL_T, x2, y);
			blit(S_PANEL_B, x2, y + h - S_PANEL_B.srcHeight);
			x2 += S_PANEL_T.srcWidth;
		}
		int y2 = y + S_PANEL_TL.srcHeight;
		while (y2 < y + h - S_PANEL_TR.srcHeight) {
			blit(S_PANEL_L, x, y2);
			blit(S_PANEL_R, x + w - S_PANEL_R.srcWidth, y2);
			y2 += S_PANEL_L.srcHeight;
		}
		blit(S_PANEL_TL, x, y);
		blit(S_PANEL_TR, x + w - S_PANEL_TR.srcWidth, y);
		blit(S_PANEL_BL, x, y + h - S_PANEL_BL.srcHeight);
		blit(S_PANEL_BR, x + w - S_PANEL_BR.srcWidth, y + h - S_PANEL_BR.srcHeight);
	}
		
	public void drawPanel(final int x, final int y, final int w, final int h, int patternIndex) {
		drawWoodGrain(x + 3, y + 3, w - 6, h - 6);
		if (patternIndex > -1) {
			Img patternImg = new Img("patterns", 1 + 201 * (patternIndex % 5), 1 + 201 * (patternIndex / 5), 200, 200, false);
			Graphics g = (Graphics) frame().nativeRenderer();
			Rectangle clip = g.getClip();
			if (clip == null) {
				g.setClip(x + PANEL_TL.srcWidth, y + PANEL_TL.srcHeight, w - PANEL_TL.srcWidth - PANEL_BR.srcWidth, h - PANEL_TL.srcHeight - PANEL_BR.srcHeight);
				blit(patternImg, x + PANEL_TL.srcWidth, y + PANEL_TL.srcHeight);
				g.clearClip();
			}
		}
		drawPanelBorder(x, y, w, h);
	}
	
	public void drawPanelBorder(final int x, final int y, final int w, final int h) {
		int x2 = x + PANEL_TL.srcWidth;
		while (x2 < x + w - PANEL_TR.srcWidth) {
			blit(PANEL_T, x2, y);
			blit(PANEL_B, x2, y + h - PANEL_B.srcHeight);
			x2 += PANEL_T.srcWidth;
		}
		int y2 = y + PANEL_TL.srcHeight;
		while (y2 < y + h - PANEL_TR.srcHeight) {
			blit(PANEL_L, x, y2);
			blit(PANEL_R, x + w - PANEL_R.srcWidth, y2);
			y2 += PANEL_L.srcHeight;
		}
		blit(PANEL_TL, x, y);
		blit(PANEL_TR, x + w - PANEL_TR.srcWidth, y);
		blit(PANEL_BL, x, y + h - PANEL_BL.srcHeight);
		blit(PANEL_BR, x + w - PANEL_BR.srcWidth, y + h - PANEL_BR.srcHeight);
	}
	
	public void drawTopPanel(final int x, final int y, final int w, final int h) {
		drawWoodGrain(x + 3, y, w - 6, h - 3);
		int x2 = x + PANEL_TL.srcWidth;
		while (x2 < x + w - PANEL_TR.srcWidth) {
			blit(PANEL_B, x2, y + h - PANEL_B.srcHeight);
			x2 += PANEL_T.srcWidth;
		}
		int y2 = y;
		while (y2 < y + h - PANEL_BR.srcHeight) {
			blit(PANEL_L, x, y2);
			blit(PANEL_R, x + w - PANEL_R.srcWidth, y2);
			y2 += PANEL_L.srcHeight;
		}
		blit(PANEL_BL, x, y + h - PANEL_BL.srcHeight);
		blit(PANEL_BR, x + w - PANEL_BR.srcWidth, y + h - PANEL_BR.srcHeight);
	}
	
	public void drawShadowedWindow(int x, int y, int w, int h, int patternIndex) {
		x -= 1 + S_WIN_L.srcWidth - WIN_L.srcWidth;
		y -= 1 + S_WIN_T.srcHeight - WIN_T.srcHeight;
		w += 2 + S_WIN_L.srcWidth - WIN_L.srcWidth + S_WIN_R.srcWidth - WIN_R.srcWidth;
		h += 2 + S_WIN_T.srcHeight - WIN_T.srcHeight + S_WIN_B.srcHeight - WIN_B.srcHeight;
		drawWoodGrain(x + S_WIN_L.srcWidth, y + S_WIN_T.srcHeight, w - S_WIN_L.srcWidth - S_WIN_R.srcWidth, h - S_WIN_T.srcHeight - S_WIN_B.srcHeight);
		
		if (patternIndex != -1) {
			Img patternImg = new Img("patterns", 1 + 201 * (patternIndex % 5), 1 + 201 * (patternIndex / 5), 200, 200, false);
			Graphics g = (Graphics) frame().nativeRenderer();
			Rectangle clip = g.getClip();
			if (clip == null) {
				g.setClip(x + WIN_TL.srcWidth, y + WIN_TL.srcHeight, w - WIN_TL.srcWidth - WIN_BR.srcWidth, h - WIN_TL.srcHeight - WIN_BR.srcHeight);
				blit(patternImg, x + WIN_TL.srcWidth, y + WIN_TL.srcHeight);
				g.clearClip();
			}
		}
		
		int rx = x + w - S_WIN_R.srcWidth;
		int y2 = y + S_WIN_TL.srcHeight;
		blit(S_WIN_L, x, y2);
		blit(S_WIN_R, rx, y2);
		y2 += S_WIN_L.srcHeight;
		while (y2 < y + h - S_WIN_BL.srcHeight) {
			if (S_WIN_L.machineImgCache != null) {
				((Image) S_WIN_L.machineImgCache).draw(
						x,
						y2,
						x + S_WIN_L.srcWidth,
						y2 + StrictMath.min(S_WIN_L.srcHeight, y + h - S_WIN_BL.srcHeight - y2),
						0,
						0,
						S_WIN_L.srcWidth,
						StrictMath.min(S_WIN_L.srcHeight, y + h - S_WIN_BL.srcHeight - y2));
			}
			if (S_WIN_R.machineImgCache != null) {
				((Image) S_WIN_R.machineImgCache).draw(
						rx,
						y2,
						rx + S_WIN_R.srcWidth,
						y2 + StrictMath.min(S_WIN_R.srcHeight, y + h - S_WIN_BR.srcHeight - y2),
						0,
						0,
						S_WIN_R.srcWidth,
						StrictMath.min(S_WIN_R.srcHeight, y + h - S_WIN_BR.srcHeight - y2));
			}
			y2 += S_WIN_L.srcHeight;
		}
		
		int x2 = x + S_WIN_TL.srcWidth;
		int by = y + h - S_WIN_B.srcHeight;
		blit(S_WIN_T, x2, y);
		blit(S_WIN_B, x2, by);
		x2 += S_WIN_T.srcWidth;
		
		while (x2 < x + w - S_WIN_TR.srcWidth) {
			if (S_WIN_T.machineImgCache != null) {
				((Image) S_WIN_T.machineImgCache).draw(
						x2,
						y,
						x2 + StrictMath.min(S_WIN_T.srcWidth, x + w - S_WIN_TR.srcWidth - x2),
						y + S_WIN_T.srcHeight,
						0,
						0,
						StrictMath.min(S_WIN_T.srcWidth, x + w - S_WIN_TR.srcWidth - x2),
						S_WIN_T.srcHeight);
			}
			if (S_WIN_B.machineImgCache != null) {
				((Image) S_WIN_B.machineImgCache).draw(
						x2,
						by,
						x2 + StrictMath.min(S_WIN_B.srcWidth, x + w - S_WIN_BR.srcWidth - x2),
						by + S_WIN_B.srcHeight,
						0,
						0,
						StrictMath.min(S_WIN_B.srcWidth, x + w - S_WIN_BR.srcWidth - x2),
						S_WIN_B.srcHeight);
			}
			x2 += S_WIN_T.srcWidth;
		}
		
		blit(S_WIN_TL, x, y);
		blit(S_WIN_TR, x + w - S_WIN_TR.srcWidth, y);
		blit(S_WIN_BL, x, y + h - S_WIN_BL.srcHeight);
		blit(S_WIN_BR, x + w - S_WIN_BR.srcWidth, y + h - S_WIN_BR.srcHeight);
	}
		
	public void drawWindow(int x, int y, int w, int h, int patternIndex) {
		x--;
		y--;
		w += 2;
		h += 2;
		drawWoodGrain(x + WIN_L.srcWidth, y + WIN_T.srcHeight, w - WIN_L.srcWidth - WIN_R.srcWidth, h - WIN_T.srcHeight - WIN_B.srcHeight);
		
		if (patternIndex != -1) {
			Img patternImg = new Img("patterns", 1 + 201 * (patternIndex % 5), 1 + 201 * (patternIndex / 5), 200, 200, false);
			Graphics g = (Graphics) frame().nativeRenderer();
			Rectangle clip = g.getClip();
			if (clip == null) {
				g.setClip(x + WIN_TL.srcWidth, y + WIN_TL.srcHeight, w - WIN_TL.srcWidth - WIN_BR.srcWidth, h - WIN_TL.srcHeight - WIN_BR.srcHeight);
				blit(patternImg, x + WIN_TL.srcWidth, y + WIN_TL.srcHeight);
				g.clearClip();
			}
		}
		
		int rx = x + w - WIN_R.srcWidth;
		int y2 = y + WIN_TL.srcHeight;
		blit(WIN_L, x, y2);
		blit(WIN_R, rx, y2);
		y2 += WIN_L.srcHeight;
		while (y2 < y + h - WIN_BL.srcHeight) {
			if (WIN_L.machineImgCache != null) {
				((Image) WIN_L.machineImgCache).draw(
						x,
						y2,
						x + WIN_L.srcWidth,
						y2 + StrictMath.min(WIN_L.srcHeight, y + h - WIN_BL.srcHeight - y2),
						0,
						0,
						WIN_L.srcWidth,
						StrictMath.min(WIN_L.srcHeight, y + h - WIN_BL.srcHeight - y2));
			}
			if (WIN_R.machineImgCache != null) {
				((Image) WIN_R.machineImgCache).draw(
						rx,
						y2,
						rx + WIN_R.srcWidth,
						y2 + StrictMath.min(WIN_R.srcHeight, y + h - WIN_BR.srcHeight - y2),
						0,
						0,
						WIN_R.srcWidth,
						StrictMath.min(WIN_R.srcHeight, y + h - WIN_BR.srcHeight - y2));
			}
			y2 += WIN_L.srcHeight;
		}
		
		int x2 = x + WIN_TL.srcWidth;
		int by = y + h - WIN_B.srcHeight;
		blit(WIN_T, x2, y);
		blit(WIN_B, x2, by);
		x2 += WIN_T.srcWidth;
		
		while (x2 < x + w - WIN_TR.srcWidth) {
			if (WIN_T.machineImgCache != null) {
				((Image) WIN_T.machineImgCache).draw(
						x2,
						y,
						x2 + StrictMath.min(WIN_T.srcWidth, x + w - WIN_TR.srcWidth - x2),
						y + WIN_T.srcHeight,
						0,
						0,
						StrictMath.min(WIN_T.srcWidth, x + w - WIN_TR.srcWidth - x2),
						WIN_T.srcHeight);
			}
			if (WIN_B.machineImgCache != null) {
				((Image) WIN_B.machineImgCache).draw(
						x2,
						by,
						x2 + StrictMath.min(WIN_B.srcWidth, x + w - WIN_BR.srcWidth - x2),
						by + WIN_B.srcHeight,
						0,
						0,
						StrictMath.min(WIN_B.srcWidth, x + w - WIN_BR.srcWidth - x2),
						WIN_B.srcHeight);
			}
			x2 += WIN_T.srcWidth;
		}
		
		blit(WIN_TL, x, y);
		blit(WIN_TR, x + w - WIN_TR.srcWidth, y);
		blit(WIN_BL, x, y + h - WIN_BL.srcHeight);
		blit(WIN_BR, x + w - WIN_BR.srcWidth, y + h - WIN_BR.srcHeight);
	}
	
	public void drawTopBar(ScreenMode sm) {
		if (WIN_BG.machineImgCache == null) {
			blit(WIN_BG, 0, 0);
			return;
		}
		Image wg = (Image) WIN_BG.machineImgCache;
		int x2 = 0;
		while (x2 < sm.width) {
			if (wg != null) {
				wg.draw(x2, 6, x2 + 256, TOP_BAR_H - 6, 0, 0, 256, TOP_BAR_H - 6);
			}
			x2 += 256;
		}
		x2 = 0;
		while (x2 < sm.width) {
			blit(TOP_BAR_TOP_SEGMENT, x2, 2);
			blit(TOP_BAR_BOTTOM_SEGMENT, x2, TOP_BAR_H - 7);
			x2 += TOP_BAR_BOTTOM_SEGMENT.srcWidth;
		}
	}
	
	public void drawRightSideWindow(ScreenMode sm, int x, int y) {
		drawWoodGrain(x + 3, y, sm.width - x, sm.height - y);
		int y2 = y;
		while (y2 < sm.height) {
			blit(RHS_WINDOW_SIDE_SEGMENT, x - 1, y2);
			y2 += RHS_WINDOW_SIDE_SEGMENT.srcHeight;
		}
	}
	
	public void drawLeftSideWindow(ScreenMode sm, int w, int y) {
		drawWoodGrain(0, y, w - 3, sm.height - y);
		int y2 = y;
		while (y2 < sm.height) {
			blit(LHS_WINDOW_SIDE_SEGMENT, w - LHS_WINDOW_SIDE_SEGMENT.srcWidth + 1, y2);
			y2 += LHS_WINDOW_SIDE_SEGMENT.srcHeight;
		}
	}
		
	public void drawSpecialCharge(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialCharge(name);
			if (img != null) {
				Appearance.unlockShader(true);
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeLeft(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeLeftHalf(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeRight(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeRightHalf(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeTop(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeTopHalf(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeBottom(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeBottomHalf(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeTopLeft(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeTopLeftQuarter(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeTopRight(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeTopRightQuarter(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeBottomLeft(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeBottomLeftQuarter(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}
	
	public void drawSpecialChargeBottomRight(String name, Clr c, double x, double y, double w, double h) {
		if (integration != null) {
			Img img = integration.getSpecialChargeBottomRightQuarter(name);
			if (img != null) {
				blit(img, c, x, y, w, h);
			}
		}
	}

	public void dottedLine(Clr c, int width, int step, double x1, double y1, double x2, double y2) {
		double l = StrictMath.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
		for (int i = 0; i < l; i += step) {
			rect(c, x1 + (x2 - x1) * i / l, y1 + (y2 - y1) * i / l, width, width);
		}
	}

	public static final Pattern REMOVE_FORMATTING = Pattern.compile("\\[[^\\]]+\\]");
	
	public void borderedText(String text, Fount f, Clr c, Clr border, int x, int y) {
		String shornText = "[" + border.toString() + "]" + REMOVE_FORMATTING.matcher(text).replaceAll("");
		for (int j = 0; j < BORDER_ADJ.length; j++) {
			//text("[" + border.toString() + "]" + text.replaceAll("\\[[^\\]]+\\]", ""), f, x + Airship.ADJ[j][0], y + Airship.ADJ[j][1]);
			text(shornText, f, x + BORDER_ADJ[j][0], y + BORDER_ADJ[j][1]);
		}
		text("[" + c.toString() + "]" + text, f, x, y);
	}
	
	public static final double[][] BORDER_ADJ = {
		{-.7, -.7}, { 0, -1}, { .7, -.7},
		{-1,  0},           { 1,  0},
		{-.7,  .7}, { 0,  1}, { .7,  .7}
	};
	
	public static final int[][] HEAVY_ADJ = {
		{-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
		{-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
		{-2,  0}, {-1,  0},          {1,  0}, {2,  0},
		{-2,  1}, {-1,  1}, {0,  1}, {1,  1}, {2,  1},
		{-2,  2}, {-1,  2}, {0,  2}, {1,  2}, {2,  2}
	};
	
	public void heavilyBorderedText(String text, Fount f, Fount bg, Clr c, Clr border, int x, int y, int maxW) {
		/*for (int j = 0; j < HEAVY_ADJ.length; j++) {
			text("[" + border.toString() + "]" + text.replaceAll("\\[[^\\]]+\\]", ""), f, x + HEAVY_ADJ[j][0], y + HEAVY_ADJ[j][1], maxW);
		}*/
		text("[" + border.toString() + "]" + text, bg, x, y, maxW);
		text("[" + c.toString() + "]" + text, f, x, y, maxW, 10000, bg.lineHeight - f.lineHeight - 2, true);
		//text("[bg=" + border.toString() + "][" + c.toString() + "]" + text, f, x, y, maxW);
	}
	
	public void borderedBlit(Img img, Clr c, Clr border, double x, double y) {
		for (int j = 0; j < Airship.ADJ.length; j++) {
			blit(img, border, 1, x + Airship.ADJ[j][0], y + Airship.ADJ[j][1], 0, 0, 0);
		}
		blit(img, c, 1, x, y, 0, 0, 0);
	}

	public static boolean in(int x, int y, int w, Pt pt) {
		return pt.x >= x && pt.y >= y && pt.x <= x + w && pt.y <= y + 20;
	}

	public static boolean in(int x, int y, int w, int h, Pt pt) {
		return pt.x >= x && pt.y >= y && pt.x <= x + w && pt.y <= y + h;
	}
	
	public int bw(String text) {
		return (int) textSize(text, AGame.BIG_FOUNT).x + MyDraw.BUTTON_EXTRA_W;
	}
	
	public int bw(String text, String hotkey) {
		if (Main.ON_STEAM_DECK) {
			hotkey = null;
		}
		if (hotkey == null) { return bw(text); }
		return (int) textSize(text + " " + hotkey, AGame.BIG_FOUNT).x + MyDraw.BUTTON_EXTRA_W;
	}
	
	public int tw(String text) {
		return (int) textSize(text, AGame.BIG_FOUNT).x + MyDraw.TOGGLE_EXTRA_W;
	}
	
	public int tw(String text, String hotkey) {
		if (Main.ON_STEAM_DECK) {
			hotkey = "";
		}
		return (int) textSize(text + " " + hotkey, AGame.BIG_FOUNT).x + MyDraw.TOGGLE_EXTRA_W;
	}
	
	public void button(int x, int y, int w, String text, final Runnable onClick) {
		button(x, y, w, text, null, onClick, null, true);
	}
	
	public void button(int x, int y, int w, String text, final Runnable onClick, boolean enabled) {
		button(x, y, w, text, null, onClick, null, enabled);
	}
	
	public void button(int x, int y, int w, String text, String hotkey, final Runnable onClick) {
		button(x, y, w, text, hotkey, onClick, null, true);
	}
	
	public void button(int x, int y, int w, String text, String hotkey, final Runnable onClick, boolean enabled) {
		button(x, y, w, text, hotkey, onClick, null, enabled);
	}

	public void button(int x, int y, int w, String text, String hotkey, final Runnable onClick, final InputRunnable onClick2, boolean enabled) {
		if (Main.ON_STEAM_DECK) {
			hotkey = null;
		}
		String oText = text;
		x--;
		y--;
		w += 2;
		if (enabled) {
			if (hotkey != null) {
				text = BUTTON_TEXT_COLOR + text + " " + BUTTON_HOTKEY_COLOR + hotkey;
			} else {
				text = BUTTON_TEXT_COLOR + text;
			}
			hook(x, y, w, BUTTON_H + 2, new Hook(oText, Hook.Type.MOUSE_1_CLICKED, Monkey.checkButton(oText) ? Hook.Type.TEST : Hook.Type.NO_TEST) {
				@Override
				public void run(Input in, Pt p, Type type) {
					if (state.canClick()) {
						state.hasClicked();
						in.play("click", 1, AirshipGame.instance.volume, 0, 0);
						if (onClick != null) { onClick.run(); }
						if (onClick2 != null) { onClick2.run(in); }
					}
				}
			});
			boolean hover = in(x, y, w, BUTTON_H, state.cursor) && (state.hookClipRect == null || in(state.hookClipRect.x, state.hookClipRect.y, state.hookClipRect.w, state.hookClipRect.h, state.cursor));
			blit(hover ? BUTTON_LIT_START : BUTTON_START, x, y);
			int x2 = x + BUTTON_START.srcWidth;
			Img middleImg = (hover ? BUTTON_LIT_MIDDLE : BUTTON_MIDDLE);
			if (middleImg.machineImgCache == null) {
				blit(middleImg, x2, y);
				x2 += middleImg.srcWidth;
			}
			while (x2 < x + w - middleImg.srcWidth) {
				if (middleImg.machineImgCache != null) {
					((Image) middleImg.machineImgCache).draw(
							x2,
							y,
							x2 + StrictMath.min(middleImg.srcWidth, x + w - BUTTON_END.srcWidth - x2),
							y + middleImg.srcHeight,
							0,
							0,
							StrictMath.min(middleImg.srcWidth, x + w - BUTTON_END.srcWidth - x2),
							middleImg.srcHeight);
				}
				x2 += BUTTON_MIDDLE.srcWidth;
			}
			blit(hover ? BUTTON_LIT_END : BUTTON_END, x + w - BUTTON_END.srcWidth, y);
			state.addGlowRect(x, y, w, BUTTON_H + 2);
		} else {
			text = BUTTON_TEXT_COLOR + text;
			// Make opaque to clicks even tho disabled.
			hook(x, y, w, BUTTON_H, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Type type) {
					state.hasClicked();
					in.play("unclick", 1, AirshipGame.instance.volume, 0, 0);
				}
			});
			
			blit(BUTTON_DIS_START, x, y);
			int x2 = x + BUTTON_START.srcWidth;
			blit(BUTTON_DIS_MIDDLE, x2, y);
			x2 += BUTTON_MIDDLE.srcWidth;
			while (x2 < x + w - BUTTON_END.srcWidth) {
				if (BUTTON_DIS_MIDDLE.machineImgCache != null) {
					((Image) BUTTON_DIS_MIDDLE.machineImgCache).draw(
							x2,
							y,
							x2 + StrictMath.min(BUTTON_DIS_MIDDLE.srcWidth, x + w - BUTTON_END.srcWidth - x2),
							y + BUTTON_DIS_MIDDLE.srcHeight,
							0,
							0,
							StrictMath.min(BUTTON_DIS_MIDDLE.srcWidth, x + w - BUTTON_END.srcWidth - x2),
							BUTTON_DIS_MIDDLE.srcHeight);
				}
				x2 += BUTTON_MIDDLE.srcWidth;
			}
			blit(BUTTON_DIS_END, x + w - BUTTON_END.srcWidth, y);
		}
		
		Pt dims = textSize(text, AGame.BIG_FOUNT); // qqDPS MEMALLOC OMG WTF BBQ
		if (dims.x > w - 8) {
			dims = textSize(text, AGame.FOUNT);
			text(text, AGame.FOUNT, x + w / 2 - (int) (dims.x / 2), y + BUTTON_TEXT_Y_OFFSET + AGame.BIG_FOUNT.height / 2 - AGame.FOUNT.height / 2);
		} else {
			text(text, AGame.BIG_FOUNT, x + w / 2 - (int) (dims.x / 2), y + BUTTON_TEXT_Y_OFFSET);
		}
	}
	
	public void repeatingButton(int x, int y, int w, int h, String text, String hotkey, final Runnable onClick, boolean enabled) {
		button(x, y, w, text, hotkey, onClick, enabled); // qqDPS
	}
	
	public void button(int x, int y, int w, String text, String hotkey, final InputRunnable onClick) {
		button(x, y, w, text, hotkey, null, onClick, true);
	}
	
	public void button(int x, int y, int w, String text, final InputRunnable onClick) {
		button(x, y, w, text, null, null, onClick, true);
	}
	
	public void button(int x, int y, int w, String text, String hotkey, final InputRunnable onClick, boolean enabled) {
		button(x, y, w, text, hotkey, null, onClick, enabled);
	}
	
	public void goldbutton(int x, int y, int w, String text, String hotkey, final InputRunnable onClick, boolean enabled) {
		button(x, y, w, text, hotkey, null, onClick, enabled);
		if (enabled) {
			int start = x - w / 4 + (state.time / 10) % (w * 3 / 2);
			int end = x + (state.time / 10) % (w * 3 / 2);
			if (start < x + w) {
				rect(GOLD_BUTTON_GLOW, StrictMath.max(x, start), y + 1, StrictMath.min(end - StrictMath.max(x, start), x + w - start), BUTTON_H - 2);
			}
			start = x - w * 3 / 16 + (state.time / 10) % (w * 3 / 2);
			end = x - w / 16 + (state.time / 10) % (w * 3 / 2);
			if (start < x + w) {
				rect(GOLD_BUTTON_GLOW, StrictMath.max(x, start), y + 3, StrictMath.min(end - StrictMath.max(x, start), x + w - start), BUTTON_H - 6);
			}
		}
	}
	
	public void bigButton(int x, int y, int w, String text, String hotkey, final Runnable onClick) {
		bigButton(x, y, w, text, hotkey, onClick, null);
	}
	
	public void bigButton(int x, int y, int w, String text, String hotkey, final InputRunnable onClick) {
		bigButton(x, y, w, text, hotkey, null, onClick);
	}
	
	private void bigButton(int x, int y, int w, String text, String hotkey, final Runnable onClick, final InputRunnable iOnClick) {
		if (Main.ON_STEAM_DECK) {
			hotkey = null;
		}
		String oText = text;
		x--;
		y--;
		w += 2;
		
		boolean hover = in(x, y, w, BIG_BUTTON_H, state.cursor) && (state.hookClipRect == null || in(state.hookClipRect.x, state.hookClipRect.y, state.hookClipRect.w, state.hookClipRect.h, state.cursor));
		
		blit(hover ? HBB_TL : BB_TL, x, y);
		Img l = hover ? HBB_L : BB_L;
		Img r = hover ? HBB_R : BB_R;
		int rx = x + w - BB_R.srcWidth;
		int y2 = y + BB_TL.srcHeight;
		blit(l, x, y2);
		blit(r, rx, y2);
		y2 += l.srcHeight;
		while (y2 < y + BIG_BUTTON_H - BB_BL.srcHeight) {
			if (l.machineImgCache != null) {
				((Image) l.machineImgCache).draw(
						x,
						y2,
						x + l.srcWidth,
						y2 + StrictMath.min(l.srcHeight, y + BIG_BUTTON_H - BB_BL.srcHeight - y2),
						0,
						0,
						l.srcWidth,
						StrictMath.min(l.srcHeight, y + BIG_BUTTON_H - BB_BL.srcHeight - y2));
			}
			if (r.machineImgCache != null) {
				((Image) r.machineImgCache).draw(
						rx,
						y2,
						rx + r.srcWidth,
						y2 + StrictMath.min(r.srcHeight, y + BIG_BUTTON_H - BB_BR.srcHeight - y2),
						0,
						0,
						r.srcWidth,
						StrictMath.min(r.srcHeight, y + BIG_BUTTON_H - BB_BR.srcHeight - y2));
			}
			y2 += BB_L.srcHeight;
		}
		blit(hover ? HBB_BL : BB_BL, x, y + BIG_BUTTON_H - BB_BL.srcHeight);
		
		Img t = hover ? HBB_T : BB_T;
		Img b = hover ? HBB_B : BB_B;
		int x2 = x + BB_TL.srcWidth;
		int by = y + BIG_BUTTON_H - BB_B.srcHeight;
		blit(t, x2, y);
		blit(b, x2, by);
		x2 += BB_T.srcWidth;
		
		while (x2 < x + w - BB_TR.srcWidth) {
			if (t.machineImgCache != null) {
				((Image) t.machineImgCache).draw(
						x2,
						y,
						x2 + StrictMath.min(t.srcWidth, x + w - BB_BR.srcWidth - x2),
						y + t.srcHeight,
						0,
						0,
						StrictMath.min(t.srcWidth, x + w - BB_BR.srcWidth - x2),
						t.srcHeight);
			}
			if (b.machineImgCache != null) {
				((Image) b.machineImgCache).draw(
						x2,
						by,
						x2 + StrictMath.min(b.srcWidth, x + w - BB_BR.srcWidth - x2),
						by + b.srcHeight,
						0,
						0,
						StrictMath.min(b.srcWidth, x + w - BB_BR.srcWidth - x2),
						b.srcHeight);
			}
			x2 += BB_T.srcWidth;
		}
		
		blit(hover ? HBB_TR : BB_TR, x + w - BB_TR.srcWidth, y);
		blit(hover ? HBB_BR : BB_BR, x + w - BB_BR.srcWidth, y + BIG_BUTTON_H - BB_BR.srcHeight);
		rect(hover ? HBB_CENTER : BB_CENTER, x + BB_TL.srcWidth, y + BB_TL.srcHeight, w - BB_TL.srcWidth - BB_TR.srcWidth, BIG_BUTTON_H - BB_TL.srcHeight - BB_BL.srcHeight);
		if (hotkey != null) {
			text = BUTTON_TEXT_COLOR + text + " " + BUTTON_HOTKEY_COLOR + hotkey;
		} else {
			text = BUTTON_TEXT_COLOR + text;
		}
		
		hook(x, y, w, BIG_BUTTON_H, new Hook(oText, Hook.Type.MOUSE_1_CLICKED, Monkey.checkButton(oText) ? Hook.Type.TEST : Hook.Type.NO_TEST) {
			@Override
			public void run(Input in, Pt p, Type type) {
				in.play("click", 0.7, AirshipGame.instance.volume, 0, 0);
				if (onClick != null) {
					onClick.run();
				} else {
					iOnClick.run(in);
				}
			}
		});
		
		Pt dims = textSize(text, AGame.BIGGER_FOUNT); // qqDPS MEMALLOC OMG WTF BBQ
		if (dims.x > w - 10) {
			dims = textSize(text, AGame.BIG_FOUNT);
			text(text, AGame.BIG_FOUNT, x + w / 2 - (int) (dims.x / 2), y + BIG_BUTTON_TEXT_Y_OFFSET + AGame.BIGGER_FOUNT.height / 2 - AGame.BIG_FOUNT.height / 2);
		} else {
			text(text, AGame.BIGGER_FOUNT, x + w / 2 - (int) (dims.x / 2), y + BIG_BUTTON_TEXT_Y_OFFSET);
		}
		state.addGlowRect(x, y, w, BIG_BUTTON_H);
	}
	
	public Clr pulse(Clr c) {
		double amt = (1.0 + StrictMath.sin(state.time * 0.003)) / 4;
		return c.mix(amt, Clr.WHITE);
	}
	
	public Clr darkPulse(Clr c) {
		double amt = (1.0 + StrictMath.sin(state.time * 0.003)) / 4;
		return c.mix(amt, Clr.BLACK);
	}
	
	public void toggle(int x, int y, int w, String text, String hotkey, final InputRunnable onClick, boolean selected, boolean enabled) {
		if (Main.ON_STEAM_DECK) {
			hotkey = null;
		}
		String oText = text;
		x--;
		y--;
		w += 2;
		if (enabled) {
			if (hotkey != null) {
				text = BUTTON_TEXT_COLOR + text + " " + BUTTON_HOTKEY_COLOR + hotkey;
			} else {
				text = BUTTON_TEXT_COLOR + text;
			}
			hook(x, y, w, BUTTON_H, new Hook(oText, Hook.Type.MOUSE_1_CLICKED, Monkey.checkButton(oText) ? Hook.Type.TEST : Hook.Type.NO_TEST) {
				@Override
				public void run(Input in, Pt p, Type type) {
					if (state.canClick()) {
						state.hasClicked();
						in.play("click", 1, AirshipGame.instance.volume, 0, 0);
						if (onClick != null) { onClick.run(in); }
					}
				}
			});
			boolean hover = in(x, y, w, BUTTON_H, state.cursor) && (state.hookClipRect == null || in(state.hookClipRect.x, state.hookClipRect.y, state.hookClipRect.w, state.hookClipRect.h, state.cursor));
			Img startImg =
					selected
					? hover
						? TOGGLE_ON_LIT_START
						: TOGGLE_ON_START
					: hover
						? TOGGLE_OFF_LIT_START
						: TOGGLE_OFF_START;
			Img middleImg =
				selected
					? hover
						? TOGGLE_ON_LIT_MIDDLE
						: TOGGLE_ON_MIDDLE
					: hover
						? TOGGLE_OFF_LIT_MIDDLE
						: TOGGLE_OFF_MIDDLE;
			Img endImg =
				selected
					? hover
						? TOGGLE_ON_LIT_END
						: TOGGLE_ON_END
					: hover
						? TOGGLE_OFF_LIT_END
						: TOGGLE_OFF_END;
			blit(startImg, x, y);
			int x2 = x + startImg.srcWidth;
			blit(middleImg, x2, y);
			x2 += middleImg.srcWidth;
			while (x2 < x + w - middleImg.srcWidth) {
				if (middleImg.machineImgCache != null) {
					((Image) middleImg.machineImgCache).draw(
							x2,
							y,
							x2 + StrictMath.min(middleImg.srcWidth, x + w - endImg.srcWidth - x2),
							y + middleImg.srcHeight,
							0,
							0,
							StrictMath.min(middleImg.srcWidth, x + w - endImg.srcWidth - x2),
							middleImg.srcHeight);
				}
				x2 += middleImg.srcWidth;
			}
			blit(endImg, x + w - endImg.srcWidth, y);
			state.addGlowRect(x, y, w, BUTTON_H + BUTTON_TEXT_Y_OFFSET);
		} else {
			text = BUTTON_TEXT_COLOR + text;
			// Make opaque to clicks even tho disabled.
			hook(x, y, w, BUTTON_H, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Type type) {
					in.play("unclick", 1, AirshipGame.instance.volume, 0, 0);
					state.hasClicked();
				}
			});
			
			blit(BUTTON_DIS_START, x, y);
			int x2 = x + BUTTON_START.srcWidth;
			blit(BUTTON_DIS_MIDDLE, x2, y);
			x2 += BUTTON_MIDDLE.srcWidth;
			while (x2 < x + w - BUTTON_END.srcWidth) {
				if (BUTTON_DIS_MIDDLE.machineImgCache != null) {
					((Image) BUTTON_DIS_MIDDLE.machineImgCache).draw(
							x2,
							y,
							x2 + StrictMath.min(BUTTON_DIS_MIDDLE.srcWidth, x + w - BUTTON_END.srcWidth - x2),
							y + BUTTON_DIS_MIDDLE.srcHeight,
							0,
							0,
							StrictMath.min(BUTTON_DIS_MIDDLE.srcWidth, x + w - BUTTON_END.srcWidth - x2),
							BUTTON_DIS_MIDDLE.srcHeight);
				}
				x2 += BUTTON_MIDDLE.srcWidth;
			}
			blit(BUTTON_DIS_END, x + w - BUTTON_END.srcWidth, y);
		}
		
		Pt dims = textSize(text, AGame.BIG_FOUNT); // qqDPS MEMALLOC OMG WTF BBQ
		if (dims.x > w - 20) {
			dims = textSize(text, AGame.FOUNT);
			text(text, AGame.FOUNT, x + w / 2 - (int) (dims.x / 2), y + BUTTON_TEXT_Y_OFFSET + AGame.BIG_FOUNT.height / 2 - AGame.FOUNT.height / 2);
		} else {
			text(text, AGame.BIG_FOUNT, x + w / 2 - (int) (dims.x / 2), y + BUTTON_TEXT_Y_OFFSET);
		}
	}

	public void confirmDialog(int x, int y, int w, String text, final Runnable onOK, final Runnable onCancel) {
		confirmDialog(x, y, w, text, _t("OK"), _t("Cancel"), onOK, onCancel);
	}

	public void confirmDialog(int x, int y, int w, String text, String ok, String cancel, final Runnable onOK, final Runnable onCancel) {
		int h = (int) textSize(text, AGame.FOUNT, 0, 0, w - WINDOW_INSET * 2).height + UI_SPACING + BUTTON_H + WINDOW_INSET * 2;
		drawShadowedWindow(x, y, w, h, 16);
		text(text, AGame.FOUNT, x + WINDOW_INSET, y + WINDOW_INSET, w - WINDOW_INSET * 2);
		button(x + WINDOW_INSET, y + h - WINDOW_INSET - BUTTON_H, bw(ok), ok, onOK);
		button(x + WINDOW_INSET + bw(ok) + BUTTON_SPACING, y + h - WINDOW_INSET - BUTTON_H, bw(cancel), cancel, onCancel);
	}
	
	public void yesNoCancelDialog(int x, int y, int w, String text, String ok, String no, String cancel, final Runnable onOK, final Runnable onNo, final Runnable onCancel) {
		int h = (int) textSize(text, AGame.FOUNT, 0, 0, w - WINDOW_INSET * 2).height + UI_SPACING + BUTTON_H + WINDOW_INSET * 2;
		drawShadowedWindow(x, y, w, h, 16);
		text(text, AGame.FOUNT, x + WINDOW_INSET, y + WINDOW_INSET, w - WINDOW_INSET * 2);
		button(x + WINDOW_INSET, y + h - WINDOW_INSET - BUTTON_H, bw(ok), ok, onOK);
		button(x + WINDOW_INSET + bw(ok) + BUTTON_SPACING, y + h - WINDOW_INSET - BUTTON_H, bw(no), no, onNo);
		button(x + WINDOW_INSET + bw(ok) + bw(no) + BUTTON_SPACING * 2, y + h - WINDOW_INSET - BUTTON_H, bw(cancel), cancel, onCancel);
	}
	
	public void messageDialog(int x, int y, int w, String text, final Runnable onOK) {
		int h = (int) textSize(text, AGame.FOUNT, 0, 0, w - WINDOW_INSET * 2).height + UI_SPACING + BUTTON_H + WINDOW_INSET * 2;
		drawShadowedWindow(x, y, w, h, 17);
		if (text.contains("{0}")) {
			AirshipGame.report(text);
		}
		text(text, AGame.FOUNT, x + WINDOW_INSET, y + WINDOW_INSET, w - WINDOW_INSET * 2);
		button(x + WINDOW_INSET, y + h - WINDOW_INSET - BUTTON_H, bw(_t("OK")), _t("OK"), onOK);
	}

	public void repeatingIconButton(int x, int y, Img icon, final Runnable onClick, boolean enabled) {
		iconButton(x, y, icon, onClick, null, enabled, true, true);
	}
	
	public void iconButton(int x, int y, Img icon, final Runnable onClick, boolean enabled) {
		iconButton(x, y, icon, onClick, null, enabled, false, true);
	}
	
	public void iconButton(int x, int y, Img icon, final Runnable onClick, boolean enabled, boolean gold) {
		iconButton(x, y, icon, onClick, null, enabled, false, true);
		if (gold) {
			int w = BUTTON_H;
			int start = x - w / 4 + (state.time / 10) % (w * 3 / 2);
			int end = x + (state.time / 10) % (w * 3 / 2);
			if (start < x + w) {
				rect(GOLD_BUTTON_GLOW, StrictMath.max(x, start), y + 1, StrictMath.min(end - StrictMath.max(x, start), x + w - start), BUTTON_H - 2);
			}
			start = x - w * 3 / 16 + (state.time / 10) % (w * 3 / 2);
			end = x - w / 16 + (state.time / 10) % (w * 3 / 2);
			if (start < x + w) {
				rect(GOLD_BUTTON_GLOW, StrictMath.max(x, start), y + 3, StrictMath.min(end - StrictMath.max(x, start), x + w - start), BUTTON_H - 6);
			}
		}
	}
	
	public void colouredIconButton(int x, int y, Img icon, final Runnable onClick, boolean enabled) {
		iconButton(x, y, icon, onClick, null, enabled, false, false);
	}
	
	public void iconButton(int x, int y, Img icon, final Runnable onClick, final InputRunnable onClick2, boolean enabled, boolean repeating, boolean tinted) {
		if (enabled) {
			boolean hover = in(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, state.cursor) && (state.hookClipRect == null || in(state.hookClipRect.x, state.hookClipRect.y, state.hookClipRect.w, state.hookClipRect.h, state.cursor));
			if (hover) {
				blit(ICON_BUTTON_LIT, x - 1, y - 1);
				blit(icon, tinted ? LIT_ICON_TINT : null, x + (ICON_BUTTON_LIT.srcWidth - 16) / 2 - 1, y + (ICON_BUTTON_LIT.srcHeight - 16) / 2 - 1);
			} else {
				blit(ICON_BUTTON, x - 1, y - 1);
				blit(icon, tinted ? ICON_TINT : null, x + (ICON_BUTTON.srcWidth - 16) / 2 - 1, y + (ICON_BUTTON.srcHeight - 16) / 2 - 1);
			}
			String monkeyKey = Monkey.checkIconButton(icon);
			if (repeating) {
				hook(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, new Hook(Type.MOUSE_1_DOWN) {
					@Override
					public void run(Input in, Pt p, Type type) {
						if (onClick != null) { onClick.run(); }
						if (onClick2 != null) { onClick2.run(in); }
					}
				});
			} else {
				hook(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, new Hook(monkeyKey, Type.MOUSE_1_CLICKED, monkeyKey == null ? Type.NO_TEST : Type.TEST) {
					@Override
					public void run(Input in, Pt p, Type type) {
						if (state.canClick()) {
							state.hasClicked();
							in.play("click", 1, AirshipGame.instance.volume, 0, 0);
							if (onClick != null) { onClick.run(); }
							if (onClick2 != null) { onClick2.run(in); }
						}
					}
				});
			}
			state.addGlowRect(x, y, BUTTON_H, BUTTON_H);
		} else {
			blit(ICON_BUTTON_DISABLED, x - 1, y - 1);
			blit(icon, tinted ? ICON_TINT : null, x + (ICON_BUTTON_DISABLED.srcWidth - 16) / 2 - 1, y + (ICON_BUTTON_DISABLED.srcHeight - 16) / 2 - 1);
			// Click opaqueness.
			hook(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, new Hook(Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Type type) {
					in.play("unclick", 1, AirshipGame.instance.volume, 0, 0);
				}
			});
		}
	}

	public void iconToggle(int x, int y, Img icon, final Runnable onClick, boolean selected, boolean enabled) {
		iconToggle(x, y, icon, onClick, null, selected, enabled, false);
	}
	
	public void iconToggle(int x, int y, Img icon, final Runnable onClick, final InputRunnable onClick2, boolean selected, boolean enabled, boolean gold) {
		if (enabled || selected) {
			String monkeyKey = Monkey.checkIconButton(icon);
			boolean hover = in(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, state.cursor) && (state.hookClipRect == null || in(state.hookClipRect.x, state.hookClipRect.y, state.hookClipRect.w, state.hookClipRect.h, state.cursor));
			if (hover) {
				blit(ICON_BUTTON_LIT, x - 1, y - 1);
				blit(icon, LIT_ICON_TINT, x + (ICON_BUTTON_LIT.srcWidth - 16) / 2 - 1, y + (ICON_BUTTON_LIT.srcHeight - 16) / 2 - 1);
			} else {
				blit(selected ? ICON_TOGGLE_ON : ICON_TOGGLE_OFF, x - 1, y - 1);
				blit(icon, ICON_TINT, x + (ICON_TOGGLE_ON.srcWidth - 16) / 2 - 1, y + (ICON_TOGGLE_ON.srcHeight - 16) / 2 - 1);
			}
			hook(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, new Hook(monkeyKey, Type.MOUSE_1_CLICKED, monkeyKey == null ? Type.NO_TEST : Type.TEST) {
				@Override
				public void run(Input in, Pt p, Type type) {
					if (state.canClick()) {
						state.hasClicked();
						in.play("click", 1, AirshipGame.instance.volume, 0, 0);
						if (onClick != null) { onClick.run(); }
						if (onClick2 != null) { onClick2.run(in); }
					}
				}
			});
			state.addGlowRect(x, y, BUTTON_H, BUTTON_H);
			if (gold) {
				int w = BUTTON_H;
				int start = x - w / 4 + (state.time / 10) % (w * 3 / 2);
				int end = x + (state.time / 10) % (w * 3 / 2);
				if (start < x + w) {
					rect(GOLD_BUTTON_GLOW, StrictMath.max(x, start), y + 1, StrictMath.min(end - StrictMath.max(x, start), x + w - start), BUTTON_H - 2);
				}
				start = x - w * 3 / 16 + (state.time / 10) % (w * 3 / 2);
				end = x - w / 16 + (state.time / 10) % (w * 3 / 2);
				if (start < x + w) {
					rect(GOLD_BUTTON_GLOW, StrictMath.max(x, start), y + 3, StrictMath.min(end - StrictMath.max(x, start), x + w - start), BUTTON_H - 6);
				}
			}
		} else {
			blit(ICON_BUTTON_DISABLED, x - 1, y - 1);
			blit(icon, ICON_TINT, x + (ICON_BUTTON_DISABLED.srcWidth - 16) / 2 - 1, y + (ICON_BUTTON_DISABLED.srcHeight - 16) / 2 - 1);
			// Click opaqueness.
			hook(x, y, ICON_BUTTON_SZ, ICON_BUTTON_SZ, new Hook(Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Type type) {
					in.play("unclick", 1, AirshipGame.instance.volume, 0, 0);
				}
			});
		}
	}
	
	public void iconButton(int x, int y, Img icon, final InputRunnable onClick, boolean enabled) {
		iconButton(x, y, icon, null, onClick, enabled, false, true);
	}
	
	public void iconToggle(int x, int y, Img icon, final InputRunnable onClick, boolean selected, boolean enabled) {
		iconToggle(x, y, icon, null, onClick, selected, enabled, false);
	}

	public void tooltip(double x, double y, double w, double h, final String text) {
		if (text == null) { return; }
		hook(x, y, w, h, new Hook(Hook.Type.HOVER) {
			@Override
			public void run(Input in, Pt p, Type type) {
				state.newtooltipText = text;
				state.maxTooltipX = -1;
			}
		});
	}
	
	public void tooltip(double x, double y, double w, double h, final String text, final Runnable onHover) {
		if (text == null) { return; }
		hook(x, y, w, h, new Hook(Hook.Type.HOVER) {
			@Override
			public void run(Input in, Pt p, Type type) {
				state.newtooltipText = text;
				state.maxTooltipX = -1;
				onHover.run();
			}
		});
	}
	
	public static interface Tooltip {
		public String get();
	}
	
	public void tooltip(double x, double y, double w, double h, final Tooltip tip) {
		hook(x, y, w, h, new Hook(Hook.Type.HOVER) {
			@Override
			public void run(Input in, Pt p, Type type) {
				state.newtooltipText = tip.get();
				state.maxTooltipX = -1;
			}
		});
	}
	
	public void tooltip(double x, double y, double w, double h, final int maxTTX, final String text) {
		hook(x, y, w, h, new Hook(Hook.Type.HOVER) {
			@Override
			public void run(Input in, Pt p, Type type) {
				state.newtooltipText = text;
				state.maxTooltipX = maxTTX;
			}
		});
	}
	
	public static int sliderHeight() { return SLIDER_KNOB.srcHeight; }
	
	public void slider(final int x, int y, int w, final SliderModel m, Pt cursor) {
		final int range = m.max() - m.min();
		final int rangeW = w - SLIDER_KNOB.srcWidth;
		drawPanelBorder(x + SLIDER_KNOB.srcWidth / 2, y + 1, rangeW, sliderHeight() - 2);
		if (cursor != null && cursor.x >= x && cursor.y >= y - 4 && cursor.x <= x + w && cursor.y <= y + sliderHeight() + 4) {
			rect(SELECTED, x + SLIDER_KNOB.srcWidth / 2 + 2, y + 2, rangeW - 4, sliderHeight() - 4);
		}
		blit(SLIDER_KNOB, x + rangeW * (m.getValue() - m.min()) / range, y);
		hook(x, y - 4, w, sliderHeight() + 8, new Hook(Hook.Type.MOUSE_1_CLICKED) {
			@Override
			public void run(Input input, Pt pt, Type type) {
				m.setValue(Math.min(m.max(), Math.max(m.min(), m.min() + (int) Math.round((pt.x - SLIDER_KNOB.srcWidth / 2) * 1.0 * range / rangeW))));
			}
		});
	}
	
	public static interface SliderModel {
		public int min();
		public int max();
		public int getValue();
		public void setValue(int value);
	}
	
	@Override
	public Draw hook(double x, double y, double width, double height, Hook h) {
		if (state.hookClipRect != null) {
			width = Math.min(x + width, state.hookClipRect.x + state.hookClipRect.w) - Math.max(state.hookClipRect.x, x);
			height = Math.min(y + height, state.hookClipRect.y + state.hookClipRect.h) - Math.max(state.hookClipRect.y, y);
			if (width <= 0 || height <= 0) {
				return this;
			}
			x = Math.max(state.hookClipRect.x, x);
			y = Math.max(state.hookClipRect.y, y);
		}
		return super.hook(x, y, width, height, h);
	}
	
	public void restrictHooks(int x, int y, int w, int h) {
		state.hookClipRect = new IntRect();
		state.hookClipRect.x = x;
		state.hookClipRect.y = y;
		state.hookClipRect.w = w;
		state.hookClipRect.h = h;
	}
	
	public void clearHookRestriction() {
		state.hookClipRect = null;
	}
	
	private static Img scaled(Img img, int scale) {
		if (img.src.contains(".")) {
			return new Img("scaled/" + img.src.split("[.]")[0] + "-" + scale + "." + img.src.split("[.]")[1]);
		} else {
			return new Img("scaled/" + img.src + "-" + scale);
		}
	}
	
	// Prettier scaled drawing, the cheap way
	public void portraitBlit(Img img, int x, int y, int w, int h) {
		if (w <= 50) {
			blit(scaled(img, 50), x, y, w, h);
		} else if (w <= 100) {
			blit(scaled(img, 100), x, y, w, h);
		} else if (w <= 200) {
			blit(scaled(img, 200), x, y, w, h);
		} else {
			blit(img, x, y, w, h);
		}
	}
	
	// Prettier scaled drawing
	/*private static ShaderProgram scaledSP;
	public void portraitBlit(Img img, int x, int y, int w, int h) {
		if (Appearance.shaderLoadFailed || Appearance.useSimpleGraphics || img.machineImgCache == null) {
			blit(img, x, y, w, h);
			return;
		}
		if (scaledSP == null) {
			try {
				scaledSP = ShaderProgram.loadProgram(
						AGame.getStaticGameDirectoryPath("data/prettyscale.vert"),
						AGame.getStaticGameDirectoryPath("data/prettyscale.frag"));
			} catch (Exception e) {
				e.printStackTrace();
				Appearance.shaderLoadFailed = true;
				return;
			}
		}
		scaledSP.bind();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GLCompat.GL_TEXTURE_2D, ((Image) img.machineImgCache).getTexture().getTextureID());
		scaledSP.setUniform1i("tex", 0);
		glBegin(GL_QUADS);
		glColor3f(1.0f, 1.0f, 1.0f);
		System.out.println(img.machineWCache);
		glTexCoord2d(0, 0);
		glVertex2d(x - ROUNDING_FIX, y - ROUNDING_FIX);
		glTexCoord2d(0, 300 / 512.0);
		glVertex2d(x - ROUNDING_FIX, y + h + ROUNDING_FIX);
		glTexCoord2d(300 / 512.0, 300 / 512.0);
		glVertex2d(x + w + ROUNDING_FIX, y + h + ROUNDING_FIX);
		glTexCoord2d(300 / 512.0, 0);
		glVertex2d(x + w + ROUNDING_FIX, y - ROUNDING_FIX);
		glEnd();
		scaledSP.unbind();
		TextureImpl.bindNone();
	}*/
}
