package com.example.huayeloltool.model.summoner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class RankedInfo {

    //@JsonProperty("highestRankedEntrySR")
    //private HighestRankedEntrySRDto highestRankedEntrySR; // 召唤师峡谷最高排位记录信息

//    @JsonProperty("currentSeasonSplitPoints")
//    private Integer currentSeasonSplitPoints; // 当前赛季分段积分

//    @JsonProperty("earnedRegaliaRewardIds")
//    private List<Object> earnedRegaliaRewardIds; // 已获得的徽章奖励ID列表

//    @JsonProperty("highestPreviousSeasonAchievedDivision")
//    private String highestPreviousSeasonAchievedDivision; // 上一赛季达到的最高分段

//    @JsonProperty("highestPreviousSeasonAchievedTier")
//    private String highestPreviousSeasonAchievedTier; // 上一赛季达到的最高段位

//    @JsonProperty("highestPreviousSeasonEndDivision")
//    private String highestPreviousSeasonEndDivision; // 上一赛季结束时的最高分段

//    @JsonProperty("highestPreviousSeasonEndTier")
//    private String highestPreviousSeasonEndTier; // 上一赛季结束时的最高段位

//    @JsonProperty("highestRankedEntry")
//    private HighestRankedEntryDto highestRankedEntry; // 最高排位记录信息（通用）

    //    @JsonProperty("queueMap")
    private QueueMapDto queueMap; // 队列信息映射表
//
//    @JsonProperty("queues")
//    private List<QueuesDto> queues; // 队列信息列表

//    @JsonProperty("rankedRegaliaLevel")
//    private Integer rankedRegaliaLevel; // 排位徽章等级

//    @JsonProperty("seasons")
//    private SeasonsDto seasons; // 赛季信息映射表

    //@NoArgsConstructor
    //@Data
    //public static class HighestRankedEntryDto {
    //    @JsonProperty("division")
    //    private String division; // 当前分段（如III、II等）
    //
    //    @JsonProperty("isProvisional")
    //    private Boolean isProvisional; // 是否处于定位赛阶段
    //
    //    @JsonProperty("leaguePoints")
    //    private Integer leaguePoints; // 段位积分（LP）
    //
    //    @JsonProperty("losses")
    //    private Integer losses; // 失败场次
    //
    //    @JsonProperty("miniSeriesProgress")
    //    private String miniSeriesProgress; // 晋级赛进度（如WLLW）
    //
    //    @JsonProperty("previousSeasonAchievedDivision")
    //    private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
    //
    //    @JsonProperty("previousSeasonAchievedTier")
    //    private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
    //
    //    @JsonProperty("previousSeasonEndDivision")
    //    private String previousSeasonEndDivision; // 上一赛季结束时的分段
    //
    //    @JsonProperty("previousSeasonEndTier")
    //    private String previousSeasonEndTier; // 上一赛季结束时的段位
    //
    //    @JsonProperty("provisionalGameThreshold")
    //    private Integer provisionalGameThreshold; // 定位赛所需总场次
    //
    //    @JsonProperty("provisionalGamesRemaining")
    //    private Integer provisionalGamesRemaining; // 剩余定位赛场次
    //
    //    @JsonProperty("queueType")
    //    private String queueType; // 队列类型（如RANKED_SOLO_5x5）
    //
    //    @JsonProperty("ratedRating")
    //    private Integer ratedRating; // 评级分数（隐藏分）
    //
    //    @JsonProperty("ratedTier")
    //    private String ratedTier; // 评级段位
    //
    //    @JsonProperty("tier")
    //    private String tier; // 当前段位（如GOLD、SILVER等）
    //
    //    @JsonProperty("warnings")
    //    private WarningsDto warnings; // 段位衰减警告信息
    //
    //    @JsonProperty("wins")
    //    private Integer wins; // 胜利场次
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class WarningsDto {
    //        @JsonProperty("daysUntilDecay")
    //        private Integer daysUntilDecay; // 距离段位衰减剩余天数
    //
    //        @JsonProperty("demotionWarning")
    //        private Integer demotionWarning; // 降级警告剩余场次
    //
    //        @JsonProperty("displayDecayWarning")
    //        private Boolean displayDecayWarning; // 是否显示段位衰减警告
    //
    //        @JsonProperty("timeUntilInactivityStatusChanges")
    //        private Integer timeUntilInactivityStatusChanges; // 距离 inactive 状态变更剩余时间
    //    }
    //}

    //@NoArgsConstructor
    //@Data
    //public static class HighestRankedEntrySRDto {
    //    @JsonProperty("division")
    //    private String division; // 当前分段（如III、II等）
    //
    //    @JsonProperty("tier")
    //    private String tier; // 当前段位（如GOLD、SILVER等）
    //
    //
    //    @JsonProperty("leaguePoints")
    //    private Integer leaguePoints; // 胜点
    //
    //    @JsonProperty("queueType")
    //    private String queueType; // 队列类型（如RANKED_SOLO_5x5）
    //
    //    @JsonProperty("ratedRating")
    //    private Integer ratedRating; // 评级分数（隐藏分）
    //
    //    @JsonProperty("ratedTier")
    //    private String ratedTier; // 评级段位


    //    @JsonProperty("isProvisional")
    //    private Boolean isProvisional; // 是否处于定位赛阶段
    //
    //
    //    @JsonProperty("losses")
    //    private Integer losses; // 失败场次
    //
    //    @JsonProperty("miniSeriesProgress")
    //    private String miniSeriesProgress; // 晋级赛进度（如WLLW）
    //
    //    @JsonProperty("previousSeasonAchievedDivision")
    //    private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
    //
    //    @JsonProperty("previousSeasonAchievedTier")
    //    private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
    //
    //    @JsonProperty("previousSeasonEndDivision")
    //    private String previousSeasonEndDivision; // 上一赛季结束时的分段
    //
    //    @JsonProperty("previousSeasonEndTier")
    //    private String previousSeasonEndTier; // 上一赛季结束时的段位
    //
    //    @JsonProperty("provisionalGameThreshold")
    //    private Integer provisionalGameThreshold; // 定位赛所需总场次
    //
    //    @JsonProperty("provisionalGamesRemaining")
    //    private Integer provisionalGamesRemaining; // 剩余定位赛场次
    //
    //
    //    @JsonProperty("warnings")
    //    private WarningsDto warnings; // 段位衰减警告信息
    //
    //    @JsonProperty("wins")
    //    private Integer wins; // 胜利场次
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class WarningsDto {
    //        @JsonProperty("daysUntilDecay")
    //        private Integer daysUntilDecay; // 距离段位衰减剩余天数
    //
    //        @JsonProperty("demotionWarning")
    //        private Integer demotionWarning; // 降级警告剩余场次
    //
    //        @JsonProperty("displayDecayWarning")
    //        private Boolean displayDecayWarning; // 是否显示段位衰减警告
    //
    //        @JsonProperty("timeUntilInactivityStatusChanges")
    //        private Integer timeUntilInactivityStatusChanges; // 距离 inactive 状态变更剩余时间
    //    }
    //}

    @Data
    public static class QueueMapDto {
        //    @JsonProperty("RANKED_FLEX_SR")
        //    private RANKEDFLEXSRDto rankedFlexSr; // 灵活组排队列信息
        //
        @JsonProperty("RANKED_SOLO_5x5")
        private RANKEDSOLO5x5Dto rankedSolo5x5; // 单双排队列信息

        //
        //    @JsonProperty("RANKED_TFT")
        //    private RANKEDTFTDto rankedTft; // 云顶之弈单排队列信息
        //
        //    @JsonProperty("RANKED_TFT_PAIRS")
        //    private RANKEDTFTPAIRSDto rankedTftPairs; // 云顶之弈双人队列信息
        //
        //    @JsonProperty("RANKED_TFT_TURBO")
        //    private RANKEDTFTTURBODto rankedTftTurbo; // 云顶之弈快速队列信息
        //
        //    @Data
        //    public static class RANKEDFLEXSRDto {
        //        @JsonProperty("division")
        //        private String division; // 当前分段（如III、II等）
        //
        //        @JsonProperty("isProvisional")
        //        private Boolean isProvisional; // 是否处于定位赛阶段
        //
        //        @JsonProperty("leaguePoints")
        //        private Integer leaguePoints; // 段位积分（LP）
        //
        //        @JsonProperty("losses")
        //        private Integer losses; // 失败场次
        //
        //        @JsonProperty("miniSeriesProgress")
        //        private String miniSeriesProgress; // 晋级赛进度（如WLLW）
        //
        //        @JsonProperty("previousSeasonAchievedDivision")
        //        private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
        //
        //        @JsonProperty("previousSeasonAchievedTier")
        //        private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
        //
        //        @JsonProperty("previousSeasonEndDivision")
        //        private String previousSeasonEndDivision; // 上一赛季结束时的分段
        //
        //        @JsonProperty("previousSeasonEndTier")
        //        private String previousSeasonEndTier; // 上一赛季结束时的段位
        //
        //        @JsonProperty("provisionalGameThreshold")
        //        private Integer provisionalGameThreshold; // 定位赛所需总场次
        //
        //        @JsonProperty("provisionalGamesRemaining")
        //        private Integer provisionalGamesRemaining; // 剩余定位赛场次
        //
        //        @JsonProperty("queueType")
        //        private String queueType; // 队列类型（固定为RANKED_FLEX_SR）
        //
        //        @JsonProperty("ratedRating")
        //        private Integer ratedRating; // 评级分数（隐藏分）
        //
        //        @JsonProperty("ratedTier")
        //        private String ratedTier; // 评级段位
        //
        //        @JsonProperty("tier")
        //        private String tier; // 当前段位（如GOLD、SILVER等）
        //
        //        @JsonProperty("warnings")
        //        private Object warnings; // 警告信息（无具体结构时用Object）
        //
        //        @JsonProperty("wins")
        //        private Integer wins; // 胜利场次
        //    }
        //
        @Data
        public static class RANKEDSOLO5x5Dto {

            @JsonProperty("tier")
            private String tier; // 当前段位（如GOLD、SILVER等）

            @JsonProperty("division")
            private String division; // 当前分段（如III、II等）

            @JsonProperty("leaguePoints")
            private Integer leaguePoints; // 段位积分（LP）
            //
            //        @JsonProperty("ratedTier")
            //        private String ratedTier; // 评级段位
            //
            //
            //        @JsonProperty("isProvisional")
            //        private Boolean isProvisional; // 是否处于定位赛阶段
            //

            //
            //        @JsonProperty("losses")
            //        private Integer losses; // 失败场次
            //
            //        @JsonProperty("miniSeriesProgress")
            //        private String miniSeriesProgress; // 晋级赛进度（如WLLW）
            //
            //        @JsonProperty("previousSeasonAchievedDivision")
            //        private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
            //
            //        @JsonProperty("previousSeasonAchievedTier")
            //        private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
            //
            //        @JsonProperty("previousSeasonEndDivision")
            //        private String previousSeasonEndDivision; // 上一赛季结束时的分段
            //
            //        @JsonProperty("previousSeasonEndTier")
            //        private String previousSeasonEndTier; // 上一赛季结束时的段位
            //
            //        @JsonProperty("provisionalGameThreshold")
            //        private Integer provisionalGameThreshold; // 定位赛所需总场次
            //
            //        @JsonProperty("provisionalGamesRemaining")
            //        private Integer provisionalGamesRemaining; // 剩余定位赛场次
            //
            //        @JsonProperty("queueType")
            //        private String queueType; // 队列类型（固定为RANKED_SOLO_5x5）
            //
            //        @JsonProperty("ratedRating")
            //        private Integer ratedRating; // 评级分数（隐藏分）

            //
            //        @JsonProperty("warnings")
            //        private WarningsDto warnings; // 段位衰减警告信息
            //
            //        @JsonProperty("wins")
            //        private Integer wins; // 胜利场次
            //
            //        @NoArgsConstructor
            //        @Data
            //        public static class WarningsDto {
            //            @JsonProperty("daysUntilDecay")
            //            private Integer daysUntilDecay; // 距离段位衰减剩余天数
            //
            //            @JsonProperty("demotionWarning")
            //            private Integer demotionWarning; // 降级警告剩余场次
            //
            //            @JsonProperty("displayDecayWarning")
            //            private Boolean displayDecayWarning; // 是否显示段位衰减警告
            //
            //            @JsonProperty("timeUntilInactivityStatusChanges")
            //            private Integer timeUntilInactivityStatusChanges; // 距离 inactive 状态变更剩余时间
            //        }
        }
        //
        //    @Data
        //    public static class RANKEDTFTDto {
        //        @JsonProperty("division")
        //        private String division; // 当前分段（如III、II等）
        //
        //        @JsonProperty("isProvisional")
        //        private Boolean isProvisional; // 是否处于定位赛阶段
        //
        //        @JsonProperty("leaguePoints")
        //        private Integer leaguePoints; // 段位积分（LP）
        //
        //        @JsonProperty("losses")
        //        private Integer losses; // 失败场次
        //
        //        @JsonProperty("miniSeriesProgress")
        //        private String miniSeriesProgress; // 晋级赛进度（如WLLW）
        //
        //        @JsonProperty("previousSeasonAchievedDivision")
        //        private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
        //
        //        @JsonProperty("previousSeasonAchievedTier")
        //        private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
        //
        //        @JsonProperty("previousSeasonEndDivision")
        //        private String previousSeasonEndDivision; // 上一赛季结束时的分段
        //
        //        @JsonProperty("previousSeasonEndTier")
        //        private String previousSeasonEndTier; // 上一赛季结束时的段位
        //
        //        @JsonProperty("provisionalGameThreshold")
        //        private Integer provisionalGameThreshold; // 定位赛所需总场次
        //
        //        @JsonProperty("provisionalGamesRemaining")
        //        private Integer provisionalGamesRemaining; // 剩余定位赛场次
        //
        //        @JsonProperty("queueType")
        //        private String queueType; // 队列类型（固定为RANKED_TFT）
        //
        //        @JsonProperty("ratedRating")
        //        private Integer ratedRating; // 评级分数（隐藏分）
        //
        //        @JsonProperty("ratedTier")
        //        private String ratedTier; // 评级段位
        //
        //        @JsonProperty("tier")
        //        private String tier; // 当前段位（如GOLD、SILVER等）
        //
        //        @JsonProperty("warnings")
        //        private Object warnings; // 警告信息（无具体结构时用Object）
        //
        //        @JsonProperty("wins")
        //        private Integer wins; // 胜利场次
        //    }
        //
        //    @Data
        //    public static class RANKEDTFTPAIRSDto {
        //        @JsonProperty("division")
        //        private String division; // 当前分段（如III、II等）
        //
        //        @JsonProperty("isProvisional")
        //        private Boolean isProvisional; // 是否处于定位赛阶段
        //
        //        @JsonProperty("leaguePoints")
        //        private Integer leaguePoints; // 段位积分（LP）
        //
        //        @JsonProperty("losses")
        //        private Integer losses; // 失败场次
        //
        //        @JsonProperty("miniSeriesProgress")
        //        private String miniSeriesProgress; // 晋级赛进度（如WLLW）
        //
        //        @JsonProperty("previousSeasonAchievedDivision")
        //        private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
        //
        //        @JsonProperty("previousSeasonAchievedTier")
        //        private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
        //
        //        @JsonProperty("previousSeasonEndDivision")
        //        private String previousSeasonEndDivision; // 上一赛季结束时的分段
        //
        //        @JsonProperty("previousSeasonEndTier")
        //        private String previousSeasonEndTier; // 上一赛季结束时的段位
        //
        //        @JsonProperty("provisionalGameThreshold")
        //        private Integer provisionalGameThreshold; // 定位赛所需总场次
        //
        //        @JsonProperty("provisionalGamesRemaining")
        //        private Integer provisionalGamesRemaining; // 剩余定位赛场次
        //
        //        @JsonProperty("queueType")
        //        private String queueType; // 队列类型（固定为RANKED_TFT_PAIRS）
        //
        //        @JsonProperty("ratedRating")
        //        private Integer ratedRating; // 评级分数（隐藏分）
        //
        //        @JsonProperty("ratedTier")
        //        private String ratedTier; // 评级段位
        //
        //        @JsonProperty("tier")
        //        private String tier; // 当前段位（如GOLD、SILVER等）
        //
        //        @JsonProperty("warnings")
        //        private Object warnings; // 警告信息（无具体结构时用Object）
        //
        //        @JsonProperty("wins")
        //        private Integer wins; // 胜利场次
        //    }
        //
        //    @Data
        //    public static class RANKEDTFTTURBODto {
        //        @JsonProperty("division")
        //        private String division; // 当前分段（如III、II等）
        //
        //        @JsonProperty("isProvisional")
        //        private Boolean isProvisional; // 是否处于定位赛阶段
        //
        //        @JsonProperty("leaguePoints")
        //        private Integer leaguePoints; // 段位积分（LP）
        //
        //        @JsonProperty("losses")
        //        private Integer losses; // 失败场次
        //
        //        @JsonProperty("miniSeriesProgress")
        //        private String miniSeriesProgress; // 晋级赛进度（如WLLW）
        //
        //        @JsonProperty("previousSeasonAchievedDivision")
        //        private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
        //
        //        @JsonProperty("previousSeasonAchievedTier")
        //        private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
        //
        //        @JsonProperty("previousSeasonEndDivision")
        //        private String previousSeasonEndDivision; // 上一赛季结束时的分段
        //
        //        @JsonProperty("previousSeasonEndTier")
        //        private String previousSeasonEndTier; // 上一赛季结束时的段位
        //
        //        @JsonProperty("provisionalGameThreshold")
        //        private Integer provisionalGameThreshold; // 定位赛所需总场次
        //
        //        @JsonProperty("provisionalGamesRemaining")
        //        private Integer provisionalGamesRemaining; // 剩余定位赛场次
        //
        //        @JsonProperty("queueType")
        //        private String queueType; // 队列类型（固定为RANKED_TFT_TURBO）
        //
        //        @JsonProperty("ratedRating")
        //        private Integer ratedRating; // 评级分数（隐藏分）
        //
        //        @JsonProperty("ratedTier")
        //        private String ratedTier; // 评级段位
        //
        //        @JsonProperty("tier")
        //        private String tier; // 当前段位（如GOLD、SILVER等）
        //
        //        @JsonProperty("warnings")
        //        private Object warnings; // 警告信息（无具体结构时用Object）
        //
        //        @JsonProperty("wins")
        //        private Integer wins; // 胜利场次
        //    }
    }

    //@Data
    //public static class SeasonsDto {
    //    @JsonProperty("RANKED_FLEX_SR")
    //    private RANKEDFLEXSRDto rankedFlexSr; // 灵活组排赛季信息
    //
    //    @JsonProperty("RANKED_SOLO_5x5")
    //    private RANKEDSOLO5x5Dto rankedSolo5x5; // 单双排赛季信息
    //
    //    @JsonProperty("RANKED_TFT")
    //    private RANKEDTFTDto rankedTft; // 云顶之弈单排赛季信息
    //
    //    @JsonProperty("RANKED_TFT_PAIRS")
    //    private RANKEDTFTPAIRSDto rankedTftPairs; // 云顶之弈双人赛季信息
    //
    //    @JsonProperty("RANKED_TFT_TURBO")
    //    private RANKEDTFTTURBODto rankedTftTurbo; // 云顶之弈快速赛季信息
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class RANKEDFLEXSRDto {
    //        @JsonProperty("currentSeasonEnd")
    //        private Long currentSeasonEnd; // 当前赛季结束时间（时间戳，毫秒）
    //
    //        @JsonProperty("currentSeasonId")
    //        private Integer currentSeasonId; // 当前赛季ID
    //
    //        @JsonProperty("nextSeasonStart")
    //        private Integer nextSeasonStart; // 下一赛季开始时间（时间戳，毫秒）
    //    }
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class RANKEDSOLO5x5Dto {
    //        @JsonProperty("currentSeasonEnd")
    //        private Long currentSeasonEnd; // 当前赛季结束时间（时间戳，毫秒）
    //
    //        @JsonProperty("currentSeasonId")
    //        private Integer currentSeasonId; // 当前赛季ID
    //
    //        @JsonProperty("nextSeasonStart")
    //        private Integer nextSeasonStart; // 下一赛季开始时间（时间戳，毫秒）
    //    }
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class RANKEDTFTDto {
    //        @JsonProperty("currentSeasonEnd")
    //        private Long currentSeasonEnd; // 当前赛季结束时间（时间戳，毫秒）
    //
    //        @JsonProperty("currentSeasonId")
    //        private Integer currentSeasonId; // 当前赛季ID
    //
    //        @JsonProperty("nextSeasonStart")
    //        private Integer nextSeasonStart; // 下一赛季开始时间（时间戳，毫秒）
    //    }
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class RANKEDTFTPAIRSDto {
    //        @JsonProperty("currentSeasonEnd")
    //        private Long currentSeasonEnd; // 当前赛季结束时间（时间戳，毫秒）
    //
    //        @JsonProperty("currentSeasonId")
    //        private Integer currentSeasonId; // 当前赛季ID
    //
    //        @JsonProperty("nextSeasonStart")
    //        private Integer nextSeasonStart; // 下一赛季开始时间（时间戳，毫秒）
    //    }
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class RANKEDTFTTURBODto {
    //        @JsonProperty("currentSeasonEnd")
    //        private Long currentSeasonEnd; // 当前赛季结束时间（时间戳，毫秒）
    //
    //        @JsonProperty("currentSeasonId")
    //        private Integer currentSeasonId; // 当前赛季ID
    //
    //        @JsonProperty("nextSeasonStart")
    //        private Integer nextSeasonStart; // 下一赛季开始时间（时间戳，毫秒）
    //    }
    //}

    //@NoArgsConstructor
    //@Data
    //public static class QueuesDto {
    //    @JsonProperty("division")
    //    private String division; // 当前分段（如III、II等）
    //
    //    @JsonProperty("isProvisional")
    //    private Boolean isProvisional; // 是否处于定位赛阶段
    //
    //    @JsonProperty("leaguePoints")
    //    private Integer leaguePoints; // 段位积分（LP）
    //
    //    @JsonProperty("losses")
    //    private Integer losses; // 失败场次
    //
    //    @JsonProperty("miniSeriesProgress")
    //    private String miniSeriesProgress; // 晋级赛进度（如WLLW）
    //
    //    @JsonProperty("previousSeasonAchievedDivision")
    //    private String previousSeasonAchievedDivision; // 上一赛季达到的最高分段
    //
    //    @JsonProperty("previousSeasonAchievedTier")
    //    private String previousSeasonAchievedTier; // 上一赛季达到的最高段位
    //
    //    @JsonProperty("previousSeasonEndDivision")
    //    private String previousSeasonEndDivision; // 上一赛季结束时的分段
    //
    //    @JsonProperty("previousSeasonEndTier")
    //    private String previousSeasonEndTier; // 上一赛季结束时的段位
    //
    //    @JsonProperty("provisionalGameThreshold")
    //    private Integer provisionalGameThreshold; // 定位赛所需总场次
    //
    //    @JsonProperty("provisionalGamesRemaining")
    //    private Integer provisionalGamesRemaining; // 剩余定位赛场次
    //
    //    @JsonProperty("queueType")
    //    private String queueType; // 队列类型（如RANKED_SOLO_5x5）
    //
    //    @JsonProperty("ratedRating")
    //    private Integer ratedRating; // 评级分数（隐藏分）
    //
    //    @JsonProperty("ratedTier")
    //    private String ratedTier; // 评级段位
    //
    //    @JsonProperty("tier")
    //    private String tier; // 当前段位（如GOLD、SILVER等）
    //
    //    @JsonProperty("warnings")
    //    private WarningsDto warnings; // 段位衰减警告信息
    //
    //    @JsonProperty("wins")
    //    private Integer wins; // 胜利场次
    //
    //    @NoArgsConstructor
    //    @Data
    //    public static class WarningsDto {
    //        @JsonProperty("daysUntilDecay")
    //        private Integer daysUntilDecay; // 距离段位衰减剩余天数
    //
    //        @JsonProperty("demotionWarning")
    //        private Integer demotionWarning; // 降级警告剩余场次
    //
    //        @JsonProperty("displayDecayWarning")
    //        private Boolean displayDecayWarning; // 是否显示段位衰减警告
    //
    //        @JsonProperty("timeUntilInactivityStatusChanges")
    //        private Integer timeUntilInactivityStatusChanges; // 距离 inactive 状态变更剩余时间
    //    }
    //}
}