# Sprint 3 评审 · 计分表与现场清单

> 用途：评审会上逐项核对、当场打分，会后把结论回填到 `docs/scrum/sprint-3.md` §4。
> 依据：《实验指导书》9.1（核查内容）与 9.2（评分方案）。
> 打分口径：每项 0–5 分，5 = 超出要求，3 = 达标，1 = 有但明显不足，0 = 缺失。
> **数据复核（2026-11-28，#139）**：提交数 / PR 数 / e2e 用例数 / run 链接已按 `GET /commits`、`GET /pulls` 与 job 日志重算。

## 会议信息

| 项 | 内容 |
| :-- | :-- |
| Sprint | S3 内容发现与端到端贯通（2026-11-20 ~ 12-03） |
| 评审日期 | 待填 |
| 演示人 | PO 韦国超 |
| 记录 | SM 张博 |
| 教师 | 待填 |

## A. 可运行的软件（权重 50%）

从 `main` 的版本标签启动（S3 交付点为 `v0.9.0`），按演示脚本走完下列场景。
若 #137 尚未合并（`main` 仍停在 `244ac23`），临时从 `develop` 的 `515b87c` 检出，别现场改 `main`。

| # | 验收项（指导书 6.5.3） | 证据 | 实测 | 分 |
| --: | :-- | :-- | :-- | --: |
| 1 | 搜索「跳表」「红黑树」等中文词与英文词均命中 | `GET /api/v1/search?q=...` | 单测：`DiscoverySearchAndNotificationBoundaryTest`(30) + `DiscoveryQueryServiceTest`(7) 覆盖多词切分与中英文混排；现场：`q=跳表` 应命中 #130 补的两条跳表问题 | |
| 2 | 标签 / 未回答 / 悬赏筛选与四种排序生效 | `GET /api/v1/questions?tag=...&sort=...` | 集成：`CampusOverflowFlowIT#按标签浏览与排序筛选在数据库层生效`（CI run #188 通过，见下） | |
| 3 | 单字关键词返回 422 `DIS-1001` | 同上，`q=跳` | 领域策略 `SearchQuery.terms` 的 < 2 字符边界由 `DiscoveryDomainTest`(5) 覆盖；现场：`q=跳` 期望 422 + `code=DIS-1001` | |
| 4 | 详情页显示相关问题及相似度 | `GET /api/v1/questions/{id}/related` | `RelatedQuestionPolicy` 的 Jaccard 与排序由 `DiscoveryDomainTest`(5) 覆盖；现场：对 1 号问题调 `/related`，看 `similarity` 字段递减 | |
| 5 | 已删除的问题不再出现在搜索与推荐中 | 软删后重查 | 软删发 `ContentDeletedEvent` → 投影标记删除，由 `SearchIndexProjectorTest`(10) 与 `DiscoverySearchAndNotificationBoundaryTest`(30) 覆盖；现场：软删后同一关键词重查应为空 | |
| 6 | 回答/采纳/认证/获奖/徽章/@ 均产生通知，未读数正确 | `GET /api/v1/notifications` | `NotificationProjectorTest`(9) 覆盖六类事件投影，`NotificationServiceTest`(7) 覆盖未读数与标记已读；现场：`GET /notifications` 与 `/notifications/unread-count` 数字一致 | |
| 7 | 只能标记自己的通知（`DIS-3001`） | 他人会话调用 | `NotificationProjectorBoundaryAndOwnershipTest`(14) 含越权用例；现场：用第二个账号的会话 POST 他人通知的 `read`，期望 403 `DIS-3001` | |
| 8 | 同一事件投递两次只生效一次（幂等） | `CampusOverflowFlowIT` 重放用例 | `SearchIndexProjectorDedupTest`(12) 专测投影去重；集成链路由 `CampusOverflowFlowIT#提问到采纳的完整链路会异步结算声誉并更新搜索与通知` 覆盖（run #188 通过） | |

> 第 8 条现在有 CI 证据：改代码的 PR 都会跑 `e2e` job（纯文档 PR 由 #120 的 `changes` 判断跳过），
> 日志口径是 `Tests run: 6`（`CampusOverflowFlowIT` 5 个 + `DisableUserSessionRevocationIT` 1 个），
> 见 run `46030936902`（`develop` @ `9410af4`）。
> 完整部署链路的实测记录见 [deploy-smoke.md](deploy-smoke.md)。

