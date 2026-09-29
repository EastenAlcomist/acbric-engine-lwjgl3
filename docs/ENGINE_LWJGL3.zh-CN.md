# 引擎迁移 MOD 说明

[English](ENGINE_LWJGL3.md) · 相关：[验证记录](VERIFICATION.zh-CN.md) · [来源与改造](MIGRATION_PROVENANCE.md)

## 一、它是什么

把 `ACSExpend/src` 的 Slick2D / LWJGL2 → LWJGL3 引擎迁移版做成 Acbric MOD。
它不扩展玩法，而是**替换引擎**：MOD 的类压过原版游戏、Slick2D 与 LWJGL2 的同名类。

关键事实：**MOD 只接管真正变过的部分**。`com.zarkonnen.airships` 共 631 个类，
其中 466 个与原版逐字节相同，仍由 `libs/asplit-*.zip` 提供；只有迁移改动过的 165 个进 MOD。

## 二、机制：为什么 MOD 能替换游戏类

Fabric 一般不允许 MOD 覆盖游戏类，但这条启动链允许。结论来自对 `fabric-loader-0.19.3` 的
反编译与实测，而不是猜测：

|`步骤`|位置|做了什么|
|---|---|---|
| 1 | `Knot.init()` | `provider.initialize()` → `loader.load()` → `provider.unlockClassPath()` |
| 2 | `FabricLoaderImpl.finishModLoading()`（在 `loader.load()` 内） | 把所有**非 builtin** MOD 的 code source 加进 `KnotClassLoader` |
| 3 | `AirshipsGameProvider.unlockClassPath()` | 到这一步才加游戏 jar（`asplit-*.zip` 与 `libs/` 顶层其余 jar） |
| 4 | `KnotClassDelegate.getRawClassByteArray()` | 走 `URLClassLoader.findResource()`，**先加入者的 URL 胜出** |

```
KnotClassLoader 的 URL 顺序（先 → 后，前者胜）
  1. <实例>/mods/*.jar                    ← 本 MOD 在这里
  2. libs/asplit-A.zip, asplit-B.zip      ← 原版游戏类
  3. libs/CatEngine.jar, CatSlick.jar
  4. libs/slick.jar, lwjgl.jar, ...       ← 原版 Slick2D / LWJGL2
```

因此：**不需要改 `game/Airships.json`，也不需要把任何东西复制进 `libs/`。**

> 反过来说，这也是本工程最脆弱的一环：一旦上游把 MOD 的 code source 改到游戏 jar 之后加入，
> 或者给游戏 jar 设上 `allowedPrefixes` 隔离，整个机制立刻失效。
> 自检（`engineSelfTest`）就是为这条假设准备的回归。

## 三、内容清单

|`分类`|数量|说明|
|---|---:|---|
| `com/zarkonnen/catengine/lwjgl3/**` | 12 | LWJGL3 后端（`Lwjgl3Engine`、`GLCompat`、`Tex`、`GlProgram`、`Framebuffer`、`TextureLoader`、`OpenAlAudio`、`OggDecoder`、`OggStream`、`Utils`、`WindowsTaskbar`、`WindowsTaskbarFfm`） |
| `org/newdawn/slick/**` | 21 | Slick2D 兼容层，取代 `slick.jar` |
| `org/lwjgl/opengl/**` | 2 | LWJGL2 `Display`/`DisplayMode` shim |
| `com/zarkonnen/airships/**` | 165 | 迁移改动过的游戏类 |
| `org/json/**` | 2 | `JSONObject`/`JSONArray`（去掉 `sun.misc` 依赖） |
| `net/fabricacs/engine/**` | 4 | Acbric 侧入口、自检、诊断、着色器安装器 |
| `acbric_engine_data/shaders/*` | 36 | `#version 330 core` GLSL |

## 四、两个必须知道的技术点

### 4.1 只换 class 不够：着色器也要换

原版 `data/*.vert|frag` 用 `attribute` / `varying` / `gl_MultiTexCoord0` / `gl_FragColor`。
GLFW 上下文切到 core profile 3.3 后，链接期直接失败：

```
error: the locations of a builtin vertex attribute (named gl_MultiTexCoord0)
       and a bound generic vertex attribute (named trgB) collided
```

`GlProgram` 已经把旧属性名绑定到通用 `gen` 槽位，冲突正出在这里。
所以 MOD 自带 36 个 `#version 330 core` 版本，由 `EngineDataInstaller` 在 preLaunch 处理：

|`目标文件状态`|行为|
|---|---|
| 不存在 | 写入 |
| 已含 `#version` | **不动**（尊重用户 / 其他 MOD 的改动） |
| 旧版固定管线 GLSL | 备份为 `<name>.legacy-glsl.bak` 后覆盖 |

