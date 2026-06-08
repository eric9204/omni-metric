package com.example.omnimetric.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 指标配置表
 * 业务人员通过配置定义指标，无需研发为每个指标单独写死查询逻辑。
 * 支持聚合方式、分组维度、筛选条件等配置化能力。
 *
 * groupKey: 自动计算的分组口径标识。
 * 当多个指标的 sourceTable + groupByField + filterConditions 相同时，
 * groupKey 相同，查询引擎会合并执行（一次查询返回多个指标结果），
 * 实现计算复用。
 */
@Entity
@Table(name = "metric_config", indexes = {
    @Index(name = "idx_group_key", columnList = "groupKey")
})
public class MetricConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指标名称 */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 指标描述 */
    @Column(name = "description", length = 500)
    private String description;

    /** 来源数据表或数据集 */
    @Column(name = "source_table", length = 100, nullable = false)
    private String sourceTable = "asset";

    /** 统计字段（如 file_size_bytes, asset_id, duration_seconds） */
    @Column(name = "aggregate_field", length = 100, nullable = false)
    private String aggregateField;

    /** 聚合方式：COUNT/SUM/AVG/MAX/MIN */
    @Column(name = "aggregate_type", length = 20, nullable = false)
    private String aggregateType;

    /** 分组维度（如 uploader, status, city, platform） */
    @Column(name = "group_by_field", length = 100)
    private String groupByField;

    /** 固定筛选条件（如 status = 'approved'） */
    @Column(name = "filter_conditions", length = 500)
    private String filterConditions;

    /** 分组口径标识，自动计算：sourceTable:groupByField:filterConditions 的hash
     *  相同 groupKey 的指标共享计算 */
    @Column(name = "group_key", length = 32)
    private String groupKey;

    /** 排序字段 */
    @Column(name = "sort_by", length = 100)
    private String sortBy;

    /** 排序方向：asc/desc */
    @Column(name = "sort_order", length = 10)
    private String sortOrder;

    /** 启用/停用状态 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }

    public String getAggregateField() { return aggregateField; }
    public void setAggregateField(String aggregateField) { this.aggregateField = aggregateField; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getGroupByField() { return groupByField; }
    public void setGroupByField(String groupByField) { this.groupByField = groupByField; }

    public String getFilterConditions() { return filterConditions; }
    public void setFilterConditions(String filterConditions) { this.filterConditions = filterConditions; }

    public String getGroupKey() { return groupKey; }
    public void setGroupKey(String groupKey) { this.groupKey = groupKey; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
