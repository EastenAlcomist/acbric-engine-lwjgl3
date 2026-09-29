# 贡献指南 / Contributing

[English](CONTRIBUTING.md)

## 环境

- **JDK 21**（必须；迁移版用 JDK 21 preview 的 FFM API）。若系统 PATH 不是 21，
  在 `gradle.properties` 里设置 `org.gradle.java.home`。
- 一份**已构建**的 Acbric 框架工作副本：需要 `build/libs/Acbric-1.0-SNAPSHOT-api-mod.jar`
  与 `build/dist/Acbric/loader-libs/`。
- 自有的 Airships 安装：`asplit-A.zip` / `asplit-B.zip` 与游戏自带库。
- 一个可写的 Fabric 实例目录（`<instanceDir>/mods`、`<instanceDir>/data`）。

路径写在 `local.properties`（已被 Git 排除），模板见 `gradle.properties.example`。

## 提交前

1. `gradlew.bat verifyInputs` 通过；
2. `gradlew.bat build` 通过；
3. `gradlew.bat engineSelfTest` 退出码 0（无头，不需要显示器）；
4. 涉及类替换或着色器时，补一次真机启动并把 `log.txt` 结果写进 PR 的「验证」一节；
5. 中英文文档同步更新。

## 不要做的事

- 不要把 `libs/`、`game/`、`local.properties`、构建产物、本地运行日志提交进仓库。
- 不要把游戏/框架类打进 MOD 源码发行包（MOD JAR 内自带引擎类是有意为之，见 README）。
- 不要新增对 `--enable-preview` 的硬依赖（见 `AGENTS.md`）。
