package com.example.huayeloltool.common;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
public class CommonRequest {

    private static final int DEFAULT_RETRY_COUNT = 3;

    private static void logErr(HttpRequest request, HttpResponse<String> response) {
        log.warn("请求失败: URL= {}, code= {}, resp= {}",
                request.uri(), response != null ? response.statusCode() : "null", response);
    }


    public static <T> T sendSingleObjectGetRequest(String url, Class<T> responseClass) {
        HttpRequest request = HttpUtil.createGetRequest(url);
        return sendRequest(request, responseClass);
    }

    public static <T> T sendTypeGetRequest(String url, TypeReference<T> typeRef) {
        HttpRequest request = HttpUtil.createGetRequest(url);
        return sendTypeRequest(request, typeRef);
    }

    public static Boolean sendPostRequest(String url) {
        HttpRequest request = HttpUtil.createPostRequest(url);
        return sendRequestWithBoolean(request);
    }


    public static Boolean sendPatchRequest(String url, Map<String, Object> body) {
        HttpRequest request = HttpUtil.createPatchRequest(url, body);
        return sendRequestWithBoolean(request);
    }


    /**
     * 返回布尔值
     */
    private static Boolean sendRequestWithBoolean(HttpRequest request) {
        try {
            HttpResponse<String> response = HttpUtil.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
            boolean successful = response.statusCode() >= 200 && response.statusCode() < 300;
            if (successful) {
                return true;
            }
            logErr(request, response);
        } catch (Exception e) {
            // 这里会提前ban英雄导致会有问题。但是不影响使用。
//            log.error("sendRequestWithBooleanError, URL: {}", request.url(), e);
        }
        return false;
    }


    /**
     * 字符串原生返回
     */
    public static String sendRequestWithStr(HttpRequest request) {
        try {
            HttpResponse<String> response = HttpUtil.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
            // 第一步：检查响应是否成功。如果不成功，直接记录日志并返回空，不继续处理。
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logErr(request, response);
                return StringUtils.EMPTY; // 提前退出
            }

            // 第二步：获取响应体
            String body = response.body();

            // 第三步：检查响应体是否为空。如果为空，记录日志并返回空。
            if (body == null) {
                log.error("Response body is null. URL: {}", request.uri());
                return StringUtils.EMPTY; // 提前退出
            }

            // 第四步：一切正常，读取字符串并返回。
            return body;

        } catch (Exception e) {
            log.error("sendRequestError, URL: {}", request.uri(), e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * 用于解析复杂/嵌套的Java泛型类型，比如 List<Foo>、Map<String, Bar>、List<Result<Foo>> 等
     */
    private static <T> T sendTypeRequest(HttpRequest request, TypeReference<T> typeRef) {
        try {
            String responseData = sendRequestWithStr(request);
            if (StringUtils.isNotEmpty(responseData)) {
                return JSON.parseObject(responseData, typeRef);
            }
        } catch (Exception e) {
            log.error("sendTypeRequestError, URL: {}", request.uri(), e);
        }
        return null;
    }

    /**
     * 用于解析 非泛型或简单对象类型，比如 Foo.class、Bar.class。
     */
    private static <T> T sendRequest(HttpRequest request, Class<T> responseClass) {
        String responseData = sendRequestWithStr(request);
        return JSON.parseObject(responseData, responseClass);
    }


}