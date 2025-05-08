package com.example.huayeloltool.model.game;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;


@Data
public class Participant {
    public Integer championId; // 英雄ID
    //public String highestAchievedSeasonTier; // 玩家在该赛季达到的最高段位，例如：GOLD, PLATINUM
    public Integer participantId; // 参与者ID，与ParticipantIdentity中的participantId对应
    //public Integer spell1Id; // 召唤师技能1 ID
    //public Integer spell2Id; // 召唤师技能2 ID
    @SuppressWarnings("CommentedOutCode")
    public Stats stats; // 详细数据
    public Integer teamId; // 队伍ID (100 or 200)
    public Timeline timeline; // 时间线数据

    @Data
    public static class Stats {
        public int assists; // 助攻数
        public boolean causedEarlySurrender; // 是否发起提前投降
        public int champLevel; // 英雄等级
        public int combatPlayerScore; // 战斗玩家得分
        public int damageDealtToObjectives; // 对建筑物造成的伤害
        public int damageDealtToTurrets; // 对防御塔造成的伤害
        public int damageSelfMitigated; // 承受伤害减免
        public int deaths; // 死亡数
        public int doubleKills; // 双杀数
        public boolean earlySurrenderAccomplice; // 提前投降同谋
        public boolean firstBloodAssist; // 一血助攻
        public boolean firstBloodKill; // 一血
        public boolean firstInhibitorAssist; // 首个水晶助攻
        public boolean firstInhibitorKill; // 首个水晶
        public boolean firstTowerAssist; // 首个防御塔助攻
        public boolean firstTowerKill; // 首个防御塔
        public boolean gameEndedInEarlySurrender; // 游戏是否提前投降结束
        public boolean gameEndedInSurrender; // 游戏是否投降结束
        public int goldEarned; // 获得金币
        public int goldSpent; // 消耗金币
        public int inhibitorKills; // 摧毁水晶数
        public int item0; // 物品0 ID
        public int item1; // 物品1 ID
        public int item2; // 物品 2 ID
        public int item3; // 物品 3 ID
        public int item4; // 物品 4 ID
        public int item5; // 物品 5 ID
        public int item6; // 物品 6 ID
        public int killingSprees; // 击杀集锦
        public int kills; // 击杀数
        public int largestCriticalStrike; // 最大暴击伤害
        public int largestKillingSpree; // 最大击杀集锦
        public int largestMultiKill; // 最大多杀
        public int longestTimeSpentLiving; // 最长存活时间 (秒)
        public int magicDamageDealt; // 魔法伤害
        public int magicDamageDealtToChampions; // 对英雄造成的魔法伤害
        public int magicalDamageTaken; // 承受的魔法伤害
        public int neutralMinionsKilled; // 野怪击杀数
        public int neutralMinionsKilledEnemyJungle; // 敌方野区野怪击杀数
        public int neutralMinionsKilledTeamJungle; // 己方野区野怪击杀数
        public int objectivePlayerScore; // 目标玩家得分
        public int participantId; // 参与者ID
        public int pentaKills; // 五杀数
        //public int perk0; // 基石符文0 ID
        //public int perk0Var1; // 基石符文0 变量1
        //public int perk0Var2; // 基石符文0 变量2
        //public int perk0Var3; // 基石符文0 变量3
        //public int perk1; // 基石符文1 ID
        //public int perk1Var1; // 基石符文1 变量1
        //public int perk1Var2; // 基石符文1 变量2
        //public int perk1Var3; // 基石符文1 变量3
        //public int perk2; // 基石符文2 ID
        //public int perk2Var1; // 基石符文2 变量1
        //public int perk2Var2; // 基石符文2 变量2
        //public int perk2Var3; // 基石符文2 变量 3
        //public int perk3; // 基石符文3 ID
        //public int perk3Var1; // 基石符文3 变量1
        //public int perk3Var2; // 基石符文3 变量 2
        //public int perk3Var3; // 基石符文3 变量 3
        //public int perk4; // 副系符文1 ID
        //public int perk4Var1; // 副系符文1 变量1
        //public int perk4Var2; // 副系符文1 变量 2
        //public int perk4Var3; // 副系符文1 变量 3
        //public int perk5; // 副系符文2 ID
        //public int perk5Var1; // 副系符文2 变量1
        //public int perk5Var2; // 副系符文2 变量 2
        //public int perk5Var3; // 副系符文2 变量 3
        //public int perkPrimaryStyle; // 主要符文风格ID
        //public int perkSubStyle; // 次要符文风格ID
        @SuppressWarnings("CommentedOutCode")
        public int physicalDamageDealt; // 物理伤害
        public int physicalDamageDealtToChampions; // 对英雄造成的物理伤害
        public int physicalDamageTaken; // 承受的物理伤害
        //public int playerScore0; // 玩家分数0
        //public int playerScore1; // 玩家分数1
        //public int playerScore2; // 玩家分数2
        //public int playerScore3; // 玩家分数3
        //public int playerScore4; // 玩家分数4
        //public int playerScore5; // 玩家分数5
        //public int playerScore6; // 玩家分数6
        //public int playerScore7; // 玩家分数7
        //public int playerScore8; // 玩家分数8
        //public int playerScore9; // 玩家分数9
        @SuppressWarnings("CommentedOutCode")
        public int quadraKills; // 四杀数
        public int sightWardsBoughtInGame; // 真眼购买数量
        public boolean teamEarlySurrendered; // 队伍是否提前投降
        public int timeCCingOthers; // 限制敌方英雄行动的时间 (秒)
        public int totalDamageDealt; // 总伤害
        public int totalDamageDealtToChampions; // 对英雄造成的总伤害
        public int totalDamageTaken; // 总承受伤害
        public int totalHeal; // 总治疗量
        public int totalMinionsKilled; // 总小兵击杀数
        public int totalPlayerScore; // 玩家总分
        public int totalScoreRank; // 总分排名
        public int totalTimeCrowdControlDealt; // 总控制时间
        public int totalUnitsHealed; // 总治疗单位数
        public int tripleKills; // 三杀数
        public int trueDamageDealt; // 真实伤害
        public int trueDamageDealtToChampions; // 对英雄造成的真实伤害
        public int trueDamageTaken; // 承受的真实伤害
        public int turretKills; // 防御塔击杀数
        public int unrealKills; // 虚幻击杀数
        public int visionScore; // 视野得分
        public int visionWardsBoughtInGame; // 假眼购买数量
        public int wardsKilled; // 眼位摧毁数
        public int wardsPlaced; // 眼位放置数
        public Boolean win; // 是否胜利

