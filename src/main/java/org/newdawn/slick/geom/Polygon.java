package org.newdawn.slick.geom;

import java.util.ArrayList;
import java.util.List;

/** Slick2D Polygon 兼容层，含耳切法三角剖分。 */
public class Polygon extends Shape {
	private boolean closed = true;

	public Polygon(float[] points) {
		this.points = points;
		recalc();
	}
	public Polygon() {
		this.points = new float[0];
	}

	private void recalc() {
		if (points.length == 0) { x = 0; y = 0; return; }
		x = getMinX(); y = getMinY();
	}

	public void setAllowDuplicatePoints(boolean allow) {}
	public void addPoint(float px, float py) {
		float[] np = new float[points.length + 2];
		System.arraycopy(points, 0, np, 0, points.length);
		np[points.length] = px; np[points.length + 1] = py;
		points = np;
		recalc();
	}
	public void setX(float x) { this.x = x; }
	public void setY(float y) { this.y = y; }
	public boolean closed() { return closed; }
	public void setClosed(boolean closed) { this.closed = closed; }

	public float getWidth() { return getMaxX() - getMinX(); }
	public float getHeight() { return getMaxY() - getMinY(); }
	public float getCenterX() { return getMinX() + getWidth() / 2; }
	public float getCenterY() { return getMinY() + getHeight() / 2; }

	public Triangulator getTriangles() { return new EarClipping(points); }

	public Polygon copy() { return new Polygon(points.clone()); }

	/** 耳切法三角剖分（支持简单凹多边形）。 */
	private static final class EarClipping implements Triangulator {
		private final List<float[]> tris = new ArrayList<>();

		EarClipping(float[] points) {
			int n = points.length / 2;
			List<Double> xs = new ArrayList<>(), ys = new ArrayList<>();
			for (int i = 0; i < n; i++) { xs.add((double) points[i * 2]); ys.add((double) points[i * 2 + 1]); }
			// 去掉闭合多边形末尾与首点重复的点
			if (xs.size() >= 2 && xs.get(0).equals(xs.get(xs.size() - 1)) && ys.get(0).equals(ys.get(ys.size() - 1))) {
				xs.remove(xs.size() - 1); ys.remove(ys.size() - 1);
			}
			earClip(xs, ys);
		}

		private void earClip(List<Double> xs, List<Double> ys) {
			int n = xs.size();
			if (n < 3) return;
			double area = 0;
			for (int i = 0; i < n; i++) {
				int j = (i + 1) % n;
				area += xs.get(i) * ys.get(j) - xs.get(j) * ys.get(i);
			}
			boolean cw = area < 0;
			List<Integer> idx = new ArrayList<>();
			for (int i = 0; i < n; i++) idx.add(i);

			int guard = 0;
			while (idx.size() > 3 && guard++ < n * n * 2) {
				boolean clipped = false;
				for (int k = 0; k < idx.size(); k++) {
					int a = idx.get(k), b = idx.get((k + 1) % idx.size()), c = idx.get((k + 2) % idx.size());
					double ax = xs.get(a), ay = ys.get(a), bx = xs.get(b), by = ys.get(b), cx = xs.get(c), cy = ys.get(c);
					double cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
					if (cw ? cross > 0 : cross < 0) continue; // 非凸顶点
					boolean ear = true;
					for (int m : idx) {
						if (m == a || m == b || m == c) continue;
						if (pointInTri(xs.get(m), ys.get(m), ax, ay, bx, by, cx, cy)) { ear = false; break; }
					}
					if (ear) {
						tris.add(new float[] { (float) ax, (float) ay, (float) bx, (float) by, (float) cx, (float) cy });
						idx.remove((k + 1) % idx.size());
						clipped = true;
						break;
					}
				}
				if (!clipped) break;
			}
			// 剩余顶点（退化情况）扇形剖分兜底
			if (idx.size() >= 3) {
				int a = idx.get(0);
				for (int k = 1; k + 1 < idx.size(); k++) {
					tris.add(new float[] {
						(float) (double) xs.get(a), (float) (double) ys.get(a),
						(float) (double) xs.get(idx.get(k)), (float) (double) ys.get(idx.get(k)),
						(float) (double) xs.get(idx.get(k + 1)), (float) (double) ys.get(idx.get(k + 1))
					});
				}
			}
		}

		private static boolean pointInTri(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
			double d1 = sign(px, py, ax, ay, bx, by);
			double d2 = sign(px, py, bx, by, cx, cy);
			double d3 = sign(px, py, cx, cy, ax, ay);
			boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
			boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
			return !(neg && pos);
		}
		private static double sign(double px, double py, double ax, double ay, double bx, double by) {
			return (px - bx) * (ay - by) - (ax - bx) * (py - by);
		}

		public int getTriangleCount() { return tris.size(); }
		public float[] getTrianglePoint(int tri, int i) {
			float[] t = tris.get(tri);
			return new float[] { t[i * 2], t[i * 2 + 1] };
		}
	}
}
