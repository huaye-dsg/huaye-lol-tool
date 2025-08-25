package com.example.huayeloltool;

import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.score.ScoreService;
import com.example.huayeloltool.service.ClientMonitorService;
import com.example.huayeloltool.service.ChampionSelectHandler;
import com.example.huayeloltool.service.GameFlowHandler;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Field;

@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        // 检查是否包含控制台模式参数
        boolean consoleMode = false;
        for (String arg : args) {
            if ("--console".equals(arg) || "--console-mode".equals(arg)) {
                consoleMode = true;
                break;
            }
        }

        if (consoleMode) {
            log.info("=== 控制台模式启动 ===");
            runConsoleMode();
        } else {
            log.info("=== SpringBoot Web模式启动 ===");
            runWebMode(args);
        }
    }

    /**
     * 控制台模式 - 直接执行业务逻辑，不启动SpringBoot容器
     */
    private static void runConsoleMode() {
        try {
            log.info("开始执行LOL工具核心功能...");

            // 创建核心服务实例（手动创建，不依赖Spring容器）
            ConsoleRunner runner = new ConsoleRunner();
            runner.start();

            // 保持程序运行
            log.info("LOL工具控制台模式运行中，按 Ctrl+C 退出");

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("正在关闭控制台模式...");
                runner.stop();
                log.info("控制台模式已关闭");
            }));

            // 阻塞主线程
            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("执行过程中发生错误", e);
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

    /**
     * 控制台模式运行器
     */
    private static class ConsoleRunner {
        private Monitor monitor;
        private ClientMonitorService clientMonitorService;

        public void start() {
            try {
                log.info("初始化LOL工具核心服务...");

                // 手动创建服务实例并设置依赖关系
                LcuApiService lcuApiService = new LcuApiService();
                ScoreService scoreService = new ScoreService();
                GameGlobalSetting gameGlobalSetting = new GameGlobalSetting();

                GameFlowHandler gameFlowHandler = new GameFlowHandler();
                ChampionSelectHandler championSelectHandler = new ChampionSelectHandler();

                // 使用反射设置@Autowired字段
                setField(gameFlowHandler, "lcuApiService", lcuApiService);
                setField(gameFlowHandler, "scoreService", scoreService);

                setField(championSelectHandler, "lcuApiService", lcuApiService);
                setField(championSelectHandler, "clientCfg", gameGlobalSetting);

                monitor = new Monitor();
                setField(monitor, "gameFlowHandler", gameFlowHandler);
                setField(monitor, "championSelectHandler", championSelectHandler);

                // 创建并启动客户端监控服务（复用现有实现）
                clientMonitorService = new ClientMonitorService();
                setField(clientMonitorService, "monitor", monitor);
                setField(clientMonitorService, "lcuApiService", lcuApiService);

                // 手动调用启动方法（模拟@PostConstruct）
                clientMonitorService.startMonitoring();

                log.info("LOL工具核心服务启动完成");
            } catch (Exception e) {
                log.error("初始化服务失败", e);
                throw new RuntimeException(e);
            }
        }

        public void stop() {
            log.info("正在停止LOL工具核心服务...");

            if (clientMonitorService != null) {
                clientMonitorService.stopMonitoring();
            }

            if (monitor != null) {
                monitor.shutdown();
            }

            log.info("LOL工具核心服务已停止");
        }

        /**
         * 使用反射设置私有字段（模拟Spring的@Autowired）
         */
        private void setField(Object target, String fieldName, Object value) throws Exception {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}