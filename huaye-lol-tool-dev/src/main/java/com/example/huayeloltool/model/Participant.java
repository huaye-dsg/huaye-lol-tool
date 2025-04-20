package com.example.huayeloltool.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;


@Data
public class Participant {
    private Integer championId; // 英雄ID
    private String highestAchievedSeasonTier; // 玩家在该赛季达到的最高段位，例如：GOLD, PLATINUM
    private Integer participantId; // 参与者ID，与ParticipantIdentity中的participantId对应
    private Integer spell1Id; // 召唤师技能1 ID
    private Integer spell2Id; // 召唤师技能2 ID
    private Stats stats; // 统计数据
    private Integer teamId; // 队伍ID (100 or 200)
    private Timeline timeline; // 时间线数据

    @Data
    public static class Stats {
        private int assists; // 助攻数
        private boolean causedEarlySurrender; // 是否发起提前投降
        private int champLevel; // 英雄等级
        private int combatPlayerScore; // 战斗玩家得分
        private int damageDealtToObjectives; // 对建筑物造成的伤害
        private int damageDealtToTurrets; // 对防御塔造成的伤害
        private int damageSelfMitigated; // 承受伤害减免
        private int deaths; // 死亡数
        private int doubleKills; // 双杀数
        private boolean earlySurrenderAccomplice; // 提前投降同谋
        private boolean firstBloodAssist; // 一血助攻
        private boolean firstBloodKill; // 一血
        private boolean firstInhibitorAssist; // 首个水晶助攻
        private boolean firstInhibitorKill; // 首个水晶
        private boolean firstTowerAssist; // 首个防御塔助攻
        private boolean firstTowerKill; // 首个防御塔
        private boolean gameEndedInEarlySurrender; // 游戏是否提前投降结束
        private boolean gameEndedInSurrender; // 游戏是否投降结束
        private int goldEarned; // 获得金币
        private int goldSpent; // 消耗金币
        private int inhibitorKills; // 摧毁水晶数
//        private int item0; // 物品0 ID
//        private int item1; // 物品1 ID
//        private int item2; // 物品 2 ID
//        private int item3; // 物品 3 ID
//        private int item4; // 物品 4 ID
//        private int item5; // 物品 5 ID
//        private int item6; // 物品 6 ID
//        private int killingSprees; // 击杀集锦
        private int kills; // 击杀数
//        private int largestCriticalStrike; // 最大暴击伤害
//        private int largestKillingSpree; // 最大击杀集锦
        private int largestMultiKill; // 最大多杀
        private int longestTimeSpentLiving; // 最长存活时间 (秒)
        private int magicDamageDealt; // 魔法伤害
        private int magicDamageDealtToChampions; // 对英雄造成的魔法伤害
        private int magicalDamageTaken; // 承受的魔法伤害
//        private int neutralMinionsKilled; // 野怪击杀数
//        private int neutralMinionsKilledEnemyJungle; // 敌方野区野怪击杀数
//        private int neutralMinionsKilledTeamJungle; // 己方野区野怪击杀数
//        private int objectivePlayerScore; // 目标玩家得分
//        private int participantId; // 参与者ID
        private int pentaKills; // 五杀数
//        private int perk0; // 基石符文0 ID
//        private int perk0Var1; // 基石符文0 变量1
//        private int perk0Var2; // 基石符文0 变量2
//        private int perk0Var3; // 基石符文0 变量3
//        private int perk1; // 基石符文1 ID
//        private int perk1Var1; // 基石符文1 变量1
//        private int perk1Var2; // 基石符文1 变量2
//        private int perk1Var3; // 基石符文1 变量3
//        private int perk2; // 基石符文2 ID
//        private int perk2Var1; // 基石符文2 变量1
//        private int perk2Var2; // 基石符文2 变量2
//        private int perk2Var3; // 基石符文2 变量 3
//        private int perk3; // 基石符文3 ID
//        private int perk3Var1; // 基石符文3 变量1
//        private int perk3Var2; // 基石符文3 变量 2
//        private int perk3Var3; // 基石符文3 变量 3
//        private int perk4; // 副系符文1 ID
//        private int perk4Var1; // 副系符文1 变量1
//        private int perk4Var2; // 副系符文1 变量 2
//        private int perk4Var3; // 副系符文1 变量 3
//        private int perk5; // 副系符文2 ID
//        private int perk5Var1; // 副系符文2 变量1
//        private int perk5Var2; // 副系符文2 变量 2
//        private int perk5Var3; // 副系符文2 变量 3
//        private int perkPrimaryStyle; // 主要符文风格ID
//        private int perkSubStyle; // 次要符文风格ID
//        private int physicalDamageDealt; // 物理伤害
//        private int physicalDamageDealtToChampions; // 对英雄造成的物理伤害
//        private int physicalDamageTaken; // 承受的物理伤害
//        private int playerScore0; // 玩家分数0
//        private int playerScore1; // 玩家分数1
//        private int playerScore2; // 玩家分数2
//        private int playerScore3; // 玩家分数3
//        private int playerScore4; // 玩家分数4
//        private int playerScore5; // 玩家分数5
//        private int playerScore6; // 玩家分数6
//        private int playerScore7; // 玩家分数7
//        private int playerScore8; // 玩家分数8
//        private int playerScore9; // 玩家分数9
        private int quadraKills; // 四杀数
        private int sightWardsBoughtInGame; // 真眼购买数量
        private boolean teamEarlySurrendered; // 队伍是否提前投降
        private int timeCCingOthers; // 限制敌方英雄行动的时间 (秒)
        private int totalDamageDealt; // 总伤害
        private int totalDamageDealtToChampions; // 对英雄造成的总伤害
        private int totalDamageTaken; // 总承受伤害
        private int totalHeal; // 总治疗量
        private int totalMinionsKilled; // 总小兵击杀数
        private int totalPlayerScore; // 玩家总分
        private int totalScoreRank; // 总分排名
        private int totalTimeCrowdControlDealt; // 总控制时间
        private int totalUnitsHealed; // 总治疗单位数
        private int tripleKills; // 三杀数
        private int trueDamageDealt; // 真实伤害
        private int trueDamageDealtToChampions; // 对英雄造成的真实伤害
        private int trueDamageTaken; // 承受的真实伤害
        private int turretKills; // 防御塔击杀数
        private int unrealKills; // 虚幻击杀数
        private int visionScore; // 视野得分
        private int visionWardsBoughtInGame; // 假眼购买数量
        private int wardsKilled; // 眼位摧毁数
        private int wardsPlaced; // 眼位放置数
        private Boolean win; // 是否胜利

