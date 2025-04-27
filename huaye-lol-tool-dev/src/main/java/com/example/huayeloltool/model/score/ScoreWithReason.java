package com.example.huayeloltool.model.score;

import com.example.huayeloltool.enums.ScoreOption;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 分数与原因组合类
@Data
public class ScoreWithReason {
    private double score;
    private List<IncScoreReason> reasons;

    private Map<GameScoreCalculator1.ScoreOption, ScoreDetail> reasons2 = new HashMap<>(); // 使用Map存储详细原因


    public static ScoreWithReason create(double initialScore) {
        return new ScoreWithReason(initialScore);
    }

    public ScoreWithReason(double initialScore) {
        this.score = initialScore;
        this.reasons = new ArrayList<>(5); // 初始容量5
    }

    public void addReason(String description, double value, GameScoreCalculator1.ScoreOption option) {
        // 可以记录更详细的信息，例如基础值、增减值、权重等
        reasons2.put(option, new ScoreDetail(description, value));
    }

    // 重载，允许添加非数值型原因，如角色
    public void addReason(String description, String value, GameScoreCalculator1.ScoreOption option) {
        reasons2.put(option, new ScoreDetail(description, value));
    }

    // 添加分数变化
    public void add(double incVal, ScoreOption reason) {
        this.score += incVal;
        this.reasons.add(new IncScoreReason(reason, incVal));
    }


    @Data
    public static class ScoreDetail {
        private String description; // 描述
        private Object value;       // 得分值或其他信息

        public ScoreDetail(String description, Object value) {
            this.description = description;
            this.value = value;
        }
    }

    // 获取当前分数
    public double value() {
        return score;
    }

}
