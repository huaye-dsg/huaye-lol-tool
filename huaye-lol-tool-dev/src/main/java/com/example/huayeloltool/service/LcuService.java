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
//            // 执行请求
//            try (Response response = client.newCall(okHttpRequest).execute()) {
//                if (!response.isSuccessful()) {
//                    log.error("getCurrSummoner请求失败，response: {}", response);
//                    throw new IOException("请求失败，返回码: " + response.code());
//                }
//
//                // 解析响应体
//                assert response.body() != null;
//                byte[] responseBody = response.body().bytes();
//                CurrSummoner data = JSON.parseObject(responseBody, CurrSummoner.class);
//
//                // 校验数据
//                if (data.getSummonerId() == 0) {
//                    log.error("获取当前召唤师失败，召唤师ID为0");
//                }
//
//                return data;
//            }
//        } catch (Exception e) {
//            log.error("getCurrSummoner请求错误", e);
//            return null;
//        }
    }


    /**
     * 找到LOL进程并解析端口和token
     */
    public ProcessInfo getLolClientApiInfo(String processName) {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();

        // 获取所有进程
        List<OSProcess> processes = os.getProcesses();

        String targetProcessName = "LeagueClientUx";
        // 在进程列表中查找LOL进程
        ProcessInfo processInfo = null;
        for (OSProcess process : processes) {
            if (process.getName().equalsIgnoreCase(targetProcessName)) {
                log.info("成功找到进程！ {}", targetProcessName);
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
            if (!GameEnums.GameQueueID.isValidData(gameItem.getQueueId())) {
                continue;
            }
            // 游戏时长
            if (gameItem.getGameDuration() < 300) {
                continue;
            }
            fmtList.add(gameItem);
        }
        return fmtList;
    }

    /**
     * 根据 PUUID 获取比赛记录
     *
     * @param puuid 用户唯一标识符
     * @param begin 开始位置
     * @param limit 获取数量
     * @return 比赛记录响应对象
     * @throws IOException 获取比赛记录失败时抛出异常
     */
    public GameAllData listGamesByPUUID(String puuid, int begin, int limit) throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-match-history/v1/products/lol/%s/matches?begIndex=%d&endIndex=%d", puuid, begin, begin + limit));

        return sendTypeRequest(request, new TypeReference<GameAllData>() {
        });
//        GameAllData gameInfos;
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                log.error("获取比赛记录失败: puuid={}", puuid);
//                throw new IOException("获取比赛记录失败");
//            }
//            assert response.body() != null;
//            String string = response.body().string();
//            gameInfos = JSON.parseObject(string, new TypeReference<GameAllData>() {
//            });
////            log.info("战绩解析结果（最后一条）：: {}", gameInfos.getGames().getGames().get(0));
//        }
//        return gameInfos;
    }

    /**
     * 查询段位信息
     */
    public RankedInfo getRankData(String puuid) {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-ranked/v1/ranked-stats/" + puuid);
        try {
            return sendRequest(request, RankedInfo.class);
//            new RankedInfo();
//            RankedInfo rankedInfo;
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    log.error("获取段位信息失败: response={}", response);
//                    throw new IOException("获取段位信息失败");
//                }
//                assert response.body() != null;
//                String string = response.body().string();
//                rankedInfo = JSON.parseObject(string, RankedInfo.class);
//                RankedInfo.QueueMapDto.RANKEDSOLO5x5Dto rankedSolo5x5 = rankedInfo.getQueueMap().getRankedSolo5x5();
//                log.info("段位信息。当前段位：{}-{}，胜点：{}", GameEnums.RankTier.getRankNameMap(rankedSolo5x5.getTier()), rankedSolo5x5.getDivision(), rankedSolo5x5.getLeaguePoints());
//                return rankedInfo;
//            }
        } catch (Exception e) {
            log.error("查询段位信息失败！", e);
        }
        return null;
    }

    /**
     * 查询英雄熟练度信息
     */
    public List<ChampionMastery> searchChampionMasteryData(String puuid) throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-champion-mastery/v1/" + puuid + "/champion-mastery"));

        List<ChampionMastery> championMasteryList = sendTypeRequest(request, new TypeReference<List<ChampionMastery>>() {
        });
        if (CollectionUtils.isEmpty(championMasteryList)){
            log.error("查询英雄熟练度失败！");
            return new ArrayList<>();
        }

        championMasteryList.sort(Comparator.comparingInt(ChampionMastery::getChampionLevel).reversed());
        List<ChampionMastery> championInfos = championMasteryList.subList(0, 10);
        for (ChampionMastery championInfo : championInfos) {
            log.info("英雄：{}， 等级：{}，积分：{}", Heros.getNameById(championInfo.getChampionId()), championInfo.getChampionLevel(), championInfo.getChampionPoints());
        }

//        List<ChampionMastery> championMasteryList = null;
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                log.error("查询英雄熟练度失败: response={}", response);
//                throw new IOException("查询英雄熟练度失败");
//            }
//            assert response.body() != null;
//            String string = response.body().string();
//            championMasteryList = JSON.parseObject(string, new TypeReference<List<ChampionMastery>>() {
//            });
//
//            championMasteryList.sort(Comparator.comparingInt(ChampionMastery::getChampionLevel).reversed());
//            List<ChampionMastery> championInfos = championMasteryList.subList(0, 10);
//            for (ChampionMastery championInfo : championInfos) {
//                log.info("英雄：{}， 等级：{}，积分：{}", Heros.getNameById(championInfo.getChampionId()), championInfo.getChampionLevel(), championInfo.getChampionPoints());
//            }
////            log.info("查询英雄熟练度：: {}", string);
//        } catch (Exception e) {
//            log.error("查询英雄熟练度异常", e);
//        }
        return championInfos;
    }

    /**
     * 自动接受对局
     */
    public void acceptGame() {
        try {
            Thread.sleep(2000);
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
        // 执行请求
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                log.error("getCurrSummoner请求失败，response: {}", response);
//                throw new IOException("请求失败，返回码: " + response.code());
//            }
//
//            // 解析响应体
//            assert response.body() != null;
//            byte[] responseBody = response.body().bytes();
//            List<Conversation> conversationList = JSON.parseObject(new String(responseBody), new TypeReference<List<Conversation>>() {
//            });
//
//            for (Conversation conversation : conversationList) {
//                if (CHAMPION_SELECT.equals(conversation.getType())) { // 换成你实际的类型比较
//                    return conversation.getId();
//                }
//            }
//
//            return null;
//        }
    }

    /**
     * 根据会话ID获取会话组消息记录
     */
    public List<ConversationMsg> listConversationMsg(String conversationID) throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-chat/v1/conversations/%s/messages", conversationID));

        return sendTypeRequest(request, new TypeReference<List<ConversationMsg>>() {
        });

//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                log.error("获取会话组消息记录失败: conversationID={}", conversationID);
//                throw new IOException("获取会话组消息记录失败");
//            }
//            assert response.body() != null;
//            return JSON.parseObject(response.body().string(), new TypeReference<List<ConversationMsg>>() {
//            });
//        }
    }
}
