package com.example.omnimetric.model.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 指标查询结果响应
 */
public class QueryResultResponse {

    private Long metricConfigId;
    private String metricConfigName;
    private String aggregateType;
    private String groupByField;
    private String filterConditions;
    private List<Map<String, Object>> rows;
    private int totalRows;

    public Long getMetricConfigId() { return metricConfigId; }
    public void setMetricConfigId(Long metricConfigId) { this.metricConfigId = metricConfigId; }

    public String getMetricConfigName() { return metricConfigName; }
    public void setMetricConfigName(String metricConfigName) { this.metricConfigName = metricConfigName; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getGroupByField() { return groupByField; }
    public void setGroupByField(String groupByField) { this.groupByField = groupByField; }

    public String getFilterConditions() { return filterConditions; }
    public void setFilterConditions(String filterConditions) { this.filterConditions = filterConditions; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
}