        // Getter, Setter 和 构造方法
    }

    @Data
    public static class Timeline {
        public PerMinDeltas creepsPerMinDeltas; // 每分钟小兵击杀数
        public PerMinDeltas csDiffPerMinDeltas; // 每分钟补刀差
        public PerMinDeltas damageTakenDiffPerMinDeltas; // 每分钟承受伤害差值
        public PerMinDeltas damageTakenPerMinDeltas; // 每分钟承受伤害
        public PerMinDeltas goldPerMinDeltas; // 每分钟金币
        public String lane;  // 路线 (TOP, JUNGLE, MIDDLE, BOTTOM)
        public int participantId; // 参与者ID
        public String role;  // 角色 (SOLO, DUO, DUO_CARRY, DUO_SUPPORT, NONE)
        public PerMinDeltas xpDiffPerMinDeltas; // 每分钟经验差值
        public PerMinDeltas xpPerMinDeltas; // 每分钟经验

        // Getter, Setter 和 构造方法
    }

    @Data
    public static class PerMinDeltas {
        @JSONField(name = "0-10")
        public Double ten; // 0-10分钟的数据

        @JSONField(name = "10-20")
        public Double twenty; // 10-20分钟的数据

        @JSONField(name = "20-30")
        public Double thirty; // 20-30分钟的数据

        @JSONField(name = "30-40")
        public Double forty; // 30-40分钟的数据

        @JSONField(name = "40-50")
        public Double fifty; // 40-50分钟的数据

        @JSONField(name = "50-60")
        public Double sixty; // 50-60分钟的数据
    }
}
