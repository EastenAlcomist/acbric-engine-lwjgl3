# Releasing

[中文](RELEASING.zh-CN.md) · See also: [verification record](VERIFICATION.md) · [contributing](../CONTRIBUTING.md)

## 1. The constraint, up front

**Building this project requires game files you own. They are not in the repository and should not be uploaded.**

`verifyInputs` in `build.gradle` needs:

| Dependency | Comes from | Can it be committed? |
|---|---|---|
| `libs/asplit-A.zip` / `asplit-B.zip` (game classes) | your own Airships install | No — game content, explicitly excluded by `.gitignore` |
| `libs/*.jar` (game libraries, natives) | same | No |
| `Acbric-1.0-SNAPSHOT-api-mod.jar` | a built framework working copy | No — build output, and the framework itself needs the game files above |
| `loader-libs/` (fabric-loader / sponge-mixin / asm) | same | No |

So: **a GitHub-hosted runner cannot build this project.** That is a dependency-source problem, not a configuration one.
`.github/workflows/verify.yml` therefore only runs static guards; `release.yml`'s build job must run on a self-hosted runner.

## 2. Three routes

| Route | Builds on | Requires | Good for |
|---|---|---|---|
| **C Local release** (works today) | your machine | nothing | solo maintenance, infrequent releases. **Start here** |
| **A Self-hosted runner** | your machine (CI-scheduled) | one registered runner + one repo variable | "push a tag, get a release" |
| **B Private deps bundle** | GitHub-hosted runner | a private repo + a PAT secret | fully hosted automation |

All three produce the same thing: one self-contained jar per platform plus `SHA256SUMS.txt` and `MANIFEST-*.txt`.

## 3. Route C: local release (default)

```powershell
.\tools\release-local.ps1 -Version 1.0.1
.\tools\release-local.ps1 -Version 1.0.1 -Natives natives-windows,natives-linux
# Linux / macOS / Git Bash:
./tools/release-local.sh 1.0.1 natives-windows natives-linux
```

The script runs `verifyInputs`, builds each platform, writes checksums and MANIFESTs into `dist/`, then prints the next steps.

Then either:

