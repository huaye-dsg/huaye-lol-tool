package com.example.huayeloltool.model.request;

import lombok.Data;

@Data
public class BanChampionRequest {
    private Boolean autoBanChamp;
    private Integer championId;
}