package com.example.huayeloltool.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

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

    // 线程池配置
    @Resource(name = "webSocketMessageExecutor")
    private ExecutorService webSocketMessageExecutor;

    // 消息统计
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong validMessages = new AtomicLong(0);
    private final AtomicLong invalidMessages = new AtomicLong(0);

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
        totalMessages.incrementAndGet();

        try {
            // 1. 基础验证
            if (StringUtils.isEmpty(message)) {
                log.debug("收到空消息，跳过处理");
                return;
            }

            // 2. 解析消息格式
            LolEvent event = parseMessage(message);
            if (event == null) {
                invalidMessages.incrementAndGet();
                return;
            }

            // 3. 路由事件到对应处理器
            routeEvent(event);
            validMessages.incrementAndGet();

        } catch (Exception e) {
            log.error("处理WebSocket消息时发生错误: {}", message, e);
            invalidMessages.incrementAndGet();
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
     *
     * @param event 解析后的事件
     */
    private void routeEvent(LolEvent event) {
        String uri = event.uri();
        String data = event.data();

        switch (uri) {
            case GAMEFLOW_PHASE_URI -> {
                webSocketMessageExecutor.submit(() -> {
                    try {
                        gameFlowHandler.onGameFlowUpdate(data);
                    } catch (Exception e) {
                        log.error("处理游戏流程事件失败", e);
                    }
                });
            }

            case CHAMP_SELECT_URI -> {
                webSocketMessageExecutor.submit(() -> {
                    try {
                        championSelectHandler.onChampSelectSessionUpdate(data);
                    } catch (Exception e) {
                        log.error("处理英雄选择事件失败", e);
                    }
                });
            }

            case MATCHMAKING_URI -> {
                webSocketMessageExecutor.submit(() -> {
                    try {
                        gameFlowHandler.handleGameMode(data);
                    } catch (Exception e) {
                        log.error("处理匹配事件失败", e);
                    }
                });
            }

            default -> {
                log.debug("未知事件URI，跳过处理: {}", uri);
            }
        }
    }

    /**
     * 获取消息处理统计信息
     *
     * @return 格式化的统计信息字符串
     */
    public String getStatistics() {
        return String.format(
                "消息统计 - 总计: %d, 有效: %d, 无效: %d, 成功率: %.2f%%",
                totalMessages.get(),
                validMessages.get(),
                invalidMessages.get(),
                totalMessages.get() > 0 ? (validMessages.get() * 100.0 / totalMessages.get()) : 0.0
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalMessages.set(0);
        validMessages.set(0);
        invalidMessages.set(0);
        log.info("消息统计信息已重置");
    }


    private record LolEvent(String uri, String data) {
    }
}