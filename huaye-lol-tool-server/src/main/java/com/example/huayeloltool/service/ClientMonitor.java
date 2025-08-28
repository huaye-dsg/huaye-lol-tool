package com.example.huayeloltool.service;

import com.example.huayeloltool.common.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.summoner.Summoner;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LOL客户端统一服务 - 事件驱动版本
 * 采用事件驱动模式，减少不必要的资源消耗
 * <p>
 * 主要功能：
 * - LOL客户端进程监控（仅在断开时进行）
 * - WebSocket连接管理
 * - 游戏事件处理
 * - 智能重连机制
 * <p>
 * 工作模式：
 * - DISCONNECTED: 未连接状态，周期性检查LOL进程
 * - CONNECTED: 已连接状态，停止检查，依赖WebSocket事件感知断开
 * - RECONNECTING: 重连状态，快速检查直到连接成功
 */
@Slf4j
@Service
public class ClientMonitor {

    @Autowired
    private LcuApiService lcuApiService;
    @Autowired
    private MessageRouter messageRouter;

    @Resource(name = "scheduledExecutor")
    private ScheduledExecutorService scheduledExecutor;

    // WebSocket相关
    private static final OkHttpClient client = OkHttpUtil.getInstance();
    private volatile WebSocket webSocket;

    /**
     * 连接状态枚举
     */
    public enum ConnectionState {
        DISCONNECTED,   // 未连接，需要周期性检查
        CONNECTED,      // 已连接，停止检查
        RECONNECTING    // 重连中，快速检查
    }

    // 状态管理
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // 连接参数管理
    private final Object connectionLock = new Object();
    private volatile int currentPort = 0;
    private volatile String currentToken = "";

    // 调度任务管理
    private volatile ScheduledFuture<?> monitoringTask;

    // 监控配置
    private static final long DISCONNECTED_CHECK_INTERVAL = 10; // 断开状态检查间隔（秒）
    private static final long RECONNECTING_CHECK_INTERVAL = 3;  // 重连状态检查间隔（秒）

    // 系统信息
    private volatile SystemInfo systemInfo;

    /**
     * 启动LOL客户端服务
     */
    @PostConstruct
    public void startService() {
        if (isMonitoring.compareAndSet(false, true)) {
            log.info("LOL客户端监控服务启动 - 事件驱动模式");

            // 初始化系统信息
            systemInfo = new SystemInfo();

            // 设置初始状态为断开
            connectionState = ConnectionState.DISCONNECTED;

            // 开始监控
            startMonitoring();
        }
    }

    /**
     * 停止LOL客户端服务
     */
    @PreDestroy
    public void stopService() {
        log.info("停止LOL客户端服务...");
        isShutdown.set(true);
        isMonitoring.set(false);

        // 停止监控任务
        stopMonitoring();

        // 清理资源
        closeWebSocket();
        clearConnectionState();

        log.info("LOL客户端服务已停止");
    }

    /**
     * 开始监控
     */
    private void startMonitoring() {
        if (isShutdown.get()) {
            return;
        }

        // 立即执行一次检查
        checkClientConnection();

        // 根据当前状态调度下次检查
        scheduleNextCheck();
    }

