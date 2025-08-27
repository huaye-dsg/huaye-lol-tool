package com.example.huayeloltool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.huayeloltool.common.OkHttpUtil;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.game.Matchmaking;
import com.example.huayeloltool.service.ChampionSelectHandler;
import com.example.huayeloltool.service.GameFlowHandler;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class Monitor {

    @Autowired
    private GameFlowHandler gameFlowHandler;
    @Autowired
    private ChampionSelectHandler championSelectHandler;

    // 注入线程池
    @Resource(name = "webSocketMessageExecutor")
    private ExecutorService webSocketMessageExecutor;

    @Resource(name = "scheduledExecutor")
    private ScheduledExecutorService scheduledExecutor;

    private static final OkHttpClient client = OkHttpUtil.getInstance();

    // 使用AtomicBoolean确保线程安全
    private final AtomicBoolean isWebSocketConnected = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // WebSocket实例引用，用于管理连接
    private volatile WebSocket webSocket;

    // 记录当前连接参数，用于检测连接参数变化 - 使用同步锁保护
    private final Object connectionParamsLock = new Object();
    private volatile int currentPort = 0;
    private volatile String currentToken = "";

    // 重连控制
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long BASE_RECONNECT_DELAY = 5; // 基础重连延迟（秒）

    /**
     * 启动游戏流程监控
     */
    public void startGameFlowMonitor() {
        if (isShutdown.get()) {
            log.warn("Monitor已关闭，无法启动新连接");
            return;
        }

        BaseUrlClient baseUrlClient = BaseUrlClient.getInstance();
        int port = baseUrlClient.getPort();
        String token = baseUrlClient.getToken();

        if (port <= 0 || StringUtils.isBlank(token)) {
            log.error("无效的连接参数: port={}, token={}", port, token != null ? "***" : "null");
            return;
        }

        // 使用同步锁保护连接参数的检查和更新
        synchronized (connectionParamsLock) {
            // 检查连接参数是否发生变化
            boolean connectionParamsChanged = (currentPort != port || !Objects.equals(currentToken, token));

            // 检查当前连接状态 - 更严格的检查
            boolean isCurrentlyConnected = isWebSocketConnected.get() &&
                    webSocket != null &&
                    webSocket.request().url().port() == port;

            // 如果已连接且参数未变化，则无需重连
            if (isCurrentlyConnected && !connectionParamsChanged) {
                log.debug("WebSocket连接正常，参数未变化，无需重连");
                return;
            }

            // 如果参数发生变化，先关闭现有连接
            if (connectionParamsChanged && isCurrentlyConnected) {
                log.info("检测到连接参数变化 (端口: {} -> {})，关闭现有连接", currentPort, port);
                closeWebSocket();
            }

            // 更新连接参数
            currentPort = port;
            currentToken = token;
        }

        try {
            String auth = Base64.getEncoder().encodeToString(("riot:" + token).getBytes());

            Request request = new Request.Builder()
                    .url("wss://127.0.0.1:" + port + "/")
                    .addHeader("Authorization", "Basic " + auth)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("WebSocket连接已建立");
                    isWebSocketConnected.set(true);
                    reconnectAttempts.set(0); // 重置重连计数
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
                    // 确保状态同步
                    isWebSocketConnected.set(false);
                    if (Monitor.this.webSocket == webSocket) {
                        Monitor.this.webSocket = null;
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("WebSocket连接失败", t);
                    // 确保状态同步
                    isWebSocketConnected.set(false);
                    if (Monitor.this.webSocket == webSocket) {
                        Monitor.this.webSocket = null;
                    }

                    // 如果不是主动关闭且未shutdown，考虑重连
                    if (!isShutdown.get()) {
                        scheduleReconnect();
                    }
                }
            });

        } catch (Exception e) {
            log.error("启动WebSocket监控失败", e);
            isWebSocketConnected.set(false);
            webSocket = null;
        }
    }

    /**
     * 安排重连（带重连次数限制和退避策略）
     */
    private void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();

        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            log.error("WebSocket重连次数超过最大限制 ({})，停止重连", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        // 优化：添加抖动避免雷群效应
        long baseDelay = BASE_RECONNECT_DELAY * (1L << (attempts - 1));
        long jitter = (long) (Math.random() * 1000); // 0-1秒随机抖动
        long delay = Math.min(baseDelay + jitter, 60);

        log.info("WebSocket连接失败，{}秒后尝试第{}次重连...", delay, attempts);

        scheduledExecutor.schedule(() -> {
            if (!isShutdown.get() && !isWebSocketConnected.get()) {
                // 优化：在重连前检查网络连通性
                if (isNetworkAvailable()) {
                    log.info("尝试第{}次重新连接WebSocket...", attempts);
                    startGameFlowMonitor();
                } else {
                    log.warn("网络不可用，延迟重连");
                    // 网络不可用时，重置重连计数并稍后重试
                    reconnectAttempts.decrementAndGet();
                    scheduledExecutor.schedule(this::scheduleReconnect, 5, TimeUnit.SECONDS);
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * 重置重连计数（供外部调用）
     */
    public void resetReconnectAttempts() {
        reconnectAttempts.set(0);
    }

    /**
     * 关闭WebSocket连接
     */
    public void closeWebSocket() {
        log.info("正在关闭WebSocket连接...");

        WebSocket currentWebSocket = this.webSocket;
        if (currentWebSocket != null) {
            this.webSocket = null;
            isWebSocketConnected.set(false);
            currentWebSocket.close(1000, "正常关闭");
        } else {
            // 确保状态一致
            isWebSocketConnected.set(false);
        }
    }

    /**
     * 检查连接状态（更严格的检查）
     */
    public boolean isConnected() {
        WebSocket currentWebSocket = this.webSocket;
        return isWebSocketConnected.get() && currentWebSocket != null;
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        synchronized (connectionParamsLock) {
            return String.format("WebSocket连接状态: %s, 端口: %d, 重连次数: %d/%d",
                    isConnected() ? "已连接" : "未连接",
                    currentPort,
                    reconnectAttempts.get(),
                    MAX_RECONNECT_ATTEMPTS);
        }
    }

    /**
     * 应用关闭时的资源清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("Monitor正在关闭...");
        isShutdown.set(true);
        closeWebSocket();
    }

    /**
     * 处理WebSocket消息（使用线程池替代直接创建线程）
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
                // 开始匹配、开始选人、进入对局
                case "/lol-gameflow/v1/gameflow-phase" ->
                        webSocketMessageExecutor.submit(() -> gameFlowHandler.onGameFlowUpdate(data));
                // 选人、禁用事件
                case "/lol-champ-select/v1/session" ->
                        webSocketMessageExecutor.submit(() -> championSelectHandler.onChampSelectSessionUpdate(data));
                // 开始匹配
                case "/lol-lobby-team-builder/v1/matchmaking" ->
                        webSocketMessageExecutor.submit(() -> gameFlowHandler.handleGameMode(data));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息时发生错误", e);
        }
    }

    /**
     * 检查网络连通性
     */
    private boolean isNetworkAvailable() {
        try {
            BaseUrlClient baseUrlClient = BaseUrlClient.getInstance();
            int port = baseUrlClient.getPort();
            String token = baseUrlClient.getToken();

            if (port <= 0 || StringUtils.isBlank(token)) {
                return false;
            }

            // 简单的连通性检查：尝试创建连接
            java.net.Socket socket = new java.net.Socket();
            try {
                socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 2000);
                return true;
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            log.debug("网络连通性检查失败: {}", e.getMessage());
            return false;
        }
    }
}