package com.example.huayeloltool.common;


import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.model.base.BaseUrlClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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
                    instance = unsafeOkHttpClient();
                }
            }
        }
        return instance;
    }


    private static OkHttpClient unsafeOkHttpClient() {
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
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            log.error("unsafeOkHttpClient error", e);
            throw new RuntimeException(e);
        }
    }


}
