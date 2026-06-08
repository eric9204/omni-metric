#!/bin/bash
# ============================================================
# OmniMetric 启动脚本
# 用法:
#   ./scripts/start.sh                    # 默认 H2 启动
#   ./scripts/start.sh mysql              # MySQL 模式启动
#   ./scripts/start.sh --build            # 先编译后启动（H2）
#   ./scripts/start.sh mysql --build      # 先编译后启动（MySQL）
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD=false
PROFILE=""

for arg in "$@"; do
    case "$arg" in
        --build|-b) BUILD=true ;;
        mysql|mysql) PROFILE="mysql" ;;
        -h|--help)
            echo "用法: ./scripts/start.sh [mysql] [--build]"
            echo ""
            echo "参数:"
            echo "  mysql        使用 MySQL 数据库（默认: H2）"
            echo "  --build|-b   先执行 mvn clean package 再启动"
            echo "  -h,--help    显示帮助信息"
            exit 0
            ;;
    esac
done

cd "$PROJECT_DIR"

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "[ERROR] 未找到 Java 命令，请安装 JDK 17+"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VER" -lt "17" ] 2>/dev/null; then
    echo "[WARN] 当前 Java 版本为 $(java -version 2>&1 | head -1)，建议使用 JDK 17+"
fi

# 编译
if [ "$BUILD" = true ]; then
    echo "[INFO] 开始编译项目..."
    mvn clean package -DskipTests -q
    echo "[INFO] 编译完成"
fi

# 设置 Spring Profile
SPRING_ARGS=""
if [ "$PROFILE" = "mysql" ]; then
    echo "[INFO] 使用 MySQL 数据库启动"
    echo "[INFO] 请确保已执行 sql/init.sql 初始化数据库"
    SPRING_ARGS="--spring.profiles.active=mysql"
else
    echo "[INFO] 使用 H2 内存数据库启动（无需额外配置）"
fi

echo "[INFO] 启动 OmniMetric 服务..."
echo "[INFO] PID: $$"
echo "[INFO] 访问地址: http://localhost:8080"
echo "[INFO] API 文档: docs/api.md"
echo "[INFO] H2 控制台: http://localhost:8080/h2-console (H2 模式)"
echo ""

if [ "$BUILD" = true ]; then
    java -jar target/omni-metric-1.0.0.jar $SPRING_ARGS
else
    mvn spring-boot:run $SPRING_ARGS
fi
