package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Pt;

public class IntRect {
	int x;
	int y;
	int w;
	int h;

	public IntRect() {}

	public IntRect(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	
	public void update(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public boolean contains(Pt p) {
		return p.x >= x && p.y >= y && p.x < x + w && p.y < y + h;
	}
}
