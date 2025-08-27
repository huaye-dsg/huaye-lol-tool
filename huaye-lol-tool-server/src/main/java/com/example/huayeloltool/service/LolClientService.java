package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LOL客户端统一服务
 * 整合了客户端监控和事件监听功能，提供统一的LOL客户端管理
 * <p>
 * 主要功能：
 * - LOL客户端进程监控
 * - WebSocket连接管理
 * - 游戏事件处理
 * - 自动重连机制
 */
@Slf4j
@Service
public class LolClientService {

    @Autowired
    private GameFlowHandler gameFlowHandler;
    @Autowired
    private ChampionSelectHandler championSelectHandler;
    @Autowired
    private LcuApiService lcuApiService;

    // 线程池配置
    @Resource(name = "webSocketMessageExecutor")
    private ExecutorService webSocketMessageExecutor;
    @Resource(name = "scheduledExecutor")
    private ScheduledExecutorService scheduledExecutor;

    // WebSocket相关
    private static final OkHttpClient client = OkHttpUtil.getInstance();
    private volatile WebSocket webSocket;

    // 状态管理 - 统一管理所有状态
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean isClientConnected = new AtomicBoolean(false);
    private final AtomicBoolean isWebSocketConnected = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // 连接参数管理
    private final Object connectionLock = new Object();
    private volatile int currentPort = 0;
    private volatile String currentToken = "";

    // 监控配置
    private final long normalCheckInterval = 15; // 正常检查间隔（秒）
    private final long fastCheckInterval = 5;    // 快速检查间隔（秒）
    private volatile long currentCheckInterval = fastCheckInterval;

    // 重连控制
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long BASE_RECONNECT_DELAY = 5;

    // 统计信息
    private final AtomicLong lastSuccessfulCheck = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    private volatile SystemInfo systemInfo;

    /**
     * 启动LOL客户端服务
     */
    @PostConstruct
    public void startService() {
        if (isMonitoring.compareAndSet(false, true)) {

            // 初始化系统信息
            systemInfo = new SystemInfo();

            // 初始化连接状态
            checkAndMaintainConnection();

            // 开始定期检查调度
            scheduleNextCheck();

            log.info("LOL客户端监控已启动，检查间隔: {}秒", currentCheckInterval);
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

        // 清理资源
        closeWebSocket();
        clearConnectionState();

        log.info("LOL客户端服务已停止");
    }

    /**
     * 智能调度下次检查
     */
    private void scheduleNextCheck() {
        if (isShutdown.get()) {
            return;
        }

        scheduledExecutor.schedule(() -> {
            if (!isShutdown.get()) {
                checkAndMaintainConnection();
                scheduleNextCheck();
            }
        }, currentCheckInterval, TimeUnit.SECONDS);
    }

    /**
     * 检查并维护连接
     */
    private void checkAndMaintainConnection() {
        try {
            // 1. 检查LOL客户端进程
            Pair<Integer, String> clientInfo = findLolClientInfo();
            int port = clientInfo.getLeft();
            String token = clientInfo.getRight();

            if (port <= 0 || token.isEmpty()) {
                log.info("未检测到LOL客户端");
                handleClientDisconnection();
                return;
            }

            // 2. 检查连接参数是否变化
            boolean connectionChanged = checkConnectionChange(port, token);

            // 3. 处理连接状态
            if (!isClientConnected.get() || connectionChanged) {
                handleClientConnection(port, token);
            } else {
                // 连接正常，更新统计信息
                updateSuccessStats();

                // 检查WebSocket状态
                if (isClientConnected.get() && !isWebSocketConnected.get()) {
                    startWebSocketConnection();
                }
            }

        } catch (Exception e) {
            log.debug("检查连接时发生错误: {}", e.getMessage());
            handleCheckFailure();
        }
    }

    /**
     * 查找LOL客户端信息
     */
    private Pair<Integer, String> findLolClientInfo() {
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            List<OSProcess> processes = os.getProcesses();

            for (OSProcess process : processes) {
                if (Constant.LOL_UX_PROCESS_NAME.equalsIgnoreCase(process.getName())) {
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
                                    log.debug("解析端口号失败: {}", split[1]);
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
                }
            }
        } catch (Exception e) {
            log.debug("查找LOL进程时发生错误: {}", e.getMessage());
        }

        return Pair.of(0, "");
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
     * 处理客户端连接
     */
    private void handleClientConnection(int port, String token) {
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
        initializeSummonerInfoAsync()
                .thenAccept(success -> {
                    if (success) {
                        isClientConnected.set(true);
                        updateSuccessStats();
                        currentCheckInterval = normalCheckInterval;

                        log.info("LOL客户端连接成功");

                        // 启动WebSocket连接
                        startWebSocketConnection();
                    } else {
                        log.warn("召唤师信息初始化失败");
                        handleCheckFailure();
                    }
                })
                .exceptionally(throwable -> {
                    log.error("初始化召唤师信息时发生错误", throwable);
                    handleCheckFailure();
                    return null;
                });
    }

    /**
     * 处理客户端断开连接
     */
    private void handleClientDisconnection() {
        if (isClientConnected.get()) {
            log.info("LOL客户端已断开连接");
            clearConnectionState();
        }
        handleCheckFailure();
    }

    /**
     * 清理连接状态
     */
    private void clearConnectionState() {
        isClientConnected.set(false);
        isWebSocketConnected.set(false);
        closeWebSocket();
    }

    /**
     * 异步初始化召唤师信息
     */
    private CompletableFuture<Boolean> initializeSummonerInfoAsync() {
        return CompletableFuture.supplyAsync(() -> {
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
                }

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            return false;
        }, scheduledExecutor);
    }

