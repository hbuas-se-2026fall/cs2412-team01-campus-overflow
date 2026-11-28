# CampusOverflow · 校园问答平台

> 《软件工程》课程贯穿项目 ｜ 团队 cs2412-team01 ｜ 模块化单体 × DDD × Scrum × GitHub 协作
> Java 21 · Spring Boot 3.4 · MySQL 8 · Redis 7 · React 18 · GitHub Actions

## 0. 项目状态（2026-11-28）

| 项 | 状态 |
| :-- | :-- |
| 待实现方法 | **0 / 107**（起始代码的 107 个 `TODO(Sx)` 已全部清零） |
| 测试 | 61 个测试类（领域 + 应用 + 契约 + 端到端），`develop` 上 CI 绿（含独立 `e2e` job） |
| 领域层覆盖率门禁 | **0.75**（`backend/pom.xml` 的 `domain.coverage.minimum`，`haltOnFailure=true`） |
| 架构约束 | ArchUnit FF-1~FF-6 全绿（无框架侵入、无跨上下文越界、无环） |
| 当前 Sprint | S3 内容发现（11-20 ~ 12-03，**评审待开**）→ S4 稳定化（12-04 起） |
| 交付版本 | `v0.1.0`（S0）、`v0.2.0`（S1）、`v0.3.0`（S2）、`v0.9.0`（S3 RC，pre-release）；`v1.0.0` 计划 12-10 |

## 1. 快速开始

```bash
# 1) 起中间件（MySQL 8 已配 ngram 分词，Redis 7）
docker compose -f deploy/docker-compose.yml up -d mysql redis

# 2) 编译 + 跑测试
cd backend && mvn clean test

# 3) 启动后端（dev profile；不要加 demo，演示数据依赖 S3 之前的业务完成度）
mvn spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=dev

# 4) 启动前端
cd ../frontend && npm install && npm run dev      # http://localhost:5173
```

- 接口文档：<http://localhost:8080/swagger-ui.html>
- 健康检查：`curl http://localhost:8080/actuator/health`
- Postman 集合：[docs/postman/](docs/postman/CampusOverflow.postman_collection.json)（6 文件夹 / 23 请求，含负向用例）

## 2. 工程结构

27 个 Maven 模块：`co-bootstrap`（启动与安全装配）+ `co-shared-kernel`（领域异常、Actor、事件契约、分页）
+ 4 个限界上下文，每个上下文再分 `api / domain / application / infrastructure / interfaces` 五层。

| 上下文 | 职责 | 对外契约 |
| :-- | :-- | :-- |
| `co-identity` | 注册登录、课程与成员关系、角色 | `IdentityApi`、`UserRegisteredEvent` |
| `co-qa` | 提问、回答、投票、评论、采纳、教师认证 | `QaQueryApi`、Q&A 领域事件 |
| `co-reputation` | 声誉账本、徽章、悬赏、排行榜、对账 | `ReputationApi`、`AnswerAcceptedEvent` 等订阅 |
| `co-discovery` | 搜索投影、相关推荐、通知与 @ 提及 | 只读投影，订阅全部上游事件 |

跨上下文**只能**经 `*-api` 或集成事件；数据库按表名前缀（`id_` `qa_` `rep_` `dis_`）隔离，禁止跨前缀 JOIN。

## 3. Sprint 计划（8 周）

| Sprint | 周次 | 目标 | 版本 | 记录 |
| :-- | :-- | :-- | :-- | :-- |
| S0 | 1 | 启动与基础设施：仓库、环境、CI、Backlog | `v0.1.0` | [sprint-0](docs/scrum/sprint-0.md) |
| S1 | 2–3 | 问答核心：提问 → 回答 → 投票 → 评论 → 采纳 | `v0.2.0` | [sprint-1](docs/scrum/sprint-1.md) |
| S2 | 4–5 | 声誉与激励：账本、徽章、悬赏、排行榜 | `v0.3.0` | [sprint-2](docs/scrum/sprint-2.md) |
| S3 | 6–7 | 内容发现：搜索、推荐、通知、端到端贯通 | `v0.9.0` | [sprint-3](docs/scrum/sprint-3.md) |
| S4 | 8 | 稳定化与发布：可观测性、一键部署、答辩 | `v1.0.0` | [sprint-4](docs/scrum/sprint-4.md) |

