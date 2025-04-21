package com.example.huayeloltool.service;

import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.GameInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
public class GameAnalysis {
    private static final int TARGET_STREAK = 3;
    private static final int MIN_RANKED_GAMES = 3;

    // 分析因子定义
    private enum AnalysisFactor {
        // 格式：触发条件, 队友描述模板, 对手描述模板, 优先级
        LOSING_STREAK(
                count -> count >= TARGET_STREAK,
                "达成排位%d连败",
                "出现排位%d连败",
                1
        ),
        WINNING_STREAK(
                count -> count >= TARGET_STREAK,
                "状态火热%d连胜",
                "保持%d连胜威胁",
                2
        ),
        LOW_RANK_GAMES(
                count -> count < MIN_RANKED_GAMES,
                "近期排位场次不足（仅%d场）",
                "排位经验不足（仅%d场）",
                3
        );

        final TriggerCondition condition;
        final String teammateTemplate;
        final String opponentTemplate;
        final int priority;

        AnalysisFactor(TriggerCondition condition, String teammate, String opponent, int priority) {
            this.condition = condition;
            this.teammateTemplate = teammate;
            this.opponentTemplate = opponent;
            this.priority = priority;
        }

        interface TriggerCondition {
            boolean trigger(int count);
        }
    }

    public static String analyzeGameHistory(List<GameInfo> gameInfoList, String gameName, boolean isTeammate) {
        // 1. 数据预处理：提取排位赛结果（按时间倒序）
        List<Boolean> rankedResults = gameInfoList.stream()
                .filter(g -> g.getQueueId() == GameEnums.GameQueueID.RANK_SOLO.getId())
                .map(g -> g.getParticipants().get(0).getStats().getWin())
                .collect(Collectors.toList());

        // 2. 计算关键指标
        StreakResult streak = calculateStreaks(rankedResults);
        int rankedCount = rankedResults.size();

        // 3. 检测触发因子
        List<FactorResult> triggeredFactors = new ArrayList<>();

        // 连败检测
        if (AnalysisFactor.LOSING_STREAK.condition.trigger(streak.maxLosing)) {
            triggeredFactors.add(new FactorResult(
                    AnalysisFactor.LOSING_STREAK,
                    streak.maxLosing
            ));
        }

        // 连胜检测
        if (AnalysisFactor.WINNING_STREAK.condition.trigger(streak.maxWinning)) {
            triggeredFactors.add(new FactorResult(
                    AnalysisFactor.WINNING_STREAK,
                    streak.maxWinning
            ));
        }

        // 排位场次检测
        if (AnalysisFactor.LOW_RANK_GAMES.condition.trigger(rankedCount)) {
            triggeredFactors.add(new FactorResult(
                    AnalysisFactor.LOW_RANK_GAMES,
                    rankedCount
            ));
        }

        // 4. 生成提示信息
        if (!triggeredFactors.isEmpty()) {
            return buildMessage(triggeredFactors, gameName, isTeammate);
        }
        return "无扩展信息";
    }

    // 计算连胜/连败记录
    private static StreakResult calculateStreaks(List<Boolean> results) {
        int maxWin = 0, maxLose = 0;
        int currentWin = 0, currentLose = 0;

        for (Boolean win : results) {
            if (win) {
                currentWin++;
                currentLose = 0;
                maxWin = Math.max(maxWin, currentWin);
            } else {
                currentLose++;
                currentWin = 0;
                maxLose = Math.max(maxLose, currentLose);
            }
        }
        return new StreakResult(maxWin, maxLose);
    }

    // 构建复合提示消息
    private static String buildMessage(List<FactorResult> factors, String name, boolean isTeammate) {
        // 按优先级排序（数值小的优先）
        factors.sort(Comparator.comparingInt(f -> f.factor.priority));

        // 最多取前两个因子
        List<FactorResult> displayFactors = factors.stream()
                .limit(2)
                .collect(Collectors.toList());

        // 生成消息前缀
        String prefix = isTeammate ? "警告：队友 " : "恭喜：对手 ";
        StringJoiner msgJoiner = new StringJoiner("，", prefix + name + " ", "。");

        // 生成因子描述
        for (FactorResult factor : displayFactors) {
            String template = isTeammate ?
                    factor.factor.teammateTemplate :
                    factor.factor.opponentTemplate;
            msgJoiner.add(String.format(template, factor.value));
        }

        // 添加战术建议
        msgJoiner.add(generateAdvice(displayFactors, isTeammate));
        return msgJoiner.toString();
    }

    // 生成动态建议
    private static String generateAdvice(List<FactorResult> factors, boolean isTeammate) {
        List<String> advises = new ArrayList<>();

        for (FactorResult factor : factors) {
            switch (factor.factor) {
                case LOSING_STREAK:
                    advises.add(isTeammate ? "建议多多支援" : "建议优先针对");
                    break;
                case WINNING_STREAK:
                    advises.add(isTeammate ? "建议围绕作战" : "建议避其锋芒");
                    break;
                case LOW_RANK_GAMES:
                    advises.add(isTeammate ? "建议谨慎观察" : "可尝试压制");
                    break;
            }
        }
        return "请" + String.join("并", advises);
    }

    // 辅助数据结构
    private static class StreakResult {
        final int maxWinning;
        final int maxLosing;

        StreakResult(int win, int lose) {
            this.maxWinning = win;
            this.maxLosing = lose;
        }
    }

    private static class FactorResult {
        final AnalysisFactor factor;
        final int value;

        FactorResult(AnalysisFactor factor, int value) {
            this.factor = factor;
            this.value = value;
        }
    }


}
