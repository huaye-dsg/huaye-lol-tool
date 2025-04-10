package com.example.huayeloltool.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameInfo {

    private String endOfGameResult;
    private Long gameCreation;
    private String gameCreationDate;
    private Integer gameDuration;
    private Long gameId;
    private String gameMode;
    private String gameType;
    private String gameVersion;
    private Integer mapId;
    private List<ParticipantIdentity> participantIdentities;
    private List<Participant> participants;
    private String platformId;
    private String queueId;
    private Integer seasonId;
    private List<Team> teams;


    @Data
    public static class ParticipantIdentity {
        private Integer participantId;
        private Player player;
    }

    @Data
    public static class Player {
        private long accountId;
        private long currentAccountId;
        private String currentPlatformId;
        private String gameName;
        private String matchHistoryUri;
        private String platformId;
        private Integer profileIcon;
        private String puuid;
        private long summonerId;
        private String summonerName;
        private String tagLine;
    }

    @Data
    public static class Participant {
        private Integer championId;
        private String highestAchievedSeasonTier;
        private Integer participantId;
        private Integer spell1Id;
        private Integer spell2Id;
        private Stats stats;
        private Integer teamId;
        private Timeline timeline;
    }

    @Data
    public static class Stats {
        private Integer assists;
        private Boolean causedEarlySurrender;
        private Integer champLevel;
        private Integer combatPlayerScore;
        private Integer damageDealtToObjectives;
        private Integer damageDealtToTurrets;
        private Integer damageSelfMitigated;
        private Integer deaths;
        private Integer doubleKills;
        private Boolean earlySurrenderAccomplice;
        private Boolean firstBloodAssist;
        private Boolean firstBloodKill;
        private Boolean firstInhibitorAssist;
        private Boolean firstInhibitorKill;
        private Boolean firstTowerAssist;
        private Boolean firstTowerKill;
        private Boolean gameEndedInEarlySurrender;
        private Boolean gameEndedInSurrender;
        private Integer goldEarned;
        private Integer goldSpent;
        private Integer inhibitorKills;
        private Integer item0;
        private Integer item1;
        private Integer item2;
        private Integer item3;
        private Integer item4;
        private Integer item5;
        private Integer item6;
        private Integer killingSprees;
        private Integer kills;
        private Integer largestCriticalStrike;
        private Integer largestKillingSpree;
        private Integer largestMultiKill;
        private Integer longestTimeSpentLiving;
        private Integer magicDamageDealt;
        private Integer magicDamageDealtToChampions;
        private Integer magicalDamageTaken;
        private Integer neutralMinionsKilled;
        private Integer neutralMinionsKilledEnemyJungle;
        private Integer neutralMinionsKilledTeamJungle;
        private Integer objectivePlayerScore;
        private Integer participantId;
        private Integer pentaKills;
        private Integer perk0;
        private Integer perk0Var1;
        private Integer perk0Var2;
        private Integer perk0Var3;
        private Integer perk1;
        private Integer perk1Var1;
        private Integer perk1Var2;
        private Integer perk1Var3;
        private Integer perk2;
        private Integer perk2Var1;
        private Integer perk2Var2;
        private Integer perk2Var3;
        private Integer perk3;
        private Integer perk3Var1;
        private Integer perk3Var2;
        private Integer perk3Var3;
        private Integer perk4;
        private Integer perk4Var1;
        private Integer perk4Var2;
        private Integer perk4Var3;
        private Integer perk5;
        private Integer perk5Var1;
        private Integer perk5Var2;
        private Integer perk5Var3;
        private Integer perkPrimaryStyle;
        private Integer perkSubStyle;
        private Integer physicalDamageDealt;
        private Integer physicalDamageDealtToChampions;
        private Integer physicalDamageTaken;
        private Integer playerAugment1;
        private Integer playerAugment2;
        private Integer playerAugment3;
        private Integer playerAugment4;
        private Integer playerAugment5;
        private Integer playerAugment6;
        private Integer playerScore0;
        private Integer playerScore1;
        private Integer playerScore2;
        private Integer playerScore3;
        private Integer playerScore4;
        private Integer playerScore5;
        private Integer playerScore6;
        private Integer playerScore7;
        private Integer playerScore8;
        private Integer playerScore9;
        private Integer playerSubteamId;
        private Integer quadraKills;
        private Integer sightWardsBoughtInGame;
        private Integer subteamPlacement;
        private Boolean teamEarlySurrendered;
        private Integer timeCCingOthers;
        private Integer totalDamageDealt;
        private Integer totalDamageDealtToChampions;
        private Integer totalDamageTaken;
        private Integer totalHeal;
        private Integer totalMinionsKilled;
        private Integer totalPlayerScore;
        private Integer totalScoreRank;
        private Integer totalTimeCrowdControlDealt;
        private Integer totalUnitsHealed;
        private Integer tripleKills;
        private Integer trueDamageDealt;
        private Integer trueDamageDealtToChampions;
        private Integer trueDamageTaken;
        private Integer turretKills;
        private Integer unrealKills;
        private Integer visionScore;
        private Integer visionWardsBoughtInGame;
        private Integer wardsKilled;
        private Integer wardsPlaced;
        private Boolean win;
    }

    @Data
    public static class Timeline {
        private Object creepsPerMinDeltas;
        private Object csDiffPerMinDeltas;
        private Object damageTakenDiffPerMinDeltas;
        private Object damageTakenPerMinDeltas;
        private Object goldPerMinDeltas;
        private String lane;
        private Integer participantId;
        private String role;
        private Object xpDiffPerMinDeltas;
        private Object xpPerMinDeltas;
    }

    @Data
    public static class Team {
        private List<Champion> bans;
        private Integer baronKills;
        private Integer dominionVictoryScore;
        private Integer dragonKills;
        private Boolean firstBaron;
        private Boolean firstBlood;
        private Boolean firstDargon;
        private Boolean firstInhibitor;
        private Boolean firstTower;
        private Integer hordeKills;
        private Integer inhibitorKills;
        private Integer riftHeraldKills;
        private Integer teamId;
        private Integer towerKills;
        private Integer vilemawKills;
        private String win;
    }

    /**
     * ban用的英雄信息
     */
    @Data
    public static class Champion {
        /**
         *  禁用的英雄ID
//         */
        private Integer championId;

        /**
         * 轮次信息（没啥用）
         */
        private Integer pickTurn;
    }

}
