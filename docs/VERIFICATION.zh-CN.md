# 验证记录

[English](VERIFICATION.md) · 本页只写实测过的内容；没测的一律列在最后一节。

## 一、环境

|`项`|值|
|---|---|
| 日期 | 2026-09-29 |
| JDK | Oracle JDK 21（`C:/Program Files/Java/jdk-21`） |
| Gradle | 8.13（wrapper） |
| Fabric Loader | 0.19.3 |
| Acbric API | `0.3.3-dev.33`（框架工作副本 `Acbric-framework`）与 `0.3.2`（旧副本 `FabricACS/Acbric`）各测一轮 |
| 游戏 | 框架识别为 1.2.15.2；迁移基线 1.2.14 |
| 平台 | Windows 11 |

## 二、构建

```
NaN
NaN
```

产物是自包含 JAR（迁移后的引擎类 + 游戏类 + GLSL + LWJGL3 3.4.2 含 natives）。

## 三、无头类解析自检（核心证据）

`engineSelfTest` 只跑 Fabric `preLaunch`，不创建窗口、不需要 GL 上下文，因此可在无人值守环境复现。

### 3.1 API 0.3.3-dev.33 实例

30 个探测点全部 `[ ok ]`，`result: PASS`：

- 引擎与兼容层由本 MOD 提供：`Lwjgl3Engine`、`GLCompat`、`OpenAlAudio`、`GlProgram`、
  `org.newdawn.slick.{Graphics,Image,Color,opengl.shader.ShaderProgram,opengl.pbuffer.FBOGraphics,opengl.TextureImpl,openal.SoundStore2}`、
  `org.lwjgl.opengl.{Display,DisplayMode}`；
- 游戏类由本 MOD 提供：`airships.{Main,AirshipGame,AGame,MyDraw,ShipLayers}`、`org.json.JSONObject`；
- LWJGL3 解析正确：`GL11`、`GL13`、`GL20`、`GL30`、`GLFW`、`STBImage`、`system.Configuration`
  全部来自 `acbric-engine-lwjgl3.jar`，而非 LWJGL2 的 `lwjgl.jar`；
- LWJGL2 / CatSlick 残留被识别为「存在（无害）」。

### 3.2 API 0.3.2 实例

同一 JAR 在 `0.3.2` 实例上同样 `result: PASS`。

> 这一步是回归：最初把 `depends` 写成了 `acbric_api >= 0.3.3-dev.3`（照上游 dev 约定），
> 结果同一个 JAR 在 0.3.2 实例上直接 `HARD_DEP_NO_CANDIDATE` 拒绝加载。
> MOD 实际只用到 `AcbricInitializer` / `AcbricModContext.logger()` / `GAME_STARTING`，
> 下限已改为 `>= 0.3.2` 并在两个实例上各验一次。

### 3.3 preview 路径

`engineSelfTest -Pacbric.enablePreview=1` → `windows taskbar helper: ffm (preview enabled)`；
不加该参数 → `disabled: UnsupportedClassVersionError ... Try running with '--enable-preview'`，
自检仍然 PASS。即降级路径按设计生效。

## 四、着色器安装

构造「全新安装」场景：目标 `data/` 只放 36 个旧版固定管线着色器（`attribute float flipped;` 开头）。

```
before: attribute float flipped;
  game shaders: data=...\scratch-instance\data added=0 upgraded=36 already-core=0 failed=0
after : #version 330 core
backups: 36
```

在真实实例上重复运行 → `already-core=36`（不会重复改写）。

## 五、真机启动

在 API 0.3.2 实例上装了 27 个 MOD（含本 MOD）实机启动约 100 秒，`log.txt` 结果：

|`观察点`|结果|
|---|---|
| MOD 列表 | `acbric_engine_lwjgl3 1.0.0` 出现 |
| 引擎后端 | `Game starting on the LWJGL3 engine backend`（说明生命周期 mixin 注入的是**迁移版** `Main`） |
| FBO | `Offscreen Buffers FBO=true PBUFFER=disabled PBUFFERRT=disabled` |
| 音频 | `OpenAL device: OpenAL Soft`、`OpenAL ready (sources=32)`、音乐流式播放与切曲正常 |
| 加载 | `data checksum OK`、`heroes data checksum OK`、`main menu inited` |
| 战斗 UI 钩子 | `Player control panel UI hook is active. panelType=DIRECT_CONTROL`、`Ship status bar UI hook is active` |
| 着色器 | **0 处** `Shader link failed` |
| 异常 | **0 处** |

对照：加入着色器安装**之前**，同一路径下有 4 处 `SlickException: Unable to load shader program`
（`RotatingColoringShader.lockShader`、`Appearance.lockMaskedBevelledShader`）。

## 五·补、GL 路由（1.0.2）

`generateGlRoutingShims` 从 LWJGL3 复制 `GL11`/`GL20` 并改写 14 个方法后：

