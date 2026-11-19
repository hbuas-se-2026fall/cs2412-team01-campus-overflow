# 测试与覆盖率报告

《实验指导书》9.4 要求提交「测试报告与覆盖率报告截图」，这个目录就是放它们的地方。
评审时老师会翻这里，所以文件名带 Sprint 号，按时间排。

## 怎么生成

```bash
cd backend

# 领域层 + 应用层测试与覆盖率（不跑端到端，不需要 Docker）
mvn -B verify -DskipITs

# 报告位置
# backend/co-qa/co-qa-domain/target/site/jacoco/index.html
# backend/co-reputation/co-reputation-domain/target/site/jacoco/index.html
# ……每个模块一个

# 端到端测试（需要本机 Docker）
mvn -B verify -pl co-bootstrap -Dtest=CampusOverflowFlowIT
```

覆盖率门禁在 `backend/pom.xml` 的 `domain.coverage.minimum`（当前 0.75），低于阈值构建会失败。

## 放什么

| 文件 | 内容 | 谁提交 |
| :-- | :-- | :-- |
| `sprint-N-tests.png` | `mvn test` 的汇总输出（Tests run / Failures / Errors） | SM |
| `sprint-N-coverage.png` | 四个 `*-domain` 模块的 JaCoCo 汇总页 | SM |
| `sprint-N-it.png` | `CampusOverflowFlowIT` 通过时的控制台片段 | 负责端到端的同学 |
| `sprint-N-ci.png` | Actions 上该次运行的绿色截图 | SM |

命名用 `sprint-0` 到 `sprint-4`，一个 Sprint 一组，别堆在一起。

## 和 CI 的关系

CI 现在只跑 `verify -DskipITs`，所以：

- 单元测试与覆盖率**有** CI 证据，截图之外还可以直接贴 Actions 链接；
- 端到端测试**没有** CI 证据（`-DskipITs`），只能本地跑完截图。这一点评审时主动说明，比被问出来好。

## 历史

Sprint 0 ~ 2 的报告还没归档，补的时候从 Actions 的历史运行里翻（`Actions → 选一次绿色运行 → Summary`）。