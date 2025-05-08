package com.example.huayeloltool.model.score.calc;

import com.example.huayeloltool.enums.Heros;
import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.game.Participant;
import com.example.huayeloltool.service.LcuApiService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 英雄联盟玩家综合评分系统（含动态基准值与稳定性分析）
 */
@Slf4j
public class LoLPlayerScorer {

    private static LcuApiService lcuApiService = new LcuApiService();

    private static final int BASE_KILL_ASSIST_MAX = 30;     // 基础击杀/助攻上限
    private static final int DAMAGE_OUTPUT_MAX = 40;         // 伤害输出上限
    private static final int TEAM_CONTRIBUTION_MAX = 30;     // 团队贡献上限
    private static final int ECONOMY_EFFICIENCY_MAX = 20;    // 经济效率上限
    private static final int SURVIVAL_RATE_MAX = 20;         // 生存能力上限
    private static final int VISION_CONTROL_MAX = 20;        // 视野控制上限

    // 动态基准值容器（示例：需替换为实时采集数据）
    private static final Map<String, Double> POSITION_BASELINES = new HashMap<>();

    static {
        // 上单位置（TOP）基准值
        POSITION_BASELINES.put("TOP_kda", 2.9);          // 平均 KDA 值 (来自 OP.GG 综合评估) [[5]]
        POSITION_BASELINES.put("TOP_cs_per_min", 7.1);   // 平均每分钟补刀数 [[5]]
        POSITION_BASELINES.put("TOP_damage_per_gold", 1.25); // 每千金币造成伤害 [[5]]
        POSITION_BASELINES.put("TOP_vision_score", 25.0); // 视野得分基准 [[5]]

        // 打野位置（JUNGLE）基准值
        POSITION_BASELINES.put("JUNGLE_kda", 3.1);       // 平均 KDA 值 [[5]]
        POSITION_BASELINES.put("JUNGLE_cs_per_min", 6.8); // 平均每分钟补刀数 [[5]]
        POSITION_BASELINES.put("JUNGLE_damage_per_gold", 1.35); // 每千金币造成伤害 [[5]]
        POSITION_BASELINES.put("JUNGLE_vision_score", 35.0); // 视野得分基准 [[5]]

        // 中单位置（MIDDLE）基准值
        POSITION_BASELINES.put("MIDDLE_kda", 3.3);       // 平均 KDA 值 [[5]]
        POSITION_BASELINES.put("MIDDLE_cs_per_min", 8.2); // 平均每分钟补刀数 [[5]]
        POSITION_BASELINES.put("MIDDLE_damage_per_gold", 1.45); // 每千金币造成伤害 [[5]]
        POSITION_BASELINES.put("MIDDLE_vision_score", 28.0); // 视野得分基准 [[5]]

        // 下路位置（BOTTOM）基准值
        POSITION_BASELINES.put("BOTTOM_kda", 2.7);       // 平均 KDA 值 [[5]]
        POSITION_BASELINES.put("BOTTOM_cs_per_min", 9.0); // 平均每分钟补刀数 [[5]]
        POSITION_BASELINES.put("BOTTOM_damage_per_gold", 1.55); // 每千金币造成伤害 [[5]]
        POSITION_BASELINES.put("BOTTOM_vision_score", 20.0); // 视野得分基准 [[5]]

        // 辅助位置（SUPPORT）基准值
        POSITION_BASELINES.put("SUPPORT_kda", 4.2);      // 平均 KDA 值（侧重助攻）[[5]]
        POSITION_BASELINES.put("SUPPORT_cs", 2.5); // 平均每分钟补刀数（较低）[[5]]
        POSITION_BASELINES.put("SUPPORT_damage_per_gold", 0.95); // 每千金币造成伤害 [[5]]
        POSITION_BASELINES.put("SUPPORT_vision_score", 50.0); // 高视野得分要求 [[5]]
    }

