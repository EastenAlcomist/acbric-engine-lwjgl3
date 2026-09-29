package com.zarkonnen.airships;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class GridBody extends Body {
	public abstract int getGridWidth();
	public abstract int getGridHeight();
	public abstract boolean solidAt(int x, int y);
	public abstract boolean fullAt(int x, int y);
	public abstract boolean enterableAt(int x, int y);
	public abstract double yBoundaryAt(double worldX);
	public abstract int firstSolidBlockYAt(int x);
	public abstract ArrayList<GridLocation> reachable(int x, int y);
	public abstract void clearReachable(int x, int y);
	public abstract boolean concaveAt(int x, int y);
	public abstract GridLocation locationAt(int x, int y);
	
	public GridLocation locationAtWorldCoords(double ptX, double ptY) {
		return locationAt((int) StrictMath.floor((ptX - getX()) / AGame.SGS), (int) StrictMath.floor((ptY - getY()) / AGame.SGS));
	}
	
	private transient ArrayList<GridLocation> concavePoints = new ArrayList<GridLocation>();
	private transient boolean concavePointsCalculated;
	// Boundaries where the body can have a grappling hook fired at it.
	// Indexing is 1-based, so 0 means "not calculated yet". -1 means "none".
	public transient int[] topHookBoundaries;
	public transient int[] bottomHookBoundaries;
	public transient int[] leftHookBoundaries;
	public transient int[] rightHookBoundaries;
	
	public GridRef jumpPointCache;
	public GridBody jumpPointCacheTarget;
	public GridRef fallPointCache;
	public GridBody fallPointCacheTarget;
	
	public ArrayList<GridLocation> getConcavePoints() {
		calcConcavePoints();
		return concavePoints;
	}
	
	public void clearConcavePoints() {
		concavePointsCalculated = false;
		concavePoints.clear();
	}
		
	void calcConcavePoints() {
		if (concavePointsCalculated) { return; }
		concavePointsCalculated = true;
		// Find the concave points
		concavePoints.clear();
		final int gh = getGridHeight(), gw = getGridWidth();
		for (int gy = 0; gy < gh; gy++) { for (int gx = 0; gx < gw; gx++) { 
			clearReachable(gx, gy);
			if (concaveAt(gx, gy)) {
				concavePoints.add(locationAt(gx, gy));
			}
		}}
		int csz = concavePoints.size();
		for (int ci = 0; ci < csz; ci++) {
			GridLocation t1 = concavePoints.get(ci);
			for (int ci2 = 0; ci2 < csz; ci2++) {
				if (ci2 == ci) { continue; }
				GridLocation t2 = concavePoints.get(ci2);
				if (reachable(t1, t2)) {
					t1.reachable().add(t2);
				}
			}
		}
	}
	
	// Manhattan
	public static double manhattan(GridLocation a, GridLocation b) {
		return StrictMath.abs(a.worldGridX() - b.worldGridX()) + StrictMath.abs(a.worldGridY() - b.worldGridY());
	}
	
	public static double heuristic(GridLocation a, GridLocation b) {
		return manhattan(a, b);
	}
	
	// A*?
	public OutsideBodyPath getPath(GridLocation start, GridLocation goal) {
		calcConcavePoints();
		for (GridLocation node : concavePoints) {
			if (start.worldGridX() == node.worldGridX() && start.worldGridY() == node.worldGridY()) {
				start = node;
			}
			if (goal.worldGridX() == node.worldGridX() && goal.worldGridY() == node.worldGridY()) {
				goal = node;
			}
		}
		if (start.worldGridX() == goal.worldGridX() && start.worldGridY() == goal.worldGridY()) {
			start = goal;
		}
		if (start.reachable() == null) {
			//System.out.println("Start reachables are null, unable to path.");
			return null;
		}
		if (start.reachable().isEmpty()) {
			for (GridLocation node : concavePoints) {
				if (!(start.worldGridX() == node.worldGridX() && start.worldGridY() == node.worldGridY()) && reachable(start, node)) {
					start.reachable().add(node);
				}
			}
		}
		// If that doesn't work let's see if we can find somewhere nearby to go to.
		GridLocation originalStart = start;
		if (start.reachable().isEmpty()) {
			lp: for (int dy = -1; dy < 2; dy++) {
				for (int dx = -1; dx < 2; dx++) {
					GridLocation gl2 = start.getRelative(dx, dy);
					if (gl2 != null && gl2.solid()) {
						if (gl2.reachable() == null) { continue; }
						if (gl2.reachable().isEmpty()) {
							for (GridLocation node : concavePoints) {
								if (!(gl2.worldGridX() == node.worldGridX() && gl2.worldGridY() == node.worldGridY()) && reachable(gl2, node)) {
									gl2.reachable().add(node);
								}
							}
						}
						if (!gl2.reachable().isEmpty()) {
							start = gl2;
							break lp;
						}
					}
				}
			}
		}
		HashSet<GridLocation> closedSet = new HashSet<GridLocation>();
		ArrayList<GridLocation> openSet = new ArrayList<GridLocation>();
		openSet.add(start);
		HashMap<GridLocation, GridLocation> cameFrom = new HashMap<GridLocation, GridLocation>();
		HashMap<GridLocation, Double> gScore = new HashMap<GridLocation, Double>();
		gScore.put(start, 0.0); gScore.put(goal, Double.POSITIVE_INFINITY);
		for (GridLocation gl : concavePoints) {
			gScore.put(gl, Double.POSITIVE_INFINITY);
		}
		HashMap<GridLocation, Double> fScore = new HashMap<GridLocation, Double>();
		fScore.put(start, heuristic(start, goal)); fScore.put(goal, Double.POSITIVE_INFINITY);
		for (GridLocation gl : concavePoints) {
			fScore.put(gl, Double.POSITIVE_INFINITY);
		}
		/*System.out.println("start " + start);
		System.out.println("goal " + goal);*/
		while (!openSet.isEmpty()) {
			double lowestFScore = Double.MAX_VALUE;
			GridLocation current = null;
			for (GridLocation gl : openSet) {
				if (current == null || fScore.get(gl) < lowestFScore) {
					current = gl;
					lowestFScore = fScore.get(gl);
				}
			}
			//System.out.println("current " + current);
			if (current == goal) {
				return reconstructPath(cameFrom, current, originalStart);
			}
			openSet.remove(current);
			closedSet.add(current);
			if (reachable(current, goal) && !closedSet.contains(goal)) {
				GridLocation neighbor = goal;
				double tentativeGScore = gScore.get(current) + manhattan(current, neighbor);
				//System.out.println("tentGS " + tentativeGScore);
				if (!openSet.contains(neighbor)) {
					openSet.add(neighbor);
				} else if (tentativeGScore >= gScore.get(neighbor)) {
					continue;
				}
				cameFrom.put(neighbor, current);
				gScore.put(neighbor, tentativeGScore);
				double newFScore = tentativeGScore + heuristic(neighbor, goal);
				fScore.put(neighbor, newFScore);
			}
			for (GridLocation neighbor : current.reachable()) {
				//System.out.println("neigh " + neighbor);
				if (closedSet.contains(neighbor)) { continue; }
				double tentativeGScore = gScore.get(current) + manhattan(current, neighbor);
				//System.out.println("tentGS " + tentativeGScore);
				if (!openSet.contains(neighbor)) {
					openSet.add(neighbor);
				} else if (tentativeGScore >= gScore.get(neighbor)) {
					continue;
				}
				cameFrom.put(neighbor, current);
				gScore.put(neighbor, tentativeGScore);
				double newFScore = tentativeGScore + heuristic(neighbor, goal);
				fScore.put(neighbor, newFScore);
			}
		}
		return null;
	}
	
	public OutsideBodyPath reconstructPath(HashMap<GridLocation, GridLocation> cameFrom, GridLocation current, GridLocation originalStart) {
		ArrayList<GridLocation> totalPath = new ArrayList<GridLocation>();
		totalPath.add(current);
		while (cameFrom.containsKey(current)) {
			current = cameFrom.get(current);
			totalPath.add(0, current);
		}
		if (!totalPath.isEmpty() && totalPath.get(0) != originalStart) {
			totalPath.add(originalStart);
		}
		return new OutsideBodyPath(totalPath);
	}
	
	private boolean reachable(GridLocation t1, GridLocation t2) {
		int tx = t1.worldGridX();
		int ty = t1.worldGridY();
		int dstX = t2.worldGridX();
		int dstY = t2.worldGridY();
		while (tx != dstX && ty != dstY) {
			if (!solidAt(tx, ty)) { return false; }
			int dx = dstX > tx ? 1 : -1;
			int dy = dstY > ty ? 1 : -1;
			if (!solidAt(tx + dx, ty) || !solidAt(tx, ty + dy)) {
				return false;
			}
			tx += dx;
			ty += dy;
		}
		while (tx != dstX) {
			if (!solidAt(tx, ty)) { return false; }
			tx += dstX > tx ? 1 : -1;
		}
		while (ty != dstY) {
			if (!solidAt(tx, ty)) { return false; }
			ty += dstY > ty ? 1 : -1;
		}
		return solidAt(tx, ty);
	}
	
	public double frictionMult(Combat c) {
		LandFormation ground = c.landFormations.get(0);
		if (ground.landscapeType.hasWater) {
			return getY() + getBBHeight() > AGame.GROUND_LEVEL ? 10 : 1;
		} else {
			return 1;
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " @ " + getX() + ", " + getY() + ", " + getBBWidth() + ", " + getBBHeight();
	}
}
