# 技术设计文档

## 1. 系统概述

OmniMetric 是一个轻量级的指标配置后台服务，面向业务人员提供可配置的指标定义与查询能力。业务人员可以通过后台页面配置常用指标，而无需每次找研发单独编写 SQL 或开发接口。

当前版本支持「视频素材经营分析」场景，围绕素材数据进行指标配置、查询和异步任务执行。

---

## 2. 技术选型

| 组件 | 选择 | 说明 |
|------|------|------|
| 后端语言 | Java 17 | 成熟的生态，强类型安全，适合企业级应用 |
| 框架 | Spring Boot 3.2 | 组件化开发，内置 JPA/验证/异步支持 |
| 数据库 | H2 (开发) / MySQL 8.0 (生产) | H2 MySQL兼容模式，无缝切换 |
| ORM | Spring Data JPA + Hibernate | 简化数据访问，自动 DDL |
| 任务队列 | Spring @Async + ThreadPoolTaskExecutor | 零外部依赖，适合单机轻量场景 |
| API 风格 | RESTful JSON | 简单通用，工具链完善 |
| 构建工具 | Maven | 标准 Java 项目构建 |

### 为什么选择本地任务队列而非消息队列

- **选择理由**: 当前作业场景为单机部署、任务量较小（~QPS < 10），使用 Spring @Async 轻量异步执行完全满足需求，无需引入 RabbitMQ/Kafka 等外部中间件，降低部署运维成本。
- **局限性**: 进程重启后内存中的排队任务丢失；不支持分布式部署；没有死信队列、延时队列等高级特性。
- **生产建议**: 如果任务量增长或需要分布式部署，建议替换为 Redis Queue（轻量）或 RabbitMQ（可靠）。

---

## 3. 数据库设计

### 3.1 表结构

#### asset（素材明细数据表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| asset_id | VARCHAR(64) | UNIQUE, NOT NULL | 素材业务ID |
| title | VARCHAR(255) | | 素材标题 |
| uploader | VARCHAR(100) | NOT NULL | 上传人 |
| uploaded_at | DATETIME | NOT NULL | 上传时间 |
| file_size_bytes | BIGINT | NOT NULL | 文件大小（字节） |
| status | VARCHAR(20) | NOT NULL | 审核状态 |
| tags | VARCHAR(500) | | 标签 |
| city | VARCHAR(100) | | 城市 |
| platform | VARCHAR(100) | | 投放平台 |
| duration_seconds | INT | | 视频时长（秒） |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

**索引**:
- `asset_id` UNIQUE KEY — 业务查询
- `status`, `uploader`, `city`, `platform` — 查询筛选和分组
- `uploaded_at` — 时间范围筛选

#### metric_config（指标配置表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 指标名称 |
| description | VARCHAR(500) | | 指标描述 |
| source_table | VARCHAR(100) | NOT NULL | 来源数据表 |
| aggregate_field | VARCHAR(100) | NOT NULL | 统计字段 |
| aggregate_type | VARCHAR(20) | NOT NULL | 聚合方式 |
| group_by_field | VARCHAR(100) | | 分组维度 |
| filter_conditions | VARCHAR(500) | | 固定筛选条件 |
| sort_by | VARCHAR(100) | | 排序字段 |
| sort_order | VARCHAR(10) | | 排序方向 |
| enabled | TINYINT(1) | NOT NULL | 启用状态 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

#### query_task（查询任务表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| metric_config_id | BIGINT | NOT NULL | 关联指标配置ID |
| status | VARCHAR(20) | NOT NULL | 任务状态 |
| result_data | TEXT | | 查询结果JSON |
| error_message | VARCHAR(1000) | | 错误信息 |
| started_at | DATETIME | | 开始时间 |
| finished_at | DATETIME | | 结束时间 |
| retry_count | INT | NOT NULL | 已重试次数 |
| max_retry_count | INT | NOT NULL | 最大重试次数 |
| created_at | DATETIME | | 创建时间 |

### 3.2 表关系

- **metric_config** 与 **asset**: 一对多关系。指标配置引用 asset 表作为数据源。
- **query_task** 与 **metric_config**: 多对一关系。一个指标配置可以对应多个查询任务。

