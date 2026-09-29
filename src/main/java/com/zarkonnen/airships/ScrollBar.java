package com.zarkonnen.airships;

import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import java.util.ArrayList;
import java.util.List;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

public class ScrollBar {
	public int offset;
	public int prevRange = 0;
	public boolean stickToBottom = false;
	
	public int clipX, clipY, clipW, clipH;
	
	private int cachedAvailableWidth = -1;
	
	public static int SCROLL_BAR_W = 24;
	public static int SMALL_SCROLL_BAR_W = 24;
	public static int MEDIUM_SCROLL_BAR_W = 24;
	public static int LARGE_SCROLL_BAR_W = 24;

	public static Img UP_BUTTON = new Img("ui", 116, 827, 26, 25, false);
	public static Img SMALL_UP_BUTTON = new Img("ui", 116, 827, 26, 25, false);
	public static Img MEDIUM_UP_BUTTON = new Img("ui", 116, 827, 26, 25, false);
	public static Img LARGE_UP_BUTTON = new Img("ui", 116, 827, 26, 25, false);

	public static Img DOWN_BUTTON = new Img("ui", 116, 853, 26, 25, false);
	public static Img SMALL_DOWN_BUTTON = new Img("ui", 116, 853, 26, 25, false);
	public static Img MEDIUM_DOWN_BUTTON = new Img("ui", 116, 853, 26, 25, false);
	public static Img LARGE_DOWN_BUTTON = new Img("ui", 116, 853, 26, 25, false);

	public static Img UP_BUTTON_LIT = new Img("ui", 89, 827, 26, 25, false);
	public static Img SMALL_UP_BUTTON_LIT = new Img("ui", 89, 827, 26, 25, false);
	public static Img MEDIUM_UP_BUTTON_LIT = new Img("ui", 89, 827, 26, 25, false);
	public static Img LARGE_UP_BUTTON_LIT = new Img("ui", 89, 827, 26, 25, false);

	public static Img DOWN_BUTTON_LIT = new Img("ui", 89, 853, 26, 25, false);
	public static Img SMALL_DOWN_BUTTON_LIT = new Img("ui", 89, 853, 26, 25, false);
	public static Img MEDIUM_DOWN_BUTTON_LIT = new Img("ui", 89, 853, 26, 25, false);
	public static Img LARGE_DOWN_BUTTON_LIT = new Img("ui", 89, 853, 26, 25, false);
	
	public static Img UP_BUTTON_DISABLED = new Img("ui", 251, 827, 26, 25, false);
	public static Img SMALL_UP_BUTTON_DISABLED = new Img("ui", 251, 827, 26, 25, false);
	public static Img MEDIUM_UP_BUTTON_DISABLED = new Img("ui", 251, 827, 26, 25, false);
	public static Img LARGE_UP_BUTTON_DISABLED = new Img("ui", 251, 827, 26, 25, false);

	public static Img DOWN_BUTTON_DISABLED = new Img("ui", 251, 853, 26, 25, false);
	public static Img SMALL_DOWN_BUTTON_DISABLED = new Img("ui", 251, 853, 26, 25, false);
	public static Img MEDIUM_DOWN_BUTTON_DISABLED = new Img("ui", 251, 853, 26, 25, false);
	public static Img LARGE_DOWN_BUTTON_DISABLED = new Img("ui", 251, 853, 26, 25, false);

	public static Img SCROLL_THUMB_TOP = new Img("ui", 143, 827, 20, 6, false);
	public static Img SMALL_SCROLL_THUMB_TOP = new Img("ui", 143, 827, 20, 6, false);
	public static Img MEDIUM_SCROLL_THUMB_TOP = new Img("ui", 143, 827, 20, 6, false);
	public static Img LARGE_SCROLL_THUMB_TOP = new Img("ui", 143, 827, 20, 6, false);
	
	public static Img SCROLL_THUMB_MIDDLE = new Img("ui", 143, 833, 20, 8, false);
	public static Img SMALL_SCROLL_THUMB_MIDDLE = new Img("ui", 143, 833, 20, 8, false);
	public static Img MEDIUM_SCROLL_THUMB_MIDDLE = new Img("ui", 143, 833, 20, 8, false);
	public static Img LARGE_SCROLL_THUMB_MIDDLE = new Img("ui", 143, 833, 20, 8, false);
	
