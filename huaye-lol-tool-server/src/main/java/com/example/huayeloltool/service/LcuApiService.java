package com.example.huayeloltool.service;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.example.huayeloltool.common.CommonRequest;
import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.conversation.Conversation;
import com.example.huayeloltool.model.conversation.ConversationMsg;
import com.example.huayeloltool.model.champion.ChampionMastery;
import com.example.huayeloltool.model.game.GameFlowSession;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.game.GameTimeLine;
import com.example.huayeloltool.model.summoner.RankedInfo;
import com.example.huayeloltool.model.summoner.Summoner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.huayeloltool.enums.GameEnums.GameFlow.CHAMPION_SELECT;

@Slf4j
@Service
public class LcuApiService extends CommonRequest {

    /**
     * 当前用户信息
     */
    public Summoner getCurrSummoner() {
        try {
            return sendSingleObjectGetRequest("/lol-summoner/v1/current-summoner", Summoner.class);
        } catch (Exception e) {
            log.error("getCurrSummoner请求错误", e);
            return null;
        }
    }


    /**
     * 有重试机制的查询对局详情方法
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
     * 根据 puuid 查询对局记录
     */
    public List<GameHistory.GameInfo> listGameHistory(Summoner summoner, int begin, int limit) {
        List<GameHistory.GameInfo> fmtList = new ArrayList<>();
        GameHistory gameHistory = listGamesByPUUID(summoner.getPuuid(), begin, limit);

        if (Objects.isNull(gameHistory)) {
            log.error("查询用户战绩失败: {}", summoner.getGameName());
            return new ArrayList<>();
        }

        GameHistory.Games games = gameHistory.getGames();
        if (Objects.isNull(games)) {
            log.error("【风险警告】查询用户{}战绩为空！", summoner.getGameName());
            return new ArrayList<>();
        }

        List<GameHistory.GameInfo> gameList = games.getGames();

        if (CollectionUtils.isEmpty(gameList)) {
            log.error("【风险警告】查询用户{}战绩为空！", summoner.getGameName());
            return new ArrayList<>();
        }

        for (GameHistory.GameInfo gameItem : gameList) {
            // 过滤时长短的无效对局
            if (gameItem.getGameDuration() > 300) {
                fmtList.add(gameItem);
            }
        }
        return fmtList;
    }


    // 对局历史列表
    public GameHistory listGamesByPUUID(String puuid, int begin, int limit) {
        String url = String.format("/lol-match-history/v1/products/lol/%s/matches?begIndex=%d&endIndex=%d", puuid, begin, begin + limit);
        return sendSingleObjectGetRequest(url, GameHistory.class);
    }

    /**
     * 对局时间线详情，数据较多
     */
    public GameTimeLine getGameTimelines(long gameID) {
        return sendSingleObjectGetRequest(String.format("/lol-match-history/v1/game-timelines/%d", gameID), GameTimeLine.class);
    }

    /**
     * 查询段位信息
     */
    public RankedInfo getRankData(String puuid) {
        try {
            return sendSingleObjectGetRequest("/lol-ranked/v1/ranked-stats/" + puuid, RankedInfo.class);
        } catch (Exception e) {
            log.error("查询段位信息失败！", e);
        }
        return null;
    }

    /**
     * 查询英雄池熟练度列表
     */
    public List<ChampionMastery> searchChampionMasteryData(String puuid) {
        List<ChampionMastery> championMasteryList = sendTypeGetRequest(String.format("/lol-champion-mastery/v1/" + puuid + "/champion-mastery"), new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(championMasteryList)) {
            log.error("查询英雄熟练度失败！");
            return new ArrayList<>();
        }

        championMasteryList.sort(Comparator.comparingInt(ChampionMastery::getChampionLevel).reversed());
        return championMasteryList;
    }

    /**
     * 获取召唤师信息
     */
    public List<Summoner> listSummoner(List<Long> summonerIDList) {
        List<String> idStrList = summonerIDList.stream().map(String::valueOf).collect(Collectors.toList());
        return sendTypeGetRequest(String.format("/lol-summoner/v2/summoners?ids=[%s]", String.join(",", idStrList)), new TypeReference<>() {
        });
    }

    /**
     * 根据会话ID获取会话组消息记录
     */
    public List<ConversationMsg> listConversationMsg(String conversationID) {
        return sendTypeGetRequest(String.format("/lol-chat/v1/conversations/%s/messages", conversationID), new TypeReference<>() {
        });
    }


    /**
     * 查询对局详情
     */
    public GameSummary queryGameSummary(long gameID) {
        return sendSingleObjectGetRequest(String.format("/lol-match-history/v1/games/%d", gameID), GameSummary.class);
    }

    /**
     * 自动接受对局
     */
    public void acceptGame() {
        try {
            Boolean result = sendPostRequest("/lol-matchmaking/v1/ready-check/accept");
            if (!result) {
                log.info("自动接受对局失败: {}", false);
            }
        } catch (Exception e) {
            log.error("自动接受对局失败!", e);
        }
    }

    /**
     * 获取本人当前会话ID
     */
    public String getCurrConversationID() {
        List<Conversation> conversations = sendTypeGetRequest("/lol-chat/v1/conversations", new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(conversations)) {
            log.info("当前未查询到会话信息");
            return StringUtils.EMPTY;
        }

        for (Conversation conversation : conversations) {
            if (CHAMPION_SELECT.equals(conversation.getType())) {
                return conversation.getId();
            }
        }
        log.info("当前未查询到会话信息：{}", JSON.toJSONString(conversations));
        return StringUtils.EMPTY;
    }


    /**
     * 查询对局状态
     */
    public GameFlowSession queryGameFlowSession() {
        return sendSingleObjectGetRequest("/lol-gameflow/v1/session", GameFlowSession.class);
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
    @SneakyThrows
    public Boolean champSelectPatchAction(int championId, int actionId, String patchType, Boolean completed) {
        if (championId <= 0) {
            log.error("通用英雄选择操作, championId 为空！");
            return false;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("championId", championId);
        body.put("completed", completed);
        body.put("type", patchType);
        return sendPatchRequest("/lol-champ-select/v1/session/actions/" + actionId, body);
    }

    /**
     * 再来一局（回到大厅）
     */
    public boolean playAgain() {
        try {
            return sendPostRequest("/lol-lobby/v2/play-again");
        } catch (Exception e) {
            log.error("回到大厅失败!", e);
            return false;
        }
    }

    /**
     * 自动开始匹配
     */
    public void autoStartMatch() {
        try {
            sendPostRequest("/lol-lobby/v2/lobby/matchmaking/search");
        } catch (Exception e) {
            log.error("自动开始匹配失败!", e);
        }
    }

    /**
     * 根据名字获取召唤师信息
     */
    @SneakyThrows
    public Summoner getSummonerByNickName(String name, String tagLine) {
        String encodedParam = URLEncoder.encode(name + "#" + tagLine, StandardCharsets.UTF_8);
        return sendSingleObjectGetRequest(String.format("/lol-summoner/v1/summoners/?name=%s", encodedParam), Summoner.class);
    }
}
