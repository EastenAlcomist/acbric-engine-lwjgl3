package com.zarkonnen.catengine.lwjgl3;

import static org.lwjgl.openal.AL10.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

/**
 * OpenAL 音频后端（LWJGL3）。
 * 声音文件按 name 在 loadBases 下解析为 name.ogg，用 jorbis 解码后以缓冲形式播放。
 * 语义对齐 Slick2D：play/loop 立即播放，playMusic 为循环 BGM（循环时回调 onLoop）。
 * 若 OpenAL 初始化失败，则静默降级为无音频（所有方法空操作）。
 */
public final class OpenAlAudio {
	private boolean initialized;
	private boolean failed;
	private final int[] sources;
	private int nextSource;
	private final HashMap<String, Integer> bufferCache = new HashMap<>();
	private final Set<String> reportedMisses = ConcurrentHashMap.newKeySet();   // 主线程与解码线程并发访问
	private List<File> loadBases;
	private float soundZ;

	// 音乐
	private int musicSource = -1;
	private int musicBuffer = -1;
	private float musicVolume = 1f;
	private boolean musicPlaying;
	private Runnable musicOnLoop;
	private String currentMusic;
	// 淡出
	private int fadeTotalMs;
	private int fadeLeftMs;
	private float fadeStartVol;
	// 流式音乐：解码在后台线程进行，主线程 tick 只做 OpenAL 队列操作
	// （避免每 0.7 秒一次的主线程解码尖峰造成画面卡顿）
	private int[] musicStreamBufs;
	private int musicBufsQueued;
	private boolean musicStreamEof;
	private boolean musicStreaming;
	private ShortBuffer musicBufNative;
	private boolean firstPlayLogged;
	private boolean firstMusicQueued;
	private final Object musicDecodeLock = new Object();
	private final ArrayDeque<short[]> musicDecoded = new ArrayDeque<>();
	private final ArrayDeque<Integer> musicFreeBufs = new ArrayDeque<>();
	private volatile String musicDecodeTrack;
	private volatile int musicChannels = 2;
	private volatile int musicRate = 48000;
	private Thread musicDecodeThread;
	private static final int MUSIC_BUF_SHORTS = 65536;   // 每块 128KB 字节
	private static final int MUSIC_BUF_COUNT = 6;        // 队列上限：约 4 秒缓冲
	private static final int MUSIC_DECODED_MAX = 8;      // 解码队列上限（背压）

	public OpenAlAudio(int maxSources) {
		this.sources = new int[Math.max(4, maxSources)];
	}

	public boolean isAvailable() { return initialized && !failed; }

