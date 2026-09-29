package net.fabricacs.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把 MOD 内置的 LWJGL3 版 GLSL 着色器安装进游戏数据目录。
 *
 * <h2>为什么必须做这一步</h2>
 * 原版 {@code data/*.vert|frag} 是固定管线时代的 GLSL：用了 {@code attribute}、
 * {@code varying}、{@code gl_MultiTexCoord0}、{@code gl_FragColor} 等内建量。迁移版把 GLFW
 * 上下文切到 <b>core profile 3.3</b>，并在 {@code GlProgram} 里把旧属性名绑定到通用
 * {@code gen} 槽位，于是旧着色器在链接期直接失败：
 *
 * <pre>
 * error: the locations of a builtin vertex attribute (named gl_MultiTexCoord0)
 *        and a bound generic vertex attribute (named trgB) collided
 * </pre>
 *
 * 也就是说，只换 class 文件不够——<b>着色器数据也要换成 {@code #version 330 core} 版本</b>，
 * 否则船体、旗帜、光照等一切经过自定义着色器的绘制都会抛
 * {@code SlickException: Unable to load shader program}。
 *
 * <h2>策略（保守、可回退）</h2>
 * <ul>
 *   <li>目标目录里的同名文件不存在 → 直接写入。</li>
 *   <li>已存在且已含 {@code #version} → 认为已被升级过，<b>不动</b>（尊重用户/其他 MOD 的改动）。</li>
 *   <li>已存在但是旧版固定管线 GLSL → 先备份为 {@code <name>.legacy-glsl.bak}（已存在则不覆盖备份），
 *       再写入升级版。</li>
 * </ul>
 * 用 {@code -Dacbric.engine.patchShaders=false} 可整体关闭本步骤。
 *
 * <p>着色器源码来自迁移工程 {@code ACSExpend/src/data/}（36 个文件，已全部改为 330 core），
 * 随 MOD jar 以资源形式携带，见 {@code MIGRATION_PROVENANCE.md}。</p>
 */
public final class EngineDataInstaller {

    private static final String SHADER_ROOT = "/acbric_engine_data/shaders/";
    private static final String SHADER_LIST = SHADER_ROOT + "shaders.list";
    /** 旧版着色器备份后缀，同时也是回退依据（restoreLegacyShaders Gradle 任务）。 */
    public static final String BACKUP_SUFFIX = ".legacy-glsl.bak";

    private EngineDataInstaller() {
    }

    /** 安装结果。 */
    public static final class Result {
        public File dataDir;
        public int added;
        public int upgraded;
        public int upToDate;
        public int failed;
        public final List<String> problems = new ArrayList<>();

        public String summary() {
            if (dataDir == null) {
                return "skipped (no shader bundle in this jar)";
            }
            return "data=" + dataDir + " added=" + added + " upgraded=" + upgraded
                    + " already-core=" + upToDate + " failed=" + failed;
        }
    }

    /** 执行安装。任何异常都被吞掉并计入 failed，绝不阻断启动。 */
    public static Result install() {
        Result r = new Result();
        List<String> names = readManifest();
        if (names.isEmpty()) {
            return r;
        }

        File dataDir = new File(resolveStaticGameDirectory(), "data");
        r.dataDir = dataDir;
        if (!dataDir.isDirectory()) {
            r.failed = names.size();
            r.problems.add("data directory not found: " + dataDir);
            return r;
        }

        for (String name : names) {
            if (!isSafeShaderName(name)) {
                r.failed++;
                r.problems.add("unsafe name skipped: " + name);
                continue;
            }
            byte[] source = readResource(SHADER_ROOT + name);
            if (source == null) {
                r.failed++;
                r.problems.add("missing resource: " + name);
                continue;
            }

            File target = new File(dataDir, name);
            try {
                if (target.isFile()) {
                    if (isCoreProfile(target)) {
                        r.upToDate++;
                        continue;
                    }
                    File backup = new File(dataDir, name + BACKUP_SUFFIX);
                    if (!backup.exists()) {
                        Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                    }
                    Files.write(target.toPath(), source);
                    r.upgraded++;
                } else {
                    Files.write(target.toPath(), source);
                    r.added++;
                }
            } catch (IOException | RuntimeException e) {
                r.failed++;
                r.problems.add(name + ": " + e);
            }
        }
        return r;
    }

    /**
     * 复刻 {@code AGame.getStaticGameDirectory()} 的解析顺序。
     * 不直接调用游戏类，避免在 preLaunch 阶段触发 {@code AGame} 的静态初始化。
     */
    private static File resolveStaticGameDirectory() {
        String override = System.getProperty("acs.staticdir");
        if (override != null && !override.isEmpty()) {
            return new File(override).getAbsoluteFile();
        }
        // Acbric 的 run 任务设置 -Ddev=true 且工作目录就是 game/，与原版 dev 模式语义一致。
        return new File("").getAbsoluteFile();
    }

    private static List<String> readManifest() {
        byte[] raw = readResource(SHADER_LIST);
        if (raw == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (String line : new String(raw, StandardCharsets.UTF_8).split("\\R")) {
            String s = line.trim();
            if (!s.isEmpty() && !s.startsWith("#")) {
                names.add(s);
            }
        }
        return names;
    }

    private static boolean isSafeShaderName(String name) {
        return (name.endsWith(".vert") || name.endsWith(".frag"))
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && !name.contains("..");
    }

    /** 判断目标文件是否已是 core profile 着色器（源码里出现 #version）。 */
    private static boolean isCoreProfile(File f) {
        try (InputStream in = Files.newInputStream(f.toPath())) {
            byte[] head = new byte[512];
            int n = in.read(head);
            if (n <= 0) {
                return false;
            }
            return new String(head, 0, n, StandardCharsets.US_ASCII).contains("#version");
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] readResource(String path) {
        try (InputStream in = EngineDataInstaller.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
