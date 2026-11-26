# 部署冒烟实测记录

> 目的：证明 `deploy/` 的生产编排（MySQL + Redis + backend + nginx）真的能起来，并能完成登录闭环。
> 结论：**通过**，并且第一次运行就抓到一个真实缺陷。
> 执行环境：GitHub Actions `ubuntu-latest`（自带 Docker），公开仓库不计额度。

## 为什么在 CI 里跑

组里多数机器没装 Docker；本地装 Docker Desktop 需要启用 WSL、重启、三次 UAC。
GitHub runner 自带 Docker，跑完还能留下日志与运行链接，评审时可直接引用。

工作流：[`.github/workflows/deploy-smoke.yml`](../../.github/workflows/deploy-smoke.yml)（PR #124）
触发方式：Actions → Deploy Smoke → Run workflow；或修改 `deploy/**` 的 PR 自动触发。

## 第一次运行：失败

[run #1 · 45727235473](https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/actions/runs/45727235473)

后端容器反复重启，日志：

```
Error: Unable to access jarfile app.jar
```

根因在 `deploy/backend.Dockerfile`：

```dockerfile
COPY --from=build /src/co-bootstrap/target/co-bootstrap-*.jar app.jar
```

`co-bootstrap/pom.xml` 里设了 `<finalName>campus-overflow</finalName>`，实际产物是
`campus-overflow.jar`，通配符匹配不到。构建日志可证：

```
--- jar:3.4.2:jar (default-jar) @ co-bootstrap ---
Building jar: /src/co-bootstrap/target/campus-overflow.jar
--- spring-boot:3.4.5:repackage (repackage) @ co-bootstrap ---
```

**修复**：改为固定路径 `COPY --from=build /src/co-bootstrap/target/campus-overflow.jar app.jar`。

## 第二次运行：通过

[run #2 · 45727743269](https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow/actions/runs/45727743269)

### 容器启动顺序

```
Container co-redis-prod Started
Container co-mysql-prod Started
Container co-mysql-prod Healthy
Container co-redis-prod Healthy
Container co-backend-prod Started
Container co-backend-prod Healthy
Container co-web-prod Started
```

### 八条断言的实际输出

| # | 检查 | 实际输出 |
| --: | :-- | :-- |
| 1 | `docker compose config -q` | `compose config OK` |
| 2 | 全栈 `up -d --build` | 四个容器全部 Started，mysql/redis/backend 转为 Healthy |
| 3 | 后端 readiness 探针 | `[1] backend=healthy`（第一次轮询即通过） |
| 4 | `GET /` 经 nginx | `GET / -> 200` |
| 5 | `GET /questions`（SPA 回退） | `GET /questions -> 200` |
| 6 | `GET /actuator/health` 经反代 | `{"status":"UP","groups":["liveness","readiness"]}` |
| 7 | `POST /api/v1/auth/register` | `POST /auth/register -> 201` |
| 8 | `POST /auth/login` + `GET /auth/me` | `POST /auth/login -> 200`、`GET /auth/me -> 200` |

`/auth/me` 的响应体：

```json
{"id":1,"username":"20269001","displayName":"冒烟测试","email":"smoke@campus.edu",
 "role":"STUDENT","verified":false,"college":"计算机工程学院","className":"计科2301","courses":[]}
```

## 这个记录能证明什么、不能证明什么

**能证明**：镜像能构建、容器编排正确、Flyway 迁移能跑、nginx 反代与会话 Cookie 链路通、
CSRF 双提交可用、注册与登录接口在真实容器里工作。

**不能证明**：

- **浏览器端行为**。工作流用 `curl`，它对 `Set-Cookie: ...; Secure` 比浏览器宽松，
  在纯 HTTP 下照样回传 Cookie。`prod` profile 曾把 `secure` 写死为 `true`（#123 修复），
  真实浏览器在 `http://IP` 下会丢弃该 Cookie，表现为「登录成功但立刻回到未登录」。
  本记录用的是 `COOKIE_SECURE=false`，所以这一条要另外验证；
- **并发与性能**，这里只验证「能起来、能登录」；
- **`deploy.sh` 自身的逻辑**（回滚标签、`preflight`），工作流直接调用 `docker compose`。

## 如何复现

```bash
cp deploy/.env.example deploy/.env
# 填入：MYSQL_ROOT_PASSWORD / DB_PASSWORD / CO_FIELD_KEY（openssl rand -base64 32）
# 纯 HTTP 演示保持 COOKIE_SECURE=false
bash deploy/deploy.sh
```

本地跑完之后应有同样的八条输出。若第 8 条失败（`/auth/me` 不是 200），先查 `COOKIE_SECURE`
与浏览器/客户端的 Cookie 策略，其次查 `SESSION_COOKIE` 是否被 nginx 转发破坏。