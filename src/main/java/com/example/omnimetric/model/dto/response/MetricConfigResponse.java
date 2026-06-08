package com.example.omnimetric.model.dto.response;

import com.example.omnimetric.model.entity.MetricConfig;
import java.time.LocalDateTime;

/**
 * 指标配置响应对象
 */
public class MetricConfigResponse {

    private Long id;
    private String name;
    private String description;
    private String sourceTable;
    private String aggregateField;
    private String aggregateType;
    private String groupByField;
    private String filterConditions;
    private String groupKey;
    private String sortBy;
    private String sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MetricConfigResponse fromEntity(MetricConfig config) {
        MetricConfigResponse resp = new MetricConfigResponse();
        resp.setId(config.getId());
        resp.setName(config.getName());
        resp.setDescription(config.getDescription());
        resp.setSourceTable(config.getSourceTable());
        resp.setAggregateField(config.getAggregateField());
        resp.setAggregateType(config.getAggregateType());
        resp.setGroupByField(config.getGroupByField());
        resp.setFilterConditions(config.getFilterConditions());
        resp.setGroupKey(config.getGroupKey());
        resp.setSortBy(config.getSortBy());
        resp.setSortOrder(config.getSortOrder());
        resp.setEnabled(config.getEnabled());
        resp.setCreatedAt(config.getCreatedAt());
        resp.setUpdatedAt(config.getUpdatedAt());
        return resp;
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
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
