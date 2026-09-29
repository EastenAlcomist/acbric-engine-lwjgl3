<#
.SYNOPSIS
  本机构建 Acbric LWJGL3 Engine，可选直接发布。

.DESCRIPTION
  这个脚本存在的唯一原因：本工程的构建需要**自有的游戏文件**（asplit-A/B.zip 与游戏库）
  和**已构建的 Acbric 框架**，GitHub 托管 runner 上拿不到，也不该上传。
  所以「本地构建 + 发布」是最省事、零密钥的发布路径（方案 C）。
  想全自动就把 release.yml 的 ENGINE_BUILD_RUNNER 指向一台自托管 runner。

.EXAMPLE
  .\tools\release-local.ps1 -Version 1.0.1
  .\tools\release-local.ps1 -Version 1.0.1 -Natives natives-windows,natives-linux
  .\tools\release-local.ps1 -Version 1.0.1 -Natives natives-windows -Publish
#>
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string[]]$Natives = @('natives-windows'),
    [switch]$Tag,
    [switch]$Publish
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if ($Version -notmatch '^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$') {
    throw "版本号格式不对：$Version（期望 1.0.1 或 1.0.1-dev.2）"
}

$gradlew = if ($IsWindows -or $env:OS -eq 'Windows_NT') { '.\gradlew.bat' } else { './gradlew' }

Write-Host "== 1/4 校验依赖 ==" -ForegroundColor Cyan
& $gradlew verifyInputs --console=plain
if ($LASTEXITCODE -ne 0) { throw 'verifyInputs 失败：请检查 local.properties 的 frameworkDir / gameLibDir' }

Write-Host "== 2/4 构建 $Version（$($Natives -join ', ')）==" -ForegroundColor Cyan
$dist = Join-Path $root 'dist'
if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }
New-Item -ItemType Directory -Path $dist | Out-Null

foreach ($n in $Natives) {
    & $gradlew jar --console=plain "-PmodVersion=$Version" "-Pacbric.lwjglNatives=$n" "-PmodClassifier=$n"
    if ($LASTEXITCODE -ne 0) { throw "构建失败：$n" }
    $jar = Get-ChildItem (Join-Path $root "build/libs/*-$n.jar") | Select-Object -First 1
    if (-not $jar) { throw "找不到 $n 的产物" }
    Copy-Item $jar.FullName $dist
}

Write-Host "== 3/4 生成校验和与 MANIFEST ==" -ForegroundColor Cyan
$sums = @()
foreach ($f in Get-ChildItem (Join-Path $dist '*.jar')) {
    $hash = (Get-FileHash $f.FullName -Algorithm SHA256).Hash.ToLower()
    $sums += "$hash  $($f.Name)"
}
$sums | Set-Content (Join-Path $dist 'SHA256SUMS.txt') -Encoding utf8

$shaderCount = (Get-ChildItem (Join-Path $root 'src/main/resources/acbric_engine_data/shaders') -Include *.vert, *.frag -File).Count
$apiFloor = (Select-String -Path (Join-Path $root 'src/main/resources/fabric.mod.json') -Pattern '"acbric_api":\s*"([^"]+)"').Matches.Groups[1].Value
foreach ($f in Get-ChildItem (Join-Path $dist '*.jar')) {
    @(
        "jar: $($f.Name)"
        "version: $Version"
        "sha256: $((Get-FileHash $f.FullName -Algorithm SHA256).Hash.ToLower())"
        "bytes: $($f.Length)"
        "acbric_api: $apiFloor"
        "shaders: $shaderCount"
    ) | Set-Content (Join-Path $dist "MANIFEST-$($f.BaseName).txt") -Encoding utf8
}
Get-ChildItem $dist | Format-Table Name, Length

Write-Host "== 4/4 发布 ==" -ForegroundColor Cyan
$tagName = "v$Version"
if ($Tag) {
    if (git tag --list $tagName) { throw "tag $tagName 已存在" }
    git tag -a $tagName -m "Acbric LWJGL3 Engine $Version"
    git push origin $tagName
    Write-Host "已推送 tag $tagName；若已配置 ENGINE_BUILD_RUNNER，release.yml 会在自托管 runner 上构建并发布。" -ForegroundColor Green
}

if ($Publish) {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw '未找到 gh（GitHub CLI）。可改用网页创建 Release，或先安装 gh。'
    }
    $assets = @(Get-ChildItem (Join-Path $dist '*.jar') | ForEach-Object { $_.FullName }) +
              @((Join-Path $dist 'SHA256SUMS.txt')) +
              @(Get-ChildItem (Join-Path $dist 'MANIFEST-*.txt') | ForEach-Object { $_.FullName })
    gh release create $tagName @assets --title "Acbric LWJGL3 Engine $Version" --generate-notes
    Write-Host "Release $tagName 已创建。" -ForegroundColor Green
}
elseif (-not $Tag) {
    Write-Host ''
    Write-Host "产物在 $dist。接下来二选一：" -ForegroundColor Yellow
    Write-Host "  A) 网页发布：GitHub → Releases → Draft a new release，tag 填 $tagName，把 dist/ 里的文件拖进去"
    Write-Host "  B) 命令行发布：重跑本脚本并加 -Publish（需要 gh CLI）"
}
