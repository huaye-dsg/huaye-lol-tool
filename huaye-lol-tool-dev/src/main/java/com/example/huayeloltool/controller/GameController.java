package com.example.huayeloltool.controller;

import com.example.huayeloltool.enums.Constant;
import com.example.huayeloltool.model.ProcessInfo;
import com.example.huayeloltool.monitor.GameFlowMonitor;
import com.example.huayeloltool.service.impl.GameUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;



//@Slf4j
@RestController(value = "game")
public class GameController {

    @Autowired
    GameUpdateService gameUpdateService;

    @GetMapping("/lcu/getAuthInfo")
    public Object getAuthInfo() {
        // 1、解析LOL进程。拿到端口和token
        ProcessInfo lolClientApiInfo = gameUpdateService.getLolClientApiInfo(Constant.LOL_UX_PROCESS_NAME);

        // 2、初始化监听器
        GameFlowMonitor gameFlowMonitor = new GameFlowMonitor();
//        if (lolClientApiInfo == null) {
//            lolClientApiInfo = new ProcessInfo(6666, "ssssssssssssssssssss");
//            System.out.println("LOL接口进程未找到!");
//        }
        try {
            gameFlowMonitor.initGameFlowMonitor(lolClientApiInfo.getPort(), lolClientApiInfo.getToken());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }




}
