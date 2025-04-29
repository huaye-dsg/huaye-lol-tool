package com.example.huayeloltool.model.score.calc;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.model.game.Participant;
import lombok.Getter;

import java.util.*;
import java.util.stream.Stream;

/**
 * LOL玩家综合评分计算器
 * 支持多场游戏数据聚合分析
 * 输出0-200分区间，包含6个评级层次
 */
public class LolPlayerScorer {

    // 各维度最大分值配置
    private static final int BASE_KILL_ASSIST_MAX = 30;     // 基础击杀/助攻上限
    private static final int DAMAGE_OUTPUT_MAX = 40;         // 伤害输出上限
    private static final int TEAM_CONTRIBUTION_MAX = 30;     // 团队贡献上限
    private static final int ECONOMY_EFFICIENCY_MAX = 20;    // 经济效率上限
    private static final int SURVIVAL_RATE_MAX = 20;         // 生存能力上限
    private static final int VISION_CONTROL_MAX = 20;        // 视野控制上限

    // 角色类型枚举
    public enum RoleType {
        TOP("上单", 0.4, 0.3, 0.2, 0.1),          // 物伤权重:团队贡献:经济效率:生存
        JUNGLE("打野", 0.3, 0.4, 0.2, 0.1),
        MIDDLE("中单", 0.35, 0.3, 0.25, 0.1),
        BOTTOM("下路", 0.4, 0.25, 0.25, 0.1),
        SUPPORT("辅助", 0.2, 0.4, 0.1, 0.3);

        @Getter
        private final String roleName;
        private final double[] weightConfig; // [物伤权重, 团队贡献, 经济效率, 生存]

        RoleType(String name, double... weights) {
            this.roleName = name;
            this.weightConfig = Arrays.copyOf(weights, weights.length);
        }

        public double getWeight(int index) {
            return weightConfig[index];
        }
    }

    /**
     * 计算单场游戏评分
     *
     * @param participant 参与者数据
     * @return 单场评分详情
     */
    public static Map<String, Object> calculateGameScore(Participant participant) {
        Participant.Stats stats = participant.getStats();
        Participant.Timeline timeline = participant.getTimeline();

        // 自动识别角色类型
        RoleType role = identifyRole(participant.getTimeline().getLane(),
                participant.getTimeline().getRole(),
                stats.getItem0(),
                stats.getItem5());

        // 计算各项基础得分
        double baseKillAssist = calculateBaseKillAssist(stats.getKills(), stats.getAssists(), role);
        double damageOutput = calculateDamageOutput(stats, role);
        double teamContribution = calculateTeamContribution(stats, timeline);
        double economyEfficiency = calculateEconomyEfficiency(stats);
        double survivalRate = calculateSurvivalRate(stats.getDeaths(), stats.getKills(), stats.getAssists());
        double visionControl = calculateVisionControl(stats.getVisionScore(), stats.getWardsPlaced(), stats.getWardsKilled());

        // 动态调整权重
        double totalScore = normalizeScore(baseKillAssist, BASE_KILL_ASSIST_MAX) *
                +normalizeScore(damageOutput, DAMAGE_OUTPUT_MAX) *
                +normalizeScore(teamContribution, TEAM_CONTRIBUTION_MAX) *
                +normalizeScore(economyEfficiency, ECONOMY_EFFICIENCY_MAX) *
                +normalizeScore(survivalRate, SURVIVAL_RATE_MAX) *
                +normalizeScore(visionControl, VISION_CONTROL_MAX);

        // 构建评分详情
        Map<String, Object> result = new HashMap<>();
        result.put("baseKillAssist", Math.round(normalizeScore(baseKillAssist, BASE_KILL_ASSIST_MAX)));
        result.put("damageOutput", Math.round(normalizeScore(damageOutput, DAMAGE_OUTPUT_MAX)));
        result.put("teamContribution", Math.round(normalizeScore(teamContribution, TEAM_CONTRIBUTION_MAX)));
        result.put("economyEfficiency", Math.round(normalizeScore(economyEfficiency, ECONOMY_EFFICIENCY_MAX)));
        result.put("survivalRate", Math.round(normalizeScore(survivalRate, SURVIVAL_RATE_MAX)));
        result.put("visionControl", Math.round(normalizeScore(visionControl, VISION_CONTROL_MAX)));
        result.put("total", Math.round(totalScore));
        result.put("role", role.getRoleName());

        return result;
    }

    /**
     * 聚合多场游戏评分
     *
     * @param gameScores 多场评分记录
     * @return 最终评分及等级评定
     */
    public static Map<String, Object> aggregateScores(List<Map<String, Object>> gameScores) {
        if (gameScores == null || gameScores.isEmpty()) {
            throw new IllegalArgumentException("评分记录不能为空");
        }

        // 计算各维度平均分
        Map<String, Integer> categoryAverages = new HashMap<>();
        int totalGames = gameScores.size();

        for (String key : Arrays.asList("baseKillAssist", "damageOutput", "teamContribution",
                "economyEfficiency", "survivalRate", "visionControl")) {
            int sum = 0;
            for (Map<String, Object> score : gameScores) {
                sum += (Integer) score.get(key);
            }
            categoryAverages.put(key, sum / totalGames);
        }

        // 计算总分(0-200)
        int totalScore = Stream.of("baseKillAssist", "damageOutput", "teamContribution",
                        "economyEfficiency", "survivalRate", "visionControl")
                .mapToInt(categoryAverages::get)
                .sum();

        // 等级划分
        String rating = determineRating(totalScore);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("分数", totalScore);
        result.put("马匹", rating);
        result.put("原因", categoryAverages);
        result.put("建议", generateImprovementSuggestion(categoryAverages, rating));

        return result;
    }

