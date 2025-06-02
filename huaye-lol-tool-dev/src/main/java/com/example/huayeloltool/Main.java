package com.example.huayeloltool;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.GameFlowMonitor;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.List;
import java.util.Objects;

@Slf4j
public class Main {
    private static final LcuApiService lcuApiService = LcuApiService.getInstance();

        // OPGGapi
    //https://lol-api-champion.op.gg/api/KR/champions/ranked/${championId}/${position}

    // LCUAPI
    // https://lcu.kebs.dev/swagger.html

    /**
     * 主程序入口，负责初始化LCU API连接、召唤师信息和游戏流程监控
     */
    public static void main(String[] args) {
        // 检查LOL客户端API连接状态
        if (!checkLolClientConnection()) {
            log.error("客户端进程不存在！");
            return;
        }
        log.info("客户端连接成功");

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
    public static boolean checkLolClientConnection() {
        Pair<Integer, String> apiInfo = getLolClientApiInfo(Constant.LOL_UX_PROCESS_NAME);
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
    public static boolean initializeSummonerInfo() {
        Summoner summoner = Summoner.setInstance(lcuApiService.getCurrSummoner());
        String privacy = summoner.getPrivacy();
//        log.info("战绩隐藏情况为: {}", privacy);
        return Objects.nonNull(summoner);
    }


    /**
     * 找到LOL进程并解析端口和token
     */
    public static Pair<Integer, String> getLolClientApiInfo(String processName) {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();
        // 获取所有进程
        List<OSProcess> processes = os.getProcesses();

        // 在进程列表中查找LOL进程
        for (OSProcess process : processes) {
            if (process.getName().equalsIgnoreCase(processName)) {
                List<String> arguments = process.getArguments();
                int port = 0;
                String token = "";
                for (String argument : arguments) {
                    if (argument.contains("--app-port")) {
                        String[] split = argument.split("=");
                        port = Integer.parseInt(split[1]);
                    }
                    if (argument.contains("--remoting-auth-token")) {
                        String[] split = argument.split("=");
                        token = split[1];
                    }
                }
                return Pair.of(port, token);
            }
        }

        return Pair.of(0, "");
    }


}