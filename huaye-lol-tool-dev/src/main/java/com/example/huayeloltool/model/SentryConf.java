package com.example.huayeloltool.model;

import lombok.Data;


@Data
public class SentryConf {
    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * DSN (数据源名称)
     */
    private String dsn;
}
