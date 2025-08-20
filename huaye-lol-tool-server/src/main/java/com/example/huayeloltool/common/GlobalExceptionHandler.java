package com.example.huayeloltool.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResponse<?> handleBusinessException(BusinessException e, WebRequest request) {
        log.warn("业务异常: {}", e.getMessage());
        CommonResponse<?> resp = new CommonResponse<>();
        resp.setCode(e.getCode());
        resp.setMessage(e.getMessage());
        return resp;
    }


    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public CommonResponse<?> handleException(Exception e, WebRequest request) {
        log.error("系统异常: ", e);
        CommonResponse<?> resp = new CommonResponse<>();
        resp.setCode(500);
        resp.setMessage(e.getMessage());
        return resp;
    }
}