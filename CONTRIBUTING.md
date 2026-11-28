# 贡献指南

面向本组 5 名成员。流程与《实验指导书》第 7 章一致，这里只写实操时需要照做的那几条。

## 环境

- JDK 21、Maven 3.9+、Docker（跑中间件与端到端测试）、Node 20
- 统一用 UTF-8；编辑器请开启 EditorConfig 支持（仓库根的 `.editorconfig`）
- 提交前确认 `git config user.name` / `user.email` 是**你自己**的 GitHub 账号，不要共用邮箱

## 分支

| 分支 | 用途 |
| :-- | :-- |
| `main` | 只接受发布 PR，合并后打 tag |
| `develop` | 集成分支，功能 PR 都往这里合 |
| `feature/*`、`fix/*`、`docs/*`、`chore/*` | 从 `develop` 切出，合并后删除 |

不要直接往 `main` / `develop` 推，也不要 force push。合并后在 GitHub 上删除分支（或开启 Settings → General 的
「Automatically delete head branches」，让平台自动删）。

## 提交信息

Conventional Commits，前缀用 `feat:` `fix:` `test:` `docs:` `refactor:` `chore:` `ci:`。

```
fix(qa): 已删除的问题不再允许回答
docs(scrum): 补 Sprint 2 记录
```

正文写「为什么」而不是「改了什么」——改了什么看 diff 就知道。

## 提交编码与合并方式（#138）

Windows 下有个坑：PowerShell 5.1 默认按 GBK 传参，中文提交标题会变成 `?`。
11-28 那批合并里有 5 个提交中招（`9410af4`、`6fa477b`、`0254f25`、`975c7c8`、`515b87c`），
**按「不改写历史」原则保留原样**，但之后不要再产生新的。

| 约定 | 做法 |
| :-- | :-- |
| 优先在网页合并 | GitHub 服务端按 UTF-8 处理，标题取自 PR 标题，不会乱码 |
| 本地命令行合并 | 用 **Git Bash**（不是 PowerShell 5.1），并设置 `git config --global i18n.commitEncoding utf-8` |
| 提交前自检 | `git log -1 --format=%s`，标题里不该出现 `?` |
| 功能 / 文档 PR | **Squash and merge**：`develop` 上一个故事一个提交 |
| 发布 PR（develop → main） | **Create a merge commit**：squash 会让 `v0.9.0` 这类标签与 `main` 脱钩 |
| 关闭 Issue | `Closes #N` 只在合入**默认分支（`main`）**时生效；合到 `develop` 需作者手动 Close |

## PR

1. 一个 PR 只做一件事，控制在 400 行以内（自动生成的文件不计）；
2. 正文写清关联 Issue：**`Closes #<Issue 号>`**，合并后会自动关闭；
3. 有测试变动的，贴 `mvn test` 的输出摘要；
4. 至少要 1 名非作者 Approve；评审人 24 小时内给首次反馈；
5. 作者负责回复每条评论并 Resolve，有分歧就升级到站会或写 ADR。

## 本地自检

```bash
# 后端
cd backend && mvn -B test
mvn -B test -pl co-bootstrap -Dtest=ArchitectureTest

# 覆盖率门禁（领域层 ≥ 0.75）
mvn -B verify -DskipITs

# 端到端（需要本机 Docker）
mvn -B verify -pl co-bootstrap -Dtest=CampusOverflowFlowIT

# 前端
cd frontend && npm install && npm run typecheck && npm run build
```

## 红线

- 不修改已有测试的断言、不 `@Disabled`、不删除测试「让它变绿」
- 不提交密钥、真实学号、手机号
- 不绕过分支保护；管理员也不要用 bypass 合并
- 使用了 AI 辅助要在 PR 里说明，并保证能解释每一行提交的代码