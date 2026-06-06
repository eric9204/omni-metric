package com.example.omnimetric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OmniMetric 指标配置后台服务
 *
 * 轻量级数据产品平台后端，面向业务人员的指标配置后台服务。
 * 支持配置化指标定义、动态查询生成、异步任务执行等核心能力。
 */
@SpringBootApplication
public class OmniMetricApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmniMetricApplication.class, args);
    }
}
