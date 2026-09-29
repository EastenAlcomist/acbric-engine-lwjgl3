# 变更记录

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
