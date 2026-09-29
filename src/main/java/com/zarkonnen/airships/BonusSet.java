package com.zarkonnen.airships;

import java.util.ArrayList;

/** Bare-bones, fast set implementation for bonuses. */
public class BonusSet {
	private static BonusSet EMPTY = new BonusSet();
	
	public static void resetCount() {
		EMPTY = null;
	}
	
	public static BonusSet empty() {
		if (EMPTY == null) {
			EMPTY = new BonusSet();
		}
		return EMPTY;
	}
	
	public final boolean[] contains = new boolean[Loadable.all(Bonus.class).size()];
	private final long[] data = new long[Loadable.all(Bonus.class).size() / 64 + 1];
	
	public BonusSet() {}
	public BonusSet(Bonus... boni) {
		for (int i = 0; i < boni.length; i++) {
			contains[boni[i].ordinal()] = true;
			data[boni[i].ordinal() / 64] |= 1L << (boni[i].ordinal() % 64);
		}
	}
	
	public void set(int index, boolean value) {
		contains[index] = value;
		if (value) {
			data[index / 64] |= 1L << (index % 64);
		} else {
			data[index / 64] &= Long.rotateLeft(0XFFFFFFFFFFFFFFFEl, index % 64);
		}
	}

	public ArrayList<Bonus> list() {
		ArrayList<Bonus> l = new ArrayList<Bonus>();
		for (Bonus b : Loadable.all(Bonus.class)) {
			if (contains[b.ordinal()]) {
				l.add(b);
			}
		}
		return l;
	}
	
	public boolean containsAll(BonusSet set) {
		/*for (int i = 0; i < contains.length; i++) {
			if (!contains[i] && set.contains[i]) { return false; }
		}
		return true;*/
		for (int i = 0; i < data.length; i++) {
			if ((data[i] & set.data[i]) != set.data[i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean intersects(BonusSet set) {
		/*for (int i = 0; i < contains.length; i++) {
			if (contains[i] && set.contains[i]) { return true; }
		}
		return false;*/
		for (int i = 0; i < data.length; i++) {
			if ((data[i] & set.data[i]) != 0) {
				return true;
			}
		}
		return false;
	}
	
	public Bonus firstCommon(BonusSet set) {
		ArrayList<Bonus> bs = Loadable.all(Bonus.class);
		for (int i = 0; i < bs.size(); i++) {
			Bonus b = bs.get(i);
			if (contains[b.ordinal()] && set.contains[b.ordinal()]) {
				return b;
			}
		}
		return null;
	}

	public void add(Bonus b) {
		contains[b.ordinal()] = true;
		data[b.ordinal() / 64] |= 1L << (b.ordinal() % 64);
	}
	
	public void remove(Bonus b) {
		contains[b.ordinal()] = false;
		data[b.ordinal() / 64] &= Long.rotateLeft(0XFFFFFFFFFFFFFFFEl, b.ordinal() % 64);
	}

	public void clear() {
		for (int i = 0; i < contains.length; i++) {
			contains[i] = false;
		}
		for (int i = 0; i < data.length; i++) {
			data[i] = 0;
		}
	}

	public BonusSet addAll(BonusSet bs2) {
		for (int i = 0; i < contains.length; i++) {
			contains[i] |= bs2.contains[i];
		}
		for (int i = 0; i < data.length; i++) {
			data[i] |= bs2.data[i];
		}
		return this;
	}
	
	public BonusSet removeAll(BonusSet bs2) {
		for (int i = 0; i < contains.length; i++) {
			if (bs2.contains[i]) {
				contains[i] = false;
			}
		}
		for (int i = 0; i < data.length; i++) {
			data[i] &= ~bs2.data[i];
		}
		return this;
	}
	
	public BonusSet retainAll(BonusSet bs2) {
		for (int i = 0; i < contains.length; i++) {
			if (!bs2.contains[i]) {
				contains[i] = false;
			}
		}
		for (int i = 0; i < data.length; i++) {
			data[i] &= bs2.data[i];
		}
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BonusSet)) { return false; }
		BonusSet s2 = (BonusSet) o;
		/*for (int i = 0; i < contains.length; i++) {
			if (contains[i] != s2.contains[i]) { return false; }
		}*/
		
		for (int i = 0; i < data.length; i++) {
			if (data[i] != s2.data[i]) { return false; }
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int code = 0;
		/*for (int i = 0; i < contains.length; i++) {
			if (contains[i]) {
				code += i;
			}
			code *= 37;
		}*/
		for (int i = 0; i < data.length; i++) {
			code += Long.hashCode(data[i]);
			code *= 37;
		}
		return code;
	}
	
	@Override
	public BonusSet clone() {
		BonusSet s2 = new BonusSet();
		System.arraycopy(contains, 0, s2.contains, 0, contains.length);
		System.arraycopy(data, 0, s2.data, 0, data.length);
		return s2;
	}
}
