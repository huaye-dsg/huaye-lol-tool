package com.example.huayeloltool.model;

import lombok.Data;

@Data
public class BuffApi {
    /**
     * API 地址
     */
    private String url;

    /**
     * 请求超时时间 (秒)
     */
    private int timeout;
}
