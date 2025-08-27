package com.example.huayeloltool;

import com.example.huayeloltool.service.ClientMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("正在启动简洁版本LOL工具...");

        // 使用原生Spring容器，而不是SpringApplication
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // 注册主配置类
        context.register(Application.class);

        // 刷新上下文，启动Spring容器
        context.refresh();

        // 从Spring容器获取服务实例
        ClientMonitor clientMonitor = context.getBean(ClientMonitor.class);

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("🔄 正在关闭简易模式...");
            context.close();
            log.info("✅ 简易模式已安全关闭");
        }));

        // 阻塞主线程，保持程序运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {

        }

    }
}