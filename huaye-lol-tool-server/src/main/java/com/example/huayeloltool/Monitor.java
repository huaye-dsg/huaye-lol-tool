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
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class Monitor {

    @Autowired
    private GameFlowHandler gameFlowHandler;
    @Autowired
    private ChampionSelectHandler championSelectHandler;

    private static final OkHttpClient client = OkHttpUtil.getInstance();

    // 使用AtomicBoolean确保线程安全
    private final AtomicBoolean isWebSocketConnected = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // WebSocket实例引用，用于管理连接
    private volatile WebSocket webSocket;

    /**
     * 启动游戏流程监控
     */
    public void startGameFlowMonitor() {
        if (isShutdown.get()) {
            log.warn("Monitor已关闭，无法启动新连接");
            return;
        }

        if (isWebSocketConnected.get()) {
            log.warn("WebSocket连接已存在，请先关闭现有连接");
            return;
        }

        try {
            BaseUrlClient baseUrlClient = BaseUrlClient.getInstance();
            int port = baseUrlClient.getPort();
            String token = baseUrlClient.getToken();

            if (port <= 0 || StringUtils.isBlank(token)) {
                log.error("无效的连接参数: port={}, token={}", port, token != null ? "***" : "null");
                return;
            }

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
                    Monitor.this.webSocket = null;
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("WebSocket连接失败", t);
                    isWebSocketConnected.set(false);
                    Monitor.this.webSocket = null;

                    // 如果不是主动关闭且未shutdown，可以考虑重连
                    if (!isShutdown.get()) {
                        log.info("连接失败，5秒后尝试重连...");
                        scheduleReconnect();
                    }
                }
            });

        } catch (Exception e) {
            log.error("启动WebSocket监控失败", e);
            isWebSocketConnected.set(false);
        }
    }

    /**
     * 安排重连（简单的延迟重连，避免频繁重连）
     */
    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒
                if (!isShutdown.get() && !isWebSocketConnected.get()) {
                    log.info("尝试重新连接WebSocket...");
                    startGameFlowMonitor();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("重连线程被中断");
            }
        }).start();
    }

    /**
     * 关闭WebSocket连接
     */
    public void closeWebSocket() {
        log.info("正在关闭WebSocket连接...");
        isWebSocketConnected.set(false);

        if (webSocket != null) {
            // 正常关闭连接
            webSocket.close(1000, "正常关闭");
            webSocket = null;
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isWebSocketConnected.get() && webSocket != null;
    }

    /**
     * 应用关闭时的资源清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("Monitor正在关闭...");
        isShutdown.set(true);
        closeWebSocket();

        // 关闭OkHttp客户端的连接池（如果需要的话）
        // 注意：由于client是静态的，这里要谨慎处理
        try {
            client.dispatcher().executorService().shutdown();
        } catch (Exception e) {
            log.warn("关闭OkHttp线程池时出现异常", e);
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
                // 开始匹配、开始选人、进入对局
                case "/lol-gameflow/v1/gameflow-phase" ->
                        new Thread(() -> gameFlowHandler.onGameFlowUpdate(data)).start();
                // 选人、禁用事件
                case "/lol-champ-select/v1/session" ->
                        new Thread(() -> championSelectHandler.onChampSelectSessionUpdate(data)).start();
                // 开始匹配
                case "/lol-lobby-team-builder/v1/matchmaking" -> handleGameMode(data);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息时发生错误", e);
        }
    }

    /**
     * 处理游戏模式数据
     *
     * @param data 包含游戏模式信息的数据字符串
     */
    private static void handleGameMode(String data) {
        CustomGameSession.getInstance().reset();
        // 如果传入的数据为空，则直接返回
        if (data == null) return;
        try {
            // 将JSON格式的数据解析成Matchmaking对象
            Matchmaking matchmaking = JSON.parseObject(data, Matchmaking.class);

            // 判断是否正在排队
            boolean isInQueue = BooleanUtils.isTrue(matchmaking.getIsCurrentlyInQueue());

            // 获取队列ID
            Integer queueId = matchmaking.getQueueId();

            // 如果正在排队并且队列ID有效（大于0）
            if (isInQueue && queueId != null && queueId > 0) {
                // 根据队列ID获取对应的游戏模式名称
                String modeName = GameEnums.GameQueueID.getGameNameMap(queueId);

                // 设置当前队列ID到自定义游戏会话实例中
                CustomGameSession.getInstance().setQueueId(queueId);

                // 记录日志，显示当前游戏模式
                log.info("当前模式：{}，queueId：{}", modeName, queueId);
            }

        } catch (Exception e) {
            // 添加异常处理，防止程序因意外情况而崩溃
            log.error("处理游戏模式时发生错误", e);
        }
    }
}