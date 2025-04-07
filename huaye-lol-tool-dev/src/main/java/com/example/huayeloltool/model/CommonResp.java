package com.example.huayeloltool.model;

import lombok.Data;

@Data
public class CommonResp {
    private String errorCode;
    private Integer httpStatus;
    private String message;
}
