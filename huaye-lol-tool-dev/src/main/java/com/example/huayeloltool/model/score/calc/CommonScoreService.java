package com.example.huayeloltool.model.score.calc;

import com.example.huayeloltool.model.game.GameSummary;


public class CommonScoreService {
    /**
     * 获取用户参与者ID
     */
    public int getUserParticipantId(long summonerID, GameSummary gameSummary) throws Exception {
        return gameSummary.getParticipantIdentities().stream()
                .filter(identity -> identity.getPlayer().getSummonerId() == summonerID)
                .findFirst() // 用于查找到第一个匹配的项
                .map(GameSummary.ParticipantIdentity::getParticipantId) // 提取ID
                .orElseThrow(() -> new Exception("获取用户参与者ID失败"));
    }
}
