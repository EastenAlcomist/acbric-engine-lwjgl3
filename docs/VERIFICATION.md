# Verification record

[中文](VERIFICATION.zh-CN.md) · This page lists only what was actually measured; untested scope is in the last section.

## 1. Environment

| Item | Value |
|---|---|
| Date | 2026-09-29 |
| JDK | Oracle JDK 21 (`C:/Program Files/Java/jdk-21`) |
| Gradle | 8.13 (wrapper) |
| Fabric Loader | 0.19.3 |
| Acbric API | `0.3.3-dev.33` (working copy `Acbric-framework`) and `0.3.2` (older copy `FabricACS/Acbric`), one round each |
| Game | reported as 1.2.15.2 by the framework; migration baseline is 1.2.14 |
| Platform | Windows 11 |

## 2. Build

```
gradlew.bat verifyInputs  -> BUILD SUCCESSFUL (names missing dependencies up front)
gradlew.bat jar           -> BUILD SUCCESSFUL, engine shader manifest: 36 files
```

The artifact is a self-contained jar (migrated engine classes + game classes + GLSL + LWJGL3 3.4.2 with natives).

## 3. Headless class-resolution self-test (core evidence)

`engineSelfTest` runs only Fabric `preLaunch`: no window, no GL context, so it reproduces unattended.

### 3.1 API 0.3.3-dev.33 instance

All 30 probes `[ ok ]`, `result: PASS`:

- engine and compat layer provided by the mod: `Lwjgl3Engine`, `GLCompat`, `OpenAlAudio`, `GlProgram`,
  `org.newdawn.slick.{Graphics,Image,Color,opengl.shader.ShaderProgram,opengl.pbuffer.FBOGraphics,opengl.TextureImpl,openal.SoundStore2}`,
  `org.lwjgl.opengl.{Display,DisplayMode}`;
- game classes provided by the mod: `airships.{Main,AirshipGame,AGame,MyDraw,ShipLayers}`, `org.json.JSONObject`;
- LWJGL3 resolves correctly: `GL11`, `GL13`, `GL20`, `GL30`, `GLFW`, `STBImage`,
  `system.Configuration` all come from `acbric-engine-lwjgl3.jar`, not from LWJGL2's `lwjgl.jar`;
- LWJGL2 / CatSlick leftovers are reported as "present (harmless)".

### 3.2 API 0.3.2 instance

The same jar reaches `result: PASS` on a `0.3.2` instance as well.

> This is a regression: the dependency was first written as `acbric_api >= 0.3.3-dev.3`
> (following the upstream dev convention), and that jar was then rejected outright with
> `HARD_DEP_NO_CANDIDATE` on a 0.3.2 instance. The mod only uses `AcbricInitializer`,
> `AcbricModContext.logger()` and `GAME_STARTING`, so the floor is now `>= 0.3.2` and both
> instances were verified.

### 3.3 Preview path

`engineSelfTest -Pacbric.enablePreview=1` → `windows taskbar helper: ffm (preview enabled)`.
Without the flag → `disabled: UnsupportedClassVersionError ... Try running with '--enable-preview'`
and the self-test still passes. The fallback behaves as designed.

## 4. Shader install

A "fresh install" scenario: the target `data/` held only the 36 legacy fixed-pipeline shaders
(starting with `attribute float flipped;`).

```
before: attribute float flipped;
  game shaders: data=...\scratch-instance\data added=0 upgraded=36 already-core=0 failed=0
after : #version 330 core
backups: 36
```

Re-running against a real instance reports `already-core=36` (no repeated rewriting).

## 5. Real launch

A ~100 second launch of an API 0.3.2 instance with 27 mods (this one included). From `log.txt`:

| Observation | Result |
|---|---|
| Mod list | `acbric_engine_lwjgl3 1.0.0` present |
| Engine backend | `Game starting on the LWJGL3 engine backend` — the lifecycle mixin injected into the **migrated** `Main` |
| FBO | `Offscreen Buffers FBO=true PBUFFER=disabled PBUFFERRT=disabled` |
| Audio | `OpenAL device: OpenAL Soft`, `OpenAL ready (sources=32)`, streaming music and track switching working |
| Loading | `data checksum OK`, `heroes data checksum OK`, `main menu inited` |
| Combat UI hooks | `Player control panel UI hook is active. panelType=DIRECT_CONTROL`, `Ship status bar UI hook is active` |
| Shaders | **0** `Shader link failed` |
| Exceptions | **0** |

Contrast: **before** the shader install step the same path produced 4
`SlickException: Unable to load shader program` (`RotatingColoringShader.lockShader`,
`Appearance.lockMaskedBevelledShader`).

## 6. Untested scope (do not draw conclusions from this)

- **Visual parity with the pre-migration build was not compared frame by frame**; only "does not
  crash and reports no shader errors" was checked.
- **Not run on Linux / macOS / Apple Silicon**; the jar only carries Windows x64 natives.
- **No full conquest, multiplayer or lobby flow.**
- **No subjective audio listening** (only device init, buffering and playback calls).
- **No per-mod functional regression**: 27 mods all loaded with no mixin injection failure, but mods
  whose injection points sit in classes whose bodies really changed (turret rotation, crew/module
  hi-res, shield, particle optimizer) deserve a dedicated pass.
- The real launch used an API 0.3.2 instance; the dev.33 instance only ran the headless self-test.
