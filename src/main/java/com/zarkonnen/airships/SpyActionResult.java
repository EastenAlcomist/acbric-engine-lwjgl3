package com.zarkonnen.airships;

public class SpyActionResult {
	public final Spy.CitySpyAction citySpyAction;
	public final Spy.ShipSpyAction shipSpyAction;
	public final boolean success;
	public final City city;
	public final Empire actor;
	public final Empire victim;
	public final String actorText;
	public final String victimText;

	public SpyActionResult(Spy.CitySpyAction citySpyAction, boolean success, City city, Empire actor, Empire victim, String actorText, String victimText) {
		this.citySpyAction = citySpyAction;
		this.shipSpyAction = null;
		this.success = success;
		this.actor = actor;
		this.victim = victim;
		this.actorText = actorText;
		this.victimText = victimText;
		this.city = city;
	}

	public SpyActionResult(Spy.ShipSpyAction shipSpyAction, boolean success, City city, Empire actor, Empire victim, String actorText, String victimText) {
		this.citySpyAction = null;
		this.shipSpyAction = shipSpyAction;
		this.success = success;
		this.actor = actor;
		this.victim = victim;
		this.actorText = actorText;
		this.victimText = victimText;
		this.city = city;
	}
}
