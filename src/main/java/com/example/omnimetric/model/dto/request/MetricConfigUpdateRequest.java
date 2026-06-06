package com.example.omnimetric.model.dto.request;

public class MetricConfigUpdateRequest {

    private String name;
    private String description;
    private String sourceTable;
    private String aggregateField;
    private String aggregateType;
    private String groupByField;
    private String filterConditions;
    private String sortBy;
    private String sortOrder;
    private Boolean enabled;

    // --- Getters & Setters ---

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

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
