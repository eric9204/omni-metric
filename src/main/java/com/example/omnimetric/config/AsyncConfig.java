package com.example.omnimetric.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器配置
 * 使用 Spring @Async + ThreadPoolTaskExecutor 实现本地任务队列
 *
 * 选择理由：
 * - 无需引入外部依赖（如 RabbitMQ/Kafka），降低部署复杂度
 * - 满足本次作业的异步任务需求
 * - 适合单机部署、任务量较小的场景
 *
 * 局限性：
 * - 任务状态不持久化到外部存储，进程重启后内存中未完成的任务丢失
 * - 不支持分布式部署
 * - 没有任务死信队列、延时队列等高级特性
 * - 生产环境建议替换为 Redis Queue 或 RabbitMQ
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("metric-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
