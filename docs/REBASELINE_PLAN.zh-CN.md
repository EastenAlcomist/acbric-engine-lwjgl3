# 基线重置与 mixin 化方案

[English](REBASELINE_PLAN.md) · 前置阅读：[来源与改造](MIGRATION_PROVENANCE.md) · [验证记录](VERIFICATION.zh-CN.md)

本页记录两项已确认的方向及其落地步骤。**当前代码尚未按本页实施**，现状见「五、现状与差距」。

## 一、决定 1：把迁移重新基线到游戏 1.2.15.2

### 为什么必须做

迁移工程的源码是 **1.2.14**（`AGame.VERSION = "1.2.14"`），而 Acbric 的 `libs/asplit-A/B.zip`
是 **1.2.15.2**（框架启动日志报 `Game 1.2.15.2`）。两者不是同一版本，后果有两层：

1. **运行期**：1.2.15.2 的类会调用 1.2.14 没有的成员。已经踩到并回填了两处
   （`AirshipGame.getClient()`、`CityUpgradeType.defenceBudget`），但这类缺口无法穷举——
   只要还有 1.2.14 派生的类留在 MOD 里，缺口就可能继续出现。
2. **mixin 化**：mixin 要求目标方法**逐字匹配**。拿 1.2.14 的源码差异去写 1.2.15.2 的
   `@Inject` / `@Redirect`，注入点本身就是错的。**这是本方案的前置条件。**

### 步骤

1. 从 `libs/asplit-A.zip` + `asplit-B.zip` 反编译出 `com/zarkonnen/airships/**` 的完整源码
   （约 631 个顶层类），作为新的基线快照 `baseline-1.2.15.2/`，与 1.2.14 的 `src.zip` 并列保存。
2. 对新基线跑一遍现有的逐 hunk 归一化分类流程（见 `MIGRATION_PROVENANCE.md`），
   得到**面向 1.2.15.2 的**「真正需要改」清单。
3. 用该清单替换当前 33 个类的纳管集：不在清单里的类一律退回游戏 jar。
4. 重新核对二进制兼容性（`javap` 比对纳管类与 `asplit-*.zip` 的公开成员），
   把 1.2.15.2 才有的成员按字节码回填。
5. 重跑 `engineSelfTest` + 实机启动，并把结果写入 `VERIFICATION`。

> 反编译工具与许可证：需确认所选反编译器（CFR / Vineflower 等）的输出可接受。
> 这一步只用于**差异分析**，不进仓库、不进发行包。

## 二、决定 2：GL 那 17 个类改在 GL 层解决，不写游戏类 mixin

### 现状

33 个纳管类里有 17 个只做了一件事：把 `GL11.x` 换成 `GLCompat.x`。
涉及的**全部 GL 函数只有 15 个**：

| 类别 | 函数 |
|---|---|
| 立即模式 | `glBegin` `glEnd` `glVertex2d` `glVertex2f` `glTexCoord2d` `glColor3f` `glColor4f` |
| 纹理/状态 | `glBindTexture` `glEnable` `glDisable` |
| 通用顶点属性 | `glVertexAttrib1f` `glVertexAttrib2f` `glVertexAttrib3f` `glVertexAttrib4f` |

### 为什么不写 mixin

- 调用点约 200–400 处，`@Redirect` 需要逐（目标类, 目标方法）写 handler；
- `AGENTS.md` 已记录：**同一调用点只允许一个 `@Redirect`，冲突是 WARN 级静默失败**；
- `Appearance`、`ShipLayers` 已被 `moduleHiRes`、`turretRotation` 注入，正是冲突高发区。

### 方案：让 `org.lwjgl.opengl.GL11` 自己路由

本 MOD 已经用「同名类覆盖」的手法提供了 `org.lwjgl.opengl.Display` / `DisplayMode` shim。
同样地，可以在 MOD 内提供一个 **`org.lwjgl.opengl.GL11` shim**，把这 15 个函数转到 `GLCompat`，
其余原样保留。这样**原版游戏类一行不用动**，也不需要任何游戏类 mixin。

实现要点：

