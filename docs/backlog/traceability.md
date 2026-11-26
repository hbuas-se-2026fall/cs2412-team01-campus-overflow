# 需求追踪矩阵（PBI → Issue → PR → 测试 → 版本）

> 生成时间：2026-11-19（Issue 列于 2026-11-26 按 `GET /issues?state=all` 重算）｜数据来源：`docs/backlog/product-backlog.md`（v1.1）+ GitHub API + 仓库测试文件
> 维护规则：**每次 PR 的正文必须写 `Closes #<Issue>`**，否则本表的 PR 列会一直是空的。

| PBI | 故事 | Sprint | 优先级 | SP | Issue | 关联 PR | 测试类（规格来源） | 版本 | 状态 |
| :-- | :-- | :--: | :-- | --: | :--: | :-- | :-- | :--: | :-- |
| PB-01 | 教师组织中的本组私有仓库、受保护分支、Issue/PR 模板与看板 | S0 | Must | 3 | #24 | — | — | v0.1.0 | 🟡 模板与 CODEOWNERS 已补，待评审确认 |
| PB-02 | 一键启动 MySQL/Redis/后端/前端并跑通测试 | S0 | Must | 2 | #86 | — | — | v0.1.0 | ✅ 已验收 |
| PB-03 | 事件风暴产出的统一语言与上下文映射 | S0 | Must | 3 | #87 | — | — | v0.1.0 | 🟡 三件套已补，待评审 |
| PB-04 | 渐进式 CI 门禁 | S0 | Must | 2 | #25 | — | — | v0.1.0 | 🟡 缺覆盖率/集成测试/镜像阶段 |
| PB-05 | 注册、登录并在课程页面浏览和加入课程 | S0 | Must | 3 | #88 | — | `IdentityApiImplTest` | v0.1.0 | ✅ 已验收 |
| PB-06 | 发布带标签、可关联课程的问题 | S1 | Must | 5 | #10 | — | `MarkdownBodyTest`、`QuestionCommandServiceTest`、`QuestionControllerTest`、`QuestionQueryServiceTest` | v0.2.0 | ✅ 已验收 |
| PB-07 | 分页浏览（按课程筛选）问题列表 | S1 | Must | 3 | #11 | — | `QuestionCommandServiceTest`、`QuestionControllerTest`、`QuestionQueryServiceTest`、`QuestionTest` | v0.2.0 | ✅ 已验收 |
| PB-08 | 查看问题详情（渲染后的正文、回答、评论、我的投票、可用操作） | S1 | Must | 5 | #89 | #28 | `QuestionCommandServiceTest`、`QuestionControllerTest`、`QuestionQueryServiceTest`、`QuestionTest` | v0.2.0 | ✅ 已验收 |
| PB-09 | 回答未删除的问题 | S1 | Must | 3 | #12 | — | `AnswerAndVoteServiceTest`、`AnswerTest` | v0.2.0 | ✅ 已验收 |
| PB-10 | 采纳一个最佳答案 | S1 | Must | 5 | #13 | — | `AnswerAndVoteServiceTest`、`AnswerTest`、`QuestionCommandServiceTest`、`QuestionControllerTest` | v0.2.0 | ✅ 已验收 |
| PB-11 | 对他人的问题/回答投票、改投或撤销 | S1 | Must | 5 | #14 | — | `VoteAndCommentBoundaryTest`、`VoteAndCommentTest` | v0.2.0 | ✅ 已验收 |
| PB-12 | 编辑问题和回答 | S1 | Must | 3 | #90 | — | `AnswerAndVoteServiceTest`、`AnswerTest`、`QuestionCommandServiceTest`、`QuestionControllerTest` | v0.2.0 | ✅ 已验收 |
| PB-13 | 软删除问题和回答 | S1 | Must | 3 | #91 | — | `AnswerAndVoteServiceTest`、`AnswerTest`、`CommentServiceTest`、`CommonmarkContentRendererTest` | v0.2.0 | ✅ 已验收 |
| PB-14 | 为优质回答做“教师认证” | S1 | Should | 2 | #92 | — | `AnswerAndVoteServiceTest`、`AnswerTest` | v0.2.0 | ✅ 已验收 |
| PB-15 | 评论问题或回答并进行一层回复 | S1 | Must | 3 | #93 | — | `CommentServiceTest` | v0.2.0 | ✅ 已验收 |
| PB-16 | 看到热门标签 | S1 | Should | 1 | #94 | — | `AnswerAndVoteServiceTest`、`AnswerTest`、`CommentServiceTest`、`CommonmarkContentRendererTest` | v0.2.0 | ✅ 已验收 |
| PB-17 | 通过 `QaQueryApi` 查询问题引用 | S1 | Must | 1 | #95 | — | `QuestionCommandServiceTest`、`QuestionControllerTest`、`QuestionQueryServiceTest`、`QuestionTest` | v0.2.0 | ✅ 已验收 |
| PB-18 | 行走骨架（未实现监听器暂为空实现） | S1 | Must | 1 | #96 | — | `DiscoveryDomainTest`、`DiscoveryQueryServiceTest`、`DiscoveryQueryServiceViewMappingTest`、`DiscoverySearchAndNotificationBoundaryTest` | v0.2.0 | ✅ 已移除 |
| PB-19 | 在前端编辑/删除内容、发表评论与回复，并看到友好的错误提示 | S1 | Should | 3 | #97 | #48 | — | v0.2.0 | ✅ 已验收 |
| PB-20 | 系统为我开立声誉账户（教师获得初始悬赏积分） | S2 | Must | 3 | #15 | — | `ReputationApiImplTest`、`ReputationEventHandlersTest`、`ReputationQueryServiceTest`、`ReputationReplayIdempotencyTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-21 | 回答被采纳时我获得 +15 声誉（自问自答不加分）并记入账本 | S2 | Must | 5 | #16 | — | `AnswerAndVoteServiceTest`、`AnswerTest`、`ReputationApiImplTest`、`ReputationEventHandlersTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-22 | 被赞 +10、被踩 −2，改投/撤销按差值计算，且每日正向投票所得 ≤ 200 | S2 | Must | 5 | #17 | — | `VoteAndCommentBoundaryTest`、`VoteAndCommentTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-23 | 达成里程碑时自动获得徽章 | S2 | Should | 3 | #98 | — | `BadgePolicyAndAcademicTermBoundaryTest`、`BadgeServiceTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-24 | 为问题发起悬赏（10–500 分，1–14 天）并冻结积分 | S2 | Must | 5 | #18 | — | `BountyAndAccountTest`、`BountyLifecycleTest`、`BountyServiceTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-25 | 问题采纳时我自动赢得悬赏；作为发起人，到期未采纳时积分自动退回 | S2 | Must | 5 | #19 | — | `BadgeCatalogTest`、`BadgePolicyAndAcademicTermBoundaryTest`、`BadgeServiceTest`、`BountyAndAccountTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-26 | 查看周/月/学期/总排行榜 | S2 | Should | 5 | #99 | — | `LeaderboardServiceTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-27 | 查看他人声誉档案与徽章，并查看自己的积分与声誉流水 | S2 | Must | 3 | #100 | — | `ReputationApiImplTest`、`ReputationEventHandlersTest`、`ReputationQueryServiceTest`、`ReputationReplayIdempotencyTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-28 | 查看每日对账结果 | S2 | Should | 2 | #101 | — | `ReconciliationServiceTest` | v0.3.0 | 🟡 已完成待验收 |
| PB-29 | 在对话框中发起悬赏并看到倒计时；作为用户，我有个人声誉主页 | S2 | Should | 3 | #102 | — | — | v0.3.0 | 🟡 已完成待验收 |
| PB-30 | Reputation 应用层单元测试（手写 Fake） | S2 | Must | 3 | #103 | — | `ReputationApiImplTest`、`ReputationEventHandlersTest`、`ReputationQueryServiceTest`、`ReputationReplayIdempotencyTest` | v0.3.0 | ✅ 已完成 |
| PB-31 | 用中英文关键词搜索并按标签/未回答/悬赏筛选、按相关度/最新/活跃/热度排序 | S3 | Must | 5 | #20 | — | `DiscoveryDomainTest`、`DiscoveryQueryServiceTest`、`DiscoveryQueryServiceViewMappingTest`、`DiscoverySearchAndNotificationBoundaryTest` | v0.9.0 | ✅ 已完成（#20 已关闭） |
| PB-32 | 搜索投影随问答与悬赏事件幂等更新 | S3 | Must | 5 | #21 | #64 | `SearchIndexProjectorDedupTest`、`SearchIndexProjectorTest` | v0.9.0 | 🟡 已完成待验收 |
| PB-33 | 问题详情页看到相关问题推荐 | S3 | Should | 3 | #104 | — | `QuestionCommandServiceTest`、`QuestionControllerTest`、`QuestionQueryServiceTest`、`QuestionTest` | v0.9.0 | ✅ 已完成（#104 已关闭） |
| PB-34 | 收到新回答、被采纳、被认证、赢得悬赏、获得徽章等通知 | S3 | Must | 5 | #22 | — | `NotificationProjectorBoundaryAndOwnershipTest`、`NotificationProjectorTest`、`NotificationServiceTest` | v0.9.0 | 🟡 已完成待验收 |
| PB-35 | 评论中被 @ 时我会收到提醒（每条最多 5 人） | S3 | Should | 3 | #23 | — | `CommentServiceTest` | v0.9.0 | 🟡 已完成待验收 |
| PB-36 | 查看通知列表、未读数，并标记单条或全部已读 | S3 | Must | 3 | #105 | — | `NotificationProjectorBoundaryAndOwnershipTest`、`NotificationProjectorTest`、`NotificationServiceTest` | v0.9.0 | 🟡 已完成待验收 |
| PB-37 | 端到端集成测试全绿且领域覆盖率 ≥ 70% | S3 | Must | 3 | #26 | #67 | — | v0.9.0 | 🟡 待本地 Docker 验证 |
| PB-38 | demo profile 能加载完整演示数据 | S3 | Should | 1 | #106 | — | — | v0.9.0 | 🟡 待验证 |
| PB-39 | Discovery 应用层单元测试（验证投影幂等） | S3 | Must | 3 | #107 | — | `DiscoveryDomainTest`、`DiscoveryQueryServiceTest`、`DiscoveryQueryServiceViewMappingTest`、`DiscoverySearchAndNotificationBoundaryTest` | v0.9.0 | ✅ 已完成 |
| PB-40 | 禁用违规账号并使其立即下线（ADR-003） | S4 | Should | 5 | #108 | #60、#61 | `UserQueryServiceTest`、`UserRegistrationServiceTest`、`UserTest` | v1.0.0 | ⬜ 未开始 |
| PB-41 | 查看和修改个人资料（学院、班级） | S4 | Could | 3 | #109 | — | `IdentityApiImplTest` | v1.0.0 | ⬜ 未开始 |
| PB-42 | 获得更好的前端体验：分页/骨架屏/错误重试、Markdown 实时预览 | S4 | Should | 3 | #110 | — | — | v1.0.0 | ⬜ 未开始 |
| PB-43 | 通知红点实时刷新（SSE 替代 30 秒轮询） | S4 | Could | 5 | #111 | — | — | v1.0.0 | ⬜ 未开始 |
| PB-44 | 在 Grafana 看到发件箱积压、死信、对账不一致等指标 | S4 | Should | 2 | #112 | — | — | v1.0.0 | 🟡 配置已在，面板待补 |
| PB-45 | 问题列表做压测并验证缓存命中 | S4 | Could | 3 | #113 | — | — | v1.0.0 | ⬜ 未开始 |
| PB-46 | 用 docker compose 一键部署 v1.0 并阅读 Release Notes | S4 | Must | 2 | #27 | — | — | v1.0.0 | 🟡 `deploy/` 已就绪，待实测 |
| PB-47 | 有管理页面：认证学生、创建教职工账号 | S4 | Could | 3 | #114 | — | `AdminUserServiceTest`、`BCryptPasswordHasherTest`、`CourseServiceTest`、`CourseTest` | v1.0.0 | ⬜ 未开始 |
| PB-48 | 审批学生的选课申请 | — | Won't | 8 | #115 | — | `IdentityApiImplTest` | — | ⛔ 本期不做 |

## 统计

- PBI 总数：48
- 已有 Issue：48 / 48
- 关联到 PR：5 / 48（PR 正文补 `Closes #` 后会提升）
- 有测试类可作规格：35 / 48

## 缺口清单（需要建 Issue 的 PBI）

（无）
