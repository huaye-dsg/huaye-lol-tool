package com.example.huayeloltool.model.score;

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
    private String puuid;
    private String extMsg;


    public UserScore(Long summonerID, Double score) {
        this.summonerID = summonerID;
        this.score = score;
    }


    @Data
    public static class Kda {
        private Integer kills;
        private Integer deaths;
        private Integer assists;

        private Boolean win;
        private String championName;
        private String queueGame;
        private Integer championId;
        private String position;
    }

}
