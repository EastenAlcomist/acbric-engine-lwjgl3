package com.zarkonnen.airships;

import com.zarkonnen.catengine.util.Utils.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONObject;
import com.zarkonnen.catengine.lwjgl3.GLCompat;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Triangulator;

public strictfp class ShapeUtils {
	public static final int MAX_INFLUENCE_DIST = 3;
	public static final int MAX_ROAD_INFLUENCE_DIST = 3;
	
	public static final int RIGHT = 0, DOWN = 1, LEFT = 2, UP = 3;
	public static final int[][] WALL_DELTAS = {
		/* RIGHT */{ 0, 0 },
		/* DOWN */ { -1, 0 },
		/* LEFT */ { -1, -1 },
		/* UP */ { 0, -1 }
	};
	public static final int[][] MOVE_DELTAS = {
		/* RIGHT */{ 1, 0 },
		/* DOWN */ { 0, 1 },
		/* LEFT */ { -1, 0 },
		/* UP */ { 0, -1 }
	};
	
	public static void smooth(ArrayList<Area> areas, ArrayList<P> allPoints, int maxInfluenceDist) {
		for (P p : allPoints) {
			double xAccum = 0;
			double yAccum = 0;
			double totalWeight = 0;
			for (Area a : p.areas) {
				int pIndex = a.points.indexOf(p);
				if (pIndex == -1) {
					throw new RuntimeException("Point claims to be in area it's not in.");
				}
				int aSize = a.points.size();
				for (int i = pIndex - maxInfluenceDist; i < pIndex + maxInfluenceDist + 1; i++) {
					int index = (i + aSize) % aSize;
					int dist = StrictMath.min(StrictMath.abs(pIndex + aSize - index) % aSize, StrictMath.abs(index + aSize - pIndex) % aSize);
					double weight = 1.0 / (dist * dist + 1);
					xAccum += a.points.get(index).x * weight;
					yAccum += a.points.get(index).y * weight;
					totalWeight += weight;
				}
			}
			p.x = xAccum / totalWeight;
			p.y = yAccum / totalWeight;
		}
	}
	
	public static void perturb(ArrayList<P> ps) {
		Perlin perlin = new Perlin();
		for (int i = 0; i < ps.size(); i++) {
			P p = ps.get(i);
			p.x += perlin.pnoise(p.x, p.y, 77) * 1.5;
			p.y += perlin.pnoise(p.x + 22, p.y + 903, 304) * 1.5;
		}
	}
	
	public static Pair<ArrayList<Area>, ArrayList<Area>> allAreas(boolean[][] water, int[][] cityOwnership) {
		Pair<ArrayList<Area>, ArrayList<Area>> areas = new Pair<ArrayList<Area>, ArrayList<Area>>(
				new ArrayList<Area>(),
				new ArrayList<Area>()
		);
		ArrayList<P> allPoints = new ArrayList<P>();
		HashMap<IntP, P> canonicalPs = new HashMap<IntP, P>();
		landBoundaryAreas(water, canonicalPs, allPoints, areas.a);
		cityOwnershipAreas(cityOwnership, canonicalPs, allPoints, areas.b);
		// Combine the areas into one list.
		ArrayList<Area> all = new ArrayList<Area>();
		all.addAll(areas.a);
		all.addAll(areas.b);
		smooth(all, allPoints, MAX_INFLUENCE_DIST);
		perturb(allPoints);
		smooth(all, allPoints, MAX_INFLUENCE_DIST);
		return areas;
	}
	
	public static void landBoundaryAreas(boolean[][] water, HashMap<IntP, P> canonicalPs, ArrayList<P> allPoints, ArrayList<Area> areas) {
		byte[][] visited = new byte[water.length][water[0].length];
		// Use flood fill to determine land masses.
		// For each land mass, use crawl-along to generate polygonal area of land mass.
		LinkedList<int[]> q = new LinkedList<int[]>();
		byte id = 0;
		for (int y = 0; y < water.length; y++) {
			for (int x = 0; x < water[0].length; x++) {
				if (!water[y][x] && visited[y][x] == 0) {
					id++;
					visited[y][x] = id;
					q.add(new int[] { x, y });
					while (!q.isEmpty()) {
						int[] xy = q.pop();
						int qx = xy[0];
						int qy = xy[1];
						for (int dy = -1; dy < 2; dy++) {
							if (qy + dy < 0 || qy + dy >= water.length) { continue; }
							for (int dx = -1; dx < 2; dx++) {
								if (dx != 0 && dy != 0) { continue; } // No diagonal flood fill. It causes the boundary tracing to go wrong.
								if (qx + dx < 0 || qx + dx >= water[0].length) { continue; }
								if (!water[qy + dy][qx + dx] && visited[qy + dy][qx + dx] == 0) {
									visited[qy + dy][qx + dx] = id;
									q.add(new int[] { qx + dx, qy + dy });
								}
							}
						}
					}
					
					// Now encircle
					Area area = new Area(-1);
					int xPos = x;
					int yPos = y;
					int dir = RIGHT;
					do {
						IntP ip = new IntP(xPos, yPos);
						P p = canonicalPs.get(ip);
						if (p == null) {
							p = new P(xPos, yPos);
							allPoints.add(p);
							canonicalPs.put(ip, p);
						}
						p.areas.add(area);
						area.points.add(p);
						boolean found = false;
						for (int dirDelta = -1; dirDelta < 2; dirDelta++) {
							int lookDir = (dir + dirDelta + 4) % 4;
							int lookX = xPos + WALL_DELTAS[lookDir][0];
							int lookY = yPos + WALL_DELTAS[lookDir][1];
							if (lookY < 0 || lookY >= water.length || lookX < 0 || lookX >= water[0].length) {
								continue;
							}
							if (visited[lookY][lookX] == id) {
								xPos += MOVE_DELTAS[lookDir][0];
								yPos += MOVE_DELTAS[lookDir][1];
								dir = lookDir;
								found = true;
								break;
							}
						}
						if (!found) {
							throw new RuntimeException("Got lost!");
						}
					} while (xPos != x || yPos != y);
					areas.add(area);
				}
			}
		}
	}

	public static void cityOwnershipAreas(int[][] cityOwnership, HashMap<IntP, P> canonicalPs, ArrayList<P> allPoints, ArrayList<Area> areas) {
		int visitingCity = -1;
		boolean progress = true;
		while (progress) {
			progress = false;
			for (int y = 0; y < cityOwnership.length; y++) {
				for (int x = 0; x < cityOwnership[0].length; x++) {
					if (cityOwnership[y][x] == visitingCity + 1) {
						int steps = 0;
						progress = true;
						visitingCity++;
						Area area = new Area(visitingCity);
						int xPos = x;
						int yPos = y;
						int dir = RIGHT;
						do {
							IntP ip = new IntP(xPos, yPos);
							P p = canonicalPs.get(ip);
							if (p == null) {
								p = new P(xPos, yPos);
								allPoints.add(p);
								canonicalPs.put(ip, p);
							}
							if (!p.areas.contains(area)) {
								p.areas.add(area);
								area.points.add(p);
							}
							
							boolean found = false;
							for (int dirDelta = -1; dirDelta < 2; dirDelta++) {
								int lookDir = (dir + dirDelta + 4) % 4;
								int lookX = xPos + WALL_DELTAS[lookDir][0];
								int lookY = yPos + WALL_DELTAS[lookDir][1];
								if (lookY < 0 || lookY >= cityOwnership.length || lookX < 0 || lookX >= cityOwnership[0].length) {
									continue;
								}
								if (cityOwnership[lookY][lookX] == visitingCity) {
									xPos += MOVE_DELTAS[lookDir][0];
									yPos += MOVE_DELTAS[lookDir][1];
									dir = lookDir;
									found = true;
									break;
								}
							}
							if (!found) {
								throw new RuntimeException("Got lost!");
							}
						} while (steps++ < 10000 && (xPos != x || yPos != y));
						areas.add(area);
					}
				}
			}
		}
	}
	
	public static final class IntP {
		int x, y;

		public IntP(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof IntP)) { return false; }
			return x == ((IntP) o).x && y == ((IntP) o).y;
		}
		
		@Override
		public int hashCode() {
			return 29 + x * 37 + y * 1029;
		}
	}
	
	public static class P {
		double x, y;

		public P(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		ArrayList<Area> areas = new ArrayList<Area>();
	}
	
	public static strictfp class Area {
		public int identifier;
		public ArrayList<P> points = new ArrayList<P>();
		public transient TrianglesArea triArea;
		
		public TrianglesArea getPolygon() {
			if (triArea == null) {
				triArea = new TrianglesArea(this);
			}
			return triArea;
		}

		public Area(int identifier) {
			this.identifier = identifier;
		}
	}
	
	public static strictfp class TrianglesArea {
		public float[] triData;
		public Polygon polygon;
		
		public TrianglesArea(Area area) {
			float[] points = new float[area.points.size() * 2];
			for (int i = 0; i < area.points.size(); i++) {
				points[i * 2] = (float) (area.points.get(i).x);
				points[i * 2 + 1] = (float) (area.points.get(i).y);
			}
			polygon = new Polygon(points);
			Triangulator t = polygon.getTriangles();
			int count = t.getTriangleCount();
			triData = new float[count * 6];
			for (int i = 0; i < count; i++) {
				for (int p = 0; p < 3; p++) {
					float[] xy = t.getTrianglePoint(i, p);
					triData[i * 6 + p * 2] = xy[0];
					triData[i * 6 + p * 2 + 1] = xy[1];
				}
			}
		}
		
		public TrianglesArea(SavePreviewInfo.Area area) {
			float[] points = new float[area.points.size() * 2];
			for (int i = 0; i < area.points.size(); i++) {
				points[i * 2] = (float) (area.points.get(i).x);
				points[i * 2 + 1] = (float) (area.points.get(i).y);
			}
			polygon = new Polygon(points);
			Triangulator t = polygon.getTriangles();
			int count = t.getTriangleCount();
			triData = new float[count * 6];
			for (int i = 0; i < count; i++) {
				for (int p = 0; p < 3; p++) {
					float[] xy = t.getTrianglePoint(i, p);
					triData[i * 6 + p * 2] = xy[0];
					triData[i * 6 + p * 2 + 1] = xy[1];
				}
			}
		}
		
		public void draw() {
			for (int i = 0; i < triData.length; i += 2) {
				GLCompat.glVertex2f(triData[i], triData[i + 1]);
			}
		}
	}
}
