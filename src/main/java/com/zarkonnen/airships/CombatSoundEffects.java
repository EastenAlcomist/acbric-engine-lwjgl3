package com.zarkonnen.airships;

import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.Loop;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.newdawn.slick.openal.SoundStore2;

public strictfp class CombatSoundEffects implements UniScreen.InfoPanel, Comparator<SoundEffect.Sound> {
	public ArrayList<SoundEffect.Sound> sounds = new ArrayList<SoundEffect.Sound>();
	public LinkedList<String> soundLog = new LinkedList<String>();
	public HashMap<String, LoopEntry> loops = new HashMap<String, LoopEntry>();
	private final ArrayList<SoundEffect.Sound> loopSnd = new ArrayList<SoundEffect.Sound>();
	private final HashSet<String> usedLoopKeys = new HashSet<String>();
	public HashMap<String, ConcreteLoop> concreteLoops = new HashMap<String, ConcreteLoop>();
	public Loop altitudeLoop;
	private boolean overlayActive = false;
	
	public static double PAN_STRENGTH = 1.0;
	public static double LISTENER_Z_DIST_RELATIVE_TO_SCREEN_WIDTH = 0.5;
	public static double LOOP_SUM_EXPONENT = 0.5;
	public static double ZOOM_VOLUME_EXPONENT = 2.0;
	
	public static class ConcreteLoop {
		public Loop loop;
		public double angleAccum;
		public double xyDistAccum;
		public double pitchAccum;
		public double volumeAccum;
		public double maxVolume;
		public int nAccum;
		public double x, y, pitch, volume;
	}
	
	public static class LoopEntry {
		public ConcreteLoop[] loops;
		public double[] lastXs;
		public double[] lastYs;
		public double[] lastVolumes;
		public double[] lastPitches;
		public String[] soundNames;
		public double[] lastDists;
		public double[] lastXSpeeds;
		public double[] lastYSpeeds;
		public boolean[] lastOnViewingSide;
		public double[] lastRelativeStrength;
		public double fadeAmount = 1.0;
		public int fadeInMs = 200;
		public int fadeOutMs = 200;
		public LoopEntry(int size) {
			loops = new ConcreteLoop[size];
			lastXs = new double[size];
			lastYs = new double[size];
			lastVolumes = new double[size];
			lastPitches = new double[size];
			soundNames = new String[size];
			lastDists = new double[size];
			lastXSpeeds = new double[size];
			lastYSpeeds = new double[size];
			lastOnViewingSide = new boolean[size];
			lastRelativeStrength = new double[size];
		}
		public double volume() {
			return 1.0 - fadeAmount;
		}
	}
	
	private static CombatSoundEffects instance;
	
	public static void alwaysTick(AirshipGame g, Input in) {
		AirshipGame.getMyInput(in).setSoundZ((float) LISTENER_Z_DIST_RELATIVE_TO_SCREEN_WIDTH);
		if (instance == null) { return; }
		if (!(g.s instanceof UniScreen) || ((UniScreen) g.s).overlay(CombatSoundEffects.class) != instance) {
			//System.out.println("CSE cleanup");
			for (ConcreteLoop l : instance.concreteLoops.values()) {
				if (l.loop != null) { l.loop.stop(); l.loop = null; }
			}
			instance.concreteLoops.clear();
			instance.loops.clear();
			if (instance.altitudeLoop != null) {
				instance.altitudeLoop.stop();
				instance.altitudeLoop = null;
			}
			instance = null;
		}
	}
	
	@Override
	public void draw(MyDraw d, Pt cursor, ScreenMode sm, Hooks hs, UniScreen us) {
		if (!overlayActive) { return; }
		int y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		int x = MyDraw.SIDE_CLEARANCE;
		String header = rPad("Name", 20) + rPad("Pitch", 6) + rPad("Vol", 6)
				+ rPad("Dist", 6) + rPad("dx", 6) + rPad("dy", 6) + rPad("MySide", 8) + rPad("Str", 6);
		int w = (int) d.textSize(header, AGame.DEBUG_FOUNT).x + MyDraw.UI_SPACING * 2;
		int h = MyDraw.UI_SPACING * 2 + (soundLog.size() + 3) * AGame.DEBUG_FOUNT.height;
		d.rect(new Clr(0, 0, 0, 128), x, y, w, h);
		x += MyDraw.UI_SPACING;
		y += MyDraw.UI_SPACING;
		d.text(SoundStore2.get().getUsedSources() + " / " + SoundStore2.get().getSourceCount() + " sources used", AGame.DEBUG_FOUNT, x, y);
		y += AGame.DEBUG_FOUNT.height;
		d.text(header, AGame.DEBUG_FOUNT, x, y);
		y += AGame.DEBUG_FOUNT.height * 2;
		for (String s : soundLog) {
			d.text(s, AGame.DEBUG_FOUNT, x, y);
			y += AGame.DEBUG_FOUNT.height;
		}
		
		x += w + MyDraw.UI_SPACING;
		y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		ArrayList<String> keys = new ArrayList<String>(loops.keySet());
		Collections.sort(keys);
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("Merged Loops");
		lines.add(rPad("Name", 20) + rPad("x", 6) + rPad("y", 6) + rPad("Pitch", 6) + rPad("Volume", 6));
		for (Map.Entry<String, ConcreteLoop> e : concreteLoops.entrySet()) {
			lines.add(rPad(e.getKey(), 20) + rPad(e.getValue().x, 6) + rPad(e.getValue().y, 6) + rPad(e.getValue().pitch, 6) + rPad(e.getValue().volume, 6));
		}
		lines.add("");
		lines.add("Individual Loops");
		lines.add(header);
		for (String k : keys) {
			LoopEntry le = loops.get(k);
			lines.add(rPad(k, 20) + lPad((int) (le.volume() * 100) + "%", 5));
			for (int i = 0; i < le.loops.length; i++) {
				if (le.loops[i] != null) {
					lines.add(
							"  " +
							rPad(le.soundNames[i], 18) +
							rPad(le.lastPitches[i], 6) +
							rPad(le.lastVolumes[i], 6) +
							rPad((int) le.lastDists[i], 6) +
							rPad(le.lastXSpeeds[i], 6) +
							rPad(le.lastYSpeeds[i], 6) +
							rPad("" + le.lastOnViewingSide[i], 8) +
							rPad(le.lastRelativeStrength[i], 6));
				}
			}
		}		
		h = MyDraw.UI_SPACING * 2 + lines.size() * AGame.DEBUG_FOUNT.height;
		d.rect(new Clr(0, 0, 0, 128), x, y, w, h);
		x += MyDraw.UI_SPACING;
		y += MyDraw.UI_SPACING;
		for (String s : lines) {
			d.text(s, AGame.DEBUG_FOUNT, x, y);
			y += AGame.DEBUG_FOUNT.height;
		}
	}
	
	@Override
	public boolean doScroll(UniScreen us, int scrollAmt, Pt cursor, ScreenMode sm) { return false; }
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		boolean hasDC = DirectControlPanel.getShip(us) != null;
		if (in.keyPressed("F10")) {
			overlayActive = !overlayActive;
		}
		alwaysTick(us.g, in);
		instance = this;
		usedLoopKeys.clear();
		if (us.combat != null) {
			double relativeStrength = 1;
			if (us.mySide != null) {
				relativeStrength = (us.mySide.getCachedDanger() + 1) * 1.0 / (us.combat.otherSide(us.mySide).getCachedDanger() + 1);
			}
			ScreenMode sm = in.mode();
			Combat combat = us.combat;
			if (us.mySide != null) {
				int al = us.mySide.ships.size();
				for (int ai = 0; ai < al; ai++) {
					Airship as = us.mySide.ships.get(ai);
					if (as.readyForCommand() && !as.wasReadyForCommand && as.getAI() == null) {
						combat.play(MiscCombatSound.BELL, (as.getIntX() + as.getBBWidth() / 2), (as.getIntY() + as.getBBHeight() / 2), as.getxSpeed(), as.getySpeed(), true);
						as.wasReadyForCommand = true;
					}
				}
			}
			
			int csl = combat.sounds.size();
			double listenerX = us.screenToWorldX(sm.width / 2);
			double listenerY = us.screenToWorldY(sm.height / 2);
			double listenerZ = sm.width / us.zoom * LISTENER_Z_DIST_RELATIVE_TO_SCREEN_WIDTH;
			for (int csi = 0; csi < csl; csi++) {
				CombatSound cs = combat.sounds.get(csi);
				double volume = StrictMath.max(0.5, StrictMath.pow(us.zoom, ZOOM_VOLUME_EXPONENT)) * cs.volume;
				if (volume < 0.01) { continue; }
				double sx = (((cs.x + us.adjScrollX) * us.zoom) / sm.width - 0.5) * PAN_STRENGTH;
				double sy = (((cs.y + us.adjScrollY) * us.zoom) / sm.height - 0.5) * PAN_STRENGTH;
				double dist = StrictMath.sqrt(
						(cs.x - listenerX) * (cs.x - listenerX) +
						(cs.y - listenerY) * (cs.y - listenerY) +
						listenerZ * listenerZ
				);
				
				if (cs.loopKey == null) {
					cs.effect.emit(sounds, sx, sy, volume, dist, cs.xSpeed, cs.ySpeed, cs.own, cs.own ? relativeStrength : (1 / relativeStrength));
				} else {
					if (loops.containsKey(cs.loopKey)) {
						LoopEntry le = loops.get(cs.loopKey);
						le.fadeInMs = cs.effect.loopFadeInMs;
						le.fadeOutMs = cs.effect.loopFadeOutMs;
						for (int i = 0; i < cs.effect.layers.size(); i++) {
							loopSnd.clear();
							cs.effect.layers.get(i).emit(loopSnd, sx, sy, volume, dist, cs.xSpeed, cs.ySpeed, cs.own, cs.own ? relativeStrength : (1 / relativeStrength));
							if (loopSnd.isEmpty()) {
								if (le.loops[i] != null) {
									le.loops[i] = null;
									le.lastVolumes[i] = 0;
									le.lastPitches[i] = 0;
									le.soundNames[i] = null;
								}
							} else {
								SoundEffect.Sound s = loopSnd.get(0);
								if (i >= le.loops.length) {
									continue;
								}
								if (le.loops[i] == null) {
									if (concreteLoops.containsKey(s.name)) {
										le.loops[i] = concreteLoops.get(s.name);
									} else {
										ConcreteLoop cl = new ConcreteLoop();
										concreteLoops.put(s.name, cl);
										le.loops[i] = cl;
									}
									le.soundNames[i] = s.name;
								}
								// The sound name stays the same, even if emit() produced a different one.
								le.lastXs[i] = s.x;
								le.lastYs[i] = s.y;
								le.lastVolumes[i] = s.volume;
								le.lastPitches[i] = s.pitch;
								le.lastDists[i] = s.dist;
								le.lastXSpeeds[i] = s.xSpeed;
								le.lastYSpeeds[i] = s.ySpeed;
								le.lastOnViewingSide[i] = s.onViewingSide;
								le.lastRelativeStrength[i] = s.relativeStrength;
							}
						}
					} else {
						//System.out.println("adding loop " + cs.loopKey);
						//soundLog.add(0, "Started " + cs.loopKey);
						LoopEntry le = new LoopEntry(cs.effect.layers.size());
						le.fadeInMs = cs.effect.loopFadeInMs;
						le.fadeOutMs = cs.effect.loopFadeOutMs;
						for (int i = 0; i < cs.effect.layers.size(); i++) {
							loopSnd.clear();
							cs.effect.layers.get(i).emit(loopSnd, sx, sy, volume, dist, cs.xSpeed, cs.ySpeed, cs.own, cs.own ? relativeStrength : (1 / relativeStrength));
							if (!loopSnd.isEmpty()) {
								SoundEffect.Sound s = loopSnd.get(0);
								if (concreteLoops.containsKey(s.name)) {
									le.loops[i] = concreteLoops.get(s.name);
								} else {
									ConcreteLoop cl = new ConcreteLoop();
									concreteLoops.put(s.name, cl);
									le.loops[i] = cl;
								}
								le.lastXs[i] = s.x;
								le.lastYs[i] = s.y;
								le.lastVolumes[i] = s.volume;
								le.lastPitches[i] = s.pitch;
								le.soundNames[i] = s.name;
								le.lastDists[i] = s.dist;
								le.lastXSpeeds[i] = s.xSpeed;
								le.lastYSpeeds[i] = s.ySpeed;
								le.lastOnViewingSide[i] = s.onViewingSide;
								le.lastRelativeStrength[i] = s.relativeStrength;
							}
						}
						loops.put(cs.loopKey, le);
					}
					usedLoopKeys.add(cs.loopKey);
				}
			}
			
			combat.sounds.clear();
			
			if (!(us.intent instanceof CombatIntent) || us.combat.speed.getMult() > 0) {
				for (Iterator<Map.Entry<String, LoopEntry>> it = loops.entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, LoopEntry> e = it.next();
					LoopEntry le = e.getValue();
					if (usedLoopKeys.contains(e.getKey())) {
						le.fadeAmount = StrictMath.max(0, le.fadeAmount - ms * 1.0 / le.fadeInMs);
					} else {
						le.fadeAmount += ms * 1.0 / le.fadeOutMs;
						if (le.fadeAmount >= 1.0) {
							it.remove();
						}
					}
				}
			} else if (us.combat.speed.getMult() == 0) {
				for (Iterator<Map.Entry<String, LoopEntry>> it = loops.entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, LoopEntry> e = it.next();
					LoopEntry le = e.getValue();
					le.fadeAmount += ms * 1.0 / le.fadeOutMs;
					if (le.fadeAmount >= 1.0) {
						it.remove();
					}
				}
			}
			
			// Adjust concrete loops
			for (ConcreteLoop cl : concreteLoops.values()) {
				cl.angleAccum = 0;
				cl.xyDistAccum = 0;
				cl.maxVolume = 0;
				cl.nAccum = 0;
				cl.pitchAccum = 0;
				cl.volumeAccum = 0;
			}
			for (LoopEntry le : loops.values()) {
				for (int i = 0; i < le.loops.length; i++) {
					ConcreteLoop cl = le.loops[i];
					if (cl != null) {
						double angle = StrictMath.atan2(le.lastYs[0], le.lastXs[0]);
						double xyDist = StrictMath.sqrt(
							(le.lastXs[i]) * (le.lastXs[i]) +
							(le.lastYs[i]) * (le.lastYs[i]));
						double volume = le.lastVolumes[i] * le.volume();
						cl.angleAccum += angle * volume;
						cl.xyDistAccum += xyDist * volume;
						cl.maxVolume = StrictMath.max(cl.maxVolume, volume);
						cl.nAccum++;
						cl.pitchAccum += le.lastPitches[i] * volume;
						cl.volumeAccum += volume;
					}
				}
			}
			for (Iterator<Map.Entry<String, ConcreteLoop>> it = concreteLoops.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, ConcreteLoop> e = it.next();
				ConcreteLoop cl = e.getValue();
				if (cl.nAccum == 0) {
					if (cl.loop != null) {
						cl.loop.stop();
					}
					it.remove();
				} else {
					double angle = cl.angleAccum / cl.volumeAccum;
					double xyDist = cl.xyDistAccum / cl.volumeAccum;
					double pitch = cl.pitchAccum / cl.volumeAccum;
					double volume = StrictMath.pow(cl.volumeAccum / cl.maxVolume, LOOP_SUM_EXPONENT) * cl.maxVolume;
					double x = StrictMath.cos(angle) * xyDist;
					double y = StrictMath.sin(angle) * xyDist;
					if (cl.loop == null) {
						cl.loop = in.loop(e.getKey(), pitch, volume * us.g.volume, x, y);
					} else {
						cl.loop.setLocation((float) x, (float) y);
						cl.loop.setPitch((float) pitch);
						cl.loop.setVolume((float) (volume * us.g.volume));
					}
					cl.x = x;
					cl.y = y;
					cl.pitch = pitch;
					cl.volume = volume;
				}
			}
			
			Collections.sort(sounds, this);
			
			for (int si = 0; si < sounds.size() && si < LaunchSettings.maxSoundsPerFrame; si++) {
				SoundEffect.Sound s = sounds.get(si);
				soundLog.add(0, rPad(s.name, 20) + rPad(s.pitch, 6) + rPad(s.volume, 6)
					+ rPad((int) s.dist, 6) + rPad(s.xSpeed, 6) + rPad(s.ySpeed, 6) + rPad("" + s.onViewingSide, 8) + rPad(s.relativeStrength, 6));
				in.play(s.name, s.pitch, s.volume * us.g.volume * (hasDC ? 0.67 : 1), s.x, s.y);
			}
			
			/*if (sounds.size() > LaunchSettings.maxSoundsPerFrame) {
				System.out.println((sounds.size() - LaunchSettings.maxSoundsPerFrame) + " discarded");
			}*/
			
			sounds.clear();
			
			while (soundLog.size() > 20) {
				soundLog.remove(soundLog.size() - 1);
			}
		}
	}
	
	@Override
	public int compare(SoundEffect.Sound a, SoundEffect.Sound b) {
		double aApparentVolume = a.volume / (1 + Math.abs(a.x) + Math.abs(a.y));
		double bApparentVolume = b.volume / (1 + Math.abs(b.x) + Math.abs(b.y));
		return Double.compare(bApparentVolume, aApparentVolume); // Because we want the loudest ones first.
	}
	
	public static String rPad(String s, int l) {
		while (s.length() < l) {
			s += " ";
		}
		return s;
	}
	
	public static String lPad(String s, int l) {
		while (s.length() < l) {
			s = " " + s;
		}
		return s;
	}
	
	public static String rPad(int i, int l) {
		return rPad("" + i, l);
	}
	
	private static String rPad(double d, int l) {
		if (d > 99) { d = 99; }
		String s = "" + d;
		if (s.indexOf(".") != 2) {
			s = " " + s;
		}
		s = s.substring(0, StrictMath.min(s.length(), 5));
		while (s.length() < l) {
			s += " ";
		}
		return s;
	}

	@Override
	public boolean chatEnabled(UniScreen us) { return false; }
	
	@Override public boolean arrowKeysInUse(UniScreen us) { return false; }
}
