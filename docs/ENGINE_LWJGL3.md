# Engine migration mod

[中文](ENGINE_LWJGL3.zh-CN.md) · See also: [verification record](VERIFICATION.md) · [origin and changes](MIGRATION_PROVENANCE.md)

## 1. What it is

Ships the Slick2D / LWJGL2 → LWJGL3 engine migration of `ACSExpend/src` as an Acbric mod.
It does not extend gameplay; it **replaces the engine**: the mod's classes shadow the vanilla
game, Slick2D and LWJGL2 classes.

The key fact: **the mod only takes over what actually changed.** Of the 631
`com.zarkonnen.airships` classes, 466 are byte-identical to vanilla and still come from
`libs/asplit-*.zip`; only the 165 changed by the migration are in the mod.

## 2. Mechanism: why a mod can replace game classes

Fabric normally forbids this, but this launch chain allows it. The conclusion comes from
decompiling `fabric-loader-0.19.3` and from measurement, not from guesswork:

| Step | Where | What happens |
|---|---|---|
| 1 | `Knot.init()` | `provider.initialize()` → `loader.load()` → `provider.unlockClassPath()` |
| 2 | `FabricLoaderImpl.finishModLoading()` (inside `loader.load()`) | adds every **non-builtin** mod code source to `KnotClassLoader` |
| 3 | `AirshipsGameProvider.unlockClassPath()` | only now are the game jars added (`asplit-*.zip` plus the rest of `libs/`) |
| 4 | `KnotClassDelegate.getRawClassByteArray()` | resolves via `URLClassLoader.findResource()` — **the first added URL wins** |

```
KnotClassLoader URL order (first → last, first wins)
  1. <instance>/mods/*.jar                <- this mod
  2. libs/asplit-A.zip, asplit-B.zip      <- vanilla game classes
  3. libs/CatEngine.jar, CatSlick.jar
  4. libs/slick.jar, lwjgl.jar, ...       <- vanilla Slick2D / LWJGL2
```

So: **no change to `game/Airships.json`, and nothing copied into `libs/`.**

> This is also the project's most fragile assumption: if upstream ever adds mod code sources
> after the game jars, or applies `allowedPrefixes` isolation to the game jars, the whole
> mechanism breaks. `engineSelfTest` exists as a regression guard for exactly that.

## 3. Contents

| Area | Count | Notes |
|---|---:|---|
| `com/zarkonnen/catengine/lwjgl3/**` | 12 | LWJGL3 backend (`Lwjgl3Engine`, `GLCompat`, `Tex`, `GlProgram`, `Framebuffer`, `TextureLoader`, `OpenAlAudio`, `OggDecoder`, `OggStream`, `Utils`, `WindowsTaskbar`, `WindowsTaskbarFfm`) |
| `org/newdawn/slick/**` | 21 | Slick2D compatibility layer replacing `slick.jar` |
| `org/lwjgl/opengl/**` | 2 | LWJGL2 `Display`/`DisplayMode` shims |
| `com/zarkonnen/airships/**` | 165 | Game classes changed by the migration |
| `org/json/**` | 2 | `JSONObject`/`JSONArray` (no `sun.misc` dependency) |
| `net/fabricacs/engine/**` | 4 | Acbric entrypoint, self-test, diagnostics, shader installer |
| `acbric_engine_data/shaders/*` | 36 | `#version 330 core` GLSL |

## 4. Two technical points you must know

### 4.1 Replacing classes is not enough: the shaders must move too

Vanilla `data/*.vert|frag` use `attribute` / `varying` / `gl_MultiTexCoord0` / `gl_FragColor`.
On a core profile 3.3 context they fail at link time:

```
error: the locations of a builtin vertex attribute (named gl_MultiTexCoord0)
       and a bound generic vertex attribute (named trgB) collided
```

`GlProgram` already binds the legacy attribute names to generic `gen` slots, which is exactly
where the collision comes from. The mod therefore bundles 36 `#version 330 core` files and has
`EngineDataInstaller` handle them at preLaunch:

| Target file state | Action |
|---|---|
| missing | write |
| already contains `#version` | **left alone** (respects user / other mod edits) |
| legacy fixed-pipeline GLSL | backed up as `<name>.legacy-glsl.bak`, then overwritten |

