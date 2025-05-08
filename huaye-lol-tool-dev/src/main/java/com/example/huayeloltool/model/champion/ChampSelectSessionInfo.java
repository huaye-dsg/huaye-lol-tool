package com.example.huayeloltool.model.champion;

import com.example.huayeloltool.enums.CommonResp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@SuppressWarnings({"DanglingJavadoc", "CommentedOutCode"})
@EqualsAndHashCode(callSuper = true)
@Data
public class ChampSelectSessionInfo extends CommonResp {

    /**
     * 分组动作数据，二维数组：
     * 每个子数组对应一轮动作，例如：同时进行的选人或禁用操作。
     */
    private List<List<Action>> actions;

    /** 是否允许战斗助推 */
//    private boolean allowBattleBoost;
    /** 是否允许重复选择英雄 */
//    private boolean allowDuplicatePicks;
    /** 是否允许锁定事件 */
//    private boolean allowLockedEvents;
    /** 是否允许重抽英雄 */
//    private boolean allowRerolling;
    /** 是否允许皮肤选择 */
//    private boolean allowSkinSelection;

    /**
     * 禁用英雄相关数据
     */
    private Bans bans;

    /**
     * 替补英雄列表
     * （一般为空数组）
     */
//    private List<Object> benchChampions;

    /** 是否启用替补 */
//    private boolean benchEnabled;
    /** 可加速选择皮肤的数量 */
//    private int boostableSkinCount;

    /**
     * 聊天详细信息，包括频道、JWT 等数据
     */
//    private ChatDetails chatDetails;

    /** 计数器字段，含义由服务端业务定义 */
//    private int counter;

    /** 当前游戏ID（一般未开始时为0） */
//    private Long gameId;

    /** 是否同时允许禁用英雄 */
//    private boolean hasSimultaneousBans;
    /** 是否同时允许选人 */
//    private boolean hasSimultaneousPicks;
    /**
     * 是否为自定义房间
     */
    private Boolean isCustomGame;
    /**
     * 是否为 Legacy 的选人界面
     */
    private Boolean isLegacyChampSelect;
    /** 是否为观战模式 */
//    private boolean isSpectating;

    /**
     * 本地玩家在己方队伍中的 cellId
     */
    private Integer localPlayerCellId;
    /** 锁定事件的索引，未锁定时通常为 -1 */
//    private int lockedEventIndex;

    /**
     * 我方队伍数据列表
     */
    private List<Player> myTeam;
    /** pickOrder 的交换信息列表（具体格式依据业务） */
//    private List<Object> pickOrderSwaps;
    /** 位置交换信息列表（具体格式依据业务） */
//    private List<Object> positionSwaps;

    /** 剩余重抽次数 */
//    private int rerollsRemaining;
    /** 是否显示退出按钮 */
//    private boolean showQuitButton;
    /** 是否跳过选人阶段 */
//    private boolean skipChampionSelect;

    /**
     * 敌方队伍数据列表
     */
    private List<Player> theirTeam;

    /**
     * 计时器对象，用于记录每个阶段的时间信息
     //     */
//    private Timer timer;
//
//    /** 交易信息列表（一般用于换位或其他交换操作） */
//    private List<Trade> trades;

    // ==== 内部子类 ====

    /**
     * 表示每个选人或禁用动作的数据
     */
    @Data
    @NoArgsConstructor
    public static class Action {
        /**
         * 操作者的 cellId，标识该动作由哪位玩家触发
         */
        private Integer actorCellId;
        /**
         * 选/禁用的英雄ID，当为0时表示未锁定
         */
        private Integer championId;
        /**
         * 是否完成此动作
         */
        private Boolean completed;
        /**
         * 动作的唯一标识符
         */
        private Integer id;
        /**
         * 是否为我方动作：true 为我方，false 为敌方
         */
        private Boolean isAllyAction;
        /**
         * 当前是否正在进行该动作
         */
        private Boolean isInProgress;
        /**
         * 此轮选人中的排序编号
         */
        private String pickTurn;
        /**
         * 动作类型：如 "pick"（选人）、"ban"（禁用）
         */
        private String type;
    }

