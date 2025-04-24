package com.example.huayeloltool.model;

import com.example.huayeloltool.enums.GameEnums;
import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * 当前本局游戏信息
 */
@Data
public class CustomGameSessionDetails {

    /**
     * 游戏模式
     */
    private Integer queueId;

    /**
     * 楼层。1-5楼
     */
    private Integer floor;

    /**
     * 位置。例如中路
     */
    private String position;

    /**
     * 是否已经禁用了英雄
     */
    private Boolean isBaned = false;

    /**
     * 是否已经选择了英雄
     */
    private Boolean isSelected = false;

    /**
     * 已处理过的动作ID
     */
    private Set<String> processedActionIds = new HashSet<>();

    /**
     * 单例
     */
    private static CustomGameSessionDetails instance;

    public static CustomGameSessionDetails getInstance() {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        return instance;
    }

    public static Set<String> getProcessedActionIds() {
        return instance.processedActionIds;
    }


    public static void addProcessedActionIds(String actionId) {
        instance.processedActionIds.add(actionId);
    }

    public static Boolean isBaned() {
        return instance.isBaned;
    }

    public static Boolean isSelected() {
        return instance.isSelected;
    }

    public static Integer getFloor() {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        return instance.floor;
    }

    public static Integer getQueueId() {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        return instance.queueId;
    }


    public static String getPosition() {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        return instance.position;
    }


    public static void setQueue(Integer queueId) {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        instance.queueId = queueId;
    }

    public static void setFloor(Integer floor) {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        instance.floor = floor;
    }


    public static void setPosition(String position) {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        instance.position = position;
    }

    public static synchronized void setIsBanned(boolean b) {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        instance.isBaned = b;

    }

    public static synchronized void setIsSelected(boolean b) {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        instance.isSelected = b;
    }

    public static void init() {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        instance.isSelected = false;
        instance.isBaned = false;
        instance.queueId = 0;
        instance.floor = 0;
        instance.position = null;
        instance.processedActionIds.clear();
    }

    /**
     * 是否是单双排排位
     */
    public static boolean isSoloRank() {
        if (instance == null) {
            instance = new CustomGameSessionDetails();
        }
        return Objects.equals(GameEnums.GameQueueID.RANK_SOLO.getId(), instance.queueId);
    }
}
