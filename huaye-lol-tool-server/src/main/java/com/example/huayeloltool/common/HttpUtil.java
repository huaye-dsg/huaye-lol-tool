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
}