package com.zarkonnen.airships;

import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Input;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class Keys {
	public static void loadKeyInfo() {
		try {
			HashMap<String, KeyGroup> lookup = new HashMap<String, KeyGroup>();
			File f = new File(new File(AGame.getStaticGameDirectory(), "data"), "keys.json");
			JSONObject o = new JSONObject(FileUtils.readFileToString(f, "UTF-8"));
			JSONArray groupsA = o.getJSONArray("groups");
			for (int i = 0; i < groupsA.length(); i++) {
				JSONObject groupO = groupsA.getJSONObject(i);
				KeyGroup group = new KeyGroup(groupO.getString("name"));
				lookup.put(group.name, group);
				groups.add(group);
				JSONArray keysA = groupO.getJSONArray("keys");
				for (int j = 0; j < keysA.length(); j++) {
					String ks = keysA.getJSONArray(j).getString(1);
					KeyInfo key = new KeyInfo(keysA.getJSONArray(j).getString(0), getKey(ks), getCmd(ks), getCtrl(ks), getAlt(ks), getShift(ks), keysA.getJSONArray(j).getString(2), group.name);
					group.items.add(key);
					keyLookup.put(key.id, key.value);
				}
				
				for (KeyInfo ki : group.items) {
					ArrayList<KeyInfo> cs = new ArrayList<KeyInfo>();
					cs.addAll(group.items);
					cs.remove(ki);
					connecteds.put(ki, cs);
				}
			}
						
			JSONArray connectedA = o.getJSONArray("connected");
			for (int i = 0; i < connectedA.length(); i++) {
				JSONArray csA = connectedA.getJSONArray(i);
				KeyGroup kg = lookup.get(csA.getString(0));
				for (KeyInfo ki : kg.items) {
					for (int j = 1; j < csA.length(); j++) {
						connecteds.get(ki).addAll(lookup.get(csA.getString(j)).items);
					}
				}
				for (int j = 1; j < csA.length(); j++) {
					KeyGroup kg2 = lookup.get(csA.getString(j));
					for (KeyInfo ki : kg2.items) {
						connecteds.get(ki).addAll(kg.items);
					}
				}
			}
			
			loadKeyPrefs();
		} catch (Exception e) {
			AirshipGame.instance.reportError("Unable to load keys.json.", e, null, true, false);
		}
	}
	
	private static boolean getCmd(String s) {
		return s.contains("cmd-");
	}
	
	private static boolean getCtrl(String s) {
		return s.contains("ctrl-");
	}
	
	private static boolean getAlt(String s) {
		return s.contains("alt-");
	}
	
	private static boolean getShift(String s) {
		return s.contains("shift-");
	}
	
	private static String getKey(String s) {
		return s.replace("shift-", "").replace("ctrl-", "").replace("cmd-", "").replace("alt-", "");
	}
	
	private static String blob(String key, boolean cmd, boolean ctrl, boolean alt, boolean shift) {
		if (key == null) { return null; }
		StringBuilder sb = new StringBuilder();
		if (cmd) {
			sb.append("cmd-");
		}
		if (ctrl) {
			sb.append("ctrl-");
		}
		if (alt) {
			sb.append("alt-");
		}
		if (shift) {
			sb.append("shift-");
		}
		sb.append(key);
		return sb.toString();
	}
	
	public static void loadKeyPrefs() {
		for (KeyGroup g : groups) {
			for (KeyInfo ki : g.items) {
				ki.value = getKey(AirshipGame.PREFS.get("k_" + ki.id, ki.original));
				if (AirshipGame.PREFS.hasKey("k_" + ki.id)) {
					ki.cmd = getCmd(AirshipGame.PREFS.get("k_" + ki.id, ki.original));
					ki.ctrl = getCtrl(AirshipGame.PREFS.get("k_" + ki.id, ki.original));
					ki.alt = getAlt(AirshipGame.PREFS.get("k_" + ki.id, ki.original));
					ki.shift = getShift(AirshipGame.PREFS.get("k_" + ki.id, ki.original));
				} else {
					ki.cmd = ki.originalCmd;
					ki.ctrl = ki.originalCtrl;
					ki.alt = ki.originalAlt;
					ki.shift = ki.originalShift;
				}
				if (ki.value.equals("NONE")) {
					ki.value = null;
					ki.cmd = false;
					ki.ctrl = false;
					ki.alt = false;
					ki.shift = false;
				}
				keyLookup.put(ki.id, ki.value);
				cmdKeyLookup.put(ki.id, ki.cmd);
				ctrlKeyLookup.put(ki.id, ki.ctrl);
				altKeyLookup.put(ki.id, ki.alt);
				shiftKeyLookup.put(ki.id, ki.shift);
			}
		}
	}
	
	public static void saveKeyPrefs() {
		for (KeyGroup g : groups) {
			for (KeyInfo ki : g.items) {
				AirshipGame.PREFS.put("k_" + ki.id, ki.value == null ? "NONE" : blob(ki.value, ki.cmd, ki.ctrl, ki.alt, ki.shift));
			}
		}
		AirshipGame.PREFS.save();
	}
	
	public static void reset() {
		for (KeyGroup g : groups) {
			for (KeyInfo ki : g.items) {
				ki.value = ki.original;
				ki.cmd = ki.originalCmd;
				ki.ctrl = ki.originalCtrl;
				ki.alt = ki.originalAlt;
				ki.shift = ki.originalShift;
				keyLookup.put(ki.id, ki.value);
				cmdKeyLookup.put(ki.id, ki.cmd);
				ctrlKeyLookup.put(ki.id, ki.ctrl);
				altKeyLookup.put(ki.id, ki.alt);
				shiftKeyLookup.put(ki.id, ki.shift);
			}
		}
	}
	
	public static class KeyInfo {
		public final String id;
		public final String original;
		public final boolean originalCmd, originalCtrl, originalAlt, originalShift;
		public String value;
		public boolean cmd, ctrl, alt, shift;
		public final String name;
		public final String groupName;

		public KeyInfo(String id, String original, boolean originalCmd, boolean originalCtrl, boolean originalAlt, boolean originalShift, String name, String groupName) {
			this.id = id;
			this.original = original;
			this.value = original;
			this.originalCmd = originalCmd;
			this.cmd = originalCmd;
			this.originalCtrl = originalCtrl;
			this.ctrl = originalCtrl;
			this.originalAlt = originalAlt;
			this.alt = originalAlt;
			this.originalShift = originalShift;
			this.shift = originalShift;
			this.name = name;
			this.groupName = groupName;
		}
		
		public KeyInfo setCollidesWith(String newValue, boolean newCmd, boolean newCtrl, boolean newAlt, boolean newShift) {
			if (!connecteds.containsKey(this)) { return null; }
			for (KeyInfo ki : connecteds.get(this)) {
				if (ki.value != null && ki.value.equals(newValue) && ki.cmd == newCmd && ki.ctrl == newCtrl && ki.alt == newAlt && ki.shift == newShift) {
					return ki;
				}
			}
			return null;
		}
		
		public boolean set(String newValue, boolean newCmd, boolean newCtrl, boolean newAlt, boolean newShift) {
			if (setCollidesWith(newValue, newCmd, newCtrl, newAlt, newShift) != null) { return false; }
			value = newValue;
			cmd = newCmd;
			ctrl = newCtrl;
			alt = newAlt;
			shift = newShift;
			keyLookup.put(id, value);
			cmdKeyLookup.put(id, newCmd);
			ctrlKeyLookup.put(id, newCtrl);
			altKeyLookup.put(id, newAlt);
			shiftKeyLookup.put(id, newShift);
			return true;
		}
		
		public String getText() {
			return Keys.formatText(value, cmd, ctrl, alt, shift);
		}
	}
	
	public static String formatText(String k, boolean cmd, boolean ctrl, boolean alt, boolean shift) {
		if (k == null) { return _t("no_key"); }
		StringBuilder sb = new StringBuilder();
		if (cmd) {
			sb.append("⌘");
		}
		if (ctrl) {
			sb.append("⎈");
		}
		if (alt) {
			sb.append("⎇");
		}
		if (shift) {
			sb.append("⇧");
		}
		if (k.equals("SLASH")) {
			sb.append("/");
		} else if (k.equals("COMMA")) {
			sb.append(",");
		} else if (k.equals("PERIOD")) {
			sb.append(".");
		} else {
			sb.append(k);
		}
		return sb.toString();
	}
	
	public static class KeyGroup {
		public final String name;
		public final ArrayList<KeyInfo> items = new ArrayList<KeyInfo>();
		
		public KeyGroup(String name) {
			this.name = name;
		}
	}
	
	private static HashMap<String, String> keyLookup = new HashMap<String, String>();
	private static HashMap<String, Boolean> cmdKeyLookup = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> ctrlKeyLookup = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> altKeyLookup = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> shiftKeyLookup = new HashMap<String, Boolean>();
	public static ArrayList<KeyGroup> groups = new ArrayList<KeyGroup>();
	public static HashMap<KeyInfo, ArrayList<KeyInfo>> connecteds = new HashMap<KeyInfo, ArrayList<KeyInfo>>();
	
	public static String getText(String id, String original, boolean originalShift) {
		String k = get(id, original);
		return k == null ? null : formatText(k, getCmd(id, false), getCtrl(id, false), getAlt(id, false), getShift(id, originalShift));
	}
	
	public static String get(String id, String original) {
		return keyLookup.containsKey(id) ? keyLookup.get(id) : original;
	}
	
	public static boolean getCmd(String id, boolean original) {
		return cmdKeyLookup.containsKey(id) ? cmdKeyLookup.get(id) : original;
	}
	
	public static boolean getCtrl(String id, boolean original) {
		return ctrlKeyLookup.containsKey(id) ? ctrlKeyLookup.get(id) : original;
	}
	
	public static boolean getAlt(String id, boolean original) {
		return altKeyLookup.containsKey(id) ? altKeyLookup.get(id) : original;
	}
	
	public static boolean getShift(String id, boolean original) {
		return shiftKeyLookup.containsKey(id) ? shiftKeyLookup.get(id) : original;
	}
	
	private static final class QKey {
		private final boolean cmd, ctrl, alt, shift;
		private final String key;

		public QKey(boolean cmd, boolean ctrl, boolean alt, boolean shift, String key) {
			this.cmd = cmd;
			this.ctrl = ctrl;
			this.alt = alt;
			this.shift = shift;
			this.key = key;
		}
		
		@Override
		public int hashCode() {
			return key.hashCode() * 16 + (cmd ? 1 : 0) + (ctrl ? 2 : 0) + (alt ? 4 : 0) + (shift ? 8 : 0);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof QKey)) { return false; }
			QKey pk2 = (QKey) o;
			return cmd == pk2.cmd && ctrl == pk2.ctrl && alt == pk2.alt && shift == pk2.shift && key.equals(pk2.key);
		}
	}
	
	private static final HashSet<QKey> queriedKeys = new HashSet<QKey>();
	private static Input queriedForInput;
	
	/**
	 * 每帧开始时调用，清空按键查询去重缓存。
	 * 去重本意是"同一帧内同一键只生效一次"（防止多个组件响应同一键），
	 * 但去重靠 input 对象引用变化来清空；全屏时 scaleFrom==null、input 是单例，
	 * 引用不变导致 queriedKeys 跨帧不清空，按键（如 ESCAPE）只在首次按下生效。
	 * 显式每帧重置即可修复。
	 */
	public static void resetQueriedKeys() {
		queriedKeys.clear();
		queriedForInput = null;
	}
	
	public static boolean check(Input in, String key) {
		if (key == null) { return false; }
		if (queriedForInput != in) {
			queriedKeys.clear();
			queriedForInput = in;
		}
		boolean cmd = false;
		boolean ctrl = false;
		boolean alt = false;
		boolean shift = false;
		QKey qk = new QKey(cmd, ctrl, alt, shift, key);
		if (queriedKeys.contains(qk)) {
			return false;
		}
		queriedKeys.add(qk);
		return
				(in.keyDown("LWIN") || in.keyDown("RWIN")) == cmd &&
				(in.keyDown("LCONTROL") || in.keyDown("RCONTROL")) == ctrl &&
				(in.keyDown("LALT") || in.keyDown("RALT") || in.keyDown("LMENU") || in.keyDown("RMENU")) == alt &&
				(in.keyDown("LSHIFT") || in.keyDown("RSHIFT")) == shift &&
				in.keyPressed(key);
	}
	
	public static boolean check(Input in, String id, String original, boolean originalShift) {
		if (queriedForInput != in) {
			queriedKeys.clear();
			queriedForInput = in;
		}
		boolean cmd = getCmd(id, false);
		boolean ctrl = getCtrl(id, false);
		boolean alt = getAlt(id, false);
		boolean shift = getShift(id, originalShift);
		String key = get(id, original);
		if (key == null) { return false; }
		QKey qk = new QKey(cmd, ctrl, alt, shift, key);
		if (queriedKeys.contains(qk)) {
			return false;
		}
		queriedKeys.add(qk);
		return
				(in.keyDown("LWIN") || in.keyDown("RWIN")) == cmd &&
				(in.keyDown("LCONTROL") || in.keyDown("RCONTROL")) == ctrl &&
				(in.keyDown("LALT") || in.keyDown("RALT") || in.keyDown("LMENU") || in.keyDown("RMENU")) == alt &&
				(in.keyDown("LSHIFT") || in.keyDown("RSHIFT")) == shift &&
				in.keyPressed(key);
	}
	
	public static boolean checkDown(Input in, String id, String original, boolean originalShift) {
		String key = get(id, original);
		if (key == null) { return false; }
		return 
				(in.keyDown("LWIN") || in.keyDown("RWIN")) == getCmd(id, false) &&
				(in.keyDown("LCONTROL") || in.keyDown("RCONTROL")) == getCtrl(id, false) &&
				(in.keyDown("LALT") || in.keyDown("RALT") || in.keyDown("LMENU") || in.keyDown("RMENU")) == getAlt(id, false) &&
				(in.keyDown("LSHIFT") || in.keyDown("RSHIFT")) == getShift(id, originalShift) &&
				in.keyDown(key);
	}
}