    public static double getFinalScore(List<Long> gameIdList, long summonerID) {
        List<LoLPlayerScorer.GameScore> gameScores = new ArrayList<>(gameIdList.size());
        for (Long gameId : gameIdList) {
            // 查到当局游戏信息
            try {
                GameSummary gameSummary = lcuApiService.queryGameSummaryWithRetry(gameId);
                // 获取用户参与者ID
                int userParticipantId = gameSummary.getParticipantIdentities().stream()
                        .filter(identity -> identity.getPlayer().getSummonerId() == summonerID)
                        .findFirst() // 用于查找到第一个匹配的项
                        .map(GameSummary.ParticipantIdentity::getParticipantId) // 提取ID
                        .orElseThrow(() -> new Exception("获取用户参与者ID失败"));

                List<Participant> participants = gameSummary.getParticipants();

                // 获取用户队伍ID
                Participant userParticipant = participants.stream()
                        .filter(item -> item.getParticipantId() == userParticipantId)
                        .findFirst()
                        .orElseThrow(() -> new Exception("获取用户队伍ID失败"));

                int userTeamID = userParticipant.getTeamId();

                // 获取同队参与者ID列表
                List<Participant> list = participants.stream().filter(item -> item.getTeamId().equals(userTeamID)).toList();

                List<Participant.Stats> statsList = list.stream().map(Participant::getStats).toList();

                GameScore gameScore = calculateGameScore(userParticipant, statsList);
                gameScores.add(gameScore);
            } catch (Exception e) {
                log.error("获取或计算游戏数据失败", e);
            }
        }


        return LoLPlayerScorer.aggregateScores(gameScores);
    }

    // 角色类型枚举（带动态权重）
    public enum RoleType {
        // 物伤:团队:经济:生存
        TOP(0.4, 0.3, 0.2, 0.1),   // 上单
        JUNGLE(0.3, 0.4, 0.2, 0.1), // 打野
        MIDDLE(0.35, 0.3, 0.25, 0.1), // 中路
        BOTTOM(0.4, 0.25, 0.25, 0.1), // 下路
        SUPPORT(0.2, 0.4, 0.1, 0.3); // 辅助

        private final double[] weights;

        RoleType(double... weights) {
            this.weights = Arrays.copyOf(weights, weights.length);
        }

        public double getWeight(int index) {
            return weights[index];
        }
    }

    /**
     * 单场游戏评分详情
     */
    public record GameScore(
            double baseKillAssist,
            double damageOutput,
            double teamContribution,
            double economyEfficiency,
            double survivalRate,
            double visionControl,
            double total,
            RoleType role
    ) {
    }


    /**
     * 计算单场游戏评分（含动态基准值适应）
     */
    public static GameScore calculateGameScore(Participant participant, List<Participant.Stats> teamStats) {
        Participant.Stats stats = participant.getStats();
        Participant.Timeline timeline = participant.getTimeline();

        // 自动识别角色类型
        RoleType role = identifyRole(participant);

        log.info("本局英雄：{}。推测位置为:{}", Heros.getNameById(participant.getChampionId()), role.name());

        // 获取位置基准值
        double kdaBaseline = getPositionBaseline(role.name(), "kda");
        double csBaseline = getPositionBaseline(role.name(), "cs");
        double dmgPGBaseline = getPositionBaseline(role.name(), "damage_per_gold");

        // 基础指标计算
        double baseKillAssist = calculateBaseKillAssist(stats.getKills(), stats.getAssists(), role, kdaBaseline);
        double damageOutput = calculateDamageOutput(stats, role, dmgPGBaseline);
        double teamContribution = timeline != null ?calculateTeamContribution(stats, timeline, teamStats):0;
        double economyEfficiency = calculateEconomyEfficiency(stats, csBaseline);
        double survivalRate = calculateSurvivalRate(stats);
        double visionControl = calculateVisionControl(stats, role);

        // 权重归一化处理（Sigmoid函数）
        double normalizedBaseKillAssist = sigmoidNormalize(baseKillAssist / kdaBaseline);
        double normalizedDamageOutput = sigmoidNormalize(damageOutput / dmgPGBaseline);
        double normalizedTeamContribution = sigmoidNormalize(teamContribution / 100); // 示例基准
        double normalizedEconomyEfficiency = sigmoidNormalize(economyEfficiency / csBaseline);
        double normalizedSurvivalRate = Math.min(survivalRate / SURVIVAL_RATE_MAX, 1.0);
        double normalizedVisionControl = Math.min(visionControl / VISION_CONTROL_MAX, 1.0);

        // 应用角色权重
        double total =
                normalizedBaseKillAssist * role.getWeight(0) +
                        normalizedDamageOutput * role.getWeight(0) +
                        normalizedTeamContribution * role.getWeight(1) +
                        normalizedEconomyEfficiency * role.getWeight(2) +
                        normalizedSurvivalRate * role.getWeight(3) +
                        normalizedVisionControl * role.getWeight(1); // 辅助位视野权重高

        // 胜负修正
        if (Boolean.FALSE.equals(stats.win)) {
            total *= 0.7; // 失败惩罚
        } else if (isCarryGame(stats, teamStats)) {
            total *= 1.3; // carry加成
        }

        return new GameScore(
                normalizedBaseKillAssist,
                normalizedDamageOutput,
                normalizedTeamContribution,
                normalizedEconomyEfficiency,
                normalizedSurvivalRate,
                normalizedVisionControl,
                total * 100, // 映射到0-100
                role
        );
    }