1. **构建期用 ASM 生成**：取 `lwjgl-opengl-3.4.2.jar` 里的 `org/lwjgl/opengl/GL11.class`，
   只改写这 15 个方法的字节码为 `invokestatic com/zarkonnen/catengine/lwjgl3/GLCompat.x`，
   其余方法（含 `native`）保持原样。**类名不变**，JNI 绑定不受影响。
   ASM 已可用（框架 `loader-libs` 带 `asm-9.8.jar`）。
2. **绕开递归陷阱**：`GLCompat` 目前用**全限定名**调真实 GL——
   `org.lwjgl.opengl.GL11.glEnable(cap)`、`glDisable`、`glBindTexture`（见 `GLCompat.java` 269/273/277/325 行）。
   一旦 `GL11` 被 shim 覆盖，这些调用会**转回 GLCompat 自己**，无限递归。
   必须把这些调用改到未被覆盖的 `org.lwjgl.opengl.GL11C`（LWJGL3 的 core 变体）。
3. **静态导入仍可用**：`GLCompat` 顶部有 `import static org.lwjgl.opengl.GL11.*;`。
   shim 覆盖后，未路由的函数（`glGenBuffers`、`glBufferData`、`glDrawArrays` …）必须仍然可用，
   所以 shim 必须是 **GL11 的完整面**，不能只手写那 15 个。
4. **验证**：`engineSelfTest` 增加断言——`org.lwjgl.opengl.GL11` 必须由本 MOD 提供，
   且 `GL11C` 必须来自 LWJGL3 jar；再实机跑一遍确认渲染无回归。

做完这一步，那 17 个类就可以**从 MOD 中删除**，纳管数从 33 降到 16。

## 三、剩下的 16 个类：写 mixin

剥掉 GL 那 17 个之后，剩余真实改动集中在三类：

| 子类 | 大致文件 | 备注 |
|---|---|---|
| 引擎/输入接线 | `AirshipGame`、`Main`、`Mod`、`Expansion`、`CombatSoundEffects`、`FBOGraphicsFactory` | `SlickEngine` → `Lwjgl3Engine`；`Main` 里的引擎构造点是关键注入点 |
| 迁移期缺陷修复 | `AGame`、`LaunchSettings`、`Keys`、`BonusableValue`、`CityUpgradeType`、`Job`、`ResChooserWidget`、`StrategicScreen`、`DiplomacyAI` | 多为单点 `@Inject` / `@ModifyArg` |
| 着色器/渲染辅助 | `Appearance`（3 处非 GL）、`MyDraw`、`ShapeUtils`、`SaveHelperWidget`、`TechScreen`、`CampaignStatsDisplay`、`FlagTestScreen`、`Particle` | 需与 GL 层方案一起复核 |

写 mixin 时的既有约束（见 `AGENTS.md`）：没有 `@Local`；同调用点只允许一个 `@Redirect`；
`@ModifyArg`/`@ModifyVariable` 在节点被 `@Redirect` 替换后会抛异常；优先 `@Inject`。

## 四、顺序

1. **基线重置**（决定 1）——不做这一步，后面全是错的参照。
2. **GL 层路由**（决定 2）——独立于基线，可与第 1 步并行；做完删掉 17 个类。
3. **剩余类的 mixin 化**——依赖前两步的结论。
4. 每步都过 `engineSelfTest` + 实机启动；证据写进 `VERIFICATION`。

## 五、现状与差距

| 项 | 现状 | 目标 |
|---|---|---|
| 纳管游戏类 | 33 个源码文件 | 0 个（全部走 GL 层 + mixin） |
| 版本基线 | 迁移源码 1.2.14 / 游戏 1.2.15.2，已回填 2 处 API | 统一到 1.2.15.2 |
| GL 路由 | 17 个类各自源码内 `GL11.x`→`GLCompat.x` | `GL11` shim 统一路由 |
| 游戏类 mixin | 无 | 覆盖剩余 16 个类的真实改动 |
| 实测 | 自检 PASS；实机 95 秒 0 异常 | 同上，且在基线重置后重跑 |
