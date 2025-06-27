package com.example.huayeloltool.common;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Slf4j
public class CommonRequest {

    private static final OkHttpClient CLIENT = OkHttpUtil.getInstance();

    private static final int DEFAULT_RETRY_COUNT = 3;

    private static void logErr(Request request, Response response) {
        log.warn("请求失败: URL= {}, code= {}, resp= {}",
                request.url(), response != null ? response.code() : "null", response);
    }


    public static <T> T sendSingleObjectGetRequest(String url, Class<T> responseClass) {
        Request request = OkHttpUtil.createOkHttpGetRequest(url);
        return sendRequest(request, responseClass);
    }

    public static <T> T sendTypeGetRequest(String url) {
        Request request = OkHttpUtil.createOkHttpGetRequest(url);
        return sendTypeRequest(request, new TypeReference<>() {
        });
    }

    public static Boolean sendPostRequest(String url) {
        Request request = OkHttpUtil.createOkHttpPostRequest(url);
        return sendRequestWithBoolean(request);
    }


    public static Boolean sendPatchRequest(String url, Map<String, Object> body) {
        Request request = OkHttpUtil.createOkHttpPatchRequest(url, body);
        return sendRequestWithBoolean(request);
    }


    /**
     * 返回布尔值
     */
    @SneakyThrows
    private static Boolean sendRequestWithBoolean(Request request) {
        try (Response response = CLIENT.newCall(request).execute()) {
            boolean successful = response.isSuccessful();
            if (successful) {
                return true;
            }
            logErr(request, response);
            return false;
        }
    }


    /**
     * 字符串原生返回
     */
    public static String sendRequestWithStr(Request request) {
        try {
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logErr(request, response);
                }
                if (response.body() == null) {
                    log.error("Response body is empty. URL: {}", request.url());
                }
                return response.body().string();
            }
        } catch (Exception e) {
            log.error("sendRequestError, URL: {}", request.url(), e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * 用于解析复杂/嵌套的Java泛型类型，比如 List<Foo>、Map<String, Bar>、List<Result<Foo>> 等
     */
    private static <T> T sendTypeRequest(Request request, TypeReference<T> typeRef) {
        for (int i = 0; i < DEFAULT_RETRY_COUNT; i++) {
            try {
                String responseData = sendRequestWithStr(request);
                return JSON.parseObject(responseData, typeRef);
            } catch (Exception e) {
                log.error("sendTypeRequestError, URL: {}, retry: {}/{}", request.url(), i + 1, DEFAULT_RETRY_COUNT, e);
            }
        }
        log.error("sendTypeRequest failed after {} retries, URL: {}", DEFAULT_RETRY_COUNT, request.url());
        return null;
    }

    /**
     * 用于解析 非泛型或简单对象类型，比如 Foo.class、Bar.class。
     */
    private static <T> T sendRequest(Request request, Class<T> responseClass) {
        String responseData = sendRequestWithStr(request);
        return JSON.parseObject(responseData, responseClass);
    }


}
