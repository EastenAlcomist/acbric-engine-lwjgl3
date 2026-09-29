package com.zarkonnen.airships;

import java.util.HashMap;
import org.lwjgl.opengl.GL;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.pbuffer.FBOGraphics;
import org.newdawn.slick.util.Log;

/**
 * qqDPS This has been copied and modified to deal with Pbuffer.getCapabilities() causing
 * driver crashes on some Windows machines, so it just doesn't use pbuffers.
 * 
 * A factory to produce an appropriate render to texture graphics context based on current
 * hardware
 *
 * @author kevin
 */
public strictfp class FBOGraphicsFactory {
	/** The graphics list of graphics contexts created */
	private static HashMap graphics = new HashMap();
	/** True if fbo are supported */
	private static boolean fbo = true;
	/** True if we've initialised */
	private static boolean init = false;
	
	/**
	 * Initialise offscreen rendering by checking what buffers are supported
	 * by the card
	 * 
	 * @return Whether buffers are supported.
	 */
	public static boolean init() {
		if (init) { return fbo; }
		init = true;
		
		if (fbo) {
			fbo = GL.getCapabilities().GL_EXT_framebuffer_object;
		}
		
		Log.info("Offscreen Buffers FBO="+fbo+" PBUFFER=disabled PBUFFERRT=disabled");
		return fbo;
	}
	
	/**
	 * Force FBO use on or off
	 * 
	 * @param useFBO True if we should try and use FBO for offscreen images
	 */
	public static void setUseFBO(boolean useFBO) {
		fbo = useFBO;
	}
	
	/**
	 * Check if we're using FBO for dynamic textures
	 * 
	 * @return True if we're using FBOs
	 */
	public static boolean usingFBO() {
		return fbo;
	}

	/**
	 * Check if we're using PBuffer for dynamic textures
	 * 
	 * @return True if we're using PBuffer
	 */
	public static boolean usingPBuffer() {
		return false;
	}
	
	/**
	 * Get a graphics context for a particular image
	 * 
	 * @param image The image for which to retrieve the graphics context
	 * @return The graphics context
	 * @throws SlickException Indicates it wasn't possible to create a graphics context
	 * given available hardware.
	 */
	public static Graphics getGraphicsForImage(Image image) throws SlickException {
		Graphics g = (Graphics) graphics.get(image.getTexture());

		if (g == null) {
			g = createGraphics(image);
			graphics.put(image.getTexture(), g);
		}

		// 每次获取时重新绑定 FBO（防止被主渲染的默认帧缓冲解绑后丢失）
		if (g instanceof FBOGraphics) {
			((FBOGraphics) g).bind();
		}

		return g;
	}
	
	/**
	 * Release any graphics context that is assocaited with the given image
	 * 
	 * @param image The image to release
	 * @throws SlickException Indicates a failure to release the context
	 */
	public static void releaseGraphicsForImage(Image image) throws SlickException {
		Graphics g = (Graphics) graphics.remove(image.getTexture());
		
		if (g != null) {
			g.destroy();
		}
	}
	
	/** 
	 * Create an underlying graphics context for the given image
	 * 
	 * @param image The image we want to render to
	 * @return The graphics context created
	 * @throws SlickException
	 */
	private static Graphics createGraphics(Image image) throws SlickException {
		init();
		
		if (fbo) {
			try {
				return new FBOGraphics(image);
			} catch (Exception e) {
				fbo = false;
				Log.warn("FBO failed in use.");
			}
		}
		
		throw new SlickException("Failed to create offscreen buffer.");
	}
}
