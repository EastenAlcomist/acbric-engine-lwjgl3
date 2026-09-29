package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Clr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.Collections;
import java.util.List;

public abstract class BonusableValue<T> {
	public abstract T get(BonusSet bonuses);
	public abstract String explain(BonusSet bonuses, ValueFormatter<T> formatter, ValueTransform<T> transform);
	public abstract List<String> descriptions(BonusSet bonuses);
	
	public List<BonusSet> getBonusesIfAvailable() { return Collections.emptyList(); }
	
	public static boolean isZero(BonusableValue<Double> v) {
		if (!(v instanceof NoBonus)) { return false; }
		return ((NoBonus<Double>) v).value == 0;
	}
	
	public String explain(BonusSet bonuses) {
		return explain(bonuses, standard(), identity());
	}
	public String explain(BonusSet bonuses, ValueFormatter<T> formatter) {
		return explain(bonuses, formatter, identity());
	}
	public String explain(BonusSet bonuses, ValueTransform<T> transform) {
		return explain(bonuses, standard(), transform);
	}
	public String bexplain(boolean explain, BonusSet bonuses) {
		return explain? explain(bonuses, standard(), identity()) : "" + get(bonuses);
	}
	public String bexplain(boolean explain, BonusSet bonuses, ValueFormatter<T> formatter) {
		return explain ? explain(bonuses, formatter, identity()) : formatter.format(get(bonuses));
	}
	public String bexplain(boolean explain, BonusSet bonuses, ValueTransform<T> transform) {
		return explain ? explain(bonuses, standard(), transform) : "" + transform.transform(get(bonuses));
	}
	public String bexplain(boolean explain, BonusSet bonuses, ValueFormatter<T> formatter, ValueTransform<T> transform) {
		return explain ? explain(bonuses, formatter, transform) : formatter.format(transform.transform(get(bonuses)));
	}
		
	private static final ValueTransform<Object> IDENTITY = new ValueTransform<Object>() {
		@Override
		public Object transform(Object v) {
			return v;
		}
	};
	
	protected ValueTransform<T> identity() {
		return (ValueTransform<T>) IDENTITY;
	}
		
	private static final ValueFormatter<Object> STANDARD = new ValueFormatter<Object>() {
		@Override
		public String format(Object v) {
			return "" + v;
		}
	};
	
	protected ValueFormatter<T> standard() {
		return (ValueFormatter<T>) STANDARD;
	}
	
	public static <T>BonusableValue<T> of(T value) {
		return new NoBonus<T>(value);
	}
	
	public static boolean isAlways(BonusableValue<Integer> bv, int value) {
		return bv instanceof NoBonus && ((NoBonus<Integer>) bv).value == value;
	}
	
	private static final class NoBonus<T> extends BonusableValue<T> {
		private final T value;

		private NoBonus(T value) {
			this.value = value;
		}
		
		@Override
		public T get(BonusSet bonuses) {
			return value;
		}

		@Override
		public String explain(BonusSet bonuses, ValueFormatter<T> formatter, ValueTransform<T> transform) {
			return formatter.format(transform.transform(value));
		}

		@Override
		public List<String> descriptions(BonusSet bonuses) {
			return Collections.emptyList();
		}
	}
	
	private static final class SingleBonus<T> extends BonusableValue<T> {
		private final T base;
		private final T bonused;
		private final Bonus bonus;
		private final String description;

		@Override
		public T get(BonusSet bonuses) {
			return bonuses.contains[bonus.ordinal()] ? bonused : base;
		}
		
		@Override
		public List<String> descriptions(BonusSet bonuses) {
			if (bonuses.contains[bonus.ordinal()] && description != null) {
				return Collections.singletonList(_t(description));
			}
			return Collections.emptyList();
		}

		private SingleBonus(T base, Bonus bonus, T bonused, String description) {
			this.base = base;
			this.bonused = bonused;
			this.bonus = bonus;
			this.description = description;
		}
		
		@Override
		public String explain(BonusSet bonuses, ValueFormatter<T> formatter, ValueTransform<T> transform) {
			T tBase = transform.transform(base);
			T tBonused = transform.transform(bonused);
			if (bonuses.contains[bonus.ordinal()]) {
				if (tBase instanceof Integer) {
					ValueFormatter<Integer> f = (ValueFormatter<Integer>) formatter;
					Integer iBase = (Integer) tBase;
					Integer iBonused = (Integer) tBonused;
					if (iBonused > iBase) {
						return f.format(iBonused) + " (+" + f.format(iBonused - iBase) + " " + _t("from_bonus_x", bonus.getName()) + ")";
					}
					if (iBonused < iBase) {
						return f.format(iBonused) + " (" + f.format(iBonused - iBase) + " " + _t("from_bonus_x", bonus.getName()) + ")";
					}
					return f.format(iBase);
				}
				if (tBase instanceof Double) {
					ValueFormatter<Double> f = (ValueFormatter<Double>) formatter;
					Double dBase = (Double) tBase;
					Double dBonused = (Double) tBonused;
					if (dBonused > dBase) {
						return f.format(dBonused) + " (+" + f.format(dBonused - dBase) + " " + _t("from_bonus_x", bonus.getName()) + ")";
					}
					if (dBonused < dBase) {
						return f.format(dBonused) + " (" + f.format(dBonused - dBase) + " " + _t("from_bonus_x", bonus.getName()) + ")";
					}
					return f.format(dBase);
				}
				return formatter.format(tBonused) + " " + _t("from_bonus_x", bonus.getName());
			} else {
				return formatter.format(tBase);
			}
		}
		
