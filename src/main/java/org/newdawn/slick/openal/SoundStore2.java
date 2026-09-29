package org.newdawn.slick.openal;

/** Slick2D SoundStore2 兼容层（音频源计数，阶段 4 才真正实现音频）。 */
public class SoundStore2 {
	private static final SoundStore2 INSTANCE = new SoundStore2();

	public static SoundStore2 get() { return INSTANCE; }

	public int getUsedSources() { return 0; }
	public int getSourceCount() { return 0; }
}