	public void init() {
		if (initialized || failed) return;
		try {
			// 直接尝试打开默认设备；失败则枚举并尝试第一个可用设备（OpenAL-Soft）。
			// 不做任何前置字符串预检：无设备时 alcOpenDevice 返回 0 即可安全降级，
			// 预检（alcGetString(0, ...)）在部分平台/驱动上可能抛异常导致静默无声。
			long device = ALC10.alcOpenDevice((ByteBuffer) null);
			if (device == 0) {
				String first = enumerateFirstDevice();
				if (first != null) {
					try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
						ByteBuffer nameBuf = stack.UTF8(first);
						device = ALC10.alcOpenDevice(nameBuf);
					}
				}
			}
			if (device == 0) {
				System.err.println("OpenAL: no audio output device found; audio disabled");
				failed = true;
				return;
			}
			System.err.println("OpenAL device: " + ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER));
			ALCCapabilities deviceCaps = ALC.createCapabilities(device);
			long context = ALC10.alcCreateContext(device, (IntBuffer) null);
			if (context == 0) {
				System.err.println("OpenAL: alcCreateContext failed; audio disabled");
				ALC10.alcCloseDevice(device);
				failed = true;
				return;
			}
			ALC10.alcMakeContextCurrent(context);
			AL.createCapabilities(deviceCaps);
			for (int i = 0; i < sources.length; i++) {
				sources[i] = alGenSources();
				alSourcei(sources[i], AL_ROLLOFF_FACTOR, 0); // 无距离衰减，保留游戏设定的音量
			}
			musicSource = alGenSources();
			alSourcei(musicSource, AL_ROLLOFF_FACTOR, 0);
			alListener3f(AL_POSITION, 0, 0, 0);
			initialized = true;
			System.err.println("OpenAL ready (sources=" + sources.length + "), audio enabled");
		} catch (Throwable t) {
			failed = true;
			System.err.println("OpenAL init failed; audio disabled: " + t);
			t.printStackTrace();
		}
	}

	/** 枚举第一个可用输出设备名（需 ALC_ENUMERATION_EXT，OpenAL-Soft 默认支持）。 */
	private static String enumerateFirstDevice() {
		try {
			if (!ALC10.alcIsExtensionPresent(0, "ALC_ENUMERATION_EXT")) return null;
			String list = ALC10.alcGetString(0, ALC10.ALC_DEVICE_SPECIFIER);
			if (list == null || list.isEmpty()) return null;
			int end = list.indexOf('\0');
			String first = end >= 0 ? list.substring(0, end) : list;
			return first.isEmpty() ? null : first;
		} catch (Throwable t) {
			System.err.println("OpenAL device enumeration failed: " + t);
			return null;
		}
	}

	public void setLoadBases(List<File> bases) {
		// 保存列表引用而非快照：游戏在引擎 setup 之后才 addSoundLoadBase
		// （数据加载阶段），快照会导致 resolve 永远找不到声音文件 → 完全静默。
		this.loadBases = bases;
	}
	public void setSoundZ(float z) { soundZ = z; }

	private File resolve(String name) {
		String fn = name.contains(".") ? name : name + ".ogg";
		if (loadBases != null) {
			for (File base : loadBases) {
				File f = new File(base, fn);
				if (f.exists()) return f;
			}
		}
		if (reportedMisses.add(name)) {
			System.err.println("Audio file not found: " + name + " (bases=" + loadBases + ")");
		}
		return null;
	}

	private int getBuffer(String name) {
		Integer cached = bufferCache.get(name);
		if (cached != null) return cached;
		File f = resolve(name);
		if (f == null) return 0;
		try {
			OggDecoder.Decoded d = OggDecoder.decode(f);
			int format = d.channels == 2 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16;
			int buf = alGenBuffers();
			// 必须用本机字节序缓冲（LWJGL3 直接传地址，不做字节序转换；
			// ShortBuffer.wrap 是固定大端序，在 x86 上会把采样字节交换成噪音）
			ShortBuffer nb = BufferUtils.createShortBuffer(d.pcm.length);
			nb.put(d.pcm).flip();
			alBufferData(buf, format, nb, d.rate);
			checkError("load audio " + name);
			bufferCache.put(name, buf);
			return buf;
		} catch (IOException | RuntimeException e) {
			System.err.println("Unable to load audio " + name + ": " + e);
			return 0;
		}
	}

	private static void checkError(String op) {
		int err = alGetError();
		if (err != AL_NO_ERROR) {
			System.err.println("OpenAL error after " + op + ": 0x" + Integer.toHexString(err));
		}
	}

	/** 预加载音效。 */
	public void preload(String name) { getBuffer(name); }

	/** 预检音乐文件存在性（音乐为流式按需解码，不做整曲缓冲）。 */
	public void preloadMusic(String name) { resolve(name); }

	/** 播放一次性音效。 */
	public void play(String name, float pitch, float volume, float x, float y) {
		if (!isAvailable()) return;
		int buf = getBuffer(name);
		if (buf == 0) return;
		int src = sources[nextSource];
		nextSource = (nextSource + 1) % sources.length;
		alSourceStop(src);
		alSourcei(src, AL_BUFFER, buf);
		alSourcef(src, AL_PITCH, pitch);
		alSourcef(src, AL_GAIN, volume);
		alSource3f(src, AL_POSITION, x, y, soundZ);
		alSourcei(src, AL_LOOPING, AL_FALSE);
		alSourcePlay(src);
		if (!firstPlayLogged) {
			firstPlayLogged = true;
			System.err.println("First sound play: name=" + name + " buf=" + buf + " src=" + src
				+ " volume=" + volume + " state=" + alGetSourcei(src, AL_SOURCE_STATE));
		}
		checkError("play " + name);
	}

	/** 播放循环音效，返回 source 句柄供外部控制。 */
	public int loop(String name, float pitch, float volume, float x, float y) {
		if (!isAvailable()) return -1;
		int buf = getBuffer(name);
		if (buf == 0) return -1;
		int src = alGenSources();
		alSourcei(src, AL_BUFFER, buf);
		alSourcef(src, AL_PITCH, pitch);
		alSourcef(src, AL_GAIN, volume);
		alSource3f(src, AL_POSITION, x, y, soundZ);
		alSourcei(src, AL_LOOPING, AL_TRUE);
		alSourcePlay(src);
		return src;
	}

	// ---- 音乐 ----
	public void playMusic(String name, float volume, Runnable onLoop) {
		if (!isAvailable()) return;
		alSourceStop(musicSource);   // 先停源，再清理上一首（stopMusicStream 需要安全的队列状态）
		// 摘掉可能的静态缓冲（整缓冲回退路径会挂 AL_BUFFER；静态与流式队列互斥，
		// 否则 alSourceQueueBuffers 会以 AL_INVALID_OPERATION 失败 → 音乐无声）
		alSourcei(musicSource, AL_BUFFER, 0);
		stopMusicStream();
		musicBuffer = -1;
		File f = resolve(name);
		if (f != null) {
			// 流式路径：缓冲池 + 后台解码线程
			musicStreaming = true;
			musicStreamEof = false;
			musicBufsQueued = 0;
			firstMusicQueued = false;
			musicStreamBufs = new int[MUSIC_BUF_COUNT];
			synchronized (musicDecodeLock) {
				musicFreeBufs.clear();
				musicDecoded.clear();
				for (int i = 0; i < MUSIC_BUF_COUNT; i++) {
					musicStreamBufs[i] = alGenBuffers();
					musicFreeBufs.addLast(musicStreamBufs[i]);
				}
			}
			musicVolume = volume;
			musicOnLoop = onLoop;
			currentMusic = name;
			alSourcei(musicSource, AL_LOOPING, AL_FALSE);
			alSourcef(musicSource, AL_PITCH, 1f);
			alSourcef(musicSource, AL_GAIN, volume);
			musicPlaying = true;
			fadeTotalMs = 0;
			fadeLeftMs = 0;
			startMusicDecode(name);
			System.err.println("Music streaming started: " + name + " (decode on background thread)");
			return;
		}
		// 整缓冲回退（原行为：小文件或流打开失败时）
		int buf = getBuffer(name);
		if (buf == 0) return;
		musicBuffer = buf;
		musicVolume = volume;
		musicOnLoop = onLoop;
		currentMusic = name;
		alSourcei(musicSource, AL_BUFFER, buf);
		alSourcef(musicSource, AL_PITCH, 1f);
		alSourcef(musicSource, AL_GAIN, volume);
		alSourcei(musicSource, AL_LOOPING, AL_FALSE); // 循环由 tick 检测并回调
		alSourcePlay(musicSource);
		musicPlaying = true;
		fadeTotalMs = 0;
		fadeLeftMs = 0;
		checkError("playMusic buffered " + name);
	}

	// ---- 后台音乐解码 ----
	private void startMusicDecode(String name) {
		synchronized (musicDecodeLock) {
			musicDecodeTrack = name;
			musicDecodeLock.notifyAll();
		}
		if (musicDecodeThread == null || !musicDecodeThread.isAlive()) {
			musicDecodeThread = new Thread(this::musicDecodeLoop, "music-decode");
			musicDecodeThread.setDaemon(true);
			musicDecodeThread.start();
		}
	}

	private void musicDecodeLoop() {
		while (initialized) {
			String track = musicDecodeTrack;
			if (track == null) {
				sleepQuiet(50);
				continue;
			}
			// 捕获一切异常：解码线程不能静默死亡（否则音乐彻底无声）
			try {
				decodeTrack(track);
			} catch (Throwable t) {
				System.err.println("Music decode thread error (continuing): " + t);
				t.printStackTrace();
				musicStreamEof = true;
				sleepQuiet(200);
			}
		}
	}

	private void decodeTrack(String track) {
		File f = resolve(track);
		if (f == null) {
			// 找不到文件：视作曲终
			System.err.println("Music file not found: " + track);
			musicStreamEof = true;
			musicDecodeTrack = null;
			return;
		}
		System.err.println("Music decode started: " + track);
		try (OggStream s = OggStream.open(f)) {
			musicChannels = s.getChannels();
			musicRate = s.getRate();
			while (initialized && track.equals(musicDecodeTrack)) {
				short[] chunk = new short[MUSIC_BUF_SHORTS];
				int got = 0;
				while (got < chunk.length) {
					int n = s.read(chunk, got, chunk.length - got);
					if (n < 0) break;
					got += n;
				}
				if (got == 0) break;   // EOF
				short[] out = got == chunk.length ? chunk : java.util.Arrays.copyOf(chunk, got);
				synchronized (musicDecodeLock) {
					while (musicDecoded.size() >= MUSIC_DECODED_MAX
							&& initialized && track.equals(musicDecodeTrack)) {
						try { musicDecodeLock.wait(50); } catch (InterruptedException ie) { return; }
					}
					if (!initialized || !track.equals(musicDecodeTrack)) break;
					musicDecoded.addLast(out);
					musicDecodeLock.notifyAll();
				}
			}
		} catch (IOException e) {
			System.err.println("Music decode failed: " + e);
		}
		if (initialized && track.equals(musicDecodeTrack)) {
			musicStreamEof = true;
			// 等待主线程处理（回调后切歌或重开），轮询即可
			while (initialized && musicStreamEof && track.equals(musicDecodeTrack)) {
				sleepQuiet(50);
			}
		}
	}

	private static void sleepQuiet(long ms) {
		try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
	}

	/** 从解码队列取一块上传到 OpenAL 缓冲并入队（主线程）。 */
	private boolean queueDecodedChunk() {
		int b;
		short[] chunk;
		synchronized (musicDecodeLock) {
			if (musicFreeBufs.isEmpty() || musicDecoded.isEmpty()) return false;
			b = musicFreeBufs.removeFirst();
			chunk = musicDecoded.removeFirst();
			musicDecodeLock.notifyAll();
		}
		if (musicBufNative == null || musicBufNative.capacity() < chunk.length) {
			musicBufNative = BufferUtils.createShortBuffer(Math.max(MUSIC_BUF_SHORTS, chunk.length));
		}
		if (musicChannels <= 0 || musicRate <= 0) {
			// 防御：头部未就绪时跳过（正常不应发生，OggStream.open 已预解析头部）
			System.err.println("Music chunk skipped: invalid format ch=" + musicChannels + " rate=" + musicRate);
			return false;
		}
		musicBufNative.clear();
		musicBufNative.put(chunk);
		musicBufNative.flip();
		alBufferData(b, musicChannels == 2 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16, musicBufNative, musicRate);
		alSourceQueueBuffers(musicSource, b);
		musicBufsQueued++;
		if (!firstMusicQueued) {
			firstMusicQueued = true;
			System.err.println("Music first chunk queued: ch=" + musicChannels + " rate=" + musicRate);
		}
		return true;
	}

	public void stopMusic() {
		if (!isAvailable()) return;
		if (musicSource >= 0) alSourceStop(musicSource);
		stopMusicStream();
		musicBuffer = -1;
		musicPlaying = false;
		currentMusic = null;
		fadeTotalMs = 0;
		fadeLeftMs = 0;
	}

	/** 清理流式音乐资源（先停源、摘队列、再删缓冲，避免在播放中的源上删除排队缓冲导致驱动崩溃）。 */
	private void stopMusicStream() {
		synchronized (musicDecodeLock) {
			musicDecodeTrack = null;
			musicDecoded.clear();
			musicFreeBufs.clear();
			musicDecodeLock.notifyAll();
		}
		if (musicStreamBufs != null) {
			if (musicSource >= 0) {
				alSourceStop(musicSource);
				int queued = alGetSourcei(musicSource, AL_BUFFERS_QUEUED);
				while (queued-- > 0) {
					alSourceUnqueueBuffers(musicSource);
				}
			}
			for (int b : musicStreamBufs) {
				if (b != 0) alDeleteBuffers(b);
			}
			musicStreamBufs = null;
		}
		musicStreaming = false;
		musicBufsQueued = 0;
		musicStreamEof = false;
	}

	public void fadeOutMusic(int ms) {
		if (!isAvailable() || !musicPlaying) return;
		fadeTotalMs = ms;
		fadeLeftMs = ms;
		fadeStartVol = alGetSourcef(musicSource, AL_GAIN);
	}

	/** 每帧调用：处理流式补块、音乐循环回调与淡出。 */
	public void tick(int msDelta) {
		if (!isAvailable() || !musicPlaying) return;
		try {
			tickInner(msDelta);
		} catch (Throwable t) {
			System.err.println("Music tick failed (music stopped): " + t);
			t.printStackTrace();
			try {
				alSourceStop(musicSource);
				stopMusicStream();
			} catch (Throwable ignore) {}
			musicPlaying = false;
			currentMusic = null;
		}
	}

	private void tickInner(int msDelta) {
		if (!isAvailable() || !musicPlaying) return;
		// 淡出
		if (fadeLeftMs > 0) {
			fadeLeftMs -= msDelta;
			if (fadeLeftMs <= 0) {
				fadeLeftMs = 0;
				alSourceStop(musicSource);
				stopMusicStream();
				musicPlaying = false;
				currentMusic = null;
				fadeTotalMs = 0;
				return;
			}
			float t = (float) fadeLeftMs / Math.max(1, fadeTotalMs);
			alSourcef(musicSource, AL_GAIN, fadeStartVol * t);
			return;
		}
		if (musicStreaming) {
			// 回收已播完的缓冲（回到空闲池）
			int processed = alGetSourcei(musicSource, AL_BUFFERS_PROCESSED);
			while (processed-- > 0) {
				int b = alSourceUnqueueBuffers(musicSource);
				musicBufsQueued--;
				synchronized (musicDecodeLock) {
					if (b != 0) {
						musicFreeBufs.addLast(b);
					}
					musicDecodeLock.notifyAll();
				}
			}
			// 从后台解码队列取块入队（纯上传，无解码）
			while (musicBufsQueued < MUSIC_BUF_COUNT && queueDecodedChunk()) {
				// 队列填满为止
			}
			int state = alGetSourcei(musicSource, AL_SOURCE_STATE);
			if (musicBufsQueued > 0 && state != AL_PLAYING) {
				alSourcePlay(musicSource);
				state = alGetSourcei(musicSource, AL_SOURCE_STATE);
			}
			// 曲终：解码队列已空、缓冲全部播完 → 回调（游戏在回调里切歌）
			if (musicStreamEof && musicBufsQueued == 0 && state == AL_STOPPED) {
				boolean decodedEmpty;
				synchronized (musicDecodeLock) {
					decodedEmpty = musicDecoded.isEmpty();
				}
				if (decodedEmpty) {
					Runnable cb = musicOnLoop;
					musicStreamEof = false;   // 唤醒解码线程（若曲目未变则重开本曲）
					if (cb != null) cb.run();
				}
			}
			checkError("music stream tick");
			return;
		}
		// 整缓冲路径：播放结束 → 循环回调并重播
		if (alGetSourcei(musicSource, AL_SOURCE_STATE) == AL_STOPPED) {
			Runnable cb = musicOnLoop;
			alSourcePlay(musicSource);
			if (cb != null) cb.run();
		}
	}

	// ---- 循环音效句柄控制 ----
	public void loopStop(int src) {
		if (src < 0 || !isAvailable()) return;
		alSourceStop(src);
		alDeleteSources(src);
	}
	public void loopSetLocation(int src, float x, float y) {
		if (src < 0 || !isAvailable()) return;
		alSource3f(src, AL_POSITION, x, y, soundZ);
	}
	public void loopSetPitch(int src, float p) {
		if (src < 0 || !isAvailable()) return;
		alSourcef(src, AL_PITCH, p);
	}
	public void loopSetVolume(int src, float v) {
		if (src < 0 || !isAvailable()) return;
		alSourcef(src, AL_GAIN, v);
	}

	public void destroy() {
		if (!initialized) return;
		if (musicSource >= 0) alSourceStop(musicSource);
		stopMusicStream();
		for (int s : sources) alDeleteSources(s);
		if (musicSource >= 0) alDeleteSources(musicSource);
		for (int b : bufferCache.values()) alDeleteBuffers(b);
		bufferCache.clear();
		// 先取 device，再销毁 context（销毁后 alcGetCurrentContext 为 0，无法再取 device）
		long context = ALC10.alcGetCurrentContext();
		long device = ALC10.alcGetContextsDevice(context);
		if (context != 0) ALC10.alcDestroyContext(context);
		if (device != 0) ALC10.alcCloseDevice(device);
		initialized = false;
	}
}
