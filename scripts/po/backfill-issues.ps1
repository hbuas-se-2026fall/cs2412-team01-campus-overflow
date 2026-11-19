# 批量创建 / 补全 Product Backlog 的 Issue
#
# 数据源：scripts/po/issues.csv（由 docs/backlog/product-backlog.md v1.1 生成，48 条 PBI）
# 行为：按标题前缀 [PB-xx] 幂等 upsert —— 已存在则 PATCH，缺失则 POST；同时补齐标签与 Milestone。
#
# 用法：
#   $env:GITHUB_TOKEN = "ghp_xxx"        # 需要 repo 权限（能改 Issue 即可）
#   pwsh scripts/po/backfill-issues.ps1 -DryRun     # 先看要做什么
#   pwsh scripts/po/backfill-issues.ps1             # 真正执行
#
# 注意：本脚本只动 Issue、标签、Milestone，不改代码、不改 PR、不打 tag。

[CmdletBinding()]
param(
    [string]$Repo = "hbuas-se-2026fall/cs2412-team01-campus-overflow",
    [string]$Csv = (Join-Path $PSScriptRoot "issues.csv"),
    [string]$Token = $env:GITHUB_TOKEN,
    [switch]$DryRun,
    [switch]$SkipMilestones
)

$ErrorActionPreference = "Stop"
$Api = "https://api.github.com/repos/$Repo"

if (-not $Token) {
    throw "缺少 token：先设置 `$env:GITHUB_TOKEN`，或传 -Token。Token 只需 repo 权限。"
}
if (-not (Test-Path $Csv)) {
    throw "找不到 $Csv"
}

$Headers = @{
    Authorization          = "Bearer $Token"
    Accept                 = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
    "User-Agent"           = "cs2412-team01-po-backfill"
}

function Invoke-GitHub {
    param(
        [Parameter(Mandatory)][string]$Uri,
        [ValidateSet("GET", "POST", "PATCH")][string]$Method = "GET",
        $Body
    )
    $call = @{
        Uri         = $Uri
        Method      = $Method
        Headers     = $Headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 8 -Compress
        $call.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
    }
    try {
        return Invoke-RestMethod @call
    }
    catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 404) { return $null }
        throw "GitHub API $Method $Uri 失败：$code $($_.ErrorDetails.Message)"
    }
}

function Get-AllPages {
    param([string]$Path)
    $all = @()
    $page = 1
    while ($true) {
        $sep = if ($Path.Contains("?")) { "&" } else { "?" }
        $batch = Invoke-GitHub -Uri "$Api$Path${sep}per_page=100&page=$page"
        if (-not $batch) { break }
        $all += $batch
        if ($batch.Count -lt 100) { break }
        $page++
    }
    return $all
}

# ---------- 1. 标签 ----------
$labelSpecs = @(
    @{ name = "type:story"; color = "0e8a16" },
    @{ name = "type:task"; color = "0e8a16" },
    @{ name = "type:bug"; color = "d73a4a" },
    @{ name = "type:tech-debt"; color = "0e8a16" },
    @{ name = "ctx:identity"; color = "1d76db" },
    @{ name = "ctx:qa"; color = "1d76db" },
    @{ name = "ctx:reputation"; color = "1d76db" },
    @{ name = "ctx:discovery"; color = "1d76db" },
    @{ name = "ctx:frontend"; color = "1d76db" },
    @{ name = "ctx:infra"; color = "1d76db" },
    @{ name = "priority:must"; color = "b60205" },
    @{ name = "priority:should"; color = "d93f0b" },
    @{ name = "priority:could"; color = "fbca04" },
    @{ name = "priority:wont"; color = "cccccc" }
)

Write-Host "==> 检查标签" -ForegroundColor Cyan
$existingLabels = (Get-AllPages "/labels") | ForEach-Object { $_.name }
foreach ($l in $labelSpecs) {
    if ($existingLabels -contains $l.name) { continue }
    if ($DryRun) { Write-Host "  [dry-run] 建标签 $($l.name)"; continue }
    Invoke-GitHub -Uri "$Api/labels" -Method POST -Body @{ name = $l.name; color = $l.color } | Out-Null
    Write-Host "  + 标签 $($l.name)"
}

