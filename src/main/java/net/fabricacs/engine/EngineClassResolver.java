package net.fabricacs.engine;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * 引擎类解析诊断：确认 LWJGL3 迁移版本的类确实<b>压过</b>了原版游戏类。
 *
 * <p>Acbric 的启动链是 {@code KnotClient} → {@code AirshipsGameProvider}。Fabric 的
 * {@code FabricLoaderImpl.finishModLoading()} 会先把所有非 builtin MOD 的 code source
 * 加进 {@code KnotClassLoader}，之后 {@code GameProvider.unlockClassPath()} 才把
 * {@code libs/asplit-*.zip}、{@code slick.jar}、{@code lwjgl.jar}、{@code CatSlick.jar}
 * 加进去；Knot 的 {@code getRawClassByteArray} 走 {@code URLClassLoader.findResource}，
 * 即<b>先加入者优先</b>。所以本 MOD 只要提供同名类，就会取代原版实现——这正是"引擎迁移
 * 版本以 MOD 形式加载"的机制基础。</p>
 *
 * <p>本类把这些假设变成可在启动时执行、可断言的检查。</p>
 *
 * <p>注意：所有打印文本一律使用 ASCII。这是有意的——游戏日志与控制台可能按 GBK/CP936
 * 解码，中文诊断会变成乱码，反而看不出结论。</p>
 */
public final class EngineClassResolver {

    /** 单个探测点的结果。 */
    public static final class Probe {
        public final String className;
        public final String expectation;
        public final boolean loaded;
        public final String location;
        public final String detail;
        public final boolean failure;

        Probe(String className, String expectation, boolean loaded, String location, String detail, boolean failure) {
            this.className = className;
            this.expectation = expectation;
            this.loaded = loaded;
            this.location = location;
            this.detail = detail;
            this.failure = failure;
        }
    }

    private final List<Probe> probes = new ArrayList<>();
    private final String modLocation;
    private int failures;

    private EngineClassResolver() {
        this.modLocation = locationOf(EngineClassResolver.class);
    }

    /** 本 MOD 的 jar 路径（诊断与自检的判定基准）。 */
    public String modLocation() {
        return modLocation;
    }

    public List<Probe> probes() {
        return probes;
    }

    public int failures() {
        return failures;
    }

    public boolean ok() {
        return failures == 0;
    }

    /** 执行全部探测。 */
    public static EngineClassResolver probe() {
        EngineClassResolver r = new EngineClassResolver();

        // ---- 1. MOD 自带的 LWJGL3 引擎后端 ----
        r.expectFromMod("com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine", "engine backend (LWJGL3)");
        r.expectFromMod("com.zarkonnen.catengine.lwjgl3.GLCompat", "immediate-mode emulation layer");
        r.expectFromMod("com.zarkonnen.catengine.lwjgl3.OpenAlAudio", "OpenAL audio backend");
        r.expectFromMod("com.zarkonnen.catengine.lwjgl3.GlProgram", "GLSL program wrapper");

        // ---- 2. 取代 slick.jar 的 Slick2D 兼容层 ----
        r.expectFromMod("org.newdawn.slick.Graphics", "shadows slick.jar");
        r.expectFromMod("org.newdawn.slick.Image", "shadows slick.jar");
        r.expectFromMod("org.newdawn.slick.Color", "shadows slick.jar");
        r.expectFromMod("org.newdawn.slick.opengl.shader.ShaderProgram", "shadows slick.jar");
        r.expectFromMod("org.newdawn.slick.opengl.pbuffer.FBOGraphics", "shadows slick.jar");
        r.expectFromMod("org.newdawn.slick.opengl.TextureImpl", "shadows slick.jar");
        r.expectFromMod("org.newdawn.slick.openal.SoundStore2", "shadows CatSlick.jar");

        // ---- 3. 取代 LWJGL2 的 Display/DisplayMode shim ----
        r.expectFromMod("org.lwjgl.opengl.Display", "LWJGL2 Display shim");
        r.expectFromMod("org.lwjgl.opengl.DisplayMode", "LWJGL2 DisplayMode shim");

        // ---- 4. 取代 asplit-A/B.zip 的迁移版游戏类 ----
        r.expectFromMod("com.zarkonnen.airships.Main", "migrated entrypoint (uses Lwjgl3Engine)");
        r.expectFromMod("com.zarkonnen.airships.AirshipGame", "migrated game class");
        r.expectFromMod("com.zarkonnen.airships.AGame", "migrated game class");
        r.expectFromMod("com.zarkonnen.airships.MyDraw", "migrated (GL calls via GLCompat)");
        r.expectFromMod("com.zarkonnen.airships.ShipLayers", "migrated game class");
        r.expectFromMod("org.json.JSONObject", "migrated (no sun.misc dependency)");

        // ---- 5. LWJGL3 运行时必须来自 LWJGL3，而不是 lwjgl.jar(LWJGL2) ----
        r.expectLwjgl3("org.lwjgl.opengl.GL11");
        r.expectLwjgl3("org.lwjgl.opengl.GL13");
        r.expectLwjgl3("org.lwjgl.opengl.GL20");
        r.expectLwjgl3("org.lwjgl.opengl.GL30");
        r.expectLwjgl3("org.lwjgl.glfw.GLFW");
        r.expectLwjgl3("org.lwjgl.stb.STBImage");
        r.expectLwjgl3("org.lwjgl.system.Configuration");

        // ---- 6. 残留检测：LWJGL2 / CatSlick 仍在 classpath 上（警告，不算失败）----
        r.report("org.lwjgl.LWJGLException", "LWJGL2 leftover (lwjgl.jar still on the classpath)");
        r.report("com.zarkonnen.catengine.SlickEngine", "CatSlick leftover (present but unused)");
        r.report("org.lwjgl.input.Keyboard", "LWJGL2 leftover (lwjgl.jar still on the classpath)");

        return r;
    }

