package org.newdawn.slick.util;

/** Slick2D 日志兼容层（最小实现）。 */
public final class Log {
	private Log() {}

	public static void info(String message) { System.out.println("[INFO] " + message); }
	public static void warn(String message) { System.out.println("[WARN] " + message); }
	public static void warn(String message, Throwable e) { System.out.println("[WARN] " + message); e.printStackTrace(); }
	public static void error(String message) { System.err.println("[ERROR] " + message); }
	public static void error(String message, Throwable e) { System.err.println("[ERROR] " + message); e.printStackTrace(); }
	public static void error(Throwable e) { e.printStackTrace(); }
	public static void debug(String message) { System.out.println("[DEBUG] " + message); }
	public static void setVerbose(boolean v) {}
}
