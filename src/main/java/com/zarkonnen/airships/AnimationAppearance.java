package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import java.util.EnumMap;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class AnimationAppearance extends Loadable {
	public final AnimationBundle bundle;
	public final EnumMap<Side, Img[]> images = new EnumMap<Side, Img[]>(Side.class);
	public final EnumMap<Side, ArrayList<BodyPartEmitter>[]> emitters = new EnumMap<Side, ArrayList<BodyPartEmitter>[]>(Side.class);
	public final EnumMap<Side, ArrayList<BodyPartEmitter>[]> deadEmitters = new EnumMap<Side, ArrayList<BodyPartEmitter>[]>(Side.class);
	public final EnumMap<AnimationType, CrewFrameAnimation> frameAnimations;
	public final int frameAnimationBoundingBoxWidth, frameAnimationBoundingBoxHeight;
	
	public AnimationAppearance(JSONObject o) {
		super(o.getString("name"));
		if (o.has("frameAnimations")) {
			bundle = null;
			frameAnimations = new EnumMap<AnimationType, CrewFrameAnimation>(AnimationType.class);
			JSONObject fas = o.getJSONObject("frameAnimations");
			for (AnimationType at : AnimationType.values()) {
				if (fas.has(at.name())) {
					frameAnimations.put(at, new CrewFrameAnimation(fas.getJSONObject(at.name())));
				}
			}
			frameAnimationBoundingBoxWidth = o.getInt("frameAnimationBoundingBoxWidth");
			frameAnimationBoundingBoxHeight = o.getInt("frameAnimationBoundingBoxHeight");
		} else {
			bundle = AnimationBundle.ofName(o.getString("bundle"));
			frameAnimations = null;
			frameAnimationBoundingBoxWidth = 0;
			frameAnimationBoundingBoxHeight = 0;
			String spritesheet = o.getString("spritesheet");
			for (Side side : Side.values()) {
				images.put(side, new Img[bundle.bodyPlan.partNames.get(side).size()]);
				emitters.put(side, new ArrayList[bundle.bodyPlan.partNames.get(side).size()]);
				deadEmitters.put(side, new ArrayList[bundle.bodyPlan.partNames.get(side).size()]);
			}
			JSONArray a = o.getJSONArray("images");
			for (int i = 0; i < a.length(); i++) {
				JSONObject io = a.getJSONObject(i);
				Side side = Side.valueOf(io.getString("side")); 
				images.get(side)[bundle.bodyPlan.getIndex(side, io.getString("part"))] =
						new Img(spritesheet, io.getInt("x"), io.getInt("y"), io.getInt("w"), io.getInt("h"), io.optBoolean("flipped", false));
				if (io.has("emitters")) {
					ArrayList<BodyPartEmitter> l = new ArrayList<BodyPartEmitter>();
					emitters.get(side)[bundle.bodyPlan.getIndex(side, io.getString("part"))] = l;
					JSONArray ems = io.getJSONArray("emitters");
					for (int ei = 0; ei < ems.length(); ei++) {
						JSONObject em = ems.getJSONObject(ei);
						l.add(new BodyPartEmitter(ParticleType.ofName(em.getString("type")), em.getDouble("freq"), em.getDouble("x"), em.getDouble("y"), em.optDouble("scale", 1.0)));
					}
				}
				if (io.has("deadEmitters")) {
					ArrayList<BodyPartEmitter> l = new ArrayList<BodyPartEmitter>();
					deadEmitters.get(side)[bundle.bodyPlan.getIndex(side, io.getString("part"))] = l;
					JSONArray ems = io.getJSONArray("deadEmitters");
					for (int ei = 0; ei < ems.length(); ei++) {
						JSONObject em = ems.getJSONObject(ei);
						l.add(new BodyPartEmitter(ParticleType.ofName(em.getString("type")), em.getDouble("freq"), em.getDouble("x"), em.getDouble("y"), em.optDouble("scale", 1.0)));
					}
				}
			}
		}
	}
	
	public static strictfp class BodyPartEmitter {
		public final ParticleType type;
		public final double freq;
		public final double x, y;
		public final double scale;

		public BodyPartEmitter(ParticleType type, double freq, double x, double y, double scale) {
			this.type = type;
			this.freq = freq;
			this.x = x;
			this.y = y;
			this.scale = scale;
		}
	}
	
	public static AnimationAppearance ofName(String name) {
		return ofName(AnimationAppearance.class, name);
	}
}
