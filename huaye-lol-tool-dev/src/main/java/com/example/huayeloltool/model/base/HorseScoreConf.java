package com.example.huayeloltool.model.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class HorseScoreConf {
    /**
     * 分数
     */
    private double score;

    /**
     * 名称
     */
    private String name;
}
