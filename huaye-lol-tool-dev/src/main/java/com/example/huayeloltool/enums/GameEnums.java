package com.example.huayeloltool.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏相关的枚举类型定义
 */
public class GameEnums {
    /**
     * 游戏模式枚举
     */
    @Getter
    @AllArgsConstructor
    public enum GameMode {
        NONE(""),          // 游戏模式：无
        CLASSIC("CLASSIC"),// 经典模式
        ARAM("ARAM"),      // 大乱斗
        TFT("TFT"),        // 云顶之弈
        URF("URF"),        // 无限火力
        CUSTOM("PRACTICETOOL"); // 自定义

        private final String value;
    }

    /**
     * 游戏队列类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum GameQueueType {
        NORMAL("NORMAL"),            // 匹配
        RANK_SOLO("RANKED_SOLO_5x5"),// 单双排
        RANK_FLEX("RANKED_FLEX_SR"), // 组排
        ARAM("ARAM_UNRANKED_5x5"),   // 大乱斗5v5
        URF("URF"),                  // 无限火力
        BOT("BOT"),                  // 人机
        CUSTOM("PRACTICETOOL");      // 自定义

        private final String value;
    }

    /**
     * 游戏状态枚举
     */
    @Getter
    @AllArgsConstructor
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
    }

    /**
     * 游戏流程枚举
     */
    @Getter
    @AllArgsConstructor
    public enum GameFlow {
        CHAMPION_SELECT("ChampSelect"), // 英雄选择中
        READY_CHECK("ReadyCheck"),      // 等待接受对局
        IN_PROGRESS("InProgress"),      // 进行中
        MATCHMAKING("Matchmaking"),     // 匹配中
        NONE("None");                   // 无

        private final String value;

        public static GameFlow getByValue(String status) {

            GameFlow[] values = GameFlow.values();
            for (GameFlow gameFlow : values) {
                if (Objects.equals(gameFlow.getValue(), status)) {
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
    @AllArgsConstructor
    public enum RankTier {
        IRON("IRON", "黑铁"),        // 黑铁
        BRONZE("BRONZE", "青铜"),    // 青铜
        SILVER("SILVER", "白银"),    // 白银
        GOLD("GOLD", "黄金"),        // 黄金
        PLATINUM("PLATINUM", "白金"),// 白金
        DIAMOND("DIAMOND", "钻石"),  // 钻石
        MASTER("MASTER", "大师"),    // 大师
        GRANDMASTER("GRANDMASTER", "宗师"),// 宗师
        CHALLENGER("CHALLENGER", "王者");  // 王者

        public static Map<String, String> rankMap = new HashMap<>();

        public static String getRankNameMap(String value) {
            return rankMap.get(value);
        }

        static {
            for (RankTier option : RankTier.values()) {
                rankMap.put(option.getValue(), option.getDesc());
            }
        }

        private final String value;
        private final String desc;
    }

    /**
     * 游戏类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum GameType {
        MATCHED("MATCHED_GAME"); // 匹配
        private final String value;
    }

    /**
     * 地图ID枚举
     */
    @Getter
    @AllArgsConstructor
    public enum MapID {
        CLASSIC(11), // 经典模式召唤师峡谷
        ARAM(12);    // 极地大乱斗
        private final int id;
    }

    /**
     * 队伍ID枚举
     */
    @Getter
    @AllArgsConstructor
    public enum TeamID {
        NONE(0),  // 未知
        BLUE(100),// 蓝色方
        RED(200); // 红色方

        private final int id;
    }

    /**
     * 队伍ID字符串枚举
     */
    @Getter
    @AllArgsConstructor
    public enum TeamIDStr {
        NONE(""),   // 未知
        BLUE("100"),// 蓝色方
        RED("200"); // 红色方

        private final String id;
    }

    /**
     * 召唤师技能枚举
     */
    @Getter
    @AllArgsConstructor
    public enum Spell {
        PINGZHANG(21),// 屏障
        SHANXIAN(4);  // 闪现

        private final int value;
    }


    /**
     * 游戏队列ID枚举
     */
    @Getter
    @AllArgsConstructor
    public enum GameQueueID {
        RANK_SOLO(420, "单排排位"),
        RANK_FLEX(440, "灵活排位"),
        NORMAL_BLIND(430, "匹配盲选"),
        NORMAL_DRAFT(400, "匹配选人"),
        FAST_NORMAL(480, "快速匹配"),
        ARAM(450, "大乱斗"),
        URF(900, "无限火力"),
        ARURF(901, "随机无限火力"),
        NEXUS_BLITZ(1200, "极限闪击"),
        ULTIMATE_SPELLBOOK(1900, "至尊魔典"),
        ARENA(1700, "斗魂竞技场"),
        CLASH(700, "战队赛 Clash"),
        TUTORIAL(2000, "新手教程"),
        BOT_INTRO(830, "人机入门"),
        BOT_BEGINNER(840, "人机新手"),
        BOT_INTERMEDIATE(850, "人机一般"),
        CUSTOM(11, "自定义"),
        UNKNOWN(-1, "未知模式");

        private final int id;
        private final String description;

        private final static Map<Integer, String> map = new HashMap<>();

        private final static List<Integer> list = Arrays.asList(RANK_SOLO.id, RANK_FLEX.id, NORMAL_BLIND.id, NORMAL_DRAFT.id, FAST_NORMAL.id, ARAM.id);

        public static Boolean isValidData(Integer value) {
            return list.contains(value);
        }

        public static String getGameNameMap(Integer value) {
            return map.getOrDefault(value,UNKNOWN.getDescription());
        }

        static {
            for (GameQueueID option : GameQueueID.values()) {
                map.put(option.getId(), option.getDescription());
            }
        }
    }


    @Getter
    @AllArgsConstructor
    public enum Position {

        TOP("TOP", "上路"),
        JUNGLE("JUNGLE", "打野"),
        MIDDLE("MIDDLE", "中路"),
        BOTTOM("BOTTOM", "下路"),
        UTILITY("UTILITY", "辅助");

        private final String value;
        private final String desc;

        public static Map<String, String> map = Arrays.stream(Position.values()).collect(Collectors.toMap(Position::getValue, Position::getDesc));

        public static String getDescByValue(String value) {
            return map.getOrDefault(value,"未知");
        }
    }


    /**
     * 大区ID
     */
    public static final String PlatformIDDX1 = "HN1"; // 艾欧尼亚
    public static final String PlatformIDDX2 = "HN2"; // 祖安
}