    private void expectFromMod(String className, String expectation) {
        Class<?> c = load(className);
        if (c == null) {
            probes.add(new Probe(className, expectation, false, "<missing>", "class could not be loaded", true));
            failures++;
            return;
        }
        String loc = locationOf(c);
        boolean sameJar = sameJar(loc, modLocation);
        probes.add(new Probe(className, expectation, true, loc,
                sameJar ? "OK: provided by this mod" : "WRONG: resolved to " + loc, !sameJar));
        if (!sameJar) {
            failures++;
        }
    }

    private void expectLwjgl3(String className) {
        Class<?> c = load(className);
        if (c == null) {
            probes.add(new Probe(className, "LWJGL3", false, "<missing>", "LWJGL3 runtime not on the classpath", true));
            failures++;
            return;
        }
        String loc = locationOf(c);
        String file = fileName(loc);
        // LWJGL2 ships lwjgl.jar / lwjgl_util.jar; LWJGL3 ships lwjgl-3.x.y.jar / lwjgl-opengl-3.x.y.jar ...
        boolean lwjgl2 = file.matches("lwjgl(_util)?\\.jar");
        probes.add(new Probe(className, "LWJGL3", true, loc,
                lwjgl2 ? "WRONG: resolved to LWJGL2 " + file : "OK: " + file, lwjgl2));
        if (lwjgl2) {
            failures++;
        }
    }

    private void report(String className, String note) {
        Class<?> c = load(className);
        if (c == null) {
            probes.add(new Probe(className, note, false, "<absent>", "-", false));
        } else {
            probes.add(new Probe(className, note, true, locationOf(c), "present (harmless for the migrated engine)", false));
        }
    }

    private static Class<?> load(String name) {
        try {
            // initialize=false: define the class but do not run <clinit>, so we never touch
            // GL/GLFW during preLaunch.
            return Class.forName(name, false, EngineClassResolver.class.getClassLoader());
        } catch (Throwable t) {
            return null;
        }
    }

    private static String locationOf(Class<?> c) {
        try {
            CodeSource cs = c.getProtectionDomain().getCodeSource();
            if (cs == null) {
                return "<bootstrap>";
            }
            URL url = cs.getLocation();
            return url == null ? "<unknown>" : url.toString();
        } catch (Throwable t) {
            return "<" + t.getClass().getSimpleName() + ">";
        }
    }

    private static boolean sameJar(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        String fa = fileName(a);
        return !fa.startsWith("<") && fa.equals(fileName(b));
    }

    private static String fileName(String url) {
        String s = url.replace('\\', '/');
        int i = s.lastIndexOf('/');
        return i < 0 ? s : s.substring(i + 1);
    }

    /** 人类可读报告（多行，ASCII）。 */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("[acbric_engine_lwjgl3] class resolution report\n");
        sb.append("  MOD jar: ").append(modLocation).append('\n');
        for (Probe p : probes) {
            String mark = p.failure ? "FAIL" : (p.loaded ? " ok " : " -- ");
            sb.append("  [").append(mark).append("] ").append(p.className).append('\n');
            sb.append("         ").append(p.expectation).append(" -> ").append(p.detail).append('\n');
        }
        sb.append("  result: ").append(failures == 0 ? "PASS" : ("FAIL (" + failures + ")")).append('\n');
        return sb.toString();
    }
}