    /**
     * 计算基础击杀/助攻得分（含角色权重）
     */
    private static double calculateBaseKillAssist(int kills, int assists, RoleType role, double kdaBaseline) {
        // 角色差异化权重应用
        double killWeight = role.getWeight(0) * 1.5;  // 击杀权重强化
        double assistWeight = role.getWeight(1) * 1.2; // 助攻权重弱化

        // 基础KDA计算（使用Sigmoid归一化）
        double rawKDA = (double) (kills + assists) / Math.max(1, kills);
        double normalizedKDA = sigmoidNormalize(rawKDA / kdaBaseline);

        // 加权合成
        return (kills * killWeight + assists * assistWeight) * normalizedKDA;
    }

    /**
     * 计算伤害输出效率（含经济效率修正）
     */
    private static double calculateDamageOutput(Participant.Stats stats, RoleType role, double dmgPGBaseline) {
        double physicalRatio = (double) stats.physicalDamageDealtToChampions /
                Math.max(1, stats.totalDamageDealtToChampions);
        double magicRatio = (double) stats.magicDamageDealtToChampions /
                Math.max(1, stats.totalDamageDealtToChampions);

        // 混合伤害系数（鼓励多伤害类型）
        double mixedDmgFactor = 1.0 + Math.sqrt(physicalRatio * magicRatio);

        // 经济效率修正因子
        double goldEfficiency = (double) stats.goldEarned / Math.max(1, stats.goldSpent);
        double efficiencyFactor = Math.min(goldEfficiency / 1.2, 1.2); // 上限保护

        // 基础伤害评分（动态基准适配）
        double baseDmgScore = (stats.totalDamageDealtToChampions * role.getWeight(0)) /
                Math.max(1, dmgPGBaseline * stats.goldEarned);

        return baseDmgScore * mixedDmgFactor * efficiencyFactor;
    }

    /**
     * 团队贡献评估（结合地图阶段特征）
     */
    private static double calculateTeamContribution(Participant.Stats stats, Participant.Timeline timeline, List<Participant.Stats> teamStats) {
        // 目标优先级系数（早期防御塔价值更高）
        double towerPriority = timeline.getCsDiffPerMinDeltas().getTen() > 0 ? 1.2 : 0.8;

        // 推塔贡献
        double towerScore = stats.turretKills * 15 * towerPriority;

        // 水晶贡献（后期价值提升）
        double inhibitorScore = stats.inhibitorKills * 25 *
                (timeline.getXpPerMinDeltas().getThirty() > 500 ? 1.3 : 1.0);

        // 目标参与度（与队友比较）
        double teamDmg = teamStats.stream()
                .mapToDouble(s -> s.totalDamageDealtToChampions)
                .sum();
        double dmgContribution = teamDmg == 0 ? 0 :
                (double) stats.totalDamageDealtToChampions / teamDmg;

        return (towerScore + inhibitorScore) * (0.7 + dmgContribution * 0.3);
    }

