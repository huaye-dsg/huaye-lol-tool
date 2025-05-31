package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.huayeloltool.common.OkHttpClientCommonBean;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.game.Matchmaking;
import com.example.huayeloltool.model.score.ScoreService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameFlowMonitor {

    static OkHttpClient client = OkHttpClientCommonBean.getInstance();
    static GameStateUpdateService gameStateUpdateService = GameStateUpdateService.getInstance();
    static GameSessionUpdateService gameSessionUpdateService = GameSessionUpdateService.getInstance();

    @SneakyThrows
    public static void startGameFlowMonitor() {
        BaseUrlClient baseUrlClient = BaseUrlClient.getInstance();
        int port = baseUrlClient.getPort();
        String token = baseUrlClient.getToken();
        String auth = Base64.getEncoder().encodeToString(("riot:" + token).getBytes());

        Request request = new Request.Builder()
                .url("wss://127.0.0.1:" + port + "/")
                .addHeader("Authorization", "Basic " + auth)
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @SneakyThrows
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send("[5, \"OnJsonApiEvent\"]");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("客户端通信连接失败", t);
            }
        });

        // 保持连接
        boolean b = client.dispatcher().executorService().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }


    private static void handleWebSocketMessage(String message) {
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
                        new Thread(() -> gameStateUpdateService.onGameFlowUpdate(data)).start();
                case "/lol-champ-select/v1/session" ->
                        new Thread(() -> gameSessionUpdateService.onChampSelectSessionUpdate(data)).start();
                case "/lol-lobby-team-builder/v1/matchmaking" -> handleGameMode(data);
            }
        } catch (Exception e) {
            log.error("handleWebSocketMessage error", e);
        }
    }

    /**
     * 处理游戏模式数据
     *
     * @param data 包含游戏模式信息的数据字符串
     */
    private static void handleGameMode(String data) {
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
