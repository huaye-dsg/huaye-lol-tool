package com.example.huayeloltool.model;

import com.example.huayeloltool.enums.GameEnums;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class GameFlowSession extends CommonResp {
    private GameData gameData;
    private GameDodge gameDodge;
    private Map map;
    private GameEnums.GameFlow phase;

    // 嵌套的 GameData 类
    @Data
    public static class GameData {
        private boolean spectatorsAllowed;
        private List<GameFolwSessionTeamUser> teamOne;
        private List<GameFolwSessionTeamUser> teamTwo;
    }

    // 嵌套的 GameDodge 类
    @Data
    public static class GameDodge {
        private List<Object> dodgeIds;
        private String phase;
        private String state;
    }

    // 嵌套的 Map 类
    @Data
    public static class Map {
        private Assets assets;
        private CategorizedContentBundles categorizedContentBundles;
        private String description;
        private String gameMode;
        private String gameModeName;
        private String gameModeShortName;
        private String gameMutator;
        private int id;
        private boolean isRGM;
        private String mapStringId;
        private String name;
        private PerPositionDisallowedSummonerSpells perPositionDisallowedSummonerSpells;
        private String platformId;
        private String platformName;
        private Properties properties;

        @Data
        public static class Assets {
            private String champSelectBackgroundSound;
            private String champSelectFlyoutBackground;
            private String champSelectPlanningIntro;
            private String gameSelectIconActive;
            private String gameSelectIconActiveVideo;
            private String gameSelectIconDefault;
            private String gameSelectIconDisabled;
            private String gameSelectIconHover;
            private String gameSelectIconIntroVideo;
            private String gameflowBackground;
            private String gameselectButtonHoverSound;
            private String iconDefeat;
            private String iconDefeatVideo;
            private String iconEmpty;
            private String iconHover;
            private String iconLeaver;
            private String iconVictory;
            private String iconVictoryVideo;
            private String mapNorth;
            private String mapSouth;
            private String musicInqueueLoopSound;
            private String partiesBackground;
            private String postgameAmbienceLoopSound;
            private String readyCheckBackground;
            private String readyCheckBackgroundSound;
            private String sfxAmbiencePregameLoopSound;
            private String socialIconLeaver;
            private String socialIconVictory;

            // Getter, Setter 和 构造方法
        }

        // 嵌套的 CategorizedContentBundles 类
        @Data
        public static class CategorizedContentBundles {
            // 类定义
        }

        @Data
        public static class PerPositionDisallowedSummonerSpells {
            // 类定义
        }

        // 嵌套的 Properties 类
        @Data
        public static class Properties {
            private boolean suppressRunesMasteriesPerks;
        }

    }

}
