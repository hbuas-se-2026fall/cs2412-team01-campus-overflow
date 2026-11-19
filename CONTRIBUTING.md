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

不要直接往 `main` / `develop` 推，也不要 force push。

## 提交信息

Conventional Commits，前缀用 `feat:` `fix:` `test:` `docs:` `refactor:` `chore:` `ci:`。

```
fix(qa): 已删除的问题不再允许回答
docs(scrum): 补 Sprint 2 记录
```

正文写「为什么」而不是「改了什么」——改了什么看 diff 就知道。

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