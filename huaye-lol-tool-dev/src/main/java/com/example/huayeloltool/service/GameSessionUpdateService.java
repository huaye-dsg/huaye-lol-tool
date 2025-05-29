package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.game.CustomGameSession;
import com.example.huayeloltool.model.base.GameGlobalSetting;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import com.example.huayeloltool.model.champion.ChampionMastery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 解析游戏会话、英雄选择或禁用等消息
 */
@Slf4j
public class GameSessionUpdateService {

    private final static GameGlobalSetting clientCfg = GameGlobalSetting.getInstance();

    private final static CustomGameSession details = CustomGameSession.getInstance();

    private final static LcuApiService lcuApiService = LcuApiService.getInstance();

    public GameSessionUpdateService() {

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

    /**
     * 处理英雄选定事件
     */
    private void handleCompletedAction(ChampSelectSessionInfo.Action action, Map<Integer, ChampSelectSessionInfo.Player> posMap) {
        boolean isPick = "pick".equals(action.getType());
        boolean isAlly = action.getIsAllyAction();

        if (!(isPick && isAlly)) {
            // 只关注我方选择英雄
            return;
        }

        String positionDesc = Optional.ofNullable(posMap.get(action.getActorCellId()))
                .map(p -> GameEnums.Position
                        .getDescByValue(p.getAssignedPosition()))
                .orElse("未知");
        String which = isPick ? "选择（锁定）英雄" : "禁用英雄";
        String heroName = Heros.getNameById(action.getChampionId());

        String logMsg = isAlly
                //? String.format("【我方】位置：%s, 动作: %s, 英雄: %s", positionDesc, which, heroName)
                ? String.format("【我方】动作: %s, 英雄: %s", which, heroName)
                : String.format("【敌方】动作: %s, 英雄: %s", which, heroName);

        if (isPick && isAlly) {
            ChampSelectSessionInfo.Player player = posMap.get(action.getActorCellId());
            // 分析该玩家对这个英雄的熟练度
            logMsg = analyzeHeroMastery(player.getPuuid(), logMsg, action.getChampionId());
        }
        log.info(logMsg);
    }

    // 自动 ban / pick
    private void handleSelfAction(ChampSelectSessionInfo.Action action, String actionKey) {
        String type = action.getType();
        int id = action.getId();

        switch (type) {
            case "ban":
                if (clientCfg.getAutoBanChampID() > 0 && !GameSessionUpdateService.details.getIsBanned()) {
                    sleepSeconds();
//                    log.info("本人禁用英雄，key：{}", buildActionKey(action));
                    if (lcuApiService.banChampion(clientCfg.getAutoBanChampID(), id)) {
//                        log.info("禁用成功");
                        GameSessionUpdateService.details.setIsBanned(true);
                        //action.setCompleted(true);
                    } else {
//                        log.info("禁用失败: {}", JSON.toJSONString(action));
                        GameSessionUpdateService.details.setIsBanned(false);
                        // 没成功就把key删了
                        GameSessionUpdateService.details.markActionUnProcessed(actionKey);
                    }
                }
                break;
            case "pick":
                if (clientCfg.getAutoPickChampID() > 0 && !GameSessionUpdateService.details.getIsSelected()) {
//                    log.info("本人选择英雄，key：{}", buildActionKey(action));
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


    private String analyzeHeroMastery(String puuid, String logMessage, int championId) {
        // 获取玩家的所有英雄熟练度数据
        List<ChampionMastery> championMasteryList = lcuApiService.searchChampionMasteryData(puuid);

        // 查找特定英雄的熟练度信息
        Optional<ChampionMastery> optionalMastery = championMasteryList.stream()
                .filter(mastery -> mastery.getChampionId() == championId)
                .findFirst();

        // 如果找到匹配的英雄熟练度信息，则更新日志消息
        if (optionalMastery.isPresent()) {
            ChampionMastery mastery = optionalMastery.get();
            logMessage += String.format(", 等级: %d, 积分: %d，最后游玩: %s",
                    mastery.championLevel, mastery.championPoints,
                    convertTimestampToDate(mastery.lastPlayTime));
        }

        return logMessage;
    }

    /**
     * 把毫秒时间戳转为年月日
     */
    private static String convertTimestampToDate(long timestamp) {
        // 1. 创建一个 Date 对象，传入时间戳
        java.util.Date date = new java.util.Date(timestamp);
        // 2. 使用 SimpleDateFormat 格式化日期
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }


}
