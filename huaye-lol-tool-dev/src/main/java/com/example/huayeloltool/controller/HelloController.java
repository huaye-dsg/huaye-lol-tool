package com.example.huayeloltool.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DESCRIPTION
 */
@RestController
@Slf4j
public class HelloController {

    @PostMapping("/hello")
    public String hello() {
        log.info("hello");
        return "hello";
    }
}
