package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import org.json.JSONArray;
import org.json.JSONObject;

public class PortraitMessageType extends Loadable {
	public String eventInfoPrefix;
	public Img[] messageImages;
	public CrewType victimCrewType;
	public CrewType observingCrewType;
	public boolean observerInSameShip;
	public int maxDistanceSq;
	public int numberOfMessages;
	public double minZoom, maxZoom;
	public String heroTypeName;
	
	public PortraitMessageType(JSONObject o) {
		super(o.getString("name"), o.optInt("sort", 1000 + o.getString("eventInfoPrefix").length()));
		eventInfoPrefix = o.getString("eventInfoPrefix");
		JSONArray a = o.getJSONArray("messageImages");
		messageImages = new Img[a.length()];
		for (int i = 0; i < a.length(); i++) {
			messageImages[i] = new Img(a.getString(i));
		}
		
		if (o.has("victimCrewType")) {
			victimCrewType = CrewType.ofName(o.getString("victimCrewType"));
		}
		if (o.has("observingCrewType")) {
			observingCrewType = CrewType.ofName(o.getString("observingCrewType"));
		}
		observerInSameShip = o.optBoolean("observerInSameShip", false);
		maxDistanceSq = o.optInt("maxDistance", 0) * o.optInt("maxDistance", 0);
		numberOfMessages = messageImages.length;
		minZoom = o.optDouble("minZoom", 0.0001);
		maxZoom = o.optDouble("maxZoom", 10000);
		heroTypeName = o.optString("heroType", null);
	}
	
	public boolean matchesNoObserverNeeded(ExceptionalCombatEvent e, Combat.Side side) {
		if (victimCrewType != null && victimCrewType != e.victimType) { return false; }
		if (heroTypeName == null) {
			return true;
		} else {
			for (Airship s : side.ships) {
				if (s.getCaptain() != null && s.getCaptain().type.name.equals(heroTypeName)) { return true; }
			}
			return false;
		}
	}
	
	public boolean matches(ExceptionalCombatEvent e, Crewman observer, double crewX, double crewY, Airship observerShip) {
		if (heroTypeName != null && (observerShip.getCaptain() == null || !observerShip.getCaptain().type.name.equals(heroTypeName))) { return false; }
		if (observingCrewType != observer.type) { return false; }
		if (victimCrewType != null && victimCrewType != e.victimType) { return false; }
		if (observerInSameShip && (e.withinShip == null || observerShip == null || e.withinShip != observerShip)) { return false; }
		return maxDistanceSq == 0 || (e.x - crewX) * (e.x - crewX) + (e.y - crewY) * (e.y - crewY) > maxDistanceSq;
	}
	
	public static PortraitMessageType ofName(String name) {
		return ofName(PortraitMessageType.class, name);
	}
}
