# OmniMetric 测试文档

## 概述

本文档包含 OmniMetric 指标配置后台服务的测试步骤、测试数据和预期结果说明。所有测试基于默认的 H2 内存数据库，应用启动时自动初始化 35 条素材数据和 3 个预置指标配置。

---

## 1. 测试环境

### 1.1 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| curl (测试用) | 7.x+ |
| 操作系统 | Linux / macOS / Windows |

### 1.2 启动服务

```bash
cd /root/workspace/omni-metric

# 启动服务（默认 H2 内存数据库）
mvn spring-boot:run

# 待控制台输出类似以下信息即启动成功：
# 2026-06-06T12:03:27.123  INFO  --- Started OmniMetricApplication in 5.23 seconds
```

> **注意**: 每次重启服务后，H2 内存数据库会自动重新初始化，数据恢复为默认的 35 条素材数据和 3 个预置指标配置。

---

## 2. 预置测试数据

### 2.1 预置指标配置

| ID | 指标名称 | 聚合方式 | 统计字段 | 分组维度 | 筛选条件 |
|----|----------|----------|----------|----------|----------|
| 1 | 按审核状态统计素材数量 | COUNT | asset_id | status | 无 |
| 2 | 已通过素材的各上传人平均文件大小 | AVG | file_size_bytes | uploader | status = 'approved' |
| 3 | 各城市素材总时长 | SUM | duration_seconds | city | 无 |

### 2.2 预置素材数据概览

启动时自动生成 35 条素材记录，覆盖以下维度：

- **上传人**: 张三、李四、王五、赵六、钱七、孙八（6 人）
- **审核状态**: approved / rejected / pending（3 种）
- **城市**: 上海、北京、广州、深圳、杭州、成都、武汉（7 个）
- **平台**: 抖音、快手、小红书、微信视频号（4 个）
- **文件大小**: 10MB ~ 510MB 随机
- **视频时长**: 15 ~ 315 秒随机

---

## 3. 测试用例

### 3.1 指标配置管理测试

#### TC-1.1 查询指标配置列表

**请求**:
```bash
curl http://localhost:8080/api/metric-configs
```

**预期结果**:
```json
{
  "code": 200,
  "data": {
    "content": [
      { "id": 3, "name": "各城市素材总时长", "enabled": true },
      { "id": 2, "name": "已通过素材的各上传人平均文件大小", "enabled": true },
      { "id": 1, "name": "按审核状态统计素材数量", "enabled": true }
    ],
    "totalElements": 3
  }
}
```

> **注意**: 由于未指定排序，默认按创建时间倒序。

**验证要点**:
- [ ] 返回 3 条预置指标配置
- [ ] 响应格式包含分页信息（content, page, size, totalElements, totalPages）

---

#### TC-1.2 分页查询指标配置

**请求**:
```bash
curl "http://localhost:8080/api/metric-configs?page=0&size=2&sortBy=id&sortOrder=asc"
```

**预期结果**: 每页 2 条，共 3 条数据，总页数 2 页。

**验证要点**:
- [ ] `content` 字段包含 2 条记录
- [ ] `totalElements` 为 3
- [ ] `totalPages` 为 2
- [ ] `first` 为 true，`last` 为 false（第一页）

---

#### TC-1.3 查询单个指标配置详情

**请求**:
```bash
curl http://localhost:8080/api/metric-configs/1
```

**预期结果**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "按审核状态统计素材数量",
    "aggregateField": "asset_id",
    "aggregateType": "COUNT",
    "groupByField": "status",
    "enabled": true
  }
}
```

**验证要点**:
- [ ] 返回 ID=1 的指标配置完整信息
- [ ] 包含 created_at 和 updated_at 时间戳

---

#### TC-1.4 查询不存在的指标配置

**请求**:
```bash
curl http://localhost:8080/api/metric-configs/999
```

**预期结果**:
```json
{
  "code": 404,
  "message": "指标配置 不存在，ID: 999",
  "timestamp": "..."
}
```

**验证要点**:
- [ ] 返回 404 错误码
- [ ] 错误信息中包含资源名称和 ID

---

#### TC-1.5 新增指标配置

**请求**:
```bash
curl -X POST http://localhost:8080/api/metric-configs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "各平台素材平均时长",
    "description": "统计各投放平台的素材平均视频时长",
    "sourceTable": "asset",
    "aggregateField": "duration_seconds",
    "aggregateType": "AVG",
    "groupByField": "platform",
    "sortBy": "result",
    "sortOrder": "desc"
  }'
