package com.example.huayeloltool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@Slf4j // 使用 Slf4j 注解来获得 logger
@Component
public class BrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // --- 核心改动：检查当前环境是否是无头模式 ---
//        String url = "http://localhost:" + 5173;
//        if (GraphicsEnvironment.isHeadless()) {
//            log.info("当前为无头环境，无法自动打开浏览器。请手动访问: {}", url);
//            return; // 直接退出，不执行后续代码
//        }
//
//
//        // 再次确认 Desktop 是否支持
//        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
//            try {
//                Desktop.getDesktop().browse(new URI(url));
//                log.info("应用程序已启动，已自动在浏览器中打开: {}", url);
//            } catch (Exception e) {
//                // 捕获所有可能的异常，比如URISyntaxException, IOException
//                log.error("自动打开浏览器时发生错误。请手动访问: {}", url, e);
//            }
//        } else {
//            log.warn("当前环境不支持自动打开浏览器。请手动访问: {}", url);
//        }
    }
}