# 统一语言术语表 v2.0（Ubiquitous Language）

> 产出：Sprint 0 事件风暴工作坊（v1.0）｜v2.0 于 2026-11-28 按代码现状复核（PB-03 / #87）
> 规则：业务沟通、表名、类名、接口字段一律使用本表英文命名；**「代码落点」列必须能在 `backend/` 中找到**，
> 新增或改名术语须同 PR 更新本表。上下文划分见 [context-map.md](context-map.md)，不变式见 [aggregates.md](aggregates.md)。

## 1. 身份与课程（Identity · `co-identity`）

| 中文术语 | 英文命名 | 定义 | 代码落点 |
| :-- | :-- | :-- | :-- |
| 用户 | User | 平台注册账号，以学号/工号作为用户名 | `identity.domain.User` |
| 用户状态 | UserStatus | `ACTIVE` 正常 / `DISABLED` 已禁用（禁用后会话立即失效，ADR-003） | `UserStatus` |
| 平台角色 | Role | 学生 STUDENT / 助教 TA / 教师 TEACHER / 管理员 ADMIN | `shared.security.Role` |
| 课程 | Course | 问答的组织单位，每个问题都属于一门课程 | `Course` |
| 课程成员 | CourseMember | 用户与课程的成员关系，携带课程内角色 | `CourseMember` |
| 课程内角色 | CourseRole | 课程内的 STUDENT / TA / TEACHER；与平台角色相互独立 | `CourseRole` |
| 课程管理者 | Course Manager | 课程内角色为 TA 或 TEACHER 的成员，可认证回答、发起悬赏、代为采纳/编辑/删除 | `CourseRole.canManage()`、`IdentityApi.canManageCourse` |
| 用户摘要 | UserSummary | 对外暴露的用户只读视图，**不含学号、邮箱等个人信息** | `identity.api.UserSummary` |
| 密码策略 | PasswordPolicy | 注册/改密时的密码强度规则 | `PasswordPolicy` |
| 禁用账号 | Disable | 管理员将用户置为 `DISABLED` 并强制下线 | `User.disable` |

## 2. 课程问答（Q&A · `co-qa`，核心域）

| 中文术语 | 英文命名 | 定义 | 代码落点 |
| :-- | :-- | :-- | :-- |
| 问题 | Question | 课程内的求助帖，由标题、Markdown 正文、标签组成 | `qa.domain.Question` |
| 标题 | Title | 问题标题值对象（10–150 字符） | `Title` |
| 正文 | MarkdownBody | Markdown 正文值对象，问题与回答共用 | `MarkdownBody` |
| 标签 | Tag | 问题的分类关键词，去重后 1–5 个、统一小写 | `TagName`、`Tags` |
| 回答 | Answer | 针对某个问题的解答 | `Answer` |
| 采纳 | Accept | 提问者或课程管理者把某个回答标记为最佳答案；每题只能采纳一次 | `Question.accept`、`AnswerAcceptedEvent` |
| 教师认证 | Endorse | 课程管理者为优质回答背书；每个回答只能认证一次 | `AnswerEndorsedEvent` |
| 投票 | Vote | 对问题或回答投 +1（赞同）/ −1（反对）；可改投、撤销，不能给自己投 | `Vote`、`VoteCastEvent` |
| 投票目标 | TargetType | 可被投票/评论的内容类型：`QUESTION` / `ANSWER` | `TargetType` |
| 评论 | Comment | 对问题或回答的简短回复，支持一层二级回复（`parentId`） | `Comment`、`CommentPostedEvent` |
| 编辑 | Edit | 作者或课程管理者修改已发布的问题/回答 | `QuestionEditedEvent` |
| 软删除 | Soft Delete | 把内容标记为已删除而非物理删除；已删除内容不可再编辑、回答、采纳（`QA-1008`） | `ContentDeletedEvent` |
| 浏览量 | View Count | 问题详情被查看的累计次数，只能经原子 SQL 递增（ADR-008） | `Question.viewCount`、`ViewCounter` |
| 问题引用 | QuestionRef | 供其他上下文查询的问题只读摘要 | `qa.api.QuestionRef`、`QaQueryApi` |

## 3. 激励与声誉（Reputation · `co-reputation`，核心域）

