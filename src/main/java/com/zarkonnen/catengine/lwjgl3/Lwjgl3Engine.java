package com.zarkonnen.catengine.lwjgl3;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.system.MemoryUtil.NULL;
import com.zarkonnen.catengine.Condition;
import com.zarkonnen.catengine.Engine;
import com.zarkonnen.catengine.ExceptionHandler;
import com.zarkonnen.catengine.Frame;
import com.zarkonnen.catengine.Game;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.Loop;
import com.zarkonnen.catengine.MusicCallback;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.newdawn.slick.Image;
import org.newdawn.slick.Graphics;

/**
 * LWJGL3 引擎后端，实现 CatEngine 的 Engine/Frame/Input。
 * 使用 GLFW + OpenGL 2.1 兼容上下文（保留固定管线与立即模式）。
 */
public class Lwjgl3Engine implements Engine {
	/** 报告处理器（对应 SlickEngine.ReportHandler）。 */
	public interface ReportHandler {
		void report(String message, Throwable t);
	}

	private long window;
	public static long glfwWindow;   // 供 Display shim 访问当前窗口句柄
	private long frameCount;
	private Game game;
	private ExceptionHandler eh;
	private MyFrame frame;
	private MyInput input;
	private int fps = 60;
	private boolean paceFrames;          // 目标帧率 > 60：关垂直同步 + 手动帧节流
	private long frameNanos = 16_666_667L;
	private String title;
	private boolean runInBackground = false;
	private boolean doExit = false;
	private int windowWidth = 1280;
	private int windowHeight = 720;
	private boolean fullscreen;   // 当前是否处于无边框全屏（用于焦点切换时隐藏/恢复任务栏）
	public ReportHandler reportHandler;
	private final OpenAlAudio audio = new OpenAlAudio(32);

	public Lwjgl3Engine(String title, String imagePath, String soundPath, int fps) {
		this.title = title;
		this.fps = fps > 0 ? fps : 60;
		this.paceFrames = this.fps > 60;
		this.frameNanos = 1_000_000_000L / this.fps;
	}

	public void setExceptionHandler(ExceptionHandler eh) { this.eh = eh; }
	public void setRunInBackground(boolean b) { this.runInBackground = b; }

