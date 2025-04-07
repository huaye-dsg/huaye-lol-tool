package com.example.huayeloltool.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * 游戏相关的枚举类型定义
 */
public class GameEnums {
    /**
     * 游戏模式枚举
     */
    @Getter
    public enum GameMode {
        NONE(""),          // 游戏模式：无
        CLASSIC("CLASSIC"),// 经典模式
        ARAM("ARAM"),      // 大乱斗
        TFT("TFT"),        // 云顶之弈
        URF("URF"),        // 无限火力
        CUSTOM("PRACTICETOOL"); // 自定义

        private final String value;

        GameMode(String value) {
            this.value = value;
        }
    }

    /**
     * 游戏队列类型枚举
     */
    @Getter
    public enum GameQueueType {
        NORMAL("NORMAL"),            // 匹配
        RANK_SOLO("RANKED_SOLO_5x5"),// 单双排
        RANK_FLEX("RANKED_FLEX_SR"), // 组排
        ARAM("ARAM_UNRANKED_5x5"),   // 大乱斗5v5
        URF("URF"),                  // 无限火力
        BOT("BOT"),                  // 人机
        CUSTOM("PRACTICETOOL");      // 自定义

        private final String value;

        GameQueueType(String value) {
            this.value = value;
        }
    }

    /**
     * 游戏状态枚举
     */
    @Getter
    public enum GameStatus {
        IN_QUEUE("inQueue"),             // 队列中
        IN_GAME("inGame"),               // 游戏中
        CHAMPION_SELECT("championSelect"),// 英雄选择中
        OUT_OF_GAME("outOfGame"),        // 退出游戏中
        HOST_NORMAL("hosting_NORMAL"),   // 匹配组队中-队长
        HOST_RANK_SOLO("hosting_RANKED_SOLO_5x5"),// 单排组队中-队长
        HOST_RANK_FLEX("hosting_RANKED_FLEX_SR"), // 组排组队中-队长
        HOST_ARAM("hosting_ARAM_UNRANKED_5x5"),   // 大乱斗5v5组队中-队长
        HOST_URF("hosting_URF"),         // 无限火力组队中-队长
        HOST_BOT("hosting_BOT");         // 人机组队中-队长

        private final String value;

        GameStatus(String value) {
            this.value = value;
        }
    }

    /**
     * 游戏流程枚举
     */
    @Getter
    public enum GameFlow {
        CHAMPION_SELECT("ChampSelect"), // 英雄选择中
        READY_CHECK("ReadyCheck"),      // 等待接受对局
        IN_PROGRESS("InProgress"),      // 进行中
        MATCHMAKING("Matchmaking"),     // 匹配中
        NONE("None");                   // 无

        private final String value;

        GameFlow(String value) {
            this.value = value;
        }

        public static GameFlow getByValue(String status){

            GameFlow[] values = GameFlow.values();
            for (GameFlow gameFlow : values){
                if (Objects.equals(gameFlow.getValue(), status)){
                    return gameFlow;
                }
            }
            return NONE;
        }



    }

    /**
     * 排位等级枚举
     */
    @Getter
    public enum RankTier {
        IRON("IRON"),        // 黑铁
        BRONZE("BRONZE"),    // 青铜
        SILVER("SILVER"),    // 白银
        GOLD("GOLD"),        // 黄金
        PLATINUM("PLATINUM"),// 白金
        DIAMOND("DIAMOND"),  // 钻石
        MASTER("MASTER"),    // 大师
        GRANDMASTER("GRANDMASTER"),// 宗师
        CHALLENGER("CHALLENGER");  // 王者

        private final String value;

        RankTier(String value) {
            this.value = value;
        }
    }

    /**
     * 游戏类型枚举
     */
    @Getter
    public enum GameType {
        MATCHED("MATCHED_GAME"); // 匹配

        private final String value;

        GameType(String value) {
            this.value = value;
        }
    }

    /**
     * 地图ID枚举
     */
    @Getter
    public enum MapID {
        CLASSIC(11), // 经典模式召唤师峡谷
        ARAM(12);    // 极地大乱斗

        private final int id;

        MapID(int id) {
            this.id = id;
        }
    }

    /**
     * 队伍ID枚举
     */
    @Getter
    public enum TeamID {
        NONE(0),  // 未知
        BLUE(100),// 蓝色方
        RED(200); // 红色方

        private final int id;

        TeamID(int id) {
            this.id = id;
        }
    }

    /**
     * 队伍ID字符串枚举
     */
    @Getter
    public enum TeamIDStr {
        NONE(""),   // 未知
        BLUE("100"),// 蓝色方
        RED("200"); // 红色方

        private final String id;

        TeamIDStr(String id) {
            this.id = id;
        }
    }

    /**
     * 召唤师技能枚举
     */
    @Getter
    public enum Spell {
        PINGZHANG(21),// 屏障
        SHANXIAN(4);  // 闪现

        private final int value;

        Spell(int value) {
            this.value = value;
        }
    }

    /**
     * 位置枚举
     */
    @Getter
    public enum Lane {
        TOP("TOP"),    // 上路
        JUNGLE("JUNGLE"),// 打野
        MIDDLE("MIDDLE"),// 中路
        BOTTOM("BOTTOM");// 下路

        private final String value;

        Lane(String value) {
            this.value = value;
        }
    }

    /**
     * 英雄角色枚举
     */
    @Getter
    public enum ChampionRole {
        SOLO("SOLE"),        // 单人路
        SUPPORT("DUO_SUPPORT"),// 辅助
        ADC("DUO_CARRY"),    // adc
        NONE("NONE");        // 无 一般是打野

        private final String value;

        ChampionRole(String value) {
            this.value = value;
        }
    }

    /**
     * 游戏队列ID枚举
     */
    @Getter
    public enum GameQueueID {
        NormalQueueID("430"),    // 匹配
        FastNormalQueueID("480"),    // 快速匹配
        RankSoleQueueID("420"),  // 单排
        RankFlexQueueID("440"),  // 组排
        ARAMQueueID("450"),      // 大乱斗
        URFQueueID("900"),       // 无限火力
        BOTSimpleQueueID("830"), // 人机入门
        BOTNoviceQueueID("840"), // 人机新手
        BOTNormalQueueID("850"); // 人机一般

        GameQueueID(String value) {
            this.value = value;
        }

        private final String value;
    }

    /**
     * 大区ID
     */
    public static final String PlatformIDDX1 = "HN1"; // 艾欧尼亚
    public static final String PlatformIDDX2 = "HN2"; // 祖安
}