	public static Img SCROLL_THUMB_BOTTOM = new Img("ui", 143, 841, 20, 6, false);
	public static Img SMALL_SCROLL_THUMB_BOTTOM = new Img("ui", 143, 841, 20, 6, false);
	public static Img MEDIUM_SCROLL_THUMB_BOTTOM = new Img("ui", 143, 841, 20, 6, false);
	public static Img LARGE_SCROLL_THUMB_BOTTOM = new Img("ui", 143, 841, 20, 6, false);

	public static Img LEFT_BORDER = new Img("ui", 80, 846, 2, 20, false);
	public static Img SMALL_LEFT_BORDER = new Img("ui", 80, 846, 2, 20, false);
	public static Img MEDIUM_LEFT_BORDER = new Img("ui", 80, 846, 2, 20, false);
	public static Img LARGE_LEFT_BORDER = new Img("ui", 80, 846, 2, 20, false);

	public static Img RIGHT_BORDER = new Img("ui", 83, 846, 2, 20, false);
	public static Img SMALL_RIGHT_BORDER = new Img("ui", 83, 846, 2, 20, false);
	public static Img MEDIUM_RIGHT_BORDER = new Img("ui", 83, 846, 2, 20, false);
	public static Img LARGE_RIGHT_BORDER = new Img("ui", 83, 846, 2, 20, false);
	
	public static Clr BAR_BG = Clr.fromHex("4f442f");
	
	public void tick(Input in, int x, int y, int w, int h) {
		if (in.scrollAmount() != 0 && MyDraw.in(x, y, w, h, in.cursor())) {
			offset = StrictMath.max(0, offset - in.scrollAmount() * AirshipGame.scrollSpeed / 60);
		}
	}
	
