package com.example.huayeloltool.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏相关的枚举类型定义
 */
public class GameEnums {

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

        public static final Map<String, String> rankMap = new HashMap<>();

        public static String getRankNameMap(String value) {
            return rankMap.getOrDefault(value, "未知");
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

        private final static List<Integer> NORMAL_GAME_MODE_LIST = Arrays.asList(RANK_SOLO.id, RANK_FLEX.id, NORMAL_BLIND.id, NORMAL_DRAFT.id, ARAM.id);

        public static Boolean isNormalGameMode(Integer value) {
            return NORMAL_GAME_MODE_LIST.contains(value);
        }

        public static String getGameNameMap(Integer value) {
            return map.getOrDefault(value, UNKNOWN.getDescription());
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

        TOP("top", "上路"),
        JUNGLE("jungle", "打野"),
        MIDDLE("middle", "中路"),
        BOTTOM("bottom", "下路"),
        UTILITY("utility", "辅助");

        private final String value;
        private final String desc;

        public static final Map<String, String> map = Arrays.stream(Position.values()).collect(Collectors.toMap(Position::getValue, Position::getDesc));

        public static String getDescByValue(String value) {
            return StringUtils.isNotEmpty(value) ? map.getOrDefault(value.toLowerCase(), "未知") : "未知";
        }
    }

}
