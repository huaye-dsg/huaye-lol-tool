package com.example.huayeloltool.model.game;

import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.cache.CustomGameCache;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 当前本局游戏信息
 */
@Data
@Component
public class CustomGameSession {

    // 游戏模式
    private Integer queueId;

    // 楼层。1 - 5 楼
    private Integer floor;

    // 位置。例如中路
    private String position;

    // 是否已经禁用了英雄
    private Boolean isBanned = false;

    // 是否已经选择了英雄
    private Boolean isSelected = false;

    // 已处理过的动作 ID
    private final Set<String> processedActionIds = ConcurrentHashMap.newKeySet();

    // 队友位置映射
    private Map<Integer, ChampSelectSessionInfo.Player> positionMap = new ConcurrentHashMap<>();


    // 使用 volatile 关键字保证可见性
    private static volatile CustomGameSession instance;

    // 私有构造函数，防止外部实例化
    private CustomGameSession() {
    }

    public void initPositionMapIfEmpty(List<ChampSelectSessionInfo.Player> team) {
        if (positionMap.isEmpty()) {
            team.forEach(player -> positionMap.putIfAbsent(player.getCellId(), player));
        }
    }

    // 双重检查锁定实现单例模式
    public static CustomGameSession getInstance() {
        if (instance == null) {
            synchronized (CustomGameSession.class) {
                if (instance == null) {
                    instance = new CustomGameSession();
                }
            }
        }
        return instance;
    }

    public boolean markActionProcessed(String actionId) {
        return processedActionIds.add(actionId);
    }

    public void markActionUnProcessed(String actionId) {
        processedActionIds.remove(actionId);
    }

    // 重置所有状态
    public void reset() {
        queueId = null;
        floor = null;
        position = null;
        isBanned = false;
        isSelected = false;
        processedActionIds.clear();
        positionMap.clear();
        CustomGameCache.clear();
    }

    // 是否是单双排排位
    public static boolean isSoloRank() {
        CustomGameSession session = getInstance();
        return Objects.equals(GameEnums.GameQueueID.RANK_SOLO.getId(), session.getQueueId());
    }
}