	public void draw(MyDraw d, final int x, final int y, final int h, int panelHeight, final int range) {
		final int panelH = StrictMath.max(panelHeight, 0);
		if (prevRange != 0 && (offset >= prevRange - panelH || (prevRange < panelH && range > panelH))) {
			offset = StrictMath.max(0, range - panelH);
		} else {
			offset = StrictMath.min(StrictMath.max(range - panelH, 0), offset);
		}
		prevRange = range;
		boolean upHover = range > panelH && MyDraw.in(x, y, SCROLL_BAR_W, SCROLL_BAR_W, d.state.cursor);
		boolean downHover = range > panelH && MyDraw.in(x, y + h - SCROLL_BAR_W, SCROLL_BAR_W, SCROLL_BAR_W, d.state.cursor);
		boolean barHover = range > panelH && MyDraw.in(x, y + SCROLL_BAR_W, SCROLL_BAR_W, h - SCROLL_BAR_W * 2, d.state.cursor);
		d.blit(offset > 0 ? (upHover ? UP_BUTTON_LIT : UP_BUTTON) : UP_BUTTON_DISABLED, x - 1, y - 1);
		d.hook(x, y, SCROLL_BAR_W, SCROLL_BAR_W, new Hook(Hook.Type.MOUSE_1_DOWN) {
			@Override
			public void run(Input in, Pt p, Hook.Type type) {
				offset = StrictMath.max(0, offset - AirshipGame.scrollSpeed);
			}
		});
		//d.drawWoodGrain(x + 1, y + SCROLL_BAR_W, SCROLL_BAR_W - 2, h - SCROLL_BAR_W * 2);
		d.rect(barHover ? MyDraw.PROGRESS_BAR_DEEP_INSIDE : BAR_BG, x + 1, y + SCROLL_BAR_W, SCROLL_BAR_W - 2, h - SCROLL_BAR_W * 2);
		int y2 = y + SCROLL_BAR_W;
		while (y2 < y + h - SCROLL_BAR_W) {
			d.blit(LEFT_BORDER, x, y2);
			d.blit(RIGHT_BORDER, x + SCROLL_BAR_W - 2, y2);
			y2 += LEFT_BORDER.srcHeight;
		}
		d.blit(offset < (range - panelH) ? (downHover ? DOWN_BUTTON_LIT : DOWN_BUTTON) : DOWN_BUTTON_DISABLED, x - 1, y + h - SCROLL_BAR_W);
		d.hook(x, y + h - SCROLL_BAR_W, SCROLL_BAR_W, SCROLL_BAR_W, new Hook(Hook.Type.MOUSE_1_DOWN) {
			@Override
			public void run(Input in, Pt p, Hook.Type type) {
				offset = StrictMath.min(range, offset + AirshipGame.scrollSpeed);
			}
		});
		if (range > panelH) {
			final int thumbH = StrictMath.max(SCROLL_THUMB_TOP.srcHeight + SCROLL_THUMB_MIDDLE.srcHeight + SCROLL_THUMB_BOTTOM.srcHeight, (h - SCROLL_BAR_W * 2) * panelH / range);
			int thumbY = y + SCROLL_BAR_W + 1 + (h - SCROLL_BAR_W * 2 - 2 - thumbH) * offset / (range - panelH);
			int tx = x + SCROLL_BAR_W / 2 - SCROLL_THUMB_TOP.srcWidth / 2;
			d.blit(SCROLL_THUMB_TOP, tx, thumbY);
			int ty = thumbY + SCROLL_THUMB_TOP.srcHeight;
			d.blit(SCROLL_THUMB_MIDDLE, tx, ty);
			ty += SCROLL_THUMB_MIDDLE.srcHeight;
			while (ty < thumbY + thumbH - SCROLL_THUMB_BOTTOM.srcHeight) {
				if (SCROLL_THUMB_MIDDLE.machineImgCache != null) {
					((Image) SCROLL_THUMB_MIDDLE.machineImgCache).draw(
						tx,
						ty,
						tx + SCROLL_THUMB_MIDDLE.srcWidth,
						ty + StrictMath.min(SCROLL_THUMB_MIDDLE.srcHeight, thumbY + thumbH - SCROLL_THUMB_BOTTOM.srcHeight - ty),
						0,
						0,
						SCROLL_THUMB_MIDDLE.srcWidth,
						StrictMath.min(SCROLL_THUMB_MIDDLE.srcHeight, thumbY + thumbH - SCROLL_THUMB_BOTTOM.srcHeight - ty));
				}
				ty += SCROLL_THUMB_MIDDLE.srcHeight;
			}
			d.blit(SCROLL_THUMB_BOTTOM, tx, thumbY + thumbH - SCROLL_THUMB_BOTTOM.srcHeight);
			d.hook(x, y + SCROLL_BAR_W, SCROLL_BAR_W, h - SCROLL_BAR_W * 2, new Hook("scrollBar", Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN, Hook.Type.TEST) {
				@Override
				public void run(Input in, Pt p, Hook.Type type) {
					offset = StrictMath.max(0, ((int) p.y - thumbH / 2) * (range - panelH) / StrictMath.max(1, h - SCROLL_BAR_W * 2 - 2 - thumbH));
				}
			});
			/*int thumbY = y + SCROLL_BAR_W + 1 + (h - SCROLL_BAR_W * 2 - 2 - SCROLL_THUMB.srcHeight) * offset / (range - panelH);
			d.blit(SCROLL_THUMB, x + SCROLL_BAR_W / 2 - SCROLL_THUMB.srcWidth / 2, thumbY);
			d.hook(x, y + SCROLL_BAR_W, SCROLL_BAR_W, h - SCROLL_BAR_W * 2, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
				@Override
				public void run(Input in, Pt p, Hook.Type type) {
					offset = ((int) p.y - SCROLL_THUMB.srcHeight / 2) * (range - panelH) / (h - SCROLL_BAR_W * 2 - 2 - SCROLL_THUMB.srcHeight);
				}
			});*/
		}
	}
	
	private <T> void recacheHeight(MyDraw d, List<T> els, ScrollElementAdapter<T> ad, int availableWidth) {
		for (int i = 0; i < els.size(); i++) {
			T t = els.get(i);
			if (t instanceof HeightCacheable) {
				HeightCacheable hct = (HeightCacheable) t;
				if (hct.getHeightCache() == 0 || availableWidth != cachedAvailableWidth) {
					hct.setHeightCache(ad.getHeight(t, d, availableWidth));
				}
			}
		}
		cachedAvailableWidth = availableWidth;
	}
	