```

**预期结果** (201 Created):
```json
{
  "code": 201,
  "message": "created",
  "data": {
    "id": 4,
    "name": "各平台素材平均时长",
    "aggregateType": "AVG",
    "groupByField": "platform",
    "enabled": true
  }
}
```

**验证要点**:
- [ ] 返回 201 状态码
- [ ] 返回的数据包含自动生成的 `id` 和 `createdAt`
- [ ] `enabled` 默认为 true

---

#### TC-1.6 新增指标配置 - 校验失败

**请求**（缺少必填字段）:
```bash
curl -X POST http://localhost:8080/api/metric-configs \
  -H "Content-Type: application/json" \
  -d '{"name": "不完整指标"}'
```

**预期结果**:
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "sourceTable": "来源数据表不能为空",
    "aggregateField": "统计字段不能为空",
    "aggregateType": "聚合方式不能为空"
  }
}
```

**验证要点**:
- [ ] 返回 400 错误码
- [ ] 错误信息中列出所有缺失的必填字段

---

#### TC-1.7 新增指标配置 - 非法字段

**请求**（使用不支持的字段名）:
```bash
curl -X POST http://localhost:8080/api/metric-configs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "危险查询",
    "sourceTable": "asset",
    "aggregateField": "password",
    "aggregateType": "COUNT"
  }'
```

**预期结果**:
```json
{
  "code": 400,
  "message": "不支持的统计字段: password，允许值: [asset_id, file_size_bytes, duration_seconds]"
}
```

**验证要点**:
- [ ] 非法字段名被白名单拦截
- [ ] 错误信息中列出所有允许值（防 SQL 注入）

---

#### TC-1.8 编辑指标配置

**请求**（部分更新）:
```bash
curl -X PUT http://localhost:8080/api/metric-configs/1 \
  -H "Content-Type: application/json" \
  -d '{"description": "更新后的指标描述", "sortOrder": "asc"}'
```

**预期结果**: 返回更新后的指标配置，description 和 sortOrder 字段已变更。

**验证要点**:
- [ ] 只更新了传入的字段，其他字段保持不变
- [ ] `updatedAt` 时间戳更新

---

#### TC-1.9 启用/停用指标配置

**停用指标**:
```bash
# 停用指标 ID=1
curl -s -X PATCH http://localhost:8080/api/metric-configs/1/toggle | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(f'enabled={d[\"enabled\"]}')"
```
预期输出: `enabled=False`

**验证停用后无法查询**:
```bash
curl -s -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1}' | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'code={d[\"code\"]}, message={d[\"message\"]}')"
```
预期输出: `code=400, message=指标配置已停用，ID: 1`

**重新启用**:
```bash
curl -s -X PATCH http://localhost:8080/api/metric-configs/1/toggle | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(f'enabled={d[\"enabled\"]}')"
```
预期输出: `enabled=True`

**验证重新启用后可查询**:
```bash
curl -s -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1}' | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'code={d[\"code\"]}, totalRows={d[\"data\"][\"totalRows\"]}')"
```
预期输出: `code=200, totalRows=3`

**验证要点**:
- [ ] 停用后查询返回 400 错误
- [ ] 重新启用后查询恢复正常

---

### 3.2 指标查询测试

#### TC-2.1 同步执行指标查询 - COUNT + GROUP BY

**请求**:
```bash
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1}'
```

**预期结果示例**:
```json
{
  "code": 200,
  "data": {
    "metricConfigId": 1,
    "metricConfigName": "按审核状态统计素材数量",
    "aggregateType": "COUNT",
    "groupByField": "status",
    "rows": [
      { "result": 16, "group_value": "pending" },
      { "result": 12, "group_value": "rejected" },
      { "result": 7,  "group_value": "approved" }
    ],
    "totalRows": 3
  }
}
```