| 中文术语 | 英文命名 | 定义 | 代码落点 |
| :-- | :-- | :-- | :-- |
| 声誉账户 | ReputationAccount | 每个用户一个，注册时开立；同时持有声誉与悬赏积分 | `ReputationAccount` |
| 声誉 | Reputation | 用户因贡献获得的分数，只能经规则书变动，决定排行榜与徽章 | `ReputationAccount.reputation` |
| 悬赏积分 | Points | 用于发起悬赏的可用积分，**与声誉是两本账**；发起悬赏时冻结，结算时支出，到期退回 | `availablePoints`、`frozenPoints` |
| 声誉流水 | LedgerEntry | 只追加的记账记录，按 `(event_id, user_id, reason)` 去重 | `LedgerEntry`、`LedgerRepository` |
| 变动原因 | ReputationReason | 回答被采纳 / 问题被投票 / 回答被投票 / 赢得悬赏 | `ReputationReason` |
| 计分规则书 | Rulebook | 采纳 +15、投票计分、自问自答不计分等规则的唯一出处 | `ReputationRulebook` |
| 每日上限 | Daily Vote Cap | 每人每自然日（UTC）因被投票获得的正向声誉最多 200 | `ReputationRulebook.DAILY_VOTE_CAP` |
| 悬赏 | Bounty | **课程管理者**以悬赏积分为问题发起的奖励（10–500 分、1–14 天）；采纳时结算给回答者，到期未采纳则退款 | `Bounty`（非管理者发起 → `REP-3001`） |
| 悬赏状态 | BountyStatus | `OPEN` 进行中 / `AWARDED` 已结算 / `EXPIRED` 已到期退款 | `BountyStatus` |
| 徽章 | Badge | 达成里程碑后自动授予的标志，每类每人一次（初来乍到、初露锋芒、解惑达人、名师认可、小有名气、德高望重） | `BadgeType`、`BadgePolicy`、`BadgeAward` |
| 排行榜 | Leaderboard | 按周 / 月 / 学期 / 总计统计声誉增量的排名（Redis ZSET） | `LeaderboardPeriod` |
| 学期 | AcademicTerm | 排行榜周期键：2–7 月为春季学期，8 月–次年 1 月为秋季学期 | `AcademicTerm` |
| 对账 | Reconciliation | 每日核对账户余额与流水合计是否一致 | `ReconciliationService` |

## 4. 内容发现（Discovery · `co-discovery`）

| 中文术语 | 英文命名 | 定义 | 代码落点 |
| :-- | :-- | :-- | :-- |
| 投影 | Projection | 下游按事件维护的自有只读副本，**不反查上游的表** | `SearchIndexProjector`、`NotificationProjector` |
| 搜索投影 | SearchDocument | 由上游事件折叠出的搜索模型（标题、纯文本、标签、得分、是否已采纳、是否悬赏中） | `SearchDocument` |
| 搜索 | Search | 基于 MySQL ngram 全文索引的中英文关键词检索 + 筛选 + 排序 | `SearchQuery`、`SearchSort`、`SearchPort` |
| 相关推荐 | Related Questions | 问题详情页按标签集合 Jaccard 相似度推荐的相似问题（同分时得分高、已采纳者优先） | `RelatedQuestionPolicy` |
| 通知 | Notification | 站内消息：收到回答、被采纳、被认证、被 @、赢得悬赏、获得徽章；只能标记自己的通知已读 | `Notification`、`NotificationType` |
| @ 提及 | Mention | 评论中的 `@昵称`，每条评论最多提醒 5 人 | `MentionParser` |

## 5. 跨上下文通用（共享内核 `co-shared-kernel` 与基础设施）

| 中文术语 | 英文命名 | 定义 | 代码落点 |
| :-- | :-- | :-- | :-- |
| 操作者 | Actor | 当前请求的身份（用户 id、昵称、平台角色），由接口层传入应用服务 | `shared.security.Actor` |
| 集成事件 | Integration Event | 上下文之间唯一的「写」方向通知，过去时命名，带 `eventId` 与 `occurredAt` | `IntegrationEvent`、`EventIds` |
| 事务性发件箱 | Outbox | 与业务数据同一本地事务写入 `outbox_event`，再由中继异步投递 | `IntegrationEventPublisher`、`OutboxRelay` |
| 死信 | Dead Letter | 重试超过 10 次仍失败的发件箱事件 | 指标 `co_outbox_dead_letters` |
| 幂等 | Idempotency | 同一事件投递多次只生效一次 | `ProcessedEventStore` |
| 领域异常 | DomainException | 违反业务规则时抛出，经错误码映射为 HTTP 状态（见 [../错误码.md](../错误码.md)） | `shared.domain.*Exception` |

## 6. 不在本期范围的术语

以下词在 v1.0 事件风暴中出现过，但**当前代码未实现**，不得在接口、表名中提前使用；如需引入，先改本表并走 ADR：

| 术语 | 原定义 | 现状 |
| :-- | :-- | :-- |
| 举报 Report / 违规 Violation | 用户举报违规内容 | 无审核上下文；违规处理目前只有管理员禁用账号（PB-40） |
| 匿名 Anonymous | 隐藏身份发布 | 未实现 |
| 金币 Coin | 虚拟货币 | 由「悬赏积分 Points」承担，不另设货币 |

## 7. 易混淆词

| 不要说 | 应该说 | 原因 |
| :-- | :-- | :-- |
| 积分（指声誉） | 声誉 Reputation | 「积分」专指可花费的悬赏积分 Points |
| 点赞 / 踩 | 投票 Vote（+1 / −1） | 代码只有 `Vote`，值域 `{-1, +1}` |
| 删帖 | 软删除 | 内容不会物理删除 |
| 认证（指教师背书） | 教师认证 Endorse | 「认证 Authenticate」专指登录鉴权，属 Spring Security 而非领域概念 |
| 提问者发悬赏 | 课程管理者发起悬赏 | `Bounty.open` 要求发起人管理该课程 |
