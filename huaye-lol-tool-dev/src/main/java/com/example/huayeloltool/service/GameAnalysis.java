package com.example.huayeloltool.service;

import com.example.huayeloltool.model.game.GameHistory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GameAnalysis {

    public static String analyzeGameHistory(List<GameHistory.GameInfo> gameInfoList, String gameName, boolean isTeammate) {
        // 1. 数据预处理：提取排位赛结果（按时间倒序）

        StringBuilder msg = new StringBuilder();

        // 判断前三场是不是排位并且全部失败
        List<GameHistory.GameInfo> gameInfos = gameInfoList.subList(0, 3);
        List<Boolean> rankedResults = gameInfos.stream()
                .map(gameInfo -> gameInfo.getQueueId() == 420)
                .collect(Collectors.toList());
        boolean allFailed = rankedResults.stream().noneMatch(result -> result);
        if (allFailed) {
            if (isTeammate) {
                msg.append("警告：队友").append(gameName).append("排位三连败，建议谨慎对局");
            }else {
                msg.append("恭喜：对手").append(gameName).append("排位三连败，建议优先针对");
            }
        }

        List<Boolean> collect = gameInfos.stream()
                .map(gameInfo -> gameInfo.getQueueId() == 420)
                .collect(Collectors.toList());
        if (collect.size() < 3) {
            if (isTeammate) {
                msg.append("警告：队友").append(gameName).append("近期排位场次不足3场，建议谨慎观察");
            }else {
                msg.append("恭喜：对手").append(gameName).append("排位场次不足不足3场，建议尝试压制");
            }
        }
        return msg.toString();
    }
}
