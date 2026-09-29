package com.zarkonnen.airships;

import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.Img;
import org.json.JSONObject;

public class Cursor extends Loadable {
	public final Img imgSmall, imgMedium, imgLarge;
	public final int dxSmall, dySmall, dxMedium, dyMedium, dxLarge, dyLarge;
	
	public void draw(Draw d, double x, double y, GUIScale scale) {
		switch (scale) {
			case SMALL:
				d.blit(imgSmall, x + dxSmall, y + dySmall);
				break;
			case MEDIUM:
				d.blit(imgMedium, x + dxMedium, y + dyMedium);
				break;
			case LARGE:
				d.blit(imgLarge, x + dxLarge, y + dyLarge);
				break;
		}
	}
	
	public int dx(GUIScale scale) {
		switch (scale) {
			case SMALL:
				return dxSmall;
			case MEDIUM:
				return dxMedium;
			case LARGE:
				return dxLarge;
		}
		return 0;
	}
	
	public int dy(GUIScale scale) {
		switch (scale) {
			case SMALL:
				return dySmall;
			case MEDIUM:
				return dyMedium;
			case LARGE:
				return dyLarge;
		}
		return 0;
	}
	
	public Cursor(JSONObject o) {
		super(o.getString("name"));
		JSONObject i;
		
		dxSmall = o.optInt("dxSmall", 0);
		dySmall = o.optInt("dySmall", 0);
		i = o.getJSONObject("imgSmall");
		imgSmall = new Img(i.getString("src"), i.getInt("x"), i.getInt("y"), i.getInt("w"), i.getInt("h"), false);
		
		if (o.has("imgMedium")) {
			dxMedium = o.optInt("dxMedium", 0);
			dyMedium = o.optInt("dyMedium", 0);
			i = o.getJSONObject("imgMedium");
			imgMedium = new Img(i.getString("src"), i.getInt("x"), i.getInt("y"), i.getInt("w"), i.getInt("h"), false);
		} else {
			dxMedium = dxSmall;
			dyMedium = dySmall;
			imgMedium = imgSmall;
		}
		
		if (o.has("imgLarge")) {
			dxLarge = o.optInt("dxLarge", 0);
			dyLarge = o.optInt("dyLarge", 0);
			i = o.getJSONObject("imgLarge");
			imgLarge = new Img(i.getString("src"), i.getInt("x"), i.getInt("y"), i.getInt("w"), i.getInt("h"), false);
		} else {
			dxLarge = dxMedium;
			dyLarge = dyMedium;
			imgLarge = imgMedium;
		}
	}
	
	private Cursor() {
		super("POINTER");
		dxSmall = -14;
		dySmall = -1;
		imgSmall = new Img("ui", 164, 827, 16, 21, false);
		dxMedium = -14;
		dyMedium = -1;
		imgMedium = new Img("ui", 164, 827, 16, 21, false);
		dxLarge = -14;
		dyLarge = -1;
		imgLarge = new Img("ui", 164, 827, 16, 21, false);
	}
	
	public static Cursor ofName(String name) {
		try {
			return ofName(Cursor.class, name);
		} catch (Exception e) {
			return new Cursor();
		}
	}
}
