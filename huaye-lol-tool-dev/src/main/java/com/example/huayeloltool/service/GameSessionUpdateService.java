package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.base.GamePlaySetting;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import com.example.huayeloltool.model.champion.ChampionMastery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 解析游戏会话、英雄选择或禁用等消息
 */
@Slf4j
public class GameSessionUpdateService {

    private final static GamePlaySetting clientCfg = GamePlaySetting.getInstance();

    private final static CustomGameSession details = CustomGameSession.getInstance();

    private final LcuApiService lcuApiService;

    public GameSessionUpdateService(LcuApiService lcuApiService) {
        this.lcuApiService = lcuApiService;
    }

    public void onChampSelectSessionUpdate(String sessionStr) {
        ChampSelectSessionInfo sessionInfo = JSON.parseObject(sessionStr, ChampSelectSessionInfo.class);
        analyzeSession(sessionInfo);
    }


    public void analyzeSession(ChampSelectSessionInfo session) {
        Map<Integer, ChampSelectSessionInfo.Player> positionMap = details.getPositionMap();
        List<ChampSelectSessionInfo.Player> myTeam = session.getMyTeam();

        // 懒初始化队友映射
        details.initPositionMapIfEmpty(myTeam);

        int localCellId = session.getLocalPlayerCellId();

        for (List<ChampSelectSessionInfo.Action> round : session.getActions()) {
            for (ChampSelectSessionInfo.Action action : round) {
                boolean completed = action.getCompleted();

                // 处理队友和对手锁定英雄的操作
                if (completed && action.getChampionId() > 0) {
                    String actionKey = buildActionKey(action);
                    if (details.markActionProcessed(actionKey)) {
                        handleCompletedAction(action, positionMap);
                    }
                    continue;
                }

                // 只处理自己、且未完成的操作。选择或禁用英雄
                if (!completed && action.getActorCellId() == localCellId && action.getIsInProgress()) {
                    String actionKey = buildActionKey(action);
                    if (details.markActionProcessed(actionKey)) {
                        handleSelfAction(action, actionKey);
                    }
                }
            }
        }
    }

    // 示例：打印 pick/ban
    private void handleCompletedAction(ChampSelectSessionInfo.Action action, Map<Integer, ChampSelectSessionInfo.Player> posMap) {
        boolean isPick = "pick".equals(action.getType());
        boolean isAlly = action.getIsAllyAction();
        String positionDesc = Optional.ofNullable(posMap.get(action.getActorCellId()))
                .map(p -> GameEnums.Position
                        .getDescByValue(p.getAssignedPosition()))
                .orElse("未知");
        String which = isPick ? "选择（锁定）英雄" : "禁用英雄";
        String heroName = Heros.getNameById(action.getChampionId());

        String logMsg = isAlly
                ? String.format("【我方】位置：%s, 动作: %s, 英雄: %s", positionDesc, which, heroName)
                : String.format("【敌方】动作: %s, 英雄: %s", which, heroName);

        if (isPick && isAlly) {
            logMsg = analyzeHeros(posMap, action.getActorCellId(), logMsg, action.getChampionId());
        }
        log.info(logMsg);
    }

    // 示例：自动 ban / pick
    private void handleSelfAction(ChampSelectSessionInfo.Action action, String actionKey) {
        String type = action.getType();
        int id = action.getId();

        switch (type) {
            case "ban":
                if (clientCfg.getAutoBanChampID() > 0 && !GameSessionUpdateService.details.getIsBanned()) {
                    sleepSeconds();
                    log.info("本人禁用英雄，key：{}", buildActionKey(action));
                    if (lcuApiService.banChampion(clientCfg.getAutoBanChampID(), id)) {
                        log.info("禁用成功");
                        GameSessionUpdateService.details.setIsBanned(true);
                        //action.setCompleted(true);
                    } else {
                        log.info("禁用失败: {}", JSON.toJSONString(action));
                        GameSessionUpdateService.details.setIsBanned(false);
                        // 没成功就把key删了
                        GameSessionUpdateService.details.markActionUnProcessed(actionKey);
                    }
                }
                break;
            case "pick":
                if (clientCfg.getAutoPickChampID() > 0 && !GameSessionUpdateService.details.getIsSelected()) {
                    log.info("本人选择英雄，key：{}", buildActionKey(action));
                    lcuApiService.pickChampion(clientCfg.getAutoPickChampID(), id);
                    GameSessionUpdateService.details.setIsSelected(true);
                    //action.setCompleted(true);
                }
                break;
            default:
                // 其他类型忽略
        }
    }

    private void sleepSeconds() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ignored) {
        }
    }

    // 抽取 actionKey 构建
    private String buildActionKey(ChampSelectSessionInfo.Action action) {
        return String.join("_",
                String.valueOf(action.getActorCellId()),
                String.valueOf(action.getId()),
                action.getType(),
                String.valueOf(action.getCompleted()),
                String.valueOf(action.getIsInProgress())
        );
    }


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
