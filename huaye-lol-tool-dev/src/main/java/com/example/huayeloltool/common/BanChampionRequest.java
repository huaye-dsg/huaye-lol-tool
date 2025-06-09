package com.example.huayeloltool.common;

import lombok.Data;

@Data
public class BanChampionRequest {
    private Boolean autoBanChamp;
    private Integer championId;
}