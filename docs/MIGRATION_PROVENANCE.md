# 来源与改造说明 / Provenance

本目录的 Java 源码来自引擎迁移工程 `C:\Users\AlchemistCat\Desktop\ACSExpend\src`，
按“以 Acbric MOD 形式加载”的需要做了**最小裁剪**与**一处结构性改造**。

## 一、纳入了哪些源码

判定基准：迁移工程自带的 `src.zip`（迁移前的原版 1.2.14 源码快照，662 个 .java）
与迁移后的 `src/java`（696 个 .java）逐字节比对。

| 分类 | 文件数 | 说明 |
|---|---:|---|
| `com/zarkonnen/catengine/lwjgl3/**` | 11 | 迁移新增的 LWJGL3 后端（引擎主循环、GLCompat、Tex、GlProgram、Framebuffer、OpenAL 音频…） |
| `org/newdawn/slick/**` | 21 | 手写 Slick2D 兼容层，**取代 `libs/slick.jar`** |
| `org/lwjgl/opengl/{Display,DisplayMode}.java` | 2 | LWJGL2 `Display`/`DisplayMode` shim（委托 GLFW） |
| `com/zarkonnen/airships/**` | **33** | 迁移**真正改过行为**的游戏类（详见下节；其余 132 个编译产物等价，改用游戏 jar 的版本） |
| `org/json/{JSONObject,JSONArray}.java` | 2 | 迁移改动过（去掉 `sun.misc.FloatingDecimal2`） |
| `net/fabricacs/engine/**` | 4 | **新增**：Acbric 侧入口、自检、类解析诊断、着色器安装器 |
| `src/main/resources/acbric_engine_data/shaders/*` | 36 | **新增**：`#version 330 core` 版 GLSL（来自 `ACSExpend/src/data/`），随 jar 携带、启动时装进实例的 `data/` |
| 合计 | **240** | |

> `com/zarkonnen/airships` 共 631 个源文件：466 个与原版逐字节相同，
> 另外 **132 个虽然源码有差异、但编译产物等价**（见下节）。因此真正纳入 MOD 的只有 **33 个**，
> 其余全部用 `libs/asplit-A.zip` / `asplit-B.zip` 里的游戏类。

### 逐 hunk 分类：165 个“改动过”的文件里只有 33 个真的改了行为

对 165 个差异文件做逐 hunk 归一化比对。归一化规则：去掉 `strictfp`、
`new X(...)`→`X.valueOf(...)`、冗余类限定符（`Loadable.foo`→`foo`、
`AnimationType.STANDING`→`STANDING`、`DiplomacyWindow.ICON_SIZE`→`ICON_SIZE`）、
纯空白与纯无关 import 行。

| 类别 | 文件数 | 处理 |
|---|---:|---|
| 无语义差异（IDE 重构 / 格式 / 无关 import） | **132** | **直接丢弃**，改用 `asplit-*.zip` 的游戏类 |
| 真实改动 | **33** | 纳入 MOD（本版），是后续改 mixin 的候选集 |

33 个里再分：

| 子类 | 文件数 | 说明 |
|---|---:|---|
| 仅 GL 路由（`GL11.x`→`GLCompat.x`） | 17 | 涉及的全部 GL 函数只有 15 个：`glBindTexture`、`glBegin/glEnd`、`glVertex2d/2f`、`glTexCoord2d`、`glColor3f/4f`、`glEnable/glDisable`、`glVertexAttrib1f/2f/3f/4f` |
| 引擎/输入接线（`SlickEngine`→`Lwjgl3Engine` 等） | 6 | `AirshipGame`、`Main`、`Mod`、`Expansion`、`CombatSoundEffects`、`FBOGraphicsFactory` |
| 迁移期缺陷修复 | 10 | `AGame`（`-Dacs.staticdir`）、`LaunchSettings`（`targetFPS`）、`Keys`（`resetQueriedKeys`）等 |

> **只带 import 改动的文件不能一律丢弃。** `CombatSoundEffects` 全文只差一行 import
> （`SlickEngine` → `Lwjgl3Engine`），但那个类型参与了 cast，
> `((SlickEngine.MyInput) in).typedText()` 会编译成指向不同 owner 的 `checkcast`。
> 这类文件必须留下——`StarsVisualLayer`、`TextField`、`WeatherVisualLayer` 同理。

### 迁移改动分类（差异分析结论）

对 167 个差异文件做归纳，改动只有三类，全部与“能在 JDK21/新渲染栈上跑起来”有关，
没有玩法逻辑改动：

1. **渲染/输入/音频改造** —— `Main`、`AirshipGame`、`MyDraw`、`ShipLayers`、
   `RotatingShader`、`RotatingColoringShader`、`LightMapLayer`、`LightHaloLayer`、
   `BeamLayer`、`ParticleVisualLayer`、`StarsVisualLayer`、`WeatherVisualLayer`、
   `FBOGraphicsFactory`、`ShapeUtils`、`SpritesheetBundle` 等 22 个文件：立即模式
   `GL11/GL20` 调用改走 `GLCompat`，`Display`/`GLContext`/`Mouse` 换成 LWJGL3 或 shim。
2. **JDK 21 兼容清理**（大面积）：`strictfp` 移除（Java 17 起恒等）、
   `new Integer(x)` → `Integer.valueOf(x)`、`sun.misc.FloatingDecimal2` → 标准 API。
   这类改动不改变字节码语义，因此**不影响其他 Acbric MOD 的 mixin 注入点**。
