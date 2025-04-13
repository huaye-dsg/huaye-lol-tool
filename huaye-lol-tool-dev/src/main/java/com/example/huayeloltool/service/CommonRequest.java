package com.example.huayeloltool.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

@Slf4j
@Component
public class CommonRequest {

    @Autowired
    @Qualifier(value = "unsafeOkHttpClient")
    private OkHttpClient client;

    public <T> T sendRequest(Request request, Class<T> responseClass) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            handleResponseStatus(response, request);
            ResponseBody body = response.body();
            if (body == null) {
                log.error("Request empty body. URL: {}", request.url());
                throw new IOException("Response body is empty");
            }
            String responseData = body.string();
            return JSON.parseObject(responseData, responseClass);
        }
    }

    public <T> T sendTypeRequest(Request request, TypeReference<T> type) {
        String result = "";
        for (int i = 0; i < 3; i++){
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.info("Request failed: URL= {}", request.url());
                    throw new Exception("Request failed");
                }
                ResponseBody body = response.body();
                if (body == null) {
                    log.error("Request empty body. URL: {}", request.url());
                    throw new IOException("Response body is empty");
                }
                result = body.string();
                return JSON.parseObject(result, type);
            }catch (SocketTimeoutException socketTimeoutException){
                log.error("Request timeout. URL: {}。i: {}", request.url(),i);
            } catch (Exception e) {
                log.error("Request IO error. result: {}。i: {}", result,i);
                throw new RuntimeException(e);
            }
        }
        return null;
    }


    public void handleResponseStatus(Response response, Request request) throws IOException {
        if (!response.isSuccessful()) {
            log.info("Request failed: URL= {}", request.url());
            throw new IOException("Request failed");
        }
    }

}