	private <T> int getHeight(ScrollElementAdapter<T> ad, T t, MyDraw d, int availableWidth) {
		if (t instanceof HeightCacheable) {
			return ((HeightCacheable) t).getHeightCache();
		} else {
			return ad.getHeight(t, d, availableWidth);
		}
	}
	
	public <T> void draw(MyDraw d, int x, int y, int w, int h, List<T> els, ScrollElementAdapter<T> ad) {
		
		d.state.hookClipRect = new IntRect(x, y, w, h);
		int panelW = w - SCROLL_BAR_W - MyDraw.PANEL_INSET * 2;
		recacheHeight(d, els, ad, panelW);
		int range = 0;
		for (int i = 0; i < els.size(); i++) {
			range += getHeight(ad, els.get(i), d, panelW);
		}
		int panelH = h - MyDraw.PANEL_INSET * 2;
		if (stickToBottom && prevRange != 0 && offset >= prevRange - panelH) {
			offset = StrictMath.max(0, range - panelH);
		} else if (prevRange != 0 && (/*offset >= prevRange - panelH || */(prevRange < panelH && range > panelH))) {
			offset = StrictMath.min(offset, StrictMath.max(0, range - panelH));
		} else {
			offset = StrictMath.min(StrictMath.max(range - panelH, 0), offset);
		}
		prevRange = range;
		d.drawPanel(x, y, w - SCROLL_BAR_W + MyDraw.PANEL_INSET, h, -1);
		draw(d, x + w - SCROLL_BAR_W, y, h, panelH, range);
		int endY = y + h - MyDraw.PANEL_INSET;
		int startY = y + MyDraw.PANEL_INSET;
		y = y + MyDraw.PANEL_INSET - offset;
		x += MyDraw.PANEL_INSET;
		Graphics g = (Graphics) d.frame().nativeRenderer();
		for (int i = 0; i < els.size(); i++) {
			int elH = getHeight(ad, els.get(i), d, panelW);
			if (y + elH > startY && y < endY) {
				clipX = x;
				clipY = startY;
				clipW = panelW;
				clipH = endY - startY;
				g.setClip(x, startY, panelW, endY - startY);
				d.restrictHooks(x, startY, panelW, endY - startY);
				ad.draw(els.get(i), d, x, y, panelW);
				d.clearHookRestriction();
				g.clearClip();
			}
			y += elH;
		}
		d.state.hookClipRect = null;
	}
	
	public <T> void drawNaked(MyDraw d, int x, int y, int w, int h, List<T> els, ScrollElementAdapter<T> ad) {
		d.state.hookClipRect = new IntRect(x, y, w, h);
		int areaW = w - SCROLL_BAR_W - MyDraw.PANEL_INSET;
		recacheHeight(d, els, ad, areaW);
		int range = 0;
		for (int i = 0; i < els.size(); i++) {
			range += getHeight(ad, els.get(i), d, areaW);
		}
		int areaH = h;
		if (prevRange != 0 && (offset >= prevRange - areaH || (prevRange < areaH && range > areaH))) {
			offset = StrictMath.max(0, range - areaH);
		} else {
			offset = StrictMath.min(StrictMath.max(range - areaH, 0), offset);
		}
		prevRange = range;
		draw(d, x + w - SCROLL_BAR_W, y, h, areaH, range);
		int endY = y + h - MyDraw.PANEL_INSET;
		int startY = y + MyDraw.PANEL_INSET;
		y = y + MyDraw.PANEL_INSET - offset;
		x += MyDraw.PANEL_INSET;
		Graphics g = (Graphics) d.frame().nativeRenderer();
		for (int i = 0; i < els.size(); i++) {
			int elH = getHeight(ad, els.get(i), d, areaW);
			if (y + elH > startY && y < endY) {
				clipX = x;
				clipY = startY;
				clipW = areaW;
				clipH = endY - startY;
				g.setClip(x, startY, areaW, endY - startY);
				d.restrictHooks(x, startY, areaW, endY - startY);
				ad.draw(els.get(i), d, x, y, areaW);
				d.clearHookRestriction();
				g.clearClip();
			}
			y += elH;
		}
		d.state.hookClipRect = null;
	}
	
