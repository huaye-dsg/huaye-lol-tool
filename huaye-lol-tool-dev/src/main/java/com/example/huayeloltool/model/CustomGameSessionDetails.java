package com.example.huayeloltool.model;

import com.example.huayeloltool.enums.GameEnums;
import com.example.huayeloltool.model.champion.ChampSelectSessionInfo;
import com.example.huayeloltool.model.game.GameSummary;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 当前本局游戏信息
 */
@Getter
@Data
public class CustomGameSessionDetails {

    // 游戏模式
    @Setter
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

    /**
     * 队友位置映射
     */
    private Map<Integer, ChampSelectSessionInfo.Player> positionMap = new ConcurrentHashMap<>();


    // 使用 volatile 关键字保证可见性
    private static volatile CustomGameSessionDetails instance;

    // 私有构造函数，防止外部实例化
    private CustomGameSessionDetails() {
    }

    public void initPositionMapIfEmpty(List<ChampSelectSessionInfo.Player> team) {
        if (positionMap.isEmpty()) {
            team.forEach(player -> positionMap.putIfAbsent(player.getCellId(), player));
        }
    }

    // 双重检查锁定实现单例模式
    public static CustomGameSessionDetails getInstance() {
        if (instance == null) {
            synchronized (CustomGameSessionDetails.class) {
                if (instance == null) {
                    instance = new CustomGameSessionDetails();
                }
            }
        }
        return instance;
    }

    public boolean markActionProcessed(String actionId) {
        return processedActionIds.add(actionId);
    }

    // 初始化方法
    public void init() {
        this.isSelected = false;
        this.isBanned = false;
        this.queueId = 0;
        this.floor = 0;
        this.position = null;
        this.processedActionIds.clear();
        this.positionMap.clear();
    }

    // 是否是单双排排位
    public static boolean isSoloRank() {
        CustomGameSessionDetails session = getInstance();
        return Objects.equals(GameEnums.GameQueueID.RANK_SOLO.getId(), session.getQueueId());
    }
}
