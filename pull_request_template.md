## 关联 Issue

Closes #

## 改动说明

<!-- 做了什么、为什么这样做；涉及哪个上下文与哪一层 -->

- 上下文 / 层：`ctx:` ____ / `domain` `application` `infrastructure` `interfaces` `frontend` `ci`
- 影响范围：新增方法 ____ 个，修改 ____ 个；是否改动公共契约（`*-api`）：是 / 否

## 测试证据

<!-- 粘贴 mvn test 输出摘要或截图；前端改动附截图/GIF -->

```text
$ mvn -B test -pl co-qa/co-qa-domain -am
Tests run: __, Failures: 0, Errors: 0, Skipped: 0
```

## 自检清单

- [ ] 本地 `mvn test -pl <模块> -am` 通过，`ArchitectureTest` 通过
- [ ] 业务规则位于领域层，领域层零框架依赖（FF-1）
- [ ] 跨上下文只经 `*-api` 或集成事件（FF-2 / FF-3）
- [ ] 新增规则有单元测试；**未修改任何已有测试断言、未删除或 @Disabled 测试**
- [ ] 异常类型与错误码符合 `docs/错误码.md`；接口变更已同步 Swagger
- [ ] 写操作在 `@Transactional` 内；事件在保存之后发布；消费端幂等
- [ ] 计数器走 `adjustXxx` 原子更新，未回写聚合
- [ ] 前端改动 `npm run typecheck && npm run build` 通过
- [ ] 无调试代码、无密钥、无无关格式化改动
- [ ] 使用了 AI 辅助：否 / 是（说明：……）
