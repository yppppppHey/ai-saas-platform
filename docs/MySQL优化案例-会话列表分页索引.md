# MySQL 优化案例：会话列表分页「index_merge + filesort」→ 联合索引

> 适用场景：秋招简历中的「MySQL 索引优化」项目经历 / 面试话术素材。
> 结论一句话：**把 6 个单列索引（含低基数列）替换为 1 个联合索引，会话列表分页查询从 ~500ms 降到 ~2ms（约 230 倍），并消除 filesort、扫描行数从 2384 降到 20。**

---

## 1. 背景与问题

聊天服务 `chat_conversation`（会话表）的会话列表接口 `ChatConversationMapper.xml#selectConversationPage` 是最高频的只读接口之一（每次进入对话页、下拉加载都会触发）。其真实 SQL 形态为：

```sql
SELECT id, user_id, title, model, provider, is_pinned, is_archived,
       message_count, token_usage, last_message_at, last_message_preview, status
FROM chat_conversation
WHERE is_deleted = 0            -- 逻辑删除过滤（几乎恒为真，~95% 行）
  AND user_id = ?               -- 当前登录用户
ORDER BY is_pinned DESC, last_message_at DESC   -- 置顶优先，再按最近消息时间
LIMIT 20;
```

原始表结构（来自 `chat_init.sql` / `docker/mysql/init/02-chat.sql`）建了 **6 个单列索引**：

```sql
KEY idx_user_id (user_id),
KEY idx_is_pinned (is_pinned),
KEY idx_is_archived (is_archived),
KEY idx_last_message_at (last_message_at),
KEY idx_status (status),
KEY idx_is_deleted (is_deleted)
```

在 100 万行 / 400 用户（每用户约 2500 条会话）的数据量下，该接口 P95 延迟高达 **~500ms**，列表页明显卡顿。

## 2. 根因分析（EXPLAIN ANALYZE）

```text
-> Limit: 20 row(s)  (actual time=796..796 rows=20)
    -> Sort: is_pinned DESC, last_message_at DESC, limit input to 20 row(s) per chunk
        -> Filter: ((user_id = 42) and (is_deleted = 0))
            -> Intersect rows sorted by row ID                      ← index_merge！
                -> Index range scan using idx_user_id (user_id = 42)   rows=2519
                -> Index range scan using idx_is_deleted (is_deleted = 0) rows=949306
```

两个致命问题：

1. **index_merge（intersect）陷阱**：`is_deleted` 只有 0/1 两个取值、区分度极低（~95% 行都是 0）。单列索引 `idx_is_deleted` 几乎没有选择性，优化器却仍把 `idx_user_id` 与 `idx_is_deleted` 做交集回表，白白多扫近百万行的索引条目。
2. **Using filesort**：排序键 `(is_pinned, last_message_at)` 没有被任何索引覆盖，MySQL 必须把该用户约 2384 行全部读出来做内存/磁盘排序，再取前 20 条。

## 3. 优化方案

删除 6 个单列索引，改为 **1 个联合索引**，把「等值过滤列」放在前、「排序列」放在后，且排序列方向与 `ORDER BY` 完全一致（利用 MySQL 8 的降序索引，避免反向扫描）：

```sql
KEY idx_user_del_pinned_lm (user_id, is_deleted, is_pinned DESC, last_message_at DESC)
```

设计要点：

- `user_id`、`is_deleted` 是等值条件 → 放索引前导列，直接 `ref` 定位到该用户未删除的会话区间；
- `is_pinned DESC, last_message_at DESC` 与 `ORDER BY` 完全一致 → **索引本身有序，无需 filesort**，`LIMIT 20` 只需沿索引顺序读 20 行即止；
- 统计接口的几个 `COUNT`（`user_id+is_deleted`、`+is_pinned`、`+is_archived`）可复用该索引的前导列，**无回归**。

## 4. 验证结果（同一份 100 万行数据）

**优化后执行计划：**

```text
-> Limit: 20 row(s)  (actual time=2.3..2.3 rows=20)
    -> Index lookup using idx_user_del_pinned_lm (user_id=42, is_deleted=0)   ← 无 filesort
```

**压测对比（200 次循环平均，热点用户 = 42）：**

| 指标 | 优化前（6 单列索引） | 优化后（1 联合索引） | 提升 |
|---|---|---|---|
| EXPLAIN ANALYZE 实际耗时 | ~796 ms | ~2.3 ms | **~345×** |
| 平均延迟 `LIMIT 20` | 501.48 ms | 2.15 ms | **~233×** |
| 平均延迟 `LIMIT 2000,20`（深翻页） | 482.46 ms | 2.04 ms | **~236×** |
| 扫描行数（EXPLAIN rows） | 2384 | 20 | **~119×** |
| 执行计划 | `index_merge` + `Using filesort` | `ref`，无 filesort | — |
| 索引数量 | 6 | 1 | 写入放大更小 |

统计类查询（`COUNT ... WHERE user_id=? AND is_deleted=0 [AND is_pinned=1]`）仍走联合索引前导列，计划 `type=ref, Using index`，无性能回归。

## 5. 可迁移的经验（面试可讲）

1. **低基数列（性别、状态、逻辑删除）不要单独建索引**，尤其是和另一个高基数列同时出现在 `WHERE` 时，极易诱发 `index_merge` 反而更慢。
2. **索引设计先看 `ORDER BY` 和 `LIMIT`**：高频分页查询要把「等值列 + 排序列」设计进同一个联合索引，让 `LIMIT` 下推到索引、彻底消灭 filesort。
3. **用 `EXPLAIN ANALYZE` 看真实耗时**，不要只信 `EXPLAIN` 的预估 `rows`；索引优化必须「造数据 + 压测」闭环验证。
4. **`DESC` 排序用降序索引**：`(a, b DESC)` 与 `ORDER BY a, b DESC` 完全匹配时，MySQL 8 走正序索引扫描，不会 backward index scan。
5. **索引不是越多越好**：6 个冗余单列索引既拖慢写入（每次 INSERT/UPDATE 维护 6 棵 B+ 树），又给优化器更多误判空间；合并为 1 个覆盖索引一举两得。

## 6. 涉及改动

| 文件 | 改动 |
|---|---|
| `ai-saas-chat-service/src/main/resources/db/chat_init.sql` | 6 单列索引 → `idx_user_del_pinned_lm (user_id, is_deleted, is_pinned DESC, last_message_at DESC)` |
| `docker/mysql/init/02-chat.sql` | 同上 |
| `sql/init.sql` | **未改**：该文件中的 `chat_conversation` 是陈旧/分叉的 schema（缺 `is_deleted`/`is_archived`/`last_message_preview` 列，与实际实体不一致），不属于运行中的服务，已单独记录待清理 |

> 复现脚本与原始输出见仓库 `E:/tmp_mysql_bench/08_real_query.sql` 与 `08_real_query.out`（本地临时实验目录，未提交）。
