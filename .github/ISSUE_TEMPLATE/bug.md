## 复现步骤
1. 以张博A（`student_a`）登录，发布问题「红黑树旋转的时间复杂度是多少？」，标签 `java`、`算法`
2. 以宾艺涵B（`student_b`）登录，在详情页提交回答
3. 切回 A，在详情页点击「采纳」该回答 → 返回 204，页面显示“已采纳”
4. 回到问题列表页刷新，再打开 B 的个人声誉页
**复现概率**：必现（5/5 次）

## 期望结果
1. 列表页该问题显示“已解决 / 已采纳”标记
2. 数秒内 B 的声誉 +15，流水中出现「回答被采纳 +15」
3. 通知中心出现「你的回答被采纳」

## 实际结果
1. 列表页仍显示“未解决”，约 60 s（列表缓存 TTL）后才变为已采纳
2. B 的声誉仍为 0，`rep_ledger` 无新增记录
3. 通知中心无新消息
- **HTTP 状态**：204（采纳接口本身成功，问题出在异步链路）
- **业务错误码**：无（前端拿不到错误码，只能看到数据不一致）
- 服务端日志可见 `UnsupportedOperationException: TODO(S2)`

## 日志与截图
```text
$ docker exec -it co-mysql mysql -ucampus -pcampus_dev_pwd campus_overflow -e \
    "SELECT event_id, event_type, published_at, attempts, left(last_error,120) FROM outbox_event ORDER BY id DESC LIMIT 3\G"
event_id: 8f2c41d0-...
event_type: AnswerAcceptedEvent
published_at: NULL
attempts: 10
last_error: java.lang.UnsupportedOperationException: TODO(S2) 见 Issue #42
