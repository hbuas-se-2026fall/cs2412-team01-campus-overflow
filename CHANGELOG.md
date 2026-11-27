# 变更日志

本项目按 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 组织，版本号与 `main` 上的 tag 对应。
每个版本的完整说明见 `docs/release/`。

## [Unreleased]

Sprint 4（稳定化与发布）尚未开始，目标版本 `v1.0.0`（计划 2026-12-10）。

## [0.9.0] - 2026-11-28

Sprint 3 交付点（发布候选），对应提交 `515b87c`，GitHub pre-release。
本版不是新写功能，而是把已经落地的搜索 / 相关推荐 / 通知三条链路**验证一遍**并纳入 CI 门禁。
完整说明见 [`docs/release/v0.9.0.md`](docs/release/v0.9.0.md)。

### 新增
- 端到端测试进入 CI：独立的 `e2e` job 用 Testcontainers 起 MySQL 8 + Redis 7，跑 `CampusOverflowFlowIT`（#119）
- 部署冒烟工作流：在 runner 上跑一遍 `docker-compose.prod.yml`，验证到「注册 → 登录 → `/auth/me` 200」（#124）
- CI 按变更范围裁剪：纯文档 PR 不再装 JDK、跑 9 个模块的 `verify`；判定失败时兜底全量，主干 push 无条件全量（#120）
- 后端 job 上传 JaCoCo 站点与 surefire 报告为 artifact，保留 30 天（#120）
- `frontend/package-lock.json`，CI 与前端镜像改用 `npm ci`（#131）
- 追踪矩阵 `docs/backlog/traceability.md`：48 条 PBI 的 Issue / PR / 测试 / 版本对应关系（#126）
- `docs/reports/deploy-smoke.md`、`docs/reports/sprint-3-review.md`（#128、#129）

### 修复
- 后端镜像 `COPY` 的 jar 名与 pom `<finalName>` 不一致，容器报 `Unable to access jarfile app.jar`（#124）
- `prod` 的 Secure Cookie 改为可被 `COOKIE_SECURE` 覆盖，便于纯 HTTP 环境演示（#123）

### 变更
- `develop` 分支保护启用必需状态检查：`后端构建与验证`、`前端构建`、`端到端测试`
- `ADR-009` 状态改为「已完成」
- 领域层覆盖率门禁维持 0.75（`co-discovery-domain` 已补至 100%）

## [0.3.0] - 2026-11-19

Sprint 2 交付点，对应提交 `cd95c3c`。

### 新增
- 声誉与激励：账户开立（教师初始 1000 分）、采纳 +15（自问自答不加分）、被赞 +10 / 被踩 −2 / 每日正向 ≤ 200
- 徽章、排行榜（周/月/学期/总）、声誉档案与流水、每日对账
- 悬赏：发起 10–500 分 / 1–14 天并冻结积分，采纳自动结算、到期自动退款
- 前端悬赏发起对话框与倒计时（#79）

### 修复
- 搜索投影只统计问题本身的投票，不再混入回答的投票（#64）
- JDBC `characterEncoding` 改为 UTF-8，修复后端无法启动（#63）
- `main` ↔ `develop` 历史分叉，`ci.yml` 与同名测试类的冲突（#58、#66）

### 变更
- 领域层覆盖率门禁 0 → 0.75，并显式开启 `haltOnFailure`（#62）
- Testcontainers 升级 1.21.4，修复 Docker Engine 29 下端到端测试无法启动（#67）

### 文档
- `docs/scrum/`：Sprint 0–4 记录、S1 燃尽图与数据
- `docs/adr/010`：已删除内容的错误码定案
- `docs/backlog/traceability.md`：PBI → Issue → PR → 测试 → 版本的追踪矩阵
- `.editorconfig`、`CONTRIBUTING.md`、`docs/reports/`、`docs/final/` 目录说明

## [0.2.0] - 2026-11-05

Sprint 1 交付点，对应提交 `1399c10`。

### 新增
- 问答闭环：提问、回答、投票/改投/撤销、评论与一层回复、采纳、教师认证、编辑与软删除
- 前端：按权限显示编辑/删除/采纳/认证按钮，错误码转中文提示

### 文档
- `docs/release/v0.2.0.md`、`docs/retro/sprint-1.md`

## [0.1.0] - 2026-10-27

Sprint 0 基线，对应提交 `602bf5b`。

### 新增
- 27 个 Maven 模块骨架（上下文 × 六边形分层）
- Identity 上下文完整参考实现：注册、登录、课程与成员、角色权限
- 共享内核、事务性发件箱与中继、Redis 服务端会话、Flyway 迁移
- `deploy/docker-compose.yml`：MySQL 8（ngram 分词）+ Redis 7
- CI 初版：编译 + 单测 + ArchUnit + 前端构建
- ADR 001–009、团队章程、Product Backlog v1.0

[Unreleased]: https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/compare/v0.3.0...develop
[0.3.0]: https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/releases/tag/v0.3.0
[0.2.0]: https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/releases/tag/v0.2.0
[0.1.0]: https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/releases/tag/v0.1.0