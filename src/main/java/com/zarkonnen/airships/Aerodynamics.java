package com.zarkonnen.airships;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;


public strictfp class Aerodynamics {
	public static enum Dir {
		FROM_FRONT {
			@Override
			int uv2x(Airship ship, int u, int v) { return u; }

			@Override
			int uv2y(Airship ship, int u, int v) { return v; }

			@Override
			int uSize(Airship ship) { return ship.getWidth(); }

			@Override
			int vSize(Airship ship) { return ship.getHeight(); }
			
			@Override
			boolean beamAt(Airship ship, int u, int v) {
				int x = uv2x(ship, u, v);
				int y = uv2y(ship, u, v);
				Tile t = ship.tileAt(x, y);
				if (t == null) { return false; }
				Module m = t.module;
				return m.type.gunPortsCreateDrag() &&
					(x == m.x + m.type.getW() - 1 && m.type.isFrontOnly()[y - m.y]);
			}
		},
		FROM_BACK {
			@Override
			int uv2x(Airship ship, int u, int v) { return ship.getWidth() - u - 1; }

			@Override
			int uv2y(Airship ship, int u, int v) { return v; }
			
			@Override
			int uSize(Airship ship) { return ship.getWidth(); }

			@Override
			int vSize(Airship ship) { return ship.getHeight(); }
			
			@Override
			boolean beamAt(Airship ship, int u, int v) {
				int x = uv2x(ship, u, v);
				int y = uv2y(ship, u, v);
				Tile t = ship.tileAt(x, y);
				if (t == null) { return false; }
				Module m = t.module;
				return m.type.gunPortsCreateDrag() &&
					(x == m.x && m.type.isBackOnly()[y - m.y]);
			}
		},
		FROM_BOTTOM {
			@Override
			int uv2x(Airship ship, int u, int v) { return v; }

			@Override
			int uv2y(Airship ship, int u, int v) { return u; }
			
			@Override
			int uSize(Airship ship) { return ship.getHeight(); }

			@Override
			int vSize(Airship ship) { return ship.getWidth(); }
			
			@Override
			boolean beamAt(Airship ship, int u, int v) {
				int x = uv2x(ship, u, v);
				int y = uv2y(ship, u, v);
				Tile t = ship.tileAt(x, y);
				if (t == null) { return false; }
				Module m = t.module;
				return m.type.gunPortsCreateDrag() &&
					(y == m.y + m.type.getH() - 1 && m.type.isBottomOnly()[x - m.x]);
			}
		},
		FROM_TOP {
			@Override
			int uv2x(Airship ship, int u, int v) { return v; }

			@Override
			int uv2y(Airship ship, int u, int v) { return ship.getHeight() - u - 1; }
			
			@Override
			int uSize(Airship ship) { return ship.getHeight(); }

			@Override
			int vSize(Airship ship) { return ship.getWidth(); }
			
			@Override
			boolean beamAt(Airship ship, int u, int v) {
				int x = uv2x(ship, u, v);
				int y = uv2y(ship, u, v);
				Tile t = ship.tileAt(x, y);
				if (t == null) { return false; }
				Module m = t.module;
				return m.type.gunPortsCreateDrag() &&
					(y == m.y && m.type.isTopOnly()[x - m.x]);
			}
		};
		abstract int uv2x(Airship ship, int u, int v);
		abstract int uv2y(Airship ship, int u, int v);
		abstract int uSize(Airship ship);
		abstract int vSize(Airship ship);
		abstract boolean beamAt(Airship ship, int u, int v);
		boolean solidAt(Airship ship, int u, int v) {
			int x = uv2x(ship, u, v);
			int y = uv2y(ship, u, v);
			Tile t = ship.tileAt(x, y);
			return t != null && !t.isMaskedEmpty() && ship.tileAt(x, y).module.type.producesHorizontalDrag();
		}
		boolean fullAt(Airship ship, int u, int v) {
			int x = uv2x(ship, u, v);
			int y = uv2y(ship, u, v);
			Tile t = ship.tileAt(x, y);
			return t != null && t.full();
		}
	}

	public static double drag(MyDraw d, Airship ship, Dir dir, boolean drawGreen) {
		int maxShift = 4;
		if (ship.getWidth() == 0) { return 0; }
		int drag = 0;
		Graphics g = d == null ? null : (Graphics) d.frame().nativeRenderer();
		if (g != null) {
			g.setColor(Color.yellow);
			g.setLineWidth(4);
		}
		int numHitLines = 0;
		for (int startV = 0; startV < dir.vSize(ship); startV++) {
			int u = dir.uSize(ship) + 1;
			int v = startV;
			int sidelined = 0;
			boolean hit = false;
			boolean hadGunPenalty = false;
			while (u > -1) {
				int v2 = v;
				int shiftAmt = 0;
				if (sidelined != 0) {
					if (dir.solidAt(ship, u - 1, v)) {
						shiftAmt = sidelined++ < 6 ? 2 : 1;
					} else {
						sidelined = 0;
					}
				}
				if (sidelined == 0) {
					if (dir.solidAt(ship, u - 1, v)) { // Bumping into something.
						if (!hit) {
							numHitLines++;
							hit = true;
						}
						shiftAmt = 1;
						// Try finding a path through within maxShift.
						boolean upBlocked = false;
						boolean downBlocked = false;
						while (shiftAmt < maxShift && (!upBlocked || !downBlocked)) {
							if (dir.solidAt(ship, u, v - shiftAmt) && dir.fullAt(ship, u, v - shiftAmt)) { upBlocked = true; }
							if (dir.solidAt(ship, u, v + shiftAmt) && dir.fullAt(ship, u, v + shiftAmt)) { downBlocked = true; }
							if (!upBlocked&& !dir.solidAt(ship, u - 1, v - shiftAmt)) {
								v2 -= shiftAmt;
								break;
							}
							if (!downBlocked && !dir.solidAt(ship, u - 1, v + shiftAmt)) {
								v2 += shiftAmt;
								break;
							}
							if (dir.fullAt(ship, u - 1, v)) {
								if (!upBlocked && dir.solidAt(ship, u - 1, v - shiftAmt) && !dir.fullAt(ship, u - 1, v - shiftAmt) && !dir.solidAt(ship, u - 1, v - shiftAmt - 1)) {
									v2 -= shiftAmt;
									break;
								}
								if (!downBlocked && dir.solidAt(ship, u - 1, v + shiftAmt) && !dir.fullAt(ship, u - 1, v + shiftAmt) && !dir.solidAt(ship, u - 1, v + shiftAmt + 1)) {
									v2 += shiftAmt;
									break;
								}
							}
							
							shiftAmt++;
							// Otherwise, give up and become sidelined, moving "through" the ship (shifting in the z-axis, really).
							if (shiftAmt == maxShift || (upBlocked && downBlocked)) {
								sidelined = 1;
							}
						}
						// Drag rebate for non-blocky things.
						if (!dir.fullAt(ship, u - 1, v) && !dir.beamAt(ship, u - 1, v)) { shiftAmt--; }
						// Drag increase for gun ports, etc.
						if (shiftAmt < 2 && dir.beamAt(ship, u - 1, v) && !hadGunPenalty) {
							shiftAmt++;
							hadGunPenalty = true;
						}
					} else if (u == 0) {
						// End of the ship, must return to startY and pay price.
						shiftAmt = StrictMath.max(0, StrictMath.abs(v - startV) - 1);
						v2 = startV;
					} else if (!dir.solidAt(ship, u - 1, v - 1) && v > startV) {
						// Turbulence off the back end of things.
						shiftAmt = 1;
						while (shiftAmt <= maxShift) {
							if (dir.solidAt(ship, u - 1, v - shiftAmt - 1) || shiftAmt == maxShift) {
								v2 -= shiftAmt;
								break;
							}
							shiftAmt++;
							// If we return to where we started in terms of flow, we're happy.
							if (StrictMath.abs(v - startV) <= shiftAmt) {
								v2 = startV;
								break;
							}
						}
						shiftAmt--;
					} else if (!dir.solidAt(ship, u - 1, v + 1) && v < startV) {
						// Turbulence off the back end of things.
						shiftAmt = 1;
						while (shiftAmt <= maxShift) {
							if (dir.solidAt(ship, u - 1, v + shiftAmt + 1) || shiftAmt == maxShift) {
								v2 += shiftAmt;
								break;
							}
							shiftAmt++;
							// If we return to where we started in terms of flow, we're happy.
							if (StrictMath.abs(v - startV) <= shiftAmt) {
								v2 = startV;
								break;
							}
						}
						shiftAmt--;
					} else if (dir.beamAt(ship, u - 1, v) && !hadGunPenalty) {
						shiftAmt = 2;
					}
				}
				drag += StrictMath.min(maxShift*maxShift*maxShift, shiftAmt * shiftAmt * shiftAmt);
				if (g != null && (shiftAmt == 0) == drawGreen) {
					switch (shiftAmt) {
						case 0:
							g.setColor(Color.green);
							break;
						case 1:
							g.setColor(Color.yellow);
							break;
						case 2:
							g.setColor(Color.orange);
							break;
						case 3:
							g.setColor(Color.red);
							break;
						case 4:
							g.setColor(new Color(180, 0, 0));
						default:
							g.setColor(new Color(130, 0, 0));
							break;
					}
					//g.setLineWidth(sidelined == 0 ? 8 : 1);
					g.drawLine(
							(float) ship.getX() + ship.gridXToWorldX(dir.uv2x(ship, u, v), 1) * AGame.SGS + AGame.SGS / 2,
							(float) ship.getY() + dir.uv2y(ship, u, v) * AGame.SGS + AGame.SGS / 2,
							(float) ship.getX() + ship.gridXToWorldX(dir.uv2x(ship, u - 1, v2), 1) * AGame.SGS + AGame.SGS / 2,
							(float) ship.getY() + dir.uv2y(ship, u - 1, v2) * AGame.SGS + AGame.SGS / 2);
				}
				u--;
				v = v2;
			}
		}
		if (g != null) {
			g.setLineWidth(1);
		}
		
		drag += numHitLines * 2.5;
		
		return StrictMath.max(1, StrictMath.min(100, StrictMath.sqrt(drag * ship.type.dragMult * 9 / ship.getMass()) - 2));
	}
}
