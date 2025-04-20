package com.example.huayeloltool.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class UserScore {
    private Long summonerID;
    private String summonerName;
    private Double score;
    private List<Kda> currKDA;

    // 扩展字段。后加的
    private String puuid;



    public UserScore(Long summonerID, Double score) {
        this.summonerID = summonerID;
        this.score = score;
    }


    @Data
    public static class Kda {
        private Integer kills;
        private Integer deaths;
        private Integer assists;

        // 扩展字段。后加的
        private Boolean win;
        private Integer championId;
        private String championName;

    }
}