**验证要点**:
- [ ] 返回 3 行数据（3 种审核状态）
- [ ] 各状态的素材数量之和为 35
- [ ] `result` 字段为整数（COUNT 结果）
- [ ] `group_value` 对应 status 字段的枚举值

---

#### TC-2.2 同步执行指标查询 - AVG + WHERE + GROUP BY

**请求**:
```bash
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 2}'
```

**预期结果示例**:
```json
{
  "code": 200,
  "data": {
    "metricConfigId": 2,
    "metricConfigName": "已通过素材的各上传人平均文件大小",
    "aggregateType": "AVG",
    "groupByField": "uploader",
    "filterConditions": "status = 'approved'",
    "rows": [
      { "result": 345331029.33..., "group_value": "张三" },
      { "result": 306184192.0,      "group_value": "钱七" },
      { "result": 269484032.0,      "group_value": "李四" }
    ],
    "totalRows": 3
  }
}
```

**验证要点**:
- [ ] 只返回 status=approved 的素材分组结果
- [ ] 上传人数量 ≤ 6（某些上传人可能没有已通过的素材）
- [ ] `result` 字段为浮点数（AVG 结果）

---

#### TC-2.3 同步执行指标查询 - SUM + GROUP BY

**请求**:
```bash
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 3}'
```

**预期结果示例**:
```json
{
  "code": 200,
  "data": {
    "metricConfigId": 3,
    "metricConfigName": "各城市素材总时长",
    "aggregateType": "SUM",
    "groupByField": "city",
    "rows": [
      { "result": 1226, "group_value": "广州" },
      { "result": 1119, "group_value": "成都" },
      { "result": 1094, "group_value": "深圳" },
      { "result": 652,  "group_value": "北京" },
      { "result": 488,  "group_value": "上海" },
      { "result": 354,  "group_value": "杭州" },
      { "result": 311,  "group_value": "武汉" }
    ],
    "totalRows": 7
  }
}
```

**验证要点**:
- [ ] 返回 7 行数据（7 个城市）
- [ ] 按 result 降序排列（配置中 sortBy=result, sortOrder=desc）
- [ ] `result` 字段为整数（SUM 结果，单位为秒）

---

#### TC-2.4 同步执行查询 - 带附加筛选条件

**请求**（额外筛选上传人=张三）:
```bash
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1, "additionalFilter": "uploader = '\''张三'\''"}'
```

**预期结果**: 只返回上传人为"张三"的素材在各审核状态下的分布。

**验证要点**:
- [ ] `totalRows` ≤ 3（可能只有部分状态有数据）
- [ ] 各 `group_value` 对应不同审核状态
- [ ] 不持久化该筛选条件（再次查询不带 additionalFilter 时恢复原始结果）

---

#### TC-2.5 通过新增配置实现新指标（零代码）

**意图**: 验证新增指标无需修改代码，仅需新增配置即可。

**步骤 1** — 新增指标配置（按城市统计素材数量）:
```bash
curl -X POST http://localhost:8080/api/metric-configs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "各城市素材数量",
    "description": "统计各城市的素材数量",
    "sourceTable": "asset",
    "aggregateField": "asset_id",
    "aggregateType": "COUNT",
    "groupByField": "city",
    "sortBy": "result",
    "sortOrder": "desc"
  }'
```

**步骤 2** — 执行新指标查询（ID 为 5，取上一步返回的实际 ID）:
```bash
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 5}'
```

**预期结果**: 返回各城市的素材数量，按数量降序排列。

**验证要点**:
- [ ] 无需重启服务，新增配置即时生效
- [ ] 查询结果符合预期（各城市素材数量之和 = 35）
- [ ] 整个过程中没有编写任何 Java 代码

---

### 3.3 异步任务测试

#### TC-3.1 创建异步查询任务

**请求**:
```bash
curl -X POST http://localhost:8080/api/queries/task \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 2}'
```

**预期结果**:
```json
{
  "code": 200,
  "message": "任务创建成功，可通过 GET /api/queries/task/1 查询状态",
  "data": {
    "id": 1,
    "metricConfigId": 2,
    "status": "success",
    "resultData": "{...}",
    "startedAt": "2026-06-06T12:03:47",
    "finishedAt": "2026-06-06T12:03:47",
    "retryCount": 0
  }
}
```

