package com.example.huayeloltool.common;


import com.alibaba.fastjson2.JSON;
import com.example.huayeloltool.model.base.BaseUrlClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.Base64;
import java.util.Map;

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
}
