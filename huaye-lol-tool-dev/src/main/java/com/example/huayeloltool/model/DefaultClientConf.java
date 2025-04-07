package com.example.huayeloltool.model;

import lombok.Data;

@Data
public class DefaultClientConf {
    /**
     * 是否自动接受游戏
     */
    private boolean autoAcceptGame;

    /**
     * 自动选择的英雄ID
     */
    private int autoPickChampID;

    /**
     * 自动禁用的英雄ID
     */
    private int autoBanChampID;

    /**
     * 是否自动发送队伍马匹信息
     */
    private boolean autoSendTeamHorse;

    /**
     * 是否发送自己的马匹信息
     */
    private boolean shouldSendSelfHorse;

    /**
     * 马匹名称配置
     */
    private String[] horseNameConf = {"通天代", "小代", "上等马", "中等马", "下等马", "牛马"};

    /**
     * 是否选择发送马匹消息
     */
    private boolean[] chooseSendHorseMsg = {true, true, true, true, true, true};

    /**
     * 选择英雄发送消息的延迟时间（秒）
     */
    private int chooseChampSendMsgDelaySec;

    /**
     * 是否保存游戏内消息到剪贴板
     */
    private boolean shouldInGameSaveMsgToClipBoard;

    /**
     * 是否自动打开浏览器
     */
    private Boolean shouldAutoOpenBrowser;

    // 可根据需要定义默认配置的构造器或方法

    public DefaultClientConf(Boolean shouldAutoOpenBrowserCfg) {
        this.autoAcceptGame = false;
        this.autoPickChampID = 0;
        this.autoBanChampID = 0;
        this.autoSendTeamHorse = true;
        this.shouldSendSelfHorse = true;
        this.chooseChampSendMsgDelaySec = 3;
        this.shouldInGameSaveMsgToClipBoard = true;
        this.shouldAutoOpenBrowser = shouldAutoOpenBrowserCfg;
    }
}
