package com.example.huayeloltool.model.score;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateItemConf {
    /**
     * 比率限制 (例如: >30%)
     */
    private double limit;

    /**
     * 分数配置，格式为 [ [最低人头限制, 加分数] ]
     */
    private double[][] scoreConf;
}
