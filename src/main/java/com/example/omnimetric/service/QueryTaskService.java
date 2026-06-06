package com.example.omnimetric.service;

import com.example.omnimetric.exception.ResourceNotFoundException;
import com.example.omnimetric.model.dto.response.TaskResponse;
import com.example.omnimetric.model.entity.MetricConfig;
import com.example.omnimetric.model.entity.QueryTask;
import com.example.omnimetric.repository.MetricConfigRepository;
import com.example.omnimetric.repository.QueryTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 异步查询任务服务
 * 负责创建异步查询任务、跟踪任务状态、支持失败重试。
 *
 * 使用 Spring @Async 异步执行，任务信息持久化到数据库。
 */
@Service
public class QueryTaskService {

    private static final Logger log = LoggerFactory.getLogger(QueryTaskService.class);

    private final QueryTaskRepository taskRepository;
    private final MetricConfigRepository configRepository;
    private final MetricQueryService queryService;
    private final ObjectMapper objectMapper;

    public QueryTaskService(QueryTaskRepository taskRepository,
                            MetricConfigRepository configRepository,
                            MetricQueryService queryService,
                            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.configRepository = configRepository;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建异步查询任务
     */
    @Transactional
    public TaskResponse createTask(Long metricConfigId) {
        // 验证指标存在且已启用
        MetricConfig config = configRepository.findById(metricConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("指标配置", metricConfigId));
        if (!config.getEnabled()) {
            throw new IllegalArgumentException("指标配置已停用，无法创建任务");
        }

        QueryTask task = new QueryTask();
        task.setMetricConfigId(metricConfigId);
        task.setStatus("pending");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);

        QueryTask saved = taskRepository.save(task);

        // 异步执行任务
        executeTaskAsync(saved.getId());

        return TaskResponse.fromEntity(saved);
    }

    /**
     * 异步执行查询任务
     */
    @Async("taskExecutor")
    public void executeTaskAsync(Long taskId) {
        QueryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("查询任务", taskId));

        try {
            log.info("Starting async task {} for metric config {}", taskId, task.getMetricConfigId());

            task.setStatus("running");
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);

            // 执行查询
            var result = queryService.executeQuery(task.getMetricConfigId(), null);

            // 序列化结果
            String jsonResult = objectMapper.writeValueAsString(result);

            task.setStatus("success");
            task.setResultData(jsonResult);
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

            log.info("Async task {} completed successfully", taskId);

        } catch (Exception e) {
            log.error("Async task {} failed: {}", taskId, e.getMessage());

            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            task.setRetryCount(task.getRetryCount() + 1);
            taskRepository.save(task);

            // 失败重试逻辑
            if (task.getRetryCount() < task.getMaxRetryCount()) {
                log.info("Retrying task {} (attempt {}/{})", taskId,
                        task.getRetryCount(), task.getMaxRetryCount());
                executeTaskAsync(taskId);
            } else {
                log.warn("Task {} exhausted all retries ({})", taskId, task.getMaxRetryCount());
                task.setStatus("failed");
                task.setErrorMessage("已重试" + task.getMaxRetryCount() + "次，任务最终失败: " + e.getMessage());
                task.setFinishedAt(LocalDateTime.now());
                taskRepository.save(task);
            }
        }
    }

    /**
     * 查询任务状态
     */
    public TaskResponse getTaskStatus(Long taskId) {
        QueryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("查询任务", taskId));
        return TaskResponse.fromEntity(task);
    }

    /**
     * 重试失败的任务
     */
    @Transactional
    public TaskResponse retryTask(Long taskId) {
        QueryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("查询任务", taskId));

        if (!"failed".equals(task.getStatus())) {
            throw new IllegalArgumentException("只有失败状态的任务可以重试");
        }

        task.setStatus("pending");
        task.setRetryCount(0);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        taskRepository.save(task);

        executeTaskAsync(taskId);

        return TaskResponse.fromEntity(task);
    }
}
