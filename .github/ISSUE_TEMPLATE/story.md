---
name: 用户故事（PBI）
about: Product Backlog Item —— 进入 Sprint 前必须满足 DoR（见 4.4 节）
title: "[PB-xx] "
labels: ["type:story"]
assignees: ""
---

<!-- 提交前自检：价值明确 / 有可验证的验收标准 / 已估算且 ≤ 8 点 / 依赖已识别 -->

## 用户故事

作为 <角色>，我希望 <功能>，以便 <价值>。

## 归属

- 史诗：E?（E0 协作基础设施 / E1 Identity / E2 Q&A / E3 Reputation / E4 悬赏 / E5 Discovery / E6 质量与发布 / E7 前端）
- 限界上下文：`ctx:qa` / `ctx:reputation` / `ctx:discovery` / `ctx:identity` / `ctx:frontend`
- 前置依赖：PB-xx / 无

## 验收标准（Given / When / Then）

- [ ] 快乐路径：Given … When … Then …
- [ ] 边界路径：Given … When … Then …（明确边界值：标题 10/150 字、标签 1/5 个、悬赏 10/500 分、期限 1/14 天）
- [ ] 异常路径：Given … When … Then … 返回 `<HTTP 状态>` `<错误码>`

## 规格与实现落点

- 相关测试：`QuestionTest#askerCanAcceptAnAnswerOnce`（已有 / 需新增）
- 涉及方法：`Question.accept`、`AnswerCommandService.accept`
- 发布 / 订阅事件：`AnswerAcceptedEvent`

## 估算与 DoR

- [ ] Story Points：____（≤ 8，超过请拆分）
- [ ] Sprint：____
- [ ] 已标注对应测试类；依赖已识别；对外部事件字段的需求已确认

## 任务拆分

- [ ] #任务1　- [ ] #任务2