3. **迁移期缺陷修复**：`AGame`（`-Dacs.staticdir` 资源根目录）、`LaunchSettings`（`targetFPS`）、
   `AirshipGame`（光标实时刷新）等，详见迁移工程 `CHANGELOG.md` F1–F13。

## 二、唯一的结构性改造：WindowsTaskbar

迁移原版把 JDK 21 preview 的 Foreign Function & Memory 调用直接写在
`WindowsTaskbar` 里，于是**该 class 文件被打上 `minor_version = 65535`**，运行时必须带
`--enable-preview` 才能加载。独立发行版由自带启动脚本统一加了这个参数；但 Acbric 是
**框架启动**，JVM 由 Gradle / IDE / 用户脚本拉起，MOD 无法假定参数存在。

因此这里拆成两个类：

- `WindowsTaskbar`（**普通类**，本 MOD 提供的公开 API，与迁移版同名同签名）——
  运行期尝试加载 FFM 实现，失败（未开 preview / JDK 大版本不一致 / 非 Windows 无 user32）
  则整体降级为 no-op。
- `WindowsTaskbarFfm`（**preview 类**，包级私有）——迁移原版实现，仅改名。

降级只影响“无边框全屏时隐藏任务栏”这一项外观行为，不影响窗口创建、渲染、输入与音频。
`Lwjgl3Engine` 的全部调用点（`destroy` / 全屏切换 / `quit`）无需改动。

## 三、第二处新增：GLSL 着色器包

**只换 class 是不够的。** 原版 `data/*.vert|frag` 是固定管线时代的 GLSL
（`attribute` / `varying` / `gl_MultiTexCoord0` / `gl_FragColor`）。迁移版把 GLFW 上下文切到
core profile 3.3，`GlProgram` 又把旧属性名绑定到通用 `gen` 槽位，于是旧着色器在**链接期**直接失败：

```
error: the locations of a builtin vertex attribute (named gl_MultiTexCoord0)
       and a bound generic vertex attribute (named trgB) collided
```

实测：只装 class 版 MOD 时 `log.txt` 里出现 4 处 `SlickException: Unable to load shader program`
（`RotatingColoringShader`、`Appearance.lockMaskedBevelledShader`）。

因此把 `ACSExpend/src/data/` 的 36 个已升级着色器原样放进
`src/main/resources/acbric_engine_data/shaders/`，由 `net.fabricacs.engine.EngineDataInstaller`
在 `preLaunch` 安装到 `<游戏目录>/data/`：

| 目标文件状态 | 行为 |
|---|---|
| 不存在 | 写入 |
| 已含 `#version` | **不动**（尊重用户 / 其他 MOD 的改动） |
| 旧版固定管线 GLSL | 备份为 `<name>.legacy-glsl.bak` 后覆盖 |

- `-Dacbric.engine.patchShaders=false` 关闭该步骤；
- `gradlew restoreLegacyShaders` 可整体回退。

`shaders.list` 由 Gradle 任务 `generateEngineShaderManifest` 在构建时按目录内容生成，
不会与实际文件漂移。

## 三·补、基线版本落差（重要，未解决）

**迁移工程的源码是游戏 1.2.14，而 Acbric 的 `libs/asplit-A/B.zip` 是游戏 1.2.15.2。**
依据：`AGame.VERSION = "1.2.14"`；框架启动日志报 `Game 1.2.15.2`。

本 MOD 本质上是“1.2.14 派生的类覆盖在 1.2.15.2 的游戏上”。删掉 132 个类之后，被删的类
改用 1.2.15.2 版本，于是暴露出 1.2.15.2 才有的 API。已按字节码忠实回填两处：

| 缺失成员 | 调用方（1.2.15.2） | 回填方式 |
|---|---|---|
| `AirshipGame.getClient()` | `CampaignWorld` | 返回已有的 `public Client client` 字段 |
| `CityUpgradeType.defenceBudget` | `City`、`HeroManagementAI` | `BonusableValue.intFromJSON(o,"defenceBudget",0)`；描述行用 lang key `local_defence_budget` |

其余差异不构成运行时问题：`getMyInput` / `addLoadBases` / `resetLoadBases` 的
`SlickEngine$MyInput` 参数、`BonusableValue.ImgFromJSON` 字段，调用方全部在本 MOD
重新编译过的类里（`CombatSoundEffects`、`Mod`、`AirshipGame`、`CityUpgradeType`）。

> **这是把改动转成 mixin 之前必须先解决的问题。** mixin 要求目标字节码逐字匹配，
> 而 1.2.14 的源码差异不是 1.2.15.2 字节码的正确参照。要么把迁移重新基线到 1.2.15.2，
> 要么让 Acbric 改用 1.2.14 的游戏 jar。

## 四、没有纳入的东西

- `libs/old/`（`slick.jar`、`lwjgl.jar`、`lwjgl_util.jar`、`CatSlick.jar`、`jinput.jar`、`ibxm.jar`）：
  仍留在 Acbric 的 `libs/` 里供**编译**其他 MOD 使用；运行期它们的同名类被本 MOD 压过。
- `data/` 的 1.3GB 游戏资源本身：属于游戏数据而非 class，仍由本机游戏实例提供。
  但其中 **36 个 `data/*.vert|frag` 必须替换**（见下），其余数据不动。
