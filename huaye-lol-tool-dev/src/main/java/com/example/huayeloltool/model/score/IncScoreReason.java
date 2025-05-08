package com.example.huayeloltool.model.score;

import com.example.huayeloltool.enums.ScoreOption;
import lombok.Data;
import lombok.Getter;


public record IncScoreReason(ScoreOption reason, double incVal) {

}
