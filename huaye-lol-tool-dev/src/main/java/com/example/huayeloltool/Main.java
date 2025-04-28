package com.example.huayeloltool;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.config.CommonBean;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.score.ScoreService;
import com.example.huayeloltool.monitor.GameFlowMonitor;
import com.example.huayeloltool.service.GameSessionUpdateService;
import com.example.huayeloltool.service.GameStateUpdateService;
import com.example.huayeloltool.service.LcuApiService;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Slf4j
public class Main {
    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger(Main.class);
        System.out.println("Logger implementation: " + log.getClass().getName());


        LcuApiService lcuApiService = new LcuApiService();

        Pair<Integer, String> lolClientApiInfo = lcuApiService.getLolClientApiInfo(Constant.LOL_UX_PROCESS_NAME);
        log.info("lolClientApiInfo: {}", JSON.toJSONString(lolClientApiInfo));
        if (lolClientApiInfo.getLeft() == 0) {
            log.error("LOL接口进程不存在！");
            return;
        }
        try {
            // 初始化url请求路径
            BaseUrlClient instance = BaseUrlClient.getInstance();
            instance.setPort(lolClientApiInfo.getLeft());
            instance.setToken(lolClientApiInfo.getRight());

            // 初始化当前召唤师信息
            Summoner summoner = Summoner.setInstance(lcuApiService.getCurrSummoner());
            if (summoner == null) {
                log.error("获取当前召唤师信息失败！");
                return;
            }

            // 初始化监听器
            GameFlowMonitor gameFlowMonitor = getGameFlowMonitor(lcuApiService);
            gameFlowMonitor.initGameFlowMonitor(instance.getPort(), instance.getToken());
        } catch (Exception e) {
            log.error("initGameFlowMonitor error", e);
        }
    }

    private static GameFlowMonitor getGameFlowMonitor(LcuApiService lcuApiService) {
        CommonBean commonBean = new CommonBean();
        OkHttpClient okHttpClient = commonBean.unsafeOkHttpClient();
        ScoreService scoreService = new ScoreService();
        GameStateUpdateService gameStateUpdateService = new GameStateUpdateService(lcuApiService, scoreService);
        GameSessionUpdateService gameSessionUpdateService = new GameSessionUpdateService(lcuApiService);
        return new GameFlowMonitor(okHttpClient, gameStateUpdateService, gameSessionUpdateService);
    }
}