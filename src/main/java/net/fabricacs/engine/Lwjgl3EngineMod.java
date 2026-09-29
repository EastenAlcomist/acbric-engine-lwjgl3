package net.fabricacs.engine;

import net.fabricacs.api.AcbricInitializer;
import net.fabricacs.api.AcbricModContext;
import net.fabricacs.api.event.AirshipsLifecycleEvents;

/**
 * Acbric 入口：把引擎迁移版登记进 Acbric 的事件体系，并在关键生命周期点记录引擎状态。
 *
 * <p>真正的类替换发生在 Fabric 的 classpath 层（见 {@link EngineClassResolver}），本入口不
 * 参与替换，只做可观测性与框架集成。</p>
 */
public final class Lwjgl3EngineMod implements AcbricInitializer {

    private static volatile boolean gameStartingLogged;

    @Override
    public void onInitializeAcbric(AcbricModContext ctx) {
        ctx.logger().info("LWJGL3 engine mod active: only the classes the migration actually changed "
                + "are provided by this mod; everything else comes from the game jars "
                + "(LWJGL3 " + lwjgl3Version() + ").");

        AirshipsLifecycleEvents.GAME_STARTING.register(args -> {
            if (gameStartingLogged) {
                return;
            }
            gameStartingLogged = true;
            ctx.logger().info("Game starting on the LWJGL3 engine backend "
                    + "(com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine).");
        });
    }

    @Override
    public void onInitializeAcbric() {
        // 无上下文重载：留空，实际逻辑在带 context 的重载里。
    }

    private static String lwjgl3Version() {
        try {
            Class<?> version = Class.forName("org.lwjgl.Version", false, Lwjgl3EngineMod.class.getClassLoader());
            Object v = version.getMethod("getVersion").invoke(null);
            return String.valueOf(v);
        } catch (Throwable t) {
            return "unknown";
        }
    }
}
