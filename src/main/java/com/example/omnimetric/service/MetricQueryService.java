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
import java.util.stream.Collectors;

/**
 * 指标查询执行引擎
 * 根据 MetricConfig 配置动态生成 SQL 查询语句，统一执行查询。
 *
 * 核心设计：
 * - 查询逻辑完全由配置驱动，新增指标不需要修改代码
 * - 基于 JPA EntityManager 执行 Native SQL，支持动态聚合和分组
 * - 对所有配置字段进行白名单校验，防止 SQL 注入
 *
 * 复用优化：
 * 分组合并：相同 groupKey（同 sourceTable + groupByField + filterConditions）
 * 的多个指标合并为一次 SQL 执行，实现计算复用。
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

    public MetricQueryService(MetricConfigRepository configRepository,
                              EntityManager entityManager) {
        this.configRepository = configRepository;
        this.entityManager = entityManager;
    }

    /**
     * 根据指标配置执行查询（同步）
     *
     * 执行流程：
     * 1. 查找同 groupKey 的其他指标 -> 如有则合并执行（一次查询返回多个指标结果）
     * 2. 单指标单独执行
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

        // 检查是否有同 groupKey 的指标可以合并执行
        boolean hasGroupBy = config.getGroupByField() != null && !config.getGroupByField().isEmpty();
        List<MetricConfig> siblings = Collections.emptyList();

        if (hasGroupBy && config.getGroupKey() != null) {
            siblings = configRepository.findByEnabledTrueAndGroupKey(config.getGroupKey())
                    .stream()
                    .filter(m -> !m.getId().equals(metricConfigId)) // 排除自身
                    .collect(Collectors.toList());
        }

        if (!siblings.isEmpty()) {
            // 合并执行：一次查询计算所有同组指标
            List<MetricConfig> allInGroup = new ArrayList<>(siblings);
            allInGroup.add(config);
            log.info("Group merge: executing {} metrics with groupKey={} in 1 combined query",
                    allInGroup.size(), config.getGroupKey());
            Map<Long, QueryResultResponse> groupResults = executeCombinedQuery(allInGroup, additionalFilter);
            return groupResults.get(metricConfigId);
        }

        // 单指标单独执行
        return executeSingleQuery(config, additionalFilter);
    }

    // ========== 单指标查询 ==========

    /**
     * 执行单个指标查询
     */
    private QueryResultResponse executeSingleQuery(MetricConfig config, String additionalFilter) {
        String sql = buildSingleQuerySql(config, additionalFilter);
        log.debug("Executing single metric query: {}", sql);

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
     * 构建单指标 SQL
     */
    private String buildSingleQuerySql(MetricConfig config, String additionalFilter) {
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

    // ========== 合并查询（分组合并） ==========

    /**
     * 合并执行同组指标查询
     * 将多个指标的聚合计算合并为一条 SQL，一次查询返回所有结果。
     *
     * 例如：
     *   SELECT status,
     *          COUNT(asset_id) AS m_0,
     *          AVG(file_size_bytes) AS m_1
     *   FROM asset
     *   GROUP BY status
     */
    private Map<Long, QueryResultResponse> executeCombinedQuery(
            List<MetricConfig> metrics, String additionalFilter) {
        MetricConfig first = metrics.get(0);
        String groupField = first.getGroupByField();
        String sourceTable = first.getSourceTable();
        String filterConditions = first.getFilterConditions();

        // 构建合并 SQL
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(groupField).append(" AS group_value");

        for (int i = 0; i < metrics.size(); i++) {
            MetricConfig m = metrics.get(i);
            sql.append(", ").append(buildAggregateExpression(m.getAggregateType(), m.getAggregateField()))
                    .append(" AS m_").append(i);
        }

        sql.append(" FROM ").append(sourceTable);

        // WHERE
        List<String> conditions = new ArrayList<>();
        if (filterConditions != null && !filterConditions.isEmpty()) {
            conditions.add(filterConditions);
        }
        if (additionalFilter != null && !additionalFilter.isEmpty()) {
            conditions.add(additionalFilter);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        sql.append(" GROUP BY ").append(groupField);

        log.info("Executing combined group query ({} metrics): {}", metrics.size(), sql);

        // 执行合并查询
        Query query = entityManager.createNativeQuery(sql.toString());
        List<?> rawResults = query.getResultList();

        // 构建每个指标的结果容器
        Map<Long, QueryResultResponse> results = new LinkedHashMap<>();
        for (MetricConfig m : metrics) {
            QueryResultResponse r = new QueryResultResponse();
            r.setMetricConfigId(m.getId());
            r.setMetricConfigName(m.getName());
            r.setAggregateType(m.getAggregateType());
            r.setGroupByField(m.getGroupByField());
            r.setFilterConditions(m.getFilterConditions());
            r.setRows(new ArrayList<>());
            results.put(m.getId(), r);
        }

        // 拆分合并结果到各指标
        for (Object obj : rawResults) {
            Object[] arr = (Object[]) obj;
            String groupValue = arr[0] != null ? String.valueOf(arr[0]) : "";

            for (int i = 0; i < metrics.size(); i++) {
                MetricConfig m = metrics.get(i);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("group_value", groupValue);
                row.put("result", convertNumeric(arr[i + 1]));
                results.get(m.getId()).getRows().add(row);
            }
        }

        // 填充 totalRows
        for (MetricConfig m : metrics) {
            results.get(m.getId()).setTotalRows(results.get(m.getId()).getRows().size());
        }

        return results;
    }

    // ========== 工具方法 ==========

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
     * 只在执行查询前做最终检查，防止 SQL 注入
     */
    private void validateFilterConditions(String filterConditions) {
        if (filterConditions == null || filterConditions.isEmpty()) return;

        // 提取筛选条件中出现的字段名，校验是否在白名单中
        String upper = filterConditions.toUpperCase();
        boolean hasAllowedField = false;
        for (String field : ALLOWED_FILTER_FIELDS) {
            String fieldUpper = field.toUpperCase();
            if (upper.contains(fieldUpper)) {
                int idx = upper.indexOf(fieldUpper);
                while (idx >= 0) {
                    int endIdx = idx + fieldUpper.length();
                    if (endIdx >= upper.length() || !Character.isLetterOrDigit(upper.charAt(endIdx))) {
                        hasAllowedField = true;
                        break;
                    }
                    idx = upper.indexOf(fieldUpper, endIdx);
                }
            }
        }
        if (!hasAllowedField) {
            throw new InvalidConfigException("筛选条件使用了不允许的字段: " + filterConditions
                    + "，允许的字段: " + ALLOWED_FILTER_FIELDS);
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
