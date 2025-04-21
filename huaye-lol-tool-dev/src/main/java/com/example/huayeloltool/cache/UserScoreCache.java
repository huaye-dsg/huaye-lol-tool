package com.example.huayeloltool.cache;

import com.google.common.collect.ImmutableMap;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 缓存当局队友战绩
 */
@Data
public class UserScoreCache {

    private static List<ScoreOverview> selfTeamScore = new ArrayList<>();
    private static List<ScoreOverview> enemyTeamScore = new ArrayList<>();

    @Data
    public static class ScoreOverview {
        private String summonerName;
        private String houseName;
        private Integer score;
        private String gameDetail;
        private String puuid;

        private String extMag;
    }


    public static Map<String, List<ScoreOverview>> getScore() {
        return ImmutableMap.of("selfTeamScore", selfTeamScore, "enemyTeamScore", enemyTeamScore);
    }

    public static void addSelfTeamScore(List<ScoreOverview> score) {
        selfTeamScore.addAll(score);
    }

    public static void addEnemyTeamScore(List<ScoreOverview> score) {
        enemyTeamScore.addAll(score);
    }

    public static void clear() {
        selfTeamScore.clear();
        enemyTeamScore.clear();
    }


}
