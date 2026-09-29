package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;

import java.util.ArrayList;
import java.util.HashSet;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBindTexture;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glTexCoord2d;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glVertex2d;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;

public strictfp class BeamLayer implements UniScreen.VisualLayer {
	@Override
	public void tick(Input in, int ms, UniScreen us) {}
	
	private final ArrayList<String> additionalSSBs = new ArrayList<String>();

	@Override
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH) {
		if (us.combat == null) { return; }
		int[] maxSubBeams = new int[] { 1 };
		for (int i = 0; i < maxSubBeams[0]; i++) {
			additionalSSBs.clear();
			draw(d, us, cropX, cropY, cropW, cropH, i, SpritesheetBundle.ofName("spritesheet"), additionalSSBs, maxSubBeams);
			for (int j = 0; j < additionalSSBs.size(); j++) {
				draw(d, us, cropX, cropY, cropW, cropH, i, SpritesheetBundle.ofName(additionalSSBs.get(j)), null, maxSubBeams);
			}
		}
	}
	
	public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH, int sbi, SpritesheetBundle ssb, ArrayList<String> additionalSSBs, int[] maxSubBeams) {
		try {
			GLCompat.glEnable(GLCompat.GL_TEXTURE_2D);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GLCompat.GL_TEXTURE_2D, ssb.getTex("").getTextureID());
			glBegin(GLCompat.GL_QUADS);
			
			for (int si = 0; si < us.combat.sides.size(); si++) {
				Combat.Side side  = us.combat.sides.get(si);
				for (int ai = 0; ai < side.ships.size(); ai++) {
					Airship ship = side.ships.get(ai);
					for (int mi = 0; mi < ship.modules.size(); mi++) {
						Module m = ship.modules.get(mi);
						if (m.firingBeam) {
							BeamSpec bs = m.type.beamSpec(ship.currentBonuses);
							maxSubBeams[0] = Math.max(maxSubBeams[0], bs.subBeams.size());
							if (sbi < bs.subBeams.size()) {
								BeamSpec.SubBeam sb = bs.subBeams.get(sbi);
								if (!sb.texture.src.equals(ssb.name)) {
									if (additionalSSBs != null && !additionalSSBs.contains(sb.texture.src)) {
										additionalSSBs.add(sb.texture.src);
									}
									continue;
								}
								double beamTimeProportion = m.beamAge * 1.0 / (bs.fadeInTime + bs.steadyTime + bs.fadeOutTime);
								double endX = m.beamStartPositionX * (1 - beamTimeProportion) + m.beamEndPositionX * beamTimeProportion;
								double endY = m.beamStartPositionY * (1 - beamTimeProportion) + m.beamEndPositionY * beamTimeProportion;
								Pt muz = m.currentMuzzle();
								double mx = muz.x;
								double my = muz.y;
								double baseIntensity = 1;
								if (m.beamAge < bs.fadeInTime) {
									baseIntensity = m.beamAge * 1.0 / bs.fadeInTime;
								} else if (m.beamAge > bs.fadeInTime + bs.steadyTime && bs.fadeOutTime > 0) {
									baseIntensity = 1 - ((m.beamAge - bs.fadeInTime - bs.steadyTime) * 1.0 / bs.fadeOutTime);
								}
								double flicker = (m.subBeamFlickers == null || m.subBeamFlickers.length <= sbi) ? 0 : m.subBeamFlickers[sbi];
								double intensity = baseIntensity * (1 + flicker);
								double r = sb.width * (1 - sb.widthFade) + sb.width * intensity * sb.widthFade;
								double a = Math.atan2(endY - my, endX - mx);

								float opacity = (float) (sb.clr.a * (1 - sb.opacityFade) + sb.clr.a * intensity * sb.opacityFade);
								glColor4f(sb.clr.r / 255f, sb.clr.g / 255f, sb.clr.b / 255f, opacity / 255f);
								
								// Figure out number of segments
								double beamDist = Math.sqrt((endX - mx) * (endX - mx) + (endY - my) * (endY - my));
								//System.out.println("beam r " + r + " op " + opacity + " dist " + beamDist + " mx " + mx + " my " + my + " endX " + endX + " endY " + endY);
								if (beamDist < 1) {
									continue;
								}
								double numSegments = beamDist / sb.texture.srcWidth;
								double shiftPx = (m.beamAge * sb.textureSpeed);
								while (shiftPx < 0) {
									shiftPx += sb.texture.srcWidth;
								}
								shiftPx %= sb.texture.srcWidth;
								double shiftAlong = shiftPx / beamDist;

								for (int segI = -1; segI < numSegments; segI++) {
									float startAlong = (float) Math.max(0, segI / numSegments + shiftAlong);
									float segStartX = (float) (mx * (1 - startAlong) + endX * startAlong);
									float segStartY = (float) (my * (1 - startAlong) + endY * startAlong);
									float endAlong = (float) Math.min(1, (segI + 1) / numSegments + shiftAlong);
									float segEndX = (float) (mx * (1 - endAlong) + endX * endAlong);
									float segEndY = (float) (my * (1 - endAlong) + endY * endAlong);
									float texStart = (float) (segI == -1 ? (1 - shiftPx / sb.texture.srcWidth) : 0);
									float texEnd = (float) (endAlong - startAlong < 1 / numSegments - 0.001 ? (1 - shiftPx / sb.texture.srcWidth) : 1);
									if (startAlong >= endAlong) { continue; }
									glTexCoord2d((sb.texture.srcX + sb.texture.srcWidth * texStart) * 1f / ssb.size, sb.texture.srcY * 1f / ssb.size);
									glVertex2d((float) segStartX + r * Math.cos(a + Math.PI / 2), (float) segStartY + r * Math.sin(a + Math.PI / 2));
									glTexCoord2d((sb.texture.srcX + sb.texture.srcWidth * texEnd) * 1f / ssb.size, (sb.texture.srcY) * 1f / ssb.size);
									glVertex2d((float) segEndX + r * Math.cos(a + Math.PI / 2), (float) segEndY + r * Math.sin(a + Math.PI / 2));
									glTexCoord2d((sb.texture.srcX + sb.texture.srcWidth * texEnd) * 1f / ssb.size, (sb.texture.srcY + sb.texture.srcHeight) * 1f / ssb.size);
									glVertex2d((float) segEndX + r * Math.cos(a - Math.PI / 2), (float) segEndY + r * Math.sin(a - Math.PI / 2));
									glTexCoord2d((sb.texture.srcX + sb.texture.srcWidth * texStart) * 1f / ssb.size, (sb.texture.srcY + sb.texture.srcHeight) * 1f / ssb.size);
									glVertex2d((float) segStartX + r * Math.cos(a - Math.PI / 2), (float) segStartY + r * Math.sin(a - Math.PI / 2));
								}
							}
						}
					}
				}
			}
			
			glEnd();
			glColor3f(1.0f, 1.0f, 1.0f);
			TextureImpl.bindNone();
			GLCompat.glDisable(GLCompat.GL_TEXTURE_2D);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
