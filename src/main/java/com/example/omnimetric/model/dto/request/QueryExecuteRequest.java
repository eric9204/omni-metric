package com.example.omnimetric.model.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 同步执行指标查询的请求
 */
public class QueryExecuteRequest {

    @NotNull(message = "指标配置ID不能为空")
    private Long metricConfigId;

    // 可选的动态过滤条件（不持久化，仅本次查询生效）
    private String additionalFilter;

    public Long getMetricConfigId() { return metricConfigId; }
    public void setMetricConfigId(Long metricConfigId) { this.metricConfigId = metricConfigId; }

    public String getAdditionalFilter() { return additionalFilter; }
    public void setAdditionalFilter(String additionalFilter) { this.additionalFilter = additionalFilter; }
}