### 3.3 设计要点

1. **字段白名单校验**: 所有配置中的字段名（统计字段、分组维度、排序字段）都经过白名单校验，防止非法字段和 SQL 注入。
2. **索引策略**: 筛选和分组常用的字段（status, uploader, city, platform）都建有索引，确保聚合查询性能。
3. **扩展性**: 如果未来增加新的维度或指标，只需：
   - 在 asset 表中增加新字段（ALTER TABLE）
   - 在允许字段白名单中增加新字段名（代码配置）
   - 通过页面新增指标配置即可，无需修改查询逻辑

---

## 4. 指标配置模型

### 4.1 配置结构

指标配置由以下核心要素组成：
- **数据源**（当前仅 asset 表）
- **统计字段** + **聚合方式** → 决定聚合表达式
- **分组维度** → 可选的 GROUP BY
- **筛选条件** → 可选的 WHERE
- **排序规则** → 可选的 ORDER BY

### 4.2 查询生成引擎

查询生成引擎 (`MetricQueryService`) 的工作流程：

```
MetricConfig → 白名单校验 → 动态 SQL 构建 → EntityManager 执行 → 结果封装
```

核心代码示意：
```java
SELECT {AGGREGATE(aggregate_field)} AS result
    [, group_by_field AS group_value]
FROM source_table
[WHERE filter_conditions [AND additional_filter]]
[GROUP BY group_by_field]
[ORDER BY sort_by sort_order]
```

### 4.3 支持与不支持场景

**支持的场景**:
- 单表聚合查询（COUNT/SUM/AVG/MAX/MIN）
- 按受控维度分组统计（status/uploader/city/platform）
- 固定筛选条件 + 查询时附加筛选条件
- 结果排序
- 启用/停用指标

**不支持场景**（设计取舍）:
- 多表 JOIN 查询
- 任意 SQL（仅支持受控字段和受控聚合）
- 复杂派生指标（如 "通过率 = 通过数 / 总数"）
- 指标血缘和影响分析
- 完整的指标审批工作流
- 指标分层和维度建模

---

## 5. 异步任务设计

### 5.1 任务生命周期

```
pending → running → success
                   → failed → (重试) → pending → ...
```

### 5.2 重试策略

- 任务失败后自动重试，最多 3 次（可配置）
- 每次重试直接重新执行（简单重试，无退避策略）
- 3 次重试后标记为最终失败
- 提供手动重试 API

### 5.3 实现方式

使用 Spring `@Async` + `ThreadPoolTaskExecutor`：

```java
@Async("taskExecutor")
public void executeTaskAsync(Long taskId) { ... }
```

**局限性**（见第2节说明）：
- 进程重启丢失排队中的任务
- 不支持分布式
- 没有延时队列

---

## 6. 安全设计

### 6.1 SQL 注入防护

- 所有查询字段通过白名单校验，不允许任意 SQL 片段
- 筛选条件中的字段名做基本检查
- 不使用字符串拼接用户输入作为 SQL

### 6.2 输入校验

- 使用 Spring Validation (`@Valid`) 校验请求参数
- 聚合类型、分组字段等使用枚举类型校验

---

## 7. 关键取舍

| 决策 | 选择 | 替代方案 | 原因 |
|------|------|----------|------|
| ORM | Spring Data JPA | MyBatis, JDBC Template | 快速开发，自动 DDL |
| 任务队列 | @Async + ThreadPool | RabbitMQ, Kafka | 零依赖，快速启动 |
| 动态查询 | Native SQL 拼接 | Criteria API, QueryDSL | 灵活，代码简洁 |
| 开发数据库 | H2 MySQL模式 | 直接使用 MySQL | 无需安装，即时运行 |
| 数据生成 | Java CommandLineRunner | SQL 种子脚本 | 随机数据更丰富 |

---

## 7. 指标复用设计

### 7.1 设计目标

