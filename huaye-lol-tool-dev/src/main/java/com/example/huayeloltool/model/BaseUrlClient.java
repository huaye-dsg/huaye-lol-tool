package com.example.huayeloltool.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
public class BaseUrlClient {

    private int port;
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

    public BaseUrlClient getCurrSummoner() {
        return instance;
    }

    public void setCurrSummoner(BaseUrlClient currSummoner) {
        instance = currSummoner;
    }

    private BaseUrlClient() {
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setAuthPwd(String authPwd) {
        this.authPwd = authPwd;
    }
    private void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    /**
     * 格式化客户端 API URL
     *
     * @return 格式化后的 API URL
     */
    private String fmtClientApiUrl() {
//        return String.format("https://riot:%s@127.0.0.1:%d", authPwd, port);
        return String.format("https://riot:%s@127.0.0.1:%d", authPwd, port);
    }

}
