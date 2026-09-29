package com.zarkonnen.airships;

public final class UIGlowRect {
	public final int x, y, w, h;
	public int hoverMs = 0;
	public boolean confirmed = true;
	public static final int MAX_GLOW = 512;

	public UIGlowRect(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	
	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof UIGlowRect)) { return false; }
		UIGlowRect uigr2 = (UIGlowRect) o2;
		return
				x == uigr2.x &&
				y == uigr2.y &&
				w == uigr2.w &&
				h == uigr2.h;
	}
	
	@Override
	public int hashCode() {
		return x + y * 1024 + w * 256 * 1024 + h * 4096 * 1024;
	}
}
