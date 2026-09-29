package com.zarkonnen.catengine.lwjgl3;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * OGG 流式解码器（阶段 4 收尾）：与 OggDecoder 相同的 jogg+jorbis 底层合成，
 * 但以拉取方式按块产出 16-bit 交织 PCM，供 OpenAL 流式音乐队列按需取用，
 * 避免整曲解码缓冲占用大量内存。
 */
public final class OggStream implements Closeable {
	private final InputStream in;
	private int channels;
	private int rate;
	private boolean decodeReady;
	private boolean eof;

	private final SyncState oy = new SyncState();
	private final StreamState os = new StreamState();
	private final Page og = new Page();
	private final Packet op = new Packet();
	private final Info vi = new Info();
	private final Comment vc = new Comment();
	private final DspState vd = new DspState();
	private Block vb;
	private boolean streamInit;
	private int headerCount;

	private static final int CONV = 4096;
	private final short[] convbuffer = new short[CONV * 2];
	private float[][][] pcm = new float[1][8][CONV];
	private final int[] indexOut = new int[8];

	// PCM 暂存
	private short[] stash = new short[1 << 18];
	private int stashPos;
	private int stashLen;

	private OggStream(InputStream in) throws IOException {
		this.in = in;
		oy.init();
		vi.init();
		vc.init();
	}

	public static OggStream open(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			OggStream s = new OggStream(in);
			// 预解析 Vorbis 头部：getChannels()/getRate() 在首次 read() 前也必须有效
			// （后台解码线程会在读取前快照这两个值，若为 0 会导致 alBufferData
			// 收到非法格式/频率 → AL_INVALID_VALUE → 音乐无声）
			while (!s.decodeReady && !s.eof) {
				s.pump();
			}
			if (!s.decodeReady) {
				s.close();
				throw new IOException("No Vorbis stream found in " + file.getName());
			}
			return s;
		} catch (IOException e) {
			try { in.close(); } catch (IOException ignore) {}
			throw e;
		}
	}

	public int getChannels() { return channels; }
	public int getRate() { return rate; }

	/**
	 * 读取交织 16-bit PCM。返回写入的 short 数量；流结束时返回 -1。
	 */
	public int read(short[] buf) throws IOException {
		return read(buf, 0, buf.length);
	}

	/**
	 * 读取交织 16-bit PCM 到 buf[off, off+len)。返回写入的 short 数量；流结束时返回 -1。
	 */
	public int read(short[] buf, int off, int len) throws IOException {
		while (stashPos >= stashLen && !eof) {
			pump();
		}
		if (stashPos >= stashLen && eof) {
			return -1;
		}
		int n = Math.min(len, stashLen - stashPos);
		System.arraycopy(stash, stashPos, buf, off, n);
		stashPos += n;
		return n;
	}

	/** 驱动解码循环：读入更多文件数据，合成 PCM 追加到暂存。 */
	private void pump() throws IOException {
		if (eof) return;
		// 仅在暂存已被读空时复位（open() 的头部预解析可能连续 pump，
		// 若每次 pump 都复位会丢弃前一次已解码的音频）
		if (stashPos >= stashLen) {
			stashPos = 0;
			stashLen = 0;
		}
		int chunk = 65536;
		int bytes = oy.buffer(chunk);
		int r = in.read(oy.data, bytes, chunk);
		if (r <= 0) {
			eof = true;
			return;
		}
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
						vb = new Block(vd);
						pcm = new float[1][channels][CONV];
						decodeReady = true;
					}
				} else {
					if (vb.synthesis(op) == 0) {
						vd.synthesis_blockin(vb);
					}
					int count;
					// synthesis_pcmout 把每个通道有效样本的起始偏移写入 indexOut[ch]
					// （= pcm_returned，首个块之后不为 0），必须从该偏移读取，
					// 否则会重复旧样本导致"电音感"失真。
					while ((count = vd.synthesis_pcmout(pcm, indexOut)) > 0) {
						ensureStash(count * channels);
						for (int i = 0; i < count; i++) {
							for (int ch = 0; ch < channels; ch++) {
								float v = pcm[0][ch][indexOut[ch] + i];
								if (v > 1.0f) v = 1.0f;
								if (v < -1.0f) v = -1.0f;
								stash[stashLen++] = (short) (v * 32767f);
							}
						}
						vd.synthesis_read(count);
					}
				}
			}
		}
		if (og.eos() != 0) {
			eof = true;
		}
	}

	private void ensureStash(int need) {
		if (stashLen + need <= stash.length) return;
		int newLen = stash.length;
		while (newLen < stashLen + need) newLen *= 2;
		short[] ns = new short[newLen];
		System.arraycopy(stash, 0, ns, 0, stashLen);
		stash = ns;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
