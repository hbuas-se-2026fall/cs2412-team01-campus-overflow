# 聚合与不变式清单（Aggregates & Invariants）

> 依据：指导书 3.4 / 3.6 / ADR-008｜错误码以 [../错误码.md](../错误码.md) 为准（本文件与之保持一致）
> 用法：Code Review 时逐条对照；写领域方法前先确认“这条规则属于哪个聚合”。

## 1. 聚合总览

| 上下文 | 聚合根 | 聚合内实体 / 值对象 | 仓储接口 | 事务边界 |
| :-- | :-- | :-- | :-- | :-- |
| Q&A | `Question` | `Title`、`MarkdownBody`、`Tags`/`TagName` | `QuestionRepository` | 一次命令一个聚合 |
| Q&A | `Answer` | `MarkdownBody` | `AnswerRepository` | 同上 |
| Q&A | `Vote` | — | `VoteRepository` | 同上 |
| Q&A | `Comment` | — | `CommentRepository` | 同上 |
| Reputation | `ReputationAccount` | — | `ReputationAccountRepository` | 同上 |
| Reputation | `Bounty` | — | `BountyRepository` | 同上 |
| Reputation | （只追加）`LedgerEntry` | 值对象 record | `LedgerRepository` | 与账户同事务写入 |
| Discovery | `Notification` | — | `NotificationRepository` | 同上 |
| Identity | `User` | `PasswordPolicy`（策略） | `UserRepository` | 参考实现 |
| Identity | `Course` + `CourseMember` | — | `CourseRepository` | 参考实现 |

**跨聚合一律不走同一个事务**：需要联动时发集成事件（见 [events.md](events.md)）。

## 2. Q&A 上下文

### 2.1 `Question`（聚合根）

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 标题长度 | 10–150 字符（按 Unicode 码点） | `QA-1001`（422） |
| 正文长度 | 20–30000 字符 | `QA-1002`（422） |
| 标签数量 | 去重后 1–5 个，统一小写、去掉前导 `#` | `QA-1005`（422） |
| 标签格式 | `^[\p{L}\p{N}][\p{L}\p{N}#+.\-_]{0,29}$`，支持中文 | `QA-1004`（422） |
| 只能采纳一次 | `acceptedAnswerId` 已存在时再次采纳 → 409 | `QA-2002`（409） |
| 采纳权限 | 仅提问者本人或课程管理者 | `QA-3001`（403） |
| 采纳目标 | 被采纳的 `Answer` 必须 `questionId == id` | `QA-1009`（422） |
| 已删除不可操作 | 编辑 / 采纳 / 回答 / 删除前必须 `ensureNotDeleted` | `QA-1008`（422） |
| 提问者删除限制 | `answerCount > 0` 时提问者不能删除（课程管理者不受限） | `QA-1010`（422） |
| 编辑权限 | 作者本人或课程管理者 | `QA-3002`（403） |
| 计数器只读 | `score` / `answerCount` / `viewCount` 只能经 `adjustXxx` 原子 SQL 变化 | —（ADR-008） |

**工厂方法**：`Question.post(title, body, tags, courseId, actor, now)` —— id 为 null、计数器为 0、未采纳、未删除、`version = 0`；校验全部在值对象构造时完成。

### 2.2 `Answer`（聚合根）

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 只能答未删除的问题 | 提交前调用 `question.ensureOpenForAnswers()`（已采纳的问题仍可回答） | `QA-1008`（422） |
| 正文长度 | 10–30000 字符（**下限 10 为暂定值，待 PO 确认**） | `QA-1002`（422） |
| 认证唯一 | `endorsedBy` 已存在时再次认证 → 409 | `QA-2003`（409） |
| 认证权限 | 仅课程管理者（回答者本人也不能认证自己的回答） | `QA-3004`（403） |
| 编辑权限 | 作者本人或课程管理者 | `QA-3002`（403） |
| 删除限制 | 已被采纳的回答作者不能删除（防“骗采纳再删除”）；课程管理者可删 | `QA-1011`（422） |
| 是否被采纳 | 由 `Question.acceptedAnswerId` 决定，`Answer` 不自持该状态 | — |

### 2.3 `Vote`（独立聚合）

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 值域 | `value ∈ {-1, +1}`；撤销用 `0` 表示删除投票记录 | `QA-1006`（422） |
| 不能自投 | `voterId == targetAuthorId` → 422，消息中必须含“自己” | `QA-1015`（422） |
| 一人一票 | `(voterId, targetType, targetId)` 唯一 | `SYS-409-DUPLICATE`（409） |
| 改投语义 | `change` 返回旧值并更新 `updatedAt`；应用层按 `delta = new − old` 计分 | — |

### 2.4 `Comment`（独立聚合）

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 正文长度 | 1–600 字符（与控制器 `@Size(max = 600)` 一致） | `QA-1002`（422） |
| 只允许一层回复 | 父评论必须是顶层（`parentId == null`） | `QA-1007`（422） |
| 同目标 | 回复的 `targetType` / `targetId` 必须与父评论一致 | `QA-1012`（422） |

### 2.5 值对象自校验（构造即合法）

| 值对象 | 校验与规范化 |
| :-- | :-- |
| `Title` | 去首尾空白；10–150 码点 |
| `MarkdownBody` | 按类型区分长度：问题 20–30000、回答 10–30000、评论 1–600 |
| `TagName` | 去前导 `#`、转小写、trim；正则校验 |
| `Tags` | 逐个规范化、去重保序；1–5 个；`names()` 返回规范化列表 |

## 3. Reputation 上下文

