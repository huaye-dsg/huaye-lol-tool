package com.example.huayeloltool.model;

import lombok.Data;


@Data
public class GameFlowSessionTeamUser {


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
