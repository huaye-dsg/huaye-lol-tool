package com.example.huayeloltool.model;

import com.example.huayeloltool.enums.ScoreOption;
import lombok.Data;


@Data
public class IncScoreReason {
    private final ScoreOption reason;
    private final double incVal;

    public IncScoreReason(ScoreOption reason, double incVal) {
        this.reason = reason;
        this.incVal = incVal;
    }

    public ScoreOption getReason() {
        return reason;
    }

    public double getIncVal() {
        return incVal;
    }
}