### 3.1 `ReputationAccount`（聚合根）

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 开户 | `reputation = 0`、`available = initialPoints`（允许 0，不可为负）、`frozen = 0`、`version = 0` | `REP-1006`（422） |
| 余额 = 流水之和 | 任何 `reputation` 变化都必须先写 `rep_ledger`（唯一键 `(event_id, user_id, reason)`）；对账任务 FF-8 校验 | — |
| 不做下限截断 | `applyReputation(delta)` 直接累加，不得 `max(0, …)`，否则破坏账本不变量 | — |
| 冻结 | `points <= available` 否则 422；`available -= p, frozen += p` | `REP-1002`（422） |
| 支出 / 退回 | 均先 `ensureFrozen`；支出 `frozen -= p`；退回 `frozen -= p, available += p` | `REP-1006`（422） |

### 3.2 `Bounty`（聚合根·状态机）

```
OPEN ──采纳他人答案──▶ AWARDED（终态）
OPEN ──到期 / 发起人自答被采纳──▶ EXPIRED（终态）
```

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 发起权限 | 仅课程管理者 | `REP-3001`（403） |
| 积分范围 | 10–500 | `REP-1003`（422） |
| 期限范围 | 1–14 天 | `REP-1004`（422） |
| 问题状态 | 已删除或已采纳的问题不能悬赏 | `REP-1001`（422） |
| 不能自赏 | `winnerId == sponsorId` → 422（走退款） | `REP-1005`（422） |
| 唯一进行中 | 同一问题已有 OPEN 悬赏 → 409 | `REP-2001`（409） |
| 终态不可变 | `award` / `close` 前必须 `ensureOpen` | `REP-2002`（409） |
| 到期判定 | `isDue(now) = OPEN && !now.isBefore(expiresAt)`（到期时刻即视为到期） | — |

### 3.3 领域策略（无状态，纯函数）

| 策略 | 规则 |
| :-- | :-- |
| `ReputationRulebook.voteEffect` | `+1 → +10`、`-1 → -2`、`0 → 0` |
| `voteDelta(old, new)` | `effect(new) − effect(old)`；赞改踩 = `-2 − 10 = -12` |
| `applyDailyCap(delta, gainedToday)` | `delta ≤ 0` 原样返回；否则 `max(0, min(delta, 200 − gainedToday))` |
| `initialPoints(role)` | `TEACHER → 1000`、`STUDENT → 0`；**TA / ADMIN 取值待 PO 决策并写入 ADR** |
| `BadgePolicy` | 采纳数 ≥1 → `FIRST_ACCEPTED`、≥10 → `HELPER_10`；声誉 ≥100 → `REPUTATION_100`、≥1000 → `REPUTATION_1000` |
| `AcademicTerm` | 校园时区 `Asia/Shanghai`；2–7 月 `YYYY-SPRING`，8–12 月 `YYYY-FALL`，1 月属上一年 `YYYY-1-FALL`；ISO 周键 `2026-W41` |

## 4. Discovery 上下文

### 4.1 `Notification`（聚合根）

| 不变式 | 规则 | 错误码 |
| :-- | :-- | :-- |
| 标题 | 非空，超过 200 字符截断 | — |
| 初始状态 | `read = false` | — |
| 只能标记自己的 | `actorId != recipientId` → 403 | `DIS-3001`（403） |
| 幂等 | `(source_event_id, recipient_id)` 唯一；重复标记已读不报错 | — |

### 4.2 `SearchDocument`（投影，非聚合）

由事件折叠而来：`insert / updateText / markDeleted / markAccepted / answerCount±1 / score 调整 / bountyOpen`。写模型不是聚合，**不承载业务规则**，只保证与事件一致且幂等（`processed_event`）。

### 4.3 领域策略

| 策略 | 规则 |
| :-- | :-- |
| `SearchQuery.terms` | 按 `\s+` 切分、去空、去重保序、最多 5 个；清洗 `" + - *` 等运算符；词项 < 2 字符 → `DIS-1001`；关键词 > 50 字符 → `DIS-1002` |
| `RelatedQuestionPolicy.jaccard` | `shared / (a + b − shared)`，分母 0 返回 0 |
| `rank` | 按 J 降序 → 得分降序 → 已采纳优先 → 截取 limit |
| `MentionParser.extract` | 正则提取 `@昵称`，`LinkedHashSet` 去重保序，满 5 个停止；昵称不足 2 字符不匹配 |

## 5. Identity 上下文（参考实现，供对照）

| 聚合 | 关键不变式 | 错误码 |
| :-- | :-- | :-- |
| `User` | 密码 8–64 位且同时含字母与数字；`verify` / `changeRole` / `disable` 仅管理员；`reconstitute` 只还原状态不做校验 | `ID-1004`（422） |
| `Course` / `CourseMember` | 只有教师或管理员能建课；课程内教师角色不可被降级 | `ID-3001` 等 |

## 6. 三条容易违反的规则（Code Review 重点）

1. **业务规则写在应用服务里**（贫血模型）——例如在 `AnswerCommandService` 里判断 `answerCount > 0`；应下沉到 `Question.softDelete`。
2. **在 `reconstitute` 里重复业务校验**——历史数据会加载失败。
3. **回写计数器**——`question.setAnswerCount(...)` + `save` 会与并发请求互相覆盖；必须走 `adjustAnswerCount` 原子 SQL。

## 7. 待 PO 决策的两处产品规则（当前代码为暂定值）

| 项 | 当前实现 | 需确认 |
| :-- | :-- | :-- |
| 回答正文下限 | 10 字符（`MarkdownBody.MIN_ANSWER`），领域测试最短用例 13 字、无下界断言 | 是否保留 10，还是对齐问题的 20 |
| TA / ADMIN 的初始悬赏积分 | 仅 `TEACHER → 1000`、`STUDENT → 0` 有断言 | TA、ADMIN 取值（如 TA 500 / ADMIN 1000），确认后写入 ADR-004 与错误码文档 |
