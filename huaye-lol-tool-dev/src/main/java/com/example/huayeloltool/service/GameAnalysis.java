package com.example.huayeloltool.service;


import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.game.GameHistory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Slf4j
public class GameAnalysis {

    public static String analyzeGameHistory(List<GameHistory.GameInfo> gameInfoList, String gameName, boolean isTeammate) {
        // 1. 数据预处理：提取排位赛结果（按时间倒序）

        try {
            StringBuilder msg = new StringBuilder();
            if (CollectionUtils.isEmpty(gameInfoList) || gameInfoList.size() < 3) {
                if (isTeammate) {
                    msg.append("警告：队友").append(gameName).append("近期游戏场次不足3场，建议谨慎观察");
                } else {
                    msg.append("恭喜：对手").append(gameName).append("近期场次不足不足3场，建议尝试压制");
                }
            } else {

                // 判断前三场是不是排位并且全部失败
                List<GameHistory.GameInfo> gameInfos = gameInfoList.subList(0, 3);
                List<Boolean> rankedResults = gameInfos.stream()
                        .map(gameInfo -> gameInfo.getQueueId() == 420)
                        .toList();
                boolean allFailed = rankedResults.stream().noneMatch(result -> result);
                if (allFailed) {
                    if (isTeammate) {
                        msg.append("警告：队友").append(gameName).append("排位三连败，建议谨慎对局");
                    } else {
                        msg.append("恭喜：对手").append(gameName).append("排位三连败，建议优先针对");
                    }
                }

                List<Boolean> collect = gameInfoList.stream()
                        .map(gameInfo -> gameInfo.getQueueId() == GameEnums.GameQueueID.RANK_SOLO.getId())
                        .toList();
                if (collect.size() < 3) {
                    if (isTeammate) {
                        msg.append("警告：队友").append(gameName).append("近20场游戏排位场次不足3场，建议谨慎观察");
                    } else {
                        msg.append("恭喜：对手").append(gameName).append("近20场游戏场次不足不足3场，建议尝试压制");
                    }
                }
            }

            return msg.toString();
        } catch (Exception e) {
            log.error("分析连败/近期排位情况失败", e);
            return "分析连败/近期排位情况失败";
        }
    }
}
