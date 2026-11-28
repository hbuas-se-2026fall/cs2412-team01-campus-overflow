# 生产部署指南（deploy/）

`docker-compose.yml` 是**开发/演示环境**（随课程初始代码提供）；本目录新增的是**生产部署**所需文件，两者互不影响。

## 文件清单

| 文件 | 用途 |
| --- | --- |
| `docker-compose.prod.yml` | 生产编排：不暴露 3306/6379、restart 策略、日志轮转、内存限制、监控端口只绑 127.0.0.1 |
| `deploy.sh` | 一键部署 / 回滚 / 停止 / 看日志，自带健康检查与失败排查输出 |
| `.env.example` | 环境变量模板（真实密码放 `deploy/.env`，不入库） |

## 服务器首次部署

```bash
# 1) 拉代码并准备配置
git clone https://github.com/jingmo2233/campus-overflow-team-01.git && cd campus-overflow-team-01
cp deploy/.env.example deploy/.env
# 修改 .env 中全部 change_me_*，并生成加密密钥：
#   openssl rand -base64 32   → 填入 CO_FIELD_KEY

# 2) 一键部署（构建 + 启动 + 健康检查）
bash deploy/deploy.sh
```

部署成功后：

- 前端：`http://<服务器IP>:<WEB_PORT>`（默认 80）
- 接口文档：`http://<服务器IP>:<WEB_PORT>/swagger-ui.html`
- 健康检查：`http://<服务器IP>:<WEB_PORT>/actuator/health`

## 日常运维

```bash
bash deploy/deploy.sh logs      # 跟踪后端日志
bash deploy/deploy.sh rollback  # 回滚到上一次成功部署
bash deploy/deploy.sh down      # 停止（数据卷保留）

# 更新版本：git pull 后重新执行部署脚本
bash deploy/deploy.sh
```

## 开启监控（可选）

```bash
cd deploy && docker compose -f docker-compose.prod.yml --env-file .env --profile observability up -d
```

Prometheus（9090）与 Grafana（3000）只绑定服务器本机，本机访问需 SSH 隧道：

```bash
ssh -L 9090:127.0.0.1:9090 -L 3000:127.0.0.1:3000 <user>@<服务器IP>
```

### Grafana 面板（PB-44）

数据源与面板都**代码化**在 `grafana/` 下，容器重建或换机器后开箱即用，不需要手工点：

| 路径 | 用途 |
| --- | --- |
| `grafana/provisioning/datasources/prometheus.yml` | 自动注册 Prometheus 数据源（uid 固定为 `prom`，免得重装后仪表盘引用失效） |
| `grafana/provisioning/dashboards/dashboards.yml` | 扫描 `/var/lib/grafana/dashboards` 自动加载 JSON |
| `grafana/dashboards/campus-overflow.json` | 「CampusOverflow 运行概览」仪表盘 |

打开 Grafana（`admin` / `.env` 里的 `GRAFANA_PASSWORD`）→ 左侧 **CampusOverflow** 文件夹 → 「CampusOverflow 运行概览」。

面板内容与告警口径：

| 面板 | 指标 | 判读 |
| --- | --- | --- |
| 发件箱积压 | `co_outbox_backlog` | 持续 > 100 超过 5 分钟 → 事件链路被堵 |
| 死信事件 | `co_outbox_dead_letters` | **> 0 必须人工介入**（重试超 10 次） |
| 声誉对账不一致 | `co_reputation_mismatches` | 应恒为 0（FF-8） |
| 接口平均延迟 | `rate(http_server_requests_seconds_sum) / rate(..._count)` | > 300 ms 关注，> 1 s 报警（QS-01 要求详情页 P95 ≤ 300ms） |
| 事件投递速率 | `rate(co_outbox_published_total[5m])` 等 | 失败率上升多为消费端事务/幂等问题 |
| JVM 堆与 CPU | `jvm_memory_used_bytes`、`process_cpu_usage` | 排查 OOM 与跑满 |

> 目前暴露的是 `_count` / `_sum` / `_max`，所以「接口延迟」用平均值口径。
> 若要真 P95，需在 `application.yml` 打开
> `management.metrics.distribution.percentiles-histogram.http.server.requests=true`，
> 再把面板表达式换成 `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))`。

## 安全约定

- 3306（MySQL）、6379（Redis）**不向宿主机暴露**，仅容器网络内互通
- 所有密码/密钥集中在 `deploy/.env`（git 已忽略），`grep change_me` 未清空时脚本拒绝启动
- `CO_FIELD_KEY` 首次生成后不可更换（否则已加密字段无法解密），请离线备份
- 走 HTTPS 发布时把 `.env` 中 `COOKIE_SECURE` 改为 `true`
- 生产 profile 为 `prod`（不含 demo 演示数据）
