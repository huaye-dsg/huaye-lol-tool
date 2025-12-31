package com.example.huayeloltool.service;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.conversation.ConversationMsg;
import com.example.huayeloltool.model.summoner.Summoner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 队伍分析服务
 * 
 * <p>负责获取和分析队友信息，包括：
 * - 获取队友召唤师ID列表
 * - 查询召唤师详细信息
 * - 队伍人数验证
 */
@Slf4j
@Service
public class TeamAnalyzer {

    @Autowired
    private LcuApiService lcuApiService;

    /**
     * 获取队友召唤师ID列表（带重试机制）
     * 
     * <p>由于LOL客户端API的特性，有时需要多次尝试才能获取到完整的队友列表
     * 
     * @param maxRetries 最大重试次数
     * @return 召唤师ID列表
     */
    public List<Long> getTeamSummonerIds(int maxRetries) {
        return fetchTeamSummonerIdsWithRetry(maxRetries);
    }

    /**
     * 根据召唤师ID列表获取召唤师详细信息
     * 
     * @param summonerIdList 召唤师ID列表
     * @return 召唤师信息列表
     */
    public List<Summoner> getSummonerDetails(List<Long> summonerIdList) {
        if (CollectionUtils.isEmpty(summonerIdList)) {
            log.warn("召唤师ID列表为空");
            return Collections.emptyList();
        }

        try {
            List<Summoner> summonerList = lcuApiService.listSummoner(summonerIdList);
            if (CollectionUtils.isEmpty(summonerList)) {
                log.warn("查询召唤师信息失败，返回空列表");
                return Collections.emptyList();
            }
            
            log.info("成功获取 {} 个召唤师信息", summonerList.size());
            return summonerList;
        } catch (Exception e) {
            log.error("查询召唤师信息时发生异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 验证队伍人数是否符合预期
     * 
     * @param summonerIdList 召唤师ID列表
     * @param isSoloRank 是否为单排模式
     */
    public void validateTeamSize(List<Long> summonerIdList, boolean isSoloRank) {
        if (CollectionUtils.isEmpty(summonerIdList)) {
            log.error("队友召唤师ID查询失败！");
            return;
        }

        if (isSoloRank && summonerIdList.size() < Constant.STANDARD_TEAM_SIZE) {
            log.warn("队伍人数不为{}，实际人数：{}", Constant.STANDARD_TEAM_SIZE, summonerIdList.size());
        }
    }

    /**
     * 递归重试获取队友召唤师ID
     * 
     * <p>实现说明：
     * - 如果获取成功且数量为5，直接返回
     * - 如果已经尝试了指定次数，返回当前结果
     * - 否则延迟后递归重试
     * 
     * @param attemptCount 当前尝试次数
     * @return 召唤师ID列表
     */
    private List<Long> fetchTeamSummonerIdsWithRetry(int attemptCount) {
        List<Long> summonerIdList = getTeamSummonerIdList();

        // 如果获取成功且数量为5，直接返回
        if (CollectionUtils.isNotEmpty(summonerIdList) && summonerIdList.size() == Constant.STANDARD_TEAM_SIZE) {
            return summonerIdList;
        }

        // 如果已经尝试了指定次数，返回当前结果
        if (attemptCount >= 2) {
            return summonerIdList;
        }

        // 延迟后递归重试
        try {
            Thread.sleep(200);
            return fetchTeamSummonerIdsWithRetry(attemptCount + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取召唤师ID列表时被中断");
            return summonerIdList;
        } catch (Exception e) {
            log.error("获取召唤师ID列表时发生异常", e);
            return summonerIdList;
        }
    }

    /**
     * 从会话消息列表中获取召唤师ID列表
     * 
     * <p>通过解析LOL客户端的会话消息，提取加入房间的召唤师ID
     * 
     * @return 召唤师ID列表
     */
    private List<Long> getTeamSummonerIdList() {
        String conversationID = lcuApiService.getCurrConversationID();
        if (StringUtils.isBlank(conversationID)) {
            log.debug("当前不在英雄选择阶段");
            return Collections.emptyList();
        }

        List<ConversationMsg> msgList = lcuApiService.listConversationMsg(conversationID);
        if (msgList == null || msgList.isEmpty()) {
            log.debug("获取会话组消息记录失败");
            return Collections.emptyList();
        }

        List<Long> summonerIDList = new ArrayList<>(Constant.STANDARD_TEAM_SIZE);
        for (ConversationMsg msg : msgList) {
            if (Constant.CONVERSATION_MSG_TYPE_SYSTEM.equals(msg.getType()) &&
                    Constant.JOINED_ROOM_MSG.equals(msg.getBody()) &&
                    msg.getFromSummonerId() > 0) {
                summonerIDList.add(msg.getFromSummonerId());
            }
        }
        return summonerIDList;
    }
}