package com.example.huayeloltool.config;


import com.alibaba.fastjson.JSON;
import com.example.huayeloltool.model.BaseUrlClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.Base64;
import java.util.Map;

@Slf4j
public class OkHttpUtil {
    public static Request createOkHttpGetRequest(String uri) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getAuthPwd()).getBytes());
        String URL = BaseUrlClient.assembleUrl(uri);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Authorization", "Basic " + auth)
                .url(URL)
                .build();
    }

    public static Request createOkHttpPostRequest(String uri) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getAuthPwd()).getBytes());
        String URL = BaseUrlClient.assembleUrl(uri);
        return new Request.Builder()
                .url(URL)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Authorization", "Basic " + auth)
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .build();
    }

    public static Request createOkHttpPatchRequest(String uri, Map<String, Object> body) {
        String auth = Base64.getEncoder().encodeToString(("riot:" + BaseUrlClient.getInstance().getAuthPwd()).getBytes());
        return new Request.Builder()
                .url(BaseUrlClient.assembleUrl(uri))
                .addHeader("Authorization", "Basic " + auth)
                .patch(RequestBody.create(
                        JSON.toJSONString(body),
                        MediaType.get("application/json")
                ))
                .build();
    }
}
