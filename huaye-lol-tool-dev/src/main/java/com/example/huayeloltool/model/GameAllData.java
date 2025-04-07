package com.example.huayeloltool.model;

import lombok.Data;

import java.util.List;

@Data
public class GameAllData {
    private long accountId;
    private Games games;
    private String platformId;

    @Data
    public static class Games {
        private String gameBeginDate;
        private int gameCount;
        private String gameEndDate;
        private int gameIndexBegin;
        private int gameIndexEnd;
        private List<GameInfo> games;
    }


}