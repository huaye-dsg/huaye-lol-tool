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
    // **重要提示**: OkHttpUtil.getInstance() 返回的OkHttpClient实例
    // 必须经过特殊配置以信任LOL客户端的自签名SSL证书。
    // 否则，所有HTTPS请求（包括WebSocket）都会失败。
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
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // 连接参数管理
    private final Object connectionLock = new Object();
    private volatile int currentPort = 0;
    private volatile String currentToken = "";

    // 调度任务管理
    private volatile ScheduledFuture<?> monitoringTask;

    // 监控配置
    private static final long DISCONNECTED_CHECK_INTERVAL = 10; // 断开状态检查间隔（秒）
    private static final long RECONNECTING_CHECK_INTERVAL = 5;  // 重连状态检查间隔（秒）

    // 系统信息
    private volatile SystemInfo systemInfo;

    /**
     * 启动LOL客户端服务
     */
    @PostConstruct
    public void startService() {
        log.info("LOL客户端监控服务启动 - 事件驱动模式");

        // 初始化系统信息
        systemInfo = new SystemInfo();

        // 设置初始状态为断开并开始监控
        transitionToState(ConnectionState.DISCONNECTED, "服务启动");
    }

    /**
     * 停止LOL客户端服务
     */
    @PreDestroy
    public void stopService() {
        log.info("正在停止LOL客户端服务...");
        isShutdown.set(true);

        stopMonitoring();
        closeWebSocket();
        clearConnectionState();

        log.info("LOL客户端服务已成功停止");
    }

    /**
     * 统一的状态转换方法，确保线程安全和逻辑清晰
     *
     * @param newState 目标状态
     * @param reason   转换原因
     */
    private synchronized void transitionToState(ConnectionState newState, String reason) {
//        if (connectionState == newState) {
//            return;
//        }

        ConnectionState oldState = connectionState;
        connectionState = newState;
//        log.info("状态切换: {} -> {}, 原因: {}", oldState, newState, reason);

        // 停止任何正在运行的监控任务
        stopMonitoring();

        // 根据新状态决定下一步操作
        switch (newState) {
            case CONNECTED:
                // 已连接，不需要做任何事，等待事件触发
//                log.info("客户端已连接，停止周期性检查，等待断开事件");
                break;
            case RECONNECTING:
            case DISCONNECTED:
                // 断开或重连时，清理旧连接并开始监控
                clearConnectionState();
                startMonitoring();
                break;
        }
    }

    /**
     * 开始监控
     */
    private void startMonitoring() {
        if (isShutdown.get() || connectionState == ConnectionState.CONNECTED) {
            return;
        }
        // 立即执行一次检查，然后根据结果调度下一次
        scheduledExecutor.execute(this::checkClientConnection);
    }

    /**
     * 停止监控
     */
    private void stopMonitoring() {
        if (monitoringTask != null && !monitoringTask.isDone()) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
    }

    /**
     * 调度下次检查
     */
    private void scheduleNextCheck() {
        if (isShutdown.get() || connectionState == ConnectionState.CONNECTED) {
            return;
        }
        long interval = (connectionState == ConnectionState.RECONNECTING)
                ? RECONNECTING_CHECK_INTERVAL
                : DISCONNECTED_CHECK_INTERVAL;

        log.debug("已调度下次检查，间隔: {}秒，当前状态: {}", interval, connectionState);
        monitoringTask = scheduledExecutor.schedule(this::checkClientConnection, interval, TimeUnit.SECONDS);
    }

    /**
     * 检查客户端连接
     */
    private void checkClientConnection() {
        if (connectionState == ConnectionState.CONNECTED) {
            return;
        }

        try {
            log.debug("检查LOL客户端连接，当前状态: {}", connectionState);

            Pair<Integer, String> clientInfo = findLolClientInfo();
            if (clientInfo.getLeft() <= 0 || clientInfo.getRight().isEmpty()) {
                if (connectionState == ConnectionState.RECONNECTING) {
                    log.info("LOL客户端进程已消失，切换到断开模式");
                    transitionToState(ConnectionState.DISCONNECTED, "客户端进程消失");
                }
                scheduleNextCheck(); // 无论如何都继续调度下一次检查
                return;
            }

            // 检查连接参数是否变化
            boolean connectionChanged;
            synchronized (connectionLock) {
                connectionChanged = currentPort != clientInfo.getLeft() || !Objects.equals(currentToken, clientInfo.getRight());
            }

            if (connectionChanged) {
//                log.info("检测到LOL客户端或其参数变化，尝试连接...");
                handleClientFound(clientInfo.getLeft(), clientInfo.getRight());
            } else {
                log.debug("客户端参数未变，等待下一次检查");
                scheduleNextCheck();
            }

        } catch (Exception e) {
            log.warn("检查连接时发生错误: {}", e.getMessage(), e);
            scheduleNextCheck(); // 出现异常也继续调度
        }
    }

    /**
     * 处理找到客户端后的连接逻辑
     */
    private void handleClientFound(int port, String token) {
        // 更新连接参数
        synchronized (connectionLock) {
            currentPort = port;
            currentToken = token;
            BaseUrlClient.getInstance().setPort(port);
            BaseUrlClient.getInstance().setToken(token);
        }

        // 异步初始化召唤师信息并建立连接
        initializeSummonerInfo().thenAcceptAsync(success -> {
            if (success) {
                log.info("LOL客户端 REST API 可访问，正在建立 WebSocket 连接...");
                startWebSocketConnection();
            } else {
                log.warn("召唤师信息初始化失败，将在下次检查时重试");
                scheduleNextCheck();
            }
        }, scheduledExecutor).exceptionally(throwable -> {
            log.error("初始化召唤师信息时发生严重错误", throwable);
            scheduleNextCheck();
            return null;
        });
    }

    /**
     * 查找LOL客户端信息
     */
    private Pair<Integer, String> findLolClientInfo() {
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            return os.getProcesses().stream()
                    .filter(p -> Constant.LOL_UX_PROCESS_NAME.equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .map(this::extractClientInfo)
                    .orElse(Pair.of(0, ""));
        } catch (Exception e) {
            log.warn("查找LOL进程时发生错误: {}", e.getMessage());
            return Pair.of(0, "");
        }
    }

    /**
     * 从进程中提取客户端信息
     */
    private Pair<Integer, String> extractClientInfo(OSProcess process) {
        int port = 0;
        String token = "";
        try {
            List<String> arguments = process.getArguments();
            for (String argument : arguments) {
                if (argument.startsWith("--app-port=")) {
                    port = Integer.parseInt(argument.substring("--app-port=".length()));
                } else if (argument.startsWith("--remoting-auth-token=")) {
                    token = argument.substring("--remoting-auth-token=".length());
                }
                if (port > 0 && !token.isEmpty()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("从进程 {} 提取参数失败: {}", process.getProcessID(), e.getMessage());
            return Pair.of(0, "");
        }
        return Pair.of(port, token);
    }

    /**
     * 清理连接状态
     */
    private void clearConnectionState() {
        synchronized (connectionLock) {
            currentPort = 0;
            currentToken = "";
        }
    }

    /**
     * 异步初始化召唤师信息
     * **重要提示**: LcuApiService 实现中必须确保关闭 Response Body,
     * 例如使用 try-with-resources 语句。
     */
    private CompletableFuture<Boolean> initializeSummonerInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Summoner summoner = lcuApiService.getCurrSummoner();
                if (summoner != null) {
                    Summoner.setInstance(summoner);
                    log.info("召唤师信息获取成功: {}", summoner.getGameName());
                    return true;
                }
                log.warn("获取到的召唤师信息为空");
                return false;
            } catch (Exception e) {
                // 在开发阶段，打印完整堆栈以诊断问题
                log.warn("获取召唤师信息失败", e);
                return false;
            }
        }, scheduledExecutor);
    }

    /**
     * 启动WebSocket连接
     */
    private void startWebSocketConnection() {
        if (isShutdown.get()) return;

        // 在创建新连接前，确保旧的已完全关闭
        closeWebSocket();

        String localToken;
        int localPort;
        synchronized (connectionLock) {
            localPort = currentPort;
            localToken = currentToken;
        }

        if (localPort <= 0 || StringUtils.isBlank(localToken)) {
            log.error("无效的连接参数，无法启动WebSocket");
            transitionToState(ConnectionState.RECONNECTING, "无效的连接参数");
            return;
        }

        try {
            String auth = Base64.getEncoder().encodeToString(("riot:" + localToken).getBytes());
            Request request = new Request.Builder()
                    .url("wss://127.0.0.1:" + localPort + "/")
                    .addHeader("Authorization", "Basic " + auth)
                    .build();

            // 创建新的WebSocket连接
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, Response response) {
                    log.info("WebSocket 连接已成功建立！");
                    ws.send("[5, \"OnJsonApiEvent\"]"); // 订阅所有事件
                    // 只有在WebSocket成功打开后，才真正进入CONNECTED状态
                    transitionToState(ConnectionState.CONNECTED, "WebSocket 连接成功");
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    messageRouter.routeMessage(text);
                }

                @Override
                public void onClosing(WebSocket ws, int code, String reason) {
                    log.info("WebSocket 连接正在关闭: code={}, reason={}", code, reason);
                }

                @Override
                public void onClosed(WebSocket ws, int code, String reason) {
                    log.info("WebSocket 连接已关闭: code={}, reason={}", code, reason);
                    // 只有在非程序主动关闭时才触发重连
                    if (!isShutdown.get()) {
                        transitionToState(ConnectionState.RECONNECTING, "WebSocket 主动关闭");
                    }
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, Response response) {
                    // **关键修改**：使用 log.error(msg, t) 来打印完整的异常堆栈，这对于诊断SSL问题至关重要
                    log.error("WebSocket 连接失败", t);
                    if (!isShutdown.get()) {
                        transitionToState(ConnectionState.RECONNECTING, "WebSocket 连接失败");
                    }
                }
            });
        } catch (Exception e) {
            log.error("启动WebSocket连接时发生同步异常", e);
            if (!isShutdown.get()) {
                transitionToState(ConnectionState.RECONNECTING, "WebSocket 启动异常");
            }
        }
    }

    /**
     * 关闭WebSocket连接
     */
    private void closeWebSocket() {
        if (webSocket != null) {
            WebSocket oldSocket = webSocket;
            webSocket = null; // 立即置空，避免重复关闭
            try {
                // 1000是正常关闭代码
                oldSocket.close(1000, "Client shutdown or reconnection");
            } catch (Exception e) {
                log.warn("关闭WebSocket时发生错误，将强制取消: {}", e.getMessage());
                try {
                    oldSocket.cancel(); // 作为最后的手段
                } catch (Exception cancelEx) {
                    log.warn("强制关闭WebSocket也失败了", cancelEx);
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
    public void manualReconnect() {
        log.info("手动触发重连...");
        transitionToState(ConnectionState.RECONNECTING, "手动触发");
    }

    /**
     * 获取客户端连接状态
     */
    public boolean isClientConnected() {
        return connectionState == ConnectionState.CONNECTED && webSocket != null;
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        synchronized (connectionLock) {
            return String.format("客户端状态: %s, WebSocket: %s, 端口: %d",
                    connectionState,
                    (webSocket != null ? "已连接" : "未连接"),
                    currentPort);
        }
    }
}