1. **Web UI**: GitHub → Releases → *Draft a new release*, tag `v1.0.1`, drag the files from `dist/`;
2. **CLI**: add `-Publish` (needs [gh CLI](https://cli.github.com/)) and the script runs `gh release create`.

Add `-Tag` to also create and push the tag.

## 4. Route A: self-hosted runner (tag → automatic release)

1. Register a runner on a machine that **has the game and a framework working copy**:
   GitHub → repo Settings → Actions → Runners → *New self-hosted runner*; give it a label (say `windows-airships`).
2. Settings → Actions → Variables → *New repository variable*:

   | Name | Value |
   |---|---|
   | `ENGINE_BUILD_RUNNER` | `windows-airships` |
   | `ENGINE_FRAMEWORK_DIR` | optional; absolute path to the framework working copy on the runner |
   | `ENGINE_GAME_LIB_DIR` | optional; absolute path to the game library directory on the runner |

   When the last two are set the workflow **generates `local.properties` in place**, so nothing has to be
   maintained by hand on the runner; if they are unset it falls back to whatever `local.properties`
   the runner's checkout already has.

3. Configure `local.properties` on that machine. It is git-ignored, so a fresh checkout will not have it.
   The reliable approach is machine-level environment variables plus explicit `-P` flags in the workflow:

   ```yaml
   ./gradlew jar --console=plain \
     -PmodVersion="$MOD_VERSION" \
     -PframeworkDir="$FRAMEWORK_DIR" \
     -PgameLibDir="$GAME_LIB_DIR" \
     -Pacbric.lwjglNatives=... -PmodClassifier=...
   ```

4. Afterwards `git tag v1.0.1 && git push origin v1.0.1` makes `release.yml` build all four platforms on the runner,
   while a hosted runner assembles and publishes the Release (packing and publishing need no game files).

> With `ENGINE_BUILD_RUNNER` unset the `build` job is skipped and `publish` **fails loudly**, printing these three
> routes — it never silently produces an empty Release.

## 5. Route B: private deps bundle (fully hosted)

To build on a hosted runner the dependencies must be downloadable:

1. Create a **private** repo (e.g. `airships-ci-deps`) with a Release carrying
   `asplit-A.zip`, `asplit-B.zip`, `game-libs.zip` (the jars in `libs/` other than asplit and fabric-loader),
   `acbric-api.jar` and `loader-libs.zip`.
2. Create a fine-grained PAT with `contents: read` on that private repo and store it as the Actions secret `DEPS_TOKEN`.
3. Add an unpack step before the `build` job body in `release.yml`:

   ```yaml
   - name: Fetch private dependencies
     env:
       GH_TOKEN: ${{ secrets.DEPS_TOKEN }}
     run: |
       gh release download deps -R <owner>/airships-ci-deps -D .deps
       mkdir -p libs game
       cp .deps/asplit-*.zip libs/
       unzip -q .deps/game-libs.zip -d libs/
       mkdir -p framework/build/libs framework/build/dist/Acbric
       unzip -q .deps/loader-libs.zip -d framework/build/dist/Acbric/loader-libs/
       cp .deps/acbric-api.jar framework/build/libs/Acbric-1.0-SNAPSHOT-api-mod.jar
   ```

   and switch the `build` job to `runs-on: ubuntu-latest` with an always-true `if`.

   > Be clear about the trade: **this uploads game classes and the framework distribution to GitHub (privately)**.
   > The upstream framework does not even commit `libs/`, so only take this route if you accept that.

## 6. Versioning

- There is one source of truth: **the tag**. `v1.0.1` → `1.0.1`.
- Priority: `-PmodVersion` > env `MOD_VERSION` (injected by the workflow) > `gradle.properties` > `1.0.0`.
- `fabric.mod.json` holds a placeholder that Gradle expands at packaging time — do not edit it by hand.
- For pre-releases use a tag like `v1.1.0-dev.1` and tick *pre-release* on GitHub.

## 7. What CI guards

`verify.yml` runs `tools/ci-guards.sh` on every push / PR (you can run it locally too);
`release.yml` runs it again before building and additionally checks that `fabric.mod.json` expands to valid JSON.

| Guard | Why |
|---|---|
| `libs/`, `game/`, `userdata/`, `runtime/`, `local.properties`, `gradle.properties` untracked | game content and machine paths must not enter the repo |
| no binaries tracked except `gradle-wrapper.jar` | dependencies come from Maven or local paths |
| `gradlew` is LF | CRLF breaks Linux/macOS with "bad interpreter" |
| `fabric.mod.json`: id, version placeholder, both entrypoints | otherwise the packaged mod is not recognised |
| all 36 shaders carry `#version 330 core` | legacy shaders fail to link on a core profile (learned the hard way) |
| **`java.lang.foreign` only in `WindowsTaskbarFfm`** | if it spreads, the whole mod hard-depends on `--enable-preview` (learned the hard way) |
| no LWJGL2-only class references | `org.lwjgl.LWJGLException` / `org.lwjgl.input.*` mean the migration is incomplete |
| every Markdown relative link resolves | docs moved around; no dead links |

## 8. First-release checklist

1. `local.properties` points at `frameworkDir` / `gameLibDir` / `instanceDir`;
2. `gradlew verifyInputs` passes;
3. `gradlew engineSelfTest` exits 0 (headless, no display);
4. `bash tools/ci-guards.sh` prints `GUARDS PASSED`;
5. one real launch showing `result: PASS` and a `log.txt` free of shader failures/exceptions;
6. `tools/release-local.ps1 -Version x.y.z`;
7. create the Release via the web UI or `gh`;
8. state **what was not tested** in the release notes (see the last section of `docs/VERIFICATION.md`).
