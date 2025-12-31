package com.example.huayeloltool;

import com.example.huayeloltool.service.ClientMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class Main {
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        log.info("正在启动LOL工具...");

        // 直接扫描包，最简洁的方式
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.example.huayeloltool");

        // 启动客户端监控服务
        context.getBean(ClientMonitor.class);
        log.info("LOL工具启动完成，开始监控客户端...");

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭LOL工具...");
            context.close();
            shutdownLatch.countDown();
            log.info("LOL工具已安全关闭");
        }));

        // 阻塞主线程，保持程序运行
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("主线程被中断，程序退出");
        }
    }
}