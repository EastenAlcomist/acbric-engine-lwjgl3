# 变更记录

### 剩余 13 类的完整分类（结论：都不宜用 mixin 消掉）

用“调用点引用集”方法把剩余 13 类全部重量了一遍（这方法此前抓出过 `Job` 的空行误判）。

### 方法的盲点（本轮踩到）

**`Keys` 显示 0/0/0，但它不能被删。** 该方法比较的是 `javap` 里 `// Method` 注释体现的
**调用点引用**；`Keys` 新增的 `resetQueriedKeys()` 是**声明**，类内没有调用点，因此不可见。
而 `AirshipGame`（本 MOD 自己的类）正在调它 —— 删掉 `Keys` 会 `NoSuchMethodError`。
**结论：该方法只能证明“没有改调用”，不能证明“没有新增成员”。**

### 13 类的实际改动类型

| 类型 | 类 | 能否用 mixin |
|---|---|---|
| **引擎类型替换**（`SlickEngine$MyInput` → `Lwjgl3Engine$MyInput`） | `Main`(25) `Mod`(21) `AirshipGame`(13) `Expansion`(4) `CombatSoundEffects`(4) | **不能**——这是**类型**变更，不是可注入的行为 |
| **API 回填** | `BonusableValue`(38) `CityUpgradeType`(7) `FBOGraphicsFactory`(5) | 部分可以；`BonusableValue` 的 38 处集中在 `ImgFromJSON` 字段→方法 |
| **新增成员** | `Keys` `LaunchSettings`(`targetFPS`) `Appearance`(`subDebugCount`) | 可以，需访问器接口（调用方是本 MOD 自己的类） |
| **单点修复** | `StrategicScreen`（文案 bug） | 可以，但需 `ordinal=6`，**且该文案极难在游戏里触发验证** |
| **编译器差异（疑似误报）** | `DiplomacyAI`（1: `String.valueOf`） | 待确认，可能只是字符串拼接实现差异 |

### 决定（2026-09-29，用户选定路线 1）

**接受现状**：这 13 个类继续以源码形式留在 MOD 里。它们主要是迁移自身的修复与引擎接线，
不是"整包发布游戏源码"—— 已消掉的 152 个类里绝大多数本来就是编译产物等价或纯 GL 换主。

放弃的备选路线：让 `Lwjgl3Engine.MyInput` 继承 `SlickEngine.MyInput` 以消掉那 5 个类。
放弃理由：`SlickEngine.MyInput` 是 non-static 内部类，继承需要外部实例，可行性存疑，
且要为此引入 `CatSlick.jar` 到运行时。

**因此纳管数定格在 13。** 历史备选方案原文：

1. **接受现状**：这 13 个类继续以源码形式留在 MOD 里，它们主要是迁移自己的修复与引擎接线。
2. **消除类型变更**：若让 `Lwjgl3Engine.MyInput` 继承 `SlickEngine.MyInput`，原版类的 cast 就能通过，
   这 5 个类可一次性消掉。代价是要引入 `CatSlick.jar` 并处理内部类的封闭实例，可行性未验证。

**没有继续盲目前推**：不可验证的改动不做。`StrategicScreen` 那处文案修复尤其如此。

当前状态：构建通过；`engineSelfTest` PASS；`ci-guards` PASS（基线 13）。


### Job 下沉（14 → 13）

`Job` 删除。逐字节对比发现它与原版**只差一个空行**（`public strictfp interface Job` 前多了一行），
属于此前 132 类漏网的一个 —— 当时的归一化分类器对"仅增删空行"的 hunk 判成了真实改动。
零行为风险。

`ci-guards` 基线 14 → 13。

### 下一个：StrategicScreen（注入点已定位）

`noticeInfoString()` 里有一处真实 bug 修复：vanilla 用 `combatInfo.defendingLoc` 拼
`You_are_fighting_x_near_y` 这条文案，迁移版改成 `nearLoc`。
注意：**同一方法里 `nearLoc` 的 null 检查用的就是 `nearLoc`（本来就对）**，
所以 `@Redirect` 必须用 `ordinal` 只命中拼字符串那一处，不能把 null 检查也改掉。

