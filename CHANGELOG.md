# Changelog

## 1.0.1

**Narrowed the game source taken over: 165 → 33 classes.**

- Per-hunk normalised comparison of the 165 differing files found **132 with no semantic change**
  (IDE removal of redundant class qualifiers, `strictfp` removal, `new Integer`→`valueOf`,
  formatting, unrelated imports). Those classes are **removed from the mod** and now come from the
  game jars (`asplit-A/B.zip`), cutting the game-derived files in the source distribution by ~80%.
- 33 classes that really change behaviour are kept. Note that an import-only diff cannot be dropped
  blindly: `CombatSoundEffects` differs by a single import line, but that type participates in a
  cast, so its bytecode differs.
- The self-test gained inverse assertions: `Airship`, `City`, `CampaignWorld`, `Combat`,
  `ModuleType`, `SpritesheetBundle`, `Server` must resolve to the game jar, guarding against the
  takeover set growing back.
- `tools/ci-guards.sh` now fails when the taken-over class count drifts from the 33 baseline.
- Fixed the 1.2.15.2 API gaps exposed by the removal (see `docs/MIGRATION_PROVENANCE.md`):
  back-ported `AirshipGame.getClient()` and `CityUpgradeType.defenceBudget`, without which
  1.2.15.2's `CampaignWorld` / `City` / `HeroManagementAI` would throw `NoSuchMethodError` /
  `NoSuchFieldError`.
- Recorded the **unresolved** baseline mismatch: the migration source is 1.2.14 while
  `asplit-*.zip` is 1.2.15.2. This is the prerequisite for converting the changes to mixins.

Verified: build passes; `engineSelfTest` PASSES with assertions in both directions (migrated classes
from the mod, untouched classes from the game jar); a 95-second real launch produced **0 exceptions**
in `log.txt` with the main menu and OpenAL working.

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
