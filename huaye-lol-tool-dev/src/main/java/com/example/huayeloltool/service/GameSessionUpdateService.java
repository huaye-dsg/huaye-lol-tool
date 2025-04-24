package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import com.example.huayeloltool.model.champion.ChampionMastery;
import com.example.huayeloltool.model.base.GamePlaySetting;
import com.example.huayeloltool.model.CustomGameSessionDetails;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 解析游戏会话、英雄选择或禁用等消息
 */
@Service
@Slf4j
public class GameSessionUpdateService {

    private final GamePlaySetting clientCfg = GamePlaySetting.getInstance();

    private final LcuApiService lcuApiService;

    public GameSessionUpdateService(LcuApiService lcuApiService) {
        this.lcuApiService = lcuApiService;
    }

    public void onChampSelectSessionUpdate(String sessionStr) {
        ChampSelectSessionInfo sessionInfo = JSON.parseObject(sessionStr, ChampSelectSessionInfo.class);
        Integer localPlayerCellId = sessionInfo.getLocalPlayerCellId();
        CustomGameSessionDetails.setFloor(localPlayerCellId);
        analyzeSession(sessionInfo);
    }


    public void analyzeSession(ChampSelectSessionInfo session) {
        int localPlayerCellId = session.getLocalPlayerCellId();
        List<List<ChampSelectSessionInfo.Action>> actions = session.getActions();

        List<ChampSelectSessionInfo.Player> myTeam = session.getMyTeam();
        //log.info("localPlayerCellId: {}, myTeam: {}", localPlayerCellId, myTeam);
        // 队友楼层和位置映射
        Map<Integer, ChampSelectSessionInfo.Player> positionMap = myTeam.stream().
                collect(Collectors.toMap(ChampSelectSessionInfo.Player::getCellId, Function.identity()));
        // 解析actions
        for (List<ChampSelectSessionInfo.Action> actionsList : actions) {
            for (ChampSelectSessionInfo.Action action : actionsList) {
                String type = action.getType();
                int actorCellId = action.getActorCellId();
                int championId = action.getChampionId();
                boolean completed = action.getCompleted();
                Boolean isAllyAction = action.getIsAllyAction();
                Integer id = action.getId();

                if (championId <= 0) {
                    continue;
                }

                // 是否本人
                boolean isSelf = actorCellId == localPlayerCellId;

                String actionKey = actorCellId + "_" + id + "_" + type + "_" + completed + "_" + action.getIsInProgress();

                if (completed && !CustomGameSessionDetails.getProcessedActionIds().contains(actionKey)) {
                    CustomGameSessionDetails.addProcessedActionIds(actionKey);

                    boolean isPick = "pick".equals(type);

                    String position = positionMap.get(actorCellId) != null ?
                            GameEnums.Position.getDescByValue(positionMap.get(actorCellId).getAssignedPosition()) : "未知";
                    String logMessage;
                    if (isAllyAction) {
                        logMessage = String.format(
                                "【我方】位置：%s, 动作: %s, 英雄: %s",
                                position,
                                isPick ? "选择（锁定）英雄" : "禁用英雄",
                                Heros.getNameById(championId)
                        );
                    } else {
                        logMessage = String.format(
                                "【敌方】动作: %s, 英雄: %s",
                                isPick ? "选择（锁定）英雄" : "禁用英雄",
                                Heros.getNameById(championId)
                        );
                    }
                    if (isPick && isAllyAction) {
                        logMessage = analyzeHeros(positionMap, actorCellId, logMessage, championId);
                    }
                    log.info(logMessage);
                } else if (!completed && isSelf) {
                    if (CustomGameSessionDetails.isBaned() && CustomGameSessionDetails.isSelected()) {
                        continue;
                    }
                    // 本人操作
                    if ("ban".equals(type) && clientCfg.getAutoBanChampID() > 0 && !CustomGameSessionDetails.isBaned()) {
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException ignored) {
                        }
                        Boolean result = lcuApiService.banChampion(clientCfg.getAutoBanChampID(), action.getId());
                        if (result) {
                            CustomGameSessionDetails.setIsBanned(true);
                            CustomGameSessionDetails.addProcessedActionIds(actionKey);
                            action.setCompleted(true);
                        }
                    } else if ("pick".equals(type) && clientCfg.getAutoPickChampID() > 0 && !CustomGameSessionDetails.isSelected()) {
                        lcuApiService.pickChampion(clientCfg.getAutoPickChampID(), action.getId());
                        CustomGameSessionDetails.setIsSelected(true);
                        action.setCompleted(true);
                    }
                }
            }
        }
    }

    @NotNull
    private String analyzeHeros(Map<Integer, ChampSelectSessionInfo.Player> positionMap, int actorCellId, String logMessage, int championId) {
        // 如果队友选择了英雄，则分析英雄熟练度
        ChampSelectSessionInfo.Player player = positionMap.get(actorCellId);
        String puuid = player.getPuuid();
        List<ChampionMastery> championMasteries = lcuApiService.searchChampionMasteryData(puuid);
        logMessage += String.format(", 熟练度: %s", calculateMasteryScore(championId, championMasteries));

        // 再找出这个人擅长的前5个英雄
        List<String> heros = championMasteries.subList(0, 5).stream().map(item -> Heros.getNameById(item.getChampionId())).collect(Collectors.toList());
        logMessage += String.format("\n 擅长英雄: %s", String.join("，", heros));
        return logMessage;
    }


    public static int calculateMasteryScore(int targetChampionId, List<ChampionMastery> masteryList) {
        try {
            for (ChampionMastery mastery : masteryList) {
                if (mastery.getChampionId() == targetChampionId) {
                    // 等级权重（30%）：满级7级对应30分
                    int levelScore = (int) (mastery.getChampionLevel() / 7.0 * 30);

                    // 熟练度积分权重（40%）：超过50w积分视为满分
                    int pointsScore = (int) (Math.min(mastery.getChampionPoints(), 500000) / 500000.0 * 40);

                    // 最近使用时间权重（20%）：90天内为满分，超过则线性衰减
                    long daysSincePlayed = (System.currentTimeMillis() - mastery.getLastPlayTime()) / (1000 * 86400L);
                    int recencyScore = daysSincePlayed <= 90 ? 20 : Math.max(0, 20 - (int) (daysSincePlayed - 90) / 30);

                    // 最高评级权重（10%）：S+为10分，S为8分，A为5分，其他0分
                    int gradeScore = calculateGradeScore(mastery.getHighestGrade());

                    return levelScore + pointsScore + recencyScore + gradeScore;
                }
            }
            return 0; // 未找到英雄
        } catch (Exception e) {
            log.error("calculateMasteryScore error", e);
            return 0;
        }
    }

    private static int calculateGradeScore(String grade) {
        if (grade == null) return 0;
        Map<String, Integer> gradeScores = new HashMap<>();
        gradeScores.put("S+", 10);
        gradeScores.put("S", 8);
        gradeScores.put("A", 5);

        String upperCaseGrade = grade.toUpperCase();
        return gradeScores.getOrDefault(upperCaseGrade, 0);
    }


}