    /**
     * 经济效率计算（带补刀基准适配）
     */
    private static double calculateEconomyEfficiency(Participant.Stats stats, double csBaseline) {
        // 补刀效率（考虑位置差异）
        double csEfficiency = (stats.totalMinionsKilled + stats.neutralMinionsKilled) /
                Math.max(1, csBaseline * 0.8); // 预留成长空间
        csEfficiency = Math.min(csEfficiency, 1.5); // 极端值限制

        // 金币转化率（装备利用率）
        double itemUtilization = stats.item6 == 0 ? 1.0 : 0.8; // 未完成装备惩罚

        // 经济稳定性（花销控制）
        double goldStability = stats.goldSpent == 0 ? 0 :
                (double) stats.goldEarned / stats.goldSpent;
        goldStability = Math.min(goldStability, 1.3); // 过度消费惩罚

        return csEfficiency * itemUtilization * goldStability;
    }

    /**
     * 生存能力评估（结合KDA动态波动）
     */
    private static double calculateSurvivalRate(Participant.Stats stats) {
        if (stats.deaths == 0) return SURVIVAL_RATE_MAX; // 全勤奖励

        double kdaRatio = (stats.kills + stats.assists) / (double) stats.deaths;
        double survivalPenalty = stats.deaths > 5 ? 1 - Math.log(stats.deaths) * 0.2 : 1.0;

        // KDA曲线拟合（非线性衰减）
        double kdaEffect = 1 - Math.exp(-kdaRatio / 3);

        // 存活时长修正（来自Timeline数据）
        double avgLifespan = 600; // 示例基准值（单位：秒）
        double timeAlive = Optional.of(stats.longestTimeSpentLiving).orElse(0);
        double lifespanBonus = timeAlive > avgLifespan ? 1.1 : 1.0;

        return (kdaEffect * survivalPenalty * lifespanBonus) * SURVIVAL_RATE_MAX;
    }

    /**
     * 视野控制评分（角色差异化处理）
     */
    private static double calculateVisionControl(Participant.Stats stats, RoleType role) {
        // 基础视野得分
        double visionScore = stats.getVisionScore() * 0.05;

        // 眼位质量评估（探测眼权重）
        int controlWards = 0;
        for (int i = 0; i <= 5; i++) {
            if (Arrays.asList(3363, 3340, 2049).contains(getItem(stats, i))) { // 控制守卫系列
                controlWards++;
            }
        }

        double wardQuality = controlWards * 0.3;
        double wardQuantity = (stats.wardsPlaced + stats.sightWardsBoughtInGame) * 0.1;

        // 排眼效率（辅助位特别重要）
        double wardKiller = role == RoleType.SUPPORT ?
                stats.wardsKilled * 0.2 :
                stats.wardsKilled * 0.1;

        return visionScore + wardQuality + wardQuantity + wardKiller;
    }

    // 辅助方法
    private static int getItem(Participant.Stats stats, int slot) {
        return switch (slot) {
            case 0 -> stats.item0;
            case 1 -> stats.item1;
            case 2 -> stats.item2;
            case 3 -> stats.item3;
            case 4 -> stats.item4;
            case 5 -> stats.item5;
            default -> 0;
        };
    }

