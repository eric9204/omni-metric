package com.example.omnimetric.model.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 创建异步查询任务的请求
 */
public class QueryTaskRequest {

    @NotNull(message = "指标配置ID不能为空")
    private Long metricConfigId;

    public Long getMetricConfigId() { return metricConfigId; }
    public void setMetricConfigId(Long metricConfigId) { this.metricConfigId = metricConfigId; }
}
