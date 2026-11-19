package com.cqust.repair.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置（通知、操作日志异步写入），必须为有界队列。
 *
 * @author school-repair
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

    private final AppProperties appProperties;

    /**
     * 通用异步执行器。
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        AppProperties.Async async = appProperties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(async.getCorePoolSize());
        executor.setMaxPoolSize(async.getMaxPoolSize());
        executor.setQueueCapacity(async.getQueueCapacity());
        executor.setKeepAliveSeconds(async.getKeepAliveSeconds());
        executor.setThreadNamePrefix("repair-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
