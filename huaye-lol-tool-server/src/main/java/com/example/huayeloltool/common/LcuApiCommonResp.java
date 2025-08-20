package com.example.huayeloltool.common;

import lombok.Data;

/**
 * LCU API 响应。禁止改动。
 */
@Data
public class LcuApiCommonResp {
    private String errorCode;
    private Integer httpStatus;
    private String message;
}
