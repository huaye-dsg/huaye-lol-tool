package com.example.huayeloltool.model.cache;

import com.example.huayeloltool.model.score.UserScore;
import lombok.Data;

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
    public static class Item {
        private String horse;
        private Double score;
        private String rank;
        private String summonerName;
        List<String> currKDA;
    }

}
