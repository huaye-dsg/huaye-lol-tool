package com.example.huayeloltool.service;

import com.example.huayeloltool.enums.ScoreOption;
import com.example.huayeloltool.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScoreService {

    private Double defaultScore = 100.0;

    /**
     * 计算用户游戏得分
     */
    public ScoreWithReason calcUserGameScore(long summonerID, GameSummary gameSummary) throws Exception {
        CalcScoreConf calcScoreConf = new CalcScoreConf();
        ScoreWithReason gameScore = new ScoreWithReason(defaultScore);
        int userParticipantId = 0;

        // 获取用户参与的ID
        for (GameSummary.ParticipantIdentity identity : gameSummary.getParticipantIdentities()) {
            if (identity.getPlayer().getSummonerId() == summonerID) {
                userParticipantId = identity.getParticipantId();
            }
        }


        if (userParticipantId == 0) {
            throw new Exception("获取用户位置失败");
        }

        Optional<Integer> userTeamID = Optional.empty();
        List<Integer> memberParticipantIDList = new ArrayList<>(4);
        Map<Integer, Participant> idMapParticipant = new HashMap<>(gameSummary.getParticipants().size());

        // 映射参与者ID和获取用户队伍ID
        for (Participant item : gameSummary.getParticipants()) {
            if (item.getParticipantId() == userParticipantId) {
                userTeamID = Optional.of(item.getTeamId());
            }
            idMapParticipant.put(item.getParticipantId(), item);
        }

        if (!userTeamID.isPresent()) {
            throw new Exception("获取用户队伍id失败");
        }

        // 获取同队参与者ID列表
        for (Participant item : gameSummary.getParticipants()) {
            if (item.getTeamId().equals(userTeamID.get())) {
                memberParticipantIDList.add(item.getParticipantId());
            }
        }

        int totalKill = 0;   // 总人头
        int totalDeath = 0;  // 总死亡
        int totalAssist = 0; // 总助攻
        int totalHurt = 0;   // 总伤害
        int totalMoney = 0;  // 总金钱

        for (Participant participant : gameSummary.getParticipants()) {
            if (!participant.getTeamId().equals(userTeamID.get())) {
                continue;
            }
            totalKill += participant.getStats().getKills();
            totalDeath += participant.getStats().getDeaths();
            totalAssist += participant.getStats().getAssists();
            totalHurt += participant.getStats().getTotalDamageDealtToChampions();
            totalMoney += participant.getStats().getGoldEarned();
        }

        Participant userParticipant = idMapParticipant.get(userParticipantId);
        boolean isSupportRole = userParticipant.getTimeline().getLane().equals("BOTTOM") &&
                userParticipant.getTimeline().getRole().equals("SUPPORT");

        // 一血击杀
        if (userParticipant.getStats().isFirstBloodKill()) {
            gameScore.add(calcScoreConf.getFirstBlood()[0], ScoreOption.FIRST_BLOOD_KILL);
        } else if (userParticipant.getStats().isFirstBloodAssist()) {
            gameScore.add(calcScoreConf.getFirstBlood()[1], ScoreOption.FIRST_BLOOD_ASSIST);
        }

        // 五杀、四杀、三杀
        if (userParticipant.getStats().getPentaKills() > 0) {
            gameScore.add(calcScoreConf.getPentaKills()[0], ScoreOption.PENTA_KILLS);
        } else if (userParticipant.getStats().getQuadraKills() > 0) {
            gameScore.add(calcScoreConf.getQuadraKills()[0], ScoreOption.QUADRA_KILLS);
        } else if (userParticipant.getStats().getTripleKills() > 0) {
            gameScore.add(calcScoreConf.getTripleKills()[0], ScoreOption.TRIPLE_KILLS);
        }

        // 参团率
        if (totalKill > 0) {
            int joinTeamRateRank = 1;
            double userJoinTeamKillRate = (double) (userParticipant.getStats().getAssists() + userParticipant.getStats().getKills()) / totalKill;
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
            int userMoney = userParticipant.getStats().getGoldEarned();
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
            int userHurt = userParticipant.getStats().getTotalDamageDealtToChampions();
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
            double userMoney2hurtRate = (double) userParticipant.getStats().getTotalDamageDealtToChampions() / userParticipant.getStats().getGoldEarned();
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
            int userVisionScore = userParticipant.getStats().getVisionScore();
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
            int totalMinionsKilled = userParticipant.getStats().getTotalMinionsKilled();
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
            double userKillRate = (double) userParticipant.getStats().getKills() / totalKill;
            adjustGameScoreForRate(userKillRate, userParticipant.getStats().getKills(), calcScoreConf.getKillRate(), gameScore, ScoreOption.KILL_RATE);
        }

        // 伤害占比
        if (totalHurt > 0) {
            double userHurtRate = (double) userParticipant.getStats().getTotalDamageDealtToChampions() / totalHurt;
            adjustGameScoreForRate(userHurtRate, userParticipant.getStats().getKills(), calcScoreConf.getHurtRate(), gameScore, ScoreOption.HURT_RATE);
        }

        // 助攻占比
        if (totalAssist > 0) {
            double userAssistRate = (double) userParticipant.getStats().getAssists() / totalAssist;
            adjustGameScoreForRate(userAssistRate, userParticipant.getStats().getKills(), calcScoreConf.getAssistRate(), gameScore, ScoreOption.ASSIST_RATE);
        }

        // KDA调整
        double userJoinTeamKillRate = (double) (userParticipant.getStats().getAssists() + userParticipant.getStats().getKills()) / totalKill;
        int userDeathTimes = userParticipant.getStats().getDeaths() == 0 ? 1 : userParticipant.getStats().getDeaths();
        double adjustVal = ((userParticipant.getStats().getKills() + userParticipant.getStats().getAssists()) / userDeathTimes - calcScoreConf.getAdjustKDA()[0] +
                (userParticipant.getStats().getKills() - userParticipant.getStats().getDeaths()) / calcScoreConf.getAdjustKDA()[1]) * userJoinTeamKillRate;
        gameScore.add(adjustVal, ScoreOption.KDA_ADJUST);

        return gameScore;
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
    private void adjustGameScoreForRate(double rate, int kills, List<RateItemConf> rateConf, ScoreWithReason gameScore, ScoreOption option) {
        for (RateItemConf confItem : rateConf) {
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