解决两个层面的复用问题：
1. **结果缓存**: 同一指标短时间内的重复查询直接返回缓存结果，避免重复计算
2. **分组合并计算**: 不同指标如果共享相同的分组口径（sourceTable + groupByField + filterConditions），合并为一次 SQL 执行

### 7.2 实现方案

#### 7.2.1 groupKey — 分组口径标识

在 `MetricConfig` 中增加 `group_key` 字段，自动计算:

```java
// 输入: sourceTable + groupByField + filterConditions
// 输出: SHA-256 短哈希
computeGroupKey("asset", "status", null)  → "gk_87ec2dd1e1ef"
```

相同 `groupKey` 的指标共享相同的分组口径，查询引擎将它们合并为一次 SQL。

#### 7.2.2 查询执行流程

```
executeQuery(metricConfigId)
  │
  ├─ 查找同 groupKey 的其他指标
  │     │
  │     ├─ 有同组指标 ──► 合并 SQL 执行
  │     │                   SELECT status,
  │     │                          COUNT(asset_id) AS m_0,
  │     │                          SUM(duration_seconds) AS m_1
  │     │                   FROM asset GROUP BY status
  │     │                   │
  │     │                   └─ 拆分结果 → 返回各指标结果
  │     │
  │     └─ 无同组指标 ──► 单指标 SQL 执行 → 返回
  │
```

### 7.3 复用示例

### 7.3 复用示例

预置的 4 个指标中，指标1和指标4共享相同的 `groupKey`：

| 指标 | groupByField | aggregateType | 说明 |
|------|-------------|---------------|------|
| 指标1 | status | COUNT(asset_id) | 按状态统计数量 |
| 指标4 | status | SUM(duration_seconds) | 按状态统计总时长（复用指标1的分组） |

查询引擎执行时，两个指标合并为一条 SQL：

```sql
SELECT status,
       COUNT(asset_id) AS m_0,
       SUM(duration_seconds) AS m_1
FROM asset
GROUP BY status;
```

一次全表扫描即计算出两个指标的结果。

### 7.4 支持与不支持的复用场景

**支持的场景**:
- 同源表、同分组口径的不同聚合指标合并计算

**不支持的场景**（设计取舍）:
- 跨不同数据源的指标复用
- 带不同筛选条件的指标合并（仅支持完全相同筛选条件的合并）
- 热数据预计算（仅在查询时按需合并，不做预聚合）

---

## 8. AI 工具使用说明

### 8.1 AI 辅助内容

| 类别 | 具体内容 | AI 参与程度 |
|------|----------|-------------|
| 代码生成 | 项目骨架、实体类、Repository、Service、Controller 等全部 Java 源码 | 全部由 AI 生成 |
| 配置 | application.yml, application-mysql.yml, pom.xml | AI 生成后人工调整 |
| SQL 脚本 | init.sql 建表语句和预置数据 | AI 生成 |
| 文档 | API 文档、技术设计文档、README | AI 生成核心内容 |

### 8.2 验证方式

所有 AI 生成的内容经过以下验证：

1. **代码审查**: 逐文件检查代码逻辑正确性，特别是：
   - 指标查询引擎的 SQL 构建逻辑是否存在注入风险
   - 异步任务的状态流转是否正确
   - 异常处理是否覆盖主要异常场景
2. **编译验证**: 项目完整编译无报错（`mvn clean compile`）
3. **运行验证**: 服务启动后通过 curl 验证所有 API 端点正常响应
4. **数据验证**: 确认初始化生成的 35 条素材数据和 4 个预置指标配置正确

## 9. 未来改进

如果有更多时间，以下方向值得投入：
1. **MySQL 部署**: 切换到 MySQL 并验证查询性能
2. **指标缓存**: 对高频查询的指标结果进行缓存
3. **导出功能**: 支持查询结果导出为 CSV/Excel
4. **指标看板**: 组合多个指标形成数据看板
5. **完整的消息队列**: 替换为 RabbitMQ 以支持分布式和可靠性
6. **单元测试**: 覆盖核心查询引擎和各种边界场景
7. **权限控制**: 指标配置的增删改查权限管理
8. **OpenAPI 文档**: 集成 SpringDoc 自动生成 Swagger 文档
