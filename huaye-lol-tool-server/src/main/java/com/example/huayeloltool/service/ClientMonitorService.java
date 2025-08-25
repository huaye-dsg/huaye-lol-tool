package com.example.huayeloltool.service;

import com.example.huayeloltool.Monitor;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.summoner.Summoner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LOL客户端监控服务（优化版）
 * 负责持续监控LOL客户端状态，自动重连和初始化
 * <p>
 * 优化特性：
 * - 智能检查频率调整
 * - 进程查找缓存
 * - 减少不必要的API调用
 * - 资源消耗最小化
 */
@Slf4j
@Service
public class ClientMonitorService {

    @Autowired
    private Monitor monitor;

    @Autowired
    private LcuApiService lcuApiService;

    // 使用单线程池，减少资源消耗
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LOL-Client-Monitor");
        t.setDaemon(true); // 设置为守护线程
        return t;
    });

    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean isClientConnected = new AtomicBoolean(false);
    private final AtomicBoolean isWebSocketConnected = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // 当前连接信息
    private volatile int currentPort = 0;
    private volatile String currentToken = "";

    // 优化：智能检查频率
    private volatile long normalCheckInterval = 15; // 正常情况下15秒检查一次（降低频率）
    private volatile long fastCheckInterval = 5;    // 连接异常时5秒检查一次
    private volatile long currentCheckInterval = fastCheckInterval;

    // 优化：缓存和计数器
    private final AtomicLong lastSuccessfulCheck = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    private volatile SystemInfo systemInfo; // 缓存SystemInfo对象

    // 优化：减少WebSocket检查频率
    private static final long WEBSOCKET_CHECK_INTERVAL = 30; // 30秒检查一次WebSocket

    /**
     * 启动监控服务
     */
    @PostConstruct
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            log.info("启动LOL客户端监控服务（优化版）...");

            // 初始化SystemInfo（一次性创建，避免重复创建）
            systemInfo = new SystemInfo();

            // 立即检查一次
            checkAndInitializeClient();

            // 使用动态间隔的单一定时任务
            scheduleNextCheck();
        }
    }

    /**
     * 智能调度下次检查
     */
    private void scheduleNextCheck() {
        if (isShutdown.get()) {
            return;
        }

        scheduler.schedule(() -> {
            if (!isShutdown.get()) {
                // 执行检查
                checkAndInitializeClient();

                // 每5次客户端检查才检查一次WebSocket（减少检查频率）
                if (System.currentTimeMillis() % (WEBSOCKET_CHECK_INTERVAL * 1000) < currentCheckInterval * 1000) {
                    checkWebSocketConnection();
                }

                // 调度下次检查
                scheduleNextCheck();
            }
        }, currentCheckInterval, TimeUnit.SECONDS);
    }

    /**
     * 停止监控服务
     */
    @PreDestroy
    public void stopMonitoring() {
        log.info("停止LOL客户端监控服务...");
        isShutdown.set(true);
        isMonitoring.set(false);

        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }

    /**
     * 检查并初始化客户端连接（优化版）
     */
    private void checkAndInitializeClient() {
        if (isShutdown.get()) {
            return;
        }

        try {
            // 优化：快速检查LOL客户端进程
            Pair<Integer, String> apiInfo = getLolClientApiInfoOptimized(Constant.LOL_UX_PROCESS_NAME);
            int port = apiInfo.getLeft();
            String token = apiInfo.getRight();

            if (port <= 0 || token.isEmpty()) {
                // LOL客户端不存在
                handleClientDisconnection();
                return;
            }

            // 检查连接信息是否发生变化
            boolean connectionChanged = (currentPort != port || !Objects.equals(currentToken, token));

            if (!isClientConnected.get() || connectionChanged) {
                log.info("检测到LOL客户端，端口: {}, 连接信息{}", port, connectionChanged ? "已更新" : "");

                // 更新连接信息
                currentPort = port;
                currentToken = token;

                // 设置BaseUrlClient
                BaseUrlClient instance = BaseUrlClient.getInstance();
                instance.setPort(port);
                instance.setToken(token);

                // 优化：延迟初始化召唤师信息（避免客户端未完全启动）
                scheduler.schedule(() -> {
                    if (initializeSummonerInfoWithRetry()) {
                        isClientConnected.set(true);
                        consecutiveFailures.set(0);
                        lastSuccessfulCheck.set(System.currentTimeMillis());

                        // 连接成功后切换到正常检查频率
                        currentCheckInterval = normalCheckInterval;

                        log.info("LOL客户端连接成功，召唤师信息已初始化");

                        // 启动WebSocket监控
                        startWebSocketMonitoring();
                    }
                }, 2, TimeUnit.SECONDS);
            } else {
                // 连接正常，更新成功时间
                lastSuccessfulCheck.set(System.currentTimeMillis());
                consecutiveFailures.set(0);
            }

        } catch (Exception e) {
            log.debug("检查客户端连接时发生错误: {}", e.getMessage());
            handleCheckFailure();
        }
    }

    /**
     * 处理客户端断开连接
     */
    private void handleClientDisconnection() {
        if (isClientConnected.get()) {
            log.info("LOL客户端已断开连接");
            isClientConnected.set(false);
            isWebSocketConnected.set(false);
            monitor.closeWebSocket();
        }
        handleCheckFailure();
    }

    /**
     * 处理检查失败
     */
    private void handleCheckFailure() {
        long failures = consecutiveFailures.incrementAndGet();

        // 连接失败时使用快速检查频率
        currentCheckInterval = fastCheckInterval;

        // 如果连续失败次数过多，适当增加检查间隔（避免过度消耗资源）
        if (failures > 10) {
            currentCheckInterval = Math.min(normalCheckInterval, fastCheckInterval * 2);
        }
    }

    /**
     * 优化的进程查找方法
     */
    private Pair<Integer, String> getLolClientApiInfoOptimized(String processName) {
        try {
            // 使用缓存的SystemInfo对象
            OperatingSystem os = systemInfo.getOperatingSystem();
            List<OSProcess> processes = os.getProcesses();

            // 优化：提前退出循环
            for (OSProcess process : processes) {
                if (processName.equalsIgnoreCase(process.getName())) {
                    List<String> arguments = process.getArguments();
                    int port = 0;
                    String token = "";

                    // 优化：找到目标参数后立即退出
                    for (String argument : arguments) {
                        if (argument.contains("--app-port")) {
                            String[] split = argument.split("=");
                            if (split.length > 1) {
                                try {
                                    port = Integer.parseInt(split[1]);
                                } catch (NumberFormatException e) {
                                    log.debug("解析端口号失败: {}", split[1]);
                                }
                            }
                        } else if (argument.contains("--remoting-auth-token")) {
                            String[] split = argument.split("=");
                            if (split.length > 1) {
                                token = split[1];
                            }
                        }

                        // 如果两个参数都找到了，提前退出
                        if (port > 0 && !token.isEmpty()) {
                            break;
                        }
                    }

                    if (port > 0 && !token.isEmpty()) {
                        return Pair.of(port, token);
                    }

                    // 找到进程但参数不完整，继续查找其他同名进程
                }
            }
        } catch (Exception e) {
            log.debug("查找LOL进程时发生错误: {}", e.getMessage());
        }

        return Pair.of(0, "");
    }

    /**
     * 带重试的召唤师信息初始化
     */
    private boolean initializeSummonerInfoWithRetry() {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Summoner summoner = lcuApiService.getCurrSummoner();
                if (summoner != null) {
                    Summoner.setInstance(summoner);
                    return true;
                }
            } catch (Exception e) {
                log.debug("获取召唤师信息失败 (尝试 {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(1000); // 等待1秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 启动WebSocket监控
     */
    private void startWebSocketMonitoring() {
        if (!isWebSocketConnected.get()) {
            try {
                // 关闭旧连接
                monitor.closeWebSocket();

                // 等待一下再启动新连接
                Thread.sleep(1000);

                // 启动新的WebSocket连接
                monitor.startGameFlowMonitor();
                isWebSocketConnected.set(true);
                log.info("WebSocket监控已启动");

            } catch (Exception e) {
                log.error("启动WebSocket监控失败", e);
            }
        }
    }

    /**
     * 检查WebSocket连接状态（降低频率）
     */
    private void checkWebSocketConnection() {
        if (isShutdown.get() || !isClientConnected.get()) {
            return;
        }

        try {
            // 检查WebSocket是否还连接着
            if (!monitor.isConnected()) {
                log.info("检测到WebSocket连接断开，尝试重新连接...");
                isWebSocketConnected.set(false);
                startWebSocketMonitoring();
            }
        } catch (Exception e) {
            log.debug("检查WebSocket连接状态时发生错误: {}", e.getMessage());
        }
    }

    /**
     * 手动触发重连
     */
    public boolean manualReconnect() {
        log.info("手动触发重连...");

        // 重置状态
        isClientConnected.set(false);
        isWebSocketConnected.set(false);
        consecutiveFailures.set(0);
        currentCheckInterval = fastCheckInterval; // 重连时使用快速检查

        // 关闭现有连接
        monitor.closeWebSocket();

        // 立即检查并重连
        checkAndInitializeClient();

        // 等待一下检查结果
        try {
            Thread.sleep(3000); // 增加等待时间，确保连接稳定
            return isClientConnected.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 获取连接状态
     */
    public boolean isClientConnected() {
        return isClientConnected.get();
    }

    /**
     * 获取WebSocket连接状态
     */
    public boolean isWebSocketConnected() {
        return isWebSocketConnected.get() && monitor.isConnected();
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        if (isClientConnected.get()) {
            long timeSinceLastCheck = System.currentTimeMillis() - lastSuccessfulCheck.get();
            return String.format("LOL客户端已连接 (端口: %d, WebSocket: %s, 检查间隔: %ds, 上次成功: %ds前)",
                    currentPort,
                    isWebSocketConnected() ? "已连接" : "未连接",
                    currentCheckInterval,
                    timeSinceLastCheck / 1000);
        } else {
            return String.format("LOL客户端未连接 (连续失败: %d次, 检查间隔: %ds)",
                    consecutiveFailures.get(), currentCheckInterval);
        }
    }
}