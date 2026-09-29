package com.zarkonnen.airships;

import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class BirdType extends Loadable {
	public final Bird.Behavior behavior;
	public final double maxSpeed;
	public final Appearance left, right, down;
	public final Bird.Spawner spawner;
	
	public BirdType(JSONObject o) {
		super(o.getString("name"));
		maxSpeed = o.getInt("maxSpeed");
		left = new Appearance(o.getJSONObject("left"));
		right = new Appearance(o.getJSONObject("right"));
		down = new Appearance(o.getJSONObject("down"));
		if (o.getString("behavior").equals("circle")) {
			behavior = new Bird.Circle(
				o.optInt("circleW", 100),
				o.optInt("circleH", 10),
				o.optInt("upDownH", 200),
				o.optInt("circlePeriod", 5100),
				o.optInt("upDownPeriod", 62300)
			);
		} else if (o.getString("behavior").equals("flyAcross")) {
			behavior = new Bird.FlyAcross();
		} else {
			behavior = new Bird.Boid();
		}
		if (o.getString("spawner").equals("v")) {
			spawner = new Bird.VSpawner(o.getInt("vMinY"), o.getInt("vMaxY"), o.getInt("vXSpawn"));
		} else {
			if (o.optBoolean("flockTargetH", false)) {
				spawner = new Bird.FlockSpawner(
					o.getInt("flockMin"),
					o.getInt("flockMax"),
					o.getInt("flockDist"),
					o.getInt("flockTargetH")
				);
			} else {
				spawner = new Bird.FlockSpawner(
					o.getInt("flockMin"),
					o.getInt("flockMax"),
					o.getInt("flockDist")
				);
			}
		}
	}

	public void spawn(ArrayList<Bird> birds, double minX, double minY, double maxX, double maxY) {
		spawner.spawn(birds, this, minX, minY, maxX, maxY);
	}
	
	public static BirdType ofName(String name) {
		return ofName(BirdType.class, name);
	}
}