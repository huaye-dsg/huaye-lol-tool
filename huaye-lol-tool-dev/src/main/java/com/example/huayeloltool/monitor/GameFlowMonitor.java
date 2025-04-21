package com.example.huayeloltool.monitor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.*;
import com.example.huayeloltool.service.GameSessionUpdateService;
import com.example.huayeloltool.service.GameStateUpdateService;
import com.example.huayeloltool.service.LcuService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GameFlowMonitor implements CommandLineRunner, DisposableBean {

    private WebSocket webSocket;

    @Resource
    GameStateUpdateService gameStateUpdateService;
    @Resource
    LcuService lcuService;
    @Resource
    GameSessionUpdateService gameSessionUpdateService;
    @Resource
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    @Override
    public void run(String... args) {
        log.info("监控任务开始运行！");
        ProcessInfo lolClientApiInfo = lcuService.getLolClientApiInfo(Constant.LOL_UX_PROCESS_NAME);
        if (lolClientApiInfo == null) {
            log.error("LOL接口进程不存在！");
            return;
        }
        try {
            // 初始化url请求路径
            BaseUrlClient instance = BaseUrlClient.getInstance();
            instance.setPort(lolClientApiInfo.getPort());
            instance.setAuthPwd(lolClientApiInfo.getToken());

            // 初始化当前召唤师信息
            CurrSummoner currSummoner2 = CurrSummoner.setInstance(lcuService.getCurrSummoner());
            if (currSummoner2 == null) {
                log.error("获取当前召唤师信息失败！");
                return;
            }
            log.info("当前召唤师信息： {}", currSummoner2);

            // 初始化监听器
            initGameFlowMonitor(lolClientApiInfo.getPort(), lolClientApiInfo.getToken());
        } catch (Exception e) {
            log.error("initGameFlowMonitor error", e);
        }
    }


    public void initGameFlowMonitor(int port, String authPwd) throws Exception {
        log.info("initGameFlowMonitor begin");
        String auth = Base64.getEncoder().encodeToString(("riot:" + authPwd).getBytes());
        Request request = new Request.Builder()
                .url("wss://127.0.0.1:" + port + "/")
                .addHeader("Authorization", "Basic " + auth)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @SneakyThrows
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                log.info("Connected to LCU");
                webSocket.send("[5, \"OnJsonApiEvent\"]");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket error: ", t);
            }
        });

        // 保持连接
        boolean result = client.dispatcher().executorService().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void handleWebSocketMessage(String message) {
        try {
            if (StringUtils.isEmpty(message)) {
                return;
            }
            JSONArray arr = JSON.parseArray(message);
            if (arr.size() < 3 || !"OnJsonApiEvent".equals(arr.getString(1))) return;

            JSONObject event = arr.getJSONObject(2);
            String uri = event.getString("uri");
            Object data = event.get("data");

            switch (uri) {
                case "/lol-gameflow/v1/gameflow-phase":
                    new Thread(() -> gameStateUpdateService.onGameFlowUpdate(data.toString())).start();
                    break;
                case "/lol-champ-select/v1/session":
                    new Thread(() -> gameSessionUpdateService.onChampSelectSessionUpdate(data.toString())).start();
                    break;
                case "/lol-lobby-team-builder/v1/matchmaking":
                    // 更新数据
                    if (data == null) {
                        return;
                    }
                    Matchmaking matchmaking = JSON.parseObject(data.toString(), Matchmaking.class);
                    if (matchmaking != null && BooleanUtils.isTrue(matchmaking.getIsCurrentlyInQueue())) {
                        Integer queueId = matchmaking.getQueueId();
                        if (queueId != null && queueId > 0) {
                            String modeName = GameEnums.GameQueueID.getGameNameMap(queueId);
                            SelfGameSession.setQueue(queueId);
                            log.info("当前游戏模式为：{}", modeName);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("handleWebSocketMessage error", e);
        }
    }


    @Override
    public void destroy() {
        if (webSocket != null) {
            webSocket.close(1000, "Application shutdown");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }


}
