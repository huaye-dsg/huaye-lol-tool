package com.example.huayeloltool.service.impl;

import com.example.huayeloltool.model.ChampSelectSessionInfo;
import com.example.huayeloltool.model.CurrSummoner;
import com.example.huayeloltool.model.ProcessInfo;

import java.io.IOException;

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
}
