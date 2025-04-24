package com.example.huayeloltool.model.score;

import com.example.huayeloltool.enums.ScoreOption;
import lombok.Data;
import lombok.Getter;


@Getter
@Data
public class IncScoreReason {
    private final ScoreOption reason;
    private final double incVal;

    public IncScoreReason(ScoreOption reason, double incVal) {
        this.reason = reason;
        this.incVal = incVal;
    }

}
