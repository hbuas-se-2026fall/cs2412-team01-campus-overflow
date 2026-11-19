# Product Backlog v1.1

> 项目：CampusOverflow 校园问答平台｜团队：cs2412-team01｜负责人：PO 韦国超（@jingmo2233）
> 基线：指导书 5.4 节（PB-01 ~ PB-48，共 48 条，**163 故事点**，其中本期承诺 155 点、PB-48 为 Won't 的 8 点）｜本表在 Sprint 0 导入 GitHub Projects，表格视图按 `Rank` 字段排序即优先级顺序。
> 状态口径见文末「状态说明」。**本表是唯一需求基线**：Issue 标题、Sprint Backlog、评审记录都以此表的 ID 为准。

## 1. 排序原则（MoSCoW + 依赖顺序 + 风险前置）

1. **依赖优先**：Q&A 是所有事件的源头，Reputation 的悬赏依赖 `QaQueryApi`，Discovery 同时订阅 Q&A 与 Reputation 的事件 → Q&A → Reputation → Discovery。
2. **价值优先**：“提问—回答—采纳”闭环是最小可用产品（MVP），其次是激励体系，再次是发现能力。
3. **风险前置**：协作基础设施、CI 门禁、发件箱阻塞（行走骨架）放在最早的 Sprint；端到端集成测试在 Sprint 3 打通，为 Sprint 4 留缓冲。
4. **同 Sprint 内**：Must 先于 Should；领域层故事先于依赖它的前端故事；Could 只在容量富余时拉入。

## 2. 完整 Backlog（48 条）

