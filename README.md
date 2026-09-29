# Acbric LWJGL3 Engine

[中文](README.zh-CN.md)

Ships the **Slick2D / LWJGL2 → LWJGL3 engine migration** for *Airships: Conquer the Skies*
as an Acbric mod. With it installed the game runs on **LWJGL3 3.4.2**; remove it and the game
returns to Slick2D + LWJGL2.

- Mod id: `acbric_engine_lwjgl3` · version 1.0.0 · MIT
- Requires: **JDK 21**, `acbric_api >= 0.3.2`, `airships`
- A standalone project — it is **not** a source set of the Acbric framework and is not part of
  the framework's default distribution.

---

## How it works

Fabric normally does not let mods override game classes, but this launch chain does (verified by
decompiling `fabric-loader-0.19.3`):

1. `Knot.init()` runs `provider.initialize()` → `loader.load()` → `provider.unlockClassPath()`;
2. `FabricLoaderImpl.finishModLoading()`, inside `loader.load()`, already adds **every non-builtin
   mod code source** to `KnotClassLoader`;
3. only afterwards does `AirshipsGameProvider.unlockClassPath()` add the game jars;
4. `KnotClassDelegate.getRawClassByteArray()` resolves through `URLClassLoader.findResource()` —
   **first added wins**.

So this mod's classes shadow `asplit-A/B.zip`, `slick.jar`, `lwjgl.jar` and `CatSlick.jar`,
with no change to `game/Airships.json` and nothing copied into `libs/`.

```
KnotClassLoader URL order (first → last, first wins)
  1. <instance>/mods/*.jar                <- this mod
  2. libs/asplit-A.zip, asplit-B.zip      <- vanilla game classes
  3. libs/CatEngine.jar, CatSlick.jar
  4. libs/slick.jar, lwjgl.jar, ...       <- vanilla Slick2D / LWJGL2
```

---

## What is inside

| Area | Count | Purpose |
|---|---:|---|
| `com/zarkonnen/catengine/lwjgl3/**` | 12 | LWJGL3 backend: main loop, `GLCompat`, `Tex`, `GlProgram`, `Framebuffer`, `OpenAlAudio`, `OggStream`, … |
| `org/newdawn/slick/**` | 21 | Hand-written Slick2D compatibility layer, **replaces `libs/slick.jar`** |
| `org/lwjgl/opengl/{Display,DisplayMode}` | 2 | LWJGL2 `Display`/`DisplayMode` shims (GLFW-backed) |
| `com/zarkonnen/airships/**` | 165 | Game classes changed by the migration (GL via `GLCompat`, JDK 21 cleanups, …) |
| `org/json/{JSONObject,JSONArray}` | 2 | Migration-changed (dropped `sun.misc.FloatingDecimal2`) |
| `net/fabricacs/engine/**` | 4 | Acbric side: entrypoint, boot self-test, class-resolution diagnostics, shader installer |
| LWJGL3 3.4.2 (API + natives) | — | Merged into the mod jar; LWJGL3 extracts its natives from the classpath |
| `acbric_engine_data/shaders/*` | 36 | `#version 330 core` GLSL, installed into the instance's `data/` at preLaunch |

The other 466 `com.zarkonnen.airships` classes are byte-identical to vanilla and are **not** in the
mod; they still come from `asplit-*.zip`. See [`docs/MIGRATION_FILES.txt`](docs/MIGRATION_FILES.txt)
and [`docs/MIGRATION_PROVENANCE.md`](docs/MIGRATION_PROVENANCE.md).

---

## Build

Prepare `local.properties` (git-ignored; see `gradle.properties.example`):

| Key | Meaning |
|---|---|
| `frameworkDir` | A **built** Acbric framework working copy (needs `build/libs/*-api-mod.jar` and `build/dist/Acbric/loader-libs/`) |
| `gameLibDir` | Your game library directory (`asplit-A.zip` / `asplit-B.zip`, …); defaults to `<frameworkDir>/libs` |
| `instanceDir` | The Fabric instance's game directory; the mod installs into `<instanceDir>/mods` and the self-test runs there. Defaults to `<frameworkDir>/game` |
| `staticDir` | Optional. Set when `data/` lives apart from the instance; same meaning as the game's `-Dacs.staticdir` |

