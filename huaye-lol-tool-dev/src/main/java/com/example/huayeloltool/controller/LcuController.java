package com.example.huayeloltool.controller;

import com.example.huayeloltool.service.LcuApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DESCRIPTION
 *
 * @author zhangshuai
 * @since 2025/5/29
 */
@RestController(value = "/lcu")
public class LcuController {

    static LcuApiService lcuApiService = LcuApiService.getInstance();

    @GetMapping("/summoner/info")
    public String getSummonerInfo(@RequestParam("name") String name, @RequestParam("tagLine") String tagLine) {
        return lcuApiService.getSummonerByNickName(name, tagLine);
    }

}
