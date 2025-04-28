package com.example.huayeloltool.monitor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.game.Matchmaking;
import com.example.huayeloltool.service.GameSessionUpdateService;
import com.example.huayeloltool.service.GameStateUpdateService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameFlowMonitor {

    public final OkHttpClient client;
    private final GameStateUpdateService gameStateUpdateService;
    private final GameSessionUpdateService gameSessionUpdateService;

    // 构造函数注入
    public GameFlowMonitor(OkHttpClient client, GameStateUpdateService gameStateUpdateService, GameSessionUpdateService gameSessionUpdateService) {
        this.client = client;
        this.gameStateUpdateService = gameStateUpdateService;
        this.gameSessionUpdateService = gameSessionUpdateService;
    }

    public void initGameFlowMonitor(int port, String token) throws Exception {
        String auth = Base64.getEncoder().encodeToString(("riot:" + token).getBytes());
        Request request = new Request.Builder()
                .url("wss://127.0.0.1:" + port + "/")
                .addHeader("Authorization", "Basic " + auth)
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @SneakyThrows
            @Override
            public void onOpen( WebSocket webSocket,  Response response) {
                log.info("Connected to LCU");
                webSocket.send("[5, \"OnJsonApiEvent\"]");
            }

            @Override
            public void onMessage( WebSocket webSocket,  String text) {
                handleWebSocketMessage(text);
            }

            @Override
            public void onFailure( WebSocket webSocket,  Throwable t, Response response) {
                log.error("WebSocket error: ", t);
            }
        });

        // 保持连接
        boolean b = client.dispatcher().executorService().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

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
            Object data = event.get("data");

            switch (uri) {
                case "/lol-gameflow/v1/gameflow-phase" ->
                        new Thread(() -> gameStateUpdateService.onGameFlowUpdate(data.toString())).start();
                case "/lol-champ-select/v1/session" ->
                        new Thread(() -> gameSessionUpdateService.onChampSelectSessionUpdate(data.toString())).start();
                case "/lol-lobby-team-builder/v1/matchmaking" -> handGameMode(data);
            }
        } catch (Exception e) {
            log.error("handleWebSocketMessage error", e);
        }
    }

    private void handGameMode(Object data) {
        if (data != null) {
            Matchmaking matchmaking = JSON.parseObject(data.toString(), Matchmaking.class);
            if (BooleanUtils.isTrue(matchmaking.getIsCurrentlyInQueue())) {
                Integer queueId = matchmaking.getQueueId();
                if (queueId != null && queueId > 0) {
                    String modeName = GameEnums.GameQueueID.getGameNameMap(queueId);
                    CustomGameSession.getInstance().setQueueId(queueId);
                    log.info("当前游戏模式为：{}", modeName);
                }
            }
        }
    }


}