		@Override
		public List<BonusSet> getBonusesIfAvailable() {
			return Collections.singletonList(new BonusSet(bonus));
		}
	}
	
	private static final class IntArithmeticBonus extends BonusableValue<Integer> {
		private final int base, min, max;
		private final Bonus[] myBonuses;
		private final int[] bonusOrdinals, bonusDeltas;
		private final double[] bonusMults, bonusDivs;
		private final String[] descriptions;

		@Override
		public Integer get(BonusSet bonuses) {
			double value = base;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					value += bonusDeltas[i];
				}
			}
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					value *= bonusMults[i] / bonusDivs[i];
				} 
			}
			return StrictMath.min(max, StrictMath.max(min, (int) value));
		}

		private IntArithmeticBonus(int base, Bonus[] bonuses, int[] bonusDeltas, double[] bonusMults, double[] bonusDivs, String[] descriptions, int min, int max) {
			this.base = base;
			this.min = min;
			this.max = max;
			this.myBonuses = bonuses;
			this.bonusOrdinals = new int[bonuses.length];
			for (int i = 0; i < bonuses.length; i++) {
				bonusOrdinals[i] = bonuses[i].ordinal();
			}
			this.bonusDeltas = bonusDeltas;
			this.bonusMults = bonusMults;
			this.bonusDivs = bonusDivs;
			this.descriptions = descriptions;
		}
		
		@Override
		public String explain(BonusSet bonuses, ValueFormatter<Integer> f, ValueTransform<Integer> t) {
			StringBuilder sb = new StringBuilder();
			sb.append(f.format(t.transform(get(bonuses))));
			boolean hasEffect = false;
			boolean hasDelta = false;
			boolean hasMult = false;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					hasEffect = true;
					hasDelta |= bonusDeltas[i] != 0;
					hasMult |= bonusMults[i] != 1 || bonusDivs[i] != 1;
				}
			}
			if (!hasEffect) { return sb.toString(); }
			sb.append(" (");
			if (hasDelta && hasMult) {
				sb.append("(");
			}
			boolean first = true;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					int delta = t.transform(base + bonusDeltas[i]) - t.transform(base);
					if (delta > 0) {
						if (!first) { sb.append(" "); }
						first = false;
						sb.append("+").append(f.format(delta)).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
					}
					if (delta < 0) {
						if (!first) { sb.append(" "); }
						first = false;
						sb.append("-").append(f.format(-delta)).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
					}
				}
			}
			if (hasDelta && hasMult) {
				sb.append(")");
			}
			first = true;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					if (bonusMults[i] != 1) {
						if (!first) { sb.append(" "); }
						first = false;
						if (bonusMults[i] == (int) bonusMults[i]) {
							sb.append("x").append((int) bonusMults[i]).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
						} else {
							sb.append("x").append(bonusMults[i]).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
						}
					}
					if (bonusDivs[i] != 1) {
						if (!first) { sb.append(" "); }
						first = false;
						if (bonusDivs[i] == (int) bonusDivs[i]) {
							sb.append("÷").append((int) bonusDivs[i]).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
						} else {
							sb.append("÷").append(bonusDivs[i]).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
						}
					}
				}
			}
			sb.append(")");
			return sb.toString();
		}

		@Override
		public List<String> descriptions(BonusSet bonuses) {
			ArrayList<String> l = null;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]] && descriptions[i] != null) {
					if (l == null) { l = new ArrayList<String>(); }
					l.add(_t(descriptions[i]));
				}
			}
			if (l == null) {
				return Collections.emptyList();
			} else {
				return l;
			}
		}
	}
	
	private static final class DoubleArithmeticBonus extends BonusableValue<Double> {
		private final double base;
		private final Bonus[] myBonuses;
		private final int[] bonusOrdinals;
		private final double[] bonusDeltas, bonusMults, bonusDivs;
		private final String[] descriptions;

		@Override
		public Double get(BonusSet bonuses) {
			double value = base;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					value += bonusDeltas[i];
				}
			}
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					value *= bonusMults[i] / bonusDivs[i];
				} 
			}
			return value;
		}

		private DoubleArithmeticBonus(double base, Bonus[] bonuses, double[] bonusDeltas, double[] bonusMults, double[] bonusDivs, String[] descriptions) {
			this.base = base;
			this.myBonuses = bonuses;
			this.bonusOrdinals = new int[bonuses.length];
			for (int i = 0; i < bonuses.length; i++) {
				bonusOrdinals[i] = bonuses[i].ordinal();
			}
			this.bonusDeltas = bonusDeltas;
			this.bonusMults = bonusMults;
			this.bonusDivs = bonusDivs;
			this.descriptions = descriptions;
		}
		
		@Override
		public String explain(BonusSet bonuses, ValueFormatter<Double> f, ValueTransform<Double> t) {
			StringBuilder sb = new StringBuilder();
			sb.append(f.format(t.transform(get(bonuses))));
			boolean hasEffect = false;
			boolean hasDelta = false;
			boolean hasMult = false;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					hasEffect = true;
					hasDelta |= bonusDeltas[i] != 0;
					hasMult |= bonusMults[i] != 1 || bonusDivs[i] != 1;
				}
			}
			if (!hasEffect) { return sb.toString(); }
			sb.append(" (");
			if (hasDelta && hasMult) {
				sb.append("(");
			}
			boolean first = true;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					double delta = t.transform(base + bonusDeltas[i]) - t.transform(base);
					if (delta > 0) {
						if (!first) { sb.append(" "); }
						first = false;
						sb.append("+").append(f.format(delta)).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
					}
					if (delta < 0) {
						if (!first) { sb.append(" "); }
						first = false;
						sb.append("-").append(f.format(-delta)).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
					}
				}
			}
			if (hasDelta && hasMult) {
				sb.append(")");
			}
			first = true;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]]) {
					if (bonusMults[i] != 1) {
						if (!first) { sb.append(" "); }
						first = false;
						sb.append(" x").append(bonusMults[i]).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
					}
					if (bonusDivs[i] != 1) {
						if (!first) { sb.append(" "); }
						first = false;
						sb.append(" ÷").append(bonusDivs[i]).append(" ").append(_t("from_bonus_x", myBonuses[i].getName()));
					}
				}
			}
			sb.append(")");
			return sb.toString();
		}
		
		@Override
		public List<String> descriptions(BonusSet bonuses) {
			ArrayList<String> l = null;
			for (int i = 0; i < bonusOrdinals.length; i++) {
				if (bonuses.contains[bonusOrdinals[i]] && descriptions[i] != null) {
					if (l == null) { l = new ArrayList<String>(); }
					l.add(_t(descriptions[i]));
				}
			}
			if (l == null) {
				return Collections.emptyList();
			} else {
				return l;
			}
		}
	}
	
	public static interface Derive<F, T> {
		public T derive(F from);
	}
	
	private static class DeriveList<F, T> implements Derive<ArrayList<F>, ArrayList<T>> {
		private final Derive<F, T> inner;

		public DeriveList(Derive<F, T> inner) {
			this.inner = inner;
		}

		@Override
		public ArrayList<T> derive(ArrayList<F> from) {
			ArrayList<T> tl = new ArrayList<T>();
			for (F f : from) {
				tl.add(inner.derive(f));
			}
			return tl;
		}
	}
	
	public static <F, T> Derive<ArrayList<F>, ArrayList<T>> list(Derive<F, T> inner) {
		return new DeriveList<F, T>(inner);
	}
		
	public static <F, T> BonusableValue<T> derive(BonusableValue<F> from, Derive<F, T> d) {
		if (from == null) {
			throw new NullPointerException("from");
		}
		if (d == null) {
			throw new NullPointerException("d");
		}
		if (from instanceof NoBonus) {
			return new NoBonus<T>(d.derive(from.get(BonusSet.empty())));
		}
		if (from instanceof ObjectVariantBonus) {
			ObjectVariantBonus<F> ovb = (ObjectVariantBonus<F>) from;
			ArrayList<T> deriveds = new ArrayList<T>();
			for (F f : ovb.objects) {
				deriveds.add(d.derive(f));
			}
			return new ObjectVariantBonus<T>(d.derive(ovb.base), ovb.myBonuses, deriveds, ovb.descriptions);
		}
		if (from instanceof SingleBonus) {
			SingleBonus<F> sb = (SingleBonus<F>) from;
			return new SingleBonus<T>(d.derive(sb.get(BonusSet.empty())), sb.bonus, d.derive(sb.bonused), sb.description);
		}
		if (from instanceof SetBonus) {
			SetBonus<F> sb = (SetBonus<F>) from;
			ArrayList<T> deriveds = new ArrayList<T>();
			for (F f : sb.objects) {
				deriveds.add(d.derive(f));
			}
			return new SetBonus<T>(d.derive(((SetBonus<F>) from).base), ((SetBonus<F>) from).sets, deriveds, sb.descriptions);
		}
		return new RuntimeDerivingBonus<F, T>(from, d);
	}
	
	private static final class RuntimeDerivingBonus<F, T> extends BonusableValue<T> {
		private final BonusableValue<F> wrapped;
		private final Derive<F, T> d;

		public RuntimeDerivingBonus(BonusableValue<F> wrapped, Derive<F, T> d) {
			this.wrapped = wrapped;
			this.d = d;
		}

		@Override
		public T get(BonusSet bonuses) {
			return d.derive(wrapped.get(bonuses));
		}
		
		@Override
		public List<BonusSet> getBonusesIfAvailable() {
			return wrapped.getBonusesIfAvailable();
		}

		@Override
		public String explain(BonusSet bonuses, ValueFormatter<T> formatter, ValueTransform<T> transform) {
			return formatter.format(transform.transform(get(bonuses)));
		}

		@Override
		public List<String> descriptions(BonusSet bonuses) {
			return wrapped.descriptions(bonuses);
		}
	}
	
	private static final class ObjectVariantBonus<T> extends BonusableValue<T> {
		private final ArrayList<Bonus> myBonuses;
		private final ArrayList<T> objects;
		private final ArrayList<String> descriptions;
		private final T base;

		public ObjectVariantBonus(T base, ArrayList<Bonus> myBonuses, ArrayList<T> objects, ArrayList<String> descriptions) {
			this.base = base;
			this.myBonuses = myBonuses;
			this.objects = objects;
			this.descriptions = descriptions;
		}

		@Override
		public T get(BonusSet bonuses) {
			for (int i = 0; i < myBonuses.size(); i++) {
				if (bonuses.contains[myBonuses.get(i).ordinal()]) {
					return objects.get(i);
				}
			}
			return base;
		}
		
		@Override
		public List<BonusSet> getBonusesIfAvailable() {
			ArrayList<BonusSet> list = new ArrayList<BonusSet>();
			for (Bonus b : myBonuses) {
				list.add(new BonusSet(b));
			}
			return list;
		}

		@Override
		public String explain(BonusSet bonuses, ValueFormatter<T> f, ValueTransform<T> t) {
			for (int i = 0; i < myBonuses.size(); i++) {
				if (bonuses.contains[myBonuses.get(i).ordinal()]) {
					return f.format(t.transform(get(bonuses))) + " (" + _t("from_bonus_x", myBonuses.get(i).getName()) + ")";
				}
			}
			return f.format(t.transform(get(bonuses)));
		}

		@Override
		public List<String> descriptions(BonusSet bonuses) {
			for (int i = 0; i < myBonuses.size(); i++) {
				if (bonuses.contains[myBonuses.get(i).ordinal()]) {
					if (descriptions.get(i) == null) {
						return Collections.emptyList();
					}
					return Collections.singletonList(descriptions.get(i));
				}
			}
			return Collections.emptyList();
		}
	}
	
	private static final class SetBonus<T> extends BonusableValue<T> {
		private final T base;
		private final ArrayList<BonusSet> sets;
		private final ArrayList<T> objects;
		private final ArrayList<String> descriptions;

		public SetBonus(T base, ArrayList<BonusSet> sets, ArrayList<T> objects, ArrayList<String> descriptions) {
			this.base = base;
			this.sets = sets;
			this.objects = objects;
			this.descriptions = descriptions;
		}

		@Override
		public T get(BonusSet bonuses) {
			for (int i = 0; i < sets.size(); i++) {
				if (bonuses.containsAll(sets.get(i))) {
					return objects.get(i);
				}
			}
			return base;
		}
		
		@Override
		public List<BonusSet> getBonusesIfAvailable() {
			return sets;
		}

		@Override
		public String explain(BonusSet bonuses, ValueFormatter<T> f, ValueTransform<T> t) {
			for (int i = 0; i < sets.size(); i++) {
				if (bonuses.containsAll(sets.get(i))) {
					String deltaInfo = "";
					if (base instanceof Integer) {
						Integer delta = ((Integer) t.transform(get(bonuses))) - ((Integer) t.transform(base));
						if (delta > 0) {
							deltaInfo = "+" + f.format((T) delta) + " ";
						}
						if (delta < 0) {
							deltaInfo = "-" + f.format((T) Integer.valueOf(-delta)) + " ";
						}
					}
					if (base instanceof Double) {
						Double delta = ((Double) t.transform(get(bonuses))) - ((Double) t.transform(base));
						if (delta > 0) {
							deltaInfo = "+" + f.format((T) delta) + " ";
						}
						if (delta < 0) {
							deltaInfo = "-" + f.format((T) Double.valueOf(-delta)) + " ";
						}
					}
					return f.format(t.transform(get(bonuses))) + " (" + deltaInfo + _t("from_bonus_x", FormatUtils.stringList(sets.get(i).list(), "", ", ", "")) + ")";
				}
			}
			return f.format(t.transform(get(bonuses)));
		}

		@Override
		public List<String> descriptions(BonusSet bonuses) {
			for (int i = 0; i < sets.size(); i++) {
				if (bonuses.containsAll(sets.get(i))) {
					return (List<String>) (descriptions.get(i) == null ? Collections.emptyList() : Collections.singletonList(descriptions.get(i)));
				}
			}
			return Collections.emptyList();
		}
	}
	
	public static interface FromJSON<T> {
		public T construct(JSONObject o, BonusSet bs);
	}
	
	/** 解析 Img 的 FromJSON。defW/defH 是 JSON 中缺省 w/h 时的默认子图尺寸（原版 icon 用 16×16，icon32 用 32×32）。 */
	public static FromJSON<Img> ImgFromJSON(final int defW, final int defH) {
		return new FromJSON<Img>() {
			@Override
			public Img construct(JSONObject io, BonusSet bs) {
				return new Img(io.getString("src"), io.getInt("x"), io.getInt("y"), io.optInt("w", defW), io.optInt("h", defH), io.optBoolean("flipped", false));
			}
		};
	}
	
	public static <T> BonusableValue<T> objectFromJSONRequired(JSONObject o, String key, FromJSON<T> fj) {
		if (o.has(key)) {
			if (o.getJSONObject(key).has("base")) {
				return objectFromJSON(o.getJSONObject(key), fj);
			} else {
				return BonusableValue.of(fj.construct(o.getJSONObject(key), BonusSet.empty()));
			}
		}
		throw new RuntimeException("Missing " + key + ".");
	}
	
	public static <T> BonusableValue<ArrayList<T>> listFromJSON(JSONObject o, String key, ArrayList<T> def, FromJSON<T> fj) {
		return listFromJSON(o, key, def, fj, false);
	}
	
	public static <T> BonusableValue<ArrayList<T>> listFromJSONRequired(JSONObject o, String key, FromJSON<T> fj) {
		return listFromJSON(o, key, null, fj, true);
	}
	
	private static <T> BonusableValue<ArrayList<T>> listFromJSON(JSONObject o, String key, ArrayList<T> def, FromJSON<T> fj, boolean required) {
		if (o.has(key)) {
			if (o.get(key) instanceof JSONObject) {
				JSONObject o2 = o.getJSONObject(key);
				ArrayList<T> base = listFromJSON(o2.getJSONArray("base"), fj, BonusSet.empty());
				if (o2.has("cases")) {
					JSONArray cases = o2.getJSONArray("cases");
					ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
					ArrayList<ArrayList<T>> objects = new ArrayList<ArrayList<T>>();
					ArrayList<String> descriptions = new ArrayList<String>();
					for (int i = 0; i < cases.length(); i++) {
						JSONObject caze = cases.getJSONObject(i);
						BonusSet bs = set(caze.getJSONArray("bonuses"));
						sets.add(bs);
						objects.add(listFromJSON(caze.getJSONArray("value"), fj, bs));
						descriptions.add(caze.optString("description", null));
					}
					return new SetBonus<ArrayList<T>>(base, sets, objects, descriptions);
				} else {
					ArrayList<Bonus> bl = new ArrayList<Bonus>();
					ArrayList<ArrayList<T>> tl = new ArrayList<ArrayList<T>>();
					ArrayList<String> descriptions = new ArrayList<String>();
					for (Bonus b : Bonus.values()) {
						if (o2.has(b.name())) {
							bl.add(b);
							tl.add(listFromJSON(o2.getJSONArray(b.name()), fj, new BonusSet(b)));
							descriptions.add(o2.optString(b.name() + "_desc", null));
						}
					}
					return new ObjectVariantBonus<ArrayList<T>>(base, bl, tl, descriptions);
				}
			} else {
				JSONArray a = o.getJSONArray(key);
				return BonusableValue.of(listFromJSON(a, fj, BonusSet.empty()));
			}
		}
		if (required) {
			throw new RuntimeException("Missing " + key + ".");
		}
		return BonusableValue.of(def);
	}
	
	public static <T> ArrayList<T> listFromJSON(JSONArray a, FromJSON<T> fj, BonusSet b) {
		ArrayList<T> l = new ArrayList<T>();
		for (int i = 0; i < a.length(); i++) {
			l.add(fj.construct(a.getJSONObject(i), b));
		}
		return l;
	}
	
	public static <T extends Loadable> BonusableValue<ArrayList<T>> loadableListFromJSON(JSONObject o, String key, ArrayList<T> def, Class<T> clazz, boolean required) {
		if (o.has(key)) {
			if (o.get(key) instanceof JSONObject) {
				JSONObject o2 = o.getJSONObject(key);
				ArrayList<T> base = listFromJSON(o2.getJSONArray("base"), clazz);
				if (o2.has("cases")) {
					JSONArray cases = o2.getJSONArray("cases");
					ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
					ArrayList<ArrayList<T>> objects = new ArrayList<ArrayList<T>>();
					ArrayList<String> descriptions = new ArrayList<String>();
					for (int i = 0; i < cases.length(); i++) {
						JSONObject caze = cases.getJSONObject(i);
						BonusSet bs = set(caze.getJSONArray("bonuses"));
						sets.add(bs);
						objects.add(listFromJSON(caze.getJSONArray("value"), clazz));
						descriptions.add(o2.optString("desc", null));
					}
					return new SetBonus<ArrayList<T>>(base, sets, objects, descriptions);
				} else {
					ArrayList<Bonus> bl = new ArrayList<Bonus>();
					ArrayList<ArrayList<T>> tl = new ArrayList<ArrayList<T>>();
					ArrayList<String> descriptions = new ArrayList<String>();
					for (Bonus b : Bonus.values()) {
						if (o2.has(b.name())) {
							bl.add(b);
							tl.add(listFromJSON(o2.getJSONArray(b.name()), clazz));
							descriptions.add(o2.optString(b.name() + "_desc", null));
						}
					}
					return new ObjectVariantBonus<ArrayList<T>>(base, bl, tl, descriptions);
				}
			} else {
				JSONArray a = o.getJSONArray(key);
				return BonusableValue.of(listFromJSON(a, clazz));
			}
		}
		if (required) {
			throw new RuntimeException("Missing " + key + ".");
		}
		return BonusableValue.of(def);
	}
	
	public static <T extends Loadable> ArrayList<T> listFromJSON(JSONArray a, Class<T> clazz) {
		ArrayList<T> l = new ArrayList<T>();
		for (int i = 0; i < a.length(); i++) {
			l.add(Loadable.ofName(clazz, a.getString(i)));
		}
		return l;
	}
	
	public static <T> BonusableValue<T> objectFromJSON(JSONObject o, String key, T def, FromJSON<T> fj) {
		if (o.has(key)) {
			if (o.getJSONObject(key).has("base")) {
				return objectFromJSON(o.getJSONObject(key), fj);
			} else {
				return BonusableValue.of(fj.construct(o.getJSONObject(key), BonusSet.empty()));
			}
		}
		return BonusableValue.of(def);
	}
	
	private static BonusSet set(JSONArray a) {
		BonusSet bs = new BonusSet();
		for (int i = 0; i < a.length(); i++) {
			if (Bonus.ofName(a.getString(i)) == null) {
				throw new RuntimeException("Unknown bonus: \"" + a.getString(i) + "\"");
			}
			bs.add(Bonus.ofName(a.getString(i)));
		}
		return bs;
	}
	
	public static <T> BonusableValue<T> objectFromJSON(JSONObject o, FromJSON<T> fj) {
		T base = fj.construct(o.getJSONObject("base"), BonusSet.empty());
		if (o.has("cases")) {
			JSONArray cases = o.getJSONArray("cases");
			ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
			ArrayList<T> objects = new ArrayList<T>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (int i = 0; i < cases.length(); i++) {
				JSONObject caze = cases.getJSONObject(i);
				BonusSet bs = set(caze.getJSONArray("bonuses"));
				sets.add(bs);
				objects.add(fj.construct(caze.getJSONObject("value"), bs));
				descriptions.add(caze.optString("description", null));
			}
			return new SetBonus<T>(base, sets, objects, descriptions);
		} else {
			ArrayList<Bonus> bl = new ArrayList<Bonus>();
			ArrayList<T> tl = new ArrayList<T>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (Bonus b : Bonus.values()) {
				if (o.has(b.name())) {
					bl.add(b);
					tl.add(fj.construct(o.getJSONObject(b.name()), new BonusSet(b)));
					descriptions.add(o.optString(b.name() + "_desc", null));
				}
			}
			return new ObjectVariantBonus<T>(base, bl, tl, descriptions);
		}
	}
	
	public static class BonusableObjectFromJSON<T> implements FromJSON<BonusableValue<T>> {
		public final FromJSON<T> inner;

		public BonusableObjectFromJSON(FromJSON<T> inner) {
			this.inner = inner;
		}

		@Override
		public BonusableValue<T> construct(JSONObject o, BonusSet bs) {
			if (o.has("base")) {
				return objectFromJSON(o, inner);
			} else {
				return BonusableValue.of(inner.construct(o, bs));
			}
		}
	}
	
	public static <T extends Loadable> BonusableValue<T> loadableFromJSON(JSONObject o, String key, T def, Class<T> clazz) {
		if (o.has(key)) {
			try {
				return BonusableValue.of(Loadable.ofName(clazz, o.getString(key)));
			} catch (Exception e) {
				return loadableFromJSON(o.getJSONObject(key), clazz);
			}
		}
		return BonusableValue.of(def);
	}
	
	public static <T extends Loadable> BonusableValue<T> loadableFromJSON(JSONObject o, Class<T> clazz) {
		T base = Loadable.ofName(clazz, o.getString("base"));
		if (o.has("cases")) {
			JSONArray cases = o.getJSONArray("cases");
			ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
			ArrayList<T> objects = new ArrayList<T>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (int i = 0; i < cases.length(); i++) {
				JSONObject caze = cases.getJSONObject(i);
				BonusSet bs = set(caze.getJSONArray("bonuses"));
				sets.add(bs);
				objects.add(Loadable.ofName(clazz, caze.getString("value")));
				descriptions.add(caze.optString("description", null));
			}
			return new SetBonus<T>(base, sets, objects, descriptions);
		} else {
			ArrayList<Bonus> bl = new ArrayList<Bonus>();
			ArrayList<T> tl = new ArrayList<T>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (Bonus b : Bonus.values()) {
				if (o.has(b.name())) {
					bl.add(b);
					tl.add(Loadable.ofName(clazz, o.getString(b.name())));
					descriptions.add(o.optString(b.name() + "_desc", null));
				}
			}
			return new ObjectVariantBonus<T>(base, bl, tl, descriptions);
		}
	}

	public static BonusableValue<Integer> intFromJSON(JSONObject o, String key, int def) {
		return intFromJSONWithDivAndMinAndMax(o, key, def, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	public static BonusableValue<Integer> intFromJSONWithDivAndMinAndMax(JSONObject o, String key, int def, int div, int min, int max) {
		if (o.has(key)) {
			if (o.get(key) instanceof Number) {
				return new NoBonus<Integer>(StrictMath.min(max, StrictMath.max(min, o.getInt(key) / div)));
			}
			if (o.get(key) instanceof JSONObject) {
				return intFromJSON(o.getJSONObject(key), div, min, max);
			}
		}
		
		return new NoBonus<Integer>(def);
	}
	
	public static BonusableValue<Integer> intFromJSON(JSONObject o, int div, int min, int max) {
		if (o.has("deltas") || o.has("multipliers") || o.has("dividers")) {
			JSONObject deltas = o.has("deltas") ? o.getJSONObject("deltas") : new JSONObject();
			JSONObject multipliers = o.has("multipliers") ? o.getJSONObject("multipliers") : new JSONObject();
			JSONObject dividers = o.has("dividers") ? o.getJSONObject("dividers") : new JSONObject();
			ArrayList<Bonus> bonuses = new ArrayList<Bonus>();
			ArrayList<Integer> bDeltas = new ArrayList<Integer>();
			ArrayList<Double> bMults = new ArrayList<Double>();
			ArrayList<Double> bDivs = new ArrayList<Double>();
			ArrayList<String> bDescs = new ArrayList<String>();
			for (Bonus b : Bonus.values()) {
				if (deltas.has(b.name()) || multipliers.has(b.name()) || dividers.has(b.name())) {
					bonuses.add(b);
					bDeltas.add(deltas.optInt(b.name(), 0) / div);
					bMults.add(multipliers.optDouble(b.name(), 1));
					bDivs.add(dividers.optDouble(b.name(), 1));
					bDescs.add(deltas.optString(b.name() + "_desc", null));
				}
			}
			Bonus[] bonusA = new Bonus[bonuses.size()];
			int[] bonusDeltas = new int[bonuses.size()];
			double[] bonusMults = new double[bonuses.size()];
			double[] bonusDivs = new double[bonuses.size()];
			String[] descs = new String[bonuses.size()];

			for (int i = 0; i < bonusA.length; i++) {
				bonusA[i] = bonuses.get(i);
				bonusDeltas[i] = bDeltas.get(i);
				bonusMults[i] = bMults.get(i);
				bonusDivs[i] = bDivs.get(i);
				descs[i] = bDescs.get(i);
			}
			return new IntArithmeticBonus(o.getInt("base") / div, bonusA, bonusDeltas, bonusMults, bonusDivs, descs, min, max);
		} else if (o.has("cases")) {
			int base = Math.max(min, Math.min(max, o.getInt("base") / div));
			JSONArray cases = o.getJSONArray("cases");
			ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
			ArrayList<Integer> objects = new ArrayList<Integer>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (int i = 0; i < cases.length(); i++) {
				JSONObject caze = cases.getJSONObject(i);
				BonusSet bs = set(caze.getJSONArray("bonuses"));
				sets.add(bs);
				objects.add(Math.max(min, Math.min(max, caze.getInt("value") / div)));
				descriptions.add(caze.optString("description", null));
			}
			return new SetBonus<Integer>(base, sets, objects, descriptions);
		}
		for (Bonus b : Bonus.values()) {
			if (o.has(b.name())) {
				return new SingleBonus<Integer>(
						StrictMath.min(max, StrictMath.max(min, o.getInt("base") / div)),
						b,
						StrictMath.min(max, StrictMath.max(min, o.getInt(b.name()) / div)),
						o.optString(b.name() + "_desc", null));
			}
		}
		return new NoBonus<Integer>(StrictMath.min(max, StrictMath.max(min, o.getInt("base") / div)));
	}
	
	public static BonusableValue<Double> doubleFromJSON(JSONObject o, String key, double def) {
		if (o.has(key)) {
			if (o.get(key) instanceof Number) {
				return new NoBonus<Double>(o.getDouble(key));
			}
			if (o.get(key) instanceof JSONObject) {
				return doubleFromJSON(o.getJSONObject(key));
			}
		}
		return new NoBonus<Double>(def);
	}
	
	public static BonusableValue<Double> doubleFromJSON(JSONObject o) {
		if (o.has("deltas") || o.has("multipliers") || o.has("dividers")) {
			JSONObject deltas = o.has("deltas") ? o.getJSONObject("deltas") : new JSONObject();
			JSONObject multipliers = o.has("multipliers") ? o.getJSONObject("multipliers") : new JSONObject();
			JSONObject dividers = o.has("dividers") ? o.getJSONObject("dividers") : new JSONObject();
			ArrayList<Bonus> bonuses = new ArrayList<Bonus>();
			ArrayList<Double> bDeltas = new ArrayList<Double>();
			ArrayList<Double> bMults = new ArrayList<Double>();
			ArrayList<Double> bDivs = new ArrayList<Double>();
			ArrayList<String> bDescs = new ArrayList<String>();
			for (Bonus b : Bonus.values()) {
				if (deltas.has(b.name()) || multipliers.has(b.name()) || dividers.has(b.name())) {
					bonuses.add(b);
					bDeltas.add(deltas.optDouble(b.name(), 0));
					bMults.add(multipliers.optDouble(b.name(), 1));
					bDivs.add(dividers.optDouble(b.name(), 1));
					bDescs.add(deltas.optString(b.name() + "_desc", null));
				}
			}
			Bonus[] bonusA = new Bonus[bonuses.size()];
			double[] bonusDeltas = new double[bonuses.size()];
			double[] bonusMults = new double[bonuses.size()];
			double[] bonusDivs = new double[bonuses.size()];
			String[] descs = new String[bonuses.size()];
			for (int i = 0; i < bonusA.length; i++) {
				bonusA[i] = bonuses.get(i);
				bonusDeltas[i] = bDeltas.get(i);
				bonusMults[i] = bMults.get(i);
				bonusDivs[i] = bDivs.get(i);
				descs[i] = bDescs.get(i);
			}
			return new DoubleArithmeticBonus(o.getDouble("base"), bonusA, bonusDeltas, bonusMults, bonusDivs, descs);
		} else if (o.has("cases")) {
			double base = o.getDouble("base");
			JSONArray cases = o.getJSONArray("cases");
			ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
			ArrayList<Double> objects = new ArrayList<Double>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (int i = 0; i < cases.length(); i++) {
				JSONObject caze = cases.getJSONObject(i);
				BonusSet bs = set(caze.getJSONArray("bonuses"));
				sets.add(bs);
				objects.add(caze.getDouble("value"));
				descriptions.add(caze.optString("description", null));
			}
			return new SetBonus<Double>(base, sets, objects, descriptions);
		}
		for (Bonus b : Bonus.values()) {
			if (o.has(b.name())) {
				return new SingleBonus<Double>(o.getDouble("base"), b, o.getDouble(b.name()), o.optString(b.name() + "_desc", null));
			}
		}
		return new NoBonus<Double>(o.getDouble("base"));
	}
	
	public static BonusableValue<String> stringFromJSON(JSONObject o, String key, String def) {
		if (o.has(key)) {
			if (o.get(key) instanceof String) {
				return new NoBonus<String>(o.getString(key));
			}
			if (o.get(key) instanceof JSONObject) {
				return stringFromJSON(o.getJSONObject(key));
			}
		}
		return new NoBonus<String>(def);
	}
	
	public static BonusableValue<String> stringFromJSON(JSONObject o) {
		if (o.has("cases")) {
			String base = o.getString("base");
			JSONArray cases = o.getJSONArray("cases");
			ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
			ArrayList<String> objects = new ArrayList<String>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (int i = 0; i < cases.length(); i++) {
				JSONObject caze = cases.getJSONObject(i);
				BonusSet bs = set(caze.getJSONArray("bonuses"));
				sets.add(bs);
				objects.add(caze.getString("value"));
				descriptions.add(caze.optString("description", null));
			}
			return new SetBonus<String>(base, sets, objects, descriptions);
		}
		for (Bonus b : Bonus.values()) {
			if (o.has(b.name())) {
				return new SingleBonus<String>(o.getString("base"), b, o.getString(b.name()), o.optString("desc", null));
			}
		}
		return new NoBonus<String>(o.getString("base"));
	}
	
	public static BonusableValue<Boolean> booleanFromJSON(JSONObject o, String key, boolean def) {
		if (o.has(key)) {
			if (o.get(key) instanceof Boolean) {
				return new NoBonus<Boolean>(o.getBoolean(key));
			}
			if (o.get(key) instanceof JSONObject) {
				return booleanFromJSON(o.getJSONObject(key));
			}
		}
		return new NoBonus<Boolean>(def);		
	}
	
	public static BonusableValue<Boolean> booleanFromJSON(JSONObject o) {
		if (o.has("cases")) {
			boolean base = o.getBoolean("base");
			JSONArray cases = o.getJSONArray("cases");
			ArrayList<BonusSet> sets = new ArrayList<BonusSet>();
			ArrayList<Boolean> objects = new ArrayList<Boolean>();
			ArrayList<String> descriptions = new ArrayList<String>();
			for (int i = 0; i < cases.length(); i++) {
				JSONObject caze = cases.getJSONObject(i);
				BonusSet bs = set(caze.getJSONArray("bonuses"));
				sets.add(bs);
				objects.add(caze.getBoolean("value"));
				descriptions.add(caze.optString("description", null));
			}
			return new SetBonus<Boolean>(base, sets, objects, descriptions);
		}
		for (Bonus b : Bonus.values()) {
			if (o.has(b.name())) {
				return new SingleBonus<Boolean>(o.getBoolean("base"), b, o.getBoolean(b.name()), o.optString(b.name() + "_desc", null));
			}
		}
		return new NoBonus<Boolean>(o.getBoolean("base"));
	}

	public static class ClrFromJSON implements FromJSON<Clr> {
		@Override
		public Clr construct(JSONObject co, BonusSet b) {
			return new Clr(co.getInt("r"), co.getInt("g"), co.getInt("b"), co.optInt("a", 255));
		}
	}
}
