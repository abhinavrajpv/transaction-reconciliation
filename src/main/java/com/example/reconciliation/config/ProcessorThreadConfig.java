package com.example.reconciliation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ProcessorThreadConfig {

    @Bean
    ThreadPoolTaskExecutor processorTaskExecutor(
            @Value("${reconciliation.processor.core-pool-size}") int corePoolSize,
            @Value("${reconciliation.processor.max-pool-size}") int maxPoolSize,
            @Value("${reconciliation.processor.queue-capacity}") int queueCapacity) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("reconciliation-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);

        return executor;
    }
}