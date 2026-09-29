# Rebasing and mixin plan

[中文](REBASELINE_PLAN.zh-CN.md) · Read first: [provenance](MIGRATION_PROVENANCE.md) · [verification](VERIFICATION.md)

This page records two confirmed directions and how to land them. **The code does not implement them yet**;
see "5. Current state vs target".

## 1. Decision 1: rebase the migration onto game 1.2.15.2

### Why it is mandatory

The migration source is **1.2.14** (`AGame.VERSION = "1.2.14"`) while Acbric's
`libs/asplit-A/B.zip` is **1.2.15.2** (the framework logs `Game 1.2.15.2`). There are two consequences:

1. **At runtime**, 1.2.15.2 classes call members 1.2.14 does not have. Two have already been hit and
   back-ported (`AirshipGame.getClient()`, `CityUpgradeType.defenceBudget`), but such gaps cannot be
   enumerated — as long as 1.2.14-derived classes remain in the mod, more can appear.
2. **For mixins**, injection requires an exact bytecode match. Writing 1.2.15.2 `@Inject` / `@Redirect`
   against 1.2.14 source diffs means the injection points themselves are wrong.
   **This is the prerequisite for the whole plan.**

### Steps

1. Decompile `com/zarkonnen/airships/**` from `libs/asplit-A.zip` + `asplit-B.zip` into a new baseline
   snapshot `baseline-1.2.15.2/`, kept beside the 1.2.14 `src.zip`.
2. Run the existing per-hunk normalised classification (see `MIGRATION_PROVENANCE.md`) against the new
   baseline to get the **1.2.15.2-oriented** "actually needs changing" list.
3. Replace today's 33-class takeover set with that list; anything not on it goes back to the game jars.
4. Re-run the binary-compatibility check (`javap` against `asplit-*.zip`) and back-port any 1.2.15.2-only
   members faithfully from bytecode.
5. Re-run `engineSelfTest` plus a real launch and record the result in `VERIFICATION`.

> Decompiler choice and licence need a decision. This step is for **diff analysis only** — nothing from
> it enters the repository or the distribution.

## 2. Decision 2: solve the 17 GL classes at the GL layer, not with game mixins

### Current state

17 of the 33 taken-over classes only swap `GL11.x` for `GLCompat.x`. The entire GL surface involved is
**15 functions**:

| Kind | Functions |
|---|---|
| Immediate mode | `glBegin` `glEnd` `glVertex2d` `glVertex2f` `glTexCoord2d` `glColor3f` `glColor4f` |
| Texture / state | `glBindTexture` `glEnable` `glDisable` |
| Generic vertex attributes | `glVertexAttrib1f` `glVertexAttrib2f` `glVertexAttrib3f` `glVertexAttrib4f` |

### Why not mixins

- ~200–400 call sites; `@Redirect` needs one handler per (target class, target method);
- `AGENTS.md` already records that **only one `@Redirect` is allowed per call site and conflicts fail
  silently at WARN level**;
- `Appearance` and `ShipLayers` are already injected by `moduleHiRes` and `turretRotation` — exactly the
  conflict hotspots.

### Approach: make `org.lwjgl.opengl.GL11` route itself

The mod already ships same-named shims for `org.lwjgl.opengl.Display` / `DisplayMode`. In the same way it
can ship a **`org.lwjgl.opengl.GL11` shim** that routes these 15 functions to `GLCompat` and leaves the
rest untouched. **No vanilla game class changes and no game-class mixin is needed.**

Implementation notes:

1. **Generate with ASM at build time**: take `org/lwjgl/opengl/GL11.class` from
   `lwjgl-opengl-3.4.2.jar`, rewrite only those 15 method bodies to
   `invokestatic com/zarkonnen/catengine/lwjgl3/GLCompat.x`, leave everything else (including `native`
   methods) untouched. The **class name is unchanged**, so JNI binding is unaffected.
   ASM is already available (`asm-9.8.jar` in the framework's `loader-libs`).
2. **Avoid the recursion trap**: `GLCompat` currently calls the real GL through fully-qualified names —
   `org.lwjgl.opengl.GL11.glEnable(cap)`, `glDisable`, `glBindTexture` (`GLCompat.java` lines 269/273/277/325).
   Once `GL11` is shadowed those calls route back into `GLCompat` itself, recursing forever. They must be
   moved to the unshadowed `org.lwjgl.opengl.GL11C` (LWJGL3's core variant).
3. **Static imports must keep working**: `GLCompat` has `import static org.lwjgl.opengl.GL11.*;`. After the
   shadowing, non-routed functions (`glGenBuffers`, `glBufferData`, `glDrawArrays`, …) must still resolve,
   so the shim has to be the **complete GL11 surface**, not just those 15 methods.
4. **Verification**: add self-test assertions that `org.lwjgl.opengl.GL11` is provided by this mod and
   `GL11C` comes from the LWJGL3 jar, then do a real launch to confirm no rendering regression.

After this step those 17 classes can be **deleted from the mod**, dropping the takeover count from 33 to 16.

## 3. The remaining 16 classes: write mixins

Once the 17 GL classes are gone, the real changes fall into three groups:

| Group | Roughly | Notes |
|---|---|---|
| Engine / input wiring | `AirshipGame`, `Main`, `Mod`, `Expansion`, `CombatSoundEffects`, `FBOGraphicsFactory` | `SlickEngine` → `Lwjgl3Engine`; the engine construction site in `Main` is the key hook |
| Migration-period fixes | `AGame`, `LaunchSettings`, `Keys`, `BonusableValue`, `CityUpgradeType`, `Job`, `ResChooserWidget`, `StrategicScreen`, `DiplomacyAI` | mostly single-point `@Inject` / `@ModifyArg` |
| Shader / render helpers | `Appearance` (3 non-GL spots), `MyDraw`, `ShapeUtils`, `SaveHelperWidget`, `TechScreen`, `CampaignStatsDisplay`, `FlagTestScreen`, `Particle` | review together with the GL-layer work |

Existing mixin constraints apply (see `AGENTS.md`): no `@Local`; one `@Redirect` per call site;
`@ModifyArg`/`@ModifyVariable` throw when the node was replaced by `@Redirect`; prefer `@Inject`.

## 4. Order

1. **Rebase** (decision 1) — without it every later reference is wrong.
2. **GL-layer routing** (decision 2) — independent of the rebase, can run in parallel; deletes 17 classes.
3. **Mixin the remaining classes** — depends on the first two.
4. Every step must pass `engineSelfTest` plus a real launch, with evidence recorded in `VERIFICATION`.

## 5. Current state vs target

| Item | Now | Target |
|---|---|---|
| Taken-over game classes | 33 source files | 0 (all via the GL layer + mixins) |
| Version baseline | migration 1.2.14 / game 1.2.15.2, two API back-ports | unified on 1.2.15.2 |
| GL routing | 17 classes each swapping `GL11.x`→`GLCompat.x` in source | a single `GL11` shim |
| Game-class mixins | none | covering the remaining 16 classes |
| Measured | self-test PASS; 95 s launch, 0 exceptions | same, re-run after the rebase |
