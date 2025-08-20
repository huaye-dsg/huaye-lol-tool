package com.example.huayeloltool.model.summoner;

import lombok.Data;

/**
 * 召唤师信息
 */
@Data
public class Summoner {
    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 是否公开个人信息
     * "PRIVATE" | "PUBLIC"
     */
    private String privacy;

    /**
     * 显示名称
     */
    //private String displayName;

    /**
     * 内部名称
     */
    //private String internalName;

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
    //private Integer profileIconId;

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

    // 使用volatile关键字确保多线程环境下的可见性
    private static volatile Summoner instance;

    /**
     * 设置单例实例（线程安全）
     * 只有在实例为null时才允许设置，避免破坏单例模式
     */
    public static Summoner setInstance(Summoner summoner) {
        if (instance == null) {
            synchronized (Summoner.class) {
                if (instance == null) {
                    instance = summoner;
                }
            }
        }
        return instance;
    }

    /**
     * 获取单例实例（双重检查锁定模式）
     */
    public static Summoner getInstance() {
        if (instance == null) {
            synchronized (Summoner.class) {
                if (instance == null) {
                    instance = new Summoner();
                }
            }
        }
        return instance;
    }

    /**
     * 清除单例实例（用于测试或重置）
     */
    public static synchronized void clearInstance() {
        instance = null;
    }

    @Data
    public static class RerollPoints {
        private int currentPoints;
        private int maxRolls;
        private int numberOfRolls;
        private int pointsCostToRoll;
        private int pointsToReroll;
    }
}