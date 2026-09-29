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

## 六、未覆盖范围（不要据此下结论）

- **画面与迁移前的一致性没有逐帧比对**：只确认了不崩、无着色器报错。
- **未在 Linux / macOS / Apple Silicon 上运行**；jar 只带 Windows x64 natives。
- **未做完整战役 / 多人 / 大厅联机流程**。
- **未做音频主观试听**（只验证了设备初始化、缓冲与播放调用）。
- **未逐项回归其他 MOD 的功能**：只确认 27 个 MOD 全部加载、无 mixin 注入失败。
  注入点落在方法体确实改过的类上的 MOD（炮塔旋转、船员/模块高清、护盾、粒子优化）
  建议单独跑一遍。
- 真机启动是在 API 0.3.2 实例上做的；dev.33 实例只跑了无头自检。
