# Changelog

## 1.0.0

First release: ships the Slick2D / LWJGL2 → LWJGL3 engine migration of `ACSExpend/src`
as an Acbric mod.

- New LWJGL3 backend (11 classes): `Lwjgl3Engine`, `GLCompat`, `Tex`, `GlProgram`,
  `Framebuffer`, `TextureLoader`, `OpenAlAudio`, `OggDecoder`, `OggStream`, `Utils`, `WindowsTaskbar`.
- Slick2D compatibility layer (21 classes) replacing `slick.jar`; LWJGL2
  `Display`/`DisplayMode` shims (2 classes).
- 165 game classes changed by the migration plus 2 `org.json` classes; the remaining
  466 game classes still come from `asplit-*.zip`.
- 36 `#version 330 core` GLSL files shipped in the jar and installed into
  `<instance>/data` at preLaunch (originals backed up).
- LWJGL3 3.4.2 (API + natives) merged into the mod jar; the mod is self-contained.
- Acbric side: `EngineBootstrap` (preLaunch self-test + strict mode),
  `EngineClassResolver` (class-resolution assertions), `EngineDataInstaller` (shader install),
  `Lwjgl3EngineMod` (lifecycle event registration).
- Sole structural change versus the migration source: `WindowsTaskbar` is split into an
  ordinary facade class plus a preview implementation class, so the mod no longer hard-depends
  on `--enable-preview`.
