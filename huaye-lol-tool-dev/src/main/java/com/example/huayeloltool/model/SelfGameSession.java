package com.example.huayeloltool.model;

import com.example.huayeloltool.enums.GameEnums;
import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
public class SelfGameSession {


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

    private Set<Integer> processedActionIds = new HashSet<>();


    private static SelfGameSession instance;


    public static SelfGameSession getInstance() {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        return instance;
    }

    public static Set<Integer> getProcessedActionIds() {
        return instance.processedActionIds;
    }


    public static void addProcessedActionIds(Integer actionId) {
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
            instance = new SelfGameSession();
        }
        return instance.floor;
    }

    public static Integer getQueueId() {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        return instance.queueId;
    }


    public static String getPosition() {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        return instance.position;
    }


    public static void setQueue(Integer queueId) {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        instance.queueId = queueId;
    }

    public static void setFloor(Integer floor) {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        instance.floor = floor;
    }


    public static void setPosition(String position) {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        instance.position = position;
    }

    public static void setIsBanned(boolean b) {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        instance.isBaned = b;

    }

    public static void setIsSelected(boolean b) {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        instance.isSelected = b;
    }

    public static void init() {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        instance.isSelected = false;
        instance.isBaned = false;
        instance.queueId = 0;
        instance.floor = 0;
        instance.position = null;
        instance.processedActionIds.clear();
    }

    public static boolean isSoloRank() {
        if (instance == null) {
            instance = new SelfGameSession();
        }
        return Objects.equals(GameEnums.GameQueueID.RANK_SOLO.getId(), instance.queueId);
    }
}
