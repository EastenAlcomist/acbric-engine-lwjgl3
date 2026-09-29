#!/usr/bin/env bash
# ci-guards.sh —— 不需要游戏文件的静态守卫。
#
# 为什么需要它：本工程编译需要自有的游戏 class 与已构建的 Acbric 框架，
# **GitHub 托管 runner 上拿不到这些文件**（见 docs/RELEASING.zh-CN.md）。
# 所以 CI 能对每个 PR 做的是「守住仓库不该出现的东西 + 守住几条硬约束」，
# 完整构建交给自托管 runner 或本地发布脚本。
#
# 用法：bash tools/ci-guards.sh
set -u

fail=0
note() { printf '  %s\n' "$*"; }
ok()   { printf '  [ ok ] %s\n' "$*"; }
bad()  { printf '  [FAIL] %s\n' "$*"; fail=$((fail + 1)); }

echo "== 1. 仓库内容策略（游戏/框架内容与构建产物不得入库）=============="
forbidden_tracked=$(git ls-files -- libs game userdata runtime local.properties gradle.properties 2>/dev/null || true)
if [ -n "$forbidden_tracked" ]; then
    bad "以下路径被 Git 跟踪，必须移除："
    echo "$forbidden_tracked" | sed 's/^/         /'
else
    ok "libs/ game/ userdata/ runtime/ local.properties gradle.properties 均未被跟踪"
fi

binaries=$(git ls-files | grep -Ei '[.](jar|zip|7z|exe|dll|class)$' | grep -v '^gradle/wrapper/gradle-wrapper.jar$' || true)
if [ -n "$binaries" ]; then
    bad "跟踪了二进制依赖（只允许 gradle/wrapper/gradle-wrapper.jar）："
    echo "$binaries" | sed 's/^/         /'
else
    ok "除 gradle wrapper 外无二进制依赖入库"
fi

echo
echo "== 2. 换行 ====================================================="
if git ls-files --eol gradlew | grep -q 'w/crlf'; then
    bad "gradlew 在工作区是 CRLF；Linux/macOS 会报 bad interpreter"
else
    ok "gradlew 换行为 LF"
fi

echo
echo "== 3. fabric.mod.json ==========================================="
FMJ=src/main/resources/fabric.mod.json
if [ ! -f "$FMJ" ]; then
    bad "缺少 $FMJ"
