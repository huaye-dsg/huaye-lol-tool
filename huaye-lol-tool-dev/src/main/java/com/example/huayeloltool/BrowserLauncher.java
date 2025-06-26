package com.example.huayeloltool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.huayeloltool.model.api.TencentHeroInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;

@Slf4j // 使用 Slf4j 注解来获得 logger
public class BrowserLauncher {

    @SneakyThrows
    public static void main(String[] args) {
        String url = "http://game.gtimg.cn/images/lol/act/img/js/heroList/hero_list.js";

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject jsonObject = JSON.parseObject(body);
                List<TencentHeroInfo> tencentHeroInfoList = jsonObject.getList("hero", TencentHeroInfo.class);
                for (TencentHeroInfo heroInfo : tencentHeroInfoList) {
                    System.out.println(heroInfo.getHeroId() + " " + heroInfo.getName() + " " + heroInfo.getAlias() + " " + heroInfo.getTitle());
                }
            } else {
                System.err.println("请求失败：" + response.code());
            }
        }
    }
}