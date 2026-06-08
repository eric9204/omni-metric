# OmniMetric - 指标配置后台服务

轻量级数据产品平台后端，面向业务人员的指标配置后台服务。业务人员可以通过配置定义指标，无需每次找研发编写 SQL 或开发接口。

## 技术栈

- **语言**: Java 17
- **框架**: Spring Boot 3.2 + Spring Data JPA
- **数据库**: H2 (开发) / MySQL 8.0 (生产)
- **任务队列**: Spring @Async + ThreadPoolTaskExecutor
- **构建工具**: Maven

## 快速启动

### 环境要求

- JDK 17+
- Maven 3.8+

### 启动步骤

```bash
# 1. 克隆或进入项目目录
cd omni-metric

# 2. 编译并启动（默认使用 H2 内存数据库）
mvn spring-boot:run

# 3. 或先编译成 JAR 再运行
mvn clean package -DskipTests
java -jar target/omni-metric-1.0.0.jar
```

### 使用启动脚本

```bash
# 默认 H2 模式启动
./scripts/start.sh

# MySQL 模式启动
./scripts/start.sh mysql

# 先编译再启动
./scripts/start.sh --build

# 查看帮助
./scripts/start.sh -h
```

### 运行测试

```bash
# 需要服务已启动
./scripts/test.sh

# 自动启动-测试-停止（推荐）
./scripts/test.sh --start
```

启动后访问：
- API 服务: `http://localhost:8080`
- H2 控制台: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:omni_metric`)

### 使用 MySQL

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

需要先执行 `sql/init.sql` 初始化数据库。

## 预置数据

启动时自动生成：
- **35 条**模拟素材数据（6 个上传人、7 个城市、4 个平台、多种标签和审核状态）
- **4 个**预置指标配置（含 1 组复用示例）

### 预置指标

| 指标 | 聚合 | 分组 | 筛选 | 业务价值 |
|------|------|------|------|----------|
| 按审核状态统计素材数量 | COUNT(asset_id) | status | - | 了解素材审核分布 |
| 已通过素材的各上传人平均文件大小 | AVG(file_size_bytes) | uploader | status='approved' | 评估上传人的内容质量 |
| 各城市素材总时长 | SUM(duration_seconds) | city | - | 评估区域内容产出规模 |
| 各审核状态素材总时长 | SUM(duration_seconds) | status | - | 与指标1复用分组口径，合并计算 |

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/metric-configs` | 新增指标配置 |
| PUT | `/api/metric-configs/{id}` | 编辑指标配置 |
| GET | `/api/metric-configs/{id}` | 获取指标详情 |
| GET | `/api/metric-configs` | 获取指标列表（分页） |
| PATCH | `/api/metric-configs/{id}/toggle` | 启用/停用指标 |
| POST | `/api/queries/execute` | 同步执行指标查询 |
| POST | `/api/queries/task` | 创建异步查询任务 |
| GET | `/api/queries/task/{id}` | 查询任务状态 |
| POST | `/api/queries/task/{id}/retry` | 重试失败任务 |

详细 API 文档请见 [docs/api.md](docs/api.md)。

## 测试验证

### 使用 curl 快速验证

```bash
# 1. 查看预置指标列表
curl http://localhost:8080/api/metric-configs

# 2. 执行指标查询（例如指标ID=1：按审核状态统计素材数量）
curl -X POST http://localhost:8080/api/queries/execute \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 1}'

# 3. 创建异步查询任务
curl -X POST http://localhost:8080/api/queries/task \
  -H "Content-Type: application/json" \
  -d '{"metricConfigId": 2}'

# 4. 查询任务状态（替换 taskId 为上一步返回的 ID）
curl http://localhost:8080/api/queries/task/1

# 5. 新增指标配置
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

## 项目结构

```
omni-metric/
├── pom.xml                          # Maven 构建配置
├── src/main/java/com/example/omnimetric/
│   ├── OmniMetricApplication.java   # 应用入口
│   ├── config/
│   │   └── AsyncConfig.java         # 异步任务配置
│   ├── controller/
│   │   ├── MetricConfigController.java  # 指标配置 API
│   │   └── QueryController.java         # 查询 & 任务 API
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Asset.java           # 素材实体
│   │   │   ├── MetricConfig.java    # 指标配置实体
│   │   │   └── QueryTask.java       # 查询任务实体
│   │   ├── dto/request/             # 请求 DTO
│   │   └── dto/response/            # 响应 DTO
│   ├── repository/                  # JPA 数据访问
│   ├── service/
│   │   ├── MetricConfigService.java # 指标配置管理
│   │   ├── MetricQueryService.java  # 动态查询引擎
│   │   ├── QueryTaskService.java    # 异步任务管理
│   │   └── DataInitializer.java     # 数据初始化
│   └── exception/                   # 异常处理
├── src/main/resources/
│   ├── application.yml              # 默认配置（H2）
│   └── application-mysql.yml        # MySQL 配置
├── sql/
│   └── init.sql                     # MySQL 初始化脚本
├── docs/
│   ├── api.md                       # API 接口文档
│   └── tech-design.md               # 技术设计文档
└── README.md                        # 本文件
```

## 设计要点

- **配置化指标**: 查询逻辑由 MetricConfig 动态驱动，新增指标仅需新增配置
- **字段白名单**: 所有查询字段通过白名单校验，防止 SQL 注入
- **统一响应**: 所有 API 使用统一 `{code, message, data, timestamp}` 格式
- **错误处理**: 全局异常处理器统一处理校验、资源不存在等异常
- **异步任务**: 支持创建/查询/重试，自动重试最多 3 次

详细设计说明请见 [docs/tech-design.md](docs/tech-design.md)。

## AI 工具使用说明

本项目使用 AI 工具辅助开发，详见 [docs/tech-design.md](docs/tech-design.md) 第8节。

## License

MIT
