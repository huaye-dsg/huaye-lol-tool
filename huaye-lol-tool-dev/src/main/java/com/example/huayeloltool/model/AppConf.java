package com.example.huayeloltool.model;

import lombok.Data;

@Data
public class AppConf {
    /**
     * 模式 (例如: prod, dev)
     */
    private String mode;

    /**
     * Sentry 配置
     */
    private SentryConf sentry;

    /**
     * PProf 配置
     */
    private PProfConf pprof;

    /**
     * 日志配置
     */
    private String log;

    /**
     * Buff API 配置
     */
    private BuffApi buffApi;

    /**
     * 评分计算配置
     */
    private CalcScoreConf calcScore;
}
