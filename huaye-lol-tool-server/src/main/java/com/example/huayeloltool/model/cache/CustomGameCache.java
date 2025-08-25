package com.example.huayeloltool.model.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
public class CustomGameCache {

    private static final CustomGameCache INSTANCE = new CustomGameCache();

    public static CustomGameCache getInstance() {
        if (INSTANCE == null) {
            return new CustomGameCache();
        }
        return INSTANCE;
    }

    // 清理数据
    public static void clear() {
        INSTANCE.enemyList.clear();
        INSTANCE.teamList.clear();
    }


    private List<Item> teamList = new ArrayList<>();

    private List<Item> enemyList = new ArrayList<>();

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
