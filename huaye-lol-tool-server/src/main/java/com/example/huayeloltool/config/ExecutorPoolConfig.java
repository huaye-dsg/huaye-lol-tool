package com.example.huayeloltool.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
     * WebSocket消息处理线程池
     * 用于处理WebSocket接收到的消息
     */
    @Bean("webSocketMessageExecutor")
    public ExecutorService webSocketMessageExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4,                              // 核心线程数
                8,                              // 最大线程数
                60L,                            // 空闲线程存活时间
                TimeUnit.SECONDS,               // 时间单位
                new LinkedBlockingQueue<>(100), // 工作队列，限制队列大小防止内存溢出
                new ThreadFactory() {           // 自定义线程工厂
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "WebSocket-Message-" + (++count));
                        t.setDaemon(true);  // 设置为守护线程
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者执行
        );

        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * 游戏事件处理线程池
     * 用于处理游戏流程相关的事件
     */
    @Bean("gameEventExecutor")
    public ExecutorService gameEventExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,                              // 核心线程数
                3,                              // 最大线程数
                60L,                            // 空闲线程存活时间
                TimeUnit.SECONDS,               // 时间单位
                new LinkedBlockingQueue<>(50),  // 工作队列
                new ThreadFactory() {           // 自定义线程工厂
                    private int count = 0;

                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = new Thread(r, "Game-Event-" + (++count));
                        t.setDaemon(true);  // 设置为守护线程
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者执行
        );

        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * 定时任务线程池
     * 用于处理延迟执行和定时任务
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

    /**
     * 通用异步任务线程池
     * 用于处理音频播放等一般异步任务
     */
    @Bean("commonAsyncExecutor")
    public ExecutorService commonAsyncExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,                              // 核心线程数（音频播放不需要太多线程）
                2,                              // 最大线程数
                60L,                            // 空闲线程存活时间
                TimeUnit.SECONDS,               // 时间单位
                new LinkedBlockingQueue<>(10),  // 工作队列（音频播放队列不需要太长）
                new ThreadFactory() {           // 自定义线程工厂
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Common-Async-" + (++count));
                        t.setDaemon(true);  // 设置为守护线程
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者执行
        );

        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

}