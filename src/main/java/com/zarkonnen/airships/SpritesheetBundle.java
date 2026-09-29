package com.zarkonnen.airships;

import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONObject;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.Texture;

public strictfp class SpritesheetBundle extends Loadable {
	public final String bump;
	public final String fragments;
	private final SpritesheetBundle damagedVersion;
	private final SpritesheetBundle fragmentsSheet;
	private boolean isDamaged;
	private boolean isFragments;
	private boolean closed;
	public int size;
	
	// Cache images versions by postfix.
	private final HashMap<String, Image> postfixToSheet = new HashMap<String, Image>();
	private final HashMap<String, Texture> postfixToTex = new HashMap<String, Texture>();
	public Texture bumpTex;
	
	private transient HashSet<String> complainedPostfixes = new HashSet<String>();
	
	public boolean isClosed() { return closed; }
	
	public boolean isPostfixLoaded(String postfix) {
		return bump == null || postfixToSheet.containsKey(postfix);
	}
	
	public Image getSheet(String postfix) {
		if (closed) {
			throw new RuntimeException("Attempting to access closed sheet.");
		}
		if (bump == null || isFragments) { return postfixToSheet.get(""); }
		if (!postfixToSheet.containsKey(postfix)) {
			return postfixToSheet.get("");
		}
		return postfixToSheet.get(postfix);
	}
	
	public Texture getTex(String postfix) {
		if (closed) {
			throw new RuntimeException("Attempting to access closed sheet.");
		}
		if (bump == null || isFragments) { return postfixToTex.get(""); }
		if (!postfixToTex.containsKey(postfix)) {
			if (!complainedPostfixes.contains(postfix)) {
				System.err.println("Cannot find postfix " + postfix + " for " + name + ".");
				complainedPostfixes.add(postfix);
			}
			return postfixToTex.get("");
		}
		return postfixToTex.get(postfix);
	}
	
	public SpritesheetBundle(JSONObject o) {
		super(o.getString("name"));
		bump = o.optString("bump", null);
		fragments = o.optString("fragments", null);
		if (bump != null && fragments != null) {
			damagedVersion = new SpritesheetBundle(name + "DAMAGED", bump + "DAMAGED");
			fragmentsSheet = new SpritesheetBundle(name + "FRAGMENTS", bump + "FRAGMENTS");
		} else {
			damagedVersion = null;
			fragmentsSheet = null;
		}
	}
	
	public void initBumps() {
		if (bump != null && bumpTex == null) {
			bumpTex = SpriteUtils.loadTexture(bump);
			if (bumpTex != null) {
				bumpTex.setTextureFilter(Image.FILTER_NEAREST);
			}
		}
		if (damagedVersion != null) {
			damagedVersion.initBumps();
		}
		if (fragmentsSheet != null) {
			fragmentsSheet.initBumps();
		}
	}
	
	private SpritesheetBundle(String name, String bump) {
		super(name);
		this.bump = bump;
		fragments = null;
		damagedVersion = null;
		fragmentsSheet = null;
	}
		
	public void loadPostfix(String postfix) {
		if ((bump != null || postfix.equals("")) && !postfixToSheet.containsKey(postfix)) {
			Image sh = SpriteUtils.loadImage(name + postfix);
			if (sh == null) {
				if (!complainedPostfixes.contains(postfix)) {
					System.err.println("No spritesheet \"" + name + postfix + "\" available.");
					complainedPostfixes.add(postfix);
				}
			} else {
				sh.setFilter(Image.FILTER_NEAREST);
				postfixToSheet.put(postfix, sh);
				postfixToTex.put(postfix, sh.getTexture());
				if (size != 0 && size != sh.getWidth()) {
					AirshipGame.instance.reportError("Inconsistent spritesheet size: " + postfix + " has " + sh.getWidth() + ", previously " + size + " " + SpriteUtils.loadImageReport(name), null, null, false, true);
				}
				size = sh.getWidth();
			}
		}
		if (damagedVersion != null && !postfix.equals("BLUEPRINT")) {
			damagedVersion.loadPostfix(postfix);
		}
		if (fragmentsSheet != null && postfix.equals("")) {
			fragmentsSheet.loadPostfix(postfix);
		}
		if (!postfix.equals("")) {
			loadPostfix("");
		}
	}
	
	public static SpritesheetBundle ofName(String name) {
		return ofName(SpritesheetBundle.class, name);
	}
	
	public SpritesheetBundle getDamagedVersion() {
		if (damagedVersion != null) {
			damagedVersion.isDamaged = true;
		}
		return damagedVersion;
	}
	
	public SpritesheetBundle getFragmentsSheet() {
		if (fragmentsSheet != null) {
			fragmentsSheet.isFragments = true;
		}
		return fragmentsSheet;
	}
	
	@Override
	public void close() {
		if (closed) { return; }
		for (Texture t : postfixToTex.values()) {
			t.release();
		}
		for (Image t : postfixToSheet.values()) {
			try {
				t.destroy();
			} catch (Exception e) {}
		}
		postfixToTex.clear();
		postfixToSheet.clear();
		if (bumpTex != null) {
			bumpTex.release();
			bumpTex = null;
		}
		if (damagedVersion != null) {
			damagedVersion.close();
		}
		if (fragmentsSheet != null) {
			fragmentsSheet.close();
		}
		closed = true;
	}
}