```powershell
.\gradlew.bat verifyInputs          # name missing dependencies up front
.\gradlew.bat build                 # compile + build the self-contained mod jar
.\gradlew.bat installMod            # install into <instanceDir>/mods
.\gradlew.bat engineSelfTest        # headless class-resolution self-test (no window); exit 0 = pass
.\gradlew.bat restoreLegacyShaders  # revert the replaced legacy shaders
.\gradlew.bat printModInfo          # print resolved paths and versions
```

Other target platforms: `-Pacbric.lwjglNatives=natives-linux` (or `natives-macos`,
`natives-macos-arm64`).

---

## Once installed

Drop `<instanceDir>/mods/acbric-engine-lwjgl3.jar` into a Fabric instance. At startup the log shows:

```
[acbric_engine_lwjgl3] class resolution report
  MOD jar: .../mods/acbric-engine-lwjgl3.jar
  [ ok ] com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine  engine backend (LWJGL3) -> OK: provided by this mod
  [ ok ] org.newdawn.slick.Graphics                   shadows slick.jar -> OK: provided by this mod
  [ ok ] com.zarkonnen.airships.Main                  migrated entrypoint -> OK: provided by this mod
  [ ok ] org.lwjgl.opengl.GL11                        LWJGL3 -> OK: acbric-engine-lwjgl3.jar
  result: PASS
```

`result: FAIL` means the mod did not take effect — see the troubleshooting table in
[`docs/ENGINE_LWJGL3.md`](docs/ENGINE_LWJGL3.md).

---

## Two things you must know

1. **Replacing classes is not enough — the shaders must move too.** Vanilla `data/*.vert|frag` are
   fixed-pipeline GLSL and fail to link on a core profile 3.3 context (`gl_MultiTexCoord0` collides
   with the generic attribute locations bound by `GlProgram`). The mod bundles 36
   `#version 330 core` files and installs them into the instance's `data/` at preLaunch; the
   originals are backed up as `*.legacy-glsl.bak` and `gradlew restoreLegacyShaders` reverts them.
   Disable with `-Dacbric.engine.patchShaders=false`.
2. **This mod does not hard-depend on `--enable-preview`.** The migration put JDK 21 preview FFM
   calls directly in `WindowsTaskbar`, whose class file needs `--enable-preview` to load. The
   standalone build adds that flag in its own launcher, but a mod cannot assume JVM flags. The mod
   therefore splits it into `WindowsTaskbar` (ordinary facade) plus `WindowsTaskbarFfm` (preview
   implementation); if the preview class cannot load the helper degrades to a no-op, costing only
   "hide the taskbar in borderless fullscreen". Add
   `--enable-preview --enable-native-access=ALL-UNNAMED` for the full behaviour.

---

## Documentation

| Document | Contents |
|---|---|
| [docs/ENGINE_LWJGL3.md](docs/ENGINE_LWJGL3.md) / [中文](docs/ENGINE_LWJGL3.zh-CN.md) | Architecture, mechanism, compatibility, troubleshooting |
| [docs/VERIFICATION.md](docs/VERIFICATION.md) / [中文](docs/VERIFICATION.zh-CN.md) | Measured evidence and untested scope |
| [docs/MIGRATION_PROVENANCE.md](docs/MIGRATION_PROVENANCE.md) | Origin, included scope, the one structural change |
| [docs/MIGRATION_FILES.txt](docs/MIGRATION_FILES.txt) | Byte-wise diff against the pre-migration source |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Environment and pre-PR checks |
| [AGENTS.md](AGENTS.md) | Project constraints for automated agents |

## License

Own sources and docs are MIT (see [LICENSE](LICENSE)). The mod jar contains LWJGL3 (BSD-3-Clause)
and game classes derived from *Airships: Conquer the Skies*; the game itself remains the property of
its authors, and this project does not redistribute game assets.
