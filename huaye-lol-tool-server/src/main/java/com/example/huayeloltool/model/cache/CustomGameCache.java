package com.example.huayeloltool.model.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Data
public class CustomGameCache {

    private static final CustomGameCache INSTANCE = new CustomGameCache();

    public static CustomGameCache getInstance() {
        return INSTANCE;
    }

    // 清理数据
    public static void clear() {
        INSTANCE.enemyList.clear();
        INSTANCE.teamList.clear();
    }

    private final List<Item> teamList = new ArrayList<>();

    private final List<Item> enemyList = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private String horse;
        private Integer score;
        private String rank;
        private String summonerName;
        List<KdaDetail> currKDA;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KdaDetail {
        private String queueGame;
        private Boolean win;
        private Integer championId;
        private String imageUrl;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
    }

}
