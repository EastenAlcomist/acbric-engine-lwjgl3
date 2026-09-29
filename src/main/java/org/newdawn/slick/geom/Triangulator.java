package org.newdawn.slick.geom;

/** 三角剖分结果接口。 */
public interface Triangulator {
	int getTriangleCount();
	float[] getTrianglePoint(int tri, int i);
}
