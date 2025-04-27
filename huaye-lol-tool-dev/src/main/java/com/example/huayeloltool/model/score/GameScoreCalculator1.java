package com.example.huayeloltool.model.score;


import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.game.Participant;
import lombok.Data; // 假设使用 Lombok 减少样板代码
import lombok.Getter; // Lombok Getter
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * 游戏得分计算器
 * 优化了性能、角色适应性和评分逻辑的客观性。
 */
@Slf4j
@Service
public class GameScoreCalculator1 {

    // --- 枚举和配置 ---

    /**
     * 表示玩家在游戏中的确定角色。
     */
    @Getter
    public enum Role {
        TOP("上单"),
        JUNGLE("打野"),
        MIDDLE("中单"),
        ADC("射手"),
        SUPPORT("辅助"),
        UNKNOWN("未知"); // 用于无法明确判断或特殊模式的情况

        private final String chineseName;

        Role(String chineseName) {
            this.chineseName = chineseName;
        }

        /**
         * 根据 Riot API 提供的 lane 和 role 字段确定角色。
         * 注意：此逻辑可能需要根据实际 API 返回值和边界情况进行微调。
         * 示例逻辑：
         * - JUNGLE lane -> 打野
         * - BOTTOM lane + DUO_SUPPORT role -> 辅助
         * - BOTTOM lane + DUO_CARRY role -> 射手
         * - MIDDLE lane -> 中单
         * - TOP lane -> 上单
         * - 处理 NONE/其他情况 -> 未知 (或进行最佳猜测)
         *
         * @param participant 参与者数据
         * @return 判定的角色枚举
         */
        public static Role determineRole(Participant participant) {
            if (participant == null || participant.getTimeline() == null ||
                    participant.getTimeline().getLane() == null || participant.getTimeline().getRole() == null) {
                return UNKNOWN; // 数据不完整则无法判断
            }

            // 统一转为大写以便比较
            String lane = participant.getTimeline().getLane().toUpperCase();
            String role = participant.getTimeline().getRole().toUpperCase();

            switch (lane) {
                case "JUNGLE":
                    return JUNGLE;
                case "MIDDLE":
                case "MID": // 兼容 "MID" 写法
                    return MIDDLE;
                case "TOP":
                    return TOP;
                case "BOTTOM":
                case "BOT": // 兼容 "BOT" 写法
                    // 在下路时，需要根据 role 细分
                    if ("DUO_SUPPORT".equals(role)) return SUPPORT;
                    if ("DUO_CARRY".equals(role)) return ADC;
                    // 如果下路 role 不明确，暂时标记为未知，后续可考虑基于英雄ID等信息猜测
                    return UNKNOWN;
                default:
                    // 如果分路信息不标准 (例如 ARAM 或特殊模式)，尝试根据 role 判断
                    if ("DUO_SUPPORT".equals(role)) return SUPPORT;
                    if ("DUO_CARRY".equals(role)) return ADC;
                    // 其他无法判断的情况
                    return UNKNOWN;
            }
        }
    }

    /**
     * 存储不同角色各项评分指标的权重。
     * 这些权重应该是可调整的，以适应游戏版本的平衡性改动。
     */
    @Getter
    private static class RoleWeights {
        private final double combatWeight;      // 战斗表现权重
        private final double economyWeight;     // 经济发育权重
        private final double visionWeight;      // 视野控制权重
        private final double objectiveWeight;   // 地图目标权重
        private final double kdaWeight;         // KDA单独权重 (可选，也可融入战斗表现)
        private final double damageWeight;      // 伤害输出权重 (可选，也可融入战斗表现)
        // ... 可以根据需要添加更多细分权重

        public RoleWeights(double combat, double economy, double vision, double objective, double kda, double damage) {
            // 可以加入校验，确保权重总和为1或在一个合理范围内
            this.combatWeight = combat;
            this.economyWeight = economy;
            this.visionWeight = vision;
            this.objectiveWeight = objective;
            this.kdaWeight = kda;
            this.damageWeight = damage;
        }

        // 预设的各角色权重 - !!关键：这些值需要根据大量数据分析和游戏理解进行精细调整!!
        private static final Map<Role, RoleWeights> WEIGHTS_MAP = createWeights();

