package com.example.huayeloltool.model;

import com.example.huayeloltool.enums.ScoreOption;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// 分数与原因组合类
@Data
public class ScoreWithReason {
    private double score;
    private List<IncScoreReason> reasons;

    // 静态工厂方法替代Go的NewScoreWithReason
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

    // 获取当前分数
    public double value() {
        return score;
    }

    // 将原因转换为字符串
    public String reasonsToString() {
        StringBuilder sb = new StringBuilder();
        for (IncScoreReason reason : reasons) {
            sb.append(reason.getReason().getDisplayName())
                    .append(String.format("%.2f", reason.getIncVal()))
                    .append(",");
        }
        return sb.toString();
    }
}
