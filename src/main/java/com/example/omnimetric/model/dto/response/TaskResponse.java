package com.example.omnimetric.model.dto.response;

import com.example.omnimetric.model.entity.QueryTask;
import java.time.LocalDateTime;

/**
 * 任务状态响应
 */
public class TaskResponse {

    private Long id;
    private Long metricConfigId;
    private String status;
    private String resultData;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer retryCount;
    private LocalDateTime createdAt;

    public static TaskResponse fromEntity(QueryTask task) {
        TaskResponse resp = new TaskResponse();
        resp.setId(task.getId());
        resp.setMetricConfigId(task.getMetricConfigId());
        resp.setStatus(task.getStatus());
        resp.setResultData(task.getResultData());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setStartedAt(task.getStartedAt());
        resp.setFinishedAt(task.getFinishedAt());
        resp.setRetryCount(task.getRetryCount());
        resp.setCreatedAt(task.getCreatedAt());
        return resp;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMetricConfigId() { return metricConfigId; }
    public void setMetricConfigId(Long metricConfigId) { this.metricConfigId = metricConfigId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