        private static Map<Role, RoleWeights> createWeights() {
            Map<Role, RoleWeights> map = new EnumMap<>(Role.class);
            // 示例权重 (需要专业调整):
            //                   战斗, 经济, 视野, 目标, KDA, 伤害
            map.put(Role.TOP, new RoleWeights(0.30, 0.25, 0.05, 0.20, 0.10, 0.10));
            map.put(Role.JUNGLE, new RoleWeights(0.30, 0.15, 0.15, 0.25, 0.10, 0.05)); // 打野更看重控图和gank节奏
            map.put(Role.MIDDLE, new RoleWeights(0.25, 0.25, 0.05, 0.15, 0.10, 0.20)); // 中单伤害占比高
            map.put(Role.ADC, new RoleWeights(0.20, 0.30, 0.05, 0.15, 0.10, 0.20)); // ADC发育和伤害重要
            map.put(Role.SUPPORT, new RoleWeights(0.25, 0.05, 0.35, 0.15, 0.15, 0.05)); // 辅助视野和KDA(助攻)重要
            map.put(Role.UNKNOWN, new RoleWeights(0.25, 0.20, 0.15, 0.20, 0.10, 0.10)); // 未知角色给一个相对平均的权重

            // 确保所有角色都有定义
            for (Role role : Role.values()) {
                map.putIfAbsent(role, map.get(Role.UNKNOWN)); // 如果有新角色或未定义，使用默认值
            }
            return Collections.unmodifiableMap(map); // 返回不可变Map
        }

        public static RoleWeights getWeights(Role role) {
            return WEIGHTS_MAP.getOrDefault(role, WEIGHTS_MAP.get(Role.UNKNOWN));
        }
    }

    // --- 核心计算逻辑 ---

