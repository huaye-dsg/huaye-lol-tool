package com.example.huayeloltool.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 公用线程池
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
                2,                              // 核心线程数
                new ThreadFactory() {           // 自定义线程工厂
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Scheduled-Task-" + (++count));
                        t.setDaemon(true);  // 设置为守护线程
                        return t;
                    }
                }
        );

        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

}