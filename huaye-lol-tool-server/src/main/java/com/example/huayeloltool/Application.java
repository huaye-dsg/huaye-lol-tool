package com.example.huayeloltool;

import com.example.huayeloltool.service.ClientMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {

        //// 检查是否包含控制台模式参数
        boolean consoleMode = false;
        for (String arg : args) {
            if ("--console".equals(arg) || "--console-mode".equals(arg)) {
                consoleMode = true;
                break;
            }
        }

        if (consoleMode) {
            log.info("=== main方法+Spring模式启动 ===");
            runSpringConsoleMode(args);
        } else {
            log.info("=== SpringBoot Web模式启动 ===");
            runWebMode(args);
        }
    }

    /**
     * main方法+Spring模式 - 使用Spring容器但不启动Web服务器
     */
    private static void runSpringConsoleMode(String[] args) {
        try {

            // 创建SpringApplication实例
            SpringApplication app = new SpringApplication(Application.class);
            // 禁用Web环境，不启动Web服务器
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
            // 启动Spring容器
            ConfigurableApplicationContext context = app.run(args);
            // 从Spring容器获取服务实例
            ClientMonitorService clientMonitorService = context.getBean(ClientMonitorService.class);
            // 服务已经通过@PostConstruct自动启动
            log.info("LOL工具Spring控制台模式运行中，按 Ctrl+C 退出");

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("正在关闭Spring控制台模式...");
                context.close();
                log.info("Spring控制台模式已关闭");
            }));

            // 阻塞主线程，保持程序运行
            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("Spring控制台模式执行过程中发生错误", e);
            System.exit(1);
        }
    }

    /**
     * Web模式 - 启动完整的SpringBoot应用
     */
    private static void runWebMode(String[] args) {
        try {
            log.info("正在启动SpringBoot应用...");
            ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
            log.info("SpringBoot应用启动成功");
        } catch (Exception e) {
            log.error("SpringBoot应用启动失败", e);
            System.exit(1);
        }
    }
}