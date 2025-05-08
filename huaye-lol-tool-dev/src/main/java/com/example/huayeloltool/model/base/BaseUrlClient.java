package com.example.huayeloltool.model.base;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class BaseUrlClient {

    /**
     * 端口号，用于连接到服务器的端口。
     */
    private int port;

    /**
     * 认证密码，用于验证访问权限。
     */
    private String token;

    /**
     * 基础URL地址，所有请求的基础路径。
     */
    private String baseUrl;

    /**
     * 单例
     */
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
        return String.format("https://riot:%s@127.0.0.1:%d", token, port);
    }

}
