package com.example.huayeloltool.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameInfo {
//    private String endOfGameResult; // 游戏结束结果，这个不是输赢
//    private Long gameCreation; // 游戏创建时间 (Unix时间戳，毫秒)
//    private String gameCreationDate; // 游戏创建日期 (字符串格式)
    private Integer gameDuration; // 游戏持续时间
    private Long gameId; // 游戏ID
//    private String gameMode; // 游戏模式，例如：CLASSIC, ARAM
//    private String gameType; // 游戏类型，例如：MATCHED_GAME
//    private String gameVersion; // 游戏版本
//    private Integer mapId; // 地图ID
//    private List<ParticipantIdentity> participantIdentities; // 参与者身份列表，包含玩家信息
    private List<Participant> participants; // 参与者列表，包含英雄、召唤师技能、属性等信息
//    private String platformId; // 平台ID，例如：KR, NA1
    private Integer queueId; // 队列ID，例如：420 (排位单双), 400 (匹配)
//    private Integer seasonId; // 赛季ID
//    private List<Team> teams; // 队伍列表，包含队伍ban选、击杀数等信息


    @Data
    public static class ParticipantIdentity {
        private Integer participantId; // 参与者ID，与Participant中的participantId对应
        private Player player; // 玩家信息
    }

    @Data
    public static class Player {
//        private long accountId; // 账号ID (已废弃，不再可用)
//        private long currentAccountId; // 当前账号ID (已废弃，不再可用)
//        private String currentPlatformId; // 当前平台ID (已废弃，不再可用)
        private String gameName; // 游戏名称
        private String matchHistoryUri; // 比赛历史URI
        private String platformId; // 平台ID
        private Integer profileIcon; // 召唤师头像ID
        private String puuid; // 玩家PUUID (永久唯一用户ID)
        private long summonerId; // 召唤师ID
        private String summonerName; // 召唤师名称
        private String tagLine; // 游戏tag
    }

    @Data
    public static class Participant {
        private Integer championId; // 英雄ID
        private String highestAchievedSeasonTier; // 玩家在该赛季达到的最高段位，例如：GOLD, PLATINUM
        private Integer participantId; // 参与者ID，与ParticipantIdentity中的participantId对应
        private Integer spell1Id; // 召唤师技能1 ID
        private Integer spell2Id; // 召唤师技能2 ID
        private Stats stats; // 统计数据
        private Integer teamId; // 队伍ID (100 or 200)
        private Timeline timeline; // 时间线数据
    }

    @Data
    public static class Stats {
        private Integer assists; // 助攻数
//        private Boolean causedEarlySurrender; // 是否发起提前投降
        private Integer champLevel; // 英雄等级
//        private Integer combatPlayerScore; // 战斗玩家得分
//        private Integer damageDealtToObjectives; // 对建筑物造成的伤害
//        private Integer damageDealtToTurrets; // 对防御塔造成的伤害
//        private Integer damageSelfMitigated; // 承受伤害减免
        private Integer deaths; // 死亡数
        private Integer doubleKills; // 双杀数
//        private Boolean earlySurrenderAccomplice; // 提前投降同谋
//        private Boolean firstBloodAssist; // 一血助攻
//        private Boolean firstBloodKill; // 一血
//        private Boolean firstInhibitorAssist; // 首个水晶助攻
//        private Boolean firstInhibitorKill; // 首个水晶
//        private Boolean firstTowerAssist; // 首个防御塔助攻
//        private Boolean firstTowerKill; // 首个防御塔
//        private Boolean gameEndedInEarlySurrender; // 游戏是否提前投降结束
//        private Boolean gameEndedInSurrender; // 游戏是否投降结束
//        private Integer goldEarned; // 获得金币
//        private Integer goldSpent; // 消耗金币
//        private Integer inhibitorKills; // 摧毁水晶数
//        private Integer item0; // 物品0 ID
//        private Integer item1; // 物品1 ID
//        private Integer item2; // 物品2 ID
//        private Integer item3; // 物品3 ID
//        private Integer item4; // 物品4 ID
//        private Integer item5; // 物品5 ID
//        private Integer item6; // 物品6 ID
//        private Integer killingSprees; // 击杀集锦
        private Integer kills; // 击杀数
//        private Integer largestCriticalStrike; // 最大暴击伤害
//        private Integer largestKillingSpree; // 最大击杀集锦
//        private Integer largestMultiKill; // 最大多杀
//        private Integer longestTimeSpentLiving; // 最长存活时间 (秒)
//        private Integer magicDamageDealt; // 魔法伤害
//        private Integer magicDamageDealtToChampions; // 对英雄造成的魔法伤害
//        private Integer magicalDamageTaken; // 承受的魔法伤害
//        private Integer neutralMinionsKilled; // 野怪击杀数
//        private Integer neutralMinionsKilledEnemyJungle; // 敌方野区野怪击杀数
//        private Integer neutralMinionsKilledTeamJungle; // 己方野区野怪击杀数
//        private Integer objectivePlayerScore; // 目标玩家得分
        private Integer participantId; // 参与者ID
        private Integer pentaKills; // 五杀数
//        private Integer perk0; // 基石符文0 ID
//        private Integer perk0Var1; // 基石符文0 变量1
//        private Integer perk0Var2; // 基石符文0 变量2
//        private Integer perk0Var3; // 基石符文0 变量3
//        private Integer perk1; // 基石符文1 ID
//        private Integer perk1Var1; // 基石符文1 变量1
//        private Integer perk1Var2; // 基石符文1 变量2
//        private Integer perk1Var3; // 基石符文1 变量3
//        private Integer perk2; // 基石符文2 ID
//        private Integer perk2Var1; // 基石符文2 变量1
//        private Integer perk2Var2; // 基石符文2 变量 2
//        private Integer perk2Var3; // 基石符文2 变量3
//        private Integer perk3; // 基石符文3 ID
//        private Integer perk3Var1; // 基石符文3 变量1
//        private Integer perk3Var2; // 基石符文3 变量 2
//        private Integer perk3Var3; // 基石符文3 变量3
//        private Integer perk4; // 副系符文1 ID
//        private Integer perk4Var1; // 副系符文1 变量1
//        private Integer perk4Var2; // 副系符文1 变量 2
//        private Integer perk4Var3; // 副系符文1 变量 3
//        private Integer perk5; // 副系符文2 ID
//        private Integer perk5Var1; // 副系符文2 变量1
//        private Integer perk5Var2; // 副系符文2 变量 2
//        private Integer perk5Var3; // 副系符文2 变量 3
//        private Integer perkPrimaryStyle; // 主要符文风格ID
//        private Integer perkSubStyle; // 次要符文风格ID
//        private Integer physicalDamageDealt; // 物理伤害
//        private Integer physicalDamageDealtToChampions; // 对英雄造成的物理伤害
//        private Integer physicalDamageTaken; // 承受的物理伤害
//        private Integer playerAugment1; // 海克斯强化1(仅限云顶之奕)
//        private Integer playerAugment2; // 海克斯强化2(仅限云顶之奕)
//        private Integer playerAugment3; // 海克斯强化3(仅限云顶之奕)
//        private Integer playerAugment4; // 海克斯强化4(仅限云顶之奕)
//        private Integer playerAugment5; // 海克斯强化5(仅限云顶之奕)
//        private Integer playerAugment6; // 海克斯强化6(仅限云顶之奕)
//        private Integer playerScore0; // 玩家分数0
//        private Integer playerScore1; // 玩家分数1
//        private Integer playerScore2; // 玩家分数2
//        private Integer playerScore3; // 玩家分数3
//        private Integer playerScore4; // 玩家分数4
//        private Integer playerScore5; // 玩家分数5
//        private Integer playerScore6; // 玩家分数6
//        private Integer playerScore7; // 玩家分数7
//        private Integer playerScore8; // 玩家分数8
//        private Integer playerScore9; // 玩家分数9
//        private Integer playerSubteamId; // 玩家子团队ID
//        private Integer quadraKills; // 四杀数
//        private Integer sightWardsBoughtInGame; // 真眼购买数量
//        private Integer subteamPlacement; // 子团队排名
//        private Boolean teamEarlySurrendered; // 队伍是否提前投降
//        private Integer timeCCingOthers; // 限制敌方英雄行动的时间 (秒)
//        private Integer totalDamageDealt; // 总伤害
//        private Integer totalDamageDealtToChampions; // 对英雄造成的总伤害
//        private Integer totalDamageTaken; // 总承受伤害
//        private Integer totalHeal; // 总治疗量
//        private Integer totalMinionsKilled; // 总小兵击杀数
//        private Integer totalPlayerScore; // 玩家总分
//        private Integer totalScoreRank; // 总分排名
//        private Integer totalTimeCrowdControlDealt; // 总控制时间
//        private Integer totalUnitsHealed; // 总治疗单位数
//        private Integer tripleKills; // 三杀数
//        private Integer trueDamageDealt; // 真实伤害
//        private Integer trueDamageDealtToChampions; // 对英雄造成的真实伤害
//        private Integer trueDamageTaken; // 承受的真实伤害
//        private Integer turretKills; // 防御塔击杀数
//        private Integer unrealKills; // 虚幻击杀数
//        private Integer visionScore; // 视野得分
//        private Integer visionWardsBoughtInGame; // 假眼购买数量
//        private Integer wardsKilled; // 眼位摧毁数
//        private Integer wardsPlaced; // 眼位放置数
        private Boolean win; // 是否胜利
    }

    @Data
    public static class Timeline {
        private Object creepsPerMinDeltas; // 每分钟小兵击杀差值
        private Object csDiffPerMinDeltas; // 每分钟补刀差
        private Object damageTakenDiffPerMinDeltas; // 每分钟承受伤害差值
        private Object damageTakenPerMinDeltas; // 每分钟承受伤害
        private Object goldPerMinDeltas; // 每分钟金币
        private String lane; // 路线 (TOP, JUNGLE, MIDDLE, BOTTOM)
        private Integer participantId; // 参与者ID
        private String role; // 角色 (SOLO, DUO, DUO_CARRY, DUO_SUPPORT, NONE)
        private Object xpDiffPerMinDeltas; // 每分钟经验差值
        private Object xpPerMinDeltas; // 每分钟经验
    }

    @Data
    public static class Team {
        private List<Champion> bans; // 禁用英雄列表
        private Integer baronKills; // 大龙击杀数
        private Integer dominionVictoryScore; // 据点争夺胜利得分
        private Integer dragonKills; // 小龙击杀数
        private Boolean firstBaron; // 是否首个大龙
        private Boolean firstBlood; // 是否一血
        private Boolean firstDargon; // 是否首个小龙
        private Boolean firstInhibitor; // 是否首个水晶
        private Boolean firstTower; // 是否首个防御塔
        private Integer hordeKills; // horde击杀数 (已废弃)
        private Integer inhibitorKills; // 摧毁水晶数
        private Integer riftHeraldKills; // 峡谷先锋击杀数
        private Integer teamId; // 队伍ID (100 or 200)
        private Integer towerKills; // 防御塔击杀数
        private Integer vilemawKills; // vilemaw击杀数 (已废弃)
        private String win; // 是否胜利 (Win or Fail)
    }

    /**
     * ban用的英雄信息
     */
    @Data
    public static class Champion {
        /**
         * 禁用的英雄ID
         */
        private Integer championId;

        /**
         * 轮次信息（没啥用）
         */
        private Integer pickTurn;
    }

}
