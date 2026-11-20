# 团队章程 v1.0

## 1. 团队价值观
- 可演示优先于完美文档
- 小步提交优先于大块合并
- 先澄清验收标准再开发
- 对事不对人，问题及时暴露

## 2. 沟通约定
- IM 群为异步沟通主渠道，工作时段 2 小时内响应
- 每日 Standup 由 SM 主持
- 阻塞超过 30 分钟必须在群里提出
- PR 评审请求发出后 4 小时内给出首次反馈

## 3. 开发规约
- 不直接推 main，所有变更走 feature/fix 分支 + PR
- 分支命名：feature/xxx、fix/xxx、hotfix/xxx
- 提交信息前缀：feat:/fix:/docs:/refactor:/test:
- PR 至少 1 人 review + LGTM，由 SM 合并
- 每个 PR 尽量小，单次不超过 300 行有效变更

## 4. DoD (Definition of Done) 初版
一条用户故事完成必须满足：
- [ ] 代码已合并到 main
- [ ] 有对应单元测试或验收场景
- [ ] 通过本地构建/lint/typecheck
- [ ] PR 已评审并合并
- [ ] 可在演示环境演示
- [ ] 相关文档/ADR 已更新

## 全员签名

| 角色 | 成员 | GitHub |
| :-- | :-- | :-- |
| PO | 韦国超（组长） | @jingmo2233 |
| SM（轮值，见下表） | 冯先顺 / 宾艺涵 / 陈阳 / 张博 | @fxs0917 / @XY1897 / @yttt697 / @llll-b01 |
| Developer | 宾艺涵、陈阳、张博、冯先顺 | — |

SM 按 Sprint 轮值，每届只写一次，避免“到底谁是 SM”说不清：

| Sprint | SM | 记录 |
| :-- | :-- | :-- |
| S0 | 冯先顺 | [scrum/sprint-0.md](../scrum/sprint-0.md) |
| S1 | 宾艺涵 | [scrum/sprint-1.md](../scrum/sprint-1.md) |
| S2 | 陈阳 | [scrum/sprint-2.md](../scrum/sprint-2.md) |
| S3 | 张博 | [scrum/sprint-3.md](../scrum/sprint-3.md) |
| S4 | 冯先顺 | [scrum/sprint-4.md](../scrum/sprint-4.md) |