else
    grep -q '"id": "acbric_engine_lwjgl3"' "$FMJ" && ok "MOD ID 正确" || bad "MOD ID 不是 acbric_engine_lwjgl3"
    grep -q '"version": "[$]{version}"' "$FMJ" && ok "版本用占位符（由 Gradle 展开），未写死" || bad "版本必须写成由 Gradle 展开的占位符"
    grep -q 'net.fabricacs.engine.EngineBootstrap' "$FMJ" && ok "声明了 preLaunch 入口" || bad "缺少 preLaunch 入口 EngineBootstrap"
    grep -q 'net.fabricacs.engine.Lwjgl3EngineMod' "$FMJ" && ok "声明了 acbric 入口" || bad "缺少 acbric 入口 Lwjgl3EngineMod"
    note "API 下限：$(grep -o '"acbric_api": *"[^"]*"' "$FMJ" | head -1)"
fi

echo
echo "== 4. 着色器包 =================================================="
SH=src/main/resources/acbric_engine_data/shaders
if [ ! -d "$SH" ]; then
    bad "缺少 $SH"
else
    total=$(find "$SH" -maxdepth 1 -type f \( -name '*.vert' -o -name '*.frag' \) | wc -l | tr -d ' ')
    other=$(find "$SH" -maxdepth 1 -type f ! -name '*.vert' ! -name '*.frag' | wc -l | tr -d ' ')
    note "着色器文件：$total 个（目录内其他文件 $other 个）"
    [ "$total" -gt 0 ] && ok "着色器非空" || bad "着色器目录为空"

    no_version=$(grep -LE '#version' "$SH"/*.vert "$SH"/*.frag 2>/dev/null || true)
    if [ -n "$no_version" ]; then
        bad "以下着色器没有 #version 指令（core profile 下会编译失败）："
        echo "$no_version" | sed 's/^/         /'
    else
        ok "全部着色器含 #version"
    fi

    not330=$(grep -LE '#version +330 +core' "$SH"/*.vert "$SH"/*.frag 2>/dev/null || true)
    if [ -n "$not330" ]; then
        bad "以下着色器不是 #version 330 core："
        echo "$not330" | sed 's/^/         /'
    else
        ok "全部着色器为 #version 330 core"
    fi
fi

echo
echo "== 5. preview API 隔离（AGENTS.md 硬约束）========================"
# 只有 WindowsTaskbarFfm 允许使用 JDK 21 preview 的 FFM API；
# 其他文件一旦引入，class 会被打上 preview 标记，整个 MOD 就硬依赖 --enable-preview。
foreign=$(grep -rl 'java[.]lang[.]foreign' src/main/java || true)
unexpected=$(echo "$foreign" | grep -v 'WindowsTaskbarFfm.java' | grep -v '^$' || true)
if [ -n "$unexpected" ]; then
    bad "以下文件使用了 java.lang.foreign，必须隔离到 WindowsTaskbarFfm："
    echo "$unexpected" | sed 's/^/         /'
else
    ok "preview FFM 仅出现在 WindowsTaskbarFfm"
fi

echo
echo "== 5b. 游戏源码纳管范围（漂移守卫）=============================="
# 本 MOD 只接管迁移"真正改过"的游戏类。这个数字是逐 hunk 分类得出的基线：
# 165 个源文件里有语义差异的只有 33 个，其余 132 个编译产物等价，直接用游戏 jar 的版本。
# 数字变了必须是有意为之：要么重新跑分类，要么说明为什么。
EXPECTED_AIRSHIPS_SOURCES=16
actual=$(find src/main/java/com/zarkonnen/airships -name '*.java' 2>/dev/null | wc -l | tr -d ' ')
if [ "$actual" -eq "$EXPECTED_AIRSHIPS_SOURCES" ]; then
    ok "纳管的游戏类仍是基线 $EXPECTED_AIRSHIPS_SOURCES 个"
else
    bad "纳管的游戏类从 $EXPECTED_AIRSHIPS_SOURCES 变成了 $actual —— 说明纳管范围漂移了。"
    echo "         要么是有人把无语义差异的类又拷了回来（应删掉，改用游戏 jar 的版本），"
    echo "         要么是迁移基线更新了（那要重新跑 docs/MIGRATION_PROVENANCE.md 里的分类流程，"
    echo "         并同步这个基线数字与 docs/MIGRATION_FILES.txt）。"
fi
if [ -f docs/MIGRATION_FILES.txt ]; then
    ok "docs/MIGRATION_FILES.txt 存在（改动清单）"
else
    bad "缺少 docs/MIGRATION_FILES.txt"
fi

echo
echo "== 6. LWJGL2 残留 =============================================="
lwjgl2=$(grep -rn 'import org[.]lwjgl[.]\(LWJGLException\|Sys\|input[.]\)' src/main/java || true)
if [ -n "$lwjgl2" ]; then
    bad "源码仍引用 LWJGL2 专有类："
    echo "$lwjgl2" | sed 's/^/         /'
else
    ok "无 LWJGL2 专有类引用"
fi

echo
echo "== 7. Markdown 相对链接 ========================================"
broken_file=$(mktemp)
while IFS= read -r f; do
    dir=$(dirname "$f")
    grep -oE '][(][^)]+[)]' "$f" | sed 's/^](//; s/)$//' | while IFS= read -r link; do
        case "$link" in
            http://*|https://*|mailto:*|'#'*) continue ;;
        esac
        target="$(printf '%s' "$link" | cut -d'#' -f1)"
        [ -z "$target" ] && continue
        if [ ! -e "$dir/$target" ]; then
            printf '  [FAIL] %s -> %s\n' "$f" "$link" >> "$broken_file"
        fi
    done
done < <(git ls-files '*.md')
if [ -s "$broken_file" ]; then
    cat "$broken_file"
    bad "$(wc -l < "$broken_file") 个相对链接无法解析"
else
    ok "所有相对链接均可解析"
fi
rm -f "$broken_file"

echo
echo "== 8. 版本注入 ================================================="
if grep -q 'MOD_VERSION' build.gradle; then
    ok "build.gradle 读取 MOD_VERSION 环境变量；本次 MOD_VERSION=${MOD_VERSION:-<未设置>}"
else
    bad "build.gradle 未读取 MOD_VERSION，CI 无法按 tag 注入版本"
fi

echo
if [ "$fail" -eq 0 ]; then
    echo "GUARDS PASSED"
    exit 0
fi
echo "GUARDS FAILED: $fail 项"
exit 1
