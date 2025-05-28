package com.example.huayeloltool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.apache.coyote.http11.Constants.a;

/**
 * DESCRIPTION
 *
 * @author zhangshuai
 * @since 2025/5/28
 */
@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        new Thread(() -> {
            try {
                Main.main(args);
            } catch (Exception e) {
                log.error("Error in Main.main", e);
            }
        }).start();
    }
}