    /**
     * 聚合多场游戏评分（含稳定性分析）
     */
    public static int aggregateScores(List<GameScore> gameScores) {
        if (gameScores.isEmpty()) {
            throw new IllegalArgumentException("评分记录不能为空");
        }

        // 按维度计算平均分
        Map<String, Double> avgScores = new HashMap<>();
        Map<String, Double> variances = new HashMap<>();
        int totalGames = gameScores.size();

        for (String dim : Arrays.asList("baseKillAssist", "damageOutput", "teamContribution",
                "economyEfficiency", "survivalRate", "visionControl")) {
            double sum = 0;
            List<Double> values = new ArrayList<>();

            for (GameScore score : gameScores) {
                double val = switch (dim) {
                    case "baseKillAssist" -> score.baseKillAssist();
                    case "damageOutput" -> score.damageOutput();
                    case "teamContribution" -> score.teamContribution();
                    case "economyEfficiency" -> score.economyEfficiency();
                    case "survivalRate" -> score.survivalRate();
                    case "visionControl" -> score.visionControl();
                    default -> 0;
                };
                sum += val;
                values.add(val);
            }

            double avg = sum / totalGames;
            double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - avg, 2))
                    .average().orElse(0);

            avgScores.put(dim, avg);
            variances.put(dim, variance);
        }

        // 稳定性系数计算
        double stabilityFactor = 1.0 - variances.values().stream()
                .mapToDouble(Math::sqrt)
                .average().orElse(0) / 100.0; // 假设最大波动为100%

        // 总分计算（含稳定性修正）
        double totalRawScore = avgScores.values().stream().mapToDouble(Double::doubleValue).sum()
                * 100 / 6; // 归一化到100

        double totalScore = (double) Math.round(totalRawScore * stabilityFactor * 2) / 2; // 0.5分精度
        return (int) totalScore;

    }

    // ------------------- 核心算法实现 -------------------

    /**
     * 动态基准值获取（示例实现）
     */
    private static double getPositionBaseline(String position, String metric) {
        return POSITION_BASELINES.getOrDefault(position + "_" + metric, 1.0);
    }

    /**
     * Sigmoid归一化函数（缓解长尾效应）
     */
    private static double sigmoidNormalize(double value) {
        if (value <= 1.0) {
            // 基准值以下：线性增长（0~0.5）
            return value * 0.5;
        } else if (value <= 2.0) {
            // 基准值~2倍：中速增长（0.5~0.8）
            return 0.5 + (value - 1.0) * 0.3;
        } else if (value <= 3.0) {
            // 2倍~3倍：较慢增长（0.8~0.9）
            return 0.8 + (value - 2.0) * 0.1;
        } else if (value <= 5.0) {
            // 3倍~5倍：微弱增长（0.9~1.0）
            return 0.9 + (value - 3.0) / 2.0 * 0.1;
        } else {
            // 超过5倍：封顶
            return 1.0;
        }
    }

    /**
     * 增强型角色识别（结合物品/视野/操作行为）
     */
    private static RoleType identifyRole(Participant participant) {
        Participant.Stats stats = participant.getStats();
        Participant.Timeline timeline = participant.getTimeline();

        // 高优先级判定：特殊物品组合
        if ((stats.getItem0() == 3340 || stats.getItem5() == 3340) &&
                stats.getVisionWardsBoughtInGame() > 8) {
            return RoleType.SUPPORT;
        }

        // 中期路径分析
        if ("JUNGLE".equals(timeline.getLane())) {
            return RoleType.JUNGLE;
        }

        // 下路双人组细化判定
        if ("BOTTOM".equals(timeline.getLane())) {
            if (stats.getPhysicalDamageDealtToChampions() >
                    stats.getMagicDamageDealtToChampions() * 1.5) {
                return RoleType.BOTTOM; // ADC特征
            }
            return RoleType.SUPPORT; // 默认视为辅助
        }

        // 其他位置基础判定
        return switch (timeline.getLane()) {
            case "TOP" -> RoleType.TOP;
            case "MIDDLE" -> RoleType.MIDDLE;
            default -> RoleType.BOTTOM;
        };
    }

    /**
     * 判断是否为carry对局
     */
    private static boolean isCarryGame(Participant.Stats stats, List<Participant.Stats> teamStats) {
        double personalDmgRatio = stats.getTotalDamageDealtToChampions() /
                teamStats.stream()
                        .mapToDouble(Participant.Stats::getTotalDamageDealtToChampions)
                        .sum();
        return personalDmgRatio > 0.3 || stats.getKills() >= 10 && stats.getDeaths() <= 2;
    }


}