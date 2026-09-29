package com.zarkonnen.airships;

import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import org.json.JSONObject;

public strictfp class LoadingQuote extends Loadable {
	public final Img img;
	public final int numQuotes;
	
	public LoadingQuote(JSONObject o) {
		super(o.getString("name"));
		img = new Img(o.getString("name"));
		numQuotes = o.getInt("numQuotes");
	}
	
	public static Quote getRandom() {
		ArrayList<LoadingQuote> l = all(LoadingQuote.class);
		if (l.isEmpty()) {
			return new Quote(new Img("SCHOLAR"), "SCHOLAR_0");
		}
		LoadingQuote lq = l.get(AGame.ANIM_R.nextInt(l.size()));
		return new Quote(lq.img, lq.name + "_" + AGame.ANIM_R.nextInt(lq.numQuotes));
	}
	
	public static class Quote {
		public final Img img;
		public final String text;

		public Quote(Img img, String text) {
			this.img = img;
			this.text = text;
		}
	}
}
