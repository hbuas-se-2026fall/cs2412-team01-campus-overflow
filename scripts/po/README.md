# PO 工具箱

| 文件 | 用途 |
| :-- | :-- |
| `issues.csv` | 48 条 PBI 的 Issue 数据（标题 / 标签 / Milestone / 正文），从 `docs/backlog/product-backlog.md` v1.1 生成 |
| `backfill-issues.ps1` | 按 `[PB-xx]` 幂等批量创建或补全 Issue；顺带补齐 14 个标签与 5 个 Milestone |
| `burndown.py` | 把 `docs/scrum/burndown/sprint-N.csv` 画成燃尽图 PNG |

## 1. 批量补 Issue

```powershell
# GitHub → Settings → Developer settings → Personal access tokens
# 新建 classic token，勾选 repo（或 fine-grained 勾 Issues: Read and write）
$env:GITHUB_TOKEN = "ghp_xxx"

# 先干跑，看清楚要动哪些
pwsh scripts/po/backfill-issues.ps1 -DryRun

# 确认后执行（幂等：已存在的 [PB-xx] 会更新正文/标签/Milestone，不会重复建）
pwsh scripts/po/backfill-issues.ps1
```

脚本行为：

1. 缺的标签自动建（`type:story`、`ctx:*`、`priority:*`），已有则跳过；
2. 缺的 Milestone 自动建（Sprint 0~4，描述里带 Sprint Goal 与截止日期）；
3. Issue 按标题前缀 `[PB-xx]` 匹配：有则 PATCH，无则 POST，并把对应 Milestone 挂上；
4. **不动代码、不动 PR、不打 tag**。

跑完的效果：27 个空壳 + 21 条缺失 → 48 条带验收标准、带三组标签、挂在 5 个 Milestone 上的 PBI。

## 2. 画燃尽图

```bash
python scripts/po/burndown.py docs/scrum/burndown/sprint-1.csv
# 输出 docs/scrum/burndown/burndown-sprint-1.png
```

CSV 只有两列是必需的：`date,remaining`（`note` 可留空）。SM 每次站会后把当天剩余点数填进去，
Sprint 末运行一次即可。缺少实际值的 Sprint 先补数据再画图——**不要用理想线冒充实际线**。

## 3. 一处必须养成的习惯

PR 正文里写 **`Closes #<Issue 号>`**。现在 `docs/backlog/traceability.md` 的「关联 PR」列
大多为空，就是因为 PR 没写这一句；补上之后 GitHub 会自动关联并关闭 Issue，
追踪矩阵也才有意义。

标签规范（指导书 7.4）：

| 标签组 | 取值 |
| :-- | :-- |
| `type:` | story / task / bug / tech-debt / spike / docs |
| `ctx:` | identity / qa / reputation / discovery / frontend / infra |
| `priority:` | must / should / could / wont |

## 4. 依赖

- `backfill-issues.ps1`：PowerShell 7（`pwsh`），只用标准库；
- `burndown.py`：Python 3.9+ 与 Pillow（`pip install pillow`），中文字体用系统 `msyh.ttc`，缺字体时回退默认字体。