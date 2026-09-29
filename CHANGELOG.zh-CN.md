# 变更记录

## 未发布：回退 GL 路由

**1.0.2 的 GL 路由（`GL11`/`GL20` shim）已回退**，回到 33 类版。

用户实测确认：
- 19 类版（含 GL 路由）：**战斗界面不渲染**（编辑器界面正常）。
- 33 类版（回退后）：**战斗界面正常**。

即 GL 路由是这次回归的原因。回退后自检 PASS，编辑器与战斗界面均正常。

GL 路由的动机仍然成立（避免给 14 个类写几百个 mixin），但它的语义等价性**没有成立**：

- `GLCompat.writeAttrib` 会**丢弃 `index < 1000` 的写入**，而 `GL20.glVertexAttrib*` 从原版
  游戏类传进来的是**真实 GL 属性位置**；假位置（`1000 + slot*4 + off`）只有迁移版代码
  通过 `GlProgram.getAttributeID` 才会产生。
- 迁移版 `Appearance` 里有一处**新增**调用 `sublsp.enableVertexAttribute("strength")`，
  说明兼容层需要显式启用属性槽位——这条状态维护在 vanilla 类里没人做。

**重新尝试路由前必须先解决这两点**，并且要有能覆盖船体渲染的验证手段
（吸引模式只到编辑器，船还没加载出来；需要一个能进战斗的自动抓图流程）。

分析结论保留在 `docs/MIGRATION_PROVENANCE.md` 与 `docs/REBASELINE_PLAN.zh-CN.md`。

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