## 4. 文档地图

| 想了解 | 去哪看 |
| :-- | :-- |
| 需求基线与优先级（48 条 PBI / 163 点） | [docs/backlog/product-backlog.md](docs/backlog/product-backlog.md) |
| PBI → Issue → PR → 测试 的追踪 | [docs/backlog/traceability.md](docs/backlog/traceability.md) |
| 验收标准（Gherkin 场景） | [docs/backlog/stories/](docs/backlog/stories/) |
| Scrum 记录（计划 / 评审 / 燃尽 / 回顾） | [docs/scrum/](docs/scrum/) |
| 领域模型、上下文映射、事件目录 | [docs/ddd/](docs/ddd/) |
| 架构决策（10 条 ADR） | [docs/adr/](docs/adr/README.md) |
| 错误码与 HTTP 状态映射 | [docs/错误码.md](docs/错误码.md) |
| 发布说明 | [docs/release/](docs/release/)（v0.1.0 ~ v0.9.0，索引见 [README](docs/release/README.md)） |
| 版本变更日志 | [CHANGELOG.md](CHANGELOG.md) |
| 部署与运维 | [deploy/README.md](deploy/README.md) |
| 起始代码的 107 个 TODO 清单（历史存档） | [docs/学生任务清单.md](docs/学生任务清单.md) |

## 5. 团队与协作

| 角色 | 成员 | GitHub |
| :-- | :-- | :-- |
| PO | 韦国超 | @jingmo2233 |
| SM（轮值） | 冯先顺（S0/S4）、宾艺涵（S1）、陈阳（S2）、张博（S3） | @fxs0917 / @XY1897 / @yttt697 / @llll-b01 |
| Developer | 宾艺涵、陈阳、张博、冯先顺 | 同上 |

- 分支：`main`（只接受发布 PR、打 tag）← `develop`（集成分支）← `feature/*`、`fix/*`、`docs/*`
- 提交信息：Conventional Commits（`feat:` `fix:` `test:` `docs:` `refactor:` `chore:` `ci:`）
- PR 规则：一个 PR 一个 Task/Story，正文写 **`Closes #<Issue>`**，至少 1 名非作者 Approve
- 详见 [团队章程](docs/team/charter-v1.0.md)

## 6. 验收门槛（自检）

| 项目 | 命令 | 要求 |
| :-- | :-- | :-- |
| 单元测试 | `cd backend && mvn test` | 全绿 |
| 领域覆盖率 | `mvn verify` | `domain.coverage.minimum = 0.75` |
| 架构约束 | `mvn test -pl co-bootstrap -Dtest=ArchitectureTest` | FF-1~FF-6 通过 |
| 集成测试 | `mvn verify`（需 Docker） | `CampusOverflowFlowIT` 通过 |
| 前端 | `cd frontend && npm run typecheck && npm run build` | 通过 |
| 流水线 | 推送后看 Actions | 全绿 |

**红线**：不修改已有测试断言、不 `@Disabled`、不删除测试、不 force push `main`/`develop`、不提交密钥与真实学号。

## 7. 常见坑

| 现象 | 原因 |
| :-- | :-- |
| `UnsupportedOperationException: TODO(Sx)` | 历史现象；107 个 TODO 已全部实现，若再出现说明合并回了旧分支 |
| 声誉不变化 | 声誉是**异步**结算：事件先落 `outbox_event`，中继投递后再记账；看 `last_error` |
| 中文搜索没结果 | 必须用 compose 里的 MySQL（`ngram_token_size=2`），自建库需对照参数 |
| 事件重复导致加分两次 | 消费端必须幂等：账本唯一键 `(event_id, user_id, reason)` 或 `processed_event` 表 |
| 提交后 CI 红 | 先本地 `mvn verify`，再推送；红的分支不要合进 `develop` |
| 已删除内容返回 422 `QA-1008` 而非 404 | 这是决策而非缺陷，见 [ADR-010](docs/adr/010-已删除内容的错误码.md) |