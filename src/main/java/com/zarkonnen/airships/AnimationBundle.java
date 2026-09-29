package com.zarkonnen.airships;

import java.util.EnumMap;
import org.json.JSONArray;
import org.json.JSONObject;

public strictfp class AnimationBundle extends Loadable {
	public final double width, height;
	public final BodyPlan bodyPlan;
	public final EnumMap<AnimationType, Animation> animations;
	
	public AnimationBundle(JSONObject o) {
		super(o.getString("name"));
		bodyPlan = BodyPlan.ofName(o.getString("bodyPlan"));
		width = o.getDouble("width");
		height = o.getDouble("height");
		animations = new EnumMap<AnimationType, AnimationBundle.Animation>(AnimationType.class);
		JSONArray a = o.getJSONArray("animations");
		for (int i = 0; i < a.length(); i++) {
			animations.put(AnimationType.valueOf(a.getJSONObject(i).getString("type")), new Animation(a.getJSONObject(i), bodyPlan));
		}
	}
	
	public static strictfp class Animation {
		public static final int LOOPING = 0;
		public final int length;
		public final Side side;
		public final BodyPlan bodyPlan;
		public final Part[] parts;
		
		public Animation(JSONObject o, BodyPlan bodyPlan) {
			length = o.optInt("length", LOOPING);
			side = Side.valueOf(o.getString("side"));
			this.bodyPlan = bodyPlan;
			parts = new Part[bodyPlan.partNames.get(side).size()];
			JSONArray a = o.getJSONArray("parts");
			for (int i = 0; i < a.length(); i++) {
				JSONObject p = a.getJSONObject(i);
				parts[bodyPlan.getIndex(side, p.getString("name"))] = new Part(p);
			}
		}
		
		public static strictfp class Part {
			public final double x;
			public final double y;
			public final double rotationOffset;
			public final double rotationPeriod;
			public final double rotationWidth;
			public final double rotationHeight;
			public final double waveOffset;
			public final double wavePeriod;
			public final double waveStartAngle;
			public final double waveEndAngle;
			public final boolean holdsResource;
			
			public Part(JSONObject o) {
				x = o.getDouble("x");
				y = o.getDouble("y");
				rotationOffset = o.optDouble("cOff", 0);
				rotationPeriod = o.optDouble("cPeriod", 1000);
				rotationWidth = o.optDouble("cW", 0);
				rotationHeight = o.optDouble("cH", 0);
				waveOffset = o.optDouble("wOff", 0);
				wavePeriod = o.optDouble("wPeriod", 1000);
				waveStartAngle = o.optDouble("wStart", 0);
				waveEndAngle = o.optDouble("wEnd", 0);
				holdsResource = o.optBoolean("holdsResource", false);
			}
		}
	}
		
	public static AnimationBundle ofName(String name) {
		return ofName(AnimationBundle.class, name);
	}
}
