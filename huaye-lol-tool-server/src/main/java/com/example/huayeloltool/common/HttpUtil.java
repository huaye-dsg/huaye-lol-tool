package com.example.huayeloltool.common;

import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.model.base.BaseUrlClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpUtil {

    // 单例 HttpClient（禁用 SSL 校验，配置连接池和超时）
    private static volatile HttpClient instance;

    public static HttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpUtil.class) {
                if (instance == null) {
                    try {
                        // 1. 禁用 SSL 证书校验（与你的 OkHttp 逻辑一致）
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                            @Override
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        }}, new java.security.SecureRandom());

                        // 2. 构建 HttpClient（配置连接池、超时、虚拟线程适配）
                        instance = HttpClient.newBuilder()
                                .sslContext(sslContext)
                                .connectTimeout(Duration.ofSeconds(10)) // 连接超时（对应 OkHttp）
                                .executor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) // 虚拟线程（提升并发）
                                .build();
                    } catch (NoSuchAlgorithmException | KeyManagementException e) {
                        throw new RuntimeException("创建 HttpClient 失败", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 获取基础认证头信息
     * 对应 OkHttpUtil 中的 Basic Auth 实现
     */
    private static String getAuthHeader() {
        String auth = "riot:" + BaseUrlClient.getInstance().getToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 GET 请求
     * 对应 OkHttpUtil.createOkHttpGetRequest 方法
     * 
     * @param uri 请求路径
     * @return 构造好的 HttpRequest 对象
     */
    public static HttpRequest createGetRequest(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BaseUrlClient.assembleUrl(uri)))
                .header("User-Agent", "Mozilla/5.0")
                .header("Authorization", getAuthHeader())
                .timeout(Duration.ofSeconds(30)) // 读取超时（对应 OkHttp 的 readTimeout）
                .GET()
                .build();
    }

    /**
     * 创建 POST 请求
     * 对应 OkHttpUtil.createOkHttpPostRequest 方法
     * 
     * @param uri 请求路径
     * @return 构造好的 HttpRequest 对象
     */
    public static HttpRequest createPostRequest(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BaseUrlClient.assembleUrl(uri)))
                .header("User-Agent", "Mozilla/5.0")
                .header("Authorization", getAuthHeader())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10)) // 写入超时（对应 OkHttp 的 writeTimeout）
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
    }

    /**
     * 创建 PATCH 请求
     * 对应 OkHttpUtil.createOkHttpPatchRequest 方法
     * 
     * @param uri 请求路径
     * @param body 请求体参数
     * @return 构造好的 HttpRequest 对象
     */
    public static HttpRequest createPatchRequest(String uri, Map<String, Object> body) {
        String jsonBody = JSON.toJSONString(body);
        return HttpRequest.newBuilder()
                .uri(URI.create(BaseUrlClient.assembleUrl(uri)))
                .header("Authorization", getAuthHeader())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10)) // 写入超时（对应 OkHttp 的 writeTimeout）
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    /**
     * 发送带有重试机制的GET请求
     * 对应OkHttp中通过RetryInterceptor实现的重试机制
     *
     * @param uri 请求路径
     * @param maxRetries 最大重试次数
     * @return HttpResponse 响应对象
     * @throws Exception 请求异常
     */
    public static HttpResponse<String> sendGetRequestWithRetry(String uri, int maxRetries) throws Exception {
        HttpRequest request = createGetRequest(uri);
        return sendRequestWithRetry(request, maxRetries);
    }

    /**
     * 发送带有重试机制的POST请求
     * 对应OkHttp中通过RetryInterceptor实现的重试机制
     *
     * @param uri 请求路径
     * @param maxRetries 最大重试次数
     * @return HttpResponse 响应对象
     * @throws Exception 请求异常
     */
    public static HttpResponse<String> sendPostRequestWithRetry(String uri, int maxRetries) throws Exception {
        HttpRequest request = createPostRequest(uri);
        return sendRequestWithRetry(request, maxRetries);
    }

    /**
     * 发送带有重试机制的PATCH请求
     * 对应OkHttp中通过RetryInterceptor实现的重试机制
     *
     * @param uri 请求路径
     * @param body 请求体
     * @param maxRetries 最大重试次数
     * @return HttpResponse 响应对象
     * @throws Exception 请求异常
     */
    public static HttpResponse<String> sendPatchRequestWithRetry(String uri, Map<String, Object> body, int maxRetries) throws Exception {
        HttpRequest request = createPatchRequest(uri, body);
        return sendRequestWithRetry(request, maxRetries);
    }

    /**
     * 发送带有重试机制的通用请求方法
     * 模拟OkHttp中RetryInterceptor的实现逻辑
     *
     * @param request 请求对象
     * @param maxRetries 最大重试次数
     * @return HttpResponse 响应对象
     * @throws Exception 请求异常
     */
    private static HttpResponse<String> sendRequestWithRetry(HttpRequest request, int maxRetries) throws Exception {
        Exception lastException = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 发送请求
                HttpResponse<String> response = getInstance().send(request, HttpResponse.BodyHandlers.ofString());
                
                // 如果请求成功，直接返回结果
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response;
                }
                
                // 对于4xx客户端错误，不进行重试，直接返回
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    return response;
                }
                
                // 对于其他情况（如5xx服务端错误），进行重试
                lastException = new Exception("HTTP Error: " + response.statusCode());
                
            } catch (Exception e) {
                lastException = e;
            }
            
            // 如果需要重试，则等待一段时间后重试
            if (i < maxRetries - 1) {
                try {
                    long waitTime = (long) Math.pow(2, i) * 100; // 指数退避策略
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("重试被中断", ie);
                }
            }
        }
        
        // 所有重试都失败后，抛出最后一个异常
        throw new Exception("请求重试失败，已达到最大重试次数: " + maxRetries, lastException);
    }
}