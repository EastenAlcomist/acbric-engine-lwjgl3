package net.fabricacs.engine;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * 引擎自检入口（Fabric {@code preLaunch}，早于 {@code Main.main} 与任何 GL 上下文创建）。
 *
 * <p>作用有二：</p>
 * <ol>
 *   <li><b>常驻断言</b>：每次启动都打印类解析报告；若迁移版类没有压过原版类，说明本 MOD
 *       没被正确安装或 classpath 顺序被破坏，日志里会立刻看到 FAIL。</li>
 *   <li><b>无头自检</b>：{@code -Dacbric.engine.selfTest=true} 时打印报告后直接退出
 *       （0 = 通过，1 = 失败），不需要显示器 / OpenGL 上下文，可用于 CI 或迁移回归验证。</li>
 * </ol>
 *
 * <p>相关开关：{@code -Dacbric.engine.quiet=true} 抑制常驻报告；
 * {@code -Dacbric.engine.strict=true} 在断言失败时中止启动。</p>
 *
 * <p>输出一律 ASCII：游戏日志/控制台可能按 GBK 解码，中文诊断会变乱码。</p>
 */
public final class EngineBootstrap implements PreLaunchEntrypoint {

    public static final String MOD_ID = "acbric_engine_lwjgl3";

    @Override
    public void onPreLaunch() {
        EngineClassResolver resolver = EngineClassResolver.probe();

        boolean selfTest = Boolean.parseBoolean(System.getProperty("acbric.engine.selfTest", "false"));
        boolean quiet = Boolean.parseBoolean(System.getProperty("acbric.engine.quiet", "false"));
        boolean strict = Boolean.parseBoolean(System.getProperty("acbric.engine.strict", "false"));

        if (!quiet || selfTest) {
            System.out.print(resolver.render());
            System.out.println("  engine backend: com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine");
            System.out.println("  windows taskbar helper: " + taskbarStatus());
        }

        // 把内置的 #version 330 core 着色器装进 <gameDir>/data。
        // 只换 class 是不够的：core profile 下旧固定管线着色器在链接期就会失败。
        if (Boolean.parseBoolean(System.getProperty("acbric.engine.patchShaders", "true"))) {
            EngineDataInstaller.Result shaders = EngineDataInstaller.install();
            if (!quiet || selfTest) {
                System.out.println("  game shaders: " + shaders.summary());
                for (String p : shaders.problems) {
                    System.out.println("    ! " + p);
                }
            }
        }

        if (!quiet || selfTest) {
            System.out.flush();
        }

        if (selfTest) {
            System.out.println("[acbric_engine_lwjgl3] selfTest " + (resolver.ok() ? "PASSED" : "FAILED"));
            System.out.flush();
            System.exit(resolver.ok() ? 0 : 1);
        }

        if (!resolver.ok() && strict) {
            throw new IllegalStateException(
                    "[" + MOD_ID + "] LWJGL3 engine classes did not shadow the vanilla classes; see the report above.");
        }
    }

    private static String taskbarStatus() {
        try {
            Class<?> c = Class.forName("com.zarkonnen.catengine.lwjgl3.WindowsTaskbar", true,
                    EngineBootstrap.class.getClassLoader());
            return String.valueOf(c.getMethod("status").invoke(null));
        } catch (Throwable t) {
            return "unavailable (" + t.getClass().getSimpleName() + ")";
        }
    }
}
