package com.example.huayeloltool;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.GameFlowMonitor;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

@Slf4j
public class Main {

    static LcuApiService lcuApiService = LcuApiService.getInstance();

    /**
     * 主程序入口，负责初始化LCU API连接、召唤师信息和游戏流程监控
     */
    public static void main(String[] args) {
        // 检查LOL客户端API连接状态
        if (!checkLolClientConnection()) {
            log.error("LOL接口进程不存在！");
            return;
        }
        log.info("LOL客户端连接成功");

        try {
            // 初始化当前召唤师信息
            if (!initializeSummonerInfo()) {
                log.error("初始化当前召唤师信息失败！");
                return;
            }

            // 原神！启动！
            GameFlowMonitor.startGameFlowMonitor();
        } catch (Exception e) {
            log.error("主程序初始化过程中发生错误", e);
        }
    }

    /**
     * 检查LOL客户端API连接状态
     *
     * @return 连接是否成功
     */
    private static boolean checkLolClientConnection() {
        Pair<Integer, String> apiInfo = lcuApiService.getLolClientApiInfo(Constant.LOL_UX_PROCESS_NAME);
        if (apiInfo.getLeft() == 0) {
            return false;
        }
        BaseUrlClient instance = BaseUrlClient.getInstance();
        instance.setPort(apiInfo.getLeft());
        instance.setToken(apiInfo.getRight());
        return true;
    }


    /**
     * 初始化当前召唤师信息
     *
     * @return 初始化是否成功
     */
    private static boolean initializeSummonerInfo() {
        Summoner summoner = Summoner.setInstance(lcuApiService.getCurrSummoner());
        return Objects.nonNull(summoner);
    }

}