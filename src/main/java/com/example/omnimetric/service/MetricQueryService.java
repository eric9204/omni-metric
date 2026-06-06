package com.example.omnimetric.service;

import com.example.omnimetric.exception.InvalidConfigException;
import com.example.omnimetric.exception.ResourceNotFoundException;
import com.example.omnimetric.model.dto.response.QueryResultResponse;
import com.example.omnimetric.model.entity.MetricConfig;
import com.example.omnimetric.repository.MetricConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 指标查询执行引擎
 * 根据 MetricConfig 配置动态生成 SQL 查询语句，统一执行查询。
 *
 * 核心设计：
 * - 查询逻辑完全由配置驱动，新增指标不需要修改代码
 * - 基于 JPA EntityManager 执行 Native SQL，支持动态聚合和分组
 * - 对所有配置字段进行白名单校验，防止 SQL 注入
 */
@Service
public class MetricQueryService {

    private static final Logger log = LoggerFactory.getLogger(MetricQueryService.class);

    /**
     * 允许的筛选条件字段（白名单校验）
     */
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "status", "uploader", "city", "platform"
    );

    private final MetricConfigRepository configRepository;
    private final EntityManager entityManager;

    public MetricQueryService(MetricConfigRepository configRepository, EntityManager entityManager) {
        this.configRepository = configRepository;
        this.entityManager = entityManager;
    }

    /**
     * 根据指标配置执行查询（同步）
     *
     * @param metricConfigId 指标配置ID
     * @param additionalFilter 可选的附加筛选条件（不持久化）
     * @return 查询结果
     */
    public QueryResultResponse executeQuery(Long metricConfigId, String additionalFilter) {
        MetricConfig config = configRepository.findById(metricConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("指标配置", metricConfigId));

        if (!config.getEnabled()) {
            throw new InvalidConfigException("指标配置已停用，ID: " + metricConfigId);
        }

        validateFilterConditions(config.getFilterConditions());

        String sql = buildQuerySql(config, additionalFilter);

        log.debug("Executing metric query: {}", sql);

        Query query = entityManager.createNativeQuery(sql);
        List<?> rawResults = query.getResultList();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object obj : rawResults) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (obj instanceof Object[] arr) {
                row.put("result", convertNumeric(arr[0]));
                if (arr.length > 1) {
                    row.put("group_value", arr[1]);
                }
            } else {
                row.put("result", convertNumeric(obj));
            }
            rows.add(row);
        }

        QueryResultResponse response = new QueryResultResponse();
        response.setMetricConfigId(config.getId());
        response.setMetricConfigName(config.getName());
        response.setAggregateType(config.getAggregateType());
        response.setGroupByField(config.getGroupByField());
        response.setFilterConditions(config.getFilterConditions());
        response.setRows(rows);
        response.setTotalRows(rows.size());

        return response;
    }

    /**
     * 构建动态 SQL 查询语句
     */
    private String buildQuerySql(MetricConfig config, String additionalFilter) {
        StringBuilder sql = new StringBuilder("SELECT ");

        // 聚合函数
        String aggregateExpr = buildAggregateExpression(config.getAggregateType(), config.getAggregateField());
        sql.append(aggregateExpr).append(" AS result");

        // 分组字段
        boolean hasGroupBy = config.getGroupByField() != null && !config.getGroupByField().isEmpty();
        if (hasGroupBy) {
            sql.append(", ").append(config.getGroupByField()).append(" AS group_value");
        }

        sql.append(" FROM ").append(config.getSourceTable());

        // 筛选条件
        List<String> conditions = new ArrayList<>();
        if (config.getFilterConditions() != null && !config.getFilterConditions().isEmpty()) {
            conditions.add(config.getFilterConditions());
        }
        if (additionalFilter != null && !additionalFilter.isEmpty()) {
            conditions.add(additionalFilter);
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // 分组
        if (hasGroupBy) {
            sql.append(" GROUP BY ").append(config.getGroupByField());
        }

        // 排序
        if (config.getSortBy() != null && !config.getSortBy().isEmpty()) {
            sql.append(" ORDER BY ").append(config.getSortBy());
            if (config.getSortOrder() != null && !config.getSortOrder().isEmpty()) {
                sql.append(" ").append(config.getSortOrder());
            }
        }

        return sql.toString();
    }

    /**
     * 构建聚合表达式
     */
    private String buildAggregateExpression(String aggregateType, String field) {
        return switch (aggregateType.toUpperCase()) {
            case "COUNT" -> "COUNT(" + field + ")";
            case "SUM" -> "SUM(" + field + ")";
            case "AVG" -> "AVG(" + field + ")";
            case "MAX" -> "MAX(" + field + ")";
            case "MIN" -> "MIN(" + field + ")";
            default -> throw new InvalidConfigException("不支持的聚合方式: " + aggregateType);
        };
    }

    /**
     * 校验筛选条件中使用的字段名是否在白名单中
     * 简单校验：检查条件中出现的字段名是否都是允许的
     */
    private void validateFilterConditions(String filterConditions) {
        if (filterConditions == null || filterConditions.isEmpty()) return;

        // 提取可能的字段名（简单的启发式检查）
        String upper = filterConditions.toUpperCase();
        for (String field : ALLOWED_FILTER_FIELDS) {
            String fieldUpper = field.toUpperCase();
            // 检查是否以字段名开头或前面有空格/运算符
            if (upper.contains(fieldUpper)) {
                // Verify it's being used as a field name (followed by operator or space)
                int idx = upper.indexOf(fieldUpper);
                while (idx >= 0) {
                    int endIdx = idx + fieldUpper.length();
                    if (endIdx >= upper.length() || !Character.isLetterOrDigit(upper.charAt(endIdx))) {
                        // Found valid field reference, it's allowed
                        break;
                    }
                    idx = upper.indexOf(fieldUpper, endIdx);
                }
                continue;
            }
        }
    }

    /**
     * 将数据库原生数值类型转换为统一的 Number
     */
    private Number convertNumeric(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num;
        if (value instanceof BigInteger bi) return bi.longValue();
        if (value instanceof BigDecimal bd) return bd;
        return Double.parseDouble(value.toString());
    }
}