    /**
     * 停止监控
     */
    private void stopMonitoring() {
        ScheduledFuture<?> task = monitoringTask;
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            monitoringTask = null;
        }
    }

    /**
     * 调度下次检查
     */
    private void scheduleNextCheck() {
        if (isShutdown.get()) {
            return;
        }

        // 根据连接状态决定检查间隔
        long interval = getCheckInterval();

        // 如果已连接，则不需要调度检查
        if (connectionState == ConnectionState.CONNECTED) {
            log.info("客户端已连接，停止周期性检查，等待断开事件");
            return;
        }

        monitoringTask = scheduledExecutor.schedule(() -> {
            if (!isShutdown.get()) {
                checkClientConnection();
                scheduleNextCheck();
            }
        }, interval, TimeUnit.SECONDS);

        log.info("已调度下次检查，间隔: {}秒，当前状态: {}", interval, connectionState);
    }

    /**
     * 获取检查间隔
     */
    private long getCheckInterval() {
        return switch (connectionState) {
            case DISCONNECTED -> DISCONNECTED_CHECK_INTERVAL;
            case RECONNECTING -> RECONNECTING_CHECK_INTERVAL;
            case CONNECTED -> Long.MAX_VALUE; // 已连接时不需要检查
        };
    }

    /**
     * 检查客户端连接
     */
    private void checkClientConnection() {
        try {
            log.info("检查LOL客户端连接，当前状态: {}", connectionState);

            // 查找LOL客户端进程
            Pair<Integer, String> clientInfo = findLolClientInfo();
            int port = clientInfo.getLeft();
            String token = clientInfo.getRight();

            if (port <= 0 || token.isEmpty()) {
                handleClientNotFound();
                return;
            }

            // 检查连接参数是否变化
            boolean connectionChanged = checkConnectionChange(port, token);

            if (connectionState == ConnectionState.DISCONNECTED ||
                    connectionState == ConnectionState.RECONNECTING ||
                    connectionChanged) {

                if (connectionChanged && connectionState == ConnectionState.CONNECTED) {
                    log.info("检测到LOL客户端参数变化 (端口: {} -> {})", currentPort, port);
                }

                handleClientFound(port, token);
            }

        } catch (Exception e) {
            log.warn("检查连接时发生错误: {}", e.getMessage());
        }
    }

    /**
     * 处理未找到客户端
     */
    private void handleClientNotFound() {
        if (connectionState == ConnectionState.CONNECTED) {
            log.info("LOL客户端进程已消失，切换到重连模式");
            transitionToReconnecting();
        } else {
            log.debug("LOL客户端进程不存在");
        }
    }

    /**
     * 处理找到客户端
     */
    private void handleClientFound(int port, String token) {
        synchronized (connectionLock) {
            log.info("检测到LOL客户端，端口: {}", port);

            // 更新连接参数
            currentPort = port;
            currentToken = token;

            // 设置BaseUrlClient
            BaseUrlClient.getInstance().setPort(port);
            BaseUrlClient.getInstance().setToken(token);
        }

        // 异步初始化召唤师信息
        initializeSummonerInfo()
                .thenAccept(success -> {
                    if (success) {
                        handleConnectionSuccess();
                    } else {
                        log.warn("召唤师信息初始化失败");
                    }
                })
                .exceptionally(throwable -> {
                    log.error("初始化召唤师信息时发生错误", throwable);
                    return null;
                });
    }

    /**
     * 处理连接成功
     */
    private void handleConnectionSuccess() {
        log.info("LOL客户端连接成功");

        // 启动WebSocket连接
        startWebSocketConnection();

        // 切换到已连接状态（这会停止周期性检查）
        transitionToConnected();
    }

    /**
     * 切换到已连接状态
     */
    private void transitionToConnected() {
        ConnectionState oldState = connectionState;
        connectionState = ConnectionState.CONNECTED;

        // 停止当前的监控任务
        stopMonitoring();

        log.info("状态切换: {} -> CONNECTED，停止周期性检查", oldState);
    }

    /**
     * 切换到重连状态
     */
    private void transitionToReconnecting() {
        ConnectionState oldState = connectionState;
        connectionState = ConnectionState.RECONNECTING;

        // 清理连接状态
        clearConnectionState();

        // 重新开始监控
        if (oldState == ConnectionState.CONNECTED) {
            startMonitoring();
        }

        log.info("状态切换: {} -> RECONNECTING，开始快速检查", oldState);
    }

    /**
     * 切换到断开状态
     */
    private void transitionToDisconnected() {
        ConnectionState oldState = connectionState;
        connectionState = ConnectionState.DISCONNECTED;

        // 清理连接状态
        clearConnectionState();

        // 重新开始监控
        if (oldState == ConnectionState.CONNECTED) {
            startMonitoring();
        }

        log.info("状态切换: {} -> DISCONNECTED，开始正常检查", oldState);
    }

    /**
     * 查找LOL客户端信息 - 简化版本，移除缓存机制
     */
    private Pair<Integer, String> findLolClientInfo() {
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            List<OSProcess> processes = os.getProcesses();

            return processes.parallelStream()
                    .filter(process -> Constant.LOL_UX_PROCESS_NAME.equalsIgnoreCase(process.getName()))
                    .findFirst()
                    .map(this::extractClientInfo)
                    .orElse(Pair.of(0, ""));

        } catch (SecurityException e) {
            log.warn("权限不足，无法访问系统进程信息: {}", e.getMessage());
            return Pair.of(0, "");
        } catch (Exception e) {
            log.warn("查找LOL进程时发生错误: {}", e.getMessage());
            return Pair.of(0, "");
        }
    }

    /**
     * 从进程中提取客户端信息
     */
    private Pair<Integer, String> extractClientInfo(OSProcess process) {
        try {
            List<String> arguments = process.getArguments();
            int port = 0;
            String token = "";

            for (String argument : arguments) {
                if (argument.contains("--app-port")) {
                    String[] split = argument.split("=");
                    if (split.length > 1) {
                        try {
                            port = Integer.parseInt(split[1]);
                        } catch (NumberFormatException e) {
                            log.warn("解析端口号失败: {}", split[1]);
                        }
                    }
                } else if (argument.contains("--remoting-auth-token")) {
                    String[] split = argument.split("=");
                    if (split.length > 1) {
                        token = split[1];
                    }
                }

                if (port > 0 && !token.isEmpty()) {
                    return Pair.of(port, token);
                }
            }

            return Pair.of(port, token);

        } catch (Exception e) {
            log.warn("提取进程参数失败: {}", e.getMessage());
            return Pair.of(0, "");
        }
    }

    /**
     * 检查连接参数是否变化
     */
    private boolean checkConnectionChange(int port, String token) {
        synchronized (connectionLock) {
            return currentPort != port || !Objects.equals(currentToken, token);
        }
    }

    /**
     * 清理连接状态
     */
    private void clearConnectionState() {
        closeWebSocket();
        synchronized (connectionLock) {
            currentPort = 0;
            currentToken = "";
        }
    }

    /**
     * 异步初始化召唤师信息
     */
    private CompletableFuture<Boolean> initializeSummonerInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Summoner summoner = lcuApiService.getCurrSummoner();
                if (summoner != null) {
                    Summoner.setInstance(summoner);
                    log.info("召唤师信息获取成功");
                    return true;
                }
                log.warn("召唤师信息为空");
                return false;
            } catch (Exception e) {
                log.warn("获取召唤师信息失败: {}", e.getMessage());
                return false;
            }
        }, scheduledExecutor);
    }

    /**
     * 启动WebSocket连接
     */
    private void startWebSocketConnection() {
        if (isShutdown.get()) {
            return;
        }

        synchronized (connectionLock) {
            if (currentPort <= 0 || StringUtils.isBlank(currentToken)) {
                log.error("无效的连接参数");
                return;
            }

            try {
                String auth = Base64.getEncoder().encodeToString(("riot:" + currentToken).getBytes());

                Request request = new Request.Builder()
                        .url("wss://127.0.0.1:" + currentPort + "/")
                        .addHeader("Authorization", "Basic " + auth)
                        .build();

                webSocket = client.newWebSocket(request, new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        log.info("WebSocket连接已建立");
                        webSocket.send("[5, \"OnJsonApiEvent\"]");
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        handleWebSocketMessage(text);
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        log.info("WebSocket连接正在关闭: code={}, reason={}", code, reason);
                    }

                    @Override
                    public void onClosed(WebSocket webSocket, int code, String reason) {
                        log.info("WebSocket连接已关闭: code={}, reason={}", code, reason);
                        handleWebSocketDisconnected();
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        log.error("WebSocket连接失败: {}", t.getMessage());
                        handleWebSocketDisconnected();
                    }
                });

            } catch (Exception e) {
                log.error("启动WebSocket连接失败", e);
                handleWebSocketDisconnected();
            }
        }
    }

    /**
     * 处理WebSocket断开连接 - 关键的事件驱动入口
     */
    private void handleWebSocketDisconnected() {
        if (webSocket != null) {
            webSocket = null;
        }

        if (!isShutdown.get()) {
            log.info("WebSocket断开，触发重连检查");

            // 切换到重连状态，这会重新开始周期性检查
            transitionToReconnecting();
        }
    }

    /**
     * 关闭WebSocket连接
     */
    private void closeWebSocket() {
        WebSocket currentWebSocket = this.webSocket;
        if (currentWebSocket != null) {
            this.webSocket = null;
            try {
                boolean closed = currentWebSocket.close(1000, "正常关闭");
                if (!closed) {
                    scheduledExecutor.schedule(() -> {
                        try {
                            currentWebSocket.cancel();
                        } catch (Exception e) {
                            log.warn("强制关闭WebSocket时发生错误: {}", e.getMessage());
                        }
                    }, 3, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("关闭WebSocket时发生错误: {}", e.getMessage());
                try {
                    currentWebSocket.cancel();
                } catch (Exception cancelEx) {
                    log.warn("强制关闭WebSocket失败: {}", cancelEx.getMessage());
                }
            }
        }
    }

    /**
     * 处理WebSocket消息
     */
    private void handleWebSocketMessage(String message) {
        try {
            messageRouter.routeMessage(message);
        } catch (Exception e) {
            log.error("WebSocket消息路由失败", e);
        }
    }

    // ========== 公共API方法 ==========

    /**
     * 手动重连
     */
    public boolean manualReconnect() {
        log.info("手动触发重连...");

        // 切换到重连状态
        transitionToReconnecting();

        return true;
    }

    /**
     * 获取客户端连接状态
     */
    public boolean isClientConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    /**
     * 获取WebSocket连接状态
     */
    public boolean isWebSocketConnected() {
        return webSocket != null;
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        synchronized (connectionLock) {
            if (connectionState == ConnectionState.CONNECTED) {
                return String.format("LOL客户端已连接 (端口: %d, WebSocket: %s, 状态: %s)",
                        currentPort,
                        isWebSocketConnected() ? "已连接" : "未连接",
                        connectionState);
            } else {
                return String.format("LOL客户端未连接 (状态: %s)",
                        connectionState);
            }
        }
    }
}