关闭：`-Dacbric.engine.patchShaders=false`。回退：`gradlew restoreLegacyShaders`。

### 4.2 preview 类不能硬依赖 `--enable-preview`

迁移原版把 JDK 21 preview 的 FFM 调用直接写在 `WindowsTaskbar` 里，该 class 被 javac 标成
`minor_version = 65535`，运行时必须带 `--enable-preview`。独立发行版由自带启动脚本统一加了
这个参数；但 MOD 无法假定 JVM 参数——用户的 JVM 由 IDE、启动器或他自己的脚本拉起。

本工程因此拆成：

- `WindowsTaskbar`（**普通类**，公开 API 与迁移版同名同签名）——运行期尝试加载 FFM 实现，
  失败（未开 preview / JDK 大版本不一致 / 非 Windows 无 user32）就整体降级为 no-op；
- `WindowsTaskbarFfm`（**preview 类**，包级私有）——迁移原版实现，仅改名。

降级只影响「无边框全屏时隐藏任务栏」这一项外观行为。`Lwjgl3Engine` 的全部调用点
（`destroy` / 全屏切换 / `quit`）无需改动。

## 五、自检与开关

```powershell
.\gradlew.bat engineSelfTest                                  # 无头类解析自检
.\gradlew.bat engineSelfTest -Pacbric.enablePreview=1         # 连 preview FFM 路径一起验证
```

|`系统属性`|默认|作用|
|---|---|---|
| `-Dacbric.engine.selfTest=true` | false | 打印报告后直接退出（0 通过 / 1 失败） |
| `-Dacbric.engine.quiet=true` | false | 抑制常驻报告 |
| `-Dacbric.engine.strict=true` | false | 断言失败时中止启动 |
| `-Dacbric.engine.patchShaders=false` | true | 跳过着色器安装 |

报告一律 ASCII：游戏日志/控制台可能按 GBK 解码，中文诊断会变乱码。

## 六、兼容性

|`项`|说明|
|---|---|
| JDK | **必须 21**。preview class 不跨 JDK 大版本；MOD 以 `--release 21` 编译 |
| Acbric API | `>= 0.3.2`。本 MOD 只用到 `AcbricInitializer`、`AcbricModContext.logger()`、`AirshipsLifecycleEvents.GAME_STARTING` |
| 游戏类来源 | `libs/asplit-A.zip` / `asplit-B.zip` 必须存在（另外 466 个游戏类由它们提供） |
| 其他 MOD 的 mixin | 迁移改动里绝大部分是 `strictfp` 移除 / `Integer.valueOf` / `sun.misc` 替换，**不改变方法体结构**，注入点不受影响 |
| 需要重点回归的 MOD | 注入点落在真正改过方法体的类上：炮塔旋转（`ShipLayers`）、船员高清（`RotatingColoringShader`）、模块高清（`Appearance`）、护盾（`Airship`）、粒子优化（`Particle`）、API（`Main`/`AirshipGame`/`DirectControlPanel`）。`@Inject(at=HEAD)` 类安全；`@Redirect` 在调用点被 GL 重构挪动时可能失效 |
| LWJGL2 残留 | `libs/lwjgl.jar`、`slick.jar`、`CatSlick.jar` 仍在 classpath 上（其他 MOD 编译期要用），但运行期同名类全部被本 MOD 压过；自检把它们列为「存在（无害）」 |
| 跨平台 | MOD JAR 只含一个平台的 natives。换平台需 `-Pacbric.lwjglNatives=...` 重新构建安装 |
| 游戏版本 | 迁移基线是 1.2.14；实测运行于 1.2.15.2 亦可（框架识别为 1.2.15.2） |

## 七、排查

|`现象`|检查|
|---|---|
| `HARD_DEP_NO_CANDIDATE ... acbric_api` | 实例里的 `acbric-api.jar` 版本低于 MOD 声明。换成本 MOD 声明或更高版本；不要只改 MOD 的 `depends` 抬版本 |
| 自检 `result: FAIL` | MOD 是否在 `<instanceDir>/mods`；是否有别的 jar 也提供同名类 |
| 启动后仍是 Slick2D | 日志里有没有 `[acbric_engine_lwjgl3]`；用 `-Dacbric.engine.quiet=false` 打开报告 |
| 画面出现异常着色 / 部件丢失 | 看 `data/*.legacy-glsl.bak` 是否存在（存在=已升级）；`gradlew restoreLegacyShaders` 回退 |
| 无边框全屏任务栏不消失 | 正常降级。加 `--enable-preview --enable-native-access=ALL-UNNAMED` |
| 想失败即停 | `-Dacbric.engine.strict=true` |
