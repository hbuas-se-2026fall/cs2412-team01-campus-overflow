# 一键部署验收清单（v1.0.0）

> 用途：**发布前**在干净环境按本清单跑一遍，逐条打勾，把结果贴进 `docs/release/v1.0.0.md` 的验收段。
> 与 [deploy-smoke.md](deploy-smoke.md) 的分工：那篇记录「已经跑过什么」，本篇规定「发布前还要再跑什么、怎么算过」。
> 依据：指导书 9.4「GitHub Release（含部署说明）」与 PB-46。

## 0. 环境前提

| 项 | 要求 | 检查命令 | 本次结果 |
| :-- | :-- | :-- | :--: |
| Docker | ≥ 20.10，守护进程可用 | `docker version` / `docker info` | ☐ |
| Compose | v2（`docker compose`，非 `docker-compose`） | `docker compose version` | ☐ |
| 资源 | ≥ 2 核 4 GB，磁盘 ≥ 10 GB | `nproc` / `free -h` / `df -h` | ☐ |
| 端口 | `WEB_PORT`（默认 80）未被占用 | `ss -lntp \| grep :80` | ☐ |
| 数据卷 | 首次验收用**干净卷**（`docker volume ls \| grep mysql-prod-data` 应为空） | 同左 | ☐ |
| 网络 | 能拉 `mysql:8.0.39`、`redis:7-alpine`、`nginx`、`maven`、`node` 基础镜像 | `docker pull mysql:8.0.39` | ☐ |

> 国内网络拉 `registry-1.docker.io` 常失败，先配加速源（`/etc/docker/daemon.json` 的 `registry-mirrors`），
> 否则会在 `docker compose build` 阶段卡住——这与代码无关。

## 1. 配置

```bash
git clone https://github.com/hbuas-se-2026fall/cs2412-team01-campus-overflow.git
cd cs2412-team01-campus-overflow
git checkout v1.0.0                      # 验收必须针对 tag，不是 develop
cp deploy/.env.example deploy/.env
```

| # | 检查 | 做法 | 期望 | 结果 |
| --: | :-- | :-- | :-- | :--: |
| 1.1 | 占位值已清空 | `grep change_me deploy/.env` | 无输出 | ☐ |
| 1.2 | 字段密钥已生成 | `openssl rand -base64 32` 填入 `CO_FIELD_KEY` | 44 字符 Base64 | ☐ |
| 1.3 | Cookie 策略正确 | 纯 HTTP 演示设 `COOKIE_SECURE=false`；HTTPS 保持 `true` | 与访问方式一致 | ☐ |
| 1.4 | 配置可解析 | `cd deploy && docker compose -f docker-compose.prod.yml config -q` | 退出码 0 | ☐ |

## 2. 部署

```bash
bash deploy/deploy.sh
```

| # | 检查 | 期望 | 结果 |
| --: | :-- | :-- | :--: |
| 2.1 | 前置检查通过 | 不出现 `缺少 deploy/.env` / `change_me` 报错 | ☐ |
| 2.2 | 镜像构建 | backend 与 web 镜像构建成功（首次约 15 分钟） | ☐ |
| 2.3 | 容器状态 | `docker ps` 中 `co-mysql-prod`/`co-redis-prod`/`co-backend-prod` 为 `(healthy)`，`co-web-prod` 为 `Up` | ☐ |
| 2.4 | 脚本收尾 | 最后一行输出 `部署完成 ✔` | ☐ |
| 2.5 | 不暴露数据库 | `ss -lntp` 无 3306 / 6379（生产编排不向宿主机暴露） | ☐ |

启动顺序应为：`redis` → `mysql` → `mysql healthy` → `redis healthy` → `backend` → `backend healthy` → `web`。

## 3. 八条断言

在部署机执行，`BASE` 按实际端口调整：

```bash
BASE=http://localhost:${WEB_PORT:-80}
JAR=$(mktemp)
```

| # | 断言 | 命令 | 期望 | 结果 |
| --: | :-- | :-- | :-- | :--: |
| 1 | 前端首页 | `curl -s -o /dev/null -w '%{http_code}\n' "$BASE/"` | `200` | ☐ |
| 2 | SPA 路由回退 | `curl -s -o /dev/null -w '%{http_code}\n' "$BASE/questions"` | `200`（不是 404） | ☐ |
| 3 | 健康检查经反代 | `curl -fsS "$BASE/actuator/health"` | `{"status":"UP","groups":["liveness","readiness"]}` | ☐ |
| 4 | 取 CSRF | `curl -sS -c "$JAR" -b "$JAR" "$BASE/api/v1/auth/csrf" -o /dev/null` | `$JAR` 里出现 `XSRF-TOKEN` | ☐ |
| 5 | 注册 | 见下方脚本，期望 `201` | `201` | ☐ |
| 6 | 登录 | 同上，期望 `200` | `200` | ☐ |
| 7 | 会话有效 | `curl -s -o /tmp/me.json -w '%{http_code}' -b "$JAR" "$BASE/api/v1/auth/me"` | `200`，body 含 `"username":"20269001"` | ☐ |
| 8 | 登出后会话失效 | 登出后再请求 `/auth/me` | `401` | ☐ |

