package com.example.huayeloltool.model.score;

import com.example.huayeloltool.enums.ScoreOption;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// 分数与原因组合类
@Data
public class ScoreWithReason {
    private double score;
    private List<IncScoreReason> reasons;

    public static ScoreWithReason create(double initialScore) {
        return new ScoreWithReason(initialScore);
    }

    public ScoreWithReason(double initialScore) {
        this.score = initialScore;
        this.reasons = new ArrayList<>(5); // 初始容量5
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