| # | ID | 用户故事 | 史诗 | 优先级 | SP | Sprint | 依赖 | 实现落点 | 状态 |
| --: | :-- | :-- | :--: | :-- | --: | :--: | :-- | :-- | :-- |
| 1 | PB-01 | 作为团队，我们需要教师组织中的本组私有仓库、受保护分支、Issue/PR 模板与看板，以便所有代码经评审后集成 | E0 | Must | 3 | S0 | — | 本组仓库设置、`.github/` | 🟡 模板与 CODEOWNERS 已补，待评审确认 |
| 2 | PB-02 | 作为开发者，我需要一键启动 MySQL/Redis/后端/前端并跑通测试，以便在一致的环境中开发 | E0 | Must | 2 | S0 | — | `deploy/docker-compose.yml` | ✅ 已验收 |
| 3 | PB-03 | 作为团队，我们需要事件风暴产出的统一语言与上下文映射，以便沟通与编码一致 | E0 | Must | 3 | S0 | — | `docs/ddd/` | 🟡 三件套已补，待评审 |
| 4 | PB-04 | 作为团队，我们需要渐进式 CI 门禁，以便每个 Sprint 结束时流水线都是绿的 | E0 | Must | 2 | S0 | PB-01 | `.github/workflows/ci.yml`、ADR-009 | 🟡 缺覆盖率/集成测试/镜像阶段 |
| 5 | PB-05 | 作为学生，我可以注册、登录并在课程页面浏览和加入课程，以便在课程问答区提问 | E1 | Must | 3 | S0 | — | Identity（已实现）+ 前端课程页 | ✅ 已验收 |
| 6 | PB-06 | 作为学生，我可以发布带标签、可关联课程的问题，以便向同学和老师求助 | E2 | Must | 5 | S1 | PB-05 | `Question.post`、`MarkdownBody`、`Tags`、`QuestionCommandService.post` | ✅ 已验收（v0.2.0） |
| 7 | PB-07 | 作为用户，我可以分页浏览（按课程筛选）问题列表，以便发现待解决的问题 | E2 | Must | 3 | S1 | PB-06 | `QuestionQueryService.list` | ✅ 已验收（v0.2.0） |
| 8 | PB-08 | 作为用户，我可以查看问题详情（渲染后的正文、回答、评论、我的投票、可用操作） | E2 | Must | 5 | S1 | PB-06 | `QuestionQueryService.detail` | ✅ 已验收（v0.2.0） |
| 9 | PB-09 | 作为学生，我可以回答未删除的问题，以便帮助提问者 | E2 | Must | 3 | S1 | PB-06 | `Answer.submit`、`AnswerCommandService.submit` | ✅ 已验收（v0.2.0） |
| 10 | PB-10 | 作为提问者或课程管理者，我可以采纳一个最佳答案，以便标记问题已解决 | E2 | Must | 5 | S1 | PB-09 | `Question.accept`、`AnswerCommandService.accept` | ✅ 已验收（v0.2.0） |
| 11 | PB-11 | 作为用户，我可以对他人的问题/回答投票、改投或撤销，以便评价内容质量 | E2 | Must | 5 | S1 | PB-08 | `Vote`、`VoteService` | ✅ 已验收（v0.2.0） |
| 12 | PB-12 | 作为作者或课程管理者，我可以编辑问题和回答，以便修正内容 | E2 | Must | 3 | S1 | PB-06、PB-09 | `Question.edit`、`Answer.edit`、`*CommandService.edit` | ✅ 已验收（v0.2.0） |
| 13 | PB-13 | 作为作者或课程管理者，我可以软删除问题和回答，以便清理不当内容且可追责 | E2 | Must | 3 | S1 | PB-12 | `softDelete`、`delete` 用例 | ✅ 已验收（v0.2.0） |
| 14 | PB-14 | 作为课程教师/助教，我可以为优质回答做“教师认证”，以便给学生权威指引 | E2 | Should | 2 | S1 | PB-09 | `Answer.endorse`、`AnswerCommandService.endorse` | ✅ 已验收（v0.2.0） |
| 15 | PB-15 | 作为用户，我可以评论问题或回答并进行一层回复，以便追问与澄清 | E2 | Must | 3 | S1 | PB-08 | `Comment.post`、`CommentService` | ✅ 已验收（v0.2.0） |
| 16 | PB-16 | 作为用户，我可以看到热门标签，以便按主题浏览 | E2 | Should | 1 | S1 | PB-06 | `popularTags`、`Tags.names` | ✅ 已验收（v0.2.0） |
| 17 | PB-17 | 作为其他上下文，我可以通过 `QaQueryApi` 查询问题引用，以便悬赏与通知使用 | E2 | Must | 1 | S1 | PB-06 | `QaQueryApiImpl.findQuestion` | ✅ 已验收（v0.2.0） |
| 18 | PB-18 | 作为团队，我们需要行走骨架（未实现监听器暂为空实现），以便发件箱不被阻塞 | E0 | Must | 1 | S1 | PB-04 | Reputation/Discovery 的 `@EventListener` | ✅ 已移除（S3 全部实现） |
| 19 | PB-19 | 作为用户，我可以在前端编辑/删除内容、发表评论与回复，并看到友好的错误提示 | E7 | Should | 3 | S1 | PB-12、PB-15 | `frontend/src/pages/*` | ✅ 已验收（v0.2.0） |
| 20 | PB-20 | 作为新用户，系统为我开立声誉账户（教师获得初始悬赏积分），以便参与激励体系 | E3 | Must | 3 | S2 | PB-05 | `ReputationAccount.open`、`initialPoints`、`on(UserRegistered)` | 🟡 已完成待验收（v0.3.0） |
| 21 | PB-21 | 作为回答者，回答被采纳时我获得 +15 声誉（自问自答不加分）并记入账本 | E3 | Must | 5 | S2 | PB-10 | `on(AnswerAccepted)`、`applyReputation` | 🟡 已完成待验收（v0.3.0） |
| 22 | PB-22 | 作为作者，被赞 +10、被踩 −2，改投/撤销按差值计算，且每日正向投票所得 ≤ 200 | E3 | Must | 5 | S2 | PB-11 | `voteEffect/voteDelta/applyDailyCap`、`on(VoteCast)` | 🟡 已完成待验收（v0.3.0） |
| 23 | PB-23 | 作为用户，我在达成里程碑时自动获得徽章 | E3 | Should | 3 | S2 | PB-21 | `BadgePolicy`、`BadgeService.grant`、其余事件处理 | 🟡 已完成待验收（v0.3.0） |
| 24 | PB-24 | 作为课程管理者，我可以为问题发起悬赏（10–500 分，1–14 天）并冻结积分 | E4 | Must | 5 | S2 | PB-17、PB-20 | `Bounty.open`、`freezePoints`、`BountyService.open/latest` | 🟡 已完成待验收（v0.3.0） |
| 25 | PB-25 | 作为回答者，问题采纳时我自动赢得悬赏；作为发起人，到期未采纳时积分自动退回 | E4 | Must | 5 | S2 | PB-24 | `award/close/isDue`、`settleOnAccept/expireDue` | 🟡 已完成待验收（v0.3.0） |
| 26 | PB-26 | 作为用户，我可以查看周/月/学期/总排行榜 | E3 | Should | 5 | S2 | PB-21 | `AcademicTerm`、`LeaderboardService` | 🟡 已完成待验收（v0.3.0） |
| 27 | PB-27 | 作为用户，我可以查看他人声誉档案与徽章，并查看自己的积分与声誉流水 | E3 | Must | 3 | S2 | PB-20 | `ReputationQueryService`、`ReputationApiImpl` | 🟡 已完成待验收（v0.3.0） |
| 28 | PB-28 | 作为管理员，我可以查看每日对账结果，以便发现声誉异常（FF-8） | E3 | Should | 2 | S2 | PB-20 | `ReconciliationService` | 🟡 已完成待验收（v0.3.0） |
| 29 | PB-29 | 作为教师，我可以在对话框中发起悬赏并看到倒计时；作为用户，我有个人声誉主页 | E7 | Should | 3 | S2 | PB-24、PB-27 | 前端悬赏对话框、声誉页 | 🟡 已完成待验收（v0.3.0） |
| 30 | PB-30 | 作为团队，我们需要 Reputation 应用层单元测试（手写 Fake），以便安全重构 | E6 | Must | 3 | S2 | PB-20 ~ PB-28 | `co-reputation-application/src/test` | ✅ 已完成（55 个用例） |
| 31 | PB-31 | 作为用户，我可以用中英文关键词搜索并按标签/未回答/悬赏筛选、按相关度/最新/活跃/热度排序 | E5 | Must | 5 | S3 | PB-32 | `SearchQuery.terms`、`DiscoveryQueryService.search` | 🟡 已完成待验收（v0.9.0） |
| 32 | PB-32 | 作为系统，搜索投影随问答与悬赏事件幂等更新，以便搜索结果及时准确 | E5 | Must | 5 | S3 | PB-06 ~ PB-13 | `SearchIndexProjector` | 🟡 已完成待验收（v0.9.0） |
| 33 | PB-33 | 作为读者，我在问题详情页看到相关问题推荐 | E5 | Should | 3 | S3 | PB-31 | `RelatedQuestionPolicy`、`related` | 🟡 已完成待验收（v0.9.0） |
| 34 | PB-34 | 作为用户，我会收到新回答、被采纳、被认证、赢得悬赏、获得徽章等通知 | E5 | Must | 5 | S3 | PB-09、PB-10、PB-21 | `Notification.create`、`NotificationProjector` | 🟡 已完成待验收（v0.9.0） |
| 35 | PB-35 | 作为用户，评论中被 @ 时我会收到提醒（每条最多 5 人） | E5 | Should | 3 | S3 | PB-15 | `MentionParser`、`on(CommentPosted)` | 🟡 已完成待验收（v0.9.0） |
| 36 | PB-36 | 作为用户，我可以查看通知列表、未读数，并标记单条或全部已读 | E5 | Must | 3 | S3 | PB-34 | `NotificationService`、`markRead` | 🟡 已完成待验收（v0.9.0） |
| 37 | PB-37 | 作为团队，端到端集成测试全绿且领域覆盖率 ≥ 70%，以便发布候选版本 | E6 | Must | 3 | S3 | PB-32、PB-34 | `CampusOverflowFlowIT`、JaCoCo | 🟡 待本地 Docker 验证 |
| 38 | PB-38 | 作为演示者，demo profile 能加载完整演示数据 | E6 | Should | 1 | S3 | PB-24 | `DemoDataSeeder` | 🟡 待验证 |
| 39 | PB-39 | 作为团队，我们需要 Discovery 应用层单元测试（验证投影幂等） | E6 | Must | 3 | S3 | PB-32 | `co-discovery-application/src/test` | ✅ 已完成（32 个用例） |
| 40 | PB-40 | 作为管理员，我可以禁用违规账号并使其立即下线（ADR-003） | E1 | Should | 5 | S4 | PB-05 | `User.disable` + 服务/接口/会话清理 | ⬜ 未开始 |
| 41 | PB-41 | 作为用户，我可以查看和修改个人资料（学院、班级） | E1 | Could | 3 | S4 | PB-05 | Identity 扩展 | ⬜ 未开始 |
| 42 | PB-42 | 作为用户，我获得更好的前端体验：分页/骨架屏/错误重试、Markdown 实时预览 | E7 | Should | 3 | S4 | PB-19 | `frontend/` | ⬜ 未开始（部分已完成） |
| 43 | PB-43 | 作为用户，通知红点实时刷新（SSE 替代 30 秒轮询） | E7 | Could | 5 | S4 | PB-36 | 前端 + 新接口 | ⬜ 未开始 |
| 44 | PB-44 | 作为运维者，我可以在 Grafana 看到发件箱积压、死信、对账不一致等指标 | E6 | Should | 2 | S4 | PB-04 | `deploy/prometheus.yml`、Grafana | 🟡 配置已在，面板待补 |
| 45 | PB-45 | 作为团队，我们对问题列表做压测并验证缓存命中 | E6 | Could | 3 | S4 | PB-07 | 压测脚本 | ⬜ 未开始 |
| 46 | PB-46 | 作为干系人，我可以用 docker compose 一键部署 v1.0 并阅读 Release Notes | E6 | Must | 2 | S4 | PB-02 | GitHub Release、镜像 | 🟡 `deploy/` 已就绪，待实测 |
| 47 | PB-47 | 作为管理员，我有管理页面：认证学生、创建教职工账号 | E1 | Could | 3 | S4 | PB-05 | 前端 + 已有接口 | ⬜ 未开始 |
| 48 | PB-48 | 作为教师，我可以审批学生的选课申请 | E1 | Won't | 8 | — | — | Identity 扩展（留作后续） | ⛔ 本期不做 |