<details>
<summary>注册 / 登录的完整 curl（含 CSRF 双提交）</summary>

```bash
XSRF=$(awk '/XSRF-TOKEN/{print $7}' "$JAR" | tail -1)
curl -s -o /tmp/reg.json -w 'register -> %{http_code}\n' -c "$JAR" -b "$JAR" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $XSRF" \
  -d '{"username":"20269001","password":"Campus2026","displayName":"部署验收","email":"smoke@campus.edu"}' \
  "$BASE/api/v1/auth/register"

curl -sS -c "$JAR" -b "$JAR" "$BASE/api/v1/auth/csrf" -o /dev/null
XSRF=$(awk '/XSRF-TOKEN/{print $7}' "$JAR" | tail -1)
curl -s -o /dev/null -w 'login -> %{http_code}\n' -c "$JAR" -b "$JAR" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $XSRF" \
  -d '{"username":"20269001","password":"Campus2026"}' "$BASE/api/v1/auth/login"
```

</details>

> **第 7 条是最容易失败的一条**，失败基本都是 `COOKIE_SECURE` 与访问方式不匹配（纯 HTTP 下发了 `Secure` Cookie，
> 浏览器会丢弃、`curl` 不会——所以 `curl` 过了不代表浏览器能登录）。真机验收请再用浏览器点一次登录。

## 4. 监控（可选，但 v1.0.0 建议勾上）

```bash
cd deploy && docker compose -f docker-compose.prod.yml --env-file .env --profile observability up -d
```

| # | 检查 | 期望 | 结果 |
| --: | :-- | :-- | :--: |
| 4.1 | 端口只绑本机 | `ss -lntp` 显示 `127.0.0.1:9090`、`127.0.0.1:3000` | ☐ |
| 4.2 | 面板自动加载 | Grafana → **CampusOverflow** 文件夹 → 「CampusOverflow 运行概览」无需手工导入 | ☐ |
| 4.3 | 四类指标有数据 | 发件箱积压、死信、对账不一致、接口延迟均有曲线（新环境其余为 0 属正常） | ☐ |

## 5. 回滚演练

```bash
bash deploy/deploy.sh rollback
```

| # | 检查 | 期望 | 结果 |
| --: | :-- | :-- | :--: |
| 5.1 | 有可回滚镜像 | 不停在 `rollback 标签不存在` | ☐ |
| 5.2 | 回滚后健康 | 输出 `已回滚并恢复健康 ✔` | ☐ |
| 5.3 | 数据未丢 | 回滚前注册的账号仍能登录 | ☐ |

## 6. 记录

| 日期 | 环境 | 执行人 | 结果 | 证据 |
| :-- | :-- | :-- | :-- | :-- |
| 2026-11-26 | GitHub Actions `ubuntu-latest` | CI | 通过 | run `45727743269`（[deploy-smoke.md](deploy-smoke.md)） |
| 2026-11-26 | Windows 11 + Docker Desktop 29.9.3 | 见 PR #128 | 通过 | [deploy-smoke.md](deploy-smoke.md) 的「本机实测」 |
| 待填 | 干净 Linux 服务器（v1.0.0 tag） | 待填 | 待跑 | — |

## 7. 失败排查

| 现象 | 大概率原因 | 处理 |
| :-- | :-- | :-- |
| 后端容器反复重启，日志 `Unable to access jarfile` | 镜像 `COPY` 的 jar 名与 `finalName` 不一致（#124 已修，旧镜像仍会复现） | 重新 `docker compose build backend` |
| `POST /auth/login` 返回 200，紧接着 `/auth/me` 401 | `COOKIE_SECURE` 与访问协议不匹配 | 纯 HTTP 设 `false` 后重建后端 |
| `error getting credentials ... docker-credential-desktop not found` | Docker 凭据助手不在 PATH | 把 `.../Docker/resources/bin` 加入 PATH |
| 拉镜像超时 `dial tcp 185.45.7.185:443` | 镜像仓库域名被解析到错误 IP | 配 `registry-mirrors` 加速源 |
| `deploy.sh` 卡在「等待后端就绪」 | 数据库未起来 / `CO_FIELD_KEY` 非法 / Flyway 迁移失败 | `docker logs co-backend-prod --tail 100` |
| 中文搜索无结果 | MySQL 未按 compose 参数启动（`ngram_token_size=2`） | 用 `deploy/docker-compose.prod.yml` 起的库，别换成自建库 |