> **注意**: 由于任务是异步执行，响应中 status 可能为 pending、running 或 success（任务执行速度很快时）。

**验证要点**:
- [ ] 返回任务 ID
- [ ] `retryCount` 初始为 0
- [ ] `metricConfigId` 与请求一致

---

#### TC-3.2 查询任务状态

**请求**:
```bash
# taskId 替换为 TC-3.1 返回的实际 ID
curl http://localhost:8080/api/queries/task/1
```

**预期结果**（已完成）:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "status": "success",
    "resultData": "{\"metricConfigId\":2, ...}",
    "startedAt": "2026-06-06T12:03:47",
    "finishedAt": "2026-06-06T12:03:47",
    "retryCount": 0
  }
}
```

**验证要点**:
- [ ] 任务最终状态为 `success` 或 `failed`
- [ ] `resultData` 包含完整的查询结果 JSON
- [ ] `startedAt` 和 `finishedAt` 时间合理

---

#### TC-3.3 为停用的指标创建任务

**准备** — 先停用一个指标:
```bash
curl -s -X PATCH http://localhost:8080/api/metric-configs/1/toggle > /dev/null
```

**请求**:
```bash
curl -X POST http://localhost:8080/api/queries/task \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1}'
```

**预期结果**:
```json
{
  "code": 400,
  "message": "指标配置已停用，无法创建任务"
}
```

**清理**:
```bash
curl -s -X PATCH http://localhost:8080/api/metric-configs/1/toggle > /dev/null
```

**验证要点**:
- [ ] 已停用的指标不能创建任务
- [ ] 错误信息清晰说明原因

---

#### TC-3.4 重试失败的任务

**说明**: 由于数据集较小，查询通常不会失败。本测试验证重试 API 的可用性。可以通过查询不存在的指标配置 ID 构造失败场景。

**准备** — 先创建一个任务（对正常指标）:
```bash
curl -s -X POST http://localhost:8080/api/queries/task \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 3}'
```

**验证重试接口可用**（假设已有失败任务 ID=5）:
```bash
# 该接口仅对 status=failed 的任务有效
curl -X POST http://localhost:8080/api/queries/task/5/retry
```

**验证要点**:
- [ ] 对非失败状态任务调用 retry 返回 400 错误
- [ ] 对失败任务调用 retry 后 status 重置为 pending

---

### 3.4 异常与边界场景测试

#### TC-4.1 资源不存在 (404)

| 场景 | 请求 | 预期 |
|------|------|------|
| 查询不存在的指标配置 | `GET /api/metric-configs/999` | code=404 |
| 查询不存在的任务 | `GET /api/queries/task/999` | code=404 |
| 对不存在指标执行查询 | `POST /api/queries/execute {"metricConfigId": 999}` | code=404 |
| 对不存在指标创建任务 | `POST /api/queries/task {"metricConfigId": 999}` | code=404 |

**验证要点**:
- [ ] 所有 404 场景统一返回 `{code: 404, message: "...不存在..."}`
- [ ] HTTP 状态码为 404

---

#### TC-4.2 参数校验失败 (400)

| 场景 | 请求 | 预期 |
|------|------|------|
| 请求体为空 | `POST /api/queries/execute {}` | code=400, metricConfigId 不能为空 |
| 指标名称为空 | `POST /api/metric-configs {"aggregateField":"..."}` | code=400, 指标名称不能为空 |
| 非法聚合字段 | `POST /api/metric-configs {"aggregateField":"bad_field"}` | code=400, 不支持的统计字段 |

**验证要点**:
- [ ] 参数校验失败统一返回 `{code: 400, message: "参数校验失败", data: {...}}`
- [ ] 错误信息具体说明哪个字段出问题

---

#### TC-4.3 系统内部错误 (500)

**说明**: 正常情况下不应出现 500 错误。可通过构造极端场景测试（如数据库连接断开等基础设施故障），但不在本文档范围内。

**验证要点**:
- [ ] 所有已知异常场景有明确的 400/404 处理
- [ ] 未预期的异常返回 500，包含错误信息

---

## 4. 批处理测试脚本

为方便一次性执行核心测试，提供以下批处理脚本：

```bash
#!/bin/bash
# 保存为 test_all.sh

