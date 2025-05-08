package com.example.huayeloltool.model.score;

import com.example.huayeloltool.enums.ScoreOption;
import com.example.huayeloltool.model.base.CalcScoreConf;
import com.example.huayeloltool.model.game.GameSummary;
import com.example.huayeloltool.model.game.Participant;
import com.example.huayeloltool.model.score.calc.CommonScoreService;

import java.util.*;

public class ScoreService extends CommonScoreService {

    static final CalcScoreConf calcScoreConf = CalcScoreConf.getInstance();
    static final double defaultScore = 100.0;

    /**
     * 计算用户游戏得分
     */
    public ScoreWithReason calcUserGameScore(long summonerID, GameSummary gameSummary) throws Exception {
        ScoreWithReason gameScore = new ScoreWithReason(defaultScore);

        // 获取用户参与者ID
        int userParticipantId = getUserParticipantId(summonerID, gameSummary);

        List<Participant> participants = gameSummary.getParticipants();

        // 获取用户队伍ID
        Participant userParticipant = participants.stream()
                .filter(item -> item.getParticipantId() == userParticipantId)
                .findFirst()
                .orElseThrow(() -> new Exception("获取用户队伍ID失败"));

        int userTeamID = userParticipant.getTeamId();

        // 获取同队参与者ID列表
        List<Integer> memberParticipantIDList = participants.stream()
                .filter(item -> item.getTeamId().equals(userTeamID))
                .map(Participant::getParticipantId).toList();

        int totalKill = 0;   // 总人头
        int totalDeath = 0;  // 总死亡
        int totalAssist = 0; // 总助攻
        int totalHurt = 0;   // 总伤害
        int totalMoney = 0;  // 总金钱
        // 统计全队数据
        for (Participant participant : participants) {
            if (participant.getTeamId().equals(userTeamID)) {
                totalKill += participant.getStats().getKills();
                totalDeath += participant.getStats().getDeaths();
                totalAssist += participant.getStats().getAssists();
                totalHurt += participant.getStats().getTotalDamageDealtToChampions();
                totalMoney += participant.getStats().getGoldEarned();
            }
        }

        // 判断是否为辅助
        boolean isSupportRole = userParticipant.getTimeline().getLane().equals("BOTTOM") &&
                userParticipant.getTimeline().getRole().equals("SUPPORT");

        Participant.Stats userParticipantStats = userParticipant.getStats();

        // 一血击杀
        if (userParticipantStats.isFirstBloodKill()) {
            gameScore.add(calcScoreConf.getFirstBlood()[0], ScoreOption.FIRST_BLOOD_KILL);
        } else if (userParticipantStats.isFirstBloodAssist()) {
            gameScore.add(calcScoreConf.getFirstBlood()[1], ScoreOption.FIRST_BLOOD_ASSIST);
        }

        // 五杀、四杀、三杀
        if (userParticipantStats.getPentaKills() > 0) {
            gameScore.add(calcScoreConf.getPentaKills()[0], ScoreOption.PENTA_KILLS);
        } else if (userParticipantStats.getQuadraKills() > 0) {
            gameScore.add(calcScoreConf.getQuadraKills()[0], ScoreOption.QUADRA_KILLS);
        } else if (userParticipantStats.getTripleKills() > 0) {
            gameScore.add(calcScoreConf.getTripleKills()[0], ScoreOption.TRIPLE_KILLS);
        }

        // 参团率
        if (totalKill > 0) {
            int joinTeamRateRank = 1;
            double userJoinTeamKillRate = (double) (userParticipantStats.getAssists() + userParticipantStats.getKills()) / totalKill;
            List<Double> memberJoinTeamKillRates = listMemberJoinTeamKillRates(gameSummary, totalKill, memberParticipantIDList);
            for (double rate : memberJoinTeamKillRates) {
                if (rate > userJoinTeamKillRate) {
                    joinTeamRateRank++;
                }
            }
            adjustGameScoreForRank(joinTeamRateRank, gameScore, calcScoreConf.getJoinTeamRateRank(), ScoreOption.JOIN_TEAM_RATE_RANK);
        }

        // 获取金钱
        if (totalMoney > 0) {
            int moneyRank = 1;
            int userMoney = userParticipantStats.getGoldEarned();
            List<Integer> memberMoneyList = listMemberMoney(gameSummary, memberParticipantIDList);
            for (int v : memberMoneyList) {
                if (v > userMoney) {
                    moneyRank++;
                }
            }
            adjustGameScoreForRank(moneyRank, gameScore, calcScoreConf.getGoldEarnedRank(), ScoreOption.GOLD_EARNED_RANK, !isSupportRole);
        }

        // 伤害占比
        if (totalHurt > 0) {
            int hurtRank = 1;
            int userHurt = userParticipantStats.getTotalDamageDealtToChampions();
            List<Integer> memberHurtList = listMemberHurt(gameSummary, memberParticipantIDList);
            for (int v : memberHurtList) {
                if (v > userHurt) {
                    hurtRank++;
                }
            }
            adjustGameScoreForRank(hurtRank, gameScore, calcScoreConf.getHurtRank(), ScoreOption.HURT_RANK);
        }

        // 金钱转换伤害比
        if (totalMoney > 0 && totalHurt > 0) {
            int money2hurtRateRank = 1;
            double userMoney2hurtRate = (double) userParticipantStats.getTotalDamageDealtToChampions() / userParticipantStats.getGoldEarned();
            List<Double> memberMoney2hurtRateList = listMemberMoney2hurtRate(gameSummary, memberParticipantIDList);
            for (double v : memberMoney2hurtRateList) {
                if (v > userMoney2hurtRate) {
                    money2hurtRateRank++;
                }
            }
            adjustGameScoreForRank(money2hurtRateRank, gameScore, calcScoreConf.getMoney2hurtRateRank(), ScoreOption.MONEY_TO_HURT_RATE_RANK);
        }

        // 视野得分
        {
            int visionScoreRank = 1;
            int userVisionScore = userParticipantStats.getVisionScore();
            List<Integer> memberVisionScoreList = listMemberVisionScore(gameSummary, memberParticipantIDList);
            for (int v : memberVisionScoreList) {
                if (v > userVisionScore) {
                    visionScoreRank++;
                }
            }
            adjustGameScoreForRank(visionScoreRank, gameScore, calcScoreConf.getVisionScoreRank(), ScoreOption.VISION_SCORE_RANK);
        }

        // 补兵 每分钟8个刀以上加5分 ,9+10, 10+20
        {
            int totalMinionsKilled = userParticipantStats.getTotalMinionsKilled();
            int gameDurationMinute = gameSummary.getGameDuration() / 60;
            int minuteMinionsKilled = totalMinionsKilled / gameDurationMinute;
            for (double[] minionsKilledLimit : calcScoreConf.getMinionsKilled()) {
                if (minuteMinionsKilled >= minionsKilledLimit[0]) {
                    gameScore.add(minionsKilledLimit[1], ScoreOption.MINIONS_KILLED);
                    break;
                }
            }
        }

        // 人头占比
        if (totalKill > 0) {
            double userKillRate = (double) userParticipantStats.getKills() / totalKill;
            adjustGameScoreForRate(userKillRate, userParticipantStats.getKills(), calcScoreConf.getKillRate(), gameScore, ScoreOption.KILL_RATE);
        }

        // 伤害占比
        if (totalHurt > 0) {
            double userHurtRate = (double) userParticipantStats.getTotalDamageDealtToChampions() / totalHurt;
            adjustGameScoreForRate(userHurtRate, userParticipantStats.getKills(), calcScoreConf.getHurtRate(), gameScore, ScoreOption.HURT_RATE);
        }

        // 助攻占比
        if (totalAssist > 0) {
            double userAssistRate = (double) userParticipantStats.getAssists() / totalAssist;
            adjustGameScoreForRate(userAssistRate, userParticipantStats.getKills(), calcScoreConf.getAssistRate(), gameScore, ScoreOption.ASSIST_RATE);
        }

        // 参团率
        double adjustVal = getAdjustVal(userParticipantStats, totalKill);
        gameScore.add(adjustVal, ScoreOption.KDA_ADJUST);

        return gameScore;
    }