## 3. 各 Sprint 点数与状态汇总

| Sprint | 周次 | 纳入 PBI | 计划点数 | 已完成点数 | 版本标签 | 状态 |
| :-- | :-- | :-- | --: | --: | :-- | :-- |
| Sprint 0 | 第 1 周 | PB-01 ~ PB-05 | 13 | 13 | `v0.1.0` | ✅ 已验收 |
| Sprint 1 | 第 2–3 周 | PB-06 ~ PB-19 | 43 | 43 | `v0.2.0` | ✅ 已验收（标签待推送） |
| Sprint 2 | 第 4–5 周 | PB-20 ~ PB-30 | 42 | 42 | `v0.3.0` | 🟡 待评审验收 |
| Sprint 3 | 第 6–7 周 | PB-31 ~ PB-39 | 31 | 31 | `v0.9.0` | 🟡 待评审验收 |
| Sprint 4 | 第 8 周 | PB-40 ~ PB-47（按容量） | 26（Must/Should 12） | — | `v1.0.0` | ⬜ 进行中 |
| 合计 | 8 周 | 48 条 | 155（含 Won't 则 163） | 129（S0–S3） | — | — |

## 4. 状态说明

| 标记 | 含义 |
| :-- | :-- |
| ✅ 已验收 | 已在 Sprint 评审会按 DoD 逐条验收通过，且对应版本标签存在 |
| 🟡 待验收 | 代码与测试已就绪（`TODO(Sx)` 已清零），但尚未在评审会上完成“接受/退回”结论 |
| ⬜ 未开始 | 尚未进入开发 |
| ⛔ 本期不做 | Won't have this time，留作后续迭代 |