        // Getter, Setter 和 构造方法
    }

    @Data
    public static class Timeline {
        private PerMinDeltas creepsPerMinDeltas; // 每分钟小兵击杀数
        private PerMinDeltas csDiffPerMinDeltas; // 每分钟补刀差
        private PerMinDeltas damageTakenDiffPerMinDeltas; // 每分钟承受伤害差值
        private PerMinDeltas damageTakenPerMinDeltas; // 每分钟承受伤害
        private PerMinDeltas goldPerMinDeltas; // 每分钟金币
        private String lane;  // 路线 (TOP, JUNGLE, MIDDLE, BOTTOM)
        private int participantId; // 参与者ID
        private String role;  // 角色 (SOLO, DUO, DUO_CARRY, DUO_SUPPORT, NONE)
        private PerMinDeltas xpDiffPerMinDeltas; // 每分钟经验差值
        private PerMinDeltas xpPerMinDeltas; // 每分钟经验

        // Getter, Setter 和 构造方法
    }

    @Data
    public static class PerMinDeltas {
        @JSONField(name = "0-10")
        private Double ten; // 0-10分钟的数据

        @JSONField(name = "10-20")
        private Double twenty; // 10-20分钟的数据

        @JSONField(name = "20-30")
        private Double thirty; // 20-30分钟的数据

        @JSONField(name = "30-40")
        private Double forty; // 30-40分钟的数据

        @JSONField(name = "40-50")
        private Double fifty; // 40-50分钟的数据

        @JSONField(name = "50-60")
        private Double sixty; // 50-60分钟的数据
    }
}
