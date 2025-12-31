package com.example.huayeloltool.common;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * HTTP 请求工具类
 * 
 * <p>封装了与 LOL 客户端 LCU API 通信的 HTTP 请求方法。
 * 所有方法均为静态方法，通过 {@link HttpUtil} 发送请求。
 * 
 * <p>使用方式：
 * <pre>
 * // 直接通过类名调用静态方法
 * Summoner summoner = LcuHttpClient.get("/lol-summoner/v1/current-summoner", Summoner.class);
 * List<Conversation> list = LcuHttpClient.get("/lol-chat/v1/conversations", new TypeReference<>() {});
 * Boolean success = LcuHttpClient.post("/lol-matchmaking/v1/ready-check/accept");
 * </pre>
 * 
 * <p>设计说明：
 * 这是一个纯工具类，不应被继承。所有方法都是静态的，
 * 私有构造函数防止实例化。
 */
@Slf4j
public final class LcuHttpClient {

    /**
     * 私有构造函数，防止实例化
     */
    private LcuHttpClient() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 记录请求失败日志
     */
    private static void logErr(HttpRequest request, HttpResponse<String> response) {
        log.warn("请求失败: URL={}, code={}, resp={}",
                request.uri(), response != null ? response.statusCode() : "null", response);
    }

    /**
     * 发送 GET 请求，返回单个对象
     * 
     * @param url           请求路径（相对路径，如 "/lol-summoner/v1/current-summoner"）
     * @param responseClass 响应对象的 Class 类型
     * @param <T>           响应对象类型
     * @return 解析后的响应对象，失败返回 null
     */
    public static <T> T get(String url, Class<T> responseClass) {
        HttpRequest request = HttpUtil.createGetRequest(url);
        return sendRequest(request, responseClass);
    }

    /**
     * 发送 GET 请求，返回泛型对象（如 List、Map 等）
     * 
     * <p>用于解析复杂/嵌套的 Java 泛型类型，如：
     * <pre>
     * List<Summoner> list = LcuHttpClient.get(url, new TypeReference<List<Summoner>>() {});
     * Map<String, Object> map = LcuHttpClient.get(url, new TypeReference<Map<String, Object>>() {});
     * </pre>
     * 
     * @param url     请求路径
     * @param typeRef 泛型类型引用
     * @param <T>     响应对象类型
     * @return 解析后的响应对象，失败返回 null
     */
    public static <T> T get(String url, TypeReference<T> typeRef) {
        HttpRequest request = HttpUtil.createGetRequest(url);
        return sendTypeRequest(request, typeRef);
    }

    /**
     * 发送 POST 请求（无请求体）
     * 
     * @param url 请求路径
     * @return 请求是否成功（HTTP 2xx 返回 true）
     */
    public static Boolean post(String url) {
        HttpRequest request = HttpUtil.createPostRequest(url);
        return sendRequestWithBoolean(request);
    }

    /**
     * 发送 PATCH 请求
     * 
     * @param url  请求路径
     * @param body 请求体参数
     * @return 请求是否成功（HTTP 2xx 返回 true）
     */
    public static Boolean patch(String url, Map<String, Object> body) {
        HttpRequest request = HttpUtil.createPatchRequest(url, body);
        return sendRequestWithBoolean(request);
    }

    /**
     * 发送请求并返回布尔结果
     * 
     * @param request HTTP 请求对象
     * @return HTTP 2xx 返回 true，否则返回 false
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
            // 某些情况下（如提前 ban 英雄）可能会失败，但不影响后续流程
            log.debug("请求失败: URL={}, error={}", request.uri(), e.getMessage());
        }
        return false;
    }

    /**
     * 发送请求并返回原始字符串响应
     * 
     * @param request HTTP 请求对象
     * @return 响应体字符串，失败返回空字符串
     */
    private static String sendRequestWithStr(HttpRequest request) {
        try {
            HttpResponse<String> response = HttpUtil.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
            
            // 检查响应状态码
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logErr(request, response);
                return StringUtils.EMPTY;
            }

            // 检查响应体
            String body = response.body();
            if (body == null) {
                log.error("响应体为空: URL={}", request.uri());
                return StringUtils.EMPTY;
            }

            return body;

        } catch (Exception e) {
            log.error("请求异常: URL={}", request.uri(), e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * 发送请求并解析为泛型对象
     */
    private static <T> T sendTypeRequest(HttpRequest request, TypeReference<T> typeRef) {
        try {
            String responseData = sendRequestWithStr(request);
            if (StringUtils.isNotEmpty(responseData)) {
                return JSON.parseObject(responseData, typeRef);
            }
        } catch (Exception e) {
            log.error("解析响应失败: URL={}", request.uri(), e);
        }
        return null;
    }

    /**
     * 发送请求并解析为指定类型对象
     */
    private static <T> T sendRequest(HttpRequest request, Class<T> responseClass) {
        String responseData = sendRequestWithStr(request);
        return JSON.parseObject(responseData, responseClass);
    }
}