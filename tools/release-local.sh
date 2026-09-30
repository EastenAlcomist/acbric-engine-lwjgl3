#!/usr/bin/env bash
# release-local.sh —— 本机构建 Acbric LWJGL3 Engine。
#
# 与 release-local.ps1 等价，供 Linux / macOS / Git Bash 使用。
# 存在的原因：构建需要自有的游戏文件与已构建的 Acbric 框架，CI 托管 runner 拿不到。
#
# 用法：
#   tools/release-local.sh 1.0.1
#   tools/release-local.sh 1.0.1 natives-windows natives-linux
set -euo pipefail

VERSION="${1:-}"
shift || true
NATIVES=("${@:-natives-windows}")

if [ -z "$VERSION" ]; then
    echo "用法：tools/release-local.sh <version> [natives ...]" >&2
    exit 2
fi
case "$VERSION" in
    [0-9]*.[0-9]*.[0-9]*) ;;
    *) echo "版本号格式不对：$VERSION（期望 1.0.1 或 1.0.1-dev.2）" >&2; exit 2 ;;
esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== 1/3 校验依赖 =="
./gradlew verifyInputs --console=plain

echo "== 2/3 构建 $VERSION（${NATIVES[*]}）=="
rm -rf dist && mkdir -p dist
for n in "${NATIVES[@]}"; do
    # 先清掉同分类的旧产物：否则 build/libs 里的历史版本会被下面的 glob 一起拷进 dist/
    rm -f "build/libs/"*"-$n.jar"
    ./gradlew jar --console=plain -PmodVersion="$VERSION" -Pacbric.lwjglNatives="$n" -PmodClassifier="$n"
    cp "build/libs/"*"-$n.jar" dist/
done

echo "== 3/3 生成校验和与 MANIFEST =="
cd dist
if command -v sha256sum >/dev/null 2>&1; then
    sha256sum ./*.jar > SHA256SUMS.txt
else
    shasum -a 256 ./*.jar > SHA256SUMS.txt
fi
cd "$ROOT"

SHADERS=$(find src/main/resources/acbric_engine_data/shaders -name '*.vert' -o -name '*.frag' | wc -l | tr -d ' ')
API=$(grep -o '"acbric_api": *"[^"]*"' src/main/resources/fabric.mod.json | head -1)
for f in dist/*.jar; do
    base=$(basename "$f" .jar)
    {
        echo "jar: $(basename "$f")"
        echo "version: $VERSION"
        echo "acbric_api: $API"
        echo "shaders: $SHADERS"
        echo "bytes: $(wc -c < "$f" | tr -d ' ')"
    } > "dist/MANIFEST-$base.txt"
done

ls -la dist
echo
echo "产物在 dist/。发布方式："
echo "  A) GitHub → Releases → Draft a new release，tag 填 v$VERSION，把 dist/ 里的文件拖进去"
echo "  B) 配置自托管 runner 后设置 ENGINE_BUILD_RUNNER，推 tag v$VERSION 由 release.yml 构建发布"
