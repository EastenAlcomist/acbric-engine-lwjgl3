# Acbric LWJGL3 Engine · 引擎迁移 MOD

[English](README.md)

把《Airships: Conquer the Skies》的 **Slick2D / LWJGL2 → LWJGL3 引擎迁移版**做成一个
Acbric MOD。装上它，游戏整体跑在 **LWJGL3 3.4.2** 上；卸掉它，游戏回到原来的 Slick2D + LWJGL2。

- MOD ID：`acbric_engine_lwjgl3` · 版本 1.0.0 · MIT
- 依赖：**JDK 21**、`acbric_api >= 0.3.2`、`airships`
- 独立工程，**不进入 Acbric 框架的 source set 或默认发行包**

---

## 它是怎么生效的

Fabric 一般不允许 MOD 覆盖游戏类，但这条启动链允许（已对 `fabric-loader-0.19.3` 反编译确认）：

1. `Knot.init()` 顺序是 `provider.initialize()` → `loader.load()` → `provider.unlockClassPath()`；
2. `FabricLoaderImpl.finishModLoading()` 在 `loader.load()` 里就把**所有非 builtin MOD 的
   code source** 加进了 `KnotClassLoader`；
3. 之后 `AirshipsGameProvider.unlockClassPath()` 才加游戏 jar；
4. `KnotClassDelegate.getRawClassByteArray()` 走 `URLClassLoader.findResource()` —— **先加入者胜**。

所以本 MOD 的类会压过 `asplit-A/B.zip`、`slick.jar`、`lwjgl.jar` 与 `CatSlick.jar`，
不需要改 `game/Airships.json`，也不需要往 `libs/` 拷任何东西。

```
KnotClassLoader 的 URL 顺序（先 → 后，前者胜）
  1. <实例>/mods/*.jar                    ← 本 MOD 在这里
  2. libs/asplit-A.zip, asplit-B.zip      ← 原版游戏类
  3. libs/CatEngine.jar, CatSlick.jar
  4. libs/slick.jar, lwjgl.jar, ...       ← 原版 Slick2D / LWJGL2
```

---

## MOD 里有什么

| 分类 | 数量 | 作用 |
|---|---:|---|
| `com/zarkonnen/catengine/lwjgl3/**` | 12 | LWJGL3 后端：主循环、`GLCompat`、`Tex`、`GlProgram`、`Framebuffer`、`OpenAlAudio`、`OggStream`… |
| `org/newdawn/slick/**` | 21 | 手写 Slick2D 兼容层，**取代 `libs/slick.jar`** |
| `org/lwjgl/opengl/{Display,DisplayMode}` | 2 | LWJGL2 `Display`/`DisplayMode` shim（委托 GLFW） |
| `com/zarkonnen/airships/**` | 165 | 迁移改动过的游戏类（GL 调用走 `GLCompat`、JDK 21 兼容清理…） |
| `org/json/{JSONObject,JSONArray}` | 2 | 迁移改动过（去掉 `sun.misc.FloatingDecimal2`） |
| `net/fabricacs/engine/**` | 4 | Acbric 侧：入口、启动自检、类解析诊断、着色器安装器 |
| LWJGL3 3.4.2（API + natives） | — | 并入 MOD JAR，LWJGL3 从 classpath 自行解包 natives |
| `acbric_engine_data/shaders/*` | 36 | `#version 330 core` GLSL，preLaunch 装进实例的 `data/` |

其余 466 个 `com.zarkonnen.airships` 类与原版逐字节相同，**不在 MOD 里**，仍由 `asplit-*.zip` 提供。
逐字节差异清单见 [`docs/MIGRATION_FILES.txt`](docs/MIGRATION_FILES.txt)，来源与改造理由见
[`docs/MIGRATION_PROVENANCE.md`](docs/MIGRATION_PROVENANCE.md)。

---

## 构建

先准备（路径写在 `local.properties`，已被 Git 排除；模板见 `gradle.properties.example`）：

| 变量 | 含义 |
|---|---|
| `frameworkDir` | **已构建**的 Acbric 框架工作副本（需有 `build/libs/*-api-mod.jar` 与 `build/dist/Acbric/loader-libs/`） |
| `gameLibDir` | 自有游戏库目录（`asplit-A.zip` / `asplit-B.zip` 等），默认 `<frameworkDir>/libs` |
| `instanceDir` | Fabric 实例的游戏目录；MOD 装进 `<instanceDir>/mods`，自检也在此运行。默认 `<frameworkDir>/game` |
| `staticDir` | 可选。把 `data/` 与实例目录分开时指定，等价于游戏的 `-Dacs.staticdir` |

```powershell
.\gradlew.bat verifyInputs          # 先点名缺失依赖，不用靠 javac 报错猜
.\gradlew.bat build                 # 编译 + 打自包含 MOD JAR
.\gradlew.bat installMod            # 装到 <instanceDir>/mods
.\gradlew.bat engineSelfTest        # 无头类解析自检（不开窗口），退出码 0 = 通过
.\gradlew.bat restoreLegacyShaders  # 回退被替换的旧着色器
.\gradlew.bat printModInfo          # 打印解析后的路径与版本
```

