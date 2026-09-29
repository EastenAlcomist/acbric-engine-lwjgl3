package org.newdawn.slick.tiled;

/** Slick2D Base64 兼容层（仅实现游戏实际用到的方法）。 */
public class Base64 {
	private Base64() {}

	public static String encodeBytes(byte[] data) {
		return java.util.Base64.getEncoder().encodeToString(data);
	}
	public static byte[] decode(String s) {
		return java.util.Base64.getDecoder().decode(s);
	}
	public static byte[] decode(byte[] data) {
		return java.util.Base64.getDecoder().decode(data);
	}
}
