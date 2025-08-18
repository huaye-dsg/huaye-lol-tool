package com.example.huayeloltool;

import com.example.huayeloltool.common.BusinessException;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.base.BaseUrlClient;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class StartLauncher implements ApplicationRunner {

    @Autowired
    LcuApiService lcuApiService;
    @Autowired
    Monitor monitor;

    @Override
    public void run(ApplicationArguments args) {
        // 检查LOL客户端API连接状态
        if (!checkLolClientConnection()) {
            log.error("客户端进程不存在！");
            // 不再抛出异常，而是记录日志并返回
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
            monitor.startGameFlowMonitor();
        } catch (Exception e) {
            log.error("主程序初始化过程中发生错误", e);
        }
    }

    /**
     * 检查LOL客户端API连接状态
     *
     * @return 连接是否成功
     */
    public boolean checkLolClientConnection() {
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
    public boolean initializeSummonerInfo() {
        Summoner summoner = Summoner.setInstance(lcuApiService.getCurrSummoner());
        return Objects.nonNull(summoner);
    }

    /**
     * 找到LOL进程并解析端口和token
     */
    public Pair<Integer, String> getLolClientApiInfo(String processName) {
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