    /**
     * 归一化评分函数
     */
    private static double normalizeScore(double value, double maxValue) {
        return Math.min(value / maxValue, 1.0);
    }

    /**
     * 自动识别角色类型
     */
    private static RoleType identifyRole(String lane, String role, int item0, int item5) {
        if ("UTILITY".equals(role)) return RoleType.SUPPORT;
        if ("JUNGLE".equals(lane)) return RoleType.JUNGLE;

        return switch (lane) {
            case "TOP" -> RoleType.TOP;
            case "MID" -> RoleType.MIDDLE;
            case "BOTTOM" -> {
                // 判断是否为辅助位（购买假眼数量）
                if (item0 == 3340 || item5 == 3340) {
                    yield RoleType.SUPPORT;
                }
                yield RoleType.BOTTOM;
            }
            default -> RoleType.BOTTOM;
        };
    }

    /**
     * 基础击杀/助攻评分计算
     */
    private static double calculateBaseKillAssist(int kills, int assists, RoleType role) {
        double killWeight = role.getWeight(0) * 1.5;
        double assistWeight = role.getWeight(0) * 1.2;

        return (kills * killWeight) + (assists * assistWeight);
    }

    /**
     * 伤害输出评分计算
     */
    private static double calculateDamageOutput(Participant.Stats stats, RoleType role) {
        double physicalRatio = (double) stats.getPhysicalDamageDealtToChampions() /
                stats.getTotalDamageDealtToChampions();

        double magicRatio = (double) stats.getMagicDamageDealtToChampions() /
                stats.getTotalDamageDealtToChampions();

        double damageWeight = role.getWeight(0);

        return (stats.getTotalDamageDealtToChampions() * 0.0001) *
                (physicalRatio * 0.6 + magicRatio * 0.4) * damageWeight;
    }

    /**
     * 团队贡献评分计算
     */
    private static double calculateTeamContribution(Participant.Stats stats, Participant.Timeline timeline) {
        double objectiveParticipation = (stats.getInhibitorKills() + stats.getTurretKills()) * 10;
        double csDiff = timeline.getCsDiffPerMinDeltas().getTen() +
                timeline.getCsDiffPerMinDeltas().getTwenty() +
                timeline.getCsDiffPerMinDeltas().getThirty();

        return (objectiveParticipation + csDiff * 2) * 0.75;
    }

    /**
     * 经济效率评分计算
     */
    private static double calculateEconomyEfficiency(Participant.Stats stats) {
        return ((stats.getGoldEarned() - stats.getGoldSpent()) * 0.0001) +
                (stats.getTotalMinionsKilled() * 0.2);
    }

    /**
     * 生存能力评分计算
     */
    private static double calculateSurvivalRate(int deaths, int kills, int assists) {
        if (deaths == 0) return 20;
        return (karmaFactor(kills, assists) * (100 - deaths * 5)) / 100.0;
    }

    /**
     * K/D/A Karma因子计算
     */
    private static double karmaFactor(int kills, int assists) {
        double kda = (kills + assists) / (double) (Math.max(1, kills + assists));
        return kda > 0.7 ? 1.2 : (kda > 0.4 ? 1.0 : 0.8);
    }

    /**
     * 视野控制评分计算
     */
    private static double calculateVisionControl(int visionScore, int wardsPlaced, int wardsKilled) {
        return (visionScore * 0.05) +
                (wardsPlaced * 0.1) -
                (wardsKilled * 0.05);
    }

    /**
     * 等级判定
     */
    private static String determineRating(int score) {
        if (score >= 180) return "通天代";
        if (score >= 160) return "小代";
        if (score >= 140) return "上等马";
        if (score >= 110) return "中等马";
        if (score >= 80) return "下等马";
        return "牛 马";
    }

    /**
     * 生成改进建议
     */
    private static String generateImprovementSuggestion(Map<String, Integer> scores, String rating) {
        StringBuilder suggestion = new StringBuilder();

        if (rating.equals("通天代")) {
            return "继续保持当前表现，注意维持团队协作平衡";
        }

        if (scores.get("baseKillAssist") < 20) {
            suggestion.append("需要提升基本功，加强补刀和击杀意识\n");
        }
        if (scores.get("damageOutput") < 30) {
            suggestion.append("需提高输出效率，注意装备选择时机\n");
        }
        if (scores.get("teamContribution") < 20) {
            suggestion.append("应更多参与团队目标，提升地图理解\n");
        }
        if (scores.get("economyEfficiency") < 15) {
            suggestion.append("注意资源分配，避免过度消费\n");
        }
        if (scores.get("survivalRate") < 15) {
            suggestion.append("需提高生存意识，减少无意义死亡\n");
        }
        if (scores.get("visionControl") < 15) {
            suggestion.append("加强视野布置，避免被gank\n");
        }

        return suggestion.toString();
    }

    // 示例调用方法
    public static void main(String[] args) {
        // 模拟数据初始化...
        List<Map<String, Object>> mockScores = new ArrayList<>();
        Map<String, Object> score1 = new HashMap<>();
        score1.put("baseKillAssist", 25);
        score1.put("damageOutput", 35);
        score1.put("teamContribution", 28);
        score1.put("economyEfficiency", 18);
        score1.put("survivalRate", 19);
        score1.put("visionControl", 17);
        mockScores.add(score1);

        Map<String, Object> score2 = new HashMap<>();
        score2.put("baseKillAssist", 20);
        score2.put("damageOutput", 30);
        score2.put("teamContribution", 25);
        score2.put("economyEfficiency", 16);
        score2.put("survivalRate", 18);
        score2.put("visionControl", 16);
        mockScores.add(score2);

        // 计算最终评分
        Map<String, Object> result = aggregateScores(mockScores);
        System.out.println(JSON.toJSONString(result));
    }
}
