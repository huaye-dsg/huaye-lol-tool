package com.example.huayeloltool.controller;

import com.example.huayeloltool.model.CurrSummoner;
import com.example.huayeloltool.model.RankedInfo;
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
        try {
            return gameUpdateService.getCurrSummoner();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/games")
    public Object searchGames() {
        try {
            String puuid1 = CurrSummoner.getInstance().getPuuid();
            return gameUpdateService.listGameHistory(puuid1, 0, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/sld")
    public Object searchGame2222s() {
        try {
            return gameUpdateService.getRankData(CurrSummoner.getInstance().getPuuid());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/lcu/search/ewrwerwerwe")
    public Object searchGamedsds2222s() {
        try {
            return gameUpdateService.searchDFDAta(CurrSummoner.getInstance().getPuuid());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
