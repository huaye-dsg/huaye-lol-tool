package com.example.huayeloltool.service;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.example.huayeloltool.common.CommonRequest;
import com.example.huayeloltool.common.OkHttpUtil;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.Conversation.Conversation;
import com.example.huayeloltool.model.Conversation.ConversationMsg;
import com.example.huayeloltool.model.summoner.Summoner;
import com.example.huayeloltool.model.champion.ChampionMastery;
import com.example.huayeloltool.model.game.GameFlowSession;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.summoner.RankedInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.CHAMPION_SELECT;

@Slf4j
public class LcuApiService extends CommonRequest {

    private static LcuApiService instance;

    public static LcuApiService getInstance() {
        if (instance == null) {
            instance = new LcuApiService();
        }
        return instance;
    }

    public LcuApiService() {
    }


    /**
     * 当前用户信息
     */
    public Summoner getCurrSummoner() {
        try {
            Request okHttpRequest = OkHttpUtil.createOkHttpGetRequest("/lol-summoner/v1/current-summoner");
            return sendRequest(okHttpRequest, Summoner.class);
        } catch (Exception e) {
            log.error("getCurrSummoner请求错误", e);
            return null;
        }
    }


    /**
     * 有重试机制的查询游戏详情方法
     */
    public GameSummary queryGameSummaryWithRetry(long gameId) throws Exception {
        int attempts = 5;
        int delay = 10; // 毫秒
        Exception lastException = null;

        for (int i = 0; i < attempts; i++) {
            try {
                return queryGameSummary(gameId);
            } catch (Exception e) {
                lastException = e;
                Thread.sleep(delay);
            }
        }
        throw lastException;
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

    /**
     * 根据 puuid 查询游戏记录
     */
    public List<GameHistory.GameInfo> listGameHistory(Summoner summoner, int begin, int limit) {
        List<GameHistory.GameInfo> fmtList = new ArrayList<>();
        GameHistory gameHistory = listGamesByPUUID(summoner.getPuuid(), begin, limit);

        if (Objects.isNull(gameHistory)) {
            log.error("查询用户战绩失败: {}", summoner.getGameName());
            return new ArrayList<>();
        }
        List<GameHistory.GameInfo> games = gameHistory.getGames().getGames();
        if (CollectionUtils.isEmpty(games)) {
            log.error("【风险警告】查询用户{}战绩为空！", summoner.getGameName());
            return new ArrayList<>();
        }

        for (GameHistory.GameInfo gameItem : games) {
            // 过滤时长短的无效游戏
            if (gameItem.getGameDuration() > 300) {
                fmtList.add(gameItem);
            }
        }
        return fmtList;
    }


    // 游戏列表
    public GameHistory listGamesByPUUID(String puuid, int begin, int limit) {
        Request request = OkHttpUtil.createOkHttpGetRequest(
                String.format("/lol-match-history/v1/products/lol/%s/matches?begIndex=%d&endIndex=%d", puuid, begin, begin + limit));
        return sendTypeRequest(request, new TypeReference<>() {
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

        List<ChampionMastery> championMasteryList = sendTypeRequest(request, new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(championMasteryList)) {
            log.error("查询英雄熟练度失败！");
            return new ArrayList<>();
        }

        championMasteryList.sort(Comparator.comparingInt(ChampionMastery::getChampionLevel).reversed());
        return championMasteryList;
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
                    log.info("自动接受对局失败: {}", JSON.toJSONString(response));
                }
            }
        } catch (Exception e) {
            log.error("自动接受对局失败!", e);
        }
    }

    /**
     * 获取本人当前会话ID
     */
    public String getCurrConversationID() {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-chat/v1/conversations");
        List<Conversation> conversations = sendTypeRequest(request, new TypeReference<>() {
        });

        for (Conversation conversation : conversations) {
            if (CHAMPION_SELECT.equals(conversation.getType())) { // 换成你实际的类型比较
                return conversation.getId();
            }
        }
        log.info("当前未查询到会话信息：{}", JSON.toJSONString(conversations));
        return StringUtils.EMPTY;
    }


    /**
     * 获取召唤师信息
     */
    public List<Summoner> listSummoner(List<Long> summonerIDList) {
        List<String> idStrList = summonerIDList.stream().map(String::valueOf).collect(Collectors.toList());
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-summoner/v2/summoners?ids=[%s]", String.join(",", idStrList)));
        return sendTypeRequest(request, new TypeReference<>() {
        });
    }

    /**
     * 根据会话ID获取会话组消息记录
     */
    public List<ConversationMsg> listConversationMsg(String conversationID) {
        Request request = OkHttpUtil.createOkHttpGetRequest(String.format("/lol-chat/v1/conversations/%s/messages", conversationID));
        return sendTypeRequest(request, new TypeReference<>() {
        });
    }

    /**
     * 查询对局状态
     */
    public GameFlowSession queryGameFlowSession() throws IOException {
        Request request = OkHttpUtil.createOkHttpGetRequest("/lol-gameflow/v1/session");
        return sendRequest(request, GameFlowSession.class);
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
    public Boolean banChampion(int championId, int actionId) {
        return champSelectPatchAction(
                championId,
                actionId,
                Constant.CHAMP_SELECT_PATCH_TYPE_BAN,
                true
        );
    }


    /**
     * 通用英雄选择操作
     */
    public Boolean champSelectPatchAction(int championId, int actionId, String patchType, Boolean completed) {
        if (championId <= 0) {
            log.error("通用英雄选择操作, championId 为空！");
            return false;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("championId", championId);
        Optional.ofNullable(patchType).ifPresent(t -> body.put("type", t));
        Optional.ofNullable(completed).ifPresent(c -> body.put("completed", c));
        Request request = OkHttpUtil.createOkHttpPatchRequest("/lol-champ-select/v1/session/actions/" + actionId, body);

        try (Response response = client.newCall(request).execute()) {
            boolean successful = response.isSuccessful();
            if (!successful) {
                log.error("champSelectPatchActionError，championId: {}, actionId: {}, patchType: {}, completed: {}, response: {}",
                        championId, actionId, patchType, completed, response);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("champSelectPatchActionIOError", e);
            return false;
        }
    }
}