Disable with `-Dacbric.engine.patchShaders=false`; revert with `gradlew restoreLegacyShaders`.

### 4.2 A preview class must not hard-require `--enable-preview`

The migration put JDK 21 preview FFM calls directly in `WindowsTaskbar`; javac marks that class
`minor_version = 65535`, so it needs `--enable-preview` to load. The standalone build adds the
flag in its own launcher, but a mod cannot assume JVM flags — the user's JVM is started by an IDE,
a launcher, or their own script.

Hence the split:

- `WindowsTaskbar` (**ordinary class**, public API identical to the migration's) — tries to load
  the FFM implementation at runtime and degrades to a no-op on failure (no preview, mismatched
  JDK major version, non-Windows without user32);
- `WindowsTaskbarFfm` (**preview class**, package-private) — the migration's implementation, renamed.

The fallback only costs "hide the taskbar in borderless fullscreen". Every `Lwjgl3Engine` call site
(`destroy`, fullscreen toggle, `quit`) is unchanged.

## 5. Self-test and switches

```powershell
.\gradlew.bat engineSelfTest                                  # headless class-resolution self-test
.\gradlew.bat engineSelfTest -Pacbric.enablePreview=1         # also exercise the preview FFM path
```

| System property | Default | Effect |
|---|---|---|
| `-Dacbric.engine.selfTest=true` | false | print the report and exit (0 pass / 1 fail) |
| `-Dacbric.engine.quiet=true` | false | suppress the always-on report |
| `-Dacbric.engine.strict=true` | false | abort startup when an assertion fails |
| `-Dacbric.engine.patchShaders=false` | true | skip the shader install |

All report text is ASCII on purpose: game logs and consoles may decode as GBK, which turns
Chinese diagnostics into mojibake.

## 6. Compatibility

| Item | Note |
|---|---|
| JDK | **21 required**. Preview classes are version-locked; the mod compiles with `--release 21` |
| Acbric API | `>= 0.3.2`. The mod only uses `AcbricInitializer`, `AcbricModContext.logger()` and `AirshipsLifecycleEvents.GAME_STARTING` |
| Game classes | `libs/asplit-A.zip` / `asplit-B.zip` must exist (they provide the other 466 game classes) |
| Other mods' mixins | Most migration edits are `strictfp` removal / `Integer.valueOf` / `sun.misc` replacement, which **do not change method bodies**, so injection points are unaffected |
| Worth re-testing | Mods injecting into classes whose bodies really changed: turret rotation (`ShipLayers`), crew hi-res (`RotatingColoringShader`), module hi-res (`Appearance`), shield (`Airship`), particle optimizer (`Particle`), API (`Main`/`AirshipGame`/`DirectControlPanel`). `@Inject(at=HEAD)` is safe; `@Redirect` may break if the GL rewrite moved the call site |
| LWJGL2 leftovers | `libs/lwjgl.jar`, `slick.jar`, `CatSlick.jar` stay on the classpath (other mods compile against them) but are shadowed at runtime; the self-test lists them as "present (harmless)" |
| Cross-platform | The jar carries one platform's natives; rebuild with `-Pacbric.lwjglNatives=...` for another target |
| Game version | Migration baseline is 1.2.14; it also ran on 1.2.15.2 (the framework reports 1.2.15.2) |

## 7. Troubleshooting

| Symptom | Check |
|---|---|
| `HARD_DEP_NO_CANDIDATE ... acbric_api` | The instance's `acbric-api.jar` is older than the mod declares. Install a matching/newer API; do not just raise the mod's `depends` |
| Self-test `result: FAIL` | Is the mod in `<instanceDir>/mods`? Does another jar also provide the same classes? |
| Still Slick2D after launch | Does `[acbric_engine_lwjgl3]` appear? Print the report with `-Dacbric.engine.quiet=false` |
| Odd shading / missing parts | Do `data/*.legacy-glsl.bak` files exist (means upgraded)? Revert with `gradlew restoreLegacyShaders` |
| Taskbar stays visible in borderless fullscreen | Expected fallback. Add `--enable-preview --enable-native-access=ALL-UNNAMED` |
| Fail hard instead of warning | `-Dacbric.engine.strict=true` |