换目标平台 natives：`-Pacbric.lwjglNatives=natives-linux`（或 `natives-macos` / `natives-macos-arm64`）。

---

## 装上之后

把 `<instanceDir>/mods/acbric-engine-lwjgl3.jar` 放进 Fabric 实例即可。启动时日志会打印：

```
[acbric_engine_lwjgl3] class resolution report
  MOD jar: .../mods/acbric-engine-lwjgl3.jar
  [ ok ] com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine  engine backend (LWJGL3) -> OK: provided by this mod
  [ ok ] org.newdawn.slick.Graphics                   shadows slick.jar -> OK: provided by this mod
  [ ok ] com.zarkonnen.airships.Main                  migrated entrypoint -> OK: provided by this mod
  [ ok ] org.lwjgl.opengl.GL11                        LWJGL3 -> OK: acbric-engine-lwjgl3.jar
  result: PASS
```

出现 `result: FAIL` 就说明 MOD 没生效，见 [`docs/ENGINE_LWJGL3.zh-CN.md`](docs/ENGINE_LWJGL3.zh-CN.md) 的排查表。

---

## 两个必须知道的点

1. **只换 class 不够，着色器也要换。** 原版 `data/*.vert|frag` 是固定管线 GLSL，
   core profile 3.3 下链接失败（`gl_MultiTexCoord0` 与通用属性位置冲突）。MOD 内置 36 个
   `#version 330 core` 版本，preLaunch 装进实例的 `data/`；旧文件备份为 `*.legacy-glsl.bak`，
   可用 `gradlew restoreLegacyShaders` 回退。用 `-Dacbric.engine.patchShaders=false` 可关闭该步骤。
2. **本 MOD 不硬依赖 `--enable-preview`。** 迁移原版把 JDK 21 preview 的 FFM 调用直接写在
   `WindowsTaskbar` 里，那个 class 必须带 `--enable-preview` 才能加载；独立发行版由启动脚本统一加了
   该参数，但作为 MOD 无法假定 JVM 参数。因此拆成 `WindowsTaskbar`（普通门面）+ `WindowsTaskbarFfm`
   （preview 实现），加载不到就降级为 no-op，只损失「无边框全屏时隐藏任务栏」这一项外观行为。
   想启用完整行为就给 JVM 加 `--enable-preview --enable-native-access=ALL-UNNAMED`。

---

## 文档

| 文档 | 内容 |
|---|---|
| [docs/ENGINE_LWJGL3.zh-CN.md](docs/ENGINE_LWJGL3.zh-CN.md) / [EN](docs/ENGINE_LWJGL3.md) | 架构、机制、兼容性、排查表 |
| [docs/VERIFICATION.zh-CN.md](docs/VERIFICATION.zh-CN.md) / [EN](docs/VERIFICATION.md) | 实测证据与未覆盖范围 |
| [docs/RELEASING.zh-CN.md](docs/RELEASING.zh-CN.md) / [EN](docs/RELEASING.md) | 发布流程：为什么 CI 托管 runner 构建不了、三条路线、版本号规则 |
| [docs/MIGRATION_PROVENANCE.md](docs/MIGRATION_PROVENANCE.md) | 来源、纳入范围、唯一一处结构性改造 |
| [docs/MIGRATION_FILES.txt](docs/MIGRATION_FILES.txt) | 与迁移前原版的逐字节差异清单 |
| [CONTRIBUTING.zh-CN.md](CONTRIBUTING.zh-CN.md) | 环境与提交前检查 |
| [AGENTS.md](AGENTS.md) | 给自动化代理的项目约束 |

## 持续集成与发布

```powershell
bash tools/ci-guards.sh                                  # 静态守卫（本地也能跑）
.\tools\release-local.ps1 -Version 1.0.1                # 本机构建发布产物到 dist/
```

- `.github/workflows/verify.yml`：每个 push / PR 跑静态守卫。**不尝试构建**——构建需要自有的游戏
  class 与已构建的框架，托管 runner 拿不到。
- `.github/workflows/release.yml`：推 `v*` tag 时发布；构建作业跑在自托管 runner 上
  （仓库变量 `ENGINE_BUILD_RUNNER`），打包与发布跑在托管 runner 上。

三条路线、自托管 runner 配置、版本号规则见 [docs/RELEASING.zh-CN.md](docs/RELEASING.zh-CN.md)。

## 许可

自有源码与文档采用 MIT（见 [LICENSE](LICENSE)）。MOD JAR 内含 LWJGL3（BSD-3-Clause）与
由《Airships: Conquer the Skies》迁移派生的游戏类；游戏本体版权归其原作者所有，本工程不再分发游戏资源。
