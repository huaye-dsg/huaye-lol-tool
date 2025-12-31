package com.example.huayeloltool.service;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.Participant;
import com.example.huayeloltool.model.score.UserScore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 游戏历史分析服务
 * 
 * <p>负责分析召唤师的游戏历史，包括：
 * - 提取KDA信息
 * - 分析连胜连败情况
 * - 位置识别
 * - 游戏模式判断
 */
@Slf4j
@Service
public class GameHistoryAnalyzer {

    /**
     * 从游戏列表中提取KDA信息
     * 
     * @param gameList 游戏历史列表
     * @return KDA信息列表
     */
    public static List<UserScore.Kda> extractKdaList(List<GameHistory.GameInfo> gameList) {
        if (CollectionUtils.isEmpty(gameList)) {
            return List.of();
        }

        return gameList.stream().map(gameInfo -> {
            try {
                Participant participant = gameInfo.getParticipants().get(0);
                Participant.Stats stats = participant.getStats();
                
                UserScore.Kda kda = new UserScore.Kda();
                kda.setKills(stats.getKills());
                kda.setDeaths(stats.getDeaths());
                kda.setAssists(stats.getAssists());
                kda.setWin(stats.getWin());
                kda.setQueueGame(GameEnums.GameQueueID.getGameNameMap(gameInfo.getQueueId()));
                kda.setChampionName(Heros.getNameById(participant.getChampionId()));
                kda.setChampionId(participant.getChampionId());
                kda.setPosition(getPositionFromLaneAndRole(
                    participant.getTimeline().getLane(), 
                    participant.getTimeline().getRole()
                ));
                return kda;
            } catch (Exception e) {
                log.warn("提取KDA信息失败: gameId={}", gameInfo.getGameId(), e);
                return null;
            }
        })
        .filter(kda -> kda != null)
        .toList();
    }

    /**
     * 分析游戏历史，判断连胜连败情况
     * 
     * @param gameInfoList 游戏信息列表
     * @param gameName 召唤师名称
     * @param isTeammate 是否为队友
     * @return 分析结果描述
     */
    public static String analyzeGameHistory(List<GameHistory.GameInfo> gameInfoList, String gameName, boolean isTeammate) {
        if (CollectionUtils.isEmpty(gameInfoList)) {
            return "";
        }
        
        if (gameInfoList.size() < Constant.ANALYSIS_GAME_COUNT) {
            return "";
        }

        // 取前3场游戏进行分析
        List<GameHistory.GameInfo> gameInfos = gameInfoList.subList(0, Constant.ANALYSIS_GAME_COUNT);
        
        // 检查是否都是正常游戏模式
        boolean isAllSoloQueue = gameInfos.stream()
                .allMatch(gameInfo -> GameEnums.GameQueueID.isNormalGameMode(gameInfo.getQueueId()));
        
        if (!isAllSoloQueue) {
            return "";
        }

        // 分析胜负情况
        boolean allWin = gameInfoList.stream()
                .filter(item -> !CollectionUtils.isEmpty(item.getParticipants()))
                .allMatch(item -> item.getParticipants().get(0).getStats().getWin());
                
        boolean allLose = gameInfoList.stream()
                .filter(item -> !CollectionUtils.isEmpty(item.getParticipants()))
                .noneMatch(item -> item.getParticipants().get(0).getStats().getWin());

        if (allWin || allLose) {
            return generateAnalysisMessage(gameName, isTeammate, allWin);
        }
        
        return "";
    }

    /**
     * 根据路线和角色判断位置
     * 
     * @param lane 路线（TOP, JUNGLE, MIDDLE, BOTTOM, NONE）
     * @param role 角色（SOLO, DUO_CARRY, DUO_SUPPORT, NONE）
     * @return 位置描述（上单、打野、中单、ADC、辅助）
     */
    public static String getPositionFromLaneAndRole(String lane, String role) {
        // 检查输入参数
        if (StringUtils.isBlank(lane) || StringUtils.isBlank(role)) {
            return "";
        }
        
        lane = lane.toUpperCase();
        role = role.toUpperCase();

        switch (lane) {
            case "TOP":
                if ("SOLO".equals(role)) return "上单";
                break;
            case "JUNGLE":
                // 打野的role字段通常为"NONE"或空
                return "打野";
            case "MIDDLE":
                if ("SOLO".equals(role)) return "中单";
                break;
            case "BOTTOM":
                if ("DUO_CARRY".equals(role) || "SOLO".equals(role)) return "ADC";
                if ("DUO_SUPPORT".equals(role)) return "辅助";
                break;
            case "NONE":
                // 兼容部分对局，通常不能断定位置
                return "";
        }
        
        // 未匹配到的组合，返回空字符串
        return "";
    }

    /**
     * 生成分析消息
     * 
     * @param gameName 召唤师名称
     * @param isTeammate 是否为队友
     * @param isWinStreak 是否为连胜
     * @return 分析消息
     */
    private static String generateAnalysisMessage(String gameName, boolean isTeammate, boolean isWinStreak) {
        if (isTeammate) {
            if (isWinStreak) {
                return "恭喜！队友：" + gameName + "三连胜，请积极对局";
            } else {
                return "警告！队友：" + gameName + "三连跪，请谨慎对局";
            }
        } else {
            if (isWinStreak) {
                return "警告！对手：" + gameName + "三连胜，请注意针对";
            } else {
                return "恭喜！对手：" + gameName + "三连跪，请注意针对";
            }
        }
    }
}