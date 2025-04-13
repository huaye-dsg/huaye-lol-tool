package com.example.huayeloltool.service;

import com.alibaba.fastjson.TypeReference;
import com.example.huayeloltool.config.OkHttpUtil;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.CHAMPION_SELECT;

@Component
@Slf4j
public class LcuService extends CommonRequest {

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    public CurrSummoner getCurrSummoner() {
        try {
            Request okHttpRequest = OkHttpUtil.createOkHttpGetRequest("/lol-summoner/v1/current-summoner");
            return sendRequest(okHttpRequest, CurrSummoner.class);
        } catch (Exception e) {
            log.error("getCurrSummoner请求错误", e);
            return null;
        }
    }


    /**
     * 找到LOL进程并解析端口和token
     */
    public ProcessInfo getLolClientApiInfo(String processName) {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();
        // 获取所有进程
        List<OSProcess> processes = os.getProcesses();

        // 在进程列表中查找LOL进程
        ProcessInfo processInfo = null;
        for (OSProcess process : processes) {
            if (process.getName().equalsIgnoreCase(processName)) {
                log.info("成功找到进程！ {}", processName);
                processInfo = new ProcessInfo();
                List<String> arguments = process.getArguments();
                for (String argument : arguments) {
                    if (argument.contains("--app-port")) {
                        String[] split = argument.split("=");
                        log.info("解析的端口：{}", split[1]);
                        processInfo.setPort(Integer.valueOf(split[1]));
                    }
                    if (argument.contains("--remoting-auth-token")) {
                        String[] split = argument.split("=");
                        log.info("解析的token：{}", split[1]);
                        processInfo.setToken(split[1]);
                    }
                }
                break;
            }
        }
        return processInfo;
    }

    /**
     * 根据 PUUID 列出游戏历史记录
     */
    public List<GameInfo> listGameHistory(String puuid, int begin, int limit) throws IOException {
        List<GameInfo> fmtList = new ArrayList<>();
        GameAllData gameAllData = listGamesByPUUID(puuid, begin, limit);

        if (Objects.isNull(gameAllData)) {
            log.error("查询用户战绩失败: puuid={}", puuid);
            return new ArrayList<>();
        }
        List<GameInfo> games = gameAllData.getGames().getGames();
        if (CollectionUtils.isEmpty(games)) {
//            log.error("查询用户战绩为空！: puuid={}", puuid);
            return new ArrayList<>();
        }

        // 过滤符合条件的游戏信息
        for (GameInfo gameItem : games) {
            // 只统计排位匹配大乱斗
            if (gameItem.getGameDuration() < 300 || !GameEnums.GameQueueID.isValidData(gameItem.getQueueId())) {
                continue;
            }
            fmtList.add(gameItem);
        }
        return fmtList;
    }

    /**
     * 根据 PUUID 获取比赛记录
     */
    public GameAllData listGamesByPUUID(String puuid, int begin, int limit) throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest(
                String.format("/lol-match-history/v1/products/lol/%s/matches?begIndex=%d&endIndex=%d", puuid, begin, begin + limit));
        return sendTypeRequest(request, new TypeReference<GameAllData>() {
        });
    }

    /**
     * 查询段位信息
     */
    public RankedInfo getRankData(String puuid) {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-ranked/v1/ranked-stats/" + puuid);
        try {
            return sendRequest(request, RankedInfo.class);
        } catch (Exception e) {
            log.error("查询段位信息失败！", e);
        }
        return null;
    }

    /**
     * 查询英雄熟练度信息
     */
    public List<ChampionMastery> searchChampionMasteryData(String puuid) {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-champion-mastery/v1/" + puuid + "/champion-mastery"));

        List<ChampionMastery> championMasteryList = sendTypeRequest(request, new TypeReference<List<ChampionMastery>>() {
        });
        if (CollectionUtils.isEmpty(championMasteryList)) {
            log.error("查询英雄熟练度失败！");
            return new ArrayList<>();
        }

        championMasteryList.sort(Comparator.comparingInt(ChampionMastery::getChampionLevel).reversed());
        List<ChampionMastery> championInfos = championMasteryList.subList(0, 10);
        for (ChampionMastery championInfo : championInfos) {
            log.info("英雄：{}， 等级：{}，积分：{}", Heros.getNameById(championInfo.getChampionId()), championInfo.getChampionLevel(), championInfo.getChampionPoints());
        }
        return championInfos;
    }

    /**
     * 自动接受对局
     */
    public void acceptGame() {
        try {
            Thread.sleep(1500);
            Request request = OkHttpUtil.createOkHttpPostRequest("/lol-matchmaking/v1/ready-check/accept");
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.info("自动接受对局失败: {}", com.alibaba.fastjson2.JSON.toJSONString(response));
                }
            }
        } catch (Exception e) {
            log.error("自动接受对局失败!", e);
        }
    }

    public String getCurrConversationID() throws Exception {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-chat/v1/conversations");

        List<Conversation> conversations = sendTypeRequest(request, new TypeReference<List<Conversation>>() {
        });

        for (Conversation conversation : conversations) {
            if (CHAMPION_SELECT.equals(conversation.getType())) { // 换成你实际的类型比较
                return conversation.getId();
            }
        }
        return null;
    }

    /**
     * 根据会话ID获取会话组消息记录
     */
    public List<ConversationMsg> listConversationMsg(String conversationID) {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-chat/v1/conversations/%s/messages", conversationID));
        return sendTypeRequest(request, new TypeReference<List<ConversationMsg>>() {
        });
    }
}
