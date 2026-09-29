# Contributing

[中文](CONTRIBUTING.zh-CN.md)

## Environment

- **JDK 21** (required; the migrated code uses JDK 21 preview FFM APIs). If your PATH JDK is
  not 21, set `org.gradle.java.home` in `gradle.properties`.
- A **built** Acbric framework working copy: it must provide
  `build/libs/Acbric-1.0-SNAPSHOT-api-mod.jar` and `build/dist/Acbric/loader-libs/`.
- Your own Airships installation: `asplit-A.zip` / `asplit-B.zip` plus the game's library jars.
- A writable Fabric instance directory (`<instanceDir>/mods`, `<instanceDir>/data`).

Put the paths in `local.properties` (git-ignored); see `gradle.properties.example`.

## Before opening a PR

1. `gradlew.bat verifyInputs` passes;
2. `gradlew.bat build` passes;
3. `gradlew.bat engineSelfTest` exits 0 (headless, no display needed);
4. if you touched class shadowing or shaders, add a real launch and record the `log.txt`
   result under "Validation" in the PR;
5. keep the English and Chinese docs in sync.

## Do not

- commit `libs/`, `game/`, `local.properties`, build output or local run logs;
- pack game/framework classes into the MOD *source* distribution (shipping engine classes
  inside the MOD jar is intentional — see README);
- introduce a hard dependency on `--enable-preview` (see `AGENTS.md`).
