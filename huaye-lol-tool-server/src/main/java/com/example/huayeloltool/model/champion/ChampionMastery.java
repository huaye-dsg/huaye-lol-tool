package com.example.huayeloltool.model.champion;

import lombok.Data;

@Data
public class ChampionMastery {
    // 英雄的 ID
    public int championId;
    // 英雄的等级
    public int championLevel;
    // 英雄的积分
    public int championPoints;
    //    // 最高评价等级
    //public String highestGrade;
    // 最后游玩时间
    public long lastPlayTime;
    //    // 自上次升级以来获得的英雄积分
//    public int championPointsSinceLastLevel;
//    // 距离下次升级还需要的英雄积分
//    public int championPointsUntilNextLevel;
//    // 英雄赛季里程碑
//    public int championSeasonMilestone;

//    // 下次升级所需的标记数
//    public int markRequiredForNextLevel;
//    // 里程碑评价等级列表
//    public List<String> milestoneGrades;
//    // 下个赛季的里程碑信息
//    public NextSeasonMilestone nextSeasonMilestone;
//    // 用户的唯一标识符
//    public String puuid;
//    // 已获得的代币数量
//    public int tokensEarned;


    // 下个赛季的里程碑信息类
//    @Data
//    public static class NextSeasonMilestone {
//        // 是否为奖励里程碑
//        public boolean bonus;
//        // 所需评价等级的数量
//        public Map<String, Integer> requireGradeCounts;
//        // 奖励配置信息
//        public RewardConfig rewardConfig;
//        // 奖励标记数
//        public int rewardMarks;
//    }
//
//    // 奖励配置信息类
//    @Data
//    public static class RewardConfig {
//        // 最大奖励值
//        public int maximumReward;
//        // 奖励值
//        public String rewardValue;
//    }
}
