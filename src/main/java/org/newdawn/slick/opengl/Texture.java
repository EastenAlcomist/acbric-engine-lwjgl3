package org.newdawn.slick.opengl;

/** Slick2D Texture 接口兼容层。 */
public interface Texture {
	boolean hasAlpha();
	String getTextureRef();
	void bind();
	int getImageHeight();
	int getImageWidth();
	float getHeight();
	float getWidth();
	int getTextureHeight();
	int getTextureWidth();
	void release();
	int getTextureID();
	byte[] getTextureData();
	void setTextureFilter(int filter);
}