    private static double getAdjustVal(Participant.Stats userParticipantStats, int totalKill) {
        double userJoinTeamKillRate = (double) (userParticipantStats.getAssists() + userParticipantStats.getKills()) / totalKill;
        // 死亡次数
        int userDeathTimes = userParticipantStats.getDeaths() == 0 ? 1 : userParticipantStats.getDeaths();
        return ((double) (userParticipantStats.getKills() + userParticipantStats.getAssists()) / userDeathTimes - calcScoreConf.getAdjustKDA()[0] +
                (userParticipantStats.getKills() - userParticipantStats.getDeaths()) / calcScoreConf.getAdjustKDA()[1]) * userJoinTeamKillRate;
    }


    // 调整分数 根据排名
    private void adjustGameScoreForRank(int rank, ScoreWithReason gameScore, double[] scoreConf, ScoreOption option) {
        adjustGameScoreForRank(rank, gameScore, scoreConf, option, true);
    }

    // 调整分数 根据排名
    private void adjustGameScoreForRank(int rank, ScoreWithReason gameScore, double[] scoreConf, ScoreOption option, boolean applyNegative) {
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

    // 调整分数 根据rate
    private void adjustGameScoreForRate(double rate, int kills, List<CalcScoreConf.RateItemConf> rateConf, ScoreWithReason gameScore, ScoreOption option) {
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

    // 获取队员视野得分
    private List<Integer> listMemberVisionScore(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Integer> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add(participant.getStats().getVisionScore());
        }
        return res;
    }

    // 获取队员金钱转换伤害比
    private List<Double> listMemberMoney2hurtRate(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Double> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add((double) participant.getStats().getTotalDamageDealtToChampions() / participant.getStats().getGoldEarned());
        }
        return res;
    }

    // 获取队员金钱
    private List<Integer> listMemberMoney(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Integer> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add(participant.getStats().getGoldEarned());
        }
        return res;
    }

    // 获取队员参团率
    private List<Double> listMemberJoinTeamKillRates(GameSummary gameSummary, int totalKill, List<Integer> memberParticipantIDList) {
        List<Double> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add((double) (participant.getStats().getAssists() + participant.getStats().getKills()) / totalKill);
        }
        return res;
    }

    // 获取队员伤害
    private List<Integer> listMemberHurt(GameSummary gameSummary, List<Integer> memberParticipantIDList) {
        List<Integer> res = new ArrayList<>(4);
        for (Participant participant : gameSummary.getParticipants()) {
            if (!memberParticipantIDList.contains(participant.getParticipantId())) {
                continue;
            }
            res.add(participant.getStats().getTotalDamageDealtToChampions());
        }
        return res;
    }


}
