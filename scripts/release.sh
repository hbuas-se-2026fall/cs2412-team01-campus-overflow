#!/usr/bin/env bash
# CampusOverflow 一键发版（T4-01 / PB-46）
#
# 用法：
#   bash scripts/release.sh --dry-run        # 只做校验，不落任何东西
#   bash scripts/release.sh                  # 校验 → 打附注标签 → 推送
#   bash scripts/release.sh v1.0.0           # 指定版本（默认 v1.0.0）
#
# 它不创建 GitHub Release（需要 token/UI 决定正文措辞），只负责把「标签」这一步做对，
# 并把 Release 正文的来源文件打印出来。
set -euo pipefail

VERSION="v1.0.0"
DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    -h|--help) sed -n '2,11p' "$0"; exit 0 ;;
    v[0-9]*) VERSION="$arg" ;;
    *) echo "未知参数：$arg（用 --help 看用法）" >&2; exit 1 ;;
  esac
done

DOC="docs/release/${VERSION}.md"
ok()   { printf '\033[1;32m[ok]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[fail]\033[0m %s\n' "$*" >&2; exit 1; }

# ── 1. 版本说明文件 ──
[ -f "$DOC" ] || fail "缺少发布说明 $DOC（先写 Release Notes 再发版）"
ok "发布说明存在：$DOC"

if grep -q '状态：草案' "$DOC"; then
  warn "$DOC 仍标注「状态：草案」—— 发版前应改为「已发布」，否则 Release 正文与仓库不一致"
fi

# ── 2. 工作区与分支 ──
[ -z "$(git status --porcelain)" ] || fail "工作区有未提交改动，先提交或 stash"
ok "工作区干净"

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[ "$BRANCH" = "main" ] || fail "当前在 $BRANCH，发版必须在 main 上（main 只接受发布 PR）"
ok "当前分支：main"

# ── 3. develop 是否已合入（tag 必须落在包含全部交付的提交上）──
git fetch --quiet --tags origin
if git rev-parse --verify --quiet origin/develop >/dev/null; then
  if git merge-base --is-ancestor origin/develop HEAD; then
    ok "origin/develop 已包含在 main 中"
  else
    fail "origin/develop 还有提交没合进 main —— 先合发布 PR，再打标签"
  fi
fi

# ── 4. 标签是否已存在 ──
if git rev-parse --verify --quiet "refs/tags/$VERSION" >/dev/null; then
  fail "标签 $VERSION 已存在（本地）。要重打请先 git tag -d $VERSION 并确认远端也删了"
fi
if git ls-remote --tags --exit-code origin "refs/tags/$VERSION" >/dev/null 2>&1; then
  fail "标签 $VERSION 已存在于远端"
fi
ok "标签 $VERSION 未被占用"

# ── 5. 提交摘要（写 Release 正文时可直接引用）──
echo
echo "即将打标签的提交："
git --no-pager log --oneline -5
echo

if [ "$DRY_RUN" = "1" ]; then
  ok "dry-run 结束：校验全部通过，未创建标签"
  exit 0
fi

# ── 6. 打附注标签并推送 ──
git tag -a "$VERSION" -m "CampusOverflow $VERSION"
git push origin "$VERSION"
ok "已推送标签 $VERSION"

cat <<EOF

下一步（GitHub 网页）：
  1. Releases → Draft a new release → 选择标签 $VERSION
  2. 标题：CampusOverflow $VERSION
  3. 正文：直接粘贴 $DOC 的内容（去掉开头的「> 状态」行）
  4. 勾选预发布？正式版不勾
  5. 发布后回到仓库，在 main 上确认：git describe --tags  应输出 $VERSION

发布前别忘了：
  - 按 docs/reports/deploy-acceptance.md 在干净环境跑一遍 8 条断言
  - 把 docs/scrum/sprint-4.md 的评审结论与 docs/retro/sprint-4.md 补完
EOF