| 检查 | 结果 |
|---|---|
| 构建日志 | `GL routing shims: 14 methods routed to GLCompat`；描述符少一个即构建失败 |
| `javap GL11.glBegin` | 已非 native，方法体为 `invokestatic GLCompat.glBegin:(I)V` + `return` |
| `javap GL11.glBindTexture` | `invokestatic GLCompat.glBindTexture:(II)V` |
| `javap GL20.glVertexAttrib1f` | `invokestatic GLCompat.glVertexAttrib1f:(IF)V` |
| `javap GL11.glBlendFunc`（未被路由） | 保持原样，未被动过 |
| `javap GL11C.glEnable` | 仍是 LWJGL3 原件（`public static native`），未被覆盖 |
| `ci-guards.sh` | GLCompat 未用全限定名调被路由函数；且确实通过 GL11C 调真实 GL |
| `engineSelfTest` | PASS；GL11/GL20 来自本 MOD、GL11C 来自 LWJGL3 jar |
| 实机启动 95 秒 | `log.txt` **0 异常**，`main menu inited`、OpenAL 正常、FBO 检查通过 |
| **删类前后画面 A/B**（见下） | 33 类版与 19 类版抓图**完全一致**，编辑器界面正常渲染 |

### 战斗界面：GL 路由回归的确认（用户实测，2026-09-29）

| 版本 | 提交 | 纳管类 | 编辑器 | **战斗** |
|---|---|---:|---|---|
| 33 类版 | `96c269a` / 回退后 `a589181` | 33 | 正常 | **用户实测正常** |
| 19 类版（含 GL 路由） | `a0f979c` | 19 | 正常 | **用户实测不渲染** |

**后续（同日晚）**：按字节码调研结论改为**只路由纯 GL 换主的 10 个类**、
把 `LightMapLayer` 留在 MOD 里（23 类），用户实测**战斗界面正常**。
至此根因确认：**整批路由会破坏战斗渲染，选择性路由不会**，差异就是 `LightMapLayer`
那一处 `Graphics.flush()`。

结论：**GL 路由破坏了船体渲染**，已回退。根因分析见 `docs/REBASELINE_PLAN.zh-CN.md`
「决定 2」下的修订说明：`GLCompat.writeAttrib` 丢弃 `index < 1000` 的写入，
而原版类传入的是真实 GL 属性位置；且属性槽位的 `enableVertexAttribute` 状态
只有迁移版代码在维护。

> 这条正是"**编辑器界面渲染正常不代表整体正常**"的实例：编辑器只画 UI 与蓝图网格，
> 不触发船体的属性/着色器路径。以后验证渲染**必须覆盖战斗**。

### 画面 A/B（编辑器界面，像素级对照）

用「`-Dattract=true` 吸引模式 → 置顶窗口 → `CopyFromScreen` 抓取」在**相同条件**下各跑一次：

| 版本 | 提交 | 纳管类 | 画面 |
|---|---|---:|---|
| GL 路由之前 | `96c269a` | 33 | 编辑器完整渲染：顶栏按钮（覆盖物/筛选科技/自动缩放/翻转/撤销/重做）、属性面板（成本/维护/速度/船员/煤炭/弹药/水/补给）、模块面板（模块 M/装甲 R/涂装 P/装饰 E/搜索/删除模块）、模块列表（浮晶舱 $192、螺旋桨 $68、走廊 $6…）、右侧蓝图网格 |
| GL 路由之后 | `a0f979c` | 19 | **与上表逐像素一致** |

结论：**把 14 个 GL 类的改动下沉到 `GL11`/`GL20` 路由，画面无差异。**

> ⚠️ **抓图陷阱（踩过，记下来）**：一开始用 `PrintWindow(hwnd, hdc, PW_RENDERFULLCONTENT)` 抓窗口，
> 得到的是一张**纯底色的空图**，据此误判成"渲染坏了"。原因是 `PrintWindow` 走 GDI，
> **读不到 OpenGL 绘制内容**。
> 正确做法：`ShowWindow(SW_RESTORE)` + `SetForegroundWindow` 置顶后，
> 用 `Graphics.CopyFromScreen` 抓**窗口矩形区域的屏幕内容**（合成后的桌面，含 GL）。
> 仅当窗口确实在前台时结果才可信；`SetForegroundWindow` 受 Windows 前台锁影响，
> 抓图前应确认抓到的确实是游戏窗口（看标题栏/画面内容）。

编辑器界面的像素级 A/B 见上一节，画面一致。

## 六、未覆盖范围（不要据此下结论）

- **没有做逐帧比对**：编辑器界面的 A/B 只对比了同一时刻的静态截图。
- **战斗界面只做了"能/不能渲染"的定性确认**（用户实测），没有抓图留证，
  也没有逐项核对船体着色、光照、粒子、旗帜等细节。
- 船体/光照/粒子这些重度依赖批渲染仿真的路径**未做迁移前后的一致性比对**。
- **未在 Linux / macOS / Apple Silicon 上运行**；jar 只带 Windows x64 natives。
- **未做完整战役 / 多人 / 大厅联机流程**。
- **未做音频主观试听**（只验证了设备初始化、缓冲与播放调用）。
- **未逐项回归其他 MOD 的功能**：只确认 27 个 MOD 全部加载、无 mixin 注入失败。
  注入点落在方法体确实改过的类上的 MOD（炮塔旋转、船员/模块高清、护盾、粒子优化）
  建议单独跑一遍。
- 真机启动是在 API 0.3.2 实例上做的；dev.33 实例只跑了无头自检。
