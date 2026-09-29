package com.zarkonnen.catengine.lwjgl3;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * OGG → 16-bit PCM 解码器（jogg + jorbis 底层解码，纯 Java）。
 * 与 Slick2D 原版一致，用 jogg 解析容器页、jorbis 做 Vorbis 合成。
 */
public final class OggDecoder {
	private OggDecoder() {}

	/** 解码结果：交织（interleaved）16-bit PCM。 */
	public static final class Decoded {
		public final short[] pcm;
		public final int channels;
		public final int rate;
		public Decoded(short[] pcm, int channels, int rate) {
			this.pcm = pcm;
			this.channels = channels;
			this.rate = rate;
		}
	}

	public static Decoded decode(File file) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			return decode(in);
		}
	}

	public static Decoded decode(InputStream in) throws IOException {
		SyncState oy = new SyncState();
		StreamState os = new StreamState();
		Page og = new Page();
		Packet op = new Packet();
		Info vi = new Info();
		Comment vc = new Comment();
		DspState vd = new DspState();
		Block vb = new Block(vd);

		oy.init();
		vi.init();
		vc.init();

		int channels = 0;
		int rate = 0;
		int headerCount = 0;
		boolean streamInit = false;
		boolean decodeReady = false;
		int eos = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int convsize = 4096;
		short[] convbuffer = new short[convsize * 2];
		float[][][] pcm = new float[1][8][convsize];
		// jorbis 会把 index[0..channels-1] 全部写入，必须按最大通道数分配
		int[] indexOut = new int[8];

		while (eos == 0) {
			int bytes = oy.buffer(4096);
			int r = in.read(oy.data, bytes, 4096);
			if (r <= 0) break;
			oy.wrote(r);

			int pageResult;
			while ((pageResult = oy.pageout(og)) > 0) {
				if (!streamInit) {
					os.init(og.serialno());
					streamInit = true;
				}
				os.pagein(og);
				while (true) {
					int result = os.packetout(op);
					if (result == 0) break;
					if (result == -1) continue;

					if (!decodeReady) {
						if (vi.synthesis_headerin(vc, op) != 0) {
							// 可能是垃圾页，忽略并继续同步
							continue;
						}
						headerCount++;
						if (headerCount >= 3) {
							channels = vi.channels;
							rate = vi.rate;
							if (channels < 1 || channels > 8) {
								throw new IOException("Unsupported channel count: " + channels);
							}
							vd.synthesis_init(vi);
							vb.init(vd);
							pcm = new float[1][channels][convsize];
							decodeReady = true;
						}
					} else {
						if (vb.synthesis(op) == 0) {
							vd.synthesis_blockin(vb);
						}
						int count;
						// 注意：synthesis_pcmout 把每个通道有效样本的起始偏移写入 indexOut[ch]
						// （= pcm_returned，首个块之后不为 0），必须从该偏移读取，否则会
						// 重复旧样本导致"电音感"失真。
						while ((count = vd.synthesis_pcmout(pcm, indexOut)) > 0) {
							int index = 0;
							for (int i = 0; i < count; i++) {
								for (int ch = 0; ch < channels; ch++) {
									float v = pcm[0][ch][indexOut[ch] + i];
									if (v > 1.0f) v = 1.0f;
									if (v < -1.0f) v = -1.0f;
									convbuffer[index++] = (short) (v * 32767f);
								}
							}
							out.write(toBytes(convbuffer, index));
							vd.synthesis_read(count);
						}
					}
				}
			}
			if (pageResult < 0) {
				// 同步失败，继续读取更多数据重试
				continue;
			}
			if (og.eos() != 0) eos = 1;
		}

		byte[] bytes = out.toByteArray();
		short[] pcmData = new short[bytes.length / 2];
		for (int i = 0; i < pcmData.length; i++) {
			pcmData[i] = (short) ((bytes[i * 2] & 0xFF) | ((bytes[i * 2 + 1] & 0xFF) << 8));
		}
		return new Decoded(pcmData, channels, rate);
	}

	private static byte[] toBytes(short[] samples, int count) {
		byte[] out = new byte[count * 2];
		for (int i = 0; i < count; i++) {
			short s = samples[i];
			out[i * 2] = (byte) (s & 0xFF);
			out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
		}
		return out;
	}
}
