package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * LOL事件路由器
 * 负责解析WebSocket消息并将事件分发到相应的处理器
 * <p>
 * 主要功能：
 * - WebSocket消息解析和验证
 * - 事件路由和分发
 * - 异步事件处理
 * - 消息统计和监控
 */
@Slf4j
@Service
public class MessageRouter {

    @Autowired
    private GameFlowHandler gameFlowHandler;
    @Autowired
    private ChampionSelectHandler championSelectHandler;

    // 支持的事件URI常量
    private static final String GAMEFLOW_PHASE_URI = "/lol-gameflow/v1/gameflow-phase";
    private static final String CHAMP_SELECT_URI = "/lol-champ-select/v1/session";
    private static final String MATCHMAKING_URI = "/lol-lobby-team-builder/v1/matchmaking";
    private static final String EVENT_TYPE = "OnJsonApiEvent";

    /**
     * 处理WebSocket消息的主入口
     *
     * @param message WebSocket接收到的原始消息
     */
    public void routeMessage(String message) {
        try {
            // 基础验证
            if (StringUtils.isEmpty(message)) {
                log.debug("收到空消息，跳过处理");
                return;
            }

            // 解析消息格式
            LolEvent event = parseMessage(message);
            if (event == null) {
                return;
            }

            // 路由事件到对应处理器（统一使用虚拟线程）
            routeEvent(event.uri, event.data);
        } catch (Exception e) {
            log.error("处理WebSocket消息时发生错误: {}", message, e);
        }
    }

    /**
     * 解析WebSocket消息
     *
     * @param message 原始消息
     * @return 解析后的事件对象，解析失败返回null
     */
    private LolEvent parseMessage(String message) {
        try {
            JSONArray arr = JSON.parseArray(message);

            // 验证消息格式
            if (arr.size() < 3) {
                log.debug("消息格式不正确，数组长度小于3: {}", message);
                return null;
            }

            // 验证事件类型
            String eventType = arr.getString(1);
            if (!EVENT_TYPE.equals(eventType)) {
                log.debug("非LOL API事件，跳过处理: {}", eventType);
                return null;
            }

            // 提取事件数据
            JSONObject eventData = arr.getJSONObject(2);
            if (eventData == null) {
                log.debug("事件数据为空");
                return null;
            }

            String uri = eventData.getString("uri");
            String data = eventData.getString("data");

            if (StringUtils.isEmpty(uri)) {
                log.debug("事件URI为空");
                return null;
            }

            return new LolEvent(uri, data);

        } catch (Exception e) {
            log.debug("解析消息失败: {}", message, e);
            return null;
        }
    }

    /**
     * 根据URI路由事件到对应的处理器
     * 统一使用 Thread.startVirtualThread() 方式创建虚拟线程
     */
    private void routeEvent(String uri, String data) {
        switch (uri) {
            case GAMEFLOW_PHASE_URI -> Thread.startVirtualThread(() -> {
                try {
                    gameFlowHandler.onGameFlowUpdate(data);
                } catch (Exception e) {
                    log.error("处理游戏流程事件失败", e);
                }
            });

            case CHAMP_SELECT_URI -> Thread.startVirtualThread(() -> {
                try {
                    championSelectHandler.onChampSelectSessionUpdate(data);
                } catch (Exception e) {
                    log.error("处理英雄选择事件失败", e);
                }
            });

            case MATCHMAKING_URI -> Thread.startVirtualThread(() -> {
                try {
                    gameFlowHandler.handleGameMode(data);
                } catch (Exception e) {
                    log.error("处理匹配事件失败", e);
                }
            });

            default -> log.debug("未知事件URI，跳过处理: {}", uri);
        }
    }


    private record LolEvent(String uri, String data) {
    }
}