# API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **统一响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-06-06T12:00:00"
}
```

- **分页参数**: `page` (从0开始), `size` (默认10)
- **错误码**:
  - `200`: 成功
  - `201`: 创建成功
  - `400`: 请求参数错误
  - `404`: 资源不存在
  - `500`: 服务器内部错误

---

## 1. 指标配置管理

### 1.1 新增指标配置

```
POST /api/metric-configs
```

**请求体**:
```json
{
  "name": "按审核状态统计素材数量",
  "description": "统计各审核状态下的素材数量",
  "sourceTable": "asset",
  "aggregateField": "asset_id",
  "aggregateType": "COUNT",
  "groupByField": "status",
  "filterConditions": "status = 'approved'",
  "sortBy": "result",
  "sortOrder": "desc",
  "enabled": true
}
```

**响应** (201):
```json
{
  "code": 201,
  "message": "created",
  "data": {
    "id": 1,
    "name": "按审核状态统计素材数量",
    "description": "统计各审核状态下的素材数量",
    "sourceTable": "asset",
    "aggregateField": "asset_id",
    "aggregateType": "COUNT",
    "groupByField": "status",
    "filterConditions": "status = 'approved'",
    "sortBy": "result",
    "sortOrder": "desc",
    "enabled": true,
    "createdAt": "2026-06-06T12:00:00",
    "updatedAt": "2026-06-06T12:00:00"
  }
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 说明 | 允许值 |
|------|------|------|------|--------|
| name | string | 是 | 指标名称 | |
| description | string | 否 | 指标描述 | |
| sourceTable | string | 是 | 来源数据表 | 当前仅支持 `asset` |
| aggregateField | string | 是 | 统计字段 | `asset_id`, `file_size_bytes`, `duration_seconds` |
| aggregateType | string | 是 | 聚合方式 | `COUNT`, `SUM`, `AVG`, `MAX`, `MIN` |
| groupByField | string | 否 | 分组维度 | `status`, `uploader`, `city`, `platform` |
| filterConditions | string | 否 | 固定筛选条件 | 如 `status = 'approved'` |
| sortBy | string | 否 | 排序字段 | `result`, `group_value` |
| sortOrder | string | 否 | 排序方向 | `asc`, `desc` |
| enabled | boolean | 否 | 启用状态 | 默认 `true` |

---

### 1.2 编辑指标配置

```
PUT /api/metric-configs/{id}
```

**请求体**: (部分更新，只传需要修改的字段)
```json
{
  "name": "新的指标名称",
  "enabled": false
}
```

**响应** (200): 同新增返回结构

---

### 1.3 获取指标配置详情

```
GET /api/metric-configs/{id}
```

**响应** (200): 同新增返回结构

---

### 1.4 分页查询指标配置列表

```
GET /api/metric-configs?page=0&size=10&sortBy=createdAt&sortOrder=desc
```

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "按审核状态统计素材数量",
        "sourceTable": "asset",
        "aggregateField": "asset_id",
        "aggregateType": "COUNT",
        "groupByField": "status",
        "enabled": true,
        "createdAt": "2026-06-06T12:00:00",
        "updatedAt": "2026-06-06T12:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 1.5 启用/停用指标配置

```
PATCH /api/metric-configs/{id}/toggle
```

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "enabled": false,
    ...
  }
}
```

---

## 2. 指标查询

### 2.1 同步执行指标查询

```
POST /api/queries/execute
```

**请求体**:
```json
{
  "metricConfigId": 1,
  "additionalFilter": "city = '上海'"
}
```

**响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "metricConfigId": 1,
    "metricConfigName": "按审核状态统计素材数量",
    "aggregateType": "COUNT",
    "groupByField": "status",
    "filterConditions": "status = 'approved'",
    "rows": [
      {
        "group_value": "approved",
        "result": 15
      },
      {
        "group_value": "rejected",
        "result": 8
      },
      {
        "group_value": "pending",
        "result": 7
      }
    ],
    "totalRows": 3
  }
}
```

---

## 3. 异步任务

### 3.1 创建异步查询任务

```
POST /api/queries/task
```

**请求体**:
```json
{
  "metricConfigId": 1
}
```

**响应** (200):
```json
{
  "code": 200,
  "message": "任务创建成功，可通过 GET /api/queries/task/1 查询状态",
  "data": {
    "id": 1,
    "metricConfigId": 1,
    "status": "pending",
    "retryCount": 0,
    "createdAt": "2026-06-06T12:00:00"
  }
}
```

---

### 3.2 查询任务状态

```
GET /api/queries/task/{taskId}
```

**响应** (200) - 进行中:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "metricConfigId": 1,
    "status": "running",
    "startedAt": "2026-06-06T12:00:05"
  }
}
```

**响应** (200) - 已完成:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "metricConfigId": 1,
    "status": "success",
    "resultData": "{\"metricConfigId\":1,\"rows\":[...]}",
    "startedAt": "2026-06-06T12:00:05",
    "finishedAt": "2026-06-06T12:00:06"
  }
}
```

**响应** (200) - 失败:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "metricConfigId": 1,
    "status": "failed",
    "errorMessage": "xxx",
    "retryCount": 3
  }
}
```

---

### 3.3 重试失败任务

```
POST /api/queries/task/{taskId}/retry
```

**响应** (200):
```json
{
  "code": 200,
  "message": "任务已重新提交",
  "data": {
    "id": 1,
    "metricConfigId": 1,
    "status": "pending",
    "retryCount": 0
  }
}
```

---

## 4. 指标复用说明

### 预置指标的分组关系

当前预置 4 个指标：

| ID | 指标名称 | groupByField | groupKey | 说明 |
|----|----------|-------------|----------|------|
| 1 | 按审核状态统计素材数量 | status | `gk_87ec2dd1e1ef` | ✅ 与指标4共享 |
| 2 | 已通过素材的各上传人平均文件大小 | uploader | `gk_230b26fca1ea` | 独立 |
| 3 | 各城市素材总时长 | city | `gk_257be1bf21a8` | 独立 |
| 4 | 各审核状态素材总时长 | status | `gk_87ec2dd1e1ef` | ✅ 与指标1共享 |

指标1和指标4共享分组口径，查询时自动合并为一次 SQL 执行。

### 响应中的 groupKey 字段

指标配置响应中新增 `groupKey` 字段：

```json
{
  "id": 1,
  "name": "按审核状态统计素材数量",
  "groupByField": "status",
  "groupKey": "gk_87ec2dd1e1ef",
  "aggregateType": "COUNT",
  "enabled": true
}
```

相同 `groupKey` 的指标会在查询时合并计算。`groupKey` 由系统根据 `sourceTable + groupByField + filterConditions` 自动计算得出。
