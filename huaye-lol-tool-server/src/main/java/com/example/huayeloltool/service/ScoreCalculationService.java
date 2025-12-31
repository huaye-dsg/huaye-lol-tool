package com.example.huayeloltool.service;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.enums.ScoreOption;
import com.example.huayeloltool.model.base.CalcScoreConf;
import com.example.huayeloltool.model.game.GameHistory;
import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.game.Participant;
import com.example.huayeloltool.model.score.ScoreWithReason;
import com.example.huayeloltool.model.score.UserScore;
import com.example.huayeloltool.model.summoner.Summoner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 分数计算服务
 * 
 * <p>负责召唤师得分的完整计算流程，包括：
 * - 获取游戏历史记录
 * - 计算单局游戏详细得分
 * - 计算加权综合得分
 * - 时间权重处理
 * 
 * <p>重构说明：
 * 合并了原来的 ScoreService 和 ScoreCalculator，
 * 统一管理所有得分计算相关的逻辑，提供更清晰的职责划分。
 */
@Slf4j
@Service
public class ScoreCalculationService {

    private static final CalcScoreConf CALC_SCORE_CONF = CalcScoreConf.getInstance();

    @Autowired
    private LcuApiService lcuApiService;

    /**
     * 计算召唤师的综合得分
     * 
     * @param summoner 召唤师信息
     * @param isSelf 是否为队友（用于日志区分）
     * @return 用户得分信息，计算失败返回null
     */
    public UserScore calculateUserScore(Summoner summoner, boolean isSelf) {
        if (summoner == null) {
            log.warn("召唤师信息为空，无法计算得分");
            return null;
        }

        try {
            long summonerID = summoner.getSummonerId();
            UserScore userScoreInfo = new UserScore(summonerID, Constant.DEFAULT_SCORE);
            userScoreInfo.setSummonerName(String.format("%s#%s", summoner.getGameName(), summoner.getTagLine()));
            userScoreInfo.setPuuid(summoner.getPuuid());

            // 获取游戏历史记录
            List<GameHistory.GameInfo> gameList = getGameHistory(summoner);
            if (CollectionUtils.isEmpty(gameList)) {
                log.warn("【{}】战绩查询为空，可能是新账号或隐私设置", summoner.getGameName());
                return null;
            }

            // 计算最终得分
            double finalScore = calculateFinalScore(gameList, summonerID);
            
            // 设置结果
            userScoreInfo.setScore(finalScore);
            userScoreInfo.setCurrKDA(GameHistoryAnalyzer.extractKdaList(gameList));
            userScoreInfo.setExtMsg(GameHistoryAnalyzer.analyzeGameHistory(gameList, summoner.getGameName(), isSelf));

            return userScoreInfo;
        } catch (Exception e) {
            log.error("【{}】计算用户得分失败，原因: {}", summoner.getGameName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取召唤师的游戏历史记录
     * 
     * @param summoner 召唤师信息
     * @return 游戏历史记录列表
     */
    private List<GameHistory.GameInfo> getGameHistory(Summoner summoner) {
        try {
            List<GameHistory.GameInfo> gameList = lcuApiService.listGameHistory(
                summoner, 0, Constant.DEFAULT_GAME_HISTORY_LIMIT - 1
            );
            
            if (CollectionUtils.isEmpty(gameList)) {
                log.debug("【{}】游戏历史记录为空", summoner.getGameName());
                return null;
            }
            
            return gameList;
        } catch (Exception e) {
            log.error("【{}】获取战绩列表失败，原因: {}", summoner.getGameName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 计算最终得分
     * 
     * <p>计算流程：
     * 1. 提取游戏ID列表
     * 2. 逐个查询游戏详情并计算得分
     * 3. 按时间权重计算最终得分
     * 
     * @param gameList 游戏历史列表
     * @param summonerID 召唤师ID
     * @return 加权后的最终得分
     */
    private double calculateFinalScore(List<GameHistory.GameInfo> gameList, long summonerID) {
        if (CollectionUtils.isEmpty(gameList)) {
            return Constant.DEFAULT_SCORE;
        }

        // 提取游戏ID并计算得分
        List<Long> gameIdList = gameList.stream()
                .map(GameHistory.GameInfo::getGameId)
                .filter(Objects::nonNull)
                .toList();

        if (gameIdList.isEmpty()) {
            log.warn("有效游戏ID列表为空");
            return Constant.DEFAULT_SCORE;
        }

        // 计算每局游戏的得分和时间
        List<AbstractMap.SimpleEntry<Double, LocalDateTime>> validScores = gameIdList.stream()
                .map(gameId -> calculateSingleGameScore(gameId, summonerID))
                .filter(Objects::nonNull)
                .toList();

        if (validScores.isEmpty()) {
            log.debug("没有有效的游戏得分数据，使用默认分数");
            return Constant.DEFAULT_SCORE;
        }

        // 按时间权重计算最终得分
        return calculateWeightedScore(validScores);
    }

    /**
     * 计算单局游戏得分
     * 
     * @param gameId 游戏ID
     * @param summonerID 召唤师ID
     * @return 得分和游戏时间的键值对，失败返回null
     */
    private AbstractMap.SimpleEntry<Double, LocalDateTime> calculateSingleGameScore(Long gameId, long summonerID) {
        try {
            GameSummary gameSummary = lcuApiService.queryGameSummary(gameId);
            if (gameSummary == null) {
                log.debug("游戏摘要为空，跳过: gameId={}", gameId);
                return null;
            }
            
            ScoreWithReason score = calculateGameScore(summonerID, gameSummary);
            if (score == null) {
                log.debug("得分计算失败，跳过: gameId={}", gameId);
                return null;
            }
            
            return new AbstractMap.SimpleEntry<>(score.getScore(), gameSummary.getGameCreationDate());
        } catch (Exception e) {
            log.warn("计算游戏得分失败: gameId={}, 错误: {}", gameId, e.getMessage());
            return null;
        }
    }
    /**
     * 计算单局游戏的详细得分
     * 
     * <p>得分计算包括多个维度：
     * - 一血击杀/助攻
     * - 多杀（五杀、四杀、三杀）
     * - 参团率排名
     * - 金钱获取排名
     * - 伤害输出排名
     * - 金钱转换伤害效率
     * - 视野得分排名
     * - 补兵效率
     * - KDA调整值
     * 
     * @param summonerID 召唤师ID
     * @param gameSummary 游戏摘要数据
     * @return 详细得分信息，计算失败返回null
     */
    public ScoreWithReason calculateGameScore(long summonerID, GameSummary gameSummary) {
        try {
            ScoreWithReason gameScore = new ScoreWithReason(Constant.DEFAULT_SCORE);

            // 获取用户参与者信息
            int userParticipantId = getUserParticipantId(summonerID, gameSummary);
            List<Participant> participants = gameSummary.getParticipants();
            
            Participant userParticipant = participants.stream()
                    .filter(item -> item.getParticipantId() == userParticipantId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("获取用户队伍ID失败"));

            int userTeamID = userParticipant.getTeamId();
            Participant.Stats userStats = userParticipant.getStats();

            // 获取同队参与者ID列表
            List<Integer> memberParticipantIDList = participants.stream()
                    .filter(item -> item.getTeamId().equals(userTeamID))
                    .map(Participant::getParticipantId)
                    .toList();

            // 统计全队数据
            TeamStats teamStats = calculateTeamStats(participants, userTeamID);
            
            // 判断是否为辅助位置
            boolean isSupportRole = isSupportRole(userParticipant);

            // 计算各项得分
            calculateFirstBloodScore(gameScore, userStats);
            calculateMultiKillScore(gameScore, userStats);
            calculateTeamParticipationScore(gameScore, userStats, teamStats, participants, memberParticipantIDList);
            calculateGoldRankScore(gameScore, userStats, teamStats, participants, memberParticipantIDList, isSupportRole);
            calculateDamageRankScore(gameScore, userStats, teamStats, participants, memberParticipantIDList);
            calculateGoldToDamageEfficiencyScore(gameScore, userStats, teamStats, participants, memberParticipantIDList);
            calculateVisionScore(gameScore, userStats, participants, memberParticipantIDList);
            calculateMinionsKilledScore(gameScore, userStats, gameSummary.getGameDuration());
            calculateRateBasedScores(gameScore, userStats, teamStats);
            calculateKdaAdjustment(gameScore, userStats, teamStats.totalKill);

            return gameScore;
        } catch (Exception e) {
            log.error("计算游戏得分失败: summonerID={}, gameId={}", summonerID, gameSummary.getGameId(), e);
            return null;
        }
    }

    /**
     * 队伍统计数据
     */
    private static class TeamStats {
        final int totalKill;
        final int totalDeath;
        final int totalAssist;
        final int totalDamage;
        final int totalGold;

        TeamStats(int totalKill, int totalDeath, int totalAssist, int totalDamage, int totalGold) {
            this.totalKill = totalKill;
            this.totalDeath = totalDeath;
            this.totalAssist = totalAssist;
            this.totalDamage = totalDamage;
            this.totalGold = totalGold;
        }
    }

    /**
     * 计算队伍统计数据
     */
    private TeamStats calculateTeamStats(List<Participant> participants, int userTeamID) {
        int totalKill = 0, totalDeath = 0, totalAssist = 0, totalDamage = 0, totalGold = 0;
        
        for (Participant participant : participants) {
            if (participant.getTeamId().equals(userTeamID)) {
                Participant.Stats stats = participant.getStats();
                totalKill += stats.getKills();
                totalDeath += stats.getDeaths();
                totalAssist += stats.getAssists();
                totalDamage += stats.getTotalDamageDealtToChampions();
                totalGold += stats.getGoldEarned();
            }
        }
        
        return new TeamStats(totalKill, totalDeath, totalAssist, totalDamage, totalGold);
    }

    /**
     * 判断是否为辅助位置
     */
    private boolean isSupportRole(Participant participant) {
        return "BOTTOM".equals(participant.getTimeline().getLane()) &&
               "SUPPORT".equals(participant.getTimeline().getRole());
    }

    /**
     * 计算一血得分
     */
    private void calculateFirstBloodScore(ScoreWithReason gameScore, Participant.Stats userStats) {
        if (userStats.isFirstBloodKill()) {
            gameScore.add(CALC_SCORE_CONF.getFirstBlood()[0], ScoreOption.FIRST_BLOOD_KILL);
        } else if (userStats.isFirstBloodAssist()) {
            gameScore.add(CALC_SCORE_CONF.getFirstBlood()[1], ScoreOption.FIRST_BLOOD_ASSIST);
        }
    }

    /**
     * 计算多杀得分
     */
    private void calculateMultiKillScore(ScoreWithReason gameScore, Participant.Stats userStats) {
        if (userStats.getPentaKills() > 0) {
            gameScore.add(CALC_SCORE_CONF.getPentaKills()[0], ScoreOption.PENTA_KILLS);
        } else if (userStats.getQuadraKills() > 0) {
            gameScore.add(CALC_SCORE_CONF.getQuadraKills()[0], ScoreOption.QUADRA_KILLS);
        } else if (userStats.getTripleKills() > 0) {
            gameScore.add(CALC_SCORE_CONF.getTripleKills()[0], ScoreOption.TRIPLE_KILLS);
        }
    }

    /**
     * 计算参团率得分
     */
    private void calculateTeamParticipationScore(ScoreWithReason gameScore, Participant.Stats userStats, 
                                               TeamStats teamStats, List<Participant> participants, 
                                               List<Integer> memberParticipantIDList) {
        if (teamStats.totalKill > 0) {
            int joinTeamRateRank = 1;
            double userJoinTeamKillRate = (double) (userStats.getAssists() + userStats.getKills()) / teamStats.totalKill;
            List<Double> memberJoinTeamKillRates = calculateMemberJoinTeamKillRates(participants, teamStats.totalKill, memberParticipantIDList);
            
            for (double rate : memberJoinTeamKillRates) {
                if (rate > userJoinTeamKillRate) {
                    joinTeamRateRank++;
                }
            }
            
            adjustGameScoreForRank(joinTeamRateRank, gameScore, CALC_SCORE_CONF.getJoinTeamRateRank(), ScoreOption.JOIN_TEAM_RATE_RANK);
        }
    }

    /**
     * 计算金钱排名得分
     */
    private void calculateGoldRankScore(ScoreWithReason gameScore, Participant.Stats userStats, 
                                      TeamStats teamStats, List<Participant> participants, 
                                      List<Integer> memberParticipantIDList, boolean isSupportRole) {
        if (teamStats.totalGold > 0) {
            int moneyRank = 1;
            int userMoney = userStats.getGoldEarned();
            List<Integer> memberMoneyList = calculateMemberMoney(participants, memberParticipantIDList);
            
            for (int money : memberMoneyList) {
                if (money > userMoney) {
                    moneyRank++;
                }
            }
            
            adjustGameScoreForRank(moneyRank, gameScore, CALC_SCORE_CONF.getGoldEarnedRank(), 
                                 ScoreOption.GOLD_EARNED_RANK, !isSupportRole);
        }
    }

    /**
     * 计算伤害排名得分
     */
    private void calculateDamageRankScore(ScoreWithReason gameScore, Participant.Stats userStats, 
                                        TeamStats teamStats, List<Participant> participants, 
                                        List<Integer> memberParticipantIDList) {
        if (teamStats.totalDamage > 0) {
            int damageRank = 1;
            int userDamage = userStats.getTotalDamageDealtToChampions();
            List<Integer> memberDamageList = calculateMemberDamage(participants, memberParticipantIDList);
            
            for (int damage : memberDamageList) {
                if (damage > userDamage) {
                    damageRank++;
                }
            }
            
            adjustGameScoreForRank(damageRank, gameScore, CALC_SCORE_CONF.getHurtRank(), ScoreOption.HURT_RANK);
        }
    }

    /**
     * 计算金钱转换伤害效率得分
     */
    private void calculateGoldToDamageEfficiencyScore(ScoreWithReason gameScore, Participant.Stats userStats, 
                                                    TeamStats teamStats, List<Participant> participants, 
                                                    List<Integer> memberParticipantIDList) {
        if (teamStats.totalGold > 0 && teamStats.totalDamage > 0) {
            int efficiencyRank = 1;
            double userEfficiency = (double) userStats.getTotalDamageDealtToChampions() / userStats.getGoldEarned();
            List<Double> memberEfficiencyList = calculateMemberGoldToDamageEfficiency(participants, memberParticipantIDList);
            
            for (double efficiency : memberEfficiencyList) {
                if (efficiency > userEfficiency) {
                    efficiencyRank++;
                }
            }
            
            adjustGameScoreForRank(efficiencyRank, gameScore, CALC_SCORE_CONF.getMoney2hurtRateRank(), 
                                 ScoreOption.MONEY_TO_HURT_RATE_RANK);
        }
    }

    /**
     * 计算视野得分
     */
    private void calculateVisionScore(ScoreWithReason gameScore, Participant.Stats userStats, 
                                    List<Participant> participants, List<Integer> memberParticipantIDList) {
        int visionScoreRank = 1;
        int userVisionScore = userStats.getVisionScore();
        List<Integer> memberVisionScoreList = calculateMemberVisionScore(participants, memberParticipantIDList);
        
        for (int visionScore : memberVisionScoreList) {
            if (visionScore > userVisionScore) {
                visionScoreRank++;
            }
        }
        
        adjustGameScoreForRank(visionScoreRank, gameScore, CALC_SCORE_CONF.getVisionScoreRank(), 
                             ScoreOption.VISION_SCORE_RANK);
    }

    /**
     * 计算补兵得分
     */
    private void calculateMinionsKilledScore(ScoreWithReason gameScore, Participant.Stats userStats, int gameDuration) {
        int totalMinionsKilled = userStats.getTotalMinionsKilled();
        int gameDurationMinute = gameDuration / 60;
        
        if (gameDurationMinute > 0) {
            int minuteMinionsKilled = totalMinionsKilled / gameDurationMinute;
            for (double[] minionsKilledLimit : CALC_SCORE_CONF.getMinionsKilled()) {
                if (minuteMinionsKilled >= minionsKilledLimit[0]) {
                    gameScore.add(minionsKilledLimit[1], ScoreOption.MINIONS_KILLED);
                    break;
                }
            }
        }
    }

    /**
     * 计算基于比率的得分（人头占比、伤害占比、助攻占比）
     */
    private void calculateRateBasedScores(ScoreWithReason gameScore, Participant.Stats userStats, TeamStats teamStats) {
        // 人头占比
        if (teamStats.totalKill > 0) {
            double userKillRate = (double) userStats.getKills() / teamStats.totalKill;
            adjustGameScoreForRate(userKillRate, userStats.getKills(), CALC_SCORE_CONF.getKillRate(), 
                                 gameScore, ScoreOption.KILL_RATE);
        }

        // 伤害占比
        if (teamStats.totalDamage > 0) {
            double userDamageRate = (double) userStats.getTotalDamageDealtToChampions() / teamStats.totalDamage;
            adjustGameScoreForRate(userDamageRate, userStats.getKills(), CALC_SCORE_CONF.getHurtRate(), 
                                 gameScore, ScoreOption.HURT_RATE);
        }

        // 助攻占比
        if (teamStats.totalAssist > 0) {
            double userAssistRate = (double) userStats.getAssists() / teamStats.totalAssist;
            adjustGameScoreForRate(userAssistRate, userStats.getKills(), CALC_SCORE_CONF.getAssistRate(), 
                                 gameScore, ScoreOption.ASSIST_RATE);
        }
    }

    /**
     * 计算KDA调整值
     */
    private void calculateKdaAdjustment(ScoreWithReason gameScore, Participant.Stats userStats, int totalKill) {
        double adjustVal = calculateKdaAdjustmentValue(userStats, totalKill);
        gameScore.add(adjustVal, ScoreOption.KDA_ADJUST);
    }

    /**
     * 计算KDA调整值的具体逻辑
     */
    private double calculateKdaAdjustmentValue(Participant.Stats userStats, int totalKill) {
        if (totalKill <= 0) {
            return 0;
        }
        
        double userJoinTeamKillRate = (double) (userStats.getAssists() + userStats.getKills()) / totalKill;
        int userDeathTimes = userStats.getDeaths() == 0 ? 1 : userStats.getDeaths();
        
        return ((double) (userStats.getKills() + userStats.getAssists()) / userDeathTimes - CALC_SCORE_CONF.getAdjustKDA()[0] +
                (userStats.getKills() - userStats.getDeaths()) / CALC_SCORE_CONF.getAdjustKDA()[1]) * userJoinTeamKillRate;
    }
    // ==================== 辅助计算方法 ====================

    /**
     * 获取用户参与者ID
     */
    private int getUserParticipantId(long summonerID, GameSummary gameSummary) {
        return gameSummary.getParticipantIdentities().stream()
                .filter(identity -> identity.getPlayer().getSummonerId() == summonerID)
                .findFirst()
                .map(GameSummary.ParticipantIdentity::getParticipantId)
                .orElseThrow(() -> new RuntimeException("获取用户参与者ID失败"));
    }

    /**
     * 计算队员参团率
     */
    private List<Double> calculateMemberJoinTeamKillRates(List<Participant> participants, int totalKill, 
                                                        List<Integer> memberParticipantIDList) {
        List<Double> rates = new ArrayList<>(Constant.STANDARD_TEAM_SIZE - 1);
        for (Participant participant : participants) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            rates.add((double) (participant.getStats().getAssists() + participant.getStats().getKills()) / totalKill);
        }
        return rates;
    }

    /**
     * 计算队员金钱
     */
    private List<Integer> calculateMemberMoney(List<Participant> participants, List<Integer> memberParticipantIDList) {
        List<Integer> moneyList = new ArrayList<>(Constant.STANDARD_TEAM_SIZE - 1);
        for (Participant participant : participants) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            moneyList.add(participant.getStats().getGoldEarned());
        }
        return moneyList;
    }

    /**
     * 计算队员伤害
     */
    private List<Integer> calculateMemberDamage(List<Participant> participants, List<Integer> memberParticipantIDList) {
        List<Integer> damageList = new ArrayList<>(Constant.STANDARD_TEAM_SIZE - 1);
        for (Participant participant : participants) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            damageList.add(participant.getStats().getTotalDamageDealtToChampions());
        }
        return damageList;
    }

    /**
     * 计算队员金钱转换伤害效率
     */
    private List<Double> calculateMemberGoldToDamageEfficiency(List<Participant> participants, 
                                                             List<Integer> memberParticipantIDList) {
        List<Double> efficiencyList = new ArrayList<>(Constant.STANDARD_TEAM_SIZE - 1);
        for (Participant participant : participants) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            Participant.Stats stats = participant.getStats();
            efficiencyList.add((double) stats.getTotalDamageDealtToChampions() / stats.getGoldEarned());
        }
        return efficiencyList;
    }

    /**
     * 计算队员视野得分
     */
    private List<Integer> calculateMemberVisionScore(List<Participant> participants, List<Integer> memberParticipantIDList) {
        List<Integer> visionScoreList = new ArrayList<>(Constant.STANDARD_TEAM_SIZE - 1);
        for (Participant participant : participants) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            visionScoreList.add(participant.getStats().getVisionScore());
        }
        return visionScoreList;
    }

    // ==================== 得分调整方法 ====================

    /**
     * 根据排名调整得分
     */
    private void adjustGameScoreForRank(int rank, ScoreWithReason gameScore, double[] scoreConf, ScoreOption option) {
        adjustGameScoreForRank(rank, gameScore, scoreConf, option, true);
    }

    /**
     * 根据排名调整得分（可选择是否应用负分）
     */
    private void adjustGameScoreForRank(int rank, ScoreWithReason gameScore, double[] scoreConf, 
                                      ScoreOption option, boolean applyNegative) {
        if (rank == 1) {
            gameScore.add(scoreConf[0], option);
        } else if (rank == 2) {
            gameScore.add(scoreConf[1], option);
        } else if (rank == 4 && applyNegative && scoreConf.length >= 3) {
            gameScore.add(-scoreConf[2], option);
        } else if (rank == 5 && applyNegative && scoreConf.length >= 4) {
            gameScore.add(-scoreConf[3], option);
        }
    }

    /**
     * 根据比率调整得分
     */
    private void adjustGameScoreForRate(double rate, int kills, List<CalcScoreConf.RateItemConf> rateConf, 
                                      ScoreWithReason gameScore, ScoreOption option) {
        for (CalcScoreConf.RateItemConf confItem : rateConf) {
            if (rate > confItem.getLimit()) {
                for (double[] limitConf : confItem.getScoreConf()) {
                    if (kills > (int) limitConf[0]) {
                        gameScore.add(limitConf[1], option);
                        break;
                    }
                }
            }
        }
    }

    // ==================== 权重计算方法 ====================

    /**
     * 计算加权得分
     * 
     * <p>权重策略：
     * - 近期对局（24小时内）权重更高，反映当前状态
     * - 历史对局权重较低，提供基础参考
     * 
     * @param validScores 有效得分列表（得分和时间的键值对）
     * @return 加权后的最终得分
     */
    private double calculateWeightedScore(List<AbstractMap.SimpleEntry<Double, LocalDateTime>> validScores) {
        LocalDateTime nowTime = LocalDateTime.now();
        List<Double> currTimeScores = new ArrayList<>(validScores.size());
        List<Double> otherTimeScores = new ArrayList<>(validScores.size());

        double totalScore = 0;
        int totalGameCount = validScores.size();
        
        // 按时间分类得分
        for (AbstractMap.SimpleEntry<Double, LocalDateTime> entry : validScores) {
            double score = entry.getKey();
            totalScore += score;
            
            if (nowTime.isBefore(entry.getValue().plusHours(Constant.RECENT_GAME_HOURS))) {
                currTimeScores.add(score);
            } else {
                otherTimeScores.add(score);
            }
        }

        return calculateWeightedAverage(currTimeScores, otherTimeScores, totalGameCount, totalScore);
    }

    /**
     * 计算加权平均分
     * 
     * @param currTimeScores 近期对局得分列表
     * @param otherTimeScores 其他对局得分列表
     * @param totalGameCount 总对局数
     * @param totalScore 总得分
     * @return 加权平均分
     */
    private double calculateWeightedAverage(List<Double> currTimeScores, List<Double> otherTimeScores, 
                                          int totalGameCount, double totalScore) {
        // 计算各时间段得分总和
        double totalTimeScore = currTimeScores.stream().mapToDouble(Double::doubleValue).sum();
        double totalOtherGameScore = otherTimeScores.stream().mapToDouble(Double::doubleValue).sum();

        // 计算总体平均得分（用于填充空时间段）
        double totalGameAvgScore = totalGameCount > 0 ? totalScore / totalGameCount : 0.0;

        // 计算各时间段平均得分
        double avgTimeScore = !currTimeScores.isEmpty() ? totalTimeScore / currTimeScores.size() : 0;
        double avgOtherGameScore = !otherTimeScores.isEmpty() ? totalOtherGameScore / otherTimeScores.size() : 0;

        // 计算加权总分
        double weightTotalScore = 0.0;
        weightTotalScore += !currTimeScores.isEmpty() ? 
                Constant.RECENT_GAME_WEIGHT * avgTimeScore : 
                Constant.RECENT_GAME_WEIGHT * totalGameAvgScore;
        weightTotalScore += !otherTimeScores.isEmpty() ? 
                Constant.OTHER_GAME_WEIGHT * avgOtherGameScore : 
                Constant.OTHER_GAME_WEIGHT * totalGameAvgScore;

        return weightTotalScore;
    }
}