    /**
     * 计算指定召唤师在一局游戏中的得分。
     *
     * @param summonerID  目标召唤师的ID
     * @param gameSummary 游戏对局的详细摘要信息
     * @return ScoreWithReason 对象，包含最终得分和得分原因（可选）
     * @throws Exception 如果找不到玩家或必要数据缺失
     */
    public ScoreWithReason calcUserGameScore(long summonerID, GameSummary gameSummary) throws Exception {
        log.info("新模式正在分析");
        // 1. 数据校验与准备
        if (gameSummary == null || gameSummary.getParticipants() == null || gameSummary.getParticipantIdentities() == null) {
            throw new IllegalArgumentException("无效的游戏摘要数据");
        }
        if (gameSummary.getParticipants().isEmpty() || gameSummary.getParticipantIdentities().isEmpty()) {
            throw new Exception("游戏对局缺少参与者信息");
        }

        final double BASE_SCORE = 50.0; // 设定一个基础分，代表平均水平
        final double MAX_SCORE = 100.0; // 设定得分上限
        final double MIN_SCORE = 0.0;   // 设定得分下限
        ScoreWithReason gameScore = new ScoreWithReason(BASE_SCORE); // 初始化得分

        long gameDurationSeconds = gameSummary.getGameDuration();
        if (gameDurationSeconds <= 0) {
            throw new Exception("无效的游戏时长");
        }
        double gameDurationMinutes = gameDurationSeconds / 60.0; // 精确到分钟的小数

        // 2. 查找目标玩家及其队伍信息
        // 创建 ParticipantId -> PlayerName (或 SummonerId) 的映射
        Map<Integer, Long> participantIdToSummonerIdMap = gameSummary.getParticipantIdentities().stream()
                .filter(identity -> identity.getPlayer() != null)
                .collect(Collectors.toMap(
                        GameSummary.ParticipantIdentity::getParticipantId,
                        identity -> identity.getPlayer().getSummonerId(),
                        (s1, s2) -> s1 // 如果有重复ID（理论上不应发生），保留第一个
                ));

        // 查找目标玩家的 ParticipantId
        Optional<Integer> userParticipantIdOpt = participantIdToSummonerIdMap.entrySet().stream()
                .filter(entry -> entry.getValue() == summonerID)
                .map(Map.Entry::getKey)
                .findFirst();

        if (!userParticipantIdOpt.isPresent()) {
            throw new Exception("在对局中未找到ID为 " + summonerID + " 的召唤师");
        }
        int userParticipantId = userParticipantIdOpt.get();

        // 创建 ParticipantId -> Participant 的映射，方便后续查找
        Map<Integer, Participant> participantMap = gameSummary.getParticipants().stream()
                .collect(Collectors.toMap(Participant::getParticipantId, Function.identity()));

        Participant userParticipant = participantMap.get(userParticipantId);
        if (userParticipant == null || userParticipant.getStats() == null || userParticipant.getTimeline() == null) {
            throw new Exception("获取召唤师当局比赛表现详情失败 (数据不完整)");
        }
        Integer userTeamId = userParticipant.getTeamId();
        if (userTeamId == null) {
            throw new Exception("获取用户队伍ID失败");
        }

        // 3. 角色判定与数据预处理
        Role userRole = Role.determineRole(userParticipant);
        Participant.Stats userStats = userParticipant.getStats();

        // 存储所有玩家的角色信息
        Map<Integer, Role> participantRoles = new HashMap<>();
        // 存储所有玩家的基础数据，按队伍分组
        Map<Integer, List<Participant>> teamParticipants = new HashMap<>();
        // 计算双方队伍的总数据
        TeamStats team100Stats = new TeamStats();
        TeamStats team200Stats = new TeamStats();

        for (Participant p : gameSummary.getParticipants()) {
            if (p.getStats() == null || p.getTeamId() == null) continue; // 跳过无效数据

            Role role = Role.determineRole(p);
            participantRoles.put(p.getParticipantId(), role);

            teamParticipants.computeIfAbsent(p.getTeamId(), k -> new ArrayList<>()).add(p);

            // 累加队伍总数据
            TeamStats currentTeamStats = p.getTeamId() == 100 ? team100Stats : team200Stats;
            currentTeamStats.totalKills += p.getStats().getKills();
            currentTeamStats.totalDeaths += p.getStats().getDeaths();
            currentTeamStats.totalAssists += p.getStats().getAssists();
            currentTeamStats.totalGold += p.getStats().getGoldEarned();
            currentTeamStats.totalDamageToChampions += p.getStats().getTotalDamageDealtToChampions();
            currentTeamStats.totalVisionScore += p.getStats().getVisionScore();
            currentTeamStats.totalMinionsKilled += p.getStats().getTotalMinionsKilled();
            // 可以按需添加更多总计数据，如承伤等
        }

        TeamStats userTeamStats = userTeamId == 100 ? team100Stats : team200Stats;

        // 4. 计算各维度表现得分 (相对于基准或平均水平)

        // 4.1 战斗表现 (Combat Score) - KDA, 击杀参与率, 多杀, 一血等
        double combatScore = calculateCombatScore(userParticipant, userTeamStats, gameDurationMinutes);
        // 可以在 ScoreWithReason 中记录具体子项得分
//         gameScore.addReason("KDA得分", kdaSubScore, ScoreOption.KDA_SCORE);
//         gameScore.addReason("击杀参与率得分", kpSubScore, ScoreOption.KP_SCORE);

        // 4.2 经济发育 (Economy Score) - GPM, CSPM (与同角色对比更有意义)
        double economyScore = calculateEconomyScore(userParticipant, gameDurationMinutes, userRole, participantMap, participantRoles);
         gameScore.addReason("经济得分", economyScore - BASE_SCORE, ScoreOption.ECONOMY_SCORE); // 记录相对基础分的增减

        // 4.3 视野控制 (Vision Score) - VSPM (与同角色对比)
        double visionScore = calculateVisionScore(userParticipant, gameDurationMinutes, userRole, participantMap, participantRoles);
         gameScore.addReason("视野得分", visionScore - BASE_SCORE, ScoreOption.VISION_SCORE);

        // 4.4 地图目标 (Objective Score) - 推塔, 控龙/先锋/男爵, 胜负
        // 注意: API提供的直接目标伤害数据有限, 这里主要基于胜负和少量指标
        double objectiveScore = calculateObjectiveScore(userParticipant, gameSummary.getParticipants().get(0).getStats().getWin()); // 假设有isWin方法
         gameScore.addReason("目标得分", objectiveScore - BASE_SCORE, ScoreOption.OBJECTIVE_SCORE);

        // 5. 根据角色权重聚合各维度得分
        RoleWeights weights = RoleWeights.getWeights(userRole);

        double finalScore = BASE_SCORE // 从基础分开始
                + (combatScore - BASE_SCORE) * weights.getCombatWeight()
                + (economyScore - BASE_SCORE) * weights.getEconomyWeight()
                + (visionScore - BASE_SCORE) * weights.getVisionWeight()
                + (objectiveScore - BASE_SCORE) * weights.getObjectiveWeight();
        // 可以根据需要加入 KDA、伤害等独立加权项

        // 6. 分数调整与限制
        // 可以加入一些特殊事件的固定加分/减分 (如果权重模型未完全覆盖)
        // 例如：五杀额外加分
        if (userStats.getPentaKills() > 0) {
            finalScore += 5.0; // 示例固定加分
            gameScore.addReason("五杀成就", 5.0, ScoreOption.PENTA_KILLS); // 假设ScoreOption存在
        } else if (userStats.getQuadraKills() > 0) {
            finalScore += 3.0;
            gameScore.addReason("四杀成就", 3.0, ScoreOption.QUADRA_KILLS);
        }
        // ... 其他类似调整

        // 确保分数在预设范围内 [MIN_SCORE, MAX_SCORE]
        finalScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, finalScore));

        // 更新最终得分 (替换初始的基础分)
        gameScore.setScore(finalScore);
        gameScore.addReason("玩家角色", userRole.getChineseName(), ScoreOption.PLAYER_ROLE); // 记录玩家角色
        gameScore.addReason("基础得分", BASE_SCORE, ScoreOption.BASE_SCORE);
        gameScore.addReason("战斗贡献分", (combatScore - BASE_SCORE) * weights.getCombatWeight(), ScoreOption.COMBAT_CONTRIBUTION);
        gameScore.addReason("经济贡献分", (economyScore - BASE_SCORE) * weights.getEconomyWeight(), ScoreOption.ECONOMY_CONTRIBUTION);
        gameScore.addReason("视野贡献分", (visionScore - BASE_SCORE) * weights.getVisionWeight(), ScoreOption.VISION_CONTRIBUTION);
        gameScore.addReason("目标贡献分", (objectiveScore - BASE_SCORE) * weights.getObjectiveWeight(), ScoreOption.OBJECTIVE_CONTRIBUTION);

        // 7. 返回结果
        return gameScore;
    }

    // --- Helper 方法 ---

    /**
     * 计算战斗表现得分。
     *
     * @param player       目标玩家
     * @param teamStats    玩家所在队伍的总数据
     * @param durationMins 游戏时长（分钟）
     * @return 战斗表现得分 (0-100范围)
     */
    private double calculateCombatScore(Participant player, TeamStats teamStats, double durationMins) {
        Participant.Stats stats = player.getStats();
        int kills = stats.getKills();
        int deaths = stats.getDeaths();
        int assists = stats.getAssists();

        // KDA 计算 (处理死亡为0的情况)
        double kda = (deaths == 0) ? (kills + assists) * 1.2 : (double) (kills + assists) / deaths; // 死亡为0时给予奖励

        // 击杀参与率 (KP)
        double killParticipation = (teamStats.totalKills == 0) ? 0 : (double) (kills + assists) / teamStats.totalKills;

        // 基础分调整 KDA (示例：KDA 3 为平均水平)
        double kdaScore = 50.0 + (kda - 3.0) * 8.0; // KDA每高1分，得分增加8分 (可调)

        // 基础分调整 KP (示例：KP 50% 为平均水平)
        double kpScore = 50.0 + (killParticipation - 0.5) * 40.0; // KP每高10%，得分增加4分 (可调)

        // 一血贡献
        double firstBloodScore = 0;
        if (stats.isFirstBloodKill()) {
            firstBloodScore = 10.0; // 一血击杀加分
        } else if (stats.isFirstBloodAssist()) {
            firstBloodScore = 5.0;  // 一血助攻加分
        }

        // 多杀贡献 (这里简化处理，可细化权重)
        double multiKillScore = stats.getTripleKills() * 1.0 + stats.getQuadraKills() * 3.0 + stats.getPentaKills() * 6.0;

        // 综合 KDA, KP, 特殊事件得分 (可以加权)
        double combinedScore = (kdaScore * 0.4) + (kpScore * 0.4) + firstBloodScore + multiKillScore;

        // 根据游戏时长调整？ (例如前期KDA权重高，后期KP权重高？复杂，暂不加入)

        // 限制得分在 0-100
        return Math.max(0.0, Math.min(100.0, combinedScore));
    }


    /**
     * 计算经济发育得分。
     * 考虑 GPM 和 CSPM，并与同位置玩家对比（如果数据足够）。
     *
     * @param player           目标玩家
     * @param durationMins     游戏时长（分钟）
     * @param playerRole       玩家角色
     * @param participantMap   所有参与者信息
     * @param participantRoles 所有参与者角色
     * @return 经济发育得分 (0-100范围)
     */
    private double calculateEconomyScore(Participant player, double durationMins, Role playerRole,
                                         Map<Integer, Participant> participantMap, Map<Integer, Role> participantRoles) {
        // 辅助角色的经济得分权重非常低，可以直接给一个固定值或简单计算
        if (playerRole == Role.SUPPORT) {
            return 30.0; // 辅助经济要求低，给个偏低的基础分
        }
        if (durationMins <= 0.1) return 50.0; // 避免除零

        Participant.Stats stats = player.getStats();
        double gpm = stats.getGoldEarned() / durationMins;
        double cspm = (stats.getTotalMinionsKilled() /* + stats.getNeutralMinionsKilled() */) / durationMins; // 考虑是否加入野怪

        // --- 与同角色对比 (可选，但更科学) ---
        List<Participant> sameRoleOpponents = new ArrayList<>();
        List<Participant> sameRoleAllies = new ArrayList<>(); // 包括自己
        for (Map.Entry<Integer, Participant> entry : participantMap.entrySet()) {
            int pId = entry.getKey();
            Participant p = entry.getValue();
            Role role = participantRoles.get(pId);
            if (role == playerRole) {
                if (p.getTeamId().equals(player.getTeamId())) {
                    sameRoleAllies.add(p);
                } else {
                    sameRoleOpponents.add(p);
                }
            }
        }

        // 计算同角色平均 GPM/CSPM (简化：只考虑对手或全场同角色)
        double avgGpmSameRole = calculateAverageStat(sameRoleOpponents, p -> p.getStats().getGoldEarned() / durationMins);
        double avgCspmSameRole = calculateAverageStat(sameRoleOpponents, p -> (p.getStats().getTotalMinionsKilled()) / durationMins);

        // 基于与同角色平均值的比较来评分
        double gpmScore = 50.0;
        if (avgGpmSameRole > 0) {
            // GPM 高于平均值越多，得分越高，幅度可调
            gpmScore += (gpm / avgGpmSameRole - 1.0) * 50.0;
        }

        double cspmScore = 50.0;
        // CSPM 对非辅助角色很重要
        if (avgCspmSameRole > 0) {
            // CSPM 高于平均值越多，得分越高
            cspmScore += (cspm / avgCspmSameRole - 1.0) * 60.0; // CSPM权重可以略高于GPM
        }


        // --- 如果没有同角色对比，使用绝对值或全场平均值 ---
        // (这里省略了没有同角色对比的后备逻辑，实际应实现)

        // 结合GPM和CSPM得分 (示例：简单平均)
        double combinedScore = (gpmScore + cspmScore) / 2.0;

        // 限制得分在 0-100
        return Math.max(0.0, Math.min(100.0, combinedScore));
    }

    /**
     * 计算视野控制得分。
     * 考虑 VSPM，与同位置玩家对比。
     *
     * @param player           目标玩家
     * @param durationMins     游戏时长（分钟）
     * @param playerRole       玩家角色
     * @param participantMap   所有参与者信息
     * @param participantRoles 所有参与者角色
     * @return 视野控制得分 (0-100范围)
     */
    private double calculateVisionScore(Participant player, double durationMins, Role playerRole,
                                        Map<Integer, Participant> participantMap, Map<Integer, Role> participantRoles) {
        if (durationMins <= 0.1) return 50.0; // 避免除零

        Participant.Stats stats = player.getStats();
        double vspm = stats.getVisionScore() / durationMins; // Vision Score Per Minute

        // --- 与同角色对比 ---
        List<Participant> sameRoleParticipants = new ArrayList<>(); // 全场同角色
        for (Map.Entry<Integer, Participant> entry : participantMap.entrySet()) {
            if (participantRoles.get(entry.getKey()) == playerRole) {
                sameRoleParticipants.add(entry.getValue());
            }
        }

        // 计算同角色平均 VSPM
        double avgVspmSameRole = calculateAverageStat(sameRoleParticipants, p -> p.getStats().getVisionScore() / durationMins);

        // 基于与同角色平均值的比较来评分
        double visionScore = 50.0;
        if (avgVspmSameRole > 0) {
            // VSPM 高于平均值越多，得分越高，辅助角色的系数应该更高
            double multiplier = (playerRole == Role.SUPPORT) ? 70.0 : 40.0; // 辅助视野更重要
            visionScore += (vspm / avgVspmSameRole - 1.0) * multiplier;
        } else {
            // 如果没有同角色或平均值为0，可以根据绝对 VSPM 给分 (e.g., VSPM > 1.5 算优秀?)
            if (vspm > 1.5) visionScore += 20;
            if (vspm > 1.0) visionScore += 10;
        }

        // 限制得分在 0-100
        return Math.max(0.0, Math.min(100.0, visionScore));
    }

    /**
     * 计算地图目标得分。
     * 主要基于胜负，可结合少量客观指标。
     *
     * @param player 目标玩家
     * @param didWin 玩家是否胜利
     * @return 地图目标得分 (0-100范围)
     */
    private double calculateObjectiveScore(Participant player, boolean didWin) {
        Participant.Stats stats = player.getStats();
        double score = didWin ? 75.0 : 25.0; // 胜负是目标得分的核心基础

        // 对防御塔/水晶的贡献 (API中只有摧毁数，没有伤害数据)
        score += stats.getInhibitorKills() * 5.0; // 摧毁水晶加分
        // score += stats.getTurretKills() * 2.0; // 如果有防御塔击杀数

        // 如果有对塔/对建筑物伤害数据，可以加进来
        // double damageToObjectives = stats.getDamageDealtToObjectives() + stats.getDamageDealtToTurrets();
        // score += damageToObjectives / 1000.0; // 示例：每1000伤害加一点分

        // 限制得分在 0-100
        return Math.max(0.0, Math.min(100.0, score));
    }


    /**
     * 计算参与者列表中某个数值属性的平均值。
     *
     * @param participants 参与者列表
     * @param extractor    用于从 Participant 对象中提取数值的函数
     * @return 计算得到的平均值，如果列表为空或无法计算则返回 0.0
     */
    private double calculateAverageStat(List<Participant> participants, Function<Participant, Double> extractor) {
        if (participants == null || participants.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        int count = 0;
        for (Participant p : participants) {
            if (p != null && p.getStats() != null) {
                try {
                    sum += extractor.apply(p);
                    count++;
                } catch (Exception e) {
                    // 处理可能的异常，例如除零
                    System.err.println("计算平均值时出错: " + e.getMessage());
                }
            }
        }
        return (count > 0) ? sum / count : 0.0;
    }


    // --- 内部数据结构 ---

    /**
     * 用于存储队伍级别的总计数据。
     */
    @Data // Lombok注解，自动生成getter/setter/toString等
    private static class TeamStats {
        int totalKills = 0;
        int totalDeaths = 0;
        int totalAssists = 0;
        long totalGold = 0;
        long totalDamageToChampions = 0;
        long totalVisionScore = 0;
        long totalMinionsKilled = 0;
        // 可以根据需要添加更多统计项
    }


    // --- 依赖的外部类（需要自行实现或引入） ---

    // GameSummary, Participant, ScoreWithReason, ScoreOption 等类需要存在且结构匹配
    // 以下是模拟的结构，请替换为你的实际类定义


    /**
     * 假设的得分选项枚举
     */
    public enum ScoreOption {
        FIRST_BLOOD_KILL, FIRST_BLOOD_ASSIST,
        PENTA_KILLS, QUADRA_KILLS, TRIPLE_KILLS,
        JOIN_TEAM_RATE_RANK, GOLD_EARNED_RANK, HURT_RANK, MONEY_TO_HURT_RATE_RANK, VISION_SCORE_RANK,
        MINIONS_KILLED, KILL_RATE, HURT_RATE, ASSIST_RATE,
        KDA_ADJUST, // 原有 KDA 调整项
        // 新增项
        PLAYER_ROLE, BASE_SCORE,
        COMBAT_CONTRIBUTION, ECONOMY_CONTRIBUTION, VISION_CONTRIBUTION, OBJECTIVE_CONTRIBUTION,
        KDA_SCORE, KP_SCORE, ECONOMY_SCORE, VISION_SCORE, OBJECTIVE_SCORE, // 各维度总分
        PENTA_KILLS_BONUS, QUADRA_KILLS_BONUS // 特殊事件加分
        // ... 可根据需要添加更多项
    }

    // 注意: Participant 类及其内部的 Stats 和 Timeline 类使用问题中提供的定义
}
