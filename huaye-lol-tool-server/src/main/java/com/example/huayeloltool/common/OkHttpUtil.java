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
//                    .retryOnConnectionFailure(true)
                    // 优化：添加拦截器用于请求重试和日志
//                    .addInterceptor(new RetryInterceptor())
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
    private static class RetryInterceptor implements Interceptor {
        private static final int MAX_RETRY_COUNT = 3;

        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request request = chain.request();
            Response response = null;
            Exception lastException = null;

            for (int i = 0; i < MAX_RETRY_COUNT; i++) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }

                    // 如果是客户端错误（4xx），不重试
                    if (response.code() >= 400 && response.code() < 500) {
                        return response;
                    }

                    // 关闭响应体以释放资源
                    if (response.body() != null) {
                        response.body().close();
                    }

                } catch (Exception e) {
                    lastException = e;
                    log.debug("请求失败，尝试重试 ({}/{}): {}", i + 1, MAX_RETRY_COUNT, e.getMessage());

                    if (i == MAX_RETRY_COUNT - 1) {
                        // 最后一次重试失败，抛出异常
                        if (e instanceof java.io.IOException) {
                            throw (java.io.IOException) e;
                        } else {
                            throw new java.io.IOException("请求重试失败", e);
                        }
                    }

                    // 重试前短暂等待
                    try {
                        Thread.sleep(100 * (i + 1)); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new java.io.IOException("重试被中断", ie);
                    }
                }
            }

            return response;
        }
    }
}