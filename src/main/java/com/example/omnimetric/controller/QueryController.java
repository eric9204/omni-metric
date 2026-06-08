package com.example.omnimetric.controller;

import com.example.omnimetric.model.dto.request.QueryExecuteRequest;
import com.example.omnimetric.model.dto.request.QueryTaskRequest;
import com.example.omnimetric.model.dto.response.ApiResponse;
import com.example.omnimetric.model.dto.response.QueryResultResponse;
import com.example.omnimetric.model.dto.response.TaskResponse;
import com.example.omnimetric.service.MetricQueryService;
import com.example.omnimetric.service.QueryTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 指标查询与异步任务 API
 */
@RestController
@RequestMapping("/api/queries")
public class QueryController {

    private final MetricQueryService metricQueryService;
    private final QueryTaskService queryTaskService;

    public QueryController(MetricQueryService metricQueryService,
                           QueryTaskService queryTaskService) {
        this.metricQueryService = metricQueryService;
        this.queryTaskService = queryTaskService;
    }

    /**
     * 同步执行单个指标查询
     * POST /api/queries/execute
     */
    @PostMapping("/execute")
    public ApiResponse<QueryResultResponse> executeQuery(
            @Valid @RequestBody QueryExecuteRequest request) {
        QueryResultResponse result = metricQueryService.executeQuery(
                request.getMetricConfigId(), request.getAdditionalFilter());
        return ApiResponse.success(result);
    }

    /**
     * 创建异步查询任务
     * POST /api/queries/task
     */
    @PostMapping("/task")
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody QueryTaskRequest request) {
        TaskResponse task = queryTaskService.createTask(request.getMetricConfigId());
        return ApiResponse.success("任务创建成功，可通过 GET /api/queries/task/" + task.getId() + " 查询状态", task);
    }

    /**
     * 查询任务状态
     * GET /api/queries/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse<TaskResponse> getTaskStatus(@PathVariable Long taskId) {
        TaskResponse task = queryTaskService.getTaskStatus(taskId);
        return ApiResponse.success(task);
    }

    /**
     * 重试失败的任务
     * POST /api/queries/task/{taskId}/retry
     */
    @PostMapping("/task/{taskId}/retry")
    public ApiResponse<TaskResponse> retryTask(@PathVariable Long taskId) {
        TaskResponse task = queryTaskService.retryTask(taskId);
        return ApiResponse.success("任务已重新提交", task);
    }
}
