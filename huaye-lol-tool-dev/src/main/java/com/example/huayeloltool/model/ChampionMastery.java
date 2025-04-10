package com.example.huayeloltool.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChampionMastery {

    // 英雄的 ID
    private int championId;
    // 英雄的等级
    private int championLevel;
    // 英雄的积分
    private int championPoints;
//    // 自上次升级以来获得的英雄积分
//    private int championPointsSinceLastLevel;
//    // 距离下次升级还需要的英雄积分
//    private int championPointsUntilNextLevel;
//    // 英雄赛季里程碑
//    private int championSeasonMilestone;
//    // 最高评价等级
//    private String highestGrade;
//    // 最后游玩时间
//    private long lastPlayTime;
//    // 下次升级所需的标记数
//    private int markRequiredForNextLevel;
//    // 里程碑评价等级列表
//    private List<String> milestoneGrades;
//    // 下个赛季的里程碑信息
//    private NextSeasonMilestone nextSeasonMilestone;
//    // 用户的唯一标识符
//    private String puuid;
//    // 已获得的代币数量
//    private int tokensEarned;


    // 下个赛季的里程碑信息类
//    @Data
//    public static class NextSeasonMilestone {
//        // 是否为奖励里程碑
//        private boolean bonus;
//        // 所需评价等级的数量
//        private Map<String, Integer> requireGradeCounts;
//        // 奖励配置信息
//        private RewardConfig rewardConfig;
//        // 奖励标记数
//        private int rewardMarks;
//    }
//
//    // 奖励配置信息类
//    @Data
//    public static class RewardConfig {
//        // 最大奖励值
//        private int maximumReward;
//        // 奖励值
//        private String rewardValue;
//    }
}