# ---------- 2. Milestone（Sprint = Milestone） ----------
$milestoneSpecs = @(
    @{ title = "Sprint 0"; due = "2026-10-22T15:59:59Z"; desc = "启动与基础设施：团队、仓库、环境、CI 与 Product Backlog 全部就绪；每位成员能本地启动系统并走完一次分支→PR→评审→合并。验收：docs/scrum/sprint-0.md" },
    @{ title = "Sprint 1"; due = "2026-11-05T15:59:59Z"; desc = "问答核心：提问→回答→投票→评论→采纳闭环，教师可认证与删除；Q&A 领域与应用层测试全绿。验收：docs/scrum/sprint-1.md" },
    @{ title = "Sprint 2"; due = "2026-11-19T15:59:59Z"; desc = "声誉与激励：账本、徽章、悬赏（冻结/结算/退款）、排行榜、对账；采纳 +15、投票 ±、每日上限 200。验收：docs/scrum/sprint-2.md" },
    @{ title = "Sprint 3"; due = "2026-12-03T15:59:59Z"; desc = "内容发现与端到端贯通：中文搜索、相关推荐、通知与 @ 提及；移除行走骨架，mvn verify 全绿，发布 RC v0.9.0。验收：docs/scrum/sprint-3.md" },
    @{ title = "Sprint 4"; due = "2026-12-10T15:59:59Z"; desc = "稳定化与发布：可观测性、一键部署 v1.0、最终答辩。验收：docs/scrum/sprint-4.md" }
)

if (-not $SkipMilestones) {
    Write-Host "==> 检查 Milestone" -ForegroundColor Cyan
    $existingMs = (Get-AllPages "/milestones?state=all") | ForEach-Object { $_.title }
    foreach ($m in $milestoneSpecs) {
        if ($existingMs -contains $m.title) { continue }
        if ($DryRun) { Write-Host "  [dry-run] 建 Milestone $($m.title)"; continue }
        $body = @{ title = $m.title; state = "open"; due_on = $m.due; description = $m.desc }
        Invoke-GitHub -Uri "$Api/milestones" -Method POST -Body $body | Out-Null
        Write-Host "  + Milestone $($m.title)"
    }
}

# Milestone 标题 -> 编号（后面每个 Issue 都要用，缓存一次）
$msMap = @{}
foreach ($m in (Get-AllPages "/milestones?state=all")) { $msMap[$m.title] = $m.number }

# ---------- 3. Issue upsert ----------
Write-Host "==> 读取 backlog：$Csv" -ForegroundColor Cyan
$rows = Import-Csv -Path $Csv -Encoding UTF8
Write-Host "  $($rows.Count) 条 PBI"

Write-Host "==> 拉取现有 Issue" -ForegroundColor Cyan
$existing = @{}
foreach ($i in (Get-AllPages "/issues?state=all")) {
    if ($i.PSObject.Properties.Name -contains "pull_request") { continue }
    if ($i.title -match '^\[(PB-\d+)\]') {
        $existing[$Matches[1]] = $i.number
    }
}
Write-Host "  已有 PBI Issue：$($existing.Count) 个"

$created = 0; $updated = 0
foreach ($r in $rows) {
    $labels = $r.labels -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    $msNumber = if ($r.milestone -and $msMap.ContainsKey($r.milestone)) { $msMap[$r.milestone] } else { $null }

    $num = $existing[$r.id]
    if ($num) {
        if ($DryRun) { Write-Host "  [dry-run] 更新 #$num $($r.id)"; continue }
        $patch = @{ body = $r.body; labels = $labels }
        if ($msNumber) { $patch.milestone = $msNumber }
        Invoke-GitHub -Uri "$Api/issues/$num" -Method PATCH -Body $patch | Out-Null
        Write-Host "  ~ 更新 #$num $($r.id)" -ForegroundColor Yellow
        $updated++
    }
    else {
        if ($DryRun) { Write-Host "  [dry-run] 新建 $($r.id) $($r.title)"; continue }
        $create = @{ title = "[$($r.id)] $($r.title)"; body = $r.body; labels = $labels }
        if ($msNumber) { $create.milestone = $msNumber }
        $res = Invoke-GitHub -Uri "$Api/issues" -Method POST -Body $create
        Write-Host "  + 新建 #$($res.number) $($r.id)" -ForegroundColor Green
        $created++
    }
}

Write-Host ""
if ($DryRun) {
    Write-Host "dry-run 完成（未写入）。去掉 -DryRun 执行。" -ForegroundColor Cyan
}
else {
    Write-Host "完成：新建 $created，更新 $updated" -ForegroundColor Cyan
    Write-Host "下一步：打开 Projects 看板，按 Rank 排序，把 S3 的 9 条 PBI 拉进 Sprint 3。" -ForegroundColor Cyan
}