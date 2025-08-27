package com.example.huayeloltool.model.api;

import lombok.Data;

/**
 * LCU API 公共响应。禁止改动。
 */
@Data
public class LcuApiCommonResult {
    private String errorCode;
    private Integer httpStatus;
    private String message;
}
