#!/bin/bash
# ============================================================
# OmniMetric 测试脚本
# 对项目所有 API 进行冒烟测试并输出测试报告
#
# 用法:
#   ./scripts/test.sh                     # 需要服务已启动
#   ./scripts/test.sh --start             # 启动服务 → 测试 → 停止服务
#   ./scripts/test.sh -h                  # 查看帮助
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="http://localhost:8080"
AUTO_START=false
PASS=0
FAIL=0
ERRORS=""

# 解析参数
for arg in "$@"; do
    case "$arg" in
        --start|-s) AUTO_START=true ;;
        -h|--help)
            echo "用法: ./scripts/test.sh [--start]"
            echo ""
            echo "  --start|-s   自动启动服务 -> 运行测试 -> 停止服务"
            echo "  不加参数     直接运行测试（需手动启动服务）"
            exit 0
            ;;
    esac
done

# 断言函数
check() {
    local desc="$1"
    local actual="$2"
    local expected="$3"
    if echo "$actual" | grep -q "$expected"; then
        echo "  [PASS] $desc"
        PASS=$((PASS+1))
    else
        echo "  [FAIL] $desc"
        echo "    Expect: contains '$expected'"
        echo "    Actual: $(echo "$actual" | head -c 120)"
        FAIL=$((FAIL+1))
        ERRORS="$ERRORS\n  ✗ $desc"
    fi
}

# 启动服务
if [ "$AUTO_START" = true ]; then
    echo "============================================"
    echo "  OmniMetric 测试脚本"
    echo "============================================"
    echo ""
    echo "[INFO] 正在启动服务..."
    cd "$PROJECT_DIR"
    mvn spring-boot:run -q > /tmp/omni-metric-test.log 2>&1 &
    SERVER_PID=$!

    for i in $(seq 1 15); do
        sleep 3
        if curl -s "$BASE_URL/api/metric-configs" > /dev/null 2>&1; then
            echo "[INFO] 服务已启动（耗时 $((i*3)) 秒）"
            break
        fi
        if [ $i -eq 15 ]; then
            echo "[ERROR] 服务启动超时"
            tail -30 /tmp/omni-metric-test.log
            exit 1
        fi
    done
    echo ""
fi

echo "============================================"
echo "  OmniMetric API 测试套件"
echo "  服务: $BASE_URL"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"
echo ""

# ========== TC-1: 指标配置管理 ==========
echo "--- TC-1 指标配置管理 ---"

R=$(curl -s "$BASE_URL/api/metric-configs")
check "TC-1.1 查询列表" "$R" "totalElements"

R=$(curl -s "$BASE_URL/api/metric-configs/1")
check "TC-1.2 查询详情" "$R" "按审核状态统计素材数量"

R=$(curl -s "$BASE_URL/api/metric-configs/999")
check "TC-1.3 不存在返回404" "$R" "code\":404"

