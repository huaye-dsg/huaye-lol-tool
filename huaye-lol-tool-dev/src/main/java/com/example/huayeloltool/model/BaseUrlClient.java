package com.example.huayeloltool.model;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
public class BaseUrlClient {

    @Setter
    private int port;
    @Setter
    private String authPwd;
    private String baseUrl;


    private static BaseUrlClient instance;


    public static synchronized BaseUrlClient getInstance() {
        if (instance == null) {
            instance = new BaseUrlClient();
        }
        return instance;
    }

    public static String assembleUrl(String uri) {
        return instance.fmtClientApiUrl() + uri;
    }


    private BaseUrlClient() {
    }


    /**
     * 格式化客户端 API URL
     *
     * @return 格式化后的 API URL
     */
    private String fmtClientApiUrl() {
        return String.format("https://riot:%s@127.0.0.1:%d", authPwd, port);
    }

}