验证：构建通过；`engineSelfTest` PASS；`ci-guards` PASS（基线 13）。

### AGame 下沉到 mixin（15 → 14）

`AGameMixin`：给原版 `AGame.getStaticGameDirectory()` 补 `-Dacs.staticdir` 覆盖分支
（迁移版与原版在这个方法上只差这一处）。用 `@Inject(at = HEAD, cancellable = true)`，
未设置属性时不干预。**纯行为注入、不新增成员**，因此不需要访问器接口——
这是本轮验证通的第二种 mixin 形态。

`ci-guards` 基线 15 → 14。剩余 14 个类全部是真实逻辑改动。

### 剩余 14 个类的处理形态（本轮摸清）

| 形态 | 类 | 做法 |
|---|---|---|
| **纯行为注入** | `AGame` ✅、`DiplomacyAI`、`StrategicScreen`、`Job` | `@Inject`/`@Redirect`，不需访问器接口 |
| **新增成员** | `Keys`（`resetQueriedKeys()`）、`LaunchSettings`（`targetFPS`） | Mixin 只能加成员，调用方（本 MOD 自己的类）需改成经**访问器接口**调用 |
| **引擎接线** | `AirshipGame`、`Main`、`Mod`、`Expansion`、`CombatSoundEffects` | 与 GL/引擎后端耦合，需逐个定注入点 |
| **API 回填** | `BonusableValue`、`CityUpgradeType`、`FBOGraphicsFactory` | 与 1.2.15.x 的 API 落差相关，谨慎处理 |

验证：构建通过；`engineSelfTest` PASS（`AGameMixin` 生效）；`ci-guards` PASS（基线 14）。
**实机画面待用户复测**。

### ShipLayers 下沉（16 → 15）

`ShipLayers` 删除。依据：源码差异是纯 GL 换主（11 处），且它用到的 11 个 `GLCompat` 函数
（`glBegin` `glBindTexture` `glColor3f` `glEnd` `glTexCoord2d` `glVertex2d`
`glVertexAttrib1f/2f/4f` + `GL_QUADS` `GL_TEXTURE_2D` 常量）**全部在路由表内**；
常量是编译期内联的整数，不需要处理。

它是体量最大的一个（14 处属性调用），且被 `turretRotation` 注入 —— 下沉后由原版类
承接，那个 mixin 照常生效。

验证：构建通过；`engineSelfTest` PASS；`ci-guards` PASS（基线 15）。
**战斗画面待用户复测。**

### 再下沉 5 个类（21 → 16）

按字节码方法判定后删除：

| 类 | 依据 |
|---|---|
| `StarsVisualLayer` | 纯 GL 换主；用到的 `glBegin/glEnd/glVertex2d/glColor4f` 与 `GL_QUADS` 全在路由表内 |
| `WeatherVisualLayer` | 同上（另有 `glColor3f/glVertex2f`，亦在表内） |
| `ShapeUtils` | 只有 1 处 `glVertex2f`，在表内 |
| `TextField` | 无 GL 调用，差异仅调试打印 |
| `ResChooserWidget` | 无 GL 调用，差异仅调试打印 |

**`ShipLayers` 暂留**：源码差异是纯 GL（11 处），11 个 GLCompat 函数也都在路由表内，
但 `javap` 两轮都没抽出可比数据（怀疑与其它类一样解析失败），证据不足以单独放行。
它体量大（14 处属性调用）且被 turretRotation 注入，值得单独一轮验证。

验证：构建通过；`engineSelfTest` PASS；`ci-guards` PASS（基线 16）。
**战斗画面待用户复测。**

### 首个游戏类 mixin 上线（23 → 21）

