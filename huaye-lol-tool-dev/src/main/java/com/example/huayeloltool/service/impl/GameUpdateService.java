package com.example.huayeloltool.service.impl;

import com.example.huayeloltool.model.*;

import java.io.IOException;
import java.util.List;

public interface GameUpdateService {

    /**
     * 游戏状态变更
     */
    void onGameFlowUpdate(String message);

    /**
     * 游戏选择会话更新，比如ban英雄、选择英雄
     */
    void onChampSelectSessionUpdate(ChampSelectSessionInfo message);

    /**
     * 获取当前召唤师信息
     */
    CurrSummoner getCurrSummoner();

    /**
     * 获取LOL进程并解析端口和token
     */
    ProcessInfo getLolClientApiInfo(String processName);

    /**
     * 查询战绩
     */
    List<GameInfo> listGameHistory(String puuid, int begin, int limit) throws IOException;

    /**
     * 查询段位信息
     */
    RankedInfo getRankData(String puuid);

    /**
     * 查询英雄熟练度信息
     */
    List<ChampionMastery> searchDFDAta(String puuid) throws IOException;


}
