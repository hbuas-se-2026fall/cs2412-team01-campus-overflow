---
name: 任务（Task）
about: Sprint Backlog 中从故事拆出的开发任务，粒度 ≤ 1 天
title: "[T?-??] "
labels: ["type:task"]
assignees: ""
---

## 所属故事

Closes #____ / 关联 #____

## 做什么 & 为什么

<!-- 一句话说明改动与动机；若偏离原设计，说明理由 -->

## 涉及模块与层

- [ ] `domain`　- [ ] `application`　- [ ] `infrastructure`　- [ ] `interfaces`
- [ ] `frontend`　- [ ] `ci` / `deploy`　- [ ] `docs`
- 路径：`backend/co-.../...`

## 完成标准（DoD 子集）

- [ ] 先写失败测试（红）→ 实现（绿）→ 重构
- [ ] `mvn -B test -pl <模块> -am` 通过；`ArchitectureTest` 通过
- [ ] **未修改任何已有测试断言、未删除或 @Disabled 测试**
- [ ] 错误码符合 `docs/错误码.md`；写操作在事务内、事件在保存后发布
- [ ] PR 关联本 Issue，至少 1 名非作者 Approve

## 估算

- 工时： 4h　｜　负责人：张博　｜　目标分支：`feature/<issue号>-<英文短描述>`
- 计划完成日：2天（分支生命周期 ≤ 3 天）
