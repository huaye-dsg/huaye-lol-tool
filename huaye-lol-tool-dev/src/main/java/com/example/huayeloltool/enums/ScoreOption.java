package com.example.huayeloltool.enums;

import lombok.Getter;


@Getter
public enum ScoreOption {
    FIRST_BLOOD_KILL("一血击杀"),
    FIRST_BLOOD_ASSIST("一血助攻"),
    PENTA_KILLS("五杀"),
    QUADRA_KILLS("四杀"),
    TRIPLE_KILLS("三杀"),
    JOIN_TEAM_RATE_RANK("参团率排名"),
    GOLD_EARNED_RANK("打钱排名"),
    HURT_RANK("伤害排名"),
    MONEY_TO_HURT_RATE_RANK("金钱转换伤害比排名"),
    VISION_SCORE_RANK("视野得分排名"),
    MINIONS_KILLED("补兵"),
    KILL_RATE("击杀占比"),
    HURT_RATE("伤害占比"),
    ASSIST_RATE("助攻占比"),
    KDA_ADJUST("kda微调");

    private final String displayName;

    ScoreOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