> **为什么 S2/S3 是“待验收”而不是“已验收”**：代码在 Sprint 1 期间已全部实现，但 `v0.3.0` / `v0.9.0` 标签与 Release 尚未创建，也没有对应的评审记录 —— 按 DoD 第 7 条（本地按验收标准演示通过）尚不能判定完成。补齐 Sprint 2、Sprint 3 的评审记录后改为 ✅。

## 5. Gherkin 验收标准索引

| 故事ID | 验收标准文件 | 场景数 |
| :--- | :--- | :--- |
| PB-01 | [us-001-register.feature](stories/us-001-register.feature) | 3 |
| PB-06 | [us-003-ask.feature](stories/us-003-ask.feature) | 3 |
| PB-07 / PB-31 | [us-002-browse.feature](stories/us-002-browse.feature) | 4 |
| PB-08 | [pb-08-detail.feature](stories/pb-08-detail.feature) | 4 |
| PB-09 | [pb-09-answer.feature](stories/pb-09-answer.feature) | 4 |
| PB-10 | [pb-10-accept.feature](stories/pb-10-accept.feature) | 5 |
| PB-11 | [pb-11-vote.feature](stories/pb-11-vote.feature) | 4 |
| PB-15 | [pb-15-comment.feature](stories/pb-15-comment.feature) | 5 |

每个文件均覆盖快乐路径、边界路径与异常路径。

## 6. 变更记录

| 版本 | 日期 | 变更 | 作者 |
| :-- | :-- | :-- | :-- |
| v1.0 | 2026-10-27 | 初版 11 条（PB-06 ~ PB-10、PB-20 ~ PB-22、PB-24、PB-31、PB-34） | PO |
| v1.1 | 2026-11-11 | 补齐至 48 条；增加史诗/依赖/实现落点/状态字段；状态口径改为“验收驱动”；补 Sprint 点数汇总 | PO |
