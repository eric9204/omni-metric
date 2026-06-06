package com.example.omnimetric.controller;

import com.example.omnimetric.model.dto.request.MetricConfigCreateRequest;
import com.example.omnimetric.model.dto.request.MetricConfigUpdateRequest;
import com.example.omnimetric.model.dto.response.ApiResponse;
import com.example.omnimetric.model.dto.response.MetricConfigResponse;
import com.example.omnimetric.model.dto.response.PageResponse;
import com.example.omnimetric.service.MetricConfigService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 指标配置管理 API
 */
@RestController
@RequestMapping("/api/metric-configs")
public class MetricConfigController {

    private final MetricConfigService metricConfigService;

    public MetricConfigController(MetricConfigService metricConfigService) {
        this.metricConfigService = metricConfigService;
    }

    /**
     * 新增指标配置
     * POST /api/metric-configs
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MetricConfigResponse> create(@Valid @RequestBody MetricConfigCreateRequest request) {
        MetricConfigResponse response = metricConfigService.create(request);
        return ApiResponse.created(response);
    }

    /**
     * 编辑指标配置
     * PUT /api/metric-configs/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<MetricConfigResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MetricConfigUpdateRequest request) {
        MetricConfigResponse response = metricConfigService.update(id, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取指标配置详情
     * GET /api/metric-configs/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<MetricConfigResponse> getById(@PathVariable Long id) {
        MetricConfigResponse response = metricConfigService.getById(id);
        return ApiResponse.success(response);
    }

    /**
     * 分页查询指标配置列表
     * GET /api/metric-configs?page=0&size=10&sortBy=createdAt&sortOrder=desc
     */
    @GetMapping
    public ApiResponse<PageResponse<MetricConfigResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        Page<MetricConfigResponse> pageResult = metricConfigService.list(page, size, sortBy, sortOrder);
        PageResponse<MetricConfigResponse> pageResponse = new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
        return ApiResponse.success(pageResponse);
    }

    /**
     * 启用/停用指标配置
     * PATCH /api/metric-configs/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ApiResponse<MetricConfigResponse> toggleEnabled(@PathVariable Long id) {
        MetricConfigResponse response = metricConfigService.toggleEnabled(id);
        return ApiResponse.success(response);
    }
}