- **新增 mixin 基础设施**：`acbric-engine-lwjgl3.mixins.json` + `fabric.mod.json` 的
  `mixins` 字段 + `net.fabricacs.engine.mixin` 包。Mixin 依赖来自框架 `loader-libs`
  的 `sponge-mixin`（已在编译类路径上）。
- **`LightMapLayerMixin`**：给原版 `LightMapLayer` 补一次 `Graphics.flush()`——
  迁移版与原版**只差这一处**，而它正是上次整批路由导致战斗画面全空的根因。
  注入方式：该方法里 `Graphics.resetTransform()` 只出现 1 次，且是实例方法，
  `@Redirect` 处理器第一参数即接收者，因此不需要 `@Local`（本项目所用 Mixin 没有该注解）。
  与迁移版的偏差：本 mixin 在 `resetTransform()` 后立刻 flush，迁移版是在
  `bindNone()`/`glDisable` 之后；批次内容相同。
- **`MyDraw` 删除**：它与原版的差异**只有调试打印**（StringBuilder），零行为风险。
- 自检断言更新：两者都改为"必须来自游戏 jar"；`ci-guards` 基线 23 → 21。

验证：构建通过；`engineSelfTest` PASS，且 `LightMapLayer` 探针加载成功——
Mixin 配置是 `defaultRequire: 1`，注入失败会直接抛错，所以注入确实生效。
**战斗画面待用户复测**（这是唯一能覆盖光照层路径的验收）。

### 按调研结论重新实施 GL 路由（33 → 23）

依据「二·补」的调研结论，只对**纯 GL 换主**的类启用路由：

- 路由 10 个类（`RotatingShader`、`RotatingColoringShader`、`ParticleVisualLayer`、
  `BeamLayer`、`FlagTestScreen`、`LightHaloLayer`、`Particle`、`CampaignStatsDisplay`、
  `SaveHelperWidget`、`TechScreen`），它们从 MOD 删除（33 → 23）。
- **`LightMapLayer` 保留源码**：它的迁移版多调了一次 `Graphics.flush()`，靠路由复现不了
  —— 这正是上一次整批路由导致战斗界面全空的根因。
- `MyDraw`、`ShapeUtils`、`ShipLayers` 也保留：`MyDraw` 的额外差异只是调试打印（本可路由，
  但保守起见留下），`ShipLayers`/`ShapeUtils` 本轮 javap 没抽到可比数据，无法判定。
- 自检断言更新：`GL11`/`GL20` 必须来自本 MOD、`GL11C` 必须来自 LWJGL3 jar；
  `RotatingShader`/`BeamLayer`/`Particle` 必须来自游戏 jar，`LightMapLayer` 必须来自本 MOD。
- `ci-guards` 纳管基线 33 → 23。

**用户实机验证：战斗界面正常。** 至此确认——整批路由（19 类）会破坏战斗渲染、
选择性路由（23 类）不会；根因就是 `LightMapLayer` 那一处 `Graphics.flush()`。

### 回退 GL 路由

**1.0.2 的 GL 路由（`GL11`/`GL20` shim）已回退**，回到 33 类版。

用户实测确认：
- 19 类版（含 GL 路由）：**战斗界面不渲染**（编辑器界面正常）。
- 33 类版（回退后）：**战斗界面正常**。

即 GL 路由是这次回归的原因。回退后自检 PASS，编辑器与战斗界面均正常。

**根因已于 2026-09-29 用字节码对比查清**（此前写的第一版原因是错的，已作废）：

逐类对比 vanilla（asplit）与迁移版的**方法引用集合**后发现，那 14 个类的差异绝大多数
只是 `GL11.x`/`GL20.x` → `GLCompat.x` 的**宿主类替换**，调用序列完全一致 ——
包括 `ShaderProgram.getAttributeID` 与 `enableVertexAttribute`（**两边都调，参数也一样**）。
所以"属性位置语义不同"并不是根因。

真正的根因是 **`LightMapLayer` 有一处光靠路由复现不了的差异**：迁移版多调了
`org.newdawn.slick.Graphics.flush()`，vanilla 版没有。光照层跑在战斗渲染路径上，
少了这次 flush，批渲染的顺序被打乱 → 战斗画面出不来；编辑器不跑光照层，所以看不出来。

