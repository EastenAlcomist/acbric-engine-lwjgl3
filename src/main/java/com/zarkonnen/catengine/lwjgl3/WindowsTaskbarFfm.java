package com.zarkonnen.catengine.lwjgl3;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * {@link WindowsTaskbar} 的真实实现：用 JDK 21 的 Foreign Function &amp; Memory API
 * （preview）调用 user32 的 {@code FindWindowA} / {@code ShowWindow}。
 *
 * <p><b>本类使用 preview API，其 class 文件被 javac 标记为 minor_version 65535，
 * 运行时必须带 {@code --enable-preview}（且 JDK 大版本需与编译期一致）才能加载。</b>
 * 调用方 {@link WindowsTaskbar} 会捕获加载失败并降级为 no-op，因此本类缺失不影响游戏启动。</p>
 *
 * <p>内容与 {@code ACSExpend/src/java/com/zarkonnen/catengine/lwjgl3/WindowsTaskbar.java}
 * 的迁移实现一致，仅改了类名。</p>
 */
final class WindowsTaskbarFfm {
	private WindowsTaskbarFfm() {}

	private static final Linker LINKER = Linker.nativeLinker();
	private static final SymbolLookup USER32 = SymbolLookup.libraryLookup("user32", Arena.global());

	// FindWindowA（ANSI 版）接受 UTF-8/ANSI 字节串；"Shell_TrayWnd" 为纯 ASCII，两者一致。
	private static final MethodHandle FIND_WINDOW_A;
	private static final MethodHandle SHOW_WINDOW;

	private static final int SW_HIDE = 0;
	private static final int SW_SHOW = 5;

	// 任务栏句柄缓存：hide() 找到后记录，show() 复用，避免重复查找。
	private static long taskbarHwnd;

	static {
		try {
			FIND_WINDOW_A = LINKER.downcallHandle(
					USER32.find("FindWindowA").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			SHOW_WINDOW = LINKER.downcallHandle(
					USER32.find("ShowWindow").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
		} catch (Throwable t) {
			throw new ExceptionInInitializerError(t);
		}
	}

	private static long findTaskbar() {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment cls = arena.allocateUtf8String("Shell_TrayWnd");
			MemorySegment hwnd = (MemorySegment) FIND_WINDOW_A.invoke(cls, MemorySegment.NULL);
			return hwnd.address();
		} catch (Throwable t) {
			return 0;
		}
	}

	/** 隐藏任务栏（进全屏时调用）。失败或找不到任务栏时静默忽略。 */
	public static synchronized void hide() {
		long hwnd = findTaskbar();
		if (hwnd == 0) {
			return;
		}
		taskbarHwnd = hwnd;
		try {
			SHOW_WINDOW.invoke(MemorySegment.ofAddress(hwnd), SW_HIDE);
		} catch (Throwable t) {
			// 忽略：即便隐藏失败也不影响游戏运行
		}
	}

	/** 恢复任务栏显示（退回窗口模式时调用）。 */
	public static synchronized void show() {
		if (taskbarHwnd == 0) {
			taskbarHwnd = findTaskbar();
		}
		if (taskbarHwnd == 0) {
			return;
		}
		try {
			SHOW_WINDOW.invoke(MemorySegment.ofAddress(taskbarHwnd), SW_SHOW);
		} catch (Throwable t) {
			// 忽略
		}
	}
}
