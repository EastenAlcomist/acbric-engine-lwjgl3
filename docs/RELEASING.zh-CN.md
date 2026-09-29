# 发布流程

[English](RELEASING.md) · 相关：[验证记录](VERIFICATION.zh-CN.md) · [贡献指南](../CONTRIBUTING.zh-CN.md)

## 一、先说清楚那个约束

**这个工程的构建需要「自有的游戏文件」，而它们不在仓库里，也不该上传。**

`build.gradle` 的 `verifyInputs` 需要：

|`依赖`|来源|能不能进仓库|
|---|---|---|
| `libs/asplit-A.zip` / `asplit-B.zip`（游戏 class） | 自有 Airships 安装 | ❌ 游戏本体，`.gitignore` 明确排除 |
| `libs/*.jar`（游戏自带库、native） | 同上 | ❌ |
| `Acbric-1.0-SNAPSHOT-api-mod.jar` | 已构建的框架工作副本 | ❌ 构建产物，且框架本身也需要上面的游戏文件 |
| `loader-libs/`（fabric-loader / sponge-mixin / asm） | 同上 | ❌ |

结论：**GitHub 托管 runner 上无法完成构建**。这不是配置问题，是依赖来源问题。
所以 `.github/workflows/verify.yml` 只做静态守卫、不尝试构建；
`release.yml` 的构建作业必须跑在自托管 runner 上。

## 二、三条路线

|`路线`|构建在哪|需要什么|适合|
|---|---|---|---|
| **C 本地发布**（默认可用） | 你的机器 | 无 | 一个人维护、发布不频繁。**推荐先用这条** |
| **A 自托管 runner** | 你的机器（CI 调度） | 注册一台 runner + 一个仓库变量 | 想要「推 tag 即自动出包」 |
| **B 私有依赖包** | GitHub 托管 runner | 一个私有仓库 + PAT secret | 想让完全托管 runner 全自动跑 |

三条路线的**版本号与产物形态完全一致**：四个平台各一个自包含 JAR + `SHA256SUMS.txt` + `MANIFEST-*.txt`。

## 三、路线 C：本地发布（默认）

```powershell
.\tools\release-local.ps1 -Version 1.0.1
.\tools\release-local.ps1 -Version 1.0.1 -Natives natives-windows,natives-linux
# Linux / macOS / Git Bash：
./tools/release-local.sh 1.0.1 natives-windows natives-linux
```

脚本做四件事：`verifyInputs` → 逐平台 `jar` → 生成 `dist/` 里的校验和与 MANIFEST → 打印后续步骤。

然后二选一：