	public <T> void drawMulticolumnNaked(MyDraw d, int numCols, int x, int y, int w, int h, List<T> els, ScrollElementAdapter<T> ad) {
		int areaW = w - SCROLL_BAR_W;
		int colW = (areaW) / numCols;
		recacheHeight(d, els, ad, colW);
		int targetColH = 0;
		for (int i = 0; i < els.size(); i++) {
			targetColH += getHeight(ad, els.get(i), d, colW);
		}
		targetColH /= numCols;
		ArrayList<ArrayList<T>> cols = new ArrayList<ArrayList<T>>();
		for (int i = 0; i < numCols; i++) {
			cols.add(new ArrayList<T>());
		}
		int range = 0;
		int currentCol = 0;
		int currentColH = 0;
		for (int i = 0; i < els.size(); i++) {
			int elH = getHeight(ad, els.get(i), d, colW);
			if (currentCol == numCols - 1 || currentColH == 0 || currentColH + elH / 2 < targetColH) {
				currentColH += elH;
			} else {
				currentCol++;
				currentColH = elH;
			}
			cols.get(currentCol).add(els.get(i));
			range = Math.max(currentColH, range);
		}
		int areaH = h;
		if (prevRange != 0 && (offset >= prevRange - areaH || (prevRange < areaH && range > areaH))) {
			offset = StrictMath.max(0, range - areaH);
		} else {
			offset = StrictMath.min(StrictMath.max(range - areaH, 0), offset);
		}
		prevRange = range;
		draw(d, x + w - SCROLL_BAR_W, y, h, areaH, range);
		final int endY = y + h - MyDraw.PANEL_INSET;
		final int startY = y + MyDraw.PANEL_INSET;
		y = y + MyDraw.PANEL_INSET - offset;
		x += MyDraw.PANEL_INSET;
		areaW -= MyDraw.PANEL_INSET;
		Graphics g = (Graphics) d.frame().nativeRenderer();
		for (int colI = 0; colI < numCols; colI++) {
			int x2 = x + (colW) * colI;
			int y2 = y;
			for (int i = 0; i < cols.get(colI).size(); i++) {
				int elH = getHeight(ad, cols.get(colI).get(i), d, colW);
				if (y2 + elH > startY && y2 < endY) {
					clipX = x;
					clipY = startY;
					clipW = areaW;
					clipH = endY - startY;
					g.setClip(x, startY, areaW, endY - startY);
					d.restrictHooks(x, startY, areaW, endY - startY);
					ad.draw(cols.get(colI).get(i), d, x2, y2, colW);
					d.clearHookRestriction();
					g.clearClip();
				}
				y2 += elH;
			}
		}
	}
	
	public <T> void scrollTo(boolean naked, MyDraw d, int x, int y, int w, int h, List<T> els, ScrollElementAdapter<T> ad, T scrollTo) {
		int panelW = w - SCROLL_BAR_W - (naked ? 0 : MyDraw.PANEL_INSET * 2);
		recacheHeight(d, els, ad, panelW);
		int panelH = h - (naked ? 0 : MyDraw.PANEL_INSET * 2);
		int range = 0;
		int scrollToY = 0;
		int scrollToH = 0;
		boolean scrollToFound = false;
		for (int i = 0; i < els.size(); i++) {
			int eH = getHeight(ad, els.get(i), d, panelW);
			range += eH;
			if (els.get(i).equals(scrollTo)) {
				scrollToFound = true;
				scrollToH = eH;
			}
			if (!scrollToFound) {
				scrollToY += eH;
			}
		}
		offset = StrictMath.max(0, scrollToY + scrollToH / 2 - h / 2);
	}
	
	public static interface ScrollElementAdapter<T> {
		public int getHeight(T t, MyDraw d, int availableWidth);
		public void draw(T t, MyDraw d, int x, int y, int width);
	}
	
	public static interface HeightCacheable {
		public void setHeightCache(int h);
		public int getHeightCache();
	}
}