BASE_URL="http://localhost:8080"
PASS=0
FAIL=0

check() {
    local desc="$1"
    local actual="$2"
    local expected="$3"
    if echo "$actual" | grep -q "$expected"; then
        echo "[PASS] $desc"
        PASS=$((PASS+1))
    else
        echo "[FAIL] $desc"
        echo "  Expected: $expected"
        echo "  Actual:   $actual"
        FAIL=$((FAIL+1))
    fi
}

echo "=== OmniMetric 测试套件 ==="
echo ""

# TC-1.1 查询列表
echo "--- 3.1 指标配置管理 ---"
RESP=$(curl -s "$BASE_URL/api/metric-configs?page=0&size=10")
check "TC-1.1 查询指标列表" "$RESP" '"totalElements":3'

# TC-1.3 查询详情
RESP=$(curl -s "$BASE_URL/api/metric-configs/1")
check "TC-1.3 查询指标详情" "$RESP" '"name":"按审核状态统计素材数量"'

# TC-1.4 查询不存在
RESP=$(curl -s "$BASE_URL/api/metric-configs/999")
check "TC-1.4 查询不存在的指标" "$RESP" '"code":404'

# TC-1.5 新增指标
RESP=$(curl -s -X POST "$BASE_URL/api/metric-configs" \
  -H "Content-Type: application/json" \
  -d '{"name":"新指标","sourceTable":"asset","aggregateField":"asset_id","aggregateType":"COUNT"}')
check "TC-1.5 新增指标" "$RESP" '"code":201'

# TC-1.6 校验失败
RESP=$(curl -s -X POST "$BASE_URL/api/metric-configs" \
  -H "Content-Type: application/json" \
  -d '{"name":"test"}')
check "TC-1.6 参数校验失败" "$RESP" '"code":400'

# TC-1.9 停用/启用
curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null
RESP=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":1}')
check "TC-1.9 停用后无法查询" "$RESP" '"code":400'
curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null

echo ""
echo "--- 3.2 指标查询 ---"
# TC-2.1 COUNT查询
RESP=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":1}')
check "TC-2.1 COUNT+GROUP BY查询" "$RESP" '"code":200'
RCOUNT=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['totalRows'])")
check "TC-2.1 返回3种状态" "$RCOUNT" "3"

# TC-2.2 AVG+WHERE查询
RESP=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":2}')
check "TC-2.2 AVG+WHERE查询" "$RESP" '"code":200'

# TC-2.3 SUM查询
RESP=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":3}')
check "TC-2.3 SUM+GROUP BY查询" "$RESP" '"code":200'

echo ""
echo "--- 3.3 异步任务 ---"
# TC-3.1 创建任务
RESP=$(curl -s -X POST "$BASE_URL/api/queries/task" \
  -H "Content-Type: application/json" -d '{"metricConfigId":2}')
check "TC-3.1 创建异步任务" "$RESP" '"code":200'

# TC-3.3 停用指标创建任务
curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null
RESP=$(curl -s -X POST "$BASE_URL/api/queries/task" \
  -H "Content-Type: application/json" -d '{"metricConfigId":1}')
check "TC-3.3 停用指标创建任务" "$RESP" '"code":400'
curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null

echo ""
echo "--- 3.4 异常场景 ---"
# TC-4.1 不存在
RESP=$(curl -s "$BASE_URL/api/queries/task/999")
check "TC-4.1 查询不存在任务" "$RESP" '"code":404'

echo ""
echo "=============================="
echo "结果: $PASS 通过, $FAIL 失败"
```

运行方式:
```bash
chmod +x test_all.sh
./test_all.sh
```

---

## 5. 数据验证说明

### 5.1 验证素材数据数量

```bash
# 通过 H2 控制台验证
# 浏览器打开 http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:omni_metric
# 执行: SELECT COUNT(*) FROM asset
# 预期: 35
```

### 5.2 验证指标配置数据

```bash
# 通过 API 验证
curl http://localhost:8080/api/metric-configs
# 验证 totalElements = 3

