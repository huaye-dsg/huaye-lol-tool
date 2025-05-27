package com.example.huayeloltool.common;

import lombok.Data;

@Data
public class CommonResp {
    private String errorCode;
    private Integer httpStatus;
    private String message;
}
