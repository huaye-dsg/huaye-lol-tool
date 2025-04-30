package com.example.huayeloltool.model.base;

import lombok.Data;

@Data
public class GameGlobalSetting {

    /**
     * 是否自动接受游戏
     */
    private Boolean autoAcceptGame = true;

    /**
     * 自动选择的英雄ID
     */
    private Integer autoPickChampID = 0;

    /**
     * 自动禁用的英雄ID
     */
    private Integer autoBanChampID = 104;


    /**
     * 马匹名称配置
     */
    private String[] horseNameConf = {"通天代", "小代", "上等马", "中等马", "下等马", "牛 马"};

    private static GameGlobalSetting instance;

    public static GameGlobalSetting getInstance() {
        if (instance == null) {
            instance = new GameGlobalSetting();
        }
        return instance;
    }

}
