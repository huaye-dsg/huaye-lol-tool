package com.example.huayeloltool.model.game;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class GameTimeLine {
    /** 时间线所有帧列表，一般每帧代表一分钟状态和事件 */
    private List<Frame> frames;

    /** 每帧的时间间隔（毫秒）通常为60000（即一分钟） */
    private long frameInterval;

    /** 游戏唯一ID */
    private long gameId;

    /** 所有参赛选手概要信息列表 */
    private List<Participant> participants;

    // --------------------- 内部类定义 ---------------------

    /**
     * 时间线中的单帧数据，包含当前时间点所有玩家状态及当分钟的所有事件
     */
    @Data
    public static class Frame {
        /** 时间戳（毫秒），对应本帧发生时点 */
        private long timestamp;

        /** 本帧发生的全部事件（比如击杀、购买、插眼等） */
        private List<Event> events;

        /** 该帧所有玩家的快照状态，key通常为"1"-"10" */
        private Map<String, ParticipantFrame> participantFrames;

        // getters & setters
    }

    /**
     * 游戏中的单个事件，如击杀、插眼、升级、购买装备、建筑被摧毁等
     */
    @Data
    public static class Event {
        /** 事件类型，如CHAMPION_KILL、WARD_PLACED、SKILL_LEVEL_UP等 */
        private String type;
        /** 事件时间戳（毫秒） */
        private long timestamp;

        /** 参与该事件的玩家ID（并非所有事件都有） */
        private Integer participantId;

        /** 击杀者ID（仅击杀类事件有） */
        private Integer killerId;

        /** 死亡者ID（仅击杀类事件有） */
        private Integer victimId;

        /** 助攻者ID列表（仅击杀类事件有） */
        private List<Integer> assistingParticipantIds;

        /** 守卫类型（插眼等事件有） */
        private Integer wardType;

        /** 技能插槽（技能升级类事件有） */
        private Integer skillSlot;

        /** 升级类型（技能升级事件有，通常为NORMAL/EVOLVE） */
        private Integer levelUpType;

        /** 物品ID（购买/销毁事件有） */
        private Integer itemId;

        /** 建筑种类（如塔、基地等，摧毁建筑事件有） */
        private String buildingType;

        /** 野怪类型（击杀野怪事件有，如DRAGON、BARON_NASHOR） */
        private String monsterType;

        /** 野怪子类型（如ELEMENTAL_DRAGON，击杀小龙事件有） */
        private String monsterSubType;

        /** 战队ID（如蓝方100，红方200） */
        private Integer teamId;

        /** 创建者ID（插眼等事件） */
        private Integer creatorId;

        // 此处仅列常见字段，其他事件类型可参考返回实际数据扩展
        // getters & setters
    }

    /**
     * 单一玩家在某帧的详细状态快照
     */
    @Data
    public static class ParticipantFrame {
        /** 玩家ID */
        private int participantId;

        /** 坐标位置（单位：像素） */
        private Position position;

        /** 当前金币 */
        private int currentGold;

        /** 当前累积获得总金币 */
        private int totalGold;

        /** 角色等级 */
        private int level;

        /** 当前经验值 */
        private int xp;

        /** 补刀数 */
        private int minionsKilled;

        /** 打野补刀数 */
        private int jungleMinionsKilled;

        /** Dominion模式分数，普通模式可忽略 */
        private int dominionScore;

        /** 队伍得分 */
        private int teamScore;

        /** 个人得分（某些模式用） */
        private int participantScore;

        /** 助攻数 */
        private int assists;

        /** 死亡数 */
        private int deaths;

        /** 击杀数 */
        private int kills;

        /** 当前生命值 */
        private int health;

        /** 当前最大生命值 */
        private int maxHealth;

        /** 当前法力值 */
        private int mana;

        /** 当前最大法力值 */
        private int maxMana;

    }

    /**
     * 用于描述地图上的具体位置（像素坐标）
     */
    @Data
    public static class Position {
        /** 地图x坐标 */
        private int x;

        /** 地图y坐标 */
        private int y;
    }

    /**
     * 游戏参与者概要信息
     */
    @Data
    public static class Participant {
        /** 参与者ID（1~10，一局游戏中唯一） */
        private int participantId;

        /** 所使用英雄ID */
        private int championId;

        /** 召唤师名称 */
        private String summonerName;

        /** 所属阵营队伍ID（100：蓝方，200：红方） */
        private int teamId;
    }
}
