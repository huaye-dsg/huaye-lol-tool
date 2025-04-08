package com.example.huayeloltool.model;

import lombok.Data;


@Data
public class GameFolwSessionTeamUser {


    private double accountId;
    private double adjustmentFlags;
    private String botDifficulty;
    private boolean clientInSynch;
    private GameCustomization gameCustomization;
    private double index;
    private double lastSelectedSkinIndex;
    private Object locale;
    private boolean minor;
    private double originalAccountNumber;
    private String originalPlatformId;
    private String partnerId;
    private double pickMode;
    private double pickTurn;
    private double profileIconId;
    private String puuid;
    private double queueRating;
    private boolean rankedTeamGuest;

    // 分路位置。 中路：MIDDLE 上路：TOP 打野：JUNGLE  下路：BOTTOM 辅助：UTILITY
    private String selectedPosition;
    private String selectedRole;
    private Long summonerId;
    private String summonerInternalName;
    private String summonerName;
    private boolean teamOwner;
    private Object teamParticipantId;
    private double teamRating;
    private Object timeAddedToQueue;
    private double timeChampionSelectStart;
    private double timeGameCreated;
    private double timeMatchmakingStart;
    private double voterRating;
    private double botSkillLevel;
    private Object championId;
    private Object role;
    private Object spell1Id;
    private Object spell2Id;
    private String teamId;

    // 嵌套的 GameCustomization 类
    public static class GameCustomization {
        private String regalia;
        private String perks;
        private String summonerEmotes;

        // Getter, Setter 和 构造方法
    }

    // Getter, Setter 和 构造方法
}
