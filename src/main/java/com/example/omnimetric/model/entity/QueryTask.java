package com.example.omnimetric.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 查询或导出任务表
 * 记录异步查询/导出任务的执行状态、结果和错误信息。
 * 支持失败重试机制。
 */
@Entity
@Table(name = "query_task", indexes = {
    @Index(name = "idx_task_status", columnList = "status"),
    @Index(name = "idx_task_metric_config", columnList = "metric_config_id")
})
public class QueryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的指标配置ID */
    @Column(name = "metric_config_id", nullable = false)
    private Long metricConfigId;

    /** 任务状态：pending/running/success/failed */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "pending";

    /** 查询结果数据（JSON格式） */
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    /** 错误信息 */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** 任务开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 任务完成时间 */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** 已重试次数 */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /** 最大重试次数 */
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 3;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