    /**
     * 禁用英雄相关数据
     */
    @SuppressWarnings("DanglingJavadoc")
    @Data
    @NoArgsConstructor
    public static class Bans {
        /**
         * 我方已禁用的英雄列表（根据业务，可定义为英雄ID或者详细对象）
         */
        private List<Integer> myTeamBans;
        /**
         * 总禁用次数
         */
//        private Integer numBans;
        /**
         * 敌方已禁用的英雄列表
         */
        private List<Integer> theirTeamBans;
    }

    /**
     * 表示聊天详细数据
     */
//    @Data
//    @NoArgsConstructor
//    public static class ChatDetails {
//        /**
//         * 多人聊天 JWT 数据对象
//         */
//        private MucJwtDto mucJwtDto;
//        /** 多人聊天的唯一标识 */
//        private String multiUserChatId;
//        /** 多人聊天的密码或令牌 */
//        private String multiUserChatPassword;
//
//        /**
//         * 内部类：多人聊天 JWT 数据
//         */
//        @Data
//        @NoArgsConstructor
//        public static class MucJwtDto {
//            /** 聊天频道声明 */
//            private String channelClaim;
//            /** 域名 */
//            private String domain;
//            /** JWT 令牌 */
//            private String jwt;
//            /** 目标区域（例如 "tj101"） */
//            private String targetRegion;
//        }
//    }

    /**
     * 表示一个队伍成员（玩家）的数据
     */
    @SuppressWarnings("CommentedOutCode")
    @Data
    @NoArgsConstructor
    public static class Player {
        /**
         * 指定位置，例如 TOP、JUNGLE、MIDDLE、BOTTOM、UTILITY，可能为空字符串
         */
        private String assignedPosition;
        /**
         * 玩家在队伍中的 cellId
         */
        private Integer cellId;
        /**
         * 锁定的英雄ID，未选人时通常为0
         */
        private Integer championId;
//        /** 选人意图（业务字段，具体解释依据业务） */
//        private Integer championPickIntent;
//        /** 名称可见性类型，具体业务自定义 */
//        private String nameVisibilityType;
//        /** 混淆过的 PUUID（可能为空） */
//        private String obfuscatedPuuid;
//        /** 混淆过的召唤师ID（可能为空或0） */
//        private long obfuscatedSummonerId;
//        /** 玩家别名 */
//        private String playerAlias;
        /**
         * 玩家 PUUID
         */
        private String puuid;
//        /** 选定的皮肤ID */
//        private int selectedSkinId;
//        /**
//         * 第一个召唤师技能ID
//         */
//        private String spell1Id;
//        /**
//         * 第二个召唤师技能ID
//         */
//        private String spell2Id;
        /**
         * 召唤师的唯一ID
         */
        private Long summonerId;
        /**
         * 所属队伍，1：我方；2：敌方
         */
        private Integer team;
    }

    /**
     * 表示计时器数据，用于记录选人阶段剩余时间等信息
     */
//    @Data
//    @NoArgsConstructor
//    public static class Timer {
//        /** 当前阶段剩余经过调整后的时间 */
//        private int adjustedTimeLeftInPhase;
//        /** 当前时间戳（Epoch毫秒） */
//        private long internalNowInEpochMs;
//        /** 是否为无限计时 */
//        private boolean isInfinite;
//        /** 当前阶段名称 */
//        private String phase;
//        /** 当前阶段总时长 */
//        private int totalTimeInPhase;
//    }

    /**
     * 表示交换操作的数据，例如换位交易
     */
//    @Data
//    @NoArgsConstructor
//    public static class Trade {
//        /** 进行交换操作的玩家 cellId */
//        private int cellId;
//        /** 交易的唯一标识 */
//        private int id;
//        /** 交易状态，例如 "INVALID" */
//        private String state;
//    }
}