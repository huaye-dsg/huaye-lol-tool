package com.example.huayeloltool.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;


@Data
public class Participant {
    private Integer championId;
    private String highestAchievedSeasonTier;
    private Integer participantId;
    private Integer spell1Id;
    private Integer spell2Id;
    private Stats stats;
    private Integer teamId;
    private Timeline timeline;

    @Data
    public static class Stats {
        private int assists;
        private boolean causedEarlySurrender;
        private int champLevel;
        private int combatPlayerScore;
        private int damageDealtToObjectives;
        private int damageDealtToTurrets;
        private int damageSelfMitigated;
        private int deaths;
        private int doubleKills;
        private boolean earlySurrenderAccomplice;
        private boolean firstBloodAssist;
        private boolean firstBloodKill;
        private boolean firstInhibitorAssist;
        private boolean firstInhibitorKill;
        private boolean firstTowerAssist;
        private boolean firstTowerKill;
        private boolean gameEndedInEarlySurrender;
        private boolean gameEndedInSurrender;
        private int goldEarned;
        private int goldSpent;
        private int inhibitorKills;
        private int item0;
        private int item1;
        private int item2;
        private int item3;
        private int item4;
        private int item5;
        private int item6;
        private int killingSprees;
        private int kills;
        private int largestCriticalStrike;
        private int largestKillingSpree;
        private int largestMultiKill;
        private int longestTimeSpentLiving;
        private int magicDamageDealt;
        private int magicDamageDealtToChampions;
        private int magicalDamageTaken;
        private int neutralMinionsKilled;
        private int neutralMinionsKilledEnemyJungle;
        private int neutralMinionsKilledTeamJungle;
        private int objectivePlayerScore;
        private int participantId;
        private int pentaKills;
        private int perk0;
        private int perk0Var1;
        private int perk0Var2;
        private int perk0Var3;
        private int perk1;
        private int perk1Var1;
        private int perk1Var2;
        private int perk1Var3;
        private int perk2;
        private int perk2Var1;
        private int perk2Var2;
        private int perk2Var3;
        private int perk3;
        private int perk3Var1;
        private int perk3Var2;
        private int perk3Var3;
        private int perk4;
        private int perk4Var1;
        private int perk4Var2;
        private int perk4Var3;
        private int perk5;
        private int perk5Var1;
        private int perk5Var2;
        private int perk5Var3;
        private int perkPrimaryStyle;
        private int perkSubStyle;
        private int physicalDamageDealt;
        private int physicalDamageDealtToChampions;
        private int physicalDamageTaken;
        private int playerScore0;
        private int playerScore1;
        private int playerScore2;
        private int playerScore3;
        private int playerScore4;
        private int playerScore5;
        private int playerScore6;
        private int playerScore7;
        private int playerScore8;
        private int playerScore9;
        private int quadraKills;
        private int sightWardsBoughtInGame;
        private boolean teamEarlySurrendered;
        private int timeCCingOthers;
        private int totalDamageDealt;
        private int totalDamageDealtToChampions;
        private int totalDamageTaken;
        private int totalHeal;
        private int totalMinionsKilled;
        private int totalPlayerScore;
        private int totalScoreRank;
        private int totalTimeCrowdControlDealt;
        private int totalUnitsHealed;
        private int tripleKills;
        private int trueDamageDealt;
        private int trueDamageDealtToChampions;
        private int trueDamageTaken;
        private int turretKills;
        private int unrealKills;
        private int visionScore;
        private int visionWardsBoughtInGame;
        private int wardsKilled;
        private int wardsPlaced;
        private boolean win;

        // Getter, Setter 和 构造方法
    }

    @Data
    public static class Timeline {
        private PerMinDeltas creepsPerMinDeltas;
        private PerMinDeltas csDiffPerMinDeltas;
        private PerMinDeltas damageTakenDiffPerMinDeltas;
        private PerMinDeltas damageTakenPerMinDeltas;
        private PerMinDeltas goldPerMinDeltas;
        private String lane;  // 假设 models.Lane 在 Java 中是一个字符串
        private int participantId;
        private String role;  // 假设 models.ChampionRole 在 Java 中是一个字符串
        private PerMinDeltas xpDiffPerMinDeltas;
        private PerMinDeltas xpPerMinDeltas;

        // Getter, Setter 和 构造方法
    }

    @Data
    public class PerMinDeltas {
        @JSONField(name = "0-10")
        private Double ten;

        @JSONField(name = "10-20")
        private Double twenty;

        @JSONField(name = "20-30")
        private Double thirty;

        @JSONField(name = "30-40")
        private Double forty;

        @JSONField(name = "40-50")
        private Double fifty;

        @JSONField(name = "50-60")
        private Double sixty;
    }

}
