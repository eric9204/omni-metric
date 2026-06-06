-- ============================================================
-- OmniMetric 数据库初始化脚本 (MySQL)
-- 适用版本: MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS omni_metric
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE omni_metric;

-- ----------------------------------------
-- 1. 素材明细数据表 (asset)
-- 存储视频素材的元数据信息
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS asset (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id        VARCHAR(64)  NOT NULL COMMENT '素材ID（业务标识）',
    title           VARCHAR(255)          COMMENT '素材标题',
    uploader        VARCHAR(100) NOT NULL COMMENT '上传人',
    uploaded_at     DATETIME     NOT NULL COMMENT '上传时间',
    file_size_bytes BIGINT       NOT NULL COMMENT '文件大小（字节）',
    status          VARCHAR(20)  NOT NULL COMMENT '审核状态: approved/rejected/pending',
    tags            VARCHAR(500)          COMMENT '标签（逗号分隔）',
    city            VARCHAR(100)          COMMENT '城市',
    platform        VARCHAR(100)          COMMENT '投放平台',
    duration_seconds INT                  COMMENT '视频时长（秒）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_asset_id (asset_id),
    KEY idx_status (status),
    KEY idx_uploader (uploader),
    KEY idx_city (city),
    KEY idx_platform (platform),
    KEY idx_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材明细数据表';


-- ----------------------------------------
-- 2. 指标配置表 (metric_config)
-- 存储业务人员配置的指标定义
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS metric_config (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL COMMENT '指标名称',
    description       VARCHAR(500)          COMMENT '指标描述',
    source_table      VARCHAR(100) NOT NULL DEFAULT 'asset' COMMENT '来源数据表',
    aggregate_field   VARCHAR(100) NOT NULL COMMENT '统计字段',
    aggregate_type    VARCHAR(20)  NOT NULL COMMENT '聚合方式: COUNT/SUM/AVG/MAX/MIN',
    group_by_field    VARCHAR(100)          COMMENT '分组维度',
    filter_conditions VARCHAR(500)          COMMENT '固定筛选条件, 如 status = ''approved''',
    sort_by           VARCHAR(100)          COMMENT '排序字段: result/group_value',
    sort_order        VARCHAR(10)           COMMENT '排序方向: asc/desc',
    enabled           TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态: 1启用/0停用',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标配置表';


-- ----------------------------------------
-- 3. 查询任务表 (query_task)
-- 记录异步查询/导出任务的执行状态及结果
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS query_task (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_config_id  BIGINT       NOT NULL COMMENT '关联指标配置ID',
    status            VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '任务状态: pending/running/success/failed',
    result_data       TEXT                   COMMENT '查询结果数据（JSON格式）',
    error_message     VARCHAR(1000)          COMMENT '错误信息',
    started_at        DATETIME               COMMENT '任务开始时间',
    finished_at       DATETIME               COMMENT '任务完成时间',
    retry_count       INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count   INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_status (status),
    KEY idx_metric_config (metric_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询任务表';


-- ----------------------------------------
-- 预置指标配置数据
-- ----------------------------------------

-- 指标1: 按审核状态统计素材数量
INSERT INTO metric_config (name, description, source_table, aggregate_field, aggregate_type, group_by_field, filter_conditions, sort_by, sort_order, enabled)
VALUES ('按审核状态统计素材数量', '统计各审核状态（已通过/已拒绝/待审核）下的素材数量', 'asset', 'asset_id', 'COUNT', 'status', NULL, 'result', 'desc', 1);

-- 指标2: 已通过素材的各上传人平均文件大小
INSERT INTO metric_config (name, description, source_table, aggregate_field, aggregate_type, group_by_field, filter_conditions, sort_by, sort_order, enabled)
VALUES ('已通过素材的各上传人平均文件大小', '统计已通过审核素材中，各上传人的平均文件大小', 'asset', 'file_size_bytes', 'AVG', 'uploader', 'status = ''approved''', 'result', 'desc', 1);

-- 指标3: 各城市素材总时长
INSERT INTO metric_config (name, description, source_table, aggregate_field, aggregate_type, group_by_field, filter_conditions, sort_by, sort_order, enabled)
VALUES ('各城市素材总时长', '统计各城市上传素材的视频总时长，反映区域内容产出规模', 'asset', 'duration_seconds', 'SUM', 'city', NULL, 'result', 'desc', 1);
