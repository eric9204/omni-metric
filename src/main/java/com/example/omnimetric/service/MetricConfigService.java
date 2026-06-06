package com.example.omnimetric.service;

import com.example.omnimetric.exception.InvalidConfigException;
import com.example.omnimetric.exception.ResourceNotFoundException;
import com.example.omnimetric.model.dto.request.MetricConfigCreateRequest;
import com.example.omnimetric.model.dto.request.MetricConfigUpdateRequest;
import com.example.omnimetric.model.dto.response.MetricConfigResponse;
import com.example.omnimetric.model.entity.MetricConfig;
import com.example.omnimetric.repository.MetricConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标配置管理服务
 * 提供指标配置的 CRUD 操作，所有新增指标应通过配置完成。
 */
@Service
public class MetricConfigService {

    /**
     * 允许的聚合字段列表（白名单校验，防止非法查询）
     */
    private static final List<String> ALLOWED_AGGREGATE_FIELDS = List.of(
            "asset_id", "file_size_bytes", "duration_seconds"
    );

    /**
     * 允许的聚合方式
     */
    private static final List<String> ALLOWED_AGGREGATE_TYPES = List.of(
            "COUNT", "SUM", "AVG", "MAX", "MIN"
    );

    /**
     * 允许的分组字段列表
     */
    private static final List<String> ALLOWED_GROUP_BY_FIELDS = List.of(
            "status", "uploader", "city", "platform"
    );

    /**
     * 允许的排序字段
     */
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "result", "group_value"
    );

    /**
     * 允许的排序方向
     */
    private static final List<String> ALLOWED_SORT_ORDERS = List.of(
            "asc", "desc"
    );

    private final MetricConfigRepository repository;

    public MetricConfigService(MetricConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建指标配置
     */
    @Transactional
    public MetricConfigResponse create(MetricConfigCreateRequest request) {
        validateConfig(request.getAggregateField(), request.getAggregateType(),
                request.getGroupByField(), request.getSortBy(), request.getSortOrder());

        MetricConfig config = new MetricConfig();
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setSourceTable(request.getSourceTable());
        config.setAggregateField(request.getAggregateField());
        config.setAggregateType(request.getAggregateType().toUpperCase());
        config.setGroupByField(request.getGroupByField());
        config.setFilterConditions(request.getFilterConditions());
        config.setSortBy(request.getSortBy());
        config.setSortOrder(request.getSortOrder());
        config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        MetricConfig saved = repository.save(config);
        return MetricConfigResponse.fromEntity(saved);
    }

    /**
     * 更新指标配置
     */
    @Transactional
    public MetricConfigResponse update(Long id, MetricConfigUpdateRequest request) {
        MetricConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("指标配置", id));

        if (request.getAggregateField() != null) config.setAggregateField(request.getAggregateField());
        if (request.getAggregateType() != null) config.setAggregateType(request.getAggregateType().toUpperCase());
        if (request.getGroupByField() != null) config.setGroupByField(request.getGroupByField());
        if (request.getSortBy() != null) config.setSortBy(request.getSortBy());
        if (request.getSortOrder() != null) config.setSortOrder(request.getSortOrder());
        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getSourceTable() != null) config.setSourceTable(request.getSourceTable());
        if (request.getFilterConditions() != null) config.setFilterConditions(request.getFilterConditions());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());

        // Validate if query-related fields changed
        if (request.getAggregateField() != null || request.getAggregateType() != null
                || request.getGroupByField() != null || request.getSortBy() != null
                || request.getSortOrder() != null) {
            validateConfig(
                    config.getAggregateField(), config.getAggregateType(),
                    config.getGroupByField(), config.getSortBy(), config.getSortOrder());
        }

        MetricConfig saved = repository.save(config);
        return MetricConfigResponse.fromEntity(saved);
    }

    /**
     * 获取单个指标配置
     */
    public MetricConfigResponse getById(Long id) {
        MetricConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("指标配置", id));
        return MetricConfigResponse.fromEntity(config);
    }

    /**
     * 分页列表查询
     */
    public Page<MetricConfigResponse> list(int page, int size, String sortBy, String sortOrder) {
        Sort sort;
        if (sortBy != null && List.of("id", "name", "createdAt", "updatedAt").contains(sortBy)) {
            sort = Sort.by("desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        return repository.findAll(pageable).map(MetricConfigResponse::fromEntity);
    }

    /**
     * 切换启用/停用状态
     */
    @Transactional
    public MetricConfigResponse toggleEnabled(Long id) {
        MetricConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("指标配置", id));
        config.setEnabled(!config.getEnabled());
        MetricConfig saved = repository.save(config);
        return MetricConfigResponse.fromEntity(saved);
    }

    /**
     * 获取启用的所有指标配置
     */
    public List<MetricConfig> getEnabledConfigs() {
        return repository.findByEnabledTrue();
    }

    /**
     * 校验指标配置
     */
    private void validateConfig(String aggregateField, String aggregateType,
                                 String groupByField, String sortBy, String sortOrder) {
        if (!ALLOWED_AGGREGATE_FIELDS.contains(aggregateField)) {
            throw new InvalidConfigException("不支持的统计字段: " + aggregateField
                    + "，允许值: " + ALLOWED_AGGREGATE_FIELDS);
        }
        if (!ALLOWED_AGGREGATE_TYPES.contains(aggregateType.toUpperCase())) {
            throw new InvalidConfigException("不支持的聚合方式: " + aggregateType
                    + "，允许值: " + ALLOWED_AGGREGATE_TYPES);
        }
        if (groupByField != null && !groupByField.isEmpty()
                && !ALLOWED_GROUP_BY_FIELDS.contains(groupByField)) {
            throw new InvalidConfigException("不支持的分组字段: " + groupByField
                    + "，允许值: " + ALLOWED_GROUP_BY_FIELDS);
        }
        if (sortBy != null && !sortBy.isEmpty()
                && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidConfigException("不支持的排序字段: " + sortBy
                    + "，允许值: " + ALLOWED_SORT_FIELDS);
        }
        if (sortOrder != null && !sortOrder.isEmpty()
                && !ALLOWED_SORT_ORDERS.contains(sortOrder.toLowerCase())) {
            throw new InvalidConfigException("不支持的排序方向: " + sortOrder
                    + "，允许值: " + ALLOWED_SORT_ORDERS);
        }
    }
}
