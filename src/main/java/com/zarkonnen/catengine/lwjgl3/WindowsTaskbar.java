package com.zarkonnen.catengine.lwjgl3;

import java.lang.reflect.Method;

/**
 * 隐藏/恢复 Windows 任务栏的<b>门面</b>（非 preview 类）。
 *
 * <p>无边框全屏（borderless fullscreen）覆盖任务栏的正确做法是<b>隐藏任务栏</b>，而不是把
 * 窗口置顶（{@code GLFW_FLOATING}/topmost）：topmost 会让 OBS 的显示器/游戏捕获失效（画面
 * 卡在一帧不更新），而独占全屏又会触发显示模式切换导致同样的 OBS 卡帧。隐藏任务栏既不改变
 * 窗口 z-order，也不切换显示模式，两者皆可兼得。</p>
 *
 * <h2>为什么拆成两个类（相对 {@code ACSExpend/src} 的迁移原版）</h2>
 * 迁移原版把 FFM 调用直接写在 {@code WindowsTaskbar} 里，导致该 class 文件被 javac 打上
 * {@code minor_version = 65535}（preview 标记），运行时<b>必须</b>带 {@code --enable-preview}
 * 才能加载。独立发行版通过自带启动脚本统一加了该参数，但 Acbric 是<b>框架启动</b>——JVM 由
 * Fabric/Knot 之外的 Gradle、IDE 或用户脚本拉起，框架无法保证参数存在。
 *
 * <p>因此这里把真正的 FFM 实现移到 {@link WindowsTaskbarFfm}（preview 类），本类保持普通
 * class 文件，仅在运行期能加载 preview 类时才委派；加载失败（未开 preview、JDK 版本与编译期
 * 不一致、非 Windows 平台没有 user32 等）则整体降级为 no-op。降级只影响"无边框全屏时隐藏
 * 任务栏"这一项外观行为，不影响窗口创建、渲染与输入。</p>
 */
public final class WindowsTaskbar {
	private WindowsTaskbar() {}

	/** 真正的 FFM 实现类名——延迟加载，避免在本类链接期就触发 preview 校验。 */
	private static final String IMPL_CLASS = "com.zarkonnen.catengine.lwjgl3.WindowsTaskbarFfm";

	private static boolean resolved;
	private static Method hideMethod;
	private static Method showMethod;
	private static String status = "unresolved";

	private static synchronized void resolve() {
		if (resolved) {
			return;
		}
		resolved = true;
		try {
			Class<?> impl = Class.forName(IMPL_CLASS, true, WindowsTaskbar.class.getClassLoader());
			hideMethod = impl.getMethod("hide");
			showMethod = impl.getMethod("show");
			status = "ffm (preview enabled)";
		} catch (Throwable t) {
			// UnsupportedClassVersionError（未开 --enable-preview / JDK 与编译期不一致）、
			// NoClassDefFoundError（非 Windows 平台没有 user32）等一律降级。
			hideMethod = null;
			showMethod = null;
			status = "disabled: " + t.getClass().getSimpleName()
					+ (t.getMessage() == null ? "" : " (" + t.getMessage() + ")");
		}
	}

	/** 隐藏任务栏（进全屏时调用）。失败、不支持或找不到任务栏时静默忽略。 */
	public static void hide() {
		invoke(true);
	}

	/** 恢复任务栏显示（退回窗口模式/退出时调用）。幂等。 */
	public static void show() {
		invoke(false);
	}

	private static void invoke(boolean hide) {
		resolve();
		Method m = hide ? hideMethod : showMethod;
		if (m == null) {
			return;
		}
		try {
			m.invoke(null);
		} catch (Throwable t) {
			// 忽略：任务栏操作失败不影响游戏运行
		}
	}

	/** 诊断用：本门面当前的实际后端状态。 */
	public static synchronized String status() {
		resolve();
		return status;
	}
}
