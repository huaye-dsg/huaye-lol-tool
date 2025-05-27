package com.example.huayeloltool.model.game;

import com.example.huayeloltool.common.CommonResp;
import com.example.huayeloltool.enums.GameEnums;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * 获取所有人当前的游戏会话，一般用于获取敌方信息和战绩
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GameFlowSession extends CommonResp {
    private GameData gameData;
    //private GameDodge gameDodge;
    //private Map map;
    private GameEnums.GameFlow phase;

    // 嵌套的 GameData 类
    @Data
    public static class GameData {
        private boolean spectatorsAllowed;
        private List<GameFlowSessionTeamUser> teamOne;
        private List<GameFlowSessionTeamUser> teamTwo;
    }

    // 嵌套的 GameDodge 类
    //@Data
    //public static class GameDodge {
    //    private List<Object> dodgeIds;
    //    private String phase;
    //    private String state;
    //}

    // 嵌套的 Map 类
    //@Data
    //public static class Map {
        //private String gameMode;
        //private String gameModeName;
        //private String gameModeShortName;
        //private String gameMutator;
        //private int id;
        //private boolean isRGM;
        //private String mapStringId;
        //private String name;
        //private String platformName;
        //private String platformId;
        //private String description;

//        private Properties properties;
//        private PerPositionDisallowedSummonerSpells perPositionDisallowedSummonerSpells;
//        private CategorizedContentBundles categorizedContentBundles;
//        private Assets assets;

//        @Data
//        public static class Assets {
//            private String champSelectBackgroundSound;
//            private String champSelectFlyoutBackground;
//            private String champSelectPlanningIntro;
//            private String gameSelectIconActive;
//            private String gameSelectIconActiveVideo;
//            private String gameSelectIconDefault;
//            private String gameSelectIconDisabled;
//            private String gameSelectIconHover;
//            private String gameSelectIconIntroVideo;
//            private String gameflowBackground;
//            private String gameselectButtonHoverSound;
//            private String iconDefeat;
//            private String iconDefeatVideo;
//            private String iconEmpty;
//            private String iconHover;
//            private String iconLeaver;
//            private String iconVictory;
//            private String iconVictoryVideo;
//            private String mapNorth;
//            private String mapSouth;
//            private String musicInqueueLoopSound;
//            private String partiesBackground;
//            private String postgameAmbienceLoopSound;
//            private String readyCheckBackground;
//            private String readyCheckBackgroundSound;
//            private String sfxAmbiencePregameLoopSound;
//            private String socialIconLeaver;
//            private String socialIconVictory;
//
//            // Getter, Setter 和 构造方法
//        }

        // 嵌套的 CategorizedContentBundles 类
//        @Data
//        public static class CategorizedContentBundles {
//            // 类定义
//        }
//
//        @Data
//        public static class PerPositionDisallowedSummonerSpells {
//            // 类定义
//        }
//
//        // 嵌套的 Properties 类
//        @Data
//        public static class Properties {
//            private boolean suppressRunesMasteriesPerks;
//        }

    //}

    @Data
    public static class GameFlowSessionTeamUser {

        //private double accountId; // 账户ID
        //private double adjustmentFlags; // 调整标志
        //private String botDifficulty; // AI难度
        //private boolean clientInSynch; // 客户端同步状态
        //private GameCustomization gameCustomization; // 游戏自定义设置
        //private double index; // 索引
        //private double lastSelectedSkinIndex; // 最后选择皮肤索引
        //private Object locale; // 地区
        //private boolean minor; // 是否未成年
        //private double originalAccountNumber; // 原始账户号码
        //private String originalPlatformId; // 原始平台ID
        //private String partnerId; // 合作伙伴ID
        //private double pickMode; // 拾取模式
        //private double pickTurn; // 拾取回合
        //private double profileIconId; // 头像图标ID
        private String puuid; // PUUID
        //private double queueRating; // 队列评分
        //private boolean rankedTeamGuest; // 排位团队客人

        // 分路位置。 中路：MIDDLE 上路：TOP 打野：JUNGLE  下路：BOTTOM 辅助：UTILITY
        //private String selectedPosition;
        //private String selectedRole;
        private Long summonerId;
        //private String summonerInternalName;
        //private String summonerName;
        //private boolean teamOwner;
        //private Object teamParticipantId;
        //private double teamRating;
        //private Object timeAddedToQueue;
        //private double timeChampionSelectStart;
        //private double timeGameCreated;
        //private double timeMatchmakingStart;
        //private double voterRating;
        //private double botSkillLevel;
        //private Object championId;
        //private Object role;
        //private Object spell1Id;
        //private Object spell2Id;
        //private String teamId;

        // 嵌套的 GameCustomization 类
        //public static class GameCustomization {
        //    private String regalia;
        //    private String perks;
        //    private String summonerEmotes;
        //
        //    // Getter, Setter 和 构造方法
        //}

        // Getter, Setter 和 构造方法
    }

}
