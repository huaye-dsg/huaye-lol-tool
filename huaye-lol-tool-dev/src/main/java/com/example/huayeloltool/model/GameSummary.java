package com.example.huayeloltool.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GameSummary extends CommonResp {

    /**
     * 游戏创建时间戳
     */
    private long gameCreation;

    /**
     * 游戏创建日期
     */
    private LocalDateTime gameCreationDate;

    /**
     * 游戏持续时间（秒）
     */
    private int gameDuration;

    /**
     * 游戏ID
     */
    private long gameId;

    /**
     * 游戏模式
     */
    private String gameMode;

    /**
     * 游戏类型
     */
    private String gameType;

    /**
     * 游戏版本
     */
    private String gameVersion;

    /**
     * 地图ID
     */
    private String mapId;

    /**
     * 参与者身份列表
     */
    private List<ParticipantIdentity> participantIdentities;

    /**
     * 参与者列表
     */
    private List<Participant> participants;

    /**
     * 平台ID
     */
    private String platformId;

    /**
     * 游戏队列ID
     */
    private int queueId;

    /**
     * 赛季ID
     */
    private int seasonId;

    /**
     * 队伍信息
     */
    private List<Team> teams;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantIdentity {
        /**
         * 参与者ID
         */
        private int participantId;

        /**
         * 玩家信息
         */
        private Player player;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Player {
        /**
         * 账号ID
         */
        private long accountId;

        /**
         * 当前账号ID
         */
        private long currentAccountId;

        /**
         * 当前平台ID
         */
        private String currentPlatformId;

        /**
         * 匹配历史URI
         */
        private String matchHistoryUri;

        /**
         * 平台ID
         */
        private String platformId;

        /**
         * 头像ID
         */
        private int profileIcon;

        /**
         * 召唤师ID
         */
        private long summonerId;

        /**
         * 召唤师名称
         */
        private String summonerName;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Team {
        /**
         * 禁用的英雄列表
         */
        private List<Ban> bans;

        /**
         * 男爵击杀数
         */
        private int baronKills;

        /**
         * Dominion胜利分数
         */
        private int dominionVictoryScore;

        /**
         * 龙的击杀数
         */
        private int dragonKills;

        /**
         * 是否首个击杀男爵
         */
        private boolean firstBaron;

        /**
         * 是否首个击杀
         */
        private boolean firstBlood;

        /**
         * 是否首个击杀龙
         */
        private boolean firstDargon;

        /**
         * 是否首个摧毁抑制器
         */
        private boolean firstInhibitor;

        /**
         * 是否首个摧毁塔
         */
        private boolean firstTower;

        /**
         * 抑制器击杀数
         */
        private int inhibitorKills;

        /**
         * 峡谷先锋击杀数
         */
        private int riftHeraldKills;

        /**
         * 队伍ID
         */
        private int teamId;

        /**
         * 摧毁塔的数量
         */
        private int towerKills;

        /**
         * 蛛后击杀数
         */
        private int vilemawKills;

        /**
         * 胜利标记
         */
        private String win;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ban {
        /**
         * 英雄ID
         */
        private int championId;

        /**
         * 选择轮次
         */
        private int pickTurn;
    }

}
