package com.example.huayeloltool.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置类
 */
@Slf4j
@Configuration
public class ExecutorPoolConfig {

    /**
     * 定时任务线程池
     */
    @Bean("scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                2,                              // 2个线程足够了
                new ThreadFactory() {           
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "LOL-Task-" + (++count));
                        t.setDaemon(true);      // 守护线程
                        return t;
                    }
                }
        );

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}