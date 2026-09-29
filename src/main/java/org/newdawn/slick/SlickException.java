package org.newdawn.slick;

/** Slick2D 异常兼容层。 */
public class SlickException extends Exception {
	public SlickException(String message) { super(message); }
	public SlickException(String message, Throwable e) { super(message, e); }
}
