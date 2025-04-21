package com.example.huayeloltool.model;

import lombok.Data;


@Data
public class CurrSummoner {
    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 内部名称
     */
    private String internalName;

    /**
     * 名称更改标志
     */
    //private Boolean nameChangeFlag;

    /**
     * 下一级的完成百分比
     */
    //private Integer percentCompleteForNextLevel;

    /**
     * 头像ID
     */
    private Integer profileIconId;

    /**
     * 玩家唯一标识符
     */
    private String puuid;

    /**
     * 重置点数信息
     */
    //private RerollPoints rerollPoints;

    /**
     * 召唤师ID
     */
    private Long summonerId;

    /**
     * 游戏名
     */
    private String gameName;

    /**
     * 标签行
     */
    private String tagLine;

    /**
     * 召唤师等级
     */
    //private Integer summonerLevel;

    /**
     * 是否未命名
     */
    //private Boolean unnamed;

    /**
     * 自上一级以来的经验值
     */
    //private Integer xpSinceLastLevel;

    /**
     * 到下一级所需的经验值
     */
    //private Integer xpUntilNextLevel;

    private static CurrSummoner instance;


    public static synchronized CurrSummoner setInstance(CurrSummoner currSummoner) {
        instance = currSummoner;
        return instance;
    }


    public static synchronized CurrSummoner getInstance() {
        if (instance == null) {
            instance = new CurrSummoner();
        }
        return instance;
    }

    //@Data
    //public static class RerollPoints {
    //    private int currentPoints;
    //    private int maxRolls;
    //    private int numberOfRolls;
    //    private int pointsCostToRoll;
    //    private int pointsToReroll;
    //}
}
