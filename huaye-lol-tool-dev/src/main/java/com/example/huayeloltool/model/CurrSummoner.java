package com.example.huayeloltool.model;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;


@Data
public class CurrSummoner {
    private Long accountId;
    private String displayName;
    private String internalName;
    private Boolean nameChangeFlag;
    private Integer percentCompleteForNextLevel;
    private Integer profileIconId;
    private String puuid;
    private RerollPoints rerollPoints;
    private Long summonerId;
    private String gameName;
    private String tagLine;
    private Integer summonerLevel;
    private Boolean unnamed;
    private Integer xpSinceLastLevel;
    private Integer xpUntilNextLevel;

    private static CurrSummoner instance;


    public static synchronized CurrSummoner setInstance(CurrSummoner currSummoner) {
        instance = currSummoner;
        return instance;
    }


    public static synchronized CurrSummoner getInstance() {
        if (instance == null) {
            instance = new CurrSummoner();
        }
        return instance;
    }

    @Data
    public static class RerollPoints {
        private int currentPoints;
        private int maxRolls;
        private int numberOfRolls;
        private int pointsCostToRoll;
        private int pointsToReroll;
    }
}
