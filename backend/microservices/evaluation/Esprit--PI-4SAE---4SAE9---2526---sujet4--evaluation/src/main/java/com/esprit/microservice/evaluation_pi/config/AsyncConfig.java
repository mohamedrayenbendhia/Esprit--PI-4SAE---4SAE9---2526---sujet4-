package com.esprit.microservice.evaluation_pi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Active le support @Async de Spring.
 * Le pool "sse-push-executor" est dédié aux push SSE asynchrones.
 *
 * ➜ Ajouter @EnableAsync ici suffit — inutile de le mettre sur la classe main.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ssePushExecutor")
    public Executor ssePushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("sse-push-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}