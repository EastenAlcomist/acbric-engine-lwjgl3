# AGENTS.md — Acbric LWJGL3 Engine

独立 MOD 工程，**不属于 Acbric 框架仓库**，也不进入框架的 source set 或默认发行包。
职责：把《Airships: Conquer the Skies》的 Slick2D / LWJGL2 → LWJGL3 引擎迁移版作为
一个 Acbric MOD 交付。MOD ID `acbric_engine_lwjgl3`，MIT，协作基线 `main`。

## 这个 MOD 不是普通功能 MOD

它不做玩法扩展，而是**替换引擎**：jar 内提供 LWJGL3 后端、Slick2D 兼容层、
LWJGL2 `Display`/`DisplayMode` shim，以及迁移改动过的 165 个游戏类。
装上它，游戏整体跑在 LWJGL3 上；卸掉它，游戏回到 Slick2D + LWJGL2。

## 机制（改代码前必须理解）

Fabric 一般不允许 MOD 覆盖游戏类，但这条启动链允许，且已实测：

1. `Knot.init()` 顺序为 `provider.initialize()` → `loader.load()` → `provider.unlockClassPath()`；
2. `FabricLoaderImpl.finishModLoading()`（在 `loader.load()` 内）把**所有非 builtin MOD 的
   code source** 加进 `KnotClassLoader`；
3. 之后 `AirshipsGameProvider.unlockClassPath()` 才加游戏 jar；
4. `KnotClassDelegate.getRawClassByteArray()` 走 `URLClassLoader.findResource()`——
   **URL 先加入者胜**。

所以 MOD 的类压过 `asplit-A/B.zip`、`slick.jar`、`lwjgl.jar`、`CatSlick.jar`。
**不要**把这类放进 `libs/` 指望它生效，也不要依赖 `game/Airships.json` 的类路径顺序。

## 约束

- **JDK 21 必须**：迁移版用 JDK 21 preview 的 FFM API，且以 `--release 21` 编译。
- **不要硬依赖 `--enable-preview`**：`WindowsTaskbar` 是普通门面类，
  真正用 preview API 的 `WindowsTaskbarFfm` 加载不到就降级为 no-op。新增 preview 代码
  必须照这个模式隔离，否则用户在 IDE / 自建脚本里启动会直接失败。
- **不要只换 class 忘了着色器**：core profile 3.3 下原版固定管线 GLSL 会链接失败
  （`gl_MultiTexCoord0` 与 `GlProgram` 绑定的通用属性位置冲突）。`src/main/resources/
  acbric_engine_data/shaders` 的 36 个 `#version 330 core` 文件是必需的，
  由 `EngineDataInstaller` 在 preLaunch 装进 `<instanceDir>/data`（旧文件备份为
  `*.legacy-glsl.bak`，`gradlew restoreLegacyShaders` 回退）。
- **游戏类与框架类不得进 MOD 源码发行包**：`compileOnly` 依赖只用于编译；
  `libs/`、`game/`、`local.properties` 一律被 `.gitignore` 排除。
  唯一打进 jar 的第三方运行时是 LWJGL3（必须，用于压过 LWJGL2）。
- 自有 Java 源码使用 UTF-8 中文职责注释；JSON 不加注释。
- 文档与用户可见说明同时维护中英文。
- 无用户要求时不自动提交、推送或清理构建证据。

## 构建 / 自检

```powershell
.\gradlew.bat verifyInputs          # 先确认本机框架发行与游戏文件齐备
.\gradlew.bat build                 # 编译 + 打自包含 MOD JAR
.\gradlew.bat installMod            # 装到 <instanceDir>/mods
.\gradlew.bat engineSelfTest        # 无头类解析自检，退出码 0 = 通过
.\gradlew.bat restoreLegacyShaders  # 回退被替换的旧着色器
```

路径来自被 Git 排除的 `local.properties`（可复制 `gradle.properties.example`），
命令行 `-PframeworkDir=... -PgameLibDir=... -PinstanceDir=...` 可覆盖。

## 关键文件

- `src/main/java/com/zarkonnen/catengine/lwjgl3/` — LWJGL3 后端（`Lwjgl3Engine`、`GLCompat`…）。
- `src/main/java/org/newdawn/slick/` — 手写 Slick2D 兼容层，取代 `slick.jar`。
- `src/main/java/com/zarkonnen/airships/` — 迁移改动过的游戏类（其余仍由 `asplit-*.zip` 提供）。
- `src/main/java/net/fabricacs/engine/` — Acbric 侧入口、自检、诊断、着色器安装器。
- `src/main/resources/acbric_engine_data/shaders/` — 36 个 `#version 330 core` GLSL。
- `docs/MIGRATION_PROVENANCE.md` — 来源、纳入范围与唯一一处结构性改造的理由。
- `docs/MIGRATION_FILES.txt` — 与迁移前原版的逐字节差异清单。
- `docs/VERIFICATION.zh-CN.md` — 实测证据与未覆盖范围。

## 陷阱

- `acbric_engine_lwjgl3` 依赖 `acbric_api >= 0.3.2`（本 MOD 只用到 `AcbricInitializer`、
  `AcbricModContext.logger()` 与 `AirshipsLifecycleEvents.GAME_STARTING`，0.3.2 即有）。
  **不要随手把它抬到 dev 版本**：那会让同一个 jar 在旧 API 实例上直接拒绝加载
  （`HARD_DEP_NO_CANDIDATE`）。声明下限要按实际用到的最小 API 写。
- 类文件被定义为「迁移版」后，其他 MOD 的 mixin 注入点会落在迁移版方法体上：
  `@Inject(at=HEAD)` 类安全；`@Redirect` 在方法体被 GL 重构挪动时可能失效。
- 自检输出的文案一律 ASCII：游戏日志/控制台可能按 GBK 解码，中文诊断会变乱码。