R=$(curl -s -X POST "$BASE_URL/api/metric-configs" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试指标","sourceTable":"asset","aggregateField":"asset_id","aggregateType":"COUNT"}')
check "TC-1.4 新增指标" "$R" "code\":201"
NEW_ID=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

R=$(curl -s -X POST "$BASE_URL/api/metric-configs" \
  -H "Content-Type: application/json" \
  -d '{"name":"不完整"}')
check "TC-1.5 参数校验" "$R" "code\":400"

R=$(curl -s -X POST "$BASE_URL/api/metric-configs" \
  -H "Content-Type: application/json" \
  -d '{"name":"x","sourceTable":"asset","aggregateField":"bad_field","aggregateType":"COUNT"}')
check "TC-1.6 非法字段拦截" "$R" "code\":400"

R=$(curl -s -X PUT "$BASE_URL/api/metric-configs/1" \
  -H "Content-Type: application/json" -d '{"description":"更新描述"}')
check "TC-1.7 编辑指标" "$R" "code\":200"

curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null
R=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":1}')
check "TC-1.8 停用拦截" "$R" "code\":400"
curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null
echo ""

# ========== TC-2: 指标查询 ==========
echo "--- TC-2 指标查询 ---"

R=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":1}')
check "TC-2.1 COUNT查询" "$R" "code\":200"

SUM=$(echo "$R" | python3 -c "import sys,json; print(sum(r['result'] for r in json.load(sys.stdin)['data']['rows']))" 2>/dev/null)
check "TC-2.1 总数=35" "$SUM" "35"

R=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":2}')
check "TC-2.2 AVG+WHERE查询" "$R" "code\":200"

R=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":3}')
check "TC-2.3 SUM+GROUP BY查询" "$R" "code\":200"

R=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" \
  -d "{\"metricConfigId\":1,\"additionalFilter\":\"uploader = '张三'\"}")
check "TC-2.4 附加筛选条件" "$R" "code\":200"
echo ""

# ========== TC-3: 异步任务 ==========
echo "--- TC-3 异步任务 ---"

R=$(curl -s -X POST "$BASE_URL/api/queries/task" \
  -H "Content-Type: application/json" -d '{"metricConfigId":2}')
check "TC-3.1 创建任务" "$R" "code\":200"
TID=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

sleep 2
R=$(curl -s "$BASE_URL/api/queries/task/$TID")
check "TC-3.2 查询任务状态" "$R" "code\":200"

curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null
R=$(curl -s -X POST "$BASE_URL/api/queries/task" \
  -H "Content-Type: application/json" -d '{"metricConfigId":1}')
check "TC-3.3 停用拦截" "$R" "code\":400"
curl -s -X PATCH "$BASE_URL/api/metric-configs/1/toggle" > /dev/null

R=$(curl -s -X POST "$BASE_URL/api/queries/task/$TID/retry")
check "TC-3.4 非失败重试拒绝" "$R" "code\":400"
echo ""

# ========== TC-4: 异常场景 ==========
echo "--- TC-4 异常场景 ---"

R=$(curl -s "$BASE_URL/api/queries/task/999")
check "TC-4.1 不存在任务404" "$R" "code\":404"

R=$(curl -s -X POST "$BASE_URL/api/queries/task" \
  -H "Content-Type: application/json" -d '{}')
check "TC-4.2 空参数400" "$R" "code\":400"
echo ""

# ========== TC-5: 指标复用 ==========
echo "--- TC-5 指标复用 ---"

GK1=$(curl -s "$BASE_URL/api/metric-configs/1" | python3 -c "import sys,json; print(json.load(sys.stdin)['data'].get('groupKey',''))" 2>/dev/null)
GK4=$(curl -s "$BASE_URL/api/metric-configs/4" | python3 -c "import sys,json; print(json.load(sys.stdin)['data'].get('groupKey',''))" 2>/dev/null)
check "TC-5.1 groupKey相同" "$GK1" "$GK4"

R=$(curl -s -X POST "$BASE_URL/api/queries/execute" \
  -H "Content-Type: application/json" -d '{"metricConfigId":4}')
check "TC-5.2 指标4查询（分组合并）" "$R" "code\":200"
check "TC-5.2 聚合为SUM" "$R" '"aggregateType":"SUM"'

R1=$(echo "$R" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']['rows']))" 2>/dev/null)
check "TC-5.2 指标4有3种状态" "$R1" "3"
echo ""

# ========== 汇总 ==========
echo "============================================"
echo "  测试汇总"
echo "  通过: $PASS"
echo "  失败: $FAIL"
if [ $FAIL -gt 0 ]; then
    echo "  失败详情:"
    echo -e "$ERRORS"
fi
echo "  完成时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"

# 停止服务
if [ "$AUTO_START" = true ]; then
    kill "$SERVER_PID" 2>/dev/null
    echo "[INFO] 服务已停止"
fi

exit $FAIL
