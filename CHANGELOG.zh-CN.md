# 变更记录

## 1.0.2

**GL 路由下沉到引擎层：纳管游戏类 33 → 19。**

- 新增 Gradle 任务 `generateGlRoutingShims`：用 ASM 从 `lwjgl-opengl-3.4.2.jar` 复制
  `org/lwjgl/opengl/GL11.class` 与 `GL20.class`，只把 **14 个方法**的字节码换成
  `invokestatic GLCompat.<同名同签名>`，其余（含 `native` 方法、`<clinit>`、注解）原样保留。
  类名不变 ⇒ JNI 绑定不受影响；生成物排在 `zipTree(lwjgl3)` 之前故在 jar 里胜出。
  路由函数（源码扫描得出，非估算）：`GL11` 上 `glBegin` `glEnd` `glVertex2d` `glVertex2f`
  `glTexCoord2d` `glColor3f` `glColor4f` `glBindTexture` `glEnable` `glDisable`；
  `GL20` 上 `glVertexAttrib1f/2f/3f/4f`。常量是编译期内联的，不需处理。
- 删除 14 个"只做 GL 路由"的游戏类（`MyDraw`、`ShipLayers`、`RotatingShader`、
  `RotatingColoringShader`、`LightMapLayer`、`LightHaloLayer`、`BeamLayer`、
  `ParticleVisualLayer`、`Particle`、`ShapeUtils`、`FlagTestScreen`、`TechScreen`、
  `CampaignStatsDisplay`、`SaveHelperWidget`）。原版游戏类一行未改，
  也没在这些类上写任何 mixin——`Appearance` / `ShipLayers` 这些被其他 MOD 占用的注入点完全没被碰。
- **修掉一个会无限递归的坑**：`GLCompat` 原用全限定名调真实 GL
  （`GL11.glEnable`/`glDisable`/`glBindTexture`，4 处），GL11 被覆盖后会转回自己。
  已全部改到未被覆盖的 `GL11C`；`ci-guards.sh` 增加专门守卫。
- 生成期校验：14 个方法的描述符必须全部命中，少一个即构建失败，避免 LWJGL3 升级导致静默漏路由。
- 自检改为断言 `GL11`/`GL20` 由本 MOD 提供、`GL11C` 仍来自 LWJGL3 jar，
  并把 `MyDraw`/`ShipLayers`/`LightMapLayer`/`RotatingShader` 改为"必须来自游戏 jar"。

验证：构建通过（`GL routing shims: 14 methods routed to GLCompat`）；
`javap` 确认 GL11.glBegin/glBindTexture、GL20.glVertexAttrib1f 已转 GLCompat，
未路由的 glBlendFunc 保持原样，GL11C.glEnable 仍是 LWJGL3 原件；
`engineSelfTest` PASS；实机启动 95 秒，`log.txt` **0 异常**，主菜单与 OpenAL 正常。

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