	@Override
	public void setup(Game g) {
		this.game = g;
		if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
		glfwDefaultWindowHints();
		// 阶段 5：core profile。macOS 只提供 forward-compat core（Apple Silicon 为 4.1）；
		// 其它平台请求 3.3 core。GLSL 330 着色器在两者下均可运行。
		boolean mac = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ENGLISH).contains("mac");
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, mac ? 2 : 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		if (mac) {
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		}
		glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);

		window = glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL);
		glfwWindow = window;
		if (window == NULL) throw new IllegalStateException("Failed to create GLFW window");
		glfwMakeContextCurrent(window);
		GL.createCapabilities();
		// 目标帧率 ≤60 用垂直同步（显示器刷新率驱动）；>60 关闭垂直同步，
		// 由 runUntil 帧节流精确控制在目标帧率（120/144/240 等）
		glfwSwapInterval(paceFrames ? 0 : 1);

		frame = new MyFrame();
		input = new MyInput();
		input.installCallbacks();
		initProjection();
		audio.init();
		audio.setLoadBases(input.soundLoadBases);
	}

	private void initProjection() {
		// 视口 = 帧缓冲尺寸（处理 DPI 缩放），投影 = 窗口逻辑坐标正交投影（CPU 矩阵栈）
		int[] fw = new int[1], fh = new int[1];
		glfwGetFramebufferSize(window, fw, fh);
		glViewport(0, 0, fw[0], fh[0]);
		setWindowProjection();
		glBindVertexArray(0);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	/** 将投影恢复为窗口逻辑坐标正交投影（主渲染入口调用，抵消 FBOGraphics 的 FBO 投影泄漏）。 */
	private void setWindowProjection() {
		int[] fw = new int[1], fh = new int[1];
		glfwGetFramebufferSize(window, fw, fh);
		glViewport(0, 0, fw[0], fh[0]);
		GLCompat.glMatrixMode(GL_PROJECTION);
		GLCompat.glLoadIdentity();
		GLCompat.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
		GLCompat.glMatrixMode(GL_MODELVIEW);
	}

	/** 每帧渲染前完整重置 GL 状态（投影/模型视图/视口/帧缓冲/VAO/纹理）。 */
	private void resetGLState() {
		setWindowProjection();
		GLCompat.glMatrixMode(GL_MODELVIEW);
		GLCompat.glLoadIdentity();
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glBindVertexArray(0);
		GLCompat.glUseProgram(0);   // 复位着色器，避免 UI 走舰船着色器路径
		glActiveTexture(GL_TEXTURE0);   // 重置纹理单元，防止着色器渲染泄漏到其他单元
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GLCompat.glColor4f(1, 1, 1, 1);
	}

	@Override
	public void runUntil(Condition c) {
		long last = System.nanoTime();
		while (!glfwWindowShouldClose(window) && !doExit && !c.satisfied()) {
			glfwPollEvents();
			// 每帧同步真实窗口尺寸（WM 可能因工作区限制/DPI 缩放调整客户区大小）。
			// mode() 必须报告真实尺寸：游戏 GUI 缩放的 letterbox 光标换算依赖 scaleTo，
			// 尺寸过期会导致窗口模式下游戏内光标点与实际鼠标位置产生偏移。
			int[] ww = new int[1], wh = new int[1];
			glfwGetWindowSize(window, ww, wh);
			if (ww[0] > 0 && wh[0] > 0) {
				windowWidth = ww[0];
				windowHeight = wh[0];
			}
			long now = System.nanoTime();
			input.msDelta = (int) ((now - last) / 1_000_000);
			last = now;

			// 窗口未就绪（帧缓冲 0x0，如窗口尚未显示）时跳过渲染，避免 0x0 视口导致 GL 错误/白屏
			int[] fw = new int[1], fh = new int[1];
			glfwGetFramebufferSize(window, fw, fh);
			if (fw[0] <= 0 || fh[0] <= 0) {
				input.frameBegin();
				game.input(input);
				input.frameEnd();
				try { Thread.sleep(16); } catch (InterruptedException ie) { break; }
				continue;
			}

			input.frameBegin();
			try {
				game.input(input);
				// 每帧开始重置 GL 状态（投影/模型视图/视口/帧缓冲/VAO/纹理），
				// 游戏渲染时自行通过 shift/scale/rotate 设置所需变换，避免状态残留与 GL_INVALID_OPERATION
				resetGLState();
				glClearColor(0, 0, 0, 1);
				glClear(GL_COLOR_BUFFER_BIT);
				game.render(frame);
			} catch (Throwable t) {
				if (eh != null) eh.handle(toException(t), false);
				else t.printStackTrace();
			}
			try {
				audio.tick(input.msDelta);
			} catch (Throwable t) {
				// 音频异常不允许使游戏闪退（流式补块/队列操作在个别驱动上可能抛错）
				System.err.println("Audio tick failed (audio continuing): " + t);
				t.printStackTrace();
			}
			glfwSwapBuffers(window);
			input.frameEnd();
			// 高帧率目标（>60）时关闭垂直同步，用帧节流把帧率稳定在目标值
			// （120/144/240 等；游戏逻辑基于 msDelta，高帧率下行为一致）
			if (paceFrames) {
				long remaining = frameNanos - (System.nanoTime() - last);
				if (remaining > 2_000_000) {
					// 长等待先睡掉大部分，留 1.5ms 自旋校准（parkNanos 在 Windows 上精度约 ±1ms）
					LockSupport.parkNanos(remaining - 1_500_000);
				}
				while (System.nanoTime() - last < frameNanos) {
					// 自旋期间持续处理窗口事件：光标移动消息实时送达，
					// glfwGetCursorPos/cursor() 才能拿到最新位置（消除光标输入延迟）
					glfwPollEvents();
					Thread.onSpinWait();
				}
			}
		}
		destroy();
	}

	private static Exception toException(Throwable t) {
		return t instanceof Exception ? (Exception) t : new RuntimeException(t);
	}

	@Override
	public void destroy() {
		// 退出时恢复任务栏：若游戏在全屏（无边框）下直接退出、未先切回窗口模式，
		// 任务栏会保持隐藏（只能重启 explorer 才恢复）。这里无条件 show（幂等），
		// 非 Windows 平台或任务栏操作失败时静默忽略。
		try {
			WindowsTaskbar.show();
		} catch (Throwable t) {
			// 忽略：任务栏恢复失败不影响退出流程
		}
		audio.destroy();
		GLCompat.destroy();
		if (window != NULL) {
			glfwDestroyWindow(window);
			window = NULL;
		}
		glfwTerminate();
	}

	// ---- Frame ----
	private final class MyFrame implements Frame {
		private final Graphics graphics = new Graphics(windowWidth, windowHeight);

		public ScreenMode mode() { return new ScreenMode(windowWidth, windowHeight, false); }
		public int fps() { return fps; }
		public Object nativeRenderer() {
			// 仅同步视口尺寸，不做 GL 状态修改（避免着色器 glBegin 锁期间触发 GL_INVALID_OPERATION）
			graphics.setScreenSize(windowWidth, windowHeight);
			return graphics;
		}
		public Pt cursor() { return input.cursor(); }

		public void rect(Clr c, double x, double y, double w, double h, double angle) {
			GLCompat.glUseProgram(0);   // 复位着色器，避免 UI 走舰船着色器路径
			glActiveTexture(GL_TEXTURE0);   // 复位纹理单元
			GLCompat.glColor4f(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f);
			GLCompat.glDisable(GL_TEXTURE_2D);   // 画纯色矩形禁用纹理
			boolean rot = angle != 0;
			if (rot) {
				GLCompat.glPushMatrix();
				GLCompat.glTranslated(x + w / 2, y + h / 2, 0);
				GLCompat.glRotated(Math.toDegrees(angle), 0, 0, 1);
				GLCompat.glTranslated(-(x + w / 2), -(y + h / 2), 0);
			}
			GLCompat.glBegin(GL_QUADS);
			GLCompat.glVertex2d(x, y);
			GLCompat.glVertex2d(x + w, y);
			GLCompat.glVertex2d(x + w, y + h);
			GLCompat.glVertex2d(x, y + h);
			GLCompat.glEnd();
			if (rot) GLCompat.glPopMatrix();
			GLCompat.glEnable(GL_TEXTURE_2D);
		}

		public void blit(Img img, Clr c, double scale, double x, double y, double w, double h, double angle) {
			if (img == null) return;   // 部分 UI 图标可能为 null，静默跳过
			if (img.machineImgCache == null) {
				// 按需加载（原版 SlickEngine 行为）：machineImgCache 存子图（getSubImage），
				// 与游戏内 ((Image) machineImgCache).draw(...,sx,sy,...) 的相对源坐标语义一致
				img.machineImgCache = input.getImage(img);
			}
			Image slick = (Image) img.machineImgCache;
			if (slick == null) return;
			GLCompat.glUseProgram(0);   // 复位着色器，避免 UI 文字走舰船着色器路径
			// 尺寸回退对齐原版 machineWCache/machineHCache：子图取 srcWidth/srcHeight，
			// 全图 Img（srcWidth==0）取 machineImgCache 实际尺寸
			double dw = (w <= 0 ? (img.srcWidth > 0 ? img.srcWidth : slick.getWidth()) : w) * scale;
			double dh = (h <= 0 ? (img.srcHeight > 0 ? img.srcHeight : slick.getHeight()) : h) * scale;
			glActiveTexture(GL_TEXTURE0);   // 复位纹理单元，避免纹理绑定到泄漏的单元
			GLCompat.glEnable(GL_TEXTURE_2D);   // 确保纹理启用（防止其他绘制 glDisable 后泄漏导致白屏）
			slick.bind();
			float texW = slick.getTextureWidth();
			float texH = slick.getTextureHeight();
			// 全图 Img（srcWidth==0，如英雄头像 scaled/*.jpg）用 machineImgCache 实际尺寸；
			// 子图 Img 用 srcX/srcY/srcWidth/srcHeight（machineImgCache 为对应子图，尺寸一致）
			float imgW = img.srcWidth > 0 ? img.srcWidth : slick.getWidth();
			float imgH = img.srcHeight > 0 ? img.srcHeight : slick.getHeight();
			float u0 = img.srcX / texW;
			float v0 = img.srcY / texH;
			float u1 = (img.srcX + imgW) / texW;
			float v1 = (img.srcY + imgH) / texH;
			// 水平翻转：Img.flip() 只翻转 flipped 标志、srcX/srcY 不变，
			// 这里交换 u 坐标实现纹理左右镜像。否则翻转的武器炮管（预览/放置）会被渲染成正方向。
			if (img.flipped) {
				float tu = u0; u0 = u1; u1 = tu;
			}
			boolean rot = angle != 0;
			if (rot) {
				GLCompat.glPushMatrix();
				GLCompat.glTranslated(x + dw / 2, y + dh / 2, 0);
				GLCompat.glRotated(Math.toDegrees(angle), 0, 0, 1);
				GLCompat.glTranslated(-(x + dw / 2), -(y + dh / 2), 0);
			}
			// 原版 SlickEngine.blit 的 tint 语义（对齐 CatSlick.jar SlickEngine$MyFrame）：
			// 1) c==null：白色、alpha=scale，画一次。
			// 2) c.a==255：tint 调制（RGB×c）、alpha=scale，画一次。
			// 3) c.a!=255 且 scale==1：先画白色原图(alpha=1)，再叠加 tint 图(alpha=c.a/255)，共两次——
			//    这样半透明深色 tint 只是把彩色图标略微压暗，而不是整体乘成深色块。
			// 4) c.a!=255 且 scale!=1：白色(alpha=scale*(255-c.a)/255) + tint(alpha=scale*c.a/255)，两次。
			// 若只做一次 texture×tint×alpha，半透明深色 tint 会把图标压成"偏黑"（如城市升级建造前图标）。
			if (c == null) {
				GLCompat.glColor4f(1, 1, 1, (float) scale);
				blitQuad(u0, v0, u1, v1, x, y, dw, dh);
			} else if (c.a == 255) {
				GLCompat.glColor4f(c.r / 255f, c.g / 255f, c.b / 255f, (float) scale);
				blitQuad(u0, v0, u1, v1, x, y, dw, dh);
			} else if (scale == 1.0) {
				GLCompat.glColor4f(1, 1, 1, 1);
				blitQuad(u0, v0, u1, v1, x, y, dw, dh);
				GLCompat.glColor4f(c.r / 255f, c.g / 255f, c.b / 255f, c.a / 255f);
				blitQuad(u0, v0, u1, v1, x, y, dw, dh);
			} else {
				GLCompat.glColor4f(1, 1, 1, (float) (scale * (255 - c.a) / 255));
				blitQuad(u0, v0, u1, v1, x, y, dw, dh);
				GLCompat.glColor4f(c.r / 255f, c.g / 255f, c.b / 255f, (float) (scale * c.a / 255));
				blitQuad(u0, v0, u1, v1, x, y, dw, dh);
			}
			if (rot) GLCompat.glPopMatrix();
		}

		/** 画一个已绑定纹理的四边形（blit 的分支共用；半透明 tint 需画两遍时复用）。 */
		private void blitQuad(float u0, float v0, float u1, float v1, double x, double y, double dw, double dh) {
			GLCompat.glBegin(GL_QUADS);
			GLCompat.glTexCoord2f(u0, v0); GLCompat.glVertex2d(x, y);
			GLCompat.glTexCoord2f(u0, v1); GLCompat.glVertex2d(x, y + dh);
			GLCompat.glTexCoord2f(u1, v1); GLCompat.glVertex2d(x + dw, y + dh);
			GLCompat.glTexCoord2f(u1, v0); GLCompat.glVertex2d(x + dw, y);
			GLCompat.glEnd();
		}

		public double getWidth(Img img) { return img.srcWidth; }
		public double getHeight(Img img) { return img.srcHeight; }
		public void shift(double x, double y) { GLCompat.glTranslated(x, y, 0); }
		public void scale(double x, double y) { GLCompat.glScaled(x, y, 1); }
		public void rotate(double degrees) { GLCompat.glRotated(degrees, 0, 0, 1); }
		public void resetTransforms() { GLCompat.glLoadIdentity(); }
	}

	// ---- Input ----
	public final class MyInput implements Input {
		private final HashMap<String, Image> images = new HashMap<>();
		private final HashMap<String, Image> flippedImages = new HashMap<>();
		private final ArrayList<File> loadBases = new ArrayList<>();
		private final ArrayList<File> soundLoadBases = new ArrayList<>();
		private final Set<Integer> keysDown = new HashSet<>();
		private final Set<Integer> keysPressed = new HashSet<>();
		private final Set<Character> charsPressed = new HashSet<>();
		private final StringBuilder typedText = new StringBuilder();
		private String lastKeyPressedName;
		private int msDelta;
		private double mouseX, mouseY;
		/** 点击容差（像素）：按下→松开位移小于此值才算"点击"，否则算拖拽。对齐 Slick2D 默认值 5。 */
		private static final double MOUSE_CLICK_TOLERANCE = 5;
		private final boolean[] mouseButtons = new boolean[8];
		private boolean mouseClicked;
		private int clickButton;        // 1-based：1=左,2=右,3=中（0=无点击）
		private double mouseDownX, mouseDownY;
		private int scrollAmount;
		private boolean cursorVisible = true;

		void installCallbacks() {
			glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
				if (action == GLFW_PRESS) {
					keysDown.add(key);
					keysPressed.add(key);
					lastKeyPressedName = keyName(key);
				} else if (action == GLFW_RELEASE) {
					keysDown.remove(key);
				}
			});
			glfwSetCharCallback(window, (w, codepoint) -> {
				char ch = (char) codepoint;
				charsPressed.add(ch);
				typedText.append(ch);
			});
			glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
				if (button < mouseButtons.length) {
					if (action == GLFW_PRESS) {
						mouseButtons[button] = true;
						mouseDownX = mouseX;
						mouseDownY = mouseY;
					} else {
						mouseButtons[button] = false;
						// Slick2D 语义：click 在"松开且按下→松开位移 < 容差"时触发（区别于拖拽），
						// 而不是按下时触发。否则 MOUSE_1_CLICKED 与 MOUSE_1_DOWN 在按下瞬间同时触发，
						// 调色板模块点击后无法正确进入"吸附鼠标"(dragMode=false) 状态，只能拖拽。
						if (Math.abs(mouseX - mouseDownX) < MOUSE_CLICK_TOLERANCE
								&& Math.abs(mouseY - mouseDownY) < MOUSE_CLICK_TOLERANCE) {
							mouseClicked = true;
							clickButton = button + 1;      // 1-based：1=左,2=右,3=中
						}
					}
				}
			});
			glfwSetCursorPosCallback(window, (w, x, y) -> { mouseX = x; mouseY = y; });
			// 焦点切换：无边框全屏时，窗口盖住任务栏；焦点一旦离开游戏就恢复任务栏，
			// 让用户能正常切到其它程序/使用任务栏；焦点回到游戏再隐藏任务栏恢复真全屏。
			glfwSetWindowFocusCallback(window, (w, focused) -> {
				if (focused) {
					if (fullscreen) WindowsTaskbar.hide();
				} else {
					if (fullscreen) WindowsTaskbar.show();
				}
			});
			// GLFW 每格滚轮为 ±1.0，而原版 LWJGL2 Mouse.getDWheel() 返回原始增量（Windows 每格 120），
			// 游戏代码按该单位校准（如 ScrollBar: scrollAmount * scrollSpeed / 60），需换算回原版单位
			glfwSetScrollCallback(window, (w, xoff, yoff) -> { scrollAmount += (int) Math.round(yoff * 120); });
		}

		void frameBegin() {
			// 不清一次性标记：glfwPollEvents 刚设置过 mouseClicked/keysPressed，
			// 需等本帧 game.input() 处理完后再清除（见 frameEnd）
		}
		void frameEnd() {
			// 一次性输入标记在输入处理之后清除（clickButton 重置为 0，与原版一致）
			keysPressed.clear();
			charsPressed.clear();
			// 清空本帧累积的输入字符：否则 typedText 会永久累积，
			// 文本框每帧读取完整历史 → 按一次键出现 "2222..." 重复输入
			typedText.setLength(0);
			mouseClicked = false;
			clickButton = 0;
			scrollAmount = 0;
		}

		public boolean keyDown(String name) {
			Integer code = KEY_NAMES.get(name);
			if (code == null) {
				// SHIFT 表示任一 Shift
				if ("SHIFT".equals(name)) return keysDown.contains(GLFW_KEY_LEFT_SHIFT) || keysDown.contains(GLFW_KEY_RIGHT_SHIFT);
				return false;
			}
			return keysDown.contains(code);
		}
		public boolean keyPressed(String name) {
			Integer code = KEY_NAMES.get(name);
			if (code == null) {
				if ("SHIFT".equals(name)) return keysPressed.contains(GLFW_KEY_LEFT_SHIFT) || keysPressed.contains(GLFW_KEY_RIGHT_SHIFT);
				return false;
			}
			return keysPressed.contains(code);
		}
		public String lastKeyPressed() { return lastKeyPressedName; }
		public char lastInput() {
			for (Character c : charsPressed) return c;
			return 0;
		}
		public String typedText() { return typedText.toString(); }

		/** 实时读取光标位置（glfwGetCursorPos，不经事件队列），消除光标输入延迟。 */
		public Pt cursor() {
			double[] x = new double[1], y = new double[1];
			glfwGetCursorPos(window, x, y);
			mouseX = x[0];
			mouseY = y[0];
			return new Pt(mouseX, mouseY);
		}
		/**
		 * 原版语义：按住任意鼠标键时返回**当前光标位置**（随拖动移动），供拖拽位移计算
		 * （如 StrategicScreen/UniScreen 的右键平移：scroll += mouseDown - prevDragPt）。
		 * 不是按下瞬间的锚点——锚点语义会导致位移恒为 0，右键拖动失效。
		 */
		public Pt mouseDown() {
			for (int i = mouseButtons.length - 1; i >= 0; i--) {
				if (mouseButtons[i]) return new Pt(mouseX, mouseY);
			}
			return null;
		}
		/** 原版语义：按住的最高编号按键 + 1（1=左,2=右,3=中），从高到低扫描，无按键返回 0。 */
		public int mouseDownButton() {
			for (int i = mouseButtons.length - 1; i >= 0; i--) {
				if (mouseButtons[i]) return i + 1;
			}
			return 0;
		}
		public Pt clicked() { return mouseClicked ? new Pt(mouseX, mouseY) : null; }
		public int clickButton() { return clickButton; }
		public int scrollAmount() { return scrollAmount; }
		public int msDelta() { return msDelta; }

		public ScreenMode mode() { return new ScreenMode(windowWidth, windowHeight, false); }
		public Input setMode(ScreenMode m) {
			if (m.width <= 0 || m.height <= 0) return this;
			long monitor = glfwGetPrimaryMonitor();
			fullscreen = monitor != NULL && (m.fullscreen || m.fullscreenWindow);
			if (fullscreen) {
				// 无边框全屏（borderless windowed fullscreen），不做独占全屏：
				// 独占全屏会触发显示模式切换 → OBS 显示器捕获卡帧；topmost 同样会卡 OBS。
				// 正确做法：隐藏任务栏（WindowsTaskbar.hide()），窗口保持普通 z-order。
				// 顺序很关键：**先**去掉边框/禁用缩放，**再**设置位置与尺寸，
				// 否则 glfwSetWindowMonitor 内部用 AdjustWindowRectExForDpi 按带边框样式
				// 计算客户区，导致窗口比显示器原生分辨率小/大几十像素（任务栏露出或超出）。
				GLFWVidMode vm = glfwGetVideoMode(monitor);
				int[] mx = new int[1], my = new int[1];
				glfwGetMonitorPos(monitor, mx, my);
				int fw = vm == null ? m.width : vm.width();
				int fh = vm == null ? m.height : vm.height();
				WindowsTaskbar.hide();
				glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE);
				glfwSetWindowAttrib(window, GLFW_RESIZABLE, GLFW_FALSE);
				glfwSetWindowMonitor(window, NULL, mx[0], my[0], fw, fh, 0);
				// 显式再设一次客户区尺寸，双保险确保客户区 == 显示器原生分辨率
				glfwSetWindowSize(window, fw, fh);
				glfwSetWindowPos(window, mx[0], my[0]);
			} else {
				// 窗口模式：恢复任务栏、装饰/可调整尺寸
				WindowsTaskbar.show();
				glfwSetWindowMonitor(window, NULL, 100, 100, m.width, m.height, 0);
				glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_TRUE);
				glfwSetWindowAttrib(window, GLFW_RESIZABLE, GLFW_TRUE);
			}
			// 查询实际窗口尺寸（OS 可能限制），以实际为准
			int[] w = new int[1], h = new int[1];
			glfwGetWindowSize(window, w, h);
			windowWidth = w[0]; windowHeight = h[0];
			initProjection();
			// 切换窗口模式（尤其无边框全屏的 glfwSetWindowMonitor/glfwSetWindowAttrib）可能
			// 使 GLFW 重置光标模式，与 cursorVisible 字段失同步 → 系统光标"有时不显示"。
			// 这里按字段值显式重放一次，保证光标模式与游戏状态一致。
			glfwSetInputMode(window, GLFW_CURSOR, cursorVisible ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);
			return this;
		}
		public ArrayList<ScreenMode> modes() {
			ArrayList<ScreenMode> list = new ArrayList<>();
			long monitor = glfwGetPrimaryMonitor();
			if (monitor != NULL) {
				// 枚举主显示器支持的全屏模式（GLFW）
				GLFWVidMode.Buffer vm = glfwGetVideoModes(monitor);
				if (vm != null) {
					for (int i = 0; i < vm.remaining(); i++) {
						GLFWVidMode m = vm.get(i);
						ScreenMode sm = new ScreenMode(m.width(), m.height(), true);
						if (!list.contains(sm)) list.add(sm);
					}
				}
			}
			if (list.isEmpty()) {
				list.add(new ScreenMode(windowWidth, windowHeight, false));
			}
			return list;
		}
		public boolean isCursorVisible() { return cursorVisible; }
		public Input setCursorVisible(boolean visible) {
			cursorVisible = visible;
			glfwSetInputMode(window, GLFW_CURSOR, visible ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);
			return this;
		}

		public void addLoadBase(File f) { if (!loadBases.contains(f)) loadBases.add(f); }
		public void addSoundLoadBase(File f) { if (!soundLoadBases.contains(f)) soundLoadBases.add(f); }
		public void clearLoadBases() { loadBases.clear(); }
		public void clearSoundLoadBases() { soundLoadBases.clear(); }

		public void preload(List<Img> imgs) {
			for (Img img : imgs) {
				if (img.machineImgCache != null) continue;
				img.machineImgCache = getImage(img);
			}
		}

		/**
		 * 加载 Img 对应的 Slick Image。与原版 SlickEngine.getImage 语义一致：
		 * 有子区域的 Img 存 getSubImage 子图（机器缓存以子图为准），
		 * 翻转的 Img 先对整图水平翻转再取镜像位置子图。
		 */
		Image getImage(Img img) {
			if (img == null) return null;
			Image image = loadImage(img.src);
			if (image == null) return null;
			if (img.flipped) {
				Image flippedFull = flippedImages.get(img.src);
				if (flippedFull == null) {
					flippedFull = image.getFlippedCopy(true, false);
					flippedImages.put(img.src, flippedFull);
				}
				image = flippedFull;
			}
			if (img.srcWidth != 0 && img.srcHeight != 0) {
				int sx = img.flipped ? image.getWidth() - img.srcX - img.srcWidth : img.srcX;
				image = image.getSubImage(sx, img.srcY, img.srcWidth, img.srcHeight);
			}
			return image;
		}

		Image loadImage(String src) {
			if (images.containsKey(src)) return images.get(src);
			String name = src;
			if (!name.contains(".")) name += ".png";
			Image img = null;
			for (File base : loadBases) {
				File f = new File(base, name);
				if (f.exists()) {
					try {
						img = new Image(new FileInputStream(f), f.getAbsolutePath(), false);
						break;
					} catch (Exception e) { /* try next */ }
				}
			}
			if (img == null) {
				// classpath 资源
				String res = "/com/zarkonnen/airships/images/" + name;
				try {
					img = new Image(res);
				} catch (Exception e) { /* ignore */ }
			}
			if (img != null) images.put(src, img);
			return img;
		}

		// ---- 音频（阶段 4 实现）----
		private float soundZ;
		public void setSoundZ(float z) {
			this.soundZ = z;
			audio.setSoundZ(z);
		}
		public void preloadSounds(List<String> sounds) {
			for (String s : sounds) audio.preload(s);
		}
		public void play(String sound, double pitch, double volume, double x, double y) {
			audio.play(sound, (float) pitch, (float) volume, (float) x, (float) y);
		}
		public Loop loop(String sound, double pitch, double volume, double x, double y) {
			int src = audio.loop(sound, (float) pitch, (float) volume, (float) x, (float) y);
			return src < 0 ? new NoopLoop() : new OpenAlLoop(src);
		}
		public void preloadMusic(String music) { audio.preloadMusic(music); }
		public void playMusic(String music, double volume, MusicCallback onDone, MusicCallback onLoop) {
			audio.playMusic(music, (float) volume, onLoop == null ? null : () -> onLoop.run(music, volume));
		}
		public void stopMusic() { audio.stopMusic(); }
		public void fadeOutMusic(int ms) { audio.fadeOutMusic(ms); }

		public void quit() {
			// 主菜单退出走 ExitScreen：in.quit() 后紧跟 System.exit(0)，destroy() 不会执行，
			// 因此必须在这里就恢复任务栏，否则全屏下退出会留下隐藏的任务栏。
			try {
				WindowsTaskbar.show();
			} catch (Throwable t) {
				// 非 Windows 平台或任务栏操作失败时忽略
			}
			doExit = true;
			glfwSetWindowShouldClose(window, true);
		}
	}

	private static final class NoopLoop implements Loop {
		public void stop() {}
		public void setLocation(float x, float y) {}
		public void setPitch(float p) {}
		public void setVolume(float v) {}
	}

	/** OpenAL 循环音效句柄。 */
	private final class OpenAlLoop implements Loop {
		private final int source;
		OpenAlLoop(int source) { this.source = source; }
		public void stop() { audio.loopStop(source); }
		public void setLocation(float x, float y) { audio.loopSetLocation(source, x, y); }
		public void setPitch(float p) { audio.loopSetPitch(source, p); }
		public void setVolume(float v) { audio.loopSetVolume(source, v); }
	}

	// ---- 按键映射（Slick 键名 → GLFW 键码）----
	private static final HashMap<String, Integer> KEY_NAMES = new HashMap<>();
	static {
		for (int i = 0; i < 26; i++) KEY_NAMES.put(String.valueOf((char) ('A' + i)), GLFW_KEY_A + i);
		for (int i = 0; i < 10; i++) KEY_NAMES.put(String.valueOf(i), GLFW_KEY_0 + i);
		for (int i = 1; i <= 12; i++) KEY_NAMES.put("F" + i, GLFW_KEY_F1 + i - 1);
		KEY_NAMES.put("LEFT", GLFW_KEY_LEFT);
		KEY_NAMES.put("RIGHT", GLFW_KEY_RIGHT);
		KEY_NAMES.put("UP", GLFW_KEY_UP);
		KEY_NAMES.put("DOWN", GLFW_KEY_DOWN);
		KEY_NAMES.put("LSHIFT", GLFW_KEY_LEFT_SHIFT);
		KEY_NAMES.put("RSHIFT", GLFW_KEY_RIGHT_SHIFT);
		KEY_NAMES.put("LCONTROL", GLFW_KEY_LEFT_CONTROL);
		KEY_NAMES.put("RCONTROL", GLFW_KEY_RIGHT_CONTROL);
		KEY_NAMES.put("LALT", GLFW_KEY_LEFT_ALT);
		KEY_NAMES.put("RALT", GLFW_KEY_RIGHT_ALT);
		KEY_NAMES.put("LMENU", GLFW_KEY_LEFT_ALT);
		KEY_NAMES.put("RMENU", GLFW_KEY_RIGHT_ALT);
		KEY_NAMES.put("LMETA", GLFW_KEY_LEFT_SUPER);
		KEY_NAMES.put("RMETA", GLFW_KEY_RIGHT_SUPER);
		KEY_NAMES.put("LWIN", GLFW_KEY_LEFT_SUPER);
		KEY_NAMES.put("RWIN", GLFW_KEY_RIGHT_SUPER);
		KEY_NAMES.put("ENTER", GLFW_KEY_ENTER);
		KEY_NAMES.put("SPACE", GLFW_KEY_SPACE);
		KEY_NAMES.put("BACK", GLFW_KEY_BACKSPACE);
		KEY_NAMES.put("TAB", GLFW_KEY_TAB);
		KEY_NAMES.put("ESCAPE", GLFW_KEY_ESCAPE);
		KEY_NAMES.put("DELETE", GLFW_KEY_DELETE);
		KEY_NAMES.put("INSERT", GLFW_KEY_INSERT);
		KEY_NAMES.put("HOME", GLFW_KEY_HOME);
		KEY_NAMES.put("END", GLFW_KEY_END);
		KEY_NAMES.put("PAGEUP", GLFW_KEY_PAGE_UP);
		KEY_NAMES.put("PAGEDOWN", GLFW_KEY_PAGE_DOWN);
		KEY_NAMES.put("COMMA", GLFW_KEY_COMMA);
		KEY_NAMES.put("PERIOD", GLFW_KEY_PERIOD);
		KEY_NAMES.put("SLASH", GLFW_KEY_SLASH);
		KEY_NAMES.put("MINUS", GLFW_KEY_MINUS);
		KEY_NAMES.put("EQUALS", GLFW_KEY_EQUAL);
		KEY_NAMES.put("BACKSLASH", GLFW_KEY_BACKSLASH);
		KEY_NAMES.put("APOSTROPHE", GLFW_KEY_APOSTROPHE);
		KEY_NAMES.put("SEMICOLON", GLFW_KEY_SEMICOLON);
		KEY_NAMES.put("LBRACKET", GLFW_KEY_LEFT_BRACKET);
		KEY_NAMES.put("RBRACKET", GLFW_KEY_RIGHT_BRACKET);
		KEY_NAMES.put("GRAVE", GLFW_KEY_GRAVE_ACCENT);
	}

	private static String keyName(int key) {
		for (var e : KEY_NAMES.entrySet()) if (e.getValue() == key) return e.getKey();
		return "";
	}
}
