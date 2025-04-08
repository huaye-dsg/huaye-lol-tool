package com.example.huayeloltool.monitor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.BaseUrlClient;
import com.example.huayeloltool.model.ChampSelectSessionInfo;
import com.example.huayeloltool.model.CurrSummoner;
import com.example.huayeloltool.model.ProcessInfo;
import com.example.huayeloltool.service.impl.GameUpdateService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

//@Slf4j
@Component
@Slf4j
public class GameFlowMonitor implements CommandLineRunner, DisposableBean {

    private static final int RETRY_ATTEMPTS = 5;
    private static final long RETRY_DELAY = 1000; // 毫秒
    private static final int PREFIX_LENGTH = 11; // 假设消息前缀长度

    private WebSocketSession session;
    private boolean lcuActive = false;
    private WebSocket webSocket;

    @Autowired
    GameUpdateService gameUpdateService;

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    CurrSummoner currSummoner;

    @Override
    public void run(String... args) {
        log.info("监控任务开始运行！");
        ProcessInfo lolClientApiInfo = gameUpdateService.getLolClientApiInfo(Constant.LOL_UX_PROCESS_NAME);
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
            CurrSummoner currSummoner1 = gameUpdateService.getCurrSummoner();
            CurrSummoner currSummoner2 = CurrSummoner.setInstance(currSummoner1);
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
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("Connected to LCU");
                webSocket.send("[5, \"OnJsonApiEvent\"]");
                lcuActive = true;
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket error: ", t);
                lcuActive = false;
            }
        });

        // 保持连接
        client.dispatcher().executorService().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void handleWebSocketMessage(String message) {
        try {
            if (StringUtils.isEmpty(message)) {
//                log.info("消息是空的！");
                return;
            }
            JSONArray arr = JSON.parseArray(message);
            if (arr.size() < 3 || !"OnJsonApiEvent".equals(arr.getString(1))) return;

            JSONObject event = arr.getJSONObject(2);
            String uri = event.getString("uri");
            Object data = event.get("data");

            switch (uri) {
                case "/lol-gameflow/v1/gameflow-phase":
                    gameUpdateService.onGameFlowUpdate(data.toString());
                    break;
                case "/lol-champ-select/v1/session":
//                    new Thread(() -> {
                    log.info("游戏会话变更！data : {}", JSON.toJSONString(data));
                    ChampSelectSessionInfo ss = JSON.parseObject(data.toString(), ChampSelectSessionInfo.class);
                    gameUpdateService.onChampSelectSessionUpdate(ss);
//                    }
//
//                    ).start();
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