**这也是"编辑器正常 ≠ 整体正常"的机制解释。**

调研结论（`docs/REBASELINE_PLAN.zh-CN.md` 有完整表）：14 个类里
**10 个是纯 GL 换主**（额外差异仅为迁移版加的调试打印）→ 可以安全路由；
**1 个（`LightMapLayer`）含真实非 GL 差异** → 需要为它保留源码或写一个单点 mixin；
**3 个（`ShipLayers`/`MyDraw`/`ShapeUtils`）本轮没抽到可比数据**，需复核。

## 1.0.1

**收窄纳管的游戏源码：165 → 33 个类。**

- 对 165 个差异文件做逐 hunk 归一化比对，发现 **132 个只有无语义差异的改动**
  （IDE 去掉冗余类限定符、`strictfp` 移除、`new Integer`→`valueOf`、格式与无关 import）。
  这些类**从 MOD 中删除**，改由游戏 jar（`asplit-A/B.zip`）提供。
  源码发行里出现的游戏派生文件因此减少约 80%。
- 保留 33 个真正改过行为的类。注意只带 import 改动的文件不能一律丢弃：
  `CombatSoundEffects` 全文只差一行 import，但那个类型参与 cast，字节码不同。
- 自检新增反向断言：`Airship`、`City`、`CampaignWorld`、`Combat`、`ModuleType`、
  `SpritesheetBundle`、`Server` 必须来自游戏 jar，防止纳管范围回涨。
- `tools/ci-guards.sh` 增加纳管数量漂移守卫（基线 33）。
- 修复因删类暴露的 1.2.15.2 API 缺口（见 `docs/MIGRATION_PROVENANCE.md` 三·补）：
  回填 `AirshipGame.getClient()` 与 `CityUpgradeType.defenceBudget`，
  否则 1.2.15.2 的 `CampaignWorld` / `City` / `HeroManagementAI` 会 `NoSuchMethodError` /
  `NoSuchFieldError`。
- 记录**尚未解决**的基线落差：迁移源码是 1.2.14，而 `asplit-*.zip` 是 1.2.15.2。
  这是后续把改动转成 mixin 的前置问题。

验证：构建通过；`engineSelfTest` 双侧断言 PASS（迁移类来自 MOD、未改动类来自游戏 jar）；
实机启动 95 秒，`log.txt` **0 异常**，主菜单与 OpenAL 正常。

## 1.0.0

首个版本：把 `ACSExpend/src` 的 Slick2D / LWJGL2 → LWJGL3 引擎迁移版作为 Acbric MOD 交付。

- 新写 LWJGL3 后端（11 类）：`Lwjgl3Engine`、`GLCompat`、`Tex`、`GlProgram`、
  `Framebuffer`、`TextureLoader`、`OpenAlAudio`、`OggDecoder`、`OggStream`、`Utils`、`WindowsTaskbar`。
- Slick2D 兼容层（21 类）取代 `slick.jar`；LWJGL2 `Display`/`DisplayMode` shim（2 类）。
- 迁移改动过的游戏类 165 个 + `org.json` 2 个；其余 466 个游戏类仍由 `asplit-*.zip` 提供。
- 36 个 `#version 330 core` GLSL 随 jar 携带，preLaunch 安装到 `<实例>/data`（旧文件备份）。
- LWJGL3 3.4.2（API + natives）并入 MOD JAR，MOD 自包含。
- Acbric 侧：`EngineBootstrap`（preLaunch 自检 + 严格模式）、`EngineClassResolver`（类解析断言）、
  `EngineDataInstaller`（着色器安装）、`Lwjgl3EngineMod`（生命周期事件登记）。
- 相对迁移原版唯一的结构性改造：`WindowsTaskbar` 拆成普通门面类 + preview 实现类，
  使 MOD 不再硬依赖 `--enable-preview`。
