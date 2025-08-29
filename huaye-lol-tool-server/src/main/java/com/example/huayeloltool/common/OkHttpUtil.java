package com.example.huayeloltool.common;


import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.model.base.BaseUrlClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpUtil {

    public static Request createOkHttpGetRequest(String uri) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getToken()).getBytes());
        String URL = BaseUrlClient.assembleUrl(uri);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Authorization", "Basic " + auth)
                .url(URL)
                .build();
    }

    public static Request createOkHttpPostRequest(String uri) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getToken()).getBytes());
        String URL = BaseUrlClient.assembleUrl(uri);
        return new Request.Builder()
                .url(URL)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Authorization", "Basic " + auth)
                .post(RequestBody.create("{}", MediaType.parse("application/json")))
                .build();
    }

    public static Request createOkHttpPatchRequest(String uri, Map<String, Object> body) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getToken()).getBytes());
        return new Request.Builder()
                .url(BaseUrlClient.assembleUrl(uri))
                .addHeader("Authorization", "Basic " + auth)
                .patch(RequestBody.create(
                        JSON.toJSONString(body),
                        MediaType.get("application/json")
                ))
                .build();
    }

    private static volatile OkHttpClient instance;

    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (OkHttpUtil.class) {
                if (instance == null) {
                    instance = createOptimizedOkHttpClient();
                }
            }
        }
        return instance;
    }

    /**
     * 创建优化的OkHttpClient实例
     * 包含连接池配置、重试机制和合理的超时设置
     */
    private static OkHttpClient createOptimizedOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    // 优化：增加超时时间，提供更好的稳定性
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    // 优化：添加连接池配置
                    .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                    // 优化：启用重试机制
                    .retryOnConnectionFailure(true)
                    // 优化：添加拦截器用于请求重试和日志
                    .addInterceptor(new RetryInterceptor())
                    .build();
        } catch (Exception e) {
            log.error("创建OkHttpClient时发生错误", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 重试拦截器
     * 为关键请求提供自动重试机制
     */
    /**
     * 重试拦截器
     * 为关键请求提供自动重试机制
     */
    private static class RetryInterceptor implements Interceptor {
        private static final int MAX_RETRY_COUNT = 3;

        /**
         * 【关键修复】检查请求是否为WebSocket升级请求
         */
        private boolean isWebSocketUpgrade(Request request) {
            String connectionHeader = request.header("Connection");
            String upgradeHeader = request.header("Upgrade");
            return "Upgrade".equalsIgnoreCase(connectionHeader) && "websocket".equalsIgnoreCase(upgradeHeader);
        }

        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request request = chain.request();

            // 【关键修复】如果这是一个WebSocket请求，则不应用任何重试逻辑，直接继续执行。
            // 让 OkHttp 的原生 WebSocket 机制来处理它。
            if (isWebSocketUpgrade(request)) {
                return chain.proceed(request);
            }

            // --- 以下是您原有的、用于普通HTTP请求的重试逻辑，它本身是正确的 ---
            Response response = null;
            java.io.IOException lastException = null;

            for (int i = 0; i < MAX_RETRY_COUNT; i++) {
                try {
                    Request requestToUse = request;
                    if (i > 0 && request.body() != null) {
                        requestToUse = request.newBuilder().build();
                    }

                    response = chain.proceed(requestToUse);

                    if (response.isSuccessful()) {
                        return response;
                    }

                    if (response.code() >= 400 && response.code() < 500) {
                        // 对于普通HTTP请求，返回4xx响应是正确的，因为调用者会处理它
                        return response;
                    }

                    log.debug("请求失败，状态码: {}，准备重试 ({}/{})", response.code(), i + 1, MAX_RETRY_COUNT);

                    closeResponseSafely(response);
                    response = null;

                } catch (java.io.IOException e) {
                    lastException = e;
                    log.debug("请求发生IO异常，准备重试 ({}/{}): {}", i + 1, MAX_RETRY_COUNT, e.getMessage());
                    closeResponseSafely(response);
                    response = null;
                    if (i == MAX_RETRY_COUNT - 1) {
                        throw e;
                    }
                } catch (Exception e) {
                    closeResponseSafely(response);
                    throw new java.io.IOException("请求执行失败", e);
                }

                if (i < MAX_RETRY_COUNT - 1) {
                    try {
                        long waitTime = (long) Math.pow(2, i) * 100;
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        closeResponseSafely(response);
                        throw new java.io.IOException("重试被中断", ie);
                    }
                }
            }

            if (lastException != null) {
                throw lastException;
            }

            throw new java.io.IOException("请求重试失败，未知错误");
        }

        private void closeResponseSafely(Response response) {
            // ... (这个方法保持不变) ...
        }
    }
}