    /**
     * 启动WebSocket连接
     */
    private void startWebSocketConnection() {
        if (isShutdown.get() || isWebSocketConnected.get()) {
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
                        isWebSocketConnected.set(true);
                        reconnectAttempts.set(0);
                        webSocket.send("[5, \"OnJsonApiEvent\"]");
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        handleWebSocketMessage(text);
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        log.info("WebSocket连接正在关闭: code={}, reason={}", code, reason);
                        isWebSocketConnected.set(false);
                    }

                    @Override
                    public void onClosed(WebSocket webSocket, int code, String reason) {
                        log.info("WebSocket连接已关闭: code={}, reason={}", code, reason);
                        isWebSocketConnected.set(false);
                        if (LolClientService.this.webSocket == webSocket) {
                            LolClientService.this.webSocket = null;
                        }
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        log.error("WebSocket连接失败", t);
                        isWebSocketConnected.set(false);
                        if (LolClientService.this.webSocket == webSocket) {
                            LolClientService.this.webSocket = null;
                        }

                        if (!isShutdown.get()) {
                            scheduleWebSocketReconnect();
                        }
                    }
                });

            } catch (Exception e) {
                log.error("启动WebSocket连接失败", e);
                isWebSocketConnected.set(false);
            }
        }
    }

    /**
     * 安排WebSocket重连
     */
    private void scheduleWebSocketReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();

        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            log.error("WebSocket重连次数超过最大限制 ({})，停止重连", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        long delay = Math.min(BASE_RECONNECT_DELAY * (1L << (attempts - 1)), 60);
        log.info("WebSocket连接失败，{}秒后尝试第{}次重连...", delay, attempts);

        scheduledExecutor.schedule(() -> {
            if (!isShutdown.get() && isClientConnected.get() && !isWebSocketConnected.get()) {
                log.info("尝试第{}次重新连接WebSocket...", attempts);
                startWebSocketConnection();
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * 关闭WebSocket连接
     */
    private void closeWebSocket() {
        WebSocket currentWebSocket = this.webSocket;
        if (currentWebSocket != null) {
            this.webSocket = null;
            isWebSocketConnected.set(false);
            currentWebSocket.close(1000, "正常关闭");
        }
    }

    /**
     * 处理WebSocket消息
     */
    private void handleWebSocketMessage(String message) {
        try {
            if (StringUtils.isEmpty(message)) {
                return;
            }

            JSONArray arr = JSON.parseArray(message);
            if (arr.size() < 3 || !"OnJsonApiEvent".equals(arr.getString(1))) {
                return;
            }

            JSONObject event = arr.getJSONObject(2);
            String uri = event.getString("uri");
            String data = event.getString("data");

            switch (uri) {
                case "/lol-gameflow/v1/gameflow-phase" ->
                        webSocketMessageExecutor.submit(() -> gameFlowHandler.onGameFlowUpdate(data));
                case "/lol-champ-select/v1/session" ->
                        webSocketMessageExecutor.submit(() -> championSelectHandler.onChampSelectSessionUpdate(data));
                case "/lol-lobby-team-builder/v1/matchmaking" ->
                        webSocketMessageExecutor.submit(() -> gameFlowHandler.handleGameMode(data));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息时发生错误", e);
        }
    }

    /**
     * 更新成功统计信息
     */
    private void updateSuccessStats() {
        lastSuccessfulCheck.set(System.currentTimeMillis());
        consecutiveFailures.set(0);
    }

    /**
     * 处理检查失败
     */
    private void handleCheckFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        currentCheckInterval = fastCheckInterval;

        if (failures > 10) {
            currentCheckInterval = Math.min(normalCheckInterval, fastCheckInterval * 2);
        }
    }

    // ========== 公共API方法 ==========

    /**
     * 手动重连
     */
    public boolean manualReconnect() {
        log.info("手动触发重连...");

        clearConnectionState();
        consecutiveFailures.set(0);
        reconnectAttempts.set(0);
        currentCheckInterval = fastCheckInterval;

        // 立即检查
        checkAndMaintainConnection();

        return true;
    }

    /**
     * 获取客户端连接状态
     */
    public boolean isClientConnected() {
        return isClientConnected.get();
    }

    /**
     * 获取WebSocket连接状态
     */
    public boolean isWebSocketConnected() {
        return isWebSocketConnected.get();
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        synchronized (connectionLock) {
            if (isClientConnected.get()) {
                long timeSinceLastCheck = System.currentTimeMillis() - lastSuccessfulCheck.get();
                return String.format("LOL客户端已连接 (端口: %d, WebSocket: %s, 检查间隔: %ds, 上次成功: %ds前)",
                        currentPort,
                        isWebSocketConnected.get() ? "已连接" : "未连接",
                        currentCheckInterval,
                        timeSinceLastCheck / 1000);
            } else {
                return String.format("LOL客户端未连接 (连续失败: %d次, 检查间隔: %ds)",
                        consecutiveFailures.get(), currentCheckInterval);
            }
        }
    }
}