> **演示时用哪个 run**：`develop` 头（`515b87c`）那次运行 [#193](https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/actions/runs/46031489381)
> 因为只改文档，三个构建 job 都被 `changes` 判断跳过（结论是 skipped），**不能用来证明跑过测试**。
> 要展示全量运行请用 [#188](https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/actions/runs/46030936902)
> （`develop` @ `9410af4`）：后端 47 s、前端 12 s、端到端 111 s，三段全部 success。

> 第 8 条（幂等）现场不点界面：直接打开最近一次 `e2e` job 的日志，指 `Tests run` 行，
> 再说明「同一事件投递两次只生效一次」由 `CampusOverflowFlowIT` 的重放用例覆盖。

**A 小计**：______ / 40 × 50% = ______

## B. 协作证据（权重 30%）

| # | 核查项（9.1） | 核查方式 | 现状 | 分 |
| --: | :-- | :-- | :-- | --: |
| 1 | 代码提交历史 | `git log --graph`，看提交分布 | 11-20 ~ 11-28 共 26 个提交（6 个合并提交）、18 个 PR 合入，集中在三个批次：11-24~25（e2e 进 CI）、11-26（发布前收尾）、11-28（CI 裁剪与发布收口） | |
| 2 | 贡献者维度 | Insights → Contributors | `jingmo2233` 58.1%（126/217 提交），其余四人 16.6% / 11.1% / 11.1% / 3.2%，均低于团队平均 20% | |
| 3 | PR 与 Code Review | 抽查 3–5 个 PR（含已合并与在评） | #119 有 `xy1897` 的实质评审；11-26 起 #121/#122/#123/#124/#126/#133/#134/#135/#136 均有逐条数据的评审（非 LGTM），另有 `yttt697` 的自测记录 | |
| 4 | Issue 使用 | Projects / Milestone 完成率 | 48 条 PBI 有 Issue 与验收标准；S3 的 9 条已关，站会记录已按提交时间线回填（见 [sprint-3.md §3](../scrum/sprint-3.md)） | |
| 5 | 分支策略 | 分支列表 + 保护规则 | `main`/`develop` 各需 1 个 approve；历史分支有残留（合并后未删） | |
| 6 | CI/CD 状态 | Actions 页面 | 单元测试 + ArchUnit + 前端 + **端到端**四段；另有 `Deploy Smoke` 冒烟（#124），但还没有自动发布到真实主机的流水线 | |

**B 小计**：______ / 30 × 30% = ______

## C. Scrum 工件（权重 20%）

| # | 核查项（9.1） | 现状 | 分 |
| --: | :-- | :-- | --: |
| 1 | Milestone 描述含 Sprint Goal | 5 个 Milestone 已建，描述含目标与截止日 | |
| 2 | 燃尽图可解释 | S1 有真实燃尽图；S2/S3 只有 CSV 骨架，站会数据未填 | |
| 3 | 回顾改进项落实 | S1/S2 有回顾；S3 待开；S2 的两条改进行动（评审记录、tag）已落实 | |

**C 小计**：______ / 15 × 20% = ______

## 换算

| 部分 | 原始分 | 满分 | 加权 |
| :-- | --: | --: | --: |
| A 可运行的软件 | | 40 | × 50% = |
| B 协作证据 | | 30 | × 30% = |
| C Scrum 工件 | | 15 | × 20% = |
| **本次评审合计** | | | **/ 100** |

> 9.2 的课程总评里，S0–S3 四次评审合计 40%（每次 10%），S4 产品与答辩 25%，
> 工程质量 20%，过程与协作 15%。本表只算单次评审的 100 分。

## 会前 15 分钟准备

1. 从 `v0.9.0` 标签检出、起服务，确认能登录（`COOKIE_SECURE=false`）；
2. 打开 Actions 最近一次**全量**绿色运行（run `46030936902`），准备展示 `e2e` job 日志里的 `Tests run: 6`；
3. 打开 `docs/scrum/sprint-3.md`，确认 §5 的数据表已是最新（11-28 已回填）；
4. 准备一个「失败案例」：S2 那 28 次 CI 红灯怎么处理的，教师大概率会问。

## 会后

- 把 A/B/C 的结论与总分回填到 `docs/scrum/sprint-3.md` §4；
- 退化项（未通过的条目）转成 Issue，挂到 S4 Milestone；
- ~~打 `v0.9.0` 标签并创建 Release~~ —— **已提前完成**（2026-11-28，tag `515b87c`，
  [pre-release](https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/releases/tag/v0.9.0)），
  评审会只需确认接受/退回结论。