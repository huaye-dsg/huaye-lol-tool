package com.example.huayeloltool.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.huayeloltool.config.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.CHAMPION_SELECT;

@Component
@Slf4j
public class LcuService extends CommonRequest {

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    /**
     * 当前用户信息
     */
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
        for (OSProcess process : processes) {
            if (process.getName().equalsIgnoreCase(processName)) {
                //log.info("成功找到进程！ {}", processName);
                ProcessInfo processInfo = new ProcessInfo();
                List<String> arguments = process.getArguments();
                for (String argument : arguments) {
                    if (argument.contains("--app-port")) {
                        String[] split = argument.split("=");
                        //log.info("解析的端口：{}", split[1]);
                        processInfo.setPort(Integer.valueOf(split[1]));
                    }
                    if (argument.contains("--remoting-auth-token")) {
                        String[] split = argument.split("=");
                        //log.info("解析的token：{}", split[1]);
                        processInfo.setToken(split[1]);
                    }
                }
                return processInfo;
            }
        }

        return null;
    }

    /**
     * 根据 PUUID 列出游戏历史记录
     */
    public List<GameInfo> listGameHistory(CurrSummoner currSummoner, int begin, int limit) {
        List<GameInfo> fmtList = new ArrayList<>();
        GameAllData gameAllData = listGamesByPUUID(currSummoner.getPuuid(), begin, limit);

        if (Objects.isNull(gameAllData)) {
            log.error("查询用户战绩失败: {}", currSummoner.getGameName());
            return new ArrayList<>();
        }
        List<GameInfo> games = gameAllData.getGames().getGames();
        if (CollectionUtils.isEmpty(games)) {
            log.error("【风险警告】查询用户{}战绩为空！", currSummoner.getGameName());
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
    public GameAllData listGamesByPUUID(String puuid, int begin, int limit) {
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
     * 查询英雄池熟练度列表
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
        return championMasteryList.subList(0, 30);
    }


    /**
     * 查询对局详情
     *
     * @param gameID 对局ID
     * @return 对局详情信息对象
     * @throws IOException 查询对局详情过程中遇到的异常
     */
    public GameSummary queryGameSummary(long gameID) throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-match-history/v1/games/%d", gameID));
        return sendRequest(request, GameSummary.class);
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

    /**
     * 获取当前会话ID
     */
    public String getCurrConversationID() {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-chat/v1/conversations");
        List<Conversation> conversations = sendTypeRequest(request, new TypeReference<List<Conversation>>() {
        });

        for (Conversation conversation : conversations) {
            if (CHAMPION_SELECT.equals(conversation.getType())) { // 换成你实际的类型比较
                return conversation.getId();
            }
        }
        log.info("当前未查询到会话信息：{}", JSON.toJSONString(conversations));
        return null;
    }



    /**
     * 获取召唤师信息
     */
    public List<CurrSummoner> listSummoner(List<Long> summonerIDList) {
        List<String> idStrList = summonerIDList.stream().map(String::valueOf).collect(Collectors.toList());
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-summoner/v2/summoners?ids=[%s]", String.join(",", idStrList)));
        return sendTypeRequest(request, new TypeReference<List<CurrSummoner>>() {
        });
    }

    /**
     * 根据会话ID获取会话组消息记录
     */
    public List<ConversationMsg> listConversationMsg(String conversationID) {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-chat/v1/conversations/%s/messages", conversationID));
        return sendTypeRequest(request, new TypeReference<List<ConversationMsg>>() {
        });
    }


    /**
     * 预选英雄
     */
    public void prePickChampion(int championId, int actionId) {
        champSelectPatchAction(championId, actionId, null, null);
    }

    /**
     * 确认选择
     */
    public void pickChampion(int championId, int actionId) {
        champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_PICK,
                true
        );
    }

    /**
     * 禁用英雄
     */
    public void banChampion(int championId, int actionId) {
        champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_BAN,
                true
        );
    }


    /**
     * 通用英雄选择操作
     */
    public void champSelectPatchAction(int championId, int actionId, String patchType, Boolean completed) {
        if (championId <= 0) {
            log.error("通用英雄选择操作, championId 为空！");
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("championId", championId);
        Optional.ofNullable(patchType).ifPresent(t -> body.put("type", t));
        Optional.ofNullable(completed).ifPresent(c -> body.put("completed", c));
        Request request = OkHttpUtil.createOkHttpPatchRequest("/lol-champ-select/v1/session/actions/" + actionId, body);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("champSelectPatchActionError: {}", response);
            }
        } catch (Exception e) {
            log.error("champSelectPatchActionIOError", e);
        }
    }
}
