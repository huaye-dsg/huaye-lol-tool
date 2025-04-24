package com.example.huayeloltool.model.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 玩家历史游戏列表
 */
@Data
public class GameHistory {
    /**
     * 玩家的账户ID。
     */
    private long accountId;

    /**
     * 包含游戏相关信息的对象。
     */
    private Games games;

    /**
     * 游戏平台ID。
     */
    private String platformId;

    /**
     * 表示一组游戏信息的数据类。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Games {

        /**
         * 游戏开始日期。
         */
        private String gameBeginDate;

        /**
         * 游戏数量。
         */
        private int gameCount;

        /**
         * 游戏结束日期。
         */
        private String gameEndDate;

        /**
         * 游戏索引起始位置。
         */
        private int gameIndexBegin;

        /**
         * 游戏索引结束位置。
         */
        private int gameIndexEnd;

        /**
         * 游戏列表。
         */
        private List<GameInfo> games;
    }

    @Data
    public static class GameInfo {
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

    }

}