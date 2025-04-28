package com.example.huayeloltool.model.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
public class CalcScoreConf {
    private boolean enabled = true;
    private double[] firstBlood = {10.0, 5.0};
    private double[] pentaKills = {20.0};
    private double[] quadraKills = {10.0};
    private double[] tripleKills = {5.0};
    private double[] joinTeamRateRank = {10.0, 5.0, 5.0, 10.0};
    private double[] goldEarnedRank = {10.0, 5.0, 5.0, 10.0};
    private double[] hurtRank = {10.0, 5.0};
    private double[] money2hurtRateRank = {10.0, 5.0};
    private double[] visionScoreRank = {10.0, 5.0};
    private double[][] minionsKilled = {
            {10.0, 20.0},
            {9.0, 10.0},
            {8.0, 5.0}
    };
    private List<RateItemConf> killRate = Arrays.asList(
            new RateItemConf(50, new double[][]{
                    {15.0, 40.0},
                    {10.0, 20.0},
                    {5.0, 10.0}
            }),
            new RateItemConf(40, new double[][]{
                    {15.0, 20.0},
                    {10.0, 10.0},
                    {5.0, 5.0}
            })
    );
    private List<RateItemConf> hurtRate = Arrays.asList(
            new RateItemConf(40, new double[][]{
                    {15.0, 40.0},
                    {10.0, 20.0},
                    {5.0, 10.0}
            }),
            new RateItemConf(30, new double[][]{
                    {15.0, 20.0},
                    {10.0, 10.0},
                    {5.0, 5.0}
            })
    );
    private List<RateItemConf> assistRate = Arrays.asList(
            new RateItemConf(50, new double[][]{
                    {20.0, 30.0},
                    {18.0, 25.0},
                    {15.0, 20.0},
                    {10.0, 10.0},
                    {5.0, 5.0}
            }),
            new RateItemConf(40, new double[][]{
                    {20.0, 15.0},
                    {15.0, 10.0},
                    {10.0, 5.0},
                    {5.0, 3.0}
            })
    );
    private double[] adjustKDA = {2.0, 5.0};
    private HorseScoreConf[] horse = {
            new HorseScoreConf(180.0, "通天代"),
            new HorseScoreConf(150.0, "小 代"),
            new HorseScoreConf(125.0, "上等马"),
            new HorseScoreConf(105.0, "中等马"),
            new HorseScoreConf(95.0, "下等马"),
            new HorseScoreConf(0.0001, "牛 马")
    };
    private boolean mergeMsg = false;

    private static CalcScoreConf instance;

    public static CalcScoreConf getInstance() {
        if (instance == null) {
            instance = new CalcScoreConf();
        }
        return instance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateItemConf {
        /**
         * 比率限制 (例如: >30%)
         */
        private double limit;

        /**
         * 分数配置，格式为 [ [最低人头限制, 加分数] ]
         */
        private double[][] scoreConf;
    }


}
