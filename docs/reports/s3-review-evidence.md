# S3 评审证据索引（指导书 9.1 逐项）

> 用途：12-03 评审时按老师的核查顺序，直接跳到对应证据，不用现场翻仓库。
> 口径：**2026-11-28 23:20** 实测；数字会随合并变化，评审当天上午再刷一次。
> 配套：[sprint-3-review.md](sprint-3-review.md)（会前打分表）、[deploy-acceptance.md](deploy-acceptance.md)（部署验收）。

## 一、逐项对照

| # | 核查维度（9.1） | 合格标准 | 现场展示 | 证据 |
| --: | :-- | :-- | :-- | :-- |
| 1 | 可运行的软件 | Sprint Goal 达成；Must 级 PBI 满足 DoD；无阻断缺陷 | 从 `main` 的 `v0.9.0` 起服务，按 [demo 脚本](../final/demo-script.md) 走一遍；必要时用 Swagger 调接口 | tag `v0.9.0` → `515b87c`；`compare v0.9.0...main` = ahead 8 / behind 0 |
| 2 | 代码提交历史 | 提交信息规范；每人每周 ≥3 次；无「最后一晚一次性提交」 | `git log --oneline --graph -30`；按作者统计 | 11-20 以来 28 个提交（含 6 个合并提交），分布在 11-20 / 11-24 / 11-25 / 11-26 / 11-28 五个批次 |
| 3 | 贡献者维度 | 每人都有领域/应用层 PR；有效贡献不低于团队平均的 50%（均分 20% → 门槛 10%） | Insights → Contributors | 242 提交：57.9% / 17.8% / 11.2% / 9.9% / **3.3%**（见「二、风险」） |
| 4 | PR 与 Code Review | 全部经 PR；有非作者 Approve；意见有实质内容并被处理；PR 关联 Issue | 抽查 3–5 个已合并 PR | #119（`e2e` 进 CI，xy1897 实质评审）、#121–#124（PB-40 与部署冒烟）、#133（15 条逐项核对）、#135（CI 时长逐条对回 API）、#136（12 条 PromQL 对回源码） |
| 5 | Issue 使用 | 每个 PBI 有 Issue 与验收标准；看板状态与实际一致；缺陷用模板登记 | Milestone 完成率 + 抽 3 个 Issue | S0 4/5、S1 14/14、S2 11/11、**S3 9/9**、S4 5/12；48 条 PBI 全部有 Issue |
| 6 | 分支策略 | 功能分支 + 合并后删除；`main` 只接受发布 PR；无强推；保护规则不可绕过 | Settings → Rules / 分支列表 | ruleset：`deletion` + `non_fast_forward` + `pull_request`（1 个非作者 Approve）；`main` 只收 develop → main 的发布 PR |
| 7 | CI/CD 状态 | `develop` 与 `main` 最新提交为绿；红灯恢复 < 24h；Sprint 末有 tag 与 Release | Actions 最近一次全量运行 | `main` `8a9bc1b` success；`develop` `ebfd838` success；`v0.9.0` pre-release；另有 `Deploy Smoke` 工作流 |
| 8 | Scrum 工件 | Sprint Goal 清晰；燃尽可解释；回顾改进项落实 | `docs/scrum/` 与 `docs/retro/` | [sprint-3.md](../scrum/sprint-3.md)（站会已按提交回填）、[sprint-3 回顾](../retro/sprint-3.md)、燃尽 CSV **未逐日采集**（如实标注） |

## 二、风险与说法（按影响排序）

| 风险 | 现状 | 现场怎么说 |
| :-- | :-- | :-- |
| **贡献分布不均** | 57.9% / 17.8% / 11.2% / 9.9% / 3.3%，最低者低于「团队平均 50%」的 10% 门槛 | 承认分布不均；说明 S4 已具体承接（PB-41 个人资料四层实现 + T4-01 发版 + T4-07 文档），并展示对应的 PR |
| **`main` 还没带 S3 收尾文档** | `main` 是 11-28 的同步点，S3 的评审表订正、站会回填、用户手册等还在 `develop` | 评审前需再合一次 develop → main（#150）；若来不及，从 `develop@515b87c` 演示，别当场改 `main` |
| **Issue 未自动关闭** | `Closes #N` 只在合入默认分支（`main`）时生效，合到 `develop` 不会自动关 | 已手动关 #139/#106/#112；#87 等在 #147 合并后手动关 |
| **燃尽图不完整** | S2/S3 的 CSV 只有首行计划值 | 如实说明「S3 未逐日采集，S4 起按站会补录」，不要拿它当证据 |
| **S4 的 Issue 缺 AC** | #110/#111/#113/#114 仍是模板 `Given ____` | 已在各 Issue 下放 AC 草稿，计划会前由负责人确认贴入 |

## 三、现场命令清单

```bash
# 版本与标签
git describe --tags                 # v0.9.0-8-g8a9bc1b
git tag -l | tail -4                # v0.1.0 v0.2.0 v0.3.0 v0.9.0

# 提交分布（评审当天重算，替换日期）
git rev-list --count --since='2026-11-19T16:00:00Z' origin/develop
git rev-list --count --merges --since='2026-11-19T16:00:00Z' origin/develop

# 行走骨架（应为 0）
grep -rn "walking-skeleton" backend/ || echo "0 命中"

# 架构约束
cd backend && mvn -q test -pl co-bootstrap -Dtest=ArchitectureTest

# 错误码文档一致性（新增门禁）
python3 scripts/check-error-codes.py
```

## 四、评审前 30 分钟的检查顺序

1. `main` 上 `git describe --tags` 能看到 `v0.9.0`；
2. 打开 Actions，确认最近一次**全量**运行是绿的（别引用纯文档 PR 那次 skipped 的 run）；
3. 按 [demo 脚本](../final/demo-script.md) 的第 0 节把服务与账号过一遍；
4. 打开本文件与 [sprint-3-review.md](sprint-3-review.md)，按上面的链接逐项对应。