1. **网页**：GitHub → Releases → *Draft a new release*，tag 填 `v1.0.1`，把 `dist/` 里的文件拖进去；
2. **命令行**：加 `-Publish`（需要 [gh CLI](https://cli.github.com/)），脚本会 `gh release create`。

想顺便打 tag 并推送：加 `-Tag`。

## 四、路线 A：自托管 runner（推 tag 即自动发包）

1. 在一台**有游戏与框架工作副本**的机器上注册 runner：
   GitHub → 仓库 Settings → Actions → Runners → *New self-hosted runner*，按提示装好；
   建议给它打一个标签（下面示例用 `windows-airships`）。

2. 仓库 Settings → Actions → Variables → *New repository variable*：

   | Name | Value |
   |---|---|
   | `ENGINE_BUILD_RUNNER` | `windows-airships`（你的 runner 标签） |
   | `ENGINE_FRAMEWORK_DIR` | 可选。runner 上 Acbric 框架工作副本的绝对路径 |
   | `ENGINE_GAME_LIB_DIR` | 可选。runner 上游戏库目录的绝对路径 |

   后两个变量存在时，workflow 会**就地生成 `local.properties`**，runner 上不需要手工维护该文件；
   都不设时就用 runner 工作副本里已有的 `local.properties`。

3. 在那台机器上把 `local.properties` 配好（它被 Git 排除，所以 runner 的工作区里不会有，
   需要放在 runner 能读到的地方）。两种做法：
   - 让 runner 以某个用户身份运行，并在**仓库工作副本**里放一份 `local.properties`；
     注意 Actions 每次 checkout 到新目录，更稳的是用**绝对路径的环境变量**；
   - 或者给 runner 设机器级环境变量，让 workflow 用 `-P` 覆盖：见下。

   最省事的做法是给 runner 机器加环境变量，然后在 workflow 里显式传参。
   如果你的路径固定，可以直接在 `release.yml` 的构建步骤里加：

   ```yaml
   ./gradlew jar --console=plain \
     -PmodVersion="$MOD_VERSION" \
     -PframeworkDir="$FRAMEWORK_DIR" \
     -PgameLibDir="$GAME_LIB_DIR" \
     -Pacbric.lwjglNatives=... -PmodClassifier=...
   ```

4. 之后：`git tag v1.0.1 && git push origin v1.0.1` → `release.yml` 在 runner 上矩阵构建四个平台，
   由托管 runner 汇总并创建 Release（打包与发布不需要游戏文件，所以放在托管 runner 上）。

> 未设置 `ENGINE_BUILD_RUNNER` 时，`build` 作业会被跳过，`publish` 会**明确失败**并打印这三条路线，
> 不会静默产出一个空 Release。

## 五、路线 B：私有依赖包（托管 runner 全自动）

想在 GitHub 托管 runner 上构建，就得让依赖能被下载。做法：

1. 建一个**私有**仓库（例如 `airships-ci-deps`），发一个 Release，附件放：
   `asplit-A.zip`、`asplit-B.zip`、`game-libs.zip`（`libs/` 下除 asplit 与 fabric-loader 外的 jar）、
   `acbric-api.jar`、`loader-libs.zip`。
2. 生成一个只对该私有仓库有 `contents: read` 权限的 **fine-grained PAT**，
   存进本仓库的 Actions secret `DEPS_TOKEN`。
3. 在 `release.yml` 的 `build` 作业前面加一步解包：

   ```yaml
   - name: 拉取私有依赖
     env:
       GH_TOKEN: ${{ secrets.DEPS_TOKEN }}
     run: |
       gh release download deps -R <owner>/airships-ci-deps -D .deps
       mkdir -p libs game
       cp .deps/asplit-*.zip libs/
       unzip -q .deps/game-libs.zip -d libs/
       unzip -q .deps/loader-libs.zip -d framework/loader-libs/
       mkdir -p framework/build/libs framework/build/dist/Acbric
       cp .deps/acbric-api.jar framework/build/libs/Acbric-1.0-SNAPSHOT-api-mod.jar
       mv framework/loader-libs framework/build/dist/Acbric/loader-libs
   ```

   并把 `build` 作业的 `runs-on` 改成 `ubuntu-latest`、`if` 改成恒真。

   > 代价要自己想清楚：**这等于把游戏 class 与框架发行包上传到 GitHub（私有）**。
   > 上游框架自己都没有把 `libs/` 放进仓库，这条路线属于「你接受这个代价」才用。

## 六、版本号

- 版本只有一个来源：**tag**。`v1.0.1` → `1.0.1`。
- 优先级：`-PmodVersion` > 环境变量 `MOD_VERSION`（workflow 注入）> `gradle.properties` > `1.0.0`。
- `fabric.mod.json` 里写的是占位符，由 Gradle 在打包时展开成实际版本，所以**不要手改**。
- 预发布用 `v1.1.0-dev.1` 这种 tag，GitHub 上勾 *pre-release*。

## 七、CI 守卫查什么

`verify.yml` 每个 push / PR 都跑 `tools/ci-guards.sh`（本地也能直接跑）。
`release.yml` 在构建前再跑一次，并额外校验 `fabric.mod.json` 能被展开成合法 JSON。

|`守卫`|为什么|
|---|---|
| `libs/`、`game/`、`userdata/`、`runtime/`、`local.properties`、`gradle.properties` 未被跟踪 | 游戏内容与本机路径不得入库 |
| 除 `gradle-wrapper.jar` 外无二进制入库 | 依赖走 Maven 或本机路径 |
| `gradlew` 为 LF | CRLF 在 Linux/macOS 上会报 bad interpreter |
| `fabric.mod.json`：ID、版本占位符、两个入口 | 打包后 MOD 才认得出来 |
| 36 个着色器全部含 `#version 330 core` | core profile 下旧着色器会链接失败（踩过） |
| **`java.lang.foreign` 只出现在 `WindowsTaskbarFfm`** | 一旦扩散，整个 MOD 就硬依赖 `--enable-preview`（踩过） |
| 无 LWJGL2 专有类引用 | `org.lwjgl.LWJGLException` / `org.lwjgl.input.*` 说明迁移没做完 |
| 所有 Markdown 相对链接可解析 | 文档搬家后不留死链 |

## 八、首次发布 checklist

1. `local.properties` 指好 `frameworkDir` / `gameLibDir` / `instanceDir`；
2. `gradlew verifyInputs` 通过；
3. `gradlew engineSelfTest` 退出码 0（无头，不需要显示器）；
4. `bash tools/ci-guards.sh` 输出 `GUARDS PASSED`；
5. 真机启动一次，确认日志里 `result: PASS` 且 `log.txt` 无着色器失败/异常；
6. `tools/release-local.ps1 -Version x.y.z`；
7. 网页或 `gh` 创建 Release；
8. 在 release notes 里写清**没测什么**（见 `docs/VERIFICATION.zh-CN.md` 末节）。