# 查询预置指标 1（按审核状态统计素材数量）
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1}'
# 验证各状态数量之和 = 35
```

### 5.3 验证异步任务流程

```bash
# 1. 创建任务
TASK_ID=$(curl -s -X POST http://localhost:8080/api/queries/task \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 3}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# 2. 轮询直到完成
for i in $(seq 1 5); do
  STATUS=$(curl -s "http://localhost:8080/api/queries/task/$TASK_ID" | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['data']['status'])")
  echo "第${i}次轮询: status=$STATUS"
  [ "$STATUS" = "success" ] || [ "$STATUS" = "failed" ] && break
  sleep 1
done
```

---

## 6. 测试结果记录表

**测试日期**: 2026-06-06
**测试环境**: JDK 17 / Spring Boot 3.2.5 / H2 in-memory database
**测试工具**: curl + python3

### 测试结果统计

| 类别 | 用例数 | 通过 | 失败 | 通过率 |
|------|--------|------|------|--------|
| TC-1 指标配置管理 | 9 | 9 | 0 | 100% |
| TC-2 指标查询 | 5 | 5 | 0 | 100% |
| TC-3 异步任务 | 4 | 4 | 0 | 100% |
| TC-4 异常场景 | 2 | 2 | 0 | 100% |
| **总计** | **20** | **20** | **0** | **100%** |

### 逐用例结果

| 用例编号 | 用例名称 | 测试结果 | 备注 |
|----------|----------|----------|------|
| TC-1.1 | 查询指标配置列表 | ✅ | totalElements=3 |
| TC-1.2 | 分页查询指标配置 | ✅ | page=0,size=2, totalPages=2 |
| TC-1.3 | 查询单个指标配置 | ✅ | 返回完整配置信息含聚合类型 |
| TC-1.4 | 查询不存在的指标配置 | ✅ | 返回 404 + 错误信息 |
| TC-1.5 | 新增指标配置 | ✅ | 返回 201 + 自动生成 ID |
| TC-1.6 | 新增指标 - 参数校验失败 | ✅ | 返回 400 + 缺失字段列表 |
| TC-1.7 | 新增指标 - 非法字段 | ✅ | SQL注入防护生效 |
| TC-1.8 | 编辑指标配置 | ✅ | 部分更新正常 |
| TC-1.9 | 启用/停用指标配置 | ✅ | 停用后查询被拦截，启用后恢复 |
| TC-2.1 | COUNT + GROUP BY 查询 | ✅ | 3种状态，数量之和=35 |
| TC-2.2 | AVG + WHERE + GROUP BY 查询 | ✅ | 仅返回 approved 素材 |
| TC-2.3 | SUM + GROUP BY 查询 | ✅ | 7个城市，按降序排列 |
| TC-2.4 | 带附加筛选条件查询 | ✅ | 附加条件生效且不持久化 |
| TC-2.5 | 新增配置实现新指标（零代码） | ✅ | 无需重启或写 Java 代码 |
| TC-3.1 | 创建异步查询任务 | ✅ | 自动执行完成 |
| TC-3.2 | 查询任务状态 | ✅ | 返回 success + resultData |
| TC-3.3 | 停用指标创建任务 | ✅ | 被 400 拦截 |
| TC-3.4 | 重试失败的任务 | ✅ | 非失败任务重试被拒绝 |
| TC-4.1 | 资源不存在 (404) | ✅ | 统一的 404 错误格式 |
| TC-4.2 | 参数校验失败 (400) | ✅ | 统一的 400 错误格式 |

---

## 7. 附录：curl 常用参数说明

```bash
# -s: silent 模式，不显示进度
# -X: 指定 HTTP 方法 (GET/POST/PUT/PATCH/DELETE)
# -H: 设置请求头
# -d: 设置请求体（JSON 格式）
# | python3 -m json.tool: 格式化 JSON 输出

# 示例：自定义端口
curl http://localhost:8080/api/metric-configs

# 示例：带认证头（如果需要）
curl -H "Authorization: Bearer xxx" http://localhost:8080/api/metric-configs
```
