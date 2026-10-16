package com.example.reconciliation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ProcessorThreadConfig {

	@Bean
	public ThreadPoolTaskExecutor firstCsvTaskExecutor(
			@Value("${reconciliation.processor.core-pool-size}") int corePoolSize,
			@Value("${reconciliation.processor.max-pool-size}") int maxPoolSize,
			@Value("${reconciliation.processor.queue-capacity}") int queueCapacity) {

		return createExecutor(corePoolSize, maxPoolSize, queueCapacity, "first-csv-");
	}

	@Bean
	public ThreadPoolTaskExecutor secondCsvTaskExecutor(
			@Value("${reconciliation.processor.core-pool-size}") int corePoolSize,
			@Value("${reconciliation.processor.max-pool-size}") int maxPoolSize,
			@Value("${reconciliation.processor.queue-capacity}") int queueCapacity) {

		return createExecutor(corePoolSize, maxPoolSize, queueCapacity, "second-csv-");
	}

	private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity,
			String threadNamePrefix) {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setWaitForTasksToCompleteOnShutdown(true);

		return executor;
	}
}