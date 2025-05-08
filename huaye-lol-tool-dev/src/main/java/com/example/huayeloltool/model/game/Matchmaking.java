package com.example.huayeloltool.model.game;


import lombok.Data;

@SuppressWarnings({"DanglingJavadoc", "CommentedOutCode"})
@Data
public class Matchmaking {

    /*
      躲避数据，记录是否存在逃避（Dodge）的情况
      例如：dodgerId 为 0 表示当前没有人因逃避而被标记。
     */
//    private DodgeData dodgeData;

    /*
      错误列表，通常为空数组。
      如果在匹配过程中出现问题，此列表可能会包含错误信息。
     */
//    private List<Object> errors;

    /*
      预估的排队时间（单位：秒或毫秒，根据实际情况）。
      示例中：163.55300903320313 表示预估队列等待时长。
     */
//    private double estimatedQueueTime;

    /**
     * 是否当前处于队列中。
     */
    private Boolean isCurrentlyInQueue;

    /*
      大厅ID，当前匹配大厅的标识。
      如果为空字符串，表示还未进入大厅。
     */
//    private String lobbyId;

    /*
      低优先级数据信息，通常用于记录离队惩罚等数据。
     */
//    private LowPriorityData lowPriorityData;

    /**
     * 队列ID，标识当前匹配队列的类型，如 420 表示排位赛等。
     */
    private Integer queueId;

    /*
      准备确认数据，用于 Ready Check 阶段的数据（如是否有拒绝响应等）。
     */
//    private ReadyCheck readyCheck;

    /*
      搜索状态，标识当前匹配的状态，例如 "Searching" 表示正在搜索对局。
     */
//    private String searchState;

    /*
      在队列中的时间，表示当前已经等待的时长。
     */
//    private double timeInQueue;


    // ========== 内部子类 ==========

    /**
     * 表示躲避（Dodge）信息的实体类。
     */
    //@Data
    //public static class DodgeData {
    //    /**
    //     * 躲避者的 ID，如果为 0 表示当前无躲避行为。
    //     */
    //    private int dodgerId;
    //    /**
    //     * 躲避状态，例如 "Invalid" 表示没有进行有效的逃避。
    //     */
    //    private String state;
    //}

    /**
     * 表示低优先级数据，通常用于记录离队惩罚、逃跑等信息。
     */
    //@Data
    //public static class LowPriorityData {
    //    /**
    //     * 离队时的访问令牌，可能为空。
    //     */
    //    private String bustedLeaverAccessToken;
    //    /**
    //     * 被惩罚的召唤师 ID 列表（通常为空）。
    //     */
    //    private List<Object> penalizedSummonerIds;
    //    /**
    //     * 惩罚时间，表示离队等违规行为的累计惩罚时长。
    //     */
    //    private double penaltyTime;
    //    /**
    //     * 剩余惩罚时间。
    //     */
    //    private double penaltyTimeRemaining;
    //}

    /**
     * 表示 Ready Check（准备确认）数据的实体类，
     * 用于在匹配开始前确认玩家是否响应等状态。
     */
    //public static class ReadyCheck {
    //    /**
    //     * 拒绝者的 ID 列表（可能为空，表示无人拒绝）。
    //     */
    //    private List<Object> declinerIds;
    //    /**
    //     * 躲避警告状态，例如 "None" 表示没有警告。
    //     */
    //    private String dodgeWarning;
    //    /**
    //     * 玩家响应状态，例如 "None" 表示尚未响应。
    //     */
    //    private String playerResponse;
    //    /**
    //     * 当前 Ready Check 的状态，例如 "Invalid"。
    //     */
    //    private String state;
    //    /**
    //     * Ready Check 倒计时，表示剩余响应时间。
    //     */
    //    private double timer;
    //
    //}
}
